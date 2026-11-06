package net.rai.scp_unleashed.z0001;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

public final class Z0001Containment implements ContainedBehaviour {
    public static final Z0001Containment Instance = new Z0001Containment();
    public static final float CloseVolume = 0.5f;

    private Z0001Containment() {
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change != StorageChange.Inserted) {
            return;
        }
        Vec3 Where = Locker.soundCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Z0001Registry.BookClose.get(), SoundSource.BLOCKS, CloseVolume, 0.9f + World.random.nextFloat() * 0.1f);
    }
}
