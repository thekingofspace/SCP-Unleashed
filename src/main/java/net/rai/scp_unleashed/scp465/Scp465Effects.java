package net.rai.scp_unleashed.scp465;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp465Effects {
    private Scp465Effects() {
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.NEUTRAL, Volume, Varied);
        }
    }

    public static void play(ServerLevel World, Vec3 At, DeferredHolder<SoundEvent, SoundEvent> Layer, float Volume, float Pitch) {
        play(World, At, List.of(Layer), Volume, Pitch);
    }

    public static void confetti(ServerLevel World, Vec3 At, int Count, double Spread, double Speed) {
        World.sendParticles(Scp465Registry.Confetti.get(), At.x, At.y, At.z, Count, Spread, Spread, Spread, Speed);
    }

    public static void streamers(ServerLevel World, Vec3 At, int Count, double Spread, double Speed) {
        World.sendParticles(Scp465Registry.Streamer.get(), At.x, At.y, At.z, Count, Spread, Spread, Spread, Speed);
    }

    public static void dust(ServerLevel World, Vec3 At, int Count) {
        World.sendParticles(ParticleTypes.POOF, At.x, At.y, At.z, Count, 0.25, 0.45, 0.25, 0.02);
        World.sendParticles(ParticleTypes.WHITE_ASH, At.x, At.y + 0.6, At.z, Count * 2, 0.3, 0.6, 0.3, 0.01);
    }

    public static void hint(Player User, String Key) {
        User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
