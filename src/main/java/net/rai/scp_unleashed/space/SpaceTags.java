package net.rai.scp_unleashed.space;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SpaceTags {
    public static final TagKey<DimensionType> Airless = TagKey.create(Registries.DIMENSION_TYPE, own("airless"));
    public static final TagKey<EntityType<?>> PressurizedVehicles = TagKey.create(Registries.ENTITY_TYPE, own("pressurized_vehicles"));
    public static final TagKey<EntityType<?>> VacuumImmune = TagKey.create(Registries.ENTITY_TYPE, own("vacuum_immune"));
    public static final TagKey<Item> SpaceWeapons = TagKey.create(Registries.ITEM, own("space_weapons"));
    public static final TagKey<Item> SpaceSuit = TagKey.create(Registries.ITEM, own("space_suit"));
    public static final TagKey<Item> AluminumPlates = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/aluminum"));
    public static final TagKey<Block> NeedsAir = TagKey.create(Registries.BLOCK, own("needs_air"));

    private SpaceTags() {
    }

    private static ResourceLocation own(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }
}
