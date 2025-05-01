package com.group2.uno.model.enums;

/**
 * Represents the color of a UNO card
 */
public enum CardColor {
    RED, 
    GREEN, 
    BLUE, 
    YELLOW, 
    WILD;  // For wild cards that can change color
    
    public String getColorStyle() {
        return switch (this) {
            case RED -> "-fx-background-color: #ff5555;";
            case GREEN -> "-fx-background-color: #55aa55;";
            case BLUE -> "-fx-background-color: #5555ff;";
            case YELLOW -> "-fx-background-color: #ffff55;";
            case WILD -> "-fx-background-color: #000000;";
        };
    }
}
