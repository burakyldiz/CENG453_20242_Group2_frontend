package com.ceng453.backend.dto;

/**
 * Data Transfer Object for card information sent from backend to frontend
 */
public class CardDTO {
    private String color;        // RED, YELLOW, GREEN, BLUE, WILD
    private String value;        // Number or action type
    private String chosenColor;  // For wild cards, the color chosen by the player
    
    // Default constructor for JSON deserialization
    public CardDTO() {}
    
    public CardDTO(String color, String value) {
        this.color = color;
        this.value = value;
    }
    
    public CardDTO(String color, String value, String chosenColor) {
        this.color = color;
        this.value = value;
        this.chosenColor = chosenColor;
    }
    
    // Getters and setters
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getChosenColor() {
        return chosenColor;
    }
    
    public void setChosenColor(String chosenColor) {
        this.chosenColor = chosenColor;
    }
    
    /**
     * Convert to string format expected by backend API
     * Format: COLOR_VALUE or WILD_VALUE_CHOSENCOLOR for wild cards
     */
    public String toApiFormat() {
        if ("WILD".equals(color)) {
            return color + "_" + value + "_" + chosenColor;
        } else {
            return color + "_" + value;
        }
    }
    
    @Override
    public String toString() {
        if ("WILD".equals(color) && chosenColor != null) {
            return value + " (" + chosenColor + ")";
        } else {
            return color + " " + value;
        }
    }
}
