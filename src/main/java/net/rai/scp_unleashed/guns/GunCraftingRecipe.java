package net.rai.scp_unleashed.guns;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunCraftingRecipe implements Recipe<GunCraftingInput> {
    public static final int MaxIngredients = 6;
    public static final int MaxCount = 64;
    public static final Codec<SizedIngredient> InputCodec = Codec.withAlternative(SizedIngredient.FLAT_CODEC, SizedIngredient.NESTED_CODEC)
            .validate(GunCraftingRecipe::checkInput);
    public static final MapCodec<GunCraftingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            InputCodec.listOf(1, MaxIngredients).fieldOf("ingredients").forGetter(GunCraftingRecipe::getInputs),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(GunCraftingRecipe::getResult)
    ).apply(Instance, GunCraftingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunCraftingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MaxIngredients)), GunCraftingRecipe::getInputs,
            ItemStack.STREAM_CODEC, GunCraftingRecipe::getResult,
            GunCraftingRecipe::new);

    private final List<SizedIngredient> Inputs;
    private final ItemStack Result;

    public GunCraftingRecipe(List<SizedIngredient> Inputs, ItemStack Result) {
        this.Inputs = List.copyOf(Inputs);
        this.Result = Result;
    }

    private static DataResult<SizedIngredient> checkInput(SizedIngredient Input) {
        if (Input.count() > MaxCount) {
            return DataResult.error(() -> "Gun crafting ingredient " + Input.ingredient() + " asks for " + Input.count() + " items, at most " + MaxCount + " are allowed");
        }
        return DataResult.success(Input);
    }

    public List<SizedIngredient> getInputs() {
        return Inputs;
    }

    public ItemStack getResult() {
        return Result;
    }

    @Nullable
    public int[] plan(List<ItemStack> Items) {
        return GunCraftingPlan.solve(Inputs, Items);
    }

    @Override
    public boolean matches(GunCraftingInput Input, Level World) {
        return plan(Input.Items()) != null;
    }

    @Override
    public ItemStack assemble(GunCraftingInput Input, HolderLookup.Provider Registries) {
        return Result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int Width, int Height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider Registries) {
        return Result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> Ingredients = NonNullList.create();
        for (SizedIngredient Input : Inputs) {
            Ingredients.add(Input.ingredient());
        }
        return Ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(GunsRegistry.GunBenchItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GunsRegistry.GunCraftingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GunsRegistry.GunCraftingType.get();
    }

    public static class Serializer implements RecipeSerializer<GunCraftingRecipe> {
        @Override
        public MapCodec<GunCraftingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GunCraftingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
