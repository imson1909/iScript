package com.iscript.iscript.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import com.iscript.iscript.gui.screen.StateListSubScreen;

public class DashboardScreen extends Screen {
    public static final int SIDEBAR_WIDTH = 28;
    public static final int TOPBAR_HEIGHT = 22;
    private static final int ICON_SIZE = 22;
    private static final int ICON_PADDING = 6;

    private DashboardTab currentTab = DashboardTab.SCRIPTS;
    public Screen currentSubScreen = null;
    private DashboardTab pendingTab = null;

    private AbstractWidget focusedWidget = null;
    public static String clipboard = "";

    public DashboardScreen() {
        super(Component.literal("iScript Dashboard"));
    }

    public void setFocusedWidget(AbstractWidget widget) {
        if (this.focusedWidget != null && this.focusedWidget != widget) {
            this.focusedWidget.setFocused(false);
        }
        this.focusedWidget = widget;
        if (widget != null) {
            widget.setFocused(true);
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.focusedWidget = null;

        if (this.minecraft == null || this.minecraft.player == null) return;

        int y = TOPBAR_HEIGHT + 6;
        for (DashboardTab tab : DashboardTab.values()) {
            final DashboardTab t = tab;
            this.addRenderableWidget(new IconButton(3, y, ICON_SIZE, ICON_SIZE, tab.icon, tab.color, tab, () -> queueSwitchTab(t)));
            y += ICON_SIZE + ICON_PADDING;
        }

        if (currentSubScreen == null) {
            switchTab(currentTab);
        } else {
            currentSubScreen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
        }
    }

    public AbstractWidget addWidget(AbstractWidget widget) {
        return this.addRenderableWidget(widget);
    }

    public void removeEditorWidget(AbstractWidget widget) {
        if (widget == focusedWidget) {
            focusedWidget = null;
        }
        this.removeWidget(widget);
    }

    private void queueSwitchTab(DashboardTab tab) {
        if (tab == currentTab) return;
        this.pendingTab = tab;
    }

    @Override
    public void tick() {
        if (pendingTab != null && this.minecraft != null) {
            final DashboardTab tab = pendingTab;
            pendingTab = null;
            switchTab(tab);
        }
        if (currentSubScreen != null) {
            currentSubScreen.tick();
        }
        super.tick();
    }

    private void switchTab(DashboardTab tab) {
        this.currentTab = tab;
        this.focusedWidget = null;

        if (this.minecraft == null || this.minecraft.player == null) {
            this.currentSubScreen = null;
            return;
        }

        ServerLevel level = null;
        if (this.minecraft.getSingleplayerServer() != null) {
            level = this.minecraft.getSingleplayerServer().getLevel(Level.OVERWORLD);
        }
        if (level == null) {
            this.currentSubScreen = null;
            return;
        }

        switch (tab) {
            case SCRIPTS -> this.currentSubScreen = new ScriptListSubScreen(this, level);
            case DIALOGS -> this.currentSubScreen = new DialogListSubScreen(this, level);
            case QUESTS -> this.currentSubScreen = new QuestListSubScreen(this, level);
            case EVENTS -> this.currentSubScreen = new EventGraphListSubScreen(this, level);
            case REGIONS -> this.currentSubScreen = new RegionListSubScreen(this, level);
            case CUTSCENES -> this.currentSubScreen = new CutsceneListSubScreen(this, level);
            case NPCS -> this.currentSubScreen = new NPCListSubScreen(this, level);
            case WORLD -> this.currentSubScreen = new WorldDataSubScreen(this, level);
            case STATES -> this.currentSubScreen = new StateListSubScreen(this, level);
        }

        if (this.currentSubScreen != null) {
            this.currentSubScreen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
        }
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void removed() {
        if (currentSubScreen != null) {
            currentSubScreen.removed();
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fill(0, 0, SIDEBAR_WIDTH, this.height, 0xFF1E1E2E);
        graphics.renderOutline(SIDEBAR_WIDTH - 1, 0, 1, this.height, 0xFF2A2A3A);

        graphics.fill(SIDEBAR_WIDTH, 0, this.width, TOPBAR_HEIGHT, 0xFF1A1A28);
        graphics.renderOutline(SIDEBAR_WIDTH, TOPBAR_HEIGHT - 1, this.width - SIDEBAR_WIDTH, 1, 0xFF2A2A3A);

        graphics.drawString(this.font, currentTab.title, SIDEBAR_WIDTH + 6, 6, 0xFFFFFFFF);

        int iconY = TOPBAR_HEIGHT + 6;
        for (int i = 0; i < DashboardTab.values().length - 1; i++) {
            int lineY = iconY + (i + 1) * (ICON_SIZE + ICON_PADDING) - ICON_PADDING / 2;
            graphics.fill(4, lineY, SIDEBAR_WIDTH - 4, lineY + 1, 0xFF2A2A3A);
        }

        if (currentSubScreen != null) {
            ((SubScreen) currentSubScreen).render(graphics, mouseX, mouseY, partialTick, SIDEBAR_WIDTH, TOPBAR_HEIGHT, this.width - SIDEBAR_WIDTH, this.height - TOPBAR_HEIGHT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        for (var widget : this.renderables) {
            if (widget instanceof IconButton btn && btn.tab == currentTab) {
                graphics.renderOutline(btn.getX() - 2, btn.getY() - 2, btn.getWidth() + 4, btn.getHeight() + 4, 0xFF00D4AA);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }
        if (focusedWidget != null && focusedWidget.active && focusedWidget.visible) {
            if (focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentSubScreen != null && mouseX > SIDEBAR_WIDTH && mouseY > TOPBAR_HEIGHT) {
            if (currentSubScreen.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (focusedWidget != null) {
            focusedWidget.setFocused(false);
            focusedWidget = null;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (currentSubScreen != null) {
            if (currentSubScreen.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (currentSubScreen != null) {
            if (currentSubScreen.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentSubScreen != null && mouseX > SIDEBAR_WIDTH) {
            if (currentSubScreen.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedWidget != null && focusedWidget.active && focusedWidget.visible) {
            if (focusedWidget.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum DashboardTab {
        SCRIPTS("Scripts", "{ }", 0xFF4488AA),
        DIALOGS("Dialogs", "\"\"", 0xFFAA44AA),
        QUESTS("Quests", "!", 0xFF44AA88),
        EVENTS("Events", "\u26A1", 0xFFAA8822),
        REGIONS("Regions", "\u25A0", 0xFF6666AA),
        CUTSCENES("Cutscenes", "\u25B6", 0xFFAA2266),
        NPCS("NPCs", "\u263A", 0xFF44AA44),
        WORLD("World", "\u2630", 0xFF888888),
        STATES("States", "\u25A0", 0xFFAA44AA);

        final String title;
        final String icon;
        final int color;

        DashboardTab(String title, String icon, int color) {
            this.title = title;
            this.icon = icon;
            this.color = color;
        }
    }

    private static class IconButton extends AbstractWidget {
        private final String icon;
        private final int color;
        private final Runnable onClick;
        DashboardTab tab;

        IconButton(int x, int y, int w, int h, String icon, int color, DashboardTab tab, Runnable onClick) {
            super(x, y, w, h, Component.empty());
            this.icon = icon;
            this.color = color;
            this.tab = tab;
            this.onClick = onClick;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            int bg = hovered ? 0xFF2A2A3A : 0x001E1E2E;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            int textColor = hovered ? 0xFFFFFFFF : color;
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, icon, getX() + width / 2, getY() + (height - 8) / 2, textColor);
            if (hovered && tab != null) {
                graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, Component.literal(tab.title), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    }

    public static abstract class SubScreen extends Screen {
        protected final DashboardScreen parent;
        protected final ServerLevel level;

        SubScreen(DashboardScreen parent, ServerLevel level) {
            super(Component.empty());
            this.parent = parent;
            this.level = level;
        }

        public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h);

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return false;
        }
    }
}