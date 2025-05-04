package com.ceng453.frontend.model;

/**
 * Adapter class to make our fallback Deck implementation compatible
 * with the actual Deck class. This helps with class loading issues.
 */
public class DeckAdapter extends Deck {
    private Game.DeckFallback fallbackDeck;
    
    public DeckAdapter(Game.DeckFallback fallbackDeck) {
        super(); // Call Deck's constructor but we'll override its methods
        this.fallbackDeck = fallbackDeck;
    }
    
    @Override
    public Card drawCard() {
        return fallbackDeck.drawCard();
    }
    
    @Override
    public void shuffle() {
        fallbackDeck.shuffle();
    }
    
    @Override
    public boolean isEmpty() {
        return fallbackDeck.isEmpty();
    }
    
    @Override
    public void removeWildCards() {
        fallbackDeck.removeWildCards();
    }
}
