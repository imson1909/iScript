package com.iscript.iscript.data;

import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.dialog.DialogCondition;
import com.iscript.iscript.data.dialog.DialogGraphData;
import com.iscript.iscript.data.dialog.DialogNodeData;
import net.minecraft.server.level.ServerLevel;
import com.iscript.iscript.IScriptMod;

public class DialogGraphManager {
    public static DialogGraphData get(ServerLevel level, String id) {
        return DialogGraphSavedData.get(level).getGraph(id);
    }
    public static void add(ServerLevel level, DialogGraphData graph) {
        DialogGraphSavedData.get(level).addGraph(graph);
    }
    public static void remove(ServerLevel level, String id) {
        DialogGraphSavedData.get(level).removeGraph(id);
    }
    public static java.util.Map<String, DialogGraphData> getAll(ServerLevel level) {
        return DialogGraphSavedData.get(level).getGraphs();
    }

    public static DialogData convertToDialogData(ServerLevel level, String graphId, String nodeId) {
        DialogGraphData graph = get(level, graphId);
        if (graph == null) return null;
        DialogNodeData node = graph.getNode(nodeId);
        if (node == null) return null;

        DialogData dialog = new DialogData();
        dialog.setId(graphId + ":" + nodeId);
        dialog.setTitle(node.getTitle());
        dialog.setText(node.getText());
        dialog.setPortrait(node.getPortrait());
        dialog.setSound(node.getSound());

        IScriptMod.LOGGER.info("Converting node {} with {} connections", nodeId, node.getConnections().size());
        for (DialogNodeData.NodeConnection conn : node.getConnections()) {
            DialogData.DialogOption opt = new DialogData.DialogOption();
            opt.setText(conn.getOptionText());
            String targetId = graphId + ":" + conn.getTargetNodeId();
            opt.setTargetDialogId(targetId);
            IScriptMod.LOGGER.info("Connection: {} -> {}", conn.getOptionText(), targetId);
            if (!conn.getConditionScript().isEmpty()) {
                DialogCondition cond = new DialogCondition();
                cond.setType(DialogCondition.ConditionType.NONE);
                opt.setCondition(cond);
            }
            dialog.getOptions().add(opt);
        }
        return dialog;
    }
}