package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.QuestSavedData;
import com.iscript.iscript.data.quest.*;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.GiveQuestPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 160;
    private static final int TOOLBAR_WIDTH = 32;
    private String selectedId = null;
    private String editingQuestId = null;

    private EditBox titleBox = null;
    private MultiLineEditBox descBox = null;
    private EditBox giverBox = null;
    private EditBox turnInBox = null;

    private int saveDebounce = 0;

    private boolean showNameDialog = false;
    private EditBox nameInputBox = null;
    private int nameDialogY = 0;
    private String nameDialogMode = "";
    private String renameOldId = null;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private String contextMenuItemId = null;

    private boolean showConfirmDialog = false;
    private int confirmDialogY = 0;
    private String confirmDialogAction = "";
    private String confirmDialogId = null;

    private int editorScroll = 0;
    private int expandedStage = -1;
    private int expandedObjective = -1;
    private int editorTab = 0;

    private boolean showAddStage = false;
    private int addStageY = 0;
    private EditBox stageIdBox = null;
    private EditBox stageDescBox = null;

    private boolean showAddObjective = false;
    private int addObjectiveStageIdx = -1;
    private int addObjectiveY = 0;
    private int objectiveTypeIndex = 0;
    private EditBox objTargetBox = null;
    private EditBox objCountBox = null;
    private EditBox objDescBox = null;

    private boolean showAddPrereq = false;
    private int addPrereqY = 0;
    private int prereqDropdownIndex = 0;

    private boolean showAddItemReward = false;
    private int addItemRewardY = 0;
    private EditBox rewardItemBox = null;
    private EditBox rewardItemCountBox = null;

    private boolean showAddObjectiveTypeMenu = false;
    private int objTypeMenuX, objTypeMenuY;

    private boolean showAddPrereqMenu = false;
    private int prereqMenuX, prereqMenuY;

    private boolean showItemPicker = false;
    private int itemPickerY = 0;
    private EditBox itemPickerSearchBox = null;
    private String itemPickerSelectedId = "";
    private int itemPickerScroll = 0;
    private final List<Item> itemPickerFiltered = new ArrayList<>();

    private boolean showEditExp = false;
    private int editExpY = 0;
    private EditBox editExpBox = null;

    private boolean showEditCommand = false;
    private int editCmdY = 0;
    private EditBox editCmdBox = null;

    private boolean showEditTitle = false;
    private int editTitleY = 0;
    private EditBox editTitleBox = null;

    private boolean showEditItemCount = false;
    private int editItemCountIdx = -1;
    private int editItemCountY = 0;
    private EditBox editItemCountBox = null;

    public QuestListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        showNameDialog = false;
        nameInputBox = null;
        showContextMenu = false;
        showConfirmDialog = false;
        showAddStage = false;
        showAddObjective = false;
        showAddPrereq = false;
        showAddItemReward = false;
        showAddObjectiveTypeMenu = false;
        showAddPrereqMenu = false;
        showItemPicker = false;
        showEditExp = false;
        showEditCommand = false;
        showEditTitle = false;
        showEditItemCount = false;
        removeEditorWidgets();
    }

    private void createEditorWidgets(QuestData quest) {
        if (this.minecraft == null) return;

        titleBox = new EditBox(this.minecraft.font, 0, 0, 100, 18, Component.literal("Title"));
        titleBox.setValue(quest.getTitle());
        titleBox.setResponder(s -> saveDebounce = 10);
        parent.addWidget(titleBox);

        descBox = new MultiLineEditBox(this.minecraft.font, 0, 0, 100, 50, Component.literal("Description"), Component.empty());
        descBox.setValue(quest.getDescription());
        descBox.setOnValueChanged(() -> saveDebounce = 10);
        parent.addWidget(descBox);

        giverBox = new EditBox(this.minecraft.font, 0, 0, 100, 18, Component.literal("Giver NPC"));
        giverBox.setValue(quest.getGiverNpcId());
        giverBox.setResponder(s -> saveDebounce = 10);
        parent.addWidget(giverBox);

        turnInBox = new EditBox(this.minecraft.font, 0, 0, 100, 18, Component.literal("Turn-in NPC"));
        turnInBox.setValue(quest.getTurnInNpcId());
        turnInBox.setResponder(s -> saveDebounce = 10);
        parent.addWidget(turnInBox);
    }

    private void removeEditorWidgets() {
        if (titleBox != null) { parent.removeEditorWidget(titleBox); titleBox = null; }
        if (descBox != null) { parent.removeEditorWidget(descBox); descBox = null; }
        if (giverBox != null) { parent.removeEditorWidget(giverBox); giverBox = null; }
        if (turnInBox != null) { parent.removeEditorWidget(turnInBox); turnInBox = null; }
        if (nameInputBox != null) { parent.removeEditorWidget(nameInputBox); nameInputBox = null; }
        if (stageIdBox != null) { parent.removeEditorWidget(stageIdBox); stageIdBox = null; }
        if (stageDescBox != null) { parent.removeEditorWidget(stageDescBox); stageDescBox = null; }
        if (objTargetBox != null) { parent.removeEditorWidget(objTargetBox); objTargetBox = null; }
        if (objCountBox != null) { parent.removeEditorWidget(objCountBox); objCountBox = null; }
        if (objDescBox != null) { parent.removeEditorWidget(objDescBox); objDescBox = null; }
        if (rewardItemBox != null) { parent.removeEditorWidget(rewardItemBox); rewardItemBox = null; }
        if (rewardItemCountBox != null) { parent.removeEditorWidget(rewardItemCountBox); rewardItemCountBox = null; }
        if (itemPickerSearchBox != null) { parent.removeEditorWidget(itemPickerSearchBox); itemPickerSearchBox = null; }
        if (editExpBox != null) { parent.removeEditorWidget(editExpBox); editExpBox = null; }
        if (editCmdBox != null) { parent.removeEditorWidget(editCmdBox); editCmdBox = null; }
        if (editTitleBox != null) { parent.removeEditorWidget(editTitleBox); editTitleBox = null; }
        if (editItemCountBox != null) { parent.removeEditorWidget(editItemCountBox); editItemCountBox = null; }
        editingQuestId = null;
    }

    private void sendSave() {
        if (editingQuestId == null) return;
        var quest = QuestManager.get(level, editingQuestId);
        if (quest == null) return;
        quest.setTitle(titleBox.getValue());
        quest.setDescription(descBox.getValue());
        quest.setGiverNpcId(giverBox.getValue());
        quest.setTurnInNpcId(turnInBox.getValue());
        QuestSavedData.get(level).setDirty();
    }

    private void switchToQuest(String id) {
        if (editingQuestId != null && saveDebounce > 0) {
            sendSave();
        }
        selectedId = id;
        editingQuestId = null;
        removeEditorWidgets();
        editorScroll = 0;
        expandedStage = -1;
        expandedObjective = -1;
        editorTab = 0;
    }

    private void giveQuest() {
        if (selectedId == null) return;
        if (saveDebounce > 0) {
            saveDebounce = 0;
            sendSave();
        }
        IScriptNetwork.sendToServer(new GiveQuestPacket(selectedId, ""));
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal("Quest Name"));
        nameInputBox.setMaxLength(64);
        if (mode.equals("rename") && oldId != null) {
            nameInputBox.setValue(oldId);
        } else {
            nameInputBox.setValue("");
        }
        parent.addWidget(nameInputBox);
        parent.setFocusedWidget(nameInputBox);
    }

    private void closeNameDialog() {
        showNameDialog = false;
        renameOldId = null;
        if (nameInputBox != null) {
            parent.removeEditorWidget(nameInputBox);
            nameInputBox = null;
        }
    }

    private void confirmNameDialog() {
        if (nameInputBox == null) return;
        String name = nameInputBox.getValue().trim();
        closeNameDialog();
        if (name.isEmpty()) return;

        String id = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (id.isEmpty()) return;

        if (nameDialogMode.equals("create")) {
            QuestData quest = new QuestData();
            quest.setId(id);
            quest.setTitle(name);
            QuestManager.add(level, quest);
            switchToQuest(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            QuestData oldQuest = QuestManager.get(level, renameOldId);
            if (oldQuest != null) {
                QuestData newQuest = oldQuest.copy();
                newQuest.setId(id);
                newQuest.setTitle(name);
                QuestManager.add(level, newQuest);
                QuestManager.remove(level, renameOldId);
                if (selectedId != null && selectedId.equals(renameOldId)) {
                    selectedId = id;
                }
            }
        }
    }

    private void openContextMenu(String itemId, int mx, int my) {
        showContextMenu = true;
        contextMenuItemId = itemId;
        contextMenuX = mx;
        contextMenuY = my;
    }

    private void closeContextMenu() {
        showContextMenu = false;
        contextMenuItemId = null;
    }

    private void openConfirmDialog(String action, String id) {
        showConfirmDialog = true;
        confirmDialogAction = action;
        confirmDialogId = id;
        confirmDialogY = this.parent.height / 2 - 30;
    }

    private void closeConfirmDialog() {
        showConfirmDialog = false;
        confirmDialogAction = "";
        confirmDialogId = null;
    }

    private void executeConfirm() {
        if ("delete".equals(confirmDialogAction) && confirmDialogId != null) {
            QuestManager.remove(level, confirmDialogId);
            if (selectedId != null && selectedId.equals(confirmDialogId)) {
                selectedId = null;
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
        QuestData source = QuestManager.get(level, sourceId);
        if (source == null) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (QuestManager.get(level, newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }

        QuestData copy = source.copy();
        copy.setId(newId);
        copy.setTitle(source.getTitle() + " (Copy)");
        QuestManager.add(level, copy);
        switchToQuest(newId);
    }

    private void duplicateItem(String id) {
        QuestData source = QuestManager.get(level, id);
        if (source == null) return;

        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (QuestManager.get(level, newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }

        QuestData copy = source.copy();
        copy.setId(newId);
        copy.setTitle(source.getTitle() + " (" + counter + ")");
        QuestManager.add(level, copy);
        switchToQuest(newId);
    }

    private void openAddStageDialog() {
        showAddStage = true;
        addStageY = this.parent.height / 2 - 40;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        stageIdBox = new EditBox(this.minecraft.font, cx - 100, addStageY + 20, 200, 20, Component.literal("Stage ID"));
        stageIdBox.setMaxLength(64);
        stageDescBox = new EditBox(this.minecraft.font, cx - 100, addStageY + 48, 200, 20, Component.literal("Description"));
        stageDescBox.setMaxLength(128);
        parent.addWidget(stageIdBox);
        parent.addWidget(stageDescBox);
        parent.setFocusedWidget(stageIdBox);
    }

    private void closeAddStageDialog() {
        showAddStage = false;
        if (stageIdBox != null) { parent.removeEditorWidget(stageIdBox); stageIdBox = null; }
        if (stageDescBox != null) { parent.removeEditorWidget(stageDescBox); stageDescBox = null; }
    }

    private void confirmAddStage() {
        if (stageIdBox == null) return;
        String id = stageIdBox.getValue().trim();
        String desc = stageDescBox != null ? stageDescBox.getValue().trim() : "";
        closeAddStageDialog();
        if (id.isEmpty()) return;

        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        QuestStage stage = new QuestStage();
        stage.setId(id);
        stage.setDescription(desc);
        quest.getStages().add(stage);
        QuestSavedData.get(level).setDirty();
    }

    private void removeStage(int idx) {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || idx < 0 || idx >= quest.getStages().size()) return;
        quest.getStages().remove(idx);
        if (expandedStage == idx) expandedStage = -1;
        else if (expandedStage > idx) expandedStage--;
        QuestSavedData.get(level).setDirty();
    }

    private void openAddObjectiveDialog(int stageIdx) {
        showAddObjective = true;
        addObjectiveStageIdx = stageIdx;
        addObjectiveY = this.parent.height / 2 - 50;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        objectiveTypeIndex = 0;
        objTargetBox = new EditBox(this.minecraft.font, cx - 100, addObjectiveY + 20, 200, 20, Component.literal("Target"));
        objTargetBox.setMaxLength(128);
        objCountBox = new EditBox(this.minecraft.font, cx - 100, addObjectiveY + 46, 200, 20, Component.literal("Count"));
        objCountBox.setValue("1");
        objCountBox.setFilter(s -> s.matches("d*"));
        objDescBox = new EditBox(this.minecraft.font, cx - 100, addObjectiveY + 72, 200, 20, Component.literal("Description"));
        objDescBox.setMaxLength(128);
        parent.addWidget(objTargetBox);
        parent.addWidget(objCountBox);
        parent.addWidget(objDescBox);
        parent.setFocusedWidget(objTargetBox);
    }

    private void closeAddObjectiveDialog() {
        showAddObjective = false;
        addObjectiveStageIdx = -1;
        if (objTargetBox != null) { parent.removeEditorWidget(objTargetBox); objTargetBox = null; }
        if (objCountBox != null) { parent.removeEditorWidget(objCountBox); objCountBox = null; }
        if (objDescBox != null) { parent.removeEditorWidget(objDescBox); objDescBox = null; }
    }

    private void confirmAddObjective() {
        if (objTargetBox == null || addObjectiveStageIdx < 0) return;
        String target = objTargetBox.getValue().trim();
        int count = 1;
        try { count = Integer.parseInt(objCountBox.getValue()); } catch (NumberFormatException ignored) {}
        String desc = objDescBox != null ? objDescBox.getValue().trim() : "";
        closeAddObjectiveDialog();

        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || addObjectiveStageIdx >= quest.getStages().size()) return;
        QuestObjective obj = new QuestObjective();
        obj.setType(QuestObjectiveType.values()[objectiveTypeIndex]);
        obj.setTarget(target);
        obj.setRequiredCount(count);
        obj.setDescription(desc);
        quest.getStages().get(addObjectiveStageIdx).getObjectives().add(obj);
        QuestSavedData.get(level).setDirty();
    }

    private void removeObjective(int stageIdx, int objIdx) {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || stageIdx < 0 || stageIdx >= quest.getStages().size()) return;
        List<QuestObjective> objs = quest.getStages().get(stageIdx).getObjectives();
        if (objIdx < 0 || objIdx >= objs.size()) return;
        objs.remove(objIdx);
        if (expandedObjective == objIdx) expandedObjective = -1;
        else if (expandedObjective > objIdx) expandedObjective--;
        QuestSavedData.get(level).setDirty();
    }

    private void openAddPrereqDialog() {
        showAddPrereq = true;
        addPrereqY = this.parent.height / 2 - 30;
        prereqDropdownIndex = 0;
    }

    private void closeAddPrereqDialog() {
        showAddPrereq = false;
    }

    private void confirmAddPrereq() {
        closeAddPrereqDialog();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        List<String> allIds = new ArrayList<>(QuestManager.getAll(level).keySet());
        Collections.sort(allIds);
        if (prereqDropdownIndex >= 0 && prereqDropdownIndex < allIds.size()) {
            String id = allIds.get(prereqDropdownIndex);
            if (!id.equals(selectedId) && !quest.getPrerequisites().contains(id)) {
                quest.getPrerequisites().add(id);
                QuestSavedData.get(level).setDirty();
            }
        }
    }

    private void removePrereq(int idx) {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || idx < 0 || idx >= quest.getPrerequisites().size()) return;
        quest.getPrerequisites().remove(idx);
        QuestSavedData.get(level).setDirty();
    }

    private void openAddItemRewardDialog() {
        showAddItemReward = true;
        addItemRewardY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        rewardItemBox = new EditBox(this.minecraft.font, cx - 100, addItemRewardY + 20, 200, 20, Component.literal("Item ID"));
        rewardItemBox.setMaxLength(128);
        rewardItemCountBox = new EditBox(this.minecraft.font, cx - 100, addItemRewardY + 46, 200, 20, Component.literal("Count"));
        rewardItemCountBox.setValue("1");
        rewardItemCountBox.setFilter(s -> s.matches("d*"));
        parent.addWidget(rewardItemBox);
        parent.addWidget(rewardItemCountBox);
        parent.setFocusedWidget(rewardItemBox);
    }

    private void closeAddItemRewardDialog() {
        showAddItemReward = false;
        if (rewardItemBox != null) { parent.removeEditorWidget(rewardItemBox); rewardItemBox = null; }
        if (rewardItemCountBox != null) { parent.removeEditorWidget(rewardItemCountBox); rewardItemCountBox = null; }
    }

    private void confirmAddItemReward() {
        if (rewardItemBox == null) return;
        String itemId = rewardItemBox.getValue().trim();
        int count = 1;
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeAddItemRewardDialog();
        if (itemId.isEmpty()) return;

        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        QuestSavedData.get(level).setDirty();
    }

    private void removeItemReward(int idx) {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || idx < 0 || idx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().remove(idx);
        QuestSavedData.get(level).setDirty();
    }

    private void removeRewardExp() {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        quest.getReward().setExp(0);
        QuestSavedData.get(level).setDirty();
    }

    private void removeRewardCommand() {
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        quest.getReward().setCommand("");
        QuestSavedData.get(level).setDirty();
    }

    private void openItemPicker() {
        showItemPicker = true;
        itemPickerY = this.parent.height / 2 - 100;
        itemPickerSelectedId = "";
        itemPickerScroll = 0;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        itemPickerSearchBox = new EditBox(this.minecraft.font, cx - 100, itemPickerY + 20, 200, 20, Component.literal("Search"));
        itemPickerSearchBox.setMaxLength(128);
        itemPickerSearchBox.setValue("");
        itemPickerSearchBox.setResponder(s -> {
            itemPickerScroll = 0;
            filterItemPicker();
        });
        parent.addWidget(itemPickerSearchBox);
        parent.setFocusedWidget(itemPickerSearchBox);
        rewardItemCountBox = new EditBox(this.minecraft.font, cx - 100, itemPickerY + 156, 200, 20, Component.literal("Count"));
        rewardItemCountBox.setValue("1");
        rewardItemCountBox.setFilter(s -> s.matches("\\d*"));
        parent.addWidget(rewardItemCountBox);
        filterItemPicker();
    }

    private void filterItemPicker() {
        itemPickerFiltered.clear();
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
        showItemPicker = false;
        itemPickerSelectedId = "";
        if (itemPickerSearchBox != null) { parent.removeEditorWidget(itemPickerSearchBox); itemPickerSearchBox = null; }
        if (rewardItemCountBox != null) { parent.removeEditorWidget(rewardItemCountBox); rewardItemCountBox = null; }
    }

    private void confirmItemPicker() {
        if (itemPickerSelectedId.isEmpty()) {
            closeItemPicker();
            return;
        }
        int count = 1;
        try { count = Integer.parseInt(rewardItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeItemPicker();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        QuestReward.ItemReward item = new QuestReward.ItemReward();
        item.setItemId(itemPickerSelectedId);
        item.setCount(count);
        quest.getReward().getItems().add(item);
        QuestSavedData.get(level).setDirty();
    }

    private void openEditExpDialog() {
        showEditExp = true;
        editExpY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        QuestData quest = QuestManager.get(level, selectedId);
        editExpBox = new EditBox(this.minecraft.font, cx - 100, editExpY + 20, 200, 20, Component.literal("EXP"));
        editExpBox.setMaxLength(10);
        editExpBox.setValue(String.valueOf(quest != null ? quest.getReward().getExp() : 0));
        editExpBox.setFilter(s -> s.matches("\\d*"));
        parent.addWidget(editExpBox);
        parent.setFocusedWidget(editExpBox);
    }

    private void closeEditExpDialog() {
        showEditExp = false;
        if (editExpBox != null) { parent.removeEditorWidget(editExpBox); editExpBox = null; }
    }

    private void confirmEditExp() {
        if (editExpBox == null) { closeEditExpDialog(); return; }
        int exp = 0;
        try { exp = Integer.parseInt(editExpBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditExpDialog();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        quest.getReward().setExp(exp);
        QuestSavedData.get(level).setDirty();
    }

    private void openEditCommandDialog() {
        showEditCommand = true;
        editCmdY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        QuestData quest = QuestManager.get(level, selectedId);
        editCmdBox = new EditBox(this.minecraft.font, cx - 100, editCmdY + 20, 200, 20, Component.literal("Command"));
        editCmdBox.setMaxLength(256);
        editCmdBox.setValue(quest != null ? quest.getReward().getCommand() : "");
        parent.addWidget(editCmdBox);
        parent.setFocusedWidget(editCmdBox);
    }

    private void closeEditCommandDialog() {
        showEditCommand = false;
        if (editCmdBox != null) { parent.removeEditorWidget(editCmdBox); editCmdBox = null; }
    }

    private void confirmEditCommand() {
        if (editCmdBox == null) { closeEditCommandDialog(); return; }
        String cmd = editCmdBox.getValue();
        closeEditCommandDialog();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        quest.getReward().setCommand(cmd);
        QuestSavedData.get(level).setDirty();
    }

    private void openEditTitleDialog() {
        showEditTitle = true;
        editTitleY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        QuestData quest = QuestManager.get(level, selectedId);
        editTitleBox = new EditBox(this.minecraft.font, cx - 100, editTitleY + 20, 200, 20, Component.literal("Reward Title"));
        editTitleBox.setMaxLength(128);
        editTitleBox.setValue(quest != null ? quest.getReward().getTitle() : "");
        parent.addWidget(editTitleBox);
        parent.setFocusedWidget(editTitleBox);
    }

    private void closeEditTitleDialog() {
        showEditTitle = false;
        if (editTitleBox != null) { parent.removeEditorWidget(editTitleBox); editTitleBox = null; }
    }

    private void confirmEditTitle() {
        if (editTitleBox == null) { closeEditTitleDialog(); return; }
        String title = editTitleBox.getValue();
        closeEditTitleDialog();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null) return;
        quest.getReward().setTitle(title);
        QuestSavedData.get(level).setDirty();
    }

    private void openEditItemCountDialog(int idx) {
        showEditItemCount = true;
        editItemCountIdx = idx;
        editItemCountY = this.parent.height / 2 - 30;
        int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
        QuestData quest = QuestManager.get(level, selectedId);
        int current = 1;
        if (quest != null && idx >= 0 && idx < quest.getReward().getItems().size()) {
            current = quest.getReward().getItems().get(idx).getCount();
        }
        editItemCountBox = new EditBox(this.minecraft.font, cx - 100, editItemCountY + 20, 200, 20, Component.literal("Count"));
        editItemCountBox.setMaxLength(10);
        editItemCountBox.setValue(String.valueOf(current));
        editItemCountBox.setFilter(s -> s.matches("\\d*"));
        parent.addWidget(editItemCountBox);
        parent.setFocusedWidget(editItemCountBox);
    }

    private void closeEditItemCountDialog() {
        showEditItemCount = false;
        editItemCountIdx = -1;
        if (editItemCountBox != null) { parent.removeEditorWidget(editItemCountBox); editItemCountBox = null; }
    }

    private void confirmEditItemCount() {
        if (editItemCountBox == null || editItemCountIdx < 0) { closeEditItemCountDialog(); return; }
        int count = 1;
        try { count = Integer.parseInt(editItemCountBox.getValue()); } catch (NumberFormatException ignored) {}
        closeEditItemCountDialog();
        QuestData quest = QuestManager.get(level, selectedId);
        if (quest == null || editItemCountIdx >= quest.getReward().getItems().size()) return;
        quest.getReward().getItems().get(editItemCountIdx).setCount(count);
        QuestSavedData.get(level).setDirty();
    }

    @Override
    public void tick() {
        if (saveDebounce > 0) {
            saveDebounce--;
            if (saveDebounce == 0) {
                sendSave();
            }
        }
        super.tick();
    }

    @Override
    public void removed() {
        if (saveDebounce > 0) {
            sendSave();
        }
        closeNameDialog();
        closeConfirmDialog();
        closeAddStageDialog();
        closeAddObjectiveDialog();
        closeAddPrereqDialog();
        closeAddItemRewardDialog();
        removeEditorWidgets();
        closeItemPicker();
        closeEditExpDialog();
        closeEditCommandDialog();
        closeEditTitleDialog();
        closeEditItemCountDialog();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        boolean modalOpen = showNameDialog || showConfirmDialog || showAddStage || showAddObjective || showAddPrereq || showAddItemReward || showItemPicker || showEditExp || showEditCommand || showEditTitle || showEditItemCount;

        boolean editorVisible = selectedId != null && !modalOpen;
        if (titleBox != null) titleBox.setVisible(editorVisible);
        if (descBox != null) descBox.setVisible(editorVisible);
        if (giverBox != null) giverBox.setVisible(editorVisible);
        if (turnInBox != null) turnInBox.setVisible(editorVisible);

        if (!modalOpen) {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);

            graphics.fill(toolbarX, y, rightX, y + h, 0xFF1A1A24);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, 0xFF2A2A3A);

            int btnSize = 24;
            int btnY = y + 8;

            boolean giveHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, giveHovered ? 0xFF2A4A2A : 0xFF1E281E);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, giveHovered ? 0xFF55FF55 : 0xFF44AA44);
            btnY += btnSize + 6;

            boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? 0xFF2A3A4A : 0xFF1E1E28);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
            graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            graphics.fill(rightX, y, x + w, y + h, 0xFF1E1E28);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);
            graphics.drawString(this.font, "Quests", rightX + 8, y + 6, 0xFF55AAFF);

            var quests = QuestManager.getAll(level);
            List<String> ids = new ArrayList<>(quests.keySet());
            Collections.sort(ids);

            int listH = h - 40;
            int listY = y + 20;
            int visible = Math.max(1, listH / ITEM_HEIGHT);

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(selectedId);

                int bg = selected ? 0xFF334466 : (hovered ? 0xFF2A2A3A : 0x001E1E28);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? 0xFF55AAFF : 0xFFCCCCCC);
            }

            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? 0xFF2A3A2A : 0xFF1E281E);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, 0xFF444455);
            graphics.drawCenteredString(this.font, "+ New Quest", rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, 0xFF55FF55);

            if (selectedId != null && quests.containsKey(selectedId)) {
                var quest = quests.get(selectedId);
                if (!selectedId.equals(editingQuestId)) {
                    removeEditorWidgets();
                    editingQuestId = selectedId;
                    createEditorWidgets(quest);
                }

                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, 0xFF111118);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, 0xFF333344);

                RenderSystem.enableScissor(
                        (int) (leftX * this.minecraft.getWindow().getGuiScale()),
                        (int) ((this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * this.minecraft.getWindow().getGuiScale()),
                        (int) (leftW * this.minecraft.getWindow().getGuiScale()),
                        (int) (leftH * this.minecraft.getWindow().getGuiScale())
                );

                int dy = leftY + 6 - editorScroll;

                graphics.drawString(font, "Title:", leftX + 4, dy, 0xFFAAAAAA);
                dy += 14;
                titleBox.setX(leftX + 4);
                titleBox.setY(dy);
                titleBox.setWidth(leftW - 8);
                titleBox.setVisible(true);
                dy += 22;

                graphics.drawString(font, "Description:", leftX + 4, dy, 0xFFAAAAAA);
                dy += 14;
                descBox.setX(leftX + 4);
                descBox.setY(dy);
                descBox.setWidth(leftW - 8);
                descBox.setHeight(50);
                descBox.setVisible(true);
                dy += 56;

                graphics.drawString(font, "Giver NPC:", leftX + 4, dy, 0xFFAAAAAA);
                dy += 14;
                giverBox.setX(leftX + 4);
                giverBox.setY(dy);
                giverBox.setWidth(leftW - 8);
                giverBox.setVisible(true);
                dy += 22;

                graphics.drawString(font, "Turn-in NPC:", leftX + 4, dy, 0xFFAAAAAA);
                dy += 14;
                turnInBox.setX(leftX + 4);
                turnInBox.setY(dy);
                turnInBox.setWidth(leftW - 8);
                turnInBox.setVisible(true);
                dy += 26;

                String[] tabs = {"Stages", "Rewards", "Prereqs"};
                int tabW = leftW / tabs.length;
                for (int i = 0; i < tabs.length; i++) {
                    int tx = leftX + 4 + i * tabW;
                    boolean th = mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= dy && mouseY <= dy + 18;
                    int tbg = editorTab == i ? 0xFF334466 : (th ? 0xFF2A3A4A : 0xFF1E1E28);
                    graphics.fill(tx, dy, tx + tabW - 2, dy + 18, tbg);
                    graphics.renderOutline(tx, dy, tabW - 2, 18, 0xFF444455);
                    graphics.drawCenteredString(font, tabs[i], tx + tabW / 2 - 1, dy + 5, editorTab == i ? 0xFF55AAFF : 0xFFCCCCCC);
                }
                dy += 22;

                if (editorTab == 0) {
                    dy = renderStages(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                } else if (editorTab == 1) {
                    dy = renderRewards(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                } else if (editorTab == 2) {
                    dy = renderPrerequisites(graphics, quest, leftX, dy, leftW, mouseX, mouseY);
                }

                RenderSystem.disableScissor();
            } else {
                removeEditorWidgets();
                editingQuestId = null;
                graphics.drawCenteredString(this.font, "Select a quest from the list", leftX + leftW / 2, y + h / 2, 0xFF555566);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);
        }

        if (showNameDialog) renderNameDialog(graphics, x, w, mouseX, mouseY);
        if (showConfirmDialog) renderConfirmDialog(graphics, x, w, mouseX, mouseY);
        if (showAddStage) renderAddStageDialog(graphics, x, w, mouseX, mouseY);
        if (showAddObjective) renderAddObjectiveDialog(graphics, x, w, mouseX, mouseY);
        if (showAddPrereq) renderAddPrereqDialog(graphics, x, w, mouseX, mouseY);
        if (showAddItemReward) renderAddItemRewardDialog(graphics, x, w, mouseX, mouseY);
        if (showAddObjectiveTypeMenu) renderObjectiveTypeMenu(graphics, mouseX, mouseY);
        if (showAddPrereqMenu) renderPrereqMenu(graphics, mouseX, mouseY);
        if (showItemPicker) renderItemPicker(graphics, x, w, mouseX, mouseY);
        if (showEditExp) renderEditExpDialog(graphics, x, w, mouseX, mouseY);
        if (showEditCommand) renderEditCommandDialog(graphics, x, w, mouseX, mouseY);
        if (showEditTitle) renderEditTitleDialog(graphics, x, w, mouseX, mouseY);
        if (showEditItemCount) renderEditItemCountDialog(graphics, x, w, mouseX, mouseY);
        if (showContextMenu && contextMenuItemId != null) renderContextMenu(graphics, mouseX, mouseY);
    }

    private int renderStages(GuiGraphics graphics, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addHovered = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addHovered ? 0xFF2A3A4A : 0xFF1E281E);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, 0xFF444455);
        graphics.drawCenteredString(font, "+ Add Stage", leftX + leftW / 2, dy + 5, 0xFF55FF55);
        dy += 22;

        for (int s = 0; s < quest.getStages().size(); s++) {
            QuestStage stage = quest.getStages().get(s);
            boolean expanded = expandedStage == s;
            boolean sh = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 18;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 18, sh ? 0xFF2A2A3A : 0xFF1A1A24);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 18, 0xFF444455);
            graphics.drawString(font, (expanded ? "v " : "> ") + stage.getId(), leftX + 8, dy + 5, expanded ? 0xFF55AAFF : 0xFFCCCCCC);

            boolean delH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 18, delH ? 0xFF4A2A2A : 0xFF281E1E);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 18, 0xFF444455);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 5, delH ? 0xFFFF5555 : 0xFFAA4444);
            dy += 20;

            if (expanded) {
                if (!stage.getDescription().isEmpty()) {
                    graphics.drawString(font, stage.getDescription(), leftX + 12, dy, 0xFF888888);
                    dy += 12;
                }

                boolean addObjH = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                graphics.fill(leftX + 12, dy, leftX + leftW - 12, dy + 16, addObjH ? 0xFF2A3A4A : 0xFF1E1E28);
                graphics.renderOutline(leftX + 12, dy, leftW - 24, 16, 0xFF444455);
                graphics.drawCenteredString(font, "+ Objective", leftX + leftW / 2, dy + 4, 0xFF55AAFF);
                dy += 18;

                for (int o = 0; o < stage.getObjectives().size(); o++) {
                    QuestObjective obj = stage.getObjectives().get(o);
                    boolean objExp = expandedObjective == o;
                    boolean oh = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 32 && mouseY >= dy && mouseY <= dy + 16;
                    graphics.fill(leftX + 12, dy, leftX + leftW - 32, dy + 16, oh ? 0xFF2A2A3A : 0xFF1A1A24);
                    graphics.renderOutline(leftX + 12, dy, leftW - 44, 16, 0xFF444455);
                    String label = (objExp ? "v " : "> ") + obj.getType().name() + ": " + obj.getTarget() + " (" + obj.getCurrentCount() + "/" + obj.getRequiredCount() + ")";
                    graphics.drawString(font, label, leftX + 16, dy + 4, 0xFFCCCCCC);

                    boolean objDelH = mouseX >= leftX + leftW - 30 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                    graphics.fill(leftX + leftW - 30, dy, leftX + leftW - 12, dy + 16, objDelH ? 0xFF4A2A2A : 0xFF281E1E);
                    graphics.renderOutline(leftX + leftW - 30, dy, 18, 16, 0xFF444455);
                    graphics.drawCenteredString(font, "x", leftX + leftW - 21, dy + 4, objDelH ? 0xFFFF5555 : 0xFFAA4444);
                    dy += 18;

                    if (objExp) {
                        if (!obj.getDescription().isEmpty()) {
                            graphics.drawString(font, obj.getDescription(), leftX + 20, dy, 0xFF888888);
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
        graphics.drawString(font, "Title:", leftX + 4, dy, 0xFFAAAAAA);
        dy += 14;
        boolean titleH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, titleH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, 0xFF444455);
        String title = quest.getReward().getTitle();
        graphics.drawString(font, title.isEmpty() ? "None" : title, leftX + 8, dy + 5, 0xFFCCCCCC);
        dy += 22;

        graphics.drawString(font, "Items:", leftX + 4, dy, 0xFFAAAAAA);
        dy += 14;

        for (int i = 0; i < quest.getReward().getItems().size(); i++) {
            QuestReward.ItemReward item = quest.getReward().getItems().get(i);
            boolean ih = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ih ? 0xFF2A2A3A : 0xFF1A1A24);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 16, 0xFF444455);
            graphics.drawString(font, item.getItemId() + " x" + item.getCount(), leftX + 8, dy + 4, 0xFFCCCCCC);

            boolean idelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, idelH ? 0xFF4A2A2A : 0xFF281E1E);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 16, 0xFF444455);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, idelH ? 0xFFFF5555 : 0xFFAA4444);
            dy += 18;
        }

        boolean addItemH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 16, addItemH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 16, 0xFF444455);
        graphics.drawCenteredString(font, "+ Item", leftX + leftW / 2, dy + 4, 0xFF55AAFF);
        dy += 20;

        graphics.drawString(font, "EXP:", leftX + 4, dy, 0xFFAAAAAA);
        dy += 14;
        boolean expH = mouseX >= leftX + 4 && mouseX <= leftX + 80 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + 80, dy + 18, expH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(leftX + 4, dy, 76, 18, 0xFF444455);
        graphics.drawString(font, String.valueOf(quest.getReward().getExp()), leftX + 8, dy + 5, 0xFFCCCCCC);
        dy += 22;

        graphics.drawString(font, "Command:", leftX + 4, dy, 0xFFAAAAAA);
        dy += 14;
        boolean cmdH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, cmdH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, 0xFF444455);
        String cmd = quest.getReward().getCommand();
        graphics.drawString(font, cmd.isEmpty() ? "None" : (cmd.length() > 30 ? cmd.substring(0, 30) + "..." : cmd), leftX + 8, dy + 5, 0xFFCCCCCC);
        dy += 22;

        return dy;
    }

    private int renderPrerequisites(GuiGraphics graphics, QuestData quest, int leftX, int dy, int leftW, int mouseX, int mouseY) {
        boolean addH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
        graphics.fill(leftX + 4, dy, leftX + leftW - 4, dy + 18, addH ? 0xFF2A3A4A : 0xFF1E281E);
        graphics.renderOutline(leftX + 4, dy, leftW - 8, 18, 0xFF444455);
        graphics.drawCenteredString(font, "+ Add Prerequisite", leftX + leftW / 2, dy + 5, 0xFF55FF55);
        dy += 22;

        for (int i = 0; i < quest.getPrerequisites().size(); i++) {
            String prereq = quest.getPrerequisites().get(i);
            boolean ph = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + 4, dy, leftX + leftW - 24, dy + 16, ph ? 0xFF2A2A3A : 0xFF1A1A24);
            graphics.renderOutline(leftX + 4, dy, leftW - 28, 16, 0xFF444455);
            graphics.drawString(font, prereq, leftX + 8, dy + 4, 0xFFCCCCCC);

            boolean pdelH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 16;
            graphics.fill(leftX + leftW - 22, dy, leftX + leftW - 4, dy + 16, pdelH ? 0xFF4A2A2A : 0xFF281E1E);
            graphics.renderOutline(leftX + leftW - 22, dy, 18, 16, 0xFF444455);
            graphics.drawCenteredString(font, "x", leftX + leftW - 13, dy + 4, pdelH ? 0xFFFF5555 : 0xFFAA4444);
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

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename Quest" : "New Quest Name", cx, dy + 6, 0xFF55AAFF);

        if (nameInputBox != null) {
            nameInputBox.setX(cx - 100);
            nameInputBox.setY(dy + 24);
        }

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okHovered ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename" : "Create", cx - 26, dy + 57, okHovered ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelHovered ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelHovered ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderConfirmDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 70;
        int dx = cx - dw / 2;
        int dy = confirmDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFFFF5555);
        graphics.drawCenteredString(this.font, "Delete \"" + confirmDialogId + "\"?", cx, dy + 8, 0xFFFF5555);

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx - 50, dy + 38, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Delete", cx - 26, dy + 43, okHovered ? 0xFFFF5555 : 0xFFAA4444);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(cx + 2, dy + 38, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 43, cancelHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
    }

    private void renderAddStageDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addStageY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "New Stage", cx, dy + 6, 0xFF55AAFF);

        if (stageIdBox != null) { stageIdBox.setX(cx - 100); stageIdBox.setY(dy + 24); }
        if (stageDescBox != null) { stageDescBox.setX(cx - 100); stageDescBox.setY(dy + 52); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 74, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Add", cx - 26, dy + 79, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 74, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 79, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderAddObjectiveDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 140;
        int dx = cx - dw / 2;
        int dy = addObjectiveY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "New Objective", cx, dy + 6, 0xFF55AAFF);

        QuestObjectiveType[] types = QuestObjectiveType.values();
        boolean typeH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        graphics.fill(cx - 100, dy + 24, cx + 100, dy + 42, typeH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(cx - 100, dy + 24, 200, 18, 0xFF444455);
        graphics.drawCenteredString(font, types[objectiveTypeIndex].name(), cx, dy + 28, 0xFFCCCCCC);

        if (objTargetBox != null) { objTargetBox.setX(cx - 100); objTargetBox.setY(dy + 46); }
        if (objCountBox != null) { objCountBox.setX(cx - 100); objCountBox.setY(dy + 70); }
        if (objDescBox != null) { objDescBox.setX(cx - 100); objDescBox.setY(dy + 94); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 116 && mouseY <= dy + 138;
        graphics.fill(cx - 50, dy + 116, cx - 2, dy + 138, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 116, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Add", cx - 26, dy + 121, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 116 && mouseY <= dy + 138;
        graphics.fill(cx + 2, dy + 116, cx + 50, dy + 138, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 116, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 121, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderAddPrereqDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = addPrereqY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Add Prerequisite", cx, dy + 6, 0xFF55AAFF);

        List<String> ids = new ArrayList<>(QuestManager.getAll(level).keySet());
        Collections.sort(ids);
        String current = prereqDropdownIndex >= 0 && prereqDropdownIndex < ids.size() ? ids.get(prereqDropdownIndex) : "None";
        boolean dropH = mouseX >= cx - 100 && mouseX <= cx + 100 && mouseY >= dy + 24 && mouseY <= dy + 42;
        graphics.fill(cx - 100, dy + 24, cx + 100, dy + 42, dropH ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(cx - 100, dy + 24, 200, 18, 0xFF444455);
        graphics.drawCenteredString(font, current, cx, dy + 28, 0xFFCCCCCC);

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Add", cx - 26, dy + 57, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderAddItemRewardDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 100;
        int dx = cx - dw / 2;
        int dy = addItemRewardY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Add Item Reward", cx, dy + 6, 0xFF55AAFF);

        if (rewardItemBox != null) { rewardItemBox.setX(cx - 100); rewardItemBox.setY(dy + 24); }
        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 48); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx - 50, dy + 74, cx - 2, dy + 96, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 74, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Add", cx - 26, dy + 79, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96;
        graphics.fill(cx + 2, dy + 74, cx + 50, dy + 96, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 74, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 79, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderItemPicker(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 200;
        int dx = cx - dw / 2;
        int dy = itemPickerY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Select Item", cx, dy + 6, 0xFF55AAFF);

        if (itemPickerSearchBox != null) { itemPickerSearchBox.setX(cx - 100); itemPickerSearchBox.setY(dy + 20); }

        int listY = dy + 44;
        int listH = 108;
        int scrollBarW = 6;
        int listX = dx + 4;
        int listW = dw - 8 - scrollBarW;

        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF111118);
        graphics.renderOutline(listX, listY, listW, listH, 0xFF444455);

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
            int bg = selected ? 0xFF334466 : (h ? 0xFF2A2A3A : 0xFF111118);
            graphics.fill(listX + 1, ry, listX + listW - 1, ry + rowH, bg);
            graphics.renderItem(new ItemStack(item), listX + 4, ry + 1);
            graphics.drawString(font, item.getDescription().getString(), listX + 22, ry + 5, selected ? 0xFF55AAFF : 0xFFCCCCCC);
        }

        RenderSystem.disableScissor();

        int scrollX = listX + listW;
        graphics.fill(scrollX, listY, scrollX + scrollBarW, listY + listH, 0xFF111118);
        graphics.renderOutline(scrollX, listY, scrollBarW, listH, 0xFF444455);

        int maxScroll = Math.max(0, itemPickerFiltered.size() - visibleRows);
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (visibleRows * listH) / itemPickerFiltered.size());
            int thumbY = listY + (itemPickerScroll * (listH - thumbH)) / maxScroll;
            graphics.fill(scrollX, thumbY, scrollX + scrollBarW, thumbY + thumbH, 0xFF666688);
        }

        if (rewardItemCountBox != null) { rewardItemCountBox.setX(cx - 100); rewardItemCountBox.setY(dy + 156); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 180 && mouseY <= dy + 198;
        graphics.fill(cx - 50, dy + 180, cx - 2, dy + 198, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 180, 48, 18, 0xFF444455);
        graphics.drawCenteredString(this.font, "Add", cx - 26, dy + 183, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 180 && mouseY <= dy + 198;
        graphics.fill(cx + 2, dy + 180, cx + 50, dy + 198, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 180, 48, 18, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 183, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderEditExpDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editExpY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Edit EXP", cx, dy + 6, 0xFF55AAFF);

        if (editExpBox != null) { editExpBox.setX(cx - 100); editExpBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 57, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderEditCommandDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editCmdY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Edit Command", cx, dy + 6, 0xFF55AAFF);

        if (editCmdBox != null) { editCmdBox.setX(cx - 100); editCmdBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 57, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderEditTitleDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editTitleY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Edit Reward Title", cx, dy + 6, 0xFF55AAFF);

        if (editTitleBox != null) { editTitleBox.setX(cx - 100); editTitleBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 57, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderEditItemCountDialog(GuiGraphics graphics, int x, int w, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = editItemCountY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, "Edit Item Count", cx, dy + 6, 0xFF55AAFF);

        if (editItemCountBox != null) { editItemCountBox.setX(cx - 100); editItemCountBox.setY(dy + 24); }

        boolean okH = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okH ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 57, okH ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelH = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
        graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelH ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 52, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 57, cancelH ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderObjectiveTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        QuestObjectiveType[] types = QuestObjectiveType.values();
        int mw = 120;
        int mh = types.length * 20 + 4;
        graphics.fill(objTypeMenuX, objTypeMenuY, objTypeMenuX + mw, objTypeMenuY + mh, 0xFF222222);
        graphics.renderOutline(objTypeMenuX, objTypeMenuY, mw, mh, 0xFF666666);
        int cy = objTypeMenuY + 2;
        for (int i = 0; i < types.length; i++) {
            boolean h = mouseX >= objTypeMenuX && mouseX <= objTypeMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            graphics.fill(objTypeMenuX + 1, cy, objTypeMenuX + mw - 1, cy + 18, h ? 0xFF444444 : 0xFF222222);
            graphics.drawString(font, types[i].name(), objTypeMenuX + 6, cy + 5, h ? 0xFFFFFFFF : 0xFFCCCCCC);
            cy += 20;
        }
    }

    private void renderPrereqMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> ids = new ArrayList<>(QuestManager.getAll(level).keySet());
        Collections.sort(ids);
        int mw = 140;
        int mh = Math.min(ids.size(), 10) * 20 + 4;
        graphics.fill(prereqMenuX, prereqMenuY, prereqMenuX + mw, prereqMenuY + mh, 0xFF222222);
        graphics.renderOutline(prereqMenuX, prereqMenuY, mw, mh, 0xFF666666);
        int cy = prereqMenuY + 2;
        for (int i = 0; i < Math.min(ids.size(), 10); i++) {
            boolean h = mouseX >= prereqMenuX && mouseX <= prereqMenuX + mw && mouseY >= cy && mouseY <= cy + 18;
            graphics.fill(prereqMenuX + 1, cy, prereqMenuX + mw - 1, cy + 18, h ? 0xFF444444 : 0xFF222222);
            graphics.drawString(font, ids.get(i), prereqMenuX + 6, cy + 5, h ? 0xFFFFFFFF : 0xFFCCCCCC);
            cy += 20;
        }
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] items = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
        boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
        int wctx = 100;
        int hctx = (canPaste ? 5 : 4) * 22 + 4;
        graphics.fill(contextMenuX, contextMenuY, contextMenuX + wctx, contextMenuY + hctx, 0xFF222222);
        graphics.renderOutline(contextMenuX, contextMenuY, wctx, hctx, 0xFF666666);

        int cy = contextMenuY + 2;
        for (String item : items) {
            if (item.equals("Paste") && !canPaste) continue;
            boolean hovered = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            int bg = hovered ? 0xFF444444 : 0xFF222222;
            int tc = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            if (item.equals("Delete")) tc = hovered ? 0xFFFF6666 : 0xFFCC4444;
            graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, bg);
            graphics.drawString(font, item, contextMenuX + 6, cy + 6, tc);
            cy += 22;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNameDialog) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
            int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
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
            if (nameInputBox != null && mouseX >= nameInputBox.getX() && mouseX <= nameInputBox.getX() + nameInputBox.getWidth() && mouseY >= nameInputBox.getY() && mouseY <= nameInputBox.getY() + nameInputBox.getHeight()) {
                parent.setFocusedWidget(nameInputBox);
                return nameInputBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showConfirmDialog) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
            int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
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

        if (showAddStage) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = addStageY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                confirmAddStage();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                closeAddStageDialog();
                return true;
            }
            if (stageIdBox != null && mouseX >= stageIdBox.getX() && mouseX <= stageIdBox.getX() + stageIdBox.getWidth() && mouseY >= stageIdBox.getY() && mouseY <= stageIdBox.getY() + stageIdBox.getHeight()) {
                parent.setFocusedWidget(stageIdBox);
                return stageIdBox.mouseClicked(mouseX, mouseY, button);
            }
            if (stageDescBox != null && mouseX >= stageDescBox.getX() && mouseX <= stageDescBox.getX() + stageDescBox.getWidth() && mouseY >= stageDescBox.getY() && mouseY <= stageDescBox.getY() + stageDescBox.getHeight()) {
                parent.setFocusedWidget(stageDescBox);
                return stageDescBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showAddObjective) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
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
            if (objTargetBox != null && mouseX >= objTargetBox.getX() && mouseX <= objTargetBox.getX() + objTargetBox.getWidth() && mouseY >= objTargetBox.getY() && mouseY <= objTargetBox.getY() + objTargetBox.getHeight()) {
                parent.setFocusedWidget(objTargetBox);
                return objTargetBox.mouseClicked(mouseX, mouseY, button);
            }
            if (objCountBox != null && mouseX >= objCountBox.getX() && mouseX <= objCountBox.getX() + objCountBox.getWidth() && mouseY >= objCountBox.getY() && mouseY <= objCountBox.getY() + objCountBox.getHeight()) {
                parent.setFocusedWidget(objCountBox);
                return objCountBox.mouseClicked(mouseX, mouseY, button);
            }
            if (objDescBox != null && mouseX >= objDescBox.getX() && mouseX <= objDescBox.getX() + objDescBox.getWidth() && mouseY >= objDescBox.getY() && mouseY <= objDescBox.getY() + objDescBox.getHeight()) {
                parent.setFocusedWidget(objDescBox);
                return objDescBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showAddPrereq) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
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

        if (showAddItemReward) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = addItemRewardY;

            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                confirmAddItemReward();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 74 && mouseY <= dy + 96) {
                closeAddItemRewardDialog();
                return true;
            }
            if (rewardItemBox != null && mouseX >= rewardItemBox.getX() && mouseX <= rewardItemBox.getX() + rewardItemBox.getWidth() && mouseY >= rewardItemBox.getY() && mouseY <= rewardItemBox.getY() + rewardItemBox.getHeight()) {
                parent.setFocusedWidget(rewardItemBox);
                return rewardItemBox.mouseClicked(mouseX, mouseY, button);
            }
            if (rewardItemCountBox != null && mouseX >= rewardItemCountBox.getX() && mouseX <= rewardItemCountBox.getX() + rewardItemCountBox.getWidth() && mouseY >= rewardItemCountBox.getY() && mouseY <= rewardItemCountBox.getY() + rewardItemCountBox.getHeight()) {
                parent.setFocusedWidget(rewardItemCountBox);
                return rewardItemCountBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showItemPicker) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
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
            if (itemPickerSearchBox != null && mouseX >= itemPickerSearchBox.getX() && mouseX <= itemPickerSearchBox.getX() + itemPickerSearchBox.getWidth() && mouseY >= itemPickerSearchBox.getY() && mouseY <= itemPickerSearchBox.getY() + itemPickerSearchBox.getHeight()) {
                parent.setFocusedWidget(itemPickerSearchBox);
                return itemPickerSearchBox.mouseClicked(mouseX, mouseY, button);
            }
            if (rewardItemCountBox != null && mouseX >= rewardItemCountBox.getX() && mouseX <= rewardItemCountBox.getX() + rewardItemCountBox.getWidth() && mouseY >= rewardItemCountBox.getY() && mouseY <= rewardItemCountBox.getY() + rewardItemCountBox.getHeight()) {
                parent.setFocusedWidget(rewardItemCountBox);
                return rewardItemCountBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showEditExp) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = editExpY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditExp();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditExpDialog();
                return true;
            }
            if (editExpBox != null && mouseX >= editExpBox.getX() && mouseX <= editExpBox.getX() + editExpBox.getWidth() && mouseY >= editExpBox.getY() && mouseY <= editExpBox.getY() + editExpBox.getHeight()) {
                parent.setFocusedWidget(editExpBox);
                return editExpBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showEditCommand) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = editCmdY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditCommand();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditCommandDialog();
                return true;
            }
            if (editCmdBox != null && mouseX >= editCmdBox.getX() && mouseX <= editCmdBox.getX() + editCmdBox.getWidth() && mouseY >= editCmdBox.getY() && mouseY <= editCmdBox.getY() + editCmdBox.getHeight()) {
                parent.setFocusedWidget(editCmdBox);
                return editCmdBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showEditTitle) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = editTitleY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditTitle();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditTitleDialog();
                return true;
            }
            if (editTitleBox != null && mouseX >= editTitleBox.getX() && mouseX <= editTitleBox.getX() + editTitleBox.getWidth() && mouseY >= editTitleBox.getY() && mouseY <= editTitleBox.getY() + editTitleBox.getHeight()) {
                parent.setFocusedWidget(editTitleBox);
                return editTitleBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (showEditItemCount) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
            int dy = editItemCountY;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmEditItemCount();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeEditItemCountDialog();
                return true;
            }
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
            List<String> ids = new ArrayList<>(QuestManager.getAll(level).keySet());
            Collections.sort(ids);
            int mw = 140;
            int cy = prereqMenuY + 2;
            for (int i = 0; i < Math.min(ids.size(), 10); i++) {
                if (mouseX >= prereqMenuX && mouseX <= prereqMenuX + mw && mouseY >= cy && mouseY <= cy + 18) {
                    prereqDropdownIndex = i;
                    showAddPrereqMenu = false;
                    return true;
                }
                cy += 20;
            }
            showAddPrereqMenu = false;
            return true;
        }

        if (showContextMenu) {
            String[] items = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int cy = contextMenuY + 2;
            for (String item : items) {
                if (item.equals("Paste") && !canPaste) continue;
                if (mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20) {
                    switch (item) {
                        case "Copy" -> copyItem(contextMenuItemId);
                        case "Paste" -> pasteItem();
                        case "Rename" -> openNameDialog("rename", contextMenuItemId);
                        case "Duplicate" -> duplicateItem(contextMenuItemId);
                        case "Delete" -> openConfirmDialog("delete", contextMenuItemId);
                    }
                    closeContextMenu();
                    return true;
                }
                cy += 22;
            }
            closeContextMenu();
            return true;
        }

        if (button != 0) return false;

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

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

        var quests = QuestManager.getAll(level);
        List<String> ids = new ArrayList<>(quests.keySet());
        Collections.sort(ids);

        int listH = h - 40;
        int listY = y + 20;
        int visible = Math.max(1, listH / ITEM_HEIGHT);

        for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (!id.equals(selectedId)) {
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

        if (selectedId != null && quests.containsKey(selectedId)) {
            var quest = quests.get(selectedId);

            int dy = leftY + 6 - editorScroll;
            dy += 14;
            dy += 22;
            dy += 14;
            dy += 56;
            dy += 14;
            dy += 22;
            dy += 14;
            dy += 26;
            String[] tabs = {"Stages", "Rewards", "Prereqs"};
            int tabW = leftW / tabs.length;
            for (int i = 0; i < tabs.length; i++) {
                int tx = leftX + 4 + i * tabW;
                if (mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= dy && mouseY <= dy + 18) {
                    editorTab = i;
                    return true;
                }
            }
            dy += 22;

            if (editorTab == 0) {
                boolean addStageH = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
                if (addStageH) {
                    openAddStageDialog();
                    return true;
                }
                dy += 22;

                for (int s = 0; s < quest.getStages().size(); s++) {
                    QuestStage stage = quest.getStages().get(s);
                    boolean expanded = expandedStage == s;
                    boolean sh = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 24 && mouseY >= dy && mouseY <= dy + 18;
                    boolean delH = mouseX >= leftX + leftW - 22 && mouseX <= leftX + leftW - 4 && mouseY >= dy && mouseY <= dy + 18;
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

                        boolean addObjH = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
                        if (addObjH) {
                            openAddObjectiveDialog(s);
                            return true;
                        }
                        dy += 18;

                        for (int o = 0; o < stage.getObjectives().size(); o++) {
                            boolean objExp = expandedObjective == o;
                            boolean oh = mouseX >= leftX + 12 && mouseX <= leftX + leftW - 32 && mouseY >= dy && mouseY <= dy + 16;
                            boolean objDelH = mouseX >= leftX + leftW - 30 && mouseX <= leftX + leftW - 12 && mouseY >= dy && mouseY <= dy + 16;
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
            } else if (editorTab == 2) {
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
        if (showNameDialog || showConfirmDialog || showContextMenu || showAddStage || showAddObjective || showAddPrereq || showAddItemReward || showAddObjectiveTypeMenu || showAddPrereqMenu || showItemPicker || showEditExp || showEditCommand || showEditTitle || showEditItemCount) return true;

        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
            int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
            int rightX = x + w - RIGHT_PANEL_WIDTH;

            var quests = QuestManager.getAll(level);
            List<String> ids = new ArrayList<>(quests.keySet());
            Collections.sort(ids);

            int listH = h - 40;
            int listY = y + 20;
            int visible = Math.max(1, listH / ITEM_HEIGHT);

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    openContextMenu(ids.get(i), (int) mouseX, (int) mouseY);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showNameDialog) {
            if (nameInputBox != null && nameInputBox.isFocused()) {
                return nameInputBox.charTyped(codePoint, modifiers);
            }
            return true;
        }
        if (showAddStage) {
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.charTyped(codePoint, modifiers);
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showAddObjective) {
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.charTyped(codePoint, modifiers);
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.charTyped(codePoint, modifiers);
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showAddItemReward) {
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.charTyped(codePoint, modifiers);
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showItemPicker) {
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.charTyped(codePoint, modifiers);
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showEditExp) {
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showEditCommand) {
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showEditTitle) {
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.charTyped(codePoint, modifiers);
            return true;
        }
        if (showEditItemCount) {
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.charTyped(codePoint, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showNameDialog) {
            if (keyCode == 256) {
                closeNameDialog();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                confirmNameDialog();
                return true;
            }
            if (nameInputBox != null && nameInputBox.isFocused()) {
                return nameInputBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (showConfirmDialog) {
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
        if (showAddStage) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddStage();
                return true;
            }
            if (keyCode == 256) {
                closeAddStageDialog();
                return true;
            }
            if (stageIdBox != null && stageIdBox.isFocused()) return stageIdBox.keyPressed(keyCode, scanCode, modifiers);
            if (stageDescBox != null && stageDescBox.isFocused()) return stageDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showAddObjective) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddObjective();
                return true;
            }
            if (keyCode == 256) {
                closeAddObjectiveDialog();
                return true;
            }
            if (objTargetBox != null && objTargetBox.isFocused()) return objTargetBox.keyPressed(keyCode, scanCode, modifiers);
            if (objCountBox != null && objCountBox.isFocused()) return objCountBox.keyPressed(keyCode, scanCode, modifiers);
            if (objDescBox != null && objDescBox.isFocused()) return objDescBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showAddPrereq) {
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
        if (showAddItemReward) {
            if (keyCode == 257 || keyCode == 335) {
                confirmAddItemReward();
                return true;
            }
            if (keyCode == 256) {
                closeAddItemRewardDialog();
                return true;
            }
            if (rewardItemBox != null && rewardItemBox.isFocused()) return rewardItemBox.keyPressed(keyCode, scanCode, modifiers);
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showItemPicker) {
            if (keyCode == 257 || keyCode == 335) {
                confirmItemPicker();
                return true;
            }
            if (keyCode == 256) {
                closeItemPicker();
                return true;
            }
            if (itemPickerSearchBox != null && itemPickerSearchBox.isFocused()) return itemPickerSearchBox.keyPressed(keyCode, scanCode, modifiers);
            if (rewardItemCountBox != null && rewardItemCountBox.isFocused()) return rewardItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showEditExp) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditExp();
                return true;
            }
            if (keyCode == 256) {
                closeEditExpDialog();
                return true;
            }
            if (editExpBox != null && editExpBox.isFocused()) return editExpBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showEditCommand) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditCommand();
                return true;
            }
            if (keyCode == 256) {
                closeEditCommandDialog();
                return true;
            }
            if (editCmdBox != null && editCmdBox.isFocused()) return editCmdBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showEditTitle) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditTitle();
                return true;
            }
            if (keyCode == 256) {
                closeEditTitleDialog();
                return true;
            }
            if (editTitleBox != null && editTitleBox.isFocused()) return editTitleBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (showEditItemCount) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEditItemCount();
                return true;
            }
            if (keyCode == 256) {
                closeEditItemCountDialog();
                return true;
            }
            if (editItemCountBox != null && editItemCountBox.isFocused()) return editItemCountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showNameDialog || showConfirmDialog || showContextMenu || showAddStage || showAddObjective || showAddPrereq || showAddItemReward || showAddObjectiveTypeMenu || showAddPrereqMenu) return true;

        if (showItemPicker) {
            int cx = DashboardScreen.SIDEBAR_WIDTH + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2;
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

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            var quests = QuestManager.getAll(level);
            int listH = h - 40;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int maxScroll = Math.max(0, quests.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }

        int leftX = x + 4;
        int leftY = y + 4;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;

        if (mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            if (delta > 0) editorScroll = Math.max(0, editorScroll - 20);
            else editorScroll += 20;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return showNameDialog || showConfirmDialog || showContextMenu || showAddStage || showAddObjective || showAddPrereq || showAddItemReward || showAddObjectiveTypeMenu || showAddPrereqMenu || showItemPicker || showEditExp || showEditCommand || showEditTitle || showEditItemCount;
    }
}