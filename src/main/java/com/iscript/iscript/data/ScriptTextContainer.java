package com.iscript.iscript.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ScriptTextContainer {
    private final Path folder;
    private final Map<String, String> cache = new HashMap<>();

    public ScriptTextContainer(Path folder) {
        this.folder = folder;
    }

    public void load() {
        cache.clear();
        if (!Files.exists(folder)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.js")) {
            for (Path file : stream) {
                String id = file.getFileName().toString().replace(".js", "");
                cache.put(id, Files.readString(file));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void save() {
        if (!Files.exists(folder)) {
            try { Files.createDirectories(folder); } catch (Exception e) { e.printStackTrace(); return; }
        }
        for (Map.Entry<String, String> e : cache.entrySet()) {
            try {
                Files.writeString(folder.resolve(e.getKey() + ".js"), e.getValue());
            } catch (IOException ex) { ex.printStackTrace(); }
        }
    }

    public String get(String id) { return cache.getOrDefault(id, ""); }
    public void put(String id, String text) {
        cache.put(id, text != null ? text : "");
        ModData.setDirty();
    }
    public void remove(String id) {
        cache.remove(id);
        ModData.setDirty();
        try { Files.deleteIfExists(folder.resolve(id + ".js")); } catch (Exception ignored) {}
    }
    public boolean has(String id) { return cache.containsKey(id); }
    public Set<String> keys() { return Collections.unmodifiableSet(cache.keySet()); }
}