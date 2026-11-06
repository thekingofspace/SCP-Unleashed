package net.rai.scp_unleashed.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;

public enum SolarTier {
    Panel(8, 1200, List.of(BlockPos.ZERO), "block.scp_unleashed.solar_panel"),
    Array(40, 6000, List.of(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(1, 0, 1)), "block.scp_unleashed.solar_array");

    private final int PeakOutput;
    private final int Capacity;
    private final List<BlockPos> Cells;
    private final String TranslationKey;

    SolarTier(int PeakOutput, int Capacity, List<BlockPos> Cells, String TranslationKey) {
        this.PeakOutput = PeakOutput;
        this.Capacity = Capacity;
        this.Cells = Cells;
        this.TranslationKey = TranslationKey;
    }

    public int getPeakOutput() {
        return PeakOutput;
    }

    public int getCapacity() {
        return Capacity;
    }

    public List<BlockPos> getCells() {
        return Cells;
    }

    public String getTranslationKey() {
        return TranslationKey;
    }

    public BlockEntityType<SolarBlockEntity> entityType() {
        return this == Panel ? SolarRegistry.SolarPanelEntity.get() : SolarRegistry.SolarArrayEntity.get();
    }
}
