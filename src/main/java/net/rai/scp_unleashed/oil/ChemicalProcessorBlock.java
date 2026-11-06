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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChemicalProcessorBlock extends OilMachineBlock {
    public static final MapCodec<ChemicalProcessorBlock> BlockCodec = simpleCodec(ChemicalProcessorBlock::new);
    public static final OilLayout Layout = new OilLayout(3, 3, 3, 1, List.of(
            new AABB(0, 0, 0, 48, 4, 48),
            new AABB(10, 4, 12, 38, 40, 40),
            new AABB(20, 40, 22, 28, 46, 30),
            new AABB(1, 4, 17, 11, 30, 27),
            new AABB(36, 4, 17, 46, 30, 27),
            new AABB(14, 4, 2, 34, 20, 10),
            new AABB(5, 5, 27, 11, 11, 48),
            new AABB(37, 5, 27, 43, 11, 48),
            new AABB(21, 4, 40, 27, 10, 46)),
            List.of(new AABB(19, 3, 46, 29, 13, 48)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 26);
    public static final int InputPart = Layout.part(0, 0, 2);
    public static final int OutputPart = Layout.part(2, 0, 2);
    public static final int ActiveLight = 6;

    public ChemicalProcessorBlock(Properties Settings) {
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
        return OilRegistry.ChemicalProcessorEntity.get();
    }

    @Override
    protected OilMachineBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new ChemicalProcessorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public OilPort portAt(int Index, Direction LocalSide) {
        if (Index == InputPart && LocalSide == Direction.SOUTH) {
            return OilPort.input(ChemicalProcessorBlockEntity.InputTank);
        }
        if (Index == OutputPart && LocalSide == Direction.SOUTH) {
            return OilPort.output(ChemicalProcessorBlockEntity.OutputTank);
        }
        return null;
    }

    @Override
    protected String translationName() {
        return "chemical_processor";
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(detail("block.scp_unleashed.chemical_processor.recipes"));
        Lines.add(detail("block.scp_unleashed.chemical_processor.ports"));
        Lines.add(detail("block.scp_unleashed.chemical_processor.draw", ChemicalProcessingRecipe.DefaultPower));
        Lines.add(detail("block.scp_unleashed.oil_machine.buckets"));
        Lines.add(detail("block.scp_unleashed.oil_machine.wiring"));
    }
}
