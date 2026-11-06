package net.rai.scp_unleashed.scp085;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp085SheetBlockEntity extends BlockEntity {
    boolean Here;
    Scp085Act Act = Scp085Act.Idle;
    long ActStart;
    int ActLength = 40;
    int FromX = 32;
    int ToX = 32;
    int Facing = 1;
    Scp085Form Form = Scp085Form.Woman;
    int Param;
    int QueuedSpot = -1;
    int PendingReact = -1;
    int LastReact = -1;
    long StayUntil;
    long FormUntil;
    long ShiftReadyAt;
    long ReactReadyAt;
    long WaveReadyAt;
    long LastSound;
    @Nullable
    UUID LastWaved;
    @Nullable
    BlockPos HopTarget;
    @Nullable
    BlockPos CameFrom;
    @Nullable
    UUID Watcher;
    private boolean NeedsSync;

    public Scp085SheetBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp085Registry.SheetEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        Scp085Brain.tick(Server, Pos, State, Sheet);
        if (Sheet.NeedsSync) {
            Sheet.NeedsSync = false;
            World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isHere() {
        return Here;
    }

    public Scp085Act act() {
        return Act;
    }

    public long actStart() {
        return ActStart;
    }

    public int actLength() {
        return ActLength;
    }

    public int fromX() {
        return FromX;
    }

    public int toX() {
        return ToX;
    }

    public int facing() {
        return Facing;
    }

    public Scp085Form form() {
        return Form;
    }

    public int param() {
        return Param;
    }

    public Scp085Scene scene() {
        return getBlockState().getBlock() instanceof Scp085SheetBlock Sheet ? Sheet.sceneOf(getBlockState()) : Scp085Scene.Room;
    }

    void start(Scp085Act Next, long Now, int Length, int From, int To, int Param) {
        this.Act = Next;
        this.ActStart = Now;
        this.ActLength = Math.max(1, Length);
        this.FromX = From;
        this.ToX = To;
        this.Param = Param;
        changed();
    }

    void changed() {
        NeedsSync = true;
        setChanged();
    }

    void syncNow() {
        NeedsSync = false;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void writeShown(CompoundTag Tag) {
        Tag.putBoolean("Here", Here);
        Tag.putInt("Act", Act.ordinal());
        Tag.putLong("ActStart", ActStart);
        Tag.putInt("ActLength", ActLength);
        Tag.putInt("FromX", FromX);
        Tag.putInt("ToX", ToX);
        Tag.putInt("Facing", Facing);
        Tag.putInt("Form", Form.ordinal());
        Tag.putInt("Param", Param);
    }

    private void readShown(CompoundTag Tag) {
        Here = Tag.getBoolean("Here");
        Act = Scp085Act.byIndex(Tag.getInt("Act"));
        ActStart = Tag.getLong("ActStart");
        ActLength = Math.max(1, Tag.getInt("ActLength"));
        FromX = Tag.getInt("FromX");
        ToX = Tag.getInt("ToX");
        Facing = Tag.getInt("Facing") < 0 ? -1 : 1;
        Form = Scp085Form.byIndex(Tag.getInt("Form"));
        Param = Tag.getInt("Param");
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        writeShown(Tag);
        Tag.putInt("QueuedSpot", QueuedSpot);
        Tag.putInt("PendingReact", PendingReact);
        Tag.putInt("LastReact", LastReact);
        Tag.putLong("StayUntil", StayUntil);
        Tag.putLong("FormUntil", FormUntil);
        Tag.putLong("ShiftReadyAt", ShiftReadyAt);
        Tag.putLong("ReactReadyAt", ReactReadyAt);
        Tag.putLong("WaveReadyAt", WaveReadyAt);
        if (LastWaved != null) {
            Tag.putUUID("LastWaved", LastWaved);
        }
        if (HopTarget != null) {
            Tag.putLong("HopTarget", HopTarget.asLong());
        }
        if (CameFrom != null) {
            Tag.putLong("CameFrom", CameFrom.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        readShown(Tag);
        QueuedSpot = Tag.contains("QueuedSpot") ? Tag.getInt("QueuedSpot") : -1;
        PendingReact = Tag.contains("PendingReact") ? Tag.getInt("PendingReact") : -1;
        LastReact = Tag.contains("LastReact") ? Tag.getInt("LastReact") : -1;
        StayUntil = Tag.getLong("StayUntil");
        FormUntil = Tag.getLong("FormUntil");
        ShiftReadyAt = Tag.getLong("ShiftReadyAt");
        ReactReadyAt = Tag.getLong("ReactReadyAt");
        WaveReadyAt = Tag.getLong("WaveReadyAt");
        LastWaved = Tag.hasUUID("LastWaved") ? Tag.getUUID("LastWaved") : null;
        HopTarget = Tag.contains("HopTarget") ? BlockPos.of(Tag.getLong("HopTarget")) : null;
        CameFrom = Tag.contains("CameFrom") ? BlockPos.of(Tag.getLong("CameFrom")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        writeShown(Tag);
        return Tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag Tag, HolderLookup.Provider Registries) {
        readShown(Tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
