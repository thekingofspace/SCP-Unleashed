package net.rai.scp_unleashed.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class VaporizerBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int TankCapacity = 4000;
    public static final int GasCapacity = 8000;
    public static final int ActiveHold = 10;
    public static final int ProgressSteps = 1000;
    public static final int DataInput = 0;
    public static final int DataGas = 1;
    public static final int DataReceived = 2;
    public static final int DataDemand = 3;
    public static final int DataStatus = 4;
    public static final int DataProgress = 5;
    public static final int DataFluidKind = 6;
    public static final int DataGasKind = 7;
    public static final int DataCount = 8;

    private static Consumer<VaporizerBlockEntity> ClientEffects = Machine -> {
    };

    private final FluidTank Input = new FluidTank(TankCapacity, this::canVaporize) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            RecipeDirty = true;
        }
    };
    private final GasTank Output = new GasTank(GasCapacity, Kind -> true, this::setChanged);

    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<VaporizingRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean RecipeDirty = true;
    private int Received;
    private int LastReceived;
    private int Demand;
    private float Progress;
    private VaporizerStatus Status = VaporizerStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataInput -> Input.getFluidAmount();
                case DataGas -> Output.getAmount();
                case DataReceived -> LastReceived;
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
                case DataProgress -> getProgressShown();
                case DataFluidKind -> Input.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(Input.getFluid().getFluid());
                case DataGasKind -> Output.isEmpty() ? -1 : GasRegistry.Gases.getId(Output.getGas().getGas());
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public VaporizerBlockEntity(BlockPos Pos, BlockState State) {
        super(GasRegistry.VaporizerEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<VaporizerBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public FluidTank getInputTank() {
        return Input;
    }

    public GasTank getGasTank() {
        return Output;
    }

    public VaporizerStatus getStatus() {
        return Status;
    }

    public int getDemand() {
        return Demand;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    @Nullable
    public VaporizingRecipe getCurrentRecipe() {
        return Current == null ? null : Current.value();
    }

    public int getProgressShown() {
        VaporizingRecipe Recipe = getCurrentRecipe();
        return Recipe == null ? 0 : Mth.clamp(Math.round(Progress * ProgressSteps / Recipe.getTime()), 0, ProgressSteps);
    }

    private boolean canVaporize(FluidStack Stack) {
        if (level == null || Stack.isEmpty()) {
            return false;
        }
        return findRecipe(level.getRecipeManager(), Stack) != null;
    }

    @Nullable
    public static RecipeHolder<VaporizingRecipe> findRecipe(RecipeManager Recipes, FluidStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        List<RecipeHolder<VaporizingRecipe>> All = Recipes.getAllRecipesFor(GasRegistry.Vaporizing.get());
        for (RecipeHolder<VaporizingRecipe> Holder : All) {
            if (Holder.value().accepts(Stack)) {
                return Holder;
            }
        }
        return null;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, VaporizerBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof VaporizerBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, VaporizerBlockEntity Machine) {
        ClientEffects.accept(Machine);
    }

    private void refreshRecipe(ServerLevel World) {
        RecipeManager Recipes = World.getRecipeManager();
        if (Recipes != KnownRecipes) {
            KnownRecipes = Recipes;
            Current = null;
            RecipeDirty = true;
        }
        if (!RecipeDirty) {
            return;
        }
        RecipeDirty = false;
        if (Current != null && Current.value().accepts(Input.getFluid())) {
            return;
        }
        RecipeHolder<VaporizingRecipe> Found = findRecipe(Recipes, Input.getFluid());
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0.0f;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > 0;
        refreshRecipe(World);
        VaporizingRecipe Recipe = getCurrentRecipe();
        boolean Ready = false;
        if (Input.isEmpty() || Recipe == null) {
            Status = VaporizerStatus.Idle;
            Progress = 0.0f;
        } else if (Input.getFluidAmount() < Recipe.getInput().amount()) {
            Status = VaporizerStatus.Short;
        } else if (Output.fill(Recipe.getResult(), true) < Recipe.getResult().getAmount()) {
            Status = VaporizerStatus.Blocked;
        } else {
            Ready = true;
            int Draw = Recipe.getPower();
            int Used = WasDemanding ? Math.min(Delivered, Draw) : 0;
            Status = Used >= Draw ? VaporizerStatus.Working : Used > 0 ? VaporizerStatus.LowPower : VaporizerStatus.Unpowered;
            if (Used > 0) {
                Progress += (float) Used / Draw;
                LastWorked = Now;
                setChanged();
                if (Progress >= Recipe.getTime()) {
                    Progress -= Recipe.getTime();
                    Input.drain(Recipe.getInput().amount(), IFluidHandler.FluidAction.EXECUTE);
                    Output.fill(Recipe.getResult(), false);
                }
            }
        }
        Demand = Ready && Recipe != null ? Recipe.getPower() : 0;
        boolean Active = Now - LastWorked <= ActiveHold;
        if (State.getValue(VaporizerBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(VaporizerBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Input", Input.writeToNBT(Registries, new CompoundTag()));
        Output.save(Tag, "Gas");
        Tag.putFloat("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            return;
        }
        if (Tag.contains("Input")) {
            Input.readFromNBT(Registries, Tag.getCompound("Input"));
        }
        Output.load(Tag, "Gas");
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        CurrentId = Tag.contains("Recipe") ? ResourceLocation.tryParse(Tag.getString("Recipe")) : null;
        Current = null;
        KnownRecipes = null;
        RecipeDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.liquid_vaporizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new VaporizerMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
