package net.rai.scp_unleashed.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

public record PressingRecipe(Ingredient Input, int Count, ItemStack Result, int Time, float PowerMultiplier, PressDie Die) implements Recipe<SingleRecipeInput> {
    public static final int DefaultTime = 100;

    public boolean accepts(ItemStack Stack) {
        return Input.test(Stack);
    }

    public List<ItemStack> inputStacks() {
        return Arrays.stream(Input.getItems()).map(Stack -> Stack.copyWithCount(Count)).toList();
    }

    @Override
    public boolean matches(SingleRecipeInput Offered, Level World) {
        return Input.test(Offered.item()) && Offered.item().getCount() >= Count;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput Offered, HolderLookup.Provider Registries) {
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
        return NonNullList.of(Ingredient.EMPTY, Input);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ProcessingRegistry.ItemPressItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingRegistry.PressingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ProcessingRegistry.PressingType.get();
    }

    public static class Serializer implements RecipeSerializer<PressingRecipe> {
        public static final MapCodec<PressingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PressingRecipe::Input),
                Codec.intRange(1, ProcessingRecipes.MaxCount).optionalFieldOf("count", 1).forGetter(PressingRecipe::Count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PressingRecipe::Result),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", DefaultTime).forGetter(PressingRecipe::Time),
                ProcessingRecipes.PowerCodec.optionalFieldOf("power_multiplier", 1.0f).forGetter(PressingRecipe::PowerMultiplier),
                PressDie.DieCodec.optionalFieldOf("die", PressDie.Plate).forGetter(PressingRecipe::Die)
        ).apply(Instance, PressingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> RecipeStreamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, PressingRecipe::Input,
                ByteBufCodecs.VAR_INT, PressingRecipe::Count,
                ItemStack.STREAM_CODEC, PressingRecipe::Result,
                ByteBufCodecs.VAR_INT, PressingRecipe::Time,
                ByteBufCodecs.FLOAT, PressingRecipe::PowerMultiplier,
                PressDie.DieStreamCodec, PressingRecipe::Die,
                PressingRecipe::new);

        @Override
        public MapCodec<PressingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
