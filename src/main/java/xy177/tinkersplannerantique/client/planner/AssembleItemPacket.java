package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class AssembleItemPacket implements IMessage {

    private ItemStack target = ItemStack.EMPTY;
    private final List<ItemStack> parts = new ArrayList<>();
    private boolean armor;

    public AssembleItemPacket() {
    }

    AssembleItemPacket(ItemStack target, List<ItemStack> parts, boolean armor) {
        this.target = target.isEmpty() ? ItemStack.EMPTY : target.copy();
        for (ItemStack part : parts) {
            if (!part.isEmpty()) {
                ItemStack copy = part.copy();
                copy.setCount(1);
                this.parts.add(copy);
            }
        }
        this.armor = armor;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = ByteBufUtils.readItemStack(buf);
        armor = buf.readBoolean();
        parts.clear();
        int count = Math.min(buf.readByte() & 255, 6);
        for (int i = 0; i < count; i++) {
            ItemStack part = ByteBufUtils.readItemStack(buf);
            if (!part.isEmpty()) {
                part.setCount(1);
                parts.add(part);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, target);
        buf.writeBoolean(armor);
        buf.writeByte(Math.min(parts.size(), 6));
        for (int i = 0; i < parts.size() && i < 6; i++) {
            ByteBufUtils.writeItemStack(buf, parts.get(i));
        }
    }

    ItemStack getTarget() {
        return target;
    }

    List<ItemStack> getParts() {
        return parts;
    }

    boolean isArmor() {
        return armor;
    }
}
