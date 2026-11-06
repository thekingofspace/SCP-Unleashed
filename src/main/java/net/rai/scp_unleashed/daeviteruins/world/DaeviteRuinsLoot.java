package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.plan.RuinCanvas;

public final class DaeviteRuinsLoot {
    private DaeviteRuinsLoot() {
    }

    public static ResourceKey<LootTable> key(String Path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static int clamp(int Tier) {
        return Math.max(1, Math.min(5, Tier));
    }

    public static ResourceKey<LootTable> container(int Kind, int Tier) {
        return switch (Kind) {
            case RuinCanvas.LootVault -> key("chests/daevite_ruin_vault_" + Math.max(2, clamp(Tier)));
            case RuinCanvas.LootOffering -> key("chests/daevite_ruin_offering_" + clamp(Tier));
            default -> key("chests/daevite_ruin_cache_" + clamp(Tier));
        };
    }

    public static ResourceKey<LootTable> dig(int Tier) {
        return key("archaeology/daevite_ruin_" + clamp(Tier));
    }

    public static ResourceKey<LootTable> gear(int Tier, boolean Archer) {
        return key("equipment/daevite_ruin_" + (Archer ? "archer_" : "melee_") + Math.max(2, clamp(Tier)));
    }
}
