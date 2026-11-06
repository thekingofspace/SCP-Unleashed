package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ForcefieldSounds {
    public static final int Bucket = 3;
    public static final int Window = 2;
    public static final float PitchJitter = 0.06f;
    private static final Map<ServerLevel, Map<Long, Long>> Recent = new WeakHashMap<>();

    private ForcefieldSounds() {
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (Layers.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        long Key = key(Layers.get(0).getId().hashCode(), At);
        Map<Long, Long> Heard = Recent.computeIfAbsent(World, Owner -> new HashMap<>());
        Long Last = Heard.get(Key);
        if (Last != null && Now - Last < Window) {
            return;
        }
        Heard.put(Key, Now);
        if (Heard.size() > 256) {
            Heard.values().removeIf(Tick -> Now - Tick >= Window);
        }
        float Shift = Pitch * (1.0f - PitchJitter * 0.5f + World.getRandom().nextFloat() * PitchJitter);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Volume, Shift);
        }
    }

    private static long key(int Sound, Vec3 At) {
        long Cell = BlockPos.asLong(Mth.floor(At.x) >> Bucket, Mth.floor(At.y) >> Bucket, Mth.floor(At.z) >> Bucket);
        return Cell * 31L + Sound;
    }
}
