package net.rai.scp_unleashed.scp207;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record Scp207Rack(List<Boolean> Caps) {
    public static final int MaxBottles = 8;
    public static final Scp207Rack Empty = new Scp207Rack(List.of());
    public static final Codec<Scp207Rack> RackCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.listOf().optionalFieldOf("blue", List.of()).forGetter(Scp207Rack::Caps)
    ).apply(Instance, Scp207Rack::new));
    public static final StreamCodec<ByteBuf, Scp207Rack> RackStreamCodec = ByteBufCodecs.BOOL.apply(ByteBufCodecs.list(MaxBottles)).map(Scp207Rack::new, Scp207Rack::Caps);

    public Scp207Rack {
        Caps = List.copyOf(Caps.size() > MaxBottles ? Caps.subList(0, MaxBottles) : Caps);
    }

    public static Scp207Rack of(ItemStack Stack) {
        if (!Scp207BottleItem.is(Stack)) {
            return Empty;
        }
        Scp207Rack Held = Stack.get(Scp207Registry.RackData.get());
        if (Held != null && !Held.isEmpty()) {
            return Held;
        }
        return new Scp207Rack(List.of(Scp207BottleItem.isOne(Stack)));
    }

    public static boolean isRack(ItemStack Stack) {
        return Scp207BottleItem.is(Stack) && Stack.has(Scp207Registry.RackData.get());
    }

    public int size() {
        return Caps.size();
    }

    public boolean isEmpty() {
        return Caps.isEmpty();
    }

    public boolean isBlue(int Index) {
        return Index >= 0 && Index < Caps.size() && Caps.get(Index);
    }

    public int blueCount() {
        int Found = 0;
        for (Boolean Blue : Caps) {
            if (Blue) {
                Found++;
            }
        }
        return Found;
    }

    public Scp207Rack plus(boolean Blue) {
        List<Boolean> Grown = new ArrayList<>(Caps);
        Grown.add(Blue);
        return new Scp207Rack(Grown);
    }

    public Scp207Rack withoutLast() {
        return Caps.isEmpty() ? this : new Scp207Rack(Caps.subList(0, Caps.size() - 1));
    }

    public ItemStack stack() {
        if (Caps.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack Made = Scp207BottleItem.stack(Caps.get(0));
        if (Caps.size() > 1) {
            Made.set(Scp207Registry.RackData.get(), this);
        }
        return Made;
    }

    public List<ItemStack> bottles() {
        List<ItemStack> Made = new ArrayList<>(Caps.size());
        for (Boolean Blue : Caps) {
            Made.add(Scp207BottleItem.stack(Blue));
        }
        return Made;
    }
}
