package com.iscript.iscript.command;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.RegionManager;
import com.iscript.iscript.data.WorldDataManager;
import com.iscript.iscript.data.cutscene.CutsceneAction;
import com.iscript.iscript.data.cutscene.CutsceneActionType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.cutscene.CutscenePlayer;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.dialog.DialogCondition;
import com.iscript.iscript.data.dialog.DialogGraphData;
import com.iscript.iscript.data.dialog.DialogNodeData;
import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.npc.NPCState;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjective;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestReward;
import com.iscript.iscript.data.quest.QuestStage;
import com.iscript.iscript.data.quest.QuestStatus;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.event.EventType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.registry.ModItems;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptGraphExecutor;
import com.iscript.iscript.script.ScriptGraphManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.iscript.iscript.script.ScriptFileManager;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import com.iscript.iscript.network.packet.OpenGuiPacket;

import java.util.Map;
import java.util.Set;

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
        dialog.then(Commands.literal("open")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            DialogData dialogData = DialogManager.get((ServerLevel) player.level(), id);
                            if (dialogData != null) {
                                DialogData filtered = new DialogData();
                                filtered.setId(dialogData.getId());
                                filtered.setTitle(dialogData.getTitle());
                                filtered.setText(dialogData.getText());
                                filtered.setPortrait(dialogData.getPortrait());
                                for (DialogData.DialogOption opt : dialogData.getAvailableOptions(player)) {
                                    filtered.getOptions().add(opt);
                                }
                                IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Dialog opened"), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Dialog not found"));
                            }
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
                                                        try {
                                                            QuestObjectiveType objType = QuestObjectiveType.valueOf(type.toUpperCase());
                                                            QuestObjective obj = new QuestObjective();
                                                            obj.setType(objType);
                                                            obj.setTarget(target);
                                                            obj.setRequiredCount(count);
                                                            obj.setCurrentCount(0);
                                                            if (q.getStages().isEmpty()) {
                                                                QuestStage stage = new QuestStage();
                                                                stage.setDescription("Stage 1");
                                                                q.getStages().add(stage);
                                                            }
                                                            q.getStages().get(0).getObjectives().add(obj);
                                                            QuestManager.add((ServerLevel) player.level(), q);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("Quest objective set"), false);
                                                        } catch (IllegalArgumentException e) {
                                                            ctx.getSource().sendFailure(Component.literal("Invalid objective type"));
                                                            return 0;
                                                        }
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
                                        QuestReward reward = q.getReward();
                                        if (reward == null) {
                                            reward = new QuestReward();
                                            q.setReward(reward);
                                        }
                                        reward.setCommand(cmd);
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
                        int objCount = 0;
                        for (QuestStage stage : q.getStages()) {
                            objCount += stage.getObjectives().size();
                        }
                        final int finalObjCount = objCount;
                        ctx.getSource().sendSuccess(() -> Component.literal(q.getId() + ": " + q.getTitle() + " (" + finalObjCount + " objectives)"), false);
                    }
                    return 1;
                }));
        quest.then(Commands.literal("progress")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Your Quests ==="), false);
                    Map<String, QuestProgress> playerQuests = QuestManager.getPlayerQuests((ServerLevel) player.level(), player.getUUID());
                    for (QuestData q : QuestManager.getAll((ServerLevel) player.level()).values()) {
                        QuestProgress progress = playerQuests.get(q.getId());
                        if (progress != null) {
                            if (progress.getStatus() == QuestStatus.COMPLETED) {
                                ctx.getSource().sendSuccess(() -> Component.literal(q.getTitle() + " [COMPLETED]"), false);
                            } else if (progress.getStatus() == QuestStatus.ACTIVE) {
                                QuestStage stage = progress.getCurrentStage();
                                if (stage != null) {
                                    int current = 0;
                                    int required = 0;
                                    for (QuestObjective obj : stage.getObjectives()) {
                                        current += obj.getCurrentCount();
                                        required += obj.getRequiredCount();
                                    }
                                    final int finalCurrent = current;
                                    final int finalRequired = required;
                                    ctx.getSource().sendSuccess(() -> Component.literal(q.getTitle() + " [" + finalCurrent + "/" + finalRequired + "]"), false);
                                }
                            }
                        }
                    }
                    return 1;
                }));
        quest.then(Commands.literal("journal")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Map<String, QuestProgress> quests = QuestManager.getPlayerQuests((ServerLevel) player.level(), player.getUUID());
                    Set<String> completed = QuestManager.getCompletedQuests((ServerLevel) player.level(), player.getUUID());
                    CompoundTag meta = new CompoundTag();
                    for (QuestData q : QuestManager.getAll((ServerLevel) player.level()).values()) {
                        meta.putString(q.getId(), q.getTitle());
                    }
                    CompoundTag progressTag = new CompoundTag();
                    for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
                        progressTag.putString(entry.getKey(), entry.getValue().getStatus().name());
                    }
                    for (String id : completed) {
                        progressTag.putBoolean(id + "_completed", true);
                    }
                    IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.QUEST_JOURNAL, OpenGuiPacket.questJournalToTag(progressTag, meta)), player);
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
                                    r.setPos1(new Vec3(0, 0, 0));
                                    r.setPos2(new Vec3(5, 5, 5));
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
                                r.setPos1(new Vec3(player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ()));
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
                                r.setPos2(new Vec3(player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ()));
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

        LiteralArgumentBuilder<CommandSourceStack> event = Commands.literal("event");
        event.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("eventType", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    String typeStr = StringArgumentType.getString(ctx, "eventType");
                                    try {
                                        EventType type = EventType.valueOf(typeStr.toUpperCase());
                                        EventGraphData graph = new EventGraphData();
                                        graph.setId(id);
                                        graph.setName(id);
                                        ScriptNodeData start = new ScriptNodeData();
                                        start.setId("start");
                                        start.setType(ScriptNodeType.START);
                                        start.setParam("eventType", type.name());
                                        start.setParam("label", "Event");
                                        start.setX(200);
                                        start.setY(200);
                                        graph.addNode(start);
                                        graph.setStartNodeId("start");
                                        EventGraphManager.add((ServerLevel) player.level(), graph);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Event graph " + id + " created for " + type.name()), false);
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown event type: " + typeStr));
                                    }
                                    return 1;
                                }))));
        event.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Event Graphs ==="), false);
                    for (EventGraphData graph : EventGraphManager.getAll((ServerLevel) player.level()).values()) {
                        ScriptNodeData start = graph.getNode(graph.getStartNodeId());
                        String eventType = start != null ? start.getParam("eventType") : "UNKNOWN";
                        ctx.getSource().sendSuccess(() -> Component.literal(graph.getId() + ": " + graph.getName() + " [" + eventType + "] (" + graph.getNodes().size() + " nodes)"), false);
                    }
                    return 1;
                }));

        LiteralArgumentBuilder<CommandSourceStack> script = Commands.literal("script");
        script.then(Commands.argument("code", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String code = StringArgumentType.getString(ctx, "code");
                    ScriptEngine engine = ScriptEngine.getInstance();
                    if (!engine.isAvailable()) {
                        ctx.getSource().sendFailure(Component.literal("Script engine not available. Check logs for GraalJS init errors."));
                        return 0;
                    }
                    try {
                        Object result = engine.execute(code, player, (ServerLevel) player.level());
                        String msg = result == null ? "Executed (no return)" : "Result: " + result.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal("Script error: " + e.getMessage()));
                        return 0;
                    }
                }));
        script.then(Commands.literal("run")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            String code = ScriptFileManager.load((ServerLevel) player.level(), name);
                            if (code == null || code.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("Script '" + name + "' not found or empty. Create file: saves/<world>/iscript/scripts/" + name + ".js"));
                                return 0;
                            }
                            ScriptEngine engine = ScriptEngine.getInstance();
                            if (!engine.isAvailable()) {
                                ctx.getSource().sendFailure(Component.literal("Script engine not available"));
                                return 0;
                            }
                            try {
                                Object result = engine.execute(code, player, (ServerLevel) player.level());
                                String msg = result == null ? "Executed '" + name + "' (no return)" : "Result: " + result.toString();
                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                return 1;
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("Script '" + name + "' error: " + e.getMessage()));
                                return 0;
                            }
                        })));
        script.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    List<String> ids = ScriptFileManager.listScriptIds((ServerLevel) player.level());
                    if (ids.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No scripts found in saves/<world>/iscript/scripts/"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal("=== Scripts ==="), false);
                        for (String id : ids) {
                            ctx.getSource().sendSuccess(() -> Component.literal("- " + id), false);
                        }
                    }
                    return 1;
                }));

        LiteralArgumentBuilder<CommandSourceStack> scriptItem = Commands.literal("scriptitem");
        scriptItem.then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String scriptCode = StringArgumentType.getString(ctx, "script");
                            ItemStack stack = new ItemStack(ModItems.SCRIPT_ITEM.get());
                            CompoundTag tag = new CompoundTag();
                            tag.putString("Script", scriptCode);
                            stack.setTag(tag);
                            target.getInventory().add(stack);
                            ctx.getSource().sendSuccess(() -> Component.literal("Script item given to " + target.getName().getString()), false);
                            return 1;
                        })));

        LiteralArgumentBuilder<CommandSourceStack> playerData = Commands.literal("playerdata");
        playerData.then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String key = StringArgumentType.getString(ctx, "key");
                                    String value = StringArgumentType.getString(ctx, "value");
                                    player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                                        data.setString(key, value);
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal("Data set"), false);
                                    return 1;
                                }))));
        playerData.then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String key = StringArgumentType.getString(ctx, "key");
                            String value = player.getCapability(ModCapabilities.PLAYER_DATA)
                                    .map(data -> data.getString(key)).orElse("");
                            ctx.getSource().sendSuccess(() -> Component.literal(key + " = " + value), false);
                            return 1;
                        })));
        playerData.then(Commands.literal("faction")
                .then(Commands.literal("set")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String faction = StringArgumentType.getString(ctx, "faction");
                                    player.getCapability(ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                                        data.setFaction(faction);
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal("Faction set to " + faction), false);
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String faction = player.getCapability(ModCapabilities.PLAYER_DATA)
                                    .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
                            ctx.getSource().sendSuccess(() -> Component.literal("Faction: " + faction), false);
                            return 1;
                        })));

        LiteralArgumentBuilder<CommandSourceStack> worldData = Commands.literal("worlddata");
        worldData.then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String key = StringArgumentType.getString(ctx, "key");
                                    String value = StringArgumentType.getString(ctx, "value");
                                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                        WorldDataManager.setBool((ServerLevel) player.level(), key, Boolean.parseBoolean(value));
                                    } else {
                                        try {
                                            int intVal = Integer.parseInt(value);
                                            WorldDataManager.setInt((ServerLevel) player.level(), key, intVal);
                                        } catch (NumberFormatException e1) {
                                            try {
                                                double dblVal = Double.parseDouble(value);
                                                WorldDataManager.setDouble((ServerLevel) player.level(), key, dblVal);
                                            } catch (NumberFormatException e2) {
                                                WorldDataManager.setString((ServerLevel) player.level(), key, value);
                                            }
                                        }
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("World data set: " + key), false);
                                    return 1;
                                }))));
        worldData.then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String key = StringArgumentType.getString(ctx, "key");
                            String type = WorldDataManager.getType((ServerLevel) player.level(), key);
                            String result = switch (type) {
                                case "string" -> WorldDataManager.getString((ServerLevel) player.level(), key);
                                case "int" -> String.valueOf(WorldDataManager.getInt((ServerLevel) player.level(), key));
                                case "double" -> String.valueOf(WorldDataManager.getDouble((ServerLevel) player.level(), key));
                                case "bool" -> String.valueOf(WorldDataManager.getBool((ServerLevel) player.level(), key));
                                default -> "<not set>";
                            };
                            ctx.getSource().sendSuccess(() -> Component.literal(key + " = " + result + " (" + type + ")"), false);
                            return 1;
                        })));
        worldData.then(Commands.literal("remove")
                .then(Commands.argument("key", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String key = StringArgumentType.getString(ctx, "key");
                            WorldDataManager.remove((ServerLevel) player.level(), key);
                            ctx.getSource().sendSuccess(() -> Component.literal("Removed: " + key), false);
                            return 1;
                        })));
        worldData.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    java.util.Set<String> keys = WorldDataManager.getKeys((ServerLevel) player.level());
                    ctx.getSource().sendSuccess(() -> Component.literal("=== World Data ==="), false);
                    for (String key : keys) {
                        ctx.getSource().sendSuccess(() -> Component.literal("- " + key), false);
                    }
                    return 1;
                }));

        LiteralArgumentBuilder<CommandSourceStack> graph = Commands.literal("graph");
        graph.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    String name = StringArgumentType.getString(ctx, "name");
                                    DialogGraphData graphData = new DialogGraphData();
                                    graphData.setId(id);
                                    graphData.setName(name);
                                    DialogNodeData start = new DialogNodeData();
                                    start.setId("start");
                                    start.setTitle("Start");
                                    start.setText("Hello! This is the start node.");
                                    start.setX(100);
                                    start.setY(100);
                                    graphData.addNode(start);
                                    com.iscript.iscript.data.DialogGraphManager.add((ServerLevel) player.level(), graphData);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Graph " + id + " created with start node"), false);
                                    return 1;
                                }))));
        graph.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Dialog Graphs ==="), false);
                    for (DialogGraphData g : com.iscript.iscript.data.DialogGraphManager.getAll((ServerLevel) player.level()).values()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(g.getId() + ": " + g.getName() + " (" + g.getNodes().size() + " nodes)"), false);
                    }
                    return 1;
                }));
        graph.then(Commands.literal("addnode")
                .then(Commands.argument("graphId", StringArgumentType.word())
                        .then(Commands.argument("nodeId", StringArgumentType.word())
                                .then(Commands.argument("title", StringArgumentType.string())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String graphId = StringArgumentType.getString(ctx, "graphId");
                                            String nodeId = StringArgumentType.getString(ctx, "nodeId");
                                            String title = StringArgumentType.getString(ctx, "title");
                                            DialogGraphData graphData = com.iscript.iscript.data.DialogGraphManager.get((ServerLevel) player.level(), graphId);
                                            if (graphData == null) {
                                                ctx.getSource().sendFailure(Component.literal("Graph not found"));
                                                return 0;
                                            }
                                            DialogNodeData node = new DialogNodeData();
                                            node.setId(nodeId);
                                            node.setTitle(title);
                                            node.setText("New node text");
                                            node.setX(200);
                                            node.setY(200);
                                            graphData.addNode(node);
                                            com.iscript.iscript.data.DialogGraphManager.add((ServerLevel) player.level(), graphData);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Node " + nodeId + " added"), false);
                                            return 1;
                                        })))));
        graph.then(Commands.literal("edit")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            DialogGraphData graphData = com.iscript.iscript.data.DialogGraphManager.get((ServerLevel) player.level(), id);
                            if (graphData == null) {
                                ctx.getSource().sendFailure(Component.literal("Graph not found"));
                                return 0;
                            }
                            IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DASHBOARD, new CompoundTag()), player);
                            ctx.getSource().sendSuccess(() -> Component.literal("Opening graph editor"), false);
                            return 1;
                        })));
        graph.then(Commands.literal("connect")
                .then(Commands.argument("graphId", StringArgumentType.word())
                        .then(Commands.argument("from", StringArgumentType.word())
                                .then(Commands.argument("to", StringArgumentType.word())
                                        .then(Commands.argument("text", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    String graphId = StringArgumentType.getString(ctx, "graphId");
                                                    String from = StringArgumentType.getString(ctx, "from");
                                                    String to = StringArgumentType.getString(ctx, "to");
                                                    String text = StringArgumentType.getString(ctx, "text");
                                                    DialogGraphData graphData = com.iscript.iscript.data.DialogGraphManager.get((ServerLevel) player.level(), graphId);
                                                    if (graphData == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Graph not found"));
                                                        return 0;
                                                    }
                                                    DialogNodeData fromNode = graphData.getNode(from);
                                                    if (fromNode == null) {
                                                        ctx.getSource().sendFailure(Component.literal("From node not found"));
                                                        return 0;
                                                    }
                                                    DialogNodeData.NodeConnection conn = new DialogNodeData.NodeConnection();
                                                    conn.setOptionText(text);
                                                    conn.setTargetNodeId(to);
                                                    fromNode.getConnections().add(conn);
                                                    com.iscript.iscript.data.DialogGraphManager.add((ServerLevel) player.level(), graphData);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Connected " + from + " -> " + to), false);
                                                    return 1;
                                                }))))));
        graph.then(Commands.literal("play")
                .then(Commands.argument("graphId", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String graphId = StringArgumentType.getString(ctx, "graphId");
                            DialogData dialogData = com.iscript.iscript.data.DialogGraphManager.convertToDialogData((ServerLevel) player.level(), graphId, "start");
                            if (dialogData != null) {
                                DialogData filtered = new DialogData();
                                filtered.setId(dialogData.getId());
                                filtered.setTitle(dialogData.getTitle());
                                filtered.setText(dialogData.getText());
                                filtered.setPortrait(dialogData.getPortrait());
                                for (DialogData.DialogOption opt : dialogData.getAvailableOptions(player)) {
                                    filtered.getOptions().add(opt);
                                }
                                IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Playing graph " + graphId), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Graph or start node not found"));
                            }
                            return 1;
                        })));

        LiteralArgumentBuilder<CommandSourceStack> cutscene = Commands.literal("cutscene");
        cutscene.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            CutsceneData c = new CutsceneData();
                            c.setId(id);
                            c.setName("Cutscene " + id);
                            com.iscript.iscript.data.CutsceneManager.add((ServerLevel) player.level(), c);
                            ctx.getSource().sendSuccess(() -> Component.literal("Cutscene " + id + " created"), false);
                            return 1;
                        })));
        cutscene.then(Commands.literal("play")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            CutsceneData c = com.iscript.iscript.data.CutsceneManager.get((ServerLevel) player.level(), id);
                            if (c != null) {
                                CutscenePlayer.play(player, c);
                                ctx.getSource().sendSuccess(() -> Component.literal("Playing cutscene " + id), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Cutscene not found"));
                            }
                            return 1;
                        })));
        cutscene.then(Commands.literal("stop")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CutscenePlayer.stop(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Cutscene stopped"), false);
                    return 1;
                }));
        cutscene.then(Commands.literal("add")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("params", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String typeStr = StringArgumentType.getString(ctx, "type");
                                            String params = StringArgumentType.getString(ctx, "params");
                                            CutsceneData c = com.iscript.iscript.data.CutsceneManager.get((ServerLevel) player.level(), id);
                                            if (c == null) {
                                                ctx.getSource().sendFailure(Component.literal("Cutscene not found"));
                                                return 0;
                                            }
                                            try {
                                                CutsceneActionType type = CutsceneActionType.valueOf(typeStr.toUpperCase());
                                                CutsceneAction action = new CutsceneAction();
                                                action.setType(type);
                                                String[] parts = params.split(" ");
                                                switch (type) {
                                                    case CAMERA_IDLE, CAMERA_PATH, CAMERA_ORBIT, CAMERA_DOLLY -> {
                                                        action.setX(Double.parseDouble(parts[0]));
                                                        action.setY(Double.parseDouble(parts[1]));
                                                        action.setZ(Double.parseDouble(parts[2]));
                                                        action.setYaw(Float.parseFloat(parts[3]));
                                                        action.setPitch(Float.parseFloat(parts[4]));
                                                        action.setDuration(Integer.parseInt(parts[5]));
                                                    }
                                                    case CAMERA_FOLLOW -> {
                                                        action.setIntValue(Integer.parseInt(parts[0]));
                                                        action.setDuration(Integer.parseInt(parts[1]));
                                                    }
                                                    case CAMERA_LOOK -> {
                                                        action.setLookAtX(Double.parseDouble(parts[0]));
                                                        action.setLookAtY(Double.parseDouble(parts[1]));
                                                        action.setLookAtZ(Double.parseDouble(parts[2]));
                                                    }
                                                    case CAMERA_SHAKE -> {
                                                        action.setShakeTrauma(Float.parseFloat(parts[0]));
                                                        action.setShakeDecay(parts.length > 1 ? Float.parseFloat(parts[1]) : 1.5f);
                                                        action.setShakeMaxAngle(parts.length > 2 ? Float.parseFloat(parts[2]) : 5.0f);
                                                        action.setShakeMaxOffset(parts.length > 3 ? Float.parseFloat(parts[3]) : 0.3f);
                                                    }
                                                    case DELAY -> action.setDuration(Integer.parseInt(parts[0]));
                                                    case DIALOG, SOUND, SCRIPT, BLOCK -> action.setStringValue(params);
                                                    case NPC_MOVE -> {
                                                        action.setIntValue(Integer.parseInt(parts[0]));
                                                        action.setX(Double.parseDouble(parts[1]));
                                                        action.setY(Double.parseDouble(parts[2]));
                                                        action.setZ(Double.parseDouble(parts[3]));
                                                    }
                                                    case NPC_ANIMATION -> {
                                                        action.setIntValue(Integer.parseInt(parts[0]));
                                                        action.setStringValue(parts[1]);
                                                    }
                                                }
                                                c.getActions().add(action);
                                                com.iscript.iscript.data.CutsceneManager.add((ServerLevel) player.level(), c);
                                                ctx.getSource().sendSuccess(() -> Component.literal("Action added"), false);
                                            } catch (Exception e) {
                                                ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                            return 1;
                                        })))));
        cutscene.then(Commands.literal("spline")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("tension", DoubleArgumentType.doubleArg(0, 1))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            double tension = DoubleArgumentType.getDouble(ctx, "tension");
                                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                            CutsceneData c = com.iscript.iscript.data.CutsceneManager.get((ServerLevel) player.level(), id);
                                            if (c == null) {
                                                ctx.getSource().sendFailure(Component.literal("Cutscene not found"));
                                                return 0;
                                            }
                                            CutsceneAction action = new CutsceneAction();
                                            action.setType(CutsceneActionType.CAMERA_PATH);
                                            action.setX(player.getX());
                                            action.setY(player.getY());
                                            action.setZ(player.getZ());
                                            action.setYaw(player.getYRot());
                                            action.setPitch(player.getXRot());
                                            action.setDuration(duration);
                                            action.setPathType("CATMULL_ROM");
                                            action.addSplinePoint(
                                                    new Vec3(player.getX(), player.getY(), player.getZ()),
                                                    player.getYRot(), player.getXRot());
                                            c.getActions().add(action);
                                            com.iscript.iscript.data.CutsceneManager.add((ServerLevel) player.level(), c);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Spline point recorded at " + String.format("%.1f", player.getX()) + ", " +
                                                            String.format("%.1f", player.getY()) + ", " +
                                                            String.format("%.1f", player.getZ()) + " tension=" + tension + " duration=" + duration), false);
                                            return 1;
                                        })))));
        cutscene.then(Commands.literal("record")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                    CutsceneData c = com.iscript.iscript.data.CutsceneManager.get((ServerLevel) player.level(), id);
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal("Cutscene not found"));
                                        return 0;
                                    }
                                    CutsceneAction action = new CutsceneAction();
                                    action.setType(CutsceneActionType.CAMERA_IDLE);
                                    action.setX(player.getX());
                                    action.setY(player.getY());
                                    action.setZ(player.getZ());
                                    action.setYaw(player.getYRot());
                                    action.setPitch(player.getXRot());
                                    action.setDuration(duration);
                                    c.getActions().add(action);
                                    com.iscript.iscript.data.CutsceneManager.add((ServerLevel) player.level(), c);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Recorded camera at " + String.format("%.1f", player.getX()) + ", " +
                                                    String.format("%.1f", player.getY()) + ", " +
                                                    String.format("%.1f", player.getZ()) + " for " + duration + " ticks"), false);
                                    return 1;
                                }))));
        cutscene.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Cutscenes ==="), false);
                    for (CutsceneData c : com.iscript.iscript.data.CutsceneManager.getAll((ServerLevel) player.level()).values()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(c.getId() + ": " + c.getName() + " (" + c.getActions().size() + " actions)"), false);
                    }
                    return 1;
                }));

        LiteralArgumentBuilder<CommandSourceStack> npc = Commands.literal("npc");
        npc.then(Commands.literal("state")
                .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .then(Commands.argument("state", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int entityId = IntegerArgumentType.getInteger(ctx, "entityId");
                                    String stateStr = StringArgumentType.getString(ctx, "state");
                                    if (player.level().getEntity(entityId) instanceof com.iscript.iscript.entity.IScriptNPCEntity npcEntity) {
                                        try {
                                            NPCState state = NPCState.valueOf(stateStr.toUpperCase());
                                            npcEntity.setCurrentState(state);
                                            ctx.getSource().sendSuccess(() -> Component.literal("NPC state set to " + state.name()), false);
                                        } catch (IllegalArgumentException e) {
                                            ctx.getSource().sendFailure(Component.literal("Invalid state: " + stateStr));
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("NPC not found"));
                                    }
                                    return 1;
                                }))));
        npc.then(Commands.literal("follow")
                .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int entityId = IntegerArgumentType.getInteger(ctx, "entityId");
                            if (player.level().getEntity(entityId) instanceof com.iscript.iscript.entity.IScriptNPCEntity npcEntity) {
                                npcEntity.setFollowTarget(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("NPC is now following you"), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("NPC not found"));
                            }
                            return 1;
                        })));
        npc.then(Commands.literal("stop")
                .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int entityId = IntegerArgumentType.getInteger(ctx, "entityId");
                            if (player.level().getEntity(entityId) instanceof com.iscript.iscript.entity.IScriptNPCEntity npcEntity) {
                                npcEntity.setFollowTarget(null);
                                npcEntity.setCurrentState(NPCState.IDLE);
                                ctx.getSource().sendSuccess(() -> Component.literal("NPC stopped following"), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("NPC not found"));
                            }
                            return 1;
                        })));
        npc.then(Commands.literal("animate")
                .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .then(Commands.argument("animation", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int entityId = IntegerArgumentType.getInteger(ctx, "entityId");
                                    String anim = StringArgumentType.getString(ctx, "animation");
                                    if (player.level().getEntity(entityId) instanceof com.iscript.iscript.entity.IScriptNPCEntity npcEntity) {
                                        npcEntity.playAnimation(anim);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Animation set: " + anim), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("NPC not found"));
                                    }
                                    return 1;
                                }))));
        LiteralArgumentBuilder<CommandSourceStack> scriptGraph = Commands.literal("scriptgraph");
        scriptGraph.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            ScriptGraphData sg = new ScriptGraphData();
                            sg.setId(id);
                            sg.setName(id);
                            ScriptNodeData start = new ScriptNodeData();
                            start.setId("start");
                            start.setType(ScriptNodeType.START);
                            start.setX(100);
                            start.setY(100);
                            sg.addNode(start);
                            ScriptGraphManager.add((ServerLevel) player.level(), sg, "");
                            ctx.getSource().sendSuccess(() -> Component.literal("Script graph " + id + " created"), false);
                            return 1;
                        })));
        scriptGraph.then(Commands.literal("edit")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            ScriptGraphData sg = ScriptGraphManager.get((ServerLevel) player.level(), id);
                            if (sg == null) {
                                ctx.getSource().sendFailure(Component.literal("Script graph not found"));
                                return 0;
                            }
                            IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DASHBOARD, new CompoundTag()), player);
                            ctx.getSource().sendSuccess(() -> Component.literal("Opening script graph editor"), false);
                            return 1;
                        })));
        scriptGraph.then(Commands.literal("run")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            ScriptGraphData sg = ScriptGraphManager.get((ServerLevel) player.level(), id);
                            if (sg == null) {
                                ctx.getSource().sendFailure(Component.literal("Script graph not found"));
                                return 0;
                            }
                            ScriptGraphExecutor executor = new ScriptGraphExecutor(sg, player, (ServerLevel) player.level());
                            executor.start();
                            ctx.getSource().sendSuccess(() -> Component.literal("Running script graph " + id), false);
                            return 1;
                        })));
        scriptGraph.then(Commands.literal("stop")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ScriptGraphExecutor.stopFor(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Script graph stopped"), false);
                    return 1;
                }));
        scriptGraph.then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Script Graphs ==="), false);
                    for (ScriptGraphData g : ScriptGraphManager.getAll((ServerLevel) player.level()).values()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(g.getId() + ": " + g.getName() + " (" + g.getNodes().size() + " nodes)"), false);
                    }
                    return 1;
                }));

        dispatcher.register(Commands.literal("iscript")
                .then(dialog)
                .then(quest)
                .then(region)
                .then(event)
                .then(script)
                .then(scriptItem)
                .then(playerData)
                .then(worldData)
                .then(cutscene)
                .then(graph)
                .then(scriptGraph)
                .then(npc));
    }
}