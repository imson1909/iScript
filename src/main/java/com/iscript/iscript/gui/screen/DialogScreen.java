package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.network.packet.ServerCommandPacket;

import java.util.ArrayList;
import java.util.List;

public class DialogScreen extends Screen {
    private static final ResourceLocation DEFAULT_PORTRAIT = new ResourceLocation("minecraft", "textures/item/paper.png");
    private final DialogData dialog;
    private final List<DialogData.DialogOption> options;
    private int textPage = 0;
    private String[] textPages;
    private int optionsScroll = 0;
    private final List<Button> optionButtons = new ArrayList<>();

    public DialogScreen(DialogData dialog) {
        super(Component.literal(dialog.getTitle()));
        this.dialog = dialog;
        this.options = dialog.getOptions();
    }

    private String[] splitText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        for (String para : paragraphs) {
            StringBuilder currentLine = new StringBuilder();
            for (String word : para.split(" ")) {
                if (!currentLine.isEmpty() && font.width(currentLine + " " + word) > maxWidth) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
                if (!currentLine.isEmpty()) currentLine.append(" ");
                currentLine.append(word);
            }
            if (!currentLine.isEmpty()) lines.add(currentLine.toString().trim());
        }
        return lines.toArray(new String[0]);
    }

    private void buildOptions() {
        for (Button b : optionButtons) {
            this.removeWidget(b);
        }
        optionButtons.clear();

        int optionAreaX = this.width - 140;
        int optionAreaY = 70;
        int optionAreaH = this.height - 110;
        int btnWidth = 120;
        int btnHeight = 20;
        int btnGap = 4;
        int maxVisible = Math.max(1, optionAreaH / (btnHeight + btnGap));
        int maxScroll = Math.max(0, options.size() - maxVisible);

        if (optionsScroll > maxScroll) optionsScroll = maxScroll;

        int startIdx = optionsScroll;
        int endIdx = Math.min(options.size(), startIdx + maxVisible);

        for (int i = startIdx; i < endIdx; i++) {
            final DialogData.DialogOption opt = options.get(i);
            int btnY = optionAreaY + (i - startIdx) * (btnHeight + btnGap);
            Button btn = Button.builder(Component.literal(opt.getText()), b -> {
                IScriptMod.LOGGER.info("Button clicked, target: {}", opt.getTargetDialogId());
                if (!opt.getCommand().isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(opt.getCommand());
                }
                if (!opt.getTargetDialogId().isEmpty()) {
                    IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_DIALOG, ServerCommandPacket.requestDialogToTag(opt.getTargetDialogId())));
                }
                this.onClose();
            }).pos(optionAreaX + 10, btnY).size(btnWidth, btnHeight).build();
            this.addRenderableWidget(btn);
            optionButtons.add(btn);
        }

        if (options.isEmpty()) {
            Button btn = Button.builder(I18n.t("iscript.dialog.screen.close"), b -> this.onClose())
                    .pos(this.width / 2 - 50, this.height - 40).size(100, 20).build();
            this.addRenderableWidget(btn);
            optionButtons.add(btn);
        }
    }

    @Override
    protected void init() {
        String fullText = dialog.getText().replace("\\n", "\n");
        int textMaxWidth = this.width - 180;
        this.textPages = splitText(fullText, textMaxWidth);

        buildOptions();

        if (textPages.length > 6) {
            this.addRenderableWidget(Button.builder(I18n.t("iscript.dialog.screen.next"), btn -> {
                textPage = Math.min(textPage + 1, (textPages.length - 1) / 6);
            }).pos(this.width - 40, this.height / 2).size(20, 20).build());
            this.addRenderableWidget(Button.builder(I18n.t("iscript.dialog.screen.prev"), btn -> {
                textPage = Math.max(textPage - 1, 0);
            }).pos(20, this.height / 2).size(20, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        UI.panel(graphics, 20, 40, 100, 100);
        graphics.renderOutline(19, 39, 102, 102, Theme.BORDER);

        ResourceLocation portrait = dialog.getPortrait().isEmpty() ? DEFAULT_PORTRAIT :
                new ResourceLocation(dialog.getPortrait());
        graphics.blit(portrait, 25, 45, 0, 0, 90, 90, 90, 90);

        graphics.drawCenteredString(this.font, this.title, this.width / 2 + 20, 50, Theme.TEXT);

        int y = 70;
        int startLine = textPage * 6;
        for (int i = startLine; i < Math.min(startLine + 6, textPages.length); i++) {
            graphics.drawString(this.font, textPages[i], 140, y, Theme.TEXT_DIM);
            y += 14;
        }

        int optionAreaX = this.width - 140;
        int optionAreaY = 70;
        int optionAreaH = this.height - 110;
        if (options.size() > 0) {
            graphics.enableScissor(optionAreaX, optionAreaY, this.width, optionAreaY + optionAreaH);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (options.size() > 0) {
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int optionAreaX = this.width - 140;
        int optionAreaY = 70;
        int optionAreaH = this.height - 110;
        if (mouseX >= optionAreaX && mouseY >= optionAreaY && mouseX <= this.width && mouseY <= optionAreaY + optionAreaH && options.size() > 0) {
            int btnHeight = 20;
            int btnGap = 4;
            int maxVisible = Math.max(1, optionAreaH / (btnHeight + btnGap));
            int maxScroll = Math.max(0, options.size() - maxVisible);
            if (delta > 0) {
                optionsScroll = Math.max(0, optionsScroll - 1);
            } else {
                optionsScroll = Math.min(optionsScroll + 1, maxScroll);
            }
            buildOptions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}