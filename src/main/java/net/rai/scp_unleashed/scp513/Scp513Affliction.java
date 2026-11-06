package net.rai.scp_unleashed.scp513;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.health.LethalDamage;
import net.rai.scp_unleashed.scp513.network.Scp513StatePayload;
import org.joml.Vector3f;

public final class Scp513Affliction {
    public static final ResourceLocation SpeedLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_frozen_speed");
    public static final ResourceLocation JumpLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_frozen_jump");
    public static final ResourceLocation DreadSlow = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_dread_slow");
    public static final int MobDreadTicks = 640;
    public static final int MobDreadStart = Scp513Stage.FreezeAt - MobDreadTicks;
    public static final double DreadSlowMax = 0.6;
    public static final int SlowEvery = 10;
    public static final int FlinchEvery = 46;
    public static final int FlinchTicks = 6;
    public static final double ShoulderSpread = 50.0;
    public static final int PanicTicks = 200;
    public static final int PanicRepath = 20;
    public static final double PanicDistance = 10.0;
    public static final double PanicSpeed = 1.35;
    public static final int GlanceEvery = 28;
    public static final int GaspChance = 110;
    public static final int ResyncEvery = 100;
    public static final int ChokeEvery = 30;
    public static final int CollapseAt = Scp513Stage.StrangleAt + Scp513Stage.StrangleTicks / 2;
    public static final double ApproachSpeed = 0.05;
    public static final double ReachDistance = 1.55;
    public static final float GrabVolume = 0.9f;
    public static final float ChokeVolume = 0.75f;
    public static final float GaspVolume = 0.7f;
    public static final int MiasmaEvery = 3;
    public static final int MiasmaCount = 4;
    public static final float MiasmaSize = 1.6f;
    public static final Vector3f MiasmaColour = new Vector3f(0.05f, 0.045f, 0.055f);

    private Scp513Affliction() {
    }

    public static Scp513State state(LivingEntity Target) {
        Scp513State Found = Target.getExistingDataOrNull(Scp513Registry.Affliction.get());
        return Found == null ? Scp513State.Fresh : Found;
    }

    public static boolean isAfflicted(LivingEntity Target) {
        return state(Target).active();
    }

    public static boolean expose(LivingEntity Target, Vec3 Source) {
        if (!(Target.level() instanceof ServerLevel) || Target instanceof DClassLike Body && Body.isPuppet()) {
            return false;
        }
        Scp513State Current = state(Target);
        if (Current.active()) {
            int Hastened = Scp513Stage.hastened(Current.Elapsed());
            if (Hastened != Current.Elapsed()) {
                Target.setData(Scp513Registry.Affliction.get(), Current.at(Hastened));
                sync(Target);
            }
            return false;
        }
        Target.setData(Scp513Registry.Affliction.get(), Scp513State.begin(Target.getRandom().nextLong(), Source).at(dreadStart(Target)));
        sync(Target);
        return true;
    }

    public static int dreadStart(LivingEntity Target) {
        return Target instanceof Player ? 0 : MobDreadStart;
    }

    public static float dreadProgress(LivingEntity Target, int Elapsed) {
        int Start = dreadStart(Target);
        return Mth.clamp((Elapsed - Start) / (float) (Scp513Stage.FreezeAt - Start), 0.0f, 1.0f);
    }

    public static void setElapsed(LivingEntity Target, int Elapsed) {
        Scp513State Current = state(Target);
        if (Current.active()) {
            Target.setData(Scp513Registry.Affliction.get(), Current.at(Elapsed));
            sync(Target);
        }
    }

    public static boolean cure(LivingEntity Target) {
        if (!Target.hasData(Scp513Registry.Affliction.get())) {
            return false;
        }
        boolean Was = isAfflicted(Target);
        Target.removeData(Scp513Registry.Affliction.get());
        unlock(Target);
        remove(Target, Attributes.MOVEMENT_SPEED, DreadSlow);
        if (Target instanceof DClass Body && Body.isResting()) {
            Body.wake();
        }
        if (Target instanceof Mob Body) {
            Body.setShiftKeyDown(false);
            if (Body.getPose() == Pose.CROUCHING) {
                Body.setPose(Pose.STANDING);
            }
        }
        if (Target instanceof ServerPlayer User) {
            send(User, Scp513StatePayload.cleared());
        }
        return Was;
    }

    public static void sync(LivingEntity Target) {
        if (Target instanceof ServerPlayer User) {
            Scp513State Current = state(User);
            send(User, Current.active() ? new Scp513StatePayload(Current.Elapsed(), Current.Seed()) : Scp513StatePayload.cleared());
        }
    }

    private static void send(ServerPlayer User, Scp513StatePayload Payload) {
        if (User instanceof FakePlayer) {
            return;
        }
        if (User.connection != null && User.connection.hasChannel(Scp513StatePayload.PayloadType)) {
            PacketDistributor.sendToPlayer(User, Payload);
        }
    }

    public static Vec3 approachDirection(long Seed) {
        double Angle = (Seed >>> 16 & 0xFFFF) / 65536.0 * Math.PI * 2.0;
        return new Vec3(Math.sin(Angle), 0.0, Math.cos(Angle));
    }

    public static double approachDistance(int Elapsed) {
        int Remaining = Mth.clamp(Scp513Stage.GrabAt - Elapsed, 0, Scp513Stage.ApproachTicks);
        return ReachDistance + Remaining * ApproachSpeed;
    }

    public static void tick(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        Scp513State Current = state(Target);
        if (!Current.active()) {
            return;
        }
        if (!Target.isAlive() || Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            cure(Target);
            return;
        }
        int Elapsed = Current.Elapsed() + 1;
        Scp513State Next = Current.at(Elapsed);
        Target.setData(Scp513Registry.Affliction.get(), Next);
        Scp513Stage Stage = Next.stage();
        if (Stage.frozen()) {
            lock(Target);
        } else {
            unlock(Target);
        }
        if (Target instanceof Mob Body) {
            if (Stage == Scp513Stage.Dread) {
                if (Elapsed % SlowEvery == 0) {
                    slow(Body, DreadSlowMax * dreadProgress(Body, Elapsed));
                }
            } else {
                remove(Body, Attributes.MOVEMENT_SPEED, DreadSlow);
            }
            behave(World, Body, Next);
        }
        if (Elapsed == Scp513Stage.GrabAt) {
            Scp513Registry.playLayers(World, Target.getEyePosition(), Scp513Registry.GrabLayers, SoundSource.HOSTILE, GrabVolume, 0.9f + World.random.nextFloat() * 0.1f);
        }
        if (Stage.seized()) {
            if ((Elapsed - Scp513Stage.GrabAt) % ChokeEvery == ChokeEvery - 1) {
                Scp513Registry.playLayers(World, Target.getEyePosition(), Scp513Registry.ChokeLayers, SoundSource.HOSTILE, ChokeVolume, 0.85f + World.random.nextFloat() * 0.1f);
            }
            if (Elapsed % MiasmaEvery == 0) {
                miasma(World, Target);
            }
        }
        if (Target instanceof ServerPlayer User && Elapsed % ResyncEvery == 0) {
            sync(User);
        }
        if (Elapsed >= Scp513Stage.DeathAt) {
            kill(Target);
        }
    }

    public static void kill(LivingEntity Target) {
        cure(Target);
        Target.hurt(Target.damageSources().source(Scp513Registry.Strangled), LethalDamage.against(Target));
    }

    private static void miasma(ServerLevel World, LivingEntity Target) {
        Vec3 Middle = Target.position().add(0.0, Target.getBbHeight() * 0.7, 0.0);
        World.sendParticles(new DustParticleOptions(MiasmaColour, MiasmaSize), Middle.x, Middle.y, Middle.z, MiasmaCount, Target.getBbWidth() * 0.6, Target.getBbHeight() * 0.3,
                Target.getBbWidth() * 0.6, 0.005);
    }

    private static void behave(ServerLevel World, Mob Body, Scp513State Current) {
        int Elapsed = Current.Elapsed();
        RandomSource Random = Body.getRandom();
        Scp513Stage Stage = Current.stage();
        if (Stage == Scp513Stage.Dread) {
            int Since = Elapsed - dreadStart(Body);
            if (Since < PanicTicks) {
                if (Body instanceof PathfinderMob Runner && !Runner.isNoAi() && Since % PanicRepath == 1) {
                    Vec3 Away = Runner.position().subtract(Current.source()).multiply(1.0, 0.0, 1.0);
                    if (Away.lengthSqr() < 0.01) {
                        Away = new Vec3(Random.nextDouble() - 0.5, 0.0, Random.nextDouble() - 0.5);
                    }
                    Vec3 Goal = Runner.position().add(Away.normalize().scale(PanicDistance));
                    Runner.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, PanicSpeed);
                }
                return;
            }
            if (Since == PanicTicks) {
                Body.getNavigation().stop();
            }
            int Flinch = Math.floorMod(Since - flinchOffset(Current.Seed()), FlinchEvery);
            if (Flinch == 0) {
                crouch(Body, true);
                glanceBack(Body, Random);
            } else if (Flinch == FlinchTicks) {
                crouch(Body, false);
            } else if (Since % GlanceEvery == 0) {
                glanceBack(Body, Random);
            }
            if (Random.nextInt(GaspChance) == 0) {
                Scp513Registry.playLayers(World, Body.getEyePosition(), Scp513Registry.GaspLayers, SoundSource.NEUTRAL, GaspVolume, 0.95f + Random.nextFloat() * 0.15f);
            }
            return;
        }
        Body.getNavigation().stop();
        Body.setSprinting(false);
        Vec3 Direction = approachDirection(Current.Seed());
        double Distance = approachDistance(Elapsed);
        Vec3 Looming = Body.position().add(Direction.scale(Distance)).add(0.0, Stage.seized() ? Body.getEyeHeight() + 0.35 : Scp513Registry.StalkerHeight * 0.85, 0.0);
        Body.getLookControl().setLookAt(Looming.x, Looming.y, Looming.z, 30.0f, 30.0f);
        if (Stage == Scp513Stage.Frozen && Elapsed == Scp513Stage.FreezeAt) {
            crouch(Body, false);
        }
        if (Elapsed == CollapseAt && Body instanceof DClass Subject) {
            crouch(Body, false);
            Subject.lieDown();
        }
    }

    public static int flinchOffset(long Seed) {
        return (int) Math.floorMod(Seed >>> 32, (long) FlinchEvery);
    }

    private static void glanceBack(Mob Body, RandomSource Random) {
        double Yaw = (Body.yBodyRot + 180.0 + (Random.nextDouble() - 0.5) * 2.0 * ShoulderSpread) * Mth.DEG_TO_RAD;
        Vec3 Glance = Body.getEyePosition().add(-Math.sin(Yaw) * 4.0, (Random.nextDouble() - 0.4) * 1.5, Math.cos(Yaw) * 4.0);
        Body.getLookControl().setLookAt(Glance.x, Glance.y, Glance.z, 60.0f, 60.0f);
    }

    private static void slow(LivingEntity Target, double Share) {
        AttributeInstance Instance = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Instance != null) {
            Instance.addOrUpdateTransientModifier(new AttributeModifier(DreadSlow, -Share, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void crouch(Mob Body, boolean Down) {
        Body.setShiftKeyDown(Down);
        if (Down && Body.getPose() == Pose.STANDING) {
            Body.setPose(Pose.CROUCHING);
        } else if (!Down && Body.getPose() == Pose.CROUCHING) {
            Body.setPose(Pose.STANDING);
        }
    }

    public static boolean isLocked(LivingEntity Target) {
        AttributeInstance Instance = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        return Instance != null && Instance.hasModifier(SpeedLock);
    }

    private static void lock(LivingEntity Target) {
        add(Target, Attributes.MOVEMENT_SPEED, SpeedLock);
        add(Target, Attributes.JUMP_STRENGTH, JumpLock);
        if (Target.isSprinting()) {
            Target.setSprinting(false);
        }
    }

    public static void unlock(LivingEntity Target) {
        remove(Target, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Target, Attributes.JUMP_STRENGTH, JumpLock);
    }

    private static void add(LivingEntity Target, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Target.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(LivingEntity Target, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Target.getAttribute(Kind);
        if (Instance != null && Instance.hasModifier(Id)) {
            Instance.removeModifier(Id);
        }
    }
}
