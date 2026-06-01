@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-javafx.ps1" %*
exit /b %ERRORLEVEL%
