package net.rai.scp_unleashed.fission;

import com.mojang.serialization.Codec;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class IrradiatingRecipe implements Recipe<SingleRecipeInput> {
    public static final int DefaultTime = 600;
    public static final int MaxTime = 72000;
    public static final MapCodec<IrradiatingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(IrradiatingRecipe::getInput),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(IrradiatingRecipe::getResult),
            Codec.intRange(1, MaxTime).optionalFieldOf("time", DefaultTime).forGetter(IrradiatingRecipe::getTime)
    ).apply(Instance, IrradiatingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IrradiatingRecipe> RecipeStreamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, IrradiatingRecipe::getInput,
            ItemStack.STREAM_CODEC, IrradiatingRecipe::getResult,
            ByteBufCodecs.VAR_INT, IrradiatingRecipe::getTime,
            IrradiatingRecipe::new);

    private final Ingredient Input;
    private final ItemStack Result;
    private final int Time;

    public IrradiatingRecipe(Ingredient Input, ItemStack Result, int Time) {
        this.Input = Input;
        this.Result = Result;
        this.Time = Time;
    }

    public Ingredient getInput() {
        return Input;
    }

    public ItemStack getResult() {
        return Result;
    }

    public int getTime() {
        return Time;
    }

    public boolean accepts(ItemStack Stack) {
        return Input.test(Stack);
    }

    @Override
    public boolean matches(SingleRecipeInput Offered, Level World) {
        return Input.test(Offered.item());
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
    public boolean showNotification() {
        return false;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(FissionRegistry.ReactorItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FissionRegistry.IrradiatingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FissionRegistry.Irradiating.get();
    }

    public static class Serializer implements RecipeSerializer<IrradiatingRecipe> {
        @Override
        public MapCodec<IrradiatingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, IrradiatingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
