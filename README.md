Tinkers Planner Antique
=======================

Tinkers Planner Antique is a helper mod for planning Tinkers' Construct tools and Construct's Armory armor. It provides a separate blueprint screen where you can choose tools, armor, parts, materials, modifiers, and embossments before actually crafting them, then preview the result directly.

If you often compare materials, modifier slots, traits, and special stats from addon mods in large modpacks, this mod lets you handle most of that trial and error in one place.


Version 1.0.2
-------------

- Added `/ticpa test print json` to export the current environment's tool, material, trait, modifier, and part registry names into a JSON file with both English and Chinese localized names.
- The JSON export excludes special emboss entries, second emboss entries, and material-special entries.


Feature Overview
----------------

In the planner screen, you can select a tool or armor type, then choose materials for each part. The screen generates a preview item in real time and tries to calculate the final result through the real Tinkers construction flow.

The material list supports sorting. Common stats such as attack, mining speed, durability, armor, and toughness appear as sort buttons; special part stats added by some addon mods can also be detected and added automatically.

Modifiers can be added or removed directly from the modifier list, making it easier to check the final result before crafting.

When selecting part materials, a trait filter list appears in the lower-left panel. If no specific material is selected, it shows traits available from materials usable by the current part. Once a material is selected, it only shows the traits that material provides on the current part. Clicking a trait filters the material list to materials with that trait.

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

- `/ticpa test delet <true|false>`
  Toggles debug delete mode for modifier entries. When enabled, deleted modifiers can be marked and hidden in the planner for testing. The command name is kept as `delet` for compatibility with existing 1.0.1 builds.

- `/ticpa cache refresh`
  Rebuilds the planner UI cache manually.

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


1.0.2 更新日志
--------------

- 新增 `/ticpa test print json`，可将当前环境中的工具、材料、特性、强化和部件注册名导出为 JSON 文件，并附带英文和中文本地化名称。
- 导出的内容会自动排除特殊刻印、刻印2以及材料特殊项。


功能概览
--------

你可以在蓝图界面中选择工具或护甲类型，然后逐个选择部件材料。界面会实时生成预览物品，并尽量按照真实匠魂构筑流程计算最终属性。

材料列表支持排序。常见属性如攻击力、挖掘速度、耐久、护甲值、护甲韧性等会显示为排序按钮；部分附属模组添加的特殊部件属性也会自动识别并加入排序项。

强化列表可以直接添加或移除强化，方便你在制作前确认最终结果。

部件材料选择时，左下角会显示特性筛选列表。未选中具体材料时，它会显示当前部件可用材料拥有的特性；选中某个材料后，则只显示这个材料在当前部件上的特性。点击特性可以筛选出拥有该特性的材料。

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

- `/ticpa test delet <true|false>`
  切换调试用的删除模式。开启后，可以在蓝图界面中标记并隐藏已删除的强化条目，方便测试。命令名保留为 `delet` 是为了兼容已经发布的 1.0.1 版本。

- `/ticpa cache refresh`
  手动重建蓝图界面缓存。

配置
----

`enableShortBlueprintCode` 控制是否启用简短蓝图码和短码列表功能。

`defaultValidationName` 是生成、重置或重建短码列表时未填写识别名所使用的默认名称。

`autoGenerateWhenMissing` 控制缺少短码列表时是否自动生成。

`creativeOnlyGiveItem` 控制“获取物品”功能是否仅限创造模式使用。

`enablePlannerUiCache` 控制蓝图界面缓存是否自动写入。关闭后，如果缓存文件不存在，模组仍会自动生成一次缓存；后续更新需要手动执行 `/ticpa cache refresh`。
