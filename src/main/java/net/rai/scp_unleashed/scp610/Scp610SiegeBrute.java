package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidRegistry;

public class Scp610SiegeBrute extends Scp610Unit {
    public static final float MaxHardness = 3.0f;
    public static final int SmashGap = 10;
    public static final int SmashLimit = 2;
    private boolean Sieging;

    public Scp610SiegeBrute(EntityType<? extends Scp610SiegeBrute> Type, Level World) {
        super(Type, World, Scp610Form.SiegeBrute);
    }

    public void setSieging(boolean Value) {
        Sieging = Value;
    }

    public boolean sieging() {
        if (Sieging) {
            return true;
        }
        if (!hasData(RaidRegistry.Mark)) {
            return false;
        }
        RaidMark Mark = getData(RaidRegistry.Mark);
        return Mark.marked();
    }

    public static boolean breakable(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.isAir() || !State.getFluidState().isEmpty() || State.is(Scp610Registry.Unspreadable) || Scp610Growth.isFlesh(State)) {
            return false;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        return Hardness >= 0.0f && Hardness <= MaxHardness;
    }

    public int smashAhead(ServerLevel World) {
        if (!EventHooks.canEntityGrief(World, this)) {
            return 0;
        }
        Direction Facing = getDirection();
        BlockPos Front = blockPosition().relative(Facing);
        int Broken = 0;
        int Reach = (int) Math.ceil(getBbHeight());
        for (int Rise = 0; Rise < Reach && Broken < SmashLimit; Rise++) {
            for (BlockPos Pos : new BlockPos[]{Front.above(Rise), Front.relative(Facing.getClockWise()).above(Rise)}) {
                if (Broken >= SmashLimit) {
                    break;
                }
                BlockState State = World.getBlockState(Pos);
                if (breakable(World, Pos, State) && World.destroyBlock(Pos, true, this)) {
                    Broken++;
                }
            }
        }
        if (Broken > 0) {
            World.playSound(null, getX(), getY(), getZ(), Scp610Sounds.Slam.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
        }
        return Broken;
    }

    @Override
    protected void serverTick() {
        if (tickCount % SmashGap != 0 || !(level() instanceof ServerLevel World) || !sieging() || getTarget() == null) {
            return;
        }
        if (horizontalCollision || getNavigation().isStuck()) {
            smashAhead(World);
        }
    }
}
