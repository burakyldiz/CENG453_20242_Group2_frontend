package com.ceng453.frontend.controller;

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
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private final ApiService apiService;
    private final SceneManager sceneManager;

    public RegisterController(ApiService apiService, SceneManager sceneManager) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
        // Clear any status messages
        statusLabel.setText("");

        // Add enter key event handler
        confirmPasswordField.setOnAction(event -> registerButton.fire());
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Basic validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        // Simple email validation
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Please enter a valid email address");
            return;
        }

        // Disable register button and show loading message
        registerButton.setDisable(true);
        statusLabel.setText("Registering...");

        // Call the register API
        apiService.register(username, email, password)
            .subscribe(response -> {
                Platform.runLater(() -> {
                    if (response.startsWith("Error:")) {
                        statusLabel.setText("Registration failed: " + response.substring(7));
                        registerButton.setDisable(false);
                    } else {
                        statusLabel.setText("Registration successful! You can now login.");
                        
                        // After a short delay, navigate back to login
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> sceneManager.showLoginScene());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                });
            }, error -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error connecting to server. Please try again.");
                    registerButton.setDisable(false);
                });
            });
    }

    @FXML
    public void handleBack(ActionEvent event) {
        sceneManager.showLoginScene();
    }
}
