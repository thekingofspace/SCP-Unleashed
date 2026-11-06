package net.rai.scp_unleashed.scp513;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public final class Scp513Ring {
    public static final double Range = 32.0;
    public static final float Volume = 2.4f;
    public static final float PitchSpread = 0.04f;
    public static final int Motes = 14;
    public static final float MoteSize = 1.2f;
    public static final Vector3f MoteColour = new Vector3f(0.07f, 0.06f, 0.07f);
    private static int Rings;

    private Scp513Ring() {
    }

    public static int rings() {
        return Rings;
    }

    public static List<LivingEntity> ring(ServerLevel World, Vec3 Source, @Nullable Entity Holder) {
        Rings++;
        float Pitch = 1.0f - PitchSpread + World.random.nextFloat() * PitchSpread * 2.0f;
        Scp513Registry.playLayers(World, Source, Scp513Registry.RingLayers, SoundSource.PLAYERS, Volume, Pitch);
        World.sendParticles(new DustParticleOptions(MoteColour, MoteSize), Source.x, Source.y, Source.z, Motes, 0.3, 0.25, 0.3, 0.01);
        World.gameEvent(Holder, GameEvent.INSTRUMENT_PLAY, Source);
        List<LivingEntity> Heard = listeners(World, Source);
        for (LivingEntity Target : Heard) {
            Scp513Affliction.expose(Target, Source);
        }
        return Heard;
    }

    public static List<LivingEntity> listeners(ServerLevel World, Vec3 Source) {
        AABB Area = AABB.ofSize(Source, Range * 2.0 + 2.0, Range * 2.0 + 2.0, Range * 2.0 + 2.0);
        return World.getEntitiesOfClass(LivingEntity.class, Area, Target -> hears(Target, Source));
    }

    public static boolean counts(Entity Target) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Living.isDeadOrDying()) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Target);
    }

    public static boolean hears(LivingEntity Target, Vec3 Source) {
        if (!counts(Target) || !Targeting.canWitness(Target) || Senses.isDeafened(Target)) {
            return false;
        }
        return Target.getEyePosition().distanceToSqr(Source) <= Range * Range;
    }
}
