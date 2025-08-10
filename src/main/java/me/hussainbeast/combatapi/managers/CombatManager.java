package me.hussainbeast.combatapi.managers;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.api.PlayerKilledEvent;
import me.hussainbeast.combatapi.util.ActionBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

public class CombatManager {
    
    private final CombatAPIPlugin plugin;
    private final Map<UUID, CombatData> combatPlayers;
    private int combatDuration;
    private boolean actionBarEnabled;
    private BukkitTask actionBarTask;
    private int actionBarUpdateFrequency;
    private int batchSize;
    
    public CombatManager(CombatAPIPlugin plugin) {
        this.plugin = plugin;
        this.combatPlayers = new ConcurrentHashMap<>();
        this.combatDuration = plugin.getConfig().getInt("combat-duration", 10);
        this.actionBarEnabled = plugin.getConfig().getBoolean("action-bar.enabled", true);
        this.actionBarUpdateFrequency = plugin.getConfig().getInt("action-bar.update-frequency", 20);
        this.batchSize = plugin.getConfig().getInt("action-bar.batch-size", 50);
        
        startActionBarTask();
    }
    
    private void startActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (actionBarEnabled) {
                    updateCombatActionBars();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, actionBarUpdateFrequency);
    }
    
    public void enterCombat(Player player, Player attacker) {
        UUID playerId = player.getUniqueId();
        
        CombatData existingData = combatPlayers.get(playerId);
        if (existingData != null && existingData.getTask() != null) {
            existingData.getTask().cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                leaveCombat(player);
            }
        }.runTaskLaterAsynchronously(plugin, combatDuration * 20L);
        
        CombatData combatData = new CombatData(attacker.getUniqueId(), task);
        combatPlayers.put(playerId, combatData);
        
        String enterMessage = plugin.getConfig().getString("messages.enter-combat", "&cYou are now in combat!");
        if (!enterMessage.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', enterMessage));
        }
    }
    
    public void leaveCombat(Player player) {
        UUID playerId = player.getUniqueId();
        CombatData combatData = combatPlayers.remove(playerId);
        
        if (combatData != null) {
            if (combatData.getTask() != null) {
                combatData.getTask().cancel();
            }
            if (player.isOnline()) {
                String leaveMessage = plugin.getConfig().getString("messages.leave-combat", "&aYou are no longer in combat!");
                if (!leaveMessage.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', leaveMessage));
                }
                
                if (actionBarEnabled) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ActionBarUtil.sendActionBar(player, "");
                    });
                }
            }
        }
    }
    
    public boolean isInCombat(Player player) {
        return combatPlayers.containsKey(player.getUniqueId());
    }
    
    public boolean isInCombat(UUID playerUUID) {
        return combatPlayers.containsKey(playerUUID);
    }
    
    public Player getLastAttacker(Player player) {
        CombatData data = combatPlayers.get(player.getUniqueId());
        if (data != null) {
            return Bukkit.getPlayer(data.getAttackerId());
        }
        return null;
    }
    
    public Player getLastAttacker(UUID playerUUID) {
        CombatData data = combatPlayers.get(playerUUID);
        if (data != null) {
            return Bukkit.getPlayer(data.getAttackerId());
        }
        return null;
    }
    
    public long getCombatTimeRemaining(Player player) {
        return getCombatTimeRemaining(player.getUniqueId());
    }
    
    public long getCombatTimeRemaining(UUID playerUUID) {
        CombatData data = combatPlayers.get(playerUUID);
        if (data != null) {
            return data.getTimeRemaining(combatDuration);
        }
        return 0;
    }
    
    public void handleCombatLog(Player player) {
        if (!isInCombat(player)) {
            return;
        }
        
        Player lastAttacker = getLastAttacker(player);
        leaveCombat(player);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerKilledEvent killEvent = new PlayerKilledEvent(player, lastAttacker, true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(killEvent);
            });
            
            if (lastAttacker != null && lastAttacker.isOnline()) {
                String killerMessage = plugin.getConfig().getString("messages.combat-log-killer", "&a{victim} &ccombat logged! You got the kill!");
                killerMessage = killerMessage.replace("{victim}", player.getName());
                lastAttacker.sendMessage(ChatColor.translateAlternateColorCodes('&', killerMessage));
            }
            
            String broadcastMessage = plugin.getConfig().getString("messages.combat-log-broadcast", "&c{victim} &4combat logged and was killed!");
            if (!broadcastMessage.isEmpty()) {
                String finalBroadcastMessage = broadcastMessage.replace("{victim}", player.getName());
                final String finalMessage = ChatColor.translateAlternateColorCodes('&', finalBroadcastMessage);
                
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(finalMessage);
                }
            }
        });
    }
    
    public void clearAllCombat() {
        for (CombatData combatData : combatPlayers.values()) {
            if (combatData.getTask() != null) {
                combatData.getTask().cancel();
            }
        }
        combatPlayers.clear();
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
    }
    
    public int getCombatDuration() {
        return combatDuration;
    }
    
    public void setCombatDuration(int seconds) {
        this.combatDuration = Math.max(1, seconds);
        plugin.getConfig().set("combat-duration", this.combatDuration);
        plugin.saveConfig();
    }
    
    public boolean isActionBarEnabled() {
        return actionBarEnabled;
    }
    
    public void setActionBarEnabled(boolean enabled) {
        this.actionBarEnabled = enabled;
        plugin.getConfig().set("action-bar.enabled", enabled);
        plugin.saveConfig();
        
        if (enabled) {
            startActionBarTask();
        } else if (actionBarTask != null) {
            actionBarTask.cancel();
            for (UUID playerId : combatPlayers.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    ActionBarUtil.sendActionBar(player, "");
                }
            }
        }
    }
    
    public Player getVictim(Player attacker) {
        UUID attackerUUID = attacker.getUniqueId();
        for (Map.Entry<UUID, CombatData> entry : combatPlayers.entrySet()) {
            if (entry.getValue().getAttackerId().equals(attackerUUID)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }
    
    public Player getVictim(UUID attackerUUID) {
        for (Map.Entry<UUID, CombatData> entry : combatPlayers.entrySet()) {
            if (entry.getValue().getAttackerId().equals(attackerUUID)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }
    
    public Set<Player> getAllPlayersInCombat() {
        Set<Player> players = new HashSet<>();
        for (UUID playerId : combatPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }
    
    public Map<Player, Player> getAllCombatPairs() {
        Map<Player, Player> pairs = new HashMap<>();
        for (Map.Entry<UUID, CombatData> entry : combatPlayers.entrySet()) {
            Player victim = Bukkit.getPlayer(entry.getKey());
            Player attacker = Bukkit.getPlayer(entry.getValue().getAttackerId());
            if (victim != null && victim.isOnline() && attacker != null && attacker.isOnline()) {
                pairs.put(victim, attacker);
            }
        }
        return pairs;
    }
    
    private void updateCombatActionBars() {
        if (combatPlayers.isEmpty()) {
            return;
        }
        
        List<UUID> playerIds = new ArrayList<>(combatPlayers.keySet());
        String actionBarFormat = plugin.getConfig().getString("action-bar.format", "&c&lCOMBAT &8» &f{time}s");
        
        for (int i = 0; i < playerIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, playerIds.size());
            List<UUID> batch = playerIds.subList(i, endIndex);
            
            final int batchIndex = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID playerId : batch) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        long timeRemaining = getCombatTimeRemaining(playerId);
                        if (timeRemaining > 0) {
                            String message = actionBarFormat.replace("{time}", String.valueOf(timeRemaining));
                            final String finalMessage = ChatColor.translateAlternateColorCodes('&', message);
                            ActionBarUtil.sendActionBar(player, finalMessage);
                        }
                    }
                }
            }, batchIndex / batchSize);
        }
    }
    
    private static class CombatData {
        private final UUID attackerId;
        private final BukkitTask task;
        private final long startTime;
        
        public CombatData(UUID attackerId, BukkitTask task) {
            this.attackerId = attackerId;
            this.task = task;
            this.startTime = System.currentTimeMillis();
        }
        
        public UUID getAttackerId() {
            return attackerId;
        }
        
        public BukkitTask getTask() {
            return task;
        }
        
        public long getTimeRemaining(int combatDuration) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = (combatDuration * 1000L) - elapsed;
            return Math.max(0, remaining / 1000L);
        }
    }
}