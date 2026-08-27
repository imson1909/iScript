package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.data.Node;
import com.iscript.imson.data.script.ScriptNodeType;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventGraph extends GraphSubScreen {
    private static final List<NodeType> NODE_TYPES = new ArrayList<>();

    static {
        NODE_TYPES.add(NodeType.builder("START", "iscript.event.node.type.event")
                .color(0xFF44AA88)
                .param("eventType", "TICK")
                .param("label", "Event")
                .outputs(1)
                .field("eventType", "iscript.event.param.eventType", NodeType.FieldType.STRING)
                .field("label", "iscript.event.param.label", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("TRIGGER", "iscript.event.node.type.trigger")
                .color(0xFF888888)
                .param("eventType", "TICK")
                .outputs(1)
                .field("eventType", "iscript.event.param.eventType", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("IF", "iscript.event.node.type.condition")
                .color(0xFFCCCCCC)
                .param("condition", "true")
                .outputs(2)
                .outputLabels("iscript.event.node.output.true", "iscript.event.node.output.false")
                .field("condition", "iscript.event.param.condition", NodeType.FieldType.STRING)
                .build());
        NODE_TYPES.add(NodeType.builder("SCRIPT_JS", "iscript.event.node.type.action")
                .color(0xFF44AA88)
                .param("script", "")
                .outputs(1)
                .field("script", "iscript.event.param.script", NodeType.FieldType.MULTILINE)
                .build());
        NODE_TYPES.add(NodeType.builder("DELAY", "iscript.event.node.type.delay")
                .color(0xFF888888)
                .param("ticks", "20")
                .outputs(1)
                .field("ticks", "iscript.event.param.ticks", NodeType.FieldType.NUMBER)
                .build());
        NODE_TYPES.add(NodeType.builder("RANDOM", "iscript.event.node.type.random")
                .color(0xFF888888)
                .param("branches", "2")
                .outputs(2)
                .field("branches", "iscript.event.param.branches", NodeType.FieldType.NUMBER)
                .build());
        NODE_TYPES.add(NodeType.builder("LOOP", "iscript.event.node.type.loop")
                .color(0xFF888888)
                .param("count", "3")
                .outputs(2)
                .outputLabels("iscript.event.node.output.body", "iscript.event.node.output.done")
                .field("count", "iscript.event.param.count", NodeType.FieldType.NUMBER)
                .build());
        NODE_TYPES.add(NodeType.builder("STOP", "iscript.event.node.type.stop")
                .color(0xFFCC4444)
                .outputs(0)
                .build());
        for (ScriptNodeType type : ScriptNodeType.values()) {
            if (type == ScriptNodeType.START || type == ScriptNodeType.TRIGGER || type == ScriptNodeType.IF ||
                    type == ScriptNodeType.SCRIPT_JS || type == ScriptNodeType.DELAY ||
                    type == ScriptNodeType.RANDOM || type == ScriptNodeType.LOOP || type == ScriptNodeType.STOP) {
                continue;
            }
            NODE_TYPES.add(NodeType.builder(type.name(), "iscript.event.node.type." + type.name().toLowerCase())
                    .color(0xFF333344)
                    .outputs(1)
                    .build());
        }
    }

    public EventGraph(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected String getCategory() {
        return "events";
    }

    @Override
    protected Map<String, Graph> getAllGraphs() {
        return DataAccess.eventGraphs();
    }

    @Override
    protected Graph getGraph(String id) {
        return DataAccess.eventGraph(id);
    }

    @Override
    protected void putGraph(Graph g) {
        DataAccess.putEventGraph(g);
    }

    @Override
    protected void removeGraph(String id) {
        DataAccess.removeEventGraph(id);
    }

    @Override
    protected void sendSavePacket(Graph g) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_EVENT_GRAPH, ServerCommandPacket.saveEventGraphToTag(g)));
    }

    @Override
    protected void sendRunPacket(String id) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.RUN_EVENT_GRAPH, ServerCommandPacket.runEventToTag(id)));
    }

    @Override
    protected void requestList() {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_EVENT_GRAPHS, new CompoundTag()));
    }

    @Override
    protected List<NodeType> getNodeTypes() {
        return NODE_TYPES;
    }

    @Override
    protected Graph createEmpty(String id, String name) {
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
        return graph;
    }

    @Override
    protected String getListTitleKey() {
        return "iscript.event.list.title";
    }

    @Override
    protected String getListNewKey() {
        return "iscript.event.list.new";
    }

    @Override
    protected String getListEmptyKey() {
        return "iscript.event.list.empty";
    }

    @Override
    protected String getSearchKey() {
        return "iscript.event.list.search";
    }

    @Override
    protected String getNameModalTitleKey(String mode) {
        return mode.equals("rename") ? "iscript.event.editor.rename" : "iscript.event.editor.new";
    }

    @Override
    protected String getNameModalBtnKey(String mode) {
        return mode.equals("rename") ? "iscript.event.editor.rename_btn" : "iscript.event.editor.create";
    }

    @Override
    protected String getEditorNewKey() {
        return "iscript.event.editor.new";
    }

    @Override
    protected String getEditorRenameKey() {
        return "iscript.event.editor.rename";
    }

    @Override
    protected String getEditorCreateKey() {
        return "iscript.event.editor.create";
    }

    @Override
    protected String getEditorRenameBtnKey() {
        return "iscript.event.editor.rename_btn";
    }

    @Override
    protected String getEditorCancelKey() {
        return "iscript.event.editor.cancel";
    }

    @Override
    protected String getEditorDeleteConfirmKey() {
        return "iscript.event.editor.delete_confirm";
    }

    @Override
    protected String getEditorDeleteKey() {
        return "iscript.event.editor.delete";
    }

    @Override
    protected String getNodeEditTitleKey() {
        return "iscript.event.node.edit_title";
    }

    @Override
    protected String getNodeEditKey() {
        return "iscript.event.node.edit";
    }

    @Override
    protected String getNodeSetStartKey() {
        return "iscript.event.node.set_start";
    }

    @Override
    protected String getNodeClearConnectionsKey() {
        return "iscript.event.node.clear_connections";
    }

    @Override
    protected String getNodeDeleteKey() {
        return "iscript.event.node.delete";
    }

    @Override
    protected String getConnectionDeleteKey() {
        return "iscript.event.connection.delete";
    }

    @Override
    protected String getNodeTypeKey(String typeId) {
        return switch (typeId.toUpperCase()) {
            case "START" -> "iscript.event.node.type.event";
            case "TRIGGER" -> "iscript.event.node.type.trigger";
            case "IF" -> "iscript.event.node.type.condition";
            case "SCRIPT_JS" -> "iscript.event.node.type.action";
            case "DELAY" -> "iscript.event.node.type.delay";
            case "RANDOM" -> "iscript.event.node.type.random";
            case "LOOP" -> "iscript.event.node.type.loop";
            case "STOP" -> "iscript.event.node.type.stop";
            default -> "iscript.event.node.type." + typeId.toLowerCase();
        };
    }

    @Override
    protected String getNodeTitle(Node node) {
        return I18n.s(getNodeTypeKey(node.getType()));
    }

    @Override
    protected String getNodeBodyText(Node node) {
        String type = node.getType();
        return switch (type) {
            case "START", "TRIGGER" -> node.getParam("eventType");
            case "IF" -> node.getParam("condition");
            case "SCRIPT_JS" -> node.getParam("script");
            case "DELAY" -> I18n.s("iscript.event.node.body.ticks", node.getParam("ticks"));
            case "RANDOM" -> I18n.s("iscript.event.node.body.branches", node.getParam("branches"));
            case "LOOP" -> I18n.s("iscript.event.node.body.times", node.getParam("count"));
            default -> "";
        };
    }

    @Override
    protected int getNodeHeaderColor(Node node) {
        String type = node.getType();
        return switch (type) {
            case "START" -> 0xFF44AA88;
            case "TRIGGER" -> 0xFF888888;
            case "IF" -> 0xFFCCCCCC;
            case "SCRIPT_JS" -> 0xFF44AA88;
            case "DELAY" -> 0xFF888888;
            case "RANDOM" -> 0xFF888888;
            case "LOOP" -> 0xFF888888;
            case "STOP" -> 0xFFCC4444;
            default -> 0xFF333344;
        };
    }

    @Override
    protected int getOutputCount(Node node) {
        String type = node.getType();
        return switch (type) {
            case "STOP" -> 0;
            case "IF" -> 2;
            case "RANDOM" -> {
                try {
                    yield Math.max(2, Integer.parseInt(node.getParam("branches")));
                } catch (NumberFormatException e) {
                    yield 2;
                }
            }
            case "LOOP" -> 2;
            default -> 1;
        };
    }

    @Override
    protected String getOutputLabel(Node node, int slot) {
        String type = node.getType();
        return switch (type) {
            case "IF" -> slot == 0 ? I18n.s("iscript.event.node.output.true") : I18n.s("iscript.event.node.output.false");
            case "LOOP" -> slot == 0 ? I18n.s("iscript.event.node.output.body") : I18n.s("iscript.event.node.output.done");
            case "RANDOM" -> I18n.s("iscript.event.node.output.out", slot + 1);
            default -> "";
        };
    }

    @Override
    protected boolean hasFollowJump(Node node) {
        return false;
    }

    @Override
    protected void executeFollowJump(Node node) {
    }

    @Override
    protected boolean useI18nForList() {
        return true;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (life.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}