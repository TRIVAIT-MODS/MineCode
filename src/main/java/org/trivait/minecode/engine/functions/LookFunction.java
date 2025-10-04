package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class LookFunction implements MineFunction {
    @Override
    public String id() { return "look"; }

    @Override
    public String tutorialKey() { return "minecode.api.look"; }

    @Override
    public List<String> hints() { return List.of("look <yaw> <pitch>"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() != 3) throw new RuntimeException("LOOK requires yaw pitch");
        float yaw = Float.parseFloat(tokens.get(1));
        float pitch = Float.parseFloat(tokens.get(2));
        return List.of(Instruction.look(yaw, pitch));
    }
}
