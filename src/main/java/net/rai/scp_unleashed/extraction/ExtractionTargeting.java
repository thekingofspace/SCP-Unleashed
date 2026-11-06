package net.rai.scp_unleashed.extraction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public final class ExtractionTargeting {
    public static final double Range = 8.0;
    public static final double Margin = 0.3;

    private ExtractionTargeting() {
    }

    @Nullable
    public static Entity findTarget(LivingEntity User) {
        return findTarget(User, User.getEyePosition(), User.getViewVector(1.0F));
    }

    @Nullable
    public static Entity findTarget(LivingEntity User, Vec3 Eye, Vec3 Look) {
        return findEntity(User, Eye, Look, Range, ExtractionTargeting::isTarget);
    }

    @Nullable
    public static Entity findEntity(LivingEntity User, double Distance, Predicate<Entity> Filter) {
        return findEntity(User, User.getEyePosition(), User.getViewVector(1.0F), Distance, Filter);
    }

    @Nullable
    public static Entity findEntity(LivingEntity User, Vec3 Eye, Vec3 Look, double Distance, Predicate<Entity> Filter) {
        Level World = User.level();
        Vec3 Reach = Eye.add(Look.scale(Distance));
        BlockHitResult Wall = World.clip(new ClipContext(Eye, Reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        if (Wall.getType() != HitResult.Type.MISS) {
            Reach = Wall.getLocation();
        }
        double Nearest = Eye.distanceToSqr(Reach) + 1.0E-4;
        Entity Found = null;
        AABB Search = new AABB(Eye, Reach).inflate(Margin + 1.0);
        for (Entity Candidate : World.getEntities(User, Search, Filter)) {
            AABB Box = Candidate.getBoundingBox().inflate(Margin);
            if (Box.contains(Eye)) {
                return Candidate;
            }
            Optional<Vec3> Hit = Box.clip(Eye, Reach);
            if (Hit.isPresent()) {
                double Gap = Eye.distanceToSqr(Hit.get());
                if (Gap < Nearest) {
                    Nearest = Gap;
                    Found = Candidate;
                }
            }
        }
        return Found;
    }

    public static boolean isTarget(Entity Candidate) {
        return Candidate instanceof Extractable Rift && Candidate.isAlive() && Rift.canBeExtracted();
    }

    public static float secondsFor(FragmentRarity Grade) {
        return switch (Grade) {
            case Faint -> 3.0F;
            case Wavering -> 4.5F;
            case Volatile -> 6.5F;
            case Severe -> 9.0F;
            case Abyssal -> 12.0F;
        };
    }

    public static float stepFor(FragmentRarity Grade) {
        return 1.0F / (secondsFor(Grade) * 20.0F);
    }
}
