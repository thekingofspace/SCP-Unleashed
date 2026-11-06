package net.rai.scp_unleashed.mining.shop;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

import java.util.List;
import java.util.Optional;

public record ShopEntry(ShopKind Kind, long Price, Optional<ItemStack> Goods, Optional<ResourceLocation> Creature, Optional<Component> Name,
                        Optional<Component> Description, Optional<ItemStack> Icon, Optional<ResourceLocation> Document, FragmentRarity Pod, int Order,
                        List<ItemStack> Extras) {
    public static final long MaxPrice = 1_000_000_000L;
    public static final Codec<ItemStack> StackCodec = Codec.either(ItemStack.SIMPLE_ITEM_CODEC, ItemStack.CODEC)
            .xmap(Either::unwrap, Stack -> Stack.getCount() == 1 && Stack.getComponentsPatch().isEmpty() ? Either.left(Stack) : Either.right(Stack));
    public static final Codec<ShopEntry> EntryCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ShopKind.KindCodec.fieldOf("kind").forGetter(ShopEntry::Kind),
            Codec.LONG.fieldOf("price").forGetter(ShopEntry::Price),
            StackCodec.optionalFieldOf("item").forGetter(ShopEntry::Goods),
            ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(ShopEntry::Creature),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(ShopEntry::Name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(ShopEntry::Description),
            StackCodec.optionalFieldOf("icon").forGetter(ShopEntry::Icon),
            ResourceLocation.CODEC.optionalFieldOf("document").forGetter(ShopEntry::Document),
            FragmentRarity.RarityCodec.optionalFieldOf("pod", FragmentRarity.Faint).forGetter(ShopEntry::Pod),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ShopEntry::Order),
            StackCodec.listOf().optionalFieldOf("extras", List.of()).forGetter(ShopEntry::Extras)
    ).apply(Instance, ShopEntry::new));
}
