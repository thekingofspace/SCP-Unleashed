package net.rai.scp_unleashed.wire;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ConnectorTier implements StringRepresentable {
    Basic("basic", 500, WireTier.Advanced),
    Heavy("heavy", 3000, WireTier.SuperHeavy),
    Ultimate("ultimate", 12000, WireTier.Ultimate),
    Tether("tether", 20000, WireTier.Tether);

    public static final Codec<ConnectorTier> TierCodec = StringRepresentable.fromEnum(ConnectorTier::values);

    private final String Name;
    private final int MaxDemand;
    private final WireTier Strongest;

    ConnectorTier(String Name, int MaxDemand, WireTier Strongest) {
        this.Name = Name;
        this.MaxDemand = MaxDemand;
        this.Strongest = Strongest;
    }

    public int getMaxDemand() {
        return MaxDemand;
    }

    public WireTier getStrongest() {
        return Strongest;
    }

    public boolean accepts(WireTier Tier) {
        return Tier == WireTier.Creative || Tier.ordinal() <= Strongest.ordinal();
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
