package net.rai.scp_unleashed.mining.integration;

import net.rai.scp_unleashed.mining.PointBankBlock;
import net.rai.scp_unleashed.mining.PointMinerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MiningJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(MiningJadeProvider.Miner, PointMinerBlock.class);
        Registration.registerBlockDataProvider(MiningJadeProvider.Bank, PointBankBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(MiningJadeProvider.Miner, PointMinerBlock.class);
        Registration.registerBlockComponent(MiningJadeProvider.Bank, PointBankBlock.class);
    }
}
