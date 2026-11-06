package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ChatChannels {
    public static final String Network = "network";
    public static final String DatabasePrefix = "db:";
    public static final int MaxName = 16;

    private ChatChannels() {
    }

    public static String database(UUID Id) {
        return DatabasePrefix + Id;
    }

    public static boolean isValidKey(@Nullable String Key) {
        return Key != null && (Network.equals(Key) || databaseId(Key) != null);
    }

    @Nullable
    public static UUID databaseId(@Nullable String Key) {
        if (Key == null || !Key.startsWith(DatabasePrefix)) {
            return null;
        }
        try {
            return UUID.fromString(Key.substring(DatabasePrefix.length()));
        } catch (IllegalArgumentException Problem) {
            return null;
        }
    }

    public record Info(String Key, String Name, boolean Online, int Count, long LastId) {
        public static final Codec<Info> InfoCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.STRING.fieldOf("key").forGetter(Info::Key),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Info::Name),
                Codec.BOOL.optionalFieldOf("online", true).forGetter(Info::Online),
                Codec.INT.optionalFieldOf("count", 0).forGetter(Info::Count),
                Codec.LONG.optionalFieldOf("last", 0L).forGetter(Info::LastId)
        ).apply(Instance, Info::new));

        public boolean isNetwork() {
            return Network.equals(Key);
        }
    }
}
