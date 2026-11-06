package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class LabExposure {
    public static final double Radius = 16.0;
    public static final int PulseTicks = 40;
    public static final double CarrierReach = 1.0;
    public static final Predicate<BlockPos> NothingIgnored = Pos -> false;

    private LabExposure() {
    }

    public static boolean isPulse(Level World, long Salt) {
        return Math.floorMod(World.getGameTime() + Salt, PulseTicks) == 0;
    }

    public static List<ServerPlayer> playersInSight(ServerLevel World, Vec3 From, Predicate<BlockPos> Ignored, double Range, Predicate<ServerPlayer> Candidate) {
        List<ServerPlayer> Found = new ArrayList<>();
        double Reach = Range * Range;
        for (ServerPlayer Target : World.players()) {
            if (!Target.isAlive() || Target.getEyePosition().distanceToSqr(From) > Reach || !Candidate.test(Target)) {
                continue;
            }
            if (sees(World, From, Target, Ignored)) {
                Found.add(Target);
            }
        }
        return Found;
    }

    public static boolean sees(Level World, Vec3 From, Entity Target, Predicate<BlockPos> Ignored) {
        Vec3 Eyes = Target.getEyePosition();
        if (Eyes.distanceToSqr(From) <= CarrierReach * CarrierReach) {
            return true;
        }
        return clearPath(World, From, Eyes, Ignored) || clearPath(World, From, Target.getBoundingBox().getCenter(), Ignored);
    }

    public static boolean clearPath(Level World, Vec3 From, Vec3 To, Predicate<BlockPos> Ignored) {
        Boolean Blocked = BlockGetter.traverseBlocks(From, To, World, (Getter, Pos) -> {
            if (Ignored.test(Pos)) {
                return null;
            }
            BlockState State = Getter.getBlockState(Pos);
            VoxelShape Shape = State.getCollisionShape(Getter, Pos);
            if (Shape.isEmpty()) {
                return null;
            }
            return Shape.clip(From, To, Pos) != null ? Boolean.TRUE : null;
        }, Getter -> Boolean.FALSE);
        return !Blocked;
    }
}
