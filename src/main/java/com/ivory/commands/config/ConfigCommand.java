package com.ivory.commands.config;

import com.ivory.commands.IvoryCommands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class ConfigCommand implements CommandExecutor, TabCompleter {

    private final IvoryCommands plugin;

    public ConfigCommand(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ivorycommands.config")) {
            sender.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("config")) {
            sender.sendMessage("§eUsage: /" + label + " config <reload|get|set>");
            return true;
        }

        // Shift args so that args[0] is reload|get|set
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        if (subArgs.length < 1) {
            sender.sendMessage("§eUsage: /" + label + " config <reload|get|set>");
            return true;
        }

        switch (subArgs[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getConfigManager().load();
                plugin.reload();
                sender.sendMessage("§aConfiguration reloaded.");
                return true;
            }

            case "get" -> {
                if (subArgs.length < 2) {
                    sender.sendMessage("§eUsage: /" + label + " config get <path>");
                    return true;
                }

                String path = subArgs[1];
                if (!plugin.getConfig().contains(path)) {
                    sender.sendMessage("§cUnknown config path: §e" + path);
                    return true;
                }

                Object current = plugin.getConfig().get(path);
                String value;
                if (current instanceof List<?> list) {
                    value = String.join(", ", list.stream().map(Object::toString).toList());
                } else {
                    value = String.valueOf(current);
                }

                sender.sendMessage("§e" + path + " §7= §f" + value);
                return true;
            }

            case "set" -> {
                if (subArgs.length < 3) {
                    sender.sendMessage("§eUsage: /" + label + " config set <path> <value>");
                    return true;
                }

                String path = subArgs[1];
                if (!plugin.getConfig().contains(path)) {
                    sender.sendMessage("§cUnknown config path: §e" + path);
                    return true;
                }

                String newValue = String.join(" ", Arrays.copyOfRange(subArgs, 2, subArgs.length));
                Object current = plugin.getConfig().get(path);

                try {
                    if (current instanceof Boolean) {
                        plugin.getConfig().set(path, Boolean.parseBoolean(newValue));
                    } else if (current instanceof Integer) {
                        plugin.getConfig().set(path, Integer.parseInt(newValue));
                    } else if (current instanceof Double) {
                        plugin.getConfig().set(path, Double.parseDouble(newValue));
                    } else if (current instanceof List<?>) {
                        List<String> newList = Arrays.stream(newValue.split(","))
                                                    .map(String::trim)
                                                    .toList();
                        plugin.getConfig().set(path, newList);
                    } else {
                        plugin.getConfig().set(path, newValue);
                    }

                    plugin.saveConfig();
                    plugin.getConfigManager().load(); // refresh cached values
                    plugin.reload();
                    sender.sendMessage("§aSet §e" + path + "§a to §f" + newValue);
                } catch (Exception e) {
                    sender.sendMessage("§cFailed to set config: " + e.getMessage());
                }

                return true;
            }
        }

        sender.sendMessage("§eUsage: /" + label + " config <reload|get|set>");
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("ivorycommands.config")) return Collections.emptyList();

        if (args.length == 1) {
            // First argument is "config"
            return Collections.singletonList("config").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            // Second argument is the actual subcommand
            return Arrays.asList("reload", "get", "set").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("config") &&
            (args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("set"))) {
            // Suggest config paths
            return plugin.getConfig().getKeys(true).stream()
                    .filter(k -> !plugin.getConfig().isConfigurationSection(k))
                    .filter(k -> k.startsWith(args[2]))
                    .toList();
        }

        return Collections.emptyList();
    }

}
