package net.rai.scp_unleashed.scp055.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp055.Scp055Registry;
import net.rai.scp_unleashed.scp055.Scp055Step;
import net.rai.scp_unleashed.scp055.Scp055Wander;
import net.rai.scp_unleashed.scp055.network.Scp055EpisodePayload;
import net.rai.scp_unleashed.scp055.network.Scp055WakePayload;

import java.util.ArrayList;
import java.util.List;

public final class Scp055ClientEpisode {
    public static final int RampSteps = 6;
    public static final float WalkLevel = 0.5f;
    public static final float WalkPulse = 0.16f;
    public static final int PulsePeriod = 28;
    public static final float[] PulseLevels = {0.0f, 0.5f, 1.0f, 0.5f, 0.0f, -0.5f, -1.0f, -0.5f};
    public static final float StaticFullAt = 0.72f;
    public static final int StaticSteps = 8;
    public static final int WakeFade = 12;
    public static final int InterruptFade = 8;
    public static final float InterruptStatic = 0.8f;
    public static final int WakeTimeout = 60;
    public static final int FlickerCount = 2;
    public static final int FlickerTicks = 2;
    public static final float FlickerLevel = 0.3f;
    public static final float ImpulseSpeed = 0.7f;
    public static final float LastLegFloor = 0.35f;
    public static final double ArriveRadius = 0.22;
    public static final double SlowRadius = 0.6;
    public static final double ClimbReach = 1.4;
    public static final double StillDistance = 0.01;
    public static final int StuckTicks = 12;
    public static final float LookRate = 0.2f;
    public static final float WalkRate = 0.35f;
    public static final float MaxTurn = 8.0f;
    public static final float WalkTurn = 14.0f;
    public static final float SwayYaw = 5.0f;
    public static final float SwayPitch = 3.0f;
    public static final float WalkPitch = 8.0f;
    public static final float RollDegrees = 3.5f;
    public static final float FovPulse = 0.05f;
    public static final float OnsetVolume = 0.9f;
    public static final float DroneVolume = 0.55f;
    public static final float StaticVolume = 0.85f;
    public static final float WakeVolume = 0.8f;
    public static final float HintVolume = 0.22f;
    public static final int StaticRepeat = 18;
    public static final double Frozen = -(double) 0.2f / (double) 0.6f;
    private static final List<SoundInstance> Playing = new ArrayList<>();
    private static boolean Active;
    private static int Age;
    private static int DistortTicks;
    private static int StaticTicks;
    private static int WalkTicks;
    private static List<Scp055Step> Steps = List.of();
    private static int[] Starts = new int[0];
    private static int[] Flickers = new int[0];
    private static long Seed;
    private static int WakeAge = -1;
    private static boolean Interrupted;
    private static boolean Abandoned;
    private static boolean DroneStarted;
    private static boolean StaticStarted;
    private static int StepIndex = -1;
    private static int Waypoint;
    private static float StartYaw;
    private static float StartPitch;
    private static float AnchorYaw;
    private static float AnchorPitch;
    private static Vec3 LastSpot = Vec3.ZERO;
    private static int StillTicks;

    private Scp055ClientEpisode() {
    }

    public static void start(Scp055EpisodePayload Payload) {
        LocalPlayer User = Minecraft.getInstance().player;
        if (User == null) {
            return;
        }
        stopSounds();
        Active = true;
        Age = 0;
        DistortTicks = Payload.DistortTicks();
        StaticTicks = Payload.StaticTicks();
        Steps = List.copyOf(Payload.Steps());
        WalkTicks = Scp055Step.total(Steps);
        Starts = new int[Steps.size()];
        int Start = DistortTicks;
        for (int Index = 0; Index < Steps.size(); Index++) {
            Starts[Index] = Start;
            Start += Steps.get(Index).Ticks();
        }
        Seed = Payload.Seed();
        RandomSource Random = RandomSource.create(Seed ^ 0x5505L);
        Flickers = new int[FlickerCount];
        for (int Index = 0; Index < FlickerCount; Index++) {
            Flickers[Index] = DistortTicks + 10 + Random.nextInt(Math.max(1, WalkTicks - 20));
        }
        WakeAge = -1;
        Interrupted = false;
        Abandoned = false;
        DroneStarted = false;
        StaticStarted = false;
        StepIndex = -1;
        Waypoint = 0;
        StartYaw = User.getYRot();
        StartPitch = User.getXRot();
        AnchorYaw = StartYaw;
        AnchorPitch = StartPitch;
        LastSpot = User.position();
        StillTicks = 0;
        User.setSprinting(false);
        play(Scp055Registry.Onset.get(), 1.0f, OnsetVolume, true);
        play(Scp055Registry.OnsetLow.get(), 1.0f, OnsetVolume, true);
    }

    public static void wake(Scp055WakePayload Payload) {
        if (!Active || WakeAge >= 0) {
            return;
        }
        WakeAge = Age;
        Interrupted = Payload.Interrupted();
        stopSounds();
        if (!Interrupted) {
            play(Scp055Registry.Wake.get(), 1.0f, WakeVolume, false);
            play(Scp055Registry.Clasp.get(), 1.15f, HintVolume, false);
        }
    }

    public static void reset() {
        stopSounds();
        Active = false;
        Age = 0;
        Steps = List.of();
        Starts = new int[0];
        Flickers = new int[0];
        WakeAge = -1;
        StepIndex = -1;
    }

    public static boolean isActive() {
        return Active;
    }

    public static boolean controlling() {
        return Active && WakeAge < 0;
    }

    public static int age() {
        return Age;
    }

    public static long seed() {
        return Seed;
    }

    public static int total() {
        return DistortTicks + WalkTicks + StaticTicks;
    }

    public static void tick(Minecraft Client) {
        if (!Active) {
            return;
        }
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Age++;
        if (WakeAge >= 0) {
            if (Age - WakeAge > fadeLength()) {
                reset();
            }
            return;
        }
        if (Age > total() + WakeTimeout) {
            WakeAge = Age;
            Interrupted = true;
            stopSounds();
            return;
        }
        if (!DroneStarted && Age >= DistortTicks) {
            DroneStarted = true;
            play(Scp055Registry.Drone.get(), 1.0f, DroneVolume, true);
        }
        int StaticAge = Age - DistortTicks - WalkTicks;
        if (!StaticStarted && StaticAge >= 0) {
            StaticStarted = true;
            play(Scp055Registry.StaticRush.get(), 1.0f, StaticVolume, true);
        }
        if (StaticStarted && StaticAge % StaticRepeat == 0) {
            play(Scp055Registry.StaticHiss.get(), 1.0f, StaticVolume, true);
            play(Scp055Registry.StaticCrackle.get(), 1.0f, StaticVolume, true);
        }
        User.setSprinting(false);
        if (Age < DistortTicks + WalkTicks) {
            steerView(User);
        }
    }

    public static void drainKeys(Minecraft Client) {
        if (!controlling()) {
            return;
        }
        for (KeyMapping Key : Client.options.keyHotbarSlots) {
            drain(Key);
        }
        drain(Client.options.keyDrop);
        drain(Client.options.keySwapOffhand);
        drain(Client.options.keyInventory);
    }

    private static void drain(KeyMapping Key) {
        while (Key.consumeClick()) {
            Key.setDown(false);
        }
    }

    private static int fadeLength() {
        return Interrupted ? InterruptFade : WakeFade;
    }

    private static int stepAt(int When) {
        for (int Index = 0; Index < Steps.size(); Index++) {
            if (When >= Starts[Index] && When < Starts[Index] + Steps.get(Index).Ticks()) {
                return Index;
            }
        }
        return -1;
    }

    private static void enterStep(LocalPlayer User, int Index) {
        if (StepIndex >= 0 && StepIndex < Steps.size()) {
            Scp055Step Previous = Steps.get(StepIndex);
            if (Previous.Kind() == Scp055Step.Walk && Waypoint < Previous.Path().size()) {
                Abandoned = true;
            }
        }
        StepIndex = Index;
        Waypoint = 0;
        AnchorYaw = User.getYRot();
        AnchorPitch = User.getXRot();
        StillTicks = 0;
        LastSpot = User.position();
    }

    private static void steerView(LocalPlayer User) {
        int Index = stepAt(Age);
        if (Index != StepIndex) {
            enterStep(User, Index);
        }
        float TargetYaw;
        float TargetPitch;
        float Rate = LookRate;
        float Cap = MaxTurn;
        if (Age < DistortTicks || Index < 0) {
            TargetYaw = StartYaw + SwayYaw * Mth.sin(Age * 0.15f);
            TargetPitch = StartPitch + SwayPitch * Mth.sin(Age * 0.11f);
        } else {
            Scp055Step Step = Steps.get(Index);
            switch (Step.Kind()) {
                case Scp055Step.Walk -> {
                    TargetYaw = User.getYRot();
                    if (!Abandoned && Waypoint < Step.Path().size()) {
                        Vec3 Goal = centre(Step.Path().get(Waypoint));
                        TargetYaw = Scp055Wander.yawTowards(Goal.x - User.getX(), Goal.z - User.getZ());
                    }
                    TargetPitch = WalkPitch + SwayPitch * Mth.sin(Age * 0.2f);
                    Rate = WalkRate;
                    Cap = WalkTurn;
                }
                case Scp055Step.Look -> {
                    TargetYaw = Step.Yaw();
                    TargetPitch = Step.Pitch();
                }
                case Scp055Step.Fiddle -> {
                    TargetYaw = AnchorYaw;
                    TargetPitch = Step.Pitch();
                }
                default -> {
                    TargetYaw = AnchorYaw + SwayYaw * Mth.sin(Age * 0.09f);
                    TargetPitch = AnchorPitch * 0.5f + SwayPitch * Mth.sin(Age * 0.12f);
                }
            }
        }
        float Yaw = User.getYRot();
        float Pitch = User.getXRot();
        float TurnYaw = Mth.clamp(Mth.wrapDegrees(TargetYaw - Yaw) * Rate, -Cap, Cap);
        float TurnPitch = Mth.clamp((TargetPitch - Pitch) * Rate, -Cap, Cap);
        User.setYRot(Yaw + TurnYaw);
        User.setXRot(Mth.clamp(Pitch + TurnPitch, -89.0f, 89.0f));
    }

    public static void steer(Player Mover, Input Controls) {
        if (!controlling() || !(Mover instanceof LocalPlayer User)) {
            return;
        }
        Controls.leftImpulse = 0.0f;
        Controls.forwardImpulse = 0.0f;
        Controls.up = false;
        Controls.down = false;
        Controls.left = false;
        Controls.right = false;
        Controls.jumping = false;
        Controls.shiftKeyDown = false;
        User.setSprinting(false);
        if (Abandoned || Age < DistortTicks || Age >= DistortTicks + WalkTicks || StepIndex < 0 || StepIndex >= Steps.size()) {
            return;
        }
        Scp055Step Step = Steps.get(StepIndex);
        if (Step.Kind() != Scp055Step.Walk) {
            return;
        }
        List<BlockPos> Path = Step.Path();
        while (Waypoint < Path.size()) {
            Vec3 Goal = centre(Path.get(Waypoint));
            double Dx = Goal.x - User.getX();
            double Dz = Goal.z - User.getZ();
            if (Dx * Dx + Dz * Dz < ArriveRadius * ArriveRadius && Math.abs(Goal.y - User.getY()) < 1.2) {
                Waypoint++;
            } else {
                break;
            }
        }
        if (Waypoint >= Path.size()) {
            return;
        }
        Vec3 Goal = centre(Path.get(Waypoint));
        double Dx = Goal.x - User.getX();
        double Dz = Goal.z - User.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (!Scp055Wander.safeAhead(User.level(), User, Dx, Dz)) {
            Abandoned = true;
            return;
        }
        Vec3 Spot = User.position();
        StillTicks = Spot.distanceToSqr(LastSpot) < StillDistance * StillDistance ? StillTicks + 1 : 0;
        LastSpot = Spot;
        if (StillTicks > StuckTicks) {
            Abandoned = true;
            return;
        }
        boolean LastLeg = Waypoint == Path.size() - 1;
        float Speed = ImpulseSpeed * (LastLeg ? (float) Mth.clamp(Distance / SlowRadius, LastLegFloor, 1.0) : 1.0f);
        float Radians = User.getYRot() * Mth.DEG_TO_RAD;
        double Sin = Mth.sin(Radians);
        double Cos = Mth.cos(Radians);
        double Forward = -Dx * Sin + Dz * Cos;
        double Left = Dx * Cos + Dz * Sin;
        double Norm = Math.max(1.0E-4, Math.sqrt(Forward * Forward + Left * Left));
        Controls.forwardImpulse = (float) (Forward / Norm * Speed);
        Controls.leftImpulse = (float) (Left / Norm * Speed);
        if (Goal.y > User.getY() + 0.5 && User.onGround() && Distance < ClimbReach) {
            Controls.jumping = true;
        }
    }

    private static Vec3 centre(BlockPos Pos) {
        return new Vec3(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5);
    }

    public static float quantize(float Value, int Count) {
        return Mth.clamp(Mth.floor(Value * Count + 1.0E-4f) / (float) Count, 0.0f, 1.0f);
    }

    public static float pulse(int Tick) {
        int Index = Math.floorMod(Tick * PulseLevels.length / PulsePeriod, PulseLevels.length);
        return PulseLevels[Index];
    }

    public static float distortion(float Partial) {
        if (!Active || WakeAge >= 0) {
            return 0.0f;
        }
        if (Age < DistortTicks) {
            return quantize((Age + 1) / (float) DistortTicks, RampSteps);
        }
        if (Age < DistortTicks + WalkTicks) {
            return WalkLevel + WalkPulse * pulse(Age);
        }
        return 1.0f;
    }

    public static float staticLevel(float Partial) {
        if (!Active) {
            return 0.0f;
        }
        if (WakeAge >= 0) {
            float Start = Interrupted ? InterruptStatic : 1.0f;
            float Left = 1.0f - (Age - WakeAge) / (float) fadeLength();
            return quantize(Start * Mth.clamp(Left, 0.0f, 1.0f), StaticSteps);
        }
        int StaticStart = DistortTicks + WalkTicks;
        if (Age >= StaticStart) {
            return quantize(Math.min(1.0f, (Age - StaticStart + 1) / (StaticTicks * StaticFullAt)), StaticSteps);
        }
        for (int Flicker : Flickers) {
            if (Age >= Flicker && Age < Flicker + FlickerTicks) {
                return FlickerLevel;
            }
        }
        return 0.0f;
    }

    public static float roll(float Partial) {
        float Level = distortion(Partial);
        return Level <= 0.0f ? 0.0f : RollDegrees * Level * Mth.sin((Age + Partial) * 0.19f);
    }

    public static float fovScale(float Partial) {
        float Level = distortion(Partial);
        return Level <= 0.0f ? 1.0f : 1.0f + FovPulse * Level * Mth.sin((Age + Partial) * 0.31f);
    }

    private static void play(SoundEvent Event, float Pitch, float Volume, boolean Tracked) {
        SoundInstance Instance = new SimpleSoundInstance(Event.getLocation(), SoundSource.PLAYERS, Volume, Pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        Minecraft.getInstance().getSoundManager().play(Instance);
        if (Tracked) {
            Playing.add(Instance);
        }
    }

    private static void stopSounds() {
        for (SoundInstance Instance : Playing) {
            Minecraft.getInstance().getSoundManager().stop(Instance);
        }
        Playing.clear();
    }
}
