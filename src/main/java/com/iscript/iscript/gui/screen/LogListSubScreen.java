package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class LogListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 28;
    private int selectedIndex = -1;
    private long lastClickTime = 0;
    private int lastClickIndex = -1;

    public LogListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        scroll = 0;
        selectedIndex = -1;
        lastClickTime = 0;
        lastClickIndex = -1;
    }

    private int getClearButtonWidth() {
        return this.font.width(I18n.s("iscript.log.list.clear")) + 12;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        UI.inner(g, x, y, w, h);

        g.drawString(this.font, I18n.s("iscript.log.list.title"), x + 8, y + 6, Theme.ACCENT);

        int clearW = getClearButtonWidth();
        int clearX = x + w - clearW - 8;
        int clearY = y + 4;
        boolean clearHover = mx >= clearX && mx <= clearX + clearW && my >= clearY && my <= clearY + 18;
        UI.buttonBg(g, clearX, clearY, clearW, 18, clearHover, true);
        g.drawCenteredString(this.font, I18n.s("iscript.log.list.clear"), clearX + clearW / 2, clearY + 5, clearHover ? Theme.ERROR : Theme.TEXT_DIM);

        int listX = x + 8;
        int listY = y + HEADER_HEIGHT;
        int listW = w - 16;
        int listH = h - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        var entries = ScriptLog.get().getEntries();
        int total = entries.size();
        int maxScroll = Math.max(0, total - maxVisible);
        if (scroll > maxScroll) scroll = maxScroll;

        for (int i = scroll; i < Math.min(scroll + maxVisible, total); i++) {
            var e = entries.get(i);
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ITEM_HEIGHT - 2;
            boolean selected = i == selectedIndex;

            int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : 0x00000000);
            g.fill(listX, rowY, listX + listW, rowY + ITEM_HEIGHT - 2, bg);

            long sec = (e.timestamp() / 1000) % 3600;
            String time = String.format("%02d:%02d", sec / 60, sec % 60);
            String prefix = "[" + time + "] ";
            int prefixW = this.font.width(prefix);
            g.drawString(this.font, prefix, listX + 4, rowY + 3, 0xFF666666);

            String msg = this.font.plainSubstrByWidth(e.message(), listW - prefixW - 12);
            g.drawString(this.font, msg, listX + 4 + prefixW, rowY + 3, e.level().color);
        }

        if (entries.isEmpty()) {
            UI.centerLabel(g, this.font, I18n.s("iscript.log.list.empty"), x, y + h / 2, w);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;

        int clearW = getClearButtonWidth();
        int clearX = x + w - clearW - 8;
        int clearY = y + 4;
        if (mx >= clearX && mx <= clearX + clearW && my >= clearY && my <= clearY + 18) {
            ScriptLog.get().clear();
            scroll = 0;
            selectedIndex = -1;
            lastClickTime = 0;
            lastClickIndex = -1;
            return true;
        }

        int listX = x + 8;
        int listY = y + HEADER_HEIGHT;
        int listW = w - 16;
        int listH = h - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        var entries = ScriptLog.get().getEntries();
        int total = entries.size();

        for (int i = scroll; i < Math.min(scroll + maxVisible, total); i++) {
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            if (mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ITEM_HEIGHT - 2) {
                selectedIndex = i;
                var e = entries.get(i);
                long now = System.currentTimeMillis();
                if (i == lastClickIndex && now - lastClickTime < 300) {
                    lastClickTime = 0;
                    lastClickIndex = -1;
                    String sf = e.sourceFile();
                    int sl = e.sourceLine();
                    if (sf != null && !sf.isEmpty()) {
                        int line = sl > 0 ? sl - 1 : 0;
                        parent.openScript(sf, line);
                    }
                } else {
                    lastClickTime = now;
                    lastClickIndex = i;
                    String text = e.message();
                    if (text != null && !text.isEmpty()) {
                        parent.clipboard = text;
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - DashboardScreen.SIDEBAR_W;
        int h = this.parent.height - DashboardScreen.TOPBAR_H;

        int listX = x + 8;
        int listY = y + HEADER_HEIGHT;
        int listW = w - 16;
        int listH = h - HEADER_HEIGHT - 8;

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            var entries = ScriptLog.get().getEntries();
            int maxVisible = Math.max(1, listH / ITEM_HEIGHT);
            int maxScroll = Math.max(0, entries.size() - maxVisible);
            if (delta > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(scroll + 1, maxScroll);
            return true;
        }
        return false;
    }
}