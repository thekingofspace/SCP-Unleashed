package net.rai.scp_unleashed.scp009;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;

import java.util.List;
import java.util.function.Predicate;

public final class Scp009Exposure {
    public static final double Radius = 10.0;
    public static final int Interval = 20;
    public static final int MaxSightChecks = 48;

    private Scp009Exposure() {
    }

    public static boolean isVulnerable(LivingEntity Target) {
        return Target.isAlive() && (!Scp009Doom.isExempt(Target) || InfectionImmunity.absorbs(Target, Scp009Registry.DocumentId)) && !Scp009Doom.isDoomed(Target)
                && !Scp009Doom.isProtected(Target);
    }

    public static int expose(ServerLevel World, Vec3 From, Predicate<BlockPos> Ignored) {
        double Reach = Radius * Radius;
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, new AABB(From, From).inflate(Radius),
                Target -> Target.getEyePosition().distanceToSqr(From) <= Reach && isVulnerable(Target));
        if (Found.isEmpty()) {
            return 0;
        }
        int Size = Found.size();
        int Start = (int) Math.floorMod(World.getGameTime() / Interval, (long) Size);
        int Checks = Math.min(Size, MaxSightChecks);
        int Doomed = 0;
        for (int Step = 0; Step < Checks; Step++) {
            LivingEntity Target = Found.get((Start + Step) % Size);
            if (sees(World, From, Target, Ignored) && Scp009Doom.doom(Target)) {
                Doomed++;
            }
        }
        return Doomed;
    }

    public static boolean sees(Level World, Vec3 From, LivingEntity Target, Predicate<BlockPos> Ignored) {
        return clearPath(World, From, Target.getEyePosition(), Ignored) || clearPath(World, From, Target.getBoundingBox().getCenter(), Ignored);
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
