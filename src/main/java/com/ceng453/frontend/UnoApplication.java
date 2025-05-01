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
        springContext = SpringApplication.run(UnoApplication.class, savedArgs);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Set up the primary stage
        primaryStage.setTitle("UNO Game");
        primaryStage.setResizable(false);
        
        // Create and inject the scene manager
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
