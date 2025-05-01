package com.ceng453.frontend.controller;

import com.ceng453.frontend.model.User;
import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    
    public LoginController(ApiService apiService, SceneManager sceneManager) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
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
        apiService.login(username, password)
            .subscribe(response -> {
                Platform.runLater(() -> {
                    if (response.startsWith("Error:")) {
                        statusLabel.setText("Login failed: Invalid username or password");
                        loginButton.setDisable(false);
                    } else {
                        // Login successful, create user object
                        User user = new User();
                        user.setUsername(username);
                        
                        // Set user in scene manager
                        sceneManager.setCurrentUser(user);
                        
                        // Navigate to main menu
                        sceneManager.showMainMenuScene();
                    }
                });
            }, error -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error connecting to server. Please try again.");
                    loginButton.setDisable(false);
                });
            });
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
