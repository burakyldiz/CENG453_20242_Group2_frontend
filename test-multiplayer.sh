#!/bin/bash

# This script launches both instances of the UNO game for multiplayer testing
echo "Starting UNO Multiplayer Test Environment"
echo "----------------------------------------"
echo "This will open two separate terminals running different instances"
echo ""

# Define color codes for terminal output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Define the project path
PROJECT_PATH="/Users/alperis/Desktop/435-2/CENG453_20242_Group2_frontend"

# Check if osascript is available (macOS only)
if command -v osascript &> /dev/null; then
    echo -e "${GREEN}Starting Player 1 instance (port 8083)...${NC}"
    osascript -e "tell application \"Terminal\" to do script \"cd $PROJECT_PATH && chmod +x run-player1.sh && ./run-player1.sh; echo 'Player 1 instance closed'; sleep 3\""
    
    # Small delay to ensure first instance starts properly
    sleep 2
    
    echo -e "${BLUE}Starting Player 2 instance (port 8084)...${NC}"
    osascript -e "tell application \"Terminal\" to do script \"cd $PROJECT_PATH && chmod +x run-player2.sh && ./run-player2.sh; echo 'Player 2 instance closed'; sleep 3\""
    
    echo ""
    echo "Both instances started in separate terminal windows."
    echo "To test multiplayer:"
    echo "1. Log in with different usernames in each window"
    echo "2. Create a game in one window, copy the game ID"
    echo "3. Join the game using that ID in the other window"
    echo "4. Start the game from the creator's window"
else
    echo "This script requires macOS with osascript support."
    echo "Please run the following commands in separate terminal windows manually:"
    echo ""
    echo "Terminal 1: cd $PROJECT_PATH && ./run-instance1.sh"
    echo "Terminal 2: cd $PROJECT_PATH && ./run-instance2.sh"
fi
