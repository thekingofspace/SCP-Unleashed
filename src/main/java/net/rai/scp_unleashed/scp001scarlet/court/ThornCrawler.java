package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class ThornCrawler extends CourtMinion {
    private boolean Climbing;

    public ThornCrawler(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.ThornCrawler);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new WallClimberNavigation(this, World);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Skitter(this));
        behaviour(4, new SowThorns(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Climbing = horizontalCollision;
        }
    }

    @Override
    public boolean onClimbable() {
        return Climbing;
    }

    @Override
    protected int hitTick() {
        return 7;
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public void sow() {
        if (level() instanceof ServerLevel World) {
            CourtProp Patch = CourtProp.spawn(World, ScarletCourtRegistry.ThornPatchType.get(), blockPosition(), 200);
            if (Patch != null) {
                play("thorns", 0.9f, 1.0f);
            }
        }
    }

    public static class Skitter extends CourtGoals.Melee {
        public Skitter(CourtMinion Owner) {
            super(Owner, 1.1, 22, 14);
        }
    }

    public static class SowThorns extends Goal {
        private final ThornCrawler Owner;
        private int Wait = 60;

        public SowThorns(ThornCrawler Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            if (--Wait > 0) {
                return false;
            }
            LivingEntity Target = Owner.getTarget();
            return Target != null && Owner.onGround() && Owner.distanceTo(Target) < 18.0
                    && Owner.level().getEntitiesOfClass(CourtProp.class, Owner.getBoundingBox().inflate(2.5), Each -> Each.getType() == ScarletCourtRegistry.ThornPatchType.get()).isEmpty();
        }

        @Override
        public void start() {
            Owner.sow();
            Wait = 120;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
