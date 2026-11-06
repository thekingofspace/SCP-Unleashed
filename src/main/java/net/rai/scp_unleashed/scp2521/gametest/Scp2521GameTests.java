package net.rai.scp_unleashed.scp2521.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.SealedDocumentItem;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DossierContent;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.scp2521.Scp2521;
import net.rai.scp_unleashed.scp2521.Scp2521Containment;
import net.rai.scp_unleashed.scp2521.Scp2521Idle;
import net.rai.scp_unleashed.scp2521.Scp2521Name;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import net.rai.scp_unleashed.scp2521.Scp2521Rules;
import net.rai.scp_unleashed.scp2521.Scp2521Scene;
import net.rai.scp_unleashed.scp2521.Scp2521State;
import net.rai.scp_unleashed.scp2521.Scp2521Tendrils;
import net.rai.scp_unleashed.scp2521.Scp2521Trip;
import net.rai.scp_unleashed.scp2521.Scp2521Trips;
import net.rai.scp_unleashed.scp2521.Scp2521Void;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp2521GameTests {
    public static final String Arena = "scp2521_arena";
    public static final int Crowd = 12;
    public static final double TrackingReach = 512.0;

    private Scp2521GameTests() {
    }

    public static void fresh(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        if (Server.getLevel(Scp2521Registry.Void) == null) {
            Scp2521Void.standIn(Level.END);
        }
        Scp2521State.get(Server).resetAll();
        Scp2521Trips.forgetAll();
        Scp2521Containment.forgetAll();
    }

    private static Scp2521TestPlayer player(GameTestHelper Helper, double X, double Z, String Name, boolean Reader) {
        ServerLevel World = Helper.getLevel();
        Scp2521TestPlayer User = new Scp2521TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 37.0f, 12.0f);
        User.setYHeadRot(37.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        if (Reader) {
            Scp2521State.get(World.getServer()).learn(User.getUUID());
        }
        return User;
    }

    private static void dismiss(ServerPlayer User) {
        Scp2521Trips.abort(User, false);
        if (User instanceof Scp2521TestPlayer Tester) {
            Tester.enlist(false);
        }
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void skipTo(ServerPlayer User, int Ticks) {
        Scp2521Trips.Run Each = Scp2521Trips.run(User.getUUID());
        if (Each != null) {
            Each.Ticks = Ticks;
        }
    }

    private static Scp2521 starerOf(ServerPlayer User) {
        Scp2521Trips.Run Each = Scp2521Trips.run(User.getUUID());
        if (Each == null) {
            return null;
        }
        for (Entity Made : Each.Spawned) {
            if (Made instanceof Scp2521 Creature && Creature.role() == Scp2521.RoleStarer && !Creature.isRemoved()) {
                return Creature;
            }
        }
        return null;
    }

    private static boolean staring(ServerPlayer User) {
        Scp2521 Starer = starerOf(User);
        Scp2521Trips.Run Each = Scp2521Trips.run(User.getUUID());
        return Starer != null && Each != null && Starer.level() == User.level() && Starer.position().distanceTo(Scp2521Void.starer(Each.Plot)) < 0.1;
    }

    private static int creatures(ServerLevel World, Vec3 Around, double Range, int Role) {
        return World.getEntitiesOfClass(Scp2521.class, new AABB(Around, Around).inflate(Range), Found -> Found.role() == Role && !Found.isRemoved()).size();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp2521_names")
    public static void namesMatchTheTable(GameTestHelper Helper) {
        for (String Text : Scp2521Name.Positives) {
            Helper.assertTrue(Scp2521Name.matches(Text), "should match: " + Text);
        }
        for (String Text : Scp2521Name.Negatives) {
            Helper.assertFalse(Scp2521Name.matches(Text), "should not match: " + Text);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2521_readers")
    public static void onlyReadersAreTaken(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Stranger = player(Helper, 4.5, 4.5, "Stranger", false);
        Scp2521TestPlayer Reader = player(Helper, 11.5, 11.5, "Reader", true);
        Helper.assertTrue(Scp2521Rules.heard(Stranger, "I said 2521 twice") == Scp2521Rules.Outcome.Unknown, "an unread speaker was answered");
        Helper.assertFalse(Scp2521Trips.active(Stranger.getUUID()), "an unread speaker was taken");
        Helper.assertTrue(creatures(Helper.getLevel(), Stranger.position(), 12.0, Scp2521.RoleTaker) == 0, "something rose for an unread speaker");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "nothing here") == Scp2521Rules.Outcome.Ignored, "plain speech was treated as the name");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "two-five-two-one") == Scp2521Rules.Outcome.Taken, "a reader speaking the name was not taken");
        Helper.assertTrue(Scp2521Trips.active(Reader.getUUID()), "no trip for the reader");
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(creatures(Helper.getLevel(), Reader.position(), 8.0, Scp2521.RoleTaker) == 1, "the taker did not rise near the reader");
            Helper.assertTrue(Scp2521Trips.locked(Reader), "the reader is not restrained");
            Helper.assertTrue(Reader.getData(Scp2521Registry.TripAttachment).active(), "the trip was not written to the player");
            dismiss(Reader);
            dismiss(Stranger);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp2521_return")
    public static void tripReturnsToTheExactSpot(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 8.37, 7.61, "Returner", true);
        Vec3 Spot = Reader.position();
        float Yaw = Reader.getYRot();
        float Pitch = Reader.getXRot();
        Helper.assertTrue(Scp2521Trips.start(Reader), "the trip did not start");
        Helper.runAfterDelay(Scp2521Scene.BindAt + 2, () -> {
            Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Scp2521Tendrils.class, new AABB(Spot, Spot).inflate(3.0)).size() == 1, "no tendrils bound the reader");
            Helper.assertTrue(Reader.position().distanceTo(Spot) < 0.1, "the reader moved while restrained");
            skipTo(Reader, Scp2521Scene.VoidAt - 1);
        });
        Helper.runAfterDelay(Scp2521Scene.BindAt + 5, () -> {
            Scp2521Trips.Run Each = Scp2521Trips.run(Reader.getUUID());
            Helper.assertTrue(Each != null && Scp2521Void.inside(Reader), "the reader did not reach the void");
            Helper.assertTrue(Reader.position().distanceTo(Scp2521Void.arrival(Each.Plot)) < 0.6, "the reader is not on their plot");
            Helper.assertTrue(staring(Reader), "nothing stares in the void");
            Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Scp2521Tendrils.class, new AABB(Spot, Spot).inflate(4.0)).isEmpty(), "tendrils were left behind");
            skipTo(Reader, Scp2521Scene.FallAt - 1);
        });
        Helper.runAfterDelay(Scp2521Scene.BindAt + 8, () -> {
            Scp2521Trips.Run Each = Scp2521Trips.run(Reader.getUUID());
            Helper.assertTrue(Each != null && Each.Stage == Scp2521Trips.StageFall, "the ground did not give way");
            Helper.assertFalse(Scp2521Void.built(Reader.serverLevel(), Each.Plot), "the void floor is still there");
            skipTo(Reader, Scp2521Scene.HomeAt - 1);
        });
        Helper.runAfterDelay(Scp2521Scene.BindAt + 11, () -> {
            Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the trip did not end");
            Helper.assertTrue(Reader.level() == Helper.getLevel(), "the reader came back to the wrong dimension");
            Helper.assertTrue(Reader.position().distanceTo(Spot) < 1.0e-3, "the reader came back to " + Reader.position() + " not " + Spot);
            Helper.assertTrue(Math.abs(Reader.getYRot() - Yaw) < 1.0e-3 && Math.abs(Reader.getXRot() - Pitch) < 1.0e-3, "the reader came back facing elsewhere");
            Helper.assertFalse(Scp2521Trips.locked(Reader), "the reader is still restrained");
            Helper.assertFalse(Reader.getData(Scp2521Registry.TripAttachment).active(), "the trip record was not cleared");
            Helper.assertTrue(Reader.fallDistance == 0.0f, "the reader kept fall distance");
            Helper.assertTrue(Scp2521State.get(Reader.server).plots().isEmpty(), "the plot was not freed");
            dismiss(Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp2521_crowd")
    public static void manyTripsGetPrivatePlots(GameTestHelper Helper) {
        fresh(Helper);
        List<Scp2521TestPlayer> Readers = new ArrayList<>();
        for (int Index = 0; Index < Crowd; Index++) {
            Scp2521TestPlayer User = player(Helper, 1.5 + (Index % 4) * 4.0, 1.5 + (Index / 4) * 4.0, "Crowd" + Index, true);
            Helper.assertTrue(Scp2521Rules.spoken(User) == Scp2521Rules.Outcome.Taken, "crowd member " + Index + " was not taken");
            Readers.add(User);
        }
        Set<Integer> Plots = new HashSet<>();
        for (Scp2521TestPlayer User : Readers) {
            Plots.add(Scp2521Trips.run(User.getUUID()).Plot);
            skipTo(User, Scp2521Scene.VoidAt - 1);
        }
        Helper.assertTrue(Plots.size() == Crowd, "plots were shared: " + Plots);
        Helper.succeedWhen(() -> {
            for (int First = 0; First < Readers.size(); First++) {
                ServerPlayer A = Readers.get(First);
                Helper.assertTrue(Scp2521Void.inside(A), "a crowd member is not in the void");
                Helper.assertTrue(staring(A), "plot " + Scp2521Trips.run(A.getUUID()).Plot + " has no starer of its own");
                for (int Second = First + 1; Second < Readers.size(); Second++) {
                    ServerPlayer B = Readers.get(Second);
                    Helper.assertTrue(A.position().distanceTo(B.position()) > TrackingReach, "two plots are within tracking reach");
                }
            }
            for (Scp2521TestPlayer User : Readers) {
                dismiss(User);
            }
            Helper.assertTrue(Scp2521State.get(Helper.getLevel().getServer()).plots().isEmpty(), "plots were not freed");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2521_reuse")
    public static void plotsAreReused(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer First = player(Helper, 4.5, 4.5, "First", true);
        Scp2521TestPlayer Second = player(Helper, 11.5, 11.5, "Second", true);
        Helper.assertTrue(Scp2521Trips.start(First), "the first trip did not start");
        int Plot = Scp2521Trips.run(First.getUUID()).Plot;
        Helper.assertTrue(Scp2521Trips.start(Second), "the second trip did not start");
        Helper.assertTrue(Scp2521Trips.run(Second.getUUID()).Plot != Plot, "two live trips shared a plot");
        Helper.assertTrue(Scp2521Trips.abort(First, true), "the first trip did not end");
        Helper.assertTrue(Scp2521Trips.abort(Second, true), "the second trip did not end");
        Scp2521Trips.uncool(First.getUUID());
        Helper.assertTrue(Scp2521Trips.start(First), "the trip did not start again");
        Helper.assertTrue(Scp2521Trips.run(First.getUUID()).Plot == Plot, "the freed plot was not reused");
        dismiss(First);
        dismiss(Second);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp2521_cooldown")
    public static void cooldownHolds(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 8.5, 8.5, "Cooler", true);
        Helper.assertTrue(Scp2521Rules.heard(Reader, "2521") == Scp2521Rules.Outcome.Taken, "the first call did not take the reader");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "2521") == Scp2521Rules.Outcome.Busy, "a second call during the trip was not ignored");
        Scp2521Trips.abort(Reader, true);
        Helper.assertTrue(Scp2521Trips.cooling(Reader), "no cooldown after the trip");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "scp-2521") == Scp2521Rules.Outcome.Cooling, "the cooldown did not hold");
        Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "a trip started during the cooldown");
        Scp2521Trips.uncool(Reader.getUUID());
        Helper.assertTrue(Scp2521Rules.heard(Reader, "scp-2521") == Scp2521Rules.Outcome.Taken, "the reader was not taken after the cooldown");
        dismiss(Reader);
        Helper.succeed();
    }

    private static Scp2521 containAt(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Blocks.SAND);
        BlockPos Absolute = Helper.absolutePos(Relative);
        Scp2521 Creature = Scp2521Containment.contain(Helper.getLevel().getServer(), new Scp2521Containment.Anchor(Helper.getLevel().dimension(), Absolute), null);
        if (Creature == null) {
            throw new IllegalStateException("could not contain at " + Absolute);
        }
        return Creature;
    }

    private static String containedProblem(Scp2521 Creature, BlockPos Anchor, ServerPlayer Reader) {
        Vec3 Base = Vec3.atBottomCenterOf(Anchor);
        double Flat = Math.sqrt(Math.pow(Creature.getX() - Base.x, 2) + Math.pow(Creature.getZ() - Base.z, 2));
        if (Flat > Scp2521Idle.Radius + 1.0e-6) {
            return "it wandered " + Flat + " blocks from its anchor";
        }
        if (Math.abs(Creature.getY() - Base.y) > 1.0e-6) {
            return "it left its floor: y " + Creature.getY() + " against " + Base.y;
        }
        if (Scp2521Trips.active(Reader.getUUID())) {
            return "it took the reader who was only watching";
        }
        if (!Scp2521Containment.held(Reader.server) || Creature.isRemoved() || Creature.mode() == Scp2521.ModeBreak) {
            return "it broke containment on its own";
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 520, batch = "scp2521_contained_idle")
    public static void containedIdles(GameTestHelper Helper) {
        fresh(Helper);
        BlockPos Relative = new BlockPos(8, 2, 8);
        Scp2521 Creature = containAt(Helper, Relative);
        BlockPos Anchor = Helper.absolutePos(Relative);
        Vec3 Base = Vec3.atBottomCenterOf(Anchor);
        Scp2521TestPlayer Reader = player(Helper, 8.5, 3.5, "Watcher", true);
        Reader.setPos(Reader.getX(), Reader.getY() + 1.0, Reader.getZ());
        double[] Farthest = {0.0};
        float[] Gazed = {0.0f};
        float[] Facing = {180.0f};
        String[] Problem = {null};
        Helper.onEachTick(() -> {
            if (Problem[0] != null || Creature.isRemoved()) {
                return;
            }
            Reader.lookAt(EntityAnchorArgument.Anchor.EYES, Creature.position().add(0.0, Creature.getBbHeight() * 0.6, 0.0));
            Problem[0] = containedProblem(Creature, Anchor, Reader);
            Farthest[0] = Math.max(Farthest[0], Math.sqrt(Math.pow(Creature.getX() - Base.x, 2) + Math.pow(Creature.getZ() - Base.z, 2)));
            Gazed[0] = Math.max(Gazed[0], Creature.gaze());
            if (Farthest[0] > 0.4 && Creature.idle().state() == Scp2521Idle.StateRest) {
                float Toward = (float) (Math.atan2(Reader.getZ() - Creature.getZ(), Reader.getX() - Creature.getX()) * 180.0 / Math.PI) - 90.0f;
                Facing[0] = Math.min(Facing[0], Math.abs(Mth.wrapDegrees(Creature.getYRot() - Toward)));
            }
        });
        Helper.runAfterDelay(Scp2521.HoldingTicks + Scp2521Idle.Settle + 260, () -> {
            try {
                Helper.assertTrue(Problem[0] == null, String.valueOf(Problem[0]));
                Helper.assertTrue(Creature.mode() == Scp2521.ModeContained, "it is not in its contained idle");
                Helper.assertTrue(Farthest[0] > 0.4, "it never paced: farthest " + Farthest[0]);
                Helper.assertTrue(Gazed[0] > 0.6f, "a stare did not draw its attention: gaze " + Gazed[0]);
                Helper.assertTrue(Facing[0] < 15.0f, "it never turned to watch the reader between paces: " + Facing[0] + " degrees off at best");
                Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "watching it triggered Event A");
                Helper.assertTrue(Scp2521Containment.held(Reader.server), "watching it broke containment");
            } finally {
                Scp2521Containment.retire(Reader.server, true);
                dismiss(Reader);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp2521_contained_walls")
    public static void containedPresses(GameTestHelper Helper) {
        fresh(Helper);
        BlockPos Relative = new BlockPos(8, 2, 8);
        for (int Dz = -2; Dz <= 2; Dz++) {
            for (int Dy = 0; Dy <= 3; Dy++) {
                Helper.setBlock(new BlockPos(10, 2 + Dy, 8 + Dz), Blocks.STONE);
            }
        }
        Scp2521 Creature = containAt(Helper, Relative);
        BlockPos Anchor = Helper.absolutePos(Relative);
        Vec3 Base = Vec3.atBottomCenterOf(Anchor);
        Scp2521TestPlayer Reader = player(Helper, 3.5, 3.5, "Keeper", true);
        ServerLevel World = Helper.getLevel();
        float[] Leaned = {0.0f};
        String[] Problem = {null};
        Helper.onEachTick(() -> {
            if (Problem[0] != null || Creature.isRemoved() || Creature.mode() != Scp2521.ModeContained) {
                return;
            }
            Problem[0] = containedProblem(Creature, Anchor, Reader);
            if (Problem[0] == null && Scp2521Idle.walled(World, Creature, new Vec3(Creature.getX(), Base.y, Creature.getZ()))) {
                Problem[0] = "it pushed into the cell wall at " + Creature.position();
            }
            Leaned[0] = Math.max(Leaned[0], Creature.lean());
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.mode() == Scp2521.ModeContained, "not contained yet"))
                .thenExecute(() -> Helper.assertTrue(Creature.idle().press(World, Creature, Base), "it found no wall to press against"))
                .thenWaitUntil(() -> Helper.assertTrue(Creature.lean() > 0.9f, "it has not leaned on the wall yet, offset " + Creature.idle().offset()))
                .thenExecute(() -> Helper.assertTrue(Creature.getX() > Base.x + 0.5, "it pressed toward the wrong side: " + Creature.position()))
                .thenWaitUntil(() -> Helper.assertTrue(Creature.idle().state() != Scp2521Idle.StatePress, "it is still pressing on the wall"))
                .thenExecute(() -> {
                    try {
                        Helper.assertTrue(Problem[0] == null, String.valueOf(Problem[0]));
                        Helper.assertTrue(Math.abs(Creature.getY() - Base.y) < 1.0e-6, "it is not standing on its floor");
                        Helper.assertTrue(Leaned[0] > 0.9f, "it never leaned on the wall");
                        Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "its idling triggered Event A");
                    } finally {
                        Scp2521Containment.retire(Reader.server, true);
                        dismiss(Reader);
                    }
                })
                .thenSucceed();
    }

    private static Set<String> bonesOf(JsonObject Clips, String Name, String Channel) {
        Set<String> Found = new HashSet<>();
        JsonObject Clip = Clips.getAsJsonObject(Name);
        if (Clip == null) {
            return Found;
        }
        for (Map.Entry<String, JsonElement> Bone : Clip.getAsJsonObject("bones").entrySet()) {
            if (Channel == null || Bone.getValue().getAsJsonObject().has(Channel)) {
                Found.add(Bone.getKey());
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp2521_contained_gait")
    public static void containedGait(GameTestHelper Helper) {
        StrideGait Gait = new StrideGait();
        for (int Tick = 0; Tick < 30; Tick++) {
            Gait.tick(Scp2521Idle.PaceSpeed, true, Scp2521.Gait);
        }
        Helper.assertTrue(Gait.stride() == StrideGait.Walk, "pacing speed does not count as a walk: " + Gait.stride());
        double Expected = Scp2521Idle.PaceSpeed / Scp2521.WalkDistance;
        for (int Tick = 0; Tick < 20; Tick++) {
            Gait.tick(Scp2521Idle.PaceSpeed, true, Scp2521.Gait);
            Helper.assertTrue(Math.abs(Gait.to() - Gait.from() - Expected) < 1.0e-4, "the stride cycle does not follow the distance walked: " + (Gait.to() - Gait.from()) + " against " + Expected);
        }
        for (int Tick = 0; Tick < 12; Tick++) {
            Gait.tick(0.0, true, Scp2521.Gait);
        }
        Helper.assertTrue(Gait.stride() == StrideGait.Idle, "it keeps walking after it stops");
        for (int Tick = 0; Tick < 30; Tick++) {
            Gait.tick(0.25, true, Scp2521.Gait);
            Helper.assertTrue(Gait.stride() != StrideGait.Run, "a fast glide switched to a run it has no clip for");
        }
        JsonObject Clips;
        try (InputStream Stream = Scp2521GameTests.class.getResourceAsStream("/assets/scp_unleashed/animations/entity/scp2521/scp2521.animation.json")) {
            Helper.assertTrue(Stream != null, "the SCP-2521 animation file is missing");
            Clips = JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
        Set<String> LegBones = Set.of("thigh_left", "shin_left", "foot_left", "thigh_right", "shin_right", "foot_right");
        Set<String> GaitBones = new HashSet<>(LegBones);
        GaitBones.add("body");
        for (String Name : List.of("walk_legs", "stand_legs")) {
            Helper.assertTrue(bonesOf(Clips, Name, null).equals(GaitBones), Name + " keys the wrong bones: " + bonesOf(Clips, Name, null));
            Helper.assertTrue(bonesOf(Clips, Name, "rotation").equals(LegBones) && bonesOf(Clips, Name, "position").equals(Set.of("body")),
                    Name + " keys the wrong channels");
        }
        Helper.assertTrue(bonesOf(Clips, "walk_upper", null).equals(bonesOf(Clips, "contained", null)), "the walking upper body does not blend with the contained idle");
        for (String Name : Clips.keySet()) {
            if (Name.equals("walk_legs") || Name.equals("stand_legs")) {
                continue;
            }
            for (String Bone : bonesOf(Clips, Name, null)) {
                Helper.assertFalse(LegBones.contains(Bone), Name + " keys " + Bone + " and would fight the walk");
            }
            Helper.assertFalse(bonesOf(Clips, Name, "position").contains("body"), Name + " moves the body and would fight the walk");
        }
        double Seconds = Clips.getAsJsonObject("walk_legs").get("animation_length").getAsDouble();
        double Natural = Scp2521.WalkDistance / (Scp2521Idle.PaceSpeed * 20.0);
        Helper.assertTrue(Math.abs(Seconds - Natural) < 1.0e-3, "the walk clip stride does not match the walk distance: " + Seconds + " s against " + Natural + " s");
        Helper.assertTrue(Math.abs(Clips.getAsJsonObject("walk_upper").get("animation_length").getAsDouble() - Seconds) < 1.0e-6, "the two walk halves differ in length");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp2521_immortal")
    public static void cannotBeKilled(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521 Creature = containAt(Helper, new BlockPos(8, 1, 8));
        MinecraftServer Server = Helper.getLevel().getServer();
        Helper.assertFalse(Creature.hurt(Helper.getLevel().damageSources().genericKill(), 10000.0f), "it took a killing blow");
        Helper.assertFalse(Creature.hurt(Helper.getLevel().damageSources().lava(), 1000.0f), "it took lava damage");
        Helper.assertTrue(StasisPods.isVetoed(Creature), "a stasis pod would accept it");
        Helper.assertFalse(Creature.canBeLeashed(), "it can be leashed");
        Helper.assertFalse(Creature.isPushable(), "it can be pushed");
        Server.getCommands().performPrefixedCommand(Server.createCommandSourceStack().withSuppressedOutput(), "kill @e[type=scp_unleashed:scp2521]");
        Helper.assertFalse(Creature.isRemoved(), "/kill removed it");
        Helper.assertTrue(Creature.mode() == Scp2521.ModeReform, "/kill did not make it sink and reform");
        Helper.setBlock(new BlockPos(8, 1, 8), Blocks.LAVA);
        Helper.runAfterDelay(60, () -> {
            Helper.assertFalse(Creature.isRemoved() || !Creature.isAlive(), "it died in lava");
            Helper.assertTrue(Creature.getHealth() == Creature.getMaxHealth(), "lava hurt it");
            Helper.assertTrue(Scp2521Containment.wanted(Server, Creature), "it is no longer the contained one");
            Helper.setBlock(new BlockPos(8, 1, 8), Blocks.AIR);
            Scp2521Containment.retire(Server, true);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp2521_ritual")
    public static void ritualContainsIt(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 5.5, 8.5, "Keeper", true);
        BlockPos Relative = new BlockPos(8, 1, 8);
        Helper.setBlock(Relative, Blocks.SAND);
        BlockPos Absolute = Helper.absolutePos(Relative);
        ItemStack Document = SealedDocumentItem.create(Scp2521Registry.DocumentId, DocumentClass.Keter);
        Reader.setItemInHand(InteractionHand.MAIN_HAND, Document);
        Reader.gameMode.useItemOn(Reader, Reader.serverLevel(), Reader.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.5, 0.0), Direction.UP, Absolute, false));
        Helper.assertTrue(Scp2521Containment.armed().stream().anyMatch(Found -> Found.Pos().equals(Absolute)), "the sand was not marked");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "●●|●●●●●|●●|●") == Scp2521Rules.Outcome.Contained, "speaking did not contain it");
        Helper.assertTrue(Helper.getLevel().getBlockState(Absolute).isAir(), "the sand was not consumed");
        Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the speaker was taken during the ritual");
        Scp2521State.Holding Held = Scp2521State.get(Reader.server).held();
        Helper.assertTrue(Held != null && Held.Pos().equals(Absolute), "the containment was not recorded at the sand");
        Helper.runAfterDelay(Scp2521.HoldingTicks + 4, () -> {
            Entity Found = Helper.getLevel().getEntity(Held.Creature());
            Helper.assertTrue(Found instanceof Scp2521 Creature && Creature.mode() == Scp2521.ModeContained, "it did not settle into its contained idle");
            Helper.assertTrue(Found.blockPosition().equals(Absolute), "it is not standing where the sand was");
            Helper.assertTrue(Scp2521Rules.heard(Reader, "the weather is nice") == Scp2521Rules.Outcome.Ignored, "plain speech disturbed it");
            Scp2521Containment.retire(Reader.server, true);
            dismiss(Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp2521_breakfree")
    public static void containedBreaksFreeForReaders(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521 Creature = containAt(Helper, new BlockPos(8, 1, 8));
        Scp2521TestPlayer Reader = player(Helper, 3.5, 3.5, "Breaker", true);
        Scp2521TestPlayer Stranger = player(Helper, 12.5, 12.5, "Passer", false);
        MinecraftServer Server = Helper.getLevel().getServer();
        Helper.assertTrue(Scp2521Rules.heard(Reader, "it stays put") == Scp2521Rules.Outcome.Ignored, "plain speech was heard");
        Helper.assertTrue(Scp2521Rules.heard(Stranger, "2521") == Scp2521Rules.Outcome.Unknown, "an unread speaker was answered");
        Helper.assertTrue(Scp2521Containment.held(Server), "containment broke without a reader");
        Helper.assertTrue(Scp2521Rules.heard(Reader, "twenty five twenty one") == Scp2521Rules.Outcome.BrokeFree, "a reader did not break it free");
        Helper.assertFalse(Scp2521Containment.held(Server), "containment survived the name");
        Helper.assertTrue(Creature.mode() == Scp2521.ModeBreak, "the contained one did not break out");
        Helper.assertTrue(Scp2521Trips.active(Reader.getUUID()), "Event A did not run on the speaker");
        Helper.runAfterDelay(Scp2521.BreakTicks + 6, () -> {
            Helper.assertTrue(Creature.isRemoved(), "the broken cell still holds it");
            dismiss(Reader);
            dismiss(Stranger);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp2521_persist")
    public static void stateSurvivesSaveAndLoad(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521 Creature = containAt(Helper, new BlockPos(8, 1, 8));
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp2521State State = Scp2521State.get(Server);
        UUID Someone = UUID.randomUUID();
        State.learn(Someone);
        State.markTaken(Someone);
        int Plot = State.claimPlot(Someone);
        CompoundTag Saved = State.save(new CompoundTag(), Server.registryAccess());
        Scp2521State Loaded = Scp2521State.load(Saved, Server.registryAccess());
        Helper.assertTrue(Loaded.knows(Someone) && Loaded.taken(Someone), "readers did not survive saving");
        Helper.assertTrue(Someone.equals(Loaded.plotOwner(Plot)), "plots did not survive saving");
        Helper.assertTrue(Loaded.held() != null && Loaded.held().Creature().equals(Creature.getUUID()) && Loaded.held().Pos().equals(Creature.anchor()),
                "containment did not survive saving");
        CompoundTag Body = new CompoundTag();
        Creature.saveWithoutId(Body);
        Scp2521 Copy = Scp2521Registry.CreatureType.get().create(Helper.getLevel());
        Copy.load(Body);
        Helper.assertTrue(Copy.contained() && Creature.anchor().equals(Copy.anchor()) && Copy.getUUID().equals(Creature.getUUID()), "the contained body did not reload");
        Helper.assertTrue(Creature.shouldBeSaved(), "the contained one would not be saved");
        Scp2521Trip Trip = new Scp2521Trip(true, Level.NETHER.location(), 1.5, 64.0, -2.25, 33.0f, -12.0f, 3, 1, 150);
        Scp2521Trip Back = Scp2521Trip.TripCodec.parse(net.minecraft.nbt.NbtOps.INSTANCE, Scp2521Trip.TripCodec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, Trip).getOrThrow())
                .getOrThrow();
        Helper.assertTrue(Back.equals(Trip), "the trip record did not survive saving");
        State.freePlot(Plot);
        Scp2521Containment.retire(Server, true);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp2521_dossier")
    public static void dossierIsWordless(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp2521Registry.DocumentId);
        Helper.assertTrue(Document != null, "there is no SCP-2521 document");
        Helper.assertTrue(Document.Classification() == DocumentClass.Keter, "the document is not Keter");
        DossierContent Text = DossierContent.resolve(Document, DossierContent.language(), new DossierContent.Fallbacks("Containment:", "Description:", "Keter"));
        Helper.assertTrue(Text.pictorial(), "the dossier is not all pictures");
        Helper.assertTrue(Text.Sections().size() >= 7, "the dossier has only " + Text.Sections().size() + " pages");
        for (DossierContent.Section Entry : Text.Sections()) {
            Helper.assertTrue(Entry.Heading().isEmpty() && Entry.Text().isBlank() && Entry.Picture().isPresent(), "a dossier page holds text");
            Helper.assertTrue(ResourceLocation.tryParse(Entry.Picture().get()) != null, "a page picture is not a location");
        }
        Helper.assertTrue(Text.Caption().isEmpty(), "the photo has a caption");
        for (String Field : new String[]{Text.Designation(), Text.Title(), Text.Item()}) {
            Helper.assertFalse(Field.codePoints().anyMatch(Character::isLetterOrDigit), "a dossier field holds words: " + Field);
        }
        Helper.assertTrue(Text.ObjectClass().equalsIgnoreCase("Keter"), "the only word is not the class: " + Text.ObjectClass());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp2521_logout")
    public static void loggingOutMidVoidComesHome(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 6.25, 9.75, "Leaver", true);
        Vec3 Spot = Reader.position();
        ServerLevel Home = Helper.getLevel();
        Helper.assertTrue(Scp2521Trips.start(Reader), "the trip did not start");
        skipTo(Reader, Scp2521Scene.VoidAt - 1);
        Helper.runAfterDelay(3, () -> {
            Scp2521Trips.Run Each = Scp2521Trips.run(Reader.getUUID());
            Helper.assertTrue(Each != null && Scp2521Void.inside(Reader), "the reader did not reach the void");
            int Plot = Each.Plot;
            ServerLevel Void = Reader.serverLevel();
            Scp2521 Starer = starerOf(Reader);
            Helper.assertTrue(Starer != null, "nothing stared before the logout");
            Scp2521Trips.leave(Reader);
            Helper.assertTrue(Starer.isRemoved(), "the starer outlived the logout");
            Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the trip kept running after logout");
            Helper.assertTrue(Reader.getData(Scp2521Registry.TripAttachment).active(), "the return point was not kept on the player");
            Helper.assertFalse(Scp2521Void.built(Void, Plot), "the plot floor was left behind");
            Helper.assertTrue(creatures(Void, Scp2521Void.arrival(Plot), 12.0, Scp2521.RoleStarer) == 0, "the starer was left behind");
            Helper.assertTrue(Scp2521State.get(Reader.server).plots().isEmpty(), "the plot was not freed");
            Helper.assertFalse(Scp2521Trips.locked(Reader), "the restraint was not released");
            Helper.assertTrue(Scp2521Trips.loggedIn(Reader), "the rejoin was not handled");
            Helper.assertTrue(Reader.level() == Home, "the reader woke up in the wrong dimension");
            Helper.assertTrue(Reader.position().distanceTo(Spot) < 1.0e-3, "the reader woke up away from their return point");
            Helper.assertFalse(Reader.getData(Scp2521Registry.TripAttachment).active(), "the trip record survived the rejoin");
            Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the cutscene resumed");
            dismiss(Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp2521_restart")
    public static void restartRecoversStrandedPlayers(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 9.5, 5.5, "Stranded", true);
        Vec3 Spot = Reader.position();
        ServerLevel Home = Helper.getLevel();
        MinecraftServer Server = Home.getServer();
        Helper.assertTrue(Scp2521Trips.start(Reader), "the trip did not start");
        skipTo(Reader, Scp2521Scene.VoidAt - 1);
        Helper.runAfterDelay(3, () -> {
            Scp2521Trips.Run Each = Scp2521Trips.run(Reader.getUUID());
            Helper.assertTrue(Each != null && Scp2521Void.inside(Reader), "the reader did not reach the void");
            int Plot = Each.Plot;
            ServerLevel Void = Reader.serverLevel();
            Scp2521 Starer = starerOf(Reader);
            Helper.assertTrue(Starer != null && !Starer.shouldBeSaved(), "the starer would be saved through a restart");
            Scp2521Trips.forgetAll();
            Helper.assertTrue(Scp2521State.get(Server).plots().containsKey(Plot), "the crashed plot was not on record");
            Helper.assertTrue(Scp2521Trips.recover(Server) >= 1, "recovery freed nothing");
            Helper.assertFalse(Scp2521Void.built(Void, Plot), "recovery left the floor");
            Helper.assertTrue(Scp2521Trips.loggedIn(Reader), "the stranded player was not sent home");
            Helper.assertTrue(Reader.level() == Home && Reader.position().distanceTo(Spot) < 1.0e-3, "the stranded player did not wake at their return point");
            Helper.assertFalse(Reader.getData(Scp2521Registry.TripAttachment).active(), "the stale trip record survived");
            Helper.assertFalse(Scp2521Trips.locked(Reader), "the stranded player is still restrained");
            dismiss(Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp2521_cleanup")
    public static void cleanupLeavesNothing(GameTestHelper Helper) {
        fresh(Helper);
        List<Scp2521TestPlayer> Readers = new ArrayList<>();
        List<Vec3> Homes = new ArrayList<>();
        for (int Index = 0; Index < 3; Index++) {
            Scp2521TestPlayer User = player(Helper, 3.5 + Index * 4.0, 8.5, "Tidy" + Index, true);
            Helper.assertTrue(Scp2521Trips.start(User), "trip " + Index + " did not start");
            Readers.add(User);
            Homes.add(User.position());
        }
        Helper.runAfterDelay(Scp2521Scene.BindAt + 2, () -> {
            for (Scp2521TestPlayer User : Readers) {
                skipTo(User, Scp2521Scene.VoidAt - 1);
            }
        });
        List<Integer> Plots = new ArrayList<>();
        List<Entity> Puppets = new ArrayList<>();
        Helper.runAfterDelay(Scp2521Scene.BindAt + 5, () -> {
            for (Scp2521TestPlayer User : Readers) {
                Plots.add(Scp2521Trips.run(User.getUUID()).Plot);
                Puppets.addAll(Scp2521Trips.run(User.getUUID()).Spawned);
                skipTo(User, Scp2521Scene.HomeAt - 1);
            }
        });
        Helper.runAfterDelay(Scp2521Scene.BindAt + 10, () -> {
            ServerLevel Void = Scp2521Void.level(Helper.getLevel().getServer());
            for (int Index = 0; Index < Readers.size(); Index++) {
                Helper.assertFalse(Scp2521Trips.active(Readers.get(Index).getUUID()), "a trip did not finish");
                Helper.assertFalse(Scp2521Void.built(Void, Plots.get(Index)), "a plot floor was left behind");
                List<Entity> Left = Void.getEntitiesOfClass(Entity.class, Scp2521Void.area(Plots.get(Index)), Found -> !(Found instanceof Player));
                Helper.assertTrue(Left.isEmpty(), "entities were left in a plot: " + Left);
                Vec3 Home = Homes.get(Index);
                Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Scp2521.class, new AABB(Home, Home).inflate(8.0)).isEmpty(), "a taker was left behind");
                Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Scp2521Tendrils.class, new AABB(Home, Home).inflate(8.0)).isEmpty(), "tendrils were left behind");
            }
            Helper.assertTrue(Scp2521State.get(Helper.getLevel().getServer()).plots().isEmpty(), "plots stayed claimed");
            Helper.assertTrue(!Puppets.isEmpty() && Puppets.stream().allMatch(Entity::isRemoved), "a starer outlived its trip");
            for (Scp2521TestPlayer User : Readers) {
                dismiss(User);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2521_death")
    public static void deathEndsTheTrip(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 8.5, 8.5, "Fallen", true);
        Helper.assertTrue(Scp2521Trips.start(Reader), "the trip did not start");
        float Health = Reader.getHealth();
        Reader.hurt(Reader.damageSources().generic(), 6.0f);
        Helper.assertTrue(Reader.getHealth() == Health, "ordinary harm reached the reader mid-trip");
        Reader.hurt(Reader.damageSources().genericKill(), Float.MAX_VALUE);
        Helper.assertFalse(Reader.isAlive(), "the reader did not die");
        Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the trip outlived the reader");
        Helper.assertFalse(Reader.getData(Scp2521Registry.TripAttachment).active(), "the trip record outlived the reader");
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(creatures(Helper.getLevel(), Reader.position(), 10.0, Scp2521.RoleTaker) == 0, "the taker outlived the trip");
            dismiss(Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2521_dimension")
    public static void commandTeleportEndsTheTrip(GameTestHelper Helper) {
        fresh(Helper);
        Scp2521TestPlayer Reader = player(Helper, 7.5, 7.5, "Moved", true);
        Vec3 Spot = Reader.position();
        Helper.assertTrue(Scp2521Trips.start(Reader), "the trip did not start");
        ServerLevel Nether = Helper.getLevel().getServer().getLevel(Level.NETHER);
        Helper.assertTrue(Nether != null, "no nether to move to");
        Reader.teleportTo(Nether, 0.5, 120.0, 0.5, 0.0f, 0.0f);
        Helper.runAfterDelay(3, () -> {
            Helper.assertFalse(Scp2521Trips.active(Reader.getUUID()), "the trip kept running after a command move");
            Helper.assertTrue(Reader.level() == Helper.getLevel() && Reader.position().distanceTo(Spot) < 1.0e-3, "the reader was not returned");
            dismiss(Reader);
            Helper.succeed();
        });
    }
}
