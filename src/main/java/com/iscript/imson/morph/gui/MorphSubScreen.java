package com.iscript.imson.morph.gui;

import com.iscript.imson.morph.MorphData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public abstract class MorphSubScreen {
    protected final MorphScreen parent;
    protected final Player player;
    protected final MorphData morphData;
    protected final Minecraft mc;
    protected final Font font;

    public MorphSubScreen(MorphScreen parent, Player player, MorphData morphData) {
        this.parent = parent;
        this.player = player;
        this.morphData = morphData;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public abstract void init();
    public abstract void render(GuiGraphics g, int mx, int my, float pt);
    public abstract boolean mouseClicked(double mx, double my, int btn);
    public abstract boolean mouseReleased(double mx, double my, int btn);
    public abstract boolean mouseDragged(double mx, double my, int btn, double dx, double dy);
    public abstract boolean mouseScrolled(double mx, double my, double delta);
    public abstract void onClose();
}