package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RegionEditScreen extends Screen {
    private final BlockPos anchor;
    private RegionData data;
    private boolean worldMode = false;
    private boolean sizeMode = true;
    private int scroll = 0;

    private EditBox nameBox;
    private EditBox cxBox, cyBox, czBox;
    private EditBox sxBox, syBox, szBox;
    private EditBox x1Box, y1Box, z1Box;
    private EditBox x2Box, y2Box, z2Box;
    private EditBox tickBox;
    private EditBox factionBox;

    private final List<Label> labels = new ArrayList<>();
    private final List<EffectRow> effectRows = new ArrayList<>();

    private static final int PANEL_X = 10;
    private static final int PANEL_W = 300;

    public RegionEditScreen(BlockPos anchor, RegionData data) {
        super(Component.literal(""));
        this.anchor = anchor;
        this.data = data;
    }

    public void rebuildWidgets() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        labels.clear();
        effectRows.clear();
        int x = PANEL_X + 14;
        int y = 14;
        int w = PANEL_W - 28;

        labels.add(new Label("Region Editor", 0, y));
        y += 24;

        btn(x, y, 64, 20, worldMode ? I18n.s("iscript.region.edit.mode_world") : I18n.s("iscript.region.edit.mode_local"), () -> { worldMode = !worldMode; rebuildWidgets(); }, false);
        btn(x + 70, y, 64, 20, sizeMode ? I18n.s("iscript.region.edit.mode_size") : I18n.s("iscript.region.edit.mode_corners"), () -> { sizeMode = !sizeMode; rebuildWidgets(); }, false);
        btn(x + 140, y, 64, 20, I18n.s("iscript.region.edit.save"), () -> { save(); this.onClose(); }, true);
        btn(x + 210, y, 64, 20, I18n.s("iscript.region.edit.close"), this::onClose, false);
        y += 30;

        lbl(I18n.s("iscript.region.edit.label.name"), x, y);
        y += 14;
        nameBox = box(x, y, w, data.getName());
        y += 30;

        y = sizeMode ? sizeMode(x, y, w) : cornersMode(x, y, w);

        lbl(I18n.s("iscript.region.edit.label.tick_interval"), x, y);
        y += 14;
        tickBox = box(x, y, w, String.valueOf(data.getTickInterval()));
        y += 30;

        lbl(I18n.s("iscript.region.edit.label.faction"), x, y);
        y += 14;
        factionBox = box(x, y, w, data.getRequiredFaction());
        y += 32;

        effects(x, y, w);
    }

    private void lbl(String t, int x, int y) {
        labels.add(new Label(t, x, y));
    }

    private EditBox box(int x, int y, int w, String val) {
        EditBox b = new EditBox(this.font, x, y, w, 20, Component.literal(""));
        b.setValue(val);
        b.setTextColor(Theme.TEXT);
        b.setBordered(false);
        addRenderableWidget(b);
        return b;
    }

    private Btn btn(int x, int y, int w, int h, String t, Runnable r, boolean a) {
        Btn b = new Btn(x, y, w, h, t, r, a);
        addRenderableWidget(b);
        return b;
    }

    private int sizeMode(int x, int y, int w) {
        lbl(I18n.s("iscript.region.edit.label.center"), x, y);
        y += 14;
        int cw = (w - 60) / 3;
        Vec3 c = getCenter();
        if (worldMode) c = c.add(anchor.getX(), anchor.getY(), anchor.getZ());
        cxBox = coord(x, y, c.x);
        cyBox = coord(x + cw + 6, y, c.y);
        czBox = coord(x + cw * 2 + 12, y, c.z);
        y += 24;
        labels.add(new Label(I18n.s("iscript.region.edit.label.x"), x + 4, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.y"), x + cw + 10, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.z"), x + cw * 2 + 16, y));
        y += 14;
        btn(x, y, w, 20, I18n.s("iscript.region.edit.button.set_center"), this::setCenterFromPlayer, false);
        y += 26;

        lbl(I18n.s("iscript.region.edit.label.size"), x, y);
        y += 14;
        Vec3 s = getSize();
        sxBox = coord(x, y, s.x);
        syBox = coord(x + cw + 6, y, s.y);
        szBox = coord(x + cw * 2 + 12, y, s.z);
        y += 24;
        labels.add(new Label(I18n.s("iscript.region.edit.label.x"), x + 4, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.y"), x + cw + 10, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.z"), x + cw * 2 + 16, y));
        return y + 14;
    }

    private int cornersMode(int x, int y, int w) {
        lbl(I18n.s("iscript.region.edit.label.corner1"), x, y);
        y += 14;
        int cw = (w - 60) / 3;
        Vec3 p1 = data.getPos1();
        if (worldMode) p1 = p1.add(anchor.getX(), anchor.getY(), anchor.getZ());
        x1Box = coord(x, y, p1.x);
        y1Box = coord(x + cw + 6, y, p1.y);
        z1Box = coord(x + cw * 2 + 12, y, p1.z);
        y += 24;
        labels.add(new Label(I18n.s("iscript.region.edit.label.x"), x + 4, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.y"), x + cw + 10, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.z"), x + cw * 2 + 16, y));
        y += 14;
        btn(x, y, w, 20, I18n.s("iscript.region.edit.button.set_corner1"), () -> setCornerFromPlayer(1), false);
        y += 26;

        lbl(I18n.s("iscript.region.edit.label.corner2"), x, y);
        y += 14;
        Vec3 p2 = data.getPos2();
        if (worldMode) p2 = p2.add(anchor.getX(), anchor.getY(), anchor.getZ());
        x2Box = coord(x, y, p2.x);
        y2Box = coord(x + cw + 6, y, p2.y);
        z2Box = coord(x + cw * 2 + 12, y, p2.z);
        y += 24;
        labels.add(new Label(I18n.s("iscript.region.edit.label.x"), x + 4, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.y"), x + cw + 10, y));
        labels.add(new Label(I18n.s("iscript.region.edit.label.z"), x + cw * 2 + 16, y));
        y += 14;
        btn(x, y, w, 20, I18n.s("iscript.region.edit.button.set_corner2"), () -> setCornerFromPlayer(2), false);
        return y + 26;
    }

    private EditBox coord(int x, int y, double v) {
        EditBox b = new EditBox(this.font, x, y, 76, 20, Component.literal(""));
        String s = String.valueOf(v);
        b.setValue(s.endsWith(".0") ? s.substring(0, s.length() - 2) : s);
        b.setTextColor(Theme.TEXT);
        b.setBordered(false);
        b.setFilter(i -> i.matches("-?\\d*\\.?\\d*") || i.isEmpty());
        addRenderableWidget(b);
        return b;
    }

    private void effects(int x, int y, int w) {
        y = effectSection(x, y, w, I18n.s("iscript.region.edit.effects.enter"), data.getEnterEffects(), "enter");
        y = effectSection(x, y, w, I18n.s("iscript.region.edit.effects.exit"), data.getExitEffects(), "exit");
        effectSection(x, y, w, I18n.s("iscript.region.edit.effects.tick"), data.getTickEffects(), "tick");
    }

    private int effectSection(int x, int y, int w, String title, List<RegionEffect> effects, String type) {
        lbl(title, x, y);
        y += 14;
        for (int i = 0; i < effects.size(); i++) {
            y = effectRow(x, y, effects.get(i), type, i);
            if (y > this.height - 50) break;
        }
        if (y <= this.height - 50) {
            btn(x, y, 76, 20, I18n.s("iscript.region.edit.button.add"), () -> { effects.add(new RegionEffect()); rebuildWidgets(); }, true);
            y += 24;
        }
        return y;
    }

    private int effectRow(int x, int y, RegionEffect e, String type, int index) {
        lbl(I18n.s("iscript.region.edit.label.type"), x, y + 5);
        lbl(I18n.s("iscript.region.edit.label.value"), x + 96, y + 5);
        EditBox typeBox = new EditBox(this.font, x + 36, y, 58, 18, Component.literal(""));
        typeBox.setValue(e.getType().name());
        typeBox.setTextColor(Theme.TEXT_DIM);
        typeBox.setBordered(false);
        addRenderableWidget(typeBox);
        EditBox valBox = new EditBox(this.font, x + 96, y, 140, 18, Component.literal(""));
        valBox.setValue(e.getValue());
        valBox.setTextColor(Theme.TEXT);
        valBox.setBordered(false);
        addRenderableWidget(valBox);
        btn(x + 240, y, 20, 18, I18n.s("iscript.region.edit.button.remove"), () -> {
            switch (type) {
                case "enter" -> data.getEnterEffects().remove(index);
                case "exit" -> data.getExitEffects().remove(index);
                case "tick" -> data.getTickEffects().remove(index);
            }
            rebuildWidgets();
        }, false);
        effectRows.add(new EffectRow(typeBox, valBox, type, index));
        return y + 22;
    }

    private void setCenterFromPlayer() {
        if (minecraft == null || minecraft.player == null) return;
        Vec3 p = minecraft.player.position();
        if (!worldMode) p = p.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
        if (cxBox != null) cxBox.setValue(fmt(p.x));
        if (cyBox != null) cyBox.setValue(fmt(p.y));
        if (czBox != null) czBox.setValue(fmt(p.z));
    }

    private void setCornerFromPlayer(int corner) {
        if (minecraft == null || minecraft.player == null) return;
        Vec3 p = minecraft.player.position();
        if (!worldMode) p = p.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
        if (corner == 1) {
            if (x1Box != null) x1Box.setValue(fmt(p.x));
            if (y1Box != null) y1Box.setValue(fmt(p.y));
            if (z1Box != null) z1Box.setValue(fmt(p.z));
        } else {
            if (x2Box != null) x2Box.setValue(fmt(p.x));
            if (y2Box != null) y2Box.setValue(fmt(p.y));
            if (z2Box != null) z2Box.setValue(fmt(p.z));
        }
    }

    private String fmt(double d) {
        String s = String.format("%.3f", d);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void save() {
        data.setName(nameBox.getValue());
        try { data.setTickInterval(Integer.parseInt(tickBox.getValue())); } catch (NumberFormatException ignored) {}
        data.setRequiredFaction(factionBox.getValue());
        if (sizeMode) saveSize(); else saveCorners();
        for (EffectRow row : effectRows) {
            RegionEffect ef = switch (row.listType) {
                case "enter" -> data.getEnterEffects().get(row.index);
                case "exit" -> data.getExitEffects().get(row.index);
                case "tick" -> data.getTickEffects().get(row.index);
                default -> null;
            };
            if (ef != null) {
                try { ef.setType(RegionEffect.EffectType.valueOf(row.typeBox.getValue().toUpperCase())); } catch (IllegalArgumentException ignored) {}
                ef.setValue(row.valBox.getValue());
            }
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.UPDATE_REGION_BLOCK, ServerCommandPacket.updateRegionToTag(anchor, data)));
    }

    private void saveSize() {
        try {
            double cx = Double.parseDouble(cxBox.getValue());
            double cy = Double.parseDouble(cyBox.getValue());
            double cz = Double.parseDouble(czBox.getValue());
            double sx = Double.parseDouble(sxBox.getValue());
            double sy = Double.parseDouble(syBox.getValue());
            double sz = Double.parseDouble(szBox.getValue());
            Vec3 c = new Vec3(cx, cy, cz);
            if (worldMode) c = c.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
            double hx = sx / 2.0, hy = sy / 2.0, hz = sz / 2.0;
            data.setPos1(new Vec3(c.x - hx, c.y - hy, c.z - hz));
            data.setPos2(new Vec3(c.x + hx, c.y + hy, c.z + hz));
        } catch (NumberFormatException ignored) {}
    }

    private void saveCorners() {
        try {
            Vec3 p1 = new Vec3(Double.parseDouble(x1Box.getValue()), Double.parseDouble(y1Box.getValue()), Double.parseDouble(z1Box.getValue()));
            Vec3 p2 = new Vec3(Double.parseDouble(x2Box.getValue()), Double.parseDouble(y2Box.getValue()), Double.parseDouble(z2Box.getValue()));
            if (worldMode) {
                p1 = p1.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
                p2 = p2.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
            }
            data.setPos1(p1);
            data.setPos2(p2);
        } catch (NumberFormatException ignored) {}
    }

    private Vec3 getCenter() {
        return new Vec3(
                (data.getPos1().x + data.getPos2().x) / 2.0,
                (data.getPos1().y + data.getPos2().y) / 2.0,
                (data.getPos1().z + data.getPos2().z) / 2.0
        );
    }

    private Vec3 getSize() {
        return new Vec3(
                Math.abs(data.getPos2().x - data.getPos1().x),
                Math.abs(data.getPos2().y - data.getPos1().y),
                Math.abs(data.getPos2().z - data.getPos1().z)
        );
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int h = this.height - 20;
        UI.panel(g, PANEL_X, 10, PANEL_W, h);
        if (minecraft != null) {
            double scale = minecraft.getWindow().getGuiScale();
            RenderSystem.enableScissor(
                    (int) (PANEL_X * scale),
                    (int) ((minecraft.getWindow().getGuiScaledHeight() - 10 - h) * scale),
                    (int) (PANEL_W * scale),
                    (int) (h * scale)
            );
        }
        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        for (Label l : labels) {
            if (l.text.equals(I18n.s("iscript.region.edit.title"))) {
                int tw = this.font.width(l.text);
                g.drawString(this.font, I18n.s("iscript.region.edit.title"), PANEL_X + (PANEL_W - tw) / 2, l.y, Theme.ACCENT);
            } else {
                g.drawString(this.font, l.text, l.x, l.y, Theme.TEXT_DIM);
            }
        }
        g.pose().popPose();
        RenderSystem.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= PANEL_X && mouseX <= PANEL_X + PANEL_W && mouseY >= 10 && mouseY <= this.height - 10) {
            int contentHeight = labels.isEmpty() ? 0 : labels.get(labels.size() - 1).y + 20;
            int maxScroll = Math.max(0, contentHeight - (this.height - 20));
            if (delta > 0) scroll = Math.max(0, scroll - 15);
            else scroll = Math.min(scroll + 15, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Label(String text, int x, int y) {}
    private record EffectRow(EditBox typeBox, EditBox valBox, String listType, int index) {}

    static class Btn extends AbstractWidget {
        String text;
        final Runnable onClick;
        final boolean accent;

        Btn(int x, int y, int w, int h, String t, Runnable r, boolean a) {
            super(x, y, w, h, Component.empty());
            text = t; onClick = r; accent = a;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isMouseOver(mx, my);
            int bg = accent ? (hov ? Theme.alpha(Theme.ACCENT, 0.25f) : Theme.alpha(Theme.ACCENT, 0.12f))
                    : (hov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.renderOutline(getX(), getY(), width, height, hov ? Theme.ACCENT : Theme.BORDER);
            int tc = active ? (accent ? Theme.ACCENT : (hov ? Theme.TEXT : Theme.TEXT_DIM)) : Theme.TEXT_MUTE;
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, text, getX() + width / 2, getY() + (height - 8) / 2, tc);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            if (active && isMouseOver(mx, my)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
}