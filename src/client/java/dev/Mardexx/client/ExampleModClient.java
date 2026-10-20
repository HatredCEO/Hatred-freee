package dev.Mardexx.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.TickEvent;
import dev.Mardexx.api.event.events.client.KeyEvent;
import dev.Mardexx.api.event.events.player.other.UpdateEvent;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.api.utils.player.combat.IdealHitUtils;
import dev.Mardexx.api.utils.other.TPSTracker;
import dev.Mardexx.client.gui.ClientGuiScreen;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.media.MediaCoverManager;
import dev.Mardexx.client.media.LyricsProvider;
import dev.Mardexx.client.media.MediaSessionProvider;
import dev.Mardexx.client.modules.ModuleManager;
import dev.Mardexx.client.render.ClientRenderManager;
import dev.Mardexx.client.render.AuroraSkyRenderer;
import dev.Mardexx.client.theme.ThemeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
	private static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		ClientRenderManager.register();
		ModuleManager.registerDefaults();
		HudManager.initialize();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return ResourceLocation.fromNamespaceAndPath("modid", "render_state");
			}

			@Override
			public void onResourceManagerReload(ResourceManager resourceManager) {
				MsdfFontRenderer.onResourceReload();
				AuroraSkyRenderer.onResourceReload();
			}
		});
		ModuleManager.loadConfig();
		FriendManager.load();
		ThemeManager.load();
		IdealHitUtils.getInstance().load();
		TPSTracker.getInstance().load();
		KeyEvent.getInstance().subscribe(new Listener<>(event -> ModuleManager.handleKey(event.key(), event.action(), event.mouse())));

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.modid.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				"category.modid"
		));

		ClientTickEvents.START_CLIENT_TICK.register(client -> ModuleManager.preTick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TickEvent.getInstance().call(TickEvent.getInstance());
			UpdateEvent.getInstance().call(UpdateEvent.getInstance());
			ModuleManager.tick();
			while (openGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ClientGuiScreen());
				}
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ModuleManager.saveConfig();
			FriendManager.save();
			ThemeManager.save();
			MediaSessionProvider.INSTANCE.close();
			MediaCoverManager.close();
			LyricsProvider.INSTANCE.close();
		});
	}
}
