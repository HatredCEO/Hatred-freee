package dev.Mardexx.client.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemStackSetting extends Setting<ItemStack> {
	private String itemId = "";
	private String selectedName = "";
	private String componentSignature = "";

	public ItemStackSetting(String name, String displayName, String description) {
		super(name, displayName, description, ItemStack.EMPTY);
	}

	public boolean hasValue() {
		return !itemId.isEmpty();
	}

	public ItemStack stack() {
		return value();
	}

	public Item item() {
		return stack().getItem();
	}

	public String itemId() {
		return itemId;
	}

	public String selectedName() {
		return selectedName;
	}

	public String componentSignature() {
		return componentSignature;
	}

	public void setStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			clear();
			return;
		}

		ItemStack selected = stack.copyWithCount(1);
		itemId = BuiltInRegistries.ITEM.getKey(selected.getItem()).toString();
		selectedName = selected.getHoverName().getString();
		componentSignature = selected.getComponents().toString();
		setValue(selected);
	}

	public void restore(String itemId, String selectedName, String componentSignature) {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		ItemStack restored = id == null
				? ItemStack.EMPTY
				: BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
		if (restored.isEmpty()) {
			clear();
			return;
		}

		this.itemId = itemId;
		this.selectedName = selectedName == null || selectedName.isEmpty()
				? restored.getHoverName().getString()
				: selectedName;
		this.componentSignature = componentSignature == null ? "" : componentSignature;
		setValue(restored);
	}

	public boolean matches(ItemStack candidate) {
		if (!hasValue() || candidate == null || candidate.isEmpty()) {
			return false;
		}
		if (!BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString().equals(itemId)) {
			return false;
		}
		if (!componentSignature.isEmpty() && candidate.getComponents().toString().equals(componentSignature)) {
			return true;
		}
		return candidate.getHoverName().getString().equals(selectedName);
	}

	public void clear() {
		itemId = "";
		selectedName = "";
		componentSignature = "";
		setValue(ItemStack.EMPTY);
	}

	@Override
	protected ItemStack copy(ItemStack value) {
		return value == null || value.isEmpty() ? ItemStack.EMPTY : value.copy();
	}
}
