package net.rai.scp_unleashed.scp4666.item;

import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public class Scp4666NecklaceItem extends Scp4666KeepsakeItem {
    public Scp4666NecklaceItem(Properties Properties) {
        super(Properties, "scp4666_tooth_necklace", () -> Scp4666Registry.Chime.get(), 1.7f);
    }
}
