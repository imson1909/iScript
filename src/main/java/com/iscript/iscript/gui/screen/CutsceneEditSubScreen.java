package com.iscript.iscript.gui.screen;

import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.CutscenePathRenderer;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.PathType;
import com.iscript.iscript.data.cutscene.CutsceneAction;
import com.iscript.iscript.data.cutscene.CutsceneActionType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.iscript.iscript.network.packet.ServerCommandPacket;

import static com.iscript.iscript.gui.screen.I18n.t;

public class CutsceneEditSubScreen extends DashboardScreen.SubScreen {
    private final CutsceneData cutscene;
    private EditBox nameBox;
    private int actionScroll = 0;
    private int selectedActionIndex = -1;
    private static final int ACTION_H = 24;
    private int propScroll = 0;
    private static final int PROP_ROW_H = 24;
    private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget, Integer> propBaseY = new java.util.HashMap<>();

    private void savePropBaseY(net.minecraft.client.gui.components.AbstractWidget w) {
        if (!propBaseY.containsKey(w)) propBaseY.put(w, w.getY());
    }

    private void applyPropScroll() {
        for (var e : propBaseY.entrySet()) {
            e.getKey().setY(e.getValue() - propScroll);
        }
    }

    private EditBox xBox, yBox, zBox, yawBox, pitchBox, rollBox, durationBox, stringBox;
    private EditBox fovBox, lookAtXBox, lookAtYBox, lookAtZBox;
    private EditBox orbitRadiusBox, orbitHeightBox, orbitSpeedBox;
    private EditBox shakeTraumaBox, shakeDecayBox, shakeAngleBox, shakeOffsetBox;
    private EditBox speedBox;
    private Btn typeBtn, saveBtn, backBtn, addBtn, removeBtn, duplicateBtn, usePosBtn, previewBtn, loopBtn, showPathBtn;
    private Btn interpBtn, pathTypeBtn, useFovBtn, constantSpeedBtn;
    private boolean dirty = false;
    private Interpolation[] interps = Interpolation.values();
    private int interpIndex = 0;
    private PathType[] pathTypes = PathType.values();
    private int pathTypeIndex = 0;
    private boolean pathVisible = false;

    public CutsceneEditSubScreen(DashboardScreen parent, CutsceneData data) {
        super(parent);
        this.cutscene = data;
    }

    @Override
    public void init() {
        this.clearWidgets();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int h = this.parent.height - y;

        nameBox = new EditBox(this.font, x + 10, y + 10, 220, 18, t("iscript.cutscene.edit.name"));
        nameBox.setValue(cutscene.getName());
        nameBox.setTextColor(Theme.TEXT);
        nameBox.setResponder(s -> dirty = true);
        this.addRenderableWidget(nameBox);

        int pad = 6;
        int gap = 4;
        int right = x + w - 10;
        int top = y + 10;
        int btm = y + h - 28;

        right = autoBtn(t("iscript.cutscene.edit.save").getString(), right, top, 18, this::save, true, pad, gap);
        right = autoBtn(t("iscript.cutscene.edit.back").getString(), right, top, 18, this::goBack, false, pad, gap);
        right = autoBtn(t("iscript.cutscene.edit.preview").getString(), right, top, 18, this::playPreview, false, pad, gap);
        right = autoBtn(t("iscript.cutscene.edit.show_path").getString(), right, top, 18, this::togglePath, false, pad, gap);
        loopBtn = btn(t("iscript.cutscene.edit.loop", cutscene.isLoop()).getString(), x + 240, y + 10, font.width(t("iscript.cutscene.edit.loop", cutscene.isLoop()).getString()) + pad * 2, 18, this::toggleLoop, false);

        right = x + w - 10;
        right = autoBtn(t("iscript.cutscene.edit.duplicate").getString(), right, btm, 22, this::duplicateAction, false, pad, gap);
        right = autoBtn(t("iscript.cutscene.edit.remove").getString(), right, btm, 22, this::removeAction, false, pad, gap);
        autoBtn(t("iscript.cutscene.edit.add").getString(), right, btm, 22, this::addAction, true, pad, gap);

        int fx = x + w / 2 + 8;
        int fy = y + 36;

        typeBtn = btn(t("iscript.cutscene.edit.type", "DELAY").getString(), fx, fy, 130, 18, this::cycleType, false);
        fy += 24;
        xBox = field(fx, fy, 70); fy += 24;
        yBox = field(fx, fy, 70); fy += 24;
        zBox = field(fx, fy, 70); fy += 24;
        yawBox = field(fx, fy, 70); fy += 24;
        pitchBox = field(fx, fy, 70); fy += 24;
        rollBox = field(fx, fy, 70); fy += 24;
        durationBox = field(fx, fy, 70); fy += 24;
        fovBox = field(fx, fy, 70); fy += 24;
        speedBox = field(fx, fy, 70); fy += 24;

        usePosBtn = btn(t("iscript.cutscene.edit.use_my_pos").getString(), fx + 80, y + 36 + 24, 80, 18, this::usePlayerPos, false);
        interpBtn = btn(t("iscript.cutscene.edit.interp", "LINEAR").getString(), fx + 80, y + 36 + 48, 110, 18, this::cycleInterp, false);
        pathTypeBtn = btn(t("iscript.cutscene.edit.path_type", "CATMULL_ROM").getString(), fx + 80, y + 36 + 72, 130, 18, this::cyclePathType, false);
        useFovBtn = btn(t("iscript.cutscene.edit.fov", t("iscript.cutscene.edit.fov_off").getString()).getString(), fx + 80, y + 36 + 96, 80, 18, this::toggleUseFov, false);
        constantSpeedBtn = btn(t("iscript.cutscene.edit.constant_speed", t("iscript.cutscene.edit.constant_speed_off").getString()).getString(), fx + 80, y + 36 + 120, 100, 18, this::toggleConstantSpeed, false);

        fy += 10;
        lookAtXBox = field(fx, fy, 70); fy += 24;
        lookAtYBox = field(fx, fy, 70); fy += 24;
        lookAtZBox = field(fx, fy, 70); fy += 24;
        orbitRadiusBox = field(fx, fy, 70); fy += 24;
        orbitHeightBox = field(fx, fy, 70); fy += 24;
        orbitSpeedBox = field(fx, fy, 70); fy += 24;
        shakeTraumaBox = field(fx, fy, 70); fy += 24;
        shakeDecayBox = field(fx, fy, 70); fy += 24;
        shakeAngleBox = field(fx, fy, 70); fy += 24;
        shakeOffsetBox = field(fx, fy, 70); fy += 24;

        stringBox = new EditBox(this.font, fx + 55, fy, w / 2 - 76, 18, Component.empty());
        stringBox.setMaxLength(256);
        stringBox.setTextColor(Theme.TEXT);
        stringBox.setResponder(s -> { if (selectedActionIndex >= 0) updateActionFromFields(); });
        this.addRenderableWidget(stringBox);

        savePropBaseY(typeBtn); savePropBaseY(xBox); savePropBaseY(yBox); savePropBaseY(zBox);
        savePropBaseY(yawBox); savePropBaseY(pitchBox); savePropBaseY(rollBox); savePropBaseY(durationBox);
        savePropBaseY(fovBox); savePropBaseY(speedBox); savePropBaseY(usePosBtn); savePropBaseY(interpBtn);
        savePropBaseY(pathTypeBtn); savePropBaseY(useFovBtn); savePropBaseY(constantSpeedBtn);
        savePropBaseY(lookAtXBox); savePropBaseY(lookAtYBox); savePropBaseY(lookAtZBox);
        savePropBaseY(orbitRadiusBox); savePropBaseY(orbitHeightBox); savePropBaseY(orbitSpeedBox);
        savePropBaseY(shakeTraumaBox); savePropBaseY(shakeDecayBox); savePropBaseY(shakeAngleBox); savePropBaseY(shakeOffsetBox);
        savePropBaseY(stringBox);
        applyPropScroll();
        updateActionFields();
        updatePathPreview();
    }

    private Btn btn(String text, int x, int y, int w, int h, Runnable r, boolean accent) {
        Btn b = new Btn(x, y, w, h, text, r, accent);
        this.addRenderableWidget(b);
        return b;
    }

    private int autoBtn(String text, int right, int y, int h, Runnable r, boolean accent, int pad, int gap) {
        int w = font.width(text) + pad * 2;
        int bx = right - w;
        btn(text, bx, y, w, h, r, accent);
        return bx - gap;
    }

    private EditBox field(int x, int y, int w) {
        EditBox b = new EditBox(this.font, x + 55, y, w, 18, Component.empty());
        b.setMaxLength(128);
        b.setTextColor(Theme.TEXT);
        b.setResponder(s -> { if (selectedActionIndex >= 0) updateActionFromFields(); });
        this.addRenderableWidget(b);
        return b;
    }

    private void playPreview() {
        if (cutscene != null)
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(cutscene.getId(), 1.0f, 0)));
    }

    private void togglePath() {
        pathVisible = !pathVisible;
        showPathBtn.text = pathVisible ? t("iscript.cutscene.edit.hide_path").getString() : t("iscript.cutscene.edit.show_path").getString();
        updatePathPreview();
    }

    private void updatePathPreview() {
        if (!pathVisible || minecraft == null || minecraft.player == null) {
            CutscenePathRenderer.clearPreview();
            return;
        }
        CutscenePath path = buildPathFromCameraActions();
        CutsceneCameraMode mode = buildModeFromCameraActions();
        if (path != null && !path.keyframes.isEmpty())
            CutscenePathRenderer.setPreviewPath(path, mode);
        else
            CutscenePathRenderer.clearPreview();
    }

    private CutscenePath buildPathFromCameraActions() {
        CutscenePath path = new CutscenePath();
        path.easing = Interpolation.LINEAR;
        for (CutsceneAction action : cutscene.getActions()) {
            if (action.getType() != CutsceneActionType.CAMERA_PATH &&
                    action.getType() != CutsceneActionType.CAMERA_IDLE &&
                    action.getType() != CutsceneActionType.CAMERA_ORBIT &&
                    action.getType() != CutsceneActionType.CAMERA_DOLLY) continue;

            try { path.easing = Interpolation.valueOf(action.getInterpolation()); } catch (Exception e) {}

            PathType segType;
            try { segType = PathType.valueOf(action.getPathType()); } catch (Exception e) { segType = PathType.CATMULL_ROM; }

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
        return path.keyframes.size() < 2 ? null : path;
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
        loopBtn.text = t("iscript.cutscene.edit.loop", cutscene.isLoop()).getString();
        dirty = true;
    }

    private void toggleUseFov() {
        if (selectedActionIndex < 0) return;
        var a = cutscene.getActions().get(selectedActionIndex);
        a.setUseFov(!a.isUseFov());
        useFovBtn.text = t("iscript.cutscene.edit.fov", a.isUseFov() ? t("iscript.cutscene.edit.fov_on").getString() : t("iscript.cutscene.edit.fov_off").getString()).getString();
        dirty = true;
    }

    private void toggleConstantSpeed() {
        if (selectedActionIndex < 0) return;
        var a = cutscene.getActions().get(selectedActionIndex);
        a.setConstantSpeed(!a.isConstantSpeed());
        constantSpeedBtn.text = t("iscript.cutscene.edit.constant_speed", a.isConstantSpeed() ? t("iscript.cutscene.edit.constant_speed_on").getString() : t("iscript.cutscene.edit.constant_speed_off").getString()).getString();
        dirty = true;
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

    private void cycleType() {
        if (selectedActionIndex < 0) return;
        var a = cutscene.getActions().get(selectedActionIndex);
        CutsceneActionType[] types = CutsceneActionType.values();
        a.setType(types[(a.getType().ordinal() + 1) % types.length]);
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void cycleInterp() {
        if (selectedActionIndex < 0) return;
        interpIndex = (interpIndex + 1) % interps.length;
        cutscene.getActions().get(selectedActionIndex).setInterpolation(interps[interpIndex].name());
        interpBtn.text = t("iscript.cutscene.edit.interp", interps[interpIndex].name()).getString();
        dirty = true;
        if (pathVisible) updatePathPreview();
    }

    private void cyclePathType() {
        if (selectedActionIndex < 0) return;
        pathTypeIndex = (pathTypeIndex + 1) % pathTypes.length;
        cutscene.getActions().get(selectedActionIndex).setPathType(pathTypes[pathTypeIndex].name());
        pathTypeBtn.text = t("iscript.cutscene.edit.path_type", pathTypes[pathTypeIndex].name()).getString();
        dirty = true;
        if (pathVisible) updatePathPreview();
    }

    private void addAction() {
        CutsceneAction a = new CutsceneAction();
        a.setType(CutsceneActionType.DELAY);
        a.setDuration(20);
        if (selectedActionIndex >= 0 && selectedActionIndex < cutscene.getActions().size() - 1) {
            cutscene.getActions().add(selectedActionIndex + 1, a);
            selectedActionIndex++;
        } else {
            cutscene.getActions().add(a);
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
        var orig = cutscene.getActions().get(selectedActionIndex);
        CutsceneAction copy = new CutsceneAction();
        CompoundTag tag = new CompoundTag();
        orig.save(tag);
        copy.load(tag);
        cutscene.getActions().add(selectedActionIndex + 1, copy);
        selectedActionIndex++;
        dirty = true;
        updateActionFields();
        if (pathVisible) updatePathPreview();
    }

    private void updateActionFields() {
        if (selectedActionIndex >= 0 && selectedActionIndex < cutscene.getActions().size()) {
            var a = cutscene.getActions().get(selectedActionIndex);
            typeBtn.text = t("iscript.cutscene.edit.type", a.getType().name()).getString();
            xBox.setValue(String.valueOf(a.getX()));
            yBox.setValue(String.valueOf(a.getY()));
            zBox.setValue(String.valueOf(a.getZ()));
            yawBox.setValue(String.valueOf(a.getYaw()));
            pitchBox.setValue(String.valueOf(a.getPitch()));
            rollBox.setValue(String.valueOf(a.getRoll()));
            durationBox.setValue(String.valueOf(a.getDuration()));
            fovBox.setValue(String.valueOf(a.getFov()));
            speedBox.setValue(String.valueOf(a.getSpeed()));
            stringBox.setValue(a.getStringValue());
            lookAtXBox.setValue(String.valueOf(a.getLookAtX()));
            lookAtYBox.setValue(String.valueOf(a.getLookAtY()));
            lookAtZBox.setValue(String.valueOf(a.getLookAtZ()));
            orbitRadiusBox.setValue(String.valueOf(a.getOrbitRadius()));
            orbitHeightBox.setValue(String.valueOf(a.getOrbitHeight()));
            orbitSpeedBox.setValue(String.valueOf(a.getOrbitSpeed()));
            shakeTraumaBox.setValue(String.valueOf(a.getShakeTrauma()));
            shakeDecayBox.setValue(String.valueOf(a.getShakeDecay()));
            shakeAngleBox.setValue(String.valueOf(a.getShakeMaxAngle()));
            shakeOffsetBox.setValue(String.valueOf(a.getShakeMaxOffset()));

            try {
                interpIndex = java.util.Arrays.asList(interps).indexOf(Interpolation.valueOf(a.getInterpolation()));
                if (interpIndex < 0) interpIndex = 0;
            } catch (Exception e) { interpIndex = 0; }
            interpBtn.text = t("iscript.cutscene.edit.interp", interps[interpIndex].name()).getString();

            try {
                pathTypeIndex = java.util.Arrays.asList(pathTypes).indexOf(PathType.valueOf(a.getPathType()));
                if (pathTypeIndex < 0) pathTypeIndex = 0;
            } catch (Exception e) { pathTypeIndex = 0; }
            pathTypeBtn.text = t("iscript.cutscene.edit.path_type", pathTypes[pathTypeIndex].name()).getString();
            useFovBtn.text = t("iscript.cutscene.edit.fov", a.isUseFov() ? t("iscript.cutscene.edit.fov_on").getString() : t("iscript.cutscene.edit.fov_off").getString()).getString();
            constantSpeedBtn.text = t("iscript.cutscene.edit.constant_speed", a.isConstantSpeed() ? t("iscript.cutscene.edit.constant_speed_on").getString() : t("iscript.cutscene.edit.constant_speed_off").getString()).getString();

            var t = a.getType();
            boolean pos = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH ||
                    t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY ||
                    t == CutsceneActionType.CAMERA_FOLLOW || t == CutsceneActionType.NPC_MOVE ||
                    t == CutsceneActionType.BLOCK;
            boolean rot = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH ||
                    t == CutsceneActionType.CAMERA_FOLLOW;
            boolean dur = t == CutsceneActionType.DELAY || t == CutsceneActionType.CAMERA_IDLE ||
                    t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_ORBIT ||
                    t == CutsceneActionType.CAMERA_DOLLY || t == CutsceneActionType.CAMERA_FOLLOW ||
                    t == CutsceneActionType.NPC_MOVE;
            boolean str = t == CutsceneActionType.DIALOG || t == CutsceneActionType.SOUND ||
                    t == CutsceneActionType.BLOCK || t == CutsceneActionType.SCRIPT ||
                    t == CutsceneActionType.NPC_ANIMATION;
            boolean look = t == CutsceneActionType.CAMERA_LOOK || t == CutsceneActionType.CAMERA_DOLLY;
            boolean orb = t == CutsceneActionType.CAMERA_ORBIT;
            boolean shake = t == CutsceneActionType.CAMERA_SHAKE;
            boolean path = t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_FOLLOW;

            xBox.active = pos; yBox.active = pos; zBox.active = pos;
            yawBox.active = rot; pitchBox.active = rot; rollBox.active = rot;
            durationBox.active = dur; stringBox.active = str;
            usePosBtn.active = pos; interpBtn.active = path;
            pathTypeBtn.active = path || t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY;
            useFovBtn.active = t == CutsceneActionType.CAMERA_IDLE || t == CutsceneActionType.CAMERA_PATH || t == CutsceneActionType.CAMERA_ORBIT || t == CutsceneActionType.CAMERA_DOLLY;
            constantSpeedBtn.active = path;
            fovBox.active = useFovBtn.active;
            speedBox.active = path;

            lookAtXBox.active = look; lookAtYBox.active = look; lookAtZBox.active = look;
            orbitRadiusBox.active = orb; orbitHeightBox.active = orb; orbitSpeedBox.active = orb;
            shakeTraumaBox.active = shake; shakeDecayBox.active = shake; shakeAngleBox.active = shake; shakeOffsetBox.active = shake;
        } else {
            typeBtn.text = t("iscript.cutscene.edit.type", "-").getString();
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
        var a = cutscene.getActions().get(selectedActionIndex);
        try { a.setX(Double.parseDouble(xBox.getValue())); } catch (Exception ignored) {}
        try { a.setY(Double.parseDouble(yBox.getValue())); } catch (Exception ignored) {}
        try { a.setZ(Double.parseDouble(zBox.getValue())); } catch (Exception ignored) {}
        try { a.setYaw(Float.parseFloat(yawBox.getValue())); } catch (Exception ignored) {}
        try { a.setPitch(Float.parseFloat(pitchBox.getValue())); } catch (Exception ignored) {}
        try { a.setRoll(Float.parseFloat(rollBox.getValue())); } catch (Exception ignored) {}
        try { a.setDuration(Integer.parseInt(durationBox.getValue())); } catch (Exception ignored) {}
        try { a.setFov(Float.parseFloat(fovBox.getValue())); } catch (Exception ignored) {}
        try { a.setSpeed(Float.parseFloat(speedBox.getValue())); } catch (Exception ignored) {}
        a.setStringValue(stringBox.getValue());
        try { a.setLookAtX(Double.parseDouble(lookAtXBox.getValue())); } catch (Exception ignored) {}
        try { a.setLookAtY(Double.parseDouble(lookAtYBox.getValue())); } catch (Exception ignored) {}
        try { a.setLookAtZ(Double.parseDouble(lookAtZBox.getValue())); } catch (Exception ignored) {}
        try { a.setOrbitRadius(Float.parseFloat(orbitRadiusBox.getValue())); } catch (Exception ignored) {}
        try { a.setOrbitHeight(Float.parseFloat(orbitHeightBox.getValue())); } catch (Exception ignored) {}
        try { a.setOrbitSpeed(Float.parseFloat(orbitSpeedBox.getValue())); } catch (Exception ignored) {}
        try { a.setShakeTrauma(Float.parseFloat(shakeTraumaBox.getValue())); } catch (Exception ignored) {}
        try { a.setShakeDecay(Float.parseFloat(shakeDecayBox.getValue())); } catch (Exception ignored) {}
        try { a.setShakeMaxAngle(Float.parseFloat(shakeAngleBox.getValue())); } catch (Exception ignored) {}
        try { a.setShakeMaxOffset(Float.parseFloat(shakeOffsetBox.getValue())); } catch (Exception ignored) {}
        dirty = true;
    }

    private void save() {
        cutscene.setName(nameBox.getValue());
        updateActionFromFields();
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(cutscene)));
        dirty = false;
    }

    private void goBack() {
        CutscenePathRenderer.clearPreview();
        if (dirty) save();
        parent.currentSubScreen = new CutsceneListSubScreen(parent);
        parent.currentSubScreen.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        UI.inner(g, x, y, w, h);

        int listW = w / 2 - 12;
        int listY = y + 36;
        int visible = (h - 84) / ACTION_H;

        for (int i = actionScroll; i < Math.min(actionScroll + visible, cutscene.getActions().size()); i++) {
            var a = cutscene.getActions().get(i);
            int ry = listY + (i - actionScroll) * ACTION_H;
            boolean hov = mx >= x + 12 && mx <= x + 12 + listW && my >= ry && my <= ry + 20;
            boolean sel = i == selectedActionIndex;

            UI.row(g, x + 12, ry, listW, 20, sel, hov);

            String label = i + ". " + a.getType().name();
            if (a.getType() == CutsceneActionType.DELAY) label += " (" + a.getDuration() + "t)";
            else if (a.getType() == CutsceneActionType.CAMERA_PATH) label += String.format(" [%.1f, %.1f, %.1f]", a.getX(), a.getY(), a.getZ());
            else if (a.getType() == CutsceneActionType.CAMERA_IDLE) label += " (idle)";
            else if (a.getType() == CutsceneActionType.CAMERA_LOOK) label += " (look)";
            else if (a.getType() == CutsceneActionType.CAMERA_ORBIT) label += " (orbit)";
            else if (a.getType() == CutsceneActionType.CAMERA_DOLLY) label += " (dolly)";
            else if (a.getType() == CutsceneActionType.CAMERA_SHAKE) label += " (shake)";
            g.drawString(this.font, label, x + 18, ry + 6, sel ? Theme.TEXT : Theme.TEXT_DIM);
        }

        if (cutscene.getActions().isEmpty())
            UI.centerLabel(g, this.font, t("iscript.cutscene.edit.no_actions").getString(), x, y + h / 2, listW + 24);

        int fx = x + w / 2 + 8;
        UI.title(g, this.font, t("iscript.cutscene.edit.action_properties").getString(), fx, y + 22);

        int fy = y + 36 + 24 - propScroll;
        lbl(g, t("iscript.cutscene.edit.x").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.y").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.z").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.yaw").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.pitch").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.roll").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.duration").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.fov_label").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.speed").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.lookat_x").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.lookat_y").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.lookat_z").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.orbit_radius").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.orbit_height").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.orbit_speed").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.shake_trauma").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.shake_decay").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.shake_angle").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.shake_offset").getString(), fx, fy + 5); fy += 24;
        lbl(g, t("iscript.cutscene.edit.value").getString(), fx, fy + 5);

        for (var r : this.renderables) r.render(g, mx, my, pt);
    }

    private void lbl(GuiGraphics g, String t, int x, int y) {
        g.drawString(this.font, t, x, y, Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b != 0) return false;
        for (var c : this.children()) {
            if (c.mouseClicked(mx, my, b)) {
                if (c instanceof AbstractWidget w) parent.setFocusedWidget(w);
                return true;
            }
        }
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int h = this.parent.height - y;
        int listW = w / 2 - 12;
        int listY = y + 36;
        int visible = (h - 84) / ACTION_H;

        for (int i = actionScroll; i < Math.min(actionScroll + visible, cutscene.getActions().size()); i++) {
            int ry = listY + (i - actionScroll) * ACTION_H;
            if (mx >= x + 12 && mx <= x + 12 + listW && my >= ry && my <= ry + 20) {
                selectedActionIndex = i;
                updateActionFields();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int h = this.parent.height - y;
        int fx = x + w / 2 + 8;
        if (mx >= fx && mx <= x + w - 8 && my >= y + 36 && my <= y + h - 32) {
            int totalRows = 24;
            int visibleRows = (h - 84) / PROP_ROW_H;
            int maxProp = Math.max(0, totalRows - visibleRows) * PROP_ROW_H;
            if (d > 0) propScroll = Math.max(0, propScroll - PROP_ROW_H);
            else propScroll = Math.min(propScroll + PROP_ROW_H, maxProp);
            applyPropScroll();
            return true;
        }
        int visible = (this.parent.height - DashboardScreen.TOPBAR_H - 84) / ACTION_H;
        int max = Math.max(0, cutscene.getActions().size() - visible);
        actionScroll = d > 0 ? Math.max(0, actionScroll - 1) : Math.min(actionScroll + 1, max);
        return true;
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        for (var c : this.children())
            if (c instanceof EditBox eb && eb.isFocused() && eb.keyPressed(k, s, m)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char c, int m) {
        for (var ch : this.children())
            if (ch instanceof EditBox eb && eb.isFocused() && eb.charTyped(c, m)) return true;
        return false;
    }

    static class Btn extends AbstractWidget {
        String text;
        final Runnable onClick;
        final boolean accent;

        Btn(int x, int y, int w, int h, String t, Runnable r, boolean a) {
            super(x, y, w, h, Component.empty());
            text = t; onClick = r; accent = a;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isMouseOver(mx, my);
            int bg = accent ? (hov ? Theme.alpha(Theme.ACCENT, 0.25f) : Theme.alpha(Theme.ACCENT, 0.12f))
                    : (hov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.renderOutline(getX(), getY(), width, height, hov ? Theme.ACCENT : Theme.BORDER);
            int tc = active ? (accent ? Theme.ACCENT : (hov ? Theme.TEXT : Theme.TEXT_DIM)) : Theme.TEXT_MUTE;
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, text, getX() + width / 2, getY() + (height - 8) / 2, tc);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            if (active && isMouseOver(mx, my)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
}