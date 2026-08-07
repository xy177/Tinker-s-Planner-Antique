Tinkers Planner Antique
=======================

Tinkers Planner Antique is a helper mod for planning Tinkers' Construct tools and Construct's Armory armor. It provides a separate blueprint screen where you can choose tools, armor, parts, materials, modifiers, and embossments before actually crafting them, then preview the result directly.

If you often compare materials, modifier slots, traits, and special stats from addon mods in large modpacks, this mod lets you handle most of that trial and error in one place.


Feature Overview
----------------

In the planner screen, you can select a tool or armor type, then choose materials for each part. The screen generates a preview item in real time and tries to calculate the final result through the real Tinkers construction flow.

The material list supports sorting. Common stats such as attack, mining speed, durability, armor, and toughness appear as sort buttons; special part stats added by some addon mods can also be detected and added automatically.

The search box filters materials, traits, modifiers, and embossment options by localized name or registry ID. Prefix the query with `#` to search tooltip text. List pages retain their positions; Shift-clicking a page arrow moves five pages, Ctrl-clicking jumps to the first or last page, and the mouse wheel pages the list under the pointer.

Modifiers can be added or removed directly from the modifier list, and their tooltips list the item names used to apply them. Fortified tool materials can also be sorted by harvest level.

When selecting part or embossment materials, a trait filter list appears in the lower-left panel. If no specific material is selected, it shows traits available from materials usable by the current selection. Once a material is selected, it only shows the traits that material provides. Clicking a trait filters the material list to materials with that trait.

When JEI is installed, hover a planner item icon and press `R` to open its JEI recipes. JEI remains optional.

Finished plans can be exported as blueprint codes, and blueprint codes can be imported back into the planner. Blueprint messages in chat support copy, bookmark, and share actions, which makes it easier to pass plans around in multiplayer or during modpack testing.

Blueprints can be bookmarked. Tool bookmarks and armor bookmarks are saved separately, so common plans can be found again quickly.

When allowed, the preview result can be obtained as an item. By default, this feature is limited to creative mode.


Short Code List
---------------

The mod supports shorter blueprint codes. Short codes depend on `planner_short_list.dat`, which records the index mapping for tools, armor, materials, and modifiers in the current environment.

Common commands:

- `/ticpa list output [name]`
  Generates a new short code list. If no name is provided, the default name from the config is used.

- `/ticpa list reset [name]`
  Changes the validation name of the current short code list without changing the order of existing entries.

- `/ticpa list remake [name]`
  Rebuilds the list based on the current environment while preserving existing indexes. Existing entries keep their original order, new entries are appended to the end, and missing old materials are kept as temporary placeholders instead of shortening the list. Importing a blueprint code that references one of those missing materials will show a missing-material message.

- `/ticpa test print modpack`
  Prints the current modpack's tool+armor count, material count, and modifier count.

- `/ticpa test print list`
  Prints the tool+armor count, material count, and modifier count recorded in the current short code list.

- `/ticpa test print json`
  Exports the current environment's tool, material, trait, modifier, and part registry names as a JSON file with English and Chinese localized names.

- `/ticpa test print power`
  Exports material power scores as JSON, including the stat and trait contributions used by weight mode. In formula mode, the dump records the active expression, available variables, and the final substituted calculation result.

- `/ticpa test delet <true|false>`
  Toggles debug delete mode for modifier entries. When enabled, deleted modifiers can be marked and hidden in the planner for testing. The command name is kept as `delet` for compatibility with existing 1.0.1 builds.

- `/ticpa cache refresh`
  Rebuilds the planner UI cache manually.

Material Power Sorting
----------------------

Modpack authors can use an extra material sort called `Power`. It calculates a configurable score for part materials, which is useful when a pack wants to rank materials by its own balance rules instead of a single vanilla TiC stat.

The config file is created at:

- `config/tinkersplannerantique/material_power.json`

By default this feature is disabled, so it will not affect normal players or pack balance unless a modpack author opts in. To enable it, set `"enabled": true` in `material_power.json`. The generated file still includes a small set of general weights and tool/armor formula examples that can be edited for your pack. In the material list, the material icon button shows or hides representative material icons, while the separate nether-star button toggles `Power` sorting after the feature is enabled.

Generated config files include `_comment_*` fields with bilingual descriptions. These fields are ignored by the loader and can be kept in the file.

Stat keys use the format `statType.fieldName`, such as `head.attack`, `head.miningspeed`, `handle.modifier`, `core.defense`, or `plates.toughness`. A wildcard stat type is also supported, for example `*.durability` or `*.attack`. Wildcard variables sum matching fields across the stat types used by the current part.

Trait values can be combined with one of these `aggregation.trait_mode` values:

- `average`
- `sum`
- `max`

You can also enable formula scoring:

```json
"formula": {
  "enabled": true,
  "expression": "",
  "tool_expression": "sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + (*.attack + 1) ** 2 + *.miningspeed + *.modifier * 5 + traits.average",
  "armor_expression": "sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + *.defense * 6 + *.toughness * 8 + traits.average"
}
```

When `formula.enabled` is true, the matching formula is used instead of the weight-sum result. `tool_expression` is used for tools and weapons, `armor_expression` is used for armor, and `expression` is the common fallback when the target-specific field is empty.

Formula syntax:

- Operators: `+`, `-`, `*`, `/`, `**`, and parentheses.
- Functions: `min(...)`, `max(...)`, `avg(...)`, `abs(x)`, `sqrt(x)`, `log(x)`, `floor(x)`, `ceil(x)`, `round(x)`, `sign(x)`, and `clamp(x,min,max)`.
- Stat variables: exact fields such as `head.attack` and `extra.durability`, plus wildcard fields such as `*.durability`.
- Trait variables: `traits.total`, `traits.sum`, `traits.average`, `traits.max`, `traits.count`, and single trait values such as `trait.ecological`.
- Missing variables, invalid expressions, infinite results, and NaN results are treated as `0` so sorting remains stable.

Available variables include stat fields such as `head.attack`, `extra.durability`, and wildcard fields such as `*.durability`. Trait variables include `traits.total`, `traits.sum`, `traits.average`, `traits.max`, `traits.count`, and `trait.<trait_id>`. The `stats` and `traits` tables are still useful in formula mode because trait variables are based on the configured trait weights, and `/ticpa test print power` also includes the weight-mode breakdown for comparison.

Stats added by other mods can be detected by the planner and exported with `/ticpa power fields`, but they are not automatically appended to `material_power.json` during `/ticpa power reload`. That file is treated as a pack-authored config, so new fields should be copied from `material_power_fields.json` into `stats` or the formula manually before reloading.

Useful commands:

- `/ticpa power fields`
  Exports `material_power_fields.json`, listing numeric part stat fields detected in the current environment. Use this file as a reference when writing `material_power.json`.

- `/ticpa power reload`
  Reloads `material_power.json` and clears the in-memory power score cache.

- `/ticpa test print power`
  Exports `material_power_scores.json`, showing each material's final score and the calculation string used to produce it.

For Modpack Authors
-------------------

If your pack plans to use blueprint code import/export across multiple updates, there are two things worth noting:

- Standard blueprint codes are based on TiC item NBT. They are more verbose, but generally do not have cross-version compatibility issues as long as the target environment can still resolve the corresponding NBT data.

- Short blueprint codes depend on `planner_short_list.dat`, located at `(.minecraft\versions\XXX\tinkersplannerantique\planner_short_list.dat)`. In normal use this file is generated automatically, and for the same modpack on the same version, both the generated file and the short blueprint codes will match. However, if a later version of the modpack adds or removes TiC materials, modifiers, or tools, the automatically generated file may differ from older versions, which can make short blueprint codes incompatible across versions.

To keep short blueprint codes stable across modpack versions, you can bundle `planner_short_list.dat` with the modpack and update it with `/ticpa list remake [name]` whenever TiC materials, modifiers, or tools are added or removed.

Short blueprint mode also uses a validation name to distinguish different modpacks and prevent blueprint codes from being used across unrelated environments. This validation name is included as part of the blueprint code itself. By default it comes from the `defaultValidationName` config entry, but modpack authors may want to change it to something more specific for their environment.

These notes only matter if blueprint code import/export is part of your intended workflow. They do not affect the preview functionality itself.

Config
------

`enableShortBlueprintCode` controls whether short blueprint codes and the short code list feature are enabled.

`defaultValidationName` is the default name used when generating, resetting, or remaking a short code list without an explicit name.

`autoGenerateWhenMissing` controls whether a short code list is generated automatically when missing.

`creativeOnlyGiveItem` controls whether the "give item" feature is limited to creative mode.

`enablePlannerUiCache` controls whether planner UI cache data is written automatically. When disabled, the mod will only create cache data automatically if the cache file does not already exist, and later updates must be triggered manually with `/ticpa cache refresh`.


---

匠魂蓝图怀古/意研订斟
=======================

匠魂蓝图怀古/意研订斟 是一个用于设计匠魂工具和匠魂护甲蓝图的辅助模组。它提供了一个独立的蓝图界面，让你可以在真正制作前先搭配工具、护甲、部件、材料、强化和刻印，并直接查看预览结果。

如果你经常在大型整合包里反复比较材料、强化槽、特性和各种附属模组带来的特殊属性，这个模组可以让这些试错过程集中在一个界面里完成。


功能概览
--------

你可以在蓝图界面中选择工具或护甲类型，然后逐个选择部件材料。界面会实时生成预览物品，并尽量按照真实匠魂构筑流程计算最终属性。

材料列表支持排序。常见属性如攻击力、挖掘速度、耐久、护甲值、护甲韧性等会显示为排序按钮；部分附属模组添加的特殊部件属性也会自动识别并加入排序项。

搜索框可以按本地化名称或注册名筛选材料、特性、强化和刻印选项；使用 `#` 前缀可以搜索 tooltip 文本。各列表会保留页码；Shift 点击翻页按钮会移动五页，Ctrl 点击会跳到首页或末页，鼠标滚轮会翻动指针所在的列表。

强化列表可以直接添加或移除强化，并在 tooltip 中列出强化所需物品的名称。工具的强化材料也可以按挖掘等级排序。

选择部件材料或刻印材料时，左下角会显示特性筛选列表。未选中具体材料时，它会显示当前选项可用材料拥有的特性；选中某个材料后，则只显示这个材料提供的特性。点击特性可以筛选出拥有该特性的材料。

安装 JEI 时，将鼠标悬停在蓝图界面的物品图标上并按 `R`，即可打开对应的 JEI 配方；JEI 仍为可选依赖。

设计好的蓝图可以导出为蓝图码，也可以从蓝图码导入。聊天中的蓝图码消息带有复制、收藏和分享功能，方便在多人游戏或整合包测试时传递方案。

蓝图可以加入收藏。工具收藏和护甲收藏分开保存，便于之后快速找回常用方案。

在允许的情况下，蓝图预览结果可以直接获取为物品。默认配置下，这个功能只允许创造模式使用。


短码列表
--------

模组支持更短的蓝图码。短码依赖 `planner_short_list.dat`，这个文件记录了当前环境中的工具、护甲、材料和强化对应的序号。

常用指令：

- `/ticpa list output [识别名]`
  生成新的短码列表。不填写识别名时，会使用配置里的默认名称。

- `/ticpa list reset [识别名]`
  修改当前短码列表的验证名，不改变已有条目的顺序。

- `/ticpa list remake [识别名]`
  在现有短码列表基础上重新整理当前环境。已有条目的序号保持不变，新出现的条目会追加到末尾；如果某些旧材料在当前环境中缺失，列表不会缩短，而是保留临时占位。之后导入引用这些缺失材料的蓝图码时，会给出材料缺失提示。

- `/ticpa test print modpack`
  输出当前整合包环境中的工具+护甲数量、材料数量和强化数量。

- `/ticpa test print list`
  输出当前短码列表中记录的工具+护甲数量、材料数量和强化数量。

- `/ticpa test print json`
  将当前环境中的工具、材料、特性、强化和部件注册名导出为 JSON 文件，并附带英文和中文本地化名称。

- `/ticpa test print power`
  将材料总评分数导出为 JSON 文件，并列出权重模式使用的属性贡献和特性贡献。公式模式下，导出文件还会记录当前公式、可用变量和最终带入计算结果。

- `/ticpa test delet <true|false>`
  切换调试用的删除模式。开启后，可以在蓝图界面中标记并隐藏已删除的强化条目，方便测试。命令名保留为 `delet` 是为了兼容已经发布的 1.0.1 版本。

- `/ticpa cache refresh`
  手动重建蓝图界面缓存。

材料总评排序
------------

整合包作者可以使用一个额外的材料排序项：`总评`。它会按照配置为部件材料计算综合分数，适合整合包按自己的平衡规则排序材料，而不是只按某个单独属性排序。

配置文件会生成在：

- `config/tinkersplannerantique/material_power.json`

默认情况下该功能关闭，因此不会在整合包作者主动启用前影响普通玩家或整合包平衡。需要启用时，将 `material_power.json` 中的 `"enabled"` 改为 `true`。生成的文件仍会保留一组通用权重和工具/护甲公式示例，方便按整合包需求编辑。在材料列表中，材料图标按钮用于显示或隐藏材料代表物图标；功能启用后，独立的下界之星按钮用于开启或取消 `总评` 排序。

生成的配置文件会包含 `_comment_*` 字段作为双语说明。这些字段会被读取器忽略，可以保留在文件中。

属性键使用 `statType.fieldName` 格式，例如 `head.attack`、`head.miningspeed`、`handle.modifier`、`core.defense` 或 `plates.toughness`。同时也支持通配 statType，例如 `*.durability` 或 `*.attack`。通配变量会汇总当前部件使用的所有 statType 中的同名字段。

特性分数可以通过 `aggregation.trait_mode` 使用以下方式合并：

- `average`
- `sum`
- `max`

也可以启用公式评分：

```json
"formula": {
  "enabled": true,
  "expression": "",
  "tool_expression": "sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + (*.attack + 1) ** 2 + *.miningspeed + *.modifier * 5 + traits.average",
  "armor_expression": "sqrt(abs(*.durability * 10)) * sign(*.durability) / 3 + *.defense * 6 + *.toughness * 8 + traits.average"
}
```

当 `formula.enabled` 为 `true` 时，会优先使用匹配公式结果，而不是权重相加结果。`tool_expression` 用于工具和武器，`armor_expression` 用于护甲，`expression` 是目标专用字段为空时使用的通用回退公式。

公式语法：

- 运算符：`+`、`-`、`*`、`/`、`**` 和括号。
- 函数：`min(...)`、`max(...)`、`avg(...)`、`abs(x)`、`sqrt(x)`、`log(x)`、`floor(x)`、`ceil(x)`、`round(x)`、`sign(x)` 和 `clamp(x,min,max)`。
- 属性变量：`head.attack`、`extra.durability` 这类精确字段，以及 `*.durability` 这类通配字段。
- 特性变量：`traits.total`、`traits.sum`、`traits.average`、`traits.max`、`traits.count`，以及 `trait.ecological` 这类单个特性值。
- 缺失变量、无效表达式、无穷大结果和 NaN 结果都会按 `0` 处理，以保证排序稳定。

可用变量包括 `head.attack`、`extra.durability` 这类具体属性字段，也包括 `*.durability` 这类通配字段。特性相关变量包括 `traits.total`、`traits.sum`、`traits.average`、`traits.max`、`traits.count` 和 `trait.<trait_id>`。公式模式下 `stats` 和 `traits` 表仍然有用，因为特性变量基于配置的特性权重生成，且 `/ticpa test print power` 会同时导出权重模式的拆解结果用于对照。

其他模组添加的部件属性可以被蓝图界面自动识别，并通过 `/ticpa power fields` 导出，但 `/ticpa power reload` 不会把这些字段自动追加到 `material_power.json`。这个文件会被视为整合包作者手写的配置，因此新增字段需要先从 `material_power_fields.json` 中查看，再手动写入 `stats` 或公式后重载。

相关指令：

- `/ticpa power fields`
  导出 `material_power_fields.json`，列出当前环境中检测到的数值型部件属性字段。编写 `material_power.json` 时可以参考这个文件。

- `/ticpa power reload`
  重新读取 `material_power.json`，并清空内存中的材料总评分数缓存。

- `/ticpa test print power`
  导出 `material_power_scores.json`，显示每个材料的最终分数和生成该分数的计算字符串。

给整合包作者的说明
------------------

如果整合包希望在多个版本更新之间继续使用蓝图码导入导出，有两点需要特别注意：

- 标准蓝图码基于 TiC 物品的 NBT。它会更长一些，但只要目标环境仍然能解析对应的 NBT 数据，通常不需要担心跨版本通用性问题。

- 简短蓝图码依赖 `planner_short_list.dat`，其路径位于 `(.minecraft\versions\XXX\tinkersplannerantique\planner_short_list.dat)`。正常情况下这个文件会自动生成，并且只要是同一整合包的同一版本，生成出的文件和短码都会一致。但如果整合包后续版本增减了 TiC 的材料、强化或工具，那么自动生成的文件就可能和旧版本不同，从而导致短码在跨版本时无法通用。

如果希望短码在整合包多个版本之间保持稳定，可以将 `planner_short_list.dat` 直接打包进整合包，并在 TiC 材料、强化或工具发生增减时使用 `/ticpa list remake [name]` 更新这个文件。

此外，简短蓝图码模式还会使用一个验证名来区分不同整合包，防止蓝图码被跨环境误用。这个验证名会被写入蓝图码本身，默认取自配置项 `defaultValidationName`。如果需要更方便地区分不同环境，整合包作者可以主动修改这一项。

以上内容只在你打算使用蓝图码导入导出时才需要关注，对蓝图本身的预览功能没有影响。

配置
----

`enableShortBlueprintCode` 控制是否启用简短蓝图码和短码列表功能。

`defaultValidationName` 是生成、重置或重建短码列表时未填写识别名所使用的默认名称。

`autoGenerateWhenMissing` 控制缺少短码列表时是否自动生成。

`creativeOnlyGiveItem` 控制“获取物品”功能是否仅限创造模式使用。

`enablePlannerUiCache` 控制蓝图界面缓存是否自动写入。关闭后，如果缓存文件不存在，模组仍会自动生成一次缓存；后续更新需要手动执行 `/ticpa cache refresh`。
