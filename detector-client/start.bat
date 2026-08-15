@echo off
chcp 65001 >nul
cd /d D:\lsb_GraduationProject\detector-client
echo Starting titration detector...
D:\download\python.exe main.py
if errorlevel 1 (
    echo.
    echo [ERROR] Detector failed to start. Please check the message above.
    pause
)
