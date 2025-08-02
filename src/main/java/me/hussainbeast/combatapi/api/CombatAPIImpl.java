package me.hussainbeast.combatapi.api;

import me.hussainbeast.combatapi.managers.CombatManager;
import org.bukkit.entity.Player;
import java.util.UUID;

public class CombatAPIImpl implements CombatAPI {
    
    private final CombatManager combatManager;
    
    public CombatAPIImpl(CombatManager combatManager) {
        this.combatManager = combatManager;
    }
    
    @Override
    public void enterCombat(Player player, Player attacker) {
        combatManager.enterCombat(player, attacker);
    }
    
    @Override
    public void leaveCombat(Player player) {
        combatManager.leaveCombat(player);
    }
    
    @Override
    public boolean isInCombat(Player player) {
        return combatManager.isInCombat(player);
    }
    
    @Override
    public boolean isInCombat(UUID playerUUID) {
        return combatManager.isInCombat(playerUUID);
    }
    
    @Override
    public Player getLastAttacker(Player player) {
        return combatManager.getLastAttacker(player);
    }
    
    @Override
    public Player getLastAttacker(UUID playerUUID) {
        return combatManager.getLastAttacker(playerUUID);
    }
    
    @Override
    public void handleCombatLog(Player player) {
        combatManager.handleCombatLog(player);
    }
    
    @Override
    public int getCombatDuration() {
        return combatManager.getCombatDuration();
    }
    
    @Override
    public void clearAllCombat() {
        combatManager.clearAllCombat();
    }
    
    @Override
    public void clearCombat(Player player) {
        combatManager.leaveCombat(player);
    }
    
    @Override
    public long getCombatTimeRemaining(Player player) {
        return combatManager.getCombatTimeRemaining(player);
    }
    
    @Override
    public long getCombatTimeRemaining(UUID playerUUID) {
        return combatManager.getCombatTimeRemaining(playerUUID);
    }
    
    @Override
    public void setCombatDuration(int seconds) {
        combatManager.setCombatDuration(seconds);
    }
    
    @Override
    public void setActionBarEnabled(boolean enabled) {
        combatManager.setActionBarEnabled(enabled);
    }
    
    @Override
    public boolean isActionBarEnabled() {
        return combatManager.isActionBarEnabled();
    }
}