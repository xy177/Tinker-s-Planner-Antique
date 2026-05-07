package xy177.tinkersplannerantique.client.planner;

import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.IClientCommand;
import xy177.tinkersplannerantique.PlannerConfig;

public final class PlannerClientCommand extends CommandBase implements IClientCommand {

    @Override
    public String getName() {
        return "ticpa";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ticpa list output [name] | /ticpa list reset [name] | /ticpa list remake [name] | /ticpa cache refresh | /ticpa test delet <true|false> | /ticpa test print <modpack|list|json>";
    }

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
        return false;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "cache", "test");
        }
        if (args.length == 2) {
            String root = args[0].toLowerCase(Locale.ROOT);
            if ("list".equals(root)) {
                return getListOfStringsMatchingLastWord(args, "output", "reset", "remake");
            }
            if ("cache".equals(root)) {
                return getListOfStringsMatchingLastWord(args, "refresh");
            }
            if ("test".equals(root)) {
                return getListOfStringsMatchingLastWord(args, "delet", "print");
            }
        }
        if (args.length == 3 && "test".equalsIgnoreCase(args[0]) && "delet".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        if (args.length == 3 && "test".equalsIgnoreCase(args[0]) && "print".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, "modpack", "list", "json");
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list":
                handleList(sender, args);
                return;
            case "copy":
                handleCopy(sender, args);
                return;
            case "bookmark":
                handleBookmark(sender, args);
                return;
            case "share":
                handleShare(sender, args);
                return;
            case "cache":
                handleCache(sender, args);
                return;
            case "test":
                handleTest(sender, args);
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void handleList(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("/ticpa list <output|reset|remake>");
        }
        if ("output".equalsIgnoreCase(args[1])) {
            handleListOutput(sender, args);
            return;
        }
        if ("reset".equalsIgnoreCase(args[1])) {
            handleListReset(sender, args);
            return;
        }
        if ("remake".equalsIgnoreCase(args[1])) {
            handleListRemake(sender, args);
            return;
        }
        throw new WrongUsageException("/ticpa list <output|reset|remake>");
    }

    private void handleListOutput(ICommandSender sender, String[] args) throws CommandException {
        String name = args.length > 2 ? buildJoined(args, 2) : PlannerConfig.defaultShortBlueprintListName;
        try {
            PlannerShortCodeList list = PlannerShortCodeList.generate(name, PlannerClientEvents.getAllTargets());
            list.save(PlannerClientEvents.getDataFolder());
            sender.sendMessage(new TextComponentString("Generated short blueprint list: " + PlannerShortCodeList.FILE_NAME + " [" + list.validationName + "] fingerprint=" + list.fingerprint));
        } catch (Exception e) {
            throw localized(e);
        }
    }

    private void handleListReset(ICommandSender sender, String[] args) throws CommandException {
        PlannerShortCodeList existing = PlannerShortCodeList.loadOrCreate(PlannerClientEvents.getDataFolder(), PlannerClientEvents.getAllTargets());
        if (existing == null) {
            throw new CommandException(I18n.translateToLocal("gui.tpa.short_list_missing"));
        }
        String name = args.length > 2 ? buildJoined(args, 2) : PlannerConfig.defaultShortBlueprintListName;
        try {
            PlannerShortCodeList updated = existing.withValidationName(name);
            updated.save(PlannerClientEvents.getDataFolder());
            sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("gui.tpa.listname_reset", updated.validationName)));
        } catch (Exception e) {
            throw localized(e);
        }
    }

    private void handleListRemake(ICommandSender sender, String[] args) throws CommandException {
        PlannerShortCodeList existing = PlannerShortCodeList.load(PlannerClientEvents.getDataFolder());
        if (existing == null) {
            throw new CommandException(I18n.translateToLocal("gui.tpa.short_list_missing"));
        }
        String name = args.length > 2 ? buildJoined(args, 2) : PlannerConfig.defaultShortBlueprintListName;
        try {
            PlannerShortCodeList updated = existing.remake(name, PlannerClientEvents.getAllTargets());
            updated.save(PlannerClientEvents.getDataFolder());
            sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("gui.tpa.list_remade", updated.validationName, updated.fingerprint)));
        } catch (Exception e) {
            throw localized(e);
        }
    }

    private void handleCopy(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("/ticpa copy <payload>");
        }
        GuiScreen.setClipboardString(PlannerChatComponents.decodeCommandPayload(args[1]));
        sender.sendMessage(new TextComponentString(I18n.translateToLocal("gui.tpa.code_copied")));
    }

    private void handleBookmark(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("/ticpa bookmark <payload>");
        }
        PlannerBlueprint blueprint = importBlueprint(PlannerChatComponents.decodeCommandPayload(args[1]));
        Collection<? extends PlannerTarget> targets = blueprint.target.getType() == PlannerTarget.TargetType.ARMOR ? PlannerClientEvents.getArmorTargets() : PlannerClientEvents.getToolTargets();
        List<PlannerBlueprint> saved = PlannerClientEvents.getData().loadSaved(blueprint.target.getType(), new java.util.ArrayList<>(targets));
        for (PlannerBlueprint existing : saved) {
            if (existing.equals(blueprint)) {
                sender.sendMessage(new TextComponentString(I18n.translateToLocal("gui.tpa.bookmark_exists")));
                return;
            }
        }
        saved.add(blueprint.copy());
        PlannerClientEvents.getData().save(blueprint.target.getType(), saved, PlannerClientEvents.getData().loadStarred(blueprint.target.getType(), new java.util.ArrayList<>(targets)));
        sender.sendMessage(new TextComponentString(I18n.translateToLocal("gui.tpa.bookmark_added")));
    }

    private void handleShare(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("/ticpa share <payload>");
        }
        String code = PlannerChatComponents.decodeCommandPayload(args[1]);
        PlannerBlueprint blueprint = importBlueprint(code);
        PlannerNetwork.sendShare(new BlueprintSharePacket(
            code,
            blueprint.target.getDisplayName() + I18n.translateToLocal("gui.tpa.blueprint_code"),
            I18n.translateToLocal("gui.tpa.chat.copy"),
            I18n.translateToLocal("gui.tpa.chat.bookmark"),
            I18n.translateToLocal("gui.tpa.chat.share")
        ));
        sender.sendMessage(new TextComponentString(I18n.translateToLocal("gui.tpa.code_shared")));
    }

    private void handleCache(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2 || !"refresh".equalsIgnoreCase(args[1])) {
            throw new WrongUsageException("/ticpa cache refresh");
        }
        PlannerScreen.refreshUiCache();
        sender.sendMessage(new TextComponentString(I18n.translateToLocal("gui.tpa.cache_refreshed")));
    }

    private void handleTest(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3) {
            throw new WrongUsageException("/ticpa test delet <true|false> | /ticpa test print <modpack|list|json>");
        }
        if ("delet".equalsIgnoreCase(args[1])) {
            handleTestDelete(sender, args[2]);
            return;
        }
        if ("print".equalsIgnoreCase(args[1])) {
            handleTestPrint(sender, args[2]);
            return;
        }
        throw new WrongUsageException("/ticpa test delet <true|false> | /ticpa test print <modpack|list|json>");
    }

    private void handleTestDelete(ICommandSender sender, String value) throws CommandException {
        boolean enabled;
        if ("true".equalsIgnoreCase(value)) {
            enabled = true;
        } else if ("false".equalsIgnoreCase(value)) {
            enabled = false;
        } else {
            throw new WrongUsageException("/ticpa test delet <true|false>");
        }
        PlannerDebugOptions.setDeleteMode(enabled);
        sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("gui.tpa.debug_delete_mode", enabled, PlannerDebugOptions.deletedModifierCount())));
    }

    private void handleTestPrint(ICommandSender sender, String value) throws CommandException {
        if ("json".equalsIgnoreCase(value)) {
            try {
                File file = PlannerRegistryJsonExporter.export(PlannerClientEvents.getDataFolder());
                sender.sendMessage(new TextComponentString("Generated registry JSON: " + file.getAbsolutePath()));
                return;
            } catch (IOException e) {
                throw localized(e);
            }
        }

        PlannerShortCodeList.Counts counts;
        String labelKey;
        if ("modpack".equalsIgnoreCase(value)) {
            counts = PlannerShortCodeList.count(PlannerClientEvents.getAllTargets());
            labelKey = "gui.tpa.print_modpack";
        } else if ("list".equalsIgnoreCase(value)) {
            PlannerShortCodeList list = PlannerShortCodeList.load(PlannerClientEvents.getDataFolder());
            if (list == null) {
                throw new CommandException(I18n.translateToLocal("gui.tpa.short_list_missing"));
            }
            counts = list.countEntries();
            labelKey = "gui.tpa.print_list";
        } else {
            throw new WrongUsageException("/ticpa test print <modpack|list|json>");
        }
        sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted(
            "gui.tpa.print_counts",
            I18n.translateToLocal(labelKey),
            counts.targets,
            counts.materials,
            counts.modifiers
        )));
    }

    private PlannerBlueprint importBlueprint(String rawCode) throws CommandException {
        try {
            return PlannerBlueprintCodecs.importCode(rawCode, PlannerShortCodeList.loadOrCreate(PlannerClientEvents.getDataFolder(), PlannerClientEvents.getAllTargets()), PlannerClientEvents.getAllTargets());
        } catch (IOException e) {
            throw localized(e);
        }
    }

    private CommandException localized(Exception e) {
        String raw = e.getMessage();
        if (raw != null && raw.startsWith("gui.tpa.")) {
            return new CommandException(I18n.translateToLocal(raw));
        }
        return new CommandException(raw == null ? I18n.translateToLocal("gui.tpa.import_failed") : raw);
    }

    private String buildJoined(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
