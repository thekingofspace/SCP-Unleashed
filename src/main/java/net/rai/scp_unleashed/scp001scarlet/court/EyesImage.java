package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EyesImage extends Mob implements GeoEntity, CourtMember {
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Life = DaughterEyes.ImageLife;

    public EyesImage(EntityType<? extends Mob> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public String courtId() {
        return "scarlet_daughter_eyes_image";
    }

    public void life(int Ticks) {
        Life = Ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (--Life <= 0) {
            pop(false);
            return;
        }
        BlockPos Ground = CourtArena.ground((ServerLevel) level(), blockPosition(), 8);
        double Want = (Ground != null ? Ground.getY() : getY()) + DaughterEyes.Hover + Math.sin((tickCount + getId()) * 0.05) * 0.25;
        setDeltaMovement(0.0, (Want - getY()) * 0.08, 0.0);
        Player Seen = level().getNearestPlayer(this, 40.0);
        if (Seen != null) {
            getLookControl().setLookAt(Seen, 10.0f, 10.0f);
            Vec3 Way = Seen.position().subtract(position());
            float Yaw = (float) Math.toDegrees(Math.atan2(Way.z, Way.x)) - 90.0f;
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), Yaw, 6.0f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    public void pop(boolean Struck) {
        if (!(level() instanceof ServerLevel World) || isRemoved()) {
            return;
        }
        Vec3 Centre = position().add(0.0, 3.0, 0.0);
        CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, Centre, 24, 1.0, 0.04);
        CourtFx.burst(World, CourtFx.Amber, Centre, 20, 1.2, 0.0);
        CourtFx.sound(World, Centre, "eyes_split", 2.0f, 1.5f);
        if (Struck) {
            CourtMinion Made = ScarletCourt.Instance.summon(World, CourtKind.Eyeling, BlockPos.containing(Centre), true);
            if (Made != null && getTarget() != null) {
                Made.setTarget(getTarget());
            }
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            discard();
            return true;
        }
        Entity Attacker = Source.getEntity();
        if (Attacker == null || CourtFx.allied(this, Attacker)) {
            return false;
        }
        pop(true);
        return true;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6.0, 4.0, 6.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return CourtFx.allied(this, Other) || super.isAlliedTo(Other);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Life", Life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Life = Tag.getInt("Life");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 5, State -> State.setAndContinue(State.isMoving() ? WalkLoop : IdleLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
