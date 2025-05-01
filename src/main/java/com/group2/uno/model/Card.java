package com.group2.uno.model;

import com.group2.uno.model.enums.CardColor;
import com.group2.uno.model.enums.CardType;
import javafx.scene.image.Image;

/**
 * Represents a UNO card
 */
public class Card {
    private final CardColor color;
    private final CardType type;
    private final int number; // Only relevant for NUMBER type cards
    private String imagePath;
    
    public Card(CardColor color, CardType type, int number) {
        this.color = color;
        this.type = type;
        this.number = number;
        
        generateImagePath();
    }
    
    private void generateImagePath() {
        StringBuilder path = new StringBuilder("/images/cards/");
        
        if (type == CardType.NUMBER) {
            path.append(color.name().toLowerCase())
                .append("_")
                .append(number)
                .append(".png");
        } else if (type == CardType.WILD || type == CardType.WILD_DRAW_FOUR) {
            path.append("wild_")
                .append(type == CardType.WILD_DRAW_FOUR ? "draw_four" : "color")
                .append(".png");
        } else {
            path.append(color.name().toLowerCase())
                .append("_")
                .append(type.name().toLowerCase())
                .append(".png");
        }
        
        this.imagePath = path.toString();
    }
    
    public boolean canPlayOn(Card topCard) {
        if (type == CardType.WILD || type == CardType.WILD_DRAW_FOUR) {
            return true;
        }
        
        if (color == topCard.getColor()) {
            return true;
        }
        
        if (type == CardType.NUMBER && topCard.getType() == CardType.NUMBER) {
            return number == topCard.getNumber();
        }
        
        return type == topCard.getType();
    }
    
    public CardColor getColor() {
        return color;
    }
    
    public CardType getType() {
        return type;
    }
    
    public int getNumber() {
        return number;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public Image getImage() {
        return new Image(getClass().getResourceAsStream(imagePath));
    }
    
    @Override
    public String toString() {
        if (type == CardType.NUMBER) {
            return color + " " + number;
        } else {
            return color + " " + type;
        }
    }
}