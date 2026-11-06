package net.rai.scp_unleashed.scp372;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.scp372.network.Scp372UnlockPayload;

public final class Scp372Unlock {
    public static final int SweepInterval = 40;
    private static volatile boolean ClientUnlocked;

    private Scp372Unlock() {
    }

    public static boolean clientUnlocked() {
        return ClientUnlocked;
    }

    public static void setClientUnlocked(boolean Value) {
        ClientUnlocked = Value;
    }

    public static boolean hasCase(ServerPlayer User) {
        return Investigations.get(User.server).find(User.getUUID(), Scp372Registry.DocumentId).isPresent();
    }

    public static boolean holdsDossier(Player User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.is(ArchiveRegistry.OpenedDocument.get())) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Scp372Registry.DocumentId.equals(Stamp.Document())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnlocked(ServerPlayer User) {
        return Scp372State.get(User.server).isUnlocked(User.getUUID());
    }

    public static boolean unlocked(ServerPlayer User) {
        if (isUnlocked(User)) {
            return true;
        }
        if (!hasCase(User) && !holdsDossier(User)) {
            return false;
        }
        unlock(User);
        return true;
    }

    public static void unlock(ServerPlayer User) {
        if (Scp372State.get(User.server).unlock(User.getUUID())) {
            sync(User);
        }
    }

    public static void sweep(ServerPlayer User) {
        if (!isUnlocked(User) && (hasCase(User) || holdsDossier(User))) {
            unlock(User);
        }
    }

    public static void sync(ServerPlayer User) {
        if (User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(User, new Scp372UnlockPayload(isUnlocked(User)));
    }
}
