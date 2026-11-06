package net.rai.scp_unleashed.scp207;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Scp207Ration {
    public static final int Daily = 16;
    public static final long DayLength = 24000L;
    public static final long DawnOffset = 1000L;
    public static final int OneChance = 10;

    private Scp207Ration() {
    }

    public record State(long Day, int Taken) {
        public static final State Fresh = new State(0L, 0);
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.LONG.optionalFieldOf("day", 0L).forGetter(State::Day),
                Codec.INT.optionalFieldOf("taken", 0).forGetter(State::Taken)
        ).apply(Instance, State::new));
        public static final StreamCodec<ByteBuf, State> StateStreamCodec = StreamCodec.composite(ByteBufCodecs.VAR_LONG, State::Day, ByteBufCodecs.VAR_INT, State::Taken, State::new);

        public int takenOn(long Today) {
            return Day == Today ? Math.max(0, Taken) : 0;
        }

        public int leftOn(long Today) {
            return Math.max(0, Daily - takenOn(Today));
        }
    }

    public static long dayOf(long DayTime) {
        return Math.floorDiv(DayTime + DawnOffset, DayLength);
    }

    public static long today(Level World) {
        MinecraftServer Server = World.getServer();
        Level Clock = Server != null && Server.overworld() != null ? Server.overworld() : World;
        return dayOf(Clock.getDayTime());
    }

    public static boolean rollOne(ServerLevel World) {
        return World.random.nextInt(OneChance) == 0;
    }
}
