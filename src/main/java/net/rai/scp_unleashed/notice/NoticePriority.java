package net.rai.scp_unleashed.notice;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum NoticePriority {
    Info(100),
    Lead(120),
    AnomalySpawn(200),
    Alert(200),
    Stranger(200);

    public static final StreamCodec<ByteBuf, NoticePriority> PriorityCodec = ByteBufCodecs.idMapper(NoticePriority::byId, NoticePriority::ordinal);

    private final int DefaultDuration;

    NoticePriority(int DefaultDuration) {
        this.DefaultDuration = DefaultDuration;
    }

    public int getDefaultDuration() {
        return DefaultDuration;
    }

    public boolean outranks(NoticePriority Other) {
        return ordinal() > Other.ordinal();
    }

    public boolean isGuaranteed() {
        return ordinal() >= AnomalySpawn.ordinal();
    }

    public static NoticePriority byId(int Id) {
        NoticePriority[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
