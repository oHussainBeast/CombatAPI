package me.hussainbeast.combatapi.util;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncAPI {
    
    private final CombatAPIPlugin plugin;
    private final Logger logger;
    private final ExecutorService executor;
    private final int maxThreads;
    
    public AsyncAPI(CombatAPIPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.maxThreads = plugin.getConfig().getInt("settings.async-threads", 4);
        this.executor = Executors.newFixedThreadPool(maxThreads);
        
        logger.info("AsyncAPI initialized with " + maxThreads + " threads");
    }
    
    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                logger.error("Async task failed: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Async task failed: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    public <T> CompletableFuture<T> executeAsyncWithTimeout(Supplier<T> task, long timeoutSeconds) {
        CompletableFuture<T> future = executeAsync(task);
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!future.isDone()) {
                timeoutFuture.completeExceptionally(new java.util.concurrent.TimeoutException("Task timed out after " + timeoutSeconds + " seconds"));
            }
        }, timeoutSeconds * 20L);
        
        return CompletableFuture.anyOf(future, timeoutFuture)
                .thenCompose(result -> {
                    if (future.isDone() && !future.isCompletedExceptionally()) {
                        return future;
                    } else {
                        CompletableFuture<T> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new java.util.concurrent.TimeoutException("Task timed out"));
                        return failed;
                    }
                })
                .exceptionally(throwable -> {
                    logger.warn("Async task timed out after " + timeoutSeconds + " seconds");
                    return null;
                });
    }
    
    public void executeAsyncThenSync(Runnable asyncTask, Runnable syncCallback) {
        executeAsync(asyncTask).thenRun(() -> {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, syncCallback);
            }
        });
    }
    
    public <T> void executeAsyncThenSync(Supplier<T> asyncTask, Consumer<T> syncCallback) {
        executeAsync(asyncTask).thenAccept(result -> {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> syncCallback.accept(result));
            }
        });
    }
    
    public CompletableFuture<Boolean> enterCombatAsync(Player player, Player attacker) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayer(player);
                ValidationUtil.validatePlayer(attacker);
                
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getCombatManager().enterCombat(player, attacker);
                    });
                    return true;
                }
                return false;
            } catch (Exception e) {
                logger.error("Failed to enter combat asynchronously: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> leaveCombatAsync(Player player) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayer(player);
                
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getCombatManager().leaveCombat(player);
                    });
                    return true;
                }
                return false;
            } catch (Exception e) {
                logger.error("Failed to leave combat asynchronously: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> isInCombatAsync(UUID playerUUID) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayerUUID(playerUUID);
                return plugin.getCombatManager().isInCombat(playerUUID);
            } catch (Exception e) {
                logger.error("Failed to check combat status asynchronously: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Player> getLastAttackerAsync(Player player) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayer(player);
                return plugin.getCombatManager().getLastAttacker(player);
            } catch (Exception e) {
                logger.error("Failed to get last attacker asynchronously: " + e.getMessage(), e);
                return null;
            }
        });
    }
    
    public CompletableFuture<Void> saveCombatDataAsync(UUID playerUUID, String data) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayerUUID(playerUUID);
                ValidationUtil.validateString(data, "combatData");
                
                logger.debug("Saving combat data for player: " + playerUUID);
                
            } catch (Exception e) {
                logger.error("Failed to save combat data: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    public CompletableFuture<String> loadCombatDataAsync(UUID playerUUID) {
        return executeAsync(() -> {
            try {
                ValidationUtil.validatePlayerUUID(playerUUID);
                
                logger.debug("Loading combat data for player: " + playerUUID);
                
                return "";
            } catch (Exception e) {
                logger.error("Failed to load combat data: " + e.getMessage(), e);
                return null;
            }
        });
    }
    
    public BukkitTask scheduleAsyncRepeating(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Scheduled async task failed: " + e.getMessage(), e);
            }
        }, delay, period);
    }
    
    public BukkitTask scheduleAsync(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Scheduled async task failed: " + e.getMessage(), e);
            }
        }, delay);
    }
    
    public void shutdown() {
        logger.info("Shutting down AsyncAPI...");
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while shutting down executor", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("AsyncAPI shutdown complete");
    }
    
    public boolean isShutdown() {
        return executor.isShutdown();
    }
    
    public int getActiveThreadCount() {
        return maxThreads;
    }
}