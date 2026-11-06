package net.rai.scp_unleashed.oil;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.pressure.PortRole;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CombustionGeneratorBlock extends OilMachineBlock {
    public static final MapCodec<CombustionGeneratorBlock> BlockCodec = simpleCodec(CombustionGeneratorBlock::new);
    public static final OilLayout Layout = new OilLayout(3, 2, 4, 1, List.of(
            new AABB(2, 0, 2, 46, 3, 62),
            new AABB(4, 3, 2, 44, 28, 14),
            new AABB(2, 3, 14, 48, 24, 46),
            new AABB(16, 24, 18, 30, 28, 44),
            new AABB(32, 28, 4, 40, 32, 12),
            new AABB(5, 20, 37, 11, 32, 43),
            new AABB(13, 3, 46, 35, 30, 62)),
            List.of(new AABB(0, 3, 35, 2, 13, 45)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 23);
    public static final int FuelPart = Layout.part(2, 0, 1);
    public static final int ExhaustPart = Layout.part(0, 1, 2);
    public static final int ActiveLight = 8;

    public CombustionGeneratorBlock(Properties Settings) {
        super(Settings);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Active) && State.getValue(Part) == Layout.getPlacePart() ? ActiveLight : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public OilLayout layout() {
        return Layout;
    }

    @Override
    public IntegerProperty partProperty() {
        return Part;
    }

    @Override
    protected BlockEntityType<? extends OilMachineBlockEntity> entityType() {
        return OilRegistry.CombustionGeneratorEntity.get();
    }

    @Override
    protected OilMachineBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new CombustionGeneratorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public OilPort portAt(int Index, Direction LocalSide) {
        if (Index == FuelPart && LocalSide == Direction.EAST) {
            return OilPort.input(CombustionGeneratorBlockEntity.FuelTank);
        }
        return null;
    }

    @Nullable
    @Override
    public PortRole gasAt(int Index, Direction LocalSide) {
        return Index == ExhaustPart && LocalSide == Direction.UP ? PortRole.Output : null;
    }

    @Override
    protected String translationName() {
        return "combustion_generator";
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(detail("block.scp_unleashed.combustion_generator.size"));
        Lines.add(detail("block.scp_unleashed.combustion_generator.fuels"));
        Lines.add(detail("block.scp_unleashed.combustion_generator.exhaust"));
        Lines.add(detail("block.scp_unleashed.combustion_generator.ports"));
        Lines.add(detail("block.scp_unleashed.combustion_generator.capacity", CombustionGeneratorBlockEntity.Capacity));
        Lines.add(detail("block.scp_unleashed.oil_machine.buckets"));
        Lines.add(detail("block.scp_unleashed.oil_machine.wiring"));
    }
}
