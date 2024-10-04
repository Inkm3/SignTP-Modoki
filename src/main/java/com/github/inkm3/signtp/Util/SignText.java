package com.github.inkm3.signtp.Util;

import com.github.inkm3.signtp.FileEdit.ServerData;
import com.github.inkm3.signtp.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class SignText {

    private static final Plugin plugin = Main.getPlugin(Main.class);

    private static final File SignTextFile = new File(plugin.getDataFolder(), "text/signtext.yml");
    private static final YamlConfiguration SignTextConfig = YamlConfiguration.loadConfiguration(SignTextFile);

    public static void load() {
        if (!SignTextFile.exists()) plugin.saveResource("text/signtext.yml", false);

        SignTextConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("text/signtext.yml"))));

        try {
            SignTextConfig.load(SignTextFile);
        }  catch (IOException | InvalidConfigurationException ignore) {
            plugin.getLogger().warning("signtext.ymlを読み込めませんでした");
        }

    }

    public static List<Component> getText(String[] args, String[] display) {

        String server = args[1];
        String title = args[2].isEmpty() ? args[1] : args[2];
        String template = args[3];

        ServerData info = ServerData.get(server);
        String unknownValue = Config.getUnknownValuePlaceholder();

        String status = info != null ?  info.status() : "UNKNOWN";
        String slot =   info != null && info.slot() >= 0 ? String.valueOf(info.slot()) : unknownValue;
        String count =  info != null && info.slot() >= 0 ? String.valueOf(info.count()) : unknownValue;

        String path = template + "." + status;
        String defPath = "Default." + status;
        List<String> line = SignTextConfig.getStringList(path);

        if (Arrays.stream(display).allMatch(String::isEmpty)) {
            if (line.size() != 4) line = SignTextConfig.getStringList(defPath);
            if (line.size() != 4) line = SignTextConfig.getDefaults().getStringList(defPath);

            return line.stream().map(text -> MiniMessage.miniMessage().deserialize(text,
                    Placeholder.unparsed("server", server),
                    Placeholder.unparsed("title", title),
                    Placeholder.unparsed("slot", slot),
                    Placeholder.unparsed("count", count))
            ).toList();
        }
        else {
            return Arrays.stream(display).map(text -> MiniMessage.miniMessage().deserialize(text,
                    Placeholder.unparsed("server", server),
                    Placeholder.unparsed("title", title),
                    Placeholder.unparsed("slot", slot),
                    Placeholder.unparsed("count", count))
            ).toList();
        }
    }

    public static List<Component> getText(String[] args, Player player) {

        String server = args[1];
        String title = args[2].isEmpty() ? args[1] : args[2];
        String template = args[3];

        ServerData info = ServerData.get(server);

        String name = player.getName();
        String unknownValue = Config.getUnknownValuePlaceholder();

        String slot = info != null ? String.valueOf(info.slot()) : unknownValue;
        String count = info != null ? String.valueOf(info.count()) : unknownValue;

        String path = template + "." + "CONNECT";
        String defPath = "Default." +  "CONNECT";
        List<String> line = SignTextConfig.getStringList(path);

        if (line.size() != 4) line = SignTextConfig.getStringList(defPath);
        if (line.size() != 4) line = SignTextConfig.getDefaults().getStringList(defPath);

        return line.stream().map(text -> MiniMessage.miniMessage().deserialize(text,
                Placeholder.unparsed("server", server),
                Placeholder.unparsed("title", title),
                Placeholder.unparsed("slot", slot),
                Placeholder.unparsed("count", count),
                Placeholder.unparsed("player", name))
        ).toList();
    }
}
