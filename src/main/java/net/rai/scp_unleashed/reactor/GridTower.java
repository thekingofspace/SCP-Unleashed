package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.reactor.core.CoreTower;
import net.rai.scp_unleashed.reactor.core.TowerType;

import java.util.List;

public final class GridTower {
    public final CoreTower Core;
    public final List<BlockPos> Segments;
    public final List<TowerSegmentBlockEntity> Entities;
    public final IFluidHandler Inlet;
    public final GasHandler Outlet;
    public int Visual = -1;
    public boolean Vented;
    public boolean Blowing;
    public boolean Leaking;
    public boolean Captured;
    public long NextCreak;

    public GridTower(CoreTower Core, List<BlockPos> Segments, List<TowerSegmentBlockEntity> Entities) {
        this.Core = Core;
        this.Segments = List.copyOf(Segments);
        this.Entities = List.copyOf(Entities);
        Inlet = new TowerWaterInlet(() -> Core.Water, Amount -> Core.Water = Amount, Core.waterCapacity());
        Outlet = Core.Type == TowerType.Fuel ? new TowerGasOutlet(Core, GasRegistry.ToxicGas, true) : new TowerGasOutlet(Core, GasRegistry.Steam, false);
    }

    public TowerType getType() {
        return Core.Type;
    }

    public BlockPos bottom() {
        return Segments.getFirst();
    }

    public BlockPos top() {
        return Segments.getLast();
    }

    public int levelOf(BlockPos Pos) {
        return Segments.indexOf(Pos);
    }

    public Vec3 centre() {
        BlockPos Low = bottom();
        return new Vec3(Low.getX() + 0.5, Low.getY() + Segments.size() / 2.0, Low.getZ() + 0.5);
    }

    public AABB bounds() {
        BlockPos Low = bottom();
        BlockPos High = top();
        return new AABB(Low.getX(), Low.getY(), Low.getZ(), High.getX() + 1, High.getY() + 1, High.getZ() + 1);
    }

    public int spentRods() {
        int Count = 0;
        for (TowerSegmentBlockEntity Piece : Entities) {
            if (TowerFuel.isDepleted(Piece.getRod())) {
                Count++;
            }
        }
        return Count;
    }

    public int fuelPercent() {
        float Left = 0.0f;
        float Total = 0.0f;
        for (int Tier = 0; Tier < Core.BurnLeft.length && Tier < Entities.size(); Tier++) {
            if (TowerFuel.isFuel(Entities.get(Tier).getRod())) {
                Total += TowerFuel.burnTicks(Entities.get(Tier).getRod());
                Left += Core.BurnLeft[Tier];
            }
        }
        return Total <= 0.0f ? 0 : Math.round(Math.max(0.0f, Math.min(1.0f, Left / Total)) * 100.0f);
    }

    public int fuelMinutes() {
        float Rate = Math.max(0.05f, Core.withdrawal());
        float Longest = 0.0f;
        for (float Left : Core.BurnLeft) {
            Longest = Math.max(Longest, Left);
        }
        return Math.round(Longest / Rate / 1200.0f);
    }
}
