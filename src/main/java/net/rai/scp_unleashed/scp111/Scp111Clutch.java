package net.rai.scp_unleashed.scp111;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Scp111Clutch(List<Entry> Eggs) {
    public static final int MaxEggs = 64;
    public static final int BoxPerVariant = 3;
    public static final Scp111Clutch Empty = new Scp111Clutch(List.of());
    public static final Codec<Scp111Clutch> ClutchCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Entry.EntryCodec.listOf().optionalFieldOf("eggs", List.of()).forGetter(Scp111Clutch::Eggs)
    ).apply(Instance, Scp111Clutch::new));
    public static final StreamCodec<ByteBuf, Scp111Clutch> ClutchStreamCodec = Entry.EntryStreamCodec.apply(ByteBufCodecs.list(MaxEggs))
            .map(Scp111Clutch::new, Scp111Clutch::Eggs);

    public Scp111Clutch {
        Eggs = List.copyOf(Eggs.size() > MaxEggs ? Eggs.subList(0, MaxEggs) : Eggs);
    }

    public static Scp111Clutch boxed() {
        List<Entry> Made = new ArrayList<>();
        for (Scp111Variant Variant : Scp111Variant.values()) {
            for (int Index = 0; Index < BoxPerVariant; Index++) {
                Made.add(new Entry(Variant, Scp111Egg.TotalTicks));
            }
        }
        return new Scp111Clutch(Made);
    }

    public static Scp111Clutch of(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return Empty;
        }
        Scp111Clutch Held = Stack.get(Scp111Registry.ClutchData.get());
        if (Held != null) {
            return Held;
        }
        if (Stack.is(Scp111Registry.BoxItem.get())) {
            return boxed();
        }
        if (Stack.is(Scp111Registry.EggItem.get())) {
            return new Scp111Clutch(Collections.nCopies(Stack.getCount(), Scp111Egg.of(Stack).entry()));
        }
        return Empty;
    }

    public int size() {
        return Eggs.size();
    }

    public boolean isEmpty() {
        return Eggs.isEmpty();
    }

    public int due() {
        int Count = 0;
        for (Entry Egg : Eggs) {
            if (Egg.isDue()) {
                Count++;
            }
        }
        return Count;
    }

    public Scp111Clutch aged(int Ticks) {
        if (Ticks <= 0) {
            return this;
        }
        List<Entry> Made = new ArrayList<>(Eggs.size());
        for (Entry Egg : Eggs) {
            Made.add(new Entry(Egg.Variant(), Math.max(0, Egg.Remaining() - Ticks)));
        }
        return new Scp111Clutch(Made);
    }

    public Scp111Clutch dueNow() {
        List<Entry> Made = new ArrayList<>(Eggs.size());
        for (Entry Egg : Eggs) {
            Made.add(new Entry(Egg.Variant(), 0));
        }
        return new Scp111Clutch(Made);
    }

    public List<Entry> dueEggs() {
        return Eggs.stream().filter(Entry::isDue).toList();
    }

    public Scp111Clutch waiting() {
        return new Scp111Clutch(Eggs.stream().filter(Egg -> !Egg.isDue()).toList());
    }

    public Scp111Clutch plus(Entry Added, int Count) {
        List<Entry> Made = new ArrayList<>(Eggs);
        for (int Index = 0; Index < Count; Index++) {
            Made.add(Added);
        }
        return new Scp111Clutch(Made);
    }

    public List<ItemStack> eggStacks() {
        Map<Scp111Egg, Integer> Counts = new LinkedHashMap<>();
        for (Entry Egg : Eggs) {
            Counts.merge(Egg.egg(), 1, Integer::sum);
        }
        List<ItemStack> Made = new ArrayList<>();
        for (Map.Entry<Scp111Egg, Integer> Group : Counts.entrySet()) {
            int Left = Group.getValue();
            while (Left > 0) {
                int Taken = Math.min(Left, Scp111EggItem.StackSize);
                Made.add(Group.getKey().stack(Taken));
                Left -= Taken;
            }
        }
        return Made;
    }

    public record Entry(Scp111Variant Variant, int Remaining) {
        public static final Codec<Entry> EntryCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Scp111Variant.VariantCodec.fieldOf("variant").forGetter(Entry::Variant),
                Codec.INT.fieldOf("remaining").forGetter(Entry::Remaining)
        ).apply(Instance, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> EntryStreamCodec = StreamCodec.composite(
                Scp111Variant.VariantStreamCodec, Entry::Variant,
                ByteBufCodecs.VAR_INT, Entry::Remaining,
                Entry::new);

        public boolean isDue() {
            return Remaining <= 0;
        }

        public Scp111Egg egg() {
            return Scp111Egg.fromRemaining(Variant, Remaining);
        }
    }
}
