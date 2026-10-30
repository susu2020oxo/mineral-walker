package dev.mineralwalker;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Original Minecraft mineral block items can be equipped in all four armor slots.
 * No custom mineral armor item is registered; only per-stack Equippable components
 * are used to select the intended slot.
 */
public final class MineralBlocks {
    private static final Map<Item, Block> MINERAL_ITEMS = new IdentityHashMap<>();
    private static final Map<Block, String> DISPLAY_NAMES = new IdentityHashMap<>();
    private static final Map<Block, Integer> COLORS = new IdentityHashMap<>();
    private static final Map<Item, Map<EquipmentSlot, Equippable>> EQUIPPABLES = new IdentityHashMap<>();

    static {
        add(Blocks.COAL_BLOCK, "coal_block", "煤炭块", 0x454545);
        add(Blocks.IRON_BLOCK, "iron_block", "铁块", 0xB8B8B8);
        add(Blocks.GOLD_BLOCK, "gold_block", "金块", 0xF4C52E);
        add(Blocks.DIAMOND_BLOCK, "diamond_block", "钻石块", 0x55E6D0);
        add(Blocks.EMERALD_BLOCK, "emerald_block", "绿宝石块", 0x16B85B);
        add(Blocks.LAPIS_BLOCK, "lapis_block", "青金石块", 0x254C9B);
        add(Blocks.REDSTONE_BLOCK, "redstone_block", "红石块", 0xA52B25);
        add(Blocks.COPPER_BLOCK.weathering().unaffected(), "copper_block", "铜块", 0xC87548);
        add(Blocks.NETHERITE_BLOCK, "netherite_block", "下界合金块", 0x51413F);
        add(Blocks.AMETHYST_BLOCK, "amethyst_block", "紫水晶块", 0xA56BCE);
        add(Blocks.QUARTZ_BLOCK, "quartz_block", "石英块", 0xE8E3D8);
        add(Blocks.RAW_IRON_BLOCK, "raw_iron_block", "粗铁块", 0x9A795C);
        add(Blocks.RAW_GOLD_BLOCK, "raw_gold_block", "粗金块", 0xC49B28);
        add(Blocks.RAW_COPPER_BLOCK, "raw_copper_block", "粗铜块", 0xA65F42);
    }

    private MineralBlocks() {
    }

    private static void add(Block block, String assetName, String displayName, int color) {
        Item item = block.asItem();
        MINERAL_ITEMS.put(item, block);
        DISPLAY_NAMES.put(block, displayName);
        COLORS.put(block, color);

        EnumMap<EquipmentSlot, Equippable> bySlot = new EnumMap<>(EquipmentSlot.class);
        bySlot.put(EquipmentSlot.HEAD, createEquippable(assetName, EquipmentSlot.HEAD));
        bySlot.put(EquipmentSlot.CHEST, createEquippable(assetName, EquipmentSlot.CHEST));
        bySlot.put(EquipmentSlot.LEGS, createEquippable(assetName, EquipmentSlot.LEGS));
        bySlot.put(EquipmentSlot.FEET, createEquippable(assetName, EquipmentSlot.FEET));
        EQUIPPABLES.put(item, bySlot);
    }

    private static ResourceKey<EquipmentAsset> equipmentAsset(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MineralWalker.MOD_ID, name);
        return ResourceKey.create(EquipmentAssets.ROOT_ID, id);
    }

    private static Equippable createEquippable(String assetName, EquipmentSlot slot) {
        return Equippable.builder(slot)
                .setAsset(equipmentAsset(assetName))
                .setSwappable(true)
                .setDamageOnHurt(false)
                .build();
    }

    public static void initialize() {
        // First equip goes to chest. Server ticks retarget the remaining stacks
        // to the next free armor slot, allowing four separate original block stacks.
        DefaultItemComponentEvents.MODIFY.register(context -> {
            MINERAL_ITEMS.forEach((item, block) -> context.modify(item,
                    builder -> builder.set(DataComponents.EQUIPPABLE,
                            equippableFor(item, EquipmentSlot.CHEST))));
        });

        MineralWalker.LOGGER.info("Enabled {} original vanilla mineral blocks in head, chest, legs and feet slots.",
                MINERAL_ITEMS.size());
    }

    public static Equippable equippableFor(Item item, EquipmentSlot slot) {
        Map<EquipmentSlot, Equippable> bySlot = EQUIPPABLES.get(item);
        return bySlot == null ? null : bySlot.get(slot);
    }

    public static void updateInventoryEquipmentSlots(Player player) {
        // Repair the component on items that are ALREADY equipped first.
        // Inventory#getContainerSize also includes armor and offhand compartments in
        // this version. Reassigning those stacks to the next empty slot made a worn
        // leggings block claim to be a chest item, so Minecraft stopped rendering its
        // humanoid_leggings equipment layer. Keep every worn stack bound to its real slot.
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (equipped.isEmpty() || blockFor(equipped.getItem()) == null) {
                continue;
            }

            Equippable correct = equippableFor(equipped.getItem(), slot);
            if (correct != null && !Objects.equals(equipped.get(DataComponents.EQUIPPABLE), correct)) {
                equipped.set(DataComponents.EQUIPPABLE, correct);
            }
        }

        // Only retarget ordinary main-inventory stacks. Armor slots (and offhand)
        // must never be overwritten by the next-free-slot calculation.
        EquipmentSlot desiredSlot = nextEmptyMineralArmorSlot(player);
        int mainInventorySize = Math.min(36, player.getInventory().getContainerSize());
        for (int i = 0; i < mainInventorySize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || blockFor(stack.getItem()) == null) {
                continue;
            }

            Equippable desired = equippableFor(stack.getItem(), desiredSlot);
            Equippable current = stack.get(DataComponents.EQUIPPABLE);
            if (desired != null && !Objects.equals(current, desired)) {
                stack.set(DataComponents.EQUIPPABLE, desired);
            }
        }
    }

    private static EquipmentSlot nextEmptyMineralArmorSlot(Player player) {
        // Preserve the familiar chest-first, leggings-second flow, then fill helmet and boots.
        EquipmentSlot[] order = {
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : order) {
            if (blockFor(player.getItemBySlot(slot).getItem()) == null) {
                return slot;
            }
        }
        return EquipmentSlot.CHEST;
    }

    public static Block uniformMineral(Player player) {
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        Block expected = null;
        for (EquipmentSlot slot : slots) {
            Block current = blockFor(player.getItemBySlot(slot).getItem());
            if (current == null) {
                return null;
            }
            if (expected == null) {
                expected = current;
            } else if (expected != current) {
                return null;
            }
        }
        return expected;
    }

    public static Block firstEquippedMineral(Player player) {
        EquipmentSlot[] slots = {
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            Block block = blockFor(player.getItemBySlot(slot).getItem());
            if (block != null) return block;
        }
        return null;
    }

    public static Block blockFor(Item item) {
        return MINERAL_ITEMS.get(item);
    }

    public static String displayName(Block block) {
        return DISPLAY_NAMES.getOrDefault(block, "矿物块");
    }

    public static int colorFor(Block block) {
        return COLORS.getOrDefault(block, 0xFFFFFF);
    }
}
