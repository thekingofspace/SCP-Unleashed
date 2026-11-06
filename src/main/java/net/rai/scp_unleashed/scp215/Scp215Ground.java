package net.rai.scp_unleashed.scp215;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Scp215Ground {
    public static final double Missing = Double.NaN;

    private Scp215Ground() {
    }

    public static double top(Level World, BlockPos Below) {
        BlockState State = World.getBlockState(Below);
        if (State.is(BlockTags.LEAVES) || !State.getFluidState().isEmpty()) {
            return Missing;
        }
        VoxelShape Shape = State.getCollisionShape(World, Below);
        if (Shape.isEmpty()) {
            return Missing;
        }
        return Below.getY() + Math.min(1.0, Shape.max(Direction.Axis.Y));
    }

    public static boolean open(Level World, BlockPos Feet, int Clearance) {
        for (int Rise = 0; Rise < Clearance; Rise++) {
            BlockPos At = Feet.above(Rise);
            BlockState State = World.getBlockState(At);
            if (!State.getCollisionShape(World, At).isEmpty() || !State.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static double ground(Level World, double X, double Y, double Z, int Clearance, int Up, int Down) {
        int Bx = Mth.floor(X);
        int Bz = Mth.floor(Z);
        int Start = Mth.floor(Y + 0.01);
        if (!World.isLoaded(new BlockPos(Bx, Start, Bz))) {
            return Missing;
        }
        int Reach = Math.max(Up, Down);
        for (int Step = 0; Step <= Reach * 2; Step++) {
            int Rise = (Step % 2 == 0 ? 1 : -1) * ((Step + 1) / 2);
            if (Rise > Up || -Rise > Down) {
                continue;
            }
            BlockPos Feet = new BlockPos(Bx, Start + Rise, Bz);
            if (World.isOutsideBuildHeight(Feet)) {
                continue;
            }
            double Top = top(World, Feet.below());
            if (!Double.isNaN(Top) && open(World, Feet, Clearance)) {
                return Top;
            }
        }
        return Missing;
    }

    public static boolean sees(Level World, Vec3 From, Vec3 To, Entity Viewer) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    public static float yawToward(double Dx, double Dz) {
        return (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static float pitchToward(double Dx, double Dy, double Dz) {
        return (float) (-(Mth.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG));
    }

    public static float approach(float Current, float Target, float Limit) {
        float Delta = Mth.wrapDegrees(Target - Current);
        return Current + Mth.clamp(Delta, -Limit, Limit);
    }
}
