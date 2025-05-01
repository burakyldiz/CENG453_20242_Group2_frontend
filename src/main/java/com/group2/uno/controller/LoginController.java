package com.group2.uno.controller;

import com.group2.uno.UnoApplication;
import com.group2.uno.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear any previous error messages
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password cannot be empty");
            return;
        }

        // Attempt login
        try {
            boolean success = authService.login(username, password);
            if (success) {
                // Navigate to main menu
                try {
                    UnoApplication.showMainMenuScreen();
                } catch (Exception e) {
                    errorLabel.setText("Error loading main menu: " + e.getMessage());
                }
            } else {
                errorLabel.setText("Invalid username or password");
            }
        } catch (Exception e) {
            errorLabel.setText("Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegisterNavigation(ActionEvent event) {
        try {
            UnoApplication.showRegisterScreen();
        } catch (Exception e) {
            errorLabel.setText("Error loading registration screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            UnoApplication.showPasswordResetScreen();
        } catch (Exception e) {
            errorLabel.setText("Error loading password reset screen: " + e.getMessage());
        }
    }
}