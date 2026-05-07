package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class AssembleResultPacket implements IMessage {

    private String translationKey = "";
    private int placed;
    private int missing;
    private int incorrect;
    private final List<String> details = new ArrayList<>();

    public AssembleResultPacket() {
    }

    AssembleResultPacket(String translationKey, int placed, int missing, int incorrect, List<String> details) {
        this.translationKey = translationKey;
        this.placed = placed;
        this.missing = missing;
        this.incorrect = incorrect;
        this.details.addAll(details);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        translationKey = ByteBufUtils.readUTF8String(buf);
        placed = buf.readInt();
        missing = buf.readInt();
        incorrect = buf.readInt();
        details.clear();
        int count = Math.min(buf.readByte() & 255, 16);
        for (int i = 0; i < count; i++) {
            details.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, translationKey);
        buf.writeInt(placed);
        buf.writeInt(missing);
        buf.writeInt(incorrect);
        buf.writeByte(Math.min(details.size(), 16));
        for (int i = 0; i < details.size() && i < 16; i++) {
            ByteBufUtils.writeUTF8String(buf, details.get(i));
        }
    }

    String getTranslationKey() {
        return translationKey;
    }

    int getPlaced() {
        return placed;
    }

    int getMissing() {
        return missing;
    }

    int getIncorrect() {
        return incorrect;
    }

    List<String> getDetails() {
        return details;
    }
}
