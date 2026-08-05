package com.iscript.imson.gui.screen;

import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.data.npc.NPCState;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.network.IScriptNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.iscript.imson.network.packet.ServerCommandPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NPCEditScreen extends Screen {
    private final int entityId;
    private final NPCData initialData;
    private NPCData workingData;

    private EditBox idBox;
    private EditBox nameBox;
    private EditBox dialogBox;
    private EditBox skinBox;
    private EditBox factionBox;
    private EditBox healthBox;
    private EditBox attackBox;
    private EditBox movementBox;
    private EditBox hostileBox;
    private EditBox scaleBox;
    private EditBox followBox;
    private EditBox regenDelayBox;
    private EditBox regenFreqBox;
    private EditBox damageDelayBox;
    private EditBox pathDistBox;
    private EditBox postXBox;
    private EditBox postYBox;
    private EditBox postZBox;
    private EditBox postRadiusBox;
    private EditBox fallbackBox;
    private CycleButton<BehaviorMode> behaviorButton;

    private NPCState selectedState = NPCState.IDLE;
    private Set<NPCState> selectedStates = new HashSet<>();
    private BehaviorMode behaviorMode = BehaviorMode.NEUTRAL;
    private boolean nameVisible = true;
    private boolean glowEnabled = false;
    private boolean noAI = false;
    private boolean invulnerable = false;
    private boolean silent = false;
    private boolean hasGravity = true;
    private boolean enableTrade = false;
    private boolean canSwim = false;
    private boolean canFly = false;
    private boolean immovable = false;
    private boolean hasPost = false;
    private boolean patrolLoop = false;
    private boolean lookAtPlayer = false;
    private boolean lookAround = false;
    private boolean wander = false;
    private boolean alwaysWander = false;
    private boolean canFallDamage = true;
    private boolean canGetBurned = true;
    private boolean killable = true;

    private List<BlockPos> patrolPoints = new ArrayList<>();

    private int scrollY = 0;
    private int maxScroll = 0;
    private int contentBottom = 0;
    private int contentTop = 0;
    private static final int CONTENT_WIDTH = 460;
    private static final int PANEL_TOP = 30;
    private static final int PANEL_BOTTOM = 10;

    private final List<LabelEntry> labels = new ArrayList<>();
    private final List<SectionEntry> sections = new ArrayList<>();
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> widgetBaseY = new HashMap<>();

    private record LabelEntry(String text, int x, int baseY, int color) {}
    private record SectionEntry(String text, int x, int baseY) {}

    public enum BehaviorMode {
        PEACEFUL, NEUTRAL, AGGRESSIVE
    }

    private boolean selectingMorph = false;
    private List<String> allMorphIds = new ArrayList<>();
    private List<String> filteredMorphIds = new ArrayList<>();
    private String morphSearchQuery = "";
    private String currentMorphValue = "";

    public NPCEditScreen(int entityId, NPCData data) {
        super(I18n.t("iscript.npc.edit.title"));
        this.entityId = entityId;
        this.initialData = data;
        this.workingData = data;
        this.currentMorphValue = safe(data.getMorphModelId());
        allMorphIds.addAll(MorphManager.getAllModels().keySet());
        filteredMorphIds.addAll(allMorphIds);
    }

    @Override
    protected void init() {
        selectedStates.clear();
        selectedStates.addAll(initialData.getStates());
        if (selectedStates.isEmpty()) selectedStates.add(initialData.getState());
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
        int right = cx + 10;
        int w = (CONTENT_WIDTH - 40) / 2;
        int h = 16;
        contentTop = PANEL_TOP + 6;
        int y = contentTop;

        if (selectingMorph) {
            buildMorphSelector(left, y);
            return;
        }

        addLabel(I18n.s("iscript.npc.edit.npc_id"), left, y, Theme.TEXT_DIM);
        idBox = addField(left, y + 12, CONTENT_WIDTH - 20, h, safe(initialData.getId()));
        y += 34;

        addSection(I18n.s("iscript.npc.edit.section_identity"), left, y);
        addLabel(I18n.s("iscript.npc.edit.name"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.dialog_id"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        nameBox = addField(left, y, w, h, safe(initialData.getName()));
        dialogBox = addField(right, y, w, h, safe(initialData.getDialogId()));
        y += 28;

        addSection(I18n.s("iscript.npc.edit.section_appearance"), left, y);
        addLabel(I18n.s("iscript.npc.edit.skin_url"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.faction"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        skinBox = addField(left, y, w, h, safe(initialData.getSkin()));
        factionBox = addField(right, y, w, h, safe(initialData.getFaction()));
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.morph_model"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        int morphBtnW = CONTENT_WIDTH - 80;
        addScrollable(Button.builder(Component.literal(currentMorphValue.isEmpty() ? I18n.s("iscript.npc.edit.morph_none") : currentMorphValue), b -> {
            selectingMorph = true;
            morphSearchQuery = "";
            filteredMorphIds.clear();
            filteredMorphIds.addAll(allMorphIds);
            scrollY = 0;
            buildUI();
            applyScroll();
        }).pos(left, y).size(morphBtnW, h).build(), y);

        addScrollable(Button.builder(Component.literal(I18n.s("iscript.npc.edit.morph_list")), b -> {
            selectingMorph = true;
            morphSearchQuery = "";
            filteredMorphIds.clear();
            filteredMorphIds.addAll(allMorphIds);
            scrollY = 0;
            buildUI();
            applyScroll();
        }).pos(left + morphBtnW + 4, y).size(56, h).build(), y);
        y += 28;

        addSection(I18n.s("iscript.npc.edit.section_visual"), left, y);
        addLabel(I18n.s("iscript.npc.edit.scale"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        scaleBox = addField(left, y, w / 2, h, String.valueOf(initialData.getScale()));
        y += 28;

        addSection(I18n.s("iscript.npc.edit.section_combat"), left, y);
        addLabel(I18n.s("iscript.npc.edit.max_health"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.attack_damage"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        healthBox = addField(left, y, w, h, String.valueOf(initialData.getMaxHealth()));
        attackBox = addField(right, y, w, h, String.valueOf(initialData.getAttackDamage()));
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.move_speed"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.path_distance"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        movementBox = addField(left, y, w, h, String.valueOf(initialData.getMovementSpeed()));
        pathDistBox = addField(right, y, w, h, String.valueOf(initialData.getPathDistance()));
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.damage_delay"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        damageDelayBox = addField(left, y, w, h, String.valueOf(initialData.getDamageDelay()));
        y += 28;

        addSection(I18n.s("iscript.npc.edit.section_regeneration"), left, y);
        addLabel(I18n.s("iscript.npc.edit.regen_delay"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.regen_frequency"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        regenDelayBox = addField(left, y, w, h, String.valueOf(initialData.getRegenDelay()));
        regenFreqBox = addField(right, y, w, h, String.valueOf(initialData.getRegenFrequency()));
        y += 28;

        addSection(I18n.s("iscript.npc.edit.section_ai"), left, y);
        addLabel(I18n.s("iscript.npc.edit.behavior"), right, y + 14, Theme.TEXT_DIM);
        y += 24;

        String savedMode = initialData.getBehaviorMode();
        try {
            behaviorMode = BehaviorMode.valueOf(savedMode.toUpperCase());
        } catch (Exception e) {
            if (initialData.isAggressive()) {
                behaviorMode = BehaviorMode.AGGRESSIVE;
            } else if (initialData.getHostileFactions() == null || initialData.getHostileFactions().isEmpty()) {
                behaviorMode = BehaviorMode.PEACEFUL;
            } else {
                behaviorMode = BehaviorMode.NEUTRAL;
            }
        }

        behaviorButton = addScrollable(CycleButton.<BehaviorMode>builder(b -> I18n.t("iscript.npc.behavior." + b.name().toLowerCase(Locale.ROOT)))
                .withValues(BehaviorMode.values())
                .withInitialValue(behaviorMode)
                .create(right, y, w, h, I18n.t("iscript.npc.edit.behavior"), (btn, mode) -> behaviorMode = mode), y);
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.ai_states"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        NPCState[] allStates = NPCState.values();
        int stateBtnW = 72;
        int stateGap = 6;
        int statesPerRow = 4;
        for (int i = 0; i < allStates.length; i++) {
            NPCState s = allStates[i];
            boolean active = selectedStates.contains(s);
            int sx = left + (i % statesPerRow) * (stateBtnW + stateGap);
            int sy = y + (i / statesPerRow) * 20;
            final NPCState fs = s;
            addScrollable(Button.builder(Component.literal(s.name()).withStyle(st -> st.withColor(active ? Theme.ACCENT : Theme.TEXT_DIM)), btn -> {
                if (selectedStates.contains(fs)) selectedStates.remove(fs);
                else selectedStates.add(fs);
                if (selectedStates.isEmpty()) selectedStates.add(NPCState.IDLE);
                boolean nowActive = selectedStates.contains(fs);
                btn.setMessage(Component.literal(fs.name()).withStyle(st -> st.withColor(nowActive ? Theme.ACCENT : Theme.TEXT_DIM)));
            }).pos(sx, sy).size(stateBtnW, 16).build(), sy);
        }
        y += ((allStates.length + statesPerRow - 1) / statesPerRow) * 20 + 8;

        addLabel(I18n.s("iscript.npc.edit.hostile_factions"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        hostileBox = addField(left, y, CONTENT_WIDTH - 20, h, safe(initialData.getHostileFactions()));
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.follow_target"), left, y + 14, Theme.TEXT_DIM);
        y += 24;
        followBox = addField(left, y, CONTENT_WIDTH - 20, h, safe(initialData.getFollowTarget()));
        y += 30;

        addSection(I18n.s("iscript.npc.edit.section_post"), left, y);
        int fieldW = 60;
        int spacing = 20;
        int totalW = 3 * fieldW + 2 * spacing;
        int startX = cx - totalW / 2;
        addLabel("X", startX, y + 14, Theme.TEXT_DIM);
        addLabel("Y", startX + fieldW + spacing, y + 14, Theme.TEXT_DIM);
        addLabel("Z", startX + 2 * (fieldW + spacing), y + 14, Theme.TEXT_DIM);
        y += 24;
        BlockPos post = initialData.getPostPosition();
        postXBox = addField(startX, y, fieldW, h, post != null ? String.valueOf(post.getX()) : "");
        postYBox = addField(startX + fieldW + spacing, y, fieldW, h, post != null ? String.valueOf(post.getY()) : "");
        postZBox = addField(startX + 2 * (fieldW + spacing), y, fieldW, h, post != null ? String.valueOf(post.getZ()) : "");
        y += 28;

        addLabel(I18n.s("iscript.npc.edit.post_radius"), left, y + 14, Theme.TEXT_DIM);
        addLabel(I18n.s("iscript.npc.edit.fallback"), right, y + 14, Theme.TEXT_DIM);
        y += 24;
        postRadiusBox = addField(left, y, w, h, String.valueOf(initialData.getPostRadius()));
        fallbackBox = addField(right, y, w, h, String.valueOf(initialData.getFallback()));
        y += 30;

        addSection(I18n.s("iscript.npc.edit.section_settings"), left, y);
        y += 18;

        int btnW = 96;
        int gap = 8;
        int sx = cx - (btnW * 4 + gap * 3) / 2;

        nameVisible = initialData.isNameVisible();
        addScrollable(Button.builder(toggle("iscript.npc.edit.show_name", nameVisible), btn -> {
            nameVisible = !nameVisible;
            btn.setMessage(toggle("iscript.npc.edit.show_name", nameVisible));
        }).pos(sx, y).size(btnW, h).build(), y);

        glowEnabled = initialData.isGlowEnabled();
        addScrollable(Button.builder(toggle("iscript.npc.edit.glow", glowEnabled), btn -> {
            glowEnabled = !glowEnabled;
            btn.setMessage(toggle("iscript.npc.edit.glow", glowEnabled));
        }).pos(sx + btnW + gap, y).size(btnW, h).build(), y);

        noAI = initialData.isNoAI();
        addScrollable(Button.builder(toggle("iscript.npc.edit.no_ai", noAI), btn -> {
            noAI = !noAI;
            btn.setMessage(toggle("iscript.npc.edit.no_ai", noAI));
        }).pos(sx + (btnW + gap) * 2, y).size(btnW, h).build(), y);

        invulnerable = initialData.isInvulnerable();
        addScrollable(Button.builder(toggle("iscript.npc.edit.god_mode", invulnerable), btn -> {
            invulnerable = !invulnerable;
            btn.setMessage(toggle("iscript.npc.edit.god_mode", invulnerable));
        }).pos(sx + (btnW + gap) * 3, y).size(btnW, h).build(), y);
        y += 24;

        silent = initialData.isSilent();
        addScrollable(Button.builder(toggle("iscript.npc.edit.silent", silent), btn -> {
            silent = !silent;
            btn.setMessage(toggle("iscript.npc.edit.silent", silent));
        }).pos(sx, y).size(btnW, h).build(), y);

        hasGravity = initialData.isHasGravity();
        addScrollable(Button.builder(toggle("iscript.npc.edit.gravity", hasGravity), btn -> {
            hasGravity = !hasGravity;
            btn.setMessage(toggle("iscript.npc.edit.gravity", hasGravity));
        }).pos(sx + btnW + gap, y).size(btnW, h).build(), y);

        canSwim = initialData.isCanSwim();
        addScrollable(Button.builder(toggle("iscript.npc.edit.swim", canSwim), btn -> {
            canSwim = !canSwim;
            btn.setMessage(toggle("iscript.npc.edit.swim", canSwim));
        }).pos(sx + (btnW + gap) * 2, y).size(btnW, h).build(), y);

        canFly = initialData.isCanFly();
        addScrollable(Button.builder(toggle("iscript.npc.edit.fly", canFly), btn -> {
            canFly = !canFly;
            btn.setMessage(toggle("iscript.npc.edit.fly", canFly));
        }).pos(sx + (btnW + gap) * 3, y).size(btnW, h).build(), y);
        y += 24;

        immovable = initialData.isImmovable();
        addScrollable(Button.builder(toggle("iscript.npc.edit.immovable", immovable), btn -> {
            immovable = !immovable;
            btn.setMessage(toggle("iscript.npc.edit.immovable", immovable));
        }).pos(sx, y).size(btnW, h).build(), y);

        hasPost = initialData.isHasPost();
        addScrollable(Button.builder(toggle("iscript.npc.edit.has_post", hasPost), btn -> {
            hasPost = !hasPost;
            btn.setMessage(toggle("iscript.npc.edit.has_post", hasPost));
        }).pos(sx + btnW + gap, y).size(btnW, h).build(), y);

        patrolLoop = initialData.isPatrolLoop();
        addScrollable(Button.builder(toggle("iscript.npc.edit.patrol_loop", patrolLoop), btn -> {
            patrolLoop = !patrolLoop;
            btn.setMessage(toggle("iscript.npc.edit.patrol_loop", patrolLoop));
        }).pos(sx + (btnW + gap) * 2, y).size(btnW, h).build(), y);

        lookAtPlayer = initialData.isLookAtPlayer();
        addScrollable(Button.builder(toggle("iscript.npc.edit.look_player", lookAtPlayer), btn -> {
            lookAtPlayer = !lookAtPlayer;
            btn.setMessage(toggle("iscript.npc.edit.look_player", lookAtPlayer));
        }).pos(sx + (btnW + gap) * 3, y).size(btnW, h).build(), y);
        y += 24;

        lookAround = initialData.isLookAround();
        addScrollable(Button.builder(toggle("iscript.npc.edit.look_around", lookAround), btn -> {
            lookAround = !lookAround;
            btn.setMessage(toggle("iscript.npc.edit.look_around", lookAround));
        }).pos(sx, y).size(btnW, h).build(), y);

        wander = initialData.isWander();
        addScrollable(Button.builder(toggle("iscript.npc.edit.wander", wander), btn -> {
            wander = !wander;
            btn.setMessage(toggle("iscript.npc.edit.wander", wander));
        }).pos(sx + btnW + gap, y).size(btnW, h).build(), y);

        alwaysWander = initialData.isAlwaysWander();
        addScrollable(Button.builder(toggle("iscript.npc.edit.always_wander", alwaysWander), btn -> {
            alwaysWander = !alwaysWander;
            btn.setMessage(toggle("iscript.npc.edit.always_wander", alwaysWander));
        }).pos(sx + (btnW + gap) * 2, y).size(btnW, h).build(), y);

        canFallDamage = initialData.isCanFallDamage();
        addScrollable(Button.builder(toggle("iscript.npc.edit.fall_damage", canFallDamage), btn -> {
            canFallDamage = !canFallDamage;
            btn.setMessage(toggle("iscript.npc.edit.fall_damage", canFallDamage));
        }).pos(sx + (btnW + gap) * 3, y).size(btnW, h).build(), y);
        y += 24;

        canGetBurned = initialData.isCanGetBurned();
        addScrollable(Button.builder(toggle("iscript.npc.edit.burn", canGetBurned), btn -> {
            canGetBurned = !canGetBurned;
            btn.setMessage(toggle("iscript.npc.edit.burn", canGetBurned));
        }).pos(sx, y).size(btnW, h).build(), y);

        killable = initialData.isKillable();
        addScrollable(Button.builder(toggle("iscript.npc.edit.killable", killable), btn -> {
            killable = !killable;
            btn.setMessage(toggle("iscript.npc.edit.killable", killable));
        }).pos(sx + btnW + gap, y).size(btnW, h).build(), y);

        enableTrade = initialData.isEnableTrade();
        addScrollable(Button.builder(toggle("iscript.npc.edit.trade", enableTrade), btn -> {
            enableTrade = !enableTrade;
            btn.setMessage(toggle("iscript.npc.edit.trade", enableTrade));
        }).pos(sx + (btnW + gap) * 2, y).size(btnW, h).build(), y);

        addScrollable(Button.builder(I18n.t("iscript.npc.edit.edit_trade"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NPCTradeEditScreen(entityId, workingData));
            }
        }).pos(sx + (btnW + gap) * 3, y).size(btnW, h).build(), y);
        y += 30;

        addScrollable(Button.builder(I18n.t("iscript.npc.edit.edit_triggers"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NPCTriggerEditScreen(entityId, workingData));
            }
        }).pos(cx - 50, y).size(100, h).build(), y);
        y += 40;

        addScrollable(Button.builder(I18n.t("iscript.npc.edit.save"), btn -> save())
                .pos(cx - 155, y).size(90, 20).build(), y);
        addScrollable(Button.builder(I18n.t("iscript.npc.edit.npc_list"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new DashboardScreen());
            }
        }).pos(cx - 45, y).size(90, 20).build(), y);
        addScrollable(Button.builder(I18n.t("iscript.npc.edit.close"), btn -> this.onClose())
                .pos(cx + 65, y).size(90, 20).build(), y);

        contentBottom = y + 40;
    }

    private void buildMorphSelector(int left, int startY) {
        int y = startY;
        int w = CONTENT_WIDTH - 20;
        int h = 16;
        int cx = this.width / 2;

        addSection(I18n.s("iscript.npc.edit.morph_select_title"), left, y);
        y += 18;

        EditBox search = new EditBox(this.font, left, y, w, h, I18n.t("iscript.npc.edit.morph_search"));
        search.setMaxLength(128);
        search.setValue(morphSearchQuery);
        search.setResponder(q -> {
            morphSearchQuery = q;
            filteredMorphIds.clear();
            String low = q.toLowerCase();
            for (String id : allMorphIds) {
                if (id.toLowerCase().contains(low)) {
                    filteredMorphIds.add(id);
                }
            }
            scrollY = 0;
            buildUI();
            applyScroll();
        });
        addScrollable(search, y);
        y += 24;

        for (String id : filteredMorphIds) {
            boolean isCurrent = id.equals(currentMorphValue);
            int color = isCurrent ? Theme.ACCENT : Theme.TEXT_DIM;
            String label = isCurrent ? "[Active] " + id : id;
            addScrollable(Button.builder(Component.literal(label).withStyle(st -> st.withColor(color)), b -> {
                currentMorphValue = id;
                selectingMorph = false;
                scrollY = 0;
                buildUI();
                applyScroll();
            }).pos(left, y).size(w, h).build(), y);
            y += 20;
        }

        if (filteredMorphIds.isEmpty()) {
            addLabel(I18n.s("iscript.npc.edit.morph_no_models"), left, y, Theme.ERROR);
            y += 20;
        }

        y += 10;
        addScrollable(Button.builder(I18n.t("iscript.npc.edit.morph_back"), b -> {
            selectingMorph = false;
            scrollY = 0;
            buildUI();
            applyScroll();
        }).pos(cx - 40, y).size(80, 20).build(), y);
        y += 30;

        contentBottom = y + 20;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void addLabel(String text, int x, int y, int color) {
        labels.add(new LabelEntry(text, x, y, color));
    }

    private void addSection(String text, int x, int y) {
        sections.add(new SectionEntry(text, x, y));
    }

    private <T extends AbstractWidget> T addScrollable(T widget, int baseY) {
        scrollableWidgets.add(widget);
        widgetBaseY.put(widget, baseY);
        widget.setY(baseY - scrollY);
        return this.addRenderableWidget(widget);
    }

    private EditBox addField(int x, int y, int w, int h, String val) {
        EditBox b = new EditBox(this.font, x, y, w, h, Component.empty());
        b.setMaxLength(512);
        b.setValue(val);
        return addScrollable(b, y);
    }

    private Component toggle(String labelKey, boolean state) {
        String s = I18n.s(state ? "iscript.toggle.on" : "iscript.toggle.off");
        int c = state ? Theme.ACCENT : Theme.ERROR;
        return Component.literal(I18n.s(labelKey) + ": ").append(Component.literal(s).withStyle(st -> st.withColor(c)));
    }

    private NPCData buildData() {
        NPCData data = new NPCData();
        data.setId(idBox.getValue());
        data.setName(nameBox.getValue());
        data.setDialogId(dialogBox.getValue());
        data.setSkin(skinBox.getValue());
        data.setFaction(factionBox.getValue());
        data.setMorphModelId(currentMorphValue);

        float maxHealth = parseFloat(healthBox.getValue(), 20.0f);
        data.setMaxHealth(maxHealth);

        if (entityId >= 0 && this.minecraft != null && this.minecraft.level != null) {
            var entity = this.minecraft.level.getEntity(entityId);
            if (entity instanceof com.iscript.imson.entity.IScriptNPCEntity npc) {
                data.setHealth(Math.min(npc.getHealth(), maxHealth));
            } else {
                data.setHealth(maxHealth);
            }
        } else {
            data.setHealth(maxHealth);
        }

        data.setAttackDamage(parseFloat(attackBox.getValue(), 2.0f));
        data.setMovementSpeed(parseFloat(movementBox.getValue(), 0.4f));
        data.setScale(parseFloat(scaleBox.getValue(), 1.0f));
        data.setPathDistance(parseFloat(pathDistBox.getValue(), 32.0f));
        data.setDamageDelay(parseInt(damageDelayBox.getValue(), 20));
        data.setRegenDelay(parseInt(regenDelayBox.getValue(), 0));
        data.setRegenFrequency(parseInt(regenFreqBox.getValue(), 20));
        data.setPostRadius(parseFloat(postRadiusBox.getValue(), 1.0f));
        data.setFallback(parseFloat(fallbackBox.getValue(), 15.0f));
        data.setBehaviorMode(behaviorMode.name().toLowerCase());
        data.setAggressive(behaviorMode == BehaviorMode.AGGRESSIVE);
        if (behaviorMode == BehaviorMode.PEACEFUL) {
            data.setHostileFactions("");
        } else {
            data.setHostileFactions(hostileBox.getValue().trim());
        }

        if (!selectedStates.isEmpty()) {
            data.setStates(new java.util.HashSet<>(selectedStates));
            data.setState(selectedStates.iterator().next());
        } else {
            data.setState(NPCState.IDLE);
            data.getStates().add(NPCState.IDLE);
        }

        data.setFollowTarget(followBox.getValue());

        int px = parseInt(postXBox.getValue(), 0);
        int py = parseInt(postYBox.getValue(), 0);
        int pz = parseInt(postZBox.getValue(), 0);
        if (hasPost) {
            data.setPostPosition(new BlockPos(px, py, pz));
        } else {
            data.setPostPosition(null);
        }

        data.setPatrolPoints(new ArrayList<>(patrolPoints));

        data.setNameVisible(nameVisible);
        data.setGlowEnabled(glowEnabled);
        data.setNoAI(noAI);
        data.setInvulnerable(invulnerable);
        data.setSilent(silent);
        data.setHasGravity(hasGravity);
        data.setEnableTrade(enableTrade);
        data.setCanSwim(canSwim);
        data.setCanFly(canFly);
        data.setImmovable(immovable);
        data.setHasPost(hasPost);
        data.setPatrolLoop(patrolLoop);
        data.setLookAtPlayer(lookAtPlayer);
        data.setLookAround(lookAround);
        data.setWander(wander);
        data.setAlwaysWander(alwaysWander);
        data.setCanFallDamage(canFallDamage);
        data.setCanGetBurned(canGetBurned);
        data.setKillable(killable);
        data.setTriggers(new ArrayList<>(workingData.getTriggers()));
        data.setTradeData(workingData.getTradeData());

        return data;
    }

    private void save() {
        NPCData data = buildData();
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_NPC_DATA, ServerCommandPacket.saveNPCToTag(entityId, data)));
        this.onClose();
    }

    private float parseFloat(String val, float def) {
        try {
            float f = Float.parseFloat(val);
            return f > 0 ? f : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private int parseInt(String val, int def) {
        try {
            int i = Integer.parseInt(val);
            return i >= 0 ? i : def;
        } catch (NumberFormatException e) {
            return def;
        }
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
        if (this.getFocused() != null) {
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