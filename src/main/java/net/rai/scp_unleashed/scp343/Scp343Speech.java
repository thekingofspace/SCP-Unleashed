package net.rai.scp_unleashed.scp343;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.senses.Senses;

import java.util.List;

public final class Scp343Speech {
    public static final String Prefix = "message.scp_unleashed.scp343.";
    public static final int Lines = 28;
    public static final int Greetings = 4;
    public static final int Partings = 3;
    public static final float Volume = 0.7f;
    public static final int SpeechColour = 0xF2E6C2;

    private Scp343Speech() {
    }

    public static int pick(RandomSource Random, int Count, int Last) {
        int Line = Random.nextInt(Count);
        if (Line == Last && Count > 1) {
            Line = (Line + 1 + Random.nextInt(Count - 1)) % Count;
        }
        return Line;
    }

    public static Component line(int Index) {
        return Component.translatable(Prefix + "line." + Index);
    }

    public static Component greeting(int Index) {
        return Component.translatable(Prefix + "greeting." + Index);
    }

    public static Component parting(int Index) {
        return Component.translatable(Prefix + "parting." + Index);
    }

    public static void say(ServerPlayer Listener, Entity Speaker, Component Spoken) {
        Component Name = Speaker.getDisplayName().copy().withStyle(ChatFormatting.WHITE);
        Component Text = Spoken.copy().withStyle(Look -> Look.withColor(SpeechColour));
        Listener.sendSystemMessage(Senses.heard(Listener, Component.translatable(Prefix + "speech", Name, Text)));
    }

    public static void play(ServerLevel World, Entity Source, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Loudness, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Source.getX(), Source.getY() + 1.2, Source.getZ(), Layer.get(), SoundSource.NEUTRAL, Loudness, Pitch);
        }
    }

    public static void chord(ServerLevel World, Entity Source) {
        play(World, Source, Scp343Registry.SpeakLayers, Volume, 1.0f);
    }
}
