package net.rai.scp_unleashed.scp024;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp024.network.Scp024CuePayload;
import org.jetbrains.annotations.Nullable;

public final class Scp024Sounds {
    private Scp024Sounds() {
    }

    public static void cue(@Nullable ServerPlayer Listener, Scp024Cue Cue) {
        if (Listener != null && Cue != Scp024Cue.None) {
            PacketDistributor.sendToPlayer(Listener, new Scp024CuePayload(Cue.ordinal()));
        }
    }

    public static void play(ServerLevel World, Vec3 Spot, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, Spot.x, Spot.y, Spot.z, Sound, SoundSource.HOSTILE, Volume, Pitch);
    }

    public static void blast(ServerLevel World, Vec3 Spot, float Scale) {
        float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
        play(World, Spot, Scp024Registry.Blast.get(), 1.4f * Scale, Pitch);
        play(World, Spot, Scp024Registry.BlastFar.get(), 0.8f * Scale, Pitch * 0.8f);
        play(World, Spot, Scp024Registry.Crumble.get(), 0.6f * Scale, 0.7f);
    }
}
