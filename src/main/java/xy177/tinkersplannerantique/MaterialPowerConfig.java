package xy177.tinkersplannerantique;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class MaterialPowerConfig {

    private static final String FILE_NAME = "material_power.json";
    private static final String DEFAULT_TRAIT_MODE = "average";
    private static final Map<String, Double> defaultStatWeights = new LinkedHashMap<>();
    private static File configFolder;
    private static File configFile;
    private static boolean enabled;
    private static String traitMode = DEFAULT_TRAIT_MODE;
    private static String fingerprint = "disabled";
    private static boolean formulaEnabled;
    private static String formulaExpression = "";
    private static String toolFormulaExpression = "";
    private static String armorFormulaExpression = "";
    private static final Map<String, Double> statWeights = new LinkedHashMap<>();
    private static final Map<String, Double> traitWeights = new LinkedHashMap<>();

    static {
        defaultStatWeights.put("*.durability", 0.02D);
        defaultStatWeights.put("*.attack", 2.0D);
        defaultStatWeights.put("*.miningspeed", 1.0D);
        defaultStatWeights.put("*.modifier", 8.0D);
        defaultStatWeights.put("*.defense", 6.0D);
        defaultStatWeights.put("*.toughness", 10.0D);
        defaultStatWeights.put("*.weight", 1.0D);
        defaultStatWeights.put("*.friction", 1.0D);
        defaultStatWeights.put("*.length", 1.0D);
    }

    private MaterialPowerConfig() {
    }

    public static synchronized void init(File configDir) {
        if (configFolder != null) {
            return;
        }
        configFolder = new File(configDir, TinkersPlannerAntique.MODID);
        configFile = new File(configFolder, FILE_NAME);
        reload();
    }

    public static synchronized void reload() {
        resetDefaults();
        ensureConfigFile();
        if (configFile == null || !configFile.exists()) {
            return;
        }
        try {
            String text = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            loadFromJson(text);
            fingerprint = Integer.toHexString(text.hashCode());
        } catch (IOException ignored) {
            fingerprint = "io_error";
        }
    }

    public static synchronized boolean isEnabled() {
        return enabled && (!statWeights.isEmpty() || !traitWeights.isEmpty() || isFormulaEnabled());
    }

    public static synchronized String getTraitMode() {
        return traitMode;
    }

    public static synchronized String getFingerprint() {
        return fingerprint;
    }

    public static synchronized boolean isFormulaEnabled() {
        return formulaEnabled && (!isBlank(formulaExpression) || !isBlank(toolFormulaExpression) || !isBlank(armorFormulaExpression));
    }

    public static synchronized boolean isFormulaEnabled(String targetType) {
        return formulaEnabled && !isBlank(getFormulaExpression(targetType));
    }

    public static synchronized String getFormulaExpression() {
        return formulaExpression;
    }

    public static synchronized String getFormulaExpression(String targetType) {
        String specific = "";
        if ("ARMOR".equalsIgnoreCase(targetType)) {
            specific = armorFormulaExpression;
        } else if ("TOOL".equalsIgnoreCase(targetType)) {
            specific = toolFormulaExpression;
        }
        return isBlank(specific) ? formulaExpression : specific;
    }

    public static synchronized Map<String, Double> getStatWeights() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(statWeights));
    }

    public static synchronized Map<String, Double> getTraitWeights() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(traitWeights));
    }

    public static synchronized boolean hasApplicableStatWeight(String statType) {
        if (statType == null || statType.isEmpty()) {
            return false;
        }
        String prefix = statType.toLowerCase(Locale.ROOT) + ".";
        for (String key : statWeights.keySet()) {
            if (key.startsWith(prefix) || key.startsWith("*.")) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean hasTraitWeights() {
        return !traitWeights.isEmpty();
    }

    public static synchronized File getConfigFolder() {
        return configFolder;
    }

    public static synchronized File getConfigFile() {
        return configFile;
    }

    private static void loadFromJson(String text) {
        JsonObject root = parseJson(text);
        if (root == null) {
            return;
        }
        JsonElement enabledValue = root.get("enabled");
        if (enabledValue != null && enabledValue.isJsonPrimitive() && enabledValue.getAsJsonPrimitive().isBoolean()) {
            enabled = enabledValue.getAsBoolean();
        }
        JsonElement stats = root.get("stats");
        if (stats != null && stats.isJsonObject()) {
            readWeights(statWeights, stats.getAsJsonObject());
        }
        JsonElement traits = root.get("traits");
        if (traits != null && traits.isJsonObject()) {
            readWeights(traitWeights, traits.getAsJsonObject());
        }
        JsonElement aggregation = root.get("aggregation");
        if (aggregation != null && aggregation.isJsonObject()) {
            JsonElement mode = aggregation.getAsJsonObject().get("trait_mode");
            if (mode != null && mode.isJsonPrimitive() && mode.getAsJsonPrimitive().isString()) {
                String textMode = mode.getAsString().trim();
                if (!textMode.isEmpty()) {
                    traitMode = textMode.toLowerCase(Locale.ROOT);
                }
            }
        }
        JsonElement formula = root.get("formula");
        if (formula != null && formula.isJsonObject()) {
            JsonObject formulaObject = formula.getAsJsonObject();
            JsonElement formulaEnabledValue = formulaObject.get("enabled");
            if (formulaEnabledValue != null && formulaEnabledValue.isJsonPrimitive() && formulaEnabledValue.getAsJsonPrimitive().isBoolean()) {
                formulaEnabled = formulaEnabledValue.getAsBoolean();
            }
            JsonElement expression = formulaObject.get("expression");
            if (expression != null && expression.isJsonPrimitive() && expression.getAsJsonPrimitive().isString()) {
                formulaExpression = expression.getAsString().trim();
            }
            JsonElement toolExpression = formulaObject.get("tool_expression");
            if (toolExpression != null && toolExpression.isJsonPrimitive() && toolExpression.getAsJsonPrimitive().isString()) {
                toolFormulaExpression = toolExpression.getAsString().trim();
            }
            JsonElement armorExpression = formulaObject.get("armor_expression");
            if (armorExpression != null && armorExpression.isJsonPrimitive() && armorExpression.getAsJsonPrimitive().isString()) {
                armorFormulaExpression = armorExpression.getAsString().trim();
            }
        }
    }

    private static void readWeights(Map<String, Double> target, JsonObject source) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            Double value = toDouble(entry.getValue());
            if (value == null) {
                continue;
            }
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty()) {
                target.put(key, value);
            }
        }
    }

    private static Double toDouble(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        if (value.getAsJsonPrimitive().isNumber()) {
            return Double.valueOf(value.getAsDouble());
        }
        if (value.getAsJsonPrimitive().isString()) {
            try {
                return Double.valueOf(Double.parseDouble(value.getAsString().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static JsonObject parseJson(String text) {
        try {
            JsonElement element = new JsonParser().parse(text);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void ensureConfigFile() {
        if (configFolder == null || configFile == null) {
            return;
        }
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        if (configFile.exists()) {
            return;
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"_comment_en_us\": \"Material power sorting config. Use /ticpa power fields to export available stat fields, /ticpa test print power to inspect score calculations, and /ticpa power reload after editing this file.\",\n");
            writer.write("  \"_comment_zh_cn\": \"材料总评排序配置。使用 /ticpa power fields 导出可用属性字段，使用 /ticpa test print power 查看评分计算过程，修改本文件后使用 /ticpa power reload 重载。\",\n");
            writer.write("  \"enabled\": false,\n");
            writer.write("  \"_enabled_en_us\": \"Set to true to enable the Power sort. It is disabled by default so modpack authors can decide whether to use custom scoring.\",\n");
            writer.write("  \"_enabled_zh_cn\": \"设为 true 可启用总评排序。该功能默认关闭，方便整合包作者自行决定是否使用自定义评分。\",\n");
            writer.write("  \"aggregation\": {\n");
            writer.write("    \"_comment_en_us\": \"How matched trait weights are combined in weight mode. Formula mode exposes traits.average / traits.sum / traits.max separately, so the expression can choose one directly. Supported values: average, sum, max.\",\n");
            writer.write("    \"_comment_zh_cn\": \"控制权重模式下命中特性分数的合并方式。公式模式会分别提供 traits.average / traits.sum / traits.max，公式可以直接选择要使用哪一个。可用值：average、sum、max。\",\n");
            writer.write("    \"trait_mode\": \"average\"\n");
            writer.write("  },\n");
            writer.write("  \"formula\": {\n");
            writer.write("    \"_comment_en_us\": \"When enabled is true, formulas are used as the final score instead of the stats/traits weight sum. tool_expression is used for tools/weapons, armor_expression is used for armor, and expression is the common fallback.\",\n");
            writer.write("    \"_syntax_en_us\": \"Operators: +, -, *, /, **, parentheses. Functions: min(...), max(...), avg(...), abs(x), sqrt(x), log(x), floor(x), ceil(x), round(x), sign(x), clamp(x,min,max). Variables: exact stat fields such as head.attack, wildcard fields such as *.durability, trait totals such as traits.sum / traits.average / traits.max / traits.count, and single traits such as trait.ecological. Missing variables and invalid formula results are treated as 0.\",\n");
            writer.write("    \"_comment_zh_cn\": \"当 enabled 为 true 时，公式结果会作为最终评分，替代 stats/traits 的权重相加结果。tool_expression 用于工具/武器，armor_expression 用于护甲，expression 是通用回退公式。\",\n");
            writer.write("    \"_syntax_zh_cn\": \"运算符：+、-、*、/、** 和括号。函数：min(...)、max(...)、avg(...)、abs(x)、sqrt(x)、log(x)、floor(x)、ceil(x)、round(x)、sign(x)、clamp(x,min,max)。变量：head.attack 这类精确属性字段、*.durability 这类通配字段、traits.sum / traits.average / traits.max / traits.count 这类特性汇总变量，以及 trait.ecological 这类单个特性变量。缺失变量和无效公式结果会按 0 处理。\",\n");
            writer.write("    \"enabled\": false,\n");
            writer.write("    \"expression\": \"\",\n");
            writer.write("    \"tool_expression\": \"sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + (*.attack + 1) ** 2 + *.miningspeed + *.modifier * 5 + traits.average\",\n");
            writer.write("    \"armor_expression\": \"sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + *.defense * 6 + *.toughness * 8 + traits.average\"\n");
            writer.write("  },\n");
            writer.write("  \"stats\": {\n");
            writer.write("    \"_comment_en_us\": \"Weight-mode stat weights. Keys use statType.fieldName. Wildcard keys such as *.durability sum matching fields across the current part's stat types.\",\n");
            writer.write("    \"_comment_zh_cn\": \"权重模式下的属性权重。键名格式为 statType.fieldName。*.durability 这类通配键会汇总当前部件使用的所有 statType 中的同名字段。\",\n");
            int index = 0;
            for (Map.Entry<String, Double> entry : defaultStatWeights.entrySet()) {
                if (index > 0) {
                    writer.write(",\n");
                }
                writer.write("    \"" + entry.getKey() + "\": " + entry.getValue());
                index++;
            }
            writer.write("\n");
            writer.write("  },\n");
            writer.write("  \"traits\": {\n");
            writer.write("    \"_comment_en_us\": \"Trait weights used by weight mode and trait variables. Use trait registry names without the trait. prefix.\",\n");
            writer.write("    \"_comment_zh_cn\": \"特性权重会被权重模式和特性变量使用。这里填写不带 trait. 前缀的特性注册名。\",\n");
            writer.write("    \"ecological\": 8.0,\n");
            writer.write("    \"autosmelt\": 20.0\n");
            writer.write("  }\n");
            writer.write("}\n");
        } catch (IOException ignored) {
        }
    }

    private static void resetDefaults() {
        enabled = false;
        traitMode = DEFAULT_TRAIT_MODE;
        fingerprint = "default";
        formulaEnabled = false;
        formulaExpression = "";
        toolFormulaExpression = "";
        armorFormulaExpression = "";
        statWeights.clear();
        statWeights.putAll(defaultStatWeights);
        traitWeights.clear();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
