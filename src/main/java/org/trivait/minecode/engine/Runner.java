// src/main/java/org/trivait/minecode/engine/Runner.java
package org.trivait.minecode.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class Runner {
    private final List<Instruction> program;
    private int pc = 0;
    private int wait = 0;
    private boolean walking = false;
    private int walkLeft = 0;
    private int walkDir = 0; // 1 вперед, -1 назад
    private boolean stopped = false;

    private final Map<String, Object> vars = new HashMap<>();
    private final Deque<Integer> whileStack = new ArrayDeque<>();
    private final Deque<Integer> doStack = new ArrayDeque<>();
    private final Deque<ForFrame> forStack = new ArrayDeque<>();
    private final Deque<SwitchFrame> switchStack = new ArrayDeque<>();

    public Runner(List<Instruction> program) {
        this.program = program;
    }

    public boolean tick(MinecraftClient client) {
        if (stopped) return false;
        PlayerEntity p = client.player;
        if (p == null) return true;

        if (wait > 0) { wait--; return true; }

        if (walking) {
            if (walkDir == 1) client.player.input.movementForward = 1.0f;
            else if (walkDir == -1) client.player.input.movementForward = -1.0f;
            walkLeft--;
            if (walkLeft <= 0) {
                walking = false;
                client.player.input.movementForward = 0.0f;
            }
            return true;
        }

        if (pc >= program.size()) return false;
        Instruction ins = program.get(pc++);

        switch (ins.type) {
            // ===== Игровые действия =====
            case SAY -> {
                Object v = evalExpr(ins.text);
                client.inGameHud.getChatHud().addMessage(Text.literal(String.valueOf(v)));
            }
            case WAIT -> wait = ins.ticks;
            case LOOK -> {
                p.setYaw(p.getYaw() + ins.yaw);
                p.setPitch(clampPitch(p.getPitch() + ins.pitch));
            }
            case WALK -> {
                walking = true;
                walkLeft = ins.ticks;
                walkDir = ins.walkDirection;
            }
            case STOP -> {
                stopped = true;
                client.player.input.movementForward = 0.0f;
                return false;
            }

            // ===== Переменные =====
            case VAR_DECL -> {
                String name = ins.text;
                String type = ins.text2;
                String init = stripOuterParens(ins.text3);
                Object val = null;
                if (init != null) val = evalExpr(init);
                vars.put(name, castToType(val, type));
            }
            case VAR_SET -> {
                String name = ins.text;
                Object val = evalExpr(stripOuterParens(ins.text2));
                Object prev = vars.get(name);
                String type = typeOf(prev);
                vars.put(name, castToType(val, type));
            }

            // ===== IF / ELSE =====
            case IF -> {
                boolean ok = evalBool(stripOuterParens(ins.text));
                if (!ok) {
                    skipUntil(Set.of(Instruction.Type.ELSE, Instruction.Type.ENDIF));
                }
            }
            case ELSE -> skipUntil(Set.of(Instruction.Type.ENDIF));
            case ENDIF -> { /* конец if */ }

            // ===== WHILE =====
            case WHILE -> {
                boolean ok = evalBool(stripOuterParens(ins.text));
                if (ok) {
                    whileStack.push(pc - 1);
                } else {
                    skipUntil(Set.of(Instruction.Type.ENDWHILE));
                }
            }
            case ENDWHILE -> {
                if (whileStack.isEmpty()) break;
                int whilePos = whileStack.peek();
                pc = whilePos;
            }

            // ===== DO ... WHILE =====
            case DO -> doStack.push(pc - 1);
            case WHILE_AFTER_DO -> {
                if (doStack.isEmpty()) break;
                int doPos = doStack.peek();
                boolean ok = evalBool(stripOuterParens(ins.text));
                if (ok) pc = doPos + 1;
                else doStack.pop();
            }

            // ===== FOR =====
            case FOR -> {
                evalAssign(ins.text); // init (внутри могут быть скобки — игнорируем)
                ForFrame frame = new ForFrame(pc - 1, stripOuterParens(ins.text2), stripOuterParens(ins.text3));
                forStack.push(frame);
                boolean ok = evalBool(frame.cond);
                if (!ok) {
                    skipUntil(Set.of(Instruction.Type.ENDFOR));
                    forStack.pop();
                }
            }
            case ENDFOR -> {
                if (forStack.isEmpty()) break;
                ForFrame f = forStack.peek();
                evalAssign(f.update);
                boolean ok = evalBool(f.cond);
                if (ok) pc = f.forPos + 1;
                else forStack.pop();
            }

            // ===== SWITCH =====
            case SWITCH -> {
                Object val = evalExpr(stripOuterParens(ins.text));
                SwitchFrame sf = new SwitchFrame(pc - 1, val);
                switchStack.push(sf);
            }
            case CASE -> {
                if (switchStack.isEmpty()) break;
                SwitchFrame sf = switchStack.peek();
                if (sf.matched || sf.inDefault) break;
                Object cur = evalExpr(stripOuterParens(ins.text));
                if (Objects.equals(sf.value, cur)) sf.matched = true;
                else skipUntil(Set.of(Instruction.Type.CASE, Instruction.Type.DEFAULT, Instruction.Type.ENDSWITCH));
            }
            case DEFAULT -> {
                if (switchStack.isEmpty()) break;
                SwitchFrame sf = switchStack.peek();
                if (!sf.matched) sf.inDefault = true;
            }
            case ENDSWITCH -> {
                if (!switchStack.isEmpty()) switchStack.pop();
            }
        }
        return true;
    }

    public void stop() {
        stopped = true;
        walkLeft = 0;
        walking = false;
        walkDir = 0;
    }

    private float clampPitch(float v) {
        if (v > 90) v = 90;
        if (v < -90) v = -90;
        return v;
    }

    private static class ForFrame {
        final int forPos;
        final String cond;
        final String update;
        ForFrame(int forPos, String cond, String update) { this.forPos = forPos; this.cond = cond; this.update = update; }
    }
    private static class SwitchFrame {
        final int switchPos;
        final Object value;
        boolean matched = false;
        boolean inDefault = false;
        SwitchFrame(int pos, Object val) { this.switchPos = pos; this.value = val; }
    }

    private void skipUntil(Set<Instruction.Type> typeSet) {
        while (pc < program.size()) {
            Instruction next = program.get(pc);
            if (typeSet.contains(next.type)) { pc++; break; }
            pc++;
        }
    }

    // ==== Вычисления выражений с поддержкой внешних () ====
    private Object evalExpr(String expr) {
        if (expr == null) return null;
        String s = stripOuterParens(expr.trim());
        if (s.isEmpty()) return "";

        // строка в кавычках
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        // булево
        if (s.equals("true")) return Boolean.TRUE;
        if (s.equals("false")) return Boolean.FALSE;
        // число
        try {
            if (s.contains(".")) return Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}

        // бинарные операции: a + 1, a < 10, a == 5, x && y
        String[] parts = s.split(" ");
        if (parts.length == 3) {
            Object left = evalExpr(parts[0]);
            String op = parts[1];
            Object right = evalExpr(parts[2]);
            return applyOp(left, op, right);
        }

        // переменная
        if (vars.containsKey(s)) return vars.get(s);

        // иначе — строка
        return s;
    }

    private String stripOuterParens(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).trim();
        }
        // убрать завершающее ":" в case
        if (s.endsWith(":")) return s.substring(0, s.length() - 1).trim();
        return s;
    }

    private Object applyOp(Object left, String op, Object right) {
        if (op.equals("==")) return Objects.equals(left, right);
        if (op.equals("!=")) return !Objects.equals(left, right);
        if (op.equals("&&")) return toBool(left) && toBool(right);
        if (op.equals("||")) return toBool(left) || toBool(right);

        double l = toNumber(left);
        double r = toNumber(right);
        switch (op) {
            case "+" -> { return (isInt(left) && isInt(right)) ? (int)(l + r) : (l + r); }
            case "-" -> { return (isInt(left) && isInt(right)) ? (int)(l - r) : (l - r); }
            case "*" -> { return (isInt(left) && isInt(right)) ? (int)(l * r) : (l * r); }
            case "/" -> { return (isInt(left) && isInt(right)) ? (int)(l / r) : (l / r); }
            case "<" -> { return l < r; }
            case ">" -> { return l > r; }
            case "<=" -> { return l <= r; }
            case ">=" -> { return l >= r; }
        }
        throw new RuntimeException("Unsupported op: " + op);
    }

    private boolean evalBool(String expr) {
        Object v = evalExpr(expr);
        return toBool(v);
    }

    private void evalAssign(String assign) {
        String s = assign.trim();
        // снимаем внешние скобки: (i = i + 1)
        s = stripOuterParens(s);
        String compact = s.replace(" ", "");
        int idx = compact.indexOf('=');
        if (idx < 0) throw new RuntimeException("Bad assign: " + assign);
        String name = compact.substring(0, idx);
        String expr = s.substring(s.indexOf('=') + 1).trim();
        Object val = evalExpr(expr);
        Object prev = vars.get(name);
        String type = typeOf(prev);
        vars.put(name, castToType(val, type));
    }

    private boolean toBool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0.0;
        if (v instanceof String s) return !s.isEmpty();
        return v != null;
    }
    private double toNumber(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b ? 1 : 0;
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { throw new RuntimeException("Not a number: " + s); }
        }
        throw new RuntimeException("Not a number: " + v);
    }
    private boolean isInt(Object v) { return v instanceof Integer; }

    private String typeOf(Object v) {
        if (v instanceof Integer) return "int";
        if (v instanceof Boolean) return "boolean";
        if (v instanceof String) return "String";
        if (v instanceof Double || v instanceof Float) return "double";
        return "String";
    }

    private Object castToType(Object val, String type) {
        if (type == null) return val;

        switch (type) {
            case "int" -> {
                if (val == null) return 0;
                if (val instanceof Number n) return n.intValue();
                if (val instanceof Boolean b) return b ? 1 : 0;
                if (val instanceof String s) {
                    try { return Integer.parseInt(s.trim()); }
                    catch (NumberFormatException e) { throw new RuntimeException("Cannot cast to int: " + s); }
                }
                throw new RuntimeException("Cannot cast to int: " + val);
            }
            case "boolean" -> {
                if (val == null) return false;
                return toBool(val);
            }
            case "String" -> {
                if (val == null) return "";
                return String.valueOf(val);
            }
            case "double" -> {
                if (val == null) return 0.0d;
                if (val instanceof Number n) return n.doubleValue();
                if (val instanceof Boolean b) return b ? 1.0d : 0.0d;
                if (val instanceof String s) {
                    try { return Double.parseDouble(s.trim()); }
                    catch (NumberFormatException e) { throw new RuntimeException("Cannot cast to double: " + s); }
                }
                throw new RuntimeException("Cannot cast to double: " + val);
            }
            case "float" -> {
                if (val == null) return 0.0f;
                if (val instanceof Number n) return n.floatValue();
                if (val instanceof Boolean b) return b ? 1.0f : 0.0f;
                if (val instanceof String s) {
                    try { return Float.parseFloat(s.trim()); }
                    catch (NumberFormatException e) { throw new RuntimeException("Cannot cast to float: " + s); }
                }
                throw new RuntimeException("Cannot cast to float: " + val);
            }
            default -> {
                return String.valueOf(val);
            }
        }
    }
}
