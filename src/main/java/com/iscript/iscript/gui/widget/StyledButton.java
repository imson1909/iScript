package com.iscript.iscript.gui.widget;

import com.iscript.iscript.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class StyledButton extends AbstractWidget {
    private final Font font;
    private final Runnable onClick;
    private boolean accent = false;

    public StyledButton(Font font, int x, int y, int w, int h, Component text, Runnable onClick) {
        super(x, y, w, h, text);
        this.font = font;
        this.onClick = onClick;
    }

    public StyledButton setAccent(boolean accent) {
        this.accent = accent;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        boolean hov = isMouseOver(mouseX, mouseY);
        int bg = accent ? (hov ? Theme.alpha(Theme.ACCENT, 0.25f) : Theme.alpha(Theme.ACCENT, 0.12f))
                : (hov ? Theme.BG_HOVER : Theme.BG_INNER);
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.renderOutline(getX(), getY(), width, height, hov ? Theme.ACCENT : Theme.BORDER);
        int tc = active ? (accent ? Theme.ACCENT : (hov ? Theme.TEXT : Theme.TEXT_DIM)) : Theme.TEXT_MUTE;
        g.drawCenteredString(font, getMessage().getString(), getX() + width / 2, getY() + (height - 8) / 2, tc);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (active && visible && isMouseOver(mx, my)) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}