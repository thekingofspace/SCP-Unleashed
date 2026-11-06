package net.rai.scp_unleashed.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.rai.scp_unleashed.gas.VaporizingRecipe;

public class ChemicalProcessingRecipe implements Recipe<VaporizingRecipe.FluidInput> {
    public static final int DefaultTime = 40;
    public static final int DefaultPower = 64;
    public static final MapCodec<ChemicalProcessingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(ChemicalProcessingRecipe::getInput),
            FluidStack.CODEC.fieldOf("result").forGetter(ChemicalProcessingRecipe::getResult),
            Codec.intRange(1, 72000).optionalFieldOf("time", DefaultTime).forGetter(ChemicalProcessingRecipe::getTime),
            Codec.intRange(1, 10000).optionalFieldOf("power", DefaultPower).forGetter(ChemicalProcessingRecipe::getPower)
    ).apply(Instance, ChemicalProcessingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalProcessingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC, ChemicalProcessingRecipe::getInput,
            FluidStack.STREAM_CODEC, ChemicalProcessingRecipe::getResult,
            ByteBufCodecs.VAR_INT, ChemicalProcessingRecipe::getTime,
            ByteBufCodecs.VAR_INT, ChemicalProcessingRecipe::getPower,
            ChemicalProcessingRecipe::new);

    private final SizedFluidIngredient Input;
    private final FluidStack Result;
    private final int Time;
    private final int Power;

    public ChemicalProcessingRecipe(SizedFluidIngredient Input, FluidStack Result, int Time, int Power) {
        this.Input = Input;
        this.Result = Result;
        this.Time = Time;
        this.Power = Power;
    }

    public SizedFluidIngredient getInput() {
        return Input;
    }

    public FluidStack getResult() {
        return Result;
    }

    public int getTime() {
        return Time;
    }

    public int getPower() {
        return Power;
    }

    public boolean accepts(FluidStack Stack) {
        return !Stack.isEmpty() && Input.ingredient().test(Stack);
    }

    @Override
    public boolean matches(VaporizingRecipe.FluidInput Offered, Level World) {
        return accepts(Offered.Liquid()) && Offered.Liquid().getAmount() >= Input.amount();
    }

    @Override
    public ItemStack assemble(VaporizingRecipe.FluidInput Offered, HolderLookup.Provider Registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int Width, int Height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider Registries) {
        return ItemStack.EMPTY;
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
        return new ItemStack(OilRegistry.ChemicalProcessorItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OilRegistry.ChemicalProcessingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return OilRegistry.ChemicalProcessing.get();
    }

    public static class Serializer implements RecipeSerializer<ChemicalProcessingRecipe> {
        @Override
        public MapCodec<ChemicalProcessingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChemicalProcessingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
