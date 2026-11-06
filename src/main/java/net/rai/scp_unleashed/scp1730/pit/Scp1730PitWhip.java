package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730PitWhip extends Entity implements GeoEntity {
    public static final float Width = 1.2f;
    public static final float Height = 1.2f;
    public static final double Reach = 8.0;
    public static final double HitRadius = 1.3;
    public static final float Damage = 7.0f;
    public static final byte PhaseRise = 0;
    public static final byte PhaseSlam = 1;
    public static final byte PhaseWithdraw = 2;
    public static final byte PhaseWrithe = 3;
    public static final int RiseTicks = 32;
    public static final int SlamTicks = 16;
    public static final int SlamContact = 9;
    public static final int WithdrawTicks = 16;
    private static final EntityDataAccessor<Byte> DataPhase = SynchedEntityData.defineId(Scp1730PitWhip.class, EntityDataSerializers.BYTE);
    private static final RawAnimation Rise = RawAnimation.begin().thenPlayAndHold("rise");
    private static final RawAnimation Slam = RawAnimation.begin().thenPlayAndHold("slam");
    private static final RawAnimation Withdraw = RawAnimation.begin().thenPlayAndHold("withdraw");
    private static final RawAnimation Writhe = RawAnimation.begin().thenPlay("rise").thenLoop("writhe");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int PhaseTicks;
    private int Life = 60;
    private int OwnerId = -1;

    public Scp1730PitWhip(EntityType<? extends Scp1730PitWhip> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    private static Scp1730PitWhip made(ServerLevel World, Scp1730PitForm Owner, Vec3 Root, Vec3 Toward) {
        Scp1730PitWhip Whip = new Scp1730PitWhip(Scp1730PitRegistry.WhipType.get(), World);
        Whip.OwnerId = Owner.getId();
        double Dx = Toward.x - Root.x;
        double Dz = Toward.z - Root.z;
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        Whip.moveTo(Root.x, Owner.rimY() - 1.2, Root.z, Yaw, 0.0f);
        return Whip;
    }

    public static Scp1730PitWhip strike(ServerLevel World, Scp1730PitForm Owner, Vec3 Target, Vec3 Root) {
        Scp1730PitWhip Whip = made(World, Owner, Root, Target);
        Whip.entityData.set(DataPhase, PhaseRise);
        World.addFreshEntity(Whip);
        World.playSound(null, Root.x, Owner.rimY(), Root.z, Scp1730PitRegistry.WhipRise.get(), SoundSource.HOSTILE, 1.8f, 0.8f + World.random.nextFloat() * 0.2f);
        return Whip;
    }

    public static Scp1730PitWhip writhe(ServerLevel World, Scp1730PitForm Owner, Vec3 Root, int Ticks) {
        Vec3 Out = Root.add(Root.subtract(Owner.home()).multiply(1.0, 0.0, 1.0));
        Scp1730PitWhip Whip = made(World, Owner, Root, Out);
        Whip.entityData.set(DataPhase, PhaseWrithe);
        Whip.Life = Ticks;
        World.addFreshEntity(Whip);
        return Whip;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataPhase, PhaseRise);
    }

    public byte phase() {
        return entityData.get(DataPhase);
    }

    private void setPhase(byte Phase) {
        entityData.set(DataPhase, Phase);
        PhaseTicks = 0;
    }

    public Vec3 direction() {
        return Vec3.directionFromRotation(0.0f, getYRot());
    }

    public Vec3 strikeEnd() {
        return new Vec3(getX(), getY() + 1.2, getZ()).add(direction().scale(Reach));
    }

    public void repel() {
        if (phase() != PhaseWithdraw) {
            setPhase(PhaseWithdraw);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && Source.getEntity() != null && phase() == PhaseRise) {
            repel();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return phase() == PhaseRise || phase() == PhaseWrithe;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        PhaseTicks++;
        if (level().isClientSide) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        Entity Owner = OwnerId >= 0 ? World.getEntity(OwnerId) : null;
        if (!(Owner instanceof Scp1730PitForm Pit) || !Pit.isAlive() || Pit.dazed()) {
            if (phase() != PhaseWithdraw) {
                repel();
            }
        }
        if (phase() == PhaseRise && Scp1730Anchors.anchored(World, position().add(0.0, 1.0, 0.0))) {
            repel();
        }
        switch (phase()) {
            case PhaseRise -> {
                if (PhaseTicks % 3 == 0) {
                    warn(World);
                }
                if (PhaseTicks >= RiseTicks) {
                    setPhase(PhaseSlam);
                }
            }
            case PhaseSlam -> {
                if (PhaseTicks == SlamContact) {
                    contact(World);
                }
                if (PhaseTicks >= SlamTicks) {
                    setPhase(PhaseWithdraw);
                }
            }
            case PhaseWrithe -> {
                if (PhaseTicks >= Life) {
                    setPhase(PhaseWithdraw);
                }
            }
            default -> {
                if (PhaseTicks >= WithdrawTicks) {
                    discard();
                }
            }
        }
    }

    private void warn(ServerLevel World) {
        Vec3 Start = position();
        Vec3 Way = direction();
        int Rim = (int) Math.floor(getY() + 1.2);
        for (int Step = 2; Step <= (int) Reach; Step += 2) {
            Vec3 At = Start.add(Way.scale(Step));
            World.sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.1f, 0.08f), 1.6f), At.x, Rim + 0.15, At.z, 1, 0.25, 0.0, 0.25, 0.0);
        }
    }

    private void contact(ServerLevel World) {
        Vec3 Start = position().add(0.0, 1.2, 0.0);
        Vec3 End = strikeEnd();
        World.playSound(null, End.x, End.y, End.z, Scp1730PitRegistry.WhipSlam.get(), SoundSource.HOSTILE, 2.4f, 0.8f + World.random.nextFloat() * 0.2f);
        DamageSource Hurt = new DamageSource(World.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(Scp1730PitRegistry.PitDamage), this);
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(Start, End).inflate(HitRadius + 1.5), Player::isAlive)) {
            if (Each.isCreative() || Each.isSpectator()) {
                continue;
            }
            Vec3 Body = Each.position().add(0.0, 0.4, 0.0);
            if (Scp1730PitForm.segmentGap(Body, Start, End) > HitRadius + Each.getBbWidth() * 0.5) {
                continue;
            }
            if (Each.hurt(Hurt, Damage)) {
                Vec3 Away = direction();
                Each.setDeltaMovement(Away.x * 0.8, 0.4, Away.z * 0.8);
                Each.hurtMarked = true;
            }
        }
        for (int Step = 3; Step <= (int) Reach; Step += 2) {
            Vec3 At = Start.add(direction().scale(Step));
            BlockState Ground = World.getBlockState(BlockPos.containing(At.x, At.y - 0.5, At.z));
            if (!Ground.isAir()) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), At.x, At.y + 0.1, At.z, 8, 0.4, 0.1, 0.4, 0.1);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 3, Animated -> switch (phase()) {
            case PhaseSlam -> Animated.setAndContinue(Slam);
            case PhaseWithdraw -> Animated.setAndContinue(Withdraw);
            case PhaseWrithe -> Animated.setAndContinue(Writhe);
            default -> Animated.setAndContinue(Rise);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
