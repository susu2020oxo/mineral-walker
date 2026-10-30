package dev.mineralwalker;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;

/**
 * Registers only matching mineral swords. The wearable items are the original
 * vanilla mineral block items, equipped directly in the player's leggings slot.
 */
public final class MineralGear {
    private static final TagKey<Item> REPAIR_ITEMS =
            TagKey.create(BuiltInRegistries.ITEM.key(), id("repairs_mineral_gear"));
    private static final TagKey<Block> INCORRECT_SWORD_BLOCKS =
            TagKey.create(Registries.BLOCK, id("incorrect_for_mineral_sword"));

    private static final List<MineralSpec> SPECS = List.of(
            new MineralSpec("coal_block", "煤炭", net.minecraft.world.item.Items.COAL, 90, 1.0F, 8, 1, 1, 0, 0),
            new MineralSpec("raw_copper_block", "粗铜", net.minecraft.world.item.Items.RAW_COPPER, 150, 1.5F, 10, 1, 1, 0, 0),
            new MineralSpec("copper_block", "铜", net.minecraft.world.item.Items.COPPER_INGOT, 180, 2.0F, 12, 2, 1, 0, 0),
            new MineralSpec("quartz_block", "石英", net.minecraft.world.item.Items.QUARTZ, 190, 2.4F, 14, 2, 1, 0, 0),
            new MineralSpec("amethyst_block", "紫水晶", net.minecraft.world.item.Items.AMETHYST_SHARD, 220, 2.8F, 16, 2, 2, 1, 0),
            new MineralSpec("raw_iron_block", "粗铁", net.minecraft.world.item.Items.RAW_IRON, 220, 3.0F, 12, 2, 1, 0, 0),
            new MineralSpec("lapis_block", "青金石", net.minecraft.world.item.Items.LAPIS_LAZULI, 240, 3.2F, 18, 3, 2, 1, 0),
            new MineralSpec("redstone_block", "红石", net.minecraft.world.item.Items.REDSTONE, 250, 3.4F, 18, 3, 1, 0, 1),
            new MineralSpec("iron_block", "铁", net.minecraft.world.item.Items.IRON_INGOT, 250, 3.6F, 16, 3, 1, 1, 0),
            new MineralSpec("raw_gold_block", "粗金", net.minecraft.world.item.Items.RAW_GOLD, 180, 3.2F, 16, 2, 1, 0, 1),
            new MineralSpec("gold_block", "黄金", net.minecraft.world.item.Items.GOLD_INGOT, 160, 3.8F, 22, 3, 2, 1, 1),
            new MineralSpec("diamond_block", "钻石", net.minecraft.world.item.Items.DIAMOND, 1561, 5.0F, 18, 4, 2, 2, 0),
            new MineralSpec("emerald_block", "绿宝石", net.minecraft.world.item.Items.EMERALD, 1800, 5.8F, 22, 4, 3, 2, 0),
            new MineralSpec("netherite_block", "下界合金", net.minecraft.world.item.Items.NETHERITE_INGOT, 2031, 7.0F, 25, 5, 3, 3, 2)
    );

    private static final Map<Item, MineralSpec> MINERAL_BLOCKS = new IdentityHashMap<>();
    private static final Map<Item, MineralSpec> WEAPONS = new IdentityHashMap<>();
    private static final Map<MineralSpec, Item> SWORDS_BY_SPEC = new HashMap<>();
    private static final List<Item> CREATIVE_ITEMS = new ArrayList<>();
    private static final Map<UUID, Item> ACTIVE_BLOCKS = new HashMap<>();

    private MineralGear() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MineralWalker.MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    public static void initialize() {
        for (MineralSpec spec : SPECS) {
            Item vanillaBlockItem = BuiltInRegistries.ITEM.getValue(id(spec.id()));
            MINERAL_BLOCKS.put(vanillaBlockItem, spec);
            registerMineralSword(spec);
        }

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(entries -> CREATIVE_ITEMS.forEach(entries::accept));

        MineralWalker.LOGGER.info("Registered {} mineral block mappings and {} matching mineral swords; no custom leggings items.",
                MINERAL_BLOCKS.size(), WEAPONS.size());
    }

    private static void registerMineralSword(MineralSpec spec) {
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
        SWORDS_BY_SPEC.put(spec, sword);
        CREATIVE_ITEMS.add(sword);
    }

    public static void handlePlayer(ServerPlayer player) {
        MineralBlocks.updateInventoryEquipmentSlots(player);
        UUID playerId = player.getUUID();
        Item currentBlockItem = player.getItemBySlot(EquipmentSlot.LEGS).getItem();
        Block currentBlock = MineralBlocks.blockFor(currentBlockItem);
        MineralSpec currentSpec = currentBlock == null ? null : MINERAL_BLOCKS.get(currentBlockItem);
        Item currentItem = currentSpec == null ? null : currentBlockItem;
        Item previousItem = ACTIVE_BLOCKS.get(playerId);

        if (Objects.equals(previousItem, currentItem)) {
            return;
        }

        if (previousItem != null) {
            Block previousBlock = MineralBlocks.blockFor(previousItem);
            String previousName = previousBlock == null ? "矿物块" : MineralBlocks.displayName(previousBlock);
            player.sendSystemMessage(Component.literal("[矿物行走者] 已卸下" + previousName
                    + "；已获得的专属武器会保留在背包中。"));
        }

        if (currentItem == null) {
            ACTIVE_BLOCKS.remove(playerId);
            return;
        }

        ACTIVE_BLOCKS.put(playerId, currentItem);
        Item weapon = SWORDS_BY_SPEC.get(currentSpec);
        String blockName = MineralBlocks.displayName(currentBlock);
        player.sendSystemMessage(Component.literal("[矿物行走者] 已装备" + blockName
                + "（护腿栏），获得对应专属武器：" + currentSpec.name() + "剑！"));

        if (!hasItem(player, weapon)) {
            ItemStack weaponStack = createEnchantedWeapon((ServerLevel) player.level(), currentSpec, weapon);
            if (!player.getInventory().add(weaponStack)) {
                player.drop(weaponStack, false);
            }
        }
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        if (item == null) return false;
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

    public static MineralSpec specForWeapon(Item weapon) {
        return WEAPONS.get(weapon);
    }

    public record MineralSpec(
            String id,
            String name,
            Item dropItem,
            int durability,
            float damageBonus,
            int enchantability,
            int sharpness,
            int looting,
            int unbreaking,
            int fireAspect
    ) {
        public int extraDrops() {
            return looting;
        }
    }
}
