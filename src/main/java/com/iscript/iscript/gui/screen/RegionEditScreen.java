package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.UpdateRegionBlockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private static final int PANEL_W = 280;

    public RegionEditScreen(BlockPos anchor, RegionData data) {
        super(Component.literal(""));
        this.anchor = anchor;
        this.data = data;
    }

    @Override
    protected void init() {
        labels.clear();
        effectRows.clear();
        int x = PANEL_X + 12;
        int y = 12;
        int w = PANEL_W - 24;

        labels.add(new Label("Region Editor", 0, y));
        y += 22;

        addRenderableWidget(Button.builder(Component.literal(worldMode ? "World" : "Local"), b -> {
            worldMode = !worldMode;
            rebuildWidgets();
        }).pos(x, y).size(60, 18).build());

        addRenderableWidget(Button.builder(Component.literal(sizeMode ? "Size" : "Corners"), b -> {
            sizeMode = !sizeMode;
            rebuildWidgets();
        }).pos(x + 64, y).size(60, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            save();
            this.onClose();
        }).pos(x + 128, y).size(60, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .pos(x + 192, y).size(60, 18).build());
        y += 28;

        addLabel("Name", x, y);
        y += 14;
        nameBox = styledEditBox(x, y, w, data.getName());
        addRenderableWidget(nameBox);
        y += 28;

        if (sizeMode) {
            y = initSizeMode(x, y, w);
        } else {
            y = initCornersMode(x, y, w);
        }

        addLabel("Tick Interval", x, y);
        y += 14;
        tickBox = styledEditBox(x, y, w, String.valueOf(data.getTickInterval()));
        addRenderableWidget(tickBox);
        y += 28;

        addLabel("Required Faction", x, y);
        y += 14;
        factionBox = styledEditBox(x, y, w, data.getRequiredFaction());
        addRenderableWidget(factionBox);
        y += 30;

        initEffects(x, y, w);
    }

    private void addLabel(String text, int x, int y) {
        labels.add(new Label(text, x, y));
    }

    private EditBox styledEditBox(int x, int y, int w, String val) {
        EditBox box = new EditBox(this.font, x, y, w, 18, Component.literal(""));
        box.setValue(val);
        box.setTextColor(0xFFFFFFFF);
        box.setBordered(true);
        return box;
    }

    private int initSizeMode(int x, int y, int w) {
        addLabel("Center", x, y);
        y += 14;

        int colW = (w - 50) / 3;
        Vec3 center = getCenter();
        if (worldMode) center = center.add(anchor.getX(), anchor.getY(), anchor.getZ());
        cxBox = coordBox(x, y, center.x);
        cyBox = coordBox(x + colW + 5, y, center.y);
        czBox = coordBox(x + colW * 2 + 10, y, center.z);
        y += 22;

        labels.add(new Label("X", x + 2, y));
        labels.add(new Label("Y", x + colW + 7, y));
        labels.add(new Label("Z", x + colW * 2 + 12, y));
        y += 12;

        addRenderableWidget(Button.builder(Component.literal("Set Center from player"), b -> setCenterFromPlayer())
                .pos(x, y).size(w, 18).build());
        y += 24;

        addLabel("Size (blocks)", x, y);
        y += 14;

        Vec3 size = getSize();
        sxBox = coordBox(x, y, size.x);
        syBox = coordBox(x + colW + 5, y, size.y);
        szBox = coordBox(x + colW * 2 + 10, y, size.z);
        y += 22;

        labels.add(new Label("X", x + 2, y));
        labels.add(new Label("Y", x + colW + 7, y));
        labels.add(new Label("Z", x + colW * 2 + 12, y));
        return y + 12;
    }

    private int initCornersMode(int x, int y, int w) {
        addLabel("Corner 1", x, y);
        y += 14;

        int colW = (w - 50) / 3;
        Vec3 p1 = data.getPos1();
        if (worldMode) p1 = p1.add(anchor.getX(), anchor.getY(), anchor.getZ());
        x1Box = coordBox(x, y, p1.x);
        y1Box = coordBox(x + colW + 5, y, p1.y);
        z1Box = coordBox(x + colW * 2 + 10, y, p1.z);
        y += 22;

        labels.add(new Label("X", x + 2, y));
        labels.add(new Label("Y", x + colW + 7, y));
        labels.add(new Label("Z", x + colW * 2 + 12, y));
        y += 12;

        addRenderableWidget(Button.builder(Component.literal("Set Corner 1 from player"), b -> setCornerFromPlayer(1))
                .pos(x, y).size(w, 18).build());
        y += 24;

        addLabel("Corner 2", x, y);
        y += 14;

        Vec3 p2 = data.getPos2();
        if (worldMode) p2 = p2.add(anchor.getX(), anchor.getY(), anchor.getZ());
        x2Box = coordBox(x, y, p2.x);
        y2Box = coordBox(x + colW + 5, y, p2.y);
        z2Box = coordBox(x + colW * 2 + 10, y, p2.z);
        y += 22;

        labels.add(new Label("X", x + 2, y));
        labels.add(new Label("Y", x + colW + 7, y));
        labels.add(new Label("Z", x + colW * 2 + 12, y));
        y += 12;

        addRenderableWidget(Button.builder(Component.literal("Set Corner 2 from player"), b -> setCornerFromPlayer(2))
                .pos(x, y).size(w, 18).build());
        return y + 24;
    }

    private EditBox coordBox(int x, int y, double val) {
        EditBox box = new EditBox(this.font, x, y, 70, 18, Component.literal(""));
        String str = String.valueOf(val);
        box.setValue(str.endsWith(".0") ? str.substring(0, str.length() - 2) : str);
        box.setTextColor(0xFFFFFFFF);
        box.setBordered(true);
        box.setFilter(input -> input.matches("-?\\d*\\.?\\d*") || input.isEmpty());
        addRenderableWidget(box);
        return box;
    }

    private void initEffects(int x, int y, int w) {
        y = effectSection(x, y, w, "Enter Effects", data.getEnterEffects(), "enter");
        y = effectSection(x, y, w, "Exit Effects", data.getExitEffects(), "exit");
        effectSection(x, y, w, "Tick Effects", data.getTickEffects(), "tick");
    }

    private int effectSection(int x, int y, int w, String title, List<RegionEffect> effects, String type) {
        addLabel(title, x, y);
        y += 14;

        for (int i = 0; i < effects.size(); i++) {
            RegionEffect e = effects.get(i);
            y = effectRow(x, y, e, type, i);
            if (y > this.height - 50) break;
        }

        if (y <= this.height - 50) {
            addRenderableWidget(Button.builder(Component.literal("+ Add"), b -> {
                effects.add(new RegionEffect());
                rebuildWidgets();
            }).pos(x, y).size(70, 18).build());
            y += 22;
        }
        return y;
    }

    private int effectRow(int x, int y, RegionEffect e, String type, int index) {
        labels.add(new Label("Type:", x, y + 4));
        labels.add(new Label("Value:", x + 90, y + 4));

        EditBox typeBox = new EditBox(this.font, x + 32, y, 54, 16, Component.literal(""));
        typeBox.setValue(e.getType().name());
        typeBox.setTextColor(0xFFAAAAAA);
        typeBox.setBordered(true);
        addRenderableWidget(typeBox);

        EditBox valBox = new EditBox(this.font, x + 90, y, 130, 16, Component.literal(""));
        valBox.setValue(e.getValue());
        valBox.setTextColor(0xFFCCCCCC);
        valBox.setBordered(true);
        addRenderableWidget(valBox);

        addRenderableWidget(Button.builder(Component.literal("x"), b -> {
            switch (type) {
                case "enter" -> data.getEnterEffects().remove(index);
                case "exit" -> data.getExitEffects().remove(index);
                case "tick" -> data.getTickEffects().remove(index);
            }
            rebuildWidgets();
        }).pos(x + 224, y).size(18, 16).build());

        effectRows.add(new EffectRow(typeBox, valBox, type, index));
        return y + 20;
    }

    private void setCenterFromPlayer() {
        if (minecraft == null || minecraft.player == null) return;
        Vec3 p = minecraft.player.position();
        if (!worldMode) p = p.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
        if (cxBox != null) cxBox.setValue(formatDouble(p.x));
        if (cyBox != null) cyBox.setValue(formatDouble(p.y));
        if (czBox != null) czBox.setValue(formatDouble(p.z));
    }

    private void setCornerFromPlayer(int corner) {
        if (minecraft == null || minecraft.player == null) return;
        Vec3 p = minecraft.player.position();
        if (!worldMode) p = p.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
        if (corner == 1) {
            if (x1Box != null) x1Box.setValue(formatDouble(p.x));
            if (y1Box != null) y1Box.setValue(formatDouble(p.y));
            if (z1Box != null) z1Box.setValue(formatDouble(p.z));
        } else {
            if (x2Box != null) x2Box.setValue(formatDouble(p.x));
            if (y2Box != null) y2Box.setValue(formatDouble(p.y));
            if (z2Box != null) z2Box.setValue(formatDouble(p.z));
        }
    }

    private String formatDouble(double d) {
        String s = String.format("%.3f", d);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void save() {
        data.setName(nameBox.getValue());
        try { data.setTickInterval(Integer.parseInt(tickBox.getValue())); } catch (NumberFormatException ignored) {}
        data.setRequiredFaction(factionBox.getValue());

        if (sizeMode) {
            saveSizeMode();
        } else {
            saveCornersMode();
        }

        for (EffectRow row : effectRows) {
            RegionEffect e = switch (row.listType) {
                case "enter" -> data.getEnterEffects().get(row.index);
                case "exit" -> data.getExitEffects().get(row.index);
                case "tick" -> data.getTickEffects().get(row.index);
                default -> null;
            };
            if (e != null) {
                try {
                    e.setType(RegionEffect.EffectType.valueOf(row.typeBox.getValue().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
                e.setValue(row.valBox.getValue());
            }
        }

        IScriptNetwork.sendToServer(new UpdateRegionBlockPacket(anchor, data));
    }

    private void saveSizeMode() {
        try {
            double cx = Double.parseDouble(cxBox.getValue());
            double cy = Double.parseDouble(cyBox.getValue());
            double cz = Double.parseDouble(czBox.getValue());
            double sx = Double.parseDouble(sxBox.getValue());
            double sy = Double.parseDouble(syBox.getValue());
            double sz = Double.parseDouble(szBox.getValue());
            Vec3 center = new Vec3(cx, cy, cz);
            if (worldMode) center = center.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
            double hx = sx / 2.0;
            double hy = sy / 2.0;
            double hz = sz / 2.0;
            data.setPos1(new Vec3(center.x - hx, center.y - hy, center.z - hz));
            data.setPos2(new Vec3(center.x + hx, center.y + hy, center.z + hz));
        } catch (NumberFormatException ignored) {}
    }

    private void saveCornersMode() {
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

        int h = this.height - 16;
        g.fill(PANEL_X, 8, PANEL_X + PANEL_W, 8 + h, 0xDD15151C);
        g.renderOutline(PANEL_X, 8, PANEL_W, h, 0xFF555566);

        for (Label l : labels) {
            if (l.text.equals("Region Editor")) {
                int tw = this.font.width(l.text);
                g.drawString(this.font, l.text, PANEL_X + (PANEL_W - tw) / 2, l.y, 0xFF55AAFF);
            } else {
                g.drawString(this.font, l.text, l.x, l.y, 0xFF888888);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Label(String text, int x, int y) {}
    private record EffectRow(EditBox typeBox, EditBox valBox, String listType, int index) {}
}