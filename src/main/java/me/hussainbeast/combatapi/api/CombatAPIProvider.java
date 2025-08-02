package me.hussainbeast.combatapi.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class CombatAPIProvider {
    
    private static CombatAPI instance;
    
    public static CombatAPI getAPI() {
        if (instance == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CombatAPI");
            if (plugin == null || !plugin.isEnabled()) {
                throw new IllegalStateException("CombatAPI plugin is not loaded or enabled!");
            }
            throw new IllegalStateException("CombatAPI instance not initialized!");
        }
        return instance;
    }
    
    public static boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CombatAPI");
        return plugin != null && plugin.isEnabled() && instance != null;
    }
    
    public static void setInstance(CombatAPI api) {
        instance = api;
    }
    
    static void reset() {
        instance = null;
    }
}