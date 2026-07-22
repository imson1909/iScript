package com.iscript.iscript.gui.screen;

import com.iscript.iscript.api.states.States;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StateListSubScreen extends DashboardScreen.SubScreen {
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;

    private int scroll = 0;
    private States clientStates = new States();
    private String selectedKey = null;
    private boolean dirty = false;

    private EditBox keyEditBox;
    private EditBox valueEditBox;
    private List<EditBox> editorWidgets = new ArrayList<>();
    private EditBox searchBox = null;

    private boolean showConfirmDialog = false;
    private int confirmDialogY = 0;
    private String confirmDialogKey = null;

    public StateListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        dirty = false;
        clearEditorWidgets();
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_STATES, new CompoundTag()));
    }

    @Override
    public void tick() {
        boolean anyFocused = (keyEditBox != null && keyEditBox.isFocused()) ||
                (valueEditBox != null && valueEditBox.isFocused()) ||
                (searchBox != null && searchBox.isFocused());
        if (dirty && !anyFocused) {
            doSave();
        }
        if (anyFocused) markDirty();
        if (searchBox == null && this.minecraft != null) {
            createSearchBox();
        }
    }

    private void createSearchBox() {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        searchBox = new EditBox(this.minecraft.font, rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.state.list.search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(Theme.TEXT);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);
    }

    private List<String> filteredKeys() {
        List<String> keys = new ArrayList<>(clientStates.keys());
        String filter = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (key == null) key = "";
            if (filter.isEmpty() || key.toLowerCase().contains(filter)) {
                result.add(key);
            }
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void markDirty() {
        dirty = true;
    }

    private void doSave() {
        if (!dirty) return;
        dirty = false;
        applyEditorToState();
        CompoundTag data = new CompoundTag();
        data.put("states", clientStates.serialize());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_STATES, data));
    }

    private void clearEditorWidgets() {
        for (EditBox w : editorWidgets) parent.removeEditorWidget(w);
        editorWidgets.clear();
        keyEditBox = null;
        valueEditBox = null;
    }

    private void buildEditor(String key) {
        clearEditorWidgets();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        keyEditBox = new EditBox(this.minecraft.font, leftX, leftY + 10, leftW, 20, I18n.t("iscript.state.editor.placeholder.key"));
        keyEditBox.setMaxLength(64);
        keyEditBox.setValue(key != null ? key : "");
        keyEditBox.setEditable(key == null);
        parent.addWidget(keyEditBox);
        editorWidgets.add(keyEditBox);

        valueEditBox = new EditBox(this.minecraft.font, leftX, leftY + 48, leftW, 20, I18n.t("iscript.state.editor.placeholder.value"));
        valueEditBox.setMaxLength(256);
        if (key != null && clientStates.has(key)) {
            if (clientStates.isNumber(key)) {
                valueEditBox.setValue(String.valueOf(clientStates.getNumber(key)));
            } else {
                valueEditBox.setValue(clientStates.getString(key));
            }
        } else {
            valueEditBox.setValue("");
        }
        parent.addWidget(valueEditBox);
        editorWidgets.add(valueEditBox);
    }

    private void switchToKey(String key) {
        doSave();
        selectedKey = key;
        clearEditorWidgets();
        dirty = false;
        buildEditor(key);
    }

    private void addNewState() {
        int index = 1;
        String key = "state_" + index;
        while (clientStates.has(key)) {
            index++;
            key = "state_" + index;
        }
        clientStates.setNumber(key, 0);
        switchToKey(key);
        markDirty();
    }

    private void deleteState(String key) {
        clientStates.remove(key);
        if (selectedKey != null && selectedKey.equals(key)) {
            selectedKey = null;
            clearEditorWidgets();
        }
        markDirty();
    }

    private void openConfirmDialog(String key) {
        showConfirmDialog = true;
        confirmDialogKey = key;
        confirmDialogY = this.parent.height / 2 - 30;
    }

    private void closeConfirmDialog() {
        showConfirmDialog = false;
        confirmDialogKey = null;
    }

    private void executeConfirm() {
        if (confirmDialogKey != null) deleteState(confirmDialogKey);
        closeConfirmDialog();
    }

    private void applyEditorToState() {
        if (selectedKey == null || keyEditBox == null || valueEditBox == null) return;
        String newKey = keyEditBox.getValue().trim();
        String valStr = valueEditBox.getValue();

        if (newKey.isEmpty()) return;

        if (!newKey.equals(selectedKey)) {
            Object oldValue = clientStates.getValues().get(selectedKey);
            clientStates.remove(selectedKey);
            selectedKey = newKey;
            if (oldValue instanceof Number) {
                try {
                    clientStates.setNumber(newKey, Double.parseDouble(valStr));
                } catch (NumberFormatException e) {
                    clientStates.setString(newKey, valStr);
                }
            } else {
                clientStates.setString(newKey, valStr);
            }
        } else {
            try {
                double d = Double.parseDouble(valStr);
                clientStates.setNumber(selectedKey, d);
            } catch (NumberFormatException e) {
                clientStates.setString(selectedKey, valStr);
            }
        }
    }

    @Override
    public void removed() {
        doSave();
        closeConfirmDialog();
        clearEditorWidgets();
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        super.removed();
    }

    public void receiveStates(CompoundTag data) {
        clientStates.deserialize(data.getCompound("states"));
        if (selectedKey != null && !clientStates.has(selectedKey)) {
            selectedKey = null;
            clearEditorWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        if (showConfirmDialog) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = confirmDialogY;

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
            graphics.drawCenteredString(this.font, "Delete \"" + confirmDialogKey + "\"?", cx, dy + 8, Theme.ERROR);

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

        graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
        graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);

        graphics.drawString(this.font, I18n.s("iscript.state.list.title"), rightX + 8, y + 26, Theme.ACCENT);

        if (searchBox != null) {
            searchBox.setX(rightX + 4);
            searchBox.setY(y + 4);
            searchBox.setWidth(RIGHT_PANEL_WIDTH - 8);
            searchBox.setHeight(16);
            searchBox.setVisible(true);
        }

        List<String> keys = filteredKeys();
        int listH = h - 68;
        int listY = y + 42;

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
            String key = keys.get(i);
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
            boolean selected = key.equals(selectedKey);
            int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
            graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);

            Object val = clientStates.getValues().get(key);
            String preview = val instanceof Number ? String.valueOf(((Number) val).doubleValue()) : "\"" + val + "\"";
            String display = key + " = " + preview;
            if (this.font.width(display) > RIGHT_PANEL_WIDTH - 16) {
                display = this.font.plainSubstrByWidth(display, RIGHT_PANEL_WIDTH - 16) + "...";
            }
            graphics.drawString(this.font, display, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
        }

        int newY = y + h - 28;
        boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
        graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
        graphics.drawCenteredString(this.font, I18n.s("iscript.state.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);

        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        if (selectedKey != null) {
            graphics.drawString(this.font, I18n.s("iscript.state.editor.label.key"), leftX, leftY - 2, Theme.TEXT_MUTE);
            graphics.drawString(this.font, I18n.s("iscript.state.editor.label.value"), leftX, leftY + 34, Theme.TEXT_MUTE);

            if (keyEditBox != null) {
                keyEditBox.setX(leftX);
                keyEditBox.setY(leftY + 10);
                keyEditBox.setWidth(leftW);
            }
            if (valueEditBox != null) {
                valueEditBox.setX(leftX);
                valueEditBox.setY(leftY + 42);
                valueEditBox.setWidth(leftW);
            }

            if (dirty) {
                graphics.drawString(this.font, "*", leftX + leftW - 10, leftY - 2, Theme.ERROR);
            }
        } else {
            clearEditorWidgets();
            graphics.drawCenteredString(this.font, I18n.s("iscript.state.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfirmDialog) {
            int x = DashboardScreen.SIDEBAR_W;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
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

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (button == 1) {
            List<String> keys = filteredKeys();
            int listH = h - 68;
            int listY = y + 42;

            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    openConfirmDialog(keys.get(i));
                    return true;
                }
            }
            return false;
        }

        if (button != 0) return false;

        List<String> keys = filteredKeys();
        int listH = h - 68;
        int listY = y + 42;

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, keys.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String key = keys.get(i);
                if (!key.equals(selectedKey)) {
                    switchToKey(key);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            addNewState();
            return true;
        }

        if (selectedKey != null) {
            int leftX = x + 8;
            int leftY = y + 8;

            if (keyEditBox != null && mouseX >= keyEditBox.getX() && mouseX <= keyEditBox.getX() + keyEditBox.getWidth() && mouseY >= keyEditBox.getY() && mouseY <= keyEditBox.getY() + keyEditBox.getHeight()) {
                parent.setFocusedWidget(keyEditBox);
                return keyEditBox.mouseClicked(mouseX, mouseY, button);
            }
            if (valueEditBox != null && mouseX >= valueEditBox.getX() && mouseX <= valueEditBox.getX() + valueEditBox.getWidth() && mouseY >= valueEditBox.getY() && mouseY <= valueEditBox.getY() + valueEditBox.getHeight()) {
                parent.setFocusedWidget(valueEditBox);
                return valueEditBox.mouseClicked(mouseX, mouseY, button);
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showConfirmDialog) return true;

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            List<String> keys = filteredKeys();
            int visible = Math.max(1, (h - 68 - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, keys.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        if (keyEditBox != null && keyEditBox.isFocused()) {
            return keyEditBox.charTyped(codePoint, modifiers);
        }
        if (valueEditBox != null && valueEditBox.isFocused()) {
            return valueEditBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyEditBox != null && keyEditBox.isFocused()) {
            return keyEditBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (valueEditBox != null && valueEditBox.isFocused()) {
            return valueEditBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}