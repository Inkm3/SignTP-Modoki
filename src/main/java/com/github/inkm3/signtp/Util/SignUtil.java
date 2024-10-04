package com.github.inkm3.signtp.Util;

import com.github.inkm3.signtp.FileEdit.SignData;
import com.github.inkm3.signtp.Main;
import com.github.inkm3.signtp.SignData.SerializeTextData;
import com.github.inkm3.signtp.SignData.SignDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


public class SignUtil {



    private static final Plugin plugin = Main.getPlugin(Main.class);

//    private static final NamespacedKey SignTPKey = new NamespacedKey(plugin, "SignTPKey");
//    private static final NamespacedKey SignTextKey = new NamespacedKey(plugin, "SignTextKey");

    private static final String SIGN_TEXT_KEY = "signtp:signtextkey";
    private static final String BUKKIT_TAG = "PublicBukkitValues";

    public static BlockFace getSignFace(BlockData data) {
        return switch (data) {
            case WallHangingSign sign -> sign.getFacing();
            case HangingSign sign -> sign.getRotation();
            case WallSign sign -> sign.getFacing();
            case Sign sign -> sign.getRotation();
            default -> BlockFace.WEST;
        };
    }

    public static void removeSignTP(org.bukkit.block.Sign sign) {
        CompoundTag signTag = ((CraftBlockEntityState<?>) sign).getSnapshotNBTWithoutComponents();
        ((CraftBlockEntityState<?>) sign).loadData(removeSignTP(signTag));
        sign.update();
    }

    public static CompoundTag removeSignTP(CompoundTag signTag) {
        if (signTag.contains(BUKKIT_TAG)) {
            signTag.getCompound(BUKKIT_TAG).remove(SIGN_TEXT_KEY);

            if (signTag.getCompound(BUKKIT_TAG).isEmpty()) {
                signTag.remove(BUKKIT_TAG);
            }
        }
        return signTag;
    }

    public static boolean isSignTP(org.bukkit.block.Sign sign) {
        return isSignTP(((CraftBlockEntityState<?>) sign).getSnapshotCustomNbtOnly());
    }

    public static boolean isSignTP(CompoundTag signTag) {
        return signTag.contains(BUKKIT_TAG) && signTag.getCompound(BUKKIT_TAG).contains(SIGN_TEXT_KEY);
    }

    public static void setSignData(org.bukkit.block.Sign sign) {
        SignData.deserializeSignData data = SignData.get(sign.getLocation());
        if (data != null) {
            setSignData(sign, data.getFront(), data.getBack(), data.getDisplay());
        }
        else {
            removeSignTP(sign);
        }
    }

    public static void setSignData(org.bukkit.block.Sign sign, String[] front, String[] back, String[] display) {
        World world = sign.getLocation().getWorld();

        if (world == null) return;
        ServerLevel level = ((CraftWorld) world).getHandle();
        BlockEntity tile = ((CraftBlockEntityState<?>) sign).getTileEntity();

        CompoundTag signTag = tile.saveWithFullMetadata(level.registryAccess());
        signTag = setSignData(signTag, front, back, display);

        ((CraftBlockEntityState<?>) sign).loadData(signTag);
        sign.update(true);
    }

    public static CompoundTag setSignData(CompoundTag signTag, String[] front, String[] back, String[] display) {
        CompoundTag defaultTag = signTag.copy();
        CompoundTag bukkitTag = signTag.getCompound(BUKKIT_TAG) instanceof CompoundTag tag ? tag : new CompoundTag();
        CompoundTag signtpTag = new CompoundTag();

        ListTag frontTextTag = new ListTag();
        ListTag backTextTag = new ListTag();
        ListTag displayTextTag = new ListTag();

        for (int i = 0; i < 4; i++) {
            frontTextTag.add(i, StringTag.valueOf(front[i]));
            backTextTag.add(i, StringTag.valueOf(back[i]));
            displayTextTag.add(i, StringTag.valueOf(display[i]));
        }

        signtpTag.put("front", frontTextTag);
        signtpTag.put("back", backTextTag);
        signtpTag.put("display", displayTextTag);

        bukkitTag.put(SIGN_TEXT_KEY, signtpTag);
        signTag.put(BUKKIT_TAG, bukkitTag);
        defaultTag.merge(signTag);

        return defaultTag;
    }

    public static SerializeTextData getSignData(org.bukkit.block.Sign sign) {
        return getSignData(((CraftBlockEntityState<?>) sign).getSnapshotNBTWithoutComponents());
    }

    public static SerializeTextData getSignData(CompoundTag signTag) {
        if (    signTag != null &&
                signTag.getCompound(BUKKIT_TAG) instanceof CompoundTag bukkitTag &&
                bukkitTag.getCompound(SIGN_TEXT_KEY) instanceof CompoundTag signtpTag)
        {
            ListTag frontTag = signtpTag.getList("front", Tag.TAG_STRING);
            ListTag backTag = signtpTag.getList("back", Tag.TAG_STRING);
            ListTag displayTag = signtpTag.getList("display", Tag.TAG_STRING);

            String[] front = frontTag.stream().map(Tag::getAsString).toArray(String[]::new);
            String[] back = backTag.stream().map(Tag::getAsString).toArray(String[]::new);
            String[] display = displayTag.stream().map(Tag::getAsString).toArray(String[]::new);

            if (Stream.of(front, back, display).allMatch(texts -> texts.length == 4)) {
                return new SerializeTextData(front, back, display);
            }
        }
        return new SerializeTextData();
    }

    public static void sendSignText(Player player, Location loc, Side side, String[] args) {
        if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign sign)) return;

        BlockPos blockPos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();
        List<Component> lines = SignText.getText(args, player);
        boolean hanging = sign instanceof org.bukkit.block.HangingSign;

        String[] jsonLine = lines.stream()
                .map(text -> GsonComponentSerializer.gson().serialize(text))
                .toArray(String[]::new);

        String textTagKey = switch (side) {
            case FRONT -> "front_text";
            case BACK -> "back_text";
        };

        CompoundTag signTag = ((CraftBlockEntityState<?>) sign).getTileEntity().saveWithFullMetadata(level.registryAccess());
        CompoundTag textTag = signTag.getCompound(textTagKey);
        ListTag msgTag = textTag.getList("messages", Tag.TAG_STRING);

        for (int i = 0; i < msgTag.size() && i < jsonLine.length; i++) {
            msgTag.set(i, StringTag.valueOf(jsonLine[i]));
        }

        textTag.remove("filtered_messages");
        textTag.put("messages", msgTag);
        signTag.put(textTagKey, textTag);

        ClientboundBlockEntityDataPacket packet = new ClientboundBlockEntityDataPacket(blockPos, hanging ? BlockEntityType.HANGING_SIGN : BlockEntityType.SIGN, signTag);
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static void sendSignRawText(Player player, Location loc, Side side, String[] args) {
        if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign sign)) return;

        BlockPos blockPos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();
        List<Component> lines = Arrays.stream(args).map(arg -> MiniMessage.miniMessage().deserialize(arg)).toList();
        boolean hanging = sign instanceof org.bukkit.block.HangingSign;

        String[] jsonLine = lines.stream()
                .map(text -> GsonComponentSerializer.gson().serialize(text))
                .toArray(String[]::new);

        String textTagKey = switch (side) {
            case FRONT -> "front_text";
            case BACK -> "back_text";
        };

        CompoundTag signTag = ((CraftBlockEntityState<?>) sign).getTileEntity().saveWithFullMetadata(level.registryAccess());
        CompoundTag textTag = signTag.getCompound(textTagKey);
        ListTag msgTag = textTag.getList("messages", Tag.TAG_STRING);

        for (int i = 0; i < msgTag.size() && i < jsonLine.length; i++) {
            msgTag.set(i, StringTag.valueOf(jsonLine[i]));
        }

        textTag.remove("filtered_messages");
        textTag.put("messages", msgTag);
        signTag.put(textTagKey, textTag);

        ClientboundBlockEntityDataPacket packet = new ClientboundBlockEntityDataPacket(blockPos, hanging ? BlockEntityType.HANGING_SIGN : BlockEntityType.SIGN, signTag);
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static void sendOpenSignEditor(Player player, Location loc, Side side) {
        if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign sign)) return;

        if (!(((CraftBlockEntityState<?>) sign).getTileEntity() instanceof SignBlockEntity blockEntity)) return;
        blockEntity.setAllowedPlayerEditor(player.getUniqueId());

        BlockPos blockPos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        boolean front = Side.FRONT.equals(side);

        ClientboundOpenSignEditorPacket packet = new ClientboundOpenSignEditorPacket(blockPos, front);
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static void reloadSignText(Player player, Location loc) {
        if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign sign)) return;

        BlockEntity tile = ((CraftBlockEntityState<?>) sign).getTileEntity();

        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(tile);
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }
}
