package com.group2.uno.controller;

import com.group2.uno.UnoApplication;
import com.group2.uno.service.LeaderboardService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the leaderboard screen
 */
public class LeaderboardController implements Initializable {
    @FXML
    private VBox leaderboardContainer;
    
    @FXML
    private Label statusLabel;
    
    private final LeaderboardService leaderboardService;
    private TableView<LeaderboardEntry> weeklyTable;
    private TableView<LeaderboardEntry> monthlyTable;
    private TableView<LeaderboardEntry> allTimeTable;
    
    /**
     * Constructor
     */
    public LeaderboardController() {
        this.leaderboardService = new LeaderboardService();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Create tab pane for different leaderboard views
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            
            // Create tables for different time periods
            weeklyTable = createLeaderboardTable();
            monthlyTable = createLeaderboardTable();
            allTimeTable = createLeaderboardTable();
            
            // Create tabs and add tables to them
            Tab weeklyTab = new Tab("Weekly");
            weeklyTab.setContent(weeklyTable);
            
            Tab monthlyTab = new Tab("Monthly");
            monthlyTab.setContent(monthlyTable);
            
            Tab allTimeTab = new Tab("All Time");
            allTimeTab.setContent(allTimeTable);
            
            // Add tabs to tab pane
            tabPane.getTabs().addAll(weeklyTab, monthlyTab, allTimeTab);
            
            // Add tab pane to the container
            leaderboardContainer.getChildren().add(tabPane);
            
            // Load leaderboard data
            loadLeaderboards();
            
            // Hide status label if no errors
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        } catch (Exception e) {
            showError("Error initializing leaderboard: " + e.getMessage());
        }
    }
    
    /**
     * Creates a TableView for displaying leaderboard entries
     * @return The TableView
     */
    private TableView<LeaderboardEntry> createLeaderboardTable() {
        TableView<LeaderboardEntry> table = new TableView<>();
        
        // Create columns
        TableColumn<LeaderboardEntry, Integer> rankColumn = new TableColumn<>("Rank");
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankColumn.setPrefWidth(50);
        
        TableColumn<LeaderboardEntry, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(200);
        
        TableColumn<LeaderboardEntry, Integer> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreColumn.setPrefWidth(80);
        
        TableColumn<LeaderboardEntry, Integer> gamesPlayedColumn = new TableColumn<>("Games");
        gamesPlayedColumn.setCellValueFactory(new PropertyValueFactory<>("gamesPlayed"));
        gamesPlayedColumn.setPrefWidth(80);
        
        // Add columns to table - using explicit list to avoid varargs warning
        table.getColumns().add(rankColumn);
        table.getColumns().add(usernameColumn);
        table.getColumns().add(scoreColumn);
        table.getColumns().add(gamesPlayedColumn);
        
        return table;
    }
    
    /**
     * Loads leaderboard data from the service
     */
    private void loadLeaderboards() {
        try {
            loadWeeklyLeaderboard();
            loadMonthlyLeaderboard();
            loadAllTimeLeaderboard();
        } catch (Exception e) {
            showError("Error loading leaderboard data: " + e.getMessage());
        }
    }
    
    /**
     * Loads the weekly leaderboard
     */
    private void loadWeeklyLeaderboard() {
        try {
            List<Map<String, Object>> entries = leaderboardService.getWeeklyLeaderboard();
            updateLeaderboardTable(weeklyTable, entries);
        } catch (Exception e) {
            System.err.println("Failed to load weekly leaderboard: " + e.getMessage());
        }
    }
    
    /**
     * Loads the monthly leaderboard
     */
    private void loadMonthlyLeaderboard() {
        try {
            List<Map<String, Object>> entries = leaderboardService.getMonthlyLeaderboard();
            updateLeaderboardTable(monthlyTable, entries);
        } catch (Exception e) {
            System.err.println("Failed to load monthly leaderboard: " + e.getMessage());
        }
    }
    
    /**
     * Loads the all-time leaderboard
     */
    private void loadAllTimeLeaderboard() {
        try {
            List<Map<String, Object>> entries = leaderboardService.getAllTimeLeaderboard();
            updateLeaderboardTable(allTimeTable, entries);
        } catch (Exception e) {
            System.err.println("Failed to load all-time leaderboard: " + e.getMessage());
        }
    }
    
    /**
     * Updates a leaderboard table with entries
     * @param table The table to update
     * @param entries The entries to add to the table
     */
    private void updateLeaderboardTable(TableView<LeaderboardEntry> table, List<Map<String, Object>> entries) {
        ObservableList<LeaderboardEntry> data = FXCollections.observableArrayList();
        
        int rank = 1;
        for (Map<String, Object> entry : entries) {
            String username = (String) entry.get("username");
            int score = entry.get("score") instanceof Number ? ((Number) entry.get("score")).intValue() : 0;
            int gamesPlayed = entry.get("gamesPlayed") instanceof Number ? ((Number) entry.get("gamesPlayed")).intValue() : 0;
            
            data.add(new LeaderboardEntry(rank++, username, score, gamesPlayed));
        }
        
        table.setItems(data);
    }
    
    /**
     * Shows an error message
     * @param message The error message
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
    
    /**
     * Handles the back to main menu button click
     * @param event The action event
     */
    @FXML
    private void handleBackToMainMenu(ActionEvent event) {
        try {
            UnoApplication.showMainMenuScreen();
        } catch (Exception e) {
            showError("Error returning to main menu: " + e.getMessage());
        }
    }
    
    /**
     * Class to represent a leaderboard entry
     */
    public static class LeaderboardEntry {
        private final int rank;
        private final String username;
        private final int score;
        private final int gamesPlayed;
        
        public LeaderboardEntry(int rank, String username, int score, int gamesPlayed) {
            this.rank = rank;
            this.username = username;
            this.score = score;
            this.gamesPlayed = gamesPlayed;
        }
        
        public int getRank() {
            return rank;
        }
        
        public String getUsername() {
            return username;
        }
        
        public int getScore() {
            return score;
        }
        
        public int getGamesPlayed() {
            return gamesPlayed;
        }
    }
}