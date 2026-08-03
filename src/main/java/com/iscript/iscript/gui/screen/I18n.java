package com.iscript.iscript.gui.screen;

import net.minecraft.network.chat.Component;

public class I18n {
    public static Component t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String s(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}