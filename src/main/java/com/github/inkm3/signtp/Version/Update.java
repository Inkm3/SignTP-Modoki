package com.github.inkm3.signtp.Version;

import com.github.inkm3.signtp.FileEdit.SignData;
import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.Util.Util;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Update {


    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final File FOLDER = plugin.getDataFolder();


    private static final File OldSaveDataFile1 = new File(FOLDER, "signdata.yml");
    private static final File OldSaveDataFile2 = new File(FOLDER, "sign.yml");

    private static final YamlConfiguration OldSaveDataConfig1 = YamlConfiguration.loadConfiguration(OldSaveDataFile1);
    private static final YamlConfiguration OldSaveDataConfig2 = YamlConfiguration.loadConfiguration(OldSaveDataFile2);


    public static class updateSaveData {

        public static void update() {
            if (OldSaveDataFile1.exists() && !OldSaveDataConfig1.getBoolean("Updated", false)) {
                updateSaveData1();
            }

            if (OldSaveDataFile2.exists() && !OldSaveDataConfig2.getBoolean("Updated", false)) {
                updateSaveData2();
            }

        }

        @SuppressWarnings("unchecked")
        private static void updateSaveData1() {
            OldSaveDataConfig1.set("Updated", true);
            try {
                OldSaveDataConfig1.save(OldSaveDataFile1);
            } catch (IOException ignore) { };

            plugin.getLogger().info("前バージョンのセーブデータが見つかったため、現在のセーブデータに追加します。");
            AtomicInteger count = new AtomicInteger(0);
            try {
                List<Map<String, String>> oldSaveData = (List<Map<String, String>>) OldSaveDataConfig1.getList("SignData.Datas");

                if (oldSaveData != null) {
                    oldSaveData.forEach(data -> {
                        World world =    (data.get("World") instanceof String str) ? plugin.getServer().getWorld(str) : null;
                        Vector pos =     (data.get("Pos")   instanceof String str) ? Util.strToVec(str) : null;
                        Material matl =  (data.get("Type")  instanceof String str) ? Material.getMaterial(str) : null;
                        BlockFace face = (data.get("Rot")   instanceof String str) ? BlockFace.valueOf(str) : null;

                        String[] front = (data.get("FTxt").split(",", 4) instanceof String[] array && array.length == 4) ? array : new String[]{ "", "", "", "" };
                        String[] back =  (data.get("BTxt").split(",", 4) instanceof String[] array && array.length == 4) ? array : new String[]{ "", "", "", "" };

                        if ("[Sign TP]".equalsIgnoreCase(front[0])) front[0] = front[0].replaceFirst(" ", "");
                        if ("[Sign TP]".equalsIgnoreCase(back[0])) back[0] = back[0].replaceFirst(" ", "");

                        SignData.deserializeSignData result = SignData.create(world, pos, matl, face, front, back, new String[]{ "", "", "", "" });
                        if (result != null) count.getAndIncrement();
                    });
                }

                if (count.get() > 0) {
                    plugin.getLogger().info(count.get()+"個のデータを追加しました。");
                }
                else {
                    plugin.getLogger().info("追加できるデータがありませんでした。");
                }

            } catch (ClassCastException ignore) {
                plugin.getLogger().warning("セーブデータの読み込み中、エラーが発生したため追加できませんでした。");
            }
        }

        @SuppressWarnings("unchecked")
        private static void updateSaveData2() {
            OldSaveDataConfig2.set("Updated", true);
            try {
                OldSaveDataConfig2.save(OldSaveDataFile2);
            } catch (IOException ignore) { };

            plugin.getLogger().info("前バージョンのセーブデータが見つかったため、現在のセーブデータに追加します。");

            try {
                AtomicInteger count = new AtomicInteger(0);
                List<Map<String, Object>> oldSaveData = (List<Map<String, Object>>) OldSaveDataConfig2.getList("data");

                if (oldSaveData != null) {
                    oldSaveData.forEach(data -> {
                        World world =    (data.get("World")  instanceof String str) ? plugin.getServer().getWorld(str) : null;
                        Vector pos =     (data.get("Loc")    instanceof String str) ? Util.strToVec(str) : null;
                        String wood =    (data.get("Type")   instanceof String str) ? str : "";
                        String attach =  (data.get("Attach") instanceof String str) ? str : "";
                        BlockFace face = (data.get("Dir")    instanceof String str) ? BlockFace.valueOf(str) : null;
                        Material matl = Material.getMaterial(wood + switch (attach) {
                            case "GROUND"-> "_SIGN";
                            case "WALL"->   "_WALL_SIGN";
                            case "NONE"->   "_HANGING_SIGN";
                            case "CEILING"->"_WALL_HANGING_SIGN";
                            default -> "";
                        });

                        String[] front =   (data.get("FRONT")   instanceof ArrayList<?> list && list.size() == 4) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };
                        String[] back =    (data.get("BACK")    instanceof ArrayList<?> list && list.size() == 4) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };
                        String[] display = (data.get("Display") instanceof ArrayList<?> list && list.size() == 4) ? list.stream().map(Object::toString).toArray(String[]::new) : new String[]{ "", "", "", "" };

                        SignData.deserializeSignData result = SignData.create(world, pos, matl, face, front, back, display);
                        if (result != null) count.getAndIncrement();
                    });
                }

                if (count.get() > 0) {
                    plugin.getLogger().info(count.get()+"個のデータを追加しました。");
                }
                else {
                    plugin.getLogger().info("追加できるデータがありませんでした。");
                }
            } catch (ClassCastException ignore) {
                plugin.getLogger().warning("セーブデータの読み込み中、エラーが発生したため追加できませんでした。");
            }
        }
    }




}
