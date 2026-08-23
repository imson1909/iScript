package com.iscript.imson.gui.screen.ListSubScreen;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.data.NPCManager;
import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.gui.screen.DashboardScreen;
import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.screen.NPCEditScreen;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class NPC extends ListSubScreen {
    private static final int RIGHT_PANEL_WIDTH = 220;
    private static final int ITEM_HEIGHT = 24;
    private List<NPCData> npcCache = new ArrayList<>();
    private boolean waitingForServer = false;
    private NPCData selectedNpc = null;

    public NPC(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected int getRightPanelWidth() { return RIGHT_PANEL_WIDTH; }

    @Override
    protected int getToolbarWidth() { return 0; }

    @Override
    protected String getListTitle() { return I18n.s("iscript.npc.list.title"); }

    @Override
    protected String getEmptyText() { return I18n.s("iscript.npc.list.empty"); }

    @Override
    public void init() {
        npcCache = new ArrayList<>(NPCManager.getClientCache());
        if (npcCache.isEmpty()) {
            waitingForServer = true;
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_NPC_LIST, new CompoundTag()));
        } else {
            waitingForServer = false;
        }
        super.init();
    }

    @Override
    protected List<String> getItemIds() {
        List<String> ids = new ArrayList<>();
        for (NPCData data : npcCache) {
            if (data != null && data.getId() != null) ids.add(data.getId());
        }
        return ids;
    }

    @Override
    protected String getItemDisplayName(String id) {
        NPCData data = findById(id);
        if (data == null) return id;
        String name = data.getName();
        if (name == null || name.isEmpty()) name = I18n.s("iscript.npc.list.unnamed");
        return name + " [" + id + "]";
    }

    @Override
    protected void onSelect(String id) {
        setSelectedId(id);
        selectedNpc = findById(id);
    }

    @Override
    protected void onNew(String id) {
        NPCData data = new NPCData();
        data.setId(id);
        data.setName(I18n.s("iscript.npc.list.new_npc"));
        npcCache.add(data);
        NPCManager.updateClientCache(new ArrayList<>(npcCache));
        onSelect(id);
    }

    @Override
    protected void onDelete(String id) {
        npcCache.removeIf(d -> d != null && id.equals(d.getId()));
        NPCManager.updateClientCache(new ArrayList<>(npcCache));
        if (selectedNpc != null && id.equals(selectedNpc.getId())) selectedNpc = null;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_NPC, ServerCommandPacket.deleteNPCToTag(id)));
    }

    @Override
    protected void onRename(String oldId, String newId) {
        NPCData data = findById(oldId);
        if (data == null || oldId.equals(newId)) return;
        data.setId(newId);
        if (selectedNpc == data) selectedNpc = data;
    }

    @Override
    protected void onDuplicate(String id) {
        NPCData src = findById(id);
        if (src == null) return;
        String base = id;
        String newId = id + "_1";
        int counter = 1;
        while (findById(newId) != null) {
            counter++;
            newId = base + "_" + counter;
        }
        NPCData copy = new NPCData();
        copy.setId(newId);
        copy.setName(src.getName() + I18n.s("iscript.npc.list.duplicate_suffix", counter));
        npcCache.add(copy);
        NPCManager.updateClientCache(new ArrayList<>(npcCache));
        onSelect(newId);
    }

    @Override
    protected void onCopy(String id) { DashboardScreen.clipboard = id; }

    @Override
    protected void onPaste() {
        String srcId = DashboardScreen.clipboard;
        if (srcId == null || srcId.isEmpty()) return;
        NPCData src = findById(srcId);
        if (src == null) return;
        String base = srcId + "_copy";
        String newId = base;
        int counter = 1;
        while (findById(newId) != null) {
            newId = base + "_" + counter;
            counter++;
        }
        NPCData copy = new NPCData();
        copy.setId(newId);
        copy.setName(src.getName() + I18n.s("iscript.npc.list.copy_suffix"));
        npcCache.add(copy);
        NPCManager.updateClientCache(new ArrayList<>(npcCache));
        onSelect(newId);
    }

    @Override
    protected boolean canPaste() {
        return DashboardScreen.clipboard != null && !DashboardScreen.clipboard.isEmpty() && findById(DashboardScreen.clipboard) != null;
    }

    @Override
    protected void renderEditor(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (selectedNpc == null) {
            g.drawCenteredString(font, I18n.s("iscript.npc.editor.empty"), x + w / 2, y + h / 2, Theme.TEXT_MUTE);
            return;
        }
        UI.panel(g, x, y, w, h);
        int ly = y + 10;
        UI.title(g, font, selectedNpc.getName(), x + 10, ly); ly += 18;
        UI.label(g, font, "ID: " + selectedNpc.getId(), x + 10, ly); ly += 14;
        ly += 10;
        int btnW = 80;
        int btnX = x + (w - btnW) / 2;
        boolean btnHov = mx >= btnX && mx <= btnX + btnW && my >= ly && my <= ly + 24;
        UI.buttonBg(g, btnX, ly, btnW, 24, btnHov, true);
        g.drawCenteredString(font, I18n.s("iscript.npc.editor.open"), btnX + btnW / 2, ly + 7, Theme.TEXT);
    }

    @Override
    protected boolean handleEditorClick(double mx, double my, int button, int leftX, int leftY, int leftW, int leftH) {
        if (button != 0 || selectedNpc == null) return false;
        int btnW = 80;
        int btnX = leftX + (leftW - btnW) / 2;
        int btnY = leftY + 10 + 18 + 14 + 10;
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + 24) {
            Minecraft.getInstance().setScreen(new NPCEditScreen(-1, selectedNpc));
            return true;
        }
        return false;
    }

    @Override
    protected void renderToolbar(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {}

    @Override
    protected boolean handleToolbarClick(double mx, double my, int button) { return false; }

    @Override
    protected void renderRowActions(GuiGraphics g, int mx, int my, float pt, String id, int x, int y, int w, int h) {
        NPCData data = findById(id);
        if (data == null) return;
        int btnH = ITEM_HEIGHT - 4;
        int btnY = y + 2;
        int spawnW = 28;
        int delW = 20;
        int editW = 24;
        int gap = 2;
        int spawnX = x + w - spawnW;
        int delX = spawnX - gap - delW;
        int editX = delX - gap - editW;

        boolean spawnHover = mx >= spawnX && mx <= spawnX + spawnW && my >= btnY && my <= btnY + btnH;
        g.fill(spawnX, btnY, spawnX + spawnW, btnY + btnH, spawnHover ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(spawnX, btnY, spawnW, btnH, Theme.BORDER);
        g.drawCenteredString(font, "⚔", spawnX + spawnW / 2, btnY + (btnH - 8) / 2, spawnHover ? Theme.ACCENT : 0xFF44AA44);

        boolean delHover = mx >= delX && mx <= delX + delW && my >= btnY && my <= btnY + btnH;
        g.fill(delX, btnY, delX + delW, btnY + btnH, delHover ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(delX, btnY, delW, btnH, Theme.BORDER);
        g.drawCenteredString(font, "✕", delX + delW / 2, btnY + (btnH - 8) / 2, delHover ? Theme.ERROR : 0xFFAA4444);

        boolean editHover = mx >= editX && mx <= editX + editW && my >= btnY && my <= btnY + btnH;
        g.fill(editX, btnY, editX + editW, btnY + btnH, editHover ? Theme.BG_HOVER : Theme.BG_INNER);
        g.renderOutline(editX, btnY, editW, btnH, Theme.BORDER);
        g.drawCenteredString(font, "✎", editX + editW / 2, btnY + (btnH - 8) / 2, editHover ? Theme.TEXT : Theme.TEXT_DIM);
    }

    @Override
    protected boolean handleRowClick(double mx, double my, int button, String id, int x, int y, int w, int h) {
        if (button != 0) return false;
        int btnH = ITEM_HEIGHT - 4;
        int btnY = y + 2;
        int spawnW = 28;
        int delW = 20;
        int editW = 24;
        int gap = 2;
        int spawnX = x + w - spawnW;
        int delX = spawnX - gap - delW;
        int editX = delX - gap - editW;

        if (mx >= spawnX && mx <= spawnX + spawnW && my >= btnY && my <= btnY + btnH) {
            NPCData data = findById(id);
            if (data != null) {
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SPAWN_NPC, ServerCommandPacket.spawnNPCToTag(data.getId())));
            }
            return true;
        }
        if (mx >= delX && mx <= delX + delW && my >= btnY && my <= btnY + btnH) {
            openConfirmDialog("delete", id);
            return true;
        }
        if (mx >= editX && mx <= editX + editW && my >= btnY && my <= btnY + btnH) {
            NPCData data = findById(id);
            if (data != null) Minecraft.getInstance().setScreen(new NPCEditScreen(-1, data));
            return true;
        }
        return false;
    }

    @Override
    protected void doSave() {}

    private NPCData findById(String id) {
        for (NPCData d : npcCache) if (d != null && id.equals(d.getId())) return d;
        return null;
    }

    public void receiveList(List<NPCData> list) {
        IScriptMod.LOGGER.info("Received {} NPCs from server", list.size());
        NPCManager.updateClientCache(list);
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                npcCache = new ArrayList<>(list);
                waitingForServer = false;
                lifecycle.selection().scroll(0);
            });
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        if (waitingForServer && npcCache.isEmpty()) {
            g.fill(x, y, x + w, y + h, Theme.BG_INNER);
            UI.centerLabel(g, font, I18n.s("iscript.npc.list.loading"), x, y + h / 2, w);
            return;
        }
        super.render(g, mx, my, pt, x, y, w, h);
    }
}