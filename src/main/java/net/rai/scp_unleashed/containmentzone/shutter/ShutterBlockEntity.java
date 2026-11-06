package net.rai.scp_unleashed.containmentzone.shutter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;

import java.util.List;

public class ShutterBlockEntity extends BlockEntity {
    public static final int TravelTicks = 32;
    public static final int GraceTicks = 3;

    private float Progress;
    private float Previous;
    private boolean Pending = true;
    private boolean Seen;
    private long Stamp = Long.MIN_VALUE;
    private long Held = Long.MIN_VALUE;
    private long Blocked = Long.MIN_VALUE / 2;
    private boolean Lead;
    private int Clock;
    private List<BlockPos> Members = List.of();
    private ShutterShapes.Column Place = new ShutterShapes.Column(0, 1);
    private int Grace;
    private int Output;

    public ShutterBlockEntity(BlockPos Pos, BlockState State) {
        super(ShutterRegistry.Shutter.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ShutterBlockEntity Shutter) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof CellShutterBlock) {
            Shutter.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ShutterBlockEntity Shutter) {
        if (State.getBlock() instanceof CellShutterBlock) {
            Shutter.tickClient(World, State);
        }
    }

    private void tickClient(Level World, BlockState State) {
        Previous = Progress;
        float Goal = State.getValue(CellShutterBlock.Open) ? 1.0f : 0.0f;
        Progress = Mth.approach(Progress, Goal, 1.0f / TravelTicks);
        if (State.getValue(CellShutterBlock.Shut)) {
            Grace = Math.max(0, Grace - 1);
        } else {
            Grace = GraceTicks;
            Place = ShutterShapes.column(World, worldPosition, State);
        }
    }

    private void tickServer(ServerLevel World, BlockState State) {
        if (!State.getValue(CellShutterBlock.Shut) || Pending) {
            Place = ShutterShapes.column(World, worldPosition, State);
        }
        boolean Opening = State.getValue(CellShutterBlock.Open);
        float Goal = Opening ? 1.0f : 0.0f;
        long Now = World.getGameTime();
        Previous = Progress;
        if (Progress != Goal && Held != Now) {
            if (!Opening && Lead && ShutterAssembly.obstructed(World, Members, ShutterAssembly.CloseReach)) {
                Blocked = Now;
                ShutterAssembly.reverse(World, this);
                return;
            }
            Progress = Mth.approach(Progress, Goal, 1.0f / TravelTicks);
            setChanged();
            if (Lead) {
                Clock++;
                ShutterAssembly.travel(World, this, Opening, Clock);
            }
            if (Progress == Goal) {
                if (!Opening) {
                    World.setBlock(worldPosition, State.setValue(CellShutterBlock.Shut, true), Block.UPDATE_CLIENTS);
                }
                if (Lead) {
                    ShutterAssembly.arrive(World, this, Opening);
                    Lead = false;
                }
            }
            int Strength = outputOf(Progress);
            if (Strength != Output) {
                Output = Strength;
                World.updateNeighbourForOutputSignal(worldPosition, State.getBlock());
            }
        }
        if (Pending && Progress == Goal && Stamp != Now) {
            ShutterAssembly.evaluate(World, worldPosition);
        }
    }

    public static int outputOf(float Progress) {
        if (Progress <= 0.0f) {
            return 0;
        }
        return Math.min(15, 1 + Mth.floor(Progress * 14.0f));
    }

    public void notice(boolean Signal) {
        Pending = true;
        if (Signal) {
            Seen = true;
        }
        setChanged();
    }

    public void request() {
        Pending = true;
    }

    public boolean isPending() {
        return Pending;
    }

    public void settle(long Now) {
        Stamp = Now;
        Pending = false;
    }

    public void hold(long Now) {
        Held = Now;
    }

    public boolean consumeSeen() {
        boolean Was = Seen;
        Seen = false;
        return Was;
    }

    public void lead(List<BlockPos> Cells) {
        Lead = true;
        Clock = 0;
        Members = List.copyOf(Cells);
    }

    public void follow() {
        Lead = false;
        Members = List.of();
    }

    public boolean isLead() {
        return Lead;
    }

    public List<BlockPos> members() {
        return Members;
    }

    public long blockedAt() {
        return Blocked;
    }

    public void markBlocked(long Now) {
        Blocked = Now;
    }

    public float getProgress() {
        return Progress;
    }

    public float getProgress(float PartialTick) {
        return Mth.lerp(PartialTick, Previous, Progress);
    }

    public void adopt(float Value) {
        Progress = Float.isFinite(Value) ? Mth.clamp(Value, 0.0f, 1.0f) : 0.0f;
        Previous = Progress;
        Output = outputOf(Progress);
    }

    public ShutterShapes.Column place() {
        return Place;
    }

    public void place(ShutterShapes.Column Value) {
        Place = Value;
    }

    public boolean isMoving() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof CellShutterBlock && Progress != (State.getValue(CellShutterBlock.Open) ? 1.0f : 0.0f);
    }

    public boolean showsCurtain() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof CellShutterBlock && (!State.getValue(CellShutterBlock.Shut) || Grace > 0);
    }

    public int comparatorOutput() {
        return outputOf(Progress);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putFloat("Progress", Progress);
        Tag.putBoolean("Seen", Seen);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        adopt(Tag.getFloat("Progress"));
        Seen = Tag.getBoolean("Seen");
        Pending = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = super.getUpdateTag(Registries);
        Tag.putFloat("Progress", Progress);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
