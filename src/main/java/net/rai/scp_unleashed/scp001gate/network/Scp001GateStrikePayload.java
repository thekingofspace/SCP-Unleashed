package net.rai.scp_unleashed.scp001gate.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;

public record Scp001GateStrikePayload(int Guardian, float X, float Y, float Z) implements CustomPacketPayload {
    public static final Type<Scp001GateStrikePayload> PayloadType = new Type<>(Scp001GateRegistry.location("scp001_gate_strike"));
    public static final StreamCodec<ByteBuf, Scp001GateStrikePayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp001GateStrikePayload::Guardian,
            ByteBufCodecs.FLOAT, Scp001GateStrikePayload::X, ByteBufCodecs.FLOAT, Scp001GateStrikePayload::Y, ByteBufCodecs.FLOAT, Scp001GateStrikePayload::Z,
            Scp001GateStrikePayload::new);

    @Override
    public Type<Scp001GateStrikePayload> type() {
        return PayloadType;
    }
}
