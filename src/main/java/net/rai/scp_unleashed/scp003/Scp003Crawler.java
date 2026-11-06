package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp003Crawler extends Scp003Creature {
    public static final int KillsToGrow = 5;
    public static final int MetamorphTicks = 70;
    public static final Scp003Gait.Profile CrawlerGait = new Scp003Gait.Profile(15.0, 0.1111, 0.4, 3.0, 4,
            new double[]{0.991, 0.142, 0.241, 0.392, 0.491, 0.642, 0.741, 0.892}, new float[]{1.0f, 0.4f, 1.0f, 0.4f, 1.0f, 0.4f, 1.0f, 0.4f},
            Scp003Registry.CrawlerStep, Scp003Registry.CrawlerWet, null, Scp003Registry.CrawlerWet, 0.4f, 0.27f, 0.0f, 0.2f, 1.0f, 0.0f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation MetamorphHold = RawAnimation.begin().thenPlayAndHold("metamorph");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");

    public Scp003Crawler(EntityType<? extends Scp003Crawler> Type, Level World) {
        super(Type, World);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.29).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean isPrey(LivingEntity Target) {
        return Target instanceof Mob && !(Target instanceof Scp003Creature) && !(Target instanceof Creeper) && !(Target instanceof StrangerEntity) && Target.isAlive();
    }

    @Override
    public int stage() {
        return 2;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp003Goals.HoldStill(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, Scp003Crawler::isPrey));
    }

    public void growNow() {
        if (!isActing() || action() == ActionEmerge) {
            Kills = Math.max(Kills, KillsToGrow);
            startMetamorph();
        }
    }

    @Override
    protected void serverTick() {
        if (!isActing() && Kills >= KillsToGrow) {
            startMetamorph();
        }
    }

    private void startMetamorph() {
        setTarget(null);
        setAction(ActionTransform, MetamorphTicks);
        playSound(Scp003Registry.Moult.get(), 1.0f, 0.75f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionTransform && level() instanceof ServerLevel World && Remaining % 5 == 0) {
            World.sendParticles(Scp003Registry.Flesh.get(), getX(), getY() + 0.5, getZ(), 3, 0.35, 0.3, 0.35, 0.03);
            if (Remaining % 25 == 0) {
                playSound(Scp003Registry.Moult.get(), 0.8f, 0.7f + random.nextFloat() * 0.2f);
            }
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind != ActionTransform || !(level() instanceof ServerLevel World)) {
            return;
        }
        World.playSound(null, getX(), getY(), getZ(), Scp003Registry.MoultBurst.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
        World.sendParticles(Scp003Registry.Flesh.get(), getX(), getY() + 0.6, getZ(), 26, 0.4, 0.4, 0.4, 0.12);
        Scp003Growth.transform(this, Scp003Registry.Host.get(), ActionEmerge, EmergeTicks + 10);
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            playSound(Scp003Registry.CrawlerAttack.get(), 0.9f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Hit;
    }

    @Override
    public void writeJade(CompoundTag Data) {
        Data.putInt("Kills", Math.min(Kills, KillsToGrow));
        Data.putInt("KillsNeeded", KillsToGrow);
        Data.putBoolean("Moulting", action() == ActionTransform);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp003Registry.CrawlerAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp003Registry.CrawlerHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp003Registry.CrawlerDeath.get();
    }

    @Override
    protected Scp003Gait.Profile gaitProfile() {
        return CrawlerGait;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp003GaitController<>(this, "body", 4, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionTransform) {
                return Event.setAndContinue(MetamorphHold);
            }
            return Event.setAndContinue(isStriding() ? WalkLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", AttackOnce));
    }
}
