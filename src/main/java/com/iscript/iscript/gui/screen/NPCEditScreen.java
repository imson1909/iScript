package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCState;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SaveNPCDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
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
    private EditBox healthBox;
    private EditBox attackBox;
    private EditBox hostileBox;
    private EditBox scaleBox;
    private EditBox glowColorBox;
    private CycleButton<NPCState> stateButton;
    private Button aggressiveButton;
    private Button tradeButton;
    private Button nameVisibleButton;
    private Button glowButton;
    private Button noAIButton;
    private Button invulnerableButton;
    private Button silentButton;
    private Button gravityButton;
    private NPCState selectedState = NPCState.IDLE;
    private boolean aggressive = false;
    private boolean enableTrade = false;
    private boolean nameVisible = true;
    private boolean glowEnabled = false;
    private boolean noAI = false;
    private boolean invulnerable = false;
    private boolean silent = false;
    private boolean hasGravity = true;

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
        int y = 40;
        int col2 = cx + 10;
        int rowHeight = 22;

        this.nameBox = new EditBox(this.font, cx - 300, y, 280, 18, Component.literal("Name"));
        this.nameBox.setMaxLength(256);
        this.nameBox.setValue(initialName);
        this.addRenderableWidget(this.nameBox);

        this.dialogBox = new EditBox(this.font, col2, y, 280, 18, Component.literal("Dialog ID"));
        this.dialogBox.setMaxLength(256);
        this.dialogBox.setValue(initialDialogId);
        this.addRenderableWidget(this.dialogBox);
        y += rowHeight;

        this.skinBox = new EditBox(this.font, cx - 300, y, 280, 18, Component.literal("Skin URL"));
        this.skinBox.setMaxLength(1024);
        this.skinBox.setValue(initialSkin);
        this.addRenderableWidget(this.skinBox);

        this.factionBox = new EditBox(this.font, col2, y, 280, 18, Component.literal("Faction"));
        this.factionBox.setMaxLength(256);
        this.factionBox.setValue(initialFaction);
        this.addRenderableWidget(this.factionBox);
        y += rowHeight;

        this.healthBox = new EditBox(this.font, cx - 300, y, 135, 18, Component.literal("Health"));
        this.healthBox.setMaxLength(10);
        this.healthBox.setValue("20");
        this.addRenderableWidget(this.healthBox);

        this.attackBox = new EditBox(this.font, cx - 155, y, 135, 18, Component.literal("Attack"));
        this.attackBox.setMaxLength(10);
        this.attackBox.setValue("2");
        this.addRenderableWidget(this.attackBox);

        this.stateButton = CycleButton.<NPCState>builder(s -> Component.literal(s.name()))
                .withValues(NPCState.values())
                .withInitialValue(NPCState.IDLE)
                .create(col2, y, 135, 18, Component.literal("State"), (btn, state) -> selectedState = state);
        this.addRenderableWidget(this.stateButton);

        this.aggressiveButton = Button.builder(Component.literal("Aggressive: OFF"), btn -> {
            aggressive = !aggressive;
            btn.setMessage(Component.literal("Aggressive: " + (aggressive ? "ON" : "OFF")));
        }).pos(col2 + 145, y).size(135, 18).build();
        this.addRenderableWidget(this.aggressiveButton);
        y += rowHeight;

        this.hostileBox = new EditBox(this.font, cx - 300, y, 590, 18, Component.literal("Hostile Factions (comma-separated)"));
        this.hostileBox.setMaxLength(512);
        this.addRenderableWidget(this.hostileBox);
        y += rowHeight;

        this.scaleBox = new EditBox(this.font, cx - 300, y, 135, 18, Component.literal("Scale"));
        this.scaleBox.setMaxLength(10);
        this.scaleBox.setValue("1.0");
        this.addRenderableWidget(this.scaleBox);

        this.glowColorBox = new EditBox(this.font, cx - 155, y, 135, 18, Component.literal("Glow Color"));
        this.glowColorBox.setMaxLength(10);
        this.glowColorBox.setValue("FFFFFF");
        this.addRenderableWidget(this.glowColorBox);

        this.nameVisibleButton = Button.builder(Component.literal("Name: ON"), btn -> {
            nameVisible = !nameVisible;
            btn.setMessage(Component.literal("Name: " + (nameVisible ? "ON" : "OFF")));
        }).pos(col2, y).size(100, 18).build();
        this.addRenderableWidget(this.nameVisibleButton);

        this.glowButton = Button.builder(Component.literal("Glow: OFF"), btn -> {
            glowEnabled = !glowEnabled;
            btn.setMessage(Component.literal("Glow: " + (glowEnabled ? "ON" : "OFF")));
        }).pos(col2 + 110, y).size(100, 18).build();
        this.addRenderableWidget(this.glowButton);
        y += rowHeight;

        this.noAIButton = Button.builder(Component.literal("No AI: OFF"), btn -> {
            noAI = !noAI;
            btn.setMessage(Component.literal("No AI: " + (noAI ? "ON" : "OFF")));
        }).pos(cx - 300, y).size(100, 18).build();
        this.addRenderableWidget(this.noAIButton);

        this.invulnerableButton = Button.builder(Component.literal("Invuln: OFF"), btn -> {
            invulnerable = !invulnerable;
            btn.setMessage(Component.literal("Invuln: " + (invulnerable ? "ON" : "OFF")));
        }).pos(cx - 190, y).size(100, 18).build();
        this.addRenderableWidget(this.invulnerableButton);

        this.silentButton = Button.builder(Component.literal("Silent: OFF"), btn -> {
            silent = !silent;
            btn.setMessage(Component.literal("Silent: " + (silent ? "ON" : "OFF")));
        }).pos(cx - 80, y).size(100, 18).build();
        this.addRenderableWidget(this.silentButton);

        this.gravityButton = Button.builder(Component.literal("Gravity: ON"), btn -> {
            hasGravity = !hasGravity;
            btn.setMessage(Component.literal("Gravity: " + (hasGravity ? "ON" : "OFF")));
        }).pos(cx + 30, y).size(100, 18).build();
        this.addRenderableWidget(this.gravityButton);

        this.tradeButton = Button.builder(Component.literal("Trade: OFF"), btn -> {
            enableTrade = !enableTrade;
            btn.setMessage(Component.literal("Trade: " + (enableTrade ? "ON" : "OFF")));
        }).pos(cx + 140, y).size(100, 18).build();
        this.addRenderableWidget(this.tradeButton);
        y += rowHeight + 10;

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
        try {
            data.setMaxHealth(Float.parseFloat(this.healthBox.getValue()));
            data.setHealth(Float.parseFloat(this.healthBox.getValue()));
        } catch (NumberFormatException ignored) {}
        try {
            data.setAttackDamage(Float.parseFloat(this.attackBox.getValue()));
        } catch (NumberFormatException ignored) {}
        try {
            data.setScale(Float.parseFloat(this.scaleBox.getValue()));
        } catch (NumberFormatException ignored) {}
        try {
            String hex = this.glowColorBox.getValue().replace("#", "");
            data.setGlowColor((int) Long.parseLong(hex, 16));
        } catch (NumberFormatException ignored) {}
        data.setState(selectedState);
        data.setAggressive(aggressive);
        data.setHostileFactions(this.hostileBox.getValue());
        data.setEnableTrade(enableTrade);
        data.setNameVisible(nameVisible);
        data.setGlowEnabled(glowEnabled);
        data.setNoAI(noAI);
        data.setInvulnerable(invulnerable);
        data.setSilent(silent);
        data.setHasGravity(hasGravity);
        IScriptNetwork.sendToServer(new SaveNPCDataPacket(entityId, data));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, "Name:", this.width / 2 - 300, 32, 0xAAAAAA);
        graphics.drawString(this.font, "Dialog ID:", this.width / 2 + 10, 32, 0xAAAAAA);
        graphics.drawString(this.font, "Skin URL:", this.width / 2 - 300, 54, 0xAAAAAA);
        graphics.drawString(this.font, "Faction:", this.width / 2 + 10, 54, 0xAAAAAA);
        graphics.drawString(this.font, "Health/Max:", this.width / 2 - 300, 76, 0xAAAAAA);
        graphics.drawString(this.font, "Attack Dmg:", this.width / 2 - 155, 76, 0xAAAAAA);
        graphics.drawString(this.font, "Hostile Factions:", this.width / 2 - 300, 98, 0xAAAAAA);
        graphics.drawString(this.font, "Scale:", this.width / 2 - 300, 120, 0xAAAAAA);
        graphics.drawString(this.font, "Glow Hex:", this.width / 2 - 155, 120, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
