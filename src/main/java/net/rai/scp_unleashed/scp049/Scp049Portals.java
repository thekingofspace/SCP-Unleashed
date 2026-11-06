package net.rai.scp_unleashed.scp049;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp049Portals {
    public static final double ReachSquared = 2.6;
    public static final int LookAhead = 3;
    public static final int MinOpenTicks = 30;
    public static final double ClearRange = 1.4;
    public static final double PassedRange = 1.2;
    public static final double VictimProbe = 0.75;
    public static final int MaxOpen = 6;

    private final Scp049Doctor Owner;
    private final List<Opened> Open = new ArrayList<>();

    private static final class Opened {
        private final BlockPos Pos;
        private final long At;
        private boolean Escort;
        private boolean Reached;

        private Opened(BlockPos Pos, long At, boolean Escort) {
            this.Pos = Pos;
            this.At = At;
            this.Escort = Escort;
        }
    }

    public Scp049Portals(Scp049Doctor Owner) {
        this.Owner = Owner;
    }

    public int openCount() {
        return Open.size();
    }

    public void tick(ServerLevel World, @Nullable LivingEntity Carried) {
        Path Route = Owner.getNavigation().getPath();
        if (Route != null && !Route.isDone()) {
            int Last = Math.min(Route.getNextNodeIndex() + LookAhead, Route.getNodeCount());
            for (int Index = Math.max(0, Route.getNextNodeIndex() - 1); Index < Last; Index++) {
                Node Step = Route.getNode(Index);
                BlockPos Pos = new BlockPos(Step.x, Step.y, Step.z);
                if (tryOpen(World, Pos, Carried != null) || tryOpen(World, Pos.above(), Carried != null)) {
                    break;
                }
            }
        }
        closeBehind(World, Carried);
    }

    public void assist(ServerLevel World, LivingEntity Victim, Vec3 Point) {
        Vec3 Here = Victim.position();
        Vec3 Flat = new Vec3(Point.x - Here.x, 0.0, Point.z - Here.z);
        if (Flat.lengthSqr() < 0.01) {
            return;
        }
        Vec3 Probe = Here.add(Flat.normalize().scale(VictimProbe));
        for (double Lift : new double[]{0.2, 1.2}) {
            BlockPos Pos = BlockPos.containing(Probe.x, Here.y + Lift, Probe.z);
            if (Scp049Navigation.isPortal(World, Pos)) {
                BlockPos Base = baseOf(World.getBlockState(Pos), Pos);
                if (!isOpen(World.getBlockState(Base))) {
                    setOpen(World, Base, true);
                }
                remember(World, Base, true);
                return;
            }
        }
    }

    private static BlockPos baseOf(BlockState State, BlockPos Pos) {
        return State.getBlock() instanceof DoorBlock && State.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    private boolean tryOpen(ServerLevel World, BlockPos Pos, boolean Carrying) {
        if (Owner.distanceToSqr(Vec3.atBottomCenterOf(Pos)) > ReachSquared * 2.0 || !Scp049Navigation.isPortal(World, Pos)) {
            return false;
        }
        BlockPos Base = baseOf(World.getBlockState(Pos), Pos);
        if (isOpen(World.getBlockState(Base))) {
            remember(World, Base, Carrying);
            return false;
        }
        if (Owner.distanceToSqr(Vec3.atBottomCenterOf(Base)) > ReachSquared) {
            return false;
        }
        setOpen(World, Base, true);
        remember(World, Base, Carrying);
        Owner.reachForDoor();
        return true;
    }

    private void remember(ServerLevel World, BlockPos Pos, boolean Escort) {
        for (Opened Entry : Open) {
            if (Entry.Pos.equals(Pos)) {
                Entry.Escort |= Escort;
                return;
            }
        }
        if (Open.size() >= MaxOpen) {
            for (Opened Entry : Open) {
                if (!Entry.Escort) {
                    Open.remove(Entry);
                    setOpen(World, Entry.Pos, false);
                    break;
                }
            }
        }
        Open.add(new Opened(Pos.immutable(), World.getGameTime(), Escort));
    }

    private void closeBehind(ServerLevel World, @Nullable LivingEntity Carried) {
        long Now = World.getGameTime();
        Open.removeIf(Entry -> {
            if (!World.isLoaded(Entry.Pos) || !Scp049Navigation.isPortal(World, Entry.Pos)) {
                return true;
            }
            if (Carried != null) {
                Entry.Escort = true;
                if (near(Carried, Entry.Pos, PassedRange)) {
                    Entry.Reached = true;
                }
                if (!Entry.Reached) {
                    return false;
                }
            }
            if (Now - Entry.At < MinOpenTicks || near(Owner, Entry.Pos, ClearRange) || Carried != null && near(Carried, Entry.Pos, ClearRange) || occupied(World, Entry.Pos)) {
                return false;
            }
            setOpen(World, Entry.Pos, false);
            return true;
        });
    }

    public void closeAll(ServerLevel World) {
        for (Opened Entry : Open) {
            if (World.isLoaded(Entry.Pos) && Scp049Navigation.isPortal(World, Entry.Pos) && !occupied(World, Entry.Pos)) {
                setOpen(World, Entry.Pos, false);
            }
        }
        Open.clear();
    }

    private static boolean near(Entity Body, BlockPos Pos, double Range) {
        return Body.distanceToSqr(Vec3.atBottomCenterOf(Pos)) < Range * Range;
    }

    private static boolean occupied(ServerLevel World, BlockPos Pos) {
        return !World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos).expandTowards(0.0, 1.0, 0.0), Body -> Body.isAlive() && !Body.isSpectator()).isEmpty();
    }

    private static boolean isOpen(BlockState State) {
        if (State.getBlock() instanceof DoorBlock) {
            return State.getValue(DoorBlock.OPEN);
        }
        return State.getBlock() instanceof FenceGateBlock && State.getValue(FenceGateBlock.OPEN);
    }

    private void setOpen(ServerLevel World, BlockPos Pos, boolean Value) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof DoorBlock Door) {
            Door.setOpen(Owner, World, State, Pos, Value);
            return;
        }
        if (!(State.getBlock() instanceof FenceGateBlock) || State.getValue(FenceGateBlock.OPEN) == Value) {
            return;
        }
        BlockState Changed = State.setValue(FenceGateBlock.OPEN, Value);
        if (Value) {
            Direction Heading = Owner.getDirection();
            if (State.getValue(FenceGateBlock.FACING) == Heading.getOpposite()) {
                Changed = Changed.setValue(FenceGateBlock.FACING, Heading);
            }
        }
        World.setBlock(Pos, Changed, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        World.playSound(null, Pos, Value ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, World.random.nextFloat() * 0.1f + 0.9f);
        World.gameEvent(Owner, Value ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, Pos);
    }
}
