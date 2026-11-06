package net.rai.scp_unleashed.scp439;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp439Brute extends Scp439Creature implements Enemy {
    public static final float Width = 1.1f;
    public static final float Height = 0.6f;
    public static final float EyeHeight = 0.45f;
    public static final float RenderScale = 1.0f;
    public static final byte ActionBite = 2;
    public static final byte ActionAlarm = 3;
    public static final int BiteTicks = 16;
    public static final int BiteHitTick = 8;
    public static final int AlarmTicks = 24;
    public static final int EmergeTicks = 32;
    public static final int DeathTicks = 32;
    public static final double GuardRadius = 12.0;
    public static final double Leash = 20.0;
    public static final double BiteReach = 1.0;
    public static final int AlarmGap = 200;
    private int AlarmCooldown;

    public Scp439Brute(EntityType<? extends Scp439Brute> Type, Level World) {
        super(Type, World);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Scp439Creature.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp439Goals.Bite(this, 1.3));
        goalSelector.addGoal(3, new Scp439Goals.Homeward(this, 6.0, 1.0));
        goalSelector.addGoal(5, new Scp439Goals.Wander(this, 6, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Scp439Brute.class, Scp439Worker.class));
        targetSelector.addGoal(2, new Scp439Goals.Guard(this));
    }

    public static boolean isIntruder(LivingEntity Found) {
        if (!Found.isAlive() || Found.isRemoved() || isColony(Found) || !Targeting.isTargetable(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Found) || Found instanceof AbstractVillager;
    }

    public boolean guards(LivingEntity Found) {
        return isIntruder(Found) && (Home == null || Found.position().distanceToSqr(homeCentre()) <= GuardRadius * GuardRadius);
    }

    public boolean outOfLeash(LivingEntity Found) {
        return Home != null && Found.position().distanceToSqr(homeCentre()) > Leash * Leash;
    }

    public boolean inBiteReach(LivingEntity Target) {
        return reachTo(Target) <= BiteReach && Math.abs(Target.getY() - getY()) < 2.0;
    }

    public void beginBite(LivingEntity Target) {
        faceToward(Target, 180.0f);
        setAction(ActionBite, BiteTicks);
    }

    public boolean wantsAlarm() {
        return AlarmCooldown <= 0 && canAct();
    }

    public void beginAlarm(LivingEntity Target) {
        faceToward(Target, 180.0f);
        AlarmCooldown = AlarmGap;
        setAction(ActionAlarm, AlarmTicks);
        playLayers(Scp439Registry.BruteAlarm, 1.2f, 1.0f);
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        LivingEntity Before = getTarget();
        super.setTarget(Target);
        if (Target != null && Target != Before && getTarget() == Target && !level().isClientSide && wantsAlarm()) {
            beginAlarm(Target);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = getTarget();
        if (Target != null) {
            faceToward(Target, Kind == ActionAlarm ? 20.0f : 40.0f);
        }
        if (Kind == ActionBite && Elapsed == BiteHitTick && Target != null && reachTo(Target) <= BiteReach + 0.7) {
            playLayers(Scp439Registry.BruteBite, 1.0f, 1.0f);
            if (strike(Target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                Vec3 Push = Target.position().subtract(position()).normalize().scale(0.35);
                Target.push(Push.x, 0.15, Push.z);
                Target.hurtMarked = true;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            AlarmCooldown = Math.max(0, AlarmCooldown - 1);
        }
    }

    @Override
    protected Scp439Stride[] strides() {
        return new Scp439Stride[]{Scp439Gaits.BruteWalk, Scp439Gaits.BruteRun};
    }

    @Override
    protected double runEnter() {
        return 0.12;
    }

    @Override
    protected double runExit() {
        return 0.09;
    }

    @Override
    protected double idleSeconds() {
        return 3.2;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public String modelName() {
        return "scp439_brute";
    }

    @Override
    @Nullable
    protected String clipFor(byte Action) {
        return Action == ActionBite ? "bite" : Action == ActionAlarm ? "alarm" : null;
    }

    @Override
    protected int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    protected int deathTicks() {
        return DeathTicks;
    }

    @Override
    protected byte memberKind() {
        return Scp439Colonies.Brutes;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> idleSounds() {
        return Scp439Registry.BruteIdle;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> hurtSounds() {
        return Scp439Registry.BruteHurt;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathSounds() {
        return Scp439Registry.BruteDeath;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> stepSounds() {
        return Scp439Registry.BruteStep;
    }

    @Override
    protected float stepVolume() {
        return 0.6f;
    }

    @Override
    protected float getSoundVolume() {
        return 1.2f;
    }
}
