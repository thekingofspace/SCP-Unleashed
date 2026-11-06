package net.rai.scp_unleashed.processing;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public enum PressDie implements StringRepresentable {
    Plate("plate"),
    Rod("rod");

    public static final Codec<PressDie> DieCodec = StringRepresentable.fromEnum(PressDie::values);
    public static final StreamCodec<FriendlyByteBuf, PressDie> DieStreamCodec = NeoForgeStreamCodecs.enumCodec(PressDie.class);

    private final String Name;

    PressDie(String Name) {
        this.Name = Name;
    }

    public static PressDie byIndex(int Index) {
        PressDie[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Plate;
    }

    public static PressDie byName(String Wanted) {
        for (PressDie Die : values()) {
            if (Die.Name.equals(Wanted)) {
                return Die;
            }
        }
        return Plate;
    }

    public PressDie next() {
        return byIndex((ordinal() + 1) % values().length);
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.item_press.die." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
