package com.ceng453.frontend.service;

import org.springframework.stereotype.Service;

/**
 * Service to maintain user session information across different controllers
 */
@Service
public class UserSessionService {

    private Long currentUserId;
    private String currentUsername;
    private boolean loggedIn = false;

    /**
     * Stores user session information after successful login
     * 
     * @param userId User ID from login response
     * @param username Username of the logged-in user
     */
    public void login(Long userId, String username) {
        System.out.println("UserSessionService.login called with userId=" + userId + ", username=" + username);
        
        if (userId == null) {
            System.out.println("WARNING: Attempted to login with null userId");
            return;
        }
        
        this.currentUserId = userId;
        this.currentUsername = username;
        this.loggedIn = true;
        System.out.println("User logged in successfully: " + username + " (ID: " + userId + ")");
        System.out.println("Session state after login - isLoggedIn: " + this.loggedIn + ", userId: " + this.currentUserId);
    }

    /**
     * Clears session data on logout
     */
    public void logout() {
        this.currentUserId = null;
        this.currentUsername = null;
        this.loggedIn = false;
        System.out.println("User logged out");
    }

    /**
     * Checks if a user is currently logged in
     * 
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Get the current user ID
     * 
     * @return User ID or null if not logged in
     */
    public Long getCurrentUserId() {
        System.out.println("UserSessionService.getCurrentUserId called, returning: " + currentUserId);
        if (currentUserId == null && loggedIn) {
            System.out.println("WARNING: User is marked as logged in but has null ID");
        }
        return currentUserId;
    }

    /**
     * Get the current username
     * 
     * @return Username or null if not logged in
     */
    public String getCurrentUsername() {
        return currentUsername;
    }
    
    /**
     * Set the current user ID
     * 
     * @param userId The new user ID to set
     */
    public void setCurrentUserId(Long userId) {
        System.out.println("UserSessionService.setCurrentUserId called, updating ID from " + currentUserId + " to " + userId);
        this.currentUserId = userId;
    }
}
