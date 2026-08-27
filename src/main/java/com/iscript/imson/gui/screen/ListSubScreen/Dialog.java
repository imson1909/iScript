package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.data.Node;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dialog extends GraphSubScreen {
    private static final List<NodeType> NODE_TYPES = new ArrayList<>();
    private int spawnOffset = 0;

    static {
        NODE_TYPES.add(NodeType.builder("start", "iscript.dialog.node.type.start")
                .color(0xFF44AA88)
                .param("title", "Start")
                .param("text", "Hello!")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("npc", "iscript.dialog.node.type.npc")
                .color(0xFF4488AA)
                .param("title", "NPC")
                .param("text", "...")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("player", "iscript.dialog.node.type.player")
                .color(0xFFAA44AA)
                .param("title", "Player")
                .param("text", "...")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("choice", "iscript.dialog.node.type.choice")
                .color(0xFFAA8822)
                .param("title", "Choice")
                .param("text", "Choose...")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("condition", "iscript.dialog.node.type.condition")
                .color(0xFFAA4444)
                .param("title", "Condition")
                .param("text", "true")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("action", "iscript.dialog.node.type.action")
                .color(0xFF6666AA)
                .param("title", "Action")
                .param("text", "do something")
                .outputs(1)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("jump", "iscript.dialog.node.type.jump")
                .color(0xFFAA66CC)
                .param("title", "Jump")
                .param("text", "")
                .param("target_dialog", "")
                .outputs(0)
                .field("title", "iscript.dialog.node.title", NodeType.FieldType.STRING)
                .field("text", "iscript.dialog.node.text", NodeType.FieldType.MULTILINE)
                .field("portrait", "iscript.dialog.node.portrait", NodeType.FieldType.STRING)
                .field("target_dialog", "iscript.dialog.node.target_dialog", NodeType.FieldType.STRING)
                .build());
    }

    public Dialog(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected String getCategory() {
        return "dialogs";
    }

    @Override
    protected Map<String, Graph> getAllGraphs() {
        return DataAccess.dialogGraphs();
    }

    @Override
    protected Graph getGraph(String id) {
        return DataAccess.dialogGraph(id);
    }

    @Override
    protected void putGraph(Graph g) {
        DataAccess.putDialogGraph(g);
    }

    @Override
    protected void removeGraph(String id) {
        DataAccess.removeDialogGraph(id);
    }

    @Override
    protected void sendSavePacket(Graph g) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_DIALOG_GRAPH, ServerCommandPacket.saveDialogGraphToTag(g)));
    }

    @Override
    protected void sendRunPacket(String id) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_DIALOG, ServerCommandPacket.requestDialogToTag(id)));
    }

    @Override
    protected void requestList() {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_DASHBOARD_LIST, ServerCommandPacket.requestDashboardListToTag("dialogs")));
    }

    @Override
    protected List<NodeType> getNodeTypes() {
        return NODE_TYPES;
    }

    @Override
    protected Graph createEmpty(String id, String name) {
        Graph graph = new Graph(null);
        graph.setId(id);
        graph.setName(name);
        Node start = new Node(null);
        start.setId("start");
        start.setType("start");
        start.setParam("title", Component.translatable("iscript.dialog.node.type.start").getString());
        start.setParam("text", Component.translatable("iscript.dialog.node.default.start_text").getString());
        start.setX(200);
        start.setY(200);
        graph.addNode(start);
        graph.setStartNodeId("start");
        return graph;
    }

    @Override
    protected String getListTitleKey() {
        return "iscript.dialog.list.title";
    }

    @Override
    protected String getListNewKey() {
        return "iscript.dialog.list.new";
    }

    @Override
    protected String getListEmptyKey() {
        return "iscript.dialog.list.empty";
    }

    @Override
    protected String getSearchKey() {
        return "iscript.dialog.list.search";
    }

    @Override
    protected String getNameModalTitleKey(String mode) {
        return mode.equals("rename") ? "iscript.dialog.editor.rename" : "iscript.dialog.editor.new";
    }

    @Override
    protected String getNameModalBtnKey(String mode) {
        return mode.equals("rename") ? "iscript.dialog.editor.rename_btn" : "iscript.dialog.editor.create";
    }

    @Override
    protected String getEditorNewKey() {
        return "iscript.dialog.editor.new";
    }

    @Override
    protected String getEditorRenameKey() {
        return "iscript.dialog.editor.rename";
    }

    @Override
    protected String getEditorCreateKey() {
        return "iscript.dialog.editor.create";
    }

    @Override
    protected String getEditorRenameBtnKey() {
        return "iscript.dialog.editor.rename_btn";
    }

    @Override
    protected String getEditorCancelKey() {
        return "iscript.dialog.editor.cancel";
    }

    @Override
    protected String getEditorDeleteConfirmKey() {
        return "iscript.dialog.editor.delete_confirm";
    }

    @Override
    protected String getEditorDeleteKey() {
        return "iscript.dialog.editor.delete";
    }

    @Override
    protected String getNodeEditTitleKey() {
        return "iscript.dialog.node.edit_title";
    }

    @Override
    protected String getNodeEditKey() {
        return "iscript.dialog.node.edit";
    }

    @Override
    protected String getNodeSetStartKey() {
        return "iscript.dialog.node.set_start";
    }

    @Override
    protected String getNodeClearConnectionsKey() {
        return "iscript.dialog.node.clear_connections";
    }

    @Override
    protected String getNodeDeleteKey() {
        return "iscript.dialog.node.delete";
    }

    @Override
    protected String getConnectionDeleteKey() {
        return "iscript.dialog.connection.delete";
    }

    @Override
    protected String getNodeTypeKey(String typeId) {
        return "iscript.dialog.node.type." + typeId;
    }

    @Override
    protected String getNodeTitle(Node node) {
        return node.getParam("title");
    }

    @Override
    protected String getNodeBodyText(Node node) {
        String type = node.getType();
        if ("jump".equals(type)) {
            String target = node.getParam("target_dialog");
            return target != null && !target.isEmpty() ? "-> " + target : "";
        }
        return node.getParam("text");
    }

    @Override
    protected int getNodeHeaderColor(Node node) {
        String type = node.getType();
        if ("jump".equals(type)) return 0xFFAA66CC;
        boolean isStart = currentGraph != null && node.getId().equals(currentGraph.getStartNodeId());
        return isStart ? 0xFF44AA88 : 0xFF333344;
    }

    @Override
    protected int getOutputCount(Node node) {
        String type = node.getType();
        return "jump".equals(type) ? 0 : 1;
    }

    @Override
    protected String getOutputLabel(Node node, int slot) {
        return "";
    }

    @Override
    protected boolean hasFollowJump(Node node) {
        return "jump".equals(node.getType());
    }

    @Override
    protected void executeFollowJump(Node node) {
        String target = node.getParam("target_dialog");
        if (target != null && !target.isEmpty() && DataAccess.dialogGraph(target) != null) {
            switchToGraph(target);
        }
    }

    @Override
    protected boolean useI18nForList() {
        return false;
    }

    @Override
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
        int offset = spawnOffset * 30;
        node.setX((int) (life.canvas().x() + 200 + offset));
        node.setY((int) (life.canvas().y() + 200 + offset));
        spawnOffset++;
        commandStack.execute(new com.iscript.imson.gui.undo.NodeCommands.AddNodeCommand(currentGraph, node));
        save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (life.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}