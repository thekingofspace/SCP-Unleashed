package net.rai.scp_unleashed.rocketry.platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;

import java.util.List;

public class RocketPlatformBlock extends LaunchPlatformBlock {
    public static final MapCodec<RocketPlatformBlock> BlockCodec = simpleCodec(RocketPlatformBlock::new);
    public static final int Width = 5;
    public static final int Height = 4;
    public static final int Depth = 5;
    public static final OilLayout Layout = new OilLayout(Width, Height, Depth, 2, List.of(
            new AABB(0, 0, 0, 80, 6, 80),
            new AABB(2, 6, 2, 14, 20, 14),
            new AABB(66, 6, 2, 78, 18, 14),
            new AABB(3, 6, 35, 13, 62, 45)),
            List.of(new AABB(0, 4, 4, 2, 14, 12)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Width * Height * Depth - 1);
    public static final int FuelPart = Layout.part(4, 0, 0);
    public static final int SocketPart = Layout.part(0, 0, 0);
    public static final int ConsolePart = Layout.part(0, 1, 0);
    public static final double[] StandPoint = {40.0, 6.0, 40.0};
    public static final int ActiveLight = 6;
    public static final int BeaconPart = Layout.part(0, 3, 2);
    private static final int[] LampParts = {ConsolePart, BeaconPart};

    public RocketPlatformBlock(Properties Settings) {
        super(Settings);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Active) && State.getValue(Part) == ConsolePart ? ActiveLight : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public int[] lampParts() {
        return LampParts;
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
    public LaunchPlatformKind kind() {
        return LaunchPlatformKind.Rocket;
    }

    @Override
    protected BlockEntityType<? extends LaunchPlatformBlockEntity> entityType() {
        return RocketryRegistry.RocketPlatformEntity.get();
    }

    @Override
    protected LaunchPlatformBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new RocketPlatformBlockEntity(Pos, State);
    }

    @Override
    public int fuelPart() {
        return FuelPart;
    }

    @Override
    public Direction fuelSide() {
        return Direction.EAST;
    }

    @Override
    public double[] standPoint() {
        return StandPoint;
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        LaunchPlatformKind Kind = kind();
        Lines.add(detail("block.scp_unleashed.rocket_platform.size", Width, Height, Depth));
        Lines.add(detail("block.scp_unleashed.rocket_platform.use"));
        Lines.add(detail("block.scp_unleashed.rocket_platform.cost", Kind.getFuelCost(), Kind.getPowerCost()));
        Lines.add(detail("block.scp_unleashed.rocket_platform.ports"));
        Lines.add(detail("block.scp_unleashed.launch_platform.sky"));
        Lines.add(detail("block.scp_unleashed.oil_machine.buckets"));
    }
}
