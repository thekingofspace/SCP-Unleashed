package net.rai.scp_unleashed.stranger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record StrangerGift(ResourceLocation ItemId, int Count, DataComponentPatch Components) {
    public static final Codec<StrangerGift> FullCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(StrangerGift::ItemId),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(StrangerGift::Count),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(StrangerGift::Components)
    ).apply(Instance, StrangerGift::new));
    public static final Codec<StrangerGift> GiftCodec = Codec.withAlternative(FullCodec, ResourceLocation.CODEC.xmap(Id -> new StrangerGift(Id, 1), StrangerGift::ItemId));

    public StrangerGift(ResourceLocation ItemId, int Count) {
        this(ItemId, Count, DataComponentPatch.EMPTY);
    }

    public ItemStack toStack() {
        Item Found = BuiltInRegistries.ITEM.get(ItemId);
        if (Found == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(Found), Math.max(1, Math.min(Count, Found.getDefaultMaxStackSize())), Components);
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("Item", ItemId.toString());
        Tag.putInt("Count", Count);
        if (!Components.isEmpty()) {
            DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, Components).result().ifPresent(Encoded -> Tag.put("Components", Encoded));
        }
        return Tag;
    }

    public static StrangerGift load(CompoundTag Tag) {
        ResourceLocation Id = ResourceLocation.tryParse(Tag.getString("Item"));
        DataComponentPatch Patch = Tag.contains("Components")
                ? DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, Tag.get("Components")).result().orElse(DataComponentPatch.EMPTY)
                : DataComponentPatch.EMPTY;
        return new StrangerGift(Id == null ? ResourceLocation.withDefaultNamespace("air") : Id, Math.max(1, Tag.getInt("Count")), Patch);
    }
}
