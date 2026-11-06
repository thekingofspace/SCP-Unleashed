package net.rai.scp_unleashed.scp027;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp027Bat extends Bat implements Scp027Vermin {
    public static final float NipDamage = 1.0f;
    public static final int NipCooldown = 30;
    public static final double NipReach = 1.15;
    public static final double ChaseRange = 12.0;
    public static final double OrbitRadius = 0.9;
    public static final double MasterOrbit = 1.7;
    public static final int RetargetTicks = 20;

    private final Scp027Leash Leash = new Scp027Leash();
    private final TargetingConditions Hunting = TargetingConditions.forCombat().range(ChaseRange).selector(Target -> Scp027Vermin.isPrey(this, Target));
    @Nullable
    private LivingEntity Quarry;
    private int NipReady;
    private int Retarget;

    public Scp027Bat(EntityType<? extends Scp027Bat> Type, Level World) {
        super(Type, World);
        setResting(false);
        xpReward = 0;
    }

    @Override
    public Scp027Leash leash() {
        return Leash;
    }

    @Override
    public boolean flies() {
        return true;
    }

    @Nullable
    public LivingEntity quarry() {
        return Quarry;
    }

    @Override
    protected void customServerAiStep() {
        if (isResting()) {
            setResting(false);
        }
        if (Leash.tick(this)) {
            return;
        }
        Vec3 Aim = aim();
        Vec3 Motion = getDeltaMovement();
        double Dx = Aim.x - getX();
        double Dy = Aim.y - getY();
        double Dz = Aim.z - getZ();
        Vec3 Steered = Motion.add((Math.signum(Dx) * 0.5 - Motion.x) * 0.12, (Math.signum(Dy) * 0.7 - Motion.y) * 0.12, (Math.signum(Dz) * 0.5 - Motion.z) * 0.12);
        setDeltaMovement(Steered);
        float Heading = (float) (Mth.atan2(Steered.z, Steered.x) * Mth.RAD_TO_DEG) - 90.0f;
        zza = 0.5f;
        setYRot(getYRot() + Mth.wrapDegrees(Heading - getYRot()));
        nip();
    }

    private Vec3 aim() {
        double Phase = tickCount * 0.21 + getId() * 1.7;
        if (Leash.scattering()) {
            Vec3 From = Leash.home() != null ? Leash.home() : position();
            Vec3 Away = position().subtract(From);
            Vec3 Flat = new Vec3(Away.x, 0.0, Away.z);
            Flat = Flat.lengthSqr() < 1.0e-4 ? new Vec3(Math.cos(Phase), 0.0, Math.sin(Phase)) : Flat.normalize();
            return position().add(Flat.scale(4.0)).add(0.0, 2.5, 0.0);
        }
        if (--Retarget <= 0) {
            Retarget = RetargetTicks;
            Quarry = level().getNearestEntity(level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(ChaseRange), Target -> true),
                    Hunting, this, getX(), getEyeY(), getZ());
            setTarget(Quarry);
        }
        if (Quarry != null && (!Quarry.isAlive() || !Scp027Vermin.isPrey(this, Quarry))) {
            Quarry = null;
            setTarget(null);
        }
        if (Quarry != null) {
            Vec3 Head = Quarry.getEyePosition();
            double Dip = Math.sin(Phase * 0.37) > 0.6 ? 0.2 : 1.0;
            return Head.add(Math.cos(Phase) * OrbitRadius * Dip, 0.15 + Math.sin(Phase * 1.3) * 0.35, Math.sin(Phase) * OrbitRadius * Dip);
        }
        Scp027Entity Master = Leash.master();
        if (Master != null) {
            return Master.position().add(Math.cos(Phase * 0.6) * MasterOrbit, 2.1 + Math.sin(Phase * 0.9) * 0.4, Math.sin(Phase * 0.6) * MasterOrbit);
        }
        return position().add(Math.cos(Phase), 0.3, Math.sin(Phase));
    }

    private void nip() {
        if (NipReady > 0) {
            NipReady--;
            return;
        }
        if (Quarry == null || level().getDifficulty() == Difficulty.PEACEFUL || distanceToSqr(Quarry.getEyePosition()) > NipReach * NipReach) {
            return;
        }
        if (Quarry.hurt(damageSources().mobAttack(this), NipDamage)) {
            playSound(SoundEvents.BAT_AMBIENT, 0.5f, 1.4f + getRandom().nextFloat() * 0.2f);
        }
        NipReady = NipCooldown + getRandom().nextInt(10);
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
        setResting(false);
    }
}
