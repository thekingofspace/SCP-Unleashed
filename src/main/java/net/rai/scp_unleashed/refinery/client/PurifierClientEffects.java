package net.rai.scp_unleashed.refinery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.PurifierLayout;
import net.rai.scp_unleashed.refinery.RefineryRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class PurifierClientEffects {
    public static final float SoundStart = 0.03f;
    public static final float ChurnLoudness = 0.7f;
    public static final float SloshChance = 0.03f;
    public static final float GrindChance = 0.02f;
    public static final float SplashChance = 0.25f;
    public static final double HopperLow = 19.5;
    public static final double HopperHigh = 28.5;
    public static final double HopperTop = 28.6;

    private static final Map<OrePurifierBlockEntity, PurifierLoopSound> Churns = new WeakHashMap<>();

    private PurifierClientEffects() {
    }

    public static void tick(OrePurifierBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        float Churn = Machine.getChurn();
        if (Churn <= SoundStart) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        keepLooping(Sounds, Churns, Machine, RefineryRegistry.PurifierChurn.get(), ChurnLoudness);
        RandomSource Random = World.getRandom();
        Vec3 Centre = Machine.getCentre();
        if (Random.nextFloat() < SloshChance * Churn) {
            World.playLocalSound(Centre.x, Centre.y - 0.4, Centre.z, RefineryRegistry.PurifierSlosh.get(), SoundSource.BLOCKS,
                    0.45f + 0.35f * Churn, 0.85f + Random.nextFloat() * 0.25f, false);
        }
        if (Random.nextFloat() < GrindChance * Churn) {
            World.playLocalSound(Centre.x, Centre.y - 0.4, Centre.z, RefineryRegistry.PurifierGrind.get(), SoundSource.BLOCKS,
                    0.18f + 0.12f * Churn, 0.55f + Random.nextFloat() * 0.15f, false);
        }
        if (Random.nextFloat() < SplashChance * Churn) {
            Direction Front = Machine.getFront();
            double Across = HopperLow + Random.nextDouble() * (HopperHigh - HopperLow);
            double Deep = 2.5 + Random.nextDouble() * 11.0;
            Vec3 Spot = PurifierLayout.toWorld(Machine.getBlockPos(), Front, Across, HopperTop, Deep);
            World.addParticle(ParticleTypes.SPLASH, Spot.x, Spot.y, Spot.z, 0.0, 0.08 + Random.nextDouble() * 0.06, 0.0);
        }
    }

    private static void keepLooping(SoundManager Sounds, Map<OrePurifierBlockEntity, PurifierLoopSound> Playing, OrePurifierBlockEntity Machine,
                                    SoundEvent Loop, float Loudness) {
        PurifierLoopSound Current = Playing.get(Machine);
        if (Current == null || !Sounds.isActive(Current)) {
            Current = new PurifierLoopSound(Machine, Loop, Loudness);
            Playing.put(Machine, Current);
            Sounds.play(Current);
        }
    }
}
