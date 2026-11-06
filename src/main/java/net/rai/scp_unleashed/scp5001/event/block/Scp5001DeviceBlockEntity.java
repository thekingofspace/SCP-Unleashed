package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp5001.event.Scp5001Event;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;
import org.jetbrains.annotations.Nullable;

public class Scp5001DeviceBlockEntity extends BlockEntity {
    public static final int SyncEvery = 10;
    private int Clock;

    public Scp5001DeviceBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
        Clock = Math.floorMod(Pos.hashCode(), SyncEvery);
    }

    @Nullable
    public Scp5001EventData.Kind kind() {
        return getBlockState().getBlock() instanceof Scp5001DeviceBlock Device ? Device.kind() : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World && World == World.getServer().overworld() && kind() != null) {
            Scp5001Event.registerDevice(World, worldPosition, kind());
        }
    }

    public void serverTick() {
        Clock++;
        if (Clock % SyncEvery != 0 || !(level instanceof ServerLevel World)) {
            return;
        }
        Scp5001Event.syncDevice(World, worldPosition, getBlockState(), this);
    }
}
