package com.github.inkm3.signtp.System;

import com.github.inkm3.signtp.Events.ServerEvents;
import com.github.inkm3.signtp.FileEdit.ServerData;
import com.github.inkm3.signtp.FileEdit.SignData;
import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.Util.Config;
import com.github.inkm3.signtp.Util.SignText;
import com.github.inkm3.signtp.Util.SignUtil;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SignTP {

    private static final Plugin plugin = Main.getPlugin(Main.class);

    private static int interval = 2;
    private static boolean cancel = false;
    private static boolean stating = false;

    public static void setInterval(int interval) {
        SignTP.interval = interval;
        SignTP.cancel = true;
    }

    public static void startDataSyncLoop() {
        if (stating) return;
        stating = true;
        serverInfoUpdate();
    }

    private static void serverInfoUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cancel) {
                    cancel();
                    cancel = false;
                    serverInfoUpdate();
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ServerData.getAll().forEach(data -> ServerEvents.SendPluginMessage.PlayerCount(null, data.name()));
                });
            }
        }.runTaskTimerAsynchronously(plugin, 0, interval * 20L);
    }

    public static void update(boolean force) {
        Set<SignData.deserializeSignData> allData = SignData.getAll();
        allData.forEach(data -> {
            List<Side> sides = new ArrayList<>();
            if ("[SignTP]".equalsIgnoreCase(data.getFront()[0])) sides.add(Side.FRONT);
            if ("[SignTP]".equalsIgnoreCase(data.getBack()[0])) sides.add(Side.BACK);
            if (!sides.isEmpty()) update(0, data, sides, force);
        });
    }

    public static void update(String name, boolean force) {
        Set<SignData.deserializeSignData> allData = SignData.getAll();

        allData.forEach(data -> {
            List<Side> sides = new ArrayList<>();
            if (name.equals(data.getFront()[1])) sides.add(Side.FRONT);
            if (name.equals(data.getBack()[1])) sides.add(Side.BACK);
            if (!sides.isEmpty()) update(0, data, sides, force);
        });
    }

    public static void update(int delay, SignData.deserializeSignData data, boolean force) {
        List<Side> sides = new ArrayList<>();
        if ("[SignTP]".equalsIgnoreCase(data.getFront()[0])) sides.add(Side.FRONT);
        if ("[SignTP]".equalsIgnoreCase(data.getBack()[0])) sides.add(Side.BACK);
        update(delay, data, sides, force);
    }

    public static void update(int delay, SignData.deserializeSignData data, List<Side> sides, boolean force) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            Location loc = data.getLoc();
            BlockData blockData = data.getData();

            boolean requireChunkLoaded = Config.getRequireChunkLoaded();
            boolean chunkLoaded = loc.getWorld() != null && loc.isChunkLoaded();

            if ((requireChunkLoaded && !chunkLoaded) && !force) return;

            if (loc.getWorld() == null) {
                SignData.delete(loc);
                return;
            }

            if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign sign)) {
                SignData.delete(loc);
                return;
            }

            if (!(blockData.getMaterial().equals(sign.getType()))) {
                SignData.delete(loc);
                return;
            }

            if (!blockData.getMaterial().equals(loc.getBlock().getType())) {
                SignData.delete(loc);
                return;
            }

            if (switch (blockData) {
                case WallHangingSign s -> !(loc.getBlock().getBlockData() instanceof WallHangingSign s2 && s2.getFacing().equals(s.getFacing()));
                case HangingSign s -> !(loc.getBlock().getBlockData() instanceof HangingSign s2 && s2.getRotation().equals(s.getRotation()));
                case WallSign s -> !(loc.getBlock().getBlockData() instanceof WallSign s2 && s2.getFacing().equals(s.getFacing()));
                case Sign s -> !(loc.getBlock().getBlockData() instanceof Sign s2 && s2.getRotation().equals(s.getRotation()));
                default -> true;
            } ) {
                SignData.delete(loc);
                return;
            }

            String[] front = data.getFront();
            String[] back = data.getBack();
            String[] display = data.getDisplay();

            for (Side side : Side.values()) {
                if (!sides.contains(side)) {
                    continue;
                }

                if (switch (side) {
                    case FRONT -> !"[SignTP]".equalsIgnoreCase(front[0]);
                    case BACK -> !"[SignTP]".equalsIgnoreCase(back[0]);
                }) {
                    continue;
                }

                List<Component> text = switch (side) {
                    case FRONT -> SignText.getText(front, display);
                    case BACK -> SignText.getText(back, display);
                };

                for (int i = 0; i < 4; i++) {
                    if (!MiniMessage.miniMessage().serialize(text.get(i)).isEmpty()) {
                        sign.getSide(side).line(i, text.get(i));
                    }
                }
            }
            sign.update(false, false);
            data.getPlayer().forEach(connData -> {
                SignUtil.sendSignText(connData.player(), sign.getLocation(), connData.side(), Side.FRONT.equals(connData.side()) ? front : back);
            });
        }, delay);
    }
}
