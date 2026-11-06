package net.rai.scp_unleashed.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record RaidLoot(List<ItemStack> Stacks, boolean Hand) {
    public static final RaidLoot None = new RaidLoot(List.of(), false);
    public static final Codec<RaidLoot> LootCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(RaidLoot::Stacks),
            Codec.BOOL.optionalFieldOf("hand", false).forGetter(RaidLoot::Hand)
    ).apply(Instance, RaidLoot::new));
    public static final Predicate<ItemStack> Wieldable = Stack -> Stack.getItem() instanceof TieredItem || Stack.getItem() instanceof TridentItem
            || Stack.getItem() instanceof MaceItem;

    public boolean empty() {
        return Stacks.isEmpty() && !Hand;
    }

    public static RaidLoot of(Mob Raider) {
        return Raider.hasData(RaidRegistry.Loot) ? Raider.getData(RaidRegistry.Loot) : None;
    }

    public static boolean carrying(Mob Raider) {
        return !of(Raider).empty();
    }

    public static void carry(Mob Raider, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        RaidLoot Held = of(Raider);
        if (!Held.Hand && Raider.getMainHandItem().isEmpty() && Wieldable.test(Stack)) {
            Raider.setItemSlot(EquipmentSlot.MAINHAND, Stack.copy());
            Raider.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            Raider.setData(RaidRegistry.Loot, new RaidLoot(Held.Stacks, true));
            return;
        }
        List<ItemStack> Next = new ArrayList<>(Held.Stacks);
        Next.add(Stack.copy());
        Raider.setData(RaidRegistry.Loot, new RaidLoot(List.copyOf(Next), Held.Hand));
    }

    public static List<ItemStack> takeAll(Mob Raider) {
        RaidLoot Held = of(Raider);
        List<ItemStack> Taken = new ArrayList<>();
        for (ItemStack Stack : Held.Stacks) {
            Taken.add(Stack.copy());
        }
        if (Held.Hand) {
            ItemStack Wielded = Raider.getMainHandItem();
            if (!Wielded.isEmpty()) {
                Taken.add(Wielded.copy());
            }
            Raider.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (Raider.hasData(RaidRegistry.Loot)) {
            Raider.removeData(RaidRegistry.Loot);
        }
        return Taken;
    }

    public static void drop(Mob Raider) {
        for (ItemStack Stack : takeAll(Raider)) {
            Raider.spawnAtLocation(Stack);
        }
    }
}
