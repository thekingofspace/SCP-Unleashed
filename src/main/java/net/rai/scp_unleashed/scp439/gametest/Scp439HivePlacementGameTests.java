package net.rai.scp_unleashed.scp439.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.geo.SharedBones;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.client.Scp439HivePose;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp439HivePlacementGameTests {
    public static final String Arena = "scp439_arena";
    public static final String Growths = "/assets/scp_unleashed/scp439/growth/";
    public static final String[][] Hosts = {
            {"humanoid", "/assets/scp_unleashed/geo/entity/player_clone/player_clone.geo.json"},
            {"humanoid", "/assets/scp_unleashed/geo/entity/dclass/dclass.geo.json"},
            {"slim", "/assets/scp_unleashed/geo/entity/player_clone/player_clone_slim.geo.json"},
            {"villager", "/assets/scp_unleashed/geo/entity/scp439/scp439_villager.geo.json"}};
    public static final String[][] Skeleton = {
            {"root", "", "0", "0", "0", "0", "0"},
            {"waist", "root", "0", "12", "0", "0", "0"},
            {"body", "waist", "0", "24", "0", "0", "0"},
            {"head", "body", "0", "24", "0", "0", "0"},
            {"hat", "head", "0", "24", "0", "0", "0"},
            {"lids", "head", "0", "28", "-4", "0", "0"},
            {"pupils", "head", "0", "28", "-4", "0", "0"},
            {"mouth", "head", "0", "26", "-4", "0", "1"},
            {"hair_curly", "head", "0", "32", "0", "1", "0"},
            {"right_arm", "body", "-5", "22", "0", "0", "0"},
            {"left_arm", "body", "5", "22", "0", "0", "0"},
            {"arms", "body", "0", "21", "-1", "0", "0"},
            {"skirt", "body", "0", "12", "0", "0", "0"},
            {"right_leg", "waist", "-1.9", "12", "0", "0", "0"},
            {"left_leg", "waist", "1.9", "12", "0", "0", "0"}};
    public static final float Tolerance = 1.0E-5f;

    private Scp439HivePlacementGameTests() {
    }

    private static Map<String, GeoBone> skeleton() {
        Map<String, GeoBone> Found = new LinkedHashMap<>();
        for (String[] Row : Skeleton) {
            GeoBone Parent = Row[1].isEmpty() ? null : Found.get(Row[1]);
            GeoBone Joint = new GeoBone(Parent, Row[0], false, null, "1".equals(Row[5]), false);
            Joint.updatePivot(Float.parseFloat(Row[2]), Float.parseFloat(Row[3]), Float.parseFloat(Row[4]));
            if ("1".equals(Row[6])) {
                Joint.updateRotation(0.3f, 0.0f, 0.0f);
            }
            Joint.saveInitialSnapshot();
            if (Parent != null) {
                Parent.getChildBones().add(Joint);
            }
            Found.put(Row[0], Joint);
        }
        return Found;
    }

    private static void scramble(Map<String, GeoBone> Bones, long Seed) {
        Random Dice = new Random(Seed);
        for (GeoBone Joint : Bones.values()) {
            Joint.updateRotation(Dice.nextFloat() * 6.0f - 3.0f, Dice.nextFloat() * 6.0f - 3.0f, Dice.nextFloat() * 6.0f - 3.0f);
            Joint.updatePosition(Dice.nextFloat() * 40.0f - 20.0f, Dice.nextFloat() * 40.0f - 20.0f, Dice.nextFloat() * 40.0f - 20.0f);
            Joint.updateScale(Dice.nextFloat() * 2.0f, Dice.nextFloat() * 2.0f, Dice.nextFloat() * 2.0f);
            Joint.setHidden(Dice.nextBoolean());
            Joint.setChildrenHidden(Dice.nextBoolean());
        }
    }

    private static void pose(Map<String, GeoBone> Bones, boolean Rural, float Seconds) {
        SharedBones.rest(Bones.values());
        Scp439HivePose.apply(Bones::get, Rural, Seconds, 0.4f, 0.3f, 0.0f);
    }

    private static float[] state(GeoBone Joint) {
        return new float[]{Joint.getRotX(), Joint.getRotY(), Joint.getRotZ(), Joint.getPosX(), Joint.getPosY(), Joint.getPosZ(),
                Joint.getScaleX(), Joint.getScaleY(), Joint.getScaleZ(), Joint.isHidden() ? 1.0f : 0.0f, Joint.isHidingChildren() ? 1.0f : 0.0f};
    }

    @Nullable
    private static String differs(float[] Expected, float[] Seen) {
        String[] Labels = {"rotX", "rotY", "rotZ", "posX", "posY", "posZ", "scaleX", "scaleY", "scaleZ", "hidden", "childrenHidden"};
        for (int Index = 0; Index < Expected.length; Index++) {
            if (Math.abs(Expected[Index] - Seen[Index]) > Tolerance) {
                return String.format("%s %.4f instead of %.4f", Labels[Index], Seen[Index], Expected[Index]);
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_hivePlacement")
    public static void hivePoseIgnoresLeftoverBoneState(GameTestHelper Helper) {
        for (boolean Rural : new boolean[]{false, true}) {
            for (float Seconds : new float[]{0.0f, 1.4f, 3.0f}) {
                Map<String, GeoBone> Clean = skeleton();
                pose(Clean, Rural, Seconds);
                for (long Seed = 1; Seed <= 6; Seed++) {
                    Map<String, GeoBone> Dirty = skeleton();
                    scramble(Dirty, Seed * 31L + (Rural ? 7L : 0L));
                    pose(Dirty, Rural, Seconds);
                    for (String Name : Clean.keySet()) {
                        String Problem = differs(state(Clean.get(Name)), state(Dirty.get(Name)));
                        Helper.assertTrue(Problem == null, String.format("%s hive at %.1fs: %s keeps state left by another renderer (%s)",
                                Rural ? "villager" : "human", Seconds, Name, Problem));
                    }
                }
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_hivePlacement")
    public static void hivePoseHandsSharedBonesBack(GameTestHelper Helper) {
        for (boolean Rural : new boolean[]{false, true}) {
            Map<String, GeoBone> Bones = skeleton();
            pose(Bones, Rural, 2.2f);
            SharedBones.release(Bones.values());
            for (GeoBone Joint : Bones.values()) {
                BoneSnapshot Rest = Joint.getInitialSnapshot();
                float[] Expected = {Rest.getRotX(), Rest.getRotY(), Rest.getRotZ(), Rest.getOffsetX(), Rest.getOffsetY(), Rest.getOffsetZ(),
                        Rest.getScaleX(), Rest.getScaleY(), Rest.getScaleZ(), SharedBones.hiddenByDefault(Joint) ? 1.0f : 0.0f, 0.0f};
                String Problem = differs(Expected, state(Joint));
                Helper.assertTrue(Problem == null, String.format("%s hive leaves %s posed for the next renderer (%s)", Rural ? "villager" : "human", Joint.getName(), Problem));
                Helper.assertFalse(Joint.hasRotationChanged() || Joint.hasPositionChanged() || Joint.hasScaleChanged(),
                        Joint.getName() + " is still flagged as changed, so GeckoLib would not reset it for the next entity");
            }
        }
        Helper.succeed();
    }

    @Nullable
    private static JsonObject read(String Path) {
        try (InputStream Stream = Scp439HivePlacementGameTests.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException Problem) {
            return null;
        }
    }

    private static float[] triple(JsonArray Values) {
        return new float[]{Values.get(0).getAsFloat(), Values.get(1).getAsFloat(), Values.get(2).getAsFloat()};
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_hivePlacement")
    public static void hiveGrowthSitsOnEveryHostModel(GameTestHelper Helper) {
        for (String[] Pair : Hosts) {
            JsonObject Growth = read(Growths + Pair[0] + ".json");
            JsonObject Host = read(Pair[1]);
            Helper.assertTrue(Growth != null && Host != null, "could not read " + Pair[0] + " growth or " + Pair[1]);
            Map<String, float[]> Pivots = new HashMap<>();
            for (JsonElement Entry : Host.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                JsonObject Joint = Entry.getAsJsonObject();
                Pivots.put(Joint.get("name").getAsString(), Joint.has("pivot") ? triple(Joint.getAsJsonArray("pivot")) : new float[3]);
            }
            String Where = Pair[0] + " growth on " + Pair[1].substring(Pair[1].lastIndexOf('/') + 1);
            Helper.assertTrue(Pivots.containsKey(Growth.getAsJsonObject("jaw").get("part").getAsString()), Where + ": the jaw part has no host bone");
            int Width = Growth.get("width").getAsInt();
            int Height = Growth.get("height").getAsInt();
            int Counted = 0;
            for (Map.Entry<String, JsonElement> Part : Growth.getAsJsonObject("parts").entrySet()) {
                float[] Expected = Pivots.get(Part.getKey());
                Helper.assertTrue(Expected != null, Where + ": " + Part.getKey() + " has no host bone");
                float[] Seen = triple(Part.getValue().getAsJsonObject().getAsJsonArray("pivot"));
                for (int Axis = 0; Axis < 3; Axis++) {
                    Helper.assertTrue(Math.abs(Expected[Axis] - Seen[Axis]) < 1.0E-3f, String.format("%s: %s pivot %.2f %.2f %.2f does not match the host bone at %.2f %.2f %.2f",
                            Where, Part.getKey(), Seen[0], Seen[1], Seen[2], Expected[0], Expected[1], Expected[2]));
                }
                for (JsonElement Entry : Part.getValue().getAsJsonObject().getAsJsonArray("cubes")) {
                    JsonObject Piece = Entry.getAsJsonObject();
                    float[] Size = triple(Piece.getAsJsonArray("size"));
                    JsonArray Corner = Piece.getAsJsonArray("uv");
                    int U = Corner.get(0).getAsInt();
                    int V = Corner.get(1).getAsInt();
                    float Across = 2.0f * (Size[0] + Size[2]);
                    float Down = Size[2] + Size[1];
                    Helper.assertTrue(U >= 0 && V >= 0 && U + Across <= Width && V + Down <= Height, String.format("%s: a %s piece maps outside the %dx%d growth texture",
                            Where, Part.getKey(), Width, Height));
                    Counted++;
                }
            }
            Helper.assertTrue(Counted > 0, Where + ": no growth pieces");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_hivePlacement")
    public static void hiveStageLooksGrownWhenItArrives(GameTestHelper Helper) {
        for (int Stage = 1; Stage <= Scp439Hive.Tiers; Stage++) {
            int Ticks = Math.max(Scp439Hive.StartTicks, (Stage - 1) * Scp439Hive.StageTicks + Scp439Hive.StartTicks);
            float Shown = Scp439Hive.shown((float) Ticks / Scp439Hive.StageTicks);
            Helper.assertTrue(Math.abs(Shown - Stage) < 1.0E-4f, String.format("a nest grown at stage %d still shows %.3f of its growth", Stage, Shown));
        }
        Helper.assertTrue(Scp439Hive.shown(Scp439Hive.Tiers) >= Scp439Hive.Tiers, "a full nest does not show full growth");
        float Before = 0.0f;
        for (int Step = 0; Step <= 5000; Step++) {
            float Raw = Scp439Hive.Tiers * Step / 5000.0f;
            float Shown = Scp439Hive.shown(Raw);
            Helper.assertTrue(Shown + 1.0E-5f >= Before && Shown - Before < 0.02f, String.format("shown growth jumps from %.3f to %.3f at %.3f", Before, Shown, Raw));
            Before = Shown;
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp439_hivePlacement")
    public static void hiveGrowthSheathesTheLegs(GameTestHelper Helper) {
        for (String Name : new String[]{"humanoid", "slim", "villager"}) {
            JsonObject Growth = read(Growths + Name + ".json");
            Helper.assertTrue(Growth != null, "could not read the " + Name + " growth");
            for (String Leg : new String[]{"right_leg", "left_leg"}) {
                int Sheaths = 0;
                for (JsonElement Entry : Growth.getAsJsonObject("parts").getAsJsonObject(Leg).getAsJsonArray("cubes")) {
                    JsonObject Piece = Entry.getAsJsonObject();
                    float[] Size = triple(Piece.getAsJsonArray("size"));
                    Arrays.sort(Size);
                    if (Piece.get("tier").getAsInt() == Scp439Hive.Tiers && Size[1] >= 6.0f) {
                        Sheaths++;
                    }
                }
                Helper.assertTrue(Sheaths >= 3, String.format("the %s growth leaves the %s bare at full growth (%d sheaths)", Name, Leg, Sheaths));
            }
        }
        Helper.succeed();
    }
}
