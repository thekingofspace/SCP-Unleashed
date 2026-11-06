package net.rai.scp_unleashed.forcefield.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.forcefield.network.ForcefieldHitPayload;

public final class ForcefieldClientHooks {
    public static final int BaseSparks = 4;
    public static final int ExtraSparks = 10;
    public static final double SparkSpeed = 0.16;

    private ForcefieldClientHooks() {
    }

    public static void onHit(ForcefieldHitPayload Payload) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        Vec3 Point = Payload.point();
        float Strength = Payload.Strength();
        ForcefieldRipples.add(Point, Payload.AlongX(), Strength, World.getGameTime());
        RandomSource Random = World.getRandom();
        int Count = BaseSparks + Math.round(ExtraSparks * Strength);
        for (int Index = 0; Index < Count; Index++) {
            double Side = Random.nextBoolean() ? 1.0 : -1.0;
            double Out = Side * SparkSpeed * (0.4 + Random.nextDouble());
            double Across = (Random.nextDouble() - 0.5) * SparkSpeed * 1.6;
            double Lift = (Random.nextDouble() - 0.3) * SparkSpeed * 1.4;
            double SpeedX = Payload.AlongX() ? Across : Out;
            double SpeedZ = Payload.AlongX() ? Out : Across;
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, SpeedX, Lift, SpeedZ);
        }
        if (Strength >= 0.5f) {
            World.addParticle(ParticleTypes.END_ROD, Point.x, Point.y, Point.z, 0.0, 0.02, 0.0);
        }
    }
}
