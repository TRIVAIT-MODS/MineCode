package org.trivait.minecode.engine.other;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;


import java.util.HashMap;
import java.util.Map;

public class VariableContext {
    private final Map<String, Integer> intVars = new HashMap<>();
    private final Map<String, String> stringVars = new HashMap<>();
    private final Map<String, Boolean> boolVars = new HashMap<>();
    private final Map<String, String> blockVars = new HashMap<>();
    private String playerTargetBlock = "";
    private String playerUnderBlock = "";

    public void updatePlayerBlocks(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        BlockPos underPos = client.player.getBlockPos().down();
        Block underBlock = client.world.getBlockState(underPos).getBlock();
        playerUnderBlock = Registries.BLOCK.getId(underBlock).toString();

        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult blockHit) {
            Block target = client.world.getBlockState(blockHit.getBlockPos()).getBlock();
            playerTargetBlock = Registries.BLOCK.getId(target).toString();
        } else {
            playerTargetBlock = "";
        }
    }

    public String getPlayerTargetBlock() {
        return playerTargetBlock;
    }

    public String getPlayerUnderBlock() {
        return playerUnderBlock;
    }


    public void setBlock(String name, String value) {
        blockVars.put(name, value);
    }

    public boolean hasBlock(String name) {
        return blockVars.containsKey(name);
    }

    public String getBlock(String name) {
        if (!blockVars.containsKey(name)) throw new RuntimeException("Block '" + name + "' не определён");
        return blockVars.get(name);
    }


    public void setInt(String name, int value) {
        intVars.put(name, value);
    }

    public void setString(String name, String value) {
        stringVars.put(name, value);
    }

    public void setBool(String name, boolean value) {
        boolVars.put(name, value);
    }

    public int getPlayerHealth(MinecraftClient client) {
        if (client.player == null) return 0;
        return (int) client.player.getHealth();
    }


    public boolean hasInt(String name) {
        return intVars.containsKey(name);
    }

    public boolean hasString(String name) {
        return stringVars.containsKey(name);
    }

    public boolean hasBool(String name) {
        return boolVars.containsKey(name);
    }

    public int getInt(String name) {
        if (!intVars.containsKey(name)) throw new RuntimeException("int '" + name + "' не определена");
        return intVars.get(name);
    }

    public String getString(String name) {
        if (!stringVars.containsKey(name)) throw new RuntimeException("string '" + name + "' не определена");
        return stringVars.get(name);
    }

    public boolean getBool(String name) {
        if (!boolVars.containsKey(name)) throw new RuntimeException("bool '" + name + "' не определена");
        return boolVars.get(name);
    }

    public int evaluateInt(String expr) {
        expr = expr.replaceAll("\\s+", "");
        String[] tokens = expr.split("(?<=[-+*/])|(?=[-+*/])");
        int result = parseInt(tokens[0]);
        for (int i = 1; i < tokens.length; i += 2) {
            String op = tokens[i];
            int b = parseInt(tokens[i + 1]);
            switch (op) {
                case "+" -> result += b;
                case "-" -> result -= b;
                case "*" -> result *= b;
                case "/" -> result /= b;
            }
        }
        return result;
    }

    public String evaluateString(String expr) {
        expr = expr.trim();
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);
        }
        if (stringVars.containsKey(expr)) return stringVars.get(expr);
        if (intVars.containsKey(expr)) return String.valueOf(intVars.get(expr));

        String[] parts = expr.split("\\+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("\"") && part.endsWith("\"")) {
                sb.append(part, 1, part.length() - 1);
            } else if (stringVars.containsKey(part)) {
                sb.append(stringVars.get(part));
            } else if (intVars.containsKey(part)) {
                sb.append(intVars.get(part));
            } else {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    public boolean evaluateCondition(String expr) {
        expr = expr.trim();

        if (expr.contains("player.targetBlock")) {
            expr = expr.replace("player.targetBlock", getPlayerTargetBlock());
        }
        if (expr.contains("player.underBlock")) {
            expr = expr.replace("player.underBlock", getPlayerUnderBlock());
        }
        if (expr.contains("player.health")) {
            expr = expr.replace("player.health", String.valueOf(getPlayerHealth(MinecraftClient.getInstance())));
        }


        if (expr.equals("true")) return true;
        if (expr.equals("false")) return false;

        if (boolVars.containsKey(expr)) return boolVars.get(expr);

        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            String left = parts[0].trim();
            String right = parts[1].trim();

            if ((left.equals("true") || left.equals("false") || hasBool(left)) &&
                    (right.equals("true") || right.equals("false") || hasBool(right))) {
                boolean a = left.equals("true") || (hasBool(left) && getBool(left));
                boolean b = right.equals("true") || (hasBool(right) && getBool(right));
                return a == b;
            }

            if ((left.startsWith("minecraft:") || left.startsWith("#") || hasBlock(left)) &&
                    (right.startsWith("minecraft:") || right.startsWith("#") || hasBlock(right))) {
                String a = left.startsWith("minecraft:") || left.startsWith("#") ? left : getBlock(left);
                String b = right.startsWith("minecraft:") || right.startsWith("#") ? right : getBlock(right);
                return a.equals(b);
            }

            if ((left.startsWith("\"") && right.startsWith("\"")) || (hasString(left) && hasString(right))) {
                String a = evaluateString(left);
                String b = evaluateString(right);
                return a.equals(b);
            }

            return evaluateInt(left) == evaluateInt(right);
        }

        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            String left = parts[0].trim();
            String right = parts[1].trim();

            if ((left.equals("true") || left.equals("false") || hasBool(left)) &&
                    (right.equals("true") || right.equals("false") || hasBool(right))) {
                boolean a = left.equals("true") || (hasBool(left) && getBool(left));
                boolean b = right.equals("true") || (hasBool(right) && getBool(right));
                return a != b;
            }

            if ((left.startsWith("minecraft:") || left.startsWith("#") || hasBlock(left)) &&
                    (right.startsWith("minecraft:") || right.startsWith("#") || hasBlock(right))) {
                String a = left.startsWith("minecraft:") || left.startsWith("#") ? left : getBlock(left);
                String b = right.startsWith("minecraft:") || right.startsWith("#") ? right : getBlock(right);
                return !a.equals(b);
            }

            if ((left.startsWith("\"") && right.startsWith("\"")) || (hasString(left) && hasString(right))) {
                String a = evaluateString(left);
                String b = evaluateString(right);
                return !a.equals(b);
            }

            return evaluateInt(left) != evaluateInt(right);
        }

        if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            return evaluateInt(parts[0].trim()) > evaluateInt(parts[1].trim());
        }

        if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            return evaluateInt(parts[0].trim()) < evaluateInt(parts[1].trim());
        }

        return boolVars.getOrDefault(expr, false);
    }


    private int parseInt(String token) {
        token = token.trim();
        if (intVars.containsKey(token)) return intVars.get(token);
        return Integer.parseInt(token);
    }
}
