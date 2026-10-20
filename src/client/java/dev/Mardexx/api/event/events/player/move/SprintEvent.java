package dev.Mardexx.api.event.events.player.move;

import dev.Mardexx.api.event.events.Event;
import dev.Mardexx.api.utils.player.DirectionalInput;

public class SprintEvent extends Event<SprintEvent.SprintEventData> {
	private static final SprintEvent INSTANCE = new SprintEvent();

	@Override
	public boolean call(SprintEventData data) {
		super.call(data);
		return data.isSprint();
	}

	public static SprintEvent getInstance() {
		return INSTANCE;
	}

	public static class SprintEventData {
		private boolean sprint;
		private final DirectionalInput directionalInput;

		public SprintEventData(DirectionalInput directionalInput) {
			this(directionalInput, false);
		}

		public SprintEventData(DirectionalInput directionalInput, boolean sprint) {
			this.directionalInput = directionalInput;
			this.sprint = sprint;
		}

		public void setSprint(boolean sprint) {
			this.sprint = sprint;
		}

		public boolean isSprint() {
			return sprint;
		}

		public DirectionalInput getDirectionalInput() {
			return directionalInput;
		}
	}
}
