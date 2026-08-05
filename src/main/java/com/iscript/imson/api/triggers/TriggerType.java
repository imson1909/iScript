package com.iscript.imson.api.triggers;

public enum TriggerType {
    LIVING_EQUIPMENT_CHANGE("iscript.trigger.living.equipment_change"),
    LIVING_WALK("iscript.trigger.living.walk"),
    LIVING_JUMP("iscript.trigger.living.jump"),
    LIVING_FALL("iscript.trigger.living.fall"),
    LIVING_DEATH("iscript.trigger.living.death"),
    LIVING_HURT("iscript.trigger.living.hurt"),
    LIVING_MORPH_CHANGE("iscript.trigger.living.morph_change"),
    LIVING_HEAL("iscript.trigger.living.heal"),

    PLAYER_EQUIPMENT_CHANGE("iscript.trigger.player.equipment_change"),
    PLAYER_TICK("iscript.trigger.player.tick"),
    PLAYER_KEYBOARD("iscript.trigger.player.keyboard"),
    PLAYER_MOUSE("iscript.trigger.player.mouse"),
    PLAYER_CHANGED_DIMENSION("iscript.trigger.player.changed_dimension"),
    PLAYER_LEFT_CLICK_AIR("iscript.trigger.player.left_click_air"),
    PLAYER_LEFT_CLICK_BLOCK("iscript.trigger.player.left_click_block"),
    PLAYER_RIGHT_CLICK_AIR("iscript.trigger.player.right_click_air"),
    PLAYER_RIGHT_CLICK_BLOCK("iscript.trigger.player.right_click_block"),
    PLAYER_LOGGED_IN("iscript.trigger.player.logged_in"),
    PLAYER_LOGGED_OUT("iscript.trigger.player.logged_out"),
    PLAYER_RESPAWN("iscript.trigger.player.respawn"),
    PLAYER_DROP_ITEM("iscript.trigger.player.drop_item"),
    PLAYER_OPEN_GUI("iscript.trigger.player.open_gui"),
    PLAYER_CLOSE_GUI("iscript.trigger.player.close_gui"),
    PLAYER_OPEN_CONTAINER("iscript.trigger.player.open_container"),
    PLAYER_PICKUP_ITEM("iscript.trigger.player.pickup_item"),
    PLAYER_DRINK_POTION("iscript.trigger.player.drink_potion"),
    PLAYER_EAT("iscript.trigger.player.eat"),
    PLAYER_DEATH("iscript.trigger.player.death"),
    PLAYER_CHAT("iscript.trigger.player.chat"),
    PLAYER_TRADE("iscript.trigger.player.trade"),
    PLAYER_BREAK_BLOCK("iscript.trigger.player.break_block"),
    PLAYER_PLACE_BLOCK("iscript.trigger.player.place_block"),
    PLAYER_SNEAK("iscript.trigger.player.sneak"),

    SERVER_TICK("iscript.trigger.server.tick"),
    SERVER_LOAD("iscript.trigger.server.load");

    private final String langKey;

    TriggerType(String langKey) {
        this.langKey = langKey;
    }

    public String getLangKey() {
        return langKey;
    }

    public String getDisplayName() {
        return com.iscript.imson.gui.screen.I18n.s(langKey);
    }
}