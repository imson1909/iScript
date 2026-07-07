package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.dialog.DialogGraphData;
import com.iscript.iscript.data.dialog.DialogNodeData;
import com.iscript.iscript.gui.widget.DialogNodeWidget;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveDialogGraphPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DialogGraphEditorScreen extends Screen {
    private final String graphId;
    private DialogGraphData graph;
    private final List<DialogNodeWidget> nodeWidgets = new ArrayList<>();
    private DialogNodeWidget draggingNode = null;
    private DialogNodeWidget connectingFrom = null;
    private int connectingSlot = -1;
    private int canvasX = 0, canvasY = 0;
    private boolean panning = false;
    private int panStartX, panStartY;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private DialogNodeWidget contextMenuNode = null;

    private boolean showNodeEditor = false;
    private int editorX, editorY;
    private DialogNodeWidget editingNode = null;
    private EditBox editorTitleBox;
    private EditBox editorTextBox;
    private EditBox editorPortraitBox;

    public DialogGraphEditorScreen(String graphId, DialogGraphData graph) {
        super(Component.literal("Dialog Graph Editor: " + graphId));
        this.graphId = graphId;
        this.graph = graph;
    }

    @Override
    protected void init() {
        this.nodeWidgets.clear();
        for (DialogNodeData node : graph.getNodes().values()) {
            DialogNodeWidget widget = new DialogNodeWidget(this, node, node.getX(), node.getY());
            this.nodeWidgets.add(widget);
            this.addRenderableWidget(widget);
        }

        this.addRenderableWidget(Button.builder(Component.literal("+ Node"), btn -> addNode())
                .pos(10, 10).size(60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> closeEditor())
                .pos(80, 10).size(60, 20).build());

        this.editorTitleBox = new EditBox(this.font, 0, 0, 180, 18, Component.literal("Title"));
        this.editorTextBox = new EditBox(this.font, 0, 0, 180, 18, Component.literal("Text"));
        this.editorPortraitBox = new EditBox(this.font, 0, 0, 180, 18, Component.literal("Portrait"));
        this.editorTitleBox.setVisible(false);
        this.editorTextBox.setVisible(false);
        this.editorPortraitBox.setVisible(false);
        this.addRenderableWidget(this.editorTitleBox);
        this.addRenderableWidget(this.editorTextBox);
        this.addRenderableWidget(this.editorPortraitBox);
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

    private void addNode() {
        String nodeId = "node_" + System.currentTimeMillis();
        DialogNodeData node = new DialogNodeData();
        node.setId(nodeId);
        node.setTitle("New Node");
        node.setText("Enter text here...");
        node.setX(200 + canvasX);
        node.setY(200 + canvasY);
        graph.addNode(node);
        DialogNodeWidget widget = new DialogNodeWidget(this, node, node.getX(), node.getY());
        nodeWidgets.add(widget);
        this.addRenderableWidget(widget);
    }

    private void saveGraph() {
        for (DialogNodeWidget widget : nodeWidgets) {
            widget.syncToData();
        }
        IScriptNetwork.sendToServer(new SaveDialogGraphPacket(graph));
    }

    public void startConnection(DialogNodeWidget from, int slot) {
        this.connectingFrom = from;
        this.connectingSlot = slot;
    }

    public void endConnection(DialogNodeWidget to) {
        if (connectingFrom != null && connectingFrom != to) {
            DialogNodeData.NodeConnection conn = new DialogNodeData.NodeConnection();
            conn.setOptionText("Option " + (connectingSlot + 1));
            conn.setTargetNodeId(to.getNode().getId());
            conn.setSourceSlot(connectingSlot);
            connectingFrom.getNode().getConnections().add(conn);
        }
        connectingFrom = null;
        connectingSlot = -1;
    }

    public void cancelConnection() {
        connectingFrom = null;
        connectingSlot = -1;
    }

    public void startDragging(DialogNodeWidget node) {
        this.draggingNode = node;
    }

    public void setStartNode(String nodeId) {
        graph.setStartNodeId(nodeId);
    }

    public void deleteNode(DialogNodeWidget widget) {
        graph.removeNode(widget.getNode().getId());
        nodeWidgets.remove(widget);
        this.removeWidget(widget);
    }

    public boolean isStartNode(String nodeId) {
        return graph.getStartNodeId().equals(nodeId);
    }

    public void openNodeEditor(DialogNodeWidget widget) {
        this.showNodeEditor = true;
        this.editingNode = widget;
        this.editorX = this.width / 2 - 100;
        this.editorY = this.height / 2 - 70;

        this.editorTitleBox.setValue(widget.getNode().getTitle());
        this.editorTitleBox.setVisible(true);
        this.editorTitleBox.setFocused(true);

        this.editorTextBox.setValue(widget.getNode().getText());
        this.editorTextBox.setVisible(true);

        this.editorPortraitBox.setValue(widget.getNode().getPortrait());
        this.editorPortraitBox.setVisible(true);

        this.showContextMenu = false;
    }

    private void closeNodeEditor() {
        if (editingNode != null) {
            editingNode.getNode().setTitle(this.editorTitleBox.getValue());
            editingNode.getNode().setText(this.editorTextBox.getValue());
            editingNode.getNode().setPortrait(this.editorPortraitBox.getValue());
            editingNode.updateTitle();
        }
        this.showNodeEditor = false;
        this.editingNode = null;
        this.editorTitleBox.setVisible(false);
        this.editorTextBox.setVisible(false);
        this.editorPortraitBox.setVisible(false);
        saveGraph();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showNodeEditor) {
            if (mouseX >= editorX - 10 && mouseX <= editorX + 200 &&
                    mouseY >= editorY - 10 && mouseY <= editorY + 125) {
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
            for (DialogNodeWidget widget : nodeWidgets) {
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
            for (DialogNodeWidget widget : nodeWidgets) {
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
                        setStartNode(contextMenuNode.getNode().getId());
                        showContextMenu = false;
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
            for (DialogNodeWidget widget : nodeWidgets) {
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

        for (DialogNodeWidget widget : nodeWidgets) {
            for (DialogNodeData.NodeConnection conn : widget.getNode().getConnections()) {
                DialogNodeWidget target = findWidget(conn.getTargetNodeId());
                if (target != null) {
                    int x1 = widget.getOutputX(conn.getSourceSlot());
                    int y1 = widget.getOutputY(conn.getSourceSlot());
                    int x2 = target.getInputX();
                    int y2 = target.getInputY();
                    drawArrow(graphics, x1, y1, x2, y2, 0xFF888888);
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

        graphics.drawString(this.font, "RMB=pan | Drag header=move | Drag arrow=connect | RMB node=menu | ESC=save&exit", 10, this.height - 15, 0xFF888888);
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int w = 100;
        int h = 66;
        graphics.fill(contextMenuX, contextMenuY, contextMenuX + w, contextMenuY + h, 0xFF222222);
        graphics.renderOutline(contextMenuX, contextMenuY, w, h, 0xFF666666);

        int y = contextMenuY + 2;
        boolean hoveredEdit = mouseX >= contextMenuX && mouseX <= contextMenuX + w && mouseY >= y && mouseY <= y + 20;
        graphics.fill(contextMenuX + 1, y, contextMenuX + w - 1, y + 20, hoveredEdit ? 0xFF444444 : 0xFF222222);
        graphics.drawString(font, "Edit", contextMenuX + 4, y + 6, hoveredEdit ? 0xFFFFFFFF : 0xFFCCCCCC);
        y += 22;

        boolean hoveredStart = mouseX >= contextMenuX && mouseX <= contextMenuX + w && mouseY >= y && mouseY <= y + 20;
        graphics.fill(contextMenuX + 1, y, contextMenuX + w - 1, y + 20, hoveredStart ? 0xFF444444 : 0xFF222222);
        graphics.drawString(font, "Set as Start", contextMenuX + 4, y + 6, hoveredStart ? 0xFFFFFFFF : 0xFFCCCCCC);
        y += 22;

        boolean hoveredDelete = mouseX >= contextMenuX && mouseX <= contextMenuX + w && mouseY >= y && mouseY <= y + 20;
        graphics.fill(contextMenuX + 1, y, contextMenuX + w - 1, y + 20, hoveredDelete ? 0xFF664444 : 0xFF222222);
        graphics.drawString(font, "Delete", contextMenuX + 4, y + 6, hoveredDelete ? 0xFFFF6666 : 0xFFCCCCCC);
    }

    private void renderNodeEditor(GuiGraphics graphics) {
        int x = editorX;
        int y = editorY;

        graphics.fill(x - 10, y - 10, x + 200, y + 125, 0xFF2A2A3A);
        graphics.renderOutline(x - 10, y - 10, 210, 135, 0xFF6688FF);

        graphics.drawString(font, "Edit Node (ESC or Enter to save)", x, y, 0xFFFFFFFF);

        graphics.drawString(font, "Title:", x, y + 18, 0xFFAAAAAA);
        editorTitleBox.setX(x);
        editorTitleBox.setY(y + 30);

        graphics.drawString(font, "Text:", x, y + 52, 0xFFAAAAAA);
        editorTextBox.setX(x);
        editorTextBox.setY(y + 64);

        graphics.drawString(font, "Portrait:", x, y + 86, 0xFFAAAAAA);
        editorPortraitBox.setX(x);
        editorPortraitBox.setY(y + 98);
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

    private DialogNodeWidget findWidget(String nodeId) {
        for (DialogNodeWidget widget : nodeWidgets) {
            if (widget.getNode().getId().equals(nodeId)) return widget;
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}