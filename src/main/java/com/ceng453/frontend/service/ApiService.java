package com.ceng453.frontend.service;

import com.ceng453.frontend.model.User;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ApiService(@Value("${api.base-url:https://ceng453-20242-group2-backend.onrender.com}") String apiBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
        System.out.println("ApiService initialized with base URL: " + apiBaseUrl);
    }

    // User Authentication Methods
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

    public Mono<User> login(String username, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);

        return webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(User.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Login error: " + errorMessage);
                    return Mono.empty();
                });
    }

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

    // Multiplayer Methods
    public Mono<List<Map<String, Object>>> getAvailableGames() {
        return webClient.get()
                .uri("/api/multiplayer/available")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Get available games error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Map<String, Object>> createGameSession(Long userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);

        return webClient.post()
                .uri("/api/multiplayer/create")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Create game session error: " + errorMessage);
                    return Mono.empty();
                });
    }

    public Mono<Map<String, Object>> joinGameSession(String sessionCode, Long userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionCode", sessionCode);
        request.put("userId", userId);

        return webClient.post()
                .uri("/api/multiplayer/join")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Join game session error: " + errorMessage);
                    return Mono.empty();
                });
    }

    public Mono<Map<String, Object>> startGame(String sessionCode, Long userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionCode", sessionCode);
        request.put("userId", userId);

        return webClient.post()
                .uri("/api/multiplayer/start")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Start game error: " + errorMessage);
                    return Mono.empty();
                });
    }

    public Mono<Map<String, Object>> getGameState(String sessionCode) {
        return webClient.get()
                .uri("/api/multiplayer/state/{sessionCode}", sessionCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Get game state error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Map<String, Object>> getGameStateIfUpdated(String sessionCode, String lastUpdate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/multiplayer/state/{sessionCode}")
                    .queryParam("lastUpdate", lastUpdate)
                    .build(sessionCode))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 304) {
                        // No updates - return empty to indicate no changes
                        return Mono.empty();
                    }
                    System.err.println("Get game state if updated error: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<String> leaveGameSession(String sessionCode, Long userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionCode", sessionCode);
        request.put("userId", userId);
        
        return webClient.post()
                .uri("/api/multiplayer/leave")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<Map<String, Object>> sendGameMove(String sessionCode, Long userId, String moveType, Map<String, Object> moveData) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionCode", sessionCode);
        request.put("userId", userId);
        request.put("type", moveType);
        
        // Add all move-specific data
        request.putAll(moveData);
        
        return webClient.post()
                .uri("/api/multiplayer/move")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    // Helper methods
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Records the result of a multiplayer game.
     * Adds +1 to the winner's score and -1 to all other players' scores.
     * 
     * @param winnerId The ID of the winning player
     * @param playerIds List of all player IDs who participated in the game
     * @return A Mono containing the response from the server
     */
    public Mono<String> recordMultiplayerGameResult(Long winnerId, List<Long> playerIds) {
        Map<String, Object> request = new HashMap<>();
        request.put("winnerId", winnerId);
        request.put("playerIds", playerIds);

        System.out.println("Recording multiplayer game result for winner ID: " + winnerId + ", players: " + playerIds);

        return webClient.post()
                .uri("/games/record")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("Successfully recorded multiplayer game result: " + response);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMessage = ex.getResponseBodyAsString();
                    System.err.println("Record multiplayer game result error: " + errorMessage);
                    return Mono.just("Error: " + errorMessage);
                });
    }
}
