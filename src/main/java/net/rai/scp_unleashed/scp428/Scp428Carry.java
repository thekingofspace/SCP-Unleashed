package net.rai.scp_unleashed.scp428;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.door.KeycardItem;
import net.rai.scp_unleashed.guns.AmmoItem;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.mtf.MtfOperative;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Scp428Carry {
    public static final float Scatter = 0.1f;

    private Scp428Carry() {
    }

    public record Haul(List<ItemStack> Stacks, boolean Owned, boolean FromPlayer) {
        public static Haul none(boolean FromPlayer) {
            return new Haul(List.of(), false, FromPlayer);
        }
    }

    public static boolean keepInventory(ServerLevel World) {
        return World.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    public static boolean dropAnomalies(ServerLevel World) {
        return AnomalyDrops.DropAnomalies != null && World.getGameRules().getBoolean(AnomalyDrops.DropAnomalies);
    }

    public static Haul capture(LivingEntity Victim) {
        if (!(Victim.level() instanceof ServerLevel World)) {
            return Haul.none(Victim instanceof Player);
        }
        return capture(Victim, keepInventory(World), dropAnomalies(World));
    }

    public static Haul capture(LivingEntity Victim, boolean KeepInventory, boolean DropAnomalies) {
        if (Victim instanceof Player User) {
            if (User.isSpectator()) {
                return Haul.none(true);
            }
            Inventory Pockets = User.getInventory();
            if (KeepInventory) {
                List<ItemStack> Seen = new ArrayList<>();
                for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
                    ItemStack Stack = Pockets.getItem(Slot);
                    if (useful(Stack)) {
                        Seen.add(Stack.copy());
                    }
                }
                return new Haul(Seen, false, true);
            }
            return new Haul(empty(Victim, Pockets, DropAnomalies), true, true);
        }
        if (Victim instanceof DClass Person) {
            return new Haul(empty(Victim, Person.pack(), DropAnomalies), true, false);
        }
        if (Victim instanceof MtfOperative Operative) {
            List<ItemStack> Seen = new ArrayList<>();
            for (EquipmentSlot Slot : EquipmentSlot.values()) {
                ItemStack Stack = Operative.getItemBySlot(Slot);
                if (useful(Stack)) {
                    Seen.add(Stack.copy());
                }
            }
            return new Haul(Seen, false, false);
        }
        return Haul.none(false);
    }

    private static List<ItemStack> empty(LivingEntity Victim, Container Pockets, boolean DropAnomalies) {
        List<ItemStack> Taken = new ArrayList<>();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.isEmpty() || EnchantmentHelper.has(Stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                continue;
            }
            Pockets.setItem(Slot, ItemStack.EMPTY);
            if (DropAnomalies && AnomalyDrops.isAnomalous(Stack)) {
                if (Victim instanceof Player User) {
                    User.drop(Stack, true, false);
                } else {
                    Victim.spawnAtLocation(Stack, 0.5f);
                }
                continue;
            }
            Taken.add(Stack);
        }
        Pockets.setChanged();
        return Taken;
    }

    public static boolean useful(ItemStack Stack) {
        return !Stack.isEmpty() && (Stack.getItem() instanceof KeycardItem || isRanged(Stack) || isAmmo(Stack));
    }

    public static boolean isRanged(ItemStack Stack) {
        return !Stack.isEmpty() && (Stack.getItem() instanceof ProjectileWeaponItem || NpcGunner.isGun(Stack));
    }

    public static boolean isAmmo(ItemStack Stack) {
        return !Stack.isEmpty() && (ProjectileWeaponItem.ARROW_OR_FIREWORK.test(Stack) || Stack.getItem() instanceof AmmoItem);
    }

    public static boolean dropsOnDeath(Scp428Record Entry, boolean KeepInventory) {
        if (!Entry.owned() || Entry.carried().isEmpty()) {
            return false;
        }
        return !Entry.fromPlayer() || !KeepInventory;
    }

    public static int spill(ServerLevel World, Vec3 At, List<ItemStack> Stacks) {
        int Count = 0;
        for (ItemStack Stack : Stacks) {
            if (Stack.isEmpty()) {
                continue;
            }
            ItemEntity Dropped = new ItemEntity(World, At.x, At.y + 0.5, At.z, Stack);
            Dropped.setDefaultPickUpDelay();
            Dropped.setDeltaMovement((World.random.nextFloat() - World.random.nextFloat()) * Scatter, World.random.nextFloat() * Scatter, (World.random.nextFloat() - World.random.nextFloat()) * Scatter);
            if (World.addFreshEntity(Dropped)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    @Nullable
    public static ItemStack weapon(Scp428Record Entry) {
        ItemStack Best = null;
        for (ItemStack Stack : Entry.carried()) {
            if (NpcGunner.isGun(Stack)) {
                return Stack;
            }
            if (Best == null && Stack.getItem() instanceof ProjectileWeaponItem) {
                Best = Stack;
            }
        }
        return Best;
    }

    public static int take(Scp428Record Entry, Predicate<ItemStack> Wanted, int Count) {
        int Taken = 0;
        for (ItemStack Stack : Entry.carried()) {
            if (Taken >= Count) {
                break;
            }
            if (!Stack.isEmpty() && Wanted.test(Stack)) {
                int Used = Math.min(Count - Taken, Stack.getCount());
                Stack.shrink(Used);
                Taken += Used;
            }
        }
        Entry.carried().removeIf(ItemStack::isEmpty);
        return Taken;
    }

    @Nullable
    public static ItemStack takeOne(Scp428Record Entry, Predicate<ItemStack> Wanted) {
        for (ItemStack Stack : Entry.carried()) {
            if (!Stack.isEmpty() && Wanted.test(Stack)) {
                ItemStack One = Stack.split(1);
                Entry.carried().removeIf(ItemStack::isEmpty);
                return One;
            }
        }
        return null;
    }
}
