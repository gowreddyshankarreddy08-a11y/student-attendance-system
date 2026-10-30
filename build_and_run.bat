@echo off
title Student Attendance Management System
echo ==============================================================
echo     Compiling Student Attendance Management System (Java)
echo ==============================================================

if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\com\attendance\model\*.java src\com\attendance\storage\*.java src\com\attendance\service\*.java src\com\attendance\server\*.java src\com\attendance\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please make sure JDK is installed and in PATH.
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful!
echo Starting server on http://localhost:8080 ...
echo Press Ctrl+C in this window to stop the server anytime.
echo.
java -cp bin com.attendance.Main
pause
