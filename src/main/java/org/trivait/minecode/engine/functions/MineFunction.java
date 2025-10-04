// src/main/java/org/trivait/minecode/engine/functions/MineFunction.java
package org.trivait.minecode.engine.functions;

import org.trivait.minecode.engine.Instruction;

import java.util.List;

public interface MineFunction {
    String id();                // Полное имя функции: "walk" или "say"
    String tutorialKey();       // Ключ перевода для описания на APIScreen
    List<String> hints();       // Сигнатуры/варианты, включая подфункции через "."

    List<Instruction> parseTokens(List<String> tokens); // Разбор токенов в инструкции
}
