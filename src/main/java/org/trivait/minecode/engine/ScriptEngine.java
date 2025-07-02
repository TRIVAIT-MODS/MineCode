package org.trivait.minecode.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.trivait.minecode.engine.commands.BlockBreaker;
import org.trivait.minecode.engine.commands.ScriptDelayQueue;
import org.trivait.minecode.engine.other.VariableContext;

import java.util.ArrayList;
import java.util.List;

public class ScriptEngine {
    public static final List<String> KNOWN_COMMANDS = List.of(
            "say", "run", "run.sprint", "run.left", "run.right", "run.back",
            "wait", "jump", "clickLeft", "clickRight", "breakBlock",
            "cameraRotation", "setCameraRotation", "setHotBar", "break",
            "int", "string", "bool", "Block", "for", "if", "follow"
    );

    private static VariableContext activeContext = null;

    public static VariableContext getContext() {
        return activeContext;
    }

    public static void runScript(String text, MinecraftClient client) {
        VariableContext ctx = new VariableContext();
        activeContext = ctx;
        ScriptDelayQueue.clear();
        List<String> lines = List.of(text.split("\n"));
        runLines(lines, ctx, client);
    }
    private static void runLines(List<String> lines, VariableContext ctx, MinecraftClient client) {
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            if (raw.isEmpty()) continue;

            // Цикл FOR
            if (raw.startsWith("for") && raw.contains("(") && raw.contains(")") && raw.contains("{")) {
                int depth = 0;
                List<String> body = new ArrayList<>();
                String head = raw;
                if (!head.contains("}")) depth++;

                int j = i + 1;
                while (j < lines.size() && depth > 0) {
                    String l = lines.get(j).trim();
                    if (l.contains("{")) depth++;
                    if (l.contains("}")) depth--;
                    if (depth > 0) body.add(l);
                    j++;
                }

                i = j - 1;
                String inside = head.substring(head.indexOf("(") + 1, head.indexOf(")"));
                String[] parts = inside.split(";");
                if (parts.length == 3) {
                    String init = parts[0].trim();
                    String cond = parts[1].trim();
                    String inc = parts[2].trim();

                    ScriptDelayQueue.addTask(() -> parseLine(init, ctx, client));
                    scheduleForLoop(cond, inc, body, ctx, client);
                }
                continue;
            }

            if (raw.startsWith("if") && raw.contains("(") && raw.contains(")") && raw.contains("{")) {
                String cond = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")).trim();
                int depth = 0;
                List<String> ifBlock = new ArrayList<>();
                List<String> elseBlock = new ArrayList<>();
                boolean inElse = false;

                if (!raw.contains("}")) depth++;
                int j = i + 1;
                while (j < lines.size() && depth > 0) {
                    String l = lines.get(j).trim();
                    if (l.equals("else {")) {
                        inElse = true;
                        j++;
                        continue;
                    }
                    if (l.contains("{")) depth++;
                    if (l.contains("}")) depth--;
                    if (depth > 0) {
                        if (inElse) elseBlock.add(l);
                        else ifBlock.add(l);
                    }
                    j++;
                }

                i = j - 1;
                ScriptDelayQueue.addTask(() -> {
                    ctx.updatePlayerBlocks(client);
                    if (ctx.evaluateCondition(cond)) {
                        runLines(ifBlock, ctx, client);
                    } else if (!elseBlock.isEmpty()) {
                        runLines(elseBlock, ctx, client);
                    }
                });
                continue;
            }

            final String line = raw;
            ScriptDelayQueue.addTask(() -> {
                ctx.updatePlayerBlocks(client);
                parseLine(line, ctx, client);
            });
        }
    }

    private static void scheduleForLoop(String cond, String inc, List<String> body, VariableContext ctx, MinecraftClient client) {
        ScriptDelayQueue.addTask(() -> {
            ctx.updatePlayerBlocks(client);
            if (ctx.evaluateCondition(cond)) {
                runLines(body, ctx, client);
                ScriptDelayQueue.addTask(() -> {
                    parseLine(inc, ctx, client);
                    scheduleForLoop(cond, inc, body, ctx, client);
                });
            }
        });
    }
    private static void parseLine(String line, VariableContext ctx, MinecraftClient client) {
        if (line == null || line.isEmpty()) return;

        if (line.equals("break")) {
            ScriptDelayQueue.clear();
            return;
        }

        if (line.startsWith("int ")) {
            String expr = line.substring(4).trim();
            if (expr.contains("=")) {
                String[] parts = expr.split("=", 2);
                ctx.setInt(parts[0].trim(), ctx.evaluateInt(parts[1].trim()));
            } else ctx.setInt(expr, 0);
            return;
        }

        if (line.startsWith("string ")) {
            String expr = line.substring(7).trim();
            if (expr.contains("=")) {
                String[] parts = expr.split("=", 2);
                ctx.setString(parts[0].trim(), ctx.evaluateString(parts[1].trim()));
            } else ctx.setString(expr, "");
            return;
        }

        if (line.startsWith("bool ")) {
            String expr = line.substring(5).trim();
            if (expr.contains("=")) {
                String[] parts = expr.split("=", 2);
                ctx.setBool(parts[0].trim(), ctx.evaluateCondition(parts[1].trim()));
            } else ctx.setBool(expr, false);
            return;
        }

        if (line.startsWith("Block ")) {
            String expr = line.substring(6).trim();
            if (expr.contains("=")) {
                String[] parts = expr.split("=", 2);
                ctx.setBlock(parts[0].trim(), parts[1].trim());
            } else ctx.setBlock(expr, "");
            return;
        }

        if (line.contains("=")) {
            String[] parts = line.split("=", 2);
            String name = parts[0].trim();
            String expr = parts[1].trim();

            if (ctx.hasInt(name)) {
                ctx.setInt(name, ctx.evaluateInt(expr));
            } else if (ctx.hasBool(name)) {
                ctx.setBool(name, ctx.evaluateCondition(expr));
            } else if (ctx.hasString(name) || expr.startsWith("\"")) {
                ctx.setString(name, ctx.evaluateString(expr));
            } else if (ctx.hasBlock(name)) {
                ctx.setBlock(name, expr);
            } else {
                try {
                    ctx.setInt(name, ctx.evaluateInt(expr));
                } catch (Exception e) {
                    ctx.setString(name, ctx.evaluateString(expr));
                }
            }
            return;
        }

        if (line.startsWith("say ")) {
            String arg = line.substring(4).trim();
            String msg = ctx.hasString(arg) ? ctx.getString(arg)
                    : ctx.hasInt(arg) ? String.valueOf(ctx.getInt(arg))
                    : ctx.hasBool(arg) ? String.valueOf(ctx.getBool(arg))
                    : ctx.hasBlock(arg) ? ctx.getBlock(arg)
                    : arg.replace("\"", "");
            client.execute(() -> client.inGameHud.getChatHud().addMessage(Text.literal(msg)));
            return;
        }

        if (line.startsWith("wait ")) {
            int ticks = ctx.evaluateInt(line.substring(5).trim());
            ScriptDelayQueue.delay(ticks);
            return;
        }
        if (line.startsWith("follow ")) {
            int seconds = ctx.evaluateInt(line.substring(7).trim());
            if (client.crosshairTarget instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                final int[] timer = {seconds * 20};
                ScriptDelayQueue.addRepeatingTask(() -> {
                    if (client.player == null || target == null || !target.isAlive()) return false;

                    Vec3d playerPos = client.player.getPos();
                    Vec3d targetPos = target.getPos();
                    double dx = targetPos.x - playerPos.x;
                    double dz = targetPos.z - playerPos.z;
                    double dist = playerPos.distanceTo(targetPos);

                    if (dist > 2.5) {
                        client.options.forwardKey.setPressed(true);
                    } else {
                        client.options.forwardKey.setPressed(false);
                    }

                    double yaw = Math.toDegrees(Math.atan2(-dx, dz));
                    client.player.setYaw((float) yaw);

                    timer[0]--;
                    if (timer[0] <= 0) {
                        client.options.forwardKey.setPressed(false);
                        return false;
                    }
                    return true;
                });
            }
            return;
        }

        if (line.startsWith("setHotBar ")) {
            int slot = ctx.evaluateInt(line.substring(10).trim());
            slot = Math.max(1, Math.min(slot, 9));
            if (client.player != null) {
                client.player.getInventory().selectedSlot = slot - 1;
            }
            return;
        }

        if (line.startsWith("run.sprint ")) {
            int n = ctx.evaluateInt(line.substring(11).trim());
            ScriptRunner.startSprintForward(client, n);
            return;
        }
        if (line.startsWith("run.left ")) {
            int n = ctx.evaluateInt(line.substring(9).trim());
            ScriptRunner.startStrafe(client, -n);
            return;
        }
        if (line.startsWith("run.right ")) {
            int n = ctx.evaluateInt(line.substring(10).trim());
            ScriptRunner.startStrafe(client, n);
            return;
        }
        if (line.startsWith("run.back ")) {
            int n = ctx.evaluateInt(line.substring(9).trim());
            ScriptRunner.startBackward(client, n);
            return;
        }
        if (line.startsWith("run ")) {
            int n = ctx.evaluateInt(line.substring(4).trim());
            ScriptRunner.startWalkForward(client, n);
            return;
        }

        if (line.equals("jump")) {
            if (client.player != null) client.player.jump();
            return;
        }

        if (line.equals("clickLeft")) {
            if (client.interactionManager != null) {
                client.interactionManager.attackBlock(client.player.getBlockPos(), client.player.getHorizontalFacing());
            }
            return;
        }

        if (line.equals("clickRight")) {
            if (client.interactionManager != null) {
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
            return;
        }

        if (line.equals("breakBlock")) {
            if (client.crosshairTarget instanceof BlockHitResult blockHit) {
                    BlockBreaker.breakBlock(client);
            }
            return;
        }


        if (line.startsWith("cameraRotation")) {
            float yaw = client.player.getYaw();
            float pitch = client.player.getPitch();
            client.inGameHud.getChatHud().addMessage(Text.literal("Yaw: " + yaw + ", Pitch: " + pitch));
            return;
        }

        if (line.startsWith("setCameraRotation ")) {
            String[] parts = line.substring(18).trim().split(" ");
            if (parts.length == 2 && client.player != null) {
                try {
                    float yaw = Float.parseFloat(parts[0]);
                    float pitch = Float.parseFloat(parts[1]);
                    client.player.setYaw(yaw);
                    client.player.setPitch(pitch);
                } catch (NumberFormatException ignored) { }
            }
        }
    }


}
