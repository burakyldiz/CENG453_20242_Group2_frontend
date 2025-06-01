package com.ceng453.frontend.dto;

public class PlayerActionDTO {
    private Long playerId;
    private String actionType;
    private CardDTO cardDetails;
    private Long targetPlayerId;
    private String chosenColor;

    // Default constructor
    public PlayerActionDTO() {
    }

    // Constructor with parameters
    public PlayerActionDTO(Long playerId, String actionType) {
        this.playerId = playerId;
        this.actionType = actionType;
    }

    // Getters and Setters
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

    public CardDTO getCardDetails() {
        return cardDetails;
    }

    public void setCardDetails(CardDTO cardDetails) {
        this.cardDetails = cardDetails;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public String getChosenColor() {
        return chosenColor;
    }

    public void setChosenColor(String chosenColor) {
        this.chosenColor = chosenColor;
    }
}
