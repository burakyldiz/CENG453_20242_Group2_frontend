package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.service.MultiplayerPollingService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class MultiplayerLobbyController {

    private final SceneManager sceneManager;
    private final ApiService apiService;
    private final MultiplayerPollingService pollingService;

    // FXML elements
    @FXML private Button createGameButton;
    @FXML private Button joinGameButton;
    @FXML private Button refreshGamesButton;
    @FXML private Button backButton;
    
    @FXML private TextField sessionCodeField;
    @FXML private ListView<String> availableGamesList;
    @FXML private TextArea statusArea;
    
    // Current session info
    @FXML private VBox currentSessionPane;
    @FXML private Label sessionCodeLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private ListView<String> playersList;
    @FXML private Button startGameButton;
    @FXML private Button leaveGameButton;
    
    private Map<String, Object> currentSession;
    private Long currentUserId;

    public MultiplayerLobbyController(SceneManager sceneManager, ApiService apiService, 
                                    MultiplayerPollingService pollingService) {
        this.sceneManager = sceneManager;
        this.apiService = apiService;
        this.pollingService = pollingService;
    }

    @FXML
    public void initialize() {
        // Hide current session pane initially
        if (currentSessionPane != null) {
            currentSessionPane.setVisible(false);
        }
        
        // Get current user ID (for testing, using a dummy value if no user is logged in)
        if (sceneManager.getCurrentUser() != null) {
            currentUserId = sceneManager.getCurrentUser().getId();
        } else {
            currentUserId = 1L; // Default for testing
            addStatus("Warning: No logged in user, using default ID 1 for testing");
        }
        
        addStatus("Multiplayer Lobby initialized. User ID: " + currentUserId);
        refreshAvailableGames();
    }

    @FXML
    private void createGame() {
        addStatus("Creating new game session...");
        createGameButton.setDisable(true);
        
        apiService.createGameSession(currentUserId)
            .subscribe(
                gameSession -> {
                    Platform.runLater(() -> {
                        addStatus("Game session created successfully!");
                        setCurrentSession(gameSession);
                        startPolling();
                        createGameButton.setDisable(false);
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        addStatus("Error creating game: " + error.getMessage());
                        createGameButton.setDisable(false);
                    });
                }
            );
    }

    @FXML
    private void joinGame() {
        String sessionCode = sessionCodeField.getText().trim();
        if (sessionCode.isEmpty()) {
            addStatus("Please enter a session code");
            return;
        }
        
        addStatus("Joining game session: " + sessionCode);
        joinGameButton.setDisable(true);
        
        apiService.joinGameSession(sessionCode, currentUserId)
            .subscribe(
                gameSession -> {
                    Platform.runLater(() -> {
                        addStatus("Successfully joined game session!");
                        setCurrentSession(gameSession);
                        startPolling();
                        joinGameButton.setDisable(false);
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        addStatus("Error joining game: " + error.getMessage());
                        joinGameButton.setDisable(false);
                    });
                }
            );
    }

    @FXML
    private void refreshAvailableGames() {
        refreshGamesButton.setDisable(true);
        addStatus("Refreshing available games...");
        
        apiService.getAvailableGames()
            .subscribe(
                games -> {
                    Platform.runLater(() -> {
                        updateAvailableGamesList(games);
                        addStatus("Available games refreshed");
                        refreshGamesButton.setDisable(false);
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        addStatus("Error refreshing games: " + error.getMessage());
                        refreshGamesButton.setDisable(false);
                    });
                }
            );
    }

    @FXML
    private void startGame() {
        if (currentSession == null) {
            addStatus("No active session");
            return;
        }
        
        String sessionCode = (String) currentSession.get("sessionCode");
        addStatus("Starting game...");
        startGameButton.setDisable(true);
        
        apiService.startGame(sessionCode, currentUserId)
            .subscribe(
                gameSession -> {
                    Platform.runLater(() -> {
                        if (gameSession != null) {
                            addStatus("Game started successfully!");
                            setCurrentSession(gameSession);
                        } else {
                            addStatus("Failed to start game");
                        }
                        startGameButton.setDisable(false);
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        addStatus("Error starting game: " + error.getMessage());
                        startGameButton.setDisable(false);
                    });
                }
            );
    }

    @FXML
    private void leaveGame() {
        if (currentSession == null) {
            addStatus("No active session");
            return;
        }
        
        String sessionCode = (String) currentSession.get("sessionCode");
        addStatus("Leaving game...");
        
        pollingService.stopPolling();
        
        apiService.leaveGameSession(sessionCode, currentUserId)
            .subscribe(
                response -> {
                    Platform.runLater(() -> {
                        addStatus("Left game session");
                        clearCurrentSession();
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        addStatus("Error leaving game: " + error.getMessage());
                    });
                }
            );
    }

    @FXML
    private void goBack() {
        pollingService.stopPolling();
        sceneManager.showMainMenuScene();
    }

    private void setCurrentSession(Map<String, Object> session) {
        this.currentSession = session;
        
        if (currentSessionPane != null) {
            currentSessionPane.setVisible(true);
        }
        
        // Update session info
        String sessionCode = (String) session.get("sessionCode");
        String status = (String) session.get("status");
        
        if (sessionCodeLabel != null) {
            sessionCodeLabel.setText("Session Code: " + sessionCode);
        }
        
        if (sessionStatusLabel != null) {
            sessionStatusLabel.setText("Status: " + status);
        }
        
        updatePlayersList(session);
        updateStartButton(session);
        
        addStatus("Current session: " + sessionCode + " (Status: " + status + ")");
    }

    private void clearCurrentSession() {
        this.currentSession = null;
        
        if (currentSessionPane != null) {
            currentSessionPane.setVisible(false);
        }
    }

    private void updatePlayersList(Map<String, Object> session) {
        if (playersList == null) return;
        
        playersList.getItems().clear();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) session.get("players");
        
        if (players != null) {
            for (Map<String, Object> player : players) {
                String username = (String) player.get("username");
                playersList.getItems().add(username);
            }
        }
        
        Integer currentCount = (Integer) session.get("currentPlayerCount");
        Integer maxPlayers = (Integer) session.get("maxPlayers");
        addStatus("Players: " + currentCount + "/" + maxPlayers);
    }

    private void updateStartButton(Map<String, Object> session) {
        if (startGameButton == null) return;
        
        String status = (String) session.get("status");
        Map<String, Object> creator = (Map<String, Object>) session.get("creator");
        Integer creatorId = (Integer) creator.get("id");
        Integer currentCount = (Integer) session.get("currentPlayerCount");
        
        boolean isCreator = creatorId != null && creatorId.longValue() == currentUserId;
        boolean canStart = "WAITING".equals(status) && currentCount >= 2;
        
        startGameButton.setVisible(isCreator);
        startGameButton.setDisable(!canStart);
    }

    private void updateAvailableGamesList(List<Map<String, Object>> games) {
        if (availableGamesList == null) return;
        
        availableGamesList.getItems().clear();
        
        if (games != null) {
            for (Map<String, Object> game : games) {
                String sessionCode = (String) game.get("sessionCode");
                String creatorName = "";
                
                Map<String, Object> creator = (Map<String, Object>) game.get("creator");
                if (creator != null) {
                    creatorName = (String) creator.get("username");
                }
                
                Integer currentCount = (Integer) game.get("currentPlayerCount");
                Integer maxPlayers = (Integer) game.get("maxPlayers");
                
                String displayText = sessionCode + " - " + creatorName + " (" + currentCount + "/" + maxPlayers + ")";
                availableGamesList.getItems().add(displayText);
            }
        }
    }

    private void startPolling() {
        if (currentSession == null) return;
        
        String sessionCode = (String) currentSession.get("sessionCode");
        
        pollingService.startPolling(
            sessionCode,
            this::handleGameStateUpdate,
            this::handlePollingError
        );
        
        addStatus("Started polling for session: " + sessionCode);
    }

    private void handleGameStateUpdate(Map<String, Object> gameState) {
        addStatus("Received game state update");
        setCurrentSession(gameState);
        
        // Check if game has started
        String status = (String) gameState.get("status");
        if ("IN_PROGRESS".equals(status)) {
            addStatus("Game has started! Navigating to game board...");
            
            // Navigate to multiplayer game board
            Platform.runLater(() -> {
                sceneManager.showMultiplayerGameScene();
            });
        }
    }

    private void handlePollingError(String error) {
        addStatus("Polling error: " + error);
    }

    private void addStatus(String message) {
        if (statusArea != null) {
            statusArea.appendText("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message + "\n");
            statusArea.setScrollTop(Double.MAX_VALUE);
        }
        System.out.println("MultiplayerLobby: " + message);
    }
} 