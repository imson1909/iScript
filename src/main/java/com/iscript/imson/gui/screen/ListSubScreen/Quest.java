package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.ModData;
import com.iscript.imson.data.quest.*;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.widget.MultiLineEditBox;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quest extends ListSubScreen {

    private int editorContentHeight = 0;
    private int editorScroll = 0;
    private int editorTab = 0;
    private int expandedStage = -1;
    private int expandedObjective = -1;

    private int addObjectiveStageIdx = -1;
    private int addObjectiveY = 0;
    private int objectiveTypeIndex = 0;

    private int addStageY = 0;

    private int addPrereqY = 0;

    private int addItemRewardY = 0;

    private int itemPickerY = 0;
    private String itemPickerSelectedId = "";
    private int itemPickerScroll = 0;
    private final List<Item> itemPickerFiltered = new ArrayList<>();

    private int editExpY = 0;
    private int editCmdY = 0;
    private int editTitleY = 0;
    private int editItemCountIdx = -1;
    private int editItemCountY = 0;

    private boolean showAddObjectiveTypeMenu = false;
    private int objTypeMenuX, objTypeMenuY;

    private boolean showAddPrereqMenu = false;
    private int prereqMenuX, prereqMenuY;

    private int prereqDropdownIndex = 0;

    public Quest(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected int getRightPanelWidth() { return 140; }

    @Override
    protected int getToolbarWidth() { return 32; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.quest.editor.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.quest.editor.empty"); }

    @Override
    protected String getNewButtonText() { return I18n.s("iscript.quest.editor.new"); }

    @Override
    protected boolean canCreateNew() { return false; }

    @Override
    protected net.minecraft.network.chat.Component getSearchLabel() {
        return net.minecraft.network.chat.Component.literal(I18n.s("iscript.quest.list.search"));
    }

    @Override
    protected List<String> getItemIds() {
        return new ArrayList<>(DataAccess.quests().keySet());
    }

    @Override
    protected String getItemDisplayName(String id) {
        QuestData q = DataAccess.quest(id);
        if (q == null) return id;
        String title = q.getTitle();
        return title == null || title.isEmpty() ? id : title;
    }

    @Override
    protected void onSelect(String id) {
        lifecycle.editors().removeAll();
        editorScroll = 0;
        editorTab = 0;
        expandedStage = -1;
        expandedObjective = -1;
        setSelectedId(id);
        if (id == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        int toolbarX = rightX - getToolbarWidth();
        int leftW = toolbarX - x - 8;
        int leftX = x + 4;

        QuestData quest = DataAccess.quest(id);
        if (quest == null) return;

        EditBox titleBox = lifecycle.editors().addBox("title", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.title"), quest.getTitle());
        if (titleBox != null) titleBox.setResponder(s -> lifecycle.save().debounce(10));

        MultiLineEditBox descBox = lifecycle.editors().addMultiBox("desc", leftX + 4, 0, leftW - 8, 50, I18n.t("iscript.quest.editor.placeholder.description"), quest.getDescription());
        if (descBox != null) descBox.setOnValueChanged(() -> lifecycle.save().debounce(10));

        EditBox giverBox = lifecycle.editors().addBox("giver", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.giver_npc"), quest.getGiverNpcId());
        if (giverBox != null) giverBox.setResponder(s -> lifecycle.save().debounce(10));

        EditBox turnInBox = lifecycle.editors().addBox("turnIn", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.turnin_npc"), quest.getTurnInNpcId());
        if (turnInBox != null) turnInBox.setResponder(s -> lifecycle.save().debounce(10));
    }

    @Override
    protected void onNew(String id) {
        QuestData quest = new QuestData();
        quest.setId(id);
        quest.setTitle(id);
        DataAccess.putQuest(quest);
        onSelect(id);
    }

    @Override
    protected void onDelete(String id) {
        DataAccess.removeQuest(id);
        if (id.equals(getSelectedId())) {
            lifecycle.editors().removeAll();
        }
    }

    @Override
    protected void onRename(String oldId, String newId) {
        if (oldId.equals(newId)) return;
        QuestData oldQuest = DataAccess.quest(oldId);
        if (oldQuest == null) return;
        QuestData newQuest = oldQuest.copy();
        newQuest.setId(newId);
        DataAccess.putQuest(newQuest);
        DataAccess.removeQuest(oldId);
        if (oldId.equals(getSelectedId())) {
            setSelectedId(newId);
        }
    }

    @Override
    protected void onDuplicate(String id) {
        QuestData source = DataAccess.quest(id);
        if (source == null) return;
        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (DataAccess.quest(newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }
        QuestData copy = source.copy();
        copy.setId(newId);
        copy.setTitle(source.getTitle() + I18n.s("iscript.quest.editor.duplicate_suffix", counter));
        DataAccess.putQuest(copy);
        onSelect(newId);
    }

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
        String sourceId = DashboardScreen.clipboard;
        if (sourceId == null || sourceId.isEmpty()) return;
        QuestData source = DataAccess.quest(sourceId);
        if (source == null) return;
        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (DataAccess.quest(newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }
        QuestData copy = source.copy();
        copy.setId(newId);
        copy.setTitle(source.getTitle() + I18n.s("iscript.quest.editor.copy_suffix"));
        DataAccess.putQuest(copy);
        onSelect(newId);
    }

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
    }

    @Override
    protected void doSave() {
        String editingQuestId = getSelectedId();
        if (editingQuestId == null) return;
        QuestData quest = DataAccess.quest(editingQuestId);
        if (quest == null) return;
        EditBox titleBox = lifecycle.editors().box("title");
        MultiLineEditBox descBox = lifecycle.editors().multi("desc");
        EditBox giverBox = lifecycle.editors().box("giver");
        EditBox turnInBox = lifecycle.editors().box("turnIn");
        if (titleBox != null) quest.setTitle(titleBox.getValue());
        if (descBox != null) quest.setDescription(descBox.getValue());
        if (giverBox != null) quest.setGiverNpcId(giverBox.getValue());
        if (turnInBox != null) quest.setTurnInNpcId(turnInBox.getValue());
        ModData.setDirty();
    }

    @Override
    public void init() {
        super.init();
        showAddObjectiveTypeMenu = false;
        showAddPrereqMenu = false;
        lifecycle.state().prereqMenuScroll = 0;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void removed() {
        super.removed();
    }

    private void giveQuest() {
        String selectedId = getSelectedId();
        if (selectedId == null) return;
        if (lifecycle.save().isDirty()) doSave();
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.GIVE_QUEST, ServerCommandPacket.giveQuestToTag(selectedId, "")));
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (w <= 0) return;
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BG_HOVER);
        int btnSize = 24;
        int btnY = y + 8;
        boolean giveHovered = mx >= x + 4 && mx <= x + w - 4 && my >= btnY && my <= btnY + btnSize;
        g.fill(x + 4, btnY, x + w - 4, btnY + btnSize, giveHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(x + 4, btnY, w - 8, btnSize, Theme.BORDER);
        g.drawCenteredString(this.font, "▶", x + w / 2, btnY + (btnSize - 8) / 2, giveHovered ? Theme.ACCENT : 0xFF44AA44);
        btnY += btnSize + 6;
        boolean addHovered = mx >= x + 4 && mx <= x + w - 4 && my >= btnY && my <= btnY + btnSize;
        g.fill(x + 4, btnY, x + w - 4, btnY + btnSize, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(x + 4, btnY, w - 8, btnSize, Theme.BORDER);
        g.drawCenteredString(this.font, "+", x + w / 2, btnY + (btnSize - 8) / 2, addHovered ? Theme.TEXT : Theme.TEXT_DIM);
    }

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        int toolbarX = rightX - getToolbarWidth();
        int btnSize = 24;
        int btnY = y + 8;
        if (mx >= toolbarX + 4 && mx <= toolbarX + getToolbarWidth() - 4 && my >= btnY && my <= btnY + btnSize) {
            giveQuest();
            return true;
        }
        btnY += btnSize + 6;
        if (mx >= toolbarX + 4 && mx <= toolbarX + getToolbarWidth() - 4 && my >= btnY && my <= btnY + btnSize) {
            openPromptDialog("create", null);
            return true;
        }
        return false;
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        String selectedId = getSelectedId();
        QuestData quest = selectedId != null ? DataAccess.quest(selectedId) : null;
        if (quest == null) return;

        int leftX = x;
        int leftY = y;
        int leftW = w;
        int leftH = h;

        EditBox titleBox = lifecycle.editors().box("title");
        MultiLineEditBox descBox = lifecycle.editors().multi("desc");
        EditBox giverBox = lifecycle.editors().box("giver");
        EditBox turnInBox = lifecycle.editors().box("turnIn");

        if (titleBox != null) {
            titleBox.setX(leftX + 4);
            titleBox.setY(leftY + 6);
            titleBox.setWidth(leftW - 8);
            titleBox.setVisible(true);
        }
        if (descBox != null) {
            descBox.setX(leftX + 4);
            descBox.setY(leftY + 28);
            descBox.setWidth(leftW - 8);
            descBox.setHeight(50);
            descBox.setVisible(true);
        }
        if (giverBox != null) {
            giverBox.setX(leftX + 4);
            giverBox.setY(leftY + 82);
            giverBox.setWidth(leftW - 8);
            giverBox.setVisible(true);
        }
        if (turnInBox != null) {
            turnInBox.setX(leftX + 4);
            turnInBox.setY(leftY + 106);
            turnInBox.setWidth(leftW - 8);
            turnInBox.setVisible(true);
        }

        if (this.minecraft != null) {
            double scale = this.minecraft.getWindow().getGuiScale();
            RenderSystem.enableScissor(
                    (int) (leftX * scale),
                    (int) ((this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * scale),
                    (int) (leftW * scale),
                    (int) (leftH * scale)
            );
        }

        int dy = leftY + 6 - editorScroll;

        g.drawString(font, I18n.s("iscript.quest.editor.label.title"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 22;
        g.drawString(font, I18n.s("iscript.quest.editor.label.description"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 56;
        g.drawString(font, I18n.s("iscript.quest.editor.label.giver_npc"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 22;
        g.drawString(font, I18n.s("iscript.quest.editor.label.turnin_npc"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 26;

        String[] tabs = {I18n.s("iscript.quest.editor.tab.stages"), I18n.s("iscript.quest.editor.tab.rewards"), I18n.s("iscript.quest.editor.tab.prereqs")};
        int tabW = leftW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tx = leftX + 4 + i * tabW;
            boolean th = mx >= tx && mx <= tx + tabW - 2 && my >= dy && my <= dy + 18;
            int tbg = editorTab == i ? 0xFF334455 : (th ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(tx, dy, tx + tabW - 2, dy + 18, tbg);
            g.renderOutline(tx, dy, tabW - 2, 18, Theme.BORDER);
            g.drawCenteredString(font, tabs[i], tx + tabW / 2 - 1, dy + 5, editorTab == i ? Theme.ACCENT : Theme.TEXT);
        }
        dy += 22;

        if (editorTab == 0) {
            dy = renderStages(g, quest, leftX, dy, leftW, mx, my);
        } else if (editorTab == 1) {
            dy = renderRewards(g, quest, leftX, dy, leftW, mx, my);
        } else if (editorTab == 2) {
            dy = renderPrerequisites(g, quest, leftX, dy, leftW, mx, my);
        }
        editorContentHeight = dy - (leftY + 6);

        RenderSystem.disableScissor();
    }

    private int renderStages(GuiGraphics g, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addHovered = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        g.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        g.drawCenteredString(font, I18n.s("iscript.quest.editor.add_stage"), leftX + leftW / 2, dy + 5, Theme.ACCENT);
        dy += 22;

        for (int s = 0; s < quest.getStages().size(); s++) {
            QuestStage stage = quest.getStages().get(s);
            boolean expanded = expandedStage == s;
            boolean sh = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 18;
            g.fill(leftX + 4, dy, leftX + leftW - 24, dy + 18, sh ? Theme.BORDER : Theme.BG_PANEL);
            g.renderOutline(leftX + 4, dy, leftW - 28, 18, Theme.BORDER);
            g.drawString(font, (expanded ? "v " : "> ") + stage.getId(), leftX + 8, dy + 5, expanded ? Theme.ACCENT : Theme.TEXT);

            boolean delH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
            g.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 18, delH ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(leftX + leftW - 22, dy, 18, 18, Theme.BORDER);
            g.drawCenteredString(font, "x", leftX + leftW - 13, dy + 5, delH ? Theme.ERROR : 0xFFAA4444);
            dy += 20;

            if (expanded) {
                if (!stage.getDescription().isEmpty()) {
                    g.drawString(font, stage.getDescription(), leftX + 12, dy, Theme.TEXT_MUTE);
                    dy += 12;
                }

                boolean addObjH = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                g.fill(leftX + 12, dy, leftX + leftW - 12, dy + 16, addObjH ? Theme.BG_HOVER : Theme.BG_INNER);
                g.renderOutline(leftX + 12, dy, leftW - 24, 16, Theme.BORDER);
                g.drawCenteredString(font, I18n.s("iscript.quest.editor.add_objective"), leftX + leftW / 2, dy + 4, Theme.ACCENT);
                dy += 18;

                for (int o = 0; o < stage.getObjectives().size(); o++) {
                    QuestObjective obj = stage.getObjectives().get(o);
                    boolean objExp = expandedObjective == o;
                    boolean oh = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 32 && mouseY >= dy && mouseY <= dy + 16;
                    g.fill(leftX + 12, dy, leftX + leftW - 32, dy + 16, oh ? Theme.BORDER : Theme.BG_PANEL);
                    g.renderOutline(leftX + 12, dy, leftW - 44, 16, Theme.BORDER);
                    String label = (objExp ? "v " : "> ") + obj.getType().name() + ": " + obj.getTarget() + " (" + obj.getCurrentCount() + "/" + obj.getRequiredCount() + ")";
                    g.drawString(font, label, leftX + 16, dy + 4, Theme.TEXT);

                    boolean objDelH = mouseX >= leftX + leftW - 30 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                    g.fill(leftX + leftW - 30, dy, leftX + leftW - 12, dy + 16, objDelH ? Theme.BG_HOVER : Theme.BG_INNER);
                    g.renderOutline(leftX + leftW - 30, dy, 18, 16, Theme.BORDER);
                    g.drawCenteredString(font, "x", leftX + leftW - 21, dy + 4, objDelH ? Theme.ERROR : 0xFFAA4444);
                    dy += 18;

                    if (objExp) {
                        if (!obj.getDescription().isEmpty()) {
                            g.drawString(font, obj.getDescription(), leftX + 20, dy, Theme.TEXT_MUTE);
                            dy += 12;
                        }
                        dy += 2;
                    }
                }
                dy += 4;
            }
        }
        return dy;
    }

    private int renderRewards(GuiGraphics g, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        g.drawString(font, I18n.s("iscript.quest.editor.label.title"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean titleH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        g.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, titleH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        String title = quest.getReward().getTitle();
        g.drawString(font, title.isEmpty() ? I18n.s("iscript.quest.editor.reward.none") : title, leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        g.drawString(font, I18n.s("iscript.quest.editor.reward.items"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;

        for (int i = 0; i < quest.getReward().getItems().size(); i++) {
            QuestReward.ItemReward item = quest.getReward().getItems().get(i);
            boolean ih = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            g.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ih ? Theme.BORDER : Theme.BG_PANEL);
            g.renderOutline(leftX + 4, dy, leftW - 28, 16, Theme.BORDER);
            g.drawString(font, item.getItemId() + " x" + item.getCount(), leftX + 8, dy + 4, Theme.TEXT);

            boolean idelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            g.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, idelH ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(leftX + leftW - 22, dy, 18, 16, Theme.BORDER);
            g.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, idelH ? Theme.ERROR : 0xFFAA4444);
            dy += 18;
        }

        boolean addItemH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
        g.fill(leftX + 4, dy, leftX + leftW - 4, dy + 16, addItemH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, leftW - 8, 16, Theme.BORDER);
        g.drawCenteredString(font, I18n.s("iscript.quest.editor.reward.add_item"), leftX + leftW / 2, dy + 4, Theme.ACCENT);
        dy += 20;

        g.drawString(font, I18n.s("iscript.quest.editor.reward.exp"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean expH = mouseX >= leftX + 4 && mouseX <= leftX + 80 && mouseY >= dy && mouseY <= dy + 18;
        g.fill(leftX + 4, dy, leftX + 80, dy + 18, expH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, 76, 18, Theme.BORDER);
        g.drawString(font, String.valueOf(quest.getReward().getExp()), leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        g.drawString(font, I18n.s("iscript.quest.editor.reward.command"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean cmdH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        g.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, cmdH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        String cmd = quest.getReward().getCommand();
        g.drawString(font, cmd.isEmpty() ? I18n.s("iscript.quest.editor.reward.none") : (cmd.length() > 30 ? cmd.substring(0, 30) + "..." : cmd), leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        return dy;
    }

    private int renderPrerequisites(GuiGraphics g, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        g.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        g.drawCenteredString(font, I18n.s("iscript.quest.editor.dialog.add_prereq"), leftX + leftW / 2, dy + 5, Theme.ACCENT);
        dy += 22;

        for (int i = 0; i < quest.getPrerequisites().size(); i++) {
            String prereq = quest.getPrerequisites().get(i);
            boolean ph = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            g.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ph ? Theme.BORDER : Theme.BG_PANEL);
            g.renderOutline(leftX + 4, dy, leftW - 28, 16, Theme.BORDER);
            g.drawString(font, prereq, leftX + 8, dy + 4, Theme.TEXT);

            boolean pdelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            g.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, pdelH ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(leftX + leftW - 22, dy, 18, 16, Theme.BORDER);
            g.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, pdelH ? Theme.ERROR : 0xFFAA4444);
            dy += 18;
        }
        return dy;
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0) return false;

        String selectedId = getSelectedId();
        QuestData quest = selectedId != null ? DataAccess.quest(selectedId) : null;
        if (quest == null) return false;

        int dy = leftY + 6 - editorScroll;

        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18) {
            return false;
        }
        dy += 22;

        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 50) {
            return false;
        }
        dy += 56;

        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18) {
            return false;
        }
        dy += 22;

        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18) {
            return false;
        }
        dy += 26;

        String[] tabs = {I18n.s("iscript.quest.editor.tab.stages"), I18n.s("iscript.quest.editor.tab.rewards"), I18n.s("iscript.quest.editor.tab.prereqs")};
        int tabW = leftW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tx = leftX + 4 + i * tabW;
            if (mx >= tx && mx <= tx + tabW - 2 && my >= dy && my <= dy + 18) {
                editorTab = i;
                return true;
            }
        }
        dy += 22;

        if (editorTab == 0) {
            boolean addStageH = mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18;
            if (addStageH) {
                openAddStageDialog();
                return true;
            }
            dy += 22;

            for (int s = 0; s < quest.getStages().size(); s++) {
                QuestStage stage = quest.getStages().get(s);
                boolean expanded = expandedStage == s;
                boolean sh = mx >= leftX + 4 && mx <= leftX + leftW - 24 && my >= dy && my <= dy + 18;
                boolean delH = mx >= leftX + leftW - 22 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18;
                if (delH) {
                    removeStage(s);
                    return true;
                }
                if (sh) {
                    expandedStage = expanded ? -1 : s;
                    expandedObjective = -1;
                    return true;
                }
                dy += 20;

                if (expanded) {
                    if (!stage.getDescription().isEmpty()) dy += 12;

                    boolean addObjH = mx >= leftX + 12 && mx <= leftX + leftW - 12 && my >= dy && my <= dy + 16;
                    if (addObjH) {
                        openAddObjectiveDialog(s);
                        return true;
                    }
                    dy += 18;

                    for (int o = 0; o < stage.getObjectives().size(); o++) {
                        boolean objExp = expandedObjective == o;
                        boolean oh = mx >= leftX + 12 && mx <= leftX + leftW - 32 && my >= dy && my <= dy + 16;
                        boolean objDelH = mx >= leftX + leftW - 30 && mx <= leftX + leftW - 12 && my >= dy && my <= dy + 16;
                        if (objDelH) {
                            removeObjective(s, o);
                            return true;
                        }
                        if (oh) {
                            expandedObjective = objExp ? -1 : o;
                            return true;
                        }
                        dy += 18;
                        if (objExp) {
                            if (!stage.getObjectives().get(o).getDescription().isEmpty()) dy += 12;
                            dy += 2;
                        }
                    }
                    dy += 4;
                }
            }
        } else if (editorTab == 1) {
            dy += 14;
            boolean titleH = mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18;
            if (titleH) {
                openEditTitleDialog();
                return true;
            }
            dy += 22;

            dy += 14;
            for (int i = 0; i < quest.getReward().getItems().size(); i++) {
                boolean idelH = mx >= leftX + leftW - 22 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 16;
                if (idelH) {
                    removeItemReward(i);
                    return true;
                }
                boolean itemH = mx >= leftX + 4 && mx <= leftX + leftW - 24 && my >= dy && my <= dy + 16;
                if (itemH) {
                    openEditItemCountDialog(i);
                    return true;
                }
                dy += 18;
            }
            boolean addItemH = mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 16;
            if (addItemH) {
                openItemPicker();
                return true;
            }
            dy += 20;

            dy += 14;
            boolean expH = mx >= leftX + 4 && mx <= leftX + 80 && my >= dy && my <= dy + 18;
            if (expH) {
                openEditExpDialog();
                return true;
            }
            dy += 22;

            dy += 14;
            boolean cmdH = mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18;
            if (cmdH) {
                openEditCommandDialog();
                return true;
            }
            dy += 22;
        } else if (editorTab == 2) {
            boolean addH = mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 18;
            if (addH) {
                openAddPrereqDialog();
                return true;
            }
            dy += 22;
            for (int i = 0; i < quest.getPrerequisites().size(); i++) {
                boolean pdelH = mx >= leftX + leftW - 22 && mx <= leftX + leftW - 4 && my >= dy && my <= dy + 16;
                if (pdelH) {
                    removePrereq(i);
                    return true;
                }
                dy += 18;
            }
        }

        EditBox titleBox = lifecycle.editors().box("title");
        MultiLineEditBox descBox = lifecycle.editors().multi("desc");
        EditBox giverBox = lifecycle.editors().box("giver");
        EditBox turnInBox = lifecycle.editors().box("turnIn");

        if (titleBox != null && titleBox.visible && mx >= titleBox.getX() && mx <= titleBox.getX() + titleBox.getWidth() && my >= titleBox.getY() && my <= titleBox.getY() + titleBox.getHeight()) {
            parent.setFocusedWidget(titleBox);
            return titleBox.mouseClicked(mx, my, button);
        }
        if (descBox != null && descBox.visible && mx >= descBox.getX() && mx <= descBox.getX() + descBox.getWidth() && my >= descBox.getY() && my <= descBox.getY() + descBox.getHeight()) {
            parent.setFocusedWidget(descBox);
            return descBox.mouseClicked(mx, my, button);
        }
        if (giverBox != null && giverBox.visible && mx >= giverBox.getX() && mx <= giverBox.getX() + giverBox.getWidth() && my >= giverBox.getY() && my <= giverBox.getY() + giverBox.getHeight()) {
            parent.setFocusedWidget(giverBox);
            return giverBox.mouseClicked(mx, my, button);
        }
        if (turnInBox != null && turnInBox.visible && mx >= turnInBox.getX() && mx <= turnInBox.getX() + turnInBox.getWidth() && my >= turnInBox.getY() && my <= turnInBox.getY() + turnInBox.getHeight()) {
            parent.setFocusedWidget(turnInBox);
            return turnInBox.mouseClicked(mx, my, button);
        }

        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleEditorScroll(double mx, double my, double delta, int leftX, int leftY, int leftW, int leftH) {
        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            int maxEditorScroll = Math.max(0, editorContentHeight - leftH);
            if (delta > 0) editorScroll = Math.max(0, editorScroll - 20);
            else editorScroll = Math.min(editorScroll + 20, maxEditorScroll);
            return true;
        }
        return false;
    }

    @Override
    protected boolean hasCustomModals() {
        return showAddObjectiveTypeMenu || showAddPrereqMenu ||
                lifecycle.modals().isOpen("addStage") ||
                lifecycle.modals().isOpen("addObjective") ||
                lifecycle.modals().isOpen("addPrereq") ||
                lifecycle.modals().isOpen("addItemReward") ||
                lifecycle.modals().isOpen("itemPicker") ||
                lifecycle.modals().isOpen("editExp") ||
                lifecycle.modals().isOpen("editCmd") ||
                lifecycle.modals().isOpen("editTitle") ||
                lifecycle.modals().isOpen("editItemCount");
    }

    @Override
    protected void renderCustomModals(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (lifecycle.modals().isOpen("addStage")) renderAddStageDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("addObjective")) renderAddObjectiveDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("addPrereq")) renderAddPrereqDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("addItemReward")) renderAddItemRewardDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("itemPicker")) renderItemPicker(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("editExp")) renderEditExpDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("editCmd")) renderEditCommandDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("editTitle")) renderEditTitleDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("editItemCount")) renderEditItemCountDialog(g, x, w, mx, my);
        if (showAddObjectiveTypeMenu) renderObjectiveTypeMenu(g, mx, my);
        if (showAddPrereqMenu) renderPrereqMenu(g, mx, my);
    }

    private void renderAddStageDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addStageY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.new_stage"), cx, dy + 6, Theme.ACCENT);

        EditBox stageIdBox = lifecycle.editors().box("stageId");
        if (stageIdBox != null) { stageIdBox.setX(cx - 100); stageIdBox.setY(dy + 24); }
        EditBox stageDescBox = lifecycle.editors().box("stageDesc");
        if (stageDescBox != null) { stageDescBox.setX(cx - 100); stageDescBox.setY(dy + 52); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        g.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 74, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 79, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        g.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 74, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 79, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddObjectiveDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 140;
        int dx = cx - dw / 2;
        int dy = addObjectiveY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.new_objective"), cx, dy + 6, Theme.ACCENT);

        QuestObjectiveType[] types = QuestObjectiveType.values();
        boolean typeH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        g.fill(cx - 100, dy + 24, cx + 100, dy + 42, typeH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 100, dy + 24, 200, 18, Theme.BORDER);
        g.drawCenteredString(font, types[objectiveTypeIndex].name(), cx, dy + 28, Theme.TEXT);

        EditBox objTargetBox = lifecycle.editors().box("objTarget");
        if (objTargetBox != null) { objTargetBox.setX(cx - 100); objTargetBox.setY(dy + 46); }
        EditBox objCountBox = lifecycle.editors().box("objCount");
        if (objCountBox != null) { objCountBox.setX(cx - 100); objCountBox.setY(dy + 70); }
        EditBox objDescBox = lifecycle.editors().box("objDesc");
        if (objDescBox != null) { objDescBox.setX(cx - 100); objDescBox.setY(dy + 94); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 116 && mouseY <= dy + 138;
        g.fill(cx - 50, dy + 116, cx - 2, dy + 138, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 116, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 121, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 116 && mouseY <= dy + 138;
        g.fill(cx + 2, dy + 116, cx + 50, dy + 138, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 116, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 121, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddPrereqDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = addPrereqY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.add_prereq"), cx, dy + 6, Theme.ACCENT);

        List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(ids);
        String current = this.prereqDropdownIndex >= 0 && this.prereqDropdownIndex < ids.size() ? ids.get(this.prereqDropdownIndex) : "None";
        boolean dropH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        g.fill(cx - 100, dy + 24, cx + 100, dy + 42, dropH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 100, dy + 24, 200, 18, Theme.BORDER);
        g.drawCenteredString(font, current, cx, dy + 28, Theme.TEXT);

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddItemRewardDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addItemRewardY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.add_item_reward"), cx, dy + 6, Theme.ACCENT);

        EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
        if (rewardItemBox != null) { rewardItemBox.setX(cx - 100); rewardItemBox.setY(dy + 24); }
        EditBox rewardItemCountBox = lifecycle.editors().box("rewardCount");
        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 48); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        g.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 74, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 79, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        g.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 74, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 79, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderItemPicker(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 200;
        int dx = cx - dw / 2;
        int dy = itemPickerY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.select_item"), cx, dy + 6, Theme.ACCENT);

        EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
        if (itemPickerSearchBox != null) { itemPickerSearchBox.setX(cx - 100); itemPickerSearchBox.setY(dy + 20); }

        int listY = dy + 44;
        int listH = 108;
        int scrollBarW = 6;
        int listX = dx + 4;
        int listW = dw - 8 - scrollBarW;

        g.fill(listX, listY, listX + listW, listY + listH, Theme.BG_INNER);
        g.renderOutline(listX, listY, listW, listH, Theme.BORDER);

        if (this.minecraft != null) {
            double scale = this.minecraft.getWindow().getGuiScale();
            RenderSystem.enableScissor(
                    (int) (listX * scale),
                    (int) ((this.minecraft.getWindow().getGuiScaledHeight() - listY - listH) * scale),
                    (int) (listW * scale),
                    (int) (listH * scale)
            );
        }

        int rowH = 18;
        int visibleRows = listH / rowH;
        for (int i = itemPickerScroll; i < Math.min(itemPickerScroll + visibleRows, itemPickerFiltered.size()); i++) {
            var item = itemPickerFiltered.get(i);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            boolean selected = id.equals(itemPickerSelectedId);
            int ry = listY + (i - itemPickerScroll) * rowH;
            boolean h = mouseX >= listX && mouseX <= listX + listW && mouseY >= ry && mouseY <= ry + rowH;
            int bg = selected ? 0xFF334455 : (h ? Theme.BORDER : Theme.BG_INNER);
            g.fill(listX + 1, ry, listX + listW - 1, ry + rowH, bg);
            g.renderItem(new ItemStack(item), listX + 4, ry + 1);
            g.drawString(font, item.getDescription().getString(), listX + 22, ry + 5, selected ? Theme.ACCENT : Theme.TEXT);
        }

        RenderSystem.disableScissor();

        int scrollX = listX + listW;
        g.fill(scrollX, listY, scrollX + scrollBarW, listY + listH, Theme.BG_INNER);
        g.renderOutline(scrollX, listY, scrollBarW, listH, Theme.BORDER);

        int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (visibleRows * listH) / itemPickerFiltered.size());
            int thumbY = listY + (itemPickerScroll * (listH - thumbH)) / maxScroll;
            g.fill(scrollX, thumbY, scrollX + scrollBarW, thumbY + thumbH, Theme.TEXT_DIM);
        }

        EditBox rewardItemCountBox = lifecycle.editors().box("itemPickerCount");
        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 156); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 180 && mouseY <= dy + 198;
        g.fill(cx - 50, dy + 180, cx - 2, dy + 198, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 180, 48, 18, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 183, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 180 && mouseY <= dy + 198;
        g.fill(cx + 2, dy + 180, cx + 50, dy + 198, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 180, 48, 18, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 183, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditExpDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editExpY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_exp"), cx, dy + 6, Theme.ACCENT);

        EditBox editExpBox = lifecycle.editors().box("editExp");
        if (editExpBox != null) { editExpBox.setX(cx - 100); editExpBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditCommandDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editCmdY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_command"), cx, dy + 6, Theme.ACCENT);

        EditBox editCmdBox = lifecycle.editors().box("editCmd");
        if (editCmdBox != null) { editCmdBox.setX(cx - 100); editCmdBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditTitleDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editTitleY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_reward_title"), cx, dy + 6, Theme.ACCENT);

        EditBox editTitleBox = lifecycle.editors().box("editTitle");
        if (editTitleBox != null) { editTitleBox.setX(cx - 100); editTitleBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditItemCountDialog(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editItemCountY;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_item_count"), cx, dy + 6, Theme.ACCENT);

        EditBox editItemCountBox = lifecycle.editors().box("editItemCount");
        if (editItemCountBox != null) { editItemCountBox.setX(cx - 100); editItemCountBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderObjectiveTypeMenu(GuiGraphics g, int mouseX, int mouseY) {
        QuestObjectiveType[] types = QuestObjectiveType.values();
        int mw = 120;
        int mh = types.length * 20 + 4;
        g.fill(objTypeMenuX, objTypeMenuY, objTypeMenuX + mw, objTypeMenuY + mh, Theme.BG_INNER);
        g.renderOutline(objTypeMenuX, objTypeMenuY, mw, mh, Theme.BORDER);
        int cy = objTypeMenuY + 2;
        for (int i = 0; i < types.length; i++) {
            boolean h = mouseX >= objTypeMenuX && mouseX <= objTypeMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            g.fill(objTypeMenuX + 1, cy, objTypeMenuX + mw - 1, cy + 18, h ? Theme.BG_HOVER : Theme.BG_INNER);
            g.drawString(font, types[i].name(), objTypeMenuX + 6, cy + 5, h ? Theme.TEXT : Theme.TEXT);
            cy += 20;
        }
    }

    private void renderPrereqMenu(GuiGraphics g, int mouseX, int mouseY) {
        List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(ids);
        int mw = 140;
        int visible = Math.min(ids.size() - lifecycle.state().prereqMenuScroll, 10);
        int mh = visible * 20 + 4;
        g.fill(prereqMenuX, prereqMenuY, prereqMenuX + mw, prereqMenuY + mh, Theme.BG_INNER);
        g.renderOutline(prereqMenuX, prereqMenuY, mw, mh, Theme.BORDER);
        int cy = prereqMenuY + 2;
        for (int i = lifecycle.state().prereqMenuScroll; i < Math.min(ids.size(), lifecycle.state().prereqMenuScroll + 10); i++) {
            boolean h = mouseX >= prereqMenuX && mouseX <= prereqMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            g.fill(prereqMenuX + 1, cy, prereqMenuX + mw - 1, cy + 18, h ? Theme.BG_HOVER : Theme.BG_INNER);
            g.drawString(font, ids.get(i), prereqMenuX + 6, cy + 5, h ? Theme.TEXT : Theme.TEXT);
            cy += 20;
        }
    }

    @Override
    protected boolean handleCustomModalClick(double mx, double my, int button) {
        if (lifecycle.modals().isOpen("addStage")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addStageY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 74 && my <= dy + 96) { confirmAddStage(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 74 && my <= dy + 96) { closeAddStageDialog(); return true; }
            EditBox stageIdBox = lifecycle.editors().box("stageId");
            if (stageIdBox != null && mx >= stageIdBox.getX() && mx <= stageIdBox.getX() + stageIdBox.getWidth() && my >= stageIdBox.getY() && my <= stageIdBox.getY() + stageIdBox.getHeight()) {
                parent.setFocusedWidget(stageIdBox); return stageIdBox.mouseClicked(mx, my, button);
            }
            EditBox stageDescBox = lifecycle.editors().box("stageDesc");
            if (stageDescBox != null && mx >= stageDescBox.getX() && mx <= stageDescBox.getX() + stageDescBox.getWidth() && my >= stageDescBox.getY() && my <= stageDescBox.getY() + stageDescBox.getHeight()) {
                parent.setFocusedWidget(stageDescBox); return stageDescBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("addObjective")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addObjectiveY;
            if (mx >= cx - 100 && mx <= cx + 100 && my >= dy + 24 && my <= dy + 42) {
                showAddObjectiveTypeMenu = true; objTypeMenuX = (int) mx; objTypeMenuY = (int) my; return true;
            }
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 116 && my <= dy + 138) { confirmAddObjective(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 116 && my <= dy + 138) { closeAddObjectiveDialog(); return true; }
            EditBox objTargetBox = lifecycle.editors().box("objTarget");
            if (objTargetBox != null && mx >= objTargetBox.getX() && mx <= objTargetBox.getX() + objTargetBox.getWidth() && my >= objTargetBox.getY() && my <= objTargetBox.getY() + objTargetBox.getHeight()) {
                parent.setFocusedWidget(objTargetBox); return objTargetBox.mouseClicked(mx, my, button);
            }
            EditBox objCountBox = lifecycle.editors().box("objCount");
            if (objCountBox != null && mx >= objCountBox.getX() && mx <= objCountBox.getX() + objCountBox.getWidth() && my >= objCountBox.getY() && my <= objCountBox.getY() + objCountBox.getHeight()) {
                parent.setFocusedWidget(objCountBox); return objCountBox.mouseClicked(mx, my, button);
            }
            EditBox objDescBox = lifecycle.editors().box("objDesc");
            if (objDescBox != null && mx >= objDescBox.getX() && mx <= objDescBox.getX() + objDescBox.getWidth() && my >= objDescBox.getY() && my <= objDescBox.getY() + objDescBox.getHeight()) {
                parent.setFocusedWidget(objDescBox); return objDescBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("addPrereq")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addPrereqY;
            if (mx >= cx - 100 && mx <= cx + 100 && my >= dy + 24 && my <= dy + 42) {
                showAddPrereqMenu = true; prereqMenuX = (int) mx; prereqMenuY = (int) my; return true;
            }
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmAddPrereq(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closeAddPrereqDialog(); return true; }
            return true;
        }

        if (lifecycle.modals().isOpen("addItemReward")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addItemRewardY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 74 && my <= dy + 96) { confirmAddItemReward(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 74 && my <= dy + 96) { closeAddItemRewardDialog(); return true; }
            EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
            if (rewardItemBox != null && mx >= rewardItemBox.getX() && mx <= rewardItemBox.getX() + rewardItemBox.getWidth() && my >= rewardItemBox.getY() && my <= rewardItemBox.getY() + rewardItemBox.getHeight()) {
                parent.setFocusedWidget(rewardItemBox); return rewardItemBox.mouseClicked(mx, my, button);
            }
            EditBox rewardItemCountBox = lifecycle.editors().box("rewardCount");
            if (rewardItemCountBox != null && mx >= rewardItemCountBox.getX() && mx <= rewardItemCountBox.getX() + rewardItemCountBox.getWidth() && my >= rewardItemCountBox.getY() && my <= rewardItemCountBox.getY() + rewardItemCountBox.getHeight()) {
                parent.setFocusedWidget(rewardItemCountBox); return rewardItemCountBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("itemPicker")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = itemPickerY;
            int dw = 220;
            int dx = cx - dw / 2;
            int listY = dy + 44;
            int listH = 108;
            int scrollBarW = 6;
            int listX = dx + 4;
            int listW = dw - 8 - scrollBarW;
            int rowH = 18;
            int visibleRows = listH / rowH;
            int scrollX = listX + listW;

            for (int i = itemPickerScroll; i < Math.min(itemPickerScroll + visibleRows, itemPickerFiltered.size()); i++) {
                int ry = listY + (i - itemPickerScroll) * rowH;
                if (mx >= listX && mx <= listX + listW && my >= ry && my <= ry + rowH) {
                    var item = itemPickerFiltered.get(i);
                    itemPickerSelectedId = BuiltInRegistries.ITEM.getKey(item).toString();
                    return true;
                }
            }

            int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
            if (maxScroll > 0 && mx >= scrollX && mx <= scrollX + scrollBarW && my >= listY && my <= listY + listH) {
                int thumbH = Math.max(10, (visibleRows * listH) / itemPickerFiltered.size());
                int trackH = listH - thumbH;
                double ratio = (my - listY) / (double) trackH;
                itemPickerScroll = (int) Math.round(Math.max(0, Math.min(maxScroll, ratio * maxScroll)));
                return true;
            }

            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 180 && my <= dy + 198) { confirmItemPicker(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 180 && my <= dy + 198) { closeItemPicker(); return true; }
            EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && mx >= itemPickerSearchBox.getX() && mx <= itemPickerSearchBox.getX() + itemPickerSearchBox.getWidth() && my >= itemPickerSearchBox.getY() && my <= itemPickerSearchBox.getY() + itemPickerSearchBox.getHeight()) {
                parent.setFocusedWidget(itemPickerSearchBox); return itemPickerSearchBox.mouseClicked(mx, my, button);
            }
            EditBox pickerCountBox = lifecycle.editors().box("itemPickerCount");
            if (pickerCountBox != null && mx >= pickerCountBox.getX() && mx <= pickerCountBox.getX() + pickerCountBox.getWidth() && my >= pickerCountBox.getY() && my <= pickerCountBox.getY() + pickerCountBox.getHeight()) {
                parent.setFocusedWidget(pickerCountBox); return pickerCountBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("editExp")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editExpY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmEditExp(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closeEditExpDialog(); return true; }
            EditBox editExpBox = lifecycle.editors().box("editExp");
            if (editExpBox != null && mx >= editExpBox.getX() && mx <= editExpBox.getX() + editExpBox.getWidth() && my >= editExpBox.getY() && my <= editExpBox.getY() + editExpBox.getHeight()) {
                parent.setFocusedWidget(editExpBox); return editExpBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("editCmd")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editCmdY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmEditCommand(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closeEditCommandDialog(); return true; }
            EditBox editCmdBox = lifecycle.editors().box("editCmd");
            if (editCmdBox != null && mx >= editCmdBox.getX() && mx <= editCmdBox.getX() + editCmdBox.getWidth() && my >= editCmdBox.getY() && my <= editCmdBox.getY() + editCmdBox.getHeight()) {
                parent.setFocusedWidget(editCmdBox); return editCmdBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("editTitle")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editTitleY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmEditTitle(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closeEditTitleDialog(); return true; }
            EditBox editTitleBox = lifecycle.editors().box("editTitle");
            if (editTitleBox != null && mx >= editTitleBox.getX() && mx <= editTitleBox.getX() + editTitleBox.getWidth() && my >= editTitleBox.getY() && my <= editTitleBox.getY() + editTitleBox.getHeight()) {
                parent.setFocusedWidget(editTitleBox); return editTitleBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("editItemCount")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editItemCountY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmEditItemCount(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closeEditItemCountDialog(); return true; }
            EditBox editItemCountBox = lifecycle.editors().box("editItemCount");
            if (editItemCountBox != null && mx >= editItemCountBox.getX() && mx <= editItemCountBox.getX() + editItemCountBox.getWidth() && my >= editItemCountBox.getY() && my <= editItemCountBox.getY() + editItemCountBox.getHeight()) {
                parent.setFocusedWidget(editItemCountBox); return editItemCountBox.mouseClicked(mx, my, button);
            }
            return true;
        }

        if (showAddObjectiveTypeMenu) {
            QuestObjectiveType[] types = QuestObjectiveType.values();
            int mw = 120;
            int cy = objTypeMenuY + 2;
            for (int i = 0; i < types.length; i++) {
                if (mx >= objTypeMenuX && mx <= objTypeMenuX + mw && my >= cy && my <= cy + 18) {
                    objectiveTypeIndex = i; showAddObjectiveTypeMenu = false; return true;
                }
                cy += 20;
            }
            showAddObjectiveTypeMenu = false; return true;
        }

        if (showAddPrereqMenu) {
            List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
            Collections.sort(ids);
            int mw = 140;
            int cy = prereqMenuY + 2;
            for (int i = lifecycle.state().prereqMenuScroll; i < Math.min(ids.size(), lifecycle.state().prereqMenuScroll + 10); i++) {
                if (mx >= prereqMenuX && mx <= prereqMenuX + mw && my >= cy && my <= cy + 18) {
                    prereqDropdownIndex = i; showAddPrereqMenu = false; return true;
                }
                cy += 20;
            }
            showAddPrereqMenu = false; return true;
        }

        return false;
    }

    @Override
    protected boolean handleCustomModalKey(int keyCode, int scanCode, int modifiers) {
        if (lifecycle.modals().isOpen("addStage")) {
            if (keyCode == 257 || keyCode == 335) { confirmAddStage(); return true; }
            if (keyCode == 256) { closeAddStageDialog(); return true; }
            EditBox stageIdBox = lifecycle.editors().box("stageId");
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox stageDescBox = lifecycle.editors().box("stageDesc");
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("addObjective")) {
            if (keyCode == 257 || keyCode == 335) { confirmAddObjective(); return true; }
            if (keyCode == 256) { closeAddObjectiveDialog(); return true; }
            EditBox objTargetBox = lifecycle.editors().box("objTarget");
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox objCountBox = lifecycle.editors().box("objCount");
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox objDescBox = lifecycle.editors().box("objDesc");
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("addPrereq")) {
            if (keyCode == 257 || keyCode == 335) { confirmAddPrereq(); return true; }
            if (keyCode == 256) { closeAddPrereqDialog(); return true; }
            return true;
        }
        if (lifecycle.modals().isOpen("addItemReward")) {
            if (keyCode == 257 || keyCode == 335) { confirmAddItemReward(); return true; }
            if (keyCode == 256) { closeAddItemRewardDialog(); return true; }
            EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox rewardItemCountBox = lifecycle.editors().box("rewardCount");
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("itemPicker")) {
            if (keyCode == 257 || keyCode == 335) { confirmItemPicker(); return true; }
            if (keyCode == 256) { closeItemPicker(); return true; }
            EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox pickerCountBox = lifecycle.editors().box("itemPickerCount");
            if (pickerCountBox != null && pickerCountBox.isFocused()) return pickerCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editExp")) {
            if (keyCode == 257 || keyCode == 335) { confirmEditExp(); return true; }
            if (keyCode == 256) { closeEditExpDialog(); return true; }
            EditBox editExpBox = lifecycle.editors().box("editExp");
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editCmd")) {
            if (keyCode == 257 || keyCode == 335) { confirmEditCommand(); return true; }
            if (keyCode == 256) { closeEditCommandDialog(); return true; }
            EditBox editCmdBox = lifecycle.editors().box("editCmd");
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editTitle")) {
            if (keyCode == 257 || keyCode == 335) { confirmEditTitle(); return true; }
            if (keyCode == 256) { closeEditTitleDialog(); return true; }
            EditBox editTitleBox = lifecycle.editors().box("editTitle");
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editItemCount")) {
            if (keyCode == 257 || keyCode == 335) { confirmEditItemCount(); return true; }
            if (keyCode == 256) { closeEditItemCountDialog(); return true; }
            EditBox editItemCountBox = lifecycle.editors().box("editItemCount");
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleCustomModalChar(char codePoint, int modifiers) {
        if (lifecycle.modals().isOpen("addStage")) {
            EditBox stageIdBox = lifecycle.editors().box("stageId");
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.charTyped(codePoint, modifiers);
            EditBox stageDescBox = lifecycle.editors().box("stageDesc");
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("addObjective")) {
            EditBox objTargetBox = lifecycle.editors().box("objTarget");
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.charTyped(codePoint, modifiers);
            EditBox objCountBox = lifecycle.editors().box("objCount");
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.charTyped(codePoint, modifiers);
            EditBox objDescBox = lifecycle.editors().box("objDesc");
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("addItemReward")) {
            EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.charTyped(codePoint, modifiers);
            EditBox rewardItemCountBox = lifecycle.editors().box("rewardCount");
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("itemPicker")) {
            EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.charTyped(codePoint, modifiers);
            EditBox pickerCountBox = lifecycle.editors().box("itemPickerCount");
            if (pickerCountBox != null && pickerCountBox.isFocused()) return pickerCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editExp")) {
            EditBox editExpBox = lifecycle.editors().box("editExp");
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editCmd")) {
            EditBox editCmdBox = lifecycle.editors().box("editCmd");
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editTitle")) {
            EditBox editTitleBox = lifecycle.editors().box("editTitle");
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (lifecycle.modals().isOpen("editItemCount")) {
            EditBox editItemCountBox = lifecycle.editors().box("editItemCount");
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleCustomModalScroll(double mx, double my, double delta) {
        if (lifecycle.modals().isOpen("itemPicker")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dw = 220;
            int dx = cx - dw / 2;
            int listY = itemPickerY + 44;
            int listH = 108;
            if (mx >= dx + 4 && mx <= dx + dw - 4 && my >= listY && my <= listY + listH) {
                int visibleRows = listH / 18;
                int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
                if (delta > 0) itemPickerScroll = Math.max(0, itemPickerScroll - 1);
                else itemPickerScroll = Math.min(itemPickerScroll + 1, maxScroll);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleCustomModalRelease(double mx, double my, int button) {
        return false;
    }

    private void openAddStageDialog() {
        lifecycle.modals().open("addStage");
        addStageY = this.parent.height / 2 - 40;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        lifecycle.editors().addBox("stageId", cx - 100, addStageY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.stage_id"));
        EditBox stageIdBox = lifecycle.editors().box("stageId");
        if (stageIdBox != null) stageIdBox.setMaxLength(64);
        lifecycle.editors().addBox("stageDesc", cx - 100, addStageY + 48, 200, 20, I18n.t("iscript.quest.editor.placeholder.description"));
        EditBox stageDescBox = lifecycle.editors().box("stageDesc");
        if (stageDescBox != null) stageDescBox.setMaxLength(128);
        parent.setFocusedWidget(stageIdBox);
    }

    private void closeAddStageDialog() {
        lifecycle.modals().close("addStage");
        lifecycle.editors().remove("stageId");
        lifecycle.editors().remove("stageDesc");
    }

    private void confirmAddStage() {
        EditBox stageIdBox = lifecycle.editors().box("stageId");
        if (stageIdBox == null) { closeAddStageDialog(); return; }
        String id = stageIdBox.getValue().trim();
        EditBox stageDescBox = lifecycle.editors().box("stageDesc");
        String desc = stageDescBox != null ? stageDescBox.getValue().trim() : "";
        closeAddStageDialog();
        if (id.isEmpty()) return;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestStage stage = new QuestStage();
        stage.setId(id);
        stage.setDescription(desc);
        quest.getStages().add(stage);
        ModData.setDirty();
    }

    private void removeStage(int idx) {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getStages().size()) return;
        quest.getStages().remove(idx);
        if (expandedStage == idx) expandedStage = -1;
        else if (expandedStage > idx) expandedStage--;
        ModData.setDirty();
    }

    private void openAddObjectiveDialog(int stageIdx) {
        lifecycle.modals().open("addObjective");
        addObjectiveStageIdx = stageIdx;
        addObjectiveY = this.parent.height / 2 - 50;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        objectiveTypeIndex = 0;
        lifecycle.editors().addBox("objTarget", cx - 100, addObjectiveY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.target"));
        EditBox objTargetBox = lifecycle.editors().box("objTarget");
        if (objTargetBox != null) objTargetBox.setMaxLength(128);
        lifecycle.editors().addNumericBox("objCount", cx - 100, addObjectiveY + 46, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        lifecycle.editors().addBox("objDesc", cx - 100, addObjectiveY + 72, 200, 20, I18n.t("iscript.quest.editor.placeholder.description"));
        EditBox objDescBox = lifecycle.editors().box("objDesc");
        if (objDescBox != null) objDescBox.setMaxLength(128);
        parent.setFocusedWidget(objTargetBox);
    }

    private void closeAddObjectiveDialog() {
        lifecycle.modals().close("addObjective");
        addObjectiveStageIdx = -1;
        lifecycle.editors().remove("objTarget");
        lifecycle.editors().remove("objCount");
        lifecycle.editors().remove("objDesc");
    }

    private void confirmAddObjective() {
        EditBox objTargetBox = lifecycle.editors().box("objTarget");
        if (objTargetBox == null || addObjectiveStageIdx < 0) { closeAddObjectiveDialog(); return; }
        String target = objTargetBox.getValue().trim();
        int count = 1;
        EditBox objCountBox = lifecycle.editors().box("objCount");
        try { count = Integer.parseInt(objCountBox.getValue()); } catch (NumberFormatException ignored) {}
        EditBox objDescBox = lifecycle.editors().box("objDesc");
        String desc = objDescBox != null ? objDescBox.getValue().trim() : "";
        int stageIdx = addObjectiveStageIdx;
        closeAddObjectiveDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || stageIdx >= quest.getStages().size()) return;
        QuestObjective obj = new QuestObjective();
        obj.setType(QuestObjectiveType.values()[objectiveTypeIndex]);
        obj.setTarget(target);
        obj.setRequiredCount(count);
        obj.setDescription(desc);
        quest.getStages().get(stageIdx).getObjectives().add(obj);
        ModData.setDirty();
    }

    private void removeObjective(int stageIdx, int objIdx) {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || stageIdx < 0 || stageIdx >= quest.getStages().size()) return;
        List<QuestObjective> objs = quest.getStages().get(stageIdx).getObjectives();
        if (objIdx < 0 || objIdx >= objs.size()) return;
        objs.remove(objIdx);
        if (expandedObjective == objIdx) expandedObjective = -1;
        else if (expandedObjective > objIdx) expandedObjective--;
        ModData.setDirty();
    }

    private void openAddPrereqDialog() {
        lifecycle.modals().open("addPrereq");
        addPrereqY = this.parent.height / 2 - 30;
        prereqDropdownIndex = 0;
    }

    private void closeAddPrereqDialog() {
        lifecycle.modals().close("addPrereq");
    }

    private void confirmAddPrereq() {
        closeAddPrereqDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        List<String> allIds = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(allIds);
        if (prereqDropdownIndex >= 0 && prereqDropdownIndex < allIds.size()) {
            String id = allIds.get(prereqDropdownIndex);
            if (!id.equals(selectedId) && !quest.getPrerequisites().contains(id)) {
                quest.getPrerequisites().add(id);
                ModData.setDirty();
            }
        }
    }

    private void removePrereq(int idx) {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getPrerequisites().size()) return;
        quest.getPrerequisites().remove(idx);
        ModData.setDirty();
    }

    private void openAddItemRewardDialog() {
        lifecycle.modals().open("addItemReward");
        addItemRewardY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        lifecycle.editors().addBox("rewardItem", cx - 100, addItemRewardY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.item_id"));
        EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
        if (rewardItemBox != null) rewardItemBox.setMaxLength(128);
        lifecycle.editors().addNumericBox("rewardCount", cx - 100, addItemRewardY + 46, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        parent.setFocusedWidget(rewardItemBox);
    }

    private void closeAddItemRewardDialog() {
        lifecycle.modals().close("addItemReward");
        lifecycle.editors().remove("rewardItem");
        lifecycle.editors().remove("rewardCount");
    }

    private void confirmAddItemReward() {
        EditBox rewardItemBox = lifecycle.editors().box("rewardItem");
        if (rewardItemBox == null) { closeAddItemRewardDialog(); return; }
        String itemId = rewardItemBox.getValue().trim();
        int count = 1;
        EditBox rewardItemCountBox = lifecycle.editors().box("rewardCount");
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeAddItemRewardDialog();
        if (itemId.isEmpty()) return;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        ModData.setDirty();
    }

    private void removeItemReward(int idx) {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().remove(idx);
        ModData.setDirty();
    }

    private void removeRewardExp() {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setExp(0);
        ModData.setDirty();
    }

    private void removeRewardCommand() {
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setCommand("");
        ModData.setDirty();
    }

    private void openItemPicker() {
        lifecycle.modals().open("itemPicker");
        itemPickerY = this.parent.height / 2 - 100;
        itemPickerSelectedId = "";
        itemPickerScroll = 0;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        lifecycle.editors().addBox("itemPickerSearch", cx - 100, itemPickerY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.search"));
        EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.setMaxLength(128);
            itemPickerSearchBox.setValue("");
            itemPickerSearchBox.setResponder(s -> {
                itemPickerScroll = 0;
                filterItemPicker();
            });
        }
        parent.setFocusedWidget(itemPickerSearchBox);
        lifecycle.editors().addNumericBox("itemPickerCount", cx - 100, itemPickerY + 156, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        filterItemPicker();
    }

    private void filterItemPicker() {
        itemPickerFiltered.clear();
        EditBox itemPickerSearchBox = lifecycle.editors().box("itemPickerSearch");
        String query = itemPickerSearchBox != null ? itemPickerSearchBox.getValue().toLowerCase() : "";
        for (var item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            String name = item.getDescription().getString().toLowerCase();
            if (query.isEmpty() || id.contains(query) || name.contains(query)) {
                itemPickerFiltered.add(item);
            }
        }
    }

    private void closeItemPicker() {
        lifecycle.modals().close("itemPicker");
        itemPickerSelectedId = "";
        lifecycle.editors().remove("itemPickerSearch");
        lifecycle.editors().remove("itemPickerCount");
    }

    private void confirmItemPicker() {
        if (itemPickerSelectedId.isEmpty()) { closeItemPicker(); return; }
        int count = 1;
        EditBox rewardItemCountBox = lifecycle.editors().box("itemPickerCount");
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeItemPicker();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemPickerSelectedId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        ModData.setDirty();
    }

    private void openEditExpDialog() {
        lifecycle.modals().open("editExp");
        editExpY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        lifecycle.editors().addNumericBox("editExp", cx - 100, editExpY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.exp"), String.valueOf(quest != null ? quest.getReward().getExp() : 0));
        parent.setFocusedWidget(lifecycle.editors().box("editExp"));
    }

    private void closeEditExpDialog() {
        lifecycle.modals().close("editExp");
        lifecycle.editors().remove("editExp");
    }

    private void confirmEditExp() {
        EditBox editExpBox = lifecycle.editors().box("editExp");
        if (editExpBox == null) { closeEditExpDialog(); return; }
        int exp = 0;
        try { exp = Integer.parseInt(editExpBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditExpDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setExp(exp);
        ModData.setDirty();
    }

    private void openEditCommandDialog() {
        lifecycle.modals().open("editCmd");
        editCmdY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        lifecycle.editors().addBox("editCmd", cx - 100, editCmdY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.command"), quest != null ? quest.getReward().getCommand() : "");
        EditBox editCmdBox = lifecycle.editors().box("editCmd");
        if (editCmdBox != null) editCmdBox.setMaxLength(256);
        parent.setFocusedWidget(editCmdBox);
    }

    private void closeEditCommandDialog() {
        lifecycle.modals().close("editCmd");
        lifecycle.editors().remove("editCmd");
    }

    private void confirmEditCommand() {
        EditBox editCmdBox = lifecycle.editors().box("editCmd");
        if (editCmdBox == null) { closeEditCommandDialog(); return; }
        String cmd = editCmdBox.getValue();
        closeEditCommandDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setCommand(cmd);
        ModData.setDirty();
    }

    private void openEditTitleDialog() {
        lifecycle.modals().open("editTitle");
        editTitleY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        lifecycle.editors().addBox("editTitle", cx - 100, editTitleY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.reward_title"), quest != null ? quest.getReward().getTitle() : "");
        EditBox editTitleBox = lifecycle.editors().box("editTitle");
        if (editTitleBox != null) editTitleBox.setMaxLength(128);
        parent.setFocusedWidget(editTitleBox);
    }

    private void closeEditTitleDialog() {
        lifecycle.modals().close("editTitle");
        lifecycle.editors().remove("editTitle");
    }

    private void confirmEditTitle() {
        EditBox editTitleBox = lifecycle.editors().box("editTitle");
        if (editTitleBox == null) { closeEditTitleDialog(); return; }
        String title = editTitleBox.getValue();
        closeEditTitleDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setTitle(title);
        ModData.setDirty();
    }

    private void openEditItemCountDialog(int idx) {
        lifecycle.modals().open("editItemCount");
        editItemCountIdx = idx;
        editItemCountY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        int current = 1;
        if (quest != null && idx >= 0 && idx < quest.getReward().getItems().size()) {
            current = quest.getReward().getItems().get(idx).getCount();
        }
        lifecycle.editors().addNumericBox("editItemCount", cx - 100, editItemCountY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), String.valueOf(current));
        parent.setFocusedWidget(lifecycle.editors().box("editItemCount"));
    }

    private void closeEditItemCountDialog() {
        lifecycle.modals().close("editItemCount");
        editItemCountIdx = -1;
        lifecycle.editors().remove("editItemCount");
    }

    private void confirmEditItemCount() {
        EditBox editItemCountBox = lifecycle.editors().box("editItemCount");
        if (editItemCountBox == null || editItemCountIdx < 0) { closeEditItemCountDialog(); return; }
        int count = 1;
        try { count = Integer.parseInt(editItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditItemCountDialog();
        String selectedId = getSelectedId();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || editItemCountIdx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().get(editItemCountIdx).setCount(count);
        ModData.setDirty();
    }
}