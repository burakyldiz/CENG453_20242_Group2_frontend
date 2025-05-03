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
        if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
            if (hasValidCardOtherThanWildDrawFour(currentPlayer, topCard)) {
                System.out.println("Cannot play Wild Draw Four when you have other valid cards to play.");
                return false;
            }
        }
        
        // Play the card
        currentPlayer.playCard(cardIndex);
        discardPile.add(card);
        
        // Update current color for non-wild cards
        if (card.getColor() != Card.Color.WILD) {
            currentColor = card.getColor();
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
            if (card.getType() != Card.Type.WILD_DRAW_FOUR && card.canBePlayedOn(topCard)) {
                return true;
            }
        }
        return false;
    }
    
    // Handle action cards effects
    private void handleActionCard(Card card) {
        switch (card.getType()) {
            case SKIP:
                moveToNextPlayer(); // Skip the next player
                break;
                
            case REVERSE:
                isClockwise = !isClockwise; // Change direction
                // In a two-player game, reverse acts like skip
                if (players.size() == 2) {
                    moveToNextPlayer();
                }
                moveToNextPlayer(); // Move to next player after reversing
                break;
                
            case DRAW_TWO:
                drawTwoCounter += 2;
                moveToNextPlayer();
                break;
                
            case WILD:
                // Color will be chosen separately
                break;
                
            case WILD_DRAW_FOUR:
                // Update the drawFourCounter instead of directly making the next player draw
                drawFourCounter += 4;
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
        moveToNextPlayer();
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
                moveToNextPlayer(); // Skip turn
            }
        }
    }
    
    // Method to handle Wild Draw Four stacking
    public void handleDrawFourStack() {
        if (drawFourCounter > 0) {
            Player currentPlayer = players.get(currentPlayerIndex);
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
                playCard(drawFourIndex);
            } else if (hasDrawFour && currentPlayer.isHuman()) {
                // Human player has Wild Draw Four card - decision to stack is made through the UI
                // We do nothing here, they will either play the card or draw manually
                System.out.println("Human player has Wild Draw Four and can choose to stack it or draw " + drawFourCounter + " cards.");
            } else {
                // Player must draw cards and skip turn
                System.out.println("Player " + currentPlayer.getName() + " must draw " + drawFourCounter + " cards!");
                for (int i = 0; i < drawFourCounter; i++) {
                    Card drawnCard = deck.drawCard();
                    if (drawnCard == null) {
                        reshuffleDeck();
                        drawnCard = deck.drawCard();
                    }
                    currentPlayer.addCard(drawnCard);
                }
                drawFourCounter = 0; // Reset counter - IMPORTANT: this clears the stack so next player can play color
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
                currentPlayer.addCard(deck.drawCard());
            }
            drawFourCounter = 0; // Reset counter
            moveToNextPlayer();
            return null;
        }
        
        // Check if there are stacked Draw Two cards
        if (drawTwoCounter > 0) {
            System.out.println("Player " + currentPlayer.getName() + " must draw " + drawTwoCounter + " cards!");
            for (int i = 0; i < drawTwoCounter; i++) {
                currentPlayer.addCard(deck.drawCard());
            }
            drawTwoCounter = 0; // Reset counter
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
    
    // Method to move to the next player's turn
    public void moveToNextPlayer() {
        if (isClockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex + players.size() - 1) % players.size();
        }
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
            
            // Find a valid card to play (but don't play it yet)
            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                if (card.canBePlayedOn(topCard)) {
                    cardToPlay = card;
                    cardIndex = i;
                    break;
                }
            }
            
            if (cardToPlay != null) {
                System.out.println("CPU played: " + cardToPlay);
                
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
                
                currentPlayer.addCard(drawnCard);
                
                // Check if the drawn card can be played
                if (drawnCard.canBePlayedOn(topCard)) {
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
    }
    
    public void resetDrawTwoCounter() {
        this.drawTwoCounter = 0;
    }
    
    // Make deck accessible for direct draw operations
    public Deck getDeck() {
        return deck;
    }
}
