package com.iscript.iscript.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerData data = new PlayerData();
    private final LazyOptional<PlayerData> optional = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.PLAYER_DATA.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.load(nbt);
    }
}