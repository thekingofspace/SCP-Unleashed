package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp105.Scp105Iris;
import net.rai.scp_unleashed.scp105.Scp105Registry;
import net.rai.scp_unleashed.scp105.Scp105Sounds;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class Scp105Photo096Gift {
    public static final int Odds = 96;
    public static final int SweepInterval = 40;
    public static final String SpeechKey = Scp105Iris.Prefix + "gift.shy";
    @Nullable
    private static BooleanSupplier ForcedRoll;

    private Scp105Photo096Gift() {
    }

    static void forceRoll(@Nullable BooleanSupplier Hook) {
        ForcedRoll = Hook;
    }

    public static boolean rolls(RandomSource Random) {
        BooleanSupplier Hook = ForcedRoll;
        return Hook != null ? Hook.getAsBoolean() : Random.nextInt(Odds) == 0;
    }

    public static boolean hasCase(MinecraftServer Server, UUID Reader) {
        return Investigations.get(Server).find(Reader, Scp096Registry.DocumentId).isPresent();
    }

    public static boolean holdsDossier(Player User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.is(ArchiveRegistry.OpenedDocument.get())) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Scp096Registry.DocumentId.equals(Stamp.Document())) {
                return true;
            }
        }
        return false;
    }

    public static boolean unlocked(ServerPlayer User) {
        Scp105Photo096Record Record = Scp105Photo096Record.get(User.server);
        UUID Reader = User.getUUID();
        if (Record.isUnlocked(Reader)) {
            return true;
        }
        if (!hasCase(User.server, Reader) && !holdsDossier(User)) {
            return false;
        }
        Record.unlock(Reader);
        return true;
    }

    public static void sweep(ServerPlayer User) {
        Scp105Photo096Record Record = Scp105Photo096Record.get(User.server);
        if (!Record.isUnlocked(User.getUUID()) && hasCase(User.server, User.getUUID())) {
            Record.unlock(User.getUUID());
        }
    }

    public static boolean eligible(ServerPlayer User) {
        return !Scp105Photo096Record.get(User.server).isGiven() && unlocked(User);
    }

    public static boolean inReach(Scp105Iris Iris, ServerPlayer User) {
        return User.isAlive() && !User.isSpectator() && User.level() == Iris.level() && User.distanceToSqr(Iris) < Scp105Iris.GiftReach * Scp105Iris.GiftReach;
    }

    public static boolean handOver(ServerLevel World, Scp105Iris Iris, @Nullable Player Target) {
        if (!(Target instanceof ServerPlayer Receiver) || !inReach(Iris, Receiver) || !eligible(Receiver) || !rolls(Iris.getRandom())) {
            return false;
        }
        if (!Scp105Photo096Record.get(World.getServer()).give(Receiver.getUUID(), Receiver.getGameProfile().getName(), World.getGameTime())) {
            return false;
        }
        ItemStack Photo = Scp105Photo096Item.create();
        Vec3 Hand = Iris.handPosition();
        Scp105Sounds.play(World, Hand, Scp105Registry.GiveLayers, SoundSource.NEUTRAL, 0.8f, 0.85f);
        Iris.say(Receiver, Component.translatable(SpeechKey));
        if (Receiver.getInventory().add(Photo)) {
            return true;
        }
        ItemEntity Drop = new ItemEntity(World, Receiver.getX(), Receiver.getY() + 0.3, Receiver.getZ(), Photo);
        Drop.setDeltaMovement(Vec3.ZERO);
        Drop.setTarget(Receiver.getUUID());
        Drop.setUnlimitedLifetime();
        Drop.setNoPickUpDelay();
        World.addFreshEntity(Drop);
        return true;
    }
}
