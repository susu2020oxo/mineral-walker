package dev.mineralwalker;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;

/** Drops the weapon's matching mineral item on every successful hit and on fatal hits. */
public final class MineralCombat {
    private MineralCombat() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MineralCombat::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(MineralCombat::afterDeath);
    }

    private static void afterDamage(LivingEntity victim, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) return;
        dropMatchingMineral(victim, source);
    }

    private static void afterDeath(LivingEntity victim, DamageSource source) {
        dropMatchingMineral(victim, source);
    }

    private static void dropMatchingMineral(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        ItemStack weaponStack = attacker.getMainHandItem();
        MineralGear.MineralSpec spec = MineralGear.specForWeapon(weaponStack.getItem());
        if (spec == null) return;
        if (!(victim.level() instanceof ServerLevel level)) return;

        int count = 1 + attacker.getRandom().nextInt(2) + spec.extraDrops();
        ItemStack mineralDrop = new ItemStack(spec.dropItem(), count);
        ItemEntity itemEntity = new ItemEntity(
                level,
                victim.getX(),
                victim.getY() + Math.max(0.3D, victim.getBbHeight() * 0.5D),
                victim.getZ(),
                mineralDrop
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
