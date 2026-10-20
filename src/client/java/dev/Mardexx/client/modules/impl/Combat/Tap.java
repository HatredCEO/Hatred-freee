package dev.Mardexx.client.modules.impl.Combat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.Mardexx.api.event.EventListener;
import dev.Mardexx.api.event.Listener;
import dev.Mardexx.api.event.events.client.KeyEvent;
import dev.Mardexx.api.event.events.player.world.AttackEvent;
import dev.Mardexx.api.system.interfaces.QuickImports;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

public final class Tap extends Module implements QuickImports {
	private static final Tap INSTANCE = new Tap();
	private static final long REVERSE_TAP_MS = 10L;

	private final BooleanSetting shiftTap = new BooleanSetting(
			"shift_tap",
			"Shift Tap",
			"Taps sneak after attacking.",
			false
	);
	private final FloatSetting shiftTapTime = new FloatSetting(
			"shift_tap_time",
			"Time",
			"Sets how long to hold shift during an attack.",
			10.0F,
			50.0F,
			100.0F
	).step(10.0F).precision(0).suffix("ms");
	private final BooleanSetting wTap = new BooleanSetting(
			"w_tap",
			"W Tap",
			"Taps forward during an attack.",
			false
	);
	private final FloatSetting wTapTime = new FloatSetting(
			"w_tap_time",
			"Time",
			"Sets how long to hold forward during an attack.",
			10.0F,
			50.0F,
			100.0F
	).step(10.0F).precision(0).suffix("ms");
	private final BooleanSetting reverseTap = new BooleanSetting(
			"reverse_tap",
			"Reverse Tap",
			"Briefly taps the opposite movement key after release.",
			false
	);

	private EventListener attackListener;
	private long shiftTapUntilMs;
	private long wTapUntilMs;
	private long reverseForwardUntilMs;
	private long reverseBackUntilMs;
	private long reverseLeftUntilMs;
	private long reverseRightUntilMs;
	private boolean reverseForwardSampled;
	private boolean reverseBackSampled;
	private boolean reverseLeftSampled;
	private boolean reverseRightSampled;
	private EventListener keyListener;

	private Tap() {
		super("Tap", ModuleCategory.COMBAT, "Runs movement taps after attacks.");
		shiftTapTime.visibleWhen(shiftTap::enabled).displayAfter(shiftTap);
		wTapTime.visibleWhen(wTap::enabled).displayAfter(wTap);
		register(shiftTap, shiftTapTime, wTap, wTapTime, reverseTap);
	}

	@Override
	protected void onEnable() {
		attackListener = AttackEvent.getInstance().subscribe(new Listener<>(-100, this::onAttack));
		keyListener = KeyEvent.getInstance().subscribe(new Listener<>(this::onKey));
	}

	@Override
	protected void onDisable() {
		if (attackListener != null) {
			attackListener.unsubscribe();
			attackListener = null;
		}
		if (keyListener != null) {
			keyListener.unsubscribe();
			keyListener = null;
		}

		clearTaps();
		KeyMapping.setAll();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.screen != null) {
			if (hasActiveTaps()) {
				clearTaps();
				KeyMapping.setAll();
			}
			return;
		}
	}

	public void onRenderFrame(WorldRenderContext context) {
		if (!enabled()) {
			return;
		}

		updateTimedTaps();
	}

	public void beforeMovementInputTick() {
		if (!enabled()) {
			return;
		}

		holdTimedTapsIfActive();
	}

	public void afterMovementInputTick() {
		if (!enabled()) {
			return;
		}

		markReverseTapsSampled();
		updateTimedTaps();
	}

	private void onAttack(AttackEvent.AttackEventData event) {
		if (AttackEvent.getInstance().isCancelled() || mc.player == null || mc.level == null || mc.screen != null || !isValidAttackTarget(event.entity())) {
			return;
		}

		if (shiftTap.enabled()) {
			startShiftTap();
		}
		if (wTap.enabled()) {
			startWTap();
		}
	}

	private void updateTimedTaps() {
		if (mc.player == null || mc.level == null || mc.screen != null) {
			if (hasActiveTaps()) {
				clearTaps();
				KeyMapping.setAll();
			}
			return;
		}

		long now = System.currentTimeMillis();
		boolean restoreKeys = false;
		if (shiftTapUntilMs > 0L) {
			if (now < shiftTapUntilMs) {
				mc.options.keyShift.setDown(true);
			} else {
				shiftTapUntilMs = 0L;
				restoreKeys = true;
			}
		}
		if (wTapUntilMs > 0L) {
			if (now < wTapUntilMs) {
				mc.options.keyUp.setDown(true);
			} else {
				wTapUntilMs = 0L;
				restoreKeys = true;
			}
		}
		if (reverseForwardUntilMs > 0L) {
			if (now < reverseForwardUntilMs || !reverseForwardSampled) {
				mc.options.keyUp.setDown(true);
			} else {
				reverseForwardUntilMs = 0L;
				restoreKeys = true;
			}
		}
		if (reverseBackUntilMs > 0L) {
			if (now < reverseBackUntilMs || !reverseBackSampled) {
				mc.options.keyDown.setDown(true);
			} else {
				reverseBackUntilMs = 0L;
				restoreKeys = true;
			}
		}
		if (reverseLeftUntilMs > 0L) {
			if (now < reverseLeftUntilMs || !reverseLeftSampled) {
				mc.options.keyLeft.setDown(true);
			} else {
				reverseLeftUntilMs = 0L;
				restoreKeys = true;
			}
		}
		if (reverseRightUntilMs > 0L) {
			if (now < reverseRightUntilMs || !reverseRightSampled) {
				mc.options.keyRight.setDown(true);
			} else {
				reverseRightUntilMs = 0L;
				restoreKeys = true;
			}
		}

		if (restoreKeys) {
			KeyMapping.setAll();
			holdTimedTapsIfActive();
		}
	}

	private void onKey(KeyEvent.Data event) {
		if (!reverseTap.enabled()
				|| event.mouse()
				|| event.action() != GLFW.GLFW_RELEASE
				|| mc.player == null
				|| mc.level == null
				|| mc.screen != null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (matchesKey(mc.options.keyUp, event.key())) {
			startReverseBack(now);
		} else if (matchesKey(mc.options.keyDown, event.key())) {
			startReverseForward(now);
		} else if (matchesKey(mc.options.keyLeft, event.key())) {
			startReverseRight(now);
		} else if (matchesKey(mc.options.keyRight, event.key())) {
			startReverseLeft(now);
		}
	}

	private void startShiftTap() {
		shiftTapUntilMs = System.currentTimeMillis() + Math.max(10L, Math.round(shiftTapTime.floatValue()));
		mc.options.keyShift.setDown(true);
	}

	private void startWTap() {
		wTapUntilMs = System.currentTimeMillis() + Math.max(10L, Math.round(wTapTime.floatValue()));
		mc.options.keyUp.setDown(true);
	}

	private void startReverseForward(long now) {
		reverseForwardUntilMs = now + REVERSE_TAP_MS;
		reverseForwardSampled = false;
		mc.options.keyUp.setDown(true);
	}

	private void startReverseBack(long now) {
		reverseBackUntilMs = now + REVERSE_TAP_MS;
		reverseBackSampled = false;
		mc.options.keyDown.setDown(true);
	}

	private void startReverseLeft(long now) {
		reverseLeftUntilMs = now + REVERSE_TAP_MS;
		reverseLeftSampled = false;
		mc.options.keyLeft.setDown(true);
	}

	private void startReverseRight(long now) {
		reverseRightUntilMs = now + REVERSE_TAP_MS;
		reverseRightSampled = false;
		mc.options.keyRight.setDown(true);
	}

	private void markReverseTapsSampled() {
		if (reverseForwardUntilMs > 0L) {
			reverseForwardSampled = true;
		}
		if (reverseBackUntilMs > 0L) {
			reverseBackSampled = true;
		}
		if (reverseLeftUntilMs > 0L) {
			reverseLeftSampled = true;
		}
		if (reverseRightUntilMs > 0L) {
			reverseRightSampled = true;
		}
	}

	private void holdTimedTapsIfActive() {
		long now = System.currentTimeMillis();
		if (shiftTapUntilMs > now) {
			mc.options.keyShift.setDown(true);
		}
		if (wTapUntilMs > now) {
			mc.options.keyUp.setDown(true);
		}
		if (reverseForwardUntilMs > now || (reverseForwardUntilMs > 0L && !reverseForwardSampled)) {
			mc.options.keyUp.setDown(true);
		}
		if (reverseBackUntilMs > now || (reverseBackUntilMs > 0L && !reverseBackSampled)) {
			mc.options.keyDown.setDown(true);
		}
		if (reverseLeftUntilMs > now || (reverseLeftUntilMs > 0L && !reverseLeftSampled)) {
			mc.options.keyLeft.setDown(true);
		}
		if (reverseRightUntilMs > now || (reverseRightUntilMs > 0L && !reverseRightSampled)) {
			mc.options.keyRight.setDown(true);
		}
	}

	private void clearTaps() {
		wTapUntilMs = 0L;
		shiftTapUntilMs = 0L;
		reverseForwardUntilMs = 0L;
		reverseBackUntilMs = 0L;
		reverseLeftUntilMs = 0L;
		reverseRightUntilMs = 0L;
		reverseForwardSampled = false;
		reverseBackSampled = false;
		reverseLeftSampled = false;
		reverseRightSampled = false;
	}

	private boolean hasActiveTaps() {
		return shiftTapUntilMs > 0L
				|| wTapUntilMs > 0L
				|| reverseForwardUntilMs > 0L
				|| reverseBackUntilMs > 0L
				|| reverseLeftUntilMs > 0L
				|| reverseRightUntilMs > 0L;
	}

	private boolean matchesKey(KeyMapping keyMapping, int keyCode) {
		InputConstants.Key key = InputConstants.getKey(keyMapping.saveString());
		return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
	}

	private boolean isValidAttackTarget(Entity entity) {
		return entity instanceof LivingEntity livingEntity
				&& livingEntity != mc.player
				&& livingEntity.isAlive();
	}

	public static Tap getInstance() {
		return INSTANCE;
	}
}
