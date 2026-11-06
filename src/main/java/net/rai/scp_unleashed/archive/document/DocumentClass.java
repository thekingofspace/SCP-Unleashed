package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum DocumentClass implements StringRepresentable {
    Safe("safe", 0xFF4FB866, 0xFF2E7A3F),
    Euclid("euclid", 0xFFE8A93A, 0xFFA66A12),
    Keter("keter", 0xFFE0473A, 0xFFA3231B),
    Thaumiel("thaumiel", 0xFFA57CF0, 0xFF5B3A9E),
    Apollyon("apollyon", 0xFFB5222E, 0xFF2A0D10),
    Neutralized("neutralized", 0xFFA4A49A, 0xFF5C5C55),
    Unknown("unknown", 0xFF7D7A8C, 0xFF2B2833),
    ZClass("z_class", 0xFF38C6D2, 0xFF136873);

    public static final Codec<DocumentClass> ClassCodec = StringRepresentable.fromEnum(DocumentClass::values);
    public static final StreamCodec<ByteBuf, DocumentClass> ClassStreamCodec = ByteBufCodecs.idMapper(DocumentClass::byId, DocumentClass::ordinal);

    private final String SerializedName;
    private final int Colour;
    private final int Ink;

    DocumentClass(String SerializedName, int Colour, int Ink) {
        this.SerializedName = SerializedName;
        this.Colour = Colour;
        this.Ink = Ink;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public int getColour() {
        return Colour;
    }

    public int getInk() {
        return Ink;
    }

    public Component getName() {
        return Component.translatable("archive.scp_unleashed.class." + SerializedName);
    }

    public Component getSummary() {
        return Component.translatable("archive.scp_unleashed.class." + SerializedName + ".summary");
    }

    public boolean isRollable() {
        return this != ZClass;
    }

    public static DocumentClass byId(int Id) {
        DocumentClass[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
