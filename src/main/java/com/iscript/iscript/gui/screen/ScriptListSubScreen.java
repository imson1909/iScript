package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.RequestScriptContentPacket;
import com.iscript.iscript.network.packet.RequestScriptGraphsPacket;
import com.iscript.iscript.network.packet.RunScriptPacket;
import com.iscript.iscript.network.packet.SaveScriptTextPacket;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class ScriptListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;
    private String selectedId = null;
    private String editorScriptId = null;
    private String loadingScriptId = null;

    private MultiLineEditBox editBox = null;
    private String pendingEditorText = "";
    private int saveDebounce = 0;
    private String lastSentText = "";

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

    public ScriptListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        showNameDialog = false;
        nameInputBox = null;
        showContextMenu = false;
        showConfirmDialog = false;
        if (editBox != null) {
            pendingEditorText = editBox.getValue();
            parent.removeEditorWidget(editBox);
            editBox = null;
        }
        IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
    }

    private void createEditBox(String text) {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        editBox = new MultiLineEditBox(this.minecraft.font, leftX, leftY, leftW, leftH, Component.literal("Script Editor"), Component.empty());
        editBox.setValue(text != null ? text : "");
        editBox.setOnValueChanged(() -> {
            saveDebounce = 10;
        });
        parent.addWidget(editBox);
    }

    private void sendSave() {
        if (editorScriptId == null || editBox == null) return;
        String text = editBox.getValue();
        if (text == null) text = "";
        if (text.equals(lastSentText)) return;
        lastSentText = text;
        IScriptNetwork.sendToServer(new SaveScriptTextPacket(editorScriptId, text));
    }

    private void switchToScript(String newId) {
        if (editorScriptId != null && editBox != null) {
            sendSave();
        }

        selectedId = newId;
        editorScriptId = null;
        loadingScriptId = newId;
        pendingEditorText = "";
        lastSentText = "";
        if (editBox != null) {
            parent.removeEditorWidget(editBox);
            editBox = null;
        }
        IScriptNetwork.sendToServer(new RequestScriptContentPacket(newId));
    }

    public void onContentReceived(String id, String text) {
        if (!id.equals(selectedId)) return;
        if (editBox != null && id.equals(editorScriptId)) {
            editBox.setValue(text);
            lastSentText = text;
        } else {
            pendingEditorText = text;
        }
    }

    private void runScript() {
        if (editorScriptId == null || editBox == null) return;
        if (saveDebounce > 0) {
            saveDebounce = 0;
            sendSave();
        }
        IScriptNetwork.sendToServer(new RunScriptPacket(editorScriptId));
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        if (editBox != null) {
            editBox.setFocused(false);
            editBox.setVisible(false);
        }
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal("Script Name"));
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
        if (editBox != null) {
            editBox.setVisible(true);
            editBox.setFocused(true);
            parent.setFocusedWidget(editBox);
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
            IScriptNetwork.sendToServer(new RequestScriptContentPacket(id));
            IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
            switchToScript(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            IScriptNetwork.sendToServer(new RequestScriptContentPacket(renameOldId));
            IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
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
                editorScriptId = null;
                loadingScriptId = null;
                if (editBox != null) {
                    parent.removeEditorWidget(editBox);
                    editBox = null;
                }
            }
            IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
        }
        closeConfirmDialog();
    }

    private void copyItem(String id) {
        DashboardScreen.clipboard = id;
    }

    private void pasteItem() {
        String sourceId = DashboardScreen.clipboard;
        if (sourceId == null || sourceId.isEmpty()) return;
        var graphs = ScriptGraphManager.getClientCache();
        if (!graphs.containsKey(sourceId)) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (graphs.containsKey(newId)) {
            newId = baseId + "_" + counter;
            counter++;
        }

        IScriptNetwork.sendToServer(new RequestScriptContentPacket(sourceId));
        IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
        switchToScript(newId);
    }

    private void duplicateItem(String id) {
        var graphs = ScriptGraphManager.getClientCache();
        if (!graphs.containsKey(id)) return;

        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (graphs.containsKey(newId)) {
            counter++;
            newId = baseId + "_" + counter;
        }

        IScriptNetwork.sendToServer(new RequestScriptContentPacket(id));
        IScriptNetwork.sendToServer(new RequestScriptGraphsPacket());
        switchToScript(newId);
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
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        if (!showNameDialog && !showConfirmDialog) {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);

            int rightX = x + w - RIGHT_PANEL_WIDTH;
            int toolbarX = rightX - TOOLBAR_WIDTH;

            graphics.fill(toolbarX, y, rightX, y + h, 0xFF1A1A24);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, 0xFF2A2A3A);

            int btnSize = 24;
            int btnY = y + 8;

            boolean runHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, runHovered ? 0xFF2A4A2A : 0xFF1E281E);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, runHovered ? 0xFF55FF55 : 0xFF44AA44);

            graphics.fill(rightX, y, x + w, y + h, 0xFF1E1E28);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);

            graphics.drawString(this.font, "Scripts", rightX + 8, y + 6, 0xFF55AAFF);

            var graphs = ScriptGraphManager.getClientCache();
            List<String> ids = new ArrayList<>(graphs.keySet());

            int listH = h - 40;
            int listY = y + 20;
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
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
            graphics.drawCenteredString(this.font, "+ New Script", rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, 0xFF55FF55);

            int leftW = toolbarX - x - 8;
            int leftH = h - 8;
            int leftX = x + 4;
            int leftY = y + 4;

            if (selectedId != null && graphs.containsKey(selectedId)) {
                if (editBox == null || !selectedId.equals(editorScriptId)) {
                    if (editBox != null) {
                        parent.removeEditorWidget(editBox);
                        editBox = null;
                    }

                    if (!pendingEditorText.isEmpty()) {
                        editorScriptId = selectedId;
                        loadingScriptId = null;
                        createEditBox(pendingEditorText);
                        lastSentText = pendingEditorText;
                        pendingEditorText = "";
                    } else if (ScriptGraphManager.hasClientJsCache(selectedId)) {
                        editorScriptId = selectedId;
                        loadingScriptId = null;
                        String text = ScriptGraphManager.getClientJsCache(selectedId);
                        createEditBox(text);
                        lastSentText = text;
                    } else if (loadingScriptId != null && loadingScriptId.equals(selectedId)) {
                        graphics.drawCenteredString(this.font, "Loading...", leftX + leftW / 2, y + h / 2, 0xFF555566);
                    } else {
                        IScriptNetwork.sendToServer(new RequestScriptContentPacket(selectedId));
                        loadingScriptId = selectedId;
                        graphics.drawCenteredString(this.font, "Loading...", leftX + leftW / 2, y + h / 2, 0xFF555566);
                    }
                }

                if (editBox != null) {
                    editBox.setX(leftX);
                    editBox.setY(leftY);
                    editBox.setWidth(leftW);
                    editBox.setHeight(leftH);
                    editBox.setVisible(true);
                    editBox.setFocused(true);
                    parent.setFocusedWidget(editBox);
                    graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, 0xFF16161E);
                    graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, 0xFF333344);
                }
            } else {
                if (editBox != null) {
                    parent.removeEditorWidget(editBox);
                    editBox = null;
                    editorScriptId = null;
                }
                graphics.drawCenteredString(this.font, "Select a script from the list", leftX + leftW / 2, y + h / 2, 0xFF555566);
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
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename Script" : "New Script Name", cx, dy + 6, 0xFF55AAFF);

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
            int idx = 0;
            for (String item : items) {
                if (item.equals("Paste") && !canPaste) continue;
                boolean hovered = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
                int bg = hovered ? 0xFF444444 : 0xFF222222;
                int tc = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
                if (item.equals("Delete")) tc = hovered ? 0xFFFF6666 : 0xFFCC4444;
                graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, bg);
                graphics.drawString(font, item, contextMenuX + 6, cy + 6, tc);
                cy += 22;
                idx++;
            }
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
            runScript();
            return true;
        }

        var graphs = ScriptGraphManager.getClientCache();
        List<String> ids = new ArrayList<>(graphs.keySet());
        int listH = h - 40;
        int listY = y + 20;

        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String newId = ids.get(i);
                if (!newId.equals(selectedId)) {
                    switchToScript(newId);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            openNameDialog("create", null);
            return true;
        }

        if (editBox != null && editBox.visible && !showNameDialog && mouseX >= editBox.getX() && mouseX <= editBox.getX() + editBox.getWidth() && mouseY >= editBox.getY() && mouseY <= editBox.getY() + editBox.getHeight()) {
            editBox.setFocused(true);
            parent.setFocusedWidget(editBox);
            return editBox.mouseClicked(mouseX, mouseY, button);
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

            var graphs = ScriptGraphManager.getClientCache();
            List<String> ids = new ArrayList<>(graphs.keySet());
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
            var graphs = ScriptGraphManager.getClientCache();
            int visible = Math.max(1, (h - 40 - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, graphs.size() - visible);
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