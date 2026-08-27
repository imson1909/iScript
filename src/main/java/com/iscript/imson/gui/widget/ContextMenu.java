package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;

public class ContextMenu {
    private int x, y;
    private int width = 100;
    private int height;
    private int contentHeight;
    private String itemId;
    private boolean open = false;
    private boolean canPaste = false;
    private Consumer<String> actionCallback;
    private String[] customActions = null;
    private String lastAction = null;
    private Map<String, String> labelOverrides = new HashMap<>();
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 22;
    private static final int MAX_HEIGHT = 240;
    private static final int SCROLLBAR_WIDTH = 4;

    public void setCustomActions(String[] actions) {
        this.customActions = actions;
    }

    public void setAction(Consumer<String> callback) {
        this.actionCallback = callback;
    }

    public void setLabelOverrides(Map<String, String> overrides) {
        this.labelOverrides = overrides;
    }

    public void clearCustom() {
        this.customActions = null;
        this.labelOverrides.clear();
    }

    public void open(int x, int y, String itemId, boolean canPaste) {
        this.x = x;
        this.y = y;
        this.itemId = itemId;
        this.canPaste = canPaste;
        this.open = true;
        this.lastAction = null;
        this.scrollOffset = 0;

        String[] actions = getActiveActions();
        this.contentHeight = actions.length * ITEM_HEIGHT + 4;
        this.height = Math.min(contentHeight, MAX_HEIGHT);
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

    public void mouseScrolled(double delta) {
        if (!open || contentHeight <= height) return;
        int maxScroll = contentHeight - height;
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - ITEM_HEIGHT * 3);
        } else {
            scrollOffset = Math.min(maxScroll, scrollOffset + ITEM_HEIGHT * 3);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || button != 0) return false;

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            close();
            return true;
        }

        String[] actions = getActiveActions();
        int localY = (int) mouseY - y - 2 + scrollOffset;
        int idx = localY / ITEM_HEIGHT;

        if (idx >= 0 && idx < actions.length) {
            String action = actions[idx];
            boolean isDisabled = action.equals("Paste") && !canPaste;
            if (!isDisabled) {
                this.lastAction = action;
                if (actionCallback != null) actionCallback.accept(action);
            }
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
        if (contentHeight > height) {
            this.width += SCROLLBAR_WIDTH + 2;
        }

        graphics.flush();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300.0F);

        int solidBg = 0xFF13131A;
        graphics.fill(x, y, x + width, y + height, solidBg);
        graphics.renderOutline(x, y, width, height, Theme.BORDER);

        int startIdx = scrollOffset / ITEM_HEIGHT;
        int endIdx = Math.min(actions.length, (scrollOffset + height) / ITEM_HEIGHT + 1);
        int contentTop = y + 2;
        int drawY = contentTop - (scrollOffset % ITEM_HEIGHT);

        MultiBufferSource.BufferSource buffers = graphics.bufferSource();

        for (int i = startIdx; i < endIdx; i++) {
            String action = actions[i];
            boolean isDisabled = action.equals("Paste") && !canPaste;
            boolean hovered = !isDisabled && mouseX >= x && mouseX <= x + width && mouseY >= drawY && mouseY < drawY + ITEM_HEIGHT;

            int itemBg = hovered ? Theme.BG_HOVER : solidBg;
            int tc = isDisabled ? Theme.TEXT_MUTE : (action.equals("Delete") ? Theme.ERROR : Theme.TEXT);

            if (drawY + ITEM_HEIGHT > y && drawY < y + height) {
                int clipTop = Math.max(y, drawY);
                int clipBottom = Math.min(y + height, drawY + ITEM_HEIGHT);
                graphics.fill(x + 1, clipTop, x + width - 1, clipBottom, itemBg);
                if (clipBottom - clipTop > 8) {
                    String label = resolveLabel(action);
                    font.drawInBatch(label, x + 6, drawY + 6, tc, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                }
            }
            drawY += ITEM_HEIGHT;
        }

        if (contentHeight > height) {
            int sbX = x + width - SCROLLBAR_WIDTH - 2;
            int sbY = y + 2;
            int sbH = height - 4;
            float ratio = (float) height / contentHeight;
            int thumbH = Math.max(10, (int)(sbH * ratio));
            int maxScroll = contentHeight - height;
            int thumbY = sbY + (int)((scrollOffset / (float) maxScroll) * (sbH - thumbH));
            graphics.fill(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + sbH, Theme.BG_INNER);
            graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbH, Theme.TEXT_DIM);
        }

        graphics.flush();
        pose.popPose();
    }

    private String resolveLabel(String action) {
        if (labelOverrides.containsKey(action)) return labelOverrides.get(action);
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