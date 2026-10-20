package dev.Mardexx.mixins;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface MixinScreenTitleAccessor {
	@Mutable
	@Final
	@Accessor("title")
	void hatred$setTitle(Component title);
}
