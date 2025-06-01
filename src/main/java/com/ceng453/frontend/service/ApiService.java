package com.ceng453.frontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.ceng453.frontend.dto.GameStateDTO;
import com.ceng453.frontend.service.UserSessionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;

@Service
public class ApiService {

    private final WebClient webClient;
    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper;
    
    // Map to store information about created games
    private final Map<String, GameInfo> gameInfoMap = new ConcurrentHashMap<>();

    public ApiService(@Value("${api.base-url:http://localhost:8080}") String apiBaseUrl, UserSessionService userSessionService) {
        this.webClient = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.userSessionService = userSessionService;
        this.objectMapper = new ObjectMapper();
        System.out.println("ApiService initialized with base URL: " + apiBaseUrl);
    }

    // User Authentication Methods
    
    /**
     * Register with username, email, and password - returns Mono<String>
     */
    public Mono<String> register(String username, String email, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("email", email);
        request.put("password", password);

        return webClient.post()
                .uri("/users/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Registration error: " + errorMessage);
                    return Mono.just("Error: " + errorMessage);
                });
    }

    /**
     * Register with username and password - callback style
     * 
     * @param username User's username
     * @param email User's email
     * @param password User's password
     * @param onSuccess Consumer that accepts the registration confirmation
     * @param onError Consumer that handles errors
     */
    public void register(String username, String email, String password, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("email", email);
        request.put("password", password);

        System.out.println("Sending registration request with username: " + username + ", email: " + email);
        webClient.post()
                .uri("/users/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(onSuccess, onError);
    }

    /**
     * Login with username and password - returns Mono<String>
     */
    public Mono<String> login(String username, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);

        return webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Login error: " + errorMessage);
                    return Mono.just("Error: " + errorMessage);
                });
    }

    /**
     * Login with username and password - callback style
     * 
     * @param username User's username
     * @param password User's password
     * @param onSuccess Consumer that accepts the user ID on successful login
     * @param onError Consumer that handles errors
     */
    public void login(String username, String password, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);

        webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(onSuccess, onError);
    }

    /**
     * Request a password reset
     */
    public Mono<String> resetPassword(String email) {
        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        return webClient.post()
                .uri("/password/reset-request")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    return Mono.just("Error: " + errorMessage);
                });
    }

    /**
     * Confirm a password reset
     */
    public Mono<String> confirmPasswordReset(String token, String newPassword) {
        Map<String, String> request = new HashMap<>();
        request.put("token", token);
        request.put("newPassword", newPassword);

        return webClient.post()
                .uri("/password/reset-confirm")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    return Mono.just("Error: " + errorMessage);
                });
    }

    // Leaderboard Methods
    
    /**
     * Get weekly leaderboard
     */
    public Mono<List<Map<String, Object>>> getWeeklyLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/weekly")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Weekly leaderboard error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Get monthly leaderboard
     */
    public Mono<List<Map<String, Object>>> getMonthlyLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/monthly")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Monthly leaderboard error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Get all-time leaderboard
     */
    public Mono<List<Map<String, Object>>> getAllTimeLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/all-time")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("All-time leaderboard error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    // Game Results Method
    
    /**
     * Record a game result
     */
    public Mono<String> recordGameResult(Long winnerId, List<Long> playerIds) {
        Map<String, Object> request = new HashMap<>();
        request.put("winnerId", winnerId);
        request.put("playerIds", playerIds);

        return webClient.post()
                .uri("/game/record-result")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Record game result error: " + errorMessage);
                    return Mono.just("Error: " + errorMessage);
                });
    }

    // Helper methods
    
    /**
     * Convert object to JSON
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    // Multiplayer Game Methods
    
    /**
     * Creates a new game session
     * 
     * @param playerId ID of the player creating the game
     * @param onSuccess Consumer that accepts the created game ID
     * @param onError Consumer that handles errors
     */
    public void createGame(Long playerId, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        // Detailed debugging
        System.out.println("CREATE GAME DEBUG - Received player ID: " + playerId + 
                         " (Type: " + (playerId != null ? playerId.getClass().getName() : "null") + ")");
        
        // Ensure playerId is not null
        if (playerId == null) {
            System.out.println("ERROR: Attempting to create game with null playerId. This will cause backend issues.");
            onError.accept(new IllegalArgumentException("Player ID cannot be null. You must be logged in to create a game."));
            return;
        }
        
        // CRITICAL FIX: Prevent using player ID 1 for game creation, as this is causing conflicts
        if (playerId == 1L) {
            System.out.println("WARNING: Attempting to create game with default playerId=1. Generating unique ID to prevent conflicts.");
            // Generate a unique ID using timestamp and random number
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Generated unique player ID: " + playerId + " to replace default ID 1");
            
            // IMPORTANT: Update the user session with the new player ID to ensure consistency
            if (userSessionService != null) {
                userSessionService.setCurrentUserId(playerId);
                System.out.println("Updated user session with new player ID: " + playerId);
            } else {
                System.err.println("WARNING: UserSessionService is null, cannot update session with new player ID");
            }
        }
        
        // Local final variable for playerId to use in the lambda expressions
        final Long finalPlayerId = playerId;
        
        webClient.post()
            .uri("/api/gameplay/create?playerId={playerId}", finalPlayerId.toString())
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> {
                System.err.println("Error creating game: " + error.getMessage());
                if (error instanceof WebClientResponseException) {
                    WebClientResponseException wcError = (WebClientResponseException) error;
                    System.err.println("Response status: " + wcError.getStatusCode());
                    System.err.println("Response body: " + wcError.getResponseBodyAsString());
                }
            })
            .subscribe(
                response -> {
                    System.out.println("Create game response: " + response);
                    
                    // Store the game information
                    GameInfo gameInfo = new GameInfo(response, finalPlayerId);
                    gameInfoMap.put(response, gameInfo);
                    System.out.println("Stored game info for game ID: " + response + ", creator ID: " + finalPlayerId);
                    
                    // Cache the creator's ID as a known player ID for this game
                    List<Long> knownIds = new ArrayList<>();
                    knownIds.add(finalPlayerId);
                    knownPlayerIdsCache.put(response, knownIds);
                    
                    onSuccess.accept(response);
                },
                error -> {
                    String errorMessage = "Failed to create game.";
                    
                    // Try to extract more specific error message from response
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcError = (WebClientResponseException) error;
                        String responseBody = wcError.getResponseBodyAsString();
                        System.out.println("Error response body: " + responseBody);
                        
                        // Try to extract more detailed error message if available
                        if (responseBody != null && !responseBody.isEmpty()) {
                            errorMessage = "Backend error: " + responseBody;
                        }
                    }
                    
                    System.out.println("Error creating game: " + errorMessage);
                    onError.accept(new IllegalStateException(errorMessage));
                }
            );
    }
    
    /**
     * Joins an existing game session
     * 
     * @param gameId ID of the game to join
     * @param playerId ID of the player joining the game
     * @param playerName Name of the player joining
     * @param onSuccess Consumer that accepts the join response
     * @param onError Consumer that handles errors
     */
    public void joinGame(String gameId, Long playerId, String playerName, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        // Detailed debugging
        System.out.println("JOIN GAME DEBUG - Received: gameId=" + gameId + ", playerId=" + playerId + 
                         " (Type: " + (playerId != null ? playerId.getClass().getName() : "null") + "), playerName=" + playerName);
        
        // Ensure playerId is not null
        if (playerId == null) {
            System.out.println("ERROR: Attempting to join game with null playerId. This will cause backend issues.");
            onError.accept(new IllegalArgumentException("Player ID cannot be null. You must be logged in to join a game."));
            return;
        }
        
        // CRITICAL FIX: Prevent using player ID 1 for joins, as this is causing conflicts
        if (playerId == 1L) {
            System.out.println("WARNING: Attempting to join with default playerId=1. Generating unique ID to prevent conflicts.");
            // Generate a unique ID using timestamp and random number
            playerId = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000);
            System.out.println("Generated unique player ID: " + playerId + " to replace default ID 1");
            
            // IMPORTANT: Update the user session with the new player ID to ensure consistency
            if (userSessionService != null) {
                userSessionService.setCurrentUserId(playerId);
                System.out.println("Updated user session with new player ID: " + playerId);
            } else {
                System.err.println("WARNING: UserSessionService is null, cannot update session with new player ID");
            }
        }
        
        System.out.println("=== JOIN REQUEST DEBUG ===");
        System.out.println("Game ID: " + gameId);
        System.out.println("Request class: com.ceng453.dto.JoinRequestDTO");
        System.out.println("Player ID: " + playerId + " (Type: " + playerId.getClass().getName() + ")");
        System.out.println("Player Name: " + playerName);
        System.out.println("=========================");
        
        // Create the JoinRequestDTO with validated playerId and playerName
        Map<String, Object> joinRequestDTO = new HashMap<>();
        joinRequestDTO.put("playerId", playerId); // Using the validated/corrected player ID
        joinRequestDTO.put("playerName", playerName);
        
        // Save the final player ID for use in lambda expressions
        final Long finalPlayerId = playerId;
        
        String jsonRequest;
        try {
            jsonRequest = objectMapper.writeValueAsString(joinRequestDTO);
            System.out.println("Join request body: " + jsonRequest);
        } catch (JsonProcessingException e) {
            System.err.println("Error creating join request JSON: " + e.getMessage());
            onError.accept(e);
            return;
        }
        
        webClient.post()
            .uri("/api/gameplay/{gameId}/join", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(joinRequestDTO)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> {
                System.err.println("Error joining game: " + error.getMessage());
                if (error instanceof WebClientResponseException) {
                    WebClientResponseException wcError = (WebClientResponseException) error;
                    System.err.println("Response status: " + wcError.getStatusCode());
                    System.err.println("Response body: " + wcError.getResponseBodyAsString());
                }
            })
            .subscribe(
                response -> {
                    System.out.println("Join game response: " + response);
                    
                    // Update game info if we're tracking this game
                    GameInfo gameInfo = gameInfoMap.get(gameId);
                    if (gameInfo != null) {
                        gameInfo.addPlayer(finalPlayerId);
                        System.out.println("Updated game info for game ID: " + gameId + ", added player ID: " + finalPlayerId);
                        
                        // Update the known player IDs cache
                        List<Long> knownIds = knownPlayerIdsCache.getOrDefault(gameId, new ArrayList<>());
                        if (!knownIds.contains(finalPlayerId)) {
                            knownIds.add(finalPlayerId);
                            knownPlayerIdsCache.put(gameId, knownIds);
                        }
                        
                        // Check if we need to auto-start the game (second player just joined)
                        if (gameInfo.isReadyToStart()) {
                            System.out.println("Second player joined game " + gameId + ". Game will auto-start.");
                            
                            // Start the game automatically using the creator's ID
                            // The backend will shuffle and deal cards at this point
                            startGame(gameId, gameInfo.getCreatorId(), 
                                startResponse -> {
                                    System.out.println("Auto-started game " + gameId + " after second player joined: " + startResponse);
                                    gameInfo.setGameStarted(true);
                                },
                                startError -> {
                                    System.err.println("Failed to auto-start game " + gameId + ": " + startError.getMessage());
                                }
                            );
                        }
                    } else {
                        System.out.println("No game info found for game ID: " + gameId + ". Creating new entry.");
                        // Create a new entry if we don't have one (unusual case, might be joining from a different client)
                        GameInfo newGameInfo = new GameInfo(gameId, finalPlayerId);
                        gameInfoMap.put(gameId, newGameInfo);
                        
                        // Add to known player IDs cache
                        List<Long> knownIds = new ArrayList<>();
                        knownIds.add(finalPlayerId);
                        knownPlayerIdsCache.put(gameId, knownIds);
                    }
                    
                    onSuccess.accept(response);
                },
                error -> {
                    String errorMessage = "Failed to join game. It might be full or you've already joined.";
                    
                    // Try to extract more specific error message from response
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcError = (WebClientResponseException) error;
                        String responseBody = wcError.getResponseBodyAsString();
                        System.out.println("Error response body: " + responseBody);
                        
                        // Try to extract more detailed error message if available
                        if (responseBody != null && !responseBody.isEmpty()) {
                            errorMessage = "Backend error: " + responseBody;
                        }
                    }
                    
                    System.out.println("Error joining game: " + errorMessage);
                    onError.accept(new IllegalStateException(errorMessage));
                }
            );
    }
    
    /**
     * Starts a game session
     * 
     * @param gameId ID of the game to start
     * @param playerId ID of the player starting the game
     * @param onSuccess Consumer that accepts the confirmation message
     * @param onError Consumer that handles errors
     */
    public void startGame(String gameId, Long playerId, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        // Validate inputs
        if (gameId == null || gameId.isEmpty()) {
            onError.accept(new IllegalArgumentException("Game ID cannot be null or empty"));
            return;
        }
        
        if (playerId == null) {
            onError.accept(new IllegalArgumentException("Player ID cannot be null. You must be logged in to start a game."));
            return;
        }
        
        System.out.println("Starting game " + gameId + " with player ID: " + playerId);
        
        try {
            // Create PlayerActionDTO for request body
            Map<String, Object> actionDto = new HashMap<>();
            actionDto.put("playerId", playerId);
            actionDto.put("actionType", "START_GAME"); // Add action type for clarity
            
            String jsonBody = toJson(actionDto);
            System.out.println("Sending start game request body: " + jsonBody);
            
            webClient.post()
                .uri("/api/gameplay/{gameId}/start", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> {
                    System.out.println("WebClient error details: " + error.getClass().getName() + ": " + error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcError = (WebClientResponseException) error;
                        System.out.println("Response status code: " + wcError.getStatusCode());
                        System.out.println("Response body: " + wcError.getResponseBodyAsString());
                    }
                })
                .subscribe(
                    response -> {
                        System.out.println("Start game response: " + response);
                        onSuccess.accept(response);
                    },
                    error -> {
                        String errorMessage = "Failed to start game. Not enough players or other error occurred.";
                        
                        // Try to extract more specific error message from response
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            String responseBody = wcError.getResponseBodyAsString();
                            System.out.println("Error response body: " + responseBody);
                            
                            // Try to extract more detailed error message if available
                            if (responseBody != null && !responseBody.isEmpty()) {
                                errorMessage = "Backend error: " + responseBody;
                            }
                        }
                        
                        System.out.println("Error starting game: " + errorMessage);
                        onError.accept(new IllegalStateException(errorMessage));
                    }
                );
        } catch (Exception e) {
            System.out.println("Error creating start game request: " + e.getMessage());
            e.printStackTrace();
            onError.accept(e);
        }
    }

    /**
     * Get the current game state
     */
    public void getGameState(String gameId, Long playerId, Consumer<GameStateDTO> onSuccess, Consumer<Throwable> onError) {
        // Check for null or default ID 1 and provide detailed logs
        if (playerId == null) {
            System.err.println("WARNING: Null player ID passed to getGameState for game " + gameId);
            onError.accept(new IllegalArgumentException("Player ID cannot be null"));
            return;
        }
        
        // Detailed logging to track ID consistency
        // System.out.println("Getting state for game ID: " + gameId + ", player ID: " + playerId);
        
        webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/gameplay/{gameId}/state")
                .queryParam("playerId", playerId)
                .build(gameId))
            .retrieve()
            .bodyToMono(GameStateDTO.class)
            .doOnError(error -> {
                if (error.getMessage() != null && error.getMessage().contains("400 Bad Request")) {
                    System.err.println("ERROR: 400 Bad Request when getting game state. Player ID " + playerId + " may not be in game " + gameId);
                }
            })
            .subscribe(onSuccess, onError);
    }
    
    /**
     * Performs a player action in the game
     * 
     * @param gameId ID of the game
     * @param actionDto The action data
     * @param onSuccess Consumer that accepts the response
     * @param onError Consumer that handles errors
     */
    public void performAction(String gameId, Map<String, Object> actionDto, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        webClient.post()
            .uri("/api/gameplay/{gameId}/action", gameId)
            .bodyValue(actionDto)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(onSuccess, onError);
    }
    
    /**
     * Plays a card from the player's hand
     * 
     * @param gameId ID of the game
     * @param playerId ID of the player playing the card
     * @param cardDetails String representation of the card (format: COLOR_VALUE or WILD_DRAW_FOUR_CHOSENCOLOR)
     * @param onSuccess Consumer that accepts the response
     * @param onError Consumer that handles errors
     */
    public void playCard(String gameId, Long playerId, String cardDetails, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, Object> request = new HashMap<>();
        request.put("playerId", playerId);
        request.put("actionType", "PLAY_CARD");
        request.put("cardDetails", cardDetails);
        
        performAction(gameId, request, onSuccess, onError);
    }
    
    /**
     * Draws a card from the deck
     * 
     * @param gameId ID of the game
     * @param playerId ID of the player drawing the card
     * @param onSuccess Consumer that accepts the response
     * @param onError Consumer that handles errors
     */
    public void drawCard(String gameId, Long playerId, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, Object> request = new HashMap<>();
        request.put("playerId", playerId);
        request.put("actionType", "DRAW_CARD");
        
        performAction(gameId, request, onSuccess, onError);
    }
    
    /**
     * Challenges a Wild Draw Four or No UNO declaration
     * 
     * @param gameId ID of the game
     * @param playerId ID of the player issuing the challenge
     * @param targetPlayerId ID of the player being challenged
     * @param challengeType Type of challenge (CHALLENGE_WILD_DRAW_FOUR or CHALLENGE_NO_UNO)
     * @param onSuccess Consumer that accepts the response
     * @param onError Consumer that handles errors
     */
    public void challenge(String gameId, Long playerId, Long targetPlayerId, String challengeType, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, Object> request = new HashMap<>();
        request.put("playerId", playerId);
        request.put("actionType", challengeType);
        request.put("targetPlayerId", targetPlayerId);
        
        performAction(gameId, request, onSuccess, onError);
    }
    
    /**
     * Declares UNO when a player has one card left
     * 
     * @param gameId ID of the game
     * @param playerId ID of the player declaring UNO
     * @param onSuccess Consumer that accepts the response
     * @param onError Consumer that handles errors
     */
    public void declareUno(String gameId, Long playerId, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Map<String, Object> actionDto = new HashMap<>();
        actionDto.put("playerId", playerId);
        actionDto.put("actionType", "DECLARE_UNO");
        
        performAction(gameId, actionDto, onSuccess, onError);
    }
    
    // Cache for storing player IDs for each game to avoid using invalid IDs
    private static final Map<String, List<Long>> knownPlayerIdsCache = new ConcurrentHashMap<>();
    
    /**
     * Inner class to store game information for tracking game lifecycle
     */
    private static class GameInfo {
        private final String gameId;
        private final Long creatorId;
        private final Instant creationTime;
        private boolean secondPlayerJoined;
        private boolean gameStarted;
        private List<Long> playerIds;
        
        public GameInfo(String gameId, Long creatorId) {
            this.gameId = gameId;
            this.creatorId = creatorId;
            this.creationTime = Instant.now();
            this.secondPlayerJoined = false;
            this.gameStarted = false;
            this.playerIds = new ArrayList<>();
            this.playerIds.add(creatorId);
        }
        
        public void addPlayer(Long playerId) {
            if (!playerIds.contains(playerId)) {
                playerIds.add(playerId);
                if (playerIds.size() == 2) {
                    secondPlayerJoined = true;
                }
            }
        }
        
        public boolean isReadyToStart() {
            return secondPlayerJoined && !gameStarted;
        }
        
        public void setGameStarted(boolean gameStarted) {
            this.gameStarted = gameStarted;
        }
        
        public List<Long> getPlayerIds() {
            return new ArrayList<>(playerIds);
        }
        
        public Long getCreatorId() {
            return creatorId;
        }
    }
    
    /**
     * Gets the player names for a specific game
     * 
     * @param gameId ID of the game
     * @param onSuccess Consumer that accepts a map of player IDs to names
     * @param onError Consumer that handles errors
     */
    public void getPlayerNames(String gameId, Consumer<Map<Long, String>> onSuccess, Consumer<Throwable> onError) {
        // This is a workaround since the backend doesn't have a specific endpoint for player names
        System.out.println("DEBUG: Getting player names for game " + gameId);
        
        // Create a new map to hold player names
        Map<Long, String> playerNames = new HashMap<>();
        
        // We need a valid player ID to make the API call
        // First try to get the current user's ID from session
        Long currentPlayerId = userSessionService.getCurrentUserId();
        
        // If that's not available, try a previously known valid ID from our cache
        if (currentPlayerId == null) {
            System.out.println("WARNING: No current user ID available from session");
            currentPlayerId = getLastKnownPlayerIdFromGame(gameId);
        }
        
        // If we still don't have a valid player ID, use a fallback approach
        if (currentPlayerId == null) {
            System.out.println("WARNING: No valid player ID available for game " + gameId);
            // Just use a default fallback ID - this will likely fail on the server but at least we tried
            currentPlayerId = 1L;
        }
        
        final Long finalPlayerId = currentPlayerId; // Need a final reference for the lambda
        System.out.println("DEBUG: Using player ID " + finalPlayerId + " to fetch game state and player names");
        
        webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/gameplay/{gameId}/state")
                .queryParam("playerId", finalPlayerId)
                .build(gameId))
            .retrieve()
            .bodyToMono(GameStateDTO.class)
            .doOnSuccess(gameState -> {
                // Check if we have game logs that contain player information
                if (gameState != null && gameState.getGameLog() != null) {
                    // Process the logs to extract player names
                    List<Long> discoveredPlayerIds = new ArrayList<>();
                    
                    for (String logLine : gameState.getGameLog()) {
                        // Look for entries like "Players in game 123: 123456 (Player123456), 234567 (username)"
                        if (logLine.contains("Players in game") || logLine.contains("Current players")) {
                            System.out.println("DEBUG: Processing log line for player names: " + logLine);
                            // Extract player info using regex for pattern "digits (name)"
                            Pattern pattern = Pattern.compile("(\\d+)\\s*\\(([^)]+)\\)");
                            Matcher matcher = pattern.matcher(logLine);
                            
                            while (matcher.find()) {
                                try {
                                    Long playerId = Long.parseLong(matcher.group(1));
                                    String playerName = matcher.group(2);
                                    
                                    // Add to our list of discovered player IDs
                                    if (!discoveredPlayerIds.contains(playerId)) {
                                        discoveredPlayerIds.add(playerId);
                                    }
                                    
                                    // Check if we should use a real name instead of the default PlayerXXX format
                                    if (!playerName.equals("Player" + playerId)) {
                                        System.out.println("DEBUG: Found real player name in logs: " + playerId + " -> " + playerName);
                                        playerNames.put(playerId, playerName);
                                    } else if (!playerNames.containsKey(playerId)) {
                                        // Only use default format if we don't already have a better name
                                        System.out.println("DEBUG: Using default player name: " + playerId + " -> " + playerName);
                                        playerNames.put(playerId, playerName);
                                    }
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing player ID: " + e.getMessage());
                                }
                            }
                        }
                    }
                    
                    // Update our cache of known player IDs for this game
                    if (!discoveredPlayerIds.isEmpty()) {
                        updateKnownPlayerIds(gameId, discoveredPlayerIds);
                    }
                }
                
                // Also add player card counts as a fallback for any players not found in logs
                if (gameState != null && gameState.getPlayerCardCounts() != null) {
                    for (Map.Entry<Long, Integer> entry : gameState.getPlayerCardCounts().entrySet()) {
                        Long playerId = entry.getKey();
                        // Only add if we don't already have a name for this player
                        if (!playerNames.containsKey(playerId)) {
                            // Use the default format as a fallback
                            String defaultName = "Player" + playerId;
                            System.out.println("DEBUG: Adding player from card counts: " + playerId + " -> " + defaultName);
                            playerNames.put(playerId, defaultName);
                        }
                    }
                }
                
                System.out.println("DEBUG: Final player names map: " + playerNames);
                
                // Return the player names to the caller
                onSuccess.accept(playerNames);
            })
            .doOnError(error -> {
                System.err.println("Error fetching game state for player names: " + error.getMessage());
                // Return an empty map in case of error, but don't fail completely
                onSuccess.accept(playerNames);
            })
            .subscribe();
    }
    
    /**
     * Helper method to get a previously known valid player ID for a game
     * This helps avoid using the placeholder ID 0 which gets rejected by the server
     * 
     * @param gameId ID of the game
     * @return A known player ID for the game, or null if none is available
     */
    private Long getLastKnownPlayerIdFromGame(String gameId) {
        List<Long> knownPlayerIds = knownPlayerIdsCache.get(gameId);
        if (knownPlayerIds != null && !knownPlayerIds.isEmpty()) {
            // Return the first known player ID
            Long playerId = knownPlayerIds.get(0);
            System.out.println("DEBUG: Using cached player ID " + playerId + " for game " + gameId);
            return playerId;
        }
        
        // No known player IDs for this game
        System.out.println("DEBUG: No cached player IDs for game " + gameId);
        return null;
    }
    
    /**
     * Updates the cache of known player IDs for a game
     * Called when we successfully extract player IDs from game state
     * 
     * @param gameId ID of the game
     * @param playerIds List of player IDs to cache
     */
    private void updateKnownPlayerIds(String gameId, List<Long> playerIds) {
        if (gameId == null || playerIds == null || playerIds.isEmpty()) {
            return;
        }
        
        System.out.println("DEBUG: Updating known player IDs for game " + gameId + ": " + playerIds);
        knownPlayerIdsCache.put(gameId, new ArrayList<>(playerIds));
        
        // Also update the game info map if we're tracking this game
        GameInfo gameInfo = gameInfoMap.get(gameId);
        if (gameInfo != null) {
            for (Long playerId : playerIds) {
                gameInfo.addPlayer(playerId);
            }
            System.out.println("DEBUG: Updated game info player list for game " + gameId);
        }
    }
    
    /**
     * Check if a game is ready for a second player to join
     * 
     * @param gameId ID of the game to check
     * @return true if the game exists and is awaiting a second player, false otherwise
     */
    public boolean isGameAwaitingSecondPlayer(String gameId) {
        GameInfo gameInfo = gameInfoMap.get(gameId);
        if (gameInfo != null) {
            return !gameInfo.secondPlayerJoined && !gameInfo.gameStarted;
        }
        return false;
    }
    
    /**
     * Check if a game has been started
     * 
     * @param gameId ID of the game to check
     * @return true if the game exists and has been started, false otherwise
     */
    public boolean isGameStarted(String gameId) {
        GameInfo gameInfo = gameInfoMap.get(gameId);
        if (gameInfo != null) {
            return gameInfo.gameStarted;
        }
        return false;
    }
    
    /**
     * Get the player IDs for a game from our local cache
     * 
     * @param gameId ID of the game
     * @return List of player IDs, or empty list if not found
     */
    public List<Long> getCachedPlayerIds(String gameId) {
        GameInfo gameInfo = gameInfoMap.get(gameId);
        if (gameInfo != null) {
            return gameInfo.getPlayerIds();
        }
        return new ArrayList<>();
    }
}
