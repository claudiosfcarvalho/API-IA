@echo off
setlocal EnableExtensions

set "ROOT_DIR=%~dp0.."
set "RUN_DIR=%ROOT_DIR%\.run"
set "LOG_DIR=%ROOT_DIR%\logs"
set "TARGET=%~1"

if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

cd /d "%ROOT_DIR%"

if /I "%TARGET%"=="ia" goto start_ia
if /I "%TARGET%"=="api" goto start_api
if /I "%TARGET%"=="frontend" goto start_frontend

echo Uso: scripts\start.bat [ia^|api^|frontend]
exit /b 1

:start_ia
echo [start] Subindo containers de IA...
docker compose up -d ollama whisperx
exit /b %errorlevel%

:start_api
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo [start] API Java ja esta em execucao na porta 8080.
    exit /b 0
)

echo [start] Compilando backend com Maven...
mvn clean package -f "%ROOT_DIR%\pom.xml"
if errorlevel 1 exit /b %errorlevel%

set "JAR_PATH="
for %%F in ("%ROOT_DIR%\target\*.jar") do (
    echo %%~nxF | findstr /B /C:"original-" >nul 2>&1
    if errorlevel 1 if not defined JAR_PATH set "JAR_PATH=%%~fF"
)

if not defined JAR_PATH (
    echo [start] Nenhum JAR executavel encontrado em target\.
    exit /b 1
)

echo [start] Iniciando API Java...
start "api-ia-backend" /b cmd /c "java -jar ""%JAR_PATH%"" > ""%LOG_DIR%\backend.log"" 2>&1"
for /l %%I in (1,1,30) do (
    for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
        > "%RUN_DIR%\backend.pid" echo %%P
        goto api_started
    )
)

:api_started
echo [start] API Java iniciada.
exit /b 0

:start_frontend
netstat -ano | findstr ":4200" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo [start] Frontend ja esta em execucao na porta 4200.
    exit /b 0
)

echo [start] Iniciando frontend Angular...
pushd "%ROOT_DIR%\frontend"
start "api-ia-frontend" /b cmd /c "npm start > ""%LOG_DIR%\frontend.log"" 2>&1"
popd

for /l %%I in (1,1,30) do (
    for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":4200" ^| findstr "LISTENING"') do (
        > "%RUN_DIR%\frontend.pid" echo %%P
        goto frontend_started
    )
)

:frontend_started
echo [start] Frontend iniciado.
exit /b 0
