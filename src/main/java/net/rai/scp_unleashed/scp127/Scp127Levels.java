package net.rai.scp_unleashed.scp127;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import org.jetbrains.annotations.Nullable;

public final class Scp127Levels {
    public static final int MaxLevel = 3;
    public static final int[] KillsNeeded = {0, 25, 75};
    public static final float[] Damage = {8.0F, 13.0F, 20.0F};
    public static final int[] RoundsPerCycle = {1, 3, 5};
    public static final int CycleTicks = 40;

    private Scp127Levels() {
    }

    public static int capacity() {
        return GunKind.Scp127.getStats().getCapacity();
    }

    public static int rankOf(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(Scp127Registry.Rank.get(), 1), 1, MaxLevel);
    }

    public static int killsOf(ItemStack Stack) {
        return Math.max(0, Stack.getOrDefault(Scp127Registry.Kills.get(), 0));
    }

    public static float damageAt(int Rank) {
        return Damage[Mth.clamp(Rank, 1, MaxLevel) - 1];
    }

    public static int roundsAt(int Rank) {
        return RoundsPerCycle[Mth.clamp(Rank, 1, MaxLevel) - 1];
    }

    public static int killsFor(int Rank) {
        return KillsNeeded[Mth.clamp(Rank, 1, MaxLevel) - 1];
    }

    public static int rankForKills(int Kills) {
        int Rank = 1;
        for (int Index = 1; Index < MaxLevel; Index++) {
            if (Kills >= KillsNeeded[Index]) {
                Rank = Index + 1;
            }
        }
        return Rank;
    }

    public static float progress(ItemStack Stack) {
        int Rank = rankOf(Stack);
        if (Rank >= MaxLevel) {
            return 1.0F;
        }
        int Floor = killsFor(Rank);
        int Ceiling = killsFor(Rank + 1);
        return Mth.clamp((killsOf(Stack) - Floor) / (float) Math.max(1, Ceiling - Floor), 0.0F, 1.0F);
    }

    public static Component numeral(int Rank) {
        return Component.translatable("enchantment.level." + Mth.clamp(Rank, 1, MaxLevel));
    }

    @Nullable
    public static Long regrowthStart(ItemStack Stack) {
        return Stack.get(Scp127Registry.Regrowth.get());
    }

    public static float regrowthProgress(ItemStack Stack, long Now, float Partial) {
        Long Start = regrowthStart(Stack);
        if (Start == null || GunItem.getAmmo(Stack) >= capacity()) {
            return 0.0F;
        }
        return Mth.clamp((Now - Start + Partial) / CycleTicks, 0.0F, 1.0F);
    }

    public static boolean regrow(ItemStack Stack, long Now) {
        int Capacity = capacity();
        int Rounds = GunItem.getAmmo(Stack);
        Long Start = regrowthStart(Stack);
        if (Rounds >= Capacity) {
            if (Start != null) {
                Stack.remove(Scp127Registry.Regrowth.get());
            }
            return false;
        }
        if (Start == null || Start > Now) {
            Stack.set(Scp127Registry.Regrowth.get(), Now);
            return false;
        }
        long Cycles = (Now - Start) / CycleTicks;
        if (Cycles <= 0L) {
            return false;
        }
        long Grown = Math.min((long) Capacity, Cycles * roundsAt(rankOf(Stack)));
        int Filled = (int) Math.min(Capacity, Rounds + Grown);
        GunItem.setAmmo(Stack, Filled);
        if (Filled >= Capacity) {
            Stack.remove(Scp127Registry.Regrowth.get());
        } else {
            Stack.set(Scp127Registry.Regrowth.get(), Start + Cycles * CycleTicks);
        }
        return true;
    }
}
