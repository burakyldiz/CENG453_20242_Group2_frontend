package com.ceng453.frontend.dto;

import java.util.List;

public class PlayerDTO {
    private Long userId;
    private String username;
    private List<CardDTO> hand;
    private boolean saidUno;

    // Default constructor
    public PlayerDTO() {
    }

    // Constructor with parameters
    public PlayerDTO(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<CardDTO> getHand() {
        return hand;
    }
    
    public int getHandSize() {
        return hand != null ? hand.size() : 0;
    }

    public void setHand(List<CardDTO> hand) {
        this.hand = hand;
    }

    public boolean isSaidUno() {
        return saidUno;
    }

    public void setSaidUno(boolean saidUno) {
        this.saidUno = saidUno;
    }
}
