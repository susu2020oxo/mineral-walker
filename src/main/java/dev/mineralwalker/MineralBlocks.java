package dev.mineralwalker;

import java.util.IdentityHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Makes the vanilla mineral block items wearable in the leggings slot.
 * The item itself remains the original Minecraft block item; no leggings item is registered.
 */
public final class MineralBlocks {
    private static final Map<Item, Block> MINERAL_ITEMS = new IdentityHashMap<>();
    private static final Map<Block, String> DISPLAY_NAMES = new IdentityHashMap<>();

    static {
        add(Blocks.COAL_BLOCK, "coal_block", "煤炭块");
        add(Blocks.IRON_BLOCK, "iron_block", "铁块");
        add(Blocks.GOLD_BLOCK, "gold_block", "金块");
        add(Blocks.DIAMOND_BLOCK, "diamond_block", "钻石块");
        add(Blocks.EMERALD_BLOCK, "emerald_block", "绿宝石块");
        add(Blocks.LAPIS_BLOCK, "lapis_block", "青金石块");
        add(Blocks.REDSTONE_BLOCK, "redstone_block", "红石块");
        add(Blocks.COPPER_BLOCK.weathering().unaffected(), "copper_block", "铜块");
        add(Blocks.NETHERITE_BLOCK, "netherite_block", "下界合金块");
        add(Blocks.AMETHYST_BLOCK, "amethyst_block", "紫水晶块");
        add(Blocks.QUARTZ_BLOCK, "quartz_block", "石英块");
        add(Blocks.RAW_IRON_BLOCK, "raw_iron_block", "粗铁块");
        add(Blocks.RAW_GOLD_BLOCK, "raw_gold_block", "粗金块");
        add(Blocks.RAW_COPPER_BLOCK, "raw_copper_block", "粗铜块");
    }

    private MineralBlocks() {
    }

    private static void add(Block block, String assetName, String displayName) {
        MINERAL_ITEMS.put(block.asItem(), block);
        DISPLAY_NAMES.put(block, displayName);
    }

    private static ResourceKey<EquipmentAsset> equipmentAsset(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MineralWalker.MOD_ID, name);
        return ResourceKey.create(EquipmentAssets.ROOT_ID, id);
    }

    public static void initialize() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            MINERAL_ITEMS.forEach((item, block) -> {
                String path = BuiltinBlockNames.assetName(block);
                Equippable equippable = Equippable.builder(EquipmentSlot.LEGS)
                        .setAsset(equipmentAsset(path))
                        .setSwappable(true)
                        .setDamageOnHurt(false)
                        .build();

                context.modify(item, builder -> builder.set(DataComponents.EQUIPPABLE, equippable));
            });
        });

        MineralWalker.LOGGER.info("Attached leggings-slot equipment to {} original vanilla mineral block items.",
                MINERAL_ITEMS.size());
    }

    public static Block blockFor(Item item) {
        return MINERAL_ITEMS.get(item);
    }

    public static String displayName(Block block) {
        return DISPLAY_NAMES.getOrDefault(block, "矿物块");
    }

    private static final class BuiltinBlockNames {
        private BuiltinBlockNames() {
        }

        private static String assetName(Block block) {
            if (block == Blocks.COAL_BLOCK) return "coal_block";
            if (block == Blocks.IRON_BLOCK) return "iron_block";
            if (block == Blocks.GOLD_BLOCK) return "gold_block";
            if (block == Blocks.DIAMOND_BLOCK) return "diamond_block";
            if (block == Blocks.EMERALD_BLOCK) return "emerald_block";
            if (block == Blocks.LAPIS_BLOCK) return "lapis_block";
            if (block == Blocks.REDSTONE_BLOCK) return "redstone_block";
            if (block == Blocks.COPPER_BLOCK.weathering().unaffected()) return "copper_block";
            if (block == Blocks.NETHERITE_BLOCK) return "netherite_block";
            if (block == Blocks.AMETHYST_BLOCK) return "amethyst_block";
            if (block == Blocks.QUARTZ_BLOCK) return "quartz_block";
            if (block == Blocks.RAW_IRON_BLOCK) return "raw_iron_block";
            if (block == Blocks.RAW_GOLD_BLOCK) return "raw_gold_block";
            if (block == Blocks.RAW_COPPER_BLOCK) return "raw_copper_block";
            throw new IllegalArgumentException("Unsupported mineral block: " + block);
        }
    }
}
