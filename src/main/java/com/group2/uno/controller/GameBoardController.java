package com.group2.uno.controller;

import com.group2.uno.UnoApplication;
import com.group2.uno.model.Card;
import com.group2.uno.model.Game;
import com.group2.uno.model.Player;
import com.group2.uno.model.enums.CardColor;
import com.group2.uno.model.enums.CardType;
import com.group2.uno.service.GameService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the game board screen
 */
public class GameBoardController implements Initializable {
    @FXML private Label cpu1Label;
    @FXML private Label cpu2Label;
    @FXML private Label cpu3Label;
    @FXML private HBox cpu1Hand;
    @FXML private VBox cpu2Hand;
    @FXML private VBox cpu3Hand;
    @FXML private Circle cpu1UnoIndicator;
    @FXML private Circle cpu2UnoIndicator;
    @FXML private Circle cpu3UnoIndicator;
    @FXML private Label cpu1UnoLabel;
    @FXML private Label cpu2UnoLabel;
    @FXML private Label cpu3UnoLabel;
    @FXML private Label directionIndicator;
    @FXML private Label currentTurnLabel;
    @FXML private Rectangle currentColorIndicator;
    @FXML private HBox cheatButtonsBox;
    @FXML private VBox colorSelectionBox;
    @FXML private Label gameMessageLabel;
    @FXML private VBox gameOverBox;
    @FXML private Label winnerLabel;
    @FXML private HBox playerHand;
    @FXML private Circle playerUnoIndicator;

    private Game game;
    private final GameService gameService;
    private final List<Player> players = new ArrayList<>();
    private Player humanPlayer;
    private Card selectedCard;
    private boolean cpuTurnInProgress = false;

    /**
     * Constructor
     */
    public GameBoardController() {
        this.gameService = GameService.getInstance();
    }

    /**
     * Initializes the controller
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            initializeGame();
            updateUI();
            
            if (!game.getCurrentPlayer().isHuman()) {
                startCpuTurn();
            }
        } catch (Exception e) {
            System.err.println("Error initializing game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeGame() {
        humanPlayer = new Player(1, "Player", true);
        players.add(humanPlayer);
        players.add(new Player(2, "CPU 1", false));
        players.add(new Player(3, "CPU 2", false));
        players.add(new Player(4, "CPU 3", false));
        
        game = new Game(players);
        game.initialize();
        
        directionIndicator.setText("Clockwise");
        
        playerUnoIndicator.setVisible(false);
        cpu1UnoIndicator.setVisible(false);
        cpu2UnoIndicator.setVisible(false);
        cpu3UnoIndicator.setVisible(false);
        cpu1UnoLabel.setVisible(false);
        cpu2UnoLabel.setVisible(false);
        cpu3UnoLabel.setVisible(false);
        
        gameMessageLabel.setText("Game started! Your turn.");
    }

    private void updateUI() {
        try {
            updatePlayerHand();
            updateCpuHands();
            
            if (game.isReversed()) {
                directionIndicator.setText("Counter-Clockwise");
            } else {
                directionIndicator.setText("Clockwise");
            }
            
            Player currentPlayer = game.getCurrentPlayer();
            currentTurnLabel.setText(currentPlayer.getName());
            
            System.out.println("Current turn: " + currentPlayer.getName());
            
            CardColor currentColor = game.getCurrentColor();
            switch (currentColor) {
                case RED -> currentColorIndicator.setFill(Color.RED);
                case GREEN -> currentColorIndicator.setFill(Color.GREEN);
                case BLUE -> currentColorIndicator.setFill(Color.BLUE);
                case YELLOW -> currentColorIndicator.setFill(Color.YELLOW);
                default -> currentColorIndicator.setFill(Color.BLACK);
            }
            
            playerUnoIndicator.setVisible(humanPlayer.hasCalledUno());
            cpu1UnoIndicator.setVisible(players.get(1).hasCalledUno());
            cpu2UnoIndicator.setVisible(players.get(2).hasCalledUno());
            cpu3UnoIndicator.setVisible(players.get(3).hasCalledUno());
            cpu1UnoLabel.setVisible(players.get(1).hasCalledUno());
            cpu2UnoLabel.setVisible(players.get(2).hasCalledUno());
            cpu3UnoLabel.setVisible(players.get(3).hasCalledUno());
            
            if (game.isGameOver()) {
                Player winner = game.getWinner();
                showGameOver(winner);
            } else {
                if (!game.getCurrentPlayer().isHuman() && !cpuTurnInProgress) {
                    startCpuTurn();
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCpuTurn() {
        if (cpuTurnInProgress || game.isGameOver()) {
            return;
        }
        
        cpuTurnInProgress = true;
        
        Platform.runLater(() -> {
            Timeline delay = new Timeline(
                new KeyFrame(Duration.seconds(1.5), event -> {
                    handleCpuTurn();
                    cpuTurnInProgress = false;
                    
                    if (!game.getCurrentPlayer().isHuman() && !game.isGameOver()) {
                        startCpuTurn();
                    }
                })
            );
            delay.play();
        });
    }

    private void updatePlayerHand() {
        playerHand.getChildren().clear();
        
        for (Card card : humanPlayer.getHand()) {
            StackPane cardPane = createCardPane(card);
            
            cardPane.setOnMouseClicked(event -> handleCardClick(card));
            
            if (game.getCurrentPlayer() == humanPlayer && game.isValidPlay(card)) {
                Rectangle highlight = new Rectangle(82, 122);
                highlight.setFill(Color.TRANSPARENT);
                highlight.setStroke(Color.YELLOW);
                highlight.setStrokeWidth(2);
                cardPane.getChildren().add(highlight);
            }
            
            playerHand.getChildren().add(cardPane);
        }
    }

    private StackPane createCardPane(Card card) {
        StackPane cardPane = new StackPane();
        Rectangle cardRect = new Rectangle(80, 120);
        
        // Set card color
        if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
            cardRect.setFill(Color.BLACK);
        } else {
            switch (card.getColor()) {
                case RED -> cardRect.setFill(Color.RED);
                case GREEN -> cardRect.setFill(Color.GREEN);
                case BLUE -> cardRect.setFill(Color.BLUE);
                case YELLOW -> cardRect.setFill(Color.YELLOW);
                default -> cardRect.setFill(Color.BLACK);
            }
        }
        cardRect.setStroke(Color.WHITE);
        cardRect.setStrokeWidth(2);
        
        Label cardLabel = new Label();
        if (card.getType() == CardType.NUMBER) {
            cardLabel.setText(String.valueOf(card.getNumber()));
        } else {
            switch (card.getType()) {
                case SKIP -> cardLabel.setText("SKIP");
                case REVERSE -> cardLabel.setText("REV");
                case DRAW_TWO -> cardLabel.setText("+2");
                case WILD -> cardLabel.setText("WILD");
                case WILD_DRAW_FOUR -> cardLabel.setText("+4");
                default -> cardLabel.setText("");
            }
        }
        cardLabel.setTextFill(Color.WHITE);
        cardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        cardPane.getChildren().addAll(cardRect, cardLabel);
        return cardPane;
    }

    private void updateCpuHands() {
        cpu1Hand.getChildren().clear();
        for (int i = 0; i < players.get(1).getHandSize(); i++) {
            Rectangle cardBack = new Rectangle(60, 90);
            cardBack.setFill(Color.BLACK);
            cardBack.setStroke(Color.WHITE);
            cpu1Hand.getChildren().add(cardBack);
        }
        
        cpu2Hand.getChildren().clear();
        for (int i = 0; i < players.get(2).getHandSize(); i++) {
            Rectangle cardBack = new Rectangle(60, 90);
            cardBack.setFill(Color.BLACK);
            cardBack.setStroke(Color.WHITE);
            cardBack.setRotate(180); // Rotated for top player
            cpu2Hand.getChildren().add(cardBack);
        }
        
        cpu3Hand.getChildren().clear();
        for (int i = 0; i < players.get(3).getHandSize(); i++) {
            Rectangle cardBack = new Rectangle(60, 90);
            cardBack.setFill(Color.BLACK);
            cardBack.setStroke(Color.WHITE);
            cardBack.setRotate(270); // Rotated for left player
            cpu3Hand.getChildren().add(cardBack);
        }
    }

    private void handleCardClick(Card card) {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        if (!game.isValidPlay(card)) {
            gameMessageLabel.setText("That card cannot be played now.");
            return;
        }
        
        selectedCard = card;
        
        if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
            colorSelectionBox.setVisible(true);
            colorSelectionBox.setManaged(true);
        } else {
            playSelectedCard(card.getColor());
        }
    }

    @FXML
    private void handleDrawCard(MouseEvent event) {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card drawnCard = game.handleDraw();
        
        if (drawnCard != null) {
            gameMessageLabel.setText("You drew " + drawnCard + ". You can play it if you want.");
            updateUI();
        } else {
            gameMessageLabel.setText("You drew a card.");
            updateUI();
        }
    }

    @FXML
    private void handleColorSelection(ActionEvent event) {
        Button button = (Button) event.getSource();
        String colorName = button.getText().toUpperCase();
        CardColor selectedColor = CardColor.valueOf(colorName);
        
        colorSelectionBox.setVisible(false);
        colorSelectionBox.setManaged(false);
        
        playSelectedCard(selectedColor);
    }

    private void playSelectedCard(CardColor chosenColor) {
        int cardIndex = humanPlayer.getHand().indexOf(selectedCard);
        
        game.playCard(cardIndex, chosenColor);
        
        // Check if the player should have called UNO
        if (humanPlayer.getHandSize() == 1 && !humanPlayer.hasCalledUno()) {
            gameMessageLabel.setText("You didn't call UNO! Drawing 2 cards as penalty.");
            for (int i = 0; i < 2; i++) {
                humanPlayer.addCard(game.drawCard());
            }
        }
        
        selectedCard = null;
        updateUI();
    }

    private void handleCpuTurn() {
        Player currentPlayer = game.getCurrentPlayer();
        
        if (currentPlayer.isHuman() || game.isGameOver()) {
            return;
        }
        
        System.out.println("CPU Turn Handler for: " + currentPlayer.getName());
        
        int cardIndex = game.getCpuMove();
        
        if (cardIndex == -1) {
            Card drawnCard = game.handleDraw();
            gameMessageLabel.setText(currentPlayer.getName() + " drew a card.");
        } else {
            Card cardToPlay = currentPlayer.getHand().get(cardIndex);
            
            CardColor chosenColor = cardToPlay.getColor();
            if (cardToPlay.getType() == CardType.WILD || cardToPlay.getType() == CardType.WILD_DRAW_FOUR) {
                chosenColor = game.getRandomColor();
            }
            
            if (currentPlayer.getHandSize() == 2) {
                currentPlayer.callUno();
                gameMessageLabel.setText(currentPlayer.getName() + " calls UNO!");
            }
            
            game.playCard(cardIndex, chosenColor);
            gameMessageLabel.setText(currentPlayer.getName() + " played " + cardToPlay);
        }
        
        updateUI();
    }

    @FXML
    private void handleCallUno() {
        if (humanPlayer.getHandSize() == 1) {
            humanPlayer.callUno();
            gameMessageLabel.setText("You called UNO!");
            updateUI();
        } else {
            gameMessageLabel.setText("You can only call UNO when you have one card left!");
        }
    }

    private void showGameOver(Player winner) {
        gameOverBox.setVisible(true);
        gameOverBox.setManaged(true);
        
        if (winner == humanPlayer) {
            winnerLabel.setText("You won!");
        } else {
            winnerLabel.setText(winner.getName() + " won!");
        }
        
        // Record game result to backend
        boolean recorded = gameService.recordGameResult(winner, players);
        if (!recorded) {
            System.err.println("Failed to record game result");
        }
    }

    @FXML
    private void handleBackToMainMenu() {
        try {
            UnoApplication.showMainMenuScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCheatSkip() {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card skipCard = new Card(game.getCurrentColor(), CardType.SKIP, 0);
        
        humanPlayer.addCard(skipCard);
        int cardIndex = humanPlayer.getHand().size() - 1;
        
        game.playCard(cardIndex, game.getCurrentColor());
        gameMessageLabel.setText("You played a Skip card!");
        
        updateUI();
    }

    @FXML
    private void handleCheatReverse() {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card reverseCard = new Card(game.getCurrentColor(), CardType.REVERSE, 0);
        
        humanPlayer.addCard(reverseCard);
        int cardIndex = humanPlayer.getHand().size() - 1;
        
        game.playCard(cardIndex, game.getCurrentColor());
        gameMessageLabel.setText("You played a Reverse card!");
        
        updateUI();
    }

    @FXML
    private void handleCheatDrawTwo() {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card drawTwoCard = new Card(game.getCurrentColor(), CardType.DRAW_TWO, 0);
        
        humanPlayer.addCard(drawTwoCard);
        int cardIndex = humanPlayer.getHand().size() - 1;
        
        game.playCard(cardIndex, game.getCurrentColor());
        gameMessageLabel.setText("You played a Draw Two card!");
        
        updateUI();
    }

    @FXML
    private void handleCheatWild() {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card wildCard = new Card(CardColor.WILD, CardType.WILD, 0);
        
        humanPlayer.addCard(wildCard);
        selectedCard = wildCard;
        
        colorSelectionBox.setVisible(true);
        colorSelectionBox.setManaged(true);
    }

    @FXML
    private void handleCheatWildDrawFour() {
        if (game.getCurrentPlayer() != humanPlayer) {
            gameMessageLabel.setText("It's not your turn!");
            return;
        }
        
        Card wildDrawFourCard = new Card(CardColor.WILD, CardType.WILD_DRAW_FOUR, 0);
        
        humanPlayer.addCard(wildDrawFourCard);
        selectedCard = wildDrawFourCard;
        
        colorSelectionBox.setVisible(true);
        colorSelectionBox.setManaged(true);
    }
}