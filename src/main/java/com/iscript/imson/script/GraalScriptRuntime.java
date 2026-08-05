package com.iscript.imson.script;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.config.IScriptConfig;
import com.iscript.imson.script.api.JSMath;
import com.iscript.imson.script.api.JavaTypeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import java.util.HashMap;
import java.util.Map;

public class GraalScriptRuntime {
    private Context context;
    private final Map<String, Object> globals = new HashMap<>();

    public GraalScriptRuntime() {
        createContext();
        initDefaultGlobals();
    }

    private void createContext() {
        try {
            HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL)
                    .allowAccessAnnotatedBy(HostAccess.Export.class)
                    .allowArrayAccess(true)
                    .allowListAccess(true)
                    .allowMapAccess(true)
                    .allowBufferAccess(true)
                    .build();

            this.context = Context.newBuilder("js")
                    .allowHostAccess(hostAccess)
                    .allowHostClassLookup(GraalScriptRuntime::isHostClassAllowed)
                    .allowCreateThread(false)
                    .allowIO(false)
                    .allowNativeAccess(false)
                    .allowExperimentalOptions(false)
                    .build();

            IScriptMod.LOGGER.info("GraalJS initialized successfully");
        } catch (Exception e) {
            IScriptMod.LOGGER.error("GraalJS initialization failed: {}", e.getMessage());
            this.context = null;
        }
    }

    private static boolean isHostClassAllowed(String className) {
        if (className.startsWith("net.minecraft.")) return true;
        if (className.startsWith("com.iscript.iscript.")) return true;
        if (className.startsWith("java.lang.")) {
            return !(className.contains("Runtime")
                    || className.contains("Process")
                    || className.contains("reflect.")
                    || className.contains("ClassLoader")
                    || className.contains("Thread")
                    || className.contains("System")
                    || className.contains("Security")
                    || className.contains("invoke."));
        }
        if (className.startsWith("java.util.")) {
            return !(className.contains("jar.")
                    || className.contains("zip.")
                    || className.contains("ServiceLoader"));
        }
        return false;
    }

    public void initDefaultGlobals() {
        setGlobal("Component", Component.class);
        setGlobal("ItemStack", ItemStack.class);
        setGlobal("Items", Items.class);
        setGlobal("Block", Block.class);
        setGlobal("Blocks", net.minecraft.world.level.block.Blocks.class);
        setGlobal("Vec3", Vec3.class);
        setGlobal("ResourceLocation", ResourceLocation.class);
        setGlobal("ChatFormatting", ChatFormatting.class);
        setGlobal("EntityType", EntityType.class);
        setGlobal("GameType", GameType.class);
        setGlobal("BlockPos", BlockPos.class);
        setGlobal("SoundSource", SoundSource.class);
        setGlobal("ForgeRegistries", ForgeRegistries.class);
        setGlobal("Math", new JSMath());
        setGlobal("JavaType", new JavaTypeHelper());
    }

    public void setGlobal(String name, Object value) {
        globals.put(name, value);
        if (context != null) {
            context.getBindings("js").putMember(name, value);
        }
    }

    public Context getContext() {
        return context;
    }

    public Map<String, Object> getGlobals() {
        return globals;
    }

    public boolean isAvailable() {
        return context != null && IScriptConfig.ENABLE_SCRIPTING.get();
    }

    public void reload() {
        IScriptMod.LOGGER.info("Reloading GraalJS context...");
        synchronized (this) {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    IScriptMod.LOGGER.warn("Error closing old GraalJS context: {}", e.getMessage());
                }
            }
            globals.clear();
            createContext();
            initDefaultGlobals();
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    IScriptMod.LOGGER.warn("Error closing GraalJS context: {}", e.getMessage());
                }
                context = null;
            }
        }
    }
}