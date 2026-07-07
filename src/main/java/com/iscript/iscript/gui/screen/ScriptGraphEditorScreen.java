package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.script.ScriptNodeConnection;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.gui.widget.ScriptNodeWidget;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveScriptGraphPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ScriptGraphEditorScreen extends Screen {
    private final String graphId;
    private ScriptGraphData graph;
    private final List<ScriptNodeWidget> nodeWidgets = new ArrayList<>();
    private ScriptNodeWidget draggingNode = null;
    private ScriptNodeWidget connectingFrom = null;
    private int connectingSlot = -1;
    private int canvasX = 0, canvasY = 0;
    private boolean panning = false;
    private int panStartX, panStartY;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private ScriptNodeWidget contextMenuNode = null;

    private boolean showNodeEditor = false;
    private boolean showNodeTypePicker = false;
    private int pickerX, pickerY;
    private ScriptNodeWidget editingNode = null;
    private final List<EditBox> paramBoxes = new ArrayList<>();
    private final List<Component> paramLabels = new ArrayList<>();

    private static final int NODE_PICKER_COLS = 4;

    public ScriptGraphEditorScreen(String graphId, ScriptGraphData graph) {
        super(Component.literal("Script Graph Editor: " + graphId));
        this.graphId = graphId;
        this.graph = graph;
    }

    @Override
    protected void init() {
        this.nodeWidgets.clear();
        this.paramBoxes.clear();
        this.paramLabels.clear();
        for (ScriptNodeData node : graph.getNodes().values()) {
            ScriptNodeWidget widget = new ScriptNodeWidget(this, node, node.getX(), node.getY());
            this.nodeWidgets.add(widget);
            this.addRenderableWidget(widget);
        }

        this.addRenderableWidget(Button.builder(Component.literal("+ Node"), btn -> showTypePicker())
                .pos(10, 10).size(60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveGraph())
                .pos(80, 10).size(60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> closeEditor())
                .pos(150, 10).size(60, 20).build());
    }

    private void showTypePicker() {
        showNodeTypePicker = true;
        pickerX = this.width / 2 - 160;
        pickerY = this.height / 2 - 120;
    }

    private void closeEditor() {
        saveGraph();
        this.onClose();
    }

    @Override
    public void onClose() {
        saveGraph();
        super.onClose();
    }

    private void addNode(ScriptNodeType type) {
        String nodeId = "node_" + System.currentTimeMillis();
        ScriptNodeData node = new ScriptNodeData();
        node.setId(nodeId);
        node.setType(type);
        node.setX(200 + canvasX);
        node.setY(200 + canvasY);
        setDefaultParams(node);
        graph.addNode(node);
        ScriptNodeWidget widget = new ScriptNodeWidget(this, node, node.getX(), node.getY());
        nodeWidgets.add(widget);
        this.addRenderableWidget(widget);
        showNodeTypePicker = false;
    }

    private void setDefaultParams(ScriptNodeData node) {
        switch (node.getType()) {
            case DELAY -> node.setParam("ticks", "20");
            case CAMERA -> {
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
                node.setParam("yaw", "0");
                node.setParam("pitch", "0");
                node.setParam("duration", "60");
            }
            case DIALOG -> node.setParam("dialogId", "");
            case GIVE_ITEM -> {
                node.setParam("itemId", "diamond");
                node.setParam("count", "1");
            }
            case SPAWN_ENTITY -> {
                node.setParam("entityType", "minecraft:zombie");
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
            }
            case SET_BLOCK -> {
                node.setParam("blockId", "minecraft:stone");
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
            }
            case PLAY_SOUND -> {
                node.setParam("soundId", "minecraft:block.note_block.pling");
                node.setParam("x", "~");
                node.setParam("y", "~");
                node.setParam("z", "~");
            }
            case RUN_COMMAND -> node.setParam("command", "say Hello");
            case PARTICLE -> {
                node.setParam("particleId", "minecraft:flame");
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
            }
            case TELEPORT -> {
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
            }
            case SET_GAMEMODE -> node.setParam("mode", "survival");
            case SET_HEALTH -> node.setParam("health", "20");
            case NPC_ANIMATE -> {
                node.setParam("entityId", "");
                node.setParam("animation", "idle");
            }
            case NPC_MOVE -> {
                node.setParam("entityId", "");
                node.setParam("x", "0");
                node.setParam("y", "64");
                node.setParam("z", "0");
            }
            case QUEST_START, QUEST_COMPLETE -> node.setParam("questId", "");
            case SET_DATA -> {
                node.setParam("key", "");
                node.setParam("value", "");
            }
            case SET_FACTION -> node.setParam("faction", "neutral");
            case SET_REPUTATION -> node.setParam("value", "0");
            case WORLD_SET -> {
                node.setParam("key", "");
                node.setParam("value", "");
                node.setParam("dataType", "string");
            }
            case SCRIPT_JS -> node.setParam("script", "// JS code");
            case IF -> {
                node.setParam("conditionType", "has_item");
                node.setParam("value", "");
                node.setParam("compare", "");
                node.setParam("count", "1");
                node.setParam("amount", "0");
                node.setParam("chance", "0.5");
            }
            default -> {}
        }
    }

    private void saveGraph() {
        for (ScriptNodeWidget widget : nodeWidgets) {
            widget.syncToData();
        }
        IScriptNetwork.sendToServer(new SaveScriptGraphPacket(graph));
    }

    public void startConnection(ScriptNodeWidget from, int slot) {
        this.connectingFrom = from;
        this.connectingSlot = slot;
    }

    public void endConnection(ScriptNodeWidget to) {
        if (connectingFrom != null && connectingFrom != to) {
            ScriptNodeConnection conn = new ScriptNodeConnection();
            conn.setSourceNodeId(connectingFrom.getNode().getId());
            conn.setTargetNodeId(to.getNode().getId());
            conn.setSourceSlot(connectingSlot);
            if (connectingFrom.getNode().getType() == ScriptNodeType.IF) {
                conn.setConditionValue(connectingSlot == 0 ? "true" : "false");
            }
            connectingFrom.getNode().getConnections().add(conn);
        }
        connectingFrom = null;
        connectingSlot = -1;
    }

    public void cancelConnection() {
        connectingFrom = null;
        connectingSlot = -1;
    }

    public void startDragging(ScriptNodeWidget node) {
        this.draggingNode = node;
    }

    public void deleteNode(ScriptNodeWidget widget) {
        graph.removeNode(widget.getNode().getId());
        nodeWidgets.remove(widget);
        this.removeWidget(widget);
    }

    public void openNodeEditor(ScriptNodeWidget widget) {
        this.showNodeEditor = true;
        this.editingNode = widget;
        this.paramBoxes.clear();
        this.paramLabels.clear();

        int ex = this.width / 2 - 120;
        int ey = this.height / 2 - 100;
        int py = ey + 20;
        int idx = 0;
        for (var e : widget.getNode().getParams().entrySet()) {
            EditBox box = new EditBox(this.font, ex + 80, py, 150, 16, Component.literal(e.getKey()));
            box.setMaxLength(512);
            box.setValue(e.getValue());
            box.setVisible(true);
            this.paramBoxes.add(box);
            this.paramLabels.add(Component.literal(e.getKey() + ":"));
            this.addRenderableWidget(box);
            py += 22;
            idx++;
            if (idx > 10) break;
        }
        this.showContextMenu = false;
    }

    private void closeNodeEditor() {
        if (editingNode != null) {
            int i = 0;
            for (var e : editingNode.getNode().getParams().entrySet()) {
                if (i < paramBoxes.size()) {
                    e.setValue(paramBoxes.get(i).getValue());
                }
                i++;
            }
            editingNode.setHeight(ScriptNodeWidget.getHeight(editingNode.getNode()));
        }
        for (EditBox box : paramBoxes) {
            this.removeWidget(box);
        }
        this.paramBoxes.clear();
        this.paramLabels.clear();
        this.showNodeEditor = false;
        this.editingNode = null;
        saveGraph();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNodeTypePicker) {
            int cols = NODE_PICKER_COLS;
            ScriptNodeType[] types = ScriptNodeType.values();
            int rows = (types.length + cols - 1) / cols;
            int cellW = 100;
            int cellH = 22;
            for (int i = 0; i < types.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int cx = pickerX + col * cellW;
                int cy = pickerY + row * cellH;
                if (mouseX >= cx && mouseX <= cx + cellW - 2 && mouseY >= cy && mouseY <= cy + cellH - 2) {
                    addNode(types[i]);
                    return true;
                }
            }
            showNodeTypePicker = false;
            return true;
        }

        if (showNodeEditor) {
            int ex = this.width / 2 - 120;
            int ey = this.height / 2 - 100;
            if (mouseX >= ex - 10 && mouseX <= ex + 240 && mouseY >= ey - 10 && mouseY <= ey + 280) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            closeNodeEditor();
            return true;
        }

        if (showContextMenu) {
            if (mouseX < contextMenuX || mouseX > contextMenuX + 100 ||
                    mouseY < contextMenuY || mouseY > contextMenuY + 70) {
                showContextMenu = false;
                contextMenuNode = null;
            }
            return true;
        }

        if (button == 1) {
            for (ScriptNodeWidget widget : nodeWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    showContextMenu = true;
                    contextMenuX = (int) mouseX;
                    contextMenuY = (int) mouseY;
                    contextMenuNode = widget;
                    return true;
                }
            }
            panning = true;
            panStartX = (int) mouseX;
            panStartY = (int) mouseY;
            return true;
        }
        if (connectingFrom != null) {
            for (ScriptNodeWidget widget : nodeWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    endConnection(widget);
                    return true;
                }
            }
            cancelConnection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (showContextMenu && contextMenuNode != null) {
                int y = contextMenuY + 2;
                if (mouseX >= contextMenuX && mouseX <= contextMenuX + 100) {
                    if (mouseY >= y && mouseY <= y + 20) {
                        openNodeEditor(contextMenuNode);
                        return true;
                    }
                    y += 22;
                    if (mouseY >= y && mouseY <= y + 20) {
                        deleteNode(contextMenuNode);
                        showContextMenu = false;
                        return true;
                    }
                }
            }
            if (draggingNode != null) {
                draggingNode = null;
                return true;
            }
        }
        if (button == 1) {
            panning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            int dx = (int) (mouseX - panStartX);
            int dy = (int) (mouseY - panStartY);
            canvasX += dx;
            canvasY += dy;
            panStartX = (int) mouseX;
            panStartY = (int) mouseY;
            for (ScriptNodeWidget widget : nodeWidgets) {
                widget.moveBy(dx, dy);
            }
            return true;
        }
        if (draggingNode != null) {
            draggingNode.moveBy((int) dragX, (int) dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (showNodeTypePicker) {
                showNodeTypePicker = false;
                return true;
            }
            if (showNodeEditor) {
                closeNodeEditor();
                return true;
            }
            closeEditor();
            return true;
        }
        if (showNodeEditor && keyCode == 257) {
            closeNodeEditor();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(0, 40, this.width, this.height, 0xFF1E1E1E);

        for (ScriptNodeWidget widget : nodeWidgets) {
            for (ScriptNodeConnection conn : widget.getNode().getConnections()) {
                ScriptNodeWidget target = findWidget(conn.getTargetNodeId());
                if (target != null) {
                    int x1 = widget.getOutputX(conn.getSourceSlot());
                    int y1 = widget.getOutputY(conn.getSourceSlot());
                    int x2 = target.getInputX();
                    int y2 = target.getInputY();
                    int color = conn.getConditionValue().equalsIgnoreCase("false") ? 0xFFAA4444 : 0xFF888888;
                    drawArrow(graphics, x1, y1, x2, y2, color);
                }
            }
        }

        if (connectingFrom != null) {
            int x1 = connectingFrom.getOutputX(connectingSlot);
            int y1 = connectingFrom.getOutputY(connectingSlot);
            drawArrow(graphics, x1, y1, mouseX, mouseY, 0xFF44AAFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (showContextMenu && contextMenuNode != null) {
            renderContextMenu(graphics, mouseX, mouseY);
        }

        if (showNodeEditor && editingNode != null) {
            renderNodeEditor(graphics);
        }

        if (showNodeTypePicker) {
            renderNodeTypePicker(graphics, mouseX, mouseY);
        }

        graphics.drawString(this.font, "RMB=pan | Drag header=move | Drag arrow=connect | RMB node=menu | ESC=save&exit", 10, this.height - 15, 0xFF888888);
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int w = 100;
        int h = 44;
        graphics.fill(contextMenuX, contextMenuY, contextMenuX + w, contextMenuY + h, 0xFF222222);
        graphics.renderOutline(contextMenuX, contextMenuY, w, h, 0xFF666666);

        int y = contextMenuY + 2;
        boolean hoveredEdit = mouseX >= contextMenuX && mouseX <= contextMenuX + w && mouseY >= y && mouseY <= y + 20;
        graphics.fill(contextMenuX + 1, y, contextMenuX + w - 1, y + 20, hoveredEdit ? 0xFF444444 : 0xFF222222);
        graphics.drawString(font, "Edit", contextMenuX + 4, y + 6, hoveredEdit ? 0xFFFFFFFF : 0xFFCCCCCC);
        y += 22;

        boolean hoveredDelete = mouseX >= contextMenuX && mouseX <= contextMenuX + w && mouseY >= y && mouseY <= y + 20;
        graphics.fill(contextMenuX + 1, y, contextMenuX + w - 1, y + 20, hoveredDelete ? 0xFF664444 : 0xFF222222);
        graphics.drawString(font, "Delete", contextMenuX + 4, y + 6, hoveredDelete ? 0xFFFF6666 : 0xFFCCCCCC);
    }

    private void renderNodeEditor(GuiGraphics graphics) {
        int x = this.width / 2 - 120;
        int y = this.height / 2 - 100;
        int h = Math.max(120, 30 + paramBoxes.size() * 22);

        graphics.fill(x - 10, y - 10, x + 240, y + h, 0xFF2A2A3A);
        graphics.renderOutline(x - 10, y - 10, 250, h + 10, 0xFF6688FF);

        graphics.drawString(font, "Edit Node (ESC or Enter to save)", x, y, 0xFFFFFFFF);

        int py = y + 20;
        for (int i = 0; i < paramLabels.size(); i++) {
            graphics.drawString(font, paramLabels.get(i), x, py, 0xFFAAAAAA);
            py += 22;
        }
    }

    private void renderNodeTypePicker(GuiGraphics graphics, int mouseX, int mouseY) {
        ScriptNodeType[] types = ScriptNodeType.values();
        int cols = NODE_PICKER_COLS;
        int rows = (types.length + cols - 1) / cols;
        int cellW = 100;
        int cellH = 22;
        int pw = cols * cellW + 4;
        int ph = rows * cellH + 4;

        graphics.fill(pickerX - 4, pickerY - 20, pickerX + pw, pickerY + ph, 0xFF2A2A3A);
        graphics.renderOutline(pickerX - 4, pickerY - 20, pw + 4, ph + 24, 0xFF6688FF);
        graphics.drawString(font, "Select Node Type", pickerX, pickerY - 16, 0xFFFFFFFF);

        for (int i = 0; i < types.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = pickerX + col * cellW;
            int cy = pickerY + row * cellH;
            boolean hovered = mouseX >= cx && mouseX <= cx + cellW - 2 && mouseY >= cy && mouseY <= cy + cellH - 2;
            int bg = hovered ? 0xFF444466 : 0xFF333344;
            graphics.fill(cx, cy, cx + cellW - 2, cy + cellH - 2, bg);
            graphics.drawString(font, types[i].name(), cx + 2, cy + 5, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
        }
    }

    private void drawArrow(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        graphics.fill(x1, y1 - 1, midX, y1 + 1, color);
        graphics.fill(midX, Math.min(y1, y2) - 1, midX + 1, Math.max(y1, y2) + 1, color);
        graphics.fill(midX, y2 - 1, x2, y2 + 1, color);

        if (x2 > x1) {
            graphics.fill(x2 - 6, y2 - 3, x2, y2 + 3, color);
            graphics.fill(x2 - 4, y2 - 5, x2, y2 + 5, color);
        } else {
            graphics.fill(x2, y2 - 3, x2 + 6, y2 + 3, color);
            graphics.fill(x2, y2 - 5, x2 + 4, y2 + 5, color);
        }
    }

    private ScriptNodeWidget findWidget(String nodeId) {
        for (ScriptNodeWidget widget : nodeWidgets) {
            if (widget.getNode().getId().equals(nodeId)) return widget;
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}