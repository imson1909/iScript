package com.iscript.iscript.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Writer;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

public class DataContainer<T extends DataObject> {
    private final Map<String, T> map = new LinkedHashMap<>();
    private final Path folder;
    private final Supplier<T> factory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public DataContainer(Path folder, Supplier<T> factory) {
        this.folder = folder;
        this.factory = factory;
    }

    public void load() {
        map.clear();
        if (!Files.exists(folder)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : stream) {
                String content = Files.readString(file);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                T t = factory.get();
                t.fromJson(json);
                if (t.getId() != null && !t.getId().isEmpty()) map.put(t.getId(), t);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void save() {
        if (!Files.exists(folder)) {
            try { Files.createDirectories(folder); } catch (Exception e) { e.printStackTrace(); return; }
        }
        for (T t : map.values()) {
            String id = t.getId();
            Path target = folder.resolve(id + ".json");
            Path temp = folder.resolve(id + ".json.tmp");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temp.toFile())) {
                String json = gson.toJson(t.toJson());
                fos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.getFD().sync();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            try {
                Files.deleteIfExists(target);
                Files.move(temp, target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAndCleanup() {
        save();
        if (!Files.exists(folder)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : stream) {
                String id = file.getFileName().toString().replace(".json", "");
                if (!map.containsKey(id)) Files.delete(file);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public T get(String id) { return id == null ? null : map.get(id); }
    public Map<String, T> all() { return Collections.unmodifiableMap(map); }
    public void put(T t) {
        if (t != null && t.getId() != null && !t.getId().isEmpty()) {
            map.put(t.getId(), t);
            ModData.setDirty();
        }
    }
    public T remove(String id) {
        T t = map.remove(id);
        if (t != null) ModData.setDirty();
        return t;
    }
    public boolean has(String id) { return map.containsKey(id); }
    public int size() { return map.size(); }
}