package com.iscript.imson.script.api;

import org.graalvm.polyglot.HostAccess;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptHUDAPI {
    private static final Map<String, HUDElement> ELEMENTS = new ConcurrentHashMap<>();

    public static Map<String, HUDElement> getElements() {
        return ELEMENTS;
    }

    @HostAccess.Export
    public void add(String id, String type, int x, int y, int w, int h, String text) {
        HUDElement el = new HUDElement();
        el.id = id;
        el.type = type.toUpperCase();
        el.x = x;
        el.y = y;
        el.w = w;
        el.h = h;
        el.text = text;
        ELEMENTS.put(id, el);
    }

    @HostAccess.Export
    public void remove(String id) {
        ELEMENTS.remove(id);
    }

    @HostAccess.Export
    public void clear() {
        ELEMENTS.clear();
    }

    @HostAccess.Export
    public void setText(String id, String text) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) el.text = text;
    }

    @HostAccess.Export
    public void setProgress(String id, double value) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) el.progress = Math.max(0, Math.min(1, value));
    }

    @HostAccess.Export
    public void setColor(String id, double textColor, double bgColor, double borderColor) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) {
            el.textColor = (int) textColor;
            el.bgColor = (int) bgColor;
            el.borderColor = (int) borderColor;
        }
    }

    @HostAccess.Export
    public void setPos(String id, int x, int y, String xRel, String yRel) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) {
            el.x = x;
            el.y = y;
            el.xRel = xRel;
            el.yRel = yRel;
        }
    }

    @HostAccess.Export
    public void setExpire(String id, long millis) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) el.expireAt = System.currentTimeMillis() + millis;
    }

    @HostAccess.Export
    public void setIcon(String id, String texturePath) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) el.icon = texturePath;
    }

    @HostAccess.Export
    public void setStyle(String id, boolean centerText, boolean shadow, int padding) {
        HUDElement el = ELEMENTS.get(id);
        if (el != null) {
            el.centerText = centerText;
            el.shadow = shadow;
            el.padding = padding;
        }
    }

    @HostAccess.Export
    public boolean exists(String id) {
        return ELEMENTS.containsKey(id);
    }

    @HostAccess.Export
    public void text(String id, int x, int y, String text, double color) {
        add(id, "TEXT", x, y, 200, 12, text);
        setColor(id, color, 0, 0);
    }

    @HostAccess.Export
    public void progress(String id, int x, int y, int w, int h, double value, String label) {
        add(id, "PROGRESS", x, y, w, h, label);
        setProgress(id, value);
    }

    @HostAccess.Export
    public void box(String id, int x, int y, int w, int h, double bgColor, double borderColor) {
        add(id, "BOX", x, y, w, h, "");
        setColor(id, 0xFFFFFFFF, bgColor, borderColor);
    }

    public static class HUDElement {
        public String id = "";
        public String type = "TEXT";
        public int x, y, w, h;
        public String xRel = "left", yRel = "top";
        public String text = "";
        public String icon = "";
        public double progress = 0.0;
        public int textColor = 0xFFFFFFFF;
        public int bgColor = 0x88000000;
        public int borderColor = 0xFFAAAAAA;
        public int progressColor = 0xFF44AA44;
        public boolean centerText = false;
        public boolean shadow = true;
        public int padding = 4;
        public long expireAt = 0;
    }
}