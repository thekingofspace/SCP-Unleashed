package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.rai.scp_unleashed.wire.WireChannels;

public record ActionStep(Kind Type, String Text, String Target, String TargetName, int Port, boolean Timed, int Seconds, int SoundId, boolean Start) {
    public static final int MaxText = 120;
    public static final int MinSeconds = 1;
    public static final int MaxSeconds = 3600;
    public static final String AllChannels = "all";
    public static final Codec<ActionStep> StepCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Kind.KindCodec.optionalFieldOf("type", Kind.Message).forGetter(ActionStep::Type),
            Codec.STRING.optionalFieldOf("text", "").forGetter(ActionStep::Text),
            Codec.STRING.optionalFieldOf("target", AllChannels).forGetter(ActionStep::Target),
            Codec.STRING.optionalFieldOf("target_name", "").forGetter(ActionStep::TargetName),
            Codec.INT.optionalFieldOf("port", 0).forGetter(ActionStep::Port),
            Codec.BOOL.optionalFieldOf("timed", true).forGetter(ActionStep::Timed),
            Codec.INT.optionalFieldOf("seconds", 10).forGetter(ActionStep::Seconds),
            Codec.INT.optionalFieldOf("sound", -1).forGetter(ActionStep::SoundId),
            Codec.BOOL.optionalFieldOf("start", true).forGetter(ActionStep::Start)
    ).apply(Instance, ActionStep::new));

    public ActionStep {
        Text = ComputerText.clean(Text, MaxText);
        Target = ChatChannels.isValidKey(Target) || AllChannels.equals(Target) ? Target : AllChannels;
        TargetName = ComputerText.upper(TargetName, ChatChannels.MaxName);
        Port = WireChannels.clampPort(Port);
        Seconds = Mth.clamp(Seconds, MinSeconds, MaxSeconds);
    }

    public static ActionStep message(String Text, String Target, String TargetName) {
        return new ActionStep(Kind.Message, Text, Target, TargetName, 0, true, 10, -1, true);
    }

    public static ActionStep redstone(int Port, boolean Timed, int Seconds) {
        return new ActionStep(Kind.Redstone, "", AllChannels, "", Port, Timed, Seconds, -1, true);
    }

    public static ActionStep sound(int SoundId, boolean Start) {
        return new ActionStep(Kind.Sound, "", AllChannels, "", 0, true, 10, SoundId, Start);
    }

    public enum Kind implements StringRepresentable {
        Message("message"),
        Redstone("redstone"),
        Sound("sound");

        public static final Codec<Kind> KindCodec = StringRepresentable.fromEnum(Kind::values);

        private final String SerializedName;

        Kind(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }
}
