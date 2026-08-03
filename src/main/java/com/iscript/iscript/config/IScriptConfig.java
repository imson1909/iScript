package com.iscript.iscript.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class IScriptConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_SCRIPTING;

    static {
        BUILDER.push("General");
        ENABLE_SCRIPTING = BUILDER.comment("Enable scripting engine").define("enableScripting", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
