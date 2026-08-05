package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class StyledPanel {
    private StyledPanel() {}

    public static void render(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    public static void renderDark(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_INNER);
        g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    public static void renderInner(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_INNER);
        g.renderOutline(x, y, w, h, Theme.BORDER);
    }

    public static void renderAccentBorder(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BORDER_ACCENT);
    }
}