package com.iscript.iscript.gui.screen;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RegionListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private BlockPos selectedPos = null;
    private long lastClickTime = 0;
    private int editBtnX = -1;
    private int editBtnY = -1;
    private int editBtnW = 0;
    private int editBtnH = 0;
    private EditBox searchBox = null;

    public RegionListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        scroll = 0;
        selectedPos = null;
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        if (searchBox == null && this.minecraft != null) {
            createSearchBox();
        }
    }

    private void createSearchBox() {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        searchBox = new EditBox(this.minecraft.font, rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.region.list.search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(Theme.TEXT);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);
    }

    private List<RegionBlockEntity> filteredRegions() {
        List<RegionBlockEntity> all = new ArrayList<>(RegionBlockEntity.CLIENT_RENDER_TARGETS);
        String filter = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        if (filter.isEmpty()) return all;
        List<RegionBlockEntity> result = new ArrayList<>();
        for (RegionBlockEntity rbe : all) {
            BlockPos pos = rbe.getBlockPos();
            String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            if (coords.toLowerCase().contains(filter)) {
                result.add(rbe);
            }
        }
        return result;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        UI.inner(graphics, x, y, w, h);

        int rightX = x + w - RIGHT_PANEL_WIDTH;
        graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
        graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
        graphics.drawString(this.font, I18n.s("iscript.region.list.title"), rightX + 8, y + 26, Theme.ACCENT);

        if (searchBox != null) {
            searchBox.setX(rightX + 4);
            searchBox.setY(y + 4);
            searchBox.setWidth(RIGHT_PANEL_WIDTH - 8);
            searchBox.setHeight(16);
            searchBox.setVisible(true);
        }

        List<RegionBlockEntity> regions = filteredRegions();
        int listH = h - 68;
        int listY = y + 42;
        int visible = listH / ITEM_HEIGHT;

        for (int i = scroll; i < Math.min(scroll + visible, regions.size()); i++) {
            RegionBlockEntity rbe = regions.get(i);
            BlockPos pos = rbe.getBlockPos();
            String label = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mouseX >= rightX + 6 && mouseX <= x + w - 6 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
            boolean selected = pos.equals(selectedPos);

            UI.row(graphics, rightX + 6, rowY, RIGHT_PANEL_WIDTH - 12, ITEM_HEIGHT - 2, selected, hovered);
            graphics.drawString(this.font, label, rightX + 12, rowY + 5, selected ? Theme.TEXT : Theme.TEXT_DIM);
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

                UI.panel(graphics, leftX, leftY, leftW, leftH);

                int ly = leftY + 10;
                UI.title(graphics, this.font, I18n.s("iscript.region.list.details.title"), leftX + 10, ly);
                ly += 18;
                UI.label(graphics, this.font, anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ(), leftX + 10, ly);
                ly += 20;

                drawLine(graphics, leftX + 10, ly, leftW - 20, Theme.BORDER);
                ly += 8;

                drawInfoRow(graphics, I18n.s("iscript.region.list.details.name"), d.getName(), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.size"), d.getSizeX() + " x " + d.getSizeY() + " x " + d.getSizeZ(), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.tick"), String.valueOf(d.getTickInterval()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.enter"), String.valueOf(d.getEnterEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.exit"), String.valueOf(d.getExitEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.tick_fx"), String.valueOf(d.getTickEffects().size()), leftX + 10, ly, leftW - 20);
                ly += 24;

                int btnW = 80;
                int btnX = leftX + (leftW - btnW) / 2;
                boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= ly && mouseY <= ly + 24;
                UI.buttonBg(graphics, btnX, ly, btnW, 24, btnHovered, true);
                graphics.drawCenteredString(this.font, I18n.s("iscript.region.list.button.edit"), btnX + btnW / 2, ly + 7, Theme.TEXT);

                editBtnX = btnX;
                editBtnY = ly;
                editBtnW = btnW;
                editBtnH = 24;
            } else {
                editBtnX = -1;
            }
        } else {
            UI.centerLabel(graphics, this.font, I18n.s("iscript.region.list.empty"), leftX, y + h / 2, leftW);
            editBtnX = -1;
        }
    }

    private void drawLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    private void drawInfoRow(GuiGraphics g, String label, String value, int x, int y, int maxW) {
        UI.label(g, this.font, label, x, y);
        int labelW = this.font.width(label) + 6;
        String trimmed = this.font.plainSubstrByWidth(value, maxW - labelW);
        g.drawString(this.font, trimmed, x + labelW, y, Theme.TEXT);
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
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        List<RegionBlockEntity> regions = filteredRegions();
        int listH = h - 68;
        int listY = y + 42;
        int visible = listH / ITEM_HEIGHT;

        for (int i = scroll; i < Math.min(scroll + visible, regions.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
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

        if (selectedPos != null && editBtnX >= 0) {
            if (mouseX >= editBtnX && mouseX <= editBtnX + editBtnW && mouseY >= editBtnY && mouseY <= editBtnY + editBtnH) {
                RegionBlockEntity rbe = findByPos(selectedPos);
                if (rbe != null) {
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
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            List<RegionBlockEntity> regions = filteredRegions();
            int listH = h - 68;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int maxScroll = Math.max(0, regions.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        super.removed();
    }
}