package com.iscript.iscript.data;

import com.iscript.iscript.data.dialog.DialogData;
import net.minecraft.server.level.ServerLevel;

public class DialogManager {
    public static DialogData get(ServerLevel level, String id) {
        return DialogSavedData.get(level).getDialogs().get(id);
    }
    public static void add(ServerLevel level, DialogData d) {
        DialogSavedData.get(level).addDialog(d);
    }
    public static void remove(ServerLevel level, String id) {
        DialogSavedData.get(level).removeDialog(id);
    }
}