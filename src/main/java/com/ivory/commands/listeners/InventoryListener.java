package com.ivory.commands.listeners;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.config.ConfigManager;
import com.ivory.commands.utils.PluginUtils;

import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final IvoryCommands plugin;

    public InventoryListener(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        blockInventories(event);
        processInventoryConversions(event);
    }

    private void blockInventories(InventoryOpenEvent event){
        Player player = (Player) event.getPlayer();
        InventoryType type = event.getInventory().getType();
        // Check blocked inventories
        if ((type == InventoryType.ENCHANTING && plugin.getConfigManager().isEnchantingTableBlocked())
            || (type == InventoryType.GRINDSTONE && plugin.getConfigManager().isGrindstoneBlocked())) {

            event.setCancelled(true);
            player.sendMessage("§cYou are not allowed to use this!");
        }
    }

    private void processInventoryConversions(InventoryOpenEvent event) {
        ConfigManager cfg = plugin.getConfigManager();
        
        if (!cfg.isRemoveEnchantedBooks() && !cfg.isConvertVanillaToMythicItems()) return;

        Inventory inv = event.getInventory();
        InventoryType type = inv.getType();

        // Only process real inventories
        if (!(type == InventoryType.CHEST && (inv.getHolder() instanceof Chest || inv.getHolder() instanceof DoubleChest))
                && !(type == InventoryType.BARREL && inv.getHolder() instanceof Barrel)
                && !(inv.getHolder() instanceof StorageMinecart)) return;

        for (int i = 0; i < inv.getSize(); i++) {
            final int slot = i;
            ItemStack item = inv.getItem(i);
            Player player = (event.getPlayer() instanceof Player p) ? p : null;
            PluginUtils.replaceInventoryItem(inv, item, slot, player);
        }
    }
}
