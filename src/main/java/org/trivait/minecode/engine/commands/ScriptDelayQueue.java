package org.trivait.minecode.engine.commands;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.trivait.minecode.engine.ScriptEngine;
import org.trivait.minecode.engine.other.VariableContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Supplier;

public class ScriptDelayQueue {
    private static final Deque<Runnable> queue = new ArrayDeque<>();
    private static final Deque<RepeatingTask> repeatingTasks = new ArrayDeque<>();
    private static int delayTicks = 0;

    public static void clear() {
        queue.clear();
        repeatingTasks.clear();
        delayTicks = 0;
    }

    public static void delay(int ticks) {
        delayTicks += ticks;
    }

    public static void addTask(Runnable task) {
        queue.addLast(task);
    }

    public static void addRepeatingTask(Supplier<Boolean> task) {
        repeatingTasks.addLast(new RepeatingTask(task));
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            VariableContext ctx = ScriptEngine.getContext();
            if (ctx != null) ctx.updatePlayerBlocks(client);
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        if (!queue.isEmpty()) {
            Runnable task = queue.removeFirst();
            task.run();
        }

        Iterator<RepeatingTask> iter = repeatingTasks.iterator();
        while (iter.hasNext()) {
            RepeatingTask rt = iter.next();
            boolean keepGoing = rt.run();
            if (!keepGoing) iter.remove();
        }
    }


    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ScriptDelayQueue.tick();
        });
    }

    private static class RepeatingTask {
        private final Supplier<Boolean> task;

        public RepeatingTask(Supplier<Boolean> task) {
            this.task = task;
        }

        public boolean run() {
            return task.get();
        }
    }
}
