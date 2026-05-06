package xy177.tinkersplannerantique.client.planner;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class BlueprintSharePacket implements IMessage {

    private String code;
    private String title;
    private String copyLabel;
    private String bookmarkLabel;
    private String shareLabel;

    public BlueprintSharePacket() {
    }

    BlueprintSharePacket(String code, String title, String copyLabel, String bookmarkLabel, String shareLabel) {
        this.code = code;
        this.title = title;
        this.copyLabel = copyLabel;
        this.bookmarkLabel = bookmarkLabel;
        this.shareLabel = shareLabel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        code = ByteBufUtils.readUTF8String(buf);
        title = ByteBufUtils.readUTF8String(buf);
        copyLabel = ByteBufUtils.readUTF8String(buf);
        bookmarkLabel = ByteBufUtils.readUTF8String(buf);
        shareLabel = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, code);
        ByteBufUtils.writeUTF8String(buf, title);
        ByteBufUtils.writeUTF8String(buf, copyLabel);
        ByteBufUtils.writeUTF8String(buf, bookmarkLabel);
        ByteBufUtils.writeUTF8String(buf, shareLabel);
    }

    String getCode() {
        return code;
    }

    String getTitle() {
        return title;
    }

    String getCopyLabel() {
        return copyLabel;
    }

    String getBookmarkLabel() {
        return bookmarkLabel;
    }

    String getShareLabel() {
        return shareLabel;
    }
}
