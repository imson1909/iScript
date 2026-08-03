package com.iscript.iscript.gui.screen.ListSubScreen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.api.triggers.TriggerData;
import com.iscript.iscript.api.triggers.TriggerType;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.I18n;
import com.iscript.iscript.gui.screen.SubScreenLifecycle;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.ContextMenu;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Trigger extends DashboardScreen.SubScreen {
    private final SubScreenLifecycle life = new SubScreenLifecycle(this);
    private static final int ITEM_HEIGHT = 24;
    private static final int RIGHT_PANEL_WIDTH = 220;
    private static final int TYPE_PICKER_W = 400;
    private static final int TYPE_PICKER_ROW_H = 20;
    private static final int SCROLLBAR_W = 6;
    private static final int SCRIPT_DROPDOWN_H = 20;
    private static final int SCRIPT_DROPDOWN_MAX_VISIBLE = 6;
    private static final Gson GSON = new Gson();

    private final List<TriggerData> triggers = new ArrayList<>();
    private TriggerData selected = null;

    private ContextMenu contextMenu = new ContextMenu();
    private String contextMenuTriggerId = null;

    private String confirmDialogId = null;

    private boolean typePickerOpen = false;
    private int typePickerScroll = 0;
    private boolean typePickerDragging = false;

    private List<String> scriptIds = new ArrayList<>();
    private boolean scriptDropdownOpen = false;
    private int scriptDropdownScroll = 0;
    private int lastScriptCacheSize = -1;

    public Trigger(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        life.init();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        life.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.trigger.list.search"));

        life.modals().register("confirm",
                () -> life.state().modalOpenFlags.getOrDefault("confirm", false),
                v -> life.state().modalOpenFlags.put("confirm", v),
                () -> {},
                () -> {},
                "confirm"
        );

        life.state().modalOpenFlags.put("confirm", false);
        life.save().clearDirty();

        typePickerOpen = false;
        typePickerDragging = false;
        scriptDropdownOpen = false;
        scriptDropdownScroll = 0;
        refreshScriptIds();
        if (scriptIds.isEmpty()) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_TRIGGERS, new CompoundTag()));
        if (selected != null) {
            buildEditor(selected);
        }
    }

    @Override
    public void tick() {
        life.tick(this::doSave);
        boolean anyFocused = (life.search().box() != null && life.search().box().isFocused())
                || (life.editors().box("id") != null && life.editors().box("id").isFocused())
                || (life.editors().box("function") != null && life.editors().box("function").isFocused());
        if (life.save().isDirty() && !anyFocused && !typePickerOpen && !scriptDropdownOpen) {
            doSave();
        }
        if (anyFocused) life.save().markDirty();
        if (life.search().box() == null && this.minecraft != null) {
            life.search().recreateIfMissing();
        }
        var cache = ScriptGraphManager.getClientCache();
        if (cache.size() != lastScriptCacheSize) {
            lastScriptCacheSize = cache.size();
            refreshScriptIds();
        }
        super.tick();
    }

    private void refreshScriptIds() {
        scriptIds = new ArrayList<>(ScriptGraphManager.getClientCache().keySet());
        Collections.sort(scriptIds);
    }

    private List<TriggerData> filteredTriggers() {
        String filter = life.search().box() != null ? life.search().box().getValue().trim().toLowerCase() : life.state().lastSearch.trim().toLowerCase();
        List<TriggerData> result = new ArrayList<>();
        for (TriggerData t : triggers) {
            if (filter.isEmpty()
                    || t.getId().toLowerCase().contains(filter)
                    || t.getType().getDisplayName().toLowerCase().contains(filter)
                    || t.getScriptId().toLowerCase().contains(filter)) {
                result.add(t);
            }
        }
        result.sort(Comparator.comparing(TriggerData::getId, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private void doSave() {
        if (!life.save().isDirty() || selected == null) return;
        life.save().clearDirty();
        applyEditorToData();
        CompoundTag tag = new CompoundTag();
        tag.putString("json", GSON.toJson(selected.toJson()));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_TRIGGER, tag));
    }

    private void requestDelete(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_TRIGGER, tag));
        triggers.removeIf(t -> t.getId().equals(id));
        if (selected != null && selected.getId().equals(id)) {
            selected = null;
            clearEditorWidgets();
        }
    }

    private void clearEditorWidgets() {
        life.editors().removeAll();
    }

    private void buildEditor(TriggerData data) {
        clearEditorWidgets();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        boolean isNew = !triggers.stream().anyMatch(t -> t.getId().equals(data.getId()) && t != data);

        EditBox idBox = life.editors().addBox("id", leftX, leftY + 12, leftW, 18, I18n.t("iscript.trigger.editor.label.id"), data.getId());
        if (idBox != null) {
            idBox.setMaxLength(64);
            idBox.setEditable(isNew);
        }

        int funcBoxX = leftX + leftW / 2 + 4;
        int funcBoxW = leftW / 2 - 4;
        EditBox funcBox = life.editors().addBox("function", funcBoxX, leftY + 84, funcBoxW, 18, I18n.t("iscript.trigger.editor.label.function"), data.getFunctionName());
        if (funcBox != null) funcBox.setMaxLength(128);
    }

    private void switchToTrigger(TriggerData data) {
        doSave();
        selected = data;
        clearEditorWidgets();
        life.save().clearDirty();
        buildEditor(data);
    }

    private void addNewTrigger() {
        int index = 1;
        String id = "trigger_1";
        while (true) {
            boolean exists = false;
            for (TriggerData t : triggers) {
                if (t.getId().equals(id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) break;
            index++;
            id = "trigger_" + index;
        }
        TriggerData t = new TriggerData(id, TriggerType.PLAYER_TICK, "", "");
        triggers.add(t);
        switchToTrigger(t);
        life.save().markDirty();
    }

    private void openConfirmDialog(String id) {
        confirmDialogId = id;
        life.modals().open("confirm");
    }

    private void closeConfirmDialog() {
        life.modals().close("confirm");
        confirmDialogId = null;
    }

    private void executeConfirm() {
        if (confirmDialogId != null) requestDelete(confirmDialogId);
        closeConfirmDialog();
    }

    private void openTypePicker() {
        typePickerOpen = true;
        typePickerDragging = false;
        if (selected != null) {
            typePickerScroll = Math.max(0, selected.getType().ordinal() - 5);
        }
        setWidgetsVisible(false);
    }

    private void closeTypePicker() {
        typePickerOpen = false;
        typePickerDragging = false;
        setWidgetsVisible(true);
    }

    private void setWidgetsVisible(boolean visible) {
        life.search().setVisible(visible);
        EditBox idBox = life.editors().box("id");
        EditBox funcBox = life.editors().box("function");
        if (idBox != null) idBox.setVisible(visible);
        if (funcBox != null) funcBox.setVisible(visible);
    }

    private void applyEditorToData() {
        if (selected == null) return;
        boolean isNew = !triggers.stream().anyMatch(t -> t.getId().equals(selected.getId()) && t != selected);
        EditBox idBox = life.editors().box("id");
        EditBox funcBox = life.editors().box("function");
        if (idBox != null && isNew) {
            String newId = idBox.getValue().trim();
            if (!newId.isEmpty()) selected.setId(newId);
        }
        if (funcBox != null) selected.setFunctionName(funcBox.getValue().trim());
    }

    public void receiveTriggers(CompoundTag data) {
        triggers.clear();
        String json = data.getString("triggers_json");
        if (!json.isEmpty()) {
            try {
                JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                for (JsonElement e : arr) {
                    TriggerData t = new TriggerData();
                    t.fromJson(e.getAsJsonObject());
                    triggers.add(t);
                }
            } catch (Exception ex) {
                IScriptMod.LOGGER.error("Failed to parse triggers JSON", ex);
            }
        }
        if (selected != null && triggers.stream().noneMatch(t -> t.getId().equals(selected.getId()))) {
            selected = null;
            clearEditorWidgets();
        }
    }

    @Override
    public void removed() {
        doSave();
        life.removed();
        closeConfirmDialog();
        closeTypePicker();
        clearEditorWidgets();
        contextMenu.close();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (life.modals().isOpen("confirm")) {
            renderConfirmDialog(g, mx, my, x, w);
            return;
        }

        if (typePickerOpen) {
            renderTypePicker(g, mx, my, x, y, w, h);
            return;
        }

        g.fill(x, y, x + w, y + h, Theme.BG_INNER);
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        g.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
        g.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);

        g.drawString(this.font, I18n.s("iscript.trigger.list.title"), rightX + 8, y + 26, Theme.ACCENT);

        if (life.search().box() != null) {
            life.search().setPos(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16);
            life.search().setVisible(true);
        }

        List<TriggerData> list = filteredTriggers();
        int listH = h - 68;
        int listY = y + 42;
        int scroll = life.selection().scroll();

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, list.size()); i++) {
            TriggerData t = list.get(i);
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2;
            boolean sel = selected != null && t.getId().equals(selected.getId());
            int bg = sel ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
            g.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);

            int color = t.isEnabled() ? (sel ? Theme.ACCENT : Theme.TEXT) : Theme.TEXT_MUTE;
            String line1 = t.getId();
            String line2 = t.getType().getDisplayName() + " -> " + t.getScriptId();
            if (!t.getFunctionName().isEmpty()) line2 += ":" + t.getFunctionName();

            g.drawString(this.font, line1, rightX + 8, rowY + 2, color);
            g.drawString(this.font, line2, rightX + 8, rowY + 12, t.isEnabled() ? Theme.TEXT_MUTE : 0xFF666666);
        }

        int newY = y + h - 28;
        boolean newHovered = mx >= rightX + 4 && mx <= x + w - 4 && my >= newY && my <= newY + 22;
        g.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.trigger.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);

        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        if (selected != null) {
            g.drawString(this.font, I18n.s("iscript.trigger.editor.label.id"), leftX, leftY, Theme.TEXT_MUTE);
            g.drawString(this.font, I18n.s("iscript.trigger.editor.label.type"), leftX, leftY + 36, Theme.TEXT_MUTE);
            g.drawString(this.font, I18n.s("iscript.trigger.editor.label.script"), leftX, leftY + 72, Theme.TEXT_MUTE);
            g.drawString(this.font, I18n.s("iscript.trigger.editor.label.function"), leftX + leftW / 2 + 4, leftY + 72, Theme.TEXT_MUTE);

            EditBox idBox = life.editors().box("id");
            EditBox functionNameBox = life.editors().box("function");

            if (idBox != null) {
                idBox.setX(leftX);
                idBox.setY(leftY + 12);
                idBox.setWidth(leftW);
            }

            String typeName = selected.getType().getDisplayName();
            boolean typeHov = mx >= leftX && mx <= leftX + leftW && my >= leftY + 48 && my <= leftY + 66;
            g.fill(leftX, leftY + 48, leftX + leftW, leftY + 66, typeHov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(leftX, leftY + 48, leftW, 18, Theme.BORDER);
            g.drawCenteredString(this.font, typeName, leftX + leftW / 2, leftY + 52, selected.isEnabled() ? Theme.ACCENT : Theme.TEXT_MUTE);

            int scriptDropdownX = leftX;
            int scriptDropdownY = leftY + 84;
            int scriptDropdownW = leftW / 2 - 4;
            boolean scriptHeaderHovered = mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= scriptDropdownY && my <= scriptDropdownY + SCRIPT_DROPDOWN_H;
            g.fill(scriptDropdownX, scriptDropdownY, scriptDropdownX + scriptDropdownW, scriptDropdownY + SCRIPT_DROPDOWN_H, scriptHeaderHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(scriptDropdownX, scriptDropdownY, scriptDropdownW, SCRIPT_DROPDOWN_H, Theme.BORDER);
            String selectedScript = selected.getScriptId().isEmpty() ? I18n.s("iscript.trigger.editor.select_script") : selected.getScriptId();
            g.drawString(this.font, selectedScript, scriptDropdownX + 4, scriptDropdownY + 6, Theme.TEXT);
            g.drawString(this.font, scriptDropdownOpen ? "\u25B2" : "\u25BC", scriptDropdownX + scriptDropdownW - 12, scriptDropdownY + 6, Theme.TEXT_DIM);

            if (scriptDropdownOpen) {
                int listDropY = scriptDropdownY + SCRIPT_DROPDOWN_H;
                int visibleCount = Math.min(scriptIds.size(), SCRIPT_DROPDOWN_MAX_VISIBLE);
                int listDropH = visibleCount * SCRIPT_DROPDOWN_H;
                g.fill(scriptDropdownX, listDropY, scriptDropdownX + scriptDropdownW, listDropY + listDropH, Theme.BG_INNER);
                g.renderOutline(scriptDropdownX, listDropY, scriptDropdownW, listDropH, Theme.BORDER);
                for (int i = 0; i < visibleCount; i++) {
                    int idx = i + scriptDropdownScroll;
                    if (idx >= scriptIds.size()) break;
                    int rowY = listDropY + i * SCRIPT_DROPDOWN_H;
                    boolean hovered = mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= rowY && my <= rowY + SCRIPT_DROPDOWN_H;
                    boolean isSel = selected.getScriptId().equals(scriptIds.get(idx));
                    int bg = isSel ? 0xFF334455 : (hovered ? Theme.BG_HOVER : Theme.BG_INNER);
                    g.fill(scriptDropdownX + 1, rowY, scriptDropdownX + scriptDropdownW - 1, rowY + SCRIPT_DROPDOWN_H, bg);
                    g.drawString(this.font, scriptIds.get(idx), scriptDropdownX + 4, rowY + 6, isSel ? Theme.ACCENT : Theme.TEXT);
                }
            }

            if (functionNameBox != null) {
                functionNameBox.setX(leftX + leftW / 2 + 4);
                functionNameBox.setY(leftY + 84);
                functionNameBox.setWidth(leftW / 2 - 4);
            }

            boolean enHov = mx >= leftX && mx <= leftX + 60 && my >= leftY + 112 && my <= leftY + 132;
            int enBg = enHov ? Theme.BG_HOVER : Theme.BG_INNER;
            int enCol = selected.isEnabled() ? 0xFF44AA44 : 0xFFAA4444;
            String enText = selected.isEnabled() ? I18n.s("iscript.trigger.state.on") : I18n.s("iscript.trigger.state.off");
            g.fill(leftX, leftY + 112, leftX + 60, leftY + 132, enBg);
            g.renderOutline(leftX, leftY + 112, 60, 20, Theme.BORDER);
            g.drawCenteredString(this.font, enText, leftX + 30, leftY + 117, enCol);

            if (life.save().isDirty()) {
                g.drawString(this.font, "*", leftX + leftW - 10, leftY, Theme.ERROR);
            }
        } else {
            clearEditorWidgets();
            g.drawCenteredString(this.font, I18n.s("iscript.trigger.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
        }

        if (contextMenu.isOpen()) {
            contextMenu.render(g, this.font, mx, my);
        }
    }

    private void renderConfirmDialog(GuiGraphics g, int mx, int my, int x, int w) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 70;
        int dx = cx - dw / 2;
        int dy = this.parent.height / 2 - 30;

        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ERROR);
        g.drawCenteredString(this.font, String.format(I18n.s("iscript.trigger.delete.confirm"), confirmDialogId), cx, dy + 8, Theme.ERROR);

        boolean okHovered = mx >= cx - 50 && mx <= cx - 2 && my >= dy + 38 && my <= dy + 60;
        g.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 38, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.trigger.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);

        boolean cancelHovered = mx >= cx + 2 && mx <= cx + 50 && my >= dy + 38 && my <= dy + 60;
        g.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.trigger.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
    }

    private void renderTypePicker(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        int pickerH = Math.min(460, h - 20);
        int pickerW = TYPE_PICKER_W;
        int px = x + (w - pickerW) / 2;
        int py = y + (h - pickerH) / 2;

        g.fill(x, y, x + w, y + h, 0xDD000000);
        g.fill(px - 2, py - 2, px + pickerW + 2, py + pickerH + 2, Theme.BG_INNER);
        g.renderOutline(px - 2, py - 2, pickerW + 4, pickerH + 4, Theme.ACCENT);
        g.drawCenteredString(this.font, I18n.s("iscript.trigger.picker.title"), px + pickerW / 2, py + 8, Theme.ACCENT);

        TriggerType[] types = TriggerType.values();
        int headerH = 28;
        int closeBtnH = 24;
        int listTop = py + headerH;
        int listBottom = py + pickerH - closeBtnH - 8;
        int listH = listBottom - listTop;
        int visibleRows = listH / TYPE_PICKER_ROW_H;
        int maxScroll = Math.max(0, types.length - visibleRows);

        int contentW = pickerW - SCROLLBAR_W - 12;

        for (int i = typePickerScroll; i < Math.min(typePickerScroll + visibleRows, types.length); i++) {
            int rowY = listTop + (i - typePickerScroll) * TYPE_PICKER_ROW_H;
            boolean hovered = mx >= px + 6 && mx <= px + 6 + contentW && my >= rowY && my <= rowY + TYPE_PICKER_ROW_H - 2;
            boolean sel = selected != null && selected.getType() == types[i];
            int bg = sel ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
            g.fill(px + 6, rowY, px + 6 + contentW, rowY + TYPE_PICKER_ROW_H - 2, bg);
            g.drawString(this.font, types[i].getDisplayName(), px + 12, rowY + 4, sel ? Theme.ACCENT : Theme.TEXT);
        }

        int sbX = px + pickerW - SCROLLBAR_W - 6;
        int sbY = listTop;
        int sbH = listH;
        g.fill(sbX, sbY, sbX + SCROLLBAR_W, sbY + sbH, 0xFF222222);

        if (maxScroll > 0) {
            float ratio = (float) visibleRows / types.length;
            int thumbH = Math.max(20, (int) (sbH * ratio));
            float scrollRatio = (float) typePickerScroll / maxScroll;
            int thumbY = sbY + (int) ((sbH - thumbH) * scrollRatio);
            boolean thumbHov = mx >= sbX && mx <= sbX + SCROLLBAR_W && my >= thumbY && my <= thumbY + thumbH;
            g.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbH, thumbHov ? Theme.ACCENT : 0xFF555555);
        } else {
            g.fill(sbX, sbY, sbX + SCROLLBAR_W, sbY + sbH, 0xFF444444);
        }

        int closeY = py + pickerH - closeBtnH - 4;
        boolean closeHov = mx >= px + pickerW / 2 - 40 && mx <= px + pickerW / 2 + 40 && my >= closeY && my <= closeY + 20;
        g.fill(px + pickerW / 2 - 40, closeY, px + pickerW / 2 + 40, closeY + 20, closeHov ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(px + pickerW / 2 - 40, closeY, 80, 20, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.trigger.picker.close"), px + pickerW / 2, closeY + 5, closeHov ? Theme.TEXT : Theme.TEXT_MUTE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (life.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int cx = x + w / 2;
            int dy = this.parent.height / 2 - 30;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 38 && my <= dy + 60) {
                executeConfirm();
                return true;
            }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 38 && my <= dy + 60) {
                closeConfirmDialog();
                return true;
            }
            return true;
        }

        if (contextMenu.isOpen()) {
            if (contextMenu.mouseClicked(mx, my, button)) {
                String action = contextMenu.getLastAction();
                if (action != null && contextMenuTriggerId != null) {
                    if ("Delete".equals(action)) {
                        openConfirmDialog(contextMenuTriggerId);
                    }
                }
                return true;
            }
            contextMenu.close();
        }

        if (typePickerOpen) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int pickerH = Math.min(460, h - 20);
            int pickerW = TYPE_PICKER_W;
            int px = x + (w - pickerW) / 2;
            int py = y + (h - pickerH) / 2;
            int headerH = 28;
            int closeBtnH = 24;
            int listTop = py + headerH;
            int listBottom = py + pickerH - closeBtnH - 8;
            int listH = listBottom - listTop;
            int visibleRows = listH / TYPE_PICKER_ROW_H;
            int contentW = pickerW - SCROLLBAR_W - 12;

            TriggerType[] types = TriggerType.values();
            for (int i = typePickerScroll; i < Math.min(typePickerScroll + visibleRows, types.length); i++) {
                int rowY = listTop + (i - typePickerScroll) * TYPE_PICKER_ROW_H;
                if (mx >= px + 6 && mx <= px + 6 + contentW && my >= rowY && my <= rowY + TYPE_PICKER_ROW_H - 2) {
                    if (selected != null) {
                        selected.setType(types[i]);
                        life.save().markDirty();
                    }
                    closeTypePicker();
                    return true;
                }
            }

            int sbX = px + pickerW - SCROLLBAR_W - 6;
            int sbY = listTop;
            int sbH = listH;
            int maxScroll = Math.max(0, types.length - visibleRows);
            if (maxScroll > 0) {
                float ratio = (float) visibleRows / types.length;
                int thumbH = Math.max(20, (int) (sbH * ratio));
                float scrollRatio = (float) typePickerScroll / maxScroll;
                int thumbY = sbY + (int) ((sbH - thumbH) * scrollRatio);
                if (mx >= sbX && mx <= sbX + SCROLLBAR_W && my >= thumbY && my <= thumbY + thumbH) {
                    typePickerDragging = true;
                    return true;
                }
                if (mx >= sbX && mx <= sbX + SCROLLBAR_W && my >= sbY && my <= sbY + sbH) {
                    float clickRatio = (float) (my - sbY) / sbH;
                    typePickerScroll = Math.max(0, Math.min(maxScroll, (int) (clickRatio * maxScroll)));
                    return true;
                }
            }

            int closeY = py + pickerH - closeBtnH - 4;
            if (mx >= px + pickerW / 2 - 40 && mx <= px + pickerW / 2 + 40 && my >= closeY && my <= closeY + 20) {
                closeTypePicker();
                return true;
            }
            return true;
        }

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        int scriptDropdownX = leftX;
        int scriptDropdownY = leftY + 84;
        int scriptDropdownW = leftW / 2 - 4;

        if (scriptDropdownOpen) {
            int listDropY = scriptDropdownY + SCRIPT_DROPDOWN_H;
            int visibleCount = Math.min(scriptIds.size(), SCRIPT_DROPDOWN_MAX_VISIBLE);
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + scriptDropdownScroll;
                if (idx >= scriptIds.size()) break;
                int rowY = listDropY + i * SCRIPT_DROPDOWN_H;
                if (mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= rowY && my <= rowY + SCRIPT_DROPDOWN_H) {
                    if (selected != null) {
                        selected.setScriptId(scriptIds.get(idx));
                        life.save().markDirty();
                    }
                    scriptDropdownOpen = false;
                    return true;
                }
            }
            scriptDropdownOpen = false;
            return true;
        }

        if (button != 0) return false;

        if (mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= scriptDropdownY && my <= scriptDropdownY + SCRIPT_DROPDOWN_H) {
            scriptDropdownOpen = !scriptDropdownOpen;
            return true;
        }

        List<TriggerData> list = filteredTriggers();
        int listH = h - 68;
        int listY = y + 42;
        int scroll = life.selection().scroll();
        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, list.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2) {
                TriggerData t = list.get(i);
                if (selected == null || !t.getId().equals(selected.getId())) {
                    switchToTrigger(t);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mx >= rightX + 4 && mx <= x + w - 4 && my >= newY && my <= newY + 22) {
            addNewTrigger();
            return true;
        }

        if (selected != null) {
            if (mx >= leftX && mx <= leftX + leftW && my >= leftY + 48 && my <= leftY + 66) {
                openTypePicker();
                return true;
            }

            if (mx >= leftX && mx <= leftX + 60 && my >= leftY + 112 && my <= leftY + 132) {
                selected.setEnabled(!selected.isEnabled());
                life.save().markDirty();
                return true;
            }

            EditBox idBox = life.editors().box("id");
            EditBox functionNameBox = life.editors().box("function");

            if (idBox != null && mx >= idBox.getX() && mx <= idBox.getX() + idBox.getWidth() && my >= idBox.getY() && my <= idBox.getY() + idBox.getHeight()) {
                parent.setFocusedWidget(idBox);
                return idBox.mouseClicked(mx, my, button);
            }
            if (functionNameBox != null && mx >= functionNameBox.getX() && mx <= functionNameBox.getX() + functionNameBox.getWidth() && my >= functionNameBox.getY() && my <= functionNameBox.getY() + functionNameBox.getHeight()) {
                parent.setFocusedWidget(functionNameBox);
                return functionNameBox.mouseClicked(mx, my, button);
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        if (typePickerDragging) {
            typePickerDragging = false;
            return true;
        }
        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            List<TriggerData> list = filteredTriggers();
            int listH = h - 68;
            int listY = y + 42;
            int scroll = life.selection().scroll();
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, list.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2) {
                    contextMenuTriggerId = list.get(i).getId();
                    contextMenu.setCustomActions(new String[]{"Delete"});
                    contextMenu.open((int) mx, (int) my, contextMenuTriggerId, false);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        if (typePickerDragging && typePickerOpen) {
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int pickerH = Math.min(460, h - 20);
            int pickerW = TYPE_PICKER_W;
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int px = x + (w - pickerW) / 2;
            int py = y + (h - pickerH) / 2;
            int headerH = 28;
            int closeBtnH = 24;
            int listTop = py + headerH;
            int listBottom = py + pickerH - closeBtnH - 8;
            int listH = listBottom - listTop;
            TriggerType[] types = TriggerType.values();
            int visibleRows = listH / TYPE_PICKER_ROW_H;
            int maxScroll = Math.max(0, types.length - visibleRows);

            float clickRatio = (float) (my - listTop) / listH;
            typePickerScroll = Math.max(0, Math.min(maxScroll, (int) (clickRatio * maxScroll)));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;

        if (typePickerOpen) {
            TriggerType[] types = TriggerType.values();
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int pickerH = Math.min(460, h - 20);
            int headerH = 28;
            int closeBtnH = 24;
            int listH = pickerH - headerH - closeBtnH - 8;
            int visibleRows = listH / TYPE_PICKER_ROW_H;
            int maxScroll = Math.max(0, types.length - visibleRows);
            if (delta > 0) typePickerScroll = Math.max(0, typePickerScroll - 3);
            else typePickerScroll = Math.min(typePickerScroll + 3, maxScroll);
            return true;
        }

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;
        int scriptDropdownX = leftX;
        int scriptDropdownY = leftY + 84;
        int scriptDropdownW = leftW / 2 - 4;

        if (scriptDropdownOpen) {
            int listDropY = scriptDropdownY + SCRIPT_DROPDOWN_H;
            int listDropH = Math.min(scriptIds.size(), SCRIPT_DROPDOWN_MAX_VISIBLE) * SCRIPT_DROPDOWN_H;
            if (mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= listDropY && my <= listDropY + listDropH) {
                int maxScroll = Math.max(0, scriptIds.size() - SCRIPT_DROPDOWN_MAX_VISIBLE);
                if (delta > 0) scriptDropdownScroll = Math.max(0, scriptDropdownScroll - 1);
                else scriptDropdownScroll = Math.min(scriptDropdownScroll + 1, maxScroll);
                return true;
            }
        }

        if (mx >= rightX && mx <= x + w) {
            List<TriggerData> list = filteredTriggers();
            int listH = h - 68;
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, list.size() - visible);
            int scroll = life.selection().scroll();
            if (delta > 0) life.selection().scroll(Math.max(0, scroll - 1));
            else life.selection().scroll(Math.min(scroll + 1, maxScroll));
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char cp, int mod) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        if (typePickerOpen) return true;
        if (life.search().box() != null && life.search().box().isFocused()) return life.search().box().charTyped(cp, mod);
        EditBox idBox = life.editors().box("id");
        EditBox functionNameBox = life.editors().box("function");
        if (idBox != null && idBox.isFocused()) return idBox.charTyped(cp, mod);
        if (functionNameBox != null && functionNameBox.isFocused()) return functionNameBox.charTyped(cp, mod);
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (life.modals().isOpen("confirm")) {
            if (key == 257 || key == 335) { executeConfirm(); return true; }
            if (key == 256) { closeConfirmDialog(); return true; }
            return true;
        }

        if (contextMenu.isOpen()) return true;

        if (typePickerOpen) {
            if (key == 256) { closeTypePicker(); return true; }
            return true;
        }

        if (scriptDropdownOpen) {
            if (key == 256) { scriptDropdownOpen = false; return true; }
        }

        if (life.search().box() != null && life.search().box().isFocused()) return life.search().box().keyPressed(key, scan, mod);
        EditBox idBox = life.editors().box("id");
        EditBox functionNameBox = life.editors().box("function");
        if (idBox != null && idBox.isFocused()) return idBox.keyPressed(key, scan, mod);
        if (functionNameBox != null && functionNameBox.isFocused()) return functionNameBox.keyPressed(key, scan, mod);
        return false;
    }
}