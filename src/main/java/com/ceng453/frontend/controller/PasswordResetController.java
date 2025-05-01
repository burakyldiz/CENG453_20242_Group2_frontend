package com.ceng453.frontend.controller;

import com.ceng453.frontend.service.ApiService;
import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Controller;

@Controller
public class PasswordResetController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;
    @FXML private Button resetButton;
    @FXML private Button backButton;

    private final ApiService apiService;
    private final SceneManager sceneManager;

    public PasswordResetController(ApiService apiService, SceneManager sceneManager) {
        this.apiService = apiService;
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
        // Clear any status messages
        statusLabel.setText("");
        
        // Add enter key event handler
        emailField.setOnAction(event -> resetButton.fire());
    }

    @FXML
    public void handleResetPassword(ActionEvent event) {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            statusLabel.setText("Please enter your email address");
            return;
        }

        // Simple email validation
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Please enter a valid email address");
            return;
        }

        // Disable reset button and show loading message
        resetButton.setDisable(true);
        statusLabel.setText("Sending reset instructions...");

        // Call the password reset API
        apiService.resetPassword(email)
            .subscribe(response -> {
                Platform.runLater(() -> {
                    if (response.startsWith("Error:")) {
                        statusLabel.setText("Password reset failed: " + response.substring(7));
                        resetButton.setDisable(false);
                    } else {
                        statusLabel.setText("Password reset email sent! Check your inbox for instructions.");
                        
                        // After a short delay, navigate back to login
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000);
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
                    resetButton.setDisable(false);
                });
            });
    }

    @FXML
    public void handleBack(ActionEvent event) {
        sceneManager.showLoginScene();
    }
}
