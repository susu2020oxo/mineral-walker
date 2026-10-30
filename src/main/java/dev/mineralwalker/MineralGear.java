package dev.mineralwalker;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

/** Registers mineral leggings and their matching swords. */
public final class MineralGear {
    private static final TagKey<Item> REPAIR_ITEMS =
            TagKey.create(BuiltInRegistries.ITEM.key(), id("repairs_mineral_gear"));
    private static final TagKey<Block> INCORRECT_SWORD_BLOCKS =
            TagKey.create(Registries.BLOCK, id("incorrect_for_mineral_sword"));

    private static final List<MineralSpec> SPECS = List.of(
            new MineralSpec("coal_block", "煤炭", 0x454545, net.minecraft.world.item.Items.COAL, 90, 1.0F, 8, 1, 1, 0, 0, 0),
            new MineralSpec("raw_copper_block", "粗铜", 0xA65F42, net.minecraft.world.item.Items.RAW_COPPER, 150, 1.5F, 10, 1, 1, 0, 0, 0),
            new MineralSpec("copper_block", "铜", 0xC87548, net.minecraft.world.item.Items.COPPER_INGOT, 180, 2.0F, 12, 2, 1, 0, 0, 0),
            new MineralSpec("quartz_block", "石英", 0xE8E3D8, net.minecraft.world.item.Items.QUARTZ, 190, 2.4F, 14, 2, 1, 0, 0, 0),
            new MineralSpec("amethyst_block", "紫水晶", 0xA56BCE, net.minecraft.world.item.Items.AMETHYST_SHARD, 220, 2.8F, 16, 2, 2, 1, 0, 0),
            new MineralSpec("raw_iron_block", "粗铁", 0x9A795C, net.minecraft.world.item.Items.RAW_IRON, 220, 3.0F, 12, 2, 1, 0, 0, 0),
            new MineralSpec("lapis_block", "青金石", 0x254C9B, net.minecraft.world.item.Items.LAPIS_LAZULI, 240, 3.2F, 18, 3, 2, 1, 0, 0),
            new MineralSpec("redstone_block", "红石", 0xA52B25, net.minecraft.world.item.Items.REDSTONE, 250, 3.4F, 18, 3, 1, 0, 1, 0),
            new MineralSpec("iron_block", "铁", 0xB8B8B8, net.minecraft.world.item.Items.IRON_INGOT, 250, 3.6F, 16, 3, 1, 1, 0, 0),
            new MineralSpec("raw_gold_block", "粗金", 0xC49B28, net.minecraft.world.item.Items.RAW_GOLD, 180, 3.2F, 16, 2, 1, 0, 1, 0),
            new MineralSpec("gold_block", "黄金", 0xF4C52E, net.minecraft.world.item.Items.GOLD_INGOT, 160, 3.8F, 22, 3, 2, 1, 1, 0),
            new MineralSpec("diamond_block", "钻石", 0x55E6D0, net.minecraft.world.item.Items.DIAMOND, 1561, 5.0F, 18, 4, 2, 2, 0, 0),
            new MineralSpec("emerald_block", "绿宝石", 0x16B85B, net.minecraft.world.item.Items.EMERALD, 1800, 5.8F, 22, 4, 3, 2, 0, 0),
            new MineralSpec("netherite_block", "下界合金", 0x51413F, net.minecraft.world.item.Items.NETHERITE_INGOT, 2031, 7.0F, 25, 5, 3, 3, 2, 0)
    );

    private static final Map<Item, MineralSpec> LEGGINGS = new IdentityHashMap<>();
    private static final Map<Item, MineralSpec> WEAPONS = new IdentityHashMap<>();
    private static final List<Item> CREATIVE_ITEMS = new ArrayList<>();
    private static final Map<UUID, Item> ACTIVE_LEGGINGS = new java.util.HashMap<>();

    private MineralGear() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MineralWalker.MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    private static ResourceKey<EquipmentAsset> equipmentAsset(String path) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, id(path));
    }

    public static void initialize() {
        for (MineralSpec spec : SPECS) {
            registerMineralGear(spec);
        }

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(entries -> CREATIVE_ITEMS.forEach(entries::accept));

        MineralWalker.LOGGER.info("Registered {} mineral leggings and {} matching swords.",
                LEGGINGS.size(), WEAPONS.size());
    }

    private static void registerMineralGear(MineralSpec spec) {
        ResourceKey<EquipmentAsset> assetKey = equipmentAsset(spec.id());
        ArmorMaterial armorMaterial = new ArmorMaterial(
                spec.durability(),
                Map.of(ArmorType.LEGGINGS, 6),
                spec.enchantability(),
                SoundEvents.ARMOR_EQUIP_IRON,
                1.0F,
                0.05F,
                REPAIR_ITEMS,
                assetKey
        );

        String leggingsPath = spec.id() + "_leggings";
        ResourceKey<Item> leggingsKey = itemKey(leggingsPath);
        Item leggings = Registry.register(
                BuiltInRegistries.ITEM,
                leggingsKey,
                new Item(new Item.Properties()
                        .humanoidArmor(armorMaterial, ArmorType.LEGGINGS)
                        .durability(ArmorType.LEGGINGS.getDurability(spec.durability()))
                        .setId(leggingsKey))
        );
        LEGGINGS.put(leggings, spec);
        CREATIVE_ITEMS.add(leggings);

        ToolMaterial toolMaterial = new ToolMaterial(
                INCORRECT_SWORD_BLOCKS,
                spec.durability(),
                6.0F,
                spec.damageBonus(),
                spec.enchantability(),
                REPAIR_ITEMS
        );

        String swordPath = spec.id() + "_sword";
        ResourceKey<Item> swordKey = itemKey(swordPath);
        Item sword = Registry.register(
                BuiltInRegistries.ITEM,
                swordKey,
                new Item(new Item.Properties()
                        .sword(toolMaterial, 0.0F, -2.4F)
                        .setId(swordKey))
        );
        WEAPONS.put(sword, spec);
        CREATIVE_ITEMS.add(sword);
    }

    public static void handlePlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Item currentLeggings = player.getItemBySlot(EquipmentSlot.LEGS).getItem();
        MineralSpec currentSpec = LEGGINGS.get(currentLeggings);
        Item currentItem = currentSpec == null ? null : currentLeggings;
        Item previousItem = ACTIVE_LEGGINGS.get(playerId);

        if (Objects.equals(previousItem, currentItem)) {
            return;
        }

        if (previousItem != null) {
            MineralSpec previous = LEGGINGS.get(previousItem);
            if (previous != null) {
                player.sendSystemMessage(Component.literal("[矿物行走者] 已脱下" + previous.name()
                        + "护腿；已获得的专属武器会保留在背包中。"));
            }
        }

        if (currentItem == null) {
            ACTIVE_LEGGINGS.remove(playerId);
            return;
        }

        ACTIVE_LEGGINGS.put(playerId, currentItem);
        Item weapon = weaponForLeggings(currentItem);
        player.sendSystemMessage(Component.literal("[矿物行走者] 已激活：" + currentSpec.name()
                + "护腿效果，获得专属武器：" + currentSpec.name() + "剑"));

        if (!hasItem(player, weapon)) {
            ItemStack weaponStack = createEnchantedWeapon((ServerLevel) player.level(), currentSpec, weapon);
            if (!player.getInventory().add(weaponStack)) {
                player.drop(weaponStack, false);
            }
        }
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return true;
            }
        }
        return player.getOffhandItem().is(item);
    }

    private static ItemStack createEnchantedWeapon(ServerLevel level, MineralSpec spec, Item weapon) {
        ItemStack stack = new ItemStack(weapon);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), spec.sharpness());
        if (spec.looting() > 0) {
            stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), spec.looting());
        }
        if (spec.unbreaking() > 0) {
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), spec.unbreaking());
        }
        if (spec.fireAspect() > 0) {
            stack.enchant(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), spec.fireAspect());
        }
        return stack;
    }

    public static Item weaponForLeggings(Item leggings) {
        MineralSpec spec = LEGGINGS.get(leggings);
        if (spec == null) return null;
        for (Map.Entry<Item, MineralSpec> entry : WEAPONS.entrySet()) {
            if (entry.getValue() == spec) return entry.getKey();
        }
        return null;
    }

    public static MineralSpec specForWeapon(Item weapon) {
        return WEAPONS.get(weapon);
    }

    public record MineralSpec(
            String id,
            String name,
            int color,
            Item dropItem,
            int durability,
            float damageBonus,
            int enchantability,
            int sharpness,
            int looting,
            int unbreaking,
            int fireAspect,
            int reserved
    ) {
        public int extraDrops() {
            return looting;
        }
    }
}
