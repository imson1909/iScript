package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.DialogGraphManager;
import com.iscript.iscript.data.dialog.DialogGraphData;
import com.iscript.iscript.data.dialog.DialogNodeData;
import com.iscript.iscript.network.IScriptNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.iscript.iscript.network.packet.ServerCommandPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;
    private String selectedId = null;

    private boolean showNameDialog = false;
    private EditBox nameInputBox = null;
    private int nameDialogY = 0;
    private String nameDialogMode = "";
    private String renameOldId = null;

    private DialogGraphData currentDialog = null;
    private double canvasX = 0, canvasY = 0;
    private double zoom = 1.0;
    private String draggingNodeId = null;
    private String connectingFromId = null;
    private int connectingSlot = -1;
    private boolean panning = false;
    private double panStartX, panStartY;
    private double panCanvasStartX, panCanvasStartY;
    private String selectedNodeId = null;
    private double dragStartMouseX, dragStartMouseY;
    private int dragStartNodeX, dragStartNodeY;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private String contextMenuNodeId = null;

    private boolean showItemContextMenu = false;
    private int itemContextMenuX, itemContextMenuY;
    private String itemContextMenuId = null;

    private boolean showConfirmDialog = false;
    private int confirmDialogY = 0;
    private String confirmDialogAction = "";
    private String confirmDialogId = null;

    private boolean showNodeTypeMenu = false;
    private int nodeTypeMenuX, nodeTypeMenuY;

    private boolean showNodeEditor = false;
    private DialogNodeData editingNode = null;
    private EditBox nodeEditTitleBox = null;
    private EditBox nodeEditTextBox = null;
    private EditBox nodeEditPortraitBox = null;
    private int nodeEditorY = 0;

    public DialogListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        showNameDialog = false;
        nameInputBox = null;
        showContextMenu = false;
        showItemContextMenu = false;
        showConfirmDialog = false;
        showNodeTypeMenu = false;
        showNodeEditor = false;
        editingNode = null;
        closeNodeEditor();
        closeNameDialog();
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal("Dialog Name"));
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
            DialogGraphData dialog = new DialogGraphData();
            dialog.setId(id);
            dialog.setName(name);
            DialogNodeData start = new DialogNodeData();
            start.setId("start");
            start.setTitle("Start");
            start.setText("Hello!");
            start.setX(200);
            start.setY(200);
            dialog.addNode(start);
            DialogGraphManager.add(level, dialog);
            switchToDialog(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            DialogGraphData oldDialog = DialogGraphManager.get(level, renameOldId);
            if (oldDialog != null) {
                DialogGraphData newDialog = oldDialog.copy();
                newDialog.setId(id);
                newDialog.setName(name);
                DialogGraphManager.add(level, newDialog);
                DialogGraphManager.remove(level, renameOldId);
                if (selectedId != null && selectedId.equals(renameOldId)) {
                    selectedId = id;
                    currentDialog = newDialog;
                }
            }
        }
    }

    private void switchToDialog(String id) {
        selectedId = id;
        selectedNodeId = null;
        draggingNodeId = null;
        cancelConnection();
        showContextMenu = false;
        showItemContextMenu = false;
        showNodeTypeMenu = false;
        closeNodeEditor();

        currentDialog = DialogGraphManager.get(level, id);
        canvasX = 0;
        canvasY = 0;
        zoom = 1.0;
    }

    private void runDialog() {
        if (selectedId == null || currentDialog == null) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_DIALOG, ServerCommandPacket.requestDialogToTag(selectedId + ":" + currentDialog.getStartNodeId())));
    }

    private void addNode(String type) {
        if (currentDialog == null) return;
        String nodeId = "node_" + System.currentTimeMillis();
        DialogNodeData node = new DialogNodeData();
        node.setId(nodeId);
        switch (type) {
            case "start" -> {
                node.setTitle("Start");
                node.setText("Hello!");
            }
            case "npc" -> {
                node.setTitle("NPC");
                node.setText("NPC says...");
            }
            case "player" -> {
                node.setTitle("Player");
                node.setText("Player says...");
            }
            case "choice" -> {
                node.setTitle("Choice");
                node.setText("Choose...");
            }
            case "condition" -> {
                node.setTitle("Condition");
                node.setText("Check...");
            }
            case "action" -> {
                node.setTitle("Action");
                node.setText("Run command...");
            }
            default -> {
                node.setTitle("Node");
                node.setText("Text...");
            }
        }
        node.setX((int) (canvasX + 200));
        node.setY((int) (canvasY + 200));
        currentDialog.addNode(node);
        saveDialog();
    }

    private void saveDialog() {
        if (currentDialog == null) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_DIALOG_GRAPH, ServerCommandPacket.saveDialogGraphToTag(currentDialog)));
    }

    private void deleteNode(String nodeId) {
        if (currentDialog == null) return;
        currentDialog.removeNode(nodeId);
        if (selectedNodeId != null && selectedNodeId.equals(nodeId)) selectedNodeId = null;
        saveDialog();
    }

    private void cancelConnection() {
        connectingFromId = null;
        connectingSlot = -1;
    }

    private void openNodeEditor(DialogNodeData node) {
        showNodeEditor = true;
        editingNode = node;
        nodeEditorY = this.parent.height / 2 - 70;

        nodeEditTitleBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, Component.literal("Title"));
        nodeEditTitleBox.setValue(node.getTitle());
        nodeEditTextBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, Component.literal("Text"));
        nodeEditTextBox.setValue(node.getText());
        nodeEditPortraitBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, Component.literal("Portrait"));
        nodeEditPortraitBox.setValue(node.getPortrait());

        parent.addWidget(nodeEditTitleBox);
        parent.addWidget(nodeEditTextBox);
        parent.addWidget(nodeEditPortraitBox);
        parent.setFocusedWidget(nodeEditTitleBox);
    }

    private void closeNodeEditor() {
        if (editingNode != null) {
            editingNode.setTitle(nodeEditTitleBox.getValue());
            editingNode.setText(nodeEditTextBox.getValue());
            editingNode.setPortrait(nodeEditPortraitBox.getValue());
            saveDialog();
        }
        showNodeEditor = false;
        if (nodeEditTitleBox != null) {
            nodeEditTitleBox.setVisible(false);
            parent.removeEditorWidget(nodeEditTitleBox);
            nodeEditTitleBox = null;
        }
        if (nodeEditTextBox != null) {
            nodeEditTextBox.setVisible(false);
            parent.removeEditorWidget(nodeEditTextBox);
            nodeEditTextBox = null;
        }
        if (nodeEditPortraitBox != null) {
            nodeEditPortraitBox.setVisible(false);
            parent.removeEditorWidget(nodeEditPortraitBox);
            nodeEditPortraitBox = null;
        }
        editingNode = null;
    }

    private void openItemContextMenu(String itemId, int mx, int my) {
        showItemContextMenu = true;
        itemContextMenuId = itemId;
        itemContextMenuX = mx;
        itemContextMenuY = my;
    }

    private void closeItemContextMenu() {
        showItemContextMenu = false;
        itemContextMenuId = null;
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
            DialogGraphManager.remove(level, confirmDialogId);
            if (selectedId != null && selectedId.equals(confirmDialogId)) {
                selectedId = null;
                currentDialog = null;
            }
        }
        closeConfirmDialog();
    }

    private void copyItem(String id) {
        DashboardScreen.clipboard = id;
    }

    private void pasteItem() {
        String sourceId = DashboardScreen.clipboard;
        if (sourceId == null || sourceId.isEmpty()) return;
        DialogGraphData source = DialogGraphManager.get(level, sourceId);
        if (source == null) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (DialogGraphManager.get(level, newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }

        DialogGraphData copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (Copy)");
        DialogGraphManager.add(level, copy);
        switchToDialog(newId);
    }

    private void duplicateItem(String id) {
        DialogGraphData source = DialogGraphManager.get(level, id);
        if (source == null) return;

        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (DialogGraphManager.get(level, newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }

        DialogGraphData copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (" + counter + ")");
        DialogGraphManager.add(level, copy);
        switchToDialog(newId);
    }

    @Override
    public void removed() {
        closeNameDialog();
        closeNodeEditor();
        closeConfirmDialog();
        super.removed();
    }

    private int getNodeWidth() {
        return Math.max(60, (int) (140 * zoom));
    }

    private int getNodeHeight() {
        return Math.max(30, (int) (78 * zoom));
    }

    private int getNodeHeaderHeight() {
        return Math.max(10, (int) (18 * zoom));
    }

    private int worldToScreenX(int wx, int leftX) {
        return (int) ((wx - canvasX) * zoom + leftX);
    }

    private int worldToScreenY(int wy, int leftY) {
        return (int) ((wy - canvasY) * zoom + leftY);
    }

    private int getOutputSlotY(int nh, int headerH, int slot, int totalSlots) {
        if (totalSlots <= 0) totalSlots = 1;
        return headerH + (slot + 1) * (nh - headerH) / (totalSlots + 1);
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) {
            graphics.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), color);
        } else if (y1 == y2) {
            graphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, color);
        } else {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int steps = Math.max(dx, dy);
            for (int i = 0; i <= steps; i++) {
                int px = x1 + (x2 - x1) * i / steps;
                int py = y1 + (y2 - y1) * i / steps;
                graphics.fill(px - 1, py - 1, px + 1, py + 1, color);
            }
        }
    }

    private void renderConnection(GuiGraphics graphics, DialogNodeData from, DialogNodeData to, int sourceSlot, int leftX, int leftY) {
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        int x1 = worldToScreenX(from.getX() + nw, leftX);
        int y1 = worldToScreenY(from.getY() + getOutputSlotY(nh, headerH, sourceSlot, Math.max(1, from.getConnections().size())), leftY);
        int x2 = worldToScreenX(to.getX(), leftX);
        int y2 = worldToScreenY(to.getY() + nh / 2, leftY);

        int color = 0xFF888888;
        int midX = (x1 + x2) / 2;

        drawLine(graphics, x1, y1, midX, y1, color);
        drawLine(graphics, midX, y1, midX, y2, color);
        drawLine(graphics, midX, y2, x2, y2, color);

        if (x2 > x1) {
            graphics.fill(x2 - 4, y2 - 3, x2, y2 + 3, color);
            graphics.fill(x2 - 2, y2 - 5, x2, y2 + 5, color);
        } else {
            graphics.fill(x2, y2 - 3, x2 + 4, y2 + 3, color);
            graphics.fill(x2, y2 - 5, x2 + 2, y2 + 5, color);
        }
    }

    private void renderNode(GuiGraphics graphics, DialogNodeData node, int leftX, int leftY, int mouseX, int mouseY) {
        int nx = worldToScreenX(node.getX(), leftX);
        int ny = worldToScreenY(node.getY(), leftY);
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();

        boolean hovered = mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh;
        boolean selected = node.getId().equals(selectedNodeId);
        boolean isStart = currentDialog != null && node.getId().equals(currentDialog.getStartNodeId());

        graphics.fill(nx + 2, ny + 2, nx + nw + 2, ny + nh + 2, 0x44000000);

        int bodyColor = selected ? 0xFF334466 : 0xFF1E1E28;
        graphics.fill(nx, ny, nx + nw, ny + nh, bodyColor);
        graphics.renderOutline(nx, ny, nw, nh, selected ? 0xFF55AAFF : (isStart ? 0xFF00D4AA : 0xFF444455));

        int headerColor = isStart ? 0xFF4488AA : 0xFF2A2A3A;
        graphics.fill(nx, ny, nx + nw, ny + headerH, headerColor);
        String title = font.plainSubstrByWidth(node.getTitle(), nw - 8);
        graphics.drawString(font, title, nx + 4, ny + (headerH - 8) / 2, 0xFFFFFFFF);

        if (nh > 30) {
            String text = node.getText();
            if (text.length() > 30) text = text.substring(0, 30) + "...";
            text = font.plainSubstrByWidth(text, nw - 8);
            graphics.drawString(font, text, nx + 4, ny + headerH + 4, 0xFFAAAAAA);
        }

        int pinR = Math.max(3, (int) (4 * zoom));
        int inX = nx;
        int inY = ny + nh / 2;
        graphics.fill(inX - pinR, inY - pinR, inX + pinR, inY + pinR, 0xFF55FF55);
        graphics.renderOutline(inX - pinR, inY - pinR, pinR * 2, pinR * 2, 0xFF338833);

        int outputs = Math.max(1, node.getConnections().size());
        int maxSlots = 3;
        for (int i = 0; i < Math.min(outputs, maxSlots); i++) {
            int slotY = ny + getOutputSlotY(nh, headerH, i, Math.min(outputs, maxSlots));
            int outX = nx + nw;
            boolean active = connectingFromId != null && connectingFromId.equals(node.getId()) && connectingSlot == i;
            int pinColor = active ? 0xFF55AAFF : 0xFFFF5555;
            graphics.fill(outX - pinR, slotY - pinR, outX + pinR, slotY + pinR, pinColor);
            graphics.renderOutline(outX - pinR, slotY - pinR, pinR * 2, pinR * 2, active ? 0xFF3366AA : 0xFFAA3333);
        }
    }

    private void renderGrid(GuiGraphics graphics, int leftX, int leftY, int leftW, int leftH) {
        int gridSize = (int) (40 * zoom);
        if (gridSize < 10) gridSize = 10;
        int offsetX = (int) ((-canvasX * zoom) % gridSize);
        if (offsetX < 0) offsetX += gridSize;
        int offsetY = (int) ((-canvasY * zoom) % gridSize);
        if (offsetY < 0) offsetY += gridSize;

        for (int gx = leftX + offsetX; gx < leftX + leftW; gx += gridSize) {
            graphics.fill(gx, leftY, gx + 1, leftY + leftH, 0xFF22222A);
        }
        for (int gy = leftY + offsetY; gy < leftY + leftH; gy += gridSize) {
            graphics.fill(leftX, gy, leftX + leftW, gy + 1, 0xFF22222A);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        if (!showNameDialog && !showNodeEditor && !showConfirmDialog) {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);

            graphics.fill(toolbarX, y, rightX, y + h, 0xFF1A1A24);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, 0xFF2A2A3A);

            int btnSize = 24;
            int btnY = y + 8;

            boolean runHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, runHovered ? 0xFF2A4A2A : 0xFF1E281E);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, runHovered ? 0xFF55FF55 : 0xFF44AA44);
            btnY += btnSize + 6;

            if (currentDialog != null) {
                boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
                graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? 0xFF2A3A4A : 0xFF1E1E28);
                graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
                graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            }

            graphics.fill(rightX, y, x + w, y + h, 0xFF1E1E28);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);
            graphics.drawString(this.font, "Dialogs", rightX + 8, y + 6, 0xFF55AAFF);

            var dialogs = DialogGraphManager.getAll(level);
            List<String> ids = new ArrayList<>(dialogs.keySet());
            Collections.sort(ids);

            int listH = h - 40;
            int listY = y + 20;
            int visible = Math.max(1, listH / ITEM_HEIGHT);

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
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
            graphics.drawCenteredString(this.font, "+ New Dialog", rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, 0xFF55FF55);

            if (selectedId != null && currentDialog != null) {
                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, 0xFF111118);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, 0xFF333344);

                RenderSystem.enableScissor(leftX * (int) this.minecraft.getWindow().getGuiScale(), (this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * (int) this.minecraft.getWindow().getGuiScale(), leftW * (int) this.minecraft.getWindow().getGuiScale(), leftH * (int) this.minecraft.getWindow().getGuiScale());

                renderGrid(graphics, leftX, leftY, leftW, leftH);

                for (DialogNodeData node : currentDialog.getNodes().values()) {
                    for (DialogNodeData.NodeConnection conn : node.getConnections()) {
                        DialogNodeData target = currentDialog.getNode(conn.getTargetNodeId());
                        if (target != null) {
                            renderConnection(graphics, node, target, conn.getSourceSlot(), leftX, leftY);
                        }
                    }
                }

                if (connectingFromId != null) {
                    DialogNodeData from = currentDialog.getNode(connectingFromId);
                    if (from != null) {
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int x1 = worldToScreenX(from.getX() + nw, leftX);
                        int y1 = worldToScreenY(from.getY() + getOutputSlotY(nh, headerH, connectingSlot, Math.max(1, from.getConnections().size())), leftY);
                        int x2 = mouseX;
                        int y2 = mouseY;
                        int midX = (x1 + x2) / 2;
                        drawLine(graphics, x1, y1, midX, y1, 0xFF55AAFF);
                        drawLine(graphics, midX, y1, midX, y2, 0xFF55AAFF);
                        drawLine(graphics, midX, y2, x2, y2, 0xFF55AAFF);
                    }
                }

                for (DialogNodeData node : currentDialog.getNodes().values()) {
                    renderNode(graphics, node, leftX, leftY, mouseX, mouseY);
                }

                RenderSystem.disableScissor();

                graphics.drawString(font, String.format("%.0f%%", zoom * 100), leftX + 4, leftY + 4, 0xFF666666);
            } else {
                graphics.drawCenteredString(this.font, "Select a dialog from the list", leftX + leftW / 2, y + h / 2, 0xFF555566);
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
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename Dialog" : "New Dialog Name", cx, dy + 6, 0xFF55AAFF);

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

        if (showNodeEditor && editingNode != null) {
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = nodeEditorY;

            graphics.fill(dx - 10, dy - 10, dx + 200, dy + 125, 0xFF2A2A3A);
            graphics.renderOutline(dx - 10, dy - 10, 210, 135, 0xFF6688FF);
            graphics.drawString(font, "Edit Node (ESC or Enter to save)", dx, dy, 0xFFFFFFFF);

            graphics.drawString(font, "Title:", dx, dy + 18, 0xFFAAAAAA);
            nodeEditTitleBox.setX(dx);
            nodeEditTitleBox.setY(dy + 30);
            nodeEditTitleBox.setVisible(true);

            graphics.drawString(font, "Text:", dx, dy + 52, 0xFFAAAAAA);
            nodeEditTextBox.setX(dx);
            nodeEditTextBox.setY(dy + 64);
            nodeEditTextBox.setVisible(true);

            graphics.drawString(font, "Portrait:", dx, dy + 86, 0xFFAAAAAA);
            nodeEditPortraitBox.setX(dx);
            nodeEditPortraitBox.setY(dy + 98);
            nodeEditPortraitBox.setVisible(true);
        }

        if (showContextMenu && contextMenuNodeId != null) {
            int wctx = 100;
            int hctx = 66;
            graphics.fill(contextMenuX, contextMenuY, contextMenuX + wctx, contextMenuY + hctx, 0xFF222222);
            graphics.renderOutline(contextMenuX, contextMenuY, wctx, hctx, 0xFF666666);

            int cy = contextMenuY + 2;
            boolean h1 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, h1 ? 0xFF444444 : 0xFF222222);
            graphics.drawString(font, "Edit", contextMenuX + 4, cy + 6, h1 ? 0xFFFFFFFF : 0xFFCCCCCC);
            cy += 22;

            boolean h2 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, h2 ? 0xFF444444 : 0xFF222222);
            graphics.drawString(font, "Set Start", contextMenuX + 4, cy + 6, h2 ? 0xFFFFFFFF : 0xFFCCCCCC);
            cy += 22;

            boolean h3 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            graphics.fill(contextMenuX + 1, cy, contextMenuX + wctx - 1, cy + 20, h3 ? 0xFF664444 : 0xFF222222);
            graphics.drawString(font, "Delete", contextMenuX + 4, cy + 6, h3 ? 0xFFFF6666 : 0xFFCCCCCC);
        }

        if (showNodeTypeMenu) {
            renderNodeTypeMenu(graphics, mouseX, mouseY);
        }

        if (showItemContextMenu && itemContextMenuId != null) {
            String[] items = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int hctx = (canPaste ? 5 : 4) * 22 + 4;
            graphics.fill(itemContextMenuX, itemContextMenuY, itemContextMenuX + wctx, itemContextMenuY + hctx, 0xFF222222);
            graphics.renderOutline(itemContextMenuX, itemContextMenuY, wctx, hctx, 0xFF666666);

            int cy = itemContextMenuY + 2;
            for (String item : items) {
                if (item.equals("Paste") && !canPaste) continue;
                boolean hovered = mouseX >= itemContextMenuX && mouseX <= itemContextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
                int bg = hovered ? 0xFF444444 : 0xFF222222;
                int tc = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
                if (item.equals("Delete")) tc = hovered ? 0xFFFF6666 : 0xFFCC4444;
                graphics.fill(itemContextMenuX + 1, cy, itemContextMenuX + wctx - 1, cy + 20, bg);
                graphics.drawString(font, item, itemContextMenuX + 6, cy + 6, tc);
                cy += 22;
            }
        }
    }

    private void renderNodeTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] types = {"Start", "NPC", "Player", "Choice", "Condition", "Action"};
        int w = 110;
        int h = types.length * 22 + 4;
        graphics.fill(nodeTypeMenuX, nodeTypeMenuY, nodeTypeMenuX + w, nodeTypeMenuY + h, 0xFF222222);
        graphics.renderOutline(nodeTypeMenuX, nodeTypeMenuY, w, h, 0xFF666666);

        int cy = nodeTypeMenuY + 2;
        for (String type : types) {
            boolean hovered = mouseX >= nodeTypeMenuX && mouseX <= nodeTypeMenuX + w && mouseY >= cy && mouseY <= cy + 20;
            graphics.fill(nodeTypeMenuX + 1, cy, nodeTypeMenuX + w - 1, cy + 20, hovered ? 0xFF444444 : 0xFF222222);
            graphics.drawString(font, type, nodeTypeMenuX + 6, cy + 6, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            cy += 22;
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

        if (showNodeEditor) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
            int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = nodeEditorY;

            if (mouseX >= dx - 10 && mouseX <= dx + 200 && mouseY >= dy - 10 && mouseY <= dy + 125) {
                if (nodeEditTitleBox != null && mouseX >= nodeEditTitleBox.getX() && mouseX <= nodeEditTitleBox.getX() + nodeEditTitleBox.getWidth() && mouseY >= nodeEditTitleBox.getY() && mouseY <= nodeEditTitleBox.getY() + nodeEditTitleBox.getHeight()) {
                    parent.setFocusedWidget(nodeEditTitleBox);
                    return nodeEditTitleBox.mouseClicked(mouseX, mouseY, button);
                }
                if (nodeEditTextBox != null && mouseX >= nodeEditTextBox.getX() && mouseX <= nodeEditTextBox.getX() + nodeEditTextBox.getWidth() && mouseY >= nodeEditTextBox.getY() && mouseY <= nodeEditTextBox.getY() + nodeEditTextBox.getHeight()) {
                    parent.setFocusedWidget(nodeEditTextBox);
                    return nodeEditTextBox.mouseClicked(mouseX, mouseY, button);
                }
                if (nodeEditPortraitBox != null && mouseX >= nodeEditPortraitBox.getX() && mouseX <= nodeEditPortraitBox.getX() + nodeEditPortraitBox.getWidth() && mouseY >= nodeEditPortraitBox.getY() && mouseY <= nodeEditPortraitBox.getY() + nodeEditPortraitBox.getHeight()) {
                    parent.setFocusedWidget(nodeEditPortraitBox);
                    return nodeEditPortraitBox.mouseClicked(mouseX, mouseY, button);
                }
                return true;
            }
            closeNodeEditor();
            return true;
        }

        if (showContextMenu) {
            int y = contextMenuY + 2;
            if (mouseX >= contextMenuX && mouseX <= contextMenuX + 100) {
                if (mouseY >= y && mouseY <= y + 20) {
                    DialogNodeData node = currentDialog != null ? currentDialog.getNode(contextMenuNodeId) : null;
                    if (node != null) openNodeEditor(node);
                    showContextMenu = false;
                    return true;
                }
                y += 22;
                if (mouseY >= y && mouseY <= y + 20) {
                    if (currentDialog != null) {
                        currentDialog.setStartNodeId(contextMenuNodeId);
                        saveDialog();
                    }
                    showContextMenu = false;
                    return true;
                }
                y += 22;
                if (mouseY >= y && mouseY <= y + 20) {
                    deleteNode(contextMenuNodeId);
                    showContextMenu = false;
                    return true;
                }
            }
            showContextMenu = false;
            return true;
        }

        if (showItemContextMenu) {
            String[] items = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int cy = itemContextMenuY + 2;
            for (String item : items) {
                if (item.equals("Paste") && !canPaste) continue;
                if (mouseX >= itemContextMenuX && mouseX <= itemContextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20) {
                    switch (item) {
                        case "Copy" -> copyItem(itemContextMenuId);
                        case "Paste" -> pasteItem();
                        case "Rename" -> openNameDialog("rename", itemContextMenuId);
                        case "Duplicate" -> duplicateItem(itemContextMenuId);
                        case "Delete" -> openConfirmDialog("delete", itemContextMenuId);
                    }
                    closeItemContextMenu();
                    return true;
                }
                cy += 22;
            }
            closeItemContextMenu();
            return true;
        }

        if (showNodeTypeMenu) {
            String[] types = {"start", "npc", "player", "choice", "condition", "action"};
            int w = 110;
            int h = types.length * 22 + 4;
            if (mouseX >= nodeTypeMenuX && mouseX <= nodeTypeMenuX + w && mouseY >= nodeTypeMenuY && mouseY <= nodeTypeMenuY + h) {
                int idx = (int) ((mouseY - nodeTypeMenuY - 2) / 22);
                if (idx >= 0 && idx < types.length) {
                    addNode(types[idx]);
                }
            }
            showNodeTypeMenu = false;
            return true;
        }

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        int btnSize = 24;
        int btnY = y + 8;

        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            runDialog();
            return true;
        }
        btnY += btnSize + 6;

        if (currentDialog != null) {
            if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
                showNodeTypeMenu = true;
                nodeTypeMenuX = (int) mouseX;
                nodeTypeMenuY = (int) mouseY;
                return true;
            }
        }

        var dialogs = DialogGraphManager.getAll(level);
        List<String> ids = new ArrayList<>(dialogs.keySet());
        Collections.sort(ids);

        int listH = h - 40;
        int listY = y + 20;
        int visible = Math.max(1, listH / ITEM_HEIGHT);

        for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (!id.equals(selectedId)) {
                    switchToDialog(id);
                }
                return true;
            }
        }

        int newY = y + h - 28;
        if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22) {
            openNameDialog("create", null);
            return true;
        }

        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        int leftX = x + 4;
        int leftY = y + 4;

        if (currentDialog != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            if (button == 0) {
                List<DialogNodeData> nodes = new ArrayList<>(currentDialog.getNodes().values());
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    DialogNodeData node = nodes.get(i);
                    int nx = worldToScreenX(node.getX(), leftX);
                    int ny = worldToScreenY(node.getY(), leftY);
                    int nw = getNodeWidth();
                    int nh = getNodeHeight();
                    int headerH = getNodeHeaderHeight();
                    int pinR = Math.max(3, (int) (4 * zoom));

                    int inX = nx;
                    int inY = ny + nh / 2;
                    if (mouseX >= inX - pinR && mouseX <= inX + pinR && mouseY >= inY - pinR && mouseY <= inY + pinR) {
                        if (connectingFromId != null && !connectingFromId.equals(node.getId())) {
                            DialogNodeData from = currentDialog.getNode(connectingFromId);
                            if (from != null) {
                                DialogNodeData.NodeConnection conn = new DialogNodeData.NodeConnection();
                                conn.setTargetNodeId(node.getId());
                                conn.setSourceSlot(connectingSlot);
                                conn.setOptionText("Option");
                                from.getConnections().add(conn);
                                saveDialog();
                            }
                            cancelConnection();
                            return true;
                        }
                        return true;
                    }

                    int outputs = Math.max(1, node.getConnections().size());
                    int maxSlots = 3;
                    for (int j = 0; j < Math.min(outputs, maxSlots); j++) {
                        int slotY = ny + getOutputSlotY(nh, headerH, j, Math.min(outputs, maxSlots));
                        int outX = nx + nw;
                        if (mouseX >= outX - pinR && mouseX <= outX + pinR && mouseY >= slotY - pinR && mouseY <= slotY + pinR) {
                            connectingFromId = node.getId();
                            connectingSlot = j;
                            return true;
                        }
                    }

                    if (mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh) {
                        selectedNodeId = node.getId();
                        draggingNodeId = node.getId();
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        dragStartNodeX = node.getX();
                        dragStartNodeY = node.getY();
                        return true;
                    }
                }

                if (connectingFromId != null) {
                    cancelConnection();
                    return true;
                }
            }

            if (button == 1) {
                for (DialogNodeData node : currentDialog.getNodes().values()) {
                    int nx = worldToScreenX(node.getX(), leftX);
                    int ny = worldToScreenY(node.getY(), leftY);
                    int nw = getNodeWidth();
                    int nh = getNodeHeight();
                    if (mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh) {
                        showContextMenu = true;
                        contextMenuNodeId = node.getId();
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
                        return true;
                    }
                }
                panning = true;
                panStartX = mouseX;
                panStartY = mouseY;
                panCanvasStartX = canvasX;
                panCanvasStartY = canvasY;
                return true;
            }

            return true;
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
        if (showNodeEditor) {
            if (nodeEditTitleBox != null && nodeEditTitleBox.isFocused()) {
                return nodeEditTitleBox.charTyped(codePoint, modifiers);
            }
            if (nodeEditTextBox != null && nodeEditTextBox.isFocused()) {
                return nodeEditTextBox.charTyped(codePoint, modifiers);
            }
            if (nodeEditPortraitBox != null && nodeEditPortraitBox.isFocused()) {
                return nodeEditPortraitBox.charTyped(codePoint, modifiers);
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
            if (keyCode == 256) {
                closeNameDialog();
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
        if (showNodeEditor) {
            if (keyCode == 256 || keyCode == 257) {
                closeNodeEditor();
                return true;
            }
            if (nodeEditTitleBox != null && nodeEditTitleBox.isFocused()) {
                return nodeEditTitleBox.keyPressed(keyCode, scanCode, modifiers);
            }
            if (nodeEditTextBox != null && nodeEditTextBox.isFocused()) {
                return nodeEditTextBox.keyPressed(keyCode, scanCode, modifiers);
            }
            if (nodeEditPortraitBox != null && nodeEditPortraitBox.isFocused()) {
                return nodeEditPortraitBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showNameDialog || showNodeEditor || showContextMenu || showNodeTypeMenu || showItemContextMenu || showConfirmDialog) return true;

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            var dialogs = DialogGraphManager.getAll(level);
            List<String> ids = new ArrayList<>(dialogs.keySet());
            int listH = h - 40;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int maxScroll = Math.max(0, ids.size() - visible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }

        int leftX = x + 4;
        int leftY = y + 4;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;

        if (currentDialog != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            double oldZoom = zoom;
            if (delta > 0) zoom *= 1.1;
            else zoom *= 0.9;
            zoom = Math.max(0.3, Math.min(3.0, zoom));
            double mx = (mouseX - leftX) / oldZoom + canvasX;
            double my = (mouseY - leftY) / oldZoom + canvasY;
            canvasX = mx - (mouseX - leftX) / zoom;
            canvasY = my - (mouseY - leftY) / zoom;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (draggingNodeId != null && currentDialog != null) {
                DialogNodeData node = currentDialog.getNode(draggingNodeId);
                if (node != null && (node.getX() != dragStartNodeX || node.getY() != dragStartNodeY)) {
                    saveDialog();
                }
            }
            draggingNodeId = null;
        }
        if (button == 1) {
            panning = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            canvasX = panCanvasStartX + (panStartX - mouseX) / zoom;
            canvasY = panCanvasStartY + (panStartY - mouseY) / zoom;
            return true;
        }
        if (draggingNodeId != null && currentDialog != null) {
            DialogNodeData node = currentDialog.getNode(draggingNodeId);
            if (node != null) {
                double dx = (mouseX - dragStartMouseX) / zoom;
                double dy = (mouseY - dragStartMouseY) / zoom;
                node.setX((int) (dragStartNodeX + dx));
                node.setY((int) (dragStartNodeY + dy));
            }
            return true;
        }
        return false;
    }
}