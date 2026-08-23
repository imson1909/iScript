package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.SubScreenLifecycle;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.widget.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ListSubScreen extends DashboardScreen.SubScreen {

    protected final ContextMenu contextMenu = new ContextMenu();
    protected static final int RIGHT_PANEL_WIDTH = 140;
    protected static final int ITEM_HEIGHT = 20;
    protected int toolbarWidth = 0;
    private String confirmDialogAction = "";
    private String confirmDialogId = null;
    private String promptDialogMode = "";
    private String promptDialogOldId = null;
    private int promptDialogY = 0;

    public ListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    protected abstract List<String> getItemIds();
    protected abstract String getItemDisplayName(String id);
    protected abstract void onSelect(String id);
    protected abstract void onNew(String id);
    protected abstract void onDelete(String id);
    protected abstract void onRename(String oldId, String newId);
    protected abstract void onDuplicate(String id);
    protected abstract void onCopy(String id);
    protected abstract void onPaste();
    protected abstract boolean canPaste();
    protected abstract void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h);
    protected abstract void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h);
    protected abstract boolean handleToolbarClick(double mx, double my, int button);
    protected abstract void doSave();

    protected boolean matchesFilter(String id, String filter) {
        return filter.isEmpty() || id.toLowerCase().contains(filter);
    }
    protected boolean canCreateNew() { return true; }
    protected String[] getContextMenuActions() { return new String[]{"Copy", "Paste", "Rename", "Duplicate", "Delete"}; }
    protected int getSearchWidth() { return getRightPanelWidth() - 8; }
    protected int getSearchTopOffset() { return 0; }
    protected int getListTitleOffset() { return 0; }
    protected void renderSearchExtras(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}
    protected boolean handleSearchExtrasClick(double mx, double my, int button, int x, int y, int w, int h) { return false; }

    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }
    protected int getToolbarWidth() { return toolbarWidth; }
    protected String getEmptyText() { return ""; }
    protected String getListTitle() { return ""; }
    protected String getNewButtonText() { return I18n.s("iscript.list.new"); }
    protected Component getSearchLabel() { return Component.literal(I18n.s("iscript.list.search")); }
    protected String getSelectedId() { return lifecycle.selection().get(); }
    protected void setSelectedId(String id) { lifecycle.selection().set(id); }

    protected void renderRightPanelExtras(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}
    protected boolean handleRightPanelClick(double mx, double my, int button, int x, int y, int w, int h) { return false; }
    protected void renderRowActions(GuiGraphics g, int mx, int my, float pt, String id, int x, int y, int w, int h) {}
    protected boolean handleRowClick(double mx, double my, int button, String id, int x, int y, int w, int h) { return false; }
    protected boolean hasCustomModals() { return false; }
    protected void renderCustomModals(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}
    protected boolean handleCustomModalClick(double mx, double my, int button) { return false; }
    protected boolean handleCustomModalKey(int keyCode, int scanCode, int modifiers) { return false; }
    protected boolean handleCustomModalChar(char codePoint, int modifiers) { return false; }
    protected boolean handleCustomModalScroll(double mx, double my, double delta) { return false; }
    protected boolean handleCustomModalRelease(double mx, double my, int button) { return false; }

    @Override
    public void init() {
        lifecycle.init();
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - getRightPanelWidth();
        lifecycle.search().request(rightX + 4, y + 4 + getSearchTopOffset(), getSearchWidth(), 16, getSearchLabel());
        lifecycle.modals().register("confirm",
                () -> lifecycle.state().modalOpenFlags.getOrDefault("confirm", false),
                v -> lifecycle.state().modalOpenFlags.put("confirm", v),
                () -> {}, () -> {}, "confirm");
        lifecycle.modals().register("prompt",
                () -> lifecycle.state().modalOpenFlags.getOrDefault("prompt", false),
                v -> lifecycle.state().modalOpenFlags.put("prompt", v),
                () -> {}, () -> {}, "promptInput");
        lifecycle.save().clearDirty();
        contextMenu.close();
    }

    protected List<String> getFilteredIds() {
        List<String> ids = new ArrayList<>(getItemIds());
        String filter = lifecycle.search().box() != null ? lifecycle.search().box().getValue().trim().toLowerCase() : lifecycle.state().lastSearch.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (String id : ids) {
            if (matchesFilter(id, filter)) result.add(id);
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        lifecycle.tick(this::doSave);
        if (lifecycle.search().box() == null && this.minecraft != null) lifecycle.search().recreateIfMissing();
    }

    @Override
    public void removed() {
        if (lifecycle.save().isDirty()) doSave();
        lifecycle.removed();
        closeConfirmDialog();
        closePromptDialog();
        contextMenu.close();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        boolean modalOpen = lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || hasCustomModals();
        if (!modalOpen) {
            g.fill(x, y, x + w, y + h, Theme.BG_INNER);
            int rightX = x + w - getRightPanelWidth();
            int toolbarX = rightX - getToolbarWidth();
            renderToolbar(g, mx, my, pt, toolbarX, y, getToolbarWidth(), h);
            g.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
            g.renderOutline(rightX, y, getRightPanelWidth(), h, Theme.BG_HOVER);
            if (lifecycle.search().box() != null) {
                lifecycle.search().setPos(rightX + 4, y + 4 + getSearchTopOffset(), getSearchWidth(), 16);
                lifecycle.search().setVisible(true);
            }
            renderRightPanelExtras(g, mx, my, pt, rightX, y, getRightPanelWidth(), h);
            renderSearchExtras(g, mx, my, pt, rightX, y, getRightPanelWidth(), h);
            g.drawString(this.font, getListTitle(), rightX + 8, y + 26 + getListTitleOffset(), Theme.ACCENT);
            List<String> ids = getFilteredIds();
            int searchOffset = getSearchTopOffset();
            int titleOffset = getListTitleOffset();
            int listH = h - 68 - searchOffset - titleOffset;
            int listY = y + 42 + titleOffset;
            int scroll = lifecycle.selection().scroll();
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(getSelectedId());
                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
                g.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                g.drawString(this.font, getItemDisplayName(id), rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
                renderRowActions(g, mx, my, pt, id, rightX + 4, rowY, getRightPanelWidth() - 8, ITEM_HEIGHT - 2);
            }
            int newY = y + h - 28;
            boolean newHovered = mx >= rightX + 4 && mx <= x + w - 4 && my >= newY && my <= newY + 22;
            boolean newDisabled = !canCreateNew();
            int newColor = newDisabled ? Theme.TEXT_MUTE : (newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(rightX + 4, newY, x + w - 4, newY + 22, newColor);
            g.renderOutline(rightX + 4, newY, getRightPanelWidth() - 8, 22, Theme.BORDER);
            g.drawCenteredString(this.font, getNewButtonText(), rightX + getRightPanelWidth() / 2, newY + 6, newDisabled ? Theme.TEXT_MUTE : Theme.ACCENT);
            int leftX = x + 4;
            int leftY = y + 4;
            int leftW = toolbarX - x - 8;
            int leftH = h - 8;
            if (getSelectedId() != null) renderEditor(g, mx, my, pt, leftX, leftY, leftW, leftH);
            else g.drawCenteredString(this.font, getEmptyText(), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
        } else {
            g.fill(x, y, x + w, y + h, Theme.BG_INNER);
            if (lifecycle.search().box() != null) {
                lifecycle.search().setVisible(false);
            }
        }
        if (lifecycle.modals().isOpen("confirm")) renderConfirmDialog(g, x, w, mx, my);
        if (lifecycle.modals().isOpen("prompt")) renderPromptDialog(g, x, w, mx, my);
        if (hasCustomModals()) renderCustomModals(g, mx, my, pt, x, y, w, h);
        if (contextMenu.isOpen()) contextMenu.render(g, this.font, mx, my);
    }

    private void renderConfirmDialog(GuiGraphics g, int x, int w, int mx, int my) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 70;
        int dx = cx - dw / 2;
        int dy = this.parent.height / 2 - 30;
        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ERROR);
        g.drawCenteredString(this.font, "Delete \"" + confirmDialogId + "\"?", cx, dy + 8, Theme.ERROR);
        boolean okHovered = mx >= cx - 50 && mx <= cx - 2 && my >= dy + 38 && my <= dy + 60;
        g.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 38, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.button.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : 0xFFAA4444);
        boolean cancelHovered = mx >= cx + 2 && mx <= cx + 50 && my >= dy + 38 && my <= dy + 60;
        g.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 38, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.button.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT);
    }

    private void renderPromptDialog(GuiGraphics g, int x, int w, int mx, int my) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 80;
        int dx = cx - dw / 2;
        int dy = promptDialogY;
        g.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_INNER, 0.8f));
        g.fill(dx, dy, dx + dw, dy + dh, Theme.BG_INNER);
        g.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
        g.drawCenteredString(this.font, "rename".equals(promptDialogMode) ? I18n.s("iscript.dialog.rename") : I18n.s("iscript.dialog.new_name"), cx, dy + 6, Theme.ACCENT);
        EditBox box = lifecycle.editors().box("promptInput");
        if (box != null) { box.setX(cx - 100); box.setY(dy + 24); }
        boolean okHovered = mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74;
        g.fill(cx - 50, dy + 52, cx - 2, dy + 74, okHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx - 50, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, "rename".equals(promptDialogMode) ? I18n.s("iscript.button.rename") : I18n.s("iscript.button.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : 0xFF44AA44);
        boolean cancelHovered = mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74;
        g.fill(cx + 2, dy + 52, cx + 50, dy + 74, cancelHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(cx + 2, dy + 52, 48, 22, Theme.BORDER);
        g.drawCenteredString(this.font, I18n.s("iscript.button.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : 0xFFAA4444);
    }

    protected void openConfirmDialog(String action, String id) {
        confirmDialogAction = action;
        confirmDialogId = id;
        lifecycle.modals().open("confirm");
    }

    protected void closeConfirmDialog() {
        lifecycle.modals().close("confirm");
        confirmDialogAction = "";
        confirmDialogId = null;
    }

    protected void executeConfirm() {
        if ("delete".equals(confirmDialogAction) && confirmDialogId != null) onDelete(confirmDialogId);
        closeConfirmDialog();
    }

    protected void openPromptDialog(String mode, String oldId) {
        promptDialogMode = mode;
        promptDialogOldId = oldId;
        lifecycle.modals().open("prompt");
        promptDialogY = this.parent.height / 2 - 40;
        int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
        EditBox box = lifecycle.editors().addBox("promptInput", cx - 100, promptDialogY + 20, 200, 20, Component.literal(I18n.s("iscript.dialog.placeholder.name")), "rename".equals(mode) && oldId != null ? oldId : "");
        if (box != null) { box.setMaxLength(64); parent.setFocusedWidget(box); }
    }

    protected void closePromptDialog() {
        lifecycle.modals().close("prompt");
        promptDialogOldId = null;
        lifecycle.editors().remove("promptInput");
    }

    protected void confirmPromptDialog() {
        EditBox box = lifecycle.editors().box("promptInput");
        if (box == null) { closePromptDialog(); return; }
        String name = box.getValue().trim();
        String mode = promptDialogMode;
        String oldId = promptDialogOldId;
        closePromptDialog();
        if (name.isEmpty()) return;
        if ("create".equals(mode)) onNew(name);
        else if ("rename".equals(mode) && oldId != null) onRename(oldId, name);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (lifecycle.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int cx = x + w / 2;
            int dy = this.parent.height / 2 - 30;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 38 && my <= dy + 60) { executeConfirm(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 38 && my <= dy + 60) { closeConfirmDialog(); return true; }
            return true;
        }
        if (lifecycle.modals().isOpen("prompt")) {
            int x = DashboardScreen.SIDEBAR_W;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int cx = x + w / 2;
            int dy = promptDialogY;
            if (mx >= cx - 50 && mx <= cx - 2 && my >= dy + 52 && my <= dy + 74) { confirmPromptDialog(); return true; }
            if (mx >= cx + 2 && mx <= cx + 50 && my >= dy + 52 && my <= dy + 74) { closePromptDialog(); return true; }
            EditBox box = lifecycle.editors().box("promptInput");
            if (box != null && mx >= box.getX() && mx <= box.getX() + box.getWidth() && my >= box.getY() && my <= box.getY() + box.getHeight()) {
                parent.setFocusedWidget(box);
                return box.mouseClicked(mx, my, button);
            }
            return true;
        }
        if (hasCustomModals() && handleCustomModalClick(mx, my, button)) return true;
        if (contextMenu.isOpen()) {
            String targetId = contextMenu.getItemId();
            boolean handled = contextMenu.mouseClicked(mx, my, button);
            String action = contextMenu.getLastAction();
            if (action != null && targetId != null) {
                switch (action) {
                    case "Copy" -> onCopy(targetId);
                    case "Paste" -> onPaste();
                    case "Rename" -> openPromptDialog("rename", targetId);
                    case "Duplicate" -> onDuplicate(targetId);
                    case "Delete" -> openConfirmDialog("delete", targetId);
                }
            }
            contextMenu.close();
            return true;
        }
        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - getRightPanelWidth();
            List<String> ids = getFilteredIds();
            int searchOffset = getSearchTopOffset();
            int titleOffset = getListTitleOffset();
            int listH = h - 68 - searchOffset - titleOffset;
            int listY = y + 42 + titleOffset;
            int scroll = lifecycle.selection().scroll();
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                if (mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2) {
                    contextMenu.setCustomActions(getContextMenuActions());
                    contextMenu.open((int) mx, (int) my, ids.get(i), canPaste());
                    return true;
                }
            }
        }
        if (button != 0) return false;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - getRightPanelWidth();
        int toolbarX = rightX - getToolbarWidth();
        if (handleToolbarClick(mx, my, button)) return true;
        if (handleRightPanelClick(mx, my, button, rightX, y, getRightPanelWidth(), h)) return true;
        if (handleSearchExtrasClick(mx, my, button, rightX, y, getRightPanelWidth(), h)) return true;
        List<String> ids = getFilteredIds();
        int listH = h - 68 - getSearchTopOffset() - getListTitleOffset();
        int listY = y + 42 + getListTitleOffset();
        int scroll = lifecycle.selection().scroll();
        int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
        for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mx >= rightX + 4 && mx <= x + w - 4 && my >= rowY && my <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (handleRowClick(mx, my, button, id, rightX + 4, rowY, getRightPanelWidth() - 8, ITEM_HEIGHT - 2)) return true;
                if (!id.equals(getSelectedId())) onSelect(id);
                return true;
            }
        }
        int newY = y + h - 28;
        if (mx >= rightX + 4 && mx <= x + w - 4 && my >= newY && my <= newY + 22) {
            if (canCreateNew()) openPromptDialog("create", null);
            return true;
        }
        if (lifecycle.search().box() != null && mx >= lifecycle.search().box().getX() && mx <= lifecycle.search().box().getX() + lifecycle.search().box().getWidth() && my >= lifecycle.search().box().getY() && my <= lifecycle.search().box().getY() + lifecycle.search().box().getHeight()) {
            lifecycle.search().box().setFocused(true);
            parent.setFocusedWidget(lifecycle.search().box());
            return lifecycle.search().box().mouseClicked(mx, my, button);
        }
        if (getSelectedId() != null) {
            int leftX = x + 4;
            int leftY = y + 4;
            int leftW = toolbarX - x - 8;
            int leftH = h - 8;
            return handleEditorClick(mx, my, button, leftX, leftY, leftW, leftH);
        }
        return false;
    }

    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || contextMenu.isOpen()) return true;
        if (hasCustomModals() && handleCustomModalRelease(mx, my, button)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (lifecycle.modals().isOpen("confirm") || lifecycle.modals().isOpen("prompt") || contextMenu.isOpen()) return true;
        if (hasCustomModals() && handleCustomModalScroll(mx, my, delta)) return true;
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused() && mx >= lifecycle.search().box().getX() && mx <= lifecycle.search().box().getX() + lifecycle.search().box().getWidth() && my >= lifecycle.search().box().getY() && my <= lifecycle.search().box().getY() + lifecycle.search().box().getHeight()) {
            return lifecycle.search().box().mouseScrolled(mx, my, delta);
        }
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - getRightPanelWidth();
        int toolbarX = rightX - getToolbarWidth();
        if (handleEditorScroll(mx, my, delta, x + 4, y + 4, toolbarX - x - 8, h - 8)) return true;
        if (mx >= rightX && mx <= x + w) {
            List<String> ids = getFilteredIds();
            int listH = h - 68 - getSearchTopOffset() - getListTitleOffset();
            int visible = Math.max(1, (listH - 24) / ITEM_HEIGHT);
            int maxScroll = Math.max(0, ids.size() - visible);
            int scroll = lifecycle.selection().scroll();
            if (delta > 0) lifecycle.selection().scroll(Math.max(0, scroll - 1));
            else lifecycle.selection().scroll(Math.min(scroll + 1, maxScroll));
            return true;
        }
        return false;
    }

    protected boolean handleEditorScroll(double mx, double my, double delta, int leftX, int leftY, int leftW, int leftH) {
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (lifecycle.modals().isOpen("prompt")) {
            EditBox box = lifecycle.editors().box("promptInput");
            if (box != null && box.isFocused()) return box.charTyped(codePoint, modifiers);
            return true;
        }
        if (hasCustomModals() && handleCustomModalChar(codePoint, modifiers)) return true;
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (lifecycle.modals().isOpen("confirm")) {
            if (keyCode == 257 || keyCode == 335) { executeConfirm(); return true; }
            if (keyCode == 256) { closeConfirmDialog(); return true; }
            return true;
        }
        if (lifecycle.modals().isOpen("prompt")) {
            if (keyCode == 257 || keyCode == 335) { confirmPromptDialog(); return true; }
            if (keyCode == 256) { closePromptDialog(); return true; }
            EditBox box = lifecycle.editors().box("promptInput");
            if (box != null && box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (hasCustomModals() && handleCustomModalKey(keyCode, scanCode, modifiers)) return true;
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}