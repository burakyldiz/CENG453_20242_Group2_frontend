package com.ceng453.frontend;

/**
 * Launcher class for Player 1 instance of the UNO game
 * Uses profile 'player1' with port 8083
 */
public class Player1Launcher {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "player1");
        UnoApplication.main(args);
    }
}
