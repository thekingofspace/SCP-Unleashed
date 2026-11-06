package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Scp280Light {
    public static final int DarkLevel = 5;
    public static final int SpotLevel = 5;
    public static final int WellLitLevel = 11;
    public static final int BrightLevel = 10;
    public static final int StrikeLevel = 7;
    public static final int ShineEmission = 10;
    public static final double ShineRange = 12.0;
    public static final double ShineCone = 0.92;
    public static final int SealReach = 8;
    public static final double RayHalf = 0.06;

    private Scp280Light() {
    }

    public static int at(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return 0;
        }
        return World.getMaxLocalRawBrightness(Pos);
    }

    public static int of(Entity Subject) {
        BlockPos Feet = Subject.blockPosition();
        BlockPos Middle = BlockPos.containing(Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.5, Subject.getZ());
        return Math.max(at(Subject.level(), Feet), at(Subject.level(), Middle));
    }

    public static boolean isDark(Entity Subject) {
        return of(Subject) <= DarkLevel;
    }

    public static boolean isWellLit(Entity Subject) {
        return of(Subject) >= WellLitLevel;
    }

    public static int standing(Level World, BlockPos Pos) {
        return Math.max(at(World, Pos), at(World, Pos.above()));
    }

    public static boolean isBright(Level World, BlockPos Pos) {
        return standing(World, Pos) >= BrightLevel;
    }

    public static boolean isBright(Entity Subject) {
        return of(Subject) >= BrightLevel;
    }

    public static boolean strikeable(Entity Subject) {
        return of(Subject) <= StrikeLevel;
    }

    public static int emission(ItemStack Stack) {
        if (Stack.isEmpty() || !(Stack.getItem() instanceof BlockItem Placed)) {
            return 0;
        }
        return Placed.getBlock().defaultBlockState().getLightEmission();
    }

    public static int heldLight(LivingEntity Holder) {
        return Math.max(emission(Holder.getMainHandItem()), emission(Holder.getOffhandItem()));
    }

    public static boolean shining(LivingEntity Holder, Entity Target) {
        if (heldLight(Holder) < ShineEmission || Holder.isSpectator()) {
            return false;
        }
        Vec3 Eye = Holder.getEyePosition();
        Vec3 Middle = Target.position().add(0.0, Target.getBbHeight() * 0.55, 0.0);
        Vec3 Toward = Middle.subtract(Eye);
        double Distance = Toward.length();
        if (Distance > ShineRange || Distance < 1.0e-3) {
            return Distance < 1.0e-3;
        }
        if (Holder.getViewVector(1.0f).dot(Toward.scale(1.0 / Distance)) < ShineCone) {
            return false;
        }
        return Holder.level().clip(new ClipContext(Eye, Middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Holder)).getType() == HitResult.Type.MISS;
    }

    public static int sealedSides(Level World, Vec3 Centre, double FeetY, double Height) {
        int Sealed = 0;
        double[] Heights = {FeetY + 0.3, FeetY + Math.min(1.3, Height * 0.5), FeetY + Math.max(0.6, Height - 0.3)};
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            boolean All = true;
            for (double Lift : Heights) {
                if (!held(World, new Vec3(Centre.x, Lift, Centre.z), Side)) {
                    All = false;
                    break;
                }
            }
            if (All) {
                Sealed++;
            }
        }
        if (held(World, new Vec3(Centre.x, FeetY + 0.5, Centre.z), Direction.DOWN)) {
            Sealed++;
        }
        if (held(World, new Vec3(Centre.x, FeetY + Math.max(0.6, Height - 0.3), Centre.z), Direction.UP)) {
            Sealed++;
        }
        return Sealed;
    }

    public static boolean sealed(Level World, Entity Subject) {
        return sealedSides(World, Subject.position(), Subject.getY(), Subject.getBbHeight()) >= 6;
    }

    public static boolean held(Level World, Vec3 From, Direction Side) {
        BlockPos Start = BlockPos.containing(From);
        for (int Step = 0; Step <= SealReach; Step++) {
            BlockPos Pos = Start.relative(Side, Step);
            if (!World.isLoaded(Pos)) {
                return false;
            }
            if (blocks(World, Pos, World.getBlockState(Pos), From, Side)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocks(Level World, BlockPos Pos, BlockState State, Vec3 From, Direction Side) {
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        if (Shape.isEmpty()) {
            return false;
        }
        AABB Segment = switch (Side.getAxis()) {
            case X -> new AABB(Pos.getX(), From.y - RayHalf, From.z - RayHalf, Pos.getX() + 1.0, From.y + RayHalf, From.z + RayHalf);
            case Z -> new AABB(From.x - RayHalf, From.y - RayHalf, Pos.getZ(), From.x + RayHalf, From.y + RayHalf, Pos.getZ() + 1.0);
            case Y -> new AABB(From.x - RayHalf, Pos.getY(), From.z - RayHalf, From.x + RayHalf, Pos.getY() + 1.0, From.z + RayHalf);
        };
        return Shapes.joinIsNotEmpty(Shape.move(Pos.getX(), Pos.getY(), Pos.getZ()), Shapes.create(Segment), BooleanOp.AND);
    }
}
