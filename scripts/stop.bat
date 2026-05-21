@echo off
setlocal EnableExtensions

set "ROOT_DIR=%~dp0.."
set "RUN_DIR=%ROOT_DIR%\.run"
set "TARGET=%~1"

cd /d "%ROOT_DIR%"

if /I "%TARGET%"=="ia" goto stop_ia
if /I "%TARGET%"=="api" goto stop_api
if /I "%TARGET%"=="frontend" goto stop_frontend

echo Uso: scripts\stop.bat [ia^|api^|frontend]
exit /b 1

:stop_ia
echo [stop] Parando containers de IA...
docker compose stop ollama whisperx
exit /b %errorlevel%

:stop_api
if exist "%RUN_DIR%\backend.pid" (
    set /p BACK_PID=<"%RUN_DIR%\backend.pid"
    if defined BACK_PID (
        echo [stop] Encerrando backend PID=%BACK_PID%
        taskkill /PID %BACK_PID% /T /F >nul 2>&1
    )
    del "%RUN_DIR%\backend.pid" >nul 2>&1
    exit /b 0
)
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do taskkill /PID %%P /T /F >nul 2>&1
echo [stop] API Java parada.
exit /b 0

:stop_frontend
if exist "%RUN_DIR%\frontend.pid" (
    set /p FRONT_PID=<"%RUN_DIR%\frontend.pid"
    if defined FRONT_PID (
        echo [stop] Encerrando frontend PID=%FRONT_PID%
        taskkill /PID %FRONT_PID% /T /F >nul 2>&1
    )
    del "%RUN_DIR%\frontend.pid" >nul 2>&1
    exit /b 0
)
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":4200" ^| findstr "LISTENING"') do taskkill /PID %%P /T /F >nul 2>&1
echo [stop] Frontend parado.
exit /b 0
