package net.rai.scp_unleashed.scp409;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicInteger;

public final class Scp409Shatter {
    public static final DustParticleOptions Dust = new DustParticleOptions(new Vector3f(0.42f, 0.92f, 0.95f), 0.9f);
    private static final AtomicInteger Count = new AtomicInteger();

    private Scp409Shatter() {
    }

    public static boolean shouldShatter(LivingEntity Dead, DamageSource Source) {
        return Scp409Infection.isInfected(Dead) || Source.is(Scp409Registry.OvergrownDamage);
    }

    public static int count() {
        return Count.get();
    }

    public static void shatter(LivingEntity Dead) {
        if (!(Dead.level() instanceof ServerLevel World)) {
            return;
        }
        Count.incrementAndGet();
        float Size = Mth.clamp(Dead.getBbWidth() * Dead.getBbHeight() / Scp409Infection.PlayerBody, 0.3f, 3.0f);
        double Middle = Dead.getY() + Dead.getBbHeight() * 0.5;
        double Wide = Dead.getBbWidth() * 0.45;
        double Tall = Dead.getBbHeight() * 0.4;
        World.sendParticles(Scp409Registry.ShardParticle.get(), Dead.getX(), Middle, Dead.getZ(), Math.round(28 + 26 * Math.min(Size, 2.0f)), Wide, Tall, Wide, 0.16);
        World.sendParticles(Dust, Dead.getX(), Middle, Dead.getZ(), Math.round(14 + 12 * Math.min(Size, 2.0f)), Wide, Tall, Wide, 0.05);
        Scp409Infection.play(World, Dead, Scp409Registry.ShatterLayers, 1.0f, 1.0f);
        Scp409Sync.shattered(Dead);
        Dead.removeData(Scp409Registry.Crystal.get());
        Scp409Infection.clearModifiers(Dead);
    }
}
