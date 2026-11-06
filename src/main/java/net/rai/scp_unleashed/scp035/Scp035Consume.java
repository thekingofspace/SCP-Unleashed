package net.rai.scp_unleashed.scp035;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;

public final class Scp035Consume {
    public static final ResourceKey<DamageType> Possession = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_possession"));
    public static final String WornKey = "scp_unleashed_scp035_worn";
    public static final float StrikeDamage = 6.0f;
    public static final float DrainStart = 1.0f;
    public static final float DrainRise = 0.25f;
    public static final int DrainInterval = 20;

    private Scp035Consume() {
    }

    public static boolean isExempt(Player User) {
        return User.isCreative() || User.isSpectator();
    }

    public static boolean canConsume(LivingEntity Target) {
        if (Target instanceof Player User) {
            return !isExempt(User);
        }
        return Target instanceof Mob && !(Target instanceof Scp035Host) && !InfectionImmunity.isImmune(Target);
    }

    public static boolean isConsumed(LivingEntity Target) {
        return canConsume(Target) && Scp035Sight.wearsMask(Target);
    }

    public static void tick(LivingEntity Wearer) {
        boolean Masked = Scp035Sight.wearsMask(Wearer);
        if (Wearer instanceof Player User) {
            adoptLegacy(User, Masked);
        }
        if (!Masked) {
            if (Wearer.hasData(Scp035Registry.Worn.get())) {
                Wearer.removeData(Scp035Registry.Worn.get());
            }
            return;
        }
        if (!canConsume(Wearer) || !Wearer.isAlive() || !Targeting.isTargetable(Wearer)) {
            return;
        }
        Integer Held = Wearer.getExistingDataOrNull(Scp035Registry.Worn.get());
        if (Held == null) {
            Wearer.setData(Scp035Registry.Worn.get(), 0);
            strike(Wearer);
            return;
        }
        int Worn = Held + 1;
        Wearer.setData(Scp035Registry.Worn.get(), Worn);
        if (Worn % DrainInterval == 0) {
            drain(Wearer, DrainStart + DrainRise * (Worn / DrainInterval - 1));
        }
    }

    private static void adoptLegacy(Player User, boolean Masked) {
        CompoundTag Data = User.getPersistentData();
        if (!Data.contains(WornKey)) {
            return;
        }
        if (Masked && !User.hasData(Scp035Registry.Worn.get())) {
            User.setData(Scp035Registry.Worn.get(), Data.getInt(WornKey));
        }
        Data.remove(WornKey);
    }

    private static void strike(LivingEntity Wearer) {
        if (!(Wearer.level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Head = Wearer.getEyePosition();
        World.playSound(null, Head.x, Head.y, Head.z, Scp035Registry.Strike.get(), Wearer.getSoundSource(), 1.0f, 0.9f + World.random.nextFloat() * 0.1f);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Scp035MaskItem.fresh()), Head.x, Head.y, Head.z, 16, 0.2, 0.2, 0.2, 0.09);
        World.sendParticles(ParticleTypes.DAMAGE_INDICATOR, Head.x, Head.y - 0.2, Head.z, 6, 0.25, 0.2, 0.25, 0.1);
        Wearer.hurt(source(Wearer), StrikeDamage);
    }

    private static void drain(LivingEntity Wearer, float Amount) {
        if (!(Wearer.level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Head = Wearer.getEyePosition();
        World.sendParticles(Scp035Registry.Weep.get(), Head.x, Head.y - 0.05, Head.z, 3, 0.12, 0.05, 0.12, 0.0);
        Wearer.hurt(source(Wearer), Amount);
    }

    private static DamageSource source(LivingEntity Wearer) {
        return Wearer.damageSources().source(Possession);
    }
}
