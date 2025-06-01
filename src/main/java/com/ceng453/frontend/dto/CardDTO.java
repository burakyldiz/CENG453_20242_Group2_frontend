package com.ceng453.frontend.dto;

public class CardDTO {
    private String color;
    private String value;
    private String chosenColor; // For wild cards

    // Default constructor
    public CardDTO() {
    }

    // Constructor with parameters
    public CardDTO(String color, String value) {
        this.color = color;
        this.value = value;
    }

    // Getters and Setters
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
}
