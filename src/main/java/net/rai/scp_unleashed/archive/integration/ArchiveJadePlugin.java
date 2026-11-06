package net.rai.scp_unleashed.archive.integration;

import net.rai.scp_unleashed.archive.database.DatabaseBlock;
import net.rai.scp_unleashed.archive.database.DatabaseTerminalBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ArchiveJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(DatabaseJadeProvider.Instance, DatabaseBlock.class);
        Registration.registerBlockComponent(DatabaseJadeProvider.Instance, DatabaseTerminalBlock.class);
    }
}
