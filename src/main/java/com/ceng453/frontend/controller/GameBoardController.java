package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.Card;
import com.ceng453.frontend.model.Game;
import com.ceng453.frontend.model.Player;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.effect.ColorAdjust;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javafx.scene.Cursor;
import java.util.Map;

@Controller
public class GameBoardController {
    // Image paths
    private static final String CARD_IMAGES_PATH = "/images/cards/";
    private static final String CARD_BACK_IMAGE = "/images/card_back.png";
    
    // Spring-injected dependencies
    private final SceneManager sceneManager;
    private final ApiService apiService;
    
    // Game state
    private Game game;
    private Card lastPlayedCard;
    private boolean waitingForColorSelection;
    private boolean wildDrawFourPlayed;  // Added flag to track if Wild Draw Four was played
    
    // FXML elements
    @FXML private Label currentPlayerLabel;
    @FXML private Label currentColorLabel;
    @FXML private Label directionLabel;
    @FXML private Label gameStateLabel;
    
    @FXML private FlowPane playerHandPane;
    @FXML private FlowPane cpu1HandPane;
    @FXML private FlowPane cpu2HandPane;
    @FXML private FlowPane cpu3HandPane;
    
    @FXML private StackPane discardPilePane;
    @FXML private HBox colorSelectPane;
    
    @FXML private Circle playerTurn;
    @FXML private Circle cpu1Turn;
    @FXML private Circle cpu2Turn;
    @FXML private Circle cpu3Turn;
    
    @FXML private Label playerLabel;
    @FXML private Label cpu1Label;
    @FXML private Label cpu2Label;
    @FXML private Label cpu3Label;
    
    @FXML private HBox playerHandContainer;
    @FXML private Button drawCardButton;
    @FXML private Button unoButton;
    @FXML private Button fullscreenButton;
    
    // Cheat buttons
    @FXML private VBox cheatButtonsContainer;
    @FXML private Button cheatSkipButton;
    @FXML private Button cheatReverseButton;
    @FXML private Button cheatDrawTwoButton;
    @FXML private Button cheatWildButton;
    @FXML private Button cheatWildDrawFourButton;
    @FXML private Button cheatSkipAllButton;
    @FXML private Button cheatColorDrawButton;
    @FXML private Button cheatSwapHandsButton;
    
    // Flag to track if CPU turn sequence is already in progress
    private boolean isCpuTurnInProgress = false;
    
    public GameBoardController(SceneManager sceneManager, ApiService apiService) {
        this.sceneManager = sceneManager;
        this.apiService = apiService;
    }
    
    private boolean isHumanTurn() {
        return game.getCurrentPlayerIndex() == 0;
    }
    
    private void handleGameOver(Player winner) {
        String message = winner.isHuman() ? 
                "Congratulations! You won!" : 
                winner.getName() + " has won the game!";
        
        Platform.runLater(() -> {
            // Show game over alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(winner.isHuman() ? "Congratulations!" : "Game Over");
            alert.setContentText(message);
            alert.showAndWait();
            
            // Record game result in the database
            recordGameResult(winner);
            
            // Return to main menu
            sceneManager.showMainMenuScene();
        });
    }
    
    private void showMessage(String message) {
        if (gameStateLabel != null) {
            gameStateLabel.setText(message);
        } else {
            System.out.println("Message: " + message);
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
            // First handle the wild card itself
            if (wildDrawFourPlayed) {
                // Set color for Wild Draw Four
                game.setCurrentColor(color);
                System.out.println("Wild Draw Four color set to: " + color);
                
                // For Wild Draw Four, the moveToNextPlayer is already handled in handleActionCard
                // so we don't need to do it again here
            } else if (lastPlayedCard.getType() == Card.Type.COLOR_DRAW) {
                // Handle Color Draw card
                game.handleColorDraw(color);
                System.out.println("Color Draw color set to: " + color);
            } else {
                // For regular Wild cards, just set the color
                game.setCurrentColor(color);
                System.out.println("Wild color set to: " + color);
                
                // For regular Wild cards, we need to manually move to the next player
                // since handleActionCard doesn't do it for WILD type
                game.moveToNextPlayer();
            }
            
            // Update UI to show the selected color
            if (currentColorLabel != null) {
                currentColorLabel.setText("Current Color: " + color.toString());
                String colorStyle = getColorStyle(color);
                currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
            }
            
            // Hide the color selection pane
            if (colorSelectPane != null) {
                colorSelectPane.setVisible(false);
            }
            waitingForColorSelection = false;
            
            // Reset wild card state
            wildDrawFourPlayed = false;
            lastPlayedCard = null;
            
            // After playing a Wild card and selecting a color, inform the player about the color choice
            showMessage("Color set to: " + color);
            
            // Update the UI to immediately reflect the turn change
            updateGameUI();
            
            // Debug the game state after color selection
            debugGameState();
            
            // If it's a CPU's turn now, play it
            if (!isHumanTurn()) {
                startCpuTurnSequence();
            }
        }
    }
    
    @FXML
    public void initialize() {
        try {
            // Get the player name from the current user
            String playerName = sceneManager.getCurrentUser() != null ? 
                    sceneManager.getCurrentUser().getUsername() : "Player";
            
            // Initialize a new game with one human player and three CPU players
            game = new Game();
            game.initializeSinglePlayerGame(playerName);
            
            // Keep wild cards in the deck for normal gameplay
            // game.getDeck().removeWildCards();  // Commented out to ensure wild cards stay in the deck
            
            // Preload all card images to prevent loading issues
            preloadCardImages();
            
            // Hide color selection pane initially
            if (colorSelectPane != null) {
                colorSelectPane.setVisible(false);
            }
            waitingForColorSelection = false;
            wildDrawFourPlayed = false;
            
            // Initial update of all UI elements
            updateGameUI();
            
            // Set up cheat buttons
            setupCheatButtons();
            
            // Automatically play for CPU if they start first
            if (!isHumanTurn()) {
                startCpuTurnSequence();
            }
        } catch (Exception e) {
            showMessage("Error initializing game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeGame() {
        // Create a new game
        game = new Game();
        
        // Initialize with the player's name (or "Player" if not logged in)
        String playerName = sceneManager.getCurrentUser() != null ? 
                sceneManager.getCurrentUser().getUsername() : "Player";
        
        // Start a single player game with 3 CPU opponents
        game.initializeSinglePlayerGame(playerName);
        
        // Update the UI
        updateGameUI();
    }
    
    private void updateGameUI() {
        try {
            // First check if the game is over and a CPU has won
            if (game.isGameOver()) {
                // Find the winner
                for (Player player : game.getPlayers()) {
                    if (player.getHand().isEmpty()) {
                        // This player has won, show the game over screen
                        handleGameOver(player);
                        // No need to update the UI further
                        return;
                    }
                }
            }
            
            // Update all visual elements
            updateDiscardPile();
            updatePlayerHand();
            updateCPUHandPanes();
            updateTurnIndicators();
            updateUnoIndicators();
            updateColorDisplay();
            
            // Update current player label
            if (currentPlayerLabel != null) {
                Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
                currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
            }
            
            // Update direction indicator
            if (directionLabel != null) {
                directionLabel.setText("Direction: " + (game.isClockwise() ? "Clockwise" : "Counter-Clockwise"));
            }
            
            // Debug game state
            debugGameState();
            
            // Clear any previous messages
            if (gameStateLabel != null) {
                gameStateLabel.setText("");
            }
            
            // Check for penalties that must be handled automatically, but only if it's human's turn
            if (isHumanTurn() && !waitingForColorSelection) {
                checkForPendingPenalties();
            }
            
            // Important: Only trigger CPU turns from here if we're not already in a CPU turn sequence
            // This prevents double-triggering of CPU turns
            if (!isHumanTurn() && !game.isGameOver() && !waitingForColorSelection && 
                !isCpuTurnInProgress) {
                startCpuTurnSequence();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating game UI: " + e.getMessage());
        }
    }
    
    // Method to check for and handle Draw Two or Draw Four penalties for human player
    private void checkForPendingPenalties() {
        // Check for Draw Four penalty
        if (game.getDrawFourCounter() > 0) {
            boolean hasWildDrawFour = false;
            
            // Check if player has a Wild Draw Four to stack
            for (Card card : game.getPlayers().get(0).getHand()) {
                if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                    hasWildDrawFour = true;
                    break;
                }
            }
            
            // If player has no Wild Draw Four, automatically draw cards and skip turn
            if (!hasWildDrawFour) {
                int cardsToDraw = game.getDrawFourCounter();
                showMessage("You don't have a Wild Draw Four to stack. Drawing " + cardsToDraw + " cards and skipping your turn.");
                
                // Handle penalty using the game's mechanism
                game.handleDrawFourStack();
                
                // Update UI
                updateGameUI();
            }
        }
        
        // Check for Draw Two penalty
        else if (game.getDrawTwoCounter() > 0) {
            boolean hasDrawTwo = false;
            
            // Check if player has a Draw Two to stack
            for (Card card : game.getPlayers().get(0).getHand()) {
                if (card.getType() == Card.Type.DRAW_TWO) {
                    hasDrawTwo = true;
                    break;
                }
            }
            
            // If player has no Draw Two, automatically draw cards and skip turn
            if (!hasDrawTwo) {
                int cardsToDraw = game.getDrawTwoCounter();
                showMessage("You don't have a Draw Two to stack. Drawing " + cardsToDraw + " cards and skipping your turn.");
                
                // Handle penalty using the game's mechanism
                game.handleDrawTwoStack();
                
                // Update UI
                updateGameUI();
            }
        }
    }
    
    // Added this method to debug game state
    private void debugGameState() {
        // Get current player to check for Wild Draw Four playability
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        boolean canPlayWildDrawFour = game.getDrawFourCounter() > 0 || 
                                     !game.hasValidCardOtherThanWildDrawFour(currentPlayer, game.getTopCard());
        
        System.out.println("------ GAME STATE DEBUG ------");
        System.out.println("Current Player: " + game.getCurrentPlayerIndex() + " (" + 
                           game.getPlayers().get(game.getCurrentPlayerIndex()).getName() + ")");
        System.out.println("Current Color: " + game.getCurrentColor());
        System.out.println("Draw Four Counter: " + game.getDrawFourCounter());
        System.out.println("Draw Two Counter: " + game.getDrawTwoCounter());
        System.out.println("Top Card: " + (game.getTopCard() != null ? game.getTopCard().toString() : "None"));
        System.out.println("Clockwise: " + game.isClockwise());
        System.out.println("Waiting for color selection: " + waitingForColorSelection);
        System.out.println("WildDrawFour played: " + wildDrawFourPlayed);
        System.out.println("Can play Wild Draw Four: " + canPlayWildDrawFour);
        System.out.println("-----------------------------");
    }
    
    // Added method to update color display
    private void updateColorDisplay() {
        // Update current color indicator
        if (currentColorLabel != null) {
            Card.Color currentColor = game.getCurrentColor();
            currentColorLabel.setText("Current Color: " + currentColor.toString());
            String colorStyle = getColorStyle(currentColor);
            currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
        }
    }
    
    private String getColorStyle(Card.Color color) {
        return switch (color) {
            case RED -> "-fx-text-fill: #ff4136;";
            case BLUE -> "-fx-text-fill: #0074D9;";
            case GREEN -> "-fx-text-fill: #2ECC40;";
            case YELLOW -> "-fx-text-fill: #FFDC00;";
            default -> "-fx-text-fill: white;";
        };
    }
    
    private Color getJavaFXColor(Card.Color cardColor) {
        switch (cardColor) {
            case RED:
                return Color.RED;
            case BLUE:
                return Color.BLUE;
            case GREEN:
                return Color.GREEN;
            case YELLOW:
                return Color.YELLOW;
            default:
                return Color.BLACK;
        }
    }
    
    private void updatePlayerHand() {
        try {
            if (playerHandPane != null) {
                playerHandPane.getChildren().clear();
                
                // Get the human player's hand
                List<Card> hand = game.getPlayers().get(0).getHand();
                Card topCard = game.getTopCard();
                
                // Create an image view for each card
                for (Card card : hand) {
                    ImageView cardView = createCardImageView(card);
                    cardView.setFitWidth(80);
                    cardView.setFitHeight(120);
                    
                    // Check if card is playable
                    boolean isPlayable = false;
                    
                    // Only apply playability checks and visual fading when it's the player's turn
                    if (isHumanTurn()) {
                        // First check if there are any Draw Four or Draw Two stacks active
                        if (game.getDrawFourCounter() > 0) {
                            // Can only play Wild Draw Four on a Draw Four stack
                            isPlayable = (card.getType() == Card.Type.WILD_DRAW_FOUR);
                        } else if (game.getDrawTwoCounter() > 0) {
                            // Can only play Draw Two on a Draw Two stack
                            isPlayable = (card.getType() == Card.Type.DRAW_TWO);
                        } else {
                            // Bonus cards are always playable
                            if (card.getType() == Card.Type.SKIP_ALL || card.getType() == Card.Type.COLOR_DRAW || card.getType() == Card.Type.SWAP_HANDS) {
                                isPlayable = true;
                            }
                            // Any card can be played on top of Skip All, Color Draw, or Swap Hands
                            else if (topCard.getType() == Card.Type.SKIP_ALL || topCard.getType() == Card.Type.COLOR_DRAW || 
                                    topCard.getType() == Card.Type.SWAP_HANDS) {
                                isPlayable = true;
                            }
                            // Regular playability check
                            else if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                                // For wild cards, check against the current color
                                isPlayable = (card.getColor() == game.getCurrentColor() || card.getColor() == Card.Color.WILD);
                            } else {
                                // Regular card matching
                                isPlayable = card.canBePlayedOn(topCard);
                            }
                        }
                        
                        // Special check for Wild Draw Four - only playable if there are no matching cards
                        if (isPlayable && card.getType() == Card.Type.WILD_DRAW_FOUR) {
                            // Check if player has any cards matching the current color
                            boolean hasMatchingCards = false;
                            for (Card handCard : game.getPlayers().get(0).getHand()) {
                                // Skip the Wild Draw Four card itself
                                if (handCard == card) continue;
                                
                                // Check if any card matches color or is a regular wild
                                if ((topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) && 
                                    (handCard.getColor() == game.getCurrentColor() || 
                                    (handCard.getColor() == Card.Color.WILD && handCard.getType() != Card.Type.WILD_DRAW_FOUR))) {
                                    hasMatchingCards = true;
                                    break;
                                } else if (handCard.getColor() == topCard.getColor() || 
                                          handCard.getType() == topCard.getType() || 
                                          handCard.getColor() == Card.Color.WILD) {
                                    hasMatchingCards = true;
                                    break;
                                }
                            }
                            
                            // Only mark as playable if player has no matching cards
                            if (hasMatchingCards) {
                                isPlayable = false;
                            }
                        }
                    } else {
                        // Not player's turn - all cards appear normal
                        cardView.setOpacity(1.0);
                        cardView.setEffect(null);
                    }
                    
                    // Apply visual effect based on playability
                    if (!isPlayable) {
                        // Make unplayable cards appear faded
                        cardView.setOpacity(0.5);
                        cardView.setEffect(new ColorAdjust(0, -0.5, -0.5, 0)); // Reduce saturation and brightness
                    } else {
                        // Highlight playable cards
                        cardView.setOpacity(1.0);
                        cardView.setEffect(null);
                    }
                    
                    // Add click handler for player's cards, but only if it's their turn
                    int cardIndex = hand.indexOf(card);
                    if (isHumanTurn() && !waitingForColorSelection) {
                        cardView.setOnMouseClicked(event -> handleCardClick(card, cardIndex));
                        cardView.setCursor(Cursor.HAND); // Show hand cursor to indicate clickability
                    } else {
                        cardView.setOnMouseClicked(null); // Remove click handler when it's not player's turn
                        cardView.setCursor(Cursor.DEFAULT); // Use default cursor
                        cardView.setOpacity(0.7); // Make cards appear slightly faded when not playable
                    }
                    
                    playerHandPane.getChildren().add(cardView);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating player hand: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateCPUHandPanes() {
        updateCPUHandPane(cpu1HandPane, game.getPlayers().get(1));
        updateCPUHandPane(cpu2HandPane, game.getPlayers().get(2));
        updateCPUHandPane(cpu3HandPane, game.getPlayers().get(3));
    }
    
    private void updateCPUHandPane(FlowPane pane, Player cpuPlayer) {
        try {
            if (pane != null) {
                pane.getChildren().clear();
                
                // Show CPU cards face-up for testing purposes as requested by professor
                for (Card card : cpuPlayer.getHand()) {
                    try {
                        // Use the same card image creation logic as for player cards
                        ImageView cardView = createCardImageView(card);
                        cardView.setFitWidth(60);
                        cardView.setFitHeight(90);
                        pane.getChildren().add(cardView);
                    } catch (Exception e) {
                        System.err.println("Error loading CPU card image: " + e.getMessage());
                        
                        // Fallback if there's an error
                        Rectangle rect = new Rectangle(60, 90, Color.DARKGRAY);
                        rect.setStroke(Color.BLACK);
                        rect.setStrokeWidth(2);
                        Text cardText = new Text(card.toString());
                        StackPane stack = new StackPane();
                        stack.getChildren().addAll(rect, cardText);
                        pane.getChildren().add(stack);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating CPU hand: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateDiscardPile() {
        try {
            if (discardPilePane != null) {
                discardPilePane.getChildren().clear();
                
                // Get the top card
                Card topCard = game.getTopCard();
                
                if (topCard != null) {
                    // Create image view for the top card
                    ImageView cardView = createCardImageView(topCard);
                    cardView.setFitWidth(100);
                    cardView.setFitHeight(150);
                    
                    // Add a label showing the current color if it's a wild card
                    if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                        Label colorLabel = new Label("Current Color: " + game.getCurrentColor().toString());
                        colorLabel.setTextFill(getJavaFXColor(game.getCurrentColor()));
                        colorLabel.setStyle("-fx-font-weight: bold;");
                        
                        VBox vbox = new VBox(5);
                        vbox.setAlignment(Pos.CENTER);
                        vbox.getChildren().addAll(cardView, colorLabel);
                        
                        discardPilePane.getChildren().add(vbox);
                    } else {
                        discardPilePane.getChildren().add(cardView);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating discard pile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Cache for card images to prevent reloading issues
    private static final Map<String, Image> cardImageCache = new HashMap<>();
    // Hard-coded fallback images for problematic cards
    private static final String[] KNOWN_PROBLEMATIC_CARDS = {
        "wild.png", "wild-draw-four.png",
        "wild-skip-all.png", "wild-color-draw.png", "wild-swap-hands.png",
        "red_draw-two.png", "blue_draw-two.png", "green_draw-two.png", "yellow_draw-two.png",
        "red_skip.png", "blue_skip.png", "green_skip.png", "yellow_skip.png",
        "red_reverse.png", "blue_reverse.png", "green_reverse.png", "yellow_reverse.png"
    };
    
    // Preload all card images at startup to avoid loading issues later
    private void preloadCardImages() {
        System.out.println("Preloading card images...");
        // Preload all card images for each color and type
        for (Card.Color color : Card.Color.values()) {
            if (color == Card.Color.WILD) continue; // Handle wild cards separately
            
            // Number cards (0-9)
            for (int i = 0; i < 10; i++) {
                String imagePath = CARD_IMAGES_PATH + color.toString() + "_" + i + ".png";
                loadAndCacheImage(imagePath);
            }
            
            // Action cards
            loadAndCacheImage(CARD_IMAGES_PATH + color.toString() + "_skip.png");
            loadAndCacheImage(CARD_IMAGES_PATH + color.toString() + "_reverse.png");
            loadAndCacheImage(CARD_IMAGES_PATH + color.toString() + "_draw-two.png");
        }
        
        // Wild cards - using the correct file naming pattern
        loadAndCacheImage(CARD_IMAGES_PATH + "wild.png");
        loadAndCacheImage(CARD_IMAGES_PATH + "wild-draw-four.png");
        
        // Bonus cards
        loadAndCacheImage(CARD_IMAGES_PATH + "wild-skip-all.png");
        loadAndCacheImage(CARD_IMAGES_PATH + "wild-color-draw.png");
        loadAndCacheImage(CARD_IMAGES_PATH + "wild-swap-hands.png");
        
        System.out.println("Card image preloading complete. Cached " + cardImageCache.size() + " images.");
    }
    
    private void loadAndCacheImage(String imagePath) {
        try {
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            if (imageStream != null) {
                Image image = new Image(imageStream);
                cardImageCache.put(imagePath, image);
            } else {
                // Try alternative naming for known problematic cards
                String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
                if (Arrays.asList(KNOWN_PROBLEMATIC_CARDS).contains(fileName)) {
                    // Try alternative naming (with dash instead of underscore)
                    String altPath = imagePath.replace("_", "-");
                    imageStream = getClass().getResourceAsStream(altPath);
                    if (imageStream != null) {
                        Image image = new Image(imageStream);
                        cardImageCache.put(imagePath, image); // Cache with original path for lookup
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to preload image: " + imagePath + ", " + e.getMessage());
        }
    }
    
    private ImageView createCardImageView(Card card) {
        try {
            String imageName = card.getImageFileName();
            String imagePath = CARD_IMAGES_PATH + imageName;
            
            // Check cache first to avoid resource stream issues after game restarts
            Image cardImage = cardImageCache.get(imagePath);
            
            if (cardImage == null) {
                // Special handling for wild cards and bonus cards
                if (card.getType() == Card.Type.WILD || card.getType() == Card.Type.WILD_DRAW_FOUR ||
                    card.getType() == Card.Type.SKIP_ALL || card.getType() == Card.Type.COLOR_DRAW ||
                    card.getType() == Card.Type.SWAP_HANDS) {
                    
                    // Try different naming patterns for wild cards
                    String[] possiblePaths = {
                        CARD_IMAGES_PATH + imageName,
                        CARD_IMAGES_PATH + "wild.png",  // Direct path for basic wild
                        CARD_IMAGES_PATH + "wild-draw-four.png",  // Direct path for wild draw four
                        CARD_IMAGES_PATH + "wild-" + card.getType().toString() + ".png",
                        CARD_IMAGES_PATH + card.getType().toString() + ".png"
                    };
                    
                    for (String path : possiblePaths) {
                        InputStream imageStream = getClass().getResourceAsStream(path);
                        if (imageStream != null) {
                            cardImage = new Image(imageStream);
                            cardImageCache.put(imagePath, cardImage); // Cache with original path
                            System.out.println("Successfully loaded wild card image from: " + path);
                            break;
                        }
                    }
                } else {
                    // Try loading with alternative naming (handling inconsistencies)
                    String altImageName = imageName.replace("_", "-");
                    String altImagePath = CARD_IMAGES_PATH + altImageName;
                    cardImage = cardImageCache.get(altImagePath);
                    
                    if (cardImage == null) {
                        // Last attempt - load directly
                        InputStream imageStream = getClass().getResourceAsStream(imagePath);
                        
                        if (imageStream != null) {
                            cardImage = new Image(imageStream);
                            // Cache for future use
                            cardImageCache.put(imagePath, cardImage);
                        } else {
                            // Try alternative path with dash instead of underscore
                            imageStream = getClass().getResourceAsStream(altImagePath);
                            if (imageStream != null) {
                                cardImage = new Image(imageStream);
                                cardImageCache.put(imagePath, cardImage);
                            } else {
                                // Only log warning once per missing image
                                if (!cardImageCache.containsKey(imagePath)) {
                                    System.err.println("Card image not found: " + imagePath);
                                    cardImageCache.put(imagePath, null); // Mark as attempted
                                }
                                return createCardPlaceholder(card);
                            }
                        }
                    }
                }
            }
            
            if (cardImage != null) {
                return new ImageView(cardImage);
            } else {
                return createCardPlaceholder(card);
            }
        } catch (Exception e) {
            System.err.println("Error creating card image view: " + e.getMessage());
            return createCardPlaceholder(card);
        }
    }
    
    private ImageView createCardPlaceholder(Card card) {
        try {
            // Create a colored rectangle as a placeholder
            Rectangle rect = new Rectangle(80, 120);
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            rect.setStroke(Color.WHITE);
            rect.setStrokeWidth(2);
            
            // Set the fill color based on the card color
            if (card.getColor() == Card.Color.RED) {
                rect.setFill(Color.RED);
            } else if (card.getColor() == Card.Color.BLUE) {
                rect.setFill(Color.BLUE);
            } else if (card.getColor() == Card.Color.GREEN) {
                rect.setFill(Color.GREEN);
            } else if (card.getColor() == Card.Color.YELLOW) {
                rect.setFill(Color.YELLOW);
            } else {
                rect.setFill(Color.BLACK);
            }
            
            // Create a label for the card number/type
            Label label = new Label();
            if (card.getType() == Card.Type.NUMBER) {
                label.setText(String.valueOf(card.getNumber()));
            } else {
                String text = card.getType().toString();
                // Abbreviate long names
                if (text.length() > 6) {
                    text = text.substring(0, 6);
                }
                label.setText(text);
            }
            label.setTextFill(Color.WHITE);
            label.setFont(Font.font("System", FontWeight.BOLD, 18));
            
            // Use a stack pane to overlay the label on the rectangle
            StackPane cardPane = new StackPane();
            cardPane.getChildren().addAll(rect, label);
            
            // Convert to ImageView
            Image snapshot = cardPane.snapshot(null, null);
            return new ImageView(snapshot);
        } catch (Exception e) {
            System.err.println("Error creating card placeholder: " + e.getMessage());
            e.printStackTrace();
            
            // Last resort fallback
            Rectangle rect = new Rectangle(80, 120, Color.DARKGRAY);
            return new ImageView(rect.snapshot(null, null));
        }
    }
    
    private void updateUnoIndicators() {
        // Reset all UNO indicators
        if (playerTurn != null) playerTurn.setFill(Color.DARKGREY);
        if (cpu1Turn != null) cpu1Turn.setFill(Color.DARKGREY);
        if (cpu2Turn != null) cpu2Turn.setFill(Color.DARKGREY);
        if (cpu3Turn != null) cpu3Turn.setFill(Color.DARKGREY);
        
        // Check each player's hand size and update indicators
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            
            // Show UNO indicator if player has exactly 1 card OR has called UNO with 2 cards
            if (player.getHand().size() == 1 || 
                (player.getHand().size() == 2 && player.hasCalledUno())) {
                
                // Change indicator to bright red for UNO
                Color unoColor = Color.RED;
                
                switch (i) {
                    case 0:
                        if (playerTurn != null) {
                            playerTurn.setFill(unoColor);
                            // Add border around the indicator
                            playerTurn.setStroke(Color.YELLOW);
                            playerTurn.setStrokeWidth(2);
                        }
                        // If player has called UNO with 2 cards, also show in the console
                        if (player.getHand().size() == 2 && player.hasCalledUno()) {
                            System.out.println("Human player UNO status: Called UNO with 2 cards");
                        }
                        break;
                    case 1:
                        if (cpu1Turn != null) {
                            cpu1Turn.setFill(unoColor);
                            cpu1Turn.setStroke(Color.YELLOW);
                            cpu1Turn.setStrokeWidth(2);
                        }
                        if (player.hasCalledUno()) {
                            System.out.println("CPU 1 UNO status: " + (player.getHand().size() == 1 ? "Has 1 card" : "Called UNO with 2 cards"));
                        }
                        break;
                    case 2:
                        if (cpu2Turn != null) {
                            cpu2Turn.setFill(unoColor);
                            cpu2Turn.setStroke(Color.YELLOW);
                            cpu2Turn.setStrokeWidth(2);
                        }
                        if (player.hasCalledUno()) {
                            System.out.println("CPU 2 UNO status: " + (player.getHand().size() == 1 ? "Has 1 card" : "Called UNO with 2 cards"));
                        }
                        break;
                    case 3:
                        if (cpu3Turn != null) {
                            cpu3Turn.setFill(unoColor);
                            cpu3Turn.setStroke(Color.YELLOW);
                            cpu3Turn.setStrokeWidth(2);
                        }
                        if (player.hasCalledUno()) {
                            System.out.println("CPU 3 UNO status: " + (player.getHand().size() == 1 ? "Has 1 card" : "Called UNO with 2 cards"));
                        }
                        break;
                }
            }
        }
    }
    
    private void handleCardClick(Card card, int cardIndex) {
        // Check if we're waiting for color selection - this takes precedence
        if (waitingForColorSelection) {
            showMessage("Please select a color first!");
            return;
        }
        
        // Check if it's the human player's turn
        if (!isHumanTurn()) {
            showMessage("It's not your turn!");
            return;
        }
        
        // Now proceed with card handling logic
        Card topCard = game.getTopCard();
        
        // Check if there's a Draw Four counter active
        if (game.getDrawFourCounter() > 0) {
            // Can only play a Wild Draw Four on a Draw Four stack
            if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                lastPlayedCard = card;
                wildDrawFourPlayed = true;
                
                // Remove the card from hand and add to discard pile
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Wild Draw Four!");
                    return;
                }
                
                // Show color selection pane
                if (colorSelectPane != null) {
                    colorSelectPane.setVisible(true);
                }
                waitingForColorSelection = true;
                showMessage("Select a color for your Wild Draw Four");
            } else {
                showMessage("You must play a Wild Draw Four or draw " + game.getDrawFourCounter() + " cards!");
            }
            return;
        }
        
        // Check if there's a Draw Two counter active
        if (game.getDrawTwoCounter() > 0) {
            // Can only play a Draw Two on a Draw Two stack
            if (card.getType() == Card.Type.DRAW_TWO) {
                boolean played = game.playCard(cardIndex);
                if (played) {
                    updateGameUI();
                    // After playing Draw Two, update game state and let the next player handle the Draw Two stack
                    // We don't need to manually move to the next player as game.playCard will handle this through handleActionCard
                    updateGameUI();
                    if (!isHumanTurn()) {
                        startCpuTurnSequence();
                    }
                } else {
                    showMessage("Failed to play Draw Two!");
                }
            } else {
                showMessage("You must play a Draw Two or draw " + game.getDrawTwoCounter() + " cards!");
            }
            return;
        }
        
        // Check if the card can be played
        boolean canPlay = false;
        
        // Bonus cards are always playable
        if (card.getType() == Card.Type.SKIP_ALL || card.getType() == Card.Type.COLOR_DRAW || card.getType() == Card.Type.SWAP_HANDS) {
            canPlay = true;
            System.out.println("Bonus card " + card.getType() + " is always playable");
        }
        // Any card can be played on top of Skip All, Color Draw, or Swap Hands
        else if (topCard.getType() == Card.Type.SKIP_ALL || topCard.getType() == Card.Type.COLOR_DRAW || 
                topCard.getType() == Card.Type.SWAP_HANDS) {
            canPlay = true;
            System.out.println("Card can be played because " + topCard.getType() + " allows any card to be played on top of it");
        }
        // If the top card is a wild card, check against the current color
        else if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
            canPlay = (card.getColor() == game.getCurrentColor() || card.getColor() == Card.Color.WILD);
            System.out.println("Checking if card " + card + " can be played on wild card with color " + game.getCurrentColor() + ": " + canPlay);
        } else {
            // Regular card matching
            canPlay = card.canBePlayedOn(topCard);
        }
        
        if (canPlay) {
            // Special check for Wild Draw Four: verify player has no other matching cards
            if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                // Check if player has any valid cards to play other than Wild Draw Four
                if (game.hasValidCardOtherThanWildDrawFour(game.getPlayers().get(0), topCard)) {
                    showMessage("You cannot play Wild Draw Four when you have other valid cards to play!");
                    return;
                }
                
                // Debug logging
                System.out.println("Wild Draw Four is playable - no other valid cards found");
                
                // Wild Draw Four is valid - set flag and continue
                lastPlayedCard = card;
                wildDrawFourPlayed = true;
                
                // Actually play the Wild Draw Four card
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Wild Draw Four card!");
                    return;
                }
                System.out.println("Successfully played Wild Draw Four card");
                
                // Show color selection pane for the Wild Draw Four card
                if (colorSelectPane != null) {
                    colorSelectPane.setVisible(true);
                }
                waitingForColorSelection = true;
                showMessage("Select a color for your Wild Draw Four card");
            } else if (card.getType() == Card.Type.WILD) {
                // Regular wild card is always playable
                lastPlayedCard = card;
                wildDrawFourPlayed = false;
                
                // Play the standard Wild card
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Wild card!");
                    return;
                }
                
                // Show color selection pane
                if (colorSelectPane != null) {
                    colorSelectPane.setVisible(true);
                }
                waitingForColorSelection = true;
                showMessage("Select a color for your wild card");
            } else if (card.getType() == Card.Type.SKIP_ALL) {
                // Skip All card is always playable if it matches color
                lastPlayedCard = card;
                
                // Play the Skip All card
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Skip All card!");
                    return;
                }
                
                showMessage("Skip All card played - your turn again!");
                
                // Since the Skip All card keeps the turn with the current player,
                // we just need to update the UI without starting a CPU turn sequence
                updateGameUI();
                
                // Update the player's hand to show the new state
                updatePlayerHand();
            } else if (card.getType() == Card.Type.COLOR_DRAW) {
                // Color Draw card is always playable if it matches color
                lastPlayedCard = card;
                
                // Play the Color Draw card
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Color Draw card!");
                    return;
                }
                
                // Show color selection pane
                if (colorSelectPane != null) {
                    colorSelectPane.setVisible(true);
                }
                waitingForColorSelection = true;
                showMessage("Select a color for your Color Draw card");
            } else if (card.getType() == Card.Type.SWAP_HANDS) {
                // Swap Hands card is always playable if it matches color
                lastPlayedCard = card;
                
                // Play the Swap Hands card
                boolean played = game.playCard(cardIndex);
                if (!played) {
                    showMessage("Failed to play Swap Hands card!");
                    return;
                }
                
                showMessage("Swap Hands card played - swapping hands with next player!");
                updateGameUI();
                
                // If it's a CPU's turn now, play it
                if (!isHumanTurn()) {
                    startCpuTurnSequence();
                }
            } else {
                // For non-wild cards, play immediately
                boolean played = game.playCard(cardIndex);
                
                if (played) {
                    // Check if the player has won
                    if (game.getPlayers().get(0).getHand().isEmpty()) {
                        handleGameOver(game.getPlayers().get(0));
                    } else {
                        // If the player has only one card left and hasn't called UNO
                        if (game.getPlayers().get(0).getHand().size() == 1 && 
                            !game.getPlayers().get(0).hasCalledUno()) {
                            showMessage("Don't forget to call UNO!");
                        }
                        
                        // Don't need to manually move to the next player anymore
                        // as game.playCard will handle this through handleActionCard
                        updateGameUI();
                        
                        // If it's a CPU's turn now, play it
                        if (!isHumanTurn()) {
                            startCpuTurnSequence();
                        }
                    }
                }
            }
        } else {
            showMessage("You can't play this card!");
        }
    }
    
    @FXML
    public void handleDrawCard(ActionEvent event) {
        if (waitingForColorSelection) {
            showMessage("Please select a color first!");
            return;
        }
        
        if (isHumanTurn()) {
            // Check if there's a Draw Four counter active
            if (game.getDrawFourCounter() > 0) {
                int cardsToDraw = game.getDrawFourCounter();
                showMessage("Drawing " + cardsToDraw + " cards due to Wild Draw Four!");
                
                // Draw the required cards
                for (int i = 0; i < cardsToDraw; i++) {
                    Card drawnCard = game.getDeck().drawCard();
                    if (drawnCard != null) {
                        game.getPlayers().get(0).addCard(drawnCard);
                    }
                }
                
                // Reset the counter and move to next player
                game.resetDrawFourCounter();
                game.moveToNextPlayer();
                updateGameUI();
                return;
            }
            
            // Check if there's a Draw Two counter active
            if (game.getDrawTwoCounter() > 0) {
                int cardsToDraw = game.getDrawTwoCounter();
                showMessage("Drawing " + cardsToDraw + " cards due to Draw Two!");
                
                // Draw the required cards
                for (int i = 0; i < cardsToDraw; i++) {
                    Card drawnCard = game.getDeck().drawCard();
                    if (drawnCard != null) {
                        game.getPlayers().get(0).addCard(drawnCard);
                    }
                }
                
                // Reset the counter and move to next player
                game.resetDrawTwoCounter();
                game.moveToNextPlayer();
                updateGameUI();
                return;
            }
            
            // Regular draw (no stack)
            Card drawnCard = game.drawCard();
            
            if (drawnCard != null) {
                showMessage("You drew: " + drawnCard.toString());
            }
            
            // Update the UI
            updateGameUI();
        } else {
            showMessage("It's not your turn!");
        }
    }
    
    private void selectColor(Card.Color color) {
        handleColorSelected(color);
    }
    
    @FXML
    public void handleColorSelection(ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Button colorButton = (Button) event.getSource();
            
            if (colorButton.getText().equalsIgnoreCase("Red")) {
                selectColor(Card.Color.RED);
            } else if (colorButton.getText().equalsIgnoreCase("Blue")) {
                selectColor(Card.Color.BLUE);
            } else if (colorButton.getText().equalsIgnoreCase("Green")) {
                selectColor(Card.Color.GREEN);
            } else if (colorButton.getText().equalsIgnoreCase("Yellow")) {
                selectColor(Card.Color.YELLOW);
            }
        }
    }
    
    @FXML
    public void callUno() {
        if (isHumanTurn()) {
            Player player = game.getPlayers().get(0);
            
            // New UNO rule: Player can call UNO when they have 2 cards and at least 1 is playable
            if (player.getHand().size() == 2) {
                // Check if at least one card is playable
                boolean hasPlayableCard = false;
                Card topCard = game.getTopCard();
                
                for (Card card : player.getHand()) {
                    boolean isPlayable;
                    if (topCard.getType() == Card.Type.WILD || topCard.getType() == Card.Type.WILD_DRAW_FOUR) {
                        // For wild cards on top, check if we have a card matching the chosen color
                        isPlayable = (card.getColor() == game.getCurrentColor() || card.getColor() == Card.Color.WILD);
                    } else {
                        // Regular card matching
                        isPlayable = card.canBePlayedOn(topCard);
                    }
                    
                    if (isPlayable) {
                        hasPlayableCard = true;
                        break;
                    }
                }
                
                if (hasPlayableCard) {
                    // Mark that player has called UNO
                    player.setHasCalledUno(true);
                    
                    // Show message with player name and make it more visible
                    String playerName = player.getName();
                    String unoMessage = playerName + " has called UNO!";
                    
                    // Show the message in a more prominent way
                    showMessage(unoMessage);
                    
                    // Display a more visible alert for UNO call
                    Platform.runLater(() -> {
                        // Create a larger, colorful label for the UNO announcement
                        Label unoLabel = new Label(unoMessage);
                        unoLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: red;");
                        
                        // Add it to the game board in a prominent position
                        // If gameStateLabel exists, we can modify it temporarily
                        if (gameStateLabel != null) {
                            String originalText = gameStateLabel.getText();
                            String originalStyle = gameStateLabel.getStyle();
                            
                            // Change the style and text
                            gameStateLabel.setText(unoMessage);
                            gameStateLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: red;");
                            
                            // Create a pause to revert after 3 seconds
                            PauseTransition pause = new PauseTransition(Duration.seconds(3));
                            pause.setOnFinished(event -> {
                                gameStateLabel.setText(originalText);
                                gameStateLabel.setStyle(originalStyle);
                            });
                            pause.play();
                        }
                        
                        // Also print to console for debugging
                        System.out.println("UNO CALLED: " + unoMessage);
                    });
                    
                    // Update UI to reflect UNO status
                    updateUnoIndicators();
                    
                } else {
                    showMessage("You can only call UNO when you have a playable card!");
                }
            } else {
                showMessage("You can only call UNO when you have exactly 2 cards left!");
            }
        } else {
            showMessage("You can only call UNO during your turn!");
        }
    }
    
    @FXML
    public void returnToMainMenu() {
        sceneManager.showMainMenuScene();
    }
    
    @FXML
    public void toggleFullscreen(ActionEvent event) {
        Stage stage = (Stage) fullscreenButton.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        
        if (isFullScreen) {
            stage.setFullScreen(false);
            fullscreenButton.setText("Fullscreen");
            
            // Reset to default size when exiting fullscreen
            stage.setWidth(1024);
            stage.setHeight(768);
        } else {
            // Set resizable before going fullscreen
            stage.setResizable(true);
            
            // Change button text
            fullscreenButton.setText("Exit Fullscreen");
            
            // Apply fullscreen
            stage.setFullScreen(true);
            
            // Apply full screen exit hint (empty = no hint)
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("ESCAPE"));
        }
    }
    
    private void recordGameResult(Player winner) {
        try {
            // Create a list for player IDs
            List<Long> playerIds = new ArrayList<>();
            
            // For a CPU win, we'll use a simplified approach with fixed IDs
            // Add fixed player IDs that will work with your backend
            Long humanPlayerId = 1L; // Use a valid ID that exists in your database
            
            // Add all players to the list
            playerIds.add(humanPlayerId); // Human player (ID 1)
            playerIds.add(2L);  // CPU 1 (ID 2)
            playerIds.add(3L);  // CPU 2 (ID 3)
            playerIds.add(4L);  // CPU 3 (ID 4)
            
            // Determine winner ID - make sure it matches one of the IDs in playerIds
            Long winnerId;
            if (winner.isHuman()) {
                winnerId = humanPlayerId; // Human player won
            } else {
                // CPU player won
                int winnerIndex = game.getPlayers().indexOf(winner);
                // Map the index to the corresponding ID in playerIds
                winnerId = Long.valueOf(winnerIndex + 1); // +1 because our IDs start at 1
            }
            
            // Debug info
            System.out.println("Recording game result: Winner ID = " + winnerId + ", Player IDs = " + playerIds);
            
            // Call the API service to record the game result
            apiService.recordGameResult(winnerId, playerIds)
                .subscribe(
                    response -> System.out.println("Game result recorded successfully: " + response),
                    error -> System.err.println("Error recording game result: " + error.getMessage())
                );
        } catch (Exception e) {
            System.err.println("Failed to record game result: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Helper method to set up cheat buttons
    private void setupCheatButtons() {
        // First check if the cheat buttons are even present in the FXML
        if (cheatSkipButton == null || cheatReverseButton == null || 
            cheatDrawTwoButton == null || cheatWildButton == null || 
            cheatWildDrawFourButton == null || cheatSkipAllButton == null || 
            cheatColorDrawButton == null || cheatSwapHandsButton == null) {
            System.out.println("Cheat buttons not found in FXML. Skipping initialization.");
            return;
        }
        
        // Set up the Skip cheat button
        cheatSkipButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Skip cheat");
                addCheatCardToHand(Card.Type.SKIP);
                showMessage("Skip card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Reverse cheat button
        cheatReverseButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Reverse cheat");
                addCheatCardToHand(Card.Type.REVERSE);
                showMessage("Reverse card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Draw Two cheat button
        cheatDrawTwoButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Draw Two cheat");
                addCheatCardToHand(Card.Type.DRAW_TWO);
                showMessage("Draw Two card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Wild cheat button
        cheatWildButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Wild cheat");
                addCheatCardToHand(Card.Type.WILD);
                showMessage("Wild card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Wild Draw Four cheat button
        cheatWildDrawFourButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Wild Draw Four cheat");
                addCheatCardToHand(Card.Type.WILD_DRAW_FOUR);
                showMessage("Wild Draw Four card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Skip All cheat button
        cheatSkipAllButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Skip All cheat");
                addCheatCardToHand(Card.Type.SKIP_ALL);
                showMessage("Skip All card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Color Draw cheat button
        cheatColorDrawButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Color Draw cheat");
                addCheatCardToHand(Card.Type.COLOR_DRAW);
                showMessage("Color Draw card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
        
        // Set up the Swap Hands cheat button
        cheatSwapHandsButton.setOnAction(event -> {
            if (isHumanTurn()) {
                System.out.println("Using Swap Hands cheat");
                addCheatCardToHand(Card.Type.SWAP_HANDS);
                showMessage("Swap Hands card added to your hand");
            } else {
                showMessage("You can only use cheats during your turn!");
            }
        });
    }
    
    // Helper method to add a cheat card to the player's hand
    private void addCheatCardToHand(Card.Type cardType) {
        // Create a new card based on the requested type
        Card cheatCard;
        
        // For wild cards and bonus cards
        if (cardType == Card.Type.WILD || cardType == Card.Type.WILD_DRAW_FOUR ||
            cardType == Card.Type.SKIP_ALL || cardType == Card.Type.COLOR_DRAW ||
            cardType == Card.Type.SWAP_HANDS) {
            cheatCard = new Card(Card.Color.WILD, cardType);
        } 
        // For action cards (Skip, Reverse, Draw Two)
        else if (cardType == Card.Type.SKIP || cardType == Card.Type.REVERSE || cardType == Card.Type.DRAW_TWO) {
            // Use current top card color for better playability
            Card.Color currentColor = game.getCurrentColor();
            cheatCard = new Card(currentColor, cardType);
        }
        // Should never reach here with our current implementation
        else {
            System.out.println("Invalid cheat card type: " + cardType);
            return;
        }
        
        // Add the card to the human player's hand
        game.getPlayers().get(0).addCard(cheatCard);
        
        // Update the UI to show the new card
        updatePlayerHand();
    }
    
    // Method to update turn indicators
    private void updateTurnIndicators() {
        // Define the styles for turn indicators
        final String playerStyle = "-fx-font-weight: normal; -fx-text-fill: white;";
        final String activeStyle = "-fx-font-weight: bold; -fx-text-fill: yellow;";
        final Color BRIGHT_GREEN = Color.web("#00FF00");  // Bright green
        
        // Store the current player index for clarity
        final int currentPlayerIndex = game.getCurrentPlayerIndex();
        
        // Run UI updates on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Verify the circles are initialized
            System.out.println("Turn indicators: playerTurn=" + (playerTurn != null) + ", cpu1Turn=" + (cpu1Turn != null) + ", cpu2Turn=" + (cpu2Turn != null) + ", cpu3Turn=" + (cpu3Turn != null));
    
            // Reset all turn indicators first
            if (playerTurn != null) {
                System.out.println("Setting playerTurn to DARKGREY");
                playerTurn.setFill(Color.DARKGREY);
                playerTurn.setStrokeWidth(0);
            }
            if (cpu1Turn != null) {
                System.out.println("Setting cpu1Turn to DARKGREY");
                cpu1Turn.setFill(Color.DARKGREY);
                cpu1Turn.setStrokeWidth(0);
            }
            if (cpu2Turn != null) {
                System.out.println("Setting cpu2Turn to DARKGREY");
                cpu2Turn.setFill(Color.DARKGREY);
                cpu2Turn.setStrokeWidth(0);
            }
            if (cpu3Turn != null) {
                System.out.println("Setting cpu3Turn to DARKGREY");
                cpu3Turn.setFill(Color.DARKGREY);
                cpu3Turn.setStrokeWidth(0);
            }
            
            // Now set the current player's indicator
            System.out.println("Current player index: " + currentPlayerIndex);
            
            switch (currentPlayerIndex) {
                case 0: // Human player
                    if (playerTurn != null) {
                        System.out.println("Setting playerTurn to GREEN");
                        playerTurn.setFill(BRIGHT_GREEN);
                        playerTurn.setStrokeWidth(2);
                    }
                    break;
                case 1: // CPU 1
                    if (cpu1Turn != null) {
                        System.out.println("Setting cpu1Turn to GREEN");
                        cpu1Turn.setFill(BRIGHT_GREEN);
                        cpu1Turn.setStrokeWidth(2);
                    }
                    break;
                case 2: // CPU 2
                    if (cpu2Turn != null) {
                        System.out.println("Setting cpu2Turn to GREEN");
                        cpu2Turn.setFill(BRIGHT_GREEN);
                        cpu2Turn.setStrokeWidth(2);
                    }
                    break;
                case 3: // CPU 3
                    if (cpu3Turn != null) {
                        System.out.println("Setting cpu3Turn to GREEN");
                        cpu3Turn.setFill(BRIGHT_GREEN);
                        cpu3Turn.setStrokeWidth(2);
                    }
                    break;
            }
            
            // Update player labels
            System.out.println("Labels defined: playerLabel=" + (playerLabel != null) + ", cpu1Label=" + (cpu1Label != null) + ", cpu2Label=" + (cpu2Label != null) + ", cpu3Label=" + (cpu3Label != null));
            
            // Reset all labels to white
            if (playerLabel != null) {
                playerLabel.setTextFill(Color.WHITE);
            }
            if (cpu1Label != null) {
                cpu1Label.setTextFill(Color.WHITE);
            }
            if (cpu2Label != null) {
                cpu2Label.setTextFill(Color.WHITE);
            }
            if (cpu3Label != null) {
                cpu3Label.setTextFill(Color.WHITE);
            }
            
            // Set the current player's label color
            switch (currentPlayerIndex) {
                case 0: // Human player
                    if (playerLabel != null) {
                        System.out.println("Setting playerLabel to YELLOW");
                        playerLabel.setTextFill(Color.YELLOW);
                    }
                    break;
                case 1: // CPU 1
                    if (cpu1Label != null) {
                        System.out.println("Setting cpu1Label to YELLOW");
                        cpu1Label.setTextFill(Color.YELLOW);
                    }
                    break;
                case 2: // CPU 2
                    if (cpu2Label != null) {
                        System.out.println("Setting cpu2Label to YELLOW");
                        cpu2Label.setTextFill(Color.YELLOW);
                    }
                    break;
                case 3: // CPU 3
                    if (cpu3Label != null) {
                        System.out.println("Setting cpu3Label to YELLOW");
                        cpu3Label.setTextFill(Color.YELLOW);
                    }
                    break;
            }
        });
    }
    
    // Method to start the CPU turn sequence
    private void startCpuTurnSequence() {
        // Set the flag to prevent multiple CPU sequences running at once
        isCpuTurnInProgress = true;
        
        // Start the CPU turn
        playCPUTurn();
    }
    
    // Modified playCPUTurn method to work with the flag
    private void playCPUTurn() {
        System.out.println("CPU turn started. Current player index: " + game.getCurrentPlayerIndex());
        
        // Add a safety check to prevent infinite CPU turns
        if (game.getCurrentPlayerIndex() == 0 || game.isGameOver()) {
            System.out.println("Aborting CPU turn - it's either player's turn or game is over");
            isCpuTurnInProgress = false; // Reset flag when we're done
            return;
        }
        
        // Log the current player index
        System.out.println("CPU player index: " + game.getCurrentPlayerIndex());
        
        // Store which CPU player we're processing now to avoid confusion if the index changes
        final int currentCpuIndex = game.getCurrentPlayerIndex();
        final String cpuName = game.getPlayers().get(currentCpuIndex).getName();
        
        // Increase the pause duration to 2.5 seconds to make CPU turns easier to follow
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> {
            // Safety check again - make sure nothing has changed
            if (game.getCurrentPlayerIndex() != currentCpuIndex) {
                System.out.println("Current player changed during delay! Was " + currentCpuIndex + 
                                  " now " + game.getCurrentPlayerIndex() + ". Skipping this CPU turn.");
                isCpuTurnInProgress = false; // Reset flag
                return;
            }
            
            if (game.isGameOver()) {
                System.out.println("Game over detected during CPU pause. Aborting CPU turn.");
                isCpuTurnInProgress = false; // Reset flag
                return;
            }
            
            System.out.println("CPU playing turn...");
            System.out.println("CPU " + cpuName + " is playing turn");
            
            // CPU takes its turn
            boolean cpuPlayed = game.playCpuTurn();
            System.out.println("CPU turn finished. Next player: " + game.getCurrentPlayerIndex());
            System.out.println("CPU " + cpuName + " played: " + cpuPlayed + ", Current player after CPU: " + game.getCurrentPlayerIndex());
            
            // Update all UI elements to reflect changes
            updateGameUI();
            
            // Debug the game state
            debugGameState();
            
            // If it's still a CPU turn (but different CPU), schedule the next CPU turn
            if (!isHumanTurn() && !game.isGameOver()) {
                // Make sure we're not stuck on the same CPU
                if (game.getCurrentPlayerIndex() == currentCpuIndex) {
                    System.out.println("WARNING: CPU turn didn't advance to the next player! Forcing advance...");
                    game.moveToNextPlayer();
                    updateGameUI();
                }
                
                // Schedule the next CPU's turn
                PauseTransition nextCpuTurn = new PauseTransition(Duration.seconds(2.5));
                nextCpuTurn.setOnFinished(event -> playCPUTurn());
                nextCpuTurn.play();
            } else {
                // We're done with CPU turns, reset the flag
                isCpuTurnInProgress = false;
            }
        });
        
        pause.play();
    }
}
