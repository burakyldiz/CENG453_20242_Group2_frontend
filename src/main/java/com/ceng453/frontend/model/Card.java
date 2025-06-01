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
        NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR, SKIP_ALL, COLOR_DRAW, SWAP_HANDS;
        
        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }
    
    private Color color;
    private Type type;
    private int number; // Only used for number cards
    private String chosenColor; // Used for wild cards when a color has been chosen
    
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
    
    public String getChosenColor() {
        return chosenColor;
    }
    
    public void setChosenColor(String chosenColor) {
        this.chosenColor = chosenColor;
    }
    
    public int getNumber() {
        return number;
    }
    
    // Helper method to check if this card can be played on top of another card
    public boolean canBePlayedOn(Card topCard) {
        // Wild cards can always be played
        if (this.type == Type.WILD) {
            return true;
        }
        
        // Wild Draw Four cards are always playable from a Card perspective
        // (special rules are enforced in Game.playCard and GameBoardController)
        if (this.type == Type.WILD_DRAW_FOUR) {
            return true;
        }
        
        // Bonus cards are always playable from a Card perspective
        if (this.type == Type.SKIP_ALL || this.type == Type.COLOR_DRAW || this.type == Type.SWAP_HANDS) {
            return true;
        }
        
        // Any card can be played on top of a Skip All, Color Draw, or Swap Hands card
        if (topCard.getType() == Type.SKIP_ALL || topCard.getType() == Type.COLOR_DRAW || 
            topCard.getType() == Type.SWAP_HANDS) {
            return true;
        }
        
        // Match by color
        if (this.color == topCard.getColor()) {
            return true;
        }
        
        // Match by number or type (including wild cards after color selection)
        if (topCard.getType() == Type.WILD || topCard.getType() == Type.WILD_DRAW_FOUR) {
            // For wild cards, we only need to check the current game color, which isn't available here
            // This is handled separately in the Game.playCard method
            return false;
        } else if (this.type == Type.NUMBER && topCard.getType() == Type.NUMBER) {
            return this.number == topCard.getNumber();
        } else {
            return this.type == topCard.getType();
        }
    }
    
    // Method to get the image file name for this card
    public String getImageFileName() {
        if (type == Type.NUMBER) {
            return color.toString() + "_" + number + ".png";
        } else if (type == Type.WILD) {
            return "wild.png";
        } else if (type == Type.WILD_DRAW_FOUR) {
            return "wild-draw-four.png";
        } else if (type == Type.SKIP_ALL || type == Type.COLOR_DRAW || type == Type.SWAP_HANDS) {
            return "wild-" + type.toString() + ".png";
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
    
    /**
     * Creates a Card from a string representation received from the backend
     * Expected formats:
     * - "RED_5" for number cards
     * - "BLUE_SKIP" for action cards
     * - "WILD_DRAW_FOUR" for wild cards
     * - "WILD_DRAW_FOUR_RED" for wild cards with chosen color
     * 
     * @param cardString String representation of the card
     * @return Card object or null if the string format is invalid
     */
    public static Card fromString(String cardString) {
        if (cardString == null || cardString.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = cardString.split("_");
            
            // Handle card back special case
            if (cardString.contains("card_back")) {
                return null; // Return null for card back as it's not a playable card
            }
            
            // Parse color
            Color cardColor;
            try {
                cardColor = Color.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                // Handle cases where the color might be lowercase
                cardColor = Color.valueOf(parts[0].toUpperCase());
            }
            
            // Handle wild cards with chosen color
            if (parts.length >= 3 && parts[0].equals("WILD")) {
                // For cards like WILD_DRAW_FOUR_RED, create the wild card first
                Card wildCard;
                if (parts[1].equals("DRAW") && parts[2].equals("FOUR")) {
                    wildCard = new Card(Color.WILD, Type.WILD_DRAW_FOUR);
                } else {
                    wildCard = new Card(Color.WILD, Type.WILD);
                }
                
                // Set the chosen color if specified
                if (parts.length >= 4) {
                    try {
                        Color chosenColor = Color.valueOf(parts[3]);
                        wildCard.setColor(chosenColor);
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid color
                    }
                }
                
                return wildCard;
            }
            
            // Handle regular cards
            if (parts.length >= 2) {
                // Check if it's a number card
                try {
                    int number = Integer.parseInt(parts[1]);
                    return new Card(cardColor, number);
                } catch (NumberFormatException e) {
                    // Not a number, must be an action card
                    String typeStr = parts[1];
                    
                    // Map action card types
                    switch (typeStr) {
                        case "SKIP":
                            return new Card(cardColor, Type.SKIP);
                        case "REVERSE":
                            return new Card(cardColor, Type.REVERSE);
                        case "DRAW_TWO":
                        case "DRAW": // Handle abbreviated format
                            return new Card(cardColor, Type.DRAW_TWO);
                        case "WILD":
                            return new Card(Color.WILD, Type.WILD);
                        case "WILD_DRAW_FOUR":
                        case "DRAW_FOUR": // Handle abbreviated format
                            return new Card(Color.WILD, Type.WILD_DRAW_FOUR);
                        // Add other special cards if needed
                        default:
                            try {
                                Type type = Type.valueOf(typeStr);
                                return new Card(cardColor, type);
                            } catch (IllegalArgumentException ex) {
                                // Unknown type
                                return null;
                            }
                    }
                }
            }
            
            return null; // Invalid format
        } catch (Exception e) {
            // Log exception if needed
            System.err.println("Error parsing card string: " + cardString + ", error: " + e.getMessage());
            return null;
        }
    }
}
