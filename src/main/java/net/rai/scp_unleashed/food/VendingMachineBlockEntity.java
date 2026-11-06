package net.rai.scp_unleashed.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class VendingMachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MaxCredit = 64;
    public static final int TraySlots = 4;
    public static final int PushTicks = 18;
    public static final int FallTicks = 8;
    public static final int VendTicks = PushTicks + FallTicks;
    public static final int RestTicks = 6;
    public static final int NoticeTicks = 60;
    public static final int SyncTicks = 4;
    public static final int VendEvent = 1;
    public static final int CreditData = 0;
    public static final int VendingData = 1;
    public static final int NoticeData = 2;
    public static final int DataCount = 3;
    public static final int NoticeNone = 0;
    public static final int NoticeThanks = 1;
    public static final int NoticeTrayFull = 2;
    public static final int NoticeCreditFull = 3;
    public static final int NoticeChange = 4;
    private static final double PanelHeight = 1.3;
    private static final double TrayHeight = 0.2;

    private final ItemStackHandler Tray = new ItemStackHandler(TraySlots) {
        @Override
        protected void onContentsChanged(int Slot) {
            changed();
        }
    };
    private int Credit;
    private int Vending = -1;
    private int VendTimer;
    private int Notice = NoticeNone;
    private int NoticeTimer;
    private boolean NeedsSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private int ShownVending = -1;
    private long ShownStart;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case CreditData -> Credit;
                case VendingData -> Vending + 1;
                case NoticeData -> Notice;
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

    public VendingMachineBlockEntity(BlockPos Pos, BlockState State) {
        super(FoodRegistry.VendingMachineEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, VendingMachineBlockEntity Machine) {
        Machine.tickServer(World);
    }

    private void tickServer(Level World) {
        if (Vending >= 0) {
            VendTimer++;
            if (VendTimer == VendTicks) {
                dispense(World);
            }
            if (VendTimer >= VendTicks + RestTicks) {
                Vending = -1;
                VendTimer = 0;
                setChanged();
            }
        }
        if (NoticeTimer > 0 && --NoticeTimer == 0) {
            Notice = NoticeNone;
        }
        if (NeedsSync && World.getGameTime() - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = World.getGameTime();
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void changed() {
        setChanged();
        NeedsSync = true;
    }

    private void notice(int Code) {
        Notice = Code;
        NoticeTimer = Code == NoticeNone ? 0 : NoticeTicks;
    }

    public int deposit(int Count) {
        int Taken = Math.min(Count, MaxCredit - Credit);
        if (Taken > 0 && level != null) {
            Credit += Taken;
            changed();
            play(null, FoodSounds.Coin.get(), PanelHeight, 0.7f, 0.94f + level.getRandom().nextFloat() * 0.12f);
        }
        if (Credit >= MaxCredit && Taken < Count) {
            notice(NoticeCreditFull);
        }
        return Taken;
    }

    public void insertByHand(Player User, ItemStack Stack) {
        if (Credit >= MaxCredit) {
            notice(NoticeCreditFull);
            play(null, FoodSounds.Error.get(), PanelHeight, 0.6f, 1.0f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.vending_machine.full", MaxCredit), true);
            return;
        }
        deposit(1);
        if (!User.hasInfiniteMaterials()) {
            Stack.shrink(1);
        }
        User.displayClientMessage(Component.translatable("message.scp_unleashed.vending_machine.credit", Credit, MaxCredit), true);
    }

    public boolean buy(Player User, int Index) {
        Snack Product = Snack.byIndex(Index);
        if (Product == null || level == null) {
            return false;
        }
        if (Vending >= 0 || Credit < Product.getPrice()) {
            play(null, FoodSounds.Error.get(), PanelHeight, 0.6f, 1.0f);
            return false;
        }
        if (!ItemHandlerHelper.insertItemStacked(Tray, new ItemStack(FoodRegistry.snack(Product)), true).isEmpty()) {
            notice(NoticeTrayFull);
            play(null, FoodSounds.Error.get(), PanelHeight, 0.6f, 0.9f);
            return false;
        }
        Credit -= Product.getPrice();
        Vending = Index;
        VendTimer = 0;
        notice(NoticeNone);
        changed();
        play(User, FoodSounds.Beep.get(), PanelHeight, 0.5f, 1.0f);
        play(null, FoodSounds.Motor.get(), 1.0, 0.75f, 0.96f + level.getRandom().nextFloat() * 0.08f);
        level.blockEvent(worldPosition, getBlockState().getBlock(), VendEvent, Index);
        return true;
    }

    private void dispense(Level World) {
        Snack Product = Snack.byIndex(Vending);
        if (Product == null) {
            return;
        }
        ItemStack Left = ItemHandlerHelper.insertItemStacked(Tray, new ItemStack(FoodRegistry.snack(Product)), false);
        if (!Left.isEmpty()) {
            Vec3 Spot = frontSpot(TrayHeight + 0.1, 0.75);
            ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y, Spot.z, Left);
            Direction Front = getFront();
            Dropped.setDeltaMovement(Front.getStepX() * 0.12, 0.08, Front.getStepZ() * 0.12);
            World.addFreshEntity(Dropped);
        }
        play(null, FoodSounds.Clunk.get(), TrayHeight, 0.7f, 0.92f + World.getRandom().nextFloat() * 0.16f);
        notice(NoticeThanks);
    }

    public void returnCredit(Player User) {
        if (level == null) {
            return;
        }
        if (Credit <= 0) {
            play(null, FoodSounds.Error.get(), PanelHeight, 0.5f, 1.1f);
            return;
        }
        int Amount = Credit;
        Credit = 0;
        changed();
        while (Amount > 0) {
            int Count = Math.min(Amount, Items.IRON_NUGGET.getDefaultMaxStackSize());
            ItemHandlerHelper.giveItemToPlayer(User, new ItemStack(Items.IRON_NUGGET, Count));
            Amount -= Count;
        }
        play(null, FoodSounds.Change.get(), PanelHeight - 0.4, 0.8f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        notice(NoticeChange);
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < TraySlots; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Tray.getStackInSlot(Slot).copy());
            Tray.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        Snack InFlight = Vending >= 0 && VendTimer < VendTicks ? Snack.byIndex(Vending) : null;
        if (InFlight != null) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), new ItemStack(FoodRegistry.snack(InFlight)));
        }
        Vending = -1;
        int Amount = Credit;
        Credit = 0;
        while (Amount > 0) {
            int Count = Math.min(Amount, Items.IRON_NUGGET.getDefaultMaxStackSize());
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), new ItemStack(Items.IRON_NUGGET, Count));
            Amount -= Count;
        }
    }

    private void play(@Nullable Player Except, SoundEvent Sound, double Height, float Volume, float Pitch) {
        if (level == null) {
            return;
        }
        Vec3 Spot = frontSpot(Height, 0.5);
        level.playSound(Except, Spot.x, Spot.y, Spot.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    private Vec3 frontSpot(double Height, double Reach) {
        Direction Front = getFront();
        return Vec3.atBottomCenterOf(worldPosition).add(Front.getStepX() * Reach, Height, Front.getStepZ() * Reach);
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(VendingMachineBlock.Facing) ? State.getValue(VendingMachineBlock.Facing) : Direction.NORTH;
    }

    public int getCredit() {
        return Credit;
    }

    public int getVending() {
        return Vending;
    }

    public IItemHandler getTray() {
        return Tray;
    }

    public int getShownVending() {
        return ShownVending;
    }

    public float getShownAge(float PartialTick) {
        return level == null ? Float.MAX_VALUE : level.getGameTime() - ShownStart + PartialTick;
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == VendEvent) {
            if (level != null && level.isClientSide) {
                ShownVending = Param;
                ShownStart = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Credit", Credit);
        Tag.put("Tray", Tray.serializeNBT(Registries));
        if (Vending >= 0) {
            Tag.putInt("Vending", Vending);
            Tag.putInt("VendTimer", VendTimer);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Credit = Math.max(0, Math.min(MaxCredit, Tag.getInt("Credit")));
        if (Tag.contains("Tray")) {
            Tray.deserializeNBT(Registries, Tag.getCompound("Tray"));
        }
        if (Tag.contains("Vending")) {
            Vending = Tag.getInt("Vending");
            VendTimer = Tag.getInt("VendTimer");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Credit", Credit);
        Tag.put("Tray", Tray.serializeNBT(Registries));
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.vending_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new VendingMenu(ContainerId, PlayerInventory, this, Data);
    }
}
