package com.example.teammarker.command;

import com.example.teammarker.config.TeamMarkerConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TeamMarkerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("teammarker")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                .executes(TeamMarkerCommand::addPlayer)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                .executes(TeamMarkerCommand::removePlayer)))
                .then(ClientCommandManager.literal("list")
                        .executes(TeamMarkerCommand::listPlayers))
                .then(ClientCommandManager.literal("toggle")
                        .executes(TeamMarkerCommand::toggle))
                .then(ClientCommandManager.literal("prefix")
                        .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(TeamMarkerCommand::setPrefix)))
        );
    }

    private static int addPlayer(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        boolean added = TeamMarkerConfigManager.addPlayer(name);
        if (added) {
            sendFeedback(ctx, Text.literal("已添加队友: " + name).formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("玩家 " + name + " 已在列表中或名称无效").formatted(Formatting.YELLOW));
        }
        return 1;
    }

    private static int removePlayer(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        boolean removed = TeamMarkerConfigManager.removePlayer(name);
        if (removed) {
            sendFeedback(ctx, Text.literal("已移除队友: " + name).formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("玩家 " + name + " 不在列表中").formatted(Formatting.YELLOW));
        }
        return 1;
    }

    private static int listPlayers(CommandContext<FabricClientCommandSource> ctx) {
        List<String> list = TeamMarkerConfigManager.getPlayerList();
        if (list.isEmpty()) {
            sendFeedback(ctx, Text.literal("队友列表为空").formatted(Formatting.YELLOW));
        } else {
            sendFeedback(ctx, Text.literal("当前队友列表 (" + list.size() + "):").formatted(Formatting.AQUA));
            for (String name : list) {
                sendFeedback(ctx, Text.literal(" - " + name).formatted(Formatting.WHITE));
            }
        }
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        TeamMarkerConfigManager.toggle();
        boolean state = TeamMarkerConfigManager.isEnabled();
        sendFeedback(ctx, Text.literal("TeamMarker 标识: " + (state ? "开启" : "关闭"))
                .formatted(state ? Formatting.GREEN : Formatting.RED));
        return 1;
    }

    private static int setPrefix(CommandContext<FabricClientCommandSource> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        TeamMarkerConfigManager.setPrefix(text);
        sendFeedback(ctx, Text.literal("前缀已修改为: " + text).formatted(Formatting.GREEN));
        return 1;
    }

    private static void sendFeedback(CommandContext<FabricClientCommandSource> ctx, Text message) {
        try {
            ctx.getSource().sendFeedback(message);
        } catch (Throwable t) {
        }
    }
}
