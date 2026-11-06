package net.rai.scp_unleashed.scp207;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class Scp207Burst {
    public static final float Power = 6.0f;
    public static final int FizzEvery = 10;
    public static final float FirstPitch = 0.75f;
    public static final float LastPitch = 1.7f;
    public static final Vector3f FoamColour = new Vector3f(0.42f, 0.22f, 0.52f);
    private static final Logger Log = LogUtils.getLogger();

    private Scp207Burst() {
    }

    public static DamageSource source(ServerLevel World) {
        return World.damageSources().source(Scp207Registry.BurstDamage);
    }

    public static void light(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        Scp207Registry.playLayers(World, User.getEyePosition(), Scp207Registry.BurstLayers, SoundSource.PLAYERS, 1.0f, FirstPitch);
        foam(World, User, 12);
    }

    public static void fizz(ServerPlayer User, int Left) {
        ServerLevel World = User.serverLevel();
        float Share = 1.0f - Left / (float) Scp207Toll.FuseTicks;
        foam(World, User, 2 + Math.round(Share * 4.0f));
        if (Left % FizzEvery == 0) {
            float Pitch = FirstPitch + (LastPitch - FirstPitch) * Share;
            Scp207Registry.playLayers(World, User.getEyePosition(), Scp207Registry.BurstLayers, SoundSource.PLAYERS, 0.8f + 0.4f * Share, Pitch);
        }
    }

    private static void foam(ServerLevel World, ServerPlayer User, int Count) {
        Vec3 Look = User.getLookAngle();
        double X = User.getX() + Look.x * 0.3;
        double Y = User.getEyeY() - 0.25;
        double Z = User.getZ() + Look.z * 0.3;
        World.sendParticles(ParticleTypes.BUBBLE_POP, X, Y, Z, Count, 0.18, 0.12, 0.18, 0.04);
        World.sendParticles(new DustParticleOptions(FoamColour, 0.9f), User.getX(), User.getY() + User.getBbHeight() * 0.5, User.getZ(), Count, 0.3, 0.45, 0.3, 0.0);
    }

    public static void detonate(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        Vec3 At = User.position().add(0.0, User.getBbHeight() * 0.4, 0.0);
        Log.info("SCP-207: {} set off a burst at {} {} {}", User.getGameProfile().getName(), Math.round(At.x), Math.round(At.y), Math.round(At.z));
        World.explode(null, source(World), null, At.x, At.y, At.z, Power, false, Level.ExplosionInteraction.MOB);
    }
}
