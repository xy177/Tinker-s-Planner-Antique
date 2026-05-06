package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import c4.conarm.client.gui.GuiArmorStation;
import c4.conarm.lib.ArmoryRegistry;
import c4.conarm.lib.ArmoryRegistryClient;
import c4.conarm.lib.armor.ArmorCore;
import c4.conarm.lib.client.ArmorBuildGuiInfo;
import c4.conarm.common.inventory.SlotArmorStationOut;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.tools.common.inventory.SlotToolStationOut;
import slimeknights.tconstruct.tools.common.client.GuiToolStation;
import xy177.tinkersplannerantique.TinkersPlannerAntique;

@Mod.EventBusSubscriber(modid = TinkersPlannerAntique.MODID, value = Side.CLIENT)
public final class PlannerClientEvents {

    private static final int OPEN_BUTTON_ID = 444200;
    private static final int OPEN_BUTTON_OUTPUT_OFFSET_X = 110;
    private static final int OPEN_BUTTON_OUTPUT_OFFSET_Y = 35;
    private static final ResourceLocation SIMULATE_BUILD_ICON = new ResourceLocation(TinkersPlannerAntique.MODID, "textures/gui/icons/simulate_build.png");

    private static PlannerData data;
    private static List<ToolPlannerTarget> toolTargets;
    private static List<ArmorPlannerTarget> armorTargets;
    private static PlannerOpenButton openButton;

    private PlannerClientEvents() {
    }

    public static void init() {
        if (data == null) {
            File folder = new File(Minecraft.getMinecraft().mcDataDir, TinkersPlannerAntique.MODID);
            data = new PlannerData(folder);
        }
    }

    static PlannerData getData() {
        init();
        return data;
    }

    static File getDataFolder() {
        return getData().getRootFolder();
    }

    static List<ToolPlannerTarget> getToolTargets() {
        if (toolTargets == null) {
            toolTargets = new ArrayList<>();
            Set<ToolCore> tools = new LinkedHashSet<>();
            tools.addAll(TinkerRegistry.getToolStationCrafting());
            tools.addAll(TinkerRegistry.getToolForgeCrafting());
            for (ToolCore tool : tools) {
                ToolBuildGuiInfo info = TinkerRegistryClient.getToolBuildInfoForTool(tool);
                if (info != null) {
                    toolTargets.add(new ToolPlannerTarget(tool, info));
                }
            }
        }
        return toolTargets;
    }

    static List<ArmorPlannerTarget> getArmorTargets() {
        if (armorTargets == null) {
            armorTargets = new ArrayList<>();
            for (ArmorCore armor : ArmoryRegistry.getArmorCrafting()) {
                ArmorBuildGuiInfo info = ArmoryRegistryClient.getArmorBuildInfoForArmor(armor);
                if (info != null) {
                    armorTargets.add(new ArmorPlannerTarget(armor, info));
                }
            }
        }
        return armorTargets;
    }

    static List<PlannerTarget> getAllTargets() {
        List<PlannerTarget> all = new ArrayList<>();
        all.addAll(getToolTargets());
        all.addAll(getArmorTargets());
        return all;
    }

    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiToolStation || gui instanceof GuiArmorStation) {
            openButton = createOpenButton(gui);
        }
    }

    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (openButton != null && openButton.owner == event.getGui()) {
            openButton.drawOverlay(Minecraft.getMinecraft(), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (openButton == null || openButton.owner != event.getGui()) {
            return;
        }
        int button = Mouse.getEventButton();
        if (button != 0 || !Mouse.getEventButtonState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.getGui();
        int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        if (openButton.mousePressed(mc, mouseX, mouseY)) {
            openButton.playPressSound(mc.getSoundHandler());
            openPlanner(gui);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton().id != OPEN_BUTTON_ID) {
            return;
        }
        GuiScreen gui = event.getGui();
        openPlanner(gui);
    }

    private static void openPlanner(GuiScreen gui) {
        if (gui instanceof GuiToolStation) {
            Minecraft.getMinecraft().displayGuiScreen(PlannerScreen.forTools((GuiToolStation) gui));
        } else if (gui instanceof GuiArmorStation) {
            Minecraft.getMinecraft().displayGuiScreen(PlannerScreen.forArmor((GuiArmorStation) gui));
        }
    }

    private static PlannerOpenButton createOpenButton(GuiScreen gui) {
        Slot output = findOutputSlot(gui);
        int x = guiLeft(gui) + 174 - 36;
        int y = guiTop(gui) + 38;
        if (output != null) {
            x = guiLeft(gui) + output.xPos + OPEN_BUTTON_OUTPUT_OFFSET_X;
            y = guiTop(gui) + output.yPos + OPEN_BUTTON_OUTPUT_OFFSET_Y;
        }
        return new PlannerOpenButton(gui, x, y);
    }

    private static Slot findOutputSlot(GuiScreen gui) {
        if (!(gui instanceof GuiContainer)) {
            return null;
        }
        for (Slot slot : ((GuiContainer) gui).inventorySlots.inventorySlots) {
            if (slot instanceof SlotToolStationOut || slot instanceof SlotArmorStationOut) {
                return slot;
            }
        }
        return null;
    }

    private static int guiLeft(GuiScreen gui) {
        return readGuiInt(gui, "guiLeft", gui.width / 2 - 176);
    }

    private static int guiTop(GuiScreen gui) {
        return readGuiInt(gui, "guiTop", gui.height / 2 - 122);
    }

    private static int readGuiInt(GuiScreen gui, String name, int fallback) {
        Class<?> type = gui.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(gui);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static class PlannerOpenButton extends GuiButton {
        private final GuiScreen owner;

        private PlannerOpenButton(GuiScreen owner, int x, int y) {
            super(OPEN_BUTTON_ID, x, y, 20, 20, "");
            this.owner = owner;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        private void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            mc.getTextureManager().bindTexture(SIMULATE_BUILD_ICON);
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1F, 1F, 1F, hovered ? 1F : 0.8F);
            drawModalRectWithCustomSizedTexture(x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
            if (hovered) {
                GuiUtils.drawHoveringText(Collections.singletonList(I18n.translateToLocal("gui.tpa.simulate_build")), mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
            }
        }
    }
}
