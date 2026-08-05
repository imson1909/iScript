package com.iscript.imson.script.api;

import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.network.MorphSyncPacket;
import com.iscript.imson.network.IScriptNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.graalvm.polyglot.HostAccess;

public class MorphAPI {
    private final ScriptAPI root;

    public MorphAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void morph(String modelId) {
        if (root.player == null) return;
        root.player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            data.setModelId(modelId);
            data.setTextureId(modelId);
            data.setMorphed(true);
            data.setCurrentAnimation("animation." + modelId + ".idle");
            data.resetAnimationTick();
            if (root.player instanceof ServerPlayer sp) {
                IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(root.player));
            }
        });
    }

    @HostAccess.Export
    public void reset() {
        if (root.player == null) return;
        root.player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            data.setMorphed(false);
            data.setModelId("");
            data.setCurrentAnimation("idle");
            if (root.player instanceof ServerPlayer sp) {
                IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(root.player));
            }
        });
    }

    @HostAccess.Export
    public void scale(float scale) {
        if (root.player == null) return;
        root.player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            data.setScale(scale);
            if (root.player instanceof ServerPlayer sp) {
                IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(root.player));
            }
        });
    }

    @HostAccess.Export
    public void playAnim(String animationName) {
        if (root.player == null) return;
        root.player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            data.setCurrentAnimation(animationName);
            data.resetAnimationTick();
            if (root.player instanceof ServerPlayer sp) {
                IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(root.player));
            }
        });
    }

    @HostAccess.Export
    public boolean isMorphed() {
        return root.player != null && root.player.getCapability(MorphData.CAPABILITY).map(MorphData::isMorphed).orElse(false);
    }

    @HostAccess.Export
    public String getId() {
        return root.player != null ? root.player.getCapability(MorphData.CAPABILITY).map(MorphData::getModelId).orElse("") : "";
    }
}