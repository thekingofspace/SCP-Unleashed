package net.rai.scp_unleashed.npc.doors.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.guards.GuardDuty;
import net.rai.scp_unleashed.guards.GuardRules;
import net.rai.scp_unleashed.guards.PatrolPoint;
import net.rai.scp_unleashed.guards.PatrolRoute;
import net.rai.scp_unleashed.npc.doors.NpcDoorAccess;
import net.rai.scp_unleashed.npc.doors.NpcDoorNavigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class NpcDoorPathGameTests {
    public static final String Arena = "guards_arena";
    public static final int West = 2;
    public static final int East = 21;
    public static final int North = 2;
    public static final int South = 21;
    public static final int WallZ = 12;
    public static final int DoorX = 6;
    public static final int GapX = 13;
    public static final int StartZ = 5;
    public static final int GoalZ = 18;
    public static final double Near = 3.0;

    private NpcDoorPathGameTests() {
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(23, Y, 23)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(23, Y + 1, 23)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void layout(GameTestHelper Helper, int F, boolean Detour) {
        for (int Y = F; Y < F + 4; Y++) {
            for (int X = West; X <= East; X++) {
                Helper.setBlock(new BlockPos(X, Y, North), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Y, South), Blocks.STONE);
                boolean Gap = Detour && (X == GapX || X == GapX + 1);
                Helper.setBlock(new BlockPos(X, Y, WallZ), Gap ? Blocks.AIR : Blocks.STONE);
            }
            for (int Z = North; Z <= South; Z++) {
                Helper.setBlock(new BlockPos(West, Y, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(East, Y, Z), Blocks.STONE);
            }
        }
        PanelDoorBlock Frame = DoorRegistry.SingleSlidingDoor.get();
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(new BlockPos(DoorX, F, WallZ));
        DoorKind Kind = Frame.getKind();
        BlockState State = Frame.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.NORTH);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Base, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part));
            World.setBlock(Cell, State.setValue(Frame.partProperty(), Part), Block.UPDATE_ALL);
        }
    }

    private static void button(GameTestHelper Helper, int F, boolean NearSide) {
        int Z = NearSide ? WallZ - 1 : WallZ + 1;
        Direction Facing = NearSide ? Direction.NORTH : Direction.SOUTH;
        Helper.setBlock(new BlockPos(DoorX + 1, F + 1, Z), DoorRegistry.DoorButton.get().defaultBlockState().setValue(DoorButtonBlock.FACING, Facing));
    }

    private static void scanner(GameTestHelper Helper, int F) {
        Helper.setBlock(new BlockPos(DoorX + 1, F + 1, WallZ - 1), DoorRegistry.KeycardScanner.get().defaultBlockState()
                .setValue(KeycardScannerBlock.FACING, Direction.NORTH).setValue(KeycardScannerBlock.Clearance, 2));
    }

    private static DClass body(GameTestHelper Helper, int F) {
        DClass Body = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(DoorX + 0.5, F, StartZ + 0.5));
        Body.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Body.setPersistenceRequired();
        return Body;
    }

    private static FakePlayer leader(GameTestHelper Helper, int F, DClass Body) {
        ServerLevel World = Helper.getLevel();
        FakePlayer Leader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "door_leader"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(DoorX + 0.5, F, GoalZ + 0.5));
        Leader.moveTo(Spot.x, Spot.y, Spot.z, 180.0f, 0.0f);
        World.addNewPlayer(Leader);
        DClassDuties.orderFollow(Body, Leader);
        return Leader;
    }

    private static List<Double> crossings(GameTestHelper Helper, Entity Body) {
        List<Double> Found = new ArrayList<>();
        double[] Last = {Helper.relativeVec(Body.position()).z};
        Helper.onEachTick(() -> {
            Vec3 Now = Helper.relativeVec(Body.position());
            double Plane = WallZ + 0.5;
            if ((Last[0] - Plane) * (Now.z - Plane) < 0.0) {
                Found.add(Now.x);
            }
            Last[0] = Now.z;
        });
        return Found;
    }

    private static boolean through(List<Double> Found, double Low, double High) {
        return !Found.isEmpty() && Found.get(0) >= Low && Found.get(0) <= High;
    }

    private static String where(GameTestHelper Helper, DClass Body) {
        Path Route = Body.getNavigation().getPath();
        String Info = Route == null ? "no path" : "path reaches " + Route.canReach() + " at node " + Route.getNextNodeIndex() + " of " + Route.getNodeCount();
        return Helper.relativeVec(Body.position()) + " " + Info + " task " + DClassDuties.task(Body);
    }

    private static double flat(GameTestHelper Helper, Entity Body, double X, double Z) {
        Vec3 Here = Helper.relativeVec(Body.position());
        return Math.sqrt(Math.pow(Here.x - X, 2) + Math.pow(Here.z - Z, 2));
    }

    private static void clean(GameTestHelper Helper, ServerPlayer... Leaders) {
        for (ServerPlayer Leader : Leaders) {
            Helper.getLevel().removePlayerImmediately(Leader, Entity.RemovalReason.DISCARDED);
        }
        AABB Area = Helper.getBounds().inflate(8.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, Area, Candidate -> !(Candidate instanceof Player))) {
            Found.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "npcdoorpath_button")
    public static void guardOpensButtonDoorOverDetour(GameTestHelper Helper) {
        int F = floor(Helper);
        layout(Helper, F, true);
        button(Helper, F, true);
        DClass Guard = body(Helper, F);
        Helper.assertTrue(Guard.getNavigation() instanceof NpcDoorNavigation, "D-Class do not use the door aware navigation");
        BlockPos Post = Helper.absolutePos(new BlockPos(DoorX, F, GoalZ));
        GuardDuty.assign(Guard, new PatrolRoute(UUID.randomUUID(), Helper.getLevel().dimension(), Post, List.of(new PatrolPoint(Post, 20, Direction.NORTH)), false,
                GuardRules.Default, Optional.empty(), ""));
        List<Double> Found = crossings(Helper, Guard);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, DoorX + 0.5, GoalZ + 0.5) < 1.5, "the guard never reached the post past the door, at " + where(Helper, Guard)))
                .thenExecute(() -> Helper.assertTrue(through(Found, DoorX - 0.2, DoorX + 1.2), "the guard walked around instead of using the door: " + Found))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "npcdoorpath_card")
    public static void followsThroughCardDoor(GameTestHelper Helper) {
        int F = floor(Helper);
        layout(Helper, F, true);
        scanner(Helper, F);
        DClass Body = body(Helper, F);
        Body.main().set(5, new ItemStack(DoorRegistry.KeycardLevel3.get()));
        FakePlayer Leader = leader(Helper, F, Body);
        List<Double> Found = crossings(Helper, Body);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Body, DoorX + 0.5, GoalZ + 0.5) < Near, "the D-Class never followed through the keycard door, at " + where(Helper, Body)))
                .thenExecute(() -> Helper.assertTrue(through(Found, DoorX - 0.2, DoorX + 1.2), "the D-Class walked around instead of swiping: " + Found))
                .thenExecute(() -> Helper.assertTrue(Body.main().get(5).is(DoorRegistry.KeycardLevel3.get()), "the D-Class lost its keycard"))
                .thenExecute(() -> clean(Helper, Leader))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "npcdoorpath_nocard")
    public static void detoursWithoutCard(GameTestHelper Helper) {
        int F = floor(Helper);
        layout(Helper, F, true);
        scanner(Helper, F);
        DClass Body = body(Helper, F);
        Body.main().set(5, new ItemStack(DoorRegistry.KeycardLevel1.get()));
        FakePlayer Leader = leader(Helper, F, Body);
        List<Double> Found = crossings(Helper, Body);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Body, DoorX + 0.5, GoalZ + 0.5) < Near, "the D-Class never found the way around, at " + where(Helper, Body)))
                .thenExecute(() -> Helper.assertTrue(through(Found, GapX - 0.2, GapX + 2.2), "the D-Class did not take the way around: " + Found))
                .thenExecute(() -> Helper.assertFalse(NpcDoorAccess.anyOpen(Helper.getLevel(), List.of(Helper.absolutePos(new BlockPos(DoorX, F, WallZ)))),
                        "the door opened without a good card"))
                .thenExecute(() -> clean(Helper, Leader))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "npcdoorpath_sealed")
    public static void givesUpAtSealedDoor(GameTestHelper Helper) {
        int F = floor(Helper);
        layout(Helper, F, false);
        button(Helper, F, false);
        DClass Body = body(Helper, F);
        FakePlayer Leader = leader(Helper, F, Body);
        List<Double> Found = crossings(Helper, Body);
        BlockPos Door = Helper.absolutePos(new BlockPos(DoorX, F, WallZ));
        Helper.startSequence()
                .thenExecuteFor(400, () -> {
                    Helper.assertTrue(Found.isEmpty(), "the D-Class got through a door it cannot open");
                    Helper.assertFalse(NpcDoorAccess.anyOpen(Helper.getLevel(), List.of(Door)), "the D-Class opened a door from the wrong side");
                })
                .thenExecute(() -> Helper.assertTrue(Helper.relativeVec(Body.position()).z < WallZ, "the D-Class left the near side"))
                .thenExecute(() -> clean(Helper, Leader))
                .thenSucceed();
    }
}
