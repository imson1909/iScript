package com.iscript.imson.morph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.*;
import java.util.stream.Collectors;

public class EntityModelProvider {
    private static final Map<String, List<String>> ENTITY_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, String> ENTITY_DISPLAY_NAMES = new HashMap<>();

    public static void init() {
        ENTITY_CATEGORIES.clear();
        ENTITY_DISPLAY_NAMES.clear();

        List<String> passive = new ArrayList<>();
        List<String> hostile = new ArrayList<>();
        List<String> neutral = new ArrayList<>();
        List<String> water = new ArrayList<>();
        List<String> ambient = new ArrayList<>();
        List<String> misc = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) continue;

            String registryName = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            String displayName = type.getDescription().getString();

            ENTITY_DISPLAY_NAMES.put(registryName, displayName);

            MobCategory category = type.getCategory();
            switch (category) {
                case CREATURE -> passive.add(registryName);
                case MONSTER -> hostile.add(registryName);
                case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE -> water.add(registryName);
                case AMBIENT -> ambient.add(registryName);
                case MISC -> misc.add(registryName);
                default -> neutral.add(registryName);
            }
        }

        if (!passive.isEmpty()) ENTITY_CATEGORIES.put("Passive Mobs", passive);
        if (!hostile.isEmpty()) ENTITY_CATEGORIES.put("Hostile Mobs", hostile);
        if (!neutral.isEmpty()) ENTITY_CATEGORIES.put("Neutral Mobs", neutral);
        if (!water.isEmpty()) ENTITY_CATEGORIES.put("Water Creatures", water);
        if (!ambient.isEmpty()) ENTITY_CATEGORIES.put("Ambient", ambient);
        if (!misc.isEmpty()) ENTITY_CATEGORIES.put("Other", misc);
    }

    public static Map<String, List<String>> getEntityCategories() {
        return Collections.unmodifiableMap(ENTITY_CATEGORIES);
    }

    public static String getDisplayName(String registryName) {
        return ENTITY_DISPLAY_NAMES.getOrDefault(registryName, registryName);
    }

    public static List<String> getAllEntities() {
        return ENTITY_DISPLAY_NAMES.keySet().stream().sorted().collect(Collectors.toList());
    }
}