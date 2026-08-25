package com.example.teammarker.screen;

import com.example.teammarker.TeamMarkerClient;
import com.example.teammarker.config.TeamMarkerConfig;
import com.example.teammarker.config.TeamMarkerConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamPickerScreen extends Screen {

    private final List<String> targetNames;

    public TeamPickerScreen(List<String> targets) {
        super(Text.literal("选择队伍"));
        this.targetNames = targets == null ? Collections.emptyList() : new ArrayList<>(targets);
    }

    private void assignToTeam(String teamName) {
        int added = 0;
        int failed = 0;
        for (String name : targetNames) {
            try {
                if (TeamMarkerConfigManager.addPlayerToTeam(name, teamName)) {
                    added++;
                } else {
                    failed++;
                }
            } catch (Throwable t) {
                failed++;
                TeamMarkerClient.LOGGER.error("[TeamMarker] 批量加入队伍异常: " + name, t);
            }
        }
        if (this.client != null && this.client.player != null) {
            MutableText msg = Text.literal("已将 " + added + " 名玩家加入 " + teamName);
            if (failed > 0) msg = msg.append(Text.literal(" (" + failed + " 失败)").formatted(Formatting.YELLOW));
            this.client.player.sendMessage(msg.formatted(Formatting.GREEN), false);
        }
        if (this.client != null) this.client.setScreen(null);
    }

    private void assignToDefault() {
        int added = 0;
        int skipped = 0;
        for (String name : targetNames) {
            try {
                if (TeamMarkerConfigManager.addPlayer(name)) added++;
                else skipped++;
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 批量加入无队伍异常: " + name, t);
            }
        }
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal(
                    "已添加 " + added + " 名玩家为无队伍队友 (已存在 " + skipped + ")"
            ).formatted(Formatting.GREEN), false);
        }
        if (this.client != null) this.client.setScreen(null);
    }

    @Override
    protected void init() {
        try {
            List<TeamMarkerConfig.Team> teams = TeamMarkerConfigManager.getTeams();
            int joinW = 200;
            int delW = 50;
            int gap = 4;
            int btnH = 20;
            int rowH = 24;
            int totalW = joinW + gap + delW;
            int startX = (this.width - totalW) / 2;
            int y = 50;

            int idx = 1;
            for (TeamMarkerConfig.Team team : teams) {
                if (team == null) continue;
                final String teamName = team.name;
                int memberCount = team.players == null ? 0 : team.players.size();
                Formatting tf = safeFormatting(team.color);
                MutableText label = Text.literal(idx + ". " + team.name + " " + team.prefix + " (" + memberCount + "人)");
                if (tf != null) label = label.formatted(tf);

                try {
                    ButtonWidget joinBtn = ButtonWidget.builder(label, b -> assignToTeam(teamName))
                            .dimensions(startX, y, joinW, btnH).build();
                    this.addDrawableChild(joinBtn);
                } catch (Throwable t) {
                    TeamMarkerClient.LOGGER.error("[TeamMarker] 创建加入按钮失败: " + teamName, t);
                }

                try {
                    ButtonWidget delBtn = ButtonWidget.builder(
                            Text.literal("删除").formatted(Formatting.RED),
                            b -> {
                                try {
                                    boolean ok = TeamMarkerConfigManager.deleteTeam(teamName);
                                    if (ok && this.client != null && this.client.player != null) {
                                        this.client.player.sendMessage(Text.literal("已删除队伍: " + teamName).formatted(Formatting.GREEN), false);
                                    }
                                } catch (Throwable t) {
                                    TeamMarkerClient.LOGGER.error("[TeamMarker] 删除队伍按钮回调异常", t);
                                }
                                this.refresh();
                            }
                    ).dimensions(startX + joinW + gap, y, delW, btnH).build();
                    this.addDrawableChild(delBtn);
                } catch (Throwable t) {
                    TeamMarkerClient.LOGGER.error("[TeamMarker] 创建删除按钮失败: " + teamName, t);
                }

                y += rowH;
                idx++;
                if (y > this.height - 80) break;
            }

            try {
                ButtonWidget btnDefault = ButtonWidget.builder(
                        Text.literal("加入无队伍（基础前缀）"),
                        b -> assignToDefault()
                ).dimensions(startX, y, totalW, btnH).build();
                this.addDrawableChild(btnDefault);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建无队伍按钮失败", t);
            }
            y += rowH;

            try {
                ButtonWidget btnCreate = ButtonWidget.builder(
                        Text.literal("+ 创建新队伍"),
                        b -> {
                            if (this.client != null) this.client.setScreen(new CreateTeamScreen(targetNames));
                        }
                ).dimensions(startX, y, totalW, btnH).build();
                this.addDrawableChild(btnCreate);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建新队伍按钮失败", t);
            }
            y += rowH;

            try {
                ButtonWidget btnCancel = ButtonWidget.builder(
                        Text.literal("取消"),
                        b -> {
                            if (this.client != null) this.client.setScreen(null);
                        }
                ).dimensions(startX, y, totalW, btnH).build();
                this.addDrawableChild(btnCancel);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建取消按钮失败", t);
            }

            TeamMarkerClient.LOGGER.info("[TeamMarker] TeamPickerScreen init: 队伍数=" + teams.size() + " 目标数=" + targetNames.size());
        } catch (Throwable t) {
            TeamMarkerClient.LOGGER.error("[TeamMarker] TeamPickerScreen init 失败", t);
        }
    }

    private void refresh() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        String title;
        if (targetNames.size() == 1) {
            title = "为 " + targetNames.get(0) + " 选择队伍";
        } else if (targetNames.size() > 1) {
            title = "为 " + targetNames.size() + " 名玩家选择队伍";
        } else {
            title = "选择队伍";
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(title),
                this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static Formatting safeFormatting(String code) {
        if (code == null || code.isEmpty()) return null;
        try {
            char c = code.charAt(0);
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
