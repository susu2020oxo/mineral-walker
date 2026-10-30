package dev.mineralwalker;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Registers one chest-slot wearable for each supported mineral block. */
public final class MineralArmorItems {
    private static final int BASE_DURABILITY = 80;
    private static final Map<Item, Block> TRAIL_BLOCKS = new IdentityHashMap<>();
    private static final List<Item> ITEMS = new ArrayList<>();

    static {
        register("coal_block", Blocks.COAL_BLOCK);
        register("iron_block", Blocks.IRON_BLOCK);
        register("gold_block", Blocks.GOLD_BLOCK);
        register("diamond_block", Blocks.DIAMOND_BLOCK);
        register("emerald_block", Blocks.EMERALD_BLOCK);
        register("lapis_block", Blocks.LAPIS_BLOCK);
        register("redstone_block", Blocks.REDSTONE_BLOCK);
        register("copper_block", Blocks.COPPER_BLOCK);
        register("netherite_block", Blocks.NETHERITE_BLOCK);
        register("amethyst_block", Blocks.AMETHYST_BLOCK);
        register("quartz_block", Blocks.QUARTZ_BLOCK);
        register("raw_iron_block", Blocks.RAW_IRON_BLOCK);
        register("raw_gold_block", Blocks.RAW_GOLD_BLOCK);
        register("raw_copper_block", Blocks.RAW_COPPER_BLOCK);
    }

    private MineralArmorItems() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MineralWalker.MOD_ID, path);
    }

    private static void register(String mineralId, Block trailBlock) {
        String itemPath = mineralId + "_chestplate";
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(itemPath));
        ResourceKey<EquipmentAsset> assetKey = ResourceKey.create(EquipmentAssets.ROOT_ID, id(mineralId));
        TagKey<Item> repairItems = TagKey.create(BuiltInRegistries.ITEM.key(), id("repairs_mineral_armor"));

        ArmorMaterial material = new ArmorMaterial(
                BASE_DURABILITY,
                Map.of(ArmorType.CHESTPLATE, 8),
                8,
                SoundEvents.ARMOR_EQUIP_IRON,
                1.0F,
                0.05F,
                repairItems,
                assetKey
        );

        Item.Properties properties = new Item.Properties()
                .humanoidArmor(material, ArmorType.CHESTPLATE)
                .durability(ArmorType.CHESTPLATE.getDurability(BASE_DURABILITY));

        Item item = new Item(properties.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        TRAIL_BLOCKS.put(item, trailBlock);
        ITEMS.add(item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(entries -> ITEMS.forEach(entries::accept));
    }

    public static Block trailBlockFor(Item item) {
        return TRAIL_BLOCKS.get(item);
    }

    public static int itemCount() {
        return ITEMS.size();
    }
}
