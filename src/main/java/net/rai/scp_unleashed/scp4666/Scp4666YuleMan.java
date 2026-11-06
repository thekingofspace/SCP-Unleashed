package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp4666YuleMan extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.7f;
    public static final float Height = 2.45f;
    public static final float EyeHeight = 2.25f;
    public static final int ModeIdle = 0;
    public static final int ModeStalk = 1;
    public static final int ModeFlee = 2;
    public static final int ModeStare = 3;
    public static final int ModeSink = 4;
    public static final int ModePeek = 5;
    public static final int ModeLoom = 6;
    public static final int ModeDrag = 7;
    public static final int ModeBeat = 8;
    public static final int ModeLift = 9;
    public static final double StalkSpeed = 0.045;
    public static final double FleeSpeed = 0.3;
    public static final int StalkLife = 2400;
    public static final int FleeTicks = 34;
    public static final int StareTicks = 140;
    public static final int StareSeen = 22;
    public static final int SinkTicks = 34;
    public static final int PeekTicks = 40;
    public static final double SeeReach = 34.0;
    public static final double SeeCone = 0.94;
    public static final double WalkDistance = 1.44;
    public static final double RunDistance = 3.6;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.006, 0.16, 0.12, WalkDistance, RunDistance, 0.25, 3);
    public static final double[] Contacts = {0.0, 0.5};
    public static final float StepVolume = 0.55f;
    private static final EntityDataAccessor<Integer> ModeData = SynchedEntityData.defineId(Scp4666YuleMan.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation StareLoop = RawAnimation.begin().thenLoop("stare");
    private static final RawAnimation SinkPlay = RawAnimation.begin().thenPlayAndHold("sink");
    private static final RawAnimation PeekPlay = RawAnimation.begin().thenPlayAndHold("peek");
    private static final RawAnimation LoomLoop = RawAnimation.begin().thenLoop("loom");
    private static final RawAnimation DragLoop = RawAnimation.begin().thenLoop("drag");
    private static final RawAnimation BeatLoop = RawAnimation.begin().thenLoop("beat");
    private static final RawAnimation LiftPlay = RawAnimation.begin().thenPlayAndHold("lift");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private final Map<UUID, Integer> Watching = new HashMap<>();
    private final Set<UUID> Counted = new HashSet<>();
    private int ModeTicks;
    private double Angle;
    private boolean Clockwise;
    private Vec3 FleeHeading = Vec3.ZERO;
    @Nullable
    private UUID Focus;
    @Nullable
    private Scp4666Map.Window Window;
    private float RenderPartial;

    public Scp4666YuleMan(EntityType<? extends Scp4666YuleMan> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
        noPhysics = true;
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public static boolean present(ServerLevel World) {
        for (Entity Found : World.getAllEntities()) {
            if (Found instanceof Scp4666YuleMan Man && Man.isAlive() && Man.mode() != ModeIdle) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeIdle);
    }

    public int mode() {
        return entityData.get(ModeData);
    }

    private void setMode(int Mode) {
        entityData.set(ModeData, Mode);
        ModeTicks = 0;
    }

    public int modeTicks() {
        return ModeTicks;
    }

    public void stalk(double StartAngle, boolean Turn) {
        Angle = StartAngle;
        Clockwise = Turn;
        setMode(ModeStalk);
    }

    public void stare(Scp4666Map.Window Glass) {
        Window = Glass;
        setMode(ModeStare);
    }

    public void peek(Player Below) {
        Focus = Below.getUUID();
        setMode(ModePeek);
    }

    public void loom(Player Victim) {
        Focus = Victim.getUUID();
        setMode(ModeLoom);
    }

    public void loomIdle() {
        Focus = null;
        setMode(ModeLoom);
    }

    public void drag(Player Victim) {
        Focus = Victim.getUUID();
        setMode(ModeDrag);
    }

    public void beat(Player Victim) {
        Focus = Victim.getUUID();
        setMode(ModeBeat);
    }

    public void lift(Player Victim) {
        Focus = Victim.getUUID();
        setMode(ModeLift);
    }

    public void faceToward(Vec3 Target) {
        float Yaw = (float) (Mth.atan2(Target.z - getZ(), Target.x - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    @Override
    public void tick() {
        super.tick();
        ModeTicks++;
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        switch (mode()) {
            case ModeStalk -> tickStalk(World);
            case ModeFlee -> tickFlee(World);
            case ModeStare -> tickStare(World);
            case ModeSink -> {
                if (ModeTicks >= SinkTicks) {
                    discard();
                }
            }
            case ModePeek -> {
                lookAtFocus(World);
                if (ModeTicks >= PeekTicks) {
                    discard();
                }
            }
            case ModeLoom, ModeDrag, ModeBeat, ModeLift -> lookAtFocus(World);
            default -> tickIdle(World);
        }
    }

    private void clientTick() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && (mode() == ModeStalk || mode() == ModeFlee || mode() == ModeDrag || mode() == ModeIdle), Gait);
        if (!Moving || isSilent()) {
            return;
        }
        BlockPos Under = blockPosition();
        boolean Snowy = level().getBlockState(Under).is(BlockTags.SNOW) || level().getBlockState(Under.below()).is(BlockTags.SNOW);
        SoundEvent Step = Snowy ? Scp4666Registry.YuleStep.get() : Scp4666Registry.YuleStepWood.get();
        for (double Contact : Contacts) {
            if (Math.floor(Stride.to() - Contact) > Math.floor(Stride.from() - Contact)) {
                level().playLocalSound(getX(), getY(), getZ(), Step, SoundSource.HOSTILE, StepVolume * (0.85f + random.nextFloat() * 0.15f),
                        (mode() == ModeFlee ? 1.1f : 0.8f) * (0.94f + random.nextFloat() * 0.12f), false);
            }
        }
    }

    private void tickStalk(ServerLevel World) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        double Step = StalkSpeed / Scp4666World.StalkRadius * (Clockwise ? 1.0 : -1.0);
        Angle += Step;
        Vec3 Next = Scp4666Director.ringPoint(Layout, Angle, Scp4666World.StalkRadius);
        Vec3 Ahead = Scp4666Director.ringPoint(Layout, Angle + Step * 20.0, Scp4666World.StalkRadius);
        moveTo(Next.x, Next.y, Next.z, getYRot(), 0.0f);
        faceToward(Ahead);
        if (ModeTicks % 4 == 0) {
            ServerPlayer Seen = seenBy(World, false);
            if (Seen != null) {
                flee(World, Layout);
                return;
            }
        }
        if (ModeTicks >= StalkLife) {
            discard();
        }
    }

    private void flee(ServerLevel World, Scp4666Map Layout) {
        Vec3 Centre = Vec3.atBottomCenterOf(Layout.Centre);
        Vec3 Out = new Vec3(getX() - Centre.x, 0.0, getZ() - Centre.z);
        FleeHeading = Out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : Out.normalize();
        setMode(ModeFlee);
        World.playSound(null, getX(), getY(), getZ(), Scp4666Registry.YuleFlee.get(), SoundSource.HOSTILE, 1.6f, 0.9f);
        World.playSound(null, getX(), getY(), getZ(), Scp4666Registry.YuleRustle.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
        if (World.getRandom().nextFloat() < 0.6f) {
            World.playSound(null, getX(), getY(), getZ(), Scp4666Registry.Laugh.get(), SoundSource.HOSTILE, 1.3f, 0.92f + World.getRandom().nextFloat() * 0.08f);
        }
    }

    private void tickFlee(ServerLevel World) {
        Vec3 Next = position().add(FleeHeading.scale(FleeSpeed));
        moveTo(Next.x, getY(), Next.z, getYRot(), 0.0f);
        faceToward(Next.add(FleeHeading));
        if (ModeTicks >= FleeTicks) {
            discard();
        }
    }

    private void tickStare(ServerLevel World) {
        ServerPlayer Nearest = null;
        double Best = Double.MAX_VALUE;
        for (ServerPlayer User : World.players()) {
            double Distance = User.distanceToSqr(this);
            if (User.isAlive() && !User.isSpectator() && Distance < Best) {
                Best = Distance;
                Nearest = User;
            }
        }
        if (Nearest != null) {
            getLookControl().setLookAt(Nearest, 20.0f, 20.0f);
            setYHeadRot(rotationToward(Nearest.getEyePosition()));
        }
        ServerPlayer Seen = seenBy(World, true);
        boolean LongLook = false;
        for (Map.Entry<UUID, Integer> Entry : Watching.entrySet()) {
            if (Entry.getValue() >= StareSeen) {
                LongLook = true;
            }
        }
        if (ModeTicks >= StareTicks || LongLook || Seen != null && ModeTicks > StareTicks / 2) {
            setMode(ModeSink);
            World.playSound(null, getX(), getY(), getZ(), Scp4666Registry.YuleRustle.get(), SoundSource.HOSTILE, 0.5f, 0.6f);
            World.playSound(null, getX(), getEyeY(), getZ(), Scp4666Registry.Giggle.get(), SoundSource.HOSTILE, 0.55f, 0.95f + random.nextFloat() * 0.1f);
        }
    }

    private float rotationToward(Vec3 Target) {
        return (float) (Mth.atan2(Target.z - getZ(), Target.x - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private void tickIdle(ServerLevel World) {
        Player Nearest = World.getNearestPlayer(this, 24.0);
        if (Nearest != null && !Scp4666World.inside(World)) {
            getLookControl().setLookAt(Nearest, 6.0f, 10.0f);
            float Wanted = rotationToward(Nearest.position());
            float Turn = Mth.approachDegrees(getYRot(), Wanted, 1.5f);
            setYRot(Turn);
            setYBodyRot(Turn);
        }
        if (Scp4666World.inside(World) && ModeTicks > StalkLife) {
            discard();
        }
    }

    private void lookAtFocus(ServerLevel World) {
        if (Focus == null) {
            return;
        }
        Entity Target = World.getEntity(Focus);
        if (Target instanceof LivingEntity Living) {
            getLookControl().setLookAt(Living, 30.0f, 30.0f);
            setYHeadRot(rotationToward(Living.getEyePosition()));
            if (mode() != ModeDrag) {
                faceToward(Living.position());
            }
        }
    }

    @Nullable
    private ServerPlayer seenBy(ServerLevel World, boolean AtWindow) {
        ServerPlayer Found = null;
        Vec3 Head = getEyePosition();
        Vec3 Middle = position().add(0.0, Height * 0.55, 0.0);
        for (ServerPlayer User : World.players()) {
            if (!User.isAlive() || User.isSpectator() || User.isSleeping()) {
                continue;
            }
            Vec3 Eye = User.getEyePosition();
            double Distance = Eye.distanceTo(Middle);
            boolean Looking = false;
            if (Distance < SeeReach) {
                Vec3 Toward = Middle.subtract(Eye).normalize();
                double Dot = User.getViewVector(1.0f).dot(Toward);
                if (Dot > SeeCone && (clear(World, Eye, Head, User) || clear(World, Eye, Middle, User))) {
                    Looking = true;
                }
            }
            if (Looking) {
                int Count = Watching.merge(User.getUUID(), AtWindow ? 1 : 4, Integer::sum);
                if (Counted.add(User.getUUID())) {
                    Scp4666Director.saw(User, AtWindow);
                }
                if (Count > 0) {
                    Found = User;
                }
            } else if (AtWindow) {
                Watching.remove(User.getUUID());
            }
        }
        return Found;
    }

    private boolean clear(ServerLevel World, Vec3 From, Vec3 To, Entity Viewer) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !Scp4666World.inside(this) && super.shouldBeSaved();
    }

    @Override
    public boolean isPickable() {
        return mode() != ModeSink;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Mode", mode() == ModeIdle ? ModeIdle : ModeIdle);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setMode(ModeIdle);
    }

    public StrideGait stride() {
        return Stride;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public boolean striding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public RawAnimation animation() {
        return switch (mode()) {
            case ModeFlee -> RunLoop;
            case ModeStalk -> striding() ? WalkLoop : IdleLoop;
            case ModeStare -> StareLoop;
            case ModeSink -> SinkPlay;
            case ModePeek -> PeekPlay;
            case ModeLoom -> LoomLoop;
            case ModeDrag -> DragLoop;
            case ModeBeat -> BeatLoop;
            case ModeLift -> LiftPlay;
            default -> striding() ? WalkLoop : IdleLoop;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp4666GaitController(this, "body", Scp4666GaitController.Blend, Event -> Event.setAndContinue(animation())));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
