@echo off
setlocal

if exist target rmdir /s /q target
if exist dist rmdir /s /q dist
if exist dist-app rmdir /s /q dist-app
if exist dist-msi rmdir /s /q dist-msi
if exist tmp-jpackage-dist rmdir /s /q tmp-jpackage-dist
if exist tmp-jpackage-temp rmdir /s /q tmp-jpackage-temp
if exist installer-run.log del /q installer-run.log

echo Local build artifacts removed.
