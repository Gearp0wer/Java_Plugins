package com.ivory.commands.listeners;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.config.ConfigManager;
import com.ivory.commands.utils.PluginUtils;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythiccrucible.items.CrucibleItem;


public class PlayerListener implements Listener {
    private final IvoryCommands plugin;

    public PlayerListener(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Reset size (existing code)
        if (plugin.getConfigManager().isResetOnJoin()) {
            resetSize(player);
        }

        // Setup nameplate 
        plugin.getNameManager().setupPlayerNameplate(player);

        // Apply injury effects immediately on join
        plugin.getInjuryManager().applyInjuryEffects(player);

        if (plugin.getConfigManager().isFilterPetTeamPrefix()) {
            plugin.getServer().getScheduler().runTaskLater(plugin,() -> {
                if (plugin.getPipelineInjector() != null) {
                    plugin.getPipelineInjector().inject(event.getPlayer());
                }
            }, 2L); // Small delay to ensure player is fully loaded
        }

        // Clean inventory
        cleanInventory(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Reset size (existing code)
        if (plugin.getConfigManager().isResetOnQuit()) {
            resetSize(player);
        }

        if (plugin.getPipelineInjector() != null) {
            plugin.getPipelineInjector().uninject(event.getPlayer());
        }

        // Remove nameplate 
        plugin.getNameManager().removeNameplate(player);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (plugin.getConfigManager().isVersionReplaceEnabled()) {
            PluginUtils.fixVersionPickup(event);
        }
    }

    private void resetSize(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.0);
        }
    }

    private void cleanInventory(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            final int slot = i;
            ItemStack item = player.getInventory().getItem(i);
            
            PluginUtils.replaceInventoryItem(player.getInventory(), item, slot, player);
        }

        // --- Off-hand slot ---
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ConfigManager cfg = IvoryCommands.getInstance().getConfigManager();
        if (offHand == null || offHand.getType() == Material.AIR) return;
        
        Material mat = offHand.getType();

        Boolean isCustom = PluginUtils.isCustomItem(offHand);
        
        // De-enchant enchanted books if enabled
        if (cfg.isRemoveEnchantedBooks() && mat == Material.ENCHANTED_BOOK && !isCustom) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.BOOK, offHand.getAmount()));
            return;
        }

        // Convert vanilla items to Mythic items if enabled
        if (cfg.isConvertVanillaToMythicItems() && PluginUtils.VANILLA_GEAR.contains(mat) && !isCustom) {
            
            String id = mat.name().toLowerCase();
            CrucibleItem cItem = PluginUtils.getMythicCrucibleItem(id);
            
            if(cItem == null) return; 

            ItemStack newItem = BukkitAdapter.adapt(
                    cItem.getMythicItem().generateItemStack(offHand.getAmount())
            );
            player.getInventory().setItemInOffHand(newItem);
        }
        

        if(!plugin.getConfigManager().isConvertVanillaToMythicItems()) return;

        
        if (offHand != null && offHand.getType() != Material.AIR && !PluginUtils.isCustomItem(offHand)) {
            String id = offHand.getType().name().toLowerCase();
            CrucibleItem cItem = PluginUtils.getMythicCrucibleItem(id);
            if (cItem != null) {
                offHand.getEnchantments().keySet().forEach(offHand::removeEnchantment); // strip enchants
                ItemStack newItem = BukkitAdapter.adapt(cItem.getMythicItem().generateItemStack(offHand.getAmount()));
                newItem.setAmount(offHand.getAmount());
                player.getInventory().setItemInOffHand(newItem);
            }
        }

        ItemStack[] armorContents = player.getInventory().getArmorContents();

        for (int i = 0; i < armorContents.length; i++) {
            ItemStack item = armorContents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            if (PluginUtils.isVanillaGear(item) && !PluginUtils.isCustomItem(item)) {

                // Strip enchantments
                item.getEnchantments().keySet().forEach(item::removeEnchantment);

                String id = item.getType().name().toLowerCase();
                CrucibleItem cItem = PluginUtils.getMythicCrucibleItem(id);
                if (cItem != null) {
                    ItemStack newItem = BukkitAdapter.adapt(
                            cItem.getMythicItem().generateItemStack(item.getAmount())
                    );
                    newItem.setAmount(item.getAmount());
                    armorContents[i] = newItem;
                }
            }
        }
        player.getInventory().setArmorContents(armorContents);
    }
}
