package com.iscript.imson.client.hud;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.script.api.ScriptHUDAPI;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScriptHUDOverlay {
    private static final Minecraft MC = Minecraft.getInstance();
    private static boolean enabled = true;

    public static void setEnabled(boolean v) { enabled = v; }
    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        if (!enabled || MC.player == null) return;
        GuiGraphics g = event.getGuiGraphics();
        int sw = event.getWindow().getGuiScaledWidth();
        int sh = event.getWindow().getGuiScaledHeight();

        long now = System.currentTimeMillis();
        for (ScriptHUDAPI.HUDElement el : ScriptHUDAPI.getElements().values()) {
            if (el.expireAt > 0 && now > el.expireAt) continue;
            renderElement(g, el, sw, sh);
        }
    }

    private static void renderElement(GuiGraphics g, ScriptHUDAPI.HUDElement el, int sw, int sh) {
        int x = resolvePos(el.x, el.xRel, sw, el.w);
        int y = resolvePos(el.y, el.yRel, sh, el.h);

        if (el.bgColor != 0) {
            g.fill(x, y, x + el.w, y + el.h, el.bgColor);
        }
        if (el.borderColor != 0) {
            g.renderOutline(x, y, el.w, el.h, el.borderColor);
        }

        if ("PROGRESS".equals(el.type)) {
            int fillW = (int) (el.w * el.progress);
            g.fill(x, y, x + fillW, y + el.h, el.progressColor);
            if (el.borderColor != 0) g.renderOutline(x, y, el.w, el.h, el.borderColor);
        }

        if ("ICON".equals(el.type) && el.icon != null && !el.icon.isEmpty()) {
            try {
                ResourceLocation rl = new ResourceLocation(el.icon);
                RenderSystem.setShaderTexture(0, rl);
                g.blit(rl, x, y, 0, 0, el.w, el.h, el.w, el.h);
            } catch (Exception e) {}
        }

        if (el.text != null && !el.text.isEmpty() && MC.font != null) {
            int tx = x + el.padding;
            int ty = y + el.padding;
            if ("PROGRESS".equals(el.type)) {
                ty = y + (el.h - 8) / 2;
                if (el.centerText) tx = x + el.w / 2;
            }
            if (el.shadow) {
                if (el.centerText) g.drawCenteredString(MC.font, el.text, tx, ty, el.textColor);
                else g.drawString(MC.font, el.text, tx, ty, el.textColor, true);
            } else {
                if (el.centerText) g.drawCenteredString(MC.font, el.text, tx, ty, el.textColor);
                else g.drawString(MC.font, el.text, tx, ty, el.textColor);
            }
        }
    }

    private static int resolvePos(int val, String rel, int screenSize, int elementSize) {
        if ("center".equals(rel)) return (screenSize - elementSize) / 2 + val;
        if ("right".equals(rel)) return screenSize - elementSize - val;
        if ("bottom".equals(rel)) return screenSize - elementSize - val;
        return val;
    }
}