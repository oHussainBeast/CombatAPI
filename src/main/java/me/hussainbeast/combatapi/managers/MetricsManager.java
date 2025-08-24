package me.hussainbeast.combatapi.managers;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricsManager {
    
    private final CombatAPIPlugin plugin;
    private final Logger logger;
    private final CombatManager combatManager;
    
    private final Map<String, AtomicLong> counters;
    private final Map<String, Long> timings;
    private BukkitTask metricsTask;
    private boolean metricsEnabled;
    private int metricsInterval;
    
    private long startTime;
    
    public MetricsManager(CombatAPIPlugin plugin, Logger logger, CombatManager combatManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.combatManager = combatManager;
        this.counters = new ConcurrentHashMap<>();
        this.timings = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
        
        initializeCounters();
    }
    
    private void initializeCounters() {
        counters.put("combat_enters", new AtomicLong(0));
        counters.put("combat_leaves", new AtomicLong(0));
        counters.put("combat_logs", new AtomicLong(0));
        counters.put("api_calls", new AtomicLong(0));
        counters.put("async_operations", new AtomicLong(0));
        counters.put("validation_failures", new AtomicLong(0));
        counters.put("error_recoveries", new AtomicLong(0));
        counters.put("config_reloads", new AtomicLong(0));
    }
    
    public void initialize() {
        this.metricsEnabled = plugin.getConfig().getBoolean("performance.metrics-enabled", true);
        this.metricsInterval = plugin.getConfig().getInt("performance.metrics-interval", 300);
        
        if (metricsEnabled) {
            startMetricsCollection();
            logger.info("Metrics collection started (interval: " + metricsInterval + "s)");
        } else {
            logger.info("Metrics collection is disabled");
        }
    }
    
    private void startMetricsCollection() {
        if (metricsTask != null) {
            metricsTask.cancel();
        }
        
        metricsTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                collectAndLogMetrics();
            } catch (Exception e) {
                logger.error("Error collecting metrics: " + e.getMessage());
            }
        }, 20L * metricsInterval, 20L * metricsInterval);
    }
    
    private void collectAndLogMetrics() {
        long currentTime = System.currentTimeMillis();
        long uptime = currentTime - startTime;
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        int activeCombatPlayers = combatManager.getCombatPlayers().size();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        double combatPlayerPercent = onlinePlayers > 0 ? (double) activeCombatPlayers / onlinePlayers * 100 : 0;
        
        logger.info("=== CombatAPI Metrics ===");
        logger.info("Uptime: " + formatDuration(uptime));
        logger.info("Memory Usage: " + formatBytes(usedMemory) + "/" + formatBytes(maxMemory) + " (" + String.format("%.1f", memoryUsagePercent) + "%)");
        logger.info("Players in Combat: " + activeCombatPlayers + "/" + onlinePlayers + " (" + String.format("%.1f", combatPlayerPercent) + "%)");
        
        logger.info("Event Counters:");
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            logger.info("  " + entry.getKey() + ": " + entry.getValue().get());
        }
        
        if (!timings.isEmpty()) {
            logger.info("Performance Timings:");
            for (Map.Entry<String, Long> entry : timings.entrySet()) {
                logger.info("  " + entry.getKey() + ": " + entry.getValue() + "ms");
            }
        }
        
        logger.info("=========================");
    }
    
    public void incrementCounter(String counterName) {
        AtomicLong counter = counters.get(counterName);
        if (counter != null) {
            counter.incrementAndGet();
        }
    }
    
    public void addCounter(String counterName, long value) {
        AtomicLong counter = counters.get(counterName);
        if (counter != null) {
            counter.addAndGet(value);
        }
    }
    
    public void recordTiming(String operation, long durationMs) {
        timings.put(operation, durationMs);
    }
    
    public long measureOperation(String operationName, Runnable operation) {
        long startTime = System.currentTimeMillis();
        try {
            operation.run();
            return System.currentTimeMillis() - startTime;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordTiming(operationName, duration);
        }
    }
    
    public <T> T measureOperation(String operationName, java.util.function.Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.get();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordTiming(operationName, duration);
        }
    }
    
    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();
        
        snapshot.put("uptime", System.currentTimeMillis() - startTime);
        snapshot.put("active_combat_players", combatManager.getCombatPlayers().size());
        snapshot.put("online_players", Bukkit.getOnlinePlayers().size());
        
        Runtime runtime = Runtime.getRuntime();
        snapshot.put("memory_used", runtime.totalMemory() - runtime.freeMemory());
        snapshot.put("memory_max", runtime.maxMemory());
        
        Map<String, Long> counterSnapshot = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            counterSnapshot.put(entry.getKey(), entry.getValue().get());
        }
        snapshot.put("counters", counterSnapshot);
        snapshot.put("timings", new ConcurrentHashMap<>(timings));
        
        return snapshot;
    }
    
    public void resetCounters() {
        for (AtomicLong counter : counters.values()) {
            counter.set(0);
        }
        timings.clear();
        logger.info("Metrics counters and timings have been reset");
    }
    
    public void setMetricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
        if (enabled && metricsTask == null) {
            startMetricsCollection();
            logger.info("Metrics collection enabled");
        } else if (!enabled && metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
            logger.info("Metrics collection disabled");
        }
    }
    
    public void setMetricsInterval(int intervalSeconds) {
        this.metricsInterval = intervalSeconds;
        if (metricsEnabled) {
            startMetricsCollection();
            logger.info("Metrics interval updated to " + intervalSeconds + " seconds");
        }
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    public int getMetricsInterval() {
        return metricsInterval;
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public void shutdown() {
        if (metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
        }
        
        if (metricsEnabled) {
            logger.info("Final metrics collection before shutdown:");
            collectAndLogMetrics();
        }
        
        logger.info("MetricsManager shutdown complete");
    }
}