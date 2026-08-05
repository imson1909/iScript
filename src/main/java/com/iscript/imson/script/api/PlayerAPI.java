package com.iscript.imson.script.api;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.capability.ModCapabilities;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.HostAccess;

public class PlayerAPI {
    private final ScriptAPI root;

    public PlayerAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void sendMessage(String text) {
        root.player.sendSystemMessage(Component.literal(text));
    }

    @HostAccess.Export
    public void sendMessage(String text, String color) {
        try {
            ChatFormatting formatting = ChatFormatting.valueOf(color.toUpperCase());
            root.player.sendSystemMessage(Component.literal(text).withStyle(formatting));
        } catch (IllegalArgumentException e) {
            root.player.sendSystemMessage(Component.literal(text));
        }
    }

    @HostAccess.Export
    public void giveItem(String itemId, int count) {
        try {
            ResourceLocation id = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                ItemStack stack = new ItemStack(item, count);
                root.player.getInventory().add(stack);
            }
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to give item: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void teleport(double x, double y, double z) {
        root.player.teleportTo(x, y, z);
    }

    @HostAccess.Export
    public double getX() { return root.player.getX(); }

    @HostAccess.Export
    public double getY() { return root.player.getY(); }

    @HostAccess.Export
    public double getZ() { return root.player.getZ(); }

    @HostAccess.Export
    public String getName() {
        return root.player.getGameProfile().getName();
    }

    @HostAccess.Export
    public void setHealth(double hp) {
        root.player.setHealth((float) hp);
    }

    @HostAccess.Export
    public double getHealth() {
        return root.player.getHealth();
    }

    @HostAccess.Export
    public void setGamemode(String mode) {
        if (root.player instanceof ServerPlayer serverPlayer) {
            switch (mode.toLowerCase()) {
                case "survival" -> serverPlayer.setGameMode(GameType.SURVIVAL);
                case "creative" -> serverPlayer.setGameMode(GameType.CREATIVE);
                case "adventure" -> serverPlayer.setGameMode(GameType.ADVENTURE);
                case "spectator" -> serverPlayer.setGameMode(GameType.SPECTATOR);
            }
        }
    }

    @HostAccess.Export
    public boolean hasItem(String itemId, int count) {
        int found = 0;
        for (ItemStack stack : root.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().toString().equals(itemId)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    @HostAccess.Export
    public void clearInventory() {
        for (int i = 0; i < root.player.getInventory().items.size(); i++) {
            root.player.getInventory().items.set(i, ItemStack.EMPTY);
        }
    }

    @HostAccess.Export
    public void cameraMove(double x, double y, double z, float yaw, float pitch, int durationTicks) {
        if (root.player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_MOVE, ClientEffectPacket.cameraMoveToTag(x, y, z, yaw, pitch, durationTicks)), serverPlayer);
        }
    }

    @HostAccess.Export
    public void cameraReset() {
        if (root.player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_RESET, new net.minecraft.nbt.CompoundTag()), serverPlayer);
        }
    }

    @HostAccess.Export
    public void cameraReset(Player target) {
        if (target instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_RESET, new net.minecraft.nbt.CompoundTag()), serverPlayer);
        }
    }

    @HostAccess.Export
    public void setData(String key, String value) {
        root.player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setString(key, value));
    }

    @HostAccess.Export
    public String getData(String key) {
        return root.player.getCapability(ModCapabilities.PLAYER_DATA).map(data -> data.getString(key)).orElse("");
    }

    @HostAccess.Export
    public void setIntData(String key, int value) {
        root.player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setInt(key, value));
    }

    @HostAccess.Export
    public int getIntData(String key) {
        return root.player.getCapability(ModCapabilities.PLAYER_DATA).map(data -> data.getInt(key)).orElse(0);
    }

    @HostAccess.Export
    public void setFaction(String faction) {
        root.player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setFaction(faction));
    }

    @HostAccess.Export
    public String getFaction() {
        return root.player.getCapability(ModCapabilities.PLAYER_DATA).map(com.iscript.imson.capability.PlayerData::getFaction).orElse("neutral");
    }

    @HostAccess.Export
    public void setReputation(int value) {
        root.player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setReputation(value));
    }

    @HostAccess.Export
    public int getReputation() {
        return root.player.getCapability(ModCapabilities.PLAYER_DATA).map(com.iscript.imson.capability.PlayerData::getReputation).orElse(0);
    }
}