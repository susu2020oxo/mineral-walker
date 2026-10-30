package dev.mineralwalker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Converts common natural terrain beneath a moving player into their equipped mineral block. */
public final class MineralTrail {
    private static final Map<UUID, BlockPos> LAST_STEPPED_BLOCK = new HashMap<>();
    private static final double MIN_HORIZONTAL_SPEED_SQUARED = 0.0004D;

    // A whitelist prevents the trail from replacing player-built floors, containers, and structures.
    private static final Set<Block> NATURAL_GROUND = Set.of(
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD, Blocks.CLAY,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL,
            Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
            Blocks.NETHERRACK, Blocks.END_STONE, Blocks.SOUL_SAND, Blocks.SOUL_SOIL,
            Blocks.SNOW_BLOCK, Blocks.MOSS_BLOCK
    );

    private MineralTrail() {
    }

    public static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            Block targetBlock = MineralArmorItems.trailBlockFor(chest.getItem());

            if (targetBlock == null || player.isSpectator() || !player.onGround()
                    || player.getDeltaMovement().horizontalDistanceSqr() < MIN_HORIZONTAL_SPEED_SQUARED) {
                LAST_STEPPED_BLOCK.remove(playerId);
                continue;
            }

            BlockPos underPlayer = player.getOnPos();
            BlockPos previous = LAST_STEPPED_BLOCK.put(playerId, underPlayer);
            if (underPlayer.equals(previous)) {
                continue;
            }

            BlockState current = level.getBlockState(underPlayer);
            if (current.isAir()
                    || !current.getFluidState().isEmpty()
                    || current.hasBlockEntity()
                    || level.getBlockEntity(underPlayer) != null
                    || current.getDestroySpeed(level, underPlayer) < 0.0F
                    || !current.isFaceSturdy(level, underPlayer, Direction.UP)
                    || !NATURAL_GROUND.contains(current.getBlock())
                    || current.is(targetBlock)) {
                continue;
            }

            level.setBlock(underPlayer, targetBlock.defaultBlockState(), 3);
        }
    }
}
