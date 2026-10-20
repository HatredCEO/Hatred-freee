package dev.Mardexx.api.event;

import java.util.function.Consumer;

public class Listener<T> implements Comparable<Listener<T>> {
	private static int counter;
	private final int priority;
	private final Consumer<T> handler;
	private final long id;

	public Listener(int priority, Consumer<T> handler) {
		this.priority = priority;
		this.handler = handler;
		this.id = counter++;
	}

	public Listener(Consumer<T> handler) {
		this(0, handler);
	}

	@Override
	public int compareTo(Listener<T> other) {
		int prioCompare = Integer.compare(other.priority, priority);
		return prioCompare != 0 ? prioCompare : Long.compare(id, other.id);
	}

	public int getPriority() {
		return priority;
	}

	public Consumer<T> getHandler() {
		return handler;
	}
}
