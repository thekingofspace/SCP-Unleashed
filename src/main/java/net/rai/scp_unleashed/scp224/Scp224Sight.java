package net.rai.scp_unleashed.scp224;

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
import net.rai.scp_unleashed.control.ControlBodies;

import java.util.ArrayList;
import java.util.List;

public final class Scp224Sight {
    public static final double MaxReach = 32.0;
    public static final double DialHeight = 25.0 / 16.0;
    public static final double DialInset = 4.0 / 16.0;
    public static final double FaceLift = 0.06;
    public static final double FrontMargin = 0.05;
    public static final double SampleAcross = 0.18;
    public static final double SampleUp = 0.18;

    private Scp224Sight() {
    }

    public static Vec3 normal(Direction Facing) {
        return new Vec3(Facing.getStepX(), 0.0, Facing.getStepZ());
    }

    public static Vec3 across(Direction Facing) {
        return new Vec3(-Facing.getStepZ(), 0.0, Facing.getStepX());
    }

    public static Vec3 dial(BlockPos Lower, Direction Facing) {
        double Out = 0.5 - DialInset;
        return new Vec3(Lower.getX() + 0.5 + Facing.getStepX() * Out, Lower.getY() + DialHeight, Lower.getZ() + 0.5 + Facing.getStepZ() * Out);
    }

    public static boolean isExempt(LivingEntity Target) {
        if (!Target.isAlive() || Target.isRemoved() || !Targeting.canWitness(Target)) {
            return true;
        }
        if (Target instanceof Player User) {
            return User.isCreative() || User.isSpectator();
        }
        return Target instanceof Mob Body && Body.isNoAi();
    }

    public static boolean inFront(Vec3 Eye, BlockPos Lower, Direction Facing) {
        Vec3 Toward = Eye.subtract(dial(Lower, Facing));
        double Distance = Toward.length();
        return Distance <= MaxReach && Toward.dot(normal(Facing)) > FrontMargin;
    }

    public static boolean sees(Level World, Vec3 Eye, BlockPos Lower, Direction Facing) {
        if (!inFront(Eye, Lower, Facing)) {
            return false;
        }
        Vec3 Surface = dial(Lower, Facing).add(normal(Facing).scale(FaceLift));
        Vec3 Side = across(Facing);
        BlockPos Upper = Lower.above();
        double[][] Offsets = {{0.0, 0.0}, {-SampleAcross, -SampleUp}, {SampleAcross, -SampleUp}, {-SampleAcross, SampleUp}, {SampleAcross, SampleUp}};
        for (double[] Offset : Offsets) {
            Vec3 Point = Surface.add(Side.scale(Offset[0])).add(0.0, Offset[1], 0.0);
            if (clear(World, Point, Eye, Lower, Upper)) {
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

    public static boolean caught(LivingEntity Target, Scp224Room Room, BlockPos Lower, Direction Facing) {
        return !isExempt(Target) && Room.contains(Target) && sees(Target.level(), Target.getEyePosition(), Lower, Facing);
    }

    public static List<LivingEntity> victims(ServerLevel World, BlockPos Lower, Direction Facing, Scp224Room Room) {
        List<LivingEntity> Found = new ArrayList<>();
        Vec3 Centre = dial(Lower, Facing);
        double Reach = (MaxReach + 1.0) * (MaxReach + 1.0);
        for (ServerPlayer User : World.players()) {
            if (User.getEyePosition().distanceToSqr(Centre) <= Reach && caught(User, Room, Lower, Facing)) {
                Found.add(User);
            }
        }
        AABB Area = AABB.ofSize(Centre, MaxReach * 2.0, MaxReach * 2.0, MaxReach * 2.0);
        for (LivingEntity Person : World.getEntitiesOfClass(LivingEntity.class, Area, Each -> !(Each instanceof Player) && ControlBodies.isPersonLike(Each))) {
            if (caught(Person, Room, Lower, Facing)) {
                Found.add(Person);
            }
        }
        return Found;
    }
}
