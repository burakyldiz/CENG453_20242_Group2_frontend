package com.group2.uno;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UnoApplication extends Application {
    private static ConfigurableApplicationContext springContext;
    private static Stage primaryStage;

    public static void main(String[] args) {
        springContext = SpringApplication.run(UnoApplication.class, args);
        
        launch(args);
    }

    @Override
    public void init() {
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("UNO Card Game");
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(new Label("UNO Card Game"));
        
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            try {
                showLoginScreen();
            } catch (Exception ex) {
                System.err.println("Error loading login screen: " + ex.getMessage());
            }
        });
        
        Button mainMenuButton = new Button("Main Menu");
        mainMenuButton.setOnAction(e -> {
            try {
                showMainMenuScreen();
            } catch (Exception ex) {
                System.err.println("Error loading main menu: " + ex.getMessage());
            }
        });
        
        root.getChildren().addAll(loginButton, mainMenuButton);
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
    }

    public static void showRegisterScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/register.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
    }

    public static void showPasswordResetScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/password-reset.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
    }

    public static void showMainMenuScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/main-menu.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
    }

    public static void showGameBoardScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/game-board.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
    }

    public static void showLeaderboardScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(UnoApplication.class.getResource("/fxml/leaderboard.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
}