package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.IMaterialStats;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;

final class MaterialPowerFieldsExporter {

    private static final String FILE_NAME = "material_power_fields.json";

    private MaterialPowerFieldsExporter() {
    }

    static File export(File folder, List<? extends PlannerTarget> targets) throws IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Unable to create folder: " + folder.getAbsolutePath());
        }
        Map<String, Map<String, Set<String>>> fields = collectFields(targets);
        File file = new File(folder, FILE_NAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("{\n");
            int targetIndex = 0;
            for (Map.Entry<String, Map<String, Set<String>>> targetEntry : fields.entrySet()) {
                writer.write("  \"" + escape(targetEntry.getKey()) + "\": {\n");
                int statIndex = 0;
                for (Map.Entry<String, Set<String>> statEntry : targetEntry.getValue().entrySet()) {
                    writer.write("    \"" + escape(statEntry.getKey()) + "\": [");
                    int fieldIndex = 0;
                    List<String> names = new ArrayList<>(statEntry.getValue());
                    Collections.sort(names);
                    for (String name : names) {
                        if (fieldIndex++ > 0) {
                            writer.write(", ");
                        }
                        writer.write("\"" + escape(name) + "\"");
                    }
                    writer.write("]");
                    writer.write(++statIndex < targetEntry.getValue().size() ? ",\n" : "\n");
                }
                writer.write("  }");
                writer.write(++targetIndex < fields.size() ? ",\n" : "\n");
            }
            writer.write("}\n");
        }
        return file;
    }

    private static Map<String, Map<String, Set<String>>> collectFields(List<? extends PlannerTarget> targets) {
        Map<String, Map<String, Set<String>>> result = new LinkedHashMap<>();
        if (targets == null) {
            return result;
        }
        for (PlannerTarget target : targets) {
            if (target == null || target.getRequiredComponents() == null) {
                continue;
            }
            String targetKey = target.getType().name().toLowerCase(java.util.Locale.ROOT);
            Map<String, Set<String>> targetFields = result.computeIfAbsent(targetKey, ignored -> new LinkedHashMap<>());
            for (PartMaterialType partType : target.getRequiredComponents()) {
                for (String statType : PlannerUiCache.getUsedStatTypes(partType, target.getType())) {
                    IMaterialStats stats = findSampleStats(partType, statType);
                    if (stats == null) {
                        continue;
                    }
                    Set<String> fields = targetFields.computeIfAbsent(statType, ignored -> new LinkedHashSet<>());
                    fields.addAll(readNumericFieldNames(stats));
                }
            }
        }
        return result;
    }

    private static IMaterialStats findSampleStats(PartMaterialType partType, String statType) {
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material == null || material == Material.UNKNOWN) {
                continue;
            }
            if (partType != null && !partType.isValidMaterial(material) && material.getStats(statType) == null) {
                continue;
            }
            IMaterialStats stats = material.getStats(statType);
            if (stats != null) {
                return stats;
            }
        }
        return null;
    }

    private static Set<String> readNumericFieldNames(IMaterialStats stats) {
        Set<String> fields = new LinkedHashSet<>();
        Class<?> type = stats.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (Number.class.isAssignableFrom(fieldType) || (fieldType.isPrimitive() && fieldType != boolean.class && fieldType != char.class)) {
                    fields.add(field.getName());
                }
            }
            type = type.getSuperclass();
        }
        return fields;
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
