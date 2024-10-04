package com.github.inkm3.signtp.Util;

import com.github.inkm3.signtp.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class Message {

    private static final Plugin plugin = Main.getPlugin(Main.class);

    private static final File LangFile = new File(plugin.getDataFolder(), "message.yml");
    private static final YamlConfiguration LangConfig = YamlConfiguration.loadConfiguration(LangFile);

    private static boolean ExistsLangFile = true;


    public static void load() {
        if (!LangFile.exists()) plugin.saveResource("message.yml", false);

        LangConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("message.yml"))));

        try {
            LangConfig.load(LangFile);
        }  catch (IOException | InvalidConfigurationException ignore) {
            plugin.getLogger().warning("langファイルを読み込めませんでした");
            ExistsLangFile = false;
        }

    }

    public static String getMessage(Locale locale, MessageType type) {
        if (!ExistsLangFile) return "";

        return  LangConfig.getString(type.getPath() + locale,
                        LangConfig.getString(type.getPath() + "en_US",
                                LangConfig.getString(type.getPath() + "ja_JP",
                                        LangConfig.getDefaults().getString(type.getPath() + "ja_JP", ""))));
    }

    public enum MessageType {
        Setting_AuthorityMissing("ErrorMessage.Setting.AuthorityMissing."),
        Setting_ServerNameEmpty("ErrorMessage.Setting.ServerNameEmpty."),

        ConnectionFailure_Authority("ErrorMessage.ConnectionFailure.Authority."),
        ConnectionFailure_Unknown("ErrorMessage.ConnectionFailure.Unknown."),
        ConnectionFailure_Offline("ErrorMessage.ConnectionFailure.Offline."),
        ConnectionFailure_Error("ErrorMessage.ConnectionFailure.Error."),
        ConnectionFailure_Connecting("ErrorMessage.ConnectionFailure.Connecting."),
        ConnectionFailure_Connected("ErrorMessage.ConnectionFailure.Connected.");

        private final String path;
        MessageType(String path) {
            this.path = path;
        }

        public String getPath() {
            return this.path;
        }
    }
}
