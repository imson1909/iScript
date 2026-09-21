package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.network.MorphUpdatePacket;
import com.iscript.imson.network.IScriptNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import java.util.*;

public class ClassicMorphSubScreen extends MorphSubScreen {
    private static final int ITEM_HEIGHT = 18;
    private static final int PADDING = 8;
    private List<String> modelIds = new ArrayList<>();
    private Map<String, List<String>> originalCategories = new LinkedHashMap<>();
    private Map<String, List<String>> categories = new LinkedHashMap<>();
    private Set<String> expandedCategories = new HashSet<>();
    private String searchQuery = "";
    private String selectedId = "";
    private float morphScale = 1.0f;
    private boolean isDraggingScrollbar = false;
    private int listScroll = 0;
    private int maxScroll = 0;
    private EditBox searchBox;
    private ModelPreviewScreen previewScreen;

    public ClassicMorphSubScreen(MorphScreen parent, Player player, MorphData morphData) {
        super(parent, player, morphData);
        if (morphData != null) {
            this.morphScale = morphData.getScale();
            this.selectedId = morphData.getModelId();
        }
        this.previewScreen = new ModelPreviewScreen();
        this.previewScreen.setSelectedId(selectedId);
        loadModels();
    }

    private int getLeftPanelWidth() {
        return parent.getScreenLeftPanelWidth();
    }

    private int getRightPanelWidth() {
        return parent.getScreenRightPanelWidth();
    }

    private int getCenterWidth() {
        return parent.getScreenCenterWidth();
    }

    private void loadModels() {
        modelIds.clear();
        categories.clear();
        originalCategories.clear();
        List<String> customModels = new ArrayList<>(MorphManager.getAllModels().keySet());
        List<String> vanillaModels = new ArrayList<>();
        Map<String, List<String>> modEntityMap = new LinkedHashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) continue;
            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (rl == null) continue;
            String id = "entity:" + rl.toString();
            vanillaModels.add(id);
            modelIds.add(id);
            String modid = rl.getNamespace();
            if (!modid.equals("minecraft")) {
                modEntityMap.computeIfAbsent(modid, k -> new ArrayList<>()).add(id);
            }
        }
        modelIds.addAll(customModels);
        for (String id : customModels) {
            String modid = "custom";
            if (id.contains(":")) {
                modid = id.substring(0, id.indexOf(':'));
            }
            if (!modid.equals("entity") && !modid.equals("custom")) {
                modEntityMap.computeIfAbsent(modid, k -> new ArrayList<>()).add(id);
            }
        }
        String allCat = I18n.s("iscript.morph.category.all");
        String vanillaCat = I18n.s("iscript.morph.category.vanilla");
        String customCat = I18n.s("iscript.morph.category.custom");
        originalCategories.put(allCat, new ArrayList<>(modelIds));
        if (!vanillaModels.isEmpty()) {
            originalCategories.put(vanillaCat, new ArrayList<>(vanillaModels));
        }
        if (!customModels.isEmpty()) {
            List<String> ungroupedCustom = new ArrayList<>();
            for (String id : customModels) {
                String modid = id.contains(":") ? id.substring(0, id.indexOf(':')) : "custom";
                if (modid.equals("custom") || modid.equals("entity")) {
                    ungroupedCustom.add(id);
                }
            }
            if (!ungroupedCustom.isEmpty()) {
                originalCategories.put(customCat, ungroupedCustom);
            }
        }
        for (Map.Entry<String, List<String>> entry : modEntityMap.entrySet()) {
            originalCategories.put(entry.getKey(), entry.getValue());
        }
        categories.putAll(originalCategories);
        expandedCategories.add(allCat);
        applyFilter();
    }

    private void applyFilter() {
        categories.clear();
        String query = searchQuery.trim().toLowerCase();
        for (Map.Entry<String, List<String>> entry : originalCategories.entrySet()) {
            List<String> filtered = new ArrayList<>();
            for (String id : entry.getValue()) {
                String displayName = getDisplayName(id).toLowerCase();
                if (query.isEmpty() || id.toLowerCase().contains(query) || displayName.contains(query)) {
                    filtered.add(id);
                }
            }
            categories.put(entry.getKey(), filtered);
        }
        listScroll = 0;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        int totalHeight = getTotalListHeight();
        int visibleHeight = getListVisibleHeight();
        maxScroll = Math.max(0, totalHeight - visibleHeight);
        listScroll = Math.max(0, Math.min(maxScroll, listScroll));
    }

    private int getTotalListHeight() {
        int height = 0;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                height += ITEM_HEIGHT;
                if (expandedCategories.contains(entry.getKey())) {
                    height += entry.getValue().size() * ITEM_HEIGHT;
                }
            }
        }
        return height;
    }

    private int getListVisibleHeight() {
        int leftH = parent.getContentHeight() - PADDING * 2;
        return leftH - PADDING * 2 - 16;
    }

    @Override
    public void init() {
        int contentY = parent.getContentY();
        int leftW = getLeftPanelWidth();
        int searchY = contentY + PADDING + PADDING;
        int searchW = leftW - PADDING * 2;
        searchBox = new EditBox(font, PADDING, searchY, Math.max(80, searchW), 16, Component.literal(I18n.s("iscript.morph.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);
        parent.addWidget(searchBox);
    }

    private void onSearchChanged(String query) {
        searchQuery = query;
        applyFilter();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int contentY = parent.getContentY();
        int leftW = getLeftPanelWidth();
        int rightW = getRightPanelWidth();
        int centerW = getCenterWidth();
        int leftX = PADDING;
        int leftY = contentY + PADDING;
        int leftH = parent.getContentHeight() - PADDING * 2;
        int rightX = parent.width - rightW - PADDING;
        int rightY = leftY;
        int rightH = leftH;
        int centerX = leftX + leftW + PADDING;
        int centerY = leftY;
        int centerH = leftH;
        UI.panel(g, leftX, leftY, leftW, leftH);
        UI.panel(g, centerX, centerY, centerW, centerH);
        UI.panel(g, rightX, rightY, rightW, rightH);
        renderCategoryList(g, mx, my, leftX, leftY, leftW, leftH);
        previewScreen.render(g, centerX, centerY, centerW, centerH, mx, my, pt);
        renderRightPanel(g, rightX, rightY, rightW, rightH, mx, my);
    }

    private void renderCategoryList(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        UI.title(g, font, I18n.s("iscript.morph.categories"), x + PADDING, y + PADDING);
        int listY = y + PADDING + 16;
        int listH = h - PADDING * 2 - 16;
        int listX = x + PADDING;
        int listW = w - PADDING * 2 - 8;
        enableScissor(listX, listY, listW, listH);
        try {
            int currentY = listY - listScroll;
            for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
                String catName = entry.getKey();
                List<String> catModels = entry.getValue();
                if (catModels.isEmpty()) continue;
                boolean isExpanded = expandedCategories.contains(catName);
                boolean isHovered = mx >= listX && mx <= listX + listW && my >= currentY && my <= currentY + ITEM_HEIGHT
                        && currentY + ITEM_HEIGHT >= listY && currentY <= listY + listH;
                int bgColor = isHovered ? Theme.BG_HOVER : 0x00000000;
                g.fill(listX, currentY, listX + listW, currentY + ITEM_HEIGHT, bgColor);
                String catText = (isExpanded ? "▼ " : "▶ ") + catName + " (" + catModels.size() + ")";
                String truncatedCat = truncateText(catText, listW - 8);
                g.drawString(font, truncatedCat, listX + 4, currentY + 4, Theme.ACCENT);
                currentY += ITEM_HEIGHT;
                if (isExpanded) {
                    for (String id : catModels) {
                        boolean isSelected = id.equals(selectedId);
                        boolean isCurrent = morphData != null && morphData.isMorphed() && id.equals(morphData.getModelId());
                        boolean rowHovered = mx >= listX + 12 && mx <= listX + listW && my >= currentY && my <= currentY + ITEM_HEIGHT
                                && currentY + ITEM_HEIGHT >= listY && currentY <= listY + listH;
                        int rowBg = isSelected ? 0xFF334455 : (rowHovered ? Theme.BG_HOVER : 0x00000000);
                        g.fill(listX + 12, currentY, listX + listW, currentY + ITEM_HEIGHT, rowBg);
                        String displayName = getDisplayName(id);
                        if (isCurrent) displayName = "[Active] " + displayName;
                        String truncatedName = truncateText(displayName, listW - 20);
                        int color = isCurrent ? 0xFF55FF55 : (isSelected ? Theme.ACCENT : Theme.TEXT);
                        g.drawString(font, truncatedName, listX + 16, currentY + 4, color);
                        currentY += ITEM_HEIGHT;
                    }
                }
            }
        } finally {
            disableScissor();
        }
        if (maxScroll > 0) {
            int thumbHeight = Math.max(20, listH * listH / (listH + maxScroll));
            int thumbY = listY + (int) ((float) listScroll / maxScroll * (listH - thumbHeight));
            int scrollBarX = x + w - 6;
            g.fill(scrollBarX, listY, scrollBarX + 4, listY + listH, Theme.alpha(Theme.BORDER, 0.2f));
            g.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, Theme.TEXT_DIM);
        }
    }

    private void enableScissor(int x, int y, int width, int height) {
        double scale = mc.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale),
                (int) (mc.getWindow().getHeight() - (y + height) * scale),
                (int) (width * scale),
                (int) (height * scale)
        );
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    private String truncateText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 0 && font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private String getDisplayName(String id) {
        if (id.startsWith("entity:")) {
            String rl = id.substring(7);
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
                if (type != null) return type.getDescription().getString();
            } catch (Exception e) {
            }
            return rl;
        }
        return id;
    }

    private void renderRightPanel(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        UI.title(g, font, I18n.s("iscript.morph.controls"), x + PADDING, y + PADDING);
        renderMorphScaleControl(g, x, y, w, mx, my);
        renderActionButtons(g, x, y, w, h, mx, my);
    }

    private void renderMorphScaleControl(GuiGraphics g, int x, int y, int w, int mx, int my) {
        int controlY = y + PADDING + 32;
        UI.title(g, font, I18n.s("iscript.morph.morph_size"), x + PADDING, controlY);
        int btnY = controlY + 16;
        int btnWidth = Math.max(20, (w - PADDING * 2 - 10) / 3);
        int btnHeight = 16;
        int minusX = x + PADDING;
        boolean minusHovered = mx >= minusX && mx <= minusX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight;
        int minusColor = minusHovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(minusX, btnY, minusX + btnWidth, btnY + btnHeight, minusColor);
        g.renderOutline(minusX, btnY, btnWidth, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, "-", minusX + btnWidth / 2, btnY + 4, Theme.TEXT);
        String scaleText = String.format("%.1fx", morphScale);
        int textW = font.width(scaleText);
        int textX = minusX + btnWidth + 5;
        g.fill(textX - 2, btnY, textX + textW + 2, btnY + btnHeight, 0xFF1E1E26);
        g.renderOutline(textX - 2, btnY, textW + 4, btnHeight, Theme.BORDER);
        g.drawString(font, scaleText, textX, btnY + 4, Theme.ACCENT);
        int plusX = textX + textW + 7;
        boolean plusHovered = mx >= plusX && mx <= plusX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight;
        int plusColor = plusHovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(plusX, btnY, plusX + btnWidth, btnY + btnHeight, plusColor);
        g.renderOutline(plusX, btnY, btnWidth, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, "+", plusX + btnWidth / 2, btnY + 4, Theme.TEXT);
    }

    private void renderActionButtons(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int btnHeight = 20;
        int btnSpacing = 6;
        int btnWidth = (w - PADDING * 2 - btnSpacing) / 2;
        int btnY = y + h - btnHeight - PADDING;
        int acquireX = x + PADDING;
        boolean acquireHovered = mx >= acquireX && mx <= acquireX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight;
        int acquireColor = acquireHovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(acquireX, btnY, acquireX + btnWidth, btnY + btnHeight, acquireColor);
        g.renderOutline(acquireX, btnY, btnWidth, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, I18n.s("iscript.morph.acquire"), acquireX + btnWidth / 2, btnY + 6, Theme.TEXT);
        int closeX = acquireX + btnWidth + btnSpacing;
        boolean closeHovered = mx >= closeX && mx <= closeX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight;
        int closeColor = closeHovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(closeX, btnY, closeX + btnWidth, btnY + btnHeight, closeColor);
        g.renderOutline(closeX, btnY, btnWidth, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, I18n.s("iscript.morph.close"), closeX + btnWidth / 2, btnY + 6, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int rightW = getRightPanelWidth();
        int rightX = parent.width - rightW - PADDING;
        int rightY = parent.getContentY() + PADDING;
        int rightH = parent.getContentHeight() - PADDING * 2;
        int btnHeight = 20;
        int btnSpacing = 6;
        int btnWidth = (rightW - PADDING * 2 - btnSpacing) / 2;
        int btnY = rightY + rightH - btnHeight - PADDING;
        int acquireX = rightX + PADDING;
        if (mx >= acquireX && mx <= acquireX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight) {
            acquireMorph();
            return true;
        }
        int closeX = acquireX + btnWidth + btnSpacing;
        if (mx >= closeX && mx <= closeX + btnWidth &&
                my >= btnY && my <= btnY + btnHeight) {
            parent.onClose();
            return true;
        }
        int scaleControlY = rightY + PADDING + 48;
        int scaleBtnWidth = Math.max(20, (rightW - PADDING * 2 - 10) / 3);
        int minusX = rightX + PADDING;
        String scaleText = String.format("%.1fx", morphScale);
        int textW = font.width(scaleText);
        int textX = minusX + scaleBtnWidth + 5;
        int plusX = textX + textW + 7;
        if (mx >= minusX && mx <= minusX + scaleBtnWidth &&
                my >= scaleControlY && my <= scaleControlY + 16) {
            morphScale = Math.max(0.1f, morphScale - 0.1f);
            return true;
        }
        if (mx >= plusX && mx <= plusX + scaleBtnWidth &&
                my >= scaleControlY && my <= scaleControlY + 16) {
            morphScale = Math.min(10.0f, morphScale + 0.1f);
            return true;
        }
        int leftW = getLeftPanelWidth();
        int leftX = PADDING;
        int leftY = parent.getContentY() + PADDING;
        int leftH = parent.getContentHeight() - PADDING * 2;
        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            int listY = leftY + PADDING + 16;
            int listH = leftH - PADDING * 2 - 16;
            if (maxScroll > 0) {
                int thumbHeight = Math.max(20, listH * listH / (listH + maxScroll));
                int thumbY = listY + (int) ((float) listScroll / maxScroll * (listH - thumbHeight));
                int scrollBarX = leftX + leftW - 6;
                if (mx >= scrollBarX && mx <= scrollBarX + 4 && my >= thumbY && my <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }
            if (handleCategoryClick(mx, my, leftX, leftY, leftW, leftH)) {
                return true;
            }
        }
        int centerX = leftX + leftW + PADDING;
        int centerY = leftY;
        int centerW = getCenterWidth();
        int centerH = leftH;
        if (previewScreen.mouseClicked(mx, my, btn, centerX, centerY, centerW, centerH)) {
            return true;
        }
        return false;
    }

    private boolean handleCategoryClick(double mx, double my, int x, int y, int w, int h) {
        int listY = y + PADDING + 16;
        int listH = h - PADDING * 2 - 16;
        int listX = x + PADDING;
        int listW = w - PADDING * 2 - 8;
        int currentY = listY - listScroll;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            String catName = entry.getKey();
            List<String> catModels = entry.getValue();
            if (catModels.isEmpty()) continue;
            if (mx >= listX && mx <= listX + listW && my >= currentY && my <= currentY + ITEM_HEIGHT
                    && currentY + ITEM_HEIGHT >= listY && currentY <= listY + listH) {
                if (expandedCategories.contains(catName)) {
                    expandedCategories.remove(catName);
                } else {
                    expandedCategories.add(catName);
                }
                updateMaxScroll();
                return true;
            }
            currentY += ITEM_HEIGHT;
            if (expandedCategories.contains(catName)) {
                for (String id : catModels) {
                    if (mx >= listX + 12 && mx <= listX + listW && my >= currentY && my <= currentY + ITEM_HEIGHT
                            && currentY + ITEM_HEIGHT >= listY && currentY <= listY + listH) {
                        selectedId = id;
                        previewScreen.setSelectedId(id);
                        return true;
                    }
                    currentY += ITEM_HEIGHT;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        isDraggingScrollbar = false;
        previewScreen.mouseReleased(mx, my, btn);
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScrollbar && btn == 0) {
            int leftW = getLeftPanelWidth();
            int leftX = PADDING;
            int leftY = parent.getContentY() + PADDING;
            int leftH = parent.getContentHeight() - PADDING * 2;
            int listY = leftY + PADDING + 16;
            int listH = leftH - PADDING * 2 - 16;
            if (maxScroll > 0) {
                int thumbHeight = Math.max(20, listH * listH / (listH + maxScroll));
                float ratio = (float) (my - listY - thumbHeight / 2.0) / (listH - thumbHeight);
                ratio = Math.max(0, Math.min(1, ratio));
                listScroll = (int) (ratio * maxScroll);
                listScroll = Math.max(0, Math.min(maxScroll, listScroll));
            }
            return true;
        }
        if (previewScreen.mouseDragged(mx, my, btn, dx, dy)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int leftW = getLeftPanelWidth();
        int leftX = PADDING;
        int leftY = parent.getContentY() + PADDING;
        int leftH = parent.getContentHeight() - PADDING * 2;
        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            if (delta > 0) {
                listScroll = Math.max(0, listScroll - ITEM_HEIGHT);
            } else {
                listScroll = Math.min(maxScroll, listScroll + ITEM_HEIGHT);
            }
            return true;
        }
        int centerX = leftX + leftW + PADDING;
        int centerY = leftY;
        int centerW = getCenterWidth();
        int centerH = leftH;
        if (previewScreen.mouseScrolled(mx, my, delta, centerX, centerY, centerW, centerH)) {
            return true;
        }
        return false;
    }

    private void acquireMorph() {
        if (selectedId.isEmpty()) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelId", selectedId);
        tag.putString("TextureId", selectedId);
        tag.putFloat("Scale", morphScale);
        tag.putBoolean("Morphed", true);
        IScriptNetwork.sendToServer(new MorphUpdatePacket(tag));
        parent.onClose();
    }

    @Override
    public void onClose() {
        previewScreen.onClose();
    }
}