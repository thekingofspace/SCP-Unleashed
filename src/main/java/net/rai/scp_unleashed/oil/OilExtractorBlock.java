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

public class OilExtractorBlock extends OilMachineBlock {
    public static final MapCodec<OilExtractorBlock> BlockCodec = simpleCodec(OilExtractorBlock::new);
    public static final OilLayout Layout = new OilLayout(3, 4, 6, 1, List.of(
            new AABB(4, 0, 2, 44, 6, 94),
            new AABB(18, 6, 2, 30, 28, 14),
            new AABB(0, 4, 4, 28, 12, 12),
            new AABB(20, 4, 0, 28, 12, 6),
            new AABB(10, 6, 30, 38, 44, 50),
            new AABB(14, 44, 36, 34, 54, 44),
            new AABB(18, 36, 8, 30, 64, 28),
            new AABB(20, 46, 28, 28, 54, 84),
            new AABB(2, 48, 78, 46, 52, 82),
            new AABB(16, 6, 62, 32, 30, 82),
            new AABB(18, 6, 84, 30, 20, 94),
            new AABB(2, 6, 86, 10, 30, 94),
            new AABB(10, 6, 58, 38, 34, 88)),
            List.of(new AABB(0, 19, 84, 2, 29, 94)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 71);
    public static final int WellPart = Layout.part(1, 0, 0);
    public static final int SidePart = Layout.part(0, 0, 0);

    public OilExtractorBlock(Properties Settings) {
        super(Settings);
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
        return OilRegistry.OilExtractorEntity.get();
    }

    @Override
    protected OilMachineBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new OilExtractorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public OilPort portAt(int Index, Direction LocalSide) {
        if ((Index == WellPart && LocalSide == Direction.NORTH) || (Index == SidePart && LocalSide == Direction.WEST)) {
            return OilPort.output(OilExtractorBlockEntity.CrudeTank);
        }
        return null;
    }

    @Override
    protected String translationName() {
        return "oil_extractor";
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(detail("block.scp_unleashed.oil_extractor.bedrock"));
        Lines.add(detail("block.scp_unleashed.oil_extractor.size"));
        Lines.add(detail("block.scp_unleashed.oil_extractor.rate", OilExtractorBlockEntity.OilPerStroke, OilExtractorBlockEntity.StrokeTicks / 20.0f));
        Lines.add(detail("block.scp_unleashed.oil_extractor.draw", OilExtractorBlockEntity.WorkDraw));
        Lines.add(detail("block.scp_unleashed.oil_extractor.ports"));
        Lines.add(detail("block.scp_unleashed.oil_machine.wiring"));
    }
}
