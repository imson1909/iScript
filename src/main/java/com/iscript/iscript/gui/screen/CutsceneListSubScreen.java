package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.DeleteCutscenePacket;
import com.iscript.iscript.network.packet.PlayCutscenePacket;
import com.iscript.iscript.network.packet.SaveCutscenePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class CutsceneListSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 26;
    private String selectedCutsceneId = null;
    private EditBox searchBox;
    private Button createBtn, deleteBtn, editBtn, playBtn, speedHalfBtn, speedNormalBtn, speedDoubleBtn;
    private float playSpeed = 1.0f;

    public CutsceneListSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        this.clearWidgets();
        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - x;
        int h = this.parent.height - y;

        searchBox = new EditBox(this.font, x + 8, y + 8, 140, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0xFFAAAAAA);
        searchBox.setBordered(true);
        searchBox.setResponder(s -> scroll = 0);
        this.addRenderableWidget(searchBox);

        int btnY = y + 8;
        int btnX = x + w - 310;
        createBtn = Button.builder(Component.literal("+ New"), b -> createNew()).pos(btnX, btnY).size(50, 18).build();
        deleteBtn = Button.builder(Component.literal("Delete"), b -> deleteSelected()).pos(btnX + 54, btnY).size(50, 18).build();
        editBtn = Button.builder(Component.literal("Edit"), b -> editSelected()).pos(btnX + 108, btnY).size(50, 18).build();
        playBtn = Button.builder(Component.literal("Play"), b -> playSelected()).pos(x + w - 60, y + h - 28).size(50, 20).build();

        speedHalfBtn = Button.builder(Component.literal("0.5x"), b -> setSpeed(0.5f)).pos(x + w - 180, y + h - 28).size(40, 20).build();
        speedNormalBtn = Button.builder(Component.literal("1x"), b -> setSpeed(1.0f)).pos(x + w - 138, y + h - 28).size(40, 20).build();
        speedDoubleBtn = Button.builder(Component.literal("2x"), b -> setSpeed(2.0f)).pos(x + w - 96, y + h - 28).size(40, 20).build();

        this.addRenderableWidget(createBtn);
        this.addRenderableWidget(deleteBtn);
        this.addRenderableWidget(editBtn);
        this.addRenderableWidget(playBtn);
        this.addRenderableWidget(speedHalfBtn);
        this.addRenderableWidget(speedNormalBtn);
        this.addRenderableWidget(speedDoubleBtn);

        updateButtons();
    }

    private void setSpeed(float speed) {
        playSpeed = speed;
        speedHalfBtn.active = speed != 0.5f;
        speedNormalBtn.active = speed != 1.0f;
        speedDoubleBtn.active = speed != 2.0f;
    }

    private void updateButtons() {
        boolean has = selectedCutsceneId != null;
        deleteBtn.active = has;
        editBtn.active = has;
        playBtn.active = has;
    }

    private void createNew() {
        String id = "cutscene_" + System.currentTimeMillis();
        CutsceneData data = new CutsceneData();
        data.setId(id);
        data.setName("New Cutscene");
        IScriptNetwork.sendToServer(new SaveCutscenePacket(data));
        selectedCutsceneId = id;
        updateButtons();
    }

    private void deleteSelected() {
        if (selectedCutsceneId != null) {
            IScriptNetwork.sendToServer(new DeleteCutscenePacket(selectedCutsceneId));
            selectedCutsceneId = null;
            updateButtons();
        }
    }

    private void editSelected() {
        if (selectedCutsceneId == null) return;
        var cutscene = CutsceneManager.get(level, selectedCutsceneId);
        if (cutscene == null) return;
        parent.currentSubScreen = new CutsceneEditSubScreen(parent, level, cutscene);
        parent.currentSubScreen.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    private void playSelected() {
        if (selectedCutsceneId != null) {
            IScriptNetwork.sendToServer(new PlayCutscenePacket(selectedCutsceneId, playSpeed));
        }
    }

    private List<String> getFilteredIds() {
        var cutscenes = CutsceneManager.getAll(level);
        String filter = searchBox.getValue().toLowerCase();
        List<String> ids = new ArrayList<>();
        for (var e : cutscenes.entrySet()) {
            if (e.getKey().toLowerCase().contains(filter) || e.getValue().getName().toLowerCase().contains(filter)) {
                ids.add(e.getKey());
            }
        }
        return ids;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF16161E);

        List<String> ids = getFilteredIds();
        int listW = selectedCutsceneId != null ? w / 2 - 8 : w - 16;
        int listY = y + 32;
        int visibleCount = (h - 80) / ITEM_HEIGHT;

        for (int i = scroll; i < Math.min(scroll + visibleCount, ids.size()); i++) {
            String id = ids.get(i);
            var cutscene = CutsceneManager.getAll(level).get(id);
            int rowY = listY + (i - scroll) * ITEM_HEIGHT;
            boolean hovered = mouseX >= x + 8 && mouseX <= x + 8 + listW && mouseY >= rowY && mouseY <= rowY + 20;
            boolean selected = id.equals(selectedCutsceneId);

            int bg = selected ? 0xFF334455 : (hovered ? 0xFF2A2A3A : 0xFF1E1E28);
            graphics.fill(x + 8, rowY, x + 8 + listW, rowY + 20, bg);
            if (selected) {
                graphics.renderOutline(x + 8, rowY, listW, 20, 0xFF00D4AA);
            } else {
                graphics.renderOutline(x + 8, rowY, listW, 20, 0xFF333344);
            }

            String label = cutscene.getName() + " (" + cutscene.getActions().size() + " actions)";
            if (cutscene.isLoop()) label += " [loop]";
            graphics.drawString(this.font, label, x + 14, rowY + 6, selected ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        if (ids.isEmpty()) {
            graphics.drawCenteredString(this.font, "No cutscenes", x + (selectedCutsceneId != null ? listW / 2 + 4 : w / 2), y + h / 2, 0xFF555566);
        }

        if (selectedCutsceneId != null) {
            var cutscene = CutsceneManager.getAll(level).get(selectedCutsceneId);
            if (cutscene != null) {
                int dx = x + w / 2 + 4;
                int dw = w / 2 - 12;
                int dh = h - 16;
                graphics.fill(dx, y + 8, dx + dw, y + 8 + dh, 0xFF1E1E28);
                graphics.renderOutline(dx, y + 8, dw, dh, 0xFF333344);

                int dy = y + 16;
                graphics.drawString(this.font, "Cutscene Details", dx + 8, dy, 0xFF55AAFF); dy += 18;
                graphics.drawString(this.font, "ID: " + selectedCutsceneId, dx + 8, dy, 0xFFAAAAAA); dy += 14;
                graphics.drawString(this.font, "Name: " + cutscene.getName(), dx + 8, dy, 0xFFFFFFFF); dy += 14;
                graphics.drawString(this.font, "Loop: " + cutscene.isLoop(), dx + 8, dy, 0xFFCCCCCC); dy += 14;
                graphics.drawString(this.font, "Actions: " + cutscene.getActions().size(), dx + 8, dy, 0xFFCCCCCC); dy += 14;
            }
        }

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        for (var child : this.children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) parent.setFocusedWidget(w);
                return true;
            }
        }

        int x = DashboardScreen.SIDEBAR_WIDTH;
        int y = DashboardScreen.TOPBAR_HEIGHT;
        int w = this.parent.width - x;
        int h = this.parent.height - y;
        List<String> ids = getFilteredIds();
        int listW = selectedCutsceneId != null ? w / 2 - 8 : w - 16;

        for (int i = scroll; i < Math.min(scroll + (h - 80) / ITEM_HEIGHT, ids.size()); i++) {
            int rowY = y + 32 + (i - scroll) * ITEM_HEIGHT;
            if (mouseX >= x + 8 && mouseX <= x + 8 + listW && mouseY >= rowY && mouseY <= rowY + 20) {
                selectedCutsceneId = ids.get(i);
                updateButtons();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<String> ids = getFilteredIds();
        int visibleCount = (this.parent.height - DashboardScreen.TOPBAR_HEIGHT - 80) / ITEM_HEIGHT;
        int maxScroll = Math.max(0, ids.size() - visibleCount);
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (var child : this.children()) {
            if (child instanceof EditBox eb && eb.isFocused() && eb.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (var child : this.children()) {
            if (child instanceof EditBox eb && eb.isFocused() && eb.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }
}