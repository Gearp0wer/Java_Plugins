package com.ivory.commands.injury;

import com.ivory.commands.IvoryCommands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InjuryManager {
    private final IvoryCommands plugin;
    private File injuriesFile;
    private FileConfiguration injuriesConfig;
    private File injuryConfigFile;
    private FileConfiguration injuryConfig;
    private final Map<UUID, Map<String, PlayerInjury>> playerInjuries; // UUID -> (InjuryName -> PlayerInjury)
    private final Map<String, String> cachedMessages; // Cached messages from config
    private final Map<String, InjuryType> cachedInjuryTypes; // Cached injury type definitions

    public InjuryManager(IvoryCommands plugin) {
        this.plugin = plugin;
        this.playerInjuries = new HashMap<>();
        this.cachedMessages = new HashMap<>();
        this.cachedInjuryTypes = new HashMap<>();
        init();
    }

    private void init() {
        createInjuryConfigFile();
        createInjuriesFile();
        loadInjuryTypes();
        loadMessages();
        loadInjuries();
        startEffectTask();
    }

    private void createInjuryConfigFile() {
        injuryConfigFile = new File(plugin.getDataFolder(), "injury_config.yml");
        if (!injuryConfigFile.exists()) {
            plugin.saveResource("injury_config.yml", false);
        }
        injuryConfig = YamlConfiguration.loadConfiguration(injuryConfigFile);
    }

    /**
     * Load and cache all injury type definitions from the config
     */
    private void loadInjuryTypes() {
        cachedInjuryTypes.clear();
        
        ConfigurationSection typesSection = injuryConfig.getConfigurationSection("types");
        if (typesSection == null) {
            plugin.getLogger().warning("No injury types found in config!");
            return;
        }
        
        for (String injuryName : typesSection.getKeys(false)) {
            String displayName = injuryConfig.getString("types." + injuryName + ".display-name", injuryName);
            List<String> effects = injuryConfig.getStringList("types." + injuryName + ".effects");
            
            cachedInjuryTypes.put(injuryName, new InjuryType(injuryName, displayName, effects));
        }
        
        plugin.getLogger().info("Loaded " + cachedInjuryTypes.size() + " injury types");
    }

    /**
     * Load and cache all messages from the config
     */
    private void loadMessages() {
        cachedMessages.clear();
        
        ConfigurationSection messagesSection = injuryConfig.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                String message = injuryConfig.getString("messages." + key, "");
                cachedMessages.put(key, message);
            }
        }
        
        plugin.getLogger().info("Loaded " + cachedMessages.size() + " injury messages");
    }

    private void createInjuriesFile() {
        injuriesFile = new File(plugin.getDataFolder(), "injuries.yml");
        if (!injuriesFile.exists()) {
            injuriesFile.getParentFile().mkdirs();
            try {
                injuriesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create injuries.yml file: " + e.getMessage());
            }
        }
        injuriesConfig = YamlConfiguration.loadConfiguration(injuriesFile);
    }

    private void loadInjuries() {
        playerInjuries.clear();
        
        if (injuriesConfig.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidString : injuriesConfig.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Map<String, PlayerInjury> injuries = new HashMap<>();
            
            ConfigurationSection playerSection = injuriesConfig.getConfigurationSection("players." + uuidString);
            if (playerSection != null) {
                for (String injuryName : playerSection.getKeys(false)) {
                    boolean staffOnly = playerSection.getBoolean(injuryName + ".staff-only", false);
                    injuries.put(injuryName, new PlayerInjury(injuryName, staffOnly));
                }
            }
            
            playerInjuries.put(uuid, injuries);
        }
        
        plugin.getLogger().info("Loaded injuries for " + playerInjuries.size() + " players");
    }

    private void saveInjuries() {
        injuriesConfig.set("players", null); // Clear existing data
        
        for (Map.Entry<UUID, Map<String, PlayerInjury>> entry : playerInjuries.entrySet()) {
            String uuidString = entry.getKey().toString();
            
            for (Map.Entry<String, PlayerInjury> injuryEntry : entry.getValue().entrySet()) {
                String injuryName = injuryEntry.getKey();
                PlayerInjury injury = injuryEntry.getValue();
                
                injuriesConfig.set("players." + uuidString + "." + injuryName + ".staff-only", injury.isStaffOnly());
            }
        }
        
        try {
            injuriesConfig.save(injuriesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save injuries.yml: " + e.getMessage());
        }
    }

    public void reload() {
        injuryConfig = YamlConfiguration.loadConfiguration(injuryConfigFile);
        loadInjuryTypes();
        loadMessages();
        loadInjuries();
        plugin.getLogger().info("Reloaded injury system");
    }

    /**
     * Add an injury to a player
     * @return true if added successfully, false if already exists
     */
    public boolean addInjury(UUID playerUuid, String injuryName, boolean staffOnly) {
        if (!isValidInjury(injuryName)) {
            return false;
        }
        
        Map<String, PlayerInjury> injuries = playerInjuries.computeIfAbsent(playerUuid, k -> new HashMap<>());
        
        if (injuries.containsKey(injuryName)) {
            return false; // Already has this injury
        }
        
        injuries.put(injuryName, new PlayerInjury(injuryName, staffOnly));
        saveInjuries();
        
        // Apply effects immediately if player is online
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            applyInjuryEffects(player);
        }
        
        return true;
    }

    /**
     * Remove an injury from a player
     * @return true if removed, false if didn't exist
     */
    public boolean removeInjury(UUID playerUuid, String injuryName) {
        Map<String, PlayerInjury> injuries = playerInjuries.get(playerUuid);
        if (injuries == null || !injuries.containsKey(injuryName)) {
            return false;
        }
        
        // If player is online, expire the injury's effects immediately (1 second duration)
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            expireInjuryEffects(player, injuryName);
        }
        
        injuries.remove(injuryName);
        
        // Remove from map if no injuries left
        if (injuries.isEmpty()) {
            playerInjuries.remove(playerUuid);
        }
        
        saveInjuries();
        
        return true;
    }

    /**
     * Check if a player has a specific injury
     */
    public boolean hasInjury(UUID playerUuid, String injuryName) {
        Map<String, PlayerInjury> injuries = playerInjuries.get(playerUuid);
        return injuries != null && injuries.containsKey(injuryName);
    }

    /**
     * Get all injuries for a player
     */
    public Map<String, PlayerInjury> getPlayerInjuries(UUID playerUuid) {
        return playerInjuries.getOrDefault(playerUuid, new HashMap<>());
    }

    /**
     * Check if an injury name is valid (exists in cached types)
     */
    public boolean isValidInjury(String injuryName) {
        return cachedInjuryTypes.containsKey(injuryName);
    }

    /**
     * Get list of all available injury names
     */
    public List<String> getAvailableInjuries() {
        return new ArrayList<>(cachedInjuryTypes.keySet());
    }

    /**
     * Get injury display name with color codes translated
     */
    public String getInjuryDisplayName(String injuryName) {
        InjuryType type = cachedInjuryTypes.get(injuryName);
        String displayName = type != null ? type.getDisplayName() : injuryName;
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    /**
     * Get a cached message from the injury config
     */
    public String getMessage(String key, String defaultValue) {
        return cachedMessages.getOrDefault(key, defaultValue);
    }

    /**
     * Remove all effects for a specific injury from a player
     */
    private void expireInjuryEffects(Player player, String injuryName) {
        InjuryType injuryType = cachedInjuryTypes.get(injuryName);
        if (injuryType == null) {
            return;
        }

        for (String effectString : injuryType.getEffects()) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    @SuppressWarnings("deprecation")
                    PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                    
                    if (effectType != null) {
                        // Remove the effect immediately
                        player.removePotionEffect(effectType);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid effect format for injury " + injuryName + ": " + effectString);
            }
        }
    }

    /**
     * Apply all injury effects to a player
     */
    public void applyInjuryEffects(Player player) {
        Map<String, PlayerInjury> injuries = playerInjuries.get(player.getUniqueId());
        if (injuries == null || injuries.isEmpty()) {
            return;
        }

        for (String injuryName : injuries.keySet()) {
            InjuryType injuryType = cachedInjuryTypes.get(injuryName);
            if (injuryType == null) {
                continue;
            }

            for (String effectString : injuryType.getEffects()) {
                try {
                    String[] parts = effectString.split(":");
                    if (parts.length >= 2) {
                        // Use uppercase name to get effect type (works with legacy names)
                        @SuppressWarnings("deprecation")
                        PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                        int amplifier = Integer.parseInt(parts[1]) - 1; // Minecraft amplifiers are 0-based
                        
                        if (effectType != null) {
                            // Apply for 9 seconds (180 ticks) - reapplied every 8 seconds
                            player.addPotionEffect(new PotionEffect(effectType, 180, amplifier, false, false, false));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid effect format for injury " + injuryName + ": " + effectString);
                }
            }
        }
    }

    /**
     * Start a repeating task to apply effects to all online players
     */
    private void startEffectTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerInjuries.containsKey(player.getUniqueId())) {
                    applyInjuryEffects(player);
                }
            }
        }, 20L, 100L); // Every 5 seconds (100 ticks) - effects last 9 seconds
    }

    /**
     * Class to represent an injury type definition
     */
    public static class InjuryType {
        private final String name;
        private final String displayName;
        private final List<String> effects;

        public InjuryType(String name, String displayName, List<String> effects) {
            this.name = name;
            this.displayName = displayName;
            this.effects = new ArrayList<>(effects);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getEffects() {
            return effects;
        }
    }

    /**
     * Class to represent a player's injury
     */
    public static class PlayerInjury {
        private final String name;
        private final boolean staffOnly;

        public PlayerInjury(String name, boolean staffOnly) {
            this.name = name;
            this.staffOnly = staffOnly;
        }

        public String getName() {
            return name;
        }

        public boolean isStaffOnly() {
            return staffOnly;
        }
    }
}
