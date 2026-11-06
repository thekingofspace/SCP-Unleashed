package net.rai.scp_unleashed.scp012;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp012Composer {
    public static final int PaperInterval = 5;
    public static final String Prefix = "message.scp_unleashed.scp012.";
    private static final Map<UUID, PaperWatch> Watches = new HashMap<>();

    private Scp012Composer() {
    }

    public static boolean isComposer(LivingEntity User) {
        return Boolean.TRUE.equals(User.getExistingDataOrNull(Scp012Registry.Composer.get()));
    }

    public static boolean claim(ServerPlayer User) {
        if (isComposer(User)) {
            return false;
        }
        User.setData(Scp012Registry.Composer.get(), Boolean.TRUE);
        PaperWatch Watch = watch(User);
        Watch.Paper = countPaper(User);
        Watch.Dirty = false;
        User.level().playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp012Registry.Complete.get(), SoundSource.PLAYERS, 0.8f, 0.92f);
        User.playNotifySound(Scp012Registry.Note.get(), SoundSource.PLAYERS, 0.5f, 0.5f);
        User.playNotifySound(Scp012Registry.NoteBell.get(), SoundSource.PLAYERS, 0.4f, 0.53f);
        User.displayClientMessage(Component.translatable(Prefix + "claimed").withStyle(ChatFormatting.DARK_RED), false);
        Scp012Exposure.sync(User);
        return true;
    }

    public static boolean release(ServerPlayer User) {
        if (!isComposer(User)) {
            return false;
        }
        User.removeData(Scp012Registry.Composer.get());
        detach(User);
        Scp012Exposure.sync(User);
        return true;
    }

    public static void tick(ServerPlayer User) {
        if (Math.floorMod(User.tickCount + User.getId(), PaperInterval) != 0) {
            return;
        }
        if (!isComposer(User)) {
            if (Watches.containsKey(User.getUUID())) {
                detach(User);
            }
            return;
        }
        PaperWatch Watch = watch(User);
        if (!Watch.Dirty && Watch.Paper >= 0 && User.containerMenu == User.inventoryMenu) {
            return;
        }
        Watch.Dirty = false;
        int Count = countPaper(User);
        if (Watch.Paper >= 0 && Count > Watch.Paper && User.isAlive()) {
            transcribe(User);
            Count = countPaper(User);
        }
        Watch.Paper = Count;
    }

    public static void pickedUp(ServerPlayer User, ItemStack Original) {
        if (!Original.is(Items.PAPER) || !isComposer(User) || !User.isAlive()) {
            return;
        }
        PaperWatch Watch = watch(User);
        transcribe(User);
        Watch.Paper = countPaper(User);
        Watch.Dirty = false;
    }

    private static void transcribe(ServerPlayer User) {
        if (!consumePaper(User)) {
            return;
        }
        ItemHandlerHelper.giveItemToPlayer(User, Scp012CompositionItem.blank());
        User.level().playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp012Registry.Copy.get(), SoundSource.PLAYERS, 0.6f, 0.9f + User.getRandom().nextFloat() * 0.1f);
        User.playNotifySound(Scp012Registry.Note.get(), SoundSource.PLAYERS, 0.35f, 0.59f + User.getRandom().nextInt(4) * 0.06f);
        User.displayClientMessage(Component.translatable(Prefix + "transcribed").withStyle(ChatFormatting.DARK_RED), true);
    }

    private static boolean consumePaper(ServerPlayer User) {
        Inventory Pack = User.getInventory();
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (Stack.is(Items.PAPER)) {
                Stack.shrink(1);
                if (Stack.isEmpty()) {
                    Pack.setItem(Slot, ItemStack.EMPTY);
                }
                Pack.setChanged();
                return true;
            }
        }
        ItemStack Carried = User.containerMenu.getCarried();
        if (Carried.is(Items.PAPER)) {
            Carried.shrink(1);
            User.containerMenu.setCarried(Carried.isEmpty() ? ItemStack.EMPTY : Carried);
            User.containerMenu.broadcastChanges();
            return true;
        }
        return false;
    }

    public static int countPaper(ServerPlayer User) {
        int Count = 0;
        Inventory Pack = User.getInventory();
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (Stack.is(Items.PAPER)) {
                Count += Stack.getCount();
            }
        }
        ItemStack Carried = User.containerMenu.getCarried();
        if (Carried.is(Items.PAPER)) {
            Count += Carried.getCount();
        }
        return Count;
    }

    private static PaperWatch watch(ServerPlayer User) {
        PaperWatch Watch = Watches.computeIfAbsent(User.getUUID(), Key -> new PaperWatch());
        if (Watch.Menu != User.inventoryMenu) {
            if (Watch.Menu != null) {
                Watch.Menu.removeSlotListener(Watch);
            }
            Watch.Menu = User.inventoryMenu;
            Watch.Menu.addSlotListener(Watch);
            Watch.Dirty = true;
        }
        return Watch;
    }

    private static void detach(Player User) {
        PaperWatch Watch = Watches.remove(User.getUUID());
        if (Watch != null && Watch.Menu != null) {
            Watch.Menu.removeSlotListener(Watch);
        }
    }

    public static void forget(Player User) {
        detach(User);
    }

    public static void clear() {
        Watches.clear();
    }

    private static final class PaperWatch implements ContainerListener {
        private AbstractContainerMenu Menu;
        private boolean Dirty = true;
        private int Paper = -1;

        @Override
        public void slotChanged(AbstractContainerMenu Changed, int Slot, ItemStack Stack) {
            Dirty = true;
        }

        @Override
        public void dataChanged(AbstractContainerMenu Changed, int Slot, int Value) {
        }
    }
}
