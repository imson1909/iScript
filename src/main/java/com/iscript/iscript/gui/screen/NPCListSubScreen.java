package com.iscript.iscript.gui.screen;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.RequestNPCListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.iscript.iscript.network.packet.DeleteNPCPacket;

import java.util.ArrayList;
import java.util.List;

public class NPCListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private EditBox searchBox;
    private Button newNpcButton;
    private List<NPCData> npcCache = new ArrayList<>();
    private boolean waitingForServer = false;

    private static final int TOP_PADDING = 8;
    private static final int ITEM_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 28;

    public NPCListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
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
            IScriptNetwork.sendToServer(new RequestNPCListPacket());
        } else {
            waitingForServer = false;
        }

        int tabX = DashboardScreen.SIDEBAR_WIDTH;
        int tabY = DashboardScreen.TOPBAR_HEIGHT;
        int tabW = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;

        int searchW = 140;
        int searchX = tabX + tabW - searchW - 8;
        int searchY = tabY + TOP_PADDING;

        searchBox = new EditBox(this.font, searchX, searchY, searchW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(s -> scroll = 0);
        parent.addWidget(searchBox);

        newNpcButton = Button.builder(Component.literal("New NPC"), btn -> {
            String newId = "npc_" + System.currentTimeMillis();
            NPCData data = new NPCData();
            data.setId(newId);
            data.setName("New NPC");
            Minecraft.getInstance().setScreen(new NPCEditScreen(-1, data));
        }).pos(tabX + 8, searchY).size(80, 18).build();
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

    private int getFilteredCount() {
        String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        int count = 0;
        for (NPCData data : npcCache) {
            if (data == null) continue;
            String id = data.getId() != null ? data.getId() : "";
            String name = data.getName() != null ? data.getName() : "";
            if (!filter.isEmpty() && !id.toLowerCase().contains(filter) && !name.toLowerCase().contains(filter)) continue;
            count++;
        }
        return count;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF16161E);

        if (waitingForServer && npcCache.isEmpty()) {
            g.drawCenteredString(this.font, "Loading...", x + w / 2, y + h / 2, 0xFFAAAAAA);
            return;
        }

        if (npcCache.isEmpty()) {
            g.drawCenteredString(this.font, "No NPCs found", x + w / 2, y + h / 2, 0xFFAAAAAA);
            return;
        }

        int listX = x + 8;
        int listY = y + TOP_PADDING + HEADER_HEIGHT;
        int listW = w - 16;
        int listH = h - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        int shown = 0;
        int visibleIndex = 0;

        for (int i = 0; i < npcCache.size(); i++) {
            NPCData data = npcCache.get(i);
            if (data == null) continue;

            String id = data.getId();
            if (id == null || id.isEmpty()) id = "unnamed";
            String name = data.getName();
            if (name == null || name.isEmpty()) name = "Unnamed";

            if (!filter.isEmpty() && !id.toLowerCase().contains(filter) && !name.toLowerCase().contains(filter)) {
                continue;
            }

            if (visibleIndex < scroll) {
                visibleIndex++;
                continue;
            }

            if (shown >= maxVisible) break;

            int rowY = listY + shown * ITEM_HEIGHT;
            boolean hovered = mx >= listX && mx <= listX + listW && my >= rowY && my <= rowY + ITEM_HEIGHT - 2;

            int bg = hovered ? 0xFF2A2A3A : 0x001E1E28;
            g.fill(listX, rowY, listX + listW, rowY + ITEM_HEIGHT - 2, bg);

            g.drawString(this.font, name + " [" + id + "]", listX + 4, rowY + 4, 0xFFCCCCCC);

            int btnY = rowY + 2;
            int btnH = ITEM_HEIGHT - 4;
            int spawnX = listX + listW - 42;
            int delX = listX + listW - 80;
            int editX = listX + listW - 118;

            boolean spawnHover = mx >= spawnX && mx <= spawnX + 38 && my >= btnY && my <= btnY + btnH;
            g.fill(spawnX, btnY, spawnX + 38, btnY + btnH, spawnHover ? 0xFF2A4A2A : 0xFF1E281E);
            g.renderOutline(spawnX, btnY, 38, btnH, 0xFF444455);
            g.drawCenteredString(this.font, "Spawn", spawnX + 19, btnY + (btnH - 8) / 2, spawnHover ? 0xFF55FF55 : 0xFF44AA44);

            boolean delHover = mx >= delX && mx <= delX + 36 && my >= btnY && my <= btnY + btnH;
            g.fill(delX, btnY, delX + 36, btnY + btnH, delHover ? 0xFF4A2A2A : 0xFF281E1E);
            g.renderOutline(delX, btnY, 36, btnH, 0xFF444455);
            g.drawCenteredString(this.font, "X", delX + 18, btnY + (btnH - 8) / 2, delHover ? 0xFFFF5555 : 0xFFAA4444);

            boolean editHover = mx >= editX && mx <= editX + 36 && my >= btnY && my <= btnY + btnH;
            g.fill(editX, btnY, editX + 36, btnY + btnH, editHover ? 0xFF2A3A4A : 0xFF1E1E28);
            g.renderOutline(editX, btnY, 36, btnH, 0xFF444455);
            g.drawCenteredString(this.font, "Edit", editX + 18, btnY + (btnH - 8) / 2, editHover ? 0xFFFFFFFF : 0xFFCCCCCC);

            shown++;
            visibleIndex++;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int tabX = DashboardScreen.SIDEBAR_WIDTH;
        int tabY = DashboardScreen.TOPBAR_HEIGHT;
        int tabW = this.parent.width - DashboardScreen.SIDEBAR_WIDTH;
        int tabH = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;

        int listX = tabX + 8;
        int listY = tabY + TOP_PADDING + HEADER_HEIGHT;
        int listW = tabW - 16;
        int listH = tabH - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);

        String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        int visibleIndex = 0;
        int shown = 0;

        for (int i = 0; i < npcCache.size(); i++) {
            NPCData data = npcCache.get(i);
            if (data == null) continue;

            String id = data.getId();
            if (id == null || id.isEmpty()) id = "unnamed";
            String name = data.getName();
            if (name == null || name.isEmpty()) name = "Unnamed";

            if (!filter.isEmpty() && !id.toLowerCase().contains(filter) && !name.toLowerCase().contains(filter)) {
                continue;
            }

            if (visibleIndex < scroll) {
                visibleIndex++;
                continue;
            }

            if (shown >= maxVisible) break;

            int rowY = listY + shown * ITEM_HEIGHT;
            int btnH = ITEM_HEIGHT - 4;
            int btnY = rowY + 2;
            int spawnX = listX + listW - 42;
            int delX = listX + listW - 80;
            int editX = listX + listW - 118;

            if (mx >= spawnX && mx <= spawnX + 38 && my >= btnY && my <= btnY + btnH) {
                IScriptNetwork.sendToServer(new com.iscript.iscript.network.packet.SpawnNPCPacket(data.getId()));
                return true;
            }
            if (mx >= delX && mx <= delX + 36 && my >= btnY && my <= btnY + btnH) {
                final int removeIndex = i;
                final String removeId = data.getId();
                IScriptNetwork.sendToServer(new DeleteNPCPacket(removeId));
                this.minecraft.execute(() -> {
                    if (removeIndex < npcCache.size()) {
                        npcCache.remove(removeIndex);
                        NPCManager.updateClientCache(new ArrayList<>(npcCache));
                    }
                });
                scroll = 0;
                return true;
            }
            if (mx >= editX && mx <= editX + 36 && my >= btnY && my <= btnY + btnH) {
                Minecraft.getInstance().setScreen(new NPCEditScreen(-1, data));
                return true;
            }

            shown++;
            visibleIndex++;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int total = getFilteredCount();
        int tabH = this.parent.height - DashboardScreen.TOPBAR_HEIGHT;
        int listH = tabH - TOP_PADDING - HEADER_HEIGHT - 8;
        int maxVisible = Math.max(1, listH / ITEM_HEIGHT);
        int maxScroll = Math.max(0, total - maxVisible);

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