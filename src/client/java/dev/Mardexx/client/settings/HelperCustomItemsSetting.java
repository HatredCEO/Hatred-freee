package dev.Mardexx.client.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class HelperCustomItemsSetting extends Setting<HelperCustomItemsSetting.State> {
	public HelperCustomItemsSetting(String name, String displayName, String description) {
		super(name, displayName, description, State.defaults());
	}

	public List<Profile> profiles() {
		return value().profiles();
	}

	public int activeIndex() {
		State state = value();
		return clampIndex(state.activeIndex(), state.profiles().size());
	}

	public Profile activeProfile() {
		List<Profile> profiles = profiles();
		return profiles.get(clampIndex(activeIndex(), profiles.size()));
	}

	public String activeName() {
		return activeProfile().name();
	}

	public List<Entry> entries() {
		return activeProfile().entries();
	}

	public void selectProfile(int index) {
		State state = value();
		setValue(new State(state.profiles(), clampIndex(index, state.profiles().size())));
	}

	public void addProfile() {
		State state = value();
		List<Profile> profiles = new ArrayList<>(state.profiles());
		profiles.add(new Profile(nextProfileName(profiles), List.of()));
		setValue(new State(profiles, profiles.size() - 1));
	}

	public void removeActiveProfile() {
		State state = value();
		List<Profile> profiles = new ArrayList<>(state.profiles());
		if (profiles.size() <= 1) {
			profiles.set(0, new Profile("Custom 1", List.of()));
			setValue(new State(profiles, 0));
			return;
		}

		profiles.remove(activeIndex());
		setValue(new State(profiles, clampIndex(activeIndex(), profiles.size())));
	}

	public void renameActiveProfile(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			return;
		}
		updateActiveProfile(active -> new Profile(trimmed, active.entries()));
	}

	public void addStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		ItemStack selected = stack.copyWithCount(1);
		Entry entry = Entry.from(selected, -1);
		for (Entry existing : entries()) {
			if (existing.matches(selected)) {
				return;
			}
		}
		updateActiveProfile(active -> {
			List<Entry> entries = new ArrayList<>(active.entries());
			entries.add(entry);
			return new Profile(active.name(), entries);
		});
	}

	public void remove(int index) {
		updateActiveProfile(active -> {
			List<Entry> entries = new ArrayList<>(active.entries());
			if (index >= 0 && index < entries.size()) {
				entries.remove(index);
			}
			return new Profile(active.name(), entries);
		});
	}

	public void setKey(int index, int key) {
		updateActiveProfile(active -> {
			List<Entry> entries = new ArrayList<>(active.entries());
			if (index >= 0 && index < entries.size()) {
				Entry entry = entries.get(index);
				entries.set(index, entry.withKey(key));
			}
			return new Profile(active.name(), entries);
		});
	}

	public void loadProfiles(List<Profile> profiles, int activeIndex) {
		List<Profile> sanitized = profiles == null ? List.of() : profiles.stream()
				.filter(profile -> profile != null)
				.map(profile -> new Profile(
						profile.name() == null || profile.name().isBlank() ? "Custom" : profile.name().trim(),
						profile.entries() == null ? List.of() : profile.entries().stream()
								.filter(entry -> entry != null && !entry.stack().isEmpty())
								.toList()
				))
				.toList();
		if (sanitized.isEmpty()) {
			sanitized = State.defaults().profiles();
		}
		setValue(new State(sanitized, clampIndex(activeIndex, sanitized.size())));
	}

	public void load(List<Entry> entries) {
		loadProfiles(List.of(new Profile("Custom 1", entries == null ? List.of() : entries)), 0);
	}

	private void updateActiveProfile(ProfileUpdater updater) {
		State state = value();
		List<Profile> profiles = new ArrayList<>(state.profiles());
		int index = clampIndex(state.activeIndex(), profiles.size());
		profiles.set(index, updater.update(profiles.get(index)));
		setValue(new State(profiles, index));
	}

	@Override
	protected State copy(State value) {
		return value == null ? State.defaults() : value.copy();
	}

	private static int clampIndex(int index, int size) {
		if (size <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(size - 1, index));
	}

	private static String nextProfileName(List<Profile> profiles) {
		int index = profiles.size() + 1;
		while (true) {
			String name = "Custom " + index;
			boolean exists = profiles.stream().anyMatch(profile -> profile.name().equalsIgnoreCase(name));
			if (!exists) {
				return name;
			}
			index++;
		}
	}

	@FunctionalInterface
	private interface ProfileUpdater {
		Profile update(Profile profile);
	}

	public record State(List<Profile> profiles, int activeIndex) {
		public static State defaults() {
			return new State(List.of(new Profile("Custom 1", List.of())), 0);
		}

		private State copy() {
			List<Profile> copied = profiles == null ? State.defaults().profiles() : profiles.stream()
					.map(Profile::copy)
					.toList();
			return new State(copied.isEmpty() ? State.defaults().profiles() : copied, activeIndex);
		}
	}

	public record Profile(String name, List<Entry> entries) {
		private Profile copy() {
			return new Profile(name, entries == null ? List.of() : new ArrayList<>(entries));
		}
	}

	public record Entry(String itemId, String name, String componentSignature, int key) {
		public static Entry from(ItemStack stack, int key) {
			ItemStack selected = stack.copyWithCount(1);
			return new Entry(
					BuiltInRegistries.ITEM.getKey(selected.getItem()).toString(),
					selected.getHoverName().getString(),
					selected.getComponents().toString(),
					key
			);
		}

		public Entry withKey(int key) {
			return new Entry(itemId, name, componentSignature, key);
		}

		public Item item() {
			ResourceLocation id = ResourceLocation.tryParse(itemId);
			return id == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
		}

		public ItemStack stack() {
			Item item = item();
			return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		}

		public boolean matches(ItemStack candidate) {
			if (candidate == null || candidate.isEmpty()) {
				return false;
			}
			if (!BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString().equals(itemId)) {
				return false;
			}
			if (componentSignature != null && !componentSignature.isEmpty() && candidate.getComponents().toString().equals(componentSignature)) {
				return true;
			}
			return candidate.getHoverName().getString().equals(name);
		}
	}
}
