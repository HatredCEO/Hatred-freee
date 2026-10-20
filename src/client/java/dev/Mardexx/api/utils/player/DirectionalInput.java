package dev.Mardexx.api.utils.player;

public final class DirectionalInput {
	private final boolean forward;
	private final boolean backward;
	private final boolean left;
	private final boolean right;

	public DirectionalInput(boolean forward, boolean backward, boolean left, boolean right) {
		this.forward = forward;
		this.backward = backward;
		this.left = left;
		this.right = right;
	}

	public boolean forward() {
		return forward;
	}

	public boolean backward() {
		return backward;
	}

	public boolean left() {
		return left;
	}

	public boolean right() {
		return right;
	}
}
