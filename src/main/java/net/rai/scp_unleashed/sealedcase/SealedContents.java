package net.rai.scp_unleashed.sealedcase;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SealedContents {
    public static final Codec<SealedContents> ContentsCodec = ItemStack.CODEC.xmap(SealedContents::new, SealedContents::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, SealedContents> ContentsStreamCodec = ItemStack.STREAM_CODEC.map(SealedContents::new, SealedContents::stack);
    private final ItemStack Held;

    public SealedContents(ItemStack Stack) {
        this.Held = Stack.copy();
    }

    public ItemStack stack() {
        return Held.copy();
    }

    public Item item() {
        return Held.getItem();
    }

    public int count() {
        return Held.getCount();
    }

    @Override
    public boolean equals(Object Other) {
        return this == Other || Other instanceof SealedContents Found && ItemStack.matches(Held, Found.Held);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStack.hashItemAndComponents(Held) + Held.getCount();
    }

    @Override
    public String toString() {
        return "SealedContents[" + Held + "]";
    }
}
