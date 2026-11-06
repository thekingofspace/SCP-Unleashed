package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

public class BellRinger extends CourtMinion {
    public static final int Toll = SpecialA;
    public static final double TollRadius = 10.0;
    public static final float TollHeal = 6.0f;
    public static final int HasteTicks = 100;
    private boolean NextHeal = true;

    public BellRinger(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.BellRinger);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Shy(this));
        behaviour(3, new Ring(this));
        behaviour(5, new Huddle(this));
    }

    public boolean nextHeal() {
        return NextHeal;
    }

    public void toll() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        boolean Heal = NextHeal;
        NextHeal = !NextHeal;
        play("toll", 2.5f, Heal ? 0.9f : 1.1f);
        CourtFx.ring(World, Heal ? CourtFx.Bone : CourtFx.Crimson, position(), TollRadius, 64, 0.2);
        CourtFx.ring(World, Heal ? CourtFx.Bone : CourtFx.Crimson, position(), TollRadius * 0.5, 32, 0.6);
        List<CourtMinion> Allies = World.getEntitiesOfClass(CourtMinion.class, getBoundingBox().inflate(TollRadius), Each -> Each != this && Each.isAlive());
        for (CourtMinion Ally : Allies) {
            if (Heal) {
                Ally.heal(TollHeal);
            } else {
                Ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, HasteTicks, 1));
            }
            World.sendParticles(Heal ? CourtFx.Bone : CourtFx.Crimson, Ally.getX(), Ally.getY() + Ally.getBbHeight() + 0.2, Ally.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (Action == Toll) {
            getNavigation().stop();
            if (Elapsed == 14) {
                toll();
            }
            return;
        }
        super.actionTick(Action, Elapsed);
    }

    @Override
    protected String clip(int Action) {
        return Action == Toll ? "toll" : null;
    }

    public static class Shy extends AvoidEntityGoal<Player> {
        public Shy(BellRinger Owner) {
            super(Owner, Player.class, 6.0f, 1.0, 1.25, Seen -> CourtFx.validTarget(Seen));
        }
    }

    public static class Ring extends Goal {
        private final BellRinger Owner;
        private int Wait = 60;

        public Ring(BellRinger Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return --Wait <= 0 && !Owner.busy() && !Owner.rising();
        }

        @Override
        public void start() {
            Owner.startAction(Toll, 30);
            Wait = 160;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    public static class Huddle extends Goal {
        private final BellRinger Owner;
        private int Repath;

        public Huddle(BellRinger Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (--Repath > 0 || Owner.busy()) {
                return false;
            }
            Repath = 30;
            return nearest() != null;
        }

        private CourtMinion nearest() {
            CourtMinion Best = null;
            for (CourtMinion Each : Owner.level().getEntitiesOfClass(CourtMinion.class, Owner.getBoundingBox().inflate(24.0), Each -> Each != Owner && !(Each instanceof BellRinger))) {
                if (Best == null || Each.distanceToSqr(Owner) < Best.distanceToSqr(Owner)) {
                    Best = Each;
                }
            }
            return Best != null && Best.distanceToSqr(Owner) > 64.0 ? Best : null;
        }

        @Override
        public void start() {
            CourtMinion Found = nearest();
            if (Found != null) {
                Owner.getNavigation().moveTo(Found, 0.9);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.getNavigation().isDone() && !Owner.busy();
        }
    }
}
