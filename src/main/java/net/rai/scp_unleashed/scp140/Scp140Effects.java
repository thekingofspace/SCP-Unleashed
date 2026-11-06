package net.rai.scp_unleashed.scp140;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;

import java.util.List;

public final class Scp140Effects {
    public static final double NoticeReach = 24.0;
    public static final int BurstCount = 26;
    public static final int MistCount = 10;
    public static final float WrittenVolume = 1.0f;
    public static final float WhisperVolume = 0.8f;

    private Scp140Effects() {
    }

    public static void pageWritten(ServerLevel World, BlockPos Origin, int Page) {
        Vec3 Centre = Vec3.atBottomCenterOf(Origin).add(0.0, 0.35, 0.0);
        RandomSource Random = World.random;
        play(World, Centre, Scp140Registry.WrittenLayers, WrittenVolume, 0.94f + Random.nextFloat() * 0.08f);
        World.sendParticles(Scp140Registry.Ink.get(), Centre.x, Centre.y, Centre.z, BurstCount, 0.22, 0.12, 0.22, 0.02);
        World.sendParticles(Scp140Registry.Ink.get(), Centre.x, Centre.y + 0.4, Centre.z, MistCount, 0.35, 0.3, 0.35, 0.0);
        Component Line = Component.translatable(noticeKey(Page)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        for (ServerPlayer Viewer : World.players()) {
            if (!Viewer.isSpectator() && Viewer.position().distanceToSqr(Centre) <= NoticeReach * NoticeReach) {
                Viewer.displayClientMessage(Line, true);
            }
        }
    }

    public static String noticeKey(int Page) {
        if (Page >= Scp140Pages.Total) {
            return "message.scp_unleashed.scp140.written_last";
        }
        if (Page >= Scp140Pages.Awakening) {
            return "message.scp_unleashed.scp140.written_return";
        }
        return "message.scp_unleashed.scp140.written";
    }

    public static void whisper(ServerLevel World, Vec3 Where) {
        float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp140Registry.WhisperLayers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.AMBIENT, WhisperVolume, Pitch);
        }
    }

    public static void play(ServerLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }
}
