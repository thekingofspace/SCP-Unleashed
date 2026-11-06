package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.rai.scp_unleashed.wire.WireChannels;

public record RedstoneButton(int Id, String Title, int Port, Mode Kind, int Seconds) {
    public static final int MaxTitle = 16;
    public static final int MinSeconds = 1;
    public static final int MaxSeconds = 300;
    public static final int DefaultSeconds = 5;
    public static final Codec<RedstoneButton> ButtonCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("id").forGetter(RedstoneButton::Id),
            Codec.STRING.optionalFieldOf("title", "").forGetter(RedstoneButton::Title),
            Codec.INT.optionalFieldOf("port", 0).forGetter(RedstoneButton::Port),
            Mode.ModeCodec.optionalFieldOf("mode", Mode.Toggle).forGetter(RedstoneButton::Kind),
            Codec.INT.optionalFieldOf("seconds", DefaultSeconds).forGetter(RedstoneButton::Seconds)
    ).apply(Instance, RedstoneButton::new));

    public RedstoneButton {
        Title = ComputerText.upper(Title, MaxTitle);
        if (Title.isEmpty()) {
            Title = "BUTTON";
        }
        Port = WireChannels.clampPort(Port);
        Seconds = Mth.clamp(Seconds, MinSeconds, MaxSeconds);
    }

    public RedstoneButton withId(int NewId) {
        return new RedstoneButton(NewId, Title, Port, Kind, Seconds);
    }

    public enum Mode implements StringRepresentable {
        Toggle("toggle"),
        Pulse("pulse");

        public static final Codec<Mode> ModeCodec = StringRepresentable.fromEnum(Mode::values);

        private final String SerializedName;

        Mode(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }
}
