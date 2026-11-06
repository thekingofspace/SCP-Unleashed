package net.rai.scp_unleashed.z0004;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;

import java.util.EnumSet;
import java.util.List;

public final class Z0004PirateGoals {
    public static final double Reach = 2.6;
    public static final int SlashWindup = 6;
    public static final int MeleeCooldown = 22;
    public static final int ShootWindup = 10;
    public static final int PistolCooldown = 50;
    public static final int BlunderbussCooldown = 70;
    public static final int CaptainShotCooldown = 80;
    public static final double PistolLandRange = 18.0;
    public static final double PistolWaterRange = 6.0;
    public static final double BlunderbussLandRange = 10.0;
    public static final double BlunderbussWaterRange = 4.5;
    public static final double ChaseLand = 1.15;
    public static final double ChaseWater = 1.3;
    public static final int CommandInterval = 160;
    public static final int Repath = 8;
    public static final float PistolDamage = 5.0f;
    public static final float CaptainShotDamage = 6.0f;
    public static final float PelletDamage = 2.5f;
    public static final int Pellets = 5;
    public static final float PelletSpread = 7.0f;
    public static final float PistolSpread = 1.5f;
    public static final List<GunSounds.Layer> LandLayers = List.of(new GunSounds.Layer(GunsRegistry.ShotHeavy, 0.35f, 0.72f, 0, false),
            new GunSounds.Layer(GunsRegistry.ShotEcho, 0.25f, 0.8f, 1, false));
    public static final List<GunSounds.Layer> WaterLayers = List.of(new GunSounds.Layer(GunsRegistry.ShotWhump, 0.3f, 0.55f, 0, false));

    private Z0004PirateGoals() {
    }

    public static double range(Z0004Pirate Pirate) {
        boolean Water = Pirate.isUnderWater();
        if (Pirate.variant() == Z0004Pirate.Blunderbuss && !Pirate.captain()) {
            return Water ? BlunderbussWaterRange : BlunderbussLandRange;
        }
        return Water ? PistolWaterRange : PistolLandRange;
    }

    public static void shoot(Z0004Pirate Pirate, LivingEntity Target) {
        if (!(Pirate.level() instanceof ServerLevel World) || !Pirate.isAlive()) {
            return;
        }
        boolean Water = Pirate.isUnderWater();
        boolean Spray = Pirate.variant() == Z0004Pirate.Blunderbuss && !Pirate.captain();
        Vec3 Eye = Pirate.getEyePosition();
        Vec3 Aim = Target.getBoundingBox().getCenter().subtract(Eye);
        Vec3 Heading = Aim.lengthSqr() < 1.0e-6 ? Pirate.getLookAngle() : Aim.normalize();
        Vec3 Muzzle = Eye.add(Heading.scale(0.7)).add(0.0, -0.25, 0.0);
        int Count = Spray ? Pellets : 1;
        float Spread = Spray ? PelletSpread : PistolSpread;
        float Damage = Spray ? PelletDamage : Pirate.captain() ? CaptainShotDamage : PistolDamage;
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Direction = scatter(Heading, Spread, Pirate);
            World.addFreshEntity(new Z0004Shot(World, Pirate, Muzzle, Direction.scale(Spray ? Z0004Shot.PelletSpeed : Z0004Shot.BallSpeed), Damage, Spray));
        }
        Pirate.countShot();
        if (Water) {
            World.sendParticles(ParticleTypes.BUBBLE, Muzzle.x, Muzzle.y, Muzzle.z, 16, 0.2, 0.2, 0.2, 0.1);
            World.playSound(null, Muzzle.x, Muzzle.y, Muzzle.z, Z0004Registry.FlintlockWater.get(), SoundSource.HOSTILE, 1.2f, 0.8f + Pirate.getRandom().nextFloat() * 0.1f);
            GunSounds.play(World, null, Muzzle.x, Muzzle.y, Muzzle.z, WaterLayers, 0.8f);
        } else {
            World.sendParticles(ParticleTypes.SMOKE, Muzzle.x, Muzzle.y, Muzzle.z, 8, 0.15, 0.15, 0.15, 0.02);
            World.sendParticles(ParticleTypes.FLAME, Muzzle.x, Muzzle.y, Muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
            World.playSound(null, Muzzle.x, Muzzle.y, Muzzle.z, (Spray ? Z0004Registry.Blunderbuss : Z0004Registry.Flintlock).get(), SoundSource.HOSTILE, 2.0f,
                    (Spray ? 0.75f : 0.95f) + Pirate.getRandom().nextFloat() * 0.1f);
            GunSounds.play(World, null, Muzzle.x, Muzzle.y, Muzzle.z, LandLayers, Spray ? 1.0f : 0.7f);
        }
    }

    private static Vec3 scatter(Vec3 Heading, float Degrees, Z0004Pirate Pirate) {
        double Spread = Math.toRadians(Degrees);
        return Heading.add(Pirate.getRandom().nextGaussian() * Spread * 0.5, Pirate.getRandom().nextGaussian() * Spread * 0.5, Pirate.getRandom().nextGaussian() * Spread * 0.5)
                .normalize();
    }

    public static final class Fight extends Goal {
        private final Z0004Pirate Pirate;
        private int Cooldown;
        private int ShotCooldown = 20;
        private int Windup;
        private int Aim;
        private int Path;
        private int Command = CommandInterval / 2;

        public Fight(Z0004Pirate Owner) {
            Pirate = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Z0004Fight.targetable(Pirate.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Pirate.getTarget();
            return Z0004Fight.targetable(Target) && Pirate.distanceToSqr(Target) < Z0004Pirate.FollowRange * Z0004Pirate.FollowRange * 1.5;
        }

        @Override
        public void start() {
            Pirate.setAggressive(true);
            Path = 0;
        }

        @Override
        public void stop() {
            Pirate.setAggressive(false);
            Pirate.getNavigation().stop();
            Windup = 0;
            Aim = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Pirate.getTarget();
            if (Target == null) {
                return;
            }
            Pirate.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Distance = Pirate.distanceTo(Target);
            boolean Water = Pirate.isInWater();
            double Range = range(Pirate);
            if (Windup > 0 && --Windup == 0 && Distance <= Reach + 0.8) {
                Pirate.doHurtTarget(Target);
            }
            if (Aim > 0 && --Aim == 0) {
                shoot(Pirate, Target);
            }
            Cooldown--;
            ShotCooldown--;
            boolean Close = Pirate.melee() && (!Pirate.gunner() || Distance < 5.0);
            double Pace = Water ? Z0004PirateGoals.ChaseWater : ChaseLand;
            if (--Path <= 0) {
                Path = Repath;
                if (Close || Distance > Range * 0.8) {
                    Pirate.getNavigation().moveTo(Target, Pace);
                } else if (Distance < 3.5) {
                    Vec3 Away = Pirate.position().subtract(Target.position()).normalize().scale(4.0).add(Pirate.position());
                    Pirate.getNavigation().moveTo(Away.x, Away.y, Away.z, Pace);
                } else {
                    Pirate.getNavigation().stop();
                }
            }
            boolean Busy = Windup > 0 || Aim > 0;
            if (!Busy && Pirate.melee() && Distance <= Reach && Cooldown <= 0) {
                Pirate.triggerAnim("action", "slash");
                Windup = SlashWindup;
                Cooldown = MeleeCooldown;
                Pirate.playSound(Z0004Registry.Swing.get(), 0.7f, 0.8f);
                return;
            }
            if (!Busy && Pirate.gunner() && ShotCooldown <= 0 && Distance <= Range && (!Pirate.melee() || Distance > 4.0) && Pirate.getSensing().hasLineOfSight(Target)) {
                Pirate.triggerAnim("action", "shoot");
                Aim = ShootWindup;
                ShotCooldown = Pirate.captain() ? CaptainShotCooldown : Pirate.variant() == Z0004Pirate.Blunderbuss ? BlunderbussCooldown : PistolCooldown;
                return;
            }
            if (!Busy && Pirate.captain() && --Command <= 0) {
                Command = CommandInterval + Pirate.getRandom().nextInt(80);
                Pirate.triggerAnim("action", "command");
                Pirate.playSound(Z0004Registry.Bellow.get(), 1.6f, 0.6f);
            }
        }

        public int aim() {
            return Aim;
        }
    }

    public static final class Home extends Goal {
        private final Z0004Pirate Pirate;

        public Home(Z0004Pirate Owner) {
            Pirate = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Pirate.getTarget() == null && Pirate.home() != null && Pirate.fightId() != null
                    && Z0004Fight.flatDistanceSqr(Pirate.position(), Vec3.atCenterOf(Pirate.home())) > Z0004Fight.CrewLeash * Z0004Fight.CrewLeash;
        }

        @Override
        public void start() {
            Vec3 Target = Vec3.atCenterOf(Pirate.home());
            Pirate.getNavigation().moveTo(Target.x, Target.y, Target.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return Pirate.getTarget() == null && !Pirate.getNavigation().isDone();
        }
    }

    public static final class Wander extends Goal {
        private final Z0004Pirate Pirate;

        public Wander(Z0004Pirate Owner) {
            Pirate = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Pirate.getTarget() == null && Pirate.getRandom().nextInt(100) == 0;
        }

        @Override
        public void start() {
            Vec3 Spot = DefaultRandomPos.getPos(Pirate, 8, 4);
            if (Spot != null) {
                Pirate.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 0.8);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Pirate.getTarget() == null && !Pirate.getNavigation().isDone();
        }
    }
}
