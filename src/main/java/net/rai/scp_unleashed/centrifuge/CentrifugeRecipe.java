package net.rai.scp_unleashed.centrifuge;

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

public class CentrifugeRecipe implements Recipe<SingleRecipeInput> {
    public static final int MaxResults = 4;
    public static final int DefaultTime = 160;
    public static final int MaxTime = 72000;
    public static final int MaxInputCount = 64;
    public static final Codec<SizedIngredient> InputCodec = SizedIngredient.FLAT_CODEC.validate(Sized -> Sized.count() <= MaxInputCount
            ? DataResult.success(Sized) : DataResult.error(() -> "ingredient count " + Sized.count() + " is more than the " + MaxInputCount + " that fit in the input slot"));
    public static final Codec<List<CentrifugeOutput>> ResultsCodec = sized(CentrifugeOutput.OutputCodec.listOf());
    public static final MapCodec<CentrifugeRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            InputCodec.fieldOf("ingredient").forGetter(CentrifugeRecipe::getInput),
            ResultsCodec.fieldOf("results").forGetter(CentrifugeRecipe::getResults),
            ExtraCodecs.intRange(1, MaxTime).optionalFieldOf("time", DefaultTime).forGetter(CentrifugeRecipe::getTime)
    ).apply(Instance, CentrifugeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CentrifugeRecipe::getInput,
            CentrifugeOutput.OutputStreamCodec.apply(ByteBufCodecs.list(MaxResults)), CentrifugeRecipe::getResults,
            ByteBufCodecs.VAR_INT, CentrifugeRecipe::getTime,
            CentrifugeRecipe::new);

    private final SizedIngredient Feedstock;
    private final List<CentrifugeOutput> Results;
    private final int Time;

    public CentrifugeRecipe(SizedIngredient Feedstock, List<CentrifugeOutput> Results, int Time) {
        this.Feedstock = Feedstock;
        this.Results = List.copyOf(Results);
        this.Time = Time;
    }

    private static Codec<List<CentrifugeOutput>> sized(Codec<List<CentrifugeOutput>> Inner) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<CentrifugeOutput>, T>> decode(DynamicOps<T> Ops, T Value) {
                DataResult<Pair<List<CentrifugeOutput>, T>> Decoded = Inner.decode(Ops, Value);
                if (Decoded.isError()) {
                    return Decoded;
                }
                int Size = Decoded.result().map(Found -> Found.getFirst().size()).orElse(0);
                return Size == 0 || Size > MaxResults ? DataResult.error(() -> "results must hold 1 to " + MaxResults + " entries, got " + Size) : Decoded;
            }

            @Override
            public <T> DataResult<T> encode(List<CentrifugeOutput> Results, DynamicOps<T> Ops, T Prefix) {
                return Inner.encode(Results, Ops, Prefix);
            }
        };
    }

    public SizedIngredient getInput() {
        return Feedstock;
    }

    public List<CentrifugeOutput> getResults() {
        return Results;
    }

    public int getTime() {
        return Time;
    }

    public int getEnergy() {
        return Time * CentrifugeBlockEntity.WorkDraw;
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
        return new ItemStack(CentrifugeRegistry.CentrifugeItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CentrifugeRegistry.CentrifugingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CentrifugeRegistry.Centrifuging.get();
    }

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        @Override
        public MapCodec<CentrifugeRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
