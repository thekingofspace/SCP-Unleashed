package net.rai.scp_unleashed.scp343;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Scp343Effects {
    public static final int AuraChance = 10;
    public static final int PhaseMotes = 2;

    private Scp343Effects() {
    }

    public static void wallGlow(ServerLevel World, Vec3 Centre, int Count) {
        World.sendParticles(Scp343Registry.Light.get(), Centre.x, Centre.y + 1.0, Centre.z, Count, 0.45, 0.8, 0.45, 0.01);
    }

    public static void blessing(ServerLevel World, Entity Target) {
        World.sendParticles(Scp343Registry.Light.get(), Target.getX(), Target.getY() + 1.0, Target.getZ(), 14, 0.4, 0.7, 0.4, 0.015);
    }

    public static void judgement(ServerLevel World, Entity Target, int Count) {
        World.sendParticles(Scp343Registry.Light.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), Count, 0.35, Target.getBbHeight() * 0.45,
                0.35, 0.04);
    }

    public static void clientAura(Scp343God God) {
        Level World = God.level();
        RandomSource Random = God.getRandom();
        int Motes = God.isPhasing() ? PhaseMotes : Random.nextInt(AuraChance) == 0 ? 1 : 0;
        for (int Index = 0; Index < Motes; Index++) {
            double X = God.getX() + (Random.nextDouble() - 0.5) * 0.9;
            double Y = God.getY() + 0.2 + Random.nextDouble() * 1.6;
            double Z = God.getZ() + (Random.nextDouble() - 0.5) * 0.9;
            World.addParticle(Scp343Registry.Light.get(), X, Y, Z, 0.0, 0.012, 0.0);
        }
    }
}
