package net.rai.scp_unleashed.scp444;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp444Affliction {
    public static final String SettleKey = "message.scp_unleashed.scp444.settle";
    public static final int OnsetGlyphs = 10;
    public static final int AmbientOdds = 140;
    public static final double MouthDrop = 0.22;
    public static final double MouthReach = 0.32;

    private Scp444Affliction() {
    }

    public static boolean isAfflicted(@Nullable Entity Subject) {
        return Subject != null && Subject.getExistingDataOrNull(Scp444Registry.Silence.get()) != null;
    }

    @Nullable
    public static Long since(Entity Subject) {
        return Subject.getExistingDataOrNull(Scp444Registry.Silence.get());
    }

    public static boolean isShielded(@Nullable LivingEntity Subject) {
        return AntiMemetic.isProtected(Subject);
    }

    public static boolean afflict(LivingEntity Subject) {
        if (Subject.level().isClientSide || !Subject.isAlive() || isAfflicted(Subject)) {
            return false;
        }
        Subject.setData(Scp444Registry.Silence.get(), Subject.level().getGameTime());
        return true;
    }

    public static boolean read(ServerPlayer Reader) {
        if (isAfflicted(Reader) || isShielded(Reader) || Reader.isSpectator() || Targeting.isStatue(Reader)) {
            return false;
        }
        if (!afflict(Reader)) {
            return false;
        }
        onset(Reader);
        return true;
    }

    public static boolean clear(Entity Subject) {
        if (!isAfflicted(Subject)) {
            return false;
        }
        Subject.removeData(Scp444Registry.Silence.get());
        return true;
    }

    public static void onset(ServerPlayer Reader) {
        ServerLevel World = Reader.serverLevel();
        Vec3 Mouth = mouth(Reader);
        play(World, Mouth, Scp444Registry.SettleLayers, 0.9f, 0.85f + Reader.getRandom().nextFloat() * 0.1f);
        World.sendParticles(Scp444Registry.Glyph.get(), Mouth.x, Mouth.y, Mouth.z, OnsetGlyphs, 0.25, 0.2, 0.25, 0.01);
        Reader.displayClientMessage(Component.translatable(SettleKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    public static Vec3 mouth(LivingEntity Subject) {
        Vec3 Look = Subject.getLookAngle();
        return Subject.getEyePosition().add(Look.x * MouthReach, -MouthDrop, Look.z * MouthReach);
    }

    public static void ambience(ServerPlayer Subject) {
        if (Subject.isSpectator() || Subject.getRandom().nextInt(AmbientOdds) != 0) {
            return;
        }
        ServerLevel World = Subject.serverLevel();
        Vec3 Mouth = mouth(Subject);
        for (ServerPlayer Watcher : World.players()) {
            if (Watcher != Subject && Watcher.distanceToSqr(Mouth) < 32.0 * 32.0) {
                World.sendParticles(Watcher, Scp444Registry.Glyph.get(), false, Mouth.x, Mouth.y, Mouth.z, 1, 0.08, 0.05, 0.08, 0.004);
            }
        }
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }

    public static void playExcept(ServerPlayer Except, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Except.level().playSound(Except, Except.getX(), Except.getY() + 1.0, Except.getZ(), Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }
}
