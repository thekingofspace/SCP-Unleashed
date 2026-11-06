package net.rai.scp_unleashed.labware.integration;

import net.rai.scp_unleashed.labware.LabAnalyzerBlock;
import net.rai.scp_unleashed.labware.LabPetriDishBlock;
import net.rai.scp_unleashed.labware.LabTubeHolderBlock;
import net.rai.scp_unleashed.labware.LabViralStorageBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class LabwareJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(LabViralStorageJadeProvider.Instance, LabViralStorageBlock.class);
        Registration.registerBlockDataProvider(LabAnalyzerJadeProvider.Instance, LabAnalyzerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(LabViralStorageJadeProvider.Instance, LabViralStorageBlock.class);
        Registration.registerBlockComponent(LabAnalyzerJadeProvider.Instance, LabAnalyzerBlock.class);
        Registration.registerBlockComponent(LabPetriDishJadeProvider.Instance, LabPetriDishBlock.class);
        Registration.registerBlockComponent(LabTubeHolderJadeProvider.Instance, LabTubeHolderBlock.class);
    }
}
