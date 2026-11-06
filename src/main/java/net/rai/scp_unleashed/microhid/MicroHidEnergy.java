package net.rai.scp_unleashed.microhid;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class MicroHidEnergy {
    public static final int Capacity = 12000;
    public static final int SpinUpTicks = 24;
    public static final int SpinDrain = 38;
    public static final int FireDrain = 75;
    public static final int DrainEvery = 4;
    public static final int WindDownTicks = 12;
    public static final int EmptyCooldown = 20;
    public static final int ChargeDraw = 40;
    public static final float LowShare = 0.2F;

    private MicroHidEnergy() {
    }

    public static int get(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(MicroHidRegistry.Energy.get(), 0), 0, Capacity);
    }

    public static void set(ItemStack Stack, int Amount) {
        Stack.set(MicroHidRegistry.Energy.get(), Mth.clamp(Amount, 0, Capacity));
    }

    public static int drain(ItemStack Stack, int Amount) {
        int Left = Math.max(0, get(Stack) - Amount);
        set(Stack, Left);
        return Left;
    }

    public static int fill(ItemStack Stack, int Amount) {
        int Before = get(Stack);
        int After = Math.min(Capacity, Before + Math.max(0, Amount));
        set(Stack, After);
        return After - Before;
    }

    public static float fraction(ItemStack Stack) {
        return get(Stack) / (float) Capacity;
    }

    public static int percent(ItemStack Stack) {
        int Stored = get(Stack);
        if (Stored <= 0) {
            return 0;
        }
        return Mth.clamp((int) Math.floor(Stored * 100.0 / Capacity), 1, 100);
    }

    public static boolean canFire(ItemStack Stack) {
        return get(Stack) > 0;
    }

    public static boolean isFull(ItemStack Stack) {
        return get(Stack) >= Capacity;
    }

    public static float fireSeconds() {
        return (Capacity - SpinDrain * SpinUpTicks) / (float) FireDrain / 20.0F;
    }

    public static float chargeSeconds() {
        return Capacity / (float) ChargeDraw / 20.0F;
    }

    public static float spinSeconds() {
        return SpinUpTicks / 20.0F;
    }
}
