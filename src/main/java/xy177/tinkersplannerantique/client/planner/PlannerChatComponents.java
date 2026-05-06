package xy177.tinkersplannerantique.client.planner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;

final class PlannerChatComponents {

    private PlannerChatComponents() {
    }

    static ITextComponent buildCodeMessage(String title, String code, String copyLabel, String bookmarkLabel, String shareLabel, boolean includeShare) {
        String payload = encodeCommandPayload(code);
        TextComponentString root = new TextComponentString("");
        root.appendSibling(button(copyLabel, TextFormatting.GREEN, "/ticpa copy " + payload));
        root.appendText(" ");
        root.appendSibling(button(bookmarkLabel, TextFormatting.YELLOW, "/ticpa bookmark " + payload));
        if (includeShare) {
            root.appendText(" ");
            root.appendSibling(button(shareLabel, TextFormatting.AQUA, "/ticpa share " + payload));
        }
        root.appendText(" ");
        root.appendSibling(new TextComponentString(title).setStyle(new Style().setColor(TextFormatting.WHITE)));
        root.appendText(" ");
        root.appendSibling(new TextComponentString(code).setStyle(new Style().setColor(TextFormatting.GRAY)));
        return root;
    }

    static String encodeCommandPayload(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static String decodeCommandPayload(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static ITextComponent button(String label, TextFormatting color, String command) {
        return new TextComponentString(label).setStyle(new Style().setColor(color).setBold(true).setClickEvent(new ClickEvent(Action.RUN_COMMAND, command)));
    }
}
