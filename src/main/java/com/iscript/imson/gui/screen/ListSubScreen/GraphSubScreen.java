package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.Graph;
import com.iscript.imson.data.Node;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.SubScreenLifecycle;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.gui.undo.CommandStack;
import com.iscript.imson.gui.undo.NodeCommands;
import com.iscript.imson.gui.widget.ContextMenu;
import com.iscript.imson.gui.widget.MultiLineEditBox;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GraphSubScreen extends DashboardScreen.SubScreen {
    protected final SubScreenLifecycle life = new SubScreenLifecycle(this);
    protected static final int ITEM_HEIGHT = 20;
    protected static final int RIGHT_PANEL_WIDTH = 140;
    protected static final int TOOLBAR_WIDTH = 32;

    protected Graph currentGraph = null;
    protected String draggingNodeId = null;
    protected String connectingFromId = null;
    protected int connectingSlot = -1;
    protected boolean panning = false;
    protected double panStartX, panStartY;
    protected double panCanvasStartX, panCanvasStartY;
    protected String selectedNodeId = null;
    protected double dragStartMouseX, dragStartMouseY;
    protected int dragStartNodeX, dragStartNodeY;

    protected boolean showConnectionContextMenu = false;
    protected int connectionContextMenuX, connectionContextMenuY;
    protected String connectionContextFromId = null;
    protected int connectionContextSlot = -1;
    protected String connectionContextToId = null;

    protected ContextMenu contextMenu = new ContextMenu();
    protected String rightClickNodeId = null;

    protected Node editingNode = null;

    protected final Map<String, Graph> clientCache = new HashMap<>();
    protected final CommandStack commandStack = new CommandStack();
    protected long lastClickTime = 0;
    protected String lastClickNodeId = null;
    protected static final long DOUBLE_CLICK_MS = 400;
    protected String hoveredConnectionFromId = null;
    protected int hoveredConnectionSlot = -1;
    protected String hoveredConnectionToId = null;

    public GraphSubScreen(DashboardScreen parent) {
        super(parent);
    }

    protected abstract String getCategory();
    protected abstract Map<String, Graph> getAllGraphs();
    protected abstract Graph getGraph(String id);
    protected abstract void putGraph(Graph g);
    protected abstract void removeGraph(String id);
    protected abstract void sendSavePacket(Graph g);
    protected abstract void sendRunPacket(String id);
    protected abstract void requestList();
    protected abstract List<NodeType> getNodeTypes();
    protected abstract Graph createEmpty(String id, String name);
    protected abstract String getListTitleKey();
    protected abstract String getListNewKey();
    protected abstract String getListEmptyKey();
    protected abstract String getSearchKey();
    protected abstract String getNameModalTitleKey(String mode);
    protected abstract String getNameModalBtnKey(String mode);
    protected abstract String getEditorNewKey();
    protected abstract String getEditorRenameKey();
    protected abstract String getEditorCreateKey();
    protected abstract String getEditorRenameBtnKey();
    protected abstract String getEditorCancelKey();
    protected abstract String getEditorDeleteConfirmKey();
    protected abstract String getEditorDeleteKey();
    protected abstract String getNodeEditTitleKey();
    protected abstract String getNodeEditKey();
    protected abstract String getNodeSetStartKey();
    protected abstract String getNodeClearConnectionsKey();
    protected abstract String getNodeDeleteKey();
    protected abstract String getConnectionDeleteKey();
    protected abstract String getNodeTypeKey(String typeId);
    protected abstract String getNodeTitle(Node node);
    protected abstract String getNodeBodyText(Node node);
    protected abstract int getNodeHeaderColor(Node node);
    protected abstract int getOutputCount(Node node);
    protected abstract String getOutputLabel(Node node, int slot);
    protected abstract boolean hasFollowJump(Node node);
    protected abstract void executeFollowJump(Node node);
    protected abstract boolean useI18nForList();

    @Override
    public void init() {
        showConnectionContextMenu = false;
        hoveredConnectionFromId = null;
        rightClickNodeId = null;
        contextMenu.close();

        String editingNodeId = life.state().modalFieldValues.get("editingNodeId");
        if (editingNodeId != null && currentGraph != null) {
            editingNode = currentGraph.getNode(editingNodeId);
        }

        life.init();

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        life.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, useI18nForList() ? I18n.t(getSearchKey()) : Component.translatable(getSearchKey()));

        life.modals().register("name",
                () -> life.modals().isOpen("name"),
                v -> {},
                () -> {
                    int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
                    int dy = this.parent.height / 2 - 40;
                    String mode = life.state().modalFieldValues.getOrDefault("nameMode", "create");
                    String oldId = life.state().modalFieldValues.get("renameOldId");
                    String initial = "";
                    if (mode.equals("rename") && oldId != null) {
                        Graph oldGraph = getGraph(oldId);
                        initial = oldGraph != null ? oldGraph.getName() : oldId;
                    }
                    life.editors().addBox("name", cx - 100, dy + 20, 200, 20, useI18nForList() ? I18n.t(getNameModalTitleKey(mode)) : Component.translatable(getNameModalTitleKey(mode)), initial);
                    parent.setFocusedWidget(life.editors().box("name"));
                },
                () -> life.editors().remove("name"),
                "name");

        life.modals().register("nodeEditor",
                () -> life.modals().isOpen("nodeEditor"),
                v -> {},
                () -> {
                    if (editingNode == null) return;
                    int cx = DashboardScreen.SIDEBAR_W + (this.parent.width - DashboardScreen.SIDEBAR_W) / 2;
                    int dy = this.parent.height / 2 - 80;
                    NodeType nodeType = getNodeTypeFor(editingNode.getType());
                    if (nodeType == null) return;
                    for (NodeType.EditorField field : nodeType.editorFields) {
                        String initial = editingNode.getParam(field.key);
                        if (initial == null) initial = "";
                        if (field.type == NodeType.FieldType.MULTILINE) {
                            life.editors().addMultiBox(field.key, cx, dy + 20, 180, 120, useI18nForList() ? I18n.t(field.labelKey) : Component.translatable(field.labelKey), initial);
                        } else if (field.type == NodeType.FieldType.NUMBER) {
                            life.editors().addNumericBox(field.key, cx, dy + 20, 180, 18, useI18nForList() ? I18n.t(field.labelKey) : Component.translatable(field.labelKey), initial);
                        } else {
                            life.editors().addBox(field.key, cx, dy + 20, 180, 18, useI18nForList() ? I18n.t(field.labelKey) : Component.translatable(field.labelKey), initial);
                        }
                        dy += 40;
                    }
                    if (!nodeType.editorFields.isEmpty()) {
                        parent.setFocusedWidget(life.editors().box(nodeType.editorFields.get(0).key));
                    }
                },
                () -> {
                    if (editingNode != null) {
                        NodeType nodeType = getNodeTypeFor(editingNode.getType());
                        if (nodeType != null) {
                            for (NodeType.EditorField field : nodeType.editorFields) {
                                String oldValue = editingNode.getParam(field.key);
                                String newValue;
                                if (field.type == NodeType.FieldType.MULTILINE) {
                                    MultiLineEditBox box = life.editors().multi(field.key);
                                    newValue = box != null ? box.getValue() : oldValue;
                                } else {
                                    EditBox box = life.editors().box(field.key);
                                    newValue = box != null ? box.getValue() : oldValue;
                                }
                                if (!oldValue.equals(newValue)) {
                                    commandStack.execute(new NodeCommands.EditNodeParamCommand(editingNode, field.key, oldValue, newValue));
                                }
                            }
                            save();
                        }
                    }
                    if (editingNode != null) {
                        NodeType nodeType = getNodeTypeFor(editingNode.getType());
                        if (nodeType != null) {
                            for (NodeType.EditorField field : nodeType.editorFields) {
                                life.editors().remove(field.key);
                            }
                        }
                    }
                    editingNode = null;
                    life.state().modalFieldValues.remove("editingNodeId");
                });

        life.modals().register("confirm",
                () -> life.modals().isOpen("confirm"),
                v -> {},
                () -> {},
                () -> {},
                "confirmAction", "confirmId");

        requestList();
    }

    protected NodeType getNodeTypeFor(String typeId) {
        for (NodeType nt : getNodeTypes()) {
            if (nt.id.equals(typeId)) return nt;
        }
        return null;
    }

    protected List<String> filteredIds() {
        Map<String, Graph> graphs = getAllGraphs();
        String filter = life.search().box() != null ? life.search().box().getValue().trim().toLowerCase() : life.state().lastSearch.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Graph> e : graphs.entrySet()) {
            String name = e.getValue().getName();
            if (name == null) name = "";
            name = name.trim();
            if (filter.isEmpty() || name.toLowerCase().contains(filter) || e.getKey().toLowerCase().contains(filter)) {
                result.add(e.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }

    protected void openNameDialog(String mode, String oldId) {
        life.state().modalFieldValues.put("nameMode", mode);
        life.state().modalFieldValues.put("renameOldId", oldId);
        life.modals().open("name");
    }

    protected void closeNameDialog() {
        life.modals().close("name");
    }

    protected void confirmNameDialog() {
        EditBox nameInputBox = life.editors().box("name");
        if (nameInputBox == null) return;
        String name = nameInputBox.getValue().trim();
        String mode = life.state().modalFieldValues.getOrDefault("nameMode", "create");
        String oldId = life.state().modalFieldValues.get("renameOldId");
        closeNameDialog();
        if (name.isEmpty()) return;
        String id = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (id.isEmpty()) return;
        if (mode.equals("create")) {
            Graph graph = createEmpty(id, name);
            putGraph(graph);
            save();
            switchToGraph(id);
        } else if (mode.equals("rename") && oldId != null) {
            if (oldId.equals(id)) return;
            Graph oldGraph = getGraph(oldId);
            if (oldGraph != null) {
                Graph newGraph = oldGraph.copy();
                newGraph.setId(id);
                newGraph.setName(name);
                removeGraph(oldId);
                putGraph(newGraph);
                clientCache.remove(oldId);
                clientCache.put(id, newGraph);
                life.state().savedCanvasPositions.remove(oldId);
                if (life.selection().get() != null && life.selection().get().equals(oldId)) {
                    life.selection().set(id);
                    currentGraph = newGraph;
                }
                save();
                if (life.selection().get() != null && life.selection().get().equals(id)) {
                    switchToGraph(id);
                }
            }
        }
    }

    protected void switchToGraph(String id) {
        if (life.selection().get() != null && currentGraph != null) {
            life.canvas().saveFor(life.selection().get());
            sendSavePacket(currentGraph);
        }
        life.selection().set(id);
        selectedNodeId = null;
        draggingNodeId = null;
        cancelConnection();
        showConnectionContextMenu = false;
        rightClickNodeId = null;
        closeNodeEditor();
        currentGraph = clientCache.get(id);
        if (currentGraph == null) {
            currentGraph = getGraph(id);
        }
        if (currentGraph == null) {
            currentGraph = createEmpty(id, id);
            clientCache.put(id, currentGraph);
        }
        life.canvas().loadFor(id);
    }

    protected void addNode(String typeId) {
        if (currentGraph == null) return;
        NodeType nodeType = getNodeTypeFor(typeId);
        if (nodeType == null) return;
        String nodeId = "node_" + System.currentTimeMillis();
        Node node = new Node(null);
        node.setId(nodeId);
        node.setType(typeId);
        for (Map.Entry<String, String> e : nodeType.defaultParams.entrySet()) {
            node.setParam(e.getKey(), e.getValue());
        }
        node.setX((int) (life.canvas().x() + 200));
        node.setY((int) (life.canvas().y() + 200));
        commandStack.execute(new NodeCommands.AddNodeCommand(currentGraph, node));
        save();
    }

    protected void save() {
        if (currentGraph == null || life.selection().get() == null) return;
        clientCache.put(life.selection().get(), currentGraph);
        sendSavePacket(currentGraph);
    }

    protected void deleteNode(String nodeId) {
        if (currentGraph == null) return;
        Node node = currentGraph.getNode(nodeId);
        if (node == null) return;
        commandStack.execute(new NodeCommands.DeleteNodeCommand(currentGraph, node));
        if (selectedNodeId != null && selectedNodeId.equals(nodeId)) selectedNodeId = null;
        save();
    }

    protected void cancelConnection() {
        connectingFromId = null;
        connectingSlot = -1;
    }

    protected void openNodeEditor(Node node) {
        editingNode = node;
        life.state().modalFieldValues.put("editingNodeId", node.getId());
        life.modals().open("nodeEditor");
    }

    protected void closeNodeEditor() {
        life.modals().close("nodeEditor");
    }

    protected void openConfirmDialog(String action, String id) {
        life.state().modalFieldValues.put("confirmAction", action);
        life.state().modalFieldValues.put("confirmId", id);
        life.modals().open("confirm");
    }

    protected void closeConfirmDialog() {
        life.modals().close("confirm");
    }

    protected void executeConfirm() {
        String action = life.state().modalFieldValues.getOrDefault("confirmAction", "");
        String confirmId = life.state().modalFieldValues.get("confirmId");
        if ("delete".equals(action) && confirmId != null) {
            removeGraph(confirmId);
            clientCache.remove(confirmId);
            life.state().savedCanvasPositions.remove(confirmId);
            if (life.selection().get() != null && life.selection().get().equals(confirmId)) {
                life.selection().set(null);
                currentGraph = null;
            }
        }
        closeConfirmDialog();
    }

    protected void copyItem(String id) {
        DashboardScreen.clipboard = id;
    }

    protected void pasteItem() {
        String sourceId = DashboardScreen.clipboard;
        if (sourceId == null || sourceId.isEmpty()) return;
        Graph source = getGraph(sourceId);
        if (source == null) return;
        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (getGraph(newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }
        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (Copy)");
        putGraph(copy);
        switchToGraph(newId);
    }

    protected void duplicateItem(String id) {
        Graph source = getGraph(id);
        if (source == null) return;
        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (getGraph(newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }
        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (" + counter + ")");
        putGraph(copy);
        switchToGraph(newId);
    }

    @Override
    public void tick() {
        life.tick(this::save);
    }

    @Override
    public void removed() {
        if (life.selection().get() != null) {
            life.canvas().saveFor(life.selection().get());
        }
        contextMenu.close();
        life.removed();
        commandStack.clear();
        super.removed();
    }

    protected int getNodeWidth() {
        return Math.max(60, (int) (140 * life.canvas().zoom()));
    }

    protected int getNodeHeight() {
        return Math.max(30, (int) (78 * life.canvas().zoom()));
    }

    protected int getNodeHeaderHeight() {
        return Math.max(10, (int) (18 * life.canvas().zoom()));
    }

    protected int worldToScreenX(int wx, int leftX) {
        return (int) ((wx - life.canvas().x()) * life.canvas().zoom() + leftX);
    }

    protected int worldToScreenY(int wy, int leftY) {
        return (int) ((wy - life.canvas().y()) * life.canvas().zoom() + leftY);
    }

    protected int getOutputSlotY(int nh, int headerH, int slot, int totalSlots) {
        if (totalSlots <= 0) totalSlots = 1;
        return headerH + (slot + 1) * (nh - headerH) / (totalSlots + 1);
    }

    protected void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
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

    protected boolean hitTestBezier(int x1, int y1, int x2, int y2, double mx, double my, double threshold) {
        double offset = Math.max(40.0, Math.abs(x2 - x1) * 0.5);
        double cp1x = x1 + offset;
        double cp1y = y1;
        double cp2x = x2 - offset;
        double cp2y = y2;
        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double inv = 1.0 - t;
            double t2 = t * t;
            double inv2 = inv * inv;
            double bx = inv2 * inv * x1 + 3 * inv2 * t * cp1x + 3 * inv * t2 * cp2x + t2 * t * x2;
            double by = inv2 * inv * y1 + 3 * inv2 * t * cp1y + 3 * inv * t2 * cp2y + t2 * t * y2;
            if (Math.hypot(bx - mx, by - my) < threshold) return true;
        }
        return false;
    }

    protected void drawSmoothPoint(GuiGraphics graphics, Matrix4f matrix, BufferBuilder buf, float x, float y, float r, float g, float b, float a, float size) {
        float s = size * 0.5f;
        buf.vertex(matrix, x - s, y - s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x - s, y + s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + s, y + s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + s, y - s, 0).color(r, g, b, a).endVertex();
    }

    protected void drawBezierCurve(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, boolean hovered) {
        double offset = Math.max(40.0, Math.abs(x2 - x1) * 0.5);
        double cp1x = x1 + offset;
        double cp1y = y1;
        double cp2x = x2 - offset;
        double cp2y = y2;
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        int steps = Math.max(200, (int) (dist * 3.0));
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (hovered) {
            a = Math.min(1.0f, a + 0.3f);
            r = Math.min(1.0f, r + 0.2f);
            g = Math.min(1.0f, g + 0.2f);
            b = Math.min(1.0f, b + 0.2f);
        }
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float thickness = hovered ? 3.0f : 1.8f;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double inv = 1.0 - t;
            double t2 = t * t;
            double inv2 = inv * inv;
            double x = inv2 * inv * x1 + 3 * inv2 * t * cp1x + 3 * inv * t2 * cp2x + t2 * t * x2;
            double y = inv2 * inv * y1 + 3 * inv2 * t * cp1y + 3 * inv * t2 * cp2y + t2 * t * y2;
            drawSmoothPoint(graphics, matrix, buf, (float) x, (float) y, r, g, b, a, thickness);
        }
        tess.end();
    }

    protected void renderConnection(GuiGraphics graphics, Node from, Node to, int sourceSlot, int leftX, int leftY, boolean hovered) {
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        int outCount = getOutputCount(from);
        int x1 = worldToScreenX(from.getX(), leftX) + nw;
        int y1 = worldToScreenY(from.getY(), leftY) + getOutputSlotY(nh, headerH, sourceSlot, Math.max(1, outCount));
        int x2 = worldToScreenX(to.getX(), leftX);
        int y2 = worldToScreenY(to.getY(), leftY) + nh / 2;
        int color = Theme.TEXT_DIM;
        drawBezierCurve(graphics, x1, y1, x2, y2, color, false);
        double offset = Math.max(40.0, Math.abs(x2 - x1) * 0.5);
        double cp1x = x1 + offset;
        double cp1y = y1;
        double cp2x = x2 - offset;
        double cp2y = y2;
        double t0 = 0.92;
        double u0 = 1.0 - t0;
        double t02 = t0 * t0;
        double u02 = u0 * u0;
        double px = u02 * u0 * x1 + 3 * u02 * t0 * cp1x + 3 * u0 * t02 * cp2x + t02 * t0 * x2;
        double py = u02 * u0 * y1 + 3 * u02 * t0 * cp1y + 3 * u0 * t02 * cp2y + t02 * t0 * y2;
        double dx = x2 - px;
        double dy = y2 - py;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            dx /= len;
            dy /= len;
        }
        int ax1 = (int) (x2 - 8 * dx + 4 * dy);
        int ay1 = (int) (y2 - 8 * dy - 4 * dx);
        int ax2 = (int) (x2 - 8 * dx - 4 * dy);
        int ay2 = (int) (y2 - 8 * dy + 4 * dx);
        drawLine(graphics, x2, y2, ax1, ay1, color);
        drawLine(graphics, x2, y2, ax2, ay2, color);
    }

    protected void renderNode(GuiGraphics graphics, Node node, int leftX, int leftY, int mouseX, int mouseY) {
        int nx = worldToScreenX(node.getX(), leftX);
        int ny = worldToScreenY(node.getY(), leftY);
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        boolean hovered = mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh;
        boolean selected = node.getId().equals(selectedNodeId);
        boolean isStart = currentGraph != null && node.getId().equals(currentGraph.getStartNodeId());
        graphics.fill(nx + 2, ny + 2, nx + nw + 2, ny + nh + 2, Theme.alpha(Theme.BG_PANEL, 0.27f));
        UI.panel(graphics, nx, ny, nw, nh);
        graphics.renderOutline(nx, ny, nw, nh, selected ? Theme.BORDER_ACCENT : (isStart ? Theme.ACCENT : Theme.BORDER));
        int headerColor = getNodeHeaderColor(node);
        UI.inner(graphics, nx, ny, nw, headerH);
        String title = getNodeTitle(node);
        String label = font.plainSubstrByWidth(title, nw - 8);
        graphics.drawString(font, label, nx + 4, ny + (headerH - 8) / 2, Theme.TEXT);
        if (nh > 30) {
            String text = getNodeBodyText(node);
            if (text.length() > 30) text = text.substring(0, 30) + "...";
            text = font.plainSubstrByWidth(text, nw - 8);
            graphics.drawString(font, text, nx + 4, ny + headerH + 4, Theme.TEXT_DIM);
        }
        int pinR = Math.max(3, (int) (4 * life.canvas().zoom()));
        int inX = nx;
        int inY = ny + nh / 2;
        graphics.fill(inX - pinR, inY - pinR, inX + pinR, inY + pinR, Theme.ACCENT);
        graphics.renderOutline(inX - pinR, inY - pinR, pinR * 2, pinR * 2, Theme.BORDER_ACCENT);
        int outCount = getOutputCount(node);
        if (outCount > 0) {
            for (int i = 0; i < outCount; i++) {
                int slotY = ny + getOutputSlotY(nh, headerH, i, outCount);
                int outX = nx + nw;
                boolean active = connectingFromId != null && connectingFromId.equals(node.getId()) && connectingSlot == i;
                int pinColor = active ? Theme.ACCENT : Theme.ERROR;
                graphics.fill(outX - pinR, slotY - pinR, outX + pinR, slotY + pinR, pinColor);
                graphics.renderOutline(outX - pinR, slotY - pinR, pinR * 2, pinR * 2, active ? Theme.BORDER_ACCENT : Theme.ERROR);
                if (life.canvas().zoom() > 0.6 && nh > 40) {
                    String lbl = getOutputLabel(node, i);
                    if (!lbl.isEmpty()) {
                        graphics.drawString(font, lbl, outX - pinR - font.width(lbl) - 2, slotY - 3, Theme.TEXT_DIM);
                    }
                }
            }
        }
    }

    protected void renderGrid(GuiGraphics graphics, int leftX, int leftY, int leftW, int leftH) {
        int gridSize = (int) (40 * life.canvas().zoom());
        if (gridSize < 10) gridSize = 10;
        int offsetX = (int) ((-life.canvas().x() * life.canvas().zoom()) % gridSize);
        if (offsetX < 0) offsetX += gridSize;
        int offsetY = (int) ((-life.canvas().y() * life.canvas().zoom()) % gridSize);
        if (offsetY < 0) offsetY += gridSize;
        for (int gx = leftX + offsetX; gx < leftX + leftW; gx += gridSize) {
            graphics.fill(gx, leftY, gx + 1, leftY + leftH, Theme.BG_INNER);
        }
        for (int gy = leftY + offsetY; gy < leftY + leftH; gy += gridSize) {
            graphics.fill(leftX, gy, leftX + leftW, gy + 1, Theme.BG_INNER);
        }
    }

    protected void updateHoveredConnection(int mouseX, int mouseY, int leftX, int leftY) {
        hoveredConnectionFromId = null;
        hoveredConnectionSlot = -1;
        hoveredConnectionToId = null;
        if (currentGraph == null) return;
        for (Node node : currentGraph.getNodes().values()) {
            for (Node.Connection conn : node.getConnections()) {
                Node target = currentGraph.getNode(conn.getTarget());
                if (target == null) continue;
                int nw = getNodeWidth();
                int nh = getNodeHeight();
                int headerH = getNodeHeaderHeight();
                int outCount = getOutputCount(node);
                int x1 = worldToScreenX(node.getX(), leftX) + nw;
                int y1 = worldToScreenY(node.getY(), leftY) + getOutputSlotY(nh, headerH, conn.getSourceSlot(), Math.max(1, outCount));
                int x2 = worldToScreenX(target.getX(), leftX);
                int y2 = worldToScreenY(target.getY(), leftY) + nh / 2;
                if (hitTestBezier(x1, y1, x2, y2, mouseX, mouseY, 8.0)) {
                    hoveredConnectionFromId = node.getId();
                    hoveredConnectionSlot = conn.getSourceSlot();
                    hoveredConnectionToId = target.getId();
                    return;
                }
            }
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

        if (!life.modals().isOpen("name") && !life.modals().isOpen("nodeEditor") && !life.modals().isOpen("confirm")) {
            life.search().setVisible(true);
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
            graphics.fill(toolbarX, y, rightX, y + h, Theme.BG_PANEL);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, Theme.BG_HOVER);
            int btnSize = 24;
            int btnY = y + 8;
            boolean runHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, runHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
            graphics.drawCenteredString(this.font, "▶", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, runHovered ? Theme.ACCENT : 0xFF44AA44);
            btnY += btnSize + 6;
            if (currentGraph != null) {
                boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
                graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
                graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
                graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? Theme.TEXT : Theme.TEXT_DIM);
            }
            life.search().setPos(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16);
            life.search().setVisible(true);
            if (life.search().box() != null) {
                life.search().box().render(graphics, mouseX, mouseY, partialTick);
            }
            graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
            graphics.drawString(this.font, useI18nForList() ? I18n.s(getListTitleKey()) : Component.translatable(getListTitleKey()).getString(), rightX + 8, y + 26, Theme.ACCENT);
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            for (int i = life.selection().scroll(); i < Math.min(life.selection().scroll() + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - life.selection().scroll()) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(life.selection().get());
                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
            }
            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getListNewKey()) : Component.translatable(getListNewKey()).getString(), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);
            if (life.selection().get() != null && currentGraph != null) {
                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);
                RenderSystem.enableScissor(leftX * (int) this.minecraft.getWindow().getGuiScale(), (this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * (int) this.minecraft.getWindow().getGuiScale(), leftW * (int) this.minecraft.getWindow().getGuiScale(), leftH * (int) this.minecraft.getWindow().getGuiScale());
                renderGrid(graphics, leftX, leftY, leftW, leftH);
                for (Node node : currentGraph.getNodes().values()) {
                    for (Node.Connection conn : node.getConnections()) {
                        Node target = currentGraph.getNode(conn.getTarget());
                        if (target != null) {
                            boolean hovered = node.getId().equals(hoveredConnectionFromId) && conn.getSourceSlot() == hoveredConnectionSlot && conn.getTarget().equals(hoveredConnectionToId);
                            renderConnection(graphics, node, target, conn.getSourceSlot(), leftX, leftY, false);
                        }
                    }
                }
                if (connectingFromId != null) {
                    Node from = currentGraph.getNode(connectingFromId);
                    if (from != null) {
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int outCount = getOutputCount(from);
                        int x1 = worldToScreenX(from.getX(), leftX) + nw;
                        int y1 = worldToScreenY(from.getY(), leftY) + getOutputSlotY(nh, headerH, connectingSlot, Math.max(1, outCount));
                        int x2 = mouseX;
                        int y2 = mouseY;
                        drawBezierCurve(graphics, x1, y1, x2, y2, Theme.ACCENT, false);
                    }
                }
                for (Node node : currentGraph.getNodes().values()) {
                    renderNode(graphics, node, leftX, leftY, mouseX, mouseY);
                }
                RenderSystem.disableScissor();
                graphics.drawString(font, String.format("%.0f%%", life.canvas().zoom() * 100), leftX + 4, leftY + 4, Theme.TEXT_MUTE);
            } else {
                graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getListEmptyKey()) : Component.translatable(getListEmptyKey()).getString(), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
            }
        } else {
            life.search().setVisible(false);
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
        }

        if (life.modals().isOpen("confirm")) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = this.parent.height / 2 - 30;
            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_PANEL, 0.8f));
            UI.panel(graphics, dx, dy, dw, dh);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
            String confirmId = life.state().modalFieldValues.get("confirmId");
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getEditorDeleteConfirmKey(), confirmId) : Component.translatable(getEditorDeleteConfirmKey(), confirmId).getString(), cx, dy + 8, Theme.ERROR);
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx - 50, dy + 38, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getEditorDeleteKey()) : Component.translatable(getEditorDeleteKey()).getString(), cx - 26, dy + 43, okHovered ? Theme.ERROR : Theme.TEXT_DIM);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx + 2, dy + 38, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getEditorCancelKey()) : Component.translatable(getEditorCancelKey()).getString(), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT_DIM);
        }

        if (life.modals().isOpen("name")) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 80;
            int dx = cx - dw / 2;
            int dy = this.parent.height / 2 - 40;
            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_PANEL, 0.8f));
            UI.panel(graphics, dx, dy, dw, dh);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
            String mode = life.state().modalFieldValues.getOrDefault("nameMode", "create");
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(mode.equals("rename") ? getEditorRenameKey() : getEditorNewKey()) : Component.translatable(mode.equals("rename") ? getEditorRenameKey() : getEditorNewKey()).getString(), cx, dy + 6, Theme.ACCENT);
            EditBox nameInputBox = life.editors().box("name");
            if (nameInputBox != null) {
                nameInputBox.setX(cx - 100);
                nameInputBox.setY(dy + 24);
            }
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx - 50, dy + 52, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(mode.equals("rename") ? getEditorRenameBtnKey() : getEditorCreateKey()) : Component.translatable(mode.equals("rename") ? getEditorRenameBtnKey() : getEditorCreateKey()).getString(), cx - 26, dy + 57, okHovered ? Theme.ACCENT : Theme.TEXT_DIM);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx + 2, dy + 52, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, useI18nForList() ? I18n.s(getEditorCancelKey()) : Component.translatable(getEditorCancelKey()).getString(), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (life.modals().isOpen("nodeEditor") && editingNode != null) {
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = this.parent.height / 2 - 80;
            NodeType nodeType = getNodeTypeFor(editingNode.getType());
            if (nodeType != null) {
                int editorH = 30 + nodeType.editorFields.size() * 40;
                for (NodeType.EditorField field : nodeType.editorFields) {
                    if (field.type == NodeType.FieldType.MULTILINE) {
                        editorH += 80;
                    }
                }
                UI.panel(graphics, dx - 10, dy - 10, 210, editorH + 20);
                graphics.renderOutline(dx - 10, dy - 10, 210, editorH + 20, Theme.BORDER_ACCENT);
                graphics.drawString(font, useI18nForList() ? I18n.s(getNodeEditTitleKey()) : Component.translatable(getNodeEditTitleKey()).getString(), dx, dy, Theme.TEXT);
                int py = dy + 20;
                for (int i = 0; i < nodeType.editorFields.size(); i++) {
                    NodeType.EditorField field = nodeType.editorFields.get(i);
                    UI.label(graphics, font, (useI18nForList() ? I18n.s(field.labelKey) : Component.translatable(field.labelKey).getString()) + ":", dx, py);
                    if (field.type == NodeType.FieldType.MULTILINE) {
                        MultiLineEditBox box = life.editors().multi(field.key);
                        if (box != null) { box.setX(dx); box.setY(py + 12); box.setVisible(true); }
                        py += 120;
                    } else {
                        EditBox box = life.editors().box(field.key);
                        if (box != null) { box.setX(dx); box.setY(py + 12); box.setVisible(true); }
                        py += 40;
                    }
                }
            }
        }

        if (showConnectionContextMenu && connectionContextFromId != null) {
            int wctx = 100;
            int hctx = 24;
            UI.panel(graphics, connectionContextMenuX, connectionContextMenuY, wctx, hctx);
            boolean h1 = mouseX >= connectionContextMenuX && mouseX <= connectionContextMenuX + wctx && mouseY >= connectionContextMenuY + 2 && mouseY <= connectionContextMenuY + 22;
            UI.row(graphics, connectionContextMenuX + 1, connectionContextMenuY + 2, wctx - 2, 20, false, h1);
            graphics.drawString(font, useI18nForList() ? I18n.s(getConnectionDeleteKey()) : Component.translatable(getConnectionDeleteKey()).getString(), connectionContextMenuX + 4, connectionContextMenuY + 6, h1 ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
        }
    }

    protected void runGraph() {
        if (life.selection().get() == null || currentGraph == null) return;
        save();
        sendRunPacket(life.selection().get());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("name")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
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
            EditBox nameInputBox = life.editors().box("name");
            if (nameInputBox != null && mouseX >= nameInputBox.getX() && mouseX <= nameInputBox.getX() + nameInputBox.getWidth() && mouseY >= nameInputBox.getY() && mouseY <= nameInputBox.getY() + nameInputBox.getHeight()) {
                parent.setFocusedWidget(nameInputBox);
                return nameInputBox.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }

        if (life.modals().isOpen("confirm")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
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

        if (life.modals().isOpen("nodeEditor")) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = this.parent.height / 2 - 80;
            NodeType nodeType = getNodeTypeFor(editingNode.getType());
            if (nodeType == null) return true;
            int editorH = 30 + nodeType.editorFields.size() * 40;
            for (NodeType.EditorField field : nodeType.editorFields) {
                if (field.type == NodeType.FieldType.MULTILINE) {
                    editorH += 80;
                }
            }
            if (mouseX >= dx - 10 && mouseX <= dx + 200 && mouseY >= dy - 10 && mouseY <= dy + editorH) {
                for (NodeType.EditorField field : nodeType.editorFields) {
                    if (field.type == NodeType.FieldType.MULTILINE) {
                        MultiLineEditBox box = life.editors().multi(field.key);
                        if (box != null && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth() && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight()) {
                            parent.setFocusedWidget(box);
                            return box.mouseClicked(mouseX, mouseY, button);
                        }
                    } else {
                        EditBox box = life.editors().box(field.key);
                        if (box != null && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth() && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight()) {
                            parent.setFocusedWidget(box);
                            return box.mouseClicked(mouseX, mouseY, button);
                        }
                    }
                }
                return true;
            }
            closeNodeEditor();
            return true;
        }

        if (showConnectionContextMenu) {
            if (mouseX >= connectionContextMenuX && mouseX <= connectionContextMenuX + 100 && mouseY >= connectionContextMenuY + 2 && mouseY <= connectionContextMenuY + 22) {
                if (connectionContextFromId != null && currentGraph != null) {
                    Node from = currentGraph.getNode(connectionContextFromId);
                    if (from != null) {
                        for (Node.Connection c : from.getConnections()) {
                            if (c.getSourceSlot() == connectionContextSlot && c.getTarget().equals(connectionContextToId)) {
                                commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, from, c));
                                save();
                                break;
                            }
                        }
                    }
                }
                showConnectionContextMenu = false;
                return true;
            }
            showConnectionContextMenu = false;
            return true;
        }

        if (contextMenu.isOpen()) {
            String itemId = contextMenu.getItemId();
            contextMenu.mouseClicked(mouseX, mouseY, button);
            String action = contextMenu.getLastAction();
            rightClickNodeId = null;
            if (action != null) {
                NodeType nt = getNodeTypeFor(action);
                if (nt != null) {
                    addNode(action);
                } else if (itemId != null) {
                    boolean isNode = currentGraph != null && currentGraph.getNode(itemId) != null;
                    if (isNode) {
                        Node node = currentGraph.getNode(itemId);
                        switch (action) {
                            case "Edit" -> openNodeEditor(node);
                            case "SetStart" -> {
                                String oldStart = currentGraph.getStartNodeId();
                                commandStack.execute(new NodeCommands.SetStartNodeCommand(currentGraph, oldStart, itemId));
                                save();
                            }
                            case "FollowJump" -> executeFollowJump(node);
                            case "ClearConnections" -> {
                                for (Node.Connection c : new ArrayList<>(node.getConnections())) {
                                    commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, node, c));
                                }
                                for (Node n : currentGraph.getNodes().values()) {
                                    for (Node.Connection c : new ArrayList<>(n.getConnections())) {
                                        if (c.getTarget().equals(itemId)) {
                                            commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, n, c));
                                        }
                                    }
                                }
                                save();
                            }
                            case "Delete" -> deleteNode(itemId);
                        }
                    } else {
                        switch (action) {
                            case "Copy" -> copyItem(itemId);
                            case "Paste" -> pasteItem();
                            case "Rename" -> openNameDialog("rename", itemId);
                            case "Duplicate" -> duplicateItem(itemId);
                            case "Delete" -> openConfirmDialog("delete", itemId);
                        }
                    }
                }
            }
            return true;
        }

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        int btnSize = 24;
        int btnY = y + 8;

        if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
            if (life.selection().get() != null && currentGraph != null) {
                runGraph();
            }
            return true;
        }
        btnY += btnSize + 6;

        if (currentGraph != null) {
            if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
                List<String> actions = new ArrayList<>();
                Map<String, String> labels = new HashMap<>();
                for (NodeType type : getNodeTypes()) {
                    actions.add(type.id);
                    labels.put(type.id, useI18nForList() ? I18n.s(getNodeTypeKey(type.id)) : Component.translatable(getNodeTypeKey(type.id)).getString());
                }
                contextMenu.setCustomActions(actions.toArray(new String[0]));
                contextMenu.setLabelOverrides(labels);
                contextMenu.open((int) mouseX, (int) mouseY, "", false);
                return true;
            }
        }

        if (life.search().box() != null && mouseX >= life.search().box().getX() && mouseX <= life.search().box().getX() + life.search().box().getWidth() && mouseY >= life.search().box().getY() && mouseY <= life.search().box().getY() + life.search().box().getHeight()) {
            life.search().box().setFocused(true);
            parent.setFocusedWidget(life.search().box());
            return life.search().box().mouseClicked(mouseX, mouseY, button);
        }

        List<String> ids = filteredIds();
        int listH = h - 68;
        int listY = y + 42;
        int visible = Math.max(1, listH / ITEM_HEIGHT);
        for (int i = life.selection().scroll(); i < Math.min(life.selection().scroll() + visible, ids.size()); i++) {
            int rowY = listY + (i - life.selection().scroll()) * ITEM_HEIGHT;
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                String id = ids.get(i);
                if (!id.equals(life.selection().get())) {
                    switchToGraph(id);
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

        if (currentGraph != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            if (button == 0) {
                List<Node> nodes = new ArrayList<>(currentGraph.getNodes().values());
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    Node node = nodes.get(i);
                    int nx = worldToScreenX(node.getX(), leftX);
                    int ny = worldToScreenY(node.getY(), leftY);
                    int nw = getNodeWidth();
                    int nh = getNodeHeight();
                    int headerH = getNodeHeaderHeight();
                    int pinR = Math.max(3, (int) (4 * life.canvas().zoom()));
                    int inX = nx;
                    int inY = ny + nh / 2;
                    if (mouseX >= inX - pinR && mouseX <= inX + pinR && mouseY >= inY - pinR && mouseY <= inY + pinR) {
                        if (connectingFromId != null && !connectingFromId.equals(node.getId())) {
                            Node from = currentGraph.getNode(connectingFromId);
                            if (from != null) {
                                Node.Connection conn = new Node.Connection();
                                conn.setTarget(node.getId());
                                conn.setSourceSlot(connectingSlot);
                                conn.setSourceNode(from.getId());
                                commandStack.execute(new NodeCommands.ConnectNodesCommand(currentGraph, from, conn));
                                save();
                            }
                            cancelConnection();
                            return true;
                        }
                        for (Node n : currentGraph.getNodes().values()) {
                            for (Node.Connection c : new ArrayList<>(n.getConnections())) {
                                if (c.getTarget().equals(node.getId())) {
                                    commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, n, c));
                                    save();
                                    return true;
                                }
                            }
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
                        long now = System.currentTimeMillis();
                        if (node.getId().equals(lastClickNodeId) && now - lastClickTime < DOUBLE_CLICK_MS) {
                            openNodeEditor(node);
                            lastClickTime = 0;
                            lastClickNodeId = null;
                            return true;
                        }
                        lastClickTime = now;
                        lastClickNodeId = node.getId();
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
                for (Node node : currentGraph.getNodes().values()) {
                    int nx = worldToScreenX(node.getX(), leftX);
                    int ny = worldToScreenY(node.getY(), leftY);
                    int nw = getNodeWidth();
                    int nh = getNodeHeight();
                    if (mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh) {
                        rightClickNodeId = node.getId();
                        return true;
                    }
                }
                for (Node node : currentGraph.getNodes().values()) {
                    for (Node.Connection conn : node.getConnections()) {
                        Node target = currentGraph.getNode(conn.getTarget());
                        if (target == null) continue;
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int outCount = getOutputCount(node);
                        int x1 = worldToScreenX(node.getX(), leftX) + nw;
                        int y1 = worldToScreenY(node.getY(), leftY) + getOutputSlotY(nh, headerH, conn.getSourceSlot(), Math.max(1, outCount));
                        int x2 = worldToScreenX(target.getX(), leftX);
                        int y2 = worldToScreenY(target.getY(), leftY) + nh / 2;
                        if (hitTestBezier(x1, y1, x2, y2, mouseX, mouseY, 6.0)) {
                            rightClickNodeId = null;
                            showConnectionContextMenu = true;
                            connectionContextMenuX = (int) mouseX;
                            connectionContextMenuY = (int) mouseY;
                            connectionContextFromId = node.getId();
                            connectionContextSlot = conn.getSourceSlot();
                            connectionContextToId = target.getId();
                            return true;
                        }
                    }
                }
                rightClickNodeId = null;
                panning = true;
                panStartX = mouseX;
                panStartY = mouseY;
                panCanvasStartX = life.canvas().x();
                panCanvasStartY = life.canvas().y();
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (life.search().box() != null && life.search().box().isFocused()) {
            return life.search().box().charTyped(codePoint, modifiers);
        }
        if (life.modals().isOpen("name")) {
            EditBox box = life.editors().box("name");
            if (box != null && box.isFocused()) return box.charTyped(codePoint, modifiers);
            return true;
        }
        if (life.modals().isOpen("nodeEditor")) {
            NodeType nodeType = getNodeTypeFor(editingNode.getType());
            if (nodeType != null) {
                for (NodeType.EditorField field : nodeType.editorFields) {
                    if (field.type == NodeType.FieldType.MULTILINE) {
                        MultiLineEditBox box = life.editors().multi(field.key);
                        if (box != null && box.isFocused()) return box.charTyped(codePoint, modifiers);
                    } else {
                        EditBox box = life.editors().box(field.key);
                        if (box != null && box.isFocused()) return box.charTyped(codePoint, modifiers);
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (life.search().box() != null && life.search().box().isFocused()) {
            return life.search().box().keyPressed(keyCode, scanCode, modifiers);
        }
        if (life.modals().isOpen("name")) {
            if (keyCode == 257 || keyCode == 335) {
                confirmNameDialog();
                return true;
            }
            if (keyCode == 256) {
                closeNameDialog();
                return true;
            }
            EditBox box = life.editors().box("name");
            if (box != null && box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (life.modals().isOpen("confirm")) {
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
        if (life.modals().isOpen("nodeEditor")) {
            if (keyCode == 256 || keyCode == 257) {
                closeNodeEditor();
                return true;
            }
            NodeType nodeType = getNodeTypeFor(editingNode.getType());
            if (nodeType != null) {
                for (NodeType.EditorField field : nodeType.editorFields) {
                    if (field.type == NodeType.FieldType.MULTILINE) {
                        MultiLineEditBox box = life.editors().multi(field.key);
                        if (box != null && box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
                    } else {
                        EditBox box = life.editors().box(field.key);
                        if (box != null && box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
                    }
                }
            }
            return true;
        }
        if ((keyCode == 259 || keyCode == 261) && selectedNodeId != null && currentGraph != null && !life.modals().isOpen("name") && !life.modals().isOpen("nodeEditor") && !life.modals().isOpen("confirm") && !showConnectionContextMenu && !contextMenu.isOpen()) {
            deleteNode(selectedNodeId);
            return true;
        }
        if (keyCode == 90 && (modifiers & 2) != 0) {
            if ((modifiers & 1) != 0) {
                if (commandStack.canRedo()) {
                    commandStack.redo();
                    save();
                }
            } else {
                if (commandStack.canUndo()) {
                    commandStack.undo();
                    save();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contextMenu.isOpen()) {
            contextMenu.mouseScrolled(delta);
            return true;
        }
        if (life.modals().isOpen("name") || life.modals().isOpen("nodeEditor") || showConnectionContextMenu || life.modals().isOpen("confirm")) return true;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;
        if (life.search().box() != null && life.search().box().isFocused() && mouseX >= life.search().box().getX() && mouseX <= life.search().box().getX() + life.search().box().getWidth() && mouseY >= life.search().box().getY() && mouseY <= life.search().box().getY() + life.search().box().getHeight()) {
            return life.search().box().mouseScrolled(mouseX, mouseY, delta);
        }
        if (mouseX >= rightX && mouseX <= x + w) {
            List<String> ids = filteredIds();
            int listH = h - 40;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            int maxScroll = Math.max(0, ids.size() - visible);
            if (delta > 0) life.selection().scroll(Math.max(0, life.selection().scroll() - 1));
            else life.selection().scroll(Math.min(life.selection().scroll() + 1, maxScroll));
            return true;
        }
        int leftX = x + 4;
        int leftY = y + 4;
        int leftW = toolbarX - x - 8;
        int leftH = h - 8;
        if (currentGraph != null && mouseX >= leftX && mouseX <= toolbarX && mouseY >= leftY && mouseY <= leftY + leftH) {
            double oldZoom = life.canvas().zoom();
            life.canvas().zoom(life.canvas().zoom() * Math.pow(1.1, delta));
            life.canvas().zoom(Math.max(0.2, Math.min(3.0, life.canvas().zoom())));
            double mx = (mouseX - leftX) / oldZoom + life.canvas().x();
            double my = (mouseY - leftY) / oldZoom + life.canvas().y();
            life.canvas().x(mx - (mouseX - leftX) / life.canvas().zoom());
            life.canvas().y(my - (mouseY - leftY) / life.canvas().zoom());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (life.modals().isOpen("name") || life.modals().isOpen("confirm") || life.modals().isOpen("nodeEditor") || showConnectionContextMenu) return true;
        if (button == 0) {
            if (draggingNodeId != null && currentGraph != null) {
                Node node = currentGraph.getNode(draggingNodeId);
                if (node != null && (node.getX() != dragStartNodeX || node.getY() != dragStartNodeY)) {
                    commandStack.execute(new NodeCommands.MoveNodeCommand(node, dragStartNodeX, dragStartNodeY, node.getX(), node.getY()));
                    save();
                }
            }
            draggingNodeId = null;
        }
        if (button == 1) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            int toolbarX = rightX - TOOLBAR_WIDTH;
            int btnSize = 24;
            int btnY = y + 8 + btnSize + 6;
            if (currentGraph != null && mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize) {
                List<String> actions = new ArrayList<>();
                Map<String, String> labels = new HashMap<>();
                for (NodeType type : getNodeTypes()) {
                    actions.add(type.id);
                    labels.put(type.id, useI18nForList() ? I18n.s(getNodeTypeKey(type.id)) : Component.translatable(getNodeTypeKey(type.id)).getString());
                }
                contextMenu.setCustomActions(actions.toArray(new String[0]));
                contextMenu.setLabelOverrides(labels);
                contextMenu.open((int) mouseX, (int) mouseY, "", false);
                return true;
            }
            if (rightClickNodeId != null && currentGraph != null) {
                Node node = currentGraph.getNode(rightClickNodeId);
                if (node != null) {
                    List<String> actions = new ArrayList<>();
                    actions.add("Edit");
                    actions.add("SetStart");
                    if (hasFollowJump(node)) actions.add("FollowJump");
                    actions.add("ClearConnections");
                    actions.add("Delete");
                    contextMenu.setCustomActions(actions.toArray(new String[0]));
                    Map<String, String> labels = new HashMap<>();
                    labels.put("Edit", useI18nForList() ? I18n.s(getNodeEditKey()) : Component.translatable(getNodeEditKey()).getString());
                    labels.put("SetStart", useI18nForList() ? I18n.s(getNodeSetStartKey()) : Component.translatable(getNodeSetStartKey()).getString());
                    labels.put("FollowJump", useI18nForList() ? I18n.s("iscript.dialog.node.follow_jump") : Component.translatable("iscript.dialog.node.follow_jump").getString());
                    labels.put("ClearConnections", useI18nForList() ? I18n.s(getNodeClearConnectionsKey()) : Component.translatable(getNodeClearConnectionsKey()).getString());
                    labels.put("Delete", useI18nForList() ? I18n.s(getNodeDeleteKey()) : Component.translatable(getNodeDeleteKey()).getString());
                    contextMenu.setLabelOverrides(labels);
                    contextMenu.open((int) mouseX, (int) mouseY, rightClickNodeId, false);
                }
                rightClickNodeId = null;
                return true;
            }
            if (contextMenu.isOpen()) {
                contextMenu.close();
                return true;
            }
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            for (int i = life.selection().scroll(); i < Math.min(life.selection().scroll() + visible, ids.size()); i++) {
                int rowY = listY + (i - life.selection().scroll()) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                    contextMenu.clearCustom();
                    contextMenu.open((int) mouseX, (int) mouseY, ids.get(i), canPaste);
                    return true;
                }
            }
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= listY && mouseY <= listY + listH) {
                boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                if (canPaste) {
                    contextMenu.clearCustom();
                    contextMenu.open((int) mouseX, (int) mouseY, "", canPaste);
                }
                return true;
            }
            panning = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            life.canvas().x(panCanvasStartX + (panStartX - mouseX) / life.canvas().zoom());
            life.canvas().y(panCanvasStartY + (panStartY - mouseY) / life.canvas().zoom());
            return true;
        }
        if (draggingNodeId != null && currentGraph != null) {
            Node node = currentGraph.getNode(draggingNodeId);
            if (node != null) {
                double dx = (mouseX - dragStartMouseX) / life.canvas().zoom();
                double dy = (mouseY - dragStartMouseY) / life.canvas().zoom();
                node.setX((int) (dragStartNodeX + dx));
                node.setY((int) (dragStartNodeY + dy));
            }
            return true;
        }
        return false;
    }
}