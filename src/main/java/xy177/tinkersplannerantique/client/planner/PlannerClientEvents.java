package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
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
    private static List<ConArmClientCompat.ArmorPlannerTarget> armorTargets;
    private static PlannerOpenButton openButton;
    private static String assemblyNotice;
    private static long assemblyNoticeUntil;

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

    static List<ConArmClientCompat.ArmorPlannerTarget> getArmorTargets() {
        if (!ConArmPresence.isLoaded()) {
            return Collections.emptyList();
        }
        if (armorTargets == null) {
            armorTargets = new ArrayList<>(ConArmClientCompat.getArmorTargets());
        }
        return armorTargets;
    }

    static List<PlannerTarget> getAllTargets() {
        List<PlannerTarget> all = new ArrayList<>();
        all.addAll(getToolTargets());
        if (ConArmPresence.isLoaded()) {
            all.addAll(getArmorTargets());
        }
        return all;
    }

    static void showAssemblyNotice(String translationKey, int placed, int missing, int incorrect) {
        showAssemblyNotice(translationKey, placed, missing, incorrect, Collections.emptyList());
    }

    static void showAssemblyNotice(String translationKey, int placed, int missing, int incorrect, List<String> details) {
        if ("gui.tpa.assemble_done".equals(translationKey)) {
            assemblyNotice = I18n.translateToLocalFormatted(translationKey, placed, missing, incorrect);
        } else {
            assemblyNotice = I18n.translateToLocal(translationKey);
        }
        assemblyNoticeUntil = System.currentTimeMillis() + 4000L;
        applyAssemblyNoticeToCurrentStation(details);
    }

    static void handleAssemblyResult(String translationKey, int placed, int missing, int incorrect, List<String> details) {
        Minecraft.getMinecraft().addScheduledTask(() -> showAssemblyNotice(translationKey, placed, missing, incorrect, details));
    }

    private static void applyAssemblyNoticeToCurrentStation(List<String> details) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc.currentScreen;
        if (!(screen instanceof GuiToolStation) && !isArmorStationGui(screen)) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (assemblyNotice != null && !assemblyNotice.isEmpty()) {
            lines.add(assemblyNotice);
        }
        for (String detail : details) {
            String[] parts = detail.split("\t", -1);
            if (parts.length >= 2 && "M".equals(parts[0])) {
                lines.add(I18n.translateToLocalFormatted("gui.tpa.assemble_missing", parts[1]));
            } else if (parts.length >= 3 && "I".equals(parts[0])) {
                lines.add(I18n.translateToLocalFormatted("gui.tpa.assemble_incorrect", parts[1], parts[2]));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        if (!setStationInfoPanel(screen, details.isEmpty() ? "gui.tpa.assemble_caption" : "gui.warning", lines)) {
            if (screen instanceof GuiToolStation) {
                ((GuiToolStation) screen).warning(lines.get(0));
            } else {
                ConArmClientCompat.warn(screen, lines.get(0));
            }
        }
    }

    private static boolean setStationInfoPanel(GuiScreen screen, String captionKey, List<String> lines) {
        try {
            Object panel = readField(screen, screen instanceof GuiToolStation ? "toolInfo" : "armorInfo");
            Object traitPanel = readField(screen, "traitInfo");
            if (panel == null) {
                return false;
            }
            Method setCaption = panel.getClass().getMethod("setCaption", String.class);
            Method setText = panel.getClass().getMethod("setText", List.class);
            setCaption.invoke(panel, I18n.translateToLocal(captionKey));
            setText.invoke(panel, lines);
            if (traitPanel != null) {
                setCaption.invoke(traitPanel, new Object[] { null });
                setText.invoke(traitPanel, Collections.emptyList());
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object readField(Object target, String name) throws IllegalAccessException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiToolStation || isArmorStationGui(gui)) {
            openButton = createOpenButton(gui);
            event.getButtonList().add(openButton);
        }
    }

    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (openButton != null && openButton.owner == event.getGui()) {
            openButton.drawTooltip(Minecraft.getMinecraft(), event.getMouseX(), event.getMouseY());
        }
        drawAssemblyNotice(event.getGui());
    }

    private static void drawAssemblyNotice(GuiScreen gui) {
        if (assemblyNotice == null || System.currentTimeMillis() > assemblyNoticeUntil) {
            assemblyNotice = null;
            return;
        }
        if (!(gui instanceof GuiToolStation) && !isArmorStationGui(gui)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int textWidth = mc.fontRenderer.getStringWidth(assemblyNotice);
        int width = Math.min(gui.width - 20, textWidth + 20);
        int x = (gui.width - width) / 2;
        int y = 10;
        GlStateManager.disableDepth();
        Gui.drawRect(x, y, x + width, y + 22, 0xD0203040);
        mc.fontRenderer.drawStringWithShadow(assemblyNotice, x + (width - textWidth) / 2, y + 7, 0xFFFFFF);
        GlStateManager.enableDepth();
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
        } else if (isArmorStationGui(gui)) {
            ConArmClientCompat.openArmorPlanner(gui);
        }
    }

    private static boolean isArmorStationGui(GuiScreen gui) {
        return ConArmPresence.isLoaded() && ConArmClientCompat.isArmorStationGui(gui);
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
            if (slot instanceof SlotToolStationOut || (ConArmPresence.isLoaded() && ConArmClientCompat.isArmorOutputSlot(slot))) {
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
            mc.getTextureManager().bindTexture(SIMULATE_BUILD_ICON);
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1F, 1F, 1F, hovered ? 1F : 0.8F);
            drawModalRectWithCustomSizedTexture(x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }

        private void drawTooltip(Minecraft mc, int mouseX, int mouseY) {
            if (hovered) {
                GuiUtils.drawHoveringText(Collections.singletonList(I18n.translateToLocal("gui.tpa.simulate_build")), mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
            }
        }
    }
}
