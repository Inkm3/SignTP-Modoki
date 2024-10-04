package com.github.inkm3.signtp.Commands;

import com.github.inkm3.signtp.FileEdit.ServerData;
import com.github.inkm3.signtp.FileEdit.SignData;
import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.SignData.SerializeTextData;
import com.github.inkm3.signtp.System.SignTP;
import com.github.inkm3.signtp.Util.Config;
import com.github.inkm3.signtp.Util.SignUtil;
import dev.jorel.commandapi.*;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

public class SignTPCommand {

    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final MiniMessage minimessage = MiniMessage.miniMessage();

    private static final String[] EMPTY_LINE = new String[]{ "", "", "", "" };

    public static void load() {

        CommandAPICommand cmd = new CommandAPICommand("signtp");

        cmd.withPermission("signtp.command.*");
        cmd.withFullDescription("signtpのコマンド");
        cmd.withSubcommands(
                GiveCommand(),
                SignCommand(),
                ConfigCommand()
        );
        cmd.register();
    }


    private static Component getValue(Object value) {
        return minimessage.deserialize(value + " に設定されています");
    }

    private static Component setValue(Object value) {
        return minimessage.deserialize(value + " に設定しました");
    }

    private static CommandAPICommand GiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("signtp.command.give")
                .withArguments(new MultiLiteralArgument("sign",
                        "oak_sign", "oak_hanging_sign",
                        "spruce_sign",      "spruce_hanging_sign",
                        "birch_sign",       "birch_hanging_sign",
                        "jungle_sign",      "jungle_hanging_sign",
                        "acacia_sign",      "acacia_hanging_sign",
                        "dark_oak_sign",    "dark_oak_hanging_sign",
                        "mangrove_sign",    "mangrove_hanging_sign",
                        "cherry_sign",      "cherry_hanging_sign",
                        "bamboo_sign",      "bamboo_hanging_sign",
                        "crimson_sign",     "crimson_hanging_sign",
                        "warped_sign",      "warped_hanging_sign"))
                .withArguments(new MultiLiteralArgument("side", "Front", "Back", "Both"))
                .withArguments(new StringArgument("server"))
                .withOptionalArguments(new TextArgument("template"))
                .withOptionalArguments(new GreedyStringArgument("title"))
                .executesPlayer((player, args) -> {
                    Material signType = Material.getMaterial(args.getUnchecked("sign").toString().toUpperCase());
                    if (signType == null || !(signType.createBlockData().createBlockState() instanceof Sign sign)) {
                        throw CommandAPI.failWithString("内部エラーが発生しました");
                    }

                    ItemStack item = new ItemStack(signType);
                    String side = args.getUnchecked("side");
                    String server = args.getUnchecked("server");
                    String title = (String) args.getOrDefault("title", "");
                    String template = (String) args.getOrDefault("template", "");

                    String[] front = new String[]{ "", "", "", "" };
                    String[] back = new String[]{ "", "", "", "" };
                    if ("Front".equals(side) || "Both".equals(side)) {
                        front[0] = "[SignTP]";
                        front[1] = server;
                        front[2] = title;
                        front[3] = template;
                    }
                    if ("Back".equals(side) || "Both".equals(side)) {
                        back[0] = "[SignTP]";
                        back[1] = server;
                        back[2] = title;
                        back[3] = template;
                    }

                    net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                    CompoundTag signTag = ((CraftBlockEntityState<?>) sign).getSnapshotNBTWithoutComponents();
                    signTag = SignUtil.setSignData(signTag, front, back, new String[]{ "", "", "", "" });

                    nmsItem.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(signTag));
                    player.getInventory().addItem(nmsItem.getBukkitStack());
                });
    }

    private static CommandAPICommand SignCommand() {
        return new CommandAPICommand("sign")
                .withPermission("signtp.command.sign.*")
                .withSubcommands(
                        SignCommands.load,
                        SignCommands.block,
                        SignCommands.item
                );
    }

    static class SignCommands {
        public static final CommandAPICommand load = new CommandAPICommand("load")
                .withPermission("signtp.command.sign.load")
                .withOptionalArguments(new BooleanArgument("force"))
                .executesPlayer((player, args) -> {
                   boolean force = Boolean.TRUE.equals(args.getOrDefault("force", false));

                   SignTP.update(force);
                   if (force) {
                       player.sendMessage(Component.text("設定済み看板を強制的に読み込みました"));
                   }
                   else {
                       player.sendMessage(Component.text("設定済み看板を読み込みました"));
                   }
                });

        public static final CommandAPICommand block = new CommandAPICommand("block")
                .withPermission("signtp.command.sign.block.*")
                .withSubcommands(
                        BlockCommands.front,
                        BlockCommands.back,
                        BlockCommands.display
                );

        public static final CommandAPICommand item = new CommandAPICommand("item")
                .withPermission("signtp.command.sign.item.*")
                .withSubcommands(
                        ItemCommands.front,
                        ItemCommands.back,
                        ItemCommands.display
                );

        static class ItemCommands {

            public static final CommandAPICommand front = new CommandAPICommand("front")
                    .withPermission("signtp.command.sign.item.front")
                    .withArguments(
                            LineArgument(Type.FRONT, 1),
                            LineArgument(Type.FRONT, 2),
                            LineArgument(Type.FRONT, 3),
                            LineArgument(Type.FRONT, 4)
                    )
                    .executesPlayer((player, args) -> {
                        setSideText(player, args, true);
                    });

            public static final CommandAPICommand back = new CommandAPICommand("back")
                    .withPermission("signtp.command.sign.item.back")
                    .withArguments(
                            LineArgument(Type.BACK, 1),
                            LineArgument(Type.BACK, 2),
                            LineArgument(Type.BACK, 3),
                            LineArgument(Type.BACK, 4)
                    )
                    .executesPlayer((player, args) -> {
                        setSideText(player, args, false);
                    });

            private static void setSideText(Player player, CommandArguments args, boolean front) throws WrapperCommandSyntaxException {
                String line1 = args.getUnchecked("line1");
                String line2 = args.getUnchecked("line2");
                String line3 = args.getUnchecked("line3");
                String line4 = args.getUnchecked("line4");
                String[] lines = new String[]{ line1, line2, line3, line4 };
                ItemStack item = getSignItem(player.getInventory());

                if (Stream.of(line1, line2, line3, line4).anyMatch(Objects::isNull)) {
                    throw CommandAPI.failWithString("内部エラーが発生しました。");
                }
                if (item == null || !(item.getType().createBlockData().createBlockState() instanceof Sign sign)) {
                    throw CommandAPI.failWithString("看板をメインハンドかオフハンドに持ってから実行してください。");
                }
                if (!"[SignTP]".equalsIgnoreCase(line1) && !line1.isEmpty()) {
                    throw CommandAPI.failWithString("line1の設定を'[SignTP]'もしくは空白に設定してください。");
                }
                else if (line2.isBlank()) {
                    throw CommandAPI.failWithString("line2にサーバー名を設定してください。");
                }

                net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                SerializeTextData data = SignUtil.getSignData(nmsItem.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag());

                CompoundTag signTag = nmsItem.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(((CraftBlockEntityState<?>) sign).getSnapshotNBTWithoutComponents())).copyTag();
                CompoundTag newTag = front ? SignUtil.setSignData(signTag, lines, data.back, data.display) : SignUtil.setSignData(signTag, data.front, lines, data.display);

                String[] text = front ? data.front : data.back;
                String type = front ? "front" : "back";

                if (line1.isBlank()) {
                    if (!"[SignTP]".equalsIgnoreCase(text[0])) {
                        player.sendMessage(Component.text("所持している看板の"+type+"データを削除しました。"));
                    }
                    else {
                        throw CommandAPI.failWithString("所持している看板の"+type+"データは未設定です。");
                    }
                }
                else {
                    if (!new SerializeTextData().equals(data)) {
                        player.sendMessage(Component.text(""+
                                "所持している看板の"+type+"データを以下に変更しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));
                    }
                    else {
                        player.sendMessage(Component.text(""+
                                "所持している看板の"+type+"データを以下の設定で作成しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));
                    }
                }

                nmsItem.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(newTag));
                item.setItemMeta(nmsItem.getBukkitStack().getItemMeta());
            }

            public static final CommandAPICommand display = new CommandAPICommand("display")
                    .withPermission("signtp.command.sign.item.display")
                    .withArguments(
                            LineArgument(Type.DISPLAY, 1),
                            LineArgument(Type.DISPLAY, 2),
                            LineArgument(Type.DISPLAY, 3),
                            LineArgument(Type.DISPLAY, 4)
                    )
                    .executesPlayer((player, args) -> {
                        String line1 = args.getUnchecked("line1");
                        String line2 = args.getUnchecked("line2");
                        String line3 = args.getUnchecked("line3");
                        String line4 = args.getUnchecked("line4");
                        String[] lines = new String[]{ line1, line2, line3, line4 };
                        ItemStack item = getSignTPItem(player.getInventory());

                        if (Stream.of(line1, line2, line3, line4).anyMatch(Objects::isNull)) {
                            throw CommandAPI.failWithString("内部エラーが発生しました");
                        }
                        if (item == null || !(item.getType().createBlockData().createBlockState() instanceof Sign sign)) {
                            throw CommandAPI.failWithString("設定済みの看板をメインハンドかオフハンドに持ってから実行してください。");
                        }

                        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                        SerializeTextData data = SignUtil.getSignData(nmsItem.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag());

                        CompoundTag signTag = nmsItem.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(((CraftBlockEntityState<?>) sign).getSnapshotNBTWithoutComponents())).copyTag();
                        CompoundTag newTag = SignUtil.setSignData(signTag, data.front, data.back, lines);

                        player.sendMessage(Component.text(""+
                                "所持している看板のdisplayデータを以下に変更しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));

                        nmsItem.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(newTag));
                        item.setItemMeta(nmsItem.getBukkitStack().getItemMeta());
                    });

            public static Argument<String> LineArgument(Type type, int line) {
                return new TextArgument("line" + line)
                        .includeSuggestions(ArgumentSuggestions.strings(info -> getLine(info, type, line-1)));
            }


            public static String[] getLine(SuggestionInfo<CommandSender> info, Type type, int index) {
                if (info.sender() instanceof Player player) {
                    ItemStack item = getSignTPItem(player.getInventory());
                    if (item != null && CraftItemStack.asNMSCopy(item).get(DataComponents.BLOCK_ENTITY_DATA) instanceof CustomData nbtData) {
                        SerializeTextData data =  SignUtil.getSignData(nbtData.copyTag());

                        String[] text = switch (type) {
                            case FRONT -> data.front;
                            case BACK -> data.back;
                            case DISPLAY -> data.display;
                        };

                        if (Type.DISPLAY.equals(type) || "[SignTP]".equalsIgnoreCase(text[0]) && !text[1].isBlank()) {
                            return new String[]{ "\""+text[index]+"\"" };
                        }
                    }
                }

                if (Type.DISPLAY.equals(type)) {
                    return new String[]{ "\"\""};
                }
                else {
                    return switch (index) {
                        case 0 -> new String[]{ "\"[SignTP]\"" };
                        case 1 -> {
                            HashSet<ServerData> allData = ServerData.getAll();
                            if (allData.isEmpty()) yield new String[]{ "\"\"" };
                            yield allData.stream().map(data -> "\"" + data.name() + "\"").toArray(String[]::new);
                        }
                        default -> new String[]{ "\"\"" };
                    };
                }
            }
        }

        static class BlockCommands {

            public static final CommandAPICommand front = new CommandAPICommand("front")
                    .withPermission("signtp.command.sign.block.front")
                    .withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
                    .withArguments(
                            LineArgument(Type.FRONT, 1),
                            LineArgument(Type.FRONT, 2),
                            LineArgument(Type.FRONT, 3),
                            LineArgument(Type.FRONT, 4))
                    .executesNative(info -> {
                        setSideText(info.sender(), info.args(), true);
                    });

            public static final CommandAPICommand back = new CommandAPICommand("back")
                    .withPermission("signtp.command.sign.block.back")
                    .withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
                    .withArguments(
                            LineArgument(Type.BACK, 1),
                            LineArgument(Type.BACK, 2),
                            LineArgument(Type.BACK, 3),
                            LineArgument(Type.BACK, 4))
                    .executesNative(info -> {
                        setSideText(info.sender(), info.args(), false);
                    });

            private static void setSideText(NativeProxyCommandSender sender, CommandArguments args, boolean front) throws WrapperCommandSyntaxException {
                String line1 = args.getUnchecked("line1");
                String line2 = args.getUnchecked("line2");
                String line3 = args.getUnchecked("line3");
                String line4 = args.getUnchecked("line4");
                String[] lines = new String[]{ line1, line2, line3, line4 };
                Location location = args.getUnchecked("location");
                if (sender.getWorld() != null) location.setWorld(sender.getWorld());

                if (Stream.of(line1, line2, line3, line4, location).anyMatch(Objects::isNull)) {
                    throw CommandAPI.failWithString("内部エラーが発生しました");
                }
                if (!(location.getBlock().getState() instanceof Sign sign)) {
                    throw CommandAPI.failWithString("指定された座標は看板ではありません。");
                }
                if (!"[SignTP]".equalsIgnoreCase(line1) && !line1.isEmpty()) {
                    throw CommandAPI.failWithString("line1の設定を'[SignTP]'もしくは空白に設定してください。");
                }
                else if (line2.isBlank()) {
                    throw CommandAPI.failWithString("line2にサーバー名を設定してください。");
                }

                SignData.deserializeSignData data = SignData.get(location);
                String type = front ? "front" : "back";

                if (line1.isBlank()) {
                    if (data != null) {
                        if ("[SignTP]".equalsIgnoreCase((front ? data.getBack() : data.getFront())[0])) {
                            sender.sendMessage("座標: " + location.toVector() + " の" + type + "データを削除しました。");
                            if (front) data.setFront(EMPTY_LINE);
                            else data.setDisplay(EMPTY_LINE);
                        }
                        else {
                            sender.sendMessage("座標: " + location.toVector() + " のデータを全て削除しました。");
                            data.delete();
                        }
                    }
                    else {
                        throw CommandAPI.failWithString("座標: " + location.toVector() + " の看板は未設定です。");
                    }
                }
                else {
                    if (data != null) {
                        if (front) data.setFront(lines);
                        else data.setBack(lines);
                        sender.sendMessage(Component.text("" +
                                "座標: " + location.toVector() + " の" + type + "データを以下に変更しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));
                    }
                    else {
                        if (front) SignData.create(sign, lines, EMPTY_LINE, EMPTY_LINE);
                        else SignData.create(sign, EMPTY_LINE, lines, EMPTY_LINE);
                        sender.sendMessage(Component.text("" +
                                "座標: " + location.toVector() + " の" + type + "データを以下の設定で作成しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));
                    }
                }
            }

            public static final CommandAPICommand display = new CommandAPICommand("display")
                    .withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
                    .withPermission("signtp.command.sign.block.display")
                    .withArguments(
                            LineArgument(Type.DISPLAY, 1),
                            LineArgument(Type.DISPLAY, 2),
                            LineArgument(Type.DISPLAY, 3),
                            LineArgument(Type.DISPLAY, 4))
                    .executesNative((sender, args) -> {
                        String line1 = args.getUnchecked("line1");
                        String line2 = args.getUnchecked("line2");
                        String line3 = args.getUnchecked("line3");
                        String line4 = args.getUnchecked("line4");
                        Location location = args.getUnchecked("location");
                        if (sender.getWorld() != null) location.setWorld(sender.getWorld());

                        if (Stream.of(line1, line2, line3, line4, location).anyMatch(Objects::isNull)) {
                            throw CommandAPI.failWithString("内部エラーが発生しました");
                        }
                        if (!(location.getBlock().getState() instanceof Sign sign)) {
                            throw CommandAPI.failWithString("指定された座標は看板ではありません。");
                        }
                        if (!(SignData.isSignTP(location) && SignUtil.isSignTP(sign))) {
                            throw CommandAPI.failWithString("指定された看板は未設定です。");
                        }

                        SignData.get(location).setDisplay(new String[]{ line1, line2, line3, line4 });
                        sender.sendMessage(Component.text(""+
                                "座標: " + location.toVector() + " のdisplayデータを以下に変更しました。\n" +
                                "LINE1 : \"" + line1 + "\"\n" +
                                "LINE2 : \"" + line2 + "\"\n" +
                                "LINE3 : \"" + line3 + "\"\n" +
                                "LINE4 : \"" + line4 + "\""
                        ));
                    });

            public static Argument<String> LineArgument(Type type, int line) {
                return new TextArgument("line" + line)
                        .includeSuggestions(ArgumentSuggestions.strings(info -> getLine(info, type, line-1)));
            }

            public static String[] getLine(SuggestionInfo<CommandSender> info, Type type, int index) {
                Location location = info.previousArgs().getUnchecked("location");
                if (location != null && SignData.isSignTP(location)) {
                    SignData.deserializeSignData data = SignData.get(location);
                    String[] text = switch (type) {
                        case FRONT -> data.getFront();
                        case BACK -> data.getBack();
                        case DISPLAY -> data.getDisplay();
                    };
                    if (Type.DISPLAY.equals(type) || "[SignTP]".equalsIgnoreCase(text[0]) && !text[1].isBlank()) {
                        return new String[]{ "\""+text[index]+"\"" };
                    }
                }
                if (Type.DISPLAY.equals(type)) {
                    return new String[]{ "\"\"" };
                }
                else {
                    return switch (index) {
                        case 0 -> new String[]{ "\"[SignTP]\"" };
                        case 1 -> {
                            HashSet<ServerData> allData = ServerData.getAll();
                            if (allData.isEmpty()) yield new String[]{ "\"\"" };
                            yield allData.stream().map(data -> "\"" + data.name() + "\"").toArray(String[]::new);
                        }
                        default -> new String[]{ "\"\"" };
                    };
                }
            }

        }


        enum Type {
            FRONT,
            BACK,
            DISPLAY
        }
    }

    private static CommandAPICommand ConfigCommand() {
        return new CommandAPICommand("config")
                .withPermission("signtp.command.config.*")
                .withSubcommands(
                        ConfigCommands.ReloadCommand(),
                        ConfigCommands.ValueCommand()
                );
    }

    static class ConfigCommands {

        public static CommandAPICommand ReloadCommand() {
            return new CommandAPICommand("reload")
                    .withPermission("signtp.command.config.reload")
                    .executesPlayer((player, args) -> {
                        Config.reloadConfig();
                        player.sendMessage(MiniMessage.miniMessage().deserialize("設定ファイルを再読み込みしました"));
                    });
        }

        public static CommandAPICommand ValueCommand() {
            return new CommandAPICommand("value")
                    .withPermission("signtp.command.config.value")
                    .withSubcommands(
                            ValueCommand.UpdateInterval,
                            ValueCommand.RequireChunkLoaded,
                            ValueCommand.DataChangeAutoSave,
                            ValueCommand.UnknownValuePlaceholder
                    );
        }

        static class ValueCommand {
            public static final CommandAPICommand UpdateInterval = new CommandAPICommand("UpdateInterval")
                    .withOptionalArguments(new IntegerArgument("value", -60, 60))
                    .executesPlayer((player, args) -> {
                       Integer value = args.getUnchecked("value");
                       int nowValue = Config.getUpdateInterval();

                       if (value == null || value == nowValue) {
                           player.sendMessage(getValue(nowValue));
                       }
                       else {
                           Config.setUpdateInterval(value);
                           SignTP.setInterval(value);
                           player.sendMessage(setValue(value));
                       }
                    });

            public static final CommandAPICommand RequireChunkLoaded = new CommandAPICommand("RequireChunkLoaded")
                    .withOptionalArguments(new BooleanArgument("value"))
                    .executesPlayer((player, args) -> {
                        Boolean value = args.getUnchecked("value");
                        boolean nowValue = Config.getRequireChunkLoaded();

                        if (value == null || value == nowValue) {
                            player.sendMessage(getValue(nowValue));
                        }
                        else {
                            Config.setRequireChunkLoaded(value);
                            player.sendMessage(setValue(value));
                        }
                    });

            public static final CommandAPICommand DataChangeAutoSave = new CommandAPICommand("DataChangeAutoSave")
                    .withOptionalArguments(new BooleanArgument("value"))
                    .executesPlayer((player, args) -> {
                        Boolean value = args.getUnchecked("value");
                        boolean nowValue = Config.getDataChangeAutoSave();

                        if (value == null || value == nowValue) {
                            player.sendMessage(getValue(nowValue));
                        }
                        else {
                            Config.setDataChangeAutoSave(value);
                            player.sendMessage(setValue(value));
                        }
                    });

            public static final CommandAPICommand UnknownValuePlaceholder = new CommandAPICommand("UnknownValuePlaceholder")
                    .withOptionalArguments(new GreedyStringArgument("value"))
                    .executesPlayer((player, args) -> {
                        String value = args.getUnchecked("value");
                        String nowValue = Config.getUnknownValuePlaceholder();

                        if (value == null || nowValue.equals(value)) {
                            player.sendMessage(getValue(nowValue));
                        }
                        else {
                            Config.setUnknownValuePlaceholder(value);
                            player.sendMessage(setValue(value));
                        }
                    });
        }
    }

    public static ItemStack getSignTPItem(PlayerInventory inventory) {
        ItemStack item = null;

        item = getSignTPItem(inventory.getItemInOffHand(), item);
        item = getSignTPItem(inventory.getItemInMainHand(), item);

        return item;
    }

    public static ItemStack getSignItem(PlayerInventory inventory) {
        ItemStack mainItem = inventory.getItemInMainHand();
        ItemStack offItem = inventory.getItemInOffHand();

        return  (mainItem.getType().isBlock() && mainItem.getType().createBlockData().createBlockState() instanceof Sign) ? mainItem :
                (offItem.getType().isBlock() && offItem.getType().createBlockData().createBlockState() instanceof Sign) ? offItem : null;
    }

    private static ItemStack getSignTPItem(ItemStack item, ItemStack def) {
        if (item.getType().createBlockData().createBlockState() instanceof  Sign) {
            if (CraftItemStack.asNMSCopy(item).get(DataComponents.BLOCK_ENTITY_DATA) instanceof CustomData data) {
                if (SignUtil.isSignTP(data.copyTag())) {
                    return item;
                }
            }
        }
        return def;
    }
}
