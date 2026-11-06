package net.rai.scp_unleashed.scp294;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp294.network.Scp294RadiancePayload;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp294Ascension {
    public static final int ChimeEvery = 15;
    public static final float ChimeLow = 0.6f;
    public static final float ChimeHigh = 2.0f;
    public static final int MinTicks = 40;
    public static final float Lethal = 1.0e6f;
    public static final DustParticleOptions Sickly = new DustParticleOptions(new Vector3f(0.55f, 0.72f, 0.28f), 1.0f);
    private static final Map<UUID, Ascent> Rising = new ConcurrentHashMap<>();

    private Scp294Ascension() {
    }

    public record Ascent(ResourceKey<Level> Dimension, long Start, int Ticks) {
    }

    public static void begin(LivingEntity Drinker, int Ticks) {
        if (!(Drinker.level() instanceof ServerLevel World)) {
            return;
        }
        int Length = Math.max(MinTicks, Ticks);
        Rising.put(Drinker.getUUID(), new Ascent(World.dimension(), World.getGameTime(), Length));
        Drinker.addEffect(new MobEffectInstance(MobEffects.LEVITATION, Length + 20, 1, false, false, true));
        if (Drinker instanceof ServerPlayer User) {
            PacketDistributor.sendToPlayer(User, new Scp294RadiancePayload(Length));
        }
    }

    public static boolean isRising(Entity Subject) {
        return Rising.containsKey(Subject.getUUID());
    }

    public static void forget(UUID Id) {
        Rising.remove(Id);
    }

    public static void clear() {
        Rising.clear();
    }

    public static void tick(MinecraftServer Server) {
        Iterator<Map.Entry<UUID, Ascent>> Walker = Rising.entrySet().iterator();
        while (Walker.hasNext()) {
            Map.Entry<UUID, Ascent> Pair = Walker.next();
            Ascent State = Pair.getValue();
            ServerLevel World = Server.getLevel(State.Dimension());
            Entity Found = World == null ? null : World.getEntity(Pair.getKey());
            if (!(Found instanceof LivingEntity Subject) || !Subject.isAlive() || Subject.level() != World) {
                Walker.remove();
                continue;
            }
            long Age = World.getGameTime() - State.Start();
            float Fraction = Math.min(1.0f, Age / (float) State.Ticks());
            if (Age % ChimeEvery == 0) {
                float Pitch = ChimeLow + (ChimeHigh - ChimeLow) * Fraction;
                World.playSound(null, Subject.getX(), Subject.getEyeY(), Subject.getZ(), Scp294Registry.Chime.get(), SoundSource.PLAYERS, 0.5f + Fraction * 0.5f, Pitch);
            }
            if (Age % 4 == 0) {
                World.sendParticles(ParticleTypes.END_ROD, Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.5, Subject.getZ(),
                        2 + Math.round(Fraction * 6), 0.45, 0.8, 0.45, 0.02);
            }
            if (Age >= State.Ticks()) {
                Walker.remove();
                vanish(World, Subject);
            }
        }
    }

    public static void vanish(ServerLevel World, LivingEntity Subject) {
        Subject.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, false));
        World.sendParticles(ParticleTypes.END_ROD, Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.5, Subject.getZ(), 60, 0.4, 0.9, 0.4, 0.12);
        World.sendParticles(ParticleTypes.FLASH, Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.5, Subject.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        World.playSound(null, Subject.getX(), Subject.getEyeY(), Subject.getZ(), Scp294Registry.Vanish.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        Subject.hurt(Scp294DamageSource.of(World, Scp294Registry.AscendedDamage, ""), Lethal);
    }
}
