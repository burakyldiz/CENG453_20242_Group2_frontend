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
    private boolean isGameOver;
    private boolean isChallengeActive; // For Wild Draw Four challenge in multiplayer
    
    public Game() {
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.discardPile = new ArrayList<>();
        this.isClockwise = true;
        this.currentPlayerIndex = 0;
        this.drawTwoCounter = 0;
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
        
        // Check if card can be played
        if (!card.canBePlayedOn(topCard) && card.getType() != Card.Type.WILD && card.getType() != Card.Type.WILD_DRAW_FOUR) {
            return false;
        }
        
        // Special rule for Wild Draw Four - can only be played if no other valid card
        if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
            if (hasValidCardOtherThanWildDrawFour(currentPlayer, topCard)) {
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
        
        // If the card was not an action card, move to the next player
        // Action cards (SKIP, REVERSE, DRAW_TWO, WILD_DRAW_FOUR) already handle turn advancement
        if (card.getType() == Card.Type.NUMBER) {
            moveToNextPlayer();
        }
        
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
                break;
                
            case DRAW_TWO:
                drawTwoCounter += 2;
                moveToNextPlayer();
                break;
                
            case WILD:
                // Color will be chosen separately
                break;
                
            case WILD_DRAW_FOUR:
                // In multiplayer, this could be challenged
                // For single player, just force the next player to draw 4
                if (players.get(currentPlayerIndex).isHuman()) {
                    // Challenge not implemented for CPU
                    Player nextPlayer = getNextPlayer();
                    for (int i = 0; i < 4; i++) {
                        nextPlayer.addCard(deck.drawCard());
                    }
                    moveToNextPlayer(); // Skip the next player's turn
                } else {
                    // For multiplayer, set challenge active
                    isChallengeActive = true;
                }
                break;
                
            default:
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
            Card drawTwo = null;
            
            // Check if player has a Draw Two card
            for (Card card : new ArrayList<>(currentPlayer.getHand())) {
                if (card.getType() == Card.Type.DRAW_TWO) {
                    drawTwo = card;
                    break;
                }
            }
            
            if (drawTwo != null) {
                // Player has a Draw Two card and can stack
                currentPlayer.getHand().remove(drawTwo);
                discardPile.add(drawTwo);
                drawTwoCounter += 2;
                moveToNextPlayer();
            } else {
                // Player must draw cards
                for (int i = 0; i < drawTwoCounter; i++) {
                    currentPlayer.addCard(deck.drawCard());
                }
                drawTwoCounter = 0; // Reset counter
                moveToNextPlayer();
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
                for (int i = 0; i < 4; i++) {
                    currentPlayer.addCard(deck.drawCard());
                }
                moveToNextPlayer(); // Skip turn
            }
            
            isChallengeActive = false;
        }
    }
    
    // Method to draw a card for the current player
    public Card drawCard() {
        Player currentPlayer = players.get(currentPlayerIndex);
        Card card = deck.drawCard();
        
        // If deck is empty, reshuffle discard pile except top card
        if (card == null) {
            Card topCard = discardPile.remove(discardPile.size() - 1);
            deck = new Deck();
            deck.getCards().clear();
            for (Card c : discardPile) {
                deck.addCard(c);
            }
            discardPile.clear();
            discardPile.add(topCard);
            deck.shuffle();
            
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
    
    // Method to get the next player (without changing turn)
    private Player getNextPlayer() {
        int nextIndex;
        if (isClockwise) {
            nextIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            nextIndex = (currentPlayerIndex + players.size() - 1) % players.size();
        }
        return players.get(nextIndex);
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
        
        if (!currentPlayer.isHuman()) {
            System.out.println("CPU " + currentPlayer.getName() + " is playing turn");
            
            // Check for any Draw Two stacking
            if (drawTwoCounter > 0) {
                handleDrawTwoStack();
                moveToNextPlayer(); // Advance to next player
                return true;
            }
            
            Card topCard = getTopCard();
            Card playedCard = currentPlayer.playAutomaticTurn(topCard);
            
            if (playedCard != null) {
                // CPU played a card successfully
                playedSuccessfully = true;
                System.out.println("CPU played: " + playedCard);
                discardPile.add(playedCard);
                
                // Update current color for non-wild cards
                if (playedCard.getColor() != Card.Color.WILD) {
                    currentColor = playedCard.getColor();
                } else {
                    // Choose a random color for wild cards
                    Card.Color[] colors = {Card.Color.RED, Card.Color.YELLOW, Card.Color.GREEN, Card.Color.BLUE};
                    currentColor = colors[new Random().nextInt(4)];
                }
                
                // Check if CPU has won
                if (currentPlayer.hasWon()) {
                    isGameOver = true;
                    return true;
                }
                
                // Handle action card effects (which will move to next player if needed)
                handleActionCard(playedCard);
                
                // If the action card was not a skip or reverse, manually advance to next player
                if (playedCard.getType() != Card.Type.SKIP && 
                    playedCard.getType() != Card.Type.REVERSE &&
                    playedCard.getType() != Card.Type.DRAW_TWO && 
                    playedCard.getType() != Card.Type.WILD_DRAW_FOUR) {
                    moveToNextPlayer();
                }
            } else {
                // CPU couldn't play, draw a card
                System.out.println("CPU drawing a card");
                Card drawnCard = drawCard();
                
                if (drawnCard != null && drawnCard.canBePlayedOn(topCard)) {
                    // Play the drawn card if possible
                    System.out.println("CPU playing drawn card: " + drawnCard);
                    int cardIndex = currentPlayer.getHand().indexOf(drawnCard);
                    if (cardIndex != -1) {
                        // The card will be played with the normal play logic
                        playCard(cardIndex);
                        playedSuccessfully = true;
                    }
                } else {
                    // CPU couldn't play after drawing, advance to next player
                    System.out.println("CPU couldn't play after drawing, passing");
                    moveToNextPlayer();
                }
            }
            
            System.out.println("CPU turn finished. Next player: " + currentPlayerIndex);
        }
        
        return playedSuccessfully;
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
    
    public boolean isClockwise() {
        return isClockwise;
    }
    
    public boolean isGameOver() {
        return isGameOver;
    }
    
    public int getDrawTwoCounter() {
        return drawTwoCounter;
    }
    
    public boolean isChallengeActive() {
        return isChallengeActive;
    }
}
