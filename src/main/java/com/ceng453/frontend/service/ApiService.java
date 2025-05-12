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

    // Helper methods
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}
