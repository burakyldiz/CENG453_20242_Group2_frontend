#!/bin/bash
# Player 2 (port 8084)

# Only kill processes on port 8084 if found
echo "Checking for existing processes on port 8084..."
if lsof -i:8084 > /dev/null 2>&1; then
    echo "Port 8084 is in use. Stopping the process..."
    lsof -i:8084 -t | xargs kill 2>/dev/null || true
    sleep 1
fi

# Force using port 8084 using multiple mechanisms
echo "Starting Player 2 on port 8084..."
JAVA_OPTS="-Dserver.port=8084" mvn javafx:run -Djavafx.jvmArgs="-Dserver.port=8084 -Dspring.profiles.active=player2"
