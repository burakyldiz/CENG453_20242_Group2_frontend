package com.ceng453.frontend;

/**
 * Launcher class for Player 2 instance of the UNO game
 * Uses profile 'player2' with port 8084
 */
public class Player2Launcher {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "player2");
        UnoApplication.main(args);
    }
}
