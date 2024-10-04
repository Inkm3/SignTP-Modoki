package com.github.inkm3.signtp.FileEdit;

import com.github.inkm3.signtp.CustomEvents.ServerDataUpdateEvent;
import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.Util.Config;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerData {
    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final PluginManager manager = plugin.getServer().getPluginManager();

    private static final HashMap<String, ServerData> MEMORY = new HashMap<>();
    private static String SERVER_NAME = "";

    private static final File ServerDataFile = new File(plugin.getDataFolder(), "data/server.yml");
    private static final YamlConfiguration ServerDataConfig = YamlConfiguration.loadConfiguration(ServerDataFile);

    private final String name;
    private String status;
    private int slot;
    private int count;
    private String ip;
    private int port;
    private boolean received;

    public ServerData(String name, String status, int slot, int count, String ip, int port, boolean received) {
        this.name = name;
        this.status = status;
        this.slot = slot;
        this.count = count;
        this.ip = ip;
        this.port = port;
        this.received = received;
        save(name, this);
        manager.callEvent(new ServerDataUpdateEvent(this));
    }

    public String name() {
        return this.name;
    }
    public String status() {
        return this.status;
    }
    public int slot() {
        return this.slot;
    }
    public int count() {
        return this.count;
    }
    public String ip() {
        return this.ip;
    }
    public int port() {
        return this.port;
    }
    public boolean received() {
        return this.received;
    }

    public void status(String status) {
        if (!this.status.equals(status)) {
            this.status = status;
            manager.callEvent(new ServerDataUpdateEvent(this));
            save(this.name, this);
        }
    }

    public void slot(int slot) {
        if (this.slot != slot) {
            this.slot = slot;
            manager.callEvent(new ServerDataUpdateEvent(this));
            save(this.name, this);
        }
    }

    public void count(int count) {
        if (this.count != count) {
            this.count = count;
            manager.callEvent(new ServerDataUpdateEvent(this));
            save(this.name, this);
        }
    }

    public void ip(String ip) {
        if (!this.ip.equals(ip)) {
            this.ip = ip;
            save(this.name, this);
        }
    }

    public void port(int port) {
        if (this.port != port) {
            this.port = port;
            save(this.name, this);
        }
    }

    public void received(boolean received) {
        if (this.received != received) {
            this.received = received;
            save(this.name, this);
        }
    }


    public static void load() {
        if (!ServerDataFile.exists()) plugin.saveResource("data/server.yml", false);

        try {
            ServerDataConfig.load(ServerDataFile);
        }  catch (IOException | InvalidConfigurationException ignore) {
            plugin.getLogger().warning("看板の保存データを読み込めませんでした");
            return;
        }

        try {
            List<Map<String, Object>> loadData = (List<Map<String, Object>>) ServerDataConfig.getList("data");
            if (loadData != null) {
                loadData.forEach(map -> {
                    if (map.size() == 7) {
                        String name =   map.get("Name") instanceof String s ? s : null;
                        String status = map.get("Status") instanceof String s ? s : null;
                        Integer slot =  map.get("Slot") instanceof Integer i ? i : null;
                        Integer count = map.get("Count") instanceof Integer i ? i : null;
                        String ip =     map.get("IP") instanceof String s ? s : null;
                        Integer port =  map.get("Port") instanceof Integer i ? i : null;
                        Boolean received = map.get("Received") instanceof Boolean b ? b : null;

                        if (Stream.of(name, status, slot, count, ip, port, received).allMatch(Objects::nonNull)) {
                            new ServerData(name, status, slot, count, ip, port, received);
                        }
                    }
                });
                save();
            }
        } catch (ClassCastException ignore) { }
    }

    public static HashSet<ServerData> getAll() {
        return MEMORY.values().stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    public static ServerData get(String name) {
        return MEMORY.getOrDefault(name, null);
    }

    public static ServerData getServer() {
        return get(SERVER_NAME);
    }

    public static void setServerName(String name) {
        if (SERVER_NAME.isEmpty()) SERVER_NAME = name;
    }

    public static void save(String name, ServerData data) {
        MEMORY.put(name, data);

        if (Config.getDataChangeAutoSave()) {
            plugin.getServer().getScheduler().runTask(plugin, ServerData::save);
        }
    }

    public static void save() {
        if (!ServerDataFile.exists()) plugin.saveResource("data/server.yml", false);

        List<LinkedHashMap<String, Object>> saveData = new ArrayList<>();

        MEMORY.values()
                .forEach(data -> saveData.add(new LinkedHashMap<>(){{
                    put("Name", data.name);
                    put("Status", data.status);
                    put("Slot", data.slot);
                    put("Count", data.count);
                    put("IP", data.ip);
                    put("Port", data.port);
                    put("Received", data.received);
                }}));

        ServerDataConfig.set("data", saveData);

        try {
            ServerDataConfig.save(ServerDataFile);
        } catch (IOException ignore) {
            plugin.getLogger().warning("看板の保存データを保存できませんでした");
        }
    }
}
