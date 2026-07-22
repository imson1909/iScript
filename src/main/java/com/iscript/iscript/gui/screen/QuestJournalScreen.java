package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.gui.widget.StyledButton;
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
    private final Map<String, String> titles = new HashMap<>();
    private int tab = 0;
    private int scroll = 0;
    private String selectedQuest = "";

    public QuestJournalScreen(CompoundTag progressTag, CompoundTag metaTag) {
        super(I18n.t("iscript.quest.journal.title"));
        CompoundTag prog = progressTag.getCompound("Progress");
        for (String key : prog.getAllKeys()) {
            progress.put(key, prog.getInt(key));
        }
        ListTag comp = progressTag.getList("Completed", 10);
        for (int i = 0; i < comp.size(); i++) {
            completed.put(comp.getCompound(i).getString("Id"), true);
        }
        for (String key : metaTag.getAllKeys()) {
            titles.put(key, metaTag.getString(key));
        }
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new StyledButton(this.font, this.width / 2 - 110, 30, 100, 20, I18n.t("iscript.quest.journal.active"), () -> { tab = 0; scroll = 0; }).setAccent(true));
        this.addRenderableWidget(new StyledButton(this.font, this.width / 2 + 10, 30, 100, 20, I18n.t("iscript.quest.journal.completed"), () -> { tab = 1; scroll = 0; }));
        this.addRenderableWidget(new StyledButton(this.font, this.width / 2 - 50, this.height - 30, 100, 20, I18n.t("iscript.quest.journal.close"), this::onClose));
    }

    private String getTitle(String id) {
        return titles.getOrDefault(id, id);
    }

    private int getTotalCount() {
        int count = 0;
        for (java.util.Map.Entry<String, Integer> e : progress.entrySet()) {
            boolean done = completed.getOrDefault(e.getKey(), false);
            if ((tab == 0 && !done) || (tab == 1 && done)) count++;
        }
        if (tab == 1) {
            for (java.util.Map.Entry<String, Boolean> e : completed.entrySet()) {
                if (!progress.containsKey(e.getKey())) count++;
            }
        }
        return count;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.TEXT);

        int y = 60;
        int count = 0;
        for (Map.Entry<String, Integer> e : progress.entrySet()) {
            boolean done = completed.getOrDefault(e.getKey(), false);
            if ((tab == 0 && !done) || (tab == 1 && done)) {
                if (count >= scroll && count < scroll + 12) {
                    int color = done ? Theme.ACCENT : 0xFFFFAA00;
                    String status = done ? I18n.s("iscript.quest.journal.status.done") : I18n.s("iscript.quest.journal.status.progress", e.getValue());
                    boolean hovered = mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100
                            && mouseY >= y && mouseY <= y + 14;
                    if (hovered) {
                        graphics.fill(this.width / 2 - 105, y - 2, this.width / 2 + 105, y + 14, Theme.alpha(Theme.BG_HOVER, 0.3f));
                    }
                    graphics.drawString(this.font, getTitle(e.getKey()) + status, this.width / 2 - 100, y, color);
                    y += 16;
                }
                count++;
            }
        }
        for (Map.Entry<String, Boolean> e : completed.entrySet()) {
            if (tab == 1 && !progress.containsKey(e.getKey())) {
                if (count >= scroll && count < scroll + 12) {
                    graphics.drawString(this.font, getTitle(e.getKey()) + I18n.s("iscript.quest.journal.status.done"), this.width / 2 - 100, y, Theme.ACCENT);
                    y += 16;
                }
                count++;
            }
        }

        if (scroll > 0) {
            graphics.drawCenteredString(this.font, "^", this.width / 2, 55, Theme.TEXT_DIM);
        }
        if (count > scroll + 12) {
            graphics.drawCenteredString(this.font, "v", this.width / 2, this.height - 50, Theme.TEXT_DIM);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalCount = getTotalCount();
        int visibleCount = Math.max(1, (this.height - 50 - 60) / 16);
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, Math.max(0, totalCount - visibleCount));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}