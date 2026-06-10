@echo off
setlocal

set "APP_NAME=DailyCostCalculator"
set "APP_VERSION=1.0.3"
set "DEFAULT_INSTALL_DIR=%APP_NAME%"
set "D_DAILY_INSTALL_DIR=D:\daily\DailyCostCalculator"
set "MAIN_JAR=daily-cost-calculator.jar"
set "MAIN_CLASS=com.dailycost.Launcher"

if not defined JAVA_HOME (
  if exist "%USERPROFILE%\.jdks\ms-21.0.7\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.7"
  ) else if exist "C:\Program Files\BellSoft\LibericaJDK-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-21"
  )
)

if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"
if defined WIX_HOME set "PATH=%WIX_HOME%;%PATH%"
if exist "%~dp0tools\wix\candle.exe" set "PATH=%~dp0tools\wix;%PATH%"

java -version >nul 2>nul
if errorlevel 1 (
  echo Java 21 not found. Set JAVA_HOME to a JDK 21 installation.
  if not defined NO_PAUSE pause
  exit /b 1
)

mvn -version >nul 2>nul
if errorlevel 1 (
  echo Maven not found. Install Maven 3.9+ or set MAVEN_HOME.
  if not defined NO_PAUSE pause
  exit /b 1
)

jpackage --version >nul 2>nul
if errorlevel 1 (
  echo jpackage not found. Use a full JDK 21, not a JRE.
  if not defined NO_PAUSE pause
  exit /b 1
)

candle.exe -? >nul 2>nul
if errorlevel 1 (
  echo WiX 3.x not found. Install WiX Toolset 3.x and add it to PATH, or set WIX_HOME.
  echo Download: https://wixtoolset.org/docs/wix3/
  if not defined NO_PAUSE pause
  exit /b 1
)

if exist dist rmdir /s /q dist
if exist dist-msi rmdir /s /q dist-msi
if exist dist-app rmdir /s /q dist-app

call mvn clean package
if errorlevel 1 (
  echo Maven package failed.
  if not defined NO_PAUSE pause
  exit /b 1
)

jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest dist ^
  --install-dir "%DEFAULT_INSTALL_DIR%" ^
  --win-dir-chooser ^
  --win-menu-group "%APP_NAME%" ^
  --win-menu ^
  --win-shortcut ^
  --vendor "DailyCostCalculator" ^
  --description "Daily Cost Calculator"

if errorlevel 1 (
  echo jpackage exe failed.
  if not defined NO_PAUSE pause
  exit /b 1
)

if exist "dist\%APP_NAME%-%APP_VERSION%.exe" move /Y "dist\%APP_NAME%-%APP_VERSION%.exe" "dist\%APP_NAME%-Setup.exe"

jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest dist-msi ^
  --install-dir "%DEFAULT_INSTALL_DIR%" ^
  --win-dir-chooser ^
  --win-menu-group "%APP_NAME%" ^
  --win-menu ^
  --win-shortcut ^
  --vendor "DailyCostCalculator" ^
  --description "Daily Cost Calculator"

if errorlevel 1 (
  echo jpackage msi failed.
  if not defined NO_PAUSE pause
  exit /b 1
)

if exist "dist-msi\%APP_NAME%-%APP_VERSION%.msi" move /Y "dist-msi\%APP_NAME%-%APP_VERSION%.msi" "dist\%APP_NAME%-Setup.msi"
if exist dist-msi rmdir /s /q dist-msi

(
  echo @echo off
  echo setlocal
  echo set "TARGET_DIR=%D_DAILY_INSTALL_DIR%"
  echo if not exist "%%TARGET_DIR%%" mkdir "%%TARGET_DIR%%"
  echo msiexec /i "%%~dp0%APP_NAME%-Setup.msi" INSTALLDIR="%%TARGET_DIR%%"
) > "dist\Install-to-D-daily.bat"

jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest dist-app ^
  --vendor "DailyCostCalculator" ^
  --description "Daily Cost Calculator"

if errorlevel 1 (
  echo app-image package failed.
  if not defined NO_PAUSE pause
  exit /b 1
)

echo Build completed:
echo - dist\%APP_NAME%-Setup.exe
echo - dist\%APP_NAME%-Setup.msi
echo - dist\Install-to-D-daily.bat
echo - dist-app\%APP_NAME%\%APP_NAME%.exe

if not defined NO_PAUSE pause
