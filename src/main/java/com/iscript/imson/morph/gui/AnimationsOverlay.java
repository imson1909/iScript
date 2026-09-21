package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.morph.MorphData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.Map;

public class AnimationsOverlay {
    private final Minecraft mc;
    private final Font font;
    private final MorphEditorScreen screen;

    private final String[] actions = {"Idle", "Running", "Sprinting", "Crouching", "Swimming", "Flying", "Falling", "Jump", "Hurt", "Dying"};
    private int selectedAction = 0;

    private EditBox idBox, speedBox, fadeBox;

    public AnimationsOverlay(MorphEditorScreen screen) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void init() {
        int x = screen.width - 280;
        int y = 60;
        int w = 100;
        int h = 16;
        int s = 24;

        idBox = textBox(x, y, w, h, "ID", v -> getAction().id = v);
        speedBox = numBox(x, y + s, w, h, "Speed", v -> getAction().speed = v);
        fadeBox = numBox(x, y + s * 2, w, h, "Fade", v -> getAction().fade = v.intValue());
        loadAction();
    }

    private EditBox textBox(int x, int y, int w, int h, String name, java.util.function.Consumer<String> c) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(name));
        b.setMaxLength(64);
        b.setResponder(c);
        return b;
    }

    private EditBox numBox(int x, int y, int w, int h, String name, java.util.function.Consumer<Float> c) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(name));
        b.setMaxLength(16);
        b.setResponder(v -> { try { c.accept(Float.parseFloat(v.replace(",", "."))); } catch (Exception ignored) {} });
        return b;
    }

    private MorphData.ActionConfig getAction() {
        Map<String, MorphData.ActionConfig> acts = screen.getActions();
        return acts.computeIfAbsent(actions[selectedAction], k -> new MorphData.ActionConfig());
    }

    private void loadAction() {
        MorphData.ActionConfig a = getAction();
        idBox.setValue(a.id);
        speedBox.setValue(String.valueOf(a.speed));
        fadeBox.setValue(String.valueOf(a.fade));
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        int leftX = 10;
        int leftY = 60;
        int btnW = 100;
        int btnH = 18;

        g.fill(leftX, leftY - 10, leftX + btnW + 20, leftY + actions.length * 22 + 10, 0xAA16161E);
        g.renderOutline(leftX, leftY - 10, btnW + 20, actions.length * 22 + 20, Theme.BORDER);

        for (int i = 0; i < actions.length; i++) {
            int by = leftY + i * 22;
            boolean hover = inRect(mx, my, leftX + 5, by, btnW, btnH);
            boolean active = i == selectedAction;
            int bg = active ? Theme.ACCENT : (hover ? Theme.BG_HOVER : 0xFF1E1E26);
            g.fill(leftX + 5, by, leftX + 5 + btnW, by + btnH, bg);
            g.renderOutline(leftX + 5, by, btnW, btnH, Theme.BORDER);
            g.drawCenteredString(font, actions[i], leftX + 5 + btnW / 2, by + 5, Theme.TEXT);
        }

        int rightX = screen.width - 280;
        int rightY = 60;
        int panelW = 260;
        int panelH = 120;

        g.fill(rightX, rightY - 10, rightX + panelW, rightY + panelH, 0xAA16161E);
        g.renderOutline(rightX, rightY - 10, panelW, panelH, Theme.BORDER);

        g.drawString(font, "Действие: " + actions[selectedAction], rightX + 10, rightY, Theme.ACCENT);

        int s = 24;
        int currentY = rightY + 20;

        g.drawString(font, "ID Анимации", rightX + 10, currentY, Theme.TEXT_DIM);
        currentY += 18;
        idBox.render(g, mx, my, pt);
        currentY += s;

        g.drawString(font, "Скорость", rightX + 10, currentY, Theme.TEXT_DIM);
        currentY += 18;
        speedBox.render(g, mx, my, pt);
        currentY += s;

        g.drawString(font, "Переход (Fade)", rightX + 10, currentY, Theme.TEXT_DIM);
        currentY += 18;
        fadeBox.render(g, mx, my, pt);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int leftX = 10;
        int leftY = 60;
        int btnW = 100;
        int btnH = 18;

        for (int i = 0; i < actions.length; i++) {
            int by = leftY + i * 22;
            if (inRect(mx, my, leftX + 5, by, btnW, btnH)) {
                selectedAction = i;
                loadAction();
                return true;
            }
        }

        if (idBox.mouseClicked(mx, my, btn)) return true;
        if (speedBox.mouseClicked(mx, my, btn)) return true;
        if (fadeBox.mouseClicked(mx, my, btn)) return true;

        return false;
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }
}