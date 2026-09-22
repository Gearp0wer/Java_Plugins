package com.ivory.commands.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.utils.PluginUtils;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythiccrucible.items.CrucibleItem;

public class MobListener implements Listener {

    private final IvoryCommands plugin;

    public MobListener(IvoryCommands plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        List<ItemStack> toAdd = new ArrayList<>();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null) continue;

            // BLOCKED overrides everything
            if (plugin.getConfigManager().isMobDropBlocked() && PluginUtils.isVanillaGear(item)) {
                iterator.remove();
                continue;
            }

            // Conversion only applies if blocked is false
            if (!plugin.getConfigManager().isMobDropBlocked() && plugin.getConfigManager().isMobLootConversion()) {
                if (PluginUtils.isVanillaGear(item) && !PluginUtils.isCustomItem(item)) {
                    item.getEnchantments().keySet().forEach(item::removeEnchantment);

                    String id = item.getType().name().toLowerCase();
                    CrucibleItem cItem = PluginUtils.getMythicCrucibleItem(id);

                    if (cItem != null) {
                        ItemStack newItem = BukkitAdapter.adapt(
                            cItem.getMythicItem().generateItemStack(item.getAmount())
                        );
                        
                        // Preserve durability from original item
                        if (item.getItemMeta() instanceof Damageable originalMeta &&
                            newItem.getItemMeta() instanceof Damageable newMeta) {
                            newMeta.setDamage(originalMeta.getDamage());
                            newItem.setItemMeta(newMeta);
                        }
                        
                        iterator.remove();
                        toAdd.add(newItem);
                        continue;
                    }
                }
            }

            // Version replacement (if enabled)
            if (plugin.getConfigManager().isVersionReplaceEnabled()) {
                ItemStack replacement = PluginUtils.fixVersionDeath(item, event.getEntity());
                if (replacement != null) {
                    iterator.remove();
                    toAdd.add(replacement);
                }
            }
        }

        // Apply all new drops after iteration
        event.getDrops().addAll(toAdd);
    }
}

    
