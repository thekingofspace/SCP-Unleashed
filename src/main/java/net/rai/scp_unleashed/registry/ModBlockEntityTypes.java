package net.rai.scp_unleashed.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.RenderableBlock;
import net.rai.scp_unleashed.block.TransferBlock;
import net.rai.scp_unleashed.block.entity.BatteryBlockEntity;
import net.rai.scp_unleashed.block.entity.BreakerBlockEntity;
import net.rai.scp_unleashed.block.entity.ChargeDisperserBlockEntity;
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.block.entity.DuctBlockEntity;
import net.rai.scp_unleashed.block.entity.DuctWallBlockEntity;
import net.rai.scp_unleashed.block.entity.EnergumBlockEntity;
import net.rai.scp_unleashed.block.entity.MiniTankBlockEntity;
import net.rai.scp_unleashed.block.entity.NetworkRouterBlockEntity;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.block.entity.PortRangeBlockEntity;
import net.rai.scp_unleashed.block.entity.PowerSensorBlockEntity;
import net.rai.scp_unleashed.block.entity.CreativeEnergySourceBlockEntity;
import net.rai.scp_unleashed.block.entity.CreativePressurizerBlockEntity;
import net.rai.scp_unleashed.block.entity.CreativeTankBlockEntity;
import net.rai.scp_unleashed.block.entity.PressurizerBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneConverterBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneReceiverBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneSenderBlockEntity;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import net.rai.scp_unleashed.block.entity.TankBlockEntity;
import net.rai.scp_unleashed.block.entity.TransferBlockEntity;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RenderableBlockEntity>> RenderableBlock = BlockEntityTypeRegister.register("renderable_block",
            () -> BlockEntityType.Builder.of(RenderableBlockEntity::new, BuiltInRegistries.BLOCK.stream()
                    .filter(Candidate -> Candidate instanceof RenderableBlock)
                    .toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutletBlockEntity>> Outlet = BlockEntityTypeRegister.register("outlet",
            () -> BlockEntityType.Builder.<OutletBlockEntity>of((Pos, State) -> State.getBlock() instanceof TransferBlock ? new TransferBlockEntity(Pos, State) : new OutletBlockEntity(Pos, State), ModBlocks.Outlet.get(), ModBlocks.AdvancedOutlet.get(), ModBlocks.TetherOutlet.get(),
                    ModBlocks.BasicTransfer.get(), ModBlocks.HeavyTransfer.get(), ModBlocks.UltimateTransfer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergumBlockEntity>> Energum = BlockEntityTypeRegister.register("energum_block",
            () -> BlockEntityType.Builder.of(EnergumBlockEntity::new, ModBlocks.EnergumBlock.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> CoalGenerator = BlockEntityTypeRegister.register("coal_generator",
            () -> BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new, ModBlocks.CoalGenerator.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargeDisperserBlockEntity>> ChargeDisperser = BlockEntityTypeRegister.register("charge_disperser",
            () -> BlockEntityType.Builder.of(ChargeDisperserBlockEntity::new, ModBlocks.ChargeDisperser.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> Battery = BlockEntityTypeRegister.register("battery",
            () -> BlockEntityType.Builder.of(BatteryBlockEntity::new, ModBlocks.Battery.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkRouterBlockEntity>> NetworkRouter = BlockEntityTypeRegister.register("network_router",
            () -> BlockEntityType.Builder.of(NetworkRouterBlockEntity::new, ModBlocks.NetworkRouter.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneSenderBlockEntity>> RedstoneSender = BlockEntityTypeRegister.register("redstone_sender",
            () -> BlockEntityType.Builder.of(RedstoneSenderBlockEntity::new, ModBlocks.RedstoneSender.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneReceiverBlockEntity>> RedstoneReceiver = BlockEntityTypeRegister.register("redstone_receiver",
            () -> BlockEntityType.Builder.of(RedstoneReceiverBlockEntity::new, ModBlocks.RedstoneReceiver.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneConverterBlockEntity>> RedstoneConverter = BlockEntityTypeRegister.register("redstone_converter",
            () -> BlockEntityType.Builder.of(RedstoneConverterBlockEntity::new, ModBlocks.RedstoneConverter.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortRangeBlockEntity>> PortRange = BlockEntityTypeRegister.register("port_range",
            () -> BlockEntityType.Builder.of(PortRangeBlockEntity::new, ModBlocks.PortRange.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreakerBlockEntity>> Breaker = BlockEntityTypeRegister.register("breaker",
            () -> BlockEntityType.Builder.of(BreakerBlockEntity::new, ModBlocks.Breaker.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DuctBlockEntity>> Duct = BlockEntityTypeRegister.register("duct",
            () -> BlockEntityType.Builder.of(DuctBlockEntity::new, ModBlocks.Pipe.get(), ModBlocks.Vent.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressurizerBlockEntity>> Pressurizer = BlockEntityTypeRegister.register("pressurizer",
            () -> BlockEntityType.Builder.of(PressurizerBlockEntity::new, ModBlocks.PipePressurizer.get(), ModBlocks.VentPressurizer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> Tank = BlockEntityTypeRegister.register("tank",
            () -> BlockEntityType.Builder.of(TankBlockEntity::new, ModBlocks.Tank.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiniTankBlockEntity>> MiniTank = BlockEntityTypeRegister.register("mini_tank",
            () -> BlockEntityType.Builder.of(MiniTankBlockEntity::new, ModBlocks.MiniTank.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerSensorBlockEntity>> PowerSensor = BlockEntityTypeRegister.register("power_sensor",
            () -> BlockEntityType.Builder.of(PowerSensorBlockEntity::new, ModBlocks.PowerSensor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEnergySourceBlockEntity>> CreativeEnergySource = BlockEntityTypeRegister.register("creative_energy_source",
            () -> BlockEntityType.Builder.of(CreativeEnergySourceBlockEntity::new, ModBlocks.CreativeEnergySource.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativePressurizerBlockEntity>> CreativePressurizer = BlockEntityTypeRegister.register("creative_pressurizer",
            () -> BlockEntityType.Builder.of(CreativePressurizerBlockEntity::new, ModBlocks.CreativePressurizer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeTankBlockEntity>> CreativeTank = BlockEntityTypeRegister.register("creative_tank",
            () -> BlockEntityType.Builder.of(CreativeTankBlockEntity::new, ModBlocks.CreativeTank.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DuctWallBlockEntity>> DuctWall = BlockEntityTypeRegister.register("duct_wall",
            () -> BlockEntityType.Builder.of(DuctWallBlockEntity::new, ModBlocks.PipeWallPort.get(), ModBlocks.VentWallPort.get()).build(null));
}
