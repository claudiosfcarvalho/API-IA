@echo off

REM *****************************************************************************
REM Script de parada da aplicação API-IA para Windows (Batch)
REM
REM Responsabilidades:
REM   1. Parar backend Spring Boot (lê PID de .run/backend.pid e mata processo)
REM   2. Parar frontend Angular (lê PID de .run/frontend.pid e mata processo)
REM   3. Derrubar infraestrutura (Docker Compose down)
REM
REM Pré-requisitos:
REM   - Scripts de inicialização (start.bat) foram executados anteriormente
REM   - Docker instalado e rodando
REM
REM Uso: stop.bat
REM *****************************************************************************

set ROOT_DIR=%~dp0\..
cd /d %ROOT_DIR%

REM Parar backend Spring Boot
if exist "%ROOT_DIR%\.run\backend.pid" (
    set /p BACK_PID=<"%ROOT_DIR%\.run\backend.pid"
    tasklist /FI "PID eq %BACK_PID%" | findstr /i "java" >nul 2>&1 && (
        echo [stop] Encerrando backend PID=%BACK_PID%
        taskkill /PID %BACK_PID% /F
    )
    del "%ROOT_DIR%\.run\backend.pid"
)

REM Parar frontend Angular
if exist "%ROOT_DIR%\.run\frontend.pid" (
    set /p FRONT_PID=<"%ROOT_DIR%\.run\frontend.pid"
    tasklist /FI "PID eq %FRONT_PID%" | findstr /i "node" >nul 2>&1 && (
        echo [stop] Encerrando frontend PID=%FRONT_PID%
        taskkill /PID %FRONT_PID% /F
    )
    del "%ROOT_DIR%\.run\frontend.pid"
)

REM Derrubar infraestrutura (Docker Compose)
echo [stop] Derrubando infraestrutura...
docker compose down
echo [stop] Infra encerrada.
