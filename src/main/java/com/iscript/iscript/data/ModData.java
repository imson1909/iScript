package com.iscript.iscript.data;

import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.morph.MorphManager;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Path;

public class ModData {
    public static final String FOLDER = "iscript";
    private static ModData instance;
    private static boolean dirty = false;
    private final Path basePath;
    private final ServerLevel level;

    public final DataContainer<CutsceneData> cutscenes;
    public final DataContainer<DialogData> dialogs;
    public final DataContainer<QuestData> quests;
    public final DataContainer<Graph> events;
    public final DataContainer<Graph> states;
    public final DataContainer<RegionData> regions;
    public final ScriptTextContainer scriptTexts;

    private ModData(ServerLevel level) {
        this.level = level;
        this.basePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve(FOLDER);
        this.cutscenes = new DataContainer<>(basePath.resolve("cutscenes"), CutsceneData::new);
        this.dialogs = new DataContainer<>(basePath.resolve("dialogs"), DialogData::new);
        this.quests = new DataContainer<>(basePath.resolve("quests"), QuestData::new);
        this.events = new DataContainer<>(basePath.resolve("events"), () -> new Graph(ScriptNodeType.class));
        this.states = new DataContainer<>(basePath.resolve("states"), () -> new Graph(null));
        this.regions = new DataContainer<>(basePath.resolve("regions"), RegionData::new);
        this.scriptTexts = new ScriptTextContainer(basePath.resolve("scripts"));
    }

    public Path getBasePath() { return basePath; }
    public ServerLevel getLevel() { return level; }

    public void load() {
        cutscenes.load();
        dialogs.load();
        quests.load();
        events.load();
        states.load();
        regions.load();
        scriptTexts.load();
        GlobalStates.init(basePath);
        NPCManager.init(basePath);
        MorphManager.init(basePath);
        IScriptDataStorage.init(level);
        IScriptDataStorage.get().loadAll();
        dirty = false;
    }

    public void save() {
        cutscenes.saveAndCleanup();
        dialogs.saveAndCleanup();
        quests.saveAndCleanup();
        events.saveAndCleanup();
        states.saveAndCleanup();
        regions.saveAndCleanup();
        scriptTexts.save();
        GlobalStates.save();
        NPCManager.save();
        IScriptDataStorage.get().saveAll();
        dirty = false;
    }

    public static void setDirty() { dirty = true; }
    public static void setDirty(boolean value) { dirty = value; }
    public static boolean isDirty() { return dirty; }
    public static ModData get() { return instance; }

    public static void init(ServerLevel level) {
        instance = new ModData(level);
        instance.load();
    }

    public static void shutdown() {
        if (instance != null) {
            instance.save();
            instance = null;
        }
    }
}