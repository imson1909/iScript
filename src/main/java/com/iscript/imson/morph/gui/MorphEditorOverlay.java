package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public class MorphEditorOverlay {
    private final Minecraft mc;
    private final Font font;
    private final MorphEditorScreen screen;

    private boolean animates = true;
    private boolean ignored = false;
    private boolean actionPlayerMode = false;
    private String interpolation = "LINEAR";
    private final String[] INTERPS = {"LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT"};
    private int interpIndex = 0;
    private boolean showInterpList = false;

    private int durationValue = 10;
    private float globalSizeValue = 1.0f;
    private float guiSizeValue = 1.0f;

    private final List<String> poses = new ArrayList<>();

    public MorphEditorOverlay(MorphEditorScreen screen) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void init() {
        this.animates = screen.animates;
        this.ignored = screen.ignored;
        this.actionPlayerMode = screen.actionPlayerMode;
        this.interpolation = screen.interpolation;
        this.durationValue = screen.animationDuration;
        this.globalSizeValue = screen.globalSize;
        this.guiSizeValue = screen.guiSize;

        for (int i = 0; i < INTERPS.length; i++) {
            if (INTERPS[i].equalsIgnoreCase(interpolation)) { interpIndex = i; break; }
        }
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        int rightX = screen.width - 200;
        int panelW = 190;
        int spacing = 26;

        // Верхняя панель - настройки анимации
        int topY = 60;
        g.fill(rightX, topY, rightX + panelW, topY + spacing * 5 + 10, 0xAA16161E);
        g.renderOutline(rightX, topY, panelW, spacing * 5 + 10, Theme.BORDER);

        int currentY = topY + 10;

        drawLabel(g, rightX + 10, currentY, "Анимируется");
        drawCheckbox(g, mx, my, rightX + panelW - 24, currentY, animates);
        currentY += spacing;

        drawLabel(g, rightX + 10, currentY, "Длительность:");
        drawNumArrows(g, mx, my, rightX + panelW - 60, currentY, durationValue, "<", ">");
        currentY += spacing;

        drawLabel(g, rightX + 10, currentY, "Игнорируется");
        drawCheckbox(g, mx, my, rightX + panelW - 24, currentY, ignored);
        currentY += spacing;

        drawLabel(g, rightX + 10, currentY, "Интерполяция:");
        drawButton(g, mx, my, rightX + panelW - 100, currentY, 90, 18, interpolation);
        if (showInterpList) {
            drawInterpList(g, mx, my, rightX + panelW - 100, currentY + 20);
        }
        currentY += spacing;

        drawLabel(g, rightX + 10, currentY, "Action player mode");
        drawCheckbox(g, mx, my, rightX + panelW - 24, currentY, actionPlayerMode);

        // Нижняя панель - размеры
        int bottomY = screen.height - 120;
        int bottomPanelH = spacing * 2 + 20;
        g.fill(rightX, bottomY, rightX + panelW, bottomY + bottomPanelH, 0xAA16161E);
        g.renderOutline(rightX, bottomY, panelW, bottomPanelH, Theme.BORDER);

        currentY = bottomY + 10;

        drawLabel(g, rightX + 10, currentY, "Глоб. размер:");
        drawNumArrows(g, mx, my, rightX + panelW - 60, currentY, (int) globalSizeValue, "<", ">");
        currentY += spacing;

        drawLabel(g, rightX + 10, currentY, "GUI размер:");
        drawNumArrows(g, mx, my, rightX + panelW - 60, currentY, (int) guiSizeValue, "<", ">");

        // Левые кнопки
        int leftX = 10;
        int leftY = 60;
        drawButton(g, mx, my, leftX, leftY, 120, 20, "Выбрать скин");
        drawButton(g, mx, my, leftX, leftY + 26, 120, 20, "Создать позу");

        if (!poses.isEmpty()) {
            for (int i = 0; i < poses.size(); i++) {
                drawButton(g, mx, my, leftX + 10, leftY + 52 + i * 22, 110, 18, poses.get(i));
            }
        }
    }

    private void drawLabel(GuiGraphics g, int x, int y, String text) {
        g.drawString(font, text, x, y, Theme.TEXT);
    }

    private void drawCheckbox(GuiGraphics g, int mx, int my, int x, int y, boolean checked) {
        boolean hover = inRect(mx, my, x, y, 14, 14);
        g.fill(x, y, x + 14, y + 14, hover ? Theme.BG_HOVER : 0xFF000000);
        g.renderOutline(x, y, 14, 14, Theme.BORDER);
        if (checked) g.drawString(font, "V", x + 3, y + 3, 0xFF55FF55);
    }

    private void drawButton(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hover = inRect(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    private void drawNumArrows(GuiGraphics g, int mx, int my, int x, int y, int value, String left, String right) {
        int btnW = 16, btnH = 16;
        boolean lh = inRect(mx, my, x, y, btnW, btnH);
        g.fill(x, y, x + btnW, y + btnH, lh ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, btnW, btnH, Theme.BORDER);
        g.drawCenteredString(font, left, x + btnW / 2, y + 4, Theme.TEXT);

        String valStr = String.valueOf(value);
        int valW = font.width(valStr);
        g.fill(x + btnW + 2, y, x + btnW + 2 + valW + 8, y + btnH, 0xFF000000);
        g.renderOutline(x + btnW + 2, y, valW + 8, btnH, Theme.BORDER);
        g.drawString(font, valStr, x + btnW + 6, y + 4, Theme.TEXT);

        int rx = x + btnW * 2 + 10;
        boolean rh = inRect(mx, my, rx, y, btnW, btnH);
        g.fill(rx, y, rx + btnW, y + btnH, rh ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(rx, y, btnW, btnH, Theme.BORDER);
        g.drawCenteredString(font, right, rx + btnW / 2, y + 4, Theme.TEXT);
    }

    private void drawInterpList(GuiGraphics g, int mx, int my, int x, int y) {
        int itemH = 16;
        int listH = INTERPS.length * itemH;
        g.fill(x, y, x + 100, y + listH, 0xEE1E1E26);
        g.renderOutline(x, y, 100, listH, Theme.BORDER);
        for (int i = 0; i < INTERPS.length; i++) {
            boolean hover = inRect(mx, my, x, y + i * itemH, 100, itemH);
            if (hover) g.fill(x, y + i * itemH, x + 100, y + i * itemH + itemH, Theme.BG_HOVER);
            g.drawString(font, INTERPS[i], x + 4, y + i * itemH + 4, Theme.TEXT);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int rightX = screen.width - 200;
        int panelW = 190;
        int spacing = 26;

        // Верхняя панель
        int topY = 60;
        int currentY = topY + 10;

        if (inRect(mx, my, rightX + panelW - 24, currentY, 14, 14)) {
            animates = !animates;
            screen.animates = animates;
            return true;
        }
        currentY += spacing;

        int arrowX = rightX + panelW - 60;
        if (inRect(mx, my, arrowX, currentY, 16, 16)) {
            durationValue = Math.max(0, durationValue - 1);
            screen.animationDuration = durationValue;
            return true;
        }
        if (inRect(mx, my, arrowX + 42, currentY, 16, 16)) {
            durationValue++;
            screen.animationDuration = durationValue;
            return true;
        }
        currentY += spacing;

        if (inRect(mx, my, rightX + panelW - 24, currentY, 14, 14)) {
            ignored = !ignored;
            screen.ignored = ignored;
            return true;
        }
        currentY += spacing;

        if (inRect(mx, my, rightX + panelW - 100, currentY, 90, 18)) {
            showInterpList = !showInterpList;
            return true;
        }
        if (showInterpList) {
            int listY = currentY + 20;
            for (int i = 0; i < INTERPS.length; i++) {
                if (inRect(mx, my, rightX + panelW - 100, listY + i * 16, 100, 16)) {
                    interpIndex = i;
                    interpolation = INTERPS[i];
                    screen.interpolation = interpolation;
                    showInterpList = false;
                    return true;
                }
            }
        }
        currentY += spacing;

        if (inRect(mx, my, rightX + panelW - 24, currentY, 14, 14)) {
            actionPlayerMode = !actionPlayerMode;
            screen.actionPlayerMode = actionPlayerMode;
            return true;
        }

        // Нижняя панель
        int bottomY = screen.height - 120;
        currentY = bottomY + 10;

        int bArrowX = rightX + panelW - 60;
        if (inRect(mx, my, bArrowX, currentY, 16, 16)) {
            globalSizeValue = Math.max(0.1f, globalSizeValue - 0.1f);
            screen.globalSize = globalSizeValue;
            return true;
        }
        if (inRect(mx, my, bArrowX + 42, currentY, 16, 16)) {
            globalSizeValue += 0.1f;
            screen.globalSize = globalSizeValue;
            return true;
        }
        currentY += spacing;

        if (inRect(mx, my, bArrowX, currentY, 16, 16)) {
            guiSizeValue = Math.max(0.1f, guiSizeValue - 0.1f);
            screen.guiSize = guiSizeValue;
            return true;
        }
        if (inRect(mx, my, bArrowX + 42, currentY, 16, 16)) {
            guiSizeValue += 0.1f;
            screen.guiSize = guiSizeValue;
            return true;
        }

        // Левые кнопки
        int leftX = 10;
        int leftY = 60;
        if (inRect(mx, my, leftX, leftY, 120, 20)) {
            mc.setScreen(new SkinPickerScreen(screen, path -> {
                screen.skinPath = path;
            }));
            return true;
        }
        if (inRect(mx, my, leftX, leftY + 26, 120, 20)) {
            String newPose = "pose_" + System.currentTimeMillis() % 1000;
            if (!poses.contains(newPose)) {
                poses.add(newPose);
            }
            screen.poseName = newPose;
            return true;
        }

        for (int i = 0; i < poses.size(); i++) {
            if (inRect(mx, my, leftX + 10, leftY + 52 + i * 22, 110, 18)) {
                screen.poseName = poses.get(i);
                return true;
            }
        }

        return false;
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }
}