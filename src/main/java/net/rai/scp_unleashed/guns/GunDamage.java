package net.rai.scp_unleashed.guns;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public final class GunDamage {
    private static boolean Pushing;
    private static float PushStrength;
    private static Vec3 PushDirection = Vec3.ZERO;

    private GunDamage() {
    }

    public static DamageSource sourceFor(ServerLevel World, Entity User, GunKind Kind) {
        ResourceKey<DamageType> Key = Kind.getStats().isPiercing() ? GunsRegistry.DisruptionDamage : GunsRegistry.GunshotDamage;
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Key), null, User, User.getEyePosition());
    }

    public static boolean hurt(ServerLevel World, Entity User, Entity Target, GunKind Kind, float Amount, Vec3 Heading) {
        DamageSource Source = sourceFor(World, User, Kind);
        Target.invulnerableTime = 0;
        Pushing = true;
        PushStrength = Kind.getStats().getKnockback();
        PushDirection = Heading;
        try {
            return Target.hurt(Source, Amount);
        } finally {
            Pushing = false;
        }
    }

    public static void adjustKnockback(LivingKnockBackEvent Event) {
        if (!Pushing) {
            return;
        }
        Event.setStrength(PushStrength);
        if (PushDirection.horizontalDistanceSqr() > 1.0E-6) {
            Event.setRatioX(-PushDirection.x);
            Event.setRatioZ(-PushDirection.z);
        }
    }
}
