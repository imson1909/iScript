package com.iscript.iscript.gui.theme;

public final class Theme {
    private Theme() {}

    public static final int ACCENT      = 0xFF00D4AA;
    public static final int TEXT        = 0xFFE0E0E0;
    public static final int TEXT_DIM    = 0xFF888899;
    public static final int TEXT_MUTE   = 0xFF555566;
    public static final int BG_PANEL    = 0xCC1A1A24;
    public static final int BG_INNER   = 0xBB13131A;
    public static final int BG_HOVER    = 0xFF252530;
    public static final int BORDER      = 0x55333344;
    public static final int BORDER_ACCENT = 0xFF00D4AA;
    public static final int ERROR       = 0xFFFF4444;

    public static int alpha(int c, float a) {
        return ((int)(Math.min(255, a * 255)) << 24) | (c & 0xFFFFFF);
    }
}