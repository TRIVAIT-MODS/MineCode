package org.trivait.minecode.engine.commands;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class BlockBreaker {
    private static boolean isBreaking = false;
    private static MinecraftClient client = null;
    private static BlockPos target = null;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (!isBreaking || client == null || client.player == null) return;

            if (client.crosshairTarget instanceof BlockHitResult hit &&
                    hit.getType() == HitResult.Type.BLOCK &&
                    client.interactionManager != null) {

                BlockPos current = hit.getBlockPos();

                if (!current.equals(target)) {
                    target = current;
                    client.interactionManager.updateBlockBreakingProgress(current, hit.getSide());
                } else {
                    client.interactionManager.updateBlockBreakingProgress(current, hit.getSide());

                    if (client.world.getBlockState(current).isAir()) {
                        isBreaking = false;
                        target = null;
                    }
                }
            } else {
                isBreaking = false;
            }
        });
    }

    public static void breakBlock(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.crosshairTarget == null) return;
        client = mc;
        isBreaking = true;
    }

    public static boolean isActive() {
        return isBreaking;
    }
}
