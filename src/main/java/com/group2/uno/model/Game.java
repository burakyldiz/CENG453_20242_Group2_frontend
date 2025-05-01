package com.group2.uno.model;

import com.group2.uno.model.enums.CardColor;
import com.group2.uno.model.enums.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Game {
    private final List<Player> players;
    private final List<Card> deck;
    private final List<Card> discardPile;
    private int currentPlayerIndex;
    private boolean isReversed;
    private CardColor currentColor;
    private int drawCount; // For stacking Draw Two cards
    
    private final Random random = new Random();
    
    public Game(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.deck = createDeck();
        this.discardPile = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.isReversed = false;
        this.drawCount = 0;
    }
    
    private List<Card> createDeck() {
        List<Card> newDeck = new ArrayList<>();
        
        for (CardColor color : CardColor.values()) {
            if (color == CardColor.WILD) continue;
            
            newDeck.add(new Card(color, CardType.NUMBER, 0));
            
            for (int number = 1; number <= 9; number++) {
                newDeck.add(new Card(color, CardType.NUMBER, number));
                newDeck.add(new Card(color, CardType.NUMBER, number));
            }
            
            for (int i = 0; i < 2; i++) {
                newDeck.add(new Card(color, CardType.SKIP, 0));
                newDeck.add(new Card(color, CardType.REVERSE, 0));
                newDeck.add(new Card(color, CardType.DRAW_TWO, 0));
            }
        }
        
        for (int i = 0; i < 4; i++) {
            newDeck.add(new Card(CardColor.WILD, CardType.WILD, 0));
            newDeck.add(new Card(CardColor.WILD, CardType.WILD_DRAW_FOUR, 0));
        }
        
        Collections.shuffle(newDeck);
        
        return newDeck;
    }
    
    public void initialize() {
        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                player.addCard(drawCard());
            }
        }
        
        Card firstCard;
        do {
            firstCard = drawCard();
        } while (firstCard.getType() == CardType.WILD_DRAW_FOUR);
        
        discardPile.add(firstCard);
        currentColor = firstCard.getColor();
        
        if (firstCard.getType() != CardType.NUMBER) {
            applyCardEffect(firstCard);
        }
    }
    
    public Card drawCard() {
        if (deck.isEmpty()) {
            reshuffleDeck();
        }
        
        return deck.remove(deck.size() - 1);
    }
    
    private void reshuffleDeck() {
        if (discardPile.isEmpty()) {
            return;
        }
        
        Card topCard = discardPile.remove(discardPile.size() - 1);
        
        deck.addAll(discardPile);
        discardPile.clear();
        
        Collections.shuffle(deck);
        
        discardPile.add(topCard);
    }
    
    public boolean isValidPlay(Card card) {
        if (card.getType() == CardType.WILD_DRAW_FOUR) {
            Player currentPlayer = getCurrentPlayer();
            boolean hasMatchingColor = currentPlayer.getHand().stream()
                    .anyMatch(c -> c.getColor() == currentColor && c.getType() != CardType.WILD_DRAW_FOUR);
            return !hasMatchingColor;
        }
        
        Card topCard = getTopCard();
        
        if (drawCount > 0) {
            return card.getType() == CardType.DRAW_TWO;
        }
        
        return card.canPlayOn(topCard) || card.getColor() == currentColor;
    }
    
    public void playCard(int cardIndex, CardColor chosenColor) {
        Player currentPlayer = getCurrentPlayer();
        Card card = currentPlayer.playCard(cardIndex);
        
        discardPile.add(card);
        
        if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
            currentColor = chosenColor;
        } else {
            currentColor = card.getColor();
        }
        
        applyCardEffect(card);
    }
    
    private void applyCardEffect(Card card) {
        switch (card.getType()) {
            case SKIP:
                advancePlayer();
                break;
                
            case REVERSE:
                isReversed = !isReversed;
                if (players.size() == 2) {
                    advancePlayer();
                }
                break;
                
            case DRAW_TWO:
                drawCount += 2;
                advancePlayer();
                break;
                
            case WILD_DRAW_FOUR:
                drawCount += 4;
                advancePlayer();
                break;
                
            default:
                advancePlayer();
                break;
        }
    }
    
    private void advancePlayer() {
        if (isReversed) {
            currentPlayerIndex--;
            if (currentPlayerIndex < 0) {
                currentPlayerIndex = players.size() - 1;
            }
        } else {
            currentPlayerIndex++;
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = 0;
            }
        }
    }
    
    public Card handleDraw() {
        Player currentPlayer = getCurrentPlayer();
        
        if (drawCount > 0) {
            for (int i = 0; i < drawCount; i++) {
                currentPlayer.addCard(drawCard());
            }
            drawCount = 0;
            advancePlayer();
            return null;
        } else {
            Card drawnCard = drawCard();
            currentPlayer.addCard(drawnCard);
            
            if (drawnCard.canPlayOn(getTopCard()) || drawnCard.getColor() == currentColor) {
                return drawnCard;
            } else {
                advancePlayer();
                return null;
            }
        }
    }
    
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    
    public Card getTopCard() {
        if (discardPile.isEmpty()) {
            return null;
        }
        return discardPile.get(discardPile.size() - 1);
    }
    
    public boolean isGameOver() {
        return players.stream().anyMatch(Player::hasWon);
    }
    
    public Player getWinner() {
        return players.stream()
                .filter(Player::hasWon)
                .findFirst()
                .orElse(null);
    }
    
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public boolean isReversed() {
        return isReversed;
    }
    
    public CardColor getCurrentColor() {
        return currentColor;
    }
    
    public int getCpuMove() {
        Player cpu = getCurrentPlayer();
        List<Card> validCards = cpu.getHand().stream()
                .filter(this::isValidPlay)
                .collect(Collectors.toList());
        
        if (validCards.isEmpty()) {
            return -1;
        }
        
        Card selectedCard = validCards.get(random.nextInt(validCards.size()));
        
        return cpu.getHand().indexOf(selectedCard);
    }
    
    public CardColor getRandomColor() {
        CardColor[] colors = {CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.YELLOW};
        return colors[random.nextInt(colors.length)];
    }
    
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }
    
    public int getDrawCount() {
        return drawCount;
    }
}