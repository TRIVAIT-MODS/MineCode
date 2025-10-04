// src/main/java/org/trivait/minecode/engine/functions/FunctionRegistry.java
package org.trivait.minecode.engine.functions;

import java.util.*;

public class FunctionRegistry {
    private static final Map<String, MineFunction> FUNCTIONS = new LinkedHashMap<>();

    public static void register(MineFunction fn) {
        FUNCTIONS.put(fn.id().toUpperCase(Locale.ROOT), fn);
    }

    public static MineFunction byId(String id) {
        if (id == null) return null;
        return FUNCTIONS.get(id.toUpperCase(Locale.ROOT));
    }

    public static Collection<MineFunction> all() {
        return FUNCTIONS.values();
    }

    // Хинты по префиксу токена, включая подфункции через "."
    public static List<String> hintNames(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT).trim();
        List<String> res = new ArrayList<>();

        if (p.contains(".")) {
            String head = p.substring(0, p.indexOf('.'));
            MineFunction fn = byId(head);
            if (fn != null) {
                // Собираем подфункции из hints() (они в формате walk.forward <ticks>)
                Set<String> subs = new LinkedHashSet<>();
                for (String h : fn.hints()) {
                    String sig = h.toLowerCase(Locale.ROOT);
                    if (sig.startsWith(head + ".")) {
                        String sub = sig.split(" ")[0];
                        subs.add(sub);
                    }
                }
                for (String sub : subs) {
                    if (p.isEmpty() || sub.startsWith(p)) res.add(sub);
                }
            }
            return res;
        }

        for (MineFunction fn : FUNCTIONS.values()) {
            String id = fn.id().toLowerCase(Locale.ROOT);
            if (p.isEmpty() || id.startsWith(p)) res.add(fn.id());
        }
        return res;
    }
}
