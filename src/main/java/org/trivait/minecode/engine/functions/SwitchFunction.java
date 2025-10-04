// src/main/java/org/trivait/minecode/engine/functions/SwitchFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class SwitchFunction implements MineFunction {
    @Override public String id() { return "switch"; }
    @Override public String tutorialKey() { return "minecode.api.switch"; }
    @Override public List<String> hints() {
        return List.of("switch (<expr>) { ... case <value>: ... default: ... }", "endswitch");
    }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        String head = tokens.get(0).toLowerCase();
        switch (head) {
            case "switch" -> {
                String expr = String.join(" ", tokens.subList(1, tokens.size())).trim();
                expr = stripOuterParens(expr.replace("{", "").trim());
                return List.of(Instruction.switchExpr(expr));
            }
            case "case" -> {
                String expr = String.join(" ", tokens.subList(1, tokens.size())).trim();
                expr = stripTrailingColon(expr);
                return List.of(Instruction.caseExpr(expr));
            }
            case "default" -> {
                return List.of(Instruction.defaultCase());
            }
            case "endswitch" -> {
                return List.of(Instruction.endSwitch());
            }
        }
        throw new RuntimeException("Unknown switch/case/default/endswitch");
    }

    private String stripOuterParens(String s) {
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }

    private String stripTrailingColon(String s) {
        s = s.trim();
        if (s.endsWith(":")) return s.substring(0, s.length()-1).trim();
        return s;
    }
}
