// src/main/java/org/trivait/minecode/engine/functions/SayFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class SayFunction implements MineFunction {
    @Override public String id() { return "say"; }
    @Override public String tutorialKey() { return "minecode.api.say"; }
    @Override public List<String> hints() { return List.of("say \"text\"", "say <expr|var>"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() < 2) throw new RuntimeException("SAY requires text or expression");
        String joined = String.join(" ", tokens.subList(1, tokens.size())).trim();
        return List.of(Instruction.say(joined));
    }
}
