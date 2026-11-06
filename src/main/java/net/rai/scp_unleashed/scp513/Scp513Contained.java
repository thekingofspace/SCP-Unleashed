package net.rai.scp_unleashed.scp513;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlockEntity;

public final class Scp513Contained implements ContainedBehaviour {
    public static final Scp513Contained Instance = new Scp513Contained();
    public static final float SettleVolume = 0.45f;

    private Scp513Contained() {
    }

    public static boolean isSealed(StorageContainerBlockEntity Locker) {
        return Locker instanceof GelStorageContainerBlockEntity Gel && Gel.isSealed();
    }

    public static void check(ServerLevel World, StorageContainerBlockEntity Locker) {
        ItemStack Stored = Locker.peekContents();
        if (!Scp513BellItem.is(Stored)) {
            return;
        }
        if (isSealed(Locker)) {
            if (Scp513Chimes.hold(World, Stored)) {
                Locker.setChanged();
            }
            return;
        }
        Scp513Chimes.Pulse Result = Scp513Chimes.pulse(World, Stored);
        Locker.setChanged();
        Scp513Chimes.answer(World, Result, Locker.soundCentre(), null);
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        if (Math.floorMod(World.getGameTime() + Locker.getBlockPos().asLong(), (long) Scp513Chimes.CheckEvery) == 0L) {
            check(World, Locker);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change != StorageChange.Inserted) {
            return;
        }
        Vec3 Where = Locker.soundCentre();
        Scp513Registry.playLayers(World, Where, Scp513Registry.ThunkLayers, SoundSource.BLOCKS, SettleVolume, 0.8f + World.random.nextFloat() * 0.1f);
    }
}
