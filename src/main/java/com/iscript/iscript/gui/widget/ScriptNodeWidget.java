package com.iscript.iscript.gui.widget;

import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.gui.screen.ScriptGraphEditorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class ScriptNodeWidget extends AbstractWidget {
    private final ScriptGraphEditorScreen screen;
    private final ScriptNodeData node;
    private static final int WIDTH = 180;
    private static final int HEADER = 22;
    private static final int ARROW = 8;
    private static final Map<ScriptNodeType, Integer> TYPE_COLORS = new HashMap<>();

    static {
        TYPE_COLORS.put(ScriptNodeType.START, 0xFF22AA22);
        TYPE_COLORS.put(ScriptNodeType.IF, 0xFFAA8822);
        TYPE_COLORS.put(ScriptNodeType.DELAY, 0xFF888888);
        TYPE_COLORS.put(ScriptNodeType.CAMERA, 0xFF2266AA);
        TYPE_COLORS.put(ScriptNodeType.DIALOG, 0xFFAA44AA);
        TYPE_COLORS.put(ScriptNodeType.GIVE_ITEM, 0xFF44AAAA);
        TYPE_COLORS.put(ScriptNodeType.SPAWN_ENTITY, 0xFFAA4444);
        TYPE_COLORS.put(ScriptNodeType.SET_BLOCK, 0xFF6666AA);
        TYPE_COLORS.put(ScriptNodeType.PLAY_SOUND, 0xFFAAAA44);
        TYPE_COLORS.put(ScriptNodeType.RUN_COMMAND, 0xFF44AA44);
        TYPE_COLORS.put(ScriptNodeType.PARTICLE, 0xFFAA6622);
        TYPE_COLORS.put(ScriptNodeType.TELEPORT, 0xFFAA2266);
        TYPE_COLORS.put(ScriptNodeType.SET_GAMEMODE, 0xFF2266AA);
        TYPE_COLORS.put(ScriptNodeType.SET_HEALTH, 0xFFAA2222);
        TYPE_COLORS.put(ScriptNodeType.FREEZE, 0xFF4444AA);
        TYPE_COLORS.put(ScriptNodeType.UNFREEZE, 0xFF4444AA);
        TYPE_COLORS.put(ScriptNodeType.NPC_ANIMATE, 0xFFAA6644);
        TYPE_COLORS.put(ScriptNodeType.NPC_MOVE, 0xFFAA6644);
        TYPE_COLORS.put(ScriptNodeType.QUEST_START, 0xFF44AA88);
        TYPE_COLORS.put(ScriptNodeType.QUEST_COMPLETE, 0xFF44AA88);
        TYPE_COLORS.put(ScriptNodeType.SET_DATA, 0xFF666666);
        TYPE_COLORS.put(ScriptNodeType.SET_FACTION, 0xFF666666);
        TYPE_COLORS.put(ScriptNodeType.SET_REPUTATION, 0xFF666666);
        TYPE_COLORS.put(ScriptNodeType.WORLD_SET, 0xFF6666AA);
        TYPE_COLORS.put(ScriptNodeType.SCRIPT_JS, 0xFF888888);
        TYPE_COLORS.put(ScriptNodeType.STOP, 0xFFAA2222);
    }

    public ScriptNodeWidget(ScriptGraphEditorScreen screen, ScriptNodeData node, int x, int y) {
        super(x, y, WIDTH, getHeight(node), Component.literal(node.getType().name()));
        this.screen = screen;
        this.node = node;
    }

    public ScriptNodeData getNode() { return node; }

    public static int getHeight(ScriptNodeData node) {
        int base = HEADER + 8;
        if (node.getType() == ScriptNodeType.IF) return base + 50;
        int paramCount = node.getParams().size();
        if (paramCount == 0 && !node.getType().name().equals("START") && !node.getType().name().equals("STOP")) {
            paramCount = getDefaultParamCount(node.getType());
        }
        return base + paramCount * 14 + 8;
    }

    private static int getDefaultParamCount(ScriptNodeType type) {
        return switch (type) {
            case DELAY -> 1;
            case CAMERA -> 6;
            case DIALOG -> 1;
            case GIVE_ITEM -> 2;
            case SPAWN_ENTITY -> 4;
            case SET_BLOCK -> 4;
            case PLAY_SOUND -> 4;
            case RUN_COMMAND -> 1;
            case PARTICLE -> 4;
            case TELEPORT -> 3;
            case SET_GAMEMODE -> 1;
            case SET_HEALTH -> 1;
            case NPC_ANIMATE -> 2;
            case NPC_MOVE -> 4;
            case QUEST_START, QUEST_COMPLETE -> 1;
            case SET_DATA, SET_FACTION -> 2;
            case SET_REPUTATION -> 1;
            case WORLD_SET -> 3;
            case SCRIPT_JS -> 1;
            default -> 0;
        };
    }

    public void syncToData() {
        node.setX(this.getX());
        node.setY(this.getY());
    }

    public void moveBy(int dx, int dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
    }

    public int getInputX() { return this.getX(); }
    public int getInputY() { return this.getY() + this.height / 2; }
    public int getOutputX(int slot) { return this.getX() + WIDTH; }
    public int getOutputY(int slot) {
        if (node.getType() == ScriptNodeType.IF) {
            if (slot == 0) return this.getY() + HEADER + 16;
            return this.getY() + HEADER + 36;
        }
        return this.getY() + HEADER + 4;
    }

    public int getOutputCount() {
        return node.getType() == ScriptNodeType.IF ? 2 : 1;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int color = TYPE_COLORS.getOrDefault(node.getType(), 0xFF555566);
        boolean isStart = node.getType() == ScriptNodeType.START;
        int border = isFocused() ? 0xFFFFFFFF : color;
        int bg = isFocused() ? 0xFF3A3A4A : 0xFF2A2A3A;

        graphics.fill(getX(), getY(), getX() + WIDTH, getY() + height, bg);
        graphics.renderOutline(getX(), getY(), WIDTH, height, border);

        graphics.fill(getX(), getY(), getX() + WIDTH, getY() + HEADER, color);
        String title = node.getType().name();
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                title, getX() + 4, getY() + 7, 0xFFFFFFFF);

        int py = getY() + HEADER + 4;
        for (Map.Entry<String, String> e : node.getParams().entrySet()) {
            String val = e.getValue();
            if (val.length() > 20) val = val.substring(0, 20) + "...";
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    e.getKey() + ": " + val, getX() + 4, py, 0xFFCCCCCC);
            py += 14;
        }

        if (node.getParams().isEmpty() && !isStart && node.getType() != ScriptNodeType.STOP) {
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    "[no params]", getX() + 4, py, 0xFF666666);
        }

        graphics.fill(getX() - ARROW, getInputY() - 4, getX(), getInputY() + 4, 0xFF44FF44);
        graphics.fill(getX() - ARROW + 2, getInputY() - 6, getX(), getInputY() + 6, 0xFF44FF44);

        int outCount = getOutputCount();
        for (int i = 0; i < outCount; i++) {
            int oy = getOutputY(i);
            boolean hovered = mouseX >= getX() + WIDTH && mouseX <= getX() + WIDTH + ARROW + 2
                    && mouseY >= oy - 6 && mouseY <= oy + 6;
            int oc = hovered ? 0xFFFF6644 : 0xFFFF8844;
            graphics.fill(getX() + WIDTH, oy - 4, getX() + WIDTH + ARROW, oy + 4, oc);
            graphics.fill(getX() + WIDTH, oy - 6, getX() + WIDTH + ARROW - 2, oy + 6, oc);
            if (node.getType() == ScriptNodeType.IF) {
                String label = i == 0 ? "T" : "F";
                graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, label,
                        getX() + WIDTH - 10, oy - 3, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int outCount = getOutputCount();
            for (int i = 0; i < outCount; i++) {
                int oy = getOutputY(i);
                if (mouseX >= getX() + WIDTH && mouseX <= getX() + WIDTH + ARROW + 2
                        && mouseY >= oy - 6 && mouseY <= oy + 6) {
                    screen.startConnection(this, i);
                    return true;
                }
            }
            int inY = getInputY();
            if (mouseX >= getX() - ARROW - 2 && mouseX <= getX()
                    && mouseY >= inY - 6 && mouseY <= inY + 6) {
                return true;
            }
            if (mouseX >= getX() && mouseX <= getX() + WIDTH
                    && mouseY >= getY() && mouseY <= getY() + HEADER) {
                screen.startDragging(this);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            this.setX(this.getX() + (int) dragX);
            this.setY(this.getY() + (int) dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}