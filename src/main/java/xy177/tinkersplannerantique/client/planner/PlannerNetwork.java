package xy177.tinkersplannerantique.client.planner;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import slimeknights.tconstruct.library.tinkering.TinkersItem;
import c4.conarm.lib.tinkering.TinkersArmor;
import xy177.tinkersplannerantique.PlannerConfig;

public final class PlannerNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ticpa");

    private PlannerNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(ShareHandler.class, BlueprintSharePacket.class, 0, Side.SERVER);
        CHANNEL.registerMessage(GiveItemHandler.class, GiveItemPacket.class, 1, Side.SERVER);
    }

    public static void sendShare(BlueprintSharePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendGiveItem(ItemStack stack) {
        CHANNEL.sendToServer(new GiveItemPacket(stack));
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
            return stack.getItem() instanceof TinkersItem || stack.getItem() instanceof TinkersArmor;
        }
    }
}
