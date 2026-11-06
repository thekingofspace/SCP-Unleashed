package net.rai.scp_unleashed.refinery;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
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
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public class PurifyingRecipe implements Recipe<SingleRecipeInput> {
    public static final int MaxResults = 6;
    public static final int DefaultTime = 160;
    public static final int MaxTime = 72000;
    public static final int MaxInputCount = 64;
    public static final int DefaultWater = 250;
    public static final int DefaultWaste = 250;
    public static final Codec<SizedIngredient> InputCodec = SizedIngredient.FLAT_CODEC.validate(Sized -> Sized.count() <= MaxInputCount
            ? DataResult.success(Sized) : DataResult.error(() -> "ingredient count " + Sized.count() + " is more than the " + MaxInputCount + " that fit in the input slot"));
    public static final Codec<List<PurifierOutput>> ResultsCodec = sized(PurifierOutput.OutputCodec.listOf());
    public static final Codec<Integer> FluidCodec = ExtraCodecs.intRange(0, OrePurifierBlockEntity.TankCapacity);
    public static final MapCodec<PurifyingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            InputCodec.fieldOf("ingredient").forGetter(PurifyingRecipe::getInput),
            ResultsCodec.fieldOf("results").forGetter(PurifyingRecipe::getResults),
            ExtraCodecs.intRange(1, MaxTime).optionalFieldOf("time", DefaultTime).forGetter(PurifyingRecipe::getTime),
            FluidCodec.optionalFieldOf("water", DefaultWater).forGetter(PurifyingRecipe::getWater),
            FluidCodec.optionalFieldOf("waste", DefaultWaste).forGetter(PurifyingRecipe::getWaste)
    ).apply(Instance, PurifyingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PurifyingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, PurifyingRecipe::getInput,
            PurifierOutput.OutputStreamCodec.apply(ByteBufCodecs.list(MaxResults)), PurifyingRecipe::getResults,
            ByteBufCodecs.VAR_INT, PurifyingRecipe::getTime,
            ByteBufCodecs.VAR_INT, PurifyingRecipe::getWater,
            ByteBufCodecs.VAR_INT, PurifyingRecipe::getWaste,
            PurifyingRecipe::new);

    private final SizedIngredient Feedstock;
    private final List<PurifierOutput> Results;
    private final int Time;
    private final int Water;
    private final int Waste;

    public PurifyingRecipe(SizedIngredient Feedstock, List<PurifierOutput> Results, int Time, int Water, int Waste) {
        this.Feedstock = Feedstock;
        this.Results = List.copyOf(Results);
        this.Time = Time;
        this.Water = Water;
        this.Waste = Waste;
    }

    private static Codec<List<PurifierOutput>> sized(Codec<List<PurifierOutput>> Inner) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<PurifierOutput>, T>> decode(DynamicOps<T> Ops, T Value) {
                DataResult<Pair<List<PurifierOutput>, T>> Decoded = Inner.decode(Ops, Value);
                if (Decoded.isError()) {
                    return Decoded;
                }
                int Size = Decoded.result().map(Found -> Found.getFirst().size()).orElse(0);
                return Size == 0 || Size > MaxResults ? DataResult.error(() -> "results must hold 1 to " + MaxResults + " entries, got " + Size) : Decoded;
            }

            @Override
            public <T> DataResult<T> encode(List<PurifierOutput> Results, DynamicOps<T> Ops, T Prefix) {
                return Inner.encode(Results, Ops, Prefix);
            }
        };
    }

    public SizedIngredient getInput() {
        return Feedstock;
    }

    public List<PurifierOutput> getResults() {
        return Results;
    }

    public int getTime() {
        return Time;
    }

    public int getWater() {
        return Water;
    }

    public int getWaste() {
        return Waste;
    }

    public int getEnergy() {
        return Time * OrePurifierBlockEntity.WorkDraw;
    }

    public boolean accepts(ItemStack Stack) {
        return Feedstock.ingredient().test(Stack);
    }

    @Override
    public boolean matches(SingleRecipeInput Content, Level World) {
        return Feedstock.test(Content.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput Content, HolderLookup.Provider Registries) {
        return getResultItem(Registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int Width, int Height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider Registries) {
        return Results.getFirst().Stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Feedstock.ingredient());
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
        return new ItemStack(RefineryRegistry.OrePurifierItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RefineryRegistry.PurifyingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RefineryRegistry.Purifying.get();
    }

    public static class Serializer implements RecipeSerializer<PurifyingRecipe> {
        @Override
        public MapCodec<PurifyingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PurifyingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
