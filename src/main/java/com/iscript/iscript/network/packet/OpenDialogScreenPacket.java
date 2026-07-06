package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.gui.screen.DialogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDialogScreenPacket {
    private final String title;
    private final String text;
    private final int optionCount;
    private final String[] optionTexts;
    private final String[] optionCommands;

    public OpenDialogScreenPacket(DialogData dialog) {
        this.title = dialog.getTitle();
        this.text = dialog.getText();
        this.optionCount = dialog.getOptions().size();
        this.optionTexts = new String[optionCount];
        this.optionCommands = new String[optionCount];
        for (int i = 0; i < optionCount; i++) {
            DialogData.DialogOption opt = dialog.getOptions().get(i);
            this.optionTexts[i] = opt.getText();
            this.optionCommands[i] = opt.getCommand();
        }
    }

    public OpenDialogScreenPacket(FriendlyByteBuf buf) {
        this.title = buf.readUtf(32767);
        this.text = buf.readUtf(32767);
        this.optionCount = buf.readInt();
        this.optionTexts = new String[optionCount];
        this.optionCommands = new String[optionCount];
        for (int i = 0; i < optionCount; i++) {
            this.optionTexts[i] = buf.readUtf(32767);
            this.optionCommands[i] = buf.readUtf(32767);
        }
    }

    public static OpenDialogScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenDialogScreenPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(title);
        buf.writeUtf(text);
        buf.writeInt(optionCount);
        for (int i = 0; i < optionCount; i++) {
            buf.writeUtf(optionTexts[i]);
            buf.writeUtf(optionCommands[i]);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DialogData data = new DialogData();
            data.setTitle(title);
            data.setText(text);
            for (int i = 0; i < optionCount; i++) {
                DialogData.DialogOption opt = new DialogData.DialogOption();
                opt.setText(optionTexts[i]);
                opt.setCommand(optionCommands[i]);
                data.getOptions().add(opt);
            }
            Minecraft.getInstance().setScreen(new DialogScreen(data));
        });
        ctx.get().setPacketHandled(true);
    }
}
