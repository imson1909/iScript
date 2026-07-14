package com.iscript.iscript.gui.screen;

import com.iscript.iscript.data.WorldDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;

public class WorldDataSubScreen extends DashboardScreen.SubScreen {
    private int scroll = 0;
    private static final int ITEM_HEIGHT = 18;

    public WorldDataSubScreen(DashboardScreen parent, ServerLevel level) {
        super(parent, level);
    }

    @Override
    public void init() {
        this.clearWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF16161E);

        var keys = new ArrayList<>(WorldDataManager.getKeys(level));
        int listY = y + 8;

        for (int i = scroll; i < Math.min(scroll + (h - 30) / ITEM_HEIGHT, keys.size()); i++) {
            String key = keys.get(i);
            String type = WorldDataManager.getType(level, key);
            String value = switch (type) {
                case "string" -> WorldDataManager.getString(level, key);
                case "int" -> String.valueOf(WorldDataManager.getInt(level, key));
                case "double" -> String.valueOf(WorldDataManager.getDouble(level, key));
                case "bool" -> String.valueOf(WorldDataManager.getBool(level, key));
                default -> "?";
            };

            graphics.drawString(this.font, key, x + 14, listY, 0xFFAAAAAA);
            graphics.drawString(this.font, "= " + value, x + 200, listY, 0xFFCCCCCC);
            graphics.drawString(this.font, "(" + type + ")", x + w - 80, listY, 0xFF888888);
            listY += ITEM_HEIGHT;
        }

        if (keys.isEmpty()) {
            graphics.drawCenteredString(this.font, "No world data", x + w / 2, y + h / 2, 0xFF555566);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        var keys = WorldDataManager.getKeys(level);
        int maxScroll = Math.max(0, keys.size() - 15);
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        else scroll = Math.min(scroll + 1, maxScroll);
        return true;
    }
}