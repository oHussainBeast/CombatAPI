package me.hussainbeast.combatapi.util;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ErrorHandler {
    
    private final CombatAPIPlugin plugin;
    private final Logger logger;
    
    public ErrorHandler(CombatAPIPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    public <T> T executeWithRecovery(Supplier<T> operation, Supplier<T> fallback, String operationName) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("Error in operation '" + operationName + "': " + e.getMessage(), e);
            if (fallback != null) {
                try {
                    logger.debug("Attempting fallback for operation: " + operationName);
                    return fallback.get();
                } catch (Exception fallbackException) {
                    logger.error("Fallback also failed for operation '" + operationName + "': " + fallbackException.getMessage(), fallbackException);
                }
            }
            return null;
        }
    }
    
    public void executeWithRecovery(Runnable operation, Runnable fallback, String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.error("Error in operation '" + operationName + "': " + e.getMessage(), e);
            if (fallback != null) {
                try {
                    logger.debug("Attempting fallback for operation: " + operationName);
                    fallback.run();
                } catch (Exception fallbackException) {
                    logger.error("Fallback also failed for operation '" + operationName + "': " + fallbackException.getMessage(), fallbackException);
                }
            }
        }
    }
    
    public <T> CompletableFuture<T> executeAsyncWithRecovery(Supplier<T> operation, Supplier<T> fallback, String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                logger.error("Error in async operation '" + operationName + "': " + e.getMessage(), e);
                if (fallback != null) {
                    try {
                        logger.debug("Attempting fallback for async operation: " + operationName);
                        return fallback.get();
                    } catch (Exception fallbackException) {
                        logger.error("Async fallback also failed for operation '" + operationName + "': " + fallbackException.getMessage(), fallbackException);
                    }
                }
                return null;
            }
        });
    }
    
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long delayMs, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt " + attempt + "/" + maxRetries + " failed for operation '" + operationName + "': " + e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted for operation: " + operationName, ie);
                        break;
                    }
                }
            }
        }
        
        logger.error("All " + maxRetries + " attempts failed for operation '" + operationName + "'", lastException);
        return null;
    }
    
    public void executeWithRetry(Runnable operation, int maxRetries, long delayMs, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                operation.run();
                return;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt " + attempt + "/" + maxRetries + " failed for operation '" + operationName + "': " + e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted for operation: " + operationName, ie);
                        break;
                    }
                }
            }
        }
        
        logger.error("All " + maxRetries + " attempts failed for operation '" + operationName + "'", lastException);
    }
    
    public boolean isPlayerValid(Player player) {
        if (player == null) {
            logger.debug("Player is null");
            return false;
        }
        
        if (!player.isOnline()) {
            logger.debug("Player " + player.getName() + " is not online");
            return false;
        }
        
        if (!player.isValid()) {
            logger.debug("Player " + player.getName() + " is not valid");
            return false;
        }
        
        return true;
    }
    
    public void handleCriticalError(String message, Throwable throwable) {
        logger.fatal("CRITICAL ERROR: " + message, throwable);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("combatapi.admin")) {
                    player.sendMessage("§c§l[CombatAPI] §cCritical error occurred! Check console for details.");
                }
            }
        });
    }
    
    public void handlePlayerError(Player player, String operation, Exception e) {
        if (isPlayerValid(player)) {
            logger.error("Error for player " + player.getName() + " in operation '" + operation + "': " + e.getMessage(), e);
        } else {
            logger.error("Error in operation '" + operation + "' with invalid player: " + e.getMessage(), e);
        }
    }
    
    public void scheduleErrorRecovery(Runnable recoveryTask, long delayTicks, String taskName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                logger.info("Executing recovery task: " + taskName);
                recoveryTask.run();
                logger.info("Recovery task completed successfully: " + taskName);
            } catch (Exception e) {
                logger.error("Recovery task failed: " + taskName, e);
            }
        }, delayTicks);
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class RecoveryException extends RuntimeException {
        public RecoveryException(String message) {
            super(message);
        }
        
        public RecoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}