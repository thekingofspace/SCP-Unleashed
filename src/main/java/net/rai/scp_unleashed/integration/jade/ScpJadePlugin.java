package net.rai.scp_unleashed.integration.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.block.CoalGeneratorBlock;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ScpJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, BlockEntity.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, PoweredBlock.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, CoalGeneratorBlock.class);
        Registration.registerBlockDataProvider(ScpPressureProvider.Instance, DuctBlock.class);
        Registration.registerBlockDataProvider(ScpPressureProvider.Instance, PressurizerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ScpPowerProvider.Instance, Block.class);
        Registration.registerBlockComponent(ScpPressureProvider.Instance, DuctBlock.class);
        Registration.registerBlockComponent(ScpPressureProvider.Instance, PressurizerBlock.class);
    }
}
