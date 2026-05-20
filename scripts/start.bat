@echo off
REM ============================================================================
REM Script de inicialização da aplicação API-IA para Windows (Batch)
REM
REM Responsabilidades:
REM   1. Criar diretórios necessários (.run para PIDs, logs para saída)
REM   2. Iniciar infraestrutura (Docker Compose com Ollama e WhisperX)
REM   3. Aguardar disponibilidade de cada serviço (health checks)
REM   4. Iniciar backend Spring Boot na porta 8080
REM   5. Iniciar frontend Angular na porta 4200
REM
REM Pré-requisitos:
REM   - Docker Desktop/CLI instalado e rodando
REM   - Maven 3.9+ instalado e no PATH
REM   - Node.js / npm instalado e no PATH
REM   - Curl instalado e no PATH (para health checks)
REM
REM Uso: .\start.bat
REM
REM ============================================================================

setlocal enabledelayedexpansion

set ROOT_DIR=%~dp0..
cd /d "%ROOT_DIR%"

REM Criar diretórios necessários
if not exist "%ROOT_DIR%\.run" mkdir "%ROOT_DIR%\.run"
if not exist "%ROOT_DIR%\logs" mkdir "%ROOT_DIR%\logs"

REM ============================================================================
REM Etapa 1: Infraestrutura (Docker Compose)
REM ============================================================================
echo [start] Subindo infraestrutura com docker compose...
docker compose up -d
if errorlevel 1 exit /b 1

REM ============================================================================
REM Etapa 2: Aguardar Ollama (LLM local)
REM ============================================================================
echo [start] Aguardando Ollama em http://localhost:11434 ...
set /a COUNT=0
:WAIT_OLLAMA
curl -s -f http://localhost:11434/ >nul 2>nul
if not errorlevel 1 goto OLLAMA_OK
set /a COUNT+=1
if !COUNT! geq 60 (
  echo [start] Timeout aguardando Ollama.
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto WAIT_OLLAMA

:OLLAMA_OK
echo [start] Ollama pronto.

REM ============================================================================
REM Etapa 3: Aguardar WhisperX (serviço de transcrição)
REM ============================================================================
echo [start] Aguardando WhisperX em http://localhost:9000/health ...
set /a COUNT=0
:WAIT_WHISPERX
curl -s -f http://localhost:9000/health >nul 2>nul
if not errorlevel 1 goto WHISPERX_OK
set /a COUNT+=1
if !COUNT! geq 120 (
  echo [start] Timeout aguardando WhisperX.
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto WAIT_WHISPERX

:WHISPERX_OK
echo [start] WhisperX pronto.

REM ============================================================================
REM Etapa 4: Backend Spring Boot
REM ============================================================================
echo [start] Iniciando backend Spring Boot...
powershell -NoProfile -Command "$p = Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run' -WorkingDirectory '%ROOT_DIR%' -RedirectStandardOutput '%ROOT_DIR%\logs\backend.log' -RedirectStandardError '%ROOT_DIR%\logs\backend.err.log' -PassThru; Set-Content -Path '%ROOT_DIR%\.run\backend.pid' -Value $p.Id -Encoding ascii"
set /p BACKEND_PID=<"%ROOT_DIR%\.run\backend.pid"

echo [start] Aguardando backend em http://localhost:8080/actuator/health ...
set /a COUNT=0
:WAIT_BACKEND
curl -s -f http://localhost:8080/actuator/health >nul 2>nul
if not errorlevel 1 goto BACKEND_OK
set /a COUNT+=1
if !COUNT! geq 120 (
  echo [start] Timeout aguardando backend. Consulte logs\backend.log
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto WAIT_BACKEND

:BACKEND_OK
echo [start] Backend pronto.

REM ============================================================================
REM Etapa 5: Frontend Angular
REM ============================================================================
echo [start] Iniciando frontend Angular...
powershell -NoProfile -Command "$p = Start-Process -FilePath 'npm.cmd' -ArgumentList 'start -- --host 0.0.0.0 --port 4200' -WorkingDirectory '%ROOT_DIR%\frontend' -RedirectStandardOutput '%ROOT_DIR%\logs\frontend.log' -RedirectStandardError '%ROOT_DIR%\logs\frontend.err.log' -PassThru; Set-Content -Path '%ROOT_DIR%\.run\frontend.pid' -Value $p.Id -Encoding ascii"
set /p FRONTEND_PID=<"%ROOT_DIR%\.run\frontend.pid"

REM ============================================================================
REM Resumo de inicialização
REM ============================================================================
echo [start] Ambiente pronto.
echo [start] URLs:
echo   Backend: http://localhost:8080
echo   Frontend: http://localhost:4200
echo   Health backend: http://localhost:8080/actuator/health
echo   Health whisperx: http://localhost:9000/health
echo [start] Logs: logs\backend.log e logs\frontend.log

endlocal
