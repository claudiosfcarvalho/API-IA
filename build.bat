@echo off
REM Build script for API-IA project with deprecation handling
REM Usage: build.bat [clean|build|serve|all]

setlocal enabledelayedexpansion

if "%1%"=="" (
    set TARGET=build
) else (
    set TARGET=%1%
)

set PROJECT_ROOT=%~dp0

echo.
echo ============================================
echo API-IA Build Script
echo ============================================
echo Target: %TARGET%
echo.

REM Configure JVM options to handle Guice warnings
set MAVEN_OPTS=--add-opens java.base/sun.misc=ALL-UNNAMED -XX:+IgnoreUnrecognizedVMOptions -Xmx2G

if "%TARGET%"=="clean" (
    echo [1/1] Cleaning backend...
    cd /d "%PROJECT_ROOT%"
    call mvn clean -q
    if !errorlevel! equ 0 (
        echo [OK] Backend cleaned successfully
    ) else (
        echo [ERROR] Backend clean failed
        exit /b 1
    )
)

if "%TARGET%"=="build" (
    echo [1/2] Building backend...
    cd /d "%PROJECT_ROOT%"
    call mvn clean compile -DskipTests -q
    if !errorlevel! equ 0 (
        echo [OK] Backend built successfully
    ) else (
        echo [ERROR] Backend build failed
        exit /b 1
    )
    
    echo [2/2] Building frontend...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm run build
    if !errorlevel! equ 0 (
        echo [OK] Frontend built successfully
        echo.
        echo ============================================
        echo Build Complete!
        echo Backend: dist in target/
        echo Frontend: dist in frontend/dist/
        echo ============================================
    ) else (
        echo [ERROR] Frontend build failed
        exit /b 1
    )
)

if "%TARGET%"=="serve" (
    echo [1/2] Starting backend (mvn spring-boot:run)...
    cd /d "%PROJECT_ROOT%"
    start "API-IA Backend" mvn spring-boot:run -q
    timeout /t 3
    
    echo [2/2] Starting frontend (ng serve)...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm start
)

if "%TARGET%"=="all" (
    call "%~f0" clean
    call "%~f0" build
    if !errorlevel! equ 0 (
        echo.
        echo Ready for development!
        echo Run: build.bat serve
    )
)

endlocal
