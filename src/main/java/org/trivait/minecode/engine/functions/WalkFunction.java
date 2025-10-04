package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.ArrayList;
import java.util.List;

public class WalkFunction implements MineFunction {
    @Override
    public String id() { return "walk"; }

    @Override
    public String tutorialKey() { return "minecode.api.walk"; }

    @Override
    public List<String> hints() {
        return List.of(
                "walk.forward <ticks>",
                "walk.back <ticks>"
        );
    }

    @Override
    public List<Instruction> parseTokens(List<String> tokens) {
        // Подфункции: walk.forward, walk.back
        if (tokens.isEmpty()) throw new RuntimeException("WALK requires subcommand");
        String head = tokens.get(0).toLowerCase();

        List<Instruction> res = new ArrayList<>();
        if (head.equals("walk.forward")) {
            if (tokens.size() != 2) throw new RuntimeException("walk.forward <ticks>");
            int steps = Integer.parseInt(tokens.get(1));
            // Положительное движение вперёд
            res.add(Instruction.walkForward(steps));
        } else if (head.equals("walk.back")) {
            if (tokens.size() != 2) throw new RuntimeException("walk.back <ticks>");
            int steps = Integer.parseInt(tokens.get(1));
            // Отрицательное движение назад — закодируем как отрицательные тики
            res.add(Instruction.walkForward(-steps));
        } else {
            throw new RuntimeException("Unknown walk subcommand: " + head);
        }
        return res;
    }
}
