package net.rai.scp_unleashed.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PointMinerBlockEntity extends BlockEntity implements WireDevice {
    public static final int ScanTicks = 20;
    public static final int StatusHold = 10;
    public static final int OrphanTicks = 40;

    private final FluidTank Water = new FluidTank(MinerRules.TankCapacity, PointMinerBlockEntity::isCoolant) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final MinerRules.Coolant Supply = new MinerRules.Coolant() {
        @Override
        public int available() {
            return Water.getFluidAmount();
        }

        @Override
        public int drain(int Amount) {
            return Water.drain(Amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    };
    private final MinerRules.Engine Core = new MinerRules.Engine();
    private List<BlockPos> Members = List.of();
    private long NextScan;
    private long Received;
    private long LastDriven = Long.MIN_VALUE / 2;
    private int StackSize = 1;
    private int StackIndex = 1;
    private boolean Linked;
    @Nullable
    private MinerStatus Pending;
    private long PendingSince;

    public PointMinerBlockEntity(BlockPos Pos, BlockState State) {
        super(MiningRegistry.PointMinerEntity.get(), Pos, State);
    }

    public static boolean isCoolant(FluidStack Stack) {
        return Stack.is(FluidTags.WATER);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PointMinerBlockEntity Miner) {
        if (World instanceof ServerLevel Server) {
            Miner.tickServer(Server);
        }
    }

    public boolean isHead() {
        return level == null || !(level.getBlockState(worldPosition.above()).getBlock() instanceof PointMinerBlock);
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (!isHead()) {
            Received = 0L;
            Members = List.of();
            if (Now - LastDriven > OrphanTicks) {
                work(World, 0.0, Now, 1, 1);
            }
            return;
        }
        if (Now >= NextScan || Members.isEmpty()) {
            Members = scan(World);
            NextScan = Now + ScanTicks;
        }
        double Share = MinerRules.share(Received, MinerRules.demand(Members.size()));
        Received = 0L;
        int Count = Members.size();
        for (int Index = 0; Index < Count; Index++) {
            BlockPos Member = Members.get(Index);
            if (World.getBlockEntity(Member) instanceof PointMinerBlockEntity Miner && !Miner.isRemoved()) {
                Miner.work(World, Share, Now, Index + 1, Count);
            }
        }
        if (MinerRules.isSecond(Now, worldPosition.asLong())) {
            flush(World);
        }
    }

    private List<BlockPos> scan(ServerLevel World) {
        List<BlockPos> Found = new ArrayList<>();
        BlockPos.MutableBlockPos Cursor = worldPosition.mutable();
        while (Cursor.getY() >= World.getMinBuildHeight() && World.isLoaded(Cursor) && World.getBlockState(Cursor).getBlock() instanceof PointMinerBlock) {
            Found.add(Cursor.immutable());
            Cursor.move(Direction.DOWN);
        }
        return List.copyOf(Found);
    }

    private void work(ServerLevel World, double Share, long Now, int Index, int Count) {
        LastDriven = Now;
        StackIndex = Index;
        StackSize = Count;
        int Before = Core.getBuffer();
        boolean WasHot = Core.isOverheated();
        Core.step(Share, MinerRules.isSecond(Now, worldPosition.asLong()), Supply);
        if (Core.getBuffer() != Before || Core.isOverheated() != WasHot) {
            setChanged();
        }
        showStatus(World, MinerStatus.of(Core), Now);
    }

    private void showStatus(ServerLevel World, MinerStatus Wanted, long Now) {
        BlockState State = getBlockState();
        if (!State.hasProperty(PointMinerBlock.Status) || State.getValue(PointMinerBlock.Status) == Wanted) {
            Pending = null;
            return;
        }
        if (Pending != Wanted) {
            Pending = Wanted;
            PendingSince = Now;
            return;
        }
        if (Now - PendingSince >= StatusHold) {
            Pending = null;
            World.setBlock(worldPosition, State.setValue(PointMinerBlock.Status, Wanted), Block.UPDATE_CLIENTS);
        }
    }

    private void flush(ServerLevel World) {
        List<PointBankBlockEntity> Banks = MiningGrid.runningBanks(World, worldPosition);
        PointBankBlockEntity Target = Banks.isEmpty() ? null : Banks.getFirst();
        for (BlockPos Member : Members) {
            if (!(World.getBlockEntity(Member) instanceof PointMinerBlockEntity Miner)) {
                continue;
            }
            Miner.Linked = Target != null;
            if (Target != null && Miner.Core.getBuffer() > 0) {
                Target.deposit(Miner.Core.takeBuffer());
                Miner.setChanged();
            }
        }
    }

    public FluidTank getTank() {
        return Water;
    }

    public MinerRules.Engine getCore() {
        return Core;
    }

    public int getStackSize() {
        return StackSize;
    }

    public int getStackIndex() {
        return StackIndex;
    }

    public boolean isLinked() {
        return Linked;
    }

    @Override
    public int getPowerDemand() {
        return isHead() ? MinerRules.demand(Math.max(1, Members.size())) : 0;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Math.max(0, Amount);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Water.writeToNBT(Registries, Tag);
        Tag.putInt("Heat", Core.getHeat());
        Tag.putBoolean("Overheated", Core.isOverheated());
        Tag.putInt("Work", Core.getWork());
        Tag.putInt("Buffer", Core.getBuffer());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Water.readFromNBT(Registries, Tag);
        Core.restore(Tag.getInt("Heat"), Tag.getBoolean("Overheated"), Tag.getInt("Work"), Tag.getInt("Buffer"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Water", Water.getFluidAmount());
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
