package com.ceng453.backend.dto;

/**
 * Data Transfer Object for player actions sent from frontend to backend
 */
public class PlayerActionDTO {
    private Long playerId;
    private String actionType;  // PLAY_CARD, DRAW_CARD, CHALLENGE_WILD_DRAW_FOUR, CHALLENGE_NO_UNO, DECLARE_UNO
    private String cardDetails;  // Used for PLAY_CARD, format: COLOR_VALUE or WILD_VALUE_CHOSENCOLOR
    private Long targetPlayerId;  // Used for CHALLENGE_NO_UNO
    
    // Default constructor for JSON serialization/deserialization
    public PlayerActionDTO() {}
    
    public PlayerActionDTO(Long playerId, String actionType) {
        this.playerId = playerId;
        this.actionType = actionType;
    }
    
    public PlayerActionDTO(Long playerId, String actionType, String cardDetails) {
        this.playerId = playerId;
        this.actionType = actionType;
        this.cardDetails = cardDetails;
    }
    
    public PlayerActionDTO(Long playerId, String actionType, Long targetPlayerId) {
        this.playerId = playerId;
        this.actionType = actionType;
        this.targetPlayerId = targetPlayerId;
    }
    
    // Getters and setters
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getCardDetails() {
        return cardDetails;
    }
    
    public void setCardDetails(String cardDetails) {
        this.cardDetails = cardDetails;
    }
    
    public Long getTargetPlayerId() {
        return targetPlayerId;
    }
    
    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}
