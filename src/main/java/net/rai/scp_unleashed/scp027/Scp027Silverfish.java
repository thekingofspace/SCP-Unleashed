package net.rai.scp_unleashed.scp027;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

public class Scp027Silverfish extends Silverfish implements Scp027Vermin {
    private final Scp027Leash Leash = new Scp027Leash();

    public Scp027Silverfish(EntityType<? extends Scp027Silverfish> Type, Level World) {
        super(Type, World);
        xpReward = 0;
    }

    @Override
    public Scp027Leash leash() {
        return Leash;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Scp027Goals.Flee(this, this, 1.4));
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, level()));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(4, new Scp027Goals.Heel(this, this, 1.15));
        goalSelector.addGoal(5, new Scp027Goals.Mill(this, this, 0.8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new Scp027Goals.PreyTarget(this, this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Leash.tick(this);
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return Distance > 48.0 * 48.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Leash.save(Saved);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Leash.load(Saved);
    }
}
