package xy177.tinkersplannerantique;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class PlannerConfig {

    private static Configuration configuration;

    public static boolean enableShortBlueprintCode = true;
    public static String defaultShortBlueprintListName = "ticlist";
    public static boolean autoGenerateShortBlueprintList = true;
    public static boolean creativeOnlyGiveItem = true;

    private PlannerConfig() {
    }

    public static void init(File configDir) {
        if (configuration != null) {
            return;
        }
        configuration = new Configuration(new File(configDir, TinkersPlannerAntique.MODID + ".cfg"));
        sync();
    }

    public static void sync() {
        if (configuration == null) {
            return;
        }
        enableShortBlueprintCode = configuration.getBoolean(
            "enableShortBlueprintCode",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable import/export of short blueprint codes and /ticpa list output generation.\n"
                + "启用简短蓝图码的导入导出与 /ticpa list output 生成功能。"
        );
        defaultShortBlueprintListName = configuration.getString(
            "defaultValidationName",
            Configuration.CATEGORY_GENERAL,
            "ticlist",
            "Default validation name used when generating a short blueprint list without an explicit name.\n"
                + "在未显式指定名称时，用于生成简短蓝图列表的默认验证名。"
        ).trim();
        if (defaultShortBlueprintListName.isEmpty()) {
            defaultShortBlueprintListName = "ticlist";
        }
        autoGenerateShortBlueprintList = configuration.getBoolean(
            "autoGenerateWhenMissing",
            Configuration.CATEGORY_GENERAL,
            true,
            "Automatically generate planner_short_list.dat with the configured default validation name when the file is missing.\n"
                + "当缺少 list 文件时，自动使用已配置的默认验证名生成 planner_short_list.dat。"
        );
        creativeOnlyGiveItem = configuration.getBoolean(
            "creativeOnlyGiveItem",
            Configuration.CATEGORY_GENERAL,
            true,
            "Allow the planner's give-item feature only in creative mode.\n"
                + "将蓝图页面的“获取物品”功能限制在创造模式下使用。"
        );
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
