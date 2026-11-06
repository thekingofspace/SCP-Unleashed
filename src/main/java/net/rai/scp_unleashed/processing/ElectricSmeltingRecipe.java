package net.rai.scp_unleashed.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class ElectricSmeltingRecipe extends AbstractCookingRecipe {
    public static final int DefaultCookingTime = 200;

    private final int Count;
    private final float PowerMultiplier;

    public ElectricSmeltingRecipe(String Group, CookingBookCategory Category, Ingredient Input, int Count, ItemStack Result, float Experience, int CookingTime, float PowerMultiplier) {
        super(ProcessingRegistry.ElectricSmeltingType.get(), Group, Category, Input, Result, Experience, CookingTime);
        this.Count = Count;
        this.PowerMultiplier = PowerMultiplier;
    }

    public Ingredient getInput() {
        return ingredient;
    }

    public int getCount() {
        return Count;
    }

    public ItemStack getResult() {
        return result;
    }

    public float getPowerMultiplier() {
        return PowerMultiplier;
    }

    public boolean accepts(ItemStack Stack) {
        return ingredient.test(Stack);
    }

    @Override
    public boolean matches(SingleRecipeInput Offered, Level World) {
        return ingredient.test(Offered.item()) && Offered.item().getCount() >= Count;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingRegistry.ElectricSmeltingSerializer.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ProcessingRegistry.ElectricFurnaceItem.get());
    }

    public static class Serializer implements RecipeSerializer<ElectricSmeltingRecipe> {
        public static final MapCodec<ElectricSmeltingRecipe> RecipeCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ElectricSmeltingRecipe::getGroup),
                CookingBookCategory.CODEC.optionalFieldOf("category", CookingBookCategory.MISC).forGetter(ElectricSmeltingRecipe::category),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ElectricSmeltingRecipe::getInput),
                Codec.intRange(1, ProcessingRecipes.MaxCount).optionalFieldOf("count", 1).forGetter(ElectricSmeltingRecipe::getCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ElectricSmeltingRecipe::getResult),
                Codec.floatRange(0.0f, ProcessingRecipes.MaxExperience).optionalFieldOf("experience", 0.0f).forGetter(ElectricSmeltingRecipe::getExperience),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("cookingtime", DefaultCookingTime).forGetter(ElectricSmeltingRecipe::getCookingTime),
                ProcessingRecipes.PowerCodec.optionalFieldOf("power_multiplier", 1.0f).forGetter(ElectricSmeltingRecipe::getPowerMultiplier)
        ).apply(Instance, ElectricSmeltingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ElectricSmeltingRecipe> RecipeStreamCodec = StreamCodec.of(Serializer::write, Serializer::read);

        private static ElectricSmeltingRecipe read(RegistryFriendlyByteBuf Buffer) {
            String Group = Buffer.readUtf();
            CookingBookCategory Category = Buffer.readEnum(CookingBookCategory.class);
            Ingredient Input = Ingredient.CONTENTS_STREAM_CODEC.decode(Buffer);
            int Count = Buffer.readVarInt();
            ItemStack Result = ItemStack.STREAM_CODEC.decode(Buffer);
            float Experience = Buffer.readFloat();
            int CookingTime = Buffer.readVarInt();
            float PowerMultiplier = Buffer.readFloat();
            return new ElectricSmeltingRecipe(Group, Category, Input, Count, Result, Experience, CookingTime, PowerMultiplier);
        }

        private static void write(RegistryFriendlyByteBuf Buffer, ElectricSmeltingRecipe Recipe) {
            Buffer.writeUtf(Recipe.getGroup());
            Buffer.writeEnum(Recipe.category());
            Ingredient.CONTENTS_STREAM_CODEC.encode(Buffer, Recipe.getInput());
            Buffer.writeVarInt(Recipe.getCount());
            ItemStack.STREAM_CODEC.encode(Buffer, Recipe.getResult());
            Buffer.writeFloat(Recipe.getExperience());
            Buffer.writeVarInt(Recipe.getCookingTime());
            Buffer.writeFloat(Recipe.getPowerMultiplier());
        }

        @Override
        public MapCodec<ElectricSmeltingRecipe> codec() {
            return RecipeCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ElectricSmeltingRecipe> streamCodec() {
            return RecipeStreamCodec;
        }
    }
}
