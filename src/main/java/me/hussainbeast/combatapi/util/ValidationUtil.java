package me.hussainbeast.combatapi.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.regex.Pattern;

public class ValidationUtil {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 3600;
    private static final int MAX_STRING_LENGTH = 256;
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static void validateNotNull(Object obj, String paramName) {
        if (obj == null) {
            throw new ValidationException("Parameter '" + paramName + "' cannot be null");
        }
    }
    
    public static void validatePlayer(Player player) {
        validateNotNull(player, "player");
        
        if (!player.isOnline()) {
            throw new ValidationException("Player '" + player.getName() + "' is not online");
        }
        
        if (!player.isValid()) {
            throw new ValidationException("Player '" + player.getName() + "' is not valid");
        }
    }
    
    public static void validatePlayerUUID(UUID uuid) {
        validateNotNull(uuid, "uuid");
        
        String uuidString = uuid.toString();
        if (!UUID_PATTERN.matcher(uuidString).matches()) {
            throw new ValidationException("Invalid UUID format: " + uuidString);
        }
    }
    
    public static void validateDuration(int duration) {
        if (duration < MIN_DURATION) {
            throw new ValidationException("Duration must be at least " + MIN_DURATION + " seconds, got: " + duration);
        }
        
        if (duration > MAX_DURATION) {
            throw new ValidationException("Duration cannot exceed " + MAX_DURATION + " seconds, got: " + duration);
        }
    }
    
    public static void validateString(String str, String paramName) {
        validateNotNull(str, paramName);
        
        if (str.trim().isEmpty()) {
            throw new ValidationException("Parameter '" + paramName + "' cannot be empty");
        }
        
        if (str.length() > MAX_STRING_LENGTH) {
            throw new ValidationException("Parameter '" + paramName + "' exceeds maximum length of " + MAX_STRING_LENGTH + " characters");
        }
    }
    
    public static void validatePositiveNumber(int number, String paramName) {
        if (number <= 0) {
            throw new ValidationException("Parameter '" + paramName + "' must be positive, got: " + number);
        }
    }
    
    public static void validateNonNegativeNumber(int number, String paramName) {
        if (number < 0) {
            throw new ValidationException("Parameter '" + paramName + "' cannot be negative, got: " + number);
        }
    }
    
    public static void validateRange(int value, int min, int max, String paramName) {
        if (value < min || value > max) {
            throw new ValidationException("Parameter '" + paramName + "' must be between " + min + " and " + max + ", got: " + value);
        }
    }
    
    public static void validatePlugin(Plugin plugin) {
        validateNotNull(plugin, "plugin");
        
        if (!plugin.isEnabled()) {
            throw new ValidationException("Plugin '" + plugin.getName() + "' is not enabled");
        }
    }
    
    public static void validatePlayerName(String playerName) {
        validateString(playerName, "playerName");
        
        if (playerName.length() < 3 || playerName.length() > 16) {
            throw new ValidationException("Player name must be between 3 and 16 characters, got: " + playerName.length());
        }
        
        if (!playerName.matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Player name contains invalid characters: " + playerName);
        }
    }
    
    public static void validateConfigPath(String path) {
        validateString(path, "configPath");
        
        if (!path.matches("^[a-zA-Z0-9._-]+$")) {
            throw new ValidationException("Invalid configuration path: " + path);
        }
    }
    
    public static boolean isValidUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return false;
        }
        
        return UUID_PATTERN.matcher(uuidString).matches();
    }
    
    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        
        return playerName.length() >= 3 && 
               playerName.length() <= 16 && 
               playerName.matches("^[a-zA-Z0-9_]+$");
    }
    
    public static boolean isValidDuration(int duration) {
        return duration >= MIN_DURATION && duration <= MAX_DURATION;
    }
    
    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        
        return input.trim().replaceAll("[\\r\\n\\t]", "").substring(0, Math.min(input.length(), MAX_STRING_LENGTH));
    }
    
    public static int clampDuration(int duration) {
        return Math.max(MIN_DURATION, Math.min(MAX_DURATION, duration));
    }
    
    public static void validateBatchOperation(int batchSize, int maxBatchSize) {
        validatePositiveNumber(batchSize, "batchSize");
        
        if (batchSize > maxBatchSize) {
            throw new ValidationException("Batch size cannot exceed " + maxBatchSize + ", got: " + batchSize);
        }
    }
}