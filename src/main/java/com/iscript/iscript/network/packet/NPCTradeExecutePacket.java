package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NPCTradeExecutePacket {
    private final int entityId;
    private final int offerIndex;

    public NPCTradeExecutePacket(int entityId, int offerIndex) {
        this.entityId = entityId;
        this.offerIndex = offerIndex;
    }

    public NPCTradeExecutePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.offerIndex = buf.readInt();
    }

    public static NPCTradeExecutePacket decode(FriendlyByteBuf buf) {
        return new NPCTradeExecutePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(offerIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.level().getEntity(entityId) instanceof IScriptNPCEntity npc) {
                NPCTradeData tradeData = npc.getNPCData().getTradeData();
                if (offerIndex >= 0 && offerIndex < tradeData.getOffers().size()) {
                    NPCTradeData.TradeOffer offer = tradeData.getOffers().get(offerIndex);
                    if (offer.isAvailable()) {
                        ItemStack input = offer.getInput();
                        boolean has = false;
                        for (int i = 0; i < player.getInventory().items.size(); i++) {
                            ItemStack stack = player.getInventory().items.get(i);
                            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, input) && stack.getCount() >= input.getCount()) {
                                stack.shrink(input.getCount());
                                if (stack.isEmpty()) player.getInventory().items.set(i, ItemStack.EMPTY);
                                has = true;
                                break;
                            }
                        }
                        if (has) {
                            ItemStack output = offer.getOutput().copy();
                            player.getInventory().add(output);
                            offer.use();
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
