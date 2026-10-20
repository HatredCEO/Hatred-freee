package dev.Mardexx.client.modules.impl.Render;

public enum EntityTargetFilter {
	PLAYERS("Players"),
	FRIENDS("Friends"),
	MOBS("Mobs"),
	ANIMALS("Animals");

	private final String display;

	EntityTargetFilter(String display) {
		this.display = display;
	}

	public String display() {
		return display;
	}
}
