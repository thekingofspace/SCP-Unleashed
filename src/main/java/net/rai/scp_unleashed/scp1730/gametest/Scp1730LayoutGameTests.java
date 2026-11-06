package net.rai.scp_unleashed.scp1730.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp1730.Scp1730Breach;
import net.rai.scp_unleashed.scp1730.Scp1730Containment;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730WingDoor;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.plan.Scp1730AnomalousCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Basin;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Halls;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PlanCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730ReachCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730RoomInfo;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SectorCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Sectors;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SetPieces;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SupportCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Variants;
import net.rai.scp_unleashed.scp1730.rooms.gametest.Scp1730RoomsGameTests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730LayoutGameTests {
    public static final String Arena = Scp1730GameTests.Arena;
    public static final int SweepSeeds = 200;
    public static final long SweepStart = 41000L;

    private Scp1730LayoutGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp1730_layoutSweep")
    public static void layoutSweep(GameTestHelper Helper) {
        long[] Next = {SweepStart};
        long End = SweepStart + SweepSeeds;
        List<String> Problems = new CopyOnWriteArrayList<>();
        Set<String> Rooms = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<Integer> Halls = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Helper.onEachTick(() -> {
            for (int Step = 0; Step < 4 && Next[0] < End; Step++) {
                long Seed = Next[0]++;
                Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
                for (String Each : Scp1730PlanCheck.reach(Plan)) {
                    Problems.add("seed " + Seed + " plan: " + Each);
                }
                for (String Each : Scp1730SupportCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " support: " + Each);
                }
                for (String Each : Scp1730AnomalousCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " anomalous: " + Each);
                }
                for (String Each : Scp1730ReachCheck.check(Plan, Scp1730RoomsGameTests.Collision)) {
                    Problems.add("seed " + Seed + " reach: " + Each);
                }
                for (String Each : Scp1730SectorCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " sector: " + Each);
                }
                for (String Each : net.rai.scp_unleashed.scp1730.plan.Scp1730CutoffCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " cutoff: " + Each);
                }
                if (Seed % 25 == 0) {
                    for (String Each : Scp1730WallAudit.audit(Seed)) {
                        Problems.add("wall: " + Each);
                    }
                }
                for (Scp1730RoomInfo Room : Plan.rooms()) {
                    Rooms.add(Room.Type());
                }
                for (Scp1730Fixture Each : Plan.fixtures("hall")) {
                    Halls.add(Each.Data());
                }
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Next[0] >= End, "sweep still running at seed " + Next[0]);
            Helper.assertTrue(Problems.isEmpty(), Problems.size() + " problems over " + SweepSeeds + " seeds, e.g. " + Problems.subList(0, Math.min(4, Problems.size())));
            List<String> Wanted = new ArrayList<>(Scp1730Variants.Rooms);
            Wanted.add(Scp1730Sectors.BioWing);
            Wanted.add(Scp1730Sectors.Reservoir);
            for (String Each : Wanted) {
                Helper.assertTrue(Rooms.contains(Each), "the " + Each + " room never appeared in " + SweepSeeds + " seeds");
            }
            for (int Kind = 0; Kind < Scp1730Halls.Kinds.size(); Kind++) {
                Helper.assertTrue(Halls.contains(Kind), "the " + Scp1730Halls.Kinds.get(Kind) + " hall never appeared in " + SweepSeeds + " seeds");
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_layoutWater")
    public static void layoutWater(GameTestHelper Helper) {
        int Flooded = 0;
        for (long Seed = 1730L; Seed < 1750L; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
            List<Scp1730RoomInfo> Reservoirs = Plan.rooms(Scp1730Sectors.Reservoir);
            Helper.assertTrue(Reservoirs.size() == 1 && Reservoirs.getFirst().Zone() == 2, "seed " + Seed + " has no reservoir in Heavy Containment");
            Scp1730RoomInfo Room = Reservoirs.getFirst();
            Scp1730Basin Basin = null;
            for (Scp1730Basin Each : Plan.basins()) {
                if (Each.wet() && Each.MinX() >= Room.MinX() && Each.MaxX() <= Room.MaxX() && Each.MinZ() >= Room.MinZ() && Each.MaxZ() <= Room.MaxZ()) {
                    Basin = Each;
                }
            }
            Helper.assertTrue(Basin != null && Room.Floor() - 1 - Basin.BottomY() >= 4, "seed " + Seed + " reservoir has no deep basin");
            int Water = 0;
            for (int X = Basin.MinX(); X <= Basin.MaxX(); X++) {
                for (int Z = Basin.MinZ(); Z <= Basin.MaxZ(); Z++) {
                    String Top = Plan.stateAt(X, Basin.TopY(), Z);
                    Water += Top != null && Top.startsWith("minecraft:water") ? 1 : 0;
                }
            }
            Helper.assertTrue(Water > 100, "seed " + Seed + " reservoir holds too little water: " + Water);
            int Ladders = 0;
            for (Scp1730Fixture Each : Plan.fixtures("ladder", 2)) {
                Ladders += Room.contains(Each.X(), Room.Floor(), Each.Z()) ? 1 : 0;
            }
            Helper.assertTrue(Ladders >= 6, "seed " + Seed + " reservoir has only " + Ladders + " ladders");
            Helper.assertTrue(!Plan.fixtures("waterfall", 2).isEmpty(), "seed " + Seed + " reservoir has no falling water");
            Helper.assertTrue(Plan.zoneAt(Basin.MinX(), Basin.BottomY(), Basin.MinZ()) == 2, "seed " + Seed + " basin floor falls outside Heavy Containment");
            List<String> Problems = Scp1730SectorCheck.check(Plan);
            Helper.assertTrue(Problems.isEmpty(), "seed " + Seed + " sector problems: " + Problems);
            for (Scp1730Fixture Each : Plan.fixtures("hall")) {
                Flooded += Each.Data() == 0 ? 1 : 0;
            }
        }
        Helper.assertTrue(Flooded >= 10, "too few flooded corridors: " + Flooded);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730_layoutWingRule")
    public static void layoutWingRule(GameTestHelper Helper) {
        RandomSource Random = RandomSource.create(1730L);
        for (int Zone = 0; Zone < 4; Zone++) {
            for (int Roll = 0; Roll < 2000; Roll++) {
                Scp1730Breach.Threat Hall = Scp1730Containment.pick(Zone, false, Random);
                Helper.assertTrue(Hall == null || !Scp1730Containment.biological(Hall.Id()), "zone " + Zone + " picked " + Hall + " for an open hall");
                Scp1730Breach.Threat Wing = Scp1730Containment.pick(Zone, true, Random);
                Helper.assertTrue(Wing == null || Scp1730Containment.biological(Wing.Id()), "zone " + Zone + " picked " + Wing + " inside a wing");
            }
            for (int Index = 0; Index < 12; Index++) {
                for (Scp1730Breach.Threat Each : Scp1730Containment.setPiece(Zone, Index, 1730L + Index, false)) {
                    Helper.assertTrue(!Scp1730Containment.biological(Each.Id()), "zone " + Zone + " hall set piece holds " + Each.Id());
                }
                List<Scp1730Breach.Threat> Inside = Scp1730Containment.setPiece(Zone, Index, 1730L + Index, true);
                Helper.assertTrue(Inside.isEmpty() || Scp1730Containment.biological(Inside), "zone " + Zone + " wing set piece holds nothing biological");
            }
        }
        for (long Seed = 1730L; Seed < 1760L; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
            for (int Zone : Scp1730SectorCheck.WingZones) {
                Helper.assertTrue(Plan.rooms(Scp1730Sectors.BioWing).stream().anyMatch(Room -> Room.Zone() == Zone), "seed " + Seed + " zone " + Zone + " has no biological wing");
            }
            Helper.assertTrue(Plan.rooms(Scp1730Sectors.BioWing).stream().noneMatch(Room -> Room.Zone() == 0), "seed " + Seed + " put a biological wing in the Entrance Zone");
            int Doctors = 0;
            for (int Zone = 0; Zone < 4; Zone++) {
                for (Scp1730Fixture Each : Plan.fixtures("setpiece", Zone)) {
                    boolean Wing = Plan.inWing(Each.X(), Each.Y(), Each.Z());
                    boolean Living = Scp1730SetPieces.biological(Zone, Each.Data(), Plan.seed());
                    Helper.assertTrue(!Living || Wing, "seed " + Seed + " zone " + Zone + " put a biological set piece outside its wing");
                    Helper.assertTrue(!Wing || Living, "seed " + Seed + " zone " + Zone + " put a harmless set piece inside its wing");
                    if (Scp1730SetPieces.scene(Zone, Each.Data(), Plan.seed()).equals(Scp1730SetPieces.PlagueDoctor)) {
                        Doctors++;
                        Helper.assertTrue(Zone == 2 && Wing, "seed " + Seed + " placed the plague doctor outside the Heavy Containment wing");
                    }
                }
            }
            Helper.assertTrue(Doctors == 1, "seed " + Seed + " has " + Doctors + " plague doctor cells");
        }
        Helper.succeed();
    }

    static BlockHitResult hit(BlockPos Pos) {
        return new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false);
    }

    static boolean sealed(ServerLevel World, int Run, List<Scp1730Fixture> Seals) {
        for (Scp1730Fixture Each : Seals) {
            if (!(World.getBlockState(Scp1730World.world(Run, Each)).getBlock() instanceof Scp1730SealBlock)) {
                return false;
            }
        }
        return !Seals.isEmpty();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_layoutWingDoor")
    public static void layoutWingDoor(GameTestHelper Helper) {
        ServerPlayer User = Scp1730GameTests.enter(Helper, "scp1730_keeper");
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        int Run = State.run();
        Scp1730Plan Plan = Scp1730World.plan(Run);
        Scp1730World.paintZone(Facility, Run, 1);
        List<Scp1730Fixture> Seals = Plan.fixtures("bioseal", 1);
        Scp1730Fixture Outer = null;
        Scp1730Fixture Inner = null;
        for (Scp1730Fixture Each : Plan.fixtures("biocontrol", 1)) {
            if (Each.Data() == Scp1730Sectors.OuterReader) {
                Outer = Each;
            } else {
                Inner = Each;
            }
        }
        Helper.assertTrue(Seals.size() == 9 && Outer != null && Inner != null, "the Light Containment wing has no sealed door with readers");
        BlockPos Reader = Scp1730World.world(Run, Outer);
        List<long[]> Forced = new ArrayList<>();
        for (int Cx = (Reader.getX() >> 4) - 2; Cx <= (Reader.getX() >> 4) + 2; Cx++) {
            for (int Cz = (Reader.getZ() >> 4) - 2; Cz <= (Reader.getZ() >> 4) + 2; Cz++) {
                if (Facility.setChunkForced(Cx, Cz, true)) {
                    Forced.add(new long[]{Cx, Cz});
                }
            }
        }
        BlockPos Exit = Scp1730World.world(Run, Inner);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Facility.isPositionEntityTicking(Reader) && Facility.isPositionEntityTicking(Exit), "the wing area is still loading"))
                .thenExecute(() -> {
                    BlockState Panel = Facility.getBlockState(Reader);
                    Helper.assertTrue(Panel.getBlock() instanceof Scp1730ControlBlock && Panel.getValue(Scp1730ControlBlock.Type) == Scp1730ControlBlock.Kind.Clearance,
                            "the wing reader is not a clearance reader: " + Panel);
                    Helper.assertTrue(sealed(Facility, Run, Seals), "the wing door was not sealed at the start");
                    Vec3 Stand = Vec3.atBottomCenterOf(Reader).add(0.0, -1.0, 0.0);
                    User.teleportTo(Stand.x, Stand.y, Stand.z);
                    User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp1730Registry.AccessCard.get()));
                    Panel.useWithoutItem(Facility, User, hit(Reader));
                    Helper.assertTrue(sealed(Facility, Run, Seals), "the wing door opened without clearance");
                    Mob Stranger = EntityType.ZOMBIE.create(Facility);
                    Helper.assertTrue(Stranger != null, "no zombie to test with");
                    Stranger.moveTo(Vec3.atBottomCenterOf(Reader).add(0.0, -1.0, 0.0));
                    Facility.addFreshEntity(Stranger);
                    Helper.assertTrue(sealed(Facility, Run, Seals), "a mob near the reader opened the wing door");
                    User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp1730Registry.ClearanceCard.get()));
                    Facility.getBlockState(Reader).useWithoutItem(Facility, User, hit(Reader));
                    Helper.assertTrue(!sealed(Facility, Run, Seals) && Scp1730WingDoor.open(Facility, Run, 1), "the clearance card did not open the wing door");
                    Helper.assertTrue(User.getMainHandItem().isEmpty(), "the reader did not keep the clearance card");
                    Helper.assertTrue(Facility.getBlockState(Reader).getValue(Scp1730ControlBlock.Active), "the reader did not unlock");
                    Helper.assertTrue(State.wingClose(1) > Facility.getGameTime(), "the open door was not scheduled to close");
                    Scp1730Fixture Middle = Seals.get(4);
                    Vec3 Doorway = Vec3.atBottomCenterOf(Scp1730World.world(Run, Middle)).add(0.0, -1.0, 0.0);
                    Stranger.teleportTo(Doorway.x, Doorway.y, Doorway.z);
                    State.setWingClose(1, Facility.getGameTime());
                    Scp1730WingDoor.closeDue(Facility, State, Run);
                    Helper.assertTrue(!sealed(Facility, Run, Seals), "the door closed on a mob standing in it: " + Stranger.position() + " alive " + Stranger.isAlive() + " removed "
                            + Stranger.isRemoved() + " doorway " + Scp1730WingDoor.doorway(Run, Seals));
                    Stranger.discard();
                    State.setWingClose(1, Facility.getGameTime());
                    Scp1730WingDoor.closeDue(Facility, State, Run);
                    Helper.assertTrue(sealed(Facility, Run, Seals) && State.wingClose(1) == 0L, "the wing door did not seal again");
                    Facility.getBlockState(Reader).useWithoutItem(Facility, User, hit(Reader));
                    Helper.assertTrue(!sealed(Facility, Run, Seals), "the unlocked reader did not reopen the door");
                    Facility.getBlockState(Reader).useWithoutItem(Facility, User, hit(Reader));
                    Scp1730WingDoor.closeDue(Facility, State, Run);
                    Helper.assertTrue(sealed(Facility, Run, Seals), "the reader did not close the open door");
                    BlockState Inside = Facility.getBlockState(Exit);
                    Helper.assertTrue(Inside.getBlock() instanceof Scp1730ControlBlock && Inside.getValue(Scp1730ControlBlock.Type) == Scp1730ControlBlock.Kind.Exit, "the airlock has no exit panel");
                    Vec3 Within = Vec3.atBottomCenterOf(Exit).add(0.0, -1.0, 0.0);
                    User.teleportTo(Within.x, Within.y, Within.z);
                    Inside.useWithoutItem(Facility, User, hit(Exit));
                    Helper.assertTrue(!sealed(Facility, Run, Seals), "the exit panel did not open the door from inside");
                    State.setWingClose(1, Facility.getGameTime());
                    Scp1730WingDoor.closeDue(Facility, State, Run);
                    Helper.assertTrue(sealed(Facility, Run, Seals), "the wing door stayed open after the exit cycle");
                    for (long[] Each : Forced) {
                        Facility.setChunkForced((int) Each[0], (int) Each[1], false);
                    }
                    GatewayTravel.leave(User);
                    Scp1730GameTests.dismiss(User);
                    Helper.succeed();
                });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp1730_layoutWingSpawns")
    public static void layoutWingSpawns(GameTestHelper Helper) {
        ServerPlayer User = Scp1730GameTests.enter(Helper, "scp1730_census");
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        int Run = State.run();
        Scp1730Plan Plan = Scp1730World.plan(Run);
        Scp1730World.paintZone(Facility, Run, 1);
        Scp1730RoomInfo Wing = Plan.rooms(Scp1730Sectors.BioWing).stream().filter(Room -> Room.Zone() == 1).findFirst().orElse(null);
        Helper.assertTrue(Wing != null, "Light Containment has no wing");
        Scp1730Fixture Inner = Plan.fixtures("biocontrol", 1).stream().filter(Each -> Each.Data() == Scp1730Sectors.InnerReader).findFirst().orElse(null);
        Helper.assertTrue(Inner != null, "the wing has no exit panel");
        Vec3 Centre = Vec3.atBottomCenterOf(Scp1730World.world(Run, (Wing.MinX() + Wing.MaxX()) / 2, Wing.Floor() + 1, (Wing.MinZ() + Wing.MaxZ()) / 2));
        Vec3 Outside = Scp1730World.world(Run, Plan.entry(1));
        int InRange = 0;
        for (Scp1730Fixture Each : Plan.fixtures("spawn", 1)) {
            double Distance = Vec3.atBottomCenterOf(Scp1730World.world(Run, Each)).distanceTo(Centre);
            if (Plan.inWing(Each.X(), Each.Y(), Each.Z()) && Distance >= Scp1730Breach.SpawnNear && Distance <= Scp1730Breach.SpawnFar) {
                InRange++;
            }
        }
        int Spots = InRange;
        User.teleportTo(Outside.x, Outside.y, Outside.z);
        AABB Box = Scp1730Breach.boxOf(Run, Plan.zone(1));
        List<long[]> Forced = new ArrayList<>();
        for (int Cx = (int) Math.floor(Box.minX) >> 4; Cx <= (int) Math.floor(Box.maxX) >> 4; Cx++) {
            for (int Cz = (int) Math.floor(Box.minZ) >> 4; Cz <= (int) Math.floor(Box.maxZ) >> 4; Cz++) {
                if (Facility.setChunkForced(Cx, Cz, true)) {
                    Forced.add(new long[]{Cx, Cz});
                }
            }
        }
        Set<String> Seen = new HashSet<>();
        List<String> Wrong = new ArrayList<>();
        Helper.runAfterDelay(60, () -> {
            for (int Round = 0; Round < 30; Round++) {
                Scp1730Breach.spawnRound(Facility, User, Run, 1);
                audit(Facility, Plan, Run, Box, Seen, Wrong);
            }
            for (Mob Each : Facility.getEntitiesOfClass(Mob.class, Box.inflate(8.0), Each -> Each.getTags().contains(Scp1730Breach.RoamingTag))) {
                Each.discard();
            }
            User.teleportTo(Centre.x, Centre.y, Centre.z);
            Helper.runAfterDelay(60, () -> {
                for (int Round = 0; Round < 30; Round++) {
                    Scp1730Breach.spawnRound(Facility, User, Run, 1);
                    audit(Facility, Plan, Run, Box, Seen, Wrong);
                }
                boolean InsideAny = false;
                for (Mob Each : Facility.getEntitiesOfClass(Mob.class, Box.inflate(8.0), Each -> Each.getTags().contains(Scp1730Breach.RoamingTag))) {
                    InsideAny |= Scp1730Containment.inWing(Plan, Run, Each.position()) && Each.getTags().contains(Scp1730Containment.WingTag);
                    Each.discard();
                }
                Helper.assertTrue(Wrong.isEmpty(), "threats spawned in the wrong place: " + Wrong.subList(0, Math.min(4, Wrong.size())));
                for (long[] Each : Forced) {
                    Facility.setChunkForced((int) Each[0], (int) Each[1], false);
                }
                Helper.assertTrue(Spots == 0 || InsideAny || Seen.stream().anyMatch(Scp1730Containment::biological), "nothing biological spawned inside the wing: " + Seen);
                GatewayTravel.leave(User);
                Scp1730GameTests.dismiss(User);
                Helper.succeed();
            });
        });
    }

    static void audit(ServerLevel World, Scp1730Plan Plan, int Run, AABB Box, Set<String> Seen, List<String> Wrong) {
        for (Mob Each : World.getEntitiesOfClass(Mob.class, Box.inflate(8.0), Each -> Each.getTags().contains(Scp1730Breach.RoamingTag))) {
            String Id = Scp1730Containment.idOf(Each);
            Seen.add(Id);
            boolean Wing = Scp1730Containment.inWing(Plan, Run, Each.position());
            if (Scp1730Containment.biological(Id) && !Wing) {
                Wrong.add(Id + " outside the wing at " + Each.blockPosition());
            }
        }
    }
}
