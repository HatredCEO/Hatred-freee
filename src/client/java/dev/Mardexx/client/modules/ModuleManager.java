package dev.Mardexx.client.modules;

import dev.Mardexx.client.config.ClientConfig;
import dev.Mardexx.client.modules.impl.Combat.AimAssist;
import dev.Mardexx.client.modules.impl.Combat.AutoSwap;
import dev.Mardexx.client.modules.impl.Combat.Tap;
import dev.Mardexx.client.modules.impl.Combat.TriggerBot;
import dev.Mardexx.client.modules.impl.Movement.InventoryMove;
import dev.Mardexx.client.modules.impl.Movement.NoJumpDelay;
import dev.Mardexx.client.modules.impl.Movement.NoPush;
import dev.Mardexx.client.modules.impl.Movement.Sprint;
import dev.Mardexx.client.modules.impl.Other.FakePlayer;
import dev.Mardexx.client.modules.impl.Other.Helper;
import dev.Mardexx.client.modules.impl.Render.AspectRatio;
import dev.Mardexx.client.modules.impl.Render.AuroraSky;
import dev.Mardexx.client.modules.impl.Render.CustomSwords;
import dev.Mardexx.client.modules.impl.Render.ESP;
import dev.Mardexx.client.modules.impl.Render.Notifications;
import dev.Mardexx.client.modules.impl.Render.FullBright;
import dev.Mardexx.client.modules.impl.Render.Indicator;
import dev.Mardexx.client.modules.impl.Render.Interface;
import dev.Mardexx.client.modules.impl.Render.NameTags;
import dev.Mardexx.client.modules.impl.Render.Particles;
import dev.Mardexx.client.modules.impl.Render.Removals;
import dev.Mardexx.client.modules.impl.Render.Tracers;
import dev.Mardexx.client.modules.impl.Render.Trajectories;
import dev.Mardexx.client.modules.impl.Render.WallHack;
import dev.Mardexx.client.modules.impl.Render.WorldTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static boolean initialized;
	private static boolean loadingConfig;

	private ModuleManager() {
	}

	public static List<Module> modules() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> modules(ModuleCategory category) {
		return MODULES.stream()
				.filter(module -> module.category() == category)
				.toList();
	}

	public static void add(Module module) {
		MODULES.add(module);
	}

	public static void registerDefaults() {
		if (initialized) {
			return;
		}

		initialized = true;
		add(AimAssist.getInstance());
		add(AutoSwap.getInstance());
		add(TriggerBot.getInstance());
		add(Tap.getInstance());
		add(new Sprint());
		add(InventoryMove.getInstance());
		add(new NoJumpDelay());
		add(NoPush.getInstance());
		add(FullBright.getInstance());
		add(AspectRatio.getInstance());
		add(WorldTweaks.getInstance());
		add(AuroraSky.getInstance());
		add(CustomSwords.getInstance());
		add(ESP.getInstance());
		add(Indicator.getInstance());
		add(NameTags.getInstance());
		add(Removals.getInstance());
		add(Tracers.getInstance());
		add(Trajectories.getInstance());
		add(WallHack.getInstance());
		add(Particles.getInstance());
		add(Interface.getInstance());
		add(FakePlayer.getInstance());
		add(Helper.getInstance());
	}

	public static void loadConfig() {
		loadingConfig = true;
		try {
			ClientConfig.load();
		} finally {
			loadingConfig = false;
		}
	}

	public static void saveConfig() {
		if (initialized && !loadingConfig) {
			ClientConfig.save();
		}
	}

	static void moduleStateChanged(Module module, boolean enabled) {
		if (initialized && !loadingConfig) {
			Notifications.getInstance().push(module, enabled);
		}
	}

	public static void handleKey(int key, int action, boolean mouse) {
		if (Minecraft.getInstance().screen != null
				|| mouse
				|| key < 0
				|| action != GLFW.GLFW_PRESS) {
			return;
		}

		for (Module module : MODULES) {
			if (module.keyBind() == key) {
				module.toggle();
			}
			if (module.enabled()) {
				module.onKey(key, action, false);
			}
		}
	}

	public static void preTick() {
		for (Module module : MODULES) {
			if (module.enabled()) {
				module.onPreTick();
			}
		}
	}

	public static void tick() {
		for (Module module : MODULES) {
			if (module.enabled()) {
				module.onTick();
			}
		}
	}
}
