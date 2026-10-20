package dev.Mardexx.client.media;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MediaCoverManager {
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("modid", "media/current_cover");
	private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "HatredFree-MediaCover");
		thread.setDaemon(true);
		return thread;
	});

	private static volatile String requestedCover = "";
	private static volatile DynamicTexture texture;

	private MediaCoverManager() {
	}

	public static void request(String encodedCover) {
		String normalized = normalize(encodedCover);
		if (normalized.equals(requestedCover)) {
			return;
		}

		requestedCover = normalized;
		if (normalized.isEmpty()) {
			Minecraft.getInstance().execute(MediaCoverManager::clearTexture);
			return;
		}

		DECODER.execute(() -> decode(normalized));
	}

	public static ResourceLocation textureId() {
		return texture == null ? null : TEXTURE_ID;
	}

	public static void close() {
		DECODER.shutdownNow();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null) {
			minecraft.execute(MediaCoverManager::clearTexture);
		}
	}

	private static void decode(String encodedCover) {
		NativeImage image = null;
		try {
			byte[] bytes = Base64.getDecoder().decode(encodedCover);
			image = NativeImage.read(bytes);
			NativeImage decoded = image;
			Minecraft.getInstance().execute(() -> installTexture(encodedCover, decoded));
		} catch (Throwable ignored) {
			if (image != null) {
				image.close();
			}
			if (encodedCover.equals(requestedCover)) {
				requestedCover = "";
				Minecraft.getInstance().execute(MediaCoverManager::clearTexture);
			}
		}
	}

	private static void installTexture(String encodedCover, NativeImage image) {
		if (!encodedCover.equals(requestedCover)) {
			image.close();
			return;
		}

		clearTexture();
		DynamicTexture newTexture = null;
		try {
			newTexture = new DynamicTexture(image);
			newTexture.setFilter(true, false);
			Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, newTexture);
			texture = newTexture;
		} catch (Throwable ignored) {
			if (newTexture != null) {
				newTexture.close();
			} else {
				image.close();
			}
			texture = null;
			requestedCover = "";
		}
	}

	private static void clearTexture() {
		if (texture == null) {
			return;
		}
		try {
			Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
		} catch (Throwable ignored) {
			texture.close();
		}
		texture = null;
	}

	private static String normalize(String encodedCover) {
		if (encodedCover == null || encodedCover.isBlank()) {
			return "";
		}
		int comma = encodedCover.indexOf(',');
		return encodedCover.startsWith("data:") && comma >= 0 ? encodedCover.substring(comma + 1) : encodedCover;
	}
}
