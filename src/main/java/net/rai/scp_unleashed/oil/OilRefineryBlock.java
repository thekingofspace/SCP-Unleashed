package net.rai.scp_unleashed.oil;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OilRefineryBlock extends OilMachineBlock {
    public static final MapCodec<OilRefineryBlock> BlockCodec = simpleCodec(OilRefineryBlock::new);
    public static final OilLayout Layout = new OilLayout(3, 4, 3, 1, List.of(
            new AABB(0, 0, 0, 48, 4, 48),
            new AABB(4, 4, 4, 44, 26, 40),
            new AABB(30, 4, 0, 42, 14, 4),
            new AABB(14, 26, 12, 34, 58, 32),
            new AABB(18, 58, 16, 30, 64, 28),
            new AABB(36, 26, 6, 46, 36, 38),
            new AABB(34, 36, 20, 48, 54, 28),
            new AABB(20, 4, 40, 28, 12, 48),
            new AABB(2, 5, 21, 4, 11, 27)),
            List.of(new AABB(0, 3, 19, 2, 13, 29)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 35);
    public static final int InputPart = Layout.part(1, 0, 2);
    public static final int WaterPart = Layout.part(2, 2, 1);
    public static final int ActiveLight = 6;

    public OilRefineryBlock(Properties Settings) {
        super(Settings);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Active) && State.getValue(Part) == Layout.getPlacePart() ? ActiveLight : 0;
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        OilMachineBlockEntity Machine = masterEntity(World, Pos, State);
        return Machine == null ? null : Machine.getItemHandler(Side);
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
        return OilRegistry.OilRefineryEntity.get();
    }

    @Override
    protected OilMachineBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new OilRefineryBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public OilPort portAt(int Index, Direction LocalSide) {
        if (Index == InputPart && LocalSide == Direction.SOUTH) {
            return OilPort.input(OilRefineryBlockEntity.InputTank);
        }
        if (Index == WaterPart && LocalSide == Direction.EAST) {
            return OilPort.output(OilRefineryBlockEntity.WaterTank);
        }
        return null;
    }

    @Override
    protected String translationName() {
        return "oil_refinery";
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(detail("block.scp_unleashed.oil_refinery.size"));
        Lines.add(detail("block.scp_unleashed.oil_refinery.recipe"));
        Lines.add(detail("block.scp_unleashed.oil_refinery.ports"));
        Lines.add(detail("block.scp_unleashed.oil_refinery.automation"));
        Lines.add(detail("block.scp_unleashed.oil_refinery.draw", OilRefiningRecipe.DefaultPower));
        Lines.add(detail("block.scp_unleashed.oil_machine.wiring"));
    }
}
