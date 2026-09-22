package com.ivory.commands.listeners;

import com.ivory.commands.IvoryCommands;
import com.ivory.commands.name.CharacterNameManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

public class BookAuthorOverrideListener implements Listener {
    private final IvoryCommands plugin;
    private final CharacterNameManager characterNameManager;

    public BookAuthorOverrideListener(IvoryCommands plugin, CharacterNameManager characterNameManager) {
        this.plugin = plugin;
        this.characterNameManager = characterNameManager;
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;
        if (!plugin.getConfigManager().isOverrideBookAuthor()) return;

        Player player = event.getPlayer();
        if (player == null) return;

        String characterName = characterNameManager.getCharacterName(player.getUniqueId());
        if (characterName == null) return;

        // Translate color codes
        characterName = ChatColor.translateAlternateColorCodes('&', characterName);

        BookMeta meta = event.getNewBookMeta();
        meta.setAuthor(characterName);
        // Store real username in NBT as 'username'
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "username"),
            PersistentDataType.STRING,
            player.getName()
        );
        event.setNewBookMeta(meta);
    }
}
