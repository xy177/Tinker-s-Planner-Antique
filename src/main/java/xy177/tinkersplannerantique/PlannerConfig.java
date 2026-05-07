package xy177.tinkersplannerantique;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class PlannerConfig {

    private static Configuration configuration;

    public static boolean enableShortBlueprintCode = true;
    public static String defaultShortBlueprintListName = "ticlist";
    public static boolean autoGenerateShortBlueprintList = true;
    public static boolean creativeOnlyGiveItem = true;
    public static boolean enablePlannerUiCache = true;

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
        enablePlannerUiCache = configuration.getBoolean(
            "enablePlannerUiCache",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable planner UI emboss cache persistence. When disabled, existing cache entries can still be used, but new cache data is only written when the cache file does not exist, or when the cache is refreshed manually."
                + "\n启用蓝图界面刻印缓存的持久化。关闭后，已存在的缓存仍可使用，但新的缓存数据只会在缓存文件不存在时，或在手动刷新缓存时写入。"
        );
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
