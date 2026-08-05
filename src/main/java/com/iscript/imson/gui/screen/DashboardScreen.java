package com.iscript.imson.gui.screen;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.gui.screen.ListSubScreen.*;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class DashboardScreen extends Screen {
    public static final int SIDEBAR_W = 32;
    public static final int TOPBAR_H = 28;
    private static final int ICON_SZ = 24;
    private static final int ICON_PAD = 4;

    private DashboardTab currentTab;
    public Screen currentSubScreen = null;
    private DashboardTab pendingTab = null;
    private AbstractWidget focusedWidget = null;
    public static String clipboard = "";

    public static final ScriptEditorState EDITOR_STATE = new ScriptEditorState();

    private static final Map<DashboardTab, SubScreen> GLOBAL_CACHE = new HashMap<>();
    private static DashboardTab globalTab = DashboardTab.SCRIPTS;

    public DashboardScreen() {
        super(I18n.t("iscript.dashboard.title"));
        this.currentTab = globalTab;
    }

    public void setFocusedWidget(AbstractWidget w) {
        if (this.focusedWidget != null && this.focusedWidget != w) {
            this.focusedWidget.setFocused(false);
        }
        this.focusedWidget = w;
        if (w != null) w.setFocused(true);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.focusedWidget = null;
        addTabButtons();
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (currentSubScreen == null) switchTab(currentTab);
        else {
            currentSubScreen.init(this.minecraft, this.width, this.height);
            if (currentSubScreen instanceof SubScreen sub) {
                sub.reinit();
            }
        }
    }

    private void addTabButtons() {
        int y = TOPBAR_H + 8;
        for (DashboardTab tab : DashboardTab.values()) {
            final DashboardTab t = tab;
            this.addRenderableWidget(new IconBtn(4, y, ICON_SZ, ICON_SZ, tab.icon, tab.color, tab, () -> queueSwitchTab(t)));
            y += ICON_SZ + ICON_PAD;
        }
    }

    public AbstractWidget addWidget(AbstractWidget w) {
        return this.addRenderableWidget(w);
    }

    public void removeEditorWidget(AbstractWidget w) {
        if (w == focusedWidget) focusedWidget = null;
        this.removeWidget(w);
    }

    private void queueSwitchTab(DashboardTab tab) {
        if (tab != currentTab) this.pendingTab = tab;
    }

    @Override
    public void tick() {
        if (pendingTab != null && this.minecraft != null) {
            final DashboardTab t = pendingTab;
            pendingTab = null;
            switchTab(t);
        }
        if (currentSubScreen != null) currentSubScreen.tick();
        super.tick();
    }

    private void switchTab(DashboardTab tab) {

        if (currentTab != null && currentTab != tab) {
            SubScreen old = GLOBAL_CACHE.get(currentTab);
            if (old != null) {
                old.lifecycle.removed();
                old.resetState();
            }
            GLOBAL_CACHE.remove(currentTab);
        }

        this.clearWidgets();
        this.focusedWidget = null;
        addTabButtons();

        this.currentTab = tab;
        globalTab = tab;
        if (this.minecraft == null || this.minecraft.player == null) {
            this.currentSubScreen = null;
            return;
        }

        SubScreen sub = GLOBAL_CACHE.computeIfAbsent(tab, this::createSubScreen);
        sub.setParent(this);
        this.currentSubScreen = sub;
        this.currentSubScreen.init(this.minecraft, this.width, this.height);
        sub.reinit();
        requestListForTab(tab);
    }

    private SubScreen createSubScreen(DashboardTab tab) {
        return switch (tab) {
            case SCRIPTS -> new Script(this);
            case DIALOGS -> new Dialog(this);
            case QUESTS -> new Quest(this);
            case EVENTS -> new EventGraph(this);
            case REGIONS -> new Region(this);
            case CUTSCENES -> new Cutscene(this);
            case NPCS -> new NPC(this);
            case STATES -> new State(this);
            case TRIGGERS -> new Trigger(this);
            case LOGS -> new Log(this);
        };
    }

    private void requestListForTab(DashboardTab tab) {
        String category = switch (tab) {
            case SCRIPTS -> "scripts";
            case DIALOGS -> "dialogs";
            case QUESTS -> "quests";
            case EVENTS -> "events";
            case REGIONS -> "regions";
            case CUTSCENES -> "cutscenes";
            case NPCS -> "npcs";
            case STATES -> "states";
            case TRIGGERS -> "triggers";
            case LOGS -> null;
        };
        if (category != null && !category.isEmpty()) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(
                    ServerCommandPacket.Type.REQUEST_DASHBOARD_LIST,
                    ServerCommandPacket.requestDashboardListToTag(category)
            ));
        }
    }

    public void receiveDashboardList(CompoundTag data) {
        if (currentSubScreen instanceof SubScreen sub) {
            sub.receiveDashboardList(data);
        }
    }
    public void receiveTriggers(CompoundTag data) {
        if (currentSubScreen instanceof Trigger sub) {
            sub.receiveTriggers(data);
        }
    }

    public void openScript(String scriptId, int line) {
        IScriptMod.LOGGER.info("[IScript] openScript called: id={} line={}", scriptId, line);
        if (scriptId == null || scriptId.isEmpty()) {
            IScriptMod.LOGGER.warn("[IScript] openScript ABORT: scriptId is null or empty");
            return;
        }
        EDITOR_STATE.selectedId = scriptId;
        EDITOR_STATE.errorLine = line;
        EDITOR_STATE.pendingContent = null;
        EDITOR_STATE.isLoading = true;
        EDITOR_STATE.lastSentText = "";
        IScriptMod.LOGGER.info("[IScript] editorState set: selectedId={} errorLine={} isLoading=true", scriptId, line);
        IScriptNetwork.sendToServer(new ServerCommandPacket(
                ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT,
                ServerCommandPacket.requestScriptToTag(scriptId)
        ));
        IScriptMod.LOGGER.info("[IScript] REQUEST_SCRIPT_CONTENT sent for {}", scriptId);
        switchTab(DashboardTab.SCRIPTS);
        IScriptMod.LOGGER.info("[IScript] switchTab(SCRIPTS) done");
    }

    public void onScriptContentReceived(String id, String text) {
        IScriptMod.LOGGER.info("[IScript] onScriptContentReceived: id={} textLen={}", id, text != null ? text.length() : -1);
        IScriptMod.LOGGER.info("[IScript] EDITOR_STATE.selectedId={}", EDITOR_STATE.selectedId);
        if (EDITOR_STATE.selectedId != null && EDITOR_STATE.selectedId.equals(id)) {
            EDITOR_STATE.pendingContent = text;
            EDITOR_STATE.isLoading = false;
            IScriptMod.LOGGER.info("[IScript] Content ACCEPTED for {}", id);
        } else {
            IScriptMod.LOGGER.warn("[IScript] Content REJECTED: expected={} got={}", EDITOR_STATE.selectedId, id);
        }
    }

    @Override
    public void removed() {
        if (currentSubScreen instanceof SubScreen sub) {
            sub.lifecycle.removed();
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        UI.panel(g, 0, 0, SIDEBAR_W, this.height);
        UI.panel(g, SIDEBAR_W, 0, this.width - SIDEBAR_W, TOPBAR_H);
        g.drawString(this.font, I18n.s(currentTab.titleKey), SIDEBAR_W + 10, 9, Theme.TEXT);

        if (currentSubScreen != null)
            ((SubScreen) currentSubScreen).render(g, mx, my, pt, SIDEBAR_W, TOPBAR_H, this.width - SIDEBAR_W, this.height - TOPBAR_H);

        super.render(g, mx, my, pt);

        for (var w : this.renderables) {
            if (w instanceof IconBtn btn && btn.tab == currentTab)
                g.renderOutline(btn.getX() - 2, btn.getY() - 2, btn.getWidth() + 4, btn.getHeight() + 4, Theme.ACCENT);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256) {
            this.onClose();
            if (this.minecraft != null) this.minecraft.setScreen(null);
            return true;
        }
        if (focusedWidget != null && focusedWidget.active && focusedWidget.visible)
            if (focusedWidget.keyPressed(key, scan, mod)) return true;
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (currentSubScreen != null && mx > SIDEBAR_W && my > TOPBAR_H)
            if (currentSubScreen.mouseClicked(mx, my, btn)) return true;
        if (focusedWidget != null) {
            focusedWidget.setFocused(false);
            focusedWidget = null;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (currentSubScreen != null && currentSubScreen.mouseReleased(mx, my, btn)) return true;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (currentSubScreen != null && currentSubScreen.mouseDragged(mx, my, btn, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        if (currentSubScreen != null && mx > SIDEBAR_W)
            if (currentSubScreen.mouseScrolled(mx, my, d)) return true;
        return super.mouseScrolled(mx, my, d);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (focusedWidget != null && focusedWidget.active && focusedWidget.visible)
            if (focusedWidget.charTyped(c, mod)) return true;
        return super.charTyped(c, mod);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    enum DashboardTab {
        SCRIPTS("iscript.dashboard.tab.scripts", "{ }", 0xFF4488AA),
        DIALOGS("iscript.dashboard.tab.dialogs", "\"", 0xFFAA44AA),
        QUESTS("iscript.dashboard.tab.quests", "!", 0xFF44AA88),
        EVENTS("iscript.dashboard.tab.events", "\u26A1", 0xFFAA8822),
        REGIONS("iscript.dashboard.tab.regions", "\u25A0", 0xFF6666AA),
        CUTSCENES("iscript.dashboard.tab.cutscenes", "\u25B6", 0xFFAA2266),
        NPCS("iscript.dashboard.tab.npcs", "\u263A", 0xFF44AA44),
        STATES("iscript.dashboard.tab.states", "\u25A0", 0xFFAA44AA),
        TRIGGERS("iscript.dashboard.tab.triggers", "\u2694", 0xFFCC4444),
        LOGS("iscript.dashboard.tab.logs", "\u25A3", 0xFF888888);

        final String titleKey, icon;
        final int color;
        DashboardTab(String k, String i, int c) { titleKey = k; icon = i; color = c; }
    }

    static class IconBtn extends AbstractWidget {
        final String icon;
        final int color;
        final Runnable onClick;
        DashboardTab tab;

        IconBtn(int x, int y, int w, int h, String i, int c, DashboardTab t, Runnable r) {
            super(x, y, w, h, Component.empty());
            icon = i; color = c; tab = t; onClick = r;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isMouseOver(mx, my);
            UI.buttonBg(g, getX(), getY(), width, height, hov, active);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, icon,
                    getX() + width / 2, getY() + (height - 8) / 2, active ? (hov ? Theme.TEXT : color) : Theme.TEXT_MUTE);
            if (hov && tab != null)
                g.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, I18n.t(tab.titleKey), mx, my);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            if (active && isMouseOver(mx, my)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }

    public static abstract class SubScreen extends Screen {
        protected DashboardScreen parent;
        public int savedScrollX = 0;
        public int savedScrollY = 0;
        public int savedSelectedIndex = -1;
        public final SubScreenLifecycle lifecycle;

        public SubScreen(DashboardScreen p) {
            super(Component.empty());
            parent = p;
            lifecycle = new SubScreenLifecycle(this);
        }

        public void setParent(DashboardScreen p) {
            this.parent = p;
        }

        public void reinit() {
            this.init();
        }

        public void resetState() {}

        public abstract void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h);

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {}

        public boolean mouseScrolled(double mx, double my, double d) {
            return false;
        }

        public void receiveDashboardList(CompoundTag data) {}
    }

    public static class ScriptEditorState {
        public String selectedId = null;
        public String pendingContent = null;
        public int errorLine = -1;
        public String lastSentText = "";
        public boolean isLoading = false;
    }
}