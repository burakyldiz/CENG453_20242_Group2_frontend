package com.ceng453.backend.dto;

/**
 * Data Transfer Object for player information sent from backend to frontend
 */
public class PlayerDTO {
    private Long userId;
    private String username;
    private Integer handSize;
    private Boolean saidUno;
    
    // Default constructor for JSON deserialization
    public PlayerDTO() {}
    
    public PlayerDTO(Long userId, String username, Integer handSize, Boolean saidUno) {
        this.userId = userId;
        this.username = username;
        this.handSize = handSize;
        this.saidUno = saidUno;
    }
    
    // Getters and setters
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
    
    public Integer getHandSize() {
        return handSize;
    }
    
    public void setHandSize(Integer handSize) {
        this.handSize = handSize;
    }
    
    public Boolean getSaidUno() {
        return saidUno;
    }
    
    public void setSaidUno(Boolean saidUno) {
        this.saidUno = saidUno;
    }
}
