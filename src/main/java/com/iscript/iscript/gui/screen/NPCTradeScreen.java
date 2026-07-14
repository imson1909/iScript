package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class NPCTradeScreen extends Screen {
    private final int entityId;
    private final NPCTradeData tradeData;
    private int scroll = 0;

    public NPCTradeScreen(int entityId, NPCTradeData tradeData) {
        super(Component.literal("Trade"));
        this.entityId = entityId;
        this.tradeData = tradeData;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 30).size(100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int y = 40;
        int idx = 0;
        for (NPCTradeData.TradeOffer offer : tradeData.getOffers()) {
            if (idx >= scroll && idx < scroll + 6) {
                int x = this.width / 2 - 120;
                boolean hovered = mouseX >= x && mouseX <= x + 240 && mouseY >= y && mouseY <= y + 36;
                int bg = hovered ? 0xFF444455 : 0xFF333344;
                graphics.fill(x, y, x + 240, y + 36, bg);
                graphics.renderOutline(x, y, 240, 36, 0xFF555566);

                ItemStack input = offer.getInput();
                ItemStack output = offer.getOutput();

                graphics.renderItem(input, x + 8, y + 10);
                graphics.renderItemDecorations(this.font, input, x + 8, y + 10);
                graphics.drawString(this.font, input.getHoverName(), x + 35, y + 6, 0xDDDDDD);
                graphics.drawString(this.font, "x" + input.getCount(), x + 35, y + 20, 0xAAAAAA);

                graphics.drawString(this.font, "->", x + 110, y + 14, 0xFFFFAA);

                graphics.renderItem(output, x + 140, y + 10);
                graphics.renderItemDecorations(this.font, output, x + 140, y + 10);
                graphics.drawString(this.font, output.getHoverName(), x + 167, y + 6, 0xDDDDDD);
                graphics.drawString(this.font, "x" + output.getCount(), x + 167, y + 20, 0xAAAAAA);

                String status = offer.isAvailable() ? "[" + offer.getUses() + "/" + offer.getMaxUses() + "]" : "[SOLD OUT]";
                int statusColor = offer.isAvailable() ? 0x55FF55 : 0xFF5555;
                graphics.drawString(this.font, status, x + 210, y + 14, statusColor);

                if (hovered && offer.isAvailable()) {
                    final int offerIdx = idx;
                    if (this.minecraft != null && this.minecraft.player != null) {
                        boolean hasItem = false;
                        for (ItemStack stack : this.minecraft.player.getInventory().items) {
                            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, input) && stack.getCount() >= input.getCount()) {
                                hasItem = true;
                                break;
                            }
                        }
                        if (hasItem) {
                            graphics.drawString(this.font, "[Click to trade]", x + 70, y + 38, 0x55FF55);
                        }
                    }
                }

                y += 42;
            }
            idx++;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = 40;
        int idx = 0;
        for (NPCTradeData.TradeOffer offer : tradeData.getOffers()) {
            if (idx >= scroll && idx < scroll + 6) {
                int x = this.width / 2 - 120;
                if (mouseX >= x && mouseX <= x + 240 && mouseY >= y && mouseY <= y + 36) {
                    if (offer.isAvailable()) {
                        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.TRADE_EXECUTE, ServerCommandPacket.tradeToTag(entityId, idx)));
                    }
                    return true;
                }
                y += 42;
            }
            idx++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, Math.max(0, tradeData.getOffers().size() - 6));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
