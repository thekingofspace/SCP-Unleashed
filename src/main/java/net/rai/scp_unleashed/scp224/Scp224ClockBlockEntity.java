package net.rai.scp_unleashed.scp224;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Scp224ClockBlockEntity extends BlockEntity {
    public static final int RoomLifetime = 600;
    public static final double NearClock = 2.5;
    public static final String SerialKey = "Serial";
    public static final String NextTollKey = "NextToll";
    public static final String StrokesKey = "Strokes";
    public static final String NextStrokeKey = "NextStroke";
    public static final String StilledKey = "Stilled";
    public static final String RestartKey = "RestartAt";
    public static final String FrozenKey = "FrozenTime";
    public static final String TollingKey = "Tolling";

    private long Serial;
    private long NextToll;
    private int StrokesLeft;
    private long NextStroke;
    private boolean Stilled;
    private long RestartAt;
    private long FrozenTime;
    private boolean Tolling;
    @Nullable
    private Scp224Room Room;
    private boolean RoomDirty = true;

    public Scp224ClockBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp224Registry.ClockEntity.get(), Pos, State);
    }

    public long getSerial() {
        if (Serial == 0L && level != null && !level.isClientSide) {
            long Rolled = 0L;
            while (Rolled == 0L) {
                Rolled = ThreadLocalRandom.current().nextLong();
            }
            Serial = Rolled;
            setChanged();
        }
        return Serial;
    }

    public boolean isStilled() {
        return Stilled;
    }

    public boolean isTolling() {
        return level != null && level.isClientSide ? Tolling : StrokesLeft > 0;
    }

    public long getFrozenTime() {
        return FrozenTime;
    }

    public long getNextToll() {
        return NextToll;
    }

    public long getRestartAt() {
        return RestartAt;
    }

    public int getStrokesLeft() {
        return StrokesLeft;
    }

    public Direction facing() {
        BlockState State = getBlockState();
        return State.hasProperty(HorizontalDirectionalBlock.FACING) ? State.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Scp224Clocks.add(this);
            getSerial();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Scp224Clocks.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Scp224Clocks.remove(this);
    }

    public Scp224Room room(Level World) {
        if (!World.isClientSide) {
            Scp224Clocks.add(this);
        }
        long Now = World.getGameTime();
        if (Room == null || RoomDirty || Now - Room.made() > RoomLifetime) {
            Room = Scp224Room.measure(World, worldPosition, Now);
            RoomDirty = false;
        }
        return Room;
    }

    @Nullable
    public Scp224Room cachedRoom() {
        return Room;
    }

    public boolean isRoomDirty() {
        return RoomDirty;
    }

    public void markRoomDirty() {
        RoomDirty = true;
    }

    public boolean roomTouches(BlockPos Pos) {
        if (Vec3.atCenterOf(Pos).distanceToSqr(Vec3.atCenterOf(worldPosition.above())) <= NearClock * NearClock) {
            return true;
        }
        return Room != null && Room.touches(Pos);
    }

    public void scheduleIn(long Ticks) {
        if (level != null) {
            NextToll = level.getGameTime() + Ticks;
            setChanged();
        }
    }

    public static void serverTick(ServerLevel World, BlockPos Pos, BlockState State, Scp224ClockBlockEntity Clock) {
        long Now = World.getGameTime();
        Direction Facing = State.getValue(HorizontalDirectionalBlock.FACING);
        if (Clock.Stilled) {
            if (Now >= Clock.RestartAt) {
                Clock.restart(World, false);
            }
            return;
        }
        if (Clock.StrokesLeft > 0) {
            if (Now >= Clock.NextStroke) {
                Scp224Toll.strike(World, Pos, Facing, Scp224Toll.Strokes - Clock.StrokesLeft);
                Clock.StrokesLeft--;
                Clock.NextStroke = Now + Scp224Toll.StrokeGap;
                if (Clock.StrokesLeft == 0) {
                    Clock.NextToll = Now + Scp224Toll.gap(World.random);
                    Clock.sync();
                }
                Clock.setChanged();
            }
            return;
        }
        if (Clock.NextToll <= 0L || Clock.NextToll < Now - Scp224Toll.MaxGap) {
            Clock.NextToll = Now + Scp224Toll.gap(World.random);
            Clock.setChanged();
            return;
        }
        if (Now < Clock.NextToll) {
            return;
        }
        if (!Scp224Toll.heard(World, Pos)) {
            Clock.NextToll = Now + Scp224Toll.gap(World.random);
            Clock.setChanged();
            return;
        }
        Clock.toll(World);
    }

    public List<LivingEntity> toll(ServerLevel World) {
        BlockState State = getBlockState();
        Direction Facing = State.getValue(HorizontalDirectionalBlock.FACING);
        long Now = World.getGameTime();
        List<LivingEntity> Caught = Scp224Toll.begin(World, worldPosition, Facing, this);
        StrokesLeft = Scp224Toll.Strokes - 1;
        NextStroke = Now + Scp224Toll.StrokeGap;
        NextToll = Now + Scp224Toll.gap(World.random);
        setChanged();
        sync();
        return Caught;
    }

    public void still(ServerLevel World) {
        if (Stilled) {
            return;
        }
        Stilled = true;
        StrokesLeft = 0;
        FrozenTime = World.getDayTime();
        RestartAt = World.getGameTime() + Scp224Toll.stillFor(World.random);
        setChanged();
        sync();
        Vec3 At = Vec3.atCenterOf(worldPosition);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp224Registry.HaltLayers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
        }
        World.gameEvent(null, GameEvent.BLOCK_CHANGE, worldPosition);
        Scp224Aging.releaseAll(World.getServer(), getSerial());
    }

    public void restart(ServerLevel World, boolean Pushed) {
        if (!Stilled) {
            return;
        }
        Stilled = false;
        NextToll = World.getGameTime() + Scp224Toll.gap(World.random);
        setChanged();
        sync();
        Vec3 At = Vec3.atCenterOf(worldPosition);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp224Registry.TickLayers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Pushed ? 0.9f : 0.6f, 0.9f);
        }
        World.gameEvent(null, GameEvent.BLOCK_CHANGE, worldPosition);
    }

    public void sync() {
        if (level != null) {
            Tolling = StrokesLeft > 0;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong(SerialKey, Serial);
        Tag.putLong(NextTollKey, NextToll);
        Tag.putInt(StrokesKey, StrokesLeft);
        Tag.putLong(NextStrokeKey, NextStroke);
        Tag.putBoolean(StilledKey, Stilled);
        Tag.putLong(RestartKey, RestartAt);
        Tag.putLong(FrozenKey, FrozenTime);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains(SerialKey)) {
            Serial = Tag.getLong(SerialKey);
        }
        if (Tag.contains(NextTollKey)) {
            NextToll = Tag.getLong(NextTollKey);
        }
        if (Tag.contains(StrokesKey)) {
            StrokesLeft = Tag.getInt(StrokesKey);
        }
        if (Tag.contains(NextStrokeKey)) {
            NextStroke = Tag.getLong(NextStrokeKey);
        }
        Stilled = Tag.getBoolean(StilledKey);
        RestartAt = Tag.getLong(RestartKey);
        FrozenTime = Tag.getLong(FrozenKey);
        Tolling = Tag.contains(TollingKey) ? Tag.getBoolean(TollingKey) : StrokesLeft > 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean(StilledKey, Stilled);
        Tag.putLong(FrozenKey, FrozenTime);
        Tag.putBoolean(TollingKey, StrokesLeft > 0);
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
