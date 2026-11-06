package net.rai.scp_unleashed.npc.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcDoorNavigation extends GroundPathNavigation {
    public static final int LookAhead = 4;
    public static final double DoorReach = 1.9;
    public static final double PassReach = 2.4;
    public static final double UseReach = 2.8;
    public static final int ReachTicks = 60;
    public static final int OpenTicks = 100;
    public static final int RefuseTicks = 600;
    public static final double PassApproach = 1.1;
    public static final double PassExit = 1.5;
    public static final double PassLane = 0.35;
    public static final int PassTicks = 60;
    public static final double Beyond = 0.6;

    private enum Phase {
        Reach, Wait, Pass
    }

    private static final class Doorway {
        private final Path Route;
        private final BlockPos Anchor;
        private final List<BlockPos> Cells;
        private final Vec3 Middle;
        private final Vec3 Normal;
        private final double Side;
        private final int Index;
        private Phase Stage;
        @Nullable
        private NpcDoorAccess.Plan Plan;
        private long Since;
        private int Step;

        private Doorway(Path Route, List<BlockPos> Cells, Vec3 Middle, Vec3 Normal, double Side, int Index, Phase Stage, long Since) {
            this.Route = Route;
            this.Anchor = NpcDoorAccess.anchor(Cells);
            this.Cells = Cells;
            this.Middle = Middle;
            this.Normal = Normal;
            this.Side = Side;
            this.Index = Index;
            this.Stage = Stage;
            this.Since = Since;
        }
    }

    @Nullable
    private Doorway Work;

    public NpcDoorNavigation(Mob Walker, Level World) {
        super(Walker, World);
        setCanOpenDoors(true);
        setCanPassDoors(true);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Nodes();
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public boolean atDoor() {
        return Work != null && Work.Stage != Phase.Pass;
    }

    @Nullable
    public BlockPos doorAhead() {
        return Work == null ? null : Work.Anchor;
    }

    public void refuse(BlockPos Anchor) {
        if (nodeEvaluator instanceof Nodes Walker) {
            Walker.refuse(Anchor, level.getGameTime() + RefuseTicks);
        }
    }

    @Override
    public void stop() {
        super.stop();
        Work = null;
    }

    @Override
    public void tick() {
        if (Work != null && (path == null || Work.Route != path || path.isDone())) {
            Work = null;
        }
        if (Work == null && path != null && !path.isDone() && level instanceof ServerLevel) {
            Work = find();
        }
        if (Work != null && work()) {
            tick++;
            return;
        }
        super.tick();
    }

    @Nullable
    private Doorway find() {
        int First = path.getNextNodeIndex();
        int Last = Math.min(path.getNodeCount(), First + LookAhead);
        for (int Index = First; Index < Last; Index++) {
            Node Step = path.getNode(Index);
            for (int Lift = 0; Lift <= 1; Lift++) {
                BlockPos Pos = new BlockPos(Step.x, Step.y + Lift, Step.z);
                BlockState State = level.getBlockState(Pos);
                boolean Closed = NpcDoorAccess.isClosedGate(State);
                if (!Closed && !NpcDoorAccess.isOpenPanel(State)) {
                    continue;
                }
                List<BlockPos> Cells = NpcDoors.extent(level, Pos);
                if (Cells.isEmpty()) {
                    continue;
                }
                Vec3 Middle = NpcDoorAccess.middle(Cells);
                Vec3 Normal = NpcDoorAccess.normal(State);
                double Flat = Math.sqrt(Math.pow(mob.getX() - Middle.x, 2) + Math.pow(mob.getZ() - Middle.z, 2));
                if (Flat > (Closed ? DoorReach : PassReach)) {
                    return null;
                }
                double Offset = NpcDoorAccess.offset(mob.position(), Middle, Normal);
                if (Math.abs(Offset) < NpcDoorAccess.Plane * 0.6) {
                    return null;
                }
                return new Doorway(path, Cells, Middle, Normal, Math.signum(Offset), Index, Closed ? Phase.Reach : Phase.Pass, level.getGameTime());
            }
        }
        return null;
    }

    private boolean work() {
        Doorway Job = Work;
        if (Job == null) {
            return false;
        }
        long Now = level.getGameTime();
        if (Job.Stage == Phase.Reach && NpcDoorAccess.fullyOpen(level, Job.Cells)) {
            Job.Stage = Phase.Pass;
            Job.Since = Now;
        }
        return switch (Job.Stage) {
            case Reach -> reach(Job, Now);
            case Wait -> await(Job, Now);
            case Pass -> pass(Job, Now);
        };
    }

    private boolean reach(Doorway Job, long Now) {
        if (Job.Plan == null) {
            Job.Plan = NpcDoorAccess.plan(mob, level, Job.Cells.get(0), Job.Side);
            if (Job.Plan == null) {
                if (NpcDoorAccess.anyOpen(level, Job.Cells)) {
                    Job.Stage = Phase.Wait;
                    Job.Since = Now;
                    return true;
                }
                give(Job);
                return false;
            }
        }
        Vec3 Device = Vec3.atCenterOf(Job.Plan.Device());
        mob.getLookControl().setLookAt(Device.x, Device.y, Device.z, 30.0f, mob.getMaxHeadXRot());
        if (Job.Plan.Kind() != NpcDoorAccess.Way.Manual && mob.getEyePosition().distanceToSqr(Device) > UseReach * UseReach) {
            if (Now - Job.Since > ReachTicks) {
                give(Job);
                return false;
            }
            Vec3 Stand = Device.add(Job.Normal.scale(Job.Side * 0.6));
            mob.getMoveControl().setWantedPosition(Stand.x, mob.getY(), Stand.z, speedModifier);
            return true;
        }
        halt();
        boolean Worked = switch (Job.Plan.Kind()) {
            case Manual -> manual(Job);
            case Opener -> NpcDoors.useOpener(mob, Job.Plan.Device());
            case Scanner -> NpcDoors.swipe(mob, Job.Plan.Card(), Job.Plan.Device());
        };
        if (!Worked && !NpcDoorAccess.anyOpen(level, Job.Cells) && !pressed(Job)) {
            give(Job);
            return false;
        }
        Job.Stage = Phase.Wait;
        Job.Since = Now;
        return true;
    }

    private boolean manual(Doorway Job) {
        BlockState State = level.getBlockState(Job.Cells.get(0));
        if (!(State.getBlock() instanceof PanelDoorBlock Frame)) {
            return false;
        }
        PanelDoorBlockEntity Door = Frame.masterEntity(level, Job.Cells.get(0), State);
        if (Door == null || !Door.toggle()) {
            return false;
        }
        mob.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    private boolean pressed(Doorway Job) {
        if (Job.Plan == null) {
            return false;
        }
        BlockState State = level.getBlockState(Job.Plan.Device());
        return State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED);
    }

    private boolean await(Doorway Job, long Now) {
        halt();
        mob.getLookControl().setLookAt(Job.Middle.x, Job.Middle.y + 1.0, Job.Middle.z, 20.0f, mob.getMaxHeadXRot());
        if (NpcDoorAccess.fullyOpen(level, Job.Cells)) {
            Job.Stage = Phase.Pass;
            Job.Since = Now;
            return true;
        }
        if (Now - Job.Since > OpenTicks) {
            give(Job);
            return false;
        }
        return true;
    }

    private boolean pass(Doorway Job, long Now) {
        if (!NpcDoorAccess.anyOpen(level, Job.Cells)) {
            Job.Stage = Phase.Reach;
            Job.Plan = null;
            Job.Since = Now;
            return reach(Job, Now);
        }
        Vec3 Approach = Job.Middle.add(Job.Normal.scale(Job.Side * PassApproach));
        Vec3 Exit = Job.Middle.subtract(Job.Normal.scale(Job.Side * PassExit));
        if (Job.Step == 0) {
            Vec3 Along = mob.position().subtract(Job.Middle);
            double Lateral = Math.abs(Along.x * Job.Normal.z - Along.z * Job.Normal.x);
            double Depth = NpcDoorAccess.offset(mob.position(), Job.Middle, Job.Normal) * Job.Side;
            double Flat = Math.pow(mob.getX() - Approach.x, 2) + Math.pow(mob.getZ() - Approach.z, 2);
            if (Lateral < PassLane || Depth < PassApproach * 0.5 || Flat < 0.3 * 0.3) {
                Job.Step = 1;
            }
        }
        Vec3 Goal = Job.Step == 0 ? Approach : Exit;
        if (Job.Step == 1) {
            double Past = -NpcDoorAccess.offset(mob.position(), Job.Middle, Job.Normal) * Job.Side;
            if (Past >= PassExit - 0.45 || Now - Job.Since > PassTicks) {
                skip(Job);
                Work = null;
                return false;
            }
        } else if (Now - Job.Since > PassTicks) {
            skip(Job);
            Work = null;
            return false;
        }
        mob.getMoveControl().setWantedPosition(Goal.x, Job.Middle.y, Goal.z, Math.max(speedModifier, 0.6));
        return true;
    }

    private void skip(Doorway Job) {
        if (path != Job.Route || path.isDone()) {
            return;
        }
        for (int Index = Job.Index; Index < path.getNodeCount(); Index++) {
            Vec3 Spot = Vec3.atBottomCenterOf(path.getNodePos(Index));
            if (-NpcDoorAccess.offset(Spot, Job.Middle, Job.Normal) * Job.Side >= Beyond) {
                path.setNextNodeIndex(Math.max(path.getNextNodeIndex(), Index));
                return;
            }
        }
        path.setNextNodeIndex(path.getNodeCount());
    }

    private void give(Doorway Job) {
        refuse(Job.Anchor);
        Work = null;
        BlockPos Goal = getTargetPos();
        double Speed = speedModifier;
        path = null;
        if (Goal != null) {
            moveTo(createPath(Goal, 1), Speed);
        }
    }

    private void halt() {
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), speedModifier);
        mob.setZza(0.0f);
        mob.setXxa(0.0f);
    }

    public static class Nodes extends WalkNodeEvaluator {
        private final Map<BlockPos, Long> Refused = new HashMap<>();
        private final Map<Long, Boolean> Verdicts = new HashMap<>();
        @Nullable
        private Node From;

        public void refuse(BlockPos Anchor, long Until) {
            Refused.put(Anchor.immutable(), Until);
        }

        @Override
        public void prepare(PathNavigationRegion Region, Mob Walker) {
            super.prepare(Region, Walker);
            Verdicts.clear();
            long Now = Walker.level().getGameTime();
            Refused.values().removeIf(Until -> Until <= Now);
        }

        @Override
        public void done() {
            super.done();
            Verdicts.clear();
            From = null;
        }

        @Override
        public int getNeighbors(Node[] Out, Node Current) {
            Node Was = From;
            From = Current;
            try {
                return super.getNeighbors(Out, Current);
            } finally {
                From = Was;
            }
        }

        @Override
        public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
            PathType Found = super.getPathType(Context, X, Y, Z);
            if (Found != PathType.BLOCKED && Found != PathType.DOOR_IRON_CLOSED || mob == null) {
                return Found;
            }
            BlockPos Pos = new BlockPos(X, Y, Z);
            BlockState State = Context.getBlockState(Pos);
            if (!NpcDoorAccess.isClosedGate(State)) {
                return Found;
            }
            return passable(Pos, State, Context) ? PathType.DOOR_WOOD_CLOSED : Found;
        }

        private boolean passable(BlockPos Pos, BlockState State, PathfindingContext Context) {
            Level World = mob.level();
            List<BlockPos> Cells = NpcDoors.extent(World, Pos);
            if (Cells.isEmpty()) {
                return false;
            }
            BlockPos Anchor = NpcDoorAccess.anchor(Cells);
            Long Until = Refused.get(Anchor);
            if (Until != null && Until > World.getGameTime()) {
                return false;
            }
            Vec3 Middle = NpcDoorAccess.middle(Cells);
            Vec3 Normal = NpcDoorAccess.normal(State);
            Vec3 Origin = From != null ? new Vec3(From.x + 0.5, From.y, From.z + 0.5) : Vec3.atBottomCenterOf(Context.mobPosition());
            double Offset = NpcDoorAccess.offset(Origin, Middle, Normal);
            if (Math.abs(Offset) < NpcDoorAccess.Plane) {
                return true;
            }
            double Side = Math.signum(Offset);
            long Key = Anchor.asLong() * 2L + (Side > 0.0 ? 1L : 0L);
            Boolean Known = Verdicts.get(Key);
            if (Known == null) {
                Known = NpcDoorAccess.plan(mob, World, Pos, Side) != null;
                Verdicts.put(Key, Known);
            }
            return Known;
        }
    }
}
