package net.rai.scp_unleashed.stasis;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;

public class StasisUpgradeRecipe extends ShapelessRecipe {
    private final FragmentRarity Tier;

    public StasisUpgradeRecipe(FragmentRarity Tier) {
        super("stasis_pod_upgrade", CraftingBookCategory.EQUIPMENT, new ItemStack(StasisRegistry.podFor(Tier)),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(StasisRegistry.podFor(FragmentRarity.byTier(Tier.getTier() - 1))),
                        Ingredient.of(ExtractionRegistry.fragmentFor(Tier))));
        this.Tier = Tier;
    }

    public FragmentRarity getTier() {
        return Tier;
    }

    @Override
    public boolean matches(CraftingInput Input, Level World) {
        if (!super.matches(Input, World)) {
            return false;
        }
        for (ItemStack Offered : Input.items()) {
            if (StasisPodItem.isOccupied(Offered)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StasisRegistry.UpgradeSerializer.get();
    }

    public static class Serializer implements RecipeSerializer<StasisUpgradeRecipe> {
        private static final MapCodec<StasisUpgradeRecipe> RecipeCodec = RecordCodecBuilder.<StasisUpgradeRecipe>mapCodec(Fields -> Fields.group(
                FragmentRarity.RarityCodec.fieldOf("tier").forGetter(StasisUpgradeRecipe::getTier)
        ).apply(Fields, StasisUpgradeRecipe::new)).validate(Made -> Made.getTier().getTier() > 1
                ? DataResult.success(Made) : DataResult.error(() -> "A stasis pod upgrade needs a tier above faint"));
        private static final StreamCodec<RegistryFriendlyByteBuf, StasisUpgradeRecipe> RecipeStreamCodec = ByteBufCodecs.VAR_INT
                .<StasisUpgradeRecipe>map(Rank -> new StasisUpgradeRecipe(FragmentRarity.byTier(Rank)), Made -> Made.getTier().getTier()).cast();

        @Override
        public MapCodec<StasisUpgradeRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StasisUpgradeRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
