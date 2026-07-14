package com.iscript.iscript.gui.screen;

import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.RequestStateMachinePacket;
import com.iscript.iscript.network.packet.RequestStateMachinesPacket;
import com.iscript.iscript.network.packet.SaveStateMachinePacket;
import com.iscript.iscript.network.packet.RenameStateMachinePacket;
import com.iscript.iscript.network.packet.DeleteStateMachinePacket;
import com.iscript.iscript.network.packet.DuplicateStateMachinePacket;
import com.iscript.iscript.data.state.StateMachineManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class StateListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 160;

    private String selectedId = null;
    private String editorMachineId = null;
    private String loadingMachineId = null;

    private boolean showNameDialog = false;
    private EditBox nameInputBox = null;
    private int nameDialogY = 0;
    private String nameDialogMode = "";
    private String renameOldId = null;

    private boolean showConfirmDialog = false;
    private int confirmDialogY = 0;
    private String confirmDialogAction = "";
    private String confirmDialogId = null;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private String contextMenuItemId = null;

    private int leftPanelMode = 0;
    private static final int MODE_EMPTY = 0;
    private static final int MODE_LOADING = 1;
    private static final int MODE_EDITOR = 2;

    private List<EditBox> editorWidgets = new ArrayList<>();
    private EditBox nameEditBox = null;
    private EditBox entryNodeEditBox = null;

    private boolean dirty = false;

    public StateListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        showNameDialog = false;
        nameInputBox = null;
        showContextMenu = false;
        showConfirmDialog = false;
        clearEditorWidgets();
        dirty = false;
        IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
    }

    @Override
    public void tick() {
        boolean anyFocused = (nameEditBox != null && nameEditBox.isFocused()) ||
                (entryNodeEditBox != null && entryNodeEditBox.isFocused());
        if (dirty && !anyFocused) {
            doSave();
        }
        if (anyFocused) {
            markDirty();
        }
    }

    private void markDirty() {
        dirty = true;
    }

    private void doSave() {
        if (!dirty || editorMachineId == null) return;
        dirty = false;
        String name = nameEditBox != null ? nameEditBox.getValue() : "";
        String entry = entryNodeEditBox != null ? entryNodeEditBox.getValue() : "";
        IScriptNetwork.sendToServer(new SaveStateMachinePacket(editorMachineId, name, entry, ""));
    }

    private void clearEditorWidgets() {
        for (EditBox w : editorWidgets) {
            parent.removeEditorWidget(w);
        }
        editorWidgets.clear();
        nameEditBox = null;
        entryNodeEditBox = null;
    }

    private void switchToMachine(String newId) {
        doSave();
        selectedId = newId;
        editorMachineId = null;
        loadingMachineId = newId;
        clearEditorWidgets();
        dirty = false;
        leftPanelMode = MODE_LOADING;
        IScriptNetwork.sendToServer(new RequestStateMachinePacket(newId));
    }

    public void onMachineReceived(String id, String name, String entryNode, String nodesJson) {
        if (!id.equals(selectedId)) return;
        loadingMachineId = null;
        editorMachineId = id;
        leftPanelMode = MODE_EDITOR;
        buildEditor(name, entryNode, nodesJson);
    }

    private void buildEditor(String name, String entryNode, String nodesJson) {
        clearEditorWidgets();

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int leftW = rightX - x - 16;
        int leftX = x + 8;
        int leftY = y + 8;

        nameEditBox = new EditBox(this.minecraft.font, leftX, leftY, leftW, 20, Component.literal("Name"));
        nameEditBox.setMaxLength(64);
        nameEditBox.setValue(name != null ? name : "");
        parent.addWidget(nameEditBox);
        editorWidgets.add(nameEditBox);

        entryNodeEditBox = new EditBox(this.minecraft.font, leftX, leftY + 28, leftW, 20, Component.literal("Entry Node"));
        entryNodeEditBox.setMaxLength(64);
        entryNodeEditBox.setValue(entryNode != null ? entryNode : "");
        parent.addWidget(entryNodeEditBox);
        editorWidgets.add(entryNodeEditBox);
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal("Machine Name"));
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
            IScriptNetwork.sendToServer(new SaveStateMachinePacket(id, name, "", ""));
            IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
            switchToMachine(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            IScriptNetwork.sendToServer(new RenameStateMachinePacket(renameOldId, id, name));
            IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
        }
    }

    private void openContextMenu(String itemId, int x, int y) {
        showContextMenu = true;
        contextMenuItemId = itemId;
        contextMenuX = x;
        contextMenuY = y;
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
            if (selectedId != null && selectedId.equals(confirmDialogId)) {
                selectedId = null;
                editorMachineId = null;
                loadingMachineId = null;
                clearEditorWidgets();
                leftPanelMode = MODE_EMPTY;
            }
            IScriptNetwork.sendToServer(new DeleteStateMachinePacket(confirmDialogId));
            IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
        }
        closeConfirmDialog();
    }

    private void copyItem(String id) {
        DashboardScreen.clipboard = id;
    }

    private void pasteItem() {
        String sourceId = DashboardScreen.clipboard;
        if (sourceId == null || sourceId.isEmpty()) return;
        var cache = StateMachineManager.getClientCache();
        if (!cache.containsKey(sourceId)) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (cache.containsKey(newId)) {
            newId = baseId + "_" + counter;
            counter++;
        }

        IScriptNetwork.sendToServer(new DuplicateStateMachinePacket(sourceId, newId));
        IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
        switchToMachine(newId);
    }

    private void duplicateItem(String id) {
        var cache = StateMachineManager.getClientCache();
        if (!cache.containsKey(id)) return;

        String newId = id + "_1";
        int counter = 1;
        while (cache.containsKey(newId)) {
            counter++;
            newId = id + "_" + counter;
        }

        IScriptNetwork.sendToServer(new DuplicateStateMachinePacket(id, newId));
        IScriptNetwork.sendToServer(new RequestStateMachinesPacket());
        switchToMachine(newId);
    }

    @Override
    public void removed() {
        if (dirty && editorMachineId != null) {
            String name = nameEditBox != null ? nameEditBox.getValue() : "";
            String entry = entryNodeEditBox != null ? entryNodeEditBox.getValue() : "";
            IScriptNetwork.sendToServer(new SaveStateMachinePacket(editorMachineId, name, entry, ""));
            dirty = false;
        }
        closeNameDialog();
        closeConfirmDialog();
        clearEditorWidgets();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        if (!showNameDialog && !showConfirmDialog) {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);

            int rightX = x + w - RIGHT_PANEL_WIDTH;

            graphics.fill(rightX, y, x + w, y + h, 0xFF1E1E28);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);

            graphics.drawString(this.font, "Machines", rightX + 8, y + 6, 0xFF55AAFF);

            var cache = StateMachineManager.getClientCache();
            List<String> ids = new ArrayList<>(cache.keySet());

            int listH = h - 40;
            int listY = y + 20;
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(selectedId);

                int bg = selected ? 0xFF334466 : (hovered ? 0xFF2A2A3A : 0x001E1E28);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                String display = cache.getOrDefault(id, id);
                graphics.drawString(this.font, display, rightX + 8, rowY + 4, selected ? 0xFF55AAFF : 0xFFCCCCCC);
            }

            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? 0xFF2A3A2A : 0xFF1E281E);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, 0xFF444455);
            graphics.drawCenteredString(this.font, "+ New Machine", rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, 0xFF55FF55);

            int leftW = rightX - x - 16;
            int leftH = h - 16;
            int leftX = x + 8;
            int leftY = y + 8;

            if (selectedId != null && cache.containsKey(selectedId)) {
                if (leftPanelMode == MODE_LOADING) {
                    if (loadingMachineId != null) {
                        var data = StateMachineManager.getClientMachineData(loadingMachineId);
                        if (data != null) {
                            loadingMachineId = null;
                            editorMachineId = data.id;
                            leftPanelMode = MODE_EDITOR;
                            buildEditor(data.name, data.entryNode, "");
                        } else {
                            graphics.drawCenteredString(this.font, "Loading...", leftX + leftW / 2, y + h / 2, 0xFF555566);
                        }
                    } else {
                        leftPanelMode = MODE_EMPTY;
                    }
                } else if (leftPanelMode == MODE_EDITOR) {
                    graphics.drawString(this.font, "Name:", leftX, leftY - 2, 0xFF888899);
                    graphics.drawString(this.font, "Entry Node:", leftX, leftY + 26, 0xFF888899);

                    if (nameEditBox != null) {
                        nameEditBox.setX(leftX);
                        nameEditBox.setY(leftY + 10);
                        nameEditBox.setWidth(leftW);
                    }
                    if (entryNodeEditBox != null) {
                        entryNodeEditBox.setX(leftX);
                        entryNodeEditBox.setY(leftY + 38);
                        entryNodeEditBox.setWidth(leftW);
                    }

                    int contentY = leftY + 70;
                    int contentH = leftH - contentY + leftY;
                    graphics.fill(leftX, contentY, leftX + leftW, contentY + contentH, 0xFF1A1A24);
                    graphics.renderOutline(leftX, contentY, leftW, contentH, 0xFF2A2A3A);

                    if (dirty) {
                        graphics.drawString(this.font, "*", leftX + leftW - 10, leftY - 2, 0xFFFF5555);
                    }
                }
            } else {
                clearEditorWidgets();
                leftPanelMode = MODE_EMPTY;
                graphics.drawCenteredString(this.font, "Select a machine from the list", leftX + leftW / 2, y + h / 2, 0xFF555566);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);
        }

        if (showNameDialog) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 80;
            int dx = cx - dw / 2;
            int dy = nameDialogY;

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
            graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
            graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename Machine" : "New Machine Name", cx, dy + 6, 0xFF55AAFF);

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

        if (showConfirmDialog) {
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

        if (showContextMenu && contextMenuItemId != null) {
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNameDialog) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
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
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
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

        var cache = StateMachineManager.getClientCache();
        List<String> ids = new ArrayList<>(cache.keySet());
        int listH = h - 40;
        int listY = y + 20;

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String newId = ids.get(i);
                if (!newId.equals(selectedId)) {
                    switchToMachine(newId);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            openNameDialog("create", null);
            return true;
        }

        if (leftPanelMode == MODE_EDITOR) {
            int leftX = x + 8;
            int leftY = y + 8;
            int leftW = rightX - x - 16;

            if (nameEditBox != null && mouseX >= nameEditBox.getX() && mouseX <= nameEditBox.getX() + nameEditBox.getWidth() && mouseY >= nameEditBox.getY() && mouseY <= nameEditBox.getY() + nameEditBox.getHeight()) {
                parent.setFocusedWidget(nameEditBox);
                return nameEditBox.mouseClicked(mouseX, mouseY, button);
            }
            if (entryNodeEditBox != null && mouseX >= entryNodeEditBox.getX() && mouseX <= entryNodeEditBox.getX() + entryNodeEditBox.getWidth() && mouseY >= entryNodeEditBox.getY() && mouseY <= entryNodeEditBox.getY() + entryNodeEditBox.getHeight()) {
                parent.setFocusedWidget(entryNodeEditBox);
                return entryNodeEditBox.mouseClicked(mouseX, mouseY, button);
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (showNameDialog || showConfirmDialog) return true;

        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
            int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
            int rightX = x + w - RIGHT_PANEL_WIDTH;

            var cache = StateMachineManager.getClientCache();
            List<String> ids = new ArrayList<>(cache.keySet());
            int listH = h - 40;
            int listY = y + 20;

            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
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
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showNameDialog) {
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
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showNameDialog || showConfirmDialog || showContextMenu) return true;

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            var cache = StateMachineManager.getClientCache();
            int visible = Math.max(1, (h - 40 - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, cache.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return showNameDialog || showConfirmDialog || showContextMenu;
    }
}