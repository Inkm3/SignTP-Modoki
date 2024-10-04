package com.github.inkm3.signtp.FileEdit;

import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.SignData.SerializeTextData;
import com.github.inkm3.signtp.System.SignTP;
import com.github.inkm3.signtp.Util.Config;
import com.github.inkm3.signtp.Util.SignUtil;
import com.github.inkm3.signtp.Util.Util;
import com.github.inkm3.signtp.Version.Update;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignData {

    private static final Plugin plugin = Main.getPlugin(Main.class);

    private static final HashMap<Location, deserializeSignData> MEMORY = new HashMap<>();

    private static final File SignDataFile = new File(plugin.getDataFolder(), "data/sign.yml");
    private static final YamlConfiguration SignDataConfig = YamlConfiguration.loadConfiguration(SignDataFile);

    @SuppressWarnings("unchecked")
    public static void load() {
        if (!SignDataFile.exists()) plugin.saveResource("data/sign.yml", false);


        try {
            SignDataConfig.load(SignDataFile);
        }  catch (IOException | InvalidConfigurationException ignore) {
            plugin.getLogger().warning("看板の保存データを読み込めませんでした");
            return;
        }

        try {
            List<Map<String, Object>> loadData = (List<Map<String, Object>>) SignDataConfig.getList("data");

            if (loadData != null) {
                loadData.forEach(map -> {
                    if (map.size() == 7) {
                        World world =      (map.get("World") instanceof String str) ? plugin.getServer().getWorld(str) : null;
                        Vector pos =       (map.get("Pos")   instanceof String str) ? Util.strToVec(str) : null;
                        Material matl =    (map.get("Matl")  instanceof String str) ? Material.getMaterial(str) : null;
                        BlockFace face =   (map.get("Face")  instanceof String str) ? BlockFace.valueOf(str) : null;

                        String[] front =   (map.get("FLine") instanceof ArrayList<?> list) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };
                        String[] back =    (map.get("BLine") instanceof ArrayList<?> list) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };
                        String[] display = (map.get("DLine") instanceof ArrayList<?> list) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };

                        create(world, pos, matl, face, front, back, display);
                    }
                });
            }
        } catch (ClassCastException ignore) { }

        Update.updateSaveData.update();
        save();
    }

    public static boolean isSignTP(Location loc) {
        loc.setYaw(0);
        return MEMORY.containsKey(loc.toBlockLocation());
    }

    public static HashSet<deserializeSignData> getAll() {
        return MEMORY.values().stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    public static deserializeSignData get(Location loc) {
        return MEMORY.getOrDefault(loc, null);
    }

    public static deserializeSignData create(org.bukkit.block.Sign sign, String[] front, String[] back, String[] display) {
        return new deserializeSignData(sign, front, back, display);
    }

    public static deserializeSignData create(World world, Vector pos, Material matl, BlockFace face, String[] front, String[] back, String[] display) {
        if (Stream.of(world, pos, matl, face, front, back, display).allMatch(Objects::nonNull)) {
            Location loc = pos.toLocation(world);
            BlockData data = matl.createBlockData();
            switch (data) {
                case WallHangingSign s -> s.setFacing(face);
                case HangingSign s -> s.setRotation(face);
                case WallSign s -> s.setFacing(face);
                case Sign s -> s.setRotation(face);
                default -> {}
            }

            return new deserializeSignData(loc, data, front, back, display);
        }
        return null;
    }

    public static deserializeSignData create(org.bukkit.block.Sign sign) {
        SerializeTextData data = SignUtil.getSignData(sign);

        if (!new SerializeTextData().equals(data)) {
            return new deserializeSignData(sign, data.front, data.back, data.display);
        }
        return null;
    }

    public static void delete(Location location) {
        location.setYaw(0);
        MEMORY.remove(location);

        if (location.getBlock().getState() instanceof org.bukkit.block.Sign sign) {
            SignUtil.removeSignTP(sign);
        }

        if (Config.getDataChangeAutoSave()) {
            save();
        }
    }

    private static void save(SignData.deserializeSignData data) {
        MEMORY.put(data.getLoc(), data);

        if (Config.getDataChangeAutoSave()) {
            save();
        }
    }

    private static void delete(SignData.deserializeSignData data) {
        MEMORY.remove(data.getLoc(), data);

        SignTP.update(0, data, true);
        if (data.getLoc().getBlock().getState() instanceof org.bukkit.block.Sign sign) {
            SignUtil.removeSignTP(sign);
            clear(sign, data);
        }

        if (Config.getDataChangeAutoSave()) {
            save();
        }
    }


    public static void save() {
        if (!SignDataFile.exists()) plugin.saveResource("data/sign.yml", false);

        List<HashMap<String, Object>> saveData = new ArrayList<>();

        MEMORY.values().stream()
                .map(serializeSignData::new)
                .forEach(data -> saveData.add(new LinkedHashMap<>(){{
                    put("Pos",   data.pos);
                    put("World", data.world);
                    put("Matl",  data.matl);
                    put("Face",  data.face);
                    put("FLine", data.front);
                    put("BLine", data.back);
                    put("DLine", data.display);
                }}));

        SignDataConfig.set("data", saveData);

        try {
            SignDataConfig.save(SignDataFile);
        } catch (IOException ignore) {
            plugin.getLogger().warning("看板の保存データを保存できませんでした");
        }
    }

    private static void clear(org.bukkit.block.Sign sign, deserializeSignData data) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if ("[SignTP]".equalsIgnoreCase(data.getFront()[0])) lineClear(sign, true);
            if ("[SignTP]".equalsIgnoreCase(data.getBack()[0])) lineClear(sign, false);
            sign.update();
        }, 0L);
    }

    private static void lineClear(org.bukkit.block.Sign sign, boolean front) {
        for (int i = 0; i < 4; i++) {
            sign.getSide(front ? Side.FRONT : Side.BACK).line(i, Component.text(""));
        }
    }

    public static class deserializeSignData {
        private final Location loc;
        private BlockData data;
        private String[] front, back, display;
        private final HashSet<ConnectingPlayer> player = new HashSet<>();

        private deserializeSignData(@NotNull org.bukkit.block.Sign sign, String[] front, String[] back, String[] display) {
            this.loc = sign.getLocation().toBlockLocation();
            this.data = sign.getBlockData();
            this.front = front;
            this.back = back;
            this.display = display;
            save(this);
            setSignData();
        }

        private deserializeSignData(Location loc, BlockData data, String[] front, String[] back, String[] display) {
            this.loc = loc.toBlockLocation();
            this.data = data;
            this.front = front;
            this.back = back;
            this.display = display;
            save(this);
            setSignData();
        }

        public Location getLoc() {
            return this.loc;
        }

        public BlockData getData() {
            return this.data;
        }

        public String[] getFront() {
            return this.front;
        }

        public String[] getBack() {
            return this.back;
        }

        public String[] getDisplay() {
            return this.display;
        }

        public HashSet<ConnectingPlayer> getPlayer() {
            return this.player;
        }

        public void setData(BlockData data) {
            if (this.data != data) {
                this.data = data;
                save(this);
            }
        }

        public void setFront(String[] front) {
            if (this.front.length == front.length && this.front.length == 4) {
                if (!Arrays.equals(this.front, front)) {
                    this.front = front;
                    save(this);
                    setSignData();
                }
            }
        }

        public void setBack(String[] back) {
            if (this.back.length == back.length && this.back.length == 4) {
                if (!Arrays.equals(this.back, back)) {
                    this.back = back;
                    save(this);
                    setSignData();
                }
            }
        }

        public void setDisplay(String[] display) {
            if (this.display.length == display.length && this.display.length == 4) {
                if (!Arrays.equals(this.display, display)) {
                    this.display = display;
                    save(this);
                    setSignData();
                }
            }
        }

        public void addPlayer(Player player, Side side) {
            this.player.add(new ConnectingPlayer(player, side));
        }

        public void removePlayer(Player player) {
            this.player.removeIf(v -> v.match(player));
            SignUtil.reloadSignText(player, this.getLoc());
        }

        private void setSignData() {
            if (this.loc.getBlock().getState() instanceof org.bukkit.block.Sign sign) {
                SignUtil.setSignData(sign);
                SignTP.update(1, this, false);
            }
        }

        public void delete() {
            SignData.delete(this);
        }

        public record ConnectingPlayer(Player player, Side side) {
            public boolean match(Player player) {
                return this.player.equals(player);
            }
        }
    }

    private static class serializeSignData {
        private final String pos;
        private final String world;
        private final String matl;
        private final String face;
        private final String[] front;
        private final String[] back;
        private final String[] display;

        private serializeSignData(deserializeSignData data) {
            this(data.loc, data.data, data.front, data.back, data.display);
        }

        private serializeSignData(Location loc, BlockData data, String[] front, String[] back, String[] display) {
            this.pos = loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
            this.world = loc.getWorld().getName();
            this.matl = data.getMaterial().name();
            this.face = SignUtil.getSignFace(data).name();
            this.front = front;
            this.back = back;
            this.display = display;
        }
    }
}
