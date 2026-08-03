package com.iscript.iscript.data;

import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DataAccess {

    public static ModData data() { return ModData.get(); }

    public static CutsceneData cutscene(String id) {
        ModData d = data();
        return d == null ? null : d.cutscenes.get(id);
    }
    public static Map<String, CutsceneData> cutscenes() {
        ModData d = data();
        return d == null ? Map.of() : d.cutscenes.all();
    }
    public static void putCutscene(CutsceneData c) {
        ModData d = data();
        if (d != null) d.cutscenes.put(c);
    }
    public static void removeCutscene(String id) {
        ModData d = data();
        if (d != null) d.cutscenes.remove(id);
    }

    public static DialogData dialog(String id) {
        ModData d = data();
        return d == null ? null : d.dialogs.get(id);
    }
    public static Map<String, DialogData> dialogs() {
        ModData d = data();
        return d == null ? Map.of() : d.dialogs.all();
    }
    public static void putDialog(DialogData d) {
        ModData md = data();
        if (md != null) md.dialogs.put(d);
    }
    public static void removeDialog(String id) {
        ModData d = data();
        if (d != null) d.dialogs.remove(id);
    }
    public static Graph dialogGraph(String id) {
        DialogData dd = dialog(id);
        return dd != null ? dd.getGraph() : null;
    }
    public static Map<String, Graph> dialogGraphs() {
        Map<String, Graph> result = new HashMap<>();
        for (var e : dialogs().entrySet()) {
            if (e.getValue().getGraph() != null) result.put(e.getKey(), e.getValue().getGraph());
        }
        return result;
    }
    public static void putDialogGraph(Graph g) {
        if (g == null || g.getId() == null || g.getId().isEmpty()) return;
        DialogData dd = dialog(g.getId());
        if (dd == null) {
            dd = new DialogData();
            dd.setId(g.getId());
            dd.setTitle(g.getName().isEmpty() ? g.getId() : g.getName());
        }
        dd.setGraph(g);
        putDialog(dd);
    }
    public static void removeDialogGraph(String id) {
        DialogData dd = dialog(id);
        if (dd != null) dd.setGraph(null);
    }

    public static QuestData quest(String id) {
        ModData d = data();
        return d == null ? null : d.quests.get(id);
    }
    public static Map<String, QuestData> quests() {
        ModData d = data();
        return d == null ? Map.of() : d.quests.all();
    }
    public static void putQuest(QuestData q) {
        ModData d = data();
        if (d != null) d.quests.put(q);
    }
    public static void removeQuest(String id) {
        ModData d = data();
        if (d != null) d.quests.remove(id);
    }
    public static Graph questGraph(String id) {
        QuestData q = quest(id);
        return q != null ? q.getGraph() : null;
    }
    public static void putQuestGraph(Graph g) {
        if (g == null || g.getId() == null || g.getId().isEmpty()) return;
        QuestData q = quest(g.getId());
        if (q == null) {
            q = new QuestData();
            q.setId(g.getId());
            q.setTitle(g.getName().isEmpty() ? g.getId() : g.getName());
        }
        q.setGraph(g);
        putQuest(q);
    }
    public static void removeQuestGraph(String id) {
        QuestData q = quest(id);
        if (q != null) q.setGraph(null);
    }

    public static Graph event(String id) {
        ModData d = data();
        return d == null ? null : d.events.get(id);
    }
    public static Map<String, Graph> events() {
        ModData d = data();
        return d == null ? Map.of() : d.events.all();
    }
    public static void putEvent(Graph g) {
        ModData d = data();
        if (d != null) d.events.put(g);
    }
    public static void removeEvent(String id) {
        ModData d = data();
        if (d != null) d.events.remove(id);
    }
    public static Graph eventGraph(String id) { return event(id); }
    public static Map<String, Graph> eventGraphs() { return events(); }
    public static void putEventGraph(Graph g) { putEvent(g); }
    public static void removeEventGraph(String id) { removeEvent(id); }

    public static Graph state(String id) {
        ModData d = data();
        return d == null ? null : d.states.get(id);
    }
    public static Map<String, Graph> states() {
        ModData d = data();
        return d == null ? Map.of() : d.states.all();
    }
    public static void putState(Graph g) {
        ModData d = data();
        if (d != null) d.states.put(g);
    }
    public static void removeState(String id) {
        ModData d = data();
        if (d != null) d.states.remove(id);
    }

    public static RegionData region(String id) {
        ModData d = data();
        return d == null ? null : d.regions.get(id);
    }
    public static Map<String, RegionData> regions() {
        ModData d = data();
        return d == null ? Map.of() : d.regions.all();
    }
    public static void putRegion(RegionData r) {
        ModData d = data();
        if (d != null) d.regions.put(r);
    }
    public static void removeRegion(String id) {
        ModData d = data();
        if (d != null) d.regions.remove(id);
    }

    public static NPCData npc(String id) { return NPCManager.get(id); }
    public static Map<String, NPCData> npcs() { return NPCManager.all(); }
    public static void putNpc(NPCData n) { NPCManager.put(n); }
    public static void removeNpc(String id) { NPCManager.remove(id); }

    public static String scriptText(String id) {
        ModData d = data();
        return d == null ? "" : d.scriptTexts.get(id);
    }
    public static void putScriptText(String id, String text) {
        ModData d = data();
        if (d != null) d.scriptTexts.put(id, text);
    }
    public static void removeScriptText(String id) {
        ModData d = data();
        if (d != null) d.scriptTexts.remove(id);
    }
    public static boolean hasScriptText(String id) {
        ModData d = data();
        return d != null && d.scriptTexts.has(id);
    }

    public static Graph scriptGraph(String id) {
        ModData d = data();
        return d != null ? ScriptGraphManager.get(d.getLevel(), id) : null;
    }
    public static Map<String, Graph> scriptGraphs() {
        ModData d = data();
        return d != null ? ScriptGraphManager.getAll(d.getLevel()) : Map.of();
    }
    public static void putScriptGraph(Graph g) {
        ModData d = data();
        if (d != null) ScriptGraphManager.add(d.getLevel(), g, "");
    }
    public static void removeScriptGraph(String id) {
        ModData d = data();
        if (d != null) ScriptGraphManager.remove(d.getLevel(), id);
    }

    public static PlayerQuestData playerQuests(ServerLevel level) { return PlayerQuestData.get(level); }

    public static boolean canStartQuest(ServerLevel level, UUID playerId, String questId) {
        QuestData q = quest(questId);
        if (q == null) return false;
        PlayerQuestData pd = playerQuests(level);
        if (pd.isActive(playerId, questId)) return false;
        if (pd.hasCompleted(playerId, questId)) return false;
        for (String pre : q.getPrerequisites()) if (!pd.hasCompleted(playerId, pre)) return false;
        return true;
    }

    public static boolean startQuest(ServerLevel level, UUID playerId, String questId) {
        if (!canStartQuest(level, playerId, questId)) return false;
        QuestData q = quest(questId);
        if (q == null) return false;
        playerQuests(level).startQuest(playerId, QuestProgress.fromTemplate(q));
        return true;
    }

    private DataAccess() {}
}