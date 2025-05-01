package com.ceng453.frontend.ui;

import com.ceng453.frontend.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SceneManager {

    private final ApplicationContext applicationContext;
    private Stage primaryStage;
    private User currentUser;

    // Default scene dimensions
    private final int DEFAULT_WIDTH = 1024;
    private final int DEFAULT_HEIGHT = 768;

    public SceneManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void showLoginScene() {
        loadScene("/fxml/login.fxml", "UNO - Login", 600, 400);
    }

    public void showRegisterScene() {
        loadScene("/fxml/register.fxml", "UNO - Register", 600, 400);
    }

    public void showPasswordResetScene() {
        loadScene("/fxml/reset_password.fxml", "UNO - Reset Password", 600, 400);
    }

    public void showMainMenuScene() {
        loadScene("/fxml/main_menu.fxml", "UNO - Main Menu", 800, 600);
    }

    public void showLeaderboardScene() {
        loadScene("/fxml/leaderboard.fxml", "UNO - Leaderboard", 800, 600);
    }

    public void showSinglePlayerGameScene() {
        loadScene("/fxml/game_board.fxml", "UNO - Single Player Game", DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void showMultiplayerLobbyScene() {
        loadScene("/fxml/multiplayer_lobby.fxml", "UNO - Multiplayer Lobby", 800, 600);
    }

    public void showMultiplayerGameScene() {
        loadScene("/fxml/multiplayer_game.fxml", "UNO - Multiplayer Game", DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    private void loadScene(String fxmlPath, String title, int width, int height) {
        try {
            System.out.println("Attempting to load scene: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(applicationContext::getBean);
            loader.setLocation(getClass().getResource(fxmlPath));
            
            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find FXML file at: " + fxmlPath);
                return;
            }
            
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            
            // Apply CSS
            String cssPath = "/css/styles.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } else {
                System.err.println("Warning: CSS file not found: " + cssPath);
            }
            
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            
            // If stage was not visible, make it visible
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            
            System.out.println("Successfully loaded scene: " + fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading scene: " + fxmlPath + " - " + e.getMessage());
        }
    }
}
