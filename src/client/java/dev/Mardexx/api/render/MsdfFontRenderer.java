package dev.Mardexx.api.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MsdfFontRenderer {
	private static final int SPACE_CODE = 32;
	private static final float FALLBACK_SPACE_ADVANCE = 0.33F;
	private static final ShaderProgram MSDF_SHADER = new ShaderProgram(
			ResourceLocation.fromNamespaceAndPath("modid", "core/fonts/msdf_font"),
			DefaultVertexFormat.POSITION_TEX_COLOR,
			ShaderDefines.EMPTY
	);
	private static final List<TextCommand> PENDING = new ArrayList<>();
	private static boolean batching;
	private static int pendingCount;
	private static volatile int resourceGeneration;

	private final ResourceLocation atlasLocation;
	private final Map<Integer, Glyph> glyphs = new HashMap<>();
	private final float textureWidth;
	private final float textureHeight;
	private final float lineHeight;
	private final float baseLine;
	private final float distanceRange;
	private int filteredGeneration = -1;

	public MsdfFontRenderer(ResourceLocation atlasLocation, ResourceLocation dataLocation) {
		this.atlasLocation = atlasLocation;

		JsonObject root = readJson(dataLocation);
		if (root.has("common")) {
			JsonObject common = root.getAsJsonObject("common");
			JsonObject distanceField = root.getAsJsonObject("distanceField");

			this.textureWidth = common.get("scaleW").getAsFloat();
			this.textureHeight = common.get("scaleH").getAsFloat();
			this.lineHeight = common.get("lineHeight").getAsFloat();
			this.baseLine = common.get("base").getAsFloat();
			this.distanceRange = distanceField.get("distanceRange").getAsFloat();

			JsonArray chars = root.getAsJsonArray("chars");
			for (int index = 0; index < chars.size(); index++) {
				JsonObject glyph = chars.get(index).getAsJsonObject();
				Glyph parsedGlyph = new Glyph(
						glyph.get("id").getAsInt(),
						glyph.get("x").getAsFloat(),
						glyph.get("y").getAsFloat(),
						glyph.get("width").getAsFloat(),
						glyph.get("height").getAsFloat(),
						glyph.get("xoffset").getAsFloat(),
						glyph.get("yoffset").getAsFloat(),
						glyph.get("width").getAsFloat(),
						glyph.get("height").getAsFloat(),
						glyph.get("xadvance").getAsFloat()
				);
				this.glyphs.put(parsedGlyph.code, parsedGlyph);
			}
		} else {
			JsonObject atlas = root.getAsJsonObject("atlas");
			JsonObject metrics = root.getAsJsonObject("metrics");

			this.textureWidth = atlas.get("width").getAsFloat();
			this.textureHeight = atlas.get("height").getAsFloat();
			this.lineHeight = metrics.get("lineHeight").getAsFloat();
			this.baseLine = 0.0F;
			this.distanceRange = atlas.get("distanceRange").getAsFloat();

			JsonArray glyphs = root.getAsJsonArray("glyphs");
			for (int index = 0; index < glyphs.size(); index++) {
				JsonObject glyph = glyphs.get(index).getAsJsonObject();
				if (!glyph.has("planeBounds") || !glyph.has("atlasBounds")) {
					continue;
				}

				JsonObject planeBounds = glyph.getAsJsonObject("planeBounds");
				JsonObject atlasBounds = glyph.getAsJsonObject("atlasBounds");
				float atlasLeft = atlasBounds.get("left").getAsFloat();
				float atlasTop = atlasBounds.get("top").getAsFloat();
				float atlasRight = atlasBounds.get("right").getAsFloat();
				float atlasBottom = atlasBounds.get("bottom").getAsFloat();
				float planeLeft = planeBounds.get("left").getAsFloat();
				float planeTop = planeBounds.get("top").getAsFloat();
				float planeRight = planeBounds.get("right").getAsFloat();
				float planeBottom = planeBounds.get("bottom").getAsFloat();
				Glyph parsedGlyph = new Glyph(
						glyph.get("unicode").getAsInt(),
						atlasLeft,
						textureHeight - atlasTop,
						atlasRight - atlasLeft,
						atlasTop - atlasBottom,
						planeLeft,
						-planeTop,
						planeRight - planeLeft,
						planeTop - planeBottom,
						glyph.get("advance").getAsFloat()
				);
				this.glyphs.put(parsedGlyph.code, parsedGlyph);
			}
		}
	}

	public void draw(GuiGraphics guiGraphics, String text, float x, float y, float size, int color) {
		draw(guiGraphics, text, x, y, size, color, 0.0F);
	}

	public void draw(GuiGraphics guiGraphics, String text, float x, float y, float size, int color, float letterSpacing) {
		draw(guiGraphics, text, x, y, size, color, letterSpacing, 0.0F);
	}

	public void draw(GuiGraphics guiGraphics, String text, float x, float y, float size, int color, float letterSpacing, float thickness) {
		if (text == null || text.isEmpty()) {
			return;
		}


		if (batching) {
			TextCommand command;
			if (pendingCount < PENDING.size()) {
				command = PENDING.get(pendingCount);
			} else {
				command = new TextCommand();
				PENDING.add(command);
			}
			pendingCount++;
			command.set(this, guiGraphics.pose().last().pose(), text, x, y, size, color, letterSpacing, thickness);
			return;
		}

		drawImmediate(guiGraphics.pose().last().pose(), text, x, y, size, color, letterSpacing, thickness);
	}

	public static void beginBatching() {
		flushPending();
		batching = true;
	}

	public static void endBatching() {
		flushPending();
		batching = false;
	}

	public static void flushPending() {
		if (pendingCount == 0) {
			return;
		}

		int start = 0;
		while (start < pendingCount) {
			TextCommand first = PENDING.get(start);
			int end = start + 1;
			while (end < pendingCount) {
				TextCommand next = PENDING.get(end);
				if (next.renderer != first.renderer
						|| Float.floatToIntBits(next.thickness) != Float.floatToIntBits(first.thickness)) {
					break;
				}
				end++;
			}
			first.renderer.drawBatch(start, end, first.thickness);
			start = end;
		}
		pendingCount = 0;
	}

	private void drawImmediate(Matrix4f matrix, String text, float x, float y, float size, int color, float letterSpacing, float thickness) {
		applyFiltering();
		setupShader(0xFFFFFFFF, thickness);
		RenderSystem.setShaderTexture(0, atlasLocation);

		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		if (!appendVertices(bufferBuilder, matrix, text, x, y, size, color, letterSpacing)) {
			RenderSystem.setShaderTexture(0, 0);
			return;
		}

		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		RenderSystem.setShaderTexture(0, 0);
	}

	private void drawBatch(int start, int end, float thickness) {
		applyFiltering();
		setupShader(0xFFFFFFFF, thickness);
		RenderSystem.setShaderTexture(0, atlasLocation);

		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		boolean hasVertices = false;
		for (int index = start; index < end; index++) {
			TextCommand command = PENDING.get(index);
			hasVertices |= appendVertices(
					bufferBuilder,
					command.matrix,
					command.text,
					command.x,
					command.y,
					command.size,
					command.color,
					command.letterSpacing
			);
		}

		if (hasVertices) {
			BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		}
		RenderSystem.setShaderTexture(0, 0);
	}



	private boolean appendVertices(BufferBuilder bufferBuilder, Matrix4f matrix, String text, float x, float y,
			float size, int color, float letterSpacing) {
		float scale = size / lineHeight;
		float cursorX = x;
		float drawY = y - baseLine * scale;
		boolean hasVertices = false;
		int c = Render2D.adjustColor(color);

		for (int index = 0; index < text.length(); index++) {
			int code = text.charAt(index);
			Glyph glyph = glyphs.get(code);
			if (glyph == null) {
				cursorX += missingGlyphAdvance(code) * scale + letterSpacing;
				continue;
			}

			float left = cursorX + glyph.xOffset * scale;
			float top = drawY + glyph.yOffset * scale;
			float right = left + glyph.width * scale;
			float bottom = top + glyph.height * scale;
			float minU = glyph.x / textureWidth;
			float maxU = (glyph.x + glyph.atlasWidth) / textureWidth;
			float minV = glyph.y / textureHeight;
			float maxV = (glyph.y + glyph.atlasHeight) / textureHeight;

			bufferBuilder.addVertex(matrix, left, top, 0.0F).setUv(minU, minV).setColor(c);
			bufferBuilder.addVertex(matrix, left, bottom, 0.0F).setUv(minU, maxV).setColor(c);
			bufferBuilder.addVertex(matrix, right, bottom, 0.0F).setUv(maxU, maxV).setColor(c);
			bufferBuilder.addVertex(matrix, right, top, 0.0F).setUv(maxU, minV).setColor(c);
			hasVertices = true;
			cursorX += glyph.xAdvance * scale + letterSpacing;
		}

		return hasVertices;
	}

	public float width(String text, float size) {
		if (text == null || text.isEmpty()) {
			return 0.0F;
		}

		float scale = size / lineHeight;
		float width = 0.0F;
		for (int index = 0; index < text.length(); index++) {
			int code = text.charAt(index);
			Glyph glyph = glyphs.get(code);
			if (glyph != null) {
				width += glyph.xAdvance * scale;
			} else {
				width += missingGlyphAdvance(code) * scale;
			}
		}
		return width;
	}

	public float centeredX(String text, float size, float centerX) {
		return centeredX(text, size, centerX, 0.0F);
	}

	public float centeredX(String text, float size, float centerX, float letterSpacing) {
		Bounds bounds = bounds(text, size, letterSpacing);
		if (bounds == null) {
			return centerX;
		}

		return centerX - (bounds.minX + bounds.maxX) / 2.0F;
	}

	public float visualHeight(String text, float size) {
		Bounds bounds = bounds(text, size, 0.0F);
		if (bounds == null) {
			return 0.0F;
		}

		return bounds.maxY - bounds.minY;
	}

	public float visualSize(String text, float size) {
		Bounds bounds = bounds(text, size, 0.0F);
		if (bounds == null) {
			return 0.0F;
		}

		return Math.max(bounds.maxX - bounds.minX, bounds.maxY - bounds.minY);
	}

	public float centeredBaseline(String text, float size, float centerY) {
		Bounds bounds = bounds(text, size, 0.0F);
		if (bounds == null) {
			return centerY;
		}

		return centerY - (bounds.minY + bounds.maxY) / 2.0F + baseLine * (size / lineHeight);
	}

	private Bounds bounds(String text, float size, float letterSpacing) {
		if (text == null || text.isEmpty()) {
			return null;
		}

		float scale = size / lineHeight;
		float cursorX = 0.0F;
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int index = 0; index < text.length(); index++) {
			int code = text.charAt(index);
			Glyph glyph = glyphs.get(code);
			if (glyph != null) {
				minX = Math.min(minX, cursorX + glyph.xOffset * scale);
				maxX = Math.max(maxX, cursorX + (glyph.xOffset + glyph.width) * scale);
				minY = Math.min(minY, glyph.yOffset * scale);
				maxY = Math.max(maxY, (glyph.yOffset + glyph.height) * scale);
				cursorX += glyph.xAdvance * scale + letterSpacing;
			} else {
				cursorX += missingGlyphAdvance(code) * scale + letterSpacing;
			}
		}

		if (minX == Float.MAX_VALUE) {
			return null;
		}

		return new Bounds(minX, maxX, minY, maxY);
	}

	private float missingGlyphAdvance(int code) {
		if (!Character.isWhitespace(code)) {
			return 0.0F;
		}

		Glyph space = glyphs.get(SPACE_CODE);
		if (space != null) {
			return space.xAdvance;
		}

		Glyph reference = glyphs.get((int) 'n');
		if (reference == null) {
			reference = glyphs.get((int) 'a');
		}
		if (reference == null) {
			reference = glyphs.get((int) '0');
		}

		return reference == null ? lineHeight * FALLBACK_SPACE_ADVANCE : reference.xAdvance * 0.45F;
	}

	private void setupShader(int color, float glyphThickness) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		CompiledShaderProgram shader = RenderSystem.setShader(MSDF_SHADER);
		if (shader == null) {
			return;
		}

		Uniform textureSize = shader.getUniform("TextureSize");
		Uniform range = shader.getUniform("Range");
		Uniform edgeStrength = shader.getUniform("EdgeStrength");
		Uniform thickness = shader.getUniform("Thickness");
		Uniform colorUniform = shader.getUniform("Color");

		if (textureSize != null) {
			textureSize.set(textureWidth, textureHeight);
		}
		if (range != null) {
			range.set(distanceRange);
		}
		if (edgeStrength != null) {
			edgeStrength.set(0.5F);
		}
		if (thickness != null) {
			thickness.set(glyphThickness);
		}
		if (colorUniform != null) {
			colorUniform.set(
					((color >> 16) & 255) / 255.0F,
					((color >> 8) & 255) / 255.0F,
					(color & 255) / 255.0F,
					((color >> 24) & 255) / 255.0F
			);
		}
	}

	private void applyFiltering() {
		if (filteredGeneration == resourceGeneration) {
			return;
		}
		Minecraft.getInstance().getTextureManager().getTexture(atlasLocation).setFilter(true, false);
		filteredGeneration = resourceGeneration;
	}

	public static void onResourceReload() {
		resourceGeneration++;
	}

	private static JsonObject readJson(ResourceLocation location) {
		try {
			InputStream stream = null;
			if (Minecraft.getInstance() != null && Minecraft.getInstance().getResourceManager() != null) {
				try {
					stream = Minecraft.getInstance().getResourceManager().open(location);
				} catch (Exception ignored) {
				}
			}
			if (stream == null) {
				String path = "/assets/" + location.getNamespace() + "/" + location.getPath();
				stream = MsdfFontRenderer.class.getResourceAsStream(path);
			}
			if (stream == null) {
				throw new IllegalStateException("Resource not found: " + location);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to read MSDF font data: " + location, exception);
		}
	}

	private record Glyph(int code, float x, float y, float atlasWidth, float atlasHeight, float xOffset, float yOffset, float width, float height, float xAdvance) {
	}

	private record Bounds(float minX, float maxX, float minY, float maxY) {
	}

	private static final class TextCommand {
		private final Matrix4f matrix = new Matrix4f();
		private MsdfFontRenderer renderer;
		private String text;
		private float x;
		private float y;
		private float size;
		private int color;
		private float letterSpacing;
		private float thickness;

		private void set(MsdfFontRenderer renderer, Matrix4f matrix, String text, float x, float y,
				float size, int color, float letterSpacing, float thickness) {
			this.renderer = renderer;
			this.matrix.set(matrix);
			this.text = text;
			this.x = x;
			this.y = y;
			this.size = size;
			this.color = color;
			this.letterSpacing = letterSpacing;
			this.thickness = thickness;
		}
	}
}
