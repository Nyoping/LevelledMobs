package me.lokka30.levelledmobs.bukkit;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.command.CommandHandler;
import me.lokka30.levelledmobs.bukkit.config.ConfigHandler;
import me.lokka30.levelledmobs.bukkit.integration.IntegrationHandler;
import me.lokka30.levelledmobs.bukkit.listener.ListenerHandler;
import me.lokka30.levelledmobs.bukkit.logic.LogicHandler;
import me.lokka30.levelledmobs.bukkit.util.ClassUtils;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LevelledMobs extends JavaPlugin {

    /* vars */

    private final CommandHandler commandHandler = new CommandHandler();
    private final ConfigHandler configHandler = new ConfigHandler();
    private final IntegrationHandler integrationHandler = new IntegrationHandler();
    private final ListenerHandler listenerHandler = new ListenerHandler();
    private final LogicHandler logicHandler = new LogicHandler();

    /* methods */

    @Override
    public void onLoad() {
        instance = this;
        Log.inf("Plugin initialized");
    }

    @Override
    public void onEnable() {
        if(!(
            assertRunningSpigot() &&
            getConfigHandler().load() &&
            getLogicHandler().load() &&
            getListenerHandler().load() &&
            getCommandHandler().load()
        )) {
            Log.sev("LevelledMobs encountered a fatal error during the startup process; " +
                "it will disable itself to prevent possible issues resulting from malfunction.",
                true);
            setEnabled(false);
            return;
        }

        final var version = getDescription().getVersion();
        if(version.contains("alpha") || version.contains("beta")) {
            Log.war("You are running an alpha/beta version of LevelledMobs. Please take care, "
            + "and beware that this version is unlikely to be tested.", false);
        }

        runTestingProcedures();

        Log.inf("Plugin enabled");
    }

    @Override
    public void onDisable() {
        Log.inf("Plugin disabled");
    }

    /*
    Check if the server is running SpigotMC, or any derivative software.
     */
    private boolean isRunningSpigot() {
        return ClassUtils.classExists("net.md_5.bungee.api.chat.TextComponent");
    }

    /*
    Ensure the server is running SpigotMC, or any derivative software.
     */
    private boolean assertRunningSpigot() {
        if(isRunningSpigot()) return true;

        Log.sev("LevelledMobs does not run on CraftBukkit or other software which is not " +
            "based upon the SpigotMC software. Switch to PaperMC or SpigotMC software - there " +
            "is no reason to run CraftBukkit.", false);

        return false;
    }

    private void runTestingProcedures() {
        Log.war("Running testing procedures", false);
    }

    /* getters and setters */

    public CommandHandler getCommandHandler() { return commandHandler; }
    public ConfigHandler getConfigHandler() { return configHandler; }
    public IntegrationHandler getIntegrationHandler() { return integrationHandler; }
    public ListenerHandler getListenerHandler() { return listenerHandler; }
    public LogicHandler getLogicHandler() { return logicHandler; }

    /* singleton */

    private static LevelledMobs instance;

    @NotNull
    public static LevelledMobs getInstance() {
        return Objects.requireNonNull(instance, "instance");
    }

}
