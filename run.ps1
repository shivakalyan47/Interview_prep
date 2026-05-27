# ==========================================================================
# AI-Based Interview Preparation System - Launcher
# Antigravity AI Pair Programming Companion
# ==========================================================================

$ErrorActionPreference = "Stop"
Clear-Host

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "[START] Launching AI Interview Prep Server Bootstrap..." -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

# 1. Verify Java Installation
try {
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $javaCheck = & java -version 2>&1
    $ErrorActionPreference = $oldPreference
    Write-Host "[SUCCESS] Java Runtime Environment detected successfully." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java JVM is not installed or not in your system PATH!" -ForegroundColor Red
    Write-Host "Please install Java 17+ and try again." -ForegroundColor Yellow
    Exit 1
}

# Define project directories
$libDir = Join-Path $PSScriptRoot "lib"
$binDir = Join-Path $PSScriptRoot "bin"
$gsonJar = Join-Path $libDir "gson-2.10.1.jar"

# 2. Check if Maven is available globally
$mvnAvailable = $false
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnAvailable = $true
} else {
    Write-Host "[INFO] Maven not detected globally. Falling back to self-contained JDK compiler..." -ForegroundColor Yellow
}

if ($mvnAvailable) {
    Write-Host "[BUILD] Global Maven found. Running compilation..." -ForegroundColor Green
    & mvn clean compile
    
    Write-Host "[RUN] Starting embedded server via Maven..." -ForegroundColor Cyan
    & mvn exec:java -Dexec.mainClass="com.interview.prep.InterviewPrepApp"
} else {
    # Self-contained compiler logic
    Write-Host "[BUILD] Starting manual compilation workflow..." -ForegroundColor Cyan

    # Create directories if missing
    if (!(Test-Path $libDir)) {
        New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    }
    if (!(Test-Path $binDir)) {
        New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    }

    # Download Gson if missing
    if (!(Test-Path $gsonJar)) {
        Write-Host "[DOWNLOAD] Downloading lightweight Gson dependency from Maven Central..." -ForegroundColor Yellow
        $gsonUrl = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Invoke-WebRequest -Uri $gsonUrl -OutFile $gsonJar -UseBasicParsing
            Write-Host "[SUCCESS] Gson library downloaded successfully (lib/gson-2.10.1.jar)." -ForegroundColor Green
        } catch {
            Write-Host "[ERROR] Failed to download Gson dependency! Please check internet connection." -ForegroundColor Red
            Exit 1
        }
    }

    # Gather Java sources
    Write-Host "[BUILD] Indexing Java source files..." -ForegroundColor Gray
    $sourceFiles = @(Get-ChildItem -Path (Join-Path $PSScriptRoot "src") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName })

    # Compile source files
    Write-Host "[BUILD] Compiling Java sources..." -ForegroundColor Yellow
    & javac -cp $gsonJar -d $binDir $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Compilation failed! Review Java errors above." -ForegroundColor Red
        Exit 1
    }
    Write-Host "[SUCCESS] Compilation successful!" -ForegroundColor Green

    # Start the application
    Write-Host "[RUN] Starting embedded Server..." -ForegroundColor Cyan
    # Include src/main/resources in classpath to support loading static files via ClassLoader resource stream
    $classpath = "$binDir;$gsonJar;src/main/resources"
    & java -cp $classpath com.interview.prep.InterviewPrepApp
}
