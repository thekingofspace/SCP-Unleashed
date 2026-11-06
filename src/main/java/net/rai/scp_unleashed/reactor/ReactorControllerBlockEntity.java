package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LinkRange = 48;
    public static final int CheckTicks = 10;
    public static final int MaxBackups = 4;

    @Nullable
    private BlockPos Link;
    private final List<BlockPos> Backups = new ArrayList<>();
    private boolean Powered;
    private int Comparator;

    public ReactorControllerBlockEntity(BlockPos Pos, BlockState State) {
        super(ReactorRegistry.ControllerEntity.get(), Pos, State);
    }

    public static boolean inRange(BlockPos Controller, BlockPos Target) {
        return Math.abs(Controller.getX() - Target.getX()) <= LinkRange && Math.abs(Controller.getY() - Target.getY()) <= LinkRange
                && Math.abs(Controller.getZ() - Target.getZ()) <= LinkRange;
    }

    @Nullable
    public static BlockPos touchingTower(BlockGetter World, BlockPos Pos) {
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (World.getBlockState(Next).getBlock() instanceof TowerSegmentBlock) {
                return Next.immutable();
            }
        }
        return null;
    }

    public void link(BlockPos Target) {
        Link = Target.immutable();
        Backups.clear();
        setChanged();
    }

    public boolean isLinked() {
        return Link != null;
    }

    @Nullable
    public BlockPos getLink() {
        return Link;
    }

    public void setPowered(boolean Value) {
        Powered = Value;
    }

    public int getComparator() {
        return Comparator;
    }

    @Nullable
    public ReactorGrid resolve(ServerLevel World) {
        if (Link == null) {
            BlockPos Touching = touchingTower(World, worldPosition);
            if (Touching == null) {
                return null;
            }
            link(Touching);
        }
        ReactorGridManager Manager = ReactorGridManager.get(World);
        ReactorGrid Grid = inRange(worldPosition, Link) ? Manager.gridAt(World, Link) : null;
        if (Grid == null) {
            for (BlockPos Backup : List.copyOf(Backups)) {
                if (inRange(worldPosition, Backup)) {
                    Grid = Manager.gridAt(World, Backup);
                    if (Grid != null) {
                        Link = Backup;
                        setChanged();
                        break;
                    }
                }
            }
        }
        if (Grid != null && World.getGameTime() % 100 == 0) {
            remember(Grid);
        }
        return Grid;
    }

    private void remember(ReactorGrid Grid) {
        List<BlockPos> Picked = new ArrayList<>();
        for (GridTower Tower : Grid.getTowers()) {
            if (Picked.size() >= MaxBackups) {
                break;
            }
            if (!Tower.bottom().equals(Link)) {
                Picked.add(Tower.bottom());
            }
        }
        if (!Picked.equals(Backups)) {
            Backups.clear();
            Backups.addAll(Picked);
            setChanged();
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ReactorControllerBlockEntity Controller) {
        if (World instanceof ServerLevel ServerWorld && Math.floorMod(World.getGameTime() + Pos.hashCode(), CheckTicks) == 0) {
            Controller.tickServer(ServerWorld, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        ReactorGrid Grid = resolve(World);
        int Status = 0;
        int Signal = 0;
        if (Grid != null) {
            if (Powered) {
                Grid.holdScram(World.getGameTime());
            }
            float Hottest = ReactorTuning.Ambient;
            float Weakest = ReactorTuning.FullIntegrity;
            for (GridTower Tower : Grid.getTowers()) {
                if (Tower.getType() == TowerType.Fuel) {
                    Hottest = Math.max(Hottest, Tower.Core.Temperature);
                    Weakest = Math.min(Weakest, Tower.Core.Integrity);
                }
            }
            TowerHeat Band = TowerHeat.of(Hottest, Weakest);
            Status = Grid.getState() != GridState.Ready ? 2 : switch (Band) {
                case Stable -> 1;
                case Hot -> 2;
                default -> 3;
            };
            float Fraction = ReactorTuning.heatFraction(Hottest);
            Signal = Hottest <= ReactorTuning.Ambient + 5.0f ? 0 : Math.min(15, 1 + (int) (Fraction * 14.0f));
        }
        if (State.getValue(ReactorControllerBlock.Status) != Status) {
            World.setBlock(Pos, State.setValue(ReactorControllerBlock.Status, Status), Block.UPDATE_CLIENTS);
        }
        if (Signal != Comparator) {
            Comparator = Signal;
            World.updateNeighbourForOutputSignal(Pos, State.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Link != null) {
            Tag.putLong("Link", Link.asLong());
        }
        long[] Saved = new long[Backups.size()];
        for (int Index = 0; Index < Saved.length; Index++) {
            Saved[Index] = Backups.get(Index).asLong();
        }
        Tag.putLongArray("Backups", Saved);
        Tag.putBoolean("Powered", Powered);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            return;
        }
        Link = Tag.contains("Link") ? BlockPos.of(Tag.getLong("Link")) : null;
        Backups.clear();
        for (long Packed : Tag.getLongArray("Backups")) {
            Backups.add(BlockPos.of(Packed));
        }
        Powered = Tag.getBoolean("Powered");
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
        return Component.translatable("container.scp_unleashed.reactor_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new ReactorControllerMenu(ContainerId, PlayerInventory, this);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
