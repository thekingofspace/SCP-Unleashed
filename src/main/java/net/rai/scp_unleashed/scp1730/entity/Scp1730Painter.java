package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class Scp1730Painter extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.5f;
    public static final float Height = 2.75f;
    public static final float EyeHeight = 2.1f;
    public static final double WallGap = 1.125;
    public static final int PaintTicks = 48;
    public static final int PlaceTick = 40;
    public static final int SymbolCap = 6;
    public static final double CapRadius = 12.0;
    public static final int MinRest = 600;
    public static final int MaxRest = 1200;
    public static final int MinRestInside = 360;
    public static final int MaxRestInside = 720;
    public static final int AnchorCheckEvery = 5;
    private static final EntityDataAccessor<Boolean> DataAnchored = SynchedEntityData.defineId(Scp1730Painter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DataPainting = SynchedEntityData.defineId(Scp1730Painter.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Walk = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation Painting = RawAnimation.begin().thenPlay("emerge").thenLoop("paint");
    private static final RawAnimation RetractIdle = RawAnimation.begin().thenPlay("retract").thenLoop("idle");
    private static final RawAnimation RetractWalk = RawAnimation.begin().thenPlay("retract").thenLoop("walk");
    public static final int RetractTicks = 15;
    private boolean PaintSeen;
    private int RetractUntil = -1;
    private static final RawAnimation Anchored = RawAnimation.begin().thenLoop("anchored");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Rest = 200;

    public Scp1730Painter(EntityType<? extends Scp1730Painter> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataAnchored, false);
        Builder.define(DataPainting, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PaintGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0f, 0.9, 1.15, Each -> !((Player) Each).isSpectator()));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public boolean anchored() {
        return entityData.get(DataAnchored);
    }

    public boolean painting() {
        return entityData.get(DataPainting);
    }

    public void setAnchoredForTest(boolean Value) {
        entityData.set(DataAnchored, Value);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && anchored() && random.nextInt(5) == 0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            level().addParticle(Scp1730Registry.AnchorParticle.get(), getX() + Math.cos(Angle) * 0.45, getY() + 0.4 + random.nextDouble() * 2.2, getZ() + Math.sin(Angle) * 0.45,
                    0.0, 0.01, 0.0);
        }
        if (!level().isClientSide && tickCount % AnchorCheckEvery == 0) {
            boolean Now = Scp1730Anchors.anchored(this);
            if (Now != anchored()) {
                entityData.set(DataAnchored, Now);
                if (Now) {
                    entityData.set(DataPainting, false);
                    level().playSound(null, getX(), getY(), getZ(), Scp1730Registry.PainterAnchored.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (Rest > 0) {
            Rest--;
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp1730Registry.PainterIdle.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    public int restFor() {
        boolean Inside = Scp1730World.inside(level());
        int Low = Inside ? MinRestInside : MinRest;
        int High = Inside ? MaxRestInside : MaxRest;
        return Low + random.nextInt(High - Low + 1);
    }

    public record Spot(BlockPos Stand, BlockPos Cell, Direction Wall) {
    }

    @Nullable
    public Spot findSpot() {
        Level World = level();
        BlockPos Base = blockPosition();
        for (int Attempt = 0; Attempt < 24; Attempt++) {
            BlockPos Floor = Base.offset(random.nextInt(17) - 8, random.nextInt(5) - 2, random.nextInt(17) - 8);
            if (!World.isLoaded(Floor) || !World.getBlockState(Floor).isAir() || !World.getBlockState(Floor.above()).isAir()
                    || !World.getBlockState(Floor.below()).isFaceSturdy(World, Floor.below(), Direction.UP)) {
                continue;
            }
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Cell = Floor.above();
                BlockPos Wall = Cell.relative(Side);
                if (World.getBlockState(Wall).isFaceSturdy(World, Wall, Side.getOpposite()) && World.getBlockState(Cell).isAir()
                        && Scp1730Symbols.countNear(World, Cell, CapRadius) < SymbolCap && !Scp1730Anchors.anchored(World, Cell)) {
                    return new Spot(Floor, Cell, Side);
                }
            }
        }
        return null;
    }

    public boolean paintAt(BlockPos Cell, Direction Wall) {
        Level World = level();
        if (!World.getBlockState(Cell).isAir()) {
            return false;
        }
        BlockState Made = Scp1730Registry.Symbol.get().defaultBlockState().setValue(Scp1730SymbolBlock.Facing, Wall.getOpposite())
                .setValue(Scp1730SymbolBlock.Glyph, random.nextInt(Scp1730SymbolBlock.GlyphCount));
        if (!Made.canSurvive(World, Cell)) {
            return false;
        }
        World.setBlock(Cell, Made, 3);
        return true;
    }

    static final class PaintGoal extends Goal {
        private final Scp1730Painter Owner;
        private Spot Target;
        private int Ticks;
        private boolean Arrived;

        PaintGoal(Scp1730Painter Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (Owner.anchored() || Owner.Rest > 0) {
                return false;
            }
            Target = Owner.findSpot();
            if (Target == null) {
                Owner.Rest = 100;
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && !Owner.anchored() && Ticks < PaintTicks + 200;
        }

        @Override
        public void start() {
            Ticks = 0;
            Arrived = false;
            Owner.getNavigation().moveTo(Target.Stand().getX() + 0.5, Target.Stand().getY(), Target.Stand().getZ() + 0.5, 0.8);
        }

        @Override
        public void stop() {
            Owner.entityData.set(DataPainting, false);
            Owner.Rest = Owner.restFor();
            Target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Target == null) {
                return;
            }
            Ticks++;
            Direction Side = Target.Wall();
            Vec3 Face = Vec3.atCenterOf(Target.Cell()).add(Side.getStepX() * 0.5, 0.0, Side.getStepZ() * 0.5);
            Vec3 Stand = new Vec3(Face.x - Side.getStepX() * WallGap, Target.Stand().getY(), Face.z - Side.getStepZ() * WallGap);
            if (!Arrived) {
                if (Owner.position().distanceToSqr(Stand.x, Owner.getY(), Stand.z) < 1.2 || Owner.getNavigation().isDone()) {
                    Arrived = Owner.position().distanceToSqr(Stand.x, Owner.getY(), Stand.z) < 2.5;
                    if (!Arrived) {
                        Target = null;
                        return;
                    }
                    Owner.getNavigation().stop();
                    Ticks = 0;
                    Owner.entityData.set(DataPainting, true);
                }
                return;
            }
            Vec3 Here = Owner.position();
            Owner.setPos(Here.x + (Stand.x - Here.x) * 0.2, Here.y, Here.z + (Stand.z - Here.z) * 0.2);
            float Yaw = Side.toYRot();
            Owner.setYRot(Yaw);
            Owner.setYHeadRot(Yaw);
            Owner.yBodyRot = Yaw;
            if (Ticks % 12 == 4 && Owner.level() instanceof ServerLevel Server) {
                Server.playSound(null, Face.x, Face.y, Face.z, Scp1730Registry.Paint.get(), SoundSource.HOSTILE, 0.8f, 0.85f + Owner.random.nextFloat() * 0.3f);
            }
            if (Ticks == PlaceTick) {
                Owner.paintAt(Target.Cell(), Side);
            }
            if (Ticks >= PaintTicks) {
                Target = null;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Rest", Rest);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Rest = Tag.getInt("Rest");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 6, Animated -> {
            boolean Now = painting() && !anchored();
            if (PaintSeen && !Now) {
                RetractUntil = tickCount + RetractTicks;
            }
            PaintSeen = Now;
            if (Now) {
                return Animated.setAndContinue(Painting);
            }
            if (tickCount < RetractUntil) {
                return Animated.setAndContinue(Animated.isMoving() ? RetractWalk : RetractIdle);
            }
            if (Animated.isMoving()) {
                return Animated.setAndContinue(Walk);
            }
            return Animated.setAndContinue(anchored() ? Anchored : Idle);
        }));
    }

    public boolean tentacleShown() {
        return painting() && !anchored() || tickCount < RetractUntil;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
