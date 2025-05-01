package com.group2.uno.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameService {
    private final RestClientService restClient;
    
    public GameService() {
        this.restClient = new RestClientService();
    }
    
    /**
     * Records the result of a game
     * @param winnerId the ID of the winning player
     * @param playerIds list of all player IDs that participated
     * @return true if the result was recorded successfully
     */
    public boolean recordGameResult(long winnerId, List<Long> playerIds) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("winnerId", winnerId);
            body.put("playerIds", playerIds);
            
            restClient.post("/games/record", body, Object.class);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to record game result: " + e.getMessage());
            return false;
        }
    }
}