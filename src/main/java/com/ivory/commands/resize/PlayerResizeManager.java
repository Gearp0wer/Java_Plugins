package com.ivory.commands.resize;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.config.ConfigManager;

import net.md_5.bungee.api.ChatColor;

public class PlayerResizeManager {

    private final IvoryCommands plugin;
    private final ConfigManager configManager; 

    public PlayerResizeManager(IvoryCommands plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.init();
    }

    public void init() {
        // Register /resize command
        plugin.getCommand("resize").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                // Permission check for self
                if (sender instanceof Player && !sender.hasPermission("ivorycommands.resizer.use")) {
                    sender.sendMessage(ChatColor.RED + "[IvoryResizer] You don't have permission.");
                    return true;
                }

                if (args.length == 0 || args.length > 2) {
                    if(!sender.hasPermission("ivorycommands.resizer.admin")){
                        sender.sendMessage(ChatColor.GRAY + "Usage: /resize <size>");
                        return true;
                    } 
                    sender.sendMessage(ChatColor.GRAY + "Usage: /resize <size> [player]");
                    return true;
                }

                Player target;

                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Console must specify a player.");
                        return true;
                    }
                    target = (Player) sender;
                } else {
                    if (sender instanceof Player && !sender.hasPermission("ivorycommands.resizer.admin")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to resize others.");
                        return true;
                    }
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
                        return true;
                    }
                }

                try {
                    double size = Double.parseDouble(args[0]);

                    if (!(sender.hasPermission("ivorycommands.resizer.bypass")) && (size < configManager.getResizeMin() || size > configManager.getResizeMax())) {
                        sender.sendMessage(ChatColor.RED + "Size must be between " + configManager.getResizeMin() + " and " + configManager.getResizeMax());
                        return true;
                    }

                    AttributeInstance scale = target.getAttribute(Attribute.SCALE);
                    if (scale != null) scale.setBaseValue(size);

                    if (target.equals(sender)) {
                        target.sendMessage(ChatColor.GREEN + "Your size is now " + size);
                    } else {
                        sender.sendMessage("Set " + target.getName() + "'s size to " + size);
                        target.sendMessage(ChatColor.GREEN + "Your size was set to " + size + " by " + sender.getName());
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid size.");
                }
                return true;
            }
        });
    }
}
