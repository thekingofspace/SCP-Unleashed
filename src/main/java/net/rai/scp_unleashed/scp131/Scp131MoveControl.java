package net.rai.scp_unleashed.scp131;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp131MoveControl extends MoveControl {
    public static final double Accel = 0.09;
    public static final double Brake = 0.14;
    public static final double BrakeDistance = 2.0;
    public static final double BrakeFloor = 0.3;
    public static final float TurnSlow = 34.0f;
    public static final float TurnFast = 13.0f;
    public static final float SharpTurn = 75.0f;
    public static final float WideTurn = 35.0f;
    public static final double CoastFloor = 0.05;
    public static final double EdgeNear = 0.45;
    public static final double EdgeTicks = 3.0;
    public static final int SafeDrop = 2;
    public static final double EdgeDamp = 0.15;

    private final Scp131Pod Pod;
    private double Pace;
    private boolean Halted;

    public Scp131MoveControl(Scp131Pod Pod) {
        super(Pod);
        this.Pod = Pod;
    }

    public double pace() {
        return Pace;
    }

    public boolean halted() {
        return Halted;
    }

    @Override
    public void tick() {
        Halted = false;
        if (operation == Operation.MOVE_TO) {
            operation = Operation.WAIT;
            double Dx = wantedX - mob.getX();
            double Dz = wantedZ - mob.getZ();
            double Dy = wantedY - mob.getY();
            double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Flat * Flat + Dy * Dy < MIN_SPEED_SQR) {
                coast();
                return;
            }
            float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            float Error = Math.abs(Mth.wrapDegrees(Wanted - mob.getYRot()));
            float Share = (float) Mth.clamp(Pace / Scp131Pod.ZoomModifier, 0.0, 1.0);
            mob.setYRot(rotlerp(mob.getYRot(), Wanted, Mth.lerp(Share, TurnSlow, TurnFast)));
            double Desired = speedModifier * Mth.clamp(remaining(Flat) / BrakeDistance, BrakeFloor, 1.0) * turnFactor(Error);
            Pace = Pace < Desired ? Math.min(Desired, Pace + Accel) : Math.max(Desired, Pace - Brake);
            if (edgeAhead()) {
                halt();
                return;
            }
            mob.setSpeed((float) (Pace * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            BlockPos Here = mob.blockPosition();
            BlockState State = mob.level().getBlockState(Here);
            VoxelShape Shape = State.getCollisionShape(mob.level(), Here);
            if (Dy > mob.maxUpStep() && Flat * Flat < Math.max(1.0f, mob.getBbWidth())
                    || !Shape.isEmpty() && mob.getY() < Shape.max(Direction.Axis.Y) + Here.getY() && !State.is(BlockTags.DOORS) && !State.is(BlockTags.FENCES)) {
                mob.getJumpControl().jump();
                operation = Operation.JUMPING;
            }
            return;
        }
        if (operation == Operation.WAIT) {
            coast();
            return;
        }
        super.tick();
    }

    private double turnFactor(float Error) {
        if (Error >= SharpTurn) {
            return 0.12;
        }
        if (Error <= WideTurn) {
            return 1.0;
        }
        return Mth.lerp((Error - WideTurn) / (SharpTurn - WideTurn), 1.0, 0.12);
    }

    private double remaining(double Flat) {
        Path Current = mob.getNavigation().getPath();
        if (Current == null || Current.isDone()) {
            return Flat;
        }
        Node End = Current.getEndNode();
        if (End == null) {
            return Flat;
        }
        double Dx = End.x + 0.5 - mob.getX();
        double Dz = End.z + 0.5 - mob.getZ();
        return Math.max(Flat, Math.sqrt(Dx * Dx + Dz * Dz));
    }

    private void coast() {
        Pace = Math.max(0.0, Pace - Brake);
        if (Pace > CoastFloor && mob.onGround() && !edgeAhead()) {
            mob.setSpeed((float) (Pace * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            return;
        }
        if (Pace > CoastFloor) {
            halt();
            return;
        }
        Pace = 0.0;
        mob.setSpeed(0.0f);
        mob.setZza(0.0f);
    }

    private void halt() {
        if (Pace > Scp131Pod.WanderModifier) {
            Pod.bumped();
        }
        Pace = 0.0;
        Halted = true;
        mob.setSpeed(0.0f);
        mob.setZza(0.0f);
        Vec3 Motion = mob.getDeltaMovement();
        mob.setDeltaMovement(Motion.x * EdgeDamp, Motion.y, Motion.z * EdgeDamp);
    }

    public void stop() {
        Pace = 0.0;
        operation = Operation.WAIT;
        mob.setSpeed(0.0f);
        mob.setZza(0.0f);
    }

    public boolean edgeAhead() {
        if (!mob.onGround() || mob.isInWater()) {
            return false;
        }
        Vec3 Facing = Vec3.directionFromRotation(0.0f, mob.getYRot());
        Vec3 Motion = mob.getDeltaMovement();
        double Speed = Math.max(Motion.horizontalDistance(), Pace * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2);
        double Far = EdgeNear + Speed * EdgeTicks;
        for (double Reach : new double[]{EdgeNear, (EdgeNear + Far) * 0.5, Far}) {
            BlockPos Ahead = BlockPos.containing(mob.getX() + Facing.x * Reach, mob.getY() + 0.2, mob.getZ() + Facing.z * Reach);
            if (!safeColumn(mob.level(), Ahead)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hazard(BlockState State) {
        return !State.getFluidState().isEmpty() || State.is(BlockTags.FIRE) || State.is(BlockTags.CAMPFIRES) || State.is(Blocks.MAGMA_BLOCK)
                || State.is(Blocks.LAVA) || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.CACTUS)
                || State.is(Blocks.WITHER_ROSE);
    }

    public static boolean safeColumn(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockState At = World.getBlockState(Pos);
        if (hazard(At)) {
            return false;
        }
        if (!At.getCollisionShape(World, Pos).isEmpty()) {
            return !hazard(World.getBlockState(Pos.above()));
        }
        for (int Down = 1; Down <= SafeDrop + 1; Down++) {
            BlockPos Below = Pos.below(Down);
            BlockState State = World.getBlockState(Below);
            if (hazard(State)) {
                return false;
            }
            if (!State.getCollisionShape(World, Below).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
