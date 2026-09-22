package com.ivory.commands;

import com.ivory.commands.archeo.ArcheoCommand;
import com.ivory.commands.config.ConfigCommand;
import com.ivory.commands.config.ConfigManager;
import com.ivory.commands.injury.InjuryCommand;
import com.ivory.commands.injury.InjuryManager;
import com.ivory.commands.injury.InjuryTabCompleter;
import com.ivory.commands.listeners.AnvilListener;
import com.ivory.commands.listeners.BookAuthorOverrideListener;
import com.ivory.commands.listeners.CraftingListener;
import com.ivory.commands.listeners.InventoryListener;
import com.ivory.commands.listeners.MobListener;
import com.ivory.commands.listeners.PlayerInteractListener;
import com.ivory.commands.listeners.PlayerListener;
import com.ivory.commands.name.CharacterNameManager;
import com.ivory.commands.name.PipelineInjector;
import com.ivory.commands.resize.PlayerResizeManager;
import com.ivory.commands.placeholders.IvoryPlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class IvoryCommands extends JavaPlugin {
    private static IvoryCommands instance;
    private ConfigManager configManager;
    private PlayerResizeManager resizeManager;
    private CharacterNameManager nameManager;
    private InjuryManager injuryManager;
    private PipelineInjector pipelineInjector;
    private IvoryPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        try {
            pipelineInjector = new PipelineInjector(this);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            getLogger().log(java.util.logging.Level.SEVERE,
                    "Unsupported server internals; pet name filtering is disabled.", exception);
            pipelineInjector = null;
        }

        resizeManager = new PlayerResizeManager(this);
        nameManager = new CharacterNameManager(this);
        injuryManager = new InjuryManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BookAuthorOverrideListener(this, nameManager), this);

        // CRITICAL: Initialize existing pets BEFORE starting packet injection
        if (configManager.isFilterPetTeamPrefix() && pipelineInjector != null)
        {
            getServer().getScheduler().runTaskLater(this, pipelineInjector::injectAll, 20L);
        }

        ConfigCommand configCommand = new ConfigCommand(this);
        getCommand("ivorycommands").setExecutor(configCommand);
        getCommand("ivorycommands").setTabCompleter(configCommand);
        getCommand("ic").setExecutor(configCommand);
        getCommand("ic").setTabCompleter(configCommand);

        ArcheoCommand archeoCommand = new ArcheoCommand(this);
        getCommand("archeo").setExecutor(archeoCommand);
        getCommand("archeo").setTabCompleter(archeoCommand);

        InjuryCommand injuryCommand = new InjuryCommand(this, injuryManager);
        InjuryTabCompleter injuryTabCompleter = new InjuryTabCompleter(injuryManager);
        getCommand("injury").setExecutor(injuryCommand);
        getCommand("injury").setTabCompleter(injuryTabCompleter);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new IvoryPlaceholderExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }

        getLogger().info("IvoryCommands enabled!");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        // Remove all handlers before disabling
        if (pipelineInjector != null) {
            pipelineInjector.unload();
        }
        getLogger().info("IvoryCommands disabled!");
    }

    public void reload() {
        nameManager.reload();
        injuryManager.reload();
        
        if (pipelineInjector == null)
        {
            return;
        }

        pipelineInjector.unload();
        if (configManager.isFilterPetTeamPrefix())
        {
            getServer().getScheduler().runTaskLater(this, pipelineInjector::injectAll, 2L);
        }
    }

    // Expose managers
    public static IvoryCommands getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public PlayerResizeManager getResizeManager() { return resizeManager; }
    public CharacterNameManager getNameManager() { return nameManager; }
    public InjuryManager getInjuryManager() { return injuryManager; }
    public PipelineInjector getPipelineInjector() { return pipelineInjector; }
}
