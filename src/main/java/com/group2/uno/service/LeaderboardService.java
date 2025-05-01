package com.group2.uno.service;

import java.util.List;
import java.util.Map;

public class LeaderboardService {
    private final RestClientService restClient;
    
    public LeaderboardService() {
        this.restClient = new RestClientService();
    }
    
    public List<Map<String, Object>> getWeeklyLeaderboard() {
        try {
            return restClient.get("/leaderboard/weekly", List.class);
        } catch (Exception e) {
            System.err.println("Failed to get weekly leaderboard: " + e.getMessage());
            return List.of();
        }
    }
    
    public List<Map<String, Object>> getMonthlyLeaderboard() {
        try {
            return restClient.get("/leaderboard/monthly", List.class);
        } catch (Exception e) {
            System.err.println("Failed to get monthly leaderboard: " + e.getMessage());
            return List.of();
        }
    }
    
    public List<Map<String, Object>> getAllTimeLeaderboard() {
        try {
            return restClient.get("/leaderboard/all-time", List.class);
        } catch (Exception e) {
            System.err.println("Failed to get all-time leaderboard: " + e.getMessage());
            return List.of();
        }
    }
}