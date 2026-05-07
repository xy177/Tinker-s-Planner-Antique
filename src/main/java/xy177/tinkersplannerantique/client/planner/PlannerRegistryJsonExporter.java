package xy177.tinkersplannerantique.client.planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.traits.ITrait;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.library.tools.ToolCore;

final class PlannerRegistryJsonExporter {

    private static final String FILE_NAME = "planner_registry_dump.json";

    private PlannerRegistryJsonExporter() {
    }

    static File export(File dataFolder) throws IOException {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IOException("Unable to create data folder: " + dataFolder.getAbsolutePath());
        }

        Map<String, String> enUs = loadTranslations("en_us");
        Map<String, String> zhCn = loadTranslations("zh_cn");

        List<Entry> weapons = collectWeapons(enUs, zhCn);
        List<Entry> materials = collectMaterials(enUs, zhCn);
        List<Entry> traits = collectTraits(enUs, zhCn);
        List<Entry> modifiers = collectModifiers(enUs, zhCn);
        List<Entry> parts = collectParts(enUs, zhCn);

        File file = new File(dataFolder, FILE_NAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writeArray(writer, "weapons", weapons, true);
            writeArray(writer, "materials", materials, true);
            writeArray(writer, "traits", traits, true);
            writeArray(writer, "modifiers", modifiers, true);
            writeArray(writer, "parts", parts, false);
            writer.write("}\n");
        }
        return file;
    }

    private static List<Entry> collectWeapons(Map<String, String> enUs, Map<String, String> zhCn) {
        List<Entry> entries = new ArrayList<>();
        for (ToolCore tool : TinkerRegistry.getTools()) {
            String id = registryName(tool);
            if (id.isEmpty()) {
                id = tool.getIdentifier();
            }
            String key = tool.getUnlocalizedName() + ".name";
            entries.add(entry(id, tool.getLocalizedToolName(), enUs, zhCn, key, itemKeyFromRegistry(id)));
        }
        sort(entries);
        return entries;
    }

    private static List<Entry> collectMaterials(Map<String, String> enUs, Map<String, String> zhCn) {
        List<Entry> entries = new ArrayList<>();
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material == null || material == Material.UNKNOWN) {
                continue;
            }
            String id = material.getIdentifier();
            entries.add(entry(id, material.getLocalizedName(), enUs, zhCn, "material." + id + ".name"));
        }
        sort(entries);
        return entries;
    }

    private static List<Entry> collectTraits(Map<String, String> enUs, Map<String, String> zhCn) {
        Map<String, ITrait> traits = new LinkedHashMap<>();
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material == null || material == Material.UNKNOWN) {
                continue;
            }
            for (ITrait trait : material.getAllTraits()) {
                if (trait != null && !isSpecialTrait(trait)) {
                    traits.putIfAbsent(trait.getIdentifier(), trait);
                }
            }
        }
        for (IModifier modifier : collectAllModifiers()) {
            if (modifier instanceof ITrait && !isSpecialModifier(modifier)) {
                ITrait trait = (ITrait) modifier;
                traits.putIfAbsent(trait.getIdentifier(), trait);
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (ITrait trait : traits.values()) {
            String id = trait.getIdentifier();
            entries.add(entry(id, trait.getLocalizedName(), enUs, zhCn,
                "modifier." + id + ".name",
                "modifier.trait." + id + ".name",
                "trait." + id + ".name"));
        }
        sort(entries);
        return entries;
    }

    private static List<Entry> collectModifiers(Map<String, String> enUs, Map<String, String> zhCn) {
        List<Entry> entries = new ArrayList<>();
        for (IModifier modifier : collectAllModifiers()) {
            if (isSpecialModifier(modifier)) {
                continue;
            }
            String id = modifier.getIdentifier();
            entries.add(entry(id, modifier.getLocalizedName(), enUs, zhCn,
                "modifier." + id + ".name",
                "modifier.trait." + id + ".name"));
        }
        sort(entries);
        return entries;
    }

    private static Collection<IModifier> collectAllModifiers() {
        Map<String, IModifier> modifiers = new LinkedHashMap<>();
        for (IModifier modifier : TinkerRegistry.getAllModifiers()) {
            if (modifier != null && !isSpecialModifier(modifier)) {
                modifiers.putIfAbsent(modifier.getIdentifier(), modifier);
            }
        }
        if (ConArmCompat.isLoaded()) {
            for (IModifier modifier : ConArmCompat.getAllArmorModifiers()) {
                if (modifier != null && !isSpecialModifier(modifier)) {
                    modifiers.putIfAbsent(modifier.getIdentifier(), modifier);
                }
            }
        }
        return modifiers.values();
    }

    private static List<Entry> collectParts(Map<String, String> enUs, Map<String, String> zhCn) {
        List<Entry> entries = new ArrayList<>();
        for (IToolPart part : TinkerRegistry.getToolParts()) {
            Item item = itemFromPart(part);
            String id = item == null ? "" : registryName(item);
            if (id.isEmpty()) {
                ItemStack stack = part.getOutlineRenderStack();
                if (!stack.isEmpty()) {
                    id = registryName(stack.getItem());
                }
            }
            if (id.isEmpty()) {
                id = part.getClass().getName();
            }

            List<String> keys = new ArrayList<>();
            if (item != null) {
                keys.add(item.getUnlocalizedName() + ".name");
            }
            keys.add(itemKeyFromRegistry(id));
            entries.add(entry(id, partFallbackName(part), enUs, zhCn, keys.toArray(new String[0])));
        }
        sort(entries);
        return entries;
    }

    private static Item itemFromPart(IToolPart part) {
        if (part instanceof Item) {
            return (Item) part;
        }
        ItemStack stack = part.getOutlineRenderStack();
        return stack.isEmpty() ? null : stack.getItem();
    }

    private static String partFallbackName(IToolPart part) {
        ItemStack stack = part.getOutlineRenderStack();
        return stack.isEmpty() ? part.getClass().getSimpleName() : stack.getDisplayName();
    }

    private static Entry entry(String id, String fallback, Map<String, String> enUs, Map<String, String> zhCn, String... keys) {
        return new Entry(
            strip(id),
            lookup(enUs, fallback, keys),
            lookup(zhCn, fallback, keys)
        );
    }

    private static String lookup(Map<String, String> translations, String fallback, String... keys) {
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = translations.get(key);
            if (value != null && !value.isEmpty()) {
                return strip(value);
            }
        }
        return strip(fallback);
    }

    private static String itemKeyFromRegistry(String id) {
        int separator = id.indexOf(':');
        if (separator < 0 || separator == id.length() - 1) {
            return "";
        }
        return "item." + id.substring(0, separator) + "." + id.substring(separator + 1) + ".name";
    }

    private static String registryName(Item item) {
        ResourceLocation registryName = item.getRegistryName();
        return registryName == null ? "" : registryName.toString();
    }

    private static Map<String, String> loadTranslations(String language) {
        Map<String, String> translations = new LinkedHashMap<>();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            ResourceLocation location = new ResourceLocation(mod.getModId(), "lang/" + language + ".lang");
            try {
                for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(location)) {
                    readLang(resource, translations);
                }
            } catch (IOException ignored) {
                // Many mods do not provide both language files.
            }
        }
        return translations;
    }

    private static void readLang(IResource resource, Map<String, String> translations) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                translations.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
    }

    private static void writeArray(Writer writer, String name, List<Entry> entries, boolean comma) throws IOException {
        writer.write("  ");
        writeString(writer, name);
        writer.write(": [\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            writer.write("    {\"id\": ");
            writeString(writer, entry.id);
            writer.write(", \"name\": {\"en_us\": ");
            writeString(writer, entry.enUsName);
            writer.write(", \"zh_cn\": ");
            writeString(writer, entry.zhCnName);
            writer.write("}}");
            if (i < entries.size() - 1) {
                writer.write(',');
            }
            writer.write('\n');
        }
        writer.write("  ]");
        if (comma) {
            writer.write(',');
        }
        writer.write('\n');
    }

    private static void writeString(Writer writer, String value) throws IOException {
        writer.write('"');
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    writer.write("\\\"");
                    break;
                case '\\':
                    writer.write("\\\\");
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        writer.write(String.format("\\u%04x", (int) c));
                    } else {
                        writer.write(c);
                    }
                    break;
            }
        }
        writer.write('"');
    }

    private static void sort(List<Entry> entries) {
        Collections.sort(entries, Comparator.comparing(entry -> entry.id));
    }

    private static String strip(String value) {
        if (value == null) {
            return "";
        }
        String stripped = TextFormatting.getTextWithoutFormattingCodes(value);
        return stripped == null ? value : stripped;
    }

    private static boolean isSpecialModifier(IModifier modifier) {
        if (modifier == null) {
            return false;
        }
        return isSpecialIdAndClass(modifier.getClass().getName(), modifier.getIdentifier());
    }

    private static boolean isSpecialTrait(ITrait trait) {
        if (trait == null) {
            return false;
        }
        return isSpecialIdAndClass(trait.getClass().getName(), trait.getIdentifier());
    }

    private static boolean isSpecialIdAndClass(String classNameRaw, String idRaw) {
        String className = classNameRaw == null ? "" : classNameRaw.toLowerCase();
        String idLower = idRaw == null ? "" : idRaw.toLowerCase();
        return isEmbossModifier(className, idLower) || isSecondEmbossModifier(className, idLower) || isMaterialSpecialModifier(className);
    }

    private static boolean isEmbossModifier(String className, String idLower) {
        return className.contains("modextratrait") && !className.contains("modextratrait2") && !className.contains("display")
            || className.contains("modextraarmortrait") && !className.contains("modextraarmortrait2") && !className.contains("display")
            || idLower.contains("extratrait") && !idLower.contains("extratrait2");
    }

    private static boolean isSecondEmbossModifier(String className, String idLower) {
        return idLower.startsWith("moretcon.extratrait2")
            || className.contains("modextratrait2")
            || className.contains("modextraarmortrait2");
    }

    private static boolean isMaterialSpecialModifier(String className) {
        return (className.contains("modfortify") || className.contains("modpolished")) && !className.contains("display");
    }

    private static final class Entry {
        final String id;
        final String enUsName;
        final String zhCnName;

        Entry(String id, String enUsName, String zhCnName) {
            this.id = id;
            this.enUsName = enUsName;
            this.zhCnName = zhCnName;
        }
    }
}
