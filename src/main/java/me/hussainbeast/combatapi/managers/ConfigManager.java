package me.hussainbeast.combatapi.managers;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.util.Logger;
import me.hussainbeast.combatapi.util.ValidationUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    
    private final CombatAPIPlugin plugin;
    private final Logger logger;
    private final File configFile;
    private final Map<String, Object> defaultValues;
    private final Map<String, ConfigValidator> validators;
    private WatchService watchService;
    private ExecutorService watchExecutor;
    private boolean hotReloadEnabled;
    
    public ConfigManager(CombatAPIPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.defaultValues = new HashMap<>();
        this.validators = new HashMap<>();
        this.hotReloadEnabled = true;
        
        initializeDefaults();
        initializeValidators();
    }
    
    private void initializeDefaults() {
        defaultValues.put("combat.duration", 15);
        defaultValues.put("combat.action-bar.enabled", true);
        defaultValues.put("combat.action-bar.format", "&c&lCOMBAT &8» &f{time}s");
        defaultValues.put("combat.action-bar.update-interval", 20);
        defaultValues.put("combat.action-bar.batch-size", 50);
        
        defaultValues.put("messages.enter-combat", "&cYou are now in combat!");
        defaultValues.put("messages.leave-combat", "&aYou are no longer in combat!");
        defaultValues.put("messages.combat-log", "&c{player} disconnected during combat!");
        
        defaultValues.put("logging.level", "INFO");
        defaultValues.put("logging.file-enabled", true);
        defaultValues.put("logging.console-enabled", true);
        defaultValues.put("logging.max-file-size", 10);
        
        defaultValues.put("error-handling.max-retries", 3);
        defaultValues.put("error-handling.retry-delay", 1000);
        defaultValues.put("error-handling.recovery-enabled", true);
        
        defaultValues.put("performance.metrics-enabled", true);
        defaultValues.put("performance.metrics-interval", 300);
        
        defaultValues.put("validation.strict-validation", true);
        defaultValues.put("validation.player-state-validation", true);
        
        defaultValues.put("async.threads", 2);
        defaultValues.put("async.timeout-seconds", 30);
    }
    
    private void initializeValidators() {
        validators.put("combat.duration", new RangeValidator(1, 300));
        validators.put("combat.action-bar.update-interval", new RangeValidator(1, 100));
        validators.put("combat.action-bar.batch-size", new RangeValidator(1, 200));
        
        validators.put("logging.level", new EnumValidator("DEBUG", "INFO", "WARN", "ERROR", "FATAL"));
        validators.put("logging.max-file-size", new RangeValidator(1, 100));
        
        validators.put("error-handling.max-retries", new RangeValidator(0, 10));
        validators.put("error-handling.retry-delay", new RangeValidator(100, 10000));
        
        validators.put("performance.metrics-interval", new RangeValidator(60, 3600));
        
        validators.put("async.threads", new RangeValidator(1, 10));
        validators.put("async.timeout-seconds", new RangeValidator(5, 300));
    }
    
    public void initialize() {
        try {
            if (!configFile.exists()) {
                plugin.saveDefaultConfig();
                logger.info("Created default configuration file");
            }
            
            validateConfiguration();
            
            if (hotReloadEnabled) {
                startFileWatcher();
            }
            
            logger.info("ConfigManager initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize ConfigManager: " + e.getMessage());
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    public boolean validateConfiguration() {
        try {
            FileConfiguration config = plugin.getConfig();
            boolean isValid = true;
            
            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                String path = entry.getKey();
                Object defaultValue = entry.getValue();
                
                if (!config.contains(path)) {
                    logger.warn("Missing configuration path: " + path + ", using default: " + defaultValue);
                    config.set(path, defaultValue);
                    isValid = false;
                } else {
                    Object value = config.get(path);
                    ConfigValidator validator = validators.get(path);
                    
                    if (validator != null && !validator.isValid(value)) {
                        logger.warn("Invalid value for " + path + ": " + value + ", using default: " + defaultValue);
                        config.set(path, defaultValue);
                        isValid = false;
                    }
                }
            }
            
            if (!isValid) {
                plugin.saveConfig();
                logger.info("Configuration has been corrected and saved");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Configuration validation failed: " + e.getMessage());
            return false;
        }
    }
    
    public CompletableFuture<Boolean> reloadConfigurationAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.reloadConfig();
                boolean isValid = validateConfiguration();
                
                if (isValid) {
                    logger.info("Configuration reloaded successfully");
                } else {
                    logger.warn("Configuration reloaded with corrections");
                }
                
                return isValid;
            } catch (Exception e) {
                logger.error("Failed to reload configuration: " + e.getMessage());
                return false;
            }
        });
    }
    
    private void startFileWatcher() {
        try {
            watchService = configFile.toPath().getFileSystem().newWatchService();
            Path configDir = configFile.getParentFile().toPath();
            configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            watchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ConfigWatcher");
                t.setDaemon(true);
                return t;
            });
            
            watchExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.context().toString().equals("config.yml")) {
                                logger.debug("Configuration file changed, reloading...");
                                
                                Thread.sleep(500);
                                
                                reloadConfigurationAsync().thenAccept(success -> {
                                    if (success) {
                                        logger.info("Hot-reload completed successfully");
                                    } else {
                                        logger.warn("Hot-reload completed with issues");
                                    }
                                });
                            }
                        }
                        
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error("Error in file watcher: " + e.getMessage());
                    }
                }
            });
            
            logger.info("Configuration file watcher started");
        } catch (Exception e) {
            logger.error("Failed to start file watcher: " + e.getMessage());
        }
    }
    
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
        if (!enabled && watchService != null) {
            shutdown();
        } else if (enabled && watchService == null) {
            startFileWatcher();
        }
    }
    
    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }
    
    public void shutdown() {
        try {
            if (watchExecutor != null) {
                watchExecutor.shutdown();
                if (!watchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    watchExecutor.shutdownNow();
                }
            }
            
            if (watchService != null) {
                watchService.close();
            }
            
            logger.info("ConfigManager shutdown complete");
        } catch (Exception e) {
            logger.error("Error during ConfigManager shutdown: " + e.getMessage());
        }
    }
    
    private interface ConfigValidator {
        boolean isValid(Object value);
    }
    
    private static class RangeValidator implements ConfigValidator {
        private final int min;
        private final int max;
        
        public RangeValidator(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        @Override
        public boolean isValid(Object value) {
            if (!(value instanceof Number)) {
                return false;
            }
            int intValue = ((Number) value).intValue();
            return intValue >= min && intValue <= max;
        }
    }
    
    private static class EnumValidator implements ConfigValidator {
        private final String[] validValues;
        
        public EnumValidator(String... validValues) {
            this.validValues = validValues;
        }
        
        @Override
        public boolean isValid(Object value) {
            if (!(value instanceof String)) {
                return false;
            }
            String stringValue = (String) value;
            for (String validValue : validValues) {
                if (validValue.equalsIgnoreCase(stringValue)) {
                    return true;
                }
            }
            return false;
        }
    }
}