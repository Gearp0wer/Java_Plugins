package com.ivory.commands.archeo;

import com.ivory.commands.IvoryCommands;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BrushableBlock;

import java.util.Arrays;
import java.util.List;

public class ArcheoCommand implements CommandExecutor, TabCompleter {

    private final IvoryCommands plugin;

    public ArcheoCommand(IvoryCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("ivorycommands.archeo.use")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§eUsage: /archeo <sand|gravel>");
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must be holding an item to create suspicious blocks.");
            return true;
        }

        Material suspiciousBlock;
        switch (args[0].toLowerCase()) {
            case "sand" -> suspiciousBlock = Material.SUSPICIOUS_SAND;
            case "gravel" -> suspiciousBlock = Material.SUSPICIOUS_GRAVEL;
            default -> {
                player.sendMessage("§eUsage: /archeo <sand|gravel>");
                return true;
            }
        }

        // Create the suspicious block with the held item inside
        ItemStack suspiciousBlockItem = new ItemStack(suspiciousBlock);
        BlockStateMeta meta = (BlockStateMeta) suspiciousBlockItem.getItemMeta();
        
        if (meta != null) {
            BrushableBlock brushableBlock = (BrushableBlock) meta.getBlockState();
            
            // Clone the held item to avoid modifying the original
            ItemStack itemToHide = heldItem.clone();
            itemToHide.setAmount(1); // Only one item per block
            
            brushableBlock.setItem(itemToHide);
            meta.setBlockState(brushableBlock);
            suspiciousBlockItem.setItemMeta(meta);
            
            // Give the suspicious block to the player
            player.getInventory().addItem(suspiciousBlockItem);
            player.sendMessage("§aYou received a suspicious " + args[0].toLowerCase() + " containing " + 
                              heldItem.getType().toString().toLowerCase().replace('_', ' ') + ".");
        } else {
            player.sendMessage("§cFailed to create suspicious block.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sand", "gravel");
        }
        return List.of();
    }
}
