package com.ceng453.frontend;

import com.ceng453.frontend.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UnoApplication extends Application {

    private static String[] savedArgs;
    private ConfigurableApplicationContext springContext;
    
    public static void main(String[] args) {
        savedArgs = args;
        launch(args);
    }
    
    @Override
    public void init() {
        // Set server port explicitly based on active profile or command line argument
        String activeProfile = System.getProperty("spring.profiles.active");
        
        if (activeProfile != null) {
            switch (activeProfile) {
                case "player1":
                    System.setProperty("server.port", "8083");
                    System.out.println("Starting Player 1 with server port: 8083");
                    break;
                case "player2":
                    System.setProperty("server.port", "8084");
                    System.out.println("Starting Player 2 with server port: 8084");
                    break;
                default:
                    // If custom port specified directly, use that
                    String serverPort = System.getProperty("server.port");
                    if (serverPort != null && !serverPort.isEmpty()) {
                        System.out.println("Using specified server port: " + serverPort);
                    } else {
                        System.out.println("Using default server port from application.properties");
                    }
            }
        } else {
            // No profile, check for direct port specification
            String serverPort = System.getProperty("server.port");
            if (serverPort != null && !serverPort.isEmpty()) {
                System.out.println("Using specified server port: " + serverPort);
            } else {
                System.out.println("Using default server port from application.properties");
            }
        }
        
        // Start Spring Boot application context
        springContext = SpringApplication.run(UnoApplication.class, savedArgs);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Configure stage
        primaryStage.setTitle("UNO Game");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Initialize scene manager
        SceneManager sceneManager = springContext.getBean(SceneManager.class);
        sceneManager.setPrimaryStage(primaryStage);
        
        // Show the login scene to start
        sceneManager.showLoginScene();
        
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        springContext.close();
    }
}
