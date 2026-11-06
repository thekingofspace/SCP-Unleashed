package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class RaidBreakDoorGoal extends RaidGoal {
    public static final double Reach = 2.25;
    private final Predicate<BlockState> Breakable;
    private final int BreakTicks;
    @Nullable
    private BlockPos Door;
    private int Time;
    private int Shown = -1;

    public RaidBreakDoorGoal(Mob Owner, ResourceLocation Objective, Predicate<BlockState> Breakable, int BreakTicks) {
        super(Owner, Objective);
        this.Breakable = Breakable;
        this.BreakTicks = Math.max(20, BreakTicks);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean breakable(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && Breakable.test(World.getBlockState(Pos));
    }

    @Nullable
    private BlockPos lowest(ServerLevel World, BlockPos Pos) {
        if (breakable(World, Pos.below()) && Owner.blockPosition().getY() <= Pos.getY() - 1) {
            return Pos.below();
        }
        if (breakable(World, Pos)) {
            return Pos;
        }
        if (breakable(World, Pos.above())) {
            return Pos.above();
        }
        return null;
    }

    @Nullable
    private BlockPos find(ServerLevel World) {
        Path Route = Owner.getNavigation().getPath();
        if (Route != null && !Route.isDone()) {
            int Last = Math.min(Route.getNextNodeIndex() + 2, Route.getNodeCount());
            for (int Index = 0; Index < Last; Index++) {
                Node Step = Route.getNode(Index);
                BlockPos Pos = new BlockPos(Step.x, Step.y, Step.z);
                if (Owner.distanceToSqr(Pos.getX() + 0.5, Owner.getY(), Pos.getZ() + 0.5) > Reach * Reach) {
                    continue;
                }
                BlockPos Found = lowest(World, Pos);
                if (Found != null) {
                    return Found;
                }
            }
        }
        return lowest(World, Owner.blockPosition().relative(Owner.getDirection()));
    }

    @Override
    public boolean canUse() {
        ServerLevel World = world();
        if (World == null || activeRaid() == null || !Owner.horizontalCollision || !EventHooks.canEntityGrief(World, Owner)) {
            return false;
        }
        Door = find(World);
        return Door != null;
    }

    @Override
    public boolean canContinueToUse() {
        ServerLevel World = world();
        return World != null && Door != null && Time <= BreakTicks && activeRaid() != null && breakable(World, Door)
                && Owner.distanceToSqr(Door.getX() + 0.5, Door.getY(), Door.getZ() + 0.5) < (Reach + 1.0) * (Reach + 1.0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        Time = 0;
        Shown = -1;
        Owner.getNavigation().stop();
    }

    @Override
    public void stop() {
        ServerLevel World = world();
        if (World != null && Door != null) {
            World.destroyBlockProgress(Owner.getId(), Door, -1);
        }
        Door = null;
    }

    @Override
    public void tick() {
        ServerLevel World = world();
        if (World == null || Door == null) {
            return;
        }
        Owner.getLookControl().setLookAt(Door.getX() + 0.5, Door.getY() + 0.5, Door.getZ() + 0.5);
        Time++;
        if (Owner.getRandom().nextInt(20) == 0) {
            World.levelEvent(LevelEvent.SOUND_ZOMBIE_WOODEN_DOOR, Door, 0);
            Owner.swing(InteractionHand.MAIN_HAND);
        }
        int Progress = (int) (Time / (float) BreakTicks * 10.0f);
        if (Progress != Shown) {
            World.destroyBlockProgress(Owner.getId(), Door, Progress);
            Shown = Progress;
        }
        if (Time >= BreakTicks) {
            BlockState Was = World.getBlockState(Door);
            World.removeBlock(Door, false);
            World.levelEvent(LevelEvent.SOUND_ZOMBIE_DOOR_CRASH, Door, 0);
            World.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, Door, Block.getId(Was));
            World.destroyBlockProgress(Owner.getId(), Door, -1);
            Door = null;
        }
    }
}
