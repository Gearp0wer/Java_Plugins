package com.ivory.commands.listeners;

import com.ivory.commands.IvoryCommands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class AnvilListener implements Listener {

    private final IvoryCommands plugin;
    private final Set<String> messageTracker = new HashSet<>();

    public AnvilListener(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfigManager().isPreventAnvilEnchantments()) return;

        Inventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0);
        ItemStack secondItem = inventory.getItem(1);

        if (secondItem == null) return;

        boolean cancel = false;

        // 1. Block enchanted books as second item
        if (secondItem.getType() == Material.ENCHANTED_BOOK) {
            cancel = true;
        }

        // 2. Block fusing two same items if enchantments are present
        else if (firstItem != null 
                && firstItem.getType() == secondItem.getType() 
                && (firstItem.getEnchantments().size() > 0 || secondItem.getEnchantments().size() > 0)) {
            cancel = true;
        }

        if (cancel) {
            event.setResult(null); // cancel anvil result

            if (event.getView().getPlayer() instanceof Player player) {
                String playerName = player.getName();

                if (!messageTracker.contains(playerName)) {
                    messageTracker.add(playerName);
                    player.sendMessage("§cYou cannot combine these items in the anvil!");

                    // Remove from tracker after 3s (60 ticks)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            messageTracker.remove(playerName);
                        }
                    }.runTaskLater(plugin, 60L);
                }
            }
        }
    }

}
