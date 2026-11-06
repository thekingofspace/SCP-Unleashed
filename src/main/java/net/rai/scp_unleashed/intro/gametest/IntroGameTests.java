package net.rai.scp_unleashed.intro.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.gateway.gametest.GatewayTraveller;
import net.rai.scp_unleashed.intro.IntroBuilder;
import net.rai.scp_unleashed.intro.IntroDirector;
import net.rai.scp_unleashed.intro.IntroPlots;
import net.rai.scp_unleashed.intro.IntroRecord;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.intro.IntroScientist;
import net.rai.scp_unleashed.intro.IntroSession;
import net.rai.scp_unleashed.intro.IntroSettings;
import net.rai.scp_unleashed.intro.IntroWorld;
import net.rai.scp_unleashed.intro.network.IntroActionPayload;
import net.rai.scp_unleashed.props.OfficeChairBlock;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class IntroGameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final int Long = 400;
    public static final double Close = 1.0E-6;

    private IntroGameTests() {
    }

    private static ServerLevel intro(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        if (Server.getLevel(IntroRegistry.Dimension) == null && IntroWorld.level(Server) == null) {
            IntroWorld.standIn(Level.END);
        }
        ServerLevel Found = IntroWorld.level(Server);
        Helper.assertTrue(Found != null, "no level is available for the intro");
        return Found;
    }

    private static ServerPlayer person(GameTestHelper Helper, String Name) {
        ServerPlayer User = GatewayGameTests.person(Helper, 2.5, 2.5, Name);
        GatewayGameTests.whenDone(Helper, () -> {
            IntroSession Found = IntroDirector.session(User);
            if (Found != null) {
                IntroDirector.loggedOut(User);
            }
            if (!User.isRemoved()) {
                User.serverLevel().removePlayerImmediately(User, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        });
        return User;
    }

    private static IntroSession session(GameTestHelper Helper, ServerPlayer User) {
        IntroSession Found = IntroDirector.session(User);
        Helper.assertTrue(Found != null, "no intro session for " + User.getGameProfile().getName());
        return Found;
    }

    private static void enterOffice(GameTestHelper Helper, ServerPlayer User) {
        intro(Helper);
        IntroDirector.loggedIn(User);
        IntroSession Found = session(Helper, User);
        IntroDirector.ready(User, Found.Token, false);
        Helper.assertTrue(IntroWorld.inside(User), "the ready signal did not move the player into the intro");
        Helper.assertTrue(Found.Stage == IntroSession.Office, "the session did not reach the office");
    }

    private static void sameSpot(GameTestHelper Helper, ServerPlayer User, ResourceKey<Level> Dimension, Vec3 Pos, float Yaw, float Pitch) {
        Helper.assertTrue(User.level().dimension() == Dimension, "returned to the wrong dimension: " + User.level().dimension().location());
        Helper.assertTrue(User.position().distanceToSqr(Pos) < Close, "returned to the wrong place: " + User.position() + " instead of " + Pos);
        Helper.assertTrue(Math.abs(User.getYRot() - Yaw) < 1.0E-3f && Math.abs(User.getXRot() - Pitch) < 1.0E-3f,
                "returned facing the wrong way: " + User.getYRot() + "/" + User.getXRot() + " instead of " + Yaw + "/" + Pitch);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_flag")
    public static void flagOnlyOnFinish(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_flag");
            enterOffice(Helper, User);
            IntroRecord During = IntroDirector.record(User);
            Helper.assertFalse(During.Done(), "the flag was set before the intro finished");
            Helper.assertTrue(During.Started() && During.active(), "the intro did not record its start and return spot");
            IntroSession Found = session(Helper, User);
            IntroDirector.action(User, Found.Token, IntroActionPayload.Skip);
            IntroRecord After = IntroDirector.record(User);
            Helper.assertTrue(After.Done() && !After.active(), "skipping did not set the flag");
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            FakePlayer Loaded = new GatewayTraveller(Helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "intro_flag_loaded"));
            Loaded.load(Saved);
            Helper.assertTrue(IntroDirector.record(Loaded).Done(), "the flag did not survive a save round trip");
            FakePlayer Respawned = new GatewayTraveller(Helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "intro_flag_respawned"));
            Respawned.restoreFrom(User, false);
            Helper.assertTrue(IntroDirector.record(Respawned).Done(), "the flag was lost on death");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "intro_veteran")
    public static void veteransSkipSilently(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_veteran");
            User.getStats().setValue(User, Stats.CUSTOM.get(Stats.PLAY_TIME), IntroSettings.VeteranTicks + 2000);
            IntroDirector.loggedIn(User);
            Helper.assertTrue(IntroDirector.record(User).Done(), "a veteran was not marked done");
            Helper.assertTrue(IntroDirector.session(User) == null, "a veteran was queued for the intro");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "intro_ready")
    public static void waitsForReady(GameTestHelper Helper) {
        ServerPlayer User = person(Helper, "intro_ready");
        intro(Helper);
        IntroDirector.loggedIn(User);
        ResourceKey<Level> Home = User.level().dimension();
        Helper.runAfterDelay(120, () -> GatewayGameTests.guard(Helper, () -> {
            IntroSession Found = session(Helper, User);
            Helper.assertTrue(Found.Stage == IntroSession.Waiting, "the intro advanced without a ready signal");
            Helper.assertTrue(User.level().dimension() == Home && !IntroWorld.inside(User), "the player moved without a ready signal");
            Helper.assertFalse(IntroDirector.record(User).Started(), "the intro started without a ready signal");
            IntroDirector.ready(User, Found.Token + 1, false);
            Helper.assertTrue(Found.Stage == IntroSession.Waiting, "a stale ready token started the intro");
            IntroDirector.ready(User, Found.Token, false);
            Helper.assertTrue(IntroWorld.inside(User) && Found.Stage == IntroSession.Office, "a ready signal did not start the intro");
            Helper.assertFalse(Found.Ready, "the office played before its own ready signal");
            IntroDirector.ready(User, Found.Token, false);
            Helper.assertTrue(Found.Ready, "the office ready signal was ignored");
            Helper.succeed();
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "intro_skip_ready")
    public static void clientSkipMarksDone(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_skip_ready");
            intro(Helper);
            Vec3 Before = User.position();
            IntroDirector.loggedIn(User);
            IntroSession Found = session(Helper, User);
            IntroDirector.ready(User, Found.Token, true);
            Helper.assertTrue(IntroDirector.record(User).Done(), "a skipping client was not marked done");
            Helper.assertTrue(IntroDirector.session(User) == null, "a skipping client kept a session");
            Helper.assertTrue(!IntroWorld.inside(User) && User.position().distanceToSqr(Before) < Close, "a skipping client was moved");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "intro_rule")
    public static void ruleOffPlaysNothing(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        boolean Was = IntroSettings.enabled(Server);
        try {
            GatewayGameTests.guard(Helper, () -> {
                IntroSettings.setEnabled(Server, false);
                ServerPlayer User = person(Helper, "intro_rule");
                IntroDirector.loggedIn(User);
                Helper.assertTrue(IntroDirector.session(User) == null, "the intro queued with the game rule off");
                Helper.assertTrue(IntroDirector.record(User).Done(), "a new player was not flagged with the game rule off");
                Helper.assertFalse(IntroWorld.inside(User), "a player was moved with the game rule off");
            });
        } finally {
            IntroSettings.setEnabled(Server, Was);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "intro_plots")
    public static void plotsArePerPlayer(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer First = person(Helper, "intro_plot_a");
            ServerPlayer Second = person(Helper, "intro_plot_b");
            enterOffice(Helper, First);
            enterOffice(Helper, Second);
            int PlotA = session(Helper, First).Plot;
            int PlotB = session(Helper, Second).Plot;
            Helper.assertTrue(PlotA != PlotB, "two players shared a plot");
            BlockPos A = IntroWorld.plotOrigin(PlotA);
            BlockPos B = IntroWorld.plotOrigin(PlotB);
            Helper.assertTrue(Math.max(Math.abs(A.getX() - B.getX()), Math.abs(A.getZ() - B.getZ())) >= 2048, "plots are closer than 2048 blocks");
            Helper.assertFalse(IntroWorld.plotBounds(PlotA).inflate(1024.0).intersects(IntroWorld.plotBounds(PlotB)), "plots overlap their margins");
            Helper.assertTrue(IntroWorld.plotAt(First.getX(), First.getZ()) == PlotA && IntroWorld.plotAt(Second.getX(), Second.getZ()) == PlotB, "plot lookup by position is wrong");
            Helper.assertFalse(Second.broadcastToPlayer(First) || First.broadcastToPlayer(Second), "two intro players can see each other");
            ServerLevel Intro = intro(Helper);
            net.minecraft.world.entity.Entity DocA = IntroDirector.scientist(First, session(Helper, First));
            net.minecraft.world.entity.Entity DocB = IntroDirector.scientist(Second, session(Helper, Second));
            Helper.assertTrue(DocA != null && DocB != null, "a scientist is missing");
            Helper.assertTrue(DocA.broadcastToPlayer(First) && DocB.broadcastToPlayer(Second), "a player cannot see their own scientist");
            Helper.assertFalse(DocA.broadcastToPlayer(Second) || DocB.broadcastToPlayer(First), "a player can see another plot's scientist");
            for (net.rai.scp_unleashed.intro.IntroStaff Walker : Intro.getEntitiesOfClass(net.rai.scp_unleashed.intro.IntroStaff.class, IntroWorld.plotBounds(PlotA).inflate(2.0))) {
                Helper.assertFalse(Walker.broadcastToPlayer(Second), "a player can see another plot's staff");
            }
            IntroDirector.replay(First);
            IntroSession Again = session(Helper, First);
            IntroDirector.ready(First, Again.Token, true);
            Helper.assertTrue(Again.Plot == PlotA, "a replay used a different plot: " + Again.Plot + " instead of " + PlotA);
            Helper.assertTrue(IntroPlots.get(First.server).plotOf(First.getUUID()) == PlotA, "the saved plot changed");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "intro_cleanup")
    public static void plotClearsAndRebuilds(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_cleanup");
            enterOffice(Helper, User);
            ServerLevel Intro = intro(Helper);
            int Plot = session(Helper, User).Plot;
            Helper.assertTrue(IntroBuilder.intact(Intro, Plot), "the rooms were not built for the run");
            IntroSession Found = session(Helper, User);
            IntroDirector.action(User, Found.Token, IntroActionPayload.Skip);
            Helper.assertTrue(vacant(Intro, Plot), "the plot was not cleared after the intro ended");
            IntroDirector.replay(User);
            IntroSession Again = session(Helper, User);
            IntroDirector.ready(User, Again.Token, false);
            Helper.assertTrue(Again.Plot == Plot && IntroBuilder.intact(Intro, Plot), "the replay did not rebuild the plot");
            IntroDirector.loggedOut(User);
            Helper.assertTrue(vacant(Intro, Plot), "the plot was not cleared after a disconnect");
            IntroDirector.loggedIn(User);
            Helper.assertTrue(vacant(Intro, Plot) && !IntroWorld.inside(User), "the rejoin did not return the player to an empty plot");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Long, batch = "intro_return_gate")
    public static void returnAfterGate(GameTestHelper Helper) {
        ServerPlayer User = person(Helper, "intro_return_gate");
        User.setYRot(37.5f);
        User.setXRot(-12.25f);
        ResourceKey<Level> Home = User.level().dimension();
        Vec3 Pos = User.position();
        GatewayGameTests.guard(Helper, () -> {
            enterOffice(Helper, User);
            IntroSession Found = session(Helper, User);
            IntroDirector.ready(User, Found.Token, false);
            IntroDirector.action(User, Found.Token, IntroActionPayload.Stand);
            IntroDirector.action(User, Found.Token, IntroActionPayload.DialogueDone);
            Helper.assertTrue(Found.Stage == IntroSession.Gate, "finishing the dialogue did not move on to the gate");
            IntroDirector.ready(User, Found.Token, false);
            Vec3 Middle = IntroWorld.gateCentre(Found.Plot);
            User.teleportTo(Middle.x, IntroWorld.gateField(Found.Plot).minY, Middle.z);
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(IntroDirector.record(User).Done(), "the gate did not finish the intro");
            sameSpot(Helper, User, Home, Pos, 37.5f, -12.25f);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_return_skip")
    public static void returnAfterSkip(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_return_skip");
            User.setYRot(-121.0f);
            User.setXRot(33.0f);
            User.fallDistance = 2.5f;
            ResourceKey<Level> Home = User.level().dimension();
            Vec3 Pos = User.position();
            enterOffice(Helper, User);
            IntroSession Found = session(Helper, User);
            IntroDirector.ready(User, Found.Token, false);
            IntroDirector.action(User, Found.Token, IntroActionPayload.Skip);
            Helper.assertTrue(IntroDirector.record(User).Done(), "skipping did not set the flag");
            sameSpot(Helper, User, Home, Pos, -121.0f, 33.0f);
            Helper.assertTrue(Math.abs(User.fallDistance - 2.5f) < 1.0E-4f, "the fall distance was not restored: " + User.fallDistance);
            Helper.assertTrue(Found.Stage == IntroSession.Returning, "the session did not wait for the arrival");
            IntroDirector.ready(User, Found.Token, false);
            Helper.assertTrue(IntroDirector.session(User) == null, "the arrival ready did not close the session");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_rejoin")
    public static void disconnectRestoresOnRejoin(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_rejoin");
            User.setYRot(12.0f);
            User.setXRot(4.0f);
            ResourceKey<Level> Home = User.level().dimension();
            Vec3 Pos = User.position();
            enterOffice(Helper, User);
            int Plot = session(Helper, User).Plot;
            IntroDirector.loggedOut(User);
            Helper.assertTrue(IntroDirector.session(User) == null, "the session outlived the disconnect");
            Helper.assertTrue(IntroDirector.record(User).active(), "the return spot was lost on disconnect");
            ServerLevel Intro = intro(Helper);
            Helper.assertTrue(Intro.getEntitiesOfClass(IntroScientist.class, IntroWorld.plotBounds(Plot).inflate(2.0)).isEmpty(), "the scientist outlived the disconnect");
            IntroDirector.loggedIn(User);
            sameSpot(Helper, User, Home, Pos, 12.0f, 4.0f);
            IntroRecord After = IntroDirector.record(User);
            Helper.assertFalse(After.active() || After.Done(), "the rejoin left the wrong record");
            IntroSession Fresh = session(Helper, User);
            Helper.assertTrue(Fresh.Stage == IntroSession.Waiting, "the intro did not restart cleanly after the rejoin");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_commands")
    public static void replayAndReset(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerPlayer User = person(Helper, "intro_commands");
            intro(Helper);
            IntroDirector.markDone(User);
            IntroDirector.replay(User);
            IntroSession Found = session(Helper, User);
            Helper.assertTrue(Found.Replay, "the replay was not marked as one");
            IntroDirector.ready(User, Found.Token, true);
            Helper.assertTrue(IntroWorld.inside(User), "a replay obeyed the client skip setting");
            IntroDirector.action(User, Found.Token, IntroActionPayload.Skip);
            Helper.assertTrue(IntroDirector.record(User).Done(), "the replay did not end done");
            IntroDirector.reset(User);
            IntroRecord After = IntroDirector.record(User);
            Helper.assertFalse(After.Done() || After.Started(), "the reset did not clear the flag");
            Helper.assertTrue(User.server.getCommands().getDispatcher().getRoot().getChild("scp_unleashed").getChild("intro") != null, "the intro command is missing");
        });
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_rooms")
    public static void roomsBuildInsidePlots(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel Intro = intro(Helper);
            int Plot = IntroPlots.get(Intro.getServer()).plotOf(UUID.randomUUID());
            AABB Bounds = IntroWorld.plotBounds(Plot);
            settle(Intro, Bounds);
            java.util.Map<BlockPos, BlockState> Around = new java.util.HashMap<>();
            for (BlockPos Spot : ring(Plot, Bounds)) {
                Around.put(Spot, Intro.getBlockState(Spot));
            }
            Helper.assertTrue(IntroBuilder.ensure(Intro, Plot), "the rooms did not build");
            Helper.assertTrue(IntroPlots.get(Intro.getServer()).builtSignature(Plot) == IntroBuilder.signature(Intro), "the build signature was not stored");
            for (java.util.Map.Entry<BlockPos, BlockState> Entry : Around.entrySet()) {
                BlockState After = Intro.getBlockState(Entry.getKey());
                Helper.assertTrue(!Entry.getValue().isAir() || After.isAir() || natural(After), "a block was built outside the plot at " + Entry.getKey() + ": " + After);
            }
            int Solid = 0;
            BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
            for (int X = (int) Bounds.minX; X < (int) Bounds.maxX; X++) {
                for (int Y = (int) Bounds.minY; Y < (int) Bounds.maxY; Y++) {
                    for (int Z = (int) Bounds.minZ; Z < (int) Bounds.maxZ; Z++) {
                        Cursor.set(X, Y, Z);
                        if (!Intro.getBlockState(Cursor).isAir()) {
                            Solid++;
                        }
                    }
                }
            }
            Helper.assertTrue(Solid > 2000, "the rooms are nearly empty: " + Solid);
            Helper.assertTrue(Intro.getBlockState(IntroWorld.playerChair(Plot)).getBlock() instanceof OfficeChairBlock, "the player chair is missing");
            Helper.assertTrue(Intro.getBlockState(IntroWorld.scientistChair(Plot)).getBlock() instanceof OfficeChairBlock, "the scientist chair is missing");
            Helper.assertTrue(Intro.getBlockState(IntroWorld.gateCore(Plot)).is(IntroRegistry.GateCore.get()), "the gate core is missing");
            Helper.assertTrue(Intro.getBlockState(IntroWorld.officeOrigin(Plot).offset(IntroWorld.FolderSpot)).is(IntroRegistry.Folder.get()), "the folder is missing");
            Helper.assertTrue(IntroBuilder.ensure(Intro, Plot), "a second build check failed");
        });
        Helper.succeed();
    }

    private static boolean natural(BlockState State) {
        return State.is(net.minecraft.world.level.block.Blocks.END_STONE) || State.is(net.minecraft.world.level.block.Blocks.CHORUS_PLANT)
                || State.is(net.minecraft.world.level.block.Blocks.CHORUS_FLOWER) || State.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                || State.is(net.minecraft.world.level.block.Blocks.BEDROCK) || State.is(net.minecraft.world.level.block.Blocks.END_GATEWAY)
                || State.is(net.minecraft.world.level.block.Blocks.END_STONE_BRICKS) || State.is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK);
    }

    private static void settle(ServerLevel World, AABB Bounds) {
        int Low = ((int) Math.floor(Bounds.minX) >> 4) - 2;
        int High = ((int) Math.floor(Bounds.maxX) >> 4) + 2;
        int Near = ((int) Math.floor(Bounds.minZ) >> 4) - 2;
        int Far = ((int) Math.floor(Bounds.maxZ) >> 4) + 2;
        for (int X = Low; X <= High; X++) {
            for (int Z = Near; Z <= Far; Z++) {
                World.getChunk(X, Z);
            }
        }
    }

    private static java.util.List<BlockPos> ring(int Plot, AABB Bounds) {
        java.util.List<BlockPos> Found = new java.util.ArrayList<>();
        for (int X = (int) Bounds.minX - 2; X < (int) Bounds.maxX + 2; X++) {
            for (int Y = (int) Bounds.minY - 2; Y < (int) Bounds.maxY + 2; Y++) {
                for (int Z = (int) Bounds.minZ - 2; Z < (int) Bounds.maxZ + 2; Z++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    if (!IntroBuilder.inside(Plot, Spot)) {
                        Found.add(Spot);
                    }
                }
            }
        }
        return Found;
    }

    private static boolean vacant(ServerLevel World, int Plot) {
        AABB Bounds = IntroWorld.plotBounds(Plot);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = (int) Bounds.minX; X < (int) Bounds.maxX; X++) {
            for (int Y = (int) Bounds.minY; Y < (int) Bounds.maxY; Y++) {
                for (int Z = (int) Bounds.minZ; Z < (int) Bounds.maxZ; Z++) {
                    BlockState State = World.getBlockState(Cursor.set(X, Y, Z));
                    if (!State.isAir() && !natural(State)) {
                        return false;
                    }
                }
            }
        }
        return World.getEntities((net.minecraft.world.entity.Entity) null, Bounds.inflate(2.0), net.rai.scp_unleashed.intro.IntroPrivacy::cast).isEmpty();
    }

    private static boolean floorUnder(ServerLevel World, BlockPos Feet) {
        BlockPos Below = Feet.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "intro_landing")
    public static void landingsAreSafe(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel Intro = intro(Helper);
            int Plot = IntroPlots.get(Intro.getServer()).plotOf(UUID.randomUUID());
            Helper.assertTrue(IntroBuilder.ensure(Intro, Plot), "the rooms did not build");
            BlockPos Seat = BlockPos.containing(IntroWorld.officeSeat(Plot));
            Helper.assertTrue(floorUnder(Intro, Seat), "no floor under the office seat");
            BlockState AtSeat = Intro.getBlockState(Seat);
            Helper.assertTrue(AtSeat.getBlock() instanceof OfficeChairBlock, "the office landing is not on the chair: " + AtSeat);
            Helper.assertTrue(Intro.getBlockState(Seat.above()).isAir() && Intro.getBlockState(Seat.above(2)).isAir(), "no air above the office seat");
            Vec3 Arrival = IntroWorld.gateArrival(Plot);
            BlockPos Feet = BlockPos.containing(Arrival);
            Helper.assertTrue(floorUnder(Intro, Feet), "no floor under the gate room arrival");
            Helper.assertTrue(Intro.getBlockState(Feet).isAir() && Intro.getBlockState(Feet.above()).isAir(), "the gate room arrival is not in open air");
            Helper.assertTrue(Intro.noCollision(net.minecraft.world.entity.EntityType.PLAYER.getDimensions().makeBoundingBox(Arrival)), "the gate room arrival collides");
            Vec3 Middle = IntroWorld.gateCentre(Plot);
            for (double Z = Arrival.z; Z > Middle.z; Z -= 0.5) {
                BlockPos Step = BlockPos.containing(Arrival.x, Arrival.y, Z);
                Helper.assertTrue(Intro.getBlockState(Step).getCollisionShape(Intro, Step).isEmpty() && Intro.getBlockState(Step.above()).isAir(),
                        "the walk to the gate is blocked at " + Step);
            }
            ServerPlayer User = person(Helper, "intro_landing");
            enterOffice(Helper, User);
            int OwnPlot = session(Helper, User).Plot;
            BlockPos Own = BlockPos.containing(IntroWorld.officeSeat(OwnPlot));
            Helper.assertTrue(BlockPos.containing(User.position()).equals(Own), "the player did not land at the office seat: " + User.position());
            java.util.List<net.rai.scp_unleashed.intro.IntroStaff> Hall = Intro.getEntitiesOfClass(net.rai.scp_unleashed.intro.IntroStaff.class,
                    IntroWorld.plotBounds(OwnPlot).inflate(2.0));
            Helper.assertTrue(Hall.size() >= 3, "too few staff in the hall: " + Hall.size());
            for (net.rai.scp_unleashed.intro.IntroStaff Walker : Hall) {
                Helper.assertTrue(IntroBuilder.inside(OwnPlot, Walker.blockPosition()), "a staff member spawned outside the plot at " + Walker.blockPosition());
            }
        });
        Helper.succeed();
    }
}
