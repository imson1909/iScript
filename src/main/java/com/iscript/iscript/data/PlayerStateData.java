package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.api.states.States;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_player_states";
    private final Map<UUID, States> players = new HashMap<>();

    public static PlayerStateData load(CompoundTag tag) {
        PlayerStateData data = new PlayerStateData();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                States states = new States();
                states.deserialize(playersTag.getCompound(key));
                data.players.put(uuid, states);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((uuid, states) -> playersTag.put(uuid.toString(), states.serialize()));
        tag.put("Players", playersTag);
        return tag;
    }

    public States get(UUID playerId) {
        return players.computeIfAbsent(playerId, k -> new States());
    }

    public static PlayerStateData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlayerStateData::load, PlayerStateData::new, DATA_NAME);
    }
}