package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.blockentities.RegionBlockEntity;
import com.iscript.imson.data.region.RegionTrigger;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.RegionEditScreen;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Region extends ListSubScreen {
    private static final int RIGHT_PANEL_WIDTH = 160;
    private BlockPos selectedPos = null;
    private int editBtnX = -1, editBtnY = -1, editBtnW = 0, editBtnH = 0;

    public Region(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.region.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.region.list.empty"); }

    @Override
    protected List<String> getItemIds() {
        List<String> ids = new ArrayList<>();
        for (RegionBlockEntity rbe : RegionBlockEntity.CLIENT_RENDER_TARGETS) {
            BlockPos pos = rbe.getBlockPos();
            ids.add(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        }
        return ids;
    }

    @Override
    protected String getItemDisplayName(String id) { return id; }

    @Override
    protected void onSelect(String id) {
        setSelectedId(id);
        selectedPos = parsePos(id);
    }

    @Override
    protected void onNew(String id) {}

    @Override
    protected void onDelete(String id) {
        BlockPos pos = parsePos(id);
        if (pos != null) {
            RegionBlockEntity.CLIENT_RENDER_TARGETS.removeIf(rbe -> rbe.getBlockPos().equals(pos));
        }
        if (pos != null && pos.equals(selectedPos)) {
            selectedPos = null;
        }
    }

    @Override
    protected void onRename(String oldId, String newId) {}

    @Override
    protected void onDuplicate(String id) {}

    @Override
    protected void onCopy(String id) {}

    @Override
    protected void onPaste() {}

    @Override
    protected boolean canPaste() { return false; }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (selectedPos == null) return;
        RegionBlockEntity rbe = findByPos(selectedPos);
        if (rbe == null) {
            g.drawCenteredString(font, I18n.s("iscript.region.list.empty"), x + w / 2, y + h / 2, Theme.TEXT_MUTE);
            return;
        }
        var d = rbe.getData();
        BlockPos anchor = rbe.getBlockPos();

        UI.panel(g, x, y, w, h);

        int ly = y + 10;
        UI.title(g, font, I18n.s("iscript.region.list.details.title"), x + 10, ly);
        ly += 18;
        UI.label(g, font, anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ(), x + 10, ly);
        ly += 20;

        drawLine(g, x + 10, ly, w - 20, Theme.BORDER);
        ly += 8;

        drawInfoRow(g, I18n.s("iscript.region.list.details.name"), d.getName(), x + 10, ly, w - 20);
        ly += 18;
        drawInfoRow(g, I18n.s("iscript.region.list.details.size"), d.getSizeX() + " x " + d.getSizeY() + " x " + d.getSizeZ(), x + 10, ly, w - 20);
        ly += 18;
        drawInfoRow(g, I18n.s("iscript.region.list.details.tick"), String.valueOf(d.getTickInterval()), x + 10, ly, w - 20);
        ly += 18;
        drawInfoRow(g, I18n.s("iscript.region.list.details.enter"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.ENTER)), x + 10, ly, w - 20);
        ly += 18;
        drawInfoRow(g, I18n.s("iscript.region.list.details.exit"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.EXIT)), x + 10, ly, w - 20);
        ly += 18;
        drawInfoRow(g, I18n.s("iscript.region.list.details.tick_fx"), String.valueOf(countTriggers(rbe, RegionTrigger.TriggerType.TICK)), x + 10, ly, w - 20);
        ly += 24;

        int btnW = 80;
        int btnX = x + (w - btnW) / 2;
        boolean btnHovered = mx >= btnX && mx <= btnX + btnW && my >= ly && my <= ly + 24;
        UI.buttonBg(g, btnX, ly, btnW, 24, btnHovered, true);
        g.drawCenteredString(font, I18n.s("iscript.region.list.button.edit"), btnX + btnW / 2, ly + 7, Theme.TEXT);

        editBtnX = btnX;
        editBtnY = ly;
        editBtnW = btnW;
        editBtnH = 24;
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0) return false;
        if (selectedPos != null && editBtnX >= 0) {
            if (mx >= editBtnX && mx <= editBtnX + editBtnW && my >= editBtnY && my <= editBtnY + editBtnH) {
                RegionBlockEntity rbe = findByPos(selectedPos);
                if (rbe != null) {
                    Minecraft.getInstance().setScreen(new RegionEditScreen(rbe.getBlockPos(), rbe.getData()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) { return false; }

    @Override
    protected void doSave() {}

    private void drawLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    private void drawInfoRow(GuiGraphics g, String label, String value, int x, int y, int maxW) {
        UI.label(g, font, label, x, y);
        int labelW = font.width(label) + 6;
        String trimmed = font.plainSubstrByWidth(value, maxW - labelW);
        g.drawString(font, trimmed, x + labelW, y, Theme.TEXT);
    }

    private BlockPos parsePos(String id) {
        String[] parts = id.split(", ");
        if (parts.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
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
}