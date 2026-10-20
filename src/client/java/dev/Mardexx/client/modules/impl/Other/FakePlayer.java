package dev.Mardexx.client.modules.impl.Other;

import com.mojang.authlib.GameProfile;
import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.player.world.AttackEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class FakePlayer extends Module implements QuickImports {
	private static final FakePlayer INSTANCE = new FakePlayer();

	private final BooleanSetting canDie = new BooleanSetting("Can die", true);
	private final BooleanSetting instantKill = new BooleanSetting("Instant kill", false);
	private final BooleanSetting autoTotem = new BooleanSetting("Auto totem", false);
	private final BooleanSetting autoRespawn = new BooleanSetting("Auto respawn", true);
	private final FloatSetting respawnDelay = (FloatSetting) new FloatSetting("Respawn delay", 3.2F, 0.5F, 10.0F).precision(1);
	private final BooleanSetting copyInventory = new BooleanSetting("Copy inventory", false);
	private final BooleanSetting withArmor = new BooleanSetting("With armor", true);

	private final String fakePlayerName = "FakePlayer";
	private RemotePlayer fakePlayer;
	private EventListener attackEvent;

	private boolean isDying = false;
	private long deathTimestamp = 0L;
	private Vec3 lastSpawnPos;
	private float lastSpawnYRot;
	private float lastSpawnXRot;
	private float lastSpawnHeadRot;
	private float lastSpawnBodyRot;

	private FakePlayer() {
		super("Fake Player", ModuleCategory.OTHER, "Spawns a client-side player copy for testing.");
		register(canDie, instantKill, autoTotem, autoRespawn, respawnDelay, copyInventory, withArmor);
	}

	@Override
	protected void onEnable() {
		isDying = false;
		lastSpawnPos = null;
		spawnFakePlayer();
		attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(this::onAttack));
	}

	@Override
	protected void onDisable() {
		if (attackEvent != null) {
			attackEvent.unsubscribe();
			attackEvent = null;
		}

		isDying = false;
		removeFakePlayer();
	}

	@Override
	public void onTick() {
		if (isDying) {
			long elapsed = System.currentTimeMillis() - deathTimestamp;
			float delayMs = respawnDelay.value() * 1000.0F;
			if (elapsed >= delayMs) {
				isDying = false;
				removeFakePlayer();
				if (autoRespawn.enabled()) {
					spawnFakePlayer();
				} else {
					setEnabled(false);
				}
			}
			return;
		}

		if (fakePlayer == null || mc.level == null) {
			setEnabled(false);
			return;
		}

		fakePlayer.noPhysics = true;
		if (autoTotem.enabled() && fakePlayer.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
			fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
		}
	}

	private void spawnFakePlayer() {
		if (mc.level == null || mc.player == null) {
			setEnabled(false);
			return;
		}

		fakePlayer = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), fakePlayerName));
		if (lastSpawnPos != null) {
			fakePlayer.setPos(lastSpawnPos.x, lastSpawnPos.y, lastSpawnPos.z);
			fakePlayer.setYRot(lastSpawnYRot);
			fakePlayer.setXRot(lastSpawnXRot);
			fakePlayer.yHeadRot = lastSpawnHeadRot;
			fakePlayer.yBodyRot = lastSpawnBodyRot;
		} else {
			fakePlayer.copyPosition(mc.player);
			fakePlayer.setYRot(mc.player.getYRot());
			fakePlayer.setXRot(mc.player.getXRot());
			fakePlayer.yHeadRot = mc.player.yHeadRot;
			fakePlayer.yBodyRot = mc.player.yBodyRot;
		}

		lastSpawnPos = fakePlayer.position();
		lastSpawnYRot = fakePlayer.getYRot();
		lastSpawnXRot = fakePlayer.getXRot();
		lastSpawnHeadRot = fakePlayer.yHeadRot;
		lastSpawnBodyRot = fakePlayer.yBodyRot;

		if (copyInventory.enabled()) {
			fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().copy());
			fakePlayer.setItemInHand(InteractionHand.OFF_HAND, mc.player.getOffhandItem().copy());
			copyArmor();
		} else {
			if (withArmor.enabled()) {
				equipNetheriteArmor();
			}
			if (autoTotem.enabled()) {
				fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			} else {
				fakePlayer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			}
		}

		fakePlayer.setHealth(20.0F);
		fakePlayer.setAbsorptionAmount(0.0F);
		fakePlayer.noPhysics = true;
		mc.level.addEntity(fakePlayer);
	}

	private void copyArmor() {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			fakePlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
		}
	}

	private void equipNetheriteArmor() {
		fakePlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		fakePlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
		fakePlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
		fakePlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
	}

	private void removeFakePlayer() {
		if (fakePlayer != null) {
			fakePlayer.remove(Entity.RemovalReason.KILLED);
			fakePlayer = null;
		}
	}

	private void onAttack(AttackEvent.AttackEventData event) {
		if (mc.player == null || isDying) return;

		float damage = 4.0F;
		boolean crit = mc.player.fallDistance > 0.0F && !mc.player.onGround() && !mc.player.onClimbable() && !mc.player.isInWater() && !mc.player.isPassenger();
		try {
			damage = (float) mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			float cooldown = mc.player.getAttackStrengthScale(0.5F);
			damage *= (0.2F + cooldown * cooldown * 0.8F);
		} catch (Throwable ignored) {
		}
		if (crit) {
			damage *= 1.5F;
		}
		if (instantKill.enabled()) {
			damage = 999.0F;
		}

		if (handleAttack(event.entity(), damage, crit)) {
			AttackEvent.getInstance().setCancel(true);
		}
	}

	public boolean handleAttack(Entity entity, float damage, boolean critical) {
		if (fakePlayer == null || entity != fakePlayer || mc.level == null || mc.player == null || isDying) {
			return false;
		}

		mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (critical) {
			mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		fakePlayer.hurtTime = 10;
		fakePlayer.hurtDuration = 10;

		float remainingDamage = Math.max(0.0F, damage);
		float absorption = fakePlayer.getAbsorptionAmount();
		if (absorption > 0.0F) {
			float absorbed = Math.min(absorption, remainingDamage);
			fakePlayer.setAbsorptionAmount(absorption - absorbed);
			remainingDamage -= absorbed;
		}

		if (remainingDamage <= 0.0F) {
			return true;
		}

		float newHealth = fakePlayer.getHealth() - remainingDamage;
		if (newHealth <= 0.0F) {
			useTotemOrDie();
		} else {
			fakePlayer.setHealth(Math.max(1.0F, newHealth));
		}

		return true;
	}

	private void useTotemOrDie() {
		if (fakePlayer.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
			new ClientboundEntityEventPacket(fakePlayer, (byte) 35).handle(mc.getConnection());
			fakePlayer.setHealth(10.0F);
			fakePlayer.setAbsorptionAmount(0.0F);
			fakePlayer.setItemInHand(InteractionHand.OFF_HAND, autoTotem.enabled() ? new ItemStack(Items.TOTEM_OF_UNDYING) : ItemStack.EMPTY);
			return;
		}

		if (canDie.enabled()) {
			killFakePlayer();
		} else {
			fakePlayer.setHealth(1.0F);
		}
	}

	private void killFakePlayer() {
		if (fakePlayer == null || isDying) return;

		isDying = true;
		deathTimestamp = System.currentTimeMillis();
		fakePlayer.setHealth(0.0F);

		if (mc.level != null && mc.player != null) {
			mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		if (mc.getConnection() != null) {
			try {
				new ClientboundEntityEventPacket(fakePlayer, (byte) 3).handle(mc.getConnection());
			} catch (Exception ignored) {
			}
		}
	}

	public RemotePlayer getFakePlayer() {
		return fakePlayer;
	}

	public boolean isFakePlayer(Entity entity) {
		return fakePlayer != null && entity == fakePlayer;
	}

	public static FakePlayer getInstance() {
		return INSTANCE;
	}
}
