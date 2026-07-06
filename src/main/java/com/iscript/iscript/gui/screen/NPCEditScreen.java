package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveNPCDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NPCEditScreen extends Screen {
    private final int entityId;
    private final String initialName;
    private final String initialDialogId;
    private final String initialSkin;
    private final String initialFaction;

    private EditBox nameBox;
    private EditBox dialogBox;
    private EditBox skinBox;
    private EditBox factionBox;

    public NPCEditScreen(int entityId, String name, String dialogId, String skin, String faction) {
        super(Component.literal("Edit NPC"));
        this.entityId = entityId;
        this.initialName = name;
        this.initialDialogId = dialogId;
        this.initialSkin = skin;
        this.initialFaction = faction;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 60;

        this.nameBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Name"));
        this.nameBox.setMaxLength(256);
        this.nameBox.setValue(initialName);
        this.addRenderableWidget(this.nameBox);
        y += 35;

        this.dialogBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Dialog ID"));
        this.dialogBox.setMaxLength(256);
        this.dialogBox.setValue(initialDialogId);
        this.addRenderableWidget(this.dialogBox);
        y += 35;

        this.skinBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Skin"));
        this.skinBox.setMaxLength(1024);
        this.skinBox.setValue(initialSkin);
        this.addRenderableWidget(this.skinBox);
        y += 35;

        this.factionBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Faction"));
        this.factionBox.setMaxLength(256);
        this.factionBox.setValue(initialFaction);
        this.addRenderableWidget(this.factionBox);
        y += 45;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .pos(cx - 100, y).size(90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(cx + 10, y).size(90, 20).build());
    }

    private void save() {
        NPCData data = new NPCData();
        data.setName(this.nameBox.getValue());
        data.setDialogId(this.dialogBox.getValue());
        data.setSkin(this.skinBox.getValue());
        data.setFaction(this.factionBox.getValue());
        IScriptNetwork.sendToServer(new SaveNPCDataPacket(entityId, data));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, "Name:", this.width / 2 - 150, 48, 0xAAAAAA);
        graphics.drawString(this.font, "Dialog ID:", this.width / 2 - 150, 83, 0xAAAAAA);
        graphics.drawString(this.font, "Skin (URL or ResourceLocation):", this.width / 2 - 150, 118, 0xAAAAAA);
        graphics.drawString(this.font, "Faction:", this.width / 2 - 150, 153, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
