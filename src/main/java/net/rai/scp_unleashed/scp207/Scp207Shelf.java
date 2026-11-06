package net.rai.scp_unleashed.scp207;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp207Shelf implements ContainedBehaviour {
    public static final Scp207Shelf Instance = new Scp207Shelf();
    public static final int SmallCapacity = 4;
    public static final int TallCapacity = 8;
    public static final String Key = "message.scp_unleashed.scp207.shelf_full";
    public static final String TooManyKey = "message.scp_unleashed.scp207.shelf_too_many";

    private Scp207Shelf() {
    }

    public static int capacity(StorageView Locker) {
        return Locker.isTall() ? TallCapacity : SmallCapacity;
    }

    @Nullable
    @Override
    public Component refuse(StorageView Locker, ItemStack Offered) {
        int Size = Scp207Rack.of(Offered).size();
        return Size > capacity(Locker) ? Component.translatable(TooManyKey, Size, capacity(Locker)) : null;
    }

    @Nullable
    @Override
    public ItemStack absorb(StorageView Locker, ItemStack Stored, ItemStack Held, Player User) {
        if (Stored.isEmpty() || !Scp207BottleItem.is(Stored) || !Scp207BottleItem.is(Held) || Scp207Rack.isRack(Held)) {
            return null;
        }
        Scp207Rack Current = Scp207Rack.of(Stored);
        if (Current.size() >= capacity(Locker)) {
            return null;
        }
        Scp207Rack Grown = Current.plus(Scp207BottleItem.isOne(Held));
        Held.consume(1, User);
        return Grown.stack();
    }

    @Override
    public List<ItemStack> unpack(ItemStack Stored) {
        return Scp207Rack.of(Stored).bottles();
    }

    @Nullable
    @Override
    public Split takeOne(ItemStack Stored) {
        Scp207Rack Current = Scp207Rack.of(Stored);
        if (Current.size() <= 1) {
            return null;
        }
        return new Split(Scp207BottleItem.stack(Current.isBlue(Current.size() - 1)), Current.withoutLast().stack());
    }

    @Nullable
    @Override
    public Component occupied(StorageView Locker, ItemStack Stored, ItemStack Held) {
        if (!Scp207BottleItem.is(Held) || !Scp207BottleItem.is(Stored)) {
            return null;
        }
        Scp207Rack Current = Scp207Rack.of(Stored);
        if (Current.size() < capacity(Locker)) {
            return null;
        }
        return Component.translatable(Key, Current.size());
    }
}
