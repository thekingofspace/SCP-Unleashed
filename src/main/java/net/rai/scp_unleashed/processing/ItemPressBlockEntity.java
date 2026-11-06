package net.rai.scp_unleashed.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ItemPressBlockEntity extends ProcessingMachineBlockEntity {
    public static final int WorkDraw = 30;
    public static final int SlamTicks = 2;
    public static final int HoldTicks = 4;
    public static final int ReturnTicks = 12;
    public static final float DropSpeed = 0.5f;
    public static final float RiseSpeed = 1.0f / ReturnTicks;
    public static final float Approach = 0.86f;
    public static final float Squeeze = 0.92f;
    public static final float ApproachStart = 0.1f;
    public static final float ApproachEnd = 0.85f;

    private PressDie Die = PressDie.Plate;
    private float Head;
    private float PreviousHead;
    private int StrokeAge = -1;
    private ItemStack Pressed = ItemStack.EMPTY;

    public ItemPressBlockEntity(BlockPos Pos, BlockState State) {
        super(ProcessingRegistry.ItemPressEntity.get(), Pos, State);
    }

    public static float descent(float Progress) {
        if (Progress < ApproachStart) {
            return 0.0f;
        }
        if (Progress < ApproachEnd) {
            float Along = (Progress - ApproachStart) / (ApproachEnd - ApproachStart);
            return Approach * Along * Along * (3.0f - 2.0f * Along);
        }
        return Approach + (Squeeze - Approach) * Math.min(1.0f, (Progress - ApproachEnd) / (1.0f - ApproachEnd));
    }

    @Override
    public int getWorkDraw() {
        return WorkDraw;
    }

    @Nullable
    @Override
    protected MachineJob findJob(Level World, ItemStack Input) {
        RecipeHolder<PressingRecipe> Holder = ProcessingRecipes.choosePressing(World, Input, Die);
        if (Holder == null) {
            return null;
        }
        PressingRecipe Recipe = Holder.value();
        ItemStack Result = Recipe.assemble(new SingleRecipeInput(Input), World.registryAccess());
        return Result.isEmpty() ? null : new MachineJob(Holder, Result, Recipe.Time(), Recipe.Count(), ProcessingRecipes.drawFor(WorkDraw, Recipe.PowerMultiplier()));
    }

    @Override
    protected boolean accepts(Level World, ItemStack Stack) {
        return ProcessingRecipes.canPress(World, Stack);
    }

    @Override
    protected void onCompleted(ServerLevel World, BlockPos Pos, MachineJob Job) {
        World.playSound(null, Pos, ProcessingRegistry.PressThunk.get(), SoundSource.BLOCKS, 0.9f, 0.94f + World.getRandom().nextFloat() * 0.12f);
    }

    public PressDie getDie() {
        return Die;
    }

    public void cycleDie() {
        Die = Die.next();
        setChanged();
        requestSync();
    }

    @Override
    protected int getMode() {
        return Die.ordinal();
    }

    @Override
    protected void onClientCompleted(ItemStack Result) {
        StrokeAge = 0;
        Pressed = Result;
    }

    @Override
    protected void tickAnimation() {
        PreviousHead = Head;
        float Target;
        if (StrokeAge >= 0) {
            StrokeAge++;
            Target = StrokeAge <= SlamTicks + HoldTicks ? 1.0f : 0.0f;
            if (StrokeAge > SlamTicks + HoldTicks + ReturnTicks) {
                StrokeAge = -1;
                Pressed = ItemStack.EMPTY;
            }
        } else {
            Target = isWorking() && !getShownWorkpiece().isEmpty() ? descent(getShownProgress(1.0f)) : 0.0f;
        }
        Head += Math.max(-RiseSpeed, Math.min(DropSpeed, Target - Head));
    }

    public float getHeadDrop(float PartialTick) {
        return PreviousHead + (Head - PreviousHead) * PartialTick;
    }

    public ItemStack getShownPiece() {
        if (StrokeAge >= SlamTicks && !Pressed.isEmpty()) {
            return Pressed;
        }
        return getShownWorkpiece();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putString("Die", Die.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Die")) {
            Die = PressDie.byName(Tag.getString("Die"));
        }
    }

    @Override
    protected void writeSync(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Die", Die.getSerializedName());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.item_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new ItemPressMenu(ContainerId, PlayerInventory, this, getData());
    }
}
