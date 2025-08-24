package me.hussainbeast.combatapi.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CombatLogEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final Player lastAttacker;
    private final long combatTimeRemaining;
    private boolean shouldKillPlayer;
    private boolean shouldDropItems;
    private String logMessage;
    
    public CombatLogEvent(Player player, Player lastAttacker, long combatTimeRemaining) {
        this.player = player;
        this.lastAttacker = lastAttacker;
        this.combatTimeRemaining = combatTimeRemaining;
        this.shouldKillPlayer = true;
        this.shouldDropItems = true;
        this.logMessage = null;
    }
    
    public CombatLogEvent(Player player, Player lastAttacker, long combatTimeRemaining, boolean shouldKillPlayer, boolean shouldDropItems) {
        this.player = player;
        this.lastAttacker = lastAttacker;
        this.combatTimeRemaining = combatTimeRemaining;
        this.shouldKillPlayer = shouldKillPlayer;
        this.shouldDropItems = shouldDropItems;
        this.logMessage = null;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Player getLastAttacker() {
        return lastAttacker;
    }
    
    public long getCombatTimeRemaining() {
        return combatTimeRemaining;
    }
    
    public boolean shouldKillPlayer() {
        return shouldKillPlayer;
    }
    
    public void setShouldKillPlayer(boolean shouldKillPlayer) {
        this.shouldKillPlayer = shouldKillPlayer;
    }
    
    public boolean shouldDropItems() {
        return shouldDropItems;
    }
    
    public void setShouldDropItems(boolean shouldDropItems) {
        this.shouldDropItems = shouldDropItems;
    }
    
    public String getLogMessage() {
        return logMessage;
    }
    
    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }
    
    public boolean hasCustomLogMessage() {
        return logMessage != null && !logMessage.trim().isEmpty();
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public String toString() {
        return "CombatLogEvent{" +
                "player=" + (player != null ? player.getName() : "null") +
                ", lastAttacker=" + (lastAttacker != null ? lastAttacker.getName() : "null") +
                ", combatTimeRemaining=" + combatTimeRemaining +
                ", shouldKillPlayer=" + shouldKillPlayer +
                ", shouldDropItems=" + shouldDropItems +
                ", cancelled=" + cancelled +
                '}';
    }
}