// src/main/java/org/trivait/minecode/engine/functions/StringFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class StringFunction implements MineFunction {
    @Override public String id() { return "String"; }
    @Override public String tutorialKey() { return "minecode.api.string"; }
    @Override public List<String> hints() { return List.of("String <name> = \"text\""); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() >= 4 && "=".equals(tokens.get(2))) {
            String name = tokens.get(1);
            String value = String.join(" ", tokens.subList(3, tokens.size())).trim();
            return List.of(Instruction.declareVar("String", name, value));
        }
        throw new RuntimeException("Usage: String <name> = \"text\"");
    }
}
