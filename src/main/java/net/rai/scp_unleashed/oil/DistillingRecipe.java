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

public class DistillingRecipe implements Recipe<VaporizingRecipe.FluidInput> {
    public static final int DefaultTime = 25;
    public static final int DefaultPower = 96;
    public static final MapCodec<DistillingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(DistillingRecipe::getInput),
            FluidStack.CODEC.fieldOf("top").forGetter(DistillingRecipe::getTop),
            FluidStack.CODEC.fieldOf("bottom").forGetter(DistillingRecipe::getBottom),
            Codec.intRange(1, 72000).optionalFieldOf("time", DefaultTime).forGetter(DistillingRecipe::getTime),
            Codec.intRange(1, 10000).optionalFieldOf("power", DefaultPower).forGetter(DistillingRecipe::getPower)
    ).apply(Instance, DistillingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC, DistillingRecipe::getInput,
            FluidStack.STREAM_CODEC, DistillingRecipe::getTop,
            FluidStack.STREAM_CODEC, DistillingRecipe::getBottom,
            ByteBufCodecs.VAR_INT, DistillingRecipe::getTime,
            ByteBufCodecs.VAR_INT, DistillingRecipe::getPower,
            DistillingRecipe::new);

    private final SizedFluidIngredient Input;
    private final FluidStack Top;
    private final FluidStack Bottom;
    private final int Time;
    private final int Power;

    public DistillingRecipe(SizedFluidIngredient Input, FluidStack Top, FluidStack Bottom, int Time, int Power) {
        this.Input = Input;
        this.Top = Top;
        this.Bottom = Bottom;
        this.Time = Time;
        this.Power = Power;
    }

    public SizedFluidIngredient getInput() {
        return Input;
    }

    public FluidStack getTop() {
        return Top;
    }

    public FluidStack getBottom() {
        return Bottom;
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
        return new ItemStack(OilRegistry.DistillationTowerItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OilRegistry.DistillingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return OilRegistry.Distilling.get();
    }

    public static class Serializer implements RecipeSerializer<DistillingRecipe> {
        @Override
        public MapCodec<DistillingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
