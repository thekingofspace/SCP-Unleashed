package net.rai.scp_unleashed.rocketry.platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteItem;

import java.util.List;

public class SatellitePlatformBlock extends LaunchPlatformBlock {
    public static final MapCodec<SatellitePlatformBlock> BlockCodec = simpleCodec(SatellitePlatformBlock::new);
    public static final int Width = 3;
    public static final int Height = 3;
    public static final int Depth = 3;
    public static final OilLayout Layout = new OilLayout(Width, Height, Depth, 1, List.of(
            new AABB(0, 0, 0, 48, 6, 48),
            new AABB(16, 6, 2, 32, 16, 10),
            new AABB(12, 6, 12, 15, 40, 15),
            new AABB(33, 6, 12, 36, 40, 15),
            new AABB(12, 6, 33, 15, 40, 36),
            new AABB(33, 6, 33, 36, 40, 36),
            new AABB(3, 6, 36, 9, 44, 42)),
            List.of(new AABB(46, 4, 20, 48, 14, 28)));
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Width * Height * Depth - 1);
    public static final int FuelPart = Layout.part(0, 0, 1);
    public static final int SocketPart = Layout.part(2, 0, 1);
    public static final int ConsolePart = Layout.part(1, 0, 0);
    public static final double[] StandPoint = {24.0, 6.0, 24.0};
    public static final int ActiveLight = 6;
    private static final int[] LampParts = {ConsolePart};

    public SatellitePlatformBlock(Properties Settings) {
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
        return LaunchPlatformKind.Satellite;
    }

    @Override
    protected BlockEntityType<? extends LaunchPlatformBlockEntity> entityType() {
        return RocketryRegistry.SatellitePlatformEntity.get();
    }

    @Override
    protected LaunchPlatformBlockEntity createMaster(BlockPos Pos, BlockState State) {
        return new SatellitePlatformBlockEntity(Pos, State);
    }

    @Override
    public int fuelPart() {
        return FuelPart;
    }

    @Override
    public Direction fuelSide() {
        return Direction.WEST;
    }

    @Override
    public double[] standPoint() {
        return StandPoint;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!SatelliteItem.isPayload(Stack)) {
            return super.useItemOn(Stack, State, World, Pos, User, Hand, Hit);
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (masterEntity(World, Pos, State) instanceof SatellitePlatformBlockEntity Platform && Platform.loadPayload(Stack)) {
            World.playSound(null, Pos, RocketryRegistry.Clamps.get(), SoundSource.BLOCKS, 0.8f, 1.3f);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        LaunchPlatformKind Kind = kind();
        Lines.add(detail("block.scp_unleashed.satellite_platform.size", Width, Height, Depth));
        Lines.add(detail("block.scp_unleashed.satellite_platform.use"));
        Lines.add(detail("block.scp_unleashed.launch_platform.cost", Kind.getFuelCost(), Kind.getPowerCost()));
        Lines.add(detail("block.scp_unleashed.satellite_platform.ports"));
        Lines.add(detail("block.scp_unleashed.launch_platform.sky"));
        Lines.add(detail("block.scp_unleashed.oil_machine.buckets"));
    }
}
