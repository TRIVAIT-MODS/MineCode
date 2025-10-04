// src/main/java/org/trivait/minecode/engine/functions/IntFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class IntFunction implements MineFunction {
    @Override public String id() { return "int"; }
    @Override public String tutorialKey() { return "minecode.api.int"; }
    @Override public List<String> hints() { return List.of("int <name> = <value>"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() >= 4 && "=".equals(tokens.get(2))) {
            String name = tokens.get(1);
            String value = String.join(" ", tokens.subList(3, tokens.size())).trim();
            return List.of(Instruction.declareVar("int", name, value));
        }
        throw new RuntimeException("Usage: int <name> = <value>");
    }
}
