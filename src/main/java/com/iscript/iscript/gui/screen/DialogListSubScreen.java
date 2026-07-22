package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.Node;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int RIGHT_PANEL_WIDTH = 140;
    private static final int TOOLBAR_WIDTH = 32;
    private String selectedId = null;
    private EditBox searchBox = null;

    private boolean showNameDialog = false;
    private EditBox nameInputBox = null;
    private int nameDialogY = 0;
    private String nameDialogMode = "";
    private String renameOldId = null;

    private Graph currentDialog = null;
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

    private boolean showConnectionContextMenu = false;
    private int connectionContextMenuX, connectionContextMenuY;
    private String connectionContextFromId = null;
    private int connectionContextSlot = -1;
    private String connectionContextToId = null;

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
    private Node editingNode = null;
    private EditBox nodeEditTitleBox = null;
    private EditBox nodeEditTextBox = null;
    private EditBox nodeEditPortraitBox = null;
    private int nodeEditorY = 0;

    public DialogListSubScreen(DashboardScreen parent) {
        super(parent);
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
        showConnectionContextMenu = false;
        editingNode = null;
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        if (searchBox == null && this.minecraft != null) {
            createSearchBox();
        }
        closeNodeEditor();
        closeNameDialog();
    }

    private void createSearchBox() {
        if (this.minecraft == null) return;
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        searchBox = new EditBox(this.minecraft.font, rightX + 4, y + 4, RIGHT_PANEL_WIDTH - 8, 16, I18n.t("iscript.dialog.list.search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(Theme.TEXT);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);
    }

    private List<String> filteredIds() {
        var dialogs = DataAccess.dialogGraphs();
        String filter = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<String> result = new ArrayList<>();
        for (var e : dialogs.entrySet()) {
            String name = e.getValue().getName();
            if (name == null) name = "";
            name = name.trim();
            if (filter.isEmpty() || name.toLowerCase().contains(filter)) {
                result.add(e.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }

    private void openNameDialog(String mode, String oldId) {
        nameDialogMode = mode;
        renameOldId = oldId;
        showNameDialog = true;
        int x = DashboardScreen.SIDEBAR_W;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int cx = x + w / 2;
        nameDialogY = this.parent.height / 2 - 40;

        nameInputBox = new EditBox(this.minecraft.font, cx - 100, nameDialogY + 20, 200, 20, I18n.t("iscript.dialog.editor.name"));
        nameInputBox.setMaxLength(64);
        nameInputBox.setTextColor(Theme.TEXT);
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
            Graph dialog = new Graph(null);
            dialog.setId(id);
            dialog.setName(name);
            Node start = new Node(null);
            start.setId("start");
            start.setParam("title", I18n.s("iscript.dialog.node.type.start"));
            start.setParam("text", I18n.s("iscript.dialog.node.default.start_text"));
            start.setX(200);
            start.setY(200);
            dialog.addNode(start);
            DataAccess.putDialogGraph(dialog);
            switchToDialog(id);
        } else if (nameDialogMode.equals("rename") && renameOldId != null) {
            Graph oldDialog = DataAccess.dialogGraph(renameOldId);
            if (oldDialog != null) {
                Graph newDialog = oldDialog.copy();
                newDialog.setId(id);
                newDialog.setName(name);
                DataAccess.putDialogGraph(newDialog);
                DataAccess.removeDialogGraph(renameOldId);
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
        showConnectionContextMenu = false;
        closeNodeEditor();

        currentDialog = DataAccess.dialogGraph(id);
        canvasX = 0;
        canvasY = 0;
        zoom = 1.0;
    }

    private void runDialog() {
        if (selectedId == null || currentDialog == null) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_DIALOG, ServerCommandPacket.requestDialogToTag(selectedId)));
    }

    private void addNode(String type) {
        if (currentDialog == null) return;
        String nodeId = "node_" + System.currentTimeMillis();
        Node node = new Node(null);
        node.setId(nodeId);
        switch (type) {
            case "start" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.start"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.start_text"));
            }
            case "npc" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.npc"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.npc_text"));
            }
            case "player" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.player"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.player_text"));
            }
            case "choice" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.choice"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.choice_text"));
            }
            case "condition" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.condition"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.condition_text"));
            }
            case "action" -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.action"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.action_text"));
            }
            default -> {
                node.setParam("title", I18n.s("iscript.dialog.node.type.start"));
                node.setParam("text", I18n.s("iscript.dialog.node.default.text"));
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

    private void openNodeEditor(Node node) {
        showNodeEditor = true;
        editingNode = node;
        nodeEditorY = this.parent.height / 2 - 70;

        nodeEditTitleBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, I18n.t("iscript.dialog.node.title"));
        nodeEditTitleBox.setValue(node.getParam("title"));
        nodeEditTextBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, I18n.t("iscript.dialog.node.text"));
        nodeEditTextBox.setValue(node.getParam("text"));
        nodeEditPortraitBox = new EditBox(this.minecraft.font, 0, 0, 180, 18, I18n.t("iscript.dialog.node.portrait"));
        nodeEditPortraitBox.setValue(node.getParam("portrait"));

        parent.addWidget(nodeEditTitleBox);
        parent.addWidget(nodeEditTextBox);
        parent.addWidget(nodeEditPortraitBox);
        parent.setFocusedWidget(nodeEditTitleBox);
    }

    private void closeNodeEditor() {
        if (editingNode != null) {
            editingNode.setParam("title", nodeEditTitleBox.getValue());
            editingNode.setParam("text", nodeEditTextBox.getValue());
            editingNode.setParam("portrait", nodeEditPortraitBox.getValue());
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
            DataAccess.removeDialogGraph(confirmDialogId);
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
        Graph source = DataAccess.dialogGraph(sourceId);
        if (source == null) return;

        String baseId = sourceId + "_copy";
        String newId = baseId;
        int counter = 1;
        while (DataAccess.dialogGraph(newId) != null) {
            newId = baseId + "_" + counter;
            counter++;
        }

        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (Copy)");
        DataAccess.putDialogGraph(copy);
        switchToDialog(newId);
    }

    private void duplicateItem(String id) {
        Graph source = DataAccess.dialogGraph(id);
        if (source == null) return;

        String baseId = id;
        String newId = id + "_1";
        int counter = 1;
        while (DataAccess.dialogGraph(newId) != null) {
            counter++;
            newId = baseId + "_" + counter;
        }

        Graph copy = source.copy();
        copy.setId(newId);
        copy.setName(source.getName() + " (" + counter + ")");
        DataAccess.putDialogGraph(copy);
        switchToDialog(newId);
    }

    @Override
    public void tick() {
        if (searchBox == null && this.minecraft != null) {
            createSearchBox();
        }
        super.tick();
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

    private void drawBezierCurve(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
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

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f matrix = graphics.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double inv = 1.0 - t;
            double t2 = t * t;
            double inv2 = inv * inv;

            double x = inv2 * inv * x1 + 3 * inv2 * t * cp1x + 3 * inv * t2 * cp2x + t2 * t * x2;
            double y = inv2 * inv * y1 + 3 * inv2 * t * cp1y + 3 * inv * t2 * cp2y + t2 * t * y2;

            drawSmoothPoint(graphics, matrix, buf, (float) x, (float) y, r, g, b, a, 1.8f);
        }

        tess.end();
    }


    private void renderConnection(GuiGraphics graphics, Node from, Node to, int sourceSlot, int leftX, int leftY) {
        int nw = getNodeWidth();
        int nh = getNodeHeight();
        int headerH = getNodeHeaderHeight();
        int x1 = worldToScreenX(from.getX(), leftX) + nw;
        int y1 = worldToScreenY(from.getY(), leftY) + getOutputSlotY(nh, headerH, sourceSlot, Math.max(1, from.getConnections().size()));
        int x2 = worldToScreenX(to.getX(), leftX);
        int y2 = worldToScreenY(to.getY(), leftY) + nh / 2;

        int color = Theme.TEXT_DIM;
        drawBezierCurve(graphics, x1, y1, x2, y2, color);

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
        boolean isStart = currentDialog != null && node.getId().equals(currentDialog.getStartNodeId());

        graphics.fill(nx + 2, ny + 2, nx + nw + 2, ny + nh + 2, Theme.alpha(Theme.BG_PANEL, 0.27f));

        int bodyColor = selected ? Theme.ACCENT : Theme.BG_PANEL;
        UI.panel(graphics, nx, ny, nw, nh);
        graphics.renderOutline(nx, ny, nw, nh, selected ? Theme.BORDER_ACCENT : (isStart ? Theme.ACCENT : Theme.BORDER));

        int headerColor = isStart ? Theme.ACCENT : Theme.BG_INNER;
        UI.inner(graphics, nx, ny, nw, headerH);
        String title = font.plainSubstrByWidth(node.getParam("title"), nw - 8);
        graphics.drawString(font, title, nx + 4, ny + (headerH - 8) / 2, Theme.TEXT);

        if (nh > 30) {
            String text = node.getParam("text");
            if (text.length() > 30) text = text.substring(0, 30) + "...";
            text = font.plainSubstrByWidth(text, nw - 8);
            graphics.drawString(font, text, nx + 4, ny + headerH + 4, Theme.TEXT_DIM);
        }

        int pinR = Math.max(3, (int) (4 * zoom));
        int inX = nx;
        int inY = ny + nh / 2;
        graphics.fill(inX - pinR, inY - pinR, inX + pinR, inY + pinR, Theme.ACCENT);
        graphics.renderOutline(inX - pinR, inY - pinR, pinR * 2, pinR * 2, Theme.BORDER_ACCENT);

        int outputs = Math.max(1, node.getConnections().size());
        int maxSlots = 3;
        for (int i = 0; i < Math.min(outputs, maxSlots); i++) {
            int slotY = ny + getOutputSlotY(nh, headerH, i, Math.min(outputs, maxSlots));
            int outX = nx + nw;
            boolean active = connectingFromId != null && connectingFromId.equals(node.getId()) && connectingSlot == i;
            int pinColor = active ? Theme.ACCENT : Theme.ERROR;
            graphics.fill(outX - pinR, slotY - pinR, outX + pinR, slotY + pinR, pinColor);
            graphics.renderOutline(outX - pinR, slotY - pinR, pinR * 2, pinR * 2, active ? Theme.BORDER_ACCENT : Theme.ERROR);
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
            graphics.fill(gx, leftY, gx + 1, leftY + leftH, Theme.BG_INNER);
        }
        for (int gy = leftY + offsetY; gy < leftY + leftH; gy += gridSize) {
            graphics.fill(leftX, gy, leftX + leftW, gy + 1, Theme.BG_INNER);
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

            if (currentDialog != null) {
                boolean addHovered = mouseX >= toolbarX + 4 && mouseX <= toolbarX + TOOLBAR_WIDTH - 4 && mouseY >= btnY && mouseY <= btnY + btnSize;
                graphics.fill(toolbarX + 4, btnY, toolbarX + TOOLBAR_WIDTH - 4, btnY + btnSize, addHovered ? Theme.BG_HOVER : Theme.BG_INNER);
                graphics.renderOutline(toolbarX + 4, btnY, TOOLBAR_WIDTH - 8, btnSize, Theme.BORDER);
                graphics.drawCenteredString(this.font, "+", toolbarX + TOOLBAR_WIDTH / 2, btnY + (btnSize - 8) / 2, addHovered ? Theme.TEXT : Theme.TEXT_DIM);
            }

            if (searchBox != null) {
                searchBox.setX(rightX + 4);
                searchBox.setY(y + 4);
                searchBox.setWidth(RIGHT_PANEL_WIDTH - 8);
                searchBox.setHeight(16);
                searchBox.setVisible(true);
            }

            graphics.fill(rightX, y, x + w, y + h, Theme.BG_INNER);
            graphics.renderOutline(rightX, y, RIGHT_PANEL_WIDTH, h, Theme.BG_HOVER);
            graphics.drawString(this.font, I18n.s("iscript.dialog.list.title"), rightX + 8, y + 26, Theme.ACCENT);

            List<String> ids = filteredIds();

            int listH = h - 68;
            int listY = y + 42;
            int visible = Math.max(1, listH / ITEM_HEIGHT);

            for (int i = scroll; i < Math.min(scroll + visible, ids.size()); i++) {
                String id = ids.get(i);
                int rowY = listY + (i - scroll) * ITEM_HEIGHT;
                boolean hovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= rowY && mouseY <= rowY + ITEM_HEIGHT - 2;
                boolean selected = id.equals(selectedId);

                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
                graphics.fill(rightX + 4, rowY, x + w - 4, rowY + ITEM_HEIGHT - 2, bg);
                graphics.drawString(this.font, id, rightX + 8, rowY + 4, selected ? Theme.ACCENT : Theme.TEXT);
            }

            int newY = y + h - 28;
            boolean newHovered = mouseX >= rightX + 4 && mouseX <= x + w - 4 && mouseY >= newY && mouseY <= newY + 22;
            graphics.fill(rightX + 4, newY, x + w - 4, newY + 22, newHovered ? Theme.BG_HOVER : Theme.BG_INNER);
            graphics.renderOutline(rightX + 4, newY, RIGHT_PANEL_WIDTH - 8, 22, Theme.BORDER);
            graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.list.new"), rightX + RIGHT_PANEL_WIDTH / 2, newY + 6, Theme.ACCENT);

            if (selectedId != null && currentDialog != null) {
                graphics.fill(leftX - 2, leftY - 2, leftX + leftW + 2, leftY + leftH + 2, Theme.BG_INNER);
                graphics.renderOutline(leftX - 2, leftY - 2, leftW + 4, leftH + 4, Theme.BORDER);

                RenderSystem.enableScissor(leftX * (int) this.minecraft.getWindow().getGuiScale(), (this.minecraft.getWindow().getGuiScaledHeight() - leftY - leftH) * (int) this.minecraft.getWindow().getGuiScale(), leftW * (int) this.minecraft.getWindow().getGuiScale(), leftH * (int) this.minecraft.getWindow().getGuiScale());

                renderGrid(graphics, leftX, leftY, leftW, leftH);

                for (Node node : currentDialog.getNodes().values()) {
                    for (Node.Connection conn : node.getConnections()) {
                        Node target = currentDialog.getNode(conn.getTarget());
                        if (target != null) {
                            renderConnection(graphics, node, target, conn.getSourceSlot(), leftX, leftY);
                        }
                    }
                }

                if (connectingFromId != null) {
                    Node from = currentDialog.getNode(connectingFromId);
                    if (from != null) {
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int x1 = worldToScreenX(from.getX(), leftX) + nw;
                        int y1 = worldToScreenY(from.getY(), leftY) + getOutputSlotY(nh, headerH, connectingSlot, Math.max(1, from.getConnections().size()));
                        int x2 = mouseX;
                        int y2 = mouseY;
                        drawBezierCurve(graphics, x1, y1, x2, y2, Theme.ACCENT);
                    }
                }

                for (Node node : currentDialog.getNodes().values()) {
                    renderNode(graphics, node, leftX, leftY, mouseX, mouseY);
                }

                RenderSystem.disableScissor();

                graphics.drawString(font, String.format("%.0f%%", zoom * 100), leftX + 4, leftY + 4, Theme.TEXT_MUTE);
            } else {
                graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.list.empty"), leftX + leftW / 2, y + h / 2, Theme.TEXT_MUTE);
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

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_PANEL, 0.8f));
            UI.panel(graphics, dx, dy, dw, dh);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ACCENT);
            graphics.drawCenteredString(this.font, I18n.s(nameDialogMode.equals("rename") ? "iscript.dialog.editor.rename" : "iscript.dialog.editor.new"), cx, dy + 6, Theme.ACCENT);

            if (nameInputBox != null) {
                nameInputBox.setX(cx - 100);
                nameInputBox.setY(dy + 24);
            }

            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx - 50, dy + 52, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, I18n.s(nameDialogMode.equals("rename") ? "iscript.dialog.editor.rename_btn" : "iscript.dialog.editor.create"), cx - 26, dy + 57, okHovered ? Theme.ACCENT : Theme.TEXT_DIM);

            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 52 && mouseY <= dy + 74;
            UI.buttonBg(graphics, cx + 2, dy + 52, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.editor.cancel"), cx + 26, dy + 57, cancelHovered ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (showConfirmDialog) {
            int cx = x + w / 2;
            int dw = 220;
            int dh = 70;
            int dx = cx - dw / 2;
            int dy = confirmDialogY;

            graphics.fill(dx - 4, dy - 4, dx + dw + 4, dy + dh + 4, Theme.alpha(Theme.BG_PANEL, 0.8f));
            UI.panel(graphics, dx, dy, dw, dh);
            graphics.renderOutline(dx, dy, dw, dh, Theme.ERROR);
            graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.editor.delete_confirm", confirmDialogId), cx, dy + 8, Theme.ERROR);

            boolean okHovered = mouseX >= cx - 50 && mouseX <= cx - 2 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx - 50, dy + 38, 48, 22, okHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.editor.delete"), cx - 26, dy + 43, okHovered ? Theme.ERROR : Theme.TEXT_DIM);

            boolean cancelHovered = mouseX >= cx + 2 && mouseX <= cx + 50 && mouseY >= dy + 38 && mouseY <= dy + 60;
            UI.buttonBg(graphics, cx + 2, dy + 38, 48, 22, cancelHovered, true);
            graphics.drawCenteredString(this.font, I18n.s("iscript.dialog.editor.cancel"), cx + 26, dy + 43, cancelHovered ? Theme.TEXT : Theme.TEXT_DIM);
        }

        if (showNodeEditor && editingNode != null) {
            int cx = x + w / 2;
            int dx = cx - 100;
            int dy = nodeEditorY;

            UI.panel(graphics, dx - 10, dy - 10, 210, 135);
            graphics.renderOutline(dx - 10, dy - 10, 210, 135, Theme.BORDER_ACCENT);
            graphics.drawString(font, I18n.s("iscript.dialog.node.edit_title"), dx, dy, Theme.TEXT);

            graphics.drawString(font, I18n.s("iscript.dialog.node.title"), dx, dy + 18, Theme.TEXT_DIM);
            nodeEditTitleBox.setX(dx);
            nodeEditTitleBox.setY(dy + 30);
            nodeEditTitleBox.setVisible(true);

            graphics.drawString(font, I18n.s("iscript.dialog.node.text"), dx, dy + 52, Theme.TEXT_DIM);
            nodeEditTextBox.setX(dx);
            nodeEditTextBox.setY(dy + 64);
            nodeEditTextBox.setVisible(true);

            graphics.drawString(font, I18n.s("iscript.dialog.node.portrait"), dx, dy + 86, Theme.TEXT_DIM);
            nodeEditPortraitBox.setX(dx);
            nodeEditPortraitBox.setY(dy + 98);
            nodeEditPortraitBox.setVisible(true);
        }

        if (showContextMenu && contextMenuNodeId != null) {
            int wctx = 100;
            int hctx = 88;
            UI.panel(graphics, contextMenuX, contextMenuY, wctx, hctx);

            int cy = contextMenuY + 2;
            boolean h1 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h1);
            graphics.drawString(font, I18n.s("iscript.dialog.node.edit"), contextMenuX + 4, cy + 6, h1 ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;

            boolean h2 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h2);
            graphics.drawString(font, I18n.s("iscript.dialog.node.set_start"), contextMenuX + 4, cy + 6, h2 ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;

            boolean h3 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h3);
            graphics.drawString(font, I18n.s("iscript.dialog.node.clear_connections"), contextMenuX + 4, cy + 6, h3 ? Theme.ERROR : Theme.TEXT_DIM);
            cy += 22;

            boolean h4 = mouseX >= contextMenuX && mouseX <= contextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, contextMenuX + 1, cy, wctx - 2, 20, false, h4);
            graphics.drawString(font, I18n.s("iscript.dialog.node.delete"), contextMenuX + 4, cy + 6, h4 ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (showConnectionContextMenu && connectionContextFromId != null) {
            int wctx = 100;
            int hctx = 24;
            UI.panel(graphics, connectionContextMenuX, connectionContextMenuY, wctx, hctx);
            boolean h1 = mouseX >= connectionContextMenuX && mouseX <= connectionContextMenuX + wctx && mouseY >= connectionContextMenuY + 2 && mouseY <= connectionContextMenuY + 22;
            UI.row(graphics, connectionContextMenuX + 1, connectionContextMenuY + 2, wctx - 2, 20, false, h1);
            graphics.drawString(font, I18n.s("iscript.dialog.connection.delete"), connectionContextMenuX + 4, connectionContextMenuY + 6, h1 ? Theme.ERROR : Theme.TEXT_DIM);
        }

        if (showNodeTypeMenu) {
            renderNodeTypeMenu(graphics, mouseX, mouseY);
        }

        if (showItemContextMenu && itemContextMenuId != null) {
            String[] items = {"Copy", "Paste", "Rename", "Duplicate", "Delete"};
            boolean canPaste = DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty();
            int wctx = 100;
            int hctx = (canPaste ? 5 : 4) * 22 + 4;
            UI.panel(graphics, itemContextMenuX, itemContextMenuY, wctx, hctx);

            int cy = itemContextMenuY + 2;
            for (String item : items) {
                if (item.equals("Paste") && !canPaste) continue;
                boolean hovered = mouseX >= itemContextMenuX && mouseX <= itemContextMenuX + wctx && mouseY >= cy && mouseY <= cy + 20;
                UI.row(graphics, itemContextMenuX + 1, cy, wctx - 2, 20, false, hovered);
                int tc = hovered ? Theme.TEXT : Theme.TEXT_DIM;
                if (item.equals("Delete")) tc = hovered ? Theme.ERROR : Theme.TEXT_DIM;
                String key = "iscript.dialog.item." + item.toLowerCase();
                graphics.drawString(font, I18n.s(key), itemContextMenuX + 6, cy + 6, tc);
                cy += 22;
            }
        }
    }

    private void renderNodeTypeMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] types = {"start", "npc", "player", "choice", "condition", "action"};
        int w = 110;
        int h = types.length * 22 + 4;
        UI.panel(graphics, nodeTypeMenuX, nodeTypeMenuY, w, h);

        int cy = nodeTypeMenuY + 2;
        for (String type : types) {
            boolean hovered = mouseX >= nodeTypeMenuX && mouseX <= nodeTypeMenuX + w && mouseY >= cy && mouseY <= cy + 20;
            UI.row(graphics, nodeTypeMenuX + 1, cy, w - 2, 20, false, hovered);
            graphics.drawString(font, I18n.s("iscript.dialog.node.type." + type), nodeTypeMenuX + 6, cy + 6, hovered ? Theme.TEXT : Theme.TEXT_DIM);
            cy += 22;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNameDialog) {
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
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
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
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
            int x = DashboardScreen.SIDEBAR_W;
            int y = DashboardScreen.TOPBAR_H;
            int w = this.parent.width - DashboardScreen.SIDEBAR_W;
            int h = this.parent.height - DashboardScreen.TOPBAR_H;
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

        if (showConnectionContextMenu) {
            if (mouseX >= connectionContextMenuX && mouseX <= connectionContextMenuX + 100 && mouseY >= connectionContextMenuY + 2 && mouseY <= connectionContextMenuY + 22) {
                if (connectionContextFromId != null && currentDialog != null) {
                    Node from = currentDialog.getNode(connectionContextFromId);
                    if (from != null) {
                        from.getConnections().removeIf(c -> c.getSourceSlot() == connectionContextSlot && c.getTarget().equals(connectionContextToId));
                        saveDialog();
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
                    Node node = currentDialog != null ? currentDialog.getNode(contextMenuNodeId) : null;
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
                    if (currentDialog != null) {
                        Node node = currentDialog.getNode(contextMenuNodeId);
                        if (node != null) {
                            node.getConnections().clear();
                            for (Node n : currentDialog.getNodes().values()) {
                                n.getConnections().removeIf(c -> c.getTarget().equals(contextMenuNodeId));
                            }
                            saveDialog();
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

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (searchBox != null && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth() && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
            searchBox.setFocused(true);
            parent.setFocusedWidget(searchBox);
            return searchBox.mouseClicked(mouseX, mouseY, button);
        }

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

        List<String> ids = filteredIds();

        int listH = h - 68;
        int listY = y + 42;
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
                List<Node> nodes = new ArrayList<>(currentDialog.getNodes().values());
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    Node node = nodes.get(i);
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
                            Node from = currentDialog.getNode(connectingFromId);
                            if (from != null) {
                                Node.Connection conn = new Node.Connection();
                                conn.setTarget(node.getId());
                                conn.setSourceSlot(connectingSlot);
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
                for (Node node : currentDialog.getNodes().values()) {
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
                for (Node node : currentDialog.getNodes().values()) {
                    for (Node.Connection conn : node.getConnections()) {
                        Node target = currentDialog.getNode(conn.getTarget());
                        if (target == null) continue;
                        int nw = getNodeWidth();
                        int nh = getNodeHeight();
                        int headerH = getNodeHeaderHeight();
                        int x1 = worldToScreenX(node.getX(), leftX) + nw;
                        int y1 = worldToScreenY(node.getY(), leftY) + getOutputSlotY(nh, headerH, conn.getSourceSlot(), Math.max(1, node.getConnections().size()));
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
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
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
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
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

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;
        int rightX = x + w - RIGHT_PANEL_WIDTH;
        int toolbarX = rightX - TOOLBAR_WIDTH;

        if (mouseX >= rightX && mouseX <= x + w) {
            var dialogs = DataAccess.dialogGraphs();
            List<String> ids = new ArrayList<>(dialogs.keySet());
            int listH = h - 68;
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
            zoom *= Math.pow(1.1, delta);
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
                Node node = currentDialog.getNode(draggingNodeId);
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
            Node node = currentDialog.getNode(draggingNodeId);
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