package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public record ChatMessage(long Id, Sender From, String Name, String Text, String Argument, Optional<ComputerFile> Attachment, long Stamp, boolean Memetic) {
    public static final int MaxText = 160;
    public static final int MaxName = 24;
    public static final Codec<ChatMessage> MessageCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.LONG.fieldOf("id").forGetter(ChatMessage::Id),
            Sender.SenderCodec.optionalFieldOf("from", Sender.Player).forGetter(ChatMessage::From),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ChatMessage::Name),
            Codec.STRING.optionalFieldOf("text", "").forGetter(ChatMessage::Text),
            Codec.STRING.optionalFieldOf("argument", "").forGetter(ChatMessage::Argument),
            ComputerFile.FileCodec.optionalFieldOf("attachment").forGetter(ChatMessage::Attachment),
            Codec.LONG.optionalFieldOf("stamp", 0L).forGetter(ChatMessage::Stamp),
            Codec.BOOL.optionalFieldOf("memetic", false).forGetter(ChatMessage::Memetic)
    ).apply(Instance, ChatMessage::new));

    public ChatMessage {
        if (From != Sender.Anomaly) {
            Name = ComputerText.clean(Name, MaxName);
            Text = ComputerText.clean(Text, MaxText);
        }
        Argument = ComputerText.clean(Argument, MaxName);
    }

    public ChatMessage withId(long NewId) {
        return new ChatMessage(NewId, From, Name, Text, Argument, Attachment, Stamp, Memetic);
    }

    public Component nameComponent() {
        return From == Sender.Anomaly ? Component.translatable(Name) : Component.literal(Name);
    }

    public Component textComponent() {
        if (From == Sender.Anomaly) {
            return Argument.isEmpty() ? Component.translatable(Text) : Component.translatable(Text, Argument);
        }
        return Component.literal(Text);
    }

    public enum Sender implements StringRepresentable {
        Player("player"),
        System("system"),
        Anomaly("anomaly");

        public static final Codec<Sender> SenderCodec = StringRepresentable.fromEnum(Sender::values);

        private final String SerializedName;

        Sender(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }
}
