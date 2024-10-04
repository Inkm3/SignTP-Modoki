package com.github.inkm3.signtp.Events;

import com.github.inkm3.signtp.FileEdit.ServerData;
import com.github.inkm3.signtp.Main;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.io.*;
import java.net.Socket;

public class ServerEvents {

    private static final Plugin plugin = Main.getPlugin(Main.class);

    private static boolean DataSent = false;

    public static void load() {
        PluginManager manager = plugin.getServer().getPluginManager();
        Messenger messenger = plugin.getServer().getMessenger();

        manager.registerEvents(new DataInit(), plugin);
        manager.registerEvents(new ServerChange(), plugin);
        messenger.registerIncomingPluginChannel(plugin, "BungeeCord", new ReceivePluginMessage());
        messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    static class ReceivePluginMessage implements PluginMessageListener {

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
            if (!"BungeeCord".equals(channel)) return;

            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            String sub = in.readUTF();

            switch (sub) {
                case "GetServer" -> {
                    String name = in.readUTF();
                    ServerData data = ServerData.get(name);

                    if (data == null) {
                        String status = "ONLINE";
                        int slot = plugin.getServer().getMaxPlayers();
                        int count = plugin.getServer().getOnlinePlayers().size();
                        String ip = "";
                        int port = plugin.getServer().getPort();

                        ServerData.getAll().forEach(svData -> svData.received(false));
                        new ServerData(name, status, slot, count, ip, port, true);
                    }
                    else if (data.slot() != plugin.getServer().getMaxPlayers()) {
                        data.slot(plugin.getServer().getMaxPlayers());
                        ServerData.getAll().stream()
                                .filter(svData -> !svData.equals(data))
                                .forEach(svData -> svData.received(false));
                    }

                    ServerData.setServerName(name);
                    SendPluginMessage.ServerIP(player, name);
                    SendPluginMessage.MaxSlot(player);
                }

                case "GetServers" -> {
                    for (String name : in.readUTF().split(", ")) {
                        if (ServerData.get(name) == null) {
                            new ServerData(name, "OFFLINE", -1, -1, "", -1, false);
                            SendPluginMessage.ServerIP(player, name);
                            SendPluginMessage.PlayerCount(player, name);
                        }
                    }
                    SendPluginMessage.MaxSlot(null);
                }

                case "MaxSlot" -> {
                    short len = in.readShort();
                    byte[] msgBytes = new byte[len];
                    in.readFully(msgBytes);

                    DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));

                    try {
                        String name = msgIn.readUTF();
                        int slot = msgIn.readShort();
                        boolean isReturn = msgIn.readBoolean();

                        ServerData data = ServerData.get(name);

                        if (data == null) {
                            new ServerData(name, "ONLINE", slot, 0, "", -1, isReturn);
                        }
                        else {
                            data.slot(slot);

                            if (isReturn) {
                                data.received(true);
                            }
                        }

                        if (!isReturn) {
                            SendPluginMessage.MaxSlot(player, name, true);
                        }
                    } catch (IOException ignore) { }
                }

                case "PlayerCount" -> {
                    String name = in.readUTF();
                    int count = in.readInt();
                    Socket s;

                    ServerData data = ServerData.get(name);

                    if (data == null) {
                        new ServerData(name, "OFFLINE", -1, count, "", -1, false);
                    }
                    else {
                        String ip = data.ip();
                        int port = data.port();

                        data.count(count);
                        try {
                            s = new Socket(ip, port);
                            s.close();

                            data.status("ONLINE");
                        } catch (IOException ignore) {
                            data.status("OFFLINE");
                        }
                    }
                }

                case "ServerIP" -> {
                    String name = in.readUTF();
                    String ip = in.readUTF();
                    int port = in.readShort();
                    Socket s;

                    ServerData data = ServerData.get(name);
                    String status;

                    try {
                        s = new Socket(ip, port);
                        s.close();

                        status = "ONLINE";
                    } catch (IOException ignore) {
                        status = "OFFLINE";
                    }
                    if (data == null) {
                        new ServerData(name, status, -1, -1, ip, port, false);
                    }
                    else {
                        data.ip(ip);
                        data.port(port);
                    }
                }
                default -> { }
            }
        }

    }

    public static class DataInit implements Listener {

        @EventHandler
        private void PlayerSpawnLocationEvent(PlayerSpawnLocationEvent event) {
            init(event.getPlayer());
        }

        @EventHandler
        private void ServerLoadEvent(ServerLoadEvent event) {
            if (ServerLoadEvent.LoadType.RELOAD.equals(event.getType())) {
                if (!plugin.getServer().getOnlinePlayers().isEmpty()) {
                    init(null);
                }
            }
        }

        public static void init(Player player) {
            if (DataSent) return;
            DataSent = true;

            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    SendPluginMessage.GetServer(player);
                    SendPluginMessage.GetServers(player);
                });
            }, 1L);
        }
    }

    public static class ServerChange implements Listener {

        @EventHandler
        private void PlayerSpawnLocationEvent(PlayerSpawnLocationEvent event) {
            Player player = event.getPlayer();

            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    SendPluginMessage.MaxSlot(player);

                    ServerData data = ServerData.getServer();
                    if (data != null) {
                        data.count(plugin.getServer().getOnlinePlayers().size());
                    }
                });
            }, 1L);
        }

        @EventHandler
        private void PlayerQuitEvent(PlayerQuitEvent event) {
            ServerData data = ServerData.getServer();
            if (data != null) {
                data.count(plugin.getServer().getOnlinePlayers().size()-1);
            }
        }

    }

    public static class SendPluginMessage {

        public static void GetServer(Player player) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("GetServer");
            (player != null ? player : plugin.getServer()).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }

        public static void GetServers(Player player) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("GetServers");
            (player != null ? player : plugin.getServer()).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }

        public static void MaxSlot(Player player) {
            ServerData.getAll().stream()
                    .filter(data -> !data.equals(ServerData.getServer()))
                    .filter(data -> !data.received())
                    .forEach(data -> MaxSlot(player, data.name(), false));
        }

        public static void MaxSlot(Player player, String target, boolean isReturn) {
            ServerData data = ServerData.getServer();

            if (data == null) return;

            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("Forward");
            out.writeUTF(target);
            out.writeUTF("MaxSlot");

            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);

            try {
                msgOut.writeUTF(data.name());
                msgOut.writeShort(plugin.getServer().getMaxPlayers());
                msgOut.writeBoolean(isReturn);
            } catch (IOException ignored) {
                return;
            }

            out.writeShort(msgBytes.toByteArray().length);
            out.write(msgBytes.toByteArray());
            (player != null ? player : plugin.getServer()).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }

        public static void PlayerCount(Player player, String target) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("PlayerCount");
            out.writeUTF(target);
            (player != null ? player : plugin.getServer()).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }

        public static void ServerIP(Player player, String target) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("ServerIP");
            out.writeUTF(target);
            (player != null ? player : plugin.getServer()).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }
}
