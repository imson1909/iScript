package com.iscript.iscript.skin;

import com.iscript.iscript.IScriptMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class SkinManager {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Path SKIN_DIR = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "iscript", "skins");
    private static final long MAX_SIZE = 8 * 1024 * 1024;

    public static ResourceLocation getOrLoad(String url) {
        if (url == null || url.isEmpty()) return null;
        if (CACHE.containsKey(url)) return CACHE.get(url);

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                IScriptMod.LOGGER.warn("Invalid skin URL: {}", url);
                return null;
            }

            Files.createDirectories(SKIN_DIR);
            String hash = sha1(url);
            Path file = SKIN_DIR.resolve(hash + ".png");

            if (!Files.exists(file)) {
                download(url, file);
            }

            try (InputStream is = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(is);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation id = new ResourceLocation(IScriptMod.MOD_ID, "skins/" + hash);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                CACHE.put(url, id);
                return id;
            }
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to load skin from {}: {}", url, e.getMessage());
            return null;
        }
    }

    private static void download(String url, Path dest) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        long size = conn.getContentLengthLong();
        if (size > MAX_SIZE) {
            throw new IllegalStateException("File too large: " + size + " bytes");
        }
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest);
        }
    }

    private static String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}