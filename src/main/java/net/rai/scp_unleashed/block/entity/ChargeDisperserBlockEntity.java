package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.ChargeDisperserBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.ChargingRedstone;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.ArrayList;
import java.util.List;

public class ChargeDisperserBlockEntity extends BlockEntity implements WireDevice {
    public static final int PowerDraw = 40;
    public static final int PoweredWindow = 2;

    private final int[] Progress = new int[Direction.values().length];
    private long LastPowered = Long.MIN_VALUE / 2;
    private boolean HasTargets;

    public ChargeDisperserBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.ChargeDisperser.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ChargeDisperserBlockEntity Disperser) {
        Disperser.HasTargets = !Disperser.targets(World).isEmpty();
        boolean Active = World.getGameTime() - Disperser.LastPowered <= PoweredWindow;
        if (State.getValue(ChargeDisperserBlock.Powered) != Active) {
            World.setBlock(Pos, State.setValue(ChargeDisperserBlock.Powered, Active), Block.UPDATE_ALL);
        }
    }

    private List<Direction> targets(Level World) {
        List<Direction> Targets = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            BlockPos Neighbour = worldPosition.relative(Side);
            if (World.isLoaded(Neighbour) && ChargingRedstone.isChargeable(World.getBlockState(Neighbour))) {
                Targets.add(Side);
            } else {
                Progress[Side.ordinal()] = 0;
            }
        }
        return Targets;
    }

    public boolean hasTargets() {
        return HasTargets;
    }

    @Override
    public int getPowerDemand() {
        return HasTargets ? PowerDraw : 0;
    }

    @Override
    public void observeNetwork(long Available) {
        if (Available > 0 && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    @Override
    public void insertPower(int Amount) {
        if (!(level instanceof ServerLevel World) || Amount <= 0) {
            return;
        }
        LastPowered = World.getGameTime();
        List<Direction> Targets = targets(World);
        if (!Targets.isEmpty()) {
            int Share = Amount / Targets.size();
            int Remainder = Amount % Targets.size();
            for (int Index = 0; Index < Targets.size(); Index++) {
                Direction Side = Targets.get(Index);
                int Slot = Side.ordinal();
                BlockPos Target = worldPosition.relative(Side);
                ChargingRedstone.Charge Match = ChargingRedstone.chargeFor(World.getBlockState(Target));
                Progress[Slot] += Share + (Index < Remainder ? 1 : 0);
                if (Match != null && Progress[Slot] >= Match.Cost()) {
                    Progress[Slot] = 0;
                    ChargingRedstone.convert(World, Target);
                }
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putIntArray("Progress", Progress);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        int[] Saved = Tag.getIntArray("Progress");
        for (int Slot = 0; Slot < Progress.length; Slot++) {
            Progress[Slot] = Slot < Saved.length ? Math.max(0, Saved[Slot]) : 0;
        }
    }
}
