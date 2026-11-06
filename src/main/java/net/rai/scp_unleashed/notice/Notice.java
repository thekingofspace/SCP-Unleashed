package net.rai.scp_unleashed.notice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.rai.scp_unleashed.archive.document.DocumentClass;

import java.util.Optional;

public record Notice(NoticeIcon Icon, Component Title, Optional<Component> Detail, NoticePriority Priority, int Duration, int Accent) {
    public static final int MinDuration = 20;
    public static final int MaxDuration = 1200;
    public static final int NoAccent = 0;
    public static final StreamCodec<RegistryFriendlyByteBuf, Notice> NoticeCodec = StreamCodec.composite(
            NoticeIcon.IconCodec, Notice::Icon,
            ComponentSerialization.STREAM_CODEC, Notice::Title,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Notice::Detail,
            NoticePriority.PriorityCodec, Notice::Priority,
            ByteBufCodecs.VAR_INT, Notice::Duration,
            ByteBufCodecs.INT, Notice::Accent,
            Notice::new);

    public Notice {
        Duration = Math.max(MinDuration, Math.min(MaxDuration, Duration));
    }

    public static Notice of(NoticeIcon Icon, Component Title, NoticePriority Priority) {
        return new Notice(Icon, Title, Optional.empty(), Priority, Priority.getDefaultDuration(), NoAccent);
    }

    public static Notice info(Component Title) {
        return of(NoticeIcon.Info, Title, NoticePriority.Info).withAccent(0x7FB2E5);
    }

    public static Notice lead(Component Title) {
        return of(NoticeIcon.Lead, Title, NoticePriority.Lead).withAccent(0xC9CDD4);
    }

    public static Notice alert(Component Title) {
        return of(NoticeIcon.Alert, Title, NoticePriority.Alert).withAccent(0xF2C230);
    }

    public static Notice anomaly(DocumentClass Classification, Component Title) {
        return of(NoticeIcon.anomaly(Classification), Title, NoticePriority.AnomalySpawn).withAccent(Classification.getColour());
    }

    public static Notice stranger(String VariantId, Component Title, int Accent) {
        return of(NoticeIcon.stranger(VariantId), Title, NoticePriority.Stranger).withAccent(Accent);
    }

    public Notice withDetail(Component Line) {
        return new Notice(Icon, Title, Optional.ofNullable(Line), Priority, Duration, Accent);
    }

    public Notice withPriority(NoticePriority Level) {
        return new Notice(Icon, Title, Detail, Level, Duration == Priority.getDefaultDuration() ? Level.getDefaultDuration() : Duration, Accent);
    }

    public Notice withDuration(int Ticks) {
        return new Notice(Icon, Title, Detail, Priority, Ticks, Accent);
    }

    public Notice withAccent(int Colour) {
        return new Notice(Icon, Title, Detail, Priority, Duration, Colour == NoAccent ? NoAccent : 0xFF000000 | Colour);
    }

    public Notice withIcon(NoticeIcon Shown) {
        return new Notice(Shown, Title, Detail, Priority, Duration, Accent);
    }

    public boolean sameAs(Notice Other) {
        return Icon.equals(Other.Icon) && Title.equals(Other.Title) && Detail.equals(Other.Detail);
    }
}
