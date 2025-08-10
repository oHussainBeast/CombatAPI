package me.hussainbeast.combatapi.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerKilledEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player victim;
    private final Player killer;
    private final Event originalEvent;
    private final boolean combatLog;
    
    public PlayerKilledEvent(Player victim, Player killer) {
        this(victim, killer, null, false);
    }
    
    public PlayerKilledEvent(Player victim, Player killer, boolean combatLog) {
        this(victim, killer, null, combatLog);
    }
    
    public PlayerKilledEvent(Player victim, Player killer, Event originalEvent, boolean combatLog) {
        this.victim = victim;
        this.killer = killer;
        this.originalEvent = originalEvent;
        this.combatLog = combatLog;
    }
    
    public Player getVictim() {
        return victim;
    }
    
    public Player getKiller() {
        return killer;
    }
    
    public boolean hasKiller() {
        return killer != null;
    }
    
    public Event getOriginalEvent() {
        return originalEvent;
    }
    
    public boolean isCombatLog() {
        return combatLog;
    }
    
    public Player getAttacker() {
        return killer;
    }
    
    public boolean hasAttacker() {
        return hasKiller();
    }
    
    public String getKillReason() {
        return combatLog ? "Combat Log" : "PvP";
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}