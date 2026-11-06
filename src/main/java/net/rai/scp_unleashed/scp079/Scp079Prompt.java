package net.rai.scp_unleashed.scp079;

import net.minecraft.network.chat.Component;

public enum Scp079Prompt {
    Who("who", false),
    Want("want", false),
    Status("status", false),
    Network("network", false),
    Leave("leave", false),
    Recent("recent", true);

    private final String Name;
    private final boolean UpgradedOnly;

    Scp079Prompt(String Name, boolean UpgradedOnly) {
        this.Name = Name;
        this.UpgradedOnly = UpgradedOnly;
    }

    public static Scp079Prompt byIndex(int Index) {
        Scp079Prompt[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Who;
    }

    public String getName() {
        return Name;
    }

    public boolean isUpgradedOnly() {
        return UpgradedOnly;
    }

    public Component getLabel() {
        return Component.translatable("gui.scp_unleashed.scp079.prompt." + Name);
    }
}
