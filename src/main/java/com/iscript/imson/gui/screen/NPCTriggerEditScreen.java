package com.iscript.imson.gui.screen;

import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.data.npc.NPCTriggerData;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ServerCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class NPCTriggerEditScreen extends Screen {
    private final int entityId;
    private final NPCData parentData;
    private final List<TriggerRow> rows = new ArrayList<>();
    private int scroll = 0;
    private static final int ROWS_TOP = 40;
    private static final int ROW_HEIGHT = 52;

    public NPCTriggerEditScreen(int entityId, NPCData parentData) {
        super(I18n.t("iscript.trigger.edit.title"));
        this.entityId = entityId;
        this.parentData = parentData;
    }

    @Override
    protected void init() {
        rows.clear();
        for (NPCTriggerData trigger : parentData.getTriggers()) {
            rows.add(new TriggerRow(trigger));
        }
        this.addRenderableWidget(Button.builder(I18n.t("iscript.trigger.edit.add"), btn -> {
            rows.add(new TriggerRow());
            rebuild();
        }).pos(this.width / 2 - 100, this.height - 58).size(90, 20).build());
        this.addRenderableWidget(Button.builder(I18n.t("iscript.trigger.edit.save"), btn -> save())
                .pos(this.width / 2 - 5, this.height - 58).size(90, 20).build());
        this.addRenderableWidget(Button.builder(I18n.t("iscript.trigger.edit.close"), btn -> this.onClose())
                .pos(this.width / 2 + 90, this.height - 58).size(90, 20).build());
        rebuild();
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 108) / ROW_HEIGHT);
    }

    private void rebuild() {
        for (TriggerRow row : rows) {
            row.remove();
        }
        int maxScroll = Math.max(0, rows.size() - visibleRows());
        if (scroll > maxScroll) scroll = maxScroll;
        int visible = Math.min(rows.size() - scroll, visibleRows());
        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            rows.get(idx).build(this, this.width / 2 - 220, ROWS_TOP + i * ROW_HEIGHT, idx);
        }
    }

    private void save() {
        parentData.getTriggers().clear();
        for (TriggerRow row : rows) {
            NPCTriggerData trigger = row.toTrigger();
            if (trigger != null) {
                parentData.getTriggers().add(trigger);
            }
        }
        IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.SAVE_NPC_DATA, ServerCommandPacket.saveNPCToTag(entityId, parentData)));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, this.width, this.height, Theme.alpha(Theme.BG_PANEL, 0.07f), Theme.alpha(Theme.BG_PANEL, 0.2f));
        int cx = this.width / 2;
        UI.panel(g, cx - 240, 16, 480, this.height - 80);
        g.drawCenteredString(this.font, this.title, cx, 20, Theme.ACCENT);
        for (int i = scroll; i < Math.min(rows.size(), scroll + visibleRows()); i++) {
            int ry = ROWS_TOP + (i - scroll) * ROW_HEIGHT;
            g.renderOutline(cx - 220, ry, 440, 48, Theme.BORDER);
            g.drawString(this.font, "#" + (i + 1), cx - 215, ry + 4, Theme.TEXT_DIM, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, Math.max(0, rows.size() - visibleRows()));
        rebuild();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class TriggerRow {
        private CycleButton<NPCTriggerData.TriggerType> typeBtn;
        private CycleButton<NPCTriggerData.ActionType> actionBtn;
        private EditBox valueBox;
        private Button deleteBtn;
        private Button enabledBtn;
        private NPCTriggerEditScreen parent;
        private NPCTriggerData data;
        private boolean enabled = true;

        TriggerRow() {
            this.data = new NPCTriggerData();
            this.enabled = true;
        }

        TriggerRow(NPCTriggerData data) {
            this.data = data;
            this.enabled = data.isEnabled();
        }

        void build(NPCTriggerEditScreen parent, int x, int y, int idx) {
            this.parent = parent;
            int w = 90;
            int h = 16;
            typeBtn = CycleButton.<NPCTriggerData.TriggerType>builder(t -> Component.literal(t.name()))
                    .withValues(NPCTriggerData.TriggerType.values())
                    .withInitialValue(data.getTriggerType())
                    .create(x + 20, y + 4, w, h, Component.empty(), (btn, val) -> data.setTriggerType(val));
            parent.addRenderableWidget(typeBtn);
            actionBtn = CycleButton.<NPCTriggerData.ActionType>builder(t -> Component.literal(t.name()))
                    .withValues(NPCTriggerData.ActionType.values())
                    .withInitialValue(data.getActionType())
                    .create(x + 20 + w + 4, y + 4, w, h, Component.empty(), (btn, val) -> data.setActionType(val));
            parent.addRenderableWidget(actionBtn);
            valueBox = new EditBox(parent.getMinecraft().font, x + 20 + w * 2 + 8, y + 4, 140, h, Component.empty());
            valueBox.setMaxLength(256);
            valueBox.setValue(data.getActionValue());
            valueBox.setResponder(data::setActionValue);
            parent.addRenderableWidget(valueBox);
            enabledBtn = Button.builder(Component.literal(enabled ? I18n.s("iscript.trigger.state.on") : I18n.s("iscript.trigger.state.off")), btn -> {
                enabled = !enabled;
                data.setEnabled(enabled);
                btn.setMessage(Component.literal(enabled ? I18n.s("iscript.trigger.state.on") : I18n.s("iscript.trigger.state.off")));
            }).pos(x + 20 + w * 2 + 152, y + 4).size(36, h).build();
            parent.addRenderableWidget(enabledBtn);
            deleteBtn = Button.builder(Component.literal(I18n.s("iscript.trigger.edit.delete")), btn -> {
                parent.rows.remove(this);
                parent.rebuild();
            }).pos(x + 380, y + 2).size(20, 20).build();
            parent.addRenderableWidget(deleteBtn);
        }

        void remove() {
            if (parent == null) return;
            if (typeBtn != null) parent.removeWidget(typeBtn);
            if (actionBtn != null) parent.removeWidget(actionBtn);
            if (valueBox != null) parent.removeWidget(valueBox);
            if (deleteBtn != null) parent.removeWidget(deleteBtn);
            if (enabledBtn != null) parent.removeWidget(enabledBtn);
        }

        NPCTriggerData toTrigger() {
            return data;
        }
    }
}