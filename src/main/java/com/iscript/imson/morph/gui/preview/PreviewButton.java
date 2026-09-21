package com.iscript.imson.morph.gui.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class PreviewButton extends PreviewWidget {
    private final String text;
    private final Runnable onClick;

    public PreviewButton(int x, int y, int width, int height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    @Override
    public void render(GuiGraphics g, int previewX, int previewY, int mouseX, int mouseY, float partialTick) {
        boolean hover = isMouseOver(mouseX, mouseY, previewX, previewY);
        int color = hover ? 0xFF3A3A4A : 0xFF1E1E26;
        g.fill(previewX + x, previewY + y, previewX + x + width, previewY + y + height, color);
        g.renderOutline(previewX + x, previewY + y, width, height, 0xFF444455);
        g.drawCenteredString(Minecraft.getInstance().font, text, previewX + x + width / 2, previewY + y + (height - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int previewX, int previewY) {
        if (button == 0 && isMouseOver(mouseX, mouseY, previewX, previewY)) {
            onClick.run();
            return true;
        }
        return false;
    }
}