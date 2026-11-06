package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class CourtFx {
    public static final String Tag = "scarlet_court";
    public static final String MarkKey = "scarlet_court_marked";
    public static final float MarkBonus = 1.25f;
    public static final DustParticleOptions Crimson = new DustParticleOptions(new Vector3f(0.78f, 0.08f, 0.1f), 1.6f);
    public static final DustParticleOptions DeepCrimson = new DustParticleOptions(new Vector3f(0.45f, 0.03f, 0.06f), 2.2f);
    public static final DustParticleOptions Bone = new DustParticleOptions(new Vector3f(0.93f, 0.9f, 0.82f), 1.4f);
    public static final DustParticleOptions Amber = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.15f), 1.5f);
    public static final DustParticleOptions Binding = new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.5f), 2.0f);
    public static final DustParticleOptions Violet = new DustParticleOptions(new Vector3f(0.42f, 0.25f, 0.55f), 1.5f);
    public static final DustParticleOptions Ash = new DustParticleOptions(new Vector3f(0.35f, 0.33f, 0.33f), 2.0f);
    public static final DustParticleOptions Moonlight = new DustParticleOptions(new Vector3f(0.72f, 0.82f, 1.0f), 1.6f);
    public static final DustParticleOptions Silver = new DustParticleOptions(new Vector3f(0.86f, 0.88f, 0.94f), 1.2f);
    public static final DustParticleOptions Indigo = new DustParticleOptions(new Vector3f(0.33f, 0.27f, 0.62f), 1.8f);

    private CourtFx() {
    }

    public static boolean courtType(Entity Other) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(Other.getType()).getPath().startsWith("scarlet_");
    }

    public static boolean allied(Entity Self, Entity Other) {
        if (Other == null || Other == Self) {
            return Other != null;
        }
        return Other instanceof CourtMember || Other.getTags().contains(Tag) || courtType(Other);
    }

    public static boolean validTarget(LivingEntity Other) {
        if (Other == null || !Other.isAlive() || allied(null, Other)) {
            return false;
        }
        return !(Other instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public static List<LivingEntity> victims(ServerLevel World, AABB Area) {
        return World.getEntitiesOfClass(LivingEntity.class, Area, CourtFx::validTarget);
    }

    public static List<LivingEntity> victimsNear(ServerLevel World, Vec3 Centre, double Radius) {
        return World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Radius, Radius, Radius),
                Each -> validTarget(Each) && Each.position().distanceToSqr(Centre) <= Radius * Radius);
    }

    public static boolean marked(LivingEntity Victim) {
        return Victim.getPersistentData().getLong(MarkKey) > Victim.level().getGameTime();
    }

    public static void mark(LivingEntity Victim, int Ticks) {
        Victim.getPersistentData().putLong(MarkKey, Victim.level().getGameTime() + Ticks);
    }

    public static boolean strike(LivingEntity Attacker, LivingEntity Victim, float Amount) {
        return strike(Attacker.damageSources().mobAttack(Attacker), Victim, Amount);
    }

    public static boolean strike(DamageSource Source, LivingEntity Victim, float Amount) {
        if (!validTarget(Victim)) {
            return false;
        }
        float Final = marked(Victim) ? Amount * MarkBonus : Amount;
        return Victim.hurt(Source, Final);
    }

    public static void push(Entity Victim, Vec3 From, double Across, double Up) {
        Vec3 Away = Victim.position().subtract(From);
        Vec3 Flat = new Vec3(Away.x, 0.0, Away.z);
        Vec3 Way = Flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : Flat.normalize();
        Victim.setDeltaMovement(Victim.getDeltaMovement().add(Way.x * Across, Up, Way.z * Across));
        Victim.hurtMarked = true;
    }

    public static void pull(Entity Victim, Vec3 Towards, double Strength) {
        Vec3 Way = Towards.subtract(Victim.position());
        if (Way.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 Step = Way.normalize().scale(Strength);
        Victim.setDeltaMovement(Victim.getDeltaMovement().scale(0.5).add(Step.x, Math.max(0.05, Step.y * 0.4), Step.z));
        Victim.hurtMarked = true;
    }

    public static void line(ServerLevel World, ParticleOptions Particle, Vec3 From, Vec3 To, double Spacing, double Spread) {
        Vec3 Way = To.subtract(From);
        double Length = Way.length();
        int Count = Math.max(2, (int) Math.ceil(Length / Spacing));
        for (int Index = 0; Index <= Count; Index++) {
            Vec3 Point = From.add(Way.scale(Index / (double) Count));
            World.sendParticles(Particle, Point.x, Point.y, Point.z, 1, Spread, Spread, Spread, 0.0);
        }
    }

    public static void ring(ServerLevel World, ParticleOptions Particle, Vec3 Centre, double Radius, int Count, double Lift) {
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Index * Math.PI * 2.0 / Count;
            World.sendParticles(Particle, Centre.x + Math.cos(Angle) * Radius, Centre.y + Lift, Centre.z + Math.sin(Angle) * Radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    public static void burst(ServerLevel World, ParticleOptions Particle, Vec3 Centre, int Count, double Spread, double Speed) {
        World.sendParticles(Particle, Centre.x, Centre.y, Centre.z, Count, Spread, Spread, Spread, Speed);
    }

    public static void crumble(ServerLevel World, BlockState State, Vec3 Centre, int Count, double Wide, double Tall) {
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y, Centre.z, Count, Wide, Tall, Wide, 0.15);
    }

    public static BlockState solidAt(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.isAir() ? Blocks.BLACKSTONE.defaultBlockState() : State;
    }

    public static void sound(ServerLevel World, Vec3 At, String Name, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, ScarletCourtSounds.get(Name), SoundSource.HOSTILE, Volume, Pitch);
    }

    public static Vec3 flat(Vec3 Way) {
        Vec3 Flat = new Vec3(Way.x, 0.0, Way.z);
        return Flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : Flat.normalize();
    }

    public static Vec3 turn(Vec3 Way, double Degrees) {
        double Radians = Math.toRadians(Degrees);
        double Cos = Math.cos(Radians);
        double Sin = Math.sin(Radians);
        return new Vec3(Way.x * Cos - Way.z * Sin, Way.y, Way.x * Sin + Way.z * Cos);
    }

    public static double segmentDistance(Vec3 Point, Vec3 From, Vec3 To) {
        Vec3 Way = To.subtract(From);
        double Length = Way.lengthSqr();
        if (Length < 1.0E-6) {
            return Point.distanceTo(From);
        }
        double Share = Math.max(0.0, Math.min(1.0, Point.subtract(From).dot(Way) / Length));
        return Point.distanceTo(From.add(Way.scale(Share)));
    }
}
