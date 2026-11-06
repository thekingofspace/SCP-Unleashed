package net.rai.scp_unleashed.matrix.integration;

import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MatrixJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(MatrixJadeProvider.Instance, InstabilityMatrixBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(MatrixJadeProvider.Instance, InstabilityMatrixBlock.class);
    }
}
