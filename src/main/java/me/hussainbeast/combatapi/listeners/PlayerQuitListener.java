package me.hussainbeast.combatapi.listeners;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.events.CombatLogEvent;
import me.hussainbeast.combatapi.managers.CombatManager;
import me.hussainbeast.combatapi.util.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {
    
    private final CombatAPIPlugin plugin;
    private final CombatManager combatManager;
    private final Logger logger;
    
    public PlayerQuitListener(CombatAPIPlugin plugin, CombatManager combatManager, Logger logger) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.logger = logger;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (combatManager.isInCombat(player)) {
            logger.debug("Player " + player.getName() + " disconnected during combat");
            
            Player lastAttacker = combatManager.getLastAttacker(player);
            long remainingTime = combatManager.getCombatTimeRemaining(player);
            
            CombatLogEvent combatLogEvent = new CombatLogEvent(player, lastAttacker, remainingTime);
                Bukkit.getPluginManager().callEvent(combatLogEvent);
                
                if (!combatLogEvent.isCancelled()) {
                    if (plugin.getMetricsManager() != null) {
                        plugin.getMetricsManager().incrementCounter("combat_logs");
                    }
                    
                    if (combatLogEvent.shouldKillPlayer()) {
                        player.setHealth(0.0);
                        logger.info("Player " + player.getName() + " was killed for combat logging");
                    }
                    
                    if (combatLogEvent.shouldDropItems()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getContents()[0]);
                        logger.debug("Dropped items for combat logger " + player.getName());
                    }
                    
                    if (combatLogEvent.getLogMessage() != null && lastAttacker != null && lastAttacker.isOnline()) {
                        lastAttacker.sendMessage(combatLogEvent.getLogMessage());
                    }
                } else {
                    logger.debug("Combat log event was cancelled for " + player.getName());
                }
                
                combatManager.leaveCombat(player);
        }
    }
}