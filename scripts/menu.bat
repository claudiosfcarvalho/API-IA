@echo off
setlocal EnableExtensions

:menu
cls
echo Selecione uma opcao:
echo 1. Iniciar containers de IA
echo 2. Parar containers de IA
echo 3. Iniciar API Java
echo 4. Parar API Java
echo 5. Iniciar Frontend
echo 6. Parar Frontend
echo 7. Sair
echo.
set /p ESCOLHA=Digite sua escolha: 

if "%ESCOLHA%"=="1" call "%~dp0start.bat" ia & goto pause_menu
if "%ESCOLHA%"=="2" call "%~dp0stop.bat" ia & goto pause_menu
if "%ESCOLHA%"=="3" call "%~dp0start.bat" api & goto pause_menu
if "%ESCOLHA%"=="4" call "%~dp0stop.bat" api & goto pause_menu
if "%ESCOLHA%"=="5" call "%~dp0start.bat" frontend & goto pause_menu
if "%ESCOLHA%"=="6" call "%~dp0stop.bat" frontend & goto pause_menu
if "%ESCOLHA%"=="7" exit /b 0

echo Opcao invalida.

:pause_menu
echo.
pause
goto menu