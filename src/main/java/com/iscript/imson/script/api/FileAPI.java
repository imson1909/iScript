package com.iscript.imson.script.api;

import com.iscript.imson.IScriptMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileAPI {
    private final ScriptAPI root;

    public FileAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public String getWorldFile(String name) {
        Path path = getWorldDir().resolve(name);
        return path.toString();
    }

    @HostAccess.Export
    public String getPathFile(String path) {
        return FMLPaths.GAMEDIR.get().resolve(path).toString();
    }

    @HostAccess.Export
    public String setWorldFile(String name, String content) {
        try {
            Path path = getWorldDir().resolve(name);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return content;
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to write world file: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public String setPathFile(String path, String content) {
        try {
            Path target = FMLPaths.GAMEDIR.get().resolve(path);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return content;
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to write path file: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public void createWorldFile(String name) {
        setWorldFile(name, "");
    }

    @HostAccess.Export
    public void createPathFile(String path) {
        setPathFile(path, "");
    }

    @HostAccess.Export
    public String read(String path) {
        try {
            Path target = FMLPaths.GAMEDIR.get().resolve(path);
            if (!Files.exists(target)) return "";
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to read file: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public String readWorld(String name) {
        try {
            Path path = getWorldDir().resolve(name);
            if (!Files.exists(path)) return "";
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to read world file: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public String write(String path, String content) {
        return setPathFile(path, content);
    }

    @HostAccess.Export
    public String append(String path, String content) {
        try {
            Path target = FMLPaths.GAMEDIR.get().resolve(path);
            String existing = "";
            if (Files.exists(target)) existing = Files.readString(target, StandardCharsets.UTF_8);
            return setPathFile(path, existing + content);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to append file: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public void remove(String path) {
        try {
            Files.deleteIfExists(FMLPaths.GAMEDIR.get().resolve(path));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to remove file: {}", e.getMessage());
        }
    }

    private Path getWorldDir() {
        String worldName = root.level.getServer().getWorldData().getLevelName();
        return FMLPaths.GAMEDIR.get().resolve("saves").resolve(worldName).resolve("iscript").resolve("files");
    }
}