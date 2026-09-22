package com.ivory.commands.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.ivory.commands.IvoryCommands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInteractListener implements Listener {
    
    private final IvoryCommands plugin;

    // Cooldown map to prevent spam clicking
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    private static final long COOLDOWN_MS = 100; // 500ms cooldown

    public PlayerInteractListener(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        if(plugin.getConfigManager().isFilterPetTeamPrefix()){
            if (entity instanceof Tameable pet
                    && pet.isTamed()
                    && pet.getOwner() == player
                    && event.getHand() == EquipmentSlot.OFF_HAND)
            {
                event.setCancelled(true);
                return;
            }

            //Only handle sittable entities
            if (!(entity instanceof Sittable)) {
                return;
            }
            
            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();
            if (lastInteraction.containsKey(playerId)) {
                long timeSinceLastClick = now - lastInteraction.get(playerId);
                if (timeSinceLastClick < COOLDOWN_MS) {
                    event.setCancelled(true);
                    return;
                }
            }
            lastInteraction.put(playerId, now);
        }
    }
}