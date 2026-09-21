package com.iscript.imson.morph.gui;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SkinPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> callback;
    private final Path morphsRoot;
    private Path currentDir;
    private final List<FileEntry> entries = new ArrayList<>();
    private int scroll = 0;
    private int maxScroll = 0;
    private String selectedPath = "";

    private static class FileEntry {
        final String name;
        final Path path;
        final boolean isDir;
        FileEntry(String n, Path p, boolean d) { name = n; path = p; isDir = d; }
    }

    public SkinPickerScreen(Screen parent, Consumer<String> callback) {
        super(Component.literal("Skin Picker"));
        this.parent = parent;
        this.callback = callback;
        this.morphsRoot = FMLPaths.GAMEDIR.get().resolve("saves").resolve(getCurrentWorldName()).resolve("iscript").resolve("morphs");
        this.currentDir = morphsRoot;
    }

    private String getCurrentWorldName() {
        try {
            if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.getServer() != null) {
                return this.minecraft.level.getServer().getWorldData().getLevelName();
            }
        } catch (Exception ignored) {}
        return "default";
    }

    @Override
    protected void init() {
        super.init();
        reloadEntries();
    }

    private void reloadEntries() {
        entries.clear();
        if (!Files.exists(currentDir)) {
            try { Files.createDirectories(currentDir); } catch (IOException ignored) {}
        }
        try (Stream<Path> stream = Files.list(currentDir)) {
            stream.sorted().forEach(p -> {
                String name = p.getFileName().toString();
                boolean isDir = Files.isDirectory(p);
                if (isDir || name.toLowerCase().endsWith(".png")) {
                    entries.add(new FileEntry(name, p, isDir));
                }
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list skins", e);
        }
        scroll = 0;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        int itemH = 22;
        int totalH = entries.size() * itemH;
        int visibleH = this.height - 80;
        maxScroll = Math.max(0, totalH - visibleH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(0, 0, this.width, 40, 0xEE16161E);
        g.fill(0, this.height - 40, this.width, this.height, 0xEE16161E);
        g.drawString(this.font, "Выбор скина: " + currentDir.getFileName(), 10, 15, Theme.ACCENT);
        String rel = morphsRoot.relativize(currentDir).toString();
        g.drawString(this.font, "Путь: " + (rel.isEmpty() ? "/" : rel), 10, 27, Theme.TEXT_DIM);

        int listX = 20;
        int listY = 50;
        int listW = this.width - 40;
        int listH = this.height - 100;
        UI.panel(g, listX, listY, listW, listH);

        int itemH = 22;
        int currentY = listY + 4 - scroll;
        for (FileEntry e : entries) {
            if (currentY + itemH < listY || currentY > listY + listH) {
                currentY += itemH;
                continue;
            }
            boolean hover = mx >= listX + 4 && mx <= listX + listW - 4 && my >= currentY && my <= currentY + itemH;
            boolean selected = e.path.toString().equals(selectedPath);
            int bg = selected ? Theme.ACCENT : (hover ? Theme.BG_HOVER : 0x00000000);
            g.fill(listX + 4, currentY, listX + listW - 4, currentY + itemH, bg);
            String prefix = e.isDir ? "[DIR] " : "[PNG] ";
            g.drawString(this.font, prefix + e.name, listX + 10, currentY + 7, Theme.TEXT);
            currentY += itemH;
        }

        if (maxScroll > 0) {
            int thumbH = Math.max(20, listH * listH / (listH + maxScroll));
            int thumbY = listY + (int) ((float) scroll / maxScroll * (listH - thumbH));
            g.fill(listX + listW - 6, listY, listX + listW - 2, listY + listH, Theme.alpha(Theme.BORDER, 0.2f));
            g.fill(listX + listW - 6, thumbY, listX + listW - 2, thumbY + thumbH, Theme.TEXT_DIM);
        }

        int btnW = 100, btnH = 24;
        int btnY = this.height - 32;
        drawBtn(g, mx, my, 20, btnY, btnW, btnH, "Назад");
        if (!selectedPath.isEmpty()) {
            drawBtn(g, mx, my, this.width - 120, btnY, btnW, btnH, "Выбрать");
        }
        super.render(g, mx, my, pt);
    }

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String text) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hover ? Theme.BG_HOVER : 0xFF1E1E26);
        g.renderOutline(x, y, w, h, Theme.BORDER);
        g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int btnW = 100, btnH = 24;
        int btnY = this.height - 32;
        if (mx >= 20 && mx <= 120 && my >= btnY && my <= btnY + btnH) {
            if (!currentDir.equals(morphsRoot)) {
                currentDir = currentDir.getParent();
                selectedPath = "";
                reloadEntries();
            } else {
                this.minecraft.setScreen(parent);
            }
            return true;
        }
        if (!selectedPath.isEmpty() && mx >= this.width - 120 && mx <= this.width - 20 && my >= btnY && my <= btnY + btnH) {
            Path sel = Path.of(selectedPath);
            String rel = morphsRoot.relativize(sel).toString().replace('\\', '/');
            callback.accept(rel);
            this.minecraft.setScreen(parent);
            return true;
        }
        int listX = 20, listY = 50, listW = this.width - 40, listH = this.height - 100;
        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            int itemH = 22;
            int currentY = listY + 4 - scroll;
            for (FileEntry e : entries) {
                if (my >= currentY && my <= currentY + itemH) {
                    if (e.isDir) {
                        currentDir = e.path;
                        selectedPath = "";
                        reloadEntries();
                    } else {
                        selectedPath = e.path.toString();
                    }
                    return true;
                }
                currentY += itemH;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0) scroll = Math.max(0, scroll - 22);
        else scroll = Math.min(maxScroll, scroll + 22);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}