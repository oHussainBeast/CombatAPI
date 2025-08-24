package me.hussainbeast.combatapi.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CombatEnterEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final Player attacker;
    private final CombatReason reason;
    private int duration;
    private String customMessage;
    
    public enum CombatReason {
        PLAYER_DAMAGE,
        PROJECTILE_DAMAGE,
        INDIRECT_DAMAGE,
        CUSTOM
    }
    
    public CombatEnterEvent(Player player, Player attacker, CombatReason reason, int duration) {
        this.player = player;
        this.attacker = attacker;
        this.reason = reason;
        this.duration = duration;
        this.customMessage = null;
    }
    
    public CombatEnterEvent(Player player, Player attacker, CombatReason reason, int duration, String customMessage) {
        this.player = player;
        this.attacker = attacker;
        this.reason = reason;
        this.duration = duration;
        this.customMessage = customMessage;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Player getAttacker() {
        return attacker;
    }
    
    public CombatReason getReason() {
        return reason;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
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
        return "CombatEnterEvent{" +
                "player=" + (player != null ? player.getName() : "null") +
                ", attacker=" + (attacker != null ? attacker.getName() : "null") +
                ", reason=" + reason +
                ", duration=" + duration +
                ", cancelled=" + cancelled +
                '}';
    }
}