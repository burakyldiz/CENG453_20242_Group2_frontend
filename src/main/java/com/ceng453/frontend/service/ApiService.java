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

    public ApiService(@Value("${api.base-url:https://ceng453-group2-uno.onrender.com}") String apiBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // User Authentication Methods
    public Mono<String> register(String username, String email, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("email", email);
        requestBody.put("password", password);

        return webClient.post()
                .uri("/users/register")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Register error: " + ex.getMessage() + ", Response: " + ex.getResponseBodyAsString());
                    return Mono.just("Error: " + ex.getResponseBodyAsString());
                })
                .onErrorResume(Exception.class, ex -> {
                    System.err.println("Unexpected error during registration: " + ex.getMessage());
                    ex.printStackTrace();
                    return Mono.just("Error: Connection failed - " + ex.getMessage());
                });
    }

    public Mono<String> login(String username, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        System.out.println("Attempting login for user: " + username + " to backend API");

        return webClient.post()
                .uri("/users/login")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> System.out.println("Login response: " + response))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Login error: " + ex.getMessage() + ", Response: " + ex.getResponseBodyAsString());
                    return Mono.just("Error: " + ex.getResponseBodyAsString());
                })
                .onErrorResume(Exception.class, ex -> {
                    System.err.println("Unexpected error during login: " + ex.getMessage());
                    ex.printStackTrace();
                    return Mono.just("Error: Connection failed - " + ex.getMessage());
                });
    }

    public Mono<String> resetPassword(String email) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);

        return webClient.post()
                .uri("/password-reset/request")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    return Mono.just("Error: " + ex.getResponseBodyAsString());
                });
    }

    public Mono<String> confirmPasswordReset(String token, String newPassword) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", token);
        requestBody.put("newPassword", newPassword);

        return webClient.post()
                .uri("/password-reset/confirm")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    return Mono.just("Error: " + ex.getResponseBodyAsString());
                });
    }

    // Leaderboard Methods
    public Mono<List<Map<String, Object>>> getWeeklyLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/weekly")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Error fetching weekly leaderboard: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<List<Map<String, Object>>> getMonthlyLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/monthly")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Error fetching monthly leaderboard: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<List<Map<String, Object>>> getAllTimeLeaderboard() {
        return webClient.get()
                .uri("/leaderboard/all-time")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Error fetching all-time leaderboard: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    // Game Results Method
    public Mono<String> recordGameResult(Long winnerId, List<Long> playerIds) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("winnerId", winnerId);
        requestBody.put("playerIds", playerIds);

        return webClient.post()
                .uri("/games/record")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    return Mono.just("Error: " + ex.getResponseBodyAsString());
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
