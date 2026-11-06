package net.rai.scp_unleashed.scp439.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.motion.gametest.MotionGameTests;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Gaits;
import net.rai.scp_unleashed.scp439.Scp439Queen;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp439QueenGaitGameTests {
    public static final String Arena = "scp439_arena";
    public static final double Floor = 2.0;
    public static final String Animations = "/assets/scp_unleashed/animations/entity/scp439/";
    public static final String Models = "/assets/scp_unleashed/geo/entity/scp439/";
    public static final int FramesPerTick = 3;
    public static final int SimTicks = 400;
    public static final int SimWarmup = 40;
    public static final double MaxBackFramesPerSecond = 6.0;
    public static final double MaxBackStepPixels = 0.5;
    public static final double MaxBlendError = 4.0;
    public static final int TravelTicks = 200;
    public static final int MaxYawZigzags = 2;
    public static final int MaxStepZigzags = 2;
    public static final double ZigzagTurn = 3.0;
    public static final double ZigzagStep = 0.005;
    public static final double MinTravel = 10.0;
    public static final Vec3 TravelFrom = new Vec3(4.5, Floor, 4.5);
    public static final Vec3 TravelTo = new Vec3(27.5, Floor, 27.5);
    public static final Vec3 CrowdAt = new Vec3(16.5, Floor, 16.5);
    public static final StrideGait.Shape QueenShape = new StrideGait.Shape(Scp439Creature.StillPace, Scp439Queen.RunEnter, Scp439Queen.RunExit,
            Scp439Gaits.QueenWalk.distance(), Scp439Gaits.QueenRun.distance(), Scp439Creature.MaxPhaseStep, Scp439Creature.LoopBlend);

    private Scp439QueenGaitGameTests() {
    }

    private record Track(double[] Times, double[][] Values) {
        double[] at(double Time) {
            if (Time <= Times[0]) {
                return Values[0];
            }
            for (int Index = 1; Index < Times.length; Index++) {
                if (Time <= Times[Index]) {
                    double Share = (Time - Times[Index - 1]) / Math.max(1.0E-9, Times[Index] - Times[Index - 1]);
                    double[] Mixed = new double[3];
                    for (int Axis = 0; Axis < 3; Axis++) {
                        Mixed[Axis] = Values[Index - 1][Axis] + (Values[Index][Axis] - Values[Index - 1][Axis]) * Share;
                    }
                    return Mixed;
                }
            }
            return Values[Values.length - 1];
        }
    }

    @Nullable
    private static JsonObject read(String Path) {
        try (InputStream Stream = Scp439QueenGaitGameTests.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException Problem) {
            return null;
        }
    }

    private static double[] vector(JsonElement Value) {
        JsonArray Triple = Value.isJsonArray() ? Value.getAsJsonArray()
                : Value.getAsJsonObject().has("post") ? Value.getAsJsonObject().getAsJsonArray("post") : Value.getAsJsonObject().getAsJsonArray("vector");
        return new double[]{Triple.get(0).getAsDouble(), Triple.get(1).getAsDouble(), Triple.get(2).getAsDouble()};
    }

    private static Track track(JsonObject Frames) {
        List<Map.Entry<String, JsonElement>> Entries = new ArrayList<>(Frames.entrySet());
        Entries.sort((First, Second) -> Double.compare(Double.parseDouble(First.getKey()), Double.parseDouble(Second.getKey())));
        double[] Times = new double[Entries.size()];
        double[][] Values = new double[Entries.size()][];
        for (int Index = 0; Index < Entries.size(); Index++) {
            Times[Index] = Double.parseDouble(Entries.get(Index).getKey());
            Values[Index] = vector(Entries.get(Index).getValue());
        }
        return new Track(Times, Values);
    }

    @Nullable
    private static Track bodyTrack(JsonObject File, String Clip) {
        JsonObject Bones = File.getAsJsonObject("animations").getAsJsonObject(Clip).getAsJsonObject("bones");
        if (!Bones.has("body") || !Bones.getAsJsonObject("body").has("position")) {
            return null;
        }
        return track(Bones.getAsJsonObject("body").getAsJsonObject("position"));
    }

    private static double[] renderedBody(JsonObject File, double Speed, long Seed) {
        Track Walk = bodyTrack(File, Scp439Gaits.QueenWalk.clip());
        Track Run = bodyTrack(File, Scp439Gaits.QueenRun.clip());
        double[] Moves = MotionGameTests.clientMoves(Tick -> Speed, SimTicks, Seed);
        StrideGait Walker = new StrideGait();
        double Before = 0.0;
        double Last = Double.NaN;
        int Back = 0;
        int Counted = 0;
        double Worst = 0.0;
        for (int Tick = 0; Tick < Moves.length; Tick++) {
            double After = Before + Moves[Tick];
            Walker.tick(Moves[Tick], true, QueenShape);
            for (int Frame = 0; Frame < FramesPerTick; Frame++) {
                float Partial = Frame / (float) FramesPerTick;
                double Place = Before + (After - Before) * Partial;
                Track Body = Walker.stride() == StrideGait.Run ? Run : Walker.stride() == StrideGait.Walk ? Walk : null;
                if (Body != null) {
                    Place -= Body.at(Walker.cycle(Partial))[2] / 16.0;
                }
                if (Tick >= SimWarmup && !Double.isNaN(Last)) {
                    Counted++;
                    if (Place < Last - 1.0E-6) {
                        Back++;
                        Worst = Math.max(Worst, (Last - Place) * 16.0);
                    }
                }
                Last = Place;
            }
            Before = After;
        }
        double Seconds = Counted / (20.0 * FramesPerTick);
        return new double[]{Back / Math.max(1.0E-9, Seconds), Worst};
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_queenGait")
    public static void queenBodyHoldsItsCourse(GameTestHelper Helper) {
        JsonObject File = read(Animations + "scp439_queen.animation.json");
        Helper.assertTrue(File != null, "the queen animation could not be read");
        for (double Speed : new double[]{0.051, 0.104, 0.15}) {
            for (long Seed = 0; Seed < 4; Seed++) {
                double[] Result = renderedBody(File, Speed, Seed * 13L + 5L);
                Helper.assertTrue(Result[0] <= MaxBackFramesPerSecond && Result[1] <= MaxBackStepPixels, String.format(
                        "at speed %.3f the rendered queen body slips backwards %.1f frames a second, up to %.2f px", Speed, Result[0], Result[1]));
            }
        }
        Helper.succeed();
    }

    private static double[] matrix(double[] Euler) {
        double X = Math.toRadians(-Euler[0]);
        double Y = Math.toRadians(-Euler[1]);
        double Z = Math.toRadians(Euler[2]);
        double Cx = Math.cos(X);
        double Sx = Math.sin(X);
        double Cy = Math.cos(Y);
        double Sy = Math.sin(Y);
        double Cz = Math.cos(Z);
        double Sz = Math.sin(Z);
        return new double[]{
                Cz * Cy, Cz * Sy * Sx - Sz * Cx, Cz * Sy * Cx + Sz * Sx,
                Sz * Cy, Sz * Sy * Sx + Cz * Cx, Sz * Sy * Cx - Cz * Sx,
                -Sy, Cy * Sx, Cy * Cx};
    }

    private static double[] quaternion(double[] M) {
        double W = Math.sqrt(Math.max(0.0, 1.0 + M[0] + M[4] + M[8])) / 2.0;
        double X = Math.copySign(Math.sqrt(Math.max(0.0, 1.0 + M[0] - M[4] - M[8])) / 2.0, M[7] - M[5]);
        double Y = Math.copySign(Math.sqrt(Math.max(0.0, 1.0 - M[0] + M[4] - M[8])) / 2.0, M[2] - M[6]);
        double Z = Math.copySign(Math.sqrt(Math.max(0.0, 1.0 - M[0] - M[4] + M[8])) / 2.0, M[3] - M[1]);
        return new double[]{W, X, Y, Z};
    }

    private static double turn(double[] A, double[] B) {
        double Dot = Math.abs(A[0] * B[0] + A[1] * B[1] + A[2] * B[2] + A[3] * B[3]);
        double Length = Math.sqrt(A[0] * A[0] + A[1] * A[1] + A[2] * A[2] + A[3] * A[3]) * Math.sqrt(B[0] * B[0] + B[1] * B[1] + B[2] * B[2] + B[3] * B[3]);
        return Math.toDegrees(2.0 * Math.acos(Mth.clamp(Dot / Math.max(1.0E-12, Length), 0.0, 1.0)));
    }

    private static Map<String, double[]> rests(JsonObject Model) {
        Map<String, double[]> Found = new HashMap<>();
        for (JsonElement Bone : Model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject Entry = Bone.getAsJsonObject();
            Found.put(Entry.get("name").getAsString(), Entry.has("rotation") ? vector(Entry.get("rotation")) : new double[3]);
        }
        return Found;
    }

    private static String blendProblem(String Creature, String Clip) {
        JsonObject File = read(Animations + "scp439_" + Creature + ".animation.json");
        JsonObject Model = read(Models + "scp439_" + Creature + ".geo.json");
        if (File == null || Model == null) {
            return Creature + " assets could not be read";
        }
        Map<String, double[]> Rest = rests(Model);
        JsonObject Bones = File.getAsJsonObject("animations").getAsJsonObject(Clip).getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> Bone : Bones.entrySet()) {
            JsonObject Channels = Bone.getValue().getAsJsonObject();
            if (!Channels.has("rotation")) {
                continue;
            }
            double[] Base = Rest.getOrDefault(Bone.getKey(), new double[3]);
            Track Keys = track(Channels.getAsJsonObject("rotation"));
            for (int Index = 1; Index < Keys.Times().length; Index++) {
                double[] First = new double[3];
                double[] Second = new double[3];
                for (int Axis = 0; Axis < 3; Axis++) {
                    First[Axis] = Keys.Values()[Index - 1][Axis] + Base[Axis];
                    Second[Axis] = Keys.Values()[Index][Axis] + Base[Axis];
                }
                double[] From = quaternion(matrix(First));
                double[] To = quaternion(matrix(Second));
                double Sign = From[0] * To[0] + From[1] * To[1] + From[2] * To[2] + From[3] * To[3] < 0.0 ? -1.0 : 1.0;
                for (double Share = 0.25; Share < 1.0; Share += 0.25) {
                    double[] Mixed = new double[3];
                    for (int Axis = 0; Axis < 3; Axis++) {
                        Mixed[Axis] = First[Axis] + (Second[Axis] - First[Axis]) * Share;
                    }
                    double[] Ideal = new double[4];
                    for (int Part = 0; Part < 4; Part++) {
                        Ideal[Part] = From[Part] * (1.0 - Share) + To[Part] * Sign * Share;
                    }
                    double Error = turn(quaternion(matrix(Mixed)), Ideal);
                    if (Error > MaxBlendError) {
                        return String.format("%s %s: %s strays %.1f degrees between its keys at %.4fs", Creature, Clip, Bone.getKey(), Error, Keys.Times()[Index - 1]);
                    }
                }
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_queenGait")
    public static void gaitLegsFollowTheirKeys(GameTestHelper Helper) {
        for (String Creature : new String[]{"queen", "worker", "brute"}) {
            for (String Clip : new String[]{"walk", "run"}) {
                String Problem = blendProblem(Creature, Clip);
                if (Problem != null) {
                    Helper.fail(Problem);
                    return;
                }
            }
        }
        Helper.succeed();
    }

    private static float facing(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static int yawZigzags(List<double[]> Samples) {
        int Found = 0;
        for (int Index = 3; Index < Samples.size(); Index++) {
            double First = Mth.wrapDegrees(Samples.get(Index - 2)[2] - Samples.get(Index - 3)[2]);
            double Second = Mth.wrapDegrees(Samples.get(Index - 1)[2] - Samples.get(Index - 2)[2]);
            double Third = Mth.wrapDegrees(Samples.get(Index)[2] - Samples.get(Index - 1)[2]);
            if (Math.min(Math.abs(First), Math.min(Math.abs(Second), Math.abs(Third))) >= ZigzagTurn && Math.signum(First) != Math.signum(Second)
                    && Math.signum(Second) != Math.signum(Third)) {
                Found++;
            }
        }
        return Found;
    }

    private static int stepZigzags(List<double[]> Samples) {
        int Found = 0;
        for (int Index = 3; Index < Samples.size(); Index++) {
            double[][] Steps = new double[3][];
            boolean Moving = true;
            for (int Part = 0; Part < 3; Part++) {
                double[] Before = Samples.get(Index - 3 + Part);
                double[] After = Samples.get(Index - 2 + Part);
                Steps[Part] = new double[]{After[0] - Before[0], After[1] - Before[1]};
                Moving &= Math.sqrt(Steps[Part][0] * Steps[Part][0] + Steps[Part][1] * Steps[Part][1]) > ZigzagStep;
            }
            if (Moving && Steps[0][0] * Steps[1][0] + Steps[0][1] * Steps[1][1] < 0.0 && Steps[1][0] * Steps[2][0] + Steps[1][1] * Steps[2][1] < 0.0) {
                Found++;
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = TravelTicks + 40, batch = "scp439_queenTravel")
    public static void queenTravelsSteadily(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp439Queen Queen = Scp439Registry.Queen.get().create(World);
        Helper.assertTrue(Queen != null, "the queen could not be made");
        Vec3 Spot = Helper.absoluteVec(TravelFrom);
        Vec3 Goal = Helper.absoluteVec(TravelTo);
        float Yaw = facing(Spot, Goal);
        Queen.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Queen.setYBodyRot(Yaw);
        Queen.setYHeadRot(Yaw);
        Helper.assertTrue(World.addFreshEntity(Queen), "the queen could not be placed");
        Queen.removeFreeWill();
        for (int Index = 0; Index < 6; Index++) {
            double Angle = Index * Math.PI / 3.0;
            Helper.spawnWithNoFreeWill(Scp439Registry.Worker.get(), new Vec3(CrowdAt.x + Math.cos(Angle) * 1.6, Floor, CrowdAt.z + Math.sin(Angle) * 1.6));
        }
        Helper.spawnWithNoFreeWill(Scp439Registry.Brute.get(), new Vec3(CrowdAt.x + 2.5, Floor, CrowdAt.z + 1.0));
        List<double[]> Samples = new ArrayList<>();
        Helper.onEachTick(() -> {
            if (Samples.size() > TravelTicks || Queen.isRemoved()) {
                return;
            }
            if (Queen.getNavigation().isDone() && Queen.distanceToSqr(Goal) > 4.0) {
                Queen.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, 1.0);
            }
            Samples.add(new double[]{Queen.getX(), Queen.getZ(), Queen.getYRot()});
        });
        Helper.runAfterDelay(TravelTicks + 5, () -> {
            double Travelled = 0.0;
            for (int Index = 1; Index < Samples.size(); Index++) {
                Travelled += Math.sqrt(Math.pow(Samples.get(Index)[0] - Samples.get(Index - 1)[0], 2) + Math.pow(Samples.get(Index)[1] - Samples.get(Index - 1)[1], 2));
            }
            int YawZigzags = yawZigzags(Samples);
            int StepZigzags = stepZigzags(Samples);
            Helper.assertTrue(Travelled > MinTravel, String.format("the queen only travelled %.2f blocks", Travelled));
            Helper.assertTrue(YawZigzags <= MaxYawZigzags, String.format("the queen's heading zigzagged tick by tick %d times", YawZigzags));
            Helper.assertTrue(StepZigzags <= MaxStepZigzags, String.format("the queen stepped back and forth tick by tick %d times", StepZigzags));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_queenGait")
    public static void travelMeasureSeesZigzags(GameTestHelper Helper) {
        List<double[]> Turning = new ArrayList<>();
        List<double[]> Shaking = new ArrayList<>();
        List<double[]> Jostled = new ArrayList<>();
        for (int Tick = 0; Tick < 60; Tick++) {
            double Yaw = Tick < 20 ? 0.0 : Tick < 26 ? (Tick - 20) * 15.0 : Tick < 30 ? 90.0 - (Tick - 26) * 10.0 : 50.0;
            Turning.add(new double[]{Tick * 0.1, 0.0, Yaw});
            Shaking.add(new double[]{Tick * 0.1, 0.0, Tick % 2 == 0 ? 10.0 : -10.0});
            Jostled.add(new double[]{Tick % 2 == 0 ? 0.0 : 0.05, Tick * 0.001, 0.0});
        }
        Helper.assertTrue(yawZigzags(Turning) == 0 && stepZigzags(Turning) == 0, "a re-path swing was mistaken for shaking");
        Helper.assertTrue(yawZigzags(Shaking) > MaxYawZigzags, "a tick by tick heading zigzag was not caught");
        Helper.assertTrue(stepZigzags(Jostled) > MaxStepZigzags, "a tick by tick back and forth step was not caught");
        Helper.succeed();
    }
}
