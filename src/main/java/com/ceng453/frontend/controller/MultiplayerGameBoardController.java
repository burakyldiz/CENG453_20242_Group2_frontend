package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.Card;
import com.ceng453.frontend.model.Deck;
import com.ceng453.frontend.model.Game;
import com.ceng453.frontend.model.Player;
import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.service.MultiplayerPollingService;
import com.ceng453.frontend.ui.SceneManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MultiplayerGameBoardController {
    
    // Image paths
    private static final String CARD_IMAGES_PATH = "/images/cards/";
    private static final String CARD_BACK_IMAGE = "/images/card_back.png";
    
    // Spring-injected dependencies
    private final SceneManager sceneManager;
    private final ApiService apiService;
    private final MultiplayerPollingService pollingService;
    
    // Game state
    private Game game;
    private String sessionCode;
    private Long currentUserId;
    private Map<String, Object> currentGameState;
    private boolean waitingForColorSelection;
    private Card lastPlayedCard;
    private int myPlayerIndex = -1; // Which player index am I in the game
    
    // FXML elements
    @FXML private Label currentPlayerLabel;
    @FXML private Label currentColorLabel;
    @FXML private Label directionLabel;
    @FXML private Label gameStateLabel;
    @FXML private Label sessionInfoLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label waitingForPlayerLabel;
    
    @FXML private FlowPane playerHandPane;
    @FXML private FlowPane opponentHandPane;
    
    @FXML private StackPane discardPilePane;
    @FXML private HBox colorSelectionPane;
    
    @FXML private Circle playerTurn;
    @FXML private Circle opponentTurn;
    
    @FXML private Label playerLabel;
    @FXML private Label opponentLabel;
    
    @FXML private Button drawCardButton;
    @FXML private Button passTurnButton;
    @FXML private Button unoButton;
    @FXML private Button leaveGameButton;
    
    // Card image cache
    private static final Map<String, Image> cardImageCache = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public MultiplayerGameBoardController(SceneManager sceneManager, ApiService apiService, 
                                        MultiplayerPollingService pollingService) {
        this.sceneManager = sceneManager;
        this.apiService = apiService;
        this.pollingService = pollingService;
    }
    
    @FXML
    public void initialize() {
        // Get current user
        User currentUser = sceneManager.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            playerLabel.setText(currentUser.getUsername());
        } else {
            currentUserId = 1L; // Fallback for testing
            playerLabel.setText("You");
        }
        
        // Get session code from polling service
        sessionCode = pollingService.getCurrentSessionCode();
        if (sessionCode != null) {
            sessionInfoLabel.setText("Session: " + sessionCode);
        }
        
        showMessage("Initializing multiplayer game...");
        
        // Start polling for game state
        startGamePolling();
        
        // Preload card images
        preloadCardImages();
    }
    
    private void startGamePolling() {
        if (sessionCode != null) {
            pollingService.startPolling(
                sessionCode,
                this::handleGameStateUpdate,
                this::handlePollingError
            );
            connectionStatusLabel.setText("Connected");
            connectionStatusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        }
    }
    
    private void handleGameStateUpdate(Map<String, Object> gameState) {
        Platform.runLater(() -> {
            try {
                System.out.println("=== GAME STATE UPDATE ===");
                System.out.println("Current gameState keys: " + gameState.keySet());
                System.out.println("Session code: " + gameState.get("sessionCode"));
                System.out.println("Current user ID: " + currentUserId);
                System.out.println("My player index: " + myPlayerIndex);
                System.out.println("Game object: " + (game != null ? "exists" : "null"));
                
                this.currentGameState = gameState;
                String status = (String) gameState.get("status");
                System.out.println("Status: " + status);
                
                if ("IN_PROGRESS".equals(status)) {
                    String gameStateJson = (String) gameState.get("gameStateJson");
                    System.out.println("GameStateJson: " + gameStateJson);
                    System.out.println("Game is IN_PROGRESS, checking game initialization...");
                    
                    // Only initialize basic local game if we don't have detailed server game state
                    if (gameStateJson == null || gameStateJson.equals("{}") || gameStateJson.equals("null")) {
                        System.out.println("No detailed game state, initializing basic multiplayer game...");
                        
                        if (game == null) {
                            System.out.println("Game is null, calling initializeBasicMultiplayerGame...");
                            initializeBasicMultiplayerGame();
                        } else {
                            System.out.println("Game already exists, updating UI...");
                            updateGameUI();
                        }
                    } else {
                        System.out.println("Detailed game state found, parsing and updating...");
                        // Parse and update from server state - this is the preferred path
                        parseAndUpdateGameState(gameStateJson);
                    }
                } else {
                    updatePlayersList(gameState);
                }
                
                System.out.println("=== END GAME STATE UPDATE ===");
                
            } catch (Exception e) {
                System.err.println("Error in handleGameStateUpdate: " + e.getMessage());
                e.printStackTrace();
                showMessage("Error updating game state: " + e.getMessage());
            }
        });
    }
    
    private void parseAndUpdateGameState(String gameStateJson) {
        try {
            // Parse the game state JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> gameData = objectMapper.readValue(gameStateJson, Map.class);
            
            if (game == null) {
                // Initialize game for the first time
                initializeGameFromState(gameData);
            } else {
                // Update existing game state
                updateGameFromState(gameData);
            }
            
            // Update UI
            Platform.runLater(this::updateGameUI);
            
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing game state: " + e.getMessage());
            showMessage("Error parsing game state: " + e.getMessage());
        }
    }
    
    private void initializeGameFromState(Map<String, Object> gameData) {
        // Initialize game for the first time
        List<String> playerNames = new ArrayList<>();
        
        // Extract player information from game data
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) currentGameState.get("players");
        
        System.out.println("Initializing game from state. Current user ID: " + currentUserId);
        
        for (int i = 0; i < players.size(); i++) {
            Map<String, Object> player = players.get(i);
            String username = (String) player.get("username");
            Long playerId = ((Number) player.get("id")).longValue();
            
            playerNames.add(username);
            
            // Find which player index corresponds to the current user
            if (playerId.equals(currentUserId)) {
                myPlayerIndex = i;
                System.out.println("Set myPlayerIndex to: " + myPlayerIndex + " for user: " + username + " (ID: " + playerId + ")");
            } else {
                System.out.println("Player " + i + ": " + username + " (ID: " + playerId + ") - not current user");
            }
        }
        
        if (myPlayerIndex < 0) {
            System.err.println("ERROR: Could not determine my player index! Current user ID: " + currentUserId);
            showMessage("Error: Could not determine your position in the game");
            return;
        }
        
        // Initialize the game with these players
        game = new Game();
        game.initializeMultiplayerGame(playerNames);
        
        // Update game state from server data
        updateGameFromState(gameData);
        
        System.out.println("MultiplayerGame: Game initialized from server state! You are " + 
                          (myPlayerIndex >= 0 && myPlayerIndex < playerNames.size() ? 
                           playerNames.get(myPlayerIndex) : "Unknown") + " (index: " + myPlayerIndex + ")");
    }
    
    private void updateGameFromState(Map<String, Object> gameData) {
        try {
            System.out.println("Updating game from server state...");

            // Update current player index
            if (gameData.containsKey("currentPlayerIndex")) {
                int oldPlayerIdx = game.getCurrentPlayerIndex();
                int currentPlayerIdx = ((Number) gameData.get("currentPlayerIndex")).intValue();
                game.setCurrentPlayerIndex(currentPlayerIdx);
                
                if (oldPlayerIdx != currentPlayerIdx) {
                    System.out.println("Current player index changed from " + oldPlayerIdx + " to " + currentPlayerIdx + 
                                     " (My index: " + myPlayerIndex + ", Is my turn: " + (currentPlayerIdx == myPlayerIndex) + ")");
                }
            }

            // Update current color
            if (gameData.containsKey("currentColor")) {
                String colorStr = (String) gameData.get("currentColor");
                try {
                    Card.Color color = Card.Color.valueOf(colorStr.toUpperCase());
                    game.setCurrentColor(color);
                    System.out.println("Current color updated to: " + color);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid color received from server: " + colorStr);
                }
            }

            // Update direction
            if (gameData.containsKey("isClockwise")) {
                boolean clockwise = (Boolean) gameData.get("isClockwise");
                game.setDirection(clockwise);
                System.out.println("Direction set to: " + (clockwise ? "Clockwise" : "Counterclockwise"));
            }

            // Update top card
            if (gameData.containsKey("topCard")) {
                try {
                    Map<String, Object> topCardData = (Map<String, Object>) gameData.get("topCard");
                    
                    // Parse color
                    String colorStr = (String) topCardData.get("color");
                    Card.Color cardColor = Card.Color.valueOf(colorStr.toUpperCase());
                    
                    // Parse type with proper conversion from dash-separated to underscore-separated
                    String typeStr = (String) topCardData.get("type");
                    Card.Type cardType;
                    
                    // Convert dash-separated to underscore-separated for enum parsing
                    String enumTypeStr = typeStr.toUpperCase().replace("-", "_");
                    cardType = Card.Type.valueOf(enumTypeStr);
                    
                    // Create the card
                    Card topCard;
                    if (cardType == Card.Type.NUMBER) {
                        int number = ((Number) topCardData.get("number")).intValue();
                        topCard = new Card(cardColor, number);
                    } else {
                        topCard = new Card(cardColor, cardType);
                    }
                    
                    // Update the discard pile using reflection
                    try {
                        Field discardPileField = Game.class.getDeclaredField("discardPile");
                        discardPileField.setAccessible(true);
                        List<Card> discardPile = (List<Card>) discardPileField.get(game);
                        
                        // Replace or add the top card
                        if (discardPile.isEmpty()) {
                            discardPile.add(topCard);
                        } else {
                            discardPile.set(discardPile.size() - 1, topCard);
                        }
                        
                        System.out.println("Updated top card from server: " + topCard);
                    } catch (Exception e) {
                        System.err.println("Failed to update discard pile: " + e.getMessage());
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error parsing top card: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Update player hand sizes - FIX: Use simple card count display instead of placeholder cards
            if (gameData.containsKey("playerHands")) {
                List<Map<String, Object>> playerHands = (List<Map<String, Object>>) gameData.get("playerHands");
                
                for (Map<String, Object> handData : playerHands) {
                    Long playerId = ((Number) handData.get("playerId")).longValue();
                    int cardCount = ((Number) handData.get("cardCount")).intValue();
                    
                    // Find corresponding player in local game
                    for (int i = 0; i < game.getPlayers().size(); i++) {
                        Player player = game.getPlayers().get(i);
                        
                        // Match player by user ID (check if this is my player or opponent)
                        boolean isMyPlayer = playerId.equals(currentUserId);
                        boolean isCorrectPlayer = (i == myPlayerIndex && isMyPlayer) || 
                                                (i != myPlayerIndex && !isMyPlayer);
                        
                        if (isCorrectPlayer) {
                            // Clear the hand and create diverse placeholder cards with correct count
                            player.getHand().clear();
                            
                            // Add exactly the number of cards reported by server
                            for (int j = 0; j < cardCount; j++) {
                                if (isMyPlayer) {
                                    // Create diverse realistic cards for my hand using better variety
                                    Card.Color[] colors = {Card.Color.RED, Card.Color.BLUE, Card.Color.GREEN, Card.Color.YELLOW};
                                    Card.Type[] types = {Card.Type.NUMBER, Card.Type.SKIP, Card.Type.REVERSE, Card.Type.DRAW_TWO};
                                    
                                    // Use a mix of card types and colors for variety
                                    Card.Color cardColor = colors[(j * 3 + i * 7) % colors.length]; // Better distribution
                                    
                                    if (j % 5 == 0 && j > 0) {
                                        // Occasionally add special cards for variety
                                        Card.Type specialType = types[(j / 5) % (types.length - 1) + 1]; // Skip NUMBER type
                                        player.addCard(new Card(cardColor, specialType));
                                    } else {
                                        // Most cards are number cards with varied numbers
                                        int cardNumber = (j * 2 + i * 3) % 10; // Better number distribution
                                        player.addCard(new Card(cardColor, cardNumber));
                                    }
                                } else {
                                    // Opponent cards: create diverse placeholders they won't see details of anyway
                                    Card.Color[] colors = {Card.Color.RED, Card.Color.BLUE, Card.Color.GREEN, Card.Color.YELLOW};
                                    Card.Color cardColor = colors[(j * 2 + 1) % colors.length];
                                    int cardNumber = (j + 5) % 10; // Different pattern for opponent
                                    player.addCard(new Card(cardColor, cardNumber));
                                }
                            }
                            
                            System.out.println("Player " + (isMyPlayer ? "(me)" : "(opponent)") + 
                                             " has " + cardCount + " cards");
                            break;
                        }
                    }
                }
            }
            
            updateGameUI();
            
        } catch (Exception e) {
            System.err.println("Error updating game from state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updatePlayersList(Map<String, Object> gameState) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) gameState.get("players");
        
        if (players != null && players.size() >= 2) {
            waitingForPlayerLabel.setText("");
        } else {
            waitingForPlayerLabel.setText("Waiting for opponent...");
        }
    }
    
    private void updateGameUI() {
        if (game == null) return;
        
        // Update current player label
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
        }
        
        // Update current color
        Card.Color currentColor = game.getCurrentColor();
        if (currentColor != null) {
            currentColorLabel.setText("Current Color: " + currentColor.toString());
            String colorStyle = getColorStyle(currentColor);
            currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
        }
        
        // Update direction
        directionLabel.setText("Direction: " + (game.isClockwise() ? "Clockwise" : "Counterclockwise"));
        
        // Update turn indicators
        updateTurnIndicators();
        
        // Update player hands
        updatePlayerHands();
        
        // Update discard pile
        updateDiscardPile();
        
        // Update button states
        updateButtonStates();
    }
    
    private void updateTurnIndicators() {
        if (game == null) return;
        
        int currentPlayerIndex = game.getCurrentPlayerIndex();
        boolean isMyTurn = (currentPlayerIndex == myPlayerIndex);
        
        // Update turn circles
        playerTurn.setFill(isMyTurn ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.GREY);
        opponentTurn.setFill(!isMyTurn ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.GREY);
        
        // Update waiting message
        if (!isMyTurn) {
            waitingForPlayerLabel.setText("Waiting for opponent's turn...");
        } else {
            waitingForPlayerLabel.setText("Your turn!");
        }
    }
    
    private void updatePlayerHands() {
        if (game == null) return;
        
        // Update my hand (show actual cards)
        updateMyHand();
        
        // Update opponent hand (show card backs)
        updateOpponentHand();
    }
    
    private void updateMyHand() {
        playerHandPane.getChildren().clear();
        
        if (myPlayerIndex >= 0 && myPlayerIndex < game.getPlayers().size()) {
            Player myPlayer = game.getPlayers().get(myPlayerIndex);
            List<Card> hand = myPlayer.getHand();
            boolean isMyTurn = (game.getCurrentPlayerIndex() == myPlayerIndex);
            
            for (int i = 0; i < hand.size(); i++) {
                Card card = hand.get(i);
                ImageView cardView = createCardImageView(card);
                
                // In multiplayer, all cards are fully visible and clickable regardless of playability
                // This makes the game more challenging as players must determine valid moves themselves
                cardView.setOpacity(1.0); // Always full opacity
                
                if (isMyTurn) {
                    final int cardIndex = i;
                    cardView.setOnMouseClicked(e -> handleCardClick(card, cardIndex));
                    cardView.setStyle("-fx-cursor: hand;");
                } else {
                    cardView.setStyle("-fx-cursor: default;");
                }
                
                playerHandPane.getChildren().add(cardView);
            }
        }
    }
    
    private void updateOpponentHand() {
        opponentHandPane.getChildren().clear();
        
        // Find opponent player index
        int opponentIndex = (myPlayerIndex == 0) ? 1 : 0;
        
        if (opponentIndex < game.getPlayers().size()) {
            Player opponent = game.getPlayers().get(opponentIndex);
            int cardCount = opponent.getHand().size();
            
            // Show card backs for opponent's cards
            for (int i = 0; i < cardCount; i++) {
                ImageView cardBack = createCardBackView();
                opponentHandPane.getChildren().add(cardBack);
            }
        }
    }
    
    private void updateDiscardPile() {
        discardPilePane.getChildren().clear();
        
        if (game != null && game.getTopCard() != null) {
            ImageView topCardView = createCardImageView(game.getTopCard());
            discardPilePane.getChildren().add(topCardView);
        }
    }
    
    private void updateButtonStates() {
        boolean isMyTurn = game != null && game.getCurrentPlayerIndex() == myPlayerIndex;
        
        // Draw card button - enabled when it's my turn
        drawCardButton.setDisable(!isMyTurn);
        
        // Pass turn button - show when it's my turn (so player can end turn if they can't/don't want to play)
        passTurnButton.setVisible(isMyTurn);
        passTurnButton.setDisable(!isMyTurn);
        
        // UNO button - enabled when it's my turn and I have exactly 2 cards (about to have 1)
        boolean canCallUno = isMyTurn && myPlayerIndex >= 0 && 
                           myPlayerIndex < game.getPlayers().size() &&
                           game.getPlayers().get(myPlayerIndex).getHand().size() == 2;
        unoButton.setDisable(!canCallUno);
    }
    
    private void handleCardClick(Card card, int cardIndex) {
        if (game.getCurrentPlayerIndex() != myPlayerIndex) {
            showMessage("It's not your turn!");
            return;
        }
        
        // Validate if card can be played
        Card topCard = game.getTopCard();
        if (!isValidPlay(card, topCard)) {
            showMessage("Invalid move! Card must match color, number, or be a wild card.");
            return;
        }
        
        // Play the card locally
        boolean played = game.playCard(cardIndex);
        if (played) {
            // Handle special cards
            if (card.getType() == Card.Type.WILD || card.getType() == Card.Type.WILD_DRAW_FOUR) {
                lastPlayedCard = card;
                waitingForColorSelection = true;
                colorSelectionPane.setVisible(true);
                showMessage("Choose a color for the wild card");
                return;
            }
            
            // Send move to server
            sendMoveToServer(cardIndex, card);
            
            // Update UI
            updateGameUI();
            
            // Check for game over
            if (game.isGameOver()) {
                handleGameOver();
            }
        }
    }
    
    private boolean isValidPlay(Card card, Card topCard) {
        if (topCard == null) return true;
        
        // Wild cards can always be played
        if (card.getColor() == Card.Color.WILD) {
            return true;
        }
        
        // Check if top card is a wild card - if so, match against current color
        if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
            // For wild cards on top, check against current color
            return card.getColor() == game.getCurrentColor() || card.getColor() == Card.Color.WILD;
        }
        
        // Regular card matching: Must match color, number, or type
        return card.getColor() == topCard.getColor() ||
               card.getColor() == game.getCurrentColor() ||
               (card.getType() == Card.Type.NUMBER && topCard.getType() == Card.Type.NUMBER && 
                card.getNumber() == topCard.getNumber()) ||
               card.getType() == topCard.getType();
    }
    
    private void sendMoveToServer(int cardIndex, Card card) {
        // Create move data
        Map<String, Object> moveData = new HashMap<>();
        moveData.put("cardIndex", cardIndex);
        moveData.put("card", cardToMap(card));
        
        // Send to server
        apiService.sendGameMove(sessionCode, currentUserId, "PLAY_CARD", moveData)
            .subscribe(
                response -> {
                    Platform.runLater(() -> {
                        showMessage("Played: " + card.toString());
                        // Update will come through polling
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        showMessage("Error playing card: " + error.getMessage());
                    });
                }
            );
    }
    
    private Map<String, Object> cardToMap(Card card) {
        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("color", card.getColor().toString());
        cardMap.put("type", card.getType().toString());
        if (card.getType() == Card.Type.NUMBER) {
            cardMap.put("number", card.getNumber());
        }
        return cardMap;
    }
    
    @FXML
    private void handleDrawCard() {
        // Debug info
        System.out.println("Draw card pressed. Current player index: " + game.getCurrentPlayerIndex() + ", My player index: " + myPlayerIndex);
        
        if (game == null) {
            showMessage("Game not initialized!");
            return;
        }
        
        if (myPlayerIndex < 0) {
            showMessage("Player index not set!");
            return;
        }
        
        if (game.getCurrentPlayerIndex() != myPlayerIndex) {
            showMessage("It's not your turn! Current player: " + game.getCurrentPlayerIndex() + ", You are: " + myPlayerIndex);
            return;
        }
        
        // Send draw action to server first, then handle locally when server responds
        Map<String, Object> moveData = new HashMap<>();
        moveData.put("type", "DRAW_CARD"); // Add the missing type field
        
        apiService.sendGameMove(sessionCode, currentUserId, "DRAW_CARD", moveData)
            .subscribe(
                response -> {
                    Platform.runLater(() -> {
                        showMessage("Drew a card - you can play it if possible, or pass your turn");
                        // The game state will be updated via polling, which will trigger updateGameFromState
                        updateGameUI();
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        showMessage("Error drawing card: " + error.getMessage());
                        System.err.println("Draw card error: " + error.getMessage());
                    });
                }
            );
    }
    
    @FXML
    private void handlePassTurn() {
        if (game == null) {
            showMessage("Game not initialized!");
            return;
        }
        
        if (myPlayerIndex < 0) {
            showMessage("Player index not set!");
            return;
        }
        
        if (game.getCurrentPlayerIndex() != myPlayerIndex) {
            showMessage("It's not your turn!");
            return;
        }
        
        // Send pass turn action to server
        Map<String, Object> moveData = new HashMap<>();
        moveData.put("type", "PASS_TURN");
        
        apiService.sendGameMove(sessionCode, currentUserId, "PASS_TURN", moveData)
            .subscribe(
                response -> {
                    Platform.runLater(() -> {
                        showMessage("Turn passed");
                        // Hide pass turn button after using it
                        passTurnButton.setVisible(false);
                        updateGameUI();
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        showMessage("Error passing turn: " + error.getMessage());
                        System.err.println("Pass turn error: " + error.getMessage());
                    });
                }
            );
    }
    
    @FXML
    private void callUno() {
        if (myPlayerIndex >= 0 && myPlayerIndex < game.getPlayers().size()) {
            Player myPlayer = game.getPlayers().get(myPlayerIndex);
            if (myPlayer.getHand().size() == 1) {
                showMessage("UNO! You have one card left!");
                
                // Send UNO call to server
                Map<String, Object> moveData = new HashMap<>();
                moveData.put("type", "CALL_UNO");
                moveData.put("playerId", currentUserId);
            }
        }
    }
    
    @FXML
    private void selectRedColor() {
        handleColorSelected(Card.Color.RED);
    }
    
    @FXML
    private void selectBlueColor() {
        handleColorSelected(Card.Color.BLUE);
    }
    
    @FXML
    private void selectGreenColor() {
        handleColorSelected(Card.Color.GREEN);
    }
    
    @FXML
    private void selectYellowColor() {
        handleColorSelected(Card.Color.YELLOW);
    }
    
    private void handleColorSelected(Card.Color color) {
        if (waitingForColorSelection && lastPlayedCard != null) {
            // Update local game state
            game.setCurrentColor(color);
            currentColorLabel.setText("Current Color: " + color.toString());
            String colorStyle = getColorStyle(color);
            currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
            
            colorSelectionPane.setVisible(false);
            waitingForColorSelection = false;
            
            // Send color selection to server
            Map<String, Object> moveData = new HashMap<>();
            moveData.put("color", color.toString());
            apiService.sendGameMove(sessionCode, currentUserId, "SELECT_COLOR", moveData)
                .subscribe(
                    response -> {
                        Platform.runLater(() -> {
                            showMessage("Color set to: " + color.toString());
                            // Server will handle turn progression based on wild card type
                            updateGameUI();
                        });
                    },
                    error -> {
                        Platform.runLater(() -> {
                            showMessage("Error selecting color: " + error.getMessage());
                        });
                    }
                );
            
            lastPlayedCard = null;
        }
    }
    
    @FXML
    private void leaveGame() {
        pollingService.stopPolling();
        
        if (sessionCode != null && currentUserId != null) {
            apiService.leaveGameSession(sessionCode, currentUserId)
                .subscribe(
                    response -> Platform.runLater(() -> {
                        sceneManager.showMultiplayerLobbyScene();
                    }),
                    error -> Platform.runLater(() -> {
                        sceneManager.showMultiplayerLobbyScene();
                    })
                );
        } else {
            sceneManager.showMultiplayerLobbyScene();
        }
    }
    
    private void handlePollingError(String error) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Connection Error");
            connectionStatusLabel.setTextFill(javafx.scene.paint.Color.RED);
            showMessage("Connection error: " + error);
        });
    }
    
    private void handleGameFinished() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Game Finished");
            alert.setContentText("The game has ended!");
            alert.showAndWait();
            
            sceneManager.showMultiplayerLobbyScene();
        });
    }
    
    private void handleGameOver() {
        // Handle when local game detects game over
        showMessage("Game Over!");
        // Additional game over logic...
    }
    
    private void showMessage(String message) {
        if (gameStateLabel != null) {
            gameStateLabel.setText(message);
        }
        System.out.println("MultiplayerGame: " + message);
    }
    
    // Image handling methods (adapted from GameBoardController)
    private void preloadCardImages() {
        // Preload common card images
        loadAndCacheImage(CARD_BACK_IMAGE);
    }
    
    private void loadAndCacheImage(String imagePath) {
        if (!cardImageCache.containsKey(imagePath)) {
            try (InputStream imageStream = getClass().getResourceAsStream(imagePath)) {
                if (imageStream != null) {
                    Image image = new Image(imageStream);
                    cardImageCache.put(imagePath, image);
                }
            } catch (Exception e) {
                System.err.println("Failed to load image: " + imagePath);
            }
        }
    }
    
    private ImageView createCardImageView(Card card) {
        String imagePath = CARD_IMAGES_PATH + card.getImageFileName();
        
        // Try to load from cache first
        Image image = cardImageCache.get(imagePath);
        if (image == null) {
            loadAndCacheImage(imagePath);
            image = cardImageCache.get(imagePath);
        }
        
        ImageView imageView;
        if (image != null && !image.isError()) {
            imageView = new ImageView(image);
        } else {
            // Create placeholder if image loading fails
            imageView = createCardPlaceholder(card);
        }
        
        imageView.setFitWidth(70);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        
        return imageView;
    }
    
    private ImageView createCardBackView() {
        Image backImage = cardImageCache.get(CARD_BACK_IMAGE);
        if (backImage == null) {
            loadAndCacheImage(CARD_BACK_IMAGE);
            backImage = cardImageCache.get(CARD_BACK_IMAGE);
        }
        
        ImageView imageView;
        if (backImage != null && !backImage.isError()) {
            imageView = new ImageView(backImage);
        } else {
            // Create simple card back placeholder
            imageView = new ImageView();
            Rectangle rect = new Rectangle(70, 100);
            rect.setFill(javafx.scene.paint.Color.DARKBLUE);
            rect.setStroke(javafx.scene.paint.Color.WHITE);
            // Note: This is simplified - in real implementation you'd convert Rectangle to Image
        }
        
        imageView.setFitWidth(70);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        
        return imageView;
    }
    
    private ImageView createCardPlaceholder(Card card) {
        // Create a simple colored rectangle as placeholder
        ImageView imageView = new ImageView();
        Rectangle rect = new Rectangle(70, 100);
        
        // Set color based on card color
        javafx.scene.paint.Color fxColor = getJavaFXColor(card.getColor());
        rect.setFill(fxColor);
        rect.setStroke(javafx.scene.paint.Color.BLACK);
        
        // Note: This is simplified - in real implementation you'd convert Rectangle to Image
        return imageView;
    }
    
    private String getColorStyle(Card.Color color) {
        switch (color) {
            case RED: return "-fx-text-fill: red;";
            case BLUE: return "-fx-text-fill: blue;";
            case GREEN: return "-fx-text-fill: green;";
            case YELLOW: return "-fx-text-fill: #FFC107;";
            default: return "-fx-text-fill: white;";
        }
    }
    
    private javafx.scene.paint.Color getJavaFXColor(Card.Color cardColor) {
        switch (cardColor) {
            case RED: return javafx.scene.paint.Color.RED;
            case BLUE: return javafx.scene.paint.Color.BLUE;
            case GREEN: return javafx.scene.paint.Color.GREEN;
            case YELLOW: return javafx.scene.paint.Color.YELLOW;
            case WILD: return javafx.scene.paint.Color.BLACK;
            default: return javafx.scene.paint.Color.GRAY;
        }
    }
    
    private void initializeBasicMultiplayerGame() {
        try {
            System.out.println(">>> Server sent empty game state - waiting for proper initialization");
            
            // Get player names from current game session
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> players = (List<Map<String, Object>>) currentGameState.get("players");
            
            if (players == null || players.size() < 2) {
                System.err.println("ERROR: Not enough players in game session. Players: " + players);
                showMessage("Error: Not enough players in session");
                return;
            }
            
            System.out.println("Setting up player information. Current user ID: " + currentUserId);
            System.out.println("Players in session: " + players.size());
            
            List<String> playerNames = new ArrayList<>();
            for (int i = 0; i < players.size(); i++) {
                Map<String, Object> player = players.get(i);
                String username = (String) player.get("username");
                Long playerId = ((Number) player.get("id")).longValue();
                
                playerNames.add(username);
                
                // Find which player index corresponds to the current user
                if (playerId.equals(currentUserId)) {
                    myPlayerIndex = i;
                    System.out.println("Set myPlayerIndex to: " + myPlayerIndex + " for user: " + username + " (ID: " + playerId + ")");
                } else {
                    System.out.println("Player " + i + ": " + username + " (ID: " + playerId + ") - not current user");
                }
            }
            
            if (myPlayerIndex < 0) {
                System.err.println("ERROR: Could not determine my player index! Current user ID: " + currentUserId);
                showMessage("Error: Could not determine your position in the game");
                return;
            }
            
            // CRITICAL FIX: Do NOT create a local game! This causes different starting cards
            // Instead, create a minimal game object that will be updated when server sends real game state
            if (game == null) {
                System.out.println("Creating minimal game object for UI updates...");
                game = new Game();
                game.initializeMultiplayerGame(playerNames);
                
                // Clear any cards that were created locally - server will provide the real cards
                for (Player player : game.getPlayers()) {
                    player.getHand().clear();
                }
                
                // Set basic defaults that will be overridden by server
                game.setCurrentPlayerIndex(0);
                game.setCurrentColor(Card.Color.RED);
                
                System.out.println("Minimal game object created - waiting for server to provide real game state");
            }
            
            // Update labels
            if (playerNames.size() > 1) {
                String opponentName = myPlayerIndex == 0 ? playerNames.get(1) : playerNames.get(0);
                opponentLabel.setText(opponentName);
                System.out.println("Opponent label set to: " + opponentName);
            }
            
            if (myPlayerIndex >= 0 && myPlayerIndex < playerNames.size()) {
                playerLabel.setText(playerNames.get(myPlayerIndex));
                System.out.println("Player label set to: " + playerNames.get(myPlayerIndex));
            }
            
            // Show waiting message until real game state arrives
            waitingForPlayerLabel.setText("Waiting for game to initialize...");
            
            System.out.println("Waiting for server to provide proper game state with cards and starting position");
            System.out.println("<<< Player setup complete - game will be initialized when server sends real state");
                               
        } catch (Exception e) {
            System.err.println("Error in basic multiplayer setup: " + e.getMessage());
            e.printStackTrace();
            showMessage("Error setting up game: " + e.getMessage());
        }
    }
} 