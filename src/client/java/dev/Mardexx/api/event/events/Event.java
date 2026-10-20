package dev.Mardexx.api.event.events;

import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event<T> {
	private final List<Listener<T>> listeners = new ArrayList<>();
	private boolean cancelled;

	public EventListener subscribe(Listener<T> listener) {
		listeners.add(listener);
		Collections.sort(listeners);
		return new EventListener(() -> listeners.remove(listener));
	}

	public boolean call(T data) {
		cancelled = false;
		for (Listener<T> listener : List.copyOf(listeners)) {
			listener.getHandler().accept(data);
		}
		return cancelled;
	}

	public void setCancel(boolean cancel) {
		cancelled = cancel;
	}

	public boolean isCancelled() {
		return cancelled;
	}
}
