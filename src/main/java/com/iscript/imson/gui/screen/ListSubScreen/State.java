package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.api.states.States;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class State extends ListSubScreen {
    private static final int RIGHT_PANEL_WIDTH = 200;
    private static final int TARGET_SELECTOR_W = 280;
    private static final int MODE_GLOBAL = 0;
    private static final int MODE_PLAYER = 1;
    private static final int MODE_ENTITY = 2;
    private static final int TYPE_STRING = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_BOOLEAN = 2;

    private States clientStates = new States();
    private int mode = MODE_GLOBAL;
    private String targetId = "";
    private int valueType = TYPE_STRING;
    private int searchMode = 0;

    private boolean showTargetSelector = false;
    private List<String> targetOptions = new ArrayList<>();
    private java.util.Map<String, String> targetOptionIds = new java.util.LinkedHashMap<>();

    public State(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) { return false; }

    @Override
    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }

    @Override
    protected int getSearchTopOffset() { return 18; }

    @Override
    protected int getListTitleOffset() {
        return mode != MODE_GLOBAL ? 36 : 20;
    }

    @Override
    protected String getListTitle() { return I18n.s("iscript.state.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.state.editor.empty"); }

    @Override
    protected int getSearchWidth() { return RIGHT_PANEL_WIDTH - 72; }

    @Override
    protected boolean canCreateNew() {
        return !(mode != MODE_GLOBAL && targetId.isEmpty());
    }

    @Override
    protected String[] getContextMenuActions() {
        return new String[]{"Copy", "Paste", "Duplicate", "Delete"};
    }

    @Override
    public void init() {
        super.init();
        if (mode != MODE_GLOBAL && targetId.isEmpty()) {
            clientStates = new States();
            setSelectedId(null);
        }
        if (getSelectedId() != null) buildEditor(getSelectedId());
        requestStates();
    }

    private void requestStates() {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        CompoundTag data = new CompoundTag();
        data.putInt("mode", mode);
        if (mode != MODE_GLOBAL) data.putString("target", targetId);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_STATES, data));
    }

    @Override
    public void tick() {
        super.tick();
        boolean anyFocused = (lifecycle.editors().box("key") != null && lifecycle.editors().box("key").isFocused()) ||
                (lifecycle.editors().box("value") != null && lifecycle.editors().box("value").isFocused()) ||
                (lifecycle.search().box() != null && lifecycle.search().box().isFocused());
        if (lifecycle.save().isDirty() && !anyFocused) doSave();
        if (anyFocused) lifecycle.save().markDirty();
        boolean modalOpen = lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || showTargetSelector;
        EditBox keyBox = lifecycle.editors().box("key");
        EditBox valueBox = lifecycle.editors().box("value");
        if (keyBox != null) keyBox.setVisible(!modalOpen);
        if (valueBox != null) valueBox.setVisible(!modalOpen);
    }

    @Override
    protected List<String> getItemIds() {
        return new ArrayList<>(clientStates.keys());
    }

    @Override
    protected boolean matchesFilter(String id, String filter) {
        if (filter.isEmpty()) return true;
        switch (searchMode) {
            case 0 -> { return id.toLowerCase().contains(filter); }
            case 1 -> {
                Object val = clientStates.getValues().get(id);
                return val != null && val.toString().toLowerCase().contains(filter);
            }
            case 2 -> {
                Object val = clientStates.getValues().get(id);
                String typeStr = "string";
                if (val instanceof Boolean) typeStr = "boolean";
                else if (val instanceof Number) typeStr = "number";
                return typeStr.startsWith(filter);
            }
        }
        return false;
    }

    @Override
    protected String getItemDisplayName(String id) {
        Object val = clientStates.getValues().get(id);
        String preview;
        if (val instanceof Boolean) preview = (Boolean) val ? I18n.s("iscript.state.boolean.true") : I18n.s("iscript.state.boolean.false");
        else if (val instanceof Number) preview = String.valueOf(((Number) val).doubleValue());
        else preview = "\"" + val + "\"";
        String display = id + " = " + preview;
        if (font.width(display) > getRightPanelWidth() - 16)
            display = font.plainSubstrByWidth(display, getRightPanelWidth() - 16) + "...";
        return display;
    }

    @Override
    protected void onSelect(String id) {
        doSave();
        setSelectedId(id);
        lifecycle.editors().removeAll();
        lifecycle.save().clearDirty();
        buildEditor(id);
    }

    @Override
    protected void onNew(String id) {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        clientStates.setString(id, "");
        onSelect(id);
        lifecycle.save().markDirty();
    }

    @Override
    protected void onDelete(String id) {
        clientStates.remove(id);
        if (id.equals(getSelectedId())) {
            setSelectedId(null);
            lifecycle.editors().removeAll();
        }
        lifecycle.save().markDirty();
        doSave();
    }

    @Override
    protected void onRename(String oldId, String newId) {
        if (oldId.equals(newId) || !clientStates.has(oldId)) return;
        Object val = clientStates.getValues().get(oldId);
        clientStates.remove(oldId);
        clientStates.set(newId, copyValue(val));
        if (oldId.equals(getSelectedId())) {
            setSelectedId(newId);
            buildEditor(newId);
        }
        lifecycle.save().markDirty();
        doSave();
    }

    @Override
    protected void onDuplicate(String id) {
        if (!clientStates.has(id)) return;
        String base = id;
        String newId = id + "_1";
        int counter = 1;
        while (clientStates.has(newId)) {
            counter++;
            newId = base + "_" + counter;
        }
        Object val = clientStates.getValues().get(id);
        clientStates.set(newId, copyValue(val));
        lifecycle.save().markDirty();
        doSave();
    }

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
        String source = DashboardScreen.clipboard;
        if (source == null || source.isEmpty() || !clientStates.has(source)) return;
        String base = source + "_copy";
        String newId = base;
        int counter = 1;
        while (clientStates.has(newId)) {
            newId = base + "_" + counter;
            counter++;
        }
        Object val = clientStates.getValues().get(source);
        clientStates.set(newId, copyValue(val));
        lifecycle.save().markDirty();
        doSave();
    }

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty() && clientStates.has(DashboardScreen.clipboard);
    }

    @Override
    protected void doSave() {
        if (!lifecycle.save().isDirty()) return;
        if (mode != MODE_GLOBAL && targetId.isEmpty()) {
            lifecycle.save().clearDirty();
            return;
        }
        lifecycle.save().clearDirty();
        applyEditorToState();
        CompoundTag data = new CompoundTag();
        data.putInt("mode", mode);
        if (mode != MODE_GLOBAL) data.putString("target", targetId);
        data.put("states", clientStates.serialize());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_STATES, data));
    }

    private void buildEditor(String key) {
        lifecycle.editors().removeAll();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        EditBox keyBox = lifecycle.editors().addBox("key", leftX, leftY, leftW, 20, I18n.t("iscript.state.editor.placeholder.key"), key != null ? key : "");
        if (keyBox != null) keyBox.setEditable(true);

        if (key != null && clientStates.has(key)) {
            Object val = clientStates.getValues().get(key);
            if (val instanceof Boolean) valueType = TYPE_BOOLEAN;
            else if (val instanceof Number) valueType = TYPE_NUMBER;
            else valueType = TYPE_STRING;
        } else {
            valueType = TYPE_STRING;
        }

        if (valueType != TYPE_BOOLEAN) {
            String valStr = "";
            if (key != null && clientStates.has(key)) {
                if (valueType == TYPE_NUMBER && clientStates.isNumber(key)) valStr = String.valueOf(clientStates.getNumber(key));
                else {
                    Object val = clientStates.getValues().get(key);
                    valStr = val != null ? val.toString() : "";
                }
            }
            lifecycle.editors().addBox("value", leftX, leftY + 38, leftW, 20, I18n.t("iscript.state.editor.placeholder.value"), valStr);
        }
    }

    private void applyEditorToState() {
        if (getSelectedId() == null) return;
        EditBox keyBox = lifecycle.editors().box("key");
        if (keyBox == null) return;
        String newKey = keyBox.getValue().trim();
        if (newKey.isEmpty()) return;
        String selKey = getSelectedId();
        if (!newKey.equals(selKey)) {
            Object oldValue = clientStates.getValues().get(selKey);
            clientStates.remove(selKey);
            setSelectedId(newKey);
            clientStates.set(newKey, oldValue);
        }
        if (valueType != TYPE_BOOLEAN) {
            EditBox valueBox = lifecycle.editors().box("value");
            if (valueBox != null) {
                String valStr = valueBox.getValue();
                if (valueType == TYPE_NUMBER) {
                    try {
                        double d = Double.parseDouble(valStr);
                        clientStates.setNumber(getSelectedId(), d);
                    } catch (NumberFormatException e) {
                        clientStates.setString(getSelectedId(), valStr);
                        valueType = TYPE_STRING;
                    }
                } else {
                    clientStates.setString(getSelectedId(), valStr);
                }
            }
        }
    }

    private Object copyValue(Object val) {
        if (val instanceof Boolean) return val;
        if (val instanceof Number) {
            if (val instanceof Double) return (Double) val;
            if (val instanceof Float) return (Float) val;
            if (val instanceof Integer) return (Integer) val;
            if (val instanceof Long) return (Long) val;
            return ((Number) val).doubleValue();
        }
        return val != null ? val.toString() : "";
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        String sel = getSelectedId();
        if (sel == null) return;

        int leftX = x;
        int leftY = y;
        int leftW = w;

        g.drawString(font, I18n.s("iscript.state.editor.label.key"), leftX, leftY, Theme.TEXT_MUTE);
        g.drawString(font, I18n.s("iscript.state.editor.label.value"), leftX, leftY + 38, Theme.TEXT_MUTE);

        EditBox keyBox = lifecycle.editors().box("key");
        EditBox valueBox = lifecycle.editors().box("value");

        if (keyBox != null) {
            keyBox.setX(leftX);
            keyBox.setY(leftY + 12);
            keyBox.setWidth(leftW);
        }

        String[] typeLabels = {I18n.s("iscript.state.type.string"), I18n.s("iscript.state.type.number"), I18n.s("iscript.state.type.boolean")};
        int typeBtnY = leftY + 76;
        boolean typeHovered = mx >= leftX && mx <= leftX + 80 && my >= typeBtnY && my <= typeBtnY + 18;
        g.fill(leftX, typeBtnY, leftX + 80, typeBtnY + 18, typeHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(leftX, typeBtnY, 80, 18, Theme.BORDER);
        g.drawString(font, "Type: " + typeLabels[valueType], leftX + 4, typeBtnY + 4, Theme.ACCENT);

        if (valueType == TYPE_BOOLEAN) {
            Object val = clientStates.getValues().get(sel);
            boolean currentBool = val instanceof Boolean && (Boolean) val;
            int boolY = leftY + 50;
            boolean boolHovered = mx >= leftX && mx <= leftX + 60 && my >= boolY && my <= boolY + 18;
            g.fill(leftX, boolY, leftX + 60, boolY + 18, currentBool ? 0xFF44AA44 : 0xFFAA4444);
            g.renderOutline(leftX, boolY, 60, 18, Theme.BORDER);
            g.drawCenteredString(font, currentBool ? I18n.s("iscript.toggle.on") : I18n.s("iscript.toggle.off"), leftX + 30, boolY + 4, 0xFFFFFFFF);
        } else {
            if (valueBox != null) {
                valueBox.setX(leftX);
                valueBox.setY(leftY + 50);
                valueBox.setWidth(leftW);
            }
        }

        if (lifecycle.save().isDirty()) {
            g.drawString(font, "*", leftX + leftW - 10, leftY, Theme.ERROR);
        }
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0) return false;

        int typeBtnY = leftY + 76;
        if (mx >= leftX && mx <= leftX + 80 && my >= typeBtnY && my <= typeBtnY + 18) {
            cycleValueType();
            return true;
        }

        if (valueType == TYPE_BOOLEAN) {
            int boolY = leftY + 50;
            if (mx >= leftX && mx <= leftX + 60 && my >= boolY && my <= boolY + 18) {
                toggleBooleanValue();
                return true;
            }
        }

        EditBox keyBox = lifecycle.editors().box("key");
        EditBox valueBox = lifecycle.editors().box("value");
        if (keyBox != null && mx >= keyBox.getX() && mx <= keyBox.getX() + keyBox.getWidth() && my >= keyBox.getY() && my <= keyBox.getY() + keyBox.getHeight()) {
            parent.setFocusedWidget(keyBox);
            return keyBox.mouseClicked(mx, my, button);
        }
        if (valueBox != null && valueBox.visible && mx >= valueBox.getX() && mx <= valueBox.getX() + valueBox.getWidth() && my >= valueBox.getY() && my <= valueBox.getY() + valueBox.getHeight()) {
            parent.setFocusedWidget(valueBox);
            return valueBox.mouseClicked(mx, my, button);
        }
        return false;
    }

    private void cycleValueType() {
        valueType = (valueType + 1) % 3;
        if (getSelectedId() != null) {
            String key = getSelectedId();
            Object current = clientStates.getValues().get(key);
            switch (valueType) {
                case TYPE_BOOLEAN:
                    boolean b = false;
                    if (current instanceof Number) b = ((Number) current).doubleValue() != 0;
                    else if (current instanceof String) b = Boolean.parseBoolean((String) current);
                    clientStates.set(key, b);
                    break;
                case TYPE_NUMBER:
                    double d = 0;
                    if (current instanceof Boolean) d = ((Boolean) current) ? 1 : 0;
                    else if (current instanceof String) {
                        try { d = Double.parseDouble((String) current); } catch (NumberFormatException e) {}
                    } else if (current instanceof Number) d = ((Number) current).doubleValue();
                    clientStates.set(key, d);
                    break;
                case TYPE_STRING:
                    String s = current != null ? current.toString() : "";
                    clientStates.set(key, s);
                    break;
            }
            buildEditor(key);
            lifecycle.save().markDirty();
        }
    }

    private void toggleBooleanValue() {
        if (getSelectedId() != null && valueType == TYPE_BOOLEAN) {
            Object current = clientStates.getValues().get(getSelectedId());
            boolean newVal = !(current instanceof Boolean && (Boolean) current);
            clientStates.set(getSelectedId(), newVal);
            lifecycle.save().markDirty();
        }
    }

    @Override
    protected void renderRightPanelExtras(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        String[] modeLabels = {I18n.s("iscript.state.mode.global"), I18n.s("iscript.state.mode.player"), I18n.s("iscript.state.mode.entity")};
        int tabPad = 4;
        int tabW = (w - tabPad * 2) / 3;
        int tabH = 14;
        for (int i = 0; i < 3; i++) {
            int tx = x + tabPad + i * tabW;
            boolean th = mx >= tx && mx <= tx + tabW - 2 && my >= y + 4 && my <= y + 4 + tabH;
            int tbg = mode == i ? 0xFF334455 : (th ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(tx, y + 4, tx + tabW - 2, y + 4 + tabH, tbg);
            g.renderOutline(tx, y + 4, tabW - 2, tabH, Theme.BORDER);
            g.drawCenteredString(font, modeLabels[i], tx + tabW / 2 - 1, y + 6, mode == i ? Theme.ACCENT : Theme.TEXT);
        }

        if (mode != MODE_GLOBAL) {
            int targetY = y + 24 + getSearchTopOffset();
            int targetH = 14;
            String targetLabel;
            if (mode == MODE_PLAYER) {
                targetLabel = targetId.isEmpty() ? I18n.s("iscript.state.target.select_player") : String.format(I18n.s("iscript.state.target.player"), targetId);
            } else {
                targetLabel = targetId.isEmpty() ? I18n.s("iscript.state.target.select_entity") : String.format(I18n.s("iscript.state.target.entity"), targetId.substring(0, Math.min(16, targetId.length())) + (targetId.length() > 16 ? "..." : ""));
            }
            boolean targetHov = mx >= x && mx <= x + w && my >= targetY && my <= targetY + targetH;
            g.fill(x, targetY, x + w, targetY + targetH, targetHov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.renderOutline(x, targetY, w, targetH, Theme.BORDER);
            g.drawCenteredString(font, targetLabel, x + w / 2, targetY + 3, Theme.ACCENT);
        }
    }

    @Override
    protected boolean handleRightPanelClick(double mx, double my, int button, int x, int y, int w, int h) {
        int tabPad = 4;
        int tabW = (w - tabPad * 2) / 3;
        int tabH = 14;
        for (int i = 0; i < 3; i++) {
            int tx = x + tabPad + i * tabW;
            if (mx >= tx && mx <= tx + tabW - 2 && my >= y + 4 && my <= y + 4 + tabH) {
                if (mode != i) {
                    doSave();
                    mode = i;
                    targetId = "";
                    setSelectedId(null);
                    lifecycle.editors().removeAll();
                    clientStates = new States();
                    requestStates();
                }
                return true;
            }
        }

        if (mode != MODE_GLOBAL) {
            int targetY = y + 24 + getSearchTopOffset();
            int targetH = 14;
            if (mx >= x && mx <= x + w && my >= targetY && my <= targetY + targetH) {
                openTargetSelector();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderSearchExtras(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        int searchBtnX = x + w - 56;
        int searchBtnY = y + 4 + getSearchTopOffset();
        String[] searchLabels = {I18n.s("iscript.state.search.key"), I18n.s("iscript.state.search.value"), I18n.s("iscript.state.search.type")};
        boolean searchBtnHovered = mx >= searchBtnX && mx <= searchBtnX + 52 && my >= searchBtnY && my <= searchBtnY + 14;
        g.fill(searchBtnX, searchBtnY, searchBtnX + 52, searchBtnY + 14, searchBtnHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(searchBtnX, searchBtnY, 52, 14, Theme.BORDER);
        g.drawCenteredString(font, searchLabels[searchMode], searchBtnX + 26, searchBtnY + 3, Theme.ACCENT);
    }

    @Override
    protected boolean handleSearchExtrasClick(double mx, double my, int button, int x, int y, int w, int h) {
        int searchBtnX = x + w - 56;
        int searchBtnY = y + 4 + getSearchTopOffset();
        if (mx >= searchBtnX && mx <= searchBtnX + 52 && my >= searchBtnY && my <= searchBtnY + 14) {
            searchMode = (searchMode + 1) % 3;
            lifecycle.selection().scroll(0);
            return true;
        }
        return false;
    }

    @Override
    protected boolean hasCustomModals() {
        return showTargetSelector;
    }

    @Override
    protected void renderCustomModals(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (!showTargetSelector) return;
        g.fill(x, y, x + w, y + h, 0x88000000);
        int sw = Math.min(TARGET_SELECTOR_W, w - 40);
        int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
        int sx = x + (w - sw) / 2;
        int sy = y + (h - sh) / 2;

        g.fill(sx, sy, sx + sw, sy + sh, Theme.BG_INNER);
        g.renderOutline(sx, sy, sw, sh, Theme.BORDER);

        String title = mode == MODE_PLAYER ? I18n.s("iscript.state.selector.player") : I18n.s("iscript.state.selector.entity");
        g.drawCenteredString(this.font, title, sx + sw / 2, sy + 8, Theme.ACCENT);

        int selListY = sy + 28;
        int selItemH = 20;
        int selListH = sh - 60;
        for (int i = lifecycle.state().targetSelectorScroll; i < targetOptions.size(); i++) {
            String option = targetOptions.get(i);
            int itemY = selListY + (i - lifecycle.state().targetSelectorScroll) * selItemH;
            if (itemY + selItemH > selListY + selListH) break;
            boolean hovered = mx >= sx + 8 && mx <= sx + sw - 8 && my >= itemY && my <= itemY + selItemH - 2;
            g.fill(sx + 8, itemY, sx + sw - 8, itemY + selItemH - 2, hovered ? Theme.BG_HOVER : Theme.BG_INNER);
            boolean isPlaceholder = option.equals(I18n.s("iscript.state.selector.no_players")) || option.equals(I18n.s("iscript.state.selector.no_entities"));
            g.drawString(this.font, option, sx + 12, itemY + 4, isPlaceholder ? Theme.TEXT_MUTE : Theme.TEXT);
        }

        int closeY = sy + sh - 26;
        boolean closeHovered = mx >= sx + sw / 2 - 40 && mx <= sx + sw / 2 + 40 && my >= closeY && my <= closeY + 20;
        g.fill(sx + sw / 2 - 40, closeY, sx + sw / 2 + 40, closeY + 20, closeHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(sx + sw / 2 - 40, closeY, 80, 20, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.state.button.close"), sx + sw / 2, closeY + 5, Theme.TEXT);
    }

    @Override
    protected boolean handleCustomModalClick(double mx, double my, int button) {
        if (!showTargetSelector) return false;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int sw = Math.min(TARGET_SELECTOR_W, w - 40);
        int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
        int sx = x + (w - sw) / 2;
        int sy = y + (h - sh) / 2;

        int closeY = sy + sh - 26;
        if (mx >= sx + sw / 2 - 40 && mx <= sx + sw / 2 + 40 && my >= closeY && my <= closeY + 20) {
            showTargetSelector = false;
            return true;
        }

        int selListY = sy + 28;
        int selItemH = 20;
        int selListH = sh - 60;
        for (int i = lifecycle.state().targetSelectorScroll; i < targetOptions.size(); i++) {
            String option = targetOptions.get(i);
            int itemY = selListY + (i - lifecycle.state().targetSelectorScroll) * selItemH;
            if (itemY + selItemH > selListY + selListH) break;
            if (mx >= sx + 8 && mx <= sx + sw - 8 && my >= itemY && my <= itemY + selItemH - 2) {
                if (!option.equals(I18n.s("iscript.state.selector.no_players")) && !option.equals(I18n.s("iscript.state.selector.no_entities"))) {
                    targetId = targetOptionIds.getOrDefault(option, option);
                    requestStates();
                }
                showTargetSelector = false;
                return true;
            }
        }

        if (mx < sx || mx > sx + sw || my < sy || my > sy + sh) {
            showTargetSelector = false;
            return true;
        }
        return true;
    }

    @Override
    protected boolean handleCustomModalKey(int keyCode, int scanCode, int modifiers) {
        if (!showTargetSelector) return false;
        if (keyCode == 256) { showTargetSelector = false; return true; }
        return true;
    }

    @Override
    protected boolean handleCustomModalScroll(double mx, double my, double delta) {
        if (!showTargetSelector) return false;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int sw = Math.min(TARGET_SELECTOR_W, w - 40);
        int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
        int sy = y + (h - sh) / 2;
        int selListY = sy + 28;
        int selListH = sh - 60;
        int selItemH = 20;
        int visibleItems = selListH / selItemH;
        int maxScroll = Math.max(0, targetOptions.size() - visibleItems);
        if (delta > 0) lifecycle.state().targetSelectorScroll = Math.max(0, lifecycle.state().targetSelectorScroll - 1);
        else lifecycle.state().targetSelectorScroll = Math.min(lifecycle.state().targetSelectorScroll + 1, maxScroll);
        return true;
    }

    @Override
    protected boolean handleCustomModalRelease(double mx, double my, int button) {
        if (showTargetSelector) return true;
        return false;
    }

    private void openTargetSelector() {
        targetOptions.clear();
        targetOptionIds.clear();
        showTargetSelector = true;
        lifecycle.state().targetSelectorScroll = 0;

        if (mode == MODE_PLAYER) {
            if (minecraft != null && minecraft.getConnection() != null) {
                for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
                    if (info.getProfile() != null) {
                        String name = info.getProfile().getName();
                        if (name != null && !name.isEmpty()) {
                            targetOptions.add(name);
                            targetOptionIds.put(name, name);
                        }
                    }
                }
            }
            if (targetOptions.isEmpty()) {
                targetOptions.add(I18n.s("iscript.state.selector.no_players"));
            }
        } else if (mode == MODE_ENTITY) {
            if (minecraft != null && minecraft.level != null && minecraft.player != null) {
                List<Entity> nearby = new ArrayList<>();
                for (Entity e : minecraft.level.entitiesForRendering()) {
                    if (e != minecraft.player && e.isAlive() && e.distanceTo(minecraft.player) <= 32.0f) {
                        nearby.add(e);
                    }
                }
                nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(minecraft.player)));
                for (Entity e : nearby) {
                    String name = e.getName().getString();
                    String uuid = e.getUUID().toString();
                    String display = name + " [" + uuid.substring(0, 8) + "...]";
                    targetOptions.add(display);
                    targetOptionIds.put(display, uuid);
                }
            }
            if (targetOptions.isEmpty()) {
                targetOptions.add(I18n.s("iscript.state.selector.no_entities"));
            }
        }
    }

    @Override
    public void removed() {
        doSave();
        showTargetSelector = false;
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (lifecycle.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void receiveStates(CompoundTag data) {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        clientStates.deserialize(data.getCompound("states"));
        if (getSelectedId() != null && !clientStates.has(getSelectedId())) {
            setSelectedId(null);
            lifecycle.editors().removeAll();
        }
    }
}