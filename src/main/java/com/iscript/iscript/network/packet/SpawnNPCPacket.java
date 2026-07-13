package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.registry.ModEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpawnNPCPacket {
    private final String id;

    public SpawnNPCPacket(String id) { this.id = id; }
    public SpawnNPCPacket(FriendlyByteBuf buf) { this.id = buf.readUtf(64); }
    public static SpawnNPCPacket decode(FriendlyByteBuf buf) { return new SpawnNPCPacket(buf); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(id); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel sl = (ServerLevel) player.level();
            if (sl == null) return;

            NPCData data = NPCManager.load(sl, id);
            if (data == null) {
                data = new NPCData();
                data.setName(id);
                data.setHealth(20);
                data.setMaxHealth(20);
            }

            var npc = ModEntities.ISCRIPT_NPC.get().create(sl);
            if (npc == null) return;

            double x = player.getX();
            double y = player.getY() + 0.1;
            double z = player.getZ();

            npc.moveTo(x, y, z, player.getYRot(), 0);
            npc.setYHeadRot(player.getYRot());
            npc.setYBodyRot(player.getYRot());

            npc.setNPCData(data);
            npc.setOwner(player);
            npc.setNoGravity(false);

            if (npc instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) npc;
                living.setHealth(living.getMaxHealth());
            }

            npc.finalizeSpawn(sl, sl.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.COMMAND, null, null);

            sl.addFreshEntity(npc);
        });
        ctx.get().setPacketHandled(true);
    }
}