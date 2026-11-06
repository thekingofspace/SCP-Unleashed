package net.rai.scp_unleashed.guns;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GunReloading {
    public static final float Volume = 0.9F;

    private GunReloading() {
    }

    public static boolean canReload(Player User, ItemStack Stack, GunKind Kind) {
        if (Kind.getStats().usesCharges() || !Kind.usesAmmo() || GunItem.isBusy(Stack) || GunItem.getAmmo(Stack) >= Kind.getStats().getCapacity()) {
            return false;
        }
        return User.getAbilities().instabuild || GunAmmo.count(User, Kind) > 0;
    }

    public static void start(ServerPlayer User) {
        ItemStack Stack = User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || User.isSpectator() || !User.isAlive() || !canReload(User, Stack, Kind)) {
            return;
        }
        long Now = User.level().getGameTime();
        Stack.set(GunsRegistry.Reload.get(), new GunTimer(Now, Now + Kind.getStats().getReloadTicks()));
        GunSounds.playAround(User, GunSounds.CueReloadStart, Kind, Volume);
    }

    public static void finish(ServerPlayer User, ItemStack Stack, GunKind Kind) {
        Stack.remove(GunsRegistry.Reload.get());
        int Missing = Kind.getStats().getCapacity() - GunItem.getAmmo(Stack);
        if (Missing <= 0) {
            return;
        }
        int Taken = User.getAbilities().instabuild ? Missing : GunAmmo.take(User, Kind, Missing);
        if (Taken <= 0) {
            return;
        }
        GunItem.setAmmo(Stack, GunItem.getAmmo(Stack) + Taken);
        GunSounds.playAround(User, GunSounds.CueReloadFinish, Kind, Volume);
    }
}
