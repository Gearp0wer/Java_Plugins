package com.ivory.commands.name;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NameTabCompleter implements TabCompleter {

    public NameTabCompleter() {}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ivorycommands.name.use")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands depending on permissions
            List<String> subcommands = new ArrayList<>();
            subcommands.add("view"); // everyone can view any name
            if (sender.hasPermission("ivorycommands.name.admin")) {
                subcommands.add("set");
                subcommands.add("clear");
            }
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
            return completions;
        }

        if (args.length == 2) {
            String firstArg = args[0].toLowerCase();

            // Suggest player names for view (everyone) and set/clear (admins only)
            if (firstArg.equals("view")
                || ((firstArg.equals("set") || firstArg.equals("clear")) && sender.hasPermission("ivorycommands.name.admin"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }

}
