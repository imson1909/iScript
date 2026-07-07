package com.iscript.iscript.gui.widget;

import com.iscript.iscript.data.dialog.DialogNodeData;
import com.iscript.iscript.gui.screen.DialogGraphEditorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class DialogNodeWidget extends AbstractWidget {
    private final DialogGraphEditorScreen screen;
    private final DialogNodeData node;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 100;
    private static final int HEADER_HEIGHT = 20;
    private static final int ARROW_SIZE = 8;

    public DialogNodeWidget(DialogGraphEditorScreen screen, DialogNodeData node, int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.literal(node.getTitle()));
        this.screen = screen;
        this.node = node;
    }

    public DialogNodeData getNode() { return node; }

    public void syncToData() {
        node.setX(this.getX());
        node.setY(this.getY());
    }

    public void moveBy(int dx, int dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
    }

    public void updateTitle() {
        this.setMessage(Component.literal(node.getTitle()));
    }

    public int getInputX() { return this.getX(); }
    public int getInputY() { return this.getY() + HEIGHT / 2; }

    public int getOutputX(int slot) { return this.getX() + WIDTH; }
    public int getOutputY(int slot) {
        int spacing = 24;
        return this.getY() + HEADER_HEIGHT + 12 + slot * spacing;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean isStart = screen.isStartNode(node.getId());
        int borderColor = isStart ? 0xFF44FF44 : (isFocused() ? 0xFF6688FF : 0xFF555566);
        int bg = isFocused() ? 0xFF3A3A4A : 0xFF2A2A3A;

        graphics.fill(getX(), getY(), getX() + WIDTH, getY() + HEIGHT, bg);
        graphics.renderOutline(getX(), getY(), WIDTH, HEIGHT, borderColor);

        graphics.fill(getX(), getY(), getX() + WIDTH, getY() + HEADER_HEIGHT, 0xFF444455);
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                node.getTitle(), getX() + 4, getY() + 6, 0xFFFFFFFF);

        String preview = node.getText();
        if (preview.length() > 25) preview = preview.substring(0, 25) + "...";
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                preview, getX() + 4, getY() + 30, 0xFFCCCCCC);

        int inY = getInputY();
        graphics.fill(getX() - ARROW_SIZE, inY - 4, getX(), inY + 4, 0xFF44FF44);
        graphics.fill(getX() - ARROW_SIZE + 2, inY - 6, getX(), inY + 6, 0xFF44FF44);

        for (int i = 0; i < 3; i++) {
            int oy = getOutputY(i);
            boolean hovered = mouseX >= getX() + WIDTH && mouseX <= getX() + WIDTH + ARROW_SIZE + 2
                    && mouseY >= oy - 6 && mouseY <= oy + 6;
            int color = hovered ? 0xFFFF6644 : 0xFFFF8844;
            graphics.fill(getX() + WIDTH, oy - 4, getX() + WIDTH + ARROW_SIZE, oy + 4, color);
            graphics.fill(getX() + WIDTH, oy - 6, getX() + WIDTH + ARROW_SIZE - 2, oy + 6, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 3; i++) {
                int oy = getOutputY(i);
                if (mouseX >= getX() + WIDTH && mouseX <= getX() + WIDTH + ARROW_SIZE + 2
                        && mouseY >= oy - 6 && mouseY <= oy + 6) {
                    screen.startConnection(this, i);
                    return true;
                }
            }
            int inY = getInputY();
            if (mouseX >= getX() - ARROW_SIZE - 2 && mouseX <= getX()
                    && mouseY >= inY - 6 && mouseY <= inY + 6) {
                return true;
            }
            if (mouseX >= getX() && mouseX <= getX() + WIDTH
                    && mouseY >= getY() && mouseY <= getY() + HEADER_HEIGHT) {
                screen.startDragging(this);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            this.setX(this.getX() + (int) dragX);
            this.setY(this.getY() + (int) dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}