package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class AshWraith extends CourtMinion {
    public static final int VisibleTicks = 100;
    public static final int PhasedTicks = 50;
    private static final EntityDataAccessor<Boolean> PhasedData = SynchedEntityData.defineId(AshWraith.class, EntityDataSerializers.BOOLEAN);
    private int Cycle;

    public AshWraith(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.AshWraith);
        Cycle = World.random.nextInt(VisibleTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PhasedData, false);
    }

    public boolean phased() {
        return entityData.get(PhasedData);
    }

    public void phase(boolean Value) {
        if (phased() == Value) {
            return;
        }
        entityData.set(PhasedData, Value);
        Cycle = 0;
        play("phase", 1.0f, Value ? 0.8f : 1.2f);
        if (level() instanceof ServerLevel World) {
            CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, position().add(0.0, 1.0, 0.0), 10, 0.3, 0.02);
            CourtFx.burst(World, CourtFx.Ash, position().add(0.0, 1.0, 0.0), 8, 0.35, 0.0);
        }
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Drift(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Cycle++;
        if (!phased() && Cycle >= VisibleTicks && !busy()) {
            phase(true);
        } else if (phased() && Cycle >= PhasedTicks) {
            phase(false);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (phased() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return phased() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    protected int hitTick() {
        return 8;
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public static class Drift extends CourtGoals.Melee {
        private final AshWraith Wraith;

        public Drift(AshWraith Owner) {
            super(Owner, 1.1, 24, 16);
            Wraith = Owner;
        }

        @Override
        public void tick() {
            if (Wraith.phased()) {
                net.minecraft.world.entity.LivingEntity Target = Wraith.getTarget();
                if (Target != null && Wraith.tickCount % 10 == 0) {
                    Wraith.getNavigation().moveTo(Target, 1.4);
                }
                return;
            }
            super.tick();
        }
    }
}
