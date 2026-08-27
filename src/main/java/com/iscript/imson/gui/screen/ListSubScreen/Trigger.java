package com.iscript.imson.gui.screen.ListSubScreen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.iscript.imson.IScriptMod;
import com.iscript.imson.api.triggers.TriggerData;
import com.iscript.imson.api.triggers.TriggerType;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.widget.ContextMenu;
import com.iscript.imson.gui.widget.DropdownMenu;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import com.iscript.imson.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trigger extends ListSubScreen {
    private static final int RIGHT_PANEL_WIDTH = 220;
    private static final int TYPE_PICKER_W = 400;
    private static final int TYPE_PICKER_ROW_H = 20;
    private static final int SCROLLBAR_W = 6;
    private static final int SCRIPT_DROPDOWN_H = 20;
    private static final int SCRIPT_DROPDOWN_MAX_VISIBLE = 6;
    private static final Gson GSON = new Gson();

    private final List<TriggerData> triggers = new ArrayList<>();
    private TriggerData selected = null;
    private boolean typePickerOpen = false;
    private int typePickerScroll = 0;
    private boolean typePickerDragging = false;
    private List<String> scriptIds = new ArrayList<>();
    private final DropdownMenu scriptDropdown = new DropdownMenu();
    private int lastScriptCacheSize = -1;

    public Trigger(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.trigger.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.trigger.editor.empty"); }

    @Override
    public void init() {
        super.init();
        typePickerOpen = false;
        typePickerDragging = false;
        scriptDropdown.close();
        refreshScriptIds();
        if (scriptIds.isEmpty()) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_TRIGGERS, new CompoundTag()));
        if (selected != null) buildEditor(selected);
    }

    @Override
    public void tick() {
        super.tick();
        boolean anyFocused = (lifecycle.editors().box("id") != null && lifecycle.editors().box("id").isFocused()) ||
                (lifecycle.editors().box("function") != null && lifecycle.editors().box("function").isFocused()) ||
                (lifecycle.search().box() != null && lifecycle.search().box().isFocused());
        if (lifecycle.save().isDirty() && !anyFocused && !typePickerOpen && !scriptDropdown.isOpen()) doSave();
        if (anyFocused || scriptDropdown.isOpen()) lifecycle.save().markDirty();
        if (lifecycle.search().box() == null && this.minecraft != null) lifecycle.search().recreateIfMissing();
        var cache = ScriptGraphManager.getClientCache();
        if (cache.size() != lastScriptCacheSize) {
            lastScriptCacheSize = cache.size();
            refreshScriptIds();
        }
        boolean modalOpen = lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || typePickerOpen;
        EditBox idBox = lifecycle.editors().box("id");
        EditBox funcBox = lifecycle.editors().box("function");
        if (idBox != null) idBox.setVisible(!modalOpen);
        if (funcBox != null) funcBox.setVisible(!modalOpen);
    }

    private void refreshScriptIds() {
        scriptIds = new ArrayList<>(ScriptGraphManager.getClientCache().keySet());
        Collections.sort(scriptIds);
    }

    @Override
    protected List<String> getItemIds() {
        List<String> ids = new ArrayList<>();
        for (TriggerData t : triggers) ids.add(t.getId());
        return ids;
    }

    @Override
    protected String getItemDisplayName(String id) {
        TriggerData t = findById(id);
        if (t == null) return id;
        String line = t.getType().getDisplayName() + " -> " + t.getScriptId();
        if (!t.getFunctionName().isEmpty()) line += ":" + t.getFunctionName();
        return id;
    }

    @Override
    protected void onSelect(String id) {
        TriggerData t = findById(id);
        if (t == null) return;
        doSave();
        setSelectedId(id);
        selected = t;
        lifecycle.editors().removeAll();
        lifecycle.save().clearDirty();
        buildEditor(t);
    }

    @Override
    protected void onNew(String id) {
        TriggerData t = new TriggerData(id, TriggerType.PLAYER_TICK, "", "");
        triggers.add(t);
        onSelect(id);
        lifecycle.save().markDirty();
    }

    @Override
    protected void onDelete(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_TRIGGER, tag));
        triggers.removeIf(t -> t.getId().equals(id));
        if (selected != null && selected.getId().equals(id)) {
            selected = null;
            lifecycle.editors().removeAll();
        }
    }

    @Override
    protected void onRename(String oldId, String newId) {
        TriggerData t = findById(oldId);
        if (t == null || oldId.equals(newId)) return;
        t.setId(newId);
        if (selected == t) buildEditor(t);
        lifecycle.save().markDirty();
    }

    @Override
    protected void onDuplicate(String id) {
        TriggerData src = findById(id);
        if (src == null) return;
        String base = id;
        String newId = id + "_1";
        int counter = 1;
        while (findById(newId) != null) {
            counter++;
            newId = base + "_" + counter;
        }
        TriggerData copy = new TriggerData(newId, src.getType(), src.getScriptId(), src.getFunctionName());
        copy.setEnabled(src.isEnabled());
        triggers.add(copy);
        onSelect(newId);
        lifecycle.save().markDirty();
    }

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
        String srcId = DashboardScreen.clipboard;
        if (srcId == null || srcId.isEmpty()) return;
        TriggerData src = findById(srcId);
        if (src == null) return;
        String base = srcId + "_copy";
        String newId = base;
        int counter = 1;
        while (findById(newId) != null) {
            newId = base + "_" + counter;
            counter++;
        }
        TriggerData copy = new TriggerData(newId, src.getType(), src.getScriptId(), src.getFunctionName());
        copy.setEnabled(src.isEnabled());
        triggers.add(copy);
        onSelect(newId);
        lifecycle.save().markDirty();
    }

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty() && findById(DashboardScreen.clipboard) != null;
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) { return false; }

    @Override
    protected void doSave() {
        if (!lifecycle.save().isDirty() || selected == null) return;
        lifecycle.save().clearDirty();
        applyEditorToData();
        CompoundTag tag = new CompoundTag();
        tag.putString("json", GSON.toJson(selected.toJson()));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_TRIGGER, tag));
    }

    private void buildEditor(TriggerData data) {
        lifecycle.editors().removeAll();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        boolean isNew = triggers.stream().noneMatch(t -> t.getId().equals(data.getId()) && t != data);

        EditBox idBox = lifecycle.editors().addBox("id", leftX, leftY + 12, leftW, 18, I18n.t("iscript.trigger.editor.label.id"), data.getId());
        if (idBox != null) {
            idBox.setMaxLength(64);
            idBox.setEditable(isNew);
        }

        int funcBoxX = leftX + leftW / 2 + 4;
        int funcBoxW = leftW / 2 - 4;
        EditBox funcBox = lifecycle.editors().addBox("function", funcBoxX, leftY + 84, funcBoxW, 18, I18n.t("iscript.trigger.editor.label.function"), data.getFunctionName());
        if (funcBox != null) funcBox.setMaxLength(128);
    }

    private void applyEditorToData() {
        if (selected == null) return;
        boolean isNew = triggers.stream().noneMatch(t -> t.getId().equals(selected.getId()) && t != selected);
        EditBox idBox = lifecycle.editors().box("id");
        EditBox funcBox = lifecycle.editors().box("function");
        if (idBox != null && isNew) {
            String newId = idBox.getValue().trim();
            if (!newId.isEmpty()) selected.setId(newId);
        }
        if (funcBox != null) selected.setFunctionName(funcBox.getValue().trim());
    }

    private TriggerData findById(String id) {
        for (TriggerData t : triggers) if (t.getId().equals(id)) return t;
        return null;
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (selected == null) return;
        int leftX = x;
        int leftY = y;
        int leftW = w;

        g.drawString(font, I18n.s("iscript.trigger.editor.label.id"), leftX, leftY, Theme.TEXT_MUTE);
        g.drawString(font, I18n.s("iscript.trigger.editor.label.type"), leftX, leftY + 36, Theme.TEXT_MUTE);
        g.drawString(font, I18n.s("iscript.trigger.editor.label.script"), leftX, leftY + 72, Theme.TEXT_MUTE);
        g.drawString(font, I18n.s("iscript.trigger.editor.label.function"), leftX + leftW / 2 + 4, leftY + 72, Theme.TEXT_MUTE);

        EditBox idBox = lifecycle.editors().box("id");
        EditBox functionNameBox = lifecycle.editors().box("function");

        if (idBox != null) {
            idBox.setX(leftX);
            idBox.setY(leftY + 12);
            idBox.setWidth(leftW);
        }

        String typeName = selected.getType().getDisplayName();
        boolean typeHov = mx >= leftX && mx <= leftX + leftW && my >= leftY + 48 && my <= leftY + 66;
        g.fill(leftX, leftY + 48, leftX + leftW, leftY + 66, typeHov ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX, leftY + 48, leftW, 18, Theme.BORDER);
        g.drawCenteredString(font, typeName, leftX + leftW / 2, leftY + 52, selected.isEnabled() ? Theme.ACCENT : Theme.TEXT_MUTE);

        int scriptDropdownX = leftX;
        int scriptDropdownY = leftY + 84;
        int scriptDropdownW = leftW / 2 - 4;
        boolean scriptHeaderHovered = mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= scriptDropdownY && my <= scriptDropdownY + SCRIPT_DROPDOWN_H;
        g.fill(scriptDropdownX, scriptDropdownY, scriptDropdownX + scriptDropdownW, scriptDropdownY + SCRIPT_DROPDOWN_H, scriptHeaderHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(scriptDropdownX, scriptDropdownY, scriptDropdownW, SCRIPT_DROPDOWN_H, Theme.BORDER);
        String selectedScript = selected.getScriptId().isEmpty() ? I18n.s("iscript.trigger.editor.select_script") : selected.getScriptId();
        g.drawString(font, selectedScript, scriptDropdownX + 4, scriptDropdownY + 6, Theme.TEXT);
        g.drawString(font, "\u25BC", scriptDropdownX + scriptDropdownW - 12, scriptDropdownY + 6, Theme.TEXT_DIM);



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
        g.drawCenteredString(font, enText, leftX + 30, leftY + 117, enCol);

        if (!typePickerOpen) {
            scriptDropdown.render(g, font, mx, my);
        }

        if (lifecycle.save().isDirty()) {
            g.drawString(font, "*", leftX + leftW - 10, leftY, Theme.ERROR);
        }
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0) return false;

        if (scriptDropdown.isOpen()) {
            scriptDropdown.mouseClicked(mx, my, button);
            return true;
        }

        if (mx >= leftX && mx <= leftX + leftW && my >= leftY + 48 && my <= leftY + 66) {
            openTypePicker();
            return true;
        }

        int scriptDropdownX = leftX;
        int scriptDropdownY = leftY + 84;
        int scriptDropdownW = leftW / 2 - 4;

        if (mx >= scriptDropdownX && mx <= scriptDropdownX + scriptDropdownW && my >= scriptDropdownY && my <= scriptDropdownY + SCRIPT_DROPDOWN_H) {
            scriptDropdown.setItems(scriptIds);
            scriptDropdown.setOnSelect(id -> {
                selected.setScriptId(id);
                lifecycle.save().markDirty();
            });
            scriptDropdown.open(scriptDropdownX, scriptDropdownY + SCRIPT_DROPDOWN_H, scriptDropdownW, selected.getScriptId());
            closeTypePicker();
            return true;
        }

        if (mx >= leftX && mx <= leftX + 60 && my >= leftY + 112 && my <= leftY + 132) {
            selected.setEnabled(!selected.isEnabled());
            lifecycle.save().markDirty();
            return true;
        }

        EditBox idBox = lifecycle.editors().box("id");
        EditBox functionNameBox = lifecycle.editors().box("function");
        if (idBox != null && mx >= idBox.getX() && mx <= idBox.getX() + idBox.getWidth() && my >= idBox.getY() && my <= idBox.getY() + idBox.getHeight()) {
            parent.setFocusedWidget(idBox);
            return idBox.mouseClicked(mx, my, button);
        }
        if (functionNameBox != null && mx >= functionNameBox.getX() && mx <= functionNameBox.getX() + functionNameBox.getWidth() && my >= functionNameBox.getY() && my <= functionNameBox.getY() + functionNameBox.getHeight()) {
            parent.setFocusedWidget(functionNameBox);
            return functionNameBox.mouseClicked(mx, my, button);
        }
        return false;
    }

    @Override
    protected boolean handleEditorScroll(double mx, double my, double delta, int leftX, int leftY, int leftW, int leftH) {
        if (scriptDropdown.isOpen()) {
            scriptDropdown.mouseScrolled(delta);
            return true;
        }
        return false;
    }

    private void openTypePicker() {
        scriptDropdown.close();
        typePickerOpen = true;
        typePickerDragging = false;
        typePickerScroll = 0;
    }

    private void closeTypePicker() {
        typePickerOpen = false;
        typePickerDragging = false;
    }


    private int getPickerHeight() {
        TriggerType[] types = TriggerType.values();
        int headerH = 28;
        int closeBtnH = 24;
        int neededListH = types.length * TYPE_PICKER_ROW_H + 8;
        int neededH = headerH + closeBtnH + 16 + neededListH;
        int maxH = this.parent.height - DashboardScreen.TOPBAR_H - 40;
        return Math.max(180, Math.min(neededH, maxH));
    }

    private int getPickerY(int pickerH) {
        return DashboardScreen.TOPBAR_H + 20;
    }

    @Override
    protected boolean hasCustomModals() { return typePickerOpen; }
    @Override
    protected void renderCustomModals(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (!typePickerOpen) return;
        int pickerH = getPickerHeight();
        int pickerW = TYPE_PICKER_W;
        int px = x + (w - pickerW) / 2;
        int py = getPickerY(pickerH);

        g.fill(x, y, x + w, y + h, 0xDD000000);
        g.fill(px - 2, py - 2, px + pickerW + 2, py + pickerH + 2, Theme.BG_INNER);
        g.renderOutline(px - 2, py - 2, pickerW + 4, pickerH + 4, Theme.ACCENT);
        g.drawCenteredString(font, I18n.s("iscript.trigger.picker.title"), px + pickerW / 2, py + 8, Theme.ACCENT);

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
            g.drawString(font, types[i].getDisplayName(), px + 12, rowY + 4, sel ? Theme.ACCENT : Theme.TEXT);
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
        g.drawCenteredString(font, I18n.s("iscript.trigger.picker.close"), px + pickerW / 2, closeY + 5, closeHov ? Theme.TEXT : Theme.TEXT_MUTE);
    }

    @Override
    protected boolean handleCustomModalClick(double mx, double my, int button) {
        if (!typePickerOpen) return false;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int pickerH = getPickerHeight();
        int pickerW = TYPE_PICKER_W;
        int px = x + (w - pickerW) / 2;
        int py = getPickerY(pickerH);
        if (mx < px || mx > px + pickerW || my < py || my > py + pickerH) {
            closeTypePicker();
            return false;
        }
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
                    lifecycle.save().markDirty();
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

    @Override
    protected boolean handleCustomModalKey(int keyCode, int scanCode, int modifiers) {
        if (!typePickerOpen) return false;
        if (keyCode == 256) { closeTypePicker(); return true; }
        return true;
    }

    @Override
    protected boolean handleCustomModalScroll(double mx, double my, double delta) {
        if (!typePickerOpen) return false;
        TriggerType[] types = TriggerType.values();
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int pickerH = getPickerHeight();
        int headerH = 28;
        int closeBtnH = 24;
        int listH = pickerH - headerH - closeBtnH - 8;
        int visibleRows = listH / TYPE_PICKER_ROW_H;
        int maxScroll = Math.max(0, types.length - visibleRows);
        if (delta > 0) typePickerScroll = Math.max(0, typePickerScroll - 3);
        else typePickerScroll = Math.min(typePickerScroll + 3, maxScroll);
        return true;
    }

    @Override
    protected boolean handleCustomModalRelease(double mx, double my, int button) {
        if (typePickerDragging) {
            typePickerDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (typePickerDragging && typePickerOpen) {
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int pickerH = getPickerHeight();
            int pickerW = TYPE_PICKER_W;
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int px = x + (w - pickerW) / 2;
            int py = getPickerY(pickerH);
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
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (scriptDropdown.isOpen() && keyCode == 256) {
            scriptDropdown.close();
            return true;
        }
        if (lifecycle.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
            lifecycle.editors().removeAll();
        }
    }
}