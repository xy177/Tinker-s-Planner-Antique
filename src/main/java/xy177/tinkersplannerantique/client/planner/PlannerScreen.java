package xy177.tinkersplannerantique.client.planner;

import java.io.IOException;
import java.util.Collection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import c4.conarm.client.gui.GuiArmorStation;
import c4.conarm.common.ConstructsRegistry;
import c4.conarm.lib.modifiers.ArmorModifier;
import c4.conarm.lib.modifiers.ArmorModifierTrait;
import c4.conarm.lib.materials.ArmorMaterialType;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.tconstruct.library.materials.ExtraMaterialStats;
import slimeknights.tconstruct.library.materials.HandleMaterialStats;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.HeadMaterialStats;
import slimeknights.tconstruct.library.materials.IMaterialStats;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.modifiers.ProjectileModifierTrait;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.traits.ITrait;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;
import slimeknights.tconstruct.common.ClientProxy;
import slimeknights.tconstruct.tools.TinkerMaterials;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.common.client.GuiToolStation;
import slimeknights.tconstruct.tools.modifiers.ToolModifier;
import slimeknights.tconstruct.tools.ranged.item.BoltCore;
import xy177.tinkersplannerantique.PlannerConfig;
import xy177.tinkersplannerantique.TinkersPlannerAntique;

public class PlannerScreen extends GuiScreen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TinkersPlannerAntique.MODID, "textures/gui/planner.png");
    private static final ResourceLocation PLABBER_WINDOW = new ResourceLocation(TinkersPlannerAntique.MODID, "textures/gui/plabber_window.png");
    private static final ResourceLocation ICON_RANDOM = icon("random");
    private static final ResourceLocation ICON_BOOKMARK_OPEN = icon("bookmark_open");
    private static final ResourceLocation ICON_BOOKMARK_CLOSE = icon("bookmark_close");
    private static final ResourceLocation ICON_SAVE = icon("save");
    private static final ResourceLocation ICON_UNSAVE = icon("unsave");
    private static final ResourceLocation ICON_MODIFIERS_OPEN = icon("modifiers_open");
    private static final ResourceLocation ICON_MODIFIERS_CLOSE = icon("modifiers_close");
    private static final ResourceLocation ICON_GIVE_ITEM = icon("give_item");
    private static final ResourceLocation ICON_EXPORT_BLUEPRINT = icon("export_blueprint");
    private static final ResourceLocation ICON_IMPORT_BLUEPRINT = icon("import_blueprint");
    private static final ResourceLocation ICON_SORT_ATTACK = icon("sort_attack");
    private static final ResourceLocation ICON_SORT_SPEED = icon("sort_speed");
    private static final ResourceLocation ICON_SORT_HARVEST = icon("sort_harvest");
    private static final ResourceLocation ICON_SORT_DURABILITY = icon("sort_durability");
    private static final ResourceLocation ICON_SORT_MODIFIER = icon("sort_modifier");
    private static final ResourceLocation ICON_SORT_ARMOR = icon("sort_armor");
    private static final ResourceLocation ICON_SORT_ARMOR_DURABILITY = icon("sort_armor_durability");
    private static final ResourceLocation ICON_SORT_TOUGHNESS = icon("sort_toughness");
    private static final ResourceLocation ICON_SORT_GENERIC = new ResourceLocation("minecraft", "textures/items/name_tag.png");
    private static final ResourceLocation ICON_TAB_BUTTON_LIGHT = icon("tab_button_light");
    private static final ResourceLocation ICON_TAB_BUTTON_DARK = icon("tab_button_dark");
    private static final ResourceLocation ICON_MATERIAL_ICONS = new ResourceLocation("minecraft", "textures/items/iron_ingot.png");
    private static final ResourceLocation ICON_SORT_WEIGHT = icon("sort_weight");
    private static final ResourceLocation ICON_SORT_FRICTION = new ResourceLocation("minecraft", "textures/items/dye_powder_black.png");
    private static final ResourceLocation ICON_SORT_YOYO_LENGTH = new ResourceLocation("yoyos", "textures/items/cord.png");
    private static final String YOYOS_MODID = "yoyos";
    private static final String YOYO_BODY = "body";
    private static final String YOYO_CORD = "cord";
    private static final String YOYO_AXLE = "axle";
    private static final Set<String> BUILT_IN_SORT_STAT_TYPES = new LinkedHashSet<>();

    static {
        Collections.addAll(BUILT_IN_SORT_STAT_TYPES,
            MaterialTypes.HEAD,
            MaterialTypes.HANDLE,
            MaterialTypes.EXTRA,
            MaterialTypes.BOW,
            MaterialTypes.BOWSTRING,
            MaterialTypes.SHAFT,
            MaterialTypes.FLETCHING,
            MaterialTypes.PROJECTILE,
            ArmorMaterialType.CORE,
            ArmorMaterialType.PLATES,
            ArmorMaterialType.TRIM,
            YOYO_BODY,
            YOYO_CORD,
            YOYO_AXLE
        );
    }

    private static final int CENTER_W = 175;
    private static final int CENTER_H = 204;
    private static final int LEFT_W = 100;
    private static final int LEFT_TOP_H = 103;
    private static final int RIGHT_W = LEFT_W;
    private static final int GAP = 4;
    private static final int TAB_W = 38;
    private static final int TAB_H = 20;
    private static final int TAB_GAP = 4;
    private static final int TOOL_PREVIEW_X = 13;
    private static final int TOOL_PREVIEW_Y = 24;
    private static final int TOOL_PREVIEW_SIZE = 81;
    private static final int BTN_TOOLS = 10;
    private static final int BTN_ARMOR = 11;
    private static final int BTN_PREV_TOOL = 16;
    private static final int BTN_NEXT_TOOL = 17;
    private static final int BTN_PREV_BOOKMARK = 18;
    private static final int BTN_NEXT_BOOKMARK = 19;
    private static final int BTN_PREV_MATERIAL = 20;
    private static final int BTN_NEXT_MATERIAL = 21;
    private static final int BTN_PREV_MOD = 22;
    private static final int BTN_NEXT_MOD = 23;
    private static final int BTN_PREV_TRAIT = 24;
    private static final int BTN_NEXT_TRAIT = 25;
    private static final int TRAIT_PANEL_GAP = 4;
    private static final int TRAIT_ROW_H = 14;

    private static final int ID_TOOL_BASE = 1000;
    private static final int ID_BOOKMARK_BASE = 2000;
    private static final int ID_PART_BASE = 3000;
    private static final int ID_MATERIAL_BASE = 4000;
    private static final int ID_MODIFIER_BASE = 6000;

    private final GuiScreen parent;
    private final PlannerTarget.TargetType type;
    private final List<? extends PlannerTarget> targets;
    private final PlannerData data;
    private final List<PlannerBlueprint> savedBlueprints;
    private PlannerBlueprint starredBlueprint;
    private final Random random = new Random();

    private enum RightPanelMode {
        NONE,
        BOOKMARKS,
        MODIFIERS
    }

    private enum MaterialSelectionMode {
        NONE,
        PART,
        EMBOSS,
        EMBOSS2,
        SPECIAL_MATERIAL
    }

    private PlannerBlueprint blueprint;
    private int selectedPart = -1;
    private int toolPage;
    private int bookmarkPage;
    private int materialPage;
    private int modifierPage;
    private int traitPage;
    private MaterialSortEntry activeSort;
    private RightPanelMode rightPanelMode = RightPanelMode.NONE;
    private MaterialSelectionMode materialSelectionMode = MaterialSelectionMode.NONE;
    private boolean exportShortMode = PlannerConfig.enableShortBlueprintCode;
    private boolean useRepresentativeMaterialIcons;

    private int centerLeft;
    private int centerTop;
    private int leftPanelLeft;
    private int rightPanelLeft;

    private final List<PanelItemButton> toolButtons = new ArrayList<>();
    private final List<PanelItemButton> bookmarkButtons = new ArrayList<>();
    private final List<PanelItemButton> partButtons = new ArrayList<>();
    private final List<PanelItemButton> materialButtons = new ArrayList<>();
    private final List<PlannerActionButton> actionButtons = new ArrayList<>();
    private final List<PanelModifierButton> modifierButtons = new ArrayList<>();
    private final List<TraitFilterButton> traitButtons = new ArrayList<>();
    private ItemStack deferredTooltipStack = ItemStack.EMPTY;
    private List<String> deferredTooltipLines;
    private String selectedTraitFilterId;

    private PlannerScreen(GuiScreen parent, PlannerTarget.TargetType type, List<? extends PlannerTarget> targets) {
        this.parent = parent;
        this.type = type;
        this.targets = targets;
        this.data = PlannerClientEvents.getData();
        this.savedBlueprints = data.loadSaved(type, targets);
        this.starredBlueprint = data.loadStarred(type, targets);
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation(TinkersPlannerAntique.MODID, "textures/gui/icons/" + name + ".png");
    }

    public static PlannerScreen forTools(GuiToolStation parent) {
        PlannerScreen screen = new PlannerScreen(parent, PlannerTarget.TargetType.TOOL, PlannerClientEvents.getToolTargets());
        screen.importFromParent();
        return screen;
    }

    public static PlannerScreen forArmor(GuiArmorStation parent) {
        PlannerScreen screen = new PlannerScreen(parent, PlannerTarget.TargetType.ARMOR, PlannerClientEvents.getArmorTargets());
        screen.importFromParent();
        return screen;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        toolButtons.clear();
        bookmarkButtons.clear();
        partButtons.clear();
        materialButtons.clear();
        actionButtons.clear();
        modifierButtons.clear();
        traitButtons.clear();

        centerLeft = width / 2 - CENTER_W / 2;
        centerTop = height / 2 - CENTER_H / 2;
        leftPanelLeft = centerLeft - LEFT_W - GAP;
        rightPanelLeft = centerLeft + CENTER_W;

        int tabX = leftPanelLeft + (LEFT_W - TAB_W * 2 - TAB_GAP) / 2;
        GuiButton toolsTab = addButton(new TabButton(BTN_TOOLS, tabX, centerTop - 22, I18n.translateToLocal("gui.tpa.tools")));
        GuiButton armorTab = addButton(new TabButton(BTN_ARMOR, tabX + TAB_W + TAB_GAP, centerTop - 22, I18n.translateToLocal("gui.tpa.armor")));
        toolsTab.enabled = type != PlannerTarget.TargetType.TOOL;
        armorTab.enabled = type != PlannerTarget.TargetType.ARMOR;

        rebuildToolButtons();
        rebuildPartButtons();
        rebuildMaterialButtons();
        rebuildTraitButtons();
        rebuildActionButtons();
        rebuildRightPanelButtons();
    }

    private void rebuildToolButtons() {
        int start = toolPage * 15;
        int end = Math.min(targets.size(), start + 15);
        for (int i = start; i < end; i++) {
            int local = i - start;
            toolButtons.add(new PanelItemButton(ID_TOOL_BASE + i, leftPanelLeft, centerTop, 6 + (local % 5) * 18, 23 + (local / 5) * 18, targets.get(i).getRenderStack(), targets.get(i).getRenderStack(), targets.get(i).getDisplayName(), isCurrentTarget(i), true));
        }
        addButton(new PageButton(BTN_PREV_TOOL, leftPanelLeft + 6, centerTop + LEFT_TOP_H - 24, false)).enabled = toolPage > 0;
        addButton(new PageButton(BTN_NEXT_TOOL, leftPanelLeft + LEFT_W - 44, centerTop + LEFT_TOP_H - 24, true)).enabled = end < targets.size();
    }

    private void rebuildBookmarkButtons() {
        int bookmarkTop = centerTop;
        int start = bookmarkPage * 25;
        int end = Math.min(savedBlueprints.size(), start + 25);
        for (int i = start; i < end; i++) {
            int local = i - start;
            PlannerBlueprint saved = savedBlueprints.get(i);
            ItemStack display = saved.buildPreview();
            bookmarkButtons.add(new PanelItemButton(ID_BOOKMARK_BASE + i, rightPanelLeft, bookmarkTop, 2 + (local % 5) * 18, 23 + (local / 5) * 18, display, display, saved.target.getDisplayName(), saved.equals(starredBlueprint), false));
        }
        addButton(new PageButton(BTN_PREV_BOOKMARK, rightPanelLeft + 6, bookmarkTop + CENTER_H - 24, false)).enabled = bookmarkPage > 0;
        addButton(new PageButton(BTN_NEXT_BOOKMARK, rightPanelLeft + RIGHT_W - 44, bookmarkTop + CENTER_H - 24, true)).enabled = end < savedBlueprints.size();
    }

    private void rebuildPartButtons() {
        if (blueprint == null) {
            return;
        }
        List<int[]> positions = getPartPositions(blueprint.target);
        List<PartMaterialType> parts = blueprint.target.getRequiredComponents();
        for (int i = 0; i < parts.size() && i < positions.size(); i++) {
            IToolPart part = getDisplayPart(parts.get(i));
            if (part == null) {
                continue;
            }
            ItemStack stack = getPartDisplayStack(i, part, blueprint.materials[i]);
            int[] pos = positions.get(i);
            partButtons.add(new PanelItemButton(ID_PART_BASE + i, centerLeft, centerTop, pos[0] + 13, pos[1] + 15, stack, stack, part.getOutlineRenderStack().getDisplayName(), selectedPart == i, true));
        }
    }

    private void rebuildMaterialButtons() {
        if (blueprint == null) {
            return;
        }
        List<MaterialOption> options = getDisplayedMaterialOptions();
        if (options.isEmpty()) {
            return;
        }
        int start = materialPage * 27;
        int end = Math.min(options.size(), start + 27);
        for (int i = start; i < end; i++) {
            int local = i - start;
            MaterialOption option = options.get(i);
            materialButtons.add(new PanelItemButton(ID_MATERIAL_BASE + i, centerLeft, centerTop + 115, 8 + (local % 9) * 18, 2 + (local / 9) * 18, option.displayStack, option.tooltipStack, option.tooltip, option.highlighted, false));
        }
        addButton(new PageButton(BTN_PREV_MATERIAL, centerLeft + 6, centerTop + 174, false)).enabled = materialPage > 0;
        addButton(new PageButton(BTN_NEXT_MATERIAL, centerLeft + CENTER_W - 44, centerTop + 174, true)).enabled = end < options.size();
    }

    private void rebuildTraitButtons() {
        if (!shouldShowTraitFilterPanel()) {
            return;
        }
        List<TraitFilterEntry> traits = getCurrentPartTraits();
        int panelTop = centerTop + LEFT_TOP_H + TRAIT_PANEL_GAP;
        int panelBottom = centerTop + CENTER_H;
        int maxRows = Math.max(0, (panelBottom - panelTop - 26) / TRAIT_ROW_H);
        int start = traitPage * maxRows;
        int end = maxRows <= 0 ? 0 : Math.min(traits.size(), start + maxRows);
        if (start >= traits.size() && traitPage > 0) {
            traitPage = 0;
            start = 0;
            end = Math.min(traits.size(), maxRows);
        }
        int count = Math.max(0, end - start);
        for (int i = 0; i < count; i++) {
            TraitFilterEntry trait = traits.get(start + i);
            traitButtons.add(new TraitFilterButton(leftPanelLeft + 2, panelTop + 2 + i * TRAIT_ROW_H, LEFT_W - 4, TRAIT_ROW_H, trait));
        }
        addButton(new PageButton(BTN_PREV_TRAIT, leftPanelLeft + 6, panelBottom - 24, false)).enabled = traitPage > 0;
        addButton(new PageButton(BTN_NEXT_TRAIT, leftPanelLeft + LEFT_W - 44, panelBottom - 24, true)).enabled = end < traits.size();
    }

    private void rebuildActionButtons() {
        int actionX = centerLeft + CENTER_W - 70;
        int actionY = centerTop + 88;
        int panelToggleY = centerTop + 28;
        addActionButton(actionX, actionY, ICON_RANDOM, tooltip("gui.tpa.random"), true, new PressHandler() {
            @Override
            public void press(int mouseButton) {
                randomize();
            }
        });

        ItemStack output = blueprint == null ? ItemStack.EMPTY : blueprint.buildPreview();
        boolean complete = !output.isEmpty();
        boolean saved = complete && isSaved(blueprint);
        boolean bookmarksOpen = rightPanelMode == RightPanelMode.BOOKMARKS;
        addActionButton(actionX, panelToggleY, bookmarksOpen ? ICON_BOOKMARK_CLOSE : ICON_BOOKMARK_OPEN, tooltip(bookmarksOpen ? "gui.tpa.hide_bookmarks" : "gui.tpa.show_bookmarks"), true, new PressHandler() {
            @Override
            public void press(int mouseButton) {
                toggleRightPanel(RightPanelMode.BOOKMARKS);
            }
        });

        if (complete) {
            addActionButton(actionX + 18, actionY, saved ? ICON_UNSAVE : ICON_SAVE, tooltip(saved ? "gui.tpa.unsave" : "gui.tpa.save"), true, new PressHandler() {
                @Override
                public void press(int mouseButton) {
                    toggleSave();
                }
            });
        }

        if (blueprint != null) {
            boolean modifiersOpen = rightPanelMode == RightPanelMode.MODIFIERS;
            addActionButton(actionX + 18, panelToggleY, modifiersOpen ? ICON_MODIFIERS_CLOSE : ICON_MODIFIERS_OPEN, tooltip(modifiersOpen ? "gui.tpa.hide_modifiers" : "gui.tpa.show_modifiers"), true, new PressHandler() {
                @Override
                public void press(int mouseButton) {
                    toggleRightPanel(RightPanelMode.MODIFIERS);
                }
            });
        }

        addActionButton(actionX + 36, panelToggleY, complete ? ICON_EXPORT_BLUEPRINT : ICON_IMPORT_BLUEPRINT, getBlueprintCodeTooltip(complete), true, new PressHandler() {
            @Override
                public void press(int mouseButton) {
                    if (complete) {
                        if (mouseButton == 1 && PlannerConfig.enableShortBlueprintCode) {
                            exportShortMode = !exportShortMode;
                            refreshLayout();
                            return;
                        }
                        exportBlueprintCode();
                    } else if (mouseButton == 0) {
                    mc.displayGuiScreen(new PlannerCodeImportScreen(PlannerScreen.this));
                }
            }
        });

        if (complete && canUseGiveItem()) {
            addActionButton(actionX + 36, actionY, ICON_GIVE_ITEM, tooltip("gui.tpa.give"), complete, new PressHandler() {
                @Override
                public void press(int mouseButton) {
                    giveCurrentItem();
                }
            });
        }

        rebuildSortActionButtons();
    }

    private void rebuildSortActionButtons() {
        List<MaterialSortEntry> sorts = getCurrentSorts();
        if (sorts.isEmpty()) {
            return;
        }
        int y = centerTop + 177;
        int totalButtons = sorts.size() + 1;
        int x = centerLeft + CENTER_W / 2 - (totalButtons * 18 - 2) / 2;
        addActionButton(x, y, ICON_MATERIAL_ICONS, tooltip("gui.tpa.material_icons"), true, useRepresentativeMaterialIcons ? 1.0F : 0.45F, new PressHandler() {
            @Override
            public void press(int mouseButton) {
                useRepresentativeMaterialIcons = !useRepresentativeMaterialIcons;
                refreshLayout();
            }
        });
        for (int i = 0; i < sorts.size(); i++) {
            addSortButton(x + (i + 1) * 18, y, sorts.get(i));
        }
    }

    private void addSortButton(int x, int y, final MaterialSortEntry sort) {
        PressHandler handler = new PressHandler() {
            @Override
            public void press(int mouseButton) {
                toggleSort(sort);
            }
        };
        String tooltip = tooltip("gui.tpa.sort", sort.getLabel());
        float alpha = sort.equals(activeSort) ? 1.0F : 0.45F;
        addActionButton(x, y, sort.getIconTexture(), tooltip, true, alpha, handler);
    }

    private void addActionButton(int x, int y, ResourceLocation icon, String tooltip, boolean enabled, PressHandler handler) {
        addActionButton(x, y, icon, tooltip, enabled, 1.0F, handler);
    }

    private void addActionButton(int x, int y, ResourceLocation icon, String tooltip, boolean enabled, float alpha, PressHandler handler) {
        actionButtons.add(new PlannerActionButton(x, y, icon, tooltip, enabled, alpha, handler));
    }

    private void addActionButton(int x, int y, ItemStack icon, String tooltip, boolean enabled, PressHandler handler) {
        addActionButton(x, y, icon, tooltip, enabled, 1.0F, handler);
    }

    private void addActionButton(int x, int y, ItemStack icon, String tooltip, boolean enabled, float alpha, PressHandler handler) {
        actionButtons.add(new PlannerActionButton(x, y, icon, tooltip, enabled, alpha, handler));
    }

    private void rebuildModifierButtons() {
        if (blueprint == null) {
            return;
        }
        List<ModifierListEntry> entries = getModifierEntries();
        int start = modifierPage * 9;
        int end = Math.min(entries.size(), start + 9);
        for (int i = start; i < end; i++) {
            modifierButtons.add(new PanelModifierButton(ID_MODIFIER_BASE + i, rightPanelLeft + 2, centerTop + 20 + (i - start) * 18, RIGHT_W - 4, 18, entries.get(i)));
        }
        addButton(new PageButton(BTN_PREV_MOD, rightPanelLeft + 6, centerTop + CENTER_H - 24, false)).enabled = modifierPage > 0;
        addButton(new PageButton(BTN_NEXT_MOD, rightPanelLeft + RIGHT_W - 44, centerTop + CENTER_H - 24, true)).enabled = end < entries.size();
    }

    private void rebuildRightPanelButtons() {
        if (rightPanelMode == RightPanelMode.NONE) {
            return;
        } else if (rightPanelMode == RightPanelMode.BOOKMARKS) {
            rebuildBookmarkButtons();
        } else {
            rebuildModifierButtons();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_TOOLS:
                saveBookmarks();
                mc.displayGuiScreen(new PlannerScreen(parent, PlannerTarget.TargetType.TOOL, PlannerClientEvents.getToolTargets()));
                return;
            case BTN_ARMOR:
                saveBookmarks();
                mc.displayGuiScreen(new PlannerScreen(parent, PlannerTarget.TargetType.ARMOR, PlannerClientEvents.getArmorTargets()));
                return;
            case BTN_PREV_TOOL:
                toolPage--;
                refreshLayout();
                return;
            case BTN_NEXT_TOOL:
                toolPage++;
                refreshLayout();
                return;
            case BTN_PREV_BOOKMARK:
                bookmarkPage--;
                refreshLayout();
                return;
            case BTN_NEXT_BOOKMARK:
                bookmarkPage++;
                refreshLayout();
                return;
            case BTN_PREV_MATERIAL:
                materialPage--;
                refreshLayout();
                return;
            case BTN_NEXT_MATERIAL:
                materialPage++;
                refreshLayout();
                return;
            case BTN_PREV_MOD:
                modifierPage--;
                refreshLayout();
                return;
            case BTN_NEXT_MOD:
                modifierPage++;
                refreshLayout();
                return;
            case BTN_PREV_TRAIT:
                traitPage--;
                refreshLayout();
                return;
            case BTN_NEXT_TRAIT:
                traitPage++;
                refreshLayout();
                return;
            default:
                if (button.id >= ID_TOOL_BASE && button.id < ID_BOOKMARK_BASE) {
                    selectTarget(button.id - ID_TOOL_BASE);
                    return;
                }
                if (button.id >= ID_BOOKMARK_BASE && button.id < ID_PART_BASE) {
                    int index = button.id - ID_BOOKMARK_BASE;
                    if (index >= 0 && index < savedBlueprints.size()) {
                        blueprint = savedBlueprints.get(index).copy();
                        selectedPart = -1;
                        materialPage = 0;
                        modifierPage = 0;
                        activeSort = null;
                        materialSelectionMode = MaterialSelectionMode.NONE;
                        refreshLayout();
                    }
                    return;
                }
                if (button.id >= ID_PART_BASE && button.id < ID_MATERIAL_BASE) {
                    selectedPart = button.id - ID_PART_BASE;
                    materialSelectionMode = MaterialSelectionMode.PART;
                    materialPage = 0;
                    traitPage = 0;
                    activeSort = null;
                    selectedTraitFilterId = null;
                    refreshLayout();
                    return;
                }
                if (button.id >= ID_MATERIAL_BASE && button.id < ID_MODIFIER_BASE) {
                    applyMaterial(button.id - ID_MATERIAL_BASE);
                    return;
                }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mousePressedRightPanelPageButton(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (PanelItemButton button : toolButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                actionPerformed(button);
                return;
            }
        }
        if (rightPanelMode == RightPanelMode.BOOKMARKS) {
            for (PanelItemButton button : bookmarkButtons) {
                if (button.mousePressed(mc, mouseX, mouseY)) {
                    actionPerformed(button);
                    return;
                }
            }
        }
        for (PanelItemButton button : partButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                int partIndex = button.id - ID_PART_BASE;
                if (mouseButton == 1 && selectedPart == partIndex && blueprint != null && partIndex >= 0 && partIndex < blueprint.materials.length) {
                    blueprint.materials[partIndex] = null;
                    materialSelectionMode = MaterialSelectionMode.PART;
                    materialPage = 0;
                    traitPage = 0;
                    activeSort = null;
                    refreshLayout();
                    return;
                }
                actionPerformed(button);
                return;
            }
        }
        for (PanelItemButton button : materialButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                actionPerformed(button);
                return;
            }
        }
        for (TraitFilterButton button : traitButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                button.press();
                return;
            }
        }
        for (PlannerActionButton button : actionButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                button.playPressSound(mc.getSoundHandler());
                button.press(mouseButton);
                return;
            }
        }
        if (rightPanelMode == RightPanelMode.MODIFIERS) {
            for (PanelModifierButton button : modifierButtons) {
                if (button.mousePressed(mc, mouseX, mouseY)) {
                    button.press(mouseButton);
                    refreshLayout();
                    return;
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        clearDeferredTooltip();
        drawDefaultBackground();
        drawLeftPanels(mouseX, mouseY);
        drawCenterPanel(mouseX, mouseY);
        if (rightPanelMode != RightPanelMode.NONE) {
            drawRightPanel(mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        resetCustomRenderState();
        drawCustomButtons(mouseX, mouseY);
        resetCustomRenderState();
        drawRightPanelPageButtons(mouseX, mouseY);
        drawDeferredTooltip(mouseX, mouseY);
    }

    private void resetCustomRenderState() {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        RenderHelper.disableStandardItemLighting();
    }

    private void drawLeftPanels(int mouseX, int mouseY) {
        drawPanel(leftPanelLeft, centerTop, LEFT_W, LEFT_TOP_H);
        drawBanner(leftPanelLeft + 5, centerTop, banner(type == PlannerTarget.TargetType.TOOL ? "gui.tpa.tools" : "gui.tpa.armor"));
        if (shouldShowTraitFilterPanel()) {
            drawPanel(leftPanelLeft, centerTop + LEFT_TOP_H + TRAIT_PANEL_GAP, LEFT_W, CENTER_H - LEFT_TOP_H - TRAIT_PANEL_GAP);
        }
    }

    private void drawCenterPanel(int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawTexturedModalRect(centerLeft, centerTop, 0, 0, CENTER_W, CENTER_H);
        drawTargetGhost();
        drawCenterOverlay();
        drawCenteredString(fontRenderer, blueprint == null ? I18n.translateToLocal("gui.tpa.select") : blueprint.target.getDisplayName(), centerLeft + CENTER_W / 2, centerTop + 7, 0xFFFFFF);
        drawOutputPreview(mouseX, mouseY);
    }

    private void drawRightPanel(int mouseX, int mouseY) {
        drawPanel(rightPanelLeft, centerTop, RIGHT_W, CENTER_H);
        drawBanner(rightPanelLeft + 5, centerTop, banner(rightPanelMode == RightPanelMode.BOOKMARKS ? "gui.tpa.bookmarked" : "gui.tpa.modifiers"));
    }

    private void drawCustomButtons(int mouseX, int mouseY) {
        for (PanelItemButton button : toolButtons) {
            button.drawButton(mc, mouseX, mouseY, 0F);
        }
        for (PanelItemButton button : bookmarkButtons) {
            if (rightPanelMode == RightPanelMode.BOOKMARKS) {
                button.drawButton(mc, mouseX, mouseY, 0F);
            }
        }
        for (PanelItemButton button : partButtons) {
            button.drawButton(mc, mouseX, mouseY, 0F);
        }
        for (PanelItemButton button : materialButtons) {
            button.drawButton(mc, mouseX, mouseY, 0F);
        }
        for (TraitFilterButton button : traitButtons) {
            button.drawButton(mc, mouseX, mouseY, 0F);
        }
        for (PlannerActionButton button : actionButtons) {
            button.drawButton(mc, mouseX, mouseY, 0F);
        }
        for (PanelModifierButton button : modifierButtons) {
            if (rightPanelMode == RightPanelMode.MODIFIERS) {
                button.drawButton(mc, mouseX, mouseY, 0F);
            }
        }
    }

    private void drawRightPanelPageButtons(int mouseX, int mouseY) {
        for (GuiButton button : buttonList) {
            if (isRightPanelPageButton(button.id) || isTraitPageButton(button.id)) {
                button.drawButton(mc, mouseX, mouseY, 0F);
            }
        }
    }

    private boolean mousePressedRightPanelPageButton(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : buttonList) {
            if ((isRightPanelPageButton(button.id) || isTraitPageButton(button.id)) && button.mousePressed(mc, mouseX, mouseY)) {
                button.playPressSound(mc.getSoundHandler());
                actionPerformed(button);
                return true;
            }
        }
        return false;
    }

    private boolean isRightPanelPageButton(int id) {
        if (rightPanelMode == RightPanelMode.BOOKMARKS) {
            return id == BTN_PREV_BOOKMARK || id == BTN_NEXT_BOOKMARK;
        }
        return rightPanelMode == RightPanelMode.MODIFIERS && (id == BTN_PREV_MOD || id == BTN_NEXT_MOD);
    }

    private boolean isTraitPageButton(int id) {
        return shouldShowTraitFilterPanel() && (id == BTN_PREV_TRAIT || id == BTN_NEXT_TRAIT);
    }

    private void drawTargetGhost() {
        if (blueprint == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerLeft + 23.7F, centerTop + 37, 0);
        GlStateManager.scale(3.7F, 3.7F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1F, 1F, 1F, 1F);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(blueprint.target.getRenderStack(), 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void drawCenterOverlay() {
        mc.getTextureManager().bindTexture(PLABBER_WINDOW);
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawTexturedModalRect(centerLeft, centerTop, 0, 0, CENTER_W, CENTER_H);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
    }

    private void drawOutputPreview(int mouseX, int mouseY) {
        if (blueprint == null) {
            return;
        }
        ItemStack output = blueprint.buildPreview();
        if (output.isEmpty()) {
            return;
        }
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(centerLeft + CENTER_W - 40, centerTop + 52, 176, 117, 28, 28);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(output, centerLeft + CENTER_W - 34, centerTop + 58);
        RenderHelper.disableStandardItemLighting();
        if (mouseX >= centerLeft + CENTER_W - 34 && mouseX < centerLeft + CENTER_W - 18 && mouseY >= centerTop + 58 && mouseY < centerTop + 74) {
            deferTooltip(output);
        }
    }

    private void clearDeferredTooltip() {
        deferredTooltipStack = ItemStack.EMPTY;
        deferredTooltipLines = null;
    }

    private void deferTooltip(ItemStack stack) {
        deferredTooltipStack = stack;
        deferredTooltipLines = null;
    }

    private void deferTooltip(List<String> lines) {
        deferredTooltipStack = ItemStack.EMPTY;
        deferredTooltipLines = lines;
    }

    private void drawDeferredTooltip(int mouseX, int mouseY) {
        if (!deferredTooltipStack.isEmpty()) {
            renderToolTip(deferredTooltipStack, mouseX, mouseY);
        } else if (deferredTooltipLines != null && !deferredTooltipLines.isEmpty()) {
            FontRenderer tooltipFont = ClientProxy.fontRenderer != null ? ClientProxy.fontRenderer : fontRenderer;
            drawHoveringText(deferredTooltipLines, mouseX, mouseY, tooltipFont);
        }
    }

    private void drawPanel(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, 0xA0305D8A);
        drawRect(x, y, x + width, y + 1, 0x80B6D8FF);
        drawRect(x, y + height - 1, x + width, y + height, 0x804C78A8);
        drawRect(x, y, x + 1, y + height, 0x80B6D8FF);
        drawRect(x + width - 1, y, x + width, y + height, 0x804C78A8);
    }

    private void drawBanner(int x, int y, String text) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(x, y, 0, 205, 90, 19);
        drawCenteredString(fontRenderer, text, x + 45, y + 6, 0xFF9090FF);
    }

    private List<String> expandTooltipLines(String text, TextFormatting defaultColor) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String normalized = text.replace("\r\n", "\n").replace("\\n", "\n");
        String carryFormat = defaultColor == null ? "" : defaultColor.toString();
        for (String rawLine : normalized.split("\n", -1)) {
            String line = rawLine;
            if (!carryFormat.isEmpty() && !line.startsWith("\u00a7")) {
                line = carryFormat + line;
            }
            lines.add(line);
            String nextFormat = FontRenderer.getFormatFromString(line);
            if (!nextFormat.isEmpty()) {
                carryFormat = nextFormat;
            }
        }
        return lines;
    }

    private String getModifierTooltipColor(IModifier modifier) {
        if (blueprint == null) {
            return "";
        }
        ItemStack preview = blueprint.buildPreview();
        if (preview.isEmpty()) {
            return "";
        }
        NBTTagCompound rootTag = TagUtil.getTagSafe(preview);
        if (!TinkerUtil.hasModifier(rootTag, modifier.getIdentifier())) {
            return "";
        }
        return ModifierNBT.readTag(TinkerUtil.getModifierTag(preview, modifier.getIdentifier())).getColorString();
    }

    private String banner(String key) {
        return I18n.translateToLocal(key);
    }

    private String tooltip(String key, Object... args) {
        return I18n.translateToLocalFormatted(key, args);
    }

    private List<IModifier> getDisplayModifiers() {
        if (blueprint == null) {
            return Collections.emptyList();
        }
        ItemStack filterStack = buildModifierFilterStack();
        if (filterStack.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, IModifier> filtered = new LinkedHashMap<>();
        for (IModifier modifier : blueprint.target.getAvailableModifiers()) {
            String id = modifier.getIdentifier();
            String name = TextFormatting.getTextWithoutFormattingCodes(modifier.getLocalizedName());
            if (name == null) {
                name = modifier.getLocalizedName();
            }
            String idLower = id == null ? "" : id.toLowerCase(Locale.ROOT);
            String nameLower = name == null ? "" : name.toLowerCase(Locale.ROOT);
            String classLower = modifier.getClass().getName().toLowerCase(Locale.ROOT);
            if (idLower.contains("extratrait") || idLower.contains("emboss") || idLower.contains("fortify") || idLower.contains("material") || idLower.contains("polished")) {
                continue;
            }
            if (classLower.contains("extratrait") || classLower.contains("fortifydisplay") || classLower.contains("modfortify") || classLower.contains("modpolished")) {
                continue;
            }
            if (name.contains("\u523b\u5370") || nameLower.contains("emboss") || name.startsWith("\u5f3a\u5316 (") || name.startsWith("\u5f3a\u5316\uff08") || nameLower.startsWith("strengthened (") || nameLower.startsWith("fortified") || name.startsWith("\u7814\u78e8 (") || name.startsWith("\u7814\u78e8\uff08") || nameLower.startsWith("polished")) {
                continue;
            }
            if (!PlannerDebugOptions.isDeleteMode() && PlannerDebugOptions.isDeletedModifier(id)) {
                continue;
            }
            if (!canApplyToFilterStack(modifier, filterStack)) {
                continue;
            }
            filtered.putIfAbsent(id, modifier);
        }
        return new ArrayList<>(filtered.values());
    }

    private List<ModifierListEntry> getModifierEntries() {
        List<ModifierListEntry> head = new ArrayList<>();
        List<ModifierListEntry> tail = new ArrayList<>();

        if (blueprint != null) {
            if (ToolLevelingCompat.isLoaded()) {
                head.add(new ToolLevelEntry());
            }
            List<PartMaterialType> parts = blueprint.target.getRequiredComponents();
            int activeEmbossPart = blueprint.hasEmboss() ? blueprint.embossPartIndex : -1;
            for (int i = 0; i < parts.size(); i++) {
                ModifierListEntry entry = new SpecialModifierEntry(SpecialModifierType.EMBOSS, i);
                if (activeEmbossPart >= 0 && i != activeEmbossPart) {
                    tail.add(entry);
                } else {
                    head.add(entry);
                }
            }
            if (blueprint.hasEmboss() && hasSecondEmbossModifiers()) {
                int activeSecondEmbossPart = blueprint.hasSecondEmboss() ? blueprint.secondEmbossPartIndex : -1;
                for (int i = 0; i < parts.size(); i++) {
                    ModifierListEntry entry = new SpecialModifierEntry(SpecialModifierType.EMBOSS2, i);
                    if (activeSecondEmbossPart >= 0 && i != activeSecondEmbossPart) {
                        tail.add(entry);
                    } else {
                        head.add(entry);
                    }
                }
            }
            head.add(new SpecialModifierEntry(type == PlannerTarget.TargetType.ARMOR ? SpecialModifierType.POLISHED : SpecialModifierType.FORTIFY, -1));
        }

        List<IModifier> modifiers = getDisplayModifiers();
        Map<String, Integer> stateRanks = new HashMap<>();
        modifiers.sort((left, right) -> {
            int leftState = stateRanks.computeIfAbsent(left.getIdentifier(), id -> modifierStateRank(left));
            int rightState = stateRanks.computeIfAbsent(right.getIdentifier(), id -> modifierStateRank(right));
            int state = Integer.compare(leftState, rightState);
            return state != 0 ? state : left.getLocalizedName().compareToIgnoreCase(right.getLocalizedName());
        });
        for (IModifier modifier : modifiers) {
            head.add(new NormalModifierEntry(modifier));
        }
        head.addAll(tail);
        return head;
    }

    private void refreshLayout() {
        initGui();
    }

    private boolean isCurrentTarget(int index) {
        return blueprint != null && index >= 0 && index < targets.size() && targets.get(index).getItem() == blueprint.target.getItem();
    }

    private void selectTarget(int index) {
        if (index >= 0 && index < targets.size()) {
            if (isCurrentTarget(index)) {
                clearTargetSelection();
                refreshLayout();
                return;
            }
            blueprint = new PlannerBlueprint(targets.get(index));
            selectedPart = -1;
            materialPage = 0;
            modifierPage = 0;
            traitPage = 0;
            activeSort = null;
            selectedTraitFilterId = null;
            materialSelectionMode = MaterialSelectionMode.NONE;
            refreshLayout();
        }
    }

    private void clearTargetSelection() {
        blueprint = null;
        selectedPart = -1;
        materialPage = 0;
        modifierPage = 0;
        traitPage = 0;
        activeSort = null;
        selectedTraitFilterId = null;
        materialSelectionMode = MaterialSelectionMode.NONE;
        if (rightPanelMode == RightPanelMode.MODIFIERS) {
            rightPanelMode = RightPanelMode.NONE;
        }
    }

    private void selectRandomTarget() {
        if (!targets.isEmpty()) {
            blueprint = new PlannerBlueprint(targets.get(random.nextInt(targets.size())));
            selectedPart = -1;
            materialPage = 0;
            modifierPage = 0;
            traitPage = 0;
            activeSort = null;
            selectedTraitFilterId = null;
            materialSelectionMode = MaterialSelectionMode.NONE;
        }
    }

    private void applyMaterial(int absoluteIndex) {
        if (blueprint == null) {
            return;
        }
        List<MaterialOption> options = getDisplayedMaterialOptions();
        if (absoluteIndex < 0 || absoluteIndex >= options.size()) {
            return;
        }
        MaterialOption option = options.get(absoluteIndex);
        switch (materialSelectionMode) {
            case PART:
                if (selectedPart >= 0 && selectedPart < blueprint.materials.length) {
                    blueprint.materials[selectedPart] = option.material;
                }
                break;
            case EMBOSS:
                if (selectedPart >= 0) {
                    IModifier modifier = getEmbossModifier(option.material, selectedPart);
                    if (modifier != null) {
                        blueprint.setEmboss(selectedPart, modifier.getIdentifier());
                    }
                }
                break;
            case EMBOSS2:
                if (selectedPart >= 0) {
                    IModifier modifier = getSecondEmbossModifier(option.material, selectedPart);
                    if (modifier != null) {
                        blueprint.setSecondEmboss(selectedPart, modifier.getIdentifier());
                    }
                }
                break;
            case SPECIAL_MATERIAL:
                IModifier specialModifier = getMaterialSpecialModifier(option.material);
                if (specialModifier != null) {
                    blueprint.setMaterialModifier(specialModifier.getIdentifier());
                }
                break;
            default:
                return;
        }
        selectedTraitFilterId = null;
        refreshLayout();
    }

    private void toggleSort(MaterialSortEntry sort) {
        if (getCurrentSorts().isEmpty()) {
            return;
        }
        activeSort = sort.equals(activeSort) ? null : sort;
        refreshLayout();
    }

    private void randomize() {
        if (blueprint == null) {
            selectRandomTarget();
        }
        if (blueprint == null) {
            return;
        }
        for (int i = 0; i < blueprint.materials.length; i++) {
            IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(i));
            List<Material> materials = getUsableMaterials(part);
            if (!materials.isEmpty()) {
                blueprint.materials[i] = materials.get(random.nextInt(materials.size()));
            }
        }
        blueprint.modifiers.clear();
        blueprint.clearEmboss();
        blueprint.clearMaterialModifier();
        ItemStack preview = blueprint.buildPreview();
        int availableSlots = preview.isEmpty() ? 0 : ToolHelper.getFreeModifiers(preview);
        int added = 0;
        while (!preview.isEmpty() && added < availableSlots) {
            List<IModifier> options = new ArrayList<>(getDisplayModifiers());
            options.removeIf(mod -> !blueprint.canAddModifier(mod));
            if (options.isEmpty()) {
                break;
            }
            if (blueprint.addModifier(options.get(random.nextInt(options.size())))) {
                added++;
            } else {
                break;
            }
            preview = blueprint.buildPreview();
        }
        refreshLayout();
    }

    private void toggleSave() {
        if (blueprint == null || !blueprint.isComplete()) {
            return;
        }
        if (isSaved(blueprint)) {
            savedBlueprints.removeIf(saved -> saved.equals(blueprint));
            if (starredBlueprint != null && starredBlueprint.equals(blueprint)) {
                starredBlueprint = null;
            }
        } else {
            savedBlueprints.add(blueprint.copy());
        }
        saveBookmarks();
        refreshLayout();
    }

    private void toggleRightPanel(RightPanelMode mode) {
        rightPanelMode = rightPanelMode == mode ? RightPanelMode.NONE : mode;
        bookmarkPage = 0;
        modifierPage = 0;
        refreshLayout();
    }

    private void giveCurrentItem() {
        if (mc.player == null || blueprint == null || !canUseGiveItem()) {
            return;
        }
        ItemStack output = blueprint.buildPreview();
        if (!output.isEmpty()) {
            PlannerNetwork.sendGiveItem(output);
        }
    }

    private boolean canUseGiveItem() {
        return mc.player != null && (!PlannerConfig.creativeOnlyGiveItem || mc.player.capabilities.isCreativeMode);
    }

    private void exportBlueprintCode() {
        if (blueprint == null || !blueprint.isComplete()) {
            return;
        }
        try {
            String code;
            if (PlannerConfig.enableShortBlueprintCode && exportShortMode) {
                PlannerShortCodeList list = PlannerShortCodeList.loadOrCreate(PlannerClientEvents.getDataFolder(), PlannerClientEvents.getAllTargets());
                if (list == null) {
                    printLocalText(I18n.translateToLocal("gui.tpa.short_list_missing"));
                    return;
                }
                code = PlannerBlueprintCodecs.exportShort(blueprint, list);
            } else {
                code = PlannerBlueprintCodecs.exportStandard(blueprint);
            }
            GuiScreen.setClipboardString(code);
            mc.ingameGUI.getChatGUI().printChatMessage(PlannerChatComponents.buildCodeMessage(
                blueprint.target.getDisplayName() + I18n.translateToLocal("gui.tpa.blueprint_code"),
                code,
                I18n.translateToLocal("gui.tpa.chat.copy"),
                I18n.translateToLocal("gui.tpa.chat.bookmark"),
                I18n.translateToLocal("gui.tpa.chat.share"),
                true
            ));
        } catch (Exception e) {
            printLocalText(e.getMessage() == null ? I18n.translateToLocal("gui.tpa.export_failed") : e.getMessage());
        }
    }

    private void printLocalText(String text) {
        mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.text.TextComponentString(text));
    }

    private String getBlueprintCodeTooltip(boolean complete) {
        if (!complete) {
            return I18n.translateToLocal("gui.tpa.import_code");
        }
        String base = I18n.translateToLocal("gui.tpa.export_code");
        if (PlannerConfig.enableShortBlueprintCode) {
            String mode = I18n.translateToLocal(exportShortMode ? "gui.tpa.code_mode_short" : "gui.tpa.code_mode_standard");
            return base + "\n" + TextFormatting.GRAY + mode + "\n" + TextFormatting.YELLOW + I18n.translateToLocal("gui.tpa.right_switch_mode");
        }
        return base;
    }

    private void importFromParent() {
        ItemStack stack = ItemStack.EMPTY;
        if (parent instanceof GuiToolStation) {
            stack = ((GuiToolStation) parent).inventorySlots.getSlot(0).getStack();
        } else if (parent instanceof GuiArmorStation) {
            stack = ((GuiArmorStation) parent).inventorySlots.getSlot(0).getStack();
        }
        if (stack.isEmpty()) {
            return;
        }
        for (PlannerTarget target : targets) {
            PlannerBlueprint imported = PlannerBlueprint.fromStack(target, stack);
            if (imported != null) {
                blueprint = imported;
                selectedPart = -1;
                activeSort = null;
                materialSelectionMode = MaterialSelectionMode.NONE;
                return;
            }
        }
    }

    private List<int[]> getPartPositions(PlannerTarget target) {
        List<int[]> positions = new ArrayList<>();
        if (target instanceof ToolPlannerTarget) {
            for (org.lwjgl.util.Point point : ((ToolPlannerTarget) target).getGuiInfo().positions) {
                positions.add(new int[] { point.getX(), point.getY() });
            }
            if (isBoltTarget(target) && target.getRequiredComponents().size() == 3 && positions.size() == 2) {
                int[] core = positions.get(0);
                int[] fletching = positions.get(1);
                positions.clear();
                positions.add(new int[] { core[0] - 9, core[1] });
                positions.add(new int[] { core[0] + 9, core[1] });
                positions.add(fletching);
            }
        } else if (target instanceof ArmorPlannerTarget) {
            for (org.lwjgl.util.Point point : ((ArmorPlannerTarget) target).getGuiInfo().positions) {
                positions.add(new int[] { point.getX(), point.getY() });
            }
        }
        return positions;
    }

    private boolean isBoltTarget(PlannerTarget target) {
        return target != null && target.getItem() != null && "slimeknights.tconstruct.tools.ranged.item.Bolt".equals(target.getItem().getClass().getName());
    }

    private IToolPart getDisplayPart(PartMaterialType partType) {
        for (IToolPart part : partType.getPossibleParts()) {
            return part;
        }
        return null;
    }

    private List<Material> getSortedMaterials(IToolPart part, PartMaterialType partType) {
        List<Material> materials = getUsableMaterials(part);
        if (activeSort != null) {
            materials.sort((left, right) -> activeSort.compare(getPrimaryStatType(partType), left, right));
        }
        return materials;
    }

    private List<Material> getUsableMaterials(IToolPart part) {
        List<Material> materials = new ArrayList<>();
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (part != null && part.canUseMaterial(material)) {
                materials.add(material);
            }
        }
        return materials;
    }

    private List<MaterialOption> getDisplayedMaterialOptions() {
        switch (materialSelectionMode) {
            case PART:
                return getPartMaterialOptions();
            case EMBOSS:
                return getEmbossMaterialOptions(false);
            case EMBOSS2:
                return getEmbossMaterialOptions(true);
            case SPECIAL_MATERIAL:
                return getSpecialMaterialOptions();
            default:
                return Collections.emptyList();
        }
    }

    private List<MaterialOption> getPartMaterialOptions() {
        if (blueprint == null || selectedPart < 0 || selectedPart >= blueprint.materials.length) {
            return Collections.emptyList();
        }
        PartMaterialType partType = blueprint.target.getRequiredComponents().get(selectedPart);
        IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(selectedPart));
        if (part == null) {
            return Collections.emptyList();
        }
        List<MaterialOption> options = new ArrayList<>();
        for (Material material : getSortedMaterials(part, partType)) {
            if (selectedTraitFilterId != null && !getPartTraitIdentifiers(partType, material).contains(selectedTraitFilterId)) {
                continue;
            }
            ItemStack tooltipStack = getPartDisplayStack(selectedPart, part, material);
            options.add(new MaterialOption(material, getDisplayedMaterialStack(material, tooltipStack), tooltipStack, material.getLocalizedName(), blueprint.materials[selectedPart] == material));
        }
        return options;
    }

    private boolean shouldShowTraitFilterPanel() {
        return blueprint != null && materialSelectionMode != MaterialSelectionMode.EMBOSS && materialSelectionMode != MaterialSelectionMode.EMBOSS2 && materialSelectionMode != MaterialSelectionMode.SPECIAL_MATERIAL;
    }

    private List<TraitFilterEntry> getCurrentPartTraits() {
        if (!shouldShowTraitFilterPanel()) {
            return Collections.emptyList();
        }
        if (selectedPart < 0 || selectedPart >= blueprint.materials.length) {
            return getAllMaterialTraits();
        }
        PartMaterialType partType = blueprint.target.getRequiredComponents().get(selectedPart);
        IToolPart part = getDisplayPart(partType);
        if (part == null) {
            return Collections.emptyList();
        }
        Map<String, TraitFilterEntry> traits = new LinkedHashMap<>();
        Set<String> traitLabels = new LinkedHashSet<>();
        Material selectedMaterial = blueprint.materials[selectedPart];
        if (selectedMaterial != null) {
            addMaterialTraits(traits, traitLabels, partType, selectedMaterial);
        } else {
            for (Material material : getUsableMaterials(part)) {
                addMaterialTraits(traits, traitLabels, partType, material);
            }
        }
        List<TraitFilterEntry> result = new ArrayList<>(traits.values());
        result.sort((left, right) -> left.label.compareToIgnoreCase(right.label));
        return result;
    }

    private List<TraitFilterEntry> getAllMaterialTraits() {
        Map<String, TraitFilterEntry> traits = new LinkedHashMap<>();
        Set<String> traitLabels = new LinkedHashSet<>();
        for (PartMaterialType partType : blueprint.target.getRequiredComponents()) {
            IToolPart part = getDisplayPart(partType);
            if (part == null) {
                continue;
            }
            for (Material material : getUsableMaterials(part)) {
                addMaterialTraits(traits, traitLabels, partType, material);
            }
        }
        List<TraitFilterEntry> result = new ArrayList<>(traits.values());
        result.sort((left, right) -> left.label.compareToIgnoreCase(right.label));
        return result;
    }

    private void addMaterialTraits(Map<String, TraitFilterEntry> traits, Set<String> traitLabels, PartMaterialType partType, Material material) {
        for (String statType : getUsedStatTypes(partType)) {
            for (ITrait trait : material.getAllTraitsForStats(statType)) {
                TraitFilterEntry entry = new TraitFilterEntry(trait);
                if (!trait.isHidden() && !traits.containsKey(entry.id) && traitLabels.add(entry.normalizedLabel)) {
                    traits.put(entry.id, entry);
                }
            }
        }
    }

    private ItemStack getPartDisplayStack(int partIndex, IToolPart part, Material material) {
        if (blueprint != null && isBoltTarget(blueprint.target) && part instanceof BoltCore && material != null) {
            if (partIndex == 0) {
                return BoltCore.getItemstackWithMaterials(material, TinkerMaterials.iron);
            }
            if (partIndex == 1) {
                return BoltCore.getItemstackWithMaterials(TinkerMaterials.reed, material);
            }
        }
        if (material == null) {
            return part.getOutlineRenderStack();
        }
        return part.getItemstackWithMaterial(material);
    }

    private List<MaterialOption> getEmbossMaterialOptions(boolean second) {
        if (blueprint == null || selectedPart < 0 || selectedPart >= blueprint.materials.length) {
            return Collections.emptyList();
        }
        IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(selectedPart));
        if (part == null) {
            return Collections.emptyList();
        }
        List<MaterialOption> options = new ArrayList<>();
        String selectedMaterialId = getModifierMaterialId(second ? blueprint.secondEmbossModifierId : blueprint.embossModifierId);
        PartMaterialType partType = blueprint.target.getRequiredComponents().get(selectedPart);
        for (Material material : getSortedMaterials(part, partType)) {
            IModifier modifier = second ? getSecondEmbossModifier(material, selectedPart) : getEmbossModifier(material, selectedPart);
            if (modifier != null) {
                boolean highlighted = second ? blueprint.hasSecondEmboss() && blueprint.secondEmbossPartIndex == selectedPart : blueprint.hasEmboss() && blueprint.embossPartIndex == selectedPart;
                ItemStack tooltipStack = part.getItemstackWithMaterial(material);
                options.add(new MaterialOption(material, getDisplayedMaterialStack(material, tooltipStack), tooltipStack, material.getLocalizedName(), highlighted && material.getIdentifier().equals(selectedMaterialId)));
            }
        }
        return options;
    }

    private List<MaterialOption> getSpecialMaterialOptions() {
        if (blueprint == null) {
            return Collections.emptyList();
        }
        List<MaterialOption> options = new ArrayList<>();
        String selectedMaterialId = getModifierMaterialId(blueprint.materialModifierId);
        for (IModifier modifier : getAvailableSpecialModifiers()) {
            if (!isMaterialSpecialModifier(modifier)) {
                continue;
            }
            Material material = getModifierMaterial(modifier);
            ItemStack stack = getSpecialMaterialDisplayStack(material);
            if (material != null && !stack.isEmpty() && isValidSpecialMaterialDisplay(material, stack)) {
                options.add(new MaterialOption(material, stack, stack, material.getLocalizedName(), material.getIdentifier().equals(selectedMaterialId)));
            }
        }
        return options;
    }

    private List<IModifier> getAvailableSpecialModifiers() {
        if (blueprint == null) {
            return Collections.emptyList();
        }
        ItemStack filterStack = buildModifierFilterStack();
        if (filterStack.isEmpty()) {
            return Collections.emptyList();
        }
        List<IModifier> result = new ArrayList<>();
        for (IModifier modifier : blueprint.target.getAvailableModifiers()) {
            if (isEmbossModifier(modifier) || isSecondEmbossModifier(modifier) || isMaterialSpecialModifier(modifier)) {
                result.add(modifier);
            }
        }
        return result;
    }

    private boolean hasSecondEmbossModifiers() {
        for (IModifier modifier : getAvailableSpecialModifiers()) {
            if (isSecondEmbossModifier(modifier)) {
                return true;
            }
        }
        return false;
    }

    private IModifier getEmbossModifier(Material material, int partIndex) {
        if (material == null) {
            return null;
        }
        for (IModifier modifier : getAvailableSpecialModifiers()) {
            if (isEmbossModifier(modifier)) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && material.getIdentifier().equals(modifierMaterial.getIdentifier()) && matchesEmbossPart(modifier, partIndex)) {
                    return modifier;
                }
            }
        }
        return null;
    }

    private IModifier getSecondEmbossModifier(Material material, int partIndex) {
        if (material == null) {
            return null;
        }
        for (IModifier modifier : getAvailableSpecialModifiers()) {
            if (isSecondEmbossModifier(modifier)) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && material.getIdentifier().equals(modifierMaterial.getIdentifier()) && matchesEmbossPart(modifier, partIndex)) {
                    return modifier;
                }
            }
        }
        return null;
    }

    private IModifier getMaterialSpecialModifier(Material material) {
        if (material == null) {
            return null;
        }
        for (IModifier modifier : getAvailableSpecialModifiers()) {
            if (isMaterialSpecialModifier(modifier)) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && material.getIdentifier().equals(modifierMaterial.getIdentifier())) {
                    return modifier;
                }
            }
        }
        return null;
    }

    private boolean isEmbossModifier(IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR ? className.contains("modextraarmortrait") && !className.contains("modextraarmortrait2") && !className.contains("display") : className.contains("modextratrait") && !className.contains("modextratrait2") && !className.contains("display");
    }

    private boolean isSecondEmbossModifier(IModifier modifier) {
        return MoreTConCompat.isSecondEmbossModifier(type, modifier);
    }

    private boolean isMaterialSpecialModifier(IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR ? className.contains("modpolished") && !className.contains("display") : className.contains("modfortify") && !className.contains("display");
    }

    private Material getModifierMaterial(IModifier modifier) {
        if (modifier == null) {
            return null;
        }
        Class<?> type = modifier.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (Material.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (Material) field.get(modifier);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private String getModifierMaterialId(String identifier) {
        if (identifier == null || identifier.isEmpty() || blueprint == null) {
            return "";
        }
        IModifier modifier = blueprint.target.resolveModifier(identifier);
        Material material = getModifierMaterial(modifier);
        return material == null ? "" : material.getIdentifier();
    }

    private String getModifierMaterialName(String identifier) {
        if (identifier == null || identifier.isEmpty() || blueprint == null) {
            return "";
        }
        IModifier modifier = blueprint.target.resolveModifier(identifier);
        Material material = getModifierMaterial(modifier);
        return material == null ? "" : material.getLocalizedName();
    }

    private ItemStack getPrimaryApplicationStack(IModifier modifier) {
        List<List<ItemStack>> items = Collections.emptyList();
        if (modifier instanceof ToolModifier) {
            items = ((ToolModifier) modifier).getItems();
        } else if (modifier instanceof ModifierTrait) {
            items = ((ModifierTrait) modifier).getItems();
        } else if (modifier instanceof ArmorModifier) {
            items = ((ArmorModifier) modifier).getItems();
        } else if (modifier instanceof ArmorModifierTrait) {
            items = ((ArmorModifierTrait) modifier).getItems();
        }
        if (!items.isEmpty() && !items.get(0).isEmpty()) {
            return items.get(0).get(0);
        }
        return ItemStack.EMPTY;
    }

    private boolean matchesEmbossPart(IModifier modifier, int partIndex) {
        if (blueprint == null || modifier == null || partIndex < 0 || partIndex >= blueprint.target.getRequiredComponents().size()) {
            return false;
        }
        Set<String> modifierTraits = getModifierTraitIdentifiers(modifier);
        Set<String> partTraits = getPartTraitIdentifiers(blueprint.target.getRequiredComponents().get(partIndex), getModifierMaterial(modifier));
        return !modifierTraits.isEmpty() && modifierTraits.equals(partTraits);
    }

    private Set<String> getModifierTraitIdentifiers(IModifier modifier) {
        Set<String> result = new LinkedHashSet<>();
        Class<?> type = modifier.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(modifier);
                    if (!(value instanceof Collection)) {
                        continue;
                    }
                    Collection<?> collection = (Collection<?>) value;
                    if (collection.isEmpty()) {
                        continue;
                    }
                    Object first = collection.iterator().next();
                    if (!(first instanceof ITrait)) {
                        continue;
                    }
                    for (Object entry : collection) {
                        result.add(((ITrait) entry).getIdentifier());
                    }
                    return result;
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private Set<String> getPartTraitIdentifiers(PartMaterialType partType, Material material) {
        Set<String> result = new LinkedHashSet<>();
        if (partType == null || material == null) {
            return result;
        }
        for (String statType : getUsedStatTypes(partType)) {
            for (ITrait trait : material.getAllTraitsForStats(statType)) {
                result.add(trait.getIdentifier());
            }
        }
        return result;
    }

    private List<String> getUsedStatTypes(PartMaterialType partType) {
        List<String> statTypes = new ArrayList<>();
        addUsedStatType(statTypes, partType, MaterialTypes.HEAD);
        addUsedStatType(statTypes, partType, MaterialTypes.HANDLE);
        addUsedStatType(statTypes, partType, MaterialTypes.EXTRA);
        addUsedStatType(statTypes, partType, MaterialTypes.BOW);
        addUsedStatType(statTypes, partType, MaterialTypes.BOWSTRING);
        addUsedStatType(statTypes, partType, MaterialTypes.SHAFT);
        addUsedStatType(statTypes, partType, MaterialTypes.FLETCHING);
        addUsedStatType(statTypes, partType, MaterialTypes.PROJECTILE);
        if (isYoyosLoaded()) {
            addUsedStatType(statTypes, partType, YOYO_BODY);
            addUsedStatType(statTypes, partType, YOYO_CORD);
            addUsedStatType(statTypes, partType, YOYO_AXLE);
        }
        addUsedStatType(statTypes, partType, ArmorMaterialType.CORE);
        addUsedStatType(statTypes, partType, ArmorMaterialType.PLATES);
        addUsedStatType(statTypes, partType, ArmorMaterialType.TRIM);
        return statTypes;
    }

    private void addUsedStatType(List<String> statTypes, PartMaterialType partType, String statType) {
        if (partType.usesStat(statType)) {
            statTypes.add(statType);
        }
    }

    private ItemStack getSpecialMaterialDisplayStack(Material material) {
        if (material == null) {
            return ItemStack.EMPTY;
        }
        if (type == PlannerTarget.TargetType.ARMOR) {
            return ConstructsRegistry.polishingKit != null ? ConstructsRegistry.polishingKit.getItemstackWithMaterial(material) : ItemStack.EMPTY;
        }
        return TinkerTools.sharpeningKit != null ? TinkerTools.sharpeningKit.getItemstackWithMaterial(material) : ItemStack.EMPTY;
    }

    private boolean isValidSpecialMaterialDisplay(Material material, ItemStack stack) {
        if (material == null || stack.isEmpty() || material.isHidden() || material.renderInfo == null) {
            return false;
        }
        if (type == PlannerTarget.TargetType.ARMOR) {
            if (ConstructsRegistry.polishingKit == null || !ConstructsRegistry.polishingKit.canUseMaterial(material) || !material.hasStats(ArmorMaterialType.PLATES)) {
                return false;
            }
        } else {
            if (TinkerTools.sharpeningKit == null || !TinkerTools.sharpeningKit.canUseMaterial(material) || !material.hasStats(MaterialTypes.HEAD)) {
                return false;
            }
        }
        for (String line : stack.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL)) {
            String lower = TextFormatting.getTextWithoutFormattingCodes(line);
            if (lower == null) {
                continue;
            }
            lower = lower.toLowerCase(Locale.ROOT);
            if (lower.contains("missing material") || lower.contains("缺失材料")) {
                return false;
            }
        }
        return true;
    }

    private ItemStack getDisplayedMaterialStack(Material material, ItemStack fallback) {
        if (!useRepresentativeMaterialIcons || material == null) {
            return fallback;
        }
        ItemStack representative = getRepresentativeMaterialStack(material);
        return representative.isEmpty() ? fallback : representative;
    }

    private ItemStack getRepresentativeMaterialStack(Material material) {
        if (material == null) {
            return ItemStack.EMPTY;
        }
        for (String methodName : new String[] { "getRepresentativeItem", "getRepresentativeStack" }) {
            try {
                Method method = material.getClass().getMethod(methodName);
                Object value = method.invoke(material);
                if (value instanceof ItemStack && !((ItemStack) value).isEmpty()) {
                    return ((ItemStack) value).copy();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Class<?> type = material.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!ItemStack.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(material);
                    if (value instanceof ItemStack && !((ItemStack) value).isEmpty()) {
                        return ((ItemStack) value).copy();
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return ItemStack.EMPTY;
    }

    private String getPrimaryStatTypeFromPart(IToolPart part) {
        if (part == null) {
            return MaterialTypes.HEAD;
        }
        if (isYoyosLoaded()) {
            if (part.hasUseForStat(YOYO_BODY)) {
                return YOYO_BODY;
            }
            if (part.hasUseForStat(YOYO_CORD)) {
                return YOYO_CORD;
            }
            if (part.hasUseForStat(YOYO_AXLE)) {
                return YOYO_AXLE;
            }
        }
        if (part.hasUseForStat(MaterialTypes.HANDLE)) {
            return MaterialTypes.HANDLE;
        }
        if (part.hasUseForStat(MaterialTypes.HEAD)) {
            return MaterialTypes.HEAD;
        }
        if (part.hasUseForStat(MaterialTypes.EXTRA)) {
            return MaterialTypes.EXTRA;
        }
        if (part.hasUseForStat(MaterialTypes.BOW)) {
            return MaterialTypes.BOW;
        }
        if (part.hasUseForStat(MaterialTypes.BOWSTRING)) {
            return MaterialTypes.BOWSTRING;
        }
        if (part.hasUseForStat(MaterialTypes.SHAFT)) {
            return MaterialTypes.SHAFT;
        }
        if (part.hasUseForStat(MaterialTypes.FLETCHING)) {
            return MaterialTypes.FLETCHING;
        }
        if (part.hasUseForStat(MaterialTypes.PROJECTILE)) {
            return MaterialTypes.PROJECTILE;
        }
        if (part.hasUseForStat(ArmorMaterialType.CORE)) {
            return ArmorMaterialType.CORE;
        }
        if (part.hasUseForStat(ArmorMaterialType.PLATES)) {
            return ArmorMaterialType.PLATES;
        }
        if (part.hasUseForStat(ArmorMaterialType.TRIM)) {
            return ArmorMaterialType.TRIM;
        }
        return MaterialTypes.HEAD;
    }

    private String getPrimaryStatType(PartMaterialType partType) {
        if (isYoyosLoaded()) {
            if (partType.usesStat(YOYO_BODY)) {
                return YOYO_BODY;
            }
            if (partType.usesStat(YOYO_CORD)) {
                return YOYO_CORD;
            }
            if (partType.usesStat(YOYO_AXLE)) {
                return YOYO_AXLE;
            }
        }
        if (partType.usesStat(ArmorMaterialType.CORE)) {
            return ArmorMaterialType.CORE;
        }
        if (partType.usesStat(ArmorMaterialType.PLATES)) {
            return ArmorMaterialType.PLATES;
        }
        if (partType.usesStat(ArmorMaterialType.TRIM)) {
            return ArmorMaterialType.TRIM;
        }
        if (partType.usesStat(MaterialTypes.HANDLE)) {
            return MaterialTypes.HANDLE;
        }
        if (partType.usesStat(MaterialTypes.HEAD)) {
            return MaterialTypes.HEAD;
        }
        if (partType.usesStat(MaterialTypes.EXTRA)) {
            return MaterialTypes.EXTRA;
        }
        if (partType.usesStat(MaterialTypes.BOW)) {
            return MaterialTypes.BOW;
        }
        if (partType.usesStat(MaterialTypes.BOWSTRING)) {
            return MaterialTypes.BOWSTRING;
        }
        if (partType.usesStat(MaterialTypes.SHAFT)) {
            return MaterialTypes.SHAFT;
        }
        if (partType.usesStat(MaterialTypes.FLETCHING)) {
            return MaterialTypes.FLETCHING;
        }
        if (partType.usesStat(MaterialTypes.PROJECTILE)) {
            return MaterialTypes.PROJECTILE;
        }
        return MaterialTypes.HEAD;
    }

    private List<MaterialSortEntry> getCurrentSorts() {
        List<MaterialSortEntry> sorts = new ArrayList<>();
        if (blueprint == null || selectedPart < 0 || selectedPart >= blueprint.materials.length || materialSelectionMode == MaterialSelectionMode.NONE || materialSelectionMode == MaterialSelectionMode.SPECIAL_MATERIAL) {
            return sorts;
        }
        PartMaterialType selectedPartType = blueprint.target.getRequiredComponents().get(selectedPart);
        if (isYoyosLoaded() && selectedPartType.usesStat(YOYO_BODY)) {
            sorts.add(MaterialSortEntry.YOYO_BODY_ATTACK);
            sorts.add(MaterialSortEntry.YOYO_BODY_WEIGHT);
            sorts.add(MaterialSortEntry.YOYO_BODY_DURABILITY);
        } else if (isYoyosLoaded() && selectedPartType.usesStat(YOYO_CORD)) {
            sorts.add(MaterialSortEntry.YOYO_FRICTION);
            sorts.add(MaterialSortEntry.YOYO_LENGTH);
        } else if (isYoyosLoaded() && selectedPartType.usesStat(YOYO_AXLE)) {
            sorts.add(MaterialSortEntry.YOYO_FRICTION);
            sorts.add(MaterialSortEntry.YOYO_AXLE_MODIFIER);
        } else if (selectedPartType.usesStat(ArmorMaterialType.CORE)) {
            sorts.add(MaterialSortEntry.ARMOR_DEFENSE);
            sorts.add(MaterialSortEntry.ARMOR_DURABILITY);
        } else if (selectedPartType.usesStat(ArmorMaterialType.PLATES)) {
            sorts.add(MaterialSortEntry.ARMOR_TOUGHNESS);
            sorts.add(MaterialSortEntry.ARMOR_DURABILITY);
        } else if (selectedPartType.usesStat(ArmorMaterialType.TRIM)) {
            sorts.add(MaterialSortEntry.ARMOR_DURABILITY);
        } else if (type == PlannerTarget.TargetType.TOOL && selectedPartType.usesStat(MaterialTypes.HEAD)) {
            sorts.add(MaterialSortEntry.HEAD_ATTACK);
            sorts.add(MaterialSortEntry.HEAD_SPEED);
            sorts.add(MaterialSortEntry.HEAD_HARVEST);
            sorts.add(MaterialSortEntry.DURABILITY);
        } else if (type == PlannerTarget.TargetType.TOOL && selectedPartType.usesStat(MaterialTypes.EXTRA)) {
            sorts.add(MaterialSortEntry.DURABILITY);
        } else if (type == PlannerTarget.TargetType.TOOL && hasDurabilityModifier(selectedPartType)) {
            sorts.add(MaterialSortEntry.MODIFIER);
            sorts.add(MaterialSortEntry.DURABILITY);
        }
        addAutomaticSorts(sorts, selectedPartType);
        return sorts;
    }

    private boolean hasDurabilityModifier(PartMaterialType partType) {
        return partType.usesStat(MaterialTypes.HANDLE) || partType.usesStat(MaterialTypes.BOWSTRING) || partType.usesStat(MaterialTypes.SHAFT) || partType.usesStat(MaterialTypes.FLETCHING);
    }

    private boolean isYoyosLoaded() {
        return Loader.isModLoaded(YOYOS_MODID);
    }

    private void addAutomaticSorts(List<MaterialSortEntry> sorts, PartMaterialType partType) {
        Set<String> existingIds = new LinkedHashSet<>();
        Set<String> existingLabels = new LinkedHashSet<>();
        for (MaterialSortEntry sort : sorts) {
            existingIds.add(sort.id);
            existingLabels.add(normalizeSortLabel(sort.getLabel()));
        }
        for (String statType : getDeclaredStatTypes(partType)) {
            if (BUILT_IN_SORT_STAT_TYPES.contains(statType)) {
                continue;
            }
            IMaterialStats sample = findSampleStats(partType, statType);
            if (sample == null) {
                continue;
            }
            Set<String> knownFields = getSupportedFieldNames(statType);
            for (Field field : sample.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!isNumericFieldType(field.getType())) {
                    continue;
                }
                if (!knownFields.isEmpty() && !knownFields.contains(field.getName())) {
                    continue;
                }
                String id = statType + "|" + field.getName();
                if (!existingIds.add(id)) {
                    continue;
                }
                MaterialSortEntry sort = MaterialSortEntry.automatic(statType, field.getName(), sample);
                if (existingLabels.add(normalizeSortLabel(sort.getLabel()))) {
                    sorts.add(sort);
                }
            }
        }
    }

    private String normalizeSortLabel(String label) {
        String cleaned = TextFormatting.getTextWithoutFormattingCodes(label);
        return cleaned == null ? "" : cleaned.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> getSupportedFieldNames(String statType) {
        Set<String> fields = new LinkedHashSet<>();
        MaterialSortEntry prototype = MaterialSortEntry.prototypical(statType);
        if (prototype != null) {
            fields.addAll(prototype.getFieldNames());
        }
        return fields;
    }

    private IMaterialStats findSampleStats(PartMaterialType partType, String statType) {
        IToolPart part = getDisplayPart(partType);
        for (Material material : getUsableMaterials(part)) {
            IMaterialStats stats = material.getStats(statType);
            if (stats != null) {
                return stats;
            }
        }
        return null;
    }

    private boolean isNumericFieldType(Class<?> type) {
        return type == byte.class || type == short.class || type == int.class || type == long.class || type == float.class || type == double.class || Number.class.isAssignableFrom(type);
    }

    private List<String> getDeclaredStatTypes(PartMaterialType partType) {
        List<String> statTypes = new ArrayList<>();
        Class<?> type = partType.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("neededTypes");
                field.setAccessible(true);
                Object value = field.get(partType);
                if (value instanceof String[]) {
                    Collections.addAll(statTypes, (String[]) value);
                }
                return statTypes;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return statTypes;
            }
        }
        return getUsedStatTypes(partType);
    }

    private int modifierStateRank(IModifier modifier) {
        if (blueprint == null) {
            return 2;
        }
        if (PlannerDebugOptions.isDeleteMode() && PlannerDebugOptions.isDeletedModifier(modifier.getIdentifier())) {
            return 3;
        }
        if (blueprint.getModifierLevel(modifier.getIdentifier()) > 0) {
            return 0;
        }
        return blueprint.canAddModifier(modifier) ? 1 : 2;
    }

    private ItemStack buildModifierFilterStack() {
        if (blueprint == null) {
            return ItemStack.EMPTY;
        }
        List<Material> materials = new ArrayList<>();
        List<PartMaterialType> parts = blueprint.target.getRequiredComponents();
        for (int i = 0; i < parts.size(); i++) {
            Material material = i < blueprint.materials.length ? blueprint.materials[i] : null;
            if (material == null) {
                IToolPart part = getDisplayPart(parts.get(i));
                List<Material> usable = getUsableMaterials(part);
                if (usable.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                material = usable.get(0);
            }
            materials.add(material);
        }
        ItemStack stack = blueprint.target.build(materials);
        ToolLevelingCompat.applyLevel(stack, blueprint.toolLevel);
        return stack;
    }

    private boolean canApplyToFilterStack(IModifier modifier, ItemStack filterStack) {
        try {
            return modifier.canApply(filterStack.copy(), filterStack);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void onGuiClosed() {
        saveBookmarks();
    }

    private boolean isSaved(PlannerBlueprint blueprint) {
        for (PlannerBlueprint saved : savedBlueprints) {
            if (saved.equals(blueprint)) {
                return true;
            }
        }
        return false;
    }

    private void saveBookmarks() {
        data.save(type, savedBlueprints, starredBlueprint);
    }

    GuiScreen getParentScreen() {
        return parent;
    }

    static PlannerScreen forImportedBlueprint(GuiScreen parent, PlannerBlueprint blueprint) {
        PlannerScreen screen = new PlannerScreen(parent, blueprint.target.getType(), blueprint.target.getType() == PlannerTarget.TargetType.ARMOR ? PlannerClientEvents.getArmorTargets() : PlannerClientEvents.getToolTargets());
        screen.blueprint = blueprint.copy();
        screen.selectedPart = -1;
        screen.materialPage = 0;
        screen.modifierPage = 0;
        screen.bookmarkPage = 0;
        screen.activeSort = null;
        screen.materialSelectionMode = MaterialSelectionMode.NONE;
        return screen;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == mc.gameSettings.keyBindInventory.getKeyCode() || keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private class PanelItemButton extends GuiButton {
        private final ItemStack stack;
        private final ItemStack tooltipStack;
        private final String tooltip;
        private final boolean highlighted;
        private final boolean simpleTooltip;

        private PanelItemButton(int id, int panelLeft, int panelTop, int relX, int relY, ItemStack stack, ItemStack tooltipStack, String tooltip, boolean highlighted, boolean simpleTooltip) {
            super(id, panelLeft + relX, panelTop + relY, 18, 18, "");
            this.stack = stack;
            this.tooltipStack = tooltipStack;
            this.tooltip = tooltip;
            this.highlighted = highlighted;
            this.simpleTooltip = simpleTooltip;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GlStateManager.pushMatrix();
            mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableDepth();
            drawTexturedModalRect(x, y, 213, 41 + ((highlighted || hovered) ? 18 : 0), 18, 18);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
            if (hovered) {
                if (simpleTooltip) {
                    deferTooltip(Collections.singletonList(tooltip));
                } else {
                    deferTooltip(tooltipStack.isEmpty() ? stack : tooltipStack);
                }
            }
        }
    }

    private class PlannerActionButton extends GuiButton {
        private final ResourceLocation iconTexture;
        private final ItemStack iconStack;
        private final String tooltip;
        private final float alpha;
        private final PressHandler handler;

        private PlannerActionButton(int x, int y, ResourceLocation icon, String tooltip, boolean enabled, float alpha, PressHandler handler) {
            super(0, x, y, 16, 16, "");
            this.iconTexture = icon;
            this.iconStack = ItemStack.EMPTY;
            this.tooltip = tooltip;
            this.enabled = enabled;
            this.alpha = alpha;
            this.handler = handler;
        }

        private PlannerActionButton(int x, int y, ItemStack icon, String tooltip, boolean enabled, float alpha, PressHandler handler) {
            super(0, x, y, 16, 16, "");
            this.iconTexture = null;
            this.iconStack = icon;
            this.tooltip = tooltip;
            this.enabled = enabled;
            this.alpha = alpha;
            this.handler = handler;
        }

        private void press(int mouseButton) {
            if (enabled) {
                handler.press(mouseButton);
            }
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            if (iconTexture != null) {
                RenderHelper.disableStandardItemLighting();
                mc.getTextureManager().bindTexture(iconTexture);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1F, 1F, 1F, enabled ? (hovered ? 1.0F : alpha) : 0.35F);
                drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
                GlStateManager.color(1F, 1F, 1F, 1F);
            } else if (!iconStack.isEmpty()) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1F, 1F, 1F, enabled ? (hovered ? 1.0F : alpha) : 0.35F);
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(iconStack, x, y);
                RenderHelper.disableStandardItemLighting();
                if (enabled && !hovered && alpha < 1.0F) {
                    drawRect(x, y, x + width, y + height, 0x8A000000);
                }
                GlStateManager.color(1F, 1F, 1F, 1F);
            }
            if (hovered) {
                deferTooltip(expandTooltipLines(tooltip, null));
            }
        }
    }

    private class TraitFilterButton extends GuiButton {
        private final TraitFilterEntry entry;

        private TraitFilterButton(int x, int y, int width, int height, TraitFilterEntry entry) {
            super(0, x, y, width, height, "");
            this.entry = entry;
        }

        private void press() {
            selectedTraitFilterId = entry.id.equals(selectedTraitFilterId) ? null : entry.id;
            materialPage = 0;
            refreshLayout();
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            boolean selected = entry.id.equals(selectedTraitFilterId);
            int color = selected ? (hovered ? 0xA06F86A8 : 0x905E7698) : (hovered ? 0x80708098 : 0x60425068);
            drawRect(x, y, x + width, y + height, color);
            String text = fontRenderer.trimStringToWidth(entry.label, width - 6);
            fontRenderer.drawString(text, x + 3, y + 3, selected ? 0xFFFFA0 : 0xFFFFFF);
            if (hovered) {
                List<String> tips = new ArrayList<>();
                tips.add(entry.label);
                tips.addAll(expandTooltipLines(entry.description, TextFormatting.GRAY));
                deferTooltip(tips);
            }
        }
    }

    private interface PressHandler {
        void press(int mouseButton);
    }

    private enum SpecialModifierType {
        EMBOSS("modifier.extratrait.name"),
        EMBOSS2("gui.tpa.special.emboss2"),
        FORTIFY("modifier.fortify.name"),
        POLISHED("modifier.polished_armor.name");

        private final String labelKey;

        SpecialModifierType(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private static final class MaterialOption {
        private final Material material;
        private final ItemStack displayStack;
        private final ItemStack tooltipStack;
        private final String tooltip;
        private final boolean highlighted;

        private MaterialOption(Material material, ItemStack displayStack, ItemStack tooltipStack, String tooltip, boolean highlighted) {
            this.material = material;
            this.displayStack = displayStack;
            this.tooltipStack = tooltipStack;
            this.tooltip = tooltip;
            this.highlighted = highlighted;
        }
    }

    private static final class TraitFilterEntry {
        private final String id;
        private final String label;
        private final String normalizedLabel;
        private final String description;

        private TraitFilterEntry(ITrait trait) {
            this.id = trait.getIdentifier();
            this.label = trait.getLocalizedName();
            this.normalizedLabel = TextFormatting.getTextWithoutFormattingCodes(label).trim().toLowerCase(Locale.ROOT);
            this.description = trait.getLocalizedDesc();
        }
    }

    private interface ModifierListEntry {
        ItemStack getIcon();
        String getLabel();
        String getValue();
        int getTextColor();
        List<String> getTooltip();
        void press(int mouseButton);
    }

    private class ToolLevelEntry implements ModifierListEntry {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(net.minecraft.init.Items.EXPERIENCE_BOTTLE);
        }

        @Override
        public String getLabel() {
            return I18n.translateToLocal("gui.tpa.level");
        }

        @Override
        public String getValue() {
            return String.valueOf(Math.max(0, blueprint == null ? 0 : blueprint.toolLevel));
        }

        @Override
        public int getTextColor() {
            return 0xFFFFFF;
        }

        @Override
        public List<String> getTooltip() {
            List<String> tips = new ArrayList<>();
            tips.add(getLabel());
            tips.add(TextFormatting.GREEN + I18n.translateToLocal("gui.tpa.left_add_level"));
            tips.add(TextFormatting.YELLOW + I18n.translateToLocal("gui.tpa.right_remove_level"));
            return tips;
        }

        @Override
        public void press(int mouseButton) {
            if (blueprint == null) {
                return;
            }
            if (mouseButton == 1) {
                blueprint.toolLevel = Math.max(0, blueprint.toolLevel - 1);
            } else if (ToolLevelingCompat.isLoaded() && ToolLevelingCompat.canLevelUp(blueprint.buildPreview(), blueprint.toolLevel + 1)) {
                blueprint.toolLevel++;
            }
        }
    }

    private class NormalModifierEntry implements ModifierListEntry {
        private final IModifier modifier;

        private NormalModifierEntry(IModifier modifier) {
            this.modifier = modifier;
        }

        private boolean isDebugDeleted() {
            return PlannerDebugOptions.isDeleteMode() && PlannerDebugOptions.isDeletedModifier(modifier.getIdentifier());
        }

        @Override
        public ItemStack getIcon() {
            return getModifierDisplayStack(modifier);
        }

        @Override
        public String getLabel() {
            return modifier.getLocalizedName();
        }

        @Override
        public String getValue() {
            int level = blueprint == null ? 0 : blueprint.getModifierLevel(modifier.getIdentifier());
            return level > 0 ? String.valueOf(level) : "";
        }

        @Override
        public int getTextColor() {
            if (isDebugDeleted()) {
                return 0x707070;
            }
            return blueprint != null && blueprint.canAddModifier(modifier) ? 0xFFFFFF : 0xD8C8C8;
        }

        @Override
        public List<String> getTooltip() {
            boolean canAdd = blueprint != null && blueprint.canAddModifier(modifier);
            int level = blueprint == null ? 0 : blueprint.getModifierLevel(modifier.getIdentifier());
            List<String> tips = new ArrayList<>();
            String modifierColor = getModifierTooltipColor(modifier);
            tips.add(modifierColor + modifier.getLocalizedName());
            tips.addAll(expandTooltipLines(modifierColor + modifier.getLocalizedDesc(), TextFormatting.GRAY));
            tips.add((canAdd ? TextFormatting.GREEN : TextFormatting.RED) + I18n.translateToLocal("gui.tpa.left_add"));
            tips.add((level > 0 ? TextFormatting.YELLOW : TextFormatting.DARK_GRAY) + I18n.translateToLocal("gui.tpa.right_remove"));
            return tips;
        }

        @Override
        public void press(int mouseButton) {
            if (blueprint == null) {
                return;
            }
            if (PlannerDebugOptions.isDeleteMode()) {
                if (mouseButton == 1 && PlannerDebugOptions.isDeletedModifier(modifier.getIdentifier())) {
                    PlannerDebugOptions.unmarkDeletedModifier(modifier.getIdentifier());
                } else {
                    PlannerDebugOptions.markDeletedModifier(modifier.getIdentifier());
                }
                return;
            }
            if (mouseButton == 1) {
                blueprint.removeModifier(modifier.getIdentifier());
            } else {
                blueprint.addModifier(modifier);
            }
        }
    }

    private boolean isDebugDeletedEntry(ModifierListEntry entry) {
        return entry instanceof NormalModifierEntry && ((NormalModifierEntry) entry).isDebugDeleted();
    }

    private class SpecialModifierEntry implements ModifierListEntry {
        private final SpecialModifierType specialType;
        private final int partIndex;

        private SpecialModifierEntry(SpecialModifierType specialType, int partIndex) {
            this.specialType = specialType;
            this.partIndex = partIndex;
        }

        @Override
        public ItemStack getIcon() {
            if (isEmbossType() && blueprint != null && partIndex >= 0 && partIndex < blueprint.target.getRequiredComponents().size()) {
                IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(partIndex));
                if (part != null) {
                    if (hasCurrentEmboss()) {
                        IModifier modifier = blueprint.target.resolveModifier(getCurrentEmbossModifierId());
                        Material material = getModifierMaterial(modifier);
                        if (material != null) {
                            return part.getItemstackWithMaterial(material);
                        }
                    }
                    return part.getOutlineRenderStack();
                }
            }
            if (specialType == SpecialModifierType.FORTIFY && TinkerTools.sharpeningKit != null) {
                IModifier modifier = blueprint == null ? null : blueprint.target.resolveModifier(blueprint.materialModifierId);
                Material material = getModifierMaterial(modifier);
                return material != null ? TinkerTools.sharpeningKit.getItemstackWithMaterial(material) : new ItemStack(TinkerTools.sharpeningKit);
            }
            if (specialType == SpecialModifierType.POLISHED && ConstructsRegistry.polishingKit != null) {
                IModifier modifier = blueprint == null ? null : blueprint.target.resolveModifier(blueprint.materialModifierId);
                Material material = getModifierMaterial(modifier);
                return material != null ? ConstructsRegistry.polishingKit.getItemstackWithMaterial(material) : new ItemStack(ConstructsRegistry.polishingKit);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public String getLabel() {
            if (isEmbossType() && blueprint != null && partIndex >= 0 && partIndex < blueprint.target.getRequiredComponents().size()) {
                if ("en_us".equals(mc.gameSettings.language)) {
                    return specialType == SpecialModifierType.EMBOSS2 ? "EBM II" : "EBM";
                }
                if (hasCurrentEmboss()) {
                    return I18n.translateToLocal(specialType.labelKey);
                }
                IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(partIndex));
                return I18n.translateToLocal(specialType.labelKey) + ": " + (part == null ? partIndex : part.getOutlineRenderStack().getDisplayName());
            }
            return I18n.translateToLocal(specialType.labelKey);
        }

        @Override
        public String getValue() {
            if (blueprint == null) {
                return "";
            }
            if (isEmbossType()) {
                return hasCurrentEmboss() ? getModifierMaterialName(getCurrentEmbossModifierId()) : "";
            }
            return blueprint.hasMaterialModifier() ? getModifierMaterialName(blueprint.materialModifierId) : "";
        }

        @Override
        public int getTextColor() {
            return 0xFFFFFF;
        }

        @Override
        public List<String> getTooltip() {
            List<String> tips = new ArrayList<>();
            tips.add(getTooltipLabel());
            String value = getValue();
            if (!value.isEmpty()) {
                tips.add(TextFormatting.GRAY + value);
                tips.add(TextFormatting.YELLOW + I18n.translateToLocal("gui.tpa.right_remove"));
            }
            tips.add(TextFormatting.GREEN + I18n.translateToLocal("gui.tpa.left_select_part"));
            return tips;
        }

        private String getTooltipLabel() {
            if (isEmbossType() && blueprint != null && partIndex >= 0 && partIndex < blueprint.target.getRequiredComponents().size()) {
                if (hasCurrentEmboss()) {
                    return I18n.translateToLocal(specialType.labelKey);
                }
                IToolPart part = getDisplayPart(blueprint.target.getRequiredComponents().get(partIndex));
                return I18n.translateToLocal(specialType.labelKey) + ": " + (part == null ? partIndex : part.getOutlineRenderStack().getDisplayName());
            }
            return getLabel();
        }

        @Override
        public void press(int mouseButton) {
            if (blueprint == null) {
                return;
            }
            if (mouseButton == 1) {
                if (specialType == SpecialModifierType.EMBOSS && blueprint.hasEmboss() && blueprint.embossPartIndex == partIndex) {
                    blueprint.clearEmboss();
                } else if (specialType == SpecialModifierType.EMBOSS2 && blueprint.hasSecondEmboss() && blueprint.secondEmbossPartIndex == partIndex) {
                    blueprint.clearSecondEmboss();
                } else if (!isEmbossType() && blueprint.hasMaterialModifier()) {
                    blueprint.clearMaterialModifier();
                }
                return;
            }
            materialPage = 0;
            activeSort = null;
            if (specialType == SpecialModifierType.EMBOSS) {
                selectedPart = partIndex;
                materialSelectionMode = MaterialSelectionMode.EMBOSS;
            } else if (specialType == SpecialModifierType.EMBOSS2) {
                selectedPart = partIndex;
                materialSelectionMode = MaterialSelectionMode.EMBOSS2;
            } else {
                selectedPart = -1;
                materialSelectionMode = MaterialSelectionMode.SPECIAL_MATERIAL;
            }
        }

        private boolean isEmbossType() {
            return specialType == SpecialModifierType.EMBOSS || specialType == SpecialModifierType.EMBOSS2;
        }

        private boolean hasCurrentEmboss() {
            return blueprint != null && (specialType == SpecialModifierType.EMBOSS ? blueprint.hasEmboss() && blueprint.embossPartIndex == partIndex : blueprint.hasSecondEmboss() && blueprint.secondEmbossPartIndex == partIndex);
        }

        private String getCurrentEmbossModifierId() {
            return specialType == SpecialModifierType.EMBOSS2 ? blueprint.secondEmbossModifierId : blueprint.embossModifierId;
        }
    }

    private class PanelModifierButton extends GuiButton {
        private final ModifierListEntry entry;

        private PanelModifierButton(int id, int x, int y, int width, int height, ModifierListEntry entry) {
            super(id, x, y, width, height, "");
            this.entry = entry;
        }

        private void press(int mouseButton) {
            entry.press(mouseButton);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            boolean debugDeleted = isDebugDeletedEntry(entry);
            int color = debugDeleted ? (hovered ? 0x80505058 : 0x80383840) : (hovered ? 0x808090A8 : 0x80525E73);
            drawRect(x, y, x + width, y + height, color);
            ItemStack icon = entry.getIcon();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(icon, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
            String value = entry.getValue();
            int maxValueWidth = entry instanceof SpecialModifierEntry ? 52 : 34;
            int valueWidth = value.isEmpty() ? 0 : Math.min(fontRenderer.getStringWidth(value), maxValueWidth);
            if (!value.isEmpty()) {
                String shownValue = fontRenderer.trimStringToWidth(value, maxValueWidth);
                fontRenderer.drawString(shownValue, x + width - 4 - fontRenderer.getStringWidth(shownValue), y + 5, debugDeleted ? 0x808080 : 0xFFD070);
            }
            int availableLabelWidth = width - 26 - valueWidth - (valueWidth > 0 ? 4 : 0);
            String label = entry.getLabel();
            String text = fontRenderer.trimStringToWidth(label, availableLabelWidth);
            fontRenderer.drawString(text, x + 20, y + 5, entry.getTextColor());
            if (hovered) {
                deferTooltip(entry.getTooltip());
            }
        }
    }

    private ItemStack getModifierDisplayStack(IModifier modifier) {
        List<List<ItemStack>> items = Collections.emptyList();
        if (modifier instanceof ToolModifier) {
            items = ((ToolModifier) modifier).getItems();
        } else if (modifier instanceof ProjectileModifierTrait) {
            items = ((ProjectileModifierTrait) modifier).getItems();
        } else if (modifier instanceof ModifierTrait) {
            items = ((ModifierTrait) modifier).getItems();
        } else if (modifier instanceof ArmorModifier) {
            items = ((ArmorModifier) modifier).getItems();
        } else if (modifier instanceof ArmorModifierTrait) {
            items = ((ArmorModifierTrait) modifier).getItems();
        }
        for (List<ItemStack> group : items) {
            for (ItemStack stack : group) {
                ItemStack resolved = resolveRenderableStack(stack);
                if (!resolved.isEmpty()) {
                    return resolved;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack resolveRenderableStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.getMetadata() != OreDictionary.WILDCARD_VALUE) {
            return stack;
        }
        NonNullList<ItemStack> subItems = NonNullList.create();
        stack.getItem().getSubItems(CreativeTabs.SEARCH, subItems);
        for (ItemStack subItem : subItems) {
            if (!subItem.isEmpty() && subItem.getItem() == stack.getItem()) {
                return subItem;
            }
        }
        return stack;
    }

    private class TabButton extends GuiButton {
        private TabButton(int id, int x, int y, String text) {
            super(id, x, y, TAB_W, TAB_H, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = enabled && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            mc.getTextureManager().bindTexture(enabled ? ICON_TAB_BUTTON_LIGHT : ICON_TAB_BUTTON_DARK);
            GlStateManager.color(1F, 1F, 1F, 1F);
            drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
            int color = enabled ? 0xFFFFFF : 0xD0D0FF;
            if (hovered) {
                color = 0xFFFFA0;
            }
            PlannerScreen.this.drawCenteredString(fontRenderer, displayString, x + width / 2, y + 6, color);
        }
    }

    private static class PageButton extends GuiButton {
        private final boolean right;

        private PageButton(int id, int x, int y, boolean right) {
            super(id, x, y, 38, 20, "");
            this.right = right;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1F, 1F, 1F, enabled ? 1F : 0.35F);
            drawTexturedModalRect(x, y, right ? 176 : 214, enabled ? 20 : 0, 38, 20);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    private static class MaterialSortEntry {
        private static final MaterialSortEntry HEAD_ATTACK = new MaterialSortEntry(ICON_SORT_ATTACK, "gui.tpa.sort.attack", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return stats instanceof HeadMaterialStats ? ((HeadMaterialStats) stats).attack : 0;
            }
        });
        private static final MaterialSortEntry HEAD_SPEED = new MaterialSortEntry(ICON_SORT_SPEED, "gui.tpa.sort.speed", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return stats instanceof HeadMaterialStats ? ((HeadMaterialStats) stats).miningspeed : 0;
            }
        });
        private static final MaterialSortEntry HEAD_HARVEST = new MaterialSortEntry(ICON_SORT_HARVEST, "gui.tpa.sort.harvest", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return stats instanceof HeadMaterialStats ? ((HeadMaterialStats) stats).harvestLevel : 0;
            }
        });
        private static final MaterialSortEntry DURABILITY = new MaterialSortEntry(ICON_SORT_DURABILITY, "gui.tpa.sort.durability", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                if (stats instanceof HeadMaterialStats) {
                    return ((HeadMaterialStats) stats).durability;
                }
                if (stats instanceof HandleMaterialStats) {
                    return ((HandleMaterialStats) stats).durability;
                }
                if (stats instanceof ExtraMaterialStats) {
                    return ((ExtraMaterialStats) stats).extraDurability;
                }
                return readNumberField(stats, "durability", "extraDurability");
            }
        });
        private static final MaterialSortEntry MODIFIER = new MaterialSortEntry(ICON_SORT_MODIFIER, "gui.tpa.sort.modifier", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "modifier");
            }
        });
        private static final MaterialSortEntry ARMOR_DEFENSE = new MaterialSortEntry(ICON_SORT_ARMOR, "gui.tpa.sort.armor", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "defense");
            }
        });
        private static final MaterialSortEntry ARMOR_TOUGHNESS = new MaterialSortEntry(ICON_SORT_TOUGHNESS, "gui.tpa.sort.toughness", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "toughness");
            }
        });
        private static final MaterialSortEntry ARMOR_DURABILITY = new MaterialSortEntry(ICON_SORT_ARMOR_DURABILITY, "gui.tpa.sort.armor_durability", new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "durability", "extraDurability");
            }
        });
        private static final MaterialSortEntry YOYO_BODY_ATTACK = new MaterialSortEntry(ICON_SORT_ATTACK, "gui.tpa.sort.attack", YOYO_BODY, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "attack");
            }
        });
        private static final MaterialSortEntry YOYO_BODY_WEIGHT = new MaterialSortEntry(ICON_SORT_WEIGHT, "gui.tpa.sort.weight", YOYO_BODY, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "weight");
            }
        });
        private static final MaterialSortEntry YOYO_BODY_DURABILITY = new MaterialSortEntry(ICON_SORT_DURABILITY, "gui.tpa.sort.durability", YOYO_BODY, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "durability");
            }
        });
        private static final MaterialSortEntry YOYO_FRICTION = new MaterialSortEntry(ICON_SORT_FRICTION, "gui.tpa.sort.friction", null, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "friction");
            }
        });
        private static final MaterialSortEntry YOYO_LENGTH = new MaterialSortEntry(ICON_SORT_YOYO_LENGTH, "gui.tpa.sort.length", YOYO_CORD, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "length");
            }
        });
        private static final MaterialSortEntry YOYO_AXLE_MODIFIER = new MaterialSortEntry(ICON_SORT_MODIFIER, "gui.tpa.sort.modifier", YOYO_AXLE, new ValueReader() {
            @Override
            public double read(IMaterialStats stats) {
                return readNumberField(stats, "modifier");
            }
        });
        private final ResourceLocation iconTexture;
        private final String labelKey;
        private final String id;
        private final String statType;
        private final String statTypeOverride;
        private final ValueReader reader;
        private final List<String> fieldNames;
        private final String fieldName;
        private final IMaterialStats sampleStats;

        private MaterialSortEntry(ResourceLocation icon, String labelKey, ValueReader reader) {
            this(icon, labelKey, labelKey, null, null, reader);
        }

        private MaterialSortEntry(ResourceLocation icon, String labelKey, String statTypeOverride, ValueReader reader) {
            this(icon, labelKey, labelKey, null, statTypeOverride, reader);
        }

        private MaterialSortEntry(ResourceLocation icon, String labelKey, String id, String statType, String statTypeOverride, ValueReader reader) {
            this.iconTexture = icon;
            this.labelKey = labelKey;
            this.id = id;
            this.statType = statType;
            this.statTypeOverride = statTypeOverride;
            this.reader = reader;
            this.fieldNames = Collections.emptyList();
            this.fieldName = null;
            this.sampleStats = null;
        }

        static MaterialSortEntry automatic(String statType, String fieldName, IMaterialStats sampleStats) {
            String id = statType + "|" + fieldName;
            return new MaterialSortEntry(ICON_SORT_GENERIC, "stat." + statType + "." + fieldName + ".name", id, statType, null, fieldName, sampleStats, new ValueReader() {
                @Override
                public double read(IMaterialStats stats) {
                    return readNumberField(stats, fieldName);
                }
            });
        }

        static MaterialSortEntry prototypical(String statType) {
            if (YOYO_BODY.equals(statType)) {
                return YOYO_BODY_ATTACK;
            }
            if (YOYO_CORD.equals(statType)) {
                return YOYO_FRICTION;
            }
            if (YOYO_AXLE.equals(statType)) {
                return YOYO_AXLE_MODIFIER;
            }
            if (ArmorMaterialType.CORE.equals(statType)) {
                return ARMOR_DEFENSE;
            }
            if (ArmorMaterialType.PLATES.equals(statType)) {
                return ARMOR_TOUGHNESS;
            }
            if (ArmorMaterialType.TRIM.equals(statType)) {
                return ARMOR_DURABILITY;
            }
            if (MaterialTypes.HEAD.equals(statType)) {
                return HEAD_ATTACK;
            }
            if (MaterialTypes.HANDLE.equals(statType) || MaterialTypes.EXTRA.equals(statType) || MaterialTypes.BOW.equals(statType) || MaterialTypes.BOWSTRING.equals(statType) || MaterialTypes.SHAFT.equals(statType) || MaterialTypes.FLETCHING.equals(statType) || MaterialTypes.PROJECTILE.equals(statType)) {
                return DURABILITY;
            }
            return null;
        }

        private MaterialSortEntry(ResourceLocation icon, String labelKey, String id, String statType, String statTypeOverride, String fieldName, ValueReader reader) {
            this.iconTexture = icon;
            this.labelKey = labelKey;
            this.id = id;
            this.statType = statType;
            this.statTypeOverride = statTypeOverride;
            this.reader = reader;
            this.fieldNames = Collections.emptyList();
            this.fieldName = fieldName;
            this.sampleStats = null;
        }

        private MaterialSortEntry(ResourceLocation icon, String labelKey, String id, String statType, String statTypeOverride, String fieldName, IMaterialStats sampleStats, ValueReader reader) {
            this.iconTexture = icon;
            this.labelKey = labelKey;
            this.id = id;
            this.statType = statType;
            this.statTypeOverride = statTypeOverride;
            this.reader = reader;
            this.fieldNames = Collections.emptyList();
            this.fieldName = fieldName;
            this.sampleStats = sampleStats;
        }

        private List<String> getFieldNames() {
            List<String> fields = new ArrayList<>();
            if (this == HEAD_ATTACK) {
                fields.add("attack");
            } else if (this == HEAD_SPEED) {
                fields.add("miningspeed");
            } else if (this == HEAD_HARVEST) {
                fields.add("harvestLevel");
            } else if (this == DURABILITY || this == YOYO_BODY_DURABILITY || this == ARMOR_DURABILITY) {
                fields.add("durability");
                fields.add("extraDurability");
            } else if (this == MODIFIER || this == YOYO_AXLE_MODIFIER) {
                fields.add("modifier");
            } else if (this == ARMOR_DEFENSE) {
                fields.add("defense");
            } else if (this == ARMOR_TOUGHNESS) {
                fields.add("toughness");
            } else if (this == YOYO_BODY_WEIGHT) {
                fields.add("weight");
            } else if (this == YOYO_FRICTION) {
                fields.add("friction");
            } else if (this == YOYO_LENGTH) {
                fields.add("length");
            }
            return fields;
        }

        private int compare(String statType, Material left, Material right) {
            if (this.statType != null) {
                statType = this.statType;
            }
            if (statTypeOverride != null) {
                statType = statTypeOverride;
            }
            IMaterialStats leftStats = left.getStats(statType);
            IMaterialStats rightStats = right.getStats(statType);
            if (leftStats == null && rightStats == null) {
                return 0;
            }
            if (leftStats == null) {
                return 1;
            }
            if (rightStats == null) {
                return -1;
            }
            return Double.compare(reader.read(rightStats), reader.read(leftStats));
        }

        private ResourceLocation getIconTexture() {
            return iconTexture;
        }

        private String getLabel() {
            if (fieldName != null) {
                String localized = localizeStatField(statType, fieldName, sampleStats);
                if (!localized.isEmpty()) {
                    return stripStatLabel(localized);
                }
                localized = findLocalizedInfoLabel(sampleStats, fieldName);
                if (!localized.isEmpty()) {
                    return localized;
                }
                return humanizeFieldName(fieldName);
            }
            String label = I18n.translateToLocal(labelKey);
            if (!labelKey.equals(label)) {
                return stripStatLabel(label);
            }
            return humanizeFieldName(labelKey.startsWith("gui.tpa.sort.") ? labelKey.substring("gui.tpa.sort.".length()) : labelKey);
        }

        private static String localizeStatField(String statType, String fieldName, IMaterialStats stats) {
            String localized = localizeStatsConstant(stats, fieldName);
            if (!localized.isEmpty()) {
                return localized;
            }
            for (String keyField : getFieldLocalizationKeys(fieldName)) {
                localized = localizeExisting("stat." + statType + "." + keyField + ".name");
                if (!localized.isEmpty()) {
                    return localized;
                }
            }
            return "";
        }

        private static String localizeStatsConstant(IMaterialStats stats, String fieldName) {
            if (stats == null || fieldName == null) {
                return "";
            }
            String normalizedField = normalizeStatName(fieldName);
            for (Field field : stats.getClass().getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !String.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                String name = field.getName();
                if (!name.startsWith("LOC_") || name.endsWith("Desc")) {
                    continue;
                }
                if (!normalizeStatName(name.substring("LOC_".length())).equals(normalizedField)) {
                    continue;
                }
                try {
                    String key = (String) field.get(null);
                    if (key != null && !key.endsWith(".desc")) {
                        String localized = localizeExisting(key);
                        if (!localized.isEmpty()) {
                            return localized;
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            return "";
        }

        private static String normalizeStatName(String value) {
            return value == null ? "" : value.replace("_", "").replace("-", "").replace(".", "").toLowerCase(Locale.ROOT);
        }

        private static List<String> getFieldLocalizationKeys(String fieldName) {
            List<String> keys = new ArrayList<>();
            keys.add(fieldName);
            String snakeCase = toSnakeCase(fieldName);
            if (!snakeCase.equals(fieldName)) {
                keys.add(snakeCase);
            }
            String lowerCase = fieldName.toLowerCase(Locale.ROOT);
            if (!lowerCase.equals(fieldName) && !keys.contains(lowerCase)) {
                keys.add(lowerCase);
            }
            return keys;
        }

        private static String toSnakeCase(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder(value.length() + 4);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > 0 && builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
                        builder.append('_');
                    }
                    builder.append(Character.toLowerCase(c));
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        private static String localizeExisting(String key) {
            if (key == null || key.isEmpty() || !I18n.canTranslate(key)) {
                return "";
            }
            return I18n.translateToLocal(key);
        }

        private static String findLocalizedInfoLabel(IMaterialStats stats, String fieldName) {
            if (stats == null || fieldName == null) {
                return "";
            }
            Double value = readNullableNumberField(stats, fieldName);
            if (value == null) {
                return "";
            }
            List<String> info = stats.getLocalizedInfo();
            if (info == null) {
                return "";
            }
            String best = "";
            for (String line : info) {
                String label = stripStatLabel(line);
                if (label.isEmpty()) {
                    continue;
                }
                String cleaned = TextFormatting.getTextWithoutFormattingCodes(line);
                if (cleaned != null && containsNumber(cleaned, value)) {
                    return label;
                }
                if (best.isEmpty()) {
                    best = label;
                }
            }
            return info.size() == 1 ? best : "";
        }

        private static boolean containsNumber(String text, double value) {
            String normalizedText = text.replace(",", "");
            String plain = trimNumber(Double.toString(value));
            String oneDecimal = trimNumber(String.format(Locale.ROOT, "%.1f", value));
            String twoDecimals = trimNumber(String.format(Locale.ROOT, "%.2f", value));
            return containsStandaloneNumber(normalizedText, plain) || containsStandaloneNumber(normalizedText, oneDecimal) || containsStandaloneNumber(normalizedText, twoDecimals);
        }

        private static boolean containsStandaloneNumber(String text, String number) {
            if (number == null || number.isEmpty()) {
                return false;
            }
            int start = text.indexOf(number);
            while (start >= 0) {
                int end = start + number.length();
                boolean leftOk = start == 0 || !isNumberChar(text.charAt(start - 1));
                boolean rightOk = end >= text.length() || !isNumberChar(text.charAt(end));
                if (leftOk && rightOk) {
                    return true;
                }
                start = text.indexOf(number, start + 1);
            }
            return false;
        }

        private static boolean isNumberChar(char c) {
            return c >= '0' && c <= '9' || c == '.' || c == '-';
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MaterialSortEntry)) {
                return false;
            }
            MaterialSortEntry other = (MaterialSortEntry) obj;
            return id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        private static String humanizeFieldName(String fieldName) {
            if (fieldName == null || fieldName.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder(fieldName.length());
            boolean capitalizeNext = true;
            for (int i = 0; i < fieldName.length(); i++) {
                char c = fieldName.charAt(i);
                if (c == '_' || c == '-' || c == '.') {
                    builder.append(' ');
                    capitalizeNext = true;
                    continue;
                }
                if (capitalizeNext) {
                    builder.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        private static String stripStatLabel(String label) {
            if (label == null) {
                return "";
            }
            String cleaned = TextFormatting.getTextWithoutFormattingCodes(label);
            if (cleaned == null) {
                return "";
            }
            int colon = cleaned.indexOf(':');
            int colonZh = cleaned.indexOf('：');
            int split = colon >= 0 ? colon : colonZh;
            if (split > 0) {
                cleaned = cleaned.substring(0, split);
            }
            return cleaned.trim();
        }

        private static String trimNumber(String value) {
            if (value == null) {
                return "";
            }
            if (value.indexOf('.') >= 0) {
                while (value.endsWith("0")) {
                    value = value.substring(0, value.length() - 1);
                }
                if (value.endsWith(".")) {
                    value = value.substring(0, value.length() - 1);
                }
            }
            return value;
        }

        private static Double readNullableNumberField(IMaterialStats stats, String name) {
            try {
                Field field = stats.getClass().getField(name);
                Object value = field.get(stats);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
            return null;
        }

        private static double readNumberField(IMaterialStats stats, String... names) {
            for (String name : names) {
                try {
                    Field field = stats.getClass().getField(name);
                    Object value = field.get(stats);
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            }
            return 0;
        }
    }

    private interface ValueReader {
        double read(IMaterialStats stats);
    }
}
