package com.ivory.commands.utils;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.config.ConfigManager;
import com.nexomc.nexo.api.NexoItems;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythiccrucible.MythicCrucible;
import io.lumine.mythiccrucible.items.CrucibleItem;
import io.lumine.mythiccrucible.items.ItemManager;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.core.drops.DropMetadataImpl;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PluginUtils {

    private final static ItemManager itemManager = MythicCrucible.inst().getItemManager();
    private final static ConfigManager configManager = IvoryCommands.getInstance().getConfigManager();

    public final static Set<Material> VANILLA_GEAR = Set.of(
        // Spears
        Material.WOODEN_SPEAR, Material.STONE_SPEAR, Material.IRON_SPEAR,
        Material.GOLDEN_SPEAR, Material.DIAMOND_SPEAR, Material.NETHERITE_SPEAR,
        Material.COPPER_SPEAR,

        // Swords
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
        Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.COPPER_SWORD,

        // Axes
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.COPPER_AXE,

        // Pickaxes
        Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
        Material.COPPER_PICKAXE,

        // Shovels
        Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
        Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
        Material.COPPER_SHOVEL,

        // Hoes
        Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
        Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
        Material.COPPER_HOE,

        // Helmets
        Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
        Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
        Material.COPPER_HELMET,
        Material.TURTLE_HELMET,

        // Chestplates
        Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
        Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
        Material.COPPER_CHESTPLATE,

        // Leggings
        Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
        Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
        Material.COPPER_LEGGINGS,

        // Boots
        Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
        Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS,
        Material.COPPER_BOOTS,

        // Other weapons/tools
        Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD,
        Material.FISHING_ROD, Material.FLINT_AND_STEEL,
        Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK,

        // New items in 1.20+
        Material.MACE,
        Material.BRUSH,

        // Shears
        Material.SHEARS
    );


    public static boolean isMythicMob(Entity entity) {
        return MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).isPresent();
    }

    public static boolean isVanillaGear(ItemStack item) {
        return item != null && VANILLA_GEAR.contains(item.getType());
    }

    public static boolean isMythicCrucibleItem(ItemStack itemStack){
        return itemManager.getItem(itemStack).isPresent();
    }

    public static CrucibleItem getMythicCrucibleItem(String item){
        return itemManager.getItem(item).orElse(null);
    }

    public static ItemStack getItemStackWithPlayerContext(String itemId, int amount, Entity entity) {
        if (entity == null) return MythicBukkit.inst().getItemManager().getItemStack(itemId, amount);
        try {
            final var maybe = MythicBukkit.inst().getItemManager().getItem(itemId);
            if (maybe.isPresent()) {
                final AbstractEntity adapted = BukkitAdapter.adapt(entity);
                final AbstractItemStack ais = maybe.get().generateItemStack(new DropMetadataImpl(new GenericCaster(adapted), adapted), amount);
                if (ais != null) return BukkitAdapter.adapt(ais);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MythicBukkit.inst().getItemManager().getItemStack(itemId, amount);
    }

    public static boolean isNexoItem(ItemStack item) {
        String id = NexoItems.idFromItem(item);
        return id != null && !id.isEmpty() && !id.trim().isEmpty();
    }

    public static boolean isCustomItem(ItemStack item){
        if (item == null || !item.hasItemMeta()) return false;
        
        // Check existing plugin APIs
        if (isNexoItem(item) || isMythicCrucibleItem(item)) {
            return true;
        }
        
        // Check for MythicMobs items via PDC (faster than full item lookup)
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Check for known custom item plugin namespaces
        if (!pdc.isEmpty()) {
            // MythicMobs items have this namespace
            if (pdc.has(new NamespacedKey("mythicmobs", "type"), PersistentDataType.STRING)) {
                return true;
            }
        }
        
        return false;
    }

    public static void replaceInventoryItem(Inventory inv, ItemStack item, int slot, Player player){
        ConfigManager cfg = IvoryCommands.getInstance().getConfigManager();
        if (item == null || item.getType() == Material.AIR) return;
        
        Material mat = item.getType();

        Boolean isCustom = PluginUtils.isCustomItem(item);
        
        // De-enchant enchanted books if enabled
        if (cfg.isRemoveEnchantedBooks() && mat == Material.ENCHANTED_BOOK && !isCustom) {
            inv.setItem(slot, new ItemStack(Material.BOOK, item.getAmount()));
            return;
        }

        // Convert vanilla items to Mythic items if enabled
        if (cfg.isConvertVanillaToMythicItems() && VANILLA_GEAR.contains(mat) && !isCustom) {
            
            String id = mat.name().toLowerCase();
            CrucibleItem cItem = PluginUtils.getMythicCrucibleItem(id);
            
            if(cItem == null) return;

            ItemStack newItem = getItemStackWithPlayerContext(cItem.getInternalName(), item.getAmount(), player);
            if (newItem != null && newItem.getType() != Material.AIR) {
                inv.setItem(slot, newItem);
            }
        }
    }

    public static void fixVersionPickup(EntityPickupItemEvent event){
        ItemStack item = event.getItem().getItemStack();
        if (item == null || !configManager.getVersionReplaceItemMaterials().contains(item.getType())) return;

        Optional<CrucibleItem> optionalItem = itemManager.getItem(item);
        if (optionalItem.isEmpty()) return;

        CrucibleItem cItem = optionalItem.get();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey versionKey = new NamespacedKey("mythicmobs", "version");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Only replace items that have no version (pre-versioning legacy items)
        if (!pdc.has(versionKey)) {
            Player player = (event.getEntity() instanceof Player p) ? p : null;
            ItemStack newItem = getItemStackWithPlayerContext(cItem.getInternalName(), item.getAmount(), player);
            if (newItem != null && newItem.getType() != Material.AIR) {
                event.getItem().setItemStack(newItem);
            }
        }
    }

    public static ItemStack fixVersionDeath(ItemStack item, Entity entity) {
        if (item == null || !configManager.getVersionReplaceItemMaterials().contains(item.getType())) {
            return null;
        }

        Optional<CrucibleItem> optionalItem = itemManager.getItem(item);
        if (optionalItem.isEmpty()) return null;

        CrucibleItem cItem = optionalItem.get();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey versionKey = new NamespacedKey("mythicmobs", "version");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Only replace if missing version (pre-versioning legacy items)
        if (!pdc.has(versionKey)) {
            ItemStack newItem = getItemStackWithPlayerContext(cItem.getInternalName(), item.getAmount(), entity);
            return (newItem != null && newItem.getType() != Material.AIR) ? newItem : null;
        }

        return null; // no replacement needed
    }

}
