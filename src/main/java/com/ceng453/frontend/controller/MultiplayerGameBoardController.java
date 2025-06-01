package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.Card; 
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.ui.SceneManager;
import com.ceng453.frontend.dto.CardDTO;
import com.ceng453.frontend.dto.GameStateDTO;
import com.ceng453.frontend.dto.PlayerDTO;
import com.ceng453.frontend.service.PollingService;
import com.ceng453.frontend.service.UserSessionService;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.effect.ColorAdjust;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ceng453.frontend.dto.CardDTO;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.scene.Cursor;

@Controller
public class MultiplayerGameBoardController {
    // Image paths
    private static final String CARD_IMAGES_PATH = "/images/cards/";
    private static final String CARD_BACK_IMAGE = "/images/card_back.png";
    
    // Store player names by ID to handle game state updates that only contain IDs
    private static final Map<Long, String> playerNames = new ConcurrentHashMap<>();
    
    private final SceneManager sceneManager;
    private final ApiService apiService;
    private final PollingService pollingService;
    private final UserSessionService userSessionService;
    
    // Game state variables
    private String currentGameId;
    private Long currentPlayerId;
    private String currentPlayerName;
    private GameStateDTO lastKnownState;
    
    // Polling and UI state variables
    private ScheduledExecutorService pollingScheduler;
    private boolean waitingForColorSelection = false;
    private Card cardForWildSelection;
    
    // FXML elements
    @FXML private Label currentPlayerLabel;
    @FXML private Label currentColorLabel;
    @FXML private Label directionLabel;
    @FXML private Label gameStateLabel; 
    
    @FXML private FlowPane playerHandPane;
    @FXML private FlowPane opponent1HandPane;
    @FXML private FlowPane opponent2HandPane;
    @FXML private FlowPane opponent3HandPane;
    
    @FXML private StackPane discardPilePane;
    @FXML private HBox colorSelectionPane;
    
    @FXML private Circle playerTurnIndicator;
    @FXML private Circle opponent1TurnIndicator;
    @FXML private Circle opponent2TurnIndicator;
    @FXML private Circle opponent3TurnIndicator;
    
    @FXML private Label playerLabel;
    @FXML private Label opponent1Label;
    @FXML private Label opponent2Label;
    @FXML private Label opponent3Label;
    
    @FXML private Button drawCardButton;
    @FXML private Button unoButton;
    @FXML private Button fullscreenButton;
    
    @FXML private VBox cheatButtonsContainer; 

    public MultiplayerGameBoardController(SceneManager sceneManager, ApiService apiService, PollingService pollingService, UserSessionService userSessionService) {
        this.sceneManager = sceneManager;
        this.apiService = apiService;
        this.pollingService = pollingService;
        this.userSessionService = userSessionService;
    }

    /**
     * Initialize the game board with required data
     * @param gameId The ID of the game to join
     * @param playerId The ID of the local player
     */
    public void initializeWithGameData(String gameId, Long playerId) {
        // Validate player ID before using it
        if (playerId == null) {
            System.err.println("ERROR: Null player ID passed to MultiplayerGameBoardController.initializeWithGameData");
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Generated new player ID: " + playerId + " to prevent null ID issues");
        }
        
        if (playerId == 1L) {
            System.err.println("WARNING: Default player ID (1) passed to MultiplayerGameBoardController. This may cause issues.");
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Replacing default ID with new ID: " + playerId);
        }
        
        // Forward to the three-parameter version with a default player name
        this.currentGameId = gameId;
        this.currentPlayerId = playerId;
        this.currentPlayerName = "Player " + playerId;
        
        System.out.println("Initializing multiplayer game board for game: " + gameId + ", player: " + playerId);
        
        // Update the user session with the validated player ID for consistency
        try {
            if (userSessionService != null) {
                userSessionService.setCurrentUserId(playerId);
                System.out.println("Updated user session with player ID: " + playerId);
            }
        } catch (Exception e) {
            System.err.println("Error updating user session: " + e.getMessage());
        }
        
        // Initialize UI elements
        setupStaticUIElements();
        
        // Start polling for game state updates
        setupPolling();
    }
    
    /**
     * Initialize the game - called by SceneManager
     * @param gameId The game identifier
     * @param playerId The local player's ID
     */
    public void initializeGame(String gameId, Long playerId) {
        // Use the existing two-parameter method for backward compatibility
        initializeWithGameData(gameId, playerId);
    }
    
    /**
     * Initialize the game board UI and start polling for game updates
     * @param gameId The game identifier
     * @param playerId The local player's ID
     * @param playerName The local player's name
     */
    public void initializeWithGameData(String gameId, Long playerId, String playerName) {
        // Validate player ID before using it
        if (playerId == null) {
            System.err.println("ERROR: Null player ID passed to MultiplayerGameBoardController.initializeWithGameData (3-param)");
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Generated new player ID: " + playerId + " to prevent null ID issues");
        }
        
        if (playerId == 1L) {
            System.err.println("WARNING: Default player ID (1) passed to MultiplayerGameBoardController. This may cause issues.");
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Replacing default ID with new ID: " + playerId);
        }
        
        this.currentGameId = gameId;
        this.currentPlayerId = playerId;
        this.currentPlayerName = playerName;
        
        // Initialize the game deck on the server if we're the first player
        // This is important to ensure the game starts with a proper deck
        initializeDeck();
        
        // Store the local player name in our playerNames map
        playerNames.put(playerId, playerName);
        System.out.println("Stored local player name in playerNames map: " + playerId + " -> " + playerName);
        
        System.out.println("Initializing multiplayer game board for game: " + gameId + ", player: " + playerId + ", name: " + playerName);
        
        // Update the user session with the validated player ID for consistency
        try {
            if (userSessionService != null) {
                userSessionService.setCurrentUserId(playerId);
                System.out.println("Updated user session with player ID: " + playerId);
            }
        } catch (Exception e) {
            System.err.println("Error updating user session: " + e.getMessage());
        }
        
        // Initialize UI elements
        setupStaticUIElements();
        
        // Start polling for game state updates
        setupPolling();
        // For now, clear opponent hands as a default state
        if(opponent1HandPane != null) opponent1HandPane.getChildren().clear();
        if(opponent2HandPane != null) opponent2HandPane.getChildren().clear();
        if(opponent3HandPane != null) opponent3HandPane.getChildren().clear();
        if(opponent1Label != null) opponent1Label.setText("Opponent 1");
        if(opponent2Label != null) opponent2Label.setText("Opponent 2");
        if(opponent3Label != null) opponent3Label.setText("Opponent 3");
        if(playerLabel != null) playerLabel.setText(currentPlayerName != null ? currentPlayerName : "Player " + currentPlayerId);
    }

    private void setupStaticUIElements(){
        if (fullscreenButton != null) {
            fullscreenButton.setOnAction(event -> toggleFullscreen());
            Stage stage = (Stage) fullscreenButton.getScene().getWindow(); 
            if (stage != null) updateFullscreenButtonText(stage.isFullScreen());
        }
        if (cheatButtonsContainer != null) cheatButtonsContainer.setVisible(false);
        if (colorSelectionPane != null) colorSelectionPane.setVisible(false);
        if (drawCardButton != null) drawCardButton.setOnAction(e -> handleDrawCardAction());
        if (unoButton != null) unoButton.setOnAction(e -> handleUnoButtonAction());

        // Setup color selection buttons
        if (colorSelectionPane != null) {
            // Assuming buttons are named redButton, blueButton etc. in FXML or get them by order
            // For simplicity, direct handlers are fine if FXML methods are linked
        }
    }

    /**
     * Set up polling for game state updates
     */
    private void setupPolling() {
        stopPolling(); 
        
        if (pollingService != null) {
            pollingService.startPolling(currentGameId, currentPlayerId, this::updateGameState, this::handlePollingError);
            System.out.println("Started polling for game updates");
        } else {
            System.err.println("PollingService is null. Cannot start polling.");
            showMessage("Error: Could not initialize game updates.");
        }
    }
    
    /**
     * Stop polling for game state updates
     */
    private void stopPolling() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdown();
            try {
                if (!pollingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollingScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Called by PollingService with new GameStateDTO from backend
    private void updateGameState(GameStateDTO newState) {
        if (newState == null) {
            System.err.println("ERROR: Received null game state in updateGameState");
            return;
        }
        
        // First fetch player names from the API
        apiService.getPlayerNames(currentGameId, 
            // Success callback - playerNamesMap will contain player ID -> name mappings
            (Map<Long, String> playerNamesMap) -> {
                // Merge any fetched names into our local playerNames map
                if (playerNamesMap != null && !playerNamesMap.isEmpty()) {
                    System.out.println("DEBUG: Received player names from API: " + playerNamesMap);
                    playerNames.putAll(playerNamesMap);
                }
                
                // Now continue with the UI update on the JavaFX thread
                Platform.runLater(() -> {
                    try {
                        // Update current player label
                        if (currentPlayerLabel != null) {
                            currentPlayerLabel.setText("Turn: Player " + newState.getCurrentPlayerId());
                        }
                        
                        // Update current color label
                        if (currentColorLabel != null && newState.getTopCard() != null) {
                            Card topCard = newState.getTopCard();
                            String currentColor = topCard.getColor().name();
                            
                            // For wild cards, use the chosen color if available
                            if (Card.Color.WILD.equals(topCard.getColor()) && topCard.getChosenColor() != null) {
                                currentColor = topCard.getChosenColor();
                            }
                            updateColorLabelStyle(currentColor);
                        } else if (currentColorLabel != null) {
                            currentColorLabel.setText("Color: --");
                        }
                
                        // Update direction indicator
                        if (directionLabel != null) {
                            String direction = newState.isGameDirectionClockwise() ? "Clockwise" : "Counter-Clockwise";
                            directionLabel.setText("Direction: " + direction);
                        }
                        
                        // Update discard pile with top card
                        if (discardPilePane != null && newState.getTopCard() != null) {
                            Card topCard = newState.getTopCard();
                            String cardString;
                            if (topCard.getType() == Card.Type.NUMBER) {
                                cardString = topCard.getColor().name() + "_" + topCard.getNumber();
                            } else {
                                cardString = topCard.getColor().name() + "_" + topCard.getType().name();
                            }
                            // Handle wild cards with chosen color
                            if (Card.Color.WILD.equals(topCard.getColor()) && topCard.getChosenColor() != null) {
                                cardString += "_" + topCard.getChosenColor();
                            }
                            updateDiscardPile(cardString);
                        }
                        
                        // Update player hand
                        if (playerHandPane != null && newState.getPlayerHand() != null) {
                            // Filter out null cards from player hand before processing
                            List<Card> validCards = newState.getPlayerHand().stream()
                                .filter(card -> card != null)
                                .collect(Collectors.toList());
                            
                            // Log info about null card filtering
                            int nullCardCount = newState.getPlayerHand().size() - validCards.size();
                            if (nullCardCount > 0) {
                                System.out.println("WARNING: Filtered out " + nullCardCount + " null card(s) from player hand of size " + newState.getPlayerHand().size());
                            }
                            
                            List<String> handCardStrings = new ArrayList<>();
                            for (Card card : validCards) {
                                try {
                                    String cardString;
                                    if (card.getType() == Card.Type.NUMBER) {
                                        cardString = card.getColor().name() + "_" + card.getNumber();
                                    } else {
                                        cardString = card.getColor().name() + "_" + card.getType().name();
                                    }
                                    handCardStrings.add(cardString);
                                } catch (Exception e) {
                                    System.err.println("Error processing card: " + e.getMessage());
                                }
                            }
                            updatePlayerHand(handCardStrings);
                        }
                        
                        // Update opponent displays using playerCardCounts since the backend doesn't send a players list
                        if (newState.getPlayerCardCounts() != null) {
                            System.out.println("DEBUG: Received " + newState.getPlayerCardCounts().size() + " player card counts in game state");
                            
                            Map<Long, PlayerDTO> opponents = new HashMap<>();
                            for (Map.Entry<Long, Integer> entry : newState.getPlayerCardCounts().entrySet()) {
                                Long playerId = entry.getKey();
                                Integer cardCount = entry.getValue();
                                
                                // Skip the local player
                                if (!playerId.equals(currentPlayerId)) {
                                    System.out.println("DEBUG: Creating opponent DTO for player ID: " + playerId + ", Hand size: " + cardCount);
                                    
                                    // Create a PlayerDTO with the available information
                                    PlayerDTO player = new PlayerDTO();
                                    player.setUserId(playerId);
                                    
                                    // PlayerDTO doesn't have setHandSize, it calculates from hand list
                                    // Create a dummy hand list with the correct size
                                    List<CardDTO> dummyHand = new ArrayList<>();
                                    for (int i = 0; i < cardCount; i++) {
                                        dummyHand.add(new CardDTO());
                                    }
                                    player.setHand(dummyHand);
                                    
                                    // Use stored player name if we have it, otherwise use default ID format
                                    String playerUsername = playerNames.getOrDefault(playerId, null);
                                    
                                    // If we don't have a stored name, use the default format
                                    if (playerUsername == null) {
                                        playerUsername = "Player" + playerId;
                                        System.out.println("DEBUG: Using default player name format: " + playerId + " -> " + playerUsername);
                                        
                                        // Store this format for future use
                                        playerNames.put(playerId, playerUsername);
                                    } else {
                                        System.out.println("DEBUG: Using stored player name: " + playerId + " -> " + playerUsername);
                                    }
                                    
                                    player.setUsername(playerUsername);
                                    opponents.put(playerId, player);
                                    System.out.println("DEBUG: Added opponent to display map - ID: " + playerId + ", Username: " + player.getUsername());
                                } else {
                                    System.out.println("DEBUG: Skipping current player (self) - ID: " + playerId);
                                }
                            }
                            
                            System.out.println("DEBUG: Found " + opponents.size() + " opponents to display");
                            updateOpponentDisplays(opponents, newState.getCurrentPlayerId());
                            updateTurnIndicators(newState.getCurrentPlayerId());
                        } else {
                            System.out.println("DEBUG: Received null playerCardCounts in game state");
                        }

                        // Show game messages from the log
                        if (newState.getGameLog() != null && !newState.getGameLog().isEmpty()){
                            showMessage(String.join("\n", newState.getGameLog()));
                        }

                        if (newState.isGameOver()) {
                            handleMultiplayerGameOver(newState.getWinnerId());
                            // if (pollingService != null) pollingService.stopPolling();
                        } else {
                            boolean isMyTurn = currentPlayerId.equals(newState.getCurrentPlayerId());
                            // TODO: Server should indicate if color selection is pending for this player
                            // boolean needsColorSelection = newState.isAwaitingColorChoice() && isMyTurn;
                            // waitingForColorSelection = needsColorSelection;
                            // if(colorSelectionPane != null) colorSelectionPane.setVisible(needsColorSelection);
                            
                            if(drawCardButton != null) drawCardButton.setDisable(!isMyTurn || waitingForColorSelection);
                            if(unoButton != null) unoButton.setDisable(!isMyTurn || waitingForColorSelection); 
                            if(playerHandPane != null) playerHandPane.setDisable(!isMyTurn || waitingForColorSelection);
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating game state UI: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            },
            // Error callback
            (Throwable error) -> {
                System.err.println("Error fetching player names: " + error.getMessage());
            }
        );
    }

    private void updateOpponentDisplays(Map<Long, PlayerDTO> opponents, Long currentTurnPlayerId) {
        if (opponents == null || opponents.isEmpty()) {
            System.out.println("DEBUG: updateOpponentDisplays called with null or empty opponents map");
            return;
        }

        System.out.println("DEBUG: updateOpponentDisplays processing " + opponents.size() + " opponents");
        
        List<FlowPane> opponentPanes = Arrays.asList(opponent1HandPane, opponent2HandPane, opponent3HandPane);
        List<Label> opponentLabels = Arrays.asList(opponent1Label, opponent2Label, opponent3Label);
        List<Circle> opponentTurnIndicators = Arrays.asList(opponent1TurnIndicator, opponent2TurnIndicator, opponent3TurnIndicator);

        int opponentUiIndex = 0;
        for (Map.Entry<Long, PlayerDTO> entry : opponents.entrySet()) {
            Long playerId = entry.getKey();
            PlayerDTO player = entry.getValue();
            
            System.out.println("DEBUG: Processing opponent - ID: " + playerId + ", Username: '" + player.getUsername() + "', CardCount: " + player.getHandSize());
            
            // Use player name if available, otherwise use ID
            String playerName = player.getUsername();
            Integer cardCount = player.getHandSize();
            
            String displayName = (playerName != null && !playerName.trim().isEmpty()) 
                ? playerName 
                : "Player " + playerId;

            System.out.println("DEBUG: Display name for opponent: '" + displayName + "'");
            
            if (opponentUiIndex < opponentPanes.size()) {
                if (opponentLabels.get(opponentUiIndex) != null) {
                    String labelText = displayName + " (" + cardCount + ")";
                    System.out.println("DEBUG: Setting opponent label " + opponentUiIndex + " to: '" + labelText + "'");
                    opponentLabels.get(opponentUiIndex).setText(labelText);
                } else {
                    System.out.println("DEBUG: Opponent label " + opponentUiIndex + " is null");
                }
                
                if (opponentPanes.get(opponentUiIndex) != null) {
                    opponentPanes.get(opponentUiIndex).getChildren().clear();
                    for (int i = 0; i < cardCount; i++) {
                        opponentPanes.get(opponentUiIndex).getChildren().add(createCardBackImageView());
                    }
                    System.out.println("DEBUG: Added " + cardCount + " cards to opponent " + opponentUiIndex + " hand pane");
                } else {
                    System.out.println("DEBUG: Opponent pane " + opponentUiIndex + " is null");
                }
                
                if (opponentTurnIndicators.get(opponentUiIndex) != null) {
                    boolean isCurrentTurn = playerId.equals(currentTurnPlayerId);
                    opponentTurnIndicators.get(opponentUiIndex).setFill(isCurrentTurn ? Color.LIGHTGREEN : Color.LIGHTGRAY);
                    System.out.println("DEBUG: Set turn indicator for opponent " + opponentUiIndex + " to " + (isCurrentTurn ? "active" : "inactive"));
                } else {
                    System.out.println("DEBUG: Opponent turn indicator " + opponentUiIndex + " is null");
                }
                
                opponentUiIndex++;
            } else {
                System.out.println("DEBUG: No more UI slots available for opponent with ID: " + playerId);
            }
        }
        
        // Clear remaining opponent UI slots
        System.out.println("DEBUG: Clearing remaining " + (opponentPanes.size() - opponentUiIndex) + " opponent UI slots");
        for (int i = opponentUiIndex; i < opponentPanes.size(); i++) {
            if (opponentLabels.get(i) != null) opponentLabels.get(i).setText("-");
            if (opponentPanes.get(i) != null) opponentPanes.get(i).getChildren().clear();
            if (opponentTurnIndicators.get(i) != null) opponentTurnIndicators.get(i).setFill(Color.LIGHTGRAY);
            System.out.println("DEBUG: Cleared opponent UI slot " + i);
        }
    }
    
    private void updateTurnIndicators(Long currentTurnPlayerId) {
        if (playerTurnIndicator != null) {
            boolean isMyTurn = currentPlayerId.equals(currentTurnPlayerId);
            playerTurnIndicator.setFill(isMyTurn ? Color.GREEN : Color.LIGHTGRAY);
            System.out.println("DEBUG: Set player turn indicator to " + (isMyTurn ? "active" : "inactive"));
        }
        // Opponent turn indicators are updated in updateOpponentDisplays
    }

    private void handleMultiplayerGameOver(Long winnerId) {
        if (pollingService != null) {
            pollingService.stopPolling();
        }
        String message = (winnerId == null) ? "Game Over!" :
                         (winnerId.equals(currentPlayerId) ? "Congratulations! You won!" :
                                                          "Player " + winnerId + " has won!");
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            sceneManager.showMainMenuScene();
        });
    }

    private void showMessage(String text) {
        if (gameStateLabel != null) gameStateLabel.setText(text);
        else System.out.println("Game Message: " + text);
    }

    private void handlePollingError(Throwable error) {
        Platform.runLater(() -> {
            showMessage("Connection error: " + error.getMessage());
            // Optionally, disable game interactions or attempt to reconnect
        });
    }
    
    /**
     * Handle the back to menu button click
     * Stops polling and returns to the main menu
     */
    @FXML
    public void handleBackToMenu() {
        System.out.println("Leaving multiplayer game");
        stopPolling();
        sceneManager.showMainMenuScene();
    }

    @FXML private void selectRedColor() { handleColorSelectedAction(Card.Color.RED); }
    @FXML private void selectBlueColor() { handleColorSelectedAction(Card.Color.BLUE); }
    @FXML private void selectGreenColor() { handleColorSelectedAction(Card.Color.GREEN); }
    @FXML private void selectYellowColor() { handleColorSelectedAction(Card.Color.YELLOW); }

    private void handleColorSelectedAction(Card.Color color) {
        if (!currentPlayerId.equals(lastKnownState != null ? lastKnownState.getCurrentPlayerId() : 0L) || !waitingForColorSelection) return;
        
        colorSelectionPane.setVisible(false);
        waitingForColorSelection = false;
        System.out.println("Selected color: " + color.name());
        
        // Prepare the card data with chosen color
        // The backend will know it's a wild card from the format
        String wildCardString;
        if (cardForWildSelection.getType() == Card.Type.WILD) {
            wildCardString = "WILD_WILD_" + color.name();
        } else if (cardForWildSelection.getType() == Card.Type.WILD_DRAW_FOUR) {
            wildCardString = "WILD_WILD_DRAW_FOUR_" + color.name();
        } else {
            showMessage("Error: Invalid wild card type");
            cardForWildSelection = null;
            return;
        }
        
        // Create action as Map<String, Object> for the wild card play with color
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", currentPlayerId);
        actionDto.put("actionType", "PLAY");
        actionDto.put("cardPlayed", wildCardString);
        actionDto.put("chosenColor", color.name());
        
        // Now call backend API with the wild card + chosen color
        apiService.performAction(currentGameId, actionDto,
            response -> {
                System.out.println("Play wild card response: " + response);
                cardForWildSelection = null; 
                // UI updates will come through polling
            },
            error -> {
                System.err.println("Error playing wild card: " + error.getMessage());
                showMessage("Error: " + error.getMessage());
                cardForWildSelection = null; 
            }
        );
    }

    /**
     * Initialize the game deck on the server
     * Called once during game initialization to ensure proper deck setup
     */
    private void initializeDeck() {
        if (currentPlayerId == null || currentGameId == null) {
            System.err.println("Cannot initialize deck: Game not properly initialized");
            return;
        }
        
        System.out.println("Initializing deck for game ID: " + currentGameId);
        
        // Create a deck initialization action as Map<String, Object>
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", currentPlayerId);
        actionDto.put("actionType", "INITIALIZE_DECK");
        
        apiService.performAction(currentGameId, actionDto,
            response -> {
                System.out.println("Deck initialization response: " + response);
            },
            error -> {
                // Don't show errors to the user during initialization
                // The backend might already have a deck initialized
                System.out.println("Note: Deck may already be initialized: " + error.getMessage());
            }
        );
    }
    
    /**
     * Handle draw card action - called when the draw card button is clicked
     */
    @FXML
    public void handleDrawCardAction() {
        System.out.println("Draw card action requested");
        // Call backend API to draw a card
        if (currentPlayerId == null || currentGameId == null) {
            showMessage("Error: Game not properly initialized");
            return;
        }
        
        // Disable button to prevent multiple requests
        if (drawCardButton != null) drawCardButton.setDisable(true);
        
        // Create action as Map<String, Object> for compatibility with ApiService
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", currentPlayerId);
        actionDto.put("actionType", "DRAW");
        
        apiService.performAction(currentGameId, actionDto,
            response -> {
                System.out.println("Draw card response: " + response);
            },
            error -> {
                System.err.println("Error drawing card: " + error.getMessage());
                showMessage("Error: Could not draw card");
                if (drawCardButton != null) drawCardButton.setDisable(false);
            }
        );
    }

    /**
     * Handle UNO button action - called when the UNO button is clicked
     */
    @FXML
    public void handleUnoButtonAction() {
        System.out.println("UNO button pressed");
        
        if (currentPlayerId == null || currentGameId == null) {
            showMessage("Error: Game not properly initialized");
            return;
        }
        
        // Create action as Map<String, Object> for compatibility with ApiService
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", currentPlayerId);
        actionDto.put("actionType", "UNO");
        
        // Call backend API to declare UNO
        apiService.performAction(currentGameId, actionDto, 
            response -> {
                System.out.println("Declare UNO response: " + response);
                showMessage("UNO declared!");
            },
            error -> {
                System.err.println("Error declaring UNO: " + error.getMessage());
                showMessage("Error: Could not declare UNO");
            }
        );
    }
    
    private void handleCardClick(ImageView cardImageView, Card card) {
        // Check if it's player's turn
        if (lastKnownState == null || !currentPlayerId.equals(lastKnownState.getCurrentPlayerId())) {
            showMessage("It's not your turn yet!");
            return;
        }

        // Check if the card is valid to play against the top card
        if (lastKnownState != null && lastKnownState.getTopCard() != null) {
            Card topCard = lastKnownState.getTopCard();
            if (!isValidPlay(card, topCard)) {
                showMessage("Invalid card! You must match the color, number, or card type.");
                return;
            }
        }

        // Prepare the card data to send to server in the format expected by the backend
        String cardString;
        if (card.getType() == Card.Type.NUMBER) {
            cardString = card.getColor().toString() + "_" + card.getNumber();
        } else {
            cardString = card.getColor().toString() + "_" + card.getType().toString();
        }
        System.out.println("Attempting to play card: " + cardString);
        
        // Handle wild cards - store the selected card and show color selection pane
        if (card.getType() == Card.Type.WILD || card.getType() == Card.Type.WILD_DRAW_FOUR) {
            cardForWildSelection = card;
            waitingForColorSelection = true;
            if (colorSelectionPane != null) {
                colorSelectionPane.setVisible(true);
                showMessage("Select a color for your wild card");
            }
            return; // Wait for color selection before proceeding
        }

        // For non-wild cards, prepare action as Map<String, Object>
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", currentPlayerId);
        actionDto.put("actionType", "PLAY");
        actionDto.put("cardPlayed", cardString);
        
        // Send the play card request using the standard performAction method
        apiService.performAction(currentGameId, actionDto,
            response -> {
                System.out.println("Play card response: " + response);
                // UI updates will come through polling
            },
            error -> {
                System.err.println("Error playing card: " + error.getMessage());
                showMessage("Error: " + error.getMessage());
            }
        );
    }
    
    /**
     * Check if a card is valid to play against the top card
     * @param card The card to play
     * @param topCard The current top card
     * @return true if the card is valid to play
     */
    private boolean isValidPlay(Card card, Card topCard) {
        // Wild cards can always be played
        if (card.getType() == Card.Type.WILD || card.getType() == Card.Type.WILD_DRAW_FOUR) {
            return true;
        }
        
        // Same color
        if (card.getColor().equals(topCard.getColor())) {
            return true;
        }
        
        // For wild cards with chosen color, check against the chosen color
        if (topCard.getColor() == Card.Color.WILD && topCard.getChosenColor() != null) {
            try {
                Card.Color chosenColor = Card.Color.valueOf(topCard.getChosenColor());
                if (card.getColor().equals(chosenColor)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid chosen color: " + topCard.getChosenColor());
            }
        }
        
        // Same number or action type
        if (card.getType() == Card.Type.NUMBER && topCard.getType() == Card.Type.NUMBER) {
            return card.getNumber() == topCard.getNumber();
        }
        
        // Same special card type (SKIP, REVERSE, etc.)
        return card.getType() == topCard.getType();
    }

    private void updatePlayerHand(List<String> handCardStrings) {
        if (playerHandPane == null) return;
        
        playerHandPane.getChildren().clear();
        
        for (String cardIdentifier : handCardStrings) {
            final Card card = Card.fromString(cardIdentifier);
            if (card == null) {
                System.err.println("Could not parse card: " + cardIdentifier);
                continue; // Skip this card if we can't parse it
            }
            playerHandPane.getChildren().add(createCardImageView(cardIdentifier, true));
        }
    }

    private void updateDiscardPile(String topCardString) {
        if (discardPilePane == null) return;
        discardPilePane.getChildren().clear();
        if (topCardString == null || topCardString.isEmpty()) {
            discardPilePane.getChildren().add(createCardImageView(CARD_BACK_IMAGE, false));
        } else {
            discardPilePane.getChildren().add(createCardImageView(topCardString, false));
        }
    }

    private ImageView createCardImageView(String cardIdentifier, boolean isPlayable) {
        String imagePath = CARD_BACK_IMAGE;
        final Card card; // Use frontend Card model for image mapping
        if (!cardIdentifier.equals(CARD_BACK_IMAGE)) {
            card = Card.fromString(cardIdentifier); // Assumes Card.fromString exists and handles backend strings
            if (card != null) imagePath = CARD_IMAGES_PATH + card.getImageFileName();
        } else {
            card = null;
        }

        try (InputStream imageStream = getClass().getResourceAsStream(imagePath)) {
            if (imageStream == null) throw new RuntimeException("Cannot find card image: " + imagePath);
            ImageView imageView = new ImageView(new Image(imageStream));
            imageView.setFitWidth(80);imageView.setFitHeight(120);imageView.setPreserveRatio(true);

            if (isPlayable && card != null) {
                imageView.setCursor(Cursor.HAND);
                imageView.setOnMouseClicked(event -> handleCardClick(imageView, card));
                ColorAdjust brighter = new ColorAdjust(0,0,0.3,0); ColorAdjust normal = new ColorAdjust();
                imageView.setOnMouseEntered(e -> imageView.setEffect(brighter));
                imageView.setOnMouseExited(e -> imageView.setEffect(normal));
            }
            return imageView;
        } catch (Exception e) {
            System.err.println("Error loading card image " + imagePath + ": " + e.getMessage());
            Rectangle r = new Rectangle(80,120, Color.GAINSBORO); Text t = new Text("?");
            return new ImageView(new StackPane(r,t).snapshot(null,null)); // Basic placeholder
        }
    }

    private ImageView createCardBackImageView() {
        return createCardImageView(CARD_BACK_IMAGE, false);
    }

    @FXML
    private void toggleFullscreen() {
        if(fullscreenButton == null || fullscreenButton.getScene() == null) return;
        Stage stage = (Stage) fullscreenButton.getScene().getWindow();
        if(stage == null) return;
        stage.setFullScreen(!stage.isFullScreen());
        updateFullscreenButtonText(stage.isFullScreen());
    }

    private void updateFullscreenButtonText(boolean isFullscreen) {
        if (fullscreenButton != null) fullscreenButton.setText(isFullscreen ? "Exit Fullscreen" : "Fullscreen");
    }

    /**
     * Converts server color string to JavaFX style
     * @param colorString Color string from server
     * @return JavaFX style string for color
     */
    /**
     * Converts server color string to JavaFX style
     * @param colorString Color string from server
     * @return JavaFX style string for color
     */
    private String getColorStyleFromServerString(String colorString) {
        if (colorString == null) return "-fx-text-fill: black;";
        
        switch (colorString.toUpperCase()) {
            case "RED": return "-fx-text-fill: #d32f2f;"; // Dark red
            case "BLUE": return "-fx-text-fill: #1976d2;"; // Dark blue
            case "GREEN": return "-fx-text-fill: #388e3c;"; // Dark green
            case "YELLOW": return "-fx-text-fill: #fbc02d;"; // Dark yellow
            default: return "-fx-text-fill: black;";
        }
    }
    
    /**
     * Apply the color style to the currentColorLabel
     * @param colorString The color string
     */
    private void updateColorLabelStyle(String colorString) {
        if (currentColorLabel == null) return;
        
        String colorStyle = getColorStyleFromServerString(colorString);
        currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
    }
    
    /**
     * Utility method to convert a Card to CardDTO
     * @param card The Card to convert
     * @return A new CardDTO with data from the Card
     */
    private CardDTO convertCardToCardDTO(Card card) {
        if (card == null) return null;
        
        CardDTO dto = new CardDTO();
        dto.setColor(card.getColor().name());
        
        if (card.getType() == Card.Type.NUMBER) {
            dto.setValue(String.valueOf(card.getNumber()));
        } else {
            dto.setValue(card.getType().name());
        }
        
        // The chosenColor is already a String in the Card class
        dto.setChosenColor(card.getChosenColor());
        return dto;
    }
    
    /**
     * Handle wild card play with selected color
     * @param selectedColor The color selected for the wild card
     * @param wildCard The wild card being played
     */
    private void handleWildCardPlay(Card.Color selectedColor, Card wildCard) {
        // Now call backend API with the wild card + chosen color
        String wildCardString;
        if (wildCard.getType() == Card.Type.WILD) {
            wildCardString = "wild_wild_" + selectedColor.name();
        } else if (wildCard.getType() == Card.Type.WILD_DRAW_FOUR) {
            wildCardString = "wild_wild-draw-four_" + selectedColor.name();
        } else {
            showMessage("Error: Invalid wild card type");
            return;
        }
        
        apiService.playCard(currentGameId, currentPlayerId, wildCardString,
            response -> {
                System.out.println("Play wild card response: " + response);
                cardForWildSelection = null; 
                // UI updates will come through polling
            },
            error -> {
                System.err.println("Error playing wild card: " + error.getMessage());
                showMessage("Error: " + error.getMessage());
                cardForWildSelection = null; 
            }
        );
    }
}
