@echo off
setlocal

if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven not found. Install Maven 3.9+ or set MAVEN_HOME.
  pause
  exit /b 1
)

call mvn javafx:run
