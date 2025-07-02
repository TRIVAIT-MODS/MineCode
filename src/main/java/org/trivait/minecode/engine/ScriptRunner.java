package org.trivait.minecode.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import org.trivait.minecode.engine.commands.ScriptDelayQueue;

public class ScriptRunner {
    private static Vec3d startPos = null;
    private static KeyBinding moveKey = null;
    private static boolean sprinting = false;
    private static double targetDistance = 0;

    public static void startWalkForward(MinecraftClient client, int blocks) {
        startMovement(client, blocks, client.options.forwardKey, false);
    }

    public static void startSprintForward(MinecraftClient client, int blocks) {
        startMovement(client, blocks, client.options.forwardKey, true);
    }

    public static void startBackward(MinecraftClient client, int blocks) {
        startMovement(client, blocks, client.options.backKey, false);
    }

    public static void startStrafe(MinecraftClient client, int blocks) {
        if (blocks < 0)
            startMovement(client, -blocks, client.options.leftKey, false);
        else
            startMovement(client, blocks, client.options.rightKey, false);
    }

    private static void startMovement(MinecraftClient client, int blocks, KeyBinding key, boolean sprint) {
        if (blocks <= 0 || client.player == null) return;

        moveKey = key;
        startPos = client.player.getPos();
        targetDistance = blocks;
        sprinting = sprint;

        ScriptDelayQueue.addRepeatingTask(() -> {
            if (client.player == null || moveKey == null || startPos == null) return false;

            double moved = client.player.getPos().distanceTo(startPos);
            if (moved >= targetDistance) {
                moveKey.setPressed(false);
                if (sprinting) client.options.sprintKey.setPressed(false);
                moveKey = null;
                startPos = null;
                return false;
            }

            moveKey.setPressed(true);
            if (sprinting) client.options.sprintKey.setPressed(true);
            return true;
        });
    }
}
