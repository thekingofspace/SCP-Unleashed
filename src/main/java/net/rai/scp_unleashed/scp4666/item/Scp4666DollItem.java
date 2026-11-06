package net.rai.scp_unleashed.scp4666.item;

import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public class Scp4666DollItem extends Scp4666KeepsakeItem {
    public Scp4666DollItem(Properties Properties) {
        super(Properties, "scp4666_flesh_doll", () -> Scp4666Registry.Sack.get(), 1.5f);
    }
}
