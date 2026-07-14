package com.iscript.iscript.gui.screen;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class RegionListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 22;
    private static final int RIGHT_PANEL_WIDTH = 180;
    private BlockPos selectedPos = null;
    private long lastClickTime = 0;

    public RegionListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        scroll = 0;
        selectedPos = null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF0D0D12);

        int rightX = x + w - RIGHT_PANEL_WIDTH;
        graphics.fill(rightX, y, x + w, y + h, 0xFF15151C);
        graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);

        graphics.drawString(this.font, "Regions", rightX + 8, y + 8, 0xFF55AAFF);

        List<RegionBlockEntity> regions = new ArrayList<>(RegionBlockEntity.CLIENT_RENDER_TARGETS);
        int listH = h - 50;
        int listY = y + 28;
        int visible = (listH - 8) / ITEM_HEIGHT;

        for (int i = scroll; i < Math.min(scroll + visible, regions.size()); i++) {
            RegionBlockEntity rbe = regions.get(i);
            BlockPos pos = rbe.getBlockPos();
            String label = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mouseX >= rightX + 6 && mouseX <= x + w - 6 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
            boolean selected = pos.equals(selectedPos);

            int bg = selected ? 0xFF334466 : (hovered ? 0xFF222233 : 0x0015151C);
            graphics.fill(rightX + 6, rowY, x + w - 6, rowY + ITEM_HEIGHT - 2, bg);
            if (selected) {
                graphics.renderOutline(rightX + 6, rowY, RIGHT_PANEL_WIDTH - 12, ITEM_HEIGHT - 2, 0xFF55AAFF);
            }
            graphics.drawString(this.font, label, rightX + 12, rowY + 5, selected ? 0xFFFFFFFF : 0xFFBBBBBB);
        }

        int leftX = x + 8;
        int leftY = y + 8;
        int leftW = rightX - x - 16;
        int leftH = h - 16;

        if (selectedPos != null) {
            RegionBlockEntity rbe = findByPos(selectedPos);
            if (rbe != null) {
                var d = rbe.getData();
                BlockPos anchor = rbe.getBlockPos();

                graphics.fill(leftX, leftY, leftX + leftW, leftY + leftH, 0xFF111118);
                graphics.renderOutline(leftX, leftY, leftW, leftH, 0xFF2A2A3A);

                int ly = leftY + 10;
                graphics.drawString(this.font, "Region Block", leftX + 10, ly, 0xFF55AAFF);
                ly += 18;
                graphics.drawString(this.font, anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ(), leftX + 10, ly, 0xFF888888);
                ly += 20;

                drawLine(graphics, leftX + 10, ly, leftW - 20, 0xFF333344);
                ly += 8;

                drawInfoRow(graphics, "Name:", d.getName(), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, "Size:", d.getSizeX() + " x " + d.getSizeY() + " x " + d.getSizeZ(), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, "Tick:", String.valueOf(d.getTickInterval()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, "Enter:", String.valueOf(d.getEnterEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, "Exit:", String.valueOf(d.getExitEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, "Tick FX:", String.valueOf(d.getTickEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 24;

                int btnW = 80;
                int btnX = leftX + (leftW - btnW) / 2;
                boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= ly && mouseY <= ly + 24;
                int btnBg = btnHovered ? 0xFF4466CC : 0xFF334499;
                graphics.fill(btnX, ly, btnX + btnW, ly + 24, btnBg);
                graphics.renderOutline(btnX, ly, btnW, 24, btnHovered ? 0xFF6688FF : 0xFF4466CC);
                graphics.drawCenteredString(this.font, "Edit", btnX + btnW / 2, ly + 7, 0xFFFFFFFF);
            }
        } else {
            graphics.drawCenteredString(this.font, "Select a region block", leftX + leftW / 2, y + h / 2, 0xFF444455);
        }
    }

    private void drawLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    private void drawInfoRow(GuiGraphics g, String label, String value, int x, int y, int maxW) {
        g.drawString(this.font, label, x, y, 0xFF888888);
        int labelW = this.font.width(label) + 6;
        String trimmed = this.font.plainSubstrByWidth(value, maxW - labelW);
        g.drawString(this.font, trimmed, x + labelW, y, 0xFFCCCCCC);
    }

    private RegionBlockEntity findByPos(BlockPos pos) {
        for (RegionBlockEntity rbe : RegionBlockEntity.CLIENT_RENDER_TARGETS) {
            if (rbe.getBlockPos().equals(pos)) return rbe;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        List<RegionBlockEntity> regions = new ArrayList<>(RegionBlockEntity.CLIENT_RENDER_TARGETS);
        int listH = h - 50;
        int listY = y + 28;
        int visible = (listH - 8) / ITEM_HEIGHT;

        for (int i = scroll; i < Math.min(scroll + visible, regions.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 6 && mouseX <= x + w - 6 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                BlockPos clickedPos = regions.get(i).getBlockPos();
                long now = System.currentTimeMillis();
                if (clickedPos.equals(selectedPos) && now - lastClickTime < 300) {
                    openEditor(regions.get(i));
                    return true;
                }
                selectedPos = clickedPos;
                lastClickTime = now;
                return true;
            }
        }

        if (selectedPos != null) {
            RegionBlockEntity rbe = findByPos(selectedPos);
            if (rbe != null) {
                int leftX = x + 8;
                int leftY = y + 8;
                int leftW = rightX - x - 16;
                int ly = leftY + 10 + 18 + 20 + 8 + 18 * 6 + 24;
                int btnW = 80;
                int btnX = leftX + (leftW - btnW) / 2;
                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= ly && mouseY <= ly + 24) {
                    openEditor(rbe);
                    return true;
                }
            }
        }
        return false;
    }

    private void openEditor(RegionBlockEntity rbe) {
        Minecraft.getInstance().setScreen(new RegionEditScreen(rbe.getBlockPos(), rbe.getData()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            int maxScroll = Math.max(0, RegionBlockEntity.CLIENT_RENDER_TARGETS.size() - 10);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }
        return false;
    }
}