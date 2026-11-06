package net.rai.scp_unleashed.dclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.labware.LabVials;

import java.util.List;

public class DClassBloodRecipe extends ShapelessRecipe {
    public static final MapCodec<DClassBloodRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(DClassBloodRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(DClassBloodRecipe::category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(DClassBloodRecipe::result),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(DClassBloodRecipe::ingredientList)
    ).apply(Instance, DClassBloodRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DClassBloodRecipe> RecipeStreamCodec = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DClassBloodRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, DClassBloodRecipe::category,
            ItemStack.STREAM_CODEC, DClassBloodRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), DClassBloodRecipe::ingredientList,
            DClassBloodRecipe::new);

    private final ItemStack Result;

    public DClassBloodRecipe(String Group, CraftingBookCategory Category, ItemStack Result, List<Ingredient> Ingredients) {
        super(Group, Category, Result, NonNullList.of(Ingredient.EMPTY, Ingredients.toArray(Ingredient[]::new)));
        this.Result = Result;
    }

    public ItemStack result() {
        return Result;
    }

    public List<Ingredient> ingredientList() {
        return getIngredients();
    }

    @Override
    public boolean matches(CraftingInput Input, Level World) {
        for (int Index = 0; Index < Input.size(); Index++) {
            ItemStack Stack = Input.getItem(Index);
            if (LabVials.isVial(Stack) && LabVials.hasSample(Stack)) {
                return false;
            }
        }
        return super.matches(Input, World);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DClassRegistry.BloodRecipeSerializer.get();
    }

    public static final class Serializer implements RecipeSerializer<DClassBloodRecipe> {
        @Override
        public MapCodec<DClassBloodRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DClassBloodRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
