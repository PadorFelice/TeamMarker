package com.example.teammarker.mixin;

import com.example.teammarker.config.TeamMarkerConfigManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void teammarker$forceGlowForTeammates(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Boolean.TRUE.equals(cir.getReturnValue())) return;
            if (!TeamMarkerConfigManager.isEnabled()) return;
            if (!TeamMarkerConfigManager.isGlowEnabled()) return;

            Entity self = (Entity) (Object) this;
            if (!(self instanceof PlayerEntity player)) return;

            String name;
            try {
                name = player.getName().getString();
            } catch (Throwable ignored) {
                return;
            }
            if (name == null || name.isEmpty()) return;
            if (!TeamMarkerConfigManager.isPlayerMarked(name)) return;

            cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    private void teammarker$useConfigColorForTeammateGlow(CallbackInfoReturnable<Integer> cir) {
        try {
            if (!TeamMarkerConfigManager.isEnabled()) return;
            if (!TeamMarkerConfigManager.isGlowEnabled()) return;

            Entity self = (Entity) (Object) this;
            if (!(self instanceof PlayerEntity player)) return;

            String name;
            try {
                name = player.getName().getString();
            } catch (Throwable ignored) {
                return;
            }
            if (name == null || name.isEmpty()) return;
            if (!TeamMarkerConfigManager.isPlayerMarked(name)) return;

            int rgb = colorCodeToRgb(TeamMarkerConfigManager.getPlayerColorCode(name));
            if (rgb != -1) {
                cir.setReturnValue(rgb);
            }
        } catch (Throwable ignored) {
        }
    }

    private static int colorCodeToRgb(String code) {
        if (code == null || code.isEmpty()) return -1;
        try {
            char c = code.charAt(0);
            switch (c) {
                case '0': return 0x000000;
                case '1': return 0x0000AA;
                case '2': return 0x00AA00;
                case '3': return 0x00AAAA;
                case '4': return 0xAA0000;
                case '5': return 0xAA00AA;
                case '6': return 0xFFAA00;
                case '7': return 0xAAAAAA;
                case '8': return 0x555555;
                case '9': return 0x5555FF;
                case 'a': return 0x55FF55;
                case 'b': return 0x55FFFF;
                case 'c': return 0xFF5555;
                case 'd': return 0xFF55FF;
                case 'e': return 0xFFFF55;
                case 'f': return 0xFFFFFF;
                default:  return -1;
            }
        } catch (Exception ignored) {
            return -1;
        }
    }
}
