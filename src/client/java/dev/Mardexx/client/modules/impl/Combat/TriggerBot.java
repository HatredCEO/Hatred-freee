package dev.Mardexx.client.modules.impl.Combat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.api.render.Render3D;
import dev.Mardexx.api.utils.other.TPSTracker;
import dev.Mardexx.api.utils.player.combat.IdealHitUtils;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatRangeSetting;
import dev.Mardexx.client.theme.ThemeManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TriggerBot extends Module implements QuickImports {
	private static final TriggerBot INSTANCE = new TriggerBot();
	private static final String RELEASE_SHIELD_DESCRIPTION = "Stops blocking with a shield before TriggerBot attacks.";
	private static final int SWORD_LIKE_COOLDOWN_TICKS = 7;
	private static final float READY_ATTACK_STRENGTH = 0.9F;

	private final BooleanSetting releaseShield = new BooleanSetting(
			"release_shield",
			"Release Shield",
			RELEASE_SHIELD_DESCRIPTION,
			false
	);
	private final BooleanSetting onlyWeapons = new BooleanSetting(
			"only_weapons",
			"Only Weapons",
			"Only attacks while holding a sword or axe.",
			false
	);
	private final EnumSetting<CriticalHitMode> criticalHits = new EnumSetting<>(
			"critical_hits",
			"Critical Hits",
			"Controls attack modes.",
			CriticalHitMode.AUTOMATIC
	);
	private final BooleanSetting syncTps = new BooleanSetting(
			"sync_tps",
			"Sync TPS",
			"Synchronizes attack timing with server TPS.",
			false
	);
	private final EnumSetting<SprintResetMode> sprintBeforeAttack = new EnumSetting<>(
			"sprint_before_attack",
			"Sprint Before Attack",
			"Selects the sprint reset mode before attacking.",
			SprintResetMode.LEGIT
	);
	private final FloatRangeSetting attackDelay = new FloatRangeSetting(
			"attack_delay",
			"Attack Delay",
			"Randomizes delay after ideal attack timing.",
			1.0F,
			2.0F,
			2.5F,
			2.5F
	).step(0.1F).precision(1);
	private final EnumBooleanMapSetting<TargetFilter> targets = EnumBooleanMapSetting.create(
			"targets",
			"Targets",
			"Selects target groups and filters.",
			TargetFilter.class,
			defaults -> {
				defaults.put(TargetFilter.PLAYERS, true);
				defaults.put(TargetFilter.UNARMED, true);
				defaults.put(TargetFilter.INVISIBLE, true);
			}
	);
	private final EnumSetting<TargetEspMode> targetEsp = new EnumSetting<>(
			"target_esp",
			"Target ESP",
			"Renders the Delta target effect around TriggerBot's current target.",
			TargetEspMode.SPHERES
	);
	private long pendingAttackDelayUntilMs = -1L;
	private LivingEntity currentTarget;
	private LivingEntity renderedEspTarget;
	private float targetEspProgress;
	private long lastEspFrameNanos = System.nanoTime();
	private LivingEntity pendingAttackTarget;
	private LivingEntity sprintResetTarget;
	private SprintResetMode preparedSprintReset = SprintResetMode.NONE;
	private int sprintResetWaitTicks;
	private int sprintResetPreparedTick;
	private boolean normalSprintControlled;
	private boolean normalSprintKeyWasDown;
	private boolean normalPlayerWasSprinting;
	private final TPSTracker tpsTracker = TPSTracker.getInstance();
	private long lastSyncedAttackTime;
	private int ticksToAttack;
	private int triggerTick;
	private int swordLikeCooldownTicks;
	private LivingEntity queuedVanillaAttackTarget;
	private SprintResetMode queuedVanillaAttackSprintReset = SprintResetMode.NONE;
	private boolean queuedVanillaAttackClick;
	private boolean attackKeyWasDown;
	private boolean attackKeyStateCaptured;

	private TriggerBot() {
		super("TriggerBot", ModuleCategory.COMBAT, "Automatically attacks a valid target under the crosshair.");
		register(releaseShield, onlyWeapons, criticalHits, syncTps, sprintBeforeAttack, attackDelay, targets, targetEsp);
		Render3D.add(this::renderTargetEsp);
	}

	private void renderTargetEsp(WorldRenderContext context) {
		LivingEntity activeTarget = enabled() && targetEsp.value() != TargetEspMode.OFF
				&& currentTarget != null && currentTarget.isAlive() && !currentTarget.isRemoved()
				? currentTarget
				: null;
		if (activeTarget != null) {
			renderedEspTarget = activeTarget;
		}

		long now = System.nanoTime();
		float delta = Math.min(0.05F, Math.max(0.0F, (now - lastEspFrameNanos) / 1_000_000_000.0F));
		lastEspFrameNanos = now;
		float targetProgress = activeTarget == null ? 0.0F : 1.0F;
		targetEspProgress += (targetProgress - targetEspProgress) * (1.0F - (float) Math.exp(-12.0F * delta));
		if (Math.abs(targetEspProgress - targetProgress) < 0.001F) {
			targetEspProgress = targetProgress;
		}
		if (renderedEspTarget == null || targetEspProgress <= 0.001F) {
			if (targetEspProgress <= 0.001F) {
				renderedEspTarget = null;
			}
			return;
		}

		TargetEspRenderer.render(context, renderedEspTarget, targetEsp.value(), ThemeManager.accent(), targetEspProgress);
	}

	@Override
	public void onPreTick() {
		if (mc.player == null || mc.level == null || mc.gameMode == null) {
			currentTarget = null;
			return;
		}

		triggerTick++;
		if (swordLikeCooldownTicks > 0) {
			swordLikeCooldownTicks--;
		}
		if (ticksToAttack > 0) {
			ticksToAttack--;
		}

		tryAttackReadyTarget(true);
	}

	private void tryAttackReadyTarget(boolean allowSprintResetPrepare) {
		LivingEntity target = getCrosshairTarget();
		currentTarget = target;
		boolean prepareSprintReset = target != null
				&& allowSprintResetPrepare
				&& preparedSprintReset == SprintResetMode.NONE
				&& sprintBeforeAttack.value() != SprintResetMode.NONE
				&& mc.player.isSprinting();
		if (target == null || !isReadyToAttack(target, prepareSprintReset)) {
			if (allowSprintResetPrepare) {
				cancelPreparedSprintReset();
				clearPendingAttack();
			}
			return;
		}

		if (pendingAttackTarget != target) {
			pendingAttackTarget = target;
			pendingAttackDelayUntilMs = randomAttackDelayUntilMs();
		}

		if (pendingAttackDelayUntilMs > System.currentTimeMillis()) {
			return;
		}

		if (preparedSprintReset != SprintResetMode.NONE) {
			if (target != sprintResetTarget) {
				cancelPreparedSprintReset();
				clearPendingAttack();
				return;
			}
			if (sprintResetWaitTicks > 0 || triggerTick <= sprintResetPreparedTick) {
				if (sprintResetWaitTicks > 0) {
					sprintResetWaitTicks--;
				}
				return;
			}

			attack(target, preparedSprintReset);
			return;
		}

		if (!allowSprintResetPrepare && sprintBeforeAttack.value() != SprintResetMode.NONE && mc.player.isSprinting()) {
			return;
		}

		SprintResetMode appliedSprintReset = prepareSprintResetBeforeAttack(target);
		if (appliedSprintReset != SprintResetMode.NONE) {
			return;
		}

		attack(target, SprintResetMode.NONE);
	}

	private void attack(LivingEntity target, SprintResetMode appliedSprintReset) {
		releaseShieldBeforeAttack();
		if (appliedSprintReset == SprintResetMode.NORMAL) {
			stopNormalSprint();
		}

		queueVanillaAttack(target, appliedSprintReset);
		markAttackTiming();
		clearPendingAttack();
	}

	public void beforeHandleKeybinds() {
		if (!enabled() || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) {
			clearQueuedVanillaAttack(true);
			return;
		}

		LivingEntity target = queuedVanillaAttackTarget;
		if (target == null) {
			tryAttackReadyTarget(true);
			target = queuedVanillaAttackTarget;
			if (target == null) {
				return;
			}
		}
		if (!target.isAlive() || target.isRemoved() || isFriend(target)) {
			clearQueuedVanillaAttack(true);
			return;
		}

		mc.hitResult = new EntityHitResult(target);
		mc.crosshairPickEntity = target;
		if (queuedVanillaAttackClick) {
			attackKeyWasDown = mc.options.keyAttack.isDown();
			attackKeyStateCaptured = true;
			mc.options.keyAttack.setDown(true);
			KeyMapping.click(InputConstants.getKey(mc.options.keyAttack.saveString()));
			queuedVanillaAttackClick = false;
		}
	}

	public void afterHandleKeybinds() {
		if (queuedVanillaAttackTarget != null) {
			clearQueuedVanillaAttack(true);
		}
	}

	public void onRenderFrame(WorldRenderContext context) {
	}

	@Override
	protected void onDisable() {
		currentTarget = null;
		cancelPreparedSprintReset();
		clearPendingAttack();
		clearQueuedVanillaAttack(true);
	}

	public BooleanSetting releaseShield() {
		return releaseShield;
	}

	public LivingEntity currentTarget() {
		return currentTarget;
	}

	public BooleanSetting onlyWeapons() {
		return onlyWeapons;
	}

	public EnumSetting<CriticalHitMode> criticalHits() {
		return criticalHits;
	}

	public EnumSetting<SprintResetMode> sprintBeforeAttack() {
		return sprintBeforeAttack;
	}

	public FloatRangeSetting attackDelay() {
		return attackDelay;
	}

	public EnumBooleanMapSetting<TargetFilter> targets() {
		return targets;
	}

	private LivingEntity getCrosshairTarget() {
		Vec3 start = mc.player.getEyePosition();
		double range = mc.player.entityInteractionRange();
		Vec3 end = start.add(mc.player.getViewVector(1.0F).scale(range));

		BlockHitResult blockHit = mc.level.clip(new ClipContext(
				start,
				end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				mc.player
		));

		double closestDistanceSq = range * range;
		if (blockHit.getType() != HitResult.Type.MISS) {
			closestDistanceSq = start.distanceToSqr(blockHit.getLocation());
		}

		AABB searchBox = new AABB(start, end).inflate(1.0D);
		LivingEntity closestTarget = null;

		for (Entity entity : mc.level.getEntities(mc.player, searchBox, candidate ->
				candidate != mc.player && !candidate.isSpectator() && candidate.isPickable())) {
			LivingEntity target = validTarget(entity);
			if (target == null) {
				continue;
			}

			AABB hitBox = target.getBoundingBox().inflate(target.getPickRadius());
			Optional<Vec3> intersection = hitBox.clip(start, end);
			if (intersection.isEmpty() && !hitBox.contains(start)) {
				continue;
			}

			Vec3 hitPosition = intersection.orElse(start);
			double distanceSq = start.distanceToSqr(hitPosition);
			if (distanceSq <= closestDistanceSq) {
				closestDistanceSq = distanceSq;
				closestTarget = target;
			}
		}

		return closestTarget;
	}

	private static boolean isUnarmed(LivingEntity entity) {
		for (ItemStack armor : entity.getArmorSlots()) {
			if (!armor.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private LivingEntity validTarget(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)
				|| entity == mc.player
				|| !livingEntity.isAlive()
				|| livingEntity.isSpectator()) {
			return null;
		}

		if (livingEntity.isInvisible() && !targets.enabled(TargetFilter.INVISIBLE)) {
			return null;
		}

		if (isFriend(livingEntity)) {
			return null;
		}

		if (livingEntity instanceof Player player) {
			return (isUnarmed(player) ? targets.enabled(TargetFilter.UNARMED) : targets.enabled(TargetFilter.PLAYERS))
					? livingEntity
					: null;
		}

		if (livingEntity instanceof Animal) {
			return targets.enabled(TargetFilter.ANIMALS) ? livingEntity : null;
		}

		if (livingEntity instanceof Mob) {
			return targets.enabled(TargetFilter.MOBS) ? livingEntity : null;
		}

		return null;
	}

	private static boolean isFriend(Entity entity) {
		return entity instanceof Player player && FriendManager.isFriend(player.getGameProfile().getName());
	}

	private boolean isReadyToAttack(LivingEntity target, boolean allowNextTickReadiness) {
		IdealHitUtils idealHitUtils = IdealHitUtils.getInstance();

		if (onlyWeapons.enabled() && !isHoldingSwordOrAxe()) {
			return false;
		}

		if (!isTpsCooldownReady(500L)) {
			return false;
		}

		if (usesSwordLikeDelay() && swordLikeCooldownTicks > 0) {
			return false;
		}

		boolean targetHurtCooldown = idealHitUtils.shouldUseTargetHurtCooldown();
		if (targetHurtCooldown) {
			if (ticksToAttack > 0) {
				return false;
			}
			if (!idealHitUtils.isTargetReadyForNextHit(target)) {
				return false;
			}
		} else {
			if (mc.player.getAttackStrengthScale(allowNextTickReadiness ? 1.0F : 0.0F) < READY_ATTACK_STRENGTH) {
				return false;
			}
			if (!idealHitUtils.cooldownIsReached(false)) {
				return false;
			}
			if (ticksToAttack > 0) {
				return false;
			}
		}

		if (criticalHits.is(CriticalHitMode.ONLY_CRITS)
				&& mc.player.getAttackStrengthScale(allowNextTickReadiness ? 1.0F : 0.0F) < READY_ATTACK_STRENGTH) {
			return false;
		}

		return criticalHits.is(CriticalHitMode.IGNORE_CRITS) || idealHitUtils.canCritical();
	}

	private long randomAttackDelayUntilMs() {
		return System.currentTimeMillis();
	}

	private void clearPendingAttack() {
		pendingAttackTarget = null;
		pendingAttackDelayUntilMs = -1L;
	}

	private void queueVanillaAttack(LivingEntity target, SprintResetMode appliedSprintReset) {
		queuedVanillaAttackTarget = target;
		queuedVanillaAttackSprintReset = appliedSprintReset;
		queuedVanillaAttackClick = true;
	}

	private void clearQueuedVanillaAttack(boolean restoreState) {
		if (restoreState) {
			if (attackKeyStateCaptured) {
				mc.options.keyAttack.setDown(attackKeyWasDown);
			}
			restoreSprintAfterAttack(queuedVanillaAttackSprintReset);
			clearPreparedSprintReset();
		}

		queuedVanillaAttackTarget = null;
		queuedVanillaAttackSprintReset = SprintResetMode.NONE;
		queuedVanillaAttackClick = false;
		attackKeyWasDown = false;
		attackKeyStateCaptured = false;
	}

	private void clearPreparedSprintReset() {
		sprintResetTarget = null;
		preparedSprintReset = SprintResetMode.NONE;
		sprintResetWaitTicks = 0;
		sprintResetPreparedTick = 0;
	}

	private void cancelPreparedSprintReset() {
		if (preparedSprintReset != SprintResetMode.NONE) {
			restoreSprintAfterAttack(preparedSprintReset);
		}
		clearPreparedSprintReset();
	}

	private boolean usesSwordLikeDelay() {
		return mc.player.getMainHandItem().isEmpty()
				|| !(mc.player.getMainHandItem().getItem() instanceof SwordItem)
				&& !(mc.player.getMainHandItem().getItem() instanceof DiggerItem)
				&& !(mc.player.getMainHandItem().getItem() instanceof MaceItem)
				&& !(mc.player.getMainHandItem().getItem() instanceof TridentItem);
	}

	private boolean isHoldingSwordOrAxe() {
		return mc.player.getMainHandItem().getItem() instanceof SwordItem
				|| mc.player.getMainHandItem().getItem() instanceof AxeItem;
	}

	private void applySwordLikeCooldown() {
		if (usesSwordLikeDelay()) {
			swordLikeCooldownTicks = SWORD_LIKE_COOLDOWN_TICKS;
		}
	}

	private void markAttackTiming() {
		lastSyncedAttackTime = System.currentTimeMillis();
		ticksToAttack = IdealHitUtils.getInstance().shouldUseTargetHurtCooldown() ? 1 : 10;
		applySwordLikeCooldown();
	}

	private boolean isTpsCooldownReady(long baseCooldown) {
		if (!syncTps.enabled()) {
			return true;
		}

		return System.currentTimeMillis() >= lastSyncedAttackTime + getTpsAdjustedCooldown(baseCooldown);
	}

	private long getTpsAdjustedCooldown(long baseCooldown) {
		if (!syncTps.enabled()) {
			return baseCooldown;
		}

		float tps = Mth.clamp(tpsTracker.getTps(), 0.1F, 20.0F);
		if (tps >= 20.0F) {
			return baseCooldown;
		}

		float multiplier = 20.0F / tps;
		float additionalFactor = 1.0F + (20.0F - tps) * 0.05F;
		long adjusted = (long) (baseCooldown * multiplier * additionalFactor);
		return Math.min(adjusted, 3000L);
	}

	private void releaseShieldBeforeAttack() {
		if (releaseShield.enabled()
				&& mc.player.isUsingItem()
				&& mc.player.getUseItem().is(Items.SHIELD)) {
			mc.gameMode.releaseUsingItem(mc.player);
		}
	}

	private SprintResetMode prepareSprintResetBeforeAttack(LivingEntity target) {
		SprintResetMode mode = resetSprintBeforeAttack(mc.player.isSprinting());
		if (mode != SprintResetMode.NONE) {
			sprintResetTarget = target;
			preparedSprintReset = mode;
			sprintResetWaitTicks = 0;
			sprintResetPreparedTick = triggerTick;
		}
		return mode;
	}

	private SprintResetMode resetSprintBeforeAttack(boolean wasSprinting) {
		if (!wasSprinting) {
			return SprintResetMode.NONE;
		}

		SprintResetMode mode = sprintBeforeAttack.value();
		switch (mode) {
			case NONE -> {
				return SprintResetMode.NONE;
			}
			case PACKET -> sendSprintCommand(ServerboundPlayerCommandPacket.Action.STOP_SPRINTING);
			case LEGIT -> {
				mc.options.keyUp.setDown(false);
				mc.options.keySprint.setDown(false);
			}
			case NORMAL -> stopNormalSprint();
		}
		return mode;
	}

	private void restoreSprintAfterAttack(SprintResetMode appliedMode) {
		switch (appliedMode) {
			case PACKET -> sendSprintCommand(ServerboundPlayerCommandPacket.Action.START_SPRINTING);
			case LEGIT -> KeyMapping.setAll();
			case NORMAL -> restoreNormalSprint();
			case NONE -> {
			}
		}
	}

	private void stopNormalSprint() {
		if (!normalSprintControlled) {
			normalSprintKeyWasDown = mc.options.keySprint.isDown();
			normalPlayerWasSprinting = mc.player.isSprinting();
			normalSprintControlled = true;
		}

		mc.player.setSprinting(false);
		mc.options.keySprint.setDown(false);
	}

	private void restoreNormalSprint() {
		if (!normalSprintControlled) {
			return;
		}

		try {
			if (mc.player != null && (normalPlayerWasSprinting || normalSprintKeyWasDown)) {
				mc.options.keySprint.setDown(true);
				mc.player.setSprinting(true);
			}
		} finally {
			normalSprintControlled = false;
			normalSprintKeyWasDown = false;
			normalPlayerWasSprinting = false;
		}
	}

	private void sendSprintCommand(ServerboundPlayerCommandPacket.Action action) {
		ClientPacketListener connection = mc.getConnection();
		if (connection != null) {
			connection.send(new ServerboundPlayerCommandPacket(mc.player, action));
		}
	}

	public enum CriticalHitMode {
		AUTOMATIC("Automatic"),
		ONLY_CRITS("Only Crits"),
		IGNORE_CRITS("Ignore Crits");

		private final String display;

		CriticalHitMode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

	public enum SprintResetMode {
		NORMAL("Normal"),
		LEGIT("Legit"),
		PACKET("Packet"),
		NONE("None");

		private final String display;

		SprintResetMode(String display) {
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
		MOBS("Mobs"),
		ANIMALS("Animals");

		private final String display;

		TargetFilter(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}
	}

	public enum TargetEspMode {
		BUBBLE("Bubble"),
		SPHERES("Spheres"),
		CIRCLE("Circle"),
		OFF("Off");

		private final String display;

		TargetEspMode(String display) {
			this.display = display;
		}

		public String display() {
			return display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	public static TriggerBot getInstance() {
		return INSTANCE;
	}
}
