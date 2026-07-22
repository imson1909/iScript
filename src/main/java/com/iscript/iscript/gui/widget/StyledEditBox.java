package com.iscript.iscript.gui.widget;

import com.iscript.iscript.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class StyledEditBox extends EditBox {
    public StyledEditBox(Font font, int x, int y, int w, int h, Component hint) {
        super(font, x, y, w, h, hint);
        setTextColor(Theme.TEXT);
        setTextColorUneditable(Theme.TEXT_DIM);
        setBordered(false);
        setMaxLength(256);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int border = isFocused() ? Theme.ACCENT : Theme.BORDER;
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Theme.BG_INNER);
        g.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        super.renderWidget(g, mouseX, mouseY, pt);
    }
}