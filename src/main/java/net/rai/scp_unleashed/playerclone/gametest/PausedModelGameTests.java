package net.rai.scp_unleashed.playerclone.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.geo.SharedBones;
import net.rai.scp_unleashed.scp428.client.Scp428Poses;
import net.rai.scp_unleashed.scp439.client.Scp439HivePose;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class PausedModelGameTests {
    public static final String Arena = "gear_arena";
    public static final String Wide = "/assets/scp_unleashed/geo/entity/player_clone/player_clone.geo.json";
    public static final String Slim = "/assets/scp_unleashed/geo/entity/player_clone/player_clone_slim.geo.json";
    public static final String Plain = "/assets/scp_unleashed/geo/entity/dclass/dclass.geo.json";
    public static final float Tolerance = 1.0E-5f;

    private PausedModelGameTests() {
    }

    private static final class Renderer {
        final String Name;
        final SharedBones Shared = new SharedBones();
        long LastRendered = Long.MIN_VALUE;

        Renderer(String Name) {
            this.Name = Name;
        }
    }

    private static final class Body {
        final String Label;
        final long Id;
        final Renderer Owner;
        final String Geo;
        final float[] Keyframes;
        final Consumer<Map<String, GeoBone>> Custom;
        boolean FirstTick = true;
        double LastUpdate = -1.0;

        Body(String Label, long Id, Renderer Owner, String Geo, float[] Keyframes, Consumer<Map<String, GeoBone>> Custom) {
            this.Label = Label;
            this.Id = Id;
            this.Owner = Owner;
            this.Geo = Geo;
            this.Keyframes = Keyframes;
            this.Custom = Custom;
        }
    }

    @Nullable
    private static Map<String, GeoBone> bake(String Path) {
        try (InputStream Stream = PausedModelGameTests.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            Model Raw = KeyFramesAdapter.GEO_GSON.fromJson(new InputStreamReader(Stream, StandardCharsets.UTF_8), Model.class);
            BakedGeoModel Made = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(Raw));
            Map<String, GeoBone> Bones = new LinkedHashMap<>();
            for (GeoBone Top : Made.topLevelBones()) {
                index(Top, Bones);
            }
            return Bones;
        } catch (IOException Problem) {
            return null;
        }
    }

    private static void index(GeoBone Joint, Map<String, GeoBone> Found) {
        Joint.saveInitialSnapshot();
        Found.put(Joint.getName(), Joint);
        for (GeoBone Child : Joint.getChildBones()) {
            index(Child, Found);
        }
    }

    private static void turn(Map<String, GeoBone> Bones, String Name, float X, float Y, float Z) {
        GeoBone Found = Bones.get(Name);
        if (Found != null) {
            Found.setRotX(X);
            Found.setRotY(Y);
            Found.setRotZ(Z);
        }
    }

    private static void hide(Map<String, GeoBone> Bones, String Name, boolean Hidden) {
        GeoBone Found = Bones.get(Name);
        if (Found != null) {
            Found.setHidden(Hidden);
        }
    }

    private static void person(Map<String, GeoBone> Bones, int Style, float Closed, float LookX, boolean Specimen, float HeadX) {
        for (String Name : new String[]{"waist", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            GeoBone Found = Bones.get(Name);
            if (Found != null) {
                Found.setPosX(0.0f);
                Found.setPosY(0.0f);
                Found.setPosZ(0.0f);
            }
        }
        hide(Bones, "hair_curly", Style != 1);
        hide(Bones, "hair_mohawk", Style != 2);
        hide(Bones, "hair_tail", Style != 3);
        for (String Lid : new String[]{"right_lid", "left_lid"}) {
            GeoBone Found = Bones.get(Lid);
            if (Found != null) {
                Found.setHidden(Closed < 0.05f);
                Found.setScaleY(Math.max(0.05f, Closed));
            }
        }
        for (String Pupil : new String[]{"right_pupil", "left_pupil"}) {
            GeoBone Found = Bones.get(Pupil);
            if (Found != null) {
                Found.setPosX(-LookX * 0.5f);
            }
        }
        hide(Bones, "mouth", true);
        GeoBone Head = Bones.get("head");
        if (!Specimen && Head != null) {
            Head.setRotX(Head.getRotX() + HeadX);
        }
    }

    private static List<Body> scene(boolean WithPeople) {
        Renderer Clones = new Renderer("player clone");
        Renderer People = new Renderer("D-Class");
        Renderer Operatives = new Renderer("MTF operative");
        Renderer Raiders = new Renderer("raider");
        Renderer Figures = new Renderer("SCP-428 figure");
        Renderer Hives = new Renderer("SCP-439 hive");
        float[] Idle = {0.08f, -0.06f, 0.04f};
        float[] Tank = {-0.16f, 0.15f, 0.17f};
        List<Body> Found = new ArrayList<>();
        if (WithPeople) {
            Found.add(new Body("D-Class aiming a gun", 201, People, Plain, Idle, Bones -> {
                person(Bones, 2, 0.0f, 0.6f, false, -0.35f);
                turn(Bones, "right_arm", 1.45f, -0.1f, 0.0f);
                turn(Bones, "left_arm", 1.4f, 0.55f, 0.0f);
            }));
            Found.add(new Body("player clone walking", 301, Clones, Wide, Idle, Bones -> person(Bones, 0, 0.0f, -0.3f, false, 0.25f)));
        }
        Found.add(new Body("MTF operative on a rope", 202, Operatives, Plain, Idle, Bones -> {
            person(Bones, 0, 0.0f, -0.4f, false, 0.2f);
            turn(Bones, "right_arm", 2.95f, 0.05f, -0.16f);
            turn(Bones, "left_arm", 2.62f, -0.05f, 0.16f);
            turn(Bones, "right_leg", 0.42f, 0.0f, 0.06f);
            turn(Bones, "left_leg", 0.12f, 0.0f, -0.08f);
        }));
        Found.add(new Body("raider cradling loot", 203, Raiders, Plain, Idle, Bones -> {
            person(Bones, 3, 0.0f, 0.2f, false, 0.1f);
            turn(Bones, "right_arm", 0.8f, 0.32f, 0.0f);
            turn(Bones, "left_arm", 0.9f, -0.42f, 0.0f);
        }));
        Found.add(new Body("SCP-428 figure", 204, Figures, Wide, Idle, Bones -> {
            person(Bones, 0, 0.0f, 0.0f, false, 0.0f);
            Scp428Poses.pose(Bones::get, new Scp428Poses.Stance(false, 0.0f, false, (byte) 1, 0.0f, 37.5f, 1.3f, 1200.5f, 0.0f));
        }));
        Found.add(new Body("SCP-439 hive on a player", 205, Hives, Wide, null, Bones -> Scp439HivePose.apply(Bones::get, false, 3.4f, 0.0f, 0.0f, 0.0f)));
        Found.add(new Body("SCP-439 hive on a D-Class", 206, Hives, Plain, null, Bones -> {
            Scp439HivePose.apply(Bones::get, false, 1.7f, 0.0f, 0.0f, 0.0f);
            hide(Bones, "hair_curly", true);
            hide(Bones, "hair_tail", true);
            hide(Bones, "mouth", true);
        }));
        Found.add(new Body("slim player clone in the cloning machine", 100, Clones, Slim, Tank, Bones -> person(Bones, 0, 1.0f, 0.0f, true, 0.0f)));
        Found.add(new Body("player clone in the cloning machine", 101, Clones, Wide, Tank, Bones -> person(Bones, 0, 1.0f, 0.0f, true, 0.0f)));
        Found.add(new Body("D-Class clone in the cloning machine", 102, People, Plain, Tank, Bones -> person(Bones, 1, 1.0f, 0.0f, true, 0.0f)));
        return Found;
    }

    private static void keyframes(Map<String, GeoBone> Bones, @Nullable float[] Keyframes) {
        if (Keyframes == null) {
            return;
        }
        String[] Keyed = {"right_arm", "left_arm", "head", "right_leg", "left_leg", "waist"};
        for (int Index = 0; Index < Keyed.length; Index++) {
            GeoBone Found = Bones.get(Keyed[Index]);
            if (Found != null) {
                BoneSnapshot Initial = Found.getInitialSnapshot();
                Found.setRotX(Initial.getRotX() + Keyframes[Index % 3] * (Index + 1));
                Found.setRotZ(Initial.getRotZ() + Keyframes[(Index + 1) % 3]);
            }
        }
    }

    private static boolean geckolib(Body Owner, Map<String, GeoBone> Bones, double FrameTime, boolean Paused) {
        boolean Again = !Owner.FirstTick && FrameTime == Owner.LastUpdate;
        if (Again && Owner.Id == Owner.Owner.LastRendered) {
            return false;
        }
        if (!Paused) {
            Owner.LastUpdate = FrameTime;
        }
        Owner.Owner.LastRendered = Owner.Id;
        keyframes(Bones, Owner.Keyframes);
        for (GeoBone Found : Bones.values()) {
            BoneSnapshot Initial = Found.getInitialSnapshot();
            if (!Found.hasRotationChanged()) {
                Found.setRotX(Initial.getRotX());
                Found.setRotY(Initial.getRotY());
                Found.setRotZ(Initial.getRotZ());
            }
            if (!Found.hasPositionChanged()) {
                Found.setPosX(Initial.getOffsetX());
                Found.setPosY(Initial.getOffsetY());
                Found.setPosZ(Initial.getOffsetZ());
            }
            if (!Found.hasScaleChanged()) {
                Found.setScaleX(Initial.getScaleX());
                Found.setScaleY(Initial.getScaleY());
                Found.setScaleZ(Initial.getScaleZ());
            }
        }
        for (GeoBone Found : Bones.values()) {
            Found.resetStateChanges();
        }
        Owner.FirstTick = false;
        Owner.Custom.accept(Bones);
        return true;
    }

    private static float[] state(Map<String, GeoBone> Bones) {
        float[] Values = new float[Bones.size() * 11];
        int At = 0;
        for (GeoBone Found : Bones.values()) {
            float[] One = {Found.getRotX(), Found.getRotY(), Found.getRotZ(), Found.getPosX(), Found.getPosY(), Found.getPosZ(), Found.getScaleX(), Found.getScaleY(),
                    Found.getScaleZ(), Found.isHidden() ? 1.0f : 0.0f, Found.isHidingChildren() ? 1.0f : 0.0f};
            System.arraycopy(One, 0, Values, At, One.length);
            At += One.length;
        }
        return Values;
    }

    private static float gap(float[] First, float[] Second) {
        float Worst = 0.0f;
        for (int Index = 0; Index < First.length; Index++) {
            Worst = Math.max(Worst, Math.abs(First[Index] - Second[Index]));
        }
        return Worst;
    }

    private static Map<String, Float> play(boolean WithPeople, boolean Guarded, @Nullable List<String> Problems) {
        Map<String, Map<String, GeoBone>> Shared = new LinkedHashMap<>();
        Map<String, float[]> Expected = new LinkedHashMap<>();
        for (Body Owner : scene(WithPeople)) {
            Map<String, GeoBone> Fresh = bake(Owner.Geo);
            Body Alone = new Body(Owner.Label, Owner.Id, new Renderer(Owner.Owner.Name), Owner.Geo, Owner.Keyframes, Owner.Custom);
            geckolib(Alone, Fresh, 250.4, false);
            Expected.put(Owner.Label, state(Fresh));
        }
        Map<String, Float> Worst = new LinkedHashMap<>();
        List<Body> Bodies = scene(WithPeople);
        for (int Frame = 0; Frame < 4; Frame++) {
            double Time = Frame == 0 ? 249.4 : 250.4;
            boolean Paused = Frame >= 2;
            for (Body Owner : Bodies) {
                Map<String, GeoBone> Bones = Shared.computeIfAbsent(Owner.Geo, PausedModelGameTests::bake);
                if (Guarded) {
                    Owner.Owner.Shared.animate(Owner.Id, Bones.values(), () -> geckolib(Owner, Bones, Time, Paused), () -> Owner.Custom.accept(Bones));
                } else {
                    geckolib(Owner, Bones, Time, Paused);
                }
                if (Frame >= 1) {
                    Worst.merge(Owner.Label, gap(Expected.get(Owner.Label), state(Bones)), Math::max);
                }
                if (Guarded) {
                    SharedBones.release(Bones.values());
                    if (Problems != null) {
                        for (GeoBone Found : Bones.values()) {
                            BoneSnapshot Initial = Found.getInitialSnapshot();
                            boolean Clean = Found.getRotX() == Initial.getRotX() && Found.getRotY() == Initial.getRotY() && Found.getRotZ() == Initial.getRotZ()
                                    && Found.getPosX() == Initial.getOffsetX() && Found.getPosY() == Initial.getOffsetY() && Found.getPosZ() == Initial.getOffsetZ()
                                    && Found.getScaleX() == Initial.getScaleX() && Found.getScaleY() == Initial.getScaleY() && Found.getScaleZ() == Initial.getScaleZ()
                                    && Found.isHidden() == SharedBones.hiddenByDefault(Found) && !Found.isHidingChildren()
                                    && !Found.hasRotationChanged() && !Found.hasPositionChanged() && !Found.hasScaleChanged();
                            if (!Clean) {
                                Problems.add(Owner.Owner.Name + " left " + Found.getName() + " posed or hidden after rendering " + Owner.Label);
                            }
                        }
                    }
                }
            }
        }
        return Worst;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "paused_models")
    public static void pausedModelsKeepTheirOwnPose(GameTestHelper Helper) {
        Helper.assertTrue(bake(Wide) != null && bake(Slim) != null && bake(Plain) != null, "the shared humanoid models could not be read");
        for (boolean WithPeople : new boolean[]{true, false}) {
            Map<String, Float> Old = play(WithPeople, false, null);
            Helper.assertTrue(Old.values().stream().anyMatch(Value -> Value > 0.01f), "the unguarded render no longer shows the paused leak, so this test proves nothing");
            Map<String, Float> Fixed = play(WithPeople, true, null);
            for (Map.Entry<String, Float> Entry : Fixed.entrySet()) {
                Helper.assertTrue(Entry.getValue() < Tolerance, String.format("%s renders %.4f off its own pose while the game is paused%s", Entry.getKey(), Entry.getValue(),
                        WithPeople ? " next to other people" : " in a cloning room"));
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "paused_models")
    public static void sharedModelsAreHandedBackClean(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        play(true, true, Problems);
        play(false, true, Problems);
        Helper.assertTrue(Problems.isEmpty(), Problems.isEmpty() ? "" : Problems.get(0) + " (" + Problems.size() + " in all)");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "paused_models")
    public static void skippedFramesStillPose(GameTestHelper Helper) {
        Map<String, GeoBone> Bones = bake(Plain);
        Map<String, GeoBone> Other = bake(Wide);
        Helper.assertTrue(Bones != null && Other != null, "the shared humanoid models could not be read");
        SharedBones Shared = new SharedBones();
        Shared.animate(7L, Bones.values(), () -> false, () -> turn(Bones, "right_arm", 1.2f, 0.0f, 0.0f));
        Helper.assertTrue(Math.abs(Bones.get("right_arm").getRotX() - 1.2f) < Tolerance, "a skipped first frame was left unposed");
        Shared.animate(7L, Bones.values(), () -> {
            turn(Bones, "right_arm", 0.4f, 0.0f, 0.0f);
            hide(Bones, "hair_curly", true);
            return true;
        }, () -> {
        });
        turn(Bones, "right_arm", -2.0f, 1.0f, 0.0f);
        hide(Bones, "hair_curly", false);
        Shared.animate(7L, Bones.values(), () -> false, () -> turn(Bones, "right_arm", 9.0f, 0.0f, 0.0f));
        Helper.assertTrue(Math.abs(Bones.get("right_arm").getRotX() - 0.4f) < Tolerance && Bones.get("hair_curly").isHidden(),
                "a skipped frame did not bring back the pose from the last real frame");
        Shared.animate(7L, Other.values(), () -> false, () -> turn(Other, "right_arm", 0.7f, 0.0f, 0.0f));
        Helper.assertTrue(Math.abs(Other.get("right_arm").getRotX() - 0.7f) < Tolerance, "a pose remembered on one model was pasted onto another");
        Helper.succeed();
    }
}
