package com.iscript.iscript.gui.screen;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;

    private MultiLineEditBox editBox = null;
    private int saveDebounce = 0;
    private String saveStatus = "";
    private int saveStatusTimer = 0;

    private EditBox searchBox = null;

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

    public ScriptListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        IScriptMod.LOGGER.info("[IScript] ScriptListSubScreen.init()");
        showNameDialog = false;
        nameInputBox = null;
        showContextMenu = false;
        showConfirmDialog = false;
        if (editBox != null) {
            parent.removeEditorWidget(editBox);
            editBox = null;
        }
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
    }

    private void createSearchBox() {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        searchBox = new EditBox(this.minecraft.font, rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, Component.literal(I18n.s("iscript.script.list.search")));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(Theme.TEXT);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);
    }

    private void createEditBox(String text) {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        IScriptMod.LOGGER.info("[IScript] createEditBox: textLen={}", text != null ? text.length() : -1);
        editBox = new MultiLineEditBox(this.minecraft.font, leftX, leftY, leftW, leftH, I18n.t("iscript.script.editor.title"), Component.empty());
        editBox.setValue(text != null ? text : "");
        editBox.setOnValueChanged(() -> {
            saveDebounce = 40;
            saveStatus = "";
        });
        parent.addWidget(editBox);
    }

    private void sendSave() {
        String selId = parent.editorState.selectedId;
        if (selId == null || editBox == null) return;
        String text = editBox.getValue();
        if (text == null) text = "";
        if (text.equals(parent.editorState.lastSentText)) return;
        parent.editorState.lastSentText = text;
        saveStatus = I18n.s("iscript.script.status.saving");
        saveStatusTimer = 60;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_SCRIPT_TEXT, ServerCommandPacket.saveScriptTextToTag(selId, text)));
    }

    public void switchToScript(String newId) {
        switchToScript(newId, true);
    }

    public void switchToScript(String newId, boolean clearError) {
        IScriptMod.LOGGER.info("[IScript] switchToScript: newId={} clearError={}", newId, clearError);
        if (parent.editorState.selectedId != null && editBox != null) {
            sendSave();
        }
        parent.editorState.selectedId = newId;
        if (clearError) parent.editorState.errorLine = -1;
        parent.editorState.pendingContent = null;
        parent.editorState.isLoading = true;
        parent.editorState.lastSentText = "";
        saveStatus = "";
        if (editBox != null) {
            parent.removeEditorWidget(editBox);
            editBox = null;
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(newId)));
    }

    public void setErrorLine(int line) {
        IScriptMod.LOGGER.info("[IScript] setErrorLine: {}", line);
        parent.editorState.errorLine = line;
    }

    public void clearErrorLine() {
        parent.editorState.errorLine = -1;
    }

    private void runScript() {
        String selId = parent.editorState.selectedId;
        if (selId == null || editBox == null) return;
        if (saveDebounce > 0) {
            saveDebounce = 0;
            sendSave();
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.RUN_SCRIPT, ServerCommandPacket.runScriptToTag(selId)));
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        if (editBox != null) {
            editBox.setFocused(false);
            editBox.setVisible(false);
        }
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;
        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal(I18n.s("iscript.script.editor.placeholder.name")));
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
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(id)));
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
            switchToScript(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(renameOldId)));
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
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
            if (parent.editorState.selectedId != null && parent.editorState.selectedId.equals(confirmDialogId)) {
                parent.editorState.selectedId = null;
                parent.editorState.pendingContent = null;
                parent.editorState.isLoading = false;
                parent.editorState.errorLine = -1;
                if (editBox != null) {
                    parent.removeEditorWidget(editBox);
                    editBox = null;
                }
            }
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
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
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(sourceId)));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
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
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(id)));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
        switchToScript(newId);
    }

    private List<String> filteredIds() {
        var graphs = ScriptGraphManager.getClientCache();
        String filter = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<String> result = new ArrayList<>();
        for (String id : graphs.keySet()) {
            if (filter.isEmpty() || id.toLowerCase().contains(filter)) {
                result.add(id);
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void tick() {
        if (saveDebounce > 0) {
            saveDebounce--;
            if (saveDebounce == 0) sendSave();
        }
        if (saveStatusTimer > 0) {
            saveStatusTimer--;
            if (saveStatusTimer == 0 && saveStatus.equals(I18n.s("iscript.script.status.saving"))) {
                saveStatus = I18n.s("iscript.script.status.saved");
                saveStatusTimer = 40;
            } else if (saveStatusTimer == 0) {
                saveStatus = "";
            }
        }
        if (searchBox == null && this.minecraft != null) {
            createSearchBox();
        }
        super.tick();
    }

    @Override
    public void removed() {
        if (saveDebounce > 0) sendSave();
        closeNameDialog();
        closeConfirmDialog();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        String selId = parent.editorState.selectedId;
        boolean hasPending = parent.editorState.pendingContent != null;
        boolean loading = parent.editorState.isLoading;
        int errLine = parent.editorState.errorLine;
        IScriptMod.LOGGER.info("[IScript] render: selectedId={} pending={} loading={} errorLine={} editBox={}", selId, hasPending, loading, errLine, editBox != null);

        if (!showNameDialog && !showConfirmDialog) {
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            int toolbarX = rightX - TOOLBAR_WIDTH;
            graphics.fill(toolbarX, y, rightX, y + h, Theme.BG_PANEL);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, Theme.BG_HOVER);
            int btnSize = 24;
            int btnY = y + 8;
            boolean runHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, runHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, runHovered ? Theme.ACCENT : 0xFF44AA44);
            graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
            graphics.drawString(this.font, I18n.s("iscript.script.list.title"), rightX + 8, y + 26, Theme.ACCENT);
            if (searchBox != null) {
                searchBox.setX(rightX + 4);
                searchBox.setY(y + 4);
                searchBox.setWidth(RIGHT_PANEL_WIDTH - 8);
                searchBox.setHeight(16);
                searchBox.setVisible(true);
            }
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(selId);
                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
            }
            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);
            int leftW = toolbarX - x - 8;
            int leftH = h - 8;
            int leftX = x + 4;
            int leftY = y + 4;

            if (selId != null) {
                if (editBox == null) {
                    if (parent.editorState.pendingContent != null) {
                        IScriptMod.LOGGER.info("[IScript] render: creating editBox from pendingContent");
                        createEditBox(parent.editorState.pendingContent);
                        parent.editorState.lastSentText = parent.editorState.pendingContent;
                        parent.editorState.pendingContent = null;
                        parent.editorState.isLoading = false;
                    } else if (!loading && ScriptGraphManager.hasClientJsCache(selId)) {
                        IScriptMod.LOGGER.info("[IScript] render: creating editBox from cache");
                        String text = ScriptGraphManager.getClientJsCache(selId);
                        createEditBox(text);
                        parent.editorState.lastSentText = text;
                    } else if (loading) {
                        IScriptMod.LOGGER.info("[IScript] render: showing loading");
                        graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
                    } else {
                        IScriptMod.LOGGER.info("[IScript] render: requesting content");
                        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(selId)));
                        parent.editorState.isLoading = true;
                        graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
                    }
                }
                if (editBox != null) {
                    editBox.setX(leftX);
                    editBox.setY(leftY);
                    editBox.setWidth(leftW);
                    editBox.setHeight(leftH);
                    editBox.setVisible(true);
                    if (!(searchBox != null && searchBox.isFocused())) {
                        editBox.setFocused(true);
                        parent.setFocusedWidget(editBox);
                    }
                    graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                    graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);
                    if (errLine >= 0) {
                        IScriptMod.LOGGER.info("[IScript] render: highlighting error line {}", errLine);
                        int lineH = 12;
                        int errY = leftY + errLine * lineH;
                        if (errY >= leftY && errY < leftY + leftH) {
                            graphics.fill(leftX, errY, leftX + leftW, errY + lineH, 0x33FF4444);
                            graphics.renderOutline(leftX, errY, leftW, lineH, 0x88FF4444);
                        }
                    }
                }
                if (!saveStatus.isEmpty()) {
                    int statusColor = saveStatus.contains(I18n.s("iscript.script.status.saved")) ? 0xFF44AA44 : Theme.ACCENT;
                    graphics.drawString(this.font, saveStatus, leftX, leftY + leftH - 10, statusColor);
                }
            } else {
                if (editBox != null) {
                    parent.removeEditorWidget(editBox);
                    editBox = null;
                }
                graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        }

        if (showNameDialog) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 80;
            int dx = cx - dw / 2;
            int dy = nameDialogY;
            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? I18n.s("iscript.script.dialog.rename") : I18n.s("iscript.script.dialog.new_name"), cx, dy + 6, Theme.ACCENT);
            if (nameInputBox != null) {
                nameInputBox.setX(cx - 100);
                nameInputBox.setY(dy + 24);
            }
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
            graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? I18n.s("iscript.script.button.rename") : I18n.s("iscript.script.button.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : 0xFF44AA44);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
            graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : 0xFFAA4444);
        }

        if (showConfirmDialog) {
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
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
        }

        if (showContextMenu && contextMenuItemId != null) {
            String[] actions = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int hctx = (canPaste ? 5 : 4) * 22 + 4;
            graphics.fill(contextMenuX, contextMenuY, contextMenuX + wctx, contextMenuY + hctx, Theme.BG_INNER);
            graphics.renderOutline(contextMenuX, contextMenuY, wctx, hctx, Theme.BORDER);
            int cy = contextMenuY + 2;
            for (String action : actions) {
                if (action.equals("Paste") && !canPaste) continue;
                boolean hovered = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
                int bg = hovered ? Theme.BG_HOVER : Theme.BG_INNER;
                int tc = hovered ? Theme.TEXT : Theme.TEXT;
                if (action.equals("Delete")) tc = hovered ? Theme.ERROR : Theme.ERROR;
                graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, bg);
                String label = switch (action) {
                    case "Copy" -> I18n.s("iscript.script.context.copy");
                    case "Paste" -> I18n.s("iscript.script.context.paste");
                    case "Rename" -> I18n.s("iscript.script.context.rename");
                    case "Duplicate" -> I18n.s("iscript.script.context.duplicate");
                    case "Delete" -> I18n.s("iscript.script.context.delete");
                    default -> action;
                };
                graphics.drawString(font, label, contextMenuX + 6, cy + 6, tc);
                cy += 22;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNameDialog) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
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
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
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
        if (showContextMenu) {
            String[] actions = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int cy = contextMenuY + 2;
            for (String action : actions) {
                if (action.equals("Paste") && !canPaste) continue;
                if (mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20) {
                    switch (action) {
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
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int btnSize = 24;
        int btnY = y + 8;
        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            runScript();
            return true;
        }
        List<String> ids = filteredIds();
        int listH = h - 68;
        int listY = y + 42;
        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String newId = ids.get(i);
                if (!newId.equals(parent.editorState.selectedId)) {
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
        if (searchBox != null && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth() && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
            searchBox.setFocused(true);
            parent.setFocusedWidget(searchBox);
            return searchBox.mouseClicked(mouseX, mouseY, button);
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
        if (editBox != null && editBox.visible && !showNameDialog) {
            editBox.mouseReleased(mouseX, mouseY, button);
        }
        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
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
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        if (editBox != null && editBox.isFocused()) {
            return editBox.charTyped(codePoint, modifiers);
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
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (editBox != null && editBox.isFocused()) {
            return editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showNameDialog || showConfirmDialog || showContextMenu) return true;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftX = x + 4;
        int leftY = y + 4;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        if (editBox != null && editBox.visible && !showNameDialog && mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH) {
            return editBox.mouseScrolled(mouseX, mouseY, delta);
        }
        if (mouseX >= rightX && mouseX <= x + w) {
            List<String> ids = filteredIds();
            int listH = h - 68;
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, ids.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (showNameDialog || showConfirmDialog || showContextMenu) return true;
        if (editBox != null && editBox.visible && !showNameDialog) {
            return editBox.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }
}