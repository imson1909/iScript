package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class AbilitiesOverlay {
    private final Minecraft mc;
    private final Font font;
    private final MorphEditorScreen screen;

    private final String[] abilities = {"Полет (Fly)", "Плавание (Swim)", "Лазание (Climb)", "Иммунитет к огню", "Дыхание под водой", "Ночное зрение"};

    public AbilitiesOverlay(MorphEditorScreen screen) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void init() {}

    private boolean getState(int i) {
        switch (i) {
            case 0: return screen.canFly;
            case 1: return screen.canSwim;
            case 2: return screen.canClimb;
            case 3: return screen.fireImmune;
            case 4: return screen.waterBreathing;
            case 5: return screen.nightVision;
        }
        return false;
    }

    private void toggleState(int i) {
        switch (i) {
            case 0: screen.canFly = !screen.canFly; break;
            case 1: screen.canSwim = !screen.canSwim; break;
            case 2: screen.canClimb = !screen.canClimb; break;
            case 3: screen.fireImmune = !screen.fireImmune; break;
            case 4: screen.waterBreathing = !screen.waterBreathing; break;
            case 5: screen.nightVision = !screen.nightVision; break;
        }
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        int centerX = screen.width / 2 - 150;
        int centerY = 60;
        int panelW = 300;
        int panelH = abilities.length * 30 + 20;

        g.fill(centerX, centerY - 10, centerX + panelW, centerY + panelH, 0xAA16161E);
        g.renderOutline(centerX, centerY - 10, panelW, panelH, Theme.BORDER);

        g.drawString(font, "Способности морфа:", centerX + 10, centerY, Theme.ACCENT);

        for (int i = 0; i < abilities.length; i++) {
            int by = centerY + 20 + i * 30;
            boolean hover = inRect(mx, my, centerX + 10, by, panelW - 20, 24);

            g.fill(centerX + 10, by, centerX + 26, by + 16, 0xFF000000);
            g.renderOutline(centerX + 10, by, 16, 16, Theme.BORDER);

            if (getState(i)) {
                g.drawString(font, "V", centerX + 14, by + 4, 0xFF55FF55);
            }

            int textColor = hover ? Theme.TEXT : Theme.TEXT_DIM;
            g.drawString(font, abilities[i], centerX + 34, by + 4, textColor);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int centerX = screen.width / 2 - 150;
        int centerY = 60;

        for (int i = 0; i < abilities.length; i++) {
            int by = centerY + 20 + i * 30;
            if (inRect(mx, my, centerX + 10, by, 280, 24)) {
                toggleState(i);
                return true;
            }
        }

        return false;
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }
}