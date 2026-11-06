package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp003Larva extends Scp003Creature {
    public static final int MinGrowth = 2400;
    public static final int MaxGrowth = 2400;
    public static final int MoultTicks = 90;
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation MoultHold = RawAnimation.begin().thenPlayAndHold("moult");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private int Growth;
    private int GrowthTarget;

    public Scp003Larva(EntityType<? extends Scp003Larva> Type, Level World) {
        super(Type, World);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    public int stage() {
        return 1;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp003Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp003Goals.FleePlayers(this, 10.0, 1.0, 1.45));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75, 0.002f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public float growthFraction() {
        return GrowthTarget <= 0 ? 0.0f : Mth.clamp((float) Growth / GrowthTarget, 0.0f, 1.0f);
    }

    public void growNow() {
        if (!isActing() || action() == ActionEmerge) {
            Growth = Math.max(Growth, GrowthTarget);
            startMoult();
        }
    }

    @Override
    protected void serverTick() {
        if (GrowthTarget <= 0) {
            GrowthTarget = MinGrowth + random.nextInt(MaxGrowth - MinGrowth + 1);
        }
        if (isActing()) {
            return;
        }
        Growth++;
        if (Growth >= GrowthTarget) {
            startMoult();
        }
    }

    private void startMoult() {
        setAction(ActionTransform, MoultTicks);
        playSound(Scp003Registry.Moult.get(), 0.8f, 1.0f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionTransform && level() instanceof ServerLevel World && Remaining % 6 == 0) {
            World.sendParticles(Scp003Registry.Flesh.get(), getX(), getY() + 0.2, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
            if (Remaining % 30 == 0) {
                playSound(Scp003Registry.Moult.get(), 0.6f, 0.9f + random.nextFloat() * 0.2f);
            }
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind != ActionTransform || !(level() instanceof ServerLevel World)) {
            return;
        }
        World.playSound(null, getX(), getY(), getZ(), Scp003Registry.MoultBurst.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        World.sendParticles(Scp003Registry.Flesh.get(), getX(), getY() + 0.3, getZ(), 18, 0.3, 0.2, 0.3, 0.1);
        Scp003Growth.transform(this, Scp003Registry.Crawler.get(), ActionEmerge, EmergeTicks);
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        float Dark = -World.getPathfindingCostFromLightLevels(Pos);
        return World.canSeeSky(Pos) ? Dark : Dark + 0.5f;
    }

    @Override
    public void writeJade(CompoundTag Data) {
        Data.putFloat("Growth", isActing() && action() == ActionTransform ? 1.0f : growthFraction());
        Data.putBoolean("Moulting", action() == ActionTransform);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp003Registry.LarvaAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp003Registry.LarvaHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp003Registry.LarvaDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        playSound(Scp003Registry.LarvaStep.get(), 0.15f, 1.0f);
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Growth", Growth);
        Tag.putInt("GrowthTarget", GrowthTarget);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Growth = Tag.getInt("Growth");
        GrowthTarget = Math.min(Tag.getInt("GrowthTarget"), MaxGrowth);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionTransform) {
                return Event.setAndContinue(MoultHold);
            }
            return Event.setAndContinue(Event.isMoving() ? CrawlLoop : IdleLoop);
        }).setAnimationSpeedHandler(Creature -> Creature.gaitSpeed(0.03)));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", AttackOnce));
    }
}
