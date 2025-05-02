package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.Card;
import com.ceng453.frontend.model.Game;
import com.ceng453.frontend.model.Player;
import com.ceng453.frontend.ui.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.util.Duration;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class GameBoardController {
    // Image paths
    private static final String CARD_IMAGES_PATH = "/images/cards/";
    private static final String CARD_BACK_IMAGE = "/images/card_back.png";
    
    // Spring-injected dependencies
    private final SceneManager sceneManager;
    
    // Game state
    private Game game;
    private Card lastPlayedCard;
    private boolean waitingForColorSelection;
    
    // FXML elements
    @FXML private FlowPane playerHandPane;
    @FXML private FlowPane cpu1HandPane;
    @FXML private FlowPane cpu2HandPane;
    @FXML private FlowPane cpu3HandPane;
    @FXML private StackPane discardPilePane;
    @FXML private Button drawCardButton;
    @FXML private Button unoButton;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private Label directionLabel;
    @FXML private VBox colorSelectPane;
    @FXML private Button redButton;
    @FXML private Button blueButton;
    @FXML private Button greenButton;
    @FXML private Button yellowButton;
    @FXML private Circle unoIndicatorPlayer;
    @FXML private Circle unoIndicatorCPU1;
    @FXML private Circle unoIndicatorCPU2;
    @FXML private Circle unoIndicatorCPU3;
    @FXML private Label playerLabel;
    @FXML private Label cpu1Label;
    @FXML private Label cpu2Label;
    @FXML private Label cpu3Label;
    
    public GameBoardController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }
    
    @FXML
    public void initialize() {
        try {
            System.out.println("GameBoardController initializing...");
            
            // Initialize color selection buttons
            if (redButton != null) {
                redButton.setOnAction(e -> selectColor(Card.Color.RED));
            }
            if (blueButton != null) {
                blueButton.setOnAction(e -> selectColor(Card.Color.BLUE));
            }
            if (greenButton != null) {
                greenButton.setOnAction(e -> selectColor(Card.Color.GREEN));
            }
            if (yellowButton != null) {
                yellowButton.setOnAction(e -> selectColor(Card.Color.YELLOW));
            }
            
            // Set up other event handlers
            if (unoButton != null) {
                unoButton.setOnAction(e -> callUno());
            }
            
            // Hide color selection pane initially
            if (colorSelectPane != null) {
                colorSelectPane.setVisible(false);
            }
            waitingForColorSelection = false;
            
            // Initialize the game
            initializeGame();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in GameBoardController.initialize(): " + e.getMessage());
            showErrorAlert("Game Initialization Error", "Failed to initialize game: " + e.getMessage());
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
        // Update player's hand
        updatePlayerHand();
        
        // Update CPU hands
        updateCPUHandPane(cpu1HandPane, game.getPlayers().get(1));
        updateCPUHandPane(cpu2HandPane, game.getPlayers().get(2));
        updateCPUHandPane(cpu3HandPane, game.getPlayers().get(3));
        
        // Update discard pile
        updateDiscardPile();
        
        // Update current player label with highlight
        if (currentPlayerLabel != null) {
            Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
            currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
            
            // Reset all turn indicators to dark gray
            if (unoIndicatorPlayer != null) unoIndicatorPlayer.setFill(Color.DARKGREY);
            if (unoIndicatorCPU1 != null) unoIndicatorCPU1.setFill(Color.DARKGREY);
            if (unoIndicatorCPU2 != null) unoIndicatorCPU2.setFill(Color.DARKGREY);
            if (unoIndicatorCPU3 != null) unoIndicatorCPU3.setFill(Color.DARKGREY);
            
            // Set the turn indicator for current player to green
            switch (game.getCurrentPlayerIndex()) {
                case 0:
                    if (unoIndicatorPlayer != null) unoIndicatorPlayer.setFill(Color.GREEN);
                    break;
                case 1:
                    if (unoIndicatorCPU1 != null) unoIndicatorCPU1.setFill(Color.GREEN);
                    break;
                case 2:
                    if (unoIndicatorCPU2 != null) unoIndicatorCPU2.setFill(Color.GREEN);
                    break;
                case 3:
                    if (unoIndicatorCPU3 != null) unoIndicatorCPU3.setFill(Color.GREEN);
                    break;
            }
            
            // Add visual indication of whose turn it is with label styling
            String playerStyle = "-fx-font-weight: normal; -fx-text-fill: white;";
            String activeStyle = "-fx-font-weight: bold; -fx-text-fill: yellow;";
            
            // Reset all styles first
            if (playerLabel != null) playerLabel.setStyle(playerStyle);
            if (cpu1Label != null) cpu1Label.setStyle(playerStyle);
            if (cpu2Label != null) cpu2Label.setStyle(playerStyle);
            if (cpu3Label != null) cpu3Label.setStyle(playerStyle);
            
            // Highlight the active player
            switch (game.getCurrentPlayerIndex()) {
                case 0:
                    if (playerLabel != null) playerLabel.setStyle(activeStyle);
                    break;
                case 1:
                    if (cpu1Label != null) cpu1Label.setStyle(activeStyle); 
                    break;
                case 2:
                    if (cpu2Label != null) cpu2Label.setStyle(activeStyle);
                    break;
                case 3:
                    if (cpu3Label != null) cpu3Label.setStyle(activeStyle);
                    break;
            }
        }
        
        // Update direction indicator
        if (directionLabel != null) {
            directionLabel.setText("Direction: " + (game.isClockwise() ? "Clockwise" : "Counter-Clockwise"));
        }
        
        // Update UNO indicators
        updateUnoIndicators();
        
        // Clear any previous messages
        if (messageLabel != null) {
            messageLabel.setText("");
        }
        
        // Check if it's CPU's turn and play automatically
        if (!isHumanTurn()) {
            playCPUTurn();
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
                            System.out.println("Loading card image: " + imagePath);
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
            
            System.out.println("Loading card image: " + imagePath);
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
        try {
            // Update UNO indicators based on player status
            if (unoIndicatorPlayer != null) {
                unoIndicatorPlayer.setFill(game.getPlayers().get(0).hasCalledUno() ? Color.RED : Color.DARKGREY);
            }
            if (unoIndicatorCPU1 != null) {
                unoIndicatorCPU1.setFill(game.getPlayers().get(1).hasCalledUno() ? Color.RED : Color.DARKGREY);
            }
            if (unoIndicatorCPU2 != null) {
                unoIndicatorCPU2.setFill(game.getPlayers().get(2).hasCalledUno() ? Color.RED : Color.DARKGREY);
            }
            if (unoIndicatorCPU3 != null) {
                unoIndicatorCPU3.setFill(game.getPlayers().get(3).hasCalledUno() ? Color.RED : Color.DARKGREY);
            }
        } catch (Exception e) {
            System.err.println("Error updating UNO indicators: " + e.getMessage());
        }
    }
    
    private void handleCardClick(Card card, int cardIndex) {
        if (waitingForColorSelection) {
            showMessage("Please select a color first!");
            return;
        }
        
        if (isHumanTurn()) {
            Card topCard = game.getTopCard();
            
            // Check if the card can be played
            if (card.canBePlayedOn(topCard)) {
                if (card.getType() == Card.Type.WILD || card.getType() == Card.Type.WILD_DRAW_FOUR) {
                    // For wild cards, we need to select a color
                    lastPlayedCard = card;
                    if (colorSelectPane != null) {
                        colorSelectPane.setVisible(true);
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
            // Draw a card from the deck
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
        if (waitingForColorSelection && lastPlayedCard != null) {
            // Get the index of the wild card in the player's hand
            int cardIndex = game.getPlayers().get(0).getHand().indexOf(lastPlayedCard);
            
            if (cardIndex >= 0) {
                // Set the chosen color
                game.chooseWildColor(color);
                
                // Play the wild card
                boolean played = game.playCard(cardIndex);
                
                if (played) {
                    // Check if the player has won
                    if (game.getPlayers().get(0).getHand().isEmpty()) {
                        handleGameOver(game.getPlayers().get(0));
                    } else {
                        // Update UI and continue the game
                        updateGameUI();
                    }
                }
            }
            
            // Hide the color selection pane
            if (colorSelectPane != null) {
                colorSelectPane.setVisible(false);
            }
            waitingForColorSelection = false;
            lastPlayedCard = null;
        }
    }
    
    @FXML
    public void handleColorSelection(ActionEvent event) {
        if (!waitingForColorSelection) return;
        
        Card.Color selectedColor;
        if (event.getSource() == redButton) {
            selectedColor = Card.Color.RED;
        } else if (event.getSource() == yellowButton) {
            selectedColor = Card.Color.YELLOW;
        } else if (event.getSource() == greenButton) {
            selectedColor = Card.Color.GREEN;
        } else if (event.getSource() == blueButton) {
            selectedColor = Card.Color.BLUE;
        } else {
            return;
        }
        
        selectColor(selectedColor);
    }
    
    @FXML
    public void callUno() {
        Player humanPlayer = game.getPlayers().get(0);
        
        if (humanPlayer.getHand().size() == 1) {
            humanPlayer.callUno();
            updateUnoIndicators();
            showMessage("You called UNO!");
        } else {
            showMessage("You can only call UNO when you have one card left!");
        }
    }
    
    @FXML
    public void handleUno() {
        callUno();
    }
    
    @FXML
    public void handleBackToMenu() {
        sceneManager.showMainMenuScene();
    }
    
    private void playCPUTurn() {
        System.out.println("CPU turn started. Current player index: " + game.getCurrentPlayerIndex());
        
        // Use a single delay for this CPU's turn, not recursive
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            try {
                if (!isHumanTurn() && !game.isGameOver()) {
                    // Let the CPU play its turn
                    System.out.println("CPU playing turn...");
                    boolean cpuPlayed = game.playCpuTurn();
                    System.out.println("CPU played: " + cpuPlayed + ", Current player after CPU: " + game.getCurrentPlayerIndex());
                    
                    // Check if a CPU has won
                    for (Player player : game.getPlayers()) {
                        if (player.getHand().isEmpty()) {
                            handleGameOver(player);
                            return;
                        }
                    }
                    
                    // Force update the discard pile to show the card that was just played
                    updateDiscardPile();
                    
                    // Update all other UI elements
                    updateCPUHandPane(cpu1HandPane, game.getPlayers().get(1));
                    updateCPUHandPane(cpu2HandPane, game.getPlayers().get(2));
                    updateCPUHandPane(cpu3HandPane, game.getPlayers().get(3));
                    updatePlayerTurnIndicators();
                    
                    // If it's still a CPU turn (but different CPU), schedule the next CPU turn
                    if (!isHumanTurn() && !game.isGameOver()) {
                        // Schedule another CPU turn after a slight delay
                        PauseTransition nextCpuTurn = new PauseTransition(Duration.seconds(1));
                        nextCpuTurn.setOnFinished(event -> playCPUTurn());
                        nextCpuTurn.play();
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error during CPU turn: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        pause.play();
    }
    
    // Helper method to update turn indicators
    private void updatePlayerTurnIndicators() {
        // Reset all turn indicators to dark gray
        if (unoIndicatorPlayer != null) unoIndicatorPlayer.setFill(Color.DARKGREY);
        if (unoIndicatorCPU1 != null) unoIndicatorCPU1.setFill(Color.DARKGREY);
        if (unoIndicatorCPU2 != null) unoIndicatorCPU2.setFill(Color.DARKGREY);
        if (unoIndicatorCPU3 != null) unoIndicatorCPU3.setFill(Color.DARKGREY);
        
        // Set the turn indicator for current player to green
        switch (game.getCurrentPlayerIndex()) {
            case 0:
                if (unoIndicatorPlayer != null) unoIndicatorPlayer.setFill(Color.GREEN);
                break;
            case 1:
                if (unoIndicatorCPU1 != null) unoIndicatorCPU1.setFill(Color.GREEN);
                break;
            case 2:
                if (unoIndicatorCPU2 != null) unoIndicatorCPU2.setFill(Color.GREEN);
                break;
            case 3:
                if (unoIndicatorCPU3 != null) unoIndicatorCPU3.setFill(Color.GREEN);
                break;
        }
        
        // Add visual indication of whose turn it is with label styling
        String playerStyle = "-fx-font-weight: normal; -fx-text-fill: white;";
        String activeStyle = "-fx-font-weight: bold; -fx-text-fill: yellow;";
        
        // Reset all styles first
        if (playerLabel != null) playerLabel.setStyle(playerStyle);
        if (cpu1Label != null) cpu1Label.setStyle(playerStyle);
        if (cpu2Label != null) cpu2Label.setStyle(playerStyle);
        if (cpu3Label != null) cpu3Label.setStyle(playerStyle);
        
        // Highlight the active player
        switch (game.getCurrentPlayerIndex()) {
            case 0:
                if (playerLabel != null) playerLabel.setStyle(activeStyle);
                break;
            case 1:
                if (cpu1Label != null) cpu1Label.setStyle(activeStyle); 
                break;
            case 2:
                if (cpu2Label != null) cpu2Label.setStyle(activeStyle);
                break;
            case 3:
                if (cpu3Label != null) cpu3Label.setStyle(activeStyle);
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
            
            // TODO: Record game result in the database
            // This would need the ApiService to be injected
            
            // Return to main menu
            sceneManager.showMainMenuScene();
        });
    }
    
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
    
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("An error occurred");
            alert.setContentText(message);
            alert.showAndWait();
        });
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
}
