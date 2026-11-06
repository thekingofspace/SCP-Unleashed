package net.rai.scp_unleashed.upgrade;

import java.util.Collection;

public interface Upgradeable {
    boolean acceptsUpgrade(UpgradeType Type);

    boolean hasUpgrade(UpgradeType Type);

    void installUpgrade(UpgradeType Type);

    default int countUpgrade(UpgradeType Type) {
        return hasUpgrade(Type) ? 1 : 0;
    }

    default int upgradeLimit(UpgradeType Type) {
        return Type.getLimit();
    }

    Collection<UpgradeType> getUpgrades();
}
