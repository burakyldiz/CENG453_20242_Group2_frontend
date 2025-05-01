package com.ceng453.frontend.model;

public class Card {
    // Enum for card colors
    public enum Color {
        RED, YELLOW, GREEN, BLUE, WILD;
        
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
    
    // Enum for card types
    public enum Type {
        NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR;
        
        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }
    
    private Color color;
    private Type type;
    private int number; // Only used for number cards
    
    // Constructor for number cards
    public Card(Color color, int number) {
        this.color = color;
        this.type = Type.NUMBER;
        this.number = number;
    }
    
    // Constructor for non-number cards
    public Card(Color color, Type type) {
        this.color = color;
        this.type = type;
        this.number = -1; // Not used for non-number cards
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public Type getType() {
        return type;
    }
    
    public int getNumber() {
        return number;
    }
    
    // Helper method to check if this card can be played on top of another card
    public boolean canBePlayedOn(Card topCard) {
        // Wild and Wild Draw Four can always be played
        if (this.type == Type.WILD || this.type == Type.WILD_DRAW_FOUR) {
            return true;
        }
        
        // Match by color
        if (this.color == topCard.getColor()) {
            return true;
        }
        
        // Match by number or type
        if (this.type == Type.NUMBER && topCard.getType() == Type.NUMBER) {
            return this.number == topCard.getNumber();
        } else {
            return this.type == topCard.getType();
        }
    }
    
    // Method to get the image file name for this card
    public String getImageFileName() {
        if (type == Type.NUMBER) {
            return color.toString() + "_" + number + ".png";
        } else if (type == Type.WILD || type == Type.WILD_DRAW_FOUR) {
            return type.toString() + ".png";
        } else {
            return color.toString() + "_" + type.toString() + ".png";
        }
    }
    
    @Override
    public String toString() {
        if (type == Type.NUMBER) {
            return color + " " + number;
        } else {
            return color + " " + type;
        }
    }
}
