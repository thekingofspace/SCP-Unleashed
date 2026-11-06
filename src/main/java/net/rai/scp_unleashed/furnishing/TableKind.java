package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TableKind implements StringRepresentable {
    Conference("conference", 13),
    Cafeteria("cafeteria", 14);

    public static final Codec<TableKind> KindCodec = StringRepresentable.fromEnum(TableKind::values);

    private final String Name;
    private final int TopLow;

    TableKind(String Name, int TopLow) {
        this.Name = Name;
        this.TopLow = TopLow;
    }

    public int getTopLow() {
        return TopLow;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
