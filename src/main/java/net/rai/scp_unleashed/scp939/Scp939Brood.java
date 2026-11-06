package net.rai.scp_unleashed.scp939;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

public final class Scp939Brood {
    public static final int LifeMin = 9600;
    public static final int LifeMax = 14400;
    public static final int ShakeTicks = 200;
    public static final int HoldAge = -24000;
    public static final int HoldBelow = -1200;
    public static final int ShiverEvery = 40;
    public static final double PlaceReach = 1.4;
    public static final DustParticleOptions Mist = new DustParticleOptions(new Vector3f(0.62f, 0.05f, 0.07f), 1.6f);
    public static final DustColorTransitionOptions Fade = new DustColorTransitionOptions(new Vector3f(0.78f, 0.12f, 0.12f), new Vector3f(0.32f, 0.04f, 0.06f), 1.3f);

    public record State(int Left, int Shaking) {
        public static final State None = new State(-1, -1);
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.fieldOf("left").forGetter(State::Left),
                Codec.INT.fieldOf("shaking").forGetter(State::Shaking)
        ).apply(Instance, State::new));

        public boolean active() {
            return Left >= 0 || Shaking >= 0;
        }

        public boolean shaking() {
            return Shaking >= 0;
        }
    }

    private Scp939Brood() {
    }

    public static State state(@Nullable Entity Target) {
        if (!(Target instanceof Villager Child) || !Child.hasData(Scp939Registry.Brood)) {
            return State.None;
        }
        return Child.getData(Scp939Registry.Brood);
    }

    public static boolean isBrood(@Nullable Entity Target) {
        return state(Target).active();
    }

    public static boolean isShaking(@Nullable Entity Target) {
        return state(Target).shaking();
    }

    public static int life(RandomSource Random) {
        return Mth.nextInt(Random, LifeMin, LifeMax);
    }

    @Nullable
    public static Villager produce(ServerLevel World, Scp939 Parent) {
        Villager Child = EntityType.VILLAGER.create(World);
        if (Child == null) {
            return null;
        }
        float Yaw = Parent.getYRot();
        Vec3 Ahead = Vec3.directionFromRotation(0.0f, Yaw).scale(PlaceReach);
        Vec3 Spot = Parent.position().add(Ahead.x, 0.0, Ahead.z);
        Child.moveTo(Spot.x, Spot.y, Spot.z, World.random.nextFloat() * 360.0f, 0.0f);
        if (!World.noCollision(Child)) {
            Child.moveTo(Parent.getX(), Parent.getY(), Parent.getZ(), Child.getYRot(), 0.0f);
        }
        Child.finalizeSpawn(World, World.getCurrentDifficultyAt(Child.blockPosition()), MobSpawnType.BREEDING, null);
        Child.setAge(HoldAge);
        Child.setData(Scp939Registry.Brood, new State(life(World.random), -1));
        if (!World.addFreshEntity(Child)) {
            return null;
        }
        World.sendParticles(ParticleTypes.POOF, Child.getX(), Child.getY() + 0.3, Child.getZ(), 6, 0.2, 0.15, 0.2, 0.01);
        World.gameEvent(Child, GameEvent.ENTITY_PLACE, Child.position());
        return Child;
    }

    public static Villager mark(Villager Child, int Left) {
        Child.setAge(HoldAge);
        Child.setData(Scp939Registry.Brood, new State(Math.max(0, Left), -1));
        return Child;
    }

    public static void tick(ServerLevel World, Villager Child) {
        State Now = Child.getData(Scp939Registry.Brood);
        if (!Now.active() || !Child.isAlive()) {
            return;
        }
        if (Child.getAge() > HoldBelow) {
            Child.setAge(HoldAge);
        }
        if (!Now.shaking()) {
            int Left = Now.Left() - 1;
            if (Left > 0) {
                Child.setData(Scp939Registry.Brood, new State(Left, -1));
                return;
            }
            Child.setData(Scp939Registry.Brood, new State(0, ShakeTicks));
            Scp939Sounds.play(World, Child.position(), Scp939Registry.ShiverLayers, 0.5f, 1.25f);
            return;
        }
        Child.setTicksFrozen(Child.getTicksRequiredToFreeze() + 2);
        int Shaking = Now.Shaking() - 1;
        if (Shaking % ShiverEvery == 0 && Shaking > 0) {
            Scp939Sounds.play(World, Child.position(), Scp939Registry.ShiverLayers, 0.4f + 0.3f * (1.0f - Shaking / (float) ShakeTicks), 1.2f + World.random.nextFloat() * 0.1f);
        }
        if (Shaking < ShakeTicks / 2 && World.random.nextInt(6) == 0) {
            World.sendParticles(Fade, Child.getX(), Child.getY() + Child.getBbHeight() * 0.5, Child.getZ(), 1, 0.15, 0.2, 0.15, 0.0);
        }
        if (Shaking > 0) {
            Child.setData(Scp939Registry.Brood, new State(0, Shaking));
            return;
        }
        burst(World, Child);
    }

    @Nullable
    public static Scp939 burst(ServerLevel World, Villager Child) {
        Vec3 Where = Child.position();
        float Yaw = Child.getYRot();
        Child.setData(Scp939Registry.Brood, State.None);
        Scp939 Young = Scp939Registry.Scp939Type.get().create(World);
        double Middle = Child.getBbHeight() * 0.5;
        World.sendParticles(Mist, Where.x, Where.y + Middle, Where.z, 70, 0.35, 0.4, 0.35, 0.02);
        World.sendParticles(Fade, Where.x, Where.y + Middle, Where.z, 40, 0.6, 0.5, 0.6, 0.01);
        World.sendParticles(ParticleTypes.POOF, Where.x, Where.y + Middle, Where.z, 8, 0.3, 0.3, 0.3, 0.02);
        Scp939Sounds.play(World, Where, Scp939Registry.BurstLayers, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        Child.discard();
        if (Young == null) {
            return null;
        }
        Young.setAge(-Scp939.GrowTicks);
        Young.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Young.setYBodyRot(Yaw);
        Young.setYHeadRot(Yaw);
        Young.finalizeSpawn(World, World.getCurrentDifficultyAt(Young.blockPosition()), MobSpawnType.CONVERSION, null);
        Young.setAge(-Scp939.GrowTicks);
        Young.setPersistenceRequired();
        World.addFreshEntity(Young);
        World.gameEvent(Young, GameEvent.ENTITY_PLACE, Where);
        return Young;
    }

    public static boolean shieldsFromCold(LivingEntity Target, DamageSource Source) {
        return isShaking(Target) && Source.is(DamageTypeTags.IS_FREEZING);
    }
}
