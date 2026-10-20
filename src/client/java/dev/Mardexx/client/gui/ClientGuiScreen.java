package dev.Mardexx.client.gui;

import dev.Mardexx.api.render.Render2D;
import dev.Mardexx.api.render.MsdfFontRenderer;
import dev.Mardexx.client.modules.Module;
import dev.Mardexx.client.modules.ModuleCategory;
import dev.Mardexx.client.modules.ModuleManager;
import dev.Mardexx.client.hud.HudManager;
import dev.Mardexx.client.config.ClientConfig;
import dev.Mardexx.client.modules.impl.Render.Interface;
import dev.Mardexx.client.modules.impl.Render.Keybinds;
import dev.Mardexx.client.modules.impl.Render.PotionList;
import dev.Mardexx.client.modules.impl.Render.Cooldowns;
import dev.Mardexx.client.friends.FriendManager;
import dev.Mardexx.client.settings.BooleanSetting;
import dev.Mardexx.client.settings.ColorSetting;
import dev.Mardexx.client.settings.CustomSwordSetting;
import dev.Mardexx.client.settings.EnumBooleanMapSetting;
import dev.Mardexx.client.settings.EnumSetting;
import dev.Mardexx.client.settings.FloatRangeSetting;
import dev.Mardexx.client.settings.FloatSetting;
import dev.Mardexx.client.settings.HelperCustomItemsSetting;
import dev.Mardexx.client.settings.ItemStackSetting;
import dev.Mardexx.client.settings.KeySetting;
import dev.Mardexx.client.settings.Setting;
import dev.Mardexx.client.modules.impl.Combat.TripleItemPickerScreen;
import dev.Mardexx.client.modules.impl.Other.HelperCustomItemPickerScreen;
import dev.Mardexx.client.modules.impl.Render.CustomSwords;
import dev.Mardexx.client.theme.ThemeManager;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientGuiScreen extends Screen {
	private static final int CONFIGS_UTILITY_INDEX = 2;
	private static final int SETTINGS_UTILITY_INDEX = 3;

	public static float getGuiScale() {
		return dev.Mardexx.client.config.ClientConfig.getGuiScale();
	}

	public static void setGuiScale(float scale) {
		dev.Mardexx.client.config.ClientConfig.setGuiScale(scale);
	}

	public static boolean isGuiAnimations() {
		return dev.Mardexx.client.config.ClientConfig.isGuiAnimations();
	}

	public static void setGuiAnimations(boolean animations) {
		dev.Mardexx.client.config.ClientConfig.setGuiAnimations(animations);
	}

	public static void toggleGuiAnimations() {
		dev.Mardexx.client.config.ClientConfig.setGuiAnimations(!dev.Mardexx.client.config.ClientConfig.isGuiAnimations());
		dev.Mardexx.client.config.ClientConfig.save();
	}

	// Config page state
	private String configNameInput = "";
	private boolean configInputFocused;
	private int configScroll;

	// Settings page state
	private enum SettingsDrag { NONE, GUI_SCALE, HUD_SCALE, HUD_TRANSPARENCY }
	private SettingsDrag draggedSettingsControl = SettingsDrag.NONE;

	// Hover animations
	private final Map<Module, Float> moduleHoverAnimations = new IdentityHashMap<>();

	private double scaleMouseX(double mouseX) {
		float centerX = this.width / 2.0F;
		return (mouseX - centerX) / getGuiScale() + centerX;
	}

	private double scaleMouseY(double mouseY) {
		float centerY = this.height / 2.0F;
		return (mouseY - centerY) / getGuiScale() + centerY;
	}

	private static final int PANEL_WIDTH = 468;
	private static final int PANEL_HEIGHT = 270;
	private static final int PANEL_RADIUS = 10;
	private static final long GUI_ANIMATION_DURATION_MS = 220L;
	private static final float GUI_START_SCALE = 0.84F;
	private static final float PAGE_TRANSITION_SPEED = 0.085F;
	private static final float PAGE_TRANSITION_DISTANCE = 18.0F;
	private static final int PANEL_BACKGROUND = 0xFF0E0E0E;
	private static final int SIDEBAR_WIDTH = 30;
	private static final int TOPBAR_HEIGHT = 30;
	private static final int SIDEBAR = 0xFF111111;
	private static final int NAV_SECTION_WIDTH = 18;
	private static final int NAV_SECTION_HEIGHT = 22;
	private static final int NAV_SECTION_GAP = 0;
	private static final int NAV_ICON_COUNT = 5;
	private static final int UTILITY_ICON_COUNT = 4;
	private static final int FRIENDS_UTILITY_INDEX = 1;
	private static final int UTILITY_BOTTOM_PADDING = 8;
	private static final int NAV_SECTION_RADIUS = 3;
	private static final int NAV_SECTION = 0xFF181818;
	private static final int NAV_ICON_COLOR = 0xFFBDBDBD;
	private static final int SEARCH_TEXT_COLOR = 0xFF777777;
	private static final int SEARCH_SHORTCUT_BACKGROUND = 0xFF202020;
	private static final int SEARCH_SHORTCUT_OUTLINE = 0xFF343434;
	private static final int SEARCH_SHORTCUT_TEXT_COLOR = 0xFF9A9A9A;
	private static final int SEARCH_WIDTH = 108;
	private static final int SEARCH_HEIGHT = 18;
	private static final int SEARCH_RADIUS = 3;
	private static final String SEARCH_ICON = "l";
	private static final float SEARCH_ICON_SIZE = 9.0F;
	private static final float SEARCH_TEXT_SIZE = 8.2F;
	private static final float SEARCH_CARET_GAP = 0.8F;
	private static final String SEARCH_BASELINE_REFERENCE = "Ag";
	private static final float SEARCH_SHORTCUT_TEXT_SIZE = 7.0F;
	private static final int SEARCH_SHORTCUT_HEIGHT = 11;
	private static final int SEARCH_SHORTCUT_PADDING = 4;
	private static final int LAYOUT_TOGGLE_WIDTH = 54;
	private static final int LAYOUT_TOGGLE_HEIGHT = 20;
	private static final int LAYOUT_TOGGLE_OPTION_WIDTH = LAYOUT_TOGGLE_WIDTH / 2;
	private static final int LAYOUT_TOGGLE_GAP = 6;
	private static final int LAYOUT_TOGGLE_RADIUS = 5;
	private static final float LAYOUT_GRID_ICON_CELL_SIZE = 4.0F;
	private static final float LAYOUT_GRID_ICON_GAP = 1.5F;
	private static final float LAYOUT_GRID_ICON_RADIUS = 0.35F;
	private static final float LAYOUT_LIST_ICON_WIDTH = 10.5F;
	private static final float LAYOUT_LIST_ICON_HEIGHT = 1.6F;
	private static final float LAYOUT_LIST_ICON_GAP = 1.8F;
	private static final float LAYOUT_LIST_ICON_RADIUS = 0.35F;
	private static final float LAYOUT_TOGGLE_ANIMATION_SPEED = 0.24F;
	private static final int MODULE_AREA_PADDING_X = 12;
	private static final int MODULE_AREA_PADDING_Y = 10;
	private static final int MODULE_CARD_GAP = 6;
	private static final int MODULE_COLUMN_GAP = 8;
	private static final float MODULE_LIST_SCROLL_STEP = 24.0F;
	private static final int MODULE_CARD_HEIGHT = 26;
	private static final float MODULE_TOGGLE_SWITCH_WIDTH = 18.0F;
	private static final float MODULE_TOGGLE_SWITCH_HEIGHT = 9.0F;
	private static final float MODULE_TOGGLE_SWITCH_RADIUS = 4.5F;
	private static final float MODULE_TOGGLE_KNOB_RADIUS = 2.7F;
	private static final int MODULE_TOGGLE_TRACK_OFF = 0xFF191919;
	private static final int MODULE_TOGGLE_OUTLINE_OFF = 0xFF242424;
	private static final int MODULE_TOGGLE_KNOB_OFF = 0xFF5A5A5A;
	private static final int MODULE_TOGGLE_KNOB_ON = 0xFFFFFFFF;
	private static final int MODULE_SETTING_ROW_HEIGHT = 30;
	private static final int MODULE_SETTING_TOP_PADDING = 2;
	private static final int MODULE_SETTING_BOTTOM_PADDING = 4;
	private static final float MODULE_SETTING_TITLE_CENTER_OFFSET = 7.0F;
	private static final float MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET = 17.0F;
	private static final float MODULE_SETTING_DESCRIPTION_LINE_HEIGHT = 7.0F;
	private static final float MODULE_SETTING_DESCRIPTION_BOTTOM_PADDING = 4.0F;
	private static final int MODULE_SETTING_DESCRIPTION_MAX_LINES = 2;
	private static final int MODULE_SETTING_CONTROL_GAP = 12;
	private static final float MODULE_SETTINGS_SCROLL_STEP = 18.0F;
	private static final float MODULE_SETTINGS_PANEL_WIDTH = 175.0F;
	private static final float MODULE_SETTINGS_PANEL_HEIGHT = 240.0F;
	private static final float MODULE_SETTINGS_PANEL_PADDING = 12.0F;
	private static final float MODULE_SETTINGS_FILLET_RADIUS = 8.0F;
	private static final float MODULE_SETTINGS_CORNER_RADIUS = 8.0F;
	private static final int MODULE_CARD_RADIUS = 5;
	private static final int MODULE_CARD_BACKGROUND = 0xFF101010;
	private static final int MODULE_CARD_HOVER_BACKGROUND = 0xFF121212;
	private static final int MODULE_CARD_ENABLED_BACKGROUND = 0xFF111111;
	private static final int MODULE_CARD_OUTLINE = 0xFF171717;
	private static final int MODULE_TITLE_COLOR = 0xFF6F6F6F;
	private static final int MODULE_DESCRIPTION_COLOR = 0xFF585858;
	private static final int MODULE_ENABLED_TEXT_COLOR = 0xFFF5F5F5;
	private static final int MODULE_ENABLED_DESCRIPTION_COLOR = 0xFFA0A0A0;
	private static final int MODULE_SETTINGS_BACKGROUND = 0xFF0D0D0D;
	private static final int MODULE_BADGE_BACKGROUND = 0xFF111111;
	private static final int MODULE_BADGE_OUTLINE = 0xFF2A2A2A;
	private static final int MODULE_BADGE_TEXT_COLOR = 0xFF626262;
	private static final int MODULE_CONTROL_BACKGROUND = 0xFF111111;
	private static final int MODULE_CONTROL_OUTLINE = 0xFF242424;
	private static final int MODULE_CONTROL_TEXT_COLOR = 0xFFCFCFCF;
	private static final int FRIEND_INPUT_HEIGHT = 24;
	private static final int FRIEND_ROW_HEIGHT = 34;
	private static final int FRIEND_ROW_GAP = 6;
	private static final int MODULE_SLIDER_TRACK = 0xFF252525;
	private static final int MODULE_DISABLED_CHECKBOX = 0xFF121212;
	private static final int MODULE_INACTIVE_CHECKBOX = 0xFF1A1A1A;
	private static final int MODULE_INACTIVE_CHECKBOX_OUTLINE = 0xFF151515;
	private static final int MODULE_INACTIVE_CHECKBOX_MARK = 0xFF3A3A3A;
	private static final String CHECKMARK_ICON = "\uE200";
	private static final float CHECKMARK_ICON_SIZE = 10.6F;
	private static final float CHECKMARK_ICON_ROTATION = 0.0F;
	private static final float CHECKMARK_ICON_THICKNESS = 0.0F;
	private static final int MODULE_COMPACT_CONTROL_WIDTH = 96;
	private static final int MODULE_COLOR_CONTROL_WIDTH = 82;
	private static final int COLOR_PICKER_WIDTH = 158;
	private static final int COLOR_PICKER_HEIGHT = 190;
	private static final int COLOR_PICKER_GAP = 0;
	private static final float COLOR_PICKER_FILLET_RADIUS = 8.0F;
	private static final float COLOR_PICKER_CORNER_RADIUS = 8.0F;
	private static final int COLOR_PICKER_PADDING = 12;
	private static final int COLOR_FIELD_HEIGHT = 92;
	private static final int COLOR_HUE_HEIGHT = 10;
	private static final int COLOR_PICKER_BACKGROUND = 0xFF101010;
	private static final int COLOR_PICKER_INNER = 0xFF0B0B0B;
	private static final int COLOR_PICKER_OUTLINE = 0xFF242424;
	private static final int COLOR_PICKER_MARKER_OUTLINE = 0xFF050505;
	private static final int COLOR_PICKER_MARKER = 0xFFF5F5F5;
	private static final float COLOR_PICKER_ANIMATION_SPEED = 0.24F;
	private static final int COLOR_GRADIENT_STEPS = 64;
	private static final int CUSTOM_SWORD_PANEL_WIDTH = Math.round(COLOR_PICKER_WIDTH * 1.25F);
	private static final int CUSTOM_SWORD_PANEL_HEIGHT = Math.round(COLOR_PICKER_HEIGHT * 1.25F);
	private static final float CUSTOM_SWORD_ROW_HEIGHT = 76.0F;
	private static final float CUSTOM_SWORD_TAB_HEIGHT = 45.0F;
	private static final float CUSTOM_SWORD_TAB_GAP = 4.0F;
	private static final int CUSTOM_SWORD_MODEL_COLUMNS = 4;
	private static final int CUSTOM_SWORD_MODEL_VISIBLE_ROWS = 3;
	private static final float CUSTOM_SWORD_MODEL_CELL_GAP = 5.0F;
	private static final float CUSTOM_SWORD_MODEL_CELL_HEIGHT = 40.0F;
	private static final float CUSTOM_SWORD_SEARCH_HEIGHT = 20.0F;
	private static final float HELPER_CUSTOM_ITEM_HEIGHT = 24.0F;
	private static final float HELPER_CUSTOM_ITEM_GAP = 4.0F;
	private static final float HELPER_CUSTOM_ADD_HEIGHT = 18.0F;
	private static final float HELPER_CUSTOM_PROFILE_HEIGHT = 18.0F;
	private static final int MODULE_RANGE_CONTROL_WIDTH = MODULE_COMPACT_CONTROL_WIDTH;
	private static final int MODULE_ENUM_OPTION_HEIGHT = 16;
	private static final int MODULE_ENUM_DROPDOWN_GAP = 3;
	private static final float MODULE_ENUM_DROPDOWN_BOTTOM_PADDING = 8.0F;
	private static final int MODULE_ENUM_DROPDOWN_RADIUS = 4;
	private static final int MODULE_ENUM_OPTION_HIGHLIGHT = 0xFF171717;
	private static final int MODULE_MULTISELECT_TAG_GAP = 4;
	private static final int MODULE_MULTISELECT_TAG_BACKGROUND = 0xFF090909;
	private static final int MODULE_MULTISELECT_TAG_OUTLINE = 0xFF151515;
	private static final float MODULE_MULTISELECT_TAG_HEIGHT = 12.0F;
	private static final float MODULE_MULTISELECT_TAG_PADDING_X = 4.0F;
	private static final float MODULE_MULTISELECT_TAG_REMOVE_WIDTH = 8.0F;
	private static final float MODULE_MULTISELECT_TAG_MIN_LABEL_WIDTH = 1.0F;
	private static final float MODULE_RANGE_VALUE_BOX_WIDTH = 16.0F;
	private static final float MODULE_RANGE_VALUE_BOX_GAP = 6.0F;
	private static final float MODULE_RANGE_HANDLE_SIZE = 7.0F;
	private static final float MODULE_RANGE_HANDLE_ACTIVE_SIZE = 7.8F;
	private static final float MODULE_RANGE_HANDLE_OUTLINE_SIZE = 9.8F;
	private static final float MODULE_RANGE_HANDLE_ANIMATION_SPEED = 0.35F;
	private static final float MODULE_RANGE_TRACK_HEIGHT = 4.0F;
	private static final float MODULE_RANGE_VALUE_SIZE = 7.0F;
	private static final float MODULE_TITLE_SIZE = 9.4F;
	private static final float MODULE_DESCRIPTION_SIZE = 7.8F;
	private static final float MODULE_DESCRIPTION_LINE_HEIGHT = 9.2F;
	private static final float MODULE_TITLE_VERTICAL_OFFSET = 0.0F;
	private static final float MODULE_SETTING_SIZE = 6.8F;
	private static final float MODULE_SETTING_TITLE_SIZE = 7.4F;
	private static final float MODULE_SETTING_DESCRIPTION_SIZE = 6.4F;
	private static final float MODULE_BADGE_TEXT_SIZE = 6.8F;
	private static final int MODULE_BADGE_HEIGHT = SEARCH_SHORTCUT_HEIGHT;
	private static final int MODULE_BADGE_HORIZONTAL_PADDING = SEARCH_SHORTCUT_PADDING;
	private static final int MODULE_BADGE_RADIUS = 3;
	private static final float MODULE_LOADING_ICON_SIZE = 11.0F;
	private static final int MODULE_LOADING_ICON_DOTS = 8;
	private static final float MODULE_LOADING_ICON_RING_RADIUS = 4.25F;
	private static final float MODULE_LOADING_ICON_DOT_RADIUS = 1.2F;
	private static final String MODULE_SETTINGS_CLOSED_ICON = "b";
	private static final String MODULE_SETTINGS_OPEN_ICON = "c";
	private static final float MODULE_SETTINGS_CLOSED_ICON_SIZE = 10.0F;
	private static final float MODULE_SETTINGS_OPEN_ICON_SIZE = 11.5F;
	private static final float INTER_BOLD_OFFSET = 0.22F;
	private static final float MODULE_TITLE_BOLD_OFFSET = 0.24F;
	private static final float NAV_ICON_SIZE = 14.0F;
	private static final float NAV_ICON_HOVER_ROTATION = 16.0F;
	private static final float NAV_ICON_ROTATION_SPEED = 0.35F;
	private static final float MODULE_ICON_ENABLED_ROTATION = 90.0F;
	private static final float MODULE_ICON_DISABLED_ROTATION = -90.0F;
	private static final float MODULE_ICON_ROTATION_SPEED = 0.28F;
	private static final String SWORDS_ICON = "\uE000";
	private static final String WIND_ICON = "\uE001";
	private static final String RENDER_ICON = "c";
	private static final String PLAYER_ICON = "d";
	private static final String OTHER_ICON = "e";
	private static final String THEMES_ICON = "\uE100";
	private static final String FRIENDS_ICON = "\uE101";
	private static final String CONFIGS_ICON = "\uE102";
	private static final String SETTINGS_ICON = "\uE103";
	private static final int LOGO_COLOR = 0xFFEAFBFF;
	private static final float LOGO_SIZE = 15.0F;
	private static final float LOGO_LETTER_SPACING = 1.0F;
	private static final String LOGO_TEXT = "HATRED";
	private static final MsdfFontRenderer ICON_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/icons/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/icons/font.json")
	);
	private static final MsdfFontRenderer HATRED_CATEGORY_ICON_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/category_icons/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/category_icons/font.json")
	);
	private static final MsdfFontRenderer HATRED_MINCED_ICON_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/iconsminced/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/hatred/iconsminced/font.json")
	);
	private static final MsdfFontRenderer BOTTOM_ICON_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/bottom_icons/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/bottom_icons/font.json")
	);
	private static final MsdfFontRenderer MODULE_ICON_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/module_icons/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/module_icons/font.json")
	);
	private static final MsdfFontRenderer CHECKMARK_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/checkmark/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/checkmark/font.json")
	);
	private static final MsdfFontRenderer LOGO_FONT = new MsdfFontRenderer(
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.png"),
			ResourceLocation.fromNamespaceAndPath("modid", "fonts/msdf/inter/font.json")
	);
	private static IconGroup savedIconGroup = IconGroup.CATEGORY;
	private static int savedIconIndex;
	private static int savedUtilityIconIndex;
	private IconGroup selectedIconGroup;
	private int selectedIconIndex;
	private int selectedUtilityIconIndex;
	private PageState previousPage;
	private float pageTransitionProgress = 1.0F;
	private int pageTransitionDirection = 1;
	private Module expandedModule;
	private Module bindingModule;
	private KeySetting bindingSetting;
	private HelperCustomItemsSetting bindingHelperCustomSetting;
	private int bindingHelperCustomIndex = -1;
	private HelperCustomItemsSetting renamingHelperCustomSetting;
	private String helperCustomRenameText = "";
	private String searchText = "";
	private boolean searchFocused;
	private String friendNameInput = "";
	private String friendAliasInput = "";
	private String customSwordSearch = "";
	private String editingFriendName;
	private FriendInputFocus friendInputFocus = FriendInputFocus.NONE;
	private boolean customSwordSearchFocused;
	private int friendScroll;
	private float targetFriendScroll;
	private boolean singleColumnModules;
	private float layoutToggleProgress;
	private float moduleScrollOffset;
	private float targetModuleScrollOffset;
	private boolean closing;
	private long animationStartMillis;
	private long lastFrameMillis;
	private float closingStartProgress = 1.0F;
	private final float[] iconRotations = new float[NAV_ICON_COUNT];
	private final float[] utilityIconRotations = new float[UTILITY_ICON_COUNT];
	private final Map<Module, Float> settingAnimations = new IdentityHashMap<>();
	private final Map<Module, Float> toggleAnimations = new IdentityHashMap<>();
	private final Map<Setting<?>, Float> enumDropdownAnimations = new IdentityHashMap<>();
	private final Map<Module, Float> moduleIconRotations = new IdentityHashMap<>();
	private final Map<Setting<?>, Boolean> expandedEnumSettings = new IdentityHashMap<>();
	private final Map<Module, Float> settingsScrollOffsets = new IdentityHashMap<>();
	private final Map<Module, Float> targetSettingsScrollOffsets = new IdentityHashMap<>();
	private final Map<FloatRangeSetting, Float> rangeFromHandleAnimations = new IdentityHashMap<>();
	private final Map<FloatRangeSetting, Float> rangeToHandleAnimations = new IdentityHashMap<>();
	private final Map<FloatSetting, Float> sliderHandleAnimations = new IdentityHashMap<>();
	private final Map<ColorSetting, Float> colorPickerHues = new IdentityHashMap<>();
	private FloatRangeSetting draggedRangeSetting;
	private RangeHandle draggedRangeHandle;
	private FloatSetting draggedSliderSetting;
	private ColorSetting colorPickerSetting;
	private CustomSwordSetting customSwordSetting;
	private CustomSwords.Target customSwordTarget;
	private ColorPickerDrag draggedColorPickerControl;
	private boolean colorPickerOpen;
	private boolean customSwordPanelOpen;
	private boolean moduleSettingsOpen;
	private Module activeModuleSettings;
	private float colorPickerProgress;
	private float customSwordPanelProgress;
	private float moduleSettingsProgress;
	private float moduleSettingsScroll;
	private float targetModuleSettingsScroll;
	private int customSwordPanelScroll;

	public ClientGuiScreen() {
		super(Component.literal("Client GUI"));
		selectedIconGroup = savedIconGroup;
		selectedIconIndex = Math.max(0, Math.min(NAV_ICON_COUNT - 1, savedIconIndex));
		selectedUtilityIconIndex = Math.max(0, Math.min(UTILITY_ICON_COUNT - 1, savedUtilityIconIndex));
		animationStartMillis = Util.getMillis();
		lastFrameMillis = animationStartMillis;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		float frameDelta = frameDelta(now);
		float animationProgress = animationProgress(now);
		if (closing && animationProgress <= 0.0F) {
			Minecraft.getInstance().setScreen(null);
			return;
		}

		updateIconRotations(mouseX, mouseY, frameDelta);
		updateSettingAnimations(frameDelta);
		updateModuleToggleAnimations(frameDelta);
		updateScrollAnimations(frameDelta);
		updateModuleSettingsAnimation(frameDelta);
		updateEnumDropdownAnimations(frameDelta);
		updateRangeHandleAnimations(frameDelta);
		updateLayoutToggleAnimation(frameDelta);
		updateColorPickerAnimation(frameDelta);
		updateCustomSwordPanelAnimation(frameDelta);
		updatePageTransition(frameDelta);

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float easedProgress = ease(animationProgress);
		float scale = (GUI_START_SCALE + (1.0F - GUI_START_SCALE) * easedProgress) * getGuiScale();
		double scaledMouseX = scaleMouseX(mouseX);
		double scaledMouseY = scaleMouseY(mouseY);
		updateModuleHoverAnimations(frameDelta, scaledMouseX, scaledMouseY);
		float centerX = this.width / 2.0F;
		float centerY = this.height / 2.0F;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, centerY, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
		renderPanelBackground(guiGraphics, left, top, easedProgress);
		renderNavigationSections(guiGraphics, left, top, easedProgress);
		renderUtilitySections(guiGraphics, left, top, easedProgress);
		renderPageTransition(guiGraphics, left, top, (int) Math.round(scaledMouseX), (int) Math.round(scaledMouseY), easedProgress, frameDelta);
		renderLogo(guiGraphics, left, top, easedProgress);
		if (selectedIconGroup == IconGroup.CATEGORY) {
			renderSearchBar(guiGraphics, left, top, easedProgress);
			renderLayoutToggle(guiGraphics, left, top, easedProgress);
		}
		renderModuleSettingsPanel(guiGraphics, left, top, easedProgress);
		renderColorPicker(guiGraphics, left, top, easedProgress);
		renderCustomSwordPanel(guiGraphics, left, top, easedProgress);
		guiGraphics.pose().popPose();
	}

	private static void renderPanelBackground(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int contentX = left + SIDEBAR_WIDTH;
		int contentY = top + TOPBAR_HEIGHT;
		int contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH;
		int contentHeight = PANEL_HEIGHT - TOPBAR_HEIGHT;
		int sidebarHeight = PANEL_HEIGHT - TOPBAR_HEIGHT;

		Render2D.roundedRect(guiGraphics, left, top, PANEL_WIDTH, TOPBAR_HEIGHT, PANEL_RADIUS, PANEL_RADIUS, 0, 0, alpha(SIDEBAR, alpha));
		Render2D.roundedRect(guiGraphics, left, top + TOPBAR_HEIGHT, SIDEBAR_WIDTH, sidebarHeight, 0, 0, 0, PANEL_RADIUS, alpha(SIDEBAR, alpha));
		Render2D.roundedRect(guiGraphics, contentX, contentY, contentWidth, contentHeight, 0, 0, PANEL_RADIUS, 0, alpha(PANEL_BACKGROUND, alpha));
	}

	private void renderNavigationSections(GuiGraphics guiGraphics, int left, int top, float alpha) {
		renderNavigationBackground(guiGraphics, left, top, alpha);
		for (int index = 0; index < NAV_ICON_COUNT; index++) {
			renderNavigationIconButton(guiGraphics, left, top, index, alpha);
		}
	}

	private void renderNavigationBackground(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int navX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int navY = iconButtonY(top, 0);
		int navHeight = NAV_ICON_COUNT * NAV_SECTION_HEIGHT + (NAV_ICON_COUNT - 1) * NAV_SECTION_GAP;
		Render2D.roundedRect(guiGraphics, navX, navY, NAV_SECTION_WIDTH, navHeight, NAV_SECTION_RADIUS, alpha(NAV_SECTION, alpha));
	}

	private void renderNavigationIconButton(GuiGraphics guiGraphics, int left, int top, int index, float alpha) {
		int navX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int navY = iconButtonY(top, index);
		renderNavigationIcon(guiGraphics, navX, navY, index, alpha);
	}

	private void renderNavigationIcon(GuiGraphics guiGraphics, int navX, int navY, int index, float alpha) {
		String icon = navIcon(index);
		MsdfFontRenderer iconFont = navIconFont(index);
		float iconSize = navIconSize(index);
		float iconCenterX = navX + NAV_SECTION_WIDTH / 2.0F;
		float iconX = iconFont.centeredX(icon, iconSize, iconCenterX);
		float iconCenterY = navY + NAV_SECTION_HEIGHT / 2.0F;
		float iconBaselineY = iconFont.centeredBaseline(icon, iconSize, iconCenterY);
		int iconColor = selectedIconGroup == IconGroup.CATEGORY && index == selectedIconIndex ? ThemeManager.accent() : NAV_ICON_COLOR;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(iconCenterX, iconCenterY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(iconRotations[index]));
		guiGraphics.pose().translate(-iconCenterX, -iconCenterY, 0.0F);
		iconFont.draw(guiGraphics, icon, iconX, iconBaselineY, iconSize, alpha(iconColor, alpha));
		guiGraphics.pose().popPose();
	}

	private void renderUtilitySections(GuiGraphics guiGraphics, int left, int top, float alpha) {
		renderUtilityBackground(guiGraphics, left, top, alpha);
		for (int index = 0; index < UTILITY_ICON_COUNT; index++) {
			renderUtilityIconButton(guiGraphics, left, top, index, alpha);
		}
	}

	private void renderUtilityBackground(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int utilityX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int utilityY = utilityIconY(top, 0);
		int utilityHeight = UTILITY_ICON_COUNT * NAV_SECTION_HEIGHT + (UTILITY_ICON_COUNT - 1) * NAV_SECTION_GAP;
		Render2D.roundedRect(guiGraphics, utilityX, utilityY, NAV_SECTION_WIDTH, utilityHeight, NAV_SECTION_RADIUS, alpha(NAV_SECTION, alpha));
	}

	private void renderUtilityIconButton(GuiGraphics guiGraphics, int left, int top, int index, float alpha) {
		int utilityX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int utilityY = utilityIconY(top, index);
		renderUtilityIcon(guiGraphics, utilityX, utilityY, index, alpha);
	}

	private void renderUtilityIcon(GuiGraphics guiGraphics, int utilityX, int utilityY, int index, float alpha) {
		String icon = utilityIcon(index);
		float iconSize = utilityIconSize(index);
		float iconCenterX = utilityX + NAV_SECTION_WIDTH / 2.0F;
		float iconX = BOTTOM_ICON_FONT.centeredX(icon, iconSize, iconCenterX);
		float iconCenterY = utilityY + NAV_SECTION_HEIGHT / 2.0F;
		float iconBaselineY = BOTTOM_ICON_FONT.centeredBaseline(icon, iconSize, iconCenterY);
		boolean themePickerActive = index == 0 && colorPickerOpen && colorPickerSetting == ThemeManager.accentSetting();
		int iconColor = themePickerActive || (selectedIconGroup == IconGroup.UTILITY && index == selectedUtilityIconIndex)
				? ThemeManager.accent()
				: NAV_ICON_COLOR;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(iconCenterX, iconCenterY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(utilityIconRotations[index]));
		guiGraphics.pose().translate(-iconCenterX, -iconCenterY, 0.0F);
		BOTTOM_ICON_FONT.draw(guiGraphics, icon, iconX, iconBaselineY, iconSize, alpha(iconColor, alpha));
		renderThemePaletteDots(guiGraphics, index, iconCenterX, iconCenterY, iconSize, alpha);
		guiGraphics.pose().popPose();
	}

	private static void renderThemePaletteDots(GuiGraphics guiGraphics, int index, float iconCenterX, float iconCenterY, float iconSize, float alpha) {
		if (index != 0) {
			return;
		}

		int dotColor = alpha(NAV_SECTION, alpha);
		float iconLeft = iconCenterX - iconSize / 2.0F;
		float iconTop = iconCenterY - iconSize / 2.0F;
		float radius = iconSize * 0.062F;
		renderPaletteDot(guiGraphics, iconLeft + iconSize * 0.325F, iconTop + iconSize * 0.430F, radius, dotColor);
		renderPaletteDot(guiGraphics, iconLeft + iconSize * 0.500F, iconTop + iconSize * 0.290F, radius, dotColor);
		renderPaletteDot(guiGraphics, iconLeft + iconSize * 0.675F, iconTop + iconSize * 0.430F, radius, dotColor);
	}

	private static void renderPaletteDot(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int color) {
		Render2D.roundedRect(guiGraphics, centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, color);
	}

	private static void renderLogo(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int logoX = left + 10;
		float logoCenterY = top + TOPBAR_HEIGHT / 2.0F;
		float logoBaselineY = LOGO_FONT.centeredBaseline(LOGO_TEXT, LOGO_SIZE, logoCenterY);
		LOGO_FONT.draw(guiGraphics, LOGO_TEXT, logoX, logoBaselineY, LOGO_SIZE, alpha(LOGO_COLOR, alpha), LOGO_LETTER_SPACING);
	}

	private void renderModules(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY, float alpha, float frameDelta) {
		if (selectedIconGroup != IconGroup.CATEGORY) {
			return;
		}

		int areaX = moduleAreaX(left);
		int areaY = moduleAreaY(top);
		int areaWidth = moduleAreaWidth();
		int areaHeight = moduleAreaHeight();
		clampedModuleScroll(left, top);
		guiGraphics.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
		for (ModuleLayout layout : moduleLayouts(left, top)) {
			renderModuleCard(guiGraphics, layout, mouseX, mouseY, alpha, frameDelta);
		}
		guiGraphics.disableScissor();
	}

	private void renderModuleCard(GuiGraphics guiGraphics, ModuleLayout layout, int mouseX, int mouseY, float alpha, float frameDelta) {
		Module module = layout.module();
		boolean hovered = isInside(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height());
		float hoverAnim = moduleHoverAnimations.getOrDefault(module, 0.0F);
		int baseBg = module.enabled() ? MODULE_CARD_ENABLED_BACKGROUND : MODULE_CARD_BACKGROUND;
		int background = lerpColor(baseBg, MODULE_CARD_HOVER_BACKGROUND, hoverAnim);
		int cardOutline = lerpColor(MODULE_CARD_OUTLINE, ThemeManager.darkAccent(), hoverAnim * 0.45F);
		Render2D.roundedRect(guiGraphics, layout.x(), layout.y(), layout.width(), layout.height(), MODULE_CARD_RADIUS, alpha(cardOutline, alpha));
		Render2D.roundedRect(guiGraphics, layout.x() + 1, layout.y() + 1, layout.width() - 2, layout.height() - 2, MODULE_CARD_RADIUS - 1, alpha(background, alpha));

		int titleColor = module.enabled() ? MODULE_ENABLED_TEXT_COLOR : MODULE_TITLE_COLOR;
		int moduleIconColor = module.enabled() ? ThemeManager.accent() : MODULE_TITLE_COLOR;
		float settingsProgress = (activeModuleSettings == module) ? ease(moduleSettingsProgress) : 0.0F;
		int moduleActionColor = (activeModuleSettings == module && moduleSettingsProgress > 0.01F)
				? ThemeManager.accent()
				: (module.enabled() ? ThemeManager.accent() : MODULE_BADGE_TEXT_COLOR);
		float headerCenterY = layout.y() + MODULE_CARD_HEIGHT / 2.0F;
		float loadingIconCenterX = layout.x() + 12.0F;
		float loadingIconRotation = updateModuleIconRotation(module, frameDelta);
		renderModuleLoadingIcon(guiGraphics, loadingIconCenterX, headerCenterY, loadingIconRotation, alpha(moduleIconColor, alpha));

		float settingsIconCenterX = layout.x() + layout.width() - 11.0F;
		float toggleSwitchRight = settingsIconCenterX - 9.0F;
		float toggleSwitchLeft = toggleSwitchRight - MODULE_TOGGLE_SWITCH_WIDTH;
		float toggleProgress = ease(toggleAnimation(module));
		renderModuleToggleSwitch(guiGraphics, toggleSwitchRight, headerCenterY, toggleProgress, alpha);

		float badgeRight = toggleSwitchLeft - 5.0F;
		String bindText = moduleBindText(module);
		float iconRight = loadingIconCenterX + MODULE_LOADING_ICON_RING_RADIUS + 2.0F;
		float titleX = iconRight + 3.0F;
		float titleMaxWidth = badgeRight - moduleBadgeWidth(bindText) - titleX - 4.0F;
		drawModuleTitleText(guiGraphics, trimToWidth(module.name(), MODULE_TITLE_SIZE, titleMaxWidth), titleX, headerCenterY + MODULE_TITLE_VERTICAL_OFFSET, MODULE_TITLE_SIZE, alpha(titleColor, alpha));
		renderModuleBadge(guiGraphics, bindText, badgeRight, headerCenterY, alpha);
		renderSettingsIcon(guiGraphics, settingsProgress, settingsIconCenterX, headerCenterY, alpha, moduleActionColor);
	}

	private static void renderModuleToggleSwitch(GuiGraphics guiGraphics, float right, float centerY, float toggleProgress, float alpha) {
		float width = MODULE_TOGGLE_SWITCH_WIDTH;
		float height = MODULE_TOGGLE_SWITCH_HEIGHT;
		float x = right - width;
		float y = centerY - height / 2.0F;

		int activeColor = ThemeManager.accent();
		int trackColor = lerpColor(MODULE_TOGGLE_TRACK_OFF, activeColor, toggleProgress);
		int outlineColor = lerpColor(MODULE_TOGGLE_OUTLINE_OFF, activeColor, toggleProgress);
		int knobColor = lerpColor(MODULE_TOGGLE_KNOB_OFF, MODULE_TOGGLE_KNOB_ON, toggleProgress);

		Render2D.roundedRect(guiGraphics, x, y, width, height, MODULE_TOGGLE_SWITCH_RADIUS, alpha(outlineColor, alpha));
		Render2D.roundedRect(guiGraphics, x + 0.6F, y + 0.6F, width - 1.2F, height - 1.2F, MODULE_TOGGLE_SWITCH_RADIUS - 0.5F, alpha(trackColor, alpha));

		float knobMinX = x + MODULE_TOGGLE_KNOB_RADIUS + 1.2F;
		float knobMaxX = x + width - MODULE_TOGGLE_KNOB_RADIUS - 1.2F;
		float knobCenterX = knobMinX + (knobMaxX - knobMinX) * toggleProgress;
		float knobCenterY = centerY;
		float knobDiameter = MODULE_TOGGLE_KNOB_RADIUS * 2.0F;

		Render2D.roundedRect(guiGraphics, knobCenterX - MODULE_TOGGLE_KNOB_RADIUS, knobCenterY - MODULE_TOGGLE_KNOB_RADIUS, knobDiameter, knobDiameter, MODULE_TOGGLE_KNOB_RADIUS, alpha(knobColor, alpha));
	}

	private static void renderModuleBadge(GuiGraphics guiGraphics, String text, float right, float centerY, float alpha) {
		int badgeWidth = moduleBadgeWidth(text);
		int badgeHeight = MODULE_BADGE_HEIGHT;
		float badgeX = right - badgeWidth;
		float badgeY = centerY - badgeHeight / 2.0F;
		Render2D.roundedRect(guiGraphics, badgeX, badgeY, badgeWidth, badgeHeight, MODULE_BADGE_RADIUS, alpha(MODULE_BADGE_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, badgeX + 1, badgeY + 1, badgeWidth - 2, badgeHeight - 2, MODULE_BADGE_RADIUS - 1, alpha(MODULE_BADGE_BACKGROUND, alpha));
		LOGO_FONT.draw(
				guiGraphics,
				text,
				LOGO_FONT.centeredX(text, MODULE_BADGE_TEXT_SIZE, badgeX + badgeWidth / 2.0F),
				LOGO_FONT.centeredBaseline(text, MODULE_BADGE_TEXT_SIZE, centerY),
				MODULE_BADGE_TEXT_SIZE,
				alpha(MODULE_BADGE_TEXT_COLOR, alpha)
		);
	}

	private static void renderModuleLoadingIcon(GuiGraphics guiGraphics, float centerX, float centerY, float rotation, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, centerY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
		for (int index = 0; index < MODULE_LOADING_ICON_DOTS; index++) {
			double angle = -Math.PI / 2.0 + index * Math.PI * 2.0 / MODULE_LOADING_ICON_DOTS;
			float dotCenterX = (float) Math.cos(angle) * MODULE_LOADING_ICON_RING_RADIUS;
			float dotCenterY = (float) Math.sin(angle) * MODULE_LOADING_ICON_RING_RADIUS;
			float dotSize = MODULE_LOADING_ICON_DOT_RADIUS * 2.0F;
			Render2D.roundedRect(
					guiGraphics,
					dotCenterX - MODULE_LOADING_ICON_DOT_RADIUS,
					dotCenterY - MODULE_LOADING_ICON_DOT_RADIUS,
					dotSize,
					dotSize,
					MODULE_LOADING_ICON_DOT_RADIUS,
					color
			);
		}
		guiGraphics.pose().popPose();
	}

	private static void renderSettingsIcon(GuiGraphics guiGraphics, float openProgress, float centerX, float centerY, float alpha, int closedColor) {
		float closedAlpha = alpha * (1.0F - openProgress);
		float openAlpha = alpha * openProgress;
		if (closedAlpha > 0.01F) {
			renderSettingsIconGlyph(guiGraphics, MODULE_SETTINGS_CLOSED_ICON, centerX, centerY, MODULE_SETTINGS_CLOSED_ICON_SIZE, alpha(closedColor, closedAlpha));
		}
		if (openAlpha > 0.01F) {
			renderSettingsIconGlyph(guiGraphics, MODULE_SETTINGS_OPEN_ICON, centerX, centerY, MODULE_SETTINGS_OPEN_ICON_SIZE, alpha(closedColor, openAlpha));
		}
	}

	private static void renderSettingsIconGlyph(GuiGraphics guiGraphics, String icon, float centerX, float centerY, float size, int color) {
		float x = MODULE_ICON_FONT.centeredX(icon, size, centerX);
		float y = MODULE_ICON_FONT.centeredBaseline(icon, size, centerY);
		MODULE_ICON_FONT.draw(guiGraphics, icon, x, y, size, color);
	}

	private static int moduleBadgeWidth(String text) {
		return Math.round(LOGO_FONT.width(text, MODULE_BADGE_TEXT_SIZE)) + MODULE_BADGE_HORIZONTAL_PADDING * 2;
	}

	private String moduleBindText(Module module) {
		if (module == bindingModule) {
			return "...";
		}
		if (module.keyBind() < 0) {
			return "n/a";
		}
		return keyName(module.keyBind());
	}

	private static String keyName(int keyCode) {
		String name = GLFW.glfwGetKeyName(keyCode, 0);
		if (name != null && !name.isEmpty()) {
			return name.toUpperCase(Locale.ROOT);
		}
		return switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL";
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "ALT";
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			case GLFW.GLFW_KEY_ENTER -> "ENTER";
			default -> "KEY" + keyCode;
		};
	}

	private void renderModuleSettingsPanel(GuiGraphics guiGraphics, int left, int top, float guiAlpha) {
		if (activeModuleSettings == null || moduleSettingsProgress <= 0.01F) {
			return;
		}

		float progress = ease(moduleSettingsProgress);
		float alpha = guiAlpha * progress;
		float panelX = moduleSettingsPanelX(left);
		float panelY = moduleSettingsPanelY(top);
		float width = MODULE_SETTINGS_PANEL_WIDTH;
		float height = MODULE_SETTINGS_PANEL_HEIGHT;
		float r = MODULE_SETTINGS_FILLET_RADIUS;
		float cornerRadius = MODULE_SETTINGS_CORNER_RADIUS;
		int bgColor = alpha(PANEL_BACKGROUND, alpha);

		int scissorLeft = Math.round(left + PANEL_WIDTH - 0.5F);
		guiGraphics.enableScissor(
				scissorLeft,
				Math.round(panelY - r - 4.0F),
				Math.round(this.width),
				Math.round(panelY + height + r + 4.0F)
		);

		try {
			Render2D.softShadow(guiGraphics, panelX, panelY, width, height, cornerRadius, 6.0F, alpha(0x80000000, alpha));
			Render2D.attachedTabRightBody(guiGraphics, panelX, panelY, width, height, cornerRadius, r, bgColor);

			drawBoldInterText(guiGraphics, trimToWidth(activeModuleSettings.name().toUpperCase(Locale.ROOT), 7.8F, width - 36.0F), panelX + MODULE_SETTINGS_PANEL_PADDING, panelY + 13.0F, 7.8F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			drawBoldInterText(guiGraphics, "Settings", panelX + MODULE_SETTINGS_PANEL_PADDING, panelY + 27.0F, 6.4F, alpha(ThemeManager.accent(), alpha));
			drawRightAlignedBoldInterText(guiGraphics, "x", panelX + width - MODULE_SETTINGS_PANEL_PADDING, panelY + 13.0F, 7.5F, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));

			int settingX = Math.round(panelX + MODULE_SETTINGS_PANEL_PADDING);
			int settingWidth = Math.round(width - MODULE_SETTINGS_PANEL_PADDING * 2.0F);
			float contentTop = panelY + 38.0F;
			float contentBottom = panelY + height - 8.0F;
			float settingY = contentTop - moduleSettingsScroll;

			int clipTop = Math.round(contentTop);
			int clipBottom = Math.round(contentBottom);
			if (clipBottom > clipTop) {
				guiGraphics.enableScissor(settingX - 2, clipTop, Math.round(panelX + width + 4.0F), clipBottom);
				for (Setting<?> setting : orderedSettings(activeModuleSettings)) {
					renderSettingRow(guiGraphics, setting, settingX, settingY, settingWidth, alpha, MODULE_ENABLED_TEXT_COLOR, MODULE_DESCRIPTION_COLOR, activeModuleSettings.enabled());
					settingY += settingRowHeight(setting, settingWidth);
				}
				guiGraphics.disableScissor();
			}
		} finally {
			guiGraphics.disableScissor();
		}
	}

	private float moduleSettingsPanelX(int left) {
		float targetX = left + PANEL_WIDTH;
		float slideOffset = (MODULE_SETTINGS_PANEL_WIDTH + 2.0F) * (1.0F - ease(moduleSettingsProgress));
		return targetX - slideOffset;
	}

	private static float moduleSettingsPanelY(int top) {
		return top + (PANEL_HEIGHT - MODULE_SETTINGS_PANEL_HEIGHT) / 2.0F;
	}

	private void renderSettingRow(GuiGraphics guiGraphics, Setting<?> setting, float x, float y, int width, float alpha, int titleColor, int descriptionColor, boolean moduleEnabled) {
		float titleY = y + MODULE_SETTING_TITLE_CENTER_OFFSET;
		float descriptionY = y + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET;
		float controlRight = x + width;
		float controlWidth = settingControlWidth(setting, width);
		float controlX = controlRight - controlWidth;
		float textMaxWidth = settingTextMaxWidth(setting, width);

		drawBoldInterText(guiGraphics, trimToWidth(setting.displayName(), MODULE_SETTING_TITLE_SIZE, textMaxWidth), x, titleY, MODULE_SETTING_TITLE_SIZE, alpha(titleColor, alpha));
		if (!setting.description().isEmpty()) {
			List<String> descriptionLines = settingDescriptionLines(setting, width);
			for (int index = 0; index < descriptionLines.size(); index++) {
				drawBoldInterText(
						guiGraphics,
						descriptionLines.get(index),
						x,
						descriptionY + index * MODULE_SETTING_DESCRIPTION_LINE_HEIGHT,
						MODULE_SETTING_DESCRIPTION_SIZE,
						alpha(descriptionColor, alpha)
				);
			}
		}

		float topControlCenterY = settingTopControlCenterY(setting, y);
		float rangeControlCenterY = y + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET;
		if (setting instanceof HelperCustomItemsSetting customItemsSetting) {
			renderHelperCustomItems(guiGraphics, customItemsSetting, x, y + 25.0F, width, alpha, titleColor);
			return;
		}
		if (setting instanceof CustomSwordSetting customSwordSetting) {
			renderCustomSwordTabs(guiGraphics, customSwordSetting, x, y + 25.0F, width, alpha, titleColor);
			return;
		}
		if (setting instanceof ColorSetting colorSetting) {
			renderColorControl(guiGraphics, colorSetting, controlX, topControlCenterY, controlWidth, alpha, moduleEnabled);
			return;
		}
		if (setting instanceof EnumSetting<?> enumSetting) {
			renderEnumControl(guiGraphics, enumSetting, controlX, topControlCenterY, Math.round(controlWidth), alpha, enumDropdownAnimation(enumSetting), titleColor);
			return;
		}
		if (setting instanceof EnumBooleanMapSetting<?> mapSetting) {
			renderTargetControl(guiGraphics, mapSetting, controlX, topControlCenterY, Math.round(controlWidth), alpha, enumDropdownAnimation(mapSetting), titleColor);
			return;
		}
		if (setting instanceof FloatRangeSetting rangeSetting) {
			renderRangeControl(guiGraphics, rangeSetting, controlX, rangeControlCenterY, controlWidth, alpha, moduleEnabled, rangeHandleAnimation(rangeSetting, RangeHandle.FROM), rangeHandleAnimation(rangeSetting, RangeHandle.TO));
			return;
		}
		if (setting instanceof FloatSetting floatSetting) {
			renderSliderControl(guiGraphics, floatSetting, controlX, rangeControlCenterY, controlWidth, alpha, moduleEnabled, sliderHandleAnimation(floatSetting));
			return;
		}
		if (setting instanceof BooleanSetting booleanSetting) {
			renderCheckbox(guiGraphics, controlRight - 14, settingCheckboxY(setting, y), booleanSetting.enabled(), moduleEnabled, alpha);
			return;
		}
		if (setting instanceof ItemStackSetting itemStackSetting) {
			renderItemStackControl(guiGraphics, itemStackSetting, controlX, topControlCenterY, controlWidth, alpha, titleColor);
			return;
		}
		if (setting instanceof KeySetting keySetting) {
			String text = bindingSetting == keySetting ? "..." : keySetting.key() < 0 ? "None" : keyName(keySetting.key());
			drawRightAlignedBoldInterText(guiGraphics, text, controlRight, topControlCenterY, MODULE_SETTING_SIZE, alpha(titleColor, alpha));
		}
	}

	private static void renderItemStackControl(GuiGraphics guiGraphics, ItemStackSetting setting, float x, float centerY, float width, float alpha, int textColor) {
		Render2D.roundedRect(guiGraphics, x, centerY - 9.0F, width, 18.0F, 4.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, centerY - 8.0F, width - 2.0F, 16.0F, 3.0F, alpha(MODULE_CONTROL_BACKGROUND, alpha));

		float textX = x + 7.0F;
		if (setting.hasValue()) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x + 3.0F, centerY - 7.0F, 0.0F);
			guiGraphics.pose().scale(0.875F, 0.875F, 1.0F);
			guiGraphics.renderItem(setting.stack(), 0, 0);
			guiGraphics.pose().popPose();
			textX = x + 22.0F;
		}

		String label = setting.hasValue() ? setting.selectedName() : "Choose item";
		drawBoldInterText(
				guiGraphics,
				trimToWidth(label, MODULE_SETTING_SIZE, width - (textX - x) - 25.0F),
				textX,
				centerY,
				MODULE_SETTING_SIZE,
				alpha(setting.hasValue() ? textColor : MODULE_DESCRIPTION_COLOR, alpha)
		);

		float iconSize = 9.0F;
		float iconCenterX = x + width - 10.0F;
		float iconX = BOTTOM_ICON_FONT.centeredX(SETTINGS_ICON, iconSize, iconCenterX);
		float iconY = BOTTOM_ICON_FONT.centeredBaseline(SETTINGS_ICON, iconSize, centerY);
		BOTTOM_ICON_FONT.draw(guiGraphics, SETTINGS_ICON, iconX, iconY, iconSize, alpha(textColor, alpha));
	}

	private void renderCustomSwordTabs(GuiGraphics guiGraphics, CustomSwordSetting setting, float x, float y, int width, float alpha, int textColor) {
		CustomSwords.Target[] targets = CustomSwords.Target.values();
		float tabWidth = (width - (targets.length - 1) * CUSTOM_SWORD_TAB_GAP) / targets.length;
		for (int index = 0; index < targets.length; index++) {
			CustomSwords.Target target = targets[index];
			float tabX = x + index * (tabWidth + CUSTOM_SWORD_TAB_GAP);
			boolean selected = customSwordPanelOpen && customSwordSetting == setting && customSwordTarget == target;
			boolean enabled = setting.enabled(target);
			int outline = selected ? ThemeManager.darkAccent() : MODULE_CONTROL_OUTLINE;
			int background = selected ? MODULE_ENUM_OPTION_HIGHLIGHT : MODULE_CONTROL_BACKGROUND;
			Render2D.roundedRect(guiGraphics, tabX, y, tabWidth, CUSTOM_SWORD_TAB_HEIGHT, 4.0F, alpha(outline, alpha));
			Render2D.roundedRect(guiGraphics, tabX + 1.0F, y + 1.0F, tabWidth - 2.0F, CUSTOM_SWORD_TAB_HEIGHT - 2.0F, 3.0F, alpha(background, alpha));

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(tabX + tabWidth / 2.0F - 8.0F, y + 4.0F, 0.0F);
			renderCustomSwordPreviewItem(guiGraphics, CustomSwords.getInstance().previewStack(target), 0, 0);
			guiGraphics.pose().popPose();

			float labelCenterX = tabX + tabWidth / 2.0F;
			drawBoldInterText(
					guiGraphics,
					target.shortDisplay(),
					LOGO_FONT.centeredX(target.shortDisplay(), 5.7F, labelCenterX),
					y + 26.0F,
					5.7F,
					alpha(selected ? MODULE_ENABLED_TEXT_COLOR : MODULE_DESCRIPTION_COLOR, alpha)
			);
			renderCustomSwordTabCheckmark(guiGraphics, tabX + tabWidth / 2.0F - 5.0F, y + CUSTOM_SWORD_TAB_HEIGHT - 13.0F, enabled, alpha);

			float gearCenterX = tabX + tabWidth - 6.0F;
			float gearCenterY = y + 7.0F;
			BOTTOM_ICON_FONT.draw(
					guiGraphics,
					SETTINGS_ICON,
					BOTTOM_ICON_FONT.centeredX(SETTINGS_ICON, 6.0F, gearCenterX),
					BOTTOM_ICON_FONT.centeredBaseline(SETTINGS_ICON, 6.0F, gearCenterY),
					6.0F,
					alpha(selected ? ThemeManager.accent() : textColor, alpha)
			);
		}
	}

	private static void renderCustomSwordTabCheckmark(GuiGraphics guiGraphics, float x, float y, boolean enabled, float alpha) {
		Render2D.roundedRect(guiGraphics, x, y, 10.0F, 10.0F, 2.5F, alpha(enabled ? ThemeManager.darkAccent() : MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, 8.0F, 8.0F, 2.0F, alpha(enabled ? ThemeManager.accent() : COLOR_PICKER_INNER, alpha));
		if (!enabled) {
			return;
		}

		float centerX = x + 5.0F;
		float centerY = y + 5.0F;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, centerY, 0.0F);
		guiGraphics.pose().scale(0.62F, 0.62F, 1.0F);
		CHECKMARK_FONT.draw(
				guiGraphics,
				CHECKMARK_ICON,
				CHECKMARK_FONT.centeredX(CHECKMARK_ICON, CHECKMARK_ICON_SIZE, 0.0F),
				CHECKMARK_FONT.centeredBaseline(CHECKMARK_ICON, CHECKMARK_ICON_SIZE, 0.0F),
				CHECKMARK_ICON_SIZE,
				alpha(ThemeManager.textOnAccent(), alpha),
				CHECKMARK_ICON_THICKNESS
		);
		guiGraphics.pose().popPose();
	}

	private void renderHelperCustomItems(GuiGraphics guiGraphics, HelperCustomItemsSetting setting, float x, float y, int width, float alpha, int textColor) {
		float buttonGap = 4.0F;
		float arrowWidth = 20.0F;
		float actionWidth = 54.0F;
		float deleteWidth = 24.0F;
		float profileNameX = x + arrowWidth + buttonGap;
		float profileNameWidth = Math.max(46.0F, width - arrowWidth * 2.0F - actionWidth * 2.0F - deleteWidth - buttonGap * 6.0F);
		float nextX = profileNameX + profileNameWidth + buttonGap;
		float addProfileX = nextX + arrowWidth + buttonGap;
		float renameX = addProfileX + actionWidth + buttonGap;
		float deleteX = renameX + actionWidth + buttonGap;
		boolean renaming = renamingHelperCustomSetting == setting;

		renderHelperCustomButton(guiGraphics, "<", x, y, arrowWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, textColor, true);
		renderHelperCustomButton(guiGraphics, renaming
				? trimToWidth(helperCustomRenameText + "_", MODULE_SETTING_SIZE, profileNameWidth - 12.0F)
				: trimToWidth(setting.activeName(), MODULE_SETTING_SIZE, profileNameWidth - 12.0F),
				profileNameX, y, profileNameWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, textColor, true);
		renderHelperCustomButton(guiGraphics, ">", nextX, y, arrowWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, textColor, true);
		renderHelperCustomButton(guiGraphics, "+ Config", addProfileX, y, actionWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, textColor, true);
		renderHelperCustomButton(guiGraphics, "Rename", renameX, y, actionWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, textColor, true);
		renderHelperCustomButton(guiGraphics, "x", deleteX, y, deleteWidth, HELPER_CUSTOM_PROFILE_HEIGHT, alpha, 0xFFFF6B78, true);

		y += HELPER_CUSTOM_PROFILE_HEIGHT + HELPER_CUSTOM_ITEM_GAP;
		float addWidth = 84.0F;
		renderHelperCustomButton(guiGraphics, "+ Add item", x, y, addWidth, HELPER_CUSTOM_ADD_HEIGHT, alpha, textColor, false);

		float itemY = y + HELPER_CUSTOM_ADD_HEIGHT + HELPER_CUSTOM_ITEM_GAP;
		List<HelperCustomItemsSetting.Entry> entries = setting.entries();
		if (entries.isEmpty()) {
			drawBoldInterText(guiGraphics, "No custom items", x, itemY + 8.0F, MODULE_SETTING_SIZE, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			return;
		}

		for (int index = 0; index < entries.size(); index++) {
			HelperCustomItemsSetting.Entry entry = entries.get(index);
			float rowY = itemY + index * (HELPER_CUSTOM_ITEM_HEIGHT + HELPER_CUSTOM_ITEM_GAP);
			Render2D.roundedRect(guiGraphics, x, rowY, width, HELPER_CUSTOM_ITEM_HEIGHT, 5.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, x + 1.0F, rowY + 1.0F, width - 2.0F, HELPER_CUSTOM_ITEM_HEIGHT - 2.0F, 4.0F, alpha(MODULE_CONTROL_BACKGROUND, alpha));
			ItemStack stack = entry.stack();
			if (!stack.isEmpty()) {
				guiGraphics.renderItem(stack, Math.round(x + 5.0F), Math.round(rowY + 4.0F));
			}
			drawBoldInterText(guiGraphics, trimToWidth(entry.name(), MODULE_SETTING_SIZE, width - 104.0F), x + 27.0F, rowY + 9.0F, MODULE_SETTING_SIZE, alpha(textColor, alpha));
			drawBoldInterText(guiGraphics, "custom", x + 27.0F, rowY + 18.0F, 5.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));

			String keyText = bindingHelperCustomSetting == setting && bindingHelperCustomIndex == index
					? "..."
					: entry.key() < 0 ? "None" : keyName(entry.key());
			float keyWidth = 34.0F;
			float keyX = x + width - keyWidth - 24.0F;
			Render2D.roundedRect(guiGraphics, keyX, rowY + 4.0F, keyWidth, 16.0F, 4.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, keyX + 1.0F, rowY + 5.0F, keyWidth - 2.0F, 14.0F, 3.0F, alpha(COLOR_PICKER_INNER, alpha));
			drawBoldInterText(guiGraphics, trimToWidth(keyText, MODULE_SETTING_SIZE, keyWidth - 8.0F), keyX + 5.0F, rowY + 12.0F, MODULE_SETTING_SIZE, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));
			drawBoldInterText(guiGraphics, "x", x + width - 15.0F, rowY + 12.0F, 7.2F, alpha(0xFFFF6B78, alpha));
		}
	}

	private void renderHelperCustomButton(GuiGraphics guiGraphics, String text, float x, float y, float width, float height, float alpha, int textColor, boolean centered) {
		Render2D.roundedRect(guiGraphics, x, y, width, height, 4.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, 3.0F, alpha(MODULE_CONTROL_BACKGROUND, alpha));
		if (centered) {
			float textWidth = LOGO_FONT.width(text, MODULE_SETTING_SIZE);
			drawBoldInterText(guiGraphics, text, x + (width - textWidth) / 2.0F, y + height / 2.0F, MODULE_SETTING_SIZE, alpha(textColor, alpha));
		} else {
			drawBoldInterText(guiGraphics, text, x + 8.0F, y + height / 2.0F, MODULE_SETTING_SIZE, alpha(textColor, alpha));
		}
	}




	private void renderCustomSwordPanel(GuiGraphics guiGraphics, int left, int top, float guiAlpha) {
		if (customSwordSetting == null || customSwordTarget == null || customSwordPanelProgress <= 0.01F) {
			return;
		}

		float progress = ease(customSwordPanelProgress);
		float alpha = guiAlpha * progress;
		float panelX = customSwordPanelX(left);
		float panelY = customSwordPanelY(top);
		float width = CUSTOM_SWORD_PANEL_WIDTH;
		float height = CUSTOM_SWORD_PANEL_HEIGHT;
		float r = COLOR_PICKER_FILLET_RADIUS;
		float cornerRadius = COLOR_PICKER_CORNER_RADIUS;
		int bgColor = alpha(PANEL_BACKGROUND, alpha);

		float fieldX = panelX + COLOR_PICKER_PADDING;
		float fieldWidth = width - COLOR_PICKER_PADDING * 2.0F;
		float previewY = panelY + 39.0F;
		float searchY = panelY + 70.0F;
		float gridY = panelY + 98.0F;
		float cellWidth = customSwordModelCellWidth(fieldWidth);
		float gridHeight = CUSTOM_SWORD_MODEL_VISIBLE_ROWS * CUSTOM_SWORD_MODEL_CELL_HEIGHT + (CUSTOM_SWORD_MODEL_VISIBLE_ROWS - 1) * CUSTOM_SWORD_MODEL_CELL_GAP;
		List<String> filteredModels = filteredCustomSwordModels();

		int scissorLeft = Math.round(left + PANEL_WIDTH - 0.5F);
		guiGraphics.enableScissor(
				scissorLeft,
				Math.round(panelY - r - 4.0F),
				Math.round(this.width),
				Math.round(panelY + height + r + 4.0F)
		);

		try {
			Render2D.softShadow(guiGraphics, panelX, panelY, width, height, cornerRadius, 6.0F, alpha(0x80000000, alpha));
			Render2D.attachedTabRightBody(guiGraphics, panelX, panelY, width, height, cornerRadius, r, bgColor);
			drawBoldInterText(guiGraphics, "CUSTOM SWORDS", fieldX, panelY + 13.0F, 7.8F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			drawBoldInterText(guiGraphics, customSwordTarget.display() + " • " + customSwordSetting.model(customSwordTarget), fieldX, panelY + 27.0F, 6.2F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawRightAlignedBoldInterText(guiGraphics, "x", panelX + width - COLOR_PICKER_PADDING, panelY + 13.0F, 7.5F, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));

			Render2D.roundedRect(guiGraphics, fieldX, previewY, fieldWidth, 24.0F, 5.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, fieldX + 1.0F, previewY + 1.0F, fieldWidth - 2.0F, 22.0F, 4.0F, alpha(COLOR_PICKER_INNER, alpha));
			renderCustomSwordPreviewItem(guiGraphics, CustomSwords.getInstance().previewStack(customSwordTarget), Math.round(fieldX + 5.0F), Math.round(previewY + 4.0F));
			drawBoldInterText(guiGraphics, customSwordTarget.display(), fieldX + 27.0F, previewY + 9.0F, 7.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			drawBoldInterText(guiGraphics, "pick visual model • enable with checkmark below tab", fieldX + 27.0F, previewY + 18.0F, 5.8F, alpha(MODULE_DESCRIPTION_COLOR, alpha));

			renderCustomSwordSearch(guiGraphics, fieldX, searchY, fieldWidth, alpha);

			Render2D.roundedRect(guiGraphics, fieldX, gridY, fieldWidth, gridHeight, 5.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, fieldX + 1.0F, gridY + 1.0F, fieldWidth - 2.0F, gridHeight - 2.0F, 4.0F, alpha(COLOR_PICKER_INNER, alpha));
			guiGraphics.enableScissor(Math.round(fieldX + 1.0F), Math.round(gridY + 1.0F), Math.round(fieldX + fieldWidth - 1.0F), Math.round(gridY + gridHeight - 1.0F));
			customSwordPanelScroll = Math.max(0, Math.min(customSwordPanelScroll, maxCustomSwordPanelScroll(filteredModels)));
			for (int row = 0; row < CUSTOM_SWORD_MODEL_VISIBLE_ROWS; row++) {
				for (int column = 0; column < CUSTOM_SWORD_MODEL_COLUMNS; column++) {
					int modelIndex = (customSwordPanelScroll + row) * CUSTOM_SWORD_MODEL_COLUMNS + column;
					if (modelIndex >= filteredModels.size()) {
						break;
					}

					String model = filteredModels.get(modelIndex);
					float cellX = fieldX + column * (cellWidth + CUSTOM_SWORD_MODEL_CELL_GAP);
					float cellY = gridY + row * (CUSTOM_SWORD_MODEL_CELL_HEIGHT + CUSTOM_SWORD_MODEL_CELL_GAP);
					renderCustomSwordModelCell(guiGraphics, model, cellX, cellY, cellWidth, CUSTOM_SWORD_MODEL_CELL_HEIGHT, alpha);
				}
			}
			guiGraphics.disableScissor();

			if (filteredModels.isEmpty()) {
				drawBoldInterText(guiGraphics, "No models found", fieldX + 8.0F, gridY + 20.0F, 7.2F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			}

			String counter = filteredModels.size() + " models";
			drawRightAlignedBoldInterText(guiGraphics, counter, fieldX + fieldWidth, panelY + CUSTOM_SWORD_PANEL_HEIGHT - 12.0F, 5.8F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawBoldInterText(guiGraphics, "scroll + search by name", fieldX, panelY + CUSTOM_SWORD_PANEL_HEIGHT - 12.0F, 5.8F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		} finally {
			guiGraphics.disableScissor();
		}
	}

	private void renderCustomSwordSearch(GuiGraphics guiGraphics, float x, float y, float width, float alpha) {
		int outline = customSwordSearchFocused ? ThemeManager.darkAccent() : MODULE_CONTROL_OUTLINE;
		Render2D.roundedRect(guiGraphics, x, y, width, CUSTOM_SWORD_SEARCH_HEIGHT, 5.0F, alpha(outline, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, width - 2.0F, CUSTOM_SWORD_SEARCH_HEIGHT - 2.0F, 4.0F, alpha(COLOR_PICKER_INNER, alpha));
		String text = customSwordSearch.isBlank() ? "Search model..." : customSwordSearch;
		drawBoldInterText(
				guiGraphics,
				trimToWidth(text, 6.8F, width - 18.0F),
				x + 8.0F,
				y + CUSTOM_SWORD_SEARCH_HEIGHT / 2.0F,
				6.8F,
				alpha(customSwordSearch.isBlank() ? MODULE_DESCRIPTION_COLOR : MODULE_CONTROL_TEXT_COLOR, alpha)
		);
		if (customSwordSearchFocused && searchCaretVisible()) {
			float caretX = x + 8.0F + LOGO_FONT.width(trimToWidth(customSwordSearch, 6.8F, width - 18.0F), 6.8F) + 1.0F;
			Render2D.roundedRect(guiGraphics, caretX, y + 5.0F, 1.0F, 10.0F, 0.5F, alpha(0xFFFFFFFF, alpha));
		}
	}

	private void renderCustomSwordModelCell(GuiGraphics guiGraphics, String model, float x, float y, float width, float height, float alpha) {
		boolean selected = model.equals(customSwordSetting.model(customSwordTarget));
		int outline = selected ? ThemeManager.darkAccent() : MODULE_CONTROL_OUTLINE;
		int background = selected ? MODULE_ENUM_OPTION_HIGHLIGHT : 0xFF151515;
		Render2D.roundedRect(guiGraphics, x, y, width, height, 5.0F, alpha(outline, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, 4.0F, alpha(background, alpha));
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x + width / 2.0F, y + 5.0F, 0.0F);
		guiGraphics.pose().scale(1.32F, 1.32F, 1.0F);
		guiGraphics.pose().translate(-8.0F, 0.0F, 0.0F);
		renderCustomSwordPreviewItem(guiGraphics, CustomSwords.getInstance().previewStack(customSwordTarget, model), 0, 0);
		guiGraphics.pose().popPose();
		if (selected) {
			Render2D.roundedRect(guiGraphics, x + width / 2.0F - 8.0F, y + height - 7.0F, 16.0F, 2.0F, 1.0F, alpha(ThemeManager.accent(), alpha));
		}
	}

	private List<String> filteredCustomSwordModels() {
		String query = customSwordSearch.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return CustomSwords.modelList();
		}

		return CustomSwords.modelList().stream()
				.filter(model -> model.toLowerCase(Locale.ROOT).contains(query))
				.toList();
	}

	private static float customSwordModelCellWidth(float gridWidth) {
		return (gridWidth - (CUSTOM_SWORD_MODEL_COLUMNS - 1) * CUSTOM_SWORD_MODEL_CELL_GAP) / CUSTOM_SWORD_MODEL_COLUMNS;
	}

	private static int maxCustomSwordPanelScroll(List<String> models) {
		int rows = (int) Math.ceil(models.size() / (float) CUSTOM_SWORD_MODEL_COLUMNS);
		return Math.max(0, rows - CUSTOM_SWORD_MODEL_VISIBLE_ROWS);
	}

	private static void renderCustomSwordPreviewItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
		CustomSwords.beginPickerPreviewRender();
		try {
			guiGraphics.renderItem(stack, x, y);
		} finally {
			CustomSwords.endPickerPreviewRender();
		}
	}

	private void renderPageTransition(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY,
			float alpha, float frameDelta) {
		PageState currentPage = currentPage();
		if (previousPage == null || pageTransitionProgress >= 1.0F) {
			renderPage(guiGraphics, currentPage, left, top, mouseX, mouseY, alpha, frameDelta, 0.0F);
			return;
		}

		float progress = ease(pageTransitionProgress);
		float outgoingOffset = -pageTransitionDirection * PAGE_TRANSITION_DISTANCE * progress;
		float incomingOffset = pageTransitionDirection * PAGE_TRANSITION_DISTANCE * (1.0F - progress);
		renderPage(guiGraphics, previousPage, left, top, Integer.MIN_VALUE, Integer.MIN_VALUE,
				alpha * (1.0F - progress), frameDelta, outgoingOffset);
		renderPage(guiGraphics, currentPage, left, top,
				Math.round(mouseX - incomingOffset), mouseY, alpha * progress, frameDelta, incomingOffset);
	}

	private void renderPage(GuiGraphics guiGraphics, PageState page, int left, int top, int mouseX, int mouseY,
			float alpha, float frameDelta, float offsetX) {
		IconGroup currentGroup = selectedIconGroup;
		int currentCategory = selectedIconIndex;
		int currentUtility = selectedUtilityIconIndex;
		selectedIconGroup = page.group();
		selectedIconIndex = page.categoryIndex();
		selectedUtilityIconIndex = page.utilityIndex();

		guiGraphics.enableScissor(left + SIDEBAR_WIDTH, top + TOPBAR_HEIGHT, left + PANEL_WIDTH, top + PANEL_HEIGHT);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(offsetX, 0.0F, 0.0F);
		try {
			renderModules(guiGraphics, left, top, mouseX, mouseY, alpha, frameDelta);
			renderFriends(guiGraphics, left, top, alpha);
			renderConfigs(guiGraphics, left, top, mouseX, mouseY, alpha);
			renderSettings(guiGraphics, left, top, mouseX, mouseY, alpha);
		} finally {
			guiGraphics.pose().popPose();
			guiGraphics.disableScissor();
			selectedIconGroup = currentGroup;
			selectedIconIndex = currentCategory;
			selectedUtilityIconIndex = currentUtility;
		}
	}

	private void renderFriends(GuiGraphics guiGraphics, int left, int top, float alpha) {
		if (!friendsPageSelected()) {
			return;
		}

		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float titleY = top + TOPBAR_HEIGHT + 22.0F;
		drawBoldInterText(guiGraphics, "FRIENDS", contentX, titleY, 11.0F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		List<FriendManager.FriendEntry> friends = FriendManager.friends();
		drawRightAlignedInterText(guiGraphics, friends.size() + (friends.size() == 1 ? " friend" : " friends"), contentX + contentWidth, titleY, 7.0F, alpha(MODULE_DESCRIPTION_COLOR, alpha));

		float inputY = top + TOPBAR_HEIGHT + 42.0F;
		float inputGap = 8.0F;
		float buttonWidth = 76.0F;
		float inputWidth = (contentWidth - buttonWidth - inputGap * 2.0F) / 2.0F;
		renderFriendInput(guiGraphics, friendNameInput, "Player name", contentX, inputY, inputWidth, friendInputFocus == FriendInputFocus.NAME, editingFriendName != null, alpha);
		renderFriendInput(guiGraphics, friendAliasInput, "Fake nick (optional)", contentX + inputWidth + inputGap, inputY, inputWidth, friendInputFocus == FriendInputFocus.ALIAS, false, alpha);

		float buttonX = contentX + inputWidth * 2.0F + inputGap * 2.0F;
		boolean canSubmit = !friendNameInput.trim().isEmpty();
		Render2D.roundedRect(guiGraphics, buttonX, inputY, buttonWidth, FRIEND_INPUT_HEIGHT, 4.0F, alpha(canSubmit ? ThemeManager.accent() : MODULE_CONTROL_OUTLINE, alpha));
		String action = editingFriendName == null ? "ADD" : "SAVE";
		float actionSize = 7.0F;
		float actionX = LOGO_FONT.centeredX(action, actionSize, buttonX + buttonWidth / 2.0F);
		drawBoldInterText(guiGraphics, action, actionX, inputY + FRIEND_INPUT_HEIGHT / 2.0F, actionSize, alpha(canSubmit ? ThemeManager.textOnAccent() : MODULE_DESCRIPTION_COLOR, alpha));

		float listY = inputY + FRIEND_INPUT_HEIGHT + 18.0F;
		if (friends.isEmpty()) {
			drawBoldInterText(guiGraphics, "No friends added yet", contentX, listY + 10.0F, 8.0F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawInterText(guiGraphics, "Add a player above. Fake nick can be left empty.", contentX, listY + 25.0F, 7.0F, alpha(0xFF4E4E4E, alpha));
			return;
		}

		friendScroll = Math.max(0, Math.min(friendScroll, Math.max(0, friends.size() - 6)));
		int maxRows = Math.min(friends.size() - friendScroll, 6);
		for (int row = 0; row < maxRows; row++) {
			FriendManager.FriendEntry friend = friends.get(friendScroll + row);
			float rowY = listY + row * (FRIEND_ROW_HEIGHT + FRIEND_ROW_GAP);
			boolean editing = editingFriendName != null && editingFriendName.equalsIgnoreCase(friend.name());
			Render2D.roundedRect(guiGraphics, contentX, rowY, contentWidth, FRIEND_ROW_HEIGHT, 5.0F, alpha(editing ? ThemeManager.darkAccent() : MODULE_CARD_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, contentX + 1.0F, rowY + 1.0F, contentWidth - 2.0F, FRIEND_ROW_HEIGHT - 2.0F, 4.0F, alpha(MODULE_CARD_BACKGROUND, alpha));
			Render2D.roundedRect(guiGraphics, contentX + 10.0F, rowY + 12.0F, 10.0F, 10.0F, 5.0F, alpha(ThemeManager.accent(), alpha));
			drawBoldInterText(guiGraphics, friend.name(), contentX + 28.0F, rowY + 12.0F, 8.0F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			String detail = friend.alias().isBlank() ? "No fake nick" : "Shown as " + friend.alias();
			drawInterText(guiGraphics, detail, contentX + 28.0F, rowY + 24.0F, 6.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawBoldInterText(guiGraphics, "x", contentX + contentWidth - 19.0F, rowY + FRIEND_ROW_HEIGHT / 2.0F, 8.0F, alpha(0xFFFF6B78, alpha));
		}
	}
	private boolean configsPageSelected() {
		return selectedIconGroup == IconGroup.UTILITY && selectedUtilityIconIndex == CONFIGS_UTILITY_INDEX;
	}

	private void renderConfigs(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY, float alpha) {
		if (!configsPageSelected()) {
			return;
		}

		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float titleY = top + TOPBAR_HEIGHT + 22.0F;
		drawBoldInterText(guiGraphics, "CONFIGS", contentX, titleY, 11.0F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		java.util.List<String> configs = dev.Mardexx.client.config.ClientConfig.listConfigs();
		drawRightAlignedInterText(guiGraphics, configs.size() + (configs.size() == 1 ? " config" : " configs"), contentX + contentWidth, titleY, 7.0F, alpha(MODULE_DESCRIPTION_COLOR, alpha));

		float inputY = top + TOPBAR_HEIGHT + 42.0F;
		float buttonWidth = 76.0F;
		float inputGap = 8.0F;
		float inputWidth = contentWidth - buttonWidth - inputGap;
		renderFriendInput(guiGraphics, configNameInput, "New config name...", contentX, inputY, inputWidth, configInputFocused, false, alpha);

		float buttonX = contentX + inputWidth + inputGap;
		boolean canSubmit = !configNameInput.trim().isEmpty();
		Render2D.roundedRect(guiGraphics, buttonX, inputY, buttonWidth, FRIEND_INPUT_HEIGHT, 4.0F, alpha(canSubmit ? ThemeManager.accent() : MODULE_CONTROL_OUTLINE, alpha));
		float actionSize = 7.0F;
		String actionText = "CREATE";
		float actionX = LOGO_FONT.centeredX(actionText, actionSize, buttonX + buttonWidth / 2.0F);
		drawBoldInterText(guiGraphics, actionText, actionX, inputY + FRIEND_INPUT_HEIGHT / 2.0F, actionSize, alpha(canSubmit ? ThemeManager.textOnAccent() : MODULE_DESCRIPTION_COLOR, alpha));

		float listY = inputY + FRIEND_INPUT_HEIGHT + 18.0F;
		if (configs.isEmpty()) {
			drawBoldInterText(guiGraphics, "No configs found", contentX, listY + 10.0F, 8.0F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawInterText(guiGraphics, "Create a new config profile above.", contentX, listY + 25.0F, 7.0F, alpha(0xFF4E4E4E, alpha));
			return;
		}

		configScroll = Math.max(0, Math.min(configScroll, Math.max(0, configs.size() - 6)));
		int maxRows = Math.min(configs.size() - configScroll, 6);
		String activeCfg = dev.Mardexx.client.config.ClientConfig.getActiveConfig();
		for (int row = 0; row < maxRows; row++) {
			String cfg = configs.get(configScroll + row);
			float rowY = listY + row * (FRIEND_ROW_HEIGHT + FRIEND_ROW_GAP);
			boolean isActive = cfg.equalsIgnoreCase(activeCfg);
			boolean isRowHovered = isInside(mouseX, mouseY, contentX, rowY, contentWidth, FRIEND_ROW_HEIGHT);

			Render2D.roundedRect(guiGraphics, contentX, rowY, contentWidth, FRIEND_ROW_HEIGHT, 5.0F, alpha(isActive ? ThemeManager.darkAccent() : (isRowHovered ? 0xFF202026 : MODULE_CARD_OUTLINE), alpha));
			Render2D.roundedRect(guiGraphics, contentX + 1.0F, rowY + 1.0F, contentWidth - 2.0F, FRIEND_ROW_HEIGHT - 2.0F, 4.0F, alpha(MODULE_CARD_BACKGROUND, alpha));

			// Active indicator dot
			Render2D.roundedRect(guiGraphics, contentX + 10.0F, rowY + 12.0F, 10.0F, 10.0F, 5.0F, alpha(isActive ? ThemeManager.accent() : 0xFF35353C, alpha));
			drawBoldInterText(guiGraphics, cfg, contentX + 28.0F, rowY + 12.0F, 8.0F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			String detail = isActive ? "Active profile" : "Saved profile";
			drawInterText(guiGraphics, detail, contentX + 28.0F, rowY + 24.0F, 6.6F, alpha(isActive ? ThemeManager.accent() : MODULE_DESCRIPTION_COLOR, alpha));

			// Right side buttons
			float loadBtnW = 38.0F;
			float saveBtnW = 38.0F;
			float deleteBtnW = 18.0F;
			float btnH = 16.0F;
			float btnY = rowY + (FRIEND_ROW_HEIGHT - btnH) / 2.0F;

			float delBtnX = contentX + contentWidth - deleteBtnW - 8.0F;
			float saveBtnX = delBtnX - saveBtnW - 6.0F;
			float loadBtnX = saveBtnX - loadBtnW - 6.0F;

			// LOAD button
			boolean loadHov = isInside(mouseX, mouseY, loadBtnX, btnY, loadBtnW, btnH);
			Render2D.roundedRect(guiGraphics, loadBtnX, btnY, loadBtnW, btnH, 3.0F, alpha(loadHov ? ThemeManager.accent() : MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, loadBtnX + 1.0F, btnY + 1.0F, loadBtnW - 2.0F, btnH - 2.0F, 2.5F, alpha(loadHov ? ThemeManager.darkAccent() : MODULE_CONTROL_BACKGROUND, alpha));
			float loadTxtX = LOGO_FONT.centeredX("LOAD", 6.2F, loadBtnX + loadBtnW / 2.0F);
			drawBoldInterText(guiGraphics, "LOAD", loadTxtX, btnY + btnH / 2.0F, 6.2F, alpha(loadHov ? 0xFFFFFFFF : MODULE_CONTROL_TEXT_COLOR, alpha));

			// SAVE button
			boolean saveHov = isInside(mouseX, mouseY, saveBtnX, btnY, saveBtnW, btnH);
			Render2D.roundedRect(guiGraphics, saveBtnX, btnY, saveBtnW, btnH, 3.0F, alpha(saveHov ? ThemeManager.accent() : MODULE_CONTROL_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, saveBtnX + 1.0F, btnY + 1.0F, saveBtnW - 2.0F, btnH - 2.0F, 2.5F, alpha(saveHov ? ThemeManager.darkAccent() : MODULE_CONTROL_BACKGROUND, alpha));
			float saveTxtX = LOGO_FONT.centeredX("SAVE", 6.2F, saveBtnX + saveBtnW / 2.0F);
			drawBoldInterText(guiGraphics, "SAVE", saveTxtX, btnY + btnH / 2.0F, 6.2F, alpha(saveHov ? 0xFFFFFFFF : MODULE_CONTROL_TEXT_COLOR, alpha));

			// DELETE button
			if (!cfg.equalsIgnoreCase("default")) {
				boolean delHov = isInside(mouseX, mouseY, delBtnX, btnY, deleteBtnW, btnH);
				drawBoldInterText(guiGraphics, "x", delBtnX + 5.0F, btnY + btnH / 2.0F, 8.0F, alpha(delHov ? 0xFFFF4555 : 0xFFFF7B88, alpha));
			}
		}
	}

	private boolean handleConfigClick(double mouseX, double mouseY) {
		if (!configsPageSelected()) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float inputY = top + TOPBAR_HEIGHT + 42.0F;
		float buttonWidth = 76.0F;
		float inputGap = 8.0F;
		float inputWidth = contentWidth - buttonWidth - inputGap;

		if (isInside(mouseX, mouseY, contentX, inputY, inputWidth, FRIEND_INPUT_HEIGHT)) {
			configInputFocused = true;
			return true;
		}
		float buttonX = contentX + inputWidth + inputGap;
		if (isInside(mouseX, mouseY, buttonX, inputY, buttonWidth, FRIEND_INPUT_HEIGHT)) {
			submitConfig();
			return true;
		}

		float listY = inputY + FRIEND_INPUT_HEIGHT + 18.0F;
		java.util.List<String> configs = dev.Mardexx.client.config.ClientConfig.listConfigs();
		configScroll = Math.max(0, Math.min(configScroll, Math.max(0, configs.size() - 6)));
		int maxRows = Math.min(configs.size() - configScroll, 6);
		for (int row = 0; row < maxRows; row++) {
			String cfg = configs.get(configScroll + row);
			float rowY = listY + row * (FRIEND_ROW_HEIGHT + FRIEND_ROW_GAP);
			if (!isInside(mouseX, mouseY, contentX, rowY, contentWidth, FRIEND_ROW_HEIGHT)) {
				continue;
			}

			float loadBtnW = 38.0F;
			float saveBtnW = 38.0F;
			float deleteBtnW = 18.0F;
			float btnH = 16.0F;
			float btnY = rowY + (FRIEND_ROW_HEIGHT - btnH) / 2.0F;

			float delBtnX = contentX + contentWidth - deleteBtnW - 8.0F;
			float saveBtnX = delBtnX - saveBtnW - 6.0F;
			float loadBtnX = saveBtnX - loadBtnW - 6.0F;

			if (isInside(mouseX, mouseY, loadBtnX, btnY, loadBtnW, btnH)) {
				dev.Mardexx.client.config.ClientConfig.loadNamed(cfg);
				return true;
			}
			if (isInside(mouseX, mouseY, saveBtnX, btnY, saveBtnW, btnH)) {
				dev.Mardexx.client.config.ClientConfig.saveNamed(cfg);
				return true;
			}
			if (!cfg.equalsIgnoreCase("default") && isInside(mouseX, mouseY, delBtnX, btnY, deleteBtnW, btnH)) {
				dev.Mardexx.client.config.ClientConfig.deleteNamed(cfg);
				return true;
			}
			return true;
		}

		configInputFocused = false;
		return true;
	}

	private void submitConfig() {
		String name = configNameInput.trim();
		if (!name.isEmpty()) {
			dev.Mardexx.client.config.ClientConfig.saveNamed(name);
			configNameInput = "";
			configInputFocused = false;
		}
	}

	private boolean settingsPageSelected() {
		return selectedIconGroup == IconGroup.UTILITY && selectedUtilityIconIndex == SETTINGS_UTILITY_INDEX;
	}

	private void renderSettings(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY, float alpha) {
		if (!settingsPageSelected()) {
			return;
		}

		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float titleY = top + TOPBAR_HEIGHT + 22.0F;
		drawBoldInterText(guiGraphics, "SETTINGS", contentX, titleY, 11.0F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		drawRightAlignedInterText(guiGraphics, "Client Preferences", contentX + contentWidth, titleY, 7.0F, alpha(MODULE_DESCRIPTION_COLOR, alpha));

		float startY = top + TOPBAR_HEIGHT + 44.0F;
		float cardHeight = 36.0F;
		float cardGap = 8.0F;
		float sliderW = 86.0F;
		float sliderX = contentX + contentWidth - sliderW - 46.0F;

		// 1. GUI Scale
		float y1 = startY;
		renderSettingCard(guiGraphics, contentX, y1, contentWidth, cardHeight, alpha);
		drawBoldInterText(guiGraphics, "GUI Scale", contentX + 12.0F, y1 + 12.0F, 8.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		drawInterText(guiGraphics, "Scale multiplier for the client interface.", contentX + 12.0F, y1 + 24.0F, 6.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		float guiScaleProg = (getGuiScale() - 0.70F) / (1.30F - 0.70F);
		renderSettingsSlider(guiGraphics, sliderX, y1 + cardHeight / 2.0F, sliderW, guiScaleProg, String.format(Locale.ROOT, "%.2fx", getGuiScale()), alpha);

		// 2. HUD Scale
		float y2 = y1 + cardHeight + cardGap;
		renderSettingCard(guiGraphics, contentX, y2, contentWidth, cardHeight, alpha);
		drawBoldInterText(guiGraphics, "HUD Scale", contentX + 12.0F, y2 + 12.0F, 8.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		drawInterText(guiGraphics, "Global scale factor for all on-screen HUD widgets.", contentX + 12.0F, y2 + 24.0F, 6.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		float hudScale = HudManager.scale();
		float hudScaleProg = (hudScale - 0.50F) / (1.50F - 0.50F);
		renderSettingsSlider(guiGraphics, sliderX, y2 + cardHeight / 2.0F, sliderW, hudScaleProg, String.format(Locale.ROOT, "%.2fx", hudScale), alpha);

		// 3. HUD Transparency
		float y3 = y2 + cardHeight + cardGap;
		renderSettingCard(guiGraphics, contentX, y3, contentWidth, cardHeight, alpha);
		drawBoldInterText(guiGraphics, "HUD Background Transparency", contentX + 12.0F, y3 + 12.0F, 8.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		drawInterText(guiGraphics, "Opacity of HUD backplates (keeps text & icons solid).", contentX + 12.0F, y3 + 24.0F, 6.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		float hudTrans = HudManager.transparency();
		renderSettingsSlider(guiGraphics, sliderX, y3 + cardHeight / 2.0F, sliderW, hudTrans, Math.round(hudTrans * 100.0F) + "%", alpha);

		// 4. GUI Animations
		float y4 = y3 + cardHeight + cardGap;
		renderSettingCard(guiGraphics, contentX, y4, contentWidth, cardHeight, alpha);
		drawBoldInterText(guiGraphics, "GUI Animations", contentX + 12.0F, y4 + 12.0F, 8.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
		drawInterText(guiGraphics, "Smooth transitions, hover effects and open/close easing.", contentX + 12.0F, y4 + 24.0F, 6.6F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		float switchX = contentX + contentWidth - MODULE_TOGGLE_SWITCH_WIDTH - 14.0F;
		renderModuleToggleSwitch(guiGraphics, switchX + MODULE_TOGGLE_SWITCH_WIDTH, y4 + cardHeight / 2.0F, isGuiAnimations() ? 1.0F : 0.0F, alpha);
	}

	private static void renderSettingCard(GuiGraphics guiGraphics, float x, float y, float width, float height, float alpha) {
		Render2D.roundedRect(guiGraphics, x, y, width, height, 5.0F, alpha(MODULE_CARD_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, 4.0F, alpha(MODULE_CARD_BACKGROUND, alpha));
	}

	private static void renderSettingsSlider(GuiGraphics guiGraphics, float x, float centerY, float width, float progress, String valueText, float alpha) {
		float trackH = 4.0F;
		float trackY = centerY - trackH / 2.0F;
		Render2D.roundedRect(guiGraphics, x, trackY, width, trackH, 2.0F, alpha(MODULE_SLIDER_TRACK, alpha));
		float fillW = Math.max(0.0F, Math.min(width, width * progress));
		if (fillW > 1.0F) {
			Render2D.roundedRect(guiGraphics, x, trackY, fillW, trackH, 2.0F, alpha(ThemeManager.accent(), alpha));
		}
		float knobX = x + fillW;
		Render2D.roundedRect(guiGraphics, knobX - 4.0F, centerY - 5.0F, 8.0F, 10.0F, 3.0F, alpha(0xFFFFFFFF, alpha));
		drawBoldInterText(guiGraphics, valueText, x + width + 8.0F, centerY, 7.0F, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));
	}

	private boolean handleSettingsClick(double mouseX, double mouseY) {
		if (!settingsPageSelected()) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float startY = top + TOPBAR_HEIGHT + 44.0F;
		float cardHeight = 36.0F;
		float cardGap = 8.0F;

		float sliderW = 86.0F;
		float sliderX = contentX + contentWidth - sliderW - 46.0F;

		// 1. GUI Scale
		float y1 = startY;
		if (isInside(mouseX, mouseY, sliderX - 6.0F, y1, sliderW + 12.0F, cardHeight)) {
			draggedSettingsControl = SettingsDrag.GUI_SCALE;
			updateSettingsDrag(mouseX);
			return true;
		}

		// 2. HUD Scale
		float y2 = y1 + cardHeight + cardGap;
		if (isInside(mouseX, mouseY, sliderX - 6.0F, y2, sliderW + 12.0F, cardHeight)) {
			draggedSettingsControl = SettingsDrag.HUD_SCALE;
			updateSettingsDrag(mouseX);
			return true;
		}

		// 3. HUD Transparency
		float y3 = y2 + cardHeight + cardGap;
		if (isInside(mouseX, mouseY, sliderX - 6.0F, y3, sliderW + 12.0F, cardHeight)) {
			draggedSettingsControl = SettingsDrag.HUD_TRANSPARENCY;
			updateSettingsDrag(mouseX);
			return true;
		}

		// 4. GUI Animations
		float y4 = y3 + cardHeight + cardGap;
		if (isInside(mouseX, mouseY, contentX, y4, contentWidth, cardHeight)) {
			toggleGuiAnimations();
			return true;
		}

		return false;
	}

	private boolean updateSettingsDrag(double mouseX) {
		if (draggedSettingsControl == SettingsDrag.NONE) {
			return false;
		}
		int left = (this.width - PANEL_WIDTH) / 2;
		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float sliderW = 86.0F;
		float sliderX = contentX + contentWidth - sliderW - 46.0F;
		float progress = (float) Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / sliderW));

		if (draggedSettingsControl == SettingsDrag.GUI_SCALE) {
			float newScale = 0.70F + progress * (1.30F - 0.70F);
			newScale = Math.round(newScale * 20.0F) / 20.0F;
			setGuiScale(newScale);
			dev.Mardexx.client.config.ClientConfig.save();
			return true;
		} else if (draggedSettingsControl == SettingsDrag.HUD_SCALE) {
			float newScale = 0.50F + progress * (1.50F - 0.50F);
			newScale = Math.round(newScale * 20.0F) / 20.0F;
			HudManager.setScale(newScale);
			dev.Mardexx.client.config.ClientConfig.save();
			return true;
		} else if (draggedSettingsControl == SettingsDrag.HUD_TRANSPARENCY) {
			float newTrans = Math.round(progress * 20.0F) / 20.0F;
			HudManager.setTransparency(newTrans);
			dev.Mardexx.client.config.ClientConfig.save();
			return true;
		}
		return false;
	}

	private void updateModuleHoverAnimations(float delta, double mouseX, double mouseY) {
		if (selectedIconGroup != IconGroup.CATEGORY) {
			return;
		}
		for (Module module : ModuleManager.modules()) {
			boolean isHovered = false;
			ModuleLayout layout = moduleLayout(module);
			if (layout != null && isInside(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height())) {
				isHovered = true;
			}
			float current = moduleHoverAnimations.getOrDefault(module, 0.0F);
			float target = isHovered ? 1.0F : 0.0F;
			float speed = isGuiAnimations() ? 0.28F : 1.0F;
			float updated = current + (target - current) * Math.min(1.0F, speed * delta);
			if (updated > 0.001F) {
				moduleHoverAnimations.put(module, updated);
			} else {
				moduleHoverAnimations.remove(module);
			}
		}
	}


	private static void renderFriendInput(GuiGraphics guiGraphics, String value, String placeholder, float x, float y, float width, boolean focused, boolean locked, float alpha) {
		int outline = focused ? ThemeManager.darkAccent() : MODULE_CONTROL_OUTLINE;
		Render2D.roundedRect(guiGraphics, x, y, width, FRIEND_INPUT_HEIGHT, 4.0F, alpha(outline, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, y + 1.0F, width - 2.0F, FRIEND_INPUT_HEIGHT - 2.0F, 3.0F, alpha(locked ? MODULE_SETTINGS_BACKGROUND : MODULE_CONTROL_BACKGROUND, alpha));
		String displayed = value.isEmpty() ? placeholder : value;
		int color = value.isEmpty() || locked ? MODULE_DESCRIPTION_COLOR : MODULE_CONTROL_TEXT_COLOR;
		drawBoldInterText(guiGraphics, trimToWidth(displayed, 7.2F, width - 18.0F), x + 8.0F, y + FRIEND_INPUT_HEIGHT / 2.0F, 7.2F, alpha(color, alpha));
		if (focused && searchCaretVisible()) {
			float caretX = x + 8.0F + LOGO_FONT.width(trimToWidth(value, 7.2F, width - 18.0F), 7.2F) + 1.0F;
			Render2D.roundedRect(guiGraphics, caretX, y + 7.0F, 1.0F, 10.0F, 0.5F, alpha(0xFFFFFFFF, alpha));
		}
	}

	private static void renderColorControl(GuiGraphics guiGraphics, ColorSetting setting, float x, float centerY, float width, float alpha, boolean moduleEnabled) {
		Render2D.roundedRect(guiGraphics, x, centerY - 9.0F, width, 18.0F, 4.0F, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, centerY - 8.0F, width - 2.0F, 16.0F, 3.0F, alpha(MODULE_CONTROL_BACKGROUND, alpha));
		Render2D.roundedRect(guiGraphics, x + 5.0F, centerY - 5.0F, 20.0F, 10.0F, 2.5F, alpha(setting.color(), alpha));
		Render2D.roundedRect(guiGraphics, x + 4.0F, centerY - 6.0F, 22.0F, 12.0F, 3.0F, alpha(MODULE_CONTROL_OUTLINE, alpha * 0.55F));
		Render2D.roundedRect(guiGraphics, x + 5.0F, centerY - 5.0F, 20.0F, 10.0F, 2.5F, alpha(setting.color(), alpha));
		int textColor = moduleEnabled ? MODULE_CONTROL_TEXT_COLOR : MODULE_INACTIVE_CHECKBOX_MARK;
		drawRightAlignedBoldInterText(guiGraphics, setting.hex(), x + width - 6.0F, centerY, MODULE_SETTING_SIZE, alpha(textColor, alpha));
	}

	private void renderColorPicker(GuiGraphics guiGraphics, int left, int top, float guiAlpha) {
		if (colorPickerSetting == null || colorPickerProgress <= 0.01F) {
			return;
		}

		float progress = ease(colorPickerProgress);
		float alpha = guiAlpha * progress;
		float panelX = colorPickerX(left);
		float panelY = colorPickerY(top);
		float width = COLOR_PICKER_WIDTH;
		float height = COLOR_PICKER_HEIGHT;
		float r = COLOR_PICKER_FILLET_RADIUS;
		float cornerRadius = COLOR_PICKER_CORNER_RADIUS;
		int bgColor = alpha(PANEL_BACKGROUND, alpha);

		float fieldX = panelX + COLOR_PICKER_PADDING;
		float fieldY = panelY + 40.0F;
		float fieldWidth = width - COLOR_PICKER_PADDING * 2.0F;
		float hueY = fieldY + COLOR_FIELD_HEIGHT + 10.0F;
		float previewY = hueY + COLOR_HUE_HEIGHT + 12.0F;

		int scissorLeft = Math.round(left + PANEL_WIDTH - 0.5F);
		guiGraphics.enableScissor(
				scissorLeft,
				Math.round(panelY - r - 4.0F),
				Math.round(this.width),
				Math.round(panelY + height + r + 4.0F)
		);

		try {
			Render2D.softShadow(guiGraphics, panelX, panelY, width, height, cornerRadius, 6.0F, alpha(0x80000000, alpha));
			Render2D.attachedTabRightBody(guiGraphics, panelX, panelY, width, height, cornerRadius, r, bgColor);
			boolean themePicker = colorPickerSetting == ThemeManager.accentSetting();
			drawBoldInterText(guiGraphics, themePicker ? "THEMES" : "COLOR", panelX + COLOR_PICKER_PADDING, panelY + 13.0F, 7.8F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			drawBoldInterText(guiGraphics, themePicker ? "Theme color" : colorPickerSetting.displayName(), panelX + COLOR_PICKER_PADDING, panelY + 27.0F, 6.8F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
			drawRightAlignedBoldInterText(guiGraphics, "x", panelX + width - COLOR_PICKER_PADDING, panelY + 13.0F, 7.5F, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));

			Hsv hsv = colorPickerHsv();
			renderSaturationValueField(guiGraphics, fieldX, fieldY, fieldWidth, COLOR_FIELD_HEIGHT, hsv.hue(), alpha);
			renderHueBar(guiGraphics, fieldX, hueY, fieldWidth, COLOR_HUE_HEIGHT, alpha);
			guiGraphics.flush();

			float markerPadding = 5.0F;
			float markerX = fieldX + markerPadding + hsv.saturation() * (fieldWidth - markerPadding * 2.0F);
			float markerY = fieldY + markerPadding + (1.0F - hsv.value()) * (COLOR_FIELD_HEIGHT - markerPadding * 2.0F);
			renderColorMarker(guiGraphics, markerX, markerY, colorPickerSetting.color(), alpha);
			float hueMarkerX = fieldX + 2.0F + hsv.hue() * (fieldWidth - 4.0F);
			Render2D.roundedRect(guiGraphics, hueMarkerX - 2.0F, hueY - 2.0F, 4.0F, COLOR_HUE_HEIGHT + 4.0F, 2.0F, alpha(COLOR_PICKER_MARKER_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, hueMarkerX - 1.0F, hueY - 1.0F, 2.0F, COLOR_HUE_HEIGHT + 2.0F, 1.0F, alpha(COLOR_PICKER_MARKER, alpha));

			Render2D.roundedRect(guiGraphics, fieldX, previewY, 30.0F, 19.0F, 4.0F, alpha(COLOR_PICKER_OUTLINE, alpha));
			Render2D.roundedRect(guiGraphics, fieldX + 1.0F, previewY + 1.0F, 28.0F, 17.0F, 3.0F, alpha(colorPickerSetting.color(), alpha));
			drawBoldInterText(guiGraphics, colorPickerSetting.hex(), fieldX + 38.0F, previewY + 6.0F, 7.2F, alpha(MODULE_ENABLED_TEXT_COLOR, alpha));
			int color = colorPickerSetting.color();
			String rgb = "R " + ((color >>> 16) & 255) + "  G " + ((color >>> 8) & 255) + "  B " + (color & 255);
			drawBoldInterText(guiGraphics, rgb, fieldX + 38.0F, previewY + 16.0F, 5.8F, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		} finally {
			guiGraphics.disableScissor();
		}
	}

	private static void renderSaturationValueField(GuiGraphics guiGraphics, float x, float y, float width, float height, float hue, float alpha) {
		Render2D.roundedRect(guiGraphics, x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 4.0F, alpha(COLOR_PICKER_OUTLINE, alpha));
		for (int index = 0; index < COLOR_GRADIENT_STEPS; index++) {
			float from = index / (float) COLOR_GRADIENT_STEPS;
			float to = (index + 1) / (float) COLOR_GRADIENT_STEPS;
			int x1 = Math.round(x + width * from);
			int x2 = Math.max(x1 + 1, Math.round(x + width * to));
			int topColor = hsvToRgb(hue, (from + to) * 0.5F, 1.0F);
			guiGraphics.fillGradient(x1, Math.round(y), x2, Math.round(y + height), alpha(topColor, alpha), alpha(0xFF000000, alpha));
		}
	}

	private static void renderHueBar(GuiGraphics guiGraphics, float x, float y, float width, float height, float alpha) {
		Render2D.roundedRect(guiGraphics, x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 4.0F, alpha(COLOR_PICKER_OUTLINE, alpha));
		for (int index = 0; index < COLOR_GRADIENT_STEPS; index++) {
			float from = index / (float) COLOR_GRADIENT_STEPS;
			float to = (index + 1) / (float) COLOR_GRADIENT_STEPS;
			int x1 = Math.round(x + width * from);
			int x2 = Math.max(x1 + 1, Math.round(x + width * to));
			guiGraphics.fill(x1, Math.round(y), x2, Math.round(y + height), alpha(hsvToRgb((from + to) * 0.5F, 1.0F, 1.0F), alpha));
		}
	}

	private static void renderColorMarker(GuiGraphics guiGraphics, float centerX, float centerY, int selectedColor, float alpha) {
		Render2D.roundedRect(guiGraphics, centerX - 5.0F, centerY - 5.0F, 10.0F, 10.0F, 5.0F, alpha(COLOR_PICKER_MARKER_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, centerX - 3.5F, centerY - 3.5F, 7.0F, 7.0F, 3.5F, alpha(COLOR_PICKER_MARKER, alpha));
		Render2D.roundedRect(guiGraphics, centerX - 2.0F, centerY - 2.0F, 4.0F, 4.0F, 2.0F, alpha(selectedColor, alpha));
	}

	private static void renderEnumControl(GuiGraphics guiGraphics, EnumSetting<?> setting, float x, float centerY, int width, float alpha, float dropdownProgress, int textColor) {
		String text = displayValue(setting.value());
		Render2D.roundedRect(guiGraphics, x, centerY - 9.0F, width, 18, 4, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1, centerY - 8.0F, width - 2, 16, 3, alpha(MODULE_CONTROL_BACKGROUND, alpha));
		drawBoldInterText(guiGraphics, trimToWidth(text, MODULE_SETTING_SIZE, width - 24), x + 7, centerY, MODULE_SETTING_SIZE, alpha(textColor, alpha));
		renderChevronDown(guiGraphics, x + width - 10.0F, centerY, alpha);

		if (dropdownProgress <= 0.01F) {
			return;
		}

		float dropdownY = enumDropdownY(centerY);
		float dropdownHeight = enumDropdownHeight(setting);
		float visibleDropdownHeight = dropdownHeight * dropdownProgress;
		if (visibleDropdownHeight <= 2.0F) {
			return;
		}

		Render2D.roundedRect(guiGraphics, x, dropdownY, width, visibleDropdownHeight, MODULE_ENUM_DROPDOWN_RADIUS, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1, dropdownY + 1, width - 2, Math.max(0.0F, visibleDropdownHeight - 2.0F), MODULE_ENUM_DROPDOWN_RADIUS - 1, alpha(MODULE_CONTROL_BACKGROUND, alpha));

		guiGraphics.enableScissor(
				Math.round(x + 1.0F),
				Math.round(dropdownY + 1.0F),
				Math.round(x + width - 1.0F),
				Math.round(dropdownY + visibleDropdownHeight - 1.0F)
		);

		Object currentValue = setting.value();
		Enum<?>[] values = setting.values();
		for (int index = 0; index < values.length; index++) {
			float optionY = dropdownY + index * MODULE_ENUM_OPTION_HEIGHT;

			Enum<?> value = values[index];
			if (value == currentValue) {
				Render2D.roundedRect(guiGraphics, x + 2, optionY + 2, width - 4, MODULE_ENUM_OPTION_HEIGHT - 4, 3, alpha(MODULE_ENUM_OPTION_HIGHLIGHT, alpha));
			}
			drawBoldInterText(
					guiGraphics,
					trimToWidth(displayValue(value), MODULE_SETTING_SIZE, width - 14),
					x + 7,
					optionY + MODULE_ENUM_OPTION_HEIGHT / 2.0F,
					MODULE_SETTING_SIZE,
					alpha(textColor, alpha)
			);
		}
		guiGraphics.disableScissor();
	}

	private static void renderTargetControl(GuiGraphics guiGraphics, EnumBooleanMapSetting<?> setting, float x, float centerY, int width, float alpha, float dropdownProgress, int textColor) {
		Render2D.roundedRect(guiGraphics, x, centerY - 9.0F, width, 18, 4, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1, centerY - 8.0F, width - 2, 16, 3, alpha(MODULE_CONTROL_BACKGROUND, alpha));
		List<Enum<?>> enabledValues = enabledEnumValues(setting);
		float tagX = x + 6;
		float tagsRight = x + width - 20.0F;
		for (Enum<?> value : enabledValues) {
			String label = trimMultiselectTagLabel(displayValue(value), tagsRight - tagX);
			if (label.isEmpty()) {
				break;
			}

			float tagWidth = multiselectTagWidth(label);

			renderMultiselectTag(guiGraphics, label, tagX, centerY, alpha);
			tagX += tagWidth + MODULE_MULTISELECT_TAG_GAP;
		}
		if (enabledValues.isEmpty()) {
			drawBoldInterText(guiGraphics, "None", x + 7, centerY, MODULE_SETTING_SIZE, alpha(MODULE_DESCRIPTION_COLOR, alpha));
		}
		renderChevronDown(guiGraphics, x + width - 10.0F, centerY, alpha);

		renderTargetDropdown(guiGraphics, setting, x, centerY, width, alpha, dropdownProgress, textColor);
	}

	private static void renderMultiselectTag(GuiGraphics guiGraphics, String label, float x, float centerY, float alpha) {
		float tagWidth = multiselectTagWidth(label);
		float tagY = centerY - MODULE_MULTISELECT_TAG_HEIGHT / 2.0F;
		Render2D.roundedRect(guiGraphics, x, tagY, tagWidth, MODULE_MULTISELECT_TAG_HEIGHT, 3, alpha(MODULE_MULTISELECT_TAG_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, tagY + 1.0F, tagWidth - 2.0F, MODULE_MULTISELECT_TAG_HEIGHT - 2.0F, 2, alpha(MODULE_MULTISELECT_TAG_BACKGROUND, alpha));
		drawBoldInterText(
				guiGraphics,
				label,
				x + MODULE_MULTISELECT_TAG_PADDING_X,
				centerY,
				MODULE_SETTING_SIZE,
				alpha(MODULE_CONTROL_TEXT_COLOR, alpha)
		);
		drawBoldInterText(
				guiGraphics,
				"x",
				x + tagWidth - MODULE_MULTISELECT_TAG_PADDING_X - MODULE_MULTISELECT_TAG_REMOVE_WIDTH / 2.0F,
				centerY,
				MODULE_SETTING_SIZE,
				alpha(MODULE_CONTROL_TEXT_COLOR, alpha)
		);
	}

	private static void renderTargetDropdown(GuiGraphics guiGraphics, EnumBooleanMapSetting<?> setting, float x, float centerY, int width, float alpha, float dropdownProgress, int textColor) {
		if (dropdownProgress <= 0.01F) {
			return;
		}

		float dropdownY = enumDropdownY(centerY);
		float dropdownHeight = enumDropdownHeight(setting);
		float visibleDropdownHeight = dropdownHeight * dropdownProgress;
		if (visibleDropdownHeight <= 2.0F) {
			return;
		}

		Render2D.roundedRect(guiGraphics, x, dropdownY, width, visibleDropdownHeight, MODULE_ENUM_DROPDOWN_RADIUS, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1, dropdownY + 1, width - 2, Math.max(0.0F, visibleDropdownHeight - 2.0F), MODULE_ENUM_DROPDOWN_RADIUS - 1, alpha(MODULE_CONTROL_BACKGROUND, alpha));

		guiGraphics.enableScissor(
				Math.round(x + 1.0F),
				Math.round(dropdownY + 1.0F),
				Math.round(x + width - 1.0F),
				Math.round(dropdownY + visibleDropdownHeight - 1.0F)
		);

		Enum<?>[] values = enumBooleanValues(setting);
		Map<?, Boolean> map = setting.value();
		for (int index = 0; index < values.length; index++) {
			float optionY = dropdownY + index * MODULE_ENUM_OPTION_HEIGHT;
			Enum<?> value = values[index];
			boolean enabled = Boolean.TRUE.equals(map.get(value));
			if (enabled) {
				Render2D.roundedRect(guiGraphics, x + 2, optionY + 2, width - 4, MODULE_ENUM_OPTION_HEIGHT - 4, 3, alpha(MODULE_ENUM_OPTION_HIGHLIGHT, alpha));
			}
			drawBoldInterText(guiGraphics, enabled ? "x" : "", x + 7, optionY + MODULE_ENUM_OPTION_HEIGHT / 2.0F, MODULE_SETTING_SIZE, alpha(MODULE_CONTROL_TEXT_COLOR, alpha));
			drawBoldInterText(
					guiGraphics,
					trimToWidth(displayValue(value), MODULE_SETTING_SIZE, width - 24),
					x + 18,
					optionY + MODULE_ENUM_OPTION_HEIGHT / 2.0F,
					MODULE_SETTING_SIZE,
					alpha(textColor, alpha)
			);
		}
		guiGraphics.disableScissor();
	}

	private static void renderRangeControl(GuiGraphics guiGraphics, FloatRangeSetting setting, float x, float centerY, float width, float alpha, boolean moduleEnabled, float fromActiveProgress, float toActiveProgress) {
		FloatRangeSetting.Range range = setting.value();
		float boxWidth = MODULE_RANGE_VALUE_BOX_WIDTH;
		renderValueBox(guiGraphics, formatRangeValue(setting, range.from()), x, centerY, boxWidth, alpha, moduleEnabled);
		renderValueBox(guiGraphics, formatRangeValue(setting, range.to()), x + width - boxWidth, centerY, boxWidth, alpha, moduleEnabled);

		float sliderX = rangeSliderX(x);
		float sliderWidth = rangeSliderWidth(width);
		float fromX = sliderX + sliderWidth * normalized(range.from(), setting.minLimit(), setting.maxLimit());
		float toX = sliderX + sliderWidth * normalized(range.to(), setting.minLimit(), setting.maxLimit());
		int trackColor = moduleEnabled ? MODULE_SLIDER_TRACK : MODULE_DISABLED_CHECKBOX;
		int activeTrackColor = moduleEnabled ? ThemeManager.darkAccent() : MODULE_INACTIVE_CHECKBOX_MARK;
		int handleColor = moduleEnabled ? ThemeManager.accent() : MODULE_INACTIVE_CHECKBOX_MARK;
		int outlineColor = moduleEnabled ? ThemeManager.darkAccent() : MODULE_INACTIVE_CHECKBOX_OUTLINE;

		float trackY = centerY - MODULE_RANGE_TRACK_HEIGHT / 2.0F;
		renderSliderTrack(guiGraphics, sliderX, trackY, sliderWidth, MODULE_RANGE_TRACK_HEIGHT, trackColor, alpha);
		Render2D.roundedRect(guiGraphics, fromX, trackY, Math.max(1.0F, toX - fromX), MODULE_RANGE_TRACK_HEIGHT, MODULE_RANGE_TRACK_HEIGHT / 2.0F, alpha(activeTrackColor, alpha));
		renderRangeHandle(guiGraphics, fromX, centerY, handleColor, outlineColor, alpha, fromActiveProgress);
		renderRangeHandle(guiGraphics, toX, centerY, handleColor, outlineColor, alpha, toActiveProgress);
	}

	private static void renderSliderControl(GuiGraphics guiGraphics, FloatSetting setting, float x, float centerY, float width, float alpha, boolean moduleEnabled, float activeProgress) {
		float boxWidth = MODULE_RANGE_VALUE_BOX_WIDTH;
		renderValueBox(guiGraphics, formatSliderValue(setting, setting.floatValue()), x + width - boxWidth, centerY, boxWidth, alpha, moduleEnabled);

		float sliderX = x;
		float sliderWidth = Math.max(8.0F, width - boxWidth - MODULE_RANGE_VALUE_BOX_GAP);
		float handleX = sliderX + sliderWidth * normalized(setting.floatValue(), setting.minLimit(), setting.maxLimit());
		int trackColor = moduleEnabled ? MODULE_SLIDER_TRACK : MODULE_DISABLED_CHECKBOX;
		int activeTrackColor = moduleEnabled ? ThemeManager.darkAccent() : MODULE_INACTIVE_CHECKBOX_MARK;
		int handleColor = moduleEnabled ? ThemeManager.accent() : MODULE_INACTIVE_CHECKBOX_MARK;
		int outlineColor = moduleEnabled ? ThemeManager.darkAccent() : MODULE_INACTIVE_CHECKBOX_OUTLINE;

		float trackY = centerY - MODULE_RANGE_TRACK_HEIGHT / 2.0F;
		renderSliderTrack(guiGraphics, sliderX, trackY, sliderWidth, MODULE_RANGE_TRACK_HEIGHT, trackColor, alpha);
		renderSliderTrack(guiGraphics, sliderX, trackY, Math.max(MODULE_RANGE_TRACK_HEIGHT, handleX - sliderX), MODULE_RANGE_TRACK_HEIGHT, activeTrackColor, alpha);
		renderRangeHandle(guiGraphics, handleX, centerY, handleColor, outlineColor, alpha, activeProgress);
	}

	private static void renderSliderTrack(GuiGraphics guiGraphics, float x, float y, float width, float height, int color, float alpha) {
		float radius = height * 0.45F;
		int appliedColor = alpha(color, alpha);
		if (width <= height) {
			Render2D.roundedRect(guiGraphics, x, y, width, height, radius, appliedColor);
			return;
		}

		Render2D.roundedRect(guiGraphics, x, y, height, height, radius, appliedColor);
		Render2D.roundedRect(guiGraphics, x + radius, y, width - height, height, 0.0F, appliedColor);
		Render2D.roundedRect(guiGraphics, x + width - height, y, height, height, radius, appliedColor);
	}

	private static void renderValueBox(GuiGraphics guiGraphics, String text, float x, float centerY, float width, float alpha, boolean moduleEnabled) {
		Render2D.roundedRect(guiGraphics, x, centerY - 5.0F, width, 10, 2.5F, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, x + 1.0F, centerY - 4.0F, width - 2.0F, 8, 1.8F, alpha(MODULE_CONTROL_BACKGROUND, alpha));
		int textColor = moduleEnabled ? MODULE_CONTROL_TEXT_COLOR : MODULE_INACTIVE_CHECKBOX_MARK;
		LOGO_FONT.draw(
				guiGraphics,
				text,
				LOGO_FONT.centeredX(text, MODULE_RANGE_VALUE_SIZE, x + width / 2.0F),
				LOGO_FONT.centeredBaseline(text, MODULE_RANGE_VALUE_SIZE, centerY),
				MODULE_RANGE_VALUE_SIZE,
				alpha(textColor, alpha)
		);
	}

	private static void renderRangeHandle(GuiGraphics guiGraphics, float centerX, float centerY, int handleColor, int outlineColor, float alpha, float activeProgress) {
		float handleSize = MODULE_RANGE_HANDLE_SIZE + (MODULE_RANGE_HANDLE_ACTIVE_SIZE - MODULE_RANGE_HANDLE_SIZE) * activeProgress;
		float baseOutlineSize = handleSize + 2.0F;
		Render2D.roundedRect(
				guiGraphics,
				centerX - baseOutlineSize / 2.0F,
				centerY - baseOutlineSize / 2.0F,
				baseOutlineSize,
				baseOutlineSize,
				baseOutlineSize / 2.0F,
				alpha(MODULE_CONTROL_BACKGROUND, alpha)
		);
		if (activeProgress > 0.01F) {
			float outlineSize = MODULE_RANGE_HANDLE_SIZE + (MODULE_RANGE_HANDLE_OUTLINE_SIZE - MODULE_RANGE_HANDLE_SIZE) * activeProgress;
			Render2D.roundedRect(
					guiGraphics,
					centerX - outlineSize / 2.0F,
					centerY - outlineSize / 2.0F,
					outlineSize,
					outlineSize,
					outlineSize / 2.0F,
					alpha(outlineColor, alpha * activeProgress)
			);
		}

		Render2D.roundedRect(
				guiGraphics,
				centerX - handleSize / 2.0F,
				centerY - handleSize / 2.0F,
				handleSize,
				handleSize,
				handleSize / 2.0F,
				alpha(handleColor, alpha)
		);
	}

	private static void renderCheckbox(GuiGraphics guiGraphics, float x, float y, boolean enabled, boolean moduleEnabled, float alpha) {
		int background = enabled ? (moduleEnabled ? ThemeManager.accent() : MODULE_INACTIVE_CHECKBOX) : MODULE_DISABLED_CHECKBOX;
		int outline = enabled ? (moduleEnabled ? ThemeManager.accent() : MODULE_INACTIVE_CHECKBOX_OUTLINE) : MODULE_CONTROL_OUTLINE;
		int mark = moduleEnabled ? ThemeManager.mutedAccent() : MODULE_INACTIVE_CHECKBOX_MARK;
		Render2D.roundedRect(guiGraphics, x, y, 14, 14, 4, alpha(outline, alpha));
		Render2D.roundedRect(guiGraphics, x + 1, y + 1, 12, 12, 3, alpha(background, alpha));
		if (enabled) {
			renderCheckMark(guiGraphics, x + 7.0F, y + 7.0F, mark, alpha);
		}
	}

	private static void renderCheckMark(GuiGraphics guiGraphics, float centerX, float centerY, int color, float alpha) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, centerY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(CHECKMARK_ICON_ROTATION));
		guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
		CHECKMARK_FONT.draw(
				guiGraphics,
				CHECKMARK_ICON,
				CHECKMARK_FONT.centeredX(CHECKMARK_ICON, CHECKMARK_ICON_SIZE, centerX),
				CHECKMARK_FONT.centeredBaseline(CHECKMARK_ICON, CHECKMARK_ICON_SIZE, centerY),
				CHECKMARK_ICON_SIZE,
				alpha(color, alpha),
				0.0F,
				CHECKMARK_ICON_THICKNESS
		);
		guiGraphics.pose().popPose();
	}

	private static void renderChevronDown(GuiGraphics guiGraphics, float centerX, float centerY, float alpha) {
		int color = alpha(MODULE_DESCRIPTION_COLOR, alpha);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX - 1.75F, centerY - 0.5F, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
		Render2D.roundedRect(guiGraphics, -0.75F, -0.75F, 4.0F, 1.5F, 0.75F, color);
		guiGraphics.pose().popPose();

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX + 1.75F, centerY - 0.5F, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-45.0F));
		Render2D.roundedRect(guiGraphics, -3.25F, -0.75F, 4.0F, 1.5F, 0.75F, color);
		guiGraphics.pose().popPose();
	}

	private List<ModuleLayout> moduleLayouts(int left, int top) {
		List<Module> modules = filteredModules();
		List<ModuleLayout> layouts = new ArrayList<>();
		int areaX = moduleAreaX(left);
		int areaY = moduleAreaY(top);
		int areaWidth = moduleAreaWidth();
		int areaHeight = moduleAreaHeight();
		int areaBottom = areaY + areaHeight;
		int columnCount = singleColumnModules ? 1 : 2;
		int columnWidth = columnCount == 1 ? areaWidth : (areaWidth - MODULE_COLUMN_GAP) / 2;
		int[] columnY = new int[columnCount];
		int scrollOffset = Math.round(clampedModuleScroll(left, top));
		for (int index = 0; index < columnY.length; index++) {
			columnY[index] = areaY - scrollOffset;
		}

		for (Module module : modules) {
			int column = shortestColumn(columnY);
			int x = areaX + column * (columnWidth + MODULE_COLUMN_GAP);
			int height = moduleCardHeight(module, columnWidth);
			if (columnY[column] + height > areaY && columnY[column] < areaBottom) {
				layouts.add(new ModuleLayout(module, x, columnY[column], columnWidth, height));
			}
			columnY[column] += height + MODULE_CARD_GAP;
		}

		return layouts;
	}

	private static int shortestColumn(int[] columnY) {
		int shortest = 0;
		for (int index = 1; index < columnY.length; index++) {
			if (columnY[index] < columnY[shortest]) {
				shortest = index;
			}
		}
		return shortest;
	}

	private float clampedModuleScroll(int left, int top) {
		float maxScroll = maxModuleScroll(left, top);
		targetModuleScrollOffset = Math.max(0.0F, Math.min(targetModuleScrollOffset, maxScroll));
		moduleScrollOffset = Math.max(0.0F, Math.min(moduleScrollOffset, maxScroll));
		return moduleScrollOffset;
	}

	private float maxModuleScroll(int left, int top) {
		List<Module> modules = filteredModules();
		if (modules.isEmpty()) {
			return 0.0F;
		}

		int areaWidth = moduleAreaWidth();
		int areaHeight = moduleAreaHeight();
		int columnCount = singleColumnModules ? 1 : 2;
		int columnWidth = columnCount == 1 ? areaWidth : (areaWidth - MODULE_COLUMN_GAP) / 2;
		int[] columnY = new int[columnCount];

		for (Module module : modules) {
			int column = shortestColumn(columnY);
			columnY[column] += moduleCardHeight(module, columnWidth) + MODULE_CARD_GAP;
		}

		int contentHeight = 0;
		for (int height : columnY) {
			contentHeight = Math.max(contentHeight, Math.max(0, height - MODULE_CARD_GAP));
		}

		return Math.max(0.0F, contentHeight - areaHeight);
	}

	private static int moduleAreaX(int left) {
		return left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
	}

	private static int moduleAreaY(int top) {
		return top + TOPBAR_HEIGHT + MODULE_AREA_PADDING_Y;
	}

	private static int moduleAreaWidth() {
		return PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2;
	}

	private List<Module> filteredModules() {
		String query = searchText.trim().toLowerCase(Locale.ROOT);
		return ModuleManager.modules(selectedCategory()).stream()
				.filter(module -> query.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(query) || module.description().toLowerCase(Locale.ROOT).contains(query))
				.toList();
	}

	private int moduleCardHeight(Module module, int cardWidth) {
		return MODULE_CARD_HEIGHT;
	}

	private float maxModuleSettingsScroll(Module module) {
		if (module == null) {
			return 0.0F;
		}
		int settingWidth = Math.round(MODULE_SETTINGS_PANEL_WIDTH - MODULE_SETTINGS_PANEL_PADDING * 2.0F);
		float contentHeight = settingsContentHeight(module, settingWidth);
		float viewportHeight = MODULE_SETTINGS_PANEL_HEIGHT - 46.0F;
		return Math.max(0.0F, contentHeight - viewportHeight);
	}

	private static int moduleAreaHeight() {
		return PANEL_HEIGHT - TOPBAR_HEIGHT - MODULE_AREA_PADDING_Y * 2;
	}

	private float settingsContentHeight(Module module, int settingWidth) {
		float contentHeight = MODULE_SETTING_TOP_PADDING + MODULE_SETTING_BOTTOM_PADDING;
		for (Setting<?> setting : orderedSettings(module)) {
			contentHeight += settingRowHeight(setting, settingWidth);
		}
		return contentHeight;
	}

	private float clampedSettingsScroll(Module module, int settingWidth, int cardHeight) {
		float maxScroll = maxSettingsScroll(module, settingWidth, cardHeight);
		float target = targetSettingsScrollOffsets.getOrDefault(module, 0.0F);
		if (target > maxScroll) {
			targetSettingsScrollOffsets.put(module, maxScroll);
		}
		float scroll = Math.max(0.0F, Math.min(settingsScrollOffsets.getOrDefault(module, 0.0F), maxScroll));
		if (scroll > 0.0F) {
			settingsScrollOffsets.put(module, scroll);
		} else {
			settingsScrollOffsets.remove(module);
		}
		return scroll;
	}

	private float maxSettingsScroll(Module module, int settingWidth, int cardHeight) {
		float viewportHeight = Math.max(0.0F, cardHeight - MODULE_CARD_HEIGHT);
		return Math.max(0.0F, settingsContentHeight(module, settingWidth) - viewportHeight);
	}

	private static List<Setting<?>> orderedSettings(Module module) {
		List<Setting<?>> ordered = new ArrayList<>();
		for (Setting<?> setting : module.settings()) {
			if (setting.visible() && (setting instanceof EnumSetting<?> || setting instanceof EnumBooleanMapSetting<?>)) {
				ordered.add(setting);
			}
		}
		for (Setting<?> setting : module.settings()) {
			if (setting.visible()
					&& !(setting instanceof EnumSetting<?>)
					&& !(setting instanceof EnumBooleanMapSetting<?>)
					&& !(setting instanceof FloatRangeSetting)
					&& !(setting instanceof FloatSetting)
					&& !(setting instanceof BooleanSetting)) {
				ordered.add(setting);
			}
		}
		for (Setting<?> setting : module.settings()) {
			if (setting.visible() && (setting instanceof FloatRangeSetting || setting instanceof FloatSetting)) {
				ordered.add(setting);
			}
		}
		for (Setting<?> setting : module.settings()) {
			if (setting.visible() && setting instanceof BooleanSetting) {
				ordered.add(setting);
			}
		}

		return applySettingAnchors(ordered);
	}

	private static List<Setting<?>> applySettingAnchors(List<Setting<?>> ordered) {
		List<Setting<?>> anchored = new ArrayList<>();
		for (Setting<?> setting : ordered) {
			if (setting.displayAfter() == null) {
				anchored.add(setting);
			}
		}
		for (Setting<?> setting : ordered) {
			Setting<?> anchor = setting.displayAfter();
			if (anchor == null) {
				continue;
			}

			int anchorIndex = anchored.indexOf(anchor);
			if (anchorIndex < 0) {
				anchored.add(setting);
			} else {
				anchored.add(anchorIndex + 1, setting);
			}
		}

		return anchored;
	}

	private float settingRowHeight(Setting<?> setting, int width) {
		float rowHeight = settingBaseRowHeight(setting, width);
		if (setting instanceof EnumSetting<?> enumSetting) {
			return settingDropdownRowHeight(setting, rowHeight, enumDropdownHeight(enumSetting), enumDropdownAnimation(enumSetting));
		}
		if (setting instanceof EnumBooleanMapSetting<?> mapSetting) {
			return settingDropdownRowHeight(setting, rowHeight, enumDropdownHeight(mapSetting), enumDropdownAnimation(mapSetting));
		}

		return rowHeight;
	}

	private static float settingDropdownRowHeight(Setting<?> setting, float rowHeight, float dropdownHeight, float dropdownProgress) {
		float dropdownBottom = settingTopControlCenterY(setting, 0.0F)
				+ 9.0F
				+ MODULE_ENUM_DROPDOWN_GAP
				+ dropdownHeight * dropdownProgress
				+ MODULE_ENUM_DROPDOWN_BOTTOM_PADDING;
		return Math.max(rowHeight, dropdownBottom);
	}

	private static float settingBaseRowHeight(Setting<?> setting, int width) {
		if (setting instanceof HelperCustomItemsSetting customItemsSetting) {
			int rows = Math.max(1, customItemsSetting.entries().size());
			return 25.0F
					+ HELPER_CUSTOM_PROFILE_HEIGHT
					+ HELPER_CUSTOM_ITEM_GAP
					+ HELPER_CUSTOM_ADD_HEIGHT
					+ HELPER_CUSTOM_ITEM_GAP
					+ rows * HELPER_CUSTOM_ITEM_HEIGHT
					+ Math.max(0, rows - 1) * HELPER_CUSTOM_ITEM_GAP
					+ 4.0F;
		}
		if (setting instanceof CustomSwordSetting) {
			return CUSTOM_SWORD_ROW_HEIGHT;
		}
		List<String> descriptionLines = settingDescriptionLines(setting, width);
		if (descriptionLines.isEmpty()) {
			return MODULE_SETTING_ROW_HEIGHT;
		}

		float descriptionBottom = MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET
				+ (descriptionLines.size() - 1) * MODULE_SETTING_DESCRIPTION_LINE_HEIGHT
				+ MODULE_SETTING_DESCRIPTION_SIZE / 2.0F
				+ MODULE_SETTING_DESCRIPTION_BOTTOM_PADDING;
		return Math.max(MODULE_SETTING_ROW_HEIGHT, (float) Math.ceil(descriptionBottom));
	}

	private static List<String> settingDescriptionLines(Setting<?> setting, int width) {
		if (setting.description().isEmpty()) {
			return List.of();
		}

		return wrapToLines(setting.description(), MODULE_SETTING_DESCRIPTION_SIZE, settingTextMaxWidth(setting, width), MODULE_SETTING_DESCRIPTION_MAX_LINES);
	}

	private static float settingTextMaxWidth(Setting<?> setting, int width) {
		return Math.max(24.0F, width - settingControlWidth(setting, width) - MODULE_SETTING_CONTROL_GAP);
	}

	private static float settingControlWidth(Setting<?> setting, int width) {
		if (setting instanceof HelperCustomItemsSetting) {
			return 0.0F;
		}
		if (setting instanceof CustomSwordSetting) {
			return 0.0F;
		}
		if (setting instanceof ColorSetting) {
			return Math.min(MODULE_COLOR_CONTROL_WIDTH, Math.max(68.0F, width * 0.42F));
		}
		if (setting instanceof FloatRangeSetting || setting instanceof FloatSetting) {
			return Math.min(MODULE_RANGE_CONTROL_WIDTH, Math.max(72.0F, width * 0.48F));
		}
		if (setting instanceof EnumSetting<?> || setting instanceof EnumBooleanMapSetting<?>) {
			return Math.min(MODULE_COMPACT_CONTROL_WIDTH, Math.max(72.0F, width * 0.48F));
		}
		if (setting instanceof KeySetting) {
			return Math.min(40.0F, Math.max(32.0F, width * 0.22F));
		}
		if (setting instanceof BooleanSetting) {
			return 14.0F;
		}
		if (setting instanceof ItemStackSetting) {
			ItemStackSetting itemSetting = (ItemStackSetting) setting;
			String label = itemSetting.hasValue() ? itemSetting.selectedName() : "Choose item";
			float iconSpace = itemSetting.hasValue() ? 22.0F : 7.0F;
			float labelWidth = Math.min(96.0F, LOGO_FONT.width(label, MODULE_SETTING_SIZE));
			return Math.min(138.0F, Math.max(68.0F, iconSpace + labelWidth + 24.0F));
		}

		return 0.0F;
	}

	private static float settingCheckboxY(Setting<?> setting, float rowY) {
		float centerY = setting.description().isEmpty()
				? rowY + MODULE_SETTING_TITLE_CENTER_OFFSET
				: rowY + (MODULE_SETTING_TITLE_CENTER_OFFSET + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET) / 2.0F;
		return centerY - 7.0F;
	}

	private static float settingTopControlCenterY(Setting<?> setting, float rowY) {
		return setting.description().isEmpty()
				? rowY + MODULE_SETTING_TITLE_CENTER_OFFSET
				: rowY + (MODULE_SETTING_TITLE_CENTER_OFFSET + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET) / 2.0F;
	}

	private static float enumDropdownY(float centerY) {
		return centerY + 9.0F + MODULE_ENUM_DROPDOWN_GAP;
	}

	private static float enumDropdownHeight(EnumSetting<?> setting) {
		return setting.values().length * MODULE_ENUM_OPTION_HEIGHT;
	}

	private static float enumDropdownHeight(EnumBooleanMapSetting<?> setting) {
		return enumBooleanValues(setting).length * MODULE_ENUM_OPTION_HEIGHT;
	}

	private static float multiselectTagWidth(String label) {
		return MODULE_MULTISELECT_TAG_PADDING_X * 2.0F
				+ MODULE_MULTISELECT_TAG_REMOVE_WIDTH
				+ MODULE_MULTISELECT_TAG_GAP
				+ LOGO_FONT.width(label, MODULE_SETTING_SIZE);
	}

	private static String trimMultiselectTagLabel(String label, float availableWidth) {
		float maxLabelWidth = availableWidth
				- MODULE_MULTISELECT_TAG_PADDING_X * 2.0F
				- MODULE_MULTISELECT_TAG_REMOVE_WIDTH
				- MODULE_MULTISELECT_TAG_GAP;
		if (maxLabelWidth < MODULE_MULTISELECT_TAG_MIN_LABEL_WIDTH) {
			return "";
		}
		if (LOGO_FONT.width(label, MODULE_SETTING_SIZE) <= maxLabelWidth) {
			return label;
		}

		String trimmed = label;
		while (!trimmed.isEmpty() && LOGO_FONT.width(trimmed, MODULE_SETTING_SIZE) > maxLabelWidth) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static String formatTechnicalName(String str) {
		if (str == null || str.isEmpty()) {
			return "";
		}
		if (str.indexOf('_') >= 0 || (str.length() > 1 && str.equals(str.toUpperCase(Locale.ROOT)) && !str.equals(str.toLowerCase(Locale.ROOT)))) {
			String[] parts = str.split("_+");
			StringBuilder sb = new StringBuilder();
			for (String part : parts) {
				if (part.isEmpty()) continue;
				if (sb.length() > 0) sb.append(' ');
				String lower = part.toLowerCase(Locale.ROOT);
				sb.append(Character.toUpperCase(lower.charAt(0)));
				if (lower.length() > 1) {
					sb.append(lower.substring(1));
				}
			}
			return sb.toString();
		}
		return str;
	}

	private static String settingValue(Setting<?> setting) {
		if (setting instanceof ColorSetting colorSetting) {
			return colorSetting.hex();
		}
		Object value = setting.value();
		if (value == null) {
			return "n/a";
		}
		if (value instanceof FloatRangeSetting.Range range) {
			return formatFloat(range.from()) + " - " + formatFloat(range.to());
		}
		if (value instanceof java.util.EnumMap<?, ?> map) {
			long enabled = map.values().stream()
					.filter(Boolean.TRUE::equals)
					.count();
			return enabled + "/" + map.size();
		}

		return displayValue(value);
	}

	private static String displayValue(Object value) {
		if (value == null) {
			return "n/a";
		}

		try {
			Object display = value.getClass().getMethod("display").invoke(value);
			if (display instanceof String text && !text.isEmpty()) {
				return formatTechnicalName(text);
			}
		} catch (ReflectiveOperationException ignored) {
		}

		try {
			Object label = value.getClass().getMethod("label").invoke(value);
			if (label instanceof String text && !text.isEmpty()) {
				return formatTechnicalName(text);
			}
		} catch (ReflectiveOperationException ignored) {
		}

		return formatTechnicalName(value.toString());
	}

	private static List<Enum<?>> enabledEnumValues(EnumBooleanMapSetting<?> setting) {
		List<Enum<?>> values = new ArrayList<>();
		for (Map.Entry<?, Boolean> entry : setting.value().entrySet()) {
			if (Boolean.TRUE.equals(entry.getValue())) {
				values.add((Enum<?>) entry.getKey());
			}
		}

		return values;
	}

	private static Enum<?>[] enumBooleanValues(EnumBooleanMapSetting<?> setting) {
		return (Enum<?>[]) setting.enumType().getEnumConstants();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void setEnumValue(EnumSetting<?> setting, Enum<?> value) {
		((EnumSetting) setting).setValue(value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void setEnumBooleanValue(EnumBooleanMapSetting<?> setting, Enum<?> value, boolean enabled) {
		((EnumBooleanMapSetting) setting).setEnabled(value, enabled);
	}

	private boolean isEnumDropdownOpen(Setting<?> setting) {
		return Boolean.TRUE.equals(expandedEnumSettings.get(setting));
	}

	private void toggleEnumDropdown(Setting<?> setting) {
		if (isEnumDropdownOpen(setting)) {
			expandedEnumSettings.remove(setting);
			return;
		}

		expandedEnumSettings.put(setting, true);
	}

	private void closeEnumDropdowns() {
		expandedEnumSettings.clear();
	}

	private void updateRangeHandleAnimations(float frameDelta) {
		updateRangeHandleAnimationMap(rangeFromHandleAnimations, RangeHandle.FROM, frameDelta);
		updateRangeHandleAnimationMap(rangeToHandleAnimations, RangeHandle.TO, frameDelta);
		updateSliderHandleAnimations(frameDelta);
	}

	private void updateLayoutToggleAnimation(float frameDelta) {
		float target = singleColumnModules ? 1.0F : 0.0F;
		layoutToggleProgress += (target - layoutToggleProgress) * Math.min(1.0F, LAYOUT_TOGGLE_ANIMATION_SPEED * frameDelta);
		if (Math.abs(layoutToggleProgress - target) < 0.01F) {
			layoutToggleProgress = target;
		}
	}

	private void updateRangeHandleAnimationMap(Map<FloatRangeSetting, Float> animations, RangeHandle handle, float frameDelta) {
		for (FloatRangeSetting setting : rangeSettings()) {
			float target = draggedRangeSetting == setting && draggedRangeHandle == handle ? 1.0F : 0.0F;
			float current = animations.getOrDefault(setting, 0.0F);
			float updated = current + (target - current) * Math.min(1.0F, MODULE_RANGE_HANDLE_ANIMATION_SPEED * frameDelta);
			if (Math.abs(updated - target) < 0.01F) {
				updated = target;
			}
			if (updated > 0.0F) {
				animations.put(setting, updated);
			} else {
				animations.remove(setting);
			}
		}
	}

	private List<FloatRangeSetting> rangeSettings() {
		List<FloatRangeSetting> settings = new ArrayList<>();
		for (Module module : filteredModules()) {
			for (Setting<?> setting : module.settings()) {
				if (setting.visible() && setting instanceof FloatRangeSetting rangeSetting) {
					settings.add(rangeSetting);
				}
			}
		}

		return settings;
	}

	private void updateSliderHandleAnimations(float frameDelta) {
		for (FloatSetting setting : sliderSettings()) {
			float target = draggedSliderSetting == setting ? 1.0F : 0.0F;
			float current = sliderHandleAnimations.getOrDefault(setting, 0.0F);
			float updated = current + (target - current) * Math.min(1.0F, MODULE_RANGE_HANDLE_ANIMATION_SPEED * frameDelta);
			if (Math.abs(updated - target) < 0.01F) {
				updated = target;
			}
			if (updated > 0.0F) {
				sliderHandleAnimations.put(setting, updated);
			} else {
				sliderHandleAnimations.remove(setting);
			}
		}
	}

	private List<FloatSetting> sliderSettings() {
		List<FloatSetting> settings = new ArrayList<>();
		for (Module module : filteredModules()) {
			for (Setting<?> setting : module.settings()) {
				if (setting.visible() && setting instanceof FloatSetting floatSetting) {
					settings.add(floatSetting);
				}
			}
		}

		return settings;
	}

	private float rangeHandleAnimation(FloatRangeSetting setting, RangeHandle handle) {
		return handle == RangeHandle.FROM
				? rangeFromHandleAnimations.getOrDefault(setting, 0.0F)
				: rangeToHandleAnimations.getOrDefault(setting, 0.0F);
	}

	private float sliderHandleAnimation(FloatSetting setting) {
		return sliderHandleAnimations.getOrDefault(setting, 0.0F);
	}

	private static float normalized(float value, float min, float max) {
		if (max <= min) {
			return 0.0F;
		}

		return Math.max(0.0F, Math.min(1.0F, (value - min) / (max - min)));
	}

	private static float rangeSliderX(float controlX) {
		return controlX + MODULE_RANGE_VALUE_BOX_WIDTH + MODULE_RANGE_VALUE_BOX_GAP;
	}

	private static float rangeSliderWidth(float controlWidth) {
		return Math.max(8.0F, controlWidth - MODULE_RANGE_VALUE_BOX_WIDTH * 2.0F - MODULE_RANGE_VALUE_BOX_GAP * 2.0F);
	}

	private static float sliderWidth(float controlWidth) {
		return Math.max(8.0F, controlWidth - MODULE_RANGE_VALUE_BOX_WIDTH - MODULE_RANGE_VALUE_BOX_GAP);
	}

	private static float rangeValueFromMouse(FloatRangeSetting setting, float sliderX, float sliderWidth, double mouseX) {
		float progress = Math.max(0.0F, Math.min(1.0F, ((float) mouseX - sliderX) / sliderWidth));
		float value = setting.minLimit() + (setting.maxLimit() - setting.minLimit()) * progress;
		if (setting.step() > 0.0F) {
			int slot = Math.round((value - setting.minLimit()) / setting.step());
			value = setting.minLimit() + slot * setting.step();
		}
		if (setting.precision() >= 0) {
			float scale = (float) Math.pow(10.0, setting.precision());
			value = Math.round(value * scale) / scale;
		}

		return Math.max(setting.minLimit(), Math.min(setting.maxLimit(), value));
	}

	private static float sliderValueFromMouse(FloatSetting setting, float sliderX, float sliderWidth, double mouseX) {
		float progress = Math.max(0.0F, Math.min(1.0F, ((float) mouseX - sliderX) / sliderWidth));
		float value = setting.minLimit() + (setting.maxLimit() - setting.minLimit()) * progress;
		if (setting.step() > 0.0F) {
			int slot = Math.round((value - setting.minLimit()) / setting.step());
			value = setting.minLimit() + slot * setting.step();
		}
		if (setting.precision() >= 0) {
			float scale = (float) Math.pow(10.0, setting.precision());
			value = Math.round(value * scale) / scale;
		}

		return Math.max(setting.minLimit(), Math.min(setting.maxLimit(), value));
	}

	private static String formatRangeValue(FloatRangeSetting setting, float value) {
		String formatted = setting.precision() < 0
				? Float.toString(value)
				: String.format(Locale.ROOT, "%." + setting.precision() + "f", value);
		String suffix = setting.suffix();
		return suffix == null || suffix.isEmpty() ? formatted : formatted + suffix;
	}

	private static String formatSliderValue(FloatSetting setting, float value) {
		String formatted = setting.precision() < 0
				? Float.toString(value)
				: String.format(Locale.ROOT, "%." + setting.precision() + "f", value);
		String suffix = setting.suffix();
		return suffix == null || suffix.isEmpty() ? formatted : formatted + suffix;
	}

	private static String formatFloat(float value) {
		if (value == Math.round(value)) {
			return Integer.toString(Math.round(value));
		}

		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String trimToWidth(String text, float size, float maxWidth) {
		if (LOGO_FONT.width(text, size) <= maxWidth) {
			return text;
		}

		String suffix = "...";
		String trimmed = text;
		while (!trimmed.isEmpty() && LOGO_FONT.width(trimmed + suffix, size) > maxWidth) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed + suffix;
	}

	private static List<String> wrapToLines(String text, float size, float maxWidth, int maxLines) {
		List<String> lines = new ArrayList<>();
		if (text.isBlank() || maxLines <= 0) {
			return lines;
		}

		String[] words = text.split("\\s+");
		StringBuilder line = new StringBuilder();
		boolean truncated = false;

		for (String word : words) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (LOGO_FONT.width(candidate, size) <= maxWidth) {
				line.setLength(0);
				line.append(candidate);
				continue;
			}

			if (!line.isEmpty()) {
				lines.add(line.toString());
				line.setLength(0);
			}

			if (lines.size() == maxLines) {
				truncated = true;
				break;
			}

			line.append(word);
			if (LOGO_FONT.width(line.toString(), size) > maxWidth) {
				lines.add(trimToWidth(line.toString(), size, maxWidth));
				line.setLength(0);
			}

			if (lines.size() == maxLines) {
				truncated = true;
				break;
			}
		}

		if (!line.isEmpty() && lines.size() < maxLines) {
			lines.add(line.toString());
		}

		if (truncated && lines.size() == maxLines) {
			int lastIndex = lines.size() - 1;
			lines.set(lastIndex, trimToWidth(lines.get(lastIndex), size, maxWidth));
		}

		return lines;
	}

	private void renderSearchBar(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int searchX = searchBarX(left);
		int searchY = top + (TOPBAR_HEIGHT - SEARCH_HEIGHT) / 2;
		Render2D.roundedRect(guiGraphics, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, SEARCH_RADIUS, alpha(NAV_SECTION, alpha));
		float searchCenterY = searchY + SEARCH_HEIGHT / 2.0F;
		float searchIconX = HATRED_MINCED_ICON_FONT.centeredX(SEARCH_ICON, SEARCH_ICON_SIZE, searchX + 11.0F);
		float searchIconY = HATRED_MINCED_ICON_FONT.centeredBaseline(SEARCH_ICON, SEARCH_ICON_SIZE, searchCenterY);
		HATRED_MINCED_ICON_FONT.draw(guiGraphics, SEARCH_ICON, searchIconX, searchIconY, SEARCH_ICON_SIZE, alpha(SEARCH_TEXT_COLOR, alpha));

		String shortcutText = "Ctrl+F";
		int shortcutWidth = Math.round(LOGO_FONT.width(shortcutText, SEARCH_SHORTCUT_TEXT_SIZE)) + SEARCH_SHORTCUT_PADDING * 2;
		int shortcutHeight = SEARCH_SHORTCUT_HEIGHT;
		int shortcutX = searchX + SEARCH_WIDTH - shortcutWidth - 4;
		String displayText = searchText.isEmpty() ? "Search..." : searchText;
		int displayColor = searchText.isEmpty() ? SEARCH_TEXT_COLOR : MODULE_TITLE_COLOR;
		float textX = searchX + 22;
		float textMaxWidth = shortcutX - searchX - 28;
		String trimmedDisplayText = trimToWidth(displayText, SEARCH_TEXT_SIZE, textMaxWidth);
		float textBaselineY = LOGO_FONT.centeredBaseline(SEARCH_BASELINE_REFERENCE, SEARCH_TEXT_SIZE, searchCenterY);
		drawBoldInterTextAtBaseline(guiGraphics, trimmedDisplayText, textX, textBaselineY, SEARCH_TEXT_SIZE, alpha(displayColor, alpha));
		if (searchFocused && searchCaretVisible()) {
			float caretX = searchText.isEmpty()
					? textX
					: textX + LOGO_FONT.width(trimToWidth(searchText, SEARCH_TEXT_SIZE, textMaxWidth), SEARCH_TEXT_SIZE) + INTER_BOLD_OFFSET + SEARCH_CARET_GAP;
			Render2D.roundedRect(guiGraphics, caretX, searchCenterY - 5.0F, 1.0F, 10.0F, 0.5F, alpha(0xFFFFFFFF, alpha));
		}

		int shortcutY = searchY + (SEARCH_HEIGHT - shortcutHeight) / 2;
		float shortcutCenterY = shortcutY + shortcutHeight / 2.0F;
		Render2D.roundedRect(guiGraphics, shortcutX, shortcutY, shortcutWidth, shortcutHeight, 3, alpha(SEARCH_SHORTCUT_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, shortcutX + 1, shortcutY + 1, shortcutWidth - 2, shortcutHeight - 2, 2, alpha(SEARCH_SHORTCUT_BACKGROUND, alpha));
		LOGO_FONT.draw(
				guiGraphics,
				shortcutText,
				LOGO_FONT.centeredX(shortcutText, SEARCH_SHORTCUT_TEXT_SIZE, shortcutX + shortcutWidth / 2.0F),
				LOGO_FONT.centeredBaseline(shortcutText, SEARCH_SHORTCUT_TEXT_SIZE, shortcutCenterY),
				SEARCH_SHORTCUT_TEXT_SIZE,
				alpha(SEARCH_SHORTCUT_TEXT_COLOR, alpha)
		);
	}

	private void renderLayoutToggle(GuiGraphics guiGraphics, int left, int top, float alpha) {
		int toggleX = layoutToggleX(left);
		int toggleY = top + (TOPBAR_HEIGHT - LAYOUT_TOGGLE_HEIGHT) / 2;
		Render2D.roundedRect(guiGraphics, toggleX, toggleY, LAYOUT_TOGGLE_WIDTH, LAYOUT_TOGGLE_HEIGHT, LAYOUT_TOGGLE_RADIUS, alpha(MODULE_CONTROL_OUTLINE, alpha));
		Render2D.roundedRect(guiGraphics, toggleX + 1, toggleY + 1, LAYOUT_TOGGLE_WIDTH - 2, LAYOUT_TOGGLE_HEIGHT - 2, LAYOUT_TOGGLE_RADIUS - 1, alpha(NAV_SECTION, alpha));
		float selectedX = toggleX + layoutToggleProgress * LAYOUT_TOGGLE_OPTION_WIDTH;
		Render2D.roundedRect(
				guiGraphics,
				selectedX + 1.0F,
				toggleY + 1.0F,
				LAYOUT_TOGGLE_OPTION_WIDTH - 2.0F,
				LAYOUT_TOGGLE_HEIGHT - 2.0F,
				LAYOUT_TOGGLE_RADIUS - 1,
				alpha(ThemeManager.accent(), alpha)
		);
		renderLayoutToggleOption(guiGraphics, toggleX, toggleY, 0, 1.0F - layoutToggleProgress, alpha);
		renderLayoutToggleOption(guiGraphics, toggleX, toggleY, 1, layoutToggleProgress, alpha);
	}

	private static void renderLayoutToggleOption(GuiGraphics guiGraphics, int toggleX, int toggleY, int optionIndex, float selectedProgress, float alpha) {
		float optionX = toggleX + optionIndex * LAYOUT_TOGGLE_OPTION_WIDTH;
		float centerX = optionX + LAYOUT_TOGGLE_OPTION_WIDTH / 2.0F;
		float centerY = toggleY + LAYOUT_TOGGLE_HEIGHT / 2.0F;
		int iconColor = lerpColor(MODULE_DESCRIPTION_COLOR, ThemeManager.mutedAccent(), selectedProgress);
		if (optionIndex == 0) {
			renderLayoutGridIcon(guiGraphics, centerX, centerY, alpha(iconColor, alpha));
			return;
		}

		renderLayoutListIcon(guiGraphics, centerX, centerY, alpha(iconColor, alpha));
	}

	private static void renderLayoutGridIcon(GuiGraphics guiGraphics, float centerX, float centerY, int color) {
		float iconSize = LAYOUT_GRID_ICON_CELL_SIZE * 2.0F + LAYOUT_GRID_ICON_GAP;
		float startX = centerX - iconSize / 2.0F;
		float startY = centerY - iconSize / 2.0F;
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 2; column++) {
				Render2D.roundedRect(
						guiGraphics,
						startX + column * (LAYOUT_GRID_ICON_CELL_SIZE + LAYOUT_GRID_ICON_GAP),
						startY + row * (LAYOUT_GRID_ICON_CELL_SIZE + LAYOUT_GRID_ICON_GAP),
						LAYOUT_GRID_ICON_CELL_SIZE,
						LAYOUT_GRID_ICON_CELL_SIZE,
						LAYOUT_GRID_ICON_RADIUS,
						color
				);
			}
		}
	}

	private static void renderLayoutListIcon(GuiGraphics guiGraphics, float centerX, float centerY, int color) {
		float iconHeight = LAYOUT_LIST_ICON_HEIGHT * 3.0F + LAYOUT_LIST_ICON_GAP * 2.0F;
		float startY = Math.round(centerY - iconHeight / 2.0F);
		float startX = Math.round(centerX - LAYOUT_LIST_ICON_WIDTH / 2.0F);
		for (int row = 0; row < 3; row++) {
			Render2D.roundedRect(
					guiGraphics,
					startX,
					startY + row * (LAYOUT_LIST_ICON_HEIGHT + LAYOUT_LIST_ICON_GAP),
					LAYOUT_LIST_ICON_WIDTH,
					LAYOUT_LIST_ICON_HEIGHT,
					LAYOUT_LIST_ICON_RADIUS,
					color
			);
		}
	}

	private static int layoutToggleX(int left) {
		return searchBarX(left) - LAYOUT_TOGGLE_GAP - LAYOUT_TOGGLE_WIDTH;
	}

	private static int searchBarX(int left) {
		return left + PANEL_WIDTH - SEARCH_WIDTH - 10;
	}

	private static boolean searchCaretVisible() {
		return (Util.getMillis() / 500L) % 2L == 0L;
	}

	private static void drawInterText(GuiGraphics guiGraphics, String text, float x, float centerY, float size, int color) {
		float baselineY = LOGO_FONT.centeredBaseline(text, size, centerY);
		LOGO_FONT.draw(guiGraphics, text, x, baselineY, size, color);
	}

	private static void drawBoldInterText(GuiGraphics guiGraphics, String text, float x, float centerY, float size, int color) {
		float baselineY = LOGO_FONT.centeredBaseline(text, size, centerY);
		drawBoldInterTextAtBaseline(guiGraphics, text, x, baselineY, size, color);
	}

	private static void drawBoldInterTextAtBaseline(GuiGraphics guiGraphics, String text, float x, float baselineY, float size, int color) {
		LOGO_FONT.draw(guiGraphics, text, x, baselineY, size, color);
		LOGO_FONT.draw(guiGraphics, text, x + INTER_BOLD_OFFSET, baselineY, size, color);
	}

	private static void drawModuleTitleText(GuiGraphics guiGraphics, String text, float x, float centerY, float size, int color) {
		float baselineY = LOGO_FONT.centeredBaseline(text, size, centerY);
		LOGO_FONT.draw(guiGraphics, text, x, baselineY, size, color);
		LOGO_FONT.draw(guiGraphics, text, x + MODULE_TITLE_BOLD_OFFSET, baselineY, size, color);
	}

	private static void renderModuleDescription(GuiGraphics guiGraphics, String text, float x, float centerY, float maxWidth, int color) {
		List<String> lines = wrapToLines(text, MODULE_DESCRIPTION_SIZE, maxWidth, 2);
		for (int index = 0; index < lines.size(); index++) {
			drawBoldInterText(guiGraphics, lines.get(index), x, centerY + index * MODULE_DESCRIPTION_LINE_HEIGHT, MODULE_DESCRIPTION_SIZE, color);
		}
	}

	private static void drawRightAlignedInterText(GuiGraphics guiGraphics, String text, float right, float centerY, float size, int color) {
		float baselineY = LOGO_FONT.centeredBaseline(text, size, centerY);
		LOGO_FONT.draw(guiGraphics, text, right - LOGO_FONT.width(text, size), baselineY, size, color);
	}

	private static void drawRightAlignedBoldInterText(GuiGraphics guiGraphics, String text, float right, float centerY, float size, int color) {
		float x = right - LOGO_FONT.width(text, size) - INTER_BOLD_OFFSET;
		float baselineY = LOGO_FONT.centeredBaseline(text, size, centerY);
		LOGO_FONT.draw(guiGraphics, text, x, baselineY, size, color);
		LOGO_FONT.draw(guiGraphics, text, x + INTER_BOLD_OFFSET, baselineY, size, color);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (renamingHelperCustomSetting != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				renamingHelperCustomSetting = null;
				helperCustomRenameText = "";
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				renamingHelperCustomSetting.renameActiveProfile(helperCustomRenameText);
				renamingHelperCustomSetting = null;
				helperCustomRenameText = "";
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (!helperCustomRenameText.isEmpty()) {
					helperCustomRenameText = helperCustomRenameText.substring(0, helperCustomRenameText.length() - 1);
				}
				return true;
			}
		}
		if (friendInputFocus != FriendInputFocus.NONE) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				friendInputFocus = FriendInputFocus.NONE;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_TAB) {
				friendInputFocus = friendInputFocus == FriendInputFocus.NAME ? FriendInputFocus.ALIAS : FriendInputFocus.NAME;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				submitFriend();
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (friendInputFocus == FriendInputFocus.NAME && !friendNameInput.isEmpty()) {
					friendNameInput = friendNameInput.substring(0, friendNameInput.length() - 1);
				} else if (friendInputFocus == FriendInputFocus.ALIAS && !friendAliasInput.isEmpty()) {
					friendAliasInput = friendAliasInput.substring(0, friendAliasInput.length() - 1);
				}
				return true;
			}
		}
		if (bindingModule != null) {
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
				bindingModule.setKeyBind(-1);
			} else {
				bindingModule.setKeyBind(keyCode);
			}
			bindingModule = null;
			return true;
		}
		if (bindingSetting != null) {
			bindingSetting.setKey(keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
			bindingSetting = null;
			return true;
		}
		if (bindingHelperCustomSetting != null && bindingHelperCustomIndex >= 0) {
			bindingHelperCustomSetting.setKey(bindingHelperCustomIndex, keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
			bindingHelperCustomSetting = null;
			bindingHelperCustomIndex = -1;
			return true;
		}
		if (selectedIconGroup == IconGroup.CATEGORY && keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
			searchFocused = true;
			return true;
		}
		if (configInputFocused) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				configInputFocused = false;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (!configNameInput.isEmpty()) {
					configNameInput = configNameInput.substring(0, configNameInput.length() - 1);
				}
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				submitConfig();
				return true;
			}
		}
		if (friendInputFocus != FriendInputFocus.NONE && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (friendInputFocus == FriendInputFocus.NAME && !friendNameInput.isEmpty()) {
				friendNameInput = friendNameInput.substring(0, friendNameInput.length() - 1);
			} else if (friendInputFocus == FriendInputFocus.ALIAS && !friendAliasInput.isEmpty()) {
				friendAliasInput = friendAliasInput.substring(0, friendAliasInput.length() - 1);
			}
			return true;
		}
		if (searchFocused && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			searchFocused = false;
			return true;
		}
		if (customSwordSearchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (!customSwordSearch.isEmpty()) {
				customSwordSearch = customSwordSearch.substring(0, customSwordSearch.length() - 1);
				customSwordPanelScroll = 0;
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && colorPickerOpen) {
			closeColorPicker();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && customSwordPanelOpen) {
			closeCustomSwordPanel();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && moduleSettingsOpen) {
			closeModuleSettings();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			startClosing();
			return true;
		}
		if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (!searchText.isEmpty()) {
				searchText = searchText.substring(0, searchText.length() - 1);
			}
			closeModuleSettings();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (configInputFocused && !Character.isISOControl(codePoint)) {
			if (configNameInput.length() < 32 && (Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-' || codePoint == ' ')) {
				configNameInput += codePoint;
			}
			return true;
		}
		if (renamingHelperCustomSetting != null && !Character.isISOControl(codePoint)) {
			if (helperCustomRenameText.length() < 28) {
				helperCustomRenameText += codePoint;
			}
			return true;
		}
		if (customSwordSearchFocused && !Character.isISOControl(codePoint)) {
			if (customSwordSearch.length() < 36) {
				customSwordSearch += codePoint;
				customSwordPanelScroll = 0;
			}
			return true;
		}
		if (friendInputFocus != FriendInputFocus.NONE && !Character.isISOControl(codePoint)) {
			if (friendInputFocus == FriendInputFocus.NAME && friendNameInput.length() < 32) {
				friendNameInput += codePoint;
			} else if (friendInputFocus == FriendInputFocus.ALIAS && friendAliasInput.length() < 32) {
				friendAliasInput += codePoint;
			}
			return true;
		}
		if (!searchFocused || Character.isISOControl(codePoint)) {
			return super.charTyped(codePoint, modifiers);
		}

		searchText += codePoint;
		closeModuleSettings();
		return true;
	}

	@Override
	public boolean mouseClicked(double rawMouseX, double rawMouseY, int button) {
		double mouseX = scaleMouseX(rawMouseX);
		double mouseY = scaleMouseY(rawMouseY);
		if (closing) {
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isUtilityIconHovered(mouseX, mouseY, 0)) {
			if (colorPickerOpen && colorPickerSetting == ThemeManager.accentSetting()) {
				closeColorPicker();
			} else {
				openColorPicker(ThemeManager.accentSetting());
			}
			return true;
		}
		if (colorPickerOpen && !isColorPickerHovered(mouseX, mouseY)) {
			closeColorPicker();
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleColorPickerClick(mouseX, mouseY)) {
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleCustomSwordPanelClick(mouseX, mouseY)) {
			return true;
		}
		if (customSwordPanelOpen && !isCustomSwordPanelHovered(mouseX, mouseY)) {
			closeCustomSwordPanel();
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleModuleSettingsPanelClick(mouseX, mouseY)) {
			return true;
		}
		if (moduleSettingsOpen && !isModuleSettingsPanelHovered(mouseX, mouseY) && hoveredModuleLayout(mouseX, mouseY) == null) {
			closeModuleSettings();
		}

		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			int layoutToggleOption = selectedIconGroup == IconGroup.CATEGORY ? hoveredLayoutToggleOption(mouseX, mouseY) : -1;
			if (layoutToggleOption >= 0) {
				singleColumnModules = layoutToggleOption == 1;
				searchFocused = false;
				bindingModule = null;
				closeEnumDropdowns();
				return true;
			}

			searchFocused = selectedIconGroup == IconGroup.CATEGORY && isSearchHovered(mouseX, mouseY);
			if (searchFocused) {
				bindingModule = null;
				return true;
			}

			for (int index = 0; index < NAV_ICON_COUNT; index++) {
				if (isIconHovered(mouseX, mouseY, index)) {
					selectPage(IconGroup.CATEGORY, index);
					saveSelectedIcon();
					closeModuleSettings();
					bindingModule = null;
					friendInputFocus = FriendInputFocus.NONE;
					return true;
				}
			}
			for (int index = 1; index < UTILITY_ICON_COUNT; index++) {
				if (isUtilityIconHovered(mouseX, mouseY, index)) {
					selectPage(IconGroup.UTILITY, index);
					saveSelectedIcon();
					closeModuleSettings();
					bindingModule = null;
					friendInputFocus = FriendInputFocus.NONE;
					return true;
				}
			}
			if (handleFriendClick(mouseX, mouseY)) {
				return true;
			}
			if (handleConfigClick(mouseX, mouseY)) {
				return true;
			}
			if (handleSettingsClick(mouseX, mouseY)) {
				return true;
			}
			ModuleLayout hoveredLayout = hoveredModuleLayout(mouseX, mouseY);
			if (hoveredLayout != null) {
				if (handleModuleBadgeClick(hoveredLayout, mouseX, mouseY)) {
					return true;
				}
				float settingsIconCenterX = hoveredLayout.x() + hoveredLayout.width() - 11.0F;
				if (Math.abs(mouseX - settingsIconCenterX) <= 8.0F && !hoveredLayout.module().settings().isEmpty()) {
					Module clickedModule = hoveredLayout.module();
					if (moduleSettingsOpen && activeModuleSettings == clickedModule) {
						closeModuleSettings();
					} else {
						openModuleSettings(clickedModule);
					}
					return true;
				}
				hoveredLayout.module().toggle();
				return true;
			}
		}

		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			ModuleLayout hoveredLayout = hoveredModuleLayout(mouseX, mouseY);
			if (hoveredLayout != null && !hoveredLayout.module().settings().isEmpty()) {
				Module clickedModule = hoveredLayout.module();
				if (moduleSettingsOpen && activeModuleSettings == clickedModule) {
					closeModuleSettings();
				} else {
					openModuleSettings(clickedModule);
				}
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleFriendClick(double mouseX, double mouseY) {
		if (!friendsPageSelected()) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float contentX = left + SIDEBAR_WIDTH + MODULE_AREA_PADDING_X;
		float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - MODULE_AREA_PADDING_X * 2.0F;
		float inputY = top + TOPBAR_HEIGHT + 42.0F;
		float inputGap = 8.0F;
		float buttonWidth = 76.0F;
		float inputWidth = (contentWidth - buttonWidth - inputGap * 2.0F) / 2.0F;
		if (isInside(mouseX, mouseY, contentX, inputY, inputWidth, FRIEND_INPUT_HEIGHT)) {
			if (editingFriendName == null) {
				friendInputFocus = FriendInputFocus.NAME;
			}
			return true;
		}
		float aliasX = contentX + inputWidth + inputGap;
		if (isInside(mouseX, mouseY, aliasX, inputY, inputWidth, FRIEND_INPUT_HEIGHT)) {
			friendInputFocus = FriendInputFocus.ALIAS;
			return true;
		}
		float buttonX = contentX + inputWidth * 2.0F + inputGap * 2.0F;
		if (isInside(mouseX, mouseY, buttonX, inputY, buttonWidth, FRIEND_INPUT_HEIGHT)) {
			submitFriend();
			return true;
		}

		float listY = inputY + FRIEND_INPUT_HEIGHT + 18.0F;
		List<FriendManager.FriendEntry> friends = FriendManager.friends();
		friendScroll = Math.max(0, Math.min(friendScroll, Math.max(0, friends.size() - 6)));
		int maxRows = Math.min(friends.size() - friendScroll, 6);
		for (int row = 0; row < maxRows; row++) {
			FriendManager.FriendEntry friend = friends.get(friendScroll + row);
			float rowY = listY + row * (FRIEND_ROW_HEIGHT + FRIEND_ROW_GAP);
			if (!isInside(mouseX, mouseY, contentX, rowY, contentWidth, FRIEND_ROW_HEIGHT)) {
				continue;
			}
			if (isInside(mouseX, mouseY, contentX + contentWidth - 32.0F, rowY, 32.0F, FRIEND_ROW_HEIGHT)) {
				FriendManager.remove(friend.name());
				if (friend.name().equalsIgnoreCase(friendNameInput)) {
					resetFriendForm();
				}
			} else {
				editingFriendName = friend.name();
				friendNameInput = friend.name();
				friendAliasInput = friend.alias();
				friendInputFocus = FriendInputFocus.ALIAS;
			}
			return true;
		}

		friendInputFocus = FriendInputFocus.NONE;
		return true;
	}

	private void submitFriend() {
		String name = editingFriendName == null ? friendNameInput : editingFriendName;
		if (FriendManager.addOrUpdate(name, friendAliasInput)) {
			resetFriendForm();
			friendInputFocus = FriendInputFocus.NAME;
		}
	}

	private void resetFriendForm() {
		editingFriendName = null;
		friendNameInput = "";
		friendAliasInput = "";
	}

	private boolean friendsPageSelected() {
		return selectedIconGroup == IconGroup.UTILITY && selectedUtilityIconIndex == FRIENDS_UTILITY_INDEX;
	}

	@Override
	public boolean mouseScrolled(double rawMouseX, double rawMouseY, double scrollX, double scrollY) {
		double mouseX = scaleMouseX(rawMouseX);
		double mouseY = scaleMouseY(rawMouseY);
		if (customSwordPanelOpen && isCustomSwordPanelHovered(mouseX, mouseY)) {
			int maxScroll = maxCustomSwordPanelScroll(filteredCustomSwordModels());
			customSwordPanelScroll = Math.max(0, Math.min(maxScroll, customSwordPanelScroll - (int) Math.signum(scrollY)));
			return true;
		}
		if (colorPickerOpen && isColorPickerHovered(mouseX, mouseY)) {
			return true;
		}
		if (moduleSettingsOpen && isModuleSettingsPanelHovered(mouseX, mouseY)) {
			float maxScroll = maxModuleSettingsScroll(activeModuleSettings);
			targetModuleSettingsScroll = Math.max(0.0F, Math.min(maxScroll, targetModuleSettingsScroll - (float) scrollY * MODULE_SETTINGS_SCROLL_STEP));
			return true;
		}
		if (friendsPageSelected()) {
			int maxScroll = Math.max(0, FriendManager.friends().size() - 6);
			targetFriendScroll = Math.max(0, Math.min(maxScroll, targetFriendScroll - (float) scrollY * 1.5F));
			return true;
		}
		if (configsPageSelected()) {
			int maxScroll = Math.max(0, dev.Mardexx.client.config.ClientConfig.listConfigs().size() - 6);
			configScroll = Math.max(0, Math.min(maxScroll, configScroll - (int) Math.signum(scrollY)));
			return true;
		}

		return scrollModuleList(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean scrollModuleList(double mouseX, double mouseY, double scrollY) {
		if (selectedIconGroup != IconGroup.CATEGORY || scrollY == 0.0D) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int areaX = moduleAreaX(left);
		int areaY = moduleAreaY(top);
		int areaWidth = moduleAreaWidth();
		int areaHeight = moduleAreaHeight();
		if (!isInside(mouseX, mouseY, areaX, areaY, areaWidth, areaHeight)) {
			return false;
		}

		float maxScroll = maxModuleScroll(left, top);
		if (maxScroll <= 0.0F) {
			return false;
		}

		targetModuleScrollOffset = Math.max(0.0F, Math.min(maxScroll, targetModuleScrollOffset - (float) scrollY * MODULE_LIST_SCROLL_STEP));
		return true;
	}

	@Override
	public boolean mouseDragged(double rawMouseX, double rawMouseY, int button, double dragX, double dragY) {
		double mouseX = scaleMouseX(rawMouseX);
		double mouseY = scaleMouseY(rawMouseY);
		if (draggedSettingsControl != SettingsDrag.NONE) {
			return updateSettingsDrag(mouseX);
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedColorPickerControl != null) {
			return updateColorPickerFromMouse(mouseX, mouseY);
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedRangeSetting != null && draggedRangeHandle != null) {
			return updateDraggedRange(mouseX);
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedSliderSetting != null) {
			return updateDraggedSlider(mouseX);
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double rawMouseX, double rawMouseY, int button) {
		double mouseX = scaleMouseX(rawMouseX);
		double mouseY = scaleMouseY(rawMouseY);
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedSettingsControl != SettingsDrag.NONE) {
			draggedSettingsControl = SettingsDrag.NONE;
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedColorPickerControl != null) {
			draggedColorPickerControl = null;
			if (colorPickerSetting == ThemeManager.accentSetting()) {
				ThemeManager.save();
			}
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedRangeSetting != null) {
			draggedRangeSetting = null;
			draggedRangeHandle = null;
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedSliderSetting != null) {
			draggedSliderSetting = null;
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	private boolean isModuleSettingsPanelHovered(double mouseX, double mouseY) {
		if (activeModuleSettings == null || moduleSettingsProgress <= 0.01F) {
			return false;
		}
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = moduleSettingsPanelX(left);
		float panelY = moduleSettingsPanelY(top);
		float r = MODULE_SETTINGS_FILLET_RADIUS;
		return mouseX >= panelX && mouseX <= panelX + MODULE_SETTINGS_PANEL_WIDTH
				&& mouseY >= panelY - r && mouseY <= panelY + MODULE_SETTINGS_PANEL_HEIGHT + r;
	}

	private boolean handleModuleSettingsPanelClick(double mouseX, double mouseY) {
		if (!moduleSettingsOpen || activeModuleSettings == null || moduleSettingsProgress <= 0.01F) {
			return false;
		}
		if (!isModuleSettingsPanelHovered(mouseX, mouseY)) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = moduleSettingsPanelX(left);
		float panelY = moduleSettingsPanelY(top);
		float width = MODULE_SETTINGS_PANEL_WIDTH;

		if (isInside(mouseX, mouseY, panelX + width - 24.0F, panelY + 4.0F, 20.0F, 20.0F)) {
			closeModuleSettings();
			return true;
		}

		int settingX = Math.round(panelX + MODULE_SETTINGS_PANEL_PADDING);
		int settingWidth = Math.round(width - MODULE_SETTINGS_PANEL_PADDING * 2.0F);
		float contentTop = panelY + 38.0F;
		float settingY = contentTop - moduleSettingsScroll;

		for (Setting<?> setting : orderedSettings(activeModuleSettings)) {
			float rowHeight = settingRowHeight(setting, settingWidth);
			float controlWidth = settingControlWidth(setting, settingWidth);
			if (setting instanceof HelperCustomItemsSetting customItemsSetting) {
				if (handleHelperCustomItemsClick(customItemsSetting, settingX, settingY + 25.0F, settingWidth, mouseX, mouseY)) {
					return true;
				}
			}
			if (setting instanceof CustomSwordSetting customSwordSetting) {
				CustomSwords.Target toggleTarget = hoveredCustomSwordToggleTarget(settingX, settingY + 25.0F, settingWidth, mouseX, mouseY);
				if (toggleTarget != null) {
					customSwordSetting.toggleEnabled(toggleTarget);
					return true;
				}
				CustomSwords.Target target = hoveredCustomSwordTarget(settingX, settingY + 25.0F, settingWidth, mouseX, mouseY);
				if (target != null) {
					openCustomSwordPanel(customSwordSetting, target);
					return true;
				}
			}
			if (setting instanceof ColorSetting colorSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingTopControlCenterY(setting, settingY);
				if (isInside(mouseX, mouseY, controlX, controlCenterY - 9.0F, controlWidth, 18.0F)) {
					openColorPicker(colorSetting);
					return true;
				}
			}
			if (setting instanceof EnumSetting<?> enumSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingTopControlCenterY(setting, settingY);
				float dropdownY = enumDropdownY(controlCenterY);
				if (isEnumDropdownOpen(enumSetting) && isInside(mouseX, mouseY, controlX, dropdownY, controlWidth, enumDropdownHeight(enumSetting))) {
					int optionIndex = Math.max(0, Math.min(enumSetting.values().length - 1, (int) ((mouseY - dropdownY) / MODULE_ENUM_OPTION_HEIGHT)));
					setEnumValue(enumSetting, enumSetting.values()[optionIndex]);
					return true;
				}
				if (isInside(mouseX, mouseY, controlX, controlCenterY - 9.0F, controlWidth, 18)) {
					toggleEnumDropdown(enumSetting);
					return true;
				}
			}

			if (setting instanceof EnumBooleanMapSetting<?> mapSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingTopControlCenterY(setting, settingY);
				float dropdownY = enumDropdownY(controlCenterY);
				if (isEnumDropdownOpen(mapSetting) && isInside(mouseX, mouseY, controlX, dropdownY, controlWidth, enumDropdownHeight(mapSetting))) {
					Enum<?>[] values = enumBooleanValues(mapSetting);
					int optionIndex = Math.max(0, Math.min(values.length - 1, (int) ((mouseY - dropdownY) / MODULE_ENUM_OPTION_HEIGHT)));
					Enum<?> value = values[optionIndex];
					boolean enabled = Boolean.TRUE.equals(mapSetting.value().get(value));
					setEnumBooleanValue(mapSetting, value, !enabled);
					return true;
				}
				if (isInside(mouseX, mouseY, controlX, controlCenterY - 9.0F, controlWidth, 18)) {
					if (handleMultiselectTagRemove(mapSetting, controlX, controlCenterY, controlWidth, mouseX, mouseY)) {
						return true;
					}

					toggleEnumDropdown(mapSetting);
					return true;
				}
			}

			if (setting instanceof FloatRangeSetting rangeSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingY + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET;
				if (handleRangeClick(rangeSetting, controlX, controlCenterY, controlWidth, mouseX, mouseY)) {
					return true;
				}
			}

			if (setting instanceof FloatSetting floatSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingY + MODULE_SETTING_DESCRIPTION_FIRST_CENTER_OFFSET;
				if (handleSliderClick(floatSetting, controlX, controlCenterY, controlWidth, mouseX, mouseY)) {
					return true;
				}
			}

			if (setting instanceof BooleanSetting booleanSetting) {
				float checkboxX = settingX + settingWidth - 14.0F;
				float checkboxY = settingCheckboxY(setting, settingY);
				if (isInside(mouseX, mouseY, checkboxX, checkboxY, 14, 14)) {
					booleanSetting.setValue(!booleanSetting.enabled());
					return true;
				}
			}

			if (setting instanceof ItemStackSetting itemStackSetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingTopControlCenterY(setting, settingY);
				if (isInside(mouseX, mouseY, controlX, controlCenterY - 9.0F, controlWidth, 18.0F)) {
					if (minecraft != null && minecraft.player != null) {
						minecraft.setScreen(new TripleItemPickerScreen(this, itemStackSetting));
					}
					return true;
				}
			}

			if (setting instanceof KeySetting keySetting) {
				float controlX = settingX + settingWidth - controlWidth;
				float controlCenterY = settingTopControlCenterY(setting, settingY);
				if (isInside(mouseX, mouseY, controlX, controlCenterY - 9.0F, controlWidth, 18.0F)) {
					bindingSetting = bindingSetting == keySetting ? null : keySetting;
					bindingModule = null;
					searchFocused = false;
					closeEnumDropdowns();
					return true;
				}
			}

			if (isInside(mouseX, mouseY, settingX, settingY, settingWidth, rowHeight)) {
				return true;
			}
			settingY += rowHeight;
		}

		return true;
	}

	private boolean handleRangeClick(FloatRangeSetting setting, float controlX, float controlCenterY, float controlWidth, double mouseX, double mouseY) {
		float sliderX = rangeSliderX(controlX);
		float sliderWidth = rangeSliderWidth(controlWidth);
		if (!isInside(
				mouseX,
				mouseY,
				sliderX - MODULE_RANGE_HANDLE_SIZE / 2.0F,
				controlCenterY - 9.0F,
				sliderWidth + MODULE_RANGE_HANDLE_SIZE,
				18.0F
		)) {
			return false;
		}

		draggedRangeSetting = setting;
		draggedRangeHandle = nearestRangeHandle(setting, sliderX, sliderWidth, mouseX);
		updateRangeSetting(setting, draggedRangeHandle, sliderX, sliderWidth, mouseX);
		return true;
	}

	private boolean handleHelperCustomItemsClick(HelperCustomItemsSetting setting, float x, float y, int width, double mouseX, double mouseY) {
		float buttonGap = 4.0F;
		float arrowWidth = 20.0F;
		float actionWidth = 54.0F;
		float deleteWidth = 24.0F;
		float profileNameX = x + arrowWidth + buttonGap;
		float profileNameWidth = Math.max(46.0F, width - arrowWidth * 2.0F - actionWidth * 2.0F - deleteWidth - buttonGap * 6.0F);
		float nextX = profileNameX + profileNameWidth + buttonGap;
		float addProfileX = nextX + arrowWidth + buttonGap;
		float renameX = addProfileX + actionWidth + buttonGap;
		float deleteX = renameX + actionWidth + buttonGap;

		if (isInside(mouseX, mouseY, x, y, arrowWidth, HELPER_CUSTOM_PROFILE_HEIGHT)) {
			setting.selectProfile(setting.activeIndex() - 1);
			clearHelperCustomEditing();
			return true;
		}
		if (isInside(mouseX, mouseY, profileNameX, y, profileNameWidth, HELPER_CUSTOM_PROFILE_HEIGHT)
				|| isInside(mouseX, mouseY, renameX, y, actionWidth, HELPER_CUSTOM_PROFILE_HEIGHT)) {
			startHelperCustomRename(setting);
			return true;
		}
		if (isInside(mouseX, mouseY, nextX, y, arrowWidth, HELPER_CUSTOM_PROFILE_HEIGHT)) {
			setting.selectProfile(setting.activeIndex() + 1);
			clearHelperCustomEditing();
			return true;
		}
		if (isInside(mouseX, mouseY, addProfileX, y, actionWidth, HELPER_CUSTOM_PROFILE_HEIGHT)) {
			setting.addProfile();
			clearHelperCustomEditing();
			return true;
		}
		if (isInside(mouseX, mouseY, deleteX, y, deleteWidth, HELPER_CUSTOM_PROFILE_HEIGHT)) {
			setting.removeActiveProfile();
			clearHelperCustomEditing();
			return true;
		}

		y += HELPER_CUSTOM_PROFILE_HEIGHT + HELPER_CUSTOM_ITEM_GAP;
		if (isInside(mouseX, mouseY, x, y, 84.0F, HELPER_CUSTOM_ADD_HEIGHT)) {
			if (minecraft != null && minecraft.player != null) {
				minecraft.setScreen(new HelperCustomItemPickerScreen(this, setting));
			}
			return true;
		}

		float itemY = y + HELPER_CUSTOM_ADD_HEIGHT + HELPER_CUSTOM_ITEM_GAP;
		List<HelperCustomItemsSetting.Entry> entries = setting.entries();
		for (int index = 0; index < entries.size(); index++) {
			float rowY = itemY + index * (HELPER_CUSTOM_ITEM_HEIGHT + HELPER_CUSTOM_ITEM_GAP);
			if (!isInside(mouseX, mouseY, x, rowY, width, HELPER_CUSTOM_ITEM_HEIGHT)) {
				continue;
			}

			if (isInside(mouseX, mouseY, x + width - 24.0F, rowY, 24.0F, HELPER_CUSTOM_ITEM_HEIGHT)) {
				setting.remove(index);
				if (bindingHelperCustomSetting == setting && bindingHelperCustomIndex == index) {
					bindingHelperCustomSetting = null;
					bindingHelperCustomIndex = -1;
				}
				return true;
			}

			float keyWidth = 34.0F;
			float keyX = x + width - keyWidth - 24.0F;
			if (isInside(mouseX, mouseY, keyX, rowY + 4.0F, keyWidth, 16.0F)) {
				bindingHelperCustomSetting = setting;
				bindingHelperCustomIndex = index;
				bindingSetting = null;
				bindingModule = null;
				return true;
			}

			return true;
		}

		return isInside(mouseX, mouseY, x, y, width, settingBaseRowHeight(setting, width) - 25.0F);
	}

	private void startHelperCustomRename(HelperCustomItemsSetting setting) {
		renamingHelperCustomSetting = setting;
		helperCustomRenameText = setting.activeName();
		bindingHelperCustomSetting = null;
		bindingHelperCustomIndex = -1;
		bindingSetting = null;
		bindingModule = null;
	}

	private void clearHelperCustomEditing() {
		renamingHelperCustomSetting = null;
		helperCustomRenameText = "";
		bindingHelperCustomSetting = null;
		bindingHelperCustomIndex = -1;
	}

	private boolean handleSliderClick(FloatSetting setting, float controlX, float controlCenterY, float controlWidth, double mouseX, double mouseY) {
		float sliderX = controlX;
		float sliderWidth = sliderWidth(controlWidth);
		if (!isInside(
				mouseX,
				mouseY,
				sliderX - MODULE_RANGE_HANDLE_SIZE / 2.0F,
				controlCenterY - 9.0F,
				sliderWidth + MODULE_RANGE_HANDLE_SIZE,
				18.0F
		)) {
			return false;
		}

		draggedSliderSetting = setting;
		updateSliderSetting(setting, sliderX, sliderWidth, mouseX);
		return true;
	}

	private static CustomSwords.Target hoveredCustomSwordTarget(float x, float y, int width, double mouseX, double mouseY) {
		CustomSwords.Target[] targets = CustomSwords.Target.values();
		float tabWidth = (width - (targets.length - 1) * CUSTOM_SWORD_TAB_GAP) / targets.length;
		for (int index = 0; index < targets.length; index++) {
			float tabX = x + index * (tabWidth + CUSTOM_SWORD_TAB_GAP);
			if (isInside(mouseX, mouseY, tabX, y, tabWidth, CUSTOM_SWORD_TAB_HEIGHT)) {
				return targets[index];
			}
		}
		return null;
	}

	private static CustomSwords.Target hoveredCustomSwordToggleTarget(float x, float y, int width, double mouseX, double mouseY) {
		CustomSwords.Target[] targets = CustomSwords.Target.values();
		float tabWidth = (width - (targets.length - 1) * CUSTOM_SWORD_TAB_GAP) / targets.length;
		for (int index = 0; index < targets.length; index++) {
			float tabX = x + index * (tabWidth + CUSTOM_SWORD_TAB_GAP);
			float checkX = tabX + tabWidth / 2.0F - 5.0F;
			float checkY = y + CUSTOM_SWORD_TAB_HEIGHT - 13.0F;
			if (isInside(mouseX, mouseY, checkX - 2.0F, checkY - 2.0F, 14.0F, 14.0F)) {
				return targets[index];
			}
		}
		return null;
	}

	private void updateCustomSwordPanelAnimation(float frameDelta) {
		if (customSwordSetting != null && (expandedModule == null || !expandedModule.settings().contains(customSwordSetting))) {
			closeCustomSwordPanel();
		}

		float target = customSwordPanelOpen ? 1.0F : 0.0F;
		customSwordPanelProgress += (target - customSwordPanelProgress) * Math.min(1.0F, COLOR_PICKER_ANIMATION_SPEED * frameDelta);
		if (Math.abs(customSwordPanelProgress - target) < 0.01F) {
			customSwordPanelProgress = target;
		}
		if (!customSwordPanelOpen && customSwordPanelProgress <= 0.0F) {
			customSwordSetting = null;
			customSwordTarget = null;
			customSwordPanelScroll = 0;
		}
	}

	private void openCustomSwordPanel(CustomSwordSetting setting, CustomSwords.Target target) {
		boolean targetChanged = customSwordTarget != target || customSwordSetting != setting;
		customSwordSetting = setting;
		customSwordTarget = target;
		customSwordPanelOpen = true;
		if (targetChanged) {
			customSwordSearch = "";
			customSwordPanelScroll = scrollForSelectedModel(setting.model(target), CustomSwords.modelList());
		}
		closeColorPicker();
		closeEnumDropdowns();
	}

	private void closeCustomSwordPanel() {
		customSwordPanelOpen = false;
		customSwordSearchFocused = false;
	}

	private static int scrollForSelectedModel(String model, List<String> models) {
		int index = models.indexOf(model);
		if (index < 0) {
			return 0;
		}
		return Math.max(0, Math.min(index / CUSTOM_SWORD_MODEL_COLUMNS, maxCustomSwordPanelScroll(models)));
	}

	private boolean handleCustomSwordPanelClick(double mouseX, double mouseY) {
		if (customSwordSetting == null || customSwordTarget == null || customSwordPanelProgress <= 0.01F) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = customSwordPanelX(left);
		float panelY = customSwordPanelY(top);
		if (!isInside(mouseX, mouseY, panelX, panelY, CUSTOM_SWORD_PANEL_WIDTH, CUSTOM_SWORD_PANEL_HEIGHT)) {
			return false;
		}

		if (isInside(mouseX, mouseY, panelX + CUSTOM_SWORD_PANEL_WIDTH - 28.0F, panelY + 4.0F, 20.0F, 20.0F)) {
			closeCustomSwordPanel();
			return true;
		}

		float fieldX = panelX + COLOR_PICKER_PADDING;
		float fieldWidth = CUSTOM_SWORD_PANEL_WIDTH - COLOR_PICKER_PADDING * 2.0F;
		float searchY = panelY + 70.0F;
		if (isInside(mouseX, mouseY, fieldX, searchY, fieldWidth, CUSTOM_SWORD_SEARCH_HEIGHT)) {
			customSwordSearchFocused = true;
			return true;
		}
		customSwordSearchFocused = false;

		float gridY = panelY + 98.0F;
		float cellWidth = customSwordModelCellWidth(fieldWidth);
		float gridHeight = CUSTOM_SWORD_MODEL_VISIBLE_ROWS * CUSTOM_SWORD_MODEL_CELL_HEIGHT + (CUSTOM_SWORD_MODEL_VISIBLE_ROWS - 1) * CUSTOM_SWORD_MODEL_CELL_GAP;
		if (isInside(mouseX, mouseY, fieldX, gridY, fieldWidth, gridHeight)) {
			int column = Math.max(0, Math.min(CUSTOM_SWORD_MODEL_COLUMNS - 1, (int) ((mouseX - fieldX) / (cellWidth + CUSTOM_SWORD_MODEL_CELL_GAP))));
			int row = Math.max(0, Math.min(CUSTOM_SWORD_MODEL_VISIBLE_ROWS - 1, (int) ((mouseY - gridY) / (CUSTOM_SWORD_MODEL_CELL_HEIGHT + CUSTOM_SWORD_MODEL_CELL_GAP))));
			float cellX = fieldX + column * (cellWidth + CUSTOM_SWORD_MODEL_CELL_GAP);
			float cellY = gridY + row * (CUSTOM_SWORD_MODEL_CELL_HEIGHT + CUSTOM_SWORD_MODEL_CELL_GAP);
			if (!isInside(mouseX, mouseY, cellX, cellY, cellWidth, CUSTOM_SWORD_MODEL_CELL_HEIGHT)) {
				return true;
			}

			int optionIndex = (customSwordPanelScroll + row) * CUSTOM_SWORD_MODEL_COLUMNS + column;
			List<String> models = filteredCustomSwordModels();
			if (optionIndex >= 0 && optionIndex < models.size()) {
				customSwordSetting.setModel(customSwordTarget, models.get(optionIndex));
			}
		}

		return true;
	}

	private boolean isCustomSwordPanelHovered(double mouseX, double mouseY) {
		if (customSwordSetting == null || customSwordTarget == null || customSwordPanelProgress <= 0.01F) {
			return false;
		}
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = customSwordPanelX(left);
		float panelY = customSwordPanelY(top);
		float r = COLOR_PICKER_FILLET_RADIUS;
		return mouseX >= panelX && mouseX <= panelX + CUSTOM_SWORD_PANEL_WIDTH
				&& mouseY >= panelY - r && mouseY <= panelY + CUSTOM_SWORD_PANEL_HEIGHT + r;
	}

	private float customSwordPanelX(int left) {
		float targetX = left + PANEL_WIDTH;
		float slideOffset = (CUSTOM_SWORD_PANEL_WIDTH + 2.0F) * (1.0F - ease(customSwordPanelProgress));
		return targetX - slideOffset;
	}

	private static float customSwordPanelY(int top) {
		return top + (PANEL_HEIGHT - CUSTOM_SWORD_PANEL_HEIGHT) / 2.0F;
	}

	private void openModuleSettings(Module module) {
		activeModuleSettings = module;
		expandedModule = module;
		moduleSettingsOpen = true;
		targetModuleSettingsScroll = 0.0F;
		moduleSettingsScroll = 0.0F;
		closeColorPicker();
		closeCustomSwordPanel();
		closeEnumDropdowns();
	}

	private void closeModuleSettings() {
		moduleSettingsOpen = false;
		closeEnumDropdowns();
	}

	private void updateModuleSettingsAnimation(float frameDelta) {
		if (closing && moduleSettingsOpen) {
			moduleSettingsOpen = false;
		}

		float target = (moduleSettingsOpen && !closing) ? 1.0F : 0.0F;
		moduleSettingsProgress += (target - moduleSettingsProgress) * Math.min(1.0F, COLOR_PICKER_ANIMATION_SPEED * frameDelta);
		if (Math.abs(moduleSettingsProgress - target) < 0.01F) {
			moduleSettingsProgress = target;
		}
		if (!moduleSettingsOpen && moduleSettingsProgress <= 0.0F) {
			activeModuleSettings = null;
			expandedModule = null;
		}

		if (activeModuleSettings != null) {
			float maxScroll = maxModuleSettingsScroll(activeModuleSettings);
			targetModuleSettingsScroll = Math.max(0.0F, Math.min(maxScroll, targetModuleSettingsScroll));
			moduleSettingsScroll += (targetModuleSettingsScroll - moduleSettingsScroll) * Math.min(1.0F, 0.26F * frameDelta);
			if (Math.abs(moduleSettingsScroll - targetModuleSettingsScroll) < 0.05F) {
				moduleSettingsScroll = targetModuleSettingsScroll;
			}
		}
	}

	private void updateColorPickerAnimation(float frameDelta) {
		if (colorPickerSetting != null
				&& colorPickerSetting != ThemeManager.accentSetting()
				&& (expandedModule == null || !expandedModule.settings().contains(colorPickerSetting))) {
			closeColorPicker();
		}

		float target = colorPickerOpen ? 1.0F : 0.0F;
		colorPickerProgress += (target - colorPickerProgress) * Math.min(1.0F, COLOR_PICKER_ANIMATION_SPEED * frameDelta);
		if (Math.abs(colorPickerProgress - target) < 0.01F) {
			colorPickerProgress = target;
		}
		if (!colorPickerOpen && colorPickerProgress <= 0.0F) {
			colorPickerSetting = null;
		}
	}

	private void openColorPicker(ColorSetting setting) {
		colorPickerSetting = setting;
		colorPickerOpen = true;
		draggedColorPickerControl = null;
		colorPickerHues.put(setting, rgbToHsv(setting.color()).hue());
		closeCustomSwordPanel();
		closeEnumDropdowns();
	}

	private void closeColorPicker() {
		if (colorPickerSetting == ThemeManager.accentSetting()) {
			ThemeManager.save();
		}
		colorPickerOpen = false;
		draggedColorPickerControl = null;
	}

	private boolean handleColorPickerClick(double mouseX, double mouseY) {
		if (colorPickerSetting == null || colorPickerProgress <= 0.01F) {
			return false;
		}
		if (!isColorPickerHovered(mouseX, mouseY)) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = colorPickerX(left);
		float panelY = colorPickerY(top);
		if (isInside(mouseX, mouseY, panelX + COLOR_PICKER_WIDTH - 24.0F, panelY + 4.0F, 20.0F, 20.0F)) {
			closeColorPicker();
			return true;
		}

		float fieldX = panelX + COLOR_PICKER_PADDING;
		float fieldY = panelY + 40.0F;
		float fieldWidth = COLOR_PICKER_WIDTH - COLOR_PICKER_PADDING * 2.0F;
		float hueY = fieldY + COLOR_FIELD_HEIGHT + 10.0F;
		boolean insideHue = isInside(mouseX, mouseY, fieldX - 2.0F, hueY - 2.0F, fieldWidth + 4.0F, COLOR_HUE_HEIGHT + 4.0F);
		boolean insideField = isInside(mouseX, mouseY, fieldX - 2.0F, fieldY - 2.0F, fieldWidth + 4.0F, COLOR_FIELD_HEIGHT + 4.0F);

		if (!insideHue && !insideField) {
			return true;
		}

		draggedColorPickerControl = insideHue ? ColorPickerDrag.HUE : ColorPickerDrag.SATURATION_VALUE;
		updateColorPickerFromMouse(mouseX, mouseY);
		return true;
	}

	private boolean updateColorPickerFromMouse(double mouseX, double mouseY) {
		if (colorPickerSetting == null || draggedColorPickerControl == null) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = colorPickerX(left);
		float panelY = colorPickerY(top);
		float fieldX = panelX + COLOR_PICKER_PADDING;
		float fieldY = panelY + 40.0F;
		float fieldWidth = COLOR_PICKER_WIDTH - COLOR_PICKER_PADDING * 2.0F;
		float hueY = fieldY + COLOR_FIELD_HEIGHT + 10.0F;
		Hsv hsv = colorPickerHsv();

		if (draggedColorPickerControl == ColorPickerDrag.HUE) {
			float hue = clamp01((float) ((mouseX - fieldX) / fieldWidth));
			colorPickerHues.put(colorPickerSetting, hue);
			colorPickerSetting.setColor(hsvToRgb(hue, hsv.saturation(), hsv.value()));
			return true;
		}

		float saturation = clamp01((float) ((mouseX - fieldX) / fieldWidth));
		float value = 1.0F - clamp01((float) ((mouseY - fieldY) / COLOR_FIELD_HEIGHT));
		colorPickerSetting.setColor(hsvToRgb(hsv.hue(), saturation, value));
		return true;
	}

	private boolean isColorPickerHovered(double mouseX, double mouseY) {
		if (colorPickerSetting == null || colorPickerProgress <= 0.01F) {
			return false;
		}
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		float panelX = colorPickerX(left);
		float panelY = colorPickerY(top);
		float r = COLOR_PICKER_FILLET_RADIUS;
		return mouseX >= panelX && mouseX <= panelX + COLOR_PICKER_WIDTH
				&& mouseY >= panelY - r && mouseY <= panelY + COLOR_PICKER_HEIGHT + r;
	}

	private float colorPickerX(int left) {
		float targetX = left + PANEL_WIDTH;
		float slideOffset = (COLOR_PICKER_WIDTH + 2.0F) * (1.0F - ease(colorPickerProgress));
		return targetX - slideOffset;
	}

	private static float colorPickerY(int top) {
		return top + (PANEL_HEIGHT - COLOR_PICKER_HEIGHT) / 2.0F;
	}

	private Hsv colorPickerHsv() {
		Hsv current = rgbToHsv(colorPickerSetting.color());
		float hue = colorPickerHues.getOrDefault(colorPickerSetting, current.hue());
		return new Hsv(hue, current.saturation(), current.value());
	}

	private boolean updateDraggedRange(double mouseX) {
		if (!moduleSettingsOpen || activeModuleSettings == null || draggedRangeSetting == null || draggedRangeHandle == null) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		float panelX = moduleSettingsPanelX(left);
		int settingWidth = Math.round(MODULE_SETTINGS_PANEL_WIDTH - MODULE_SETTINGS_PANEL_PADDING * 2.0F);
		float controlWidth = settingControlWidth(draggedRangeSetting, settingWidth);
		float controlX = panelX + MODULE_SETTINGS_PANEL_PADDING + settingWidth - controlWidth;
		updateRangeSetting(draggedRangeSetting, draggedRangeHandle, rangeSliderX(controlX), rangeSliderWidth(controlWidth), mouseX);
		return true;
	}

	private boolean handleModuleBadgeClick(ModuleLayout layout, double mouseX, double mouseY) {
		if (mouseY > layout.y() + MODULE_CARD_HEIGHT) {
			return false;
		}

		float settingsIconCenterX = layout.x() + layout.width() - 11.0F;
		float toggleSwitchRight = settingsIconCenterX - 9.0F;
		float toggleSwitchLeft = toggleSwitchRight - MODULE_TOGGLE_SWITCH_WIDTH;
		float badgeRight = toggleSwitchLeft - 5.0F;
		String text = moduleBindText(layout.module());
		int badgeWidth = moduleBadgeWidth(text);
		float headerCenterY = layout.y() + MODULE_CARD_HEIGHT / 2.0F;
		float badgeX = badgeRight - badgeWidth;
		float badgeY = headerCenterY - MODULE_BADGE_HEIGHT / 2.0F;
		if (!isInside(mouseX, mouseY, badgeX, badgeY, badgeWidth, MODULE_BADGE_HEIGHT)) {
			return false;
		}

		bindingModule = bindingModule == layout.module() ? null : layout.module();
		searchFocused = false;
		closeEnumDropdowns();
		return true;
	}

	private boolean updateDraggedSlider(double mouseX) {
		if (!moduleSettingsOpen || activeModuleSettings == null || draggedSliderSetting == null) {
			return false;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		float panelX = moduleSettingsPanelX(left);
		int settingWidth = Math.round(MODULE_SETTINGS_PANEL_WIDTH - MODULE_SETTINGS_PANEL_PADDING * 2.0F);
		float controlWidth = settingControlWidth(draggedSliderSetting, settingWidth);
		float controlX = panelX + MODULE_SETTINGS_PANEL_PADDING + settingWidth - controlWidth;
		updateSliderSetting(draggedSliderSetting, controlX, sliderWidth(controlWidth), mouseX);
		return true;
	}

	private static RangeHandle nearestRangeHandle(FloatRangeSetting setting, float sliderX, float sliderWidth, double mouseX) {
		FloatRangeSetting.Range range = setting.value();
		float fromX = sliderX + sliderWidth * normalized(range.from(), setting.minLimit(), setting.maxLimit());
		float toX = sliderX + sliderWidth * normalized(range.to(), setting.minLimit(), setting.maxLimit());
		if (Math.abs(fromX - toX) <= 0.5F) {
			if (range.from() <= setting.minLimit()) {
				return RangeHandle.TO;
			}
			if (range.to() >= setting.maxLimit()) {
				return RangeHandle.FROM;
			}
			return mouseX < fromX ? RangeHandle.FROM : RangeHandle.TO;
		}
		return Math.abs(mouseX - fromX) <= Math.abs(mouseX - toX) ? RangeHandle.FROM : RangeHandle.TO;
	}

	private static void updateRangeSetting(FloatRangeSetting setting, RangeHandle handle, float sliderX, float sliderWidth, double mouseX) {
		FloatRangeSetting.Range range = setting.value();
		float value = rangeValueFromMouse(setting, sliderX, sliderWidth, mouseX);
		if (handle == RangeHandle.FROM) {
			setting.setRange(Math.min(value, range.to()), range.to());
			return;
		}

		setting.setRange(range.from(), Math.max(value, range.from()));
	}

	private static void updateSliderSetting(FloatSetting setting, float sliderX, float sliderWidth, double mouseX) {
		setting.setFloatValue(sliderValueFromMouse(setting, sliderX, sliderWidth, mouseX));
	}

	private boolean handleMultiselectTagRemove(EnumBooleanMapSetting<?> setting, float controlX, float controlCenterY, float controlWidth, double mouseX, double mouseY) {
		float tagX = controlX + 6;
		float tagsRight = controlX + controlWidth - 20.0F;
		for (Enum<?> value : enabledEnumValues(setting)) {
			String label = trimMultiselectTagLabel(displayValue(value), tagsRight - tagX);
			if (label.isEmpty()) {
				break;
			}

			float tagWidth = multiselectTagWidth(label);

			if (isInside(
					mouseX,
					mouseY,
					tagX + tagWidth - MODULE_MULTISELECT_TAG_PADDING_X - MODULE_MULTISELECT_TAG_REMOVE_WIDTH,
					controlCenterY - MODULE_MULTISELECT_TAG_HEIGHT / 2.0F,
					MODULE_MULTISELECT_TAG_REMOVE_WIDTH + MODULE_MULTISELECT_TAG_PADDING_X,
					MODULE_MULTISELECT_TAG_HEIGHT
			)) {
				setEnumBooleanValue(setting, value, false);
				return true;
			}
			tagX += tagWidth + MODULE_MULTISELECT_TAG_GAP;
		}

		return false;
	}

	private Module hoveredModule(double mouseX, double mouseY) {
		ModuleLayout layout = hoveredModuleLayout(mouseX, mouseY);
		return layout == null ? null : layout.module();
	}

	private ModuleLayout moduleLayout(Module module) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		for (ModuleLayout layout : moduleLayouts(left, top)) {
			if (layout.module() == module) {
				return layout;
			}
		}

		return null;
	}

	private ModuleLayout hoveredModuleLayout(double mouseX, double mouseY) {
		if (selectedIconGroup != IconGroup.CATEGORY) {
			return null;
		}

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		for (ModuleLayout layout : moduleLayouts(left, top)) {
			if (isInside(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height())) {
				return layout;
			}
		}

		return null;
	}

	private boolean isSearchHovered(double mouseX, double mouseY) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int searchX = searchBarX(left);
		int searchY = top + (TOPBAR_HEIGHT - SEARCH_HEIGHT) / 2;
		return isInside(mouseX, mouseY, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT);
	}

	private int hoveredLayoutToggleOption(double mouseX, double mouseY) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int toggleX = layoutToggleX(left);
		int toggleY = top + (TOPBAR_HEIGHT - LAYOUT_TOGGLE_HEIGHT) / 2;
		if (!isInside(mouseX, mouseY, toggleX, toggleY, LAYOUT_TOGGLE_WIDTH, LAYOUT_TOGGLE_HEIGHT)) {
			return -1;
		}

		return mouseX < toggleX + LAYOUT_TOGGLE_OPTION_WIDTH ? 0 : 1;
	}

	private boolean isIconHovered(double mouseX, double mouseY, int index) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int navX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int navY = iconButtonY(top, index);
		float iconSize = navIconSize(index);
		float iconX = navX + (NAV_SECTION_WIDTH - iconSize) / 2.0F;
		float iconY = navY + (NAV_SECTION_HEIGHT - iconSize) / 2.0F;

		return mouseX >= iconX
				&& mouseX <= iconX + iconSize
				&& mouseY >= iconY
				&& mouseY <= iconY + iconSize;
	}

	private boolean isUtilityIconHovered(double mouseX, double mouseY, int index) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int utilityX = left + (SIDEBAR_WIDTH - NAV_SECTION_WIDTH) / 2;
		int utilityY = utilityIconY(top, index);
		float iconSize = utilityIconSize(index);
		float iconX = utilityX + (NAV_SECTION_WIDTH - iconSize) / 2.0F;
		float iconY = utilityY + (NAV_SECTION_HEIGHT - iconSize) / 2.0F;

		return mouseX >= iconX
				&& mouseX <= iconX + iconSize
				&& mouseY >= iconY
				&& mouseY <= iconY + iconSize;
	}

	private static int iconButtonY(int top, int index) {
		return top + TOPBAR_HEIGHT + index * (NAV_SECTION_HEIGHT + NAV_SECTION_GAP);
	}

	private static int utilityIconY(int top, int index) {
		int utilityHeight = UTILITY_ICON_COUNT * NAV_SECTION_HEIGHT + (UTILITY_ICON_COUNT - 1) * NAV_SECTION_GAP;
		return top + PANEL_HEIGHT - UTILITY_BOTTOM_PADDING - utilityHeight + index * (NAV_SECTION_HEIGHT + NAV_SECTION_GAP);
	}

	private static String navIcon(int index) {
		return switch (index) {
			case 0 -> SWORDS_ICON;
			case 1 -> WIND_ICON;
			case 2 -> RENDER_ICON;
			case 3 -> PLAYER_ICON;
			case 4 -> OTHER_ICON;
			default -> "";
		};
	}

	private static MsdfFontRenderer navIconFont(int index) {
		return switch (index) {
			case 2, 3 -> HATRED_CATEGORY_ICON_FONT;
			case 4 -> HATRED_MINCED_ICON_FONT;
			default -> ICON_FONT;
		};
	}

	private static float navIconSize(int index) {
		if (index < 2) {
			return NAV_ICON_SIZE;
		}

		float targetSize = ICON_FONT.visualSize(SWORDS_ICON, NAV_ICON_SIZE);
		float currentSize = navIconFont(index).visualSize(navIcon(index), NAV_ICON_SIZE);
		if (currentSize <= 0.0F) {
			return NAV_ICON_SIZE;
		}

		return NAV_ICON_SIZE * targetSize / currentSize;
	}

	private static String utilityIcon(int index) {
		return switch (index) {
			case 0 -> THEMES_ICON;
			case 1 -> FRIENDS_ICON;
			case 2 -> CONFIGS_ICON;
			case 3 -> SETTINGS_ICON;
			default -> "";
		};
	}

	private static float utilityIconSize(int index) {
		float targetSize = ICON_FONT.visualSize(SWORDS_ICON, NAV_ICON_SIZE);
		float currentSize = BOTTOM_ICON_FONT.visualSize(utilityIcon(index), NAV_ICON_SIZE);
		if (currentSize <= 0.0F) {
			return NAV_ICON_SIZE;
		}

		return NAV_ICON_SIZE * targetSize / currentSize;
	}

	private ModuleCategory selectedCategory() {
		return navCategory(selectedIconIndex);
	}

	private void selectPage(IconGroup group, int index) {
		PageState from = currentPage();
		PageState to = group == IconGroup.CATEGORY
				? new PageState(group, index, selectedUtilityIconIndex)
				: new PageState(group, selectedIconIndex, index);
		if (sameVisiblePage(from, to)) {
			selectedIconGroup = group;
			selectedIconIndex = to.categoryIndex();
			selectedUtilityIconIndex = to.utilityIndex();
			return;
		}

		targetModuleScrollOffset = 0.0F;
		moduleScrollOffset = 0.0F;
		targetSettingsScrollOffsets.clear();
		settingsScrollOffsets.clear();

		previousPage = from;
		pageTransitionDirection = Integer.compare(pageOrder(to), pageOrder(from));
		if (pageTransitionDirection == 0) {
			pageTransitionDirection = 1;
		}
		pageTransitionProgress = 0.0F;
		selectedIconGroup = group;
		selectedIconIndex = to.categoryIndex();
		selectedUtilityIconIndex = to.utilityIndex();
	}

	private PageState currentPage() {
		return new PageState(selectedIconGroup, selectedIconIndex, selectedUtilityIconIndex);
	}

	private static boolean sameVisiblePage(PageState first, PageState second) {
		if (first.group() != second.group()) {
			return false;
		}
		return first.group() == IconGroup.CATEGORY
				? first.categoryIndex() == second.categoryIndex()
				: first.utilityIndex() == second.utilityIndex();
	}

	private static int pageOrder(PageState page) {
		return page.group() == IconGroup.CATEGORY
				? page.categoryIndex()
				: NAV_ICON_COUNT + page.utilityIndex();
	}

	private void saveSelectedIcon() {
		savedIconGroup = selectedIconGroup;
		savedIconIndex = selectedIconIndex;
		savedUtilityIconIndex = selectedUtilityIconIndex;
	}

	private static ModuleCategory navCategory(int index) {
		return switch (index) {
			case 0 -> ModuleCategory.COMBAT;
			case 1 -> ModuleCategory.MOVEMENT;
			case 2 -> ModuleCategory.RENDER;
			case 3 -> ModuleCategory.PLAYER;
			case 4 -> ModuleCategory.OTHER;
			default -> ModuleCategory.COMBAT;
		};
	}

	private void startClosing() {
		if (closing) {
			return;
		}
		ThemeManager.save();

		long now = Util.getMillis();
		closingStartProgress = animationProgress(now);
		animationStartMillis = now;
		closing = true;
	}

	private float animationProgress(long now) {
		float elapsed = Math.min(GUI_ANIMATION_DURATION_MS, Math.max(0L, now - animationStartMillis));
		float progress = elapsed / GUI_ANIMATION_DURATION_MS;
		if (closing) {
			return closingStartProgress * (1.0F - progress);
		}

		return progress;
	}

	private float frameDelta(long now) {
		long elapsed = Math.min(50L, Math.max(0L, now - lastFrameMillis));
		lastFrameMillis = now;
		return elapsed / 16.666F;
	}

	private void updateIconRotations(double mouseX, double mouseY, float frameDelta) {
		for (int index = 0; index < NAV_ICON_COUNT; index++) {
			iconRotations[index] = updateIconRotation(iconRotations[index], !closing && isIconHovered(mouseX, mouseY, index), frameDelta);
		}
		for (int index = 0; index < UTILITY_ICON_COUNT; index++) {
			utilityIconRotations[index] = updateIconRotation(utilityIconRotations[index], !closing && isUtilityIconHovered(mouseX, mouseY, index), frameDelta);
		}
	}

	private void updateSettingAnimations(float frameDelta) {
		for (Module module : ModuleManager.modules()) {
			float current = settingAnimation(module);
			float target = module == expandedModule ? 1.0F : 0.0F;
			float updated = current + (target - current) * Math.min(1.0F, 0.22F * frameDelta);
			if (Math.abs(updated - target) < 0.01F) {
				updated = target;
			}
			if (updated > 0.0F) {
				settingAnimations.put(module, updated);
			}
		}

		Iterator<Map.Entry<Module, Float>> iterator = settingAnimations.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue() <= 0.0F) {
				iterator.remove();
			}
		}
	}

	private float settingAnimation(Module module) {
		return settingAnimations.getOrDefault(module, 0.0F);
	}

	private void updateModuleToggleAnimations(float frameDelta) {
		for (Module module : ModuleManager.modules()) {
			float current = toggleAnimation(module);
			float target = module.enabled() ? 1.0F : 0.0F;
			float updated = current + (target - current) * Math.min(1.0F, 0.24F * frameDelta);
			if (Math.abs(updated - target) < 0.01F) {
				updated = target;
			}
			if (updated > 0.0F || module.enabled()) {
				toggleAnimations.put(module, updated);
			} else {
				toggleAnimations.remove(module);
			}
		}
	}

	private float toggleAnimation(Module module) {
		return toggleAnimations.getOrDefault(module, module.enabled() ? 1.0F : 0.0F);
	}

	private void updateScrollAnimations(float frameDelta) {
		float maxModule = 0.0F;
		if (selectedIconGroup == IconGroup.CATEGORY && Minecraft.getInstance().screen != null) {
			int left = (this.width - PANEL_WIDTH) / 2;
			int top = (this.height - PANEL_HEIGHT) / 2;
			maxModule = maxModuleScroll(left, top);
		}
		targetModuleScrollOffset = Math.max(0.0F, Math.min(maxModule, targetModuleScrollOffset));
		float moduleStep = Math.min(1.0F, 0.24F * frameDelta);
		moduleScrollOffset += (targetModuleScrollOffset - moduleScrollOffset) * moduleStep;
		if (Math.abs(moduleScrollOffset - targetModuleScrollOffset) < 0.05F) {
			moduleScrollOffset = targetModuleScrollOffset;
		}

		for (Module module : ModuleManager.modules()) {
			float targetSetting = targetSettingsScrollOffsets.getOrDefault(module, 0.0F);
			float currentSetting = settingsScrollOffsets.getOrDefault(module, 0.0F);
			if (Math.abs(currentSetting - targetSetting) > 0.05F) {
				float updated = currentSetting + (targetSetting - currentSetting) * Math.min(1.0F, 0.26F * frameDelta);
				settingsScrollOffsets.put(module, updated);
			} else if (targetSetting > 0.0F) {
				settingsScrollOffsets.put(module, targetSetting);
			} else {
				settingsScrollOffsets.remove(module);
			}
		}
	}

	private void updateEnumDropdownAnimations(float frameDelta) {
		for (Module module : ModuleManager.modules()) {
			for (Setting<?> setting : module.settings()) {
				if (!setting.visible() || (!(setting instanceof EnumSetting<?>) && !(setting instanceof EnumBooleanMapSetting<?>))) {
					continue;
				}

				float current = enumDropdownAnimation(setting);
				float target = isEnumDropdownOpen(setting) && module == expandedModule ? 1.0F : 0.0F;
				float updated = current + (target - current) * Math.min(1.0F, 0.24F * frameDelta);
				if (Math.abs(updated - target) < 0.01F) {
					updated = target;
				}
				if (updated > 0.0F) {
					enumDropdownAnimations.put(setting, updated);
				} else {
					enumDropdownAnimations.remove(setting);
				}
			}
		}

		Iterator<Map.Entry<Setting<?>, Float>> iterator = enumDropdownAnimations.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue() <= 0.0F) {
				iterator.remove();
			}
		}
	}

	private float enumDropdownAnimation(Setting<?> setting) {
		return enumDropdownAnimations.getOrDefault(setting, 0.0F);
	}

	private float updateModuleIconRotation(Module module, float frameDelta) {
		float targetRotation = module.enabled() ? MODULE_ICON_ENABLED_ROTATION : MODULE_ICON_DISABLED_ROTATION;
		float currentRotation = moduleIconRotations.getOrDefault(module, targetRotation);
		float updatedRotation = currentRotation + (targetRotation - currentRotation) * Math.min(1.0F, MODULE_ICON_ROTATION_SPEED * frameDelta);
		if (Math.abs(updatedRotation - targetRotation) < 0.1F) {
			updatedRotation = targetRotation;
		}
		moduleIconRotations.put(module, updatedRotation);
		return updatedRotation;
	}

	private static float updateIconRotation(float currentRotation, boolean hovered, float frameDelta) {
		float targetRotation = hovered ? NAV_ICON_HOVER_ROTATION : 0.0F;
		float step = Math.min(1.0F, NAV_ICON_ROTATION_SPEED * frameDelta);
		return currentRotation + (targetRotation - currentRotation) * step;
	}

	private static float ease(float progress) {
		return progress * progress * (3.0F - 2.0F * progress);
	}

	private static int alpha(int color, float alpha) {
		int baseAlpha = color >>> 24;
		int appliedAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
		return appliedAlpha << 24 | color & 0x00FFFFFF;
	}

	private static int lerpColor(int from, int to, float progress) {
		float clamped = Math.max(0.0F, Math.min(1.0F, progress));
		int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * clamped);
		int r = Math.round(((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * clamped);
		int g = Math.round(((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * clamped);
		int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * clamped);
		return a << 24 | r << 16 | g << 8 | b;
	}

	private void updatePageTransition(float frameDelta) {
		if (pageTransitionProgress >= 1.0F) {
			previousPage = null;
			return;
		}
		pageTransitionProgress = Math.min(1.0F, pageTransitionProgress + PAGE_TRANSITION_SPEED * frameDelta);
		if (pageTransitionProgress >= 1.0F) {
			previousPage = null;
		}
	}

	private static Hsv rgbToHsv(int color) {
		float red = ((color >>> 16) & 255) / 255.0F;
		float green = ((color >>> 8) & 255) / 255.0F;
		float blue = (color & 255) / 255.0F;
		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float delta = max - min;
		float hue;
		if (delta <= 0.0001F) {
			hue = 0.0F;
		} else if (max == red) {
			hue = ((green - blue) / delta) % 6.0F;
		} else if (max == green) {
			hue = (blue - red) / delta + 2.0F;
		} else {
			hue = (red - green) / delta + 4.0F;
		}
		hue /= 6.0F;
		if (hue < 0.0F) {
			hue += 1.0F;
		}
		float saturation = max <= 0.0001F ? 0.0F : delta / max;
		return new Hsv(hue, saturation, max);
	}

	private static int hsvToRgb(float hue, float saturation, float value) {
		float normalizedHue = hue - (float) Math.floor(hue);
		float chroma = value * saturation;
		float section = normalizedHue * 6.0F;
		float secondary = chroma * (1.0F - Math.abs(section % 2.0F - 1.0F));
		float red;
		float green;
		float blue;
		if (section < 1.0F) {
			red = chroma;
			green = secondary;
			blue = 0.0F;
		} else if (section < 2.0F) {
			red = secondary;
			green = chroma;
			blue = 0.0F;
		} else if (section < 3.0F) {
			red = 0.0F;
			green = chroma;
			blue = secondary;
		} else if (section < 4.0F) {
			red = 0.0F;
			green = secondary;
			blue = chroma;
		} else if (section < 5.0F) {
			red = secondary;
			green = 0.0F;
			blue = chroma;
		} else {
			red = chroma;
			green = 0.0F;
			blue = secondary;
		}
		float match = value - chroma;
		int r = Math.round((red + match) * 255.0F);
		int g = Math.round((green + match) * 255.0F);
		int b = Math.round((blue + match) * 255.0F);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
		return mouseX >= x
				&& mouseX <= x + width
				&& mouseY >= y
				&& mouseY <= y + height;
	}

	private record ModuleLayout(Module module, int x, int y, int width, int height) {
	}

	private record PageState(IconGroup group, int categoryIndex, int utilityIndex) {
	}

	private record Hsv(float hue, float saturation, float value) {
	}

	private enum ColorPickerDrag {
		SATURATION_VALUE,
		HUE
	}

	private enum IconGroup {
		CATEGORY,
		UTILITY
	}

	private enum FriendInputFocus {
		NONE,
		NAME,
		ALIAS
	}

	private enum RangeHandle {
		FROM,
		TO
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	}
}
