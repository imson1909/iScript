package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import com.iscript.iscript.gui.widget.StyledButton;
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
        super(I18n.t("iscript.script.block.title"));
        this.pos = pos;
        this.initialLabel = label;
        this.initialScriptId = scriptId;
        this.initialScript = script;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        this.labelBox = new EditBox(this.font, cx - 150, 45, 300, 20, I18n.t("iscript.script.block.placeholder.label"));
        this.labelBox.setMaxLength(128);
        this.labelBox.setValue(this.initialLabel);
        this.labelBox.setTextColor(Theme.TEXT);
        this.addRenderableWidget(this.labelBox);

        this.scriptIdBox = new EditBox(this.font, cx - 150, 85, 200, 20, I18n.t("iscript.script.block.placeholder.script_id"));
        this.scriptIdBox.setMaxLength(64);
        this.scriptIdBox.setValue(this.initialScriptId);
        this.scriptIdBox.setTextColor(Theme.TEXT);
        this.addRenderableWidget(this.scriptIdBox);

        this.addRenderableWidget(new StyledButton(this.font, cx + 60, 85, 40, 20, I18n.t("iscript.script.block.button.new"), () -> createNew()).setAccent(true));

        this.scriptBox = new MultiLineEditBox(this.font, cx - 150, 125, 300, 100, I18n.t("iscript.script.block.placeholder.script"), I18n.t("iscript.script.block.placeholder.script_hint"));
        this.scriptBox.setValue(this.initialScript);
        this.addRenderableWidget(this.scriptBox);

        this.addRenderableWidget(new StyledButton(this.font, cx - 100, 230, 90, 20, I18n.t("iscript.script.block.button.save"), () -> save()).setAccent(true));
        this.addRenderableWidget(new StyledButton(this.font, cx + 10, 230, 90, 20, I18n.t("iscript.script.block.button.close"), () -> this.onClose()));
    }

    private void createNew() {
        String id = "script_" + System.currentTimeMillis();
        this.scriptIdBox.setValue(id);
        this.scriptBox.setValue(I18n.s("iscript.script.block.new_script"));
    }

    private void save() {
        String id = this.scriptIdBox.getValue();
        if (id.isEmpty()) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SCRIPT_BLOCK_SAVE, ServerCommandPacket.scriptBlockToTag(pos, this.labelBox.getValue(), id, this.scriptBox.getValue())));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.TEXT);
        graphics.drawString(this.font, I18n.s("iscript.script.block.label.label"), this.width / 2 - 150, 33, Theme.TEXT_DIM);
        graphics.drawString(this.font, I18n.s("iscript.script.block.label.script_id"), this.width / 2 - 150, 73, Theme.TEXT_DIM);
        graphics.drawString(this.font, I18n.s("iscript.script.block.label.script"), this.width / 2 - 150, 113, Theme.TEXT_DIM);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}