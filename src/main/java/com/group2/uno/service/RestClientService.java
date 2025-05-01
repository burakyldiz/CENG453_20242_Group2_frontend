package com.group2.uno.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Base service for making HTTP requests to the backend
 */
public class RestClientService {
    private static final String BASE_URL = "https://ceng453-20242-group2-backend.onrender.com";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public RestClientService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public <T> T get(String endpoint, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), responseType);
        } else {
            throw new IOException("Request failed with status code: " + response.statusCode());
        }
    }
    
    public <T> T post(String endpoint, Object body, Class<T> responseType) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);
        
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            if (response.body() == null || response.body().isEmpty()) {
                return null;
            }
            
            if (responseType == Map.class && response.body().contains("successful")) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", response.body());
                // Add a dummy token if we're expecting one for login
                if (endpoint.equals("/users/login")) {
                    result.put("token", "dummy-token"); // Or extract from headers if available
                }
                return (T) result;
            }
            
            try {
                return objectMapper.readValue(response.body(), responseType);
            } catch (Exception e) {
                if (responseType == Map.class) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", response.body());
                    return (T) result;
                } else if (responseType == String.class) {
                    return (T) response.body();
                } else {
                    throw e; // Re-throw if we can't handle this type
                }
            }
        } else {
            throw new IOException("Request failed with status code: " + response.statusCode() + ", body: " + response.body());
        }
    }
}