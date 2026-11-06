package net.rai.scp_unleashed.scp140.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.Scp_unleashed;

public final class DaeviteTags {
    public static final TagKey<EntityType<?>> Daevites = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevites"));
    public static final TagKey<EntityType<?>> Allies = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_allies"));
    public static final TagKey<Item> Robes = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_robes"));
    public static final TagKey<Block> Decor = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_decor"));

    private DaeviteTags() {
    }
}
