package com.iscript.imson.script.api;

import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.entity.IScriptNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.HostAccess;

public class NpcAPI {
    private final IScriptNPCEntity npc;

    public NpcAPI(IScriptNPCEntity npc) {
        this.npc = npc;
    }

    @HostAccess.Export
    public NPCData getNpc() {
        return npc.getNPCData();
    }

    @HostAccess.Export
    public void setWalking(boolean value) {
        NPCData data = npc.getNPCData();
        if (data != null) {
            data.setWander(value);
            npc.rebuildAI();
        }
    }

    @HostAccess.Export
    public void setSprinting(boolean value) {
        npc.setSprinting(value);
    }

    @HostAccess.Export
    public void setInvulnerable(boolean value) {
        npc.setInvulnerable(value);
        NPCData data = npc.getNPCData();
        if (data != null) data.setInvulnerable(value);
    }

    @HostAccess.Export
    public boolean isNpc() {
        return true;
    }

    @HostAccess.Export
    public boolean isSwimming() {
        return npc.isInWater();
    }

    @HostAccess.Export
    public boolean isFlying() {
        return !npc.onGround();
    }

    @HostAccess.Export
    public boolean isSprinting() {
        return npc.isSprinting();
    }

    @HostAccess.Export
    public boolean isWalking() {
        return npc.getDeltaMovement().horizontalDistanceSqr() > 0.001;
    }

    @HostAccess.Export
    public boolean isFreeWalking() {
        NPCData data = npc.getNPCData();
        return data != null && (data.isWander() || data.isAlwaysWander());
    }

    @HostAccess.Export
    public boolean isInvulnerable() {
        return npc.isInvulnerable();
    }

    @HostAccess.Export
    public void canFly(boolean value) {
        NPCData data = npc.getNPCData();
        if (data != null) {
            data.setCanFly(value);
            npc.setNoGravity(value);
        }
    }

    @HostAccess.Export
    public void canPatrol(boolean value) {
        NPCData data = npc.getNPCData();
        if (data != null) {
            data.setHasPost(value);
            npc.rebuildAI();
        }
    }

    @HostAccess.Export
    public void canSwimming(boolean value) {
        NPCData data = npc.getNPCData();
        if (data != null) data.setCanSwim(value);
    }

    @HostAccess.Export
    public void addPatrolPoint(double x, double y, double z) {
        NPCData data = npc.getNPCData();
        if (data != null) {
            data.getPatrolPoints().add(new BlockPos((int)x, (int)y, (int)z));
            npc.rebuildAI();
        }
    }

    @HostAccess.Export
    public void addTrade(String inputId, int inputCount, String outputId, int outputCount) {
        NPCData data = npc.getNPCData();
        if (data == null) return;
        var tradeData = data.getTradeData();
        if (tradeData == null) return;
        var inputItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(inputId));
        var outputItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(outputId));
        if (inputItem == null || outputItem == null) return;
        var offers = tradeData.getOffers();
        offers.add(new com.iscript.imson.data.npc.NPCTradeData.TradeOffer(new ItemStack(inputItem, inputCount), new ItemStack(outputItem, outputCount), Integer.MAX_VALUE));
    }

    @HostAccess.Export
    public void removeTrade(int index) {
        NPCData data = npc.getNPCData();
        if (data == null) return;
        var offers = data.getTradeData().getOffers();
        if (index >= 0 && index < offers.size()) offers.remove(index);
    }

    @HostAccess.Export
    public void removeAllTrade() {
        NPCData data = npc.getNPCData();
        if (data == null) return;
        data.getTradeData().getOffers().clear();
    }

    @HostAccess.Export
    public Object[] getTrade(int index) {
        NPCData data = npc.getNPCData();
        if (data == null) return new Object[0];
        var offers = data.getTradeData().getOffers();
        if (index < 0 || index >= offers.size()) return new Object[0];
        var offer = offers.get(index);
        var input = offer.getInput();
        var output = offer.getOutput();
        return new Object[] {
                ForgeRegistries.ITEMS.getKey(input.getItem()).toString(),
                input.getCount(),
                ForgeRegistries.ITEMS.getKey(output.getItem()).toString(),
                output.getCount()
        };
    }
}