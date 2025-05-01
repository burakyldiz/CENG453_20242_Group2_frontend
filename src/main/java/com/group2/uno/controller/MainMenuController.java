package com.group2.uno.controller;

import com.group2.uno.UnoApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class MainMenuController implements Initializable {
    
    @FXML
    private Label usernameLabel;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // You can set the username here from the AuthService
        // For now, we'll use a placeholder
        usernameLabel.setText("Player");
    }
    
    @FXML
    private void handleSinglePlayerGame(ActionEvent event) {
        try {
            UnoApplication.showGameBoardScreen();
        } catch (Exception e) {
            System.err.println("Error loading game board: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleMultiplayerGame(ActionEvent event) {
        // TODO: Implement multiplayer game functionality
        System.out.println("Multiplayer game selected");
    }
    
    @FXML
    private void handleLeaderboards(ActionEvent event) {
        try {
            UnoApplication.showLeaderboardScreen();
        } catch (Exception e) {
            System.err.println("Error loading leaderboard: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleHowToPlay(ActionEvent event) {
        // TODO: Implement how to play screen
        System.out.println("How to play selected");
    }
    
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            UnoApplication.showLoginScreen();
        } catch (Exception e) {
            System.err.println("Error logging out: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleExit(ActionEvent event) {
        System.exit(0);
    }
}