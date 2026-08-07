package xy177.tinkersplannerantique.client.planner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import slimeknights.tconstruct.library.materials.IMaterialStats;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import xy177.tinkersplannerantique.MaterialPowerConfig;

final class MaterialPowerScorer {

    private static final Map<String, Double> scoreCache = new HashMap<>();

    private MaterialPowerScorer() {
    }

    static void clearCache() {
        scoreCache.clear();
    }

    static boolean isEnabled() {
        return MaterialPowerConfig.isEnabled();
    }

    static boolean hasApplicableWeights(PlannerTarget.TargetType targetType, PartMaterialType partType) {
        if (!isEnabled() || partType == null) {
            return false;
        }
        if (MaterialPowerConfig.isFormulaEnabled(targetType == null ? null : targetType.name())) {
            return true;
        }
        for (String statType : PlannerUiCache.getUsedStatTypes(partType, targetType)) {
            if (MaterialPowerConfig.hasApplicableStatWeight(statType)) {
                return true;
            }
        }
        return MaterialPowerConfig.hasTraitWeights();
    }

    static int compare(PlannerTarget.TargetType targetType, PartMaterialType partType, Material left, Material right) {
        return Double.compare(getScore(targetType, partType, right), getScore(targetType, partType, left));
    }

    static double getScore(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        if (!isEnabled() || partType == null || material == null) {
            return 0D;
        }
        String key = buildCacheKey(targetType, partType, material);
        Double cached = scoreCache.get(key);
        if (cached != null) {
            return cached.doubleValue();
        }
        double score = computeScore(targetType, partType, material);
        scoreCache.put(key, Double.valueOf(score));
        return score;
    }

    static ScoreBreakdown explainScore(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        ScoreBreakdown breakdown = new ScoreBreakdown();
        if (!isEnabled() || partType == null || material == null) {
            return breakdown;
        }
        Map<String, Double> variables = collectVariables(targetType, partType, material);
        for (Map.Entry<String, Double> entry : MaterialPowerConfig.getStatWeights().entrySet()) {
            int separator = entry.getKey().indexOf('.');
            if (separator <= 0 || separator >= entry.getKey().length() - 1) {
                continue;
            }
            String statType = entry.getKey().substring(0, separator);
            String fieldName = entry.getKey().substring(separator + 1);
            if ("*".equals(statType)) {
                for (String usedStatType : PlannerUiCache.getUsedStatTypes(partType, targetType)) {
                    addStatContribution(breakdown, entry.getKey(), usedStatType, material.getStats(usedStatType), fieldName, entry.getValue().doubleValue());
                }
            } else if (partType.usesStat(statType)) {
                addStatContribution(breakdown, entry.getKey(), statType, material.getStats(statType), fieldName, entry.getValue().doubleValue());
            }
        }
        addTraitBreakdown(breakdown, targetType, partType, material);
        breakdown.weightTotal = breakdown.total;
        breakdown.variables.putAll(variables);
        if (MaterialPowerConfig.isFormulaEnabled(targetType == null ? null : targetType.name())) {
            breakdown.formulaMode = true;
            breakdown.formulaExpression = MaterialPowerConfig.getFormulaExpression(targetType == null ? null : targetType.name());
            breakdown.total = MaterialPowerFormula.evaluate(breakdown.formulaExpression, variables);
        }
        return breakdown;
    }

    private static double computeScore(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        if (MaterialPowerConfig.isFormulaEnabled(targetType == null ? null : targetType.name())) {
            return MaterialPowerFormula.evaluate(MaterialPowerConfig.getFormulaExpression(targetType == null ? null : targetType.name()), collectVariables(targetType, partType, material));
        }
        double score = 0D;
        for (Map.Entry<String, Double> entry : MaterialPowerConfig.getStatWeights().entrySet()) {
            int separator = entry.getKey().indexOf('.');
            if (separator <= 0 || separator >= entry.getKey().length() - 1) {
                continue;
            }
            String statType = entry.getKey().substring(0, separator);
            String fieldName = entry.getKey().substring(separator + 1);
            if ("*".equals(statType)) {
                score += computeWildcardStatScore(targetType, partType, material, fieldName, entry.getValue().doubleValue());
            } else {
                if (!partType.usesStat(statType)) {
                    continue;
                }
                IMaterialStats stats = material.getStats(statType);
                if (stats == null) {
                    continue;
                }
                score += readNumberField(stats, fieldName) * entry.getValue().doubleValue();
            }
        }
        score += computeTraitScore(targetType, partType, material);
        return score;
    }

    private static Map<String, Double> collectVariables(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        Map<String, Double> variables = new LinkedHashMap<>();
        for (String statType : PlannerUiCache.getUsedStatTypes(partType, targetType)) {
            IMaterialStats stats = material.getStats(statType);
            if (stats == null) {
                continue;
            }
            collectStatVariables(variables, statType, stats);
        }
        collectTraitVariables(variables, targetType, partType, material);
        return variables;
    }

    private static void collectStatVariables(Map<String, Double> variables, String statType, IMaterialStats stats) {
        Class<?> type = stats.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!Number.class.isAssignableFrom(fieldType) && !(fieldType.isPrimitive() && fieldType != boolean.class && fieldType != char.class)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object rawValue = field.get(stats);
                    if (!(rawValue instanceof Number)) {
                        continue;
                    }
                    double value = ((Number) rawValue).doubleValue();
                    String fieldName = field.getName().toLowerCase(Locale.ROOT);
                    variables.put((statType + "." + fieldName).toLowerCase(Locale.ROOT), Double.valueOf(value));
                    String wildcardKey = ("*." + fieldName).toLowerCase(Locale.ROOT);
                    Double previous = variables.get(wildcardKey);
                    variables.put(wildcardKey, Double.valueOf((previous == null ? 0D : previous.doubleValue()) + value));
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void collectTraitVariables(Map<String, Double> variables, PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        Map<String, Double> weights = MaterialPowerConfig.getTraitWeights();
        Set<String> traits = PlannerUiCache.getPartTraitIdentifiers(partType, material, targetType);
        double sum = 0D;
        double max = 0D;
        int count = 0;
        for (String traitId : traits) {
            Double value = weights.get(traitId.toLowerCase(Locale.ROOT));
            if (value == null) {
                continue;
            }
            variables.put(("trait." + traitId).toLowerCase(Locale.ROOT), value);
            sum += value.doubleValue();
            max = Math.max(max, value.doubleValue());
            count++;
        }
        variables.put("traits.total", Double.valueOf(sum));
        variables.put("traits.sum", Double.valueOf(sum));
        variables.put("traits.max", Double.valueOf(max));
        variables.put("traits.count", Double.valueOf(count));
        variables.put("traits.average", Double.valueOf(count == 0 ? 0D : sum / count));
    }

    private static double computeWildcardStatScore(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material, String fieldName, double weight) {
        double score = 0D;
        for (String statType : PlannerUiCache.getUsedStatTypes(partType, targetType)) {
            IMaterialStats stats = material.getStats(statType);
            if (stats == null) {
                continue;
            }
            score += readNumberField(stats, fieldName) * weight;
        }
        return score;
    }

    private static double computeTraitScore(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        Map<String, Double> weights = MaterialPowerConfig.getTraitWeights();
        if (weights.isEmpty()) {
            return 0D;
        }
        Set<String> traits = PlannerUiCache.getPartTraitIdentifiers(partType, material, targetType);
        if (traits.isEmpty()) {
            return 0D;
        }
        List<Double> matches = new ArrayList<>();
        for (String traitId : traits) {
            Double value = weights.get(traitId.toLowerCase(Locale.ROOT));
            if (value != null) {
                matches.add(value);
            }
        }
        if (matches.isEmpty()) {
            return 0D;
        }
        String mode = MaterialPowerConfig.getTraitMode();
        if ("max".equals(mode)) {
            double max = 0D;
            for (Double value : matches) {
                max = Math.max(max, value.doubleValue());
            }
            return max;
        }
        double sum = 0D;
        for (Double value : matches) {
            sum += value.doubleValue();
        }
        if ("sum".equals(mode)) {
            return sum;
        }
        return sum / matches.size();
    }

    private static void addStatContribution(ScoreBreakdown breakdown, String configKey, String statType, IMaterialStats stats, String fieldName, double weight) {
        if (stats == null) {
            return;
        }
        Double value = readNumberFieldOrNull(stats, fieldName);
        if (value == null) {
            return;
        }
        double contribution = value.doubleValue() * weight;
        breakdown.statContributions.add(new StatContribution(configKey, statType, fieldName, value.doubleValue(), weight, contribution));
        breakdown.statScore += contribution;
        breakdown.total += contribution;
    }

    private static void addTraitBreakdown(ScoreBreakdown breakdown, PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        Map<String, Double> weights = MaterialPowerConfig.getTraitWeights();
        if (weights.isEmpty()) {
            return;
        }
        Set<String> traits = PlannerUiCache.getPartTraitIdentifiers(partType, material, targetType);
        for (String traitId : traits) {
            Double value = weights.get(traitId.toLowerCase(Locale.ROOT));
            if (value != null) {
                breakdown.traitMatches.add(new TraitContribution(traitId, value.doubleValue()));
            }
        }
        if (breakdown.traitMatches.isEmpty()) {
            return;
        }
        String mode = MaterialPowerConfig.getTraitMode();
        double sum = 0D;
        double max = 0D;
        for (TraitContribution contribution : breakdown.traitMatches) {
            sum += contribution.weight;
            max = Math.max(max, contribution.weight);
        }
        if ("max".equals(mode)) {
            breakdown.traitScore = max;
        } else if ("sum".equals(mode)) {
            breakdown.traitScore = sum;
        } else {
            breakdown.traitScore = sum / breakdown.traitMatches.size();
        }
        breakdown.traitMode = mode;
        breakdown.total += breakdown.traitScore;
    }

    private static double readNumberField(IMaterialStats stats, String fieldName) {
        Double value = readNumberFieldOrNull(stats, fieldName);
        return value == null ? 0D : value.doubleValue();
    }

    private static Double readNumberFieldOrNull(IMaterialStats stats, String fieldName) {
        Class<?> type = stats.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!normalize(field.getName()).equals(normalize(fieldName))) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (!Number.class.isAssignableFrom(fieldType) && !(fieldType.isPrimitive() && fieldType != boolean.class && fieldType != char.class)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(stats);
                    return value instanceof Number ? Double.valueOf(((Number) value).doubleValue()) : null;
                } catch (IllegalAccessException ignored) {
                    return null;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static String buildCacheKey(PlannerTarget.TargetType targetType, PartMaterialType partType, Material material) {
        StringBuilder builder = new StringBuilder();
        builder.append(targetType == null ? "" : targetType.name()).append('|');
        for (String statType : PlannerUiCache.getUsedStatTypes(partType, targetType)) {
            builder.append(statType).append(',');
        }
        builder.append('|').append(material.getIdentifier());
        builder.append('|').append(MaterialPowerConfig.getFingerprint());
        return builder.toString();
    }

    static final class ScoreBreakdown {
        double statScore;
        double traitScore;
        double weightTotal;
        double total;
        String traitMode = MaterialPowerConfig.getTraitMode();
        boolean formulaMode;
        String formulaExpression = "";
        final List<StatContribution> statContributions = new ArrayList<>();
        final List<TraitContribution> traitMatches = new ArrayList<>();
        final Map<String, Double> variables = new LinkedHashMap<>();
    }

    static final class StatContribution {
        final String configKey;
        final String statType;
        final String fieldName;
        final double value;
        final double weight;
        final double contribution;

        StatContribution(String configKey, String statType, String fieldName, double value, double weight, double contribution) {
            this.configKey = configKey;
            this.statType = statType;
            this.fieldName = fieldName;
            this.value = value;
            this.weight = weight;
            this.contribution = contribution;
        }
    }

    static final class TraitContribution {
        final String traitId;
        final double weight;

        TraitContribution(String traitId, double weight) {
            this.traitId = traitId;
            this.weight = weight;
        }
    }
}
