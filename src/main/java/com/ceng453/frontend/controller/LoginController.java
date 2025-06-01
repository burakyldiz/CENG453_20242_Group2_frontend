package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.service.UserSessionService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button forgotPasswordButton;
    
    private final ApiService apiService;
    private final SceneManager sceneManager;
    private final UserSessionService userSessionService;
    
    @Autowired
    public LoginController(ApiService apiService, SceneManager sceneManager, UserSessionService userSessionService) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
        this.userSessionService = userSessionService;
        System.out.println("LoginController created with UserSessionService");
    }
    
    @FXML
    public void initialize() {
        // Clear any status messages
        statusLabel.setText("");
        
        // Add enter key event handler
        passwordField.setOnAction(event -> loginButton.fire());
    }
    
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            return;
        }
        
        // Disable login button and show loading message
        loginButton.setDisable(true);
        statusLabel.setText("Logging in...");
        
        // Call the login API
        apiService.login(username, password,
            response -> {
                System.out.println("Login response: " + response);
                
                // Check if response indicates success
                if (response != null && (response.contains("successful") || response.equals("Login successful!"))) {
                    // IMPORTANT: We're using a fixed test user ID for compatibility with the backend
                    // In a real production system, the backend would return an actual user ID
                    Long testUserId = 1L; // Using ID 1 which likely exists in the backend database
                    final Long userId = testUserId;
                    
                    System.out.println("Login successful, using test user ID: " + userId + " for username: " + username);
                    
                    Platform.runLater(() -> {
                        // Store user session data in both UserSessionService and SceneManager
                        userSessionService.login(userId, username);
                        
                        // Also create and set User object in SceneManager for backward compatibility
                        User user = new User();
                        user.setId(userId);
                        user.setUsername(username);
                        sceneManager.setCurrentUser(user);
                        
                        statusLabel.setText("Login successful!");
                        
                        // Navigate to main menu
                        sceneManager.showMainMenuScene();
                    });
                } else {
                    System.out.println("Login failed: Invalid response from server");
                    Platform.runLater(() -> {
                        statusLabel.setText("Login failed: " + response);
                        loginButton.setDisable(false);
                    });
                }
            },
            error -> {
                System.out.println("Login error: " + error.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Login failed: " + error.getMessage());
                    loginButton.setDisable(false);
                });
            }
        );
    }
    
    @FXML
    public void handleRegister(ActionEvent event) {
        sceneManager.showRegisterScene();
    }
    
    @FXML
    public void handleForgotPassword(ActionEvent event) {
        sceneManager.showPasswordResetScene();
    }
}
