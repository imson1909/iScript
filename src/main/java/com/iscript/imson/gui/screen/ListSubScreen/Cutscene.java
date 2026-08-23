package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.cutscene.CutsceneData;
import com.iscript.imson.gui.screen.CutsceneEditorScreen;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cutscene extends ListSubScreen {
    private static final int RIGHT_PANEL_WIDTH = 180;
    private float playSpeed = 1.0f;

    public Cutscene(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.cutscene.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.cutscene.list.no_cutscenes"); }

    @Override
    protected List<String> getItemIds() {
        return new ArrayList<>(DataAccess.cutscenes().keySet());
    }

    @Override
    protected String getItemDisplayName(String id) {
        var c = DataAccess.cutscenes().get(id);
        if (c == null) return id;
        String name = c.getName();
        if (name == null || name.isEmpty()) name = id;
        String label = name + " (" + c.getActions().size() + ")";
        if (c.isLoop()) label += I18n.s("iscript.cutscene.list.loop_tag");
        return label;
    }

    @Override
    protected void onSelect(String id) {
        setSelectedId(id);
    }

    @Override
    protected void onNew(String id) {
        CutsceneData d = new CutsceneData();
        d.setId(id);
        d.setName("New Cutscene");
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(d)));
    }

    @Override
    protected void onDelete(String id) {
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_CUTSCENE, ServerCommandPacket.deleteCutsceneToTag(id)));
    }

    @Override
    protected void onRename(String oldId, String newId) {
        var c = DataAccess.cutscenes().get(oldId);
        if (c == null || oldId.equals(newId)) return;
        c.setId(newId);
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(c)));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_CUTSCENE, ServerCommandPacket.deleteCutsceneToTag(oldId)));
    }

    @Override
    protected void onDuplicate(String id) {
        var c = DataAccess.cutscenes().get(id);
        if (c == null) return;
        String base = id;
        String newId = id + "_1";
        int counter = 1;
        while (DataAccess.cutscenes().containsKey(newId)) {
            counter++;
            newId = base + "_" + counter;
        }
        CutsceneData copy = new CutsceneData();
        copy.setId(newId);
        copy.setName(c.getName() + I18n.s("iscript.cutscene.list.duplicate_suffix"));
        copy.setLoop(c.isLoop());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(copy)));
    }

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
        String srcId = DashboardScreen.clipboard;
        if (srcId == null || srcId.isEmpty()) return;
        var src = DataAccess.cutscenes().get(srcId);
        if (src == null) return;
        String base = srcId + "_copy";
        String newId = base;
        int counter = 1;
        while (DataAccess.cutscenes().containsKey(newId)) {
            newId = base + "_" + counter;
            counter++;
        }
        CutsceneData copy = new CutsceneData();
        copy.setId(newId);
        copy.setName(src.getName() + I18n.s("iscript.cutscene.list.copy_suffix"));
        copy.setLoop(src.isLoop());
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_CUTSCENE, ServerCommandPacket.saveCutsceneToTag(copy)));
    }

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty() && DataAccess.cutscenes().containsKey(DashboardScreen.clipboard);
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        String sel = getSelectedId();
        if (sel == null) {
            g.drawCenteredString(font, I18n.s("iscript.cutscene.editor.empty"), x + w / 2, y + h / 2, Theme.TEXT_MUTE);
            return;
        }
        var c = DataAccess.cutscenes().get(sel);
        if (c == null) return;

        UI.panel(g, x, y, w, h);
        int dy = y + 10;
        UI.title(g, font, I18n.s("iscript.cutscene.list.details"), x + 10, dy); dy += 18;
        UI.label(g, font, I18n.s("iscript.cutscene.list.id", sel), x + 10, dy); dy += 14;
        g.drawString(font, I18n.s("iscript.cutscene.list.name", c.getName()), x + 10, dy, Theme.TEXT); dy += 14;
        UI.label(g, font, I18n.s("iscript.cutscene.list.loop", c.isLoop()), x + 10, dy); dy += 14;
        UI.label(g, font, I18n.s("iscript.cutscene.list.actions", c.getActions().size()), x + 10, dy); dy += 20;

        int btnW = 60;
        int gap = 4;
        int startX = x + 10;

        for (int i = 0; i < 3; i++) {
            float speed = i == 0 ? 0.5f : (i == 1 ? 1.0f : 2.0f);
            String label = speed + "x";
            int bx = startX + i * (btnW + gap);
            boolean active = playSpeed == speed;
            boolean hov = mx >= bx && mx <= bx + btnW && my >= dy && my <= dy + 20;
            int bg = active ? 0xFF334455 : (hov ? Theme.BG_HOVER : Theme.BG_INNER);
            g.fill(bx, dy, bx + btnW, dy + 20, bg);
            g.renderOutline(bx, dy, btnW, 20, Theme.BORDER);
            g.drawCenteredString(font, label, bx + btnW / 2, dy + 5, active ? Theme.ACCENT : Theme.TEXT);
        }
        dy += 28;

        int playW = 80;
        int playX = x + 10;
        boolean playHov = mx >= playX && mx <= playX + playW && my >= dy && my <= dy + 24;
        UI.buttonBg(g, playX, dy, playW, 24, playHov, true);
        g.drawCenteredString(font, I18n.s("iscript.cutscene.list.play"), playX + playW / 2, dy + 7, Theme.ACCENT);
        dy += 32;

        int editW = 80;
        int editX = x + 10;
        boolean editHov = mx >= editX && mx <= editX + editW && my >= dy && my <= dy + 24;
        UI.buttonBg(g, editX, dy, editW, 24, editHov, true);
        g.drawCenteredString(font, I18n.s("iscript.cutscene.list.edit"), editX + editW / 2, dy + 7, Theme.TEXT);
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0 || getSelectedId() == null) return false;
        String sel = getSelectedId();
        var c = DataAccess.cutscenes().get(sel);
        if (c == null) return false;

        int dy = leftY + 10 + 18 + 14 + 14 + 14 + 20;
        int btnW = 60;
        int gap = 4;
        int startX = leftX + 10;
        for (int i = 0; i < 3; i++) {
            float speed = i == 0 ? 0.5f : (i == 1 ? 1.0f : 2.0f);
            int bx = startX + i * (btnW + gap);
            if (mx >= bx && mx <= bx + btnW && my >= dy && my <= dy + 20) {
                playSpeed = speed;
                return true;
            }
        }
        dy += 28;

        int playW = 80;
        int playX = leftX + 10;
        if (mx >= playX && mx <= playX + playW && my >= dy && my <= dy + 24) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.PLAY_CUTSCENE, ServerCommandPacket.playCutsceneToTag(sel, playSpeed, 0)));
            return true;
        }
        dy += 32;

        int editW = 80;
        int editX = leftX + 10;
        if (mx >= editX && mx <= editX + editW && my >= dy && my <= dy + 24) {
            Minecraft.getInstance().setScreen(new CutsceneEditorScreen(DataAccess.cutscenes(), sel));
            return true;
        }
        return false;
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) { return false; }

    @Override
    protected void doSave() {}
}