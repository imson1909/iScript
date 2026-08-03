package com.iscript.iscript.gui.screen.ListSubScreen;

import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.I18n;
import com.iscript.iscript.gui.screen.SubScreenLifecycle;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.ContextMenu;

import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.ModData;
import com.iscript.iscript.data.quest.*;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quest extends DashboardScreen.SubScreen {
    private final SubScreenLifecycle life = new SubScreenLifecycle(this);
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;

    private ContextMenu contextMenu = new ContextMenu();
    private String contextMenuItemId = null;

    private int editorContentHeight = 0;

    private String nameDialogMode = "";
    private String renameOldId = null;
    private int nameDialogY = 0;

    private String confirmDialogAction = "";
    private String confirmDialogId = null;
    private int confirmDialogY = 0;

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
    public void tick() {
        super.tick();
        life.tick(this::sendSave);
    }

    @Override
    public void init() {
        life.init();

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = this.parent.height - DashboardScreen.TOPBAR_H - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        life.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.quest.list.search"));

        life.modals().register("name",
                () -> life.modals().isOpen("name"),
                v -> {},
                null,
                null,
                "nameInput");

        life.modals().register("confirm",
                () -> life.modals().isOpen("confirm"),
                v -> {},
                null,
                null);

        life.modals().register("addStage",
                () -> life.modals().isOpen("addStage"),
                v -> {},
                null,
                null,
                "stageId", "stageDesc");

        life.modals().register("addObjective",
                () -> life.modals().isOpen("addObjective"),
                v -> {},
                null,
                null,
                "objTarget", "objCount", "objDesc");

        life.modals().register("addPrereq",
                () -> life.modals().isOpen("addPrereq"),
                v -> {},
                null,
                null);

        life.modals().register("addItemReward",
                () -> life.modals().isOpen("addItemReward"),
                v -> {},
                null,
                null,
                "rewardItem", "rewardCount");

        life.modals().register("itemPicker",
                () -> life.modals().isOpen("itemPicker"),
                v -> {},
                null,
                null,
                "itemPickerSearch", "itemPickerCount");

        life.modals().register("editExp",
                () -> life.modals().isOpen("editExp"),
                v -> {},
                null,
                null,
                "editExp");

        life.modals().register("editCmd",
                () -> life.modals().isOpen("editCmd"),
                v -> {},
                null,
                null,
                "editCmd");

        life.modals().register("editTitle",
                () -> life.modals().isOpen("editTitle"),
                v -> {},
                null,
                null,
                "editTitle");

        life.modals().register("editItemCount",
                () -> life.modals().isOpen("editItemCount"),
                v -> {},
                null,
                null,
                "editItemCount");


        showAddObjectiveTypeMenu = false;
        showAddPrereqMenu = false;
        life.state().prereqMenuScroll = 0;
        contextMenu.close();

        String sel = life.selection().get();
        if (sel != null && DataAccess.quest(sel) != null) {
            createEditorWidgets(sel);
        }

        if (life.modals().isOpen("name")) {
            openNameDialog(nameDialogMode, renameOldId);
        }
        if (life.modals().isOpen("confirm")) {
            openConfirmDialog(confirmDialogAction, confirmDialogId);
        }
        if (life.modals().isOpen("addStage")) {
            openAddStageDialog();
        }
        if (life.modals().isOpen("addObjective")) {
            openAddObjectiveDialog(addObjectiveStageIdx);
        }
        if (life.modals().isOpen("addPrereq")) {
            openAddPrereqDialog();
        }
        if (life.modals().isOpen("addItemReward")) {
            openAddItemRewardDialog();
        }
        if (life.modals().isOpen("itemPicker")) {
            openItemPicker();
        }
        if (life.modals().isOpen("editExp")) {
            openEditExpDialog();
        }
        if (life.modals().isOpen("editCmd")) {
            openEditCommandDialog();
        }
        if (life.modals().isOpen("editTitle")) {
            openEditTitleDialog();
        }
        if (life.modals().isOpen("editItemCount")) {
            openEditItemCountDialog(editItemCountIdx);
        }
    }

    private void createEditorWidgets(String questId) {
        if (this.minecraft == null) return;
        var quest = DataAccess.quest(questId);
        if (quest == null) return;

        int x = DashboardScreen.SIDEBAR_W;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftX = x + 4;

        life.editors().addBox("title", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.title"), quest.getTitle());
        EditBox titleBox = life.editors().box("title");
        if (titleBox != null) titleBox.setResponder(s -> life.save().debounce(10));

        life.editors().addMultiBox("desc", leftX + 4, 0, leftW - 8, 50, I18n.t("iscript.quest.editor.placeholder.description"), quest.getDescription());
        MultiLineEditBox descBox = life.editors().multi("desc");
        if (descBox != null) descBox.setOnValueChanged(() -> life.save().debounce(10));

        life.editors().addBox("giver", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.giver_npc"), quest.getGiverNpcId());
        EditBox giverBox = life.editors().box("giver");
        if (giverBox != null) giverBox.setResponder(s -> life.save().debounce(10));

        life.editors().addBox("turnIn", leftX + 4, 0, leftW - 8, 18, I18n.t("iscript.quest.editor.placeholder.turnin_npc"), quest.getTurnInNpcId());
        EditBox turnInBox = life.editors().box("turnIn");
        if (turnInBox != null) turnInBox.setResponder(s -> life.save().debounce(10));
    }


    private List<String> filteredIds() {
        var quests = DataAccess.quests();
        EditBox box = life.search().box();
        String filter = box != null ? box.getValue().trim().toLowerCase() : life.state().lastSearch.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (var e : quests.entrySet()) {
            String name = e.getValue().getTitle();
            if (name == null) name = "";
            name = name.trim();
            if (filter.isEmpty() || name.toLowerCase().contains(filter)) {
                result.add(e.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }

    private void removeEditorWidgets() {
        life.editors().removeAll();
    }

    private void sendSave() {
        String editingQuestId = life.selection().get();
        if (editingQuestId == null) return;
        var quest = DataAccess.quest(editingQuestId);
        if (quest == null) return;
        EditBox titleBox = life.editors().box("title");
        MultiLineEditBox descBox = life.editors().multi("desc");
        EditBox giverBox = life.editors().box("giver");
        EditBox turnInBox = life.editors().box("turnIn");
        if (titleBox != null) quest.setTitle(titleBox.getValue());
        if (descBox != null) quest.setDescription(descBox.getValue());
        if (giverBox != null) quest.setGiverNpcId(giverBox.getValue());
        if (turnInBox != null) quest.setTurnInNpcId(turnInBox.getValue());
        ModData.setDirty();
    }

    private void switchToQuest(String id) {
        String editingQuestId = life.selection().get();
        if (editingQuestId != null && life.save().isDirty()) {
            sendSave();
        }
        life.selection().set(id);
        removeEditorWidgets();
        life.state().editorScroll = 0;
        life.state().expandedStage = -1;
        life.state().expandedObjective = -1;
        life.state().editorTab = 0;
        if (id != null) {
            createEditorWidgets(id);
        }
    }

    private void giveQuest() {
        String selectedId = life.selection().get();
        if (selectedId == null) return;
        if (life.save().isDirty()) {
            sendSave();
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.GIVE_QUEST, ServerCommandPacket.giveQuestToTag(selectedId, "")));
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        life.modals().open("name");
        int x = DashboardScreen.SIDEBAR_W;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        life.editors().addBox("nameInput", cx - 100, nameDialogY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.quest_name"), mode.equals("rename") && oldId != null ? oldId : "");
        EditBox nameInputBox = life.editors().box("nameInput");
        if (nameInputBox != null) {
            nameInputBox.setMaxLength(64);
            parent.setFocusedWidget(nameInputBox);
        }
    }

    private void closeNameDialog() {
        life.modals().close("name");
        renameOldId = null;
        life.editors().remove("nameInput");
    }

    private void confirmNameDialog() {
        EditBox nameInputBox = life.editors().box("nameInput");
        if (nameInputBox == null) { closeNameDialog(); return; }
        String name = nameInputBox.getValue().trim();
        String mode = nameDialogMode;
        String oldId = renameOldId;
        closeNameDialog();
        if (name.isEmpty()) return;

        String id = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (id.isEmpty()) return;

        if (mode.equals("create")) {
            QuestData quest = new QuestData();
            quest.setId(id);
            quest.setTitle(name);
            DataAccess.putQuest(quest);
            switchToQuest(id);
        } else if (mode.equals("rename") && oldId != null) {
            if (oldId.equals(id)) {
                return;
            }
            QuestData oldQuest = DataAccess.quest(oldId);
            if (oldQuest != null) {
                QuestData newQuest = oldQuest.copy();
                newQuest.setId(id);
                newQuest.setTitle(name);
                DataAccess.putQuest(newQuest);
                DataAccess.removeQuest(oldId);
                String selectedId = life.selection().get();
                if (selectedId != null && selectedId.equals(oldId)) {
                    life.selection().set(id);
                }
            }
        }
    }

    private void openConfirmDialog(String action, String id) {
        life.modals().open("confirm");
        confirmDialogAction = action;
        confirmDialogId = id;
        confirmDialogY = this.parent.height / 2 - 30;
    }

    private void closeConfirmDialog() {
        life.modals().close("confirm");
        confirmDialogAction = "";
        confirmDialogId = null;
    }

    private void executeConfirm() {
        if ("delete".equals(confirmDialogAction) && confirmDialogId != null) {
            DataAccess.removeQuest(confirmDialogId);
            String selectedId = life.selection().get();
            if (selectedId != null && selectedId.equals(confirmDialogId)) {
                life.selection().set(null);
                removeEditorWidgets();
            }
        }
        closeConfirmDialog();
    }

    private void copyItem(String id) {
        DashboardScreen.clipboard = id;
    }

    private void pasteItem() {
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
        switchToQuest(newId);
    }

    private void duplicateItem(String id) {
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
        switchToQuest(newId);
    }

    private void openAddStageDialog() {
        life.modals().open("addStage");
        addStageY = this.parent.height / 2 - 40;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        life.editors().addBox("stageId", cx - 100, addStageY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.stage_id"));
        EditBox stageIdBox = life.editors().box("stageId");
        if (stageIdBox != null) stageIdBox.setMaxLength(64);
        life.editors().addBox("stageDesc", cx - 100, addStageY + 48, 200, 20, I18n.t("iscript.quest.editor.placeholder.description"));
        EditBox stageDescBox = life.editors().box("stageDesc");
        if (stageDescBox != null) stageDescBox.setMaxLength(128);
        parent.setFocusedWidget(stageIdBox);
    }

    private void closeAddStageDialog() {
        life.modals().close("addStage");
        life.editors().remove("stageId");
        life.editors().remove("stageDesc");
    }

    private void confirmAddStage() {
        EditBox stageIdBox = life.editors().box("stageId");
        if (stageIdBox == null) { closeAddStageDialog(); return; }
        String id = stageIdBox.getValue().trim();
        EditBox stageDescBox = life.editors().box("stageDesc");
        String desc = stageDescBox != null ? stageDescBox.getValue().trim() : "";
        closeAddStageDialog();
        if (id.isEmpty()) return;

        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestStage stage = new QuestStage();
        stage.setId(id);
        stage.setDescription(desc);
        quest.getStages().add(stage);
        ModData.setDirty();
    }

    private void removeStage(int idx) {
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getStages().size()) return;
        quest.getStages().remove(idx);
        if (life.state().expandedStage == idx) life.state().expandedStage = -1;
        else if (life.state().expandedStage > idx) life.state().expandedStage--;
        ModData.setDirty();
    }

    private void openAddObjectiveDialog(int stageIdx) {
        life.modals().open("addObjective");
        addObjectiveStageIdx = stageIdx;
        addObjectiveY = this.parent.height / 2 - 50;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        objectiveTypeIndex = 0;
        life.editors().addBox("objTarget", cx - 100, addObjectiveY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.target"));
        EditBox objTargetBox = life.editors().box("objTarget");
        if (objTargetBox != null) objTargetBox.setMaxLength(128);
        life.editors().addNumericBox("objCount", cx - 100, addObjectiveY + 46, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        life.editors().addBox("objDesc", cx - 100, addObjectiveY + 72, 200, 20, I18n.t("iscript.quest.editor.placeholder.description"));
        EditBox objDescBox = life.editors().box("objDesc");
        if (objDescBox != null) objDescBox.setMaxLength(128);
        parent.setFocusedWidget(objTargetBox);
    }

    private void closeAddObjectiveDialog() {
        life.modals().close("addObjective");
        addObjectiveStageIdx = -1;
        life.editors().remove("objTarget");
        life.editors().remove("objCount");
        life.editors().remove("objDesc");
    }

    private void confirmAddObjective() {
        EditBox objTargetBox = life.editors().box("objTarget");
        if (objTargetBox == null || addObjectiveStageIdx < 0) { closeAddObjectiveDialog(); return; }
        String target = objTargetBox.getValue().trim();
        int count = 1;
        EditBox objCountBox = life.editors().box("objCount");
        try { count = Integer.parseInt(objCountBox.getValue()); } catch (NumberFormatException ignored) {}
        EditBox objDescBox = life.editors().box("objDesc");
        String desc = objDescBox != null ? objDescBox.getValue().trim() : "";
        int stageIdx = addObjectiveStageIdx;
        closeAddObjectiveDialog();

        String selectedId = life.selection().get();
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
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || stageIdx < 0 || stageIdx >= quest.getStages().size()) return;
        List<QuestObjective> objs = quest.getStages().get(stageIdx).getObjectives();
        if (objIdx < 0 || objIdx >= objs.size()) return;
        objs.remove(objIdx);
        if (life.state().expandedObjective == objIdx) life.state().expandedObjective = -1;
        else if (life.state().expandedObjective > objIdx) life.state().expandedObjective--;
        ModData.setDirty();
    }

    private void openAddPrereqDialog() {
        life.modals().open("addPrereq");
        addPrereqY = this.parent.height / 2 - 30;
        this.prereqDropdownIndex = 0;
    }

    private void closeAddPrereqDialog() {
        life.modals().close("addPrereq");
    }

    private void confirmAddPrereq() {
        closeAddPrereqDialog();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        List<String> allIds = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(allIds);
        if (this.prereqDropdownIndex >= 0 && this.prereqDropdownIndex < allIds.size()) {
            String id = allIds.get(this.prereqDropdownIndex);
            if (!id.equals(selectedId) && !quest.getPrerequisites().contains(id)) {
                quest.getPrerequisites().add(id);
                ModData.setDirty();
            }
        }
    }

    private void removePrereq(int idx) {
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getPrerequisites().size()) return;
        quest.getPrerequisites().remove(idx);
        ModData.setDirty();
    }

    private void openAddItemRewardDialog() {
        life.modals().open("addItemReward");
        addItemRewardY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        life.editors().addBox("rewardItem", cx - 100, addItemRewardY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.item_id"));
        EditBox rewardItemBox = life.editors().box("rewardItem");
        if (rewardItemBox != null) rewardItemBox.setMaxLength(128);
        life.editors().addNumericBox("rewardCount", cx - 100, addItemRewardY + 46, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        parent.setFocusedWidget(rewardItemBox);
    }

    private void closeAddItemRewardDialog() {
        life.modals().close("addItemReward");
        life.editors().remove("rewardItem");
        life.editors().remove("rewardCount");
    }

    private void confirmAddItemReward() {
        EditBox rewardItemBox = life.editors().box("rewardItem");
        if (rewardItemBox == null) { closeAddItemRewardDialog(); return; }
        String itemId = rewardItemBox.getValue().trim();
        int count = 1;
        EditBox rewardItemCountBox = life.editors().box("rewardCount");
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeAddItemRewardDialog();
        if (itemId.isEmpty()) return;

        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        ModData.setDirty();
    }

    private void removeItemReward(int idx) {
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || idx < 0 || idx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().remove(idx);
        ModData.setDirty();
    }

    private void removeRewardExp() {
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setExp(0);
        ModData.setDirty();
    }

    private void removeRewardCommand() {
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setCommand("");
        ModData.setDirty();
    }

    private void openItemPicker() {
        life.modals().open("itemPicker");
        itemPickerY = this.parent.height / 2 - 100;
        itemPickerSelectedId = "";
        itemPickerScroll = 0;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        life.editors().addBox("itemPickerSearch", cx - 100, itemPickerY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.search"));
        EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.setMaxLength(128);
            itemPickerSearchBox.setValue("");
            itemPickerSearchBox.setResponder(s -> {
                itemPickerScroll = 0;
                filterItemPicker();
            });
        }
        parent.setFocusedWidget(itemPickerSearchBox);
        life.editors().addNumericBox("itemPickerCount", cx - 100, itemPickerY + 156, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), "1");
        filterItemPicker();
    }

    private void filterItemPicker() {
        itemPickerFiltered.clear();
        EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
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
        life.modals().close("itemPicker");
        itemPickerSelectedId = "";
        life.editors().remove("itemPickerSearch");
        life.editors().remove("itemPickerCount");
    }

    private void confirmItemPicker() {
        if (itemPickerSelectedId.isEmpty()) {
            closeItemPicker();
            return;
        }
        int count = 1;
        EditBox rewardItemCountBox = life.editors().box("itemPickerCount");
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeItemPicker();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemPickerSelectedId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        ModData.setDirty();
    }

    private void openEditExpDialog() {
        life.modals().open("editExp");
        editExpY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        life.editors().addNumericBox("editExp", cx - 100, editExpY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.exp"), String.valueOf(quest != null ? quest.getReward().getExp() : 0));
        parent.setFocusedWidget(life.editors().box("editExp"));
    }

    private void closeEditExpDialog() {
        life.modals().close("editExp");
        life.editors().remove("editExp");
    }

    private void confirmEditExp() {
        EditBox editExpBox = life.editors().box("editExp");
        if (editExpBox == null) { closeEditExpDialog(); return; }
        int exp = 0;
        try { exp = Integer.parseInt(editExpBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditExpDialog();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setExp(exp);
        ModData.setDirty();
    }

    private void openEditCommandDialog() {
        life.modals().open("editCmd");
        editCmdY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        life.editors().addBox("editCmd", cx - 100, editCmdY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.command"), quest != null ? quest.getReward().getCommand() : "");
        EditBox editCmdBox = life.editors().box("editCmd");
        if (editCmdBox != null) editCmdBox.setMaxLength(256);
        parent.setFocusedWidget(editCmdBox);
    }

    private void closeEditCommandDialog() {
        life.modals().close("editCmd");
        life.editors().remove("editCmd");
    }

    private void confirmEditCommand() {
        EditBox editCmdBox = life.editors().box("editCmd");
        if (editCmdBox == null) { closeEditCommandDialog(); return; }
        String cmd = editCmdBox.getValue();
        closeEditCommandDialog();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setCommand(cmd);
        ModData.setDirty();
    }

    private void openEditTitleDialog() {
        life.modals().open("editTitle");
        editTitleY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        life.editors().addBox("editTitle", cx - 100, editTitleY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.reward_title"), quest != null ? quest.getReward().getTitle() : "");
        EditBox editTitleBox = life.editors().box("editTitle");
        if (editTitleBox != null) editTitleBox.setMaxLength(128);
        parent.setFocusedWidget(editTitleBox);
    }

    private void closeEditTitleDialog() {
        life.modals().close("editTitle");
        life.editors().remove("editTitle");
    }

    private void confirmEditTitle() {
        EditBox editTitleBox = life.editors().box("editTitle");
        if (editTitleBox == null) { closeEditTitleDialog(); return; }
        String title = editTitleBox.getValue();
        closeEditTitleDialog();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null) return;
        quest.getReward().setTitle(title);
        ModData.setDirty();
    }

    private void openEditItemCountDialog(int idx) {
        life.modals().open("editItemCount");
        editItemCountIdx = idx;
        editItemCountY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        int current = 1;
        if (quest != null && idx >= 0 && idx < quest.getReward().getItems().size()) {
            current = quest.getReward().getItems().get(idx).getCount();
        }
        life.editors().addNumericBox("editItemCount", cx - 100, editItemCountY + 20, 200, 20, I18n.t("iscript.quest.editor.placeholder.count"), String.valueOf(current));
        parent.setFocusedWidget(life.editors().box("editItemCount"));
    }

    private void closeEditItemCountDialog() {
        life.modals().close("editItemCount");
        editItemCountIdx = -1;
        life.editors().remove("editItemCount");
    }

    private void confirmEditItemCount() {
        EditBox editItemCountBox = life.editors().box("editItemCount");
        if (editItemCountBox == null || editItemCountIdx < 0) { closeEditItemCountDialog(); return; }
        int count = 1;
        try { count = Integer.parseInt(editItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditItemCountDialog();
        String selectedId = life.selection().get();
        QuestData quest = DataAccess.quest(selectedId);
        if (quest == null || editItemCountIdx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().get(editItemCountIdx).setCount(count);
        ModData.setDirty();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        boolean modalOpen = life.modals().isOpen("name") || life.modals().isOpen("confirm") || life.modals().isOpen("addStage") || life.modals().isOpen("addObjective") || life.modals().isOpen("addPrereq") || life.modals().isOpen("addItemReward") || life.modals().isOpen("itemPicker") || life.modals().isOpen("editExp") || life.modals().isOpen("editCmd") || life.modals().isOpen("editTitle") || life.modals().isOpen("editItemCount");

        boolean editorVisible = life.selection().get() != null && !modalOpen;
        EditBox titleBox = life.editors().box("title");
        MultiLineEditBox descBox = life.editors().multi("desc");
        EditBox giverBox = life.editors().box("giver");
        EditBox turnInBox = life.editors().box("turnIn");
        if (titleBox != null) titleBox.setVisible(editorVisible);
        if (descBox != null) descBox.setVisible(editorVisible);
        if (giverBox != null) giverBox.setVisible(editorVisible);
        if (turnInBox != null) turnInBox.setVisible(editorVisible);

        if (!modalOpen) {
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);

            graphics.fill(toolbarX, y, rightX, y + h, Theme.BG_PANEL);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, Theme.BORDER);

            int btnSize = 24;
            int btnY = y + 8;

            boolean giveHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, giveHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, giveHovered ? Theme.ACCENT : 0xFF44AA44);
            btnY += btnSize + 6;

            boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
            graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? Theme.TEXT : Theme.TEXT_DIM);

            graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
            graphics.drawString(this.font, I18n.s("iscript.quest.editor.title"), rightX + 8, y + 26, Theme.ACCENT);

            life.search().setPos(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16);
            life.search().setVisible(true);
            EditBox searchBox = life.search().box();
            if (searchBox != null) searchBox.render(graphics, mouseX, mouseY, partialTick);

            List<String> ids = filteredIds();

            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int scroll = life.selection().scroll();

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(life.selection().get());

                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
            }

            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);

            var quests = DataAccess.quests();
            String selectedId = life.selection().get();
            if (selectedId != null && quests.containsKey(selectedId)) {
                var quest = quests.get(selectedId);

                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);

                RenderSystem.enableScissor(
                        (int) (leftX * this.minecraft.getWindow().getGuiScale()),
                        (int) ((this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * this.minecraft.getWindow().getGuiScale()),
                        (int) (leftW * this.minecraft.getWindow().getGuiScale()),
                        (int) (leftH * this.minecraft.getWindow().getGuiScale())
                );

                int dy = leftY + 6 - life.state().editorScroll;

                graphics.drawString(font, I18n.s("iscript.quest.editor.label.title"), leftX + 4, dy, Theme.TEXT_DIM);
                dy += 14;
                if (titleBox != null) {
                    titleBox.setX(leftX + 4);
                    titleBox.setY(dy);
                    titleBox.setWidth(leftW - 8);
                    titleBox.setVisible(true);
                }
                dy += 22;

                graphics.drawString(font, I18n.s("iscript.quest.editor.label.description"), leftX + 4, dy, Theme.TEXT_DIM);
                dy += 14;
                if (descBox != null) {
                    descBox.setX(leftX + 4);
                    descBox.setY(dy);
                    descBox.setWidth(leftW - 8);
                    descBox.setHeight(50);
                    descBox.setVisible(true);
                }
                dy += 56;

                graphics.drawString(font, I18n.s("iscript.quest.editor.label.giver_npc"), leftX + 4, dy, Theme.TEXT_DIM);
                dy += 14;
                if (giverBox != null) {
                    giverBox.setX(leftX + 4);
                    giverBox.setY(dy);
                    giverBox.setWidth(leftW - 8);
                    giverBox.setVisible(true);
                }
                dy += 22;

                graphics.drawString(font, I18n.s("iscript.quest.editor.label.turnin_npc"), leftX + 4, dy, Theme.TEXT_DIM);
                dy += 14;
                if (turnInBox != null) {
                    turnInBox.setX(leftX + 4);
                    turnInBox.setY(dy);
                    turnInBox.setWidth(leftW - 8);
                    turnInBox.setVisible(true);
                }
                dy += 26;

                String[] tabs = {I18n.s("iscript.quest.editor.tab.stages"), I18n.s("iscript.quest.editor.tab.rewards"), I18n.s("iscript.quest.editor.tab.prereqs")};
                int tabW = leftW / tabs.length;
                for (int i = 0; i < tabs.length; i++) {
                    int tx = leftX + 4 + i * tabW;
                    boolean th = mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= dy && mouseY <= dy + 18;
                    int tbg = life.state().editorTab == i ? 0xFF334455 : (th ? Theme.BG_HOVER : Theme.BG_INNER);
                    graphics.fill(tx, dy, tx + tabW - 2, dy + 18, tbg);
                    graphics.renderOutline(tx, dy, tabW - 2, 18, Theme.BORDER);
                    graphics.drawCenteredString(font, tabs[i], tx + tabW / 2 - 1, dy + 5, life.state().editorTab == i ? Theme.ACCENT : Theme.TEXT);
                }
                dy += 22;

                if (life.state().editorTab == 0) {
                    dy = renderStages(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                } else if (life.state().editorTab == 1) {
                    dy = renderRewards(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                } else if (life.state().editorTab == 2) {
                    dy = renderPrerequisites(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                }
                editorContentHeight = dy - (leftY + 6);

                RenderSystem.disableScissor();
            } else {
                removeEditorWidgets();
                graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        }

        if (life.modals().isOpen("name")) renderNameDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("confirm")) renderConfirmDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("addStage")) renderAddStageDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("addObjective")) renderAddObjectiveDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("addPrereq")) renderAddPrereqDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("addItemReward")) renderAddItemRewardDialog(graphics, x, w, mouseX, mouseY);
        if (showAddObjectiveTypeMenu) renderObjectiveTypeMenu(graphics, mouseX, mouseY);
        if (showAddPrereqMenu) renderPrereqMenu(graphics, mouseX, mouseY);
        if (life.modals().isOpen("itemPicker")) renderItemPicker(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("editExp")) renderEditExpDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("editCmd")) renderEditCommandDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("editTitle")) renderEditTitleDialog(graphics, x, w, mouseX, mouseY);
        if (life.modals().isOpen("editItemCount")) renderEditItemCountDialog(graphics, x, w, mouseX, mouseY);
        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
        }
    }

    private int renderStages(GuiGraphics graphics, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addHovered = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        graphics.drawCenteredString(font, I18n.s("iscript.quest.editor.add_stage"), leftX + leftW / 2, dy + 5, Theme.ACCENT);
        dy += 22;

        for (int s = 0; s < quest.getStages().size(); s++) {
            QuestStage stage = quest.getStages().get(s);
            boolean expanded = life.state().expandedStage == s;
            boolean sh = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 18;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 18, sh ? Theme.BORDER : Theme.BG_PANEL);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 18, Theme.BORDER);
            graphics.drawString(font, (expanded ? "v " : "> ") + stage.getId(), leftX + 8, dy + 5, expanded ? Theme.ACCENT : Theme.TEXT);

            boolean delH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 18, delH ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 18, Theme.BORDER);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 5, delH ? Theme.ERROR : 0xFFAA4444);
            dy += 20;

            if (expanded) {
                if (!stage.getDescription().isEmpty()) {
                    graphics.drawString(font, stage.getDescription(), leftX + 12, dy, Theme.TEXT_MUTE);
                    dy += 12;
                }

                boolean addObjH = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                graphics.fill(leftX + 12, dy, leftX + leftW - 12, dy + 16, addObjH ? Theme.BG_HOVER : Theme.BG_INNER);
                graphics.renderOutline(leftX + 12, dy, leftW - 24, 16, Theme.BORDER);
                graphics.drawCenteredString(font, I18n.s("iscript.quest.editor.add_objective"), leftX + leftW / 2, dy + 4, Theme.ACCENT);
                dy += 18;

                for (int o = 0; o < stage.getObjectives().size(); o++) {
                    QuestObjective obj = stage.getObjectives().get(o);
                    boolean objExp = life.state().expandedObjective == o;
                    boolean oh = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 32 && mouseY >= dy && mouseY <= dy + 16;
                    graphics.fill(leftX + 12, dy, leftX + leftW - 32, dy + 16, oh ? Theme.BORDER : Theme.BG_PANEL);
                    graphics.renderOutline(leftX + 12, dy, leftW - 44, 16, Theme.BORDER);
                    String label = (objExp ? "v " : "> ") + obj.getType().name() + ": " + obj.getTarget() + " (" + obj.getCurrentCount() + "/" + obj.getRequiredCount() + ")";
                    graphics.drawString(font, label, leftX + 16, dy + 4, Theme.TEXT);

                    boolean objDelH = mouseX >= leftX + leftW - 30 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                    graphics.fill(leftX + leftW - 30, dy, leftX + leftW - 12, dy + 16, objDelH ? Theme.BG_HOVER : Theme.BG_INNER);
                    graphics.renderOutline(leftX + leftW - 30, dy, 18, 16, Theme.BORDER);
                    graphics.drawCenteredString(font, "x", leftX + leftW - 21, dy + 4, objDelH ? Theme.ERROR : 0xFFAA4444);
                    dy += 18;

                    if (objExp) {
                        if (!obj.getDescription().isEmpty()) {
                            graphics.drawString(font, obj.getDescription(), leftX + 20, dy, Theme.TEXT_MUTE);
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

    private int renderRewards(GuiGraphics graphics, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        graphics.drawString(font, I18n.s("iscript.quest.editor.label.title"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean titleH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, titleH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        String title = quest.getReward().getTitle();
        graphics.drawString(font, title.isEmpty() ? I18n.s("iscript.quest.editor.reward.none") : title, leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        graphics.drawString(font, I18n.s("iscript.quest.editor.reward.items"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;

        for (int i = 0; i < quest.getReward().getItems().size(); i++) {
            QuestReward.ItemReward item = quest.getReward().getItems().get(i);
            boolean ih = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ih ? Theme.BORDER : Theme.BG_PANEL);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 16, Theme.BORDER);
            graphics.drawString(font, item.getItemId() + " x" + item.getCount(), leftX + 8, dy + 4, Theme.TEXT);

            boolean idelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, idelH ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 16, Theme.BORDER);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, idelH ? Theme.ERROR : 0xFFAA4444);
            dy += 18;
        }

        boolean addItemH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 16, addItemH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 16, Theme.BORDER);
        graphics.drawCenteredString(font, I18n.s("iscript.quest.editor.reward.add_item"), leftX + leftW / 2, dy + 4, Theme.ACCENT);
        dy += 20;

        graphics.drawString(font, I18n.s("iscript.quest.editor.reward.exp"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean expH = mouseX >= leftX + 4 && mouseX <= leftX + 80 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + 80, dy + 18, expH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, 76, 18, Theme.BORDER);
        graphics.drawString(font, String.valueOf(quest.getReward().getExp()), leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        graphics.drawString(font, I18n.s("iscript.quest.editor.reward.command"), leftX + 4, dy, Theme.TEXT_DIM);
        dy += 14;
        boolean cmdH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, cmdH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        String cmd = quest.getReward().getCommand();
        graphics.drawString(font, cmd.isEmpty() ? I18n.s("iscript.quest.editor.reward.none") : (cmd.length() > 30 ? cmd.substring(0, 30) + "..." : cmd), leftX + 8, dy + 5, Theme.TEXT);
        dy += 22;

        return dy;
    }

    private int renderPrerequisites(GuiGraphics graphics, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, Theme.BORDER);
        graphics.drawCenteredString(font, I18n.s("iscript.quest.editor.dialog.add_prereq"), leftX + leftW / 2, dy + 5, Theme.ACCENT);
        dy += 22;

        for (int i = 0; i < quest.getPrerequisites().size(); i++) {
            String prereq = quest.getPrerequisites().get(i);
            boolean ph = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ph ? Theme.BORDER : Theme.BG_PANEL);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 16, Theme.BORDER);
            graphics.drawString(font, prereq, leftX + 8, dy + 4, Theme.TEXT);

            boolean pdelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, pdelH ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 16, Theme.BORDER);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, pdelH ? Theme.ERROR : 0xFFAA4444);
            dy += 18;
        }
        return dy;
    }

    private void renderNameDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = nameDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? I18n.s("iscript.quest.editor.dialog.rename_quest") : I18n.s("iscript.quest.editor.dialog.new_quest_name"), cx, dy + 6, Theme.ACCENT);

        EditBox nameInputBox = life.editors().box("nameInput");
        if (nameInputBox != null) {
            nameInputBox.setX(cx - 100);
            nameInputBox.setY(dy + 24);
        }

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? I18n.s("iscript.quest.editor.button.rename") : I18n.s("iscript.quest.editor.button.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderConfirmDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 70;
        int dx = cx - dw / 2;
        int dy = confirmDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
        graphics.drawCenteredString(this.font, "Delete \"" + confirmDialogId + "\"?", cx, dy + 8, Theme.ERROR);

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 38, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
    }

    private void renderAddStageDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addStageY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.new_stage"), cx, dy + 6, Theme.ACCENT);

        EditBox stageIdBox = life.editors().box("stageId");
        if (stageIdBox != null) { stageIdBox.setX(cx - 100); stageIdBox.setY(dy + 24); }
        EditBox stageDescBox = life.editors().box("stageDesc");
        if (stageDescBox != null) { stageDescBox.setX(cx - 100); stageDescBox.setY(dy + 52); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 74, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 79, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 74, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 79, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddObjectiveDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 140;
        int dx = cx - dw / 2;
        int dy = addObjectiveY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.new_objective"), cx, dy + 6, Theme.ACCENT);

        QuestObjectiveType[] types = QuestObjectiveType.values();
        boolean typeH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        graphics.fill(cx - 100, dy + 24, cx + 100, dy + 42, typeH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 100, dy + 24, 200, 18, Theme.BORDER);
        graphics.drawCenteredString(font, types[objectiveTypeIndex].name(), cx, dy + 28, Theme.TEXT);

        EditBox objTargetBox = life.editors().box("objTarget");
        if (objTargetBox != null) { objTargetBox.setX(cx - 100); objTargetBox.setY(dy + 46); }
        EditBox objCountBox = life.editors().box("objCount");
        if (objCountBox != null) { objCountBox.setX(cx - 100); objCountBox.setY(dy + 70); }
        EditBox objDescBox = life.editors().box("objDesc");
        if (objDescBox != null) { objDescBox.setX(cx - 100); objDescBox.setY(dy + 94); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 116 && mouseY <= dy + 138;
        graphics.fill(cx - 50, dy + 116, cx - 2, dy + 138, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 116, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 121, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 116 && mouseY <= dy + 138;
        graphics.fill(cx + 2, dy + 116, cx + 50, dy + 138, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 116, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 121, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddPrereqDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = addPrereqY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.add_prereq"), cx, dy + 6, Theme.ACCENT);

        List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(ids);
        String current = this.prereqDropdownIndex >= 0 && this.prereqDropdownIndex < ids.size() ? ids.get(this.prereqDropdownIndex) : "None";
        boolean dropH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        graphics.fill(cx - 100, dy + 24, cx + 100, dy + 42, dropH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 100, dy + 24, 200, 18, Theme.BORDER);
        graphics.drawCenteredString(font, current, cx, dy + 28, Theme.TEXT);

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderAddItemRewardDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addItemRewardY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.add_item_reward"), cx, dy + 6, Theme.ACCENT);

        EditBox rewardItemBox = life.editors().box("rewardItem");
        if (rewardItemBox != null) { rewardItemBox.setX(cx - 100); rewardItemBox.setY(dy + 24); }
        EditBox rewardItemCountBox = life.editors().box("rewardCount");
        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 48); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 74, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 79, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 74, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 79, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderItemPicker(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 200;
        int dx = cx - dw / 2;
        int dy = itemPickerY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.select_item"), cx, dy + 6, Theme.ACCENT);

        EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
        if (itemPickerSearchBox != null) { itemPickerSearchBox.setX(cx - 100); itemPickerSearchBox.setY(dy + 20); }

        int listY = dy + 44;
        int listH = 108;
        int scrollBarW = 6;
        int listX = dx + 4;
        int listW = dw - 8 - scrollBarW;

        graphics.fill(listX, listY, listX + listW, listY + listH, Theme.BG_INNER);
        graphics.renderOutline(listX, listY, listW, listH, Theme.BORDER);

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
            graphics.fill(listX + 1, ry, listX + listW - 1, ry + rowH, bg);
            graphics.renderItem(new ItemStack(item), listX + 4, ry + 1);
            graphics.drawString(font, item.getDescription().getString(), listX + 22, ry + 5, selected ? Theme.ACCENT : Theme.TEXT);
        }

        RenderSystem.disableScissor();

        int scrollX = listX + listW;
        graphics.fill(scrollX, listY, scrollX + scrollBarW, listY + listH, Theme.BG_INNER);
        graphics.renderOutline(scrollX, listY, scrollBarW, listH, Theme.BORDER);

        int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (visibleRows * listH) / itemPickerFiltered.size());
            int thumbY = listY + (itemPickerScroll * (listH - thumbH)) / maxScroll;
            graphics.fill(scrollX, thumbY, scrollX + scrollBarW, thumbY + thumbH, Theme.TEXT_DIM);
        }

        EditBox rewardItemCountBox = life.editors().box("itemPickerCount");
        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 156); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 180 && mouseY <= dy + 198;
        graphics.fill(cx - 50, dy + 180, cx - 2, dy + 198, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 180, 48, 18, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.add"), cx - 26, dy + 183, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 180 && mouseY <= dy + 198;
        graphics.fill(cx + 2, dy + 180, cx + 50, dy + 198, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 180, 48, 18, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 183, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditExpDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editExpY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_exp"), cx, dy + 6, Theme.ACCENT);

        EditBox editExpBox = life.editors().box("editExp");
        if (editExpBox != null) { editExpBox.setX(cx - 100); editExpBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditCommandDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editCmdY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_command"), cx, dy + 6, Theme.ACCENT);

        EditBox editCmdBox = life.editors().box("editCmd");
        if (editCmdBox != null) { editCmdBox.setX(cx - 100); editCmdBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditTitleDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editTitleY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_reward_title"), cx, dy + 6, Theme.ACCENT);

        EditBox editTitleBox = life.editors().box("editTitle");
        if (editTitleBox != null) { editTitleBox.setX(cx - 100); editTitleBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderEditItemCountDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editItemCountY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.dialog.edit_item_count"), cx, dy + 6, Theme.ACCENT);

        EditBox editItemCountBox = life.editors().box("editItemCount");
        if (editItemCountBox != null) { editItemCountBox.setX(cx - 100); editItemCountBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.save"), cx - 26, dy + 57, okH ? Theme.ACCENT : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.quest.editor.button.cancel"), cx + 26, dy + 57, cancelH ? Theme.ERROR : 0xFFAA4444);
    }

    private void renderObjectiveTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        QuestObjectiveType[] types = QuestObjectiveType.values();
        int mw = 120;
        int mh = types.length * 20 + 4;
        graphics.fill(objTypeMenuX, objTypeMenuY, objTypeMenuX + mw, objTypeMenuY + mh, Theme.BG_INNER);
        graphics.renderOutline(objTypeMenuX, objTypeMenuY, mw, mh, Theme.BORDER);
        int cy = objTypeMenuY + 2;
        for (int i = 0; i < types.length; i++) {
            boolean h = mouseX >= objTypeMenuX && mouseX <= objTypeMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            graphics.fill(objTypeMenuX + 1, cy, objTypeMenuX + mw - 1, cy + 18, h ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.drawString(font, types[i].name(), objTypeMenuX + 6, cy + 5, h ? Theme.TEXT : Theme.TEXT);
            cy += 20;
        }
    }

    private void renderPrereqMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
        Collections.sort(ids);
        int mw = 140;
        int visible = Math.min(ids.size() - life.state().prereqMenuScroll, 10);
        int mh = visible * 20 + 4;
        graphics.fill(prereqMenuX, prereqMenuY, prereqMenuX + mw, prereqMenuY + mh, Theme.BG_INNER);
        graphics.renderOutline(prereqMenuX, prereqMenuY, mw, mh, Theme.BORDER);
        int cy = prereqMenuY + 2;
        for (int i = life.state().prereqMenuScroll; i < Math.min(ids.size(), life.state().prereqMenuScroll + 10); i++) {
            boolean h = mouseX >= prereqMenuX && mouseX <= prereqMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            graphics.fill(prereqMenuX + 1, cy, prereqMenuX + mw - 1, cy + 18, h ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.drawString(font, ids.get(i), prereqMenuX + 6, cy + 5, h ? Theme.TEXT : Theme.TEXT);
            cy += 20;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu.isOpen()) {
            String itemId = contextMenu.getItemId();
            contextMenu.mouseClicked(mouseX, mouseY, button);
            String action = contextMenu.getLastAction();
            if (action != null && itemId != null) {
                switch (action) {
                    case "Copy" -> copyItem(itemId);
                    case "Paste" -> pasteItem();
                    case "Rename" -> openNameDialog("rename", itemId);
                    case "Duplicate" -> duplicateItem(itemId);
                    case "Delete" -> openConfirmDialog("delete", itemId);
                }
            }
            contextMenu.close();
            return true;
        }

        EditBox searchBox = life.search().box();
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            searchBox.setFocused(true);
            return true;
        }

        if (life.modals().isOpen("name")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int cx = x + w / 2;
            int dy = nameDialogY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmNameDialog();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeNameDialog();
                return true;
            }
            EditBox nameInputBox = life.editors().box("nameInput");
            if (nameInputBox != null && mouseX >= nameInputBox.getX() && mouseX <= nameInputBox.getX() + nameInputBox.getWidth() && mouseY >= nameInputBox.getY() && mouseY <= nameInputBox.getY() + nameInputBox.getHeight()) {
                parent.setFocusedWidget(nameInputBox);
                return nameInputBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int cx = x + w / 2;
            int dy = confirmDialogY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60) {
                executeConfirm();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60) {
                closeConfirmDialog();
                return true;
            }
            return true;
        }

        if (life.modals().isOpen("addStage")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addStageY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                confirmAddStage();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                closeAddStageDialog();
                return true;
            }
            EditBox stageIdBox = life.editors().box("stageId");
            if (stageIdBox != null && mouseX >= stageIdBox.getX() && mouseX <= stageIdBox.getX() + stageIdBox.getWidth() && mouseY >= stageIdBox.getY() && mouseY <= stageIdBox.getY() + stageIdBox.getHeight()) {
                parent.setFocusedWidget(stageIdBox);
                return stageIdBox.mouseClicked(mouseX, mouseY, button);
            }
            EditBox stageDescBox = life.editors().box("stageDesc");
            if (stageDescBox != null && mouseX >= stageDescBox.getX() && mouseX <= stageDescBox.getX() + stageDescBox.getWidth() && mouseY >= stageDescBox.getY() && mouseY <= stageDescBox.getY() + stageDescBox.getHeight()) {
                parent.setFocusedWidget(stageDescBox);
                return stageDescBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("addObjective")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addObjectiveY;

            if (mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42) {
                showAddObjectiveTypeMenu = true;
                objTypeMenuX = (int) mouseX;
                objTypeMenuY = (int) mouseY;
                return true;
            }
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 116 && mouseY <= dy + 138) {
                confirmAddObjective();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 116 && mouseY <= dy + 138) {
                closeAddObjectiveDialog();
                return true;
            }
            EditBox objTargetBox = life.editors().box("objTarget");
            if (objTargetBox != null && mouseX >= objTargetBox.getX() && mouseX <= objTargetBox.getX() + objTargetBox.getWidth() && mouseY >= objTargetBox.getY() && mouseY <= objTargetBox.getY() + objTargetBox.getHeight()) {
                parent.setFocusedWidget(objTargetBox);
                return objTargetBox.mouseClicked(mouseX, mouseY, button);
            }
            EditBox objCountBox = life.editors().box("objCount");
            if (objCountBox != null && mouseX >= objCountBox.getX() && mouseX <= objCountBox.getX() + objCountBox.getWidth() && mouseY >= objCountBox.getY() && mouseY <= objCountBox.getY() + objCountBox.getHeight()) {
                parent.setFocusedWidget(objCountBox);
                return objCountBox.mouseClicked(mouseX, mouseY, button);
            }
            EditBox objDescBox = life.editors().box("objDesc");
            if (objDescBox != null && mouseX >= objDescBox.getX() && mouseX <= objDescBox.getX() + objDescBox.getWidth() && mouseY >= objDescBox.getY() && mouseY <= objDescBox.getY() + objDescBox.getHeight()) {
                parent.setFocusedWidget(objDescBox);
                return objDescBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("addPrereq")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addPrereqY;

            if (mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42) {
                showAddPrereqMenu = true;
                prereqMenuX = (int) mouseX;
                prereqMenuY = (int) mouseY;
                return true;
            }
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmAddPrereq();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeAddPrereqDialog();
                return true;
            }
            return true;
        }

        if (life.modals().isOpen("addItemReward")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = addItemRewardY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                confirmAddItemReward();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                closeAddItemRewardDialog();
                return true;
            }
            EditBox rewardItemBox = life.editors().box("rewardItem");
            if (rewardItemBox != null && mouseX >= rewardItemBox.getX() && mouseX <= rewardItemBox.getX() + rewardItemBox.getWidth() && mouseY >= rewardItemBox.getY() && mouseY <= rewardItemBox.getY() + rewardItemBox.getHeight()) {
                parent.setFocusedWidget(rewardItemBox);
                return rewardItemBox.mouseClicked(mouseX, mouseY, button);
            }
            EditBox rewardItemCountBox = life.editors().box("rewardCount");
            if (rewardItemCountBox != null && mouseX >= rewardItemCountBox.getX() && mouseX <= rewardItemCountBox.getX() + rewardItemCountBox.getWidth() && mouseY >= rewardItemCountBox.getY() && mouseY <= rewardItemCountBox.getY() + rewardItemCountBox.getHeight()) {
                parent.setFocusedWidget(rewardItemCountBox);
                return rewardItemCountBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("itemPicker")) {
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
                if (mouseX >= listX && mouseX <= listX + listW && mouseY >= ry && mouseY <= ry + rowH) {
                    var item = itemPickerFiltered.get(i);
                    itemPickerSelectedId = BuiltInRegistries.ITEM.getKey(item).toString();
                    return true;
                }
            }

            int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
            if (maxScroll > 0 && mouseX >= scrollX && mouseX <= scrollX + scrollBarW && mouseY >= listY && mouseY <= listY + listH) {
                int thumbH = Math.max(10, (visibleRows * listH) / itemPickerFiltered.size());
                int trackH = listH - thumbH;
                double ratio = (mouseY - listY) / (double) trackH;
                itemPickerScroll = (int) Math.round(Math.max(0, Math.min(maxScroll, ratio * maxScroll)));
                return true;
            }

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 180 && mouseY <= dy + 198) {
                confirmItemPicker();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 180 && mouseY <= dy + 198) {
                closeItemPicker();
                return true;
            }
            EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && mouseX >= itemPickerSearchBox.getX() && mouseX <= itemPickerSearchBox.getX() + itemPickerSearchBox.getWidth() && mouseY >= itemPickerSearchBox.getY() && mouseY <= itemPickerSearchBox.getY() + itemPickerSearchBox.getHeight()) {
                parent.setFocusedWidget(itemPickerSearchBox);
                return itemPickerSearchBox.mouseClicked(mouseX, mouseY, button);
            }
            EditBox pickerCountBox = life.editors().box("itemPickerCount");
            if (pickerCountBox != null && mouseX >= pickerCountBox.getX() && mouseX <= pickerCountBox.getX() + pickerCountBox.getWidth() && mouseY >= pickerCountBox.getY() && mouseY <= pickerCountBox.getY() + pickerCountBox.getHeight()) {
                parent.setFocusedWidget(pickerCountBox);
                return pickerCountBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("editExp")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editExpY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditExp();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditExpDialog();
                return true;
            }
            EditBox editExpBox = life.editors().box("editExp");
            if (editExpBox != null && mouseX >= editExpBox.getX() && mouseX <= editExpBox.getX() + editExpBox.getWidth() && mouseY >= editExpBox.getY() && mouseY <= editExpBox.getY() + editExpBox.getHeight()) {
                parent.setFocusedWidget(editExpBox);
                return editExpBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("editCmd")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editCmdY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditCommand();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditCommandDialog();
                return true;
            }
            EditBox editCmdBox = life.editors().box("editCmd");
            if (editCmdBox != null && mouseX >= editCmdBox.getX() && mouseX <= editCmdBox.getX() + editCmdBox.getWidth() && mouseY >= editCmdBox.getY() && mouseY <= editCmdBox.getY() + editCmdBox.getHeight()) {
                parent.setFocusedWidget(editCmdBox);
                return editCmdBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("editTitle")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editTitleY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditTitle();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditTitleDialog();
                return true;
            }
            EditBox editTitleBox = life.editors().box("editTitle");
            if (editTitleBox != null && mouseX >= editTitleBox.getX() && mouseX <= editTitleBox.getX() + editTitleBox.getWidth() && mouseY >= editTitleBox.getY() && mouseY <= editTitleBox.getY() + editTitleBox.getHeight()) {
                parent.setFocusedWidget(editTitleBox);
                return editTitleBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("editItemCount")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dy = editItemCountY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditItemCount();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditItemCountDialog();
                return true;
            }
            EditBox editItemCountBox = life.editors().box("editItemCount");
            if (editItemCountBox != null && mouseX >= editItemCountBox.getX() && mouseX <= editItemCountBox.getX() + editItemCountBox.getWidth() && mouseY >= editItemCountBox.getY() && mouseY <= editItemCountBox.getY() + editItemCountBox.getHeight()) {
                parent.setFocusedWidget(editItemCountBox);
                return editItemCountBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showAddObjectiveTypeMenu) {
            QuestObjectiveType[] types = QuestObjectiveType.values();
            int mw = 120;
            int cy = objTypeMenuY + 2;
            for (int i = 0; i < types.length; i++) {
                if (mouseX >= objTypeMenuX && mouseX <= objTypeMenuX + mw && mouseY >= cy && mouseY <= cy + 18) {
                    objectiveTypeIndex = i;
                    showAddObjectiveTypeMenu = false;
                    return true;
                }
                cy += 20;
            }
            showAddObjectiveTypeMenu = false;
            return true;
        }

        if (showAddPrereqMenu) {
            List<String> ids = new ArrayList<>(DataAccess.quests().keySet());
            Collections.sort(ids);
            int mw = 140;
            int cy = prereqMenuY + 2;
            for (int i = life.state().prereqMenuScroll; i < Math.min(ids.size(), life.state().prereqMenuScroll + 10); i++) {
                if (mouseX >= prereqMenuX && mouseX <= prereqMenuX + mw && mouseY >= cy && mouseY <= cy + 18) {
                    this.prereqDropdownIndex = i;
                    showAddPrereqMenu = false;
                    return true;
                }
                cy += 20;
            }
            showAddPrereqMenu = false;
            return true;
        }

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (button == 1) {
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int scroll = life.selection().scroll();
            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                    contextMenu.open((int) mouseX, (int) mouseY, ids.get(i), canPaste);
                    return true;
                }
            }
        }

        if (button != 0) return false;

        int btnSize = 24;
        int btnY = y + 8;

        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            giveQuest();
            return true;
        }
        btnY += btnSize + 6;

        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            openNameDialog("create", null);
            return true;
        }

        List<String> ids = filteredIds();

        int listH = h - 68;
        int listY = y + 42;
        int visible = Math.max(1, listH / ITEM_HEIGHT);
        int scroll = life.selection().scroll();

        for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (!id.equals(life.selection().get())) {
                    switchToQuest(id);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            openNameDialog("create", null);
            return true;
        }

        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        var quests = DataAccess.quests();
        String selectedId = life.selection().get();
        if (selectedId != null && quests.containsKey(selectedId)) {
            var quest = quests.get(selectedId);

            int dy = leftY + 6 - life.state().editorScroll;
            dy += 14;
            dy += 22;
            dy += 14;
            dy += 56;
            dy += 14;
            dy += 22;
            dy += 14;
            dy += 26;
            String[] tabs = {I18n.s("iscript.quest.editor.tab.stages"), I18n.s("iscript.quest.editor.tab.rewards"), I18n.s("iscript.quest.editor.tab.prereqs")};
            int tabW = leftW / tabs.length;
            for (int i = 0; i < tabs.length; i++) {
                int tx = leftX + 4 + i * tabW;
                if (mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= dy && mouseY <= dy + 18) {
                    life.state().editorTab = i;
                    return true;
                }
            }
            dy += 22;

            if (life.state().editorTab == 0) {
                boolean addStageH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                if (addStageH) {
                    openAddStageDialog();
                    return true;
                }
                dy += 22;

                for (int s = 0; s < quest.getStages().size(); s++) {
                    QuestStage stage = quest.getStages().get(s);
                    boolean expanded = life.state().expandedStage == s;
                    boolean sh = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 18;
                    boolean delH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                    if (delH) {
                        removeStage(s);
                        return true;
                    }
                    if (sh) {
                        life.state().expandedStage = expanded ? -1 : s;
                        life.state().expandedObjective = -1;
                        return true;
                    }
                    dy += 20;

                    if (expanded) {
                        if (!stage.getDescription().isEmpty()) dy += 12;

                        boolean addObjH = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                        if (addObjH) {
                            openAddObjectiveDialog(s);
                            return true;
                        }
                        dy += 18;

                        for (int o = 0; o < stage.getObjectives().size(); o++) {
                            boolean objExp = life.state().expandedObjective == o;
                            boolean oh = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 32 && mouseY >= dy && mouseY <= dy + 16;
                            boolean objDelH = mouseX >= leftX + leftW - 30 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                            if (objDelH) {
                                removeObjective(s, o);
                                return true;
                            }
                            if (oh) {
                                life.state().expandedObjective = objExp ? -1 : o;
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
            } else if (life.state().editorTab == 1) {
                dy += 14;
                boolean titleH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                if (titleH) {
                    openEditTitleDialog();
                    return true;
                }
                dy += 22;

                dy += 14;
                for (int i = 0; i < quest.getReward().getItems().size(); i++) {
                    boolean idelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
                    if (idelH) {
                        removeItemReward(i);
                        return true;
                    }
                    boolean itemH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
                    if (itemH) {
                        openEditItemCountDialog(i);
                        return true;
                    }
                    dy += 18;
                }
                boolean addItemH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
                if (addItemH) {
                    openItemPicker();
                    return true;
                }
                dy += 20;

                dy += 14;
                boolean expH = mouseX >= leftX + 4 && mouseX <= leftX + 80 && mouseY >= dy && mouseY <= dy + 18;
                if (expH) {
                    openEditExpDialog();
                    return true;
                }
                dy += 22;

                dy += 14;
                boolean cmdH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                if (cmdH) {
                    openEditCommandDialog();
                    return true;
                }
                dy += 22;
            } else if (life.state().editorTab == 2) {
                boolean addH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                if (addH) {
                    openAddPrereqDialog();
                    return true;
                }
                dy += 22;
                for (int i = 0; i < quest.getPrerequisites().size(); i++) {
                    boolean pdelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
                    if (pdelH) {
                        removePrereq(i);
                        return true;
                    }
                    dy += 18;
                }
            }

            EditBox titleBox = life.editors().box("title");
            MultiLineEditBox descBox = life.editors().multi("desc");
            EditBox giverBox = life.editors().box("giver");
            EditBox turnInBox = life.editors().box("turnIn");

            if (titleBox != null && titleBox.visible && mouseX >= titleBox.getX() && mouseX <= titleBox.getX() + titleBox.getWidth() && mouseY >= titleBox.getY() && mouseY <= titleBox.getY() + titleBox.getHeight()) {
                parent.setFocusedWidget(titleBox);
                return titleBox.mouseClicked(mouseX, mouseY, button);
            }
            if (descBox != null && descBox.visible && mouseX >= descBox.getX() && mouseX <= descBox.getX() + descBox.getWidth() && mouseY >= descBox.getY() && mouseY <= descBox.getY() + descBox.getHeight()) {
                parent.setFocusedWidget(descBox);
                return descBox.mouseClicked(mouseX, mouseY, button);
            }
            if (giverBox != null && giverBox.visible && mouseX >= giverBox.getX() && mouseX <= giverBox.getX() + giverBox.getWidth() && mouseY >= giverBox.getY() && mouseY <= giverBox.getY() + giverBox.getHeight()) {
                parent.setFocusedWidget(giverBox);
                return giverBox.mouseClicked(mouseX, mouseY, button);
            }
            if (turnInBox != null && turnInBox.visible && mouseX >= turnInBox.getX() && mouseX <= turnInBox.getX() + turnInBox.getWidth() && mouseY >= turnInBox.getY() && mouseY <= turnInBox.getY() + turnInBox.getHeight()) {
                parent.setFocusedWidget(turnInBox);
                return turnInBox.mouseClicked(mouseX, mouseY, button);
            }

            if (mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("name") || life.modals().isOpen("confirm") || life.modals().isOpen("addStage") || life.modals().isOpen("addObjective") || life.modals().isOpen("addPrereq") || life.modals().isOpen("addItemReward") || showAddObjectiveTypeMenu || showAddPrereqMenu || life.modals().isOpen("itemPicker") || life.modals().isOpen("editExp") || life.modals().isOpen("editCmd") || life.modals().isOpen("editTitle") || life.modals().isOpen("editItemCount")) return true;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        EditBox searchBox = life.search().box();
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        if (life.modals().isOpen("name")) {
            EditBox nameInputBox = life.editors().box("nameInput");
            if (nameInputBox != null && nameInputBox.isFocused()) {
                return nameInputBox.charTyped(codePoint, modifiers);
            }
            return true;
        }
        if (life.modals().isOpen("addStage")) {
            EditBox stageIdBox = life.editors().box("stageId");
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.charTyped(codePoint, modifiers);
            EditBox stageDescBox = life.editors().box("stageDesc");
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("addObjective")) {
            EditBox objTargetBox = life.editors().box("objTarget");
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.charTyped(codePoint, modifiers);
            EditBox objCountBox = life.editors().box("objCount");
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.charTyped(codePoint, modifiers);
            EditBox objDescBox = life.editors().box("objDesc");
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("addItemReward")) {
            EditBox rewardItemBox = life.editors().box("rewardItem");
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.charTyped(codePoint, modifiers);
            EditBox rewardItemCountBox = life.editors().box("rewardCount");
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("itemPicker")) {
            EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.charTyped(codePoint, modifiers);
            EditBox pickerCountBox = life.editors().box("itemPickerCount");
            if (pickerCountBox != null && pickerCountBox.isFocused()) return pickerCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("editExp")) {
            EditBox editExpBox = life.editors().box("editExp");
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("editCmd")) {
            EditBox editCmdBox = life.editors().box("editCmd");
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("editTitle")) {
            EditBox editTitleBox = life.editors().box("editTitle");
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("editItemCount")) {
            EditBox editItemCountBox = life.editors().box("editItemCount");
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        EditBox searchBox = life.search().box();
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (life.modals().isOpen("name")) {
            if (keyCode == 256) {
                closeNameDialog();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                confirmNameDialog();
                return true;
            }
            EditBox nameInputBox = life.editors().box("nameInput");
            if (nameInputBox != null && nameInputBox.isFocused()) {
                return nameInputBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (life.modals().isOpen("confirm")) {
            if (keyCode == 257 || keyCode == 335) {
                executeConfirm();
                return true;
            }
            if (keyCode == 256) {
                closeConfirmDialog();
                return true;
            }
            return true;
        }
        if (life.modals().isOpen("addStage")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddStage();
                return true;
            }
            if (keyCode == 256) {
                closeAddStageDialog();
                return true;
            }
            EditBox stageIdBox = life.editors().box("stageId");
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox stageDescBox = life.editors().box("stageDesc");
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("addObjective")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddObjective();
                return true;
            }
            if (keyCode == 256) {
                closeAddObjectiveDialog();
                return true;
            }
            EditBox objTargetBox = life.editors().box("objTarget");
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox objCountBox = life.editors().box("objCount");
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox objDescBox = life.editors().box("objDesc");
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("addPrereq")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddPrereq();
                return true;
            }
            if (keyCode == 256) {
                closeAddPrereqDialog();
                return true;
            }
            return true;
        }
        if (life.modals().isOpen("addItemReward")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddItemReward();
                return true;
            }
            if (keyCode == 256) {
                closeAddItemRewardDialog();
                return true;
            }
            EditBox rewardItemBox = life.editors().box("rewardItem");
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox rewardItemCountBox = life.editors().box("rewardCount");
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("itemPicker")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmItemPicker();
                return true;
            }
            if (keyCode == 256) {
                closeItemPicker();
                return true;
            }
            EditBox itemPickerSearchBox = life.editors().box("itemPickerSearch");
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.keyPressed(keyCode, scanCode, modifiers);
            EditBox pickerCountBox = life.editors().box("itemPickerCount");
            if (pickerCountBox != null && pickerCountBox.isFocused()) return pickerCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("editExp")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditExp();
                return true;
            }
            if (keyCode == 256) {
                closeEditExpDialog();
                return true;
            }
            EditBox editExpBox = life.editors().box("editExp");
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("editCmd")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditCommand();
                return true;
            }
            if (keyCode == 256) {
                closeEditCommandDialog();
                return true;
            }
            EditBox editCmdBox = life.editors().box("editCmd");
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("editTitle")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditTitle();
                return true;
            }
            if (keyCode == 256) {
                closeEditTitleDialog();
                return true;
            }
            EditBox editTitleBox = life.editors().box("editTitle");
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("editItemCount")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditItemCount();
                return true;
            }
            if (keyCode == 256) {
                closeEditItemCountDialog();
                return true;
            }
            EditBox editItemCountBox = life.editors().box("editItemCount");
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (life.modals().isOpen("name") || life.modals().isOpen("confirm") || life.modals().isOpen("addStage") || life.modals().isOpen("addObjective") || life.modals().isOpen("addPrereq") || life.modals().isOpen("addItemReward") || showAddObjectiveTypeMenu || showAddPrereqMenu || contextMenu.isOpen()) return true;

        EditBox searchBox = life.search().box();
        if (searchBox != null && searchBox.isFocused() && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth() && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
            return searchBox.mouseScrolled(mouseX, mouseY, delta);
        }

        if (life.modals().isOpen("itemPicker")) {
            int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
            int dw = 220;
            int dx = cx - dw / 2;
            int listY = itemPickerY + 44;
            int listH = 108;
            if (mouseX >= dx + 4 && mouseX <= dx + dw - 4 && mouseY >= listY && mouseY <= listY + listH) {
                int visibleRows = listH / 18;
                int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
                if (delta > 0) itemPickerScroll = Math.max(0, itemPickerScroll - 1);
                else itemPickerScroll = Math.min(itemPickerScroll + 1, maxScroll);
                return true;
            }
            return true;
        }

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            List<String> ids = filteredIds();
            int listH = h - 68;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int scroll = life.selection().scroll();
            int maxScroll = Math.max(0, ids.size() - visible);
            if (delta > 0) {
                life.selection().scroll(Math.max(0, scroll - 1));
            } else {
                life.selection().scroll(Math.min(scroll + 1, maxScroll));
            }
            return true;
        }

        int leftX = x + 4;
        int leftY = y + 4;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;

        if (mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            int maxEditorScroll = Math.max(0, editorContentHeight - leftH);
            if (delta > 0) life.state().editorScroll = Math.max(0, life.state().editorScroll - 20);
            else life.state().editorScroll = Math.min(life.state().editorScroll + 20, maxEditorScroll);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return life.modals().isOpen("name") || life.modals().isOpen("confirm") || life.modals().isOpen("addStage") || life.modals().isOpen("addObjective") || life.modals().isOpen("addPrereq") || life.modals().isOpen("addItemReward") || showAddObjectiveTypeMenu || showAddPrereqMenu || life.modals().isOpen("itemPicker") || life.modals().isOpen("editExp") || life.modals().isOpen("editCmd") || life.modals().isOpen("editTitle") || life.modals().isOpen("editItemCount") || contextMenu.isOpen();
    }

    @Override
    public void removed() {
        if (life.save().isDirty()) {
            sendSave();
        }
        life.removed();
        contextMenu.close();
        super.removed();
    }
}