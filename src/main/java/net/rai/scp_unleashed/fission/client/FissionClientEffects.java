package net.rai.scp_unleashed.fission.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.FissionReactorBlockEntity;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import net.rai.scp_unleashed.fission.HeatExchangerBlock;
import net.rai.scp_unleashed.fission.ReactorLayout;
import net.rai.scp_unleashed.fission.ReactorThermal;
import net.rai.scp_unleashed.fission.SonicVibratorBlock;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.fission.ToxicGasCloud;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class FissionClientEffects {
    public static final float HumStart = 0.02f;
    public static final int HumEvery = 120;
    public static final int DroneEvery = 90;
    public static final int WarningEvery = 10;
    public static final int CriticalEvery = 6;
    public static final int BellSteps = 4;
    public static final float[] WarningPitch = {1.0f, 0.75f};
    public static final float[] CriticalPitch = {1.0f, 0.8f};
    public static final float VibrationStart = 0.03f;
    public static final int VibratorHumEvery = 50;
    public static final int SloshMin = 25;
    public static final int SloshSpread = 30;
    public static final float SmokeRate = 0.35f;
    public static final float RingRate = 0.3f;
    public static final float SteamRate = 0.05f;
    public static final float ClickRate = 0.012f;
    public static final float SteamStart = 320.0f;
    public static final float SteamChance = 0.3f;
    public static final float HissChance = 0.03f;
    public static final float DissolveStart = 0.03f;
    public static final int BubbleMin = 18;
    public static final int BubbleSpread = 10;
    public static final int PopMin = 16;
    public static final int PopSpread = 40;
    public static final float SuctionRate = 0.35f;
    public static final double SuctionSpeed = 0.07;

    private static final Map<FissionReactorBlockEntity, ReactorVoice> ReactorVoices = new WeakHashMap<>();
    private static final Map<SonicVibratorBlockEntity, VibratorVoice> VibratorVoices = new WeakHashMap<>();
    private static final Map<GasDissolverBlockEntity, DissolverVoice> DissolverVoices = new WeakHashMap<>();

    private FissionClientEffects() {
    }

    private static final class ReactorVoice {
        private FissionLoopSound<FissionReactorBlockEntity> Core;
        private long NextHum;
        private long NextDrone;
        private long NextAlarm;
        private int AlarmStep;
    }

    private static final class VibratorVoice {
        private FissionLoopSound<SonicVibratorBlockEntity> Tone;
        private long NextHum;
        private long NextSlosh;
    }

    private static final class DissolverVoice {
        private long NextBubble;
        private long NextPop;
    }

    private static float humStrength(FissionReactorBlockEntity Reactor) {
        return Reactor.isRunning() || Reactor.getPower() > HumStart ? 0.35f + 0.65f * Reactor.getPower() : 0.0f;
    }

    public static void reactorTick(FissionReactorBlockEntity Reactor) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Reactor.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        ReactorVoice Voice = ReactorVoices.computeIfAbsent(Reactor, Key -> new ReactorVoice());
        Vec3 Centre = ReactorLayout.centre(Reactor.getBlockPos());
        long Now = World.getGameTime();
        if (humStrength(Reactor) > 0.0f) {
            if (Voice.Core == null || !Sounds.isActive(Voice.Core)) {
                Voice.Core = new FissionLoopSound<>(Reactor, FissionRegistry.ReactorCore.get(), Centre, 0.8f, 0.05f, 0.03f, true,
                        FissionClientEffects::humStrength, Target -> 1.0f);
                Sounds.play(Voice.Core);
            }
            if (Now >= Voice.NextHum) {
                Voice.NextHum = Now + HumEvery;
                Sounds.play(new FissionLoopSound<>(Reactor, FissionRegistry.ReactorHum.get(), Centre, 1.0f, 0.3f, 0.04f, false,
                        FissionClientEffects::humStrength, Target -> 0.9f + 0.2f * Target.getPower()));
            }
            if (Now >= Voice.NextDrone) {
                Voice.NextDrone = Now + DroneEvery;
                Sounds.play(new FissionLoopSound<>(Reactor, FissionRegistry.ReactorDrone.get(), Centre, 0.75f, 0.3f, 0.04f, false,
                        FissionClientEffects::humStrength, Target -> 0.9f + 0.15f * Target.getPower()));
            }
        } else {
            Voice.NextHum = 0L;
            Voice.NextDrone = 0L;
        }
        alarm(Reactor, World, Voice, Centre, Now);
        smoke(Reactor, World);
        steam(Reactor, World);
    }

    private static void alarm(FissionReactorBlockEntity Reactor, Level World, ReactorVoice Voice, Vec3 Centre, long Now) {
        ReactorThermal Thermal = Reactor.getThermal();
        if (Thermal == ReactorThermal.Normal) {
            Voice.NextAlarm = 0L;
            Voice.AlarmStep = 0;
            return;
        }
        if (Now < Voice.NextAlarm) {
            return;
        }
        double Y = Centre.y + 2.5;
        if (Thermal == ReactorThermal.Warning) {
            Voice.NextAlarm = Now + WarningEvery;
            World.playLocalSound(Centre.x, Y, Centre.z, FissionRegistry.ReactorAlarm.get(), SoundSource.BLOCKS, 1.0f, WarningPitch[Voice.AlarmStep % 2], false);
        } else {
            Voice.NextAlarm = Now + CriticalEvery;
            World.playLocalSound(Centre.x, Y, Centre.z, FissionRegistry.ReactorKlaxon.get(), SoundSource.BLOCKS, 1.0f, CriticalPitch[Voice.AlarmStep % 2], false);
            if (Voice.AlarmStep % BellSteps == 0) {
                World.playLocalSound(Centre.x, Y, Centre.z, FissionRegistry.ReactorBell.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
            }
        }
        Voice.AlarmStep++;
    }

    private static void smoke(FissionReactorBlockEntity Reactor, Level World) {
        float Heat = Reactor.getTemperature();
        if (Heat < ReactorThermal.WarningLevel) {
            return;
        }
        RandomSource Random = World.getRandom();
        float Strength = (Heat - ReactorThermal.WarningLevel) / (ReactorThermal.MeltdownLevel - ReactorThermal.WarningLevel);
        if (Random.nextFloat() > SmokeRate * (0.3f + Strength)) {
            return;
        }
        BlockPos Origin = Reactor.getBlockPos();
        double X = Origin.getX() + 1.0 + Random.nextDouble() * (ReactorLayout.Width - 2);
        double Z = Origin.getZ() + 1.0 + Random.nextDouble() * (ReactorLayout.Width - 2);
        double Y = Origin.getY() + ReactorLayout.Height + 0.05;
        World.addParticle(Strength > 0.5f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, X, Y, Z, (Random.nextDouble() - 0.5) * 0.02, 0.05 + Random.nextDouble() * 0.06, (Random.nextDouble() - 0.5) * 0.02);
        if (Heat >= ReactorThermal.CriticalLevel && Random.nextFloat() < 0.25f) {
            World.addParticle(ParticleTypes.LAVA, X, Y, Z, 0.0, 0.0, 0.0);
        }
    }

    private static void steam(FissionReactorBlockEntity Reactor, Level World) {
        RandomSource Random = World.getRandom();
        if (Reactor.getTemperature() < SteamStart || Random.nextFloat() > SteamChance) {
            return;
        }
        int Edge = Random.nextInt(4);
        int Along = Random.nextInt(ReactorLayout.Width);
        int X = Edge == 0 ? 0 : Edge == 1 ? ReactorLayout.Last : Along;
        int Z = Edge == 2 ? 0 : Edge == 3 ? ReactorLayout.Last : Along;
        BlockPos Below = Reactor.getBlockPos().offset(X, -1, Z);
        BlockState State = World.getBlockState(Below);
        if (!(State.getBlock() instanceof HeatExchangerBlock) || !State.getValue(HeatExchangerBlock.Active)) {
            return;
        }
        Direction Out = Edge == 0 ? Direction.WEST : Edge == 1 ? Direction.EAST : Edge == 2 ? Direction.NORTH : Direction.SOUTH;
        double PosX = Below.getX() + 0.5 + Out.getStepX() * 0.56 + (Out.getStepX() == 0 ? (Random.nextDouble() - 0.5) * 0.6 : 0.0);
        double PosZ = Below.getZ() + 0.5 + Out.getStepZ() * 0.56 + (Out.getStepZ() == 0 ? (Random.nextDouble() - 0.5) * 0.6 : 0.0);
        double PosY = Below.getY() + 0.55 + Random.nextDouble() * 0.3;
        World.addParticle(ParticleTypes.CLOUD, PosX, PosY, PosZ, Out.getStepX() * 0.03, 0.02 + Random.nextDouble() * 0.02, Out.getStepZ() * 0.03);
        if (Random.nextFloat() < HissChance) {
            World.playLocalSound(PosX, PosY, PosZ, FissionRegistry.ExchangerHiss.get(), SoundSource.BLOCKS, 0.35f, 0.9f + Random.nextFloat() * 0.2f, false);
        }
    }

    public static void vibratorTick(SonicVibratorBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        float Intensity = Machine.getIntensity();
        BlockPos Pos = Machine.getBlockPos();
        VibratorVoice Voice = VibratorVoices.computeIfAbsent(Machine, Key -> new VibratorVoice());
        if (Intensity <= VibrationStart) {
            Voice.NextHum = 0L;
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        RandomSource Random = World.getRandom();
        long Now = World.getGameTime();
        Vec3 Centre = Vec3.atCenterOf(Pos);
        if (Voice.Tone == null || !Sounds.isActive(Voice.Tone)) {
            Voice.Tone = new FissionLoopSound<>(Machine, FissionRegistry.VibratorTone.get(), Centre, 0.45f, 0.08f, 0.05f, true,
                    Target -> Target.getIntensity(), Target -> 0.9f + 0.2f * Target.getIntensity());
            Sounds.play(Voice.Tone);
        }
        if (Now >= Voice.NextHum) {
            Voice.NextHum = Now + VibratorHumEvery;
            Sounds.play(new FissionLoopSound<>(Machine, FissionRegistry.VibratorHum.get(), Centre, 0.7f, 0.3f, 0.06f, false,
                    Target -> Target.getIntensity(), Target -> 0.9f + 0.15f * Target.getIntensity()));
        }
        if (Now >= Voice.NextSlosh) {
            Voice.NextSlosh = Now + SloshMin + Random.nextInt(SloshSpread);
            World.playLocalSound(Centre.x, Centre.y, Centre.z, FissionRegistry.VibratorSlosh.get(), SoundSource.BLOCKS, 0.35f * Intensity, 0.85f + Random.nextFloat() * 0.2f, false);
        }
        if (Random.nextFloat() < ClickRate * Intensity) {
            World.playLocalSound(Centre.x, Pos.getY() + 1.0, Centre.z, FissionRegistry.VibratorClick.get(), SoundSource.BLOCKS, 0.45f, 0.9f + Random.nextFloat() * 0.3f, false);
        }
        if (Random.nextFloat() < RingRate * Intensity) {
            Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
            double Across = (Random.nextDouble() - 0.5) * 0.6;
            double X = Pos.getX() + 0.5 + Side.getStepX() * 0.47 + (Side.getStepX() == 0 ? Across : 0.0);
            double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.47 + (Side.getStepZ() == 0 ? Across : 0.0);
            double Y = Pos.getY() + 0.3 + Random.nextDouble() * 0.4;
            World.addParticle(FissionRegistry.SonicRing.get(), X, Y, Z, Side.getStepX() * 0.02, 0.0, Side.getStepZ() * 0.02);
        }
        if (Random.nextFloat() < RingRate * 0.4f * Intensity) {
            World.addParticle(ParticleTypes.SCULK_CHARGE_POP, Pos.getX() + 0.3 + Random.nextDouble() * 0.4, Pos.getY() + 1.15 + Random.nextDouble() * 0.25,
                    Pos.getZ() + 0.3 + Random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
        }
        if (Random.nextFloat() < SteamRate * Intensity) {
            BlockState State = World.getBlockState(Pos);
            Direction Front = State.hasProperty(SonicVibratorBlock.Facing) ? State.getValue(SonicVibratorBlock.Facing) : Direction.NORTH;
            Direction Back = Front.getOpposite();
            World.addParticle(ParticleTypes.WHITE_SMOKE, Pos.getX() + 0.5 + Back.getStepX() * 0.4, Pos.getY() + 0.85, Pos.getZ() + 0.5 + Back.getStepZ() * 0.4,
                    Back.getStepX() * 0.01, 0.03, Back.getStepZ() * 0.01);
        }
    }

    public static void dissolverTick(GasDissolverBlockEntity Dissolver) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Dissolver.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        BlockPos Pos = Dissolver.getBlockPos();
        RandomSource Random = World.getRandom();
        long Now = World.getGameTime();
        Vec3 Centre = Vec3.atCenterOf(Pos);
        DissolverVoice Voice = DissolverVoices.computeIfAbsent(Dissolver, Key -> new DissolverVoice());
        float Intensity = Dissolver.getIntensity();
        if (Intensity > DissolveStart) {
            if (Now >= Voice.NextBubble) {
                Voice.NextBubble = Now + BubbleMin + Random.nextInt(BubbleSpread);
                Game.getSoundManager().play(new FissionLoopSound<>(Dissolver, FissionRegistry.DissolverBubble.get(), Centre, 0.55f, 0.3f, 0.05f, false,
                        Target -> Target.getIntensity(), Target -> 1.0f));
            }
            if (Now >= Voice.NextPop) {
                Voice.NextPop = Now + PopMin + Random.nextInt(PopSpread);
                World.playLocalSound(Centre.x, Centre.y, Centre.z, FissionRegistry.DissolverPop.get(), SoundSource.BLOCKS, 0.3f * Intensity, 0.9f + Random.nextFloat() * 0.25f, false);
            }
        } else {
            Voice.NextBubble = 0L;
        }
        if (Dissolver.isScrubbing() && Random.nextFloat() < SuctionRate) {
            suction(World, Pos, Centre, Random);
        }
    }

    private static void suction(Level World, BlockPos Pos, Vec3 Centre, RandomSource Random) {
        double Radius = GasDissolverBlockEntity.ScrubRadius;
        List<ToxicGasCloud> Clouds = World.getEntitiesOfClass(ToxicGasCloud.class, new AABB(Pos).inflate(Radius),
                Cloud -> Cloud.getGas() > 1.0f && Cloud.position().distanceToSqr(Centre) <= Radius * Radius);
        if (Clouds.isEmpty()) {
            return;
        }
        ToxicGasCloud Cloud = Clouds.get(Random.nextInt(Clouds.size()));
        double Spread = Cloud.getRadius() * 0.7;
        Vec3 From = Cloud.position().add((Random.nextDouble() - 0.5) * 2.0 * Spread, (Random.nextDouble() - 0.5) * Spread, (Random.nextDouble() - 0.5) * 2.0 * Spread);
        Vec3 Pull = Centre.subtract(From).normalize().scale(SuctionSpeed * (0.8 + Random.nextDouble() * 0.4));
        World.addParticle(FissionRegistry.ToxicGasParticle.get(), From.x, From.y, From.z, Pull.x, Pull.y, Pull.z);
    }
}
