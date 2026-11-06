package net.rai.scp_unleashed.processing;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {
    public static final int WorkDraw = 20;
    public static final int SpeedFactor = 2;

    private final RecipeManager.CachedCheck<SingleRecipeInput, ElectricSmeltingRecipe> ElectricCheck = RecipeManager.createCheck(ProcessingRegistry.ElectricSmeltingType.get());
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> SmeltingCheck = RecipeManager.createCheck(RecipeType.SMELTING);
    private final Object2IntOpenHashMap<ResourceLocation> RecipesUsed = new Object2IntOpenHashMap<>();

    public ElectricFurnaceBlockEntity(BlockPos Pos, BlockState State) {
        super(ProcessingRegistry.ElectricFurnaceEntity.get(), Pos, State);
    }

    public static int machineTicks(AbstractCookingRecipe Recipe) {
        return Math.max(1, Recipe.getCookingTime() / SpeedFactor);
    }

    @Override
    public int getWorkDraw() {
        return WorkDraw;
    }

    @Nullable
    @Override
    protected MachineJob findJob(Level World, ItemStack Input) {
        SingleRecipeInput Offered = new SingleRecipeInput(Input);
        RecipeHolder<ElectricSmeltingRecipe> Electric = ElectricCheck.getRecipeFor(Offered, World).orElse(null);
        if (Electric != null) {
            ElectricSmeltingRecipe Recipe = Electric.value();
            ItemStack Result = Recipe.assemble(Offered, World.registryAccess());
            return Result.isEmpty() ? null : new MachineJob(Electric, Result, machineTicks(Recipe), Recipe.getCount(), ProcessingRecipes.drawFor(WorkDraw, Recipe.getPowerMultiplier()));
        }
        RecipeHolder<SmeltingRecipe> Smelting = SmeltingCheck.getRecipeFor(Offered, World).orElse(null);
        if (Smelting == null) {
            return null;
        }
        ItemStack Result = Smelting.value().assemble(Offered, World.registryAccess());
        return Result.isEmpty() ? null : new MachineJob(Smelting, Result, machineTicks(Smelting.value()), 1, WorkDraw);
    }

    @Override
    protected boolean accepts(Level World, ItemStack Stack) {
        return ProcessingRecipes.canSmelt(World, Stack);
    }

    @Override
    protected void onCompleted(ServerLevel World, BlockPos Pos, MachineJob Job) {
        RecipesUsed.addTo(Job.id(), 1);
    }

    @Override
    public void awardExperience(ServerPlayer User) {
        List<RecipeHolder<?>> Used = popExperience(User.serverLevel(), User.position());
        User.awardRecipes(Used);
        List<ItemStack> Contents = List.of(getStorage().getStackInSlot(InputSlot), getStorage().getStackInSlot(OutputSlot));
        for (RecipeHolder<?> Holder : Used) {
            User.triggerRecipeCrafted(Holder, Contents);
        }
        RecipesUsed.clear();
        setChanged();
    }

    @Override
    public void onBroken(ServerLevel World, BlockPos Pos) {
        popExperience(World, Vec3.atCenterOf(Pos));
        RecipesUsed.clear();
    }

    private List<RecipeHolder<?>> popExperience(ServerLevel World, Vec3 At) {
        List<RecipeHolder<?>> Used = new ArrayList<>();
        for (Object2IntMap.Entry<ResourceLocation> Entry : RecipesUsed.object2IntEntrySet()) {
            World.getRecipeManager().byKey(Entry.getKey()).ifPresent(Holder -> {
                Used.add(Holder);
                if (Holder.value() instanceof AbstractCookingRecipe Cooking) {
                    createExperience(World, At, Entry.getIntValue(), Cooking.getExperience());
                }
            });
        }
        return Used;
    }

    private static void createExperience(ServerLevel World, Vec3 At, int Count, float Experience) {
        int Whole = Mth.floor(Count * Experience);
        float Fraction = Mth.frac(Count * Experience);
        if (Fraction != 0.0f && World.getRandom().nextFloat() < Fraction) {
            Whole++;
        }
        ExperienceOrb.award(World, At, Whole);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        CompoundTag Used = new CompoundTag();
        RecipesUsed.forEach((Id, Count) -> Used.putInt(Id.toString(), Count));
        Tag.put("RecipesUsed", Used);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("RecipesUsed")) {
            RecipesUsed.clear();
            CompoundTag Used = Tag.getCompound("RecipesUsed");
            for (String Key : Used.getAllKeys()) {
                ResourceLocation Id = ResourceLocation.tryParse(Key);
                if (Id != null) {
                    RecipesUsed.put(Id, Used.getInt(Key));
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new ElectricFurnaceMenu(ContainerId, PlayerInventory, this, getData());
    }
}
