// src/main/java/org/trivait/minecode/engine/functions/WhileFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class WhileFunction implements MineFunction {
    @Override public String id() { return "while"; }
    @Override public String tutorialKey() { return "minecode.api.while"; }
    @Override public List<String> hints() { return List.of("while (<condition>) { ... }", "endwhile"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        String head = tokens.get(0).toLowerCase();
        if (head.equals("while")) {
            String cond = String.join(" ", tokens.subList(1, tokens.size())).trim();
            cond = stripOuterParens(cond.replace("{", "").trim());
            return List.of(Instruction.whileLoop(cond));
        } else if (head.equals("endwhile")) {
            return List.of(Instruction.endWhile());
        }
        throw new RuntimeException("Unknown while/endwhile");
    }

    private String stripOuterParens(String s) {
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }
}
