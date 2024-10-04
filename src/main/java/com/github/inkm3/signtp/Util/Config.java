package com.github.inkm3.signtp.Util;

import com.github.inkm3.signtp.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class Config {

    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final FileConfiguration config = plugin.getConfig();

    private static final String PATH_UpdateInterval = "SignTextConfig.UpdateInterval";
    private static final String PATH_RequireChunkLoaded = "SignTextConfig.RequireChunkLoaded";
    private static final String PATH_DataChangeAutoSave = "SignTextConfig.DataChangeAutoSave";
    private static final String PATH_UnknownValuePlaceholder = "SignTextConfig.UnknownValuePlaceholder";


    private static int UpdateInterval = 2;
    private static boolean RequireChunkLoaded = true;
    private static boolean DataChangeAutoSave = false;
    private static String UnknownValuePlaceholder = "?";


    public static void load() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public static void reloadConfig() {
        setUpdateInterval(config.getInt(PATH_UpdateInterval, config.getDefaults().getInt(PATH_UpdateInterval)));
        setRequireChunkLoaded(config.getBoolean(PATH_RequireChunkLoaded, config.getDefaults().getBoolean(PATH_RequireChunkLoaded)));
        setDataChangeAutoSave(config.getBoolean(PATH_DataChangeAutoSave, config.getDefaults().getBoolean(PATH_DataChangeAutoSave)));
        setUnknownValuePlaceholder(config.getString(PATH_UnknownValuePlaceholder, config.getDefaults().getString(PATH_UnknownValuePlaceholder)));
    }

    public static int getUpdateInterval() {
        return UpdateInterval;
    }

    public static boolean getRequireChunkLoaded() {
        return RequireChunkLoaded;
    }

    public static boolean getDataChangeAutoSave() {
        return DataChangeAutoSave;
    }

    public static String getUnknownValuePlaceholder() {
        return UnknownValuePlaceholder;
    }

    public static void setUpdateInterval(int value) {
        UpdateInterval = Math.min(Math.max(value, -60), 60);
        config.set(PATH_UpdateInterval, value);
        plugin.saveConfig();
    }

    public static void setRequireChunkLoaded(boolean value) {
        RequireChunkLoaded = value;
        config.set(PATH_RequireChunkLoaded, value);
        plugin.saveConfig();
    }

    public static void setDataChangeAutoSave(boolean value) {
        DataChangeAutoSave = value;
        config.set(PATH_DataChangeAutoSave, value);
        plugin.saveConfig();
    }

    public static void setUnknownValuePlaceholder(String value) {
        UnknownValuePlaceholder = value;
        config.set(PATH_UnknownValuePlaceholder, value);
        plugin.saveConfig();
    }
}
