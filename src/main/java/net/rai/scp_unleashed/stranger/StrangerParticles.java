package net.rai.scp_unleashed.stranger;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class StrangerParticles {
    private StrangerParticles() {
    }

    public static void tick(StrangerEntity Figure) {
        Level World = Figure.level();
        RandomSource Random = Figure.getRandom();
        int Ticks = Figure.clientTicks();
        switch (Figure.clientState()) {
            case StrangerEntity.StateAppearing -> {
                float Left = 1.0f - Mth.clamp((float) Ticks / StrangerEntity.AppearTicks, 0.0f, 1.0f);
                int Count = Random.nextFloat() < Left ? 2 : 0;
                for (int Index = 0; Index < Count; Index++) {
                    rising(Figure, World, Random, 0.45, 0.02);
                }
            }
            case StrangerEntity.StateVanishing -> {
                float Progress = Mth.clamp((float) Ticks / StrangerEntity.VanishTicks, 0.0f, 1.0f);
                int Count = 3 + (int) (Progress * 4.0f);
                for (int Index = 0; Index < Count; Index++) {
                    double Angle = Random.nextDouble() * Math.PI * 2.0;
                    double Radius = 0.25 + Random.nextDouble() * 0.25 * (1.0 - Progress);
                    double Height = Random.nextDouble() * 1.9 * (1.0 - Progress * 0.4);
                    World.addParticle(StrangerRegistry.Shadow.get(), Figure.getX() + Math.cos(Angle) * Radius, Figure.getY() + Height, Figure.getZ() + Math.sin(Angle) * Radius,
                            -Math.sin(Angle) * 0.04, 0.03 + Random.nextDouble() * 0.05, Math.cos(Angle) * 0.04);
                }
            }
            case StrangerEntity.StateFading -> {
                if (Random.nextInt(2) == 0) {
                    rising(Figure, World, Random, 0.35, 0.015);
                }
            }
            default -> {
                if (Random.nextInt(14) == 0) {
                    double Angle = Random.nextDouble() * Math.PI * 2.0;
                    World.addParticle(StrangerRegistry.Shadow.get(), Figure.getX() + Math.cos(Angle) * 0.35, Figure.getY() + 0.05 + Random.nextDouble() * 0.2,
                            Figure.getZ() + Math.sin(Angle) * 0.35, 0.0, 0.006, 0.0);
                }
            }
        }
    }

    private static void rising(StrangerEntity Figure, Level World, RandomSource Random, double Spread, double Lift) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Radius = Random.nextDouble() * Spread;
        World.addParticle(StrangerRegistry.Shadow.get(), Figure.getX() + Math.cos(Angle) * Radius, Figure.getY() + Random.nextDouble() * 1.8, Figure.getZ() + Math.sin(Angle) * Radius,
                0.0, Lift + Random.nextDouble() * Lift, 0.0);
    }
}
