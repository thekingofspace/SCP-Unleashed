package net.rai.scp_unleashed.scp215;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Supplier;

public class Scp215Phantom extends Entity implements GeoEntity {
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation ApproachLoop = RawAnimation.begin().thenLoop("approach");
    public static final RawAnimation VanishHold = RawAnimation.begin().thenPlayAndHold("vanish");
    public static final int FadeIn = 16;
    public static final int VanishLength = 12;
    public static final int FadeStart = 3;
    public static final double DropDistance = 44.0;
    public static final double ViewCone = 60.0;
    public static final double CrawlSpeed = 0.14;
    public static final float HeadLimit = 70.0f;
    public static Supplier<Player> ClientViewer = () -> null;

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private Scp215Variant Kind = Scp215Variant.Lurker;
    private boolean Puppet;
    private boolean Moving;
    private int Age;
    private int Lifetime = Scp215Variant.Lurker.Lifetime;
    private int Stare;
    private int Timer = 20;
    private int Moves;
    private int MoveHold;
    private int Vanishing = -1;
    private int Twitch;
    private int RustleCooldown;
    private double StepX;
    private double StepZ;
    private float TwitchYaw;
    private float BodyYaw;
    private float BodyYawO;
    private float HeadYaw;
    private float HeadYawO;
    private float HeadPitch;
    private float HeadPitchO;

    public Scp215Phantom(EntityType<? extends Scp215Phantom> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
    }

    public void setup(Scp215Variant Chosen, int Life, float Yaw) {
        Kind = Chosen;
        Lifetime = Life;
        Timer = 20 + random.nextInt(30);
        BodyYaw = Yaw;
        BodyYawO = Yaw;
        HeadYaw = Yaw;
        HeadYawO = Yaw;
        setYRot(Yaw);
        yRotO = Yaw;
        refreshDimensions();
    }

    public void becomePuppet(Scp215Variant Chosen) {
        Kind = Chosen;
        Puppet = true;
        refreshDimensions();
    }

    public void mirror(LivingEntity Source) {
        xo = Source.xo;
        yo = Source.yo;
        zo = Source.zo;
        xOld = Source.xOld;
        yOld = Source.yOld;
        zOld = Source.zOld;
        setPos(Source.getX(), Source.getY(), Source.getZ());
        BodyYawO = Source.yBodyRotO;
        BodyYaw = Source.yBodyRot;
        HeadYawO = Source.yHeadRotO;
        HeadYaw = Source.yHeadRot;
        HeadPitchO = Source.xRotO;
        HeadPitch = Source.getXRot();
        yRotO = BodyYawO;
        setYRot(BodyYaw);
        Moving = Source.walkAnimation.speed() > 0.12f;
        tickCount = Source.tickCount;
    }

    public Scp215Variant variant() {
        return Kind;
    }

    public boolean isPuppet() {
        return Puppet;
    }

    public boolean isMoving() {
        return Moving;
    }

    public boolean isVanishing() {
        return Vanishing >= 0;
    }

    public float bodyYaw(float Partial) {
        return Mth.rotLerp(Partial, BodyYawO, BodyYaw);
    }

    public float netHeadYaw(float Partial) {
        float Net = Mth.wrapDegrees(Mth.rotLerp(Partial, HeadYawO, HeadYaw) - bodyYaw(Partial));
        Net = Mth.clamp(Net, -HeadLimit, HeadLimit);
        return Twitch > 0 ? Net + TwitchYaw : Net;
    }

    public float headPitch(float Partial) {
        return Mth.lerp(Partial, HeadPitchO, HeadPitch);
    }

    public float fade(float Partial) {
        if (Puppet) {
            return 1.0f;
        }
        float In = Mth.clamp((Age + Partial) / FadeIn, 0.0f, 1.0f);
        if (Vanishing < 0) {
            return In;
        }
        float Out = 1.0f - Mth.clamp((Vanishing + Partial - FadeStart) / (VanishLength - FadeStart), 0.0f, 1.0f);
        return Math.min(In, Out);
    }

    public boolean flickerOff() {
        return !Puppet && Vanishing > FadeStart && Vanishing % 3 != 0;
    }

    public Vec3 focus() {
        return position().add(0.0, Kind.Focus, 0.0);
    }

    @Override
    public EntityDimensions getDimensions(Pose Stance) {
        return Kind.dimensions();
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            discard();
            return;
        }
        if (Puppet) {
            return;
        }
        BodyYawO = BodyYaw;
        HeadYawO = HeadYaw;
        HeadPitchO = HeadPitch;
        Player Viewer = ClientViewer.get();
        if (Viewer == null || Viewer.level() != level() || Viewer.isSpectator() || !Scp215GlassesItem.isWorn(Viewer)) {
            discard();
            return;
        }
        Age++;
        if (Twitch > 0) {
            Twitch--;
        }
        if (RustleCooldown > 0) {
            RustleCooldown--;
        }
        if (Vanishing >= 0) {
            Vanishing++;
            Moving = false;
            if (Vanishing >= VanishLength) {
                discard();
            }
            return;
        }
        Vec3 Eye = Viewer.getEyePosition();
        Vec3 Toward = focus().subtract(Eye);
        double Distance = Toward.length();
        if (Distance > DropDistance) {
            discard();
            return;
        }
        boolean Clear = Scp215Ground.sees(level(), Eye, focus(), Viewer);
        double Angle = Distance < 1.0E-4 ? 0.0 : Math.toDegrees(Math.acos(Mth.clamp(Viewer.getViewVector(1.0f).dot(Toward.scale(1.0 / Distance)), -1.0, 1.0)));
        boolean InView = Clear && Angle < ViewCone;
        boolean Watched = Clear && Angle < Kind.WatchCone;
        Stare = Watched ? Stare + 1 : Math.max(0, Stare - 2);
        face(Eye);
        if (Distance < Kind.VanishDistance || Stare >= Kind.StareLimit || Age > Lifetime && InView) {
            vanish();
            return;
        }
        if (Age > Lifetime) {
            discard();
            return;
        }
        switch (Kind) {
            case Lurker -> lurk(Eye, Watched);
            case Crawler -> crawl(Eye, Watched);
            case Watcher -> watch(InView);
        }
    }

    private void face(Vec3 Eye) {
        double Dx = Eye.x - getX();
        double Dz = Eye.z - getZ();
        double Dy = Eye.y - (getY() + Kind.Focus);
        float Target = Scp215Ground.yawToward(Dx, Dz);
        float Pitch = Mth.clamp(Scp215Ground.pitchToward(Dx, Dy, Dz), -40.0f, 50.0f);
        switch (Kind) {
            case Lurker -> {
                HeadYaw = Scp215Ground.approach(HeadYaw, Target, 9.0f);
                if (Math.abs(Mth.wrapDegrees(HeadYaw - BodyYaw)) > 55.0f) {
                    BodyYaw = Scp215Ground.approach(BodyYaw, Target, 30.0f);
                }
                if (Twitch == 0 && random.nextFloat() < 0.02f) {
                    Twitch = 4;
                    TwitchYaw = (18.0f + random.nextFloat() * 16.0f) * (random.nextBoolean() ? 1.0f : -1.0f);
                }
            }
            case Crawler -> {
                BodyYaw = Scp215Ground.approach(BodyYaw, Target, 9.0f);
                HeadYaw = Scp215Ground.approach(HeadYaw, Target, 14.0f);
            }
            case Watcher -> {
                BodyYaw = Scp215Ground.approach(BodyYaw, Target, 1.2f);
                HeadYaw = Scp215Ground.approach(HeadYaw, Target, 4.5f);
            }
        }
        float Net = Mth.wrapDegrees(HeadYaw - BodyYaw);
        if (Math.abs(Net) > HeadLimit) {
            HeadYaw = BodyYaw + Math.copySign(HeadLimit, Net);
        }
        HeadPitch = Scp215Ground.approach(HeadPitch, Pitch, 6.0f);
        setYRot(BodyYaw);
    }

    private void lurk(Vec3 Eye, boolean Watched) {
        if (Moves > 0) {
            Moves--;
            if (!stepBy(StepX, StepZ)) {
                Moves = 0;
            }
            MoveHold = 10;
            Moving = true;
            return;
        }
        if (MoveHold > 0) {
            MoveHold--;
        }
        Moving = MoveHold > 0;
        if (Watched) {
            Timer = Math.max(Timer, 12);
            return;
        }
        if (--Timer > 0) {
            return;
        }
        Timer = 26 + random.nextInt(40);
        Vec3 Ahead = flatToward(Eye);
        double Hop = 0.7 + random.nextDouble() * 0.6;
        StepX = Ahead.x * Hop / 2.0;
        StepZ = Ahead.z * Hop / 2.0;
        Moves = 2;
        BodyYaw = Scp215Ground.yawToward(Ahead.x, Ahead.z);
        if (random.nextFloat() < 0.35f) {
            sound(Scp215Registry.ClickLayers, 0.55f);
        }
    }

    private void crawl(Vec3 Eye, boolean Watched) {
        if (Moves > 0) {
            if (Watched && random.nextFloat() < 0.25f) {
                Moves = 0;
                Moving = false;
                return;
            }
            Moves--;
            Vec3 Ahead = flatToward(Eye).yRot((float) Math.sin(Age * 0.45) * 0.35f);
            if (!stepBy(Ahead.x * CrawlSpeed, Ahead.z * CrawlSpeed)) {
                Moves = 0;
            }
            Moving = Moves > 0;
            if (Moves % 7 == 3) {
                sound(Scp215Registry.ScuttleLayers, 0.45f);
            }
            return;
        }
        Moving = false;
        if (Watched) {
            Timer = Math.max(Timer, 10);
            return;
        }
        if (--Timer > 0) {
            return;
        }
        Moves = 10 + random.nextInt(14);
        Timer = 30 + random.nextInt(40);
    }

    private void watch(boolean InView) {
        Moving = false;
        if (InView && RustleCooldown == 0 && Math.abs(Mth.wrapDegrees(BodyYaw - BodyYawO)) > 0.6f && random.nextFloat() < 0.03f) {
            RustleCooldown = 240;
            sound(Scp215Registry.RustleLayers, 0.5f);
        }
    }

    private Vec3 flatToward(Vec3 Eye) {
        Vec3 Flat = new Vec3(Eye.x - getX(), 0.0, Eye.z - getZ());
        double Length = Flat.length();
        return Length < 1.0E-4 ? Vec3.ZERO : Flat.scale(1.0 / Length);
    }

    private boolean stepBy(double Dx, double Dz) {
        for (float Turn : new float[]{0.0f, 0.7f, -0.7f}) {
            Vec3 Step = new Vec3(Dx, 0.0, Dz).yRot(Turn);
            double Y = Scp215Ground.ground(level(), getX() + Step.x, getY(), getZ() + Step.z, Kind.clearance(), 1, 3);
            if (!Double.isNaN(Y)) {
                setPos(getX() + Step.x, Y, getZ() + Step.z);
                return true;
            }
        }
        return false;
    }

    public void vanish() {
        if (Vanishing >= 0) {
            return;
        }
        Vanishing = 0;
        Moving = false;
        Moves = 0;
        sound(Scp215Registry.VanishLayers, 0.7f);
    }

    public void sound(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.9f + random.nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playLocalSound(getX(), getY() + Kind.Focus, getZ(), Layer.get(), SoundSource.AMBIENT, Volume, Pitch, false);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 3, State -> {
            if (Vanishing >= 0) {
                return State.setAndContinue(VanishHold);
            }
            return State.setAndContinue(Moving ? ApproachLoop : IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
