package com.iscript.imson.script.api;

import com.iscript.imson.capability.ModCapabilities;
import org.graalvm.polyglot.HostAccess;

public class QuestAPI {
    private final ScriptAPI root;

    public QuestAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void start(String questId) {
        root.player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
            if (data.getProgress(questId) == 0 && !data.isCompleted(questId)) {
                data.setProgress(questId, 1);
            }
        });
    }

    @HostAccess.Export
    public void complete(String questId) {
        root.player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> data.complete(questId));
    }
}