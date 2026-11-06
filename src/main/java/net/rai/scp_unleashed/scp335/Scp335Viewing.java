package net.rai.scp_unleashed.scp335;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

public final class Scp335Viewing {
    private Scp335Viewing() {
    }

    public static void viewed(ServerPlayer User, ComputerBlockEntity Computer, @Nullable UploadPortBlockEntity Port, ComputerFile File) {
        if (!Scp335Image.isOriginal(File)) {
            return;
        }
        Scp335Episodes.playAt(User.serverLevel(), Vec3.atBottomCenterOf(Computer.getBlockPos()), Scp335Registry.StaticLayers, 0.9f, 1.0f);
        if (!Scp335Episodes.canBegin(User) || Senses.refuseSight(User) || AntiMemetic.isProtected(User)) {
            return;
        }
        Scp335Episodes.begin(User, Computer.getBlockPos());
    }
}
