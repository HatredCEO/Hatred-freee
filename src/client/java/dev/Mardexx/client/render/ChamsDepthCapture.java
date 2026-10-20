package dev.Mardexx.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public final class ChamsDepthCapture {
	private static int depthTexture = -1;
	private static int textureWidth = -1;
	private static int textureHeight = -1;
	private static int textureInternalFormat = -1;
	private static boolean available;

	private ChamsDepthCapture() {
	}

	public static boolean capture() {
		available = false;
		if (!RenderSystem.isOnRenderThread()) {
			return false;
		}

		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		if (target == null || target.frameBufferId <= 0 || target.getDepthTextureId() <= 0) {
			return false;
		}

		int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		GlStateManager._activeTexture(GL13.GL_TEXTURE0);
		int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		try {
			GlStateManager._bindTexture(target.getDepthTextureId());
			int internalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
			if (!ensureTarget(target.width, target.height, internalFormat)) {
				return false;
			}

			while (GL11.glGetError() != GL11.GL_NO_ERROR) {
				// Clear stale errors so this copy can be validated independently.
			}
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
			GlStateManager._bindTexture(depthTexture);
			GL11.glCopyTexSubImage2D(
					GL11.GL_TEXTURE_2D,
					0,
					0,
					0,
					0,
					0,
					target.width,
					target.height
			);
			available = GL11.glGetError() == GL11.GL_NO_ERROR;
			return available;
		} finally {
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
			GlStateManager._bindTexture(previousTexture);
			GlStateManager._activeTexture(previousActiveTexture);
		}
	}

	public static boolean available() {
		return available;
	}

	public static void invalidate() {
		available = false;
	}

	public static int textureId() {
		return depthTexture;
	}

	private static boolean ensureTarget(int width, int height, int internalFormat) {
		if (depthTexture != -1 && textureWidth == width && textureHeight == height && textureInternalFormat == internalFormat) {
			return true;
		}

		destroyTarget();
		int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		try {
			depthTexture = GL11.glGenTextures();
			GlStateManager._bindTexture(depthTexture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
			GL11.glTexImage2D(
					GL11.GL_TEXTURE_2D,
					0,
					internalFormat,
					width,
					height,
					0,
					GL11.GL_DEPTH_COMPONENT,
					GL11.GL_FLOAT,
					(ByteBuffer) null
			);
			textureWidth = width;
			textureHeight = height;
			textureInternalFormat = internalFormat;
			return true;
		} finally {
			GlStateManager._bindTexture(previousTexture);
		}
	}

	private static void destroyTarget() {
		if (depthTexture != -1) {
			GL11.glDeleteTextures(depthTexture);
			depthTexture = -1;
		}
		textureWidth = -1;
		textureHeight = -1;
		textureInternalFormat = -1;
	}
}
