package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.wire.WireChannels;

public record SoundEntry(int Id, String Title, int Port, ResourceLocation Sound, String Label, float Volume, float Pitch, boolean Loop) {
    public static final int MaxTitle = 16;
    public static final int MaxLabel = 32;
    public static final Codec<SoundEntry> EntryCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("id").forGetter(SoundEntry::Id),
            Codec.STRING.optionalFieldOf("title", "").forGetter(SoundEntry::Title),
            Codec.INT.optionalFieldOf("port", 0).forGetter(SoundEntry::Port),
            ResourceLocation.CODEC.optionalFieldOf("sound", SoundCatalog.first().Id()).forGetter(SoundEntry::Sound),
            Codec.STRING.optionalFieldOf("label", "").forGetter(SoundEntry::Label),
            Codec.FLOAT.optionalFieldOf("volume", SoundMessages.DefaultVolume).forGetter(SoundEntry::Volume),
            Codec.FLOAT.optionalFieldOf("pitch", SoundMessages.DefaultPitch).forGetter(SoundEntry::Pitch),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(SoundEntry::Loop)
    ).apply(Instance, SoundEntry::new));

    public SoundEntry {
        Title = ComputerText.upper(Title, MaxTitle);
        if (Title.isEmpty()) {
            Title = "SOUND";
        }
        Port = WireChannels.clampPort(Port);
        Label = ComputerText.clean(Label, MaxLabel);
        Volume = SoundConsoleBlockEntity.clampVolume(Volume);
        Pitch = SoundMessages.clampPitch(Pitch);
    }

    public SoundEntry withId(int NewId) {
        return new SoundEntry(NewId, Title, Port, Sound, Label, Volume, Pitch, Loop);
    }

    public SoundEntry withSound(ResourceLocation NewSound, String NewLabel) {
        return new SoundEntry(Id, Title, Port, NewSound, NewLabel, Volume, Pitch, Loop);
    }

    public Component soundName() {
        SoundCatalog.Entry Entry = SoundCatalog.find(Sound);
        if (Entry != null && !Entry.isAnomalous()) {
            return Component.translatable(Entry.getTranslationKey());
        }
        return Label.isEmpty() ? Component.literal(Sound.getPath()) : Component.literal(Label);
    }

    public String channel() {
        return SoundMessages.channel(Port);
    }

    public static String channelOf(int Port) {
        return WireChannels.onPort(SoundMessages.Type, Port);
    }
}
