package com.ceng453.frontend.controller;

import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class LeaderboardController {

    @FXML private TableView<LeaderboardEntry> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntry, String> usernameColumn;
    @FXML private TableColumn<LeaderboardEntry, Integer> scoreColumn;
    @FXML private ComboBox<String> timeframeComboBox;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private final ApiService apiService;
    private final SceneManager sceneManager;

    // Inner class to represent leaderboard entries in the table
    public static class LeaderboardEntry {
        private final String username;
        private final int score;

        public LeaderboardEntry(String username, int score) {
            this.username = username;
            this.score = score;
        }

        public String getUsername() {
            return username;
        }

        public int getScore() {
            return score;
        }
    }

    public LeaderboardController(ApiService apiService, SceneManager sceneManager) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
        // Set up the table columns
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        // Set up the timeframe combo box
        ObservableList<String> timeframes = FXCollections.observableArrayList(
                "Weekly", "Monthly", "All Time"
        );
        timeframeComboBox.setItems(timeframes);
        timeframeComboBox.getSelectionModel().select(0);

        // Add listener to load leaderboard when timeframe changes
        timeframeComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> loadLeaderboard());

        // Load the initial leaderboard (weekly)
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        String selectedTimeframe = timeframeComboBox.getSelectionModel().getSelectedItem();
        statusLabel.setText("Loading leaderboard...");
        
        switch (selectedTimeframe) {
            case "Weekly":
                fetchLeaderboard(apiService.getWeeklyLeaderboard());
                break;
            case "Monthly":
                fetchLeaderboard(apiService.getMonthlyLeaderboard());
                break;
            case "All Time":
                fetchLeaderboard(apiService.getAllTimeLeaderboard());
                break;
        }
    }

    private void fetchLeaderboard(reactor.core.publisher.Mono<List<Map<String, Object>>> leaderboardMono) {
        leaderboardMono.subscribe(entries -> {
            Platform.runLater(() -> {
                if (entries == null || entries.isEmpty()) {
                    statusLabel.setText("No data available for this timeframe");
                    leaderboardTable.setItems(FXCollections.observableArrayList());
                } else {
                    // Convert Map entries to LeaderboardEntry objects
                    List<LeaderboardEntry> leaderboardEntries = entries.stream()
                            .map(entry -> new LeaderboardEntry(
                                    (String) entry.get("username"),
                                    ((Number) entry.get("score")).intValue()))
                            .collect(Collectors.toList());

                    leaderboardTable.setItems(FXCollections.observableArrayList(leaderboardEntries));
                    statusLabel.setText("");
                }
            });
        }, error -> {
            Platform.runLater(() -> {
                statusLabel.setText("Error loading leaderboard: " + error.getMessage());
            });
        });
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadLeaderboard();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        sceneManager.showMainMenuScene();
    }
}
