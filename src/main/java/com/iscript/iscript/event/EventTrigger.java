package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class EventTrigger {
    private EventType type;
    private String condition = "";
    private String script = "";
    private boolean enabled = true;

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void execute(Player player, ServerLevel level) {
        if (!enabled || script.isEmpty()) return;
        ScriptEngine engine = ScriptEngine.getInstance();
        if (engine.isAvailable()) {
            try {
                engine.execute(script, player, level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Event trigger failed: {}", e.getMessage());
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putString("Condition", condition);
        tag.putString("Script", script);
        tag.putBoolean("Enabled", enabled);
    }

    public void load(CompoundTag tag) {
        try {
            type = EventType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = EventType.TICK;
        }
        condition = tag.getString("Condition");
        script = tag.getString("Script");
        enabled = tag.getBoolean("Enabled");
    }
}