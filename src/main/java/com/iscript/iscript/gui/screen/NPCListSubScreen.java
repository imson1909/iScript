package com.iscript.iscript.gui.screen;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.theme.UI;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.iscript.iscript.gui.widget.StyledButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NPCListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private EditBox searchBox;
    private StyledButton newNpcButton;
    private List<NPCData> npcCache = new ArrayList<>();
    private boolean waitingForServer = false;

    private static final int TOP_PADDING = 8;
    private static final int ITEM_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 28;

    public NPCListSubScreen(DashboardScreen parent) {
        super(parent);
    }

    @Override
    protected void init() {
        if (newNpcButton != null) {
            parent.removeEditorWidget(newNpcButton);
            newNpcButton = null;
        }
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }

        scroll = 0;
        npcCache = new ArrayList<>(NPCManager.getClientCache());

        if (npcCache.isEmpty()) {
            waitingForServer = true;
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_NPC_LIST, new CompoundTag()));
        } else {
            waitingForServer = false;
        }

        int tabX = DashboardScreen.SIDEBAR_W;
        int tabY = DashboardScreen.TOPBAR_H;
        int tabW = this.parent.width - DashboardScreen.SIDEBAR_W;

        int searchW = 140;
        int searchX = tabX + tabW - searchW - 8;
        int searchY = tabY + TOP_PADDING;

        searchBox = new EditBox(this.font, searchX, searchY, searchW, 18, I18n.t("iscript.npc.list.search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(Theme.TEXT);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);

        newNpcButton = new StyledButton(this.font, tabX + 8, searchY, 80, 18, I18n.t("iscript.npc.list.new_npc"), () -> {
            String newId = "npc_" + System.currentTimeMillis();
            NPCData data = new NPCData();
            data.setId(newId);
            data.setName(I18n.s("iscript.npc.list.new_npc"));
            Minecraft.getInstance().setScreen(new NPCEditScreen(-1, data));
        }).setAccent(true);
        parent.addWidget(newNpcButton);
    }

    @Override
    public void removed() {
        if (searchBox != null) {
            parent.removeEditorWidget(searchBox);
            searchBox = null;
        }
        if (newNpcButton != null) {
            parent.removeEditorWidget(newNpcButton);
            newNpcButton = null;
        }
        super.removed();
    }

    public void receiveList(List<NPCData> list) {
        IScriptMod.LOGGER.info("Received {} NPCs from server", list.size());
        NPCManager.updateClientCache(list);
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                npcCache = new ArrayList<>(list);
                waitingForServer = false;
                scroll = 0;
            });
        }
    }

    private List<NPCData> getFilteredList() {
        String filter = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<NPCData> result = new ArrayList<>();
        for (NPCData data : npcCache) {
            if (data == null) continue;
            String name = data.getName();
            if (name == null) name = "";
            name = name.trim();
            if (filter.isEmpty() || name.toLowerCase().contains(filter)) {
                result.add(data);
            }
        }
        result.sort((a, b) -> {
            String na = a.getName();
            String nb = b.getName();
            if (na == null) na = "";
            if (nb == null) nb = "";
            return na.compareToIgnoreCase(nb);
        });
        return result;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        UI.inner(g, x, y, w, h);

        if (waitingForServer && npcCache.isEmpty()) {
            UI.centerLabel(g, this.font, I18n.s("iscript.npc.list.loading"), x, y + h / 2, w);
            return;
        }

        if (npcCache.isEmpty()) {
            UI.centerLabel(g, this.font, I18n.s("iscript.npc.list.empty"), x, y + h / 2, w);
            return;
        }

        int listX = x + 8;
        int listY = y + TOP_PADDING + HEADER_HEIGHT;
        int listW = w - 16;
        int listH = h - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        List<NPCData> filtered = getFilteredList();
        int shown = 0;

        for (int i = scroll; i < Math.min(scroll + maxVisible, filtered.size()); i++) {
            NPCData data = filtered.get(i);

            String id = data.getId();
            if (id == null || id.isEmpty()) id = I18n.s("iscript.npc.list.unnamed");
            String name = data.getName();
            if (name == null || name.isEmpty()) name = I18n.s("iscript.npc.list.unnamed");

            int rowY = listY + shown * ITEM_HEIGHT;
            boolean hovered = mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ITEM_HEIGHT - 2;

            UI.row(g, listX, rowY, listW, ITEM_HEIGHT - 2, false, hovered);
            g.drawString(this.font, name + " [" + id + "]", listX + 4, rowY + 4, Theme.TEXT);

            int btnY = rowY + 2;
            int btnH = ITEM_HEIGHT - 4;
            int spawnX = listX + listW - 42;
            int delX = listX + listW - 80;
            int editX = listX + listW - 118;

            boolean spawnHover = mx >= spawnX && mx <= spawnX + 38 && my >= btnY && my <= btnY + btnH;
            UI.buttonBg(g, spawnX, btnY, 38, btnH, spawnHover, true);
            g.drawCenteredString(this.font, I18n.s("iscript.npc.list.spawn"), spawnX + 19, btnY + (btnH - 8) / 2, spawnHover ? Theme.ACCENT : 0xFF44AA44);

            boolean delHover = mx >= delX && mx <= delX + 36 && my >= btnY && my <= btnY + btnH;
            UI.buttonBg(g, delX, btnY, 36, btnH, delHover, true);
            g.drawCenteredString(this.font, "X", delX + 18, btnY + (btnH - 8) / 2, delHover ? Theme.ERROR : 0xFFAA4444);

            boolean editHover = mx >= editX && mx <= editX + 36 && my >= btnY && my <= btnY + btnH;
            UI.buttonBg(g, editX, btnY, 36, btnH, editHover, true);
            g.drawCenteredString(this.font, I18n.s("iscript.npc.list.edit"), editX + 18, btnY + (btnH - 8) / 2, editHover ? Theme.TEXT : Theme.TEXT_DIM);

            shown++;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int tabX = DashboardScreen.SIDEBAR_W;
        int tabY = DashboardScreen.TOPBAR_H;
        int tabW = this.parent.width - DashboardScreen.SIDEBAR_W;
        int tabH = this.parent.height - DashboardScreen.TOPBAR_H;

        int listX = tabX + 8;
        int listY = tabY + TOP_PADDING + HEADER_HEIGHT;
        int listW = tabW - 16;
        int listH = tabH - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        List<NPCData> filtered = getFilteredList();

        for (int i = scroll; i < Math.min(scroll + maxVisible, filtered.size()); i++) {
            NPCData data = filtered.get(i);

            String id = data.getId();
            if (id == null || id.isEmpty()) id = I18n.s("iscript.npc.list.unnamed");
            String name = data.getName();
            if (name == null || name.isEmpty()) name = I18n.s("iscript.npc.list.unnamed");

            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            int btnH = ITEM_HEIGHT - 4;
            int btnY = rowY + 2;
            int spawnX = listX + listW - 42;
            int delX = listX + listW - 80;
            int editX = listX + listW - 118;

            if (mx >= spawnX && mx <= spawnX + 38 && my >= btnY && my <= btnY + btnH) {
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SPAWN_NPC, ServerCommandPacket.spawnNPCToTag(data.getId())));
                return true;
            }
            if (mx >= delX && mx <= delX + 36 && my >= btnY && my <= btnY + btnH) {
                final String removeId = data.getId();
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.DELETE_NPC, ServerCommandPacket.deleteNPCToTag(removeId)));
                this.minecraft.execute(() -> {
                    npcCache.removeIf(d -> d != null && removeId.equals(d.getId()));
                    NPCManager.updateClientCache(new ArrayList<>(npcCache));
                });
                scroll = 0;
                return true;
            }
            if (mx >= editX && mx <= editX + 36 && my >= btnY && my <= btnY + btnH) {
                Minecraft.getInstance().setScreen(new NPCEditScreen(-1, data));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        List<NPCData> filtered = getFilteredList();
        int tabH = this.parent.height - DashboardScreen.TOPBAR_H;
        int listH = tabH - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);
        int maxScroll = Math.max(0, filtered.size() - maxVisible);

        if (delta > 0) {
            scroll = Math.max(0, scroll - 1);
        } else {
            scroll = Math.min(scroll + 1, maxScroll);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}