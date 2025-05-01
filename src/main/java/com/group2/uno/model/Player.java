package com.group2.uno.model;

import java.util.ArrayList;
import java.util.List;


public class Player {
    private final long id;
    private final String name;
    private final boolean isHuman; // Whether this player is controlled by a human
    private final List<Card> hand;
    private boolean hasCalledUno;
    
    public Player(long id, String name, boolean isHuman) {
        this.id = id;
        this.name = name;
        this.isHuman = isHuman;
        this.hand = new ArrayList<>();
        this.hasCalledUno = false;
    }
    
   
    public void addCard(Card card) {
        hand.add(card);
        hasCalledUno = false; // Reset UNO status when drawing a card
    }
    
    
    public Card playCard(int index) {
        if (index < 0 || index >= hand.size()) {
            throw new IllegalArgumentException("Invalid card index");
        }
        return hand.remove(index);
    }
    
    
    public List<Card> getValidCards(Card topCard) {
        List<Card> validCards = new ArrayList<>();
        
        for (Card card : hand) {
            if (card.canPlayOn(topCard)) {
                validCards.add(card);
            }
        }
        
        return validCards;
    }

    public void callUno() {
        hasCalledUno = true;
    }
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isHuman() {
        return isHuman;
    }
    
    public List<Card> getHand() {
        return new ArrayList<>(hand); // Return a copy to prevent modification
    }
    
    public int getHandSize() {
        return hand.size();
    }
    
    public boolean hasCalledUno() {
        return hasCalledUno;
    }
    
    public boolean hasWon() {
        return hand.isEmpty();
    }
}
