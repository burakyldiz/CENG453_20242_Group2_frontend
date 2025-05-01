package com.group2.uno.model.enums;

/**
 * Represents the type of a UNO card
 */
public enum CardType {
    NUMBER,
    SKIP,
    REVERSE,
    DRAW_TWO,
    WILD,
    WILD_DRAW_FOUR;
    
    /**
     * Determines if this card type is considered a special card
     */
    public boolean isSpecial() {
        return this != NUMBER;
    }
    
    /**
     * Determines if this card type requires choosing a color
     */
    public boolean requiresColorChoice() {
        return this == WILD || this == WILD_DRAW_FOUR;
    }
}