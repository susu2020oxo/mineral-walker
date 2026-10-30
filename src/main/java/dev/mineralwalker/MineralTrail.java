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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replaces the solid block directly under a moving player with the block represented
 * by the vanilla mineral block item worn in the chest slot.
 */
public final class MineralTrail {
    private static final Map<UUID, BlockPos> LAST_FOOT_BLOCK = new HashMap<>();
    private static final Map<UUID, Block> ACTIVE_MINERAL = new HashMap<>();

    private MineralTrail() {
    }

    public static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
            Block mineral = MineralBlocks.blockFor(chestStack.getItem());

            Block previousMineral = ACTIVE_MINERAL.get(playerId);
            if (!Objects.equals(previousMineral, mineral)) {
                if (previousMineral != null) {
                    sendNotice(player, MineralBlocks.displayName(previousMineral) + "行走效果已关闭");
                }
                if (mineral != null) {
                    sendNotice(player, "已激活：" + MineralBlocks.displayName(mineral) + "行走效果");
                    ACTIVE_MINERAL.put(playerId, mineral);
                    // Do not replace the block the player is currently standing on just by equipping.
                    LAST_FOOT_BLOCK.put(playerId, footBlock(player));
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

            BlockState ground = level.getBlockState(underFoot);
            if (ground.isAir()
                    || !ground.getFluidState().isEmpty()
                    || ground.hasBlockEntity()
                    || level.getBlockEntity(underFoot) != null
                    || ground.getDestroySpeed(level, underFoot) < 0.0F
                    || !ground.isFaceSturdy(level, underFoot, Direction.UP)
                    || ground.is(mineral)) {
                continue;
            }

            // Replace solid ground blocks (including player-built floors), but never containers,
            // fluids, air, block entities, or unbreakable blocks.
            level.setBlock(underFoot, mineral.defaultBlockState(), 3);
        }
    }

    private static BlockPos footBlock(ServerPlayer player) {
        return BlockPos.containing(player.getX(), player.getY() - 0.01D, player.getZ());
    }

    private static void sendNotice(ServerPlayer player, String message) {
        Component notice = Component.literal("[矿物行走者] " + message);
        player.sendSystemMessage(notice);
    }
}
