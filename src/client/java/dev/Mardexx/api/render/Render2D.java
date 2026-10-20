package dev.Mardexx.api.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class Render2D {
	private static final ShaderProgram ROUNDED_RECT_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/rect/rounded_rect"),
			DefaultVertexFormat.POSITION_COLOR,
			ShaderDefines.EMPTY
	);
	private static final ShaderProgram RECT_OUTLINE_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/rect/outline"),
			DefaultVertexFormat.POSITION_COLOR,
			ShaderDefines.EMPTY
	);
	private static final ShaderProgram SOFT_SHADOW_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/rect/soft_shadow"),
			DefaultVertexFormat.POSITION_COLOR,
			ShaderDefines.EMPTY
	);
	private static final ShaderProgram SOFT_SHADOW_BATCH_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/rect/soft_shadow_batch"),
			DefaultVertexFormat.POSITION_TEX_COLOR,
			ShaderDefines.EMPTY
	);
	private static final ShaderProgram INVERTED_FILLET_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/rect/inverted_fillet"),
			DefaultVertexFormat.POSITION_COLOR,
			ShaderDefines.EMPTY
	);
	private static final List<Renderer> RENDERERS = new ArrayList<>();
	private static float tickDelta;

	public static int adjustColor(int color) {
		return color;
	}

	private Render2D() {
	}

	public static void add(Renderer renderer) {
		RENDERERS.add(renderer);
	}

	public static void render(GuiGraphics guiGraphics, float tickDelta) {
		Render2D.tickDelta = tickDelta;
		for (Renderer renderer : RENDERERS) {
			renderer.render(guiGraphics);
		}
	}

	public static float tickDelta() {
		return tickDelta;
	}

	public static void rect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
		rect(guiGraphics, (float) x, (float) y, (float) width, (float) height, color);
	}

	public static void rect(GuiGraphics guiGraphics, float x, float y, float width, float height, int color) {
		int c = adjustColor(color);
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(CoreShaders.POSITION_COLOR);

		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, x + width, y + height, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, x + width, y, 0.0F).setColor(c);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

		RenderSystem.disableBlend();
	}

	public static RectBatch beginRectBatch(GuiGraphics guiGraphics) {
		MsdfFontRenderer.flushPending();
		return new RectBatch(guiGraphics);
	}

	public static SoftShadowBatch beginSoftShadowBatch(GuiGraphics guiGraphics, float radius, float spread) {
		MsdfFontRenderer.flushPending();
		return new SoftShadowBatch(guiGraphics, radius, spread);
	}

	public static void roundedRect(GuiGraphics guiGraphics, float x, float y, float width, float height, float radius, int color) {
		roundedRect(guiGraphics, x, y, width, height, radius, radius, radius, radius, color);
	}

	public static void softShadow(GuiGraphics guiGraphics, float x, float y, float width, float height, float radius, float spread, int color) {
		if (width <= 0.0F || height <= 0.0F || spread <= 0.0F) {
			return;
		}

		int c = adjustColor(color);
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		CompiledShaderProgram shader = RenderSystem.setShader(SOFT_SHADOW_SHADER);
		if (shader == null) {
			roundedRect(guiGraphics, x - spread, y - spread, width + spread * 2.0F, height + spread * 2.0F, radius + spread, c);
			RenderSystem.disableBlend();
			return;
		}

		float padding = spread + 1.0F;
		Uniform sizeUniform = shader.getUniform("Size");
		Uniform radiusUniform = shader.getUniform("Radius");
		Uniform spreadUniform = shader.getUniform("Spread");
		Uniform paddingUniform = shader.getUniform("Padding");
		if (sizeUniform != null) sizeUniform.set(width, height);
		if (radiusUniform != null) radiusUniform.set(radius);
		if (spreadUniform != null) spreadUniform.set(spread);
		if (paddingUniform != null) paddingUniform.set(padding);

		float left = x - padding;
		float top = y - padding;
		float paddedWidth = width + padding * 2.0F;
		float paddedHeight = height + padding * 2.0F;
		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.addVertex(matrix, left, top, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top, 0.0F).setColor(c);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		RenderSystem.disableBlend();
	}

	public static void roundedRect(GuiGraphics guiGraphics, float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
		int c = adjustColor(color);
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		CompiledShaderProgram shader = RenderSystem.setShader(ROUNDED_RECT_SHADER);
		if (shader == null) {
			rect(guiGraphics, Math.round(x), Math.round(y), Math.round(width), Math.round(height), c);
			RenderSystem.disableBlend();
			return;
		}

		Uniform sizeUniform = shader.getUniform("Size");
		Uniform radiusUniform = shader.getUniform("Radius");
		Uniform smoothnessUniform = shader.getUniform("Smoothness");

		float smoothness = 1.0F;
		if (sizeUniform != null) {
			sizeUniform.set(width, height);
		}
		if (radiusUniform != null) {
			radiusUniform.set(topLeft, bottomLeft, topRight, bottomRight);
		}
		if (smoothnessUniform != null) {
			smoothnessUniform.set(smoothness);
		}

		float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
		float verticalPadding = smoothness / 2.0F + smoothness;
		float left = x - horizontalPadding / 2.0F;
		float top = y - verticalPadding / 2.0F;
		float paddedWidth = width + horizontalPadding;
		float paddedHeight = height + verticalPadding;

		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.addVertex(matrix, left, top, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top, 0.0F).setColor(c);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

		RenderSystem.disableBlend();
	}

	public static void texture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
		texturedQuad(guiGraphics, texture, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF);
	}

	public static void item(GuiGraphics guiGraphics, ItemStack stack, float x, float y, float scale, int color) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		float a = ((color >>> 24) & 0xFF) / 255.0F;
		float r = ((color >>> 16) & 0xFF) / 255.0F;
		float g = ((color >>> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
		item(guiGraphics, stack, x, y, scale);
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void item(GuiGraphics guiGraphics, ItemStack stack, float x, float y, float scale) {
		if (stack.isEmpty()) {
			return;
		}

		ResolvedItemSprite resolved = itemSprite(stack);
		if (resolved == null) {
			return;
		}

		TextureAtlasSprite sprite = resolved.sprite();
		float size = 16.0F * scale;
		float inset = 0.0001F;
		float minU = sprite.getU0() + inset;
		float minV = sprite.getV0() + inset;
		float maxU = sprite.getU1() - inset;
		float maxV = sprite.getV1() - inset;

		if (!resolved.gui3d()) {
			texturedQuad(guiGraphics, sprite.atlasLocation(), x, y, size, size, minU, minV, maxU, maxV, 0xFFFFFFFF);
			return;
		}

		float half = size / 2.0F;
		float quarter = size / 4.0F;
		float depth = size / 16.0F;
		float top = (y - quarter) - (depth / 2.0F);
		int topColor = 0xFFFFFFFF;
		int westColor = 0xFFB2B2B2;
		int eastColor = 0xFFD8D8D8;

		texturedQuad(guiGraphics, sprite.atlasLocation(), x + half, top + quarter, x + size, top + half, x + half, top + size - quarter, x, top + half, minU, minV, maxU, maxV, topColor);
		texturedQuad(guiGraphics, sprite.atlasLocation(), x + size, y + half, x + size, y + size + depth, x + half, y + size + quarter + depth, x + half, y + half + quarter, minU, minV, maxU, maxV, westColor);
		texturedQuad(guiGraphics, sprite.atlasLocation(), x, y + size + depth, x + half, y + size + quarter + depth, x + half, y + half + quarter, x, y + half, minU, minV, maxU, maxV, eastColor);
	}

	public static boolean canRenderItem(ItemStack stack) {
		return !stack.isEmpty() && itemSprite(stack) != null;
	}

	public static void sprite(GuiGraphics guiGraphics, TextureAtlasSprite sprite, float x, float y, float width, float height, int color) {
		if (sprite == null) {
			return;
		}
		var tex = Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
		if (tex != null) {
			tex.setFilter(true, false);
		}
		float minU = sprite.getU0();
		float minV = sprite.getV0();
		float maxU = sprite.getU1();
		float maxV = sprite.getV1();
		texturedQuad(guiGraphics, sprite.atlasLocation(), x, y, width, height, minU, minV, maxU, maxV, color);
	}

	public static void texturedQuad(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, float width, float height, float minU, float minV, float maxU, float maxV, int color) {
		texturedQuad(guiGraphics, texture, x, y + height, x + width, y + height, x + width, y, x, y, minU, minV, maxU, maxV, color);
	}

	public static void texturedQuad(GuiGraphics guiGraphics, ResourceLocation texture, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float minU, float minV, float maxU, float maxV, int color) {
		int c = adjustColor(color);
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);

		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferBuilder.addVertex(matrix, x1, y1, 0.0F).setUv(minU, maxV).setColor(c);
		bufferBuilder.addVertex(matrix, x2, y2, 0.0F).setUv(maxU, maxV).setColor(c);
		bufferBuilder.addVertex(matrix, x3, y3, 0.0F).setUv(maxU, minV).setColor(c);
		bufferBuilder.addVertex(matrix, x4, y4, 0.0F).setUv(minU, minV).setColor(c);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

		RenderSystem.setShaderTexture(0, 0);
		RenderSystem.disableBlend();
	}

	public static void smoothOutline(GuiGraphics guiGraphics, float x, float y, float width, float height, float thickness, int color) {
		if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F) {
			return;
		}

		int c = adjustColor(color);
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		CompiledShaderProgram shader = RenderSystem.setShader(RECT_OUTLINE_SHADER);
		if (shader == null) {
			outline(guiGraphics, Math.round(x), Math.round(y), Math.round(width), Math.round(height), 1, c);
			RenderSystem.disableBlend();
			return;
		}

		float smoothness = 1.0F;
		float padding = thickness * 0.5F + smoothness;
		Uniform sizeUniform = shader.getUniform("Size");
		Uniform paddingUniform = shader.getUniform("Padding");
		Uniform thicknessUniform = shader.getUniform("Thickness");
		Uniform smoothnessUniform = shader.getUniform("Smoothness");
		if (sizeUniform != null) sizeUniform.set(width, height);
		if (paddingUniform != null) paddingUniform.set(padding);
		if (thicknessUniform != null) thicknessUniform.set(thickness);
		if (smoothnessUniform != null) smoothnessUniform.set(smoothness);

		float left = x - padding;
		float top = y - padding;
		float paddedWidth = width + padding * 2.0F;
		float paddedHeight = height + padding * 2.0F;
		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.addVertex(matrix, left, top, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top + paddedHeight, 0.0F).setColor(c);
		bufferBuilder.addVertex(matrix, left + paddedWidth, top, 0.0F).setColor(c);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		RenderSystem.disableBlend();
	}

	private static ResolvedItemSprite itemSprite(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemStackRenderState state = new ItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, false, minecraft.level, null, 0);
		return state.isEmpty() ? null : new ResolvedItemSprite(state.pickParticleIcon(RandomSource.create(0L)), state.isGui3d());
	}

	public static void outline(GuiGraphics guiGraphics, int x, int y, int width, int height, int thickness, int color) {
		rect(guiGraphics, x, y, width, thickness, color);
		rect(guiGraphics, x, y + height - thickness, width, thickness, color);
		rect(guiGraphics, x, y, thickness, height, color);
		rect(guiGraphics, x + width - thickness, y, thickness, height, color);
	}

	public static int color(int alpha, int red, int green, int blue) {
		return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
	}

	@FunctionalInterface
	public interface Renderer {
		void render(GuiGraphics guiGraphics);
	}

	public static final class RectBatch implements AutoCloseable {
		private final Matrix4f matrix;
		private final BufferBuilder bufferBuilder;
		private boolean hasVertices;
		private boolean closed;

		private RectBatch(GuiGraphics guiGraphics) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableDepthTest();
			RenderSystem.setShader(CoreShaders.POSITION_COLOR);
			matrix = new Matrix4f(guiGraphics.pose().last().pose());
			bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		}

		public void add(float x, float y, float width, float height, int color) {
			if (closed || width <= 0.0F || height <= 0.0F) {
				return;
			}
			int c = adjustColor(color);
			bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(c);
			bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setColor(c);
			bufferBuilder.addVertex(matrix, x + width, y + height, 0.0F).setColor(c);
			bufferBuilder.addVertex(matrix, x + width, y, 0.0F).setColor(c);
			hasVertices = true;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			if (hasVertices) {
				BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
			}
			RenderSystem.disableBlend();
		}
	}

	public static final class SoftShadowBatch implements AutoCloseable {
		private final GuiGraphics guiGraphics;
		private final Matrix4f matrix;
		private final BufferBuilder bufferBuilder;
		private final float radius;
		private final float spread;
		private final float padding;
		private final boolean available;
		private boolean hasVertices;
		private boolean closed;

		private SoftShadowBatch(GuiGraphics guiGraphics, float radius, float spread) {
			this.guiGraphics = guiGraphics;
			this.radius = radius;
			this.spread = spread;
			this.padding = spread + 1.0F;

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableDepthTest();
			CompiledShaderProgram shader = RenderSystem.setShader(SOFT_SHADOW_BATCH_SHADER);
			available = shader != null;
			if (available) {
				Uniform radiusUniform = shader.getUniform("Radius");
				Uniform spreadUniform = shader.getUniform("Spread");
				Uniform paddingUniform = shader.getUniform("Padding");
				if (radiusUniform != null) radiusUniform.set(radius);
				if (spreadUniform != null) spreadUniform.set(spread);
				if (paddingUniform != null) paddingUniform.set(padding);
				matrix = new Matrix4f(guiGraphics.pose().last().pose());
				bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			} else {
				matrix = null;
				bufferBuilder = null;
			}
		}

		public void add(float x, float y, float width, float height, int color) {
			if (closed || width <= 0.0F || height <= 0.0F) {
				return;
			}
			int c = adjustColor(color);
			if (!available) {
				softShadow(guiGraphics, x, y, width, height, radius, spread, c);
				return;
			}

			float left = x - padding;
			float top = y - padding;
			float paddedWidth = width + padding * 2.0F;
			float paddedHeight = height + padding * 2.0F;
			bufferBuilder.addVertex(matrix, left, top, 0.0F).setUv(width, height).setColor(c);
			bufferBuilder.addVertex(matrix, left, top + paddedHeight, 0.0F).setUv(width, height).setColor(c);
			bufferBuilder.addVertex(matrix, left + paddedWidth, top + paddedHeight, 0.0F).setUv(width, height).setColor(c);
			bufferBuilder.addVertex(matrix, left + paddedWidth, top, 0.0F).setUv(width, height).setColor(c);
			hasVertices = true;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			if (available && hasVertices) {
				BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
			}
			RenderSystem.disableBlend();
		}
	}

	public static void invertedFillet(GuiGraphics guiGraphics, float x, float y, float width, float height, float side, int color) {
		if (width <= 0.0F || height <= 0.0F) return;
		MsdfFontRenderer.flushPending();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		CompiledShaderProgram shader = RenderSystem.setShader(INVERTED_FILLET_SHADER);
		if (shader != null) {
			Uniform sizeUniform = shader.getUniform("Size");
			Uniform sideUniform = shader.getUniform("Side");
			if (sizeUniform != null) sizeUniform.set(width, height);
			if (sideUniform != null) sideUniform.set(side);

			Matrix4f matrix = guiGraphics.pose().last().pose();
			BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			int adj = adjustColor(color);
			bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(adj);
			bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setColor(adj);
			bufferBuilder.addVertex(matrix, x + width, y + height, 0.0F).setColor(adj);
			bufferBuilder.addVertex(matrix, x + width, y, 0.0F).setColor(adj);
			BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		}
		RenderSystem.disableBlend();
	}

	public static void attachedTabBody(GuiGraphics guiGraphics, float x, float y, float width, float height, float cornerRadius, float filletRadius, int bgColor) {
		// 1. Fill main body
		roundedRect(guiGraphics, x, y, width, height, 0.0F, 0.0F, cornerRadius, cornerRadius, bgColor);

		// 2. Fill left and right inverted fillets via anti-aliased shader
		invertedFillet(guiGraphics, x - filletRadius, y, filletRadius, filletRadius, 0.0F, bgColor);
		invertedFillet(guiGraphics, x + width, y, filletRadius, filletRadius, 1.0F, bgColor);
	}

	public static void attachedTabRightBody(GuiGraphics guiGraphics, float x, float y, float width, float height, float cornerRadius, float filletRadius, int bgColor) {
		// 1. Fill main body with right corners rounded (top-right and bottom-right)
		roundedRect(guiGraphics, x, y, width, height, 0.0F, cornerRadius, cornerRadius, 0.0F, bgColor);

		// 2. Fill top and bottom inverted fillets
		if (filletRadius > 0.0F) {
			invertedFillet(guiGraphics, x, y - filletRadius, filletRadius, filletRadius, 3.0F, bgColor);
			invertedFillet(guiGraphics, x, y + height, filletRadius, filletRadius, 1.0F, bgColor);
		}
	}

	private record ResolvedItemSprite(TextureAtlasSprite sprite, boolean gui3d) {
	}
}
