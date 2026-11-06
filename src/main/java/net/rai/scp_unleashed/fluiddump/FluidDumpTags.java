package net.rai.scp_unleashed.fluiddump;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FluidDumpTags {
    public static final TagKey<Fluid> Pollutant = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pollutant"));

    private FluidDumpTags() {
    }
}
