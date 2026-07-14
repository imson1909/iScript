package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.EventSavedData;
import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.script.ScriptNodeConnection;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.RequestEventGraphsPacket;
import com.iscript.iscript.network.packet.RunEventGraphPacket;
import com.iscript.iscript.network.packet.SaveEventGraphPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventGraphListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 160;
    private static final int TOOLBAR_WIDTH = 32;
    private String selectedGraphId = null;

    private boolean showNameDialog = false;
    private EditBox nameInputBox = null;
    private int nameDialogY = 0;
    private String nameDialogMode = "";
    private String renameOldId = null;

    private EventGraphData currentGraph = null;
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
    private ScriptNodeData editingNode = null;
    private final List<EditBox> paramEditBoxes = new ArrayList<>();
    private MultiLineEditBox scriptMultiLineBox = null;
    private int nodeEditorY = 0;

    public EventGraphListSubScreen(DashboardScreen parent, ServerLevel level) {
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
        IScriptNetwork.sendToServer(new RequestEventGraphsPacket());
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, Component.literal("Graph Name"));
        nameInputBox.setMaxLength(64);
        if (mode.equals("rename") && oldId != null) {
            EventGraphData oldGraph = EventGraphManager.get(level, oldId);
            nameInputBox.setValue(oldGraph != null ? oldGraph.getName() : oldId);
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
        String mode = nameDialogMode;
        String oldId = renameOldId;
        closeNameDialog();
        if (name.isEmpty()) return;

        String id = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (id.isEmpty()) return;

        if (mode.equals("create")) {
            EventGraphData graph = new EventGraphData();
            graph.setId(id);
            graph.setName(name);
            ScriptNodeData start = new ScriptNodeData();
            start.setId("start");
            start.setType(ScriptNodeType.START);
            start.setParam("eventType", "TICK");
            start.setParam("label", "Event");
            start.setX(200);
            start.setY(200);
            graph.addNode(start);
            graph.setStartNodeId("start");
            EventGraphManager.add(level, graph);
            saveGraph();
            switchToGraph(id);
        } else if (mode.equals("rename") && oldId != null) {
            EventGraphData oldGraph = EventGraphManager.get(level, oldId);
            if (oldGraph != null) {
                EventGraphData newGraph = oldGraph.copy();
                newGraph.setId(id);
                newGraph.setName(name);
                EventGraphManager.remove(level, oldId);
                EventGraphManager.add(level, newGraph);
                EventGraphManager.removeClientCache(oldId);
                EventGraphManager.putClientCache(id, newGraph);
                if (selectedGraphId != null && selectedGraphId.equals(oldId)) {
                    selectedGraphId = id;
                    currentGraph = newGraph;
                }
                saveGraph();
                if (selectedGraphId != null && selectedGraphId.equals(id)) {
                    switchToGraph(id);
                }
            }
        }
    }

    private void switchToGraph(String id) {
        if (selectedGraphId != null && currentGraph != null) {
            IScriptNetwork.sendToServer(new SaveEventGraphPacket(currentGraph));
        }
        selectedGraphId = id;
        selectedNodeId = null;
        draggingNodeId = null;
        cancelConnection();
        showContextMenu = false;
        showItemContextMenu = false;
        showNodeTypeMenu = false;
        closeNodeEditor();

        currentGraph = EventGraphManager.getClientCache().get(id);
        if (currentGraph == null) {
            currentGraph = EventGraphManager.get(level, id);
        }
        if (currentGraph == null) {
            currentGraph = new EventGraphData();
            currentGraph.setId(id);
            currentGraph.setName(id);
            ScriptNodeData start = new ScriptNodeData();
            start.setId("start");
            start.setType(ScriptNodeType.START);
            start.setParam("eventType", "TICK");
            start.setParam("label", "Event");
            start.setX(200);
            start.setY(200);
            currentGraph.addNode(start);
            currentGraph.setStartNodeId("start");
            EventGraphManager.putClientCache(id, currentGraph);
        }
        canvasX = 0;
        canvasY = 0;
        zoom = 1.0;
    }

    private void addNode(String typeStr) {
        if (currentGraph == null) return;
        String nodeId = "node_" + System.currentTimeMillis();
        ScriptNodeData node = new ScriptNodeData();
        node.setId(nodeId);
        switch (typeStr) {
            case "event" -> {
                node.setType(ScriptNodeType.START);
                node.setParam("eventType", "TICK");
                node.setParam("label", "Event");
            }
            case "trigger" -> {
                node.setType(ScriptNodeType.TRIGGER);
                node.setParam("eventType", "TICK");
            }
            case "condition" -> {
                node.setType(ScriptNodeType.IF);
                node.setParam("condition", "true");
            }
            case "action" -> {
                node.setType(ScriptNodeType.SCRIPT_JS);
                node.setParam("script", "");
            }
            case "delay" -> {
                node.setType(ScriptNodeType.DELAY);
                node.setParam("ticks", "20");
            }
            case "random" -> {
                node.setType(ScriptNodeType.RANDOM);
                node.setParam("branches", "2");
            }
            case "loop" -> {
                node.setType(ScriptNodeType.LOOP);
                node.setParam("count", "3");
            }
            case "stop" -> {
                node.setType(ScriptNodeType.STOP);
            }
            default -> node.setType(ScriptNodeType.SCRIPT_JS);
        }
        node.setX((int) (canvasX + 200));
        node.setY((int) (canvasY + 200));
        currentGraph.addNode(node);
        saveGraph();
    }

    private void saveGraph() {
        if (currentGraph == null || selectedGraphId == null) return;
        EventGraphManager.putClientCache(selectedGraphId, currentGraph);
        IScriptNetwork.sendToServer(new SaveEventGraphPacket(currentGraph));
    }

    private void deleteNode(String nodeId) {
        if (currentGraph == null) return;
        currentGraph.removeNode(nodeId);
        if (selectedNodeId != null && selectedNodeId.equals(nodeId)) selectedNodeId = null;
        if (currentGraph.getStartNodeId().equals(nodeId)) currentGraph.setStartNodeId("");
        for (ScriptNodeData n : currentGraph.getNodes().values()) {
            n.getConnections().removeIf(c -> c.getTargetNodeId().equals(nodeId));
        }
        saveGraph();
    }

    private void cancelConnection() {
        connectingFromId = null;
        connectingSlot = -1;
    }

    private void openNodeEditor(ScriptNodeData node) {
        showNodeEditor = true;
        editingNode = node;
        nodeEditorY = this.parent.height / 2 - 80;
        paramEditBoxes.clear();
        scriptMultiLineBox = null;

        int yOffset = nodeEditorY + 20;
        String[] params = getNodeParams(node.getType());
        for (String key : params) {
            if (key.equals("script") && node.getType() == ScriptNodeType.SCRIPT_JS) {
                MultiLineEditBox box = new MultiLineEditBox(this.minecraft.font, 0, 0, 180, 120, Component.literal(key), Component.empty());
                box.setValue(node.getParam(key));
                box.setOnValueChanged(() -> {});
                parent.addWidget(box);
                scriptMultiLineBox = box;
            } else {
                EditBox box = new EditBox(this.minecraft.font, 0, 0, 180, 18, Component.literal(key));
                box.setValue(node.getParam(key));
                box.setMaxLength(512);
                parent.addWidget(box);
                paramEditBoxes.add(box);
            }
            yOffset += 40;
        }
        parent.setFocusedWidget(paramEditBoxes.isEmpty() ? (scriptMultiLineBox != null ? scriptMultiLineBox : null) : paramEditBoxes.get(0));
    }

    private String[] getNodeParams(ScriptNodeType type) {
        return switch (type) {
            case START -> new String[]{"eventType", "label"};
            case TRIGGER -> new String[]{"eventType"};
            case IF -> new String[]{"condition"};
            case SCRIPT_JS -> new String[]{"script"};
            case DELAY -> new String[]{"ticks"};
            case RANDOM -> new String[]{"branches"};
            case LOOP -> new String[]{"count"};
            default -> new String[]{};
        };
    }

    private void closeNodeEditor() {
        if (editingNode != null) {
            String[] params = getNodeParams(editingNode.getType());
            for (int i = 0; i < params.length; i++) {
                if (params[i].equals("script") && scriptMultiLineBox != null) {
                    editingNode.setParam(params[i], scriptMultiLineBox.getValue());
                } else if (i < paramEditBoxes.size()) {
                    editingNode.setParam(params[i], paramEditBoxes.get(i).getValue());
                }
            }
            saveGraph();
        }
        showNodeEditor = false;
        for (EditBox box : paramEditBoxes) {
            box.setVisible(false);
            parent.removeEditorWidget(box);
        }
        paramEditBoxes.clear();
        if (scriptMultiLineBox != null) {
            scriptMultiLineBox.setVisible(false);
            parent.removeEditorWidget(scriptMultiLineBox);
            scriptMultiLineBox = null;
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
            EventGraphManager.remove(level, confirmDialogId);
            EventGraphManager.removeClientCache(confirmDialogId);
            if (selectedGraphId != null && selectedGraphId.equals(confirmDialogId)) {
                selectedGraphId = null;
                currentGraph = null;
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
        EventGraphData source = EventGraphManager.get(level, sourceId);
        if (source == null) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (EventGraphManager.get(level, newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }

        EventGraphData copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (Copy)");
        EventGraphManager.add(level, copy);
        switchToGraph(newId);
    }

    private void duplicateItem(String id) {
        EventGraphData source = EventGraphManager.get(level, id);
        if (source == null) return;

        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (EventGraphManager.get(level, newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }

        EventGraphData copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (" + counter + ")");
        EventGraphManager.add(level, copy);
        switchToGraph(newId);
    }

    @Override
    public void removed() {
        closeNodeEditor();
        closeConfirmDialog();
        closeNameDialog();
        if (selectedGraphId != null && currentGraph != null) {
            IScriptNetwork.sendToServer(new SaveEventGraphPacket(currentGraph));
        }
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

    private int getOutputCount(ScriptNodeData node) {
        return switch (node.getType()) {
            case STOP -> 0;
            case IF -> 2;
            case RANDOM -> {
                try {
                    yield Math.max(2, Integer.parseInt(node.getParam("branches")));
                } catch (NumberFormatException e) {
                    yield 2;
                }
            }
            case LOOP -> 2;
            default -> 1;
        };
    }

    private String getOutputLabel(ScriptNodeData node, int slot) {
        return switch (node.getType()) {
            case IF -> slot == 0 ? "True" : "False";
            case LOOP -> slot == 0 ? "Body" : "Done";
            case RANDOM -> "Out " + (slot + 1);
            default -> "";
        };
    }

    private void renderConnection(GuiGraphics graphics, ScriptNodeData from, ScriptNodeData to, int sourceSlot, int leftX, int leftY) {
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        int outCount = getOutputCount(from);
        int x1 = worldToScreenX(from.getX() + nw, leftX);
        int y1 = worldToScreenY(from.getY() + getOutputSlotY(nh, headerH, sourceSlot, Math.max(1, outCount)), leftY);
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

    private void renderNode(GuiGraphics graphics, ScriptNodeData node, int leftX, int leftY, int mouseX, int mouseY) {
        int nx = worldToScreenX(node.getX(), leftX);
        int ny = worldToScreenY(node.getY(), leftY);
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();

        boolean hovered = mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh;
        boolean selected = node.getId().equals(selectedNodeId);
        boolean isStart = currentGraph != null && node.getId().equals(currentGraph.getStartNodeId());

        graphics.fill(nx + 2, ny + 2, nx + nw + 2, ny + nh + 2, 0x44000000);

        int bodyColor = selected ? 0xFF334466 : 0xFF1E1E28;
        graphics.fill(nx, ny, nx + nw, ny + nh, bodyColor);
        graphics.renderOutline(nx, ny, nw, nh, selected ? 0xFF55AAFF : (isStart ? 0xFF00D4AA : 0xFF444455));

        int headerColor = isStart ? 0xFF4488AA : getNodeHeaderColor(node.getType());
        graphics.fill(nx, ny, nx + nw, ny + headerH, headerColor);
        String title = getNodeTitle(node);
        String label = font.plainSubstrByWidth(title, nw - 8);
        graphics.drawString(font, label, nx + 4, ny + (headerH - 8) / 2, 0xFFFFFFFF);

        if (nh > 30) {
            String text = getNodeBodyText(node);
            if (text.length() > 30) text = text.substring(0, 30) + "...";
            text = font.plainSubstrByWidth(text, nw - 8);
            graphics.drawString(font, text, nx + 4, ny + headerH + 4, 0xFFAAAAAA);
        }

        int pinR = Math.max(3, (int) (4 * zoom));
        int inX = nx;
        int inY = ny + nh / 2;
        graphics.fill(inX - pinR, inY - pinR, inX + pinR, inY + pinR, 0xFF55FF55);
        graphics.renderOutline(inX - pinR, inY - pinR, pinR * 2, pinR * 2, 0xFF338833);

        int outCount = getOutputCount(node);
        if (outCount > 0) {
            for (int i = 0; i < outCount; i++) {
                int slotY = ny + getOutputSlotY(nh, headerH, i, outCount);
                int outX = nx + nw;
                boolean active = connectingFromId != null && connectingFromId.equals(node.getId()) && connectingSlot == i;
                int pinColor = active ? 0xFF55AAFF : 0xFFFF5555;
                graphics.fill(outX - pinR, slotY - pinR, outX + pinR, slotY + pinR, pinColor);
                graphics.renderOutline(outX - pinR, slotY - pinR, pinR * 2, pinR * 2, active ? 0xFF3366AA : 0xFFAA3333);
                if (zoom > 0.6 && nh > 40) {
                    String lbl = getOutputLabel(node, i);
                    if (!lbl.isEmpty()) {
                        graphics.drawString(font, lbl, outX - pinR - font.width(lbl) - 2, slotY - 3, 0xFFCCCCCC);
                    }
                }
            }
        }
    }

    private int getNodeHeaderColor(ScriptNodeType type) {
        return switch (type) {
            case START -> 0xFF4488AA;
            case TRIGGER -> 0xFFAA6622;
            case IF -> 0xFFAA8844;
            case SCRIPT_JS -> 0xFF44AA66;
            case DELAY -> 0xFFAA44AA;
            case RANDOM -> 0xFF44AAAA;
            case LOOP -> 0xFFAA44AA;
            case STOP -> 0xFFAA4444;
            default -> 0xFF2A2A3A;
        };
    }

    private String getNodeTitle(ScriptNodeData node) {
        return switch (node.getType()) {
            case START -> "Event";
            case TRIGGER -> "Trigger";
            case IF -> "Condition";
            case SCRIPT_JS -> "Action";
            case DELAY -> "Delay";
            case RANDOM -> "Random";
            case LOOP -> "Loop";
            case STOP -> "Stop";
            default -> node.getType().name();
        };
    }

    private String getNodeBodyText(ScriptNodeData node) {
        return switch (node.getType()) {
            case START, TRIGGER -> node.getParam("eventType");
            case IF -> node.getParam("condition");
            case SCRIPT_JS -> node.getParam("script");
            case DELAY -> node.getParam("ticks") + " ticks";
            case RANDOM -> node.getParam("branches") + " branches";
            case LOOP -> node.getParam("count") + " times";
            default -> "";
        };
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

            if (currentGraph != null) {
                boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
                graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? 0xFF2A3A4A : 0xFF1E1E28);
                graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
                graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
            btnY += btnSize + 6;

            boolean delHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, delHovered ? 0xFF4A2A2A : 0xFF281E1E);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, 0xFF444455);
            graphics.drawCenteredString(this.font, "\u2715", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, delHovered ? 0xFFFF5555 : 0xFFAA4444);

            graphics.fill(rightX, y, x + w, y + h, 0xFF1E1E28);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, 0xFF2A2A3A);
            graphics.drawString(this.font, "Events", rightX + 8, y + 6, 0xFF55AAFF);

            var graphs = EventGraphManager.getAll(level);
            List<String> ids = new ArrayList<>(graphs.keySet());
            Collections.sort(ids);

            int listH = h - 40;
            int listY = y + 20;
            int visible = Math.max(1, listH / ITEM_HEIGHT);

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(selectedGraphId);

                int bg = selected ? 0xFF334466 : (hovered ? 0xFF2A2A3A : 0x001E1E28);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? 0xFF55AAFF : 0xFFCCCCCC);
            }

            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? 0xFF2A3A2A : 0xFF1E281E);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, 0xFF444455);
            graphics.drawCenteredString(this.font, "+ New Event", rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, 0xFF55FF55);

            if (selectedGraphId != null && currentGraph != null) {
                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, 0xFF111118);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, 0xFF333344);

                RenderSystem.enableScissor(leftX * (int) this.minecraft.getWindow().getGuiScale(), (this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * (int) this.minecraft.getWindow().getGuiScale(), leftW * (int) this.minecraft.getWindow().getGuiScale(), leftH * (int) this.minecraft.getWindow().getGuiScale());

                renderGrid(graphics, leftX, leftY, leftW, leftH);

                for (ScriptNodeData node : currentGraph.getNodes().values()) {
                    for (ScriptNodeConnection conn : node.getConnections()) {
                        ScriptNodeData target = currentGraph.getNode(conn.getTargetNodeId());
                        if (target != null) {
                            renderConnection(graphics, node, target, conn.getSourceSlot(), leftX, leftY);
                        }
                    }
                }

                if (connectingFromId != null) {
                    ScriptNodeData from = currentGraph.getNode(connectingFromId);
                    if (from != null) {
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int outCount = getOutputCount(from);
                        int x1 = worldToScreenX(from.getX() + nw, leftX);
                        int y1 = worldToScreenY(from.getY() + getOutputSlotY(nh, headerH, connectingSlot, Math.max(1, outCount)), leftY);
                        int x2 = mouseX;
                        int y2 = mouseY;
                        int midX = (x1 + x2) / 2;
                        drawLine(graphics, x1, y1, midX, y1, 0xFF55AAFF);
                        drawLine(graphics, midX, y1, midX, y2, 0xFF55AAFF);
                        drawLine(graphics, midX, y2, x2, y2, 0xFF55AAFF);
                    }
                }

                for (ScriptNodeData node : currentGraph.getNodes().values()) {
                    renderNode(graphics, node, leftX, leftY, mouseX, mouseY);
                }

                RenderSystem.disableScissor();

                graphics.drawString(font, String.format("%.0f%%", zoom * 100), leftX + 4, leftY + 4, 0xFF666666);
            } else {
                graphics.drawCenteredString(this.font, "Select an event graph from the list", leftX + leftW / 2, y + h / 2, 0xFF555566);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, 0xFF16161E);
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

        if (showNameDialog) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 80;
            int dx = cx - dw / 2;
            int dy = nameDialogY;

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
            graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
            graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
            graphics.drawCenteredString(this.font, nameDialogMode.equals("rename") ? "Rename Event Graph" : "New Event Graph Name", cx, dy + 6, 0xFF55AAFF);

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

        if (showNodeEditor && editingNode != null) {
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = nodeEditorY;

            String[] params = getNodeParams(editingNode.getType());
            int editorH = 30 + params.length * 40;
            if (editingNode.getType() == ScriptNodeType.SCRIPT_JS && scriptMultiLineBox != null) {
                editorH = 30 + 40 + 120;
            }

            graphics.fill(dx - 10, dy - 10, dx + 200, dy + editorH, 0xFF2A2A3A);
            graphics.renderOutline(dx - 10, dy - 10, 210, editorH + 20, 0xFF6688FF);
            graphics.drawString(font, "Edit Node (ESC/Enter to save)", dx, dy, 0xFFFFFFFF);

            int py = dy + 20;
            for (int i = 0; i < params.length && i < paramEditBoxes.size(); i++) {
                graphics.drawString(font, params[i] + ":", dx, py, 0xFFAAAAAA);
                paramEditBoxes.get(i).setX(dx);
                paramEditBoxes.get(i).setY(py + 12);
                paramEditBoxes.get(i).setVisible(true);
                py += 40;
            }

            if (scriptMultiLineBox != null) {
                graphics.drawString(font, "script:", dx, py, 0xFFAAAAAA);
                scriptMultiLineBox.setX(dx);
                scriptMultiLineBox.setY(py + 12);
                scriptMultiLineBox.setVisible(true);
            }
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

    private void runGraph() {
        if (selectedGraphId == null || currentGraph == null) return;
        saveGraph();
        IScriptNetwork.sendToServer(new RunEventGraphPacket(selectedGraphId));
    }

    private void renderNodeTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] types = {"Event", "Trigger", "Condition", "Action", "Delay", "Random", "Loop", "Stop"};
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

            String[] params = editingNode != null ? getNodeParams(editingNode.getType()) : new String[]{};
            int editorH = 30 + params.length * 40;
            if (editingNode != null && editingNode.getType() == ScriptNodeType.SCRIPT_JS) {
                editorH = 30 + 40 + 120;
            }

            if (mouseX >= dx - 10 && mouseX <= dx + 200 && mouseY >= dy - 10 && mouseY <= dy + editorH) {
                for (EditBox box : paramEditBoxes) {
                    if (box != null && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth() && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight()) {
                        parent.setFocusedWidget(box);
                        return box.mouseClicked(mouseX, mouseY, button);
                    }
                }
                if (scriptMultiLineBox != null && mouseX >= scriptMultiLineBox.getX() && mouseX <= scriptMultiLineBox.getX() + scriptMultiLineBox.getWidth() && mouseY >= scriptMultiLineBox.getY() && mouseY <= scriptMultiLineBox.getY() + scriptMultiLineBox.getHeight()) {
                    parent.setFocusedWidget(scriptMultiLineBox);
                    return scriptMultiLineBox.mouseClicked(mouseX, mouseY, button);
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
                    ScriptNodeData node = currentGraph != null ? currentGraph.getNode(contextMenuNodeId) : null;
                    if (node != null) openNodeEditor(node);
                    showContextMenu = false;
                    return true;
                }
                y += 22;
                if (mouseY >= y && mouseY <= y + 20) {
                    if (currentGraph != null) {
                        currentGraph.setStartNodeId(contextMenuNodeId);
                        saveGraph();
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
            String[] types = {"event", "trigger", "condition", "action", "delay", "random", "loop", "stop"};
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
            if (selectedGraphId != null && currentGraph != null) {
                runGraph();
            }
            return true;
        }
        btnY += btnSize + 6;

        if (currentGraph != null) {
            if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
                showNodeTypeMenu = true;
                nodeTypeMenuX = (int) mouseX;
                nodeTypeMenuY = (int) mouseY;
                return true;
            }
        }
        btnY += btnSize + 6;

        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            if (selectedGraphId != null) {
                openConfirmDialog("delete", selectedGraphId);
            }
            return true;
        }

        var graphs = EventGraphManager.getAll(level);
        List<String> ids = new ArrayList<>(graphs.keySet());
        Collections.sort(ids);

        int listH = h - 40;
        int listY = y + 20;
        int visible = Math.max(1, listH / ITEM_HEIGHT);

        for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (button == 1) {
                    openItemContextMenu(id, (int) mouseX, (int) mouseY);
                    return true;
                }
                if (!id.equals(selectedGraphId)) {
                    switchToGraph(id);
                }
                return true;
            }
        }

        if (button == 1 && mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= listY && mouseY <= listY + listH) {
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            if (canPaste) {
                openItemContextMenu("", (int) mouseX, (int) mouseY);
            }
            return true;
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

        if (currentGraph != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            if (button == 0) {
                List<ScriptNodeData> nodes = new ArrayList<>(currentGraph.getNodes().values());
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    ScriptNodeData node = nodes.get(i);
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
                            ScriptNodeData from = currentGraph.getNode(connectingFromId);
                            if (from != null) {
                                ScriptNodeConnection conn = new ScriptNodeConnection();
                                conn.setTargetNodeId(node.getId());
                                conn.setSourceSlot(connectingSlot);
                                conn.setSourceNodeId(from.getId());
                                from.getConnections().add(conn);
                                saveGraph();
                            }
                            cancelConnection();
                            return true;
                        }
                        return true;
                    }

                    int outCount = getOutputCount(node);
                    for (int j = 0; j < outCount; j++) {
                        int slotY = ny + getOutputSlotY(nh, headerH, j, outCount);
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
                for (ScriptNodeData node : currentGraph.getNodes().values()) {
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
            if (scriptMultiLineBox != null && scriptMultiLineBox.isFocused()) {
                return scriptMultiLineBox.charTyped(codePoint, modifiers);
            }
            for (EditBox box : paramEditBoxes) {
                if (box != null && box.isFocused()) {
                    return box.charTyped(codePoint, modifiers);
                }
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
            if (scriptMultiLineBox != null && scriptMultiLineBox.isFocused()) {
                return scriptMultiLineBox.keyPressed(keyCode, scanCode, modifiers);
            }
            for (EditBox box : paramEditBoxes) {
                if (box != null && box.isFocused()) {
                    return box.keyPressed(keyCode, scanCode, modifiers);
                }
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
            var graphs = EventGraphManager.getAll(level);
            List<String> ids = new ArrayList<>(graphs.keySet());
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

        if (currentGraph != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
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
            if (draggingNodeId != null && currentGraph != null) {
                ScriptNodeData node = currentGraph.getNode(draggingNodeId);
                if (node != null && (node.getX() != dragStartNodeX || node.getY() != dragStartNodeY)) {
                    saveGraph();
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
        if (draggingNodeId != null && currentGraph != null) {
            ScriptNodeData node = currentGraph.getNode(draggingNodeId);
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