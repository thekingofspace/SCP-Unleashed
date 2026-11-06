package net.rai.scp_unleashed.scp303;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.scp303.network.Scp303UnlockPayload;

public final class Scp303Unlock {
    public static final int SweepInterval = 40;
    private static volatile boolean ClientUnlocked;

    private Scp303Unlock() {
    }

    public static boolean clientUnlocked() {
        return ClientUnlocked;
    }

    public static void setClientUnlocked(boolean Value) {
        ClientUnlocked = Value;
    }

    public static boolean hasCase(ServerPlayer User) {
        return Investigations.get(User.server).find(User.getUUID(), Scp303Registry.DocumentId).isPresent();
    }

    public static boolean holdsDossier(Player User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.is(ArchiveRegistry.OpenedDocument.get())) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Scp303Registry.DocumentId.equals(Stamp.Document())) {
                return true;
            }
        }
        return false;
    }

    public static boolean unlocked(ServerPlayer User) {
        Scp303State State = Scp303State.get(User.server);
        if (State.isUnlocked(User.getUUID())) {
            return true;
        }
        if (!hasCase(User) && !holdsDossier(User)) {
            return false;
        }
        unlock(User);
        return true;
    }

    public static boolean canBind(ServerPlayer User) {
        return Scp303State.get(User.server).isActive() || unlocked(User);
    }

    public static boolean bindable(ServerPlayer User) {
        Scp303State State = Scp303State.get(User.server);
        return State.isActive() || State.isUnlocked(User.getUUID());
    }

    public static void unlock(ServerPlayer User) {
        if (Scp303State.get(User.server).unlock(User.getUUID())) {
            sync(User);
        }
    }

    public static void sweep(ServerPlayer User) {
        if (!Scp303State.get(User.server).isUnlocked(User.getUUID()) && (hasCase(User) || holdsDossier(User))) {
            unlock(User);
        }
    }

    public static void sync(ServerPlayer User) {
        if (User.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(User, new Scp303UnlockPayload(bindable(User)));
    }
}
