package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.PowerSensorBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireNetworkManager;

public class PowerSensorBlockEntity extends BlockEntity {
    public static final int CheckTicks = 2;
    public static final int HoldTicks = 10;
    public static final double FlowThreshold = 0.5;
    public static final double UnlimitedScale = 1000.0;

    private long LastFlow = Long.MIN_VALUE / 2;
    private int ComparatorLevel;
    private double Flow;

    public PowerSensorBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.PowerSensor.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PowerSensorBlockEntity Sensor) {
        if (World instanceof ServerLevel ServerWorld && World.getGameTime() % CheckTicks == 0) {
            Sensor.sense(ServerWorld, State);
        }
    }

    private void sense(ServerLevel World, BlockState State) {
        double[] Load = WireNetworkManager.get(World).switchLoad(worldPosition);
        Flow = Load == null ? 0.0 : Load[0];
        double Limit = Load == null || Load[1] <= 0.0 || Load[1] >= Integer.MAX_VALUE / 2.0 ? UnlimitedScale : Load[1];
        long Now = World.getGameTime();
        if (Flow >= FlowThreshold) {
            LastFlow = Now;
        }
        boolean Sensing = Now - LastFlow <= HoldTicks;
        int Reading = Flow < FlowThreshold ? 0 : Mth.clamp((int) Math.ceil(15.0 * Flow / Limit), 1, 15);
        if (Reading != ComparatorLevel) {
            ComparatorLevel = Reading;
            World.updateNeighbourForOutputSignal(worldPosition, State.getBlock());
        }
        if (State.hasProperty(PowerSensorBlock.Powered) && State.getValue(PowerSensorBlock.Powered) != Sensing) {
            World.setBlock(worldPosition, State.setValue(PowerSensorBlock.Powered, Sensing), Block.UPDATE_ALL);
        }
    }

    public int getComparatorLevel() {
        return ComparatorLevel;
    }

    public double getFlow() {
        return Flow;
    }
}
