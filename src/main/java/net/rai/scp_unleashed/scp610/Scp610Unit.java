package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Scp610Unit extends Scp610Creature {
    public static final double SlamRadius = 2.5;
    public static final double HulkKnock = 1.2;
    @Nullable
    private Vec3 MergePoint;

    public Scp610Unit(EntityType<? extends Scp610Unit> Type, Level World, Scp610Form Form) {
        super(Type, World, Form);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GoMerge(this));
    }

    public boolean merging() {
        return MergePoint != null;
    }

    public void mergeAt(@Nullable Vec3 Point) {
        MergePoint = Point;
        if (Point != null) {
            setTarget(null);
        }
    }

    @Nullable
    public Vec3 mergePoint() {
        return MergePoint;
    }

    @Override
    protected void hit() {
        if (form() == Scp610Form.Colossus && level() instanceof ServerLevel World) {
            AABB Area = getBoundingBox().inflate(SlamRadius, 0.5, SlamRadius);
            float Damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity Victim : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> isQuarry(this, Found) && Found.distanceToSqr(this) <= (SlamRadius + 1.0) * (SlamRadius + 1.0))) {
                strike(Victim, Damage, 0.8);
            }
            World.sendParticles(Gore, getX(), getY() + 0.1, getZ(), 30, SlamRadius * 0.6, 0.1, SlamRadius * 0.6, 0.0);
            return;
        }
        if (form() == Scp610Form.Hulk) {
            LivingEntity Target = getTarget();
            if (Target != null && reachTo(Target) <= attackReach() + 0.7) {
                strike(Target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), HulkKnock);
            }
            return;
        }
        super.hit();
    }

    public static final class GoMerge extends Goal {
        private final Scp610Unit Walker;

        public GoMerge(Scp610Unit Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Walker.merging() && !Walker.locksMovement();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 Point = Walker.mergePoint();
            if (Point != null && Walker.tickCount % 10 == 0) {
                Walker.getNavigation().moveTo(Point.x, Point.y, Point.z, 1.0);
            }
        }
    }
}
