package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import java.util.function.Consumer;

public class ContextMenu {
    private int x, y;
    private int width = 100;
    private int height;
    private String itemId;
    private boolean open = false;
    private boolean canPaste = false;
    private Consumer<String> actionCallback;
    private String[] customActions = null;
    private String lastAction = null;

    public void setCustomActions(String[] actions) {
        this.customActions = actions;
    }

    public void setAction(Consumer<String> callback) {
        this.actionCallback = callback;
    }

    public void open(int x, int y, String itemId, boolean canPaste) {
        this.x = x;
        this.y = y;
        this.itemId = itemId;
        this.canPaste = canPaste;
        this.open = true;
        this.lastAction = null;

        String[] actions = getActiveActions();
        this.height = actions.length * 22 + 4;
    }

    public void close() {
        this.open = false;
        this.itemId = null;
    }

    public boolean isOpen() {
        return open;
    }

    public String getItemId() {
        return itemId;
    }

    public String getLastAction() {
        return lastAction;
    }

    private String[] getActiveActions() {
        if (customActions != null) return customActions;
        return new String[]{"Copy", "Paste", "Rename", "Duplicate", "Delete"};
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || button != 0) return false;

        String[] actions = getActiveActions();
        int cy = y + 2;

        for (String action : actions) {
            boolean isDisabled = action.equals("Paste") && !canPaste;

            if (mouseX >= x && mouseX <= x + width && mouseY >= cy && mouseY <= cy + 20) {
                if (!isDisabled) {
                    this.lastAction = action;
                    if (actionCallback != null) actionCallback.accept(action);
                }
                close();
                return true;
            }
            cy += 22;
        }

        close();
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!open) return;

        String[] actions = getActiveActions();
        int maxTextWidth = 0;
        for (String action : actions) {
            String label = resolveLabel(action);
            int w = font.width(label);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        this.width = maxTextWidth + 16;

        graphics.flush();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300.0F);

        int solidBg = 0xFF13131A;
        graphics.fill(x, y, x + width, y + height, solidBg);
        graphics.renderOutline(x, y, width, height, Theme.BORDER);

        int cy = y + 2;
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();

        for (String action : actions) {
            boolean isDisabled = action.equals("Paste") && !canPaste;
            boolean hovered = !isDisabled && mouseX >= x && mouseX <= x + width && mouseY >= cy && mouseY <= cy + 20;

            int itemBg = hovered ? Theme.BG_HOVER : solidBg;
            int tc = isDisabled ? Theme.TEXT_MUTE : (action.equals("Delete") ? Theme.ERROR : Theme.TEXT);

            graphics.fill(x + 1, cy, x + width - 1, cy + 20, itemBg);
            String label = resolveLabel(action);

            font.drawInBatch(label, x + 6, cy + 6, tc, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            cy += 22;
        }

        graphics.flush();
        pose.popPose();
    }

    private String resolveLabel(String action) {
        return switch (action) {
            case "Copy" -> I18n.s("iscript.script.context.copy");
            case "Paste" -> I18n.s("iscript.script.context.paste");
            case "Rename" -> I18n.s("iscript.script.context.rename");
            case "Duplicate" -> I18n.s("iscript.script.context.duplicate");
            case "Delete" -> I18n.s("iscript.script.context.delete");
            default -> action;
        };
    }
}