package net.rai.scp_unleashed.scp465.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp465.Scp465BoxBlockEntity;
import net.rai.scp_unleashed.scp465.Scp465Music;
import net.rai.scp_unleashed.scp465.Scp465Playhead;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp465MusicPlayer {
    public static final double HearRange = 56.0;
    public static final double NanosPerSecond = 1.0e9;
    private static final Map<Scp465BoxBlockEntity, Track> Tracks = new WeakHashMap<>();
    private static final RandomSource Random = RandomSource.create();
    private static final List<Scp465Music.Note> Pending = new ArrayList<>();
    private static long LastFrame;

    private record Track(int Party, Scp465Playhead Head) {
    }

    private Scp465MusicPlayer() {
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        long Nanos = Util.getNanos();
        double Seconds = LastFrame == 0L ? 0.0 : (Nanos - LastFrame) / NanosPerSecond;
        LastFrame = Nanos;
        if (Game.level == null || Game.player == null || Game.isPaused()) {
            return;
        }
        TickRateManager Rates = Game.level.tickRateManager();
        double Elapsed = Rates.runsNormally() ? Seconds * Rates.tickrate() : 0.0;
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long Now = Game.level.getGameTime();
        Iterator<Scp465BoxBlockEntity> Boxes = Scp465BoxBlockEntity.clientBoxes().iterator();
        while (Boxes.hasNext()) {
            Scp465BoxBlockEntity Carton = Boxes.next();
            if (Carton.isRemoved() || Carton.getLevel() != Game.level) {
                Tracks.remove(Carton);
                Boxes.remove();
                continue;
            }
            BlockPos Booth = Carton.booth();
            if (!Carton.partying() || Booth == null) {
                Tracks.remove(Carton);
                continue;
            }
            Vec3 Source = Vec3.atCenterOf(Booth);
            if (Game.player.position().distanceToSqr(Source) > HearRange * HearRange) {
                Tracks.remove(Carton);
                continue;
            }
            double Target = Now - Carton.musicStart() + Partial;
            if (Target < 0.0) {
                Tracks.remove(Carton);
                continue;
            }
            Track Playing = Tracks.get(Carton);
            if (Playing == null || Playing.Party() != Carton.partyId()) {
                Playing = new Track(Carton.partyId(), new Scp465Playhead());
                Tracks.put(Carton, Playing);
            }
            int Step = Playing.Head().advance(Elapsed, Target);
            if (Step == Scp465Playhead.Silent) {
                continue;
            }
            Pending.clear();
            Scp465Music.notes(Step, Pending);
            for (Scp465Music.Note Each : Pending) {
                Game.getSoundManager().play(new SimpleSoundInstance(Each.Voice().sound().get(), SoundSource.RECORDS, Each.Volume(), Each.Pitch(), Random,
                        Source.x, Source.y, Source.z));
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Tracks.clear();
        Scp465BoxBlockEntity.clientBoxes().clear();
    }
}
