package net.rai.scp_unleashed.scp131;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

import java.util.List;

public final class Scp131Locker implements ContainedBehaviour {
    public static final Scp131Locker Instance = new Scp131Locker();
    public static final int ChirpOdds = 900;
    public static final int SadChirpOdds = 1800;
    public static final float OpenVolume = 0.45f;
    public static final float ClosedVolume = 0.22f;

    private Scp131Locker() {
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        boolean Sad = Scp131Grief.isGrieving(World);
        if (World.random.nextInt(Sad ? SadChirpOdds : ChirpOdds) != 0) {
            return;
        }
        ItemStack Stored = Locker.peekContents();
        if (!Scp131PodItem.is(Stored)) {
            return;
        }
        Scp131PodData Data = Scp131PodData.of(Stored);
        float Pitch = Scp131Pod.voiceFor(Data.Colour(), Data.Voice()) * (Sad ? Scp131Pod.SadPitch : 1.0f) * (0.95f + World.random.nextFloat() * 0.1f);
        float Volume = Locker.isOpen() ? OpenVolume : ClosedVolume;
        Vec3 Where = Locker.soundCentre();
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Sad ? Scp131Registry.SadLayers : Scp131Registry.ChirpLayers;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }
}
