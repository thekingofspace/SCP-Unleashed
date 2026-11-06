package net.rai.scp_unleashed.scp4666.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.gateway.gametest.GatewayTraveller;
import net.rai.scp_unleashed.scp4666.Scp4666Boundary;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666ClockModel;
import net.rai.scp_unleashed.scp4666.Scp4666Heat;
import net.rai.scp_unleashed.scp4666.Scp4666Builder;
import net.rai.scp_unleashed.scp4666.Scp4666Clock;
import net.rai.scp_unleashed.scp4666.Scp4666Cold;
import net.rai.scp_unleashed.scp4666.Scp4666Commands;
import net.rai.scp_unleashed.scp4666.Scp4666Cues;
import net.rai.scp_unleashed.scp4666.Scp4666Destination;
import net.rai.scp_unleashed.scp4666.Scp4666Director;
import net.rai.scp_unleashed.scp4666.Scp4666Endings;
import net.rai.scp_unleashed.scp4666.Scp4666Happenings;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Objectives;
import net.rai.scp_unleashed.scp4666.Scp4666Pose;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666Seal;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666CalendarBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CellarDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CoatHooksBlock;
import net.rai.scp_unleashed.scp4666.item.Scp4666WinterGearItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.rai.scp_unleashed.scp4666.block.Scp4666ExitDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666StoveBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666WindowBlock;
import net.rai.scp_unleashed.scp4666.item.Scp4666ChoreItem;
import net.rai.scp_unleashed.scp4666.block.Scp4666FurnitureBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666GramophoneBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666GramophoneBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666LampBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666OwnedBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666SwitchBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666TreeBlock;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp4666GameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final int Long = 900;

    private Scp4666GameTests() {
    }

    private static GatewayDestination winter() {
        return Scp4666Destination.standIn(Level.NETHER);
    }

    private static boolean Prepared;

    private static ServerLevel prepare(GameTestHelper Helper) {
        winter();
        ServerLevel World = Scp4666World.level(Helper.getLevel().getServer());
        fresh(World);
        if (!Prepared) {
            Prepared = true;
            Scp4666Map Layout = Scp4666Map.get(World.getServer());
            int Reach = Math.floorDiv(Scp4666World.RegionRadius, 16) + 1;
            for (int X = -Reach; X <= Reach; X++) {
                for (int Z = -Reach; Z <= Reach; Z++) {
                    World.setChunkForced((Layout.Centre.getX() >> 4) + X, (Layout.Centre.getZ() >> 4) + Z, true);
                }
            }
            Scp4666Director.reset(World);
        }
        Scp4666World.ensureBuilt(World);
        GatewayStash Stash = GatewayStash.get(World.getServer());
        for (GatewayStash.Entry Entry : Stash.entries()) {
            if (Entry.Destination.equals(Scp4666Destination.StandinId) && World.getServer().getPlayerList().getPlayer(Entry.Player) == null) {
                Stash.discard(Entry.Player);
            }
        }
        tidy(World);
        for (ServerPlayer Left : new ArrayList<>(World.players())) {
            Left.serverLevel().removePlayerImmediately(Left, Entity.RemovalReason.DISCARDED);
        }
        for (Entity Found : World.getAllEntities()) {
            if (Found instanceof Scp4666YuleMan) {
                Found.discard();
            }
        }
        return World;
    }

    private static void fresh(ServerLevel World) {
        Scp4666Cues.clear(World.getServer());
        Scp4666Endings.clear(World.getServer());
    }

    private static void tidy(ServerLevel World) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Director.restorePrints(World, State);
        for (BlockPos Spot : Layout.Presents) {
            if (World.getBlockState(Spot).is(Scp4666Registry.Present.get())) {
                World.setBlock(Spot, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (BlockPos Spot : Layout.Stockings) {
            if (World.getBlockState(Spot).is(Scp4666Registry.Stocking.get())) {
                World.setBlock(Spot, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (World.getBlockState(Layout.ExitDoor).is(Scp4666Registry.ExitDoor.get())) {
            World.setBlock(Layout.ExitDoor.above(), Blocks.AIR.defaultBlockState(), 18);
            World.setBlock(Layout.ExitDoor, Blocks.SNOW.defaultBlockState(), 18);
        }
        BlockState Door = World.getBlockState(Layout.FrontDoor);
        if (Door.getBlock() instanceof DoorBlock Front && Door.getValue(DoorBlock.OPEN)) {
            Front.setOpen(null, World, Door, Layout.FrontDoor, false);
        }
        BlockState Cellar = World.getBlockState(Layout.CellarDoor);
        if (Cellar.getBlock() instanceof DoorBlock Hatch && Cellar.getValue(DoorBlock.OPEN)) {
            Hatch.setOpen(null, World, Cellar, Layout.CellarDoor, false);
        }
        Scp4666Director.setTree(World, Layout, State, Scp4666State.TreeFull);
        State.setLights(true);
        State.clearCold();
        Scp4666Cold.clear(World.getServer());
        for (int Index = 0; Index < Layout.Windows.size(); Index++) {
            if (State.cracked().contains(Index) || State.boarded().contains(Index)) {
                Scp4666Chores.setWindow(World, Layout, State, Index, false, false);
            }
        }
        if (World.getBlockState(Layout.Offering).is(Scp4666Registry.Parcel.get())) {
            World.setBlock(Layout.Offering, Blocks.AIR.defaultBlockState(), 3);
        }
        State.setOffering(false);
        State.setActivity(0.0f);
        State.setFire(Scp4666State.StartFire);
        State.setCoal(0);
        State.clearDaily();
        Scp4666Heat.refresh(World, Layout, State);
        Scp4666Chores.clear(World.getServer());
    }

    private static void expect(ServerLevel World, BlockPos Pos, Class<?> Kind, String Name, List<String> Missing) {
        if (!Kind.isInstance(World.getBlockState(Pos).getBlock())) {
            Missing.add(Name + " at " + Pos.toShortString() + " is " + World.getBlockState(Pos));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_cabin")
    public static void everyAnchorIsInPlace(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        List<String> Missing = new ArrayList<>();
        expect(World, Layout.Tree, Scp4666TreeBlock.class, "tree", Missing);
        expect(World, Layout.Fireplace, Scp4666FireplaceBlock.class, "fireplace", Missing);
        expect(World, Layout.Heater, Scp4666HeaterBlock.class, "boiler", Missing);
        expect(World, Layout.Heater.above(), Scp4666HeaterBlock.class, "boiler top", Missing);
        if (!World.getBlockState(Layout.Offering).isAir()) {
            Missing.add("the gift spot by the tree is taken by " + World.getBlockState(Layout.Offering));
        }
        for (BlockPos Spot : List.of(Layout.CoalBin, Layout.ChoppingBlock, Layout.Workbench)) {
            expect(World, Spot, Scp4666FurnitureBlock.class, "chore furniture", Missing);
        }
        expect(World, Layout.Gramophone, Scp4666GramophoneBlock.class, "gramophone", Missing);
        expect(World, Layout.Calendar, Scp4666CalendarBlock.class, "calendar", Missing);
        expect(World, Layout.Switch, Scp4666SwitchBlock.class, "switch", Missing);
        expect(World, Layout.CellarDoor, Scp4666CellarDoorBlock.class, "cellar door", Missing);
        expect(World, Layout.CellarDoor.above(), Scp4666CellarDoorBlock.class, "cellar door top", Missing);
        expect(World, Layout.CellarDoor.above(2), RotatedPillarBlock.class, "lintel over the cellar door", Missing);
        if (!(World.getBlockState(Layout.Gramophone.below()).getBlock() instanceof Scp4666FurnitureBlock)) {
            Missing.add("the gramophone is not on a table");
        }
        for (int X = Layout.PorchMinX + 1; X < Layout.PorchMaxX; X++) {
            for (int Z = Layout.PorchMinZ; Z < Layout.PorchMaxZ; Z++) {
                BlockPos Deck = new BlockPos(X, Layout.Floor, Z);
                if (!World.getBlockState(Deck).isFaceSturdy(World, Deck, Direction.UP)) {
                    Missing.add("porch deck missing at " + Deck.toShortString());
                }
            }
        }
        if (!World.getBlockState(Layout.BoxSpot).isAir() && !World.getBlockState(Layout.BoxSpot).is(Scp4666Registry.Box.get())) {
            Missing.add("the box spot is blocked by " + World.getBlockState(Layout.BoxSpot));
        }
        expect(World, Layout.AtticHatch, TrapDoorBlock.class, "attic hatch", Missing);
        expect(World, Layout.FrontDoor, DoorBlock.class, "front door", Missing);
        expect(World, Layout.SideDoor, DoorBlock.class, "side door", Missing);
        for (BlockPos Spot : Layout.Lamps) {
            expect(World, Spot, Scp4666LampBlock.class, "lamp", Missing);
        }
        for (Scp4666Map.Bed Entry : Layout.Beds) {
            expect(World, Entry.Head(), BedBlock.class, "bed", Missing);
        }
        for (BlockPos Step : Layout.BasementStairs) {
            expect(World, Step, StairBlock.class, "cellar step", Missing);
        }
        for (Scp4666Map.Window Entry : Layout.Windows) {
            Direction Along = Entry.Out().getClockWise();
            for (int Index = 0; Index < Entry.Width(); Index++) {
                for (int Row = 0; Row < Entry.Height(); Row++) {
                    expect(World, Entry.Pos().relative(Along, Index).above(Row), IronBarsBlock.class, "window " + Entry.Room(), Missing);
                }
            }
            if (!World.getBlockState(BlockPos.containing(Entry.outside(0.75))).isAir()) {
                Missing.add("something stands where SCP-4666 stares in at the " + Entry.Room() + " window");
            }
        }
        Vec3 Wake = Vec3.atBottomCenterOf(Layout.BasementWake);
        Vec3 Middle = Vec3.atBottomCenterOf(Layout.BasementCentre);
        Vec3 Beyond = Middle.add(Middle.subtract(Wake).normalize().scale(1.3));
        for (int Index = 0; Index <= 12; Index++) {
            Vec3 Point = Index <= 10 ? Wake.lerp(Middle, Index / 10.0) : Middle.lerp(Beyond, (Index - 10) / 2.0);
            for (int Rise = 0; Rise < 4; Rise++) {
                BlockPos Spot = BlockPos.containing(Point.x, Layout.BasementMinY + 1 + Rise, Point.z);
                if (!World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty() || World.getBlockState(Spot).getBlock() instanceof Scp4666LampBlock) {
                    Missing.add("no headroom for the loom at " + Spot.toShortString());
                }
            }
        }
        Helper.assertTrue(Missing.isEmpty(), "cabin anchors: " + String.join("; ", Missing));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_cellar")
    public static void theCellarDoorSwingsAndHolds(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GatewayTraveller User = enter(Helper, "scp4666_cellarer", 4.5, 4.5);
        BlockPos Pos = Layout.CellarDoor;
        place(User, indoors(Layout, Pos.getX() + 0.5, Pos.getZ() + 1.5), 180.0f);
        BlockState Door = World.getBlockState(Pos);
        Helper.assertTrue(Door.getBlock() instanceof Scp4666CellarDoorBlock && !Door.getValue(DoorBlock.OPEN), "no shut cellar door at the stairs");
        Door.useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.SOUTH, Pos, false));
        Helper.assertTrue(World.getBlockState(Pos).getValue(DoorBlock.OPEN) && World.getBlockState(Pos.above()).getValue(DoorBlock.OPEN), "the cellar door did not swing open");
        World.getBlockState(Pos).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.SOUTH, Pos, false));
        Helper.assertTrue(!World.getBlockState(Pos).getValue(DoorBlock.OPEN), "the cellar door did not shut again");
        BlockEvent.BreakEvent Breaking = NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Pos, World.getBlockState(Pos), User));
        Helper.assertTrue(Breaking.isCanceled(), "the cellar door could be broken");
        Scp4666Happenings.cellarAjar(World, Layout);
        Helper.assertTrue(World.getBlockState(Pos).getValue(DoorBlock.OPEN), "the cellar door was not found standing open");
        Helper.assertTrue(Scp4666Commands.trigger(World, "basement"), "nothing breathed below");
        Helper.succeed();
    }

    private static List<BlockPos> cellarRoute(Scp4666Map Layout) {
        List<BlockPos> Route = new ArrayList<>(Layout.BasementStairs);
        Collections.reverse(Route);
        return Route;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_stairs")
    public static void theCellarStairsCanBeWalked(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        List<String> Blocked = new ArrayList<>();
        List<BlockPos> Route = cellarRoute(Layout);
        int Feet = Layout.CellarDoor.getY();
        BlockPos Last = Route.get(Route.size() - 1);
        Direction Down = Direction.getNearest(Last.getX() - Route.get(Route.size() - 2).getX(), 0, Last.getZ() - Route.get(Route.size() - 2).getZ());
        List<BlockPos> Steps = new ArrayList<>(Route);
        Steps.add(Last.relative(Down).below());
        for (BlockPos Step : Steps) {
            for (int Y = Step.getY() + 1; Y <= Math.max(Step.getY() + 2, Feet + 2); Y++) {
                BlockPos Spot = new BlockPos(Step.getX(), Y, Step.getZ());
                if (!World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty()) {
                    Blocked.add(Spot.toShortString() + " " + World.getBlockState(Spot));
                }
            }
            Feet = Step.getY();
        }
        Helper.assertTrue(Blocked.isEmpty(), "the cellar stairs are blocked at " + String.join("; ", Blocked));
        GatewayTraveller User = enter(Helper, "scp4666_cellar_walker", 4.5, 4.5);
        BlockPos First = Route.get(0);
        place(User, new Vec3(First.getX() + 0.5, First.getY() + 0.5, First.getZ() + 0.5), 0.0f);
        Vec3 Bottom = Vec3.atBottomCenterOf(Steps.get(Steps.size() - 1).above());
        walk(User, Bottom, 120);
        Helper.assertTrue(User.position().distanceTo(Bottom) < 0.8, "could not walk down the cellar stairs: stuck at " + User.position());
        Vec3 Top = Vec3.atBottomCenterOf(First.above()).add(0.0, -0.5, 0.0);
        walk(User, Top, 160);
        Helper.assertTrue(User.getY() > First.getY() + 0.3 && Math.abs(User.getZ() - (First.getZ() + 0.5)) < 0.9, "could not climb back up the cellar stairs: stuck at " + User.position());
        Vec3 Path = new Vec3(Layout.FrontDoor.getX() + 0.5, Layout.Ground + 1.2, Layout.PorchMaxZ + 2.5);
        Vec3 Door = new Vec3(Layout.FrontDoor.getX() + 0.5, Layout.Floor + 1.0, Layout.FrontDoor.getZ() + 1.5);
        place(User, Path, 180.0f);
        walk(User, Door, 120);
        Helper.assertTrue(User.position().distanceTo(Door) < 0.8, "could not walk up the porch steps to the front door: stuck at " + User.position());
        Helper.succeed();
    }

    private static void walk(GatewayTraveller User, Vec3 Target, int Ticks) {
        for (int Tick = 0; Tick < Ticks; Tick++) {
            Vec3 Flat = new Vec3(Target.x - User.getX(), 0.0, Target.z - User.getZ());
            if (Flat.length() < 0.05) {
                User.move(MoverType.SELF, new Vec3(0.0, -0.2, 0.0));
                continue;
            }
            Vec3 Push = Flat.length() > 0.15 ? Flat.normalize().scale(0.15) : Flat;
            User.move(MoverType.SELF, new Vec3(Push.x, -0.2, Push.z));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_trapdoors")
    public static void everyTrapdoorSitsRight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = Scp4666State.get(World.getServer());
        List<String> Wrong = trapdoorProblems(World, Layout, "fresh cabin");
        Scp4666Chores.crack(World, Layout, State, 0);
        Scp4666Chores.setWindow(World, Layout, State, 0, false, true);
        Wrong.addAll(trapdoorProblems(World, Layout, "after a crack and boards"));
        Scp4666Chores.setWindow(World, Layout, State, 0, false, false);
        Wrong.addAll(trapdoorProblems(World, Layout, "after the glass came back"));
        Helper.assertTrue(Wrong.isEmpty(), "trapdoors: " + String.join("; ", Wrong));
        Helper.succeed();
    }

    private static boolean flush(ServerLevel World, BlockPos Spot, BlockState Found) {
        if (!(Found.getBlock() instanceof TrapDoorBlock) || !Found.getValue(TrapDoorBlock.OPEN)) {
            return false;
        }
        Direction Facing = Found.getValue(TrapDoorBlock.FACING);
        BlockPos Wall = Spot.relative(Facing.getOpposite());
        return World.getBlockState(Wall).isFaceSturdy(World, Wall, Facing) && Found.getShape(World, Spot).bounds().min(Facing.getAxis())
                == (Facing.getOpposite().getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0.0 : 13.0 / 16.0);
    }

    private static boolean sill(ServerLevel World, Scp4666Map Layout, BlockPos Spot) {
        BlockState Found = World.getBlockState(Spot);
        if (!flush(World, Spot, Found) || Layout.insideCabin(Vec3.atCenterOf(Spot))) {
            return false;
        }
        for (Scp4666Map.Window Each : Layout.Windows) {
            if (Each.covers(Spot.above().relative(Each.Out().getOpposite())) && Found.getValue(TrapDoorBlock.FACING) == Each.Out()) {
                return true;
            }
        }
        return false;
    }

    private static boolean ledge(ServerLevel World, Scp4666Map Layout, BlockPos Spot) {
        BlockState Found = World.getBlockState(Spot);
        if (!flush(World, Spot, Found) || !Layout.insideCabin(Vec3.atCenterOf(Spot))) {
            return false;
        }
        for (Scp4666Map.Window Each : Layout.Windows) {
            BlockPos Glass = Spot.above().relative(Each.Out());
            if (Each.covers(Glass) && Glass.getY() == Each.Pos().getY() && Found.getValue(TrapDoorBlock.FACING) == Each.Out().getOpposite()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> windowFrameProblems(ServerLevel World, Scp4666Map Layout, String When) {
        List<String> Wrong = new ArrayList<>();
        Set<BlockPos> Outer = new HashSet<>();
        Set<BlockPos> Inner = new HashSet<>();
        for (Scp4666Map.Window Each : Layout.Windows) {
            for (BlockPos Cell : Each.cells()) {
                if (Cell.getY() != Each.Pos().getY()) {
                    continue;
                }
                BlockPos Below = Cell.below();
                if (!World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
                    Wrong.add(When + " nothing solid under the glass at " + Cell.toShortString());
                }
                Outer.add(Below.relative(Each.Out()));
                Inner.add(Below.relative(Each.Out().getOpposite()));
            }
            Direction Along = Each.Out().getClockWise();
            for (BlockPos End : List.of(Each.Pos().relative(Along, -1), Each.Pos().relative(Along, Each.Width()))) {
                BlockState Under = World.getBlockState(End.relative(Each.Out()).below());
                if (Under.getBlock() instanceof TrapDoorBlock) {
                    Wrong.add(When + " a trapdoor sits under the shutter at " + End.relative(Each.Out()).toShortString());
                }
            }
        }
        if (!Outer.equals(new HashSet<>(Layout.Sills))) {
            Wrong.add(When + " outer sills " + Layout.Sills + " are not exactly under the glass " + Outer);
        }
        for (BlockPos Spot : Layout.InnerSills) {
            if (!Inner.contains(Spot)) {
                Wrong.add(When + " inner sill " + Spot.toShortString() + " is not under a window");
            } else if (!ledge(World, Layout, Spot)) {
                Wrong.add(When + " inner sill " + Spot.toShortString() + " is " + World.getBlockState(Spot));
            }
        }
        if (Layout.InnerSills.size() < Layout.Windows.size()) {
            Wrong.add(When + " only " + Layout.InnerSills.size() + " inner sills");
        }
        return Wrong;
    }

    private static List<String> trapdoorProblems(ServerLevel World, Scp4666Map Layout, String When) {
        List<String> Wrong = windowFrameProblems(World, Layout, When);
        int Shutters = 0;
        int Sills = 0;
        for (BlockPos Spot : Layout.Sills) {
            if (sill(World, Layout, Spot)) {
                Sills++;
            } else {
                Wrong.add(When + " sill " + Spot.toShortString() + " " + World.getBlockState(Spot));
            }
        }
        for (int X = Layout.CabinMinX - 2; X <= Layout.CabinMaxX + 2; X++) {
            for (int Z = Layout.CabinMinZ - 2; Z <= Layout.CabinMaxZ + 2; Z++) {
                for (int Y = Layout.BasementMinY; Y <= Layout.CabinMaxY; Y++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    BlockState Found = World.getBlockState(Spot);
                    if (!(Found.getBlock() instanceof TrapDoorBlock) || Layout.Sills.contains(Spot) || Layout.InnerSills.contains(Spot)) {
                        continue;
                    }
                    if (Spot.equals(Layout.AtticHatch)) {
                        if (Found.getValue(TrapDoorBlock.OPEN) || Found.getValue(TrapDoorBlock.HALF) != Half.TOP) {
                            Wrong.add(When + " hatch " + Found);
                        }
                        continue;
                    }
                    Direction Out = Found.getValue(TrapDoorBlock.FACING);
                    BlockPos Wall = Spot.relative(Out.getOpposite());
                    boolean Flush = Found.getValue(TrapDoorBlock.OPEN) && Found.getValue(TrapDoorBlock.HALF) == Half.BOTTOM && World.getBlockState(Wall).isFaceSturdy(World, Wall, Out)
                            && Found.getShape(World, Spot).bounds().min(Out.getOpposite().getAxis()) == (Out.getOpposite().getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0.0 : 13.0 / 16.0);
                    if (!Flush || Layout.insideCabin(Vec3.atCenterOf(Spot))) {
                        Wrong.add(When + " " + Spot.toShortString() + " " + Found);
                    } else {
                        Shutters++;
                    }
                }
            }
        }
        int Widths = 0;
        for (Scp4666Map.Window Each : Layout.Windows) {
            Widths += Each.Width();
        }
        if (Sills != Widths || Layout.Sills.size() != Widths) {
            Wrong.add(When + " " + Sills + " sills of " + Widths + " window columns");
        }
        if (Shutters < 2 * Layout.Windows.size()) {
            Wrong.add(When + " only " + Shutters + " shutters");
        }
        return Wrong;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_snowsupport")
    public static void noSnowFloats(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        List<String> Floating = new ArrayList<>();
        List<String> Undergrowth = new ArrayList<>();
        int Checked = 0;
        int Reach = Scp4666World.RegionRadius;
        for (int X = Layout.Centre.getX() - Reach; X <= Layout.Centre.getX() + Reach; X++) {
            for (int Z = Layout.Centre.getZ() - Reach; Z <= Layout.Centre.getZ() + Reach; Z++) {
                for (int Y = Layout.Ground; Y <= Layout.Ground + Scp4666Builder.ClearAbove; Y++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    BlockState Found = World.getBlockState(Spot);
                    if (Found.is(Blocks.DEAD_BUSH) || Found.is(Blocks.SPRUCE_SAPLING) || Found.is(Blocks.SWEET_BERRY_BUSH)) {
                        Undergrowth.add(Spot.toShortString() + " " + Found.getBlock());
                        continue;
                    }
                    if (!Found.is(Blocks.SNOW)) {
                        continue;
                    }
                    Checked++;
                    if (!Found.canSurvive(World, Spot)) {
                        Floating.add(Spot.toShortString() + " over " + World.getBlockState(Spot.below()));
                    }
                }
            }
        }
        Helper.assertTrue(Undergrowth.isEmpty(), "undergrowth breaking the snow: " + Undergrowth.size() + " " + String.join("; ", Undergrowth.subList(0, Math.min(6, Undergrowth.size()))));
        Helper.assertTrue(Checked > 2000 && Floating.isEmpty(), "snow layers checked " + Checked + ", floating " + Floating.size() + ": "
                + String.join("; ", Floating.subList(0, Math.min(6, Floating.size()))));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_nosnowfall")
    public static void noNewSnowSettles(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        BlockPos Column = new BlockPos(Layout.Centre.getX() + 16, Layout.Ground + 1, Layout.Centre.getZ() - 16);
        BlockState Before = World.getBlockState(Column);
        long Calls = Scp4666World.stilled();
        for (int Index = 0; Index < 64; Index++) {
            World.tickPrecipitation(Column);
        }
        Helper.assertTrue(Scp4666World.stilled() - Calls == 64, "precipitation was not stilled in the winter: " + (Scp4666World.stilled() - Calls) + " of 64");
        Helper.assertTrue(World.getBlockState(Column) == Before, "snow settled: " + Before + " became " + World.getBlockState(Column));
        Helper.assertTrue(!Scp4666World.still(World.getServer().overworld(), Column), "the overworld would stop snowing too");
        Helper.assertTrue(!Scp4666World.standingIn() || !Scp4666World.still(World, Column.offset(Scp4666World.RegionRadius * 4, 0, 0)), "the stand-in stops snowing far from the winter");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_cold_assign")
    public static void extraVisitorsAreSentOut(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        List<GatewayTraveller> Bedded = new ArrayList<>();
        for (int Index = 0; Index < Layout.Beds.size(); Index++) {
            BlockPos Head = Layout.Beds.get(Index).Head();
            GatewayTraveller User = enter(Helper, "scp4666_bedded_" + Index, 4.5, 4.5);
            place(User, new Vec3(Head.getX() + 0.5, Layout.Floor + 1.0, Head.getZ() + 2.5), 0.0f);
            Bedded.add(User);
        }
        GatewayTraveller Spare = enter(Helper, "scp4666_spare", 4.5, 4.5);
        place(Spare, indoors(Layout, 7.5, 2.5), 0.0f);
        List<ServerPlayer> Chosen = Scp4666Cold.assign(World, Layout, State);
        Helper.assertTrue(Chosen.size() == 1 && Chosen.get(0) == Spare && State.isCold(Spare.getUUID()), "the wrong visitors were sent out: " + Chosen);
        for (GatewayTraveller User : Bedded) {
            Helper.assertTrue(!State.isCold(User.getUUID()), "a visitor beside a bed was sent out");
        }
        Helper.assertTrue(Scp4666Cold.assign(World, Layout, State).isEmpty(), "a second pass sent out more visitors");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_cold_enough")
    public static void enoughBedsSendNobodyOut(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        for (int Index = 0; Index < Layout.Beds.size(); Index++) {
            GatewayTraveller User = enter(Helper, "scp4666_guest_" + Index, 4.5, 4.5);
            place(User, indoors(Layout, -6.5 + Index * 2.0, 3.5), 0.0f);
        }
        Helper.assertTrue(Scp4666Cold.assign(World, Layout, State).isEmpty() && State.cold().isEmpty(), "visitors were sent out with a bed each");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_cold_sleep")
    public static void theColdDoNotHoldUpTheNight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 5, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        List<GatewayTraveller> Sleepers = new ArrayList<>();
        for (int Index = 0; Index < Layout.Beds.size(); Index++) {
            BlockPos Head = Layout.Beds.get(Index).Head();
            GatewayTraveller User = enter(Helper, "scp4666_dreamer_" + Index, 4.5, 4.5);
            place(User, new Vec3(Head.getX() + 0.5, Layout.Floor + 1.0, Head.getZ() + 2.5), 0.0f);
            Sleepers.add(User);
        }
        GatewayTraveller Spare = enter(Helper, "scp4666_wakeful", 4.5, 4.5);
        place(Spare, indoors(Layout, 7.5, 2.5), 0.0f);
        Scp4666Cold.assign(World, Layout, State);
        Helper.assertTrue(State.isCold(Spare.getUUID()), "the spare visitor was not sent out");
        for (GatewayTraveller User : Sleepers) {
            User.doze(true);
        }
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.night() == 6 && State.phase() == Scp4666State.PhaseDay, "the cold visitor held up the night: night " + State.night()))
                .thenExecute(() -> Helper.assertTrue(State.cold().isEmpty(), "the cold list survived the morning"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp4666_cold_death")
    public static void theColdTakeThoseWhoGoOut(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        GatewayTraveller User = enter(Helper, "scp4666_wanderer", 4.5, 4.5);
        place(User, indoors(Layout, 7.5, 2.5), 0.0f);
        Scp4666Cold.mark(World, State, User);
        Helper.assertTrue(State.isCold(User.getUUID()) && !Scp4666Cold.sinking(User), "the visitor was not marked");
        Vec3 Snow = new Vec3(Layout.Centre.getX() + 6.5, Layout.Ground + 1.2, Layout.Centre.getZ() + 12.5);
        double[] Start = new double[1];
        Helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> Helper.assertTrue(!Scp4666Cold.sinking(User), "the visitor sank indoors"))
                .thenExecute(() -> {
                    place(User, Snow, 180.0f);
                    Start[0] = User.getY();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Scp4666Cold.sinking(User), "the visitor walked into the cold and nothing happened"))
                .thenWaitUntil(() -> Helper.assertTrue(User.getHealth() <= 0.0f, "the cold never took the visitor: health " + User.getHealth()))
                .thenExecute(() -> {
                    Helper.assertTrue(Start[0] - User.getY() > Scp4666Cold.SinkDepth - 0.2, "the visitor did not sink: " + Start[0] + " to " + User.getY());
                    Helper.assertTrue(!State.isCold(User.getUUID()), "the taken visitor is still marked");
                    Helper.assertTrue("scp_unleashed.scp4666_cold".equals(Scp4666Cold.source(World).getMsgId()), "wrong death message: " + Scp4666Cold.source(World).getMsgId());
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp4666_lying")
    public static void theTakenStayLyingDown(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, Scp4666State.FinalNight, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        GatewayTraveller Victim = enter(Helper, "scp4666_supine", 4.5, 4.5);
        place(Victim, indoors(Layout, 7.5, 2.5), 0.0f);
        Scp4666Endings.force(Victim.getUUID(), Scp4666Endings.FateBad);
        Scp4666Endings.begin(World, Layout, State, List.of(Victim));
        Vec3 Wake = Victim.position();
        Vec3 Middle = Vec3.atBottomCenterOf(Layout.BasementCentre);
        double Floor = Layout.BasementMinY + 1.0;
        List<String> Faults = new ArrayList<>();
        int[] Watched = new int[1];
        Helper.onEachTick(() -> {
            if (Victim.getHealth() <= 0.0f || Faults.size() > 6) {
                return;
            }
            Watched[0]++;
            if (Victim.getPose() != Pose.SLEEPING || !Scp4666Pose.lying(Victim)) {
                Faults.add("tick " + Watched[0] + " pose " + Victim.getPose());
            }
            if (Math.abs(Victim.getY() - Floor) > 0.3) {
                Faults.add("tick " + Watched[0] + " height " + Victim.getY());
            }
            Vec3 Along = Middle.subtract(Wake);
            Vec3 Offset = Victim.position().subtract(Wake);
            double Share = Math.max(0.0, Math.min(1.0, Offset.dot(Along) / Math.max(1.0E-6, Along.lengthSqr())));
            Vec3 Nearest = Wake.add(Along.scale(Share));
            if (new Vec3(Victim.getX() - Nearest.x, 0.0, Victim.getZ() - Nearest.z).length() > 0.6) {
                Faults.add("tick " + Watched[0] + " off the drag line at " + Victim.position());
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Victim.getHealth() <= 0.0f, "the visitor was never taken"))
                .thenExecute(() -> Helper.assertTrue(Faults.isEmpty() && Watched[0] > Scp4666Endings.DragTicks, "the visitor did not stay down: " + String.join("; ", Faults)
                        + " over " + Watched[0] + " ticks"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_snowroof")
    public static void noSnowFallsUnderTheRoof(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        List<String> Wrong = new ArrayList<>();
        int Sheltered = 0;
        for (int X = Layout.CabinMinX + 1; X < Layout.CabinMaxX; X++) {
            for (int Z = Layout.CabinMinZ + 1; Z < Layout.CabinMaxZ; Z++) {
                for (int Y = Layout.BasementMinY; Y <= Layout.Floor + 6; Y++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    boolean UnderEaves = Y > Layout.Floor + 4 && (Z < Layout.CabinMinZ + 2 || Z > Layout.CabinMaxZ - 2);
                    if (UnderEaves || !World.getBlockState(Spot).isAir()) {
                        continue;
                    }
                    Sheltered++;
                    if (Scp4666World.openToSky(World, Spot)) {
                        Wrong.add(Spot.toShortString());
                    }
                }
            }
        }
        for (int X = Layout.FrontDoor.getX() - 2; X <= Layout.FrontDoor.getX() + 2; X++) {
            for (int Z = Layout.FrontDoor.getZ() + 1; Z <= Layout.FrontDoor.getZ() + 3; Z++) {
                BlockPos Spot = new BlockPos(X, Layout.Floor + 1, Z);
                if (World.getBlockState(Spot).isAir() && Scp4666World.openToSky(World, Spot)) {
                    Wrong.add("porch " + Spot.toShortString());
                }
            }
        }
        int Open = 0;
        for (int Index = 0; Index < 24; Index++) {
            double Angle = Index * Math.PI * 2.0 / 24.0;
            BlockPos Spot = BlockPos.containing(Layout.Centre.getX() + 0.5 + Math.cos(Angle) * 17.0, Layout.Ground + 4, Layout.Centre.getZ() + 0.5 + Math.sin(Angle) * 17.0);
            if (Scp4666World.openToSky(World, Spot)) {
                Open++;
            }
        }
        Helper.assertTrue(Sheltered > 500 && Wrong.isEmpty(), "snow could fall at " + Wrong.size() + " sheltered spots: " + String.join(", ", Wrong.subList(0, Math.min(8, Wrong.size()))));
        Helper.assertTrue(Open >= 20, "only " + Open + " of 24 clearing spots are open to the sky");
        World.registryAccess().registry(Registries.BIOME).flatMap(Biomes -> Biomes.getOptional(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_winter_woods")))
                .ifPresent(Biome -> Helper.assertTrue(Biome.getAmbientParticle().isEmpty() && Biome.getAmbientLoop().isEmpty() && Biome.getAmbientAdditions().isEmpty(),
                        "the winter biome still spawns ambient snow or plays its own wind"));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_snowdrift")
    public static void snowDriftsAreSmooth(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        int Reach = Scp4666World.RegionRadius - 2;
        int Jumps = 0;
        int Flips = 0;
        int Checked = 0;
        for (int X = Layout.Centre.getX() - Reach; X < Layout.Centre.getX() + Reach - 2; X++) {
            for (int Z = Layout.Centre.getZ() - Reach; Z < Layout.Centre.getZ() + Reach; Z++) {
                if (Scp4666Builder.onPath(Layout, X, Z, 3.5) || Scp4666Builder.onPath(Layout, X + 2, Z, 3.5) || Scp4666Builder.nearCabin(Layout, X, Z, Scp4666Builder.DriftReach + 3)) {
                    continue;
                }
                int A = Scp4666Builder.snowDepth(Layout, X, Z);
                int B = Scp4666Builder.snowDepth(Layout, X + 1, Z);
                int C = Scp4666Builder.snowDepth(Layout, X + 2, Z);
                Checked++;
                if (Math.abs(A - B) > 1) {
                    Jumps++;
                }
                if (A != B && A == C) {
                    Flips++;
                }
            }
        }
        Helper.assertTrue(Checked > 1000 && Jumps == 0 && Flips < Checked / 50, "snow depth: " + Checked + " checked, " + Jumps + " jumps, " + Flips + " flips");
        Helper.succeed();
    }

    private static GatewayTraveller enter(GameTestHelper Helper, String Name, double X, double Z) {
        ServerLevel Home = Helper.getLevel();
        GatewayTraveller User = new GatewayTraveller(Home, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, GatewayGameTests.Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Home.addNewPlayer(User);
        GatewayGameTests.whenDone(Helper, () -> GatewayGameTests.clean(User));
        boolean Entered = GatewayTravel.enter(User, winter(), new GatewayStash.Spot(Home.dimension(), Spot, 0.0f));
        Helper.assertTrue(Entered, Name + " could not enter the winter");
        clearMen(User.serverLevel());
        return User;
    }

    private static void place(ServerPlayer User, Vec3 Spot, float Yaw) {
        User.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setYHeadRot(Yaw);
    }

    private static Scp4666State fresh(ServerLevel World, int Night, int Phase, long Clock) {
        Scp4666State State = Scp4666State.get(World.getServer());
        State.start();
        State.setNight(Night);
        State.setPhase(Phase);
        State.setClock(Clock);
        State.newNight();
        Scp4666Director.refreshLamps(World, Scp4666Map.get(World.getServer()), State);
        return State;
    }

    private static Vec3 indoors(Scp4666Map Layout, double X, double Z) {
        return new Vec3(X, Layout.Floor + 1.0, Z);
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_clock")
    public static void timeStopsAtMidnight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_clock", 4.5, 4.5);
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, Scp4666Clock.Midnight - 10L);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.phase() == Scp4666State.PhaseMidnight, "midnight never came: phase " + State.phase() + " clock " + State.clock()))
                .thenIdle(40)
                .thenExecute(() -> {
                    Helper.assertTrue(State.clock() == Scp4666Clock.Midnight, "the clock moved past midnight: " + State.clock());
                    Helper.assertTrue(World.getDayTime() == Scp4666Clock.Midnight, "the dimension reports day time " + World.getDayTime());
                    Helper.assertTrue(State.night() == 3, "the night changed on its own");
                    Helper.assertTrue(User.isAlive(), "the visitor did not survive");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_sleep")
    public static void everyoneMustSleepToEndTheNight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller First = enter(Helper, "scp4666_sleeper_a", 4.5, 4.5);
        GatewayTraveller Second = enter(Helper, "scp4666_sleeper_b", 6.5, 4.5);
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        First.doze(true);
        Helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> Helper.assertTrue(State.night() == 3 && State.phase() == Scp4666State.PhaseMidnight, "one sleeper ended the night for everyone"))
                .thenExecute(() -> Second.doze(true))
                .thenWaitUntil(() -> Helper.assertTrue(State.night() == 4 && State.phase() == Scp4666State.PhaseDay, "sleeping together did not bring morning: night " + State.night()))
                .thenExecute(() -> {
                    Helper.assertTrue(State.clock() < 200L, "morning started at clock " + State.clock());
                    Helper.assertTrue(World.getBlockState(Layout.BoxSpot).is(Scp4666Registry.Box.get()), "no box at the door on the first morning");
                    Helper.assertTrue(!First.isSleeping() && !Second.isSleeping(), "the sleepers were not woken");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_box")
    public static void morningBoxStopsFoodOnDayTen(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_breakfast", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 8, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        Scp4666Director.morning(World, Layout, State);
        Helper.assertTrue(State.night() == 9, "night 8 led to night " + State.night());
        Scp4666BoxBlockEntity Box = (Scp4666BoxBlockEntity) World.getBlockEntity(Layout.BoxSpot);
        Helper.assertTrue(Box != null && Box.hasFood(), "day 9 had no food in the box");
        List<ItemStack> Share = Box.take(User);
        Helper.assertTrue(Share.size() == 2 && Share.get(0).is(Scp4666Registry.Cocoa.get()), "day 9 share was " + Share);
        Helper.assertTrue(Box.take(User).isEmpty(), "the box gave a second share");
        State.setPhase(Scp4666State.PhaseMidnight);
        Scp4666Director.morning(World, Layout, State);
        Box = (Scp4666BoxBlockEntity) World.getBlockEntity(Layout.BoxSpot);
        Helper.assertTrue(State.night() == 10 && Box != null && !Box.hasFood(), "day 10 still brought food");
        Share = Box.take(User);
        Helper.assertTrue(Share.size() == 1 && Share.get(0).is(Scp4666Registry.Cocoa.get()), "day 10 share was " + Share);
        State.setClock(Scp4666Clock.Dusk - 1L);
        State.setPhase(Scp4666State.PhaseDay);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!World.getBlockState(Layout.BoxSpot).is(Scp4666Registry.Box.get()), "the box stayed after dusk"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_lights")
    public static void lightsDieOnNightTwelve(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        enter(Helper, "scp4666_lamplighter", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 5, Scp4666State.PhaseDay, 6000L);
        State.setLights(false);
        Scp4666Director.refreshLamps(World, Layout, State);
        Helper.assertTrue(!lit(World, Layout), "the switch could not turn the lamps off");
        State.setLights(true);
        Scp4666Director.refreshLamps(World, Layout, State);
        Helper.assertTrue(lit(World, Layout), "the switch could not turn the lamps on");
        State.setNight(11);
        State.setPhase(Scp4666State.PhaseMidnight);
        Scp4666Director.morning(World, Layout, State);
        Helper.assertTrue(State.night() == 12 && State.lightsDead(), "night twelve did not kill the lights");
        Helper.assertTrue(dark(World, Layout), "a lamp still burns on night twelve");
        State.setLights(true);
        Scp4666Director.refreshLamps(World, Layout, State);
        Helper.assertTrue(dark(World, Layout), "the switch relit the lamps on night twelve");
        BlockState Tree = World.getBlockState(Layout.Tree);
        Helper.assertTrue(Tree.getBlock() instanceof Scp4666TreeBlock && Scp4666TreeBlock.lightOf(Tree) > 0, "the tree went dark with the lamps");
        State.setFire(Scp4666Heat.FireMax);
        Scp4666Heat.refresh(World, Layout, State);
        BlockState Fire = World.getBlockState(Layout.Fireplace);
        Helper.assertTrue(Fire.is(Scp4666Registry.Fireplace.get()) && Fire.getLightEmission(World, Layout.Fireplace) >= 15, "a fed fireplace does not burn on night twelve");
        Helper.succeed();
    }

    private static boolean lit(ServerLevel World, Scp4666Map Layout) {
        for (BlockPos Pos : Layout.Lamps) {
            BlockState Found = World.getBlockState(Pos);
            if (!(Found.getBlock() instanceof Scp4666LampBlock) || !Found.getValue(Scp4666LampBlock.Lit)) {
                return false;
            }
        }
        return true;
    }

    private static boolean dark(ServerLevel World, Scp4666Map Layout) {
        for (BlockPos Pos : Layout.Lamps) {
            BlockState Found = World.getBlockState(Pos);
            if (!(Found.getBlock() instanceof Scp4666LampBlock) || Found.getValue(Scp4666LampBlock.Lit)) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_protection")
    public static void nothingBreaksInTheWinter(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_vandal", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        BlockPos Wall = Layout.FrontDoor.east(2);
        BlockEvent.BreakEvent Breaking = NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Wall, World.getBlockState(Wall), User));
        Helper.assertTrue(Breaking.isCanceled(), "a wall could be broken");
        BlockPos Spot = Layout.FrontDoor.south(3);
        BlockEvent.EntityPlaceEvent Placing = NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(World.dimension(), World, Spot),
                World.getBlockState(Spot.below()), User));
        Helper.assertTrue(Placing.isCanceled(), "a block could be placed");
        PlayerEvent.BreakSpeed Speed = NeoForge.EVENT_BUS.post(
                new PlayerEvent.BreakSpeed(User, World.getBlockState(Wall), 1.0f, Wall));
        Helper.assertTrue(Speed.getNewSpeed() == 0.0f, "breaking speed is " + Speed.getNewSpeed());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_boundary")
    public static void walkingAwayTurnsYouAround(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_wanderer", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        double Cx = Layout.Centre.getX() + 0.5;
        double Cz = Layout.Centre.getZ() + 0.5;
        place(User, new Vec3(Cx + 3.0, Layout.Ground + 1.25, Cz - Scp4666World.TurnRadius - 1.0), 180.0f);
        User.setDeltaMovement(0.0, 0.0, -0.2);
        Helper.assertTrue(Scp4666Boundary.check(World, Layout, User), "the boundary did not catch the wanderer");
        double After = Layout.distanceFromCentre(User.position());
        Helper.assertTrue(After < Scp4666World.TurnRadius && After > Scp4666World.TurnRadius - 8.0, "the wanderer was placed " + After + " from the cabin");
        Helper.assertTrue(User.getZ() > Cz, "the wanderer was not moved to the far side: z " + User.getZ());
        Helper.assertTrue(User.getYRot() == 180.0f, "the wanderer's view was turned: yaw " + User.getYRot());
        Vec3 Motion = User.getDeltaMovement();
        Vec3 Inward = new Vec3(Cx - User.getX(), 0.0, Cz - User.getZ()).normalize();
        Helper.assertTrue(Motion.z < 0.0 && Motion.normalize().dot(Inward) > 0.5, "the wanderer is not walking back toward the cabin: " + Motion);
        place(User, new Vec3(Cx, Layout.Ground + 1.25, Cz - 30.0), 180.0f);
        Helper.assertTrue(!Scp4666Boundary.check(World, Layout, User), "the boundary caught someone well inside");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp4666_endings")
    public static void bothEndingsPlayTogether(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GatewayTraveller Lucky = enter(Helper, "scp4666_lucky", 4.5, 4.5);
        GatewayTraveller Unlucky = enter(Helper, "scp4666_unlucky", 8.5, 4.5);
        Scp4666State State = fresh(World, Scp4666State.FinalNight, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        place(Lucky, indoors(Layout, -6.5, 3.5), 0.0f);
        place(Unlucky, indoors(Layout, 7.5, 2.5), 0.0f);
        Scp4666Endings.force(Lucky.getUUID(), Scp4666Endings.FateGood);
        Scp4666Endings.force(Unlucky.getUUID(), Scp4666Endings.FateBad);
        Scp4666Endings.begin(World, Layout, State, List.of(Lucky, Unlucky));
        BlockPos Gift = Layout.Presents.get(0);
        Helper.assertTrue(World.getBlockEntity(Gift) instanceof Scp4666OwnedBlockEntity Tag && Lucky.getUUID().equals(Tag.owner()), "no present for the lucky visitor");
        Helper.assertTrue(!(World.getBlockEntity(Layout.Presents.get(1)) instanceof Scp4666OwnedBlockEntity), "the unlucky visitor got a present");
        BlockState Door = World.getBlockState(Layout.ExitDoor);
        Helper.assertTrue(Door.is(Scp4666Registry.ExitDoor.get()) && Door.getValue(Scp4666ExitDoorBlock.Half) == DoubleBlockHalf.LOWER
                && World.getBlockState(Layout.ExitDoor.above()).is(Scp4666Registry.ExitDoor.get()), "the way out did not appear");
        Helper.assertTrue(Unlucky.position().distanceTo(Vec3.atBottomCenterOf(Layout.BasementWake)) < 2.0, "the unlucky visitor did not wake in the basement");
        Helper.assertTrue(Scp4666Endings.held(Unlucky), "the unlucky visitor can move");
        BlockState Wrapped = World.getBlockState(Gift);
        Wrapped.useWithoutItem(World, Unlucky, new BlockHitResult(Vec3.atCenterOf(Gift), Direction.UP, Gift, false));
        Helper.assertTrue(Unlucky.getInventory().countItem(Scp4666Registry.Doll.get()) == 0, "someone else's present opened");
        Wrapped.useWithoutItem(World, Lucky, new BlockHitResult(Vec3.atCenterOf(Gift), Direction.UP, Gift, false));
        Helper.assertTrue(Lucky.getInventory().countItem(Scp4666Registry.Doll.get()) == 1 && Lucky.getInventory().countItem(Scp4666Registry.Necklace.get()) == 1
                && Lucky.getInventory().countItem(Scp4666Registry.Cane.get()) == 1, "the present did not hold the doll, necklace and cane");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Scp4666YuleMan.present(World), "SCP-4666 did not come for the unlucky visitor"))
                .thenWaitUntil(() -> Helper.assertTrue(Unlucky.getHealth() <= 0.0f, "the unlucky visitor was never taken: health " + Unlucky.getHealth()))
                .thenWaitUntil(() -> Helper.assertTrue(State.phase() == Scp4666State.PhaseOver, "the ending never finished: phase " + State.phase()))
                .thenWaitUntil(() -> Helper.assertTrue(!Scp4666YuleMan.present(World), "SCP-4666 lingered after the ending"))
                .thenExecute(() -> {
                    Helper.assertTrue(Lucky.isAlive() && !Scp4666Endings.held(Lucky), "the lucky visitor was harmed");
                    BlockState Exit = World.getBlockState(Layout.ExitDoor);
                    Exit.useWithoutItem(World, Lucky, new BlockHitResult(Vec3.atCenterOf(Layout.ExitDoor), Direction.NORTH, Layout.ExitDoor, false));
                    Helper.assertTrue(Lucky.level().dimension() == Helper.getLevel().dimension(), "the door did not let the lucky visitor out");
                    Helper.assertTrue(Lucky.getInventory().countItem(Scp4666Registry.Doll.get()) == 1, "the doll was lost on the way out");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_reset")
    public static void emptyWinterResets(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666State State = fresh(World, 7, Scp4666State.PhaseDay, 6000L);
        int Epoch = State.epoch();
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Vec3 Where = Layout.Spawn.add(4.0, 0.0, 0.0);
        int ChunkX = BlockPos.containing(Where).getX() >> 4;
        int ChunkZ = BlockPos.containing(Where).getZ() >> 4;
        World.setChunkForced(ChunkX, ChunkZ, true);
        GatewayGameTests.whenDone(Helper, () -> World.setChunkForced(ChunkX, ChunkZ, false));
        Scp4666YuleMan Man = Scp4666Happenings.spawn(World, Where, 0.0f);
        Helper.assertTrue(Man != null, "could not spawn SCP-4666 for the reset test");
        Man.stalk(0.0, true);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.epoch() > Epoch, "the empty winter never reset: inside " + Scp4666World.visitors(World).size() + " started "
                        + State.started() + " empty " + Scp4666Director.emptyTicks(World.getServer()) + " offline " + Scp4666Director.offlineParticipants(World.getServer())))
                .thenExecute(() -> {
                    Helper.assertTrue(State.night() == 1, "the reset left night " + State.night());
                    Helper.assertTrue(State.visitors().isEmpty(), "the reset kept visitors");
                    Helper.assertTrue(!Man.isAlive() || Man.isRemoved(), "SCP-4666 survived the reset");
                })
                .thenExecute(() -> Scp4666World.ensureBuilt(World))
                .thenExecute(() -> Helper.assertTrue(World.getBlockState(Layout.Tree).getBlock() instanceof Scp4666TreeBlock, "the cabin was not rebuilt"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_late")
    public static void lateArrivalsJoinTheCurrentNight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller First = enter(Helper, "scp4666_early", 4.5, 4.5);
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseDay, 8000L);
        int Epoch = State.epoch();
        GatewayTraveller Second = enter(Helper, "scp4666_late", 8.5, 4.5);
        Helper.assertTrue(State.night() == 6 && State.epoch() == Epoch, "a late arrival restarted the event: night " + State.night());
        Scp4666State.Visitor Entry = State.known(Second.getUUID());
        Helper.assertTrue(Entry != null && Entry.Joined == 6, "the late arrival was not recorded on night six");
        Helper.assertTrue(State.known(First.getUUID()) != null, "the first visitor was forgotten");
        Helper.assertTrue(Scp4666World.visitors(World).size() == 2, "both visitors are not inside");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp4666_events")
    public static void everyNamedEventPlays(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GatewayTraveller User = enter(Helper, "scp4666_witness", 4.5, 4.5);
        Scp4666State State = fresh(World, 13, Scp4666State.PhaseDay, 15000L);
        Scp4666Director.setTree(World, Layout, State, Scp4666State.TreeFull);
        place(User, indoors(Layout, -6.5, 4.5), 90.0f);
        List<String> Failed = new ArrayList<>();
        for (String Name : List.of("chimes", "whistle", "roof", "gramophone", "breath_glass", "stairs", "snow_inside", "hum", "knock", "basement", "tree_out", "prints", "door",
                "cellar", "stocking", "box")) {
            if (!Scp4666Commands.trigger(World, Name)) {
                Failed.add(Name);
            }
        }
        Helper.assertTrue(Scp4666Commands.trigger(World, "watcher") && Scp4666YuleMan.present(World), "the watcher did not appear");
        clearMen(World);
        place(User, indoors(Layout, Layout.AtticHatch.getX() - 0.5, Layout.AtticHatch.getZ() + 0.5), 0.0f);
        Helper.assertTrue(Scp4666Commands.trigger(World, "attic") && Scp4666YuleMan.present(World), "nothing looked down from the attic");
        clearMen(World);
        place(User, indoors(Layout, -6.0, 4.0), 0.0f);
        Scp4666Commands.trigger(World, "window");
        Helper.assertTrue(Scp4666YuleMan.present(World), "nobody came to the window");
        clearMen(World);
        Helper.assertTrue(Failed.isEmpty(), "events did not play: " + Failed);
        Helper.assertTrue(World.getBlockEntity(Layout.Gramophone) instanceof Scp4666GramophoneBlockEntity Player && Player.playing(), "the gramophone did not start");
        Helper.assertTrue(!State.prints().isEmpty(), "no footprints were left");
        Helper.assertTrue(World.getBlockState(Layout.FrontDoor).getValue(DoorBlock.OPEN), "the front door was not left ajar");
        Helper.assertTrue(World.getBlockEntity(Layout.Stockings.get(0)) instanceof Scp4666OwnedBlockEntity Tag && User.getUUID().equals(Tag.owner()), "no stocking with the visitor's name");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.tree() == 0, "the tree lights never went out: " + State.tree()))
                .thenExecute(() -> Scp4666Director.restorePrints(World, State))
                .thenSucceed();
    }

    private static String sight(ServerLevel World, ServerPlayer User, Scp4666YuleMan Man) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Middle = Man.position().add(0.0, Scp4666YuleMan.Height * 0.55, 0.0);
        double Dot = User.getViewVector(1.0f).dot(Middle.subtract(Eye).normalize());
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Middle, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, User));
        return " eye " + Eye + " man " + Man.position() + " dot " + Dot + " hit " + Hit.getType() + " at " + Hit.getBlockPos() + " " + World.getBlockState(Hit.getBlockPos())
                + " yaw " + User.getYRot() + " alive " + Man.isAlive() + " players " + World.players().size();
    }

    private static void clearMen(ServerLevel World) {
        for (Entity Found : World.getAllEntities()) {
            if (Found instanceof Scp4666YuleMan) {
                Found.discard();
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp4666_watcher")
    public static void theWatcherFleesWhenSeen(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GatewayTraveller User = enter(Helper, "scp4666_looker", 4.5, 4.5);
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseDay, 15000L);
        Vec3 Him = Scp4666Director.ringPoint(Layout, 0.0, Scp4666World.StalkRadius);
        Vec3 Eye = Scp4666Director.ringPoint(Layout, 0.22, Scp4666World.StalkRadius).add(0.0, 0.25, 0.0);
        float Yaw = (float) (Math.toDegrees(Math.atan2(Him.z - Eye.z, Him.x - Eye.x))) - 90.0f;
        place(User, Eye, Yaw);
        Scp4666YuleMan Man = Scp4666Happenings.spawn(World, Him, 0.0f);
        Helper.assertTrue(Man != null, "could not spawn the watcher");
        Man.stalk(0.0, false);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Man.mode() == Scp4666YuleMan.ModeFlee || Man.isRemoved(), "the watcher did not notice being seen: mode " + Man.mode() + sight(World, User, Man)))
                .thenWaitUntil(() -> Helper.assertTrue(Man.isRemoved(), "the watcher did not vanish after fleeing"))
                .thenExecute(() -> Helper.assertTrue(State.visitor(User.getUUID()).Sightings > 0, "the sighting was not counted"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp4666_window")
    public static void theFaceAtTheWindowSinksAway(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GatewayTraveller User = enter(Helper, "scp4666_insomniac", 4.5, 4.5);
        Scp4666State State = fresh(World, 9, Scp4666State.PhaseWoken, 19200L);
        Scp4666Map.Window Glass = null;
        for (Scp4666Map.Window Entry : Layout.Windows) {
            if (Entry.Room().equals("living") && Entry.Out() == Direction.SOUTH) {
                Glass = Entry;
            }
        }
        Helper.assertTrue(Glass != null, "no south living room window is mapped");
        Vec3 Inside = Glass.glass().add(0.0, -1.0 - 0.62, -2.6);
        place(User, new Vec3(Inside.x, Layout.Floor + 1.0, Inside.z), 0.0f);
        Scp4666Happenings.windowVisit(World, Layout, World.getRandom());
        Scp4666YuleMan Man = null;
        for (Entity Found : World.getAllEntities()) {
            if (Found instanceof Scp4666YuleMan Each) {
                Man = Each;
            }
        }
        Helper.assertTrue(Man != null && Man.mode() == Scp4666YuleMan.ModeStare, "SCP-4666 is not staring in");
        Scp4666YuleMan Starer = Man;
        Helper.assertTrue(Starer.position().distanceTo(Glass.outside(0.75)) < 2.5, "SCP-4666 is not at the window");
        float Yaw = (float) (Math.toDegrees(Math.atan2(Starer.getZ() - User.getZ(), Starer.getX() - User.getX()))) - 90.0f;
        place(User, User.position(), Yaw);
        User.setXRot(0.0f);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Starer.mode() == Scp4666YuleMan.ModeSink || Starer.isRemoved(), "SCP-4666 never sank away: mode " + Starer.mode()))
                .thenWaitUntil(() -> Helper.assertTrue(Starer.isRemoved(), "SCP-4666 did not vanish"))
                .thenExecute(() -> Helper.assertTrue(State.visitor(User.getUUID()).Stares > 0, "watching the window was not counted"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_arrival")
    public static void theDossierHandsOverAShard(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp4666Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_4666 dossier did not load");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-4666 does not arrive by courier");
        StrangerEncounterCondition Courier = (StrangerEncounterCondition) Document.Condition();
        Helper.assertTrue(!Courier.Gift().isEmpty(), "the courier brings nothing");
        StrangerGift Gift = Courier.Gift().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Stack.is(GatewayRegistry.Shard.get()), "the gift is " + Stack);
        Helper.assertTrue(GatewayDestinations.of(Stack) != null && Scp4666Destination.Id.equals(GatewayDestinations.of(Stack).Id()), "the gifted shard does not lead to the winter");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_death")
    public static void dyingInsideRestoresBelongings(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        ServerLevel Home = Helper.getLevel();
        GatewayTraveller User = new GatewayTraveller(Home, new GameProfile(UUID.randomUUID(), "scp4666_lost"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(6.5, GatewayGameTests.Floor, 6.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Home.addNewPlayer(User);
        GatewayGameTests.whenDone(Helper, () -> GatewayGameTests.clean(User));
        Map<Item, Integer> Before = GatewayGameTests.fill(User);
        Helper.assertTrue(GatewayTravel.enter(User, winter(), new GatewayStash.Spot(Home.dimension(), Spot, 0.0f)), "could not enter");
        User.getInventory().add(new ItemStack(Scp4666Registry.Cocoa.get(), 4));
        ServerPlayer Fresh = GatewayGameTests.dieAndRespawn(Helper, User, Spot);
        Helper.assertTrue(GatewayGameTests.counts(Fresh, List.of()).equals(Before), "after dying the visitor holds " + GatewayGameTests.describe(GatewayGameTests.counts(Fresh, List.of())));
        Helper.assertTrue(Fresh.getInventory().countItem(Scp4666Registry.Cocoa.get()) == 0, "cocoa found inside came out with a dead visitor");
        Helper.succeed();
    }

    private static BlockHitResult hit(BlockPos Pos) {
        return new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false);
    }

    private static void poke(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        for (int Index = 0; Index < 9; Index++) {
            if (User.getInventory().getItem(Index).isEmpty()) {
                User.getInventory().selected = Index;
                break;
            }
        }
        World.getBlockState(Pos).useWithoutItem(World, User, hit(Pos));
    }

    private static void apply(ServerLevel World, ServerPlayer User, BlockPos Pos, Item Kind) {
        int Slot = -1;
        for (int Index = 0; Index < User.getInventory().getContainerSize(); Index++) {
            if (User.getInventory().getItem(Index).is(Kind)) {
                Slot = Index;
            }
        }
        ItemStack Stack = Slot >= 0 ? User.getInventory().getItem(Slot) : ItemStack.EMPTY;
        if (Slot >= 0 && Slot < 9) {
            User.getInventory().selected = Slot;
        } else if (Slot >= 0) {
            User.getInventory().setItem(Slot, ItemStack.EMPTY);
            User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        }
        BlockState Found = World.getBlockState(Pos);
        if (Found.useItemOn(User.getMainHandItem(), World, User, InteractionHand.MAIN_HAND, hit(Pos)) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            Found.useWithoutItem(World, User, hit(Pos));
        }
    }

    private static int fireLevel(ServerLevel World, Scp4666Map Layout) {
        BlockState Found = World.getBlockState(Layout.Fireplace);
        return Found.getBlock() instanceof Scp4666FireplaceBlock ? Found.getValue(Scp4666FireplaceBlock.Fire) : -1;
    }

    private record Cost(float Activity, long Ticks) {
    }

    private static Cost measure(Scp4666State State, long Start, Runnable Action) {
        State.setPhase(Scp4666State.PhaseDay);
        State.setClock(Start);
        State.setActivity(0.0f);
        Action.run();
        return new Cost(State.activity(), State.clock() - Start);
    }

    private static boolean costs(Cost Found, Scp4666Chores.Chore Kind, float Scale) {
        return Math.abs(Found.Activity() - Kind.Activity * Scale) < 1.0E-4f && Found.Ticks() == Kind.Ticks;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_chore_fire")
    public static void theFireplaceBurnsDownAndWarmsWhenFed(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_stoker", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, 3000L);
        place(User, indoors(Layout, -5.5, 2.5), 90.0f);
        State.setCoal(0);
        State.setFire(30);
        Scp4666Heat.refresh(World, Layout, State);
        Helper.assertTrue(fireLevel(World, Layout) == 1, "a nearly spent fire shows level " + fireLevel(World, Layout));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.fire() == 0 && fireLevel(World, Layout) == 0, "the fire never burned out: fuel " + State.fire() + " level " + fireLevel(World, Layout)))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp4666Heat.warmth(State) == 0 && Scp4666Heat.dead(State), "a dead cabin still has warmth " + Scp4666Heat.warmth(State));
                    BlockState Chimney = World.getBlockState(Layout.Chimney);
                    Helper.assertTrue(!(Chimney.getBlock() instanceof CampfireBlock) || !Chimney.getValue(CampfireBlock.LIT), "the chimney still smokes over a dead fire");
                    Helper.assertTrue(World.getBlockState(Layout.Fireplace).getLightEmission(World, Layout.Fireplace) == 0, "a dead fire still glows");
                    User.getInventory().add(new ItemStack(Scp4666Registry.Firewood.get(), 4));
                    for (int Index = 0; Index < 4; Index++) {
                        apply(World, User, Layout.Fireplace, Scp4666Registry.Firewood.get());
                    }
                    Helper.assertTrue(Scp4666Chores.carried(User, Scp4666Registry.Firewood.get()) == 0, "the firewood was not burned");
                    int Expected = 4 * (Scp4666Heat.LogTicks - Scp4666Chores.Chore.Feed.Ticks);
                    Helper.assertTrue(State.fire() == Expected, "four logs gave " + State.fire() + " fuel, not " + Expected);
                    Helper.assertTrue(fireLevel(World, Layout) == Scp4666Heat.fireLevel(Expected) && Scp4666Heat.warmth(State) == Scp4666Heat.FireWarmth[Scp4666Heat.fireLevel(Expected)],
                            "the fed fire shows level " + fireLevel(World, Layout) + " and warmth " + Scp4666Heat.warmth(State));
                    Helper.assertTrue(World.getBlockState(Layout.Fireplace).getLightEmission(World, Layout.Fireplace) > 0, "the fed fire gives no light");
                    BlockState Lit = World.getBlockState(Layout.Chimney);
                    Helper.assertTrue(Lit.getBlock() instanceof CampfireBlock && Lit.getValue(CampfireBlock.LIT), "no smoke from the chimney over a lit fire");
                    State.setFire(Scp4666Heat.FireMax);
                    User.getInventory().add(new ItemStack(Scp4666Registry.Firewood.get(), 1));
                    apply(World, User, Layout.Fireplace, Scp4666Registry.Firewood.get());
                    Helper.assertTrue(Scp4666Chores.carried(User, Scp4666Registry.Firewood.get()) == 1 && State.fire() == Scp4666Heat.FireMax, "a full grate took another log");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp4666_chore_chill")
    public static void theCabinChillsOnlyWhileBothFiresAreOut(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_shiverer", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        place(User, indoors(Layout, 6.5, -4.5), 0.0f);
        State.setFire(0);
        State.setCoal(0);
        Scp4666Heat.refresh(World, Layout, State);
        Scp4666Cold.setChill(User, 30.0f);
        float[] Seen = new float[1];
        Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) == Scp4666Cold.Freezing, "a dead cabin chills at " + Scp4666Cold.rate(Layout, State, User));
        Helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    Helper.assertTrue(Scp4666Cold.chill(User) > 30.5f, "the cold cabin did not chill: " + Scp4666Cold.chill(User));
                    Helper.assertTrue(User.getTicksFrozen() > 0, "no frost on a chilled visitor");
                    Scp4666Cold.setChill(User, Scp4666Cold.ChillSleep + 5.0f);
                    CanPlayerSleepEvent Asked = new CanPlayerSleepEvent(User, Layout.Beds.get(2).Head(), null);
                    NeoForge.EVENT_BUS.post(Asked);
                    Helper.assertTrue(Asked.getProblem() != null, "a freezing visitor could go to sleep");
                    Helper.assertTrue(Scp4666Cold.biting(Scp4666Cold.ChillMax) && !Scp4666Cold.biting(Scp4666Cold.ChillMax - 1.0f), "the cold bites at the wrong chill");
                    State.setFire(Scp4666Heat.FireMax);
                    State.setCoal(Scp4666Heat.CoalMax);
                    Scp4666Heat.refresh(World, Layout, State);
                    Seen[0] = Scp4666Cold.chill(User);
                    Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) < 0.0f, "a warm cabin still chills");
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    Helper.assertTrue(Scp4666Cold.chill(User) < Seen[0] - 1.0f, "the fed fires did not warm the visitor: " + Scp4666Cold.chill(User) + " from " + Seen[0]);
                    Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) == Scp4666Cold.Warm, "both fires give " + Scp4666Cold.rate(Layout, State, User));
                    State.setCoal(0);
                    Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) == Scp4666Cold.Mild, "the fire alone gives " + Scp4666Cold.rate(Layout, State, User));
                    place(User, new Vec3(-5.5, Layout.Floor + 1.0, 2.5), 0.0f);
                    Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) == Scp4666Cold.Mild + Scp4666Cold.Hearth, "the hearth gives no extra warmth");
                    place(User, new Vec3(0.5, Layout.Floor + 1.0, 20.5), 0.0f);
                    Helper.assertTrue(Scp4666Cold.rate(Layout, State, User) == Scp4666Cold.OutsideDay, "outside chills at " + Scp4666Cold.rate(Layout, State, User));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_chore_coal")
    public static void coalHasToBeCarriedToTheBoiler(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_coalman", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, 3000L);
        State.setCoal(0);
        Scp4666Heat.refresh(World, Layout, State);
        Vec3 Bin = Vec3.atCenterOf(Layout.CoalBin);
        Vec3 Boiler = Vec3.atCenterOf(Layout.Heater);
        Helper.assertTrue(!Layout.insideCabin(Bin) && Layout.inBasement(Boiler) && Bin.distanceTo(Boiler) > 16.0, "the coal is not kept away from the cellar");
        Helper.assertTrue(World.getBlockState(Layout.CoalBin).getBlock() instanceof Scp4666FurnitureBlock Store && Store.kind() == Scp4666FurnitureBlock.Kind.CoalBin, "no coal bin at the store");
        place(User, new Vec3(Boiler.x + 1.5, Layout.BasementMinY + 1.0, Boiler.z), 90.0f);
        Helper.assertTrue(!Scp4666Chores.stoke(World, User) && State.coal() == 0, "the boiler took coal from nowhere");
        place(User, new Vec3(Bin.x - 1.5, Layout.Floor, Bin.z), 270.0f);
        poke(World, User, Layout.CoalBin);
        Helper.assertTrue(Scp4666Chores.channelling(User), "shovelling coal did not start");
        Scp4666Chores.finish(User);
        Helper.assertTrue(Scp4666Chores.carried(User, Scp4666Registry.Scuttle.get()) == 1, "the bin gave " + Scp4666Chores.carried(User, Scp4666Registry.Scuttle.get()) + " scuttles");
        poke(World, User, Layout.CoalBin);
        Scp4666Chores.finish(User);
        Helper.assertTrue(Scp4666Chores.carried(User, Scp4666Registry.Scuttle.get()) == 1, "a second scuttle could be carried");
        Helper.assertTrue(Scp4666ChoreItem.burden(World, User) && User.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a full scuttle is not heavy");
        place(User, new Vec3(Boiler.x + 1.5, Layout.BasementMinY + 1.0, Boiler.z), 90.0f);
        apply(World, User, Layout.Heater, Scp4666Registry.Scuttle.get());
        Helper.assertTrue(Scp4666Chores.carried(User, Scp4666Registry.Scuttle.get()) == 0, "the scuttle was not emptied");
        Helper.assertTrue(State.coal() == Scp4666Heat.ScuttleTicks - Scp4666Chores.Chore.Stoke.Ticks, "the boiler holds " + State.coal());
        BlockState Lit = World.getBlockState(Layout.Heater);
        Helper.assertTrue(Lit.getBlock() instanceof Scp4666HeaterBlock && Lit.getValue(Scp4666HeaterBlock.Heat) == 1 && Scp4666Heat.warmth(State) > 0, "the boiler did not light: " + Lit);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_chore_costs")
    public static void everyChoreStirsHimAndSpeedsTheDay(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_busy", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, 2000L);
        List<String> Wrong = new ArrayList<>();
        for (Scp4666Chores.Chore Kind : Scp4666Chores.Chore.values()) {
            Cost Found = measure(State, 2000L, () -> Scp4666Chores.perform(World, User, Kind));
            if (!costs(Found, Kind, 1.0f)) {
                Wrong.add(Kind + " by day " + Found);
            }
        }
        Cost Dark = measure(State, Scp4666Clock.Dusk + 100L, () -> Scp4666Chores.perform(World, User, Scp4666Chores.Chore.Chop));
        if (!costs(Dark, Scp4666Chores.Chore.Chop, Scp4666Chores.NightScale)) {
            Wrong.add("chop after dusk " + Dark);
        }
        State.setClock(Scp4666Clock.Midnight - 100L);
        State.setActivity(0.0f);
        Scp4666Chores.perform(World, User, Scp4666Chores.Chore.Wrap);
        if (State.phase() != Scp4666State.PhaseMidnight || State.clock() != Scp4666Clock.Midnight) {
            Wrong.add("a chore ran the clock past midnight: phase " + State.phase() + " clock " + State.clock());
        }
        Scp4666Chores.perform(World, User, Scp4666Chores.Chore.Chop);
        if (State.clock() != Scp4666Clock.Midnight) {
            Wrong.add("a chore moved the stopped midnight clock");
        }
        place(User, new Vec3(Layout.ChoppingBlock.getX() - 0.5, Layout.Floor, Layout.ChoppingBlock.getZ() + 0.5), 270.0f);
        Cost Chopped = measure(State, 2000L, () -> {
            poke(World, User, Layout.ChoppingBlock);
            Scp4666Chores.finish(User);
        });
        if (!costs(Chopped, Scp4666Chores.Chore.Chop, 1.0f) || Scp4666Chores.carried(User, Scp4666Registry.Firewood.get()) != 2) {
            Wrong.add("chopping " + Chopped);
        }
        place(User, indoors(Layout, -5.5, 2.5), 90.0f);
        Cost Fed = measure(State, 2000L, () -> apply(World, User, Layout.Fireplace, Scp4666Registry.Firewood.get()));
        if (!costs(Fed, Scp4666Chores.Chore.Feed, 1.0f)) {
            Wrong.add("feeding " + Fed);
        }
        place(User, new Vec3(Layout.Workbench.getX() + 1.5, Layout.BasementMinY + 1.0, Layout.Workbench.getZ() + 0.5), 90.0f);
        Cost Sawn = measure(State, 2000L, () -> {
            poke(World, User, Layout.Workbench);
            Scp4666Chores.finish(User);
        });
        if (!costs(Sawn, Scp4666Chores.Chore.Saw, 1.0f) || Scp4666Chores.carried(User, Scp4666Registry.Boards.get()) != 2) {
            Wrong.add("sawing " + Sawn);
        }
        BlockPos Trunk = Layout.Beds.get(1).Head().south(2);
        place(User, Vec3.atBottomCenterOf(Trunk).add(1.0, 0.0, 0.0), 90.0f);
        Cost Wrapped = measure(State, 2000L, () -> {
            poke(World, User, Trunk);
            Scp4666Chores.finish(User);
        });
        if (!costs(Wrapped, Scp4666Chores.Chore.Wrap, 1.0f) || Scp4666Chores.carried(User, Scp4666Registry.Gift.get()) != 1) {
            Wrong.add("wrapping a gift at " + World.getBlockState(Trunk) + " " + Wrapped);
        }
        poke(World, User, Trunk);
        Scp4666Chores.finish(User);
        if (Scp4666Chores.carried(User, Scp4666Registry.Gift.get()) != 1) {
            Wrong.add("a second gift on the same day");
        }
        place(User, indoors(Layout, -6.5, 0.5), 270.0f);
        Cost Offered = measure(State, 2000L, () -> apply(World, User, Layout.Tree, Scp4666Registry.Gift.get()));
        if (!costs(Offered, Scp4666Chores.Chore.Offer, 1.0f) || !State.offering() || !World.getBlockState(Layout.Offering).is(Scp4666Registry.Parcel.get())) {
            Wrong.add("setting the gift under the tree " + Offered);
        }
        BlockPos Pantry = Layout.Stove.offset(-1, 1, -2);
        place(User, Vec3.atBottomCenterOf(Pantry).add(0.0, -1.0, 1.0), 180.0f);
        Cost Scooped = measure(State, 2000L, () -> poke(World, User, Pantry));
        if (!costs(Scooped, Scp4666Chores.Chore.Oats, 1.0f) || Scp4666Chores.carried(User, Scp4666Registry.Oats.get()) != 1) {
            Wrong.add("taking oats from " + World.getBlockState(Pantry) + " " + Scooped);
        }
        place(User, Vec3.atBottomCenterOf(Layout.Stove).add(-1.0, 0.0, 0.0), 90.0f);
        Cost Baked = measure(State, 2000L, () -> apply(World, User, Layout.Stove, Scp4666Registry.Oats.get()));
        if (!costs(Baked, Scp4666Chores.Chore.Bake, 1.0f) || World.getBlockState(Layout.Stove).getValue(Scp4666StoveBlock.Oven) != 1) {
            Wrong.add("baking " + Baked);
        }
        Scp4666Chores.crack(World, Layout, State, 0);
        BlockPos Pane = Layout.Windows.get(0).cells().get(0);
        place(User, Vec3.atBottomCenterOf(Pane).add(0.0, -1.0, -1.5), 0.0f);
        Cost Boarded = measure(State, 2000L, () -> {
            apply(World, User, Pane, Scp4666Registry.Boards.get());
            Scp4666Chores.finish(User);
        });
        if (!costs(Boarded, Scp4666Chores.Chore.Board, 1.0f) || !State.boarded().contains(0) || Scp4666Chores.carried(User, Scp4666Registry.Boards.get()) != 0) {
            Wrong.add("boarding a window " + Boarded + " boarded " + State.boarded());
        }
        Helper.assertTrue(Wrong.isEmpty(), "wrong chore costs: " + Wrong);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_chore_spectator")
    public static void spectatorsNeverJoinAndCauseNoFlicker(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_watcher_only", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 5, Scp4666State.PhaseDay, 9000L);
        place(User, indoors(Layout, 6.5, 2.5), 0.0f);
        Helper.assertTrue(Scp4666Director.running(World, State), "the clock does not run with a visitor inside");
        User.setGameMode(GameType.SPECTATOR);
        int Epoch = State.epoch();
        int Built = State.built();
        int Known = State.visitors().size();
        boolean Lamps = lit(World, Layout);
        int Fire = fireLevel(World, Layout);
        Helper.assertTrue(Scp4666World.visitors(World).isEmpty() && Scp4666World.living(World).isEmpty(), "a spectator counts as a visitor");
        Helper.assertTrue(!Scp4666Chores.active(World, User), "a spectator can do chores");
        Helper.assertTrue(!Scp4666Director.running(World, State) && Scp4666Director.watched(World), "a watched empty winter reports a running clock");
        Helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    Helper.assertTrue(State.epoch() == Epoch && State.started(), "a spectator alone reset the winter");
                    Helper.assertTrue(State.clock() == 9000L && World.getDayTime() == 9000L, "the clock jumped under a spectator: " + State.clock() + " day time " + World.getDayTime());
                    Helper.assertTrue(State.built() == Built && !Scp4666Builder.pending(World.getServer()), "the cabin was rebuilt under a spectator");
                    Helper.assertTrue(lit(World, Layout) == Lamps && fireLevel(World, Layout) == Fire, "the lights changed under a spectator");
                    Helper.assertTrue(State.visitors().size() == Known && Scp4666Cold.chill(User) == 0.0f, "the spectator was counted or chilled");
                    Scp4666Director.joined(User);
                    Helper.assertTrue(State.visitors().size() == Known, "joining as a spectator made a visitor record");
                    Scp4666Director.reset(World);
                    Scp4666Director.joined(User);
                    Helper.assertTrue(!State.started() && State.visitors().isEmpty(), "a spectator started a new winter");
                    User.setGameMode(GameType.SURVIVAL);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_chore_migrate")
    public static void aWinterInProgressIsPatchedNotRebuilt(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_tenant", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 7, Scp4666State.PhaseDay, 6000L);
        Scp4666Director.setTree(World, Layout, State, 2);
        int Epoch = State.epoch();
        World.setBlock(Layout.Heater, Blocks.FURNACE.defaultBlockState(), 18);
        World.setBlock(Layout.Heater.above(), Blocks.COBBLESTONE_WALL.defaultBlockState(), 18);
        World.setBlock(Layout.CoalBin, Blocks.SPRUCE_LOG.defaultBlockState(), 18);
        State.markBuilt(5);
        Scp4666World.ensureBuilt(World);
        Helper.assertTrue(State.built() == Scp4666World.Signature && !Scp4666Builder.pending(World.getServer()), "the old cabin was not brought up to date");
        Helper.assertTrue(State.epoch() == Epoch && State.night() == 7 && State.known(User.getUUID()) != null, "the winter in progress was reset");
        Helper.assertTrue(World.getBlockState(Layout.Tree).getValue(Scp4666TreeBlock.Lights) == 2, "the cabin was rebuilt instead of patched");
        Helper.assertTrue(World.getBlockState(Layout.Heater).getBlock() instanceof Scp4666HeaterBlock && World.getBlockState(Layout.Heater.above()).getBlock() instanceof Scp4666HeaterBlock,
                "the boiler was not patched in: " + World.getBlockState(Layout.Heater));
        Helper.assertTrue(World.getBlockState(Layout.CoalBin).getBlock() instanceof Scp4666FurnitureBlock, "the coal bin was not patched in");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_chore_windows")
    public static void brokenWindowsLeakAndBoardsBlockHisFace(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_glazier", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 9, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        place(User, indoors(Layout, -5.5, 4.5), 180.0f);
        State.setFire(Scp4666Heat.FireMax);
        State.setCoal(0);
        int Before = Scp4666Heat.warmth(State);
        Helper.assertTrue(Scp4666Chores.crack(World, Layout, State, 0), "the window would not crack");
        Helper.assertTrue(Scp4666Heat.warmth(State) == Before - Scp4666Heat.DraughtWarmth, "a broken window lets no cold in");
        for (BlockPos Cell : Layout.Windows.get(0).cells()) {
            BlockState Found = World.getBlockState(Cell);
            Helper.assertTrue(Found.getBlock() instanceof Scp4666WindowBlock && !Found.getValue(Scp4666WindowBlock.Boarded), "a cracked window cell is " + Found);
        }
        for (int Index = 0; Index < Layout.Windows.size(); Index++) {
            Scp4666Chores.setWindow(World, Layout, State, Index, false, true);
        }
        clearMen(World);
        Scp4666Happenings.windowVisit(World, Layout, World.getRandom());
        boolean Seen = false;
        for (Entity Found : World.getAllEntities()) {
            Seen |= Found instanceof Scp4666YuleMan;
        }
        Helper.assertTrue(!Seen, "he stared in through a boarded window");
        Helper.assertTrue(Scp4666Heat.warmth(State) == Before, "boarded windows still leak");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_chore_midnight")
    public static void midnightTakesTheGiftAndCountsTheNoise(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_giver", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, Scp4666Director.WakeFrom - 1, Scp4666State.PhaseDay, Scp4666Clock.Midnight - 50L);
        place(User, indoors(Layout, -5.5, 2.5), 0.0f);
        Scp4666State.Visitor Entry = State.visitor(User.getUUID());
        User.getInventory().add(new ItemStack(Scp4666Registry.Gift.get()));
        Scp4666Chores.offer(World, User);
        State.setActivity(90.0f);
        State.setFire(0);
        State.setCoal(0);
        Helper.assertTrue(World.getBlockState(Layout.Offering).is(Scp4666Registry.Parcel.get()), "the parcel is not under the tree");
        Helper.assertTrue(Scp4666Director.wakeNight(State) && Scp4666Director.wakeChance(State) > Scp4666Director.MidnightWakeChance, "a loud house does not wake him sooner");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(State.phase() == Scp4666State.PhaseMidnight, "midnight never came"))
                .thenExecute(() -> {
                    Helper.assertTrue(!State.offering() && !World.getBlockState(Layout.Offering).is(Scp4666Registry.Parcel.get()), "the gift was not taken at midnight");
                    Helper.assertTrue(State.activity() <= 90.0f - Scp4666Chores.OfferingCalm + 0.01f, "the gift did not calm him: " + State.activity());
                    Helper.assertTrue(Entry.LoudNights == 1 && Entry.ColdNights == 1, "the night was not counted as loud and cold: " + Entry.LoudNights + " " + Entry.ColdNights);
                    float With = Scp4666Director.luck(Entry);
                    Entry.LoudNights = 0;
                    Entry.ColdNights = 0;
                    float Without = Scp4666Director.luck(Entry);
                    Entry.LoudNights = 1;
                    Entry.ColdNights = 1;
                    Helper.assertTrue(Without - With > Scp4666Director.LoudPenalty + Scp4666Director.ColdPenalty - 0.001f, "a loud cold night cost " + (Without - With) + " luck");
                    State.setActivity(0.0f);
                    Helper.assertTrue(!Scp4666Director.wakeNight(State) && Scp4666Director.wakeChance(State) == Scp4666Director.MidnightWakeChance, "a quiet house still wakes him early");
                })
                .thenSucceed();
    }

    private static final int[][] Corners = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}};

    private static boolean sealing(BlockState Found) {
        return Found.getBlock() instanceof IronBarsBlock || Found.getBlock() instanceof Scp4666WindowBlock || Found.getBlock() instanceof DoorBlock
                || Found.getBlock() instanceof TrapDoorBlock || Found.getBlock() instanceof WallBlock || Found.getBlock() instanceof Scp4666FireplaceBlock
                || Found.getBlock() instanceof Scp4666HeaterBlock;
    }

    private static boolean solidPart(ServerLevel World, BlockPos Pos, BlockState Found, int[] Corner) {
        if (Found.isAir()) {
            return false;
        }
        if (sealing(Found)) {
            return true;
        }
        double X = Corner[0] * 0.5 + 0.25;
        double Y = Corner[1] * 0.5 + 0.25;
        double Z = Corner[2] * 0.5 + 0.25;
        for (AABB Box : Found.getCollisionShape(World, Pos).toAabbs()) {
            if (Box.minX <= X && X <= Box.maxX && Box.minY <= Y && Y <= Box.maxY && Box.minZ <= Z && Z <= Box.maxZ) {
                return true;
            }
        }
        return false;
    }

    public static List<BlockPos> skyLeaks(ServerLevel World, Scp4666Map Layout) {
        int Cx = Layout.Centre.getX();
        int Cz = Layout.Centre.getZ();
        int MinX = (Cx - 13) * 2;
        int MaxX = (Cx + 13) * 2 + 1;
        int MinZ = (Cz - 10) * 2;
        int MaxZ = (Cz + 12) * 2 + 1;
        int MinY = (Layout.BasementMinY - 1) * 2;
        int MaxY = (Layout.CabinMaxY + 4) * 2 + 1;
        Set<Long> Seen = new HashSet<>();
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        Map<Long, Boolean> Solid = new HashMap<>();
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        Function<int[], Boolean> Blocked = Sub -> Solid.computeIfAbsent(BlockPos.asLong(Sub[0], Sub[1], Sub[2]), Key -> {
            Probe.set(Math.floorDiv(Sub[0], 2), Math.floorDiv(Sub[1], 2), Math.floorDiv(Sub[2], 2));
            BlockState Found = World.getBlockState(Probe);
            return solidPart(World, Probe, Found, new int[]{Math.floorMod(Sub[0], 2), Math.floorMod(Sub[1], 2), Math.floorMod(Sub[2], 2)});
        });
        for (int X = Layout.CabinMinX + 1; X < Layout.CabinMaxX; X++) {
            for (int Z = Layout.CabinMinZ + 1; Z < Layout.CabinMaxZ; Z++) {
                for (int Y = Layout.BasementMinY + 1; Y < Layout.Floor + 4 + Math.max(0, 7 - Math.abs(Z - Cz)); Y++) {
                    if (Y == Layout.Floor || Y == Layout.Floor + 4) {
                        continue;
                    }
                    for (int[] Corner : Corners) {
                        int[] Sub = {X * 2 + Corner[0], Y * 2 + Corner[1], Z * 2 + Corner[2]};
                        if (!Blocked.apply(Sub) && Seen.add(BlockPos.asLong(Sub[0], Sub[1], Sub[2]))) {
                            Queue.add(Sub);
                        }
                    }
                }
            }
        }
        List<BlockPos> Leaks = new ArrayList<>();
        int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!Queue.isEmpty()) {
            int[] Sub = Queue.poll();
            if (Sub[1] >= (Layout.CabinMaxY + 3) * 2 || Sub[0] <= MinX || Sub[0] >= MaxX || Sub[2] <= MinZ || Sub[2] >= MaxZ) {
                Leaks.add(new BlockPos(Math.floorDiv(Sub[0], 2), Math.floorDiv(Sub[1], 2), Math.floorDiv(Sub[2], 2)));
                if (Leaks.size() > 20) {
                    break;
                }
                continue;
            }
            for (int[] Step : Steps) {
                int[] Next = {Sub[0] + Step[0], Sub[1] + Step[1], Sub[2] + Step[2]};
                if (Next[1] < MinY || Next[1] > MaxY || !Seen.add(BlockPos.asLong(Next[0], Next[1], Next[2])) || Blocked.apply(Next)) {
                    continue;
                }
                Queue.add(Next);
            }
        }
        return Leaks;
    }

    private static List<BlockPos> glassEdges(ServerLevel World, Scp4666Map Layout) {
        return Scp4666Seal.openGlass(World, new BlockPos(Layout.CabinMinX, Layout.BasementMinY, Layout.CabinMinZ), new BlockPos(Layout.CabinMaxX, Layout.CabinMaxY, Layout.CabinMaxZ));
    }

    private static List<String> yardProblems(ServerLevel World, Scp4666Map Layout) {
        List<String> Wrong = new ArrayList<>();
        for (BlockPos Pos : Scp4666Builder.cabin(World).Cells().keySet()) {
            if (Pos.getX() >= Layout.CabinMinX && Pos.getX() <= Layout.CabinMaxX && Pos.getZ() >= Layout.CabinMinZ && Pos.getZ() <= Layout.CabinMaxZ || Pos.getY() <= Layout.Ground) {
                continue;
            }
            BlockState Found = World.getBlockState(Pos);
            if (Found.isAir() || Found.is(Blocks.SNOW)) {
                continue;
            }
            VoxelShape Shape = Found.getCollisionShape(World, Pos);
            BlockState Below = World.getBlockState(Pos.below());
            if (!Shape.isEmpty() && Shape.min(Direction.Axis.Y) >= 0.5 && !Below.isAir() && !Below.is(Blocks.SNOW)) {
                Wrong.add("hovers at " + Pos.toShortString() + " over " + Below.getBlock().getName().getString());
            }
            if (Pos.getY() == Layout.Ground + 1 && !Below.isFaceSturdy(World, Pos.below(), Direction.UP)) {
                Wrong.add("floats at " + Pos.toShortString() + " over " + Below.getBlock().getName().getString());
            }
            BlockState Above = World.getBlockState(Pos.above());
            VoxelShape Cap = Above.getCollisionShape(World, Pos.above());
            if ((Found.getBlock() instanceof FenceBlock || Found.is(Blocks.CHAIN)) && !Above.isAir() && !Above.is(Blocks.SNOW) && !Cap.isEmpty() && Cap.min(Direction.Axis.Y) > 0.0) {
                Wrong.add("post stops short at " + Pos.toShortString() + " under " + Above.getBlock().getName().getString());
            }
        }
        return Wrong;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_yard_grounded")
    public static void yardPiecesRestOnTheGround(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_well_keeper", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseDay, 6000L);
        List<String> Fresh = yardProblems(World, Layout);
        Helper.assertTrue(Fresh.isEmpty(), "the built yard: " + Fresh);
        age(World, Layout, OldYard);
        List<String> Old = yardProblems(World, Layout);
        Helper.assertTrue(Old.size() >= 7, "the yard check cannot see the old well roof and woodpile caps: " + Old);
        State.markBuilt(8);
        Scp4666World.ensureBuilt(World);
        Helper.assertTrue(State.built() == Scp4666World.Signature, "the yard was not patched");
        List<String> Patched = yardProblems(World, Layout);
        Helper.assertTrue(Patched.isEmpty(), "the patched yard: " + Patched);
        Scp4666Builder.thaw(World);
        List<String> Thawed = yardProblems(World, Layout);
        Helper.assertTrue(Thawed.isEmpty(), "the thawed yard: " + Thawed);
        BlockPos Well = Layout.Centre.offset(-12, 3, 11);
        Helper.assertTrue(World.getBlockState(Well).isFaceSturdy(World, Well, Direction.DOWN) && World.getBlockState(Well.above()).is(Blocks.SNOW),
                "the well roof is not a full beam with snow on it: " + World.getBlockState(Well));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp4666_yule_window_ground")
    public static void theYuleManStandsOnTheGroundAtWindows(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_window_watch", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        fresh(World, 9, Scp4666State.PhaseDay, 13000L);
        clearMen(World);
        List<String> Wrong = new ArrayList<>();
        int OldOnSill = 0;
        for (Scp4666Map.Window Glass : Layout.Windows) {
            Vec3 Feet = Scp4666Happenings.windowSpot(World, Layout, Glass);
            BlockPos Under = BlockPos.containing(Feet.x, Feet.y - 0.05, Feet.z);
            BlockState Ground = World.getBlockState(Under);
            if (Scp4666Happenings.fixture(Ground)) {
                Wrong.add(Glass.Room() + " " + Glass.Pos().toShortString() + " stands on " + Ground.getBlock().getName().getString());
            }
            if (Feet.y > Glass.Pos().getY() - 0.5) {
                Wrong.add(Glass.Room() + " " + Glass.Pos().toShortString() + " feet raised to " + Feet.y);
            }
            if (!Scp4666Happenings.clear(World, Feet)) {
                Wrong.add(Glass.Room() + " " + Glass.Pos().toShortString() + " body overlaps a fixture at " + Feet);
            }
            Vec3 Eye = Feet.add(0.0, Scp4666YuleMan.EyeHeight, 0.0);
            BlockHitResult Sight = World.clip(new ClipContext(Eye, Glass.glass(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (Sight.getType() == HitResult.Type.BLOCK && !Glass.covers(Sight.getBlockPos())) {
                Wrong.add(Glass.Room() + " " + Glass.Pos().toShortString() + " eye line blocked at " + Sight.getBlockPos().toShortString());
            }
            Vec3 Old = Glass.outside(0.75);
            BlockPos OldUnder = BlockPos.containing(Old.x, Glass.Pos().getY() - 1, Old.z);
            if (World.getBlockState(OldUnder).getBlock() instanceof TrapDoorBlock) {
                OldOnSill++;
            }
        }
        Helper.assertTrue(OldOnSill > 0, "the check cannot see the old placement on the sills");
        Helper.assertTrue(Wrong.isEmpty(), "window spots: " + Wrong);
        Scp4666Happenings.windowVisit(World, Layout, World.getRandom());
        Helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    List<Scp4666YuleMan> Men = World.getEntitiesOfClass(Scp4666YuleMan.class, new AABB(Layout.Centre).inflate(40.0));
                    Helper.assertTrue(!Men.isEmpty(), "no one came to the window");
                    for (Scp4666YuleMan Man : Men) {
                        BlockPos Under = BlockPos.containing(Man.getX(), Man.getY() - 0.05, Man.getZ());
                        Helper.assertTrue(!Scp4666Happenings.fixture(World.getBlockState(Under)) && Man.getY() < Layout.Floor + 1.5,
                                "the Yule Man stands on " + World.getBlockState(Under) + " at " + Man.position());
                        Helper.assertTrue(Scp4666Happenings.clear(World, Man.position()), "the Yule Man overlaps a window fixture at " + Man.position());
                    }
                    clearMen(World);
                })
                .thenSucceed();
    }

    private static final String[][] OldDormer = {
            {"-3", "73", "2", "minecraft:dark_oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"},
            {"-1", "73", "2", "minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]"},
            {"-3", "74", "2", "minecraft:air"}, {"-1", "74", "2", "minecraft:air"}, {"-2", "73", "4", "minecraft:air"}, {"-2", "70", "4", "minecraft:air"}};

    private static final String[][] OldYard = {
            {"-13", "67", "11", "minecraft:spruce_slab[type=top,waterlogged=false]"}, {"-12", "67", "11", "minecraft:spruce_slab[type=top,waterlogged=false]"},
            {"-11", "67", "11", "minecraft:spruce_slab[type=top,waterlogged=false]"},
            {"10", "66", "4", "minecraft:spruce_slab[type=top,waterlogged=false]"}, {"10", "66", "5", "minecraft:spruce_slab[type=top,waterlogged=false]"},
            {"2", "66", "7", "minecraft:spruce_slab[type=top,waterlogged=false]"}, {"3", "66", "7", "minecraft:spruce_slab[type=top,waterlogged=false]"},
            {"10", "67", "4", "minecraft:snow[layers=2]"}, {"10", "67", "5", "minecraft:snow[layers=2]"}, {"2", "67", "7", "minecraft:snow[layers=2]"},
            {"3", "67", "7", "minecraft:snow[layers=2]"}};

    private static void age(ServerLevel World, Scp4666Map Layout, String[][] Rows) {
        HolderLookup<Block> Lookup = World.holderLookup(Registries.BLOCK);
        for (String[] Row : Rows) {
            try {
                BlockState Old = BlockStateParser.parseForBlock(Lookup, Row[3], false).blockState();
                World.setBlock(Layout.Centre.offset(Integer.parseInt(Row[0]), Integer.parseInt(Row[1]) - Layout.Centre.getY(), Integer.parseInt(Row[2])), Old, 18);
            } catch (CommandSyntaxException Problem) {
                throw new IllegalStateException(Problem);
            }
        }
    }

    private static void ageCabin(ServerLevel World, Scp4666Map Layout) {
        age(World, Layout, OldDormer);
        age(World, Layout, OldYard);
        for (BlockPos Spot : Layout.Sills) {
            BlockState Found = World.getBlockState(Spot);
            if (Found.getBlock() instanceof TrapDoorBlock) {
                World.setBlock(Spot, Found.setValue(TrapDoorBlock.OPEN, false).setValue(TrapDoorBlock.HALF, Half.TOP), 18);
            }
        }
        for (BlockPos Spot : Layout.InnerSills) {
            World.setBlock(Spot, Blocks.AIR.defaultBlockState(), 18);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_cabin_sealed")
    public static void theCabinHasNoHolesToTheSky(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_roofer", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseDay, 6000L);
        List<BlockPos> Fresh = skyLeaks(World, Layout);
        Helper.assertTrue(Fresh.isEmpty(), "the built cabin lets the sky in at " + Fresh);
        List<BlockPos> FreshEdges = glassEdges(World, Layout);
        Scp4666Seal.Report FreshSeal = Scp4666Seal.scan(World, Layout);
        Helper.assertTrue(FreshEdges.isEmpty(), "glass with an open edge at " + FreshEdges);
        Helper.assertTrue(FreshSeal.sealed(), "seen from inside: snow at " + FreshSeal.Snow() + " outside at " + FreshSeal.Sky());
        BlockPos Dormer = Layout.Centre.offset(-2, 7, 4);
        World.setBlock(Dormer.below(), Blocks.AIR.defaultBlockState(), 18);
        Helper.assertTrue(skyLeaks(World, Layout).isEmpty(), "the old half-block scan was expected to miss the dormer slit");
        Helper.assertTrue(glassEdges(World, Layout).contains(Dormer), "the glass check cannot see the open dormer pane at " + Dormer.toShortString());
        Scp4666Seal.Report Slit = Scp4666Seal.scan(World, Layout);
        Helper.assertTrue(Slit.Snow().contains(Dormer.south()), "the sub-block scan cannot see the snow past the dormer pane: " + Slit.Snow());
        ageCabin(World, Layout);
        List<BlockPos> Old = skyLeaks(World, Layout);
        Helper.assertTrue(!Old.isEmpty(), "the scan cannot see the old dormer holes");
        Scp4666Director.setTree(World, Layout, State, 1);
        int Epoch = State.epoch();
        State.markBuilt(6);
        Scp4666World.ensureBuilt(World);
        List<BlockPos> Patched = skyLeaks(World, Layout);
        Helper.assertTrue(Patched.isEmpty(), "the patched cabin still lets the sky in at " + Patched);
        List<BlockPos> PatchedEdges = glassEdges(World, Layout);
        Scp4666Seal.Report PatchedSeal = Scp4666Seal.scan(World, Layout);
        Helper.assertTrue(PatchedEdges.isEmpty() && PatchedSeal.sealed(), "the patched cabin still shows outside: glass " + PatchedEdges + " snow " + PatchedSeal.Snow() + " sky " + PatchedSeal.Sky());
        Helper.assertTrue(yardProblems(World, Layout).isEmpty(), "the patched yard: " + yardProblems(World, Layout));
        Helper.assertTrue(State.built() == Scp4666World.Signature && State.epoch() == Epoch && State.known(User.getUUID()) != null, "the patch disturbed the winter");
        Helper.assertTrue(World.getBlockState(Layout.Tree).getValue(Scp4666TreeBlock.Lights) == 1, "the patch rebuilt the cabin");
        List<String> Sills = trapdoorProblems(World, Layout, "after the patch");
        Helper.assertTrue(Sills.isEmpty(), "sills after the patch: " + Sills);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4666_no_snow_buildup")
    public static void noSnowPilesUpOutside(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_shoveller", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 6, Scp4666State.PhaseDay, 6000L);
        Helper.assertTrue(Scp4666World.stills(Scp4666World.Key, false) && !Scp4666World.stills(Level.OVERWORLD, true), "the real winter is not always stilled");
        BlockPos Path = new BlockPos(Layout.Path.get(2).getX(), Layout.Ground + 1, Layout.Path.get(2).getZ());
        BlockPos Field = new BlockPos(Layout.Centre.getX() + 15, Layout.Ground + 1, Layout.Centre.getZ() - 14);
        BlockPos Roof = new BlockPos(Layout.Centre.getX(), Layout.Floor + 9, Layout.Centre.getZ() + 3);
        List<BlockPos> Designed = List.of(Path, Field, Roof);
        List<BlockState> Before = new ArrayList<>();
        for (BlockPos Spot : Designed) {
            Before.add(World.getBlockState(Spot));
        }
        Helper.assertTrue(Before.get(2).is(Blocks.SNOW) && Before.get(0).is(Blocks.SNOW), "the test spots are not snow: " + Before);
        List<BlockPos> Drifts = new ArrayList<>(List.of(Layout.ChoppingBlock.above(), Layout.CoalBin.above(), Layout.FrontDoor.south(), Layout.Sills.get(0).above()));
        World.setBlock(Path, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 6), 18);
        for (BlockPos Spot : Drifts) {
            if (World.getBlockState(Spot).isAir()) {
                World.setBlock(Spot, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3), 18);
            }
        }
        long Calls = Scp4666World.stilled();
        for (int Index = 0; Index < 200; Index++) {
            World.tickPrecipitation(Path);
            World.tickPrecipitation(Layout.ChoppingBlock);
        }
        Helper.assertTrue(Scp4666World.stilled() - Calls == 400, "precipitation ran on the site");
        State.markBuilt(Scp4666World.Signature - 1);
        Scp4666World.ensureBuilt(World);
        List<String> Wrong = new ArrayList<>();
        for (int Index = 0; Index < Designed.size(); Index++) {
            if (World.getBlockState(Designed.get(Index)) != Before.get(Index)) {
                Wrong.add("designed snow at " + Designed.get(Index).toShortString() + " became " + World.getBlockState(Designed.get(Index)));
            }
        }
        for (BlockPos Spot : Drifts) {
            if (World.getBlockState(Spot).is(Blocks.SNOW)) {
                Wrong.add("a drift stayed at " + Spot.toShortString());
            }
        }
        Scp4666Builder.Sketch Woods = Scp4666Builder.sketch(World);
        BlockPos Far = new BlockPos(Layout.Centre.getX() + 40, Layout.Ground + 1, Layout.Centre.getZ() - 33);
        BlockState FarBefore = World.getBlockState(Far);
        if (FarBefore.is(Blocks.SNOW)) {
            World.setBlock(Far, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8), 18);
        }
        BlockPos Crown = null;
        BlockPos Capped = null;
        for (int X = Layout.Centre.getX() + 25; X < Layout.Centre.getX() + 55 && (Crown == null || Capped == null); X++) {
            for (int Z = Layout.Centre.getZ() - 50; Z < Layout.Centre.getZ() + 50 && (Crown == null || Capped == null); Z++) {
                for (int Y = Layout.Ground + 2; Y < Layout.Ground + 20; Y++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    if (Crown == null && Woods.getBlockState(Spot).is(Blocks.SPRUCE_LEAVES) && Woods.getBlockState(Spot.above()).isAir() && World.getBlockState(Spot.above()).isAir()) {
                        Crown = Spot.above();
                    }
                    if (Capped == null && Woods.getBlockState(Spot).is(Blocks.SNOW) && World.getBlockState(Spot).equals(Woods.getBlockState(Spot))) {
                        Capped = Spot;
                    }
                }
            }
        }
        Helper.assertTrue(Crown != null && Capped != null, "no bare treetop or snowy cap found in the woods");
        BlockState CappedBefore = World.getBlockState(Capped);
        World.setBlock(Crown, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2), 18);
        Scp4666Builder.thaw(World);
        if (World.getBlockState(Crown).is(Blocks.SNOW)) {
            Wrong.add("snow stayed on a bare treetop at " + Crown.toShortString());
        }
        if (FarBefore.is(Blocks.SNOW) && World.getBlockState(Far) != FarBefore) {
            Wrong.add("a deep drift far out became " + World.getBlockState(Far) + " not " + FarBefore);
        }
        if (World.getBlockState(Capped) != CappedBefore) {
            Wrong.add("designed snow in the woods changed at " + Capped.toShortString());
        }
        for (int X = Layout.TemplateOrigin.getX(); X <= Layout.TemplateFar.getX(); X++) {
            for (int Z = Layout.TemplateOrigin.getZ(); Z <= Layout.TemplateFar.getZ(); Z++) {
                if (Scp4666Builder.snowDepth(Layout, X, Z) > Scp4666Builder.ShelterDepth) {
                    Wrong.add("a deep drift is built by the cabin at " + X + " " + Z);
                    X = Layout.TemplateFar.getX() + 1;
                    break;
                }
            }
        }
        World.setBlock(Path, Scp4666Registry.Footprints.get().defaultBlockState(), 18);
        State.addPrint(Path);
        Scp4666Director.restorePrints(World, State);
        if (World.getBlockState(Path) != Before.get(0)) {
            Wrong.add("a footprint healed to " + World.getBlockState(Path) + " instead of " + Before.get(0));
        }
        Helper.assertTrue(Wrong.isEmpty(), "snow: " + Wrong);
        Helper.succeed();
    }

    private static boolean interior(Scp4666Map Layout, BlockPos Spot) {
        int Dx = Spot.getX() - Layout.Centre.getX();
        int Dz = Spot.getZ() - Layout.Centre.getZ();
        if (Spot.getX() <= Layout.CabinMinX || Spot.getX() >= Layout.CabinMaxX || Spot.getZ() <= Layout.CabinMinZ || Spot.getZ() >= Layout.CabinMaxZ) {
            return false;
        }
        int Y = Spot.getY();
        int Roof = Layout.Floor + 4 + 7 - Math.abs(Dz);
        return Y > Layout.BasementMinY && Y < Layout.Floor || Y > Layout.Floor && Y < Layout.Floor + 4 || Y > Layout.Floor + 4 && Y < Roof && Math.abs(Dx) < 9;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_no_indoor_snow")
    public static void noSnowSettlesIndoors(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_indoors", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 12, Scp4666State.PhaseMidnight, Scp4666Clock.Midnight);
        place(User, indoors(Layout, -5.5, 4.5), 180.0f);
        for (int Index = 0; Index < Layout.Windows.size(); Index++) {
            Scp4666Chores.crack(World, Layout, State, Index);
        }
        Scp4666Happenings.snowInside(World, Layout, World.getRandom());
        Helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    List<BlockPos> Found = new ArrayList<>();
                    for (int X = Layout.CabinMinX; X <= Layout.CabinMaxX; X++) {
                        for (int Z = Layout.CabinMinZ; Z <= Layout.CabinMaxZ; Z++) {
                            for (int Y = Layout.BasementMinY; Y <= Layout.CabinMaxY; Y++) {
                                BlockPos Spot = new BlockPos(X, Y, Z);
                                if (interior(Layout, Spot) && (World.getBlockState(Spot).is(Blocks.SNOW) || World.getBlockState(Spot).is(Blocks.SNOW_BLOCK))) {
                                    Found.add(Spot);
                                }
                            }
                        }
                    }
                    Helper.assertTrue(Found.isEmpty(), "snow inside the cabin at " + Found);
                })
                .thenSucceed();
    }

    private static List<Scp4666Objectives.Line> tasksOf(ServerLevel World, ServerPlayer User) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        return Scp4666Objectives.compute(Scp4666Objectives.facts(World, Layout, Scp4666State.get(World.getServer()), User));
    }

    private static Scp4666Objectives.Line taskOf(List<Scp4666Objectives.Line> Lines, Scp4666Objectives.Task Kind) {
        for (Scp4666Objectives.Line Each : Lines) {
            if (Each.Kind() == Kind) {
                return Each;
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_hearth")
    public static void aDeadHearthIsUrgent(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_noter", 4.5, 4.5);
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, 3000L);
        State.setFire(0);
        List<Scp4666Objectives.Line> Lines = tasksOf(World, User);
        Scp4666Objectives.Line Hearth = taskOf(Lines, Scp4666Objectives.Task.Hearth);
        Helper.assertTrue(Hearth != null && Hearth.Status() == Scp4666Objectives.Urgent && Hearth.Value() == 0, "a dead hearth is not urgent: " + Lines);
        Helper.assertTrue(Lines.get(0).Kind() == Scp4666Objectives.Task.Hearth, "the dead hearth is not first: " + Lines);
        Scp4666Objectives.Line Wood = taskOf(Lines, Scp4666Objectives.Task.Firewood);
        Helper.assertTrue(Wood != null && Wood.Status() == Scp4666Objectives.Urgent, "no firewood task for a dead hearth: " + Lines);
        State.setFire(Scp4666Heat.FireMax);
        Lines = tasksOf(World, User);
        Hearth = taskOf(Lines, Scp4666Objectives.Task.Hearth);
        Helper.assertTrue(Hearth != null && Hearth.Status() == Scp4666Objectives.Done && taskOf(Lines, Scp4666Objectives.Task.Firewood) == null, "a roaring hearth still asks for work: " + Lines);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_windows")
    public static void crackedWindowsAreCounted(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_glass_note", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 7, Scp4666State.PhaseDay, 3000L);
        Helper.assertTrue(taskOf(tasksOf(World, User), Scp4666Objectives.Task.Windows) == null, "a whole cabin asks for boards");
        Scp4666Chores.crack(World, Layout, State, 1);
        Scp4666Chores.crack(World, Layout, State, 4);
        Scp4666Objectives.Line Boards = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Windows);
        Helper.assertTrue(Boards != null && Boards.Value() == 2 && Boards.Step() == 0, "two cracked windows gave " + Boards);
        User.getInventory().add(new ItemStack(Scp4666Registry.Boards.get(), 2));
        Boards = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Windows);
        Helper.assertTrue(Boards != null && Boards.Step() == 1, "carrying boards did not move to nailing: " + Boards);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_bread")
    public static void breadIsAskedForOnceTheBoxStops(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_baker", 4.5, 4.5);
        Scp4666State State = fresh(World, Scp4666State.FoodStops - 1, Scp4666State.PhaseDay, 3000L);
        Helper.assertTrue(taskOf(tasksOf(World, User), Scp4666Objectives.Task.Bread) == null, "bread was asked for while the box still brings food");
        State.setNight(Scp4666State.FoodStops);
        Scp4666Objectives.Line Bread = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Bread);
        Helper.assertTrue(Bread != null && Bread.Status() == Scp4666Objectives.Pending && Bread.Step() == 0, "no bread task on day ten: " + Bread);
        User.getInventory().add(new ItemStack(Scp4666Registry.Oats.get()));
        Bread = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Bread);
        Helper.assertTrue(Bread != null && Bread.Step() == 1, "carrying oats did not point at the stove: " + Bread);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_gift")
    public static void theGiftLineIsDoneOncePlaced(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_wrapper", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        place(User, indoors(Layout, -6.5, 0.5), 270.0f);
        Scp4666Objectives.Line Gift = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gift);
        Helper.assertTrue(Gift != null && Gift.Status() == Scp4666Objectives.Pending && Gift.Step() == 0, "no gift task in the morning: " + Gift);
        User.getInventory().add(new ItemStack(Scp4666Registry.Gift.get()));
        Gift = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gift);
        Helper.assertTrue(Gift != null && Gift.Step() == 1, "carrying a gift did not point at the tree: " + Gift);
        Scp4666Chores.offer(World, User);
        Gift = taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gift);
        Helper.assertTrue(Gift != null && Gift.Status() == Scp4666Objectives.Done, "the placed gift is not done: " + Gift);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_spectator")
    public static void spectatorsOnlySeeTheNight(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_onlooker", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 5, Scp4666State.PhaseDay, 3000L);
        State.setFire(0);
        Helper.assertTrue(!Scp4666Objectives.forPlayer(World, Layout, State, User).isEmpty(), "a visitor gets no tasks");
        User.setGameMode(GameType.SPECTATOR);
        Helper.assertTrue(Scp4666Objectives.forPlayer(World, Layout, State, User).isEmpty(), "a spectator gets tasks");
        User.setGameMode(GameType.SURVIVAL);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_objective_order")
    public static void tasksAreListedUrgentFirst(GameTestHelper Helper) {
        Scp4666Objectives.Facts Busy = new Scp4666Objectives.Facts(11, Scp4666State.PhaseMidnight, false, 0, 3, 1, 55.0f, 0, false, 0, false, false, false, false, 0, 0,
                true, false, false, true, false);
        List<Scp4666Objectives.Task> Order = new ArrayList<>();
        for (Scp4666Objectives.Line Each : Scp4666Objectives.compute(Busy)) {
            Order.add(Each.Kind());
            Helper.assertTrue(Scp4666Objectives.Line.unpack(Each.pack()).equals(Each), "a task did not survive packing: " + Each);
        }
        List<Scp4666Objectives.Task> Expected = List.of(Scp4666Objectives.Task.Freezing, Scp4666Objectives.Task.Hearth, Scp4666Objectives.Task.Firewood,
                Scp4666Objectives.Task.Gear, Scp4666Objectives.Task.Windows, Scp4666Objectives.Task.Sleep, Scp4666Objectives.Task.Bread, Scp4666Objectives.Task.Gift,
                Scp4666Objectives.Task.Boiler, Scp4666Objectives.Task.Calendar);
        Helper.assertTrue(Order.equals(Expected), "task order " + Order);
        Scp4666Objectives.Facts Ending = new Scp4666Objectives.Facts(14, Scp4666State.PhaseEnding, false, 0, 0, 0, 90.0f, 0, false, 0, false, false, false, false, 0, 0,
                false, false, false, true, false);
        Helper.assertTrue(Scp4666Objectives.compute(Ending).isEmpty(), "tasks show during the ending");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_objective_list")
    public static void theChoreListIsPinnedAndFree(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_reader", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        Helper.assertTrue(World.getBlockState(Layout.ChoreList).is(Scp4666Registry.ChoreList.get()), "no chore list in the kitchen: " + World.getBlockState(Layout.ChoreList));
        List<Integer> Sent = Scp4666Objectives.forPlayer(World, Layout, State, User);
        Helper.assertTrue(Sent.equals(Scp4666Objectives.packed(Scp4666Objectives.compute(Scp4666Objectives.facts(World, Layout, State, User)))), "the list does not show the computed tasks");
        State.setActivity(0.0f);
        long Clock = State.clock();
        place(User, Vec3.atBottomCenterOf(Layout.ChoreList).add(-1.0, -1.0, 0.0), 270.0f);
        World.getBlockState(Layout.ChoreList).useWithoutItem(World, User, hit(Layout.ChoreList));
        Helper.assertTrue(State.activity() == 0.0f && State.clock() == Clock, "reading the list cost activity or time");
        World.setBlock(Layout.ChoreList, Blocks.AIR.defaultBlockState(), 18);
        State.markBuilt(7);
        Scp4666World.ensureBuilt(World);
        Helper.assertTrue(World.getBlockState(Layout.ChoreList).is(Scp4666Registry.ChoreList.get()), "the patch did not pin the chore list in an old cabin");
        User.setGameMode(GameType.SPECTATOR);
        Helper.assertTrue(Scp4666Objectives.forPlayer(World, Layout, State, User).isEmpty(), "a spectator sees personal lines");
        User.setGameMode(GameType.SURVIVAL);
        Helper.succeed();
    }

    private static BlockPos hooks(Scp4666Map Layout) {
        return new BlockPos(-3, Layout.Floor + 2, 5);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_gear_warmth")
    public static void eachPieceCutsTheColdButNeverStopsIt(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_dresser", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseDay, Scp4666Clock.Dusk + 200L);
        place(User, new Vec3(0.5, Layout.Floor + 1.0, 20.5), 0.0f);
        float Bare = Scp4666Cold.rate(Layout, State, User);
        Helper.assertTrue(Bare == Scp4666Cold.OutsideNight, "bare chill " + Bare);
        List<String> Wrong = new ArrayList<>();
        Object[][] Pieces = {{EquipmentSlot.HEAD, Scp4666Registry.WinterHat.get(), Scp4666Registry.HatShare}, {EquipmentSlot.CHEST, Scp4666Registry.WinterCoat.get(), Scp4666Registry.CoatShare},
                {EquipmentSlot.FEET, Scp4666Registry.WinterBoots.get(), Scp4666Registry.BootsShare}};
        for (Object[] Piece : Pieces) {
            User.setItemSlot((EquipmentSlot) Piece[0], new ItemStack((Item) Piece[1]));
            float Rate = Scp4666Cold.rate(Layout, State, User);
            if (Math.abs(Rate - Bare * (1.0f - (float) Piece[2])) > 1.0E-4f) {
                Wrong.add(Piece[1] + " alone gives " + Rate);
            }
            User.setItemSlot((EquipmentSlot) Piece[0], ItemStack.EMPTY);
        }
        for (Object[] Piece : Pieces) {
            User.setItemSlot((EquipmentSlot) Piece[0], new ItemStack((Item) Piece[1]));
        }
        float Dressed = Scp4666Cold.rate(Layout, State, User);
        if (Dressed <= 0.0f || Dressed > Bare * 0.5f) {
            Wrong.add("the full set gives " + Dressed + " against " + Bare);
        }
        Helper.assertTrue(Wrong.isEmpty(), "gear warmth: " + Wrong);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_gear_wear")
    public static void gearOnlyWearsOutside(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_walker", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        place(User, indoors(Layout, 6.5, 2.5), 0.0f);
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp4666Registry.WinterCoat.get()));
        Helper.startSequence()
                .thenIdle(50)
                .thenExecute(() -> {
                    Helper.assertTrue(User.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0, "the coat wore indoors");
                    place(User, new Vec3(0.5, Layout.Floor + 1.0, 20.5), 0.0f);
                })
                .thenIdle(50)
                .thenExecute(() -> {
                    ItemStack Coat = User.getItemBySlot(EquipmentSlot.CHEST);
                    Helper.assertTrue(Coat.is(Scp4666Registry.WinterCoat.get()) && Coat.getDamageValue() >= 2, "the coat did not wear outside: " + Coat.getDamageValue());
                    Coat.setDamageValue(Coat.getMaxDamage() - 1);
                })
                .thenIdle(25)
                .thenExecute(() -> Helper.assertTrue(User.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "a worn out coat did not tear"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_gear_hooks")
    public static void eachHookHoldsOneSetPerWinter(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_hanger", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        BlockPos Hook = hooks(Layout);
        Helper.assertTrue(World.getBlockState(Hook).is(Scp4666Registry.CoatHooks.get()), "no coat hooks by the door: " + World.getBlockState(Hook));
        place(User, Vec3.atBottomCenterOf(Hook).add(0.0, -1.0, -1.0), 0.0f);
        float Activity = State.activity();
        World.getBlockState(Hook).useWithoutItem(World, User, hit(Hook));
        Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).is(Scp4666Registry.WinterHat.get()) && User.getItemBySlot(EquipmentSlot.CHEST).is(Scp4666Registry.WinterCoat.get())
                && User.getItemBySlot(EquipmentSlot.FEET).is(Scp4666Registry.WinterBoots.get()), "the hook did not dress the visitor");
        BlockState Bare = World.getBlockState(Hook);
        Helper.assertTrue(!Bare.getValue(Scp4666CoatHooksBlock.Hat) && !Bare.getValue(Scp4666CoatHooksBlock.Coat) && !Bare.getValue(Scp4666CoatHooksBlock.Boots), "the hook still shows gear");
        World.getBlockState(Hook).useWithoutItem(World, User, hit(Hook));
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            Count += User.getInventory().getItem(Slot).getItem() instanceof Scp4666WinterGearItem ? 1 : 0;
        }
        Helper.assertTrue(Count == 3, "the hook gave a second set: " + Count + " pieces");
        ItemStack Coat = User.getItemBySlot(EquipmentSlot.CHEST);
        Coat.setDamageValue(57);
        User.setShiftKeyDown(true);
        World.getBlockState(Hook).useWithoutItem(World, User, hit(Hook));
        User.setShiftKeyDown(false);
        Helper.assertTrue(User.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && World.getBlockState(Hook).getValue(Scp4666CoatHooksBlock.Coat), "the coat was not hung back");
        World.getBlockState(Hook).useWithoutItem(World, User, hit(Hook));
        Helper.assertTrue(User.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 57, "the hung coat lost its wear: " + User.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
        Helper.assertTrue(State.activity() == Activity, "taking gear cost activity");
        for (EquipmentSlot Slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
            User.setItemSlot(Slot, ItemStack.EMPTY);
        }
        State.reset();
        State.start();
        World.getBlockState(Hook).useWithoutItem(World, User, hit(Hook));
        Helper.assertTrue(User.getItemBySlot(EquipmentSlot.CHEST).is(Scp4666Registry.WinterCoat.get()) && User.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0,
                "a new winter did not restock the hook");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_gear_stays")
    public static void gearNeverLeavesTheWinter(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_smuggler", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp4666Registry.WinterCoat.get()));
        User.getInventory().add(new ItemStack(Scp4666Registry.WinterHat.get()));
        GatewayTravel.leave(User);
        boolean Kept = false;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            Kept |= User.getInventory().getItem(Slot).getItem() instanceof Scp4666WinterGearItem;
        }
        Helper.assertTrue(!Scp4666World.inside(User) && !Kept, "winter gear left the winter");
        ItemStack Stray = new ItemStack(Scp4666Registry.WinterBoots.get());
        Stray.inventoryTick(User.level(), User, 0, false);
        Helper.assertTrue(Stray.isEmpty() && Scp4666WinterGearItem.warmth(User) == 0.0f, "winter gear works outside the winter");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_gear_objective")
    public static void theCoatLineShowsOutsideWithoutOne(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_shiver_note", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        fresh(World, 4, Scp4666State.PhaseDay, 3000L);
        place(User, indoors(Layout, 6.5, 2.5), 0.0f);
        Helper.assertTrue(taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gear) == null, "asked to dress up indoors");
        place(User, new Vec3(0.5, Layout.Floor + 1.0, 20.5), 0.0f);
        Helper.assertTrue(taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gear) != null, "no dress-warmly line outside");
        User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp4666Registry.WinterCoat.get()));
        Helper.assertTrue(taskOf(tasksOf(World, User), Scp4666Objectives.Task.Gear) == null, "the line stayed with a coat on");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_clock_speed")
    public static void winterDaysRunAtTheChosenLength(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_clock_keeper", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        GameRules.IntegerValue Minutes = World.getGameRules().getRule(Scp4666Registry.DayMinutes);
        int Before = Minutes.get();
        Minutes.set(Scp4666Registry.DefaultDayMinutes, World.getServer());
        float Rate = Scp4666Clock.rate(World.getServer());
        double[] Owed = new double[2];
        long Day = 0L;
        int Ticks = Scp4666Registry.DefaultDayMinutes * 1200;
        for (int Index = 0; Index < Ticks; Index++) {
            Day += Scp4666Director.whole(Owed, 0, Rate);
        }
        Helper.assertTrue(Math.abs(Day - Scp4666Clock.Day) <= 1L, "an " + Scp4666Registry.DefaultDayMinutes + " minute day moved the clock " + Day + " not " + Scp4666Clock.Day);
        Minutes.set(20, World.getServer());
        Helper.assertTrue(Math.abs(Scp4666Clock.rate(World.getServer()) - 1.0f) < 1.0e-6f, "a 20 minute day is not one tick per tick");
        Minutes.set(Scp4666Registry.DefaultDayMinutes, World.getServer());
        Scp4666State State = fresh(World, 4, Scp4666State.PhaseDay, Scp4666Clock.Dusk - 24L);
        place(User, indoors(Layout, 6.5, 2.5), 0.0f);
        int Night = State.night();
        long[] Start = {State.clock()};
        Scp4666ClockModel Model = new Scp4666ClockModel();
        Model.accept(State.clock(), Scp4666Director.running(World, State), State.phase(), Rate);
        long[] Last = {State.clock()};
        double[] Shown = {Model.shown(0.0f)};
        int[] Lagging = {0};
        List<String> Wrong = new ArrayList<>();
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    State.fire("probe_dusk");
                    Start[0] = State.clock();
                    Last[0] = State.clock();
                    Model.accept(State.clock(), Scp4666Director.running(World, State), State.phase(), Rate);
                    Shown[0] = Model.shown(0.0f);
                })
                .thenExecuteFor(30, () -> {
                    if (World.getDayTime() != State.clock()) {
                        Wrong.add("day time " + World.getDayTime() + " clock " + State.clock());
                    }
                    if (Math.abs(State.clock() - Last[0]) > 3L || World.getServer().getTickCount() % Scp4666Director.SyncEvery == 0) {
                        Model.accept(State.clock(), Scp4666Director.running(World, State), State.phase(), Rate);
                    }
                    Last[0] = State.clock();
                    Model.tick();
                    double Now = Model.shown(0.0f);
                    if (Now < Shown[0] - 1.0e-6) {
                        Wrong.add("the shown time went back from " + Shown[0] + " to " + Now);
                    }
                    Shown[0] = Now;
                    if (Math.abs(Now - State.clock()) > Rate * 4.0 + 1.0) {
                        Lagging[0]++;
                    }
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Wrong.isEmpty(), "clock: " + Wrong.subList(0, Math.min(5, Wrong.size())));
                    long Moved = State.clock() - Start[0];
                    Helper.assertTrue(Moved >= (long) (30 * Rate) - 3L && Moved <= (long) (30 * Rate) + 3L, "the clock moved " + Moved + " in 30 ticks at rate " + Rate);
                    Helper.assertTrue(!State.fired("probe_dusk") && State.night() == Night && State.phase() == Scp4666State.PhaseDay && Scp4666Clock.night(State.clock()),
                            "dusk did not fall in order at the faster speed");
                    Helper.assertTrue(Lagging[0] <= 2, "the client clock strayed from the server " + Lagging[0] + " times");
                    Scp4666Chores.perform(World, User, Scp4666Chores.Chore.Bake);
                    Model.accept(State.clock(), Scp4666Director.running(World, State), State.phase(), Rate);
                    Helper.assertTrue(Model.snaps() == 1 && Math.abs(Model.lastError()) > 100.0, "a chore skip was not seen as a smooth correction: " + Model.lastError());
                    double Gap = Model.target() - Model.shown(0.0f);
                    for (int Index = 0; Index < 20; Index++) {
                        Model.tick();
                    }
                    Helper.assertTrue(Math.abs(Model.target() - Model.shown(0.0f)) < Gap * 0.1, "the chore skip did not ease in over 20 ticks");
                    State.setClock(Scp4666Clock.Midnight - 10L);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    Helper.assertTrue(State.phase() == Scp4666State.PhaseMidnight && State.clock() == Scp4666Clock.Midnight && World.getDayTime() == Scp4666Clock.Midnight,
                            "midnight did not come in order: phase " + State.phase() + " clock " + State.clock());
                    Minutes.set(Before, World.getServer());
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4666_clock_exempt")
    public static void exemptPlayersNeverResetTheWinter(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_builder_mode", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 5, Scp4666State.PhaseDay, 9000L);
        place(User, indoors(Layout, 6.5, 2.5), 0.0f);
        int Epoch = State.epoch();
        User.setGameMode(GameType.CREATIVE);
        Helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    Helper.assertTrue(State.epoch() == Epoch && State.started() && World.getDayTime() == State.clock(), "a creative player reset the winter or split the clock");
                    User.setGameMode(GameType.SPECTATOR);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    Helper.assertTrue(State.epoch() == Epoch && State.started() && World.getDayTime() == State.clock(), "a spectator reset the winter or split the clock");
                    Helper.assertTrue(!Scp4666Builder.pending(World.getServer()), "a rebuild was queued with a player present");
                    User.setGameMode(GameType.SURVIVAL);
                })
                .thenIdle(10)
                .thenExecute(() -> Helper.assertTrue(State.epoch() == Epoch && Scp4666Director.running(World, State), "the winter did not carry on after switching back"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_audio_events")
    public static void audioEventsAreHalvedButVisualsStay(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        List<String> Wrong = new ArrayList<>();
        for (Scp4666Director.Plan Entry : Scp4666Director.plans()) {
            float Full = Scp4666Director.chanceOf(Entry, 0.0f, 1.0f);
            float Half = Scp4666Director.chanceOf(Entry, 0.0f, 0.5f);
            if (Entry.Audio() && Math.abs(Half - Full * 0.5f) > 1.0e-5f || !Entry.Audio() && Half != Full) {
                Wrong.add(Entry.Key() + " " + Full + " to " + Half);
            }
        }
        for (Scp4666Director.Plan Entry : Scp4666Director.plans()) {
            if (Scp4666Director.chanceOf(Entry, 60.0f, 0.5f) <= Scp4666Director.chanceOf(Entry, 0.0f, 0.5f) && Entry.Chance() < Scp4666Director.EventCap) {
                Wrong.add(Entry.Key() + " ignores activity");
            }
        }
        Helper.assertTrue(Wrong.isEmpty(), "audio scaling: " + Wrong);
        Helper.assertTrue(Math.abs(Scp4666Registry.audioShare(World.getServer()) - Scp4666Registry.DefaultAudioEvents / 100.0f) < 1.0e-6f, "the audio rule default is not half");
        for (int Night = 1; Night <= Scp4666State.FinalNight; Night++) {
            float Old = Scp4666Director.expectedEvents(Night, 0.0f, 1.0f, true);
            float Now = Scp4666Director.expectedEvents(Night, 0.0f, Scp4666Registry.audioShare(World.getServer()), true);
            Helper.assertTrue(Old == 0.0f || Now <= Old * 0.55f, "night " + Night + " still has " + Now + " audio events of " + Old);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_lights_steady")
    public static void hearthAndLampsNeverFlap(GameTestHelper Helper) {
        ServerLevel World = prepare(Helper);
        GatewayTraveller User = enter(Helper, "scp4666_lamp_watch", 4.5, 4.5);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = fresh(World, 3, Scp4666State.PhaseDay, 3000L);
        State.setFire(Scp4666Heat.FireMax);
        State.setCoal(Scp4666Heat.CoalMax);
        Scp4666Heat.refresh(World, Layout, State);
        float Rate = Scp4666Clock.rate(World.getServer());
        double[] Owed = new double[2];
        int Fire = fireLevel(World, Layout);
        boolean Lamps = lit(World, Layout);
        int Changes = 0;
        List<String> Wrong = new ArrayList<>();
        for (int Index = 0; Index < Scp4666Heat.FireMax; Index++) {
            Scp4666Heat.tick(World, Layout, State, Scp4666Director.whole(Owed, 1, Rate));
            int Now = fireLevel(World, Layout);
            if (Now > Fire) {
                Wrong.add("the hearth rose from " + Fire + " to " + Now + " at " + Index);
            }
            if (Now != Fire) {
                Changes++;
            }
            Fire = Now;
            if (lit(World, Layout) != Lamps) {
                Wrong.add("the lamps toggled at " + Index);
                Lamps = lit(World, Layout);
            }
        }
        Helper.assertTrue(Wrong.isEmpty() && Changes <= Scp4666Heat.FireLevels, "lights flapped: " + Wrong + " changes " + Changes);
        Helper.assertTrue(Fire == 0, "the hearth did not burn out at the day speed");
        Helper.succeed();
    }
}
