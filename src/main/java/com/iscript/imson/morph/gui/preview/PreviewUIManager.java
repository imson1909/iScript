package com.iscript.imson.morph.gui.preview;

import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public class PreviewUIManager {
    private final List<PreviewWidget> widgets = new ArrayList<>();

    public void addWidget(PreviewWidget widget) {
        widgets.add(widget);
    }

    public void removeWidget(PreviewWidget widget) {
        widgets.remove(widget);
    }

    public void clear() {
        widgets.clear();
    }

    public void render(GuiGraphics g, int previewX, int previewY, int previewW, int previewH, int mouseX, int mouseY, float pt) {
        for (PreviewWidget widget : widgets) {
            widget.render(g, previewX, previewY, mouseX, mouseY, pt);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int previewX, int previewY, int previewW, int previewH) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).mouseClicked(mouseX, mouseY, button, previewX, previewY)) {
                return true;
            }
        }
        return false;
    }
}