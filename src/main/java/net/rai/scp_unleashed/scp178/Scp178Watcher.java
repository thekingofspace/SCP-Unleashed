package net.rai.scp_unleashed.scp178;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Scp178Watcher extends Mob implements GeoEntity {
    public static final float Width = 0.6f;
    public static final float Height = 2.8f;
    public static final float EyeHeight = 2.6f;
    public static final double MaxHealth = 30.0;
    public static final int FadeTicks = 20;
    public static final int DeathTicks = 24;
    public static final float TurnRate = 9.0f;
    public static final float PresenceStep = 0.1f;
    public static final byte ActionNone = 0;
    public static final byte ActionWindup = 1;
    public static final byte ActionLunge = 2;
    public static final byte ActionRecover = 3;
    public static final byte StrikeCue = 71;
    public static final byte HurtCue = 72;
    public static final byte TurnCue = 73;
    public static final int LungeTicks = 12;
    public static final int RecoverTicks = 16;
    public static final String[] ActionClips = {"", "windup", "lunge", "recover"};
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation TendrilLoop = RawAnimation.begin().thenLoop("tendrils");
    public static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    public static final RawAnimation[] ActionPlays = {null, RawAnimation.begin().thenPlayAndHold("windup"), RawAnimation.begin().thenPlayAndHold("lunge"),
            RawAnimation.begin().thenPlayAndHold("recover")};
    public static Predicate<Scp178Watcher> ClientSight = Watcher -> false;
    public static Consumer<Scp178Watcher> ClientTick = Watcher -> {
    };

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int PhantomId;
    private boolean Hostile;
    private byte Action = ActionNone;
    private int ActionStart;
    private int LeaveStart = -1;
    private int DeathStart = -1;
    private float RenderPartial;
    @Nullable
    private Scp178Gait Gait;
    private float FacingYaw;
    private float HeadYaw;
    private float Presence;
    private float PresenceO;
    private float PresenceGoal = 1.0f;

    public Scp178Watcher(EntityType<? extends Scp178Watcher> Type, Level World) {
        super(Type, World);
        setNoAi(true);
        setSilent(true);
        noPhysics = true;
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    public int phantomId() {
        return PhantomId;
    }

    public void bind(int Id) {
        PhantomId = Id;
    }

    public boolean isHostile() {
        return Hostile;
    }

    public void setHostile(boolean Value) {
        Hostile = Value;
    }

    public boolean isLeaving() {
        return LeaveStart >= 0;
    }

    public boolean isFading() {
        return LeaveStart >= 0 || DeathStart >= 0;
    }

    public boolean fadeDone() {
        return LeaveStart >= 0 && tickCount - LeaveStart >= FadeTicks || DeathStart >= 0 && tickCount - DeathStart >= DeathTicks;
    }

    public void leave() {
        if (!isFading()) {
            LeaveStart = tickCount;
            Action = ActionNone;
        }
    }

    public void vanish() {
        if (DeathStart < 0) {
            DeathStart = tickCount;
            LeaveStart = -1;
            Action = ActionNone;
        }
    }

    public void flinch() {
        hurtTime = 10;
        hurtDuration = 10;
    }

    public void startAction(byte Kind) {
        if (isFading()) {
            return;
        }
        Action = Kind;
        ActionStart = tickCount;
    }

    public byte action() {
        return Action;
    }

    public void face(float BodyYaw, float LookYaw, float Pitch) {
        FacingYaw = BodyYaw;
        HeadYaw = LookYaw;
        setXRot(Mth.clamp(Pitch, -35.0f, 35.0f));
    }

    public void fadeOut() {
        PresenceGoal = 0.0f;
    }

    public void fadeIn() {
        PresenceGoal = 1.0f;
    }

    public boolean absent() {
        return Presence <= 0.0f && PresenceGoal <= 0.0f;
    }

    public float presence(float Partial) {
        return Mth.lerp(Partial, PresenceO, Presence);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        if (Gait != null) {
            Gait.setPartial(Partial);
        }
    }

    @Nullable
    public Scp178Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp178Gait();
        }
        return Gait;
    }

    public double actionElapsed() {
        return Math.max(0.0, tickCount - ActionStart + RenderPartial);
    }

    @Nullable
    public String actionClip() {
        return Action > ActionNone && Action < ActionClips.length ? ActionClips[Action] : null;
    }

    public float leavingShare(float Partial) {
        return LeaveStart < 0 ? 0.0f : Mth.clamp((tickCount - LeaveStart + Partial) / FadeTicks, 0.0f, 1.0f);
    }

    public float deathShare(float Partial) {
        return DeathStart < 0 ? 0.0f : Mth.clamp((tickCount - DeathStart + Partial) / DeathTicks, 0.0f, 1.0f);
    }

    public RawAnimation bodyAnimation() {
        if (DeathStart >= 0) {
            return DeathPlay;
        }
        if (Action > ActionNone && Action < ActionPlays.length) {
            return ActionPlays[Action];
        }
        Scp178Gait Walking = gait();
        return Walking != null && Walking.striding() ? WalkLoop : IdleLoop;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            discard();
            return;
        }
        Scp178Gait Walking = gait();
        if (Walking != null) {
            Walking.tick(this);
        }
        setOldPosAndRot();
        yBodyRotO = yBodyRot;
        yHeadRotO = yHeadRot;
        PresenceO = Presence;
        Presence = PresenceGoal > Presence ? Math.min(PresenceGoal, Presence + PresenceStep) : Math.max(PresenceGoal, Presence - PresenceStep);
        if (hurtTime > 0) {
            hurtTime--;
        }
        if (Action == ActionLunge && tickCount - ActionStart >= LungeTicks) {
            startAction(ActionRecover);
        } else if (Action == ActionRecover && tickCount - ActionStart >= RecoverTicks) {
            Action = ActionNone;
        }
        yBodyRot = Mth.approachDegrees(yBodyRot, FacingYaw, TurnRate);
        setYRot(yBodyRot);
        yHeadRot = Mth.approachDegrees(yHeadRot, HeadYaw, TurnRate * 2.0f);
        ClientTick.accept(this);
    }

    @Override
    public boolean isPickable() {
        return level().isClientSide && !isRemoved() && !isFading() && Presence > 0.5f && ClientSight.test(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity Other) {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp178WatcherController(this, "body", 4, Event -> {
            Event.getController().transitionLength(Scp178WatcherController.blendFor(action()));
            return Event.setAndContinue(bodyAnimation());
        }));
        Controllers.add(new AnimationController<>(this, "tendrils", 0, Event -> Event.setAndContinue(TendrilLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
