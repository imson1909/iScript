package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.gui.widget.StyledButton;
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

    private static final int ROWS_TOP = 40;
    private static final int ROW_HEIGHT = 42;

    public NPCTradeScreen(int entityId, NPCTradeData tradeData) {
        super(I18n.t("iscript.trade.title"));
        this.entityId = entityId;
        this.tradeData = tradeData;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new StyledButton(this.font, this.width / 2 - 50, this.height - 30, 100, 20, I18n.t("iscript.trade.close"), () -> this.onClose()));
    }

    private int visibleOffers() {
        return Math.max(1, (this.height - 80) / ROW_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.TEXT);

        int y = ROWS_TOP;
        int idx = 0;
        for (NPCTradeData.TradeOffer offer : tradeData.getOffers()) {
            if (idx >= scroll && idx < scroll + visibleOffers()) {
                int x = this.width / 2 - 120;
                boolean hovered = mouseX >= x && mouseX <= x + 240 && mouseY >= y && mouseY <= y + 36;
                UI.row(graphics, x, y, 240, 36, false, hovered);

                ItemStack input = offer.getInput();
                ItemStack output = offer.getOutput();

                graphics.renderItem(input, x + 8, y + 10);
                graphics.renderItemDecorations(this.font, input, x + 8, y + 10);
                graphics.drawString(this.font, input.getHoverName(), x + 35, y + 6, Theme.TEXT);
                graphics.drawString(this.font, "x" + input.getCount(), x + 35, y + 20, Theme.TEXT_DIM);

                graphics.drawString(this.font, "->", x + 110, y + 14, Theme.ACCENT);

                graphics.renderItem(output, x + 140, y + 10);
                graphics.renderItemDecorations(this.font, output, x + 140, y + 10);
                graphics.drawString(this.font, output.getHoverName(), x + 167, y + 6, Theme.TEXT);
                graphics.drawString(this.font, "x" + output.getCount(), x + 167, y + 20, Theme.TEXT_DIM);

                String status = offer.isAvailable() ? "[" + offer.getUses() + "/" + offer.getMaxUses() + "]" : I18n.s("iscript.trade.sold_out");
                int statusColor = offer.isAvailable() ? Theme.ACCENT : Theme.ERROR;
                graphics.drawString(this.font, status, x + 210, y + 14, statusColor);

                if (hovered && offer.isAvailable()) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        boolean hasItem = false;
                        for (ItemStack stack : this.minecraft.player.getInventory().items) {
                            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, input) && stack.getCount() >= input.getCount()) {
                                hasItem = true;
                                break;
                            }
                        }
                        if (hasItem) {
                            graphics.drawString(this.font, I18n.s("iscript.trade.click_to_trade"), x + 70, y + 38, Theme.ACCENT);
                        }
                    }
                }

                y += ROW_HEIGHT;
            }
            idx++;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = ROWS_TOP;
        int idx = 0;
        for (NPCTradeData.TradeOffer offer : tradeData.getOffers()) {
            if (idx >= scroll && idx < scroll + visibleOffers()) {
                int x = this.width / 2 - 120;
                if (mouseX >= x && mouseX <= x + 240 && mouseY >= y && mouseY <= y + 36) {
                    if (offer.isAvailable()) {
                        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.TRADE_EXECUTE, ServerCommandPacket.tradeToTag(entityId, idx)));
                    }
                    return true;
                }
                y += ROW_HEIGHT;
            }
            idx++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, Math.max(0, tradeData.getOffers().size() - visibleOffers()));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}