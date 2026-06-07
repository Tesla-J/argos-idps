#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$PROJECT_DIR/build/libs/Argos_IDPS-1.0-SNAPSHOT.jar"

echo "[ARGOS] Building..."
sudo ./gradlew build
if [ $? -ne 0 ]; then
    echo "[ARGOS] Build failed. Aborting."
    exit 1
fi

echo "[ARGOS] Starting..."
sudo java -Xms256m -Xmx768m -jar "$JAR"