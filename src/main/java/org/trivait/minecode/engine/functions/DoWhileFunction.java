// src/main/java/org/trivait/minecode/engine/functions/DoWhileFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class DoWhileFunction implements MineFunction {
    @Override public String id() { return "do"; }
    @Override public String tutorialKey() { return "minecode.api.do"; }
    @Override public List<String> hints() { return List.of("do { ... } while (<condition>)"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        String head = tokens.get(0).toLowerCase();
        if (head.equals("do")) {
            return List.of(Instruction.doBlock());
        } else if (head.equals("while")) {
            String cond = String.join(" ", tokens.subList(1, tokens.size())).trim();
            cond = stripOuterParens(cond.replace("{", "").trim());
            return List.of(Instruction.whileAfterDo(cond));
        }
        throw new RuntimeException("Unknown do/while");
    }

    private String stripOuterParens(String s) {
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }
}
