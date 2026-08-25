package com.example.teammarker.screen;

import com.example.teammarker.TeamMarkerClient;
import com.example.teammarker.config.TeamMarkerConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateTeamScreen extends Screen {

    private final List<String> targetNames;
    private TextFieldWidget nameField;
    private TextFieldWidget prefixField;
    private String selectedColor = "e";

    public CreateTeamScreen(List<String> targets) {
        super(Text.literal("创建新队伍"));
        this.targetNames = targets == null ? Collections.emptyList() : new ArrayList<>(targets);
    }

    @Override
    protected void init() {
        try {
            int fieldW = 200;
            int fieldH = 20;
            int x = (this.width - fieldW) / 2;
            int y = 50;

            nameField = new TextFieldWidget(this.textRenderer, fieldW, fieldH, Text.literal("队伍名"));
            nameField.setMaxLength(20);
            nameField.setX(x);
            nameField.setY(y);
            this.addDrawableChild(nameField);
            y += 30;

            prefixField = new TextFieldWidget(this.textRenderer, fieldW, fieldH, Text.literal("前缀"));
            prefixField.setMaxLength(20);
            prefixField.setX(x);
            prefixField.setY(y);
            this.addDrawableChild(prefixField);
            y += 30;

            int presetW = (fieldW - 8) / 2;
            try {
                ButtonWidget btnFriend = ButtonWidget.builder(
                        Text.literal("队友预设").formatted(Formatting.GREEN),
                        b -> {
                            try {
                                if (nameField != null) nameField.setText("队友");
                                if (prefixField != null) prefixField.setText("[友]");
                                selectedColor = "a";
                            } catch (Throwable ignored) {
                            }
                        }
                ).dimensions(x, y, presetW, fieldH).build();
                this.addDrawableChild(btnFriend);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建队友预设按钮失败", t);
            }
            try {
                ButtonWidget btnEnemy = ButtonWidget.builder(
                        Text.literal("敌人预设").formatted(Formatting.RED),
                        b -> {
                            try {
                                if (nameField != null) nameField.setText("敌人");
                                if (prefixField != null) prefixField.setText("[敌]");
                                selectedColor = "c";
                            } catch (Throwable ignored) {
                            }
                        }
                ).dimensions(x + presetW + 8, y, presetW, fieldH).build();
                this.addDrawableChild(btnEnemy);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建敌人预设按钮失败", t);
            }
            y += 30;

            String[] codes = {"c", "6", "e", "a", "b", "9", "d", "f"};
            String[] labels = {"红", "金", "黄", "绿", "青", "蓝", "粉", "白"};
            int btnSize = 24;
            int gap = 4;
            int totalW = btnSize * codes.length + gap * (codes.length - 1);
            int startX = (this.width - totalW) / 2;
            for (int i = 0; i < codes.length; i++) {
                final String code = codes[i];
                String label = labels[i];
                Formatting fmt = safeFormatting(code);
                MutableText btnText = Text.literal(label);
                if (fmt != null) btnText = btnText.formatted(fmt);
                try {
                    ButtonWidget cb = ButtonWidget.builder(btnText, b -> {
                        selectedColor = code;
                    }).dimensions(startX + i * (btnSize + gap), y, btnSize, btnSize).build();
                    this.addDrawableChild(cb);
                } catch (Throwable t) {
                    TeamMarkerClient.LOGGER.error("[TeamMarker] 创建颜色按钮失败: " + code, t);
                }
            }
            y += btnSize + 20;

            try {
                ButtonWidget createBtn = ButtonWidget.builder(
                        Text.literal("创建并加入队伍"),
                        b -> {
                            try {
                                String name = nameField.getText();
                                String prefix = prefixField.getText();
                                if (name == null || name.isBlank()) {
                                    if (this.client != null && this.client.player != null) {
                                        this.client.player.sendMessage(Text.literal("队伍名不能为空").formatted(Formatting.RED), false);
                                    }
                                    return;
                                }
                                if (prefix == null || prefix.isBlank()) {
                                    prefix = "[" + name + "]";
                                }
                                boolean ok = TeamMarkerConfigManager.createTeam(name, prefix, selectedColor);
                                if (ok) {
                                    int added = 0;
                                    for (String t : targetNames) {
                                        try {
                                            if (TeamMarkerConfigManager.addPlayerToTeam(t, name)) added++;
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    if (this.client != null && this.client.player != null) {
                                        this.client.player.sendMessage(Text.literal("已创建队伍 " + name + " 并加入 " + added + " 名玩家").formatted(Formatting.GREEN), false);
                                    }
                                    if (this.client != null) this.client.setScreen(null);
                                } else {
                                    if (this.client != null && this.client.player != null) {
                                        this.client.player.sendMessage(Text.literal("创建失败：队伍已存在").formatted(Formatting.RED), false);
                                    }
                                }
                            } catch (Throwable t) {
                                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建队伍按钮回调异常", t);
                            }
                        }
                ).dimensions(x, y, fieldW, fieldH).build();
                this.addDrawableChild(createBtn);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建提交按钮失败", t);
            }
            y += 24;

            try {
                ButtonWidget cancelBtn = ButtonWidget.builder(
                        Text.literal("取消"),
                        b -> {
                            if (this.client != null) this.client.setScreen(new TeamPickerScreen(targetNames));
                        }
                ).dimensions(x, y, fieldW, fieldH).build();
                this.addDrawableChild(cancelBtn);
            } catch (Throwable t) {
                TeamMarkerClient.LOGGER.error("[TeamMarker] 创建取消按钮失败", t);
            }

            if (this.client != null) {
                this.setInitialFocus(nameField);
            }
        } catch (Throwable t) {
            TeamMarkerClient.LOGGER.error("[TeamMarker] CreateTeamScreen init 失败", t);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("创建新队伍"),
                this.width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("队伍名"),
                (this.width - 200) / 2 - 60, 50 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("前缀"),
                (this.width - 200) / 2 - 60, 80 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("预设"),
                (this.width - 200) / 2 - 60, 110 + 6, 0xFFFFFF);
        Formatting selFmt = safeFormatting(selectedColor);
        MutableText sel = Text.literal("当前颜色: " + selectedColor);
        if (selFmt != null) sel = sel.formatted(selFmt);
        context.drawCenteredTextWithShadow(this.textRenderer, sel,
                this.width / 2, 140 + 30, 0xFFFFFF);
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
