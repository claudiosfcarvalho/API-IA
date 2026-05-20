@echo off
REM ============================================================================
REM Script de parada da aplicação API-IA para Windows (Batch)
REM
REM Responsabilidades:
REM   1. Parar backend Spring Boot (lê PID de .run\backend.pid e mata processo)
REM   2. Parar frontend Angular (lê PID de .run\frontend.pid e mata processo)
REM   3. Derrubar infraestrutura (Docker Compose down)
REM
REM Pré-requisitos:
REM   - Scripts de inicialização (start.bat) foram executados anteriormente
REM   - Docker Desktop/CLI instalado
REM
REM Uso: .\stop.bat
REM
REM ============================================================================

setlocal enabledelayedexpansion

set ROOT_DIR=%~dp0..
cd /d "%ROOT_DIR%"

REM Parar backend Spring Boot
if exist "%ROOT_DIR%\.run\backend.pid" (
	set /p BACKEND_PID=<"%ROOT_DIR%\.run\backend.pid"
	echo [stop] Encerrando backend PID=!BACKEND_PID!
	REM Valida se BACKEND_PID contém apenas números antes de matar
	echo !BACKEND_PID!| findstr /r "^[0-9][0-9]*$" >nul && taskkill /PID !BACKEND_PID! /T /F >nul 2>nul
	del "%ROOT_DIR%\.run\backend.pid" >nul 2>nul
)

REM Parar frontend Angular
if exist "%ROOT_DIR%\.run\frontend.pid" (
	set /p FRONTEND_PID=<"%ROOT_DIR%\.run\frontend.pid"
	echo [stop] Encerrando frontend PID=!FRONTEND_PID!
	REM Valida se FRONTEND_PID contém apenas números antes de matar
	echo !FRONTEND_PID!| findstr /r "^[0-9][0-9]*$" >nul && taskkill /PID !FRONTEND_PID! /T /F >nul 2>nul
	del "%ROOT_DIR%\.run\frontend.pid" >nul 2>nul
)

REM Derrubar infraestrutura (Docker Compose)
echo [stop] Derrubando infraestrutura...
docker compose down
if errorlevel 1 exit /b 1

echo [stop] Infra encerrada.
endlocal
