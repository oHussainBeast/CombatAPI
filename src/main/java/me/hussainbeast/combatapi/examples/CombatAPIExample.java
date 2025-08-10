package me.hussainbeast.combatapi.examples;

import me.hussainbeast.combatapi.api.CombatAPI;
import me.hussainbeast.combatapi.api.CombatAPIProvider;
import me.hussainbeast.combatapi.api.PlayerKilledEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;

public class CombatAPIExample extends JavaPlugin implements Listener {
    
    private CombatAPI combatAPI;
    
    @Override
    public void onEnable() {
        if (!CombatAPIProvider.isAvailable()) {
            getLogger().severe("CombatAPI is not available! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        combatAPI = CombatAPIProvider.getAPI();
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("CombatAPI Example enabled!");
    }
    
    @EventHandler
    public void onPlayerKilled(PlayerKilledEvent event) {
        Player victim = event.getVictim();
        Player attacker = event.getAttacker();
        
        if (event.isCombatLog()) {
            getLogger().info(victim.getName() + " was killed by combat logging!");
            if (attacker != null) {
                getLogger().info("Last attacker was: " + attacker.getName());
            }
        } else {
            getLogger().info(victim.getName() + " was killed by " + 
                (attacker != null ? attacker.getName() : "unknown"));
        }
    }
    
    public void exampleUsage() {
        Set<Player> playersInCombat = combatAPI.getAllPlayersInCombat();
        getLogger().info("Players currently in combat: " + playersInCombat.size());
        
        Map<Player, Player> combatPairs = combatAPI.getAllCombatPairs();
        for (Map.Entry<Player, Player> entry : combatPairs.entrySet()) {
            Player victim = entry.getKey();
            Player attacker = entry.getValue();
            getLogger().info(victim.getName() + " is being attacked by " + attacker.getName());
        }
        
        for (Player player : playersInCombat) {
            if (combatAPI.isInCombat(player)) {
                Player lastAttacker = combatAPI.getAttacker(player);
                long timeRemaining = combatAPI.getCombatTimeRemaining(player);
                
                if (lastAttacker != null) {
                    getLogger().info(player.getName() + " has " + timeRemaining + 
                        " seconds left in combat with " + lastAttacker.getName());
                }
            }
        }
    }
}