package net.rai.scp_unleashed.scp151;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.senses.Senses;

public final class Scp151Gaze {
    public static final double Range = 12.0;
    public static final double ViewCone = 15.0;
    public static final double CanvasRadius = 0.45;
    public static final double MaxOblique = 72.0;
    public static final double FrontMargin = 0.05;
    public static final double CanvasInset = 6.0 / 16.0;
    public static final double CanvasHeight = 21.0 / 16.0;
    public static final double HalfWidth = 8.0 / 16.0;
    public static final double HalfHeight = 6.0 / 16.0;
    public static final double SurfaceLift = 0.03;
    public static final double SampleAcross = 0.3;
    public static final double SampleUp = 0.22;
    public static final double NearLimit = 0.15;
    private static final double ConeCosine = Math.cos(Math.toRadians(MaxOblique));

    private Scp151Gaze() {
    }

    public static Vec3 normal(Direction Facing) {
        return new Vec3(Facing.getStepX(), 0.0, Facing.getStepZ());
    }

    public static Vec3 across(Direction Facing) {
        return new Vec3(-Facing.getStepZ(), 0.0, Facing.getStepX());
    }

    public static Vec3 centre(BlockPos Lower, Direction Facing) {
        return new Vec3(Lower.getX() + 0.5 + Facing.getStepX() * CanvasInset, Lower.getY() + CanvasHeight, Lower.getZ() + 0.5 + Facing.getStepZ() * CanvasInset);
    }

    public static boolean isExempt(Player User) {
        return !User.isAlive() || User.isCreative() || User.isSpectator() || User.isSleeping() || AntiMemetic.isProtected(User);
    }

    public static boolean isExempt(LivingEntity Viewer) {
        if (Viewer instanceof Player User) {
            return isExempt(User);
        }
        return !Viewer.isAlive() || Viewer.isSleeping() || Viewer instanceof Mob Body && Body.isNoAi() || AntiMemetic.isProtected(Viewer);
    }

    public static boolean inView(Vec3 Eye, Vec3 Look, BlockPos Lower, Direction Facing) {
        Vec3 Centre = centre(Lower, Facing);
        Vec3 Toward = Eye.subtract(Centre);
        double Distance = Toward.length();
        if (Distance > Range || Distance < NearLimit) {
            return false;
        }
        double Front = Toward.dot(normal(Facing));
        if (Front <= FrontMargin || Front / Distance < ConeCosine) {
            return false;
        }
        double Allowed = Math.toRadians(ViewCone) + Math.atan(CanvasRadius / Distance);
        return Look.dot(Toward.scale(-1.0 / Distance)) >= Math.cos(Allowed);
    }

    public static boolean sees(Level World, Vec3 Eye, Vec3 Look, BlockPos Lower, Direction Facing) {
        if (!inView(Eye, Look, Lower, Facing)) {
            return false;
        }
        Vec3 Surface = centre(Lower, Facing).add(normal(Facing).scale(SurfaceLift));
        Vec3 Side = across(Facing);
        BlockPos Upper = Lower.above();
        double[][] Offsets = {{0.0, 0.0}, {-SampleAcross, -SampleUp}, {SampleAcross, -SampleUp}, {-SampleAcross, SampleUp}, {SampleAcross, SampleUp}};
        for (double[] Offset : Offsets) {
            Vec3 Point = Surface.add(Side.scale(Offset[0])).add(0.0, Offset[1], 0.0);
            if (clear(World, Eye, Point, Lower, Upper)) {
                return true;
            }
        }
        return false;
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To, BlockPos Lower, BlockPos Upper) {
        Boolean Blocked = BlockGetter.traverseBlocks(From, To, World, (Getter, Pos) -> {
            if (Pos.equals(Lower) || Pos.equals(Upper)) {
                return null;
            }
            BlockState State = Getter.getBlockState(Pos);
            VoxelShape Shape = State.getVisualShape(Getter, Pos, CollisionContext.empty());
            if (Shape.isEmpty()) {
                return null;
            }
            return Shape.clip(From, To, Pos) != null ? Boolean.TRUE : null;
        }, Getter -> Boolean.FALSE);
        return !Blocked;
    }

    public static boolean watching(LivingEntity Viewer, BlockPos Lower, Direction Facing) {
        return Targeting.canWitness(Viewer) && !isExempt(Viewer) && !Senses.isBlindfolded(Viewer) && sees(Viewer.level(), CameraViews.eye(Viewer), CameraViews.look(Viewer), Lower, Facing);
    }

    public static int sweep(ServerLevel World, BlockPos Lower, Direction Facing) {
        Vec3 Centre = centre(Lower, Facing);
        double Reach = (Range + 1.0) * (Range + 1.0);
        long Now = World.getGameTime();
        int Count = 0;
        for (ServerPlayer User : World.players()) {
            if (CameraViews.eye(User).distanceToSqr(Centre) <= Reach && watching(User, Lower, Facing)) {
                Scp151Breath.gaze(User, Now);
                Count++;
            }
        }
        AABB Area = AABB.ofSize(Centre, Range * 2.0, Range * 2.0, Range * 2.0);
        for (LivingEntity Person : World.getEntitiesOfClass(LivingEntity.class, Area, Each -> !(Each instanceof Player) && ControlBodies.isPersonLike(Each))) {
            if (watching(Person, Lower, Facing)) {
                Scp151Breath.gaze(Person, Now);
                Count++;
            }
        }
        return Count;
    }
}
