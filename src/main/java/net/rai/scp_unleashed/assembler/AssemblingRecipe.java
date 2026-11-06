package net.rai.scp_unleashed.assembler;

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

public class AssemblingRecipe implements Recipe<AssemblingInput> {
    public static final int MaxIngredients = 9;
    public static final int MaxCount = 64;
    public static final int DefaultTime = 200;
    public static final int MaxTime = 32000;
    public static final float DefaultPower = 1.0f;
    public static final float MinPower = 0.05f;
    public static final float MaxPower = 20.0f;
    public static final Codec<SizedIngredient> InputCodec = Codec.withAlternative(SizedIngredient.FLAT_CODEC, SizedIngredient.NESTED_CODEC)
            .validate(AssemblingRecipe::checkInput);
    public static final MapCodec<AssemblingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            InputCodec.listOf(1, MaxIngredients).fieldOf("ingredients").forGetter(AssemblingRecipe::getInputs),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblingRecipe::getResult),
            Codec.intRange(1, MaxTime).optionalFieldOf("time", DefaultTime).forGetter(AssemblingRecipe::getTime),
            Codec.floatRange(MinPower, MaxPower).optionalFieldOf("power", DefaultPower).forGetter(AssemblingRecipe::getPower)
    ).apply(Instance, AssemblingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> RecipeStreamCodec = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MaxIngredients)), AssemblingRecipe::getInputs,
            ItemStack.STREAM_CODEC, AssemblingRecipe::getResult,
            ByteBufCodecs.VAR_INT, AssemblingRecipe::getTime,
            ByteBufCodecs.FLOAT, AssemblingRecipe::getPower,
            AssemblingRecipe::new);

    private final List<SizedIngredient> Inputs;
    private final ItemStack Result;
    private final int Time;
    private final float Power;

    public AssemblingRecipe(List<SizedIngredient> Inputs, ItemStack Result, int Time, float Power) {
        this.Inputs = List.copyOf(Inputs);
        this.Result = Result;
        this.Time = Time;
        this.Power = Power;
    }

    private static DataResult<SizedIngredient> checkInput(SizedIngredient Input) {
        if (Input.count() > MaxCount) {
            return DataResult.error(() -> "Assembling ingredient " + Input.ingredient() + " asks for " + Input.count() + " items, the most one slot can hold is " + MaxCount);
        }
        return DataResult.success(Input);
    }

    public List<SizedIngredient> getInputs() {
        return Inputs;
    }

    public ItemStack getResult() {
        return Result;
    }

    public int getTime() {
        return Time;
    }

    public float getPower() {
        return Power;
    }

    public int getDraw() {
        return Math.max(1, Math.round(AdvancedAssemblerBlockEntity.WorkingDraw * Power));
    }

    public int getTotalCount() {
        int Total = 0;
        for (SizedIngredient Input : Inputs) {
            Total += Input.count();
        }
        return Total;
    }

    public boolean uses(ItemStack Stack) {
        for (SizedIngredient Input : Inputs) {
            if (Input.ingredient().test(Stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public int[] plan(List<ItemStack> Items) {
        return AssemblingPlan.solve(Inputs, Items);
    }

    @Override
    public boolean matches(AssemblingInput Input, Level World) {
        return plan(Input.Items()) != null;
    }

    @Override
    public ItemStack assemble(AssemblingInput Input, HolderLookup.Provider Registries) {
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
        return new ItemStack(AssemblerRegistry.AdvancedAssemblerItem.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AssemblerRegistry.AssemblingSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AssemblerRegistry.AssemblingType.get();
    }

    public static class Serializer implements RecipeSerializer<AssemblingRecipe> {
        @Override
        public MapCodec<AssemblingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
