package net.rai.scp_unleashed.registry;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.BatteryBlock;
import net.rai.scp_unleashed.block.BreakerBlock;
import net.rai.scp_unleashed.block.ChargeDisperserBlock;
import net.rai.scp_unleashed.block.ChargedRedstoneBlock;
import net.rai.scp_unleashed.block.CoalGeneratorBlock;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.EnergumBlock;
import net.rai.scp_unleashed.block.MiniTankBlock;
import net.rai.scp_unleashed.block.NetworkRouterBlock;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.PortRangeBlock;
import net.rai.scp_unleashed.block.PowerCutoffBlock;
import net.rai.scp_unleashed.block.PowerSensorBlock;
import net.rai.scp_unleashed.block.CreativeEnergySourceBlock;
import net.rai.scp_unleashed.block.CreativePressurizerBlock;
import net.rai.scp_unleashed.block.CreativeTankBlock;
import net.rai.scp_unleashed.block.DuctWallBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.block.RedstoneConverterBlock;
import net.rai.scp_unleashed.block.RedstoneReceiverBlock;
import net.rai.scp_unleashed.block.RedstoneSenderBlock;
import net.rai.scp_unleashed.block.TankBlock;
import net.rai.scp_unleashed.block.TransferBlock;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.wire.ConnectorTier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);

    public static final DeferredBlock<OutletBlock> Outlet = BlockRegister.register("outlet", () -> new OutletBlock(ConnectorTier.Basic, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DropExperienceBlock> EnergumOre = BlockRegister.register("energum_ore", () -> new DropExperienceBlock(UniformInt.of(1, 4),
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<EnergumBlock> EnergumBlock = BlockRegister.register("energum_block", () -> new EnergumBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(5.0f, 6.0f).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<ChargedRedstoneBlock> ChargedRedstone = BlockRegister.register("charged_redstone", () -> new ChargedRedstoneBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<CoalGeneratorBlock> CoalGenerator = BlockRegister.register("coal_generator", () -> new CoalGeneratorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> State.getValue(CoalGeneratorBlock.Lit) ? 12 : 0)));
    public static final DeferredBlock<ChargeDisperserBlock> ChargeDisperser = BlockRegister.register("charge_disperser", () -> new ChargeDisperserBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .lightLevel(State -> State.getValue(ChargeDisperserBlock.Powered) ? 7 : 0)));
    public static final DeferredBlock<BatteryBlock> Battery = BlockRegister.register("battery", () -> new BatteryBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<NetworkRouterBlock> NetworkRouter = BlockRegister.register("network_router", () -> new NetworkRouterBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<RedstoneSenderBlock> RedstoneSender = BlockRegister.register("redstone_sender", () -> new RedstoneSenderBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<RedstoneReceiverBlock> RedstoneReceiver = BlockRegister.register("redstone_receiver", () -> new RedstoneReceiverBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<DuctBlock> Pipe = BlockRegister.register("pipe", () -> new DuctBlock(DuctKind.Pipe, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<DuctBlock> Vent = BlockRegister.register("vent", () -> new DuctBlock(DuctKind.Vent, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.COPPER_GRATE).noOcclusion()));
    public static final DeferredBlock<RedstoneConverterBlock> RedstoneConverter = BlockRegister.register("redstone_converter", () -> new RedstoneConverterBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<PortRangeBlock> PortRange = BlockRegister.register("port_range", () -> new PortRangeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<OutletBlock> AdvancedOutlet = BlockRegister.register("advanced_outlet", () -> new OutletBlock(ConnectorTier.Ultimate, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<OutletBlock> TetherOutlet = BlockRegister.register("tether_outlet", () -> new OutletBlock(ConnectorTier.Tether, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).strength(2.5f, 8.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<TransferBlock> BasicTransfer = BlockRegister.register("transfer_block", () -> new TransferBlock(ConnectorTier.Basic, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<TransferBlock> HeavyTransfer = BlockRegister.register("heavy_transfer_block", () -> new TransferBlock(ConnectorTier.Heavy, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<TransferBlock> UltimateTransfer = BlockRegister.register("ultimate_transfer_block", () -> new TransferBlock(ConnectorTier.Ultimate, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(5.0f, 10.0f).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<BreakerBlock> Breaker = BlockRegister.register("breaker", () -> new BreakerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<PowerCutoffBlock> PowerCutoff = BlockRegister.register("power_cutoff", () -> new PowerCutoffBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<PressurizerBlock> PipePressurizer = BlockRegister.register("pipe_pressurizer", () -> new PressurizerBlock(DuctKind.Pipe, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<PressurizerBlock> VentPressurizer = BlockRegister.register("vent_pressurizer", () -> new PressurizerBlock(DuctKind.Vent, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<TankBlock> Tank = BlockRegister.register("tank", () -> new TankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredBlock<MiniTankBlock> MiniTank = BlockRegister.register("mini_tank", () -> new MiniTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredBlock<PowerSensorBlock> PowerSensor = BlockRegister.register("power_sensor", () -> new PowerSensorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<CreativeEnergySourceBlock> CreativeEnergySource = BlockRegister.register("creative_energy_source", () -> new CreativeEnergySourceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA).requiresCorrectToolForDrops().strength(3.0f, 3600000.0f).sound(SoundType.METAL).lightLevel(State -> 10)));
    public static final DeferredBlock<CreativePressurizerBlock> CreativePressurizer = BlockRegister.register("creative_pressurizer", () -> new CreativePressurizerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA).requiresCorrectToolForDrops().strength(3.0f, 3600000.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<CreativeTankBlock> CreativeTank = BlockRegister.register("creative_tank", () -> new CreativeTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA).requiresCorrectToolForDrops().strength(3.0f, 3600000.0f).sound(SoundType.METAL).noOcclusion()
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredBlock<DuctWallBlock> PipeWallPort = BlockRegister.register("pipe_wall_port", () -> new DuctWallBlock(DuctKind.Pipe, BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));
    public static final DeferredBlock<DuctWallBlock> VentWallPort = BlockRegister.register("vent_wall_port", () -> new DuctWallBlock(DuctKind.Vent, BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));
}
