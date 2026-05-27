#!/bin/bash
# ==========================================================================
# AI-Based Interview Preparation System - Launcher (Bash)
# Antigravity AI Pair Programming Companion
# ==========================================================================

set -e
clear

echo -e "\033[36m=========================================================\033[0m"
echo -e "\033[36m🚀 Launching AI Interview Prep Server Bootstrap...\033[0m"
echo -e "\033[36m=========================================================\033[0m"

# 1. Verify Java Installation
if ! command -v java &> /dev/null; then
    echo -e "\033[31m❌ ERROR: Java JVM is not installed or not in your system PATH!\033[0m"
    echo -e "\033[33mPlease install Java 17+ and try again.\033[0m"
    exit 1
fi
echo -e "\033[32m✅ Java detected successfully.\033[0m"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
BIN_DIR="$SCRIPT_DIR/bin"
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"

# 2. Check if Maven is available globally
if command -v mvn &> /dev/null; then
    echo -e "\033[32m📦 Global Maven found. Running compilation...\033[0m"
    mvn clean compile
    
    echo -e "\033[36m🔥 Starting embedded server via Maven...\033[0m"
    mvn exec:java -Dexec.mainClass="com.interview.prep.InterviewPrepApp"
else
    echo -e "\033[33mℹ️  Maven not detected globally. Falling back to self-contained JDK compiler...\033[0m"
    echo -e "\033[36m🛠️  Starting manual compilation workflow...\033[0m"

    mkdir -p "$LIB_DIR"
    mkdir -p "$BIN_DIR"

    # Download Gson if missing
    if [ ! -f "$GSON_JAR" ]; then
        echo -e "\033[33m🌐 Downloading lightweight Gson dependency from Maven Central...\033[0m"
        GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
        
        if command -v curl &> /dev/null; then
            curl -L -o "$GSON_JAR" "$GSON_URL"
        elif command -v wget &> /dev/null; then
            wget -O "$GSON_JAR" "$GSON_URL"
        else
            echo -e "\033[31m❌ ERROR: Neither curl nor wget was found. Please download Gson manually into lib/.\033[0m"
            exit 1
        fi
        echo -e "\033[32m✅ Gson library downloaded successfully (lib/gson-2.10.1.jar).\033[0m"
    fi

    # Compile source files
    echo -e "\033[33m🔨 Compiling Java sources...\033[0m"
    find "$SCRIPT_DIR/src" -name "*.java" > "$SCRIPT_DIR/sources.txt"
    
    if javac -cp "$GSON_JAR" -d "$BIN_DIR" @"$SCRIPT_DIR/sources.txt"; then
        echo -e "\033[32m✅ Compilation successful!\033[0m"
        rm -f "$SCRIPT_DIR/sources.txt"
    else
        echo -e "\033[31m❌ ERROR: Compilation failed! Review Java errors above.\033[0m"
        rm -f "$SCRIPT_DIR/sources.txt"
        exit 1
    fi

    # Start the application
    echo -e "\033[36m🔥 Starting embedded Server...\033[0m"
    CLASSPATH="$BIN_DIR:$GSON_JAR:src/main/resources"
    java -cp "$CLASSPATH" com.interview.prep.InterviewPrepApp
fi
