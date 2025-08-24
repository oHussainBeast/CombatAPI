package me.hussainbeast.combatapi.commands;

import me.hussainbeast.combatapi.CombatAPIPlugin;
import me.hussainbeast.combatapi.api.CombatAPIProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombatAPICommand implements CommandExecutor, TabCompleter {
    
    private final CombatAPIPlugin plugin;
    
    public CombatAPICommand(CombatAPIPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combatapi.admin")) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!")));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "duration":
                handleDuration(sender, args);
                break;
            case "actionbar":
                handleActionBar(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        sender.sendMessage(color("&eReloading configuration..."));
        
        plugin.getConfigManager().reloadConfigurationAsync().thenAccept(success -> {
            if (success) {
                sender.sendMessage(color("&aConfiguration reloaded successfully!"));
            } else {
                sender.sendMessage(color("&cConfiguration reloaded with some issues. Check console for details."));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(color("&cFailed to reload configuration: " + throwable.getMessage()));
            return null;
        });
    }
    
    private void handleInfo(CommandSender sender) {
        sender.sendMessage(color("&6&lCombatAPI Info:"));
        sender.sendMessage(color("&7Version: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(color("&7Author: &f" + plugin.getDescription().getAuthors().get(0)));
        sender.sendMessage(color("&7Combat Duration: &f" + plugin.getCombatManager().getCombatDuration() + "s"));
        sender.sendMessage(color("&7Action Bar: &f" + (plugin.getCombatManager().isActionBarEnabled() ? "&aEnabled" : "&cDisabled")));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean inCombat = CombatAPIProvider.getAPI().isInCombat(player);
            sender.sendMessage(color("&7Your Combat Status: &f" + (inCombat ? "&cIn Combat" : "&aNot in Combat")));
            if (inCombat) {
                long timeRemaining = CombatAPIProvider.getAPI().getCombatTimeRemaining(player);
                sender.sendMessage(color("&7Time Remaining: &f" + timeRemaining + "s"));
            }
        }
    }
    
    private void handleDuration(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /combatapi duration <seconds>"));
            return;
        }
        
        try {
            int duration = Integer.parseInt(args[1]);
            int minDuration = plugin.getConfig().getInt("settings.min-duration", 1);
            int maxDuration = plugin.getConfig().getInt("settings.max-duration", 300);
            
            if (duration < minDuration || duration > maxDuration) {
                sender.sendMessage(color(plugin.getConfig().getString("messages.invalid-duration", "&cInvalid duration! Please use a number between 1 and 300.")));
                return;
            }
            
            plugin.getCombatManager().setCombatDuration(duration);
            String message = plugin.getConfig().getString("messages.duration-set", "&aCombat duration set to {duration} seconds!");
            message = message.replace("{duration}", String.valueOf(duration));
            sender.sendMessage(color(message));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.invalid-duration", "&cInvalid duration! Please use a number between 1 and 300.")));
        }
    }
    
    private void handleActionBar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /combatapi actionbar <enable|disable>"));
            return;
        }
        
        boolean enable = args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("true");
        plugin.getCombatManager().setActionBarEnabled(enable);
        
        String message = enable ? 
            plugin.getConfig().getString("messages.actionbar-enabled", "&aAction bar enabled!") :
            plugin.getConfig().getString("messages.actionbar-disabled", "&cAction bar disabled!");
        sender.sendMessage(color(message));
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6&lCombatAPI Commands:"));
        sender.sendMessage(color("&7/combatapi reload &f- Reload the configuration"));
        sender.sendMessage(color("&7/combatapi info &f- Show plugin information"));
        sender.sendMessage(color("&7/combatapi duration <seconds> &f- Set combat duration"));
        sender.sendMessage(color("&7/combatapi actionbar <enable|disable> &f- Toggle action bar"));
    }
    
    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("combatapi.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "info", "duration", "actionbar"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("duration")) {
                completions.addAll(Arrays.asList("5", "10", "15", "30", "60"));
            } else if (args[0].equalsIgnoreCase("actionbar")) {
                completions.addAll(Arrays.asList("enable", "disable"));
            }
        }
        
        return completions;
    }
}