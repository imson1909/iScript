package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.theme.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropdownMenu {
    private int x, y, width;
    private int height;
    private int contentHeight;
    private boolean open = false;
    private String selected = "";
    private Consumer<String> onSelect;
    private List<String> items = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int MAX_HEIGHT = 200;
    private static final int SCROLLBAR_WIDTH = 4;

    public void setItems(List<String> items) {
        this.items = new ArrayList<>(items);
    }

    public void setOnSelect(Consumer<String> callback) {
        this.onSelect = callback;
    }

    public void open(int x, int y, int width, String selected) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.selected = selected != null ? selected : "";
        this.open = true;
        this.scrollOffset = 0;
        this.contentHeight = items.size() * ITEM_HEIGHT + 4;
        this.height = Math.min(contentHeight, MAX_HEIGHT);
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
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
        int localY = (int) mouseY - y - 2 + scrollOffset;
        int idx = localY / ITEM_HEIGHT;
        if (idx >= 0 && idx < items.size()) {
            if (onSelect != null) onSelect.accept(items.get(idx));
        }
        close();
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!open || items.isEmpty()) return;

        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300.0F);

        int solidBg = 0xFF13131A;
        graphics.fill(x, y, x + width, y + height, solidBg);
        graphics.renderOutline(x, y, width, height, Theme.BORDER);

        int startIdx = scrollOffset / ITEM_HEIGHT;
        int endIdx = Math.min(items.size(), (scrollOffset + height) / ITEM_HEIGHT + 1);
        int drawY = y + 2 - (scrollOffset % ITEM_HEIGHT);

        MultiBufferSource.BufferSource buffers = graphics.bufferSource();

        for (int i = startIdx; i < endIdx; i++) {
            String item = items.get(i);
            boolean isSelected = item.equals(selected);
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= drawY && mouseY < drawY + ITEM_HEIGHT;
            int itemBg = isSelected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : solidBg);
            int tc = isSelected ? Theme.ACCENT : Theme.TEXT;

            if (drawY + ITEM_HEIGHT > y && drawY < y + height) {
                int clipTop = Math.max(y, drawY);
                int clipBottom = Math.min(y + height, drawY + ITEM_HEIGHT);
                graphics.fill(x + 1, clipTop, x + width - 1, clipBottom, itemBg);
                if (clipBottom - clipTop > 8) {
                    font.drawInBatch(item, x + 6, drawY + 6, tc, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
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
}