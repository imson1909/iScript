package com.iscript.iscript.morph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.morph.animation.AnimationData;
import com.iscript.iscript.morph.model.GeoModel;
import net.minecraft.server.level.ServerLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MorphManager {
    private static final Map<String, GeoModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AnimationData> ANIMATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static Path morphsPath;

    public static void init(Path basePath) {
        morphsPath = basePath.resolve("morphs");
        try {
            Files.createDirectories(morphsPath);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to create morphs directory", e);
        }
        reload();
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

        IScriptMod.LOGGER.info("MorphManager loaded: {} models, {} animations, {} textures",
                MODEL_CACHE.size(), ANIMATION_CACHE.size(), TEXTURE_CACHE.size());
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
        return Collections.unmodifiableMap(MODEL_CACHE);
    }

    public static boolean hasModel(String id) {
        return MODEL_CACHE.containsKey(id);
    }
}