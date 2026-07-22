package com.iscript.iscript.morph.gui;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.morph.MorphManager;
import com.iscript.iscript.morph.model.GeoModel;
import com.iscript.iscript.morph.network.MorphUpdatePacket;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorphScreen extends Screen {
    private static final int CONTENT_WIDTH = 460;
    private static final int PANEL_TOP = 30;
    private static final int PANEL_BOTTOM = 10;

    private final Player player;
    private MorphData morphData;
    private List<String> modelIds = new ArrayList<>();
    private List<String> filteredIds = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollY = 0;
    private int maxScroll = 0;
    private int contentTop = 0;
    private int contentBottom = 0;

    private EditBox searchBox;
    private Button applyBtn;
    private Button resetBtn;
    private Button closeBtn;
    private Button scaleUpBtn;
    private Button scaleDownBtn;

    private float scaleValue = 1.0f;
    private boolean visible = true;

    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> widgetBaseY = new HashMap<>();
    private final List<LabelEntry> labels = new ArrayList<>();
    private final List<SectionEntry> sections = new ArrayList<>();

    private record LabelEntry(String text, int x, int baseY, int color) {}
    private record SectionEntry(String text, int x, int baseY) {}

    public MorphScreen(Player player) {
        super(Component.literal("Morph Selector"));
        this.player = player;
        player.getCapability(MorphData.CAPABILITY).ifPresent(d -> {
            this.morphData = d;
            this.scaleValue = d.getScale();
            this.visible = d.isVisible();
        });
        modelIds.addAll(MorphManager.getAllModels().keySet());
        filteredIds.addAll(modelIds);
    }

    @Override
    protected void init() {
        buildUI();
        applyScroll();
    }

    private void buildUI() {
        clearWidgets();
        scrollableWidgets.clear();
        widgetBaseY.clear();
        labels.clear();
        sections.clear();

        int cx = this.width / 2;
        int left = cx - CONTENT_WIDTH / 2 + 10;
        int w = CONTENT_WIDTH - 20;
        int h = 16;
        contentTop = PANEL_TOP + 6;
        int y = contentTop;

        addSection("Search", left, y);
        y += 18;
        searchBox = new EditBox(this.font, left, y, w, h, Component.literal("Search models..."));
        searchBox.setMaxLength(128);
        searchBox.setResponder(this::onSearch);
        addScrollable(searchBox, y);
        y += 28;

        addSection("Available Models (" + filteredIds.size() + ")", left, y);
        y += 18;

        for (int i = 0; i < filteredIds.size(); i++) {
            String id = filteredIds.get(i);
            boolean isSelected = i == selectedIndex;
            boolean isCurrent = morphData != null && morphData.isMorphed() && id.equals(morphData.getModelId());

            String label = isCurrent ? "[Active] " + id : id;
            int color = isSelected ? Theme.ACCENT : (isCurrent ? 0x55FF55 : Theme.TEXT_DIM);

            int finalI = i;
            Button btn = Button.builder(Component.literal(label).withStyle(st -> st.withColor(color)), b -> {
                selectedIndex = finalI;
                buildUI();
                applyScroll();
            }).pos(left, y).size(w, h).build();
            addScrollable(btn, y);
            y += 20;
        }

        if (filteredIds.isEmpty()) {
            addLabel("No models found. Place .geo.json files in saves/<world>/iscript/morphs/", left, y, Theme.ERROR);
            y += 20;
        }

        y += 10;

        if (selectedIndex >= 0 && selectedIndex < filteredIds.size()) {
            String id = filteredIds.get(selectedIndex);
            GeoModel model = MorphManager.getModel(id);

            addSection("Model Info: " + id, left, y);
            y += 18;

            if (model != null) {
                addLabel("Bones: " + model.getBones().size(), left, y, Theme.TEXT_DIM);
                addLabel("Texture: " + model.getTextureWidth() + "x" + model.getTextureHeight(), left + 120, y, Theme.TEXT_DIM);
                y += 16;
                addLabel("Identifier: " + model.getIdentifier(), left, y, Theme.TEXT_DIM);
                y += 20;
            } else {
                addLabel("Failed to load model data", left, y, Theme.ERROR);
                y += 20;
            }
        }

        if (morphData != null && morphData.isMorphed()) {
            y += 6;
            addSection("Current Morph", left, y);
            y += 18;
            addLabel("Model: " + morphData.getModelId(), left, y, 0x55FF55);
            y += 16;
            addLabel("Animation: " + (morphData.getCurrentAnimation().isEmpty() ? "none" : morphData.getCurrentAnimation()), left, y, 0x55FF55);
            y += 20;
        }

        y += 6;
        addSection("Settings", left, y);
        y += 18;

        addLabel("Scale: " + String.format("%.2f", scaleValue), left, y, Theme.TEXT_DIM);
        y += 18;

        scaleDownBtn = Button.builder(Component.literal("-"), b -> adjustScale(-0.1f))
                .pos(left, y).size(24, h).build();
        addScrollable(scaleDownBtn, y);

        scaleUpBtn = Button.builder(Component.literal("+"), b -> adjustScale(0.1f))
                .pos(left + 28, y).size(24, h).build();
        addScrollable(scaleUpBtn, y);

        Button visibleBtn = Button.builder(toggleLabel("Visible", visible), b -> {
            visible = !visible;
            b.setMessage(toggleLabel("Visible", visible));
        }).pos(left + 60, y).size(80, h).build();
        addScrollable(visibleBtn, y);
        y += 28;

        y += 10;
        applyBtn = Button.builder(Component.literal("Apply Morph"), b -> applyMorph())
                .pos(left, y).size(100, 20).build();
        addScrollable(applyBtn, y);

        resetBtn = Button.builder(Component.literal("Reset"), b -> resetMorph())
                .pos(left + 108, y).size(80, 20).build();
        addScrollable(resetBtn, y);

        closeBtn = Button.builder(Component.literal("Close"), b -> this.onClose())
                .pos(left + 196, y).size(80, 20).build();
        addScrollable(closeBtn, y);

        contentBottom = y + 30;
    }

    private Component toggleLabel(String label, boolean state) {
        String s = state ? "ON" : "OFF";
        int c = state ? Theme.ACCENT : Theme.ERROR;
        return Component.literal(label + ": ").append(Component.literal(s).withStyle(st -> st.withColor(c)));
    }

    private void addSection(String text, int x, int y) {
        sections.add(new SectionEntry(text, x, y));
    }

    private void addLabel(String text, int x, int y, int color) {
        labels.add(new LabelEntry(text, x, y, color));
    }

    private <T extends AbstractWidget> T addScrollable(T widget, int baseY) {
        scrollableWidgets.add(widget);
        widgetBaseY.put(widget, baseY);
        widget.setY(baseY - scrollY);
        return this.addRenderableWidget(widget);
    }

    private void onSearch(String query) {
        filteredIds.clear();
        String q = query.toLowerCase();
        for (String id : modelIds) {
            if (id.toLowerCase().contains(q)) {
                filteredIds.add(id);
            }
        }
        selectedIndex = -1;
        scrollY = 0;
        buildUI();
        applyScroll();
    }

    private void adjustScale(float delta) {
        scaleValue = Math.max(0.1f, Math.min(10.0f, scaleValue + delta));
        buildUI();
        applyScroll();
    }

    private void applyMorph() {
        if (selectedIndex < 0 || selectedIndex >= filteredIds.size()) return;
        String id = filteredIds.get(selectedIndex);
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelId", id);
        tag.putString("TextureId", id);
        tag.putFloat("Scale", scaleValue);
        tag.putBoolean("Morphed", true);
        tag.putBoolean("Visible", visible);
        tag.putString("CurrentAnimation", "");
        tag.putInt("AnimationTick", 0);
        IScriptNetwork.sendToServer(new MorphUpdatePacket(tag));
        this.onClose();
    }

    private void resetMorph() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Morphed", false);
        tag.putString("ModelId", "");
        tag.putString("TextureId", "");
        tag.putString("CurrentAnimation", "");
        tag.putFloat("Scale", 1.0f);
        tag.putBoolean("Visible", true);
        tag.putInt("AnimationTick", 0);
        IScriptNetwork.sendToServer(new MorphUpdatePacket(tag));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        int cx = this.width / 2;
        int panelX = cx - CONTENT_WIDTH / 2;
        int panelTop = PANEL_TOP;
        int panelW = CONTENT_WIDTH;
        int panelH = this.height - PANEL_TOP - PANEL_BOTTOM;

        UI.panel(g, panelX, panelTop, panelW, panelH);
        UI.title(g, this.font, this.title.getString(), cx, 8);

        g.enableScissor(panelX + 2, panelTop + 2, panelX + panelW - 2, panelTop + panelH - 2);

        for (SectionEntry s : sections) {
            UI.title(g, this.font, "— " + s.text + " —", s.x, s.baseY - scrollY);
        }
        for (LabelEntry l : labels) {
            UI.label(g, this.font, l.text, l.x, l.baseY - scrollY, l.color);
        }

        super.render(g, mx, my, pt);

        g.disableScissor();

        if (maxScroll > 0) {
            int trackTop = panelTop + 4;
            int trackHeight = panelH - 8;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbY = trackTop + (int)((float)scrollY / maxScroll * (trackHeight - thumbHeight));
            int scrollBarX = panelX + panelW - 8;

            g.fill(scrollBarX, trackTop, scrollBarX + 4, trackTop + trackHeight, Theme.alpha(Theme.BORDER, 0.2f));
            g.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, Theme.TEXT_DIM);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll <= 0) return false;

        int cx = this.width / 2;
        int panelX = cx - CONTENT_WIDTH / 2;
        int panelTop = PANEL_TOP;
        int panelW = CONTENT_WIDTH;
        int panelH = this.height - PANEL_TOP - PANEL_BOTTOM;

        if (mx < panelX || mx > panelX + panelW || my < panelTop || my > panelTop + panelH) {
            return false;
        }

        if (delta > 0) {
            scrollY = Math.max(0, scrollY - 30);
        } else {
            scrollY = Math.min(scrollY + 30, maxScroll);
        }

        applyScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.getFocused() != null && this.getFocused() != searchBox) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (maxScroll > 0) {
            if (keyCode == 265) {
                scrollY = Math.max(0, scrollY - 30);
                applyScroll();
                return true;
            }
            if (keyCode == 264) {
                scrollY = Math.min(scrollY + 30, maxScroll);
                applyScroll();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int panelX = cx - CONTENT_WIDTH / 2;
        int panelTop = PANEL_TOP;
        int panelW = CONTENT_WIDTH;
        int panelH = this.height - PANEL_TOP - PANEL_BOTTOM;

        boolean insidePanel = mouseX >= panelX && mouseX <= panelX + panelW &&
                mouseY >= panelTop && mouseY <= panelTop + panelH;

        if (!insidePanel) {
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyScroll() {
        int visibleHeight = this.height - PANEL_TOP - PANEL_BOTTOM;
        int contentHeight = contentBottom - contentTop;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (scrollY > maxScroll) scrollY = maxScroll;
        if (scrollY < 0) scrollY = 0;

        for (AbstractWidget w : scrollableWidgets) {
            Integer baseY = widgetBaseY.get(w);
            if (baseY != null) {
                w.setY(baseY - scrollY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}