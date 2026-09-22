package com.ivory.commands.injury;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InjuryTabCompleter implements TabCompleter {
    private final InjuryManager injuryManager;

    public InjuryTabCompleter(InjuryManager injuryManager) {
        this.injuryManager = injuryManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("ivorycommands.injury.admin");

        if (args.length == 1) {
            // First argument: subcommands
            completions.addAll(Arrays.asList("help", "list", "add", "remove"));
            return filterMatching(completions, args[0]);
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "add":
                if (args.length == 2) {
                    // Second argument for add: injury names
                    completions.addAll(injuryManager.getAvailableInjuries());
                } else if (args.length == 3 && isAdmin) {
                    // Third argument for add (admin only): player names
                    completions.addAll(getOnlinePlayerNames());
                } else if (args.length == 4 && isAdmin) {
                    // Fourth argument for add (admin only): staff-only boolean
                    completions.addAll(Arrays.asList("true", "false"));
                }
                break;

            case "remove":
                if (args.length == 2) {
                    // Second argument for remove: injury names
                    if (sender instanceof Player && !isAdmin) {
                        // Non-admin: show their own removable injuries
                        Player player = (Player) sender;
                        completions.addAll(
                            injuryManager.getPlayerInjuries(player.getUniqueId()).values().stream()
                                .filter(injury -> !injury.isStaffOnly())
                                .map(InjuryManager.PlayerInjury::getName)
                                .collect(Collectors.toList())
                        );
                    } else {
                        // Admin: show all injury types
                        completions.addAll(injuryManager.getAvailableInjuries());
                    }
                } else if (args.length == 3 && isAdmin) {
                    // Third argument for remove (admin only): player names
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case "list":
                if (args.length == 2 && isAdmin) {
                    // Second argument for list (admin only): player names
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case "help":
                // No additional arguments for help
                break;
        }

        return filterMatching(completions, args[args.length - 1]);
    }

    /**
     * Get list of online player names
     */
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Filter completions that match the current input
     */
    private List<String> filterMatching(List<String> completions, String input) {
        String lowerInput = input.toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerInput))
                .sorted()
                .collect(Collectors.toList());
    }
}
