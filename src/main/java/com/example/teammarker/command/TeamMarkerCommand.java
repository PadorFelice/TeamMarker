package com.example.teammarker.command;

import com.example.teammarker.config.TeamMarkerConfig;
import com.example.teammarker.config.TeamMarkerConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TeamMarkerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("teammarker")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                .executes(TeamMarkerCommand::addPlayerDefault)
                                .then(ClientCommandManager.argument("team", StringArgumentType.string())
                                        .executes(TeamMarkerCommand::addPlayerToTeam))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                .executes(TeamMarkerCommand::removePlayer)))
                .then(ClientCommandManager.literal("list")
                        .executes(TeamMarkerCommand::listAll))
                .then(ClientCommandManager.literal("toggle")
                        .executes(TeamMarkerCommand::toggle))
                .then(ClientCommandManager.literal("prefix")
                        .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(TeamMarkerCommand::setPrefix)))
                .then(ClientCommandManager.literal("team")
                        .then(ClientCommandManager.literal("create")
                                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("prefix", StringArgumentType.string())
                                                .then(ClientCommandManager.argument("color", StringArgumentType.string())
                                                        .executes(TeamMarkerCommand::createTeam)))))
                        .then(ClientCommandManager.literal("delete")
                                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                        .executes(TeamMarkerCommand::deleteTeam)))
                        .then(ClientCommandManager.literal("list")
                                .executes(TeamMarkerCommand::listTeams))
                )
        );
    }

    private static int addPlayerDefault(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        boolean added = TeamMarkerConfigManager.addPlayer(name);
        if (added) {
            sendFeedback(ctx, Text.literal("已添加队友: " + name + " (无队伍)").formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("玩家 " + name + " 已在列表中或名称无效").formatted(Formatting.YELLOW));
        }
        return 1;
    }

    private static int addPlayerToTeam(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        String teamName = StringArgumentType.getString(ctx, "team");
        boolean added = TeamMarkerConfigManager.addPlayerToTeam(name, teamName);
        if (added) {
            sendFeedback(ctx, Text.literal("已将 " + name + " 加入队伍: " + teamName).formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("加入失败：队伍不存在或玩家已在队伍中").formatted(Formatting.YELLOW));
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

    private static int listAll(CommandContext<FabricClientCommandSource> ctx) {
        List<String> defaultList = TeamMarkerConfigManager.getPlayerList();
        List<TeamMarkerConfig.Team> teams = TeamMarkerConfigManager.getTeams();
        if (defaultList.isEmpty() && teams.isEmpty()) {
            sendFeedback(ctx, Text.literal("队友列表为空").formatted(Formatting.YELLOW));
            return 1;
        }
        sendFeedback(ctx, Text.literal("=== 队友列表 ===").formatted(Formatting.AQUA));
        if (!defaultList.isEmpty()) {
            sendFeedback(ctx, Text.literal("[无队伍] (" + defaultList.size() + "):").formatted(Formatting.GRAY));
            for (String name : defaultList) {
                sendFeedback(ctx, Text.literal("  - " + name).formatted(Formatting.WHITE));
            }
        }
        for (TeamMarkerConfig.Team t : teams) {
            if (t == null) continue;
            Formatting tf = safeFormatting(t.color);
            MutableText header = Text.literal("[" + t.name + "] prefix=" + t.prefix + " color=" + t.color + " (" + (t.players == null ? 0 : t.players.size()) + "):");
            if (tf != null) header = header.formatted(tf);
            sendFeedback(ctx, header);
            if (t.players != null) {
                for (String p : t.players) {
                    MutableText line = Text.literal("  - " + p);
                    if (tf != null) line = line.formatted(tf);
                    sendFeedback(ctx, line);
                }
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
        sendFeedback(ctx, Text.literal("前缀已修改为: " + text + " (仅影响无队伍标记)").formatted(Formatting.GREEN));
        return 1;
    }

    private static int createTeam(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        String prefix = StringArgumentType.getString(ctx, "prefix");
        String color = StringArgumentType.getString(ctx, "color");
        boolean created = TeamMarkerConfigManager.createTeam(name, prefix, color);
        if (created) {
            sendFeedback(ctx, Text.literal("已创建队伍: " + name + " (prefix=" + prefix + ", color=" + color + ")").formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("创建失败：队伍已存在或参数无效").formatted(Formatting.YELLOW));
        }
        return 1;
    }

    private static int deleteTeam(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean deleted = TeamMarkerConfigManager.deleteTeam(name);
        if (deleted) {
            sendFeedback(ctx, Text.literal("已删除队伍: " + name).formatted(Formatting.GREEN));
        } else {
            sendFeedback(ctx, Text.literal("删除失败：队伍不存在").formatted(Formatting.YELLOW));
        }
        return 1;
    }

    private static int listTeams(CommandContext<FabricClientCommandSource> ctx) {
        List<TeamMarkerConfig.Team> teams = TeamMarkerConfigManager.getTeams();
        if (teams.isEmpty()) {
            sendFeedback(ctx, Text.literal("尚未创建任何队伍").formatted(Formatting.YELLOW));
            return 1;
        }
        sendFeedback(ctx, Text.literal("=== 队伍列表 (" + teams.size() + ") ===").formatted(Formatting.AQUA));
        for (TeamMarkerConfig.Team t : teams) {
            if (t == null) continue;
            Formatting tf = safeFormatting(t.color);
            MutableText line = Text.literal("- " + t.name + " | prefix=" + t.prefix + " | color=" + t.color + " | 成员=" + (t.players == null ? 0 : t.players.size()));
            if (tf != null) line = line.formatted(tf);
            sendFeedback(ctx, line);
        }
        return 1;
    }

    private static void sendFeedback(CommandContext<FabricClientCommandSource> ctx, Text message) {
        try {
            ctx.getSource().sendFeedback(message);
        } catch (Throwable t) {
        }
    }

    private static Formatting safeFormatting(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return null;
        try {
            char c = colorCode.charAt(0);
            switch (c) {
                case '0': return Formatting.BLACK;
                case '1': return Formatting.DARK_BLUE;
                case '2': return Formatting.DARK_GREEN;
                case '3': return Formatting.DARK_AQUA;
                case '4': return Formatting.DARK_RED;
                case '5': return Formatting.DARK_PURPLE;
                case '6': return Formatting.GOLD;
                case '7': return Formatting.GRAY;
                case '8': return Formatting.DARK_GRAY;
                case '9': return Formatting.BLUE;
                case 'a': return Formatting.GREEN;
                case 'b': return Formatting.AQUA;
                case 'c': return Formatting.RED;
                case 'd': return Formatting.LIGHT_PURPLE;
                case 'e': return Formatting.YELLOW;
                case 'f': return Formatting.WHITE;
                default: return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
