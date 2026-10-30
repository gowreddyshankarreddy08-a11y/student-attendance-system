# PowerShell Runner for Student Attendance Management System
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Compiling & Launching Student Attendance System (Java 27+)   " -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$javaFiles = (Get-ChildItem -Path "src" -Recurse -Filter "*.java").FullName
& javac -encoding UTF-8 -d bin $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[SUCCESS] Compilation successful!" -ForegroundColor Green
Write-Host "Starting server at http://localhost:8080 ..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the server anytime.`n" -ForegroundColor Gray

& java -cp bin com.attendance.Main
