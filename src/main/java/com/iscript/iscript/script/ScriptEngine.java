package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.config.IScriptConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;

public class ScriptEngine {
    private static ScriptEngine instance;
    private final javax.script.ScriptEngine engine;
    private final ScriptContext context;
    private final Map<String, Object> globals = new HashMap<>();

    private ScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("nashorn");
        if (this.engine == null) {
            IScriptMod.LOGGER.warn("Nashorn not available. Scripting disabled.");
        }
        this.context = new SimpleScriptContext();
    }

    public static ScriptEngine getInstance() {
        if (instance == null) {
            instance = new ScriptEngine();
        }
        return instance;
    }

    public void setGlobal(String name, Object value) {
        globals.put(name, value);
        if (engine != null) {
            engine.put(name, value);
        }
    }

    public Object execute(String script, Player player, ServerLevel level) {
        if (!IScriptConfig.ENABLE_SCRIPTING.get() || engine == null) return null;
        try {
            engine.put("player", player);
            engine.put("level", level);
            engine.put("server", level.getServer());
            for (Map.Entry<String, Object> entry : globals.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
            return engine.eval(script, context);
        } catch (ScriptException e) {
            IScriptMod.LOGGER.error("Script execution failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return engine != null && IScriptConfig.ENABLE_SCRIPTING.get();
    }
}
