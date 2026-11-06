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

public class OilRefiningRecipe implements Recipe<VaporizingRecipe.FluidInput> {
    public static final int DefaultTime = 120;
    public static final int DefaultPower = 64;
    public static final MapCodec<OilRefiningRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(OilRefiningRecipe::getInput),
            ItemStack.CODEC.fieldOf("result").forGetter(OilRefiningRecipe::getResult),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", FluidStack.EMPTY).forGetter(OilRefiningRecipe::getByproduct),
            Codec.intRange(1, 72000).optionalFieldOf("time", DefaultTime).forGetter(OilRefiningRecipe::getTime),
            Codec.intRange(1, 10000).optionalFieldOf("power", DefaultPower).forGetter(OilRefiningRecipe::getPower)
    ).apply(Instance, OilRefiningRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OilRefiningRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC, OilRefiningRecipe::getInput,
            ItemStack.STREAM_CODEC, OilRefiningRecipe::getResult,
            FluidStack.OPTIONAL_STREAM_CODEC, OilRefiningRecipe::getByproduct,
            ByteBufCodecs.VAR_INT, OilRefiningRecipe::getTime,
            ByteBufCodecs.VAR_INT, OilRefiningRecipe::getPower,
            OilRefiningRecipe::new);

    private final SizedFluidIngredient Input;
    private final ItemStack Result;
    private final FluidStack Byproduct;
    private final int Time;
    private final int Power;

    public OilRefiningRecipe(SizedFluidIngredient Input, ItemStack Result, FluidStack Byproduct, int Time, int Power) {
        this.Input = Input;
        this.Result = Result;
        this.Byproduct = Byproduct;
        this.Time = Time;
        this.Power = Power;
    }

    public SizedFluidIngredient getInput() {
        return Input;
    }

    public ItemStack getResult() {
        return Result;
    }

    public FluidStack getByproduct() {
        return Byproduct;
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
        return Result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int Width, int Height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider Registries) {
        return Result.copy();
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
        return new ItemStack(OilRegistry.OilRefineryItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OilRegistry.OilRefiningSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return OilRegistry.OilRefining.get();
    }

    public static class Serializer implements RecipeSerializer<OilRefiningRecipe> {
        @Override
        public MapCodec<OilRefiningRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OilRefiningRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
