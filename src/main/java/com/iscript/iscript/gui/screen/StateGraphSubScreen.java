package com.iscript.iscript.gui.screen;

import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveStateMachinePacket;
import com.iscript.iscript.data.state.ClientMachineData;
import com.iscript.iscript.data.state.ClientNodeData;
import com.iscript.iscript.data.state.ClientTransitionData;
import com.iscript.iscript.data.state.StateMachineManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class StateGraphSubScreen extends DashboardScreen.SubScreen {
    private static final int NODE_WIDTH = 140;
    private static final int NODE_HEIGHT = 36;
    private static final int NODE_HEADER = 14;
    private static final int GRID_SIZE = 20;
    private static final int PORT_SIZE = 8;

    private String machineId;
    private String machineName;

    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<GraphEdge> edges = new ArrayList<>();

    private GraphNode draggedNode = null;
    private GraphNode selectedNode = null;
    private GraphEdge selectedEdge = null;
    private int dragOffsetX, dragOffsetY;

    private int cameraX = 0;
    private int cameraY = 0;
    private boolean panning = false;
    private int panStartX, panStartY;
    private int cameraStartX, cameraStartY;

    private boolean connecting = false;
    private GraphNode connectSource = null;
    private int connectMouseX, connectMouseY;

    private boolean showNodeDialog = false;
    private EditBox nodeNameBox = null;
    private EditBox nodeIdBox = null;
    private int nodeDialogY = 0;
    private String nodeDialogMode = "";
    private String editNodeId = null;

    private boolean showConfirmDialog = false;
    private int confirmDialogY = 0;
    private String confirmDialogAction = "";
    private String confirmDialogId = null;

    private boolean showEdgeDialog = false;
    private int edgeDialogY = 0;
    private GraphEdge editEdge = null;
    private EditBox edgeTargetBox = null;
    private EditBox edgeConditionBox = null;

    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public StateGraphSubScreen(DashboardScreen parent, ServerLevel level, String machineId, String machineName) {
        super(parent, level);
        this.machineId = machineId;
        this.machineName = machineName;
    }

    @Override
    public void init() {
        showNodeDialog = false;
        showEdgeDialog = false;
        showConfirmDialog = false;
        clearNodeDialog();
        clearEdgeDialog();
        loadMachine();
    }

    private void loadMachine() {
        nodes.clear();
        edges.clear();

        ClientMachineData data = StateMachineManager.getClientMachineData(machineId);
        if (data == null) return;

        for (ClientNodeData nodeData : data.nodes) {
            GraphNode n = new GraphNode();
            n.id = nodeData.id;
            n.name = nodeData.name;
            n.color = nodeData.color;
            n.x = nodeData.posX;
            n.y = nodeData.posY;
            nodes.add(n);
        }

        for (ClientNodeData nodeData : data.nodes) {
            GraphNode from = findNode(nodeData.id);
            if (from == null) continue;
            for (ClientTransitionData trans : nodeData.transitions) {
                GraphNode to = findNode(trans.targetNode);
                if (to == null) continue;
                GraphEdge e = new GraphEdge();
                e.from = from;
                e.to = to;
                e.auto = trans.auto;
                edges.add(e);
                from.outputs.add(e);
                to.inputs.add(e);
            }
        }

        if (!nodes.isEmpty()) {
            int minX = nodes.stream().mapToInt(n -> n.x).min().orElse(0);
            int minY = nodes.stream().mapToInt(n -> n.y).min().orElse(0);
            cameraX = minX - 100;
            cameraY = minY - 100;
        }
    }

    private GraphNode findNode(String id) {
        for (GraphNode n : nodes) if (n.id.equals(id)) return n;
        return null;
    }

    private void saveMachine() {
        StringBuilder nodesJson = new StringBuilder();
        nodesJson.append("[");
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode n = nodes.get(i);
            if (i > 0) nodesJson.append(",");
            nodesJson.append("{");
            nodesJson.append("\"id\":\"").append(escape(n.id)).append("\",");
            nodesJson.append("\"name\":\"").append(escape(n.name)).append("\",");
            nodesJson.append("\"color\":").append(n.color).append(",");
            nodesJson.append("\"posX\":").append(n.x).append(",");
            nodesJson.append("\"posY\":").append(n.y).append(",");
            nodesJson.append("\"transitions\":[");
            for (int j = 0; j < n.outputs.size(); j++) {
                GraphEdge e = n.outputs.get(j);
                if (j > 0) nodesJson.append(",");
                nodesJson.append("{");
                nodesJson.append("\"targetNode\":\"").append(escape(e.to.id)).append("\",");
                nodesJson.append("\"auto\":").append(e.auto);
                nodesJson.append("}");
            }
            nodesJson.append("]}");
        }
        nodesJson.append("]");

        IScriptNetwork.sendToServer(new SaveStateMachinePacket(machineId, machineName, getEntryNode(), nodesJson.toString()));
    }

    private String getEntryNode() {
        if (nodes.isEmpty()) return "";
        for (GraphNode n : nodes) {
            if (n.inputs.isEmpty()) return n.id;
        }
        return nodes.get(0).id;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void clearNodeDialog() {
        if (nodeNameBox != null) {
            parent.removeEditorWidget(nodeNameBox);
            nodeNameBox = null;
        }
        if (nodeIdBox != null) {
            parent.removeEditorWidget(nodeIdBox);
            nodeIdBox = null;
        }
    }

    private void clearEdgeDialog() {
        if (edgeTargetBox != null) {
            parent.removeEditorWidget(edgeTargetBox);
            edgeTargetBox = null;
        }
        if (edgeConditionBox != null) {
            parent.removeEditorWidget(edgeConditionBox);
            edgeConditionBox = null;
        }
    }

    private void openNodeDialog(String mode, GraphNode node) {
        nodeDialogMode = mode;
        editNodeId = node != null ? node.id : null;
        showNodeDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        nodeDialogY = this.parent.height / 2 - 60;

        nodeIdBox = new EditBox(this.minecraft.font, cx - 100, nodeDialogY + 20, 200, 20, Component.literal("Node ID"));
        nodeIdBox.setMaxLength(64);
        nodeIdBox.setValue(node != null ? node.id : "");
        parent.addWidget(nodeIdBox);

        nodeNameBox = new EditBox(this.minecraft.font, cx - 100, nodeDialogY + 46, 200, 20, Component.literal("Node Name"));
        nodeNameBox.setMaxLength(64);
        nodeNameBox.setValue(node != null ? node.name : "");
        parent.addWidget(nodeNameBox);

        parent.setFocusedWidget(nodeIdBox);
    }

    private void confirmNodeDialog() {
        if (nodeIdBox == null || nodeNameBox == null) return;
        String id = nodeIdBox.getValue().trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        String name = nodeNameBox.getValue().trim();
        if (id.isEmpty()) {
            closeNodeDialog();
            return;
        }

        if (nodeDialogMode.equals("create")) {
            if (findNode(id) != null) {
                closeNodeDialog();
                return;
            }
            GraphNode n = new GraphNode();
            n.id = id;
            n.name = name.isEmpty() ? id : name;
            n.color = 0xFF4488AA;
            n.x = cameraX + (this.parent.width - DashboardScreen.SIDEBAR_WIDTH) / 2 - NODE_WIDTH / 2;
            n.y = cameraY + (this.parent.height - DashboardScreen.TOPBAR_HEIGHT) / 2 - NODE_HEIGHT / 2;
            nodes.add(n);
        } else if (nodeDialogMode.equals("edit") && editNodeId != null) {
            GraphNode n = findNode(editNodeId);
            if (n != null) {
                if (!n.id.equals(id) && findNode(id) != null) {
                    closeNodeDialog();
                    return;
                }
                String oldId = n.id;
                n.id = id;
                n.name = name.isEmpty() ? id : name;
                for (GraphEdge e : edges) {
                    if (e.from != null && e.from.id.equals(oldId)) e.from = n;
                    if (e.to != null && e.to.id.equals(oldId)) e.to = n;
                }
            }
        }

        closeNodeDialog();
        saveMachine();
    }

    private void closeNodeDialog() {
        showNodeDialog = false;
        editNodeId = null;
        clearNodeDialog();
    }

    private void deleteNode(GraphNode node) {
        if (node == null) return;
        edges.removeAll(node.outputs);
        edges.removeAll(node.inputs);
        for (GraphNode n : nodes) {
            n.outputs.removeIf(e -> e.to == node);
            n.inputs.removeIf(e -> e.from == node);
        }
        nodes.remove(node);
        if (selectedNode == node) selectedNode = null;
        saveMachine();
    }

    private void openEdgeDialog(GraphEdge edge) {
        editEdge = edge;
        showEdgeDialog = true;
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        edgeDialogY = this.parent.height / 2 - 50;

        edgeTargetBox = new EditBox(this.minecraft.font, cx - 100, edgeDialogY + 20, 200, 20, Component.literal("Target Node"));
        edgeTargetBox.setMaxLength(64);
        edgeTargetBox.setValue(edge.to != null ? edge.to.id : "");
        parent.addWidget(edgeTargetBox);

        edgeConditionBox = new EditBox(this.minecraft.font, cx - 100, edgeDialogY + 46, 200, 20, Component.literal("Condition"));
        edgeConditionBox.setMaxLength(128);
        edgeConditionBox.setValue(edge.auto ? "auto" : "manual");
        parent.addWidget(edgeConditionBox);

        parent.setFocusedWidget(edgeTargetBox);
    }

    private void confirmEdgeDialog() {
        if (editEdge == null || edgeTargetBox == null) return;
        String targetId = edgeTargetBox.getValue().trim();
        GraphNode target = findNode(targetId);
        if (target == null || target == editEdge.from) {
            closeEdgeDialog();
            return;
        }

        editEdge.to.inputs.remove(editEdge);
        editEdge.to = target;
        target.inputs.add(editEdge);
        editEdge.auto = "auto".equalsIgnoreCase(edgeConditionBox != null ? edgeConditionBox.getValue().trim() : "");

        closeEdgeDialog();
        saveMachine();
    }

    private void closeEdgeDialog() {
        showEdgeDialog = false;
        editEdge = null;
        clearEdgeDialog();
    }

    private void deleteEdge(GraphEdge edge) {
        if (edge == null) return;
        edge.from.outputs.remove(edge);
        edge.to.inputs.remove(edge);
        edges.remove(edge);
        if (selectedEdge == edge) selectedEdge = null;
        saveMachine();
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
        if ("delete_node".equals(confirmDialogAction)) {
            GraphNode n = findNode(confirmDialogId);
            if (n != null) deleteNode(n);
        } else if ("delete_edge".equals(confirmDialogAction)) {
        }
        closeConfirmDialog();
    }

    @Override
    public void removed() {
        saveMachine();
        closeNodeDialog();
        closeEdgeDialog();
        closeConfirmDialog();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        graphics.fill(x, y, x + w, y + h, 0xFF12121A);

        int originX = x - cameraX;
        int originY = y - cameraY;

        for (int gx = originX % GRID_SIZE; gx < w; gx += GRID_SIZE) {
            graphics.fill(x + gx, y, x + gx + 1, y + h, 0xFF1A1A24);
        }
        for (int gy = originY % GRID_SIZE; gy < h; gy += GRID_SIZE) {
            graphics.fill(x, y + gy, x + w, y + gy + 1, 0xFF1A1A24);
        }

        for (GraphEdge e : edges) {
            if (e.from == null || e.to == null) continue;
            int x1 = originX + e.from.x + NODE_WIDTH / 2;
            int y1 = originY + e.from.y + NODE_HEIGHT;
            int x2 = originX + e.to.x + NODE_WIDTH / 2;
            int y2 = originY + e.to.y;

            int color = e == selectedEdge ? 0xFF55AAFF : (e.auto ? 0xFF4488AA : 0xFF888888);
            drawArrow(graphics, x1, y1, x2, y2, color);
        }

        if (connecting && connectSource != null) {
            int x1 = originX + connectSource.x + NODE_WIDTH / 2;
            int y1 = originY + connectSource.y + NODE_HEIGHT;
            drawArrow(graphics, x1, y1, mouseX, mouseY, 0xFF55AAFF);
        }

        for (GraphNode n : nodes) {
            int nx = originX + n.x;
            int ny = originY + n.y;
            if (nx + NODE_WIDTH < x || nx > x + w || ny + NODE_HEIGHT < y || ny > y + h) continue;

            boolean hovered = mouseX >= nx && mouseX <= nx + NODE_WIDTH && mouseY >= ny && mouseY <= ny + NODE_HEIGHT;
            boolean selected = n == selectedNode;

            int bg = n.color;
            int border = selected ? 0xFFFFFFFF : (hovered ? 0xFFCCCCCC : 0xFF2A2A3A);

            graphics.fill(nx, ny, nx + NODE_WIDTH, ny + NODE_HEIGHT, 0xFF16161E);
            graphics.renderOutline(nx, ny, NODE_WIDTH, NODE_HEIGHT, border);
            graphics.fill(nx + 1, ny + 1, nx + NODE_WIDTH - 1, ny + NODE_HEADER, bg);

            String title = n.name.isEmpty() ? n.id : n.name;
            if (title.length() > 18) title = title.substring(0, 16) + "..";
            graphics.drawString(this.font, title, nx + 4, ny + 3, 0xFFFFFFFF);

            graphics.drawString(this.font, n.id, nx + 4, ny + NODE_HEADER + 4, 0xFF888899);

            int inX = nx + NODE_WIDTH / 2 - PORT_SIZE / 2;
            int inY = ny - PORT_SIZE / 2;
            graphics.fill(inX, inY, inX + PORT_SIZE, inY + PORT_SIZE, 0xFF55AAFF);

            int outX = nx + NODE_WIDTH / 2 - PORT_SIZE / 2;
            int outY = ny + NODE_HEIGHT - PORT_SIZE / 2;
            graphics.fill(outX, outY, outX + PORT_SIZE, outY + PORT_SIZE, 0xFFAA5555);
        }

        graphics.fill(x, y, x + w, y + 24, 0xFF1A1A24);
        graphics.renderOutline(x, y, w, 24, 0xFF2A2A3A);
        graphics.drawString(this.font, "Graph: " + machineName, x + 8, y + 6, 0xFF55AAFF);

        String hint = "LMB=select/drag  RMB=node menu  Double-click=edit  Shift+LMB=connect  Space=new node  Del=delete";
        graphics.drawString(this.font, hint, x + 8, y + h - 12, 0xFF555566);

        if (showNodeDialog) {
            renderNodeDialog(graphics, mouseX, mouseY, x, y, w, h);
        }
        if (showEdgeDialog) {
            renderEdgeDialog(graphics, mouseX, mouseY, x, y, w, h);
        }
        if (showConfirmDialog) {
            renderConfirmDialog(graphics, mouseX, mouseY, x, y, w, h);
        }
    }

    private void drawArrow(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.renderOutline(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1, color);
        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;
        graphics.fill(midX - 2, midY - 2, midX + 3, midY + 3, color);
    }

    private void renderNodeDialog(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 110;
        int dx = cx - dw / 2;
        int dy = nodeDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF55AAFF);
        graphics.drawCenteredString(this.font, nodeDialogMode.equals("edit") ? "Edit Node" : "New Node", cx, dy + 6, 0xFF55AAFF);

        graphics.drawString(this.font, "ID:", dx + 10, dy + 22, 0xFF888899);
        graphics.drawString(this.font, "Name:", dx + 10, dy + 48, 0xFF888899);

        if (nodeIdBox != null) {
            nodeIdBox.setX(cx - 100);
            nodeIdBox.setY(dy + 20);
        }
        if (nodeNameBox != null) {
            nodeNameBox.setX(cx - 100);
            nodeNameBox.setY(dy + 46);
        }

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 78 && mouseY <= dy + 100;
        graphics.fill(cx - 50, dy + 78, cx - 2, dy + 100, okHovered ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 78, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 83, okHovered ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 78 && mouseY <= dy + 100;
        graphics.fill(cx + 2, dy + 78, cx + 50, dy + 100, cancelHovered ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 78, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 83, cancelHovered ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderEdgeDialog(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 110;
        int dx = cx - dw / 2;
        int dy = edgeDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFFAA8822);
        graphics.drawCenteredString(this.font, "Edit Transition", cx, dy + 6, 0xFFAA8822);

        graphics.drawString(this.font, "Target:", dx + 10, dy + 22, 0xFF888899);
        graphics.drawString(this.font, "Mode:", dx + 10, dy + 48, 0xFF888899);

        if (edgeTargetBox != null) {
            edgeTargetBox.setX(cx - 100);
            edgeTargetBox.setY(dy + 20);
        }
        if (edgeConditionBox != null) {
            edgeConditionBox.setX(cx - 100);
            edgeConditionBox.setY(dy + 46);
        }

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 78 && mouseY <= dy + 100;
        graphics.fill(cx - 50, dy + 78, cx - 2, dy + 100, okHovered ? 0xFF2A4A2A : 0xFF1E281E);
        graphics.renderOutline(cx - 50, dy + 78, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Save", cx - 26, dy + 83, okHovered ? 0xFF55FF55 : 0xFF44AA44);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 78 && mouseY <= dy + 100;
        graphics.fill(cx + 2, dy + 78, cx + 50, dy + 100, cancelHovered ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx + 2, dy + 78, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 83, cancelHovered ? 0xFFFF5555 : 0xFFAA4444);
    }

    private void renderConfirmDialog(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int dw = 220;
        int dh = 70;
        int dx = cx - dw / 2;
        int dy = confirmDialogY;

        graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, 0xCC000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF1E1E28);
        graphics.renderOutline(dx, dy, dw, dh, 0xFFFF5555);
        graphics.drawCenteredString(this.font, "Delete node?", cx, dy + 8, 0xFFFF5555);

        boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx - 50, dy + 38, cx - 2, dy + 60, okHovered ? 0xFF4A2A2A : 0xFF281E1E);
        graphics.renderOutline(cx - 50, dy + 38, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Delete", cx - 26, dy + 43, okHovered ? 0xFFFF5555 : 0xFFAA4444);

        boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
        graphics.fill(cx + 2, dy + 38, cx + 50, dy + 60, cancelHovered ? 0xFF2A3A4A : 0xFF1E1E28);
        graphics.renderOutline(cx + 2, dy + 38, 48, 22, 0xFF444455);
        graphics.drawCenteredString(this.font, "Cancel", cx + 26, dy + 43, cancelHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNodeDialog) {
            return handleNodeDialogClick(mouseX, mouseY);
        }
        if (showEdgeDialog) {
            return handleEdgeDialogClick(mouseX, mouseY);
        }
        if (showConfirmDialog) {
            return handleConfirmDialogClick(mouseX, mouseY);
        }

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int h = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int originX = x - cameraX;
        int originY = y - cameraY;

        if (button == 0) {
            if (isShiftDown()) {
                GraphNode portNode = getPortNodeAt((int) mouseX, (int) mouseY, originX, originY, false);
                if (portNode != null) {
                    connecting = true;
                    connectSource = portNode;
                    connectMouseX = (int) mouseX;
                    connectMouseY = (int) mouseY;
                    return true;
                }
            }

            GraphNode clicked = getNodeAt((int) mouseX, (int) mouseY, originX, originY);
            if (clicked != null) {
                if (selectedNode == clicked && System.currentTimeMillis() - clicked.lastClickTime < 400) {
                    openNodeDialog("edit", clicked);
                    clicked.lastClickTime = 0;
                    return true;
                }
                clicked.lastClickTime = System.currentTimeMillis();
                selectedNode = clicked;
                selectedEdge = null;
                draggedNode = clicked;
                dragOffsetX = (int) mouseX - (originX + clicked.x);
                dragOffsetY = (int) mouseY - (originY + clicked.y);
                return true;
            }

            GraphEdge edge = getEdgeAt((int) mouseX, (int) mouseY);
            if (edge != null) {
                selectedEdge = edge;
                selectedNode = null;
                return true;
            }

            selectedNode = null;
            selectedEdge = null;
            panning = true;
            panStartX = (int) mouseX;
            panStartY = (int) mouseY;
            cameraStartX = cameraX;
            cameraStartY = cameraY;
            return true;
        }

        if (button == 1) {
            GraphNode clicked = getNodeAt((int) mouseX, (int) mouseY, originX, originY);
            if (clicked != null) {
                selectedNode = clicked;
                return true;
            }
            GraphEdge edge = getEdgeAt((int) mouseX, (int) mouseY);
            if (edge != null) {
                openEdgeDialog(edge);
                return true;
            }
        }

        return false;
    }

    private boolean handleNodeDialogClick(double mouseX, double mouseY) {
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        int dy = nodeDialogY;

        if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 78 && mouseY <= dy + 100) {
            confirmNodeDialog();
            return true;
        }
        if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 78 && mouseY <= dy + 100) {
            closeNodeDialog();
            return true;
        }
        if (nodeIdBox != null && mouseX >= nodeIdBox.getX() && mouseX <= nodeIdBox.getX() + nodeIdBox.getWidth() &&
                mouseY >= nodeIdBox.getY() && mouseY <= nodeIdBox.getY() + nodeIdBox.getHeight()) {
            parent.setFocusedWidget(nodeIdBox);
            return nodeIdBox.mouseClicked(mouseX, mouseY, 0);
        }
        if (nodeNameBox != null && mouseX >= nodeNameBox.getX() && mouseX <= nodeNameBox.getX() + nodeNameBox.getWidth() &&
                mouseY >= nodeNameBox.getY() && mouseY <= nodeNameBox.getY() + nodeNameBox.getHeight()) {
            parent.setFocusedWidget(nodeNameBox);
            return nodeNameBox.mouseClicked(mouseX, mouseY, 0);
        }
        return true;
    }

    private boolean handleEdgeDialogClick(double mouseX, double mouseY) {
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int w = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int cx = x + w / 2;
        int dy = edgeDialogY;

        if (mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 78 && mouseY <= dy + 100) {
            confirmEdgeDialog();
            return true;
        }
        if (mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 78 && mouseY <= dy + 100) {
            closeEdgeDialog();
            return true;
        }
        if (edgeTargetBox != null && mouseX >= edgeTargetBox.getX() && mouseX <= edgeTargetBox.getX() + edgeTargetBox.getWidth() &&
                mouseY >= edgeTargetBox.getY() && mouseY <= edgeTargetBox.getY() + edgeTargetBox.getHeight()) {
            parent.setFocusedWidget(edgeTargetBox);
            return edgeTargetBox.mouseClicked(mouseX, mouseY, 0);
        }
        if (edgeConditionBox != null && mouseX >= edgeConditionBox.getX() && mouseX <= edgeConditionBox.getX() + edgeConditionBox.getWidth() &&
                mouseY >= edgeConditionBox.getY() && mouseY <= edgeConditionBox.getY() + edgeConditionBox.getHeight()) {
            parent.setFocusedWidget(edgeConditionBox);
            return edgeConditionBox.mouseClicked(mouseX, mouseY, 0);
        }
        return true;
    }

    private boolean handleConfirmDialogClick(double mouseX, double mouseY) {
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (connecting && button == 0 && connectSource != null) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int originX = x - cameraX;
            int originY = y - cameraY;

            GraphNode target = getPortNodeAt((int) mouseX, (int) mouseY, originX, originY, true);
            if (target != null && target != connectSource) {
                boolean exists = false;
                for (GraphEdge e : connectSource.outputs) {
                    if (e.to == target) { exists = true; break; }
                }
                if (!exists) {
                    GraphEdge e = new GraphEdge();
                    e.from = connectSource;
                    e.to = target;
                    e.auto = true;
                    edges.add(e);
                    connectSource.outputs.add(e);
                    target.inputs.add(e);
                    saveMachine();
                }
            }
            connecting = false;
            connectSource = null;
            return true;
        }

        if (draggedNode != null) {
            draggedNode = null;
            saveMachine();
            return true;
        }
        if (panning) {
            panning = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (connecting) {
            connectMouseX = (int) mouseX;
            connectMouseY = (int) mouseY;
            return true;
        }
        if (draggedNode != null) {
            int x = DashboardScreen.SIDEBAR_WIDTH;
            int y = DashboardScreen.TOPBAR_HEIGHT;
            int originX = x - cameraX;
            int originY = y - cameraY;
            draggedNode.x = (int) mouseX - originX - dragOffsetX;
            draggedNode.y = (int) mouseY - originY - dragOffsetY;
            draggedNode.x = Math.round(draggedNode.x / 10f) * 10;
            draggedNode.y = Math.round(draggedNode.y / 10f) * 10;
            return true;
        }
        if (panning) {
            cameraX = cameraStartX + panStartX - (int) mouseX;
            cameraY = cameraStartY + panStartY - (int) mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showNodeDialog) {
            if (keyCode == 257 || keyCode == 335) {
                confirmNodeDialog();
                return true;
            }
            if (keyCode == 256) {
                closeNodeDialog();
                return true;
            }
            if (nodeIdBox != null && nodeIdBox.isFocused()) {
                return nodeIdBox.keyPressed(keyCode, scanCode, modifiers);
            }
            if (nodeNameBox != null && nodeNameBox.isFocused()) {
                return nodeNameBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (showEdgeDialog) {
            if (keyCode == 257 || keyCode == 335) {
                confirmEdgeDialog();
                return true;
            }
            if (keyCode == 256) {
                closeEdgeDialog();
                return true;
            }
            if (edgeTargetBox != null && edgeTargetBox.isFocused()) {
                return edgeTargetBox.keyPressed(keyCode, scanCode, modifiers);
            }
            if (edgeConditionBox != null && edgeConditionBox.isFocused()) {
                return edgeConditionBox.keyPressed(keyCode, scanCode, modifiers);
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

        if (keyCode == 32) {
            openNodeDialog("create", null);
            return true;
        }
        if (keyCode == 261 || keyCode == 259) {
            if (selectedNode != null) {
                openConfirmDialog("delete_node", selectedNode.id);
                return true;
            }
            if (selectedEdge != null) {
                deleteEdge(selectedEdge);
                return true;
            }
        }
        if (keyCode == 256) {
            saveMachine();
            return false;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showNodeDialog) {
            if (nodeIdBox != null && nodeIdBox.isFocused()) {
                return nodeIdBox.charTyped(codePoint, modifiers);
            }
            if (nodeNameBox != null && nodeNameBox.isFocused()) {
                return nodeNameBox.charTyped(codePoint, modifiers);
            }
            return true;
        }
        if (showEdgeDialog) {
            if (edgeTargetBox != null && edgeTargetBox.isFocused()) {
                return edgeTargetBox.charTyped(codePoint, modifiers);
            }
            if (edgeConditionBox != null && edgeConditionBox.isFocused()) {
                return edgeConditionBox.charTyped(codePoint, modifiers);
            }
            return true;
        }
        return false;
    }

    private GraphNode getNodeAt(int mx, int my, int originX, int originY) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode n = nodes.get(i);
            int nx = originX + n.x;
            int ny = originY + n.y;
            if (mx >= nx && mx <= nx + NODE_WIDTH && my >= ny && my <= ny + NODE_HEIGHT) {
                return n;
            }
        }
        return null;
    }

    private GraphNode getPortNodeAt(int mx, int my, int originX, int originY, boolean input) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode n = nodes.get(i);
            int nx = originX + n.x + NODE_WIDTH / 2 - PORT_SIZE / 2;
            int ny;
            if (input) {
                ny = originY + n.y - PORT_SIZE / 2;
            } else {
                ny = originY + n.y + NODE_HEIGHT - PORT_SIZE / 2;
            }
            if (mx >= nx && mx <= nx + PORT_SIZE && my >= ny && my <= ny + PORT_SIZE) {
                return n;
            }
        }
        return null;
    }

    private GraphEdge getEdgeAt(int mx, int my) {
        for (GraphEdge e : edges) {
            if (e.from == null || e.to == null) continue;
            int x1 = e.from.x + NODE_WIDTH / 2;
            int y1 = e.from.y + NODE_HEIGHT;
            int x2 = e.to.x + NODE_WIDTH / 2;
            int y2 = e.to.y;
            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2;
            int dx = mx - (DashboardScreen.SIDEBAR_WIDTH - cameraX + midX);
            int dy = my - (DashboardScreen.TOPBAR_HEIGHT - cameraY + midY);
            if (dx * dx + dy * dy < 100) {
                return e;
            }
        }
        return null;
    }

    private boolean isShiftDown() {
        return this.minecraft != null && this.minecraft.options.keyShift.isDown();
    }

    private static class GraphNode {
        String id;
        String name;
        int color;
        int x, y;
        long lastClickTime = 0;
        final List<GraphEdge> outputs = new ArrayList<>();
        final List<GraphEdge> inputs = new ArrayList<>();
    }

    private static class GraphEdge {
        GraphNode from;
        GraphNode to;
        boolean auto;
    }
}