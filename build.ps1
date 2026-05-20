#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Build script for API-IA project with deprecation handling

.DESCRIPTION
    Compiles backend (Maven) and frontend (Angular) with proper JVM configuration
    to suppress known Guice Unsafe warnings

.PARAMETER Target
    Build target: clean, build, serve, all (default: build)

.EXAMPLE
    .\build.ps1 -Target clean
    .\build.ps1 -Target build
    .\build.ps1 -Target serve
    .\build.ps1 -Target all
#>

param(
    [ValidateSet('clean', 'build', 'serve', 'all')]
    [string]$Target = 'build'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommandPath

# Configure JVM options for Guice warnings
$env:MAVEN_OPTS = '--add-opens java.base/sun.misc=ALL-UNNAMED -XX:+IgnoreUnrecognizedVMOptions -Xmx2G'

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "API-IA Build Script" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Target: $Target`n"

function Invoke-Build {
    Write-Host "[1/2] Building backend..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    
    & mvn clean compile -DskipTests -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Backend built successfully" -ForegroundColor Green
    } else {
        Write-Error "Backend build failed"
    }
    
    Pop-Location
    
    Write-Host "[2/2] Building frontend..." -ForegroundColor Yellow
    Push-Location "$ProjectRoot\frontend"
    
    & npm run build
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Frontend built successfully" -ForegroundColor Green
        Write-Host "`n============================================" -ForegroundColor Cyan
        Write-Host "Build Complete!" -ForegroundColor Green
        Write-Host "Backend: dist in target/" -ForegroundColor Cyan
        Write-Host "Frontend: dist in frontend/dist/" -ForegroundColor Cyan
        Write-Host "============================================`n" -ForegroundColor Cyan
    } else {
        Write-Error "Frontend build failed"
    }
    
    Pop-Location
}

function Invoke-Clean {
    Write-Host "[1/1] Cleaning backend..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    
    & mvn clean -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Backend cleaned successfully" -ForegroundColor Green
    } else {
        Write-Error "Backend clean failed"
    }
    
    Pop-Location
}

function Invoke-Serve {
    Write-Host "[1/2] Starting backend..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    
    $backendProcess = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -NoNewWindow
    Start-Sleep -Seconds 3
    
    Write-Host "[OK] Backend started (PID: $($backendProcess.Id))" -ForegroundColor Green
    
    Pop-Location
    
    Write-Host "[2/2] Starting frontend..." -ForegroundColor Yellow
    Push-Location "$ProjectRoot\frontend"
    
    & npm start
    
    Pop-Location
}

switch ($Target) {
    'clean' { Invoke-Clean }
    'build' { Invoke-Build }
    'serve' { Invoke-Serve }
    'all' {
        Invoke-Clean
        Invoke-Build
        Write-Host "Ready for development! Run: .\build.ps1 -Target serve" -ForegroundColor Green
    }
}

Write-Host "`nBuild completed successfully!" -ForegroundColor Green
