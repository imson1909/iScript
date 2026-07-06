package com.iscript.iscript.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities {
    public static final Capability<PlayerQuestData> PLAYER_QUESTS = CapabilityManager.get(new CapabilityToken<>() {});
}
