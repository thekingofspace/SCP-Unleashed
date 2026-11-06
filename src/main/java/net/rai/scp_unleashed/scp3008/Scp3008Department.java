package net.rai.scp_unleashed.scp3008;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

public enum Scp3008Department implements StringRepresentable {
    Living,
    Dining,
    Kitchen,
    Bedroom,
    Bathroom,
    Office,
    Children,
    Lighting,
    Textiles,
    Outdoor,
    Warehouse,
    Electronics;

    private static final Scp3008Department[] Ordered = values();

    public final String Key;
    public final TagKey<Block> FurnitureTag;
    public final ResourceKey<LootTable> Loot;
    public final ResourceKey<Biome> BiomeKey;

    Scp3008Department() {
        Key = Scp3008Plan.Departments[ordinal()];
        FurnitureTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_furniture/" + Key));
        Loot = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp3008/" + Key));
        BiomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_" + Key));
    }

    public static Scp3008Department of(int Index) {
        return Ordered[Math.floorMod(Index, Ordered.length)];
    }

    @Override
    public String getSerializedName() {
        return Key;
    }
}
