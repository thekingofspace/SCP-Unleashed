package net.rai.scp_unleashed.scp5001.mekhane.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneTimeline;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MekhaneVisibilityGameTests {
    public static final String GeoPath = "/assets/scp_unleashed/geo/entity/mekhane/mekhane.geo.json";
    public static final String AnimationPath = "/assets/scp_unleashed/animations/entity/mekhane/mekhane.animation.json";
    public static final float Step = 0.05f;
    public static final float[] Dives = {Mekhane.DiveLow, -12.0f, 0.0f, 16.0f, Mekhane.DiveHigh};
    public static final float[] Banks = {-Mekhane.BankLimit, -16.0f, 0.0f, 16.0f, Mekhane.BankLimit};
    public static final double FarWatcher = 420.0;
    public static final double BeyondWatcher = Mekhane.TrackChunks * 16.0 + 80.0;

    private record Bone(String Name, String Parent, float[] Pivot, float[] Rotation, float[] Corners) {
    }

    private MekhaneVisibilityGameTests() {
    }

    @GameTest(template = MekhaneGameTests.Arena, timeoutTicks = 200, batch = "scp5001mekhane_cull")
    public static void cullingBoxHoldsEveryPose(GameTestHelper Helper) {
        List<Bone> Bones = bones(Helper);
        JsonObject Clips = json(Helper, AnimationPath).getAsJsonObject("animations");
        JsonObject Gears = Clips.getAsJsonObject("gears");
        float GearLength = Gears.get("animation_length").getAsFloat();
        List<Matrix4f> Placements = new ArrayList<>();
        for (float Dive : Dives) {
            for (float Bank : Banks) {
                Placements.add(placement(Dive, Bank));
            }
        }
        double Radius = 0.0;
        double Low = Double.MAX_VALUE;
        double High = -Double.MAX_VALUE;
        String Tallest = "";
        int Poses = 0;
        Vector3f Point = new Vector3f();
        for (Map.Entry<String, JsonElement> Clip : Clips.entrySet()) {
            if (Clip.getKey().equals("gears")) {
                continue;
            }
            JsonObject Body = Clip.getValue().getAsJsonObject();
            float Length = Body.get("animation_length").getAsFloat();
            for (float Time = 0.0f; Time <= Length + 1.0e-4f; Time += Step) {
                Map<String, Matrix4f> Posed = pose(Bones, Body.getAsJsonObject("bones"), Time, Gears.getAsJsonObject("bones"), Time % GearLength);
                Poses++;
                for (Matrix4f Placement : Placements) {
                    for (Bone Each : Bones) {
                        if (Each.Corners().length == 0) {
                            continue;
                        }
                        Matrix4f Full = new Matrix4f(Placement).mul(Posed.get(Each.Name()));
                        float[] Corners = Each.Corners();
                        for (int Index = 0; Index < Corners.length; Index += 3) {
                            Full.transformPosition(Corners[Index], Corners[Index + 1], Corners[Index + 2], Point);
                            Radius = Math.max(Radius, Math.sqrt(Point.x * Point.x + Point.z * Point.z));
                            Low = Math.min(Low, Point.y);
                            if (Point.y > High) {
                                High = Point.y;
                                Tallest = Clip.getKey() + " at " + Time;
                            }
                        }
                    }
                }
            }
        }
        Helper.assertTrue(Poses > 400 && High > 40.0 && Radius > 30.0, "the pose sweep did not read the model: " + Poses + " poses, top " + High + ", radius " + Radius);
        AABB Box = Mekhane.body(0.0, 0.0, 0.0);
        double Half = Math.min(Math.min(Box.maxX, -Box.minX), Math.min(Box.maxZ, -Box.minZ));
        String Extents = String.format(java.util.Locale.ROOT, "model radius %.1f, low %.1f, top %.1f (%s); box half %.1f, %.1f to %.1f", Radius, Low, High, Tallest, Half, Box.minY,
                Box.maxY);
        Helper.assertTrue(Radius <= Half, "the culling box is narrower than the posed model: " + Extents);
        Helper.assertTrue(Low >= Box.minY, "the culling box stops above the posed model's lowest point: " + Extents);
        Helper.assertTrue(High <= Box.maxY, "the culling box stops below the posed model's highest point: " + Extents);
        ServerLevel World = Helper.getLevel();
        Mekhane Giant = new Mekhane(Scp5001MekhaneRegistry.MekhaneType.get(), World);
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(MekhaneGameTests.Middle)).add(0.0, 90.0, 0.0);
        Giant.place(At, 37.0f);
        AABB Culled = Giant.getBoundingBoxForCulling();
        Helper.assertTrue(Culled.contains(At.add(Half - 0.5, Box.maxY - 0.5, 0.0)) && Culled.contains(At.add(0.0, Box.minY + 0.5, -Half + 0.5)),
                "the entity does not use the checked culling box: " + Culled);
        Giant.portal(At.add(120.0, 0.0, 0.0));
        Giant.opening(1.0f);
        Helper.assertTrue(Giant.getBoundingBoxForCulling().contains(At.add(120.0 + 50.0, 0.0, 0.0)), "the portal rays fall outside the culling box");
        Giant.opening(0.0f);
        Helper.assertTrue(!Giant.getBoundingBoxForCulling().contains(At.add(120.0 + 50.0, 0.0, 0.0)), "a closed portal still stretches the culling box");
        Helper.succeed();
    }

    @GameTest(template = MekhaneGameTests.Arena, timeoutTicks = 60, batch = "scp5001mekhane_fresh")
    public static void freshCopyStartsVisible(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(MekhaneGameTests.Middle)).add(0.0, 40.0, 0.0);
        Mekhane Source = new Mekhane(Scp5001MekhaneRegistry.MekhaneType.get(), World);
        Source.place(At, 0.0f);
        int Checked = 0;
        for (int Tick = MekhaneTimeline.DescentStart; Tick < MekhaneTimeline.SummonTicks; Tick += 20) {
            float Alpha = 1.0f - MekhaneTimeline.descent(Tick) * 0.6f;
            Source.drive(MekhaneTimeline.Act.Descend, 0, 0.0f, Alpha);
            check(Helper, World, Source, At, Alpha, "summon " + Tick);
            Checked++;
        }
        for (int Tick = 0; Tick < MekhaneTimeline.DissolveStart; Tick += 40) {
            float Alpha = 1.0f - MekhaneTimeline.dissolve(Tick);
            Source.drive(MekhaneTimeline.act(Tick), MekhaneTimeline.damage(Tick), MekhaneTimeline.charge(Tick), Alpha);
            Source.aim(At.add(30.0, 0.0, 0.0), MekhaneTimeline.laser(Tick));
            check(Helper, World, Source, At, Alpha, "duel " + Tick);
            Checked++;
        }
        Helper.assertTrue(Checked > 80, "too few phase samples: " + Checked);
        Helper.succeed();
    }

    private static void check(GameTestHelper Helper, ServerLevel World, Mekhane Source, Vec3 At, float Alpha, String Moment) {
        Mekhane Copy = new Mekhane(Scp5001MekhaneRegistry.MekhaneType.get(), World);
        Copy.place(At, 0.0f);
        List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> Values = Source.getEntityData().getNonDefaultValues();
        if (Values != null) {
            Copy.getEntityData().assignValues(Values);
        }
        Helper.assertTrue(Alpha > Mekhane.Faded, "the timeline hides the giant at " + Moment);
        for (float Partial : new float[]{0.0f, 0.3f, 0.99f}) {
            float Seen = Copy.alpha(Partial);
            Helper.assertTrue(Math.abs(Seen - Alpha) < 0.01f && Seen > Mekhane.Faded, "a freshly sent giant renders at alpha " + Seen + " instead of " + Alpha + " at " + Moment
                    + " (partial " + Partial + ")");
        }
        Helper.assertTrue(Copy.act() == Source.act() && Math.abs(Copy.laser(0.0f) - Source.laser(1.0f)) < 0.01f && Math.abs(Copy.charge(0.0f) - Source.charge(1.0f)) < 0.01f,
                "a freshly sent giant starts in another state at " + Moment);
    }

    @GameTest(template = MekhaneGameTests.Arena, timeoutTicks = 60, batch = "scp5001mekhane_track")
    public static void farPlayersTrackTheGiant(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(MekhaneGameTests.Middle)).add(0.0, 60.0, 0.0);
        Mekhane Giant = new Mekhane(Scp5001MekhaneRegistry.MekhaneType.get(), World);
        Giant.place(At, 0.0f);
        ArmorStand Control = new ArmorStand(EntityType.ARMOR_STAND, World);
        Control.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        Control.setNoGravity(true);
        World.addFreshEntity(Giant);
        World.addFreshEntity(Control);
        ServerPlayer Watcher = FakePlayerFactory.get(World, new GameProfile(UUID.nameUUIDFromBytes("scp5001_mekhane_watcher".getBytes(StandardCharsets.UTF_8)), "MekhaneWatcher"));
        Helper.runAfterDelay(2, () -> {
            try {
                Watcher.moveTo(At.x + FarWatcher, At.y - 60.0, At.z, 0.0f, 0.0f);
                boolean GiantSeen = seen(World, Giant.getId(), Watcher);
                boolean ControlSeen = seen(World, Control.getId(), Watcher);
                Watcher.moveTo(At.x + BeyondWatcher, At.y - 60.0, At.z, 0.0f, 0.0f);
                boolean GiantBeyond = seen(World, Giant.getId(), Watcher);
                Helper.assertTrue(!ControlSeen, "a vanilla entity at the same spot reaches the watcher too, so the check proves nothing");
                Helper.assertTrue(GiantSeen, "a player " + (int) FarWatcher + " blocks away with a short view distance is never sent the giant");
                Helper.assertTrue(!GiantBeyond, "the giant is sent past its own tracking range");
            } finally {
                Giant.discard();
                Control.discard();
            }
            Helper.succeed();
        });
    }

    private static boolean seen(ServerLevel World, int Id, ServerPlayer Watcher) {
        try {
            ChunkMap Map = World.getChunkSource().chunkMap;
            Field Entities = ChunkMap.class.getDeclaredField("entityMap");
            Entities.setAccessible(true);
            Object Tracked = ((Int2ObjectMap<?>) Entities.get(Map)).get(Id);
            if (Tracked == null) {
                throw new IllegalStateException("entity " + Id + " is not tracked at all");
            }
            Method Update = Tracked.getClass().getDeclaredMethod("updatePlayer", ServerPlayer.class);
            Update.setAccessible(true);
            Update.invoke(Tracked, Watcher);
            Field SeenBy = Tracked.getClass().getDeclaredField("seenBy");
            SeenBy.setAccessible(true);
            return ((Set<?>) SeenBy.get(Tracked)).contains(Watcher.connection);
        } catch (ReflectiveOperationException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static Matrix4f placement(float Dive, float Bank) {
        return new Matrix4f().scale(Mekhane.RenderScale).rotateY((180.0f - 0.0f) * Mth.DEG_TO_RAD).translate(0.0f, Mekhane.TiltPivot, 0.0f).rotateX(-Dive * Mth.DEG_TO_RAD)
                .rotateZ(-Bank * Mth.DEG_TO_RAD).translate(0.0f, -Mekhane.TiltPivot, 0.0f).translate(0.0f, 0.01f, 0.0f);
    }

    private static Map<String, Matrix4f> pose(List<Bone> Bones, JsonObject Body, float Time, JsonObject Gears, float GearTime) {
        Map<String, Matrix4f> Posed = new HashMap<>();
        for (Bone Each : Bones) {
            float[] Turn = sample(Gears, Each.Name(), "rotation", GearTime);
            if (Turn == null) {
                Turn = sample(Body, Each.Name(), "rotation", Time);
            }
            float[] Shift = sample(Body, Each.Name(), "position", Time);
            if (Turn == null) {
                Turn = new float[3];
            }
            if (Shift == null) {
                Shift = new float[3];
            }
            float Px = -Each.Pivot()[0] / 16.0f;
            float Py = Each.Pivot()[1] / 16.0f;
            float Pz = Each.Pivot()[2] / 16.0f;
            Matrix4f Local = new Matrix4f().translate(-Shift[0] / 16.0f, Shift[1] / 16.0f, Shift[2] / 16.0f).translate(Px, Py, Pz)
                    .rotateZ((Each.Rotation()[2] + Turn[2]) * Mth.DEG_TO_RAD).rotateY(-(Each.Rotation()[1] + Turn[1]) * Mth.DEG_TO_RAD)
                    .rotateX(-(Each.Rotation()[0] + Turn[0]) * Mth.DEG_TO_RAD).translate(-Px, -Py, -Pz);
            Matrix4f Parent = Each.Parent() == null ? null : Posed.get(Each.Parent());
            Posed.put(Each.Name(), Parent == null ? Local : new Matrix4f(Parent).mul(Local));
        }
        return Posed;
    }

    private static float[] sample(JsonObject Bones, String Name, String Channel, float Time) {
        if (Bones == null || !Bones.has(Name) || !Bones.getAsJsonObject(Name).has(Channel)) {
            return null;
        }
        JsonElement Track = Bones.getAsJsonObject(Name).get(Channel);
        if (Track.isJsonArray()) {
            return triple(Track);
        }
        float BeforeTime = -Float.MAX_VALUE;
        float AfterTime = Float.MAX_VALUE;
        float[] Before = null;
        float[] After = null;
        for (Map.Entry<String, JsonElement> Key : Track.getAsJsonObject().entrySet()) {
            float When = Float.parseFloat(Key.getKey());
            if (When <= Time && When > BeforeTime) {
                BeforeTime = When;
                Before = triple(Key.getValue());
            }
            if (When >= Time && When < AfterTime) {
                AfterTime = When;
                After = triple(Key.getValue());
            }
        }
        if (Before == null) {
            return After;
        }
        if (After == null || AfterTime <= BeforeTime) {
            return Before;
        }
        float Blend = (Time - BeforeTime) / (AfterTime - BeforeTime);
        return new float[]{Mth.lerp(Blend, Before[0], After[0]), Mth.lerp(Blend, Before[1], After[1]), Mth.lerp(Blend, Before[2], After[2])};
    }

    private static float[] triple(JsonElement Value) {
        if (Value == null) {
            return new float[3];
        }
        JsonArray Items = Value.getAsJsonArray();
        return new float[]{Items.get(0).getAsFloat(), Items.get(1).getAsFloat(), Items.get(2).getAsFloat()};
    }

    private static List<Bone> bones(GameTestHelper Helper) {
        JsonObject Geometry = json(Helper, GeoPath).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        Map<String, Bone> Found = new HashMap<>();
        List<String> Order = new ArrayList<>();
        for (JsonElement Entry : Geometry.getAsJsonArray("bones")) {
            JsonObject Item = Entry.getAsJsonObject();
            List<Float> Corners = new ArrayList<>();
            if (Item.has("cubes")) {
                for (JsonElement CubeEntry : Item.getAsJsonArray("cubes")) {
                    corners(CubeEntry.getAsJsonObject(), Corners);
                }
            }
            float[] Packed = new float[Corners.size()];
            for (int Index = 0; Index < Packed.length; Index++) {
                Packed[Index] = Corners.get(Index);
            }
            String Name = Item.get("name").getAsString();
            Found.put(Name, new Bone(Name, Item.has("parent") ? Item.get("parent").getAsString() : null, Item.has("pivot") ? triple(Item.get("pivot")) : new float[3],
                    Item.has("rotation") ? triple(Item.get("rotation")) : new float[3], Packed));
            Order.add(Name);
        }
        List<Bone> Sorted = new ArrayList<>();
        List<String> Placed = new ArrayList<>();
        while (Sorted.size() < Order.size()) {
            int Before = Sorted.size();
            for (String Name : Order) {
                Bone Each = Found.get(Name);
                if (!Placed.contains(Name) && (Each.Parent() == null || Placed.contains(Each.Parent()))) {
                    Sorted.add(Each);
                    Placed.add(Name);
                }
            }
            Helper.assertTrue(Sorted.size() > Before, "the Mekhane geo has an orphaned bone");
        }
        return Sorted;
    }

    private static void corners(JsonObject Cube, List<Float> Out) {
        float[] Origin = triple(Cube.get("origin"));
        float[] Size = triple(Cube.get("size"));
        float Inflate = Cube.has("inflate") ? Cube.get("inflate").getAsFloat() / 16.0f : 0.0f;
        float Ox = -(Origin[0] + Size[0]) / 16.0f;
        float Oy = Origin[1] / 16.0f;
        float Oz = Origin[2] / 16.0f;
        Matrix4f Turn = new Matrix4f();
        if (Cube.has("rotation")) {
            float[] Rotation = triple(Cube.get("rotation"));
            float[] Pivot = Cube.has("pivot") ? triple(Cube.get("pivot"))
                    : new float[]{Origin[0] + Size[0] / 2.0f, Origin[1] + Size[1] / 2.0f, Origin[2] + Size[2] / 2.0f};
            float Px = -Pivot[0] / 16.0f;
            float Py = Pivot[1] / 16.0f;
            float Pz = Pivot[2] / 16.0f;
            Turn.translate(Px, Py, Pz).rotateZ(Rotation[2] * Mth.DEG_TO_RAD).rotateY(-Rotation[1] * Mth.DEG_TO_RAD).rotateX(-Rotation[0] * Mth.DEG_TO_RAD).translate(-Px, -Py, -Pz);
        }
        Vector3f Point = new Vector3f();
        for (float X : new float[]{Ox - Inflate, Ox + Size[0] / 16.0f + Inflate}) {
            for (float Y : new float[]{Oy - Inflate, Oy + Size[1] / 16.0f + Inflate}) {
                for (float Z : new float[]{Oz - Inflate, Oz + Size[2] / 16.0f + Inflate}) {
                    Turn.transformPosition(X, Y, Z, Point);
                    Out.add(Point.x);
                    Out.add(Point.y);
                    Out.add(Point.z);
                }
            }
        }
    }

    private static JsonObject json(GameTestHelper Helper, String Path) {
        try (InputStream Stream = MekhaneVisibilityGameTests.class.getResourceAsStream(Path)) {
            Helper.assertTrue(Stream != null, "missing " + Path);
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }
}
