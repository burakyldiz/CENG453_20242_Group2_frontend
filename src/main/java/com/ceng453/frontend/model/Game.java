package com.ceng453.frontend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private List<Player> players;
    private Deck deck;
    private List<Card> discardPile;
    private Card.Color currentColor;
    private int currentPlayerIndex;
    private boolean isClockwise;
    private int drawTwoCounter;
    private int drawFourCounter;
    private boolean isGameOver;
    private boolean isChallengeActive; // For Wild Draw Four challenge in multiplayer
    
    public Game() {
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.discardPile = new ArrayList<>();
        this.isClockwise = true;
        this.currentPlayerIndex = 0;
        this.drawTwoCounter = 0;
        this.drawFourCounter = 0;
        this.isGameOver = false;
        this.isChallengeActive = false;
        
        // Shuffle the deck
        deck.shuffle();
    }
    
    // Initialize a single player game with one human and three CPU players
    public void initializeSinglePlayerGame(String playerName) {
        players.clear();
        
        // Add human player
        players.add(new Player(playerName, true));
        
        // Add three CPU players
        players.add(new Player("CPU 1", false));
        players.add(new Player("CPU 2", false));
        players.add(new Player("CPU 3", false));
        
        // Deal 7 cards to each player
        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                player.addCard(deck.drawCard());
            }
        }
        
        // Draw first card to start the game
        Card initialCard = deck.drawInitialCard();
        discardPile.add(initialCard);
        currentColor = initialCard.getColor();
        
        // Apply effect of initial card if it's an action card
        if (initialCard.getType() != Card.Type.NUMBER) {
            handleActionCard(initialCard);
        }
    }
    
    // Initialize a multiplayer game
    public void initializeMultiplayerGame(List<String> playerNames) {
        players.clear();
        
        // Add all players (all human in multiplayer)
        for (String name : playerNames) {
            players.add(new Player(name, true));
        }
        
        // Deal 7 cards to each player
        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                player.addCard(deck.drawCard());
            }
        }
        
        // Draw first card to start the game
        Card initialCard = deck.drawInitialCard();
        discardPile.add(initialCard);
        currentColor = initialCard.getColor();
        
        // Apply effect of initial card if it's an action card
        if (initialCard.getType() != Card.Type.NUMBER) {
            handleActionCard(initialCard);
        }
    }
    
    // Method to play a card from the current player's hand
    public boolean playCard(int cardIndex) {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (cardIndex < 0 || cardIndex >= currentPlayer.getHand().size()) {
            return false;
        }
        
        Card card = currentPlayer.getHand().get(cardIndex);
        Card topCard = getTopCard();
        
        // First, check if there's a pending Draw Four stack
        if (drawFourCounter > 0) {
            // Can only play a Wild Draw Four to stack
            if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                currentPlayer.playCard(cardIndex);
                discardPile.add(card);
                drawFourCounter += 4;
                System.out.println("Draw Four counter increased to: " + drawFourCounter);
                moveToNextPlayer();
                return true;
            } else {
                System.out.println("Cannot play " + card + " when there's a Draw Four stack. Must play a Wild Draw Four or draw cards.");
                return false;
            }
        }
        
        // Next, check if there's a pending Draw Two stack
        if (drawTwoCounter > 0) {
            // Can only play a Draw Two to stack
            if (card.getType() == Card.Type.DRAW_TWO) {
                currentPlayer.playCard(cardIndex);
                discardPile.add(card);
                drawTwoCounter += 2;
                System.out.println("Draw Two counter increased to: " + drawTwoCounter);
                moveToNextPlayer();
                return true;
            } else {
                System.out.println("Cannot play " + card + " when there's a Draw Two stack. Must play a Draw Two or draw cards.");
                return false;
            }
        }
        
        // Regular card play logic
        boolean canPlay = false;
        
        // If the top card is a wild card, match by current color
        if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
            // For wild cards, we need to check if the card matches the current color
            if (card.getColor() == currentColor || card.getColor() == Card.Color.WILD) {
                canPlay = true;
                System.out.println("Card can be played because it matches the current color: " + currentColor);
            } else {
                System.out.println("Card color " + card.getColor() + " doesn't match current color " + currentColor);
            }
        } else {
            // Regular card playability check
            canPlay = card.canBePlayedOn(topCard);
        }
        
        // If the card can't be played, return false
        if (!canPlay) {
            System.out.println("Cannot play " + card + " on " + topCard + ". Current color: " + currentColor);
            return false;
        }
        
        // Special rule for Wild Draw Four - can only be played if no other valid card
        // Skip this check if there's a Draw Four stack already (for stacking Wild Draw Four)
        if (card.getType() == Card.Type.WILD_DRAW_FOUR && drawFourCounter == 0) {
            if (hasValidCardOtherThanWildDrawFour(currentPlayer, topCard)) {
                System.out.println("Cannot play Wild Draw Four when you have other valid cards to play.");
                return false;
            }
            else {
                return true;
            }
        }
        
        // Play the card
        currentPlayer.playCard(cardIndex);
        discardPile.add(card);
        
        // Update current color for non-wild cards
        if (card.getColor() != Card.Color.WILD) {
            currentColor = card.getColor();
            System.out.println("Current color updated to: " + currentColor);
        }
        
        // Check if player has won
        if (currentPlayer.hasWon()) {
            isGameOver = true;
            return true;
        }
        
        // Handle action cards
        handleActionCard(card);
        
        return true;
    }
    
    // Check if player has a valid card other than Wild Draw Four
    private boolean hasValidCardOtherThanWildDrawFour(Player player, Card topCard) {
        for (Card card : player.getHand()) {
            if (card.getType() != Card.Type.WILD_DRAW_FOUR) {
                if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                    // For wild cards, check against current color
                    if (card.getColor() == currentColor || card.getColor() == Card.Color.WILD) {
                        return true;
                    }
                } else if (card.canBePlayedOn(topCard)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Get the index of the next player based on current direction
    private int getNextPlayerIndex() {
        if (isClockwise) {
            return (currentPlayerIndex + 1) % players.size();
        } else {
            return (currentPlayerIndex - 1 + players.size()) % players.size();
        }
    }
    
    // Handle action cards effects
    private void handleActionCard(Card card) {
        switch (card.getType()) {
            case SKIP:
                // Get the name of the player being skipped for logging
                String skippedPlayerName = players.get(getNextPlayerIndex()).getName();
                System.out.println("Skipping player " + skippedPlayerName);
                
                // Need to move TWICE to actually skip the next player
                moveToNextPlayer(); // Move to the next player
                moveToNextPlayer(); // Skip to the player after that
                System.out.println("After skip, turn goes to " + players.get(currentPlayerIndex).getName());
                break;
                
            case REVERSE:
                isClockwise = !isClockwise; // Change direction
                System.out.println("Direction changed. Now " + (isClockwise ? "clockwise" : "counter-clockwise"));
                
                // After reversing, move to the next player in the NEW direction
                // This ensures the player who played the reverse doesn't get another turn
                moveToNextPlayer();
                
                // In a 2-player game, reverse should effectively act as a skip
                // which is already handled by the moveToNextPlayer() call above
                break;
                
            case DRAW_TWO:
                drawTwoCounter += 2;
                System.out.println("Draw Two counter set to: " + drawTwoCounter);
                moveToNextPlayer();
                break;
                
            case WILD:
                // Color will be chosen separately
                break;
                
            case WILD_DRAW_FOUR:
                // Update the drawFourCounter instead of directly making the next player draw
                drawFourCounter += 4;
                System.out.println("Draw Four counter set to: " + drawFourCounter);
                moveToNextPlayer();
                break;
                
            case NUMBER:
                // For number cards, simply move to the next player
                moveToNextPlayer();
                break;
                
            default:
                moveToNextPlayer(); // Default behavior: move to next player
                break;
        }
    }
    
    // Method to choose color for wild cards
    public void chooseWildColor(Card.Color color) {
        currentColor = color;
        System.out.println("Wild color set to: " + color);
        // Do not move to next player here as it's handled elsewhere
    }
    
    // Method to handle draw two stacking
    public void handleDrawTwoStack() {
        if (drawTwoCounter > 0) {
            Player currentPlayer = players.get(currentPlayerIndex);
            boolean hasDrawTwo = false;
            int drawTwoIndex = -1;
            
            // Check if player has a Draw Two card
            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                if (card.getType() == Card.Type.DRAW_TWO) {
                    hasDrawTwo = true;
                    drawTwoIndex = i;
                    break;
                }
            }
            
            if (hasDrawTwo && !currentPlayer.isHuman()) {
                // CPU player has a Draw Two card and can stack - always stack for CPU
                playCard(drawTwoIndex);
            } else if (hasDrawTwo && currentPlayer.isHuman()) {
                // Human player has Draw Two card - decision to stack is made through the UI
                // We do nothing here, they will either play the card or draw manually
                System.out.println("Human player has Draw Two and can choose to stack it or draw " + drawTwoCounter + " cards.");
            } else {
                // Player must draw cards and skip turn
                System.out.println("Player " + currentPlayer.getName() + " must draw " + drawTwoCounter + " cards!");
                for (int i = 0; i < drawTwoCounter; i++) {
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                }
                drawTwoCounter = 0; // Reset counter
                System.out.println("Draw Two counter reset to 0");
                moveToNextPlayer(); // Skip turn
            }
        }
    }
    
    // Method to handle Wild Draw Four stacking
    public void handleDrawFourStack() {
        if (drawFourCounter > 0) {
            Player currentPlayer = players.get(currentPlayerIndex);
            System.out.println("Player " + currentPlayer.getName() + " has drawFourCounter = " + drawFourCounter);
            boolean hasDrawFour = false;
            int drawFourIndex = -1;
            
            // Check if player has a Wild Draw Four card
            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                    hasDrawFour = true;
                    drawFourIndex = i;
                    break;
                }
            }
            
            if (hasDrawFour && !currentPlayer.isHuman()) {
                // CPU player has a Wild Draw Four card and can stack - always stack for CPU
                System.out.println("CPU " + currentPlayer.getName() + " stacking Wild Draw Four!");
                playCard(drawFourIndex);
            } else if (hasDrawFour && currentPlayer.isHuman()) {
                // Human player has Wild Draw Four card - decision to stack is made through the UI
                // We do nothing here, they will either play the card or draw manually
                System.out.println("Human player has Wild Draw Four and can choose to stack it or draw " + drawFourCounter + " cards.");
            } else {
                // Player must draw cards and skip turn
                System.out.println("Player " + currentPlayer.getName() + " must draw " + drawFourCounter + " cards and skip turn!");
                for (int i = 0; i < drawFourCounter; i++) {
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                    System.out.println("Player " + currentPlayer.getName() + " drew card: " + drawnCard);
                }
                
                // Reset counter - IMPORTANT: this clears the stack so next player can play color
                System.out.println("Draw Four counter reset from " + drawFourCounter + " to 0");
                drawFourCounter = 0;
                
                // Don't change the current color - it should remain what was set by the Wild Draw Four
                System.out.println("After drawing penalty cards, current color remains: " + currentColor);
                
                moveToNextPlayer(); // Skip turn
            }
        }
    }
    
    // Method to handle Wild Draw Four challenge in multiplayer
    public void challengeWildDrawFour(boolean isChallenge) {
        if (isChallengeActive) {
            Player previousPlayer = getPreviousPlayer();
            Player currentPlayer = players.get(currentPlayerIndex);
            
            if (isChallenge) {
                // Check if challenge is valid (did previous player have a matching color)
                boolean hasMatch = previousPlayer.hasColorMatch(getSecondTopCard().getColor());
                
                if (hasMatch) {
                    // Challenge successful - previous player draws 4 cards
                    for (int i = 0; i < 4; i++) {
                        previousPlayer.addCard(deck.drawCard());
                    }
                } else {
                    // Challenge failed - current player draws 6 cards
                    for (int i = 0; i < 6; i++) {
                        currentPlayer.addCard(deck.drawCard());
                    }
                    moveToNextPlayer(); // Skip turn
                }
            } else {
                // No challenge - current player draws 4 cards
                // We now use the drawFourCounter instead of direct drawing
                drawFourCounter += 4;
                handleDrawFourStack();
            }
            
            isChallengeActive = false;
        }
    }
    
    // Method to draw a card for the current player
    public Card drawCard() {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        // Check if there are stacked Draw Four cards
        if (drawFourCounter > 0) {
            System.out.println("Player " + currentPlayer.getName() + " must draw " + drawFourCounter + " cards!");
            for (int i = 0; i < drawFourCounter; i++) {
                Card drawnCard = deck.drawCard();
                if (drawnCard == null) {
                    reshuffleDeck();
                    drawnCard = deck.drawCard();
                }
                currentPlayer.addCard(drawnCard);
                System.out.println("Drew card: " + drawnCard);
            }
            drawFourCounter = 0; // Reset counter
            System.out.println("Draw Four counter reset to 0");
            moveToNextPlayer();
            return null;
        }
        
        // Check if there are stacked Draw Two cards
        if (drawTwoCounter > 0) {
            System.out.println("Player " + currentPlayer.getName() + " must draw " + drawTwoCounter + " cards!");
            for (int i = 0; i < drawTwoCounter; i++) {
                Card drawnCard = deck.drawCard();
                if (drawnCard == null) {
                    reshuffleDeck();
                    drawnCard = deck.drawCard();
                }
                currentPlayer.addCard(drawnCard);
                System.out.println("Drew card: " + drawnCard);
            }
            drawTwoCounter = 0; // Reset counter
            System.out.println("Draw Two counter reset to 0");
            moveToNextPlayer();
            return null;
        }
        
        // Regular draw
        Card card = deck.drawCard();
        
        // If deck is empty, reshuffle discard pile except top card
        if (card == null) {
            reshuffleDeck();
            card = deck.drawCard();
        }
        
        currentPlayer.addCard(card);
        System.out.println("Player " + currentPlayer.getName() + " drew: " + card);
        
        // Check if drawn card can be played
        Card topCard = getTopCard();
        if (card.canBePlayedOn(topCard)) {
            return card;
        } else {
            moveToNextPlayer();
            return null;
        }
    }
    
    // Helper method to reshuffle the deck when it's empty
    private void reshuffleDeck() {
        if (discardPile.size() > 1) { // Keep at least the top card
            Card topCard = discardPile.remove(discardPile.size() - 1);
            deck = new Deck();
            deck.getCards().clear();
            for (Card c : discardPile) {
                deck.addCard(c);
            }
            discardPile.clear();
            discardPile.add(topCard);
            deck.shuffle();
        }
    }
    
    // Method to move to the next player and handle related logic
    public void moveToNextPlayer() {
        // Move to the next player based on direction
        if (isClockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex + players.size() - 1) % players.size();
        }
        
        // If there's a draw four penalty, current player must draw cards
        if (drawFourCounter > 0) {
            handleDrawFourStack();
        }
        
        System.out.println("Moving to next player: " + currentPlayerIndex + " (" + players.get(currentPlayerIndex).getName() + ")");
    }
    
    // Method to get the current top card on the discard pile
    public Card getTopCard() {
        if (discardPile.isEmpty()) {
            return null;
        }
        return discardPile.get(discardPile.size() - 1);
    }
    
    // Method to get the second card from the top of the discard pile (for challenges)
    private Card getSecondTopCard() {
        if (discardPile.size() < 2) {
            return null;
        }
        return discardPile.get(discardPile.size() - 2);
    }
    
    // Method to get the previous player
    private Player getPreviousPlayer() {
        int prevIndex;
        if (isClockwise) {
            prevIndex = (currentPlayerIndex + players.size() - 1) % players.size();
        } else {
            prevIndex = (currentPlayerIndex + 1) % players.size();
        }
        return players.get(prevIndex);
    }
    
    // Method for CPU to play their turn
    public boolean playCpuTurn() {
        Player currentPlayer = players.get(currentPlayerIndex);
        boolean playedSuccessfully = false;
        
        // Make sure it's a CPU player's turn
        if (currentPlayerIndex == 0) {
            System.out.println("Not a CPU player's turn!");
            return false;
        }

        // For debugging - show CPU's hand
        System.out.println("CPU " + currentPlayer.getName() + " hand: " + currentPlayer.getHand());
        System.out.println("Top card: " + getTopCard() + ", Current color: " + currentColor);
        
        try {
            // Check for any Draw Two stacking
            if (drawTwoCounter > 0) {
                // Draw cards and move to next player
                System.out.println("CPU must draw " + drawTwoCounter + " cards due to Draw Two stack");
                for (int i = 0; i < drawTwoCounter; i++) {
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                }
                drawTwoCounter = 0; // Reset the counter
                moveToNextPlayer();
                return true;
            }
            
            // Check for any Wild Draw Four stacking
            if (drawFourCounter > 0) {
                // Draw cards and move to next player
                System.out.println("CPU must draw " + drawFourCounter + " cards due to Draw Four stack");
                for (int i = 0; i < drawFourCounter; i++) {
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                }
                drawFourCounter = 0; // Reset the counter
                moveToNextPlayer();
                return true;
            }
            
            Card topCard = getTopCard();
            Card cardToPlay = null;
            int cardIndex = -1;
            
            // First check if we have any non-Wild-Draw-Four cards that match the current color or top card
            // This helps us avoid the illegal Wild Draw Four play when we have other valid options
            boolean hasMatchingCard = false;
            
            // First pass: check if we have any matching cards that aren't Wild Draw Four
            for (Card card : currentPlayer.getHand()) {
                boolean matches;
                if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                    // For wild cards on top, check if we have a card matching the chosen color
                    matches = (card.getColor() == currentColor);
                } else {
                    // For regular cards, check normal matching rules (color or value)
                    matches = (card.getColor() == topCard.getColor() || card.getType() == topCard.getType());
                }
                
                // If we found any matching card (except Wild Draw Four), remember this for later
                if (matches && card.getType() != Card.Type.WILD_DRAW_FOUR) {
                    hasMatchingCard = true;
                    System.out.println("CPU has matching card: " + card + ", cannot play Wild Draw Four");
                    break;
                }
            }
            
            // Now find the best card to play based on our hand
            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                
                // Special handling for wild cards on top
                boolean isPlayable;
                if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR && drawFourCounter == 0) {
                    // Match against current color for wild cards
                    isPlayable = (card.getColor() == currentColor || card.getColor() == Card.Color.WILD);
                } else {
                    // Regular card matching
                    isPlayable = card.canBePlayedOn(topCard);
                }
                
                // Skip Wild Draw Four cards if we have other matching cards
                if (card.getType() == Card.Type.WILD_DRAW_FOUR && hasMatchingCard) {
                    continue; // Skip Wild Draw Four if we have other valid cards
                }
                
                if (isPlayable) {
                    cardToPlay = card;
                    cardIndex = i;
                    break;
                }
            }
            
            if (cardToPlay != null) {
                System.out.println("CPU played: " + cardToPlay);
                
                // Extra safety check for Wild Draw Four
                if (cardToPlay.getType() == Card.Type.WILD_DRAW_FOUR && hasMatchingCard) {
                    System.out.println("ERROR: CPU attempted to play Wild Draw Four with matching cards in hand!");
                    // Force CPU to draw instead
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                    System.out.println("CPU drew a card instead: " + drawnCard);
                    moveToNextPlayer();
                    return true;
                }
                
                // Play the card using the playCard method to ensure proper game updates
                playedSuccessfully = playCard(cardIndex);
                
                // If a wild card was played, choose a color
                if (playedSuccessfully && (cardToPlay.getType() == Card.Type.WILD || cardToPlay.getType() == Card.Type.WILD_DRAW_FOUR)) {
                    Card.Color chosenColor = chooseBestColorForCpu(currentPlayer);
                    currentColor = chosenColor;
                    System.out.println("CPU chose color: " + chosenColor);
                }
            } else {
                // No valid card, draw one
                System.out.println("CPU has no valid cards, drawing...");
                Card drawnCard = deck.drawCard();
                
                if (drawnCard == null) {
                    reshuffleDeck();
                    drawnCard = deck.drawCard();
                }
                
                System.out.println("CPU drew: " + drawnCard);
                currentPlayer.addCard(drawnCard);
                System.out.println("Player " + currentPlayer.getName() + " has " + currentPlayer.getHand().size() + " cards");
                
                // Special handling for wild cards on top when checking if drawn card is playable
                boolean canPlayDrawnCard;
                if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                    // Match against current color for wild cards
                    canPlayDrawnCard = (drawnCard.getColor() == currentColor || drawnCard.getColor() == Card.Color.WILD);
                } else {
                    // Regular card matching
                    canPlayDrawnCard = drawnCard.canBePlayedOn(topCard);
                }
                
                // Check if the drawn card can be played
                if (canPlayDrawnCard) {
                    System.out.println("CPU can play the drawn card: " + drawnCard);
                    cardIndex = currentPlayer.getHand().size() - 1;
                    
                    // Play the drawn card using playCard method for consistent game updates
                    playedSuccessfully = playCard(cardIndex);
                    
                    // If a wild card was played, choose a color
                    if (playedSuccessfully && (drawnCard.getType() == Card.Type.WILD || drawnCard.getType() == Card.Type.WILD_DRAW_FOUR)) {
                        Card.Color chosenColor = chooseBestColorForCpu(currentPlayer);
                        currentColor = chosenColor;
                        System.out.println("CPU chose color for drawn card: " + chosenColor);
                    }
                } else {
                    System.out.println("CPU cannot play the drawn card, moving to next player");
                    moveToNextPlayer();
                }
            }
            
            // Check for game over
            if (currentPlayer.getHand().isEmpty()) {
                isGameOver = true;
                System.out.println("CPU " + currentPlayer.getName() + " has won the game!");
            }
            
            return playedSuccessfully;
        } catch (Exception e) {
            System.err.println("Error during CPU turn: " + e.getMessage());
            e.printStackTrace();
            moveToNextPlayer(); // Ensure the game continues even if there's an error
            return false;
        }
    }
    
    // Helper method to choose the best color for CPU based on their hand
    private Card.Color chooseBestColorForCpu(Player cpuPlayer) {
        int[] colorCounts = new int[4]; // RED, YELLOW, GREEN, BLUE
        
        // Count the number of each color in CPU's hand
        for (Card card : cpuPlayer.getHand()) {
            if (card.getColor() == Card.Color.RED) {
                colorCounts[0]++;
            } else if (card.getColor() == Card.Color.YELLOW) {
                colorCounts[1]++;
            } else if (card.getColor() == Card.Color.GREEN) {
                colorCounts[2]++;
            } else if (card.getColor() == Card.Color.BLUE) {
                colorCounts[3]++;
            }
        }
        
        // Find the most common color
        int maxCount = -1;
        int maxIndex = 0;
        
        for (int i = 0; i < colorCounts.length; i++) {
            if (colorCounts[i] > maxCount) {
                maxCount = colorCounts[i];
                maxIndex = i;
            }
        }
        
        // If CPU has no colored cards, pick a random color
        if (maxCount == 0) {
            Random random = new Random();
            maxIndex = random.nextInt(4);
        }
        
        // Convert index to color
        switch (maxIndex) {
            case 0: return Card.Color.RED;
            case 1: return Card.Color.YELLOW;
            case 2: return Card.Color.GREEN;
            default: return Card.Color.BLUE;
        }
    }
    
    // Getters and setters
    public List<Player> getPlayers() {
        return players;
    }
    
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public Card.Color getCurrentColor() {
        return currentColor;
    }
    
    public void setCurrentColor(Card.Color color) {
        this.currentColor = color;
        System.out.println("Color set to: " + color);
    }
    
    public boolean isClockwise() {
        return isClockwise;
    }
    
    public boolean isGameOver() {
        return isGameOver;
    }
    
    public int getDrawTwoCounter() {
        return drawTwoCounter;
    }
    
    public int getDrawFourCounter() {
        return drawFourCounter;
    }
    
    public boolean isChallengeActive() {
        return isChallengeActive;
    }
    
    public void resetDrawFourCounter() {
        this.drawFourCounter = 0;
        System.out.println("Draw Four counter reset to 0");
    }
    
    public void resetDrawTwoCounter() {
        this.drawTwoCounter = 0;
        System.out.println("Draw Two counter reset to 0");
    }
    
    // Method to increment drawFourCounter (for UI control)
    public void incrementDrawFourCounter(int amount) {
        this.drawFourCounter += amount;
        System.out.println("Draw Four counter increased by " + amount + " to " + this.drawFourCounter);
    }
    
    // Make deck accessible for direct draw operations
    public Deck getDeck() {
        return deck;
    }
}