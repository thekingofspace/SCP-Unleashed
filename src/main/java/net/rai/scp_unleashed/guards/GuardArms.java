package net.rai.scp_unleashed.guards;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.guns.npc.NpcAmmoSource;
import net.rai.scp_unleashed.guns.npc.NpcGunner;

public final class GuardArms {
    public static final double MeleeThreshold = 2.0;

    private GuardArms() {
    }

    public static int gunSlot(DClass Guard) {
        NonNullList<ItemStack> Main = Guard.main();
        int Held = Guard.selected();
        if (usableGun(Guard, Main.get(Held))) {
            return Held;
        }
        for (int Slot = 0; Slot < Main.size(); Slot++) {
            if (usableGun(Guard, Main.get(Slot))) {
                return Slot;
            }
        }
        return -1;
    }

    public static boolean usableGun(DClass Guard, ItemStack Stack) {
        return NpcGunner.isGun(Stack) && !NpcGunner.dry(Guard, Stack);
    }

    public static int meleeSlot(DClass Guard) {
        NonNullList<ItemStack> Main = Guard.main();
        int Best = -1;
        double BestDamage = MeleeThreshold - 1.0E-3;
        for (int Slot = 0; Slot < Main.size(); Slot++) {
            ItemStack Stack = Main.get(Slot);
            if (Stack.isEmpty() || NpcGunner.isGun(Stack)) {
                continue;
            }
            double Damage = meleeDamage(Stack) + (Slot == Guard.selected() ? 1.0E-4 : 0.0);
            if (Damage > BestDamage) {
                Best = Slot;
                BestDamage = Damage;
            }
        }
        return Best;
    }

    public static double meleeDamage(ItemStack Stack) {
        double[] Total = {0.0};
        Stack.forEachModifier(EquipmentSlot.MAINHAND, (Stat, Change) -> {
            if (Stat.is(Attributes.ATTACK_DAMAGE) && Change.operation() == AttributeModifier.Operation.ADD_VALUE) {
                Total[0] += Change.amount();
            }
        });
        return Total[0];
    }

    public static int emptyHotbar(DClass Guard) {
        NonNullList<ItemStack> Main = Guard.main();
        if (Main.get(Guard.selected()).isEmpty()) {
            return Guard.selected();
        }
        for (int Slot = 0; Slot < DClass.HotbarSize; Slot++) {
            if (Main.get(Slot).isEmpty()) {
                return Slot;
            }
        }
        return -1;
    }

    public static void equip(DClass Guard, int Slot) {
        if (Slot < 0 || Slot >= DClass.MainSize || Slot == Guard.selected() || Guard.isUsingItem()) {
            return;
        }
        if (Slot < DClass.HotbarSize) {
            Guard.setSelected(Slot);
            return;
        }
        NonNullList<ItemStack> Main = Guard.main();
        ItemStack Wanted = Main.get(Slot);
        Main.set(Slot, Main.get(Guard.selected()));
        Main.set(Guard.selected(), Wanted);
    }

    public static void equipDuty(DClass Guard, GuardRules Rules) {
        int Gun = gunSlot(Guard);
        int Melee = meleeSlot(Guard);
        if (Rules.HoldFire() && Melee >= 0) {
            equip(Guard, Melee);
        } else if (Gun >= 0) {
            equip(Guard, Gun);
        } else if (Melee >= 0) {
            equip(Guard, Melee);
        }
    }

    public static NpcAmmoSource ammo(DClass Guard) {
        return (AmmoItem, Wanted) -> {
            NonNullList<ItemStack> Main = Guard.main();
            int Taken = 0;
            for (int Slot = Main.size() - 1; Slot >= 0 && Taken < Wanted; Slot--) {
                ItemStack Stack = Main.get(Slot);
                if (!Stack.is(AmmoItem)) {
                    continue;
                }
                int Used = Math.min(Wanted - Taken, Stack.getCount());
                Stack.shrink(Used);
                Taken += Used;
            }
            return Taken;
        };
    }
}
