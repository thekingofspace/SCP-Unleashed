package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.npc.doors.NpcDoorNavigation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RaidAccess {
    public static final double Reach = 4.5;
    public static final double Inset = 0.05;
    public static final int RouteNodes = 900;
    public static final float RouteRange = 40.0f;
    public static final int RouteMargin = 8;

    private RaidAccess() {
    }

    public static AABB box(Level World, BlockPos Pos) {
        VoxelShape Shape = World.getBlockState(Pos).getShape(World, Pos);
        return Shape.isEmpty() ? new AABB(Pos) : Shape.bounds().move(Pos);
    }

    public static boolean inReach(Entity Raider, AABB Box) {
        return Box.distanceToSqr(Raider.getEyePosition()) <= Reach * Reach;
    }

    public static boolean partner(Level World, BlockPos Hit, BlockPos Pos) {
        if (Hit.equals(Pos)) {
            return true;
        }
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof ChestBlock) || !State.hasProperty(ChestBlock.TYPE) || State.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return false;
        }
        return Pos.relative(ChestBlock.getConnectedDirection(State)).equals(Hit);
    }

    public static List<Vec3> aims(AABB Box, Vec3 Eye) {
        List<Vec3> Found = new ArrayList<>();
        Vec3 Middle = Box.getCenter();
        Found.add(Middle);
        for (Direction Side : Direction.values()) {
            double X = Side.getStepX() < 0 ? Box.minX + Inset : Side.getStepX() > 0 ? Box.maxX - Inset : Middle.x;
            double Y = Side.getStepY() < 0 ? Box.minY + Inset : Side.getStepY() > 0 ? Box.maxY - Inset : Middle.y;
            double Z = Side.getStepZ() < 0 ? Box.minZ + Inset : Side.getStepZ() > 0 ? Box.maxZ - Inset : Middle.z;
            Vec3 Face = new Vec3(X, Y, Z);
            if (Face.subtract(Middle).dot(Eye.subtract(Middle)) > 0.0) {
                Found.add(Face);
            }
        }
        return Found;
    }

    public static boolean sees(Level World, Entity Raider, BlockPos Pos) {
        Vec3 Eye = Raider.getEyePosition();
        AABB Box = box(World, Pos);
        for (Vec3 Aim : aims(Box, Eye)) {
            BlockHitResult Hit = World.clip(new ClipContext(Eye, Aim, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, Raider));
            if (Hit.getType() == HitResult.Type.BLOCK && partner(World, Hit.getBlockPos(), Pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean touch(Entity Raider, BlockPos Pos) {
        Level World = Raider.level();
        return World.isLoaded(Pos) && inReach(Raider, box(World, Pos)) && sees(World, Raider, Pos);
    }

    public static boolean sees(Entity Raider, Entity Body) {
        Vec3 Eye = Raider.getEyePosition();
        Vec3 Aim = Body.getBoundingBox().getCenter();
        BlockHitResult Hit = Raider.level().clip(new ClipContext(Eye, Aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Raider));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static boolean touch(Entity Raider, Entity Body) {
        return Body.isAlive() && Body.level() == Raider.level() && inReach(Raider, Body.getBoundingBox()) && sees(Raider, Body);
    }

    @Nullable
    public static Path route(Mob Walker, Set<BlockPos> Goals, int Accuracy, float Range, int Nodes) {
        Level World = Walker.level();
        BlockPos From = Walker.blockPosition();
        int Span = (int) Range + RouteMargin;
        PathNavigationRegion Region = new PathNavigationRegion(World, From.offset(-Span, -Span, -Span), From.offset(Span, Span, Span));
        NpcDoorNavigation.Nodes Walk = new NpcDoorNavigation.Nodes();
        Walk.setCanPassDoors(true);
        Walk.setCanOpenDoors(true);
        Walk.setCanFloat(true);
        PathFinder Finder = new PathFinder(Walk, Nodes);
        Path Found = Finder.findPath(Region, Walker, Goals, Range, Accuracy, 1.0f);
        return Found != null && Found.canReach() ? Found : null;
    }

    public static boolean worthChecking(Mob Walker, BlockPos Goal) {
        double Near = RouteRange * 0.75;
        return Walker.blockPosition().distSqr(Goal) <= Near * Near;
    }

    public static boolean reachable(Mob Walker, BlockPos Goal, int Accuracy) {
        return route(Walker, Set.of(Goal), Accuracy, RouteRange, RouteNodes) != null;
    }

    public static boolean reachable(Mob Walker, BlockPos Goal) {
        return reachable(Walker, Goal, 1);
    }
}
