package com.iscript.imson.data.dialog;

import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogManager {
    private static final Map<UUID, String> activeDialogs = new HashMap<>();
    private static final Map<UUID, String> currentNodes = new HashMap<>();

    public static void setActiveDialog(Player player, String dialogId) {
        activeDialogs.put(player.getUUID(), dialogId);
    }

    public static String getActiveDialog(Player player) {
        return activeDialogs.get(player.getUUID());
    }

    public static void setCurrentNode(Player player, String nodeId) {
        currentNodes.put(player.getUUID(), nodeId);
    }

    public static String getCurrentNode(Player player) {
        return currentNodes.get(player.getUUID());
    }

    public static void clear(Player player) {
        activeDialogs.remove(player.getUUID());
        currentNodes.remove(player.getUUID());
    }
}