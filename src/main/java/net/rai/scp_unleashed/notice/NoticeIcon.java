package net.rai.scp_unleashed.notice;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.document.DocumentClass;

public record NoticeIcon(Kind Type, String Id) {
    public static final NoticeIcon Info = generic("info");
    public static final NoticeIcon Lead = generic("lead");
    public static final NoticeIcon Alert = generic("alert");
    public static final StreamCodec<ByteBuf, NoticeIcon> IconCodec = StreamCodec.composite(
            ByteBufCodecs.idMapper(Kind::byId, Kind::ordinal), NoticeIcon::Type,
            ByteBufCodecs.STRING_UTF8, NoticeIcon::Id,
            NoticeIcon::new);

    public enum Kind {
        Generic,
        Anomaly,
        Stranger,
        Texture;

        public static Kind byId(int Id) {
            Kind[] All = values();
            return All[Math.max(0, Math.min(All.length - 1, Id))];
        }
    }

    public static NoticeIcon anomaly(DocumentClass Classification) {
        return new NoticeIcon(Kind.Anomaly, Classification.getSerializedName());
    }

    public static NoticeIcon stranger(String VariantId) {
        return new NoticeIcon(Kind.Stranger, VariantId);
    }

    public static NoticeIcon generic(String Name) {
        return new NoticeIcon(Kind.Generic, Name);
    }

    public static NoticeIcon texture(ResourceLocation Location) {
        return new NoticeIcon(Kind.Texture, Location.toString());
    }
}
