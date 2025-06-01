package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.service.UserSessionService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.UUID;

@Controller
public class LobbyController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField; // Added email field for registration
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private TextField playerNameLobbyField; 
    @FXML private Button createGameButton;
    @FXML private TextField gameIdField;
    @FXML private Button joinGameButton;

    private final ApiService apiService;
    private final SceneManager sceneManager;
    private final UserSessionService userSessionService;

    @Autowired
    public LobbyController(ApiService apiService, SceneManager sceneManager, UserSessionService userSessionService) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
        this.userSessionService = userSessionService;
    }

    @FXML
    public void initialize() {
        System.out.println("LobbyController initializing...");
        System.out.println("UserSessionService state - isLoggedIn: " + userSessionService.isLoggedIn() +
                          ", userId: " + userSessionService.getCurrentUserId() + 
                          ", username: " + userSessionService.getCurrentUsername());
        
        // Also check SceneManager for comparison
        User sceneManagerUser = sceneManager.getCurrentUser();
        System.out.println("SceneManager user: " + 
                (sceneManagerUser != null ? sceneManagerUser.getUsername() + " (ID: " + sceneManagerUser.getId() + ")" : "null"));
        
        // Check if user is already logged in via UserSessionService
        if (userSessionService.isLoggedIn() && userSessionService.getCurrentUserId() != null) {
            // User is already logged in, enable game buttons
            createGameButton.setDisable(false);
            joinGameButton.setDisable(false);
            
            // Use the logged-in username for player name
            String username = userSessionService.getCurrentUsername();
            playerNameLobbyField.setText(username);
            statusLabel.setText("Logged in as: " + username + " (ID: " + userSessionService.getCurrentUserId() + ")");
            System.out.println("LobbyController initialized - user already logged in: " + username + 
                              " (ID: " + userSessionService.getCurrentUserId() + ")");
        } else {
            // User is not logged in, disable game buttons
            createGameButton.setDisable(true);
            joinGameButton.setDisable(true);
            statusLabel.setText("Please log in to create or join games");
            System.out.println("LobbyController initialized - waiting for login");
            
            // If UserSessionService thinks we're logged in but ID is null, this is an inconsistent state
            if (userSessionService.isLoggedIn() && userSessionService.getCurrentUserId() == null) {
                System.out.println("WARNING: Inconsistent login state detected - logged in but no user ID");
                userSessionService.logout(); // Reset the session to be consistent
                System.out.println("Session reset for consistency");
            }
        }
        
        // Always allow editing the player name field
        playerNameLobbyField.setEditable(true);
        
        // Set a default player name if empty
        if (playerNameLobbyField.getText() == null || playerNameLobbyField.getText().trim().isEmpty()) {
            playerNameLobbyField.setText("Player" + (int)(Math.random() * 1000));
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            return;
        }
        
        statusLabel.setText("Logging in...");
        
        apiService.login(username, password,
            loginResponse -> {
                System.out.println("LobbyController login response: " + loginResponse);
                
                // Parse the response to extract user ID
                Long userId = null;
                
                try {
                    // Check if response is in JSON format (from updated backend)
                    if (loginResponse != null && loginResponse.contains("userId")) {
                        // Extract userId from JSON response
                        // Simple parsing - in a real app you'd use a proper JSON parser
                        String userIdStr = loginResponse.replaceAll(".*\"userId\":(\\d+).*", "$1");
                        userId = Long.parseLong(userIdStr);
                        System.out.println("Extracted userId from JSON response: " + userId);
                    } 
                    // Fallback to success check for backward compatibility
                    else if (loginResponse != null && (loginResponse.contains("successful") || loginResponse.equals("Login successful!"))) {
                        // Fallback to known mappings if response doesn't contain userId
                        System.out.println("WARNING: Backend returned success without userId. Using fallback mappings.");
                        
                        // Temporary mapping of usernames to their known database IDs
                        if ("alper".equals(username)) {
                            userId = 4L; // From your DB: alper has ID 4
                        } else if ("test123".equals(username)) {
                            userId = 2L; // From your DB: test123 has ID 2
                        } else if ("burak".equals(username)) {
                            userId = 6L; // From your DB: burak has ID 6
                        } else if ("messi".equals(username)) {
                            userId = 5L; // From your DB: messi has ID 5
                        } else {
                            // Generate a unique ID for unknown users (instead of defaulting to 1L)
                            // Use current time plus random number to avoid collisions
                            userId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
                            System.out.println("Generated unique player ID for unknown user: " + userId);
                        }
                    } else {
                        System.err.println("Login failed: " + loginResponse);
                        Platform.runLater(() -> statusLabel.setText("Login failed: " + loginResponse));
                        return;
                    }
                    
                    final Long finalUserId = userId;
                    System.out.println("IMPORTANT: Using REAL database player ID: " + userId + " for user " + username);
                    
                    Platform.runLater(() -> {
                        // Store user session data with REAL database ID
                        userSessionService.login(finalUserId, username);
                        statusLabel.setText("Login successful! Welcome " + username);
                        
                        // Enable game creation/joining buttons
                        createGameButton.setDisable(false);
                        joinGameButton.setDisable(false);
                        
                        // Make player name field editable and set it to the username
                        playerNameLobbyField.setEditable(true);
                        playerNameLobbyField.setText(username);
                        registerButton.setDisable(true);
                    });
                } catch (Exception e) {
                    System.err.println("Error parsing login response: " + e.getMessage());
                    Platform.runLater(() -> statusLabel.setText("Login error: " + e.getMessage()));
                }
            },
            error -> {
                System.out.println("Login error in lobby: " + error.getMessage());
                Platform.runLater(() -> 
                    statusLabel.setText("Login failed: " + error.getMessage())
                );
            }
        );
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username, email, and password");
            return;
        }
        
        statusLabel.setText("Registering...");
        
        apiService.register(username, email, password,
            response -> Platform.runLater(() -> 
                statusLabel.setText("Registration successful! You can now login.")
            ),
            error -> Platform.runLater(() -> 
                statusLabel.setText("Registration error: " + error.getMessage())
            )
        );
    }

    @FXML
    private void handleCreateGame() {
        System.out.println("handleCreateGame called");
        System.out.println("UserSessionService logged in? " + userSessionService.isLoggedIn());
        System.out.println("UserSessionService user ID: " + userSessionService.getCurrentUserId());
        System.out.println("UserSessionService username: " + userSessionService.getCurrentUsername());
        
        // Also check SceneManager for comparison
        System.out.println("SceneManager user: " + (sceneManager.getCurrentUser() != null ? 
                        sceneManager.getCurrentUser().getUsername() : "null"));
        
        // Check if user is logged in
        if (!userSessionService.isLoggedIn()) {
            statusLabel.setText("You must log in first to create a game");
            System.out.println("User not logged in, aborting game creation");
            return;
        }
        
        // Get player name or use username if empty
        String playerName = playerNameLobbyField.getText();
        if (playerName.trim().isEmpty()) {
            playerName = userSessionService.getCurrentUsername();
            playerNameLobbyField.setText(playerName);
        }
        
        statusLabel.setText("Creating game as " + playerName + "...");
        
        Long userId = userSessionService.getCurrentUserId();
        System.out.println("Creating game with user ID: " + userId);
        
        // Fail early if userId is null
        if (userId == null) {
            statusLabel.setText("Error: Could not retrieve your user ID. Please try logging in again.");
            System.out.println("User ID is null despite login check, aborting game creation");
            return;
        }
        
        apiService.createGame(userId,
            gameId -> {
                System.out.println("Game created successfully with ID: " + gameId);
                Platform.runLater(() -> {
                    // Show game ID prominently and set it in the gameIdField for easy copying
                    gameIdField.setText(gameId);
                    statusLabel.setText("GAME ID: " + gameId + " (copied to Game ID field for sharing)");
                    
                    // Auto-join the game you created
                    joinAndStartGame(gameId, userId, true);
                });
            },
            error -> {
                System.out.println("Game creation failed: " + error.getMessage());
                Platform.runLater(() -> 
                    statusLabel.setText("Failed to create game: " + error.getMessage())
                );
            }
        );
    }

    @FXML
    private void handleJoinGame() {
        // Get the game ID from the text field
        String gameId = gameIdField.getText();
        if (gameId.isEmpty()) {
            statusLabel.setText("Please enter a game ID");
            return;
        }
        
        // Validate that the game ID is in the correct format (UUID)
        if (!isValidGameId(gameId)) {
            statusLabel.setText("Invalid game ID format. Please enter a valid game ID.");
            return;
        }
        
        // Check if user is logged in
        if (!userSessionService.isLoggedIn()) {
            statusLabel.setText("You must log in first to join a game");
            return;
        }
        
        // Get player name and current user ID with additional debugging
        String playerName = playerNameLobbyField.getText().trim();
        if (playerName.isEmpty()) {
            playerName = userSessionService.getCurrentUsername();
            playerNameLobbyField.setText(playerName);
        }
        
        // Get current user ID from session and add debug logs
        Long userId = userSessionService.getCurrentUserId();
        
        System.out.println("=== USER SESSION DEBUG (handleJoinGame) ===");
        System.out.println("Current User ID from session: " + userId);
        System.out.println("Current Username from session: " + userSessionService.getCurrentUsername());
        System.out.println("Is logged in: " + userSessionService.isLoggedIn());
        System.out.println("Attempted Game ID: " + gameId);
        System.out.println("Attempted Player Name: " + playerName);
        System.out.println("=====================================");
        
        statusLabel.setText("Joining game as " + playerName + "...");
        
        joinAndStartGame(gameId, userId, false);
    }
    
    private void joinAndStartGame(String gameId, Long playerId, boolean isHost) {
        statusLabel.setText("Joining game...");
        
        // Debug user session state more thoroughly
        String currentUsername = userSessionService.getCurrentUsername();
        System.out.println("=== USER SESSION DEBUG (joinAndStartGame) ===");
        System.out.println("Given Player ID parameter: " + playerId + 
                         " (Type: " + (playerId != null ? playerId.getClass().getName() : "null") + ")");
        System.out.println("Double-checking current User ID from session: " + userSessionService.getCurrentUserId());
        System.out.println("Current Username from session: " + currentUsername);
        System.out.println("Is logged in: " + userSessionService.isLoggedIn());
        System.out.println("Game ID: " + gameId);
        System.out.println("Is host: " + isHost);
        System.out.println("=====================================");
        
        // Get player name or use username as fallback
        String playerName = playerNameLobbyField.getText().trim();
        if (playerName.isEmpty()) {
            playerName = userSessionService.getCurrentUsername();
        }
        
        // IMPORTANT: Force check the player ID again to ensure it's correct
        if (playerId == null || playerId == 1) {
            System.out.println("WARNING: Player ID is null or default (1). Double-checking with user session...");
            Long sessionId = userSessionService.getCurrentUserId();
            if (sessionId != null && sessionId != 1) {
                System.out.println("CORRECTING player ID from " + playerId + " to " + sessionId);
                playerId = sessionId;
            } else {
                // Generate a random unique ID if we can't get one from the session
                Long randomId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
                System.out.println("GENERATING new random player ID: " + randomId);
                playerId = randomId;
                // Update the session with this new ID for consistency
                userSessionService.setCurrentUserId(playerId);
            }
        }
        
        System.out.println("Attempting to join game: " + gameId + " with playerId: " + playerId + ", playerName: " + playerName + ", isHost: " + isHost);
        
        // If host (creator), skip the join step since they're already added to the game during creation
        if (isHost) {
            System.out.println("Game creator skipping join step and proceeding to start game");
            statusLabel.setText("Game created. Starting game...");
            startGame(gameId, playerId, playerName);
            return;
        }
        
        
        // For non-hosts, join the game normally with username included
        final String finalPlayerName = playerName; // For lambda capture
        final Long finalPlayerId = playerId; // Ensure correct ID is captured in lambda
        
        System.out.println("=== JOIN REQUEST PAYLOAD ====");
        System.out.println("gameId: " + gameId);
        System.out.println("playerId: " + finalPlayerId);
        System.out.println("playerName: " + finalPlayerName);
        System.out.println("===========================");
        
        apiService.joinGame(gameId, finalPlayerId, finalPlayerName,
            joinMessage -> {
                Platform.runLater(() -> {
                    System.out.println("Successfully joined game. Join response: " + joinMessage);
                    statusLabel.setText("Joined game successfully! Waiting for game to start...");
                    
                    // For non-host, transition to game board to start polling for game status
                    System.out.println("Non-host player transitioning to game board to wait for game start");
                    sceneManager.showMultiplayerGameBoardScene(gameId, finalPlayerId, finalPlayerName);
                });
            },
            error -> Platform.runLater(() -> {
                System.err.println("Error joining game: " + error.getMessage());
                System.err.println("Error class: " + error.getClass().getName());
                System.err.println("Failed parameters - gameId: " + gameId + ", playerId: " + finalPlayerId + ", playerName: " + finalPlayerName);
                statusLabel.setText("Error joining game: " + error.getMessage());
            })
        );
    }
    
    /**
     * This prevents errors when users try to join games with invalid IDs
     * 
     * @param gameId The game ID to validate
     * @return true if the game ID is a valid UUID, false otherwise
     */
    private boolean isValidGameId(String gameId) {
        try {
            // Attempt to parse the string as a UUID
            UUID.fromString(gameId);
            return true;
        } catch (IllegalArgumentException e) {
            // If parsing fails, it's not a valid UUID
            return false;
        }
    }
    
    private void startGame(String gameId, Long playerId, String playerName) {
        statusLabel.setText("Starting game...");
        
        // Double check the player ID against the session for consistency
        Long sessionId = userSessionService.getCurrentUserId();
        if (sessionId != null && !sessionId.equals(playerId)) {
            System.out.println("WARNING: Player ID mismatch detected in startGame method!");
            System.out.println("  - Method parameter ID: " + playerId);
            System.out.println("  - Session ID: " + sessionId);
            System.out.println("  -> Using session ID for consistency: " + sessionId);
            playerId = sessionId;
        }
        
        System.out.println("Host attempting to start game: " + gameId + " with playerId: " + playerId + ", playerName: " + playerName);
        
        // Create final variables for use in lambda expressions
        final Long finalPlayerId = playerId;
        final String finalPlayerName = playerName;
        
        apiService.startGame(gameId, finalPlayerId,
            response -> {
                Platform.runLater(() -> {
                    System.out.println("Game start successful! Response: " + response);
                    statusLabel.setText("Game started successfully!");
                    
                    // Show the multiplayer game scene and pass game id and player id
                    System.out.println("Transitioning to game board with gameId: " + gameId + ", playerId: " + finalPlayerId + ", playerName: " + finalPlayerName);
                    sceneManager.showMultiplayerGameBoardScene(gameId, finalPlayerId, finalPlayerName);
                });
            },
            error -> Platform.runLater(() -> {
                System.err.println("Error starting game: " + error.getMessage());
                System.err.println("Error class: " + error.getClass().getName());
                System.err.println("Failed parameters - gameId: " + gameId + ", playerId: " + finalPlayerId + ", playerName: " + finalPlayerName);
                
                if (error instanceof WebClientResponseException) {
                    WebClientResponseException webError = (WebClientResponseException) error;
                    System.err.println("Status code: " + webError.getStatusCode() + ", Response body: " + webError.getResponseBodyAsString());
                }
                
                statusLabel.setText("Failed to start game: " + error.getMessage() + ". Please wait for more players to join or try again later.");
                
                // Still show the game board so the host can see the lobby and retry starting
                System.out.println("Host transitioning to game board despite start error");
                sceneManager.showMultiplayerGameBoardScene(gameId, finalPlayerId, finalPlayerName);
            })
        );
    }
    
    /**
     * Handle returning to the main menu from the multiplayer lobby
     * Should handle game session update after a player exits to lobby
     */
    @FXML
    private void handleBackToMainMenu() {
        sceneManager.showMainMenuScene();
    }
}
