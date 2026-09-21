package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MorphSettingsOverlay {
    private final Minecraft mc;
    private final Font font;
    private final MorphEditorScreen screen;
    private final List<AbstractWidget> widgets = new ArrayList<>();

    private EditBox skinBox;
    private EditBox poseBox;
    private EditBox durationBox;
    private EditBox globalSizeBox;
    private EditBox guiSizeBox;

    private boolean animates = true;
    private String interpolation = "LINEAR";
    private boolean actionPlayerMode = false;

    private final String[] INTERPS = {"LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT"};
    private int interpIndex = 0;

    public MorphSettingsOverlay(MorphEditorScreen screen) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void init() {
        widgets.clear();
        int x = 20, y = 70, w = 220, h = 16, sp = 28;

        skinBox = textBox(x, y, w, h, screen.skinPath, v -> screen.skinPath = v);
        widgets.add(skinBox);
        y += sp;

        poseBox = textBox(x, y, w, h, screen.poseName, v -> screen.poseName = v);
        widgets.add(poseBox);
        y += sp;

        durationBox = numBox(x, y, w, h, screen.animationDuration, v -> screen.animationDuration = v);
        widgets.add(durationBox);
        y += sp;

        globalSizeBox = numBoxF(x, y, w, h, screen.globalSize, v -> screen.globalSize = v);
        widgets.add(globalSizeBox);
        y += sp;

        guiSizeBox = numBoxF(x, y, w, h, screen.guiSize, v -> screen.guiSize = v);
        widgets.add(guiSizeBox);

        this.animates = screen.animates;
        this.interpolation = screen.interpolation;
        this.actionPlayerMode = screen.actionPlayerMode;
        for (int i = 0; i < INTERPS.length; i++) {
            if (INTERPS[i].equalsIgnoreCase(interpolation)) { interpIndex = i; break; }
        }
    }

    private EditBox textBox(int x, int y, int w, int h, String val, Consumer<String> c) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(""));
        b.setMaxLength(256); b.setValue(val); b.setResponder(c); return b;
    }

    private EditBox numBox(int x, int y, int w, int h, int val, Consumer<Integer> c) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(""));
        b.setMaxLength(16); b.setValue(String.valueOf(val));
        b.setResponder(v -> { try { c.accept(Integer.parseInt(v)); } catch (Exception ignored) {} });
        return b;
    }

    private EditBox numBoxF(int x, int y, int w, int h, float val, Consumer<Float> c) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(""));
        b.setMaxLength(16); b.setValue(String.valueOf(val));
        b.setResponder(v -> { try { c.accept(Float.parseFloat(v.replace(",", "."))); } catch (Exception ignored) {} });
        return b;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        int panelX = 10, panelY = 60, panelW = 240, panelH = 280;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xDD16161E);
        g.renderOutline(panelX, panelY, panelW, panelH, Theme.BORDER);
        g.drawString(font, "Настройки модели", panelX + 10, panelY + 8, Theme.ACCENT);

        int x = panelX + 10;
        int y = panelY + 30;
        int sp = 28;
        int btnW = 100, btnH = 18;

        g.drawString(font, "Скин:", x, y - 10, Theme.TEXT);
        drawBtn(g, mx, my, x + 120, y - 12, 100, btnH, "Выбрать...");
        y += sp;

        g.drawString(font, "Поза:", x, y - 10, Theme.TEXT);
        drawBtn(g, mx, my, x + 60, y - 12, 70, btnH, "Создать");
        drawBtn(g, mx, my, x + 135, y - 12, 85, btnH, "Сбросить");
        y += sp;

        g.drawString(font, "Анимация:", x, y - 10, Theme.TEXT);
        drawCheck(g, mx, my, x + 80, y - 12, animates);
        g.drawString(font, animates ? "ВКЛ" : "ВЫКЛ", x + 100, y - 10, animates ? 0xFF55FF55 : 0xFFAA0000);
        y += sp;

        g.drawString(font, "Длительность:", x, y - 10, Theme.TEXT);
        y += sp;

        g.drawString(font, "Интерполяция:", x, y - 10, Theme.TEXT);
        drawBtn(g, mx, my, x + 100, y - 12, 120, btnH, INTERPS[interpIndex]);
        y += sp;

        g.drawString(font, "Action Player Mode:", x, y - 10, Theme.TEXT);
        drawCheck(g, mx, my, x + 130, y - 12, actionPlayerMode);
        g.drawString(font, actionPlayerMode ? "ВКЛ" : "ВЫКЛ", x + 150, y - 10, actionPlayerMode ? 0xFF55FF55 : 0xFFAA0000);
        y += sp;

        g.drawString(font, "Глобальный размер:", x, y - 10, Theme.TEXT);
        y += sp;

        g.drawString(font, "Размер в GUI:", x, y - 10, Theme.TEXT);

        for (AbstractWidget wgt : widgets) {
            wgt.render(g, mx, my, pt);
        }
    }

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    private void drawCheck(GuiGraphics g, int mx, int my, int x, int y, boolean checked) {
        boolean hover = mx >= x && mx <= x + 14 && my >= y && my <= y + 14;
        g.fill(x, y, x + 14, y + 14, hover ? Theme.BG_HOVER : 0xFF000000);
        g.renderOutline(x, y, 14, 14, Theme.BORDER);
        if (checked) g.drawString(font, "V", x + 3, y + 3, 0xFF55FF55);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        for (AbstractWidget wgt : widgets) {
            if (wgt.mouseClicked(mx, my, btn)) return true;
        }
        int x = 30, y = 90, sp = 28;
        y += sp;
        if (inBtn(mx, my, x + 60, y - 12, 70, 18)) {
            screen.poseName = "pose_" + System.currentTimeMillis() % 1000;
            poseBox.setValue(screen.poseName);
            return true;
        }
        if (inBtn(mx, my, x + 135, y - 12, 85, 18)) {
            screen.poseName = "";
            poseBox.setValue("");
            return true;
        }
        y += sp;
        if (inBtn(mx, my, x + 80, y - 12, 14, 18)) {
            animates = !animates;
            screen.animates = animates;
            return true;
        }
        y += sp;
        if (inBtn(mx, my, x + 100, y - 12, 120, 18)) {
            interpIndex = (interpIndex + 1) % INTERPS.length;
            interpolation = INTERPS[interpIndex];
            screen.interpolation = interpolation;
            return true;
        }
        y += sp;
        if (inBtn(mx, my, x + 130, y - 12, 14, 18)) {
            actionPlayerMode = !actionPlayerMode;
            screen.actionPlayerMode = actionPlayerMode;
            return true;
        }
        int skinBtnX = 30 + 120;
        int skinBtnY = 90 - 12;
        if (inBtn(mx, my, skinBtnX, skinBtnY, 100, 18)) {
            mc.setScreen(new SkinPickerScreen(screen, path -> {
                screen.skinPath = path;
                skinBox.setValue(path);
            }));
            return true;
        }
        return false;
    }

    private boolean inBtn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }
}