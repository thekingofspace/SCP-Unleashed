package net.rai.scp_unleashed.raid.underground.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.raid.chaos.ChaosFaction;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.chaos.ChaosRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.serpentshand.SerpentSeal;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandAgent;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandFaction;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandRaidStyle;
import net.rai.scp_unleashed.raid.underground.RaidBreach;
import net.rai.scp_unleashed.raid.underground.RaidCavities;
import net.rai.scp_unleashed.raid.underground.RaidSurvey;
import net.rai.scp_unleashed.raid.underground.UndergroundRules;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RaidUndergroundGameTests {
    public static final String Arena = "faction_raids_arena";
    public static final int RockRise = 7;
    public static final int RoomLow = 12;
    public static final int RoomHigh = 18;
    public static final int PocketX = 4;
    public static final int PocketZ = 4;

    private RaidUndergroundGameTests() {
    }

    public static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 2;
    }

    public static BlockPos rel(GameTestHelper Helper, BlockPos Absolute) {
        return Absolute.subtract(Helper.absolutePos(BlockPos.ZERO));
    }

    public static BlockPos centre(int F) {
        return new BlockPos(15, F, 15);
    }

    public static void fill(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1, BlockState State) {
        ServerLevel World = Helper.getLevel();
        for (int X = X0; X <= X1; X++) {
            for (int Y = Y0; Y <= Y1; Y++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    World.setBlock(Helper.absolutePos(new BlockPos(X, Y, Z)), State, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    public static void rock(GameTestHelper Helper, int F, int MaxX) {
        fill(Helper, 0, F, 0, MaxX, F + RockRise, 31, Blocks.STONE.defaultBlockState());
    }

    public static void mainRoom(GameTestHelper Helper, int F) {
        fill(Helper, RoomLow, F, RoomLow, RoomHigh, F + 2, RoomHigh, Blocks.AIR.defaultBlockState());
    }

    public static void tunnel(GameTestHelper Helper, int F, int FromX, int ToX, int Z) {
        fill(Helper, FromX, F, Z, ToX, F + 1, Z, Blocks.AIR.defaultBlockState());
    }

    public static void bunker(GameTestHelper Helper, int F, boolean Entrance) {
        rock(Helper, F, 27);
        mainRoom(Helper, F);
        fill(Helper, PocketX - 1, F, PocketZ - 1, PocketX + 1, F + 2, PocketZ + 1, Blocks.AIR.defaultBlockState());
        if (Entrance) {
            tunnel(Helper, F, RoomHigh + 1, 27, 15);
        }
    }

    public static boolean inRoom(Vec3 Relative, int F) {
        return Relative.x >= RoomLow && Relative.x <= RoomHigh + 1 && Relative.z >= RoomLow && Relative.z <= RoomHigh + 1 && Relative.y >= F - 0.1 && Relative.y < F + 2.0;
    }

    public static RaidContext staged(GameTestHelper Helper, HostileFaction Faction, BlockPos TargetRel, double Footprint, double Heading) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState State = FactionWorld.state(Server, Faction);
        RaidWorld Data = RaidWorld.get(Server);
        BlockPos Target = Helper.absolutePos(TargetRel);
        long Now = Server.overworld().getGameTime();
        Raid Current = new Raid(Data.nextId(), Faction.id(), State.era(), World.dimension(), "gametest_underground", Target, 4.0, Footprint, 1.0, Heading, true, Now, Target);
        Data.add(Current);
        return new RaidContext(Server, World, Current, Faction, State, Faction.hostileStyle(), Data.ledger(Faction.id(), State.era()), Now);
    }

    public static RaidContext open(GameTestHelper Helper, HostileFaction Faction, BlockPos TargetRel, BlockPos ExitRel, int Level, double Heading) {
        RaidContext Context = staged(Helper, Faction, TargetRel, 4.0, Heading);
        Raid Current = Context.Current();
        Current.extra().putLong(RaidDirector.ExitKey, Helper.absolutePos(ExitRel).asLong());
        Current.extra().putInt(HostileRaidStyle.LevelKey, Level);
        Current.setStage(RaidStage.Active, Context.Now());
        Current.setDeadline(Context.Now() + 24000L);
        return Context;
    }

    public static RaidContext fresh(GameTestHelper Helper, RaidContext Old) {
        MinecraftServer Server = Helper.getLevel().getServer();
        RaidContext Found = RaidDirector.context(Server, Old.Current(), Server.overworld().getGameTime());
        return Found != null ? Found : Old;
    }

    public static void close(GameTestHelper Helper, RaidContext Context) {
        Raid Current = Context.Current();
        HostileHauls.clear(Current);
        if (Current.live()) {
            RaidDirector.end(fresh(Helper, Context), RaidOutcome.Stopped);
        }
        RaidWorld.get(Helper.getLevel().getServer()).remove(Current);
    }

    public static <T extends HostileRaider> T raider(GameTestHelper Helper, EntityType<T> Type, RaidContext Context, double X, double Y, double Z, int Level, byte Role) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        if (Made == null) {
            throw new IllegalStateException("could not create " + Type);
        }
        Made.configure(Level, Role, 0, World.random);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        if (Context != null) {
            Context.enlist(Made, RaidMark.Raider);
        }
        World.addFreshEntity(Made);
        return Made;
    }

    public static void sweep(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(12.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, Area, Candidate -> !(Candidate instanceof Player))) {
            Found.discard();
        }
    }

    public static boolean solidCell(GameTestHelper Helper, int X, int Y, int Z) {
        BlockPos Pos = Helper.absolutePos(new BlockPos(X, Y, Z));
        return !Helper.getLevel().getBlockState(Pos).getCollisionShape(Helper.getLevel(), Pos).isEmpty();
    }

    public static int solidCount(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1) {
        int Count = 0;
        for (int X = X0; X <= X1; X++) {
            for (int Y = Y0; Y <= Y1; Y++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    if (solidCell(Helper, X, Y, Z)) {
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "raid_underground_detect", skyAccess = true)
    public static void undergroundBasesAreDetected(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        bunker(Helper, F, true);
        fill(Helper, 29, F + 3, 29, 31, F + 9, 31, Blocks.OAK_PLANKS.defaultBlockState());
        RaidContext Below = staged(Helper, ChaosFaction.Instance, centre(F), 4.0, 0.0);
        RaidContext Above = staged(Helper, ChaosFaction.Instance, new BlockPos(15, F + RockRise + 1, 15), 4.0, 0.0);
        RaidContext Tower = staged(Helper, ChaosFaction.Instance, new BlockPos(30, F, 30), 4.0, 0.0);
        try {
            RaidSurvey Deep = RaidSurvey.survey(World, Below.Current());
            Helper.assertTrue(Deep.Underground(), "a room under five blocks of stone was not seen as underground");
            Helper.assertTrue(inRoom(Vec3.atBottomCenterOf(rel(Helper, Deep.Anchor())), F), "the anchor is not in the buried room: " + rel(Helper, Deep.Anchor()));
            Helper.assertTrue(Deep.Flood().entrance() != null, "the tunnel out to open ground was not found");
            Helper.assertTrue(RaidSurvey.open(World, Deep.Flood().entrance()), "the entrance has no sky above it");
            int[] Cover = RaidSurvey.cover(World, Deep.Anchor());
            Helper.assertTrue(Cover[0] >= UndergroundRules.MinDepth && Cover[1] >= UndergroundRules.MinCover && Cover[2] >= UndergroundRules.MinEarth,
                    "the cover numbers are off: depth " + Cover[0] + " solid " + Cover[1] + " earth " + Cover[2]);
            RaidSurvey Roof = RaidSurvey.survey(World, Above.Current());
            Helper.assertFalse(Roof.Underground(), "a spot on open ground was seen as underground");
            RaidSurvey Built = RaidSurvey.survey(World, Tower.Current());
            Helper.assertFalse(Built.Underground(), "a room under a wooden tower was seen as underground");
        } finally {
            close(Helper, Below);
            close(Helper, Above);
            close(Helper, Tower);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "raid_underground_cavity", skyAccess = true)
    public static void serpentsWarpIntoSafeCavity(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        bunker(Helper, F, false);
        fill(Helper, RoomLow, F - 1, RoomLow, RoomLow, F - 1, RoomLow, Blocks.LAVA.defaultBlockState());
        fill(Helper, RoomHigh, F - 1, RoomHigh, RoomHigh, F - 1, RoomHigh, Blocks.WATER.defaultBlockState());
        Player Watcher = Helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 Stand = Helper.absoluteVec(new Vec3(RoomLow + 1.5, F, 15.5));
        Watcher.moveTo(Stand.x, Stand.y, Stand.z, -90.0f, 0.0f);
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, centre(F), centre(F), 2, 0.0);
        Context.data().putInt(HostileRaidStyle.PendingKey, 2);
        Context.data().putInt(HostileRaidStyle.SquadKey, 2);
        Context.data().putInt(HostileRaidStyle.SeekersKey, 1);
        RaidSurvey Survey = RaidSurvey.survey(World, Context.Current());
        Helper.assertTrue(Survey.Underground(), "the buried room was not seen as underground");
        SerpentsHandAgent Probe = HostileRegistry.AgentType.get().create(World);
        RaidCavities.Spot Hidden = RaidCavities.find(World, Survey, List.of(Watcher), Probe);
        Helper.assertTrue(Hidden != null && Hidden.Tier() == RaidCavities.TierInside, "no hidden cavity was found inside the base: " + Hidden);
        Helper.assertTrue(RaidCavities.view(World, Hidden.Feet(), List.of(Watcher)) < 2, "the chosen cavity is in the watcher's view at " + rel(Helper, Hidden.Feet()));
        RaidCavities.Spot Found = SerpentsHandRaidStyle.cavity(Context);
        Helper.assertTrue(Found != null && Found.Tier() == RaidCavities.TierInside, "the raid did not pick a cavity inside the base: " + Found);
        BlockPos Feet = Found.Feet();
        Vec3 Relative = Vec3.atBottomCenterOf(rel(Helper, Feet));
        Helper.assertTrue(inRoom(Relative, F), "the warp point is not in the base room: " + Relative);
        Helper.assertTrue(RaidCavities.safe(World, Feet), "the warp point is not a safe three by three by three cavity");
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                for (int Dy = 0; Dy < 3; Dy++) {
                    BlockPos Cell = Feet.offset(Dx, Dy, Dz);
                    Helper.assertTrue(World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty() && World.getFluidState(Cell).isEmpty(),
                            "the cavity holds a block or fluid at " + rel(Helper, Cell));
                }
            }
        }
        Helper.assertTrue(Math.abs(Relative.x - (PocketX + 0.5)) > 2.0 || Math.abs(Relative.z - (PocketZ + 0.5)) > 2.0, "the sealed pocket was chosen");
        SerpentSeal Seal = SerpentsHandRaidStyle.openAt(Context, Feet);
        Helper.startSequence()
                .thenExecuteFor(4, () -> SerpentsHandRaidStyle.Instance.tick(fresh(Helper, Context)))
                .thenWaitUntil(() -> {
                    List<SerpentsHandAgent> Up = World.getEntitiesOfClass(SerpentsHandAgent.class, Helper.getBounds().inflate(8.0), One -> One.isAlive() && One.active());
                    Helper.assertTrue(Up.size() == 2, "the agents did not rise in the cavity, " + Up.size() + " are up");
                    for (SerpentsHandAgent Agent : Up) {
                        Helper.assertTrue(inRoom(Helper.relativeVec(Agent.position()), F), "an agent rose outside the base room at " + Helper.relativeVec(Agent.position()));
                        Helper.assertFalse(Agent.isInWall(), "an agent rose inside a wall");
                    }
                })
                .thenExecute(() -> {
                    Helper.assertTrue(RaidDirector.exitOf(Context.Current()).equals(Feet), "the warp out is not the same cavity");
                    Seal.discard();
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "raid_underground_fallback_entrance", skyAccess = true)
    public static void serpentsFallBackToTheEntrance(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        rock(Helper, F, 19);
        tunnel(Helper, F, 6, 19, 15);
        RaidContext Context = staged(Helper, SerpentsHandFaction.Instance, new BlockPos(8, F, 15), 4.0, 0.0);
        try {
            RaidSurvey Survey = RaidSurvey.survey(World, Context.Current());
            Helper.assertTrue(Survey.Underground(), "the buried tunnel was not seen as underground");
            Helper.assertTrue(Survey.Flood().entrance() != null, "the tunnel mouth was not found");
            RaidCavities.Spot Found = SerpentsHandRaidStyle.cavity(Context);
            Helper.assertTrue(Found != null && Found.Tier() == RaidCavities.TierOpen, "the warp point did not fall back to the open ground by the entrance: " + Found);
            Helper.assertTrue(Survey.Flood().contains(Found.Feet()), "the open warp point is not connected to the base");
            Helper.assertTrue(RaidSurvey.open(World, Found.Feet()), "the open warp point has a roof");
            Helper.assertTrue(RaidCavities.safe(World, Found.Feet()), "the open warp point is not safe");
        } finally {
            close(Helper, Context);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "raid_underground_fallback_surface", skyAccess = true)
    public static void serpentsFallBackToSafeGround(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        rock(Helper, F, 19);
        tunnel(Helper, F, 6, 14, 15);
        RaidContext Context = staged(Helper, SerpentsHandFaction.Instance, new BlockPos(8, F, 15), 4.0, 0.0);
        try {
            RaidSurvey Survey = RaidSurvey.survey(World, Context.Current());
            Helper.assertTrue(Survey.Underground(), "the sealed tunnel was not seen as underground");
            Helper.assertTrue(Survey.Flood().entrance() == null, "a sealed tunnel reported an entrance");
            RaidCavities.Spot Found = SerpentsHandRaidStyle.cavity(Context);
            Helper.assertTrue(Found == null || Found.Tier() == RaidCavities.TierSurface, "a sealed tunnel without room still produced tier " + (Found == null ? 0 : Found.Tier()));
            if (Found != null) {
                Helper.assertTrue(RaidCavities.safeStand(World, Found.Feet()), "the last resort warp point is inside blocks or fluid at " + rel(Helper, Found.Feet()));
                Helper.assertFalse(Survey.Flood().contains(Found.Feet()), "the last resort warp point landed in the cramped tunnel");
            }
            Helper.assertTrue(Context.data().getInt(SerpentsHandRaidStyle.TierKey) == (Found == null ? 0 : RaidCavities.TierSurface), "the tier was not recorded");
        } finally {
            close(Helper, Context);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 1400, batch = "raid_underground_breach", skyAccess = true)
    public static void chaosBlastsAHoleIntoTheBase(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        int Roof = F + RockRise + 1;
        bunker(Helper, F, false);
        RaidContext Context = open(Helper, ChaosFaction.Instance, centre(F), new BlockPos(29, Roof, 3), 3, 0.0);
        byte Mode = ChaosRaidStyle.underground(Context);
        Raid Current = Context.Current();
        Helper.assertTrue(Mode == RaidBreach.ModeShaft, "no breach was planned over the buried room, mode " + Mode);
        CompoundTag Tag = RaidBreach.data(Current);
        BlockPos Corner = rel(Helper, RaidBreach.corner(Tag));
        BlockPos LipRel = rel(Helper, RaidBreach.lip(Tag));
        int Cells = Tag.getInt("Cells");
        Helper.assertTrue(Cells > 0 && Cells <= UndergroundRules.BlockCap, "the breach needs " + Cells + " blocks");
        Helper.assertTrue(LipRel.getY() == Roof, "the charge point is not on the ground above: " + LipRel);
        Helper.assertTrue(Corner.getX() >= RoomLow && Corner.getX() + 1 <= RoomHigh && Corner.getZ() >= RoomLow && Corner.getZ() + 1 <= RoomHigh,
                "the shaft does not come down into the room: " + Corner);
        ChaosInsurgent Demo = raider(Helper, HostileRegistry.InsurgentType.get(), Context, LipRel.getX() + 3.5, Roof, LipRel.getZ() + 0.5, 3, HostileRaider.RoleFighter);
        int[] Peak = new int[1];
        Helper.onEachTick(() -> {
            if (Demo.isAlive()) {
                Peak[0] = Math.max(Peak[0], (int) Math.floor(Helper.relativeVec(Demo.position()).y + 0.01));
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(RaidBreach.phase(Current) >= RaidBreach.PhasePlanting, "the charge was never planted, phase " + RaidBreach.phase(Current)
                        + " insurgent at " + Helper.relativeVec(Demo.position())))
                .thenWaitUntil(() -> Helper.assertTrue(RaidBreach.phase(Current) == RaidBreach.PhaseOpen, "the breach never opened, phase " + RaidBreach.phase(Current)))
                .thenExecute(() -> {
                    int Removed = RaidBreach.data(Current).getInt("Removed");
                    Helper.assertTrue(Removed > 0 && Removed <= RaidBreach.data(Current).getInt("Cap"), "the breach removed " + Removed + " blocks");
                    Helper.assertTrue(solidCount(Helper, Corner.getX(), F, Corner.getZ(), Corner.getX() + 1, F + RockRise, Corner.getZ() + 1) == 0, "the shaft is still blocked");
                    Helper.assertTrue(RaidSurvey.open(World, RaidBreach.corner(RaidBreach.data(Current))), "the shaft does not open to the sky");
                    Helper.assertTrue(solidCell(Helper, Corner.getX() - 1, F + 5, Corner.getZ() - 1) && solidCell(Helper, Corner.getX() + 2, F + 5, Corner.getZ() + 2),
                            "the blast carved more than the shaft");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Demo.active() && inRoom(Helper.relativeVec(Demo.position()), F), "the insurgent never came down into the room, at "
                        + Helper.relativeVec(Demo.position()) + " state " + Demo.state()))
                .thenExecute(() -> {
                    Peak[0] = 0;
                    RaidDirector.retreat(fresh(Helper, Context), RaidOutcome.TimedOut);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Peak[0] >= Roof, "the insurgent never climbed back out through the hole, at " + Helper.relativeVec(Demo.position())
                        + " state " + Demo.state()))
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "raid_underground_protected", skyAccess = true)
    public static void breachSparesProtectedBlocks(GameTestHelper Helper) {
        int F = floor(Helper);
        bunker(Helper, F, true);
        fill(Helper, 0, F + 5, 0, 27, F + 5, 31, Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        RaidContext Context = open(Helper, ChaosFaction.Instance, centre(F), new BlockPos(29, F, 3), 3, 0.0);
        byte Mode = ChaosRaidStyle.underground(Context);
        Helper.assertTrue(Mode == RaidBreach.ModeEntrance, "a breach was planned through protected blocks, mode " + Mode);
        int Before = solidCount(Helper, RoomLow, F + 3, RoomLow, RoomHigh, F + RockRise, RoomHigh);
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(solidCount(Helper, RoomLow, F + 3, RoomLow, RoomHigh, F + RockRise, RoomHigh) == Before, "blocks over the room were removed");
            Helper.assertTrue(solidCount(Helper, 0, F + 5, 0, 27, F + 5, 31) == 28 * 32, "the protected layer was damaged");
            close(Helper, Context);
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "raid_underground_nogrief", skyAccess = true)
    public static void breachHonoursMobGriefing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        bunker(Helper, F, true);
        RaidContext Context = open(Helper, ChaosFaction.Instance, centre(F), new BlockPos(29, F, 3), 3, 0.0);
        GameRules.BooleanValue Griefing = World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        GameRules.BooleanValue Breaching = World.getGameRules().getRule(HostileRegistry.BreachRule);
        boolean GriefingBefore = Griefing.get();
        boolean BreachingBefore = Breaching.get();
        byte Quiet;
        byte Ruled;
        boolean Allowed;
        try {
            Griefing.set(false, World.getServer());
            Quiet = ChaosRaidStyle.underground(Context);
            Allowed = RaidBreach.allowed(World, null);
            Griefing.set(true, World.getServer());
            Breaching.set(false, World.getServer());
            Ruled = ChaosRaidStyle.underground(Context);
        } finally {
            Griefing.set(GriefingBefore, World.getServer());
            Breaching.set(BreachingBefore, World.getServer());
        }
        Helper.assertFalse(Allowed, "breaching was allowed with mobGriefing off");
        Helper.assertTrue(Quiet == RaidBreach.ModeEntrance, "with mobGriefing off the raid did not fall back to the entrance, mode " + Quiet);
        Helper.assertTrue(Ruled == RaidBreach.ModeEntrance, "with breaching off the raid did not fall back to the entrance, mode " + Ruled);
        BlockPos Door = RaidBreach.entrance(RaidBreach.data(Context.Current()));
        Helper.assertTrue(Door != null && rel(Helper, Door).getX() >= 28, "the entrance is not the tunnel mouth: " + (Door == null ? null : rel(Helper, Door)));
        int Before = solidCount(Helper, RoomLow, F + 3, RoomLow, RoomHigh, F + RockRise, RoomHigh);
        ChaosInsurgent Walker = raider(Helper, HostileRegistry.InsurgentType.get(), Context, 30.5, F, 4.5, 3, HostileRaider.RoleFighter);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Walker.active() && inRoom(Helper.relativeVec(Walker.position()), F), "the insurgent never walked in through the entrance, at "
                        + Helper.relativeVec(Walker.position())))
                .thenExecute(() -> {
                    Helper.assertTrue(solidCount(Helper, RoomLow, F + 3, RoomLow, RoomHigh, F + RockRise, RoomHigh) == Before, "blocks were removed with griefing off");
                    RaidDirector.retreat(fresh(Helper, Context), RaidOutcome.TimedOut);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Helper.relativeVec(Walker.position()).x >= 27.5, "the insurgent never left through the entrance, at "
                        + Helper.relativeVec(Walker.position())))
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "raid_underground_cap", skyAccess = true)
    public static void breachStopsAtTheBlockCap(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = floor(Helper);
        bunker(Helper, F, false);
        RaidContext Context = open(Helper, ChaosFaction.Instance, centre(F), new BlockPos(29, F, 3), 3, 0.0);
        RaidSurvey Survey = RaidSurvey.survey(World, Context.Current());
        ChaosInsurgent Probe = HostileRegistry.InsurgentType.get().create(World);
        RaidBreach.Plan Full = RaidBreach.plan(World, Survey, Probe, UndergroundRules.BlockCap);
        Helper.assertTrue(Full != null && Full.Cells() <= UndergroundRules.BlockCap, "no breach fits the normal cap: " + Full);
        RaidBreach.Plan Tight = RaidBreach.plan(World, Survey, Probe, Full.Cells() - 1);
        Helper.assertTrue(Tight == null || Tight.Cells() <= Full.Cells() - 1, "a plan went over a tight cap");
        Helper.assertTrue(RaidBreach.plan(World, Survey, Probe, 4) == null, "a plan was made under a cap of four blocks");
        int Cap = Full.Cells() - 6;
        RaidBreach.storeShaft(Context.Current(), Full, Cap, null);
        CompoundTag Tag = RaidBreach.data(Context.Current());
        Tag.putByte("Phase", RaidBreach.PhaseArmed);
        Tag.putLong("Since", World.getServer().overworld().getGameTime() - UndergroundRules.FuseTicks);
        Context.Current().changed();
        raider(Helper, HostileRegistry.InsurgentType.get(), Context, 25.5, F + RockRise + 1, 25.5, 3, HostileRaider.RoleFighter).setNoAi(true);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(RaidBreach.phase(Context.Current()) == RaidBreach.PhaseFailed, "the over cap breach never stopped, phase "
                        + RaidBreach.phase(Context.Current())))
                .thenExecute(() -> {
                    int Removed = RaidBreach.data(Context.Current()).getInt("Removed");
                    Helper.assertTrue(Removed <= Cap, "the breach removed " + Removed + " blocks past its cap of " + Cap);
                    Helper.assertTrue(RaidBreach.mode(Context.Current()) == RaidBreach.ModeNone, "the failed breach did not fall back");
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }
}
