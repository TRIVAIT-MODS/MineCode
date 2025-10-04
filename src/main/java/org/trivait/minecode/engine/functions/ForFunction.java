// src/main/java/org/trivait/minecode/engine/functions/ForFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class ForFunction implements MineFunction {
    @Override public String id() { return "for"; }
    @Override public String tutorialKey() { return "minecode.api.for"; }
    @Override public List<String> hints() { return List.of("for (<init>; <condition>; <update>) { ... }", "endfor"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        String head = tokens.get(0).toLowerCase();
        if (head.equals("for")) {
            // поддержка: for i=0; i<10; i=i+1
            // или: for (i=0; i<10; i=i+1) {
            String joined = String.join(" ", tokens.subList(1, tokens.size())).trim();
            joined = joined.replace("{", "").trim();
            joined = stripOuterParens(joined);
            String[] parts = joined.split(";");
            if (parts.length != 3) throw new RuntimeException("Usage: for init; condition; update");
            return List.of(Instruction.forLoop(parts[0].trim(), parts[1].trim(), parts[2].trim()));
        } else if (head.equals("endfor")) {
            return List.of(Instruction.endFor());
        }
        throw new RuntimeException("Unknown for/endfor");
    }

    private String stripOuterParens(String s) {
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }
}
