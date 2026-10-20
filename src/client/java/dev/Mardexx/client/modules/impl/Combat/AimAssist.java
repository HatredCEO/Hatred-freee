package dev.Mardexx.client.modules.impl.Combat;

import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.player.world.AttackEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AimAssist extends Module implements QuickImports {
	private static final AimAssist INSTANCE = new AimAssist();
	private static final double HORIZONTAL_PRIORITY_PADDING = 0.1D;

	private final BooleanSetting onlyWeapons = new BooleanSetting("only_weapons", "Only Weapons", "Only aims while holding a weapon or tool.", false);
	private final BooleanSetting throughWalls = new BooleanSetting("through_walls", "Through Walls", "Allows aiming through walls.", false);
	private final EnumBooleanMapSetting<TargetFilter> targets = EnumBooleanMapSetting.create(
			"targets",
			"Targets",
			"Selects valid aim targets.",
			TargetFilter.class,
			defaults -> {
				defaults.put(TargetFilter.PLAYERS, true);
				defaults.put(TargetFilter.UNARMED, true);
				defaults.put(TargetFilter.ANIMALS, true);
				defaults.put(TargetFilter.MONSTERS, true);
			}
	);
	private final BooleanSetting lastHit = new BooleanSetting("last_hit", "Last Hit", "Keeps aiming at the last attacked target.", false);
	private final FloatSetting forgetDistance = new FloatSetting(
			"forget_distance",
			"Forget Distance",
			"Maximum distance a remembered target can move away before being forgotten.",
			2.0F,
			6.0F,
			15.0F
	).step(0.1F).precision(1).suffix("m");
	private final BooleanSetting sortByFov = new BooleanSetting("sort_by_fov", "Sort By FOV", "Prioritizes targets closest to your crosshair.", true);
	private final BooleanSetting aimY = new BooleanSetting("aim_y", "Aim Y", "Allows vertical aim movement.", true);
	private final FloatSetting distance = new FloatSetting("distance", "Distance", "Maximum aim distance.", 2.0F, 3.7F, 7.0F).step(0.1F).precision(1).suffix("m");
	private final FloatSetting fov = new FloatSetting("fov", "FOV", "Maximum aim field of view.", 1.0F, 90.0F, 360.0F).step(1.0F).precision(0).suffix("deg");
	private final FloatSetting horizontalSpeed = new FloatSetting("horizontal_speed", "Horizontal Speed", "Horizontal rotation speed.", 0.0F, 180.0F, 400.0F).step(1.0F).precision(0);
	private final FloatSetting verticalSpeed = new FloatSetting("vertical_speed", "Vertical Speed", "Vertical rotation speed.", 0.0F, 150.0F, 400.0F).step(1.0F).precision(0);
	private final FloatSetting smoothing = new FloatSetting("smoothing", "Smoothing", "Smooths rotation transitions.", 0.0F, 3.0F, 15.0F).step(0.5F).precision(1);
	private final EnumSetting<AimMode> aimMode = new EnumSetting<>("aim_mode", "Aim Mode", "Selects rotation style.", AimMode.NORMAL);
	private final EnumSetting<AimPoint> aimPoint = new EnumSetting<>("aim_point", "Aim Point", "Selects target point.", AimPoint.TORSO);
	private final BooleanSetting hitboxes = new BooleanSetting("hitboxes", "Hitboxes", "Enables hitbox targeting and expansion.", true);
	private final FloatSetting hitboxSize = new FloatSetting("hitbox_size", "Hitbox Size", "Hitbox expansion percentage.", 0.0F, 20.0F, 100.0F).step(1.0F).precision(0).suffix("%");
	private final BooleanSetting humanRotation = new BooleanSetting("human_rotation", "Human Rotation", "Adds inertia and jitter.", true);
	private final FloatSetting inertia = new FloatSetting("inertia", "Inertia", "Human rotation inertia.", 0.01F, 0.1F, 0.5F).step(0.01F).precision(2);
	private final FloatSetting jitter = new FloatSetting("jitter", "Jitter", "Human rotation jitter.", 0.0F, 0.4F, 2.0F).step(0.1F).precision(1);
	private final FloatSetting acceleration = new FloatSetting("acceleration", "Acceleration", "Human rotation acceleration.", 0.01F, 0.08F, 0.3F).step(0.01F).precision(2);
	private final FloatSetting maxOffset = new FloatSetting("max_offset", "Max Offset", "Maximum human rotation offset.", 0.5F, 6.0F, 20.0F).step(0.5F).precision(1);

	private final SecureRandom random = new SecureRandom();
	private final List<Entity> sortedEntities = new ArrayList<>();
	private final float[] recentFactors = new float[5];
	private EventListener attackListener;
	private LivingEntity target;
	private float previousYaw = Float.MAX_VALUE;
	private float previousPitch = Float.MAX_VALUE;
	private float lastFactor;
	private float pitchVelocity;
	private float pitchOffset;
	private float yawVelocity;
	private int recentFactorIndex;
	private int patternIndex;
	private long lastRotationTime;
	private long lastModeSwitchTime;
	private long lastFrameNano;

	private AimAssist() {
		super("Aim Assist", ModuleCategory.COMBAT, "Smoothly aims at valid targets.");
		forgetDistance.visibleWhen(lastHit::enabled).displayAfter(lastHit);
		register(
				onlyWeapons,
				targets,
				lastHit,
				forgetDistance,
				sortByFov,
				aimY,
				throughWalls,
				distance,
				fov,
				horizontalSpeed,
				verticalSpeed,
				smoothing,
				aimMode,
				aimPoint,
				hitboxes,
				hitboxSize,
				humanRotation,
				inertia,
				jitter,
				acceleration,
				maxOffset
		);
	}

	@Override
	protected void onEnable() {
		resetState();
		attackListener = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
			if (lastHit.enabled() && event.entity() instanceof LivingEntity livingEntity && matchesFilters(livingEntity) && livingEntity.distanceTo(mc.player) <= forgetDistance.floatValue()) {
				target = livingEntity;
			}
		}));
	}

	@Override
	protected void onDisable() {
		if (attackListener != null) {
			attackListener.unsubscribe();
			attackListener = null;
		}
		resetState();
	}

	public void onRenderFrame(WorldRenderContext context) {
		if (!enabled() || mc.player == null || mc.level == null || mc.screen != null) {
			return;
		}

		if (onlyWeapons.enabled() && !isWeaponOrTool(mc.player.getMainHandItem())) {
			return;
		}

		updateLastHitTargetFromCrosshair();
		float currentYaw = mc.player.getYRot();
		float currentPitch = mc.player.getXRot();
		if (previousYaw != Float.MAX_VALUE && previousPitch != Float.MAX_VALUE
				&& (Math.abs(currentYaw - previousYaw) > 0.001F || Math.abs(currentPitch - previousPitch) > 0.001F)) {
			lastRotationTime = System.currentTimeMillis();
		}

		updateTarget();
		if (target != null) {
			rotateTo(target);
		}

		previousYaw = mc.player.getYRot();
		previousPitch = mc.player.getXRot();
	}

	private void updateLastHitTargetFromCrosshair() {
		if (!lastHit.enabled() || !mc.options.keyAttack.isDown() || !(mc.hitResult instanceof EntityHitResult hitResult)) {
			return;
		}

		if (hitResult.getEntity() instanceof LivingEntity livingEntity && isValidTarget(livingEntity)) {
			target = livingEntity;
		}
	}

	private void updateTarget() {
		if (target != null && !isValidRememberedTarget(target)) {
			target = null;
			pitchVelocity = 0.0F;
			pitchOffset = 0.0F;
		}

		if (lastHit.enabled()) {
			return;
		}

		if (target != null) {
			return;
		}

		for (Entity entity : sortedEntities(mc.level.entitiesForRendering())) {
			if (entity instanceof LivingEntity livingEntity && isValidTarget(livingEntity)) {
				target = livingEntity;
				return;
			}
		}
	}

	private void rotateTo(LivingEntity target) {
		if (!isValidRememberedTarget(target)) {
			this.target = null;
			pitchVelocity = 0.0F;
			pitchOffset = 0.0F;
			return;
		}

		if (isInDeadZone(target)) {
			pitchVelocity = 0.0F;
			pitchOffset = 0.0F;
			return;
		}

		float[] rotations = rotationsTo(target);
		float targetPitch = switch (aimPoint.value()) {
			case HEAD -> rotations[1] - 4.0F;
			case TORSO -> rotations[1] + 2.0F;
			case CENTER, HITBOX, NEAREST -> rotations[1];
		};

		float yawDiff = wrapDegrees(rotations[0] - mc.player.getYRot());
		float pitchDiff = aimY.enabled() ? wrapDegrees(targetPitch - mc.player.getXRot()) : 0.0F;
		float speedYaw = Math.max(0.08F, (horizontalSpeed.floatValue() / 180.0F) * 0.4F);
		float speedPitch = Math.max(0.08F, (verticalSpeed.floatValue() / 150.0F) * 0.4F);
		float[] adjusted = switch (aimMode.value()) {
			case NORMAL -> normalRotation(yawDiff, pitchDiff, speedYaw, speedPitch);
			case SMOOTH -> smoothRotation(target, yawDiff, pitchDiff, speedYaw, speedPitch);
			case SYSTEM_DLC -> systemDlcRotation(target, yawDiff, pitchDiff);
			case ON_TIME -> onTimeRotation(target, yawDiff, pitchDiff);
			case CAKE_WORLD -> cakeWorldRotation(yawDiff, pitchDiff, speedYaw, speedPitch);
			case HVH -> hvhRotation(yawDiff, pitchDiff);
		};

		if (smoothing.floatValue() > 0.0F) {
			float smoothFactor = 1.0F / (1.0F + smoothing.floatValue() * 0.35F);
			adjusted[0] *= smoothFactor;
			adjusted[1] *= smoothFactor;
		}

		if (horizontalSpeed.floatValue() > 0.0F && Math.abs(yawDiff) > 0.01F) {
			float fixedYaw = applyGcd(mc.player.getYRot() + adjusted[0], mc.player.getYRot());
			mc.player.yRotO = fixedYaw;
			mc.player.setYRot(fixedYaw);
			mc.player.yHeadRot = fixedYaw;
		}
		if (aimY.enabled() && verticalSpeed.floatValue() > 0.0F && Math.abs(pitchDiff) > 0.01F) {
			float fixedPitch = applyGcd(Mth.clamp(mc.player.getXRot() + adjusted[1], -90.0F, 90.0F), mc.player.getXRot());
			mc.player.xRotO = fixedPitch;
			mc.player.setXRot(fixedPitch);
		}
	}

	private float[] systemDlcRotation(LivingEntity entity, float yawDiff, float pitchDiff) {
		long now = System.nanoTime();
		double delta = lastFrameNano > 0L ? (now - lastFrameNano) / 16_666_666.7D : 1.0D;
		lastFrameNano = now;
		delta = Mth.clamp(delta, 0.05D, 3.0D);

		double hypot = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
		if (hypot < 0.05D) {
			return new float[]{0.0F, 0.0F};
		}

		double factorYaw = Mth.clamp((horizontalSpeed.floatValue() / 100.0D) * 0.04D * delta, 0.01D, 0.95D);
		double factorPitch = Mth.clamp((verticalSpeed.floatValue() / 100.0D) * 0.04D * delta, 0.01D, 0.95D);

		double stepYaw = yawDiff * factorYaw;
		double stepPitch = pitchDiff * factorPitch;

		return new float[]{(float) stepYaw, (float) stepPitch};
	}

	private float[] normalRotation(float yawDiff, float pitchDiff, float speedYaw, float speedPitch) {
		float yawStep = yawDiff * speedYaw;
		float pitchStep = humanPitchStep(mc.player.getXRot() + pitchDiff * speedPitch, verticalSpeed.floatValue(), true);
		return new float[]{yawStep, pitchStep};
	}

	private float[] smoothRotation(LivingEntity entity, float yawDiff, float pitchDiff, float speedYaw, float speedPitch) {
		float distanceScale = distanceScale(entity);
		float yawScale = Math.min(1.0F, Math.abs(yawDiff) / 10.0F);
		float pitchScale = Math.min(1.0F, Math.abs(pitchDiff) / 10.0F);
		float yawStep = yawDiff * speedYaw * distanceScale * (0.4F + 0.6F * yawScale);
		float pitchStep = pitchDiff * speedPitch * distanceScale * (0.4F + 0.6F * pitchScale);
		return new float[]{clampToDiff(yawStep, yawDiff), humanPitchStep(mc.player.getXRot() + clampToDiff(pitchStep, pitchDiff), verticalSpeed.floatValue() * distanceScale, true)};
	}

	private float[] onTimeRotation(LivingEntity entity, float yawDiff, float pitchDiff) {
		float totalDiff = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));
		if (totalDiff < 1.0E-4F || random.nextFloat() < 0.02F) {
			return new float[]{0.0F, 0.0F};
		}

		boolean readyToAttack = mc.player.getAttackStrengthScale(0.0F) >= 0.87F;
		float factor = adaptiveFactor(totalDiff, entity, readyToAttack);
		recentFactors[recentFactorIndex] = factor;
		recentFactorIndex = (recentFactorIndex + 1) % recentFactors.length;
		float averageFactor = averageFactor();
		float limitScale = angleScale(totalDiff);
		float yawLimit = Math.abs(yawDiff / totalDiff) * 180.0F * limitScale;
		float pitchLimit = Math.abs(pitchDiff / totalDiff) * 180.0F * limitScale;
		float yawStep = Mth.clamp(yawDiff, -yawLimit, yawLimit);
		float pitchStep = Mth.clamp(pitchDiff, -pitchLimit, pitchLimit);
		float waveYaw = 0.0F;
		float wavePitch = 0.0F;
		if (!readyToAttack) {
			waveYaw = (float) (randomBetween(21.0F, 28.2F) * Math.sin(System.currentTimeMillis() / 20.0D));
			wavePitch = (float) (randomBetween(6.0F, 23.8F) * Math.sin(System.currentTimeMillis() / 22.0D));
		}
		return blendRotation(yawStep, pitchStep, averageFactor, waveYaw, wavePitch);
	}

	private float[] cakeWorldRotation(float yawDiff, float pitchDiff, float speedYaw, float speedPitch) {
		float factor = random.nextBoolean() ? 0.4F : 0.2F;
		float yawStep = Mth.lerp(factor, 0.0F, yawDiff * speedYaw);
		float pitchStep = Mth.lerp(factor, 0.0F, pitchDiff * speedPitch);
		return new float[]{yawStep, humanPitchStep(mc.player.getXRot() + pitchStep, verticalSpeed.floatValue(), true)};
	}

	private float[] hvhRotation(float yawDiff, float pitchDiff) {
		float totalDiff = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));
		if (totalDiff < 1.0E-4F) {
			return new float[]{0.0F, 0.0F};
		}

		float factor = adaptiveFactor(totalDiff, target, mc.player.getAttackStrengthScale(0.0F) >= 0.87F);
		float limitScale = angleScale(totalDiff);
		float yawStep = Mth.clamp(yawDiff, -Math.abs(yawDiff / totalDiff) * 180.0F * limitScale, Math.abs(yawDiff / totalDiff) * 180.0F * limitScale);
		float pitchStep = Mth.clamp(pitchDiff, -Math.abs(pitchDiff / totalDiff) * 180.0F * limitScale, Math.abs(pitchDiff / totalDiff) * 180.0F * limitScale);
		return blendRotation(yawStep, pitchStep, factor, 0.0F, 0.0F);
	}

	private float[] blendRotation(float yawStep, float pitchStep, float factor, float yawWave, float pitchWave) {
		float noise = random.nextFloat() * 0.2F - 0.1F;
		float yaw = Mth.lerp(factor + noise, 0.0F, yawStep) + yawWave;
		float pitch = Mth.lerp(factor + noise, 0.0F, pitchStep) + pitchWave;
		return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
	}

	private float humanPitchStep(float targetPitch, float speed, boolean enabled) {
		float directStep = (targetPitch - mc.player.getXRot()) * (speed / 100.0F) * 0.12F;
		if (!humanRotation.enabled() || !enabled) {
			return targetPitch - mc.player.getXRot();
		}

		long now = System.currentTimeMillis();
		long elapsed = Math.max(1L, Math.min(100L, now - lastRotationTime));
		lastRotationTime = now;
		float velocityDelta = (directStep - pitchVelocity) * acceleration.floatValue();
		pitchVelocity += velocityDelta * (elapsed / 16.0F);
		float wave = (float) (Math.sin(now * 0.01D) * jitter.floatValue() * 0.625D + Math.cos(now * 0.017D) * jitter.floatValue() * 0.375D);
		wave += ((float) Math.random() - 0.5F) * jitter.floatValue() * 0.75F;
		float blend = inertia.floatValue() + Math.abs(pitchVelocity) * 0.05F;
		pitchOffset = pitchOffset + (pitchVelocity + wave - pitchOffset) * blend;
		pitchOffset = Mth.clamp(pitchOffset, -maxOffset.floatValue(), maxOffset.floatValue());
		return pitchOffset;
	}

	private List<Entity> sortedEntities(Iterable<Entity> entities) {
		sortedEntities.clear();
		for (Entity entity : entities) {
			sortedEntities.add(entity);
		}
		if (sortByFov.enabled()) {
			sortedEntities.sort(Comparator.comparingDouble(this::targetFovDistance));
		}
		return sortedEntities;
	}

	private boolean matchesFilters(Entity entity) {
		if (mc.player == null || entity == null || !entity.isAlive() || !(entity instanceof LivingEntity) || entity == mc.player) {
			return false;
		}
		if (entity.isInvisible() && !targets.enabled(TargetFilter.INVISIBLE)) {
			return false;
		}
		if (!throughWalls.enabled() && !mc.player.hasLineOfSight(entity)) {
			return false;
		}
		if (entity instanceof Player player) {
			if (FriendManager.isFriend(player.getGameProfile().getName())) {
				return false;
			}
			return isUnarmed(player)
					? targets.enabled(TargetFilter.UNARMED)
					: targets.enabled(TargetFilter.PLAYERS);
		}
		if (isAnimal(entity)) {
			return targets.enabled(TargetFilter.ANIMALS);
		}
		if (isMonster(entity)) {
			return targets.enabled(TargetFilter.MONSTERS);
		}
		return false;
	}

	private boolean isValidTarget(Entity entity) {
		if (!matchesFilters(entity)) {
			return false;
		}
		float yawDiff = horizontalBoundsYawDiff(entity);
		float[] rotations = rotationsTo(entity);
		float pitchDiff = aimY.enabled() ? Math.abs(wrapDegrees(rotations[1] - mc.player.getXRot())) : 0.0F;
		if (Math.hypot(yawDiff, pitchDiff) > fov.floatValue() * 0.5F) {
			return false;
		}
		if (distance.floatValue() < entity.distanceTo(mc.player)) {
			return false;
		}
		return true;
	}

	private boolean isValidRememberedTarget(Entity entity) {
		if (!matchesFilters(entity)) {
			return false;
		}
		if (lastHit.enabled()) {
			return entity.distanceTo(mc.player) <= forgetDistance.floatValue();
		}
		return isValidTarget(entity);
	}

	private double targetFovDistance(Entity entity) {
		float yawDiff = horizontalBoundsYawDiff(entity);
		float[] rotations = rotationsTo(entity);
		float pitchDiff = aimY.enabled() ? Math.abs(wrapDegrees(rotations[1] - mc.player.getXRot())) : 0.0F;
		return Math.hypot(yawDiff, pitchDiff);
	}

	private float horizontalBoundsYawDiff(Entity entity) {
		if (mc.player == null) {
			return 180.0F;
		}

		AABB fullBox = entity.getBoundingBox();
		double width = entity.getBbWidth();
		double scale = hitboxes.enabled() ? Math.min(1.0D, Math.max(0.01D, hitboxSize.floatValue() / 100.0D)) : 1.0D;
		double halfWidth = (width * 0.5D) * scale;
		AABB bounds = new AABB(
				entity.getX() - halfWidth - HORIZONTAL_PRIORITY_PADDING,
				fullBox.minY,
				entity.getZ() - halfWidth - HORIZONTAL_PRIORITY_PADDING,
				entity.getX() + halfWidth + HORIZONTAL_PRIORITY_PADDING,
				fullBox.maxY,
				entity.getZ() + halfWidth + HORIZONTAL_PRIORITY_PADDING
		);
		Vec3 eye = mc.player.getEyePosition();
		if (horizontalLookIntersects(bounds, eye)) {
			return 0.0F;
		}

		double[][] corners = {
				{bounds.minX, bounds.minZ},
				{bounds.minX, bounds.maxZ},
				{bounds.maxX, bounds.minZ},
				{bounds.maxX, bounds.maxZ}
		};
		float bestDiff = 180.0F;
		for (double[] corner : corners) {
			float yaw = yawTo(eye, corner[0], corner[1]);
			bestDiff = Math.min(bestDiff, Math.abs(wrapDegrees(yaw - mc.player.getYRot())));
		}
		return bestDiff;
	}

	private boolean horizontalLookIntersects(AABB bounds, Vec3 eye) {
		double yawRadians = Math.toRadians(mc.player.getYRot());
		double directionX = -Math.sin(yawRadians);
		double directionZ = Math.cos(yawRadians);
		double minT = 0.0D;
		double maxT = Double.POSITIVE_INFINITY;

		if (Math.abs(directionX) < 1.0E-7D) {
			if (eye.x < bounds.minX || eye.x > bounds.maxX) {
				return false;
			}
		} else {
			double t1 = (bounds.minX - eye.x) / directionX;
			double t2 = (bounds.maxX - eye.x) / directionX;
			minT = Math.max(minT, Math.min(t1, t2));
			maxT = Math.min(maxT, Math.max(t1, t2));
		}

		if (Math.abs(directionZ) < 1.0E-7D) {
			if (eye.z < bounds.minZ || eye.z > bounds.maxZ) {
				return false;
			}
		} else {
			double t1 = (bounds.minZ - eye.z) / directionZ;
			double t2 = (bounds.maxZ - eye.z) / directionZ;
			minT = Math.max(minT, Math.min(t1, t2));
			maxT = Math.min(maxT, Math.max(t1, t2));
		}

		return maxT >= minT && maxT >= 0.0D;
	}

	private float yawTo(Vec3 eye, double x, double z) {
		double deltaX = x - eye.x;
		double deltaZ = z - eye.z;
		return (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F;
	}

	private float[] rotationsTo(Entity entity) {
		Vec3 eye = mc.player.getEyePosition();
		Vec3 point = targetPoint(entity);
		double deltaX = point.x - eye.x;
		double deltaY = point.y - eye.y;
		double deltaZ = point.z - eye.z;
		double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		float yaw = (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(deltaY, Math.max(horizontal, 1.0E-4D)) * Mth.RAD_TO_DEG);
		return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
	}

	private Vec3 targetPoint(Entity entity) {
		Vec3 eye = mc.player != null ? mc.player.getEyePosition() : Vec3.ZERO;
		AABB fullBox = entity.getBoundingBox();
		double width = entity.getBbWidth();
		double scale = hitboxes.enabled() ? Math.min(1.0D, Math.max(0.01D, hitboxSize.floatValue() / 100.0D)) : 1.0D;
		double halfWidth = (width * 0.5D) * scale;
		AABB miniHitbox = new AABB(
				entity.getX() - halfWidth,
				fullBox.minY,
				entity.getZ() - halfWidth,
				entity.getX() + halfWidth,
				fullBox.maxY,
				entity.getZ() + halfWidth
		);

		double clampedX = Mth.clamp(eye.x, miniHitbox.minX, miniHitbox.maxX);
		double clampedZ = Mth.clamp(eye.z, miniHitbox.minZ, miniHitbox.maxZ);
		double clampedY = switch (aimPoint.value()) {
			case HEAD -> entity.getY() + entity.getEyeHeight();
			case TORSO -> entity.getY() + entity.getBbHeight() * 0.5D;
			case CENTER, HITBOX, NEAREST -> Mth.clamp(eye.y, miniHitbox.minY + 0.1D, miniHitbox.maxY - 0.1D);
		};

		return new Vec3(clampedX, clampedY, clampedZ);
	}

	public boolean isInDeadZone(LivingEntity entity) {
		if (!hitboxes.enabled() || mc.player == null) {
			return false;
		}

		float percent = hitboxSize.floatValue();
		if (percent <= 0.0F) {
			return false;
		}

		AABB fullBox = entity.getBoundingBox();
		double width = entity.getBbWidth();
		double scale = Math.min(1.0D, Math.max(0.01D, percent / 100.0D));
		double halfWidth = (width * 0.5D) * scale;
		AABB miniHitbox = new AABB(
				entity.getX() - halfWidth,
				fullBox.minY,
				entity.getZ() - halfWidth,
				entity.getX() + halfWidth,
				fullBox.maxY,
				entity.getZ() + halfWidth
		);

		Vec3 eye = mc.player.getEyePosition();
		Vec3 look = mc.player.getViewVector(1.0F);
		Vec3 reachEnd = eye.add(look.scale(distance.floatValue() + 2.0D));
		Optional<Vec3> hit = miniHitbox.clip(eye, reachEnd);
		return hit.isPresent() || miniHitbox.contains(eye);
	}

	private float applyGcd(float targetAngle, float currentAngle) {
		double sensitivity = mc.options.sensitivity().get();
		double base = sensitivity * 0.6D + 0.2D;
		double gcd = base * base * base * 8.0D * 0.15D;
		if (gcd <= 0.0001D) {
			return targetAngle;
		}
		float delta = targetAngle - currentAngle;
		double quant = Math.round(delta / gcd) * gcd;
		if (quant == 0.0D && Math.abs(delta) > gcd) {
			quant = Math.signum(delta) * gcd;
		}
		return currentAngle + (float) quant;
	}

	private float adaptiveFactor(float angle, Entity entity, boolean readyToAttack) {
		long sinceRotation = System.currentTimeMillis() - lastRotationTime;
		float factor = switch (patternIndex) {
			case 0 -> 0.6F + random.nextFloat() * 0.4F;
			case 1 -> 0.8F + random.nextFloat() * 0.3F;
			case 2 -> 0.5F + random.nextFloat() * 0.3F;
			case 3 -> 0.7F + random.nextFloat() * 0.5F;
			case 4 -> 0.4F + random.nextFloat() * 0.4F;
			default -> 0.7F + random.nextFloat() * 0.4F;
		};
		factor *= readyToAttack ? 1.0F + random.nextFloat() * 0.15F : 0.8F + random.nextFloat() * 0.2F;
		if (entity != null) {
			factor *= 0.9F + random.nextFloat() * 0.3F;
		}
		float targetDistance = entity != null ? mc.player.distanceTo(entity) : 0.0F;
		if (targetDistance > 0.0F && targetDistance < 1.5F) {
			factor *= Mth.clamp(targetDistance / 2.0F, 0.4F, 0.9F);
		}
		if (sinceRotation < 45L) {
			factor *= 0.7F + random.nextFloat() * 0.4F;
		} else if (sinceRotation > 300L) {
			factor *= 1.1F + random.nextFloat() * 0.4F;
		}
		if (angle > 120.0F) {
			factor *= 1.3F;
		} else if (angle < 15.0F) {
			factor *= 0.5F + random.nextFloat() * 0.4F;
		}
		if (lastFactor > 0.0F) {
			float delta = factor - lastFactor;
			if (Math.abs(delta) > 0.35F) {
				factor = lastFactor + (delta > 0.0F ? 0.35F : -0.35F);
			}
		}
		lastFactor = Mth.clamp(factor, 0.15F, 1.8F);
		updatePattern();
		return lastFactor;
	}

	private void updatePattern() {
		long now = System.currentTimeMillis();
		if (now - lastModeSwitchTime > 3000L + random.nextInt(2000)) {
			patternIndex = (patternIndex + 1) % 5;
			lastModeSwitchTime = now;
		}
	}

	private float averageFactor() {
		float sum = 0.0F;
		int count = 0;
		for (float factor : recentFactors) {
			if (factor > 0.0F) {
				sum += factor;
				count++;
			}
		}
		return count > 0 ? sum / count : lastFactor;
	}

	private float angleScale(float angle) {
		if (angle < 20.0F) {
			return 0.95F + random.nextFloat() * 0.1F;
		}
		return angle > 90.0F ? 0.8F + random.nextFloat() * 0.3F : 0.85F + random.nextFloat() * 0.25F;
	}

	private float distanceScale(Entity entity) {
		float targetDistance = mc.player.distanceTo(entity);
		if (targetDistance <= 1.0F) {
			return 0.2F;
		}
		if (targetDistance <= 3.0F) {
			return 0.2F + 0.8F * ((targetDistance - 1.0F) / 2.0F);
		}
		return 1.0F;
	}

	private float randomBetween(float min, float max) {
		return Mth.lerp(random.nextFloat(), min, max);
	}

	private static float clampToDiff(float step, float diff) {
		return Math.abs(step) > Math.abs(diff) ? diff : step;
	}

	private static float wrapDegrees(float value) {
		return Mth.wrapDegrees(value);
	}

	private static boolean isWeaponOrTool(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.getItem() instanceof SwordItem
				|| stack.getItem() instanceof DiggerItem
				|| stack.getItem() instanceof MaceItem
				|| stack.getItem() instanceof TridentItem);
	}

	private static boolean isAnimal(Entity entity) {
		MobCategory category = entity.getType().getCategory();
		return entity instanceof Animal
				|| category == MobCategory.CREATURE
				|| category == MobCategory.WATER_CREATURE
				|| category == MobCategory.AXOLOTLS
				|| category == MobCategory.UNDERGROUND_WATER_CREATURE;
	}

	private static boolean isUnarmed(LivingEntity entity) {
		for (ItemStack armor : entity.getArmorSlots()) {
			if (!armor.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isMonster(Entity entity) {
		return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
	}

	private void resetState() {
		target = null;
		previousYaw = Float.MAX_VALUE;
		previousPitch = Float.MAX_VALUE;
		lastFactor = 0.0F;
		pitchVelocity = 0.0F;
		pitchOffset = 0.0F;
		yawVelocity = 0.0F;
		recentFactorIndex = 0;
		patternIndex = 0;
		lastRotationTime = System.currentTimeMillis();
		lastModeSwitchTime = System.currentTimeMillis();
		lastFrameNano = 0L;
		for (int i = 0; i < recentFactors.length; i++) {
			recentFactors[i] = 0.0F;
		}
	}

	public LivingEntity currentTarget() {
		return target;
	}

	public static AimAssist getInstance() {
		return INSTANCE;
	}

	public enum AimMode {
		NORMAL("Normal"),
		SMOOTH("Smooth"),
		SYSTEM_DLC("System DLC"),
		ON_TIME("On Time"),
		CAKE_WORLD("Cake World"),
		HVH("HvH");

		private final String display;

		AimMode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

	public enum AimPoint {
		HEAD("Head"),
		TORSO("Torso"),
		CENTER("Center"),
		HITBOX("Hitbox"),
		NEAREST("Nearest");

		private final String display;

		AimPoint(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

	public enum TargetFilter {
		PLAYERS("Players"),
		INVISIBLE("Invisible"),
		UNARMED("Unarmed"),
		ANIMALS("Animals"),
		MONSTERS("Monsters");

		private final String display;

		TargetFilter(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}
}
