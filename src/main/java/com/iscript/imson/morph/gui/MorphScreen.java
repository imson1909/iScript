package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.gui.widget.DropdownMenu;
import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.Bone;
import com.iscript.imson.morph.model.GeoModel;
import com.iscript.imson.morph.network.MorphUpdatePacket;
import com.iscript.imson.morph.render.MorphRenderer;
import com.iscript.imson.network.IScriptNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;

import java.lang.reflect.Field;
import java.util.*;

public class MorphScreen extends Screen {
    private static final int LEFT_PANEL_WIDTH = 200;
    private static final int RIGHT_PANEL_WIDTH = 180;
    private static final int ITEM_HEIGHT = 18;
    private static final int PADDING = 8;
    private List<String> animationKeys = new ArrayList<>();
    private float swingAccumulator = 0;
    private static final Map<String, String> STATE_DISPLAY_NAMES = new HashMap<>();

    private final Player player;
    private MorphData morphData;
    private List<String> modelIds = new ArrayList<>();
    private Map<String, List<String>> originalCategories = new LinkedHashMap<>();
    private Map<String, List<String>> categories = new LinkedHashMap<>();
    private Set<String> expandedCategories = new HashSet<>();
    private String searchQuery = "";
    private String selectedId = "";
    private float scaleValue = 1.0f;
    private boolean isVisible = true;
    private String selectedAnimation = "";
    private boolean isAnimationPlaying = false;
    private int animationTick = 0;
    private float modelYaw = 0f;
    private float modelPitch = 0f;
    private boolean isDragging = false;
    private boolean isPanning = false;
    private boolean isDraggingScrollbar = false;
    private float panX = 0f;
    private float panY = 0f;
    private double lastMouseX;
    private double lastMouseY;
    private int listScroll = 0;
    private int maxScroll = 0;
    private Entity previewEntity = null;
    private String previewEntityId = "";
    private EditBox searchBox;
    private Button acquireBtn;
    private Button closeBtn;
    private Button visibleBtn;
    private Button playStopBtn;
    private DropdownMenu animationDropdown;
    private List<String> availableAnimations = new ArrayList<>();
    private long lastRenderTime = 0;
    private float tickAccumulator = 0;

    public MorphScreen(Player player) {
        super(Component.literal(I18n.s("iscript.morph.title")));
        this.player = player;
        player.getCapability(MorphData.CAPABILITY).ifPresent(d -> {
            this.morphData = d;
            this.scaleValue = d.getScale();
            this.isVisible = d.isVisible();
            this.selectedId = d.getModelId();
            this.selectedAnimation = d.getCurrentAnimation();
            this.animationTick = d.getAnimationTick();
        });
        loadModels();
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

    private void loadAnimationsForModel(String id) {
        availableAnimations.clear();
        animationKeys.clear();

        if (id.startsWith("entity:")) {
            String rl = id.substring(7);

            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));

                if (type != null && Minecraft.getInstance().level != null) {
                    Entity entity = type.create(Minecraft.getInstance().level);

                    if (entity != null) {
                        collectEntityAnimations(id, entity);
                        entity.discard();
                    }
                }
            } catch (Exception e) {
            }

            return;
        }

        GeoModel model = MorphManager.getModel(id);

        if (model != null) {
            try {
                java.lang.reflect.Method method = null;
                Class<?> modelClass = model.getClass();

                while (modelClass != null && method == null) {
                    try {
                        method = modelClass.getDeclaredMethod("getAvailableAnimations");
                    } catch (NoSuchMethodException e) {
                        modelClass = modelClass.getSuperclass();
                    }
                }

                if (method != null) {
                    method.setAccessible(true);
                    Object result = method.invoke(model);

                    if (result instanceof Collection<?>) {
                        for (Object entry : (Collection<?>) result) {
                            if (entry != null) {
                                String value = entry.toString();

                                if (!value.isEmpty()) {
                                    addAnimation(value, value);
                                }
                            }
                        }
                    } else if (result instanceof Map<?, ?>) {
                        for (Object key : ((Map<?, ?>) result).keySet()) {
                            if (key != null) {
                                String value = key.toString();

                                if (!value.isEmpty()) {
                                    addAnimation(value, value);
                                }
                            }
                        }
                    } else if (result instanceof String[]) {
                        for (String entry : (String[]) result) {
                            if (entry != null && !entry.isEmpty()) {
                                addAnimation(entry, entry);
                            }
                        }
                    } else if (result instanceof String) {
                        String value = result.toString();

                        if (!value.isEmpty()) {
                            addAnimation(value, value);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private void collectEntityAnimations(String id, Entity entity) {
        addAnimation("idle", "idle");

        if (entity instanceof LivingEntity) {
            addAnimation("walk", "walk");
            addAnimation("run", "run");
            addAnimation("attack", "attack");
        }

        if (entity instanceof TamableAnimal) {
            addAnimation("sit", "sit");
        }

        int stateIndex = 1;
        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!AnimationState.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                String key = field.getName();
                String display = STATE_DISPLAY_NAMES.get(id + ":" + key);

                if (display == null || display.isEmpty()) {
                    display = "animation_" + stateIndex;
                }

                addAnimation(display, key);
                stateIndex++;
            }

            clazz = clazz.getSuperclass();
        }
    }

    private void addAnimation(String display, String key) {
        if (!availableAnimations.contains(display)) {
            availableAnimations.add(display);
            animationKeys.add(key);
        }
    }

    private void applyEntityAnimation(Entity entity, String animName, float deltaTicks) {
        String key = animName;
        int idx = availableAnimations.indexOf(animName);

        if (idx >= 0 && idx < animationKeys.size()) {
            key = animationKeys.get(idx);
        }

        boolean isUniversal = key.equals("idle") || key.equals("walk") || key.equals("run")
                || key.equals("attack") || key.equals("sit");
        float walkSpeed = 0f;

        if (key.equals("walk")) {
            walkSpeed = 0.5f;
        }

        if (key.equals("run")) {
            walkSpeed = 1.0f;
        }

        if (entity instanceof LivingEntity living) {
            if (isAnimationPlaying && walkSpeed > 0f) {
                living.walkAnimation.setSpeed(walkSpeed);
                living.walkAnimation.position(living.walkAnimation.position() + walkSpeed * deltaTicks);
            } else {
                living.walkAnimation.setSpeed(0f);
            }

            if (isAnimationPlaying && key.equals("attack")) {
                living.swinging = true;
                swingAccumulator += deltaTicks;

                if (swingAccumulator >= 1f) {
                    living.swingTime = (living.swingTime + (int) swingAccumulator) % 4;
                    swingAccumulator %= 1f;
                }
            } else {
                living.swinging = false;
                living.swingTime = 0;
                swingAccumulator = 0f;
            }
        }

        if (entity instanceof TamableAnimal tamable) {
            tamable.setInSittingPose(isAnimationPlaying && key.equals("sit"));
        }

        if (isUniversal) {
            if (!isAnimationPlaying) {
                stopAllStates(entity);
            }

            return;
        }

        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!AnimationState.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                if (!field.getName().equals(key)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    AnimationState state = (AnimationState) field.get(entity);

                    if (state != null) {
                        if (isAnimationPlaying) {
                            state.startIfStopped(entity.tickCount);
                        } else {
                            state.stop();
                        }
                    }
                } catch (Exception e) {
                }

                return;
            }

            clazz = clazz.getSuperclass();
        }
    }

    private void stopAllStates(Entity entity) {
        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!AnimationState.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    AnimationState state = (AnimationState) field.get(entity);

                    if (state != null) {
                        state.stop();
                    }
                } catch (Exception e) {
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    private void applyFilter() {
        categories.clear();
        for (Map.Entry<String, List<String>> entry : originalCategories.entrySet()) {
            List<String> filtered = new ArrayList<>();
            for (String id : entry.getValue()) {
                if (searchQuery.isEmpty() || id.toLowerCase().contains(searchQuery.toLowerCase())
                        || getDisplayName(id).toLowerCase().contains(searchQuery.toLowerCase())) {
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
        return this.height - PADDING * 4 - 100;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int searchY = PADDING;
        int searchW = LEFT_PANEL_WIDTH - PADDING * 2;
        searchBox = new EditBox(this.font, PADDING, searchY, searchW, 16, Component.literal(I18n.s("iscript.morph.search")));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        int bottomY = this.height - 30 - PADDING;
        int btnW = 80;
        int btnSpacing = 10;
        int totalBtnW = btnW * 2 + btnSpacing;
        int startX = this.width - totalBtnW - PADDING;

        acquireBtn = Button.builder(Component.literal(I18n.s("iscript.morph.acquire")), b -> acquireMorph())
                .pos(startX, bottomY).size(btnW, 20).build();
        this.addRenderableWidget(acquireBtn);

        closeBtn = Button.builder(Component.literal(I18n.s("iscript.morph.close")), b -> this.onClose())
                .pos(startX + btnW + btnSpacing, bottomY).size(btnW, 20).build();
        this.addRenderableWidget(closeBtn);

        int rightX = this.width - RIGHT_PANEL_WIDTH;
        int rightY = PADDING + 24;
        int buttonWidth = (RIGHT_PANEL_WIDTH - PADDING * 2 - 4) / 2;

        visibleBtn = Button.builder(Component.literal(isVisible ? I18n.s("iscript.morph.visible.on") : I18n.s("iscript.morph.visible.off")), b -> {
            isVisible = !isVisible;
            b.setMessage(Component.literal(isVisible ? I18n.s("iscript.morph.visible.on") : I18n.s("iscript.morph.visible.off")));
        }).pos(rightX + PADDING, rightY).size(buttonWidth, 16).build();
        this.addRenderableWidget(visibleBtn);

        playStopBtn = Button.builder(Component.literal(isAnimationPlaying ? "⏸ " + I18n.s("iscript.morph.stop") : "▶ " + I18n.s("iscript.morph.play")), b -> {
            isAnimationPlaying = !isAnimationPlaying;
            if (!isAnimationPlaying) {
                lastRenderTime = 0;
            }
            b.setMessage(Component.literal(isAnimationPlaying ? "⏸ " + I18n.s("iscript.morph.stop") : "▶ " + I18n.s("iscript.morph.play")));
        }).pos(rightX + PADDING + buttonWidth + 4, rightY).size(buttonWidth, 16).build();
        this.addRenderableWidget(playStopBtn);

        animationDropdown = new DropdownMenu();
        refreshAnimationList();
        animationDropdown.setOnSelect(anim -> {
            if (!anim.equals(selectedAnimation)) {
                selectedAnimation = anim;
                animationTick = 0;
                previewEntity = null;
            }
        });
    }

    private void onSearchChanged(String query) {
        searchQuery = query;
        applyFilter();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        int leftX = PADDING;
        int leftY = PADDING + 24;
        int leftW = LEFT_PANEL_WIDTH;
        int leftH = this.height - leftY - 50;

        int rightX = this.width - RIGHT_PANEL_WIDTH;
        int rightY = leftY;
        int rightW = RIGHT_PANEL_WIDTH;
        int rightH = leftH;

        int centerX = leftX + leftW + PADDING;
        int centerY = leftY;
        int centerW = this.width - leftW - rightW - PADDING * 3;
        int centerH = leftH;

        UI.panel(g, leftX, leftY, leftW, leftH);
        UI.panel(g, centerX, centerY, centerW, centerH);
        UI.panel(g, rightX, rightY, rightW, rightH);

        renderCategoryList(g, mx, my, leftX, leftY, leftW, leftH);
        renderModelPreview(g, centerX, centerY, centerW, centerH);
        renderRightPanel(g, rightX, rightY, rightW, mx, my);

        super.render(g, mx, my, pt);

        if (animationDropdown != null && animationDropdown.isOpen()) {
            animationDropdown.render(g, this.font, mx, my);
        }

        if (isAnimationPlaying) {
            animationTick++;
        }
    }

    private void renderCategoryList(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        UI.title(g, this.font, I18n.s("iscript.morph.categories"), x + PADDING, y + PADDING);

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
                g.drawString(this.font, truncatedCat, listX + 4, currentY + 4, Theme.ACCENT);

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
                        g.drawString(this.font, truncatedName, listX + 16, currentY + 4, color);

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
        Minecraft mc = Minecraft.getInstance();
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
        if (this.font.width(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 0 && this.font.width(result + "...") > maxWidth) {
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

    private void renderModelPreview(GuiGraphics g, int x, int y, int w, int h) {
        String scaleText = String.format("Scale: %.0f%%", scaleValue * 100);
        int textW = this.font.width(scaleText);
        int textX = x + w - PADDING - textW;
        int textY = y + PADDING;

        g.fill(textX - 4, textY - 2, textX + textW + 4, textY + this.font.lineHeight + 2, 0x80000000);
        g.drawString(this.font, scaleText, textX, textY, Theme.TEXT);

        if (selectedId.isEmpty()) {
            UI.centerLabel(g, this.font, I18n.s("iscript.morph.no_model"), x, y + h / 2 - 10, w);
            return;
        }

        PoseStack pose = g.pose();
        pose.pushPose();

        float centerX = x + w / 2f + panX;
        float centerY = y + h / 2f + panY;
        float scale = 30f * scaleValue;
        float zOffset = 500f + (scale * 2f);

        pose.translate(centerX, centerY, zOffset);
        pose.scale(scale, -scale, scale);

        float rx = (float) Math.toRadians(modelPitch);
        float ry = (float) Math.toRadians(modelYaw);
        pose.mulPose(com.mojang.math.Axis.XP.rotation(rx));
        pose.mulPose(com.mojang.math.Axis.YP.rotation(ry));

        if (selectedId.startsWith("entity:")) {
            if (!selectedId.equals(previewEntityId) || previewEntity == null || previewEntity.isRemoved()) {
                previewEntityId = selectedId;
                previewEntity = null;
                String rl = selectedId.substring(7);
                try {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
                    if (type != null && Minecraft.getInstance().level != null) {
                        previewEntity = type.create(Minecraft.getInstance().level);
                        if (previewEntity != null) {
                            if (previewEntity instanceof Mob mob) {
                                mob.setNoAi(true);
                            }
                            previewEntity.setYRot(180f);
                            previewEntity.setXRot(0f);
                        }
                    }
                } catch (Exception e) {
                }
            }

            if (previewEntity != null) {
                long currentTime = System.nanoTime();
                float deltaTicks = 0.0f;
                if (lastRenderTime != 0 && isAnimationPlaying) {
                    deltaTicks = Math.min((currentTime - lastRenderTime) / 50000000.0f, 2.0f);
                    tickAccumulator += deltaTicks;
                    if (tickAccumulator >= 1.0f) {
                        previewEntity.tickCount += (int) tickAccumulator;
                        tickAccumulator %= 1.0f;
                    }
                }
                lastRenderTime = currentTime;

                applyEntityAnimation(previewEntity, selectedAnimation, deltaTicks);

                previewEntity.setYRot(180f);
                previewEntity.setXRot(0f);

                if (previewEntity instanceof LivingEntity living) {
                    living.yHeadRot = 180f;
                    living.yBodyRot = 180f;
                    living.yHeadRotO = 180f;
                    living.yBodyRotO = 180f;
                    living.yRotO = 180f;
                    living.xRotO = 0f;
                }

                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                dispatcher.setRenderShadow(false);
                MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
                float partialTicks = isAnimationPlaying ? tickAccumulator : 0.0f;

                RenderSystem.enableDepthTest();
                try {
                    dispatcher.render(previewEntity, 0.0, 0.0, 0.0, 0.0f, partialTicks, pose, buffer, 15728880);
                    buffer.endBatch();
                } catch (Exception e) {
                }
                RenderSystem.disableDepthTest();
                dispatcher.setRenderShadow(true);
            } else {
                UI.centerLabel(g, this.font, I18n.s("iscript.morph.model_not_found"), x, y + h / 2 - 10, w);
            }
        } else {
            GeoModel model = MorphManager.getModel(selectedId);
            if (model == null) {
                UI.centerLabel(g, this.font, I18n.s("iscript.morph.model_not_found"), x, y + h / 2 - 10, w);
                pose.popPose();
                return;
            }

            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            ResourceLocation texture = MorphRenderer.getTextureLocation(selectedId);
            if (texture == null) texture = new ResourceLocation("minecraft", "textures/block/stone.png");

            com.mojang.blaze3d.vertex.VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

            RenderSystem.enableDepthTest();
            try {
                for (Bone bone : model.getBones()) {
                    if (bone.getParent().isEmpty()) {
                        MorphRenderer.renderBoneGui(bone, model, pose, builder, 15728880, OverlayTexture.NO_OVERLAY, 1.0f);
                    }
                }
                buffer.endBatch();
            } catch (Exception e) {
            }
            RenderSystem.disableDepthTest();
        }

        pose.popPose();
        g.drawCenteredString(this.font, I18n.s("iscript.morph.rotate_hint"), x + w / 2, y + h - 15, Theme.TEXT_DIM);
    }

    private void renderRightPanel(GuiGraphics g, int x, int y, int w, int mx, int my) {
        UI.title(g, this.font, I18n.s("iscript.morph.controls"), x + PADDING, y + PADDING);

        int dropdownY = y + PADDING + 32;
        UI.title(g, this.font, I18n.s("iscript.morph.animation"), x + PADDING, dropdownY);

        int dropdownBtnY = dropdownY + 16;
        int dropdownBtnW = w - PADDING * 2;
        int dropdownBtnH = 18;

        boolean dropdownHovered = mx >= x + PADDING && mx <= x + PADDING + dropdownBtnW &&
                my >= dropdownBtnY && my <= dropdownBtnY + dropdownBtnH;

        int dropdownBg = dropdownHovered ? Theme.BG_HOVER : 0xFF1E1E26;
        g.fill(x + PADDING, dropdownBtnY, x + PADDING + dropdownBtnW, dropdownBtnY + dropdownBtnH, dropdownBg);
        g.renderOutline(x + PADDING, dropdownBtnY, dropdownBtnW, dropdownBtnH, Theme.BORDER);

        String displayText = selectedAnimation.isEmpty() ? I18n.s("iscript.morph.select_animation") : selectedAnimation;
        g.drawString(this.font, displayText + " ▼", x + PADDING + 6, dropdownBtnY + 5, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int rightX = this.width - RIGHT_PANEL_WIDTH;
        int rightY = PADDING + 24;
        int dropdownBtnY = rightY + PADDING + 48;
        int dropdownBtnW = RIGHT_PANEL_WIDTH - PADDING * 2;
        int dropdownBtnH = 18;

        if (animationDropdown.isOpen()) {
            boolean clickedDropdown = animationDropdown.mouseClicked(mx, my, btn);
            boolean clickedButton = (mx >= rightX + PADDING && mx <= rightX + PADDING + dropdownBtnW &&
                    my >= dropdownBtnY && my <= dropdownBtnY + dropdownBtnH);

            if (!clickedDropdown && !clickedButton) {
                animationDropdown.close();
            }
            if (clickedDropdown) {
                return true;
            }
        }

        if (mx >= rightX + PADDING && mx <= rightX + PADDING + dropdownBtnW &&
                my >= dropdownBtnY && my <= dropdownBtnY + dropdownBtnH) {
            if (animationDropdown.isOpen()) {
                animationDropdown.close();
            } else {
                String safeSelection = availableAnimations.contains(selectedAnimation) ? selectedAnimation : "";
                animationDropdown.open(rightX + PADDING, dropdownBtnY + dropdownBtnH, dropdownBtnW, safeSelection);
            }
            return true;
        }

        int leftX = PADDING;
        int leftY = PADDING + 24;
        int leftW = LEFT_PANEL_WIDTH;
        int leftH = this.height - leftY - 50;

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
        int centerW = this.width - leftW - RIGHT_PANEL_WIDTH - PADDING * 3;
        int centerH = leftH;

        if (mx >= centerX && mx <= centerX + centerW && my >= centerY && my <= centerY + centerH) {
            if (btn == 0) {
                isDragging = true;
                lastMouseX = mx;
                lastMouseY = my;
                return true;
            }
            if (btn == 1) {
                isPanning = true;
                lastMouseX = mx;
                lastMouseY = my;
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
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
                        panX = 0f;
                        panY = 0f;
                        refreshAnimationList();
                        animationTick = 0;
                        previewEntity = null;
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
        isDragging = false;
        isPanning = false;
        isDraggingScrollbar = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScrollbar && btn == 0) {
            int leftX = PADDING;
            int leftY = PADDING + 24;
            int leftW = LEFT_PANEL_WIDTH;
            int leftH = this.height - leftY - 50;
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

        if (isDragging && btn == 0) {
            modelYaw += (float) (mx - lastMouseX) * 0.8f;
            modelPitch += (float) (my - lastMouseY) * 0.8f;
            modelPitch = Math.max(-90f, Math.min(90f, modelPitch));
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }

        if (isPanning && btn == 1) {
            panX += (float) (mx - lastMouseX);
            panY += (float) (my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }

        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int leftX = PADDING;
        int leftY = PADDING + 24;
        int leftW = LEFT_PANEL_WIDTH;
        int leftH = this.height - leftY - 50;

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
        int centerW = this.width - leftW - RIGHT_PANEL_WIDTH - PADDING * 3;
        int centerH = leftH;

        if (mx >= centerX && mx <= centerX + centerW && my >= centerY && my <= centerY + centerH) {
            if (delta > 0) {
                scaleValue = Math.min(10.0f, scaleValue + 0.1f);
            } else {
                scaleValue = Math.max(0.1f, scaleValue - 0.1f);
            }
            return true;
        }

        int rightX = this.width - RIGHT_PANEL_WIDTH;
        int rightY = PADDING + 24;
        int rightW = RIGHT_PANEL_WIDTH;
        int rightH = this.height - rightY - 50;

        if (mx >= rightX && mx <= rightX + rightW && my >= rightY && my <= rightY + rightH) {
            if (animationDropdown.isOpen()) {
                animationDropdown.mouseScrolled(delta);
                return true;
            }

            int idx = availableAnimations.indexOf(selectedAnimation);
            if (delta > 0) {
                if (idx > 0) {
                    selectedAnimation = availableAnimations.get(idx - 1);
                    previewEntity = null;
                }
            } else {
                if (idx < availableAnimations.size() - 1) {
                    selectedAnimation = availableAnimations.get(idx + 1);
                    previewEntity = null;
                }
            }
            return true;
        }

        return super.mouseScrolled(mx, my, delta);
    }

    private void acquireMorph() {
        if (selectedId.isEmpty()) return;

        CompoundTag tag = new CompoundTag();
        tag.putString("ModelId", selectedId);
        tag.putString("TextureId", selectedId);
        tag.putFloat("Scale", scaleValue);
        tag.putBoolean("Morphed", true);
        tag.putBoolean("Visible", isVisible);
        tag.putString("CurrentAnimation", selectedAnimation);
        tag.putBoolean("AnimationPlaying", isAnimationPlaying);
        tag.putInt("AnimationTick", animationTick);

        IScriptNetwork.sendToServer(new MorphUpdatePacket(tag));
        this.onClose();
    }

    @Override
    public void onClose() {
        previewEntity = null;
        previewEntityId = "";
        animationDropdown.close();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshAnimationList() {
        loadAnimationsForModel(selectedId);

        if (!availableAnimations.contains(selectedAnimation)) {
            selectedAnimation = "";
        }

        if (animationDropdown != null) {
            animationDropdown.close();
            animationDropdown.setItems(new ArrayList<>(availableAnimations));
        }
    }
}