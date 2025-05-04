package com.ceng453.frontend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> cards;
    
    public Deck() {
        cards = new ArrayList<>();
        initializeDeck();
    }
    
    // Initialize a standard UNO deck
    private void initializeDeck() {
        // Add number cards (0-9) for each color
        for (Card.Color color : new Card.Color[]{Card.Color.RED, Card.Color.YELLOW, Card.Color.GREEN, Card.Color.BLUE}) {
            // Add one zero card per color
            cards.add(new Card(color, 0));
            
            // Add two of each number card 1-9 per color
            for (int i = 1; i <= 9; i++) {
                cards.add(new Card(color, i));
                cards.add(new Card(color, i));
            }
            
            // Add action cards (two of each per color)
            for (int i = 0; i < 2; i++) {
                cards.add(new Card(color, Card.Type.SKIP));
                cards.add(new Card(color, Card.Type.REVERSE));
                cards.add(new Card(color, Card.Type.DRAW_TWO));
            }
        }
        
        // Add wild cards (4 of each type)
        for (int i = 0; i < 4; i++) {
            cards.add(new Card(Card.Color.WILD, Card.Type.WILD));
            cards.add(new Card(Card.Color.WILD, Card.Type.WILD_DRAW_FOUR));
        }
    }
    
    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    public Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }
    
    public void addCard(Card card) {
        cards.add(card);
    }
    
    public List<Card> getCards() {
        return cards;
    }
    
    public int size() {
        return cards.size();
    }
    
    public boolean isEmpty() {
        return cards.isEmpty();
    }
    
    // Method to draw initial card to start the game (should not be wild draw four)
    public Card drawInitialCard() {
        shuffle(); // Make sure deck is shuffled
        
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card.getType() != Card.Type.WILD_DRAW_FOUR) {
                cards.remove(i);
                return card;
            }
        }
        
        // Fallback in case all cards are Wild Draw Four (extremely unlikely)
        return drawCard();
    }
    
    // Method to filter out wild cards from the deck
    public void removeWildCards() {
        List<Card> filteredCards = new ArrayList<>();
        
        for (Card card : cards) {
            if (card.getType() != Card.Type.WILD && card.getType() != Card.Type.WILD_DRAW_FOUR) {
                filteredCards.add(card);
            }
        }
        
        this.cards = filteredCards;
        System.out.println("Wild cards have been removed from the deck. New deck size: " + cards.size());
    }
}
