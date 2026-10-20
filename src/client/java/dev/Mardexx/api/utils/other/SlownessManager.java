package dev.Mardexx.api.utils.other;

import net.minecraft.Util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class SlownessManager {
	private static final List<DelayedTask> TASKS = new LinkedList<>();
	private static long slowedUntil;

	private SlownessManager() {
	}

	public static void applySlowness(long totalMs, long delayMs, Runnable action) {
		long now = Util.getMillis();
		slowedUntil = Math.max(slowedUntil, now + Math.max(0L, totalMs));
		TASKS.add(new DelayedTask(now + Math.max(0L, delayMs), action));
	}

	public static boolean slowed() {
		return Util.getMillis() < slowedUntil;
	}

	public static void tick() {
		long now = Util.getMillis();
		Iterator<DelayedTask> iterator = TASKS.iterator();
		while (iterator.hasNext()) {
			DelayedTask task = iterator.next();
			if (now >= task.executeAt()) {
				task.action().run();
				iterator.remove();
			}
		}
	}

	public static void clear() {
		TASKS.clear();
		slowedUntil = 0L;
	}

	private record DelayedTask(long executeAt, Runnable action) {
	}
}
