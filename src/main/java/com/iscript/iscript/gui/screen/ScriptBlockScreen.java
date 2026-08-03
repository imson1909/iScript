package com.iscript.iscript.gui.screen;

import com.iscript.iscript.gui.theme.Theme;
import com.iscript.iscript.gui.widget.MultiLineEditBox;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import com.iscript.iscript.gui.widget.StyledButton;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptBlockScreen extends Screen {
    private final BlockPos pos;
    private final String initialScriptId;
    private final String initialScript;

    private MultiLineEditBox scriptBox;
    private List<String> scriptIds = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean dropdownOpen = false;
    private int dropdownScroll = 0;
    private static final int DROPDOWN_H = 20;
    private static final int MAX_VISIBLE = 6;

    private int saveDebounce = 0;
    private String saveStatus = "";
    private int saveStatusTimer = 0;
    private boolean loadingContent = false;
    private int lastCacheSize = -1;

    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;

    public ScriptBlockScreen(BlockPos pos, String scriptId, String script) {
        super(I18n.t("iscript.script.block.title"));
        this.pos = pos;
        this.initialScriptId = scriptId;
        this.initialScript = script;
    }

    @Override
    protected void init() {
        boxX = 20;
        boxY = 70;
        boxW = this.width - 40;
        boxH = this.height - boxY - 48;

        refreshScriptIds();
        if (scriptIds.isEmpty()) {
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_GRAPHS, new CompoundTag()));
        }

        this.scriptBox = new MultiLineEditBox(this.font, boxX, boxY, boxW, boxH, I18n.t("iscript.script.block.placeholder.script"), I18n.t("iscript.script.block.placeholder.script_hint"));
        this.scriptBox.setValue(this.initialScript);
        this.scriptBox.setOnValueChanged(() -> {
            if (!loadingContent) {
                saveDebounce = 40;
                saveStatus = "";
            }
        });
        this.addRenderableWidget(this.scriptBox);

        int cx = this.width / 2;
        this.addRenderableWidget(new StyledButton(this.font, cx + 60, 40, 40, 20, I18n.t("iscript.script.block.button.new"), () -> createNew()).setAccent(true));
        this.addRenderableWidget(new StyledButton(this.font, cx - 45, this.height - 28, 90, 20, I18n.t("iscript.script.block.button.close"), () -> this.onClose()));

        if (!this.initialScriptId.isEmpty()) {
            selectedIndex = scriptIds.indexOf(this.initialScriptId);
            if (selectedIndex < 0) {
                scriptIds.add(this.initialScriptId);
                Collections.sort(scriptIds);
                selectedIndex = scriptIds.indexOf(this.initialScriptId);
            }
        }
    }

    private void refreshScriptIds() {
        scriptIds = new ArrayList<>(ScriptGraphManager.getClientCache().keySet());
        Collections.sort(scriptIds);
    }

    private String getSelectedId() {
        if (selectedIndex >= 0 && selectedIndex < scriptIds.size()) {
            return scriptIds.get(selectedIndex);
        }
        return "";
    }

    private void selectScript(int index) {
        if (index < 0 || index >= scriptIds.size()) return;
        selectedIndex = index;
        dropdownOpen = false;
        String id = scriptIds.get(index);
        String text = ScriptGraphManager.getClientJsCache(id);
        loadingContent = true;
        scriptBox.setValue("");
        if (text != null) {
            scriptBox.setValue(text);
        } else {
            scriptBox.setValue("");
            IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.REQUEST_SCRIPT_CONTENT, ServerCommandPacket.requestScriptToTag(id)));
        }
        loadingContent = false;
    }

    private void createNew() {
        String id = "script_" + System.currentTimeMillis();
        refreshScriptIds();
        scriptIds.add(id);
        Collections.sort(scriptIds);
        selectedIndex = scriptIds.indexOf(id);
        loadingContent = true;
        scriptBox.setValue("");
        scriptBox.setValue(I18n.s("iscript.script.block.new_script"));
        loadingContent = false;
        saveDebounce = 40;
    }

    private void sendSave() {
        String id = getSelectedId();
        if (id.isEmpty() || scriptBox == null) return;
        String text = scriptBox.getValue();
        if (text == null) text = "";
        saveStatus = I18n.s("iscript.script.status.saving");
        saveStatusTimer = 60;
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_SCRIPT_TEXT, ServerCommandPacket.saveScriptTextToTag(id, text)));
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SCRIPT_BLOCK_SAVE, ServerCommandPacket.scriptBlockToTag(pos, id, text)));
    }

    @Override
    public void tick() {
        var cache = ScriptGraphManager.getClientCache();
        if (cache.size() != lastCacheSize) {
            lastCacheSize = cache.size();
            refreshScriptIds();
            if (!initialScriptId.isEmpty() && selectedIndex < 0) {
                selectedIndex = scriptIds.indexOf(initialScriptId);
            }
        }
        if (saveDebounce > 0) {
            saveDebounce--;
            if (saveDebounce == 0) sendSave();
        }
        if (saveStatusTimer > 0) {
            saveStatusTimer--;
            if (saveStatusTimer == 0 && saveStatus.equals(I18n.s("iscript.script.status.saving"))) {
                saveStatus = I18n.s("iscript.script.status.saved");
                saveStatusTimer = 40;
            } else if (saveStatusTimer == 0) {
                saveStatus = "";
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.TEXT);

        int cx = this.width / 2;
        int dx = cx - 150;
        int dy = 40;
        int dw = 200;

        graphics.drawString(this.font, I18n.s("iscript.script.block.label.script_id"), dx, 28, Theme.TEXT_DIM);

        boolean headerHovered = mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + DROPDOWN_H;
        graphics.fill(dx, dy, dx + dw, dy + DROPDOWN_H, headerHovered ? Theme.BG_HOVER : Theme.BG_INNER);
        graphics.renderOutline(dx, dy, dw, DROPDOWN_H, Theme.BORDER);

        String selectedText = selectedIndex >= 0 ? scriptIds.get(selectedIndex) : I18n.s("iscript.script.block.select");
        graphics.drawString(this.font, selectedText, dx + 4, dy + 6, Theme.TEXT);
        graphics.drawString(this.font, dropdownOpen ? "\u25B2" : "\u25BC", dx + dw - 12, dy + 6, Theme.TEXT_DIM);

        if (dropdownOpen) {
            int listY = dy + DROPDOWN_H;
            int visibleCount = Math.min(scriptIds.size(), MAX_VISIBLE);
            int listH = visibleCount * DROPDOWN_H;
            graphics.fill(dx, listY, dx + dw, listY + listH, Theme.BG_INNER);
            graphics.renderOutline(dx, listY, dw, listH, Theme.BORDER);

            for (int i = 0; i < visibleCount; i++) {
                int idx = i + dropdownScroll;
                if (idx >= scriptIds.size()) break;
                int rowY = listY + i * DROPDOWN_H;
                boolean hovered = mouseX >= dx && mouseX <= dx + dw && mouseY >= rowY && mouseY <= rowY + DROPDOWN_H;
                boolean selected = idx == selectedIndex;
                int bg = selected ? 0xFF334455 : (hovered ? Theme.BG_HOVER : Theme.BG_INNER);
                graphics.fill(dx + 1, rowY, dx + dw - 1, rowY + DROPDOWN_H, bg);
                graphics.drawString(this.font, scriptIds.get(idx), dx + 4, rowY + 6, selected ? Theme.ACCENT : Theme.TEXT);
            }
        }

        graphics.drawString(this.font, I18n.s("iscript.script.block.label.script"), boxX, boxY - 12, Theme.TEXT_DIM);

        if (!saveStatus.isEmpty()) {
            int statusColor = saveStatus.contains(I18n.s("iscript.script.status.saved")) ? 0xFF44AA44 : Theme.ACCENT;
            graphics.drawString(this.font, saveStatus, boxX, this.height - 32, statusColor);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int cx = this.width / 2;
        int dx = cx - 150;
        int dy = 40;
        int dw = 200;

        if (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + DROPDOWN_H) {
            dropdownOpen = !dropdownOpen;
            return true;
        }

        if (dropdownOpen) {
            int listY = dy + DROPDOWN_H;
            int visibleCount = Math.min(scriptIds.size(), MAX_VISIBLE);
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + dropdownScroll;
                if (idx >= scriptIds.size()) break;
                int rowY = listY + i * DROPDOWN_H;
                if (mouseX >= dx && mouseX <= dx + dw && mouseY >= rowY && mouseY <= rowY + DROPDOWN_H) {
                    selectScript(idx);
                    return true;
                }
            }
            dropdownOpen = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (dropdownOpen) {
            int cx = this.width / 2;
            int dx = cx - 150;
            int dy = 40;
            int dw = 200;
            int listY = dy + DROPDOWN_H;
            int listH = Math.min(scriptIds.size(), MAX_VISIBLE) * DROPDOWN_H;
            if (mouseX >= dx && mouseX <= dx + dw && mouseY >= listY && mouseY <= listY + listH) {
                int maxScroll = Math.max(0, scriptIds.size() - MAX_VISIBLE);
                if (delta > 0) dropdownScroll = Math.max(0, dropdownScroll - 1);
                else dropdownScroll = Math.min(dropdownScroll + 1, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}