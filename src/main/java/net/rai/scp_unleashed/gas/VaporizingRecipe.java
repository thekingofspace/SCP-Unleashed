package net.rai.scp_unleashed.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class VaporizingRecipe implements Recipe<VaporizingRecipe.FluidInput> {
    public static final int DefaultTime = 20;
    public static final int MaxTime = 72000;
    public static final int DefaultPower = 40;
    public static final MapCodec<VaporizingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(VaporizingRecipe::getInput),
            GasStack.CODEC.fieldOf("result").forGetter(VaporizingRecipe::getResult),
            Codec.intRange(1, MaxTime).optionalFieldOf("time", DefaultTime).forGetter(VaporizingRecipe::getTime),
            Codec.intRange(1, 10000).optionalFieldOf("power", DefaultPower).forGetter(VaporizingRecipe::getPower)
    ).apply(Instance, VaporizingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaporizingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC, VaporizingRecipe::getInput,
            GasStack.STREAM_CODEC, VaporizingRecipe::getResult,
            ByteBufCodecs.VAR_INT, VaporizingRecipe::getTime,
            ByteBufCodecs.VAR_INT, VaporizingRecipe::getPower,
            VaporizingRecipe::new);

    private final SizedFluidIngredient Input;
    private final GasStack Result;
    private final int Time;
    private final int Power;

    public VaporizingRecipe(SizedFluidIngredient Input, GasStack Result, int Time, int Power) {
        this.Input = Input;
        this.Result = Result;
        this.Time = Time;
        this.Power = Power;
    }

    public SizedFluidIngredient getInput() {
        return Input;
    }

    public GasStack getResult() {
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
    public boolean matches(FluidInput Offered, Level World) {
        return accepts(Offered.Liquid()) && Offered.Liquid().getAmount() >= Input.amount();
    }

    @Override
    public ItemStack assemble(FluidInput Offered, HolderLookup.Provider Registries) {
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
        return new ItemStack(GasRegistry.VaporizerItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GasRegistry.VaporizingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GasRegistry.Vaporizing.get();
    }

    public record FluidInput(FluidStack Liquid) implements RecipeInput {
        @Override
        public ItemStack getItem(int Index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return Liquid.isEmpty();
        }
    }

    public static class Serializer implements RecipeSerializer<VaporizingRecipe> {
        @Override
        public MapCodec<VaporizingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VaporizingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
