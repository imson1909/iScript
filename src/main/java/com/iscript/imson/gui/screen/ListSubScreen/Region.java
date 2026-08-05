package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.blockentities.RegionBlockEntity;
import com.iscript.imson.data.region.RegionTrigger;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.RegionEditScreen;
import com.iscript.imson.gui.screen.SubScreenLifecycle;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.gui.widget.ContextMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Region extends DashboardScreen.SubScreen {
    private final SubScreenLifecycle life = new SubScreenLifecycle(this);
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private BlockPos selectedPos = null;
    private long lastClickTime = 0;
    private int editBtnX = -1;
    private int editBtnY = -1;
    private int editBtnW = 0;
    private int editBtnH = 0;
    private ContextMenu contextMenu = new ContextMenu();
    private int confirmDialogY = 0;
    private String confirmDialogAction = "";
    private BlockPos confirmDialogPos = null;
    private BlockPos contextMenuPos = null;

    public Region(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void tick() {
        super.tick();
        life.tick(null);
    }

    @Override
    public void init() {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        life.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.region.list.search"));
        life.modals().register("confirm", () -> life.modals().isOpen("confirm"), v -> {}, null, null);
        life.init();
        contextMenu.close();
    }

    private List<RegionBlockEntity> filteredRegions() {
        List<RegionBlockEntity> all = new ArrayList<>(RegionBlockEntity.CLIENT_RENDER_TARGETS);
        EditBox box = life.search().box();
        String filter = box != null ? box.getValue().trim().toLowerCase() : life.state().lastSearch.trim().toLowerCase();
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

    private void openConfirmDialog(String action, BlockPos pos) {
        life.modals().open("confirm");
        confirmDialogAction = action;
        confirmDialogPos = pos;
        confirmDialogY = this.parent.height / 2 - 30;
    }

    private void closeConfirmDialog() {
        life.modals().close("confirm");
        confirmDialogAction = "";
        confirmDialogPos = null;
    }

    private void executeConfirm() {
        if ("delete".equals(confirmDialogAction) && confirmDialogPos != null) {
        }
        closeConfirmDialog();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        UI.inner(graphics, x, y, w, h);

        int rightX = x + w - RIGHT_PANEL_WIDTH;
        graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
        graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
        graphics.drawString(this.font, I18n.s("iscript.region.list.title"), rightX + 8, y + 26, Theme.ACCENT);

        life.search().setPos(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16);
        life.search().setVisible(true);

        List<RegionBlockEntity> regions = filteredRegions();
        int listH = h - 68;
        int listY = y + 42;
        int visible = listH / ITEM_HEIGHT;
        int scroll = life.selection().scroll();

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
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.enter"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.ENTER)), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.exit"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.EXIT)), leftX + 10, ly, leftW - 20);
                ly += 18;
                drawInfoRow(graphics, I18n.s("iscript.region.list.details.tick_fx"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.TICK)), leftX + 10, ly, leftW - 20);
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

        if (life.modals().isOpen("confirm")) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = confirmDialogY;
            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
            String label = confirmDialogPos != null ? confirmDialogPos.getX() + ", " + confirmDialogPos.getY() + ", " + confirmDialogPos.getZ() : "";
            graphics.drawCenteredString(this.font, "Delete \"" + label + "\"?", cx, dy + 8, Theme.ERROR);
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
            graphics.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx - 50, dy + 38, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
        }

        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
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

    private int countTriggers(RegionBlockEntity rbe, RegionTrigger.TriggerType type) {
        int count = 0;
        for (RegionTrigger t : rbe.getData().getTriggers()) {
            if (t.getTriggerType() == type) count++;
        }
        return count;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int cx = x + w / 2;
            int dy = confirmDialogY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60) {
                executeConfirm();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60) {
                closeConfirmDialog();
                return true;
            }
            return true;
        }

        if (contextMenu.isOpen()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button)) {
                String action = contextMenu.getLastAction();
                if (action != null && contextMenuPos != null) {
                    RegionBlockEntity rbe = findByPos(contextMenuPos);
                    switch (action) {
                        case "Edit" -> { if (rbe != null) openEditor(rbe); }
                        case "Delete" -> openConfirmDialog("delete", contextMenuPos);
                    }
                }
                return true;
            }
            contextMenu.close();
        }

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
        int scroll = life.selection().scroll();

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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            List<RegionBlockEntity> regions = filteredRegions();
            int listH = h - 68;
            int listY = y + 42;
            int visible = listH / ITEM_HEIGHT;
            int scroll = life.selection().scroll();
            for (int i = scroll; i < Math.min(scroll + visible, regions.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    contextMenuPos = regions.get(i).getBlockPos();
                    contextMenu.setCustomActions(new String[]{"Edit", "Delete"});
                    contextMenu.open((int) mouseX, (int) mouseY, contextMenuPos.toString(), false);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            List<RegionBlockEntity> regions = filteredRegions();
            int listH = h - 68;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int scroll = life.selection().scroll();
            int maxScroll = Math.max(0, regions.size() - visible);
            if (delta > 0) {
                life.selection().scroll(Math.max(0, scroll - 1));
            } else {
                life.selection().scroll(Math.min(scroll + 1, maxScroll));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (life.modals().isOpen("confirm")) {
            if (keyCode == 257 || keyCode == 335) {
                executeConfirm();
                return true;
            }
            if (keyCode == 256) {
                closeConfirmDialog();
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        life.removed();
        contextMenu.close();
        super.removed();
    }
}