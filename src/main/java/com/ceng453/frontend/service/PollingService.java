package com.ceng453.frontend.service;

import com.ceng453.frontend.dto.GameStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service // Or make it a regular class if not managed by Spring in this context
public class PollingService {

    private final ApiService apiService;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTaskFuture;

    private String currentGameId;
    private Long currentPlayerId;
    private Consumer<GameStateDTO> onSuccessCallback;
    private Consumer<Throwable> onErrorCallback;

    @Autowired
    public PollingService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void startPolling(String gameId, Long playerId, Consumer<GameStateDTO> onSuccess, Consumer<Throwable> onError) {
        if (scheduler != null && !scheduler.isShutdown()) {
            stopPolling(); // Stop any existing polling
        }
        
        // Validate player ID before storing it
        if (playerId == null) {
            System.err.println("ERROR: Cannot start polling with null player ID for game " + gameId);
            if (onError != null) {
                onError.accept(new IllegalArgumentException("Player ID cannot be null for polling"));
            }
            return;
        }
        
        if (playerId == 1L) {
            System.err.println("WARNING: Using default player ID (1) for polling game " + gameId + ". This may cause issues.");
        }
        
        System.out.println("Starting polling for game " + gameId + " with player ID " + playerId);
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.currentGameId = gameId;
        this.currentPlayerId = playerId; // Store the validated player ID
        this.onSuccessCallback = onSuccess;
        this.onErrorCallback = onError;

        Runnable pollingRunnable = () -> {
            try {
                // Every polling cycle, log the game ID and player ID being used
                System.out.println("Polling cycle - game: " + currentGameId + ", player: " + currentPlayerId);
                
                apiService.getGameState(currentGameId, currentPlayerId, gameState -> {
                    // Successfully received game state
                    if (onSuccessCallback != null) {
                        onSuccessCallback.accept(gameState);
                    }
                }, error -> {
                    // Log detailed error information
                    System.err.println("Error polling game state for game " + currentGameId + ", player " + currentPlayerId + ": " + error.getMessage());
                    if (onErrorCallback != null) {
                        onErrorCallback.accept(error);
                    }
                });
            } catch (Exception e) {
                if (onErrorCallback != null) {
                    onErrorCallback.accept(e);
                }
            }
        };

        // Poll every 2 seconds, with an initial delay of 0 seconds
        // Adjust polling interval as needed
        this.pollingTaskFuture = scheduler.scheduleAtFixedRate(pollingRunnable, 0, 2, TimeUnit.SECONDS);
        System.out.println("Polling started for game: " + gameId + ", player: " + playerId);
    }

    public void stopPolling() {
        if (pollingTaskFuture != null && !pollingTaskFuture.isDone()) {
            pollingTaskFuture.cancel(true); // true to interrupt the running task
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // Attempt to stop all actively executing tasks
            try {
                // Wait a while for tasks to respond to being cancelled
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Polling scheduler did not terminate in time.");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Polling stopped for game: " + currentGameId);
        this.scheduler = null;
        this.pollingTaskFuture = null;
    }

    // Optional: A method to check if polling is active
    public boolean isPollingActive() {
        return scheduler != null && !scheduler.isShutdown() && pollingTaskFuture != null && !pollingTaskFuture.isDone();
    }
}
