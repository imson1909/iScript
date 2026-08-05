package com.iscript.imson.script.api;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.dialog.DialogData;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.OpenGuiPacket;
import net.minecraft.server.level.ServerPlayer;
import org.graalvm.polyglot.HostAccess;

public class DialogAPI {
    private final ScriptAPI root;

    public DialogAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void open(String dialogId) {
        if (root.player instanceof ServerPlayer serverPlayer) {
            DialogData dialog = DataAccess.dialog(dialogId);
            if (dialog != null) {
                DialogData filtered = new DialogData();
                filtered.setId(dialog.getId());
                filtered.setTitle(dialog.getTitle());
                filtered.setText(dialog.getText());
                filtered.setPortrait(dialog.getPortrait());
                for (DialogData.DialogOption opt : dialog.getAvailableOptions(root.player)) {
                    filtered.getOptions().add(opt);
                }
                IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), serverPlayer);
            }
        }
    }
}