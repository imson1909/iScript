package com.iscript.imson.morph.gui.preview;

import net.minecraft.client.gui.GuiGraphics;

public abstract class PreviewWidget {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public PreviewWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(GuiGraphics g, int previewX, int previewY, int mouseX, int mouseY, float partialTick);

    public abstract boolean mouseClicked(double mouseX, double mouseY, int button, int previewX, int previewY);

    public boolean isMouseOver(double mouseX, double mouseY, int previewX, int previewY) {
        double absX = previewX + x;
        double absY = previewY + y;
        return mouseX >= absX && mouseX <= absX + width && mouseY >= absY && mouseY <= absY + height;
    }
}