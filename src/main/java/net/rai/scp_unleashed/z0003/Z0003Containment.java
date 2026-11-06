package net.rai.scp_unleashed.z0003;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

public final class Z0003Containment implements ContainedBehaviour {
    public static final Z0003Containment Instance = new Z0003Containment();
    public static final float ClickVolume = 0.45f;

    private Z0003Containment() {
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change != StorageChange.Inserted || !Z0003DeviceItem.isArmed(Subject)) {
            return;
        }
        ItemStack Calmed = Subject.copy();
        Z0003DeviceItem.disarm(Calmed);
        Locker.setContents(Calmed);
        Vec3 Where = Locker.soundCentre();
        Z0003Registry.play(World, Where, Z0003Registry.ArmLayers, SoundSource.BLOCKS, ClickVolume, 0.7f);
    }
}
