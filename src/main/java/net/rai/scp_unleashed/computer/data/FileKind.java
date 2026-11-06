package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum FileKind implements StringRepresentable {
    Dossier("dossier"),
    Note("note"),
    Image("image");

    public static final Codec<FileKind> KindCodec = StringRepresentable.fromEnum(FileKind::values);
    public static final StreamCodec<ByteBuf, FileKind> KindStreamCodec = ByteBufCodecs.idMapper(FileKind::byId, FileKind::ordinal);

    private final String Name;

    FileKind(String Name) {
        this.Name = Name;
    }

    public static FileKind byId(int Id) {
        FileKind[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
