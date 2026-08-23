package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.widget.MultiLineEditBox;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import com.iscript.imson.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class Script extends ListSubScreen {
    private static final int TOOLBAR_WIDTH = 32;

    public Script(DashboardScreen parent) {
        super(parent);
        this.toolbarWidth = TOOLBAR_WIDTH;
    }

    @Override
    public void init() {
        if (getSelectedId() == null) {
            DashboardScreen.EDITOR_STATE.selectedId = null;
            DashboardScreen.EDITOR_STATE.pendingContent = null;
            DashboardScreen.EDITOR_STATE.isLoading = false;
            DashboardScreen.EDITOR_STATE.errorLine = -1;
            DashboardScreen.EDITOR_STATE.lastSentText = "";
        }
        super.init();
        lifecycle.state().saveDebounce = 0;
        lifecycle.state().saveStatus = "";
        lifecycle.state().saveStatusTimer = 0;
        lifecycle.state().pendingSwitchId = null;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
    }

    @Override
    protected int getRightPanelWidth() { return 140; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.script.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.script.editor.empty"); }

    @Override
    protected String getNewButtonText() { return I18n.s("iscript.script.list.new"); }

    @Override
    protected net.minecraft.network.chat.Component getSearchLabel() {
        return net.minecraft.network.chat.Component.literal(I18n.s("iscript.script.list.search"));
    }

    @Override
    protected List<String> getItemIds() {
        return new ArrayList<>(ScriptGraphManager.getClientCache().keySet());
    }

    @Override
    protected String getItemDisplayName(String id) { return id; }

    @Override
    protected void onSelect(String id) { switchToScript(id); }

    @Override
    protected void onNew(String id) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(id)));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
        switchToScript(id);
    }

    @Override
    protected void onDelete(String id) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_SCRIPT_GRAPH, ServerCommandPacket.deleteScriptGraphToTag(id)));
        if (DashboardScreen.EDITOR_STATE.selectedId != null && DashboardScreen.EDITOR_STATE.selectedId.equals(id)) {
            DashboardScreen.EDITOR_STATE.selectedId = null;
            setSelectedId(null);
            DashboardScreen.EDITOR_STATE.pendingContent = null;
            DashboardScreen.EDITOR_STATE.isLoading = false;
            DashboardScreen.EDITOR_STATE.errorLine = -1;
            DashboardScreen.EDITOR_STATE.lastSentText = "";
            lifecycle.editors().remove("script");
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
    }

    @Override
    protected void onRename(String oldId, String newId) {
        if (oldId.equals(newId)) return;
        String jsText = ScriptGraphManager.getClientJsCache(oldId);
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if ((jsText == null || jsText.isEmpty()) && oldId.equals(DashboardScreen.EDITOR_STATE.selectedId) && scriptBox != null) {
            jsText = scriptBox.getValue();
        }
        if (jsText == null) jsText = "";
        var graphs = ScriptGraphManager.getClientCache();
        if (graphs.containsKey(oldId)) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_SCRIPT_TEXT, ServerCommandPacket.saveScriptTextToTag(newId, jsText)));
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_SCRIPT_GRAPH, ServerCommandPacket.deleteScriptGraphToTag(oldId)));
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
            lifecycle.state().pendingSwitchId = newId;
        }
    }

    @Override
    protected void onDuplicate(String id) {
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

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
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

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
    }

    @Override
    protected void doSave() {
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

    @Override
    public void tick() {
        super.tick();
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null && !parent.children().contains(box)) {
            lifecycle.editors().remove("script");
        }
        if (lifecycle.state().pendingSwitchId != null) {
            if (ScriptGraphManager.getClientCache().containsKey(lifecycle.state().pendingSwitchId)) {
                switchToScript(lifecycle.state().pendingSwitchId);
            }
            lifecycle.state().pendingSwitchId = null;
        }
    }

    private void switchToScript(String newId) { switchToScript(newId, true); }

    private void switchToScript(String newId, boolean clearError) {
        setSelectedId(newId);
        if (DashboardScreen.EDITOR_STATE.selectedId != null) doSave();
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

    public void setErrorLine(int line) { DashboardScreen.EDITOR_STATE.errorLine = line; }
    public void clearErrorLine() { DashboardScreen.EDITOR_STATE.errorLine = -1; }

    private void runScript() {
        String selId = DashboardScreen.EDITOR_STATE.selectedId;
        if (selId == null) return;
        if (lifecycle.save().isDirty()) {
            lifecycle.state().saveDebounce = 0;
            doSave();
            lifecycle.save().clearDirty();
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.RUN_SCRIPT, ServerCommandPacket.runScriptToTag(selId)));
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (w <= 0) return;
        g.fill(x, y, x + w, y + h, Theme.BG_PANEL);
        g.renderOutline(x, y, w, h, Theme.BG_HOVER);
        int btnSize = 24;
        int btnY = y + 8;
        boolean runHovered = mx >= x + 4 && mx <= x + w - 4 && my >= btnY && my <= btnY + btnSize;
        g.fill(x + 4, btnY, x + w - 4, btnY + btnSize, runHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(x + 4, btnY, w - 8, btnSize, Theme.BORDER);
        g.drawCenteredString(this.font, "\u25B6", x + w / 2, btnY + (btnSize - 8) / 2, runHovered ? Theme.ACCENT : 0xFF44AA44);
        btnY += btnSize + 6;
        boolean addHovered = mx >= x + 4 && mx <= x + w - 4 && my >= btnY && my <= btnY + btnSize;
        g.fill(x + 4, btnY, x + w - 4, btnY + btnSize, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(x + 4, btnY, w - 8, btnSize, Theme.BORDER);
        g.drawCenteredString(this.font, "+", x + w / 2, btnY + (btnSize - 8) / 2, addHovered ? Theme.TEXT : Theme.TEXT_DIM);
    }

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        int toolbarX = rightX - getToolbarWidth();
        int btnSize = 24;
        int btnY = y + 8;
        if (mx >= toolbarX + 4 && mx <= toolbarX + getToolbarWidth() - 4 && my >= btnY && my <= btnY + btnSize) {
            runScript();
            return true;
        }
        btnY += btnSize + 6;
        if (mx >= toolbarX + 4 && mx <= toolbarX + getToolbarWidth() - 4 && my >= btnY && my <= btnY + btnSize) {
            openPromptDialog("create", null);
            return true;
        }
        return false;
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        String selId = DashboardScreen.EDITOR_STATE.selectedId;
        boolean loading = DashboardScreen.EDITOR_STATE.isLoading;
        int errLine = DashboardScreen.EDITOR_STATE.errorLine;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox == null) {
            if (DashboardScreen.EDITOR_STATE.pendingContent != null) {
                if (this.minecraft != null) {
                    scriptBox = lifecycle.editors().addMultiBox("script", x, y, w, h, net.minecraft.network.chat.Component.literal(I18n.s("iscript.script.editor.title")), DashboardScreen.EDITOR_STATE.pendingContent);
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
                    scriptBox = lifecycle.editors().addMultiBox("script", x, y, w, h, net.minecraft.network.chat.Component.literal(I18n.s("iscript.script.editor.title")), text);
                    if (scriptBox != null) {
                        scriptBox.setOnValueChanged(() -> {
                            lifecycle.save().debounce(40);
                            lifecycle.save().status("", 0);
                        });
                    }
                }
                DashboardScreen.EDITOR_STATE.lastSentText = text;
            } else if (loading) {
                g.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), x + w / 2, y + h / 2, Theme.TEXT_MUTE);
                return;
            } else {
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(selId)));
                DashboardScreen.EDITOR_STATE.isLoading = true;
                g.drawCenteredString(this.font, I18n.s("iscript.script.editor.loading"), x + w / 2, y + h / 2, Theme.TEXT_MUTE);
                return;
            }
        }
        scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null) {
            scriptBox.setX(x);
            scriptBox.setY(y);
            scriptBox.setWidth(w);
            scriptBox.setHeight(h);
            scriptBox.setVisible(true);
            if (!(lifecycle.search().box() != null && lifecycle.search().box().isFocused())) {
                scriptBox.setFocused(true);
                parent.setFocusedWidget(scriptBox);
            }
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, Theme.BG_INNER);
            g.renderOutline(x - 2, y - 2, w + 4, h + 4, Theme.BORDER);
            if (errLine >= 0) {
                int lineH = 12;
                int errY = y + errLine * lineH;
                if (errY >= y && errY < y + h) {
                    g.fill(x, errY, x + w, errY + lineH, 0x33FF4444);
                    g.renderOutline(x, errY, w, lineH, 0x88FF4444);
                }
            }
        }
        String saveStatus = lifecycle.save().status();
        if (!saveStatus.isEmpty()) {
            int statusColor = saveStatus.contains(I18n.s("iscript.script.status.saved")) ? 0xFF44AA44 : Theme.ACCENT;
            g.drawString(this.font, saveStatus, x, y + h - 10, statusColor);
        }
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("prompt") && mx >= scriptBox.getX() && mx <= scriptBox.getX() + scriptBox.getWidth() && my >= scriptBox.getY() && my <= scriptBox.getY() + scriptBox.getHeight()) {
            scriptBox.setFocused(true);
            parent.setFocusedWidget(scriptBox);
            return scriptBox.mouseClicked(mx, my, button);
        }
        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleEditorScroll(double mx, double my, double delta, int leftX, int leftY, int leftW, int leftH) {
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("prompt") && mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH) {
            return scriptBox.mouseScrolled(mx, my, delta);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (super.mouseReleased(mx, my, button)) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("prompt")) {
            scriptBox.mouseReleased(mx, my, button);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (super.charTyped(codePoint, modifiers)) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.isFocused()) {
            return scriptBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.isFocused()) {
            return scriptBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || contextMenu.isOpen()) return true;
        MultiLineEditBox scriptBox = lifecycle.editors().multi("script");
        if (scriptBox != null && scriptBox.visible && !lifecycle.modals().isOpen("prompt")) {
            return scriptBox.mouseDragged(mx, my, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public void removed() {
        MultiLineEditBox box = lifecycle.editors().multi("script");
        if (box != null) {
            lifecycle.state().lastEditText = box.getValue();
            lifecycle.state().savedScrollOffset = box.getScrollOffset();
            lifecycle.state().savedHorizontalScrollOffset = box.getHorizontalScrollOffset();
            lifecycle.state().savedCursorPos = box.getCursorPos();
            lifecycle.state().savedSelectStart = box.getSelectStart();
        }
        super.removed();
    }
}