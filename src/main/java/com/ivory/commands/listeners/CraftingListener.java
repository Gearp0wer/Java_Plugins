package com.ivory.commands.listeners;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.utils.PluginUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    private final IvoryCommands plugin;

    public CraftingListener(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!plugin.getConfigManager().isConvertCraftedVanillaToMythic()) return;

        // Check if recipe exists (can be null when items don't form a valid recipe)
        if (event.getRecipe() == null) return;

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        // Only convert valid vanilla gear
        if (PluginUtils.isVanillaGear(result)
            && !PluginUtils.isCustomItem(result)) {

            String id = result.getType().name().toLowerCase();
            if (PluginUtils.getMythicCrucibleItem(id) == null) return;

            Player player = (event.getView().getPlayer() instanceof Player p) ? p : null;
            ItemStack newItem = PluginUtils.getItemStackWithPlayerContext(id, result.getAmount(), player);
            
            // Only set result if mythic item was successfully created
            if (newItem != null && newItem.getType() != Material.AIR) {
                event.getInventory().setResult(newItem);
            }
            
        }
    }
}
