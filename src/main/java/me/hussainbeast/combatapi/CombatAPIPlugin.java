package me.hussainbeast.combatapi;

import me.hussainbeast.combatapi.api.CombatAPIProvider;
import me.hussainbeast.combatapi.api.CombatAPIImpl;
import me.hussainbeast.combatapi.managers.CombatManager;
import me.hussainbeast.combatapi.managers.ConfigManager;
import me.hussainbeast.combatapi.managers.MetricsManager;
import me.hussainbeast.combatapi.managers.VersionManager;
import me.hussainbeast.combatapi.listeners.CombatListener;
import me.hussainbeast.combatapi.listeners.PlayerQuitListener;
import me.hussainbeast.combatapi.commands.CombatAPICommand;
import me.hussainbeast.combatapi.util.Logger;
import me.hussainbeast.combatapi.util.ErrorHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class CombatAPIPlugin extends JavaPlugin {
    
    private static CombatAPIPlugin instance;
    private CombatManager combatManager;
    private ConfigManager configManager;
    private MetricsManager metricsManager;
    private VersionManager versionManager;
    private Logger customLogger;
    private ErrorHandler errorHandler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            saveDefaultConfig();
            
            this.customLogger = new Logger(this);
            this.errorHandler = new ErrorHandler(this, customLogger);
            this.versionManager = new VersionManager(this, customLogger);
            versionManager.logVersionInfo();
            
            customLogger.info("Initializing CombatAPI v" + getDescription().getVersion());
            
            this.configManager = new ConfigManager(this, customLogger);
            configManager.initialize();
            
            this.combatManager = new CombatManager(this, customLogger, errorHandler);
            
            this.metricsManager = new MetricsManager(this, customLogger, combatManager);
            metricsManager.initialize();
            combatManager.setMetricsManager(metricsManager);
            combatManager.setVersionManager(versionManager);
            
            CombatAPIProvider.setInstance(new CombatAPIImpl(combatManager));
            
            getServer().getPluginManager().registerEvents(new CombatListener(combatManager), this);
             getServer().getPluginManager().registerEvents(new PlayerQuitListener(this, combatManager, customLogger), this);
            
            getCommand("combatapi").setExecutor(new CombatAPICommand(this));
            
            customLogger.info("CombatAPI has been enabled successfully!");
            getLogger().info("CombatAPI has been enabled!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable CombatAPI: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (customLogger != null) {
                customLogger.info("CombatAPI plugin is shutting down...");
            }
            
            if (combatManager != null) {
                combatManager.shutdown();
            }
            
            if (metricsManager != null) {
                metricsManager.shutdown();
            }
            
            if (configManager != null) {
                configManager.shutdown();
            }
            
            if (customLogger != null) {
                customLogger.shutdown();
            }
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static CombatAPIPlugin getInstance() {
        return instance;
    }
    
    public CombatManager getCombatManager() {
        return combatManager;
    }
    
    public Logger getCustomLogger() {
        return customLogger;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MetricsManager getMetricsManager() {
        return metricsManager;
    }
    
    public VersionManager getVersionManager() {
        return versionManager;
    }
}