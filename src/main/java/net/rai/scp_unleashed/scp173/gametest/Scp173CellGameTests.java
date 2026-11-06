package net.rai.scp_unleashed.scp173.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containment.ContainmentRegistry;
import net.rai.scp_unleashed.containmentzone.CellStackBlock;
import net.rai.scp_unleashed.containmentzone.ContainmentZoneRegistry;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassGaze;
import net.rai.scp_unleashed.decor.DecorRegistry;
import net.rai.scp_unleashed.decor.FacilityZone;
import net.rai.scp_unleashed.decor.ZonePart;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import net.rai.scp_unleashed.props.LockerBlock;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.props.TallProps;
import net.rai.scp_unleashed.scp173.Scp173Clones;
import net.rai.scp_unleashed.scp173.Scp173Passage;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Residue;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp173CellGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final double Floor = 2.0;
    public static final int Ground = 2;
    public static final int Held = 100000;
    public static final int Centre = 12;
    public static final int Ring = 2;
    public static final int Rows = 3;
    public static final int WatchTicks = 300;
    public static final int SealedTimeout = 400;
    public static final int OpenTimeout = 600;
    public static final int CloneTimeout = 300;
    public static final int CloneWatch = 200;
    public static final int SeepTries = 60;
    public static final long SeepSeed = 173L;
    public static final double Slack = 1.0e-3;
    public static final double OutsideX = Centre + Ring + 1.5;

    private Scp173CellGameTests() {
    }

    private interface Mason {
        BlockState at(Direction Outward, int Row);
    }

    private static void quiet(GameTestHelper Helper) {
        Helper.onEachTick(() -> {
            for (Monster Stray : Helper.getLevel().getEntitiesOfClass(Monster.class, Helper.getBounds().inflate(8.0))) {
                if ("minecraft".equals(EntityType.getKey(Stray.getType()).getNamespace())) {
                    Stray.discard();
                }
            }
        });
    }

    private static DClass person(GameTestHelper Helper, double X, double Z) {
        DClass Made = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
        Made.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        return Made;
    }

    private static Scp173Statue statue(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp173Statue Made = Scp173Registry.Statue.get().create(World);
        if (Made == null) {
            throw new IllegalStateException("SCP-173 could not be created");
        }
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Made);
        return Made;
    }

    private static void faceAway(LivingEntity Body, Vec3 From) {
        Vec3 Eye = Body.getEyePosition();
        double Dx = Eye.x - From.x;
        double Dz = Eye.z - From.z;
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        Body.setYRot(Yaw);
        Body.yRotO = Yaw;
        Body.setYHeadRot(Yaw);
        Body.yHeadRotO = Yaw;
        Body.setYBodyRot(Yaw);
        Body.yBodyRotO = Yaw;
        Body.setXRot(0.0f);
        Body.xRotO = 0.0f;
    }

    private static DClass prey(GameTestHelper Helper, Scp173Statue Statue, double X, double Z) {
        DClass Victim = person(Helper, X, Z);
        faceAway(Victim, Statue.position().add(0.0, 1.0, 0.0));
        DClassGaze.holdEyes(Victim, Held);
        return Victim;
    }

    private static Direction outward(int Dx, int Dz) {
        if (Dx == Ring) {
            return Direction.EAST;
        }
        if (Dx == -Ring) {
            return Direction.WEST;
        }
        return Dz == Ring ? Direction.SOUTH : Direction.NORTH;
    }

    private static void put(GameTestHelper Helper, BlockPos Relative, BlockState State, List<BlockPos> Placed) {
        BlockPos Absolute = Helper.absolutePos(Relative);
        Helper.getLevel().setBlock(Absolute, State, Block.UPDATE_CLIENTS);
        Placed.add(Absolute);
    }

    private static void settle(GameTestHelper Helper, List<BlockPos> Placed) {
        ServerLevel World = Helper.getLevel();
        for (BlockPos Absolute : Placed) {
            BlockState Current = World.getBlockState(Absolute);
            BlockState Settled = Block.updateFromNeighbourShapes(Current, World, Absolute);
            if (Settled != Current && !Settled.isAir()) {
                World.setBlock(Absolute, Settled, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void build(GameTestHelper Helper, Mason Walls, @Nullable BlockState Roof) {
        List<BlockPos> Placed = new ArrayList<>();
        for (int Dx = -Ring; Dx <= Ring; Dx++) {
            for (int Dz = -Ring; Dz <= Ring; Dz++) {
                if (Math.abs(Dx) != Ring && Math.abs(Dz) != Ring) {
                    continue;
                }
                Direction Outward = outward(Dx, Dz);
                for (int Row = 0; Row < Rows; Row++) {
                    BlockState State = Walls.at(Outward, Row);
                    if (State != null) {
                        put(Helper, new BlockPos(Centre + Dx, Ground + Row, Centre + Dz), State, Placed);
                    }
                }
            }
        }
        if (Roof != null) {
            for (int Dx = -Ring; Dx <= Ring; Dx++) {
                for (int Dz = -Ring; Dz <= Ring; Dz++) {
                    put(Helper, new BlockPos(Centre + Dx, Ground + Rows, Centre + Dz), Roof, Placed);
                }
            }
        }
        settle(Helper, Placed);
    }

    private static AABB interior(GameTestHelper Helper, double Top) {
        Vec3 Low = Helper.absoluteVec(new Vec3(Centre - Ring + 1, Ground, Centre - Ring + 1));
        Vec3 High = Helper.absoluteVec(new Vec3(Centre + Ring, Ground + Top, Centre + Ring));
        return new AABB(Low, High);
    }

    private static boolean within(AABB Box, AABB Inside) {
        return Box.minX >= Inside.minX - Slack && Box.maxX <= Inside.maxX + Slack && Box.minY >= Inside.minY - Slack && Box.maxY <= Inside.maxY + Slack
                && Box.minZ >= Inside.minZ - Slack && Box.maxZ <= Inside.maxZ + Slack;
    }

    private static String where(GameTestHelper Helper, Scp173Statue Statue) {
        Vec3 At = Helper.relativeVec(Statue.position());
        return String.format(" at %.2f %.2f %.2f, tick %d, target %s, clinging %s", At.x, At.y, At.z, Helper.getTick(), Statue.hunt().target(), Statue.isClinging());
    }

    private static void solid(GameTestHelper Helper, Scp173Statue Statue, String Name) {
        AABB Box = Scp173Passage.box(Statue.position()).deflate(Slack);
        Helper.assertTrue(Helper.getLevel().noBlockCollision(Statue, Box), "SCP-173 overlaps a block of the " + Name + " cell" + where(Helper, Statue));
    }

    private static void guard(GameTestHelper Helper, List<Scp173Statue> Statues, AABB Inside, String Name) {
        Helper.onEachTick(() -> {
            for (Scp173Statue Statue : Statues) {
                if (Statue.isRemoved()) {
                    continue;
                }
                Helper.assertTrue(within(Statue.getBoundingBox(), Inside), "SCP-173 got out of the sealed " + Name + " cell" + where(Helper, Statue));
                solid(Helper, Statue, Name);
            }
        });
    }

    private static void tidy(List<? extends Entity> Bodies) {
        for (Entity Body : Bodies) {
            if (!Body.isRemoved()) {
                Body.discard();
            }
        }
    }

    private static void sealed(GameTestHelper Helper, String Name, Mason Walls, BlockState Roof, double Top, Vec3 Start, Vec3 Target) {
        quiet(Helper);
        build(Helper, Walls, Roof);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, Start.x, Start.z);
        DClass Victim = prey(Helper, Statue, Target.x, Target.z);
        AABB Inside = interior(Helper, Top);
        guard(Helper, List.of(Statue), Inside, Name);
        Helper.startSequence()
                .thenExecuteAfter(WatchTicks, () -> {
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed the D-Class outside the sealed " + Name + " cell" + where(Helper, Statue));
                    Helper.assertTrue(Statue.hunt().isPrey(World, Victim), "the D-Class outside the " + Name + " cell is not prey, so the test proves nothing");
                    tidy(List.of(Statue, Victim));
                })
                .thenSucceed();
    }

    private static void sealed(GameTestHelper Helper, String Name, Mason Walls) {
        sealed(Helper, Name, Walls, Blocks.STONE.defaultBlockState(), Rows, new Vec3(Centre + 0.5, 0.0, Centre + 0.5), new Vec3(OutsideX, 0.0, Centre + 0.5));
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_stone")
    public static void stoneCellHolds(GameTestHelper Helper) {
        sealed(Helper, "stone", (Outward, Row) -> Blocks.STONE.defaultBlockState());
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_panes")
    public static void glassPaneCellHolds(GameTestHelper Helper) {
        sealed(Helper, "glass pane", (Outward, Row) -> Blocks.GLASS_PANE.defaultBlockState());
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_bars")
    public static void ironBarCellHolds(GameTestHelper Helper) {
        sealed(Helper, "iron bar", (Outward, Row) -> Blocks.IRON_BARS.defaultBlockState());
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_slabs")
    public static void slabAndStairCellHolds(GameTestHelper Helper) {
        Mason Walls = (Outward, Row) -> switch (Row) {
            case 0 -> Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Outward);
            case 1 -> Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            default -> Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Outward.getOpposite()).setValue(StairBlock.HALF, Half.TOP);
        };
        BlockState Roof = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        sealed(Helper, "slab and stair", Walls, Roof, Rows + 1.0, new Vec3(Centre + 0.5, 0.0, Centre + 0.5), new Vec3(OutsideX, 0.0, Centre + 0.5));
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_storage")
    public static void storageContainerCellHolds(GameTestHelper Helper) {
        sealed(Helper, "storage container and chest", (Outward, Row) -> Outward.getAxis() == Direction.Axis.X
                ? StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Outward.getOpposite())
                : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Outward.getOpposite()));
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_lockers")
    public static void lockerCellHolds(GameTestHelper Helper) {
        sealed(Helper, "locker", (Outward, Row) -> switch (Row) {
            case 0 -> PropsRegistry.Locker.get().defaultBlockState().setValue(LockerBlock.Facing, Outward.getOpposite()).setValue(TallProps.Half, DoubleBlockHalf.LOWER);
            case 1 -> PropsRegistry.Locker.get().defaultBlockState().setValue(LockerBlock.Facing, Outward.getOpposite()).setValue(TallProps.Half, DoubleBlockHalf.UPPER);
            default -> Blocks.STONE.defaultBlockState();
        });
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_shutters")
    public static void shutterAndPanelCellHolds(GameTestHelper Helper) {
        sealed(Helper, "lockdown shutter and padded panel", (Outward, Row) -> Outward.getAxis() == Direction.Axis.X
                ? ContainmentZoneRegistry.LockdownShutter.get().defaultBlockState().setValue(CellStackBlock.FACING, Outward)
                : ContainmentZoneRegistry.PaddedPanel.get().defaultBlockState().setValue(CellStackBlock.FACING, Outward));
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_facility")
    public static void facilityWallCellHolds(GameTestHelper Helper) {
        sealed(Helper, "facility and containment wall", (Outward, Row) -> switch (Outward) {
            case EAST -> DecorRegistry.zoneBlock(FacilityZone.Containment, Row == 0 ? ZonePart.WallBottom : Row == 1 ? ZonePart.WallMiddle : ZonePart.WallTop).get().defaultBlockState();
            case WEST -> DecorRegistry.zoneBlock(FacilityZone.Hcz, Row == 1 ? ZonePart.WallWindow : ZonePart.WallMiddle).get().defaultBlockState();
            case NORTH -> (Row == 2 ? ContainmentRegistry.HazardWall : ContainmentRegistry.ReinforcedWall).get().defaultBlockState();
            default -> Row == 1 ? ContainmentRegistry.ReinforcedGlass.get().defaultBlockState() : ContainmentRegistry.ReinforcedPane.get().defaultBlockState();
        });
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_trapdoors")
    public static void trapdoorCellHolds(GameTestHelper Helper) {
        Mason Walls = (Outward, Row) -> (Row == 1 ? Blocks.IRON_TRAPDOOR : Blocks.OAK_TRAPDOOR).defaultBlockState()
                .setValue(TrapDoorBlock.FACING, Outward).setValue(TrapDoorBlock.OPEN, true);
        BlockState Roof = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
        sealed(Helper, "trapdoor", Walls, Roof, Rows + 1.0, new Vec3(Centre + 0.5, 0.0, Centre + 0.5), new Vec3(OutsideX, 0.0, Centre + 0.5));
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_irondoor")
    public static void ironDoorCellHolds(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        List<BlockPos> Placed = new ArrayList<>();
        BlockState Door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, false);
        put(Helper, new BlockPos(Centre + Ring, Ground, Centre), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Placed);
        put(Helper, new BlockPos(Centre + Ring, Ground + 1, Centre), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Placed);
        settle(Helper, Placed);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, Centre + 0.5, Centre + 0.5);
        DClass Victim = prey(Helper, Statue, Centre + Ring + 0.5, Centre + 0.5);
        AABB Inside = interior(Helper, Rows);
        guard(Helper, List.of(Statue), Inside, "iron door");
        Helper.startSequence()
                .thenExecuteAfter(WatchTicks, () -> {
                    Helper.assertTrue(World.getBlockState(Helper.absolutePos(new BlockPos(Centre + Ring, Ground, Centre))).is(Blocks.IRON_DOOR), "the iron door is gone");
                    Helper.assertTrue(!World.getBlockState(Helper.absolutePos(new BlockPos(Centre + Ring, Ground, Centre))).getValue(DoorBlock.OPEN), "SCP-173 opened the iron door");
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed the D-Class through the closed iron door" + where(Helper, Statue));
                    Helper.assertTrue(Statue.hunt().isPrey(World, Victim), "the D-Class behind the iron door is not prey, so the test proves nothing");
                    tidy(List.of(Statue, Victim));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_containmentdoor")
    public static void containmentDoorCellHolds(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        PanelDoorBlock Door = DoorRegistry.ContainmentDoor.get();
        BlockPos Origin = new BlockPos(Centre + Ring, Ground, Centre + 1);
        List<BlockPos> Placed = new ArrayList<>();
        for (int Part = 0; Part < Door.getKind().getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Origin, Direction.EAST, Door.getKind().columnOf(Part), Door.getKind().rowOf(Part));
            put(Helper, Cell, Door.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.EAST).setValue(PanelDoorBlock.Open, false).setValue(Door.partProperty(), Part), Placed);
        }
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, Centre + 0.5, Centre + 0.5);
        DClass Victim = prey(Helper, Statue, OutsideX, Centre + 0.5);
        AABB Inside = interior(Helper, Rows);
        guard(Helper, List.of(Statue), Inside, "containment door");
        Helper.startSequence()
                .thenExecuteAfter(WatchTicks, () -> {
                    BlockState Now = World.getBlockState(Helper.absolutePos(Origin));
                    Helper.assertTrue(Now.is(Door) && !Now.getValue(PanelDoorBlock.Open), "the containment door did not stay closed");
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed the D-Class outside the closed containment door" + where(Helper, Statue));
                    Helper.assertTrue(Statue.hunt().isPrey(World, Victim), "the D-Class outside the containment door is not prey, so the test proves nothing");
                    tidy(List.of(Statue, Victim));
                })
                .thenSucceed();
    }

    private static BlockPos singleDoor(GameTestHelper Helper) {
        PanelDoorBlock Door = DoorRegistry.LczSingleDoor.get();
        BlockPos Origin = new BlockPos(Centre + Ring, Ground, Centre);
        List<BlockPos> Placed = new ArrayList<>();
        for (int Part = 0; Part < Door.getKind().getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Origin, Direction.EAST, Door.getKind().columnOf(Part), Door.getKind().rowOf(Part));
            put(Helper, Cell, Door.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.EAST).setValue(PanelDoorBlock.Open, false).setValue(Door.partProperty(), Part), Placed);
        }
        BlockPos Absolute = Helper.absolutePos(Origin);
        Helper.assertTrue(Door.holdOpen(Helper.getLevel(), Absolute, null), "the single door could not be held open");
        return Absolute;
    }

    private static boolean wideOpen(GameTestHelper Helper, BlockPos Origin) {
        BlockState State = Helper.getLevel().getBlockState(Origin);
        return State.getBlock() instanceof PanelDoorBlock Door && State.getValue(PanelDoorBlock.Open) && Door.masterEntity(Helper.getLevel(), Origin, State) instanceof PanelDoorBlockEntity Master
                && Master.getEased() >= 1.0f;
    }

    @GameTest(template = Arena, timeoutTicks = OpenTimeout, batch = "s173cell_doorway")
    public static void openDoorIsStillUsed(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        BlockPos Origin = singleDoor(Helper);
        AABB Inner = interior(Helper, Rows);
        AABB Doorway = new AABB(Helper.absoluteVec(new Vec3(Centre + Ring, Ground, Centre)), Helper.absoluteVec(new Vec3(Centre + Ring + 1, Ground + 2, Centre + 1)));
        Vec3 OuterLow = Helper.absoluteVec(new Vec3(Centre - Ring, Ground, Centre - Ring));
        Vec3 OuterHigh = Helper.absoluteVec(new Vec3(Centre + Ring + 1, Ground + Rows + 1, Centre + Ring + 1));
        AABB Outer = new AABB(OuterLow, OuterHigh);
        List<Entity> Bodies = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(wideOpen(Helper, Origin), "the single door never finished opening"))
                .thenExecute(() -> {
                    Scp173Statue Statue = statue(Helper, Centre - 0.5, Centre + 0.5);
                    DClass Victim = prey(Helper, Statue, OutsideX, Centre + 0.5);
                    Bodies.add(Statue);
                    Bodies.add(Victim);
                    Helper.onEachTick(() -> {
                        if (Statue.isRemoved()) {
                            return;
                        }
                        AABB Box = Statue.getBoundingBox().deflate(Slack);
                        boolean OverRing = Box.maxX > Outer.minX && Box.minX < Outer.maxX && Box.maxZ > Outer.minZ && Box.minZ < Outer.maxZ
                                && !(Box.minX >= Inner.minX && Box.maxX <= Inner.maxX && Box.minZ >= Inner.minZ && Box.maxZ <= Inner.maxZ);
                        boolean InDoorway = Box.minX >= Inner.minX - Slack && Box.minZ >= Doorway.minZ - Slack && Box.maxZ <= Doorway.maxZ + Slack && Box.maxY <= Doorway.maxY + Slack;
                        Helper.assertTrue(!OverRing || InDoorway, "SCP-173 crossed the wall beside the open door" + where(Helper, Statue));
                    });
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Bodies.get(1).isAlive(), "SCP-173 never went through the open door" + where(Helper, (Scp173Statue) Bodies.get(0))))
                .thenExecute(() -> tidy(Bodies))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_closing")
    public static void doorClosingOnTheWayOutHolds(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        BlockPos Origin = singleDoor(Helper);
        ServerLevel World = Helper.getLevel();
        AABB Inside = interior(Helper, Rows);
        List<Scp173Statue> Statues = new ArrayList<>();
        List<Entity> Bodies = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(wideOpen(Helper, Origin), "the single door never finished opening"))
                .thenExecute(() -> {
                    Scp173Statue Statue = statue(Helper, Centre - 0.5, Centre + 0.5);
                    Statues.add(Statue);
                    Bodies.add(Statue);
                    Bodies.add(prey(Helper, Statue, OutsideX, Centre + 0.5));
                    guard(Helper, Statues, Inside, "closing door");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Statues.get(0).hunt().course() != null && !Statues.get(0).hunt().course().isDone(), "SCP-173 never set off for the open door"))
                .thenExecute(() -> {
                    PanelDoorBlock Door = DoorRegistry.LczSingleDoor.get();
                    Door.releaseHold(World, Origin);
                    Door.setOpen(World, Origin, Direction.EAST, false);
                    Helper.assertTrue(World.getBlockEntity(Origin) instanceof PanelDoorBlockEntity, "the single door lost its block entity");
                    PanelDoorBlockEntity Master = (PanelDoorBlockEntity) World.getBlockEntity(Origin);
                    CompoundTag Saved = Master.saveWithoutMetadata(World.registryAccess());
                    Saved.putFloat("Progress", 0.0f);
                    Master.loadWithComponents(Saved, World.registryAccess());
                    Helper.assertTrue(Master.getEased() <= 0.0f, "the single door did not slam shut");
                })
                .thenExecuteAfter(WatchTicks, () -> {
                    Helper.assertTrue(Bodies.get(1).isAlive(), "SCP-173 killed the D-Class after the door shut" + where(Helper, Statues.get(0)));
                    tidy(Bodies);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = SealedTimeout, batch = "s173cell_seam")
    public static void cornerSeamHolds(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        ServerLevel World = Helper.getLevel();
        BlockPos Corner = new BlockPos(Centre + Ring, Ground, Centre + Ring);
        for (int Row = 0; Row < Rows; Row++) {
            World.setBlock(Helper.absolutePos(Corner.above(Row)), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        Scp173Statue Statue = statue(Helper, Centre + Ring - 0.5, Centre + Ring - 0.5);
        RandomSource Dice = RandomSource.create(SeepSeed);
        for (int Try = 0; Try < SeepTries; Try++) {
            Scp173Residue.placeNear(World, Statue.blockPosition(), Statue.position(), Dice);
        }
        DClass Victim = prey(Helper, Statue, Centre + Ring + 0.5, Centre + Ring + 0.5);
        AABB Inside = interior(Helper, Rows);
        guard(Helper, List.of(Statue), Inside, "corner seam");
        Helper.startSequence()
                .thenExecuteAfter(WatchTicks, () -> {
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed the D-Class through the corner seam" + where(Helper, Statue));
                    Helper.assertTrue(!Scp173Residue.isResidue(World.getBlockState(Helper.absolutePos(Corner))), "SCP-173 left residue outside the cell through the corner seam");
                    Helper.assertTrue(Statue.hunt().isPrey(World, Victim), "the D-Class at the seam is not prey, so the test proves nothing");
                    tidy(List.of(Statue, Victim));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = OpenTimeout, batch = "s173cell_open")
    public static void openCellIsClimbedNotCrossed(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), null);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, Centre + 0.5, Centre + 0.5);
        DClass Victim = prey(Helper, Statue, OutsideX + 1.0, Centre + 0.5);
        AABB Inner = interior(Helper, Rows);
        Vec3 OuterLow = Helper.absoluteVec(new Vec3(Centre - Ring, Ground, Centre - Ring));
        Vec3 OuterHigh = Helper.absoluteVec(new Vec3(Centre + Ring + 1, Ground + Rows, Centre + Ring + 1));
        AABB Outer = new AABB(OuterLow, OuterHigh);
        double WallTop = Outer.maxY;
        Helper.onEachTick(() -> {
            if (Statue.isRemoved()) {
                return;
            }
            AABB Box = Statue.getBoundingBox().deflate(Slack);
            solid(Helper, Statue, "open");
            boolean OverRing = Box.maxX > Outer.minX && Box.minX < Outer.maxX && Box.maxZ > Outer.minZ && Box.minZ < Outer.maxZ
                    && !(Box.minX >= Inner.minX && Box.maxX <= Inner.maxX && Box.minZ >= Inner.minZ && Box.maxZ <= Inner.maxZ);
            Helper.assertTrue(!OverRing || Box.minY >= WallTop - Slack, "SCP-173 crossed the wall line below the wall top" + where(Helper, Statue));
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 never climbed out of the open topped cell" + where(Helper, Statue)))
                .thenExecute(() -> {
                    Helper.assertTrue(!within(Statue.getBoundingBox(), Inner), "SCP-173 killed the D-Class without leaving the cell" + where(Helper, Statue));
                    tidy(List.of(Statue, Victim));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = CloneTimeout, batch = "s173cell_clones")
    public static void clonesStayInTheCell(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, Centre + 0.5, Centre + 0.5);
        DClass Victim = prey(Helper, Statue, OutsideX, Centre + 0.5);
        int[][] Outside = {{Centre + Ring + 2, Centre}, {Centre - Ring - 2, Centre + 2}, {Centre, Centre + Ring + 3}, {Centre + Ring + 1, Centre + Ring + 1}, {Centre - Ring - 1, Centre - Ring - 1}};
        for (int[] Spot : Outside) {
            Helper.assertTrue(Scp173Residue.place(World, Helper.absolutePos(new BlockPos(Spot[0], Ground, Spot[1]))), "residue could not be placed outside the cell");
        }
        AABB Inside = interior(Helper, Rows);
        List<Scp173Statue> Statues = new ArrayList<>();
        Statues.add(Statue);
        guard(Helper, Statues, Inside, "clone");
        List<Entity> Bodies = new ArrayList<>();
        Bodies.add(Victim);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    Scp173Statue Outsider = Scp173Clones.spawnFrom(World, Statue, false);
                    if (Outsider != null) {
                        Statues.add(Outsider);
                    }
                    Helper.assertTrue(Outsider == null, "a clone grew from residue outside the sealed cell" + (Outsider == null ? "" : where(Helper, Outsider)));
                    BlockPos Inner = Helper.absolutePos(new BlockPos(Centre - 1, Ground, Centre - 1));
                    Helper.assertTrue(Scp173Residue.place(World, Inner), "residue could not be placed inside the cell");
                    Scp173Statue Insider = Scp173Clones.spawnFrom(World, Statue, false);
                    Helper.assertTrue(Insider != null, "no clone grew from the residue inside the cell");
                    Statues.add(Insider);
                    Helper.assertTrue(Insider.blockPosition().equals(Inner), "the clone did not grow on the residue inside the cell" + where(Helper, Insider));
                    for (int Try = 0; Try < 3; Try++) {
                        Scp173Statue Forced = Scp173Clones.spawnFrom(World, Statue, true);
                        if (Forced != null) {
                            Statues.add(Forced);
                            Helper.assertTrue(within(Forced.getBoundingBox(), Inside), "a forced clone appeared outside the sealed cell" + where(Helper, Forced));
                        }
                    }
                })
                .thenExecuteAfter(CloneWatch, () -> {
                    Helper.assertTrue(Victim.isAlive(), "a statue killed the D-Class outside the sealed cell");
                    Bodies.addAll(Statues);
                    tidy(Bodies);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = CloneTimeout, batch = "s173cell_clone_rule")
    public static void cloningRuleStopsCopies(GameTestHelper Helper) {
        quiet(Helper);
        build(Helper, (Outward, Row) -> Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Rule = World.getGameRules().getRule(Scp173Registry.CloningRule);
        boolean Before = Rule.get();
        Scp173Statue Statue = statue(Helper, Centre + 0.5, Centre + 0.5);
        AABB Inside = interior(Helper, Rows);
        List<Scp173Statue> Statues = new ArrayList<>();
        Statues.add(Statue);
        Helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    try {
                        BlockPos Inner = Helper.absolutePos(new BlockPos(Centre - 1, Ground, Centre - 1));
                        Helper.assertTrue(Scp173Residue.place(World, Inner), "residue could not be placed inside the cell");
                        Rule.set(false, World.getServer());
                        Helper.assertFalse(Scp173Registry.cloning(World.getGameRules()), "the cloning rule did not turn off");
                        Scp173Statue Blocked = Scp173Clones.spawnFrom(World, Statue, false);
                        if (Blocked != null) {
                            Statues.add(Blocked);
                        }
                        Helper.assertTrue(Blocked == null, "a clone grew from residue with the cloning rule off" + (Blocked == null ? "" : where(Helper, Blocked)));
                        Helper.assertTrue(Scp173Residue.isResidue(World.getBlockState(Inner)), "the residue was used up with the cloning rule off");
                        Scp173Statue Forced = Scp173Clones.spawnFrom(World, Statue, true);
                        Helper.assertTrue(Forced != null, "the operator clone command stopped working with the cloning rule off");
                        Statues.add(Forced);
                        Helper.assertTrue(within(Forced.getBoundingBox(), Inside), "a forced clone appeared outside the sealed cell" + where(Helper, Forced));
                        Rule.set(true, World.getServer());
                        BlockPos Second = Helper.absolutePos(new BlockPos(Centre + 1, Ground, Centre + 1));
                        Helper.assertTrue(Scp173Residue.place(World, Second), "residue could not be placed back inside the cell");
                        Scp173Statue Grown = Scp173Clones.spawnFrom(World, Statue, false);
                        if (Grown != null) {
                            Statues.add(Grown);
                        }
                        Helper.assertTrue(Grown != null, "no clone grew from residue with the cloning rule back on");
                    } finally {
                        Rule.set(Before, World.getServer());
                    }
                })
                .thenExecute(() -> tidy(Statues))
                .thenSucceed();
    }
}
