package com.ivory.commands.name;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.ivory.commands.IvoryCommands;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CharacterNameManager {
    private final IvoryCommands plugin;
    private File characterNamesFile;
    private FileConfiguration characterNamesConfig;
    private final NameTagManager nameTagManager;
    private final TabAPI tabAPI;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public CharacterNameManager(IvoryCommands plugin) {
        this.plugin = plugin;
        this.nameTagManager = TabAPI.getInstance().getNameTagManager();
        this.tabAPI = TabAPI.getInstance();
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        this.init();
    }

    private void init() {
        // Create character names file
        createCharacterNamesFile();
        
        // Register /name command
        plugin.getCommand("name").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                return handleNameCommand(sender, args);
            }
        });
        plugin.getCommand("name").setTabCompleter(new NameTabCompleter());
    }

    private void createCharacterNamesFile() {
        characterNamesFile = new File(plugin.getDataFolder(), "character_names.yml");
        if (!characterNamesFile.exists()) {
            characterNamesFile.getParentFile().mkdirs();
            try {
                characterNamesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create character_names.yml file: " + e.getMessage());
            }
        }
        characterNamesConfig = YamlConfiguration.loadConfiguration(characterNamesFile);
    }

    private boolean handleNameCommand(CommandSender sender, String[] args) {
        // Check if sender has basic permission
        if (sender instanceof Player && !sender.hasPermission("ivorycommands.name.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use character names.");
            return true;
        }

        // Handle different argument lengths
        if (args.length == 0) {
            // Show usage
            sender.sendMessage(ChatColor.GRAY + "Usage:");
            sender.sendMessage(ChatColor.GRAY + "  /name <character name> - Set your character name");
            sender.sendMessage(ChatColor.GRAY + "  /name view <player> - View player's character name");
            
            if(!sender.hasPermission("ivorycommands.name.admin")){
                sender.sendMessage(ChatColor.GRAY + "  /name clear - Clear character name");
                return true;
            }

            sender.sendMessage(ChatColor.GRAY + "  /name set <player> <character name> - Set player's character name");
            sender.sendMessage(ChatColor.GRAY + "  /name clear [player] - Clear character name");
            
            return true;
        }

        // Handle subcommands
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "view":
                return handleViewCommand(sender, args);
            case "set":
                return handleSetCommand(sender, args);
            case "clear":
            case "remove":
            case "delete":
                return handleClearCommand(sender, args);
            default:
                // Default behavior: set own name (join all arguments as the character name)
                return handleSetOwnName(sender, args);
        }
    }

    private boolean handleViewCommand(CommandSender sender, String[] args) {

        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            String currentName = getCharacterName(player.getUniqueId());
            if (currentName != null) {
                sender.sendMessage(ChatColor.YELLOW + "Your current character name: " + ChatColor.WHITE + currentName);
            } else {
                sender.sendMessage(ChatColor.GRAY + "You don't have a character name set.");
            }
        }

        if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /name view <player>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return true;
        }

        String characterName = getCharacterName(targetPlayer.getUniqueId());
        if (characterName != null) {
            sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + "'s character name: " + ChatColor.WHITE + characterName);
        } else {
            sender.sendMessage(ChatColor.GRAY + targetPlayer.getName() + " doesn't have a character name set.");
        }
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        // Check admin permission
        if (sender instanceof Player && !sender.hasPermission("ivorycommands.name.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to set other players' character names.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /name set <player> <character name>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return true;
        }

        // Join remaining arguments as character name (supports spaces)
        StringBuilder characterNameBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) characterNameBuilder.append(" ");
            characterNameBuilder.append(args[i]);
        }
        String characterName = characterNameBuilder.toString();

        // Validate character name
        if (!isValidCharacterName(characterName, sender)) {
            int minLength = plugin.getConfigManager().getMinNameLength();
            int maxLength = plugin.getConfigManager().getMaxNameLength();
            boolean allowColors = plugin.getConfigManager().isAllowColors() || sender.hasPermission("ivorycommands.name.colors");

            String errorMsg = "Invalid character name. Must be " + minLength + "-" + maxLength + " characters";
            if (!allowColors) {
                errorMsg += " and cannot contain color codes (MiniMessage tags or &, §)";
            }
            errorMsg += ".";
            
            sender.sendMessage(ChatColor.RED + errorMsg);
            return true;
        }

        setCharacterName(targetPlayer.getUniqueId(), characterName);
        updateNameplate(targetPlayer);
        String saved = getCharacterName(targetPlayer.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Set " + targetPlayer.getName() + "'s character name to: " + ChatColor.WHITE + saved);
        targetPlayer.sendMessage(ChatColor.GREEN + "Your character name has been set to: " + ChatColor.WHITE + saved + ChatColor.GREEN + " by " + sender.getName());
        return true;
    }

    private boolean handleClearCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Clear own name
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player.");
                return true;
            }

            Player player = (Player) sender;
            String currentName = getCharacterName(player.getUniqueId());
            if (currentName == null) {
                sender.sendMessage(ChatColor.YELLOW + "You don't have a character name to clear.");
                return true;
            }
            
            removeCharacterName(player.getUniqueId());
            removeNameplate((Player) sender);
            sender.sendMessage(ChatColor.GREEN + "Your character name has been cleared.");
            return true;
        }

        if (args.length == 2) {
            // Clear another player's name (admin permission required)
            if (sender instanceof Player && !sender.hasPermission("ivorycommands.name.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to clear other players' character names.");
                return true;
            }

            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
                return true;
            }

            String currentName = getCharacterName(targetPlayer.getUniqueId());
            if (currentName == null) {
                sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " doesn't have a character name to clear.");
                return true;
            }

            removeCharacterName(targetPlayer.getUniqueId());
            removeNameplate(targetPlayer);
            sender.sendMessage(ChatColor.GREEN + "Cleared " + targetPlayer.getName() + "'s character name.");
            targetPlayer.sendMessage(ChatColor.GREEN + "Your character name has been cleared by " + sender.getName());
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /name clear [player]");
        return true;
    }

    private boolean handleSetOwnName(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Console must use: /name set <player> <character name>");
            return true;
        }

        Player player = (Player) sender;
        
        // Join all arguments as character name (supports spaces)
        StringBuilder characterNameBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) characterNameBuilder.append(" ");
            characterNameBuilder.append(args[i]);
        }
        String characterName = characterNameBuilder.toString();

        // Validate character name
        if (!isValidCharacterName(characterName, sender)) {

            int minLength = plugin.getConfigManager().getMinNameLength();
            int maxLength = plugin.getConfigManager().getMaxNameLength();
            boolean allowColors = plugin.getConfigManager().isAllowColors() || sender.hasPermission("ivorycommands.name.colors");
            
            String errorMsg = "Invalid character name. Must be " + minLength + "-" + maxLength + " characters";
            if (!allowColors) {
                errorMsg += " and cannot contain color codes (MiniMessage tags or &, §) or some other special characters";
            } else {
                errorMsg += " and cannot contain some special characters";
            }
            errorMsg += ".";
            
            sender.sendMessage(ChatColor.RED + errorMsg);
            return true;
        }

        setCharacterName(player.getUniqueId(), characterName);
        updateNameplate((Player) sender);
        sender.sendMessage(ChatColor.GREEN + "Your character name has been set to: " + ChatColor.WHITE + getCharacterName(player.getUniqueId()));
        return true;
    }

    private boolean isValidCharacterName(String name, CommandSender sender) {
        int minLength = plugin.getConfigManager().getMinNameLength();
        int maxLength = plugin.getConfigManager().getMaxNameLength();
        boolean allowColors = plugin.getConfigManager().isAllowColors() || (sender != null && sender.hasPermission("ivorycommands.name.colors"));

        // If colors are not allowed, reject names containing formatting markers
        if (!allowColors && containsFormatting(name)) {
            return false;
        }

        // Strip color codes and mini-message tags for validation and length checks
        String visible = stripFormatting(name);

        // Check length (colors/formatting not counted)
        if (visible.length() < minLength || visible.length() > maxLength) {
            return false;
        }

        // Disallow tabs
        if (visible.contains("\t")) {
            return false;
        }

        // Regex whitelist applied to visible characters (no color tags)
        // Note: keep pattern ASCII-safe to avoid source-encoding issues
        String pattern = "^[\\p{L}\\p{N} '.,!?_~\\-]+$";

        return visible.matches(pattern);
    }

    private boolean containsFormatting(String input) {
        if (input == null) return false;
        if (input.contains("<") && input.contains(">")) return true; // MiniMessage tags
        if (input.contains("&") || input.contains("§")) return true; // legacy
        if (input.matches("(?i).*&#[0-9A-F]{6}.*")) return true; // hex
        if (input.matches("(?i).*§x(§[0-9A-Fa-f]){6}.*")) return true; // legacy hex
        return false;
    }

    private String stripFormatting(String input) {
        if (input == null) return "";
        String s = input;
        // remove mini-message tags like <red>, <gradient:...>, <#hex>
        s = s.replaceAll("<[^>]+>", "");
        // remove legacy section and ampersand color/formatting codes
        s = s.replaceAll("(?i)§[0-9A-FK-OR]", "");
        s = s.replaceAll("(?i)&[0-9A-FK-OR]", "");
        // remove hex forms like &#rrggbb
        s = s.replaceAll("(?i)&#[0-9A-F]{6}", "");
        // remove legacy hex sequence §x§r§r... patterns
        s = s.replaceAll("(?i)§x(§[0-9A-Fa-f]){6}", "");
        return s.trim();
    }

    private String toMiniMessage(String input) {
        if (input == null) return null;
        try {
            Component comp;
            if (input.contains("<") && input.contains(">")) {
                comp = miniMessage.deserialize(input);
            } else {
                comp = legacySerializer.deserialize(input);
            }
            return miniMessage.serialize(comp);
        } catch (Exception e) {
            return input;
        }
    }

    public void setCharacterName(UUID playerUUID, String characterName) {
        // Always store names in MiniMessage canonical form for compatibility
        String mini = toMiniMessage(characterName);
        characterNamesConfig.set(playerUUID.toString(), mini);
        saveCharacterNamesFile();
    }

    public String getCharacterName(UUID playerUUID) {
        return characterNamesConfig.getString(playerUUID.toString());
    }

    public void removeCharacterName(UUID playerUUID) {
        characterNamesConfig.set(playerUUID.toString(), null);
        saveCharacterNamesFile();
    }

    private void saveCharacterNamesFile() {
        try {
            characterNamesConfig.save(characterNamesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save character_names.yml: " + e.getMessage());
        }
    }

    public void updateNameplate(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());
        if (tabPlayer == null) {
            return;
        }

        String characterName = this.getCharacterName(player.getUniqueId());
        if (characterName == null) {
            nameTagManager.setSuffix(tabPlayer, null);
            return;
        }

        if (plugin.getConfigManager().isShowNameInPlate()) {
            String separator = plugin.getConfigManager().getNameSeparator();
            String sepMini = toMiniMessage(separator);
            nameTagManager.setSuffix(tabPlayer, sepMini + characterName);
        } else {
            nameTagManager.setSuffix(tabPlayer, null);
        }
    }

    public void removeNameplate(Player player){
        if (player == null) {
            return;
        }

        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());
        if (tabPlayer == null) {
            return;
        }

        nameTagManager.setSuffix(tabPlayer, null);
    }
    
    public void refreshAllNameplates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNameplate(player);
        }
    }
    
    public void setupPlayerNameplate(Player player) {
        // Small delay to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateNameplate(player);
        }, 10L); // 0.5 second delay
    }

    public void reload() {
        characterNamesConfig = YamlConfiguration.loadConfiguration(characterNamesFile);
        plugin.getLogger().info("Updating nameplate format for all online players...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            refreshAllNameplates();
            plugin.getLogger().info("Nameplate format updated for " + Bukkit.getOnlinePlayers().size() + " players.");
        }, 5L);
    }
}