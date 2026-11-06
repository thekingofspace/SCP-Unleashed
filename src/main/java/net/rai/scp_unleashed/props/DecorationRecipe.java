package net.rai.scp_unleashed.props;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record DecorationRecipe(DecorationCategory Category, SizedIngredient Material, Optional<SizedIngredient> Extra, ItemStack Result) implements Recipe<DecorationInput> {
    public static final int MaxCount = 64;
    public static final Codec<SizedIngredient> InputCodec = Codec.withAlternative(SizedIngredient.FLAT_CODEC, SizedIngredient.NESTED_CODEC)
            .validate(DecorationRecipe::checkInput);
    public static final MapCodec<DecorationRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DecorationCategory.CategoryCodec.optionalFieldOf("category", DecorationCategory.Misc).forGetter(DecorationRecipe::Category),
            InputCodec.fieldOf("material").forGetter(DecorationRecipe::Material),
            InputCodec.optionalFieldOf("extra").forGetter(DecorationRecipe::Extra),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(DecorationRecipe::Result)
    ).apply(Instance, DecorationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DecorationRecipe> RecipeStreamCodec = StreamCodec.composite(
            DecorationCategory.CategoryStreamCodec, DecorationRecipe::Category,
            SizedIngredient.STREAM_CODEC, DecorationRecipe::Material,
            ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), DecorationRecipe::Extra,
            ItemStack.STREAM_CODEC, DecorationRecipe::Result,
            DecorationRecipe::new);
    private static final Comparator<RecipeHolder<DecorationRecipe>> Order = Comparator
            .comparingInt((RecipeHolder<DecorationRecipe> Holder) -> Holder.value().Category().ordinal())
            .thenComparingInt(Holder -> BuiltInRegistries.ITEM.getId(Holder.value().Result().getItem()))
            .thenComparing(Holder -> Holder.id().toString());

    private static DataResult<SizedIngredient> checkInput(SizedIngredient Input) {
        if (Input.count() > MaxCount) {
            return DataResult.error(() -> "Decoration ingredient " + Input.ingredient() + " asks for " + Input.count() + " items, at most " + MaxCount + " fit in a slot");
        }
        return DataResult.success(Input);
    }

    public static List<RecipeHolder<DecorationRecipe>> sorted(RecipeManager Recipes) {
        List<RecipeHolder<DecorationRecipe>> Found = new ArrayList<>(Recipes.getAllRecipesFor(PropsRegistry.DecorationType.get()));
        Found.sort(Order);
        return Found;
    }

    public static boolean usesMaterial(List<RecipeHolder<DecorationRecipe>> Recipes, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<DecorationRecipe> Holder : Recipes) {
            if (Holder.value().Material().ingredient().test(Stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean usesExtra(List<RecipeHolder<DecorationRecipe>> Recipes, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<DecorationRecipe> Holder : Recipes) {
            Optional<SizedIngredient> Needed = Holder.value().Extra();
            if (Needed.isPresent() && Needed.get().ingredient().test(Stack)) {
                return true;
            }
        }
        return false;
    }

    private static int shortOf(SizedIngredient Needed, ItemStack Stack) {
        if (!Needed.ingredient().test(Stack)) {
            return Needed.count();
        }
        return Math.max(0, Needed.count() - Stack.getCount());
    }

    public int materialShort(ItemStack Stack) {
        return shortOf(Material, Stack);
    }

    public int extraShort(ItemStack Stack) {
        return Extra.map(Needed -> shortOf(Needed, Stack)).orElse(0);
    }

    public boolean affordable(ItemStack MaterialStack, ItemStack ExtraStack) {
        return materialShort(MaterialStack) == 0 && extraShort(ExtraStack) == 0;
    }

    @Override
    public boolean matches(DecorationInput Offered, Level World) {
        return affordable(Offered.Material(), Offered.Extra());
    }

    @Override
    public ItemStack assemble(DecorationInput Offered, HolderLookup.Provider Registries) {
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
        Ingredients.add(Material.ingredient());
        Extra.ifPresent(Needed -> Ingredients.add(Needed.ingredient()));
        return Ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(PropsRegistry.DecorationWorkbench.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PropsRegistry.DecorationSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PropsRegistry.DecorationType.get();
    }

    public static class Serializer implements RecipeSerializer<DecorationRecipe> {
        @Override
        public MapCodec<DecorationRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DecorationRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
