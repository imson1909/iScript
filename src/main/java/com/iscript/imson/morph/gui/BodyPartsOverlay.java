package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.Bone;
import com.iscript.imson.morph.model.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BodyPartsOverlay {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Minecraft mc;
    private final Font font;
    private final MorphEditorScreen screen;
    private List<String> limbIds = new ArrayList<>();
    private int selectedLimb = -1;
    private List<String> currentModelBones = new ArrayList<>();
    private int selectedBone = -1;
    private EditBox limbIdBox;

    public BodyPartsOverlay(MorphEditorScreen screen) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void init() {
        loadLimbs();
        loadCurrentModelBones();
        int leftX = 10;
        int y = 60;
        int w = 130;
        int h = 18;
        limbIdBox = new EditBox(font, leftX, y + 20 + limbIds.size() * 20 + 5, w, h, Component.literal("ID сущности"));
        limbIdBox.setMaxLength(128);
        limbIdBox.setResponder(v -> {});
    }

    private void loadLimbs() {
        limbIds.clear();
        Map<String, MorphData.BodyPartTransform> parts = screen.getBodyParts();
        for (String key : parts.keySet()) {
            MorphData.BodyPartTransform part = parts.get(key);
            if (part.attachedId != null && !part.attachedId.isEmpty()) {
                limbIds.add(key);
            }
        }
        if (selectedLimb >= limbIds.size()) {
            selectedLimb = limbIds.isEmpty() ? -1 : limbIds.size() - 1;
        }
    }

    private void loadCurrentModelBones() {
        currentModelBones.clear();
        selectedBone = -1;
        String modelId = screen.getModelId();

        LOGGER.info("========== [BodyPartsOverlay] Начало загрузки костей ==========");
        LOGGER.info("[BodyPartsOverlay] Текущий modelId из экрана: {}", modelId);

        GeoModel model = MorphManager.getModel(modelId);

        if (model == null && modelId.contains(":")) {
            String stripped = modelId.substring(modelId.indexOf(':') + 1);
            LOGGER.info("[BodyPartsOverlay] Модель не найдена по полному ID, пробуем без префикса: {}", stripped);
            model = MorphManager.getModel(stripped);
        }

        if (model != null) {
            LOGGER.info("[BodyPartsOverlay] Модель GeoModel найдена! Количество костей: {}", model.getBones().size());
            for (Bone bone : model.getBones()) {
                currentModelBones.add(bone.getName());
                LOGGER.info("[BodyPartsOverlay] -> Найдена кость: {}", bone.getName());
            }
        } else if (modelId.startsWith("entity:")) {
            String entityName = modelId.replace("entity:", "");
            LOGGER.info("[BodyPartsOverlay] Geo-модель не найдена, автоопределение костей для сущности: {}", entityName);
            addEntityBonesAuto(entityName);
        } else {
            LOGGER.warn("[BodyPartsOverlay] Модель НЕ НАЙДЕНА в MorphManager.");
            currentModelBones.add("root");
        }

        if (currentModelBones.isEmpty()) {
            currentModelBones.add("root");
            LOGGER.info("[BodyPartsOverlay] Список костей пуст, добавлен базовый 'root'.");
        }

        if (selectedBone == -1 && !currentModelBones.isEmpty()) {
            selectedBone = 0;
        }
        LOGGER.info("========== [BodyPartsOverlay] Конец загрузки костей ==========");
    }

    private void addEntityBonesAuto(String entityName) {
        try {
            ResourceLocation entityRL = new ResourceLocation(entityName);
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityRL);

            if (entityType == null) {
                LOGGER.warn("[BodyPartsOverlay] Сущность не найдена в реестре: {}", entityName);
                currentModelBones.add("root");
                return;
            }

            Entity entity = entityType.create(mc.level);
            if (entity == null) {
                LOGGER.warn("[BodyPartsOverlay] Не удалось создать экземпляр сущности: {}", entityName);
                currentModelBones.add("root");
                return;
            }

            EntityRenderer<? extends Entity> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) {
                LOGGER.warn("[BodyPartsOverlay] Рендерер не найден для сущности: {}", entityName);
                currentModelBones.add("root");
                return;
            }

            Model model = getModelFromRenderer(renderer);
            if (model == null) {
                LOGGER.warn("[BodyPartsOverlay] Модель не найдена у рендерера: {}", entityName);
                currentModelBones.add("root");
                return;
            }

            collectModelParts(model, "");

            LOGGER.info("[BodyPartsOverlay] Автоопределение завершено. Найдено костей: {}", currentModelBones.size());
            for (String bone : currentModelBones) {
                LOGGER.info("[BodyPartsOverlay] -> {}", bone);
            }

        } catch (Exception e) {
            LOGGER.error("[BodyPartsOverlay] Ошибка автоопределения костей для {}: {}", entityName, e.getMessage());
            currentModelBones.add("root");
            currentModelBones.add("body");
            currentModelBones.add("head");
        }
    }

    private Model getModelFromRenderer(EntityRenderer<?> renderer) {
        try {
            if (renderer instanceof LivingEntityRenderer) {
                LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) renderer;
                return livingRenderer.getModel();
            }

            Field modelField = renderer.getClass().getDeclaredField("model");
            modelField.setAccessible(true);
            return (Model) modelField.get(renderer);
        } catch (Exception e) {
            LOGGER.warn("[BodyPartsOverlay] Не удалось получить модель из рендерера: {}", e.getMessage());
            return null;
        }
    }

    private void collectModelParts(Model model, String prefix) {
        try {
            Field[] fields = model.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (ModelPart.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ModelPart part = (ModelPart) field.get(model);
                    if (part != null) {
                        String partName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                        currentModelBones.add(partName);
                        collectChildParts(part, partName);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[BodyPartsOverlay] Ошибка при сборке частей модели: {}", e.getMessage());
        }
    }

    private void collectChildParts(ModelPart part, String parentName) {
        try {
            Field childrenField = ModelPart.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<String, ModelPart> children = (Map<String, ModelPart>) childrenField.get(part);

            if (children != null) {
                for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
                    String childName = parentName + "." + entry.getKey();
                    currentModelBones.add(childName);
                    collectChildParts(entry.getValue(), childName);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[BodyPartsOverlay] Ошибка при сборке дочерних частей: {}", e.getMessage());
        }
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        int leftX = 10;
        int leftY = 60;
        int leftW = 150;
        int leftH = 280;
        g.fill(leftX, leftY, leftX + leftW, leftY + leftH, 0xAA16161E);
        g.renderOutline(leftX, leftY, leftW, leftH, Theme.BORDER);
        g.drawString(font, "Конечности:", leftX + 5, leftY + 5, Theme.ACCENT);
        int btnH = 20;
        for (int i = 0; i < limbIds.size(); i++) {
            int by = leftY + 20 + i * btnH;
            boolean hover = inRect(mx, my, leftX + 5, by, leftW - 10, btnH);
            boolean active = i == selectedLimb;
            int bg = active ? Theme.ACCENT : (hover ? Theme.BG_HOVER : 0xFF1E1E26);
            g.fill(leftX + 5, by, leftX + leftW - 5, by + btnH, bg);
            g.renderOutline(leftX + 5, by, leftW - 10, btnH, Theme.BORDER);
            g.drawCenteredString(font, limbIds.get(i), leftX + leftW / 2, by + 6, Theme.TEXT);
        }
        int addBtnY = leftY + 20 + limbIds.size() * btnH;
        boolean addHover = inRect(mx, my, leftX + 5, addBtnY, leftW - 10, btnH);
        g.fill(leftX + 5, addBtnY, leftX + leftW - 5, addBtnY + btnH, addHover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(leftX + 5, addBtnY, leftW - 10, btnH, Theme.BORDER);
        g.drawCenteredString(font, "+ Добавить", leftX + leftW / 2, addBtnY + 6, Theme.TEXT);
        if (selectedLimb >= 0) {
            limbIdBox.render(g, mx, my, pt);
        }
        int rightX = screen.width - 200;
        int rightY = 60;
        int panelW = 180;
        int panelH = 280;
        g.fill(rightX, rightY, rightX + panelW, rightY + panelH, 0xAA16161E);
        g.renderOutline(rightX, rightY, panelW, panelH, Theme.BORDER);
        g.drawString(font, "Кости модели:", rightX + 5, rightY + 5, Theme.ACCENT);
        int boneBtnH = 20;
        for (int i = 0; i < currentModelBones.size(); i++) {
            int by = rightY + 20 + i * boneBtnH;
            boolean hover = inRect(mx, my, rightX + 5, by, panelW - 10, boneBtnH);
            boolean active = i == selectedBone;
            int bg = active ? Theme.ACCENT : (hover ? Theme.BG_HOVER : 0xFF1E1E26);
            g.fill(rightX + 5, by, rightX + panelW - 5, by + boneBtnH, bg);
            g.renderOutline(rightX + 5, by, panelW - 10, boneBtnH, Theme.BORDER);
            g.drawCenteredString(font, currentModelBones.get(i), rightX + panelW / 2, by + 6, Theme.TEXT);
        }
        if (selectedLimb >= 0 && selectedBone >= 0) {
            int infoY = rightY + panelH - 40;
            g.drawString(font, "Прикрепить к:", rightX + 5, infoY, Theme.TEXT_DIM);
            g.drawString(font, currentModelBones.get(selectedBone), rightX + 5, infoY + 12, Theme.ACCENT);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int leftX = 10;
        int leftY = 60;
        int leftW = 150;
        int btnH = 20;
        for (int i = 0; i < limbIds.size(); i++) {
            int by = leftY + 20 + i * btnH;
            if (inRect(mx, my, leftX + 5, by, leftW - 10, btnH)) {
                selectedLimb = i;
                return true;
            }
        }
        int addBtnY = leftY + 20 + limbIds.size() * btnH;
        if (inRect(mx, my, leftX + 5, addBtnY, leftW - 10, btnH)) {
            String newLimbId = "entity:minecraft:bat";
            limbIds.add(newLimbId);
            selectedLimb = limbIds.size() - 1;
            MorphData.BodyPartTransform newPart = new MorphData.BodyPartTransform();
            newPart.attachedId = newLimbId;
            screen.getBodyParts().put(newLimbId, newPart);
            limbIdBox.setValue(newLimbId);
            return true;
        }
        if (selectedLimb >= 0 && btn == 1) {
            screen.getBodyParts().remove(limbIds.get(selectedLimb));
            limbIds.remove(selectedLimb);
            selectedLimb = limbIds.isEmpty() ? -1 : Math.min(selectedLimb, limbIds.size() - 1);
            return true;
        }
        int rightX = screen.width - 200;
        int rightY = 60;
        int panelW = 180;
        int boneBtnH = 20;
        for (int i = 0; i < currentModelBones.size(); i++) {
            int by = rightY + 20 + i * boneBtnH;
            if (inRect(mx, my, rightX + 5, by, panelW - 10, boneBtnH)) {
                selectedBone = i;
                if (selectedLimb >= 0 && selectedLimb < limbIds.size()) {
                    String limbId = limbIds.get(selectedLimb);
                    MorphData.BodyPartTransform part = screen.getBodyParts().get(limbId);
                    if (part != null) {
                        part.attachedId = currentModelBones.get(selectedBone);
                    }
                }
                return true;
            }
        }
        if (selectedLimb >= 0 && limbIdBox.mouseClicked(mx, my, btn)) {
            return true;
        }
        return false;
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }
}