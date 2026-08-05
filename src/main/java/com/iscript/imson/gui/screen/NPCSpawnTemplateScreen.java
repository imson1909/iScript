package com.iscript.imson.gui.screen;

import com.iscript.imson.data.NPCManager;
import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

public class NPCSpawnTemplateScreen extends Screen {
    private final InteractionHand hand;
    private List<NPCData> templates = new ArrayList<>();
    private int scroll = 0;
    private static final int ROW_HEIGHT = 24;

    public NPCSpawnTemplateScreen(InteractionHand hand) {
        super(Component.literal("Select NPC Template"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        templates = new ArrayList<>(NPCManager.getClientCache());
        if (templates.isEmpty()) {
            NPCData empty = new NPCData();
            empty.setId("");
            empty.setName("Empty NPC");
            templates.add(empty);
        }
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(this.width / 2 - 40, this.height - 30).size(80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.ACCENT);
        int listX = this.width / 2 - 120;
        int listY = 40;
        int listW = 240;
        int listH = this.height - 80;
        int maxVisible = Math.max(1, listH / ROW_HEIGHT);
        UI.panel(g, listX - 4, listY - 4, listW + 8, listH + 8);
        for (int i = scroll; i < Math.min(scroll + maxVisible, templates.size()); i++) {
            NPCData data = templates.get(i);
            String name = data.getName();
            if (name == null || name.isEmpty()) name = "Unnamed";
            String id = data.getId();
            if (id == null || id.isEmpty()) id = "empty";
            int rowY = listY + (i - scroll) * ROW_HEIGHT;
            boolean hovered = mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ROW_HEIGHT - 2;
            UI.row(g, listX, rowY, listW, ROW_HEIGHT - 2, false, hovered);
            g.drawString(this.font, name + " [" + id + "]", listX + 4, rowY + 6, Theme.TEXT);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int listX = this.width / 2 - 120;
        int listY = 40;
        int listW = 240;
        int listH = this.height - 80;
        int maxVisible = Math.max(1, listH / ROW_HEIGHT);
        for (int i = scroll; i < Math.min(scroll + maxVisible, templates.size()); i++) {
            int rowY = listY + (i - scroll) * ROW_HEIGHT;
            if (mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ROW_HEIGHT - 2) {
                NPCData data = templates.get(i);
                String id = data.getId();
                IScriptNetwork.sendToServer(new ServerCommandPacket(
                        ServerCommandPacket.Type.SPAWN_NPC_TEMPLATE,
                        ServerCommandPacket.spawnNPCTemplateToTag(id, hand.ordinal())
                ));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int listH = this.height - 80;
        int maxVisible = Math.max(1, listH / ROW_HEIGHT);
        int maxScroll = Math.max(0, templates.size() - maxVisible);
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}