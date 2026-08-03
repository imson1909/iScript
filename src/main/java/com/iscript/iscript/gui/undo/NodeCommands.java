package com.iscript.iscript.gui.undo;

import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeCommands {

    public static class AddNodeCommand implements Command {
        private final Graph graph;
        private final Node node;

        public AddNodeCommand(Graph graph, Node node) {
            this.graph = graph;
            this.node = node;
        }

        @Override
        public void execute() {
            graph.addNode(node);
        }

        @Override
        public void undo() {
            graph.removeNode(node.getId());
        }
    }

    public static class DeleteNodeCommand implements Command {
        private final Graph graph;
        private final Node node;
        private final List<Node.Connection> incomingConnections = new ArrayList<>();
        private final List<Node> sourceNodes = new ArrayList<>();
        private final String oldStartNodeId;

        public DeleteNodeCommand(Graph graph, Node node) {
            this.graph = graph;
            this.node = node;
            this.oldStartNodeId = graph.getStartNodeId();
            for (Node n : graph.getNodes().values()) {
                for (Node.Connection c : n.getConnections()) {
                    if (c.getTarget().equals(node.getId())) {
                        incomingConnections.add(c);
                        sourceNodes.add(n);
                    }
                }
            }
        }

        @Override
        public void execute() {
            graph.removeNode(node.getId());
        }

        @Override
        public void undo() {
            graph.addNode(node);
            for (int i = 0; i < sourceNodes.size(); i++) {
                sourceNodes.get(i).getConnections().add(incomingConnections.get(i));
            }
            if (oldStartNodeId.equals(node.getId())) {
                graph.setStartNodeId(node.getId());
            }
        }
    }

    public static class MoveNodeCommand implements Command {
        private final Node node;
        private final int oldX, oldY;
        private final int newX, newY;

        public MoveNodeCommand(Node node, int oldX, int oldY, int newX, int newY) {
            this.node = node;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
        }

        @Override
        public void execute() {
            node.setX(newX);
            node.setY(newY);
        }

        @Override
        public void undo() {
            node.setX(oldX);
            node.setY(oldY);
        }
    }

    public static class EditNodeParamCommand implements Command {
        private final Node node;
        private final String key;
        private final String oldValue;
        private final String newValue;

        public EditNodeParamCommand(Node node, String key, String oldValue, String newValue) {
            this.node = node;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void execute() {
            node.setParam(key, newValue);
        }

        @Override
        public void undo() {
            node.setParam(key, oldValue);
        }
    }

    public static class ConnectNodesCommand implements Command {
        private final Node fromNode;
        private final Node.Connection connection;

        public ConnectNodesCommand(Node fromNode, Node.Connection connection) {
            this.fromNode = fromNode;
            this.connection = connection;
        }

        @Override
        public void execute() {
            fromNode.getConnections().add(connection);
        }

        @Override
        public void undo() {
            fromNode.getConnections().removeIf(c ->
                    c.getTarget().equals(connection.getTarget()) &&
                            c.getSourceSlot() == connection.getSourceSlot()
            );
        }
    }

    public static class DisconnectNodesCommand implements Command {
        private final Node fromNode;
        private final Node.Connection connection;

        public DisconnectNodesCommand(Node fromNode, Node.Connection connection) {
            this.fromNode = fromNode;
            this.connection = connection;
        }

        @Override
        public void execute() {
            fromNode.getConnections().removeIf(c ->
                    c.getTarget().equals(connection.getTarget()) &&
                            c.getSourceSlot() == connection.getSourceSlot()
            );
        }

        @Override
        public void undo() {
            fromNode.getConnections().add(connection);
        }
    }

    public static class SetStartNodeCommand implements Command {
        private final Graph graph;
        private final String oldStartId;
        private final String newStartId;

        public SetStartNodeCommand(Graph graph, String oldStartId, String newStartId) {
            this.graph = graph;
            this.oldStartId = oldStartId;
            this.newStartId = newStartId;
        }

        @Override
        public void execute() {
            graph.setStartNodeId(newStartId);
        }

        @Override
        public void undo() {
            graph.setStartNodeId(oldStartId);
        }
    }
}