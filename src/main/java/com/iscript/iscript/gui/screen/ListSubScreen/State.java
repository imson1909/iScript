package com.iscript.iscript.gui.screen.ListSubScreen;

import com.iscript.iscript.api.states.States;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.I18n;
import com.iscript.iscript.gui.screen.SubScreenLifecycle;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.ContextMenu;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class State extends DashboardScreen.SubScreen {
    private final SubScreenLifecycle life = new SubScreenLifecycle(this);
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 200;
    private static final int TARGET_SELECTOR_W = 280;
    private static final int MODE_GLOBAL = 0;
    private static final int MODE_PLAYER = 1;
    private static final int MODE_ENTITY = 2;
    private static final int TYPE_STRING = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_BOOLEAN = 2;

    private States clientStates = new States();
    private ContextMenu contextMenu = new ContextMenu();
    private String contextMenuKey = null;
    private String confirmDialogKey = null;

    private boolean showTargetSelector = false;
    private List<String> targetOptions = new ArrayList<>();
    private Map<String, String> targetOptionIds = new LinkedHashMap<>();

    private int mode = MODE_GLOBAL;
    private String targetId = "";
    private int valueType = TYPE_STRING;
    private int searchMode = 0;

    public State(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        life.init();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int topOffset = 26;
        int searchW = RIGHT_PANEL_WIDTH - 72;
        life.search().request(rightX + 4, y + 4 + topOffset, searchW, 16, I18n.t("iscript.state.list.search"));

        life.modals().register("confirm",
                () -> life.state().modalOpenFlags.getOrDefault("confirm", false),
                v -> life.state().modalOpenFlags.put("confirm", v),
                () -> {},
                () -> {},
                "confirm"
        );

        life.state().modalOpenFlags.put("confirm", false);
        life.save().clearDirty();

        if (mode != MODE_GLOBAL && targetId.isEmpty()) {
            clientStates = new States();
            life.selection().scroll(0);
        }
        if (life.selection().get() != null) {
            buildEditor(life.selection().get());
        }
        requestStates();
    }

    private void requestStates() {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        CompoundTag data = new CompoundTag();
        data.putInt("mode", mode);
        if (mode != MODE_GLOBAL) {
            data.putString("target", targetId);
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_STATES, data));
    }

    @Override
    public void tick() {
        life.tick(this::doSave);
        boolean anyFocused = (life.editors().box("key") != null && life.editors().box("key").isFocused()) ||
                (life.editors().box("value") != null && life.editors().box("value").isFocused()) ||
                (life.search().box() != null && life.search().box().isFocused());
        if (life.save().isDirty() && !anyFocused) {
            doSave();
        }
        if (anyFocused) life.save().markDirty();
    }

    private List<String> filteredKeys() {
        List<String> keys = new ArrayList<>(clientStates.keys());
        String filter = life.search().box() != null ? life.search().box().getValue().trim().toLowerCase() : life.state().lastSearch.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (key == null) key = "";
            if (filter.isEmpty()) {
                result.add(key);
                continue;
            }
            boolean match = false;
            switch (searchMode) {
                case 0 -> match = key.toLowerCase().contains(filter);
                case 1 -> {
                    Object val = clientStates.getValues().get(key);
                    match = val != null && val.toString().toLowerCase().contains(filter);
                }
                case 2 -> {
                    Object val = clientStates.getValues().get(key);
                    String typeStr = "string";
                    if (val instanceof Boolean) typeStr = "boolean";
                    else if (val instanceof Number) typeStr = "number";
                    match = typeStr.startsWith(filter);
                }
            }
            if (match) result.add(key);
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void doSave() {
        if (!life.save().isDirty()) return;
        if (mode != MODE_GLOBAL && targetId.isEmpty()) {
            life.save().clearDirty();
            return;
        }
        life.save().clearDirty();
        applyEditorToState();
        CompoundTag data = new CompoundTag();
        data.putInt("mode", mode);
        if (mode != MODE_GLOBAL) {
            data.putString("target", targetId);
        }
        data.put("states", clientStates.serialize());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_STATES, data));
    }

    private void clearEditorWidgets() {
        life.editors().remove("key");
        life.editors().remove("value");
    }

    private void buildEditor(String key) {
        clearEditorWidgets();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        EditBox keyBox = life.editors().addBox("key", leftX, leftY + 12, leftW, 20, I18n.t("iscript.state.editor.placeholder.key"), key != null ? key : "");
        if (keyBox != null) keyBox.setEditable(true);

        if (key != null && clientStates.has(key)) {
            Object val = clientStates.getValues().get(key);
            if (val instanceof Boolean) {
                valueType = TYPE_BOOLEAN;
            } else if (val instanceof Number) {
                valueType = TYPE_NUMBER;
            } else {
                valueType = TYPE_STRING;
            }
        } else {
            valueType = TYPE_STRING;
        }

        if (valueType != TYPE_BOOLEAN) {
            String valStr = "";
            if (key != null && clientStates.has(key)) {
                if (valueType == TYPE_NUMBER && clientStates.isNumber(key)) {
                    valStr = String.valueOf(clientStates.getNumber(key));
                } else {
                    Object val = clientStates.getValues().get(key);
                    valStr = val != null ? val.toString() : "";
                }
            }
            life.editors().addBox("value", leftX, leftY + 50, leftW, 20, I18n.t("iscript.state.editor.placeholder.value"), valStr);
        }
    }

    private void switchToKey(String key) {
        doSave();
        life.selection().set(key);
        clearEditorWidgets();
        life.save().clearDirty();
        buildEditor(key);
    }

    private void addNewState() {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        int index = 1;
        String key = "state_" + index;
        while (clientStates.has(key)) {
            index++;
            key = "state_" + index;
        }
        clientStates.setString(key, "");
        switchToKey(key);
        life.save().markDirty();
    }

    private void deleteState(String key) {
        clientStates.remove(key);
        if (life.selection().get() != null && life.selection().get().equals(key)) {
            life.selection().set(null);
            clearEditorWidgets();
        }
        life.save().markDirty();
    }

    private void openConfirmDialog(String key) {
        confirmDialogKey = key;
        life.modals().open("confirm");
    }

    private void closeConfirmDialog() {
        life.modals().close("confirm");
        confirmDialogKey = null;
    }

    private void executeConfirm() {
        if (confirmDialogKey != null) deleteState(confirmDialogKey);
        closeConfirmDialog();
    }

    private void copyItem(String key) {
        DashboardScreen.clipboard = key;
    }

    private void pasteItem() {
        String sourceKey = DashboardScreen.clipboard;
        if (sourceKey == null || sourceKey.isEmpty()) return;
        if (!clientStates.has(sourceKey)) return;

        String baseKey = sourceKey + "_copy";
        String newKey = baseKey;
        int counter = 1;
        while (clientStates.has(newKey)) {
            newKey = baseKey + "_" + counter;
            counter++;
        }

        Object val = clientStates.getValues().get(sourceKey);
        clientStates.set(newKey, copyValue(val));
        life.save().markDirty();
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        doSave();
    }

    private void duplicateItem(String key) {
        if (!clientStates.has(key)) return;

        String baseKey = key;
        String newKey = key + "_1";
        int counter = 1;
        while (clientStates.has(newKey)) {
            counter++;
            newKey = baseKey + "_" + counter;
        }

        Object val = clientStates.getValues().get(key);
        clientStates.set(newKey, copyValue(val));
        life.save().markDirty();
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        doSave();
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

    private void cycleSearchMode() {
        searchMode = (searchMode + 1) % 3;
        life.selection().scroll(0);
    }

    private void cycleValueType() {
        valueType = (valueType + 1) % 3;
        if (life.selection().get() != null) {
            String key = life.selection().get();
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
            life.save().markDirty();
        }
    }

    private void toggleBooleanValue() {
        if (life.selection().get() != null && valueType == TYPE_BOOLEAN) {
            Object current = clientStates.getValues().get(life.selection().get());
            boolean newVal = !(current instanceof Boolean && (Boolean) current);
            clientStates.set(life.selection().get(), newVal);
            life.save().markDirty();
        }
    }

    private void applyEditorToState() {
        if (life.selection().get() == null) return;
        EditBox keyBox = life.editors().box("key");
        if (keyBox == null) return;
        String newKey = keyBox.getValue().trim();
        if (newKey.isEmpty()) return;

        String selKey = life.selection().get();
        if (!newKey.equals(selKey)) {
            Object oldValue = clientStates.getValues().get(selKey);
            clientStates.remove(selKey);
            life.selection().set(newKey);
            clientStates.set(newKey, oldValue);
        }

        if (valueType == TYPE_BOOLEAN) {
        } else {
            EditBox valueBox = life.editors().box("value");
            if (valueBox != null) {
                String valStr = valueBox.getValue();
                if (valueType == TYPE_NUMBER) {
                    try {
                        double d = Double.parseDouble(valStr);
                        clientStates.setNumber(life.selection().get(), d);
                    } catch (NumberFormatException e) {
                        clientStates.setString(life.selection().get(), valStr);
                        valueType = TYPE_STRING;
                    }
                } else {
                    clientStates.setString(life.selection().get(), valStr);
                }
            }
        }
    }

    @Override
    public void removed() {
        doSave();
        life.removed();
        closeConfirmDialog();
        clearEditorWidgets();
        showTargetSelector = false;
        contextMenu.close();
        super.removed();
    }

    public void receiveStates(CompoundTag data) {
        if (mode != MODE_GLOBAL && targetId.isEmpty()) return;
        clientStates.deserialize(data.getCompound("states"));
        if (life.selection().get() != null && !clientStates.has(life.selection().get())) {
            life.selection().set(null);
            clearEditorWidgets();
        }
    }

    private void openTargetSelector() {
        targetOptions.clear();
        targetOptionIds.clear();
        showTargetSelector = true;
        life.state().targetSelectorScroll = 0;

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        boolean modalOpen = life.modals().isOpen("confirm") || showTargetSelector;
        if (life.search().box() != null) life.search().setVisible(!modalOpen);
        EditBox keyBox = life.editors().box("key");
        EditBox valueBox = life.editors().box("value");
        if (keyBox != null) keyBox.setVisible(!modalOpen);
        if (valueBox != null) valueBox.setVisible(!modalOpen);

        if (life.modals().isOpen("confirm")) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = this.parent.height / 2 - 30;

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
            graphics.drawCenteredString(this.font, String.format(I18n.s("iscript.state.confirm.delete"), confirmDialogKey), cx, dy + 8, Theme.ERROR);

            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
            graphics.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx - 50, dy + 38, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.state.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);

            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.state.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
            return;
        }

        if (showTargetSelector) {
            graphics.fill(x, y, x + w, y + h, 0x88000000);
            int sw = Math.min(TARGET_SELECTOR_W, w - 40);
            int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
            int sx = x + (w - sw) / 2;
            int sy = y + (h - sh) / 2;

            graphics.fill(sx, sy, sx + sw, sy + sh, Theme.BG_INNER);
            graphics.renderOutline(sx, sy, sw, sh, Theme.BORDER);

            String title = mode == MODE_PLAYER ? I18n.s("iscript.state.selector.player") : I18n.s("iscript.state.selector.entity");
            graphics.drawCenteredString(this.font, title, sx + sw / 2, sy + 8, Theme.ACCENT);

            int selListY = sy + 28;
            int selItemH = 20;
            int selListH = sh - 60;
            for (int i = life.state().targetSelectorScroll; i < targetOptions.size(); i++) {
                String option = targetOptions.get(i);
                int itemY = selListY + (i - life.state().targetSelectorScroll) * selItemH;
                if (itemY + selItemH > selListY + selListH) break;
                boolean hovered = mouseX >= sx + 8 && mouseX <= sx + sw - 8 && mouseY >= itemY && mouseY <= itemY + selItemH - 2;
                graphics.fill(sx + 8, itemY, sx + sw - 8, itemY + selItemH - 2, hovered ? Theme.BG_HOVER : Theme.BG_INNER);
                boolean isPlaceholder = option.equals(I18n.s("iscript.state.selector.no_players")) || option.equals(I18n.s("iscript.state.selector.no_entities"));
                graphics.drawString(this.font, option, sx + 12, itemY + 4, isPlaceholder ? Theme.TEXT_MUTE : Theme.TEXT);
            }

            int closeY = sy + sh - 26;
            boolean closeHovered = mouseX >= sx + sw / 2 - 40 && mouseX <= sx + sw / 2 + 40 && mouseY >= closeY && mouseY <= closeY + 20;
            graphics.fill(sx + sw / 2 - 40, closeY, sx + sw / 2 + 40, closeY + 20, closeHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(sx + sw / 2 - 40, closeY, 80, 20, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.state.button.close"), sx + sw / 2, closeY + 5, Theme.TEXT);
            return;
        }

        graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
        graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);

        String[] modeLabels = {I18n.s("iscript.state.mode.global"), I18n.s("iscript.state.mode.player"), I18n.s("iscript.state.mode.entity")};
        int topOffset = 26;
        int searchBtnX = rightX + RIGHT_PANEL_WIDTH - 62;
        int searchBtnY = y + 4 + topOffset;

        int tabW = RIGHT_PANEL_WIDTH / 3;
        for (int i = 0; i < 3; i++) {
            int tabX = rightX + i * tabW;
            boolean tabHovered = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= y + 2 && mouseY <= y + 22;
            boolean tabSelected = mode == i;
            int tabColor = tabSelected ? Theme.ACCENT : (tabHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.fill(tabX, y + 2, tabX + tabW, y + 22, tabColor);
            graphics.renderOutline(tabX, y + 2, tabW, 20, Theme.BORDER);
            graphics.drawCenteredString(this.font, modeLabels[i], tabX + tabW / 2, y + 7, tabSelected ? Theme.ACCENT : Theme.TEXT);
        }

        if (life.search().box() != null) {
            life.search().setPos(rightX + 4, y + 4 + topOffset, RIGHT_PANEL_WIDTH - 72, 16);
            life.search().setVisible(true);
            life.search().box().render(graphics, mouseX, mouseY, partialTick);
        }

        String[] searchLabels = {I18n.s("iscript.state.search.key"), I18n.s("iscript.state.search.value"), I18n.s("iscript.state.search.type")};
        boolean searchBtnHovered = mouseX >= searchBtnX && mouseX <= searchBtnX + 56 && mouseY >= searchBtnY && mouseY <= searchBtnY + 16;
        graphics.fill(searchBtnX, searchBtnY, searchBtnX + 56, searchBtnY + 16, searchBtnHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(searchBtnX, searchBtnY, 56, 16, Theme.BORDER);
        graphics.drawCenteredString(this.font, searchLabels[searchMode], searchBtnX + 28, searchBtnY + 4, Theme.ACCENT);

        int extraOffset = 0;
        if (mode != MODE_GLOBAL) {
            int targetBtnY = y + 24 + topOffset;
            int targetBtnH = 18;
            String targetLabel;
            if (mode == MODE_PLAYER) {
                targetLabel = targetId.isEmpty() ? I18n.s("iscript.state.target.select_player") : String.format(I18n.s("iscript.state.target.player"), targetId);
            } else {
                targetLabel = targetId.isEmpty() ? I18n.s("iscript.state.target.select_entity") : String.format(I18n.s("iscript.state.target.entity"), targetId.substring(0, Math.min(16, targetId.length())) + (targetId.length() > 16 ? "..." : ""));
            }
            boolean targetHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= targetBtnY && mouseY <= targetBtnY + targetBtnH;
            graphics.fill(rightX + 4, targetBtnY, x + w - 4, targetBtnY + targetBtnH, targetHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(rightX + 4, targetBtnY, RIGHT_PANEL_WIDTH - 8, targetBtnH, Theme.BORDER);
            graphics.drawCenteredString(this.font, targetLabel, rightX + RIGHT_PANEL_WIDTH / 2, targetBtnY + 4, Theme.ACCENT);
            extraOffset += 24;
        }

        graphics.drawString(this.font, I18n.s("iscript.state.list.title"), rightX + 8, y + 26 + topOffset + extraOffset, Theme.ACCENT);

        List<String> keys = filteredKeys();
        int listH = h - 68 - topOffset - extraOffset;
        int listY = y + 42 + topOffset + extraOffset;
        int scroll = life.selection().scroll();

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
            String key = keys.get(i);
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
            boolean selected = key.equals(life.selection().get());
            int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
            graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);

            Object val = clientStates.getValues().get(key);
            String preview;
            if (val instanceof Boolean) {
                preview = (Boolean) val ? I18n.s("iscript.state.boolean.true") : I18n.s("iscript.state.boolean.false");
            } else if (val instanceof Number) {
                preview = String.valueOf(((Number) val).doubleValue());
            } else {
                preview = "\"" + val + "\"";
            }
            String display = key + " = " + preview;
            if (this.font.width(display) > RIGHT_PANEL_WIDTH - 16) {
                display = this.font.plainSubstrByWidth(display, RIGHT_PANEL_WIDTH - 16) + "...";
            }
            graphics.drawString(this.font, display, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
        }

        int newY = y + h - 28;
        boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
        boolean newDisabled = mode != MODE_GLOBAL && targetId.isEmpty();
        int newColor = newDisabled ? Theme.TEXT_MUTE : (newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newColor);
        graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.state.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, newDisabled ? Theme.TEXT_MUTE : Theme.ACCENT);

        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        if (life.selection().get() != null) {
            graphics.drawString(this.font, I18n.s("iscript.state.editor.label.key"), leftX, leftY, Theme.TEXT_MUTE);
            graphics.drawString(this.font, I18n.s("iscript.state.editor.label.value"), leftX, leftY + 38, Theme.TEXT_MUTE);

            if (keyBox != null) {
                keyBox.setX(leftX);
                keyBox.setY(leftY + 12);
                keyBox.setWidth(leftW);
                keyBox.render(graphics, mouseX, mouseY, partialTick);
            }

            String[] typeLabels = {I18n.s("iscript.state.type.string"), I18n.s("iscript.state.type.number"), I18n.s("iscript.state.type.boolean")};
            int typeBtnY = leftY + 74;
            boolean typeHovered = mouseX >= leftX && mouseX <= leftX + 80 && mouseY >= typeBtnY && mouseY <= typeBtnY + 18;
            graphics.fill(leftX, typeBtnY, leftX + 80, typeBtnY + 18, typeHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(leftX, typeBtnY, 80, 18, Theme.BORDER);
            graphics.drawString(this.font, "Type: " + typeLabels[valueType], leftX + 4, typeBtnY + 4, Theme.ACCENT);

            if (valueType == TYPE_BOOLEAN) {
                Object val = clientStates.getValues().get(life.selection().get());
                boolean currentBool = val instanceof Boolean && (Boolean) val;
                int boolY = leftY + 50;
                boolean boolHovered = mouseX >= leftX && mouseX <= leftX + 60 && mouseY >= boolY && mouseY <= boolY + 18;
                graphics.fill(leftX, boolY, leftX + 60, boolY + 18, currentBool ? 0xFF44AA44 : 0xFFAA4444);
                graphics.renderOutline(leftX, boolY, 60, 18, Theme.BORDER);
                graphics.drawCenteredString(this.font, currentBool ? I18n.s("iscript.toggle.on") : I18n.s("iscript.toggle.off"), leftX + 30, boolY + 4, 0xFFFFFFFF);
            } else {
                if (valueBox != null) {
                    valueBox.setX(leftX);
                    valueBox.setY(leftY + 50);
                    valueBox.setWidth(leftW);
                    valueBox.render(graphics, mouseX, mouseY, partialTick);
                }
            }

            if (life.save().isDirty()) {
                graphics.drawString(this.font, "*", leftX + leftW - 10, leftY, Theme.ERROR);
            }
        } else {
            clearEditorWidgets();
            graphics.drawCenteredString(this.font, I18n.s("iscript.state.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
        }

        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (life.modals().isOpen("confirm")) {
            int cx = x + w / 2;
            int dy = this.parent.height / 2 - 30;
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

        if (showTargetSelector) {
            int sw = Math.min(TARGET_SELECTOR_W, w - 40);
            int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
            int sx = x + (w - sw) / 2;
            int sy = y + (h - sh) / 2;

            int closeY = sy + sh - 26;
            if (mouseX >= sx + sw / 2 - 40 && mouseX <= sx + sw / 2 + 40 && mouseY >= closeY && mouseY <= closeY + 20) {
                showTargetSelector = false;
                return true;
            }

            int selListY = sy + 28;
            int selItemH = 20;
            int selListH = sh - 60;
            for (int i = life.state().targetSelectorScroll; i < targetOptions.size(); i++) {
                String option = targetOptions.get(i);
                int itemY = selListY + (i - life.state().targetSelectorScroll) * selItemH;
                if (itemY + selItemH > selListY + selListH) break;
                if (mouseX >= sx + 8 && mouseX <= sx + sw - 8 && mouseY >= itemY && mouseY <= itemY + selItemH - 2) {
                    if (!option.equals(I18n.s("iscript.state.selector.no_players")) && !option.equals(I18n.s("iscript.state.selector.no_entities"))) {
                        targetId = targetOptionIds.getOrDefault(option, option);
                        requestStates();
                    }
                    showTargetSelector = false;
                    return true;
                }
            }

            if (mouseX < sx || mouseX > sx + sw || mouseY < sy || mouseY > sy + sh) {
                showTargetSelector = false;
                return true;
            }
            return true;
        }

        int topOffset = 26;

        if (button == 1) {
            int extraOffset = (mode != MODE_GLOBAL ? 24 : 0);
            int listH = h - 68 - topOffset - extraOffset;
            int listY = y + 42 + topOffset + extraOffset;
            List<String> keys = filteredKeys();
            int scroll = life.selection().scroll();
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    String key = keys.get(i);
                    contextMenuKey = key;
                    boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty() && clientStates.has(DashboardScreen.clipboard);
                    contextMenu.setCustomActions(new String[]{"Copy", "Paste", "Duplicate", "Delete"});
                    contextMenu.open((int) mouseX, (int) mouseY, key, canPaste);
                    return true;
                }
            }
        }

        if (contextMenu.isOpen()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button)) {
                String action = contextMenu.getLastAction();
                if (action != null) {
                    switch (action) {
                        case "Copy":
                            if (contextMenuKey != null) copyItem(contextMenuKey);
                            break;
                        case "Paste":
                            pasteItem();
                            break;
                        case "Duplicate":
                            if (contextMenuKey != null) duplicateItem(contextMenuKey);
                            break;
                        case "Delete":
                            if (contextMenuKey != null) openConfirmDialog(contextMenuKey);
                            break;
                    }
                }
                contextMenu.close();
                return true;
            }
            contextMenu.close();
        }

        if (button != 0) return false;

        if (mode != MODE_GLOBAL) {
            int targetBtnY = y + 24 + topOffset;
            int targetBtnH = 18;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= targetBtnY && mouseY <= targetBtnY + targetBtnH) {
                openTargetSelector();
                return true;
            }
        }

        int searchBtnX = rightX + RIGHT_PANEL_WIDTH - 62;
        int searchBtnY = y + 4 + topOffset;
        if (mouseX >= searchBtnX && mouseX <= searchBtnX + 56 && mouseY >= searchBtnY && mouseY <= searchBtnY + 16) {
            cycleSearchMode();
            return true;
        }

        int tabW = RIGHT_PANEL_WIDTH / 3;
        for (int i = 0; i < 3; i++) {
            int tabX = rightX + i * tabW;
            if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= y + 2 && mouseY <= y + 22) {
                if (mode != i) {
                    doSave();
                    mode = i;
                    life.selection().set(null);
                    clearEditorWidgets();
                    showTargetSelector = false;
                    targetId = "";
                    init();
                }
                return true;
            }
        }

        int extraOffset = (mode != MODE_GLOBAL ? 24 : 0);
        int listH = h - 68 - topOffset - extraOffset;
        int listY = y + 42 + topOffset + extraOffset;

        List<String> keys = filteredKeys();
        int scroll = life.selection().scroll();
        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String key = keys.get(i);
                if (!key.equals(life.selection().get())) {
                    switchToKey(key);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            if (!(mode != MODE_GLOBAL && targetId.isEmpty())) {
                addNewState();
            }
            return true;
        }

        if (life.selection().get() != null) {
            int leftX = x + 8;
            int leftY = y + 8;
            EditBox keyBox = life.editors().box("key");
            EditBox valueBox = life.editors().box("value");

            if (keyBox != null && mouseX >= keyBox.getX() && mouseX <= keyBox.getX() + keyBox.getWidth() && mouseY >= keyBox.getY() && mouseY <= keyBox.getY() + keyBox.getHeight()) {
                keyBox.setFocused(true);
                return keyBox.mouseClicked(mouseX, mouseY, button);
            }

            int typeBtnY = leftY + 74;
            if (mouseX >= leftX && mouseX <= leftX + 80 && mouseY >= typeBtnY && mouseY <= typeBtnY + 18) {
                cycleValueType();
                return true;
            }

            if (valueType == TYPE_BOOLEAN) {
                int boolY = leftY + 50;
                if (mouseX >= leftX && mouseX <= leftX + 60 && mouseY >= boolY && mouseY <= boolY + 18) {
                    toggleBooleanValue();
                    return true;
                }
            } else {
                if (valueBox != null && mouseX >= valueBox.getX() && mouseX <= valueBox.getX() + valueBox.getWidth() && mouseY >= valueBox.getY() && mouseY <= valueBox.getY() + valueBox.getHeight()) {
                    valueBox.setFocused(true);
                    return valueBox.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("confirm") || showTargetSelector || contextMenu.isOpen()) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;

        if (showTargetSelector) {
            int sw = Math.min(TARGET_SELECTOR_W, w - 40);
            int sh = Math.min(Math.max(120, targetOptions.size() * 20 + 60), h - 60);
            int sy = y + (h - sh) / 2;
            int selListY = sy + 28;
            int selListH = sh - 60;
            int selItemH = 20;
            int visibleItems = selListH / selItemH;
            int maxScroll = Math.max(0, targetOptions.size() - visibleItems);
            if (delta > 0) life.state().targetSelectorScroll = Math.max(0, life.state().targetSelectorScroll - 1);
            else life.state().targetSelectorScroll = Math.min(life.state().targetSelectorScroll + 1, maxScroll);
            return true;
        }
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            int topOffset = 26;
            int extraOffset = (mode != MODE_GLOBAL ? 24 : 0);
            int listH = h - 68 - topOffset - extraOffset;
            List<String> keys = filteredKeys();
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, keys.size() - visible);
            int scroll = life.selection().scroll();
            if (delta > 0) life.selection().scroll(Math.max(0, scroll - 1));
            else life.selection().scroll(Math.min(scroll + 1, maxScroll));
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showTargetSelector) return true;
        if (life.search().box() != null && life.search().box().isFocused()) {
            return life.search().box().charTyped(codePoint, modifiers);
        }
        EditBox keyBox = life.editors().box("key");
        EditBox valueBox = life.editors().box("value");
        if (keyBox != null && keyBox.isFocused()) {
            return keyBox.charTyped(codePoint, modifiers);
        }
        if (valueBox != null && valueBox.isFocused()) {
            return valueBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showTargetSelector) {
            if (keyCode == 256) {
                showTargetSelector = false;
                return true;
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

        if (life.search().box() != null && life.search().box().isFocused()) {
            return life.search().box().keyPressed(keyCode, scanCode, modifiers);
        }
        EditBox keyBox = life.editors().box("key");
        EditBox valueBox = life.editors().box("value");
        if (keyBox != null && keyBox.isFocused()) {
            return keyBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (valueBox != null && valueBox.isFocused()) {
            return valueBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}