package me.hussainbeast.combatapi.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CombatLeaveEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final Player lastAttacker;
    private final LeaveReason reason;
    private String customMessage;
    private boolean clearActionBar;
    
    public enum LeaveReason {
        TIMEOUT,
        DEATH,
        DISCONNECT,
        ADMIN_COMMAND,
        CUSTOM
    }
    
    public CombatLeaveEvent(Player player, Player lastAttacker, LeaveReason reason) {
        this.player = player;
        this.lastAttacker = lastAttacker;
        this.reason = reason;
        this.customMessage = null;
        this.clearActionBar = true;
    }
    
    public CombatLeaveEvent(Player player, Player lastAttacker, LeaveReason reason, String customMessage) {
        this.player = player;
        this.lastAttacker = lastAttacker;
        this.reason = reason;
        this.customMessage = customMessage;
        this.clearActionBar = true;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Player getLastAttacker() {
        return lastAttacker;
    }
    
    public LeaveReason getReason() {
        return reason;
    }
    
    public String getCustomMessage() {
        return customMessage;
    }
    
    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }
    
    public boolean hasCustomMessage() {
        return customMessage != null && !customMessage.trim().isEmpty();
    }
    
    public boolean shouldClearActionBar() {
        return clearActionBar;
    }
    
    public void setClearActionBar(boolean clearActionBar) {
        this.clearActionBar = clearActionBar;
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
        return "CombatLeaveEvent{" +
                "player=" + (player != null ? player.getName() : "null") +
                ", lastAttacker=" + (lastAttacker != null ? lastAttacker.getName() : "null") +
                ", reason=" + reason +
                ", cancelled=" + cancelled +
                '}';
    }
}