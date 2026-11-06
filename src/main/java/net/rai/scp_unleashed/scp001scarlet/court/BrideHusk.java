package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class BrideHusk extends CourtMinion {
    public static final int Brood = 3;
    public static final double BroodHealth = 12.0;

    public BrideHusk(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.BrideHusk);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Trudge(this));
    }

    @Override
    protected int hitTick() {
        return 16;
    }

    @Override
    protected double reach() {
        return 2.2;
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (Action == Attack && level() instanceof ServerLevel World) {
            getNavigation().stop();
            if (Elapsed == 2) {
                play("attack", 1.0f, 0.85f);
            }
            if (Elapsed < 16 && Elapsed % 4 == 0) {
                CourtFx.ring(World, CourtFx.Crimson, front(1.4), 1.3, 12, 0.1);
            }
            if (Elapsed == 16) {
                CourtFx.crumble(World, CourtFx.solidAt(World, blockPosition().below()), front(1.4), 16, 0.6, 0.1);
                for (LivingEntity Victim : CourtFx.victimsNear(World, front(1.4), 2.6)) {
                    if (CourtFx.strike(this, Victim, (float) Kind.damage())) {
                        CourtFx.push(Victim, position(), 0.7, 0.3);
                    }
                }
            }
            return;
        }
        super.actionTick(Action, Elapsed);
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel World) {
            burst(World);
        }
    }

    public void burst(ServerLevel World) {
        CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, position().add(0.0, 1.0, 0.0), 16, 0.6, 0.03);
        CourtFx.burst(World, CourtFx.Crimson, position().add(0.0, 1.0, 0.0), 24, 0.7, 0.0);
        for (int Index = 0; Index < Brood; Index++) {
            double Angle = Index * Math.PI * 2.0 / Brood + random.nextDouble();
            BlockPos Spot = CourtArena.groundOr(World, BlockPos.containing(getX() + Math.cos(Angle) * 1.2, getY(), getZ() + Math.sin(Angle) * 1.2), blockPosition());
            Thrall Child = ScarletCourtRegistry.ThrallType.get().create(World);
            if (Child == null) {
                continue;
            }
            Child.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
            Child.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BroodHealth);
            Child.setHealth((float) BroodHealth);
            Child.addTag(CourtFx.Tag);
            Child.setPersistenceRequired();
            Child.rise(CourtMinion.SpawnTicks);
            if (getTarget() != null) {
                Child.setTarget(getTarget());
            }
            World.addFreshEntity(Child);
            ScarletCourt.Instance.adopt(Child);
        }
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public static class Trudge extends CourtGoals.Melee {
        public Trudge(CourtMinion Owner) {
            super(Owner, 1.0, 40, 30);
        }
    }
}
