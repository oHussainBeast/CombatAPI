package me.hussainbeast.combatapi;

import me.hussainbeast.combatapi.api.CombatAPIProvider;
import me.hussainbeast.combatapi.api.CombatAPIImpl;
import me.hussainbeast.combatapi.managers.CombatManager;
import me.hussainbeast.combatapi.listeners.CombatListener;
import me.hussainbeast.combatapi.commands.CombatAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CombatAPIPlugin extends JavaPlugin {
    
    private static CombatAPIPlugin instance;
    private CombatManager combatManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.combatManager = new CombatManager(this);
        
        CombatAPIProvider.setInstance(new CombatAPIImpl(combatManager));
        
        getServer().getPluginManager().registerEvents(new CombatListener(combatManager), this);
        
        getCommand("combatapi").setExecutor(new CombatAPICommand(this));
        
        getLogger().info("CombatAPI has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.clearAllCombat();
        }
        getLogger().info("CombatAPI has been disabled!");
    }
    
    public static CombatAPIPlugin getInstance() {
        return instance;
    }
    
    public CombatManager getCombatManager() {
        return combatManager;
    }
}