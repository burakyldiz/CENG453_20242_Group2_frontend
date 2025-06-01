package com.ceng453.frontend.dto;

import com.ceng453.frontend.model.Card;
import java.util.List;
import java.util.Map;

public class GameStateDTO {
    private String gameId;
    private Long currentTurnPlayerId;
    private Card lastPlayedCard;
    private Map<Long, Integer> playerCardCounts;
    private String gameDirection;
    private Map<Long, Boolean> unoDeclaredStatus;
    private List<Card> yourHand;
    private boolean gameOver;
    private Long winnerId;
    private List<String> gameMessages;
    private Long yourPlayerId;
    private List<PlayerDTO> players;
    private List<String> gameLog;

    // Default constructor
    public GameStateDTO() {
    }

    // Getters and Setters
    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Long getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }
    
    // Alias for getCurrentTurnPlayerId to match expected method name in controller
    public Long getCurrentPlayerId() {
        return currentTurnPlayerId;
    }

    public void setCurrentTurnPlayerId(Long currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
    }

    public Card getLastPlayedCard() {
        return lastPlayedCard;
    }
    
    // Alias for getLastPlayedCard to match expected method name in controller
    public Card getTopCard() {
        return lastPlayedCard;
    }

    public void setLastPlayedCard(Card lastPlayedCard) {
        this.lastPlayedCard = lastPlayedCard;
    }

    public Map<Long, Integer> getPlayerCardCounts() {
        return playerCardCounts;
    }

    public void setPlayerCardCounts(Map<Long, Integer> playerCardCounts) {
        this.playerCardCounts = playerCardCounts;
    }

    public String getGameDirection() {
        return gameDirection;
    }
    
    // Method to check if game direction is clockwise
    public boolean isGameDirectionClockwise() {
        return "CLOCKWISE".equalsIgnoreCase(gameDirection);
    }

    public void setGameDirection(String gameDirection) {
        this.gameDirection = gameDirection;
    }

    public Map<Long, Boolean> getUnoDeclaredStatus() {
        return unoDeclaredStatus;
    }

    public void setUnoStatus(Map<Long, Boolean> unoDeclaredStatus) {
        this.unoDeclaredStatus = unoDeclaredStatus;
    }

    public List<Card> getYourHand() {
        return yourHand;
    }
    
    // Alias for getYourHand to match expected method name in controller
    public List<Card> getPlayerHand() {
        return yourHand;
    }

    public void setYourHand(List<Card> yourHand) {
        this.yourHand = yourHand;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public List<String> getGameMessages() {
        return gameMessages;
    }

    public void setGameMessages(List<String> gameMessages) {
        this.gameMessages = gameMessages;
    }

    public Long getYourPlayerId() {
        return yourPlayerId;
    }

    public void setYourPlayerId(Long yourPlayerId) {
        this.yourPlayerId = yourPlayerId;
    }
    
    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }
    
    public List<String> getGameLog() {
        return gameLog;
    }

    public void setGameLog(List<String> gameLog) {
        this.gameLog = gameLog;
    }
}
