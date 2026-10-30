package dev.mineralwalker;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Drops the weapon's matching mineral material on every successful hit. */
public final class MineralCombat {
    private MineralCombat() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MineralCombat::afterDamage);
    }

    private static void afterDamage(LivingEntity victim, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) return;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;

        MineralGear.MineralSpec spec = MineralGear.specForWeapon(attacker.getMainHandItem().getItem());
        if (spec == null || !(victim.level() instanceof ServerLevel level)) return;

        int count = 1 + attacker.getRandom().nextInt(2) + spec.extraDrops();
        ItemEntity drop = new ItemEntity(
                level,
                victim.getX(),
                victim.getY() + Math.max(0.3D, victim.getBbHeight() * 0.5D),
                victim.getZ(),
                new ItemStack(spec.dropItem(), count)
        );
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
