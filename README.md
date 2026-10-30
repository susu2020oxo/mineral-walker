# 矿物行走者（Mineral Walker）

**Minecraft Java 26.2 / Fabric** 模组。

穿上对应的矿物方块胸甲后，玩家行走时脚下符合条件的自然地面会变成对应的矿物方块。脱下胸甲、停下或离开地面后停止转换。

## 内容
- 14 种矿物胸甲：煤炭、铁、金、钻石、绿宝石、青金石、红石、铜、下界合金、紫水晶、石英、粗铁、粗金、粗铜。
- 胸甲放在原版胸甲栏；在创造模式「战斗」栏获取，也可用 /give。
- 为避免误毁建筑，只转换常见自然地面；不会替换箱子等方块实体、液体、空气或不可破坏方块。
- 转换后的方块会永久留在世界中，建议先在测试世界体验并备份存档。

## 版本
- Minecraft Java 26.2
- Fabric Loader 0.19.3
- Fabric API 0.155.2+26.2
- Fabric Loom 1.17.13
- Java 25 / Gradle 9.5.1

## 获取胸甲
```mcfunction
/give @s mineralwalker:diamond_block_chestplate
/give @s mineralwalker:gold_block_chestplate
/give @s mineralwalker:iron_block_chestplate
/give @s mineralwalker:emerald_block_chestplate
```
其余物品 ID 格式：`mineralwalker:<矿物方块英文ID>_chestplate`。

## 下载
构建成功后，打开仓库的 **Actions → Build Mineral Walker → Artifacts**，下载 `mineral-walker-1.0.0-minecraft-26.2-fabric`，解压后把 JAR 放进游戏实例的 `mods` 文件夹。该实例还需要安装同版本 Fabric Loader 和 Fabric API。
