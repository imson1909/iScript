package com.iscript.imson.morph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iscript.imson.IScriptMod;
import com.iscript.imson.morph.animation.AnimationData;
import com.iscript.imson.morph.model.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MorphManager {
    private static final Map<String, GeoModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AnimationData> ANIMATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, EntityModel<?>> ENTITY_MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, EntityType<?>> ENTITY_CACHE = new ConcurrentHashMap<>();
    private static Path morphsPath;

    public static void init(Path basePath) {
        morphsPath = basePath.resolve("morphs");
        try {
            Files.createDirectories(morphsPath);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to create morphs directory", e);
        }
        reload();
        initEntityCache();
    }

    private static void initEntityCache() {
        ENTITY_CACHE.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) continue;
            if (type.getCategory() == MobCategory.MISC) continue;

            String registryName = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            ENTITY_CACHE.put("entity:" + registryName, type);
        }
        IScriptMod.LOGGER.info("Entity cache initialized with {} entities", ENTITY_CACHE.size());
    }

    public static void reload() {
        MODEL_CACHE.clear();
        ANIMATION_CACHE.clear();
        TEXTURE_CACHE.clear();
        if (morphsPath == null || !Files.exists(morphsPath)) return;

        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(morphsPath, Files::isDirectory)) {
            for (Path dir : dirs) {
                String modelId = dir.getFileName().toString();
                loadModelDir(dir, modelId);
            }
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Morph reload failed", e);
        }
        IScriptMod.LOGGER.info("MorphManager loaded: {} models, {} animations, {} textures, {} entities",
                MODEL_CACHE.size(), ANIMATION_CACHE.size(), TEXTURE_CACHE.size(), ENTITY_CACHE.size());
    }

    private static void loadModelDir(Path dir, String modelId) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            Path geoFile = null;
            Path animFile = null;
            Path textureFile = null;
            for (Path file : files) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".geo.json")) {
                    geoFile = file;
                } else if (name.endsWith(".json") && !name.endsWith(".geo.json")) {
                    animFile = file;
                } else if (name.endsWith(".png")) {
                    textureFile = file;
                }
            }
            if (geoFile != null) {
                try (InputStream is = Files.newInputStream(geoFile)) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    GeoModel model = GeoModel.parse(json);
                    model.setId(modelId);
                    MODEL_CACHE.put(modelId, model);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Failed to load model {}: {}", modelId, e.getMessage());
                }
            }
            if (animFile != null) {
                try (InputStream is = Files.newInputStream(animFile)) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    AnimationData anim = AnimationData.parse(json);
                    ANIMATION_CACHE.put(modelId, anim);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Failed to load animation {}: {}", modelId, e.getMessage());
                }
            }
            if (textureFile != null) {
                try (InputStream is = Files.newInputStream(textureFile)) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) TEXTURE_CACHE.put(modelId, img);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Failed to load texture {}: {}", modelId, e.getMessage());
                }
            }
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to read model dir {}: {}", modelId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static EntityModel<?> getEntityModel(EntityType<?> type) {
        return ENTITY_MODEL_CACHE.computeIfAbsent(type, t -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                ModelLayerLocation layer = switch (t.toString()) {
                    case "minecraft:zombie", "minecraft:husk", "minecraft:drowned" ->
                            net.minecraft.client.model.geom.ModelLayers.ZOMBIE;
                    case "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton" ->
                            net.minecraft.client.model.geom.ModelLayers.SKELETON;
                    case "minecraft:creeper" ->
                            net.minecraft.client.model.geom.ModelLayers.CREEPER;
                    case "minecraft:spider" ->
                            net.minecraft.client.model.geom.ModelLayers.SPIDER;
                    case "minecraft:cow", "minecraft:mooshroom" ->
                            net.minecraft.client.model.geom.ModelLayers.COW;
                    case "minecraft:pig" ->
                            net.minecraft.client.model.geom.ModelLayers.PIG;
                    case "minecraft:sheep" ->
                            net.minecraft.client.model.geom.ModelLayers.SHEEP;
                    case "minecraft:chicken" ->
                            net.minecraft.client.model.geom.ModelLayers.CHICKEN;
                    default -> null;
                };

                if (layer != null) {
                    ModelPart modelPart = mc.getEntityModels().bakeLayer(layer);
                    return new net.minecraft.client.model.HumanoidModel<>(modelPart);
                }
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Failed to create model for entity: {}", t, e);
            }
            return null;
        });
    }

    public static ResourceLocation getEntityTexture(EntityType<?> type) {
        return switch (type.toString()) {
            case "minecraft:zombie" -> new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");
            case "minecraft:skeleton" -> new ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png");
            case "minecraft:creeper" -> new ResourceLocation("minecraft", "textures/entity/creeper/creeper.png");
            case "minecraft:spider" -> new ResourceLocation("minecraft", "textures/entity/spider/spider.png");
            case "minecraft:cow" -> new ResourceLocation("minecraft", "textures/entity/cow/cow.png");
            case "minecraft:pig" -> new ResourceLocation("minecraft", "textures/entity/pig/pig.png");
            case "minecraft:sheep" -> new ResourceLocation("minecraft", "textures/entity/sheep/sheep.png");
            case "minecraft:chicken" -> new ResourceLocation("minecraft", "textures/entity/chicken/chicken.png");
            default -> new ResourceLocation("minecraft", "textures/block/stone.png");
        };
    }

    public static GeoModel getModel(String id) {
        return MODEL_CACHE.get(id);
    }

    public static AnimationData getAnimation(String id) {
        return ANIMATION_CACHE.get(id);
    }

    public static BufferedImage getTexture(String id) {
        return TEXTURE_CACHE.get(id);
    }

    public static Map<String, GeoModel> getAllModels() {
        Map<String, GeoModel> all = new HashMap<>(MODEL_CACHE);
        // Добавляем entity модели
        for (Map.Entry<String, EntityType<?>> entry : ENTITY_CACHE.entrySet()) {
            // Создаём заглушку для entity (фактически их нет в MODEL_CACHE, но они будут в списке)
        }
        return Collections.unmodifiableMap(all);
    }

    public static Map<String, EntityType<?>> getAllEntities() {
        return Collections.unmodifiableMap(ENTITY_CACHE);
    }

    public static boolean hasModel(String id) {
        return MODEL_CACHE.containsKey(id) || ENTITY_CACHE.containsKey(id);
    }

    public static boolean isEntityModel(String id) {
        return id.startsWith("entity:");
    }
}