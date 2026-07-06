package com.iscript.iscript.command;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.RegionManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.dialog.DialogCondition;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.OpenQuestJournalPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class IScriptCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        LiteralArgumentBuilder<CommandSourceStack> dialog = Commands.literal("dialog");
        dialog.then(Commands.literal("create")
            .then(Commands.argument("id", StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    DialogData d = new DialogData();
                    d.setId(id);
                    d.setTitle("Dialog " + id);
                    d.setText("Hello!");
                    DialogManager.add((ServerLevel) player.level(), d);
                    ctx.getSource().sendSuccess(() -> Component.literal("Dialog " + id + " created"), false);
                    return 1;
                })));
        dialog.then(Commands.literal("settext")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String text = StringArgumentType.getString(ctx, "text");
                        DialogData d = DialogManager.get((ServerLevel) player.level(), id);
                        if (d != null) {
                            d.setText(text);
                            DialogManager.add((ServerLevel) player.level(), d);
                            ctx.getSource().sendSuccess(() -> Component.literal("Dialog updated"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Dialog not found"));
                        }
                        return 1;
                    }))));
        dialog.then(Commands.literal("setportrait")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("portrait", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String portrait = StringArgumentType.getString(ctx, "portrait");
                        DialogData d = DialogManager.get((ServerLevel) player.level(), id);
                        if (d != null) {
                            d.setPortrait(portrait);
                            DialogManager.add((ServerLevel) player.level(), d);
                            ctx.getSource().sendSuccess(() -> Component.literal("Portrait set"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Dialog not found"));
                        }
                        return 1;
                    }))));
        dialog.then(Commands.literal("addoption")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.string())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            String text = StringArgumentType.getString(ctx, "text");
                            String cmd = StringArgumentType.getString(ctx, "command");
                            DialogData d = DialogManager.get((ServerLevel) player.level(), id);
                            if (d != null) {
                                DialogData.DialogOption opt = new DialogData.DialogOption();
                                opt.setText(text);
                                opt.setCommand(cmd);
                                d.getOptions().add(opt);
                                DialogManager.add((ServerLevel) player.level(), d);
                                ctx.getSource().sendSuccess(() -> Component.literal("Option added"), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Dialog not found"));
                            }
                            return 1;
                        })))));
        dialog.then(Commands.literal("addoptionquest")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.string())
                    .then(Commands.argument("questId", StringArgumentType.word())
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String id = StringArgumentType.getString(ctx, "id");
                                String text = StringArgumentType.getString(ctx, "text");
                                String questId = StringArgumentType.getString(ctx, "questId");
                                String cmd = StringArgumentType.getString(ctx, "command");
                                DialogData d = DialogManager.get((ServerLevel) player.level(), id);
                                if (d != null) {
                                    DialogData.DialogOption opt = new DialogData.DialogOption();
                                    opt.setText(text);
                                    DialogCondition cond = new DialogCondition();
                                    cond.setType(DialogCondition.ConditionType.QUEST_COMPLETED);
                                    cond.setValue(questId);
                                    opt.setCondition(cond);
                                    opt.setCommand(cmd);
                                    d.getOptions().add(opt);
                                    DialogManager.add((ServerLevel) player.level(), d);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest-locked option added"), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("Dialog not found"));
                                }
                                return 1;
                            }))))));

        LiteralArgumentBuilder<CommandSourceStack> quest = Commands.literal("quest");
        quest.then(Commands.literal("create")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("title", StringArgumentType.string())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String title = StringArgumentType.getString(ctx, "title");
                        QuestData q = new QuestData();
                        q.setId(id);
                        q.setTitle(title);
                        QuestManager.add((ServerLevel) player.level(), q);
                        ctx.getSource().sendSuccess(() -> Component.literal("Quest " + id + " created"), false);
                        return 1;
                    }))));
        quest.then(Commands.literal("setdesc")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("description", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String desc = StringArgumentType.getString(ctx, "description");
                        QuestData q = QuestManager.get((ServerLevel) player.level(), id);
                        if (q != null) {
                            q.setDescription(desc);
                            QuestManager.add((ServerLevel) player.level(), q);
                            ctx.getSource().sendSuccess(() -> Component.literal("Description set"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Quest not found"));
                        }
                        return 1;
                    }))));
        quest.then(Commands.literal("setobjective")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("type", StringArgumentType.word())
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String id = StringArgumentType.getString(ctx, "id");
                                String type = StringArgumentType.getString(ctx, "type");
                                String target = StringArgumentType.getString(ctx, "target");
                                int count = IntegerArgumentType.getInteger(ctx, "count");
                                QuestData q = QuestManager.get((ServerLevel) player.level(), id);
                                if (q != null) {
                                    q.setObjectiveType(type);
                                    q.setTarget(target);
                                    q.setRequiredCount(count);
                                    QuestManager.add((ServerLevel) player.level(), q);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest objective set"), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("Quest not found"));
                                }
                                return 1;
                            }))))));
        quest.then(Commands.literal("setreward")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String cmd = StringArgumentType.getString(ctx, "command");
                        QuestData q = QuestManager.get((ServerLevel) player.level(), id);
                        if (q != null) {
                            q.setRewardCommand(cmd);
                            QuestManager.add((ServerLevel) player.level(), q);
                            ctx.getSource().sendSuccess(() -> Component.literal("Quest reward set"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Quest not found"));
                        }
                        return 1;
                    }))));
        quest.then(Commands.literal("list")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ctx.getSource().sendSuccess(() -> Component.literal("=== Quests ==="), false);
                for (QuestData q : QuestManager.getAll((ServerLevel) player.level()).values()) {
                    ctx.getSource().sendSuccess(() -> Component.literal(q.getId() + ": " + q.getTitle() + " [" + q.getObjectiveType() + "]"), false);
                }
                return 1;
            }));
        quest.then(Commands.literal("progress")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Your Quests ==="), false);
                    for (QuestData q : QuestManager.getAll((ServerLevel) player.level()).values()) {
                        if (data.isCompleted(q.getId())) {
                            ctx.getSource().sendSuccess(() -> Component.literal(q.getTitle() + " [COMPLETED]"), false);
                        } else if (data.getProgress(q.getId()) > 0) {
                            ctx.getSource().sendSuccess(() -> Component.literal(q.getTitle() + " [" + data.getProgress(q.getId()) + "/" + q.getRequiredCount() + "]"), false);
                        }
                    }
                });
                return 1;
            }));
        quest.then(Commands.literal("journal")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
                    IScriptNetwork.sendToPlayer(new OpenQuestJournalPacket(data.save()), player);
                });
                return 1;
            }));

        LiteralArgumentBuilder<CommandSourceStack> region = Commands.literal("region");
        region.then(Commands.literal("create")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String name = StringArgumentType.getString(ctx, "name");
                        RegionData r = new RegionData();
                        r.setId(id);
                        r.setName(name);
                        r.setPos1(player.blockPosition());
                        r.setPos2(player.blockPosition().offset(5, 5, 5));
                        RegionManager.add((ServerLevel) player.level(), r);
                        ctx.getSource().sendSuccess(() -> Component.literal("Region " + id + " created at your position"), false);
                        return 1;
                    }))));
        region.then(Commands.literal("setpos1")
            .then(Commands.argument("id", StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    RegionData r = RegionManager.get((ServerLevel) player.level(), id);
                    if (r != null) {
                        r.setPos1(player.blockPosition());
                        RegionManager.add((ServerLevel) player.level(), r);
                        ctx.getSource().sendSuccess(() -> Component.literal("Pos1 set"), false);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("Region not found"));
                    }
                    return 1;
                })));
        region.then(Commands.literal("setpos2")
            .then(Commands.argument("id", StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    RegionData r = RegionManager.get((ServerLevel) player.level(), id);
                    if (r != null) {
                        r.setPos2(player.blockPosition());
                        RegionManager.add((ServerLevel) player.level(), r);
                        ctx.getSource().sendSuccess(() -> Component.literal("Pos2 set"), false);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("Region not found"));
                    }
                    return 1;
                })));
        region.then(Commands.literal("setenter")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String cmd = StringArgumentType.getString(ctx, "command");
                        RegionData r = RegionManager.get((ServerLevel) player.level(), id);
                        if (r != null) {
                            RegionEffect effect = new RegionEffect();
                            effect.setType(RegionEffect.EffectType.COMMAND);
                            effect.setValue(cmd);
                            r.getEnterEffects().add(effect);
                            RegionManager.add((ServerLevel) player.level(), r);
                            ctx.getSource().sendSuccess(() -> Component.literal("Enter effect added"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Region not found"));
                        }
                        return 1;
                    }))));
        region.then(Commands.literal("setexit")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        String cmd = StringArgumentType.getString(ctx, "command");
                        RegionData r = RegionManager.get((ServerLevel) player.level(), id);
                        if (r != null) {
                            RegionEffect effect = new RegionEffect();
                            effect.setType(RegionEffect.EffectType.COMMAND);
                            effect.setValue(cmd);
                            r.getExitEffects().add(effect);
                            RegionManager.add((ServerLevel) player.level(), r);
                            ctx.getSource().sendSuccess(() -> Component.literal("Exit effect added"), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Region not found"));
                        }
                        return 1;
                    }))));
        region.then(Commands.literal("setpotion")
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("effect", StringArgumentType.word())
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String id = StringArgumentType.getString(ctx, "id");
                                String effect = StringArgumentType.getString(ctx, "effect");
                                int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                int amplifier = IntegerArgumentType.getInteger(ctx, "amplifier");
                                RegionData r = RegionManager.get((ServerLevel) player.level(), id);
                                if (r != null) {
                                    RegionEffect re = new RegionEffect();
                                    re.setType(RegionEffect.EffectType.POTION);
                                    re.setValue(effect);
                                    re.setDuration(duration);
                                    re.setAmplifier(amplifier);
                                    r.getTickEffects().add(re);
                                    RegionManager.add((ServerLevel) player.level(), r);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Potion effect added to region"), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("Region not found"));
                                }
                                return 1;
                            }))))));

        dispatcher.register(Commands.literal("iscript")
            .then(dialog)
            .then(quest)
            .then(region));
    }
}
