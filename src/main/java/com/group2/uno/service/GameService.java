package com.group2.uno.service;

import com.group2.uno.model.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for game-related operations
 */
public class GameService {
    private final RestClientService restClient;
    private static GameService instance;
    
    private GameService() {
        this.restClient = new RestClientService();
    }
    
    /**
     * Gets the singleton instance of GameService
     * @return The GameService instance
     */
    public static synchronized GameService getInstance() {
        if (instance == null) {
            instance = new GameService();
        }
        return instance;
    }
    
    /**
     * Records the result of a game
     * @param winner The winning player
     * @param allPlayers List of all players that participated
     * @return true if the result was recorded successfully
     */
    public boolean recordGameResult(Player winner, List<Player> allPlayers) {
        try {
            long winnerId = winner.getId();
            List<Long> playerIds = allPlayers.stream()
                    .map(Player::getId)
                    .collect(Collectors.toList());
            
            Map<String, Object> body = new HashMap<>();
            body.put("winnerId", winnerId);
            body.put("playerIds", playerIds);
            body.put("timestamp", System.currentTimeMillis());
            
            // Additional game statistics
            Map<String, Object> gameStats = new HashMap<>();
            for (Player player : allPlayers) {
                Map<String, Object> playerStats = new HashMap<>();
                playerStats.put("cardsLeft", player.getHandSize());
                playerStats.put("isHuman", player.isHuman());
                
                gameStats.put(String.valueOf(player.getId()), playerStats);
            }
            body.put("statistics", gameStats);
            
            restClient.post("/games/record", body, Object.class);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to record game result: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a new online game
     * @return The ID of the created game, or null if creation failed
     */
    @SuppressWarnings("unchecked")
    public Long createGame() {
        try {
            Map<String, Object> response = restClient.post("/games/create", new HashMap<>(), Map.class);
            if (response != null && response.containsKey("gameId")) {
                return ((Number) response.get("gameId")).longValue();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to create game: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Joins an existing game
     * @param gameId The ID of the game to join
     * @return true if joining was successful
     */
    public boolean joinGame(Long gameId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("gameId", gameId);
            
            restClient.post("/games/join", body, Object.class);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to join game: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retrieves a list of available games
     * @return A list of available games
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAvailableGames() {
        try {
            return restClient.get("/games/available", List.class);
        } catch (Exception e) {
            System.err.println("Failed to get available games: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Sets the authentication token for the RestClientService
     * This should be called after successful authentication
     * @param token The authentication token
     */
    public void setAuthToken(String token) {
        restClient.setAuthToken(token);
    }
}