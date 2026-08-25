package com.example.teammarker;

import com.example.teammarker.command.TeamMarkerCommand;
import com.example.teammarker.config.TeamMarkerConfigManager;
import com.example.teammarker.screen.TeamPickerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TeamMarkerClient implements ClientModInitializer {

    public static final String MOD_ID = "teammarker";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding addTargetKey;

    private static KeyBinding removeTargetKey;

    private static KeyBinding addNearKey;

    private static final double ADD_NEAR_RADIUS = 4.0;

    @Override
    public void onInitializeClient() {
        TeamMarkerConfigManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TeamMarkerCommand.register(dispatcher);
        });

        registerKeybindings();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        LOGGER.info("[TeamMarker] 客户端模组已加载。配置文件位置: config/teammarker.json");
        LOGGER.info("[TeamMarker] 快捷键：K=加准星玩家, L=删准星玩家, M=范围添加(4格)（均可在控制设置中更改）");
    }

    private void registerKeybindings() {
        try {
            addTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.teammarker.add_target",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "category.teammarker"
            ));
            removeTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.teammarker.remove_target",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_L,
                    "category.teammarker"
            ));
            addNearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.teammarker.add_near",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_M,
                    "category.teammarker"
            ));
        } catch (Throwable t) {
            LOGGER.error("[TeamMarker] 注册快捷键失败", t);
        }
    }

    private void onClientTick(MinecraftClient client) {
        try {
            if (addTargetKey != null) {
                while (addTargetKey.wasPressed()) {
                    handleAddTargetKey(client);
                }
            }
            if (removeTargetKey != null) {
                while (removeTargetKey.wasPressed()) {
                    handleRemoveTargetKey(client);
                }
            }
            if (addNearKey != null) {
                while (addNearKey.wasPressed()) {
                    handleAddNearKey(client);
                }
            }
        } catch (Throwable t) {
        }
    }

    private void handleAddTargetKey(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        HitResult target = client.crosshairTarget;
        if (!(target instanceof EntityHitResult entityHit)) {
            sendLocalMessage(client, Text.literal("准星未指向任何实体").formatted(Formatting.YELLOW));
            return;
        }

        Entity entity = entityHit.getEntity();
        if (!(entity instanceof PlayerEntity targetPlayer)) {
            sendLocalMessage(client, Text.literal("准星指向的不是玩家").formatted(Formatting.YELLOW));
            return;
        }

        if (targetPlayer == client.player) {
            sendLocalMessage(client, Text.literal("不能把自己加进队友名单").formatted(Formatting.YELLOW));
            return;
        }

        String name;
        try {
            name = targetPlayer.getName().getString();
        } catch (Throwable t) {
            sendLocalMessage(client, Text.literal("无法读取玩家名").formatted(Formatting.RED));
            return;
        }
        if (name == null || name.isEmpty()) {
            sendLocalMessage(client, Text.literal("玩家名为空").formatted(Formatting.RED));
            return;
        }

        try {
            client.setScreen(new TeamPickerScreen(List.of(name)));
        } catch (Throwable t) {
            boolean added = TeamMarkerConfigManager.addPlayer(name);
            if (added) {
                sendLocalMessage(client, Text.literal("已添加队友: " + name).formatted(Formatting.GREEN));
            } else {
                sendLocalMessage(client, Text.literal(name + " 已在名单中").formatted(Formatting.YELLOW));
            }
        }
    }

    private void handleRemoveTargetKey(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        HitResult target = client.crosshairTarget;
        if (!(target instanceof EntityHitResult entityHit)) {
            sendLocalMessage(client, Text.literal("准星未指向任何实体").formatted(Formatting.YELLOW));
            return;
        }
        Entity entity = entityHit.getEntity();
        if (!(entity instanceof PlayerEntity targetPlayer)) {
            sendLocalMessage(client, Text.literal("准星指向的不是玩家").formatted(Formatting.YELLOW));
            return;
        }
        String name;
        try {
            name = targetPlayer.getName().getString();
        } catch (Throwable t) {
            sendLocalMessage(client, Text.literal("无法读取玩家名").formatted(Formatting.RED));
            return;
        }
        if (name == null || name.isEmpty()) {
            sendLocalMessage(client, Text.literal("玩家名为空").formatted(Formatting.RED));
            return;
        }
        boolean removed = TeamMarkerConfigManager.removePlayer(name);
        if (removed) {
            sendLocalMessage(client, Text.literal("已移除队友: " + name).formatted(Formatting.GREEN));
        } else {
            sendLocalMessage(client, Text.literal(name + " 不在名单中").formatted(Formatting.YELLOW));
        }
    }

    private void handleAddNearKey(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        try {
            double radiusSq = ADD_NEAR_RADIUS * ADD_NEAR_RADIUS;
            List<String> names = new ArrayList<>();
            for (PlayerEntity other : client.world.getPlayers()) {
                if (other == client.player) continue;
                try {
                    if (other.squaredDistanceTo(client.player) > radiusSq) continue;
                } catch (Throwable ignored) {
                    continue;
                }
                String name;
                try {
                    name = other.getName().getString();
                } catch (Throwable ignored) {
                    continue;
                }
                if (name == null || name.isEmpty()) continue;
                names.add(name);
            }
            if (names.isEmpty()) {
                sendLocalMessage(client, Text.literal("4 格范围内没有其他玩家").formatted(Formatting.YELLOW));
                return;
            }
            try {
                client.setScreen(new TeamPickerScreen(names));
            } catch (Throwable t) {
                sendLocalMessage(client, Text.literal("打开选队界面失败").formatted(Formatting.RED));
            }
        } catch (Throwable t) {
            sendLocalMessage(client, Text.literal("范围添加失败").formatted(Formatting.RED));
        }
    }

    private static void sendLocalMessage(MinecraftClient client, Text message) {
        try {
            if (client.player != null) {
                client.player.sendMessage(message, false);
            }
        } catch (Throwable ignored) {
        }
    }
}
