package com.ceng453.backend.dto;

/**
 * Data Transfer Object for join game requests
 */
public class JoinRequestDTO {
    private Long playerId;
    
    // Default constructor for JSON serialization/deserialization
    public JoinRequestDTO() {}
    
    public JoinRequestDTO(Long playerId) {
        this.playerId = playerId;
    }
    
    // Getters and setters
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
}
