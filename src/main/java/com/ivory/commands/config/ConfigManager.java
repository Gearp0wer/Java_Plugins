package com.ivory.commands.config;

import com.ivory.commands.IvoryCommands;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final IvoryCommands plugin;

    // Cached values
    // Resize
    private double resizeMin;
    private double resizeMax;
    private boolean resetOnJoin;
    private boolean resetOnQuit;

    // Character names
    private boolean showNameInPlate;
    private String nameSeparator;
    private int minNameLength;
    private int maxNameLength;
    private boolean allowColors;
    private boolean filterPetTeamPrefix;
    private boolean overrideBookAuthor;

    // Inventory blocker
    private boolean enchantingTableBlocked;
    private boolean grindstoneBlocked;

    // Mob behaviours
    private boolean mobsDropItems;

    // Conversions
    private boolean mobLootConversion;
    private boolean changeVanillaToMmItems;
    private boolean convertCraftedVanillaToMm;
    private boolean preventAnvilEnchantments;
    private boolean removeEnchantedBooks;
    private boolean versionReplaceEnabled;
    private List<Material> versionReplaceItemMaterials;

    public ConfigManager(IvoryCommands plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Resize
        resizeMin = cfg.getDouble("resize.min-size", 0.75);
        resizeMax = cfg.getDouble("resize.max-size", 1.25);
        resetOnJoin = cfg.getBoolean("resize.reset-size-on-join", false);
        resetOnQuit = cfg.getBoolean("resize.reset-size-on-quit", false);

        // Character names
        showNameInPlate = cfg.getBoolean("character-names.show-name-in-nameplate", true);
        nameSeparator = cfg.getString("character-names.separator", "~");
        minNameLength = cfg.getInt("character-names.min-length", 3);
        maxNameLength = cfg.getInt("character-names.max-length", 32);
        allowColors = cfg.getBoolean("character-names.allow-colors", false);
        filterPetTeamPrefix = cfg.getBoolean("character-names.filter-pet-team-prefix", true);
        overrideBookAuthor = cfg.getBoolean("character-names.override-book-author", true);

        // Inventory blocker
        enchantingTableBlocked = cfg.getBoolean("inventory-blocker.enchanting-table-blocked", true);
        grindstoneBlocked = cfg.getBoolean("inventory-blocker.grindstone-blocked", true);
        preventAnvilEnchantments = cfg.getBoolean("inventory-blocker.prevent-anvil-enchantments", true);

        // Mob behaviours
        mobsDropItems = cfg.getBoolean("mob-behaviours.loot-blocked", false);
        mobLootConversion = cfg.getBoolean("mob-behaviours.loot-conversion", true);

        // Conversions
        removeEnchantedBooks = cfg.getBoolean("conversions.remove-enchanted-books", false);
        changeVanillaToMmItems = cfg.getBoolean("conversions.convert-vanilla-to-mm-items", true);
        convertCraftedVanillaToMm = cfg.getBoolean("conversions.convert-crafted-vanilla-to-mm", true);
        versionReplaceEnabled = cfg.getBoolean("conversions.version-replace-enabled", false);
        versionReplaceItemMaterials = cfg.getStringList("conversions.version-replace-item-materials").stream()
                                    .map(String::toUpperCase) // Bukkit enum names are uppercase
                                    .map(s -> {
                                        Material mat = Material.getMaterial(s);
                                        if (mat == null) {
                                            plugin.getLogger().warning("Invalid material in version-replace-item-groups: " + s);
                                        }
                                        return mat;
                                    })
                                    .filter(mat -> mat != null)
                                    .toList();

        plugin.getLogger().info("Config loaded!");
    }

    // === Getters ===
    public double getResizeMin() { return resizeMin; }
    public double getResizeMax() { return resizeMax; }
    public boolean isResetOnJoin() { return resetOnJoin; }
    public boolean isResetOnQuit() { return resetOnQuit; }

    public boolean isShowNameInPlate() { return showNameInPlate; }
    public String getNameSeparator() { return nameSeparator; }
    public int getMinNameLength() { return minNameLength; }
    public int getMaxNameLength() { return maxNameLength; }
    public boolean isAllowColors() { return allowColors; }
    public boolean isFilterPetTeamPrefix() { return filterPetTeamPrefix; }
    public boolean isOverrideBookAuthor() { return overrideBookAuthor; }

    public boolean isEnchantingTableBlocked() { return enchantingTableBlocked; }
    public boolean isGrindstoneBlocked() { return grindstoneBlocked; }

    public boolean isMobDropBlocked() { return mobsDropItems; }

    public boolean isMobLootConversion() { return mobLootConversion; }
    public boolean isRemoveEnchantedBooks() { return removeEnchantedBooks; }
    public boolean isConvertVanillaToMythicItems() { return changeVanillaToMmItems; }
    public boolean isConvertCraftedVanillaToMythic() { return convertCraftedVanillaToMm; }
    public boolean isPreventAnvilEnchantments() { return preventAnvilEnchantments; }
    public boolean isVersionReplaceEnabled() { return versionReplaceEnabled; }
    public List<Material> getVersionReplaceItemMaterials() { return versionReplaceItemMaterials; }
}
