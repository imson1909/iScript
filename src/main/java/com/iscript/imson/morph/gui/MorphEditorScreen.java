package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.network.MorphUpdatePacket;
import com.iscript.imson.network.IScriptNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public class MorphEditorScreen extends Screen {
    private final Screen parent;
    private final String modelId;
    private final ModelPreviewScreen preview;
    private final MorphData sourceData;
    private MorphEditorOverlay settingsOverlay;
    private BodyPartsOverlay bodyPartsOverlay;
    private AnimationsOverlay animationsOverlay;
    private AbilitiesOverlay abilitiesOverlay;
    private int activeTab = 0;

    private static final int TOP_BAR_H = 40;
    private static final int BOTTOM_BAR_H = 40;

    public String displayName = "";
    public float scale = 1.0f;
    public float hitboxWidth = 0.6f;
    public float hitboxHeight = 1.8f;
    public float hitboxEye = 1.62f;
    public float hitboxSneak = 1.5f;
    public String customTexture = "";
    public float animationSpeed = 1.0f;
    public String currentAnimation = "";
    public boolean animates = true;
    public boolean ignored = false;
    public int animationDuration = 10;
    public String interpolation = "LINEAR";
    public String poseName = "";
    public boolean actionPlayerMode = false;
    public float globalSize = 1.0f;
    public float guiSize = 1.0f;
    public String skinPath = "";
    public Map<String, MorphData.BodyPartTransform> bodyParts = new HashMap<>();
    public Map<String, MorphData.ActionConfig> actions = new HashMap<>();
    public boolean canFly = false, canSwim = false, canClimb = false;
    public boolean fireImmune = false, waterBreathing = false, nightVision = false;

    public MorphEditorScreen(Screen parent, String modelId, MorphData currentData) {
        super(Component.literal("Morph Editor"));
        this.parent = parent;
        this.modelId = modelId;
        this.sourceData = currentData;
        this.preview = new ModelPreviewScreen();
        this.preview.setSelectedId(modelId);

        if (currentData != null && currentData.isMorphed() && currentData.getModelId().equals(modelId)) {
            this.displayName = currentData.getDisplayName();
            this.scale = currentData.getScale();
            this.hitboxWidth = currentData.getHitboxWidth();
            this.hitboxHeight = currentData.getHitboxHeight();
            this.hitboxEye = currentData.getHitboxEye();
            this.hitboxSneak = currentData.getHitboxSneakingHeight();
            this.customTexture = currentData.getCustomTexture();
            this.animationSpeed = currentData.getAnimationSpeed();
            this.currentAnimation = currentData.getCurrentAnimation();
            this.animates = currentData.isAnimates();
            this.animationDuration = currentData.getAnimationDuration();
            this.interpolation = currentData.getInterpolation();
            this.poseName = currentData.getPoseName();
            this.actionPlayerMode = currentData.isActionPlayerMode();
            this.globalSize = currentData.getGlobalSize();
            this.guiSize = currentData.getGuiSize();
            this.skinPath = currentData.getSkinPath();
            this.bodyParts.putAll(currentData.getBodyParts());
            this.actions.putAll(currentData.getActions());
            this.canFly = currentData.canFly();
            this.canSwim = currentData.canSwim();
            this.canClimb = currentData.canClimb();
            this.fireImmune = currentData.isFireImmune();
            this.waterBreathing = currentData.hasWaterBreathing();
            this.nightVision = currentData.hasNightVision();
        }
    }

    public String getModelId() {
        return modelId;
    }

    @Override
    protected void init() {
        super.init();
        this.settingsOverlay = new MorphEditorOverlay(this);
        this.bodyPartsOverlay = new BodyPartsOverlay(this);
        this.animationsOverlay = new AnimationsOverlay(this);
        this.abilitiesOverlay = new AbilitiesOverlay(this);
        settingsOverlay.init();
        bodyPartsOverlay.init();
        animationsOverlay.init();
        abilitiesOverlay.init();
    }

    public Font getFont() { return this.font; }
    public Map<String, MorphData.BodyPartTransform> getBodyParts() { return bodyParts; }
    public Map<String, MorphData.ActionConfig> getActions() { return actions; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        preview.render(g, 0, TOP_BAR_H, this.width, this.height - TOP_BAR_H - BOTTOM_BAR_H, mx, my, pt);

        switch (activeTab) {
            case 0: settingsOverlay.render(g, mx, my, pt); break;
            case 1: bodyPartsOverlay.render(g, mx, my, pt); break;
            case 2: animationsOverlay.render(g, mx, my, pt); break;
            case 3: abilitiesOverlay.render(g, mx, my, pt); break;
        }

        g.fill(0, 0, this.width, TOP_BAR_H, 0xEE16161E);
        g.renderOutline(0, 0, this.width, TOP_BAR_H, Theme.BORDER);
        g.drawString(this.font, "Редактор: " + modelId, 10, 16, Theme.ACCENT);

        int topBtnW = 110, topBtnH = 24;
        int topBtnY = (TOP_BAR_H - topBtnH) / 2;
        int acquireX = this.width - topBtnW * 3 - 30;
        int morphX = acquireX + topBtnW + 10;
        int closeX = morphX + topBtnW + 10;

        drawTopBtn(g, mx, my, acquireX, topBtnY, topBtnW, topBtnH, "Приобрести");
        drawTopBtn(g, mx, my, morphX, topBtnY, topBtnW, topBtnH, "Превратиться");
        drawTopBtn(g, mx, my, closeX, topBtnY, topBtnW, topBtnH, "Закрыть");

        g.fill(0, this.height - BOTTOM_BAR_H, this.width, this.height, 0xEE16161E);
        g.renderOutline(0, this.height - BOTTOM_BAR_H, this.width, BOTTOM_BAR_H, Theme.BORDER);

        String[] tabNames = {"Настройки", "Тело", "Анимации", "Способн."};
        int tabW = 100, tabH = 24;
        int tabY = this.height - BOTTOM_BAR_H + (BOTTOM_BAR_H - tabH) / 2;
        int tabX = 10;

        for (int i = 0; i < tabNames.length; i++) {
            boolean isActive = i == activeTab;
            boolean isHovered = mx >= tabX && mx <= tabX + tabW && my >= tabY && my <= tabY + tabH;
            int bg = isActive ? Theme.ACCENT : (isHovered ? Theme.BG_HOVER : 0xFF1E1E26);
            g.fill(tabX, tabY, tabX + tabW, tabY + tabH, bg);
            g.renderOutline(tabX, tabY, tabW, tabH, Theme.BORDER);
            g.drawCenteredString(this.font, tabNames[i], tabX + tabW / 2, tabY + (tabH - 8) / 2, Theme.TEXT);
            tabX += tabW + 5;
        }

        int backW = 80, backH = 24;
        int backX = this.width - backW - 10;
        int backY = tabY;
        drawBottomBtn(g, mx, my, backX, backY, backW, backH, "Назад");

        super.render(g, mx, my, pt);
    }

    private void drawTopBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    private void drawBottomBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int topBtnW = 110, topBtnH = 24;
        int topBtnY = (TOP_BAR_H - topBtnH) / 2;
        int acquireX = this.width - topBtnW * 3 - 30;
        int morphX = acquireX + topBtnW + 10;
        int closeX = morphX + topBtnW + 10;

        if (inBtn(mx, my, acquireX, topBtnY, topBtnW, topBtnH)) {
            saveAndApply();
            return true;
        }
        if (inBtn(mx, my, morphX, topBtnY, topBtnW, topBtnH)) {
            saveAndApply();
            return true;
        }
        if (inBtn(mx, my, closeX, topBtnY, topBtnW, topBtnH)) {
            saveAndApply();
            this.onClose();
            return true;
        }

        int tabW = 100, tabH = 24;
        int tabY = this.height - BOTTOM_BAR_H + (BOTTOM_BAR_H - tabH) / 2;
        int tabX = 10;

        for (int i = 0; i < 4; i++) {
            if (inBtn(mx, my, tabX, tabY, tabW, tabH)) {
                if (i != activeTab) {
                    activeTab = i;
                    switch (i) {
                        case 0: settingsOverlay.init(); break;
                        case 1: bodyPartsOverlay.init(); break;
                        case 2: animationsOverlay.init(); break;
                        case 3: abilitiesOverlay.init(); break;
                    }
                }
                return true;
            }
            tabX += tabW + 5;
        }

        int backW = 80, backH = 24;
        int backX = this.width - backW - 10;
        int backY = tabY;

        if (inBtn(mx, my, backX, backY, backW, backH)) {
            saveAndApply();
            this.onClose();
            return true;
        }

        switch (activeTab) {
            case 0: if (settingsOverlay.mouseClicked(mx, my, btn)) return true; break;
            case 1: if (bodyPartsOverlay.mouseClicked(mx, my, btn)) return true; break;
            case 2: if (animationsOverlay.mouseClicked(mx, my, btn)) return true; break;
            case 3: if (abilitiesOverlay.mouseClicked(mx, my, btn)) return true; break;
        }

        if (my >= TOP_BAR_H && my <= this.height - BOTTOM_BAR_H) {
            if (preview.mouseClicked(mx, my, btn, 0, TOP_BAR_H, this.width, this.height - TOP_BAR_H - BOTTOM_BAR_H)) {
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    private boolean inBtn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        switch (activeTab) {
            case 0: if (settingsOverlay.mouseScrolled(mx, my, delta)) return true; break;
            case 1: if (bodyPartsOverlay.mouseScrolled(mx, my, delta)) return true; break;
            case 2: if (animationsOverlay.mouseScrolled(mx, my, delta)) return true; break;
            case 3: if (abilitiesOverlay.mouseScrolled(mx, my, delta)) return true; break;
        }

        if (my >= TOP_BAR_H && my <= this.height - BOTTOM_BAR_H) {
            if (preview.mouseScrolled(mx, my, delta, 0, TOP_BAR_H, this.width, this.height - TOP_BAR_H - BOTTOM_BAR_H)) {
                return true;
            }
        }

        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (my >= TOP_BAR_H && my <= this.height - BOTTOM_BAR_H) {
            if (preview.mouseDragged(mx, my, btn, dx, dy)) return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        preview.mouseReleased(mx, my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    private void saveAndApply() {
        if (sourceData != null) {
            sourceData.setModelId(modelId);
            sourceData.setTextureId(modelId);
            sourceData.setScale(scale);
            sourceData.setMorphed(true);
            sourceData.setDisplayName(displayName);
            sourceData.setHitboxWidth(hitboxWidth);
            sourceData.setHitboxHeight(hitboxHeight);
            sourceData.setHitboxEye(hitboxEye);
            sourceData.setHitboxSneakingHeight(hitboxSneak);
            sourceData.setCustomTexture(customTexture);
            sourceData.setAnimationSpeed(animationSpeed);
            sourceData.setCurrentAnimation(currentAnimation);
            sourceData.setAnimates(animates);
            sourceData.setAnimationDuration(animationDuration);
            sourceData.setInterpolation(interpolation);
            sourceData.setPoseName(poseName);
            sourceData.setActionPlayerMode(actionPlayerMode);
            sourceData.setGlobalSize(globalSize);
            sourceData.setGuiSize(guiSize);
            sourceData.setSkinPath(skinPath);
            sourceData.getBodyParts().clear();
            sourceData.getBodyParts().putAll(bodyParts);
            sourceData.getActions().clear();
            sourceData.getActions().putAll(actions);
            sourceData.setCanFly(canFly);
            sourceData.setCanSwim(canSwim);
            sourceData.setCanClimb(canClimb);
            sourceData.setFireImmune(fireImmune);
            sourceData.setWaterBreathing(waterBreathing);
            sourceData.setNightVision(nightVision);
            IScriptNetwork.sendToServer(new MorphUpdatePacket(sourceData.serialize()));
        }
    }

    @Override
    public void onClose() {
        saveAndApply();
        preview.onClose();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}