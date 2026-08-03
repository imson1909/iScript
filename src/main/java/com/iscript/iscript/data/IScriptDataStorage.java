package com.iscript.iscript.data;

import com.iscript.iscript.script.ScriptFileManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Path;

public class IScriptDataStorage {
    private static IScriptDataStorage instance;
    private final Path basePath;
    private final ServerLevel level;

    private IScriptDataStorage(ServerLevel level) {
        this.level = level;
        this.basePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve("iscript");
    }

    public static void init(ServerLevel level) {
        instance = new IScriptDataStorage(level);
        ScriptFileManager.setBasePath(instance.basePath);
    }

    public static IScriptDataStorage get() {
        return instance;
    }

    public void loadAll() {
    }

    public void saveAll() {
    }
}