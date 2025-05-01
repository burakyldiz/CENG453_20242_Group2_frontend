package com.group2.uno.service;

import com.group2.uno.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static AuthService instance;
    private final RestClientService restClient;
    private String authToken;
    private User currentUser;
    
    private AuthService() {
        this.restClient = new RestClientService();
    }
    
    /**
     * Gets the singleton instance of AuthService
     * @return The AuthService instance
     */
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Registers a new user
     * @param username The username for the new user
     * @param email The email for the new user
     * @param password The password for the new user
     * @return True if registration was successful, false otherwise
     */
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
    
    /**
     * Logs in a user
     * @param username The username of the user
     * @param password The password of the user
     * @return True if login was successful, false otherwise
     */
    public boolean login(String username, String password) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            
            Map<String, Object> response = restClient.post("/users/login", body, Map.class);
            
            // Check for token
            if (response != null && response.containsKey("token")) {
                this.authToken = (String) response.get("token");
                restClient.setAuthToken(this.authToken);
                
                // Try to fetch user details after successful login
                fetchUserDetails();
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
    
    /**
     * Fetches user details from the backend
     */
    private void fetchUserDetails() {
        try {
            this.currentUser = restClient.get("/users/me", User.class);
        } catch (Exception e) {
            System.err.println("Failed to fetch user details: " + e.getMessage());
        }
    }
    
    /**
     * Logs out the current user
     */
    public void logout() {
        this.authToken = null;
        this.currentUser = null;
        restClient.setAuthToken(null);
    }
    
    /**
     * Requests a password reset for a user
     * @param email The email of the user
     * @return True if the request was successful, false otherwise
     */
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
    
    /**
     * Confirms a password reset
     * @param token The reset token
     * @param newPassword The new password
     * @return True if the confirmation was successful, false otherwise
     */
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
    
    /**
     * Gets the authentication token
     * @return The authentication token
     */
    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Gets the current user
     * @return The current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
}