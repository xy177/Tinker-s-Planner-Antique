package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;

final class MaterialPowerScoreExporter {

    private static final String FILE_NAME = "material_power_scores.json";

    private MaterialPowerScoreExporter() {
    }

    static File export(File folder, List<? extends PlannerTarget> targets) throws IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Unable to create folder: " + folder.getAbsolutePath());
        }
        File file = new File(folder, FILE_NAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writeProperty(writer, "enabled", Boolean.toString(MaterialPowerScorer.isEnabled()), false, 1);
            writer.write(",\n");
            writeStringProperty(writer, "fingerprint", xy177.tinkersplannerantique.MaterialPowerConfig.getFingerprint(), false, 1);
            writer.write(",\n");
            writeTargets(writer, targets);
            writer.write("\n}\n");
        }
        return file;
    }

    private static void writeTargets(Writer writer, List<? extends PlannerTarget> targets) throws IOException {
        writer.write("  \"targets\": [\n");
        if (targets != null) {
            for (int i = 0; i < targets.size(); i++) {
                PlannerTarget target = targets.get(i);
                writer.write("    {\n");
                writeStringProperty(writer, "id", target.getItem() == null || target.getItem().getRegistryName() == null ? target.getDisplayName() : target.getItem().getRegistryName().toString(), true, 3);
                writer.write(",\n");
                writeStringProperty(writer, "name", target.getDisplayName(), true, 3);
                writer.write(",\n");
                writeStringProperty(writer, "type", target.getType().name().toLowerCase(Locale.ROOT), true, 3);
                writer.write(",\n");
                writeParts(writer, target);
                writer.write("\n    }");
                if (i < targets.size() - 1) {
                    writer.write(',');
                }
                writer.write('\n');
            }
        }
        writer.write("  ]");
    }

    private static void writeParts(Writer writer, PlannerTarget target) throws IOException {
        writer.write("      \"parts\": [\n");
        List<PartMaterialType> parts = target.getRequiredComponents();
        for (int i = 0; i < parts.size(); i++) {
            PartMaterialType partType = parts.get(i);
            writer.write("        {\n");
            writeStringProperty(writer, "index", Integer.toString(i), false, 5);
            writer.write(",\n");
            writeStringProperty(writer, "stat_types", String.join(",", PlannerUiCache.getUsedStatTypes(partType, target.getType())), true, 5);
            writer.write(",\n");
            writeMaterials(writer, target, partType);
            writer.write("\n        }");
            if (i < parts.size() - 1) {
                writer.write(',');
            }
            writer.write('\n');
        }
        writer.write("      ]");
    }

    private static void writeMaterials(Writer writer, PlannerTarget target, PartMaterialType partType) throws IOException {
        writer.write("          \"materials\": [\n");
        List<Material> materials = collectMaterials(partType);
        Collections.sort(materials, new Comparator<Material>() {
            @Override
            public int compare(Material left, Material right) {
                return MaterialPowerScorer.compare(target.getType(), partType, left, right);
            }
        });
        for (int i = 0; i < materials.size(); i++) {
            Material material = materials.get(i);
            MaterialPowerScorer.ScoreBreakdown breakdown = MaterialPowerScorer.explainScore(target.getType(), partType, material);
            writer.write("            {\n");
            writeStringProperty(writer, "id", material.getIdentifier(), true, 7);
            writer.write(",\n");
            writeStringProperty(writer, "name", material.getLocalizedName(), true, 7);
            writer.write(",\n");
            writeNumberProperty(writer, "total", breakdown.total, 7);
            writer.write(",\n");
            writeNumberProperty(writer, "stat_score", breakdown.statScore, 7);
            writer.write(",\n");
            writeNumberProperty(writer, "trait_score", breakdown.traitScore, 7);
            writer.write(",\n");
            writeNumberProperty(writer, "weight_total", breakdown.weightTotal, 7);
            writer.write(",\n");
            writeStringProperty(writer, "trait_mode", breakdown.traitMode, true, 7);
            writer.write(",\n");
            writeProperty(writer, "formula_mode", Boolean.toString(breakdown.formulaMode), false, 7);
            writer.write(",\n");
            writeStringProperty(writer, "formula_expression", breakdown.formulaExpression, true, 7);
            writer.write(",\n");
            writeStringProperty(writer, "formula", buildFormula(breakdown), true, 7);
            writer.write(",\n");
            writeStatContributions(writer, breakdown.statContributions);
            writer.write(",\n");
            writeTraitContributions(writer, breakdown.traitMatches);
            writer.write("\n            }");
            if (i < materials.size() - 1) {
                writer.write(',');
            }
            writer.write('\n');
        }
        writer.write("          ]");
    }

    private static List<Material> collectMaterials(PartMaterialType partType) {
        List<Material> materials = new ArrayList<>();
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material != null && material != Material.UNKNOWN && partType.isValidMaterial(material)) {
                materials.add(material);
            }
        }
        return materials;
    }

    private static String buildFormula(MaterialPowerScorer.ScoreBreakdown breakdown) {
        if (breakdown.formulaMode) {
            return breakdown.formulaExpression + " with {" + formatVariables(breakdown.variables) + "} = " + format(breakdown.total);
        }
        List<String> terms = new ArrayList<>();
        for (MaterialPowerScorer.StatContribution contribution : breakdown.statContributions) {
            terms.add(contribution.configKey + "[" + contribution.statType + "." + contribution.fieldName + "](" + format(contribution.value) + " * " + format(contribution.weight) + ")");
        }
        if (!breakdown.traitMatches.isEmpty()) {
            List<String> traitValues = new ArrayList<>();
            for (MaterialPowerScorer.TraitContribution contribution : breakdown.traitMatches) {
                traitValues.add(contribution.traitId + ":" + format(contribution.weight));
            }
            terms.add("traits." + breakdown.traitMode + "(" + join(traitValues, ", ") + ")");
        }
        if (terms.isEmpty()) {
            terms.add("0");
        }
        return join(terms, " + ") + " = " + format(breakdown.total);
    }

    private static void writeStatContributions(Writer writer, List<MaterialPowerScorer.StatContribution> contributions) throws IOException {
        writer.write("              \"stat_contributions\": [\n");
        for (int i = 0; i < contributions.size(); i++) {
            MaterialPowerScorer.StatContribution contribution = contributions.get(i);
            writer.write("                {");
            writeInlineStringProperty(writer, "config_key", contribution.configKey);
            writer.write(", ");
            writeInlineStringProperty(writer, "stat_type", contribution.statType);
            writer.write(", ");
            writeInlineStringProperty(writer, "field", contribution.fieldName);
            writer.write(", \"value\": " + contribution.value);
            writer.write(", \"weight\": " + contribution.weight);
            writer.write(", \"contribution\": " + contribution.contribution);
            writer.write("}");
            if (i < contributions.size() - 1) {
                writer.write(',');
            }
            writer.write('\n');
        }
        writer.write("              ]");
    }

    private static void writeTraitContributions(Writer writer, List<MaterialPowerScorer.TraitContribution> contributions) throws IOException {
        writer.write("              \"trait_matches\": [\n");
        for (int i = 0; i < contributions.size(); i++) {
            MaterialPowerScorer.TraitContribution contribution = contributions.get(i);
            writer.write("                {");
            writeInlineStringProperty(writer, "trait", contribution.traitId);
            writer.write(", \"weight\": " + contribution.weight);
            writer.write("}");
            if (i < contributions.size() - 1) {
                writer.write(',');
            }
            writer.write('\n');
        }
        writer.write("              ]");
    }

    private static void writeStringProperty(Writer writer, String name, String value, boolean quoted, int indent) throws IOException {
        indent(writer, indent);
        writeString(writer, name);
        writer.write(": ");
        if (quoted) {
            writeString(writer, value);
        } else {
            writer.write(value);
        }
    }

    private static void writeNumberProperty(Writer writer, String name, double value, int indent) throws IOException {
        indent(writer, indent);
        writeString(writer, name);
        writer.write(": " + value);
    }

    private static void writeProperty(Writer writer, String name, String value, boolean quoted, int indent) throws IOException {
        writeStringProperty(writer, name, value, quoted, indent);
    }

    private static void writeInlineStringProperty(Writer writer, String name, String value) throws IOException {
        writeString(writer, name);
        writer.write(": ");
        writeString(writer, value);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static String formatVariables(java.util.Map<String, Double> variables) {
        List<String> values = new ArrayList<>();
        for (java.util.Map.Entry<String, Double> entry : variables.entrySet()) {
            values.add(entry.getKey() + "=" + format(entry.getValue().doubleValue()));
        }
        return join(values, ", ");
    }

    private static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static void indent(Writer writer, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            writer.write("  ");
        }
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
}
