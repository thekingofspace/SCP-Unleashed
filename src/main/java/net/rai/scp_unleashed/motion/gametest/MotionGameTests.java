package net.rai.scp_unleashed.motion.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.PaceMeter;
import net.rai.scp_unleashed.motion.Settle;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.scp354.Scp354Gait;
import net.rai.scp_unleashed.scp354.Scp354Lion;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import net.rai.scp_unleashed.scp407.Scp407Gait;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Sprout;
import net.rai.scp_unleashed.scp439.Scp439Gaits;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.IntToDoubleFunction;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MotionGameTests {
    public static final String Arena = "scp439_arena";
    public static final int Ticks = 1200;
    public static final int Warmup = 40;
    public static final int StrideWarmup = 120;
    public static final int Seeds = 8;
    public static final double PacketJitter = 40.0;
    public static final double FrameMillis = 8.0;
    public static final StrideGait.Shape WorkerShape = new StrideGait.Shape(0.008, 0.11, 0.085, Scp439Gaits.WorkerWalk.distance(), Scp439Gaits.WorkerRun.distance(), 0.45, 3);
    public static final StrideGait.Shape BruteShape = new StrideGait.Shape(0.008, 0.12, 0.09, Scp439Gaits.BruteWalk.distance(), Scp439Gaits.BruteRun.distance(), 0.45, 3);

    private MotionGameTests() {
    }

    public static double[] clientMoves(IntToDoubleFunction Speed, int Count, long Seed) {
        Random Rng = new Random(Seed);
        List<double[]> Packets = new ArrayList<>();
        double ServerX = 0.0;
        for (int Tick = 0; Tick < Count + 12; Tick++) {
            ServerX += Speed.applyAsDouble(Tick);
            if (Tick % 3 == 0) {
                Packets.add(new double[]{50.0 * Tick + 1.0 + Rng.nextDouble() * PacketJitter, ServerX});
            }
        }
        double[] Moves = new double[Count];
        double X = 0.0;
        double Target = 0.0;
        int Steps = 0;
        int Next = 0;
        int Done = 0;
        double NextTick = Rng.nextDouble() * 50.0;
        double Time = 0.0;
        while (Done < Count) {
            while (Next < Packets.size() && Packets.get(Next)[0] <= Time) {
                Target = Packets.get(Next)[1];
                Steps = 3;
                Next++;
            }
            while (NextTick <= Time && Done < Count) {
                double Old = X;
                if (Steps > 0) {
                    X += (Target - X) / Steps;
                    Steps--;
                }
                Moves[Done++] = X - Old;
                NextTick += 50.0;
            }
            Time += FrameMillis * (0.8 + Rng.nextDouble() * 0.4);
        }
        return Moves;
    }

    private static int legacyFlips(double[] Moves, StrideGait.Shape Form) {
        double Smooth = 0.0;
        int Still = 0;
        int Gait = StrideGait.Idle;
        int Flips = 0;
        for (int Index = 0; Index < Moves.length; Index++) {
            Smooth = Smooth * 0.4 + Moves[Index] * 0.6;
            Still = Smooth < Form.StillPace() ? Still + 1 : 0;
            int Next;
            if (Still >= 3) {
                Next = StrideGait.Idle;
            } else if (Gait != StrideGait.Run) {
                Next = Smooth > Form.RunEnter() ? StrideGait.Run : StrideGait.Walk;
            } else {
                Next = Smooth < Form.RunExit() ? StrideGait.Walk : StrideGait.Run;
            }
            if (Next != Gait && Index >= StrideWarmup) {
                Flips++;
            }
            Gait = Next;
        }
        return Flips;
    }

    private static String strideRun(double Speed, StrideGait.Shape Form, int Seeds) {
        int Legacy = 0;
        int Zero = 0;
        for (int Seed = 0; Seed < Seeds; Seed++) {
            double[] Moves = clientMoves(Tick -> Speed, Ticks, Seed * 31L + 7L);
            Legacy += legacyFlips(Moves, Form);
            StrideGait Walker = new StrideGait();
            int Settled = StrideGait.Idle;
            for (int Index = 0; Index < Moves.length; Index++) {
                if (Moves[Index] <= 1.0E-9 && Index >= StrideWarmup) {
                    Zero++;
                }
                boolean Stepped = Walker.tick(Moves[Index], true, Form);
                if (Index == StrideWarmup) {
                    Settled = Walker.stride();
                }
                if (Index > StrideWarmup) {
                    if (Walker.stride() != Settled) {
                        return "speed " + Speed + " seed " + Seed + " changed stride at tick " + Index + " from " + Settled + " to " + Walker.stride();
                    }
                    if (!Stepped || Walker.to() - Walker.from() <= 0.0) {
                        return "speed " + Speed + " seed " + Seed + " froze the stride phase at tick " + Index;
                    }
                }
            }
        }
        if (Zero == 0) {
            return "the packet model produced no stalled client ticks at speed " + Speed;
        }
        return Legacy + " legacy flips";
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "motion_logic")
    public static void strideGaitRidesThroughPacketJitter(GameTestHelper Helper) {
        double[][] Cases = {{0.124, 0}, {0.1, 0}, {0.077, 1}, {0.266, 1}, {0.03, 0}};
        int LegacyTotal = 0;
        for (double[] Case : Cases) {
            String Result = strideRun(Case[0], Case[1] == 0 ? WorkerShape : BruteShape, Seeds);
            if (!Result.endsWith("legacy flips")) {
                Helper.fail(Result);
                return;
            }
            LegacyTotal += Integer.parseInt(Result.substring(0, Result.indexOf(' ')));
        }
        Helper.assertTrue(LegacyTotal > 50, "the jitter model should reproduce the old stride flicker, got " + LegacyTotal + " legacy flips");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "motion_logic")
    public static void strideGaitStartsStopsAndSwitchesCleanly(GameTestHelper Helper) {
        double[] Moves = clientMoves(Tick -> Tick < 200 ? 0.124 : Tick < 300 ? 0.0 : Tick < 500 ? 0.06 : 0.25, 700, 99L);
        StrideGait Walker = new StrideGait();
        int StopAt = -1;
        int StartAt = -1;
        int RunAt = -1;
        for (int Index = 0; Index < Moves.length; Index++) {
            int Before = Walker.stride();
            Walker.tick(Moves[Index], true, WorkerShape);
            int After = Walker.stride();
            if (Index >= 200 && StopAt < 0 && After == StrideGait.Idle) {
                StopAt = Index;
            }
            if (Index >= 300 && StartAt < 0 && After == StrideGait.Walk) {
                StartAt = Index;
            }
            if (Index >= 500 && RunAt < 0 && After == StrideGait.Run) {
                RunAt = Index;
                Helper.assertTrue(Before == StrideGait.Walk, "the run should follow the walk");
                Helper.assertTrue(Walker.cycle(1.0f) == 0.0, "a new stride should start from the top of its cycle");
            }
        }
        Helper.assertTrue(StopAt >= 200 && StopAt <= 216, "the stride should settle to idle soon after stopping, got " + StopAt);
        Helper.assertTrue(StartAt >= 300 && StartAt <= 308, "the stride should start soon after moving again, got " + StartAt);
        Helper.assertTrue(RunAt >= 500 && RunAt <= 530, "the run should start once the speed holds, got " + RunAt);
        StrideGait Dying = new StrideGait();
        for (int Index = 0; Index < 30; Index++) {
            Dying.tick(0.12, Index < 20, WorkerShape);
        }
        Helper.assertTrue(Dying.stride() == StrideGait.Idle, "a dead creature should stop striding");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "motion_logic")
    public static void actionClockIgnoresTickDrift(GameTestHelper Helper) {
        for (int Drift : new int[]{-300, -7, 0, 7, 300}) {
            ActionClock Clock = new ActionClock();
            Clock.settle();
            int Server = 5000;
            int Client = 1000;
            int Origin = Server - Client + Drift;
            Clock.begin((byte) 3, "chirp", Client, ActionClock.already(Clock.fresh(), Client, Origin, Server));
            Helper.assertTrue(Clock.elapsed(Client) == 0.0, "a fresh action should start at zero whatever the drift, drift " + Drift);
            Helper.assertTrue(Math.abs(Clock.elapsed(Client + 4.5) - 4.5) < 1.0E-9, "the clip clock should run at client speed");
            Clock.learn("chirp", 10.0);
            Clock.finish();
            Helper.assertTrue("chirp".equals(Clock.clip(Client + 9.0)) && Clock.kind(Client + 9.0) == 3, "the chirp should play to its end after the server stops");
            Helper.assertTrue(Clock.clip(Client + 10.0) == null && Clock.kind(Client + 10.0) == ActionClock.None, "the chirp should release at its end");
        }
        ActionClock Paired = new ActionClock();
        Paired.begin((byte) 2, "hop", 0, ActionClock.already(Paired.fresh(), 0, 120, 112));
        Helper.assertTrue(Math.abs(Paired.elapsed(0) - 8.0) < 1.0E-9, "a creature seen mid action should pick the clip up where the server is");
        ActionClock Unknown = new ActionClock();
        Unknown.settle();
        Unknown.begin((byte) 2, "hop", 0, 0.0);
        Unknown.finish();
        Helper.assertTrue(Unknown.clip(0.5) == null, "without a known length the clip should end with the server");
        ActionClock Swapped = new ActionClock();
        Swapped.settle();
        Swapped.begin((byte) 3, "chirp", 0, 0.0);
        Swapped.learn("chirp", 10.0);
        Swapped.learn("hop", 50.0);
        Swapped.begin((byte) 2, "hop", 4, 0.0);
        Helper.assertTrue("hop".equals(Swapped.clip(5)) && Swapped.elapsed(5) == 1.0, "a new action should replace a running one");
        Helper.assertTrue(ActionClock.catchUp(1.0, 2.0) == 0.0 && ActionClock.catchUp(6.0, 2.0) == 6.0 && ActionClock.catchUp(4.0, 2.0) == 3.0,
                "catch up should rejoin the clip clock after the blend");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "motion_logic")
    public static void settleAndMeterFilterFlicker(GameTestHelper Helper) {
        Settle<Integer> Latch = new Settle<>();
        Helper.assertTrue(!Latch.ready(1, 2, 3) && !Latch.ready(1, 2, 3) && Latch.ready(1, 2, 3), "three steady wishes should switch");
        Latch.clear();
        Helper.assertTrue(!Latch.ready(1, 2, 3) && !Latch.ready(1, 3, 3) && !Latch.ready(1, 2, 3), "changing wishes should restart the count");
        Helper.assertTrue(!Latch.ready(1, 1, 3) && Latch.count() == 0, "wanting the current state should clear the count");
        PaceMeter Meter = new PaceMeter();
        Meter.push(0.3);
        Meter.push(0.0);
        double Mean = Meter.push(0.3);
        Helper.assertTrue(Math.abs(Mean - 0.2) < 1.0E-9, "the meter should average the last three ticks");
        Helper.succeed();
    }

    private static int countRunFlips(Mob Body, Runnable Tick, BooleanSupplier Running, double Speed, long Seed) {
        double[] Moves = clientMoves(Index -> Speed, 600, Seed);
        int Flips = 0;
        boolean Last = false;
        for (int Index = 0; Index < Moves.length; Index++) {
            Body.setOnGround(true);
            Body.xo = Body.getX() - Moves[Index];
            Body.zo = Body.getZ();
            Tick.run();
            boolean Now = Running.getAsBoolean();
            if (Index > Warmup && Now != Last) {
                Flips++;
            }
            Last = Now;
        }
        return Flips;
    }

    @GameTest(template = "scp407_arena", timeoutTicks = 40, batch = "motion_gaits")
    public static void sproutGaitHoldsItsStride(GameTestHelper Helper) {
        Scp407Sprout Body = Helper.spawn(Scp407Registry.Sprout.get(), new Vec3(8.5, 2.0, 8.5));
        Body.setNoAi(true);
        for (double Speed : new double[]{0.085, 0.127, 0.06}) {
            for (long Seed = 0; Seed < 4; Seed++) {
                Scp407Gait Gait = new Scp407Gait(Scp407Sprout.SproutGait);
                int Flips = countRunFlips(Body, () -> Gait.tick(Body, false), () -> Gait.striding() && Gait.running(), Speed, Seed);
                Helper.assertTrue(Flips == 0, "the sprout flipped walk and run " + Flips + " times at speed " + Speed);
            }
        }
        Body.discard();
        Helper.succeed();
    }

    @GameTest(template = "scp354_arena", timeoutTicks = 40, batch = "motion_gaits")
    public static void lionGaitHoldsItsStride(GameTestHelper Helper) {
        Scp354Lion Body = Helper.spawn(Scp354Registry.Lion.get(), new Vec3(4.5, 7.0, 4.5));
        Body.setNoAi(true);
        for (double Speed : new double[]{0.17, 0.12, 0.25}) {
            for (long Seed = 0; Seed < 4; Seed++) {
                Scp354Gait Gait = new Scp354Gait(Scp354Lion.LionGait);
                int Flips = countRunFlips(Body, () -> Gait.tick(Body), () -> Gait.striding() && Gait.running(), Speed, Seed);
                Helper.assertTrue(Flips == 0, "the lion flipped walk and run " + Flips + " times at speed " + Speed);
            }
        }
        Body.discard();
        Helper.succeed();
    }
}
