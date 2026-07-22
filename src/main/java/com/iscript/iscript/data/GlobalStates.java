package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.api.states.States;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalStates {
    private static final String FILENAME = "global_states.dat";
    private static final String OLD_FILENAME = "iscript_global_states.dat";
    private static States instance;
    private static File file;
    private static boolean initialized = false;

    public static void init(Path worldFolder) {
        if (initialized) {
            IScriptMod.LOGGER.warn("GlobalStates already initialized at: {}", file);
            return;
        }
        file = worldFolder.resolve(FILENAME).toFile();
        initialized = true;
        instance = new States();

        File oldFile = worldFolder.resolve(OLD_FILENAME).toFile();
        if (oldFile.exists()) {
            if (!file.exists()) {
                try {
                    Files.move(oldFile.toPath(), file.toPath());
                    IScriptMod.LOGGER.info("GlobalStates migrated: {} -> {}", OLD_FILENAME, FILENAME);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("GlobalStates migration failed", e);
                }
            } else {
                IScriptMod.LOGGER.warn("Both old and new global states files exist! Using: {}", FILENAME);
            }
        }

        load();
        IScriptMod.LOGGER.info("GlobalStates ready: {} ({} entries)", file.getAbsolutePath(), instance.keys().size());
    }

    public static States get() {
        if (instance == null) {
            throw new IllegalStateException("GlobalStates not initialized");
        }
        return instance;
    }

    public static void load() {
        if (file == null || !file.exists()) return;
        try {
            CompoundTag tag = NbtIo.read(file);
            if (tag != null) instance.deserialize(tag);
        } catch (Exception e) {
            IScriptMod.LOGGER.error("GlobalStates load failed: {}", file, e);
        }
    }

    public static void save() {
        if (file == null || instance == null) {
            IScriptMod.LOGGER.warn("GlobalStates.save() skipped: file={}, instance={}", file, instance);
            return;
        }
        try {
            Path target = file.toPath();
            Path temp = target.resolveSibling(FILENAME + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(temp.toFile());
                 DataOutputStream dos = new DataOutputStream(fos)) {
                NbtIo.write(instance.serialize(), dos);
                fos.getFD().sync();
            }
            Files.deleteIfExists(target);
            Files.move(temp, target);
        } catch (Exception e) {
            IScriptMod.LOGGER.error("GlobalStates save failed: {}", file, e);
        }
    }

    public static void shutdown() {
        save();
        initialized = false;
        instance = null;
        file = null;
    }
}