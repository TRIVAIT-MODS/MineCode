package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public class WaitFunction implements MineFunction {
    @Override
    public String id() { return "wait"; }

    @Override
    public String tutorialKey() { return "minecode.api.wait"; }

    @Override
    public List<String> hints() { return List.of("wait <ticks>"); }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        if (tokens.size() != 2) throw new RuntimeException("WAIT requires ticks");
        int t = Integer.parseInt(tokens.get(1));
        return List.of(Instruction.waitTicks(t));
    }
}
