package me.hussainbeast.combatapi.util;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {
    
    public enum LogLevel {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR"),
        FATAL(4, "FATAL");
        
        private final int level;
        private final String name;
        
        LogLevel(int level, String name) {
            this.level = level;
            this.name = name;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getName() {
            return name;
        }
    }
    
    private final CombatAPIPlugin plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final ConcurrentLinkedQueue<String> logQueue;
    private final AtomicBoolean isLogging;
    private LogLevel currentLogLevel;
    private boolean fileLoggingEnabled;
    private boolean consoleLoggingEnabled;
    
    public Logger(CombatAPIPlugin plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.isLogging = new AtomicBoolean(false);
        this.currentLogLevel = LogLevel.INFO;
        this.fileLoggingEnabled = plugin.getConfig().getBoolean("settings.file-logging", false);
        this.consoleLoggingEnabled = plugin.getConfig().getBoolean("settings.console-logging", true);
        
        String logLevelStr = plugin.getConfig().getString("settings.log-level", "INFO");
        try {
            this.currentLogLevel = LogLevel.valueOf(logLevelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.currentLogLevel = LogLevel.INFO;
            warn("Invalid log level in config: " + logLevelStr + ", defaulting to INFO");
        }
        
        this.logFile = new File(plugin.getDataFolder(), "combat-api.log");
        
        if (fileLoggingEnabled) {
            initializeLogFile();
        }
    }
    
    private void initializeLogFile() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to initialize log file: " + e.getMessage());
            fileLoggingEnabled = false;
        }
    }
    
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    public void fatal(String message) {
        log(LogLevel.FATAL, message, null);
    }
    
    public void fatal(String message, Throwable throwable) {
        log(LogLevel.FATAL, message, throwable);
    }
    
    private void log(LogLevel level, String message, Throwable throwable) {
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return;
        }
        
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level.getName(), message);
        
        if (throwable != null) {
            logMessage += "\n" + getStackTrace(throwable);
        }
        
        if (consoleLoggingEnabled) {
            logToConsole(level, message, throwable);
        }
        
        if (fileLoggingEnabled) {
            logQueue.offer(logMessage);
            processLogQueue();
        }
    }
    
    private void logToConsole(LogLevel level, String message, Throwable throwable) {
        switch (level) {
            case DEBUG:
            case INFO:
                plugin.getLogger().info("[" + level.getName() + "] " + message);
                break;
            case WARN:
                plugin.getLogger().warning(message);
                break;
            case ERROR:
            case FATAL:
                plugin.getLogger().severe(message);
                break;
        }
        
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    private void processLogQueue() {
        if (!isLogging.compareAndSet(false, true)) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String logMessage;
                while ((logMessage = logQueue.poll()) != null) {
                    writer.println(logMessage);
                }
                writer.flush();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
            } finally {
                isLogging.set(false);
            }
        });
    }
    
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
        }
        
        return sb.toString();
    }
    
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
        info("Log level changed to: " + level.getName());
    }
    
    public LogLevel getLogLevel() {
        return currentLogLevel;
    }
    
    public void setFileLoggingEnabled(boolean enabled) {
        this.fileLoggingEnabled = enabled;
        if (enabled) {
            initializeLogFile();
        }
    }
    
    public void setConsoleLoggingEnabled(boolean enabled) {
        this.consoleLoggingEnabled = enabled;
    }
    
    public void shutdown() {
        if (fileLoggingEnabled) {
            processLogQueue();
        }
    }
}