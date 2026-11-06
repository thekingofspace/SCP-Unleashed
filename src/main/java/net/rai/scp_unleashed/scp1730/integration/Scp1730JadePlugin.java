package net.rai.scp_unleashed.scp1730.integration;

import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp1730JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp1730FolderJadeProvider.Instance, RealityFolderBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp1730FolderJadeProvider.Instance, RealityFolderBlock.class);
        Registration.registerBlockComponent(Scp1730ControlJadeProvider.Instance, Scp1730ControlBlock.class);
        Registration.registerBlockComponent(Scp1730DefenceJadeProvider.Turret, Scp1730TurretBlock.class);
        Registration.registerBlockComponent(Scp1730DefenceJadeProvider.Laser, Scp1730LaserBlock.class);
    }
}
