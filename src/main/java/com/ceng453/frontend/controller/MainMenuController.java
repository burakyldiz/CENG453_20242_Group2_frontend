package com.ceng453.frontend.controller;

import com.ceng453.frontend.ui.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import org.springframework.stereotype.Controller;

@Controller
public class MainMenuController {
    
    @FXML private Label welcomeLabel;
    @FXML private Button singlePlayerButton;
    @FXML private Button multiplayerButton;
    @FXML private Button leaderboardButton;
    @FXML private Button logoutButton;
    
    private final SceneManager sceneManager;
    
    public MainMenuController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }
    
    @FXML
    public void initialize() {
        if (sceneManager.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + sceneManager.getCurrentUser().getUsername() + "!");
        } else {
            welcomeLabel.setText("Welcome to UNO!");
        }
    }
    
    @FXML
    public void handleSinglePlayer(ActionEvent event) {
        // Launch the actual single-player game
        sceneManager.showSinglePlayerGameScene();
    }
    
    @FXML
    public void handleMultiplayer(ActionEvent event) {
        sceneManager.showMultiplayerLobbyScene();
    }
    
    @FXML
    public void handleLeaderboard(ActionEvent event) {
        sceneManager.showLeaderboardScene();
    }
    
    @FXML
    public void handleLogout(ActionEvent event) {
        // Clear the current user
        sceneManager.setCurrentUser(null);
        
        // Navigate back to login screen
        sceneManager.showLoginScene();
    }
}
