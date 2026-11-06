package net.rai.scp_unleashed.fusion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fusion.FusionLayout;
import net.rai.scp_unleashed.fusion.FusionPhase;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.fusion.FusionRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class FusionClientEffects {
    private static final double HumFileSeconds = 5.7995;
    private static final double HumFilePitch = 0.8;
    private static final double HumStagger = 0.5 * HumFileSeconds / HumFilePitch;
    private static final double ChargeFileSeconds = 4.5487;
    private static final double ChargeStagger = 0.16 * ChargeFileSeconds;
    private static final Map<FusionReactorBlockEntity, FusionLoopChorus> Hums = new WeakHashMap<>();
    private static final Map<FusionReactorBlockEntity, FusionLoopChorus> Charges = new WeakHashMap<>();
    private static final double[][] Viewports = {{40.0, 24.0, 4.6, 0.0, -1.0}, {40.0, 24.0, 75.4, 0.0, 1.0}, {4.6, 24.0, 40.0, -1.0, 0.0}, {75.4, 24.0, 40.0, 1.0, 0.0}};
    private static final double[] InputTerminal = {88.0, 16.6, 40.0};
    private static final double[] OutputTerminal = {-8.0, 16.6, 40.0};

    private FusionClientEffects() {
    }

    public static void tick(FusionReactorBlockEntity Reactor) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Reactor.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        if (humLevel(Reactor) > 0.0) {
            FusionLoopChorus Hum = Hums.get(Reactor);
            if (Hum == null || !Hum.isPlaying(Sounds)) {
                if (Hum != null) {
                    Hum.stop(Sounds);
                }
                Hum = new FusionLoopChorus(Sounds, Reactor, FusionRegistry.Hum.get(), FusionRegistry.HumSub.get(), HumStagger, FusionClientEffects::humLevel, FusionClientEffects::humPitch);
                Hums.put(Reactor, Hum);
            }
            Hum.tick(Sounds, Reactor);
        }
        if (chargeLevel(Reactor) > 0.0) {
            FusionLoopChorus Charge = Charges.get(Reactor);
            if (Charge == null || !Charge.isPlaying(Sounds)) {
                if (Charge != null) {
                    Charge.stop(Sounds);
                }
                Charge = new FusionLoopChorus(Sounds, Reactor, FusionRegistry.Charge.get(), null, ChargeStagger, FusionClientEffects::chargeLevel, FusionClientEffects::chargePitch);
                Charges.put(Reactor, Charge);
            }
            Charge.tick(Sounds, Reactor);
        }
        particles(Reactor, World);
    }

    public static double humLevel(FusionReactorBlockEntity Reactor) {
        FusionPhase Phase = Reactor.getPhase();
        if (!Phase.isLit()) {
            return 0.0;
        }
        return Math.max(0.05, Reactor.getStability() / (double) FusionReactorBlockEntity.StabilityScale);
    }

    public static double humPitch(FusionReactorBlockEntity Reactor) {
        double Lift = Reactor.getStability() / (double) FusionReactorBlockEntity.StabilityScale;
        return 0.86 + 0.08 * Lift + 0.08 * Reactor.getClientLoad();
    }

    public static double chargeLevel(FusionReactorBlockEntity Reactor) {
        return Reactor.getPhase() == FusionPhase.Charging ? 0.35 + 0.65 * Reactor.getClientCharge() : 0.0;
    }

    public static double chargePitch(FusionReactorBlockEntity Reactor) {
        return 0.6 + 0.9 * Reactor.getClientCharge();
    }

    private static void particles(FusionReactorBlockEntity Reactor, Level World) {
        RandomSource Random = World.getRandom();
        BlockPos Origin = Reactor.getOrigin();
        Direction Front = Reactor.getFront();
        FusionPhase Phase = Reactor.getPhase();
        if (Phase.isLit()) {
            float Strength = Reactor.getStability() / (float) FusionReactorBlockEntity.StabilityScale;
            if (Random.nextFloat() < 0.35f * Strength) {
                double[] Window = Viewports[Random.nextInt(Viewports.length)];
                double Across = (Random.nextDouble() - 0.5) * 12.0;
                double Rise = (Random.nextDouble() - 0.5) * 10.0;
                double X = Window[0] + (Window[3] == 0.0 ? Across : 0.0);
                double Z = Window[2] + (Window[4] == 0.0 ? Across : 0.0);
                Vec3 At = FusionLayout.toWorld(Origin, Front, X, Window[1] + Rise, Z);
                Vec3 Out = FusionLayout.toWorld(Origin, Front, X + Window[3] * 4.0, Window[1] + Rise, Z + Window[4] * 4.0).subtract(At);
                World.addParticle(ParticleTypes.REVERSE_PORTAL, At.x, At.y, At.z, Out.x * 0.1, 0.01, Out.z * 0.1);
            }
            if (Phase == FusionPhase.Running && Reactor.getClientLoad() > 0.0f && Random.nextFloat() < 0.12f + 0.25f * Reactor.getClientLoad()) {
                spark(World, Random, FusionLayout.toWorld(Origin, Front, OutputTerminal[0], OutputTerminal[1], OutputTerminal[2]));
            }
        } else if (Phase == FusionPhase.Charging && Random.nextFloat() < 0.08f + 0.3f * Reactor.getClientCharge()) {
            spark(World, Random, FusionLayout.toWorld(Origin, Front, InputTerminal[0], InputTerminal[1], InputTerminal[2]));
        }
    }

    private static void spark(Level World, RandomSource Random, Vec3 At) {
        World.addParticle(ParticleTypes.ELECTRIC_SPARK, At.x + (Random.nextDouble() - 0.5) * 0.5, At.y + Random.nextDouble() * 0.25, At.z + (Random.nextDouble() - 0.5) * 0.5,
                (Random.nextDouble() - 0.5) * 0.1, Random.nextDouble() * 0.08, (Random.nextDouble() - 0.5) * 0.1);
    }
}
