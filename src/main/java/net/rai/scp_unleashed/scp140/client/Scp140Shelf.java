package net.rai.scp_unleashed.scp140.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp140.Scp140BookItem;
import net.rai.scp_unleashed.scp140.Scp140Motion;
import net.rai.scp_unleashed.scp140.Scp140Registry;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageContainers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp140Shelf {
    public static final long Forget = 40L;
    public static final float Volume = 0.4f;
    private static final Map<BlockPos, Entry> Entries = new HashMap<>();

    private Scp140Shelf() {
    }

    public static Scp140Motion motion(BlockPos Pos, long Now) {
        Entry Found = Entries.computeIfAbsent(Pos.immutable(), Key -> new Entry(new Scp140Motion()));
        Found.Seen = Now;
        return Found.Motion;
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        if (World == null || Game.isPaused()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Map.Entry<BlockPos, Entry>> Cursor = Entries.entrySet().iterator();
        while (Cursor.hasNext()) {
            Map.Entry<BlockPos, Entry> Tracked = Cursor.next();
            BlockPos Pos = Tracked.getKey();
            Entry Found = Tracked.getValue();
            Optional<StorageContainerBlockEntity> Locker = StorageContainers.find(World, Pos);
            if (Now - Found.Seen > Forget || Locker.isEmpty() || !Scp140BookItem.is(Locker.get().peekContents())) {
                Cursor.remove();
                continue;
            }
            Vec3 Centre = Locker.get().soundCentre();
            Scp140Motion.Event Happened = Found.Motion.step(World, Centre, Locker.get().isInteriorShown(), World.getRandom());
            switch (Happened) {
                case Opened -> play(World, Centre, Scp140Registry.OpenLayers);
                case Closed -> play(World, Centre, Scp140Registry.CloseLayers);
                case Turned -> play(World, Centre, Scp140Registry.TurnLayers);
                default -> {
                }
            }
        }
    }

    private static void play(ClientLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playLocalSound(Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, Volume, 0.95f + World.getRandom().nextFloat() * 0.1f, false);
        }
    }

    public static void clear() {
        Entries.clear();
    }

    private static final class Entry {
        private final Scp140Motion Motion;
        private long Seen;

        private Entry(Scp140Motion Motion) {
            this.Motion = Motion;
        }
    }
}
