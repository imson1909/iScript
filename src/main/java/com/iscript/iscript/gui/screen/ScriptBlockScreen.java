package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ScriptBlockScreen extends Screen {
    private final BlockPos pos;
    private final String initialLabel;
    private final String initialScriptId;
    private final String initialScript;
    private EditBox labelBox;
    private EditBox scriptIdBox;
    private MultiLineEditBox scriptBox;

    public ScriptBlockScreen(BlockPos pos, String label, String scriptId, String script) {
        super(Component.literal("Script Block"));
        this.pos = pos;
        this.initialLabel = label;
        this.initialScriptId = scriptId;
        this.initialScript = script;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        this.labelBox = new EditBox(this.font, cx - 150, 45, 300, 20, Component.literal("Label"));
        this.labelBox.setMaxLength(128);
        this.labelBox.setValue(this.initialLabel);
        this.addRenderableWidget(this.labelBox);

        this.scriptIdBox = new EditBox(this.font, cx - 150, 85, 200, 20, Component.literal("Script ID"));
        this.scriptIdBox.setMaxLength(64);
        this.scriptIdBox.setValue(this.initialScriptId);
        this.addRenderableWidget(this.scriptIdBox);

        this.addRenderableWidget(Button.builder(Component.literal("New"), btn -> createNew())
                .pos(cx + 60, 85).size(40, 20).build());

        this.scriptBox = new MultiLineEditBox(this.font, cx - 150, 125, 300, 100, Component.literal("Script"), Component.literal("Enter script..."));
        this.scriptBox.setValue(this.initialScript);
        this.addRenderableWidget(this.scriptBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .pos(cx - 100, 230).size(90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(cx + 10, 230).size(90, 20).build());
    }

    private void createNew() {
        String id = "script_" + System.currentTimeMillis();
        this.scriptIdBox.setValue(id);
        this.scriptBox.setValue("// New script\n");
    }

    private void save() {
        String id = this.scriptIdBox.getValue();
        if (id.isEmpty()) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SCRIPT_BLOCK_SAVE, ServerCommandPacket.scriptBlockToTag(pos, this.labelBox.getValue(), id, this.scriptBox.getValue())));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        graphics.drawString(this.font, "Label:", this.width / 2 - 150, 33, 0xAAAAAA);
        graphics.drawString(this.font, "Script ID:", this.width / 2 - 150, 73, 0xAAAAAA);
        graphics.drawString(this.font, "Script:", this.width / 2 - 150, 113, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}