package net.rai.scp_unleashed.upgrade.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.rai.scp_unleashed.upgrade.MufflerPayload;
import net.rai.scp_unleashed.upgrade.SoundMuffling;

import java.util.Map;
import java.util.WeakHashMap;

public final class MufflerClient {
    private static final Long2ObjectOpenHashMap<long[]> Machines = new Long2ObjectOpenHashMap<>();
    private static final LongSet Cells = new LongOpenHashSet();
    private static final Map<SoundInstance, Float> Decided = new WeakHashMap<>();
    private static boolean FromServer;

    private MufflerClient() {
    }

    public static void accept(MufflerPayload Payload) {
        if (Payload.Replace()) {
            Machines.clear();
        }
        for (long Core : Payload.Removed()) {
            Machines.remove(Core);
        }
        for (MufflerPayload.Machine Added : Payload.Added()) {
            Machines.put(Added.Core(), Added.Cells());
        }
        Cells.clear();
        for (long[] Footprint : Machines.values()) {
            for (long Cell : Footprint) {
                Cells.add(Cell);
            }
        }
        Decided.clear();
    }

    public static void clear() {
        Machines.clear();
        Cells.clear();
        Decided.clear();
        FromServer = false;
    }

    public static void serverSound(boolean Active) {
        FromServer = Active;
    }

    public static boolean muffles() {
        return !Cells.isEmpty();
    }

    public static float factor(SoundInstance Sound) {
        if (Cells.isEmpty() || Sound == null) {
            return 1.0f;
        }
        Float Known = Decided.get(Sound);
        if (Known != null) {
            return Known;
        }
        float Found = 1.0f;
        if (!FromServer && !Sound.isRelative() && Sound.getAttenuation() != SoundInstance.Attenuation.NONE && !(Sound instanceof EntityBoundSoundInstance)) {
            Found = SoundMuffling.factor(Cells, Sound.getX(), Sound.getY(), Sound.getZ(), Sound.getSource());
        }
        Decided.put(Sound, Found);
        return Found;
    }
}
