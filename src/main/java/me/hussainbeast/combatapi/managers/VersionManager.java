package me.hussainbeast.combatapi.managers;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class VersionManager {
    private final CombatAPIPlugin plugin;
    private final Logger logger;
    private final String serverVersion;
    private final boolean isPaper;
    private final boolean isSpigot;
    private final boolean isFolia;
    private final int majorVersion;
    private final int minorVersion;
    
    private Method sendActionBarMethod;
    private Method getHandleMethod;
    private Method sendPacketMethod;
    private Class<?> packetClass;
    private Class<?> chatComponentClass;
    
    public VersionManager(CombatAPIPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.serverVersion = Bukkit.getVersion();
        this.isPaper = checkPaper();
        this.isSpigot = checkSpigot();
        this.isFolia = checkFolia();
        
        String[] versionParts = extractVersion();
        this.majorVersion = Integer.parseInt(versionParts[0]);
        this.minorVersion = Integer.parseInt(versionParts[1]);
        
        initializeReflection();
        logger.info("Version Manager initialized for " + getServerType() + " " + majorVersion + "." + minorVersion);
    }
    
    private boolean checkPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }
    
    private boolean checkSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private String[] extractVersion() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        if (parts.length >= 2) {
            return new String[]{parts[1], parts.length > 2 ? parts[2] : "0"};
        }
        return new String[]{"1", "20"};
    }
    
    private void initializeReflection() {
        try {
            if (majorVersion >= 1 && minorVersion >= 12) {
                try {
                    sendActionBarMethod = Player.class.getMethod("sendActionBar", String.class);
                } catch (NoSuchMethodException e) {
                    logger.debug("Native sendActionBar method not found, using reflection");
                    initializeNMSReflection();
                }
            } else {
                initializeNMSReflection();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize reflection for version " + majorVersion + "." + minorVersion, e);
        }
    }
    
    private void initializeNMSReflection() {
        try {
            String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            
            if (majorVersion >= 1 && minorVersion >= 17) {
                packetClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutChat");
                chatComponentClass = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
            } else {
                packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");
                chatComponentClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
            }
            
            getHandleMethod = Player.class.getMethod("getHandle");
        } catch (Exception e) {
            logger.debug("NMS reflection initialization failed: " + e.getMessage());
        }
    }
    
    public void sendActionBar(Player player, String message) {
        try {
            if (sendActionBarMethod != null) {
                sendActionBarMethod.invoke(player, message);
            } else {
                sendActionBarNMS(player, message);
            }
        } catch (Exception e) {
            logger.debug("Failed to send action bar to " + player.getName() + ": " + e.getMessage());
            player.sendMessage(message);
        }
    }
    
    private void sendActionBarNMS(Player player, String message) {
        try {
            Object handle = getHandleMethod.invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            
            Object chatComponent;
            if (majorVersion >= 1 && minorVersion >= 16) {
                Method fromString = chatComponentClass.getMethod("a", String.class);
                chatComponent = fromString.invoke(null, "{\"text\":\"" + message + "\"}");
            } else {
                chatComponent = chatComponentClass.getMethod("a", String.class).invoke(null, message);
            }
            
            Object packet = packetClass.getConstructor(chatComponentClass, byte.class).newInstance(chatComponent, (byte) 2);
            sendPacketMethod = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".Packet"));
            sendPacketMethod.invoke(connection, packet);
        } catch (Exception e) {
            logger.debug("NMS action bar failed: " + e.getMessage());
        }
    }
    
    public BukkitTask scheduleTask(Runnable task, long delay) {
        if (isFolia) {
            try {
                Object scheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
                return (BukkitTask) scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (scheduledTask) -> task.run(), delay);
            } catch (Exception e) {
                logger.debug("Folia scheduler not available, falling back to Bukkit scheduler");
                return plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            return plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    public BukkitTask scheduleAsyncTask(Runnable task, long delay) {
        if (isFolia) {
            try {
                Object scheduler = plugin.getServer().getClass().getMethod("getAsyncScheduler").invoke(plugin.getServer());
                return (BukkitTask) scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, java.util.concurrent.TimeUnit.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (scheduledTask) -> task.run(), delay * 50, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.debug("Folia async scheduler not available, falling back to Bukkit scheduler");
                return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            }
        } else {
            return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }
    
    public CompletableFuture<Void> runAsync(Runnable task) {
        if (isPaper) {
            try {
                Object executor = plugin.getServer().getScheduler().getClass().getMethod("getMainThreadExecutor", org.bukkit.plugin.Plugin.class)
                    .invoke(plugin.getServer().getScheduler(), plugin);
                return CompletableFuture.runAsync(task, (java.util.concurrent.Executor) executor);
            } catch (Exception e) {
                logger.debug("Paper main thread executor not available, falling back to async scheduler");
            }
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    
    public boolean supportsAsyncEvents() {
        return isPaper && majorVersion >= 1 && minorVersion >= 13;
    }
    
    public boolean supportsComponentAPI() {
        return majorVersion >= 1 && minorVersion >= 16;
    }
    
    public boolean supportsPersistentData() {
        return majorVersion >= 1 && minorVersion >= 14;
    }
    
    public String getServerType() {
        if (isFolia) return "Folia";
        if (isPaper) return "Paper";
        if (isSpigot) return "Spigot";
        return "Bukkit";
    }
    
    public String getServerVersion() {
        return serverVersion;
    }
    
    public int getMajorVersion() {
        return majorVersion;
    }
    
    public int getMinorVersion() {
        return minorVersion;
    }
    
    public boolean isPaper() {
        return isPaper;
    }
    
    public boolean isSpigot() {
        return isSpigot;
    }
    
    public boolean isFolia() {
        return isFolia;
    }
    
    public void logVersionInfo() {
        logger.info("Server Type: " + getServerType());
        logger.info("Server Version: " + majorVersion + "." + minorVersion);
        logger.info("Async Events Support: " + supportsAsyncEvents());
        logger.info("Component API Support: " + supportsComponentAPI());
        logger.info("Persistent Data Support: " + supportsPersistentData());
    }
}