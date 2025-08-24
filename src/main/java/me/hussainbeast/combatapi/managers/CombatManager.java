package me.hussainbeast.combatapi.managers;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.api.PlayerKilledEvent;
import me.hussainbeast.combatapi.util.ActionBarUtil;
import me.hussainbeast.combatapi.util.Logger;
import me.hussainbeast.combatapi.util.ErrorHandler;
import me.hussainbeast.combatapi.util.ValidationUtil;
import me.hussainbeast.combatapi.util.AsyncAPI;
import me.hussainbeast.combatapi.managers.VersionManager;
import me.hussainbeast.combatapi.events.CombatEnterEvent;
import me.hussainbeast.combatapi.events.CombatLeaveEvent;
import me.hussainbeast.combatapi.events.CombatLogEvent;
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
    private final Logger logger;
    private final ErrorHandler errorHandler;
    private final AsyncAPI asyncAPI;
    private final Map<UUID, CombatData> combatPlayers;
    private MetricsManager metricsManager;
    private VersionManager versionManager;
    private int combatDuration;
    private boolean actionBarEnabled;
    private BukkitTask actionBarTask;
    private int actionBarUpdateFrequency;
    private int batchSize;
    
    public CombatManager(CombatAPIPlugin plugin, Logger logger, ErrorHandler errorHandler) {
        this.plugin = plugin;
        this.logger = logger;
        this.errorHandler = errorHandler;
        this.asyncAPI = new AsyncAPI(plugin, logger);
        this.combatPlayers = new ConcurrentHashMap<>();
        
        errorHandler.executeWithRecovery(
            () -> {
                this.combatDuration = plugin.getConfig().getInt("combat-duration", 10);
                this.actionBarEnabled = plugin.getConfig().getBoolean("action-bar.enabled", true);
                this.actionBarUpdateFrequency = plugin.getConfig().getInt("action-bar.update-frequency", 20);
                this.batchSize = plugin.getConfig().getInt("action-bar.batch-size", 50);
                
                logger.info("CombatManager initialized - Action Bar Enabled: " + actionBarEnabled);
                logger.debug("Combat Duration: " + combatDuration + "s, Update Frequency: " + actionBarUpdateFrequency + " ticks");
                
                startActionBarTask();
            },
            () -> {
                this.combatDuration = 10;
                this.actionBarEnabled = true;
                this.actionBarUpdateFrequency = 20;
                this.batchSize = 50;
                logger.warn("Failed to load configuration, using default values");
            },
            "CombatManager initialization"
        );
    }
    
    private void startActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        
        if (actionBarEnabled) {
            plugin.getLogger().info("Starting action bar task with frequency: " + actionBarUpdateFrequency + " ticks");
            actionBarTask = versionManager != null ?
                versionManager.scheduleAsyncTask(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                         updateCombatActionBars();
                     });
                }, actionBarUpdateFrequency) :
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            updateCombatActionBars();
                        });
                    }
                }.runTaskTimerAsynchronously(plugin, 0L, actionBarUpdateFrequency);
        } else {
            plugin.getLogger().info("Action bar is disabled, not starting task");
        }
    }
    
    public void enterCombat(Player player, Player attacker) {
        if (!errorHandler.isPlayerValid(player) || !errorHandler.isPlayerValid(attacker)) {
            return;
        }
        
        errorHandler.executeWithRecovery(() -> {
            ValidationUtil.validatePlayer(player);
            ValidationUtil.validatePlayer(attacker);
            
            if (player.getUniqueId().equals(attacker.getUniqueId())) {
                logger.warn("Player " + player.getName() + " attempted to enter combat with themselves");
                return;
            }
            
            CombatEnterEvent enterEvent = new CombatEnterEvent(player, attacker, CombatEnterEvent.CombatReason.PLAYER_DAMAGE, combatDuration);
            Bukkit.getPluginManager().callEvent(enterEvent);
            
            if (enterEvent.isCancelled()) {
                logger.debug("Combat enter event was cancelled for " + player.getName());
                return;
            }
            
            UUID playerId = player.getUniqueId();
            logger.debug("Entering combat: " + player.getName() + " vs " + attacker.getName());
            
            int finalDuration = enterEvent.getDuration();
            
            CombatData existingData = combatPlayers.get(playerId);
            if (existingData != null && existingData.getTask() != null) {
                existingData.getTask().cancel();
                logger.debug("Cancelled existing combat task for " + player.getName());
            }
            
            BukkitTask task = versionManager != null ? 
                versionManager.scheduleTask(() -> {
                    leaveCombat(player);
                }, finalDuration * 20L) :
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        leaveCombat(player);
                    }
                }.runTaskLaterAsynchronously(plugin, finalDuration * 20L);
            
            CombatData combatData = new CombatData(attacker.getUniqueId(), task);
            combatPlayers.put(playerId, combatData);
            
            if (metricsManager != null) {
                metricsManager.incrementCounter("combat_enters");
            }
            
            errorHandler.executeWithRecovery(
                 () -> {
                     String enterMessage;
                     if (enterEvent.hasCustomMessage()) {
                         enterMessage = enterEvent.getCustomMessage();
                     } else {
                         enterMessage = plugin.getConfig().getString("messages.enter-combat", "&cYou are now in combat!");
                     }
                     
                     ValidationUtil.validateString(enterMessage, "enterMessage");
                     
                     if (!enterMessage.trim().isEmpty()) {
                         player.sendMessage(ChatColor.translateAlternateColorCodes('&', enterMessage));
                     }
                 },
                 () -> {
                     player.sendMessage(ChatColor.RED + "You are now in combat!");
                     logger.warn("Failed to send custom enter combat message, used fallback");
                 },
                 "send enter combat message"
            ); 
            
            if (actionBarEnabled) {
                ActionBarUtil.sendActionBar(player, "");
            }
        }, () -> {
            logger.warn("Failed to process combat enter for " + player.getName());
        }, "enterCombat for " + player.getName());
    }
    
    public void leaveCombat(Player player) {
        if (!errorHandler.isPlayerValid(player)) {
            return;
        }
        
        errorHandler.executeWithRecovery(() -> {
            ValidationUtil.validatePlayer(player);
            
            UUID playerId = player.getUniqueId();
            CombatData combatData = combatPlayers.get(playerId);
            
            logger.debug("Leaving combat: " + player.getName());
            
            if (combatData != null) {
                Player lastAttacker = null;
                if (combatData.getAttackerId() != null) {
                    lastAttacker = Bukkit.getPlayer(combatData.getAttackerId());
                }
                
                CombatLeaveEvent leaveEvent = new CombatLeaveEvent(player, lastAttacker, CombatLeaveEvent.LeaveReason.TIMEOUT);
                Bukkit.getPluginManager().callEvent(leaveEvent);
                
                if (leaveEvent.isCancelled()) {
                    logger.debug("Combat leave event was cancelled for " + player.getName());
                    return;
                }
                
                combatPlayers.remove(playerId);
                
                if (combatData.getTask() != null) {
                    combatData.getTask().cancel();
                }
                
                if (metricsManager != null) {
                    metricsManager.incrementCounter("combat_leaves");
                }
                
                if (player.isOnline()) {
                    errorHandler.executeWithRecovery(
                        () -> {
                            String leaveMessage;
                            if (leaveEvent.hasCustomMessage()) {
                                leaveMessage = leaveEvent.getCustomMessage();
                            } else {
                                leaveMessage = plugin.getConfig().getString("messages.leave-combat", "&aYou are no longer in combat!");
                            }
                            
                            ValidationUtil.validateString(leaveMessage, "leaveMessage");
                            
                            if (!leaveMessage.trim().isEmpty()) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', leaveMessage));
                            }
                        },
                        () -> {
                            player.sendMessage(ChatColor.GREEN + "You are no longer in combat!");
                            logger.warn("Failed to send custom leave combat message, used fallback");
                        },
                        "send leave combat message"
                    );
                    
                    if (actionBarEnabled) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            ActionBarUtil.sendActionBar(player, "");
                        });
                    }
                }
            }
        }, () -> {
            logger.warn("Failed to process combat leave for " + player.getName());
        }, "leaveCombat for " + player.getName());
    }

    
    public boolean isInCombat(Player player) {
        try {
            ValidationUtil.validatePlayer(player);
            return combatPlayers.containsKey(player.getUniqueId());
        } catch (ValidationUtil.ValidationException e) {
            logger.warn("Invalid player in isInCombat check: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isInCombat(UUID playerUUID) {
        try {
            ValidationUtil.validatePlayerUUID(playerUUID);
            return combatPlayers.containsKey(playerUUID);
        } catch (ValidationUtil.ValidationException e) {
            logger.warn("Invalid UUID in isInCombat check: " + e.getMessage());
            return false;
        }
    }
    
    public Player getLastAttacker(Player player) {
        try {
            ValidationUtil.validatePlayer(player);
            
            CombatData data = combatPlayers.get(player.getUniqueId());
            if (data != null) {
                Player attacker = Bukkit.getPlayer(data.getAttackerId());
                if (attacker != null && attacker.isOnline()) {
                    return attacker;
                }
                logger.debug("Attacker for " + player.getName() + " is no longer online");
            }
            return null;
        } catch (ValidationUtil.ValidationException e) {
            logger.warn("Invalid player in getLastAttacker: " + e.getMessage());
            return null;
        }
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
        
        if (plugin.isEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (actionBarEnabled) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ActionBarUtil.sendActionBar(player, "");
                    });
                }
            }
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
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        
        if (enabled) {
            startActionBarTask();
        } else {
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
    
    public Set<Player> getCombatPlayers() {
        return getAllPlayersInCombat();
    }
    
    public AsyncAPI getAsyncAPI() {
        return asyncAPI;
    }
    
    public void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
    
    public void setVersionManager(VersionManager versionManager) {
        this.versionManager = versionManager;
    }
    
    public void shutdown() {
        logger.info("Shutting down CombatManager...");
        
        for (CombatData combatData : combatPlayers.values()) {
            if (combatData.getTask() != null) {
                combatData.getTask().cancel();
            }
        }
        combatPlayers.clear();
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        
        if (asyncAPI != null) {
            asyncAPI.shutdown();
        }
        
        logger.info("CombatManager shutdown complete");
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
                            if (versionManager != null) {
                                versionManager.sendActionBar(player, finalMessage);
                            } else {
                                ActionBarUtil.sendActionBar(player, finalMessage);
                            }
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