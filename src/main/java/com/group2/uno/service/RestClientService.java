package com.group2.uno.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
    private String authToken;
    
    public RestClientService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Sets the authentication token for subsequent requests
     * @param token The authentication token
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    /**
     * Gets the current authentication token
     * @return The authentication token
     */
    public String getAuthToken() {
        return authToken;
    }
    
    public <T> T get(String endpoint, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json");
        
        // Add authentication if token is available
        addAuthHeader(requestBuilder);
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        return handleResponse(response, responseType);
    }
    
    public <T> T post(String endpoint, Object body, Class<T> responseType) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json");
        
        // Add authentication if token is available
        addAuthHeader(requestBuilder);
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        return handleResponse(response, responseType);
    }
    
    /**
     * Adds authorization header to the request builder if auth token is available
     * @param requestBuilder The request builder to add the header to
     */
    private void addAuthHeader(HttpRequest.Builder requestBuilder) {
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
    }
    
    /**
     * Handles the HTTP response and converts it to the expected type
     * @param response The HTTP response
     * @param responseType The expected response type
     * @return The parsed response object
     * @throws IOException If parsing fails or if the response indicates an error
     */
    @SuppressWarnings("unchecked")
    private <T> T handleResponse(HttpResponse<String> response, Class<T> responseType) throws IOException {
        int statusCode = response.statusCode();
        
        if (statusCode >= 200 && statusCode < 300) {
            if (response.body() == null || response.body().isEmpty()) {
                return null;
            }
            
            if (responseType == Map.class && response.body().contains("successful")) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", response.body());
                // Add a token if we're expecting one for login
                if (response.body().contains("token")) {
                    // Try to extract token from response
                    try {
                        Map<String, Object> responseMap = objectMapper.readValue(
                            response.body(), 
                            new TypeReference<Map<String, Object>>() {}
                        );
                        if (responseMap.containsKey("token")) {
                            result.put("token", responseMap.get("token"));
                        }
                    } catch (Exception e) {
                        // Fall back to dummy token if parsing fails
                        result.put("token", "dummy-token");
                    }
                }
                return (T) result;
            }
            
            try {
                if (responseType == Map.class) {
                    // Use TypeReference for proper generic type handling
                    return (T) objectMapper.readValue(
                        response.body(), 
                        new TypeReference<Map<String, Object>>() {}
                    );
                } else if (responseType == java.util.List.class) {
                    // Handle list responses with proper generic type
                    return (T) objectMapper.readValue(
                        response.body(),
                        new TypeReference<java.util.List<Map<String, Object>>>() {}
                    );
                } else {
                    return objectMapper.readValue(response.body(), responseType);
                }
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
        } else if (statusCode == 401) {
            throw new IOException("Authentication failed. Please log in again.");
        } else if (statusCode == 403) {
            throw new IOException("You don't have permission to access this resource.");
        } else {
            throw new IOException("Request failed with status code: " + statusCode + ", body: " + response.body());
        }
    }
}