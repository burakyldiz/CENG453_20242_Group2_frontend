package com.ceng453.frontend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Player {
    private String name;
    private List<Card> hand;
    private boolean isHuman;
    private boolean hasCalledUno;
    
    public Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        this.hand = new ArrayList<>();
        this.hasCalledUno = false;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Card> getHand() {
        return hand;
    }
    
    public boolean isHuman() {
        return isHuman;
    }
    
    public boolean hasCalledUno() {
        return hasCalledUno;
    }
    
    public void setHasCalledUno(boolean hasCalledUno) {
        this.hasCalledUno = hasCalledUno;
    }
    
    public void addCard(Card card) {
        hand.add(card);
        // Reset UNO status when drawing cards
        if (hand.size() > 1) {
            hasCalledUno = false;
        }
    }
    
    public Card playCard(int index) {
        if (index < 0 || index >= hand.size()) {
            return null;
        }
        
        Card played = hand.remove(index);
        
        // Auto-detect UNO for CPU players
        if (!isHuman && hand.size() == 1) {
            hasCalledUno = true;
        }
        
        return played;
    }
    
    public boolean hasWon() {
        return hand.isEmpty();
    }
    
    public int getCardCount() {
        return hand.size();
    }
    
    // Method for CPU to play a card automatically
    public Card playAutomaticTurn(Card topCard) {
        if (!isHuman) {
            List<Integer> validCardIndices = new ArrayList<>();
            
            // Find all valid cards that can be played
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).canBePlayedOn(topCard)) {
                    validCardIndices.add(i);
                }
            }
            
            // If there are valid cards, play a random one
            if (!validCardIndices.isEmpty()) {
                Random random = new Random();
                int randomIndex = validCardIndices.get(random.nextInt(validCardIndices.size()));
                return playCard(randomIndex);
            }
        }
        
        // No valid card or not a CPU player
        return null;
    }
    
    // Find a valid card in hand
    public Card findValidCard(Card topCard) {
        for (Card card : hand) {
            if (card.canBePlayedOn(topCard)) {
                return card;
            }
        }
        return null;
    }
    
    // Check if player has a card matching the current color
    public boolean hasColorMatch(Card.Color color) {
        for (Card card : hand) {
            if (card.getColor() == color) {
                return true;
            }
        }
        return false;
    }
    
    // Call UNO when having one card left
    public void callUno() {
        if (hand.size() == 1) {
            hasCalledUno = true;
        }
    }
}
