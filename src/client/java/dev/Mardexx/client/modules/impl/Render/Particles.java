package dev.Mardexx.client.modules.impl.Render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.PacketEvent;
import dev.Mardexx.api.event.events.player.world.AttackEvent;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;
import dev.Mardexx.client.theme.ThemeManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Particles extends Module implements QuickImports {
	private static final Particles INSTANCE = new Particles();
	private final BooleanSetting onAttack = new BooleanSetting("on_attack", "On Attack", "Spawns particles when you attack an entity.", true);
	private final FloatSetting attackCount = slider("attack_count", "Attack Count", 5.0F, 20.0F, 50.0F, 1.0F, 0, onAttack);
	private final BooleanSetting onTotem = new BooleanSetting("on_totem", "On Totem", "Spawns particles when an entity pops a totem.", true);
	private final FloatSetting totemCount = slider("totem_count", "Totem Count", 20.0F, 75.0F, 150.0F, 1.0F, 0, onTotem);
	private final BooleanSetting onProjectile = new BooleanSetting("on_projectile", "On Throw", "Adds trails to pearls, arrows and tridents.", true);
	private final FloatSetting projectileCount = slider("projectile_count", "Throw Count", 1.0F, 3.0F, 8.0F, 1.0F, 0, onProjectile);
	private final BooleanSetting inWorld = new BooleanSetting("in_world", "In World", "Spawns ambient particles around the player.", true);
	private final FloatSetting worldCount = slider("world_count", "World Count", 2.0F, 10.0F, 60.0F, 1.0F, 0, inWorld);
	private final BooleanSetting onMove = new BooleanSetting("on_move", "On Move", "Spawns particles around the moving player.", true);
	private final EnumSetting<ParticleType> particleType = new EnumSetting<>("particle_type", "Particle Type", "Selects the particle texture.", ParticleType.CIRCLE);
	private final FloatSetting size = new FloatSetting("size", "Size", "Particle size.", 0.0F, 0.5F, 1.0F).step(0.1F).precision(1);
	private final BooleanSetting glow = new BooleanSetting("glow", "Glow", "Adds a soft glow around particles.", true);
	private final FloatSetting glowStrength = slider("glow_strength", "Glow Strength", 0.0F, 0.5F, 1.0F, 0.1F, 1, glow);
	private final BooleanSetting rotation = new BooleanSetting("rotation", "Rotation", "Rotates particles while they move.", true);

	private final List<Particle> attackParticles = new ArrayList<>();
	private final List<Particle> totemParticles = new ArrayList<>();
	private final List<Particle> trailParticles = new ArrayList<>();
	private final List<Particle> worldParticles = new ArrayList<>();
	private Entity totemEntity;
	private int totemLeft;
	private int totemTotal;
	private long totemStarted;
	private long lastRenderNanos = System.nanoTime();
	private ClientLevel activeLevel;

	private Particles() {
		super("Particles", ModuleCategory.RENDER, "Custom textured particles around entities and the world.");
		register(onAttack, attackCount, onTotem, totemCount, onProjectile, projectileCount, inWorld, worldCount,
				onMove, particleType, size, glow, glowStrength, rotation);
		AttackEvent.getInstance().subscribe(new Listener<>(event -> {
			if (enabled() && onAttack.enabled()) spawnAttack(event.entity());
		}));
		PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacket));
		Render3D.add(this::renderWorld);
	}

	private static FloatSetting slider(String name, String display, float min, float value, float max,
			float step, int precision, BooleanSetting parent) {
		return (FloatSetting) new FloatSetting(name, display, "", min, value, max).step(step).precision(precision).visibleWhen(parent::enabled);
	}

	@Override
	protected void onEnable() {
		clear();
		activeLevel = mc.level;
	}

	@Override
	protected void onDisable() {
		clear();
	}

	@Override
	public void onTick() {
		if (mc.level == null || mc.player == null) return;
		if (activeLevel != mc.level) {
			clear();
			activeLevel = mc.level;
		}

		if (onProjectile.enabled()) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (!(entity instanceof ThrownEnderpearl || entity instanceof AbstractArrow || entity instanceof ThrownTrident)) continue;
				if (entity.getDeltaMovement().lengthSqr() < 1.0E-5D) continue;
				for (int index = 0; index < Math.round(projectileCount.floatValue()); index++) {
					spawn(trailParticles, entity.position().add(random(-0.1, 0.1), random(-0.1, 0.1), random(-0.1, 0.1)),
							new Vec3(random(-0.15, 0.15), random(-0.15, 0.15), random(-0.15, 0.15)), true, false);
				}
			}
		}

		if (onMove.enabled() && mc.player.position().distanceToSqr(new Vec3(mc.player.xOld, mc.player.yOld, mc.player.zOld)) > 1.0E-6D) {
			spawn(trailParticles, mc.player.position().add(random(-0.1, 0.1), random(0.0, mc.player.getBbHeight()), random(-0.1, 0.1)),
					new Vec3(random(-0.3, 0.3), random(-0.03, 0.3), random(-0.3, 0.3)), true, false);
		}

		if (inWorld.enabled()) {
			for (int index = 0; index < Math.round(worldCount.floatValue()); index++) {
				int x = Mth.floor(mc.player.getX() + random(-24.0, 24.0));
				int z = Mth.floor(mc.player.getZ() + random(-24.0, 24.0));
				int ground = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				double y = Math.max(ground + 1.0, mc.player.getY() + random(mc.player.getBbHeight(), 24.0));
				spawn(worldParticles, new Vec3(x + random(-3.0, 3.0), y, z + random(-3.0, 3.0)),
						new Vec3(mc.player.getDeltaMovement().x + random(-0.6, 0.6), random(-0.01, 0.44),
								mc.player.getDeltaMovement().z + random(-0.6, 0.6)), true, false);
			}
		}

		spawnScheduledTotemParticles();
		removeExpired(attackParticles, 6000L);
		removeExpired(totemParticles, 3500L);
		removeExpired(trailParticles, 3000L);
		removeExpired(worldParticles, 3500L);
	}

	private void handlePacket(PacketEvent.PacketEventData event) {
		if (!enabled() || !onTotem.enabled() || !event.isReceive() || mc.level == null
				|| !(event.packet() instanceof ClientboundEntityEventPacket packet) || packet.getEventId() != 35) return;
		Entity entity = packet.getEntity(mc.level);
		if (entity != null) {
			totemEntity = entity;
			totemTotal = Math.round(totemCount.floatValue());
			totemLeft = totemTotal;
			totemStarted = System.currentTimeMillis();
		}
	}

	private void spawnScheduledTotemParticles() {
		if (totemLeft <= 0 || totemEntity == null) return;
		long elapsed = System.currentTimeMillis() - totemStarted;
		int expected = elapsed >= 1500L ? totemTotal : (int) (elapsed / 1500.0D * totemTotal);
		int count = Math.min(totemLeft, Math.max(0, expected - (totemTotal - totemLeft)));
		if (elapsed >= 1500L) count = totemLeft;
		for (int index = 0; index < count; index++) {
			double angle = random(0.0, Math.PI * 2.0);
			double speed = random(2.5, 4.0) * 0.1;
			spawn(totemParticles,
					totemEntity.position().add(random(-0.2, 0.2), random(0.1, totemEntity.getBbHeight()), random(-0.2, 0.2)),
					new Vec3(Math.cos(angle) * speed, random(0.0, totemEntity.getBbHeight() / 4.0), Math.sin(angle) * speed), false, true);
			totemLeft--;
		}
		if (totemLeft <= 0 || (totemEntity.isRemoved() && mc.level.getEntity(totemEntity.getId()) == null)) totemEntity = null;
	}

	private void spawnAttack(Entity target) {
		if (target == null) return;
		for (int index = 0; index < Math.round(attackCount.floatValue()); index++) {
			spawn(attackParticles,
					target.position().add(random(-0.035, 0.035), random(0.1, target.getBbHeight()), random(-0.035, 0.035)),
					new Vec3(random(-0.9, 0.9), random(-1.1, 0.55), random(-0.9, 0.9)), false, false);
		}
	}

	private void spawn(List<Particle> destination, Vec3 position, Vec3 velocity, boolean world, boolean totem) {
		float particleSize = 0.05F + size.floatValue() * 0.2F;
		int color = totem ? totemColor() : themedColor(destination.size());
		destination.add(new Particle(particleType.value(), position.add(0.0, particleSize, 0.0),
				totem ? velocity.scale(0.08) : velocity.scale(0.05), color, particleSize, world));
	}

	private void renderWorld(WorldRenderContext context) {
		if (!enabled() || mc.level == null || mc.player == null) return;
		long now = System.nanoTime();
		double delta = Math.min(0.05D, Math.max(0.0D, (now - lastRenderNanos) / 1_000_000_000.0D));
		lastRenderNanos = now;
		update(attackParticles, delta);
		update(totemParticles, delta);
		update(trailParticles, delta);
		update(worldParticles, delta);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);

		if (glow.enabled()) {
			drawList(context, attackParticles, 6000L, 2000L, 2500L, true);
			drawList(context, totemParticles, 3500L, 1000L, 1500L, true);
			drawList(context, worldParticles, 3500L, 1000L, 1500L, true);
			drawList(context, trailParticles, 3000L, 750L, 1000L, true);
		}
		drawList(context, attackParticles, 6000L, 2000L, 2500L, false);
		drawList(context, totemParticles, 3500L, 1000L, 1500L, false);
		drawList(context, worldParticles, 3500L, 1000L, 1500L, false);
		drawList(context, trailParticles, 3000L, 750L, 1000L, false);

		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	private void drawList(WorldRenderContext context, List<Particle> particles, long lifespan, long fadeIn, long fadeOutStart, boolean glowPass) {
		if (particles.isEmpty()) return;
		Map<ParticleType, List<Particle>> grouped = new EnumMap<>(ParticleType.class);
		for (Particle particle : particles) grouped.computeIfAbsent(glowPass ? ParticleType.BLOOM : particle.type, ignored -> new ArrayList<>()).add(particle);
		for (Map.Entry<ParticleType, List<Particle>> entry : grouped.entrySet()) {
			if (entry.getValue().stream().noneMatch(particle -> visible(particle, lifespan, fadeIn, fadeOutStart, glowPass))) {
				continue;
			}
			RenderSystem.setShaderTexture(0, entry.getKey().texture);
			BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			for (Particle particle : entry.getValue()) addParticleQuad(context, buffer, particle, lifespan, fadeIn, fadeOutStart, glowPass);
			MeshData mesh = buffer.build();
			if (mesh != null) BufferUploader.drawWithShader(mesh);
		}
	}

	private boolean visible(Particle particle, long lifespan, long fadeIn, long fadeOutStart, boolean glowPass) {
		long age = System.currentTimeMillis() - particle.created;
		float alpha = Mth.clamp(age / (float) fadeIn, 0.0F, 1.0F);
		if (age > fadeOutStart) alpha *= Mth.clamp((lifespan - age) / (float) Math.max(1L, lifespan - fadeOutStart), 0.0F, 1.0F);
		if (glowPass) alpha *= glowStrength.floatValue();
		return alpha > 0.001F;
	}

	private void addParticleQuad(WorldRenderContext context, BufferBuilder buffer, Particle particle, long lifespan,
			long fadeIn, long fadeOutStart, boolean glowPass) {
		long age = System.currentTimeMillis() - particle.created;
		float alpha = Mth.clamp(age / (float) fadeIn, 0.0F, 1.0F);
		if (age > fadeOutStart) alpha *= Mth.clamp((lifespan - age) / (float) Math.max(1L, lifespan - fadeOutStart), 0.0F, 1.0F);
		if (glowPass) alpha *= glowStrength.floatValue();
		if (alpha <= 0.001F) return;

		PoseStack pose = context.matrixStack();
		Vec3 camera = context.camera().getPosition();
		pose.pushPose();
		pose.translate(particle.position.x - camera.x, particle.position.y - camera.y, particle.position.z - camera.z);
		pose.mulPose(context.camera().rotation());
		if (rotation.enabled()) pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(particle.rotation));
		Matrix4f matrix = pose.last().pose();
		float half = particle.size * (glowPass ? 3.0F : 1.0F) * alpha;
		int color = ((int) (alpha * 255.0F) << 24) | (particle.color & 0x00FFFFFF);
		buffer.addVertex(matrix, -half, -half, 0.0F).setUv(0.0F, 1.0F).setColor(color);
		buffer.addVertex(matrix, half, -half, 0.0F).setUv(1.0F, 1.0F).setColor(color);
		buffer.addVertex(matrix, half, half, 0.0F).setUv(1.0F, 0.0F).setColor(color);
		buffer.addVertex(matrix, -half, half, 0.0F).setUv(0.0F, 0.0F).setColor(color);
		pose.popPose();
	}

	private void update(List<Particle> particles, double delta) {
		for (Particle particle : particles) particle.update(delta);
	}

	private static void removeExpired(List<Particle> particles, long lifespan) {
		long now = System.currentTimeMillis();
		particles.removeIf(particle -> now - particle.created >= lifespan);
	}

	private void clear() {
		attackParticles.clear();
		totemParticles.clear();
		trailParticles.clear();
		worldParticles.clear();
		totemEntity = null;
		totemLeft = 0;
		totemTotal = 0;
	}

	private static double random(double min, double max) { return ThreadLocalRandom.current().nextDouble(min, max); }

	private static int themedColor(int index) {
		int accent = ThemeManager.accent();
		float[] hsb = Color.RGBtoHSB((accent >>> 16) & 255, (accent >>> 8) & 255, accent & 255, null);
		float brightness = 0.72F + 0.28F * (float) Math.sin((System.currentTimeMillis() + index * 100L) / 550.0D);
		return 0xFF000000 | Color.HSBtoRGB(hsb[0], hsb[1], Mth.clamp(brightness, 0.25F, 1.0F)) & 0x00FFFFFF;
	}

	private static int totemColor() {
		int[] colors = {0xFFDDDA7F, 0xFF7FDD90, 0xFFFFD700, 0xFF32CD32};
		return colors[ThreadLocalRandom.current().nextInt(colors.length)];
	}

	private static boolean solid(Vec3 position) {
		if (mc.level == null) return false;
		BlockPos blockPos = BlockPos.containing(position);
		return mc.level.getBlockState(blockPos).isCollisionShapeFullBlock(mc.level, blockPos);
	}

	public static Particles getInstance() { return INSTANCE; }

	public enum ParticleType {
		HEART("heart"), STAR("star"), BLOOM("glow"), CIRCLE("circle"),
		SNOW("snowflake"), DOLLAR("dollar"), PUMPKIN("pumpkin");

		private final ResourceLocation texture;
		ParticleType(String name) { texture = ResourceLocation.fromNamespaceAndPath("modid", "textures/particles/" + name + ".png"); }
	}

	private static final class Particle {
		private final ParticleType type;
		private final int color;
		private final float size;
		private final boolean world;
		private final long created = System.currentTimeMillis();
		private Vec3 position;
		private Vec3 velocity;
		private float rotation = (float) random(0.0, 360.0);

		private Particle(ParticleType type, Vec3 position, Vec3 velocity, int color, float size, boolean world) {
			this.type = type;
			this.position = position;
			this.velocity = velocity;
			this.color = color;
			this.size = size;
			this.world = world;
		}

		private void update(double delta) {
			double ticks = delta * 120.0D;
			if (ticks <= 0.0D) return;
			double bounce = 0.85D;
			double gravity = world ? 0.0001D : 0.0005D;
			Vec3 xNext = position.add(velocity.x * ticks, 0.0, 0.0);
			if (solid(xNext)) velocity = new Vec3(-velocity.x * bounce, velocity.y, velocity.z); else position = xNext;
			Vec3 yNext = position.add(0.0, velocity.y * ticks, 0.0);
			if (solid(yNext)) velocity = new Vec3(velocity.x, -velocity.y * bounce, velocity.z); else position = yNext;
			Vec3 zNext = position.add(0.0, 0.0, velocity.z * ticks);
			if (solid(zNext)) velocity = new Vec3(velocity.x, velocity.y, -velocity.z * bounce); else position = zNext;
			velocity = velocity.subtract(0.0, gravity * ticks, 0.0);
			rotation += (float) (2.0D * ticks);
		}
	}
}
