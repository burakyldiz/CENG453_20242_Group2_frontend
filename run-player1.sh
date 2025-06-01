#!/bin/bash
# Player 1 (port 8083)

# Only kill processes on port 8083 if found
echo "Checking for existing processes on port 8083..."
if lsof -i:8083 > /dev/null 2>&1; then
    echo "Port 8083 is in use. Stopping the process..."
    lsof -i:8083 -t | xargs kill 2>/dev/null || true
    sleep 1
fi

# Force using port 8083 using multiple mechanisms
echo "Starting Player 1 on port 8083..."
JAVA_OPTS="-Dserver.port=8083" mvn javafx:run -Djavafx.jvmArgs="-Dserver.port=8083 -Dspring.profiles.active=player1"
