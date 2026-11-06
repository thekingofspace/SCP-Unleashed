package net.rai.scp_unleashed.rocketry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.rocketry.RocketryBursts;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketryInputPayload;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class RocketryClientEffects {
    public static final double ShakeReach = 64.0;
    public static final int ResendTicks = 10;
    public static final int FlightSteps = 2;
    public static final int FlamesPerStep = 2;
    public static final int LiftoffSmoke = 12;
    public static final int IgnitionSmoke = 6;
    public static final int LandDust = 24;
    public static final int SalvagePoof = 10;
    private static final Map<Entity, Boolean> Voiced = new WeakHashMap<>();
    private static final Map<DropPodEntity, Boolean> PodLanded = new WeakHashMap<>();
    private static final Map<DropPodEntity, Boolean> PodThrust = new WeakHashMap<>();
    private static final Map<Entity, Boolean> Grounded = new WeakHashMap<>();
    private static float Shake;
    private static float Impulse;
    private static int Ticks;
    private static boolean LastThrust;
    private static int Resend;

    private RocketryClientEffects() {
    }

    private static float proximity(Vec3 From) {
        LocalPlayer User = Minecraft.getInstance().player;
        if (User == null) {
            return 0.0f;
        }
        double Distance = User.position().distanceTo(From);
        return (float) Mth.clamp(1.0 - Distance / ShakeReach, 0.0, 1.0);
    }

    private static boolean ridden(Entity Vehicle) {
        LocalPlayer User = Minecraft.getInstance().player;
        return User != null && User.getVehicle() == Vehicle;
    }

    private static void addShake(float Amount) {
        Shake = Math.max(Shake, Amount);
    }

    private static void puff(ClientLevel World, ParticleOptions Kind, boolean Far, double X, double Y, double Z, double Dx, double Dy, double Dz) {
        if (Far) {
            World.addAlwaysVisibleParticle(Kind, true, X, Y, Z, Dx, Dy, Dz);
        } else {
            World.addParticle(Kind, X, Y, Z, Dx, Dy, Dz);
        }
    }

    public static void rocketTick(RocketEntity Rocket) {
        if (!(Rocket.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.random;
        byte Phase = Rocket.getPhase();
        if (Phase == RocketEntity.PhaseIdle) {
            Voiced.remove(Rocket);
            return;
        }
        voice(Rocket);
        double X = Rocket.getX();
        double Base = Rocket.getStandY();
        int Time = Rocket.getPhaseTicks();
        boolean Riding = ridden(Rocket);
        Vec3 Pad = new Vec3(X, Base, Rocket.getZ());
        if (Phase == RocketEntity.PhaseCountdown) {
            float Build = Mth.clamp(Time / (float) RocketEntity.CountdownTicks, 0.0f, 1.0f);
            addShake((Riding ? 0.45f : 0.3f) * Build * Math.max(Riding ? 1.0f : 0.0f, proximity(Pad)));
            if (Random.nextFloat() < 0.6f) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                puff(World, ParticleTypes.CLOUD, false, X + Math.cos(Angle) * 1.05, Base + 1.6 + Random.nextDouble() * 2.0, Rocket.getZ() + Math.sin(Angle) * 1.05,
                        Math.cos(Angle) * 0.08, 0.01, Math.sin(Angle) * 0.08);
            }
            if (Time > RocketEntity.CountdownTicks - 30) {
                for (int Index = 0; Index < 3; Index++) {
                    puff(World, ParticleTypes.FLAME, false, X + (Random.nextDouble() - 0.5) * 0.5, Base + 0.2, Rocket.getZ() + (Random.nextDouble() - 0.5) * 0.5,
                            (Random.nextDouble() - 0.5) * 0.1, -0.05, (Random.nextDouble() - 0.5) * 0.1);
                }
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                puff(World, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, X + Math.cos(Angle), Base + 0.3, Rocket.getZ() + Math.sin(Angle),
                        Math.cos(Angle) * 0.18, 0.01, Math.sin(Angle) * 0.18);
            }
            return;
        }
        float Fade = Math.max(0.0f, 1.0f - Time / 120.0f);
        addShake(Riding ? Math.max(0.2f, 1.1f * Fade) : 1.0f * Fade * proximity(Pad));
        double Top = Rocket.getY();
        double Below = Rocket.yo;
        for (int Step = 0; Step < FlightSteps; Step++) {
            double Y = Mth.lerp(Step / (double) FlightSteps, Top, Below) - 0.3;
            for (int Index = 0; Index < FlamesPerStep; Index++) {
                puff(World, ParticleTypes.FLAME, false, X + (Random.nextDouble() - 0.5) * 0.7, Y, Rocket.getZ() + (Random.nextDouble() - 0.5) * 0.7,
                        (Random.nextDouble() - 0.5) * 0.12, -0.6 - Random.nextDouble() * 0.5, (Random.nextDouble() - 0.5) * 0.12);
            }
            puff(World, ParticleTypes.LARGE_SMOKE, Step == 0, X + (Random.nextDouble() - 0.5) * 0.9, Y - 0.6, Rocket.getZ() + (Random.nextDouble() - 0.5) * 0.9,
                    (Random.nextDouble() - 0.5) * 0.08, -0.2, (Random.nextDouble() - 0.5) * 0.08);
        }
        if (Time % 3 == 0) {
            puff(World, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, X + (Random.nextDouble() - 0.5) * 0.8, Top - 1.2, Rocket.getZ() + (Random.nextDouble() - 0.5) * 0.8,
                    0.0, 0.02, 0.0);
        }
        if (Random.nextFloat() < 0.3f) {
            puff(World, ParticleTypes.LAVA, false, X, Top - 0.2, Rocket.getZ(), 0.0, 0.0, 0.0);
        }
        if (Time < 40) {
            for (int Index = 0; Index < 3; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Speed = 0.25 + Random.nextDouble() * 0.25;
                puff(World, Index % 2 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CLOUD, Index == 0, X + Math.cos(Angle) * 1.2, Base + 0.4,
                        Rocket.getZ() + Math.sin(Angle) * 1.2, Math.cos(Angle) * Speed, 0.02, Math.sin(Angle) * Speed);
            }
        }
    }

    public static void burst(Entity Source, byte Id) {
        if (!(Source.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.random;
        double X = Source.getX();
        double Y = Source.getY();
        double Z = Source.getZ();
        if (Id == RocketryBursts.Liftoff) {
            World.addParticle(ParticleTypes.EXPLOSION_EMITTER, X, Y + 0.5, Z, 0.0, 0.0, 0.0);
            for (int Index = 0; Index < LiftoffSmoke; Index++) {
                double Angle = Index * Math.PI * 2.0 / LiftoffSmoke;
                puff(World, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, Index % 3 == 0, X + Math.cos(Angle) * 1.6, Y + 0.5, Z + Math.sin(Angle) * 1.6,
                        Math.cos(Angle) * 0.05, 0.04, Math.sin(Angle) * 0.05);
            }
            Impulse = Math.max(Impulse, 0.6f * proximity(Source.position()));
        } else if (Id == RocketryBursts.Ignition) {
            World.addParticle(ParticleTypes.EXPLOSION, X, Y + 0.2, Z, 0.0, 0.0, 0.0);
            for (int Index = 0; Index < IgnitionSmoke; Index++) {
                double Angle = Index * Math.PI * 2.0 / IgnitionSmoke;
                puff(World, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, Index == 0, X + Math.cos(Angle) * 0.8, Y + 0.2, Z + Math.sin(Angle) * 0.8,
                        Math.cos(Angle) * 0.03, 0.03, Math.sin(Angle) * 0.03);
            }
        } else if (Id == RocketryBursts.LandSoft || Id == RocketryBursts.LandHard) {
            Grounded.put(Source, Boolean.TRUE);
            boolean Hard = Id == RocketryBursts.LandHard;
            BlockState Ground = World.getBlockState(Source.getOnPos());
            if (Ground.isAir()) {
                Ground = World.getBlockState(Source.blockPosition().below());
            }
            int Dust = Hard ? LandDust * 2 : LandDust;
            for (int Index = 0; Index < Dust; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Reach = 0.6 + Random.nextDouble() * 1.4;
                double Dx = Math.cos(Angle) * Reach;
                double Dz = Math.sin(Angle) * Reach;
                if (!Ground.isAir() && Index % 2 == 0) {
                    World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Ground), X + Dx, Y + 0.1, Z + Dz, Dx * 0.15, 0.15, Dz * 0.15);
                } else if (Index % 4 == 1) {
                    World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, X + Dx, Y + 0.2, Z + Dz, Dx * 0.02, 0.02, Dz * 0.02);
                } else {
                    World.addParticle(ParticleTypes.POOF, X + Dx, Y + 0.3, Z + Dz, Dx * 0.06, 0.02, Dz * 0.06);
                }
            }
        } else if (Id == RocketryBursts.Salvage) {
            for (int Index = 0; Index < SalvagePoof; Index++) {
                World.addParticle(ParticleTypes.POOF, X + (Random.nextDouble() - 0.5) * 1.6, Y + 0.4 + Random.nextDouble() * 1.6, Z + (Random.nextDouble() - 0.5) * 1.6,
                        0.0, 0.02, 0.0);
            }
        }
    }

    public static void podTick(DropPodEntity Pod) {
        if (!(Pod.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.random;
        boolean Riding = ridden(Pod);
        boolean Landed = Pod.isLanded();
        Boolean Before = PodLanded.put(Pod, Landed);
        if (Before != null && !Before && Landed && Riding) {
            Impulse = Math.max(Impulse, Pod.getImpact() == DropPodEntity.ImpactHard ? 1.0f : 0.45f);
        }
        if (Landed) {
            return;
        }
        voice(Pod);
        boolean Thrusting = Pod.isThrusting();
        Boolean WasThrusting = PodThrust.put(Pod, Thrusting);
        if (Thrusting && !Boolean.TRUE.equals(WasThrusting) && !podDone(Pod)) {
            float Tone = 0.9f + Random.nextFloat() * 0.2f;
            Minecraft.getInstance().getSoundManager().play(EntityLoopSound.once(Pod, RocketryRegistry.Thruster.get(), 1.2, Tone,
                    Each -> !((DropPodEntity) Each).isThrusting() || podDone(Each)));
        }
        if (Riding) {
            addShake(Pod.isThrusting() ? 0.3f : 0.12f);
        }
        if (Pod.isThrusting()) {
            for (int Corner = 0; Corner < 4; Corner++) {
                double Dx = Corner % 2 == 0 ? -0.7 : 0.7;
                double Dz = Corner < 2 ? -0.7 : 0.7;
                puff(World, ParticleTypes.FLAME, false, Pod.getX() + Dx, Pod.getY() + 0.1, Pod.getZ() + Dz, Dx * 0.05, -0.5 - Random.nextDouble() * 0.3, Dz * 0.05);
                if (Random.nextFloat() < 0.5f) {
                    puff(World, ParticleTypes.SMOKE, false, Pod.getX() + Dx, Pod.getY() - 0.3, Pod.getZ() + Dz, 0.0, -0.2, 0.0);
                }
            }
        } else if (Random.nextFloat() < 0.5f) {
            puff(World, ParticleTypes.CLOUD, true, Pod.getX() + (Random.nextDouble() - 0.5), Pod.getY() + DropPodEntity.Height, Pod.getZ() + (Random.nextDouble() - 0.5),
                    0.0, 0.05, 0.0);
        }
    }

    public static void boosterTick(SatelliteBoosterEntity Booster) {
        if (!(Booster.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.random;
        double X = Booster.getX();
        double Z = Booster.getZ();
        Vec3 Pad = new Vec3(X, Booster.getStartY(), Z);
        if (!Booster.isClimbing()) {
            if (Random.nextFloat() < 0.5f) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                puff(World, ParticleTypes.CLOUD, false, X + Math.cos(Angle) * 0.4, Booster.getStartY() + 0.4, Z + Math.sin(Angle) * 0.4,
                        Math.cos(Angle) * 0.06, 0.02, Math.sin(Angle) * 0.06);
            }
            addShake(0.08f * proximity(Pad));
            return;
        }
        voice(Booster);
        int Burn = Booster.getFlightTicks() - SatelliteBoosterEntity.IgnitionTicks;
        addShake(Math.max(0.0f, 0.35f - Burn / 120.0f) * proximity(Pad));
        double Top = Booster.getY();
        double Below = Booster.yo;
        double Y = Mth.lerp(0.5, Top, Below) - 0.2;
        puff(World, ParticleTypes.FLAME, false, X + (Random.nextDouble() - 0.5) * 0.25, Y, Z + (Random.nextDouble() - 0.5) * 0.25,
                (Random.nextDouble() - 0.5) * 0.05, -0.4, (Random.nextDouble() - 0.5) * 0.05);
        puff(World, ParticleTypes.SMOKE, false, X + (Random.nextDouble() - 0.5) * 0.3, Y - 0.4, Z + (Random.nextDouble() - 0.5) * 0.3, 0.0, -0.05, 0.0);
        if (Burn % 4 == 0) {
            puff(World, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, X, Top - 0.8, Z, 0.0, 0.02, 0.0);
        }
    }

    public static boolean podDone(Entity Source) {
        return !(Source instanceof DropPodEntity Pod) || Pod.isLanded() || Grounded.containsKey(Pod);
    }

    private static boolean rocketIdle(Entity Source) {
        return !(Source instanceof RocketEntity Rocket) || Rocket.getPhase() == RocketEntity.PhaseIdle;
    }

    private static void voice(Entity Source) {
        if (Voiced.containsKey(Source)) {
            return;
        }
        Voiced.put(Source, Boolean.TRUE);
        SoundManager Sounds = Minecraft.getInstance().getSoundManager();
        if (Source instanceof RocketEntity Rocket) {
            Sounds.play(EntityLoopSound.loop(Rocket, RocketryRegistry.Roar.get(), Each -> rocketLoud((RocketEntity) Each, 0.4f, 2.2f),
                    Each -> 0.75 + Math.min(0.25, ((RocketEntity) Each).getPhaseTicks() / 800.0), RocketryClientEffects::rocketIdle));
            Sounds.play(EntityLoopSound.loop(Rocket, RocketryRegistry.RoarFire.get(), Each -> rocketLoud((RocketEntity) Each, 0.25f, 1.6f), Each -> 0.6,
                    RocketryClientEffects::rocketIdle));
            Sounds.play(EntityLoopSound.loop(Rocket, RocketryRegistry.RoarRumble.get(), Each -> rocketLoud((RocketEntity) Each, 0.5f, 2.0f), Each -> 0.5,
                    RocketryClientEffects::rocketIdle));
        } else if (Source instanceof DropPodEntity Pod) {
            Sounds.play(EntityLoopSound.loop(Pod, RocketryRegistry.Reentry.get(), RocketryClientEffects::fallLoud, Each -> 0.7, RocketryClientEffects::podDone));
            Sounds.play(EntityLoopSound.loop(Pod, RocketryRegistry.ThrusterHiss.get(), Each -> ((DropPodEntity) Each).isThrusting() ? 1.1 : 0.0, Each -> 1.0,
                    RocketryClientEffects::podDone));
        } else if (Source instanceof SatelliteBoosterEntity) {
            Sounds.play(EntityLoopSound.loop(Source, RocketryRegistry.BoosterBurn.get(), Each -> 1.6, Each -> 1.1, Each -> false));
        }
    }

    private static double fallLoud(Entity Source) {
        return podDone(Source) ? 0.0 : Math.min(1.2, Math.abs(Source.getY() - Source.yo) * 0.7);
    }

    private static double rocketLoud(RocketEntity Rocket, float Countdown, float Flight) {
        if (Rocket.getPhase() == RocketEntity.PhaseCountdown) {
            return Countdown * Mth.clamp(Rocket.getPhaseTicks() / (float) RocketEntity.CountdownTicks, 0.1f, 1.0f);
        }
        return Rocket.getPhase() == RocketEntity.PhaseFlight ? Flight : 0.0;
    }

    public static void clientTick() {
        Ticks++;
        Shake *= 0.82f;
        Impulse *= 0.86f;
        if (Shake < 0.005f) {
            Shake = 0.0f;
        }
        if (Impulse < 0.005f) {
            Impulse = 0.0f;
        }
        LocalPlayer User = Minecraft.getInstance().player;
        if (User == null || Minecraft.getInstance().getConnection() == null) {
            LastThrust = false;
            return;
        }
        Entity Vehicle = User.getVehicle();
        if (!(Vehicle instanceof RocketEntity) && !(Vehicle instanceof DropPodEntity)) {
            LastThrust = false;
            return;
        }
        boolean Thrust = User.input.jumping;
        Resend--;
        if (Thrust != LastThrust || Thrust && Resend <= 0) {
            LastThrust = Thrust;
            Resend = ResendTicks;
            PacketDistributor.sendToServer(new RocketryInputPayload(Thrust));
        }
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Amount = Shake + Impulse;
        if (Amount < 0.01f) {
            return;
        }
        double Time = Ticks + Event.getPartialTick();
        float Yaw = (float) (Math.sin(Time * 1.9) * Math.cos(Time * 0.73) + Math.sin(Time * 3.7) * 0.35) * Amount * 1.2f;
        float Pitch = (float) (Math.sin(Time * 2.3 + 1.1) * Math.cos(Time * 0.51) + Math.sin(Time * 4.1 + 0.3) * 0.35) * Amount * 1.4f;
        float Roll = (float) (Math.sin(Time * 1.6 + 0.4) + Math.sin(Time * 3.1 + 2.0) * 0.4) * Amount * 1.6f;
        Event.setYaw(Event.getYaw() + Yaw);
        Event.setPitch(Event.getPitch() + Pitch);
        Event.setRoll(Event.getRoll() + Roll);
    }
}
