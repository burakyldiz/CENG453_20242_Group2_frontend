#!/bin/bash

# Script to run multiple instances of UNO game for multiplayer testing
echo "UNO Multiplayer Test Script"
echo "=========================="

# Kill any existing Java processes that might be using our ports
pkill -f "java.*8082" || true
pkill -f "java.*8083" || true
pkill -f "java.*8084" || true
sleep 1

# Compile the project first
echo "Compiling project..."
mvn clean compile

# Check if compilation was successful
if [ $? -ne 0 ]; then
    echo "Compilation failed. Exiting."
    exit 1
fi

# Path variables
PROJECT_DIR="/Users/alperis/Desktop/435-2/CENG453_20242_Group2_frontend"
TARGET_DIR="$PROJECT_DIR/target/classes"
RESOURCE_DIR="$PROJECT_DIR/src/main/resources"

# Create special launcher classes for each player instance
cat > $TARGET_DIR/com/ceng453/frontend/Player1Launcher.class << 'EOL'
����   = 
      java/lang/Object <init> ()V  java/lang/String
 
     'com/ceng453/frontend/UnoApplication main ([Ljava/lang/String;)V  'com/ceng453/frontend/Player1Launcher Code LineNumberTable LocalVariableTable this )Lcom/ceng453/frontend/Player1Launcher; args [Ljava/lang/String; 
SourceFile Player1Launcher.java !               /     *� �                        	       6     � � 	�       
                        
EOL

cat > $TARGET_DIR/com/ceng453/frontend/Player2Launcher.class << 'EOL'
����   = 
      java/lang/Object <init> ()V  java/lang/String
 
     'com/ceng453/frontend/UnoApplication main ([Ljava/lang/String;)V  'com/ceng453/frontend/Player2Launcher Code LineNumberTable LocalVariableTable this )Lcom/ceng453/frontend/Player2Launcher; args [Ljava/lang/String; 
SourceFile Player2Launcher.java !               /     *� �                        	       6     � � 	�       
                        
EOL

# Use osascript to open new terminal windows for each player
if command -v osascript &> /dev/null; then
    echo "Starting Player 1 (port 8083)..."
    osascript -e "tell application \"Terminal\" to do script \"cd $PROJECT_DIR && java -cp $TARGET_DIR -Dspring.profiles.active=player1 com.ceng453.frontend.Player1Launcher\""
    
    sleep 2
    
    echo "Starting Player 2 (port 8084)..."
    osascript -e "tell application \"Terminal\" to do script \"cd $PROJECT_DIR && java -cp $TARGET_DIR -Dspring.profiles.active=player2 com.ceng453.frontend.Player2Launcher\""
    
    echo ""
    echo "UNO Multiplayer Test Environment started!"
    echo ""
    echo "To test multiplayer:"
    echo "1. Log in with different usernames in each window"
    echo "2. Create a game in one window, copy the game ID"
    echo "3. Join the game using that ID in the other window"
    echo "4. Start the game from the creator's window"
else
    echo "This script requires macOS with osascript. Please run these commands manually:"
    echo ""
    echo "Terminal 1: java -cp $TARGET_DIR -Dspring.profiles.active=player1 com.ceng453.frontend.Player1Launcher"
    echo "Terminal 2: java -cp $TARGET_DIR -Dspring.profiles.active=player2 com.ceng453.frontend.Player2Launcher"
fi
