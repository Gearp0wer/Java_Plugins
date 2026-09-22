package com.ivory.commands.placeholders;

import com.ivory.commands.IvoryCommands;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class IvoryPlaceholderExpansion extends PlaceholderExpansion {
    
    private final IvoryCommands plugin;
    
    public IvoryPlaceholderExpansion(IvoryCommands plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "ivory"; // This means placeholders will be %ivory_placeholder%
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    private String stripFormatting(String input) {
        if (input == null) {
            return "";
        }

        String result = input;

        // Remove MiniMessage tags like <red> or <gradient:...>
        result = result.replaceAll("<[^>]+>", "");

        // Remove legacy color codes like &a and §c
        result = result.replaceAll("(?i)&[0-9A-FK-OR]", "");
        result = result.replaceAll("(?i)§[0-9A-FK-OR]", "");

        // Remove hex forms like &#RRGGBB and §x§r§r... patterns
        result = result.replaceAll("(?i)&#[0-9A-F]{6}", "");
        result = result.replaceAll("(?i)§x(§[0-9A-Fa-f]){6}", "");

        return result;
    }

    private String toVentureChatCompatible(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            Component component;
            if (input.contains("<") && input.contains(">")) {
                component = MiniMessage.miniMessage().deserialize(input);
            } else {
                component = LegacyComponentSerializer.legacyAmpersand().deserialize(input);
            }
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            return input;
        }
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Handle different placeholder requests
        switch (params.toLowerCase()) {
            case "character_name":
            case "charactername":
            case "name":
                // Get the character name for the player
                if (plugin.getNameManager() != null) {
                    String characterName = plugin.getNameManager().getCharacterName(player.getUniqueId());
                    return characterName != null ? ChatColor.translateAlternateColorCodes('&', characterName) : player.getName(); // Fallback to player name if no character name set
                }
                return player.getName();
                
            case "character_name_or_empty":
            case "charactername_or_empty":
            case "name_or_empty":
                // Get character name or return empty string if not set
                if (plugin.getNameManager() != null) {
                    String characterName = plugin.getNameManager().getCharacterName(player.getUniqueId());
                    return characterName != null ? ChatColor.translateAlternateColorCodes('&', characterName) : "";
                }
                return "";
                
            case "has_character_name":
            case "has_charactername":
            case "has_name":
                // Return true/false if player has a character name set
                if (plugin.getNameManager() != null) {
                    String characterName = plugin.getNameManager().getCharacterName(player.getUniqueId());
                    return characterName != null ? "true" : "false";
                }
                return "false";
            
            case "character_name_plain":
            case "charactername_plain":
            case "name_plain":
                if (plugin.getNameManager() != null) {
                    String characterName = plugin.getNameManager().getCharacterName(player.getUniqueId());
                    if (characterName != null) {
                        return stripFormatting(characterName);
                    }
                }
                return player.getName();

            case "character_name_vc":
            case "charactername_vc":
            case "name_vc":
                if (plugin.getNameManager() != null) {
                    String characterName = plugin.getNameManager().getCharacterName(player.getUniqueId());
                    if (characterName != null) {
                        return toVentureChatCompatible(characterName);
                    }
                }
                return player.getName();

        }
        
        // We return null if an invalid placeholder (f.e. %ivory_invalidplaceholder%) was provided
        return null;
    }
}