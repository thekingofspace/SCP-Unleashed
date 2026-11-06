package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class RaidSwitchGoal extends RaidGoal {
    public static final int Height = 3;
    public static final int Rest = 100;
    public static final int GiveUp = 200;
    public static final int StallLimit = 60;
    private final Predicate<BlockState> Switch;
    private final int Radius;
    private final Set<Long> Done = new HashSet<>();
    private int Layer = -Height;
    private int Pause;
    @Nullable
    private BlockPos Found;
    private int Time;
    private int Wait;
    private int Stall;

    public RaidSwitchGoal(Mob Owner, ResourceLocation Objective, Predicate<BlockState> Switch, int Radius) {
        super(Owner, Objective);
        this.Switch = Switch;
        this.Radius = Math.max(2, Radius);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    private BlockPos scanLayer(ServerLevel World, Raid Current) {
        BlockPos Centre = Owner.blockPosition();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockPos Best = null;
        double BestGap = Double.MAX_VALUE;
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                Cursor.set(Centre.getX() + Dx, Centre.getY() + Layer, Centre.getZ() + Dz);
                if (!World.isLoaded(Cursor) || !Switch.test(World.getBlockState(Cursor))) {
                    continue;
                }
                long Key = Cursor.asLong();
                if (Done.contains(Key) || Current.claimed(Key)) {
                    continue;
                }
                double Gap = Cursor.distSqr(Centre);
                if (Gap < BestGap) {
                    BestGap = Gap;
                    Best = Cursor.immutable();
                }
            }
        }
        return Best;
    }

    @Override
    public boolean canUse() {
        ServerLevel World = world();
        Raid Current = activeRaid();
        if (World == null || Current == null || Owner.getTarget() != null) {
            return false;
        }
        if (Pause > 0) {
            Pause--;
            return false;
        }
        BlockPos Seen = scanLayer(World, Current);
        Layer++;
        if (Layer > Height) {
            Layer = -Height;
            Pause = Rest;
        }
        if (Seen == null || !Current.claim(Seen.asLong())) {
            return false;
        }
        Found = Seen;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        ServerLevel World = world();
        return World != null && Found != null && Time < GiveUp && activeRaid() != null && Switch.test(World.getBlockState(Found));
    }

    @Override
    public void start() {
        Time = 0;
        Wait = 0;
        Stall = 0;
    }

    @Override
    public void stop() {
        Raid Current = raid();
        if (Current != null && Found != null) {
            Current.unclaim(Found.asLong());
            Done.add(Found.asLong());
        }
        Found = null;
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        ServerLevel World = world();
        if (World == null || Found == null) {
            return;
        }
        Time++;
        Owner.getLookControl().setLookAt(Found.getX() + 0.5, Found.getY() + 0.5, Found.getZ() + 0.5);
        if (!RaidAccess.touch(Owner, Found)) {
            if (--Wait <= 0) {
                Wait = 15;
                Owner.getNavigation().moveTo(Found.getX() + 0.5, Found.getY(), Found.getZ() + 0.5, 1.0);
            }
            if (Owner.getNavigation().isDone() && ++Stall > StallLimit) {
                Time = GiveUp;
            }
            return;
        }
        Stall = 0;
        BlockState State = World.getBlockState(Found);
        if (State.getBlock() instanceof LeverBlock Lever) {
            Lever.pull(State, World, Found, null);
        } else if (State.getBlock() instanceof ButtonBlock Button) {
            Button.press(State, World, Found, null);
        }
        Owner.swing(InteractionHand.MAIN_HAND);
        Done.add(Found.asLong());
        Time = GiveUp;
    }
}
