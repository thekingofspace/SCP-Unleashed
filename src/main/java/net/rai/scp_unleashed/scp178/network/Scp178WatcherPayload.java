package net.rai.scp_unleashed.scp178.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp178WatcherPayload(byte Kind, int Id, float Value, float Angle) implements CustomPacketPayload {
    public static final byte Spawn = 0;
    public static final byte Hostile = 1;
    public static final byte Distance = 2;
    public static final byte Windup = 3;
    public static final byte Attack = 4;
    public static final byte Killed = 5;
    public static final byte Clear = 6;
    public static final byte Calm = 7;
    public static final Type<Scp178WatcherPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_watcher"));
    public static final StreamCodec<ByteBuf, Scp178WatcherPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BYTE, Scp178WatcherPayload::Kind,
            ByteBufCodecs.VAR_INT, Scp178WatcherPayload::Id,
            ByteBufCodecs.FLOAT, Scp178WatcherPayload::Value,
            ByteBufCodecs.FLOAT, Scp178WatcherPayload::Angle,
            Scp178WatcherPayload::new);

    public Scp178WatcherPayload(byte Kind, int Id, float Value) {
        this(Kind, Id, Value, 0.0f);
    }

    public static Scp178WatcherPayload of(byte Kind, int Id) {
        return new Scp178WatcherPayload(Kind, Id, 0.0f);
    }

    public static Scp178WatcherPayload clear() {
        return new Scp178WatcherPayload(Clear, 0, 0.0f);
    }

    @Override
    public Type<Scp178WatcherPayload> type() {
        return PayloadType;
    }
}
