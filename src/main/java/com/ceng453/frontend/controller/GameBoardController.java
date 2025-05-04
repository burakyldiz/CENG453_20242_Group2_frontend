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
import java.util.List;

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
    @FXML private HBox colorSelectionPane;
    
    @FXML private Circle playerTurn;
    @FXML private Circle cpu1Turn;
    @FXML private Circle cpu2Turn;
    @FXML private Circle cpu3Turn;
    
    @FXML private Label playerLabel;
    @FXML private Label cpu1Label;
    @FXML private Label cpu2Label;
    @FXML private Label cpu3Label;
    
    @FXML private Button drawCardButton;
    @FXML private Button unoButton;
    @FXML private Button fullscreenButton;
    
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
                
                // Increment the draw four counter
                game.incrementDrawFourCounter(4);
                System.out.println("Draw Four counter set to: " + game.getDrawFourCounter());
            } else {
                // For regular Wild cards, just set the color
                game.setCurrentColor(color);
                System.out.println("Wild color set to: " + color);
            }
            
            // Update UI to show the selected color
            if (currentColorLabel != null) {
                currentColorLabel.setText("Current Color: " + color.toString());
                String colorStyle = getColorStyle(color);
                currentColorLabel.setStyle("-fx-font-weight: bold; " + colorStyle);
            }
            
            // Hide the color selection pane
            if (colorSelectionPane != null) {
                colorSelectionPane.setVisible(false);
            }
            waitingForColorSelection = false;
            wildDrawFourPlayed = false;
            lastPlayedCard = null;
            
            // Move to the next player's turn after color selection
            game.moveToNextPlayer();
            updateGameUI();
            
            // Debug the game state after color selection
            debugGameState();
            
            // Check if it's now a CPU's turn
            if (!isHumanTurn() && !game.isGameOver()) {
                playCPUTurn();
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
            
            // Hide color selection pane initially
            if (colorSelectionPane != null) {
                colorSelectionPane.setVisible(false);
            }
            waitingForColorSelection = false;
            wildDrawFourPlayed = false;
            
            // Initial update of all UI elements
            updateGameUI();
            
            // Automatically play for CPU if they start first
            if (!isHumanTurn()) {
                playCPUTurn();
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
            
            updateDiscardPile();
            updatePlayerHand();
            updateCPUHandPanes();
            updateTurnIndicators();
            updateUnoIndicators();
            updateColorDisplay();
            
            // Debug game state
            debugGameState();
            
            // Clear any previous messages
            if (gameStateLabel != null) {
                gameStateLabel.setText("");
            }
            
            // Check for penalties that must be handled automatically
            if (isHumanTurn() && !waitingForColorSelection) {
                checkForPendingPenalties();
            }
            
            // Check if it's CPU's turn and play automatically
            if (!isHumanTurn() && !game.isGameOver() && !waitingForColorSelection) {
                playCPUTurn();
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
                    boolean isPlayable = true;
                    
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
                            // Regular playability check
                            isPlayable = card.canBePlayedOn(topCard);
                            
                            // Special check for Wild Draw Four - only playable if there are no matching cards
                            if (isPlayable && card.getType() == Card.Type.WILD_DRAW_FOUR) {
                                // Check if player has any cards matching the current color
                                boolean hasMatchingCards = false;
                                for (Card handCard : game.getPlayers().get(0).getHand()) {
                                    // Skip the Wild Draw Four card itself
                                    if (handCard == card) continue;
                                    
                                    // Check if any card matches color or is a regular wild
                                    if (handCard.getColor() == game.getCurrentColor() || 
                                        (handCard.getColor() == Card.Color.WILD && handCard.getType() != Card.Type.WILD_DRAW_FOUR)) {
                                        hasMatchingCards = true;
                                        break;
                                    }
                                }
                                
                                // Only mark as playable if player has no matching cards
                                if (hasMatchingCards) {
                                    isPlayable = false;
                                }
                            }
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
                    } else {
                        // Not player's turn - all cards appear normal
                        cardView.setOpacity(1.0);
                        cardView.setEffect(null);
                    }
                    
                    // Add click handler for player's cards
                    int cardIndex = hand.indexOf(card);
                    cardView.setOnMouseClicked(event -> handleCardClick(card, cardIndex));
                    
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
                        String imagePath = "/images/cards/" + card.getImageFileName();
                        InputStream stream = getClass().getResourceAsStream(imagePath);
                        
                        if (stream != null) {
                            //System.out.println("Loading card image: " + imagePath);
                            ImageView cardView = new ImageView(new Image(stream));
                            cardView.setFitWidth(60);
                            cardView.setFitHeight(90);
                            pane.getChildren().add(cardView);
                        } else {
                            // Fallback if image can't be loaded
                            Rectangle rect = new Rectangle(60, 90, Color.DARKGRAY);
                            rect.setStroke(Color.BLACK);
                            rect.setStrokeWidth(2);
                            Text cardText = new Text(card.toString());
                            StackPane stack = new StackPane();
                            stack.getChildren().addAll(rect, cardText);
                            pane.getChildren().add(stack);
                        }
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
    
    private ImageView createCardImageView(Card card) {
        try {
            String imageName = card.getImageFileName();
            String imagePath = CARD_IMAGES_PATH + imageName;
            
            //System.out.println("Loading card image: " + imagePath);
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            
            if (imageStream != null) {
                return new ImageView(new Image(imageStream));
            } else {
                System.err.println("Card image not found: " + imagePath);
                return createCardPlaceholder(card);
            }
        } catch (Exception e) {
            System.err.println("Error creating card image view: " + e.getMessage());
            e.printStackTrace();
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
            if (player.getHand().size() == 1) {
                // Player has UNO!
                switch (i) {
                    case 0:
                        if (playerTurn != null) playerTurn.setFill(Color.RED);
                        break;
                    case 1:
                        if (cpu1Turn != null) cpu1Turn.setFill(Color.RED);
                        break;
                    case 2:
                        if (cpu2Turn != null) cpu2Turn.setFill(Color.RED);
                        break;
                    case 3:
                        if (cpu3Turn != null) cpu3Turn.setFill(Color.RED);
                        break;
                }
            }
        }
    }
    
    private void handleCardClick(Card card, int cardIndex) {
        if (waitingForColorSelection) {
            showMessage("Please select a color first!");
            return;
        }
        
        if (isHumanTurn()) {
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
                    if (colorSelectionPane != null) {
                        colorSelectionPane.setVisible(true);
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
                    } else {
                        showMessage("Failed to play Draw Two!");
                    }
                } else {
                    showMessage("You must play a Draw Two or draw " + game.getDrawTwoCounter() + " cards!");
                }
                return;
            }
            
            // Check if the card can be played
            if (card.canBePlayedOn(topCard)) {
                // Special check for Wild Draw Four: verify player has no other matching cards
                if (card.getType() == Card.Type.WILD_DRAW_FOUR) {
                    // Check if player has any cards matching the current color
                    boolean hasMatchingCards = false;
                    for (Card handCard : game.getPlayers().get(0).getHand()) {
                        // Skip the Wild Draw Four card itself
                        if (handCard == card) continue;
                        
                        // Check if card matches current color or is a wild (not draw four)
                        if (handCard.getColor() == game.getCurrentColor() || 
                            (handCard.getColor() == Card.Color.WILD && handCard.getType() != Card.Type.WILD_DRAW_FOUR)) {
                            hasMatchingCards = true;
                            break;
                        }
                    }
                    
                    // If player has matching cards, they can't play Wild Draw Four
                    if (hasMatchingCards) {
                        showMessage("You cannot play Wild Draw Four when you have cards matching the current color!");
                        return;
                    }
                    
                    // Wild Draw Four is valid - set flag and continue
                    lastPlayedCard = card;
                    wildDrawFourPlayed = true;
                } else if (card.getType() == Card.Type.WILD) {
                    // Regular wild card is always playable
                    lastPlayedCard = card;
                    wildDrawFourPlayed = false;
                    
                    // Play the card if it's a standard Wild card
                    if (card.getType() == Card.Type.WILD) {
                        boolean played = game.playCard(cardIndex);
                        if (!played) {
                            showMessage("Failed to play Wild card!");
                            return;
                        }
                    }
                    
                    // Show color selection pane
                    if (colorSelectionPane != null) {
                        colorSelectionPane.setVisible(true);
                    }
                    waitingForColorSelection = true;
                    showMessage("Select a color for your wild card");
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
                            
                            // Update UI and start CPU turns
                            updateGameUI();
                        }
                    }
                }
            } else {
                showMessage("You can't play this card!");
            }
        } else {
            showMessage("It's not your turn!");
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
                showMessage("You must draw " + cardsToDraw + " cards!");
                
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
                showMessage("You must draw " + cardsToDraw + " cards!");
                
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
                    showMessage("You called UNO!");
                    // Update UI
                    updateGameUI();
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
    
    private void playCPUTurn() {
        System.out.println("CPU turn started. Current player index: " + game.getCurrentPlayerIndex());
        
        // Add a safety check to prevent infinite CPU turns
        if (game.getCurrentPlayerIndex() == 0 || game.isGameOver()) {
            System.out.println("Aborting CPU turn - it's either player's turn or game is over");
            return;
        }
        
        // Log the current player index
        System.out.println("CPU player index: " + game.getCurrentPlayerIndex());
        
        // Increase the pause duration to 2.5 seconds to make CPU turns easier to follow
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> {
            if (!isHumanTurn() && !game.isGameOver()) {
                System.out.println("CPU playing turn...");
                System.out.println("CPU " + game.getPlayers().get(game.getCurrentPlayerIndex()).getName() + " is playing turn");
                
                // CPU takes its turn
                boolean cpuPlayed = game.playCpuTurn();
                System.out.println("CPU turn finished. Next player: " + game.getCurrentPlayerIndex());
                System.out.println("CPU played: " + cpuPlayed + ", Current player after CPU: " + game.getCurrentPlayerIndex());
                
                // Update all UI elements to reflect changes
                updateGameUI();
                
                // Debug the game state
                debugGameState();
                
                // If it's still a CPU turn (but different CPU), schedule the next CPU turn
                if (!isHumanTurn() && !game.isGameOver()) {
                    
                    // Normal case: Continue with the next CPU player turn
                    PauseTransition nextCpuTurn = new PauseTransition(Duration.seconds(2.5));
                    nextCpuTurn.setOnFinished(event -> playCPUTurn());
                    nextCpuTurn.play();
                }
            }
        });
        
        pause.play();
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
    // Helper method to update turn indicators
    private void updateTurnIndicators() {
        // Define the styles once outside the lambdas
        final String playerStyle = "-fx-font-weight: normal; -fx-text-fill: white;";
        final String activeStyle = "-fx-font-weight: bold; -fx-text-fill: yellow;";
        final Color BRIGHT_GREEN = Color.web("#00FF00");  // Bright green
        
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
                cpu1Turn.setFill(Color.DARKGREY);
                cpu1Turn.setStrokeWidth(0);
            }
            if (cpu2Turn != null) {
                cpu2Turn.setFill(Color.DARKGREY);
                cpu2Turn.setStrokeWidth(0);
            }
            if (cpu3Turn != null) {
                cpu3Turn.setFill(Color.DARKGREY);
                cpu3Turn.setStrokeWidth(0);
            }
            
            // Set the turn indicator for current player to green
            int currentPlayerIndex = game.getCurrentPlayerIndex();
            System.out.println("Current player index: " + currentPlayerIndex);
            
            switch (currentPlayerIndex) {
                case 0:
                    if (playerTurn != null) {
                        System.out.println("Setting playerTurn to GREEN");
                        playerTurn.setFill(BRIGHT_GREEN);
                        playerTurn.setStroke(Color.WHITE);
                        playerTurn.setStrokeWidth(2);
                    }
                    break;
                case 1:
                    if (cpu1Turn != null) {
                        System.out.println("Setting cpu1Turn to GREEN");
                        cpu1Turn.setFill(BRIGHT_GREEN);
                        cpu1Turn.setStroke(Color.WHITE);
                        cpu1Turn.setStrokeWidth(2);
                    }
                    break;
                case 2:
                    if (cpu2Turn != null) {
                        System.out.println("Setting cpu2Turn to GREEN");
                        cpu2Turn.setFill(BRIGHT_GREEN);
                        cpu2Turn.setStroke(Color.WHITE);
                        cpu2Turn.setStrokeWidth(2);
                    }
                    break;
                case 3:
                    if (cpu3Turn != null) {
                        System.out.println("Setting cpu3Turn to GREEN");
                        cpu3Turn.setFill(BRIGHT_GREEN);
                        cpu3Turn.setStroke(Color.WHITE);
                        cpu3Turn.setStrokeWidth(2);
                    }
                    break;
            }
            
            // Debug to check if labels are defined
            System.out.println("Labels defined: playerLabel=" + (playerLabel != null) + ", cpu1Label=" + (cpu1Label != null) + ", cpu2Label=" + (cpu2Label != null) + ", cpu3Label=" + (cpu3Label != null)); 
            
            // Reset all styles first
            if (playerLabel != null) playerLabel.setStyle(playerStyle);
            if (cpu1Label != null) cpu1Label.setStyle(playerStyle);
            if (cpu2Label != null) cpu2Label.setStyle(playerStyle);
            if (cpu3Label != null) cpu3Label.setStyle(playerStyle);
            
            // Highlight the active player name with yellow text
            switch (game.getCurrentPlayerIndex()) {
                case 0:
                    if (playerLabel != null) {
                        System.out.println("Setting playerLabel to YELLOW");
                        playerLabel.setStyle(activeStyle);
                    }
                    break;
                case 1:
                    if (cpu1Label != null) {
                        System.out.println("Setting cpu1Label to YELLOW");
                        cpu1Label.setStyle(activeStyle);
                    }
                    break;
                case 2:
                    if (cpu2Label != null) {
                        System.out.println("Setting cpu2Label to YELLOW");
                        cpu2Label.setStyle(activeStyle);
                    }
                    break;
                case 3:
                    if (cpu3Label != null) {
                        System.out.println("Setting cpu3Label to YELLOW");
                        cpu3Label.setStyle(activeStyle);
                    }
                    break;
            }
            
            // Update current player label
            if (currentPlayerLabel != null) {
                Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
                currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
            }
            
            // Update direction indicator
            if (directionLabel != null) {
                directionLabel.setText("Direction: " + (game.isClockwise() ? "Clockwise" : "Counter-Clockwise"));
            }
        });
    }
}
