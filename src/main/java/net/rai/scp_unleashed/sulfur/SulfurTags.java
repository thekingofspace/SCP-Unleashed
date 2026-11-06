package net.rai.scp_unleashed.sulfur;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public final class SulfurTags {
    public static final TagKey<Fluid> Acid = TagKey.create(Registries.FLUID, SulfurRegistry.location(SulfurRegistry.AcidId));
    public static final TagKey<Item> AcidProof = TagKey.create(Registries.ITEM, SulfurRegistry.location("acid_proof"));

    private SulfurTags() {
    }
}
