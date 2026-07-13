package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveNPCDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NPCTradeEditScreen extends Screen {
    private final int entityId;
    private final NPCData parentData;
    private final List<TradeRow> rows = new ArrayList<>();
    private int scroll = 0;

    public NPCTradeEditScreen(int entityId, NPCData parentData) {
        super(Component.literal("Trade Editor"));
        this.entityId = entityId;
        this.parentData = parentData;
    }

    @Override
    protected void init() {
        rows.clear();
        for (NPCTradeData.TradeOffer offer : parentData.getTradeData().getOffers()) {
            rows.add(new TradeRow(offer));
        }

        this.addRenderableWidget(Button.builder(Component.literal("+ Add Offer"), btn -> {
            rows.add(new TradeRow());
            rebuild();
        }).pos(this.width / 2 - 100, this.height - 58).size(90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .pos(this.width / 2 - 5, this.height - 58).size(90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(this.width / 2 + 90, this.height - 58).size(90, 20).build());

        rebuild();
    }

    private void rebuild() {
        for (TradeRow row : rows) {
            row.remove();
        }
        int startY = 40;
        int rowH = 52;
        int visible = Math.min(rows.size() - scroll, 6);
        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            rows.get(idx).build(this, this.width / 2 - 220, startY + i * rowH, idx);
        }
    }

    private void save() {
        parentData.getTradeData().getOffers().clear();
        for (TradeRow row : rows) {
            NPCTradeData.TradeOffer offer = row.toOffer();
            if (offer != null) {
                parentData.getTradeData().getOffers().add(offer);
            }
        }
        IScriptNetwork.sendToServer(new SaveNPCDataPacket(entityId, parentData));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, this.width, this.height, 0xCC0A0A12, 0xDD0A0A12);

        int cx = this.width / 2;
        panel(g, cx - 240, 16, 480, this.height - 80);

        g.drawCenteredString(this.font, this.title, cx, 20, 0x55AAFF);

        for (int i = scroll; i < Math.min(rows.size(), scroll + 6); i++) {
            int ry = 40 + (i - scroll) * 52;
            g.renderOutline(cx - 220, ry, 440, 48, 0xFF444455);
            g.drawString(this.font, "#" + (i + 1), cx - 215, ry + 4, 0x888888, false);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, Math.max(0, rows.size() - 6));
        rebuild();
        return true;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x88000000);
        g.renderOutline(x, y, w, h, 0xFF444455);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class TradeRow {
        private EditBox inputId;
        private EditBox inputCount;
        private EditBox outputId;
        private EditBox outputCount;
        private EditBox maxUses;
        private Button deleteBtn;
        private NPCTradeEditScreen parent;

        private String inputIdVal = "minecraft:diamond";
        private int inputCountVal = 1;
        private String outputIdVal = "minecraft:emerald";
        private int outputCountVal = 1;
        private int maxUsesVal = 64;

        TradeRow() {}
        TradeRow(NPCTradeData.TradeOffer offer) {
            this.inputIdVal = offer.getInput().getItem().builtInRegistryHolder().key().location().toString();
            this.inputCountVal = offer.getInput().getCount();
            this.outputIdVal = offer.getOutput().getItem().builtInRegistryHolder().key().location().toString();
            this.outputCountVal = offer.getOutput().getCount();
            this.maxUsesVal = offer.getMaxUses();
        }

        void build(NPCTradeEditScreen parent, int x, int y, int idx) {
            this.parent = parent;
            int w = 90;
            int h = 16;

            inputId = new EditBox(parent.getMinecraft().font, x + 20, y + 4, w, h, Component.empty());
            inputId.setMaxLength(128);
            inputId.setValue(inputIdVal);
            parent.addRenderableWidget(inputId);

            inputCount = new EditBox(parent.getMinecraft().font, x + 20 + w + 4, y + 4, 36, h, Component.empty());
            inputCount.setMaxLength(4);
            inputCount.setValue(String.valueOf(inputCountVal));
            parent.addRenderableWidget(inputCount);

            outputId = new EditBox(parent.getMinecraft().font, x + 20 + w + 48, y + 4, w, h, Component.empty());
            outputId.setMaxLength(128);
            outputId.setValue(outputIdVal);
            parent.addRenderableWidget(outputId);

            outputCount = new EditBox(parent.getMinecraft().font, x + 20 + w * 2 + 52, y + 4, 36, h, Component.empty());
            outputCount.setMaxLength(4);
            outputCount.setValue(String.valueOf(outputCountVal));
            parent.addRenderableWidget(outputCount);

            maxUses = new EditBox(parent.getMinecraft().font, x + 20 + w * 2 + 96, y + 4, 40, h, Component.empty());
            maxUses.setMaxLength(4);
            maxUses.setValue(String.valueOf(maxUsesVal));
            parent.addRenderableWidget(maxUses);

            deleteBtn = Button.builder(Component.literal("X"), btn -> {
                parent.rows.remove(this);
                parent.rebuild();
            }).pos(x + 380, y + 2).size(20, 20).build();
            parent.addRenderableWidget(deleteBtn);
        }

        void remove() {
            if (parent == null) return;
            if (inputId != null) parent.removeWidget(inputId);
            if (inputCount != null) parent.removeWidget(inputCount);
            if (outputId != null) parent.removeWidget(outputId);
            if (outputCount != null) parent.removeWidget(outputCount);
            if (maxUses != null) parent.removeWidget(maxUses);
            if (deleteBtn != null) parent.removeWidget(deleteBtn);
        }

        NPCTradeData.TradeOffer toOffer() {
            try {
                net.minecraft.resources.ResourceLocation inputRes = new net.minecraft.resources.ResourceLocation(inputId.getValue());
                net.minecraft.resources.ResourceLocation outputRes = new net.minecraft.resources.ResourceLocation(outputId.getValue());
                net.minecraft.world.item.Item inputItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(inputRes);
                net.minecraft.world.item.Item outputItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(outputRes);
                if (inputItem == null || outputItem == null) return null;
                int inCount = Integer.parseInt(inputCount.getValue());
                int outCount = Integer.parseInt(outputCount.getValue());
                int uses = Integer.parseInt(maxUses.getValue());
                return new NPCTradeData.TradeOffer(new ItemStack(inputItem, inCount), new ItemStack(outputItem, outCount), uses);
            } catch (Exception e) {
                return null;
            }
        }
    }
}