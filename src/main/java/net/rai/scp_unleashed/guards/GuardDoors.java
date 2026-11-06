package net.rai.scp_unleashed.guards;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.npc.doors.NpcDoors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GuardDoors {
    public static final int ScanRadius = 3;
    public static final double UseReach = 2.6;
    public static final int TaskTicks = 200;
    public static final int ReuseTicks = 100;
    public static final int WallTicks = 300;
    public static final double HurrySpeed = 1.0;
    public static final int HurryTicks = 60;
    public static final int StandDepth = 5;
    public static final int SearchSpan = 20;
    public static final int PassageLook = 4;
    public static final double PassageReach = 3.0;
    public static final double PassageApproach = 1.3;
    public static final double PassageExit = 1.6;
    public static final int PassageTicks = 80;
    public static final double WaitBack = 1.5;

    private final DClass Owner;
    private final Map<BlockPos, Long> Used = new HashMap<>();
    private final Map<BlockPos, Long> Walls = new HashMap<>();
    @Nullable
    private Task Current;
    @Nullable
    private Vec3[] Passage;
    private int PassageStep;
    private long PassageStarted;
    private long HurryUntil = Long.MIN_VALUE / 2;
    private boolean Blocked;
    @Nullable
    private Vec3 Waiting;
    private Vec3 BlockedDoor = Vec3.ZERO;

    public GuardDoors(DClass Owner) {
        this.Owner = Owner;
    }

    private enum Kind {
        Opener, Scanner, Plain
    }

    private record Task(BlockPos Door, BlockPos Device, Kind Type, ItemStack Card, long Started) {
    }

    public boolean busy() {
        return Current != null;
    }

    public boolean blocked() {
        return Blocked;
    }

    public void clearBlocked() {
        Blocked = false;
        Waiting = null;
    }

    @Nullable
    public Vec3 waiting() {
        return Waiting;
    }

    public Vec3 blockedDoor() {
        return BlockedDoor;
    }

    public void cancel() {
        Current = null;
        Passage = null;
    }

    public double pace(double Normal) {
        return Owner.level().getGameTime() < HurryUntil ? Math.max(Normal, HurrySpeed) : Normal;
    }

    public static boolean obstructed(@Nullable Path Route) {
        return Route == null || !Route.canReach();
    }

    private static boolean plainClosed(BlockState State) {
        if (State.getBlock() instanceof DoorBlock Door) {
            return Door.type().canOpenByHand() && !Door.isOpen(State);
        }
        return State.getBlock() instanceof FenceGateBlock && !State.getValue(FenceGateBlock.OPEN);
    }

    private static boolean closedDoor(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (plainClosed(State)) {
            return true;
        }
        boolean Panel = State.getBlock() instanceof PanelDoorBlock || State.getBlock() instanceof DoorBlock;
        return Panel && NpcDoors.blocks(World, Pos);
    }

    private static boolean isOpener(BlockState State) {
        Block Found = State.getBlock();
        boolean Device = Found instanceof ButtonBlock || Found instanceof DoorButtonBlock || Found instanceof LeverBlock || Found instanceof DoorLeverBlock;
        return Device && !(State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED));
    }

    private static boolean isScanner(BlockState State) {
        return State.getBlock() instanceof KeycardScannerBlock;
    }

    public boolean plan(Vec3 Target) {
        Level World = Owner.level();
        long Now = World.getGameTime();
        prune(Now);
        if (Current != null) {
            return true;
        }
        Path Route = Owner.getNavigation().getPath();
        Node End = Route == null ? null : Route.getEndNode();
        BlockPos Door = nearestDoor(World, End == null ? Owner.blockPosition() : End.asBlockPos(), Target);
        if (Door == null) {
            return false;
        }
        BlockPos Key = anchor(World, Door);
        Long Wall = Walls.get(Key);
        if (Wall != null && Wall > Now) {
            block(World, Door);
            return false;
        }
        Long Last = Used.get(Key);
        if (Last != null && Now - Last < ReuseTicks) {
            return false;
        }
        if (plainClosed(World.getBlockState(Door))) {
            start(Key, Door, Kind.Plain, ItemStack.EMPTY, Now);
            return true;
        }
        BlockPos Opener = reachable(World, openers(World, Door));
        if (Opener != null) {
            start(Key, Opener, Kind.Opener, ItemStack.EMPTY, Now);
            return true;
        }
        BlockPos Scanner = reachable(World, scanners(World, Door));
        if (Scanner != null) {
            Optional<ItemStack> Card = NpcDoors.pickCard(World, Scanner, carried());
            if (Card.isPresent()) {
                start(Key, Scanner, Kind.Scanner, Card.get(), Now);
                return true;
            }
        }
        Walls.put(Key, Now + WallTicks);
        block(World, Door);
        return false;
    }

    private void block(Level World, BlockPos Door) {
        Blocked = true;
        List<BlockPos> Cells = NpcDoors.extent(World, Door);
        double SumX = 0.0;
        double SumZ = 0.0;
        int Bottom = Integer.MAX_VALUE;
        for (BlockPos Cell : Cells) {
            Bottom = Math.min(Bottom, Cell.getY());
        }
        int Count = 0;
        for (BlockPos Cell : Cells) {
            if (Cell.getY() == Bottom) {
                SumX += Cell.getX() + 0.5;
                SumZ += Cell.getZ() + 0.5;
                Count++;
            }
        }
        Vec3 Middle = Count == 0 ? Vec3.atBottomCenterOf(Door) : new Vec3(SumX / Count, Bottom, SumZ / Count);
        BlockedDoor = Middle;
        Vec3 Normal = facingOf(World.getBlockState(Door));
        double Side = Owner.position().subtract(Middle).dot(Normal) >= 0.0 ? 1.0 : -1.0;
        Waiting = Middle.add(Normal.scale(Side * WaitBack));
    }

    private static Vec3 facingOf(BlockState State) {
        Direction Front = Direction.NORTH;
        if (State.getBlock() instanceof PanelDoorBlock) {
            Front = State.getValue(PanelDoorBlock.Facing);
        } else if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Front = State.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
    }

    private void start(BlockPos Key, BlockPos Device, Kind Type, ItemStack Card, long Now) {
        Current = new Task(Key, Device, Type, Card, Now);
        clearBlocked();
    }

    public boolean tick() {
        Task Job = Current;
        if (Job == null) {
            return false;
        }
        Level World = Owner.level();
        long Now = World.getGameTime();
        if (Now - Job.Started() > TaskTicks) {
            Walls.put(Job.Door(), Now + WallTicks);
            Current = null;
            block(World, Job.Door());
            return false;
        }
        if (Job.Type() != Kind.Plain && !NpcDoors.blocks(World, Job.Door()) && !anyClosed(World, Job.Door())) {
            Current = null;
            HurryUntil = Now + HurryTicks;
            return false;
        }
        Vec3 Centre = Vec3.atCenterOf(Job.Device());
        Owner.getLookControl().setLookAt(Centre.x, Centre.y, Centre.z, 30.0f, Owner.getMaxHeadXRot());
        if (Owner.getEyePosition().distanceToSqr(Centre) > UseReach * UseReach) {
            if (Owner.getNavigation().isDone() || Now % 20 == 0) {
                BlockPos Stand = standing(World, Job.Device());
                Owner.getNavigation().moveTo(Stand.getX() + 0.5, Stand.getY(), Stand.getZ() + 0.5, HurrySpeed);
            }
            return true;
        }
        Owner.getNavigation().stop();
        Owner.swing(InteractionHand.MAIN_HAND);
        boolean Worked = switch (Job.Type()) {
            case Opener -> NpcDoors.useOpener(Owner, Job.Device());
            case Scanner -> NpcDoors.swipe(Owner, Job.Card(), Job.Device());
            case Plain -> openPlain(World, Job.Device());
        };
        Used.put(Job.Door(), Now);
        Current = null;
        HurryUntil = Now + HurryTicks;
        if (!Worked && Job.Type() == Kind.Scanner) {
            Walls.put(Job.Door(), Now + WallTicks);
            block(World, Job.Door());
        }
        return false;
    }

    private boolean anyClosed(Level World, BlockPos Key) {
        for (BlockPos Cell : NpcDoors.extent(World, Key)) {
            if (NpcDoors.blocks(World, Cell)) {
                return true;
            }
        }
        return false;
    }

    private boolean openPlain(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof DoorBlock Door && Door.type().canOpenByHand()) {
            Door.setOpen(Owner, World, State, Pos, true);
            return true;
        }
        if (State.getBlock() instanceof FenceGateBlock && !State.getValue(FenceGateBlock.OPEN)) {
            World.setBlock(Pos, State.setValue(FenceGateBlock.OPEN, true), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            World.playSound(null, Pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, World.getRandom().nextFloat() * 0.1f + 0.9f);
            World.gameEvent(Owner, GameEvent.BLOCK_OPEN, Pos);
            return true;
        }
        return false;
    }

    private void prune(long Now) {
        Walls.values().removeIf(Until -> Until <= Now);
        Iterator<Map.Entry<BlockPos, Long>> Entries = Used.entrySet().iterator();
        while (Entries.hasNext()) {
            if (Now - Entries.next().getValue() > WallTicks) {
                Entries.remove();
            }
        }
    }

    private BlockPos anchor(Level World, BlockPos Door) {
        BlockPos Best = Door;
        for (BlockPos Cell : NpcDoors.extent(World, Door)) {
            if (Cell.asLong() < Best.asLong()) {
                Best = Cell;
            }
        }
        return Best.immutable();
    }

    @Nullable
    private BlockPos nearestDoor(Level World, BlockPos End, Vec3 Target) {
        BlockPos Best = null;
        double BestScore = Double.MAX_VALUE;
        Vec3 Here = Owner.position();
        double Direct = Here.distanceTo(Target);
        BlockPos From = Owner.blockPosition();
        BlockPos To = BlockPos.containing(Target);
        int MinX = Math.max(Math.min(From.getX(), To.getX()) - ScanRadius, From.getX() - SearchSpan);
        int MaxX = Math.min(Math.max(From.getX(), To.getX()) + ScanRadius, From.getX() + SearchSpan);
        int MinZ = Math.max(Math.min(From.getZ(), To.getZ()) - ScanRadius, From.getZ() - SearchSpan);
        int MaxZ = Math.min(Math.max(From.getZ(), To.getZ()) + ScanRadius, From.getZ() + SearchSpan);
        int MinY = Math.min(From.getY(), Math.max(To.getY(), From.getY() - 4)) - 1;
        int MaxY = Math.max(From.getY(), Math.min(To.getY(), From.getY() + 4)) + 2;
        for (BlockPos Spot : BlockPos.betweenClosed(MinX, MinY, MinZ, MaxX, MaxY, MaxZ)) {
            if (!closedDoor(World, Spot)) {
                continue;
            }
            Vec3 Middle = Vec3.atCenterOf(Spot);
            double Ahead = Middle.distanceTo(Target);
            if (Ahead > Direct + 1.5) {
                continue;
            }
            double Score = Middle.distanceTo(Here) + Ahead + Middle.distanceTo(Vec3.atCenterOf(End)) * 0.25;
            if (Score < BestScore) {
                BestScore = Score;
                Best = Spot.immutable();
            }
        }
        return Best;
    }

    public boolean hurrying() {
        return Owner.level().getGameTime() < HurryUntil;
    }

    public boolean transit() {
        Level World = Owner.level();
        if (Passage == null && !findPassage(World)) {
            return false;
        }
        long Now = World.getGameTime();
        if (Now - PassageStarted > PassageTicks) {
            Passage = null;
            return false;
        }
        Vec3 Goal = Passage[PassageStep];
        double Flat = Math.pow(Owner.getX() - Goal.x, 2) + Math.pow(Owner.getZ() - Goal.z, 2);
        double Close = PassageStep == 0 ? 0.3 : 0.45;
        if (Flat <= Close * Close) {
            PassageStep++;
            if (PassageStep >= Passage.length) {
                Passage = null;
                HurryUntil = Now + HurryTicks;
                return false;
            }
            Goal = Passage[PassageStep];
        }
        Owner.getNavigation().stop();
        Owner.getMoveControl().setWantedPosition(Goal.x, Goal.y, Goal.z, HurrySpeed);
        return true;
    }

    private boolean findPassage(Level World) {
        Path Route = Owner.getNavigation().getPath();
        if (Route == null || Route.isDone()) {
            return false;
        }
        int Last = Math.min(Route.getNodeCount(), Route.getNextNodeIndex() + PassageLook);
        for (int Index = Math.max(0, Route.getNextNodeIndex() - 1); Index < Last; Index++) {
            BlockPos Pos = Route.getNodePos(Index);
            BlockState State = World.getBlockState(Pos);
            if (!(State.getBlock() instanceof PanelDoorBlock) || !State.getValue(PanelDoorBlock.Open)) {
                continue;
            }
            if (Owner.distanceToSqr(Vec3.atBottomCenterOf(Pos)) > PassageReach * PassageReach) {
                return false;
            }
            List<BlockPos> Cells = NpcDoors.extent(World, Pos);
            int Bottom = Integer.MAX_VALUE;
            for (BlockPos Cell : Cells) {
                Bottom = Math.min(Bottom, Cell.getY());
            }
            double SumX = 0.0;
            double SumZ = 0.0;
            int Count = 0;
            for (BlockPos Cell : Cells) {
                if (Cell.getY() == Bottom) {
                    SumX += Cell.getX() + 0.5;
                    SumZ += Cell.getZ() + 0.5;
                    Count++;
                }
            }
            if (Count == 0) {
                return false;
            }
            Vec3 Middle = new Vec3(SumX / Count, Bottom, SumZ / Count);
            Direction Front = State.getValue(PanelDoorBlock.Facing);
            Vec3 Normal = new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
            double Side = Owner.position().subtract(Middle).dot(Normal) >= 0.0 ? 1.0 : -1.0;
            Passage = new Vec3[]{Middle.add(Normal.scale(Side * PassageApproach)), Middle.subtract(Normal.scale(Side * PassageExit))};
            PassageStep = 0;
            PassageStarted = World.getGameTime();
            return true;
        }
        return false;
    }

    private List<BlockPos> openers(Level World, BlockPos Door) {
        Set<BlockPos> Found = new LinkedHashSet<>();
        NpcDoors.findOpener(World, Door).filter(Spot -> isOpener(World.getBlockState(Spot))).ifPresent(Found::add);
        Found.addAll(nearby(World, Door, true));
        return new ArrayList<>(Found);
    }

    private List<BlockPos> scanners(Level World, BlockPos Door) {
        Set<BlockPos> Found = new LinkedHashSet<>();
        NpcDoors.findScanner(World, Door).ifPresent(Found::add);
        Found.addAll(nearby(World, Door, false));
        return new ArrayList<>(Found);
    }

    private List<BlockPos> nearby(Level World, BlockPos Door, boolean Openers) {
        List<BlockPos> Cells = NpcDoors.extent(World, Door);
        Set<BlockPos> Found = new LinkedHashSet<>();
        for (BlockPos Cell : Cells) {
            for (BlockPos Spot : BlockPos.betweenClosed(Cell.offset(-NpcDoors.Search, -NpcDoors.Search, -NpcDoors.Search), Cell.offset(NpcDoors.Search, NpcDoors.Search, NpcDoors.Search))) {
                BlockState State = World.getBlockState(Spot);
                if (Openers ? isOpener(State) : isScanner(State)) {
                    Found.add(Spot.immutable());
                }
            }
        }
        List<BlockPos> Sorted = new ArrayList<>(Found);
        Sorted.sort((First, Second) -> Double.compare(Owner.distanceToSqr(Vec3.atCenterOf(First)), Owner.distanceToSqr(Vec3.atCenterOf(Second))));
        return Sorted;
    }

    @Nullable
    private BlockPos reachable(Level World, List<BlockPos> Devices) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Device : Devices) {
            BlockPos Stand = standing(World, Device);
            Vec3 Centre = Vec3.atCenterOf(Device);
            if (Vec3.atBottomCenterOf(Stand).add(0.0, Owner.getEyeHeight(), 0.0).distanceToSqr(Centre) > (UseReach + 1.0) * (UseReach + 1.0)) {
                continue;
            }
            double Distance = Owner.distanceToSqr(Centre);
            if (Distance <= UseReach * UseReach) {
                return Device;
            }
            Path Route = Owner.getNavigation().createPath(Stand, 1);
            if (Route != null && Route.canReach() && Distance < BestDistance) {
                Best = Device;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    private static BlockPos standing(Level World, BlockPos Device) {
        BlockPos Spot = Device;
        for (int Step = 0; Step < StandDepth; Step++) {
            BlockPos Below = Spot.below();
            if (!World.getBlockState(Below).getCollisionShape(World, Below).isEmpty() && World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty()) {
                return Spot;
            }
            Spot = Below;
        }
        return Device;
    }

    private List<ItemStack> carried() {
        List<ItemStack> Pack = new ArrayList<>(Owner.main());
        Pack.add(Owner.getOffhandItem());
        return Pack;
    }
}
