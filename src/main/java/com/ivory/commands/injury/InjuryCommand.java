package com.ivory.commands.injury;

import com.ivory.commands.IvoryCommands;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class InjuryCommand implements CommandExecutor {
    private final InjuryManager injuryManager;

    public InjuryCommand(IvoryCommands plugin, InjuryManager injuryManager) {
        this.injuryManager = injuryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check basic permission
        if (sender instanceof Player && !sender.hasPermission("ivorycommands.injury.use")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        // Handle no arguments - show help
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "help":
                return handleHelp(sender);
            case "list":
                return handleList(sender, args);
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            default:
                sender.sendMessage(getMessage("unknown-subcommand").replace("%subcommand%", args[0]));
                return true;
        }
    }

    private boolean handleHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("ivorycommands.injury.admin");
        
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━ " + ChatColor.YELLOW + "Injury System Help" + ChatColor.GOLD + " ━━━━━━━━━━");
        sender.sendMessage("");
        
        if (isAdmin) {
            sender.sendMessage(ChatColor.YELLOW + "Commands:");
            sender.sendMessage(ChatColor.GRAY + "  /injury help " + ChatColor.WHITE + "- Show this help message");
            sender.sendMessage(ChatColor.GRAY + "  /injury list [player] " + ChatColor.WHITE + "- List injuries");
            sender.sendMessage(ChatColor.GRAY + "  /injury add <injury> [player] [unremovable] " + ChatColor.WHITE + "- Add injury");
            sender.sendMessage(ChatColor.GRAY + "  /injury remove <injury> [player] " + ChatColor.WHITE + "- Remove injury");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Commands:");
            sender.sendMessage(ChatColor.GRAY + "  /injury help " + ChatColor.WHITE + "- Show this help message");
            sender.sendMessage(ChatColor.GRAY + "  /injury list " + ChatColor.WHITE + "- List your injuries");
            sender.sendMessage(ChatColor.GRAY + "  /injury add <injury> " + ChatColor.WHITE + "- Add an injury to yourself");
            sender.sendMessage(ChatColor.GRAY + "  /injury remove <injury> " + ChatColor.WHITE + "- Remove an injury");
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Available Injuries:");
        
        List<String> availableInjuries = injuryManager.getAvailableInjuries();
        if (availableInjuries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  No injuries configured");
        } else {
            for (String injuryName : availableInjuries) {
                String displayName = injuryManager.getInjuryDisplayName(injuryName);
                sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + displayName + ChatColor.DARK_GRAY + " (" + injuryName + ")");
            }
        }
        
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        Player target;
        
        // Determine target player
        if (args.length >= 2) {
            // Admin checking another player
            if (!sender.hasPermission("ivorycommands.injury.admin")) {
                sender.sendMessage(getMessage("no-permission-admin"));
                return true;
            }
            
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[1]));
                return true;
            }
        } else {
            // Player checking themselves
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("must-specify-player"));
                return true;
            }
            target = (Player) sender;
        }

        Map<String, InjuryManager.PlayerInjury> injuries = injuryManager.getPlayerInjuries(target.getUniqueId());
        
        if (injuries.isEmpty()) {
            if (target.equals(sender)) {
                sender.sendMessage(getMessage("no-injuries-self"));
            } else {
                sender.sendMessage(getMessage("no-injuries-other").replace("%player%", target.getName()));
            }
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━ " + ChatColor.YELLOW + 
                (target.equals(sender) ? "Your Injuries" : target.getName() + "'s Injuries") + 
                ChatColor.GOLD + " ━━━━━━━━━");
        
        boolean isAdmin = sender.hasPermission("ivorycommands.injury.admin");
        
        for (InjuryManager.PlayerInjury injury : injuries.values()) {
            String displayName = injuryManager.getInjuryDisplayName(injury.getName());
            
            // Build the base message
            TextComponent message = new TextComponent(ChatColor.GRAY + "  • " + ChatColor.WHITE);
            TextComponent injuryNameComponent = new TextComponent(displayName);
            message.addExtra(injuryNameComponent);
            message.addExtra(" ");
            
            // Build clickable tag
            TextComponent tag;
            if (injury.isStaffOnly()) {
                // Staff-only injuries - only admins can remove
                tag = new TextComponent(ChatColor.RED + "[Unremovable]");
                if (isAdmin) {
                    String command = target.equals(sender) 
                        ? "/injury remove " + injury.getName()
                        : "/injury remove " + injury.getName() + " " + target.getName();
                    tag.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                    tag.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new Text(ChatColor.YELLOW + "Click to remove")));
                }
            } else {
                // Removable injuries - anyone can remove their own, admins can remove others'
                tag = new TextComponent(ChatColor.GREEN + "[Removable]");
                boolean canRemove = target.equals(sender) || isAdmin;
                
                if (canRemove) {
                    String command = target.equals(sender) 
                        ? "/injury remove " + injury.getName()
                        : "/injury remove " + injury.getName() + " " + target.getName();
                    tag.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                    tag.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new Text(ChatColor.YELLOW + "Click to remove")));
                }
            }
            
            message.addExtra(tag);
            
            // Send the clickable message (only works for Player senders)
            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(message);
            } else {
                // Console fallback - send plain text
                sender.sendMessage(message.toLegacyText());
            }
        }
        
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean isAdmin = sender.hasPermission("ivorycommands.injury.admin");
            sender.sendMessage(getMessage(isAdmin ? "usage-add-admin" : "usage-add-player"));
            return true;
        }

        String injuryName = args[1].toLowerCase();
        
        // Validate injury exists
        if (!injuryManager.isValidInjury(injuryName)) {
            sender.sendMessage(getMessage("invalid-injury").replace("%injury%", injuryName));
            return true;
        }

        Player target;
        boolean staffOnly = false;

        // Determine target and staff-only flag
        if (args.length >= 3) {
            // Admin adding to another player
            if (!sender.hasPermission("ivorycommands.injury.admin")) {
                sender.sendMessage(getMessage("no-permission-admin"));
                return true;
            }
            
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[2]));
                return true;
            }
            
            // Check for staff-only flag
            if (args.length >= 4) {
                staffOnly = Boolean.parseBoolean(args[3]);
            }
        } else {
            // Player adding to themselves
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("must-specify-player"));
                return true;
            }
            target = (Player) sender;
        }

        // Add the injury
        boolean added = injuryManager.addInjury(target.getUniqueId(), injuryName, staffOnly);
        
        if (!added) {
            sender.sendMessage(getMessage("injury-already-exists")
                    .replace("%player%", target.getName())
                    .replace("%injury%", injuryManager.getInjuryDisplayName(injuryName)));
            return true;
        }

        String displayName = injuryManager.getInjuryDisplayName(injuryName);
        
        if (target.equals(sender)) {
            sender.sendMessage(getMessage("injury-added-self").replace("%injury%", displayName));
        } else {
            sender.sendMessage(getMessage("injury-added-other")
                    .replace("%player%", target.getName())
                    .replace("%injury%", displayName));
            target.sendMessage(getMessage("injury-received").replace("%injury%", displayName));
        }
        
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean isAdmin = sender.hasPermission("ivorycommands.injury.admin");
            sender.sendMessage(getMessage(isAdmin ? "usage-remove-admin" : "usage-remove-player"));
            return true;
        }

        String injuryName = args[1].toLowerCase();
        Player target;

        // Determine target player
        if (args.length >= 3) {
            // Admin removing from another player
            if (!sender.hasPermission("ivorycommands.injury.admin")) {
                sender.sendMessage(getMessage("no-permission-admin"));
                return true;
            }
            
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[2]));
                return true;
            }
        } else {
            // Player removing from themselves
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("must-specify-player"));
                return true;
            }
            target = (Player) sender;
        }

        // Check if injury exists
        if (!injuryManager.hasInjury(target.getUniqueId(), injuryName)) {
            sender.sendMessage(getMessage("injury-not-found")
                    .replace("%player%", target.getName())
                    .replace("%injury%", injuryManager.getInjuryDisplayName(injuryName)));
            return true;
        }

        // Check if injury is staff-only and sender is not admin
        Map<String, InjuryManager.PlayerInjury> injuries = injuryManager.getPlayerInjuries(target.getUniqueId());
        InjuryManager.PlayerInjury injury = injuries.get(injuryName);
        
        if (injury.isStaffOnly() && !sender.hasPermission("ivorycommands.injury.admin")) {
            sender.sendMessage(getMessage("injury-staff-only"));
            return true;
        }

        // Remove the injury
        injuryManager.removeInjury(target.getUniqueId(), injuryName);
        
        String displayName = injuryManager.getInjuryDisplayName(injuryName);
        
        if (target.equals(sender)) {
            sender.sendMessage(getMessage("injury-removed-self").replace("%injury%", displayName));
        } else {
            sender.sendMessage(getMessage("injury-removed-other")
                    .replace("%player%", target.getName())
                    .replace("%injury%", displayName));
            target.sendMessage(getMessage("injury-cleared").replace("%injury%", displayName));
        }
        
        return true;
    }

    private String getMessage(String key) {
        String message = injuryManager.getMessage(key, "Message not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
