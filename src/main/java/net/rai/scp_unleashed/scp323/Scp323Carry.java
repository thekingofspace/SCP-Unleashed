package net.rai.scp_unleashed.scp323;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassPack;
import net.rai.scp_unleashed.scp148.Scp148Shield;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp323Carry {
    public static final int Step = 5;
    public static final int WarningTicks = 60;
    public static final int EquipTicks = 100;
    public static final int OffhandSlot = 40;
    public static final int MainSlots = 36;
    private static final Map<UUID, Integer> Timers = new HashMap<>();
    private static final Set<UUID> Warned = new HashSet<>();

    private Scp323Carry() {
    }

    public static void tick(ServerPlayer User) {
        if ((User.server.getTickCount() + User.getId()) % Step != 0) {
            return;
        }
        if (!Scp323Night.isNight(User.level()) || !Scp323Sight.isDrawable(User) || !carries(User) || Scp148Shield.blocks(User)) {
            reset(User);
            return;
        }
        int Timer = Timers.merge(User.getUUID(), Step, Integer::sum);
        if (Timer >= WarningTicks && Warned.add(User.getUUID())) {
            User.playNotifySound(Scp323Registry.Compel.get(), SoundSource.HOSTILE, 0.9f, 0.9f + User.getRandom().nextFloat() * 0.1f);
            User.displayClientMessage(Component.translatable(Scp323Possession.Prefix + "compel").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
        if (Timer >= EquipTicks) {
            ItemStack Taken = take(User);
            reset(User);
            if (!Taken.isEmpty()) {
                Scp323Possession.equip(User, Taken);
            }
        }
    }

    public static void tick(Mob Person) {
        if ((Person.tickCount + Person.getId()) % Step != 0) {
            return;
        }
        if (!Scp323Night.isNight(Person.level()) || !Scp323Sight.isDrawable(Person) || !(Person.level() instanceof ServerLevel World) || takeFrom(Person, false).isEmpty()
                || Scp148Shield.blocks(Person)) {
            reset(Person);
            return;
        }
        int Timer = Timers.merge(Person.getUUID(), Step, Integer::sum);
        if (Timer >= EquipTicks) {
            ItemStack Taken = takeFrom(Person, true);
            reset(Person);
            if (!Taken.isEmpty()) {
                Scp323Possession.seize(World, Person, Taken);
            }
        }
    }

    private static ItemStack takeFrom(Mob Person, boolean Remove) {
        if (Person instanceof DClass Subject) {
            Container Pack = Subject.pack();
            for (int Index = 0; Index < Pack.getContainerSize(); Index++) {
                if (DClassPack.slotFor(Index) != EquipmentSlot.HEAD && Scp323SkullItem.is(Pack.getItem(Index))) {
                    return Remove ? Pack.removeItemNoUpdate(Index) : Pack.getItem(Index);
                }
            }
            return ItemStack.EMPTY;
        }
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            ItemStack Stack = Person.getItemBySlot(Slot);
            if (Slot != EquipmentSlot.HEAD && Scp323SkullItem.is(Stack)) {
                if (Remove) {
                    Person.setItemSlot(Slot, ItemStack.EMPTY);
                }
                return Stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean carries(Player User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < MainSlots; Slot++) {
            if (Scp323SkullItem.is(Pockets.getItem(Slot))) {
                return true;
            }
        }
        return Scp323SkullItem.is(Pockets.getItem(OffhandSlot)) || Scp323SkullItem.is(User.containerMenu.getCarried());
    }

    private static ItemStack take(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        int Selected = Pockets.selected;
        if (Scp323SkullItem.is(Pockets.getItem(Selected))) {
            return Pockets.removeItemNoUpdate(Selected);
        }
        if (Scp323SkullItem.is(Pockets.getItem(OffhandSlot))) {
            return Pockets.removeItemNoUpdate(OffhandSlot);
        }
        for (int Slot = 0; Slot < MainSlots; Slot++) {
            if (Scp323SkullItem.is(Pockets.getItem(Slot))) {
                return Pockets.removeItemNoUpdate(Slot);
            }
        }
        ItemStack Cursor = User.containerMenu.getCarried();
        if (Scp323SkullItem.is(Cursor)) {
            User.containerMenu.setCarried(ItemStack.EMPTY);
            return Cursor;
        }
        return ItemStack.EMPTY;
    }

    public static float compel(Player User) {
        Integer Timer = Timers.get(User.getUUID());
        return Timer == null ? 0.0f : Math.min(1.0f, (float) Timer / EquipTicks);
    }

    public static void reset(Entity Holder) {
        Timers.remove(Holder.getUUID());
        Warned.remove(Holder.getUUID());
    }

    public static void clear() {
        Timers.clear();
        Warned.clear();
    }
}
