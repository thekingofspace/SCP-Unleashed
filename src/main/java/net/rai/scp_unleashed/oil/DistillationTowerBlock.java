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

public class DistillationTowerBlock extends OilMachineBlock {
    public static final MapCodec<DistillationTowerBlock> BlockCodec = simpleCodec(DistillationTowerBlock::new);
    public static final OilLayout Layout = new OilLayout(3, 10, 3, 1, List.of(
            new AABB(0, 0, 0, 48, 28, 48),
            new AABB(8, 28, 8, 40, 150, 40),
            new AABB(2, 56, 2, 46, 58, 46),
            new AABB(2, 104, 2, 46, 106, 46),
            new AABB(2, 140, 2, 46, 142, 46),
            new AABB(12, 150, 12, 36, 160, 36),
            new AABB(20, 26, 5, 28, 150, 8),
            new AABB(40, 132, 20, 48, 140, 28),
            new AABB(38, 60, 34, 46, 130, 38),
            new AABB(20, 20, 46, 28, 28, 48)),
            List.of(new AABB(46, 3, 19, 48, 13, 29)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 89);
    public static final int HeavyPart = Layout.part(0, 0, 1);
    public static final int FeedPart = Layout.part(1, 1, 2);
    public static final int LightPart = Layout.part(2, 8, 1);
    public static final int ActiveLight = 9;

    public DistillationTowerBlock(Properties Settings) {
        super(Settings);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Active) && State.getValue(Part) < 3 ? ActiveLight : 0;
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
        return OilRegistry.DistillationTowerEntity.get();
    }

    @Override
    protected OilMachineBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new DistillationTowerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public OilPort portAt(int Index, Direction LocalSide) {
        if (Index == FeedPart && LocalSide == Direction.SOUTH) {
            return OilPort.input(DistillationTowerBlockEntity.FeedTank);
        }
        if (Index == HeavyPart && LocalSide == Direction.WEST) {
            return OilPort.output(DistillationTowerBlockEntity.BottomTank);
        }
        if (Index == LightPart && LocalSide == Direction.EAST) {
            return OilPort.output(DistillationTowerBlockEntity.TopTank);
        }
        return null;
    }

    @Override
    protected String translationName() {
        return "distillation_tower";
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(detail("block.scp_unleashed.distillation_tower.size"));
        Lines.add(detail("block.scp_unleashed.distillation_tower.split", 60, 40));
        Lines.add(detail("block.scp_unleashed.distillation_tower.ports"));
        Lines.add(detail("block.scp_unleashed.distillation_tower.draw", DistillingRecipe.DefaultPower));
        Lines.add(detail("block.scp_unleashed.oil_machine.buckets"));
        Lines.add(detail("block.scp_unleashed.oil_machine.wiring"));
    }
}
