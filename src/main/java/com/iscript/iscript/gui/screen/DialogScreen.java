package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.dialog.DialogData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class DialogScreen extends Screen {
    private static final ResourceLocation DEFAULT_PORTRAIT = new ResourceLocation("minecraft", "textures/item/paper.png");
    private final DialogData dialog;
    private final List<DialogData.DialogOption> options;
    private int textPage = 0;
    private final String[] textPages;

    public DialogScreen(DialogData dialog) {
        super(Component.literal(dialog.getTitle()));
        this.dialog = dialog;
        this.options = dialog.getOptions();
        String fullText = dialog.getText().replace("\\n", "\n");
        this.textPages = splitText(fullText, 45);
    }

    private String[] splitText(String text, int maxLineLength) {
        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > maxLineLength) {
                result.append(line.toString().trim()).append("\n");
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        result.append(line.toString().trim());
        return result.toString().split("\n");
    }

    @Override
    protected void init() {
        int y = this.height - 40;
        int btnWidth = Math.min(200, (this.width - 40) / Math.max(options.size(), 1));
        int startX = (this.width - (btnWidth + 10) * options.size()) / 2;

        for (int i = 0; i < options.size(); i++) {
            final int idx = i;
            DialogData.DialogOption opt = options.get(i);
            String tooltip = opt.getTooltip().isEmpty() ? opt.getText() : opt.getTooltip();
            this.addRenderableWidget(Button.builder(Component.literal(opt.getText()), btn -> {
                if (!opt.getCommand().isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(opt.getCommand());
                }
                if (!opt.getTargetDialogId().isEmpty()) {
                    // TODO: chain to next dialog
                }
                this.onClose();
            }).pos(startX + i * (btnWidth + 10), y).size(btnWidth, 20)
            .build());
        }

        if (textPages.length > 6) {
            this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                textPage = Math.min(textPage + 1, (textPages.length - 1) / 6);
            }).pos(this.width - 40, this.height / 2).size(20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                textPage = Math.max(textPage - 1, 0);
            }).pos(20, this.height / 2).size(20, 20).build());
        }

        if (options.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                    .pos(this.width / 2 - 50, y).size(100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Portrait background
        graphics.fill(20, 40, 120, 140, 0xFF333333);
        graphics.renderOutline(19, 39, 102, 102, 0xFF888888);

        // Portrait
        ResourceLocation portrait = dialog.getPortrait().isEmpty() ? DEFAULT_PORTRAIT : 
            new ResourceLocation(dialog.getPortrait());
        graphics.blit(portrait, 25, 45, 0, 0, 90, 90, 90, 90);

        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2 + 60, 50, 0xFFFFFF);

        // Text
        int y = 70;
        int startLine = textPage * 6;
        for (int i = startLine; i < Math.min(startLine + 6, textPages.length); i++) {
            graphics.drawString(this.font, textPages[i], 140, y, 0xDDDDDD);
            y += 14;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
