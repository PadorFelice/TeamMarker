package com.example.teammarker.mixin;

import com.example.teammarker.config.TeamMarkerConfigManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    private static final ThreadLocal<EntityRenderState> TEAMMARKER_CURRENT_STATE = new ThreadLocal<>();

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
    private void teammarker$onRenderLabelHead(EntityRenderState state, Text text, MatrixStack matrices,
                                                VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        try {
            TEAMMARKER_CURRENT_STATE.set(state);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"))
    private void teammarker$onRenderLabelReturn(EntityRenderState state, Text text, MatrixStack matrices,
                                                 VertexConsumerProvider vertexConsumers, int light,
                                                 CallbackInfo ci) {
        try {
            TEAMMARKER_CURRENT_STATE.remove();
        } catch (Throwable ignored) {
        }
    }

    @ModifyArg(
            method = "renderLabelIfPresent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"
            ),
            index = 0
    )
    private Text teammarker$modifyDrawnText(Text text) {
        try {
            EntityRenderState state = TEAMMARKER_CURRENT_STATE.get();
            if (state == null) {
                return text;
            }
            if (!(state instanceof PlayerEntityRenderState playerState)) {
                return text;
            }
            if (!TeamMarkerConfigManager.isEnabled()) {
                return text;
            }

            String playerName = playerState.name;
            if (playerName == null || playerName.isEmpty()) {
                return text;
            }
            if (!TeamMarkerConfigManager.isPlayerMarked(playerName)) {
                return text;
            }

            String prefix = TeamMarkerConfigManager.getPlayerPrefix(playerName);
            if (prefix == null || prefix.isEmpty()) {
                return text;
            }

            Formatting color = safeFormatting(TeamMarkerConfigManager.getPlayerColorCode(playerName));
            MutableText result = Text.literal(prefix).append(Text.literal(" ")).append(text);
            if (color != null) {
                result = result.formatted(color);
            }
            return result;
        } catch (Throwable t) {
            return text;
        }
    }

    private static Formatting safeFormatting(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return null;
        }
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
                default:  return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
