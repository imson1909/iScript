package com.iscript.iscript.gui.screen;

import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.CutscenePathRenderer;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.PathType;
import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.cutscene.CutsceneAction;
import com.iscript.iscript.data.cutscene.CutsceneActionType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.PlayCutscenePacket;
import com.iscript.iscript.network.packet.SaveCutscenePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class CutsceneEditSubScreen extends DashboardScreen.SubScreen {
    private final CutsceneData cutscene;
    private EditBox nameBox;
    private int actionScroll = 0;
    private int selectedActionIndex = -1;
    private static final int ACTION_HEIGHT = 22;

    private EditBox xBox, yBox, zBox, yawBox, pitchBox, rollBox, durationBox, stringBox;
    private EditBox fovBox, lookAtXBox, lookAtYBox, lookAtZBox;
    private EditBox orbitRadiusBox, orbitHeightBox, orbitSpeedBox;
    private EditBox shakeTraumaBox, shakeDecayBox, shakeAngleBox, shakeOffsetBox;
    private EditBox speedBox;
    private Button typeBtn, saveBtn, backBtn, addBtn, removeBtn, duplicateBtn, usePosBtn, previewBtn, loopBtn, showPathBtn;
    private Button interpBtn, pathTypeBtn, useFovBtn, constantSpeedBtn;
    private boolean dirty = false;
    private Interpolation[] interps = Interpolation.values();
    private int interpIndex = 0;
    private PathType[] pathTypes = PathType.values();
    private int pathTypeIndex = 0;
    private boolean pathVisible = false;

    public CutsceneEditSubScreen(DashboardScreen parent, ServerLevel level, CutsceneData cutscene) {
        super(parent, level);
        this.cutscene = cutscene;
    }

    @Override
    public void init() {
        this.clearWidgets();
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - x;
        int h = this.parent.height - y;

        nameBox = new EditBox(this.font, x + 8, y + 8, 200, 18, Component.literal("Name"));
        nameBox.setValue(cutscene.getName());
        nameBox.setResponder(s -> dirty = true);
        this.addRenderableWidget(nameBox);

        loopBtn = Button.builder(Component.literal("Loop: " + cutscene.isLoop()), b -> toggleLoop()).pos(x + 220, y + 8).size(60, 18).build();
        this.addRenderableWidget(loopBtn);

        backBtn = Button.builder(Component.literal("Back"), b -> goBack()).pos(x + w - 110, y + 8).size(50, 18).build();
        saveBtn = Button.builder(Component.literal("Save"), b -> save()).pos(x + w - 56, y + 8).size(50, 18).build();
        previewBtn = Button.builder(Component.literal("Preview"), b -> playPreview()).pos(x + w - 170, y + 8).size(50, 18).build();
        showPathBtn = Button.builder(Component.literal("Show Path"), b -> togglePath()).pos(x + w - 240, y + 8).size(60, 18).build();

        addBtn = Button.builder(Component.literal("+ Add"), b -> addAction()).pos(x + 8, y + h - 28).size(50, 20).build();
        removeBtn = Button.builder(Component.literal("- Remove"), b -> removeAction()).pos(x + 62, y + h - 28).size(60, 20).build();
        duplicateBtn = Button.builder(Component.literal("Duplicate"), b -> duplicateAction()).pos(x + 126, y + h - 28).size(60, 20).build();

        this.addRenderableWidget(backBtn);
        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(previewBtn);
        this.addRenderableWidget(showPathBtn);
        this.addRenderableWidget(addBtn);
        this.addRenderableWidget(removeBtn);
        this.addRenderableWidget(duplicateBtn);

        int fx = x + w / 2 + 4;
        int fy = y + 32;

        typeBtn = Button.builder(Component.literal("Type: DELAY"), b -> cycleType()).pos(fx, fy).size(120, 18).build();
        this.addRenderableWidget(typeBtn);

        fy += 24;
        xBox = makeField(fx, fy, 60); fy += 22;
        yBox = makeField(fx, fy, 60); fy += 22;
        zBox = makeField(fx, fy, 60); fy += 22;
        yawBox = makeField(fx, fy, 60); fy += 22;
        pitchBox = makeField(fx, fy, 60); fy += 22;
        rollBox = makeField(fx, fy, 60); fy += 22;
        durationBox = makeField(fx, fy, 60); fy += 22;
        fovBox = makeField(fx, fy, 60); fy += 22;
        speedBox = makeField(fx, fy, 60); fy += 22;

        usePosBtn = Button.builder(Component.literal("Use My Pos"), b -> usePlayerPos()).pos(fx + 70, y + 32 + 24).size(70, 18).build();
        this.addRenderableWidget(usePosBtn);

        interpBtn = Button.builder(Component.literal("Interp: LINEAR"), b -> cycleInterp()).pos(fx + 70, y + 32 + 24 + 22).size(100, 18).build();
        this.addRenderableWidget(interpBtn);

        pathTypeBtn = Button.builder(Component.literal("Path: CATMULL_ROM"), b -> cyclePathType()).pos(fx + 70, y + 32 + 24 + 44).size(120, 18).build();
        this.addRenderableWidget(pathTypeBtn);

        useFovBtn = Button.builder(Component.literal("FOV: Off"), b -> toggleUseFov()).pos(fx + 70, y + 32 + 24 + 66).size(70, 18).build();
        this.addRenderableWidget(useFovBtn);

        constantSpeedBtn = Button.builder(Component.literal("ConstSpd: Off"), b -> toggleConstantSpeed()).pos(fx + 70, y + 32 + 24 + 88).size(90, 18).build();
        this.addRenderableWidget(constantSpeedBtn);

        fy += 8;
        lookAtXBox = makeField(fx, fy, 60); fy += 22;
        lookAtYBox = makeField(fx, fy, 60); fy += 22;
        lookAtZBox = makeField(fx, fy, 60); fy += 22;
        orbitRadiusBox = makeField(fx, fy, 60); fy += 22;
        orbitHeightBox = makeField(fx, fy, 60); fy += 22;
        orbitSpeedBox = makeField(fx, fy, 60); fy += 22;
        shakeTraumaBox = makeField(fx, fy, 60); fy += 22;
        shakeDecayBox = makeField(fx, fy, 60); fy += 22;
        shakeAngleBox = makeField(fx, fy, 60); fy += 22;
        shakeOffsetBox = makeField(fx, fy, 60); fy += 22;

        stringBox = makeField(fx, fy, w / 2 - 66); fy += 22;

        updateActionFields();
        updatePathPreview();
    }

    private void playPreview() {
        if (cutscene != null) {
            IScriptNetwork.sendToServer(new PlayCutscenePacket(cutscene.getId(), 1.0f));
        }
    }

    private EditBox makeField(int x, int y, int width) {
        EditBox box = new EditBox(this.font, x + 50, y, width, 16, Component.empty());
        box.setMaxLength(128);
        box.setResponder(s -> { if (selectedActionIndex >= 0) updateActionFromFields(); });
        this.addRenderableWidget(box);
        return box;
    }

    private void togglePath() {
        pathVisible = !pathVisible;
        showPathBtn.setMessage(Component.literal(pathVisible ? "Hide Path" : "Show Path"));
        updatePathPreview();
    }

    private void updatePathPreview() {
        if (!pathVisible || minecraft == null || minecraft.player == null) {
            CutscenePathRenderer.clearPreview();
            return;
        }

        CutscenePath path = buildPathFromCameraActions();
        CutsceneCameraMode mode = buildModeFromCameraActions();
        if (path != null && !path.keyframes.isEmpty()) {
            CutscenePathRenderer.setPreviewPath(path, mode);
        } else {
            CutscenePathRenderer.clearPreview();
        }
    }

    private CutscenePath buildPathFromCameraActions() {
        CutscenePath path = new CutscenePath();
        path.easing = Interpolation.LINEAR;

        for (CutsceneAction action : cutscene.getActions()) {
            if (action.getType() != CutsceneActionType.CAMERA_PATH &&
                    action.getType() != CutsceneActionType.CAMERA_IDLE &&
                    action.getType() != CutsceneActionType.CAMERA_ORBIT &&
                    action.getType() != CutsceneActionType.CAMERA_DOLLY) continue;

            try {
                path.easing = Interpolation.valueOf(action.getInterpolation());
            } catch (Exception e) {
                path.easing = Interpolation.LINEAR;
            }

            PathType segType;
            try {
                segType = PathType.valueOf(action.getPathType());
            } catch (Exception e) {
                segType = PathType.CATMULL_ROM;
            }

            if (!action.getSplinePoints().isEmpty()) {
                for (int i = 0; i < action.getSplinePoints().size(); i++) {
                    Vec3 p = action.getSplinePoints().get(i);
                    float yaw = i < action.getSplineYaws().size() ? action.getSplineYaws().get(i) : action.getYaw();
                    float pitch = i < action.getSplinePitches().size() ? action.getSplinePitches().get(i) : action.getPitch();
                    path.keyframes.add(new CutscenePath.Keyframe(p, yaw, pitch, action.getRoll(), action.getFov(), 0, segType));
                }
            } else {
                path.keyframes.add(new CutscenePath.Keyframe(
                        new Vec3(action.getX(), action.getY(), action.getZ()),
                        action.getYaw(), action.getPitch(), action.getRoll(), action.getFov(), 0, segType));
            }
        }

        if (path.keyframes.size() < 2) return null;
        return path;
    }

    private CutsceneCameraMode buildModeFromCameraActions() {
        CutsceneCameraMode mode = new CutsceneCameraMode();
        for (CutsceneAction action : cutscene.getActions()) {
            if (action.getType() == CutsceneActionType.CAMERA_LOOK) {
                mode.useLookAt = true;
                mode.lookAtTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
            }
            if (action.getType() == CutsceneActionType.CAMERA_ORBIT) {
                mode.orbitMode = true;
                mode.orbitCenter = new Vec3(action.getX(), action.getY(), action.getZ());
                mode.orbitRadius = action.getOrbitRadius();
                mode.orbitHeight = action.getOrbitHeight();
                mode.orbitSpeed = action.getOrbitSpeed();
            }
            if (action.getType() == CutsceneActionType.CAMERA_DOLLY) {
                mode.useDollyZoom = true;
                mode.dollyTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                mode.dollyBaseFov = action.getDollyBaseFov();
                mode.dollyBaseDistance = action.getDollyTargetDistance();
            }
        }
        return mode;
    }

    private void toggleLoop() {
        cutscene.setLoop(!cutscene.isLoop());
        loopBtn.setMessage(Component.literal("Loop: " + cutscene.isLoop()));
        dirty = true;
    }

    private void toggleUseFov() {
        if (selectedActionIndex < 0) return;
        var action = cutscene.getActions().get(selectedActionIndex);
        action.setUseFov(!action.isUseFov());
        useFovBtn.setMessage(Component.literal("FOV: " + (action.isUseFov() ? "On" : "Off")));
        dirty = true;
    }

    private void toggleConstantSpeed() {
        if (selectedActionIndex < 0) return;
        var action = cutscene.getActions().get(selectedActionIndex);
        action.setConstantSpeed(!action.isConstantSpeed());
        constantSpeedBtn.setMessage(Component.literal("ConstSpd: " + (action.isConstantSpeed() ? "On" : "Off")));
        dirty = true;
    }

    private void usePlayerPos() {
        if (minecraft == null || minecraft.player == null) return;
        var player = minecraft.player;
        xBox.setValue(String.format("%.2f", player.getX()));
        yBox.setValue(String.format("%.2f", player.getY() + player.getEyeHeight()));
        zBox.setValue(String.format("%.2f", player.getZ()));
        yawBox.setValue(String.format("%.1f", player.getYRot()));
        pitchBox.setValue(String.format("%.1f", player.getXRot()));
        updateActionFromFields();
        if (pathVisible) updatePathPreview();
    }

    private void cycleType() {
        if (selectedActionIndex < 0) return;
        var action = cutscene.getActions().get(selectedActionIndex);
        CutsceneActionType[] types = CutsceneActionType.values();
        int next = (action.getType().ordinal() + 1) % types.length;
        action.setType(types[next]);
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void cycleInterp() {
        if (selectedActionIndex < 0) return;
        interpIndex = (interpIndex + 1) % interps.length;
        cutscene.getActions().get(selectedActionIndex).setInterpolation(interps[interpIndex].name());
        interpBtn.setMessage(Component.literal("Interp: " + interps[interpIndex].name()));
        dirty = true;
        if (pathVisible) updatePathPreview();
    }

    private void cyclePathType() {
        if (selectedActionIndex < 0) return;
        pathTypeIndex = (pathTypeIndex + 1) % pathTypes.length;
        cutscene.getActions().get(selectedActionIndex).setPathType(pathTypes[pathTypeIndex].name());
        pathTypeBtn.setMessage(Component.literal("Path: " + pathTypes[pathTypeIndex].name()));
        dirty = true;
        if (pathVisible) updatePathPreview();
    }

    private void addAction() {
        CutsceneAction action = new CutsceneAction();
        action.setType(CutsceneActionType.DELAY);
        action.setDuration(20);
        if (selectedActionIndex >= 0 && selectedActionIndex < cutscene.getActions().size() - 1) {
            cutscene.getActions().add(selectedActionIndex + 1, action);
            selectedActionIndex++;
        } else {
            cutscene.getActions().add(action);
            selectedActionIndex = cutscene.getActions().size() - 1;
        }
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void removeAction() {
        if (selectedActionIndex < 0 || selectedActionIndex >= cutscene.getActions().size()) return;
        cutscene.getActions().remove(selectedActionIndex);
        if (selectedActionIndex >= cutscene.getActions().size()) selectedActionIndex = cutscene.getActions().size() - 1;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void duplicateAction() {
        if (selectedActionIndex < 0 || selectedActionIndex >= cutscene.getActions().size()) return;
        var original = cutscene.getActions().get(selectedActionIndex);
        CutsceneAction copy = new CutsceneAction();
        CompoundTag tag = new CompoundTag();
        original.save(tag);
        copy.load(tag);
        cutscene.getActions().add(selectedActionIndex + 1, copy);
        selectedActionIndex++;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void updateActionFields() {
        if (selectedActionIndex >= 0 && selectedActionIndex < cutscene.getActions().size()) {
            var action = cutscene.getActions().get(selectedActionIndex);
            typeBtn.setMessage(Component.literal("Type: " + action.getType().name()));
            xBox.setValue(String.valueOf(action.getX()));
            yBox.setValue(String.valueOf(action.getY()));
            zBox.setValue(String.valueOf(action.getZ()));
            yawBox.setValue(String.valueOf(action.getYaw()));
            pitchBox.setValue(String.valueOf(action.getPitch()));
            rollBox.setValue(String.valueOf(action.getRoll()));
            durationBox.setValue(String.valueOf(action.getDuration()));
            fovBox.setValue(String.valueOf(action.getFov()));
            speedBox.setValue(String.valueOf(action.getSpeed()));
            stringBox.setValue(action.getStringValue());
            lookAtXBox.setValue(String.valueOf(action.getLookAtX()));
            lookAtYBox.setValue(String.valueOf(action.getLookAtY()));
            lookAtZBox.setValue(String.valueOf(action.getLookAtZ()));
            orbitRadiusBox.setValue(String.valueOf(action.getOrbitRadius()));
            orbitHeightBox.setValue(String.valueOf(action.getOrbitHeight()));
            orbitSpeedBox.setValue(String.valueOf(action.getOrbitSpeed()));
            shakeTraumaBox.setValue(String.valueOf(action.getShakeTrauma()));
            shakeDecayBox.setValue(String.valueOf(action.getShakeDecay()));
            shakeAngleBox.setValue(String.valueOf(action.getShakeMaxAngle()));
            shakeOffsetBox.setValue(String.valueOf(action.getShakeMaxOffset()));

            try {
                interpIndex = java.util.Arrays.asList(interps).indexOf(Interpolation.valueOf(action.getInterpolation()));
                if (interpIndex < 0) interpIndex = 0;
            } catch (Exception e) {
                interpIndex = 0;
            }
            interpBtn.setMessage(Component.literal("Interp: " + interps[interpIndex].name()));

            try {
                pathTypeIndex = java.util.Arrays.asList(pathTypes).indexOf(PathType.valueOf(action.getPathType()));
                if (pathTypeIndex < 0) pathTypeIndex = 0;
            } catch (Exception e) {
                pathTypeIndex = 0;
            }
            pathTypeBtn.setMessage(Component.literal("Path: " + pathTypes[pathTypeIndex].name()));
            useFovBtn.setMessage(Component.literal("FOV: " + (action.isUseFov() ? "On" : "Off")));
            constantSpeedBtn.setMessage(Component.literal("ConstSpd: " + (action.isConstantSpeed() ? "On" : "Off")));

            CutsceneActionType t = action.getType();
            boolean needsPos = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH ||
                    t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY ||
                    t == CutsceneActionType.CAMERA_FOLLOW || t == CutsceneActionType.NPC_MOVE ||
                    t == CutsceneActionType.BLOCK;
            boolean needsRot = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH ||
                    t == CutsceneActionType.CAMERA_FOLLOW;
            boolean needsDur = t == CutsceneActionType.DELAY || t == CutsceneActionType.CAMERA_IDLE ||
                    t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_ORBIT ||
                    t == CutsceneActionType.CAMERA_DOLLY || t == CutsceneActionType.CAMERA_FOLLOW ||
                    t == CutsceneActionType.NPC_MOVE;
            boolean needsStr = t == CutsceneActionType.DIALOG || t == CutsceneActionType.SOUND ||
                    t == CutsceneActionType.BLOCK || t == CutsceneActionType.SCRIPT ||
                    t == CutsceneActionType.NPC_ANIMATION;
            boolean needsLookAt = t == CutsceneActionType.CAMERA_LOOK || t == CutsceneActionType.CAMERA_DOLLY;
            boolean needsOrbit = t == CutsceneActionType.CAMERA_ORBIT;
            boolean needsShake = t == CutsceneActionType.CAMERA_SHAKE;
            boolean needsPath = t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_FOLLOW;

            xBox.active = needsPos;
            yBox.active = needsPos;
            zBox.active = needsPos;
            yawBox.active = needsRot;
            pitchBox.active = needsRot;
            rollBox.active = needsRot;
            durationBox.active = needsDur;
            stringBox.active = needsStr;
            usePosBtn.active = needsPos;
            interpBtn.active = needsPath;
            pathTypeBtn.active = needsPath || t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY;
            useFovBtn.active = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY;
            constantSpeedBtn.active = needsPath;
            fovBox.active = useFovBtn.active;
            speedBox.active = needsPath;

            lookAtXBox.active = needsLookAt;
            lookAtYBox.active = needsLookAt;
            lookAtZBox.active = needsLookAt;
            orbitRadiusBox.active = needsOrbit;
            orbitHeightBox.active = needsOrbit;
            orbitSpeedBox.active = needsOrbit;
            shakeTraumaBox.active = needsShake;
            shakeDecayBox.active = needsShake;
            shakeAngleBox.active = needsShake;
            shakeOffsetBox.active = needsShake;
        } else {
            typeBtn.setMessage(Component.literal("Type: -"));
            xBox.setValue(""); yBox.setValue(""); zBox.setValue("");
            yawBox.setValue(""); pitchBox.setValue(""); rollBox.setValue("");
            durationBox.setValue(""); fovBox.setValue(""); speedBox.setValue("");
            stringBox.setValue("");
            lookAtXBox.setValue(""); lookAtYBox.setValue(""); lookAtZBox.setValue("");
            orbitRadiusBox.setValue(""); orbitHeightBox.setValue(""); orbitSpeedBox.setValue("");
            shakeTraumaBox.setValue(""); shakeDecayBox.setValue(""); shakeAngleBox.setValue(""); shakeOffsetBox.setValue("");
            xBox.active = false; yBox.active = false; zBox.active = false;
            yawBox.active = false; pitchBox.active = false; rollBox.active = false;
            durationBox.active = false; stringBox.active = false;
            usePosBtn.active = false; interpBtn.active = false; pathTypeBtn.active = false;
            useFovBtn.active = false; constantSpeedBtn.active = false;
            fovBox.active = false; speedBox.active = false;
            lookAtXBox.active = false; lookAtYBox.active = false; lookAtZBox.active = false;
            orbitRadiusBox.active = false; orbitHeightBox.active = false; orbitSpeedBox.active = false;
            shakeTraumaBox.active = false; shakeDecayBox.active = false; shakeAngleBox.active = false; shakeOffsetBox.active = false;
        }
    }

    private void updateActionFromFields() {
        if (selectedActionIndex < 0 || selectedActionIndex >= cutscene.getActions().size()) return;
        var action = cutscene.getActions().get(selectedActionIndex);
        try { action.setX(Double.parseDouble(xBox.getValue())); } catch (Exception ignored) {}
        try { action.setY(Double.parseDouble(yBox.getValue())); } catch (Exception ignored) {}
        try { action.setZ(Double.parseDouble(zBox.getValue())); } catch (Exception ignored) {}
        try { action.setYaw(Float.parseFloat(yawBox.getValue())); } catch (Exception ignored) {}
        try { action.setPitch(Float.parseFloat(pitchBox.getValue())); } catch (Exception ignored) {}
        try { action.setRoll(Float.parseFloat(rollBox.getValue())); } catch (Exception ignored) {}
        try { action.setDuration(Integer.parseInt(durationBox.getValue())); } catch (Exception ignored) {}
        try { action.setFov(Float.parseFloat(fovBox.getValue())); } catch (Exception ignored) {}
        try { action.setSpeed(Float.parseFloat(speedBox.getValue())); } catch (Exception ignored) {}
        action.setStringValue(stringBox.getValue());
        try { action.setLookAtX(Double.parseDouble(lookAtXBox.getValue())); } catch (Exception ignored) {}
        try { action.setLookAtY(Double.parseDouble(lookAtYBox.getValue())); } catch (Exception ignored) {}
        try { action.setLookAtZ(Double.parseDouble(lookAtZBox.getValue())); } catch (Exception ignored) {}
        try { action.setOrbitRadius(Float.parseFloat(orbitRadiusBox.getValue())); } catch (Exception ignored) {}
        try { action.setOrbitHeight(Float.parseFloat(orbitHeightBox.getValue())); } catch (Exception ignored) {}
        try { action.setOrbitSpeed(Float.parseFloat(orbitSpeedBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeTrauma(Float.parseFloat(shakeTraumaBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeDecay(Float.parseFloat(shakeDecayBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeMaxAngle(Float.parseFloat(shakeAngleBox.getValue())); } catch (Exception ignored) {}
        try { action.setShakeMaxOffset(Float.parseFloat(shakeOffsetBox.getValue())); } catch (Exception ignored) {}
        dirty = true;
    }

    private void save() {
        cutscene.setName(nameBox.getValue());
        updateActionFromFields();
        IScriptNetwork.sendToServer(new SaveCutscenePacket(cutscene));
        dirty = false;
    }

    private void goBack() {
        CutscenePathRenderer.clearPreview();
        if (dirty) save();
        parent.currentSubScreen = new CutsceneListSubScreen(parent, level);
        parent.currentSubScreen.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF16161E);

        int listW = w / 2 - 8;
        int listY = y + 32;
        int visibleCount = (h - 80) / ACTION_HEIGHT;

        for (int i = actionScroll; i < Math.min(actionScroll + visibleCount, cutscene.getActions().size()); i++) {
            var action = cutscene.getActions().get(i);
            int rowY = listY + (i - actionScroll) * ACTION_HEIGHT;
            boolean hovered = mouseX >= x + 8 && mouseX <= x + 8 + listW && mouseY >= rowY && mouseY <= rowY + 18;
            boolean selected = i == selectedActionIndex;

            int bg = selected ? 0xFF334455 : (hovered ? 0xFF2A2A3A : 0xFF1E1E28);
            graphics.fill(x + 8, rowY, x + 8 + listW, rowY + 18, bg);
            if (selected) graphics.renderOutline(x + 8, rowY, listW, 18, 0xFF00D4AA);
            else graphics.renderOutline(x + 8, rowY, listW, 18, 0xFF333344);

            String label = i + ". " + action.getType().name();
            if (action.getType() == CutsceneActionType.DELAY) label += " (" + action.getDuration() + "t)";
            else if (action.getType() == CutsceneActionType.CAMERA_PATH) label += String.format(" [%.1f, %.1f, %.1f]", action.getX(), action.getY(), action.getZ());
            else if (action.getType() == CutsceneActionType.CAMERA_IDLE) label += " (idle)";
            else if (action.getType() == CutsceneActionType.CAMERA_LOOK) label += " (look)";
            else if (action.getType() == CutsceneActionType.CAMERA_ORBIT) label += " (orbit)";
            else if (action.getType() == CutsceneActionType.CAMERA_DOLLY) label += " (dolly)";
            else if (action.getType() == CutsceneActionType.CAMERA_SHAKE) label += " (shake)";
            graphics.drawString(this.font, label, x + 14, rowY + 5, selected ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        if (cutscene.getActions().isEmpty()) {
            graphics.drawCenteredString(this.font, "No actions", x + listW / 2 + 4, y + h / 2, 0xFF555566);
        }

        int fx = x + w / 2 + 4;
        int fy = y + 32;
        graphics.drawString(this.font, "Action Properties", fx, fy - 14, 0xFF55AAFF);

        fy = y + 32 + 24;
        drawLabel(graphics, "X:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Y:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Z:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Yaw:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Pitch:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Roll:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Duration:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "FOV:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Speed:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "LookAtX:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "LookAtY:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "LookAtZ:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "OrbitR:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "OrbitH:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "OrbitSpd:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "ShakeTr:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "ShakeDc:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "ShakeAng:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "ShakeOff:", fx, fy + 4); fy += 22;
        drawLabel(graphics, "Value:", fx, fy + 4); fy += 22;

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawLabel(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(this.font, text, x, y, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        for (var child : this.children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) parent.setFocusedWidget(w);
                return true;
            }
        }

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - x;
        int h = this.parent.height - y;
        int listW = w / 2 - 8;
        int listY = y + 32;
        int visibleCount = (h - 80) / ACTION_HEIGHT;

        for (int i = actionScroll; i < Math.min(actionScroll + visibleCount, cutscene.getActions().size()); i++) {
            int rowY = listY + (i - actionScroll) * ACTION_HEIGHT;
            if (mouseX >= x + 8 && mouseX <= x + 8 + listW && mouseY >= rowY && mouseY <= rowY + 18) {
                selectedActionIndex = i;
                updateActionFields();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visibleCount = (this.parent.height - DashboardScreen.TOPBAR_HEIGHT - 80) / ACTION_HEIGHT;
        int maxScroll = Math.max(0, cutscene.getActions().size() - visibleCount);
        if (delta > 0) actionScroll = Math.max(0, actionScroll - 1);
        else actionScroll = Math.min(actionScroll + 1, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (var child : this.children()) {
            if (child instanceof EditBox eb && eb.isFocused() && eb.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (var child : this.children()) {
            if (child instanceof EditBox eb && eb.isFocused() && eb.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }
}