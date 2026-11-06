package net.rai.scp_unleashed.powerbridge;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PowerBridgeKind implements StringRepresentable {
    Inbound("inbound"),
    Outbound("outbound");

    public static final Codec<PowerBridgeKind> KindCodec = StringRepresentable.fromEnum(PowerBridgeKind::values);

    private final String Name;

    PowerBridgeKind(String Name) {
        this.Name = Name;
    }

    public boolean takesFe() {
        return this == Inbound;
    }

    public boolean givesFe() {
        return this == Outbound;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
