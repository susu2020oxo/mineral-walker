package dev.mineralwalker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base mineral walking uses one block underfoot. A complete four-piece matching
 * set expands the effect to a 10x10 area around the player's feet.
 */
public final class MineralTrail {
    private static final Map<UUID, BlockPos> LAST_FOOT_BLOCK = new HashMap<>();
    private static final Map<UUID, Block> ACTIVE_MINERAL = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_FULL_SET = new HashMap<>();

    private MineralTrail() {
    }

    public static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            MineralGear.handlePlayer(player);
            MineralTransformation.handlePlayer(player, level);

            UUID playerId = player.getUUID();
            Block fullSetMineral = MineralBlocks.uniformMineral(player);
            boolean fullSet = fullSetMineral != null;
            Boolean wasFullSet = LAST_FULL_SET.put(playerId, fullSet);
            if (wasFullSet == null || wasFullSet != fullSet) {
                // Re-run the footprint immediately when the avatar mode changes.
                LAST_FOOT_BLOCK.remove(playerId);
            }

            Block mineral = fullSetMineral != null
                    ? fullSetMineral
                    : MineralBlocks.firstEquippedMineral(player);

            Block previousMineral = ACTIVE_MINERAL.get(playerId);
            if (!Objects.equals(previousMineral, mineral)) {
                if (previousMineral != null) {
                    sendNotice(player, MineralBlocks.displayName(previousMineral) + "行走效果已关闭");
                }
                if (mineral != null) {
                    sendNotice(player, "已激活：" + MineralBlocks.displayName(mineral)
                            + (fullSet ? "10×10矿物领域" : "矿物行走效果"));
                    ACTIVE_MINERAL.put(playerId, mineral);
                    LAST_FOOT_BLOCK.remove(playerId);
                } else {
                    ACTIVE_MINERAL.remove(playerId);
                    LAST_FOOT_BLOCK.remove(playerId);
                }
            }

            if (mineral == null || player.isSpectator() || !player.onGround()) {
                continue;
            }

            BlockPos underFoot = footBlock(player);
            BlockPos previousPos = LAST_FOOT_BLOCK.put(playerId, underFoot);
            if (underFoot.equals(previousPos)) {
                continue;
            }

            if (fullSet) {
                // A 10x10 square, centered on the player's current foot block.
                for (int dx = -5; dx < 5; dx++) {
                    for (int dz = -5; dz < 5; dz++) {
                        replaceGroundBlock(level, underFoot.offset(dx, 0, dz), mineral);
                    }
                }
            } else {
                replaceGroundBlock(level, underFoot, mineral);
            }
        }
    }

    private static void replaceGroundBlock(ServerLevel level, BlockPos pos, Block mineral) {
        BlockState ground = level.getBlockState(pos);
        if (ground.isAir()
                || !ground.getFluidState().isEmpty()
                || ground.hasBlockEntity()
                || level.getBlockEntity(pos) != null
                || ground.getDestroySpeed(level, pos) < 0.0F
                || !ground.isFaceSturdy(level, pos, Direction.UP)
                || ground.is(mineral)) {
            return;
        }

        // Never overwrite containers, fluids, air or unbreakable blocks.
        level.setBlock(pos, mineral.defaultBlockState(), 3);
    }

    private static BlockPos footBlock(ServerPlayer player) {
        return BlockPos.containing(player.getX(), player.getY() - 0.01D, player.getZ());
    }

    private static void sendNotice(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[矿物行走者] " + message));
    }
}
