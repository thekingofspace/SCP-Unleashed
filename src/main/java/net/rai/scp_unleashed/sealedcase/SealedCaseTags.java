package net.rai.scp_unleashed.sealedcase;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SealedCaseTags {
    public static final TagKey<Item> Hazardous = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sealed_case/hazardous"));

    private SealedCaseTags() {
    }
}
