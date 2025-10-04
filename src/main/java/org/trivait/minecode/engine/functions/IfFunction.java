package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class IfFunction implements MineFunction {
    @Override public String id() { return "if"; }
    @Override public String tutorialKey() { return "minecode.api.if"; }
    @Override public List<String> hints() {
        return List.of("if (<condition>) { ... }", "else { ... }", "endif / }");
    }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        String head = tokens.get(0).toLowerCase();
        switch (head) {
            case "if" -> {
                // if (a > 5) {
                String cond = String.join(" ", tokens.subList(1, tokens.size())).trim();
                cond = stripOuterParens(cond.replace("{", "").trim());
                return List.of(Instruction.ifCond(cond));
            }
            case "else" -> {
                // else {
                return List.of(Instruction.elseBlock());
            }
            case "endif" -> {
                return List.of(Instruction.endIf());
            }
        }
        throw new RuntimeException("Unknown if/else/endif");
    }

    private String stripOuterParens(String s) {
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }
}
