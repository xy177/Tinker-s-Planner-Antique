package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.library.tinkering.TinkersItem;
import slimeknights.tconstruct.tools.common.inventory.ContainerToolStation;
import slimeknights.tconstruct.tools.common.inventory.SlotToolStationIn;
import slimeknights.tconstruct.tools.common.inventory.SlotToolStationOut;
import xy177.tinkersplannerantique.PlannerConfig;

public final class PlannerNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ticpa");

    private PlannerNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(ShareHandler.class, BlueprintSharePacket.class, 0, Side.SERVER);
        CHANNEL.registerMessage(GiveItemHandler.class, GiveItemPacket.class, 1, Side.SERVER);
        CHANNEL.registerMessage(AssembleItemHandler.class, AssembleItemPacket.class, 2, Side.SERVER);
        CHANNEL.registerMessage(AssembleResultHandler.class, AssembleResultPacket.class, 3, Side.CLIENT);
    }

    public static void sendShare(BlueprintSharePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendGiveItem(ItemStack stack) {
        CHANNEL.sendToServer(new GiveItemPacket(stack));
    }

    public static void sendAssemble(ItemStack target, List<ItemStack> parts, boolean armor) {
        CHANNEL.sendToServer(new AssembleItemPacket(target, parts, armor));
    }

    public static class ShareHandler implements IMessageHandler<BlueprintSharePacket, IMessage> {
        @Override
        public IMessage onMessage(BlueprintSharePacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ITextComponent component = PlannerChatComponents.buildCodeMessage(message.getTitle(), message.getCode(), message.getCopyLabel(), message.getBookmarkLabel(), message.getShareLabel(), true);
                player.getServer().getPlayerList().sendMessage(component);
            });
            return null;
        }
    }

    public static class GiveItemHandler implements IMessageHandler<GiveItemPacket, IMessage> {
        @Override
        public IMessage onMessage(GiveItemPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ItemStack stack = message.getStack().isEmpty() ? ItemStack.EMPTY : message.getStack().copy();
            player.getServerWorld().addScheduledTask(() -> giveItem(player, stack));
            return null;
        }

        private void giveItem(EntityPlayerMP player, ItemStack stack) {
            if (stack.isEmpty() || !isPlannerItem(stack)) {
                return;
            }
            if (PlannerConfig.creativeOnlyGiveItem && !player.capabilities.isCreativeMode) {
                return;
            }
            stack.setCount(1);
            ItemStack remaining = stack.copy();
            boolean added = player.inventory.addItemStackToInventory(remaining);
            if (!added && !remaining.isEmpty()) {
                player.dropItem(remaining, false);
            }
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }

        private boolean isPlannerItem(ItemStack stack) {
            return stack.getItem() instanceof TinkersItem || ConArmCompat.isPlannerArmor(stack);
        }
    }

    public static class AssembleItemHandler implements IMessageHandler<AssembleItemPacket, IMessage> {
        @Override
        public IMessage onMessage(AssembleItemPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ItemStack target = message.getTarget().isEmpty() ? ItemStack.EMPTY : message.getTarget().copy();
            List<ItemStack> parts = new ArrayList<>();
            for (ItemStack part : message.getParts()) {
                if (!part.isEmpty()) {
                    ItemStack copy = part.copy();
                    copy.setCount(1);
                    parts.add(copy);
                }
            }
            player.getServerWorld().addScheduledTask(() -> assemble(player, target, parts, message.isArmor()));
            return null;
        }

        private void assemble(EntityPlayerMP player, ItemStack target, List<ItemStack> parts, boolean armor) {
            if (target.isEmpty() || parts.isEmpty() || parts.size() > 6) {
                return;
            }
            Container container = player.openContainer;
            List<Slot> inputSlots = getInputSlots(container, armor);
            if (inputSlots.size() < parts.size() || !selectTarget(container, target, parts.size(), armor)) {
                sendResult(player, "gui.tpa.assemble_no_station", 0, 0, 0);
                return;
            }

            int placed = 0;
            int missing = 0;
            int incorrect = 0;
            List<String> details = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                Slot input = inputSlots.get(i);
                ItemStack expected = parts.get(i);
                ItemStack current = input.getStack();
                if (!current.isEmpty()) {
                    if (!matches(current, expected)) {
                        incorrect++;
                        details.add("I\t" + expected.getDisplayName() + "\t" + current.getDisplayName());
                        player.sendMessage(new TextComponentTranslation("gui.tpa.assemble_incorrect", expected.getDisplayName(), current.getDisplayName()));
                    }
                    continue;
                }
                if (!input.isItemValid(expected)) {
                    incorrect++;
                    details.add("I\t" + expected.getDisplayName() + "\t-");
                    player.sendMessage(new TextComponentTranslation("gui.tpa.assemble_incorrect", expected.getDisplayName(), "-"));
                    continue;
                }
                Slot source = findPlayerSlot(container, player, expected);
                if (source == null) {
                    missing++;
                    details.add("M\t" + expected.getDisplayName());
                    player.sendMessage(new TextComponentTranslation("gui.tpa.assemble_missing", expected.getDisplayName()));
                    continue;
                }
                moveOne(source, input, expected);
                placed++;
            }

            player.inventory.markDirty();
            container.detectAndSendChanges();
            ITextComponent result = new TextComponentTranslation("gui.tpa.assemble_done", placed, missing, incorrect);
            player.sendMessage(result);
            sendResult(player, "gui.tpa.assemble_done", placed, missing, incorrect, details);
        }

        private void sendResult(EntityPlayerMP player, String translationKey, int placed, int missing, int incorrect) {
            sendResult(player, translationKey, placed, missing, incorrect, new ArrayList<>());
        }

        private void sendResult(EntityPlayerMP player, String translationKey, int placed, int missing, int incorrect, List<String> details) {
            CHANNEL.sendTo(new AssembleResultPacket(translationKey, placed, missing, incorrect, details), player);
        }

        private boolean selectTarget(Container container, ItemStack target, int activeSlots, boolean armor) {
            if (armor) {
                return ConArmCompat.selectArmorTarget(container, target, activeSlots);
            }
            if (!(container instanceof ContainerToolStation) || !(target.getItem() instanceof ToolCore)) {
                return false;
            }
            ((ContainerToolStation) container).setToolSelection((ToolCore) target.getItem(), activeSlots);
            return true;
        }

        private List<Slot> getInputSlots(Container container, boolean armor) {
            List<Slot> slots = new ArrayList<>();
            if (container == null) {
                return slots;
            }
            for (Slot slot : container.inventorySlots) {
                if (armor ? ConArmCompat.isArmorInputSlot(slot) : slot instanceof SlotToolStationIn) {
                    slots.add(slot);
                }
            }
            return slots;
        }

        private Slot findPlayerSlot(Container container, EntityPlayerMP player, ItemStack expected) {
            for (Slot slot : container.inventorySlots) {
                if (slot instanceof SlotToolStationIn || slot instanceof SlotToolStationOut || ConArmCompat.isArmorStationSlot(slot)) {
                    continue;
                }
                if (slot.inventory == player.inventory && matches(slot.getStack(), expected)) {
                    return slot;
                }
            }
            return null;
        }

        private boolean matches(ItemStack stack, ItemStack expected) {
            return !stack.isEmpty() && ItemStack.areItemsEqual(stack, expected) && ItemStack.areItemStackTagsEqual(stack, expected);
        }

        private void moveOne(Slot source, Slot input, ItemStack expected) {
            ItemStack sourceStack = source.getStack();
            if (sourceStack.isEmpty()) {
                return;
            }
            ItemStack placed = expected.copy();
            placed.setCount(1);
            sourceStack.shrink(1);
            if (sourceStack.getCount() <= 0) {
                source.putStack(ItemStack.EMPTY);
            } else {
                source.putStack(sourceStack);
            }
            input.putStack(placed);
            source.onSlotChanged();
            input.onSlotChanged();
        }
    }

    public static class AssembleResultHandler implements IMessageHandler<AssembleResultPacket, IMessage> {
        @Override
        public IMessage onMessage(AssembleResultPacket message, MessageContext ctx) {
            PlannerClientEvents.handleAssemblyResult(message.getTranslationKey(), message.getPlaced(), message.getMissing(), message.getIncorrect(), message.getDetails());
            return null;
        }
    }
}
