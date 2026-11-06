package net.rai.scp_unleashed.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.TankBlock;
import net.rai.scp_unleashed.pressure.MiniTankColumn;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Holder, Side) -> TankBlock.capabilityAt(World, Pos, State, Side), ModBlocks.Tank.get());
        Event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntityTypes.MiniTank.get(), (Tank, Side) -> MiniTankColumn.of(Tank.getLevel(), Tank.getBlockPos()));
        Event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntityTypes.CreativeTank.get(), (Tank, Side) -> Tank.getHandler());
    }
}
