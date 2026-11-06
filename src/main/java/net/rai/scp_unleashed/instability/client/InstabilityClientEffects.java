package net.rai.scp_unleashed.instability.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.instability.InstabilityReactorBlockEntity;
import net.rai.scp_unleashed.instability.InstabilityRegistry;
import net.rai.scp_unleashed.instability.ReactorStatus;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class InstabilityClientEffects {
    public static final int PulsePeriod = 96;
    public static final float HumStart = 0.02f;
    public static final float RunningMotes = 0.3f;
    public static final float IdleMotes = 0.08f;
    public static final double ReachMin = 0.45;
    public static final double ReachSpread = 0.3;

    private static final Map<InstabilityReactorBlockEntity, InstabilityHumSound> Hums = new WeakHashMap<>();

    private InstabilityClientEffects() {
    }

    public static void tick(InstabilityReactorBlockEntity Reactor) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Reactor.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        Vec3 Centre = Reactor.getCoreCentre();
        RandomSource Random = World.getRandom();
        int Change = Reactor.takePendingChange();
        if (Change > 0) {
            World.playLocalSound(Centre.x, Centre.y, Centre.z, InstabilityRegistry.Ignite.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
        } else if (Change < 0) {
            World.playLocalSound(Centre.x, Centre.y, Centre.z, InstabilityRegistry.Shutdown.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
        }
        float Open = Reactor.getOpen(1.0f);
        if (Open > HumStart) {
            SoundManager Sounds = Game.getSoundManager();
            InstabilityHumSound Hum = Hums.get(Reactor);
            if (Hum == null || !Sounds.isActive(Hum)) {
                Hum = new InstabilityHumSound(Reactor);
                Hums.put(Reactor, Hum);
                Sounds.play(Hum);
            }
        }
        int Tier = Reactor.getVisualGrade();
        int Colour = Reactor.getTint();
        int Surge = Reactor.takePendingSurge();
        ReactorStatus Status = Reactor.getStatus();
        if (Surge > 0) {
            float Pitch = 0.92f - 0.05f * Surge + Random.nextFloat() * 0.08f;
            World.playLocalSound(Centre.x, Centre.y, Centre.z, InstabilityRegistry.Pulse.get(), SoundSource.BLOCKS, 0.6f + 0.08f * Surge, Pitch, false);
            sparks(World, Random, Centre, Colour, 5 + 2 * Surge);
            if (Surge >= 3) {
                World.addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.18f + 0.03f * Surge), Centre.x, Centre.y, Centre.z, 0.0, 0.0, 0.0);
            }
        } else if (Status == ReactorStatus.Running && (Reactor.getClock() + (Reactor.getBlockPos().hashCode() & 63)) % PulsePeriod == 0) {
            World.playLocalSound(Centre.x, Centre.y, Centre.z, InstabilityRegistry.Pulse.get(), SoundSource.BLOCKS, 0.32f, 1.0f - 0.04f * Tier, false);
        }
        float MoteChance = Status == ReactorStatus.Running ? RunningMotes : Status == ReactorStatus.Idle ? IdleMotes : 0.0f;
        if (MoteChance > 0.0f && Random.nextFloat() < MoteChance * Mth.clamp(Open, 0.0f, 1.0f)) {
            mote(World, Random, Centre, Colour);
        }
    }

    private static void mote(Level World, RandomSource Random, Vec3 Centre, int Colour) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = ReachMin + Random.nextDouble() * ReachSpread;
        double X = Centre.x + Math.cos(Angle) * Reach;
        double Y = Centre.y + (Random.nextDouble() - 0.5) * 0.6;
        double Z = Centre.z + Math.sin(Angle) * Reach;
        double Inward = 0.01 + Random.nextDouble() * 0.008;
        double Swirl = 0.006;
        double SpeedX = (Centre.x - X) / Reach * Inward - Math.sin(Angle) * Swirl;
        double SpeedZ = (Centre.z - Z) / Reach * Inward + Math.cos(Angle) * Swirl;
        double SpeedY = (Centre.y - Y) * 0.02;
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 0.55f), X, Y, Z, SpeedX, SpeedY, SpeedZ);
    }

    private static void sparks(Level World, RandomSource Random, Vec3 Centre, int Colour, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Lift = (Random.nextDouble() - 0.5) * 0.5;
            double Speed = 0.05 + Random.nextDouble() * 0.07;
            double X = Centre.x + Math.cos(Angle) * 0.12;
            double Y = Centre.y + Lift * 0.4;
            double Z = Centre.z + Math.sin(Angle) * 0.12;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.8f), X, Y, Z,
                    Math.cos(Angle) * Speed, Lift * Speed, Math.sin(Angle) * Speed);
        }
    }
}
