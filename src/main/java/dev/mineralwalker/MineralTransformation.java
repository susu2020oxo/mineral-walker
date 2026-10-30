package dev.mineralwalker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/**
 * Full mineral avatar: requires the same original mineral block in all four armor slots.
 */
public final class MineralTransformation {
    private static final Map<UUID, Block> ACTIVE_FORMS = new HashMap<>();

    private MineralTransformation() {
    }

    public static void handlePlayer(ServerPlayer player, ServerLevel level) {
        Block currentForm = MineralBlocks.uniformMineral(player);
        Block previousForm = ACTIVE_FORMS.get(player.getUUID());

        if (currentForm != previousForm) {
            if (currentForm == null) {
                ACTIVE_FORMS.remove(player.getUUID());
                if (previousForm != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "[矿物行走者] 矿物化身已解除。"));
                }
            } else {
                ACTIVE_FORMS.put(player.getUUID(), currentForm);
                String name = MineralBlocks.displayName(currentForm);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[矿物行走者] 完整化身启动：" + name
                                + "！10×10矿物领域、触碰生物转化、同材质生命值和强化武器已激活。"));
                MineralGear.boostWeaponForBlock(player, currentForm);
            }
        }

        if (currentForm != null && !player.isSpectator()) {
            convertTouchingCreatures(player, level, currentForm);
        }
    }

    private static void convertTouchingCreatures(ServerPlayer player, ServerLevel level, Block mineral) {
        // Only entities actually touching the player's collision box are affected.
        AABB touchArea = player.getBoundingBox().inflate(0.12D);
        List<LivingEntity> touching = level.getEntitiesOfClass(
                LivingEntity.class,
                touchArea,
                entity -> entity != player
                        && !(entity instanceof ServerPlayer)
                        && entity.isAlive()
                        && !entity.isSpectator()
        );

        for (LivingEntity creature : touching) {
            BlockPos origin = creature.blockPosition();
            creature.kill(level);
            placeTwoMineralBlocks(level, origin, mineral);
        }
    }

    private static void placeTwoMineralBlocks(ServerLevel level, BlockPos origin, Block mineral) {
        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] offset : offsets) {
            BlockPos bottom = origin.offset(offset[0], 0, offset[1]);
            BlockPos top = bottom.above();
            if (level.getBlockState(bottom).isAir() && level.getBlockState(top).isAir()) {
                level.setBlockAndUpdate(bottom, mineral.defaultBlockState());
                level.setBlockAndUpdate(top, mineral.defaultBlockState());
                return;
            }
        }

        // If the location is obstructed, still show the two-block conversion as drops.
        for (int i = 0; i < 2; i++) {
            ItemEntity drop = new ItemEntity(
                    level,
                    origin.getX() + 0.5D,
                    origin.getY() + 0.35D + i * 0.18D,
                    origin.getZ() + 0.5D,
                    new ItemStack(mineral.asItem())
            );
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }
}
