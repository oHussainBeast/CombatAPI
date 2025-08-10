package me.hussainbeast.combatapi.api;

import org.bukkit.entity.Player;
import java.util.UUID;

public interface CombatAPI {
    
    void enterCombat(Player player, Player attacker);
    
    void leaveCombat(Player player);
    
    boolean isInCombat(Player player);
    
    boolean isInCombat(UUID playerUUID);
    
    Player getLastAttacker(Player player);
    
    Player getLastAttacker(UUID playerUUID);
    
    void handleCombatLog(Player player);
    
    int getCombatDuration();
    
    void clearAllCombat();
    
    void clearCombat(Player player);
    
    long getCombatTimeRemaining(Player player);
    
    long getCombatTimeRemaining(UUID playerUUID);
    
    void setCombatDuration(int seconds);
    
    void setActionBarEnabled(boolean enabled);
    
    boolean isActionBarEnabled();
    
    Player getAttacker(Player victim);
    
    Player getAttacker(UUID victimUUID);
    
    Player getVictim(Player attacker);
    
    Player getVictim(UUID attackerUUID);
    
    java.util.Set<Player> getAllPlayersInCombat();
    
    java.util.Map<Player, Player> getAllCombatPairs();
}