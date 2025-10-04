// src/main/java/org/trivait/minecode/engine/functions/BooleanFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class BooleanFunction implements MineFunction {
    @Override public String id() { return "boolean"; }
    @Override public String tutorialKey() { return "minecode.api.boolean"; }
    @Override public List<String> hints() { return List.of("boolean <name> = <true|false>"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() >= 4 && "=".equals(tokens.get(2))) {
            String name = tokens.get(1);
            String value = String.join(" ", tokens.subList(3, tokens.size())).trim();
            return List.of(Instruction.declareVar("boolean", name, value));
        }
        throw new RuntimeException("Usage: boolean <name> = <true|false>");
    }
}
