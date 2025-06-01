package com.ceng453.frontend.ui;

import com.ceng453.frontend.controller.MultiplayerGameBoardController;
import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.UserSessionService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SceneManager {

    private final ApplicationContext applicationContext;
    private final UserSessionService userSessionService;
    private Stage primaryStage;
    private User currentUser; // For backward compatibility

    // Default scene dimensions
    private final int DEFAULT_WIDTH = 1024;
    private final int DEFAULT_HEIGHT = 768;

    @Autowired
    public SceneManager(ApplicationContext applicationContext, UserSessionService userSessionService) {
        this.applicationContext = applicationContext;
        this.userSessionService = userSessionService;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        
        // Sync with UserSessionService
        if (currentUser != null) {
            userSessionService.login(currentUser.getId(), currentUser.getUsername());
            System.out.println("Set current user in SceneManager: " + currentUser.getUsername());
        } else {
            userSessionService.logout();
            System.out.println("Cleared current user in SceneManager");
        }
    }

    public User getCurrentUser() {
        // If we have a user in the session service but not locally, restore it
        if (currentUser == null && userSessionService.isLoggedIn()) {
            currentUser = new User(
                userSessionService.getCurrentUserId(),
                userSessionService.getCurrentUsername(),
                null // Email not stored in session
            );
            System.out.println("Restored user from session: " + currentUser.getUsername());
        }
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
        loadScene("/fxml/multiplayer_game_board.fxml", "UNO - Multiplayer Game", DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public void showMultiplayerGameBoardScene(String gameId, Long playerId, String playerName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(applicationContext::getBean);
            loader.setLocation(getClass().getResource("/fxml/multiplayer_game_board.fxml"));
            
            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find FXML file at: /fxml/multiplayer_game_board.fxml");
                return;
            }
            
            Parent root = loader.load();
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            // Get the controller and set game properties
            MultiplayerGameBoardController controller = loader.getController();
            controller.initializeWithGameData(gameId, playerId, playerName);
            
            // Make root resizable
            if (root instanceof Region) {
                Region region = (Region) root;
                region.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
            
            // Apply CSS
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("UNO - Multiplayer Game");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("Successfully loaded multiplayer game scene with gameId: " + gameId + ", playerId: " + playerId);
        } catch (Exception e) {
            System.err.println("Error loading multiplayer game board scene: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show the multiplayer game board scene (backward compatibility method)
     * 
     * @param gameId The game identifier
     * @param playerId The local player's ID
     */
    public void showMultiplayerGameBoardScene(String gameId, Long playerId) {
        // Use default player name format when not explicitly provided
        showMultiplayerGameBoardScene(gameId, playerId, "Player " + playerId);
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
            
            // Make root resizable
            if (root instanceof Region) {
                Region region = (Region) root;
                region.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
            
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
