package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.morph.MorphData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MorphScreen extends Screen {
    private final Player player;
    private MorphData morphData;
    private MorphSubScreen[] subScreens;
    private int activeSubScreen = 0;

    public MorphScreen(Player player) {
        super(Component.literal(I18n.s("iscript.morph.title")));
        this.player = player;
        player.getCapability(MorphData.CAPABILITY).ifPresent(d -> this.morphData = d);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        subScreens = new MorphSubScreen[]{
                new ClassicMorphSubScreen(this, player, morphData),
                new AlternativeMorphSubScreen(this, player, morphData)
        };
        subScreens[activeSubScreen].init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int tabWidth = Math.max(80, this.width / 6);
        int tabHeight = 16;
        int tabY = 2;
        int tabSpacing = 4;
        for (int i = 0; i < subScreens.length; i++) {
            int tabX = 4 + i * (tabWidth + tabSpacing);
            boolean isActive = i == activeSubScreen;
            boolean isHovered = mx >= tabX && mx <= tabX + tabWidth && my >= tabY && my <= tabY + tabHeight;
            int bgColor = isActive ? Theme.ACCENT : (isHovered ? Theme.BG_HOVER : 0xFF1E1E26);
            g.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, bgColor);
            g.renderOutline(tabX, tabY, tabWidth, tabHeight, Theme.BORDER);
            String title = i == 0 ? I18n.s("iscript.morph.tab.classic") : I18n.s("iscript.morph.tab.alt");
            g.drawCenteredString(this.font, title, tabX + tabWidth / 2, tabY + 4, Theme.TEXT);
        }
        int contentY = tabHeight + 4;
        int contentH = this.height - contentY - 4;
        UI.panel(g, 4, contentY, this.width - 8, contentH);
        subScreens[activeSubScreen].render(g, mx, my, pt);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int tabWidth = Math.max(80, this.width / 6);
        int tabHeight = 16;
        int tabY = 2;
        int tabSpacing = 4;
        for (int i = 0; i < subScreens.length; i++) {
            int tabX = 4 + i * (tabWidth + tabSpacing);
            if (mx >= tabX && mx <= tabX + tabWidth && my >= tabY && my <= tabY + tabHeight) {
                if (activeSubScreen != i) {
                    activeSubScreen = i;
                    this.clearWidgets();
                    subScreens[activeSubScreen].init();
                }
                return true;
            }
        }
        return subScreens[activeSubScreen].mouseClicked(mx, my, btn) || super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        return subScreens[activeSubScreen].mouseReleased(mx, my, btn) || super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        return subScreens[activeSubScreen].mouseDragged(mx, my, btn, dx, dy) || super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return subScreens[activeSubScreen].mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void onClose() {
        for (MorphSubScreen subScreen : subScreens) {
            if (subScreen != null) subScreen.onClose();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    public int getContentY() {
        return 20;
    }

    public int getContentHeight() {
        return this.height - 24;
    }

    public int getScreenLeftPanelWidth() {
        return Math.max(120, this.width / 5);
    }

    public int getScreenRightPanelWidth() {
        return Math.max(100, this.width / 6);
    }

    public int getScreenCenterWidth() {
        return this.width - getScreenLeftPanelWidth() - getScreenRightPanelWidth() - 24;
    }
}