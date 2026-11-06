package net.rai.scp_unleashed.radio;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.TreeSet;

public final class RadioCarry {
    public static final int MinChannel = 1;
    public static final int MaxChannel = 99;
    public static final int DefaultChannel = 1;

    private RadioCarry() {
    }

    public static int clampChannel(int Value) {
        return Mth.clamp(Value, MinChannel, MaxChannel);
    }

    public static int wrapChannel(int Value) {
        int Span = MaxChannel - MinChannel + 1;
        return Math.floorMod(Value - MinChannel, Span) + MinChannel;
    }

    public static String label(int Channel) {
        return String.format("%02d", clampChannel(Channel));
    }

    public static boolean isRadio(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getItem() instanceof HandheldRadioItem;
    }

    public static int channel(ItemStack Stack) {
        Integer Value = Stack.get(RadioRegistry.Channel.get());
        return Value == null ? DefaultChannel : clampChannel(Value);
    }

    public static boolean isOn(ItemStack Stack) {
        return isRadio(Stack) && Boolean.TRUE.equals(Stack.get(RadioRegistry.Switched.get()));
    }

    public static void setChannel(ItemStack Stack, int Channel) {
        Stack.set(RadioRegistry.Channel.get(), clampChannel(Channel));
    }

    public static void setOn(ItemStack Stack, boolean On) {
        Stack.set(RadioRegistry.Switched.get(), On);
    }

    public static ItemStack configured(int Channel, boolean On) {
        ItemStack Made = new ItemStack(RadioRegistry.Radio.get());
        setChannel(Made, Channel);
        setOn(Made, On);
        return Made;
    }

    public static boolean hears(Player Listener, int Channel) {
        if (Listener.isSpectator() || !Listener.isAlive()) {
            return false;
        }
        Inventory Pack = Listener.getInventory();
        for (int Slot = 0; Slot < Inventory.getSelectionSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (isOn(Stack) && channel(Stack) == Channel) {
                return true;
            }
        }
        ItemStack Off = Listener.getOffhandItem();
        return isOn(Off) && channel(Off) == Channel;
    }

    public static Set<Integer> channels(Player Listener) {
        Set<Integer> Found = new TreeSet<>();
        if (Listener.isSpectator() || !Listener.isAlive()) {
            return Found;
        }
        Inventory Pack = Listener.getInventory();
        for (int Slot = 0; Slot < Inventory.getSelectionSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (isOn(Stack)) {
                Found.add(channel(Stack));
            }
        }
        if (isOn(Listener.getOffhandItem())) {
            Found.add(channel(Listener.getOffhandItem()));
        }
        return Found;
    }

    public static ItemStack held(Player Speaker) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = Speaker.getItemInHand(Hand);
            if (isOn(Stack)) {
                return Stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack keyed(Player Speaker) {
        if (!Speaker.isUsingItem() || Speaker.isSpectator()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Speaker.getUseItem();
        return isOn(Stack) ? Stack : ItemStack.EMPTY;
    }
}
