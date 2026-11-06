package net.rai.scp_unleashed.scp1730.puzzle.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.gametest.Scp1730GameTests;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PuzzleCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Puzzles;
import net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleDirector;
import net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleRegistry;
import net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleState;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730GeneratorBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730JunctionBoxBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730LockdownPanelBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PickupBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PylonBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730SwitchBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730TerminalBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730VentGrateBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730PuzzleGameTests {
    public static final String Arena = Scp1730GameTests.Arena;
    public static final int SweepSeeds = 120;
    public static final long SweepStart = 52000L;
    public static final int Search = 400;

    private Scp1730PuzzleGameTests() {
    }

    record Scene(GameTestHelper Helper, ServerPlayer User, ServerLevel World, int Run, Scp1730Plan Plan, int Zone, int Puzzle, List<long[]> Forced) {
        Scp1730PuzzleDirector.Context context() {
            return Scp1730PuzzleDirector.zoneContext(World, Run, Zone);
        }

        Scp1730PuzzleState.Zone data() {
            return Scp1730PuzzleState.get(World.getServer()).zone(Run, Zone);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp1730_puzzleSweep")
    public static void puzzleSweep(GameTestHelper Helper) {
        long[] Next = {SweepStart};
        long End = SweepStart + SweepSeeds;
        List<String> Problems = new CopyOnWriteArrayList<>();
        int[] Seen = new int[Scp1730Puzzles.Ids.length];
        Helper.onEachTick(() -> {
            for (int Step = 0; Step < 3 && Next[0] < End; Step++) {
                long Seed = Next[0]++;
                Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
                for (String Each : Scp1730PuzzleCheck.check(Plan)) {
                    Problems.add("seed " + Seed + ": " + Each);
                }
                for (int Zone = 0; Zone < Scp1730Puzzles.Zones; Zone++) {
                    int Puzzle = Scp1730Puzzles.puzzleOf(Plan, Zone);
                    if (Puzzle >= 0) {
                        synchronized (Seen) {
                            Seen[Puzzle]++;
                        }
                    }
                }
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Next[0] >= End, "sweep still running at seed " + Next[0]);
            Helper.assertTrue(Problems.isEmpty(), Problems.size() + " puzzle problems over " + SweepSeeds + " seeds, e.g. " + Problems.subList(0, Math.min(4, Problems.size())));
            for (int Puzzle = 0; Puzzle < Seen.length; Puzzle++) {
                Helper.assertTrue(Seen[Puzzle] > 0, "the " + Scp1730Puzzles.Ids[Puzzle] + " puzzle never appeared in " + SweepSeeds + " seeds");
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_puzzleState")
    public static void puzzleState(GameTestHelper Helper) {
        Scp1730PuzzleState First = new Scp1730PuzzleState();
        Scp1730PuzzleState.Zone Data = First.zone(7, 1);
        Data.Progress = 5;
        Data.Counts[0] = 2;
        Data.Counts[3] = 9;
        Data.Timer = 1234L;
        Data.Lockout = 99L;
        Data.Taken = 6;
        Data.TakenAt = 88L;
        Data.Hold = 300;
        Data.Waved = true;
        First.zone(7, 3).Solved = true;
        CompoundTag Saved = First.save(new CompoundTag(), Helper.getLevel().registryAccess());
        Scp1730PuzzleState Second = Scp1730PuzzleState.load(Saved, Helper.getLevel().registryAccess());
        Helper.assertTrue(Second.run() == 7, "the run was not saved");
        Scp1730PuzzleState.Zone Back = Second.zone(7, 1);
        Helper.assertTrue(Back.Progress == 5 && Back.Counts[0] == 2 && Back.Counts[3] == 9 && Back.Timer == 1234L && Back.Lockout == 99L && Back.Taken == 6 && Back.TakenAt == 88L
                && Back.Hold == 300 && Back.Waved, "zone progress did not survive a save");
        Helper.assertTrue(Second.zone(7, 3).Solved && !Second.zone(7, 0).Solved, "solved flags did not survive a save");
        Helper.assertTrue(!Second.zone(8, 1).has(0) && !Second.zone(8, 3).Solved && Second.run() == 8, "a new run kept the old progress");
        Helper.succeed();
    }

    static Scene start(GameTestHelper Helper, int Zone, int Puzzle, String Name) {
        MinecraftServer Server = Helper.getLevel().getServer();
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Scp1730State Main = Scp1730State.get(Server);
        if (Main.active()) {
            Main.endRun();
        }
        int Found = -1;
        for (int Run = Main.run() + 1; Run < Main.run() + 1 + Search && Found < 0; Run++) {
            if (Scp1730Puzzles.puzzleOf(Scp1730World.plan(Run), Zone) == Puzzle) {
                Found = Run;
            }
        }
        Helper.assertTrue(Found >= 0, "no run in " + Search + " has the " + Scp1730Puzzles.Ids[Puzzle] + " puzzle");
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Run", Found - 1);
        Tag.putBoolean("Active", false);
        Server.overworld().getDataStorage().set(Scp1730State.Name, Scp1730State.load(Tag, Server.registryAccess()));
        Scp1730State Fresh = Scp1730State.get(Server);
        Fresh.startRun(Facility.getGameTime());
        Helper.assertTrue(Fresh.active() && Fresh.run() == Found, "the run did not start where expected: " + Fresh.run() + " vs " + Found);
        Scp1730Plan Plan = Scp1730World.plan(Found);
        Vec3 Arrive = Scp1730World.world(Found, Plan.entry(Zone));
        ServerPlayer User = Scp1730GameTests.player(Helper, Facility, Arrive, Name);
        User.getAbilities().invulnerable = true;
        User.onUpdateAbilities();
        List<Scp1730Fixture> Marks = new ArrayList<>();
        for (String Type : new String[]{"pdevice", "pickup", "puzzle", "seal", "dseal", "pseal", "card"}) {
            Marks.addAll(Plan.fixtures(Type, Zone));
        }
        for (Scp1730Fixture Each : Marks) {
            BlockPos At = Scp1730World.world(Found, Each);
            Scp1730World.paint(Facility, Found, At.getX() - 6, At.getZ() - 6, At.getX() + 6, At.getZ() + 6);
        }
        BlockPos Start = BlockPos.containing(Arrive);
        Scp1730World.paint(Facility, Found, Start.getX() - 4, Start.getZ() - 4, Start.getX() + 4, Start.getZ() + 4);
        List<long[]> Forced = new ArrayList<>();
        Marks.add(new Scp1730Fixture("entry", (int) Math.floor(Plan.entry(Zone).X()), (int) Plan.entry(Zone).Y(), (int) Math.floor(Plan.entry(Zone).Z()), "north", 0, Zone));
        for (Scp1730Fixture Each : Marks) {
            BlockPos At = Scp1730World.world(Found, Each);
            if (Facility.setChunkForced(At.getX() >> 4, At.getZ() >> 4, true)) {
                Forced.add(new long[]{At.getX() >> 4, At.getZ() >> 4});
            }
        }
        Vec3 Entry = Scp1730World.world(Found, Plan.entry(Zone));
        User.teleportTo(Entry.x, Entry.y, Entry.z);
        User.setXRot(90.0f);
        return new Scene(Helper, User, Facility, Found, Plan, Zone, Puzzle, Forced);
    }

    static void finish(Scene S) {
        for (long[] Each : S.Forced()) {
            S.World().setChunkForced((int) Each[0], (int) Each[1], false);
        }
        Scp1730GameTests.dismiss(S.User());
        Scp1730State.get(S.World().getServer()).endRun();
        S.Helper().succeed();
    }

    static BlockPos device(Scene S, int Role) {
        Scp1730Fixture Found = Scp1730Puzzles.device(S.Plan(), S.Zone(), Role);
        S.Helper().assertTrue(Found != null, Scp1730Puzzles.Ids[S.Puzzle()] + " has no part " + Role);
        return Scp1730World.world(S.Run(), Found);
    }

    static int extra(Scene S, int Role) {
        return Scp1730Puzzles.extra(Scp1730Puzzles.device(S.Plan(), S.Zone(), Role).Data());
    }

    static List<BlockPos> pickups(Scene S) {
        List<BlockPos> Found = new ArrayList<>();
        for (Scp1730Fixture Each : S.Plan().fixtures("pickup", S.Zone())) {
            Found.add(Scp1730World.world(S.Run(), Each));
        }
        return Found;
    }

    static BlockPos panel(Scene S) {
        return Scp1730World.world(S.Run(), Scp1730Puzzles.panel(S.Plan(), S.Zone()));
    }

    static BlockHitResult hit(BlockPos Pos) {
        return new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false);
    }

    static void click(Scene S, BlockPos Pos, ItemStack Held) {
        S.User().getInventory().selected = 8;
        S.User().setItemInHand(InteractionHand.MAIN_HAND, Held);
        BlockState State = S.World().getBlockState(Pos);
        ItemInteractionResult Result = State.useItemOn(Held, S.World(), S.User(), InteractionHand.MAIN_HAND, hit(Pos));
        if (Result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            S.World().getBlockState(Pos).useWithoutItem(S.World(), S.User(), hit(Pos));
        }
    }

    static void stand(Scene S, BlockPos Device) {
        Direction Out = S.World().getBlockState(Device).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        BlockPos Spot = Device.relative(Out).below();
        S.User().teleportTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5);
    }

    static void click(Scene S, BlockPos Pos) {
        click(S, Pos, ItemStack.EMPTY);
    }

    static void take(Scene S, BlockPos Pos) {
        BlockState State = S.World().getBlockState(Pos);
        S.Helper().assertTrue(State.getBlock() instanceof Scp1730PickupBlock, "no pickup at " + Pos + ": " + State);
        click(S, Pos);
        S.Helper().assertTrue(S.World().getBlockState(Pos).isAir(), "the pickup stayed after taking it at " + Pos);
    }

    static ItemStack held(Scene S, net.minecraft.world.item.Item Wanted) {
        for (int Slot = 0; Slot < S.User().getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = S.User().getInventory().getItem(Slot);
            if (Stack.is(Wanted)) {
                ItemStack Copy = Stack.copy();
                S.User().getInventory().setItem(Slot, ItemStack.EMPTY);
                return Copy;
            }
        }
        S.Helper().fail("the player does not carry " + Wanted);
        return ItemStack.EMPTY;
    }

    static boolean locked(Scene S) {
        if (S.Zone() == Scp1730Puzzles.DeepZone) {
            List<Scp1730Fixture> Gate = S.Plan().fixtures("dseal", S.Zone());
            for (Scp1730Fixture Each : Gate) {
                if (!(S.World().getBlockState(Scp1730World.world(S.Run(), Each)).getBlock() instanceof Scp1730SealBlock)) {
                    return false;
                }
            }
            return !Gate.isEmpty() && !S.data().Solved;
        }
        if (Scp1730State.get(S.World().getServer()).open(S.Zone())) {
            return false;
        }
        for (Scp1730Fixture Each : S.Plan().fixtures("seal", S.Zone())) {
            if (!(S.World().getBlockState(Scp1730World.world(S.Run(), Each)).getBlock() instanceof Scp1730SealBlock)) {
                return false;
            }
        }
        return true;
    }

    static void stillLocked(Scene S, String Before) {
        S.Helper().assertTrue(locked(S), "the exit opened before " + Before);
        S.Helper().assertTrue(!S.data().Solved, "the puzzle counted as solved before " + Before);
    }

    static void lifted(Scene S) {
        S.Helper().assertTrue(S.data().Solved, "the final step did not solve the " + Scp1730Puzzles.Ids[S.Puzzle()] + " puzzle");
        S.World().getChunkAt(panel(S));
        Scp1730PuzzleDirector.refresh(S.context(), false);
        BlockState Panel = S.World().getBlockState(panel(S));
        S.Helper().assertTrue(Panel.getBlock() instanceof Scp1730LockdownPanelBlock && Panel.getValue(Scp1730LockdownPanelBlock.Lit) == Panel.getValue(Scp1730LockdownPanelBlock.Steps),
                "the lockdown panel does not show every lamp: " + Panel);
        if (S.Zone() < Scp1730Puzzles.DeepZone) {
            S.Helper().assertTrue(Scp1730State.get(S.World().getServer()).open(S.Zone()), "the stairwell seal was not opened");
            S.Helper().runAfterDelay(40, () -> {
                for (Scp1730Fixture Each : S.Plan().fixtures("seal", S.Zone())) {
                    S.Helper().assertTrue(!(S.World().getBlockState(Scp1730World.world(S.Run(), Each)).getBlock() instanceof Scp1730SealBlock), "a stairwell seal block stayed shut");
                }
                finish(S);
            });
        } else {
            S.Helper().assertTrue(!locked(S), "the sanctum gate stayed shut");
            finish(S);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleKeycard")
    public static void puzzleKeycard(GameTestHelper Helper) {
        Scene S = start(Helper, 0, Scp1730Puzzles.Keycard, "scp1730_puzzle_keycard");
        Scp1730Fixture Card = null;
        for (Scp1730Fixture Each : S.Plan().fixtures("card", 0)) {
            if (Each.Data() == 0) {
                Card = Each;
            }
        }
        Helper.assertTrue(Card != null, "the keycard puzzle has no access card");
        BlockPos CardAt = Scp1730World.world(S.Run(), Card);
        click(S, CardAt);
        ItemStack Access = held(S, Scp1730Registry.AccessCard.get());
        click(S, panel(S), Access.copy());
        stillLocked(S, "the override card");
        List<BlockPos> Locks = new ArrayList<>();
        for (Scp1730Fixture Each : S.Plan().fixtures("pseal", 0)) {
            Locks.add(Scp1730World.world(S.Run(), Each));
        }
        Helper.assertTrue(Locks.size() == 9 && S.World().getBlockState(Locks.get(4)).getBlock() instanceof Scp1730SealBlock, "the secure room is not sealed");
        BlockPos Reader = device(S, Scp1730Puzzles.RoleLockerReader);
        click(S, Reader);
        Helper.assertTrue(S.World().getBlockState(Locks.get(4)).getBlock() instanceof Scp1730SealBlock, "the secure room opened without a card");
        click(S, Reader, Access);
        Helper.assertTrue(S.World().getBlockState(Locks.get(4)).isAir() && S.data().has(0), "the access card did not open the secure room");
        stillLocked(S, "the override card was swiped");
        List<BlockPos> Inside = pickups(S);
        Helper.assertTrue(Inside.size() == 1, "the secure room holds " + Inside.size() + " override cards");
        take(S, Inside.getFirst());
        click(S, panel(S), held(S, Scp1730PuzzleRegistry.OverrideCard.get()));
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleFuses")
    public static void puzzleFuses(GameTestHelper Helper) {
        Scene S = start(Helper, 0, Scp1730Puzzles.Fuses, "scp1730_puzzle_fuses");
        List<BlockPos> Fuses = pickups(S);
        Helper.assertTrue(Fuses.size() == 3, "the fuse puzzle has " + Fuses.size() + " fuses");
        for (Scp1730Fixture Each : S.Plan().fixtures("card", 0)) {
            Helper.assertTrue(Each.Data() != 0, "a stray access card remains in a fuse run");
        }
        BlockPos Box = device(S, Scp1730Puzzles.RoleJunction);
        take(S, Fuses.get(0));
        S.data().TakenAt = S.World().getGameTime() - Scp1730PuzzleDirector.RespawnDelay - 1;
        Scp1730PuzzleDirector.respawn(S.context(), List.of(S.User()), S.World().getGameTime());
        Helper.assertTrue(S.World().getBlockState(Fuses.get(0)).isAir(), "a fuse respawned while the player still had it");
        S.User().getInventory().clearContent();
        Scp1730PuzzleDirector.respawn(S.context(), List.of(S.User()), S.World().getGameTime());
        Helper.assertTrue(S.World().getBlockState(Fuses.get(0)).getBlock() instanceof Scp1730PickupBlock, "a lost fuse did not respawn at its spot");
        for (int Index = 0; Index < 3; Index++) {
            take(S, Fuses.get(Index));
        }
        ItemStack All = held(S, Scp1730PuzzleRegistry.Fuse.get());
        Helper.assertTrue(All.getCount() == 3, "the player picked up " + All.getCount() + " fuses");
        stillLocked(S, "any fuse was seated");
        click(S, Box, All.copyWithCount(1));
        click(S, Box, All.copyWithCount(1));
        Helper.assertTrue(S.World().getBlockState(Box).getValue(Scp1730JunctionBoxBlock.Fuses) == 2 && S.data().Counts[0] == 2, "seated fuses were not counted");
        stillLocked(S, "the third fuse");
        click(S, Box, All.copyWithCount(1));
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleOverride")
    public static void puzzleOverride(GameTestHelper Helper) {
        Scene S = start(Helper, 0, Scp1730Puzzles.Override, "scp1730_puzzle_override");
        BlockPos First = device(S, Scp1730Puzzles.RoleTerminalA);
        BlockPos Second = device(S, Scp1730Puzzles.RoleTerminalB);
        int CodeA = extra(S, Scp1730Puzzles.RoleTerminalA);
        int CodeB = extra(S, Scp1730Puzzles.RoleTerminalB);
        Helper.assertTrue(extra(S, Scp1730Puzzles.RoleClueA) == CodeA && extra(S, Scp1730Puzzles.RoleClueB) == CodeB, "the posted clues do not match the terminal codes");
        Helper.assertTrue(S.World().getBlockState(device(S, Scp1730Puzzles.RoleClueA)).getValue(net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730ClueBoardBlock.Clue) == CodeA,
                "the first clue board shows the wrong glyph");
        dial(S, First, (CodeA + 1) % 6);
        S.User().setShiftKeyDown(true);
        click(S, First);
        S.User().setShiftKeyDown(false);
        Helper.assertTrue(!S.World().getBlockState(First).getValue(Scp1730TerminalBlock.Accepted) && S.data().Lockout > S.World().getGameTime(), "a wrong code was not rejected");
        S.data().Lockout = 0L;
        dial(S, First, CodeA);
        S.User().setShiftKeyDown(true);
        click(S, First);
        S.User().setShiftKeyDown(false);
        Helper.assertTrue(S.World().getBlockState(First).getValue(Scp1730TerminalBlock.Accepted), "the right code was refused");
        stillLocked(S, "the second authorisation");
        dial(S, Second, CodeB);
        S.User().setShiftKeyDown(true);
        click(S, Second);
        S.User().setShiftKeyDown(false);
        lifted(S);
    }

    static void dial(Scene S, BlockPos Pos, int Wanted) {
        for (int Turn = 0; Turn < 7 && S.World().getBlockState(Pos).getValue(Scp1730TerminalBlock.Glyph) != Wanted; Turn++) {
            click(S, Pos);
        }
        S.Helper().assertTrue(S.World().getBlockState(Pos).getValue(Scp1730TerminalBlock.Glyph) == Wanted, "the terminal would not dial " + Wanted);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleDualKey")
    public static void puzzleDualKey(GameTestHelper Helper) {
        Scene S = start(Helper, 0, Scp1730Puzzles.DualKey, "scp1730_puzzle_dualkey");
        BlockPos First = device(S, Scp1730Puzzles.RoleKeyA);
        BlockPos Second = device(S, Scp1730Puzzles.RoleKeyB);
        int Window = extra(S, Scp1730Puzzles.RoleKeyB);
        Helper.assertTrue(Window >= 15 && Window <= 60, "the key window is " + Window + " seconds");
        click(S, First);
        Helper.assertTrue(!S.World().getBlockState(First).getValue(Scp1730SwitchBlock.On), "a key switch turned without a key");
        take(S, pickups(S).getFirst());
        ItemStack Key = held(S, Scp1730PuzzleRegistry.DutyKey.get());
        click(S, First, Key.copy());
        Helper.assertTrue(S.World().getBlockState(First).getValue(Scp1730SwitchBlock.On) && S.data().Timer > S.World().getGameTime(), "the first key did not arm");
        stillLocked(S, "the second key");
        S.data().Timer = S.World().getGameTime() + 1L;
        stand(S, First);
        Helper.runAfterDelay(25, () -> {
            Helper.assertTrue(S.data().Progress == 0 && S.data().Timer == 0L, "the key window did not lapse: progress " + S.data().Progress + ", timer " + S.data().Timer);
            Helper.assertTrue(!S.World().getBlockState(First).getValue(Scp1730SwitchBlock.On), "the first key switch still shows turned after the window lapsed");
            S.World().setBlock(First, S.World().getBlockState(First).setValue(Scp1730SwitchBlock.On, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
            click(S, First, Key.copy());
            Helper.assertTrue(S.data().has(0) && S.data().Timer > S.World().getGameTime(), "a switch left showing turned blocked a fresh turn");
            stillLocked(S, "both keys were turned together");
            click(S, Second, Key.copy());
            lifted(S);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleVent")
    public static void puzzleVent(GameTestHelper Helper) {
        Scene S = start(Helper, 1, Scp1730Puzzles.Vent, "scp1730_puzzle_vent");
        BlockPos Grate = device(S, Scp1730Puzzles.RoleVent);
        BlockPos Fan = device(S, Scp1730Puzzles.RoleFan);
        BlockPos Inner = device(S, Scp1730Puzzles.RoleVentInner);
        click(S, Grate);
        Helper.assertTrue(!S.World().getBlockState(Grate).getValue(Scp1730VentGrateBlock.Open), "the grate opened before the interlock");
        click(S, device(S, Scp1730Puzzles.RoleInterlock));
        Helper.assertTrue(S.data().has(0), "the interlock did not register");
        click(S, Grate);
        Helper.assertTrue(S.World().getBlockState(Grate).getValue(Scp1730VentGrateBlock.Open) && S.data().has(1), "the grate did not open after the interlock");
        click(S, Grate);
        Helper.assertTrue(S.User().blockPosition().equals(Grate) && S.User().getPose() == Pose.SWIMMING, "using the open grate did not put the player into the crawlway: "
                + S.User().blockPosition() + " " + S.User().getPose());
        Helper.assertTrue(S.World().getBlockState(Inner).getValue(Scp1730VentGrateBlock.Open), "the fan room side of the shaft is closed");
        Helper.assertTrue(!S.World().getBlockState(Grate.above()).getCollisionShape(S.World(), Grate.above()).isEmpty(), "the crawlway has headroom, so nobody would crawl");
        Direction Out = S.World().getBlockState(Fan).getValue(Scp1730SwitchBlock.FACING);
        BlockPos Room = Fan.relative(Out).below();
        Helper.assertTrue(S.World().getBlockState(Room).getCollisionShape(S.World(), Room).isEmpty() && S.World().getBlockState(Room.above()).getCollisionShape(S.World(), Room.above()).isEmpty(),
                "the fan room has no room to stand up");
        stillLocked(S, "the fan breaker");
        S.User().teleportTo(Room.getX() + 0.5, Room.getY(), Room.getZ() + 0.5);
        click(S, Fan);
        Helper.assertTrue(!S.World().getBlockState(Fan).getValue(Scp1730SwitchBlock.On), "the fan breaker did not cut");
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleSplice")
    public static void puzzleSplice(GameTestHelper Helper) {
        Scene S = start(Helper, 1, Scp1730Puzzles.Splice, "scp1730_puzzle_splice");
        List<BlockPos> Kits = pickups(S);
        Helper.assertTrue(Kits.size() == 2, "the splice puzzle has " + Kits.size() + " kits");
        BlockPos First = device(S, Scp1730Puzzles.RoleCableA);
        BlockPos Second = device(S, Scp1730Puzzles.RoleCableB);
        click(S, First);
        Helper.assertTrue(!S.data().has(0), "a cable was spliced without a kit");
        take(S, Kits.get(0));
        take(S, Kits.get(1));
        ItemStack Pair = held(S, Scp1730PuzzleRegistry.SpliceKit.get());
        Pair.setCount(2);
        click(S, First, Pair.copyWithCount(1));
        Helper.assertTrue(S.data().has(0), "the first splice did not hold");
        stillLocked(S, "the second splice");
        click(S, Second, Pair.copyWithCount(1));
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleRelays")
    public static void puzzleRelays(GameTestHelper Helper) {
        Scene S = start(Helper, 1, Scp1730Puzzles.Relays, "scp1730_puzzle_relays");
        int[] Order = Scp1730Puzzles.Orders[extra(S, Scp1730Puzzles.RoleChart)];
        BlockPos[] Relays = {device(S, Scp1730Puzzles.RoleRelayRed), device(S, Scp1730Puzzles.RoleRelayYellow), device(S, Scp1730Puzzles.RoleRelayBlue)};
        click(S, Relays[Order[0]]);
        click(S, Relays[Order[2]]);
        Helper.assertTrue(S.data().Progress == 0 && S.data().Counts[0] == 0, "a wrong relay did not reset the sequence");
        Helper.assertTrue(!S.World().getBlockState(Relays[Order[0]]).getValue(Scp1730SwitchBlock.On), "the tripped relays stayed thrown");
        S.World().setBlock(Relays[Order[0]], S.World().getBlockState(Relays[Order[0]]).setValue(Scp1730SwitchBlock.On, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
        click(S, Relays[Order[0]]);
        Helper.assertTrue(S.data().Counts[0] == 1, "a relay left showing thrown after a fault blocked the restart");
        click(S, Relays[Order[1]]);
        stillLocked(S, "the last relay");
        click(S, Relays[Order[2]]);
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleSample")
    public static void puzzleSample(GameTestHelper Helper) {
        Scene S = start(Helper, 1, Scp1730Puzzles.Sample, "scp1730_puzzle_sample");
        BlockPos Lever = device(S, Scp1730Puzzles.RoleFlush);
        BlockPos Hatch = device(S, Scp1730Puzzles.RoleHatch);
        BlockState LeverState = S.World().getBlockState(Lever);
        Helper.assertTrue(LeverState.getBlock() instanceof Scp1730ControlBlock && LeverState.getValue(Scp1730ControlBlock.Type) == Scp1730ControlBlock.Kind.Release,
                "the flush lever is not the release lever: " + LeverState);
        click(S, Lever);
        Helper.assertTrue(!S.World().getBlockState(Lever).getValue(Scp1730ControlBlock.Active) && !S.data().has(1), "an empty flush stuck the lever");
        take(S, pickups(S).getFirst());
        Scp1730PuzzleDirector.tick(S.World().getServer());
        Helper.assertTrue(S.User().hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the sample canister did not weigh the player down");
        click(S, Hatch, held(S, Scp1730PuzzleRegistry.SampleCanister.get()));
        Helper.assertTrue(S.data().has(0), "the hatch did not take the sample");
        stillLocked(S, "the flush");
        click(S, Lever);
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleCells")
    public static void puzzleCells(GameTestHelper Helper) {
        Scene S = start(Helper, 2, Scp1730Puzzles.Cells, "scp1730_puzzle_cells");
        click(S, device(S, Scp1730Puzzles.RoleLockA));
        click(S, device(S, Scp1730Puzzles.RoleLockB));
        Helper.assertTrue(S.data().has(0) && S.data().has(1), "the cell locks did not register");
        stillLocked(S, "the third cell");
        click(S, device(S, Scp1730Puzzles.RoleLockC));
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp1730_puzzleGenerator")
    public static void puzzleGenerator(GameTestHelper Helper) {
        Scene S = start(Helper, 2, Scp1730Puzzles.Generator, "scp1730_puzzle_generator");
        BlockPos Engine = device(S, Scp1730Puzzles.RoleGenerator);
        List<BlockPos> Cans = pickups(S);
        Helper.assertTrue(Cans.size() == 2, "the generator puzzle has " + Cans.size() + " fuel cans");
        click(S, Engine);
        Helper.assertTrue(!S.World().getBlockState(Engine).getValue(Scp1730GeneratorBlock.Running), "a dry generator started");
        take(S, Cans.get(0));
        take(S, Cans.get(1));
        ItemStack Fuel = held(S, Scp1730PuzzleRegistry.FuelCanister.get());
        Fuel.setCount(2);
        click(S, Engine, Fuel.copyWithCount(1));
        click(S, Engine, Fuel.copyWithCount(1));
        click(S, Engine);
        Helper.assertTrue(S.World().getBlockState(Engine).getValue(Scp1730GeneratorBlock.Running) && S.data().has(1), "the fuelled generator did not start");
        stillLocked(S, "the override cycle");
        BlockPos Panel = panel(S);
        Direction Out = S.World().getBlockState(Panel).getValue(Scp1730LockdownPanelBlock.FACING);
        BlockPos Stand = Panel.relative(Out).below();
        S.User().teleportTo(Stand.getX() + 0.5, Stand.getY(), Stand.getZ() + 0.5);
        Helper.runAfterDelay(40, () -> {
            int Held = S.data().Hold;
            Helper.assertTrue(Held > 0, "the override cycle did not run with a player at the panel");
            Vec3 Away = Scp1730World.world(S.Run(), S.Plan().entry(2));
            S.User().teleportTo(Away.x, Away.y, Away.z);
            Helper.runAfterDelay(40, () -> {
                Helper.assertTrue(S.data().Hold == Held && Held > 0, "the cycle did not pause when the panel was left: " + S.data().Hold + " vs " + Held);
                stillLocked(S, "the cycle finished");
                S.data().Hold = Scp1730PuzzleDirector.HoldTicks - Scp1730PuzzleDirector.TickEvery;
                S.User().teleportTo(Stand.getX() + 0.5, Stand.getY(), Stand.getZ() + 0.5);
                Helper.runAfterDelay(30, () -> lifted(S));
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleCoolant")
    public static void puzzleCoolant(GameTestHelper Helper) {
        Scene S = start(Helper, 2, Scp1730Puzzles.Coolant, "scp1730_puzzle_coolant");
        BlockPos Inflow = device(S, Scp1730Puzzles.RoleInflow);
        BlockPos Bypass = device(S, Scp1730Puzzles.RoleBypass);
        BlockPos Drain = device(S, Scp1730Puzzles.RoleDrain);
        click(S, Drain);
        click(S, Bypass);
        Helper.assertTrue(S.data().Progress == 0, "a valve turned against the pressure");
        click(S, Inflow);
        click(S, Drain);
        Helper.assertTrue(S.data().has(0) && !S.data().has(2), "the drain turned before the bypass");
        click(S, Bypass);
        stillLocked(S, "the drain");
        click(S, Drain);
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzlePowerCycle")
    public static void puzzlePowerCycle(GameTestHelper Helper) {
        Scene S = start(Helper, 2, Scp1730Puzzles.PowerCycle, "scp1730_puzzle_powercycle");
        BlockPos Breaker = device(S, Scp1730Puzzles.RoleBreaker);
        BlockPos Controller = device(S, Scp1730Puzzles.RoleController);
        click(S, Controller);
        Helper.assertTrue(!S.data().has(1), "the controller reset while powered");
        click(S, Breaker);
        Helper.assertTrue(!Scp1730State.get(S.World().getServer()).powered(2) && S.data().has(0), "the main breaker did not cut the power");
        stillLocked(S, "the controller reset");
        click(S, Controller);
        Helper.assertTrue(S.data().has(1), "the unpowered controller did not reset");
        stillLocked(S, "power came back");
        click(S, Breaker);
        Helper.assertTrue(Scp1730State.get(S.World().getServer()).powered(2), "the breaker did not restore power");
        lifted(S);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzlePylons")
    public static void puzzlePylons(GameTestHelper Helper) {
        Scene S = start(Helper, 3, Scp1730Puzzles.Pylons, "scp1730_puzzle_pylons");
        Scp1730PuzzleDirector.tick(S.World().getServer());
        stillLocked(S, "any pylon turned");
        int[] Roles = {Scp1730Puzzles.RolePylonA, Scp1730Puzzles.RolePylonB, Scp1730Puzzles.RolePylonC};
        int[] Tablets = {Scp1730Puzzles.RoleTabletA, Scp1730Puzzles.RoleTabletB, Scp1730Puzzles.RoleTabletC};
        for (int Index = 0; Index < 3; Index++) {
            Helper.assertTrue(extra(S, Tablets[Index]) == extra(S, Roles[Index]), "tablet " + Index + " does not show its pylon's glyph");
        }
        for (int Index = 0; Index < 2; Index++) {
            align(S, device(S, Roles[Index]), extra(S, Roles[Index]));
        }
        Helper.assertTrue(S.data().has(0) && S.data().has(1), "aligned pylons did not register");
        click(S, device(S, Roles[0]));
        Helper.assertTrue(!S.data().has(0), "turning a pylon out of line kept its lamp");
        align(S, device(S, Roles[0]), extra(S, Roles[0]));
        stillLocked(S, "the last pylon");
        align(S, device(S, Roles[2]), extra(S, Roles[2]));
        lifted(S);
    }

    static void align(Scene S, BlockPos Pos, int Wanted) {
        for (int Turn = 0; Turn < 7 && !(S.World().getBlockState(Pos).getValue(Scp1730PylonBlock.Glyph) == Wanted && S.World().getBlockState(Pos).getValue(Scp1730PylonBlock.Lit)); Turn++) {
            click(S, Pos);
        }
        S.Helper().assertTrue(S.World().getBlockState(Pos).getValue(Scp1730PylonBlock.Lit), "the pylon never lit at glyph " + Wanted);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_puzzleSluices")
    public static void puzzleSluices(GameTestHelper Helper) {
        Scene S = start(Helper, 3, Scp1730Puzzles.Sluices, "scp1730_puzzle_sluices");
        Scp1730PuzzleDirector.tick(S.World().getServer());
        stillLocked(S, "any sluice turned");
        BlockPos First = device(S, Scp1730Puzzles.RoleSluiceA);
        BlockPos Second = device(S, Scp1730Puzzles.RoleSluiceB);
        for (int Turn = 0; Turn < Scp1730Puzzles.Cranks; Turn++) {
            click(S, First);
        }
        Helper.assertTrue(S.data().has(0), "the first sluice did not open after " + Scp1730Puzzles.Cranks + " cranks");
        for (int Turn = 0; Turn < Scp1730Puzzles.Cranks - 1; Turn++) {
            click(S, Second);
        }
        stillLocked(S, "the last crank");
        click(S, Second);
        lifted(S);
    }
}
