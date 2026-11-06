package net.rai.scp_unleashed.scp330;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp330Pop {
    public static final float Power = 4.0f;
    public static final double Lift = 0.0625;
    public static final float PopVolume = 1.2f;

    private Scp330Pop() {
    }

    @Nullable
    public static Explosion pop(LivingEntity Eater) {
        if (!(Eater.level() instanceof ServerLevel World)) {
            return null;
        }
        Holder<DamageType> Type = World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp330Registry.PopDamage);
        Vec3 At = new Vec3(Eater.getX(), Eater.getY() + Lift, Eater.getZ());
        Scp330Taking.play(World, At.add(0.0, Eater.getBbHeight() * 0.5, 0.0), Scp330Registry.PopLayers, PopVolume, 1.0f);
        return World.explode(null, new Scp330PopSource(Type, Eater), null, At.x, At.y, At.z, Power, false, Level.ExplosionInteraction.TNT);
    }
}
