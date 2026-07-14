package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.DialogGraphManager;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestDialogPacket {
    private final String dialogId;

    public RequestDialogPacket(String dialogId) {
        this.dialogId = dialogId;
    }

    public RequestDialogPacket(FriendlyByteBuf buf) {
        this.dialogId = buf.readUtf(32767);
    }

    public static RequestDialogPacket decode(FriendlyByteBuf buf) {
        return new RequestDialogPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dialogId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            IScriptMod.LOGGER.info("RequestDialogPacket received: {}", dialogId);

            if (dialogId.contains(":")) {
                String[] parts = dialogId.split(":", 2);
                IScriptMod.LOGGER.info("Graph request: graph={}, node={}", parts[0], parts[1]);
                DialogData dialog = DialogGraphManager.convertToDialogData((ServerLevel) player.level(), parts[0], parts[1]);
                if (dialog != null) {
                    IScriptMod.LOGGER.info("Graph dialog found: {}", dialog.getTitle());
                    DialogData filtered = new DialogData();
                    filtered.setId(dialog.getId());
                    filtered.setTitle(dialog.getTitle());
                    filtered.setText(dialog.getText());
                    filtered.setPortrait(dialog.getPortrait());
                    for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) {
                        filtered.getOptions().add(opt);
                    }
                    IScriptNetwork.sendToPlayer(new OpenDialogScreenPacket(filtered), player);
                } else {
                    IScriptMod.LOGGER.warn("Graph dialog not found: {}", dialogId);
                }
            } else {
                DialogData dialog = DialogManager.get((ServerLevel) player.level(), dialogId);
                if (dialog != null) {
                    DialogData filtered = new DialogData();
                    filtered.setId(dialog.getId());
                    filtered.setTitle(dialog.getTitle());
                    filtered.setText(dialog.getText());
                    filtered.setPortrait(dialog.getPortrait());
                    for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) {
                        filtered.getOptions().add(opt);
                    }
                    IScriptNetwork.sendToPlayer(new OpenDialogScreenPacket(filtered), player);
                } else {
                    IScriptMod.LOGGER.warn("Dialog not found: {}", dialogId);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}