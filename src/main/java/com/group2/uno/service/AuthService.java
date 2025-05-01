package com.group2.uno.service;

import com.group2.uno.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final RestClientService restClient;
    private String authToken;
    private User currentUser;
    
    public AuthService() {
        this.restClient = new RestClientService();
    }
    
    public boolean register(String username, String email, String password) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);
            
            Object response = restClient.post("/users/register", body, Object.class);
            return response != null;
        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean login(String username, String password) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            
            Map<String, Object> response = restClient.post("/users/login", body, Map.class);
            
            // Check for token
            if (response != null && response.containsKey("token")) {
                this.authToken = (String) response.get("token");
                return true;
            }
            
            // Check for success message
            if (response != null && 
                (response.containsKey("success") && (Boolean)response.get("success") ||
                 response.containsKey("message") && response.get("message").toString().contains("successful"))) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean requestPasswordReset(String email) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            
            restClient.post("/password/reset-request", body, Object.class);
            return true;
        } catch (Exception e) {
            System.err.println("Password reset request failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean confirmPasswordReset(String token, String newPassword) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("token", token);
            body.put("newPassword", newPassword);
            
            restClient.post("/password/reset-confirm", body, Object.class);
            return true;
        } catch (Exception e) {
            System.err.println("Password reset confirmation failed: " + e.getMessage());
            return false;
        }
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
}