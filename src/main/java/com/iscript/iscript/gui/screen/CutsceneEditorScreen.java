package com.iscript.iscript.gui.screen;

import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.CutscenePathRenderer;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.CameraShake;
import com.iscript.iscript.data.cutscene.CutsceneAction;
import com.iscript.iscript.data.cutscene.CutsceneActionType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.iscript.iscript.client.camera.PathType;
import com.iscript.iscript.network.packet.ServerCommandPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.iscript.iscript.gui.screen.I18n.t;

public class CutsceneEditorScreen extends Screen {
    private static final int TOPBAR_H = 24;
    private static final int MIN_PANEL_W = 120;
    private static final int MAX_PANEL_W = 300;
    private static final int TIMELINE_H = 90;
    private static final int MIN_TIMELINE_H = 60;
    private static final int MAX_TIMELINE_H = 160;
    private static final int ICON_SIZE = 20;
    private static final int TICK_W = 2;

    private Map<String, CutsceneData> cutscenes = new HashMap<>();
    private String selectedId = null;
    private int selectedActionIndex = -1;
    private int listScroll = 0;
    private int splineScroll = 0;
    private int propScroll = 0;
    private int propsMaxScroll = 0;
    private int labelOffset = 36;
    private int cutscenePanelW = 150;
    private int propsPanelW = 210;
    private int timelineH = TIMELINE_H;

    private boolean showCutscenes = false;
    private boolean showTimeline = false;
    private boolean showProperties = false;
    private boolean dirty = false;
    private boolean pathVisible = false;
    private boolean showAllPaths = false;
    private boolean isPlaying = false;
    private boolean draggingPlayhead = false;
    private boolean draggingAction = false;
    private boolean resizingLeft = false;
    private boolean resizingRight = false;
    private boolean resizingTimeline = false;
    private int dragActionIndex = -1;
    private int dropTargetIndex = -1;
    private int dragStartX = 0;
    private int dragOffsetX = 0;
    private int dragActionWidth = 0;
    private int playheadTick = 0;
    private float timelineScale = 1.0f;
    private int timelineOffset = 0;
    private float playSpeed = 1.0f;
    private long playStartTime = 0;
    private int playStartTick = 0;

    private Interpolation[] interps = Interpolation.values();
    private int interpIndex = 0;

    private static final java.util.Map<PathType, String> PATH_TYPE_NAMES = new java.util.HashMap<>() {{
        put(PathType.LINEAR, "iscript.path_type.linear");
        put(PathType.CATMULL_ROM, "iscript.path_type.catmull_rom");
        put(PathType.CUBIC_BEZIER, "iscript.path_type.cubic_bezier");
        put(PathType.HERMITE, "iscript.path_type.hermite");
        put(PathType.BSPLINE, "iscript.path_type.bspline");
        put(PathType.CIRCULAR, "iscript.path_type.circular");
        put(PathType.ELLIPTICAL, "iscript.path_type.elliptical");
        put(PathType.SPIRAL, "iscript.path_type.spiral");
        put(PathType.HELIX, "iscript.path_type.helix");
        put(PathType.STEP, "iscript.path_type.step");
        put(PathType.NONE, "iscript.path_type.none");
    }};
    private EditBox searchBox, nameBox;
    private EditBox xBox, yBox, zBox, yawBox, pitchBox, rollBox, durationBox, stringBox;
    private List<EditBox> spXBoxes = new ArrayList<>();
    private List<EditBox> spYBoxes = new ArrayList<>();
    private List<EditBox> spZBoxes = new ArrayList<>();
    private List<EditBox> spYawBoxes = new ArrayList<>();
    private List<EditBox> spPitchBoxes = new ArrayList<>();
    private List<Btn> spRemoveBtns = new ArrayList<>();


    private EditBox shakeTraumaBox, shakeDecayBox, shakeAngleBox, shakeOffsetBox;
    private EditBox speedBox;
    private EditBox scriptIdBox, funcBox;
    private boolean splineEnabled = false;
    private EditBox lookAtXBox, lookAtYBox, lookAtZBox;
    private EditBox orbitRadiusBox, orbitHeightBox, orbitSpeedBox;
    private EditBox dollyDistanceBox;

    private Btn playBtn, pauseBtn, stopBtn, prevActionBtn, nextActionBtn;
    private Btn addActionBtn, removeActionBtn, duplicateActionBtn, moveUpBtn, moveDownBtn;
    private Btn saveBtn, backBtn, showPathBtn, showAllPathsBtn;
    private Btn openCutscenesBtn, openTimelineBtn, openPropertiesBtn;
    private Btn zoomInBtn, zoomOutBtn, zoomResetBtn;
    private Button usePosBtn, interpBtn, typeBtn, splineToggleBtn, addSplinePointBtn;
    private Button pathTypeBtn;
    private Button newCutsceneBtn, deleteCutsceneBtn, loopBtn;

    private CutsceneAction clipboardAction = null;

    public CutsceneEditorScreen(Map<String, CutsceneData> cutscenes) {
        this(cutscenes, null);
    }

    public CutsceneEditorScreen(Map<String, CutsceneData> cutscenes, String preselectId) {
        super(t("iscript.cutscene.editor.title"));
        this.cutscenes = new HashMap<>(cutscenes);
        this.selectedId = preselectId;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        spXBoxes.clear(); spYBoxes.clear(); spZBoxes.clear();
        spYawBoxes.clear(); spPitchBoxes.clear(); spRemoveBtns.clear();

        int cy = (TOPBAR_H - ICON_SIZE) / 2;
        int lx = 4;
        addActionBtn = new Btn(lx, cy, ICON_SIZE, ICON_SIZE, "+", Theme.ACCENT, t("iscript.cutscene.editor.add_action"), () -> addAction());
        removeActionBtn = new Btn(lx + 22, cy, ICON_SIZE, ICON_SIZE, "-", Theme.ERROR, t("iscript.cutscene.editor.remove_action"), () -> removeAction());
        duplicateActionBtn = new Btn(lx + 44, cy, ICON_SIZE, ICON_SIZE, "\u29C9", Theme.ACCENT, t("iscript.cutscene.editor.duplicate"), () -> duplicateAction());
        moveUpBtn = new Btn(lx + 66, cy, ICON_SIZE, ICON_SIZE, "\u25B2", Theme.TEXT_DIM, t("iscript.cutscene.editor.move_up"), () -> moveAction(-1));
        moveDownBtn = new Btn(lx + 88, cy, ICON_SIZE, ICON_SIZE, "\u25BC", Theme.TEXT_DIM, t("iscript.cutscene.editor.move_down"), () -> moveAction(1));
        addRenderableWidget(addActionBtn);
        addRenderableWidget(removeActionBtn);
        addRenderableWidget(duplicateActionBtn);
        addRenderableWidget(moveUpBtn);
        addRenderableWidget(moveDownBtn);

        int mx = width / 2 - 70;
        stopBtn = new Btn(mx, cy, ICON_SIZE, ICON_SIZE, "\u25A0", Theme.ERROR, t("iscript.cutscene.editor.stop"), () -> stopPlayback());
        prevActionBtn = new Btn(mx + 22, cy, ICON_SIZE, ICON_SIZE, "\u23EE", Theme.TEXT_DIM, t("iscript.cutscene.editor.prev"), () -> selectActionBy(-1));
        playBtn = new Btn(mx + 44, cy, ICON_SIZE, ICON_SIZE, "\u25B6", Theme.ACCENT, t("iscript.cutscene.editor.play"), () -> startPlayback());
        pauseBtn = new Btn(mx + 44, cy, ICON_SIZE, ICON_SIZE, "\u23F8", Theme.ACCENT, t("iscript.cutscene.editor.pause"), () -> pausePlayback());
        pauseBtn.visible = false;
        nextActionBtn = new Btn(mx + 66, cy, ICON_SIZE, ICON_SIZE, "\u23ED", Theme.TEXT_DIM, t("iscript.cutscene.editor.next"), () -> selectActionBy(1));
        addRenderableWidget(stopBtn);
        addRenderableWidget(prevActionBtn);
        addRenderableWidget(playBtn);
        addRenderableWidget(pauseBtn);
        addRenderableWidget(nextActionBtn);

        int zx = mx + 100;
        zoomOutBtn = new Btn(zx, cy, ICON_SIZE, ICON_SIZE, "\u2212", Theme.TEXT_DIM, t("iscript.cutscene.editor.zoom_out"), () -> setTimelineScale(timelineScale * 0.8f));
        zoomResetBtn = new Btn(zx + 22, cy, ICON_SIZE, ICON_SIZE, "\u27F2", Theme.TEXT_DIM, t("iscript.cutscene.editor.zoom_reset"), () -> { timelineScale = 1.0f; timelineOffset = 0; });
        zoomInBtn = new Btn(zx + 44, cy, ICON_SIZE, ICON_SIZE, "+", Theme.TEXT_DIM, t("iscript.cutscene.editor.zoom_in"), () -> setTimelineScale(timelineScale * 1.25f));
        addRenderableWidget(zoomOutBtn);
        addRenderableWidget(zoomResetBtn);
        addRenderableWidget(zoomInBtn);

        int rx = width - 4;
        backBtn = new Btn(rx - 20, cy, ICON_SIZE, ICON_SIZE, "\u21A9", Theme.ERROR, t("iscript.cutscene.editor.back"), () -> onClose());
        showPathBtn = new Btn(rx - 42, cy, ICON_SIZE, ICON_SIZE, "\u25CE", Theme.TEXT_DIM, t("iscript.cutscene.editor.show_path"), () -> togglePath());
        showAllPathsBtn = new Btn(rx - 64, cy, ICON_SIZE, ICON_SIZE, "\u2726", Theme.TEXT_MUTE, t("iscript.cutscene.editor.show_all_paths"), () -> toggleShowAllPaths());
        openPropertiesBtn = new Btn(rx - 86, cy, ICON_SIZE, ICON_SIZE, "\u2699", Theme.TEXT_DIM, t("iscript.cutscene.editor.properties"), () -> togglePanel("properties"));
        openTimelineBtn = new Btn(rx - 108, cy, ICON_SIZE, ICON_SIZE, "\u29C4", Theme.TEXT_DIM, t("iscript.cutscene.editor.timeline"), () -> togglePanel("timeline"));
        openCutscenesBtn = new Btn(rx - 130, cy, ICON_SIZE, ICON_SIZE, "\u2630", Theme.TEXT_DIM, t("iscript.cutscene.editor.cutscenes"), () -> togglePanel("cutscenes"));
        addRenderableWidget(backBtn);
        addRenderableWidget(showPathBtn);
        addRenderableWidget(showAllPathsBtn);
        addRenderableWidget(openPropertiesBtn);
        addRenderableWidget(openTimelineBtn);
        addRenderableWidget(openCutscenesBtn);

        openCutscenesBtn.setColor(showCutscenes ? Theme.ACCENT : Theme.TEXT_DIM);
        openTimelineBtn.setColor(showTimeline ? Theme.ACCENT : Theme.TEXT_DIM);
        openPropertiesBtn.setColor(showProperties ? Theme.ACCENT : Theme.TEXT_DIM);

        searchBox = new EditBox(font, 0, 0, 100, 14, t("iscript.cutscene.editor.search"));
        searchBox.setResponder(s -> listScroll = 0);
        addRenderableWidget(searchBox);

        nameBox = new EditBox(font, 0, 0, 100, 14, t("iscript.cutscene.editor.name"));
        nameBox.setResponder(s -> dirty = true);
        addRenderableWidget(nameBox);

        newCutsceneBtn = Button.builder(t("iscript.cutscene.editor.new"), b -> createNew()).size(52, 14).build();
        deleteCutsceneBtn = Button.builder(t("iscript.cutscene.editor.delete"), b -> deleteSelected()).size(36, 14).build();
        loopBtn = Button.builder(t("iscript.cutscene.editor.loop"), b -> toggleLoop()).size(60, 14).build();
        addRenderableWidget(newCutsceneBtn);
        addRenderableWidget(deleteCutsceneBtn);
        addRenderableWidget(loopBtn);

        int pw = propsPanelW - 8;
        typeBtn = Button.builder(t("iscript.cutscene.editor.type", "-"), b -> cycleType()).size(pw, 14).build();
        addRenderableWidget(typeBtn);
        xBox = makeField(0, 0, pw); yBox = makeField(0, 0, pw); zBox = makeField(0, 0, pw);
        yawBox = makeField(0, 0, pw); pitchBox = makeField(0, 0, pw); rollBox = makeField(0, 0, pw);
        durationBox = makeField(0, 0, pw); stringBox = makeField(0, 0, pw);

        usePosBtn = Button.builder(t("iscript.cutscene.editor.use_my_pos"), b -> usePlayerPos()).size(80, 14).build();
        interpBtn = Button.builder(t("iscript.cutscene.editor.interp", "LINEAR"), b -> cycleInterp()).size(pw, 14).build();
        splineToggleBtn = Button.builder(t("iscript.cutscene.editor.spline", t("iscript.cutscene.editor.spline_off").getString()), b -> toggleSpline()).size(pw, 14).build();
        addSplinePointBtn = Button.builder(t("iscript.cutscene.editor.add_point"), b -> addSplinePoint()).size(pw, 14).build();
        addRenderableWidget(usePosBtn);
        addRenderableWidget(interpBtn);
        addRenderableWidget(splineToggleBtn);
        addRenderableWidget(addSplinePointBtn);

        pathTypeBtn = Button.builder(t("iscript.cutscene.editor.path", "CATMULL_ROM"), b -> cyclePathType()).size(pw, 14).build();
        addRenderableWidget(pathTypeBtn);
        shakeTraumaBox = makeField(0, 0, pw);
        shakeDecayBox = makeField(0, 0, pw);
        shakeAngleBox = makeField(0, 0, pw);
        shakeOffsetBox = makeField(0, 0, pw);

        lookAtXBox = makeField(0, 0, pw);
        lookAtYBox = makeField(0, 0, pw);
        lookAtZBox = makeField(0, 0, pw);

        orbitRadiusBox = makeField(0, 0, pw);
        orbitHeightBox = makeField(0, 0, pw);
        orbitSpeedBox = makeField(0, 0, pw);

        dollyDistanceBox = makeField(0, 0, pw);

        speedBox = makeField(0, 0, pw);
        scriptIdBox = makeField(0, 0, pw);
        funcBox = makeField(0, 0, pw);
        layout();
        updateSelection();
    }

    private EditBox makeField(int x, int y, int width) {
        EditBox box = new EditBox(font, x + 36, y, width - 36, 12, Component.empty());
        box.setMaxLength(128);
        box.setResponder(s -> { if (selectedActionIndex >= 0) updateActionFromFields(); });
        addRenderableWidget(box);
        return box;
    }

    private void setTimelineScale(float scale) {
        timelineScale = Math.max(0.1f, Math.min(5.0f, scale));
    }

    private void layout() {
        int bottomY = height - (showTimeline ? timelineH : 0);
        int cutX = showCutscenes ? cutscenePanelW : 0;
        int propX = showProperties ? width - propsPanelW : width;

        searchBox.setX(4);
        searchBox.setY(TOPBAR_H + 20);
        searchBox.setWidth(cutscenePanelW - 8);
        searchBox.visible = showCutscenes;

        int btnY = height - 24;
        int pad = 4;
        int availableW = cutscenePanelW - 8;

        int nw = font.width(t("iscript.cutscene.editor.new").getString()) + pad * 2;
        int dw = font.width(t("iscript.cutscene.editor.delete").getString()) + pad * 2;
        int lw = font.width(t("iscript.cutscene.editor.loop").getString()) + pad * 2;

        int totalNeeded = nw + dw + lw + pad * 2;
        if (totalNeeded > availableW) {
            float scale = (float) availableW / totalNeeded;
            nw = (int) (nw * scale);
            dw = (int) (dw * scale);
            lw = (int) (lw * scale);
        }

        newCutsceneBtn.setX(4);
        newCutsceneBtn.setY(btnY);
        newCutsceneBtn.setWidth(nw);
        newCutsceneBtn.visible = showCutscenes;

        deleteCutsceneBtn.setX(newCutsceneBtn.getX() + nw + pad);
        deleteCutsceneBtn.setY(btnY);
        deleteCutsceneBtn.setWidth(dw);
        deleteCutsceneBtn.visible = showCutscenes;

        loopBtn.setX(deleteCutsceneBtn.getX() + dw + pad);
        loopBtn.setY(btnY);
        loopBtn.setWidth(lw);
        loopBtn.visible = showCutscenes;
        loopBtn.active = selectedId != null;

        if (selectedId != null) {
            boolean isLoop = cutscenes.get(selectedId).isLoop();
            loopBtn.setMessage(isLoop ? t("iscript.cutscene.editor.loop_on") : t("iscript.cutscene.editor.loop"));
        }

        nameBox.setX(propX + 4);
        nameBox.setY(TOPBAR_H + 6);
        nameBox.setWidth(propsPanelW - 8);
        nameBox.visible = showProperties && selectedId != null && selectedActionIndex < 0;

        boolean propsVisible = showProperties && selectedId != null && selectedActionIndex >= 0;

        boolean isCamAction = false;
        CutsceneActionType camType = null;
        if (propsVisible) {
            var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
            isCamAction = action.isCameraAction();
            camType = action.getType();
        }

        boolean isShake = camType == CutsceneActionType.CAMERA_SHAKE;
        boolean isLookAt = camType == CutsceneActionType.CAMERA_LOOK || camType == CutsceneActionType.CAMERA_FOLLOW;
        boolean isOrbit = camType == CutsceneActionType.CAMERA_ORBIT;
        boolean isDolly = camType == CutsceneActionType.CAMERA_DOLLY;
        boolean isPath = camType == CutsceneActionType.CAMERA_PATH;

        typeBtn.visible = propsVisible;
        splineToggleBtn.visible = isPath;
        addSplinePointBtn.visible = isPath;
        pathTypeBtn.visible = isPath;
        shakeTraumaBox.visible = isShake;
        shakeDecayBox.visible = isShake;
        shakeAngleBox.visible = isShake;
        shakeOffsetBox.visible = isShake;
        lookAtXBox.visible = isLookAt;
        lookAtYBox.visible = isLookAt;
        lookAtZBox.visible = isLookAt;
        orbitRadiusBox.visible = isOrbit;
        orbitHeightBox.visible = isOrbit;
        orbitSpeedBox.visible = isOrbit;
        dollyDistanceBox.visible = isDolly;
        speedBox.visible = propsVisible && isCamAction;

        labelOffset = 36;
        if (propsVisible) {
            String[] labelKeys = {
                    "iscript.cutscene.editor.x",
                    "iscript.cutscene.editor.y",
                    "iscript.cutscene.editor.z",
                    "iscript.cutscene.editor.yaw",
                    "iscript.cutscene.editor.pitch",
                    "iscript.cutscene.editor.roll",
                    "iscript.cutscene.editor.duration",
                    "iscript.cutscene.editor.value",
                    "iscript.cutscene.editor.shake_trauma",
                    "iscript.cutscene.editor.shake_decay",
                    "iscript.cutscene.editor.shake_angle",
                    "iscript.cutscene.editor.shake_offset",
                    "iscript.cutscene.editor.lookat_x",
                    "iscript.cutscene.editor.lookat_y",
                    "iscript.cutscene.editor.lookat_z",
                    "iscript.cutscene.editor.orbit_radius",
                    "iscript.cutscene.editor.orbit_height",
                    "iscript.cutscene.editor.orbit_speed",
                    "iscript.cutscene.editor.dolly_distance",
                    "iscript.cutscene.editor.speed"
            };
            int maxW = 0;
            for (String key : labelKeys) {
                int w = font.width(t(key).getString());
                if (w > maxW) maxW = w;
            }
            labelOffset = Math.min(100, Math.max(36, maxW + 8));
        }

        int py = TOPBAR_H + 24 - propScroll;
        int pw = propsPanelW - 8;
        typeBtn.setX(propX + 4); typeBtn.setY(py); typeBtn.setWidth(pw); if (typeBtn.visible) py += 18;
        xBox.setX(propX + 4 + labelOffset); xBox.setY(py); xBox.setWidth(pw - labelOffset); if (xBox.visible) py += 18;
        yBox.setX(propX + 4 + labelOffset); yBox.setY(py); yBox.setWidth(pw - labelOffset); if (yBox.visible) py += 18;
        zBox.setX(propX + 4 + labelOffset); zBox.setY(py); zBox.setWidth(pw - labelOffset); if (zBox.visible) py += 18;
        yawBox.setX(propX + 4 + labelOffset); yawBox.setY(py); yawBox.setWidth(pw - labelOffset); if (yawBox.visible) py += 18;
        pitchBox.setX(propX + 4 + labelOffset); pitchBox.setY(py); pitchBox.setWidth(pw - labelOffset); if (pitchBox.visible) py += 18;
        rollBox.setX(propX + 4 + labelOffset); rollBox.setY(py); rollBox.setWidth(pw - labelOffset); if (rollBox.visible) py += 18;
        durationBox.setX(propX + 4 + labelOffset); durationBox.setY(py); durationBox.setWidth(pw - labelOffset); if (durationBox.visible) py += 18;
        stringBox.setX(propX + 4 + labelOffset); stringBox.setY(py); stringBox.setWidth(pw - labelOffset); if (stringBox.visible) py += 18;

        usePosBtn.setX(propX + 4); usePosBtn.setY(py); if (usePosBtn.visible) py += 18;
        interpBtn.setX(propX + 4); interpBtn.setY(py); if (interpBtn.visible) py += 18;
        splineToggleBtn.setX(propX + 4); splineToggleBtn.setY(py); if (splineToggleBtn.visible) py += 18;
        addSplinePointBtn.setX(propX + 4); addSplinePointBtn.setY(py); if (addSplinePointBtn.visible) py += 18;

        pathTypeBtn.setX(propX + 4); pathTypeBtn.setY(py); if (pathTypeBtn.visible) py += 18;

        shakeTraumaBox.setX(propX + 4 + labelOffset); shakeTraumaBox.setY(py); shakeTraumaBox.setWidth(pw - labelOffset); if (shakeTraumaBox.visible) py += 18;
        shakeDecayBox.setX(propX + 4 + labelOffset); shakeDecayBox.setY(py); shakeDecayBox.setWidth(pw - labelOffset); if (shakeDecayBox.visible) py += 18;
        shakeAngleBox.setX(propX + 4 + labelOffset); shakeAngleBox.setY(py); shakeAngleBox.setWidth(pw - labelOffset); if (shakeAngleBox.visible) py += 18;
        shakeOffsetBox.setX(propX + 4 + labelOffset); shakeOffsetBox.setY(py); shakeOffsetBox.setWidth(pw - labelOffset); if (shakeOffsetBox.visible) py += 18;

        lookAtXBox.setX(propX + 4 + labelOffset); lookAtXBox.setY(py); lookAtXBox.setWidth(pw - labelOffset); if (lookAtXBox.visible) py += 18;
        lookAtYBox.setX(propX + 4 + labelOffset); lookAtYBox.setY(py); lookAtYBox.setWidth(pw - labelOffset); if (lookAtYBox.visible) py += 18;
        lookAtZBox.setX(propX + 4 + labelOffset); lookAtZBox.setY(py); lookAtZBox.setWidth(pw - labelOffset); if (lookAtZBox.visible) py += 18;

        orbitRadiusBox.setX(propX + 4 + labelOffset); orbitRadiusBox.setY(py); orbitRadiusBox.setWidth(pw - labelOffset); if (orbitRadiusBox.visible) py += 18;
        orbitHeightBox.setX(propX + 4 + labelOffset); orbitHeightBox.setY(py); orbitHeightBox.setWidth(pw - labelOffset); if (orbitHeightBox.visible) py += 18;
        orbitSpeedBox.setX(propX + 4 + labelOffset); orbitSpeedBox.setY(py); orbitSpeedBox.setWidth(pw - labelOffset); if (orbitSpeedBox.visible) py += 18;

        dollyDistanceBox.setX(propX + 4 + labelOffset); dollyDistanceBox.setY(py); dollyDistanceBox.setWidth(pw - labelOffset); if (dollyDistanceBox.visible) py += 18;
        speedBox.setX(propX + 4 + labelOffset); speedBox.setY(py); speedBox.setWidth(pw - labelOffset); if (speedBox.visible) py += 18;
        scriptIdBox.setX(propX + 4 + labelOffset); scriptIdBox.setY(py); scriptIdBox.setWidth(pw - labelOffset); if (scriptIdBox.visible) py += 18;
        funcBox.setX(propX + 4 + labelOffset); funcBox.setY(py); funcBox.setWidth(pw - labelOffset); if (funcBox.visible) py += 18;

        if (splineToggleBtn.visible) py += 14;
        updateSplineLayout(propX + 4, py, pw);

        int contentBottom = TOPBAR_H + 24;
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                if (w.visible && w.getX() >= propX && w.getX() < propX + propsPanelW) {
                    contentBottom = Math.max(contentBottom, w.getY() + w.getHeight() + propScroll);
                }
            }
        }
        if (splineToggleBtn.visible && !spXBoxes.isEmpty()) {
            int splineBottom = addSplinePointBtn.getY() + 32;
            for (var b : spXBoxes) if (b.visible) splineBottom = Math.max(splineBottom, b.getY() + 32);
            contentBottom = Math.max(contentBottom, splineBottom + propScroll);
        }
        propsMaxScroll = Math.max(0, contentBottom - height + 40);
    }

    private void cyclePathType() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        if (!action.isCameraAction()) return;

        PathType[] types = PathType.VALUES;
        int idx = 0;
        try {
            idx = java.util.Arrays.asList(types).indexOf(PathType.valueOf(action.getPathType()));
        } catch (Exception e) {}
        if (idx < 0) idx = 0;
        idx = (idx + 1) % types.length;

        PathType next = types[idx];
        action.setPathType(next.name());

        String key = PATH_TYPE_NAMES.getOrDefault(next, next.name());
        String full = Component.translatable(key).getString();
        String text = t("iscript.cutscene.editor.path", full).getString();
        String trimmed = font.plainSubstrByWidth(text, propsPanelW - 16);
        pathTypeBtn.setMessage(Component.literal(trimmed));
        dirty = true;
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void updateSplineLayout(int x, int startY, int w) {
        for (var b : spXBoxes) b.visible = false;
        for (var b : spYBoxes) b.visible = false;
        for (var b : spZBoxes) b.visible = false;
        for (var b : spYawBoxes) b.visible = false;
        for (var b : spPitchBoxes) b.visible = false;
        for (var b : spRemoveBtns) b.visible = false;

        if (!showProperties || selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        if (action.getType() != CutsceneActionType.CAMERA_PATH) return;

        int maxScroll = Math.max(0, action.getSplinePoints().size() - 5);
        splineScroll = Math.max(0, Math.min(splineScroll, maxScroll));

        for (int i = 0; i < action.getSplinePoints().size(); i++) {
            int rowY = startY + (i - splineScroll) * 32;
            boolean vis = i >= splineScroll && i < splineScroll + 5;
            if (!vis) continue;
            Vec3 pt = action.getSplinePoints().get(i);
            float yaw = i < action.getSplineYaws().size() ? action.getSplineYaws().get(i) : 0;
            float pitch = i < action.getSplinePitches().size() ? action.getSplinePitches().get(i) : 0;

            spXBoxes.get(i).setX(x); spXBoxes.get(i).setY(rowY); spXBoxes.get(i).setWidth((w - 20) / 3); spXBoxes.get(i).visible = true; spXBoxes.get(i).setValue(String.format("%.2f", pt.x));
            spYBoxes.get(i).setX(x + (w - 20) / 3 + 2); spYBoxes.get(i).setY(rowY); spYBoxes.get(i).setWidth((w - 20) / 3); spYBoxes.get(i).visible = true; spYBoxes.get(i).setValue(String.format("%.2f", pt.y));
            spZBoxes.get(i).setX(x + (w - 20) / 3 * 2 + 4); spZBoxes.get(i).setY(rowY); spZBoxes.get(i).setWidth((w - 20) / 3); spZBoxes.get(i).visible = true; spZBoxes.get(i).setValue(String.format("%.2f", pt.z));
            spYawBoxes.get(i).setX(x); spYawBoxes.get(i).setY(rowY + 16); spYawBoxes.get(i).setWidth(w / 2 - 2); spYawBoxes.get(i).visible = true; spYawBoxes.get(i).setValue(String.format("%.1f", yaw));
            spPitchBoxes.get(i).setX(x + w / 2 + 2); spPitchBoxes.get(i).setY(rowY + 16); spPitchBoxes.get(i).setWidth(w / 2 - 2); spPitchBoxes.get(i).visible = true; spPitchBoxes.get(i).setValue(String.format("%.1f", pitch));
            spRemoveBtns.get(i).setX(x + w - 16); spRemoveBtns.get(i).setY(rowY); spRemoveBtns.get(i).visible = true;
        }
    }

    private void togglePanel(String panel) {
        switch (panel) {
            case "cutscenes" -> showCutscenes = !showCutscenes;
            case "timeline" -> showTimeline = !showTimeline;
            case "properties" -> showProperties = !showProperties;
        }
        openCutscenesBtn.setColor(showCutscenes ? Theme.ACCENT : Theme.TEXT_DIM);
        openTimelineBtn.setColor(showTimeline ? Theme.ACCENT : Theme.TEXT_DIM);
        openPropertiesBtn.setColor(showProperties ? Theme.ACCENT : Theme.TEXT_DIM);
        layout();
    }

    private void startPlayback() {
        if (selectedId == null) return;
        int total = getTotalDuration();
        if (total <= 0) return;

        if (playheadTick >= total) {
            playheadTick = 0;
            playStartTick = 0;
        }

        isPlaying = true;
        playBtn.visible = false;
        pauseBtn.visible = true;

        playStartTick = playheadTick;
        playStartTime = System.currentTimeMillis();
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(selectedId, playSpeed, playheadTick)));
        dirty = true;
    }

    private void pausePlayback() {
        if (!isPlaying) return;

        isPlaying = false;
        playBtn.visible = true;
        pauseBtn.visible = false;

        long elapsed = System.currentTimeMillis() - playStartTime;
        playheadTick = playStartTick + (int) (elapsed / 50f * playSpeed);
        int total = getTotalDuration();
        if (playheadTick > total) playheadTick = total;

        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PAUSE_CUTSCENE, new CompoundTag()));
    }

    private void stopPlayback() {
        isPlaying = false;
        playBtn.visible = true;
        pauseBtn.visible = false;
        playheadTick = 0;
        playStartTick = 0;
        playStartTime = 0;

        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.STOP_CUTSCENE, ServerCommandPacket.stopCutsceneToTag(true)));
    }

    private void updatePlayback() {
        if (!isPlaying || selectedId == null) return;
        int total = getTotalDuration();
        if (total <= 0) {
            stopPlayback();
            return;
        }

        long elapsed = System.currentTimeMillis() - playStartTime;
        playheadTick = playStartTick + (int) (elapsed / 50f * playSpeed);

        if (playheadTick > total) playheadTick = total;

        if (playheadTick >= total) {
            var data = cutscenes.get(selectedId);
            if (data.isLoop()) {
                playStartTime = System.currentTimeMillis();
                playStartTick = 0;
                playheadTick = 0;
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(selectedId, playSpeed, 0)));
            } else {
                stopPlayback();
            }
        }
    }

    private void createNew() {
        String id = "cutscene_" + System.currentTimeMillis();
        CutsceneData data = new CutsceneData();
        data.setId(id);
        data.setName("New Cutscene");
        cutscenes.put(id, data);
        selectedId = id;
        selectedActionIndex = -1;
        dirty = true;
        updateSelection();
    }

    private void deleteSelected() {
        if (selectedId == null) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_CUTSCENE, ServerCommandPacket.deleteCutsceneToTag(selectedId)));
        cutscenes.remove(selectedId);
        selectedId = null;
        selectedActionIndex = -1;
        updateSelection();
    }

    private void toggleLoop() {
        if (selectedId == null) return;
        var data = cutscenes.get(selectedId);
        data.setLoop(!data.isLoop());
        dirty = true;
        layout();
        autoSave();
    }

    private void preview() {
        save();
        if (selectedId != null) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(selectedId, 1.0f, 0)));
        }
    }

    private void togglePath() {
        pathVisible = !pathVisible;
        showPathBtn.setColor(pathVisible ? Theme.ACCENT : Theme.TEXT_DIM);
        updatePathPreview();
    }

    private void toggleShowAllPaths() {
        showAllPaths = !showAllPaths;
        showAllPathsBtn.setColor(showAllPaths ? Theme.ACCENT : Theme.TEXT_MUTE);
        updateAllPaths();
    }

    private void updateAllPaths() {
        if (!showAllPaths) { CutscenePathRenderer.clearAllPaths(); return; }
        Map<String, CutscenePath> map = new HashMap<>();
        for (var e : cutscenes.entrySet()) {
            CutscenePath path = buildPathFromData(e.getValue());
            if (path != null && path.keyframes.size() >= 2) map.put(e.getKey(), path);
        }
        CutscenePathRenderer.setAllPaths(map);
    }

    private void updatePathPreview() {
        if (!pathVisible || minecraft == null) { CutscenePathRenderer.clearPreview(); return; }
        var path = buildPathFromActions();
        if (path != null && !path.keyframes.isEmpty()) {
            var mode = buildCameraModeFromData(cutscenes.get(selectedId));
            CutscenePathRenderer.setPreviewPath(path, mode);
        }
        else CutscenePathRenderer.clearPreview();
        if (showAllPaths) updateAllPaths();
    }

    private CutscenePath buildPathFromActions() {
        if (selectedId == null) return null;
        return buildPathFromData(cutscenes.get(selectedId));
    }

    private CutscenePath buildPathFromData(CutsceneData data) {
        CutscenePath path = new CutscenePath();
        float currentTick = 0;

        for (var action : data.getActions()) {
            if (action.isCameraAction()) {
                try {
                    path.easing = Interpolation.valueOf(action.getInterpolation());
                } catch (Exception e) {}

                PathType segmentType;
                try {
                    segmentType = PathType.valueOf(action.getPathType());
                } catch (Exception e) {
                    segmentType = PathType.CATMULL_ROM;
                }

                if (!action.getSplinePoints().isEmpty()) {
                    for (int i = 0; i < action.getSplinePoints().size(); i++) {
                        Vec3 p = action.getSplinePoints().get(i);
                        float yaw = i < action.getSplineYaws().size() ? action.getSplineYaws().get(i) : action.getYaw();
                        float pitch = i < action.getSplinePitches().size() ? action.getSplinePitches().get(i) : action.getPitch();
                        path.keyframes.add(new CutscenePath.Keyframe(p, yaw, pitch, action.getRoll(), action.getFov(), currentTick, segmentType));
                    }
                } else {
                    path.keyframes.add(new CutscenePath.Keyframe(
                            new Vec3(action.getX(), action.getY(), action.getZ()),
                            action.getYaw(), action.getPitch(), action.getRoll(), action.getFov(), currentTick, segmentType
                    ));
                }
            }
            currentTick += action.getDuration();
        }

        float totalDur = 0;
        for (var a : data.getActions()) totalDur += a.getDuration();
        path.durationTicks = Math.max(totalDur, 1);

        return path.keyframes.size() < 2 ? null : path;
    }

    private CutsceneCameraMode buildCameraModeFromData(CutsceneData data) {
        CutsceneCameraMode mode = new CutsceneCameraMode();
        for (var action : data.getActions()) {
            if (!action.isCameraAction()) continue;
            switch (action.getType()) {
                case CAMERA_SHAKE -> {
                    mode.shake = new CameraShake();
                    mode.shake.setTrauma(action.getShakeTrauma());
                    mode.shake.setDecay(action.getShakeDecay());
                    mode.shake.setMaxAngle(action.getShakeMaxAngle());
                    mode.shake.setMaxOffset(action.getShakeMaxOffset());
                }
                case CAMERA_LOOK, CAMERA_FOLLOW -> {
                    mode.useLookAt = true;
                    mode.lookAtTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                }
                case CAMERA_ORBIT -> {
                    mode.orbitMode = true;
                    mode.orbitRadius = action.getOrbitRadius();
                    mode.orbitHeight = action.getOrbitHeight();
                    mode.orbitSpeed = action.getOrbitSpeed();
                    mode.orbitCenter = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                }
                case CAMERA_DOLLY -> {
                    mode.useDollyZoom = true;
                    mode.dollyTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                    mode.dollyBaseFov = action.getFov();
                }
            }
        }
        return mode;
    }

    private void selectActionBy(int delta) {
        if (selectedId == null) return;
        var data = cutscenes.get(selectedId);
        int idx = selectedActionIndex + delta;
        if (idx >= 0 && idx < data.getActions().size()) {
            selectedActionIndex = idx;
            updateActionFields();
        }
    }

    private void moveAction(int delta) {
        if (selectedId == null || selectedActionIndex < 0) return;
        var data = cutscenes.get(selectedId);
        int to = selectedActionIndex + delta;
        if (to < 0 || to >= data.getActions().size()) return;
        var actions = data.getActions();
        var temp = actions.get(selectedActionIndex);
        actions.set(selectedActionIndex, actions.get(to));
        actions.set(to, temp);
        selectedActionIndex = to;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void moveActionTo(int fromIndex, int toIndex) {
        if (selectedId == null || fromIndex == toIndex) return;
        var actions = cutscenes.get(selectedId).getActions();
        if (fromIndex < 0 || fromIndex >= actions.size()) return;
        if (toIndex < 0 || toIndex > actions.size()) return;

        var action = actions.remove(fromIndex);
        if (toIndex > fromIndex) toIndex--;
        actions.add(toIndex, action);
        selectedActionIndex = toIndex;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private int getActionIndexAtScreenX(int screenX) {
        if (selectedId == null) return -1;
        int tx = showCutscenes ? cutscenePanelW : 0;
        int propX = showProperties ? width - propsPanelW : width;
        int trackXStart = tx + 4;
        int trackXEnd = propX - 4;
        if (screenX < trackXStart) return 0;
        if (screenX > trackXEnd) return cutscenes.get(selectedId).getActions().size();

        for (int i = 0; i < cutscenes.get(selectedId).getActions().size(); i++) {
            int startTick = getActionStartTick(i);
            int endTick = startTick + cutscenes.get(selectedId).getActions().get(i).getDuration();
            int sx1 = tickToScreenX(startTick);
            int sx2 = tickToScreenX(endTick);
            sx1 = Math.max(trackXStart, sx1);
            sx2 = Math.min(trackXEnd, sx2);
            if (screenX >= sx1 && screenX <= sx2) {
                return i;
            }
        }
        return -1;
    }

    private int getActionIndexAtTick(int tick) {
        if (selectedId == null) return 0;
        var actions = cutscenes.get(selectedId).getActions();
        int accumulated = 0;
        for (int i = 0; i < actions.size(); i++) {
            int dur = actions.get(i).getDuration();
            if (tick < accumulated + dur) {
                return i;
            }
            accumulated += dur;
        }
        return actions.size();
    }

    private void cycleType() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        var types = CutsceneActionType.values();
        action.setType(types[(action.getType().ordinal() + 1) % types.length]);
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void cycleInterp() {
        if (selectedId == null || selectedActionIndex < 0) return;
        interpIndex = (interpIndex + 1) % interps.length;
        cutscenes.get(selectedId).getActions().get(selectedActionIndex).setInterpolation(interps[interpIndex].name());
        interpBtn.setMessage(t("iscript.cutscene.editor.interp", interps[interpIndex].name()));
        dirty = true;
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void toggleSpline() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        if (action.getType() != CutsceneActionType.CAMERA_PATH) return;
        splineEnabled = !splineEnabled;
        splineToggleBtn.setMessage(t("iscript.cutscene.editor.spline", splineEnabled ? t("iscript.cutscene.editor.spline_on").getString() : t("iscript.cutscene.editor.spline_off").getString()));
        dirty = true;
        layout();
    }

    private void addSplinePoint() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        if (action.getType() != CutsceneActionType.CAMERA_PATH) return;
        if (minecraft == null || minecraft.player == null) return;
        var p = minecraft.player;
        action.addSplinePoint(new Vec3(p.getX(), p.getY() + p.getEyeHeight(), p.getZ()), p.getYRot(), p.getXRot());
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void removeSplinePoint(int index) {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        if (index >= 0 && index < action.getSplinePoints().size()) {
            action.getSplinePoints().remove(index);
            action.getSplineYaws().remove(index);
            action.getSplinePitches().remove(index);
            dirty = true;
            updateActionFields();
            if (pathVisible) updatePathPreview();
            autoSave();
        }
    }

    private void addAction() {
        if (selectedId == null) return;
        var data = cutscenes.get(selectedId);
        var action = new CutsceneAction();
        action.setType(CutsceneActionType.DELAY);
        action.setDuration(20);
        if (selectedActionIndex >= 0 && selectedActionIndex < data.getActions().size() - 1) {
            data.getActions().add(selectedActionIndex + 1, action);
            selectedActionIndex++;
        } else {
            data.getActions().add(action);
            selectedActionIndex = data.getActions().size() - 1;
        }
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void removeAction() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var data = cutscenes.get(selectedId);
        data.getActions().remove(selectedActionIndex);
        if (selectedActionIndex >= data.getActions().size()) selectedActionIndex = data.getActions().size() - 1;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void duplicateAction() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var data = cutscenes.get(selectedId);
        var original = data.getActions().get(selectedActionIndex);
        var copy = new CutsceneAction();
        CompoundTag tag = new CompoundTag();
        original.save(tag);
        copy.load(tag);
        data.getActions().add(selectedActionIndex + 1, copy);
        selectedActionIndex++;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void copyAction() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var original = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        clipboardAction = new CutsceneAction();
        CompoundTag tag = new CompoundTag();
        original.save(tag);
        clipboardAction.load(tag);
    }

    private void pasteAction() {
        if (selectedId == null || clipboardAction == null) return;
        var data = cutscenes.get(selectedId);
        var copy = new CutsceneAction();
        CompoundTag tag = new CompoundTag();
        clipboardAction.save(tag);
        copy.load(tag);
        if (selectedActionIndex >= 0 && selectedActionIndex < data.getActions().size() - 1) {
            data.getActions().add(selectedActionIndex + 1, copy);
            selectedActionIndex++;
        } else {
            data.getActions().add(copy);
            selectedActionIndex = data.getActions().size() - 1;
        }
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void usePlayerPos() {
        if (minecraft == null || minecraft.player == null) return;
        var p = minecraft.player;
        xBox.setValue(String.format("%.2f", p.getX()));
        yBox.setValue(String.format("%.2f", p.getY() + p.getEyeHeight()));
        zBox.setValue(String.format("%.2f", p.getZ()));
        yawBox.setValue(String.format("%.1f", p.getYRot()));
        pitchBox.setValue(String.format("%.1f", p.getXRot()));
        updateActionFromFields();
        if (pathVisible) updatePathPreview();
    }

    private void updateSelection() {
        if (selectedId != null && cutscenes.containsKey(selectedId)) {
            var data = cutscenes.get(selectedId);
            nameBox.setValue(data.getName());
            updateActionFields();
        } else {
            nameBox.setValue("");
            selectedActionIndex = -1;
            updateActionFields();
        }
    }

    private void clearSplineWidgets() {
        for (var b : spXBoxes) removeWidget(b);
        for (var b : spYBoxes) removeWidget(b);
        for (var b : spZBoxes) removeWidget(b);
        for (var b : spYawBoxes) removeWidget(b);
        for (var b : spPitchBoxes) removeWidget(b);
        for (var b : spRemoveBtns) removeWidget(b);
        spXBoxes.clear(); spYBoxes.clear(); spZBoxes.clear(); spYawBoxes.clear(); spPitchBoxes.clear(); spRemoveBtns.clear();
    }

    private void updateActionFields() {
        clearSplineWidgets();
        splineScroll = 0;
        if (selectedId == null || selectedActionIndex < 0 || selectedActionIndex >= cutscenes.get(selectedId).getActions().size()) {
            typeBtn.setMessage(t("iscript.cutscene.editor.type", "-"));
            xBox.setValue(""); yBox.setValue(""); zBox.setValue("");
            yawBox.setValue(""); pitchBox.setValue(""); rollBox.setValue("");
            durationBox.setValue(""); stringBox.setValue("");
            xBox.visible = false; yBox.visible = false; zBox.visible = false;
            yawBox.visible = false; pitchBox.visible = false; rollBox.visible = false;
            durationBox.visible = false; stringBox.visible = false;
            usePosBtn.visible = false; interpBtn.visible = false;
            typeBtn.visible = false;
            pathTypeBtn.visible = false;

            layout();
            return;
        }

        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        typeBtn.setMessage(t("iscript.cutscene.editor.type", action.getType().name()));
        typeBtn.active = true;
        xBox.setValue(String.valueOf(action.getX()));
        yBox.setValue(String.valueOf(action.getY()));
        zBox.setValue(String.valueOf(action.getZ()));
        yawBox.setValue(String.valueOf(action.getYaw()));
        pitchBox.setValue(String.valueOf(action.getPitch()));
        rollBox.setValue(String.valueOf(action.getRoll()));
        durationBox.setValue(String.valueOf(action.getDuration()));
        stringBox.setValue(action.getStringValue());

        try {
            interpIndex = java.util.Arrays.asList(interps).indexOf(Interpolation.valueOf(action.getInterpolation()));
            if (interpIndex < 0) interpIndex = 0;
        } catch (Exception e) { interpIndex = 0; }
        interpBtn.setMessage(t("iscript.cutscene.editor.interp", interps[interpIndex].name()));

        try {
            PathType pt = PathType.valueOf(action.getPathType());
            String key = PATH_TYPE_NAMES.getOrDefault(pt, pt.name());
            String full = Component.translatable(key).getString();
            String text = t("iscript.cutscene.editor.path", full).getString();
            String trimmed = font.plainSubstrByWidth(text, propsPanelW - 16);
            pathTypeBtn.setMessage(Component.literal(trimmed));
        } catch (Exception e) {
            String def = Component.translatable("iscript.path_type.catmull_rom").getString();
            String text = t("iscript.cutscene.editor.path", def).getString();
            String trimmed = font.plainSubstrByWidth(text, propsPanelW - 16);
            pathTypeBtn.setMessage(Component.literal(trimmed));
        }
        pathTypeBtn.active = action.getType() == CutsceneActionType.CAMERA_PATH;


        shakeTraumaBox.setValue(String.format("%.2f", action.getShakeTrauma()));
        shakeDecayBox.setValue(String.format("%.2f", action.getShakeDecay()));
        shakeAngleBox.setValue(String.format("%.1f", action.getShakeMaxAngle()));
        shakeOffsetBox.setValue(String.format("%.2f", action.getShakeMaxOffset()));


        lookAtXBox.setValue(String.format("%.2f", action.getLookAtX()));
        lookAtYBox.setValue(String.format("%.2f", action.getLookAtY()));
        lookAtZBox.setValue(String.format("%.2f", action.getLookAtZ()));


        orbitRadiusBox.setValue(String.format("%.1f", action.getOrbitRadius()));
        orbitHeightBox.setValue(String.format("%.1f", action.getOrbitHeight()));
        orbitSpeedBox.setValue(String.format("%.1f", action.getOrbitSpeed()));


        dollyDistanceBox.setValue(String.format("%.1f", action.getDollyTargetDistance()));
        speedBox.setValue(String.valueOf(action.getSpeed()));
        scriptIdBox.setValue(action.getScriptId());
        funcBox.setValue(action.getFunctionName());

        boolean needsPos = action.isCameraAction() || action.getType() == CutsceneActionType.NPC_MOVE || action.getType() == CutsceneActionType.BLOCK;
        boolean needsRot = action.isCameraAction();
        boolean needsDur = action.getType() == CutsceneActionType.DELAY || action.isCameraAction() || action.getType() == CutsceneActionType.NPC_MOVE;
        boolean needsStr = action.getType() == CutsceneActionType.DIALOG || action.getType() == CutsceneActionType.SOUND || action.getType() == CutsceneActionType.BLOCK || action.getType() == CutsceneActionType.NPC_ANIMATION;
        boolean needsScriptRun = action.getType() == CutsceneActionType.SCRIPT_RUN;

        xBox.visible = needsPos; yBox.visible = needsPos; zBox.visible = needsPos;
        yawBox.visible = needsRot; pitchBox.visible = needsRot; rollBox.visible = needsRot;
        durationBox.visible = needsDur; stringBox.visible = needsStr;
        scriptIdBox.visible = needsScriptRun;
        funcBox.visible = needsScriptRun;
        usePosBtn.visible = needsPos;
        interpBtn.visible = action.isCameraAction();

        boolean isCam = action.isCameraAction();
        boolean isPath = action.getType() == CutsceneActionType.CAMERA_PATH;
        splineToggleBtn.visible = isPath;
        splineToggleBtn.active = isPath;

        if (isPath) {
            for (int i = 0; i < action.getSplinePoints().size(); i++) {
                Vec3 pt = action.getSplinePoints().get(i);
                float yaw = i < action.getSplineYaws().size() ? action.getSplineYaws().get(i) : 0;
                float pitch = i < action.getSplinePitches().size() ? action.getSplinePitches().get(i) : 0;

                EditBox bx = new EditBox(font, 0, 0, 40, 12, Component.empty());
                bx.setMaxLength(32); bx.setValue(String.format("%.2f", pt.x)); bx.setResponder(s -> updateSplineFromFields());
                spXBoxes.add(bx); addRenderableWidget(bx);

                EditBox by = new EditBox(font, 0, 0, 40, 12, Component.empty());
                by.setMaxLength(32); by.setValue(String.format("%.2f", pt.y)); by.setResponder(s -> updateSplineFromFields());
                spYBoxes.add(by); addRenderableWidget(by);

                EditBox bz = new EditBox(font, 0, 0, 40, 12, Component.empty());
                bz.setMaxLength(32); bz.setValue(String.format("%.2f", pt.z)); bz.setResponder(s -> updateSplineFromFields());
                spZBoxes.add(bz); addRenderableWidget(bz);

                EditBox byaw = new EditBox(font, 0, 0, 36, 12, Component.empty());
                byaw.setMaxLength(32); byaw.setValue(String.format("%.1f", yaw)); byaw.setResponder(s -> updateSplineFromFields());
                spYawBoxes.add(byaw); addRenderableWidget(byaw);

                EditBox bpitch = new EditBox(font, 0, 0, 36, 12, Component.empty());
                bpitch.setMaxLength(32); bpitch.setValue(String.format("%.1f", pitch)); bpitch.setResponder(s -> updateSplineFromFields());
                spPitchBoxes.add(bpitch); addRenderableWidget(bpitch);

                final int idx = i;
                Btn rem = new Btn(0, 0, 12, 12, "\u00D7", Theme.ERROR, t("iscript.cutscene.editor.remove"), () -> removeSplinePoint(idx));
                spRemoveBtns.add(rem); addRenderableWidget(rem);
            }
        }

        layout();
    }

    private void updateActionFromFields() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        try { action.setX(Double.parseDouble(xBox.getValue())); } catch (Exception ignored) {}
        try { action.setY(Double.parseDouble(yBox.getValue())); } catch (Exception ignored) {}
        try { action.setZ(Double.parseDouble(zBox.getValue())); } catch (Exception ignored) {}
        try { action.setYaw(Float.parseFloat(yawBox.getValue())); } catch (Exception ignored) {}
        try { action.setPitch(Float.parseFloat(pitchBox.getValue())); } catch (Exception ignored) {}
        try { action.setRoll(Float.parseFloat(rollBox.getValue())); } catch (Exception ignored) {}
        try { action.setDuration(Integer.parseInt(durationBox.getValue())); } catch (Exception ignored) {}
        action.setStringValue(stringBox.getValue());

        try { action.setShakeTrauma(Float.parseFloat(shakeTraumaBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeDecay(Float.parseFloat(shakeDecayBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeMaxAngle(Float.parseFloat(shakeAngleBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeMaxOffset(Float.parseFloat(shakeOffsetBox.getValue())); } catch (Exception ignored) {}

        try { action.setLookAtX(Double.parseDouble(lookAtXBox.getValue())); } catch (Exception ignored) {}
        try { action.setLookAtY(Double.parseDouble(lookAtYBox.getValue())); } catch (Exception ignored) {}
        try { action.setLookAtZ(Double.parseDouble(lookAtZBox.getValue())); } catch (Exception ignored) {}

        try { action.setOrbitRadius(Float.parseFloat(orbitRadiusBox.getValue())); } catch (Exception ignored) {}
        try { action.setOrbitHeight(Float.parseFloat(orbitHeightBox.getValue())); } catch (Exception ignored) {}
        try { action.setOrbitSpeed(Float.parseFloat(orbitSpeedBox.getValue())); } catch (Exception ignored) {}

        try { action.setDollyTargetDistance(Float.parseFloat(dollyDistanceBox.getValue())); } catch (Exception ignored) {}
        try { action.setSpeed(Float.parseFloat(speedBox.getValue())); } catch (Exception ignored) {}
        try { action.setScriptId(scriptIdBox.getValue()); } catch (Exception ignored) {}
        try { action.setFunctionName(funcBox.getValue()); } catch (Exception ignored) {}

        dirty = true;
        autoSave();
    }

    private void updateSplineFromFields() {
        if (selectedId == null || selectedActionIndex < 0) return;
        var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
        for (int i = 0; i < spXBoxes.size() && i < action.getSplinePoints().size(); i++) {
            try {
                double nx = Double.parseDouble(spXBoxes.get(i).getValue());
                double ny = Double.parseDouble(spYBoxes.get(i).getValue());
                double nz = Double.parseDouble(spZBoxes.get(i).getValue());
                action.getSplinePoints().set(i, new Vec3(nx, ny, nz));
            } catch (Exception ignored) {}
            try {
                float yaw = Float.parseFloat(spYawBoxes.get(i).getValue());
                if (i < action.getSplineYaws().size()) action.getSplineYaws().set(i, yaw);
                else action.getSplineYaws().add(yaw);
            } catch (Exception ignored) {}
            try {
                float pitch = Float.parseFloat(spPitchBoxes.get(i).getValue());
                if (i < action.getSplinePitches().size()) action.getSplinePitches().set(i, pitch);
                else action.getSplinePitches().add(pitch);
            } catch (Exception ignored) {}
        }
        dirty = true;
        if (pathVisible) updatePathPreview();
        autoSave();
    }

    private void save() {
        if (selectedId == null) return;
        var data = cutscenes.get(selectedId);
        data.setName(nameBox.getValue());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(data)));
        dirty = false;
    }

    private void autoSave() {
        if (selectedId == null) return;
        var data = cutscenes.get(selectedId);
        data.setName(nameBox.getValue());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(data)));
        dirty = false;
    }

    private int getTotalDuration() {
        if (selectedId == null) return 0;
        int total = 0;
        for (var action : cutscenes.get(selectedId).getActions()) total += action.getDuration();
        return total;
    }

    private int getActionStartTick(int index) {
        if (selectedId == null) return 0;
        int tick = 0;
        for (int i = 0; i < index && i < cutscenes.get(selectedId).getActions().size(); i++) {
            tick += cutscenes.get(selectedId).getActions().get(i).getDuration();
        }
        return tick;
    }

    private int screenXToTick(int screenX) {
        int tx = showCutscenes ? cutscenePanelW : 0;
        return (int) ((screenX - tx - 4 + timelineOffset) / (TICK_W * timelineScale));
    }

    private int tickToScreenX(int tick) {
        int tx = showCutscenes ? cutscenePanelW : 0;
        return (int) (tx + 4 + tick * TICK_W * timelineScale - timelineOffset);
    }

    @Override
    public void tick() {
        updatePlayback();
        super.tick();
    }

    @Override
    public void onClose() {
        stopPlayback();
        CutscenePathRenderer.clearPreview();
        CutscenePathRenderer.clearAllPaths();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePlayback();

        graphics.fillGradient(0, 0, width, height, Theme.alpha(Theme.BG_PANEL, 0.07f), Theme.alpha(Theme.BG_PANEL, 0.2f));

        UI.inner(graphics, 0, 0, width, TOPBAR_H);
        graphics.renderOutline(0, TOPBAR_H - 1, width, 1, Theme.BORDER);


        int bottomY = height - (showTimeline ? timelineH : 0);
        int cutX = showCutscenes ? cutscenePanelW : 0;
        int propX = showProperties ? width - propsPanelW : width;

        if (showCutscenes) {
            int panelBottom = height;
            UI.inner(graphics, 0, TOPBAR_H, cutscenePanelW, panelBottom - TOPBAR_H);
            graphics.renderOutline(cutscenePanelW - 1, TOPBAR_H, 1, panelBottom - TOPBAR_H, Theme.BORDER);
            graphics.renderOutline(cutscenePanelW - 4, panelBottom / 2 - 10, 3, 20, Theme.BORDER);

            UI.title(graphics, font, t("iscript.cutscene.editor.cutscene").getString(), 6, TOPBAR_H + 6);

            List<String> ids = new ArrayList<>();
            String filter = searchBox.getValue().toLowerCase();
            for (var e : cutscenes.entrySet()) {
                if (e.getKey().toLowerCase().contains(filter) || e.getValue().getName().toLowerCase().contains(filter)) ids.add(e.getKey());
            }

            int listY = TOPBAR_H + 38;
            int listH = panelBottom - listY - 28;
            int visible = Math.max(0, listH / 18);

            for (int i = listScroll; i < Math.min(listScroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                var cutscene = cutscenes.get(id);
                int rowY = listY + (i - listScroll) * 18;
                boolean hovered = mouseX >= 4 && mouseX <= cutscenePanelW - 4 && mouseY >= rowY && mouseY <= rowY + 16;
                boolean selected = id.equals(selectedId);
                UI.row(graphics, 4, rowY, cutscenePanelW - 8, 16, selected, hovered);
                String label = cutscene.getName();
                if (cutscene.isLoop()) label += " \u27F3";
                graphics.drawString(font, label, 8, rowY + 4, selected ? Theme.TEXT : Theme.TEXT_DIM);
            }
            if (ids.isEmpty()) UI.centerLabel(graphics, font, t("iscript.cutscene.editor.no_cutscenes").getString(), 0, listY + listH / 2, cutscenePanelW);
        }

        if (showTimeline) {
            int tx = cutX;
            int tw = propX - cutX;
            int ty = height - timelineH;

            UI.inner(graphics, tx, ty, tw, timelineH);
            graphics.renderOutline(tx + tw / 2 - 10, height - 4, 20, 3, Theme.BORDER);

            int headerH = 18;
            UI.inner(graphics, tx, ty, tw, headerH);
            UI.title(graphics, font, t("iscript.cutscene.editor.timeline_title").getString(), tx + 6, ty + 5);

            int totalTicks = getTotalDuration();
            int maxTick = Math.max(totalTicks, 100);
            int tickStep = Math.max(1, (int) (20 / timelineScale));

            for (int t = 0; t <= maxTick; t += tickStep) {
                int sx = tickToScreenX(t);
                if (sx < tx + 4 || sx > propX - 4) continue;
                graphics.fill(sx, ty + headerH, sx + 1, height - 4, Theme.BORDER);
                if (t % (tickStep * 5) == 0) {
                    String label = String.valueOf(t);
                    int lw = font.width(label);
                    graphics.drawString(font, label, sx - lw / 2, ty + headerH + 2, Theme.TEXT_MUTE);
                }
            }

            if (selectedId != null) {
                var data = cutscenes.get(selectedId);
                int trackY = ty + headerH + 16;
                int trackH = timelineH - headerH - 24;

                UI.inner(graphics, tx + 4, trackY, tw - 8, trackH);

                for (int i = 0; i < data.getActions().size(); i++) {
                    var action = data.getActions().get(i);
                    int startTick = getActionStartTick(i);
                    int endTick = startTick + action.getDuration();
                    int sx1 = tickToScreenX(startTick);
                    int sx2 = tickToScreenX(endTick);
                    sx1 = Math.max(tx + 4, sx1);
                    sx2 = Math.min(propX - 4, sx2);
                    if (sx2 <= sx1) continue;

                    boolean selected = i == selectedActionIndex;
                    int color = switch (action.getType()) {
                        case DELAY -> Theme.BG_INNER;
                        case CAMERA_PATH, CAMERA_IDLE, CAMERA_LOOK, CAMERA_FOLLOW, CAMERA_ORBIT, CAMERA_DOLLY, CAMERA_SHAKE -> Theme.ACCENT;
                        case NPC_MOVE -> Theme.TEXT_DIM;
                        case DIALOG -> Theme.TEXT;
                        case SOUND -> Theme.ACCENT;
                        case BLOCK -> Theme.TEXT_DIM;
                        case SCRIPT_RUN -> Theme.TEXT_DIM;
                        case NPC_ANIMATION -> Theme.ACCENT;
                        default -> Theme.TEXT_MUTE;
                    };

                    if (selected) {
                        color = Theme.ACCENT;
                    }

                    graphics.fill(sx1 + 1, trackY + 1, sx2 - 1, trackY + trackH - 1, color);

                    int borderColor = selected ? Theme.ACCENT : Theme.BORDER;
                    graphics.renderOutline(sx1, trackY, sx2 - sx1, trackH, borderColor);

                    if (sx2 - sx1 > 20) {
                        String label = action.getType().name();
                        if (label.length() > 3) label = label.substring(0, 3);
                        graphics.drawString(font, label, sx1 + 3, trackY + 3, Theme.TEXT);
                        if (sx2 - sx1 > 40) {
                            String dur = action.getDuration() + "t";
                            graphics.drawString(font, dur, sx1 + 3, trackY + 13, Theme.TEXT_DIM);
                        }
                        if (sx2 - sx1 > 60 && action.isCameraAction()) {
                            String pos = String.format("%.0f,%.0f", action.getX(), action.getZ());
                            graphics.drawString(font, pos, sx1 + 3, trackY + 23, Theme.TEXT_DIM);
                        }
                    }
                }

                if (draggingAction && dragActionIndex >= 0) {
                    int ghostX = mouseX - dragOffsetX;
                    int ghostY = trackY;
                    int ghostW = dragActionWidth;
                    int ghostH = trackH;
                    int trackXStart = tx + 4;
                    int trackXEnd = propX - 4;
                    if (ghostX < trackXStart) ghostX = trackXStart;
                    if (ghostX + ghostW > trackXEnd) ghostX = trackXEnd - ghostW;
                    graphics.fill(ghostX, ghostY, ghostX + ghostW, ghostY + ghostH, Theme.alpha(Theme.ACCENT, 0.53f));
                    graphics.renderOutline(ghostX, ghostY, ghostW, ghostH, Theme.ACCENT);
                }
            }

            int phx = tickToScreenX(playheadTick);
            if (phx >= tx + 4 && phx <= propX - 4) {
                graphics.fill(phx - 1, ty + headerH, phx + 1, height - 4, Theme.ERROR);
                graphics.fill(phx - 4, ty + headerH - 3, phx + 4, ty + headerH, Theme.ERROR);
                graphics.fill(phx - 4, height - 4, phx + 4, height - 4 + 3, Theme.ERROR);
            }
        }

        if (showProperties) {
            UI.inner(graphics, propX, TOPBAR_H, propsPanelW, height - TOPBAR_H);
            graphics.renderOutline(propX, TOPBAR_H, 1, height - TOPBAR_H, Theme.BORDER);
            graphics.renderOutline(propX + 1, height / 2 - 10, 3, 20, Theme.BORDER);

            if (selectedId != null && selectedActionIndex >= 0) {
                UI.title(graphics, font, t("iscript.cutscene.editor.properties_title").getString(), propX + 6, TOPBAR_H + 6);
                String[] labels = {
                        t("iscript.cutscene.editor.x").getString(),
                        t("iscript.cutscene.editor.y").getString(),
                        t("iscript.cutscene.editor.z").getString(),
                        t("iscript.cutscene.editor.yaw").getString(),
                        t("iscript.cutscene.editor.pitch").getString(),
                        t("iscript.cutscene.editor.roll").getString(),
                        t("iscript.cutscene.editor.duration").getString(),
                        t("iscript.cutscene.editor.value").getString()
                };
                EditBox[] boxes = {xBox, yBox, zBox, yawBox, pitchBox, rollBox, durationBox, stringBox};
                for (int i = 0; i < labels.length; i++) {
                    if (boxes[i].visible) {
                        UI.label(graphics, font, labels[i], propX + 6, boxes[i].getY() + 2);
                    }
                }

                if (shakeTraumaBox.visible) {
                    UI.label(graphics, font, t("iscript.cutscene.editor.shake_trauma").getString(), propX + 6, shakeTraumaBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.shake_decay").getString(), propX + 6, shakeDecayBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.shake_angle").getString(), propX + 6, shakeAngleBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.shake_offset").getString(), propX + 6, shakeOffsetBox.getY() + 2);
                }
                if (lookAtXBox.visible) {
                    UI.label(graphics, font, t("iscript.cutscene.editor.lookat_x").getString(), propX + 6, lookAtXBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.lookat_y").getString(), propX + 6, lookAtYBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.lookat_z").getString(), propX + 6, lookAtZBox.getY() + 2);
                }
                if (orbitRadiusBox.visible) {
                    UI.label(graphics, font, t("iscript.cutscene.editor.orbit_radius").getString(), propX + 6, orbitRadiusBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.orbit_height").getString(), propX + 6, orbitHeightBox.getY() + 2);
                    UI.label(graphics, font, t("iscript.cutscene.editor.orbit_speed").getString(), propX + 6, orbitSpeedBox.getY() + 2);
                }
                if (dollyDistanceBox.visible) {
                    UI.label(graphics, font, t("iscript.cutscene.editor.dolly_distance").getString(), propX + 6, dollyDistanceBox.getY() + 2);
                }
                if (speedBox.visible) {
                    UI.label(graphics, font, t("iscript.cutscene.editor.speed").getString(), propX + 6, speedBox.getY() + 2);
                }
                if (scriptIdBox.visible) {
                    UI.label(graphics, font, "Script ID", propX + 6, scriptIdBox.getY() + 2);
                }
                if (funcBox.visible) {
                    UI.label(graphics, font, "Function", propX + 6, funcBox.getY() + 2);
                }

                if (splineToggleBtn.visible) {
                    int spY = addSplinePointBtn.getY() + 18;
                    if (pathTypeBtn.visible) spY += 18;
                    if (speedBox.visible) spY += 18;
                    UI.title(graphics, font, t("iscript.cutscene.editor.spline_points").getString(), propX + 6, spY);
                }
            } else if (selectedId != null) {
                var data = cutscenes.get(selectedId);
                UI.label(graphics, font, t("iscript.cutscene.editor.actions_count", data.getActions().size()).getString(), propX + 6, TOPBAR_H + 24);
                UI.label(graphics, font, t("iscript.cutscene.editor.duration", getTotalDuration()).getString(), propX + 6, TOPBAR_H + 38);
                UI.label(graphics, font, t("iscript.cutscene.edit.loop", data.isLoop()).getString(), propX + 6, TOPBAR_H + 52);
                graphics.drawString(font, t("iscript.cutscene.editor.select_action").getString(), propX + 6, TOPBAR_H + 76, Theme.TEXT_MUTE);
            } else {
                graphics.drawString(font, t("iscript.cutscene.editor.select_cutscene").getString(), propX + 6, TOPBAR_H + 20, Theme.TEXT_MUTE);
            }

        }

        for (var renderable : renderables) {
            if (renderable instanceof EditBox eb && !eb.visible) continue;
            if (renderable instanceof Button b && !b.visible) continue;
            if (renderable instanceof Btn b && !b.visible) continue;
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        if (showProperties && propsMaxScroll > 0) {
            int sbX = propX + propsPanelW - 8;
            int sbY = TOPBAR_H + 2;
            int sbH = height - TOPBAR_H - 4;
            int trackH = sbH;
            graphics.fill(sbX, sbY, sbX + 6, sbY + trackH, Theme.BG_INNER);
            graphics.renderOutline(sbX, sbY, 6, trackH, Theme.BORDER);
            int visibleH = height - TOPBAR_H;
            int thumbH = Math.max(10, (visibleH * trackH) / (propsMaxScroll + visibleH));
            int trackAvail = trackH - thumbH;
            int thumbY = sbY + (propScroll * trackAvail) / propsMaxScroll;
            graphics.fill(sbX, thumbY, sbX + 6, thumbY + thumbH, Theme.TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        for (var child : children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) setFocused(w);
                return true;
            }
        }

        int bottomY = height - (showTimeline ? timelineH : 0);
        int cutX = showCutscenes ? cutscenePanelW : 0;
        int propX = showProperties ? width - propsPanelW : width;

        if (showCutscenes && mouseX >= cutscenePanelW - 6 && mouseX <= cutscenePanelW + 3 && mouseY > TOPBAR_H && mouseY < bottomY) {
            resizingLeft = true;
            return true;
        }
        if (showProperties && mouseX >= propX - 3 && mouseX <= propX + 6 && mouseY > TOPBAR_H && mouseY < bottomY) {
            resizingRight = true;
            return true;
        }
        if (showTimeline && mouseY >= height - timelineH - 3 && mouseY <= height - timelineH + 6 && mouseX > cutX && mouseX < propX) {
            resizingTimeline = true;
            return true;
        }

        if (showCutscenes && mouseX >= 0 && mouseX <= cutscenePanelW && mouseY > TOPBAR_H && mouseY < bottomY) {
            List<String> ids = new ArrayList<>();
            String filter = searchBox.getValue().toLowerCase();
            for (var e : cutscenes.entrySet()) {
                if (e.getKey().toLowerCase().contains(filter) || e.getValue().getName().toLowerCase().contains(filter)) ids.add(e.getKey());
            }
            int listY = TOPBAR_H + 38;
            int listH = bottomY - listY - 28;
            int visible = Math.max(0, listH / 18);
            for (int i = listScroll; i < Math.min(listScroll + visible, ids.size()); i++) {
                int rowY = listY + (i - listScroll) * 18;
                if (mouseY >= rowY && mouseY <= rowY + 16) {
                    selectedId = ids.get(i);
                    selectedActionIndex = -1;
                    updateSelection();
                    return true;
                }
            }
            return true;
        }

        if (showTimeline && mouseY >= height - timelineH && mouseY <= height) {
            int tx = cutX;
            int tw = propX - cutX;
            int ty = height - timelineH;
            int headerH = 18;
            int trackY = ty + headerH + 16;
            int trackH = timelineH - headerH - 24;

            int phx = tickToScreenX(playheadTick);

            if (Math.abs(mouseX - phx) <= 8 && mouseY >= ty + headerH && mouseY <= height - 4) {
                if (isPlaying) pausePlayback();
                draggingPlayhead = true;
                return true;
            }

            if (mouseY >= trackY && mouseY <= trackY + trackH && selectedId != null) {
                var data = cutscenes.get(selectedId);

                for (int i = 0; i < data.getActions().size(); i++) {
                    int startTick = getActionStartTick(i);
                    int endTick = startTick + data.getActions().get(i).getDuration();
                    int sx1 = tickToScreenX(startTick);
                    int sx2 = tickToScreenX(endTick);
                    sx1 = Math.max(tx + 4, sx1);
                    sx2 = Math.min(propX - 4, sx2);
                    if (sx2 <= sx1) continue;

                    if (mouseX >= sx1 && mouseX <= sx2) {
                        selectedActionIndex = i;
                        updateActionFields();
                        draggingAction = true;
                        dragActionIndex = i;
                        dragStartX = (int) mouseX;
                        dragOffsetX = (int) mouseX - sx1;
                        dragActionWidth = sx2 - sx1;
                        return true;
                    }
                }

                if (isPlaying) {
                    pausePlayback();
                }
                playheadTick = Math.max(0, screenXToTick((int) mouseX));
                int total = getTotalDuration();
                if (playheadTick > total) playheadTick = total;
                draggingPlayhead = true;
                return true;
            }

            if (mouseY >= height - timelineH && mouseY <= height - timelineH + headerH) {
                if (isPlaying) {
                    pausePlayback();
                }
                playheadTick = Math.max(0, screenXToTick((int) mouseX));
                int total = getTotalDuration();
                if (playheadTick > total) playheadTick = total;
                draggingPlayhead = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (resizingLeft) {
            cutscenePanelW = (int) Math.max(MIN_PANEL_W, Math.min(MAX_PANEL_W, mouseX));
            layout();
            return true;
        }
        if (resizingRight) {
            propsPanelW = (int) Math.max(MIN_PANEL_W, Math.min(MAX_PANEL_W, width - mouseX));
            layout();
            return true;
        }
        if (resizingTimeline) {
            timelineH = (int) Math.max(MIN_TIMELINE_H, Math.min(MAX_TIMELINE_H, height - mouseY));
            layout();
            return true;
        }
        if (draggingPlayhead) {
            playheadTick = screenXToTick((int) mouseX);
            playheadTick = Math.max(0, playheadTick);
            int total = getTotalDuration();
            if (playheadTick > total) playheadTick = total;
            return true;
        }
        if (draggingAction && selectedId != null) {
            int targetIndex = getActionIndexAtScreenX((int) mouseX);
            if (targetIndex < 0) {
                int tick = screenXToTick((int) mouseX);
                targetIndex = getActionIndexAtTick(tick);
            }
            if (targetIndex >= 0) {
                dropTargetIndex = targetIndex;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        resizingLeft = false;
        resizingRight = false;
        resizingTimeline = false;
        draggingPlayhead = false;
        if (draggingAction && dragActionIndex >= 0 && dropTargetIndex >= 0 && dropTargetIndex != dragActionIndex) {
            moveActionTo(dragActionIndex, dropTargetIndex);
        }
        draggingAction = false;
        dragActionIndex = -1;
        dropTargetIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int bottomY = height - (showTimeline ? timelineH : 0);
        if (showCutscenes && mouseX < cutscenePanelW && mouseY > TOPBAR_H && mouseY < bottomY) {
            int listY = TOPBAR_H + 38;
            int listH = bottomY - listY - 28;
            int visible = listH / 18;
            List<String> ids = new ArrayList<>();
            String filter = searchBox.getValue().toLowerCase();
            for (var e : cutscenes.entrySet()) {
                if (e.getKey().toLowerCase().contains(filter) || e.getValue().getName().toLowerCase().contains(filter)) ids.add(e.getKey());
            }
            int maxScroll = Math.max(0, ids.size() - visible);
            if (delta > 0) listScroll = Math.max(0, listScroll - 1);
            else listScroll = Math.min(listScroll + 1, maxScroll);
        } else if (showTimeline && mouseY > height - timelineH && mouseX > cutscenePanelW && mouseX < (showProperties ? width - propsPanelW : width)) {
            int centerTick = screenXToTick((int) mouseX);
            if (delta > 0) setTimelineScale(timelineScale * 1.2f);
            else setTimelineScale(timelineScale * 0.8f);
            int tx = showCutscenes ? cutscenePanelW : 0;
            timelineOffset = (int) (centerTick * TICK_W * timelineScale - (mouseX - tx - 4));
            timelineOffset = Math.max(0, timelineOffset);
        } else if (showProperties && mouseX > width - propsPanelW) {
            if (selectedId != null && selectedActionIndex >= 0) {
                var action = cutscenes.get(selectedId).getActions().get(selectedActionIndex);
                if (action.getType() == CutsceneActionType.CAMERA_PATH && action.getSplinePoints().size() > 5) {
                    int maxScroll = Math.max(0, action.getSplinePoints().size() - 5);
                    if (delta > 0) splineScroll = Math.max(0, splineScroll - 1);
                    else splineScroll = Math.min(splineScroll + 1, maxScroll);
                    layout();
                }
                if (delta > 0) propScroll = Math.max(0, propScroll - 10);
                else propScroll = Math.min(propScroll + 10, propsMaxScroll);
                layout();
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 && getFocused() instanceof EditBox eb) {
            if (eb.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == 256) { onClose(); return true; }
        if (keyCode == 32) { if (isPlaying) { pausePlayback(); } else { startPlayback(); } return true; }
        if (keyCode == 257 && modifiers == 0) { if (selectedId != null) { addAction(); return true; } }
        if (keyCode == 259 && modifiers == 0) { if (selectedId != null) { removeAction(); return true; } }
        if (keyCode == 67 && (modifiers & 2) != 0) { togglePanel("cutscenes"); return true; }
        if (keyCode == 84 && (modifiers & 2) != 0) { togglePanel("timeline"); return true; }
        if (keyCode == 80 && (modifiers & 2) != 0) { togglePanel("properties"); return true; }

        if (keyCode == 68 && (modifiers & 2) != 0) { duplicateAction(); return true; }
        if (keyCode == 67 && (modifiers & 1) != 0) { copyAction(); return true; }
        if (keyCode == 86 && (modifiers & 1) != 0) { pasteAction(); return true; }
        if (keyCode == 265) { selectActionBy(-1); return true; }
        if (keyCode == 264) { selectActionBy(1); return true; }
        if (keyCode == 263) { if (isPlaying) { playheadTick = Math.max(0, playheadTick - 1); } else { selectActionBy(-1); } return true; }
        if (keyCode == 262) { if (isPlaying) { playheadTick++; } else { selectActionBy(1); } return true; }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (var child : children()) {
            if (child instanceof EditBox eb && eb.isFocused() && eb.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class Btn extends net.minecraft.client.gui.components.AbstractWidget {
        private final String icon;
        private int color;
        private final Component tooltip;
        private final Runnable onClick;

        Btn(int x, int y, int w, int h, String icon, int color, Component tooltip, Runnable onClick) {
            super(x, y, w, h, Component.empty());
            this.icon = icon;
            this.color = color;
            this.tooltip = tooltip;
            this.onClick = onClick;
        }

        void setColor(int c) { this.color = c; }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            int bg = hovered ? Theme.BG_HOVER : 0x00000000;
            if (bg != 0) graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            if (hovered) graphics.renderOutline(getX(), getY(), width, height, Theme.ACCENT);
            int textColor = active ? (hovered ? Theme.TEXT : color) : Theme.TEXT_MUTE;
            graphics.drawCenteredString(Minecraft.getInstance().font, icon, getX() + width / 2, getY() + (height - 8) / 2, textColor);
            if (hovered && active) graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (active && isMouseOver(mouseX, mouseY)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    }
}