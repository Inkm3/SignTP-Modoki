package com.github.inkm3.signtp;

import com.github.inkm3.signtp.Commands.SignTPCommand;
import com.github.inkm3.signtp.CustomEvents.SignTPEnableEvent;
import com.github.inkm3.signtp.Events.ServerEvents;
import com.github.inkm3.signtp.Events.SignEvents;
import com.github.inkm3.signtp.FileEdit.ServerData;
import com.github.inkm3.signtp.FileEdit.SignData;
import com.github.inkm3.signtp.System.SignTP;
import com.github.inkm3.signtp.Util.Config;
import com.github.inkm3.signtp.Util.Message;
import com.github.inkm3.signtp.Util.SignText;
import com.github.inkm3.signtp.Util.UpdateChecker;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));
    }

    @Override
    public void onEnable() {
        UpdateChecker.getInstance(this).checkForUpdates();
        CommandAPI.onEnable();

        Config.load();
        ServerData.load();
        SignData.load();
        Message.load();
        SignText.load();

        ServerEvents.load();
        SignEvents.load();
        SignTPCommand.load();

        getServer().getPluginManager().callEvent(new SignTPEnableEvent());

        SignTP.startDataSyncLoop();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();

        SignData.save();
        ServerData.save();
    }

}
