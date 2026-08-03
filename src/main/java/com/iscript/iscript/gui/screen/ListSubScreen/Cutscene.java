package com.iscript.iscript.gui.screen.ListSubScreen;

import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.iscript.iscript.gui.screen.I18n.t;
import com.iscript.iscript.gui.screen.CutsceneEditorScreen;

public class Cutscene extends DashboardScreen.SubScreen {
    private static final int ITEM_H = 26;
    private final List<Btn> buttons = new ArrayList<>();

    public Cutscene(DashboardScreen p) {
        super(p);
    }

    @Override
    public void init() {
        this.clearWidgets();
        buttons.clear();
        lifecycle.init();

        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int h = this.parent.height - y;

        lifecycle.search().request(x + 10, y + 10, 140, 18, t("iscript.cutscene.list.search"));

        int pad = 6;
        int gap = 4;
        int right = x + w - 20;
        int by = y + 16;
        int btm = y + h - 34;

        right = addAutoBtn(t("iscript.cutscene.list.edit").getString(), right, by, 18, this::editSelected, false, pad, gap);
        right = addAutoBtn(t("iscript.cutscene.list.delete").getString(), right, by, 18, this::deleteSelected, false, pad, gap);
        right = addAutoBtn(t("iscript.cutscene.list.new").getString(), right, by, 18, this::createNew, true, pad, gap);

        right = x + w - 10;
        right = addAutoBtn(t("iscript.cutscene.list.play").getString(), right, btm, 20, this::playSelected, false, pad, gap);
        right = addAutoBtn(t("iscript.cutscene.list.speed_2").getString(), right, btm, 20, () -> setSpeed(2.0f), true, pad, gap);
        right = addAutoBtn(t("iscript.cutscene.list.speed_1").getString(), right, btm, 20, () -> setSpeed(1.0f), true, pad, gap);
        addAutoBtn(t("iscript.cutscene.list.speed_05").getString(), right, btm, 20, () -> setSpeed(0.5f), true, pad, gap);

        updateButtons();
    }

    private void addBtn(String text, int x, int y, int w, int h, Runnable r, boolean accent) {
        Btn b = new Btn(x, y, w, h, text, r, accent);
        buttons.add(b);
        this.addRenderableWidget(b);
    }

    private int addAutoBtn(String text, int right, int y, int h, Runnable r, boolean accent, int pad, int gap) {
        int w = font.width(text) + pad * 2;
        int x = right - w;
        addBtn(text, x, y, w, h, r, accent);
        return x - gap;
    }

    private void setSpeed(float s) {
        for (Btn b : buttons) {
            if (b.text.equals(t("iscript.cutscene.list.speed_05").getString())) b.active = s != 0.5f;
            if (b.text.equals(t("iscript.cutscene.list.speed_1").getString())) b.active = s != 1.0f;
            if (b.text.equals(t("iscript.cutscene.list.speed_2").getString())) b.active = s != 2.0f;
        }
    }

    private void updateButtons() {
        boolean has = lifecycle.selection().get() != null;
        for (Btn b : buttons) {
            if (b.text.equals(t("iscript.cutscene.list.delete").getString()) || b.text.equals(t("iscript.cutscene.list.edit").getString()) || b.text.equals(t("iscript.cutscene.list.play").getString()))
                b.active = has;
        }
    }

    private void createNew() {
        String id = "cutscene_" + System.currentTimeMillis();
        CutsceneData d = new CutsceneData();
        d.setId(id); d.setName("New Cutscene");
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(d)));
        lifecycle.selection().set(id);
        updateButtons();
    }

    private void deleteSelected() {
        if (lifecycle.selection().get() == null) return;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_CUTSCENE, ServerCommandPacket.deleteCutsceneToTag(lifecycle.selection().get())));
        lifecycle.selection().set(null);
        updateButtons();
    }

    private void editSelected() {
        if (lifecycle.selection().get() == null) return;
        this.clearWidgets();
        this.removed();
        minecraft.setScreen(new CutsceneEditorScreen(DataAccess.cutscenes(), lifecycle.selection().get()));
    }

    private void playSelected() {
        if (lifecycle.selection().get() != null)
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(lifecycle.selection().get(), 1.0f, 0)));
    }

    @Override
    public void removed() {
        lifecycle.removed();
        super.removed();
    }

    @Override
    public void tick() {
        lifecycle.tick(null);
    }

    private List<String> filteredIds() {
        var all = DataAccess.cutscenes();
        String f = lifecycle.search().box() != null ? lifecycle.search().box().getValue().trim().toLowerCase() : lifecycle.state().lastSearch.trim().toLowerCase();
        List<String> r = new ArrayList<>();
        for (var e : all.entrySet()) {
            String name = e.getValue().getName();
            if (name == null) name = "";
            name = name.trim();
            if (f.isEmpty() || name.toLowerCase().contains(f))
                r.add(e.getKey());
        }
        Collections.sort(r);
        return r;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        lifecycle.search().setPos(x + 10, y + 4, 140, 16);
        lifecycle.search().setVisible(true);

        UI.inner(g, x, y, w, h);

        List<String> ids = filteredIds();
        int listW = lifecycle.selection().get() != null ? w / 2 - 12 : w - 20;
        int listY = y + 36;
        int visible = (h - 84) / ITEM_H;

        for (int i = lifecycle.selection().scroll(); i < Math.min(lifecycle.selection().scroll() + visible, ids.size()); i++) {
            String id = ids.get(i);
            var c = DataAccess.cutscenes().get(id);
            int ry = listY + (i - lifecycle.selection().scroll()) * ITEM_H;
            boolean hov = mx >= x + 10 && mx <= x + 10 + listW && my >= ry && my <= ry + 22;
            boolean sel = id.equals(lifecycle.selection().get());

            UI.row(g, x + 10, ry, listW, 22, sel, hov);

            String label = c.getName() + " (" + t("iscript.cutscene.list.actions_count", c.getActions().size()).getString() + ")";
            if (c.isLoop()) label += t("iscript.cutscene.list.loop_tag").getString();
            g.drawString(this.font, label, x + 16, ry + 6, sel ? Theme.TEXT : Theme.TEXT_DIM);
        }

        if (ids.isEmpty())
            UI.centerLabel(g, this.font, t("iscript.cutscene.list.no_cutscenes").getString(), x, y + h / 2, lifecycle.selection().get() != null ? listW : w);

        if (lifecycle.selection().get() != null) {
            var c = DataAccess.cutscenes().get(lifecycle.selection().get());
            if (c != null) {
                int dx = x + w / 2 + 4, dw = w / 2 - 16, dh = h - 20;
                UI.panel(g, dx, y + 10, dw, dh);
                int dy = y + 18;
                UI.title(g, this.font, t("iscript.cutscene.list.details").getString(), dx + 10, dy); dy += 18;
                UI.label(g, this.font, t("iscript.cutscene.list.id", lifecycle.selection().get()).getString(), dx + 10, dy); dy += 14;
                g.drawString(this.font, t("iscript.cutscene.list.name", c.getName()).getString(), dx + 10, dy, Theme.TEXT); dy += 14;
                UI.label(g, this.font, t("iscript.cutscene.list.loop", c.isLoop()).getString(), dx + 10, dy); dy += 14;
                UI.label(g, this.font, t("iscript.cutscene.list.actions", c.getActions().size()).getString(), dx + 10, dy);
            }
        }

        for (var r : this.renderables) r.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (lifecycle.search().box() != null && mx >= lifecycle.search().box().getX() && mx <= lifecycle.search().box().getX() + lifecycle.search().box().getWidth() && my >= lifecycle.search().box().getY() && my <= lifecycle.search().box().getY() + lifecycle.search().box().getHeight()) {
            lifecycle.search().box().setFocused(true);
            parent.setFocusedWidget(lifecycle.search().box());
            return lifecycle.search().box().mouseClicked(mx, my, b);
        }
        if (b != 0) return false;
        for (var c : this.children()) {
            if (c.mouseClicked(mx, my, b)) {
                if (c instanceof net.minecraft.client.gui.components.AbstractWidget w) parent.setFocusedWidget(w);
                return true;
            }
        }
        int x = DashboardScreen.SIDEBAR_W;
        int y = DashboardScreen.TOPBAR_H;
        int w = this.parent.width - x;
        int h = this.parent.height - y;
        List<String> ids = filteredIds();
        int listW = lifecycle.selection().get() != null ? w / 2 - 12 : w - 20;
        for (int i = lifecycle.selection().scroll(); i < Math.min(lifecycle.selection().scroll() + (h - 84) / ITEM_H, ids.size()); i++) {
            int ry = y + 36 + (i - lifecycle.selection().scroll()) * ITEM_H;
            if (mx >= x + 10 && mx <= x + 10 + listW && my >= ry && my <= ry + 22) {
                lifecycle.selection().set(ids.get(i));
                updateButtons();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        List<String> ids = filteredIds();
        int visible = (this.parent.height - DashboardScreen.TOPBAR_H - 84) / ITEM_H;
        int max = Math.max(0, ids.size() - visible);
        lifecycle.selection().scroll(d > 0 ? Math.max(0, lifecycle.selection().scroll() - 1) : Math.min(lifecycle.selection().scroll() + 1, max));
        return true;
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().keyPressed(k, s, m);
        }
        for (var c : this.children())
            if (c instanceof EditBox eb && eb.isFocused() && eb.keyPressed(k, s, m)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char c, int m) {
        if (lifecycle.search().box() != null && lifecycle.search().box().isFocused()) {
            return lifecycle.search().box().charTyped(c, m);
        }
        for (var ch : this.children())
            if (ch instanceof EditBox eb && eb.isFocused() && eb.charTyped(c, m)) return true;
        return false;
    }

    static class Btn extends net.minecraft.client.gui.components.AbstractWidget {
        final String text;
        final Runnable onClick;
        final boolean accent;

        Btn(int x, int y, int w, int h, String t, Runnable r, boolean a) {
            super(x, y, w, h, Component.empty());
            text = t; onClick = r; accent = a;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isMouseOver(mx, my);
            int bg = accent ? (hov ? Theme.alpha(Theme.ACCENT, 0.3f) : Theme.alpha(Theme.ACCENT, 0.15f))
                    : (hov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.renderOutline(getX(), getY(), width, height, hov ? Theme.ACCENT : Theme.BORDER);
            int tc = active ? (accent ? Theme.ACCENT : (hov ? Theme.TEXT : Theme.TEXT_DIM)) : Theme.TEXT_MUTE;
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, text, getX() + width / 2, getY() + (height - 8) / 2, tc);
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
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {}
    }
}