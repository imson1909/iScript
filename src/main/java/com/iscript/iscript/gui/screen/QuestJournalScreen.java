package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.quest.QuestData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class QuestJournalScreen extends Screen {
    private final Map<String, Integer> progress = new HashMap<>();
    private final Map<String, Boolean> completed = new HashMap<>();
    private int tab = 0;
    private int scroll = 0;
    private String selectedQuest = "";

    public QuestJournalScreen(CompoundTag tag) {
        super(Component.literal("Quest Journal"));
        CompoundTag prog = tag.getCompound("Progress");
        for (String key : prog.getAllKeys()) {
            progress.put(key, prog.getInt(key));
        }
        ListTag comp = tag.getList("Completed", 10);
        for (int i = 0; i < comp.size(); i++) {
            completed.put(comp.getCompound(i).getString("Id"), true);
        }
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Active"), btn -> { tab = 0; scroll = 0; })
                .pos(this.width / 2 - 110, 30).size(100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Completed"), btn -> { tab = 1; scroll = 0; })
                .pos(this.width / 2 + 10, 30).size(100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 30).size(100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int y = 60;
        int count = 0;
        for (Map.Entry<String, Integer> e : progress.entrySet()) {
            boolean done = completed.getOrDefault(e.getKey(), false);
            if ((tab == 0 && !done) || (tab == 1 && done)) {
                if (count >= scroll && count < scroll + 12) {
                    int color = done ? 0x55FF55 : 0xFFFF55;
                    String status = done ? " [DONE]" : " [" + e.getValue() + " prog]";
                    boolean hovered = mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 
                            && mouseY >= y && mouseY <= y + 14;
                    if (hovered) {
                        graphics.fill(this.width / 2 - 105, y - 2, this.width / 2 + 105, y + 14, 0x44FFFFFF);
                    }
                    graphics.drawString(this.font, e.getKey() + status, this.width / 2 - 100, y, color);
                    y += 16;
                }
                count++;
            }
        }
        for (Map.Entry<String, Boolean> e : completed.entrySet()) {
            if (tab == 1 && !progress.containsKey(e.getKey())) {
                if (count >= scroll && count < scroll + 12) {
                    graphics.drawString(this.font, e.getKey() + " [DONE]", this.width / 2 - 100, y, 0x55FF55);
                    y += 16;
                }
                count++;
            }
        }

        // Scroll indicators
        if (scroll > 0) {
            graphics.drawCenteredString(this.font, "^", this.width / 2, 55, 0xAAAAAA);
        }
        if (count > scroll + 12) {
            graphics.drawCenteredString(this.font, "v", this.width / 2, this.height - 50, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll++;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
