package net.rai.scp_unleashed.scp330;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Scp330Greed {
    public static final int Delay = 40;
    public static final int Interval = 20;
    public static final float BaseDamage = 0.25f;
    public static final float Step = 0.0125f;
    public static final int SpillDelay = 40;

    private Scp330Greed() {
    }

    public static boolean isGreedy(LivingEntity Target) {
        return Target.hasData(Scp330Registry.GreedState);
    }

    public static void afflict(LivingEntity Target) {
        if (!isGreedy(Target)) {
            Target.setData(Scp330Registry.GreedState, Scp330GreedState.Fresh);
        }
        showIcon(Target);
        emptyHands(Target);
        Scp330Marks.track(Target);
    }

    public static boolean cure(LivingEntity Target) {
        boolean Was = isGreedy(Target);
        if (Was) {
            Target.removeData(Scp330Registry.GreedState);
        }
        boolean Shown = Target.hasEffect(Scp330Registry.Greed) && Target.removeEffect(Scp330Registry.Greed);
        Scp330Marks.update(Target);
        return Was || Shown;
    }

    public static void tick(LivingEntity Target, ServerLevel World) {
        Scp330GreedState State = Target.getData(Scp330Registry.GreedState).next();
        Target.setData(Scp330Registry.GreedState, State);
        showIcon(Target);
        emptyHands(Target);
        if (Target.getAbsorptionAmount() > 0.0f) {
            Target.setAbsorptionAmount(0.0f);
        }
        int Since = State.Elapsed() - Delay;
        if (Since >= 0 && Since % Interval == 0 && !Target.isDeadOrDying()) {
            Target.hurt(source(Target), damage(Since / Interval));
        }
    }

    public static DamageSource source(LivingEntity Target) {
        return Target.damageSources().source(Scp330Registry.GreedDamage);
    }

    public static float damage(int Pulse) {
        return BaseDamage + Step * Pulse;
    }

    public static int pulsesToKill(float Health) {
        float Dealt = 0.0f;
        int Pulse = 0;
        while (Dealt < Health) {
            Dealt += damage(Pulse);
            Pulse++;
        }
        return Pulse;
    }

    public static int ticksToKill(float Health) {
        return Delay + (pulsesToKill(Health) - 1) * Interval;
    }

    private static void showIcon(LivingEntity Target) {
        if (!Target.hasEffect(Scp330Registry.Greed)) {
            Target.addEffect(new MobEffectInstance(Scp330Registry.Greed, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        }
    }

    public static void emptyHands(LivingEntity Target) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Held = Target.getItemInHand(Hand);
            if (Held.isEmpty()) {
                continue;
            }
            Target.setItemInHand(Hand, ItemStack.EMPTY);
            spill(Target, Held);
        }
    }

    private static void spill(LivingEntity Target, ItemStack Held) {
        ItemEntity Dropped = Target instanceof Player User ? User.drop(Held, false, true) : Target.spawnAtLocation(Held);
        if (Dropped != null) {
            Dropped.setPickUpDelay(SpillDelay);
        }
    }

    public static int pickUp(Player User, ItemEntity Found) {
        if (Found.hasPickUpDelay() || Found.getTarget() != null && !Found.getTarget().equals(User.getUUID())) {
            return 0;
        }
        ItemStack Stack = Found.getItem();
        Item Kind = Stack.getItem();
        int Before = Stack.getCount();
        Inventory Pack = User.getInventory();
        for (int Slot = 0; Slot < Pack.items.size() && !Stack.isEmpty(); Slot++) {
            ItemStack Here = Pack.items.get(Slot);
            if (Slot == Pack.selected || Here.isEmpty() || !ItemStack.isSameItemSameComponents(Here, Stack)) {
                continue;
            }
            int Move = Math.min(Stack.getCount(), Math.min(Here.getMaxStackSize(), Pack.getMaxStackSize(Here)) - Here.getCount());
            if (Move > 0) {
                Here.grow(Move);
                Stack.shrink(Move);
            }
        }
        for (int Slot = 0; Slot < Pack.items.size() && !Stack.isEmpty(); Slot++) {
            if (Slot == Pack.selected || !Pack.items.get(Slot).isEmpty()) {
                continue;
            }
            Pack.items.set(Slot, Stack.split(Math.min(Stack.getMaxStackSize(), Pack.getMaxStackSize(Stack))));
        }
        int Taken = Before - Stack.getCount();
        if (Taken <= 0) {
            return 0;
        }
        Pack.setChanged();
        User.take(Found, Taken);
        if (Stack.isEmpty()) {
            Found.discard();
        } else {
            Found.setItem(Stack.copy());
        }
        User.awardStat(Stats.ITEM_PICKED_UP.get(Kind), Taken);
        User.onItemPickup(Found);
        return Taken;
    }
}
