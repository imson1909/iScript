package com.iscript.iscript.data.dialog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class DialogData {
    private String id = "";
    private String title = "Dialog";
    private String text = "Hello!";
    private String sound = "";
    private String portrait = "";
    private List<DialogOption> options = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    public String getPortrait() { return portrait; }
    public void setPortrait(String portrait) { this.portrait = portrait; }
    public List<DialogOption> getOptions() { return options; }

    public List<DialogOption> getAvailableOptions(Player player) {
        List<DialogOption> result = new ArrayList<>();
        for (DialogOption opt : options) {
            if (opt.getCondition().check(player)) {
                result.add(opt);
            }
        }
        return result;
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Title", this.title);
        tag.putString("Text", this.text);
        tag.putString("Sound", this.sound);
        tag.putString("Portrait", this.portrait);
        CompoundTag optionsTag = new CompoundTag();
        for (int i = 0; i < options.size(); i++) {
            CompoundTag opt = new CompoundTag();
            options.get(i).save(opt);
            optionsTag.put(String.valueOf(i), opt);
        }
        tag.put("Options", optionsTag);
    }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.title = tag.getString("Title");
        this.text = tag.getString("Text");
        this.sound = tag.getString("Sound");
        this.portrait = tag.getString("Portrait");
        this.options.clear();
        CompoundTag optionsTag = tag.getCompound("Options");
        for (String key : optionsTag.getAllKeys()) {
            DialogOption opt = new DialogOption();
            opt.load(optionsTag.getCompound(key));
            this.options.add(opt);
        }
    }

    public static class DialogOption {
        private String text = "Continue...";
        private String targetDialogId = "";
        private String command = "";
        private DialogCondition condition = new DialogCondition();
        private String tooltip = "";

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getTargetDialogId() { return targetDialogId; }
        public void setTargetDialogId(String id) { this.targetDialogId = id; }
        public String getCommand() { return command; }
        public void setCommand(String cmd) { this.command = cmd; }
        public DialogCondition getCondition() { return condition; }
        public void setCondition(DialogCondition c) { this.condition = c; }
        public String getTooltip() { return tooltip; }
        public void setTooltip(String t) { this.tooltip = t; }

        public void save(CompoundTag tag) {
            tag.putString("Text", this.text);
            tag.putString("Target", this.targetDialogId);
            tag.putString("Command", this.command);
            tag.putString("Tooltip", this.tooltip);
            CompoundTag cond = new CompoundTag();
            this.condition.save(cond);
            tag.put("Condition", cond);
        }

        public void load(CompoundTag tag) {
            this.text = tag.getString("Text");
            this.targetDialogId = tag.getString("Target");
            this.command = tag.getString("Command");
            this.tooltip = tag.getString("Tooltip");
            this.condition.load(tag.getCompound("Condition"));
        }
    }
}
