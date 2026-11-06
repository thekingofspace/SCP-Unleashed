package net.rai.scp_unleashed.mining.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.archive.document.DocumentClass;

import java.util.Optional;

public record ShopListing(ResourceLocation Id, ShopKind Kind, long Price, Component Name, Component Description, ItemStack Icon, int Count,
                          Optional<DocumentClass> Classification, boolean Dossier) {
    public static final Codec<ShopListing> ListingCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ShopListing::Id),
            ShopKind.KindCodec.fieldOf("kind").forGetter(ShopListing::Kind),
            Codec.LONG.fieldOf("price").forGetter(ShopListing::Price),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(ShopListing::Name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(ShopListing::Description),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(ShopListing::Icon),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ShopListing::Count),
            DocumentClass.ClassCodec.optionalFieldOf("classification").forGetter(ShopListing::Classification),
            Codec.BOOL.optionalFieldOf("dossier", false).forGetter(ShopListing::Dossier)
    ).apply(Instance, ShopListing::new));
}
