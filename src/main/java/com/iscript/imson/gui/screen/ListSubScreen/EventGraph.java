package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.data.Node;
import com.iscript.imson.data.script.ScriptNodeType;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.SubScreenLifecycle;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.gui.widget.ContextMenu;
import com.iscript.imson.gui.undo.CommandStack;
import com.iscript.imson.gui.undo.NodeCommands;
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
import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventGraph extends DashboardScreen.SubScreen {
    private final SubScreenLifecycle life = new SubScreenLifecycle(this);
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;

    private Graph currentGraph = null;
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

    private boolean showConnectionContextMenu = false;
    private int connectionContextMenuX, connectionContextMenuY;
    private String connectionContextFromId = null;
    private int connectionContextSlot = -1;
    private String connectionContextToId = null;

    private ContextMenu contextMenu = new ContextMenu();

    private boolean showNodeTypeMenu = false;
    private int nodeTypeMenuX, nodeTypeMenuY;

    private Node editingNode = null;

    private final Map<String, Graph> clientCache = new HashMap<>();
    private final CommandStack commandStack = new CommandStack();
    private long lastClickTime = 0;
    private String lastClickNodeId = null;
    private static final long DOUBLE_CLICK_MS = 400;
    private String hoveredConnectionFromId = null;
    private int hoveredConnectionSlot = -1;
    private String hoveredConnectionToId = null;

    public EventGraph(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        showContextMenu = false;
        showConnectionContextMenu = false;
        showNodeTypeMenu = false;
        hoveredConnectionFromId = null;

        String editingNodeId = life.state().modalFieldValues.get("editingNodeId");
        if (editingNodeId != null && currentGraph != null) {
            editingNode = currentGraph.getNode(editingNodeId);
        }

        life.init();

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        life.search().request(rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.event.list.search"));

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
                        Graph oldGraph = DataAccess.eventGraph(oldId);
                        initial = oldGraph != null ? oldGraph.getName() : oldId;
                    }
                    life.editors().addBox("name", cx - 100, dy + 20, 200, 20, I18n.t("iscript.event.graph.name"), initial);
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
                    ScriptNodeType type;
                    try { type = ScriptNodeType.valueOf(editingNode.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
                    String[] params = getNodeParams(type);
                    for (String key : params) {
                        if (key.equals("script") && ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                            life.editors().addMultiBox("script", cx, dy + 20, 180, 120, I18n.t("iscript.event.param." + key), editingNode.getParam(key));
                        } else {
                            life.editors().addBox(key, cx, dy + 20, 180, 18, I18n.t("iscript.event.param." + key), editingNode.getParam(key));
                        }
                        dy += 40;
                    }
                    parent.setFocusedWidget(life.editors().box(params[0]));
                },
                () -> {
                    if (editingNode != null) {
                        ScriptNodeType type;
                        try { type = ScriptNodeType.valueOf(editingNode.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
                        String[] params = getNodeParams(type);
                        for (String key : params) {
                            String oldValue = editingNode.getParam(key);
                            String newValue;
                            if (key.equals("script") && ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                                MultiLineEditBox box = life.editors().multi("script");
                                newValue = box != null ? box.getValue() : oldValue;
                            } else {
                                EditBox box = life.editors().box(key);
                                newValue = box != null ? box.getValue() : oldValue;
                            }
                            if (!oldValue.equals(newValue)) {
                                commandStack.execute(new NodeCommands.EditNodeParamCommand(editingNode, key, oldValue, newValue));
                            }
                        }
                        saveGraph();
                    }
                    if (editingNode != null) {
                        ScriptNodeType type;
                        try { type = ScriptNodeType.valueOf(editingNode.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
                        String[] params = getNodeParams(type);
                        for (String key : params) {
                            if (key.equals("script") && ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                                life.editors().remove("script");
                            } else {
                                life.editors().remove(key);
                            }
                        }
                    }
                    editingNode = null;
                    life.state().modalFieldValues.remove("editingNodeId");
                },
                "script", "eventType", "label", "condition", "ticks", "branches", "count");

        life.modals().register("confirm",
                () -> life.modals().isOpen("confirm"),
                v -> {},
                () -> {},
                () -> {},
                "confirmAction", "confirmId");

        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_EVENT_GRAPHS, new CompoundTag()));
    }

    private List<String> filteredIds() {
        Map<String, Graph> graphs = DataAccess.eventGraphs();
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

    private void openNameDialog(String mode, String oldId) {
        life.state().modalFieldValues.put("nameMode", mode);
        life.state().modalFieldValues.put("renameOldId", oldId);
        life.modals().open("name");
    }

    private void closeNameDialog() {
        life.modals().close("name");
    }

    private void confirmNameDialog() {
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
            Graph graph = new Graph(ScriptNodeType.class);
            graph.setId(id);
            graph.setName(name);
            Node start = new Node(ScriptNodeType.class);
            start.setId("start");
            start.setType(ScriptNodeType.START.name());
            start.setParam("eventType", "TICK");
            start.setParam("label", I18n.s("iscript.event.node.type.event"));
            start.setX(200);
            start.setY(200);
            graph.addNode(start);
            graph.setStartNodeId("start");
            DataAccess.putEventGraph(graph);
            saveGraph();
            switchToGraph(id);
        } else if (mode.equals("rename") && oldId != null) {
            Graph oldGraph = DataAccess.eventGraph(oldId);
            if (oldGraph != null) {
                Graph newGraph = oldGraph.copy();
                newGraph.setId(id);
                newGraph.setName(name);
                DataAccess.removeEventGraph(oldId);
                DataAccess.putEventGraph(newGraph);
                clientCache.remove(oldId);
                clientCache.put(id, newGraph);
                life.state().savedCanvasPositions.remove(oldId);
                if (life.selection().get() != null && life.selection().get().equals(oldId)) {
                    life.selection().set(id);
                    currentGraph = newGraph;
                }
                saveGraph();
                if (life.selection().get() != null && life.selection().get().equals(id)) {
                    switchToGraph(id);
                }
            }
        }
    }

    private void switchToGraph(String id) {
        if (life.selection().get() != null && currentGraph != null) {
            life.canvas().saveFor(life.selection().get());
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_EVENT_GRAPH, ServerCommandPacket.saveEventGraphToTag(currentGraph)));
        }
        life.selection().set(id);
        selectedNodeId = null;
        draggingNodeId = null;
        cancelConnection();
        showContextMenu = false;
        showNodeTypeMenu = false;
        showConnectionContextMenu = false;
        closeNodeEditor();
        currentGraph = clientCache.get(id);
        if (currentGraph == null) {
            currentGraph = DataAccess.eventGraph(id);
        }
        if (currentGraph == null) {
            currentGraph = new Graph(ScriptNodeType.class);
            currentGraph.setId(id);
            currentGraph.setName(id);
            Node start = new Node(ScriptNodeType.class);
            start.setId("start");
            start.setType(ScriptNodeType.START.name());
            start.setParam("eventType", "TICK");
            start.setParam("label", I18n.s("iscript.event.node.type.event"));
            start.setX(200);
            start.setY(200);
            currentGraph.addNode(start);
            currentGraph.setStartNodeId("start");
            clientCache.put(id, currentGraph);
        }
        life.canvas().loadFor(id);
    }

    private void addNode(String typeStr) {
        if (currentGraph == null) return;
        String nodeId = "node_" + System.currentTimeMillis();
        Node node = new Node(ScriptNodeType.class);
        node.setId(nodeId);
        switch (typeStr) {
            case "event" -> {
                node.setType(ScriptNodeType.START.name());
                node.setParam("eventType", "TICK");
                node.setParam("label", I18n.s("iscript.event.node.type.event"));
            }
            case "trigger" -> {
                node.setType(ScriptNodeType.TRIGGER.name());
                node.setParam("eventType", "TICK");
            }
            case "condition" -> {
                node.setType(ScriptNodeType.IF.name());
                node.setParam("condition", "true");
            }
            case "action" -> {
                node.setType(ScriptNodeType.SCRIPT_JS.name());
                node.setParam("script", "");
            }
            case "delay" -> {
                node.setType(ScriptNodeType.DELAY.name());
                node.setParam("ticks", "20");
            }
            case "random" -> {
                node.setType(ScriptNodeType.RANDOM.name());
                node.setParam("branches", "2");
            }
            case "loop" -> {
                node.setType(ScriptNodeType.LOOP.name());
                node.setParam("count", "3");
            }
            case "stop" -> {
                node.setType(ScriptNodeType.STOP.name());
            }
            default -> node.setType(ScriptNodeType.SCRIPT_JS.name());
        }
        node.setX((int) (life.canvas().x() + 200));
        node.setY((int) (life.canvas().y() + 200));
        commandStack.execute(new NodeCommands.AddNodeCommand(currentGraph, node));
        saveGraph();
    }

    private void saveGraph() {
        if (currentGraph == null || life.selection().get() == null) return;
        clientCache.put(life.selection().get(), currentGraph);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_EVENT_GRAPH, ServerCommandPacket.saveEventGraphToTag(currentGraph)));
    }

    private void deleteNode(String nodeId) {
        if (currentGraph == null) return;
        Node node = currentGraph.getNode(nodeId);
        if (node == null) return;
        commandStack.execute(new NodeCommands.DeleteNodeCommand(currentGraph, node));
        if (selectedNodeId != null && selectedNodeId.equals(nodeId)) selectedNodeId = null;
        saveGraph();
    }

    private void cancelConnection() {
        connectingFromId = null;
        connectingSlot = -1;
    }

    private void openNodeEditor(Node node) {
        editingNode = node;
        life.state().modalFieldValues.put("editingNodeId", node.getId());
        life.modals().open("nodeEditor");
    }

    private void closeNodeEditor() {
        life.modals().close("nodeEditor");
    }

    private void openConfirmDialog(String action, String id) {
        life.state().modalFieldValues.put("confirmAction", action);
        life.state().modalFieldValues.put("confirmId", id);
        life.modals().open("confirm");
    }

    private void closeConfirmDialog() {
        life.modals().close("confirm");
    }

    private void executeConfirm() {
        String action = life.state().modalFieldValues.getOrDefault("confirmAction", "");
        String confirmId = life.state().modalFieldValues.get("confirmId");
        if ("delete".equals(action) && confirmId != null) {
            DataAccess.removeEventGraph(confirmId);
            clientCache.remove(confirmId);
            life.state().savedCanvasPositions.remove(confirmId);
            if (life.selection().get() != null && life.selection().get().equals(confirmId)) {
                life.selection().set(null);
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
        Graph source = DataAccess.eventGraph(sourceId);
        if (source == null) return;
        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (DataAccess.eventGraph(newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }
        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (Copy)");
        DataAccess.putEventGraph(copy);
        switchToGraph(newId);
    }

    private void duplicateItem(String id) {
        Graph source = DataAccess.eventGraph(id);
        if (source == null) return;
        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (DataAccess.eventGraph(newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }
        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (" + counter + ")");
        DataAccess.putEventGraph(copy);
        switchToGraph(newId);
    }

    @Override
    public void tick() {
        life.tick(this::saveGraph);
    }

    @Override
    public void removed() {
        if (life.selection().get() != null) {
            life.canvas().saveFor(life.selection().get());
        }
        life.removed();
        commandStack.clear();
        super.removed();
    }

    private int getNodeWidth() {
        return Math.max(60, (int) (140 * life.canvas().zoom()));
    }

    private int getNodeHeight() {
        return Math.max(30, (int) (78 * life.canvas().zoom()));
    }

    private int getNodeHeaderHeight() {
        return Math.max(10, (int) (18 * life.canvas().zoom()));
    }

    private int worldToScreenX(int wx, int leftX) {
        return (int) ((wx - life.canvas().x()) * life.canvas().zoom() + leftX);
    }

    private int worldToScreenY(int wy, int leftY) {
        return (int) ((wy - life.canvas().y()) * life.canvas().zoom() + leftY);
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

    private boolean hitTestBezier(int x1, int y1, int x2, int y2, double mx, double my, double threshold) {
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

    private void drawSmoothPoint(GuiGraphics graphics, Matrix4f matrix, BufferBuilder buf, float x, float y, float r, float g, float b, float a, float size) {
        float s = size * 0.5f;
        buf.vertex(matrix, x - s, y - s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x - s, y + s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + s, y + s, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + s, y - s, 0).color(r, g, b, a).endVertex();
    }

    private void drawBezierCurve(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, boolean hovered) {
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

    private int getOutputCount(Node node) {
        ScriptNodeType type;
        try { type = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { return 1; }
        return switch (type) {
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

    private String getOutputLabel(Node node, int slot) {
        ScriptNodeType type;
        try { type = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { return ""; }
        return switch (type) {
            case IF -> slot == 0 ? I18n.s("iscript.event.node.output.true") : I18n.s("iscript.event.node.output.false");
            case LOOP -> slot == 0 ? I18n.s("iscript.event.node.output.body") : I18n.s("iscript.event.node.output.done");
            case RANDOM -> I18n.s("iscript.event.node.output.out", slot + 1);
            default -> "";
        };
    }

    private void renderConnection(GuiGraphics graphics, Node from, Node to, int sourceSlot, int leftX, int leftY, boolean hovered) {
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        int outCount = getOutputCount(from);
        int x1 = worldToScreenX(from.getX(), leftX) + nw;
        int y1 = worldToScreenY(from.getY(), leftY) + getOutputSlotY(nh, headerH, sourceSlot, Math.max(1, outCount));
        int x2 = worldToScreenX(to.getX(), leftX);
        int y2 = worldToScreenY(to.getY(), leftY) + nh / 2;
        int color = hovered ? Theme.ACCENT : Theme.TEXT_DIM;
        drawBezierCurve(graphics, x1, y1, x2, y2, color, hovered);
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

    private void renderNode(GuiGraphics graphics, Node node, int leftX, int leftY, int mouseX, int mouseY) {
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
        ScriptNodeType nodeType;
        try { nodeType = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { nodeType = ScriptNodeType.STOP; }
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

    private int getNodeHeaderColor(ScriptNodeType type) {
        return switch (type) {
            case START -> Theme.ACCENT;
            case TRIGGER -> Theme.TEXT_DIM;
            case IF -> Theme.TEXT;
            case SCRIPT_JS -> Theme.ACCENT;
            case DELAY -> Theme.TEXT_DIM;
            case RANDOM -> Theme.TEXT_DIM;
            case LOOP -> Theme.TEXT_DIM;
            case STOP -> Theme.ERROR;
            default -> Theme.BG_INNER;
        };
    }

    private String getNodeTitle(Node node) {
        ScriptNodeType type;
        try { type = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
        return switch (type) {
            case START -> I18n.s("iscript.event.node.type.event");
            case TRIGGER -> I18n.s("iscript.event.node.type.trigger");
            case IF -> I18n.s("iscript.event.node.type.condition");
            case SCRIPT_JS -> I18n.s("iscript.event.node.type.action");
            case DELAY -> I18n.s("iscript.event.node.type.delay");
            case RANDOM -> I18n.s("iscript.event.node.type.random");
            case LOOP -> I18n.s("iscript.event.node.type.loop");
            case STOP -> I18n.s("iscript.event.node.type.stop");
            default -> node.getType();
        };
    }

    private String getNodeBodyText(Node node) {
        ScriptNodeType type;
        try { type = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
        return switch (type) {
            case START, TRIGGER -> node.getParam("eventType");
            case IF -> node.getParam("condition");
            case SCRIPT_JS -> node.getParam("script");
            case DELAY -> I18n.s("iscript.event.node.body.ticks", node.getParam("ticks"));
            case RANDOM -> I18n.s("iscript.event.node.body.branches", node.getParam("branches"));
            case LOOP -> I18n.s("iscript.event.node.body.times", node.getParam("count"));
            default -> "";
        };
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

    private void renderGrid(GuiGraphics graphics, int leftX, int leftY, int leftW, int leftH) {
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

    private void updateHoveredConnection(int mouseX, int mouseY, int leftX, int leftY) {
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
            graphics.fill(x, y, x + w, y + h, Theme.BG_INNER);
            graphics.fill(toolbarX, y, rightX, y + h, Theme.BG_PANEL);
            graphics.renderOutline(toolbarX, y, TOOLBAR_WIDTH, h, Theme.BG_HOVER);
            int btnSize = 24;
            int btnY = y + 8;
            boolean runHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
            graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, runHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
            graphics.drawCenteredString(this.font, "\u25B6", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, runHovered ? Theme.ACCENT : 0xFF44AA44);
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
            graphics.drawString(this.font, I18n.s("iscript.event.list.title"), rightX + 8, y + 26, Theme.ACCENT);
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
            graphics.drawCenteredString(this.font, I18n.s("iscript.event.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);
            if (life.selection().get() != null && currentGraph != null) {
                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);
                updateHoveredConnection(mouseX, mouseY, leftX, leftY);
                RenderSystem.enableScissor(leftX * (int) this.minecraft.getWindow().getGuiScale(), (this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * (int) this.minecraft.getWindow().getGuiScale(), leftW * (int) this.minecraft.getWindow().getGuiScale(), leftH * (int) this.minecraft.getWindow().getGuiScale());
                renderGrid(graphics, leftX, leftY, leftW, leftH);
                for (Node node : currentGraph.getNodes().values()) {
                    for (Node.Connection conn : node.getConnections()) {
                        Node target = currentGraph.getNode(conn.getTarget());
                        if (target != null) {
                            boolean hovered = node.getId().equals(hoveredConnectionFromId) && conn.getSourceSlot() == hoveredConnectionSlot && conn.getTarget().equals(hoveredConnectionToId);
                            renderConnection(graphics, node, target, conn.getSourceSlot(), leftX, leftY, hovered);
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
                graphics.drawCenteredString(this.font, I18n.s("iscript.event.list.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
            }
        } else {
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
            graphics.drawCenteredString(this.font, I18n.s("iscript.event.editor.delete_confirm", confirmId), cx, dy + 8, Theme.ERROR);
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx - 50, dy + 38, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.event.editor.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : Theme.TEXT_DIM);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx + 2, dy + 38, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.event.editor.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT_DIM);
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
            graphics.drawCenteredString(this.font, I18n.s(mode.equals("rename") ? "iscript.event.editor.rename" : "iscript.event.editor.new"), cx, dy + 6, Theme.ACCENT);
            EditBox nameInputBox = life.editors().box("name");
            if (nameInputBox != null) {
                nameInputBox.setX(cx - 100);
                nameInputBox.setY(dy + 24);
            }
            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx - 50, dy + 52, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, I18n.s(mode.equals("rename") ? "iscript.event.editor.rename_btn" : "iscript.event.editor.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : Theme.TEXT_DIM);
            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx + 2, dy + 52, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.event.editor.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (life.modals().isOpen("nodeEditor") && editingNode != null) {
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = this.parent.height / 2 - 80;
            ScriptNodeType type;
            try { type = ScriptNodeType.valueOf(editingNode.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
            String[] params = getNodeParams(type);
            int editorH = 30 + params.length * 40;
            if (ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                editorH = 30 + 40 + 120;
            }
            UI.panel(graphics, dx - 10, dy - 10, 210, editorH + 20);
            graphics.renderOutline(dx - 10, dy - 10, 210, editorH + 20, Theme.BORDER_ACCENT);
            graphics.drawString(font, I18n.s("iscript.event.node.edit_title"), dx, dy, Theme.TEXT);
            int py = dy + 20;
            for (int i = 0; i < params.length; i++) {
                UI.label(graphics, font, I18n.s("iscript.event.param." + params[i]) + ":", dx, py);
                if (params[i].equals("script") && ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                    MultiLineEditBox box = life.editors().multi("script");
                    if (box != null) { box.setX(dx); box.setY(py + 12); box.setVisible(true); }
                } else {
                    EditBox box = life.editors().box(params[i]);
                    if (box != null) { box.setX(dx); box.setY(py + 12); box.setVisible(true); }
                }
                py += 40;
            }
        }

        if (showContextMenu && contextMenuNodeId != null) {
            int wctx = 100;
            int hctx = 88;
            UI.panel(graphics, contextMenuX, contextMenuY, wctx, hctx);
            int cy = contextMenuY + 2;
            boolean h1 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h1);
            graphics.drawString(font, I18n.s("iscript.event.node.edit"), contextMenuX + 4, cy + 6, h1 ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;
            boolean h2 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h2);
            graphics.drawString(font, I18n.s("iscript.event.node.set_start"), contextMenuX + 4, cy + 6, h2 ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;
            boolean h3 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h3);
            graphics.drawString(font, I18n.s("iscript.event.node.clear_connections"), contextMenuX + 4, cy + 6, h3 ? Theme.ERROR : Theme.TEXT_DIM);
            cy += 22;
            boolean h4 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h4);
            graphics.drawString(font, I18n.s("iscript.event.node.delete"), contextMenuX + 4, cy + 6, h4 ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (showConnectionContextMenu && connectionContextFromId != null) {
            int wctx = 100;
            int hctx = 24;
            UI.panel(graphics, connectionContextMenuX, connectionContextMenuY, wctx, hctx);
            boolean h1 = mouseX >= connectionContextMenuX && mouseX <= connectionContextMenuX + wctx && mouseY >= connectionContextMenuY + 2 && mouseY <= connectionContextMenuY + 22;
            UI.row(graphics, connectionContextMenuX + 1, connectionContextMenuY + 2, wctx - 2, 20, false, h1);
            graphics.drawString(font, I18n.s("iscript.event.connection.delete"), connectionContextMenuX + 4, connectionContextMenuY + 6, h1 ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (showNodeTypeMenu) {
            renderNodeTypeMenu(graphics, mouseX, mouseY);
        }

        if (contextMenu.isOpen()) {
            contextMenu.render(graphics, this.font, mouseX, mouseY);
        }
    }

    private void runGraph() {
        if (life.selection().get() == null || currentGraph == null) return;
        saveGraph();
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.RUN_EVENT_GRAPH, ServerCommandPacket.runEventToTag(life.selection().get())));
    }

    private void renderNodeTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] types = {"event", "trigger", "condition", "action", "delay", "random", "loop", "stop"};
        int w = 110;
        int h = types.length * 22 + 4;
        UI.panel(graphics, nodeTypeMenuX, nodeTypeMenuY, w, h);
        int cy = nodeTypeMenuY + 2;
        for (String type : types) {
            boolean hovered = mouseX >= nodeTypeMenuX && mouseX <= nodeTypeMenuX + w && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, nodeTypeMenuX + 1, cy, w - 2, 20, false, hovered);
            graphics.drawString(font, I18n.s("iscript.event.node.type." + type), nodeTypeMenuX + 6, cy + 6, hovered ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;
        }
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
            ScriptNodeType type;
            try { type = ScriptNodeType.valueOf(editingNode.getType()); } catch (Exception e) { type = ScriptNodeType.STOP; }
            String[] params = getNodeParams(type);
            int editorH = 30 + params.length * 40;
            if (ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                editorH = 30 + 40 + 120;
            }
            if (mouseX >= dx - 10 && mouseX <= dx + 200 && mouseY >= dy - 10 && mouseY <= dy + editorH) {
                for (String key : params) {
                    if (key.equals("script") && ScriptNodeType.SCRIPT_JS.name().equals(editingNode.getType())) {
                        MultiLineEditBox box = life.editors().multi("script");
                        if (box != null && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth() && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight()) {
                            parent.setFocusedWidget(box);
                            return box.mouseClicked(mouseX, mouseY, button);
                        }
                    } else {
                        EditBox box = life.editors().box(key);
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
                                saveGraph();
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

        if (showContextMenu) {
            int y = contextMenuY + 2;
            if (mouseX >= contextMenuX && mouseX <= contextMenuX + 100) {
                if (mouseY >= y && mouseY <= y + 20) {
                    Node node = currentGraph != null ? currentGraph.getNode(contextMenuNodeId) : null;
                    if (node != null) openNodeEditor(node);
                    showContextMenu = false;
                    return true;
                }
                y += 22;
                if (mouseY >= y && mouseY <= y + 20) {
                    if (currentGraph != null) {
                        String oldStart = currentGraph.getStartNodeId();
                        commandStack.execute(new NodeCommands.SetStartNodeCommand(currentGraph, oldStart, contextMenuNodeId));
                        saveGraph();
                    }
                    showContextMenu = false;
                    return true;
                }
                y += 22;
                if (mouseY >= y && mouseY <= y + 20) {
                    if (currentGraph != null) {
                        Node node = currentGraph.getNode(contextMenuNodeId);
                        if (node != null) {
                            for (Node.Connection c : new ArrayList<>(node.getConnections())) {
                                commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, node, c));
                            }
                            for (Node n : currentGraph.getNodes().values()) {
                                for (Node.Connection c : new ArrayList<>(n.getConnections())) {
                                    if (c.getTarget().equals(contextMenuNodeId)) {
                                        commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, n, c));
                                    }
                                }
                            }
                            saveGraph();
                        }
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

        if (contextMenu.isOpen()) {
            String itemId = contextMenu.getItemId();
            contextMenu.mouseClicked(mouseX, mouseY, button);
            String action = contextMenu.getLastAction();
            if (action != null && itemId != null) {
                switch (action) {
                    case "Copy" -> copyItem(itemId);
                    case "Paste" -> pasteItem();
                    case "Rename" -> openNameDialog("rename", itemId);
                    case "Duplicate" -> duplicateItem(itemId);
                    case "Delete" -> openConfirmDialog("delete", itemId);
                }
            }
            contextMenu.close();
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
                showNodeTypeMenu = true;
                nodeTypeMenuX = (int) mouseX;
                nodeTypeMenuY = (int) mouseY;
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
                                saveGraph();
                            }
                            cancelConnection();
                            return true;
                        }
                        for (Node n : currentGraph.getNodes().values()) {
                            for (Node.Connection c : new ArrayList<>(n.getConnections())) {
                                if (c.getTarget().equals(node.getId())) {
                                    commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, n, c));
                                    saveGraph();
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
                            commandStack.execute(new NodeCommands.DisconnectNodesCommand(currentGraph, node, conn));
                            saveGraph();
                            return true;
                        }
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
                        showContextMenu = true;
                        contextMenuNodeId = node.getId();
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
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
            MultiLineEditBox scriptBox = life.editors().multi("script");
            if (scriptBox != null && scriptBox.isFocused()) return scriptBox.charTyped(codePoint, modifiers);
            for (String pid : new String[]{"eventType", "label", "condition", "ticks", "branches", "count"}) {
                EditBox box = life.editors().box(pid);
                if (box != null && box.isFocused()) return box.charTyped(codePoint, modifiers);
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
            MultiLineEditBox scriptBox = life.editors().multi("script");
            if (scriptBox != null && scriptBox.isFocused()) return scriptBox.keyPressed(keyCode, scanCode, modifiers);
            for (String pid : new String[]{"eventType", "label", "condition", "ticks", "branches", "count"}) {
                EditBox box = life.editors().box(pid);
                if (box != null && box.isFocused()) return box.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if ((keyCode == 259 || keyCode == 261) && selectedNodeId != null && currentGraph != null && !life.modals().isOpen("name") && !life.modals().isOpen("nodeEditor") && !life.modals().isOpen("confirm") && !showContextMenu && !showNodeTypeMenu && !showConnectionContextMenu && !contextMenu.isOpen()) {
            deleteNode(selectedNodeId);
            return true;
        }
        if (keyCode == 90 && (modifiers & 2) != 0) {
            if ((modifiers & 1) != 0) {
                if (commandStack.canRedo()) {
                    commandStack.redo();
                    saveGraph();
                }
            } else {
                if (commandStack.canUndo()) {
                    commandStack.undo();
                    saveGraph();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (life.modals().isOpen("name") || life.modals().isOpen("nodeEditor") || showContextMenu || showNodeTypeMenu || showConnectionContextMenu || life.modals().isOpen("confirm") || contextMenu.isOpen()) return true;
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
        if (life.modals().isOpen("name") || life.modals().isOpen("confirm") || showContextMenu || life.modals().isOpen("nodeEditor") || showNodeTypeMenu || showConnectionContextMenu) return true;
        if (button == 0) {
            if (draggingNodeId != null && currentGraph != null) {
                Node node = currentGraph.getNode(draggingNodeId);
                if (node != null && (node.getX() != dragStartNodeX || node.getY() != dragStartNodeY)) {
                    commandStack.execute(new NodeCommands.MoveNodeCommand(node, dragStartNodeX, dragStartNodeY, node.getX(), node.getY()));
                    saveGraph();
                }
            }
            draggingNodeId = null;
        }
        if (button == 1) {
            if (contextMenu.isOpen()) {
                contextMenu.close();
                return true;
            }
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
            int rightX = x + w - RIGHT_PANEL_WIDTH;
            List<String> ids = filteredIds();
            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);
            for (int i = life.selection().scroll(); i < Math.min(life.selection().scroll() + visible, ids.size()); i++) {
                int rowY = listY + (i - life.selection().scroll()) * ITEM_HEIGHT;
                if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2) {
                    boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                    contextMenu.open((int) mouseX, (int) mouseY, ids.get(i), canPaste);
                    return true;
                }
            }
            if (mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= listY && mouseY <= listY + listH) {
                boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
                if (canPaste) {
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