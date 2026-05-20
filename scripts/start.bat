@echo off

REM *****************************************************************************
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
REM   - Docker instalado e rodando
REM   - Maven 3.9+ instalado e no PATH
REM   - Node.js / npm instalado e no PATH
REM   - curl instalado e no PATH (para health checks)
REM
REM Uso: start.bat
REM *****************************************************************************

set ROOT_DIR=%~dp0\..
cd /d %ROOT_DIR%

REM Criar diretórios necessários
if not exist "%ROOT_DIR%\.run" mkdir "%ROOT_DIR%\.run"
if not exist "%ROOT_DIR%\logs" mkdir "%ROOT_DIR%\logs"

REM Etapa 1: Infraestrutura (Docker Compose)
echo [start] Subindo infraestrutura com docker compose...
docker compose up -d

REM Etapa 2: Aguardar Ollama (LLM local)
echo [start] Aguardando Ollama em http://localhost:11434 ...
for /l %%i in (1,1,60) do (
    curl -sf http://localhost:11434/ >nul 2>&1 && (
        echo [start] Ollama pronto.
        goto :OLLAMA_READY
    ) || (
        timeout /t 2 >nul
    )
)
echo [start] Timeout aguardando Ollama.
exit /b 1
:OLLAMA_READY

REM Etapa 3: Backend Spring Boot
echo [start] Iniciando backend Spring Boot na porta 8080...
start /b java -jar "%ROOT_DIR%\backend\target\api-ia.jar" > "%ROOT_DIR%\logs\backend.log" 2>&1
(for /f "tokens=*" %%a in ('wmic process where "commandline like '%%api-ia.jar%%'" get processid ^| findstr /r /v "^$"') do @echo %%a) > "%ROOT_DIR%\.run\backend.pid"
echo [start] Backend iniciado com PID=%ROOT_DIR%\.run\backend.pid

REM Etapa 4: Frontend Angular
echo [start] Iniciando frontend Angular na porta 4200...
cd "%ROOT_DIR%\frontend"
start /b npm start > "%ROOT_DIR%\logs\frontend.log" 2>&1
(for /f "tokens=*" %%a in ('wmic process where "commandline like '%%npm start%%'" get processid ^| findstr /r /v "^$"') do @echo %%a) > "%ROOT_DIR%\.run\frontend.pid"
echo [start] Frontend iniciado com PID=%ROOT_DIR%\.run\frontend.pid"
