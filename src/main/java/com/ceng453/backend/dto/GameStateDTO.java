package com.ceng453.backend.dto;

import java.util.List;

/**
 * Data Transfer Object for game state information sent from backend to frontend
 */
public class GameStateDTO {
    private String gameId;
    private boolean gameStarted;
    private boolean gameOver;
    private Long winnerId;
    private Long currentPlayerId;
    private boolean gameDirectionClockwise;
    private CardDTO topCard;
    private Integer deckSize;
    private List<PlayerDTO> players;
    private List<CardDTO> playerHand;
    private Boolean saidUno;
    private List<String> gameLog;

    // Default constructor for JSON deserialization
    public GameStateDTO() {}

    // Getters and setters
    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
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

    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public boolean isGameDirectionClockwise() {
        return gameDirectionClockwise;
    }

    public void setGameDirectionClockwise(boolean gameDirectionClockwise) {
        this.gameDirectionClockwise = gameDirectionClockwise;
    }

    public CardDTO getTopCard() {
        return topCard;
    }

    public void setTopCard(CardDTO topCard) {
        this.topCard = topCard;
    }

    public Integer getDeckSize() {
        return deckSize;
    }

    public void setDeckSize(Integer deckSize) {
        this.deckSize = deckSize;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public List<CardDTO> getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(List<CardDTO> playerHand) {
        this.playerHand = playerHand;
    }

    public Boolean getSaidUno() {
        return saidUno;
    }

    public void setSaidUno(Boolean saidUno) {
        this.saidUno = saidUno;
    }

    public List<String> getGameLog() {
        return gameLog;
    }

    public void setGameLog(List<String> gameLog) {
        this.gameLog = gameLog;
    }
}
