package xy177.tinkersplannerantique.client.planner;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class GiveItemPacket implements IMessage {

    private ItemStack stack = ItemStack.EMPTY;

    public GiveItemPacket() {
    }

    GiveItemPacket(ItemStack stack) {
        this.stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
    }

    ItemStack getStack() {
        return stack;
    }
}
