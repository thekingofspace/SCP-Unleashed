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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.VaporizingRecipe;
import org.jetbrains.annotations.Nullable;

public class CombustionFuel implements Recipe<VaporizingRecipe.FluidInput> {
    public static final int DefaultBurnTime = 1000;
    public static final MapCodec<CombustionFuel> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("fuel").forGetter(CombustionFuel::getFuel),
            Codec.intRange(1, 100000).fieldOf("power").forGetter(CombustionFuel::getPower),
            Codec.intRange(20, 720000).optionalFieldOf("burn_time", DefaultBurnTime).forGetter(CombustionFuel::getBurnTime),
            GasStack.CODEC.fieldOf("exhaust").forGetter(CombustionFuel::getExhaust)
    ).apply(Instance, CombustionFuel::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombustionFuel> RecipeStreamCodec = StreamCodec.composite(
            FluidIngredient.STREAM_CODEC, CombustionFuel::getFuel,
            ByteBufCodecs.VAR_INT, CombustionFuel::getPower,
            ByteBufCodecs.VAR_INT, CombustionFuel::getBurnTime,
            GasStack.STREAM_CODEC, CombustionFuel::getExhaust,
            CombustionFuel::new);

    private final FluidIngredient Fuel;
    private final int Power;
    private final int BurnTime;
    private final GasStack Exhaust;

    public CombustionFuel(FluidIngredient Fuel, int Power, int BurnTime, GasStack Exhaust) {
        this.Fuel = Fuel;
        this.Power = Power;
        this.BurnTime = BurnTime;
        this.Exhaust = Exhaust;
    }

    @Nullable
    public static CombustionFuel find(@Nullable RecipeManager Recipes, FluidStack Stack) {
        if (Recipes == null || Stack.isEmpty()) {
            return null;
        }
        for (RecipeHolder<CombustionFuel> Holder : Recipes.getAllRecipesFor(OilRegistry.CombustionFuelType.get())) {
            if (Holder.value().accepts(Stack)) {
                return Holder.value();
            }
        }
        return null;
    }

    public FluidIngredient getFuel() {
        return Fuel;
    }

    public int getPower() {
        return Power;
    }

    public int getBurnTime() {
        return BurnTime;
    }

    public GasStack getExhaust() {
        return Exhaust;
    }

    public long energyPerBucket() {
        return (long) Power * BurnTime;
    }

    public long exhaustPerBucket() {
        return (long) Exhaust.getAmount() * BurnTime;
    }

    public int bucketSeconds() {
        return BurnTime / 20;
    }

    public boolean accepts(FluidStack Stack) {
        return !Stack.isEmpty() && Fuel.test(Stack);
    }

    @Override
    public boolean matches(VaporizingRecipe.FluidInput Offered, Level World) {
        return accepts(Offered.Liquid());
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
        return new ItemStack(OilRegistry.CombustionGeneratorItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OilRegistry.CombustionFuelSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return OilRegistry.CombustionFuelType.get();
    }

    public static class Serializer implements RecipeSerializer<CombustionFuel> {
        @Override
        public MapCodec<CombustionFuel> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CombustionFuel> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
