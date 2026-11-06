package net.rai.scp_unleashed.scp140;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

public final class Scp140Containment implements ContainedBehaviour {
    public static final Scp140Containment Instance = new Scp140Containment();

    private Scp140Containment() {
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        if (Locker.isVisible()) {
            Scp140Witnesses.stored(World, Locker.getStoragePos());
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change != StorageChange.Inserted) {
            return;
        }
        Vec3 Where = Locker.soundCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Scp140Registry.BookThump.get(), SoundSource.BLOCKS, 0.6f, 0.9f + World.random.nextFloat() * 0.1f);
    }
}
