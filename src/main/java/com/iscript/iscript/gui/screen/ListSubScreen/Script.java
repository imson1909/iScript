package com.iscript.iscript.gui.screen.ListSubScreen;

import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.I18n;
import com.iscript.iscript.gui.screen.SubScreenLifecycle;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.ContextMenu;
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
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.Graph;

public class Script extends DashboardScreen.SubScreen {
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;

    private ContextMenu contextMenu = new ContextMenu();

    private String nameDialogMode = "";
    private String renameOldId = null;

    private String confirmDialogAction = "";
    private String confirmDialogId = null;

    public Script(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        if (lifecycle.selection().get() == null) {
            DashboardScreen.EDITOR_STATE.selectedId = null;
            DashboardScreen.EDITOR_STATE.pendingContent = null;
            DashboardScreen.EDITOR_STATE.isLoading = false;
            DashboardScreen.EDITOR_STATE.errorLine = -1;
            DashboardScreen.EDITOR_STATE.lastSentText = "";
        }
        lifecycle.init();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        lifecycle.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, Component.literal(I18n.s("iscript.script.list.search")));

        lifecycle.modals().register("name",
                () -> lifecycle.state().modalOpenFlags.getOrDefault("name", false),
                v -> lifecycle.state().modalOpenFlags.put("name", v),
                () -> {
                    if (lifecycle.editors().box("nameInput") != null) return;
                    int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
                    int ny = this.parent.height / 2 - 40;
                    EditBox box = lifecycle.editors().addBox("nameInput", cx - 100, ny + 20, 200, 20, Component.literal(I18n.s("iscript.script.editor.placeholder.name")));
                    if (box != null) {
                        box.setMaxLength(64);
                        if ("rename".equals(nameDialogMode) && renameOldId != null) {
                            box.setValue(renameOldId);
                        } else {
                            box.setValue("");
                        }
                        parent.setFocusedWidget(box);
                    }
                },
                () -> {
                    lifecycle.editors().remove("nameInput");
                },
                "nameInput"
        );

        lifecycle.modals().register("confirm",
                () -> lifecycle.state().modalOpenFlags.getOrDefault("confirm", false),
                v -> lifecycle.state().modalOpenFlags.put("confirm", v),
                () -> {},
                () -> {},
                "confirm"
        );

        lifecycle.state().modalOpenFlags.put("name", false);
        lifecycle.state().modalOpenFlags.put("confirm", false);
        lifecycle.state().saveDebounce = 0;
        lifecycle.state().saveStatus = "";
        lifecycle.state().saveStatusTimer = 0;
        lifecycle.state().pendingSwitchId = null;

        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
    }

    private void sendSave() {
        String selId = DashboardScreen.EDITOR_STATE.selectedId;
        if (selId == null) return;
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box == null) return;
        String text = box.getValue();
        if (text == null) text = "";
        if (text.equals(DashboardScreen.EDITOR_STATE.lastSentText)) return;
        DashboardScreen.EDITOR_STATE.lastSentText = text;
        lifecycle.save().status(I18n.s("iscript.script.status.saving"), 60);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_SCRIPT_TEXT, ServerCommandPacket.saveScriptTextToTag(selId, text)));
    }

    public void switchToScript(String newId) {
        switchToScript(newId, true);
    }

    public void switchToScript(String newId, boolean clearError) {
        lifecycle.selection().set(newId);
        if (DashboardScreen.EDITOR_STATE.selectedId != null) {
            sendSave();
        }
        DashboardScreen.EDITOR_STATE.selectedId = newId;
        if (clearError) DashboardScreen.EDITOR_STATE.errorLine = -1;
        DashboardScreen.EDITOR_STATE.pendingContent = null;
        DashboardScreen.EDITOR_STATE.isLoading = true;
        DashboardScreen.EDITOR_STATE.lastSentText = "";
        lifecycle.save().status("", 0);
        lifecycle.state().lastEditText = null;
        lifecycle.editors().remove("script");
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(newId)));
    }

    public void setErrorLine(int line) {
        DashboardScreen.EDITOR_STATE.errorLine = line;
    }

    public void clearErrorLine() {
        DashboardScreen.EDITOR_STATE.errorLine = -1;
    }

    private void runScript() {
        String selId = DashboardScreen.EDITOR_STATE.selectedId;
        if (selId == null) return;
        if (lifecycle.save().isDirty()) {
            lifecycle.state().saveDebounce = 0;
            sendSave();
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.RUN_SCRIPT, ServerCommandPacket.runScriptToTag(selId)));
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) {
            box.setFocused(false);
            box.setVisible(false);
        }
        lifecycle.search().setVisible(false);
        lifecycle.modals().open("name");
    }

    private void closeNameDialog() {
        lifecycle.modals().close("name");
        renameOldId = null;
        lifecycle.editors().remove("nameInput");
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) {
            box.setVisible(true);
            box.setFocused(true);
            parent.setFocusedWidget(box);
        }
        lifecycle.search().setVisible(true);
    }

    private void confirmNameDialog() {
        EditBox box = lifecycle.editors().box("nameInput");
        if (box == null) return;
        String id = box.getValue().trim();
        String mode = nameDialogMode;
        String oldIdLocal = renameOldId;
        closeNameDialog();
        if (id.isEmpty()) {
            return;
        }
        if ("create".equals(mode)) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(id)));
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
            switchToScript(id);
        } else if ("rename".equals(mode) && oldIdLocal != null) {
            String oldId = oldIdLocal;
            if (oldId.equals(id)) {
                return;
            }
            String jsText = ScriptGraphManager.getClientJsCache(oldId);
            MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
            if ((jsText == null || jsText.isEmpty()) && oldId.equals(DashboardScreen.EDITOR_STATE.selectedId) && scriptBox != null) {
                jsText = scriptBox.getValue();
            }
            if (jsText == null) jsText = "";
            var graphs = ScriptGraphManager.getClientCache();
            Graph oldGraph = graphs.get(oldId);
            if (oldGraph != null) {
                Graph newGraph = oldGraph.copy();
                newGraph.setId(id);
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_SCRIPT_TEXT, ServerCommandPacket.saveScriptTextToTag(id, jsText)));
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_SCRIPT_GRAPH, ServerCommandPacket.deleteScriptGraphToTag(oldId)));
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
                lifecycle.state().pendingSwitchId = id;
            }
        }
    }

    private void openConfirmDialog(String action, String id) {
        confirmDialogAction = action;
        confirmDialogId = id;
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) box.setVisible(false);
        lifecycle.search().setVisible(false);
        lifecycle.modals().open("confirm");
    }

    private void closeConfirmDialog() {
        lifecycle.modals().close("confirm");
        confirmDialogAction = "";
        confirmDialogId = null;
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) box.setVisible(true);
        lifecycle.search().setVisible(true);
    }

    private void executeConfirm() {
        if ("delete".equals(confirmDialogAction) && confirmDialogId != null) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_SCRIPT_GRAPH, ServerCommandPacket.deleteScriptGraphToTag(confirmDialogId)));
            if (DashboardScreen.EDITOR_STATE.selectedId != null && DashboardScreen.EDITOR_STATE.selectedId.equals(confirmDialogId)) {
                DashboardScreen.EDITOR_STATE.selectedId = null;
                lifecycle.selection().set(null);
                DashboardScreen.EDITOR_STATE.pendingContent = null;
                DashboardScreen.EDITOR_STATE.isLoading = false;
                DashboardScreen.EDITOR_STATE.errorLine = -1;
                DashboardScreen.EDITOR_STATE.lastSentText = "";
                lifecycle.editors().remove("script");
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
        String filter = lifecycle.search().box() != null ? lifecycle.search().box().getValue().trim().toLowerCase() : lifecycle.state().lastSearch.trim().toLowerCase();
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
        lifecycle.tick(this::sendSave);
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null && !parent.children().contains(box)) {
            lifecycle.editors().remove("script");
        }
        if (lifecycle.search().box() != null && !parent.children().contains(lifecycle.search().box())) {
            lifecycle.state().lastSearch = lifecycle.search().box().getValue();
            lifecycle.search().captureAndRemove();
        }
        if (lifecycle.search().box() == null && this.minecraft != null) {
            lifecycle.search().recreateIfMissing();
        }
        if (lifecycle.state().pendingSwitchId != null) {
            if (ScriptGraphManager.getClientCache().containsKey(lifecycle.state().pendingSwitchId)) {
                switchToScript(lifecycle.state().pendingSwitchId);
            }
            lifecycle.state().pendingSwitchId = null;
        }
        super.tick();
    }

    @Override
    public void removed() {
        if (lifecycle.save().isDirty()) sendSave();
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) {
            lifecycle.state().lastEditText = box.getValue();
            lifecycle.state().savedScrollOffset = box.getScrollOffset();
            lifecycle.state().savedHorizontalScrollOffset = box.getHorizontalScrollOffset();
            lifecycle.state().savedCursorPos = box.getCursorPos();
            lifecycle.state().savedSelectStart = box.getSelectStart();
        }
        lifecycle.removed();
        closeNameDialog();
        closeConfirmDialog();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        String selId = DashboardScreen.EDITOR_STATE.selectedId;
        boolean loading = DashboardScreen.EDITOR_STATE.isLoading;
        int errLine = DashboardScreen.EDITOR_STATE.errorLine;
        boolean nameOpen = lifecycle.modals().isOpen("name");
        boolean confirmOpen = lifecycle.modals().isOpen("confirm");

        if (!nameOpen && !confirmOpen) {
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
            if (lifecycle.search().box() != null) {
                lifecycle.search().setPos(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16);
                lifecycle.search().setVisible(true);
            }
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            int scroll = lifecycle.selection().scroll();
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
                MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
                if (scriptBox == null) {
                    if (DashboardScreen.EDITOR_STATE.pendingContent != null) {
                        if (this.minecraft != null) {
                            scriptBox = lifecycle.editors().addMultiBox("script", leftX, leftY, leftW, leftH, I18n.t("iscript.script.editor.title"), DashboardScreen.EDITOR_STATE.pendingContent);
                            if (scriptBox != null) {
                                scriptBox.setOnValueChanged(() -> {
                                    lifecycle.save().debounce(40);
                                    lifecycle.save().status("", 0);
                                });
                            }
                        }
                        DashboardScreen.EDITOR_STATE.lastSentText = DashboardScreen.EDITOR_STATE.pendingContent;
                        DashboardScreen.EDITOR_STATE.pendingContent = null;
                        DashboardScreen.EDITOR_STATE.isLoading = false;
                    } else if (!loading && ScriptGraphManager.hasClientJsCache(selId)) {
                        String text = ScriptGraphManager.getClientJsCache(selId);
                        if (this.minecraft != null) {
                            scriptBox = lifecycle.editors().addMultiBox("script", leftX, leftY, leftW, leftH, I18n.t("iscript.script.editor.title"), text);
                            if (scriptBox != null) {
                                scriptBox.setOnValueChanged(() -> {
                                    lifecycle.save().debounce(40);
                                    lifecycle.save().status("", 0);
                                });
                            }
                        }
                        DashboardScreen.EDITOR_STATE.lastSentText = text;
                    } else if (loading) {
                        graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
                    } else {
                        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(selId)));
                        DashboardScreen.EDITOR_STATE.isLoading = true;
                        graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
                    }
                }
                scriptBox = lifecycle.editors().multi("script");
                if (scriptBox != null) {
                    scriptBox.setX(leftX);
                    scriptBox.setY(leftY);
                    scriptBox.setWidth(leftW);
                    scriptBox.setHeight(leftH);
                    scriptBox.setVisible(true);
                    if (!(lifecycle.search().box() != null && lifecycle.search().box().isFocused())) {
                        scriptBox.setFocused(true);
                        parent.setFocusedWidget(scriptBox);
                    }
                    graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                    graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);
                    if (errLine >= 0) {
                        int lineH = 12;
                        int errY = leftY + errLine * lineH;
                        if (errY >= leftY && errY < leftY + leftH) {
                            graphics.fill(leftX, errY, leftX + leftW, errY + lineH, 0x33FF4444);
                            graphics.renderOutline(leftX, errY, leftW, lineH, 0x88FF4444);
                        }
                    }
                }
                String saveStatus = lifecycle.save().status();
                if (!saveStatus.isEmpty()) {
                    int statusColor = saveStatus.contains(I18n.s("iscript.script.status.saved")) ? 0xFF44AA44 : Theme.ACCENT;
                    graphics.drawString(this.font, saveStatus, leftX, leftY + leftH - 10, statusColor);
                }
            } else {
                lifecycle.editors().remove("script");
                graphics.drawCenteredString(this.font, I18n.s("iscript.script.editor.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        }

        if (nameOpen) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 80;
            int dx = cx - dw / 2;
            int dy = this.parent.height / 2 - 40;
            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
            graphics.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
            graphics.drawCenteredString(this.font, "rename".equals(nameDialogMode) ? I18n.s("iscript.script.dialog.rename") : I18n.s("iscript.script.dialog.new_name"), cx, dy + 6, Theme.ACCENT);
            EditBox nameBox = lifecycle.editors().box("nameInput");
            if (nameBox != null) {
                nameBox.setX(cx - 100);
                nameBox.setY(dy + 24);
            }
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
            graphics.fill(cx - 50, dy + 52, cx - 2, dy + 74, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, "rename".equals(nameDialogMode) ? I18n.s("iscript.script.button.rename") : I18n.s("iscript.script.button.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : 0xFF44AA44);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
            graphics.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.script.button.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : 0xFFAA4444);
        }

        if (confirmOpen) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = this.parent.height / 2 - 30;
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

        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (lifecycle.modals().isOpen("name")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int cx = x + w / 2;
            int dy = this.parent.height / 2 - 40;
            if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                confirmNameDialog();
                return true;
            }
            if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74) {
                closeNameDialog();
                return true;
            }
            EditBox nameBox = lifecycle.editors().box("nameInput");
            if (nameBox != null && mouseX >= nameBox.getX() && mouseX <= nameBox.getX() + nameBox.getWidth() && mouseY >= nameBox.getY() && mouseY <= nameBox.getY() + nameBox.getHeight()) {
                parent.setFocusedWidget(nameBox);
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (lifecycle.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
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

        if (contextMenu.isOpen()) {
            String targetItemId = contextMenu.getItemId();
            boolean handled = contextMenu.mouseClicked(mouseX, mouseY, button);
            String action = contextMenu.getLastAction();

            if (action != null && targetItemId != null) {
                switch (action) {
                    case "Copy" -> copyItem(targetItemId);
                    case "Paste" -> pasteItem();
                    case "Rename" -> openNameDialog("rename", targetItemId);
                    case "Duplicate" -> duplicateItem(targetItemId);
                    case "Delete" -> openConfirmDialog("delete", targetItemId);
                }
            }
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
        int scroll = lifecycle.selection().scroll();
        for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String newId = ids.get(i);
                if (!newId.equals(DashboardScreen.EDITOR_STATE.selectedId)) {
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

        if (lifecycle.search().box() != null && mouseX >= lifecycle.search().box().getX() && mouseX <= lifecycle.search().box().getX() + lifecycle.search().box().getWidth() && mouseY >= lifecycle.search().box().getY() && mouseY <= lifecycle.search().box().getY() + lifecycle.search().box().getHeight()) {
            lifecycle.search().box().setFocused(true);
            parent.setFocusedWidget(lifecycle.search().box());
            return lifecycle.search().box().mouseClicked(mouseX, mouseY, button);
        }

        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("name") && mouseX >= scriptBox.getX() && mouseX <= scriptBox.getX() + scriptBox.getWidth() && mouseY >= scriptBox.getY() && mouseY <= scriptBox.getY() + scriptBox.getHeight()) {
            scriptBox.setFocused(true);
            parent.setFocusedWidget(scriptBox);
            return scriptBox.mouseClicked(mouseX, mouseY, button);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (lifecycle.modals().isOpen("name") || lifecycle.modals().isOpen("confirm")) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("name")) {
            scriptBox.mouseReleased(mouseX, mouseY, button);
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
            int scroll = lifecycle.selection().scroll();
            for (int i = scroll; i < Math.min(scroll + (listH - 24) / ITEM_HEIGHT, ids.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                    contextMenu.open((int) mouseX, (int) mouseY, ids.get(i), canPaste);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (lifecycle.modals().isOpen("name")) {
            EditBox nameBox = lifecycle.editors().box("nameInput");
            if (nameBox != null && nameBox.isFocused()) {
                return nameBox.charTyped(codePoint, modifiers);
            }
            return true;
        }
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().charTyped(codePoint, modifiers);
        }
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.isFocused()) {
            return scriptBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (lifecycle.modals().isOpen("name")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmNameDialog();
                return true;
            }
            EditBox nameBox = lifecycle.editors().box("nameInput");
            if (nameBox != null && nameBox.isFocused()) {
                return nameBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (lifecycle.modals().isOpen("confirm")) {
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
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().keyPressed(keyCode, scanCode, modifiers);
        }
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.isFocused()) {
            return scriptBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (lifecycle.modals().isOpen("name") || lifecycle.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
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
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("name") && mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH) {
            return scriptBox.mouseScrolled(mouseX, mouseY, delta);
        }
        if (mouseX >= rightX && mouseX <= x + w) {
            List<String> ids = filteredIds();
            int listH = h - 68;
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, ids.size() - visible);
            int scroll = lifecycle.selection().scroll();
            if (delta > 0) lifecycle.selection().scroll(Math.max(0, scroll - 1));
            else lifecycle.selection().scroll(Math.min(scroll + 1, maxScroll));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (lifecycle.modals().isOpen("name") || lifecycle.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("name")) {
            return scriptBox.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }
}