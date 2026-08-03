package com.iscript.iscript.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class UI {
    private UI() {}

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    public static void panelAccent(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BORDER_ACCENT);
    }

    public static void inner(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_INNER);
    }

    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.fill(x, y, x + w, y + h, bg);
        if (selected) g.renderOutline(x, y, w, h, Theme.ACCENT);
        else g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    public static void label(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color);
    }

    public static void label(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text, x, y, Theme.TEXT_DIM);
    }

    public static void title(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text, x, y, Theme.ACCENT);
    }

    public static void buttonBg(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active) {
        int bg = !active ? Theme.BG_INNER : (hovered ? Theme.BG_HOVER : 0xFF1E1E2E);
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, hovered ? Theme.BORDER_ACCENT : Theme.BORDER);
    }

    public static void centerLabel(GuiGraphics g, Font font, String text, int x, int y, int w) {
        g.drawCenteredString(font, text, x + w / 2, y, Theme.TEXT_MUTE);
    }
}