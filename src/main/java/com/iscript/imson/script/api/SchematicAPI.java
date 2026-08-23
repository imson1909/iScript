package com.iscript.imson.script.api;

import com.iscript.imson.IScriptMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SchematicAPI {
    private final ScriptAPI root;

    public SchematicAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void saveSchematic(String name, double x1, double y1, double z1, double x2, double y2, double z2) {
        try {
            ServerLevel level = root.level;
            StructureTemplate template = new StructureTemplate();
            BlockPos pos1 = new BlockPos((int)x1, (int)y1, (int)z1);
            BlockPos pos2 = new BlockPos((int)x2, (int)y2, (int)z2);
            template.fillFromWorld(level, pos1, pos2.subtract(pos1).offset(1, 1, 1), true, null);
            CompoundTag tag = template.save(new CompoundTag());
            Path path = getSchematicPath(name);
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(tag, path.toFile());
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save schematic: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void loadSchematic(String name, double x, double y, double z, boolean ignoreEntities, boolean ignoreBlocks) {
        try {
            Path path = getSchematicPath(name);
            if (!Files.exists(path)) return;
            CompoundTag tag = NbtIo.readCompressed(path.toFile());
            StructureTemplate template = new StructureTemplate();
            template.load(root.level.registryAccess().registryOrThrow(Registries.BLOCK).asLookup(), tag);
            BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
            template.placeInWorld(root.level, pos, pos, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(ignoreEntities).setKnownShape(true), root.level.random, 2);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to load schematic: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void deleteSchematic(String name) {
        try {
            Files.deleteIfExists(getSchematicPath(name));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete schematic: {}", e.getMessage());
        }
    }

    private Path getSchematicPath(String name) {
        String worldName = root.level.getServer().getWorldData().getLevelName();
        return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("saves").resolve(worldName).resolve("iscript").resolve("schematics").resolve(name + ".nbt");
    }
}