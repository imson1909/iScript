package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.Bone;
import com.iscript.imson.morph.model.GeoModel;
import com.iscript.imson.morph.network.MorphUpdatePacket;
import com.iscript.imson.morph.render.MorphRenderer;
import com.iscript.imson.network.IScriptNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import java.util.*;

public class AlternativeMorphSubScreen extends MorphSubScreen {
    private static final int PADDING = 8;
    private static final int CELL_WIDTH = 110;
    private static final int CELL_HEIGHT = 140;
    private static final int CELL_SPACING_X = 5;
    private static final int CELL_SPACING_Y = 5;
    private static final int HEADER_HEIGHT = 20;
    private static final int CATEGORY_HEIGHT = 16;
    private static final int CATEGORY_SPACING = 5;
    private static final int CONTEXT_ITEM_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 50;

    private final List<MorphSection> sections = new ArrayList<>();
    private String selectedId = "";
    private String searchQuery = "";
    private float morphScale = 1.0f;
    private int gridScroll = 0;
    private int maxGridScroll = 0;
    private boolean isDraggingScrollbar = false;
    private EditBox searchBox;
    private ContextMenu contextMenu;
    private final Map<String, Entity> entityCache = new HashMap<>();

    public AlternativeMorphSubScreen(MorphScreen parent, Player player, MorphData morphData) {
        super(parent, player, morphData);
        if (morphData != null) {
            this.morphScale = morphData.getScale();
            this.selectedId = morphData.getModelId();
        }
        loadSections();
    }

    private void loadSections() {
        sections.clear();
        Map<String, List<String>> vanillaByMod = new LinkedHashMap<>();
        Map<String, List<String>> customByMod = new LinkedHashMap<>();
        List<String> allVanilla = new ArrayList<>();
        List<String> allCustom = new ArrayList<>();
        List<String> allModels = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) continue;
            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (rl == null) continue;
            String id = "entity:" + rl.toString();
            String modid = rl.getNamespace();
            allVanilla.add(id);
            allModels.add(id);
            vanillaByMod.computeIfAbsent(modid, k -> new ArrayList<>()).add(id);
        }

        for (String id : MorphManager.getAllModels().keySet()) {
            String modid = id.contains(":") ? id.substring(0, id.indexOf(':')) : "custom";
            allCustom.add(id);
            allModels.add(id);
            customByMod.computeIfAbsent(modid, k -> new ArrayList<>()).add(id);
        }

        MorphSection mainSection = new MorphSection("");
        mainSection.expanded = true;
        mainSection.addCategory(new MorphCategory(I18n.s("iscript.morph.category.all"), allModels));

        if (!allCustom.isEmpty()) {
            mainSection.addCategory(new MorphCategory(I18n.s("iscript.morph.category.custom"), allCustom));
        }

        Set<String> allMods = new LinkedHashSet<>();
        allMods.addAll(vanillaByMod.keySet());
        allMods.addAll(customByMod.keySet());

        for (String modid : allMods) {
            if (modid.equals("minecraft")) continue;
            List<String> modModels = new ArrayList<>();
            if (vanillaByMod.containsKey(modid)) {
                modModels.addAll(vanillaByMod.get(modid));
            }
            if (customByMod.containsKey(modid)) {
                modModels.addAll(customByMod.get(modid));
            }
            if (!modModels.isEmpty()) {
                mainSection.addCategory(new MorphCategory(modid, modModels));
            }
        }
        sections.add(mainSection);
        applyFilter();
    }

    private void applyFilter() {
        String query = searchQuery.trim().toLowerCase();
        for (MorphSection section : sections) {
            for (MorphCategory cat : section.categories) {
                cat.filteredIds.clear();
                for (String id : cat.originalIds) {
                    String display = getDisplayName(id).toLowerCase();
                    if (query.isEmpty() || id.toLowerCase().contains(query) || display.contains(query)) {
                        cat.filteredIds.add(id);
                    }
                }
            }
        }
        gridScroll = 0;
    }

    private int getCellStepX() { return CELL_WIDTH + CELL_SPACING_X; }
    private int getCellStepY() { return CELL_HEIGHT + CELL_SPACING_Y; }

    private int getGridCols(int width) {
        if (width < CELL_WIDTH) return 1;
        return Math.max(1, (width + CELL_SPACING_X) / getCellStepX());
    }

    private int getCategoryHeight(MorphCategory cat, int gridWidth) {
        if (cat.filteredIds.isEmpty() || !cat.expanded) return 0;
        int cols = getGridCols(gridWidth);
        int rows = (cat.filteredIds.size() + cols - 1) / cols;
        return CATEGORY_HEIGHT + rows * CELL_HEIGHT + (rows - 1) * CELL_SPACING_Y;
    }

    private int getGridContentHeight(int gridWidth) {
        int h = 0;
        for (MorphSection section : sections) {
            for (MorphCategory cat : section.categories) {
                if (!cat.filteredIds.isEmpty()) {
                    h += CATEGORY_HEIGHT;
                    if (cat.expanded) {
                        h += getCategoryHeight(cat, gridWidth);
                    }
                    h += CATEGORY_SPACING;
                }
            }
        }
        return h;
    }

    private void updateMaxScroll(int gridWidth, int visibleHeight) {
        int contentH = getGridContentHeight(gridWidth);
        maxGridScroll = Math.max(0, contentH - visibleHeight);
        gridScroll = Math.max(0, Math.min(maxGridScroll, gridScroll));
    }

    @Override
    public void init() {
        int contentY = parent.getContentY();
        int screenW = parent.width;
        searchBox = new EditBox(font, 12, contentY + PADDING, screenW - 24, 20, Component.literal(I18n.s("iscript.morph.search")));
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
        int contentH = parent.getContentHeight() - BOTTOM_BAR_HEIGHT;
        int screenW = parent.width;
        UI.panel(g, 4, contentY, screenW - 8, contentH);

        int gridY = contentY + PADDING + 24;
        int gridH = contentH - PADDING - 24;
        int gridX = 12;
        int gridW = screenW - 24;

        updateMaxScroll(gridW, gridH);
        renderGrid(g, mx, my, gridX, gridY, gridW, gridH, pt);

        RenderSystem.disableDepthTest();
        renderBottomBar(g, mx, my, 12, contentY + contentH, screenW - 24, BOTTOM_BAR_HEIGHT);

        if (contextMenu != null) {
            contextMenu.render(g, mx, my);
        }
    }

    private void renderGrid(GuiGraphics g, int mx, int my, int x, int y, int w, int h, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF16161E);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        enableScissor(x, y, w, h);

        int currentY = y - gridScroll;
        int leftPadding = 8;

        for (MorphSection section : sections) {
            if (section.expanded) {
                currentY += HEADER_HEIGHT;
                for (MorphCategory cat : section.categories) {
                    if (cat.filteredIds.isEmpty()) continue;
                    boolean catHovered = mx >= x + 8 && mx <= x + w && my >= currentY && my <= currentY + CATEGORY_HEIGHT;
                    int catBg = catHovered ? Theme.BG_HOVER : 0xFF1A1A24;
                    g.fill(x + 8, currentY, x + w, currentY + CATEGORY_HEIGHT, catBg);

                    String catArrow = cat.expanded ? "▼ " : "▶ ";
                    String catText = catArrow + cat.title + " (" + cat.filteredIds.size() + ")";
                    g.drawString(font, catText, x + 12, currentY + 4, Theme.TEXT);
                    currentY += CATEGORY_HEIGHT;

                    if (cat.expanded) {
                        int cols = getGridCols(w);
                        int index = 0;
                        for (String id : cat.filteredIds) {
                            int row = index / cols;
                            int col = index % cols;
                            int cellX = x + leftPadding + col * getCellStepX();
                            int cellY = currentY + row * getCellStepY();

                            if (cellY + CELL_HEIGHT > y && cellY < y + h) {
                                int drawX = cellX;
                                int drawY = Math.max(cellY, y);
                                int drawW = CELL_WIDTH;
                                int drawH = Math.min(cellY + CELL_HEIGHT, y + h) - drawY;
                                enableScissor(drawX, drawY, drawW, drawH);

                                boolean isSelected = id.equals(selectedId);
                                boolean isCurrent = morphData != null && morphData.isMorphed() && id.equals(morphData.getModelId());
                                boolean isHovered = mx >= cellX && mx <= cellX + CELL_WIDTH && my >= cellY && my <= cellY + CELL_HEIGHT;

                                int cellBg = isSelected ? 0xFF334455 : (isHovered ? Theme.BG_HOVER : 0xFF1E1E26);
                                g.fill(cellX, cellY, cellX + CELL_WIDTH, cellY + CELL_HEIGHT, cellBg);
                                g.renderOutline(cellX, cellY, CELL_WIDTH, CELL_HEIGHT, Theme.BORDER);
                                renderModelInCell(g, id, cellX, cellY, CELL_WIDTH, CELL_HEIGHT, pt);

                                if (isCurrent) {
                                    g.renderOutline(cellX + 1, cellY + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2, 0xFF55FF55);
                                }
                                disableScissor();
                            }
                            index++;
                        }
                        int rows = (cat.filteredIds.size() + cols - 1) / cols;
                        currentY += rows * CELL_HEIGHT + (rows - 1) * CELL_SPACING_Y;
                        currentY += CATEGORY_SPACING;
                    }
                }
            } else {
                currentY += HEADER_HEIGHT;
            }
        }

        if (maxGridScroll > 0) {
            int thumbHeight = Math.max(20, h * h / (h + maxGridScroll));
            int thumbY = y + (int) ((float) gridScroll / maxGridScroll * (h - thumbHeight));
            int scrollBarX = x + w - 6;
            g.fill(scrollBarX, y, scrollBarX + 4, y + h, Theme.alpha(Theme.BORDER, 0.2f));
            g.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, Theme.TEXT_DIM);
        }
        disableScissor();
    }

    private void renderModelInCell(GuiGraphics g, String id, int x, int y, int w, int h, float pt) {
        int textHeight = 18;
        int modelH = h - textHeight;

        if (id.startsWith("entity:")) {
            Entity entity = getEntity(id);
            if (entity != null) {
                PoseStack pose = g.pose();
                pose.pushPose();
                float centerX = x + w / 2f;
                float centerY = y + modelH * 0.65f;
                float baseScale = 25f;
                float height = entity.getBbHeight();
                String entityType = id.substring(7);

                if (entityType.equals("minecraft:giant")) {
                    baseScale = 8f;
                    centerY = y + modelH * 0.5f;
                } else if (entityType.equals("minecraft:ender_dragon")) {
                    baseScale = 12f;
                    centerY = y + modelH * 0.55f;
                } else if (entityType.equals("minecraft:warden")) {
                    baseScale = 14f;
                    centerY = y + modelH * 0.55f;
                } else if (height > 2) {
                    baseScale *= 2 / height;
                } else if (height < 0.5) {
                    baseScale *= 0.6 / height;
                }

                float scale = Math.max(8f, Math.min(35f, baseScale));
                float zOffset = 500f;
                pose.translate(centerX, centerY, zOffset);
                pose.scale(scale, -scale, scale);

                if (entityType.equals("minecraft:ender_dragon")) {
                    pose.mulPose(com.mojang.math.Axis.YP.rotation((float) Math.toRadians(180)));
                }
                pose.mulPose(com.mojang.math.Axis.YP.rotation((float) Math.toRadians(20)));

                EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                dispatcher.setRenderShadow(false);
                MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
                RenderSystem.enableDepthTest();
                try {
                    dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 0.0f, pose, buffer, 15728880);
                    buffer.endBatch();
                } catch (Exception e) {}
                RenderSystem.disableDepthTest();
                dispatcher.setRenderShadow(true);
                pose.popPose();
            }
        } else {
            PoseStack pose = g.pose();
            pose.pushPose();
            float centerX = x + w / 2f;
            float centerY = y + modelH * 0.65f;
            float scale = 45f;
            float zOffset = 500f + (scale * 2f);
            pose.translate(centerX, centerY, zOffset);
            pose.scale(scale, -scale, scale);
            pose.mulPose(com.mojang.math.Axis.YP.rotation((float) Math.toRadians(20)));

            GeoModel model = MorphManager.getModel(id);
            if (model != null) {
                MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
                ResourceLocation texture = MorphRenderer.getTextureLocation(id);
                if (texture == null) texture = new ResourceLocation("minecraft", "textures/block/stone.png");
                com.mojang.blaze3d.vertex.VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
                RenderSystem.enableDepthTest();
                try {
                    for (Bone bone : model.getBones()) {
                        if (bone.getParent().isEmpty()) {
                            MorphRenderer.renderBone(bone, model, null, 0.0f, pose, builder, 15728880, OverlayTexture.NO_OVERLAY, 1.0f, null, pt);
                        }
                    }
                    buffer.endBatch();
                } catch (Exception e) {}
                RenderSystem.disableDepthTest();
            }
            pose.popPose();
        }

        g.fill(x, y + modelH, x + w, y + h, 0xFF1E1E26);
        String displayName = getDisplayName(id);
        String truncated = truncateText(displayName, w - 8);
        int textW = font.width(truncated);
        int textX = x + (w - textW) / 2;
        int textY = y + modelH + 4;
        g.drawString(font, truncated, textX, textY, Theme.TEXT);
    }

    private String truncateText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 0 && font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private void renderBottomBar(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        UI.panel(g, x, y, w, h);
        int btnHeight = 24;
        int btnSpacing = 6;
        int btnY = y + (h - btnHeight) / 2;
        int btnWidth = 80;

        int leftX = x + PADDING;
        int acquireX = leftX;
        int closeX = acquireX + btnWidth + btnSpacing;
        int copyX = closeX + btnWidth + btnSpacing;
        int resetX = copyX + btnWidth + btnSpacing;

        int rightX = x + w - PADDING - btnWidth;
        int editX = rightX;

        int scaleAreaStart = resetX + btnWidth + btnSpacing;
        int scaleAreaEnd = editX - btnSpacing;
        int scaleAreaWidth = scaleAreaEnd - scaleAreaStart;
        int scaleCenterX = scaleAreaStart + scaleAreaWidth / 2;

        String scaleText = String.format("%.1fx", morphScale);
        int textW = font.width(scaleText);
        int totalScaleW = btnHeight + 4 + textW + 8 + 12 + btnHeight;
        int scaleStartX = scaleCenterX - totalScaleW / 2;

        int minusX = scaleStartX;
        boolean minusHovered = mx >= minusX && mx <= minusX + btnHeight && my >= btnY && my <= btnY + btnHeight;
        g.fill(minusX, btnY, minusX + btnHeight, btnY + btnHeight, minusHovered ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(minusX, btnY, btnHeight, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, "-", minusX + btnHeight / 2, btnY + 8, Theme.TEXT);

        int textX = minusX + btnHeight + 4;
        g.fill(textX, btnY, textX + textW + 8, btnY + btnHeight, 0xFF1E1E26);
        g.renderOutline(textX, btnY, textW + 8, btnHeight, Theme.BORDER);
        g.drawString(font, scaleText, textX + 4, btnY + 8, Theme.ACCENT);

        int plusX = textX + textW + 12;
        boolean plusHovered = mx >= plusX && mx <= plusX + btnHeight && my >= btnY && my <= btnY + btnHeight;
        g.fill(plusX, btnY, plusX + btnHeight, btnY + btnHeight, plusHovered ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(plusX, btnY, btnHeight, btnHeight, Theme.BORDER);
        g.drawCenteredString(font, "+", plusX + btnHeight / 2, btnY + 8, Theme.TEXT);

        renderButton(g, mx, my, acquireX, btnY, btnWidth, btnHeight, I18n.s("iscript.morph.acquire"));
        renderButton(g, mx, my, closeX, btnY, btnWidth, btnHeight, I18n.s("iscript.morph.close"));
        renderButton(g, mx, my, copyX, btnY, btnWidth, btnHeight, "Copy");
        renderButton(g, mx, my, resetX, btnY, btnWidth, btnHeight, "Reset");
        renderButton(g, mx, my, editX, btnY, btnWidth, btnHeight, "Редактировать");
    }

    private void renderButton(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bgColor = hovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(x, y, x + w, y + h, bgColor);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(font, text, x + w / 2, y + 8, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (contextMenu != null) {
            if (contextMenu.handleClick(mx, my, btn)) return true;
            contextMenu = null;
        }

        int contentY = parent.getContentY();
        int contentH = parent.getContentHeight() - BOTTOM_BAR_HEIGHT;
        int screenW = parent.width;

        int btnY = contentY + contentH + (BOTTOM_BAR_HEIGHT - 24) / 2;
        int btnHeight = 24;
        int btnWidth = 80;
        int btnSpacing = 6;

        int leftX = 12 + PADDING;
        int acquireX = leftX;
        int closeX = acquireX + btnWidth + btnSpacing;
        int copyX = closeX + btnWidth + btnSpacing;
        int resetX = copyX + btnWidth + btnSpacing;

        int rightX = 12 + screenW - 24 - PADDING - btnWidth;
        int editX = rightX;

        int scaleAreaStart = resetX + btnWidth + btnSpacing;
        int scaleAreaEnd = editX - btnSpacing;
        int scaleAreaWidth = scaleAreaEnd - scaleAreaStart;
        int scaleCenterX = scaleAreaStart + scaleAreaWidth / 2;

        String scaleText = String.format("%.1fx", morphScale);
        int textW = font.width(scaleText);
        int totalScaleW = btnHeight + 4 + textW + 8 + 12 + btnHeight;
        int scaleStartX = scaleCenterX - totalScaleW / 2;

        int minusX = scaleStartX;
        int textX = minusX + btnHeight + 4;
        int plusX = textX + textW + 12;

        if (mx >= acquireX && mx <= acquireX + btnWidth && my >= btnY && my <= btnY + btnHeight) {
            acquireMorph();
            return true;
        }
        if (mx >= closeX && mx <= closeX + btnWidth && my >= btnY && my <= btnY + btnHeight) {
            parent.onClose();
            return true;
        }
        if (mx >= copyX && mx <= copyX + btnWidth && my >= btnY && my <= btnY + btnHeight) {
            mc.keyboardHandler.setClipboard(selectedId);
            return true;
        }
        if (mx >= resetX && mx <= resetX + btnWidth && my >= btnY && my <= btnY + btnHeight) {
            morphScale = 1.0f;
            return true;
        }
        if (mx >= editX && mx <= editX + btnWidth && my >= btnY && my <= btnY + btnHeight) {
            if (!selectedId.isEmpty()) {
                mc.setScreen(new MorphEditorScreen(parent, selectedId, morphData));
            }
            return true;
        }

        if (mx >= minusX && mx <= minusX + btnHeight && my >= btnY && my <= btnY + btnHeight) {
            morphScale = Math.max(0.1f, morphScale - 0.1f);
            return true;
        }
        if (mx >= plusX && mx <= plusX + btnHeight && my >= btnY && my <= btnY + btnHeight) {
            morphScale = Math.min(10.0f, morphScale + 0.1f);
            return true;
        }

        int gridY = contentY + PADDING + 24;
        int gridH = contentH - PADDING - 24;
        int gridX = 12;
        int gridW = screenW - 24;

        if (mx >= gridX && mx <= gridX + gridW && my >= gridY && my <= gridY + gridH) {
            if (maxGridScroll > 0) {
                int thumbHeight = Math.max(20, gridH * gridH / (gridH + maxGridScroll));
                int thumbY = gridY + (int) ((float) gridScroll / maxGridScroll * (gridH - thumbHeight));
                int scrollBarX = gridX + gridW - 6;
                if (mx >= scrollBarX && mx <= scrollBarX + 4 && my >= thumbY && my <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }

            int currentY = gridY - gridScroll;
            for (MorphSection section : sections) {
                for (MorphCategory cat : section.categories) {
                    if (cat.filteredIds.isEmpty()) continue;
                    if (mx >= gridX + 8 && mx <= gridX + gridW && my >= currentY && my <= currentY + CATEGORY_HEIGHT) {
                        cat.expanded = !cat.expanded;
                        return true;
                    }
                    currentY += CATEGORY_HEIGHT;
                    if (cat.expanded) {
                        int cols = getGridCols(gridW);
                        int leftPadding = 8;
                        int index = 0;
                        for (String id : cat.filteredIds) {
                            int row = index / cols;
                            int col = index % cols;
                            int cellX = gridX + leftPadding + col * getCellStepX();
                            int cellY = currentY + row * getCellStepY();
                            if (mx >= cellX && mx <= cellX + CELL_WIDTH && my >= cellY && my <= cellY + CELL_HEIGHT) {
                                if (btn == 0) {
                                    selectedId = id;
                                    return true;
                                } else if (btn == 1) {
                                    showContextMenu(mx, my, id);
                                    return true;
                                }
                            }
                            index++;
                        }
                        int rows = (cat.filteredIds.size() + cols - 1) / cols;
                        currentY += rows * CELL_HEIGHT + (rows - 1) * CELL_SPACING_Y;
                        currentY += CATEGORY_SPACING;
                    }
                }
            }
        }
        return false;
    }

    private void showContextMenu(double mx, double my, String id) {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(new ContextMenuItem(I18n.s("iscript.morph.acquire"), () -> {
            selectedId = id;
            acquireMorph();
        }));
        items.add(new ContextMenuItem(I18n.s("iscript.morph.copy_id"), () -> mc.keyboardHandler.setClipboard(id)));
        contextMenu = new ContextMenu(font, (int) mx, (int) my, items);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        isDraggingScrollbar = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScrollbar && btn == 0) {
            int contentY = parent.getContentY();
            int contentH = parent.getContentHeight() - BOTTOM_BAR_HEIGHT;
            int screenW = parent.width;
            int gridY = contentY + PADDING + 24;
            int gridH = contentH - PADDING - 24;
            int gridX = 12;
            int gridW = screenW - 24;

            if (maxGridScroll > 0) {
                int thumbHeight = Math.max(20, gridH * gridH / (gridH + maxGridScroll));
                float ratio = (float) (my - gridY - thumbHeight / 2.0) / (gridH - thumbHeight);
                ratio = Math.max(0, Math.min(1, ratio));
                gridScroll = (int) (ratio * maxGridScroll);
                gridScroll = Math.max(0, Math.min(maxGridScroll, gridScroll));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int contentY = parent.getContentY();
        int contentH = parent.getContentHeight() - BOTTOM_BAR_HEIGHT;
        int screenW = parent.width;
        int gridY = contentY + PADDING + 24;
        int gridH = contentH - PADDING - 24;
        int gridX = 12;
        int gridW = screenW - 24;

        if (mx >= gridX && mx <= gridX + gridW && my >= gridY && my <= gridY + gridH) {
            if (delta > 0) gridScroll = Math.max(0, gridScroll - getCellStepY());
            else gridScroll = Math.min(maxGridScroll, gridScroll + getCellStepY());
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
        contextMenu = null;
        entityCache.clear();
    }

    private void enableScissor(int x, int y, int width, int height) {
        double scale = mc.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (x * scale), (int) (mc.getWindow().getHeight() - (y + height) * scale), (int) (width * scale), (int) (height * scale));
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    private String getDisplayName(String id) {
        if (id.startsWith("entity:")) {
            String rl = id.substring(7);
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
                if (type != null) return type.getDescription().getString();
            } catch (Exception e) {}
            return rl;
        }
        return id;
    }

    private Entity getEntity(String id) {
        if (entityCache.containsKey(id)) {
            Entity e = entityCache.get(id);
            if (e != null && !e.isRemoved()) return e;
        }
        String rl = id.substring(7);
        try {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
            if (type != null && mc.level != null) {
                Entity entity = type.create(mc.level);
                if (entity instanceof Mob mob) mob.setNoAi(true);
                if (entity instanceof LivingEntity living) {
                    living.setYRot(0f);
                    living.setXRot(0f);
                    living.yHeadRot = 0f;
                    living.yBodyRot = 0f;
                }
                entityCache.put(id, entity);
                return entity;
            }
        } catch (Exception e) {}
        entityCache.put(id, null);
        return null;
    }

    private static class MorphSection {
        final String title;
        final List<MorphCategory> categories = new ArrayList<>();
        boolean expanded = true;
        MorphSection(String title) { this.title = title; }
        void addCategory(MorphCategory cat) { categories.add(cat); }
    }

    private static class MorphCategory {
        final String title;
        final List<String> originalIds;
        final List<String> filteredIds = new ArrayList<>();
        boolean expanded = true;
        MorphCategory(String title, List<String> ids) {
            this.title = title;
            this.originalIds = new ArrayList<>(ids);
            this.filteredIds.addAll(ids);
        }
    }

    private static class ContextMenu {
        final int x, y;
        final List<ContextMenuItem> items;
        final net.minecraft.client.gui.Font font;
        int width = 120, height;

        ContextMenu(net.minecraft.client.gui.Font font, int x, int y, List<ContextMenuItem> items) {
            this.font = font;
            this.x = x;
            this.y = y;
            this.items = items;
            this.height = items.size() * CONTEXT_ITEM_HEIGHT + 4;
        }

        void render(GuiGraphics g, int mx, int my) {
            g.fill(x, y, x + width, y + height, 0xFF1E1E26);
            g.renderOutline(x, y, width, height, Theme.BORDER);
            for (int i = 0; i < items.size(); i++) {
                ContextMenuItem item = items.get(i);
                int itemY = y + 2 + i * CONTEXT_ITEM_HEIGHT;
                boolean hovered = mx >= x && mx <= x + width && my >= itemY && my <= itemY + CONTEXT_ITEM_HEIGHT;
                if (hovered) g.fill(x + 1, itemY, x + width - 1, itemY + CONTEXT_ITEM_HEIGHT, Theme.BG_HOVER);
                g.drawString(font, item.label, x + 6, itemY + 6, Theme.TEXT);
            }
        }

        boolean handleClick(double mx, double my, int btn) {
            if (mx < x || mx > x + width || my < y || my > y + height) return false;
            for (int i = 0; i < items.size(); i++) {
                int itemY = y + 2 + i * CONTEXT_ITEM_HEIGHT;
                if (my >= itemY && my <= itemY + CONTEXT_ITEM_HEIGHT) {
                    items.get(i).action.run();
                    return true;
                }
            }
            return false;
        }
    }

    private static class ContextMenuItem {
        final String label;
        final Runnable action;
        ContextMenuItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}