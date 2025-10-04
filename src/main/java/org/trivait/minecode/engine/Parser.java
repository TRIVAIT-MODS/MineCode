// src/main/java/org/trivait/minecode/engine/Parser.java
package org.trivait.minecode.engine;

import org.trivait.minecode.engine.functions.FunctionRegistry;
import org.trivait.minecode.engine.functions.MineFunction;

import java.util.*;

public class Parser {
    public static List<Instruction> parse(String code) {
        List<Token> tokens = tokenize(code);
        List<Instruction> program = new ArrayList<>();
        Deque<Instruction.Type> blockStack = new ArrayDeque<>();

        List<String> cur = new ArrayList<>();
        int lineNo = 1;

        for (Token tk : tokens) {
            String s = tk.lexeme;
            lineNo = tk.line;

            // закрытие блока
            if ("}".equals(s)) {
                // завершить возможный текущий стейтмент до "}"
                if (!cur.isEmpty()) {
                    program.addAll(parseStatement(cur, lineNo));
                    cur.clear();
                }
                if (blockStack.isEmpty()) {
                    throw new RuntimeException("Unexpected } at line " + lineNo);
                }
                Instruction.Type t = blockStack.pop();
                switch (t) {
                    case IF -> program.add(Instruction.endIf());
                    case WHILE -> program.add(Instruction.endWhile());
                    case FOR -> program.add(Instruction.endFor());
                    case SWITCH -> program.add(Instruction.endSwitch());
                    case DO -> { /* do закрывается while(...) — тут ничего не добавляем */ }
                }
                continue;
            }

            cur.add(s);

            // окончание стейтмента по ";"
            if (";".equals(s)) {
                program.addAll(parseStatement(cur, lineNo));
                cur.clear();
                continue;
            }

            // открытие блока "{"
            if ("{".equals(s) && !cur.isEmpty()) {
                List<Instruction> ins = parseStatement(cur, lineNo);
                program.addAll(ins);
                if (!ins.isEmpty()) {
                    Instruction.Type type = ins.get(0).type;
                    switch (type) {
                        case IF -> blockStack.push(Instruction.Type.IF);
                        case WHILE -> blockStack.push(Instruction.Type.WHILE);
                        case FOR -> blockStack.push(Instruction.Type.FOR);
                        case SWITCH -> blockStack.push(Instruction.Type.SWITCH);
                        case DO -> blockStack.push(Instruction.Type.DO);
                        default -> { /* не блоковая конструкция */ }
                    }
                }
                cur.clear();
            }
        }

        // хвост без ";" — допустим для одиночных выражений/команд
        if (!cur.isEmpty()) {
            program.addAll(parseStatement(cur, lineNo));
        }

        return program;
    }

    // разбор одного стейтмента
    private static List<Instruction> parseStatement(List<String> raw, int lineNo) {
        List<String> t = normalizeTokens(raw);
        if (t.isEmpty()) return List.of();

        String head = t.get(0).toLowerCase(Locale.ROOT);

        // присваивание: name = expr
        if (t.size() >= 3 && "=".equals(t.get(1))) {
            String name = t.get(0);
            String expr = String.join(" ", t.subList(2, t.size())).trim();
            return List.of(Instruction.setVar(name, expr));
        }

        // ключевые слова блоков перенаправляем на соответствующие функции
        if (head.equals("else") || head.equals("endif")) {
            MineFunction fn = FunctionRegistry.byId("if");
            if (fn == null) throw new RuntimeException("Unknown function at line " + lineNo + ": if (needed for else/endif)");
            return fn.parseTokens(t);
        }
        if (head.equals("endwhile")) {
            MineFunction fn = FunctionRegistry.byId("while");
            if (fn == null) throw new RuntimeException("Unknown function at line " + lineNo + ": while (needed for endwhile)");
            return fn.parseTokens(t);
        }
        if (head.equals("endfor")) {
            MineFunction fn = FunctionRegistry.byId("for");
            if (fn == null) throw new RuntimeException("Unknown function at line " + lineNo + ": for (needed for endfor)");
            return fn.parseTokens(t);
        }
        if (head.equals("case") || head.equals("default") || head.equals("endswitch")) {
            MineFunction fn = FunctionRegistry.byId("switch");
            if (fn == null) throw new RuntimeException("Unknown function at line " + lineNo + ": switch (needed for case/default/endswitch)");
            return fn.parseTokens(t);
        }

        // обычная функция
        String baseId = head.contains(".") ? head.substring(0, head.indexOf('.')) : head;
        MineFunction fn = FunctionRegistry.byId(baseId);
        if (fn == null) fn = FunctionRegistry.byId(head);
        if (fn == null) throw new RuntimeException("Unknown function at line " + lineNo + ": " + head);

        return fn.parseTokens(t);
    }

    // токенизация всего текста с учётом кавычек и строк
    private static List<Token> tokenize(String code) {
        List<Token> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        int line = 1;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (c == '\n') {
                if (inStr) {
                    cur.append(c);
                } else {
                    flushToken(res, cur, line);
                    line++;
                }
                continue;
            }

            if (c == '"') {
                cur.append(c);
                if (inStr) {
                    // конец строкового литерала
                    res.add(new Token(cur.toString(), line));
                    cur.setLength(0);
                    inStr = false;
                } else {
                    inStr = true;
                }
                continue;
            }

            if (inStr) {
                cur.append(c);
                continue;
            }

            if (Character.isWhitespace(c)) {
                flushToken(res, cur, line);
                continue;
            }

            // спецсимволы как отдельные токены
            if ("(){};:.".indexOf(c) >= 0) {
                flushToken(res, cur, line);
                res.add(new Token(String.valueOf(c), line));
                continue;
            }

            cur.append(c);
        }

        flushToken(res, cur, line);
        return res;
    }

    private static void flushToken(List<Token> res, StringBuilder cur, int line) {
        if (cur.length() > 0) {
            res.add(new Token(cur.toString(), line));
            cur.setLength(0);
        }
    }

    // нормализация токенов: схлопывание "walk . forward" -> "walk.forward", удаление финального ";"
    private static List<String> normalizeTokens(List<String> tokens) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String s = tokens.get(i);

            // убрать финальный ";" внутри стейтмента
            if (";".equals(s)) {
                // финальная точка с запятой не нужна для функции
                continue;
            }

            // схлопывание "name" "." "member" → "name.member"
            if (i + 2 < tokens.size() && ".".equals(tokens.get(i + 1))) {
                String merged = (s + "." + tokens.get(i + 2)).toLowerCase(Locale.ROOT);
                out.add(merged);
                i += 2;
                continue;
            }

            out.add(s);
        }

        // убрать ведущую/замыкающую фигурную скобку из токенов стейтмента — блоки мы обрабатываем выше
        if (!out.isEmpty() && "{".equals(out.get(out.size() - 1))) {
            out.remove(out.size() - 1);
        }

        return out;
    }

    // служебные структуры
    private static class Token {
        final String lexeme;
        final int line;
        Token(String lexeme, int line) { this.lexeme = lexeme; this.line = line; }
    }
}
