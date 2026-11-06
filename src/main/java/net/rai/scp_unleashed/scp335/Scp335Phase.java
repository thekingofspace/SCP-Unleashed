package net.rai.scp_unleashed.scp335;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Scp335Phase implements StringRepresentable {
    None("none"),
    Glimpse("glimpse"),
    Crawl("crawl"),
    Rise("rise"),
    Walk("walk"),
    Dissolve("dissolve"),
    Gone("gone");

    public static final Codec<Scp335Phase> PhaseCodec = StringRepresentable.fromEnum(Scp335Phase::values);

    private final String SerializedName;

    Scp335Phase(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public static Scp335Phase byId(int Id) {
        Scp335Phase[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }

    public boolean isLocked() {
        return this != None;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
