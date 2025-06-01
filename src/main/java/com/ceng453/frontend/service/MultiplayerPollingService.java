package com.ceng453.frontend.service;

import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class MultiplayerPollingService {
    
    private final ApiService apiService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private ScheduledFuture<?> pollingTask;
    private String currentSessionCode;
    private String lastUpdateTime;
    private Consumer<Map<String, Object>> gameStateUpdateHandler;
    private Consumer<String> errorHandler;
    
    public MultiplayerPollingService(ApiService apiService) {
        this.apiService = apiService;
    }
    
    /**
     * Start polling for game state updates
     * @param sessionCode The session code to poll
     * @param updateHandler Callback for when game state updates
     * @param errorHandler Callback for when errors occur
     */
    public void startPolling(String sessionCode, 
                           Consumer<Map<String, Object>> updateHandler,
                           Consumer<String> errorHandler) {
        
        // Stop any existing polling
        stopPolling();
        
        this.currentSessionCode = sessionCode;
        this.gameStateUpdateHandler = updateHandler;
        this.errorHandler = errorHandler;
        this.lastUpdateTime = null; // Start fresh
        
        System.out.println("Starting polling for session: " + sessionCode);
        
        // Poll every 2 seconds
        pollingTask = scheduler.scheduleAtFixedRate(this::pollGameState, 0, 2, TimeUnit.SECONDS);
    }
    
    /**
     * Stop polling
     */
    public void stopPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(false);
            pollingTask = null;
            System.out.println("Stopped polling");
        }
    }
    
    /**
     * Check if currently polling
     */
    public boolean isPolling() {
        return pollingTask != null && !pollingTask.isCancelled();
    }
    
    /**
     * Get the current session code being polled
     */
    public String getCurrentSessionCode() {
        return currentSessionCode;
    }
    
    private void pollGameState() {
        if (currentSessionCode == null) {
            return;
        }
        
        try {
            if (lastUpdateTime != null) {
                // Use efficient polling with lastUpdate parameter
                apiService.getGameStateIfUpdated(currentSessionCode, lastUpdateTime)
                    .subscribe(
                        gameState -> {
                            if (gameState != null) {
                                // Update received
                                handleGameStateUpdate(gameState);
                            }
                            // If gameState is null, it means no updates (304 Not Modified)
                        },
                        error -> handlePollingError(error)
                    );
            } else {
                // First poll - get full state
                apiService.getGameState(currentSessionCode)
                    .subscribe(
                        gameState -> {
                            if (gameState != null) {
                                handleGameStateUpdate(gameState);
                            }
                        },
                        error -> handlePollingError(error)
                    );
            }
        } catch (Exception e) {
            handlePollingError(e);
        }
    }
    
    private void handleGameStateUpdate(Map<String, Object> gameState) {
        try {
            // Update lastUpdateTime from the response
            Object lastUpdated = gameState.get("lastUpdated");
            if (lastUpdated != null) {
                lastUpdateTime = lastUpdated.toString();
            }
            
            // Run the update handler on the JavaFX Application Thread
            Platform.runLater(() -> {
                try {
                    if (gameStateUpdateHandler != null) {
                        gameStateUpdateHandler.accept(gameState);
                    }
                } catch (Exception e) {
                    System.err.println("Error in game state update handler: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            handlePollingError(e);
        }
    }
    
    private void handlePollingError(Throwable error) {
        System.err.println("Polling error: " + error.getMessage());
        
        Platform.runLater(() -> {
            try {
                if (errorHandler != null) {
                    errorHandler.accept("Polling error: " + error.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error in error handler: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Force a single poll (useful for immediate updates)
     */
    public void pollNow() {
        if (currentSessionCode != null) {
            pollGameState();
        }
    }
    
    /**
     * Cleanup when service is destroyed
     */
    public void shutdown() {
        stopPolling();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 